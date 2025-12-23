package a75f.io.logic.bo.building.statprofiles.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.StandAloneEquip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.hyperstatsplit.HyperStatSplitEquip
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.util.CCUUtils
import android.util.Log
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

fun canWeDoConditioning(basicSettings: BasicSettings): Boolean {
    return (basicSettings.conditioningMode != StandaloneConditioningMode.OFF)
}
fun canWeDoConditioning(mode: StandaloneConditioningMode): Boolean {
    return (mode != StandaloneConditioningMode.OFF)
}

fun canWeDoConditioning(basicSettings: MyStatBasicSettings): Boolean {
    return (basicSettings.conditioningMode != StandaloneConditioningMode.OFF)
}

fun canWeRunFan(basicSettings: BasicSettings): Boolean {
    return (basicSettings.fanMode != StandaloneFanStage.OFF)
}
fun canWeRunFan(basicSettings: MyStatBasicSettings): Boolean {
    return (basicSettings.fanMode != MyStatFanStages.OFF)
}

fun isSupplyOppositeToConditioning(
    conditioningMode: StandaloneConditioningMode,
    supplyWaterTemp: Double, heatingThreshold: Double, coolingThreshold: Double
): Boolean {
    return when (conditioningMode) {
        StandaloneConditioningMode.OFF, StandaloneConditioningMode.AUTO -> false
        StandaloneConditioningMode.HEAT_ONLY -> (supplyWaterTemp < coolingThreshold)
        StandaloneConditioningMode.COOL_ONLY -> (supplyWaterTemp > heatingThreshold)
    }
}
fun canWeOperate(basicSettings: BasicSettings) = canWeDoConditioning(basicSettings)

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

fun runLowestFanSpeedDuringDoorOpen(equip: StandAloneEquip, tag: String) {
    mutableMapOf(
        equip.fanLowSpeedVentilation to Stage.FAN_1.displayName,
        equip.fanLowSpeed to Stage.FAN_1.displayName,
        equip.fanMediumSpeed to Stage.FAN_2.displayName,
        equip.fanHighSpeed to Stage.FAN_3.displayName,
        equip.fanEnable to StatusMsgKeys.FAN_ENABLED.name,
        equip.occupiedEnable to StatusMsgKeys.EQUIP_ON.name
    ).forEach {
        if (it.key.pointExists()) {
            it.key.writeHisVal(1.0)
            equip.relayStages[it.value] = 1
            Log.i(tag, "Lowest Fan Speed is running due to Title 24 Door Open open state: ${it.key.domainName}")
            return
        }
    }
}

fun doorWindowIsOpen(equip: StandAloneEquip): Pair<Boolean, Boolean> {

    var doorWindowEnabled = 0.0
    var doorWindowSensor = 0.0
    var isDoorOpenFromTitle24 = false
    listOf(
        equip.doorWindowSensorNCTitle24,
        equip.doorWindowSensorTitle24,
        equip.doorWindowSensorNOTitle24,
        equip.doorWindowSensorNC,
        equip.doorWindowSensor,
        equip.doorWindowSensorNO,
    ).forEach {
        if (it.pointExists()) {
            if (doorWindowEnabled != 1.0) {
                doorWindowEnabled = 1.0
            }
            if (it.readHisVal() > 0) {
                doorWindowSensor = 1.0
                if (it == equip.doorWindowSensorNCTitle24 ||
                    it == equip.doorWindowSensorTitle24 ||
                    it == equip.doorWindowSensorNOTitle24
                ) {
                    isDoorOpenFromTitle24 = true
                }
            }
        }
    }
    equip.doorWindowSensingEnable.writePointValue(doorWindowEnabled)
    equip.doorWindowSensorInput.writePointValue(doorWindowSensor)
    return Pair(doorWindowSensor > 0, isDoorOpenFromTitle24)
}

fun keyCardIsInSlot(
    equip: StandAloneEquip
) {
    equip.apply {
        val enabled = (keyCardSensorNC.pointExists() ||
                keyCardSensorNO.pointExists() ||
                keyCardSensor.pointExists())

        val active = (keyCardSensorNC.readHisVal() > 0 ||
                keyCardSensorNO.readHisVal() > 0 ||
                keyCardSensor.readHisVal() > 0)
        keyCardSensingEnable.writePointValue(if (enabled) 1.0 else 0.0)
        keyCardSensorInput.writePointValue(if (active) 1.0 else 0.0)
    }
}

