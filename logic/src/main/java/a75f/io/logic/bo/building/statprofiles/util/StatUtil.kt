package a75f.io.logic.bo.building.statprofiles.util

import a75f.io.domain.HyperStatSplitEquip
import a75f.io.domain.api.Point
import a75f.io.domain.equips.DomainEquip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.util.CCUUtils
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Created by Manjunath K on 28-04-2025.
 */

fun getPercentFromVolt(voltage: Int, min: Int = 0, max: Int = 10) =
    (((voltage - min).toDouble() / (max - min)) * 100).roundToInt()

fun milliToMin(milliseconds: Long) = (milliseconds / (1000 * 60) % 60)

fun updateLoopOutputs(
    coolingLoop: Int, coolingPoint: Point,
    heatingLoop: Int, heatingPoint: Point,
    fanLoop: Int, fanPoint: Point,
    dcvLoop: Int, dcvLoopPoint: Point,
    isHpuProfile: Boolean = false,
    compressorLoop: Int = 0, compressorLoopPoint: Point? = null,
) {
    coolingPoint.writePointValue(coolingLoop.toDouble())
    heatingPoint.writePointValue(heatingLoop.toDouble())
    fanPoint.writePointValue(fanLoop.toDouble())
    dcvLoopPoint.writePointValue(dcvLoop.toDouble())
    if (isHpuProfile) {
        compressorLoopPoint?.writePointValue(compressorLoop.toDouble())
    }
}

fun updateOperatingMode(
    currentTemp: Double,
    averageDesiredTemp: Double,
    conditioningMode: StandaloneConditioningMode,
    operatingMode: Point
) {
    val zoneOperatingMode = when {
        currentTemp < averageDesiredTemp && conditioningMode != StandaloneConditioningMode.COOL_ONLY -> ZoneState.HEATING.ordinal
        currentTemp >= averageDesiredTemp && conditioningMode != StandaloneConditioningMode.HEAT_ONLY -> ZoneState.COOLING.ordinal
        else -> ZoneState.DEADBAND.ordinal
    }
    operatingMode.writeHisVal(zoneOperatingMode.toDouble())
}


fun canWeDoCooling(conditioningMode: StandaloneConditioningMode): Boolean {
    return (conditioningMode == StandaloneConditioningMode.COOL_ONLY || conditioningMode == StandaloneConditioningMode.AUTO)
}

fun canWeDoHeating(conditioningMode: StandaloneConditioningMode): Boolean {
    return (conditioningMode == StandaloneConditioningMode.HEAT_ONLY || conditioningMode == StandaloneConditioningMode.AUTO)
}

fun canWeRunFan(basicSettings: BasicSettings): Boolean {
    return (basicSettings.fanMode != StandaloneFanStage.OFF && basicSettings.conditioningMode != StandaloneConditioningMode.OFF)
}

fun isUserIntentFanMode(fanOpMode: Point): Boolean {
    val fanMode = fanOpMode.readPriorityVal().toInt()
    return (fanMode != StandaloneFanStage.OFF.ordinal && fanMode != StandaloneFanStage.AUTO.ordinal)
}

fun isLowUserIntentFanMode(fanOpMode: Point): Boolean {
    val fanMode = fanOpMode.readPriorityVal().toInt()
    return fanMode == StandaloneFanStage.LOW_CUR_OCC.ordinal
            || fanMode == StandaloneFanStage.LOW_OCC.ordinal
            || fanMode == StandaloneFanStage.LOW_ALL_TIME.ordinal
}

fun isMediumUserIntentFanMode(fanOpMode: Point): Boolean {
    val fanMode = fanOpMode.readPriorityVal().toInt()
    return fanMode == StandaloneFanStage.MEDIUM_CUR_OCC.ordinal
            || fanMode == StandaloneFanStage.MEDIUM_OCC.ordinal
            || fanMode == StandaloneFanStage.MEDIUM_ALL_TIME.ordinal
}

fun isHighUserIntentFanMode(fanOpMode: Point): Boolean {
    val fanMode = fanOpMode.readPriorityVal().toInt()
    return fanMode == StandaloneFanStage.HIGH_CUR_OCC.ordinal
            || fanMode == StandaloneFanStage.HIGH_OCC.ordinal
            || fanMode == StandaloneFanStage.HIGH_ALL_TIME.ordinal
}


fun getAirEnthalpy(temp: Double, humidity: Double): Double {
    val a = 0.007468 * temp.pow(2.0) - 0.4344 * temp + 11.176
    val b = 0.2372 * temp + 0.1230
    val h = a * 0.01 * humidity + b
    CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "temperature $temp humidity $humidity Enthalpy: $h")
    return CCUUtils.roundToTwoDecimal(h)
}

fun isDcvEligibleToOff(currentOccupancy: CalibratedPoint): Boolean {
    return (currentOccupancy.data.toInt() != Occupancy.OCCUPIED.ordinal
            && currentOccupancy.data.toInt() != Occupancy.AUTOFORCEOCCUPIED.ordinal
            && currentOccupancy.data.toInt() != Occupancy.DEMAND_RESPONSE_OCCUPIED.ordinal
            && currentOccupancy.data.toInt() != Occupancy.FORCEDOCCUPIED.ordinal)
}

fun isDcvEligibleToOn(currentOccupancy: CalibratedPoint): Boolean {
    return (currentOccupancy.data.toInt() == Occupancy.OCCUPIED.ordinal
            || currentOccupancy.data.toInt() == Occupancy.AUTOFORCEOCCUPIED.ordinal
            || currentOccupancy.data.toInt() == Occupancy.DEMAND_RESPONSE_OCCUPIED.ordinal
            || currentOccupancy.data.toInt() == Occupancy.FORCEDOCCUPIED.ordinal)
}

fun fetchUserIntents(equip: DomainEquip): UserIntents {

    return when (equip) {
        is HyperStatEquip -> {
            UserIntents(
                currentTemp = equip.currentTemp.readHisVal(),
                coolingDesiredTemp = equip.desiredTempCooling.readPriorityVal(),
                heatingDesiredTemp = equip.desiredTempHeating.readPriorityVal(),
                targetMinHumidity = equip.targetHumidifier.readPriorityVal(),
                targetMaxHumidity = equip.targetDehumidifier.readPriorityVal()
            )
        }

        is HyperStatSplitEquip -> {
            UserIntents(
                currentTemp = equip.currentTemp.readHisVal(),
                coolingDesiredTemp = equip.desiredTempCooling.readPriorityVal(),
                heatingDesiredTemp = equip.desiredTempHeating.readPriorityVal(),
                targetMinHumidity = equip.targetHumidifier.readPriorityVal(),
                targetMaxHumidity = equip.targetDehumidifier.readPriorityVal()
            )
        }

        is MyStatEquip -> {
            UserIntents(
                currentTemp = equip.currentTemp.readHisVal(),
                coolingDesiredTemp = equip.desiredTempCooling.readPriorityVal(),
                heatingDesiredTemp = equip.desiredTempHeating.readPriorityVal(),
                targetMinHumidity = equip.targetHumidifier.readPriorityVal(),
                targetMaxHumidity = equip.targetDehumidifier.readPriorityVal()
            )
        }

        else -> {
            UserIntents(
                currentTemp = 0.0,
                coolingDesiredTemp = 0.0,
                heatingDesiredTemp = 0.0,
                targetMinHumidity = 0.0,
                targetMaxHumidity = 0.0
            )
        }
    }
}

fun getInCalibratedPointPoint(data: Int): CalibratedPoint {
    return CalibratedPoint(
        "InCalibratedPoint", // dummy name
        "", data.toDouble()
    )
}