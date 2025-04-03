package a75f.io.logic.bo.building.mystat.profiles.packageunit

import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.MyStatFanStages
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.mystat.configs.MyStatCpuAnalogOutMapping
import a75f.io.logic.bo.building.mystat.configs.MyStatCpuRelayMapping
import a75f.io.logic.bo.building.mystat.configs.MyStatHpuAnalogOutMapping
import a75f.io.logic.bo.building.mystat.configs.MyStatHpuRelayMapping
import a75f.io.logic.bo.building.mystat.profiles.MyStatProfile
import a75f.io.logic.bo.building.mystat.profiles.util.putPointToMap
import a75f.io.logic.bo.building.mystat.profiles.util.updateLogicalPoint

/**
 * Created by Manjunath K on 17-01-2025.
 */

abstract class MyStatPackageUnitProfile: MyStatProfile() {

    private var fanEnabledStatus = false

    private var lowestStageFanLow = false
    private var lowestStageFanHigh = false

    fun setFanEnabledStatus(status: Boolean) {
        fanEnabledStatus = status
    }

    fun resetFanLowestFanStatus() {
        lowestStageFanLow = false
        lowestStageFanHigh = false
    }

    fun setFanLowestFanLowStatus(status: Boolean) {
        lowestStageFanLow = status
    }

    fun setFanLowestFanHighStatus(status: Boolean) {
        lowestStageFanHigh = status
    }