fun isFanGoodRun(
    isDoorWindowOpen: Boolean, equip: StandAloneEquip,
    heatingLoopOutput: Int, coolingLoopOutput: Int, lowVentilationExist: CalibratedPoint
): Boolean {
    return if (lowVentilationExist.data > 0) true
    else if (isDoorWindowOpen || heatingLoopOutput > 0) {
        equip.waterValve.pointExists() || equip.modulatingWaterValve.pointExists() || equip.auxHeatingStage1.pointExists() || equip.auxHeatingStage2.pointExists()
    } else if (isDoorWindowOpen || coolingLoopOutput > 0) {
        equip.waterValve.pointExists() || equip.modulatingWaterValve.pointExists()
    } else {
        false
    }
}
fun getHyperStatDevice(nodeAddress: Int): HashMap<Any, Any> {
    return CCUHsApi.getInstance().readEntity("domainName == \"${DomainName.hyperstatDevice}\" and addr == \"$nodeAddress\"")
}

fun fetchUserIntents(equip: StandAloneEquip): UserIntents {
    val haystack = CCUHsApi.getInstance()
    val coolingDesiredTemp: Double
    val heatingDesiredTemp: Double
    val desiredTemp: Double

    val isScheduleSlotsAvailable = haystack.isScheduleSlotExitsForRoom(equip.equipRef)
    if (isScheduleSlotsAvailable) {
        val unoccupiedSetback = haystack.getUnoccupiedSetback(equip.equipRef)
        coolingDesiredTemp = CCUUtils.DEFAULT_COOLING_DESIRED.toDouble() + unoccupiedSetback
        heatingDesiredTemp = CCUUtils.DEFAULT_HEATING_DESIRED.toDouble() - unoccupiedSetback
        desiredTemp = (coolingDesiredTemp + heatingDesiredTemp) / 2.0
        CcuLog.d(
            L.TAG_CCU_HSSPLIT_CPUECON,
            "Schedule Slots found for ${equip.equipRef}, using schedule setpoints with setback $unoccupiedSetback"
        )
        equip.desiredTempCooling.writePointValue(coolingDesiredTemp)
        equip.desiredTempHeating.writePointValue(heatingDesiredTemp)
        equip.desiredTemp.writePointValue(desiredTemp)

        CcuLog.d(
            L.TAG_CCU_HSSPLIT_CPUECON,
            "No Schedule Slots found for ${equip.equipRef}, using default setpoints with setback $unoccupiedSetback"
        )
    } else {
        coolingDesiredTemp = equip.desiredTempCooling.readPriorityVal()
        heatingDesiredTemp = equip.desiredTempHeating.readPriorityVal()
    }

    return when (equip) {
        is HyperStatEquip -> {
            UserIntents(
                currentTemp = equip.currentTemp.readHisVal(),
                coolingDesiredTemp = coolingDesiredTemp,
                heatingDesiredTemp = heatingDesiredTemp,
                targetMinHumidity = equip.targetHumidifier.readPriorityVal(),
                targetMaxHumidity = equip.targetDehumidifier.readPriorityVal()
            )
        }

        is HyperStatSplitEquip -> {
            UserIntents(
                currentTemp = equip.currentTemp.readHisVal(),
                coolingDesiredTemp = coolingDesiredTemp,
                heatingDesiredTemp = heatingDesiredTemp,
                targetMinHumidity = equip.targetHumidifier.readPriorityVal(),
                targetMaxHumidity = equip.targetDehumidifier.readPriorityVal()
            )
        }

        is MyStatEquip -> {
            UserIntents(
                currentTemp = equip.currentTemp.readHisVal(),
                coolingDesiredTemp = coolingDesiredTemp,
                heatingDesiredTemp = heatingDesiredTemp,
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

fun updateConditioningMode(equip: StandAloneEquip, isCoolingAvailable: Boolean, isHeatingAvailable: Boolean) {
    equip.apply {
        if (!isCoolingAvailable && !isHeatingAvailable) {
            conditioningMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
        } else if (!isCoolingAvailable) {
            conditioningMode.writePointValue(StandaloneConditioningMode.HEAT_ONLY.ordinal.toDouble())
        } else if (!isHeatingAvailable) {
            conditioningMode.writePointValue(StandaloneConditioningMode.COOL_ONLY.ordinal.toDouble())
        }
    }
}

enum class MyStatDeviceType {
    MYSTAT_V1,
    MYSTAT_V2
}

data class StagesCounts(
    var coolingStages: CalibratedPoint = CalibratedPoint("coolingStages", "", 0.0),
    var heatingStages: CalibratedPoint = CalibratedPoint("heatingStages", "", 0.0),
    var fanStages: CalibratedPoint = CalibratedPoint("fanStages", "", 0.0),
    var compressorStages: CalibratedPoint = CalibratedPoint("compressorStages", "", 0.0),
)
