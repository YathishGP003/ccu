package a75f.io.domain.api

import a75f.io.api.haystack.CCUHsApi
import kotlin.reflect.KClass

/**
 * An entity definition that binds its domainName to database UUID.
 * domainName - domainName of the entity defined in model
 * id - uuid of the entity instance in haystack
 */
open class Entity (var domainName : String, val id : String)
open class EntityConfig(val domainName: String)

class Site(domainName : String, id : String) : Entity(domainName, id) {
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
class Floor(domainName : String, id : String) : Entity(domainName, id) {
    val rooms = mutableMapOf<String, Room>()
    fun addRoom(entityMap : HashMap<Any, Any>) {
        val domainName = entityMap["domainName"].toString()
        val id = entityMap["id"].toString()
        rooms[id] = Room(domainName, id)
    }
}
class Room(domainName : String, id : String) : Entity(domainName, id) {
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
class Equip(domainName : String, id : String) : Entity(domainName, id) {
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
class Device(domainName : String, id : String) : Entity(domainName, id) {
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
class Point(domainName : String, id : String) : Entity(domainName, id) {
    fun readHisVal() : Double {
        return CCUHsApi.getInstance().readHisValById(id)
    }
    fun writeHisVal(hisVal : Double) {
        CCUHsApi.getInstance().writeHisValById(id, hisVal)
    }
    fun readPriorityVal() : Double {
        return CCUHsApi.getInstance().readPointPriorityVal(id)
    }
    fun writeDefaultVal(defaultVal : Any) {
        if (defaultVal is String) {
            CCUHsApi.getInstance().writeDefaultValById(id, defaultVal)
        } else if (defaultVal is Double) {
            CCUHsApi.getInstance().writeDefaultValById(id, defaultVal)
        }
    }
    fun readDefaultVal() : Double {
        return CCUHsApi.getInstance().readDefaultValById(id)
    }
    fun readDefaultStrVal() : String {
        return CCUHsApi.getInstance().readDefaultStrVal(id)
    }
    fun writeVal(id: String?, level: Int, who: String?, writableVal: Double?, duration: Int) {
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