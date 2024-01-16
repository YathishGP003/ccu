package a75f.io.logic.bo.building.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName.airTempCoolingSp
import a75f.io.domain.api.DomainName.airTempHeatingSp
import a75f.io.domain.api.DomainName.co2WeightedAverage
import a75f.io.domain.api.DomainName.conditioningMode
import a75f.io.domain.api.DomainName.dcvDamperCalculatedSetpoint
import a75f.io.domain.api.DomainName.dcvDamperControlEnable
import a75f.io.domain.api.DomainName.dcvLoopOutput
import a75f.io.domain.api.DomainName.dehumidifierEnable
import a75f.io.domain.api.DomainName.dehumidifierOperationEnable
import a75f.io.domain.api.DomainName.dualSetpointControlEnable
import a75f.io.domain.api.DomainName.ductStaticPressureSetpoint
import a75f.io.domain.api.DomainName.equipStatusMessage
import a75f.io.domain.api.DomainName.fanLoopOutput
import a75f.io.domain.api.DomainName.humidifierEnable
import a75f.io.domain.api.DomainName.humidifierOperationEnable
import a75f.io.domain.api.DomainName.occupancyModeControl
import a75f.io.domain.api.DomainName.operatingMode
import a75f.io.domain.api.DomainName.satSetpointControlEnable
import a75f.io.domain.api.DomainName.staticPressureSetpointControlEnable
import a75f.io.domain.api.DomainName.supplyAirflowTemperatureSetpoint
import a75f.io.domain.api.DomainName.systemCO2DamperOpeningRate
import a75f.io.domain.api.DomainName.systemCO2Target
import a75f.io.domain.api.DomainName.systemCO2Threshold
import a75f.io.domain.api.DomainName.systemCoolingSATMaximum
import a75f.io.domain.api.DomainName.systemCoolingSATMinimum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMaximum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMinimum
import a75f.io.domain.api.DomainName.systemHeatingSATMaximum
import a75f.io.domain.api.DomainName.systemHeatingSATMinimum
import a75f.io.domain.api.DomainName.systemOccupancyMode
import a75f.io.domain.api.DomainName.systemSATMaximum
import a75f.io.domain.api.DomainName.systemSATMinimum
import a75f.io.domain.api.DomainName.systemStaticPressureMaximum
import a75f.io.domain.api.DomainName.systemStaticPressureMinimum
import a75f.io.domain.api.DomainName.systemtargetMaxInsideHumidty
import a75f.io.domain.api.DomainName.systemtargetMinInsideHumidty
import a75f.io.domain.api.Equip
import a75f.io.domain.api.Point
import a75f.io.domain.config.ExternalAhuConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.schedules.ScheduleUtil
import a75f.io.logic.bo.building.system.vav.VavExternalAhu
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.tuners.TunerUtil
import a75f.io.logic.util.RxjavaUtil
import android.content.Intent
import android.util.Log
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags
import java.util.Objects
import kotlin.math.roundToInt

/**
 * Created by Manjunath K on 27-10-2023.
 */

fun mapToSetPoint(min: Double, max: Double, current: Double): Double =
    ((max - min) * (current / 100.0) + min)


const val SINGLE_SAT_SET_POINT = "air and discharge and sp and temp"
const val HEATING_SAT_SET_POINT = "air and heating and sp and temp"
const val COOLING_SAT_SET_POINT = "air and cooling and sp and temp"
const val DUCT_STATIC_PRESSURE = "pressure and air and discharge and sp"
const val DAMPER_CMD = "cmd and outside and dcv and damper"
const val HUMIDIFIER_CMD = "cmd and enable and humidifier"
const val DEHUMIDIFIER_CMD = "cmd and dessicantDehumidifier"
const val OCCUPANCY_MODE = "mode and occupied and sp"
const val DISCHARGE_AIR_TEMP = "air and discharge and temp and sensor"
const val DUCT_STATIC_PRESSURE_SENSOR = "air and discharge and pressure and sensor"
const val OPERATING_MODE = "mode and operating and sp"

