package a75f.io.domain.cutover

object HyperStatSplitDeviceCutoverMapping {
    val entries = linkedMapOf(
        "humiditySensor" to "humiditySensor",
        "co2Sensor" to "co2Sensor",
        "pm2p5" to "pm25Sensor",
        "pm10Sensor" to "pm10Sensor", // TODO: Check dis
        // TODO: UVI
        "occupancySensor" to "occupancySensor",
        "soundSensor" to "soundSensor",
        "illuminanceSensor" to "illuminanceSensor",
        "pressureSensor" to "ductStaticPressureSensor",

        "supplyAirTempSensor" to "supplyAirTemperature",
        "supplyAirHumiditySensor" to "supplyAirHumiditySensor",
        "mixedAirTempSensor" to "mixedAirTempSensor",
        "mixedAirHumiditySensor" to "mixedAirHumiditySensor",
        "outsideAirTempSensor" to "outsideAirTempSensor",
        "outsideAirHumiditySensor" to "outsideAirHumiditySensor",

        "universal1In" to "universal1In",
        "universal2In" to "universal2In",
        "universal3In" to "universal3In",
        "universal4In" to "universal4In",
        "universal5In" to "universal5In",
        "universal6In" to "universal6In",
        "universal7In" to "universal7In",
        "universal8In" to "universal8In",

        "relay1" to "relay1",
        "relay2" to "relay2",
        "relay3" to "relay3",
        "relay4" to "relay4",
        "relay5" to "relay5",
        "relay6" to "relay6",
        "relay7" to "relay7",
        "relay8" to "relay8",

        "analog1Out" to "analog1Out",
        "analog2Out" to "analog2Out",
        "analog3Out" to "analog3Out",
        "analog4Out" to "analog4Out",

        "currentTemp" to "currentTemp",
        "desiredTemp" to "desiredTemp",

        "firmwareVersion" to "firmwareVersion",
        "rssi" to "rssi",

    )
}