package a75f.io.device.mesh.hyperstat

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.device.HyperStat
import a75f.io.device.HyperStat.HyperStatSettingsMessage2_t
import a75f.io.device.HyperStat.HyperStatSettingsMessage3_t
import a75f.io.device.HyperStat.HyperStatSettingsMessage_t
import a75f.io.device.util.DeviceConfigurationUtil.Companion.getUserConfiguration
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.readValAtLevelByDomain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.hyperstat.CpuV2Equip
import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.hyperstat.MonitoringEquip
import a75f.io.domain.equips.hyperstat.Pipe2V2Equip
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsCpuAnalogOutMapping
import a75f.io.logic.tuners.TunerConstants
import a75f.io.logic.tuners.TunerUtil

/**
 * Created by Manjunath K on 14-12-2021.
 */
class HyperStatSettingsUtil {

    companion object {
        var ccuControlMessageTimer: Long = 0
            get() {
                if (field == 0L) {
                    ccuControlMessageTimer = System.currentTimeMillis()
                }
                return field
            }
    }
}

private fun getHeatingUserLimit(type: String, roomRef: String): Int {
    val hsApi = CCUHsApi.getInstance()
    var pointValue = hsApi.readPointPriorityValByQuery("schedulable and heating and user and limit and $type and roomRef == \"$roomRef\"")
    if (pointValue == 0.0) { // // Fall back to default value
        pointValue = hsApi.readPointPriorityValByQuery("point and schedulable and default and heating and user and limit and min")
    }
    return pointValue.toInt()
}

private fun getCoolingUserLimit(type: String, roomRef: String): Int {
    val hsApi = CCUHsApi.getInstance()
    var pointValue = hsApi.readPointPriorityValByQuery("schedulable and cooling and user and limit and $type and roomRef == \"$roomRef\"")
    if (pointValue == 0.0) { // Fall back to default value
        pointValue = hsApi.readPointPriorityValByQuery("point and schedulable and default and cooling and user and limit and min")
    }
    return pointValue.toInt()
}

private fun getHeatingDeadBand(roomRef: String): Double {
    val pointId = CCUHsApi.getInstance().readId("deadband and heating and not multiplier and roomRef == \"$roomRef\"")
    if (pointId != null)
        return HSUtil.getPriorityVal(pointId)
    return 0.0
}

private fun getCoolingDeadBand(roomRef: String): Double {
    val pointId = CCUHsApi.getInstance().readId("deadband and cooling and not multiplier and roomRef == \"$roomRef\"")
    if (pointId != null)
        return HSUtil.getPriorityVal(pointId)
    return 0.0
}

private fun getTempMode(): HyperStat.HyperStatTemperatureMode_e {
    val temperatureMode = readValAtLevelByDomain(DomainName.temperatureMode, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL).toInt()
    return if (temperatureMode == 0)
        HyperStat.HyperStatTemperatureMode_e.HYPERSTAT_TEMP_MODE_DUAL_FIXED_DB
    else
        HyperStat.HyperStatTemperatureMode_e.HYPERSTAT_TEMP_MODE_DUAL_VARIABLE_DB
}

private fun isAnalogOutEnabledAndIsMapped(enablePoint: Point, association: Point, mapping: Int): Boolean {
    return (enablePoint.readDefaultVal() == 1.0 && association.readDefaultVal().toInt() == mapping )
}

private fun setLinearFanSpeedDetails(equip: HyperStatEquip): HyperStat.HyperstatLinearFanSpeeds_t {
    val linearFanSpeedBuilder = HyperStat.HyperstatLinearFanSpeeds_t.newBuilder()
    equip.apply {
            // For all the profiles fan speed position is 2 so just using same cpu linear fan speed position
            // DO NOT CHANGE THIS
            if (isAnalogOutEnabledAndIsMapped(analog1OutputEnable, analog1OutputAssociation, HsCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal)) {
                linearFanSpeedBuilder.linearFanLowSpeedLevel = analog1FanLow.readPriorityVal().toInt()
                linearFanSpeedBuilder.linearFanMediumSpeedLevel = analog1FanMedium.readPriorityVal().toInt()
                linearFanSpeedBuilder.linearFanHighSpeedLevel = analog1FanHigh.readPriorityVal().toInt()
            }
            if (isAnalogOutEnabledAndIsMapped(analog2OutputEnable, analog2OutputAssociation, HsCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal)) {
                linearFanSpeedBuilder.linearFanLowSpeedLevel = analog2FanLow.readPriorityVal().toInt()
                linearFanSpeedBuilder.linearFanMediumSpeedLevel = analog2FanMedium.readPriorityVal().toInt()
                linearFanSpeedBuilder.linearFanHighSpeedLevel = analog2FanHigh.readPriorityVal().toInt()
            }
            if (isAnalogOutEnabledAndIsMapped(analog3OutputEnable, analog3OutputAssociation, HsCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal)) {
                linearFanSpeedBuilder.linearFanLowSpeedLevel = analog3FanLow.readPriorityVal().toInt()
                linearFanSpeedBuilder.linearFanMediumSpeedLevel = analog3FanMedium.readPriorityVal().toInt()
                linearFanSpeedBuilder.linearFanHighSpeedLevel = analog3FanHigh.readPriorityVal().toInt()
            }
    }
    return linearFanSpeedBuilder.build()
}

