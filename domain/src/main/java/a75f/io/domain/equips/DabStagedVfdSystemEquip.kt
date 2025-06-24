package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

class DabStagedVfdSystemEquip(equipRef : String) : DabStagedSystemEquip (equipRef) {
    val analog2OutputEnable = Point(DomainName.analog2OutputEnable, equipRef)
    val analog2OutputAssociation = Point(DomainName.analog2OutputAssociation, equipRef)
    val analog2CoolStage1 = Point(DomainName.analog2CoolStage1, equipRef)
    val analog2CoolStage2 = Point(DomainName.analog2CoolStage2, equipRef)
    val analog2CoolStage3 = Point(DomainName.analog2CoolStage3, equipRef)
    val analog2CoolStage4 = Point(DomainName.analog2CoolStage4, equipRef)
    val analog2CoolStage5 = Point(DomainName.analog2CoolStage5, equipRef)
    val analog2HeatStage1 = Point(DomainName.analog2HeatStage1, equipRef)
    val analog2HeatStage2 = Point(DomainName.analog2HeatStage2, equipRef)
    val analog2HeatStage3 = Point(DomainName.analog2HeatStage3, equipRef)
    val analog2HeatStage4 = Point(DomainName.analog2HeatStage4, equipRef)
    val analog2HeatStage5 = Point(DomainName.analog2HeatStage5, equipRef)
    val analog2Recirculate = Point(DomainName.analog2Recirculate, equipRef)
    val analog2Economizer = Point(DomainName.analog2Economizer, equipRef)
    val analog2Default = Point(DomainName.analog2Default, equipRef)
    val analog2MinCompressorSpeed = Point(DomainName.analog2MinCompressorSpeed, equipRef)
    val analog2MaxCompressorSpeed = Point(DomainName.analog2MaxCompressorSpeed, equipRef)
    val analog2MinDCVDamper = Point(DomainName.analog2MinDCVDamper, equipRef)
    val analog2MaxDCVDamper = Point(DomainName.analog2MaxDCVDamper, equipRef)

    val fanSignal = Point(DomainName.fanSignal, equipRef)
    val compressorSpeed = Point(DomainName.compressorSpeed, equipRef)
    val damperModulation = Point(DomainName.dcvDamperModulating, equipRef)
}