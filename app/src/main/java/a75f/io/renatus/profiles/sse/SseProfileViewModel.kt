package a75f.io.renatus.profiles.sse

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Device
import a75f.io.device.mesh.LSerial
import a75f.io.device.mesh.MeshUtil
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t
import a75f.io.device.serial.MessageType
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.dab.getDevicePointDict
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.sse.SingleStageProfile
import a75f.io.logic.bo.building.sse.SseProfileConfiguration
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.getSchedule
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.TestSignalManager
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

open class SseProfileViewModel : ViewModel() {

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var nodeType: NodeType
    private var deviceAddress by Delegates.notNull<Short>()
    private lateinit var sseProfile: SingleStageProfile
    lateinit var profileConfiguration: SseProfileConfiguration

    private lateinit var model: SeventyFiveFProfileDirective
    private lateinit var deviceModel: SeventyFiveFDeviceDirective
    lateinit var viewState: SseConfigViewState

    @SuppressLint("StaticFieldLeak")
    private lateinit var context: Context
    lateinit var hayStack: CCUHsApi

    lateinit var relay1AssociationList: List<String>
    lateinit var relay2AssociationList: List<String>
    lateinit var analogIn1AssociationList: List<String>

    lateinit var temperatureOffsetsList: List<String>

    private lateinit var pairingCompleteListener: OnPairingCompleteListener
    private var saveJob : Job? = null


    fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        profileType = ProfileType.values()[bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]

