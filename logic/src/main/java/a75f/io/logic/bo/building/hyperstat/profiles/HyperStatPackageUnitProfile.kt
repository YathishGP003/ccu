package a75f.io.logic.bo.building.hyperstat.profiles

import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.HyperStatPipe2Equip
import android.util.Log

/**
 * Created by Manjunath K on 22-07-2022.
 */

abstract class HyperStatPackageUnitProfile: HyperStatProfile(){

    override fun doFanLowSpeed(
        logicalPointId: String,
        mediumLogicalPoint : String?,
        highLogicalPoint : String?,
        fanMode: StandaloneFanStage,
        fanLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>,
        divider: Int,
    ) {

        var relayState = -1.0
        if (fanMode == StandaloneFanStage.AUTO) {
            if (fanLoopOutput > relayActivationHysteresis)
                relayState = 1.0
            if (fanLoopOutput == 0)
                relayState = 0.0
        } else {
            relayState = 1.0
        }
        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointId, relayState)
            if (relayState == 1.0) {
                relayStages[Stage.FAN_1.displayName] = 1
            }
        }

    }

    override fun doFanMediumSpeed(
        logicalPointId: String,
        superLogicalPoint : String?,
        fanMode: StandaloneFanStage,
        fanLoopOutput: Int,
        relayActivationHysteresis: Int,
        divider: Int,
        relayStages: HashMap<String, Int>
    ) {

        var relayState = -1.0
        if (fanMode == StandaloneFanStage.AUTO) {
            if (fanLoopOutput > (divider + (relayActivationHysteresis / 2)))
                relayState = 1.0
            if (fanLoopOutput <= (divider - (relayActivationHysteresis / 2)))
                relayState = 0.0
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
            Log.i(L.TAG_CCU_HSPIPE2, "$logicalPointId = FanMediumSpeed:  $relayState")
            if (relayState == 1.0) {
                relayStages[Stage.FAN_2.displayName] = 1
            }
        }

    }

    override fun doFanHighSpeed(
        logicalPointId: String,
        fanMode: StandaloneFanStage,
        fanLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>
    ) {
        var relayState = -1.0
        if (fanMode == StandaloneFanStage.AUTO) {
            if (fanLoopOutput > (66 + (relayActivationHysteresis / 2)))
                relayState = 1.0
            if (fanLoopOutput <= (66 - (relayActivationHysteresis / 2)))
                relayState = 0.0
        } else {
            relayState = if (fanMode == StandaloneFanStage.HIGH_CUR_OCC
                || fanMode == StandaloneFanStage.HIGH_OCC
                || fanMode == StandaloneFanStage.HIGH_ALL_TIME
            ) 1.0 else 0.0
        }
        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointId, relayState)
            Log.i(L.TAG_CCU_HSPIPE2, "$logicalPointId = FanHighSpeed:  $relayState")
            if (relayState == 1.0) {
                relayStages[Stage.FAN_3.displayName] = 1
            }
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
            Log.i(L.TAG_CCU_HSCPU, "$port = Fan Speed  analogSignal   $fanLoopForAnalog")
        }
    }


}
