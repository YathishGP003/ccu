package a75f.io.domain

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

class VavEquip (equipRef : String) : DomainEquip(equipRef){

    val heartBeat = Point(DomainName.heartBeat, equipRef)

    val currentTemp = Point(DomainName.currentTemp, equipRef)
    val zoneHumidity = Point(DomainName.zoneHumidity, equipRef)

    val desiredTemp = Point(DomainName.desiredTemp, equipRef)
    val desiredTempCooling = Point(DomainName.desiredTempCooling, equipRef)
    val desiredTempHeating = Point(DomainName.desiredTempHeating, equipRef)

    val coolingLoopOutput = Point(DomainName.coolingLoopOutput, equipRef)
    val heatingLoopOutput = Point(DomainName.heatingLoopOutput, equipRef)

    val temperatureOffset = Point(DomainName.temperatureOffset, equipRef)
    val scheduleType = Point(DomainName.scheduleType, equipRef)
    val autoForceOccupied = Point(DomainName.autoForceOccupied, equipRef)
    val autoAway = Point(DomainName.autoAway, equipRef)
    val enableCo2Control = Point(DomainName.enableCo2Control, equipRef)
    val enableCFMControl = Point(DomainName.enableCFMControl, equipRef)
    val enableIAQControl = Point(DomainName.enableIAQControl, equipRef)
    val damperShape = Point(DomainName.damperShape, equipRef)
    val damperType = Point(DomainName.damperType, equipRef)
    val damperSize = Point(DomainName.damperSize, equipRef)
    val reheatType = Point(DomainName.reheatType, equipRef)
    val zonePriority = Point(DomainName.zonePriority, equipRef)

    val equipStatus = Point(DomainName.equipStatus, equipRef)
    val equipStatusMessage = Point(DomainName.equipStatusMessage, equipRef)
    val equipScheduleStatus = Point(DomainName.equipScheduleStatus, equipRef)

    val dischargeAirTemp = Point(DomainName.dischargeAirTemp, equipRef)
    val zoneCO2 = Point(DomainName.zoneCO2, equipRef)
    val enteringAirTemp = Point(DomainName.enteringAirTemp, equipRef)
    val pressureSensor = Point(DomainName.pressureSensor, equipRef)
    val zoneVoc = Point(DomainName.zoneVoc, equipRef)

    val minCFMCooling = Point(DomainName.minCFMCooling, equipRef)
    val minCFMReheating = Point(DomainName.minCFMReheating, equipRef)
    val maxCFMCooling = Point(DomainName.maxCFMCooling, equipRef)
    val maxCFMReheating = Point(DomainName.maxCFMReheating, equipRef)
    val maxHeatingDamperPos = Point(DomainName.maxHeatingDamperPos, equipRef)
    val occupancyMode = Point(DomainName.occupancyMode, equipRef)
    val occupancyDetection = Point(DomainName.occupancyDetection, equipRef)

    val dischargeAirTempSetpoint = Point(DomainName.dischargeAirTempSetpoint, equipRef)
    val otaStatus = Point(DomainName.otaStatus, equipRef)
    val kFactor = Point(DomainName.kFactor, equipRef)
    val reheatCmd = Point(DomainName.reheatCmd, equipRef)

    val damperCmd = Point(DomainName.damperCmd, equipRef)
    val damperFeedback = Point(DomainName.damperFeedback, equipRef)

    val normalizedDamperCmd = Point(DomainName.normalizedDamperCmd, equipRef)
    val airVelocity = Point(DomainName.airVelocity, equipRef)
    val airFlowSensor = Point(DomainName.airFlowSensor, equipRef)


    val zoneDynamicPriority = Point(DomainName.zoneDynamicPriority, equipRef)
    val co2RequestPercentage = Point(DomainName.co2RequestPercentage, equipRef)
    val satRequestPercentage = Point(DomainName.satRequestPercentage, equipRef)
    val satCurrentRequest = Point(DomainName.satCurrentRequest, equipRef)
    val co2CurrentRequest = Point(DomainName.co2CurrentRequest, equipRef)
    val pressureRequestPercentage = Point(DomainName.pressureRequestPercentage, equipRef)
    val pressureCurrentRequest = Point(DomainName.pressureCurrentRequest, equipRef)
    val vavZonePriorityMultiplier = Point(DomainName.vavZonePriorityMultiplier, equipRef)
    val zoneDeadTime = Point(DomainName.zoneDeadTime, equipRef)
    val abnormalCurTempRiseTrigger = Point(DomainName.abnormalCurTempRiseTrigger, equipRef)
    val autoAwaySetback = Point(DomainName.autoAwaySetback, equipRef)
    val autoAwayTime = Point(DomainName.autoAwayTime, equipRef)
    val vavCoolingDeadbandMultiplier = Point(DomainName.vavCoolingDeadbandMultiplier, equipRef)
    val forcedOccupiedTime = Point(DomainName.forcedOccupiedTime, equipRef)
    val constantTempAlertTime = Point(DomainName.constantTempAlertTime, equipRef)
    val heatingAirflowTemp = Point(DomainName.heatingAirflowTemp, equipRef)
    val coolingAirflowTemp = Point(DomainName.coolingAirflowTemp, equipRef)
    val vavZonePrioritySpread = Point(DomainName.vavZonePrioritySpread, equipRef)
    val valveActuationStartDamperPosDuringSysHeating = Point(DomainName.valveActuationStartDamperPosDuringSysHeating, equipRef)
    val vavAirflowCFMProportionalRange = Point(DomainName.vavAirflowCFMProportionalRange, equipRef)
    val reheatZoneMaxDischargeTemp = Point(DomainName.reheatZoneMaxDischargeTemp, equipRef)
    val vavHeatingDeadbandMultiplier = Point(DomainName.vavHeatingDeadbandMultiplier, equipRef)
    val vavIntegralKfactor = Point(DomainName.vavIntegralKfactor, equipRef)
    val vavProportionalKFactor = Point(DomainName.vavProportionalKFactor, equipRef)
    val vavZoneCo2Target = Point(DomainName.vavZoneCo2Target, equipRef)
    val vavTemperatureIntegralTime = Point(DomainName.vavTemperatureIntegralTime, equipRef)
    val vavZoneVocTarget = Point(DomainName.vavZoneVocTarget, equipRef)
    val vavZoneVocThreshold = Point(DomainName.vavZoneVocThreshold, equipRef)
    val reheatZoneToDATMinDifferential = Point(DomainName.reheatZoneToDATMinDifferential, equipRef)
    val vavZoneCo2Threshold = Point(DomainName.vavZoneCo2Threshold, equipRef)
    val vavTemperatureProportionalRange = Point(DomainName.vavTemperatureProportionalRange, equipRef)
    val reheatZoneDischargeTempOffset = Point(DomainName.reheatZoneDischargeTempOffset, equipRef)
}