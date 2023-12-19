package a75f.io.logic.bo.building.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.DomainName.dcvDamperCalculatedSetpoint
import a75f.io.domain.api.DomainName.dcvDamperControlEnable
import a75f.io.domain.api.DomainName.dcvLoopOutput
import a75f.io.domain.api.DomainName.dehumidifierEnable
import a75f.io.domain.api.DomainName.dehumidifierOperationEnable
import a75f.io.domain.api.DomainName.dualSetpointControlEnable
import a75f.io.domain.api.DomainName.humidifierEnable
import a75f.io.domain.api.DomainName.humidifierOperationEnable
import a75f.io.domain.api.DomainName.occupancyModeControl
import a75f.io.domain.api.DomainName.systemCO2DamperOpeningRate
import a75f.io.domain.api.DomainName.systemCO2Threshold
import a75f.io.domain.api.DomainName.systemCoolingSATMaximum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMaximum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMinimum
import a75f.io.domain.api.DomainName.systemHeatingSATMinimum
import a75f.io.domain.api.DomainName.systemOccupancyMode
import a75f.io.domain.api.DomainName.systemSATMaximum
import a75f.io.domain.api.DomainName.systemSATMinimum
import a75f.io.domain.api.DomainName.systemtargetMaxInsideHumidty
import a75f.io.domain.api.DomainName.systemtargetMinInsideHumidty
import a75f.io.domain.api.Equip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.tuners.TunerUtil
import a75f.io.logic.util.RxjavaUtil
import android.util.Log
import io.seventyfivef.ph.core.Tags

/**
 * Created by Manjunath K on 27-10-2023.
 */

fun mapToSetPoint(min: Double, max: Double, current: Double): Double =
    ((max - min) * (current / 100.0) + min)


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
        val pointId = point[Tags.ID].toString()
        val currentValue = haystack.readHisValById(pointId)
        if (currentValue != value) {
            setPointsList.add(pointId)
            if (point.containsKey(Tags.HIS))
                haystack.writeHisValById(pointId, value)
            if (point.containsKey(Tags.WRITABLE))
                haystack.writeDefaultValById(pointId, value)
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
        SystemMode.AUTO, SystemMode.OFF -> {
            if (!isDualSetPointEnabled) {
                Domain.getPointByDomain(systemEquip, systemSATMinimum)
            } else {
                when (lastLoopDirection) {
                    TempDirection.COOLING -> Domain.getPointByDomain(
                        systemEquip,
                        systemCoolingSATMaximum
                    )
                    else -> Domain.getPointByDomain(systemEquip, systemHeatingSATMinimum)
                }
            }
        }

        SystemMode.HEATONLY -> {
            if (!isDualSetPointEnabled)
                Domain.getPointByDomain(systemEquip, systemSATMinimum)
            else
                Domain.getPointByDomain(systemEquip, systemHeatingSATMinimum)
        }

        SystemMode.COOLONLY -> {
            if (!isDualSetPointEnabled)
                Domain.getPointByDomain(systemEquip, systemSATMaximum)
            else
                Domain.getPointByDomain(systemEquip, systemCoolingSATMaximum)
        }
    }
}

fun getTunerByDomainName(systemEquip: Equip,domainName: String): Double =
    TunerUtil.readTunerValByQuery("domainName == \"$domainName\"", systemEquip.id)

fun isConfigEnabled(systemEquip: Equip, domainName: String): Boolean =
    Domain.getPointByDomain(systemEquip, domainName) == 1.0

fun writePointForCcuUser(hayStack: CCUHsApi, domainName: String, value: Double) {
    val point = Domain.readPointOnEquip(domainName, L.ccu().systemProfile.systemEquipRef)
    if (point.isNotEmpty()) {
        RxjavaUtil.executeBackground {
            hayStack.writePointForCcuUser(
                point[Tags.ID].toString(),
                HayStackConstants.SYSTEM_POINT_LEVEL,
                value,
                0
            )
        }
    }
}

