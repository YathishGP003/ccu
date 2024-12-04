package a75f.io.domain.equips.hyperstat

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

/**
 * Created by Manjunath K on 26-09-2024.
 */

class CpuV2Equip(equipRef: String) : HyperStatEquip(equipRef) {


    val coolingStage1 = Point(DomainName.coolingStage1, equipRef)
    val coolingStage2 = Point(DomainName.coolingStage2, equipRef)
    val coolingStage3 = Point(DomainName.coolingStage3, equipRef)
    val heatingStage1 = Point(DomainName.heatingStage1, equipRef)
    val heatingStage2 = Point(DomainName.heatingStage2, equipRef)
    val heatingStage3 = Point(DomainName.heatingStage3, equipRef)
    val coolingSignal = Point(DomainName.coolingSignal, equipRef)
    val heatingSignal = Point(DomainName.heatingSignal, equipRef)
    val linearFanSpeed = Point(DomainName.linearFanSpeed, equipRef)
    val stagedFanSpeed = Point(DomainName.stagedFanSpeed, equipRef)


    val analog1MinCooling = Point(DomainName.analog1MinCooling, equipRef)
    val analog2MinCooling = Point(DomainName.analog2MinCooling, equipRef)
    val analog3MinCooling = Point(DomainName.analog3MinCooling, equipRef)
    val analog1MinHeating = Point(DomainName.analog1MinHeating, equipRef)
    val analog2MinHeating = Point(DomainName.analog2MinHeating, equipRef)
    val analog3MinHeating = Point(DomainName.analog3MinHeating, equipRef)
    val analog1MinLinearFanSpeed = Point(DomainName.analog1MinLinearFanSpeed, equipRef)
    val analog2MinLinearFanSpeed = Point(DomainName.analog2MinLinearFanSpeed, equipRef)
    val analog3MinLinearFanSpeed = Point(DomainName.analog3MinLinearFanSpeed, equipRef)

    val analog1MaxCooling = Point(DomainName.analog1MaxCooling, equipRef)
    val analog2MaxCooling = Point(DomainName.analog2MaxCooling, equipRef)
    val analog3MaxCooling = Point(DomainName.analog3MaxCooling, equipRef)
    val analog1MaxHeating = Point(DomainName.analog1MaxHeating, equipRef)
    val analog2MaxHeating = Point(DomainName.analog2MaxHeating, equipRef)
    val analog3MaxHeating = Point(DomainName.analog3MaxHeating, equipRef)
    val analog1MaxLinearFanSpeed = Point(DomainName.analog1MaxLinearFanSpeed, equipRef)
    val analog2MaxLinearFanSpeed = Point(DomainName.analog2MaxLinearFanSpeed, equipRef)
    val analog3MaxLinearFanSpeed = Point(DomainName.analog3MaxLinearFanSpeed, equipRef)

    val fanOutCoolingStage1 = Point(DomainName.fanOutCoolingStage1, equipRef)
    val fanOutCoolingStage2 = Point(DomainName.fanOutCoolingStage2, equipRef)
    val fanOutCoolingStage3 = Point(DomainName.fanOutCoolingStage3, equipRef)
    val fanOutHeatingStage1 = Point(DomainName.fanOutHeatingStage1, equipRef)
    val fanOutHeatingStage2 = Point(DomainName.fanOutHeatingStage2, equipRef)
    val fanOutHeatingStage3 = Point(DomainName.fanOutHeatingStage3, equipRef)

    val analog1FanRecirculate = Point(DomainName.analog1FanRecirculate, equipRef)
    val analog2FanRecirculate = Point(DomainName.analog2FanRecirculate, equipRef)
    val analog3FanRecirculate = Point(DomainName.analog3FanRecirculate, equipRef)

}