package a75f.io.renatus.profiles.ti


import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.getListByDomainName
import a75f.io.domain.api.DomainName
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.caz.configs.TIConfiguration
import a75f.io.logic.bo.building.ccu.TIProfile
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu
import a75f.io.logic.bo.building.system.util.getAdvancedAhuSystemEquip
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.R
import a75f.io.renatus.modbus.util.ALERT
import a75f.io.renatus.modbus.util.OK
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates


class TIViewModel: ViewModel() {

    lateinit var roomTempList: List<String>
    lateinit var supplyAirTempList: List<String>
    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var nodeType: NodeType
    lateinit var profileConfiguration: TIConfiguration
    lateinit var context: Context
    lateinit var hayStack: CCUHsApi
    lateinit var equipModel: SeventyFiveFProfileDirective
    lateinit var deviceModel: SeventyFiveFDeviceDirective

    lateinit var pairingCompleteListener: OnPairingCompleteListener
    lateinit var tiProfile: TIProfile
    protected var deviceAddress by Delegates.notNull<Short>()
    lateinit var zonePrioritiesList: List<String>
    lateinit var temperatureOffsetsList: List<String>

    var equipRef: String? = null
    private var saveJob : Job? = null
    lateinit var viewState : TIViewState
    private lateinit var profile: TIProfile

