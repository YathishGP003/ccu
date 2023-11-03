package a75f.io.domain.api

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.logic.DomainManager
import a75f.io.logger.CcuLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.projecthaystack.HDict

object Domain {

    const val LOG_TAG = "CCU_DOMAIN"
    val domainScope = CoroutineScope(Dispatchers.IO + Job())
    var site: Site? = null

    /**
     * Retrieve the domain object of a point by it id and equipRef.
     */
    fun getEquipPoint(pointId : String, parentRef : String) : Point? {
        site?.floors?.entries?.forEach{
            val floor = it.value
            floor.rooms.entries.forEach { room ->
                val equip = room.value.equips.values.find { entry ->
                    entry.id == parentRef
                }
                val point = equip?.points?.values?.find { p ->
                    p.id == pointId
                }
                if (point != null) {
                    return point
                }
            }
        }
        return null
    }

    /**
     * Retrieve the domain object of a point by it id and deviceRef.
     */
    fun getDevicePoint(pointId : String, deviceRef : String) : Point? {
        site?.floors?.entries?.forEach{
            val floor = it.value
            floor.rooms.entries.forEach { room ->
                val device = room.value.devices.values.find { entry ->
                    entry.id == deviceRef
                }
                val point = device?.points?.values?.find { p ->
                    p.id == pointId
                }
                if (point != null) {
                    return point
                }
            }
        }
        return null
    }


    fun getEquipDetailsByDomain(domainName: String): List<Equip> {
        DomainManager.buildDomain(CCUHsApi.getInstance())
        val equips = mutableListOf<Equip>()
        site?.floors?.entries?.forEach{
            val floor = it.value
            floor.rooms.entries.forEach { r ->
                val room =  r.value
                room.equips.forEach { (_, equip) ->
                    if (equip.domainName == domainName){
                        equips.add(equip)
                    }
                }
            }
        }
        return equips
    }

    fun getSystemEquipByDomainName(domainName: String): Equip? {
        DomainManager.buildDomain(CCUHsApi.getInstance())
        site?.ccus?.entries?.forEach {
            it.value.equips.forEach { (_,equip)->
                if (equip.domainName == domainName){
                    return equip
                }
            }
        }
        return null
    }


    fun getPointFromDomain(equip: Equip, domainName: String): Double {
        val pointId = getPointIdFromDomain(equip,domainName)
        if (pointId != null) {
            return hayStack.readDefaultValById(pointId)
        }
        return 0.0
    }

    private fun getPointIdFromDomain(equip: Equip, domainName: String): String? {
        val point = equip.points.entries.find { it.key.contentEquals(domainName) }?.value
        return point?.id
    }

    fun writePointByDomainName(equip: Equip, domainName: String, value: Any){
        val pointId = getPointIdFromDomain(equip,domainName)
        if (pointId != null) {
            if (value is String)
                hayStack.writeDefaultValById(pointId, value)
            if (value is Double) {
                hayStack.writeHisValById(pointId, value)
            }
        }
        CcuLog.i("DEV_DEBUG","$domainName : $pointId")
    }

    @JvmStatic
    fun readPoint(domainName: String) : Map<Any,Any> {
        return CCUHsApi.getInstance().readEntity("point and domainName == \"$domainName\"")
    }

    @JvmStatic
    fun readDict(domainName: String) : HDict {
        return CCUHsApi.getInstance().readHDict("point and domainName == \"$domainName\"")
    }

    @JvmStatic
    fun readPointForEquip(domainName: String, equipRef : String) : Map<Any,Any> {
        return CCUHsApi.getInstance().readEntity("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    }
    fun readPointValueByDomainName(domainName: String, equipRef : String): Double {
        return CCUHsApi.getInstance().readDefaultVal("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    }
}