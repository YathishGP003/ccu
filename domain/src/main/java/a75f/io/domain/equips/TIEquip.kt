package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

class TIEquip(equipRef: String) : DomainEquip(equipRef) {
    val zonePriority = Point(DomainName.zonePriority, equipRef)
    val temperatureOffset = Point(DomainName.temperatureOffset, equipRef)
    val roomTemperature = Point(DomainName.roomTemperature, equipRef)
    val supplyAirTemperature = Point(DomainName.supplyAirTemperature, equipRef)
    val roomTemperatureType = Point(DomainName.roomTemperatureType, equipRef)
    val supplyAirTemperatureType = Point(DomainName.supplyAirTempType, equipRef)

    val currentTemp = Point(DomainName.currentTemp, equipRef)
    val desiredTempCooling = Point(DomainName.desiredTempCooling, equipRef)
    val desiredTempHeating = Point(DomainName.desiredTempHeating, equipRef)
    val desiredTemp = Point(DomainName.desiredTemp, equipRef)

    val equipStatus = Point(DomainName.equipStatus, equipRef)
    val equipStatusMessage = Point(DomainName.equipStatusMessage, equipRef)
    val equipScheduleStatus = Point(DomainName.equipScheduleStatus, equipRef)
    val dischargeAirTempSetpoint = Point(DomainName.dischargeAirTempSetpoint, equipRef)
    val otaStatus = Point(DomainName.otaStatus, equipRef)
    val scheduleType = Point(DomainName.scheduleType, equipRef)
}