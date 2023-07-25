package a75f.io.logic

import a75f.io.api.haystack.Point
import android.util.Log

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
        val nodeAdd = nodeAddress - L.ccu().smartNodeAddressBand + 1000
        val bacnetId = "$nodeAdd$objectId"
        point.bacnetId = bacnetId.toInt()
        point.bacnetType = objectType
        Log.d("CCU_BACNET", "Tag added to ${point.displayName}, bacnetId: $bacnetId and bacnetType: $objectType")
    }
