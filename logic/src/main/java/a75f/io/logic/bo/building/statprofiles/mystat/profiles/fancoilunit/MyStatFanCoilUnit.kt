package a75f.io.logic.bo.building.statprofiles.mystat.profiles.fancoilunit

import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.mystat.profiles.MyStatProfile
import a75f.io.logic.bo.building.statprofiles.util.MyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.updateLogicalPoint

/**
 * Created by Manjunath K on 17-01-2025.
 */

abstract class MyStatFanCoilUnit: MyStatProfile() {

    /* Flags for checking if the current stage is lowest
    *  Eg: If FAN MEDIUM and FAN HIGH is used, then FAN MEDIUM is the lowest stage */
    private var lowestStageFanLow = false
    private var lowestStageFanHigh = false

    /**
     * Sets the status of the lowest fan low stage.
     *
     * @param status `true` if the lowest fan low stage is enabled, `false` otherwise.
     */
    fun setFanLowestFanLowStatus(status: Boolean) {
        lowestStageFanLow = status
    }

    /**
     * Sets the status of the lowest fan medium stage.
     *
     * @param status `true` if the lowest fan medium stage is enabled, `false` otherwise.
     */
    fun setFanLowestFanHighStatus(status: Boolean) {
        lowestStageFanHigh = status
    }

    fun resetFanStatus(){
        lowestStageFanLow = false
        lowestStageFanHigh = false
    }

    fun doFanLowSpeed(
        logicalPointId: String,
        highLogicalPoint: String?,
        fanMode: MyStatFanStages,
        fanLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>,
        doorWindowOperate: Boolean
    ) {
        var relayState = -1
        if (fanMode == MyStatFanStages.AUTO) {
            if(fanLoopOutput > relayActivationHysteresis)
                relayState = 1
            else if(fanLoopOutput == 0) {
                relayState = 0
            }
            if((!highLogicalPoint.isNullOrEmpty() && getCurrentLogicalPointStatus(highLogicalPoint) == 1.0))
                relayState = 0

            // For Title 24 compliance check if doorwindowStatus is true and lowest stage is fanLow
            if(lowestStageFanLow && doorWindowOperate) {
                relayState = 1
            }

        } else {
            relayState =  if (fanMode == MyStatFanStages.LOW_CUR_OCC
                || fanMode == MyStatFanStages.LOW_OCC
                || fanMode == MyStatFanStages.LOW_ALL_TIME
            ) 1 else 0
        }
        if(relayState != -1){
            updateLogicalPoint(logicalPointId, relayState.toDouble())
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
            if(fanLoopOutput <= (50 - (relayActivationHysteresis / 2)))
                relayState = 0

            // For Title 24 compliance check if doorwindowStatus is true and lowest stage is fanHigh
            if(lowestStageFanHigh && doorWindowOperate) {
                relayState = 1
            }

        } else {
            relayState =  if (fanMode == MyStatFanStages.HIGH_CUR_OCC
                || fanMode == MyStatFanStages.HIGH_OCC
                || fanMode == MyStatFanStages.HIGH_ALL_TIME
            ) 1 else 0
        }

        if(relayState != -1){
            updateLogicalPoint(logicalPointId, relayState.toDouble())
        }
        if (getCurrentLogicalPointStatus(logicalPointId) == 1.0) {
            relayStages[Stage.FAN_2.displayName] = 1
        }
    }

    // Analog Fan operation is common for all the modules
    fun doAnalogFanAction(
        port: Port,
        fanLowPercent: Int,
        fanHighPercent: Int,
        fanMode: MyStatFanStages,
        conditioningMode: StandaloneConditioningMode,
        fanLoopOutput: Int,
        analogOutStages: HashMap<String, Int>
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
            if (fanLoopForAnalog > 0) analogOutStages[StatusMsgKeys.FAN_SPEED.name] = 1 else analogOutStages.remove(StatusMsgKeys.FAN_SPEED.name)
            updateLogicalPoint(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
        }
    }


    fun doRelayWaterValveOperation(
        equip: MyStatPipe2Equip,
        port: Port,
        basicSettings: MyStatBasicSettings,
        loopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>
    ) {
        var relayState = -1.0
        if (basicSettings.conditioningMode != StandaloneConditioningMode.OFF && basicSettings.fanMode != MyStatFanStages.OFF) {
            if (loopOutput > relayActivationHysteresis) {
                relayState = 1.0
                equip.lastWaterValveTurnedOnTime = System.currentTimeMillis()
            } else if (loopOutput == 0)
                relayState = 0.0
        } else {
            relayState = 0.0
        }
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointsList[port]!!, relayState)
        }
        // show status message
        if (getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            relayStages[StatusMsgKeys.WATER_VALVE.name] = 1
        }
    }

    fun doAnalogWaterValveAction(
        port: Port,
        basicSettings: MyStatBasicSettings,
        loopOutput: Int,
        analogOutStages: HashMap<String, Int>
    ) {
        if (basicSettings.conditioningMode != StandaloneConditioningMode.OFF && basicSettings.fanMode != MyStatFanStages.OFF) {
            updateLogicalPoint(logicalPointsList[port]!!, loopOutput.toDouble())
            if (loopOutput > 0) analogOutStages[StatusMsgKeys.WATER_VALVE.name] = 1
        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }
}
