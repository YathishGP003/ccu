package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

/**
 * Could be a subclass of VavStagedEquip. But the cooling/heating stages are defined as load-based and
 * are having different domainName. Hence a new class is created by extending VavSystemEquip directly.
 */
class VavAdvancedHybridSystemEquip (equipRef : String, connectEquipRef : String) : VavSystemEquip (equipRef) {
    val cmEquip = AdvancedHybridSystemEquip(equipRef)
    val connectEquip1 = ConnectModuleEquip(connectEquipRef)
    val vavProportionalKFactor = Point(DomainName.vavProportionalKFactor, equipRef)
    val vavTemperatureProportionalRange = Point(DomainName.vavTemperatureProportionalRange, equipRef)
    val vavIntegralKFactor = Point(DomainName.vavIntegralKFactor, equipRef)
    val vavTemperatureIntegralTime = Point(DomainName.vavTemperatureIntegralTime, equipRef)
}