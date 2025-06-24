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

class AuxHeatingController(
    private val controllerName: String,
    private val currentTemp: Point,
    private val heatingDesiredTemp: Point,
    private val auxHeatingActivateTuner: Point,
    private val logTag: String
) : Controller {
    private val controller = GenericBooleanControllerImpl()

    init {
        controller.setOnConstraints(listOf(Constraint {
            val temp = currentTemp.readHisVal()
            temp > 0 && temp < (heatingDesiredTemp.readPriorityVal() - auxHeatingActivateTuner.readPriorityVal())
        }))
        controller.setOffConstraints(listOf(Constraint {
            val temp = currentTemp.readHisVal()
            temp > 0 && temp >= (heatingDesiredTemp.readPriorityVal() - (auxHeatingActivateTuner.readPriorityVal() - 1))
        }))
    }

    override fun runController(): Boolean {
        CcuLog.d(
            logTag, "Running $controllerName" +
                    " ${currentTemp.domainName} ${currentTemp.readHisVal()} " +
                    " ${heatingDesiredTemp.readPriorityVal()} ${auxHeatingActivateTuner.readPriorityVal()}" +
                    " Status = ${controller.getActiveControl()}"
        )
        return controller.getActiveControl()
    }

    fun addOnConstraint(constraint: Constraint) {
        controller.addOnConstraint(constraint)
    }

    fun addOffConstraint(constraint: Constraint) {
        controller.addOffConstraints(constraint)
    }
    override fun resetController() {
        CcuLog.d(L.TAG_CCU_SYSTEM, "Resetting $controllerName")
    }
}