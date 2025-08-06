package a75f.io.domain.equips.unitVentilator

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.HyperStatSplitEquip

open class UnitVentilatorEquip(equipRef:String) : HyperStatSplitEquip(equipRef){

    val analog1MaxFaceBypassDamper = Point(DomainName.analog1MaxFaceBypassDamper, equipRef)
    val analog2MaxFaceBypassDamper = Point(DomainName.analog2MaxFaceBypassDamper, equipRef)
    val analog3MaxFaceBypassDamper = Point(DomainName.analog3MaxFaceBypassDamper, equipRef)
    val analog4MaxFaceBypassDamper = Point(DomainName.analog4MaxFaceBypassDamper, equipRef)

    val analog1MinFaceBypassDamper = Point(DomainName.analog1MinFaceBypassDamper, equipRef)
    val analog2MinFaceBypassDamper = Point(DomainName.analog2MinFaceBypassDamper, equipRef)
    val analog3MinFaceBypassDamper = Point(DomainName.analog3MinFaceBypassDamper, equipRef)
    val analog4MinFaceBypassDamper = Point(DomainName.analog4MinFaceBypassDamper, equipRef)

    val analog1MinFanSpeed = Point(DomainName.analog1MinFanSpeed, equipRef)
    val analog2MinFanSpeed = Point(DomainName.analog2MinFanSpeed, equipRef)
    val analog3MinFanSpeed = Point(DomainName.analog3MinFanSpeed, equipRef)
    val analog4MinFanSpeed = Point(DomainName.analog4MinFanSpeed, equipRef)

    val analog1MaxFanSpeed = Point(DomainName.analog1MaxFanSpeed, equipRef)
    val analog2MaxFanSpeed = Point(DomainName.analog2MaxFanSpeed, equipRef)
    val analog3MaxFanSpeed = Point(DomainName.analog3MaxFanSpeed, equipRef)
    val analog4MaxFanSpeed = Point(DomainName.analog4MaxFanSpeed, equipRef)

    // config

    val controlVia = Point(DomainName.controlVia, equipRef)
    val enableSaTemperingControl = Point(DomainName.enableSaTemperingControl, equipRef)
    val fanSignal = Point(DomainName.fanSignal,equipRef)
    //relay
    val faceBypassDamperCmd = Point(DomainName.faceBypassDamperCmd, equipRef)
    //analog
    val faceBypassDamperModulatingCmd = Point(DomainName.faceBypassDamperModulatingCmd, equipRef)


    val saTemperingSetpoint = Point(DomainName.saTemperingSetpoint, equipRef)
    val saTemperingIntegralKFactor = Point(DomainName.saTemperingIntegralKFactor, equipRef)
    val saTemperingTemperatureIntegralTime = Point(DomainName.saTemperingTemperatureIntegralTime, equipRef)
    val saTemperingProportionalKFactor = Point(DomainName.saTemperingProportionalKFactor, equipRef)
    val saTemperingTemperatureProportionalRange = Point(DomainName.saTemperingTemperatureProportionalRange, equipRef)


}