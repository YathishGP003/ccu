package a75f.io.domain.equips

import a75f.io.api.haystack.SettingPoint
import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.api.Domain
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

    private val addressBandMap = Domain.readPoint(DomainName.addressBand)
    private val addressBandPointBuilder = SettingPoint.Builder().setHashMap(addressBandMap as HashMap)

    fun updateAddressBand(addressBandValue : String) {
        val addressBandPoint = addressBandPointBuilder.setVal(addressBandValue).build()
        hayStack.updateSettingPoint(addressBandPoint, addressBandPoint.id)
        hayStack.syncEntityTree()
    }

    fun getAddressBandValue() : String {
        return addressBandPointBuilder.build().`val`
    }
    val disName = ccuEquipMap[Tags.DIS].toString()
}