package a75f.io.domain.equips.unitVentilator

import a75f.io.domain.equips.HyperStatSplitEquip
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

/**
 * Author: Manjunath Kundaragi
 * Created on: 21-07-2025
 */
class HsSplitCpuEquip(equipRef: String): HyperStatSplitEquip(equipRef) {

    val coolingStage1 = Point(DomainName.coolingStage1, equipRef)
    val coolingStage2 = Point(DomainName.coolingStage2, equipRef)
    val coolingStage3 = Point(DomainName.coolingStage3, equipRef)
    val heatingStage1 = Point(DomainName.heatingStage1, equipRef)
    val heatingStage2 = Point(DomainName.heatingStage2, equipRef)
    val heatingStage3 = Point(DomainName.heatingStage3, equipRef)
    val compressorStage1 = Point(DomainName.compressorStage1, equipRef)
    val compressorStage2 = Point(DomainName.compressorStage2, equipRef)
    val compressorStage3 = Point(DomainName.compressorStage3, equipRef)
    val changeOverCooling = Point(DomainName.changeOverCooling, equipRef)
    val changeOverHeating = Point(DomainName.changeOverHeating, equipRef)
    val exhaustFanStage1 = Point(DomainName.exhaustFanStage1, equipRef)
    val exhaustFanStage2 = Point(DomainName.exhaustFanStage2, equipRef)

    val linearFanSpeed = Point(DomainName.linearFanSpeed, equipRef)
    val stagedFanSpeed = Point(DomainName.stagedFanSpeed, equipRef)
    val heatingSignal = Point(DomainName.heatingSignal, equipRef)
    val coolingSignal = Point(DomainName.coolingSignal, equipRef)
    val compressorSpeed = Point(DomainName.compressorSpeed, equipRef)

    val analog1AtMinCooling = Point(DomainName.analog1MinCooling, equipRef)
    val analog1AtMaxCooling = Point(DomainName.analog1MaxCooling, equipRef)
    val analog2AtMinCooling = Point(DomainName.analog2MinCooling, equipRef)
    val analog2AtMaxCooling = Point(DomainName.analog2MaxCooling, equipRef)
    val analog3AtMinCooling = Point(DomainName.analog3MinCooling, equipRef)
    val analog3AtMaxCooling = Point(DomainName.analog3MaxCooling, equipRef)
    val analog4AtMinCooling = Point(DomainName.analog4MinCooling, equipRef)
    val analog4AtMaxCooling = Point(DomainName.analog4MaxCooling, equipRef)

    val analog1AtMinHeating = Point(DomainName.analog1MinHeating, equipRef)
    val analog1AtMaxHeating = Point(DomainName.analog1MaxHeating, equipRef)
    val analog2AtMinHeating = Point(DomainName.analog2MinHeating, equipRef)
    val analog2AtMaxHeating = Point(DomainName.analog2MaxHeating, equipRef)
    val analog3AtMinHeating = Point(DomainName.analog3MinHeating, equipRef)
    val analog3AtMaxHeating = Point(DomainName.analog3MaxHeating, equipRef)
    val analog4AtMinHeating = Point(DomainName.analog4MinHeating, equipRef)
    val analog4AtMaxHeating = Point(DomainName.analog4MaxHeating, equipRef)

    val analog1AtMinLinearFanSpeed = Point(DomainName.analog1MinLinearFanSpeed, equipRef)
    val analog1AtMaxLinearFanSpeed = Point(DomainName.analog1MaxLinearFanSpeed, equipRef)
    val analog2AtMinLinearFanSpeed = Point(DomainName.analog2MinLinearFanSpeed, equipRef)
    val analog2AtMaxLinearFanSpeed = Point(DomainName.analog2MaxLinearFanSpeed, equipRef)
    val analog3AtMinLinearFanSpeed = Point(DomainName.analog3MinLinearFanSpeed, equipRef)
    val analog3AtMaxLinearFanSpeed = Point(DomainName.analog3MaxLinearFanSpeed, equipRef)
    val analog4AtMinLinearFanSpeed = Point(DomainName.analog4MinLinearFanSpeed, equipRef)
    val analog4AtMaxLinearFanSpeed = Point(DomainName.analog4MaxLinearFanSpeed, equipRef)

    val analog1AtMinCompressorSpeed = Point(DomainName.analog1MinCompressorSpeed, equipRef)
    val analog1AtMaxCompressorSpeed = Point(DomainName.analog1MaxCompressorSpeed, equipRef)
    val analog2AtMinCompressorSpeed = Point(DomainName.analog2MinCompressorSpeed, equipRef)
    val analog2AtMaxCompressorSpeed = Point(DomainName.analog2MaxCompressorSpeed, equipRef)
    val analog3AtMinCompressorSpeed = Point(DomainName.analog3MinCompressorSpeed, equipRef)
    val analog3AtMaxCompressorSpeed = Point(DomainName.analog3MaxCompressorSpeed, equipRef)
    val analog4AtMinCompressorSpeed = Point(DomainName.analog4MinCompressorSpeed, equipRef)
    val analog4AtMaxCompressorSpeed = Point(DomainName.analog4MaxCompressorSpeed, equipRef)

    val fanOutCoolingStage1 = Point(DomainName.fanOutCoolingStage1, equipRef)
    val fanOutCoolingStage2 = Point(DomainName.fanOutCoolingStage2, equipRef)
    val fanOutCoolingStage3 = Point(DomainName.fanOutCoolingStage3, equipRef)
    val fanOutHeatingStage1 = Point(DomainName.fanOutHeatingStage1, equipRef)
    val fanOutHeatingStage2 = Point(DomainName.fanOutHeatingStage2, equipRef)
    val fanOutHeatingStage3 = Point(DomainName.fanOutHeatingStage3, equipRef)
    val fanOutCompressorStage1 = Point(DomainName.fanOutCompressorStage1, equipRef)
    val fanOutCompressorStage2 = Point(DomainName.fanOutCompressorStage2, equipRef)
    val fanOutCompressorStage3 = Point(DomainName.fanOutCompressorStage3, equipRef)
}