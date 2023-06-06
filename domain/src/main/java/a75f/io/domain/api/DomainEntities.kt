package a75f.io.domain.api

/**
 * An entity definition that binds its domainName to database UUID.
 * domainName - domainName of the entity defined in model
 * id - uuid of the entity instance in haystack
 */
open class Entity (val domainName : String, val id : String)
open class EntityConfig(val domainName: String)

class Site(domainName : String, id : String) : Entity(domainName, id) {
    val floors = listOf<Floor>()
    val ccus = listOf<CcuDevice>()
}
class Floor(domainName : String, id : String) : Entity(domainName, id) {
    val rooms = listOf<Room>()
}
class Room(domainName : String, id : String) : Entity(domainName, id) {
    val equips = listOf<Equip>()
    val devices = listOf<Device>()
}
class Equip(domainName : String, id : String) : Entity(domainName, id) {
    val points = listOf<Point>()
}
class Device(domainName : String, id : String) : Entity(domainName, id) {
    val points = listOf<RawPoint>()
}

class CcuDevice(domainName : String, id : String) : Entity(domainName, id) {
    val points = listOf<SettingPoint>()
}
class Point(domainName : String, id : String) : Entity(domainName, id)
class RawPoint(domainName : String, id : String) : Entity(domainName, id)
class SettingPoint(domainName : String, id : String) : Entity(domainName, id)
