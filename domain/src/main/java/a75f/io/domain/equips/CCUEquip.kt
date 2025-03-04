package a75f.io.domain.equips

import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import io.seventyfivef.ph.core.Tags

class CCUEquip(equipRef : String) : DomainEquip(equipRef) {
    private val ccuEquipMap = hayStack.readMapById(equipRef)
    val backFillDuration = Point(DomainName.backfillDuration, equipRef)
    val demandResponseActivation = Point(DomainName.demandResponseActivation, equipRef)
    val demandResponseEnrollment = Point(DomainName.demandResponseEnrollment, equipRef)
    val addressBand = Point(DomainName.addressBand, equipRef)
    val offlineMode = Point(DomainName.offlineMode, equipRef)
    val logLevel = Point(DomainName.logLevel, equipRef)
    val disName = ccuEquipMap[Tags.DIS].toString()
}