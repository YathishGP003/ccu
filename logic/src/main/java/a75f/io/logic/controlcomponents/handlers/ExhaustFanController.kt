package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point
import a75f.io.logic.controlcomponents.controlimpls.GenericBooleanControllerImpl
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.logIt

/**
 * Created by Manjunath K on 05-05-2025.
 */

open class ExhaustFanController(
    private val currentPoint: Point, val threshold: Point, val hysteresis: Point, private val logTag: String
) : Controller {
    private val controller = GenericBooleanControllerImpl()

    init {
        controller.setOnConstraints(listOf(Constraint {
            val currentValue = currentPoint.readHisVal()
            currentValue > 0 && currentValue > threshold.readPriorityVal()
        }))
        controller.setOffConstraints(listOf(Constraint {
                currentPoint.readHisVal() < (threshold.readPriorityVal() - hysteresis.readPriorityVal())
        }))

    }

    override fun runController(): Boolean {
        logIt(logTag, "Running ThresholdRelayController " +
                "${currentPoint.domainName} ${currentPoint.readHisVal()}"
                + " ${threshold.readPriorityVal()} ${hysteresis.readPriorityVal()} Status = ${controller.getActiveControl()}")
        return controller.getActiveControl()
    }

    fun addOnConstraint(constraint: Constraint) {
        controller.addOnConstraint(constraint)
    }

    fun addOffConstraint(constraint: Constraint) {
        controller.addOffConstraints(constraint)
    }
}