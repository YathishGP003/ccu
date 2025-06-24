package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint

/**
 * Created by Manjunath K on 07-06-2024.
 */

open class DabSystemEquip(equipRef: String) : SystemEquip(equipRef) {

    val weightedAverageCoolingLoadMA = Point(DomainName.weightedAverageCoolingLoadMA, equipRef)
    val weightedAverageHeatingLoadMA = Point(DomainName.weightedAverageHeatingLoadMA, equipRef)
    val weightedAverageCoolingLoadPostML = Point(DomainName.weightedAverageCoolingLoadPostML, equipRef)
    val weightedAverageHeatingLoadPostML = Point(DomainName.weightedAverageHeatingLoadPostML, equipRef)

    val dabTargetCumulativeDamper = Point(DomainName.dabTargetCumulativeDamper, equipRef)
    val dabAnalogFanSpeedMultiplier = Point(DomainName.dabAnalogFanSpeedMultiplier, equipRef)
    val dabHumidityHysteresis = Point(DomainName.dabHumidityHysteresis, equipRef)

    val dabRelayDeactivationHysteresis = Point(DomainName.dabRelayDeactivationHysteresis, equipRef)



    val dabStageUpTimerCounter = Point(DomainName.dabStageUpTimerCounter, equipRef)
    val dabStageDownTimerCounter = Point(DomainName.dabStageDownTimerCounter, equipRef)

    val satSPInit = Point(DomainName.satSPInit, equipRef)
    val satSPMin = Point(DomainName.satSPMin, equipRef)
    val satSPMax = Point(DomainName.satSPMax, equipRef)
    val satTimeDelay = Point(DomainName.satTimeDelay, equipRef)
    val satTimeInterval = Point(DomainName.satTimeInterval, equipRef)
    val satIgnoreRequest = Point(DomainName.satIgnoreRequest, equipRef)
    val satSPTrim = Point(DomainName.satSPTrim, equipRef)
    val satSPRes = Point(DomainName.satSPRes, equipRef)
    val satSPResMax = Point(DomainName.satSPResMax, equipRef)

    val staticPressureSPInit = Point(DomainName.staticPressureSPInit, equipRef)
    val staticPressureSPMin = Point(DomainName.staticPressureSPMin, equipRef)
    val staticPressureSPMax = Point(DomainName.staticPressureSPMax, equipRef)
    val staticPressureTimeDelay = Point(DomainName.staticPressureTimeDelay, equipRef)
    val staticPressureTimeInterval = Point(DomainName.staticPressureTimeInterval, equipRef)
    val staticPressureIgnoreRequest = Point(DomainName.staticPressureIgnoreRequest, equipRef)
    val staticPressureSPTrim = Point(DomainName.staticPressureSPTrim, equipRef)
    val staticPressureSPRes = Point(DomainName.staticPressureSPRes, equipRef)
    val staticPressureSPResMax = Point(DomainName.staticPressureSPResMax, equipRef)

    val co2SPInit = Point(DomainName.co2SPInit, equipRef)
    val co2SPMin = Point(DomainName.co2SPMin, equipRef)
    val co2SPMax = Point(DomainName.co2SPMax, equipRef)
    val co2TimeDelay = Point(DomainName.co2TimeDelay, equipRef)
    val co2TimeInterval = Point(DomainName.co2TimeInterval, equipRef)
    val co2IgnoreRequest = Point(DomainName.co2IgnoreRequest, equipRef)
    val co2SPTrim = Point(DomainName.co2SPTrim, equipRef)
    val co2SPRes = Point(DomainName.co2SPRes, equipRef)
    val co2SPResMax = Point(DomainName.co2SPResMax, equipRef)

    val satTRSp = Point(DomainName.satTRSp, equipRef)
    val co2TRSp = Point(DomainName.co2TRSp, equipRef)
    val staticPressureTRSp = Point(DomainName.staticPressureTRSp, equipRef)


    // analog output points
    val analog1Out = Point(DomainName.analog1Out , equipRef)
    val analog2Out = Point(DomainName.analog2Out , equipRef)
    val analog3Out = Point(DomainName.analog3Out , equipRef)
    val analog4Out = Point(DomainName.analog4Out , equipRef)

    // relay points
    val relay3 = Point(DomainName.relay3 , equipRef)
    val relay7 = Point(DomainName.relay7 , equipRef)


    // others
    val systemPurgeVavMinFanLoopOutput = Point(DomainName.systemPurgeVavMinFanLoopOutput , equipRef)
    val currentOccupancy = CalibratedPoint(DomainName.occupancyMode , equipRef, 0.0)

}