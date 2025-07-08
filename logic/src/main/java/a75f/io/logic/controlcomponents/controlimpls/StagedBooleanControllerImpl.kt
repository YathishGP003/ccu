package a75f.io.logic.controlcomponents.controlimpls

import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.StagedBooleanController
import a75f.io.logic.controlcomponents.util.logIt

class StagedBooleanControllerImpl(
    private val totalStages: CalibratedPoint,
    private val stageUpTimer: Point,
    private val stageDownTimer: Point,
    val logTag: String
) : StagedBooleanController {

    val onConstraints = mutableMapOf<Int, List<Constraint>>()
    val offConstraints = mutableMapOf<Int, List<Constraint>>()
    private val currentStages = mutableMapOf<Int, Boolean>().apply {
        repeat(totalStages.data.toInt()) { this[it] = false }
    }

    private var upTimer = 0
    private var downTimer = 0

    override fun getActiveControls(): List<Pair<Int, Boolean>> {

        if (upTimer > 0) upTimer--
        if (downTimer > 0) downTimer--
        logIt(logTag, "Tuners : stageUpTimer ${stageUpTimer.readPriorityVal()} stageDownTimer ${stageDownTimer.readPriorityVal()} Started Timers => up: $upTimer, down: $downTimer")
        val newState = mutableMapOf<Int, Boolean>()

        logIt(logTag, "-------- On constrains ----------------------")
        onConstraints.toSortedMap().forEach { (stage, constraints) ->
            val shouldTurnOn = constraints.any { it() }
            if (stageUpTimer.readPriorityVal() > 0) {
                if (shouldTurnOn && !isStageUpTimerActive() && !currentStages.getOrDefault(stage, false)) {
                    activateStageUpTimer()
                    currentStages[stage] = true
                }
            } else {
                if (shouldTurnOn && !currentStages.getOrDefault(stage, false)) {
                    currentStages[stage] = true
                }
            }
            newState[stage] = shouldTurnOn
        }
        logIt(logTag, "\n-------- Off constrains -------------------------------------")
        offConstraints.toSortedMap().forEach { (stage, constraints) ->
            if (newState.containsKey(stage) && newState[stage]!!) {
                return@forEach
            }
            val shouldTurnOff = constraints.any { it() }
            if (stageDownTimer.readPriorityVal() > 0) {
                if (shouldTurnOff && !isStageDownTimerActive() && currentStages.getOrDefault(stage, false)) {
                    if (stage != 0 && currentStages.getOrDefault(stage - 1, false) && !isStageUpTimerActive()) {
                        activateStageDownTimer()
                    }
                    currentStages[stage] = false
                }
            } else {
                if (shouldTurnOff && currentStages.getOrDefault(stage, false)) {
                    currentStages[stage] = false
                }
            }
        }
        logIt(logTag, "Result :  ${currentStages.toList()}\n\n--------------------------------------------------------------------------")
        return currentStages.toList()
    }

    private fun activateStageUpTimer() {
        logIt(logTag, "Activated Stage UP Timer ${stageUpTimer.readPriorityVal().toInt()}")
        upTimer = stageUpTimer.readPriorityVal().toInt() + 1
    }

    private fun activateStageDownTimer() {
        logIt(logTag, "Activated Stage DOWN Timer ${stageDownTimer
            .readPriorityVal().toInt()}")
        downTimer = stageDownTimer.readPriorityVal().toInt() + 1
    }

    private fun isStageUpTimerActive() = upTimer > 0
    private fun isStageDownTimerActive() = downTimer > 0

    override fun setOnConstraints(constraints: List<Constraint>) {
        TODO("Not required because it has multiple stages")
    }

    override fun setOffConstraints(constraints: List<Constraint>) {
        TODO("Not required because it has multiple stages")
    }

    fun setOnConstraint(index: Int, constraints: List<Constraint>) {
        onConstraints[index] = constraints
    }

    fun setOffConstraint(index: Int, constraints: List<Constraint>) {
        offConstraints[index] = constraints
    }

    fun addOnConstraint(index: Int, constraint: Constraint) {
        onConstraints[index] = onConstraints.getOrDefault(index, emptyList()) + constraint
    }

    fun addOffConstraint(index: Int, constraint: Constraint) {
        offConstraints[index] = offConstraints.getOrDefault(index, emptyList()) + constraint
    }

    fun getOnConstraints(index: Int): List<Constraint>? {
        return onConstraints[index]
    }

    fun getOffConstraints(index: Int): List<Constraint>? {
        return offConstraints[index]
    }

    fun resetStagesAndTimers() {
        upTimer = 0
        downTimer = 0
        currentStages.forEach { (stage, _) ->
            currentStages[stage] = false
        }
        logIt(logTag, "Timers reset to => up: $upTimer, down: $downTimer")
    }
}
