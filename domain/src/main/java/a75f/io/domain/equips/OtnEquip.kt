package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

class OtnEquip(equipRef : String) : DomainEquip(equipRef){

    val zonePriority = Point(DomainName.zonePriority, equipRef)
    val autoForceOccupied = Point(DomainName.autoForceOccupied, equipRef)
    val autoAway = Point(DomainName.autoAway, equipRef)
    val temperatureOffset = Point(DomainName.temperatureOffset, equipRef)

    val currentTemp = Point(DomainName.currentTemp, equipRef)
    val desiredTempCooling = Point(DomainName.desiredTempCooling, equipRef)
    val desiredTempHeating = Point(DomainName.desiredTempHeating, equipRef)
    val desiredTemp = Point(DomainName.desiredTemp, equipRef)

    val equipStatus = Point(DomainName.equipStatus, equipRef)
    val equipStatusMessage = Point(DomainName.equipStatusMessage, equipRef)
    val equipScheduleStatus = Point(DomainName.equipScheduleStatus, equipRef)
    val occupancyMode = Point(DomainName.occupancyMode, equipRef)
    val occupancyDetection = Point(DomainName.occupancyDetection, equipRef)

    val dischargeAirTempSetpoint = Point(DomainName.dischargeAirTempSetpoint, equipRef)
    val otaStatus = Point(DomainName.otaStatus, equipRef)
    val scheduleType = Point(DomainName.scheduleType, equipRef)
}