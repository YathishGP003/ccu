package a75f.io.renatus.profiles.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Device
import a75f.io.device.mesh.LSerial
import a75f.io.device.mesh.LSmartNode
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.equips.DabEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.dab.DabProfile
import a75f.io.logic.bo.building.dab.DabProfile.CARRIER_PROD
import a75f.io.logic.bo.building.dab.DabProfileConfiguration
import a75f.io.logic.bo.building.dab.getDevicePointDict
import a75f.io.logic.bo.building.definitions.DamperType
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.definitions.ReheatType
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.getSchedule
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.BuildConfig
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import android.annotation.SuppressLint
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

class DabProfileViewModel : ViewModel() {
    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var nodeType: NodeType
    private var deviceAddress by Delegates.notNull<Short>()

    private lateinit var dabProfile: DabProfile
    lateinit var profileConfiguration: DabProfileConfiguration
    private lateinit var model: SeventyFiveFProfileDirective
    private lateinit var deviceModel: SeventyFiveFDeviceDirective
    lateinit var viewState: DabConfigViewState
    @SuppressLint("StaticFieldLeak")
    private lateinit var context: Context
    lateinit var hayStack: CCUHsApi

    lateinit var damper1TypesList: List<String>
    lateinit var damper1SizesList: List<String>
    lateinit var damper1ShapesList: List<String>
    lateinit var damper2TypesList: List<String>
    lateinit var damper2SizesList: List<String>
    lateinit var damper2ShapesList: List<String>
    lateinit var reheatTypesList: List<String>
    lateinit var zonePrioritiesList: List<String>
    lateinit var temperatureOffsetsList: List<String>

    lateinit var minCfmIaqList: List<String>
    lateinit var minReheatDamperPosList: List<String>
    lateinit var maxCoolingDamperPosList: List<String>
    lateinit var minCoolingDamperPosList: List<String>
    lateinit var maxHeatingDamperPosList: List<String>
    lateinit var minHeatingDamperPosList: List<String>

    lateinit var kFactorsList: List<String>

