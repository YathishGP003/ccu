package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

/**
 * Created by Manjunath K on 19-05-2024.
 */
class DabAdvancedHybridSystemEquip (equipRef : String, connectEquipRef : String) : DabSystemEquip (equipRef) {

    val cmEquip = AdvancedHybridSystemEquip(equipRef)
    val connectEquip1 = ConnectModuleEquip(connectEquipRef)
    val dabProportionalKFactor = Point(DomainName.dabProportionalKFactor, equipRef)
    val dabTemperatureProportionalRange = Point(DomainName.dabTemperatureProportionalRange, equipRef)
    val dabIntegralKFactor = Point(DomainName.dabIntegralKFactor, equipRef)
    val dabTemperatureIntegralTime = Point(DomainName.dabTemperatureIntegralTime, equipRef)
}