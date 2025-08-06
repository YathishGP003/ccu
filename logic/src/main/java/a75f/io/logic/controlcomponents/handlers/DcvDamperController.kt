package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.controlcomponents.controlimpls.GenericBooleanControllerImpl
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.isSoftOccupied
import a75f.io.logic.controlcomponents.util.logIt

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
            dcvLoopOutput.readHisVal() > hysteresis.readPriorityVal() && isSoftOccupied(currentOccupancy)
        }))
        controller.setOffConstraints(listOf(Constraint {
            (dcvLoopOutput.readHisVal() == 0.0 || isSoftOccupied(currentOccupancy).not())
        }))
    }

    override fun runController(): Boolean {
        val status = controller.getActiveControl()
        logIt(logTag, "Running DcvDamperController dcvLoopOutput" +
                " ${dcvLoopOutput.readHisVal()} currentOccupancy ${currentOccupancy.readHisVal()} Eligible to ON = ${isSoftOccupied(currentOccupancy)} ")
        return status
    }

    fun addOnConstraint(constraint: Constraint) {
        controller.addOnConstraint(constraint)
    }

    fun addOffConstraint(constraint: Constraint) {
        controller.addOffConstraints(constraint)
    }
}