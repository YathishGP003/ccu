package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

/**
 * Created by Manjunath K on 19-05-2024.
 */
class DabAdvancedHybridSystemEquip (equipRef : String, connectEquipRef : String) : AdvancedHybridSystemEquip (equipRef, connectEquipRef) {

    val dabProportionalKFactor = Point(DomainName.vavProportionalKFactor, equipRef)
    val dabTemperatureProportionalRange = Point(DomainName.vavTemperatureProportionalRange, equipRef)
    val dabIntegralKFactor = Point(DomainName.vavIntegralKFactor, equipRef)
    val dabTemperatureIntegralTime = Point(DomainName.vavTemperatureIntegralTime, equipRef)
}