private fun setStagedFanSpeedDetails(equip: HyperStatEquip): HyperStat.HyperstatStagedFanSpeeds_t {
    val stagedFanSpeedBuilder = HyperStat.HyperstatStagedFanSpeeds_t.newBuilder()
    equip.apply {
        if (this is CpuV2Equip) {
            if (isAnalogOutEnabledAndIsMapped(analog1OutputEnable, analog1OutputAssociation, HsCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal)) {
                stagedFanSpeedBuilder.stagedFanLowSpeedLevel = analog1FanLow.readPriorityVal().toInt()
                stagedFanSpeedBuilder.stagedFanMediumSpeedLevel = analog1FanMedium.readPriorityVal().toInt()
                stagedFanSpeedBuilder.stagedFanHighSpeedLevel = analog1FanHigh.readPriorityVal().toInt()
            }
            if (isAnalogOutEnabledAndIsMapped(analog2OutputEnable, analog2OutputAssociation, HsCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal)) {
                stagedFanSpeedBuilder.stagedFanLowSpeedLevel = analog2FanLow.readPriorityVal().toInt()
                stagedFanSpeedBuilder.stagedFanMediumSpeedLevel = analog2FanMedium.readPriorityVal().toInt()
                stagedFanSpeedBuilder.stagedFanHighSpeedLevel = analog2FanHigh.readPriorityVal().toInt()
            }
            if (isAnalogOutEnabledAndIsMapped(analog3OutputEnable, analog3OutputAssociation, HsCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal)) {
                stagedFanSpeedBuilder.stagedFanLowSpeedLevel = analog3FanLow.readPriorityVal().toInt()
                stagedFanSpeedBuilder.stagedFanMediumSpeedLevel = analog3FanMedium.readPriorityVal().toInt()
                stagedFanSpeedBuilder.stagedFanHighSpeedLevel = analog3FanHigh.readPriorityVal().toInt()
            }
        }
    }
    return stagedFanSpeedBuilder.build()
}

private fun getRelayConfigs(equip: HyperStatEquip): HyperStat.HyperstatRelay_t {
    val relayConfigs = HyperStat.HyperstatRelay_t.newBuilder()
    equip.apply {
        relayConfigs.relay1Enable = relay1OutputEnable.readDefaultVal() == 1.0
        relayConfigs.relay2Enable = relay2OutputEnable.readDefaultVal() == 1.0
        relayConfigs.relay3Enable = relay3OutputEnable.readDefaultVal() == 1.0
        relayConfigs.relay4Enable = relay4OutputEnable.readDefaultVal() == 1.0
        relayConfigs.relay5Enable = relay5OutputEnable.readDefaultVal() == 1.0
        relayConfigs.relay6Enable = relay6OutputEnable.readDefaultVal() == 1.0

        /**
         * Firmware mapping enum has "none" at 0 position but ccu will will not use none.
         * So we are adding 1 to avoid the 0 position all the time
         */

        if (relayConfigs.relay1Enable)
            relayConfigs.relay1Mapping = 1 + relay1OutputAssociation.readDefaultVal().toInt()
        if (relayConfigs.relay2Enable)
            relayConfigs.relay2Mapping = 1 + relay2OutputAssociation.readDefaultVal().toInt()
        if (relayConfigs.relay3Enable)
            relayConfigs.relay3Mapping = 1 + relay3OutputAssociation.readDefaultVal().toInt()
        if (relayConfigs.relay4Enable)
            relayConfigs.relay4Mapping = 1 + relay4OutputAssociation.readDefaultVal().toInt()
        if (relayConfigs.relay5Enable)
            relayConfigs.relay5Mapping = 1 + relay5OutputAssociation.readDefaultVal().toInt()
        if (relayConfigs.relay6Enable)
            relayConfigs.relay6Mapping = 1 + relay6OutputAssociation.readDefaultVal().toInt()
    }
    return relayConfigs.build()
}

