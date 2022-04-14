package a75f.io.logic.bo.building.hyperstat.common

import a75.io.algos.ControlLoop

/**
 * Created by Manjunath K on 16-08-2021.
 */
class HyperstatLoopController {

    private var coolingControlLoop = ControlLoop()
    private var heatingControlLoop = ControlLoop()

    fun initialise(tuners: HyperStatProfileTuners) {
        initialiseControlLoop(coolingControlLoop, tuners)
        initialiseControlLoop(heatingControlLoop, tuners)
    }

    private fun initialiseControlLoop(controlLoop: ControlLoop, tuners: HyperStatProfileTuners) {
        controlLoop.setProportionalGain(tuners.proportionalGain)
        controlLoop.setIntegralGain(tuners.integralGain)
        controlLoop.setProportionalSpread(tuners.proportionalSpread.toInt())
        controlLoop.setIntegralMaxTimeout(tuners.integralMaxTimeout)
        controlLoop.useNegativeCumulativeError(false)
    }

    /**
     * Calculate cooling loop output
     * resets previous cooling loop output if exist
     * Takes target and current value and calculates new loop output
     */
    fun calculateCoolingLoopOutput(currentValue: Double, targetValue: Double): Double {
        return coolingControlLoop.getLoopOutput(currentValue, targetValue)
    }

    /**
     * Calculate Heating loop output
     * resets previous Heating loop output if exist
     * Takes target and current value and calculates new loop output
     */
    fun calculateHeatingLoopOutput(targetValue: Double, currentValue: Double): Double {
        return  heatingControlLoop.getLoopOutput(targetValue, currentValue)
    }

    /**
     * Reset the cooling control
     */
    fun resetCoolingControl(){
       coolingControlLoop.setDisabled()
       coolingControlLoop.setEnabled()
    }


    /**
     * Reset the Heating control
     */
    fun resetHeatingControl(){
        heatingControlLoop.setDisabled()
        heatingControlLoop.setEnabled()
    }

    fun dumpLogs(){
        heatingControlLoop.dumpWithTag("CCU_CPU-HS")
        coolingControlLoop.dumpWithTag("CCU_CPU-HS")
    }
}