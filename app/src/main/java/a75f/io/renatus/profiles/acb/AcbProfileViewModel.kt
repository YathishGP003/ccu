package a75f.io.renatus.profiles.acb

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.VavAcbEquip
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
import a75f.io.logic.bo.building.vav.AcbProfileConfiguration
import a75f.io.logic.bo.building.vav.VavAcbProfile
import a75f.io.logic.getSchedule
import a75f.io.messaging.handler.ACBConfigHandler
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates


class AcbProfileViewModel : ViewModel() {

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var nodeType: NodeType
    private var deviceAddress by Delegates.notNull<Short>()

    private lateinit var acbProfile: VavAcbProfile
    lateinit var profileConfiguration: AcbProfileConfiguration

    private lateinit var model : SeventyFiveFProfileDirective
    private lateinit var deviceModel : SeventyFiveFDeviceDirective
    lateinit var viewState: AcbConfigViewState

    private lateinit var context : Context
    lateinit var hayStack : CCUHsApi

    lateinit var damperTypesList: List<String>
    lateinit var damperSizesList: List<String>
    lateinit var damperShapesList: List<String>
    lateinit var valveTypesList: List<String>
    lateinit var zonePrioritiesList: List<String>
    lateinit var relay1AssociationList: List<String>

    lateinit var condensateSensorTypesList: List<String>

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