private fun getAnalogOutConfigs(equip: HyperStatEquip): HyperStat.HyperstatAnalogOut_t {
    val analogOutConfigs = HyperStat.HyperstatAnalogOut_t.newBuilder()
    val hsApi = CCUHsApi.getInstance()
    equip.apply {
        analogOutConfigs.analogOut1Enable = analog1OutputEnable.readDefaultVal() == 1.0
        analogOutConfigs.analogOut2Enable = analog2OutputEnable.readDefaultVal() == 1.0
        analogOutConfigs.analogOut3Enable = analog3OutputEnable.readDefaultVal() == 1.0

        if (analogOutConfigs.analogOut1Enable) {
            analogOutConfigs.analogOut1Mapping = analog1OutputAssociation.readDefaultVal().toInt()
            analogOutConfigs.analogOut1AtMinSetting = hsApi.readDefaultVal("min and analog == 1 and equipRef == \"$equipRef\"").toInt() * 10
            analogOutConfigs.analogOut1AtMaxSetting = hsApi.readDefaultVal("max and analog == 1 and equipRef == \"$equipRef\"").toInt() * 10
        }
        if (analogOutConfigs.analogOut2Enable) {
            analogOutConfigs.analogOut2Mapping = analog2OutputAssociation.readDefaultVal().toInt()
            analogOutConfigs.analogOut2AtMinSetting = hsApi.readDefaultVal("min and analog == 2 and equipRef == \"$equipRef\"").toInt() * 10
            analogOutConfigs.analogOut2AtMaxSetting = hsApi.readDefaultVal("max and analog == 2 and equipRef == \"$equipRef\"").toInt() * 10
        }
        if (analogOutConfigs.analogOut3Enable) {
            analogOutConfigs.analogOut3Mapping = analog3OutputAssociation.readDefaultVal().toInt()
            analogOutConfigs.analogOut3AtMinSetting = hsApi.readDefaultVal("min and analog == 3 and equipRef == \"$equipRef\"").toInt() * 10
            analogOutConfigs.analogOut3AtMaxSetting = hsApi.readDefaultVal("max and analog == 3 and equipRef == \"$equipRef\"").toInt() * 10
        }
    }
    return analogOutConfigs.build()
}

private fun getAnalogInputConfigs(equip: HyperStatEquip): HyperStat.HyperstatAnalogIn_t {
    val analogIn = HyperStat.HyperstatAnalogIn_t.newBuilder()
    equip.apply {
        analogIn.analogIn1Enable = analog1InputEnable.readDefaultVal() == 1.0
        analogIn.analogIn2Enable = analog2InputEnable.readDefaultVal() == 1.0

        if (analogIn.analogIn1Enable) {
            analogIn.analogIn1Mapping = HyperStat.HyperstatAnalogInMapping_t.values()[analog1InputAssociation.readDefaultVal().toInt()]
        }
        if (analogIn.analogIn2Enable) {
            analogIn.analogIn2Mapping = HyperStat.HyperstatAnalogInMapping_t.values()[analog2InputAssociation.readDefaultVal().toInt()]
        }
    }
    return analogIn.build()
}

