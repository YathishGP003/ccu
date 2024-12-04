package a75f.io.domain.cutover

/**
 * Created by Manjunath K on 26-11-2024.
 */
object HyperStatDeviceCutOverMapping {
    val entries = linkedMapOf(
            "SENSOR_RH" to "humiditySensor",
            "SENSOR_ILLUMINANCE" to "illuminanceSensor",
            "SENSOR_OCCUPANCY" to "occupancySensor",
            "SENSOR_PM10" to "pm10Sensor",
            "SENSOR_PM2P5" to "pm25Sensor",
            "SENSOR_SOUND" to "soundSensor",
            "SENSOR_PRESSURE" to "pressureSensor",
            "SENSOR_CO2" to "co2Sensor",
            "SENSOR_NO" to "noSensor",
            "SENSOR_CO2_EQUIVALENT" to "co2EquivalentSensor",

            "relay1" to "relay1",
            "relay2" to "relay2",
            "relay3" to "relay3",
            "relay4" to "relay4",
            "relay5" to "relay5",
            "relay6" to "relay6",

            "Analog1In" to "analog1In",
            "Analog2In" to "analog2In",

            "Th1In" to "th1In",
            "Th2In" to "th2In",

            "analog1Out" to "analog1Out",
            "analog2Out" to "analog2Out",
            "analog3Out" to "analog3Out",

            "currentTemp" to "currentTemp",
            "desiredTemp" to "desiredTemp",
            "firmwareVersion" to "firmwareVersion",
            "rssi" to "rssi",
            )
}