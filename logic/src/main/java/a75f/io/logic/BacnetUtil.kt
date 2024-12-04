package a75f.io.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Tags.BACNET_ID
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.hvac.Stage

    const val ANALOG_VALUE = "AnalogValue"
    const val BINARY_VALUE = "BinaryValue"
    const val MULTI_STATE_VALUE = "MultiStateValue"
    const val OCCUPANCY = "occupancy"
    const val HUMIDITY = "humidity"
    const val ILLUMINANCE = "illuminance"
    const val CO2 = "co2"
    const val VOC = "voc"
    const val CO2EQUIVALENT = "co2Equivalent"
    const val SOUND = "sound"


    fun addBacnetTags(
        point: Point,
        objectId: Int,
        objectType: String,
        nodeAddress: Int
    ) {
        val nodeAdd = nodeAddress - L.ccu().addressBand + 1000
        val bacnetId = "$nodeAdd$objectId"
        point.bacnetId = bacnetId.toInt()
        point.bacnetType = objectType
        CcuLog.d("CCU_BACNET", "Tag added to ${point.displayName}, bacnetId: $bacnetId and bacnetType: $objectType")
    }

fun getBacnetId(ordinal: Double): Int {
    return when (ordinal.toInt()) {
        Stage.COOLING_1.ordinal -> COOLINGSTAGE1ID
        Stage.COOLING_2.ordinal -> COOLINGSTAGE2ID
        Stage.COOLING_3.ordinal -> COOLINGSTAGE3ID
        Stage.COOLING_4.ordinal -> COOLINGSTAGE4ID
        Stage.COOLING_5.ordinal -> COOLINGSTAGE5ID
        Stage.HEATING_1.ordinal -> HEATINGSTAGE1ID
        Stage.HEATING_2.ordinal -> HEATINGSTAGE2ID
        Stage.HEATING_3.ordinal -> HEATINGSTAGE3ID
        Stage.HEATING_4.ordinal -> HEATINGSTAGE4ID
        Stage.HEATING_5.ordinal -> HEATINGSTAGE5ID
        Stage.FAN_1.ordinal -> FANSTAGE1ID
        Stage.FAN_2.ordinal -> FANSTAGE2ID
        Stage.FAN_3.ordinal -> FANSTAGE3ID
        Stage.FAN_4.ordinal -> FANSTAGE4ID
        Stage.FAN_5.ordinal -> FANSTAGE5ID
        else -> 0
    }
}

fun generateBacnetIdForRoom(zoneID: String): Int {
    var bacnetID = 1
    var isBacnetIDUsed = true
    try {
        val currentRoom = CCUHsApi.getInstance().readMapById(zoneID)
        if (currentRoom.containsKey(BACNET_ID) && currentRoom[BACNET_ID] != 0) {
            val bacnetID2 = (currentRoom[BACNET_ID].toString() + "").toDouble()
            CcuLog.d(L.TAG_CCU_BACNET, "Already have bacnetID $bacnetID2")
            return bacnetID2.toInt()
        }
        val rooms = CCUHsApi.getInstance().readAllEntities("room")
        if (rooms.size == 0) {
            CcuLog.d(L.TAG_CCU_BACNET, "rooms size : 0 ")
            return bacnetID
        }
        while (isBacnetIDUsed) {
            for (room in rooms) {
                if (room.containsKey(BACNET_ID)
                    && room[BACNET_ID] != 0
                    && (room[Tags.BACNET_ID].toString() + "").toDouble() == bacnetID.toDouble()
                ) {
                    CcuLog.d(L.TAG_CCU_BACNET,"In looping over - {bacnetID: ${room[BACNET_ID]} ,tempBacnetID: $bacnetID} - room object: $room")
                    bacnetID += 1
                    isBacnetIDUsed = true
                    break
                } else {
                    isBacnetIDUsed = false
                }
            }
        }
        CcuLog.d(L.TAG_CCU_BACNET, "Generated bacnetID: $bacnetID")
    } catch (e: NumberFormatException) {
        e.printStackTrace()
    }
    return bacnetID
}

fun updateRoom(roomRef: String, floorRef: String){
    try {
        val bacnetId = generateBacnetIdForRoom(roomRef)
        val zone = HSUtil.getZone(roomRef, floorRef)
        zone.bacnetId = bacnetId
        zone.bacnetType = Tags.DEVICE
        CCUHsApi.getInstance().updateZone(zone, zone.id)
    } catch (e: NullPointerException) {
        CcuLog.d(L.TAG_CCU_BACNET, "Unable to update zone:  " + e.message)
        e.printStackTrace()
    }
}
