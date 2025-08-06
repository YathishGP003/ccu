package a75f.io.renatus.profiles.hss.cpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.equips.unitVentilator.HsSplitCpuEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.allStandaloneProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getPossibleConditioningModeSettings
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getSplitPossibleFanModeSettings
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.CpuEconSensorBusTempAssociation
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.UniversalInputs
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuAnalogControlType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuRelayType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconProfile
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.getSchedule
import a75f.io.logic.util.modifyConditioningMode
import a75f.io.logic.util.modifyFanMode
import a75f.io.messaging.handler.HyperstatSplitReconfigurationHandler.Companion.correctSensorBusTempPoints
import a75f.io.messaging.handler.HyperstatSplitReconfigurationHandler.Companion.handleNonDefaultConditioningMode
import a75f.io.messaging.handler.HyperstatSplitReconfigurationHandler.Companion.handleNonDefaultFanMode
import a75f.io.messaging.handler.HyperstatSplitReconfigurationHandler.Companion.initializePrePurgeStatus
import a75f.io.messaging.handler.HyperstatSplitReconfigurationHandler.Companion.mapSensorBusPressureLogicalPoint
import a75f.io.messaging.handler.HyperstatSplitReconfigurationHandler.Companion.setOutputTypes
import a75f.io.renatus.FloorPlanFragment
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
                showErrorDialog(context,Html.fromHtml("The profile must not have <b>Pressure Sensor or Sensor Address 0</b> mapped. Please remove the mapping from the Sensor Bus or Pressure Sensor.", Html.FROM_HTML_MODE_LEGACY))
                return
            }
            if (saveJob == null) {
                ProgressDialogUtils.showProgressDialog(context, "Saving HyperStat Split Configuration")
                saveJob = viewModelScope.launch(highPriorityDispatcher) {
                    CCUHsApi.getInstance().resetCcuReady()
                    setUpHyperStatSplitProfile()
                    CcuLog.i(Domain.LOG_TAG, "HSS Profile Setup complete")
                    L.saveCCUState()
                    hayStack.syncEntityTree()
                    CCUHsApi.getInstance().setCcuReady()
                    CcuLog.i(Domain.LOG_TAG, "HSS Profile Pairing complete")
                    withContext(Dispatchers.Main) {
                        context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                        showToast("HSS Configuration saved successfully", context)
                        CcuLog.i(Domain.LOG_TAG, "Close Pairing dialog")
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
                    CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY)) ||
                !(isAnyUniversalInMapped(UniversalInputs.MIXED_AIR_TEMPERATURE) || isAnySensorBusMapped(CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY))
            )
        ) {
            openMissingDialog = true
            return false
        }

        if (isUniversalInDuplicated(UniversalInputs.OUTSIDE_AIR_TEMPERATURE) ||
            isSensorBusDuplicated(CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(UniversalInputs.OUTSIDE_AIR_TEMPERATURE) && isAnySensorBusMapped(CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY))
        ) {
            openDuplicateDialog = true
            return false
        }

        if (isUniversalInDuplicated(UniversalInputs.MIXED_AIR_TEMPERATURE) ||
            isSensorBusDuplicated(CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(UniversalInputs.MIXED_AIR_TEMPERATURE) && isAnySensorBusMapped(CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY))
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
            isSensorBusDuplicated(CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(UniversalInputs.SUPPLY_AIR_TEMPERATURE) && isAnySensorBusMapped(CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY))
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
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_200) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200))
        ) {
            openDuplicateDialog = true
            return false
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

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-cpuecon-" + profileConfiguration.nodeAddress
        val equipId: String
        if (profileConfiguration.isDefault) {

            equipId = addEquipAndPoints(
                deviceAddress,
                profileConfiguration,
                hayStack,
                equipModel,
                deviceModel
            )

            handleNonDefaultConditioningMode(
                profileConfiguration as HyperStatSplitCpuConfiguration,
                hayStack
            )

            handleNonDefaultFanMode(
                profileConfiguration as HyperStatSplitCpuConfiguration,
                hayStack
            )

            CoroutineScope(highPriorityDispatcher).launch {
                runBlocking {

                    correctSensorBusTempPoints(
                        profileConfiguration,
                        hayStack
                    )
                    mapSensorBusPressureLogicalPoint(
                        profileConfiguration,
                        equipId,
                        hayStack
                    )
                    setOutputTypes(
                        profileConfiguration,
                        hayStack
                    )
                    setScheduleType(profileConfiguration)

                    initializePrePurgeStatus(
                        profileConfiguration,
                        hayStack,
                        1.0
                    )
                    L.ccu().zoneProfiles.add(hssProfile)
                }
            }

        } else {
            equipId = equipBuilder.updateEquipAndPoints(profileConfiguration, equipModel, hayStack.site!!.id, equipDis, true)
            val entityMapper = EntityMapper(equipModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            val deviceDis = hayStack.siteName +  "-HSS-" + profileConfiguration.nodeAddress
            CcuLog.i(Domain.LOG_TAG, " updateDeviceAndPoints")
            deviceBuilder.updateDeviceAndPoints(
                profileConfiguration,
                deviceModel,
                equipId,
                hayStack.site!!.id,
                deviceDis
            )

            handleNonDefaultConditioningMode(
                profileConfiguration as HyperStatSplitCpuConfiguration,
                hayStack
            )

            handleNonDefaultFanMode(
                profileConfiguration as HyperStatSplitCpuConfiguration,
                hayStack
            )

            CoroutineScope(highPriorityDispatcher).launch {
                runBlocking {

                    correctSensorBusTempPoints(
                        profileConfiguration,
                        hayStack
                    )
                    mapSensorBusPressureLogicalPoint(
                        profileConfiguration,
                        equipId,
                        hayStack
                    )
                    setOutputTypes(
                        profileConfiguration,
                        hayStack
                    )
                    setScheduleType(profileConfiguration)
                    initializePrePurgeStatus(
                        profileConfiguration,
                        hayStack,
                        1.0
                    )
                }
            }
        }

        val possibleConditioningMode = getPossibleConditioningModeSettings(profileConfiguration as HyperStatSplitCpuConfiguration)
        val possibleFanMode = getSplitPossibleFanModeSettings(profileConfiguration.nodeAddress)
        val hssEquip = HsSplitCpuEquip(equipId)
        modifyFanMode(possibleFanMode.ordinal,hssEquip.fanOpMode)
        modifyConditioningMode(possibleConditioningMode.ordinal, hssEquip.conditioningMode, allStandaloneProfileConditions)
        DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
    }

    private fun addEquipAndPoints(
        addr: Short,
        config: ProfileConfiguration,
        hayStack: CCUHsApi,
        equipModel: SeventyFiveFProfileDirective?,
        deviceModel: SeventyFiveFDeviceDirective?
    ) : String {
        requireNotNull(equipModel)
        requireNotNull(deviceModel)
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-cpuecon-" + config.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " buildEquipAndPoints ${equipModel.domainName} profileType ${config.profileType}" )
        val equipId = equipBuilder.buildEquipAndPoints(
            config, equipModel, hayStack.site!!
                .id, equipDis
        )
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val deviceDis = hayStack.siteName +  "-HSS-" + config.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        deviceBuilder.buildDeviceAndPoints(
            config,
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )
        CcuLog.i(Domain.LOG_TAG, " add Profile")
        hssProfile = HyperStatSplitCpuEconProfile(equipId, addr)
        return equipId

    }

    override fun hasUnsavedChanges() : Boolean {
        return try {
            !HyperStatSplitCpuState.fromProfileConfigToState(profileConfiguration as HyperStatSplitCpuConfiguration).equalsViewState(viewState.value as HyperStatSplitCpuState)
        } catch (e: Exception) {
            false
        }
    }

}