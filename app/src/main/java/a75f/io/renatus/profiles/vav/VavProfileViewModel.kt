package a75f.io.renatus.profiles.vav

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.RawPoint
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
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.definitions.ReheatType
import a75f.io.logic.bo.building.vav.VavParallelFanProfile
import a75f.io.logic.bo.building.vav.VavProfile
import a75f.io.logic.bo.building.vav.VavProfileConfiguration
import a75f.io.logic.bo.building.vav.VavReheatProfile
import a75f.io.logic.bo.building.vav.VavSeriesFanProfile
import a75f.io.logic.getSchedule
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.RxjavaUtil
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
import kotlin.properties.Delegates

class VavProfileViewModel : ViewModel() {

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var nodeType: NodeType
    private var deviceAddress by Delegates.notNull<Short>()

    private lateinit var vavProfile: VavProfile
    lateinit var profileConfiguration: VavProfileConfiguration

    private lateinit var model : SeventyFiveFProfileDirective
    private lateinit var deviceModel : SeventyFiveFDeviceDirective
    lateinit var viewState: VavConfigViewState

    private lateinit var context : Context
    lateinit var hayStack : CCUHsApi

    lateinit var damperTypesList: List<String>
    lateinit var damperSizesList: List<String>
    lateinit var damperShapesList: List<String>
    lateinit var reheatTypesList: List<String>
    lateinit var zonePrioritiesList: List<String>

    lateinit var temperatureOffsetsList: List<String>

    lateinit var maxCoolingDamperPosList: List<String>
    lateinit var minCoolingDamperPosList: List<String>
    lateinit var maxHeatingDamperPosList: List<String>
    lateinit var minHeatingDamperPosList: List<String>

    lateinit var kFactorsList: List<String>

    lateinit var maxCFMCoolingList: List<String>
    lateinit var minCFMCoolingList: List<String>
    lateinit var maxCFMReheatingList: List<String>
    lateinit var minCFMReheatingList: List<String>

    private val _isDialogOpen = MutableLiveData<Boolean>()
    private var saveJob : Job? = null