fun getHyperStatSettingsMessage(equipRef: String, zone: String): HyperStatSettingsMessage_t {
    val hyperStatEquip = Domain.getDomainEquip(equipRef) as HyperStatEquip
    val zoneId = HSUtil.getZoneIdFromEquipId(equipRef)
    return HyperStatSettingsMessage_t.newBuilder()
            .setRoomName(zone)
            .setHeatingDeadBand((getHeatingDeadBand(zoneId) * 10).toInt())
            .setCoolingDeadBand((getCoolingDeadBand(zoneId) * 10).toInt())
            .setMinCoolingUserTemp(getCoolingUserLimit("min", zoneId))
            .setMaxCoolingUserTemp(getCoolingUserLimit("max", zoneId))
            .setMinHeatingUserTemp(getHeatingUserLimit("min", zoneId))
            .setMaxHeatingUserTemp(getHeatingUserLimit("max", zoneId))
            .setTemperatureOffset((hyperStatEquip.temperatureOffset.readPriorityVal() * 10).toInt())
            .setHumidityMinSetpoint(hyperStatEquip.targetHumidifier.readPriorityVal().toInt())
            .setHumidityMaxSetpoint(hyperStatEquip.targetDehumidifier.readPriorityVal().toInt())
            .setShowCentigrade(getUserConfiguration() == 1.0)
            .setDisplayHumidity(hyperStatEquip.enableHumidityDisplay.readDefaultVal() == 1.0)
            .setDisplayCO2(hyperStatEquip.enableCo2Display.readDefaultVal() == 1.0)
            .setDisplayVOC(false)
            .setDisplayPM25(hyperStatEquip.enablePm25Display.readDefaultVal() == 1.0)
            .setCo2AlertTarget(hyperStatEquip.co2Target.readPriorityVal().toInt())
            .setPm25AlertTarget(hyperStatEquip.pm25Target.readPriorityVal().toInt())
            .setVocAlertTarget(4000)
            .setHyperstatLinearFanSpeeds(setLinearFanSpeedDetails(hyperStatEquip))
            .setHyperstatStagedFanSpeeds(setStagedFanSpeedDetails(hyperStatEquip))
            .setTemperatureMode(getTempMode())
            .setMiscSettings1(4) //  Sending value 4 because firmware expects it to be 4
            //sending MiscSettings1 always 001
            // bit 0: enableExternal10kTemperatureSensor
            // bit 1: disableTouch
            // bit 2: brightnessVariationEnable

            .build()
}

fun getHyperStatSettings2Message(equipRef: String): HyperStatSettingsMessage2_t {
    val settings2 = HyperStatSettingsMessage2_t.newBuilder()
    val hyperStatEquip = Domain.getDomainEquip(equipRef) as HyperStatEquip

    /*For monitoring Equip settings2 message not required.
    * CCU will crash if we try to send setting2 for monitoring
    * Because From proto fil we have only 6 values for Analog-in
    * But from CCU configuration we have till 11. */
    if(hyperStatEquip is MonitoringEquip){
        return settings2.build()
    }
    settings2.apply {
        enableForceOccupied = hyperStatEquip.autoForceOccupied.readDefaultVal() == 1.0
        enableAutoAway = hyperStatEquip.autoAway.readDefaultVal() == 1.0
        hyperstatRelayConfig = getRelayConfigs(hyperStatEquip)
        hyperstatAnalogOutConfig = getAnalogOutConfigs(hyperStatEquip)
        hyperstatAnalogInConfig = getAnalogInputConfigs(hyperStatEquip)
        thermistor1Enable = hyperStatEquip.thermistor1InputEnable.readDefaultVal() == 1.0
        thermistor2Enable = hyperStatEquip.thermistor2InputEnable.readDefaultVal() == 1.0
        zoneCO2Target = hyperStatEquip.co2Target.readDefaultVal().toInt()
        zoneCO2Threshold = hyperStatEquip.co2Threshold.readDefaultVal().toInt()
        zoneCO2DamperOpeningRate = hyperStatEquip.co2DamperOpeningRate.readDefaultVal().toInt()
        proportionalConstant = (TunerUtil.getProportionalGain(equipRef) * 100).toInt()
        integralConstant = (TunerUtil.getIntegralGain(equipRef) * 100).toInt()
        proportionalTemperatureRange = (TunerUtil.getProportionalSpread(equipRef) * 10).toInt()
        integrationTime = TunerUtil.getIntegralTimeout(equipRef).toInt()
    }

    when (hyperStatEquip) {
        is CpuV2Equip -> settings2.profile = HyperStat.HyperStatProfiles_t.HYPERSTAT_PROFILE_CONVENTIONAL_PACKAGE_UNIT
        is HpuV2Equip -> settings2.profile = HyperStat.HyperStatProfiles_t.HYPERSTAT_PROFILE_HEAT_PUMP_UNIT
        is Pipe2V2Equip -> settings2.profile = HyperStat.HyperStatProfiles_t.HYPERSTAT_PROFILE_2_PIPE_FANCOIL_UNIT

    }
    return settings2.build()
}

private fun getStagedFanDetails(equip: CpuV2Equip): HyperStat.HyperStatConfigsCpu_t {
    return HyperStat.HyperStatConfigsCpu_t.newBuilder().apply {
        coolingStage1FanAnalogVoltage = (equip.fanOutCoolingStage1.readPriorityVal() * 10).toInt()
        coolingStage2FanAnalogVoltage = (equip.fanOutCoolingStage2.readPriorityVal() * 10).toInt()
        coolingStage3FanAnalogVoltage = (equip.fanOutCoolingStage3.readPriorityVal() * 10).toInt()
        heatingStage1FanAnalogVoltage = (equip.fanOutHeatingStage1.readPriorityVal() * 10).toInt()
        heatingStage2FanAnalogVoltage = (equip.fanOutHeatingStage2.readPriorityVal() * 10).toInt()
        heatingStage3FanAnalogVoltage = (equip.fanOutHeatingStage3.readPriorityVal() * 10).toInt()
        analogoutAtRecFanAnalogVoltage = ((CCUHsApi.getInstance()
                .readPointPriorityValByQuery("config and recirculate and equipRef == \"${equip.equipRef}\"") * 10)
                .toInt()) // this needs to be checked again.
    }.build()
}

