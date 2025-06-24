package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

class ConditioningStages (equipRef : String) {
    val coolingStage1 = Point(DomainName.coolingStage1 ,equipRef)
    val coolingStage2 = Point(DomainName.coolingStage2 ,equipRef)
    val coolingStage3 = Point(DomainName.coolingStage3 ,equipRef)
    val coolingStage4 = Point(DomainName.coolingStage4 ,equipRef)
    val coolingStage5 = Point(DomainName.coolingStage5 ,equipRef)

    val heatingStage1 = Point(DomainName.heatingStage1 ,equipRef)
    val heatingStage2 = Point(DomainName.heatingStage2 ,equipRef)
    val heatingStage3 = Point(DomainName.heatingStage3 ,equipRef)
    val heatingStage4 = Point(DomainName.heatingStage4 ,equipRef)
    val heatingStage5 = Point(DomainName.heatingStage5 ,equipRef)

    val fanStage1 = Point(DomainName.fanStage1 ,equipRef)
    val fanStage2 = Point(DomainName.fanStage2 ,equipRef)
    val fanStage3 = Point(DomainName.fanStage3 ,equipRef)
    val fanStage4 = Point(DomainName.fanStage4 ,equipRef)
    val fanStage5 = Point(DomainName.fanStage5 ,equipRef)

    val humidifierEnable = Point(DomainName.humidifierEnable ,equipRef)
    val dehumidifierEnable = Point(DomainName.dehumidifierEnable ,equipRef)
    val compressorStage1 = Point(DomainName.compressorStage1 ,equipRef)
    val compressorStage2 = Point(DomainName.compressorStage2 ,equipRef)
    val compressorStage3 = Point(DomainName.compressorStage3 ,equipRef)
    val compressorStage4 = Point(DomainName.compressorStage4 ,equipRef)
    val compressorStage5 = Point(DomainName.compressorStage5 ,equipRef)
    val changeOverCooling = Point(DomainName.changeOverCooling ,equipRef)
    val changeOverHeating = Point(DomainName.changeOverHeating ,equipRef)
    val fanEnable = Point(DomainName.fanEnable ,equipRef)
    val occupiedEnabled = Point(DomainName.occupiedEnable ,equipRef)
    val dcvDamper = Point(DomainName.dcvDamper ,equipRef)
}