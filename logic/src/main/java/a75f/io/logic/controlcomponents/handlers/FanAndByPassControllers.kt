package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point
import a75f.io.logic.controlcomponents.controlimpls.GenericBooleanControllerImpl
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.logIt

/**
 * Author: Manjunath Kundaragi
 * Created on: 24-07-2025
 */
class FanAndByPassControllers(
    private val coolingLoop: Point,
    private val heatingLoop: Point,
    private val hysteresis: Point,
    private val logTag: String
) : Controller {
    private val controller = GenericBooleanControllerImpl()

    init {
        controller.setOnConstraints(listOf(Constraint {
            val hysteresisVal = hysteresis.readPriorityVal()
            (coolingLoop.readHisVal() > hysteresisVal || heatingLoop.readHisVal() > hysteresisVal)
        }))
        controller.setOffConstraints(listOf(Constraint {
            (coolingLoop.readHisVal() == 0.0 && heatingLoop.readHisVal() == 0.0)
        }))
    }

    override fun runController(): Boolean {
        logIt(logTag,"Running FanAndByPassControllers hysteresis ${hysteresis.readPriorityVal()}")
        return controller.getActiveControl()
    }

    fun addOnConstraint(constraint: Constraint) {
        controller.addOnConstraint(constraint)
    }

    fun addOffConstraint(constraint: Constraint) {
        controller.addOffConstraints(constraint)
    }
}