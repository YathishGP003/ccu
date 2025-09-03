package a75f.io.domain.equips.unitVentilator

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point


class Pipe4UVEquip(equipRef: String) : UnitVentilatorEquip(equipRef) {
    val analog1MaxHotWaterValve = Point(DomainName.analog1MaxHotWaterValve, equipRef)
    val analog2MaxHotWaterValve = Point(DomainName.analog2MaxHotWaterValve, equipRef)
    val analog3MaxHotWaterValve = Point(DomainName.analog3MaxHotWaterValve, equipRef)
    val analog4MaxHotWaterValve = Point(DomainName.analog4MaxHotWaterValve, equipRef)

    val analog1MinHotWaterValve = Point(DomainName.analog1MinHotWaterValve, equipRef)
    val analog2MinHotWaterValve = Point(DomainName.analog2MinHotWaterValve, equipRef)
    val analog3MinHotWaterValve = Point(DomainName.analog3MinHotWaterValve, equipRef)
    val analog4MinHotWaterValve = Point(DomainName.analog4MinHotWaterValve, equipRef)

    val analog1MinChilledWaterValve = Point(DomainName.analog1MinChilledWaterValve, equipRef)
    val analog2MinChilledWaterValve = Point(DomainName.analog2MinChilledWaterValve, equipRef)
    val analog3MinChilledWaterValve = Point(DomainName.analog3MinChilledWaterValve, equipRef)
    val analog4MinChilledWaterValve = Point(DomainName.analog4MinChilledWaterValve, equipRef)

    val analog1MaxChilledWaterValve = Point(DomainName.analog1MaxChilledWaterValve, equipRef)
    val analog2MaxChilledWaterValve = Point(DomainName.analog2MaxChilledWaterValve, equipRef)
    val analog3MaxChilledWaterValve = Point(DomainName.analog3MaxChilledWaterValve, equipRef)
    val analog4MaxChilledWaterValve = Point(DomainName.analog4MaxChilledWaterValve, equipRef)

    val chilledWaterCoolValve = Point(DomainName.chilledWaterCoolValve, equipRef)
    val hotWaterHeatValve = Point(DomainName.hotWaterHeatValve, equipRef)

    val chilledWaterModulatingCoolValve = Point(DomainName.chilledWaterModulatingCoolValve, equipRef)
    val hotWaterModulatingHeatValve = Point(DomainName.hotWaterModulatingHeatValve, equipRef)


}