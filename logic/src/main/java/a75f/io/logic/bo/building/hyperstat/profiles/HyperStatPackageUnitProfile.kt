package a75f.io.logic.bo.building.hyperstat.profiles

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage

/**
 * Created by Manjunath K on 22-07-2022.
 */

abstract class HyperStatPackageUnitProfile: HyperStatProfile(){

    private var fanEnabledStatus = false
    /* Flags for checking if the current stage is lowest
    *  Eg: If FAN MEDIUM and FAN HIGH is used, then FAN MEDIUM is the lowest stage */
    private var lowestStageFanLow = false
    private var lowestStageFanMedium = false
    private var lowestStageFanHigh = false

    /**
     * Sets the enabled status of the fan.
     *
     * @param status `true` if the fan is enabled, `false` otherwise.
     */
    fun setFanEnabledStatus(status: Boolean) {
        fanEnabledStatus = status
    }

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

        var relayState = -1.0
        if (fanMode == StandaloneFanStage.AUTO) {
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
            updateLogicalPointIdValue(logicalPointId, relayState)
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

        var relayState = -1.0
        if (fanMode == StandaloneFanStage.AUTO) {
            if (fanLoopOutput > (divider + (relayActivationHysteresis / 2)))
                relayState = 1.0
            if (fanLoopOutput <= (divider - (relayActivationHysteresis / 2)))
                relayState = 0.0

            // For Title 24 compliance, check if fanEnabled is mapped and fanMedium is the lowest
            // Or check if doorwindowStatus is true and lowest stage is fanMedium
            if ((fanEnabledStatus && (fanLoopOutput > 0) && lowestStageFanMedium) ||
                (lowestStageFanMedium && doorWindowOperate)) {
                relayState = 1.0
            }

        } else {
            relayState = if (fanMode == StandaloneFanStage.MEDIUM_CUR_OCC
                || fanMode == StandaloneFanStage.MEDIUM_OCC
                || fanMode == StandaloneFanStage.MEDIUM_ALL_TIME
                || fanMode == StandaloneFanStage.HIGH_CUR_OCC
                || fanMode == StandaloneFanStage.HIGH_OCC
                || fanMode == StandaloneFanStage.HIGH_ALL_TIME
            ) 1.0 else 0.0
        }
        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointId, relayState)
            CcuLog.i(L.TAG_CCU_HSPIPE2, "$logicalPointId = FanMediumSpeed:  $relayState")
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
        var relayState = -1.0
        if (fanMode == StandaloneFanStage.AUTO) {
            if (fanLoopOutput > (66 + (relayActivationHysteresis / 2)))
                relayState = 1.0
            if (fanLoopOutput <= (66 - (relayActivationHysteresis / 2)))
                relayState = 0.0

            // For Title 24 compliance, check if fanEnabled is mapped and fanHigh is the lowest
            // Or check if doorwindowStatus is true and lowest stage is fanHigh
            if ((fanEnabledStatus && (fanLoopOutput > 0) && lowestStageFanHigh) ||
                (lowestStageFanHigh && doorWindowOperate)) {
                    relayState = 1.0
            }
        } else {
            relayState = if (fanMode == StandaloneFanStage.HIGH_CUR_OCC
                || fanMode == StandaloneFanStage.HIGH_OCC
                || fanMode == StandaloneFanStage.HIGH_ALL_TIME
            ) 1.0 else 0.0
        }
        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointId, relayState)
            CcuLog.i(L.TAG_CCU_HSPIPE2, "$logicalPointId = FanHighSpeed:  $relayState")
        }
        if (getCurrentLogicalPointStatus(logicalPointId) == 1.0) {
            relayStages[Stage.FAN_3.displayName] = 1
        }
    }


    /**
     * Performs analog fan action for the CPU.
     * This function updates the logical point values for fan control based on various parameters such as fan mode, conditioning mode, and occupancy status.
     * It also handles the Title 24 compliance by adjusting the fan runtime when transitioning from OCCUPIED to UNOCCUPIED status.
     *
     * @param port The port associated with the fan action.
     * @param fanLowPercent The percentage of fan speed for the low stage.
     * @param fanMediumPercent The percentage of fan speed for the medium stage.
     * @param fanHighPercent The percentage of fan speed for the high stage.
     * @param fanMode The mode of the fan (e.g., OFF, AUTO, LOW_CUR_OCC).
     * @param conditioningMode The conditioning mode (e.g., OFF, COOLING, HEATING).
     * @param fanLoopOutput The output value for the fan loop.
     * @param analogOutStages A HashMap containing analog output stages.
     */
    override fun doAnalogFanAction(
        port: Port,
        fanLowPercent: Int,
        fanMediumPercent: Int,
        fanHighPercent: Int,
        fanMode: StandaloneFanStage,
        conditioningMode: StandaloneConditioningMode,
        fanLoopOutput: Int,
        analogOutStages: HashMap<String, Int>,
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
            if (fanLoopForAnalog > 0) analogOutStages[AnalogOutput.FAN_SPEED.name] =
                fanLoopForAnalog
            updateLogicalPointIdValue(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
            CcuLog.i(L.TAG_CCU_HSCPU, "$port = Linear Fan Speed  analogSignal   $fanLoopForAnalog")
        }
    }


}
