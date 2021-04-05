package a75f.io.algos.dcwb

import a75.io.algos.ControlLoop

class AdaptiveDeltaTData (val inletWaterTemperature : Double,
                          val chilledWaterTargetDeltaT : Double,
                          val adaptiveComfortThresholdMargin : Double,
                          val averageDesiredCoolingTemp : Double,
                          val piLoop : ControlLoop)



fun getChilledWaterAdaptiveDeltaTValveLoop (data : AdaptiveDeltaTData) : Double {
    val adaptiveComfortThreshold = data.averageDesiredCoolingTemp - data.adaptiveComfortThresholdMargin

    val chilledWaterTargetTemp = data.inletWaterTemperature + data.chilledWaterTargetDeltaT
    return if (chilledWaterTargetTemp < adaptiveComfortThreshold) {
        data.piLoop.getLoopOutput(adaptiveComfortThreshold, chilledWaterTargetTemp)
    } else {
        val chilledWaterDeltaTValveLoop = data.piLoop.loopOutput

        val linearIncrementRange = 100 - chilledWaterDeltaTValveLoop
        chilledWaterDeltaTValveLoop + linearIncrementRange * chilledWaterTargetTemp/data.adaptiveComfortThresholdMargin
    }
}