package a75f.io.renatus.profiles.bypass

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Device
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.sync.PointWriteCache
import a75f.io.device.mesh.LSerial
import a75f.io.device.mesh.LSmartNode
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.getListByDomainName
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.TunerUtil.updateChildEquipsTunerVal
import a75f.io.domain.util.TunerUtil.updateSystemTunerVal
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.bypassdamper.BypassDamperProfile
import a75f.io.logic.bo.building.bypassdamper.BypassDamperProfileConfiguration
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.tuners.TunerUtil
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.SystemConfigFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.util.ProgressDialogUtils
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.projecthaystack.HDateTime
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HNum
import org.projecthaystack.HRef
import org.projecthaystack.HVal
import kotlin.properties.Delegates

class BypassConfigViewModel : ViewModel() {

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var nodeType: NodeType
    private var deviceAddress by Delegates.notNull<Short>()

    private lateinit var bypassDamperProfile: BypassDamperProfile
    lateinit var profileConfiguration: BypassDamperProfileConfiguration

    private lateinit var model : SeventyFiveFProfileDirective
    private lateinit var deviceModel : SeventyFiveFDeviceDirective
    lateinit var viewState: BypassConfigViewState

    private lateinit var context : Context
    lateinit var hayStack : CCUHsApi

    lateinit var damperTypesList: List<String>
    lateinit var damperMinPosList: List<String>
    lateinit var damperMaxPosList: List<String>

    lateinit var pressureSensorTypesList: List<String>
    lateinit var sensorMinVoltageList: List<String>
    lateinit var sensorMaxVoltageList: List<String>
    lateinit var pressureSensorMinValList: List<String>
    lateinit var pressureSensorMaxValList: List<String>

    lateinit var satMinThresholdList: List<String>
    lateinit var satMaxThresholdList: List<String>

    lateinit var pressureSetpointsList: List<String>
    lateinit var expectedPressureErrorList: List<String>

    private val _isDialogOpen = MutableLiveData(true)
    private var saveJob : Job? = null

    var modelLoaded by  mutableStateOf(false)
    var openCancelDialog by mutableStateOf(false)
    var hasUnsavedChanges by mutableStateOf(false)
    var nextDestination by mutableStateOf(-1)
    val isDialogOpen: LiveData<Boolean>
        get() = _isDialogOpen

    fun init(bundle: Bundle, context: Context, hayStack : CCUHsApi) {
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        profileType = ProfileType.values()[bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]
        CcuLog.i(Domain.LOG_TAG, "VavProfileViewModel Init profileType:$profileType " +
                "nodeType:$nodeType deviceAddress:$deviceAddress")
        model = getProfileDomainModel()
        CcuLog.i(Domain.LOG_TAG, "BypassDamperViewModel EquipModel Loaded")
        deviceModel = getDeviceDomainModel() as SeventyFiveFDeviceDirective
        CcuLog.i(Domain.LOG_TAG, "BypassDamperViewModel Device Model Loaded")

        if (L.ccu().bypassDamperProfile != null) {
            bypassDamperProfile = L.ccu().bypassDamperProfile as BypassDamperProfile
            profileConfiguration = BypassDamperProfileConfiguration(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, model ).getActiveConfiguration()
        } else {
            profileConfiguration = BypassDamperProfileConfiguration(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, model ).getDefaultConfiguration()
        }

        CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())



        viewState = BypassConfigViewState.fromBypassDamperProfileConfig(profileConfiguration)

        this.context = context
        this.hayStack = hayStack

