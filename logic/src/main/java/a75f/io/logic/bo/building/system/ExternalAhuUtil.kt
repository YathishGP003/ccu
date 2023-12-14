package a75f.io.logic.bo.building.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.DomainName.dualSetpointControlEnable
import a75f.io.domain.api.DomainName.systemCoolingSATMaximum
import a75f.io.domain.api.DomainName.systemHeatingSATMinimum
import a75f.io.domain.api.DomainName.systemSATMaximum
import a75f.io.domain.api.DomainName.systemSATMinimum
import a75f.io.domain.api.Equip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.util.RxjavaUtil
import android.util.Log
import io.seventyfivef.ph.core.Tags

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
const val OCCUPANCY_MODE = "mode and occupied and sp"
const val DISCHARGE_AIR_TEMP = "air and discharge and temp and sensor"
const val DUCT_STATIC_PRESSURE_SENSOR = "air and discharge and pressure and sensor"

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
        CcuLog.i(L.TAG_CCU_MODBUS, " point not found $query")
    }
}

fun updateDefaultSetPoints(
    conditioningMode: SystemMode,
    systemEquip: Equip,
    lastLoopDirection: TempDirection
): Double {
    val isDualSetPointEnabled = isConfigEnabled(systemEquip, dualSetpointControlEnable)
    return when (conditioningMode) {
        SystemMode.AUTO , SystemMode.OFF-> {
            if (!isDualSetPointEnabled) {
                Domain.getPointFromDomain(systemEquip, systemSATMinimum)
            } else {
                when (lastLoopDirection) {
                    TempDirection.COOLING -> Domain.getPointFromDomain(systemEquip,
                        systemCoolingSATMaximum
                    )
                    else -> Domain.getPointFromDomain(systemEquip, systemHeatingSATMinimum)
                }
            }
        }
        SystemMode.HEATONLY -> {
            if (!isDualSetPointEnabled)
                Domain.getPointFromDomain(systemEquip, systemSATMinimum)
            else 
                Domain.getPointFromDomain(systemEquip, systemHeatingSATMinimum)
        }
        SystemMode.COOLONLY -> {
            if (!isDualSetPointEnabled)
                Domain.getPointFromDomain(systemEquip, systemSATMaximum)
            else
                Domain.getPointFromDomain(systemEquip, systemCoolingSATMaximum)
        }
    }
}

fun isConfigEnabled(systemEquip: Equip, domainName: String): Boolean {
    return Domain.getPointFromDomain(systemEquip, domainName) == 1.0
}

fun writePointForCcuUser(hayStack: CCUHsApi, domainName: String,value: Double) {
    val point = Domain.readPointOnEquip(domainName,L.ccu().systemProfile.systemEquipRef)
    if (point.isNotEmpty()) {
        RxjavaUtil.executeBackground {
            hayStack.writePointForCcuUser(point[Tags.ID].toString(), HayStackConstants.SYSTEM_POINT_LEVEL, value, 0)
        }
    }
}
fun updatePointHistoryAndDefaultValue(domainName: String, value: Double) {
    Domain.writeDefaultValByDomainName(domainName, value)
    Domain.writeHisValByDomainName(domainName, value)
}

data class BasicDabConfig(
    var coolingLoop: Int,
    val heatingLoop: Int,
    val loopOutput: Double,
    val weightedAverageCO2: Double,
)

fun getTempDirection(heatingLoop: Int): TempDirection {
    return if (heatingLoop > 0)
        TempDirection.HEATING
    else
        TempDirection.COOLING
}

enum class TempDirection {
    COOLING, HEATING
}

fun logIt(msg: String) {
    Log.i(L.TAG_CCU_SYSTEM, msg)
}

