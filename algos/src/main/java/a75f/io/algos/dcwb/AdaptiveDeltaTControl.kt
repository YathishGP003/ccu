package a75f.io.algos.dcwb

import a75.io.algos.ControlLoop
import android.util.Log

data class AdaptiveDeltaTDto (val inletWaterTemperature : Double,
                          val chilledWaterTargetDeltaT : Double,
                          val adaptiveComfortThresholdMargin : Double,
                          val averageDesiredCoolingTemp : Double,
                          val piLoop : ControlLoop)

/**
 * Implements Adaptive Delta T algorithm to control Chiller.
 * We use PI to control the delta t to 15F  as long as water exit temp from AHU is under 70F (or 4 F below the average
 * desired cooling temperature this is called adaptiveComfortThreshold). If exit temp is greater than
 * adaptiveComfortThreshold, we proportionally increase  the valve opening to 100% from adaptiveComfortThreshold
 * to average desired cooling temperature
 */

class AdaptiveDeltaTControl {
    companion object Algo{

        var linearModeLoop : Boolean = false;

        fun getChilledWaterAdaptiveDeltaTValveLoop(data: AdaptiveDeltaTDto): Double {
            Log.i("CCU_SYSTEM", " getChilledWaterAdaptiveDeltaTValveLoop $data")
            data.piLoop.dump()
            val adaptiveComfortThreshold = data.averageDesiredCoolingTemp - data.adaptiveComfortThresholdMargin

            val chilledWaterTargetTemp = data.inletWaterTemperature + data.chilledWaterTargetDeltaT
            return if (chilledWaterTargetTemp < adaptiveComfortThreshold) {
                if(linearModeLoop) {
                    linearModeLoop = false
                    data.piLoop.loopOutput;
                } else {
                    data.piLoop.getLoopOutput(adaptiveComfortThreshold, chilledWaterTargetTemp)
                }
            } else {
                linearModeLoop = true
                val chilledWaterDeltaTValveLoop = data.piLoop.loopOutput

                val linearIncrementRange = 100 - chilledWaterDeltaTValveLoop
                val chilledWaterTargetLinearDelta = chilledWaterTargetTemp - adaptiveComfortThreshold
                chilledWaterDeltaTValveLoop + linearIncrementRange * chilledWaterTargetLinearDelta / data.adaptiveComfortThresholdMargin
            }
        }
    }
}
