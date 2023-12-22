package a75f.io.renatus.profiles.acb

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
import a75f.io.logic.bo.building.vav.VavAcbProfile
import a75f.io.logic.bo.util.DesiredTempDisplayMode
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
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
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

    private val _isDialogOpen = MutableLiveData<Boolean>()

    var modelLoaded by  mutableStateOf(false)
    val isDialogOpen: LiveData<Boolean>
        get() = _isDialogOpen

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
        } else {
            profileConfiguration = AcbProfileConfiguration(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, model).getDefaultConfiguration()

        }

        CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())

        viewState = AcbConfigViewState.fromAcbProfileConfig(profileConfiguration)

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
        valveTypesList = getListByDomainName(DomainName.valveType, model)
        zonePrioritiesList = getListByDomainName(DomainName.zonePriority, model)

        condensateSensorTypesList = getListByDomainName(DomainName.thermistor2Type, model)

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

        RxjavaUtil.executeBackgroundTask({
            ProgressDialogUtils.showProgressDialog(context, "Saving ACB Configuration")
        }, {
            CCUHsApi.getInstance().resetCcuReady()

            setUpAcbProfile()
            CcuLog.i(Domain.LOG_TAG, "VavAcbProfile Setup complete")
            L.saveCCUState()

            hayStack.syncEntityTree()
            CCUHsApi.getInstance().setCcuReady()
            CcuLog.i(Domain.LOG_TAG, "Send seed for $deviceAddress")
            LSerial.getInstance().sendSeedMessage(false,false, deviceAddress, zoneRef,floorRef)
            DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
            CcuLog.i(Domain.LOG_TAG, "VavAcbProfile Pairing complete")
        }, {
            ProgressDialogUtils.hideProgressDialog()
            _isDialogOpen.value = false
            context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
            showToast("ACB Configuration saved successfully", context)
            CcuLog.i(Domain.LOG_TAG, "Close Pairing dialog")
        })

        // TODO: Sam's original code. Some or all of this will be restored in a future cleanup operation.
        /*
        viewModelScope.launch {
            ProgressDialogUtils.showProgressDialog(context, "Saving VAV Configuration")
            withContext(Dispatchers.IO) {

                viewState.updateConfigFromViewState(profileConfiguration)
                val equipBuilder = ProfileEquipBuilder(hayStack)
                if (profileConfiguration.isDefault) {
                    equipBuilder.buildEquipAndPoints(profileConfiguration, model, hayStack.site!!.id)
                } else {
                    equipBuilder.updateEquipAndPoints(profileConfiguration, model, hayStack.site!!.id)
                }

                withContext(Dispatchers.Main) {
                    ProgressDialogUtils.hideProgressDialog()
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                }
            }
        }
         */
    }

    private fun setUpAcbProfile() {
        //DomainManager.buildDomain(CCUHsApi.getInstance())
        viewState.updateConfigFromViewState(profileConfiguration)

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-ACB-" + profileConfiguration.nodeAddress

        if (profileConfiguration.isDefault) {

            addEquipAndPoints(deviceAddress, profileConfiguration, floorRef, zoneRef, nodeType, hayStack, model, deviceModel)
            setOutputTypes(profileConfiguration)
            L.ccu().zoneProfiles.add(acbProfile)

        } else {
            updateEquipAndPoints(deviceAddress, profileConfiguration, floorRef, zoneRef, nodeType, hayStack, model, deviceModel)
            setOutputTypes(profileConfiguration)
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

    // "analogType" tag is used by control message code and cannot easily be replaced with a domain name query.
    // We are setting this value upon equip creation/reconfiguration for now.
    private fun setOutputTypes(config: AcbProfileConfiguration) {
        val device = hayStack.read("device and addr == \"" + config.nodeAddress + "\"")

        // Set
        val relay1 = hayStack.read("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay1 + "\"");
        var relay1Point = RawPoint.Builder().setHashMap(relay1)
        hayStack.updatePoint(relay1Point.setType("Relay N/C").build(), relay1.get("id").toString())

        var relay2 = hayStack.read("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay2 + "\"");
        var relay2Point = RawPoint.Builder().setHashMap(relay2)
        hayStack.updatePoint(relay2Point.setType("Relay N/C").build(), relay2.get("id").toString())

        var analogOut1 = hayStack.read("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog1Out + "\"");
        var analog1Point = RawPoint.Builder().setHashMap(analogOut1)
        hayStack.updatePoint(analog1Point.setType(getDamperTypeString(config)).build(), analogOut1.get("id").toString())

        var analogOut2 = hayStack.read("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog2Out + "\"");
        var analog2Point = RawPoint.Builder().setHashMap(analogOut2)
        hayStack.updatePoint(analog2Point.setType(getValveTypeString(config)).build(), analogOut2.get("id").toString())

    }

    // This logic will break if the "damperType" point enum is changed
    private fun getDamperTypeString(config: AcbProfileConfiguration) : String {
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
    private fun getValveTypeString(config: AcbProfileConfiguration) : String {
        return when(config.valveType.currentVal.toInt()) {
            1 -> "0-10v"
            2 -> "2-10v"
            3 -> "10-2v"
            4 -> "10-0v"
            5 -> LSmartNode.PULSE
            else -> { "0-10v" }
        }
    }

}
