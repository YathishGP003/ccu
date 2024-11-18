package a75f.io.renatus.profiles.oao

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Device
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.dab.getDevicePointDict
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.oao.OAOProfile
import a75f.io.logic.bo.building.oao.OAOProfileConfiguration
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.SystemConfigFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.util.ProgressDialogUtils
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class OAOViewModel : ViewModel() {
    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var nodeType: NodeType
    private var deviceAddress by Delegates.notNull<Short>()
    private lateinit var model: SeventyFiveFProfileDirective
    private lateinit var deviceModel: SeventyFiveFDeviceDirective
    private lateinit var oaoProfile: OAOProfile
    lateinit var profileConfiguration: OAOProfileConfiguration
    private lateinit var pairingCompleteListener: OnPairingCompleteListener

    lateinit var viewState: OAOViewState
    lateinit var outsideDamperMinDrivePosList: List<String>
    lateinit var outsideDamperMaxDrivePosList: List<String>
    lateinit var returnDamperMinDrivePosList: List<String>
    lateinit var returnDamperMaxDrivePosList: List<String>
    lateinit var outsideDamperMinOpenDuringRecirculationList: List<String>
    lateinit var outsideDamperMinOpenDuringConditioningList: List<String>
    lateinit var outsideDamperMinOpenDuringFanLowList: List<String>
    lateinit var outsideDamperMinOpenDuringFanMediumList: List<String>
    lateinit var outsideDamperMinOpenDuringFanHighList: List<String>

    lateinit var returnDamperMinOpenPosList: List<String>
    lateinit var exhaustFanStage1ThresholdList: List<String>
    lateinit var exhaustFanStage2ThresholdList: List<String>
    lateinit var currentTransformerTypeList: List<String>
    lateinit var co2ThresholdList: List<String>
    lateinit var exhaustFanHysteresisList: List<String>
    lateinit var systemPurgeOutsideDamperMinPosList: List<String>
    lateinit var enhancedVentilationOutsideDamperMinOpenList: List<String>

    var openCancelDialog by mutableStateOf(false)
    var nextDestination by mutableStateOf(-1)
    private lateinit var context: Context
    lateinit var hayStack: CCUHsApi

    fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        profileType = ProfileType.values()[bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]
        CcuLog.i(
            Domain.LOG_TAG,
            "OAO Init profileType:$profileType nodeType:$nodeType deviceAddress:$deviceAddress"
        )
        model = getProfileDomainModel()
        deviceModel = getDeviceDomainModel() as SeventyFiveFDeviceDirective
        this.context = context
        this.hayStack = hayStack
        if (L.ccu().oaoProfile != null) {
            oaoProfile = L.ccu().oaoProfile as OAOProfile
            profileConfiguration = OAOProfileConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, model
            ).getActiveConfiguration()
            profileConfiguration.unusedPorts["Relay1"] = getUnUsedPort(deviceAddress.toInt())
        } else {
            profileConfiguration = OAOProfileConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, model
            ).getDefaultConfiguration()
        }
        viewState = OAOViewState.fromOaoProfileConfig(profileConfiguration)
        initializeLists()
        CcuLog.i(L.TAG_CCU_DOMAIN, "OAO config: $profileConfiguration")
        CcuLog.i(Domain.LOG_TAG, "OAO Loaded")
    }

    private fun initializeLists() {
        outsideDamperMinDrivePosList =
            Domain.getListByDomainName(DomainName.outsideDamperMinDrive, model)
        outsideDamperMaxDrivePosList =
            Domain.getListByDomainName(DomainName.outsideDamperMaxDrive, model)
        returnDamperMinDrivePosList =
            Domain.getListByDomainName(DomainName.returnDamperMinDrive, model)
        returnDamperMaxDrivePosList =
            Domain.getListByDomainName(DomainName.returnDamperMaxDrive, model)
        outsideDamperMinOpenDuringRecirculationList =
            Domain.getListByDomainName(DomainName.outsideDamperMinOpenDuringRecirculation, model)
        outsideDamperMinOpenDuringConditioningList =
            Domain.getListByDomainName(DomainName.outsideDamperMinOpenDuringConditioning, model)
        outsideDamperMinOpenDuringFanLowList =
            Domain.getListByDomainName(DomainName.outsideDamperMinOpenDuringFanLow, model)
        outsideDamperMinOpenDuringFanMediumList =
            Domain.getListByDomainName(DomainName.outsideDamperMinOpenDuringFanMedium, model)
        outsideDamperMinOpenDuringFanHighList =
            Domain.getListByDomainName(DomainName.outsideDamperMinOpenDuringFanHigh, model)
        returnDamperMinOpenPosList =
            Domain.getListByDomainName(DomainName.returnDamperMinOpen, model)
        exhaustFanStage1ThresholdList =
            Domain.getListByDomainName(DomainName.exhaustFanStage1Threshold, model)
        exhaustFanStage2ThresholdList =
            Domain.getListByDomainName(DomainName.exhaustFanStage2Threshold, model)
        currentTransformerTypeList =
            Domain.getListOfDisNameByDomainName(DomainName.currentTransformerType, model)
        co2ThresholdList = Domain.getListByDomainName(DomainName.co2Threshold, model)
        exhaustFanHysteresisList =
            Domain.getListByDomainName(DomainName.exhaustFanHysteresis, model)
        systemPurgeOutsideDamperMinPosList =
            Domain.getListByDomainName(DomainName.systemPurgeOutsideDamperMinPos, model)
        enhancedVentilationOutsideDamperMinOpenList =
            Domain.getListByDomainName(DomainName.enhancedVentilationOutsideDamperMinOpen, model)

    }

    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }

    private fun getProfileDomainModel(): SeventyFiveFProfileDirective {
        return ModelLoader.getSmartNodeOAOModelDef() as SeventyFiveFProfileDirective
    }

    private fun getDeviceDomainModel(): ModelDirective {
        return ModelLoader.getSmartNodeDevice()
    }

    fun saveConfiguration() {
        viewModelScope.launch {
            ProgressDialogUtils.showProgressDialog(
                context,
                "Saving OAO Configuration"
            )
            withContext(Dispatchers.IO) {
                CCUHsApi.getInstance().resetCcuReady()

                setUpOAOProfile()
                CcuLog.i(Domain.LOG_TAG, "OAO Profile Setup complete")
                L.saveCCUState()
                saveUnMappedPort(
                    configuration = profileConfiguration,
                    profileConfiguration.unusedPorts["Relay1"]!!
                )
                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()
                CcuLog.i(Domain.LOG_TAG, "Send seed for $deviceAddress")
                LSerial.getInstance().sendOAOSeedMessage()

                CcuLog.i(Domain.LOG_TAG, "OAO Profile Pairing complete")

                withContext(Dispatchers.Main) {
                    if (ProgressDialogUtils.isDialogShowing()) {
                        ProgressDialogUtils.hideProgressDialog()
                        CcuLog.i(Domain.LOG_TAG, "Closing OAO dialog")
                        pairingCompleteListener.onPairingComplete()
                    }
                    SystemConfigFragment.SystemConfigFragmentHandler.sendEmptyMessage(6)
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("OAO Configuration saved successfully", context)
                }
            }
        }
    }

    private fun setUpOAOProfile() {
        viewState.updateConfigFromViewState(profileConfiguration)
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-OAO-" + profileConfiguration.nodeAddress
        if (profileConfiguration.isDefault) {
            addEquipAndPoints(profileConfiguration, nodeType, hayStack, model, deviceModel)
            updateDevicePoints(
                hayStack,
                profileConfiguration,
                DeviceBuilder(hayStack, EntityMapper(model)),
                deviceModel
            )
        } else {
            val equipRef = equipBuilder.updateEquipAndPoints(
                profileConfiguration,
                model,
                hayStack.site!!.id,
                equipDis,
                true
            )
            updateDevicePoints(
                hayStack,
                profileConfiguration,
                DeviceBuilder(hayStack, EntityMapper(model)),
                deviceModel,
                true
            )
            oaoProfile.addOAOEquip(
                equipRef,
                profileConfiguration.nodeAddress.toShort(),
                ProfileType.getProfileTypeForName(profileConfiguration.profileType)
            )
        }
        if (L.ccu().systemProfile.profileType != ProfileType.SYSTEM_DEFAULT) {
            L.ccu().systemProfile.setOutsideTempCoolingLockoutEnabled(CCUHsApi.getInstance(), true)
        }
        L.ccu().oaoProfile = oaoProfile
    }

    private fun addEquipAndPoints(
        profileConfiguration: OAOProfileConfiguration,
        nodeType: NodeType,
        hayStack: CCUHsApi,
        model: SeventyFiveFProfileDirective,
        deviceModel: SeventyFiveFDeviceDirective
    ) {
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-OAO-" + profileConfiguration.nodeAddress

        val equipId = equipBuilder.buildEquipAndPoints(
            profileConfiguration, model, hayStack.site!!
                .id, equipDis
        )
        val entityMapper = EntityMapper(model)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val deviceName = when (nodeType) {
            NodeType.HELIO_NODE -> "-HN-"
            else -> "-SN-"
        }
        val deviceDis = hayStack.siteName + deviceName + profileConfiguration.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        deviceBuilder.buildDeviceAndPoints(
            profileConfiguration,
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )

        CcuLog.i(Domain.LOG_TAG, "OAO Profile Added.")
        oaoProfile = OAOProfile()
        oaoProfile.addOAOEquip(
            equipId,
            profileConfiguration.nodeAddress.toShort(),
            ProfileType.getProfileTypeForName(profileConfiguration.profileType)
        )
    }

    fun unpair() {
        viewModelScope.launch {
            ProgressDialogUtils.showProgressDialog(context, "Deleting OAO Equip")
            withContext(Dispatchers.IO) {
                CCUHsApi.getInstance().resetCcuReady()

                try {
                    val oao = CCUHsApi.getInstance().readEntity("equip and oao")
                    hayStack.deleteEntityTree(oao["id"].toString())
                    val sysDevices = HSUtil.getDevices("SYSTEM") + HSUtil.getDevices("@SYSTEM")
                    val bdDevices = sysDevices.filter { d: Device ->
                        d.domainName != null && d.addr.equals(profileConfiguration.nodeAddress.toString()) && d.domainName.equals(
                            "smartnodeDevice"
                        )
                    }
                    bdDevices.forEach { bdDevice -> hayStack.deleteEntityTree(bdDevice?.id) }
                    L.ccu().oaoProfile = null
                    CcuLog.i(Domain.LOG_TAG, "OAO Equip deleted successfully")
                } catch (e: Exception) {
                    CcuLog.d(L.TAG_CCU_UI, "Exception while trying to delete OAO equip")
                    e.printStackTrace()
                }

                L.saveCCUState()

                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()

                CcuLog.i(Domain.LOG_TAG, "OAO Profile Deletion complete")
            }

            withContext(Dispatchers.Main) {
                SystemConfigFragment.SystemConfigFragmentHandler.sendEmptyMessage(6)
                ProgressDialogUtils.hideProgressDialog()
                showToast("OAO Equip deleted successfully", context)
                CcuLog.i(Domain.LOG_TAG, "Close Pairing dialog")
            }
        }
    }

    private fun getUnUsedPort(nodeAddress: Int): Boolean {
        val device = hayStack.readEntity("device and addr == \"$nodeAddress\"")
        return hayStack.readEntity(
            "point and domainName == \"relay1\" and" +
                    " deviceRef == \"" + device["id"].toString() + "\""
        ).containsKey("unused")
    }

    private fun saveUnMappedPort(configuration: OAOProfileConfiguration, enabled: Boolean) {
        val deviceId =
            hayStack.readEntity("device and addr == \"${configuration.nodeAddress}\"")["id"].toString()
        val relay1Dict =
            hayStack.readHDict("point and domainName == \"relay1\" and deviceRef == \"$deviceId\"")

        if (enabled != relay1Dict.has(Tags.UNUSED)) {
            val relay1 = RawPoint.Builder().setHDict(relay1Dict).build()
            if (enabled) {
                relay1.markers.add(Tags.WRITABLE)
                relay1.markers.add(Tags.UNUSED)
                relay1.enabled = false
            } else {
                relay1.markers.remove(Tags.WRITABLE)
                relay1.markers.remove(Tags.UNUSED)
            }
            hayStack.updatePoint(relay1, relay1Dict["id"].toString())
        }
    }


    fun updateDevicePoints(
        hayStack: CCUHsApi,
        config: OAOProfileConfiguration,
        deviceBuilder: DeviceBuilder,
        deviceModel: SeventyFiveFDeviceDirective,
        isReconfig : Boolean = false
    ) {
        val deviceEntityId =
            hayStack.readEntity("device and addr == \"${config.nodeAddress}\"")["id"].toString()
        val device = Device.Builder().setHDict(hayStack.readHDictById(deviceEntityId)).build()

        fun updateDevicePoint(domainName: String, port: String, analogType: Any) {
            val pointDef = deviceModel.points.find { it.domainName == domainName }
            pointDef?.let {
                val pointDict = getDevicePointDict(domainName, deviceEntityId, hayStack).apply {
                    this["port"] = port
                    this["analogType"] = analogType
                }
                deviceBuilder.updatePoint(it, config, device, pointDict)
            }
        }

        //Update analog input points
        updateDevicePoint(DomainName.analog1In, Port.ANALOG_IN_ONE.name, 5)
        updateDevicePoint(
            DomainName.analog2In,
            Port.ANALOG_IN_TWO.name,
            8 + config.currentTransformerType.currentVal
        )

        //Update analog output points
        updateDevicePoint(
            DomainName.analog1Out,
            Port.ANALOG_OUT_ONE.name,
            "${config.outsideDamperMinDrive.currentVal} - ${config.outsideDamperMaxDrive.currentVal}"
        )
        updateDevicePoint(
            DomainName.analog2Out,
            Port.ANALOG_OUT_TWO.name,
            "${config.returnDamperMinDrive.currentVal} - ${config.returnDamperMaxDrive.currentVal}"
        )

        // not updating below points if reconfiguring
        if(isReconfig)
            return

        //Update TH input points
        updateDevicePoint(DomainName.th1In, Port.TH1_IN.name, 0)
        updateDevicePoint(DomainName.th2In, Port.TH2_IN.name, 0)

        //Update relay points
        updateDevicePoint(
            DomainName.relay1,
            Port.RELAY_ONE.name,
            OutputRelayActuatorType.NormallyClose.displayName
        )
        updateDevicePoint(
            DomainName.relay2,
            Port.RELAY_TWO.name,
            OutputRelayActuatorType.NormallyClose.displayName
        )

    }


    private fun getSystemProfileType(): String {
        val profileType = L.ccu().systemProfile.profileType
        return when (profileType) {
            ProfileType.SYSTEM_DAB_ANALOG_RTU, ProfileType.SYSTEM_DAB_HYBRID_RTU, ProfileType.SYSTEM_DAB_STAGED_RTU, ProfileType.SYSTEM_DAB_STAGED_VFD_RTU, ProfileType.dabExternalAHUController, ProfileType.SYSTEM_DAB_ADVANCED_AHU -> "dab"
            ProfileType.SYSTEM_VAV_ANALOG_RTU, ProfileType.SYSTEM_VAV_HYBRID_RTU, ProfileType.SYSTEM_VAV_IE_RTU, ProfileType.SYSTEM_VAV_STAGED_RTU, ProfileType.SYSTEM_VAV_STAGED_VFD_RTU, ProfileType.SYSTEM_VAV_ADVANCED_AHU, ProfileType.vavExternalAHUController -> "vav"
            else -> {
                "default"
            }
        }
    }
}