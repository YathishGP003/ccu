package a75f.io.domain.api

import java.util.Objects

/**
 * An entity definition that binds its domainName to database UUID.
 * domainName - domainName of the entity defined in model
 * id - uuid of the entity instance in haystack
 */
open class Entity (var domainName : String)
open class EntityConfig(val domainName: String)

class Site(domainName : String, val id : String) : Entity(domainName) {

    val floors = mutableMapOf<String, Floor>()
    val ccus = mutableMapOf<String, Ccu>()
    fun addFloor(entityMap : HashMap<Any, Any>) {
        val domainName = entityMap["domainName"].toString()
        val id = entityMap["id"].toString()
        floors[id] = Floor(domainName, id)
    }
    fun addCcu(entityMap : HashMap<Any, Any>) {
        // Revisit CCU don't have domain name
        val domainName = entityMap["dis"].toString()
        val id = entityMap["id"].toString()
        ccus[id] = Ccu(domainName, id)
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
       // val id = entityMap["id"].toString()
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
class Ccu(domainName : String, id : String) : Entity(domainName) {
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
open class Point(domainName : String, val equipRef: String) : Entity(domainName) {

    var id = ""
   /* constructor(domainName: String, equipRef : String, id: String = "") : this(domainName, id) {
        this.equipRef = equipRef
    }*/

    private fun requireId() {
        // If a query returned null before, we want to search again for the point id next time (rather than accepting the null id)
        // This matters for dynamic sensor points that could be queried before the point is actually created
        if (id.isEmpty() || id.equals("null")) {
            id = domainName.readPoint(equipRef)["id"].toString()
        }
        /*if (id.isEmpty()) {
            throw IllegalStateException("Invalid point domain name")
        }*/
    }
    fun pointExists() : Boolean {
        try {
            requireId()
        } catch (e: IllegalStateException) {
            return false
        }
        return id.isNotEmpty() && id != "null" && id != "@null"
    }
    fun readHisVal() : Double {
        requireId()
        return Domain.hayStack.readHisValById(id)
    }
    fun writeHisVal(hisVal : Double) {
        requireId()
        Domain.hayStack.writeHisValById(id, hisVal)
    }
    fun readPriorityVal() : Double {
        requireId()
        val priorityVal = Domain.hayStack.readPointPriorityVal(id)
        if (priorityVal == 0.0) {
            id = domainName.readPoint(equipRef)["id"].toString()
            return Domain.hayStack.readPointPriorityVal(id)
        }
        return priorityVal
    }
    fun writeDefaultVal(defaultVal : Any) {
        requireId()
        if (defaultVal is String) {
            Domain.hayStack.writeDefaultValById(id, defaultVal)
        } else if (defaultVal is Double) {
            Domain.hayStack.writeDefaultValById(id, defaultVal)
        }
    }
    fun readDefaultVal() : Double {
        requireId()
        return Domain.hayStack.readDefaultValById(id)
    }
    fun readDefaultStrVal() : String {
        requireId()
        return Domain.hayStack.readDefaultStrValById(id)
    }

    fun writeVal(level: Int, value : Double) {
        requireId()
        Domain.hayStack.writePointForCcuUser(id, level, value, 0, null)
    }
    fun writeVal(level: Int, who: String?, writableVal: Double?, duration: Int) {
        requireId()
        Domain.hayStack.writePoint(id, level, who, writableVal, duration)
    }

    override fun equals(other: Any?)
            = (other is Point) && this.domainName == other.domainName
    override fun hashCode() = Objects.hash(domainName)
}

open class PhysicalPoint(domainName : String, val deviceRef: String) : Entity (domainName) {
    var id = ""
    private fun requireId() {
        // If a query returned null before, we want to search again for the point id next time (rather than accepting the null id)
        // This matters for dynamic sensor points that could be queried before the point is actually created
        if (id.isEmpty() || id.equals("null")) {
            id = domainName.readPhysicalPoint(deviceRef)["id"].toString()
        }
        if (id.isEmpty()) {
            throw IllegalStateException("Invalid point domain name")
        }
    }
    fun readHisVal() : Double {
        requireId()
        return Domain.hayStack.readHisValById(id)
    }
    fun writeHisVal(hisVal : Double) {
        requireId()
        Domain.hayStack.writeHisValById(id, hisVal)
    }

    override fun equals(other: Any?)
            = (other is Point) && this.domainName == other.domainName
}
/*private fun  <T : Entity> getEntity(entityMap : HashMap<Any, Any>, clazz: KClass<T>) : Entity?{
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
}*/

fun Boolean.toInt() = if (this) 1 else 0

fun String.readPoint(equipRef: String) : Map <Any, Any>{
    return Domain.hayStack.readEntity("point and domainName == \"$this\" and equipRef == \"$equipRef\"")
}

fun String.readPhysicalPoint(deviceRef: String) : Map <Any, Any>{
    return Domain.hayStack.readEntity("point and domainName == \"$this\" and deviceRef == \"$deviceRef\"")
}

