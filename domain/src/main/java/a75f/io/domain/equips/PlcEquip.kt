package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

class PlcEquip (equipRef : String) : DomainEquip(equipRef) {

    val heartBeat = Point(DomainName.heartBeat, equipRef)
    val analog1InputType = Point(DomainName.analog1InputType, equipRef)
    val pidTargetValue = Point(DomainName.pidTargetValue, equipRef)
    val thermistor1InputType = Point(DomainName.thermistor1InputType, equipRef)
    val pidProportionalRange = Point(DomainName.pidProportionalRange, equipRef)
    val nativeSensorType = Point(DomainName.nativeSensorType, equipRef)
    val expectZeroErrorAtMidpoint = Point(DomainName.expectZeroErrorAtMidpoint, equipRef)
    val invertControlLoopoutput = Point(DomainName.invertControlLoopoutput, equipRef)
    val useAnalogIn2ForSetpoint = Point(DomainName.useAnalogIn2ForSetpoint, equipRef)
    val analog2InputType = Point(DomainName.analog2InputType, equipRef)
    val setpointSensorOffset = Point(DomainName.setpointSensorOffset, equipRef)
    val analog1MinOutput = Point(DomainName.analog1MinOutput, equipRef)
    val analog1MaxOutput = Point(DomainName.analog1MaxOutput, equipRef)
    val relay1OutputEnable = Point(DomainName.relay1OutputEnable, equipRef)
    val relay2OutputEnable = Point(DomainName.relay2OutputEnable, equipRef)
    val relay1OnThreshold = Point(DomainName.relay1OnThreshold, equipRef)
    val relay2OnThreshold = Point(DomainName.relay2OnThreshold, equipRef)
    val relay1OffThreshold = Point(DomainName.relay1OffThreshold, equipRef)
    val relay2OffThreshold = Point(DomainName.relay2OffThreshold, equipRef)

    val proportionalKFactor = Point(DomainName.proportionalKFactor, equipRef)
    val integralKFactor = Point(DomainName.integralKFactor, equipRef)
    val pidIntegralTime = Point(DomainName.pidIntegralTime, equipRef)
    val relay1Cmd = Point(DomainName.relay1Cmd, equipRef)
    val relay2Cmd = Point(DomainName.relay2Cmd, equipRef)
    val controlVariable = Point(DomainName.controlVariable, equipRef)
    val equipStatusMessage = Point(DomainName.equipStatusMessage, equipRef)
    val dynamicTargetValue = Point(DomainName.dynamicTargetValue, equipRef)
}