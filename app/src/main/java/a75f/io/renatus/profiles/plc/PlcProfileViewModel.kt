package a75f.io.renatus.profiles.plc

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.getListByDomainName
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader.getHelioNodePidModel
import a75f.io.domain.util.ModelLoader.getSmartNodeDevice
import a75f.io.domain.util.ModelLoader.getSmartNodePidModel
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.plc.PlcProfile
import a75f.io.logic.bo.building.plc.PlcProfileConfig
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class PlcProfileViewModel : ViewModel() {

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var nodeType: NodeType
    private var deviceAddress by Delegates.notNull<Short>()

    private lateinit var plcProfile: PlcProfile
    lateinit var profileConfiguration: PlcProfileConfig

    private lateinit var model : SeventyFiveFProfileDirective
    private lateinit var deviceModel : SeventyFiveFDeviceDirective
    lateinit var viewState: PlcProfileViewState

    private lateinit var context : Context
    lateinit var hayStack : CCUHsApi

    lateinit var analog1InputType: List<String>
    lateinit var pidTargetValue: List<String>
    lateinit var thermistor1InputType: List<String>
    lateinit var pidProportionalRange: List<String>
    lateinit var nativeSensorType: List<String>
    lateinit var analog2InputType: List<String>
    lateinit var setpointSensorOffset: List<String>

    lateinit var analog1MinOutput: List<String>
    lateinit var analog1MaxOutput: List<String>
    lateinit var relay1OnThreshold: List<String>
    lateinit var relay1OffThreshold: List<String>
    lateinit var relay2OnThreshold: List<String>
    lateinit var relay2OffThreshold: List<String>

    private lateinit var pairingCompleteListener: OnPairingCompleteListener
    private var saveJob : Job? = null

    fun init(bundle: Bundle, context: Context, hayStack : CCUHsApi) {
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        profileType = ProfileType.values()[bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]
        CcuLog.i(
            Domain.LOG_TAG, "PlcProfileViewModel Init profileType:$profileType " +
                "nodeType:$nodeType deviceAddress:$deviceAddress")
        model = getProfileDomainModel()
        CcuLog.i(Domain.LOG_TAG, "PlcProfileViewModel EquipModel Loaded")
        deviceModel = getSmartNodeDevice() as SeventyFiveFDeviceDirective
        CcuLog.i(Domain.LOG_TAG, "PlcProfileViewModel Device Model Loaded")

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is PlcProfile) {
            plcProfile = L.getProfile(deviceAddress) as PlcProfile
            profileConfiguration = PlcProfileConfig(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, model ).getActiveConfiguration()
            viewState = PlcProfileViewState.fromPlcProfileConfig(profileConfiguration)
        } else {
            profileConfiguration = PlcProfileConfig(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, model ).getDefaultConfiguration()
            viewState = PlcProfileViewState.fromPlcProfileConfig(profileConfiguration)
        }
        CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
        this.context = context
        this.hayStack = hayStack

        initializeLists()
        CcuLog.i(Domain.LOG_TAG, "Plc profile cofig Loaded")
    }

    private fun initializeLists() {
        analog1InputType = getListByDomainName(DomainName.analog1InputType, model)
        pidTargetValue = getListByDomainName(DomainName.pidTargetValue, model)
        thermistor1InputType = getListByDomainName(DomainName.thermistor1InputType, model)
        pidProportionalRange = getListByDomainName(DomainName.pidProportionalRange, model)
        nativeSensorType = getListByDomainName(DomainName.nativeSensorType, model)

        analog2InputType = getListByDomainName(DomainName.analog2InputType, model)
        setpointSensorOffset = getListByDomainName(DomainName.setpointSensorOffset, model)
        analog1MinOutput = getListByDomainName(DomainName.analog1MinOutput, model)
        analog1MaxOutput = getListByDomainName(DomainName.analog1MaxOutput, model)
        relay1OnThreshold = getListByDomainName(DomainName.relay1OnThreshold, model)
        relay1OffThreshold = getListByDomainName(DomainName.relay1OffThreshold, model)
        relay2OnThreshold = getListByDomainName(DomainName.relay2OnThreshold, model)
        relay2OffThreshold = getListByDomainName(DomainName.relay2OffThreshold, model)
    }

    fun saveConfiguration() {
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving Plc Configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpPlcProfile()
                CcuLog.i(Domain.LOG_TAG, "PlcProfile Setup complete")
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("Plc Configuration saved successfully", context)
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
                CcuLog.i(Domain.LOG_TAG, "PlcProfile Pairing complete")

                // This check is needed because the dialog sometimes fails to close inside the coroutine.
                // We don't know why this happens.
                if (ProgressDialogUtils.isDialogShowing()) {
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }
            }
        }
    }

    private fun setUpPlcProfile() {
        viewState.updateConfigFromViewState(profileConfiguration)

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-PID-" + profileConfiguration.nodeAddress

        if (profileConfiguration.isDefault) {

            addEquipAndPoints(deviceAddress, profileConfiguration, nodeType, hayStack, model, deviceModel)
            //vavProfile.init()
            L.ccu().zoneProfiles.add(plcProfile)

        } else {
            equipBuilder.updateEquipAndPoints(profileConfiguration, model, hayStack.site!!.id, equipDis, true)
            /*CoroutineScope(Dispatchers.IO).launch {
                saveUnUsedPortStatus(profileConfiguration, deviceAddress, hayStack)
            }*/

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
        val equipDis = hayStack.siteName + "-PID-" + config.nodeAddress
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
        plcProfile = PlcProfile(deviceAddress)

    }

    private fun getProfileDomainModel() : SeventyFiveFProfileDirective{
        return if (nodeType == NodeType.SMART_NODE) {
             getSmartNodePidModel() as SeventyFiveFProfileDirective
        } else {
            getHelioNodePidModel() as SeventyFiveFProfileDirective
        }
    }

    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }

}