fun calculateSATSetPoints(
    systemEquip: Equip,
    basicConfig: BasicConfig,
    externalEquipId: String?,
    conditioningMode: SystemMode,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
    loopRunningDirection: TempDirection,
    coolingLoop: Double,
    heatingLoop: Double
) {
    val isSetPointEnabled = isConfigEnabled(systemEquip, DomainName.satSetpointControlEnable)
    if (!isSetPointEnabled) {
        logIt("satSetpointControl disabled")
        return
    }
    L.ccu().oaoProfile?.let {
        checkOaoLoop(basicConfig, conditioningMode,coolingLoop,heatingLoop)
    }

    val satSetPointLimits = getSetPointMinMax(systemEquip, basicConfig.heatingLoop)
    val satSetPointValue: Double = if (basicConfig.loopOutput == 0.0)
        updateDefaultSetPoints(conditioningMode, systemEquip, loopRunningDirection)
    else
        mapToSetPoint(satSetPointLimits.first,satSetPointLimits.second, basicConfig.loopOutput)

    updatePointValue(systemEquip, DomainName.supplyAirflowTemperatureSetpoint, satSetPointValue)
    externalEquipId?.let {
        pushSatSetPoints(haystack, externalEquipId, satSetPointValue, externalSpList)
    }
    logIt("SATMin: ${satSetPointLimits.first} SATMax ${satSetPointLimits.second} satSetPointValue: $satSetPointValue")
}

fun checkOaoLoop(
    basicConfig: BasicConfig,
    conditioningMode: SystemMode,
    systemCoolingLoopOp: Double,
    systemHeatingLoopOp: Double,
) {
   val tempDirection = getTempDirection(basicConfig.heatingLoop)
    if (tempDirection == TempDirection.COOLING &&
        (conditioningMode == SystemMode.COOLONLY || conditioningMode == SystemMode.AUTO)
    ) {
        val smartPurgeDabFanLoopOp: Double = TunerUtil.readTunerValByQuery(
            "system and purge and dab and fan and loop and output",
            L.ccu().oaoProfile.equipRef
        )
        if (L.ccu().oaoProfile.isEconomizingAvailable) {
            val economizingToMainCoolingLoopMap = TunerUtil.readTunerValByQuery(
                "oao and economizing and main and cooling and loop and map",
                L.ccu().oaoProfile.equipRef
            )
            basicConfig.coolingLoop = (systemCoolingLoopOp * 100 / economizingToMainCoolingLoopMap).coerceAtLeast(systemHeatingLoopOp).coerceAtLeast(smartPurgeDabFanLoopOp).toInt()
        }
    }
}

fun calculateDSPSetPoints(
    systemEquip: Equip,
    loopOutput: Double,
    externalEquipId: String?,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
) {
    if (!isConfigEnabled(systemEquip, DomainName.staticPressureSetpointControlEnable)) {
        logIt("StaticPressureSp is disabled")
        return
    }
    val analogFanMultiplier = getTunerByDomainName(systemEquip,
        DomainName.dabAnalogFanSpeedMultiplier
    )
    val fanLoop = loopOutput * analogFanMultiplier.coerceAtMost(100.0)
    val min = Domain.getPointByDomain(systemEquip, DomainName.systemStaticPressureMinimum)
    val max = Domain.getPointByDomain(systemEquip, DomainName.systemStaticPressureMaximum)
    val dspSetPoint: Double = mapToSetPoint(min, max, fanLoop).coerceAtMost(max)
    updatePointValue(systemEquip, DomainName.ductStaticPressureSetpoint, dspSetPoint)
    updatePointValue(systemEquip, DomainName.fanLoopOutput, fanLoop)
    externalEquipId?.let {
        pushDuctStaticPressure(haystack, externalEquipId, dspSetPoint, externalSpList)
    }
    logIt("DSP Min: $min DSP Max: $max analogFanMultiplier: $analogFanMultiplier ductStaticPressureSetPoint: $dspSetPoint")
}
fun handleHumidityOperation(
    systemEquip: Equip,
    externalEquipId: String?,
    occupancyMode: Occupancy,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
    humidityHysteresis: Double,
    currentHumidity: Double
) {
    val currentStatus = Domain.getHisByDomain(systemEquip, humidifierEnable)
    var newStatus = 0.0

    if ((occupancyMode == Occupancy.UNOCCUPIED || occupancyMode == Occupancy.VACATION)
        && (currentStatus == 1.0)
    ) {
        updatePointValue(systemEquip, humidifierEnable, 0.0)
        externalEquipId?.let {
            pushHumidifierCmd(haystack, externalEquipId, 0.0, externalSpList)
        }
        return
    }

    val isHumidifierEnabled = isConfigEnabled(systemEquip, humidifierOperationEnable)
    if (isHumidifierEnabled) {
        val targetMinInsideHumidity =
            Domain.getPointByDomain(systemEquip, systemtargetMinInsideHumidty)

        if (currentHumidity > 0 && currentHumidity < targetMinInsideHumidity)
            newStatus = 1.0
        else if (currentStatus > 0 && currentHumidity > (targetMinInsideHumidity + humidityHysteresis))
            newStatus = 0.0

        logIt(
            "currentHumidity $currentHumidity " +
                    " humidityHysteresis: $humidityHysteresis" +
                    " targetMinInsideHumidity $targetMinInsideHumidity" +
                    " Humidifier $newStatus"
        )
    } else {
        logIt("Humidifier control is disabled")
    }

    if (currentStatus != newStatus) {
        updatePointValue(systemEquip, humidifierEnable, newStatus)
        externalEquipId?.let {
            pushHumidifierCmd(haystack, externalEquipId, newStatus, externalSpList)
        }
    }
}

