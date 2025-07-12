package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint

/**
 * Could be a subclass of VavStagedEquip. But the cooling/heating stages are defined as load-based and
 * are having different domainName. Hence a new class is created by extending VavSystemEquip directly.
 */
class VavAdvancedHybridSystemEquip (equipRef : String, connectEquipRef : String) : VavSystemEquip (equipRef) {
    val cmEquip = AdvancedHybridSystemEquip(equipRef)
    val connectEquip1 = ConnectModuleEquip(connectEquipRef)

    val vavSupplyAirProportionalKFactor = Point(DomainName.vavSupplyAirTemperatureProportionalKFactor, equipRef)
    val vavSupplyAirTemperatureProportionalRange = Point(DomainName.vavSupplyAirTemperatureProportionalRange, equipRef)
    val vavSupplyAirIntegralKFactor = Point(DomainName.vavSupplyAirTemperatureIntegralKFactor, equipRef)
    val vavSupplyAirTemperatureIntegralTime = Point(DomainName.vavSupplyAirTemperatureIntegralTime, equipRef)

    val vavDuctStaticProportionalKFactor = Point(DomainName.vavDuctStaticPressureProportionalKFactor, equipRef)
    val vavDuctStaticPressureProportionalRange = Point(DomainName.vavDuctStaticPressureProportionalRange, equipRef)
    val vavDuctStaticPressureIntegralTime = Point(DomainName.vavDuctStaticPressureIntegralTime, equipRef)
    val vavDuctStaticIntegralKFactor = Point(DomainName.vavDuctStaticPressureIntegralKFactor, equipRef)
}