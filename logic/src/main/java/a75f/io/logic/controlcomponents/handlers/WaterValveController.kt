package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logger.CcuLog
import a75f.io.logic.controlcomponents.controlimpls.GenericBooleanControllerImpl
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.Controller

/**
 * Created by Manjunath K on 03-06-2025.
 */

class WaterValveController(
    private val controllerName: String,
    private val current: CalibratedPoint,
    private val activationHysteresis: Point,
    private val logTag: String
) : Controller {

    private val controller = GenericBooleanControllerImpl()

    init {
        controller.setOnConstraints(listOf(Constraint { current.readHisVal() > activationHysteresis.readPriorityVal() }))
        controller.setOffConstraints(listOf(Constraint { current.readHisVal() == 0.0 }))
    }

    override fun runController(): Any {
        val status = controller.getActiveControl()
        CcuLog.d(
            logTag,
            "Running $controllerName with current: ${current.readHisVal()} Status = $status"
        )
        return status
    }

    fun addOnConstraint(constraint: Constraint) {
        controller.addOnConstraint(constraint)
    }

    fun addOffConstraint(constraint: Constraint) {
        controller.addOffConstraints(constraint)
    }
}