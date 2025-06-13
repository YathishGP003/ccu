package a75f.io.logic.bo.building.statprofiles.mystat.profiles.packageunit

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.mystat.profiles.MyStatProfile
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.updateLogicalPoint

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
            if (coolingLoopOutput > 0) analogOutStages[StatusMsgKeys.COOLING.name] = coolingLoopOutput
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
            if (heatingLoopOutput > 0) analogOutStages[StatusMsgKeys.HEATING.name] =
                heatingLoopOutput
        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }

    fun doAnalogCompressorSpeed(port: Port, conditioningMode: StandaloneConditioningMode, analogOutStages: HashMap<String, Int>, compressorLoopOutput: Int, zoneMode: ZoneState) {
        if (conditioningMode != StandaloneConditioningMode.OFF) {
            updateLogicalPoint(logicalPointsList[port]!!, compressorLoopOutput.toDouble())
            if (compressorLoopOutput > 0) {
                if (zoneMode == ZoneState.COOLING) analogOutStages[StatusMsgKeys.COOLING.name] = compressorLoopOutput
                if (zoneMode == ZoneState.HEATING) analogOutStages[StatusMsgKeys.HEATING.name] = compressorLoopOutput
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
            if (fanLoopForAnalog > 0) analogOutStages[StatusMsgKeys.FAN_SPEED.name] =
                fanLoopForAnalog
            updateLogicalPoint(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
            CcuLog.i(L.TAG_CCU_HSHST, "$port = Linear Fan Speed  analogSignal   $fanLoopForAnalog")
        }
    }
}