    fun doFanLowSpeed(
        logicalPointId: String,
        fanMode: MyStatFanStages,
        fanLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>,
        doorWindowOperate: Boolean
    ) {

        var relayState = -1.0
        if (fanMode == MyStatFanStages.AUTO) {
            if (fanLoopOutput > relayActivationHysteresis)
                relayState = 1.0
            else if (fanLoopOutput == 0)
                relayState = 0.0

            // For Title 24 compliance, set relay when one of the fan is mapped to enabled and loop > 0
            // Or check if doorwindowStatus is true and lowest stage is fanLow
            if ((fanEnabledStatus && (fanLoopOutput > 0) && lowestStageFanLow) ||
                (lowestStageFanLow && doorWindowOperate)) {
                relayState = 1.0
            }

        } else {
            relayState = 1.0
        }
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointId, relayState)
        }
        if (getCurrentLogicalPointStatus(logicalPointId) == 1.0) {
            relayStages[Stage.FAN_1.displayName] = 1
        }
    }

    fun doFanHighSpeed(
        logicalPointId: String,
        fanMode: MyStatFanStages,
        fanLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>,
        doorWindowOperate: Boolean
    ) {
        var relayState = -1
        if (fanMode == MyStatFanStages.AUTO) {
            if (fanLoopOutput > (50 + (relayActivationHysteresis / 2)))
                relayState = 1
            if (fanLoopOutput <= (50 - (relayActivationHysteresis / 2)))
                relayState = 0

            // For Title 24 compliance, check if fanEnabled is mapped and fanHigh is the lowest
            // Or check if doorwindowStatus is true and lowest stage is fanHigh
            if ((fanEnabledStatus && (fanLoopOutput > 0) && lowestStageFanHigh) ||
                (lowestStageFanHigh && doorWindowOperate)) {
                relayState = 1
            }

        } else {
            relayState = if (fanMode == MyStatFanStages.HIGH_CUR_OCC
                || fanMode == MyStatFanStages.HIGH_OCC
                || fanMode == MyStatFanStages.HIGH_ALL_TIME
            ) 1 else 0
        }

        if (relayState != -1) {
            updateLogicalPoint(logicalPointId, relayState.toDouble())
        }
        if (getCurrentLogicalPointStatus(logicalPointId) == 1.0) {
            relayStages[Stage.FAN_2.displayName] = 1
        }
    }


    fun doCoolingStage1(port: Port, coolingLoopOutput: Int, relayActivationHysteresis: Int, relayStages: HashMap<String, Int>) {
        var relayState = -1.0
        if (coolingLoopOutput > relayActivationHysteresis)
            relayState = 1.0
        if (coolingLoopOutput == 0)
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointsList[port]!!, relayState)
        }
        if (getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            relayStages[Stage.COOLING_1.displayName] = 1
        }
    }

    fun doCoolingStage2(port: Port, coolingLoopOutput: Int, relayActivationHysteresis: Int, divider: Int, relayStages: HashMap<String, Int>) {
        var relayState = -1.0
        if (coolingLoopOutput > (divider + (relayActivationHysteresis / 2)))
            relayState = 1.0
        if (coolingLoopOutput <= (divider - (relayActivationHysteresis / 2)))
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointsList[port]!!, relayState)
        }
        if (getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            relayStages[Stage.COOLING_2.displayName] = 1
        }
    }

    fun doHeatingStage1(port: Port, heatingLoopOutput: Int, relayActivationHysteresis: Int, relayStages: HashMap<String, Int>) {
        var relayState = -1.0
        if (heatingLoopOutput > relayActivationHysteresis)
            relayState = 1.0
        if (heatingLoopOutput == 0)
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointsList[port]!!, relayState)
        }
        if (getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            relayStages[Stage.HEATING_1.displayName] = 1
        }
    }

    fun doHeatingStage2(
        port: Port, heatingLoopOutput: Int, relayActivationHysteresis: Int,
        divider: Int, relayStages: HashMap<String, Int>
    ) {
        var relayState = -1.0
        if (heatingLoopOutput > (divider + (relayActivationHysteresis / 2)))
            relayState = 1.0
        if (heatingLoopOutput <= (divider - (relayActivationHysteresis / 2)))
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointsList[port]!!, relayState)
        }
        if (getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            relayStages[Stage.HEATING_2.displayName] = 1
        }
    }

    fun doCompressorStage1(port: Port, compressorLoopOutput: Int, relayActivationHysteresis: Int, relayStages: HashMap<String, Int>, zoneMode: ZoneState) {
        var relayState = -1.0
        if (compressorLoopOutput > relayActivationHysteresis)
            relayState = 1.0
        if (compressorLoopOutput == 0)
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointsList[port]!!, relayState)
        }
        if (getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            if (zoneMode == ZoneState.COOLING)
                relayStages[Stage.COOLING_1.displayName] = 1
            if (zoneMode == ZoneState.HEATING)
                relayStages[Stage.HEATING_1.displayName] = 1
        }
    }

    fun doCompressorStage2(
        port: Port, compressorLoopOutput: Int, relayActivationHysteresis: Int,
        divider: Int, relayStages: HashMap<String, Int>, zoneMode: ZoneState
    ) {
        var relayState = -1.0
        if (compressorLoopOutput > (divider + (relayActivationHysteresis / 2)))
            relayState = 1.0
        if (compressorLoopOutput <= (divider - (relayActivationHysteresis / 2)))
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointsList[port]!!, relayState)
        }
        if (getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            if (zoneMode == ZoneState.COOLING)
                relayStages[Stage.COOLING_2.displayName] = 1
            if (zoneMode == ZoneState.HEATING)
                relayStages[Stage.HEATING_2.displayName] = 1
        }
    }

    fun doAnalogCooling(port: Port, conditioningMode: StandaloneConditioningMode, analogOutStages: HashMap<String, Int>, coolingLoopOutput: Int) {
        if (conditioningMode.ordinal == StandaloneConditioningMode.COOL_ONLY.ordinal || conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal) {
            updateLogicalPoint(logicalPointsList[port]!!, coolingLoopOutput.toDouble())
            if (coolingLoopOutput > 0) analogOutStages[AnalogOutput.COOLING.name] = coolingLoopOutput
        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }

    fun doAnalogHeating(
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        heatingLoopOutput: Int
    ) {
        if (conditioningMode.ordinal == StandaloneConditioningMode.HEAT_ONLY.ordinal ||
            conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
        ) {
            updateLogicalPoint(logicalPointsList[port]!!, heatingLoopOutput.toDouble())
            if (heatingLoopOutput > 0) analogOutStages[AnalogOutput.HEATING.name] =
                heatingLoopOutput
        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }

    fun doAnalogCompressorSpeed(port: Port, conditioningMode: StandaloneConditioningMode, analogOutStages: HashMap<String, Int>, compressorLoopOutput: Int, zoneMode: ZoneState) {
        if (conditioningMode != StandaloneConditioningMode.OFF) {
            updateLogicalPoint(logicalPointsList[port]!!, compressorLoopOutput.toDouble())
            if (compressorLoopOutput > 0) {
                if (zoneMode == ZoneState.COOLING) analogOutStages[AnalogOutput.COOLING.name] = compressorLoopOutput
                if (zoneMode == ZoneState.HEATING) analogOutStages[AnalogOutput.HEATING.name] = compressorLoopOutput
            }
        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }


    fun doAnalogFanAction(
        port: Port,
        fanLowPercent: Int,
        fanHighPercent: Int,
        fanMode: MyStatFanStages,
        conditioningMode: StandaloneConditioningMode,
        fanLoopOutput: Int,
        analogOutStages: HashMap<String, Int>,
    ) {
        if (fanMode != MyStatFanStages.OFF) {
            var fanLoopForAnalog = 0
            if (fanMode == MyStatFanStages.AUTO) {
                if (conditioningMode == StandaloneConditioningMode.OFF) {
                    updateLogicalPoint(logicalPointsList[port]!!, 0.0)
                    return
                }
                fanLoopForAnalog = fanLoopOutput
            } else {
                when {
                    (fanMode == MyStatFanStages.LOW_CUR_OCC
                            || fanMode == MyStatFanStages.LOW_OCC
                            || fanMode == MyStatFanStages.LOW_ALL_TIME) -> {
                        fanLoopForAnalog = fanLowPercent
                    }
                    (fanMode == MyStatFanStages.HIGH_CUR_OCC
                            || fanMode == MyStatFanStages.HIGH_OCC
                            || fanMode == MyStatFanStages.HIGH_ALL_TIME) -> {
                        fanLoopForAnalog = fanHighPercent
                    }
                }
            }
            if (fanLoopForAnalog > 0) analogOutStages[AnalogOutput.FAN_SPEED.name] =
                fanLoopForAnalog
            updateLogicalPoint(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
            CcuLog.i(L.TAG_CCU_HSHST, "$port = Linear Fan Speed  analogSignal   $fanLoopForAnalog")
        }
    }
}

fun getMyStatCpuRelayOutputPoints(equip: MyStatCpuEquip): HashMap<Int, String> {
    val relayStatus: HashMap<Int, String> = HashMap()

    putPointToMap(equip.coolingStage1, relayStatus, MyStatCpuRelayMapping.COOLING_STAGE_1.ordinal)
    putPointToMap(equip.coolingStage2, relayStatus, MyStatCpuRelayMapping.COOLING_STAGE_2.ordinal)
    putPointToMap(equip.heatingStage1, relayStatus, MyStatCpuRelayMapping.HEATING_STAGE_1.ordinal)
    putPointToMap(equip.heatingStage2, relayStatus, MyStatCpuRelayMapping.HEATING_STAGE_2.ordinal)
    putPointToMap(equip.fanLowSpeed, relayStatus, MyStatCpuRelayMapping.FAN_LOW_SPEED.ordinal)
    putPointToMap(equip.fanHighSpeed, relayStatus, MyStatCpuRelayMapping.FAN_HIGH_SPEED.ordinal)
    putPointToMap(equip.fanEnable, relayStatus, MyStatCpuRelayMapping.FAN_ENABLED.ordinal)
    putPointToMap(equip.occupiedEnable, relayStatus, MyStatCpuRelayMapping.OCCUPIED_ENABLED.ordinal)
    putPointToMap(equip.humidifierEnable, relayStatus, MyStatCpuRelayMapping.HUMIDIFIER.ordinal)
    putPointToMap(equip.dehumidifierEnable, relayStatus, MyStatCpuRelayMapping.DEHUMIDIFIER.ordinal)
    putPointToMap(equip.dcvDamper, relayStatus, MyStatCpuRelayMapping.DCV_DAMPER.ordinal)
    return relayStatus
}
fun getMyStatCpuAnalogOutputPoints(equip: MyStatCpuEquip): HashMap<Int, String> {
    val analogOutputPoints: HashMap<Int, String> = HashMap()
    putPointToMap(equip.coolingSignal, analogOutputPoints, MyStatCpuAnalogOutMapping.COOLING.ordinal)
    putPointToMap(equip.linearFanSpeed, analogOutputPoints, MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal)
    putPointToMap(equip.heatingSignal, analogOutputPoints, MyStatCpuAnalogOutMapping.HEATING.ordinal)
    putPointToMap(equip.stagedFanSpeed, analogOutputPoints, MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal)
    putPointToMap(equip.stagedFanSpeed, analogOutputPoints, MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal)
    putPointToMap(equip.dcvDamperModulating, analogOutputPoints, MyStatCpuAnalogOutMapping.DCV_DAMPER.ordinal)
    return analogOutputPoints
}

fun getMyStatHpuRelayOutputPoints(equip: MyStatHpuEquip): HashMap<Int, String> {
    val relayStatus: HashMap<Int, String> = HashMap()

    putPointToMap(equip.compressorStage1, relayStatus, MyStatHpuRelayMapping.COMPRESSOR_STAGE1.ordinal)
    putPointToMap(equip.compressorStage2, relayStatus, MyStatHpuRelayMapping.COMPRESSOR_STAGE2.ordinal)
    putPointToMap(equip.auxHeatingStage1, relayStatus, MyStatHpuRelayMapping.AUX_HEATING_STAGE1.ordinal)
    putPointToMap(equip.fanLowSpeed, relayStatus, MyStatHpuRelayMapping.FAN_LOW_SPEED.ordinal)
    putPointToMap(equip.fanHighSpeed, relayStatus, MyStatHpuRelayMapping.FAN_HIGH_SPEED.ordinal)
    putPointToMap(equip.fanEnable, relayStatus, MyStatHpuRelayMapping.FAN_ENABLED.ordinal)
    putPointToMap(equip.occupiedEnable, relayStatus, MyStatHpuRelayMapping.OCCUPIED_ENABLED.ordinal)
    putPointToMap(equip.humidifierEnable, relayStatus, MyStatHpuRelayMapping.HUMIDIFIER.ordinal)
    putPointToMap(equip.dehumidifierEnable, relayStatus, MyStatHpuRelayMapping.DEHUMIDIFIER.ordinal)
    putPointToMap(equip.changeOverCooling, relayStatus, MyStatHpuRelayMapping.CHANGE_OVER_O_COOLING.ordinal)
    putPointToMap(equip.changeOverHeating, relayStatus, MyStatHpuRelayMapping.CHANGE_OVER_B_HEATING.ordinal)
    putPointToMap(equip.dcvDamper, relayStatus, MyStatHpuRelayMapping.DCV_DAMPER.ordinal)

    return relayStatus
}

fun getMyStatHpuAnalogOutputPoints(equip: MyStatHpuEquip): HashMap<Int, String> {
    val analogOutputPoints: HashMap<Int, String> = HashMap()
    putPointToMap(equip.compressorSpeed, analogOutputPoints, MyStatHpuAnalogOutMapping.COMPRESSOR_SPEED.ordinal)
    putPointToMap(equip.dcvDamperModulating, analogOutputPoints, MyStatHpuAnalogOutMapping.DCV_DAMPER_MODULATION.ordinal)
    putPointToMap(equip.fanSignal, analogOutputPoints, MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal)
    return analogOutputPoints
}