fun pushSatSetPoints(
    haystack: CCUHsApi,
    equipId: String,
    value: Double,
    setPointsList: ArrayList<String>
) {
    mapModbusPoint(haystack, SINGLE_SAT_SET_POINT, equipId, value, setPointsList)
}

fun pushSatCoolingSetPoints(
    haystack: CCUHsApi,
    equipId: String,
    value: Double,
    setPointsList: ArrayList<String>
) {
    mapModbusPoint(haystack, COOLING_SAT_SET_POINT, equipId, value, setPointsList)
}

fun pushSatHeatingSetPoints(
    haystack: CCUHsApi,
    equipId: String,
    value: Double,
    setPointsList: ArrayList<String>
) {
    mapModbusPoint(haystack, HEATING_SAT_SET_POINT, equipId, value, setPointsList)
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

fun pushOperatingMode(
    haystack: CCUHsApi,
    equipId: String,
    value: Double,
    setPointsList: ArrayList<String>
) {
    mapModbusPoint(haystack, OPERATING_MODE, equipId, value, setPointsList)
}


fun updateSetPoint(
    systemEquip: Equip, setPoint: Double, domainName: String,
    externalSpList: ArrayList<String>, externalEquipId: String?, haystack: CCUHsApi
) {

    updatePointValue(systemEquip, domainName, setPoint)
    externalEquipId?.let {
        pushSatSetPoints(haystack, externalEquipId, setPoint, externalSpList)
    }
}

fun updateCoolingSetPoint(
    systemEquip: Equip, setPoint: Double, domainName: String,
    externalSpList: ArrayList<String>, externalEquipId: String?, haystack: CCUHsApi
) {

    updatePointValue(systemEquip, domainName, setPoint)
    externalEquipId?.let {
        pushSatCoolingSetPoints(haystack, externalEquipId, setPoint, externalSpList)
    }
}

fun updateHeatingSetPoint(
    systemEquip: Equip, setPoint: Double, domainName: String,
    externalSpList: ArrayList<String>, externalEquipId: String?, haystack: CCUHsApi
) {

    updatePointValue(systemEquip, domainName, setPoint)
    externalEquipId?.let {
        pushSatHeatingSetPoints(haystack, externalEquipId, setPoint, externalSpList)
    }
}

fun updateOperatingMode(
    systemEquip: Equip, setPoint: Double, domainName: String,
    externalSpList: ArrayList<String>, externalEquipId: String?, haystack: CCUHsApi
) {

    updatePointValue(systemEquip, domainName, setPoint)
    externalEquipId?.let {
        pushOperatingMode(haystack, externalEquipId, setPoint, externalSpList)
    }
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
        if (!setPointsList.contains(pointId))
            setPointsList.add(pointId)
        val currentHisValue = haystack.readHisValById(pointId)
        val currentDefaultValue = haystack.readDefaultValById(pointId)
        if (currentHisValue != value)
            haystack.writeHisValById(pointId, value)
        if (currentDefaultValue != value)
            haystack.writeDefaultValById(pointId, value)
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
                when (lastLoopDirection) {
                    TempDirection.COOLING -> Domain.getPointByDomain(systemEquip, systemSATMaximum)
                    else -> Domain.getPointByDomain(systemEquip, systemSATMinimum)
                }
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
            if (!isDualSetPointEnabled) {
                when (lastLoopDirection) {
                    TempDirection.COOLING -> Domain.getPointByDomain(systemEquip, systemSATMaximum)
                    else -> Domain.getPointByDomain(systemEquip, systemSATMinimum)
                }
            }
            else
                Domain.getPointByDomain(systemEquip, systemHeatingSATMinimum)
        }

        SystemMode.COOLONLY -> {
            if (!isDualSetPointEnabled) {
                when (lastLoopDirection) {
                    TempDirection.COOLING -> Domain.getPointByDomain(systemEquip, systemSATMaximum)
                    else -> Domain.getPointByDomain(systemEquip, systemSATMinimum)
                }
            }
            else
                Domain.getPointByDomain(systemEquip, systemCoolingSATMaximum)
        }
    }
}

