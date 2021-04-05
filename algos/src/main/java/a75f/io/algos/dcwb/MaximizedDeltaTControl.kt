package a75f.io.algos.dcwb

import a75.io.algos.ControlLoop


class MaximizedDeltaTData( val outletWaterTemperature : Double,
                            val averageDesiredCoolingTemp : Double,
                            val chilledWaterExitTemperatureMargin : Double,
                            val piLoop : ControlLoop)


fun getChilledWaterMaximizedDeltaTValveLoop (data : MaximizedDeltaTData) : Double {

    val chilledWaterTargetExitTemperature = data.averageDesiredCoolingTemp - data.chilledWaterExitTemperatureMargin

    return if (data.outletWaterTemperature <= chilledWaterTargetExitTemperature) {
        val loopOp = data.piLoop.getLoopOutput(chilledWaterTargetExitTemperature, chilledWaterTargetExitTemperature)
        loopOp.coerceAtLeast(1.0)
    } else {
        data.piLoop.getLoopOutput(chilledWaterTargetExitTemperature, data.outletWaterTemperature)
    }
}