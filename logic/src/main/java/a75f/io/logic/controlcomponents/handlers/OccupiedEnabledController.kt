package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.util.CalibratedPoint
import a75f.io.logger.CcuLog
import a75f.io.logic.controlcomponents.controlimpls.GenericBooleanControllerImpl
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.isOccupiedDcvHumidityControl

/**
 * Created by Manjunath K on 05-05-2025.
 */

class OccupiedEnabledController(val occupancy: CalibratedPoint, val logTag: String): Controller {

    private val controller = GenericBooleanControllerImpl()

    init {
        controller.setOnConstraints(listOf(Constraint { isOccupiedDcvHumidityControl(occupancy) }))
        controller.setOffConstraints(listOf(Constraint { isOccupiedDcvHumidityControl(occupancy).not() }))
    }

    override fun runController(): Boolean {
        CcuLog.d(logTag, "Running OccupiedEnabledController" +
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