fun getTunerByDomainName(systemEquip: Equip, domainName: String): Double =
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
) {
    val isSetPointEnabled = isConfigEnabled(systemEquip, satSetpointControlEnable)
    if (!isSetPointEnabled) {
        logIt("satSetpointControl disabled")
        return
    }
    logIt("Cooling lockout ${L.ccu().systemProfile.isCoolingLockoutActive} heating lockout ${L.ccu().systemProfile.isHeatingLockoutActive}")
    val isDualSetPointEnabled = isConfigEnabled(systemEquip, dualSetpointControlEnable)
    if (isDualSetPointEnabled) {
        val satSetPointLimits = getDualSetPointMinMax(systemEquip)
        val coolingSatSetPointValue = if (basicConfig.coolingLoop.toDouble() == 0.0 || L.ccu().systemProfile.isCoolingLockoutActive)
            updateDefaultSetPoints(conditioningMode, systemEquip, TempDirection.COOLING)
        else
            mapToSetPoint(
                satSetPointLimits.first.first,
                satSetPointLimits.first.second,
                basicConfig.coolingLoop.toDouble()
            )

        val heatingSatSetPointValue = if (basicConfig.heatingLoop.toDouble() == 0.0 || L.ccu().systemProfile.isHeatingLockoutActive)
            updateDefaultSetPoints(conditioningMode, systemEquip, TempDirection.HEATING)
        else
            mapToSetPoint(
                satSetPointLimits.second.first,
                satSetPointLimits.second.second,
                basicConfig.heatingLoop.toDouble()
            )
        updateCoolingSetPoint(
            systemEquip,
            coolingSatSetPointValue,
            airTempCoolingSp,
            externalSpList,
            externalEquipId,
            haystack
        )
        updateHeatingSetPoint(
            systemEquip,
            heatingSatSetPointValue,
            airTempHeatingSp,
            externalSpList,
            externalEquipId,
            haystack
        )
        logIt(
            "Dual SP  cooling MinMax (${satSetPointLimits.first.first}, ${satSetPointLimits.first.second})" +
                    "heating MinMax (${satSetPointLimits.second.first}, ${satSetPointLimits.second.second})" +
                    " coolingSatSetPointValue $coolingSatSetPointValue heatingSatSetPointValue $heatingSatSetPointValue"
        )
    } else {
        val satSetPointLimits = getSingleSetPointMinMax(systemEquip, loopRunningDirection)
        var isLockoutActive = false
        if (loopRunningDirection == TempDirection.COOLING)
            isLockoutActive = L.ccu().systemProfile.isCoolingLockoutActive
        if (loopRunningDirection == TempDirection.HEATING)
            isLockoutActive = L.ccu().systemProfile.isHeatingLockoutActive
        logIt( "isLockoutActive:$isLockoutActive ")
        val satSetPointValue: Double = if (basicConfig.loopOutput == 0.0 || isLockoutActive)
            updateDefaultSetPoints(conditioningMode, systemEquip, loopRunningDirection)
        else
            mapToSetPoint(satSetPointLimits.first, satSetPointLimits.second, basicConfig.loopOutput)
        updateSetPoint(
            systemEquip,
            satSetPointValue,
            supplyAirflowTemperatureSetpoint,
            externalSpList,
            externalEquipId,
            haystack
        )
        logIt(
            "Single SP Direction $loopRunningDirection min (${satSetPointLimits.first}, ${satSetPointLimits.second})" +
                    " setpoint $satSetPointValue"
        )
    }

}


