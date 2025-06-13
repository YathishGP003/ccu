package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point
import a75f.io.logger.CcuLog
import a75f.io.logic.controlcomponents.controlimpls.GenericBooleanControllerImpl
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.Controller

class HumidifierController(
    private val humidityPoint: Point, val targetMinHumidity: Point, hysteresis: Point, private val logTag: String
) : Controller {
    private val controller = GenericBooleanControllerImpl()

    init {
        controller.setOnConstraints(listOf(Constraint {
            humidityPoint.readHisVal() > 0 && humidityPoint.readHisVal() < targetMinHumidity.readPriorityVal()
        }))
        controller.setOffConstraints(listOf(Constraint {
            humidityPoint.readHisVal() > 0 && humidityPoint.readHisVal() > (targetMinHumidity.readPriorityVal() + hysteresis.readPriorityVal())
        }))
    }

    override fun runController(): Boolean {
        CcuLog.d(logTag, "Running HumidifierController" +
                " ${humidityPoint.readPriorityVal()} Status = ${controller.getActiveControl()}")

        return controller.getActiveControl()
    }

    fun addOnConstraint(constraint: Constraint) {
        controller.addOnConstraint(constraint)
    }

    fun addOffConstraint(constraint: Constraint) {
        controller.addOffConstraints(constraint)
    }
}