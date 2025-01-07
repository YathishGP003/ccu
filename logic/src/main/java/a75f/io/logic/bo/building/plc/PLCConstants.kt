package a75f.io.logic.bo.building.plc

object PLCConstants {

    // analog1InputType sensors

    const val NOT_USED = 0
    const val VOLTAGE_INPUT_AI1 = 1
    const val PRESSURE_SENSOR_AI1 = 2
    const val DIFFERENTIAL_AIR_PRESSURE_SENSOR_025_AI1 = 3
    const val AIR_FLOW_SENSOR_AI1 = 4
    const val ZONE_HUMIDITY_AI1 = 5
    const val ZONE_CO2_AI1 = 6
    const val ZONE_CO_AI1 = 7
    const val ZONE_NO2_AI1 = 8
    const val CURRENT_TX10_AI1 = 9
    const val CURRENT_TX20_AI1 = 10
    const val CURRENT_TX50_AI1 = 11
    const val GENERIC_SENSOR_POINT_AI1 = 12

    // thermistor1InputType sensors

    const val EXTERNAL_AIR_TEMP_SENSOR = 1
    const val AIR_TEMP_SENSOR_100K_OHMS = 2
    const val GENERIC_ALARM_NC = 3
    const val GENERIC_ALARM_NO = 4


    // nativeSensorType sensors

    const val CURRENT_TEMP = 1
    const val ZONE_HUMIDITY = 2
    const val ZONE_CO2 = 3
    const val ZONE_CO = 4
    const val ZONE_NO = 5
    const val PRESSURE_SENSOR = 6
    const val ZONE_SOUND = 7
    const val ZONE_OCCUPANCY = 8
    const val ZONE_ILLUMINANCE = 9
    const val ZONE_CO2E = 10
    const val ZONE_UVI = 11
    const val ZONE_PM25 = 12
    const val ZONE_PM10 = 13

}