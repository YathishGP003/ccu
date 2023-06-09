package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Entity
import a75f.io.domain.api.Point


/**
 *
 */
object DomainManager {

    fun buildDomain(hayStack : CCUHsApi) {
        val floors = hayStack.readAllEntities("floor")
        floors.forEach{ floor ->
            run {
                val floorId = floor["id"]
                floorId?.let {
                    Domain.site.addFloor(floor)
                    val rooms = hayStack.readAllEntities(
                        "room and floorRef == $floorId")

                    rooms.forEach { room ->
                        val roomId = room["id"]
                        roomId?.let {
                            Domain.site.floors[floorId]?.addRoom(room)
                            addEquips(hayStack, floorId as String, roomId as String)
                            addDevices(hayStack, floorId, roomId)
                        }
                    }
                }
            }
        }
    }

    private fun addEquips(hayStack: CCUHsApi, floorId : String, roomId : String) {
        val equips = hayStack.readAllEntities("equip and roomRef == $roomId")
        equips.forEach { equip ->
            val equipId = equip["id"]
            equipId?.let {
                Domain.site.floors[floorId]?.rooms?.get(roomId)?.addEquip(equip)
                val points =
                    hayStack.readAllEntities("point and equipRef == $equipId")
                points.forEach {
                    val domainName = it["domainName"]
                    domainName?.let {
                    }
                    Domain.site.floors[floorId]?.rooms?.get(roomId)?.equips?.get(equipId)
                        ?.addPoint(it)
                }
            }
        }
    }

    private fun addDevices(hayStack: CCUHsApi, floorId : String, roomId : String) {
        val devices = hayStack.readAllEntities("device and roomRef == $roomId")
        devices.forEach { device ->
            val deviceId = device["id"]
            deviceId?.let {
                Domain.site.floors[floorId]?.rooms?.get(roomId)?.addDevice(device)
                val points =
                    hayStack.readAllEntities("point and deviceRef == $deviceId")
                points.forEach {
                    val domainName = it["domainName"]
                    domainName?.let {
                    }
                    Domain.site.floors[floorId]?.rooms?.get(roomId)?.devices?.get(deviceId)
                        ?.addPoint(it)
                }
            }
        }
    }

    fun addEquip(domainE : Entity, hayStackPoint : a75f.io.api.haystack.Point) {

    }

    fun addPoint(hayStackPoint : a75f.io.api.haystack.Point) {
        Domain.site.floors[hayStackPoint.floorRef]?.
                rooms?.get(hayStackPoint.roomRef)?.equips?.get(hayStackPoint.equipRef)?.
                points?.add(Point(hayStackPoint.domainName, hayStackPoint.id))
    }
}

