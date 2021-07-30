package a75f.io.algos.dcwb

import a75.io.algos.ControlLoop
import android.util.Log


data class MaximizedDeltaTInput( val outletWaterTemperature : Double,
                            val averageDesiredCoolingTemp : Double,
                            val chilledWaterExitTemperatureMargin : Double,
                            val piLoop : ControlLoop)


/**
 * Implements algorith to control chillers based on Maximized Delta T approach.
 * This is used for maximizing delta T with an exit temp that is as high as needed to give the comfort that
 * is needed. Throttle the valve to its minimum position (typically 1%) as long as exit water temperature
 * from the AHU (CHWR) is 4 F below the desired cooling temp. If exit temp is greater than that, we us PI
 * to open the valve to 100% from 4 deg below desired temperature to same as desired cooling temperature .
 */
class MaximizedDeltaTControl {

    companion object {
        @JvmStatic
        fun getChilledWaterMaximizedDeltaTValveLoop(data: MaximizedDeltaTInput): Double {
            Log.i("CCU_SYSTEM", " getChilledWaterAdaptiveDeltaTValveLoop $data")
            data.piLoop.dump()
            val chilledWaterTargetExitTemperature = data.averageDesiredCoolingTemp - data.chilledWaterExitTemperatureMargin

            return if (data.outletWaterTemperature < chilledWaterTargetExitTemperature) {
                data.piLoop.reset()
                1.0
            } else {
                data.piLoop.getLoopOutput(data.outletWaterTemperature, chilledWaterTargetExitTemperature)
            }
        }
    }
}