fun handleDeHumidityOperation(
    systemEquip: Equip,
    externalEquipId: String?,
    occupancyMode: Occupancy,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
    humidityHysteresis: Double,
    currentHumidity: Double
) {
    val currentStatus = Domain.getHisByDomain(systemEquip, dehumidifierEnable)
    var newStatus = 0.0

    if ((occupancyMode == Occupancy.UNOCCUPIED || occupancyMode == Occupancy.VACATION)
        && (currentStatus == 1.0)
    ) {
        updatePointValue(systemEquip, dehumidifierEnable, 0.0)
        externalEquipId?.let {
            pushDeHumidifierCmd(haystack, externalEquipId, 0.0, externalSpList)
        }
        return
    }

    val isDeHumidifierEnabled = isConfigEnabled(systemEquip, dehumidifierOperationEnable)
    if (isDeHumidifierEnabled) {
        val targetMaxInsideHumidity =
            Domain.getPointByDomain(systemEquip, systemtargetMaxInsideHumidty)

        if (currentHumidity > 0 && currentHumidity > targetMaxInsideHumidity)
            newStatus = 1.0
        else if (currentStatus > 0 && currentHumidity < (targetMaxInsideHumidity - humidityHysteresis))
            newStatus = 0.0

        logIt(" targetMaxInsideHumidity: $targetMaxInsideHumidity DeHumidifier $newStatus")
    } else {
        logIt("DeHumidifier control is disabled")
    }

    if (currentStatus != newStatus) {
        updatePointValue(systemEquip, dehumidifierEnable, newStatus)
        externalEquipId?.let {
            pushDeHumidifierCmd(haystack, externalEquipId, newStatus, externalSpList)
        }
    }
}

