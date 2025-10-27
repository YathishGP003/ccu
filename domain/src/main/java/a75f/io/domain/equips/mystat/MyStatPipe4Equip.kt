package a75f.io.domain.equips.mystat

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

class MyStatPipe4Equip(equipRef: String) : MyStatEquip(equipRef) {

    val analog1MinFanSpeed = Point(DomainName.analog1MinFanSpeed, equipRef)
    val analog1MaxFanSpeed = Point(DomainName.analog1MaxFanSpeed, equipRef)

    val analog2MinFanSpeed = Point(DomainName.analog2MinFanSpeed, equipRef)
    val analog2MaxFanSpeed = Point(DomainName.analog2MaxFanSpeed, equipRef)

    val analog2MinWaterValve = Point(DomainName.analog2MinWaterValve, equipRef)
    val analog2MaxWaterValve = Point(DomainName.analog2MaxWaterValve, equipRef)

    val analog1MaxHotWaterValve = Point(DomainName.analog1MaxHotWaterValve, equipRef)
    val analog2MaxHotWaterValve = Point(DomainName.analog2MaxHotWaterValve, equipRef)
    val analog1MaxChilledWaterValve = Point(DomainName.analog1MaxChilledWaterValve, equipRef)
    val analog2MaxChilledWaterValve = Point(DomainName.analog2MaxChilledWaterValve, equipRef)

    val analog1MinHotWaterValve = Point(DomainName.analog1MinHotWaterValve, equipRef)
    val analog2MinHotWaterValve = Point(DomainName.analog2MinHotWaterValve, equipRef)
    val analog1MinChilledWaterValve = Point(DomainName.analog1MinChilledWaterValve, equipRef)
    val analog2MinChilledWaterValve = Point(DomainName.analog2MinChilledWaterValve, equipRef)

    val fanSignal = Point(DomainName.fanSignal, equipRef)

    val hotWaterModulatingHeatValve = Point(DomainName.hotWaterModulatingHeatValve, equipRef)
    val chilledWaterModulatingCoolValve = Point(DomainName.chilledWaterModulatingCoolValve, equipRef)

    val hotWaterHeatValve = Point(DomainName.hotWaterHeatValve, equipRef)
    val chilledWaterCoolValve = Point(DomainName.chilledWaterCoolValve, equipRef)

    val auxHeatingStage1 = Point(DomainName.auxHeatingStage1, equipRef)
    val auxHeating1Activate = Point(DomainName.mystatAuxHeating1Activate, equipRef)
    val fanLowSpeedVentilation = Point(DomainName.fanLowSpeedVentilation, equipRef)
}