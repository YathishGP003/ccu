package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logger.CcuLog
import a75f.io.logic.controlcomponents.controlimpls.GenericBooleanControllerImpl
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.isOccupiedDcvHumidityControl

class DehumidifierController (
    private val humidityPoint: Point, private val targetMaxHumidity: Point, private  val hysteresis: Point, private val logTag: String, val occupancy : CalibratedPoint
) : Controller {
    private val controller = GenericBooleanControllerImpl()

    init {
        controller.setOnConstraints(listOf(Constraint {
            humidityPoint.readHisVal() > 0 && humidityPoint.readHisVal() > targetMaxHumidity.readPriorityVal() && isOccupiedDcvHumidityControl(occupancy)
        }))
        controller.setOffConstraints(listOf(Constraint {
            (humidityPoint.readHisVal() < (targetMaxHumidity.readPriorityVal() - hysteresis.readPriorityVal())
                    || isOccupiedDcvHumidityControl(occupancy).not())
        }))
    }

    override fun runController(): Boolean {
        CcuLog.d(logTag, "Running DeHumidifierController" +
                " ${humidityPoint.readHisVal()} , hysteresis: ${hysteresis.readPriorityVal()} targetMaxHumidity ${targetMaxHumidity.readPriorityVal()} Status = ${controller.getActiveControl()}" +
                " Occupancy = ${occupancy.readHisVal()}")

        return controller.getActiveControl()
    }

    fun addOnConstraint(constraint: Constraint) {
        controller.addOnConstraint(constraint)
    }

    fun addOffConstraint(constraint: Constraint) {
        controller.addOffConstraints(constraint)
    }
}