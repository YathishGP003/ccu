package a75f.io.domain.equips.hyperstatsplit

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

open class UnitVentilatorEquip(equipRef:String) : HyperStatSplitEquip(equipRef){

    val analog1MaxFaceBypassDamper = Point(DomainName.analog1MaxFaceBypassDamper, equipRef)
    val analog2MaxFaceBypassDamper = Point(DomainName.analog2MaxFaceBypassDamper, equipRef)
    val analog3MaxFaceBypassDamper = Point(DomainName.analog3MaxFaceBypassDamper, equipRef)
    val analog4MaxFaceBypassDamper = Point(DomainName.analog4MaxFaceBypassDamper, equipRef)

    val analog1MinFaceBypassDamper = Point(DomainName.analog1MinFaceBypassDamper, equipRef)
    val analog2MinFaceBypassDamper = Point(DomainName.analog2MinFaceBypassDamper, equipRef)
    val analog3MinFaceBypassDamper = Point(DomainName.analog3MinFaceBypassDamper, equipRef)
    val analog4MinFaceBypassDamper = Point(DomainName.analog4MinFaceBypassDamper, equipRef)

    val controlVia = Point(DomainName.controlVia, equipRef)
    val enableSaTemperingControl = Point(DomainName.enableSaTemperingControl, equipRef)
    val faceBypassDamperCmd = Point(DomainName.faceBypassDamperCmd, equipRef)
    val faceBypassDamperModulatingCmd = Point(DomainName.faceBypassDamperModulatingCmd, equipRef)

    val saTemperingSetpoint = Point(DomainName.saTemperingSetpoint, equipRef)
    val saTemperingIntegralKFactor = Point(DomainName.saTemperingIntegralKFactor, equipRef)
    val saTemperingTemperatureIntegralTime = Point(DomainName.saTemperingTemperatureIntegralTime, equipRef)
    val saTemperingProportionalKFactor = Point(DomainName.saTemperingProportionalKFactor, equipRef)
    val saTemperingTemperatureProportionalRange = Point(DomainName.saTemperingTemperatureProportionalRange, equipRef)

}