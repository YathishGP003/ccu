package a75f.io.logic.bo.building.hyperstat.profiles

import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage

/**
 * Created by Manjunath K on 01-08-2022.
 */

abstract class HyperStatFanCoilUnit: HyperStatProfile(){
    private var fanEnabledStatus = false
    /* Flags for checking if the current stage is lowest
    *  Eg: If FAN MEDIUM and FAN HIGH is used, then FAN MEDIUM is the lowest stage */
    private var lowestStageFanLow = false
    private var lowestStageFanMedium = false
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
     * @param status `true` if the lowest fan low stage is enabled, `false` otherwise.
     */
    fun setFanLowestFanMediumStatus(status: Boolean) {
        lowestStageFanMedium = status
    }

    /**
     * Sets the status of the lowest fan medium stage.
     *
     * @param status `true` if the lowest fan medium stage is enabled, `false` otherwise.
     */
    fun setFanLowestFanHighStatus(status: Boolean) {
        lowestStageFanHigh = status
    }

    override fun doFanLowSpeed(
        logicalPointId: String,
        mediumLogicalPoint : String?,
        highLogicalPoint : String?,
        fanMode: StandaloneFanStage,
        fanLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>,
        divider: Int,
        doorWindowOperate: Boolean
    ) {
        var relayState = -1
        if (fanMode == StandaloneFanStage.AUTO) {
            if(fanLoopOutput > relayActivationHysteresis)
                relayState = 1
            else if(fanLoopOutput == 0) {
                relayState = 0
            }
            if((!mediumLogicalPoint.isNullOrEmpty() && getCurrentLogicalPointStatus(mediumLogicalPoint) == 1.0))
                relayState = 0
            if((!highLogicalPoint.isNullOrEmpty() && getCurrentLogicalPointStatus(highLogicalPoint) == 1.0))
                relayState = 0

            // For Title 24 compliance check if doorwindowStatus is true and lowest stage is fanLow
            if(lowestStageFanLow && doorWindowOperate) {
                relayState = 1
            }

        } else {
                relayState =  if (fanMode == StandaloneFanStage.LOW_CUR_OCC
                || fanMode == StandaloneFanStage.LOW_OCC
                || fanMode == StandaloneFanStage.LOW_ALL_TIME
            ) 1 else 0
        }
        if(relayState != -1){
            updateLogicalPointIdValue(logicalPointId, relayState.toDouble())
        }
        if (getCurrentLogicalPointStatus(logicalPointId) == 1.0) {
            relayStages[Stage.FAN_1.displayName] = 1
        }

    }


    override fun doFanMediumSpeed(
        logicalPointId: String,
        superLogicalPoint : String?,
        fanMode: StandaloneFanStage,
        fanLoopOutput: Int,
        relayActivationHysteresis: Int,
        divider: Int,
        relayStages: HashMap<String, Int>,
        doorWindowOperate: Boolean
    ) {

        var relayState = -1
       if (fanMode == StandaloneFanStage.AUTO) {
           if(fanLoopOutput > (divider + (relayActivationHysteresis / 2)))
               relayState = 1
           if((!superLogicalPoint.isNullOrEmpty() && getCurrentLogicalPointStatus(superLogicalPoint) == 1.0))
               relayState = 0
           if(fanLoopOutput <= (33 - (relayActivationHysteresis / 2)))
                   relayState = 0

           // For Title 24 compliance check if doorwindowStatus is true and lowest stage is fanMedium
           if(lowestStageFanMedium && doorWindowOperate) {
               relayState = 1
           }

       } else {
           relayState =   if (fanMode == StandaloneFanStage.MEDIUM_CUR_OCC
                || fanMode == StandaloneFanStage.MEDIUM_OCC
                || fanMode == StandaloneFanStage.MEDIUM_ALL_TIME
            ) 1 else 0

        }

        if(relayState != -1){
            updateLogicalPointIdValue(logicalPointId, relayState.toDouble())
        }
        if (getCurrentLogicalPointStatus(logicalPointId) == 1.0) {
            relayStages[Stage.FAN_2.displayName] = 1
        }
    }

    override fun doFanHighSpeed(
        logicalPointId: String,
        fanMode: StandaloneFanStage,
        fanLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>,
        doorWindowOperate: Boolean
    ) {
        var relayState = -1
       if (fanMode == StandaloneFanStage.AUTO) {
            if (fanLoopOutput > (66 + (relayActivationHysteresis / 2)))
                relayState = 1
           if(fanLoopOutput <= (66 - (relayActivationHysteresis / 2)))
               relayState = 0

           // For Title 24 compliance check if doorwindowStatus is true and lowest stage is fanHigh
           if(lowestStageFanHigh && doorWindowOperate) {
               relayState = 1
           }

       } else {
            relayState =  if (fanMode == StandaloneFanStage.HIGH_CUR_OCC
                || fanMode == StandaloneFanStage.HIGH_OCC
                || fanMode == StandaloneFanStage.HIGH_ALL_TIME
            ) 1 else 0
        }

        if(relayState != -1){
            updateLogicalPointIdValue(logicalPointId, relayState.toDouble())
        }
        if (getCurrentLogicalPointStatus(logicalPointId) == 1.0) {
            relayStages[Stage.FAN_3.displayName] = 1
        }
    }

    // Analog Fan operation is common for all the modules
    override fun doAnalogFanAction(
            port: Port,
            fanLowPercent: Int,
            fanMediumPercent: Int,
            fanHighPercent: Int,
            fanMode: StandaloneFanStage,
            conditioningMode: StandaloneConditioningMode,
            fanLoopOutput: Int,
            analogOutStages: HashMap<String, Int>
    ) {
        if (fanMode != StandaloneFanStage.OFF) {
            var fanLoopForAnalog = 0
            if (fanMode == StandaloneFanStage.AUTO) {
                if (conditioningMode == StandaloneConditioningMode.OFF) {
                    updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
                    return
                }
                fanLoopForAnalog = fanLoopOutput
            } else {
                when {
                    (fanMode == StandaloneFanStage.LOW_CUR_OCC
                            || fanMode == StandaloneFanStage.LOW_OCC
                            || fanMode == StandaloneFanStage.LOW_ALL_TIME) -> {
                        fanLoopForAnalog = fanLowPercent
                    }

                    (fanMode == StandaloneFanStage.MEDIUM_CUR_OCC
                            || fanMode == StandaloneFanStage.MEDIUM_OCC
                            || fanMode == StandaloneFanStage.MEDIUM_ALL_TIME) -> {
                        fanLoopForAnalog = fanMediumPercent
                    }

                    (fanMode == StandaloneFanStage.HIGH_CUR_OCC
                            || fanMode == StandaloneFanStage.HIGH_OCC
                            || fanMode == StandaloneFanStage.HIGH_ALL_TIME) -> {
                        fanLoopForAnalog = fanHighPercent
                    }
                }
            }
            if (fanLoopForAnalog > 0) analogOutStages[AnalogOutput.FAN_SPEED.name] = 1 else analogOutStages.remove(AnalogOutput.FAN_SPEED.name)
            updateLogicalPointIdValue(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
        }
    }


}