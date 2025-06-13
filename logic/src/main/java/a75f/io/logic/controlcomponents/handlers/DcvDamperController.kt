package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.statprofiles.util.isDcvEligibleToOff
import a75f.io.logic.bo.building.statprofiles.util.isDcvEligibleToOn
import a75f.io.logic.controlcomponents.controlimpls.GenericBooleanControllerImpl
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.Controller

/**
 * Created by Manjunath K on 05-05-2025.
 */

class DcvDamperController(
    val dcvLoopOutput: Point,
    val hysteresis: Point,
    private val currentOccupancy: CalibratedPoint,
    private val logTag: String
) : Controller {
    private val controller = GenericBooleanControllerImpl()

    init {
        controller.setOnConstraints(listOf(Constraint {
            dcvLoopOutput.readHisVal() > hysteresis.readPriorityVal() && isDcvEligibleToOn(
                currentOccupancy
            )
        }))
        controller.setOffConstraints(listOf(Constraint {
            (dcvLoopOutput.readHisVal() == 0.0 || isDcvEligibleToOff(currentOccupancy))
        }))
    }

    override fun runController(): Boolean {
        CcuLog.d(logTag, "Running DcvDamperController")
        return controller.getActiveControl()
    }

    fun addOnConstraint(constraint: Constraint) {
        controller.addOnConstraint(constraint)
    }

    fun addOffConstraint(constraint: Constraint) {
        controller.addOffConstraints(constraint)
    }
}