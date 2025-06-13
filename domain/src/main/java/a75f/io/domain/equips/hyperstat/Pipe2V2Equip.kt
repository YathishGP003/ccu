package a75f.io.domain.equips.hyperstat

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint

/**
 * Created by Manjunath K on 26-09-2024.
 */

class Pipe2V2Equip(equipRef : String) : HyperStatEquip(equipRef){

    val auxHeatingStage1 = Point(DomainName.auxHeatingStage1, equipRef)
    val auxHeatingStage2 = Point(DomainName.auxHeatingStage2, equipRef)
    val waterValve = Point(DomainName.waterValve, equipRef)

    val analog1MinFanSpeed = Point(DomainName.analog1MinFanSpeed, equipRef)
    val analog2MinFanSpeed = Point(DomainName.analog2MinFanSpeed, equipRef)
    val analog3MinFanSpeed = Point(DomainName.analog3MinFanSpeed, equipRef)

    val analog1MaxFanSpeed = Point(DomainName.analog1MaxFanSpeed, equipRef)
    val analog2MaxFanSpeed = Point(DomainName.analog2MaxFanSpeed, equipRef)
    val analog3MaxFanSpeed = Point(DomainName.analog3MaxFanSpeed, equipRef)

    val analog1MinWaterValve = Point(DomainName.analog1MinWaterValve, equipRef)
    val analog2MinWaterValve = Point(DomainName.analog2MinWaterValve, equipRef)
    val analog3MinWaterValve = Point(DomainName.analog3MinWaterValve, equipRef)

    val analog1MaxWaterValve = Point(DomainName.analog1MaxWaterValve, equipRef)
    val analog2MaxWaterValve = Point(DomainName.analog2MaxWaterValve, equipRef)
    val analog3MaxWaterValve = Point(DomainName.analog3MaxWaterValve, equipRef)

    val fanSignal = Point(DomainName.fanSignal, equipRef)
    val modulatingWaterValve = Point(DomainName.modulatingWaterValve, equipRef)
    val leavingWaterTemperature = Point(DomainName.leavingWaterTemperature, equipRef)

    var lastWaterValveTurnedOnTime: Long = System.currentTimeMillis()
    var waterSamplingStartTime: Long = 0
    var waterValveLoop = CalibratedPoint(DomainName.waterValve, equipRef,0.0)

}