package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.controlcomponents.controlimpls.GenericBooleanControllerImpl
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.isSystemOccupied
import a75f.io.logic.controlcomponents.util.logIt

/**
 * Created by Manjunath K on 17-06-2025.
 */

class FanRunCommandController(
    private val isSystemOccupied: CalibratedPoint,
    private val systemCo2Loop: Point,
    private val logTag: String
) : Controller {

    private val controller = GenericBooleanControllerImpl()


    init {
        controller.setOnConstraints(listOf(Constraint {
            isSystemOccupied(isSystemOccupied) && systemCo2Loop.readHisVal() > 0
        }))

        controller.setOffConstraints(listOf(Constraint {
            !isSystemOccupied(isSystemOccupied) || systemCo2Loop.readHisVal() == 0.0
        }))
    }

    override fun runController(): Boolean {
        logIt(
            logTag, "Running FanRunCommandController" + " Status = ${controller.getActiveControl()}"
        )
        return controller.getActiveControl()
    }

    fun addOnConstraint(constraint: Constraint) {
        controller.addOnConstraint(constraint)
    }

    fun addOffConstraint(constraint: Constraint) {
        controller.addOffConstraints(constraint)
    }
}