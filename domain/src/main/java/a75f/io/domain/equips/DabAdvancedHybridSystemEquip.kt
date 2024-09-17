package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

/**
 * Created by Manjunath K on 19-05-2024.
 */
class DabAdvancedHybridSystemEquip (equipRef : String, connectEquipRef : String) : DabSystemEquip (equipRef) {

    val cmEquip = AdvancedHybridSystemEquip(equipRef)
    val connectEquip1 = ConnectModuleEquip(connectEquipRef)

    val dabSupplyAirProportionalKFactor = Point(DomainName.dabSupplyAirTemperatureProportionalKFactor, equipRef)
    val dabSupplyAirTemperatureProportionalRange = Point(DomainName.dabSupplyAirTemperatureProportionalRange, equipRef)
    val dabSupplyAirIntegralKFactor = Point(DomainName.dabSupplyAirTemperatureIntegralKFactor, equipRef)
    val dabSupplyAirTemperatureIntegralTime = Point(DomainName.dabSupplyAirTemperatureIntegralTime, equipRef)

    val dabDuctStaticProportionalKFactor = Point(DomainName.dabDuctStaticPressureProportionalKFactor, equipRef)
    val dabDuctStaticPressureProportionalRange = Point(DomainName.dabDuctStaticPressureProportionalRange, equipRef)
    val dabDuctStaticPressureIntegralTime = Point(DomainName.dabDuctStaticPressureIntegralTime, equipRef)
    val dabDuctStaticIntegralKFactor = Point(DomainName.dabDuctStaticIntegralKFactor, equipRef)
}