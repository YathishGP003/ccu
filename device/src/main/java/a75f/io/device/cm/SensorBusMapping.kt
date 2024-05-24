package a75f.io.device.cm

//TODO - Use the domain names as enum values and replace these with capitalized enum constants
enum class TemperatureSensorBusMapping {
    notConnected, returnAirTempature, mixedAirTemperature, supplyAirTemperature1, supplyAirTemperature2, supplyAirTemperature3
}

enum class PressureSensorBusMapping {
    notConnected, ductStaticPressure12, ductStaticPressure22, ductStaticPressure32
}

enum class HumiditySensorBusMapping {
    notConnected, returnAirHumidity, mixedAirHumidity, supplyAirHumidity1, supplyAirHumidity2, supplyAirHumidity3
}

enum class OccupancySensorBusMapping {
    notConnected, occupancySensor1, occupancySensor2, occupancySensor3
}

enum class Co2SensorBusMapping {
    notConnected, returnAirCo2, mixedAirCo2
}