package a75f.io.domain.api

object Domain {
    var site: Site? = null

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
    fun getDevicePoint(pointId : String, deviceRef : String) : RawPoint? {
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
}