        model = getProfileDomainModel()
        CcuLog.i(Domain.LOG_TAG, "SSE Profile Model Loaded")
        deviceModel = getDeviceDomainModel() as SeventyFiveFDeviceDirective
        CcuLog.i(Domain.LOG_TAG, "SSE Device Model Loaded")

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is SingleStageProfile) {
            sseProfile = L.getProfile(deviceAddress) as SingleStageProfile
            profileConfiguration = SseProfileConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, model
            ).getActiveConfiguration()

            viewState = SseConfigViewState.fromSseProfileConfig(profileConfiguration)
        } else {
            profileConfiguration = SseProfileConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, model
            ).getDefaultConfiguration()
            viewState = SseConfigViewState.fromSseProfileConfig(profileConfiguration)
        }
        initializeLists()
        CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
        this.context = context
        this.hayStack = hayStack
        CcuLog.i(Domain.LOG_TAG, "SSE Init completed.")
    }

    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }


    private fun initializeLists() {
        relay1AssociationList =
            Domain.getListOfDisNameByDomainName(DomainName.relay1OutputAssociation, model)
        relay2AssociationList =
            Domain.getListOfDisNameByDomainName(DomainName.relay2OutputAssociation, model)
        analogIn1AssociationList =
            Domain.getListOfDisNameByDomainName(DomainName.analog1InputAssociation, model)
        temperatureOffsetsList = Domain.getListByDomainName(DomainName.temperatureOffset, model)
    }

    fun sendTestCommand(relayName: String, isOn: Boolean, viewModel: SseProfileViewModel) {

        if (relayName == DomainName.relay1) {
            relayHisWrite(DomainName.relay1, if (isOn) 1.0 else 0.0)
        } else {
            relayHisWrite(DomainName.relay2, if (isOn) 1.0 else 0.0)
        }

        val msg = CcuToCmOverUsbSnControlsMessage_t()
        msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS)
        msg.smartNodeAddress.set(sseProfile.equip.group.toInt())
        msg.controls.setTemperature.set(
            (sseProfile.desiredTemp * 2).toInt().toShort()
        )
        msg.controls.digitalOut1.set((if (viewModel.viewState.testRelay1) 1 else 0).toShort())
        msg.controls.digitalOut2.set((if (viewModel.viewState.testRelay2) 1 else 0).toShort())
        MeshUtil.sendStructToCM(msg)

        if (profileConfiguration.relay1EnabledState.enabled || profileConfiguration.relay2EnabledState.enabled) {
            if (!Globals.getInstance().isTestMode) {
                Globals.getInstance().isTestMode = true
            }
        } else {
            if (Globals.getInstance().isTestMode) {
                Globals.getInstance().isTestMode = false
                TestSignalManager.restoreAllPoints()
            }
        }
    }


    private fun getProfileDomainModel(): SeventyFiveFProfileDirective {
        return if (nodeType == NodeType.SMART_NODE) {
            ModelLoader.getSmartNodeSSEModel() as SeventyFiveFProfileDirective
        } else {
            ModelLoader.getHelioNodeSSEModel() as SeventyFiveFProfileDirective
        }
    }

    private fun getDeviceDomainModel(): ModelDirective {
        return if (nodeType == NodeType.SMART_NODE) {
            ModelLoader.getSmartNodeDevice()
        } else {
            ModelLoader.getHelioNodeDevice()
        }
    }


    fun getRelayState(relayName: String): Boolean {
        if(getDeviceRef() == null) return false
        return CCUHsApi.getInstance()
            .readHisValByQuery(
                "domainName == \"" + relayName + "\" and deviceRef == \""
                        + getDeviceRef() +"\"") > 0
    }

    fun saveConfiguration() {
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving SSE Configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpSseProfile()
                CcuLog.i(Domain.LOG_TAG, "SSE Profile Setup complete")
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("SSE Configuration saved successfully", context)
                    CcuLog.i(Domain.LOG_TAG, "Close Pairing dialog")
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }

                L.saveCCUState()
                DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()
                CcuLog.i(Domain.LOG_TAG, "Send seed for $deviceAddress")
                LSerial.getInstance()
                    .sendSeedMessage(false, false, deviceAddress, zoneRef, floorRef)
                CcuLog.i(Domain.LOG_TAG, "SSE Profile Pairing complete")
                if (ProgressDialogUtils.isDialogShowing()) {
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }
            }
        }
    }

    private fun setUpSseProfile() {
        viewState.updateConfigFromViewState(profileConfiguration)

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-SSE-" + profileConfiguration.nodeAddress

        val entityMapper = EntityMapper(model)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        if (profileConfiguration.isDefault) {
            addEquipAndPoints(profileConfiguration, nodeType, hayStack, model, deviceModel)
            updatePortConfiguration(
                hayStack,
                profileConfiguration,
                DeviceBuilder(hayStack, EntityMapper(model)),
                deviceModel
            )
            CoroutineScope(Dispatchers.IO).launch {
                setScheduleType(profileConfiguration)
                UnusedPortsModel.saveUnUsedPortStatus(profileConfiguration, deviceAddress, hayStack)
            }
            L.ccu().zoneProfiles.add(sseProfile)

        } else {
            val equipId = equipBuilder.updateEquipAndPoints(
                profileConfiguration,
                model,
                hayStack.site!!.id,
                equipDis,
                true
            )
            val deviceName = when (nodeType) {
                NodeType.HELIO_NODE -> "-HN-"
                else -> "-SN-"
            }
            deviceBuilder.updateDeviceAndPoints(
                profileConfiguration,
                deviceModel,
                equipId,
                hayStack.site!!.id,
                hayStack.siteName + deviceName + profileConfiguration.nodeAddress
            )
            updatePortConfiguration(
                hayStack,
                profileConfiguration,
                DeviceBuilder(hayStack, EntityMapper(model)),
                deviceModel
            )
            CoroutineScope(Dispatchers.IO).launch {
                setScheduleType(profileConfiguration)
                UnusedPortsModel.saveUnUsedPortStatus(profileConfiguration, deviceAddress, hayStack)
            }
        }
    }

    private fun updatePortConfiguration(
        hayStack: CCUHsApi,
        config: SseProfileConfiguration,
        deviceBuilder: DeviceBuilder,
        deviceModel: SeventyFiveFDeviceDirective
    ) {
        val deviceEntityId =
            hayStack.readEntity("device and addr == \"${config.nodeAddress}\"")["id"].toString()
        val device = Device.Builder().setHDict(hayStack.readHDictById(deviceEntityId)).build()

        fun updateDevicePoint(
            domainName: String, port: String, analogType: Any,
            isPortEnabled: Boolean = false
        ) {
            val pointDef = deviceModel.points.find { it.domainName == domainName }
            pointDef?.let {
                val pointDict = getDevicePointDict(domainName, deviceEntityId, hayStack).apply {
                    this["port"] = port
                    this["analogType"] = analogType
                    this["portEnabled"] = isPortEnabled
                }
                deviceBuilder.updatePoint(it, config, device, pointDict)
            }
        }

        // Analog In 1
        if (config.analog1InEnabledState.enabled) {
            when (viewState.analog1InAssociationIndex.value) {
                0 -> updateDevicePoint(DomainName.analog1In, Port.ANALOG_IN_ONE.name, 8, true)
                1 -> updateDevicePoint(DomainName.analog1In, Port.ANALOG_IN_ONE.name, 9,true)
                else -> updateDevicePoint(DomainName.analog1In, Port.ANALOG_IN_ONE.name, 10,true)
            }
        }

        // Relay 1
        if (config.relay1EnabledState.enabled)
            updateDevicePoint(DomainName.relay1, Port.RELAY_ONE.name, "Relay N/O", true)
        else
            updateDevicePoint(DomainName.relay1, Port.RELAY_ONE.name, "Relay N/C", false)

        // Relay 2
        if (config.relay2EnabledState.enabled)
            updateDevicePoint(DomainName.relay2, Port.RELAY_TWO.name, "Relay N/O", true)
        else
            updateDevicePoint(DomainName.relay2, Port.RELAY_TWO.name, "Relay N/C", false)

        // Thermistor 1
        if (config.th1EnabledState.enabled) {
            updateDevicePoint(DomainName.th1In, Port.TH1_IN.name, 0, true)
        } else {
            updateDevicePoint(DomainName.th1In, Port.TH1_IN.name, 0, false)
        }

        // Thermistor 2
        if (config.th2EnabledState.enabled) {
            updateDevicePoint(DomainName.th2In, Port.TH2_IN.name, 0, true)
        } else {
            updateDevicePoint(DomainName.th2In, Port.TH2_IN.name, 0, false)
        }
    }

    private fun addEquipAndPoints(
        config: ProfileConfiguration,
        nodeType: NodeType?,
        hayStack: CCUHsApi,
        equipModel: SeventyFiveFProfileDirective?,
        deviceModel: SeventyFiveFDeviceDirective?
    ) {
        requireNotNull(equipModel)
        requireNotNull(deviceModel)
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-SSE-" + config.nodeAddress
        CcuLog.i(
            Domain.LOG_TAG,
            " buildEquipAndPoints ${model.domainName} profileType ${config.profileType}"
        )
        val equipId = equipBuilder.buildEquipAndPoints(
            config, equipModel, hayStack.site!!
                .id, equipDis
        )
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
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
        CcuLog.i(Domain.LOG_TAG, " add Profile")
        sseProfile = SingleStageProfile()
        sseProfile.addSSEEquip(deviceAddress, equipId)
    }

    private fun setScheduleType(config: SseProfileConfiguration) {
        val scheduleTypePoint = hayStack.readEntity("point and " +
                "domainName == \"" + DomainName.scheduleType + "\" and group == \""
                + config.nodeAddress + "\"")
        val scheduleTypeId = scheduleTypePoint["id"].toString()

        val roomSchedule = getSchedule(zoneRef, floorRef)
        if(roomSchedule.isZoneSchedule) {
            hayStack.writeDefaultValById(scheduleTypeId, 1.0)
        } else {
            hayStack.writeDefaultValById(scheduleTypeId, 2.0)
        }
    }

    private fun getDeviceRef(): String? {
        return CCUHsApi.getInstance().readId("device and addr == \"$deviceAddress\"") ?: null
    }

    private fun relayHisWrite(relayName: String, value: Double) {
        if (getDeviceRef() == null) {
            CcuLog.i(Domain.LOG_TAG, "Device not found")
        }
        val query =   "domainName == \"" + relayName + "\" and deviceRef == \"" + getDeviceRef() + "\""
        val deviceRef  = getDeviceRef()
        if(deviceRef != null) {
            val point = Point(relayName, deviceRef)
            point.writePointValue(value)
        } else{
            CcuLog.d(
                "CCU_DEVICE",
                "test-writable sse relayHisWrite writeHisValById:=======value====>$value<--query-->$query<--iswritable-->false"
            )
            CCUHsApi.getInstance()
                .writeHisValByQuery(query, value)
        }
    }
}