        initializeLists()
        CcuLog.i(Domain.LOG_TAG, "BypassDamperViewModel Loaded")
        modelLoaded = true
    }

    private fun initializeLists() {

        pressureSensorTypesList = getListByDomainName(DomainName.pressureSensorType, model)
        damperTypesList = getListByDomainName(DomainName.damperType, model)
        damperMinPosList = getListByDomainName(DomainName.damperMinPosition, model)
        damperMaxPosList = getListByDomainName(DomainName.damperMaxPosition, model)

        sensorMinVoltageList = getListByDomainName(DomainName.sensorMinVoltage, model)
        sensorMaxVoltageList = getListByDomainName(DomainName.sensorMaxVoltage, model)
        pressureSensorMinValList = getListByDomainName(DomainName.pressureSensorMinVal, model)
        pressureSensorMaxValList = getListByDomainName(DomainName.pressureSensorMaxVal, model)

        satMinThresholdList = getListByDomainName(DomainName.satMinThreshold, model)
        satMaxThresholdList = getListByDomainName(DomainName.satMaxThreshold, model)

        pressureSetpointsList = getListByDomainName(DomainName.ductStaticPressureSetpoint, model)
        expectedPressureErrorList = getListByDomainName(DomainName.expectedPressureError, model)

    }

    fun unpair() {
        viewModelScope.launch {
            ProgressDialogUtils.showProgressDialog(context, "Deleting Bypass Damper Equip")
            withContext(Dispatchers.IO) {
                CCUHsApi.getInstance().resetCcuReady()

                try {
                    val sysEquips = HSUtil.getEquips("SYSTEM") + HSUtil.getEquips("@SYSTEM")
                    val bdEquips = sysEquips.filter { eq : Equip -> eq.domainName != null && eq.domainName == DomainName.smartnodeBypassDamper && eq.group.equals(profileConfiguration.nodeAddress.toString())}
                    val sysDevices = HSUtil.getDevices("SYSTEM") + HSUtil.getDevices("@SYSTEM")
                    val bdDevices = sysDevices.filter { d : Device -> d.domainName != null && d.addr.equals(profileConfiguration.nodeAddress.toString()) && d.domainName.equals("smartnodeDevice")}
                    bdEquips.forEach { bdEquip -> hayStack.deleteEntityTree(bdEquip?.id) }
                    bdDevices.forEach { bdDevice -> hayStack.deleteEntityTree(bdDevice?.id) }
                    L.ccu().bypassDamperProfile = null
                    CcuLog.i(Domain.LOG_TAG, "Bypass Damper Equip deleted successfully")
                } catch (e: Exception) {
                    CcuLog.d(L.TAG_CCU_UI, "Exception while trying to delete Bypass Damper equip")
                    e.printStackTrace()
                }

                L.saveCCUState()

                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()

                CcuLog.i(Domain.LOG_TAG, "Bypass Damper Profile Deletion complete")
                releaseTunerOverrides()
            }

            withContext(Dispatchers.Main) {
                SystemConfigFragment.SystemConfigFragmentHandler.sendEmptyMessage(7)
                ProgressDialogUtils.hideProgressDialog()
                _isDialogOpen.value = false
                showToast("Bypass Damper Equip deleted successfully", context)
                CcuLog.i(Domain.LOG_TAG, "Close Pairing dialog")
            }
        }
    }

    fun cancelConfirm() {
        hasUnsavedChanges = true

        if (L.ccu().bypassDamperProfile != null) {
            bypassDamperProfile = L.ccu().bypassDamperProfile as BypassDamperProfile
            profileConfiguration = BypassDamperProfileConfiguration(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, model ).getActiveConfiguration()
        } else {
            profileConfiguration = BypassDamperProfileConfiguration(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, model ).getDefaultConfiguration()
        }

        viewState = BypassConfigViewState.fromBypassDamperProfileConfig(profileConfiguration)

        hasUnsavedChanges = false
    }

    fun hasUnsavedChanges() : Boolean {
        try {
            hasUnsavedChanges = !BypassConfigViewState.fromBypassDamperProfileConfig(profileConfiguration).equalsViewState(viewState)
            return hasUnsavedChanges
        } catch (e: Exception) {
            return false
        }

    }

    fun saveConfiguration() {
        if (saveJob == null) {
            saveJob = viewModelScope.launch {
                ProgressDialogUtils.showProgressDialog(
                    context,
                    "Saving Bypass Damper Configuration"
                )
                withContext(Dispatchers.IO) {
                    CCUHsApi.getInstance().resetCcuReady()

                    setUpBypassDamperProfile()
                    CcuLog.i(Domain.LOG_TAG, "Bypass Damper Profile Setup complete")
                    L.saveCCUState()

                    hayStack.syncEntityTree()
                    CCUHsApi.getInstance().setCcuReady()
                    CcuLog.i(Domain.LOG_TAG, "Send seed for $deviceAddress")
                    LSerial.getInstance().sendBypassSeedMessage()

                    CcuLog.i(Domain.LOG_TAG, "Bypass Damper Profile Pairing complete")

                    withContext(Dispatchers.Main) {
                        SystemConfigFragment.SystemConfigFragmentHandler.sendEmptyMessage(7)
                        ProgressDialogUtils.hideProgressDialog()
                        _isDialogOpen.value = false
                        context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                        showToast("Bypass Damper Configuration saved successfully", context)
                        CcuLog.i(Domain.LOG_TAG, "Close Pairing dialog")
                    }
                }
            }
        }
    }

    private fun setUpBypassDamperProfile() {
        viewState.updateConfigFromViewState(profileConfiguration)

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-BYPASS-" + profileConfiguration.nodeAddress

        if (profileConfiguration.isDefault) {

            addEquipAndPoints(deviceAddress, profileConfiguration, floorRef, zoneRef, nodeType, hayStack, model, deviceModel)
            setPressureSensorRef(profileConfiguration)
            setOutputTypes(profileConfiguration)
            setDamperFeedback(profileConfiguration)

        } else {
            val equipRef = equipBuilder.updateEquipAndPoints(profileConfiguration, model, hayStack.site!!.id, equipDis, isReconfiguration = true)
            bypassDamperProfile = BypassDamperProfile(equipRef, deviceAddress)
            setPressureSensorRef(profileConfiguration)
            setOutputTypes(profileConfiguration)
            setDamperFeedback(profileConfiguration)
            overrideTunersForBypassDamper()
        }
        L.ccu().bypassDamperProfile = bypassDamperProfile
    }

    private fun addEquipAndPoints(
        addr: Short,
        config: ProfileConfiguration,
        floorRef: String?,
        roomRef: String?,
        nodeType: NodeType?,
        hayStack: CCUHsApi,
        equipModel: SeventyFiveFProfileDirective?,
        deviceModel: SeventyFiveFDeviceDirective?
    ) {
        requireNotNull(equipModel)
        requireNotNull(deviceModel)
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-BYPASS-" + config.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " buildEquipAndPoints ${model.domainName} profileType ${config.profileType}" )
        val equipId = equipBuilder.buildEquipAndPoints(
            config, equipModel, hayStack.site!!
                .id, equipDis
        )
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val deviceName = when(nodeType) { NodeType.HELIO_NODE -> "-HN-" else -> "-SN-"}
        val deviceDis = hayStack.siteName + deviceName + config.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        deviceBuilder.buildDeviceAndPoints(
            config,
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )
        CcuLog.i(Domain.LOG_TAG, " add Profile")
        bypassDamperProfile = BypassDamperProfile(equipId, addr)
        overrideTunersForBypassDamper()
    }

    private fun overrideTunersForBypassDamper() {
        updateSystemTunerVal("dab and target and cumulative and damper", 20.0, "Bypass Damper Added", hayStack)
        updateSystemTunerVal("dab and pgain and not reheat", 0.7, "Bypass Damper Added", hayStack)
        updateSystemTunerVal("dab and igain and not reheat", 0.3, "Bypass Damper Added", hayStack)
        updateSystemTunerVal("dab and pspread and not reheat", 1.5, "Bypass Damper Added", hayStack)

        updateSystemTunerVal("dualDuct and target and cumulative and damper", 20.0, "Bypass Damper Added", hayStack)
        updateSystemTunerVal("dualDuct and pgain and not reheat", 0.7, "Bypass Damper Added", hayStack)
        updateSystemTunerVal("dualDuct and igain and not reheat", 0.3, "Bypass Damper Added", hayStack)
        updateSystemTunerVal("dualDuct and pspread and not reheat", 1.5, "Bypass Damper Added", hayStack)

        updateSystemTunerVal("vav and target and cumulative and damper", 20.0, "Bypass Damper Added", hayStack)
        updateSystemTunerVal("vav and pgain and not airflow and not air", 0.7, "Bypass Damper Added", hayStack)
        updateSystemTunerVal("vav and igain and not airflow and not air", 0.3, "Bypass Damper Added", hayStack)
        updateSystemTunerVal("vav and pspread and not airflow and not air", 1.5, "Bypass Damper Added", hayStack)


        CCUHsApi.getInstance().readEntity("equip and system and not modbus and not connectModule")["id"]?.let {sysEquipId ->
            val childEquips = HSUtil.getEquipsWithAhuRefOnThisCcu(sysEquipId.toString())
            val childEquipsIterator = childEquips.iterator()
            while(childEquipsIterator.hasNext()) {
                val eq = childEquipsIterator.next()

                hayStack.readEntity("point and config and damper and cooling and min and not analog1 and not analog2 and equipRef == \"" + eq.id + "\"")["id"]?.let { minCoolingDamperPosPointId ->
                    hayStack.writePointForCcuUser(minCoolingDamperPosPointId.toString(), 8, 10.0, 0, "Bypass Damper Added")
                    hayStack.writeHisValById(minCoolingDamperPosPointId.toString(), 10.0)
                }

                hayStack.readEntity("point and config and damper and heating and min and not analog1 and not analog2 and equipRef == \"" + eq.id + "\"")["id"]?.let { minHeatingDamperPosPointId ->
                    hayStack.writePointForCcuUser(minHeatingDamperPosPointId.toString(), 8, 10.0, 0, "Bypass Damper Added")
                    hayStack.writeHisValById(minHeatingDamperPosPointId.toString(), 10.0)
                }

                if (eq.markers.contains("dualDuct")) {
                    val systemPGain = TunerUtil.readTunerValByQuery("system and dab and pgain and not reheat and not default")
                    hayStack.readEntity("point and tuner and dualDuct and pgain and not reheat and equipRef == \"" + eq.id + "\"")["id"]?.let { pGainPointId ->
                        hayStack.writePointForCcuUser(pGainPointId.toString(), 14, systemPGain, 0, "Bypass Damper Added")
                    }

                    val systemIGain = TunerUtil.readTunerValByQuery("system and dab and igain and not reheat and not default")
                    val iGainPoint = hayStack.readEntity("point and tuner and dualDuct and igain and not reheat and equipRef == \"" + eq.id + "\"")
                    val iGainPointId = iGainPoint["id"].toString()
                    hayStack.writePointForCcuUser(iGainPointId, 14, systemIGain, 0, "Bypass Damper Added")

                    val systemPSpread = TunerUtil.readTunerValByQuery("system and dab and pspread and not reheat and not default")
                    hayStack.readEntity("point and tuner and dualDuct and pspread and not reheat and equipRef == \"" + eq.id + "\"")["id"]?.let { pSpreadPointId ->
                        hayStack.writePointForCcuUser(pSpreadPointId.toString(), 14, systemPSpread, 0, "Bypass Damper Added")
                    }
                }
            }
        }
    }

    private fun releaseTunerOverrides() {
        updateSystemTunerVal("dab and target and cumulative and damper", null, "Bypass Damper Unpaired", hayStack)
        updateSystemTunerVal("dab and pgain and not reheat", null, "Bypass Damper Unpaired", hayStack)
        updateSystemTunerVal("dab and igain and not reheat", null, "Bypass Damper Unpaired", hayStack)
        updateSystemTunerVal("dab and pspread and not reheat", null, "Bypass Damper Unpaired", hayStack)

        updateSystemTunerVal("vav and target and cumulative and damper", null, "Bypass Damper Unpaired", hayStack)
        updateSystemTunerVal("vav and pgain and not airflow and not air", null, "Bypass Damper Unpaired", hayStack)
        updateSystemTunerVal("vav and igain and not airflow and not air", null, "Bypass Damper Unpaired", hayStack)
        updateSystemTunerVal("vav and pspread and not airflow and not air", null, "Bypass Damper Unpaired", hayStack)

        hayStack.readEntity("equip and system and not modbus and not connectModule")["id"]?.let { sysEquipId ->
            val childEquips = HSUtil.getEquipsWithAhuRefOnThisCcu(sysEquipId.toString())
            val childEquipsIterator = childEquips.iterator()
            while(childEquipsIterator.hasNext()) {
                val eq = childEquipsIterator.next()

                if (eq.markers.contains("dualDuct")) {
                    //val pGainPoint = hayStack.readEntity("point and tuner and dualDuct and pgain and not reheat and equipRef == \"" + eq.id + "\"")
                    //val pGainPointId = pGainPoint["id"].toString()
                    updateChildEquipsTunerVal(
                        sysEquipId.toString(),
                        "dualDuct and pgain and not reheat",
                        null,
                        "Bypass Damper Added",
                        hayStack
                    )
                    //hayStack.writePointForCcuUser(pGainPointId, 14, null, 0, "Bypass Damper Added")

                    //val iGainPoint = hayStack.readEntity("point and tuner and dualDuct and igain and not reheat and equipRef == \"" + eq.id + "\"")
                    //val iGainPointId = iGainPoint["id"].toStrin
                    updateChildEquipsTunerVal(
                        sysEquipId.toString(),
                        "dualDuct and igain and not reheat",
                        null,
                        "Bypass Damper Added",
                        hayStack
                    )
                    //hayStack.writePointForCcuUser(iGainPointId, 14, null, 0, "Bypass Damper Added")

                    //val pSpreadPoint = hayStack.readEntity("point and tuner and dualDuct and pspread and not reheat and equipRef == \"" + eq.id + "\"")
                    //val pSpreadPointId = pSpreadPoint["id"].toString()
                    updateChildEquipsTunerVal(
                        sysEquipId.toString(),
                        "dualDuct and pspread and not reheat",
                        null,
                        "Bypass Damper Added",
                        hayStack
                    )
                    //hayStack.writePointForCcuUser(pSpreadPointId, 14, null, 0, "Bypass Damper Added")
                }
            }
        }
    }

    private fun getProfileDomainModel() : SeventyFiveFProfileDirective{
        return ModelLoader.getSmartNodeBypassDamperModelDef() as SeventyFiveFProfileDirective
    }

    private fun getDeviceDomainModel() : ModelDirective {
        return ModelLoader.getSmartNodeDevice()
    }

    private fun setPressureSensorRef(config: BypassDamperProfileConfiguration) {
        val device = hayStack.read("device and addr == \"" + config.nodeAddress + "\"")
        val equip = hayStack.read("equip and group == \"" + config.nodeAddress + "\"")
        
        val physPressureSensor = hayStack.read("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.pressureSensor + "\"")
        val physPressureSensorPoint = RawPoint.Builder().setHashMap(physPressureSensor)
        val analogIn1 = hayStack.read("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog1In + "\"")
        val analogIn1Point = RawPoint.Builder().setHashMap(analogIn1)
        val logPressureSensor = hayStack.read("point and equipRef == \""+equip.get("id")+"\" and domainName == \"" + DomainName.ductStaticPressureSensor + "\"")

        if (config.pressureSensorType.currentVal > 0.0) {
            hayStack.updatePoint(analogIn1Point.setPointRef(logPressureSensor.get("id").toString()).build(), analogIn1.get("id").toString())
            hayStack.updatePoint(physPressureSensorPoint.setPointRef(null).build(), physPressureSensor.get("id").toString())
        } else {
            hayStack.updatePoint(analogIn1Point.setPointRef(null).build(), analogIn1.get("id").toString())
            hayStack.updatePoint(physPressureSensorPoint.setPointRef(logPressureSensor.get("id").toString()).build(), physPressureSensor.get("id").toString())
        }
    }
    
    // "analogType" tag is used by control message code and cannot easily be replaced with a domain name query.
    // We are setting this value upon equip creation/reconfiguration for now.
    // In Bypass Damper, this only applies to the Damper on AO1.
    private fun setOutputTypes(config: BypassDamperProfileConfiguration) {
        val device = hayStack.read("device and addr == \"" + config.nodeAddress + "\"")

        var analogOut1 = hayStack.read("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog1Out + "\"")
        var analog1Point = RawPoint.Builder().setHashMap(analogOut1)
        hayStack.updatePoint(analog1Point.setType(getDamperTypeString(config)).build(), analogOut1.get("id").toString())

    }

    /*
        Our approach has been to set the range (e.g. 0-10v = 0-100%) of the Damper Feedback point to the same range as the Damper Output. User cannot modify this.
        This works for our 75F actuators, which are 0-10V Control with 0-10V Feedback.
        But, not all third-party actuators work this way. E.g. 0-10V Control with 2-10V Feedback is fairly common for Belimo actuators.

        As more retrofits of existing buildings are done, we may start to hear complaints about damper feedback appearing incorrect. Until then, I don't think this small
        issue justifies the effort it would take to enhance it.
     */
    private fun setDamperFeedback(config: BypassDamperProfileConfiguration) {
        val device = hayStack.read("device and addr == \"" + config.nodeAddress + "\"")

        var analogIn2 = hayStack.read("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog2In + "\"")
        var analogIn2Point = RawPoint.Builder().setHashMap(analogIn2)
        hayStack.updatePoint(analogIn2Point.setType(getDamperTypeString(config)).build(), analogIn2.get("id").toString())
    }

    // This logic will break if the "damperType" point enum is changed
    private fun getDamperTypeString(config: BypassDamperProfileConfiguration) : String {
        return when(config.damperType.currentVal.toInt()) {
            0 -> "0-10v"
            1 -> "2-10v"
            2 -> "10-0v"
            3 -> "10-2v"
            4 -> LSmartNode.MAT
            5 -> "0-5v"
            else -> { "0-10v" }
        }
    }

}