    private lateinit var pairingCompleteListener: OnPairingCompleteListener
    private var saveJob: Job? = null
    fun init(bundle: Bundle, requireContext: Context, hayStack: CCUHsApi) {

        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        profileType = ProfileType.values()[bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]
        CcuLog.i(
            Domain.LOG_TAG, "DABProfileViewModel Init profileType:$profileType " +
                    "nodeType:$nodeType deviceAddress:$deviceAddress"
        )
        model = getProfileDomainModel()
        CcuLog.i(Domain.LOG_TAG, "DABProfileViewModel EquipModel Loaded")
        deviceModel = getDeviceDomainModel() as SeventyFiveFDeviceDirective
        CcuLog.i(Domain.LOG_TAG, "DABProfileViewModel Device Model Loaded")

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is DabProfile) {
            dabProfile = L.getProfile(deviceAddress) as DabProfile
            profileConfiguration = DabProfileConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, model
            ).getActiveConfiguration()
            CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
            viewState = DabConfigViewState.fromDabProfileConfig(profileConfiguration)
        } else {
            profileConfiguration = DabProfileConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, model
            ).getDefaultConfiguration()
            CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
            viewState = DabConfigViewState.fromDabProfileConfig(profileConfiguration)
        }
        this.context = requireContext
        this.hayStack = hayStack

        initializeLists()
        CcuLog.i(Domain.LOG_TAG, "DabProfileViewModel Loaded")
    }

    private fun initializeLists() {
        damper1TypesList = Domain.getListOfDisNameByDomainName(DomainName.damper1Type, model)
        damper1SizesList = Domain.getListByDomainName(DomainName.damper1Size, model)
        damper1ShapesList = Domain.getListByDomainName(DomainName.damper1Shape, model)

        damper2TypesList = Domain.getListOfDisNameByDomainName(DomainName.damper2Type, model)
        damper2SizesList = Domain.getListByDomainName(DomainName.damper2Size, model)
        damper2ShapesList = Domain.getListByDomainName(DomainName.damper2Shape, model)

        reheatTypesList = Domain.getListByDomainName(DomainName.reheatType, model)
        zonePrioritiesList = Domain.getListByDomainName(DomainName.zonePriority, model)

        temperatureOffsetsList = Domain.getListByDomainName(DomainName.temperatureOffset, model)

        minCfmIaqList = Domain.getListByDomainName(DomainName.minCFMIAQ, model)
        minReheatDamperPosList = Domain.getListByDomainName(DomainName.minReheatDamperPos, model)
        maxCoolingDamperPosList = Domain.getListByDomainName(DomainName.maxCoolingDamperPos, model)
        minCoolingDamperPosList = Domain.getListByDomainName(DomainName.minCoolingDamperPos, model)
        maxHeatingDamperPosList = Domain.getListByDomainName(DomainName.maxHeatingDamperPos, model)
        minHeatingDamperPosList = Domain.getListByDomainName(DomainName.minHeatingDamperPos, model)

        kFactorsList = Domain.getListByDomainName(DomainName.kFactor, model)
    }

    private fun getProfileDomainModel(): SeventyFiveFProfileDirective {
        return if (nodeType == NodeType.SMART_NODE) {
            ModelLoader.getSmartNodeDabModel() as SeventyFiveFProfileDirective
        } else {
            ModelLoader.getHelioNodeDabModel() as SeventyFiveFProfileDirective
        }
    }

    private fun getDeviceDomainModel(): ModelDirective {
        return if (nodeType == NodeType.SMART_NODE) {
            ModelLoader.getSmartNodeDevice()
        } else {
            ModelLoader.getHelioNodeDevice()
        }
    }

    fun saveConfiguration() {
        if (saveJob == null) {
            CCUHsApi.getInstance().resetCcuReady()
            if(BuildConfig.BUILD_TYPE == CARRIER_PROD){
                ProgressDialogUtils.showProgressDialog(context,"Saving VVT-C Configuration")
            }else{
                ProgressDialogUtils.showProgressDialog(context, "Saving DAB Configuration")
            }
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                    setUpDabProfile()
                    withContext(Dispatchers.Main) {
                        context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                        if(BuildConfig.BUILD_TYPE == CARRIER_PROD) {
                            showToast("VVT-C Configuration saved successfully", context)
                        }else{
                            showToast("DAB Configuration saved successfully", context)
                        }
                        if (ProgressDialogUtils.isDialogShowing()) {
                            ProgressDialogUtils.hideProgressDialog()
                            CcuLog.i(Domain.LOG_TAG, "Closing DAB dialog")
                            pairingCompleteListener.onPairingComplete()
                        }
                    }
                    L.saveCCUState()
                    CCUHsApi.getInstance().setCcuReady()
                    hayStack.syncEntityTree()
                    CcuLog.i(Domain.LOG_TAG, "Send seed for $deviceAddress")
                    LSerial.getInstance()
                        .sendSeedMessage(false, false, deviceAddress, zoneRef, floorRef)
                    DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
                    CcuLog.i(Domain.LOG_TAG, "DabProfile Pairing complete")
            }
        }
    }

    private fun setUpDabProfile() {
        viewState.updateConfigFromViewState(profileConfiguration)
        val equipDis =  if (BuildConfig.BUILD_TYPE.equals(CARRIER_PROD, ignoreCase = true)) {
            hayStack.siteName + "-VVT-C-" + profileConfiguration.nodeAddress
        }else{
            hayStack.siteName + "-DAB-" + profileConfiguration.nodeAddress
        }
        val entityMapper = EntityMapper(model)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        if (profileConfiguration.isDefault) {
            addEquipAndPoints(
                deviceAddress,
                profileConfiguration,
                nodeType,
                hayStack,
                model,
                deviceModel,
                deviceBuilder
            )
            setOutputTypes(hayStack, profileConfiguration, deviceBuilder, deviceModel)
            if (L.ccu().bypassDamperProfile != null) { overrideForBypassDamper(profileConfiguration); }
            setScheduleType(profileConfiguration)
            L.ccu().zoneProfiles.add(dabProfile)
        } else {
            val equipBuilder = ProfileEquipBuilder(hayStack)
            equipBuilder.updateEquipAndPoints(
                profileConfiguration,
                model,
                hayStack.site!!.id,
                equipDis,
                true
            )
            if (L.ccu().bypassDamperProfile != null) { overrideForBypassDamper(profileConfiguration); }
            setOutputTypes(hayStack, profileConfiguration, deviceBuilder, deviceModel)
            setScheduleType(profileConfiguration)
            CoroutineScope(Dispatchers.Default).launch {
                UnusedPortsModel.saveUnUsedPortStatus(profileConfiguration, deviceAddress, hayStack)
            }
        }

    }

    private fun addEquipAndPoints(
        addr: Short,
        config: ProfileConfiguration,
        nodeType: NodeType?,
        hayStack: CCUHsApi,
        equipModel: SeventyFiveFProfileDirective?,
        deviceModel: SeventyFiveFDeviceDirective?,
        deviceBuilder: DeviceBuilder
    ) {
        requireNotNull(equipModel)
        requireNotNull(deviceModel)
        val equipBuilder = ProfileEquipBuilder(hayStack)

        val equipDis =  if (BuildConfig.BUILD_TYPE.equals(CARRIER_PROD, ignoreCase = true)) {
            hayStack.siteName + "-VVT-C-" + config.nodeAddress
        }else{
            hayStack.siteName + "-DAB-" + config.nodeAddress
        }
        CcuLog.i(
            Domain.LOG_TAG,
            " buildEquipAndPoints ${model.domainName} profileType ${config.profileType}"
        )
        val equipId = equipBuilder.buildEquipAndPoints(
            config, equipModel, hayStack.site!!
                .id, equipDis
        )
        val deviceName = when (nodeType) {
            NodeType.HELIO_NODE -> "-HN-"
            else -> "-SN-"
        }
        val deviceDis = hayStack.siteName + deviceName + config.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        deviceBuilder.buildDeviceAndPoints(
            config,
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )
        dabProfile = DabProfile(addr)
    }

    private fun overrideForBypassDamper(config: DabProfileConfiguration) {
        val equip = hayStack.readEntity("equip and group == \"" + config.nodeAddress + "\"")
        val dabEquip = DabEquip(equip["id"].toString())

        dabEquip.dabProportionalKFactor.writeVal(14, hayStack.ccuUserName, 0.7, 0)
        dabEquip.dabProportionalKFactor.writeHisVal(0.7)

        dabEquip.dabIntegralKFactor.writeVal(14, hayStack.ccuUserName, 0.3, 0)
        dabEquip.dabIntegralKFactor.writeHisVal(0.3)

        dabEquip.dabTemperatureProportionalRange.writeVal(14, hayStack.ccuUserName, 1.5, 0)
        dabEquip.dabTemperatureProportionalRange.writeHisVal(1.5)
    }

    private fun setScheduleType(config: DabProfileConfiguration) {
        val scheduleTypePoint = hayStack.readEntity("point and domainName == \"" + DomainName.scheduleType + "\" and group == \"" + config.nodeAddress + "\"")
        val scheduleTypeId = scheduleTypePoint["id"].toString()

        val roomSchedule = getSchedule(zoneRef, floorRef)
        if(roomSchedule.isZoneSchedule) {
            hayStack.writeDefaultValById(scheduleTypeId, 1.0)
        } else {
            hayStack.writeDefaultValById(scheduleTypeId, 2.0)
        }
    }

    private fun setOutputTypes(
        hayStack: CCUHsApi,
        config: DabProfileConfiguration,
        deviceBuilder: DeviceBuilder,
        deviceModel: SeventyFiveFDeviceDirective
    ) {
        val deviceEntity = hayStack.readEntity("device and addr == \"" + config.nodeAddress + "\"")
        val device = Device.Builder().setHDict(hayStack.readHDictById(deviceEntity["id"].toString())).build()
        val reheatType = config.reheatType.currentVal.toInt() - 1
        val reheatCmdPoint = hayStack.readEntity("point and group == \"" + config.nodeAddress + "\" and domainName == \"" + DomainName.reheatCmd + "\"")
        val normalizedDamper1Cmd = hayStack.readEntity("point and group == \"" + config.nodeAddress + "\" and domainName == \"" + DomainName.normalizedDamper1Cmd + "\"")
        val normalizedDamper2Cmd = hayStack.readEntity("point and group == \"" + config.nodeAddress + "\" and domainName == \"" + DomainName.normalizedDamper2Cmd + "\"")
        val analog2Def = deviceModel.points.find { it.domainName == DomainName.analog2Out }
        val analog2out = getDevicePointDict(DomainName.analog2Out, deviceEntity["id"].toString(), hayStack)
        val relay1Def = deviceModel.points.find { it.domainName == DomainName.relay1 }
        val relay1 = getDevicePointDict(DomainName.relay1, deviceEntity["id"].toString(), hayStack)
        val relay2Def = deviceModel.points.find { it.domainName == DomainName.relay2 }
        val relay2 = getDevicePointDict(DomainName.relay2, deviceEntity["id"].toString(), hayStack)
        val analog1Def = deviceModel.points.find { it.domainName == DomainName.analog1Out }
        val analog1out = getDevicePointDict(DomainName.analog1Out, deviceEntity["id"].toString(), hayStack)
        val analog1InDef = deviceModel.points.find { it.domainName == DomainName.analog1In }
        val analog1In = getDevicePointDict(DomainName.analog1In, deviceEntity["id"].toString(), hayStack)
        val damperType1 = hayStack.readPointPriorityValByQuery("point and group == \"" + config.nodeAddress + "\" and domainName == \"" + DomainName.damper1Type + "\"")
        val damperType2 = hayStack.readPointPriorityValByQuery("point and group == \"" + config.nodeAddress + "\" and domainName == \"" + DomainName.damper2Type + "\"")

        val analog2OpEnabled = reheatType in listOf(
            ReheatType.ZeroToTenV.ordinal,
            ReheatType.TwoToTenV.ordinal,
            ReheatType.TenToTwov.ordinal,
            ReheatType.TenToZeroV.ordinal,
            ReheatType.Pulse.ordinal
        )
        setPortConfiguration(
            rawPoint = analog1out,
            analogType = getDamperTypeString(config.damper1Type.currentVal.toInt()),
            port = Port.ANALOG_OUT_ONE.toString(),
            portEnabled = (damperType1 != 4.0),
            pointRef = if(damperType1 != 4.0) normalizedDamper1Cmd["id"].toString() else null
        )
        deviceBuilder.updatePoint(analog1Def!!, config, device, analog1out)
        fun getAnalog2PointRef() : String? {
            if (reheatType >= 0 ) {
                if (reheatType <= ReheatType.OneStage.ordinal) {
                    return reheatCmdPoint["id"].toString()
                }
                if (damperType2.toInt() != DamperType.MAT.ordinal) {
                    return normalizedDamper2Cmd["id"].toString()
                }
            } else {
                return normalizedDamper2Cmd["id"].toString()
            }
            return null
        }

        setPortConfiguration(
            rawPoint = analog2out,
            analogType = if(reheatType == -1) getDamperTypeString(config.damper2Type.currentVal.toInt()) else getReheatTypeString(config),
            port = Port.ANALOG_OUT_TWO.toString(),
            portEnabled = if((damperType2 == 4.0 && reheatType == -1)) false else if(damperType2 == 4.0 && analog2OpEnabled) true else if(damperType2 == 4.0 && reheatType == 0) false  else if(damperType2 != 4.0) true else false,
            pointRef = getAnalog2PointRef()
        )
        deviceBuilder.updatePoint(analog2Def!!, config, device, analog2out)

        setPortConfiguration(
            rawPoint = relay1,
            analogType = null,
            port = Port.RELAY_ONE.toString(),
            portEnabled = false,
            pointRef = null
        )
        deviceBuilder.updatePoint(relay1Def!!, config, device, relay1)

        setPortConfiguration(
            rawPoint = relay2,
            analogType = null,
            port = Port.RELAY_TWO.toString(),
            portEnabled = false,
            pointRef = null
        )
        deviceBuilder.updatePoint(relay2Def!!, config, device, relay2)

        when (reheatType) {
            ReheatType.OneStage.ordinal -> {
                setPortConfiguration(
                    rawPoint = relay1,
                    analogType = "Relay N/C",
                    port = Port.RELAY_ONE.toString(),
                    portEnabled = true,
                    pointRef = reheatCmdPoint["id"].toString()
                )
                deviceBuilder.updatePoint(relay1Def!!, config, device, relay1)
            }
            ReheatType.TwoStage.ordinal -> {
                setPortConfiguration(
                    rawPoint = relay1,
                    analogType = "Relay N/C",
                    port = Port.RELAY_ONE.toString(),
                    portEnabled = true,
                    pointRef = reheatCmdPoint["id"].toString()
                )
                deviceBuilder.updatePoint(relay1Def!!, config, device, relay1)

                setPortConfiguration(
                    rawPoint = relay2,
                    analogType = "Relay N/C",
                    port = Port.RELAY_TWO.toString(),
                    portEnabled = true,
                    pointRef = reheatCmdPoint["id"].toString(),
                )
                deviceBuilder.updatePoint(relay2Def!!, config, device, relay2)
            }
        }

        analog1In["analogType"] = getDamperTypeString(config.damper1Type.currentVal.toInt())
        deviceBuilder.updatePoint(analog1InDef!!, config, device, analog1In)
    }

    private fun getDamperTypeString(currentVal : Int) : String {
        return when(currentVal) {
            0 -> "0-10v"
            1 -> "2-10v"
            2 -> "10-2v"
            3 -> "10-0v"
            4 -> LSmartNode.MAT
            5 -> "0-5v"
            else -> { "0-10v" }
        }
    }

    private fun getReheatTypeString(config: DabProfileConfiguration) : String {
        return when(config.reheatType.currentVal.toInt()) {
            1 -> "0-10v"
            2 -> "2-10v"
            3 -> "10-2v"
            4 -> "10-0v"
            5 -> LSmartNode.PULSE
            else -> { "0-10v" }
        }
    }

    private fun setPortConfiguration(
        rawPoint: HashMap<Any, Any>,
        analogType: String?,
        port: String?,
        portEnabled: Boolean,
        pointRef: String?
    ) {
        if(analogType != null) rawPoint["analogType"] = analogType else rawPoint.remove("analogType")
        if(port != null) rawPoint["port"] = port else rawPoint.remove("port")
        rawPoint["portEnabled"] = portEnabled
        if(pointRef != null) rawPoint["pointRef"] = pointRef else rawPoint.remove("pointRef")
    }

    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }
}
