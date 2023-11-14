package a75f.io.domain.api

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.BuildingEquip
import a75f.io.domain.logic.DomainManager
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
        assert(Domain.site?.floors?.size  == 1)
        Domain.site?.floors?.entries?.forEach{
            val floor = it.value
            assert(floor.rooms.size == 1)
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

    @JvmStatic
    fun readPoint(domainName: String) : Map<Any,Any> {
        return hayStack.readEntity("point and domainName == \"$domainName\"")
    }

    @JvmStatic
    fun readDict(domainName: String) : HDict {
        return hayStack.readHDict("point and domainName == \"$domainName\"")
    }

    @JvmStatic
    fun readPointForEquip(domainName: String, equipRef : String) : Map<Any,Any> {
        return hayStack.readEntity("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    }
}