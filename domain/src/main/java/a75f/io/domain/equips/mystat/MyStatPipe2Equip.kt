package a75f.io.domain.equips.mystat

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

/**
 * Created by Manjunath K on 26-09-2024.
 */

class MyStatPipe2Equip(equipRef: String) : MyStatEquip(equipRef) {

    val auxHeatingStage1 = Point(DomainName.auxHeatingStage1, equipRef)
    val auxHeating1Activate = Point(DomainName.mystatAuxHeating1Activate, equipRef)
    val waterValve = Point(DomainName.waterValve, equipRef)
    val fanLowSpeedVentilation = Point(DomainName.fanLowSpeedVentilation, equipRef)

    val analog1MinFanSpeed = Point(DomainName.analog1MinFanSpeed, equipRef)
    val analog1MaxFanSpeed = Point(DomainName.analog1MaxFanSpeed, equipRef)
    val analog1MinWaterValve = Point(DomainName.analog1MinWaterValve, equipRef)
    val analog1MaxWaterValve = Point(DomainName.analog1MaxWaterValve, equipRef)

    val analog2MinFanSpeed = Point(DomainName.analog2MinFanSpeed, equipRef)
    val analog2MaxFanSpeed = Point(DomainName.analog2MaxFanSpeed, equipRef)
    val analog2MinWaterValve = Point(DomainName.analog2MinWaterValve, equipRef)
    val analog2MaxWaterValve = Point(DomainName.analog2MaxWaterValve, equipRef)

    val fanSignal = Point(DomainName.fanSignal, equipRef)
    val modulatingWaterValve = Point(DomainName.modulatingWaterValve, equipRef)
    val leavingWaterTemperature = Point(DomainName.leavingWaterTemperature, equipRef)
}