package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Device
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Equip
import a75f.io.domain.api.Point
import a75f.io.domain.api.Site


/**
 *
 */
object DomainManager {

    fun buildDomain(hayStack : CCUHsApi) {
        val site = hayStack.site
        site?.let { Domain.site = Site(site.displayName, site.id) }

        val ccu =  hayStack.ccu
        ccu?.let {
            Domain.site?.addCcu(it)
            addSystemEquip(hayStack, it["id"].toString())
            addSystemDevice(hayStack, it["id"].toString())
        }

        val floors = hayStack.readAllEntities("floor")
        floors.forEach{ floor ->
            val floorId = floor["id"]
            floorId?.let {
                Domain.site?.addFloor(floor)
                val rooms = hayStack.readAllEntities(
                    "room and floorRef == \"$floorId\"")

                rooms.forEach { room ->
                    val roomId = room["id"]
                    roomId?.let {
                        Domain.site?.floors?.get(floorId.toString())?.addRoom(room)
                        addEquips(hayStack, floorId.toString(), roomId.toString())
                        addDevices(hayStack, floorId.toString(), roomId.toString())
                    }
                }
            }

        }
    }

    private fun addSystemEquip(hayStack: CCUHsApi, ccuId: String) {
        val systemEquip = hayStack.readAllEntities("system and equip")
        systemEquip.forEach { equip->
            val equipId = equip["id"]
            equipId?.let {
                Domain.site?.ccus?.get(ccuId)?.addEquip(equip)
                val points =
                    hayStack.readAllEntities("point and equipRef == \"$equipId\"")
                points.forEach { point ->
                    val domainName = point["domainName"]
                    domainName?.let {
                        Domain.site?.ccus?.get(ccuId)?.equips?.get(equipId.toString())?.addPoint(point)
                    }
                }
            }
        }
    }
    private fun addSystemDevice(hayStack: CCUHsApi,ccuId: String) {
        val devices = hayStack.readAllEntities("device and roomRef == \"SYSTEM\"")
        devices.forEach { device ->
            val deviceId = device["id"]
            deviceId?.let {
                Domain.site?.ccus?.get(ccuId)?.addDevice(device)
                val points =
                    hayStack.readAllEntities("point and deviceRef == \"$deviceId\"")
                points.forEach { point ->
                    val domainName = point["domainName"]
                    domainName?.let {
                        Domain.site?.ccus?.get(ccuId)?.devices?.get(deviceId)?.addPoint(point)
                    }
                }
            }
        }
    }

    private fun addEquips(hayStack: CCUHsApi, floorId : String, roomId : String) {
        val equips = hayStack.readAllEntities("equip and roomRef == \"$roomId\"")
        equips.forEach { equip ->
            val equipId = equip["id"]
            equipId?.let {
                Domain.site?.floors?.get(floorId)?.rooms?.get(roomId)?.addEquip(equip)
                val points =
                    hayStack.readAllEntities("point and equipRef == \"$equipId\"")
                points.forEach {point ->
                    val domainName = point["domainName"]
                    domainName?.let {
                        Domain.site?.floors?.get(floorId)?.rooms
                            ?.get(roomId)?.equips?.get(equipId.toString())?.addPoint(point)
                    }
                }
            }
        }
    }

    private fun addDevices(hayStack: CCUHsApi, floorId : String, roomId : String) {
        val devices = hayStack.readAllEntities("device and roomRef == \"$roomId\"")
        devices.forEach { device ->
            val deviceId = device["id"]
            deviceId?.let {
                Domain.site?.floors?.get(floorId)?.rooms?.get(roomId)?.addDevice(device)
                val points =
                    hayStack.readAllEntities("point and deviceRef == \"$deviceId\"")
                points.forEach { point ->
                    val domainName = point["domainName"]
                    domainName?.let {
                        Domain.site?.floors?.get(floorId)?.rooms?.get(roomId)?.devices?.get(deviceId)
                            ?.addPoint(point)
                    }

                }
            }
        }
    }

    fun addEquip(hayStackEquip : a75f.io.api.haystack.Equip) {
        if (Domain.site == null){
            return
        }
        Domain.site?.floors?.get(hayStackEquip.floorRef)?.
        rooms?.get(hayStackEquip.roomRef)?.equips?.put(hayStackEquip.id, Equip(hayStackEquip.domainName, hayStackEquip.id))
    }

    fun addPoint(hayStackPoint : a75f.io.api.haystack.Point) {
        if (Domain.site == null) {
            return
        }
        Domain.site?.floors?.get(hayStackPoint.floorRef)?.
                rooms?.get(hayStackPoint.roomRef)?.equips?.get(hayStackPoint.equipRef)?.
                points?.put(hayStackPoint.domainName, Point(hayStackPoint.domainName, hayStackPoint.id))
    }

    fun addDevice(hayStackDevice : a75f.io.api.haystack.Device) {
        if (Domain.site == null) {
            return
        }
        Domain.site?.floors?.get(hayStackDevice.floorRef)?.
        rooms?.get(hayStackDevice.roomRef)?.devices?.put(hayStackDevice.id, Device(hayStackDevice.domainName, hayStackDevice.id))

        val floor = Domain.site?.floors?.get(hayStackDevice.floorRef)
        val room = Domain.site?.floors?.get(hayStackDevice.floorRef)?.
        rooms?.get(hayStackDevice.roomRef)
        val device = Domain.site?.floors?.get(hayStackDevice.floorRef)?.
        rooms?.get(hayStackDevice.roomRef)?.equips?.get(hayStackDevice.equipRef)
    }

    fun addRawPoint(hayStackPoint : a75f.io.api.haystack.RawPoint) {
        if (Domain.site == null) {
            return
        }
        Domain.site?.floors?.get(hayStackPoint.floorRef)?.
        rooms?.get(hayStackPoint.roomRef)?.devices?.get(hayStackPoint.deviceRef)?.
        points?.put(hayStackPoint.domainName, Point(hayStackPoint.domainName, hayStackPoint.id))
    }
}

