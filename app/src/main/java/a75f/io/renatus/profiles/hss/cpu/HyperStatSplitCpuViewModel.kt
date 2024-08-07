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
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.hyperstatsplit.profiles.HyperStatSplitProfileConfiguration
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconSensorBusTempAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuUniInType
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconProfile
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuProfileConfiguration
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.getSchedule
import a75f.io.messaging.handler.HyperstatSplitReconfigurationHandler
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.hss.HyperStatSplitViewModel
import a75f.io.renatus.util.ProgressDialogUtils
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HyperStatSplitCpuViewModel : HyperStatSplitViewModel() {

    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        super.init(bundle, context, hayStack)
        equipModel = ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
        deviceModel = ModelLoader.getHyperStatSplitDeviceModel() as SeventyFiveFDeviceDirective

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is HyperStatSplitCpuEconProfile) {
            hssProfile = L.getProfile(deviceAddress) as HyperStatSplitCpuEconProfile
            profileConfiguration = HyperStatSplitCpuProfileConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, equipModel
            ).getActiveConfiguration()
        } else {
            profileConfiguration = HyperStatSplitCpuProfileConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, equipModel
            ).getDefaultConfiguration()
        }

        viewState.value = HyperStatSplitCpuState.fromProfileConfigToState(profileConfiguration as HyperStatSplitCpuProfileConfiguration)

        this.context = context
        this.hayStack = hayStack

        initializeLists()

        modelLoaded = true
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

    fun isCoolingAOEnabled() = isAnyAnalogMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuControlType.COOLING)
    fun isHeatingAOEnabled() = isAnyAnalogMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuControlType.HEATING)
    fun isLinearFanAOEnabled() = isAnyAnalogMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuControlType.LINEAR_FAN)
    fun isOAODamperAOEnabled() = isAnyAnalogMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuControlType.OAO_DAMPER)
    fun isStagedFanAOEnabled() = isAnyAnalogMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuControlType.STAGED_FAN)
    fun isReturnDamperAOEnabled() = isAnyAnalogMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuControlType.RETURN_DAMPER)

    private fun isAnyAnalogMappedToControl(type: HyperstatSplitReconfigurationHandler.Companion.CpuControlType): Boolean {
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
        var nInstances : Int = 0

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
        var nInstances : Int = 0

        if (this.viewState.value.sensorAddress0.enabled && this.viewState.value.sensorAddress0.association == type.ordinal) nInstances++
        if (this.viewState.value.sensorAddress1.enabled && this.viewState.value.sensorAddress1.association == type.ordinal) nInstances++
        if (this.viewState.value.sensorAddress2.enabled && this.viewState.value.sensorAddress2.association == type.ordinal) nInstances++

        return nInstances > 1
    }

    fun isHeatStage1RelayEnabled() = isAnyRelayMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuRelayType.HEATING_STAGE1)
    fun isHeatStage2RelayEnabled() = isAnyRelayMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuRelayType.HEATING_STAGE2)
    fun isHeatStage3RelayEnabled() = isAnyRelayMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuRelayType.HEATING_STAGE3)
    fun isCoolStage1RelayEnabled() = isAnyRelayMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuRelayType.COOLING_STAGE1)
    fun isCoolStage2RelayEnabled() = isAnyRelayMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuRelayType.COOLING_STAGE2)
    fun isCoolStage3RelayEnabled() = isAnyRelayMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuRelayType.COOLING_STAGE3)
    fun isFanLowRelayEnabled() = isAnyRelayMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuRelayType.FAN_LOW_SPEED)
    fun isFanMediumRelayEnabled() = isAnyRelayMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuRelayType.FAN_MEDIUM_SPEED)
    fun isFanHighRelayEnabled() = isAnyRelayMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuRelayType.FAN_HIGH_SPEED)

    fun isAO1MappedToFan() : Boolean {
        return this.viewState.value.analogOut1Enabled &&
                (this.viewState.value.analogOut1Association == HyperstatSplitReconfigurationHandler.Companion.CpuControlType.LINEAR_FAN.ordinal ||
                        this.viewState.value.analogOut1Association == HyperstatSplitReconfigurationHandler.Companion.CpuControlType.STAGED_FAN.ordinal)
    }
    fun isAO2MappedToFan() : Boolean {
        return this.viewState.value.analogOut2Enabled &&
                (this.viewState.value.analogOut2Association == HyperstatSplitReconfigurationHandler.Companion.CpuControlType.LINEAR_FAN.ordinal ||
                        this.viewState.value.analogOut2Association == HyperstatSplitReconfigurationHandler.Companion.CpuControlType.STAGED_FAN.ordinal)
    }
    fun isAO3MappedToFan() : Boolean {
        return this.viewState.value.analogOut3Enabled &&
                (this.viewState.value.analogOut3Association == HyperstatSplitReconfigurationHandler.Companion.CpuControlType.LINEAR_FAN.ordinal ||
                        this.viewState.value.analogOut3Association == HyperstatSplitReconfigurationHandler.Companion.CpuControlType.STAGED_FAN.ordinal)
    }
    fun isAO4MappedToFan() : Boolean {
        return this.viewState.value.analogOut4Enabled &&
                (this.viewState.value.analogOut4Association == HyperstatSplitReconfigurationHandler.Companion.CpuControlType.LINEAR_FAN.ordinal ||
                        this.viewState.value.analogOut4Association == HyperstatSplitReconfigurationHandler.Companion.CpuControlType.STAGED_FAN.ordinal)
    }

    fun isPrePurgeEnabled() : Boolean {
        return this.viewState.value.prePurge
    }

    private fun isAnyRelayMappedToControl(type: HyperstatSplitReconfigurationHandler.Companion.CpuRelayType): Boolean {
        return ((this.viewState.value.relay1Config.enabled && this.viewState.value.relay1Config.association == type.ordinal) || (this.viewState.value.relay2Config.enabled && this.viewState.value.relay2Config.association == type.ordinal) || (this.viewState.value.relay3Config.enabled && this.viewState.value.relay3Config.association == type.ordinal) || (this.viewState.value.relay4Config.enabled && this.viewState.value.relay4Config.association == type.ordinal) || (this.viewState.value.relay5Config.enabled && this.viewState.value.relay5Config.association == type.ordinal) || (this.viewState.value.relay6Config.enabled && this.viewState.value.relay6Config.association == type.ordinal) || (this.viewState.value.relay7Config.enabled && this.viewState.value.relay7Config.association == type.ordinal) || (this.viewState.value.relay8Config.enabled && this.viewState.value.relay8Config.association == type.ordinal))
    }

    fun isAnalogEnabledAndMapped(type: HyperstatSplitReconfigurationHandler.Companion.CpuControlType, enabled: Boolean, association: Int) =
        (enabled && association == type.ordinal)

    override fun saveConfiguration() {
        if (validateProfileConfig()) {
            if (saveJob == null) {
                saveJob = viewModelScope.launch {
                    ProgressDialogUtils.showProgressDialog(context, "Saving HyperStat Split Configuration")
                    withContext(Dispatchers.IO) {
                        CCUHsApi.getInstance().resetCcuReady()

                        setUpHyperStatSplitProfile()
                        CcuLog.i(Domain.LOG_TAG, "HSS Profile Setup complete")
                        L.saveCCUState()

                        hayStack.syncEntityTree()
                        CCUHsApi.getInstance().setCcuReady()
                        CcuLog.i(Domain.LOG_TAG, "Send seed for $deviceAddress")
                        LSerial.getInstance()
                            .sendHyperSplitSeedMessage(deviceAddress, zoneRef, floorRef)

                        DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
                        CcuLog.i(Domain.LOG_TAG, "HSS Profile Pairing complete")

                        withContext(Dispatchers.Main) {
                            context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                            showToast("HSS Configuration saved successfully", context)
                            CcuLog.i(Domain.LOG_TAG, "Close Pairing dialog")
                            ProgressDialogUtils.hideProgressDialog()
                            _isDialogOpen.postValue(false)
                        }

                    }

                    // This check is needed because the dialog sometimes fails to close inside the coroutine.
                    // We don't know why this happens.
                    if (ProgressDialogUtils.isDialogShowing()) {
                        ProgressDialogUtils.hideProgressDialog()
                        _isDialogOpen.postValue(false)
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

        if (viewState.value.enableOutsideAirOptimization && (
                !isAnyAnalogMappedToControl(HyperstatSplitReconfigurationHandler.Companion.CpuControlType.OAO_DAMPER) ||
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
            profileConfiguration = HyperStatSplitCpuProfileConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, equipModel
            ).getActiveConfiguration()
        } else {
            profileConfiguration = HyperStatSplitCpuProfileConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, equipModel
            ).getDefaultConfiguration()
        }

        viewState.value = HyperStatSplitCpuState.fromProfileConfigToState(profileConfiguration as HyperStatSplitCpuProfileConfiguration)

        openCancelDialog = false
    }

    private fun setUpHyperStatSplitProfile() {
        (viewState.value as HyperStatSplitCpuState).updateConfigFromViewState(profileConfiguration as HyperStatSplitCpuProfileConfiguration)

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-cpuecon-" + profileConfiguration.nodeAddress

        if (profileConfiguration.isDefault) {

            val equipId = addEquipAndPoints(deviceAddress, profileConfiguration, floorRef, zoneRef, nodeType, hayStack, equipModel, deviceModel)

            HyperstatSplitReconfigurationHandler.Companion.addLinearFanLowMedHighPoints(equipId, hayStack.site!!.id, equipDis, hayStack, profileConfiguration as HyperStatSplitCpuProfileConfiguration, equipModel)
            HyperstatSplitReconfigurationHandler.Companion.correctSensorBusTempPoints(profileConfiguration, hayStack)
            HyperstatSplitReconfigurationHandler.Companion.addSensorBusPressureLogicalPoint(profileConfiguration, equipId, hayStack, equipModel)
            HyperstatSplitReconfigurationHandler.Companion.setOutputTypes(profileConfiguration, hayStack)
            setScheduleType(profileConfiguration)
            HyperstatSplitReconfigurationHandler.Companion.handleNonDefaultConditioningMode(profileConfiguration as HyperStatSplitCpuProfileConfiguration, hayStack)
            HyperstatSplitReconfigurationHandler.Companion.handleNonDefaultFanMode(profileConfiguration as HyperStatSplitCpuProfileConfiguration, hayStack)
            HyperstatSplitReconfigurationHandler.Companion.initializePrePurgeStatus(profileConfiguration, hayStack, 1.0)
            HyperstatSplitReconfigurationHandler.Companion.initializePrePurgeStatus(profileConfiguration, hayStack, 0.0)
            L.ccu().zoneProfiles.add(hssProfile)

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

            HyperstatSplitReconfigurationHandler.Companion.addLinearFanLowMedHighPoints(equipId, hayStack.site!!.id, equipDis, hayStack, profileConfiguration as HyperStatSplitCpuProfileConfiguration, equipModel)
            HyperstatSplitReconfigurationHandler.Companion.correctSensorBusTempPoints(profileConfiguration, hayStack)
            HyperstatSplitReconfigurationHandler.Companion.addSensorBusPressureLogicalPoint(profileConfiguration, equipId, hayStack, equipModel)
            HyperstatSplitReconfigurationHandler.Companion.setOutputTypes(profileConfiguration, hayStack)
            setScheduleType(profileConfiguration)
            HyperstatSplitReconfigurationHandler.Companion.handleNonDefaultConditioningMode(profileConfiguration as HyperStatSplitCpuProfileConfiguration, hayStack)
            HyperstatSplitReconfigurationHandler.Companion.handleNonDefaultFanMode(profileConfiguration as HyperStatSplitCpuProfileConfiguration, hayStack)
            HyperstatSplitReconfigurationHandler.Companion.initializePrePurgeStatus(profileConfiguration, hayStack, 1.0)
            HyperstatSplitReconfigurationHandler.Companion.initializePrePurgeStatus(profileConfiguration, hayStack, 0.0)
            hssProfile.refreshEquip()
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

    private fun setScheduleType(config: HyperStatSplitProfileConfiguration) {
        val scheduleTypePoint = hayStack.readEntity("point and domainName == \"" + DomainName.scheduleType + "\" and group == \"" + config.nodeAddress + "\"")
        val scheduleTypeId = scheduleTypePoint.get("id").toString()

        val roomSchedule = getSchedule(zoneRef, floorRef)
        if(roomSchedule.isZoneSchedule) {
            hayStack.writeDefaultValById(scheduleTypeId, 1.0)
        } else {
            hayStack.writeDefaultValById(scheduleTypeId, 2.0)
        }
    }

    override fun hasUnsavedChanges() : Boolean {
        try {
            return !HyperStatSplitCpuState.fromProfileConfigToState(profileConfiguration as HyperStatSplitCpuProfileConfiguration).equalsViewState(viewState.value as HyperStatSplitCpuState)
        } catch (e: Exception) {
            return false
        }
    }

}