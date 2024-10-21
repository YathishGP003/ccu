package a75f.io.renatus.profiles.vav

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.device.mesh.LSerial
import a75f.io.device.mesh.LSmartNode
import a75f.io.domain.equips.VavEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.getListByDomainName
import a75f.io.domain.api.Domain.getListByDomainNameWithCustomMaxVal
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
import a75f.io.logic.bo.building.vav.VavParallelFanProfile
import a75f.io.logic.bo.building.vav.VavProfile
import a75f.io.logic.bo.building.vav.VavProfileConfiguration
import a75f.io.logic.bo.building.vav.VavReheatProfile
import a75f.io.logic.bo.building.vav.VavSeriesFanProfile
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.getSchedule
import a75f.io.messaging.handler.VavConfigHandler
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel.Companion.saveUnUsedPortStatus
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.CoroutineScope
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
    private lateinit var unusedPorts: HashMap<String, Boolean>

    private lateinit var pairingCompleteListener: OnPairingCompleteListener
    private var saveJob : Job? = null

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
            viewState = VavConfigViewState.fromVavProfileConfig(profileConfiguration)
            unusedPorts = UnusedPortsModel.initializeUnUsedPorts(deviceAddress, hayStack)
        } else {
            profileConfiguration = VavProfileConfiguration(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, model ).getDefaultConfiguration()
            viewState = VavConfigViewState.fromVavProfileConfig(profileConfiguration)
        }
        CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
        this.context = context
        this.hayStack = hayStack

        initializeLists()
        CcuLog.i(Domain.LOG_TAG, "Vav profile cofig Loaded")
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
        maxCFMReheatingList = getListByDomainName(DomainName.maxCFMReheating, model)

        if (!profileConfiguration.isDefault && profileConfiguration.enableCFMControl.enabled) {
            val equip = hayStack.read("equip and group == \"" + profileConfiguration.nodeAddress + "\"")
            val vavEquip = VavEquip(equip.get("id").toString())

            minCFMCoolingList = getListByDomainNameWithCustomMaxVal(DomainName.minCFMCooling, model, vavEquip.maxCFMCooling.readDefaultVal())
            minCFMReheatingList = getListByDomainNameWithCustomMaxVal(DomainName.minCFMReheating, model, vavEquip.maxCFMReheating.readDefaultVal())
        } else {
            minCFMCoolingList = getListByDomainName(DomainName.minCFMCooling, model)
            minCFMReheatingList = getListByDomainName(DomainName.minCFMReheating, model)
        }

    }

    fun saveConfiguration() {
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving VAV Configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpVavProfile()
                CcuLog.i(Domain.LOG_TAG, "VavProfile Setup complete")
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("VAV Configuration saved successfully", context)
                    CcuLog.i(Domain.LOG_TAG, "Close Pairing dialog")
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }
                L.saveCCUState()
                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()
                CcuLog.i(Domain.LOG_TAG, "Send seed for $deviceAddress")
                LSerial.getInstance()
                    .sendSeedMessage(false, false, deviceAddress, zoneRef, floorRef)
                DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
                CcuLog.i(Domain.LOG_TAG, "VavProfile Pairing complete")

                // This check is needed because the dialog sometimes fails to close inside the coroutine.
                // We don't know why this happens.
                if (ProgressDialogUtils.isDialogShowing()) {
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }
            }
        }
    }

    private fun setUpVavProfile() {
        viewState.updateConfigFromViewState(profileConfiguration)

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-VAV-" + profileConfiguration.nodeAddress

        if (profileConfiguration.isDefault) {

            addEquipAndPoints(deviceAddress, profileConfiguration, nodeType, hayStack, model, deviceModel)
            VavConfigHandler.setVavOutputTypes(hayStack, profileConfiguration)
            if (L.ccu().bypassDamperProfile != null) overrideForBypassDamper(profileConfiguration)
            setScheduleType(profileConfiguration)
            VavConfigHandler.setMinCfmSetpointMaxVals(hayStack, profileConfiguration)
            VavConfigHandler.setAirflowCfmProportionalRange(hayStack, profileConfiguration)
            vavProfile.init()
            L.ccu().zoneProfiles.add(vavProfile)

        } else {
            equipBuilder.updateEquipAndPoints(profileConfiguration, model, hayStack.site!!.id, equipDis, true)
            if (L.ccu().bypassDamperProfile != null) overrideForBypassDamper(profileConfiguration)
            VavConfigHandler.setVavOutputTypes(hayStack, profileConfiguration)
            VavConfigHandler.setMinCfmSetpointMaxVals(hayStack, profileConfiguration)
            setScheduleType(profileConfiguration)
            VavConfigHandler.setAirflowCfmProportionalRange(hayStack, profileConfiguration)
            CoroutineScope(Dispatchers.IO).launch {
                setScheduleType(profileConfiguration)
                saveUnUsedPortStatus(profileConfiguration, deviceAddress, hayStack)
            }

        }

    }


    private fun addEquipAndPoints(
        deviceAddress: Short,
        config: ProfileConfiguration,
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
            ProfileType.VAV_SERIES_FAN -> VavSeriesFanProfile(equipId, deviceAddress)
            ProfileType.VAV_PARALLEL_FAN -> VavParallelFanProfile(equipId, deviceAddress)
            else -> VavReheatProfile(equipId, deviceAddress)
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

    private fun overrideForBypassDamper(config: VavProfileConfiguration) {
        val equip = hayStack.readEntity("equip and group == \"" + config.nodeAddress + "\"")
        val vavEquip = VavEquip(equip["id"].toString())

        if (vavEquip.enableCFMControl.readDefaultVal() <= 0.0) {
            vavEquip.minCoolingDamperPos.writeVal(7, hayStack.ccuUserName, vavEquip.minCoolingDamperPos.readDefaultVal(), 0)
            vavEquip.minCoolingDamperPos.writeDefaultVal(20.0)
            vavEquip.minCoolingDamperPos.writeHisVal(10.0)

            vavEquip.minHeatingDamperPos.writeVal(7, hayStack.ccuUserName, vavEquip.minHeatingDamperPos.readDefaultVal(), 0)
            vavEquip.minHeatingDamperPos.writeDefaultVal(20.0)
            vavEquip.minHeatingDamperPos.writeHisVal(10.0)
        }

        vavEquip.vavProportionalKFactor.writeVal(14, hayStack.ccuUserName, 0.7, 0)
        vavEquip.vavProportionalKFactor.writeHisVal(0.7)

        vavEquip.vavIntegralKfactor.writeVal(14, hayStack.ccuUserName, 0.3, 0)
        vavEquip.vavIntegralKfactor.writeHisVal(0.3)

        vavEquip.vavTemperatureProportionalRange.writeVal(14, hayStack.ccuUserName, 1.5, 0)
        vavEquip.vavTemperatureProportionalRange.writeHisVal(1.5)

    }

    private fun setScheduleType(config: VavProfileConfiguration) {
        val scheduleTypePoint = hayStack.readEntity("point and domainName == \"" + DomainName.scheduleType + "\" and group == \"" + config.nodeAddress + "\"")
        val scheduleTypeId = scheduleTypePoint["id"].toString()

        val roomSchedule = getSchedule(zoneRef, floorRef)
        if(roomSchedule.isZoneSchedule) {
            hayStack.writeDefaultValById(scheduleTypeId, 1.0)
        } else {
            hayStack.writeDefaultValById(scheduleTypeId, 2.0)
        }
    }

    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }


}
