package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.controlcomponents.controlimpls.GenericBooleanControllerImpl
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.isSoftOccupied
import a75f.io.logic.controlcomponents.util.logIt


class FanEnableController(val fanLoopPoint: Point, val occupancy: CalibratedPoint, private val logTag: String) : Controller {
    private val controller = GenericBooleanControllerImpl()

    init {
        controller.setOnConstraints(
            listOf(Constraint { fanLoopPoint.readHisVal() > 0 },
                Constraint { isSoftOccupied(occupancy) })
        )
        controller.setOffConstraints(listOf(Constraint { (fanLoopPoint.readHisVal() == 0.0 && isSoftOccupied(occupancy).not()) }))
    }

    override fun runController(): Boolean {
        logIt(logTag, "Running FanEnableController" +
                " ${fanLoopPoint.domainName} ${fanLoopPoint.readHisVal()} " +
                " ${occupancy.data} Status = ${controller.getActiveControl()}")
        return controller.getActiveControl()
    }

    fun addOnConstraint(constraint: Constraint) {
        controller.addOnConstraint(constraint)
    }

    fun addOffConstraint(constraint: Constraint) {
        controller.addOffConstraints(constraint)
    }
}