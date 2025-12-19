package a75f.io.device.mesh.hyperstat

/**
 * Author: Manjunath Kundaragi
 * Created on: 18-12-2025
 */

enum class Th1InputAssociation {
    DISCHARGE_AIR_TEMPERATURE,
    GENERIC_ALARM_NC,
    GENERIC_ALARM_N0,
    FAN_RUN_STATUS_NO,
    FAN_RUN_STATUS_NC,
    DOOR_WINDOW_SENSOR_NO_TITLE24,
    DOOR_WINDOW_SENSOR_NC_TITLE24,
    DOOR_WINDOW_SENSOR_NO,
    DOOR_WINDOW_SENSOR_NC,
    KEYCARD_SENSOR_NO,
    KEYCARD_SENSOR_NC,
    GENERIC_THERMISTOR_INPUT,
    CHILLED_WATER_SUPPLY_TEMPERATURE,  // applicable only for pipe4 profile
    HOT_WATER_SUPPLY_TEMPERATURE
}

enum class Th2InputAssociation {
    DOOR_WINDOW_SENSOR_NC_TITLE24,
    GENERIC_ALARM_NC,
    GENERIC_ALARM_N0,
    FAN_RUN_STATUS_NO,
    FAN_RUN_STATUS_NC,
    DOOR_WINDOW_SENSOR_NO_TITLE24,
    DISCHARGE_AIR_TEMPERATURE,
    DOOR_WINDOW_SENSOR_NO,
    DOOR_WINDOW_SENSOR_NC,
    KEYCARD_SENSOR_NO,
    KEYCARD_SENSOR_NC,
    GENERIC_THERMISTOR_INPUT,
    CHILLED_WATER_SUPPLY_TEMPERATURE, // applicable only for pipe4 profile
    HOT_WATER_SUPPLY_TEMPERATURE
}

// For backward compatibility with firmware mapping are updated
// DO NOT CHANGE THE VALUES
class FirmwareThermistorMapping {
    companion object {
        const val TH1_DISCHARGE_AIR_TEMPERATURE = 1
        const val TH2_DOOR_WINDOW_SENSOR_NC_TITLE24 = 1
        const val GENERIC_ALARM_NC = 2
        const val GENERIC_ALARM_N0 = 3
        const val FAN_RUN_STATUS_NO = 4
        const val FAN_RUN_STATUS_NC = 5
        const val DOOR_WINDOW_SENSOR_NO_TITLE24 = 6
        const val TH1_DOOR_WINDOW_SENSOR_NC_TITLE24 = 7
        const val TH2_DISCHARGE_AIR_TEMPERATURE = 7
        const val DOOR_WINDOW_SENSOR_NO = 8
        const val DOOR_WINDOW_SENSOR_NC = 9
        const val KEYCARD_SENSOR_NO = 10
        const val KEYCARD_SENSOR_NC = 11
        const val GENERIC_THERMISTOR_INPUT = 12
        const val CHILLED_WATER_SUPPLY_TEMPERATURE = 14
        const val HOT_WATER_SUPPLY_TEMPERATURE = 15

    }
}