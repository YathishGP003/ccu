package a75f.io.logic.controlcomponents.util

import a75f.io.domain.api.Point
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.schedules.Occupancy


/**
 * Created by Manjunath K on 07-05-2025.
 */

fun logIt(tag: String, msg: String) = CcuLog.d(tag, msg)

class ControllerNames {

    // DO NOT CHANGE THESE NAMES, they are used in the controller factory
    companion object {
        const val FAN_ENABLED = "FAN_ENABLED"
        const val OCCUPIED_ENABLED = "OCCUPIED_ENABLED"
        const val HUMIDIFIER_CONTROLLER = "HUMIDIFIER_CONTROLLER"
        const val DEHUMIDIFIER_CONTROLLER = "DEHUMIDIFIER_CONTROLLER"
        const val FAN_SPEED_CONTROLLER = "FAN_SPEED_CONTROLLER"
        const val COOLING_STAGE_CONTROLLER = "COOLING_STAGE_CONTROLLER"
        const val HEATING_STAGE_CONTROLLER = "HEATING_STAGE_CONTROLLER"
        const val EXHAUST_FAN_STAGE1_CONTROLLER = "EXHAUST_FAN_STAGE1_CONTROLLER"
        const val EXHAUST_FAN_STAGE2_CONTROLLER = "EXHAUST_FAN_STAGE2_CONTROLLER"
        const val DAMPER_RELAY_CONTROLLER = "DAMPER_RELAY_CONTROLLER"
        const val COMPRESSOR_RELAY_CONTROLLER = "COMPRESSOR_RELAY_CONTROLLER"
        const val AUX_HEATING_STAGE1 = "AUX_HEATING_STAGE1"
        const val AUX_HEATING_STAGE2 = "AUX_HEATING_STAGE2"
        const val CHANGE_OVER_O_COOLING = "CHANGE_OVER_O_COOLING"
        const val CHANGE_OVER_B_HEATING = "CHANGE_OVER_B_HEATING"
        const val WATER_VALVE_CONTROLLER = "WATER_VALVE_CONTROLLER"
        const val FAN_RUN_COMMAND_CONTROLLER = "FAN_RUN_COMMAND_CONTROLLER"

        const val LOAD_COOLING_STAGE_CONTROLLER = "LOAD_COOLING_STAGE_CONTROLLER"
        const val LOAD_HEATING_STAGE_CONTROLLER = "LOAD_HEATING_STAGE_CONTROLLER"
        const val LOAD_FAN_STAGE_CONTROLLER = "LOAD_FAN_STAGE_CONTROLLER"
        const val SAT_COOLING_STAGE_CONTROLLER = "SAT_COOLING_STAGE_CONTROLLER"
        const val SAT_HEATING_STAGE_CONTROLLER = "SAT_HEATING_STAGE_CONTROLLER"
        const val PRESSURE_FAN_STAGE_CONTROLLER = "PRESSURE_FAN_STAGE_CONTROLLER"
    }
}

fun isSoftOccupied(occupancyPoint : Point) : Boolean {
    val occupancyValue = Occupancy.values()[occupancyPoint.readHisVal().toInt()]
    return occupancyValue == Occupancy.OCCUPIED ||
            occupancyValue == Occupancy.AUTOAWAY ||
            occupancyValue == Occupancy.KEYCARD_AUTOAWAY ||
            occupancyValue == Occupancy.DEMAND_RESPONSE_OCCUPIED||
            occupancyValue == Occupancy.FORCEDOCCUPIED ||
            occupancyValue == Occupancy.AUTOFORCEOCCUPIED
}

fun isSystemOccupied(occupancyPoint: Point): Boolean {
    val occupancy = Occupancy.values()[occupancyPoint.readHisVal().toInt()]
    return occupancy != Occupancy.UNOCCUPIED &&
            occupancy != Occupancy.VACATION &&
            occupancy != Occupancy.AUTOAWAY &&
            occupancy != Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
            occupancy != Occupancy.NONE
}
