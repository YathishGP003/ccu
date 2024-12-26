package a75f.io.domain.equips.hyperstat

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

/**
 * Created by Manjunath K on 26-09-2024.
 */

class HpuV2Equip(equipRef: String) : HyperStatEquip(equipRef) {

    val compressorLoopOutput = Point(DomainName.compressorLoopOutput, equipRef)
    val compressorSpeed = Point(DomainName.compressorSpeed, equipRef)
    val fanSignal = Point(DomainName.fanSignal, equipRef)
    val compressorStage1 = Point(DomainName.compressorStage1, equipRef)
    val compressorStage2 = Point(DomainName.compressorStage2, equipRef)
    val compressorStage3 = Point(DomainName.compressorStage3, equipRef)
    val auxHeatingStage1 = Point(DomainName.auxHeatingStage1, equipRef)
    val auxHeatingStage2 = Point(DomainName.auxHeatingStage2, equipRef)
    val changeOverCooling = Point(DomainName.changeOverCooling, equipRef)
    val changeOverHeating = Point(DomainName.changeOverHeating, equipRef)

    val analog1MinCompressorSpeed = Point(DomainName.analog1MinCompressorSpeed, equipRef)
    val analog2MinCompressorSpeed = Point(DomainName.analog2MinCompressorSpeed, equipRef)
    val analog3MinCompressorSpeed = Point(DomainName.analog3MinCompressorSpeed, equipRef)
    val analog1MaxCompressorSpeed = Point(DomainName.analog1MaxCompressorSpeed, equipRef)
    val analog2MaxCompressorSpeed = Point(DomainName.analog2MaxCompressorSpeed, equipRef)
    val analog3MaxCompressorSpeed = Point(DomainName.analog3MaxCompressorSpeed, equipRef)

    val analog1MinFanSpeed = Point(DomainName.analog1MinFanSpeed, equipRef)
    val analog2MinFanSpeed = Point(DomainName.analog2MinFanSpeed, equipRef)
    val analog3MinFanSpeed = Point(DomainName.analog3MinFanSpeed, equipRef)
    val analog1MaxFanSpeed = Point(DomainName.analog1MaxFanSpeed, equipRef)
    val analog2MaxFanSpeed = Point(DomainName.analog2MaxFanSpeed, equipRef)
    val analog3MaxFanSpeed = Point(DomainName.analog3MaxFanSpeed, equipRef)

}