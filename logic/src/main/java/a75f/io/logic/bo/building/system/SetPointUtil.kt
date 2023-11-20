package a75f.io.logic.bo.building.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog

/**
 * Created by Manjunath K on 27-10-2023.
 */

fun mapToSetPoint(min: Double, max: Double, current: Double): Double {
    return ((max - min) * (current / 100.0) + min)
}


const val SAT_SET_POINT = "air and discharge and sp and temp"
const val DUCT_STATIC_PRESSURE = "pressure and air and discharge and sp"
const val DAMPER_CMD = "cmd and outside and dcv and damper"
const val HUMIDIFIER_CMD = "cmd and enable and humidifier"
const val DEHUMIDIFIER_CMD = "cmd and dessicantDehumidifier"
const val OCCUPANCY_MODE = "hvac and occupancy and sensor"


fun pushSatSetPoints(
    haystack: CCUHsApi,
    equipId: String,
    value: Double,
    setPointsList: ArrayList<String>
) {
    mapModbusPoint(haystack, SAT_SET_POINT, equipId, value, setPointsList)
}

fun pushDuctStaticPressure(
    haystack: CCUHsApi,
    equipId: String,
    value: Double,
    setPointsList: ArrayList<String>
) {
    mapModbusPoint(haystack, DUCT_STATIC_PRESSURE, equipId, value, setPointsList)
}

fun pushDamperCmd(
    haystack: CCUHsApi,
    equipId: String,
    value: Double,
    setPointsList: ArrayList<String>
) {
    mapModbusPoint(haystack, DAMPER_CMD, equipId, value, setPointsList)
}
fun pushOccupancyMode(
    haystack: CCUHsApi,
    equipId: String,
    value: Double,
    setPointsList: ArrayList<String>
) {
    mapModbusPoint(haystack, OCCUPANCY_MODE, equipId, value, setPointsList)
}

fun pushHumidifierCmd(
    haystack: CCUHsApi,
    equipId: String,
    value: Double,
    setPointsList: ArrayList<String>
) {
    mapModbusPoint(haystack, HUMIDIFIER_CMD, equipId, value, setPointsList)
}

fun pushDeHumidifierCmd(
    haystack: CCUHsApi,
    equipId: String,
    value: Double,
    setPointsList: ArrayList<String>
) {
    mapModbusPoint(haystack, DEHUMIDIFIER_CMD, equipId, value, setPointsList)
}

fun mapModbusPoint(
    haystack: CCUHsApi,
    query: String,
    equipId: String,
    value: Double,
    setPointsList: ArrayList<String>
) {
    val point = haystack.readEntity("$query and equipRef == \"$equipId\"")
    if (point.isNotEmpty()) {
        val currentValue = haystack.readHisValById(point["id"].toString())
        if (currentValue != value) {
            setPointsList.add(point["id"].toString())
            if (point.containsKey("his")) {
                haystack.writeHisValById(point["id"].toString(), value)
            }
            if (point.containsKey("writable")) {
                haystack.writeDefaultValById(point["id"].toString(), value)
            }
        }
    } else {
        CcuLog.i("DEV_DEBUG", " point not found $query")
    }
}
