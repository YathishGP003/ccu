package a75f.io.domain

import a75f.io.domain.api.Domain

object TestUtil {
    fun dumpDomain() {
        println("## Dump Domain start ## ")
        Domain.site?.floors?.entries?.forEach{
            val floor = it.value
            println(floor)
            floor.rooms.entries.forEach{ r ->
                val room =  r.value
                println(room)
                room.equips.entries.forEach{ e ->
                    val equip = e.value
                    println(equip)
                    equip.points.entries.forEach{ point -> println(point) }
                }
                room.devices.entries.forEach{ e ->
                    val device = e.value
                    println(device)
                    device.points.entries.forEach{ point -> println(point) }
                }
            }
        }
        println("## Dump Domain end ## ")
    }
}