    var modelLoaded by  mutableStateOf(false)
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
        CcuLog.i(Domain.LOG_TAG, "VavProfileViewModel EquipModel Loaded")
        deviceModel = getDeviceDomainModel() as SeventyFiveFDeviceDirective
        CcuLog.i(Domain.LOG_TAG, "VavProfileViewModel Device Model Loaded")

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is VavProfile) {
            vavProfile = L.getProfile(deviceAddress) as VavProfile
            profileConfiguration = VavProfileConfiguration(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, model ).getActiveConfiguration()
        } else {
            profileConfiguration = VavProfileConfiguration(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, model ).getDefaultConfiguration()
            /*vavProfile = when (profileType) {
                ProfileType.VAV_PARALLEL_FAN -> VavParallelFanProfile()
                ProfileType.VAV_SERIES_FAN -> VavSeriesFanProfile()
                else -> VavReheatProfile()
            }*/
        }

        CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())



        viewState = VavConfigViewState.fromVavProfileConfig(profileConfiguration)

        this.context = context
        this.hayStack = hayStack

        initializeLists()
        CcuLog.i(Domain.LOG_TAG, "VavProfileViewModel Loaded")
        modelLoaded = true
    }

    private fun initializeLists() {
        damperTypesList = getListByDomainName(DomainName.damperType, model)
        damperSizesList = getListByDomainName(DomainName.damperSize, model)
        damperShapesList = getListByDomainName(DomainName.damperShape, model)
        reheatTypesList = getListByDomainName(DomainName.reheatType, model)
        zonePrioritiesList = getListByDomainName(DomainName.zonePriority, model)

        temperatureOffsetsList = getListByDomainName(DomainName.temperatureOffset, model)

        maxCoolingDamperPosList = getListByDomainName(DomainName.maxCoolingDamperPos, model)
        minCoolingDamperPosList = getListByDomainName(DomainName.minCoolingDamperPos, model)
        maxHeatingDamperPosList = getListByDomainName(DomainName.maxHeatingDamperPos, model)
        minHeatingDamperPosList = getListByDomainName(DomainName.minHeatingDamperPos, model)

        kFactorsList = getListByDomainName(DomainName.kFactor, model)

        maxCFMCoolingList = getListByDomainName(DomainName.maxCFMCooling, model)
        minCFMCoolingList = getListByDomainName(DomainName.minCFMCooling, model)
        maxCFMReheatingList = getListByDomainName(DomainName.maxCFMReheating, model)
        minCFMReheatingList = getListByDomainName(DomainName.minCFMReheating, model)
    }

    fun saveConfiguration() {
        if (saveJob == null) {
            saveJob = viewModelScope.launch {
                ProgressDialogUtils.showProgressDialog(context, "Saving VAV Configuration")
                withContext(Dispatchers.IO) {
                    CCUHsApi.getInstance().resetCcuReady()

                    setUpVavProfile()
                    CcuLog.i(Domain.LOG_TAG, "VavProfile Setup complete")
                    L.saveCCUState()

                    hayStack.syncEntityTree()
                    CCUHsApi.getInstance().setCcuReady()
                    CcuLog.i(Domain.LOG_TAG, "Send seed for $deviceAddress")
                    LSerial.getInstance()
                        .sendSeedMessage(false, false, deviceAddress, zoneRef, floorRef)
                    CcuLog.i(Domain.LOG_TAG, "VavProfile Pairing complete")

                    withContext(Dispatchers.Main) {
                        context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                        showToast("VAV Configuration saved successfully", context)
                        CcuLog.i(Domain.LOG_TAG, "Close Pairing dialog")
                        ProgressDialogUtils.hideProgressDialog()
                        _isDialogOpen.value = false
                    }

                }

                // This check is needed because the dialog sometimes fails to close inside the coroutine.
                // We don't know why this happens.
                if (ProgressDialogUtils.isDialogShowing()) {
                    ProgressDialogUtils.hideProgressDialog()
                    _isDialogOpen.value = false
                }
            }
        }
    }

    private fun setUpVavProfile() {
        viewState.updateConfigFromViewState(profileConfiguration)

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-VAV-" + profileConfiguration.nodeAddress

        if (profileConfiguration.isDefault) {

            addEquipAndPoints(deviceAddress, profileConfiguration, floorRef, zoneRef, nodeType, hayStack, model, deviceModel)
            setOutputTypes(profileConfiguration)
            setScheduleType(profileConfiguration)
            L.ccu().zoneProfiles.add(vavProfile)

        } else {
            equipBuilder.updateEquipAndPoints(profileConfiguration, model, hayStack.site!!.id, equipDis, true)
            vavProfile.init()
            setOutputTypes(profileConfiguration)
            setScheduleType(profileConfiguration)
        }

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
        val equipDis = hayStack.siteName + "-VAV-" + config.nodeAddress
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
        vavProfile = when(profileType) {
            ProfileType.VAV_SERIES_FAN -> VavSeriesFanProfile(equipId, addr)
            ProfileType.VAV_PARALLEL_FAN -> VavParallelFanProfile(equipId, addr)
            else -> VavReheatProfile(equipId, addr)
        }

    }

    private fun getProfileDomainModel() : SeventyFiveFProfileDirective{
        return if (nodeType == NodeType.SMART_NODE) {
            when (profileType) {
                ProfileType.VAV_SERIES_FAN -> ModelLoader.getSmartNodeVavSeriesModelDef() as SeventyFiveFProfileDirective
                ProfileType.VAV_PARALLEL_FAN -> ModelLoader.getSmartNodeVavParallelFanModelDef() as SeventyFiveFProfileDirective
                else -> ModelLoader.getSmartNodeVavNoFanModelDef() as SeventyFiveFProfileDirective
            }
        } else {
            when (profileType) {
                ProfileType.VAV_SERIES_FAN -> ModelLoader.getHelioNodeVavSeriesModelDef() as SeventyFiveFProfileDirective
                ProfileType.VAV_PARALLEL_FAN -> ModelLoader.getHelioNodeVavParallelFanModelDef() as SeventyFiveFProfileDirective
                else -> ModelLoader.getHelioNodeVavNoFanModelDef() as SeventyFiveFProfileDirective
            }
        }
    }

    private fun getDeviceDomainModel() : ModelDirective {
        return if (nodeType == NodeType.SMART_NODE) {
            ModelLoader.getSmartNodeDevice()
        } else {
            ModelLoader.getHelioNodeDevice()
        }
    }

    // "analogType" tag is used by control message code and cannot easily be replaced with a domain name query.
    // We are setting this value upon equip creation/reconfiguration for now.
    private fun setOutputTypes(config: VavProfileConfiguration) {
        val device = hayStack.read("device and addr == \"" + config.nodeAddress + "\"")

        val reheatType = config.reheatType.currentVal.toInt() - 1
        val reheatCmdPoint = hayStack.read("point and group == \"" + config.nodeAddress + "\" and domainName == \"" + DomainName.reheatCmd + "\"")

        // Relay 1 enabled if Reheat Type is 1-Stage or 2-Stage (2-Stage is only an option for VAV No Fan)
        var relay1OpEnabled = reheatType == ReheatType.OneStage.ordinal || reheatType == ReheatType.TwoStage.ordinal
        val relay1 = hayStack.read("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay1 + "\"")
        var relay1Point = RawPoint.Builder().setHashMap(relay1).setType(if (relay1OpEnabled) "Relay N/C" else "Relay N/O").setEnabled(relay1OpEnabled)
        if (reheatType == ReheatType.OneStage.ordinal || reheatType == ReheatType.TwoStage.ordinal) relay1Point.setPointRef(reheatCmdPoint.get("id").toString())
        hayStack.updatePoint(relay1Point.build(), relay1.get("id").toString())

        // Relay 2 is always enabled for VAV Series and Parallel Fan. For VAV No Fan, enabled only when Reheat Type is 2-Stage.
        var relay2OpEnabled = ((!config.profileType.equals(ProfileType.VAV_REHEAT.name)) || reheatType == ReheatType.TwoStage.ordinal)
        var relay2 = hayStack.read("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay2 + "\"")
        var relay2Point = RawPoint.Builder().setHashMap(relay2).setType(if (relay2OpEnabled) "Relay N/C" else "Relay N/O").setEnabled(relay2OpEnabled)
        if (reheatType == ReheatType.TwoStage.ordinal) relay2Point.setPointRef(reheatCmdPoint.get("id").toString())
        hayStack.updatePoint(relay2Point.build(), relay2.get("id").toString())

        var analogOut1 = hayStack.read("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog1Out + "\"")
        var analog1Point = RawPoint.Builder().setHashMap(analogOut1)
        hayStack.updatePoint(analog1Point.setType(getDamperTypeString(config)).build(), analogOut1.get("id").toString())

        var analog2OpEnabled = reheatType == ReheatType.ZeroToTenV.ordinal ||
                reheatType == ReheatType.TwoToTenV.ordinal ||
                reheatType == ReheatType.TenToZeroV.ordinal ||
                reheatType == ReheatType.TenToTwov.ordinal ||
                reheatType == ReheatType.Pulse.ordinal
        var analogOut2 = hayStack.read("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog2Out + "\"");
        var analog2Point = RawPoint.Builder().setHashMap(analogOut2)
        hayStack.updatePoint(analog2Point.setType(getReheatTypeString(config)).setEnabled(analog2OpEnabled).build(), analogOut2.get("id").toString())

    }

    private fun setScheduleType(config: VavProfileConfiguration) {
        val scheduleTypePoint = hayStack.readEntity("point and domainName == \"" + DomainName.scheduleType + "\" and group == \"" + config.nodeAddress + "\"")
        val scheduleTypeId = scheduleTypePoint.get("id").toString()

        val roomSchedule = getSchedule(zoneRef, floorRef)
        if(roomSchedule.isZoneSchedule) {
            hayStack.writeDefaultValById(scheduleTypeId, 1.0)
        } else {
            hayStack.writeDefaultValById(scheduleTypeId, 2.0)
        }
    }

    // This logic will break if the "damperType" point enum is changed
    private fun getDamperTypeString(config: VavProfileConfiguration) : String {
        return when(config.damperType.currentVal.toInt()) {
            0 -> "0-10v"
            1 -> "2-10v"
            2 -> "10-2v"
            3 -> "10-0v"
            4 -> LSmartNode.MAT
            5 -> "0-5v"
            else -> { "0-10v" }
        }
    }

    // This logic will break if the "reheatType" enum is changed
    private fun getReheatTypeString(config: VavProfileConfiguration) : String {
        return when(config.reheatType.currentVal.toInt()) {
            1 -> "0-10v"
            2 -> "2-10v"
            3 -> "10-2v"
            4 -> "10-0v"
            5 -> LSmartNode.PULSE
            else -> { "0-10v" }
        }
    }


}
