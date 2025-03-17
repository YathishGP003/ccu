package a75f.io.logic.bo.building.mystat.profiles.util

import a75.io.algos.ControlLoop
import a75f.io.logic.L


/**
 * Created by Manjunath K on 17-01-2025.
 */
/**
 * Created by Manjunath K on 16-08-2021.
 */
open class MyStatLoopController {

    private var coolingControlLoop = ControlLoop()
    private var heatingControlLoop = ControlLoop()

    fun initialise(tuners: MyStatTuners) {
        initialiseControlLoop(coolingControlLoop, tuners)
        initialiseControlLoop(heatingControlLoop, tuners)
    }

    private fun initialiseControlLoop(controlLoop: ControlLoop, tuners: MyStatTuners) {
        controlLoop.setProportionalGain(tuners.proportionalGain)
        controlLoop.setIntegralGain(tuners.integralGain)
        controlLoop.setProportionalSpread(tuners.proportionalSpread)
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
        heatingControlLoop.dumpWithTag(L.TAG_CCU_MSHST)
        coolingControlLoop.dumpWithTag(L.TAG_CCU_MSHST)
    }
}