    fun init(requireArguments: Bundle, requireContext: Context, instance: CCUHsApi) {
        deviceAddress = requireArguments.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = requireArguments.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = requireArguments.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        profileType = ProfileType.values()[requireArguments.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]
        nodeType = NodeType.values()[requireArguments.getInt(FragmentCommonBundleArgs.NODE_TYPE)]

        equipModel = ModelLoader.getTIModel() as SeventyFiveFProfileDirective
        deviceModel = ModelLoader.getTIDeviceModel() as SeventyFiveFDeviceDirective


        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is TIProfile) {
            tiProfile = L.getProfile(deviceAddress) as TIProfile
            profileConfiguration = TIConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, equipModel
            ).getActiveConfiguration()
        } else {
            profileConfiguration = TIConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, equipModel
            ).getDefaultConfiguration()
        }

        viewState = TIViewState.fromTIProfileConfig(profileConfiguration)
        CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
        this.context = requireContext
        this.hayStack = instance

        initializeLists()
        CcuLog.i(Domain.LOG_TAG, "TI initialized")
    }

    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener

    }

    private fun initializeLists() {
        zonePrioritiesList = getListByDomainName(DomainName.zonePriority, equipModel)
        temperatureOffsetsList = getListByDomainName(DomainName.temperatureOffset, equipModel)
        roomTempList =getAllowedValues(DomainName.roomTemperatureType, equipModel)
        supplyAirTempList = getAllowedValues(DomainName.supplyAirTempType, equipModel)

    }
    fun getAllowedValues(domainName: String, model: SeventyFiveFProfileDirective): List<String> {
        val pointDef = getPointByDomainName(model, domainName) ?: return emptyList()
        return if (pointDef.valueConstraint.constraintType == Constraint.ConstraintType.MULTI_STATE) {
            val constraint = pointDef.valueConstraint as MultiStateConstraint
            constraint.allowedValues.map { "${it.dis}" }
        } else {
            emptyList()
        }
    }

    private fun getPointByDomainName(
        modelDefinition: SeventyFiveFProfileDirective, domainName: String
    ): SeventyFiveFProfilePointDef? {
        return modelDefinition.points.find { (it.domainName.contentEquals(domainName)) }
    }
    fun showErrorDialog(context: Context, message: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(ALERT)
        builder.setIcon(R.drawable.ic_warning)
        builder.setMessage(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY))
        builder.setCancelable(false)
        builder.setPositiveButton(OK) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun isValidConfig(): Boolean {
        var isTh1Used = false
        var isTh2Used = false
        var isSensorBusIsUsed = false

        if (L.ccu().systemProfile is DabAdvancedAhu || L.ccu().systemProfile is VavAdvancedAhu) {
            val ahuConfig = getAdvancedAhuSystemEquip()
            isTh1Used = ahuConfig.thermistor1InputEnable.readDefaultVal() > 0
            isTh2Used = ahuConfig.thermistor2InputEnable.readDefaultVal() > 0
            isSensorBusIsUsed = (ahuConfig.temperatureSensorBusAdd0.readDefaultVal() > 0 ||
                    ahuConfig.temperatureSensorBusAdd1.readDefaultVal() > 0 ||
                    ahuConfig.temperatureSensorBusAdd2.readDefaultVal() > 0 ||
                    ahuConfig.temperatureSensorBusAdd3.readDefaultVal() > 0)
        }

        fun isTiMapped(mapping: Int) = (viewState.roomTemperatureType.toInt() == mapping ||  viewState.supplyAirTemperatureType.toInt() == mapping)

        if (isSensorBusIsUsed && viewState.roomTemperatureType.toInt() == 0) {
            showErrorDialog(context, "Sensor bus is already used in system profile")
            return false
        }

        if (isTh1Used && isTh2Used && (isTiMapped(1) || isTiMapped(2))) {
            showErrorDialog(context, "Both thermistors are used in system profile")
            return false
        }
        if (isTh1Used && isTiMapped(1)) {
            showErrorDialog(context, "Thermistor 1 is already used in system profile")
            return false
        }

        if (isTh2Used && isTiMapped(2)) {
            showErrorDialog(context, "Thermistor 2 is already used in system profile")
            return false
        }
        return true
    }


    fun saveConfiguration() {
        if (!isValidConfig()) {
            return
        }
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving Configuration")

            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                setUpProfile()
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("Configuration saved successfully", context)
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }
                L.saveCCUState()
                hayStack.syncEntityTree()

                CcuLog.i(Domain.LOG_TAG, "Profile Pairing complete")
                if (ProgressDialogUtils.isDialogShowing()) {
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }
            }
        }
    }

    private fun setUpProfile() {
        CcuLog.i(Domain.LOG_TAG, "setupProfile")
        viewState.updateConfigFromViewState(profileConfiguration)
        CcuLog.i(Domain.LOG_TAG, "init DM")
        val equipBuilder = ProfileEquipBuilder(hayStack)

        val equipDis = hayStack.siteName + "-" + equipModel.name + "-" + profileConfiguration.nodeAddress
        val deviceDis = hayStack.siteName + "-" + deviceModel.name + "-" + profileConfiguration.nodeAddress

        if (profileConfiguration.isDefault) {
            addEquipAndPoints(deviceAddress, profileConfiguration, hayStack, equipModel, deviceModel)
            L.ccu().zoneProfiles.add(profile)
        } else {
            CcuLog.i(Domain.LOG_TAG, "updateEquipAndPoints")
            val equipId = equipBuilder.updateEquipAndPoints(profileConfiguration, equipModel, hayStack.site!!.id, equipDis, true)
            val entityMapper = EntityMapper(equipModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            val deviceId = deviceBuilder.updateDeviceAndPoints(profileConfiguration, deviceModel, equipId, hayStack.site!!.id, deviceDis)
            profileConfiguration.updatePhysicalPointRef(equipId, deviceId)
        }

    }
    private fun addEquipAndPoints(
            deviceAddress: Short,
            config: TIConfiguration,
            hayStack: CCUHsApi,
            equipModel: SeventyFiveFProfileDirective,
            deviceModel: SeventyFiveFDeviceDirective
    ) {

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-" + equipModel.name + "-" + config.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " buildEquipAndPoints ${equipModel.domainName} profileType ${config.profileType}" )
        val equipId = equipBuilder.buildEquipAndPoints(
            config, equipModel, hayStack.site!!
                .id, equipDis
        )
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val deviceDis = hayStack.siteName + "-" + deviceModel.name + "-" + config.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        val deviceId = deviceBuilder.buildDeviceAndPoints(
            config,
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )
        CcuLog.i(Domain.LOG_TAG, " add Profile")
        profile = TIProfile(equipId, deviceAddress)
        profileConfiguration.updatePhysicalPointRef(equipId, deviceId)
    }
}