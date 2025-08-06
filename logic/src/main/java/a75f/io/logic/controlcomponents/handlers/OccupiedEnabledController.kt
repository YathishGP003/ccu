package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.controlcomponents.controlimpls.GenericBooleanControllerImpl
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.isSoftOccupied
import a75f.io.logic.controlcomponents.util.logIt

/**
 * Created by Manjunath K on 05-05-2025.
 */

class OccupiedEnabledController(val occupancy: CalibratedPoint, val logTag: String): Controller {

    private val controller = GenericBooleanControllerImpl()

    init {
        controller.setOnConstraints(listOf(Constraint { isSoftOccupied(occupancy) }))
        controller.setOffConstraints(listOf(Constraint { isSoftOccupied(occupancy).not() }))
    }

    override fun runController(): Boolean {
        logIt(logTag, "Running OccupiedEnabledController" +
                " occupancy ${occupancy.data} Status = ${controller.getActiveControl()}")
        return controller.getActiveControl()
    }

    fun addOnConstraint(constraint: Constraint) {
        controller.addOnConstraint(constraint)
    }

    fun addOffConstraint(constraint: Constraint) {
        controller.addOffConstraints(constraint)
    }
}