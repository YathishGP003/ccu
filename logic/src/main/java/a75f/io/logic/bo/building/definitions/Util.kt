package a75f.io.logic.bo.building.definitions


fun getPortName(port: String): String {
    return when (port) {
        Port.RELAY_ONE.name -> "relay1"
        Port.RELAY_TWO.name -> "relay2"
        Port.RELAY_THREE.name -> "relay3"
        Port.RELAY_FOUR.name -> "relay4"
        Port.RELAY_FIVE.name -> "relay5"
        Port.RELAY_SIX.name -> "relay6"
        Port.RELAY_SEVEN.name -> "relay7"
        Port.RELAY_EIGHT.name -> "relay8"
        Port.ANALOG_IN_ONE.name -> "analog1In"
        Port.ANALOG_IN_TWO.name -> "analog2In"
        Port.ANALOG_OUT_ONE.name -> "analog1Out"
        Port.ANALOG_OUT_TWO.name -> "analog2Out"
        Port.ANALOG_OUT_THREE.name -> "analog3Out"
        Port.ANALOG_OUT_FOUR.name -> "analog4Out"
        Port.TH1_IN.name -> "th1In"
        Port.TH2_IN.name -> "th2In"
        Port.DESIRED_TEMP.name -> "desiredTemp"
        Port.SENSOR_RT.name -> "currentTemp"
        Port.SENSOR_RH.name -> "humiditySensor"
        Port.SENSOR_CO2.name -> "co2Sensor"
        else -> port
    }
}