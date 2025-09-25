package a75f.io.device.mesh

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain.hayStack
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.Point
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.logic.bo.util.CCUUtils.isCurrentTemperatureWithinLimits
import a75f.io.logic.interfaces.ZoneDataInterface

/**
 * Created by Manjunath K on 13-01-2025.
 */

/**
 * Update the RSSI value of the device
 */
fun updateHsRssi(rssiPhysicalPoint: PhysicalPoint, rssi: Int) {
    rssiPhysicalPoint.writeHisValueByIdWithoutCOV(rssi.toDouble())
    val logicalPoint = rssiPhysicalPoint.readPoint()
    if (logicalPoint.pointRef != null) {
        hayStack.writeHisValueByIdWithoutCOV(logicalPoint.pointRef, 1.0)
    }
}

fun updateTemp(currentTemp: PhysicalPoint, temp: Double, address: Int, refresh: ZoneDataInterface?) {
    val currentTempPoint = currentTemp.readPointMap()
    if (isCurrentTemperatureWithinLimits(temp/10,currentTempPoint)) {
        currentTemp.writeHisVal(temp)
        updateLogicalPoint(currentTemp, Pulse.getRoomTempConversion(temp))
        refresh?.updateTemperature(temp, address.toShort())
    } else {
        CcuLog.d(L.TAG_CCU_DEVICE, "Invalid Current Temp : $temp")
    }

}

fun updateOccupancy(occupancySensor: PhysicalPoint, occupancyDetected: Boolean) {
    occupancySensor.writeHisVal(if (occupancyDetected) 1.0 else 0.0)
    updateLogicalPoint(occupancySensor, if (occupancyDetected) 1.0 else 0.0)
}

fun updateIlluminance(illuminanceSensor: PhysicalPoint, illuminance: Double) {
    illuminanceSensor.writeHisVal(illuminance)
    updateLogicalPoint(illuminanceSensor, illuminance)
}

fun updateHumidity(humiditySensor: PhysicalPoint, humidity: Double) {
    humiditySensor.writeHisVal(humidity)
    updateLogicalPoint(humiditySensor, CCUUtils.roundToOneDecimal(humidity / 10.0))
}

fun updateSound(soundSensor: PhysicalPoint, sound: Double) {
    soundSensor.writeHisVal(sound)
    updateLogicalPoint(soundSensor, sound)
}

fun updateCo2(co2Sensor: PhysicalPoint, co2: Double) {
    co2Sensor.writeHisVal(co2)
    updateLogicalPoint(co2Sensor, co2)
}

fun updateLogicalPoint(point: PhysicalPoint, value: Double) {
    CcuLog.d(L.TAG_CCU_DEVICE, "PhysicalPoint: ${point.domainName}, value: $value")
    val logicalPoint = point.readPoint()
    if (logicalPoint.pointRef != null) {
        hayStack.writeHisValById(logicalPoint.pointRef, value)
    }
}

fun updateSensorData(domainName: String, equipRef: String, value: Double) {
    val map = hayStack.readEntity("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    hayStack.writePointValue(map, value)
}


fun getSensorMappedValue(point: HashMap<Any, Any>, rawValue: Double): Double {
    /*Analog's range is 0 to 10, if CCU receive 5v it means its 50 perc so we need to multiply by 10*/
    val deciVolts = rawValue * 10

    try {
        val min = point[Tags.MIN_VAL].toString().toDouble()
        val max = point[Tags.MAX_VAL].toString().toDouble()
        val analogConversion = ((max - min) * (deciVolts / 100.0)) + min
        return CCUUtils.roundToTwoDecimal(analogConversion)
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_DEVICE, "Error in getSensorMappedValue $point", e)
    }
    return 0.0
}


fun getHeatingDeadBand(roomRef: String): Double {
    val pointId = CCUHsApi.getInstance().readId("deadband and heating and not multiplier and roomRef == \"$roomRef\"")
    if (pointId != null)
        return HSUtil.getPriorityVal(pointId)
    return 0.0
}

fun getCoolingDeadBand(roomRef: String): Double {
    val pointId = CCUHsApi.getInstance().readId("deadband and cooling and not multiplier and roomRef == \"$roomRef\"")
    if (pointId != null)
        return HSUtil.getPriorityVal(pointId)
    return 0.0
}


fun getHeatingUserLimit(type: String, roomRef: String): Int {
    val hsApi = CCUHsApi.getInstance()
    var pointValue = hsApi.readPointPriorityValByQuery("schedulable and heating and user and limit and $type and roomRef == \"$roomRef\"")
    if (pointValue == 0.0) { // // Fall back to default value
        pointValue = hsApi.readPointPriorityValByQuery("point and schedulable and default and heating and user and limit and min")
    }
    return pointValue.toInt()
}

fun getCoolingUserLimit(type: String, roomRef: String): Int {
    val hsApi = CCUHsApi.getInstance()
    var pointValue = hsApi.readPointPriorityValByQuery("schedulable and cooling and user and limit and $type and roomRef == \"$roomRef\"")
    if (pointValue == 0.0) { // Fall back to default value
        pointValue = hsApi.readPointPriorityValByQuery("point and schedulable and default and cooling and user and limit and min")
    }
    return pointValue.toInt()
}

fun getPin(point: Point): Int {
    return try {
        Base64Util.decode(point.readDefaultStrVal()).toInt()
    } catch (e: Exception) {
        0
    }
}