        // Models are temporarily loaded from local files to allow quick model revisions during development.
        // In the released CCU build, these will draw from the Hayloft API.
        model = getProfileDomainModel()
        CcuLog.i(Domain.LOG_TAG, "AcbProfileViewModel EquipModel Loaded")
        deviceModel = getDeviceDomainModel() as SeventyFiveFDeviceDirective
        CcuLog.i(Domain.LOG_TAG, "AcbProfileViewModel Device Model Loaded")

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is VavAcbProfile) {
            acbProfile = L.getProfile(deviceAddress) as VavAcbProfile
            profileConfiguration = AcbProfileConfiguration(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, model).getActiveConfiguration()

            viewState = AcbConfigViewState.fromAcbProfileConfig(profileConfiguration)
            unusedPorts = UnusedPortsModel.initializeUnUsedPorts(deviceAddress, hayStack)
        } else {
            profileConfiguration = AcbProfileConfiguration(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, model).getDefaultConfiguration()
            viewState = AcbConfigViewState.fromAcbProfileConfig(profileConfiguration)
        }
        CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
        this.context = context
        this.hayStack = hayStack
        initializeLists()
        CcuLog.i(Domain.LOG_TAG, "ACB Config Loaded")
    }

    private fun initializeLists() {
        damperTypesList = getListByDomainName(DomainName.damperType, model)
        damperSizesList = getListByDomainName(DomainName.damperSize, model)
        damperShapesList = getListByDomainName(DomainName.damperShape, model)
        valveTypesList = getListByDomainName(DomainName.valveType, model)
        zonePrioritiesList = getListByDomainName(DomainName.zonePriority, model)

        relay1AssociationList = listOf("Water Valve (N/C)", "Water Valve (N/O)")

        condensateSensorTypesList = getListByDomainName(DomainName.thermistor2Type, model)

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
            val vavEquip = VavAcbEquip(equip.get("id").toString())

            minCFMCoolingList = Domain.getListByDomainNameWithCustomMaxVal(DomainName.minCFMCooling, model, vavEquip.maxCFMCooling.readDefaultVal())
            minCFMReheatingList = Domain.getListByDomainNameWithCustomMaxVal(DomainName.minCFMReheating, model, vavEquip.maxCFMReheating.readDefaultVal())
        } else {
            minCFMCoolingList = getListByDomainName(DomainName.minCFMCooling, model)
            minCFMReheatingList = getListByDomainName(DomainName.minCFMReheating, model)
        }

    }

    fun saveConfiguration() {
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving ACB Configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpAcbProfile()
                CcuLog.i(Domain.LOG_TAG, "AcbProfile Setup complete")
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("ACB Configuration saved successfully", context)
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
                CcuLog.i(Domain.LOG_TAG, "AcbProfile Pairing complete")

                // This check is needed because the dialog sometimes fails to close inside the coroutine.
                // We don't know why this happens.
                if (ProgressDialogUtils.isDialogShowing()) {
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }
            }
        }
    }

    private fun setUpAcbProfile() {
        //DomainManager.buildDomain(CCUHsApi.getInstance())
        viewState.updateConfigFromViewState(profileConfiguration)

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-ACB-" + profileConfiguration.nodeAddress

        if (profileConfiguration.isDefault) {

            addEquipAndPoints(deviceAddress, profileConfiguration, floorRef, zoneRef, nodeType, hayStack, model, deviceModel)
            ACBConfigHandler.setOutputTypes(hayStack, profileConfiguration)
            ACBConfigHandler.updateCondensateSensor(hayStack, profileConfiguration)
            if (L.ccu().bypassDamperProfile != null) overrideForBypassDamper(profileConfiguration)
            setScheduleType(profileConfiguration)
            ACBConfigHandler.setMinCfmSetpointMaxVals(hayStack, profileConfiguration)
            ACBConfigHandler.setAirflowCfmProportionalRange(hayStack, profileConfiguration)
            // Have to reload the profile here because we just changed the CFM proportional range
            acbProfile.init()
            L.ccu().zoneProfiles.add(acbProfile)

        } else {
            equipBuilder.updateEquipAndPoints(profileConfiguration, model, hayStack.site!!.id, equipDis, true)
            if (L.ccu().bypassDamperProfile != null) overrideForBypassDamper(profileConfiguration)

            ACBConfigHandler.setOutputTypes(hayStack, profileConfiguration)
            saveUnUsedPortStatus(profileConfiguration, deviceAddress, hayStack)
            ACBConfigHandler.updateCondensateSensor(hayStack, profileConfiguration)
            ACBConfigHandler.updateRelayAssociation(hayStack, profileConfiguration)
            ACBConfigHandler.setMinCfmSetpointMaxVals(hayStack, profileConfiguration)
            ACBConfigHandler.setAirflowCfmProportionalRange(hayStack, profileConfiguration)
            acbProfile.init()
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
        //val deviceMap = VavEquip(getProfileType(), addr)
        requireNotNull(equipModel)
        requireNotNull(deviceModel)
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-ACB-" + config.nodeAddress
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
        acbProfile = VavAcbProfile(equipId, addr)
    }

    private fun getProfileDomainModel() : SeventyFiveFProfileDirective{
        return if (nodeType == NodeType.SMART_NODE) {
            ModelLoader.getSmartNodeVavAcbModelDef() as SeventyFiveFProfileDirective
        } else {
            ModelLoader.getHelioNodeVavAcbModelDef() as SeventyFiveFProfileDirective
        }
    }

    private fun getDeviceDomainModel() : ModelDirective {
        return if (nodeType == NodeType.SMART_NODE) {
            ModelLoader.getSmartNodeDevice()
        } else {
            ModelLoader.getHelioNodeDevice()
        }
    }

    private fun updateEquipAndPoints(
        addr: Short,
        config: ProfileConfiguration,
        floorRef: String?,
        roomRef: String?,
        nodeType: NodeType?,
        hayStack: CCUHsApi,
        equipModel: SeventyFiveFProfileDirective,
        deviceModel: SeventyFiveFDeviceDirective
    ) {
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-ACB-" + config.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " updateEquipAndPoints")
        val equipId = equipBuilder.updateEquipAndPoints(profileConfiguration, model, hayStack.site!!.id, equipDis)
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val deviceDis = hayStack.siteName + "-SN-" + config.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " updateDeviceAndPoints")
        deviceBuilder.updateDeviceAndPoints(
            config,
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )
        acbProfile = VavAcbProfile(equipId, addr)
    }

    private fun setScheduleType(config: AcbProfileConfiguration) {
        hayStack.readEntity("point and domainName == \"" + DomainName.scheduleType + "\" and group == \"" + config.nodeAddress + "\"")["id"]?.let { scheduleTypeId ->
            val roomSchedule = getSchedule(zoneRef, floorRef)
            if(roomSchedule.isZoneSchedule) {
                hayStack.writeDefaultValById(scheduleTypeId.toString(), 1.0)
            } else {
                hayStack.writeDefaultValById(scheduleTypeId.toString(), 2.0)
            }
        }
    }

    private fun overrideForBypassDamper(config: AcbProfileConfiguration) {
        val equip = hayStack.read("equip and group == \"" + config.nodeAddress + "\"")
        val acbEquip = VavAcbEquip(equip.get("id").toString())

        if (!(acbEquip.enableCFMControl.readDefaultVal() > 0.0)) {
            acbEquip.minCoolingDamperPos.writeVal(7, hayStack.ccuUserName, acbEquip.minCoolingDamperPos.readDefaultVal(), 0)
            acbEquip.minCoolingDamperPos.writeDefaultVal(20.0)
            acbEquip.minCoolingDamperPos.writeHisVal(10.0)

            acbEquip.minHeatingDamperPos.writeVal(7, hayStack.ccuUserName, acbEquip.minHeatingDamperPos.readDefaultVal(), 0)
            acbEquip.minHeatingDamperPos.writeDefaultVal(20.0)
            acbEquip.minHeatingDamperPos.writeHisVal(10.0)
        }

        // Right now, framework is not copying from System Tuner correctly. Duct-tape fix for this case.
        acbEquip.vavProportionalKFactor.writeVal(14, hayStack.ccuUserName, 0.7, 0)
        acbEquip.vavProportionalKFactor.writeHisVal(0.7)

        acbEquip.vavIntegralKfactor.writeVal(14, hayStack.ccuUserName, 0.3, 0)
        acbEquip.vavIntegralKfactor.writeHisVal(0.3)

        acbEquip.vavTemperatureProportionalRange.writeVal(14, hayStack.ccuUserName, 1.5, 0)
        acbEquip.vavTemperatureProportionalRange.writeHisVal(1.5)

    }

    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }

}