fun operateDamper(
    systemEquip: Equip,
    co2: Double,
    occupancyMode: Occupancy,
    externalEquipId: String?,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
) {
    val isDcvControlEnabled = isConfigEnabled(systemEquip, dcvDamperControlEnable)

    if (!isDcvControlEnabled) {
        logIt("DCV control is disabled")
        return
    }

    val dcvMin = Domain.getPointByDomain(systemEquip, systemDCVDamperPosMinimum)
    val dcvMax = Domain.getPointByDomain(systemEquip, systemDCVDamperPosMaximum)
    val damperOpeningRate = Domain.getPointByDomain(systemEquip, systemCO2DamperOpeningRate)
    val co2Threshold = Domain.getPointByDomain(systemEquip, systemCO2Threshold)

    var damperOperationPercent = 0.0

    if (shouldOperateDamper(co2, occupancyMode, co2Threshold))
        damperOperationPercent =
            calculateDamperOperationPercent(co2, co2Threshold, damperOpeningRate)
    else if (shouldResetDamper(co2, occupancyMode, co2Threshold))
        damperOperationPercent = 0.0

    damperOperationPercent = minOf(damperOperationPercent, 100.0)

    val dcvSetPoint = mapToSetPoint(dcvMin, dcvMax, damperOperationPercent).coerceAtMost(dcvMax)

    updatePointValue(systemEquip, dcvLoopOutput, damperOperationPercent)
    updatePointValue(systemEquip, dcvDamperCalculatedSetpoint, dcvSetPoint)
    externalEquipId?.let {
        pushDamperCmd(haystack, externalEquipId, dcvSetPoint, externalSpList)
    }

    logIt(
        "DCVDamperPosMinimum: $dcvMin DCVDamperPosMaximum: $dcvMax co2: $co2" +
                " DamperOpeningRate $damperOpeningRate CO2Threshold: $co2Threshold" +
                " damperOperationPercent $damperOperationPercent dcvSetPoint: $dcvSetPoint"
    )
}

fun setOccupancyMode(
    systemEquip: Equip,
    externalEquipId: String?,
    occupancy: Occupancy,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
) {
    val occupancyMode = when (occupancy) {
        Occupancy.UNOCCUPIED, Occupancy.VACATION, Occupancy.AUTOAWAY -> 0.0
        else -> 1.0
    }

    if (isConfigEnabled(systemEquip, occupancyModeControl)) {
        updatePointValue(systemEquip, systemOccupancyMode, occupancyMode)
        externalEquipId?.let {
            pushOccupancyMode(haystack, it, occupancyMode, externalSpList)
        }
    } else {
        logIt("OccupancyModeControlEnabled disabled")
    }
}

private fun shouldOperateDamper(
    sensorCO2: Double,
    mode: Occupancy,
    systemCO2Threshold: Double
): Boolean =
    sensorCO2 > 0 && sensorCO2 > systemCO2Threshold && (mode == Occupancy.OCCUPIED || mode == Occupancy.AUTOFORCEOCCUPIED)

private fun shouldResetDamper(
    sensorCO2: Double,
    mode: Occupancy,
    systemCO2Threshold: Double
): Boolean =
    mode == Occupancy.UNOCCUPIED || mode == Occupancy.PRECONDITIONING || sensorCO2 < systemCO2Threshold

private fun calculateDamperOperationPercent(
    sensorCO2: Double,
    threshold: Double,
    openingRate: Double
): Double =
    (sensorCO2 - threshold) / openingRate

fun getSetPointMinMax(equip: Equip, heatingLoop: Int): Pair<Double, Double> {
    val tempDirection = getTempDirection(heatingLoop)
    val isDualSetPointEnabled = isConfigEnabled(equip, dualSetpointControlEnable)

    val minKey: String
    val maxKey: String

    when (tempDirection) {
        TempDirection.COOLING -> {
            minKey = if (isDualSetPointEnabled) systemCoolingSATMaximum else systemSATMaximum
            maxKey = if (isDualSetPointEnabled) DomainName.systemCoolingSATMinimum else systemSATMinimum
        }

        TempDirection.HEATING -> {
            minKey = if (isDualSetPointEnabled) systemHeatingSATMinimum else systemSATMinimum
            maxKey = if (isDualSetPointEnabled) DomainName.systemHeatingSATMaximum else systemSATMaximum
        }
    }

    val minSetPoint = Domain.getPointByDomain(equip, minKey)
    val maxSetPoint = Domain.getPointByDomain(equip, maxKey)

    return Pair(minSetPoint, maxSetPoint)
}

fun getConditioningMode(systemEquip: Equip) =
    SystemMode.values()[Domain.getPointByDomain(systemEquip, DomainName.conditioningMode).toInt()]

fun updatePointValue(equip: Equip, domainName: String, pointValue: Double) =
    Domain.writePointByDomain(equip, domainName, pointValue)

fun updatePointHistoryAndDefaultValue(domainName: String, value: Double) {
    Domain.writeDefaultValByDomain(domainName, value)
    Domain.writeHisValByDomain(domainName, value)
}

data class BasicConfig(
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

