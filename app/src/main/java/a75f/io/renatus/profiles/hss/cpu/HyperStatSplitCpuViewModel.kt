package a75f.io.renatus.profiles.hss.cpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.unitVentilator.HsSplitCpuEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.allStandaloneProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.getHssProfileFanLevel
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.EconSensorBusTempAssociation
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.UniversalInputs
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuAnalogControlType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuRelayType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconProfile
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.correctSensorBusTempPoints
import a75f.io.logic.bo.building.statprofiles.util.getCpuPossibleConditioningModeSettings
import a75f.io.logic.bo.building.statprofiles.util.getPossibleFanMode
import a75f.io.logic.bo.building.statprofiles.util.mapSensorBusPressureLogicalPoint
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.util.modifyConditioningMode
import a75f.io.logic.util.modifyFanMode
import a75f.io.messaging.handler.setPortConfiguration
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.R
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.hss.HyperStatSplitViewModel
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import a75f.io.renatus.util.showErrorDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class HyperStatSplitCpuViewModel : HyperStatSplitViewModel() {
    // this is used only for hssplit cpu econ profile
    var openDuplicateDialog by mutableStateOf(false)
    var openMissingDialog by mutableStateOf(false)
    var noOBRelay by mutableStateOf(false)
    var noCompressorStages by mutableStateOf(false)
    var noMatSensor by mutableStateOf(false)

    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        super.init(bundle, context, hayStack)
        openDuplicateDialog = false
        openMissingDialog = false
        openCancelDialog = false
        equipModel = ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
        deviceModel = ModelLoader.getHyperStatSplitDeviceModel() as SeventyFiveFDeviceDirective
        viewState = mutableStateOf(HyperStatSplitCpuState())
        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is HyperStatSplitCpuEconProfile) {
            hssProfile = L.getProfile(deviceAddress) as HyperStatSplitCpuEconProfile
            profileConfiguration = HyperStatSplitCpuConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, equipModel
            ).getActiveConfiguration()
            equipRef = profileConfiguration.equipId
        } else {
            profileConfiguration = HyperStatSplitCpuConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, equipModel
            ).getDefaultConfiguration()
        }

        viewState.value = HyperStatSplitCpuState.fromProfileConfigToState(profileConfiguration as HyperStatSplitCpuConfiguration)

        this.context = context
        this.hayStack = hayStack

        initializeLists()
        isCopiedConfigurationAvailable()
        CcuLog.i(Domain.LOG_TAG, "HSS initialized")
    }

    override fun saveConfiguration() {
        if (validateProfileConfig()) {
            if(viewState.value.pressureSensorAddress0.enabled && viewState.value.sensorAddress0.enabled){
                showErrorDialog(context,Html.fromHtml(context.getString(R.string.pressure_sensor_should_be_mapped), Html.FROM_HTML_MODE_LEGACY))
                return
            }
            if (saveJob == null) {
                ProgressDialogUtils.showProgressDialog(context, context.getString(R.string.saving_hss_config))
                saveJob = viewModelScope.launch(highPriorityDispatcher) {
                    CCUHsApi.getInstance().resetCcuReady()
                    setUpHyperStatSplitProfile()
                    L.saveCCUState()
                    hayStack.syncEntityTree()
                    CCUHsApi.getInstance().setCcuReady()
                    withContext(Dispatchers.Main) {
                        context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                        showToast(context.getString(R.string.hss_config_saved), context)
                        ProgressDialogUtils.hideProgressDialog()
                        pairingCompleteListener.onPairingComplete()
                    }
                    CcuLog.i(Domain.LOG_TAG, "Send seed for $deviceAddress")
                    LSerial.getInstance()
                        .sendHyperSplitSeedMessage(deviceAddress, zoneRef, floorRef)
                    // This check is needed because the dialog sometimes fails to close inside the coroutine.
                    // We don't know why this happens.
                    if (ProgressDialogUtils.isDialogShowing()) {
                        ProgressDialogUtils.hideProgressDialog()
                        pairingCompleteListener.onPairingComplete()
                    }
                }
            }
        }

    }

    /*
        It is still possible to configure an equip in a way that leads to irregular behavior of widgets (e.g. duplicated monitoring-only points).
        We are only validating to prevent configs that will mess with algo operation.
     */
    private fun validateProfileConfig(): Boolean {


        val isCompressorMapped = (isCompressorMappedWithAnyRelay() || isCompressorAOEnabled(
            CpuAnalogControlType.COMPRESSOR_SPEED.ordinal))
        val isChangeOverIsMapped =
            (isAnyRelayMappedToControl(CpuRelayType.CHANGE_OVER_B_HEATING) || isAnyRelayMappedToControl(
                CpuRelayType.CHANGE_OVER_O_COOLING
            ))

        if (isCompressorMapped && !isChangeOverIsMapped) {
            noOBRelay = true
            return false
        }

        if (isChangeOverIsMapped && !isCompressorMapped) {
            noCompressorStages = true
            return false
        }

        if (viewState.value.enableOutsideAirOptimization && (
                !isAnyAnalogMappedToControl(CpuAnalogControlType.OAO_DAMPER.ordinal) ||
                !(isAnyUniversalInMapped(UniversalInputs.OUTSIDE_AIR_TEMPERATURE)  || isAnySensorBusMapped(
                    EconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY)) ||
                !(isAnyUniversalInMapped(UniversalInputs.MIXED_AIR_TEMPERATURE) || isAnySensorBusMapped(EconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY))
            )
        ) {
            openMissingDialog = true
            return false
        }

        if (isUniversalInDuplicated(UniversalInputs.OUTSIDE_AIR_TEMPERATURE) ||
            isSensorBusDuplicated(EconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(UniversalInputs.OUTSIDE_AIR_TEMPERATURE) && isAnySensorBusMapped(EconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY))
        ) {
            openDuplicateDialog = true
            return false
        }

        if (isUniversalInDuplicated(UniversalInputs.MIXED_AIR_TEMPERATURE) ||
            isSensorBusDuplicated(EconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(UniversalInputs.MIXED_AIR_TEMPERATURE) && isAnySensorBusMapped(EconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY))
        ) {
            openDuplicateDialog = true
            return false
        }

        if (isUniversalInDuplicated(UniversalInputs.CONDENSATE_STATUS_NO) ||
            isUniversalInDuplicated(UniversalInputs.CONDENSATE_STATUS_NC) ||
            (isAnyUniversalInMapped(UniversalInputs.CONDENSATE_STATUS_NO) && isAnyUniversalInMapped(UniversalInputs.CONDENSATE_STATUS_NC))
        ) {
            openDuplicateDialog = true
            return false
        }

        if (isUniversalInDuplicated(UniversalInputs.SUPPLY_AIR_TEMPERATURE) ||
            isSensorBusDuplicated(EconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(UniversalInputs.SUPPLY_AIR_TEMPERATURE) && isAnySensorBusMapped(EconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY))
        ) {
            openDuplicateDialog = true
            return false
        }
        
        if (isUniversalInDuplicated(UniversalInputs.FILTER_STATUS_NO) ||
            isUniversalInDuplicated(UniversalInputs.FILTER_STATUS_NC) ||
            (isAnyUniversalInMapped(UniversalInputs.FILTER_STATUS_NO) && isAnyUniversalInMapped(UniversalInputs.FILTER_STATUS_NC))
        ) {
            openDuplicateDialog = true
            return false
        }

        if (isUniversalInDuplicated(UniversalInputs.GENERIC_ALARM_NO) ||
            isUniversalInDuplicated(UniversalInputs.GENERIC_ALARM_NC) ||
            (isAnyUniversalInMapped(UniversalInputs.GENERIC_ALARM_NO) && isAnyUniversalInMapped(UniversalInputs.GENERIC_ALARM_NC))
        ) {
            openDuplicateDialog = true
            return false
        }

        if (isUniversalInDuplicated(UniversalInputs.CURRENT_TX_10) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_20) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_30) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_50) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_60) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_100) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_120) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_150) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_200)
        ) {
            openDuplicateDialog = true
            return false
        }

        if (isAnyAnalogMappedToControl(CpuAnalogControlType.DCV_MODULATING_DAMPER.ordinal)) {
            if (!(isAnySensorBusMapped(EconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY)
                        || isAnyUniversalInMapped(UniversalInputs.MIXED_AIR_TEMPERATURE))) {
                noMatSensor = true
                return false
            }
        }

        return true
    }

    fun cancelConfirm() {
        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is HyperStatSplitCpuEconProfile) {
            hssProfile = L.getProfile(deviceAddress) as HyperStatSplitCpuEconProfile
            profileConfiguration = HyperStatSplitCpuConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, equipModel
            ).getActiveConfiguration()
        } else {
            profileConfiguration = HyperStatSplitCpuConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, equipModel
            ).getDefaultConfiguration()
        }

        viewState.value = HyperStatSplitCpuState.fromProfileConfigToState(profileConfiguration as HyperStatSplitCpuConfiguration)

        openCancelDialog = false
    }

    private fun setUpHyperStatSplitProfile() {
        (viewState.value as HyperStatSplitCpuState).updateConfigFromViewState(profileConfiguration as HyperStatSplitCpuConfiguration)
        profileConfiguration.nodeType = nodeType.name
        profileConfiguration.nodeAddress = deviceAddress.toInt()
        profileConfiguration.priority = ZonePriority.NONE.ordinal

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipId: String
        if (profileConfiguration.isDefault) {
            equipId = addEquipment(profileConfiguration, equipModel, deviceModel)
            hssProfile = HyperStatSplitCpuEconProfile(equipId, profileConfiguration.nodeAddress.toShort())
            L.ccu().zoneProfiles.add(hssProfile)
            val equip = HsSplitCpuEquip(equipId)
            setConditioningMode(equip)
            updateFanMode(false, equip,getHssProfileFanLevel(equip))
        } else {
            equipId = equipBuilder.updateEquipAndPoints(profileConfiguration, equipModel, hayStack.site!!.id, getEquipDis(), true)
            val entityMapper = EntityMapper(equipModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            CcuLog.i(Domain.LOG_TAG, " updateDeviceAndPoints")
            deviceBuilder.updateDeviceAndPoints(profileConfiguration, deviceModel, equipId, hayStack.site!!.id, getDeviceDis())
            val equip = HsSplitCpuEquip(equipId)
            updateConditioningMode(equip)
            updateFanMode(true, equip, getHssProfileFanLevel(equip))
        }
        CoroutineScope(highPriorityDispatcher).launch {
            runBlocking {
                correctSensorBusTempPoints(profileConfiguration)
                mapSensorBusPressureLogicalPoint(profileConfiguration, equipId)
            }
        }
        val hssEquip = HsSplitCpuEquip(equipId)
        profileConfiguration.updateConditioningMode(equipId)
        profileConfiguration.apply { setPortConfiguration(profileConfiguration.nodeAddress, getRelayMap(), getAnalogMap())  }
        val possibleConditioningMode = getCpuPossibleConditioningModeSettings(profileConfiguration as HyperStatSplitCpuConfiguration)
        val possibleFanMode = getPossibleFanMode(hssEquip)
        modifyFanMode(possibleFanMode.ordinal,hssEquip.fanOpMode)
        modifyConditioningMode(possibleConditioningMode.ordinal, hssEquip.conditioningMode, allStandaloneProfileConditions)
        DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
    }

    private fun updateConditioningMode(equip: HsSplitCpuEquip) {
        val currentMode = equip.conditioningMode.readPriorityVal().toInt()
        val possible = getCpuPossibleConditioningModeSettings(profileConfiguration as HyperStatSplitCpuConfiguration)
        val offValue = StandaloneConditioningMode.OFF.ordinal.toDouble()
        if (possible == PossibleConditioningMode.OFF ||
            (currentMode == StandaloneConditioningMode.AUTO.ordinal &&
                    (possible == PossibleConditioningMode.HEATONLY || possible == PossibleConditioningMode.COOLONLY)) ||
            (currentMode == StandaloneConditioningMode.HEAT_ONLY.ordinal && possible == PossibleConditioningMode.COOLONLY) ||
            (currentMode == StandaloneConditioningMode.COOL_ONLY.ordinal && possible == PossibleConditioningMode.HEATONLY)
        ) {
            equip.conditioningMode.writePointValue(offValue)
        }
    }

    private fun setConditioningMode(equip: HsSplitCpuEquip) {
        val possible = getCpuPossibleConditioningModeSettings(profileConfiguration as HyperStatSplitCpuConfiguration)
        val newMode = when (possible) {
            PossibleConditioningMode.BOTH -> StandaloneConditioningMode.AUTO
            PossibleConditioningMode.HEATONLY -> StandaloneConditioningMode.HEAT_ONLY
            PossibleConditioningMode.COOLONLY -> StandaloneConditioningMode.COOL_ONLY
            else -> StandaloneConditioningMode.OFF
        }
        equip.conditioningMode.writePointValue(newMode.ordinal.toDouble())
    }

    override fun hasUnsavedChanges() : Boolean {
        return try {
            !HyperStatSplitCpuState.fromProfileConfigToState(profileConfiguration as HyperStatSplitCpuConfiguration).equalsViewState(viewState.value as HyperStatSplitCpuState)
        } catch (e: Exception) {
            false
        }
    }

}