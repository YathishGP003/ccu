package a75f.io.renatus.profiles.hss.cpu

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
import a75f.io.domain.util.allStandaloneProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getPossibleConditioningModeSettings
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getSplitPossibleFanModeSettings
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuControlType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuEconSensorBusTempAssociation
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuRelayType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuUniInType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconProfile
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.getSchedule
import a75f.io.messaging.handler.HyperstatSplitReconfigurationHandler.Companion.correctSensorBusTempPoints
import a75f.io.messaging.handler.HyperstatSplitReconfigurationHandler.Companion.handleNonDefaultConditioningMode
import a75f.io.messaging.handler.HyperstatSplitReconfigurationHandler.Companion.handleNonDefaultFanMode
import a75f.io.messaging.handler.HyperstatSplitReconfigurationHandler.Companion.initializePrePurgeStatus
import a75f.io.messaging.handler.HyperstatSplitReconfigurationHandler.Companion.mapSensorBusPressureLogicalPoint
import a75f.io.messaging.handler.HyperstatSplitReconfigurationHandler.Companion.setOutputTypes
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.hss.ConfigState
import a75f.io.renatus.profiles.hss.HyperStatSplitViewModel
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import a75f.io.logic.util.modifyConditioningMode
import a75f.io.logic.util.modifyFanMode
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class HyperStatSplitCpuViewModel : HyperStatSplitViewModel() {

    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        super.init(bundle, context, hayStack)
        equipModel = ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
        deviceModel = ModelLoader.getHyperStatSplitDeviceModel() as SeventyFiveFDeviceDirective

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

        this.context = context
        this.hayStack = hayStack

        initializeLists()
        isCopiedConfigurationAvailable()
        CcuLog.i(Domain.LOG_TAG, "HSS initialized")
    }

    private fun initializeLists() {
        temperatureOffsetsList = getListByDomainName(DomainName.temperatureOffset, equipModel)

        fanLowMedHighSpeedsList = getListByDomainName(DomainName.analog1FanLow, equipModel)

        outsideDamperMinOpenList = getListByDomainName(DomainName.outsideDamperMinOpenDuringRecirculation, equipModel)
        exhaustFanThresholdList = getListByDomainName(DomainName.exhaustFanStage1Threshold, equipModel)
        exhaustFanHysteresisList = getListByDomainName(DomainName.exhaustFanHysteresis, equipModel)

        prePurgeOutsideDamperOpenList = getListByDomainName(DomainName.prePurgeOutsideDamperOpen, equipModel)
        zoneCO2DamperOpeningRateList = getListByDomainName(DomainName.co2DamperOpeningRate, equipModel)
        zoneCO2ThresholdList = getListByDomainName(DomainName.co2Threshold, equipModel)
        zoneCO2TargetList = getListByDomainName(DomainName.co2Target, equipModel)
        zoneVOCThresholdList = getListByDomainName(DomainName.vocThreshold, equipModel)
        zoneVOCTargetList = getListByDomainName(DomainName.vocTarget, equipModel)
        zonePM2p5TargetList = getListByDomainName(DomainName.pm25Target, equipModel)
    }

    fun isCoolingAOEnabled() = isAnyAnalogMappedToControl(CpuControlType.COOLING)
    fun isHeatingAOEnabled() = isAnyAnalogMappedToControl(CpuControlType.HEATING)
    fun isLinearFanAOEnabled() = isAnyAnalogMappedToControl(CpuControlType.LINEAR_FAN)
    fun isOAODamperAOEnabled() = isAnyAnalogMappedToControl(CpuControlType.OAO_DAMPER)
    fun isStagedFanAOEnabled() = isAnyAnalogMappedToControl(CpuControlType.STAGED_FAN)
    fun isReturnDamperAOEnabled() = isAnyAnalogMappedToControl(CpuControlType.RETURN_DAMPER)
    fun isCompressorAOEnabled() = isAnyAnalogMappedToControl(CpuControlType.COMPRESSOR_SPEED)
    fun isDamperModulationAOEnabled() = isAnyAnalogMappedToControl(CpuControlType.DCV_MODULATING_DAMPER)

    private fun isAnyAnalogMappedToControl(type: CpuControlType): Boolean {
        return (
                (this.viewState.value.analogOut1Enabled && this.viewState.value.analogOut1Association == type.ordinal) ||
                (this.viewState.value.analogOut2Enabled && this.viewState.value.analogOut2Association == type.ordinal) ||
                (this.viewState.value.analogOut3Enabled && this.viewState.value.analogOut3Association == type.ordinal) ||
                (this.viewState.value.analogOut4Enabled && this.viewState.value.analogOut4Association == type.ordinal)
            )
    }

    private fun isAnyUniversalInMapped(type: CpuUniInType): Boolean {
        return (
                (this.viewState.value.universalIn1Config.enabled && this.viewState.value.universalIn1Config.association == type.ordinal) ||
                (this.viewState.value.universalIn2Config.enabled && this.viewState.value.universalIn2Config.association == type.ordinal) ||
                (this.viewState.value.universalIn3Config.enabled && this.viewState.value.universalIn3Config.association == type.ordinal) ||
                (this.viewState.value.universalIn4Config.enabled && this.viewState.value.universalIn4Config.association == type.ordinal) ||
                (this.viewState.value.universalIn5Config.enabled && this.viewState.value.universalIn5Config.association == type.ordinal) ||
                (this.viewState.value.universalIn6Config.enabled && this.viewState.value.universalIn6Config.association == type.ordinal) ||
                (this.viewState.value.universalIn7Config.enabled && this.viewState.value.universalIn7Config.association == type.ordinal) ||
                (this.viewState.value.universalIn8Config.enabled && this.viewState.value.universalIn8Config.association == type.ordinal)
            )
    }

    private fun isUniversalInDuplicated(type: CpuUniInType): Boolean {
        var nInstances = 0

        if (this.viewState.value.universalIn1Config.enabled && this.viewState.value.universalIn1Config.association == type.ordinal) nInstances++
        if (this.viewState.value.universalIn2Config.enabled && this.viewState.value.universalIn2Config.association == type.ordinal) nInstances++
        if (this.viewState.value.universalIn3Config.enabled && this.viewState.value.universalIn3Config.association == type.ordinal) nInstances++
        if (this.viewState.value.universalIn4Config.enabled && this.viewState.value.universalIn4Config.association == type.ordinal) nInstances++
        if (this.viewState.value.universalIn5Config.enabled && this.viewState.value.universalIn5Config.association == type.ordinal) nInstances++
        if (this.viewState.value.universalIn6Config.enabled && this.viewState.value.universalIn6Config.association == type.ordinal) nInstances++
        if (this.viewState.value.universalIn7Config.enabled && this.viewState.value.universalIn7Config.association == type.ordinal) nInstances++
        if (this.viewState.value.universalIn8Config.enabled && this.viewState.value.universalIn8Config.association == type.ordinal) nInstances++

        return nInstances > 1
    }

    private fun isAnySensorBusMapped(type: CpuEconSensorBusTempAssociation): Boolean {
        return (
                (this.viewState.value.sensorAddress0.enabled && this.viewState.value.sensorAddress0.association == type.ordinal) ||
                (this.viewState.value.sensorAddress1.enabled && this.viewState.value.sensorAddress1.association == type.ordinal) ||
                (this.viewState.value.sensorAddress2.enabled && this.viewState.value.sensorAddress2.association == type.ordinal)
            )
    }

    private fun isSensorBusDuplicated(type: CpuEconSensorBusTempAssociation): Boolean {
        var nInstances = 0

        if (this.viewState.value.sensorAddress0.enabled && this.viewState.value.sensorAddress0.association == type.ordinal) nInstances++
        if (this.viewState.value.sensorAddress1.enabled && this.viewState.value.sensorAddress1.association == type.ordinal) nInstances++
        if (this.viewState.value.sensorAddress2.enabled && this.viewState.value.sensorAddress2.association == type.ordinal) nInstances++

        return nInstances > 1
    }

    fun isHeatStage1RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.HEATING_STAGE1)
    fun isHeatStage2RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.HEATING_STAGE2)
    fun isHeatStage3RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.HEATING_STAGE3)
    fun isCoolStage1RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.COOLING_STAGE1)
    fun isCoolStage2RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.COOLING_STAGE2)
    fun isCoolStage3RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.COOLING_STAGE3)
    fun isFanLowRelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.FAN_LOW_SPEED)
    fun isFanMediumRelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.FAN_MEDIUM_SPEED)
    fun isFanHighRelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.FAN_HIGH_SPEED)
    fun isCompressorStage1RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE1)
    fun isCompressorStage2RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE2)
    fun isCompressorStage3RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE3)
    fun isCompressorMappedWithAnyRelay(): Boolean {
       return (isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE1)||
        isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE2)||
        isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE3))
    }
    fun isChangeOverCoolingMapped(ignoreConfig: ConfigState) = isAnyRelayMappedToControl(CpuRelayType.CHANGE_OVER_O_COOLING, ignoreConfig)
    fun isChangeOverHeatingMapped(ignoreConfig: ConfigState) = isAnyRelayMappedToControl(CpuRelayType.CHANGE_OVER_B_HEATING, ignoreConfig)

    fun isAO1MappedToFan() : Boolean {
        return this.viewState.value.analogOut1Enabled &&
                (this.viewState.value.analogOut1Association == CpuControlType.LINEAR_FAN.ordinal ||
                        this.viewState.value.analogOut1Association == CpuControlType.STAGED_FAN.ordinal)
    }
    fun isAO2MappedToFan() : Boolean {
        return this.viewState.value.analogOut2Enabled &&
                (this.viewState.value.analogOut2Association == CpuControlType.LINEAR_FAN.ordinal ||
                        this.viewState.value.analogOut2Association == CpuControlType.STAGED_FAN.ordinal)
    }
    fun isAO3MappedToFan() : Boolean {
        return this.viewState.value.analogOut3Enabled &&
                (this.viewState.value.analogOut3Association == CpuControlType.LINEAR_FAN.ordinal ||
                        this.viewState.value.analogOut3Association == CpuControlType.STAGED_FAN.ordinal)
    }
    fun isAO4MappedToFan() : Boolean {
        return this.viewState.value.analogOut4Enabled &&
                (this.viewState.value.analogOut4Association == CpuControlType.LINEAR_FAN.ordinal ||
                        this.viewState.value.analogOut4Association == CpuControlType.STAGED_FAN.ordinal)
    }

    fun isPrePurgeEnabled() : Boolean {
        return this.viewState.value.prePurge
    }


    private fun isAnyRelayMappedToControl(type: CpuRelayType): Boolean {

        fun isEnabledAndMapped(configState: ConfigState): Boolean {
            return (configState.enabled && configState.association == type.ordinal)
        }
        return (isEnabledAndMapped(this.viewState.value.relay1Config) ||
                isEnabledAndMapped(this.viewState.value.relay2Config) ||
                isEnabledAndMapped(this.viewState.value.relay3Config) ||
                isEnabledAndMapped(this.viewState.value.relay4Config) ||
                isEnabledAndMapped(this.viewState.value.relay5Config) ||
                isEnabledAndMapped(this.viewState.value.relay6Config) ||
                isEnabledAndMapped(this.viewState.value.relay7Config) ||
                isEnabledAndMapped(this.viewState.value.relay8Config))
    }

    fun isAnyRelayMappedToControl(
        type: CpuRelayType,
        ignoreConfig: ConfigState
    ): Boolean {
        fun isEnabledAndMapped(configState: ConfigState): Boolean {
            return (configState != ignoreConfig && configState.enabled && configState.association == type.ordinal)
        }
        return (isEnabledAndMapped(this.viewState.value.relay1Config) ||
                isEnabledAndMapped(this.viewState.value.relay2Config) ||
                isEnabledAndMapped(this.viewState.value.relay3Config) ||
                isEnabledAndMapped(this.viewState.value.relay4Config) ||
                isEnabledAndMapped(this.viewState.value.relay5Config) ||
                isEnabledAndMapped(this.viewState.value.relay6Config) ||
                isEnabledAndMapped(this.viewState.value.relay7Config) ||
                isEnabledAndMapped(this.viewState.value.relay8Config))
    }

    fun isAnalogEnabledAndMapped(type: CpuControlType, enabled: Boolean, association: Int) =
        (enabled && association == type.ordinal)

    override fun saveConfiguration() {
        if (validateProfileConfig()) {
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
                    DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
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


        val isCompressorMapped = (isCompressorMappedWithAnyRelay() || isCompressorAOEnabled())
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
                !isAnyAnalogMappedToControl(CpuControlType.OAO_DAMPER) ||
                !(isAnyUniversalInMapped(CpuUniInType.OUTSIDE_AIR_TEMPERATURE)  || isAnySensorBusMapped(CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY)) ||
                !(isAnyUniversalInMapped(CpuUniInType.MIXED_AIR_TEMPERATURE) || isAnySensorBusMapped(CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY))
            )
        ) {
            openMissingDialog = true
            return false
        }

        if (isUniversalInDuplicated(CpuUniInType.OUTSIDE_AIR_TEMPERATURE) ||
            isSensorBusDuplicated(CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(CpuUniInType.OUTSIDE_AIR_TEMPERATURE) && isAnySensorBusMapped(CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY))
        ) {
            openDuplicateDialog = true
            return false
        }

        if (isUniversalInDuplicated(CpuUniInType.MIXED_AIR_TEMPERATURE) ||
            isSensorBusDuplicated(CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(CpuUniInType.MIXED_AIR_TEMPERATURE) && isAnySensorBusMapped(CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY))
        ) {
            openDuplicateDialog = true
            return false
        }

        if (isUniversalInDuplicated(CpuUniInType.CONDENSATE_STATUS_NO) ||
            isUniversalInDuplicated(CpuUniInType.CONDENSATE_STATUS_NC) ||
            (isAnyUniversalInMapped(CpuUniInType.CONDENSATE_STATUS_NO) && isAnyUniversalInMapped(CpuUniInType.CONDENSATE_STATUS_NC))
        ) {
            openDuplicateDialog = true
            return false
        }

        if (isUniversalInDuplicated(CpuUniInType.SUPPLY_AIR_TEMPERATURE) ||
            isSensorBusDuplicated(CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(CpuUniInType.SUPPLY_AIR_TEMPERATURE) && isAnySensorBusMapped(CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY))
        ) {
            openDuplicateDialog = true
            return false
        }
        
        if (isUniversalInDuplicated(CpuUniInType.FILTER_STATUS_NO) ||
            isUniversalInDuplicated(CpuUniInType.FILTER_STATUS_NC) ||
            (isAnyUniversalInMapped(CpuUniInType.FILTER_STATUS_NO) && isAnyUniversalInMapped(CpuUniInType.FILTER_STATUS_NC))
        ) {
            openDuplicateDialog = true
            return false
        }

        if (isUniversalInDuplicated(CpuUniInType.GENERIC_ALARM_NO) ||
            isUniversalInDuplicated(CpuUniInType.GENERIC_ALARM_NC) ||
            (isAnyUniversalInMapped(CpuUniInType.GENERIC_ALARM_NO) && isAnyUniversalInMapped(CpuUniInType.GENERIC_ALARM_NC))
        ) {
            openDuplicateDialog = true
            return false
        }

        if (isUniversalInDuplicated(CpuUniInType.CURRENT_TX_10) ||
            isUniversalInDuplicated(CpuUniInType.CURRENT_TX_20) ||
            isUniversalInDuplicated(CpuUniInType.CURRENT_TX_30) ||
            isUniversalInDuplicated(CpuUniInType.CURRENT_TX_50) ||
            isUniversalInDuplicated(CpuUniInType.CURRENT_TX_60) ||
            isUniversalInDuplicated(CpuUniInType.CURRENT_TX_100) ||
            isUniversalInDuplicated(CpuUniInType.CURRENT_TX_120) ||
            isUniversalInDuplicated(CpuUniInType.CURRENT_TX_150) ||
            isUniversalInDuplicated(CpuUniInType.CURRENT_TX_200) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_10) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_20)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_10) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_30)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_10) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_50)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_10) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_60)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_10) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_10) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_10) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_10) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_20) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_30)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_20) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_50)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_20) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_60)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_20) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_20) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_20) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_20) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_30) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_50)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_30) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_60)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_30) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_30) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_30) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_30) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_50) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_60)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_50) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_50) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_50) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_50) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_60) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_60) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_60) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_60) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_100) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_100) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_100) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_120) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_120) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_150) && isAnyUniversalInMapped(CpuUniInType.CURRENT_TX_200))
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

        if (profileConfiguration.isDefault) {

            val equipId = addEquipAndPoints(
                deviceAddress,
                profileConfiguration,
                hayStack,
                equipModel,
                deviceModel
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
                    handleNonDefaultConditioningMode(
                        profileConfiguration as HyperStatSplitCpuConfiguration,
                        hayStack
                    )
                    handleNonDefaultFanMode(
                        profileConfiguration as HyperStatSplitCpuConfiguration,
                        hayStack
                    )
                    initializePrePurgeStatus(
                        profileConfiguration,
                        hayStack,
                        1.0
                    )
                    initializePrePurgeStatus(
                        profileConfiguration,
                        hayStack,
                        0.0
                    )
                    L.ccu().zoneProfiles.add(hssProfile)
                }
            }

        } else {
            val equipId = equipBuilder.updateEquipAndPoints(profileConfiguration, equipModel, hayStack.site!!.id, equipDis, true)

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
                    handleNonDefaultConditioningMode(
                        profileConfiguration as HyperStatSplitCpuConfiguration,
                        hayStack
                    )
                    handleNonDefaultFanMode(
                        profileConfiguration as HyperStatSplitCpuConfiguration,
                        hayStack
                    )
                    initializePrePurgeStatus(
                        profileConfiguration,
                        hayStack,
                        1.0
                    )
                    initializePrePurgeStatus(
                        profileConfiguration,
                        hayStack,
                        0.0
                    )
                }
            }
        }

        val possibleConditioningMode = getPossibleConditioningModeSettings(profileConfiguration as HyperStatSplitCpuConfiguration)
        val possibleFanMode = getSplitPossibleFanModeSettings(profileConfiguration.nodeAddress)
        modifyFanMode(possibleFanMode.ordinal, hssProfile.hssEquip.fanOpMode)
        modifyConditioningMode(possibleConditioningMode.ordinal, hssProfile.hssEquip.conditioningMode, allStandaloneProfileConditions)
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

    private fun setScheduleType(config: HyperStatSplitConfiguration) {
        hayStack.readEntity("point and domainName == \"" + DomainName.scheduleType + "\" and group == \"" + config.nodeAddress + "\"")["id"]?.let { scheduleTypeId ->
            val roomSchedule = getSchedule(zoneRef, floorRef)
            if(roomSchedule.isZoneSchedule) {
                hayStack.writeDefaultValById(scheduleTypeId.toString(), 1.0)
            } else {
                hayStack.writeDefaultValById(scheduleTypeId.toString(), 2.0)
            }
        }
    }

    override fun hasUnsavedChanges() : Boolean {
        return try {
            !HyperStatSplitCpuState.fromProfileConfigToState(profileConfiguration as HyperStatSplitCpuConfiguration).equalsViewState(viewState.value as HyperStatSplitCpuState)
        } catch (e: Exception) {
            false
        }
    }

}