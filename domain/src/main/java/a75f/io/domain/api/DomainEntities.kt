package a75f.io.domain.api

import a75f.io.api.haystack.CCUHsApi
import java.lang.IllegalStateException
import kotlin.reflect.KClass

/**
 * An entity definition that binds its domainName to database UUID.
 * domainName - domainName of the entity defined in model
 * id - uuid of the entity instance in haystack
 */
open class Entity (var domainName : String)
open class EntityConfig(val domainName: String)

class Site(domainName : String, val id : String) : Entity(domainName) {

    val floors = mutableMapOf<String, Floor>()
    val ccus = mutableMapOf<String, Device>()
    fun addFloor(entityMap : HashMap<Any, Any>) {
        val domainName = entityMap["domainName"].toString()
        val id = entityMap["id"].toString()
        floors[id] = Floor(domainName, id)
    }
    fun addCcu(entityMap : HashMap<Any, Any>) {
        val domainName = entityMap["domainName"].toString()
        val id = entityMap["id"].toString()
        ccus[id] = Device(domainName, id)
    }
}
class Floor(domainName : String, val id : String) : Entity(domainName) {
    val rooms = mutableMapOf<String, Room>()
    fun addRoom(entityMap : HashMap<Any, Any>) {
        val domainName = entityMap["domainName"].toString()
        val id = entityMap["id"].toString()
        rooms[id] = Room(domainName, id)
    }
}
class Room(domainName : String, val id : String) : Entity(domainName) {
    val equips = mutableMapOf<String, Equip>()
    val devices = mutableMapOf<String, Device>()

    fun addEquip(entityMap : HashMap<Any, Any>) {
        val domainName = entityMap["domainName"].toString()
        val id = entityMap["id"].toString()
        equips[id] = Equip(domainName, id)
    }

    fun addDevice(entityMap : HashMap<Any, Any>) {
        val domainName = entityMap["domainName"].toString()
        val id = entityMap["id"].toString()
        devices[id] = Device(domainName, id)
    }
}
class Equip(domainName : String, val id : String) : Entity(domainName) {
    val points = mutableMapOf<String, Point>()
    fun addPoint(entityMap : HashMap<Any, Any>) {
        val domainName = entityMap["domainName"].toString()
        val id = entityMap["id"].toString()
        points[domainName] = Point(domainName, id)
    }

    fun getPoint(domainName: String) : Point? {
        return points[domainName]
    }
}
class Device(domainName : String, val id : String) : Entity(domainName) {
    val points = mutableMapOf<String, Point>()
    fun addPoint(entityMap : HashMap<Any, Any>) {
        val domainName = entityMap["domainName"].toString()
        val id = entityMap["id"].toString()
        points[domainName] = Point(domainName, id)
    }
    fun getPoint(domainName: String) : Point? {
        return points[domainName]
    }
}
class Point(domainName : String, var id : String) : Entity(domainName) {

    var equipRef = ""
    constructor(domainName: String, equipRef : String, id: String = "") : this(domainName, id) {
        this.equipRef = equipRef
    }

    private fun requireId() {
        if (id.isEmpty()) {
            id = domainName.readPoint(equipRef)["id"].toString()
        }
        if (id.isEmpty()) {
            throw IllegalStateException("Invalid point domain name")
        }
    }
    fun readHisVal() : Double {
        requireId()
        return CCUHsApi.getInstance().readHisValById(id)
    }
    fun writeHisVal(hisVal : Double) {
        requireId()
        CCUHsApi.getInstance().writeHisValById(id, hisVal)
    }
    fun readPriorityVal() : Double {
        requireId()
        return CCUHsApi.getInstance().readPointPriorityVal(id)
    }
    fun writeDefaultVal(defaultVal : Any) {
        requireId()
        if (defaultVal is String) {
            CCUHsApi.getInstance().writeDefaultValById(id, defaultVal)
        } else if (defaultVal is Double) {
            CCUHsApi.getInstance().writeDefaultValById(id, defaultVal)
        }
    }
    fun readDefaultVal() : Double {
        requireId()
        return CCUHsApi.getInstance().readDefaultValById(id)
    }
    fun readDefaultStrVal() : String {
        requireId()
        return CCUHsApi.getInstance().readDefaultStrVal(id)
    }
    fun writeVal(level: Int, who: String?, writableVal: Double?, duration: Int) {
        requireId()
        CCUHsApi.getInstance().writePoint(id, level, who, writableVal, duration )
    }

}
private fun  <T : Entity> getEntity(entityMap : HashMap<Any, Any>, clazz: KClass<T>) : Entity?{
    val domainName = entityMap["domainName"].toString()
    val id = entityMap["id"].toString()
    return when(clazz) {
        Floor::class -> Floor(domainName, id)
        Room::class -> Room(domainName, id)
        Equip::class -> Equip(domainName, id)
        Device::class -> Device(domainName, id)
        Point::class -> Point(domainName, id)
        else -> null
    }
}

fun Boolean.toInt() = if (this) 1 else 0

fun String.readPoint(equipRef: String) : Map <Any, Any>{
    return CCUHsApi.getInstance().readEntity("point and $this and equipRef == \"$equipRef\"")
}