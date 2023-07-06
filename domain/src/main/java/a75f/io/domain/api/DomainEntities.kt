package a75f.io.domain.api

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * An entity definition that binds its domainName to database UUID.
 * domainName - domainName of the entity defined in model
 * id - uuid of the entity instance in haystack
 */
open class Entity (var domainName : String, val id : String)
open class EntityConfig(val domainName: String)

class Site(domainName : String, id : String) : Entity(domainName, id) {
    val floors = mutableMapOf<String, Floor>()
    val ccus = mutableMapOf<String, CcuDevice>()
    fun addFloor(entityMap : HashMap<Any, Any>) {
        val domainName = entityMap["domainName"].toString()
        val id = entityMap["id"].toString()
        floors[id] = Floor(domainName, id)
    }
    fun addCcu(entityMap : HashMap<Any, Any>) {
        val domainName = entityMap["domainName"].toString()
        val id = entityMap["id"].toString()
        ccus[id] = CcuDevice(domainName, id)
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
    val points = mutableMapOf<String, RawPoint>()
    fun addPoint(entityMap : HashMap<Any, Any>) {
        val domainName = entityMap["domainName"].toString()
        val id = entityMap["id"].toString()
        points[domainName] = RawPoint(domainName, id)
    }
    fun getPoint(domainName: String) : RawPoint? {
        return points[domainName]
    }
}

class CcuDevice(domainName : String, id : String) : Entity(domainName, id) {
    val points = mutableMapOf<String, SettingPoint>()
    fun addPoint(entityMap : HashMap<Any, Any>) {
        val domainName = entityMap["domainName"].toString()
        val id = entityMap["id"].toString()
        points[domainName] = SettingPoint(domainName, id)
    }
    fun getPoint(domainName: String) : SettingPoint? {
        return points[domainName]
    }
}
class Point(domainName : String, id : String) : Entity(domainName, id)
class RawPoint(domainName : String, id : String) : Entity(domainName, id)
class SettingPoint(domainName : String, id : String) : Entity(domainName, id)
private fun  <T : Entity> getEntity(entityMap : HashMap<Any, Any>, clazz: KClass<T>) : Entity?{
    val domainName = entityMap["domainName"].toString()
    val id = entityMap["id"].toString()
    if (domainName == null && id == null) {
        return null
    }

    return when(clazz) {
        Floor::class -> Floor(domainName, id)
        Room::class -> Room(domainName, id)
        Equip::class -> Equip(domainName, id)
        Device::class -> Device(domainName, id)
        Point::class -> Point(domainName, id)
        RawPoint::class -> RawPoint(domainName, id)
        SettingPoint::class -> SettingPoint(domainName, id)
        else -> null
    }
}

fun Boolean.toInt() = if (this) 1 else 0