package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.controlcomponents.controlimpls.GenericBooleanControllerImpl
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.Controller


class FanEnableController(val fanLoopPoint: Point, val occupancy: CalibratedPoint, private val logTag: String) : Controller {
    private val controller = GenericBooleanControllerImpl()

    init {
        controller.setOnConstraints(
            listOf(Constraint { fanLoopPoint.readHisVal() > 0 },
                Constraint { occupancy.data.toInt() == Occupancy.OCCUPIED.ordinal })
        )
        controller.setOffConstraints(listOf(Constraint { (fanLoopPoint.readHisVal() == 0.0 && occupancy.data.toInt() != Occupancy.OCCUPIED.ordinal) }))
    }

    override fun runController(): Boolean {
        CcuLog.d(logTag, "Running FanEnableController" +
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