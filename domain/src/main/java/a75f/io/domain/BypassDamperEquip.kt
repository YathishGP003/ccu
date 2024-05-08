package a75f.io.domain

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.DomainEquip

class BypassDamperEquip (equipRef : String) : DomainEquip(equipRef){

    val ductStaticPressureSensor = Point(DomainName.ductStaticPressureSensor, equipRef)
    val ductStaticPressureSetpoint = Point(DomainName.ductStaticPressureSetpoint, equipRef)
    val pressureSensorType = Point(DomainName.pressureSensorType, equipRef)
    val pressureSensorMinVal = Point(DomainName.pressureSensorMinVal, equipRef)
    val sensorMinVoltageOutput = Point(DomainName.sensorMinVoltage, equipRef)
    val pressureSensorMaxVal = Point(DomainName.pressureSensorMaxVal, equipRef)
    val sensorMaxVoltageOutput = Point(DomainName.sensorMaxVoltage, equipRef)

    val bypassDamperPos = Point(DomainName.bypassDamperCmd, equipRef)
    val damperType = Point(DomainName.damperType, equipRef)
    val supplyAirTemp = Point(DomainName.supplyAirTemp, equipRef)
    val expectedPressureError = Point(DomainName.expectedPressureError, equipRef)
    val satMinThreshold = Point(DomainName.satMinThreshold, equipRef)
    val satMaxThreshold = Point(DomainName.satMaxThreshold, equipRef)
    val damperFeedback = Point(DomainName.damperFeedback, equipRef)
    val damperMinPosition = Point(DomainName.damperMinPosition, equipRef)
    val damperMaxPosition = Point(DomainName.damperMaxPosition, equipRef)
    val bypassDamperLoopOutput = Point(DomainName.bypassDamperLoopOutput, equipRef)
    val bypassCoolingLockout = Point(DomainName.bypassCoolingLockout, equipRef)
    val bypassHeatingLockout = Point(DomainName.bypassHeatingLockout, equipRef)
    val proportionalKFactor = Point(DomainName.proportionalKFactor, equipRef)
    val integralKFactor = Point(DomainName.integralKFactor, equipRef)
    val bypassDamperSATTimeDelay = Point(DomainName.bypassDamperSATTimeDelay, equipRef)
    val bypassDamperIntegralTime = Point(DomainName.bypassDamperIntegralTime, equipRef)
    val heartBeat = Point(DomainName.heartBeat, equipRef)


}