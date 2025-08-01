package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.controlcomponents.controlimpls.StagedBooleanControllerImpl
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.logIt

class StageControlHandler(
    private val controllerName: String,
    private val loopOutput: Point,
    private val hysteresis: Point,
    private val totalStages: CalibratedPoint,
    stageUpTimer: Point = Point("StageUpTimer", ""),
    stageDownTimer: Point = Point("StageDownTimer", ""),
    private val economizingAvailable: CalibratedPoint = CalibratedPoint("economizingAvailable", "", 0.0),
    private val lockOutActive: CalibratedPoint = CalibratedPoint("economizingAvailable", "", 0.0),
    private val logTag: String
) : Controller {
    val controller = StagedBooleanControllerImpl(totalStages, stageUpTimer, stageDownTimer, logTag)

    private fun getThreshold(): Double {
        return if (economizingAvailable.data == 1.0) (100 / (totalStages.data + 1)) else (100 / totalStages.data)
    }

    private fun stageOn(stageNumber: Int): Boolean {
        if (lockOutActive.readHisVal() > 0) {
            logIt(logTag, "Lockout is active, Stage$stageNumber is OFF")
            return false
        }
        logIt(
            logTag, "Stage ON $stageNumber On , economizingAvailable: ${economizingAvailable.readHisVal()} " +
                    "loopOutput: ${loopOutput.readHisVal()} , Threshold : ${getThreshold()},  hysteresis: ${hysteresis.readPriorityVal()}"
        )
        return if (stageNumber == 0) {
            if (economizingAvailable.readHisVal() > 0) {
                loopOutput.readHisVal() > getThreshold() + (hysteresis.readPriorityVal() / 2)
            } else {
                loopOutput.readHisVal() > hysteresis.readPriorityVal()
            }
        } else {
            if (economizingAvailable.readHisVal() > 0) {
                (loopOutput.readHisVal() > (((stageNumber + 1) * getThreshold()) + (hysteresis.readPriorityVal() / 2)))
            } else {
                (loopOutput.readHisVal() > ((stageNumber * getThreshold()) + (hysteresis.readPriorityVal() / 2)))
            }
        }
    }

    private fun stageOff(stageNumber: Int): Boolean {
        if (lockOutActive.readHisVal() > 0) {
            return true
        }
        logIt(
            logTag,
            "Stage OFF $stageNumber , economizingAvailable: ${economizingAvailable.readHisVal()} " +
                    "loopOutput: ${loopOutput.readHisVal()} , Threshold : ${getThreshold()},  hysteresis: ${hysteresis.readPriorityVal()}"
        )
        return if (stageNumber == 0) {
            if (economizingAvailable.readHisVal() > 0) {
                loopOutput.readHisVal() < getThreshold() - (hysteresis.readPriorityVal() / 2)
            } else {
                loopOutput.readHisVal() <= 0
            }
        } else {
            if (economizingAvailable.readHisVal() > 0) {
                loopOutput.readHisVal() <= (((stageNumber + 1) * getThreshold()) - (hysteresis.readPriorityVal() / 2))
            } else  {
                loopOutput.readHisVal() <= ((stageNumber * getThreshold()) - (hysteresis.readPriorityVal() / 2))
            }
        }
    }

    init {
        logIt(logTag, "Initializing Control $controllerName totalStages: ${totalStages.data}")
        for (i in 0 until totalStages.data.toInt()) {
            controller.setOnConstraint(i, listOf(Constraint { stageOn(i) }))
            controller.setOffConstraint(i, listOf(Constraint { stageOff(i) }))
        }
        controller.onConstraints.forEach { (i, constraints) ->
            logIt(logTag, "onConstraints $i : ${constraints.size}")
        }
        controller.offConstraints.forEach { (i, constraints) ->
            logIt(logTag, "offConstraints $i : ${constraints.size}")
        }
    }

    override fun runController(): List<Pair<Int, Boolean>> {
        logIt(logTag, "Running Control $controllerName")
        updateConstrainsIfRequired()
        return controller.getActiveControls()
    }

    private fun updateConstrainsIfRequired() {
        if (controller.currentStages.keys.size != totalStages.data.toInt()) {
            for (i in 0 until totalStages.data.toInt()) {
                if (!controller.currentStages.containsKey(i)) {
                    controller.setOnConstraint(i, listOf(Constraint { stageOn(i) }))
                    controller.setOffConstraint(i, listOf(Constraint { stageOff(i) }))
                }
            }
        }
    }

    fun addOnConstraint(stage: Int, constraint: Constraint) {
        controller.addOnConstraint(stage, constraint)
    }

    fun addOffConstraint(stage: Int, constraint: Constraint) {
        controller.addOffConstraint(stage, constraint)
    }

    override fun resetController() {
        controller.resetStagesAndTimers()
    }
}