package a75f.io.device.cm

import a75f.io.domain.api.DomainName

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

fun getTemperatureDomainName(association: Int): String? {
    return when(TemperatureSensorBusMapping.values()[association]) {
        TemperatureSensorBusMapping.notConnected -> null
        TemperatureSensorBusMapping.returnAirTempature -> DomainName.returnAirTemperature
        TemperatureSensorBusMapping.mixedAirTemperature -> DomainName.mixedAirTemperature
        TemperatureSensorBusMapping.supplyAirTemperature1 -> DomainName.supplyAirTemperature1
        TemperatureSensorBusMapping.supplyAirTemperature2 -> DomainName.supplyAirTemperature2
        TemperatureSensorBusMapping.supplyAirTemperature3 -> DomainName.supplyAirTemperature3
    }
}

fun getHumidityDomainName(association: Int): String? {
    return when(HumiditySensorBusMapping.values()[association]) {
        HumiditySensorBusMapping.notConnected -> null
        HumiditySensorBusMapping.returnAirHumidity -> DomainName.returnAirHumidity
        HumiditySensorBusMapping.mixedAirHumidity -> DomainName.mixedAirHumidity
        HumiditySensorBusMapping.supplyAirHumidity1 -> DomainName.supplyAirHumidity1
        HumiditySensorBusMapping.supplyAirHumidity2 -> DomainName.supplyAirHumidity2
        HumiditySensorBusMapping.supplyAirHumidity3 -> DomainName.supplyAirHumidity3
    }
}

fun getOccupancyDomainName(association: Int): String? {
    return when(OccupancySensorBusMapping.values()[association]) {
        OccupancySensorBusMapping.notConnected -> null
        OccupancySensorBusMapping.occupancySensor1 -> DomainName.occupancySensor1
        OccupancySensorBusMapping.occupancySensor2 -> DomainName.occupancySensor2
        OccupancySensorBusMapping.occupancySensor3 -> DomainName.occupancySensor3
    }
}

fun getCo2DomainName(association: Int): String? {
    return when(Co2SensorBusMapping.values()[association]) {
        Co2SensorBusMapping.notConnected -> null
        Co2SensorBusMapping.returnAirCo2 -> DomainName.returnAirCo2
        Co2SensorBusMapping.mixedAirCo2 -> DomainName.mixedAirCo2
    }
}
