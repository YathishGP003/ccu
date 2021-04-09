package a75f.io.algos.dcwb

import a75.io.algos.ControlLoop
import android.util.Log


data class MaximizedDeltaTDto( val outletWaterTemperature : Double,
                            val averageDesiredCoolingTemp : Double,
                            val chilledWaterExitTemperatureMargin : Double,
                            val piLoop : ControlLoop)


class MaximizedDeltaTControl {
    companion object Algo{
        fun getChilledWaterMaximizedDeltaTValveLoop(data: MaximizedDeltaTDto): Double {
            Log.i("CCU_SYSTEM", " getChilledWaterAdaptiveDeltaTValveLoop $data")
            val chilledWaterTargetExitTemperature = data.averageDesiredCoolingTemp - data.chilledWaterExitTemperatureMargin

            return if (data.outletWaterTemperature <= chilledWaterTargetExitTemperature) {
                val loopOp = data.piLoop.getLoopOutput(chilledWaterTargetExitTemperature, chilledWaterTargetExitTemperature)
                loopOp.coerceAtLeast(1.0)
            } else {
                data.piLoop.getLoopOutput(chilledWaterTargetExitTemperature, data.outletWaterTemperature)
            }
        }
    }
}