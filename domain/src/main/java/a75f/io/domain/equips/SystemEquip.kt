package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint

/**
 * Base System Domain Equip.
 * Contain points common to all system equips
 */
open class SystemEquip (equipRef : String) : DomainEquip(equipRef) {

    val heatingPreconditioningRate = Point(DomainName.heatingPreconditioningRate, equipRef)
    val coolingPreconditioningRate = Point(DomainName.coolingPreconditioningRate, equipRef)
    val cmTempPercentDeadZonesAllowed = Point(DomainName.cmTempPercentDeadZonesAllowed, equipRef)
    val airflowSampleWaitTime = Point(DomainName.airflowSampleWaitTime, equipRef)
    val stage1CoolingAirflowTempLowerOffset = Point(DomainName.stage1CoolingAirflowTempLowerOffset, equipRef)
    val stage2CoolingAirflowTempLowerOffset = Point(DomainName.stage2CoolingAirflowTempLowerOffset, equipRef)
    val stage3CoolingAirflowTempLowerOffset = Point(DomainName.stage3CoolingAirflowTempLowerOffset, equipRef)
    val stage4CoolingAirflowTempLowerOffset = Point(DomainName.stage4CoolingAirflowTempLowerOffset, equipRef)
    val stage5CoolingAirflowTempLowerOffset = Point(DomainName.stage5CoolingAirflowTempLowerOffset, equipRef)
    val stage1CoolingAirflowTempUpperOffset = Point(DomainName.stage1CoolingAirflowTempUpperOffset, equipRef)
    val stage2CoolingAirflowTempUpperOffset = Point(DomainName.stage2CoolingAirflowTempUpperOffset, equipRef)
    val stage3CoolingAirflowTempUpperOffset = Point(DomainName.stage3CoolingAirflowTempUpperOffset, equipRef)
    val stage4CoolingAirflowTempUpperOffset = Point(DomainName.stage4CoolingAirflowTempUpperOffset, equipRef)
    val stage5CoolingAirflowTempUpperOffset = Point(DomainName.stage5CoolingAirflowTempUpperOffset, equipRef)
    val stage1HeatingAirflowTempLowerOffset = Point(DomainName.stage1HeatingAirflowTempLowerOffset, equipRef)
    val stage2HeatingAirflowTempLowerOffset = Point(DomainName.stage2HeatingAirflowTempLowerOffset, equipRef)
    val stage3HeatingAirflowTempLowerOffset = Point(DomainName.stage3HeatingAirflowTempLowerOffset, equipRef)
    val stage4HeatingAirflowTempLowerOffset = Point(DomainName.stage4HeatingAirflowTempLowerOffset, equipRef)
    val stage5HeatingAirflowTempLowerOffset = Point(DomainName.stage5HeatingAirflowTempLowerOffset, equipRef)
    val stage1HeatingAirflowTempUpperOffset = Point(DomainName.stage1HeatingAirflowTempUpperOffset, equipRef)
    val stage2HeatingAirflowTempUpperOffset = Point(DomainName.stage2HeatingAirflowTempUpperOffset, equipRef)
    val stage3HeatingAirflowTempUpperOffset = Point(DomainName.stage3HeatingAirflowTempUpperOffset, equipRef)
    val stage4HeatingAirflowTempUpperOffset = Point(DomainName.stage4HeatingAirflowTempUpperOffset, equipRef)
    val stage5HeatingAirflowTempUpperOffset = Point(DomainName.stage5HeatingAirflowTempUpperOffset, equipRef)

    val clockUpdateInterval = Point(DomainName.clockUpdateInterval, equipRef)
    val perDegreeHumidityFactor = Point(DomainName.perDegreeHumidityFactor, equipRef)
    val ccuAlarmVolumeLevel = Point(DomainName.ccuAlarmVolumeLevel, equipRef)
    val cmHeartBeatInterval = Point(DomainName.cmHeartBeatInterval, equipRef)
    val heartBeatsToSkip = Point(DomainName.heartBeatsToSkip, equipRef)
    val cmResetCommandTimer = Point(DomainName.cmResetCommandTimer, equipRef)
    val zoneTemperatureDeadLeeway = Point(DomainName.zoneTemperatureDeadLeeway, equipRef)
    val humidityCompensationOffset = Point(DomainName.humidityCompensationOffset, equipRef)

    val cmCoolingDesiredTemp = Point(DomainName.cmCoolingDesiredTemp, equipRef)
    val cmHeatingDesiredTemp = Point(DomainName.cmHeatingDesiredTemp, equipRef)
    val cmCurrentTemp = Point(DomainName.cmCurrentTemp, equipRef)

    val equipScheduleStatus = Point(DomainName.equipScheduleStatus, equipRef) //scheduleStatus
    val equipStatusMessage = Point(DomainName.equipStatusMessage, equipRef)
    val outsideTemperature = Point(DomainName.outsideTemperature, equipRef)
    val outsideHumidity = Point(DomainName.outsideHumidity, equipRef)
    val systemOccupancyMode = Point(DomainName.systemOccupancyMode, equipRef)
    val operatingMode = Point(DomainName.operatingMode, equipRef)
    val systemCI = Point(DomainName.systemCI, equipRef)
    val averageHumidity = Point(DomainName.averageHumidity, equipRef)
    val cmHumidity = Point(DomainName.cmHumidity, equipRef)
    val averageTemperature = Point(DomainName.averageTemperature, equipRef)
    val relayActivationHysteresis = Point(DomainName.relayActivationHysteresis, equipRef)
    val epidemicModeSystemState = Point(DomainName.epidemicModeSystemState, equipRef)
    val systemPrePurgeEnabled = Point(DomainName.systemPrePurgeEnabled, equipRef)//NA
    val systemPostPurgeEnabled = Point(DomainName.systemPostPurgeEnabled, equipRef)
    val systemEnhancedVentilationEnabled = Point(DomainName.systemEnhancedVentilationEnabled, equipRef)

    val useOutsideTempLockoutCooling = Point(DomainName.useOutsideTempLockoutCooling, equipRef)
    val useOutsideTempLockoutHeating = Point(DomainName.useOutsideTempLockoutHeating, equipRef)

    val mechanicalCoolingAvailable = Point(DomainName.mechanicalCoolingAvailable, equipRef) //NPIDM
    val mechanicalHeatingAvailable = Point(DomainName.mechanicalHeatingAvailable, equipRef)
    val vavOutsideTempCoolingLockout = Point(DomainName.vavOutsideTempCoolingLockout, equipRef)
    val vavOutsideTempHeatingLockout = Point(DomainName.vavOutsideTempHeatingLockout, equipRef)

    val desiredCI = Point(DomainName.desiredCI, equipRef)
    val conditioningMode = Point(DomainName.conditioningMode, equipRef)
    val systemtargetMaxInsideHumidity = Point(DomainName.systemtargetMaxInsideHumidity, equipRef)
    val systemtargetMinInsideHumidity = Point(DomainName.systemtargetMinInsideHumidity, equipRef)
    val demandResponseMode = Point(DomainName.demandResponseMode, equipRef)

    val coolingLoopOutput = Point(DomainName.coolingLoopOutput, equipRef)
    val heatingLoopOutput = Point(DomainName.heatingLoopOutput, equipRef)
    val fanLoopOutput = Point(DomainName.fanLoopOutput, equipRef)
    val co2LoopOutput = Point(DomainName.co2LoopOutput, equipRef)
    val compressorLoopOutput = Point(DomainName.compressorLoopOutput, equipRef)
    val dcvLoopOutput = Point(DomainName.dcvLoopOutput, equipRef)
}