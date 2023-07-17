package a75f.io.domain.api

object Domain {
    var site: Site? = null

    fun getPoint(pointId : String, equipRef : String) : Entity {
        site?.floors?.entries?.forEach{
            val floor = it.value
            floor.rooms.entries.forEach{ room ->
                val equip = room.value.equips.values.find { entry ->
                    entry.id == equipRef
                }
                val point = equip.points.entries.forEach { point ->
                }

            }
        }
    }
}