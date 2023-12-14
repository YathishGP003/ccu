package a75f.io.domain.api

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.BuildingEquip
import a75f.io.domain.logic.DomainManager
import a75f.io.logger.CcuLog
import android.annotation.SuppressLint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.projecthaystack.HDict

@SuppressLint("StaticFieldLeak")
object Domain {

    const val LOG_TAG = "CCU_DOMAIN"
    val domainScope = CoroutineScope(Dispatchers.IO + Job())
    val hayStack: CCUHsApi = CCUHsApi.getInstance()
    var site: Site? = null
    lateinit var buildingEquip : BuildingEquip

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
        assert(site?.floors?.size  == 1)
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
        val point = equip.points.entries.find { it.key.contentEquals(domainName) }?.value
        point?.let { return point.readDefaultVal() }
        return 0.0
    }

    fun getPointHisFromDomain(equip: Equip, domainName: String): Double {
        val point = equip.points.entries.find { it.key.contentEquals(domainName) }?.value
        point?.let { return point.readHisVal() }
        return 0.0
    }

    private fun getPointIdFromDomain(equip: Equip, domainName: String): String? {
        val point = equip.points.entries.find { it.key.contentEquals(domainName) }?.value
        return point?.id
    }

    fun writePointByDomainName(equip: Equip, domainName: String, value: Any) {
        val point = equip.points.entries.find { it.key.contentEquals(domainName) }?.value
        point?.let {
            it.writeDefaultVal(value)
            if (value is Double) {
                it.writeHisVal(value)
            }
        }
    }

    @JvmStatic
    fun readPoint(domainName: String) : Map<Any,Any> {
        return hayStack.readEntity("point and domainName == \"$domainName\"")
    }

    @JvmStatic
    fun readDict(domainName: String) : HDict {
        return hayStack.readHDict("point and domainName == \"$domainName\"")
    }

    @JvmStatic
    fun readDictOnEquip(domainName: String, equipRef: String) : HDict {
        return hayStack.readHDict("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    }

    @JvmStatic
    fun readPointOnEquip(domainName: String, equipRef : String) : Map<Any,Any> {
        return hayStack.readEntity("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    }
    fun readPointValueByDomainName(domainName: String, equipRef : String): Double {
        return hayStack.readDefaultVal("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    }
    @JvmStatic
    fun readDefaultValByDomainName(domainName: String): Double {
        return hayStack.readDefaultVal("point and domainName == \"$domainName\"")
    }
    @JvmStatic
    fun writeDefaultValByDomainName(domainName: String, value: Double) {
        return hayStack.writeDefaultVal("point and domainName == \"$domainName\"", value)
    }
    @JvmStatic
    fun writeHisValByDomainName(domainName: String, value: Double) {
        return hayStack.writeDefaultVal("point and domainName == \"$domainName\"", value)
    }
}