fun calculateDSPSetPoints(
    systemEquip: Equip,
    loopOutput: Double,
    externalEquipId: String?,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
    basicConfig: BasicConfig,
    analogFanMultiplier: Double,
    coolingLoop: Double,
    conditioningMode: SystemMode
) {
    if (!isConfigEnabled(systemEquip, staticPressureSetpointControlEnable)) {
        logIt("StaticPressureSp is disabled")
        return
    }

    val tempDirection = getTempDirection(basicConfig.heatingLoop)
    var fanLoop = loopOutput * analogFanMultiplier.coerceAtMost(100.0)
    logIt("System Fan loop $fanLoop")
    if (isFanLoopUpdateRequired(tempDirection, conditioningMode)) {
        fanLoop = checkOaoLoop(coolingLoop)
        logIt("After OAO economization  Fan loop $fanLoop")
    }

    val min = Domain.getPointByDomain(systemEquip, systemStaticPressureMinimum)
    val max = Domain.getPointByDomain(systemEquip, systemStaticPressureMaximum)
    var dspSetPoint: Double = mapToSetPoint(min, max, fanLoop).coerceAtMost(max)
    dspSetPoint = (dspSetPoint * 100.0).roundToInt() / 100.0
    updatePointValue(systemEquip, ductStaticPressureSetpoint, dspSetPoint)
    updatePointValue(systemEquip, fanLoopOutput, fanLoop)
    externalEquipId?.let {
        pushDuctStaticPressure(haystack, externalEquipId, dspSetPoint, externalSpList)
    }
    logIt("DSP Min: $min DSP Max: $max analogFanMultiplier: $analogFanMultiplier ductStaticPressureSetPoint: $dspSetPoint")
}

fun isFanLoopUpdateRequired(tempDirection: TempDirection, conditioningMode: SystemMode): Boolean {
    return ( tempDirection == TempDirection.COOLING
            && (conditioningMode == SystemMode.COOLONLY || conditioningMode == SystemMode.AUTO)
            && (L.ccu().oaoProfile != null)
            && (L.ccu().oaoProfile.isEconomizingAvailable)
            )
}

fun checkOaoLoop(systemCoolingLoopOp: Double): Double {

    val economizingToMainCoolingLoopMap = TunerUtil.readTunerValByQuery(
        "oao and economizing and main and cooling and loop and map",
        L.ccu().oaoProfile.equipRef
    )
    logIt("OAO profile is available isEconomizingAvailable ? = ${L.ccu().oaoProfile.isEconomizingAvailable}")

    val updatedFanLoop =
        (systemCoolingLoopOp * 100 / economizingToMainCoolingLoopMap).coerceIn(0.0, 100.0)
    logIt("economizingToMainCoolingLoopMap = $economizingToMainCoolingLoopMap updatedFanLoop = $updatedFanLoop ")
    return updatedFanLoop
}

