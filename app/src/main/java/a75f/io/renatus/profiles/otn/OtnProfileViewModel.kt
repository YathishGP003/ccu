package a75f.io.renatus.profiles.otn

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
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
import a75f.io.logic.bo.building.otn.OTNProfile
import a75f.io.logic.bo.building.otn.OtnProfileConfiguration
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

class OtnProfileViewModel : ViewModel() {
    lateinit var viewState: OtnConfigViewState

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var nodeType: NodeType
    private var deviceAddress by Delegates.notNull<Short>()

    private lateinit var profile: OTNProfile
    lateinit var profileConfiguration: OtnProfileConfiguration

    private lateinit var model : SeventyFiveFProfileDirective
    private lateinit var deviceModel : SeventyFiveFDeviceDirective
    private lateinit var context : Context
    lateinit var hayStack : CCUHsApi

    lateinit var zonePrioritiesList: List<String>
    lateinit var temperatureOffsetsList: List<String>

    private lateinit var pairingCompleteListener: OnPairingCompleteListener
    private var saveJob : Job? = null

    fun init(bundle: Bundle, context: Context, hayStack : CCUHsApi) {
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        profileType = ProfileType.values()[bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]
        CcuLog.i(
            Domain.LOG_TAG, "OtnProfileViewModel Init profileType:$profileType " +
                "nodeType:$nodeType deviceAddress:$deviceAddress")
        model = ModelLoader.getOtnTiModel() as SeventyFiveFProfileDirective
        CcuLog.i(Domain.LOG_TAG, "OtnTiModel Loaded")
        deviceModel =ModelLoader.getOtnDeviceModel() as SeventyFiveFDeviceDirective
        CcuLog.i(Domain.LOG_TAG, "OtnDevice Model Loaded")

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is OTNProfile) {
            profile = L.getProfile(deviceAddress) as OTNProfile
            profileConfiguration = OtnProfileConfiguration(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, model ).getActiveConfiguration()
        } else {
            profileConfiguration = OtnProfileConfiguration(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, model ).getDefaultConfiguration()
        }
        viewState = OtnConfigViewState.fromOtnProfileConfig(profileConfiguration)
        CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
        this.context = context
        this.hayStack = hayStack

        initializeLists()
        CcuLog.i(Domain.LOG_TAG, "Otn profile config Loaded")
    }

    private fun initializeLists() {
        zonePrioritiesList = getListByDomainName(DomainName.zonePriority, model)
        temperatureOffsetsList = getListByDomainName(DomainName.temperatureOffset, model)
    }

    fun saveConfiguration() {
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving OTN Configuration")
            CcuLog.i(Domain.LOG_TAG, "setupOtnProfile")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CcuLog.i(Domain.LOG_TAG, "resetCcuReady")
                CCUHsApi.getInstance().resetCcuReady()
                CcuLog.i(Domain.LOG_TAG, "resetCcuReady complete")
                setUpOtnProfile()
                CcuLog.i(Domain.LOG_TAG, "OtnProfile Setup complete")
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("OTN Configuration saved successfully", context)
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
                CcuLog.i(Domain.LOG_TAG, "OtnProfile Pairing complete")

                // This check is needed because the dialog sometimes fails to close inside the coroutine.
                // We don't know why this happens.
                if (ProgressDialogUtils.isDialogShowing()) {
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }
            }
        }
    }

    private fun setUpOtnProfile() {
        CcuLog.i(Domain.LOG_TAG, "setupOtnProfile")
        viewState.updateConfigFromViewState(profileConfiguration)
        CcuLog.i(Domain.LOG_TAG, "init DM")
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-OTN-" + profileConfiguration.nodeAddress

        if (profileConfiguration.isDefault) {
            addEquipAndPoints(deviceAddress, profileConfiguration, nodeType, hayStack, model, deviceModel)
            L.ccu().zoneProfiles.add(profile)
        } else {
            CcuLog.i(Domain.LOG_TAG, "updateEquipAndPoints")
            equipBuilder.updateEquipAndPoints(profileConfiguration, model, hayStack.site!!.id, equipDis, true)
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
        val equipDis = hayStack.siteName + "-OTN-" + config.nodeAddress
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
        profile = OTNProfile()
    }

    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }

}