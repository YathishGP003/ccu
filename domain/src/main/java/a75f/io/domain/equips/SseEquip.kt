package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

open class SseEquip (equipRef : String) : DomainEquip(equipRef){
    val heartBeat = Point(DomainName.heartBeat, equipRef)
    val currentTemp = Point(DomainName.currentTemp, equipRef)
    val zoneHumidity = Point(DomainName.zoneHumidity, equipRef)
    val desiredTemp = Point(DomainName.desiredTemp, equipRef)
    val desiredTempCooling = Point(DomainName.desiredTempCooling, equipRef)
    val desiredTempHeating = Point(DomainName.desiredTempHeating, equipRef)
    val temperatureOffset = Point(DomainName.temperatureOffset, equipRef)
    val relay1OutputState = Point(DomainName.relay1OutputEnable, equipRef)
    val relay2OutputState = Point(DomainName.relay2OutputEnable, equipRef)
    val thermistor1InputEnable = Point(DomainName.thermistor1InputEnable, equipRef)
    val thermistor2InputEnable = Point(DomainName.thermistor2InputEnable, equipRef)
    val analog1InputEnable = Point(DomainName.analog1InputEnable, equipRef)
    val relay1OutputAssociation = Point(DomainName.relay1OutputAssociation, equipRef)
    val relay2OutputAssociation = Point(DomainName.relay2OutputAssociation, equipRef)
    val analog1InputAssociation = Point(DomainName.analog1InputAssociation, equipRef)
    val coolingStage1 = Point(DomainName.coolingStage1, equipRef)
    val heatingStage1 = Point(DomainName.heatingStage1, equipRef)
    val fanStage1 = Point(DomainName.fanStage1, equipRef)
    val occupiedEnable = Point(DomainName.occupiedEnable, equipRef)
    val equipStatus = Point(DomainName.equipStatus, equipRef)
    val equipStatusMessage = Point(DomainName.equipStatusMessage, equipRef)
    val occupancyMode = Point(DomainName.occupancyMode, equipRef)
    val autoForceOccupied = Point(DomainName.autoForceOccupied, equipRef)
    val autoAway = Point(DomainName.autoAway, equipRef)
    val standaloneStage1Hysteresis = Point(DomainName.standaloneStage1Hysteresis, equipRef)
    val dischargeAirTemperature = Point(DomainName.dischargeAirTemperature, equipRef)
    val demandResponseSetback = Point(DomainName.demandResponseSetback, equipRef)
    val equipScheduleStatus = Point(DomainName.equipScheduleStatus, equipRef)
    val scheduleType = Point(DomainName.scheduleType, equipRef)
}