fun getHyperStatSettings3Message(equipRef: String): HyperStatSettingsMessage3_t {
    val hyperStatEquip = Domain.getDomainEquip(equipRef) as HyperStatEquip
    return HyperStatSettingsMessage3_t.newBuilder().apply {
        genertiTuners = getCommonTuners(equipRef)
        when (hyperStatEquip) {
            is CpuV2Equip -> hyperStatConfigsCpu = getStagedFanDetails(hyperStatEquip)
            is HpuV2Equip -> fcuTuners = getHpuTunerDetails(equipRef)
            is Pipe2V2Equip -> fcuTuners = getFcuTunerDetails(equipRef)
        }
    }.build()
}


private fun getCommonTuners(equipRef: String): HyperStat.HyperStatTunersGeneric_t {
    val equip = CCUHsApi.getInstance().readHDictById(equipRef)
    return HyperStat.HyperStatTunersGeneric_t.newBuilder().apply {
        unoccupiedSetback = (CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and zone and unoccupied and setback and roomRef == \"" + equip.get("roomRef").toString() + "\"") * 10).toInt()
        minFanRuntimePostconditioning = getTunerByDomain(DomainName.minFanRuntimePostConditioning, equipRef).toInt()
        relayActivationHysteresis = TunerUtil.getHysteresisPoint("hysteresis and activation", equipRef).toInt()
        analogFanSpeedMultiplier = (getTunerByDomain(DomainName.standaloneAnalogFanSpeedMultiplier, equipRef) * 10).toInt()
        humidityHysteresis = getTunerByDomain(DomainName.standaloneHumidityHysteresis, equipRef).toInt()
        autoAwayZoneSetbackTemp = (getTunerByDomain(DomainName.autoAwaySetback, equipRef) * 10).toInt()
        autoAwayTime = getTunerByDomain(DomainName.autoAwayTime, equipRef).toInt()
        forcedOccupiedTime = getTunerByDomain(DomainName.forcedOccupiedTime, equipRef).toInt()
    }.build()
}


private fun getHpuTunerDetails(equipRef: String): HyperStat.HyperStatTunersFcu_t {
    return HyperStat.HyperStatTunersFcu_t.newBuilder().apply {
        auxHeating1Activate = getTunerByDomain(DomainName.auxHeating1Activate, equipRef).toInt()
        auxHeating2Activate = getTunerByDomain(DomainName.auxHeating2Activate, equipRef).toInt()
    }.build()
}

private fun getFcuTunerDetails(equipRef: String): HyperStat.HyperStatTunersFcu_t {

    return HyperStat.HyperStatTunersFcu_t.newBuilder().apply {
        auxHeating1Activate = getTunerByDomain(DomainName.auxHeating1Activate, equipRef).toInt()
        auxHeating2Activate = getTunerByDomain(DomainName.auxHeating2Activate, equipRef).toInt()
        twoPipeHeatingThreshold = getTunerByDomain(DomainName.pipe2FancoilHeatingThreshold, equipRef).toInt()
        twoPipeCoolingThreshold = getTunerByDomain(DomainName.pipe2FancoilCoolingThreshold, equipRef).toInt()
        waterValueSamplingOnTime = getTunerByDomain(DomainName.waterValveSamplingOnTime, equipRef).toInt()
        watreValueSamplingWaitTime = getTunerByDomain(DomainName.waterValveSamplingWaitTime, equipRef).toInt()
        waterValveSamplingDuringLoopDeadbandOnTime = getTunerByDomain(DomainName.waterValveSamplingLoopDeadbandOnTime, equipRef).toInt()
        waterValveSamplingDuringLoopDeadbandWaitTime = getTunerByDomain(DomainName.waterValveSamplingLoopDeadbandWaitTime, equipRef).toInt()
    }.build()
}

private fun getTunerByDomain(domainName: String, equipRef: String) = TunerUtil.readTunerValByQuery("domainName ==\"$domainName\"", equipRef)