fun handleHumidityOperation(
    systemEquip: Equip,
    externalEquipId: String?,
    occupancyMode: Occupancy,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
    humidityHysteresis: Double,
    currentHumidity: Double,
    conditioningMode: SystemMode
) {
    val currentStatus = Domain.getHisByDomain(systemEquip, humidifierEnable)
    var newStatus = 0.0

    if ((occupancyMode == Occupancy.UNOCCUPIED || occupancyMode == Occupancy.PRECONDITIONING ||
                occupancyMode == Occupancy.VACATION) || conditioningMode == SystemMode.OFF) {
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

fun updateSystemStatusPoints(equipRef: String, newValue: String, domainName: String) {
    val currentStatus = Domain.readStrPointValueByDomainName(domainName, equipRef)
    if (!currentStatus.contentEquals(newValue)) {
        Domain.writeDefaultValByDomain(domainName, newValue, equipRef)
        if (domainName.contentEquals(equipStatusMessage))
            Globals.getInstance().applicationContext.sendBroadcast(Intent(ScheduleUtil.ACTION_STATUS_CHANGE))
    }

}

fun handleDeHumidityOperation(
    systemEquip: Equip,
    externalEquipId: String?,
    occupancyMode: Occupancy,
    haystack: CCUHsApi,
    externalSpList: ArrayList<String>,
    humidityHysteresis: Double,
    currentHumidity: Double,
    conditioningMode: SystemMode
) {
    val currentStatus = Domain.getHisByDomain(systemEquip, dehumidifierEnable)
    var newStatus = 0.0

    if (occupancyMode == Occupancy.UNOCCUPIED || occupancyMode == Occupancy.PRECONDITIONING
        || occupancyMode == Occupancy.VACATION || conditioningMode == SystemMode.OFF
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
    conditioningMode: SystemMode
) {
    val isDcvControlEnabled = isConfigEnabled(systemEquip, dcvDamperControlEnable)
    Domain.writeHisValByDomain(co2WeightedAverage, co2, systemEquip.id)
    if (!isDcvControlEnabled) {
        logIt("DCV control is disabled")
        return
    }

    val dcvMin = Domain.getPointByDomain(systemEquip, systemDCVDamperPosMinimum)
    val dcvMax = Domain.getPointByDomain(systemEquip, systemDCVDamperPosMaximum)
    val damperOpeningRate = Domain.getPointByDomain(systemEquip, systemCO2DamperOpeningRate)
    val co2Threshold = Domain.getPointByDomain(systemEquip, systemCO2Threshold)

    var damperOperationPercent = 0.0
    if (conditioningMode == SystemMode.OFF)
        damperOperationPercent = 0.0
    else if (shouldOperateDamper(co2, occupancyMode, co2Threshold))
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
        Occupancy.UNOCCUPIED, Occupancy.VACATION, Occupancy.PRECONDITIONING -> 0.0
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
): Double {
    val damperSp = (sensorCO2 - threshold) / openingRate
    return (damperSp * 100.0).roundToInt() / 100.0
}


fun getSingleSetPointMinMax(equip: Equip, tempDirection: TempDirection): Pair<Double, Double> {
    return when (tempDirection) {
        TempDirection.COOLING -> {
            Pair(
                Domain.getPointByDomain(equip, systemSATMaximum),
                Domain.getPointByDomain(equip, systemSATMinimum)
            )
        }
        TempDirection.HEATING -> {
            Pair(
                Domain.getPointByDomain(equip, systemSATMinimum),
                Domain.getPointByDomain(equip, systemSATMaximum)
            )
        }
    }
}

fun getDualSetPointMinMax(
    equip: Equip
): Pair<Pair<Double, Double>, Pair<Double, Double>> {
    return Pair(
        Pair(
            Domain.getPointByDomain(equip, systemCoolingSATMaximum),
            Domain.getPointByDomain(equip, systemCoolingSATMinimum)
        ),
        Pair(
            Domain.getPointByDomain(equip, systemHeatingSATMinimum),
            Domain.getPointByDomain(equip, systemHeatingSATMaximum)
        )
    )
}

fun getConditioningMode(systemEquip: Equip) =
    SystemMode.values()[Domain.getPointByDomain(systemEquip, conditioningMode).toInt()]

fun updatePointValue(equip: Equip, domainName: String, pointValue: Double) =
    Domain.writePointByDomain(equip, domainName, pointValue)

fun updatePointHistoryAndDefaultValue(domainName: String, value: Double) {
    Domain.writeDefaultValByDomain(domainName, value)
    Domain.writeHisValByDomain(domainName, value)
}

fun getConfiguration(profileDomain: String, profileType: ProfileType): ExternalAhuConfiguration {
    val systemEquip = Domain.getSystemEquipByDomainName(profileDomain)
    val config = ExternalAhuConfiguration(profileType.name)

    if (systemEquip == null)
        return config

    config.setPointControl.enabled =
        getConfigByDomainName(systemEquip, satSetpointControlEnable)
    config.dualSetPointControl.enabled =
        getConfigByDomainName(systemEquip, dualSetpointControlEnable)
    config.fanStaticSetPointControl.enabled =
        getConfigByDomainName(systemEquip, staticPressureSetpointControlEnable)
    config.dcvControl.enabled = getConfigByDomainName(systemEquip, dcvDamperControlEnable)
    config.occupancyMode.enabled = getConfigByDomainName(systemEquip, occupancyModeControl)
    config.humidifierControl.enabled =
        getConfigByDomainName(systemEquip, humidifierOperationEnable)
    config.dehumidifierControl.enabled =
        getConfigByDomainName(systemEquip, dehumidifierOperationEnable)

    config.satMin.currentVal = getConfigValue(systemSATMinimum, systemEquip)
    config.satMax.currentVal = getConfigValue(systemSATMaximum, systemEquip)
    config.heatingMinSp.currentVal =
        getConfigValue(systemHeatingSATMinimum, systemEquip)
    config.heatingMaxSp.currentVal =
        getConfigValue(systemHeatingSATMaximum, systemEquip)
    config.coolingMinSp.currentVal =
        getConfigValue(systemCoolingSATMinimum, systemEquip)
    config.coolingMaxSp.currentVal =
        getConfigValue(systemCoolingSATMaximum, systemEquip)
    config.fanMinSp.currentVal =
        getConfigValue(systemStaticPressureMinimum, systemEquip)
    config.fanMaxSp.currentVal =
        getConfigValue(systemStaticPressureMaximum, systemEquip)
    config.dcvMin.currentVal = getConfigValue(systemDCVDamperPosMinimum, systemEquip)
    config.dcvMax.currentVal = getConfigValue(systemDCVDamperPosMaximum, systemEquip)
    config.co2Threshold.currentVal = getConfigValue(systemCO2Threshold, systemEquip)
    config.damperOpeningRate.currentVal = getConfigValue(
        systemCO2DamperOpeningRate,
        systemEquip
    )
    config.co2Target.currentVal = getConfigValue(systemCO2Target, systemEquip)
    return config
}

fun getExternalEquipId(): String? {
    // TODO check if bacnet is configured then we need to find bacnet equip id
    val modbusEquip =
        CCUHsApi.getInstance().readEntity("system and equip and modbus and not emr and not btu")
    if (modbusEquip.isNotEmpty()) {
        return modbusEquip["id"].toString()
    }
    return null
}

fun addSystemEquip(
    config: ProfileConfiguration?,
    definition: SeventyFiveFProfileDirective?,
    systemProfile: SystemProfile
) {
    val profileEquipBuilder = ProfileEquipBuilder(CCUHsApi.getInstance())
    val equipId = profileEquipBuilder.buildEquipAndPoints(
        config!!, definition!!,
        CCUHsApi.getInstance().site!!.id
    )
    systemProfile.updateAhuRef(equipId)
    ControlMote(equipId)
    if (systemProfile is VavExternalAhu) {
        systemProfile.initTRSystem()
    }
}

fun getModbusPointValue(query: String): String {
    val equipId = getExternalEquipId()
    val point = CCUHsApi.getInstance().readEntity("$query and equipRef == \"$equipId\"")

    if (point.isEmpty()) {
        logIt("$query = point not found $equipId")
        return ""
    }

    val pointId = point["id"].toString()
    val value = CCUHsApi.getInstance().readHisValById(pointId)
    val unit = point["unit"]

    return " $value $unit"
}

fun getConfigValue(domainName: String, modelName: String): Boolean {
    val systemEquip = Domain.getSystemEquipByDomainName(modelName)
    return getConfigByDomainName(systemEquip!!, domainName)
}

fun getSetPoint(domainName: String): String {
    val point = Domain.readPoint(domainName)
    if (point.isEmpty()) return ""
    val unit = Objects.requireNonNull(point["unit"]).toString()
    val value = CCUHsApi.getInstance().readHisValById(point["id"].toString())
    return (" $value $unit")
}

fun getOperatingMode(equipName: String): String {
    val systemEquip = Domain.getSystemEquipByDomainName(equipName)
    val mode = getDefaultValueByDomain(systemEquip!!, operatingMode).toInt()
    return when (SystemController.State.values()[mode]) {
        SystemController.State.COOLING -> " Cooling"
        SystemController.State.HEATING -> " Heating"
        else -> {
            " Off"
        }
    }


}

fun getConfigValue(domainName: String, equip: Equip): Double =
    getDefaultValueByDomain(equip, domainName)

fun getConfigByDomainName(equip: Equip, domainName: String): Boolean {
    val config = getPointByDomain(equip, domainName)
    config?.let { return config.readDefaultVal() == 1.0 }
    return false
}

fun getDefaultValueByDomain(equip: Equip, domainName: String): Double {
    val config = getPointByDomain(equip, domainName)
    config?.let { return config.readDefaultVal() }
    return 0.0
}

fun getPointByDomain(equip: Equip, domainName: String): Point? =
    equip.points.entries.find { (it.value.domainName.contentEquals(domainName)) }?.value

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

