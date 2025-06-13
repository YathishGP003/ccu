package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.controlcomponents.controlimpls.GenericBooleanControllerImpl
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.Controller

/**
 * Created by Manjunath K on 09-05-2025.
 */

class EnableController(
    private val controllerName: String,
    private val current: Point,
    private val logTag: String
) : Controller {
    private val controller = GenericBooleanControllerImpl()

    init {
        controller.setOnConstraints(listOf(Constraint { current.readHisVal() > 0 }))
        controller.setOffConstraints(listOf(Constraint { current.readHisVal() == 0.0 }))
    }

    override fun runController(): Boolean {
        CcuLog.d(logTag, "Running $controllerName" +
                 " ${current.readHisVal()} Status = ${controller.getActiveControl()}")
        return controller.getActiveControl()
    }

    fun addOnConstraint(constraint: Constraint) {
        controller.addOnConstraint(constraint)
    }

    fun addOffConstraint(constraint: Constraint) {
        controller.addOffConstraints(constraint)
    }
}