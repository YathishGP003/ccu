package a75f.io.logic.util.uiutils

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.domain.api.Point
import a75f.io.domain.api.readPoint
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.EpidemicState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.util.ExecutorTask

/**
 * Created by Manjunath K on 24-04-2025.
 */

/**
 * Please run respective test cases in UserIntentStatusUtilKtTest to verify the changes.
 */
fun getTempStateMessage(temperatureState: ZoneTempState): String {
    return when (temperatureState) {
        ZoneTempState.RF_DEAD -> "RF Signal dead "
        ZoneTempState.TEMP_DEAD -> "Zone Temp Dead "
        ZoneTempState.EMERGENCY -> "Emergency "
        ZoneTempState.NONE -> ""
        ZoneTempState.FAN_OP_MODE_OFF -> "OFF "
    }
}


/**
 * Please run respective test cases in UserIntentStatusUtilKtTest to verify the changes.
 */
fun getStagesStatus(
    portStages: HashMap<String, Int>, prefix: String, stages: List<Stage>
): String? {
    val activeStages = stages.filter { portStages.containsKey(it.displayName) }
    return when (activeStages.size) {
        3 -> "$prefix 1,2&3 ON"
        2 -> "$prefix ${activeStages[0].displayName.last()}&${activeStages[1].displayName.last()} ON"
        1 -> "$prefix ${activeStages[0].displayName.last()} ON"
        else -> null
    }
}

fun getFanStatus(temperatureState: ZoneTempState, portStages: HashMap<String, Int>): String? {
    if (temperatureState == ZoneTempState.FAN_OP_MODE_OFF || temperatureState == ZoneTempState.TEMP_DEAD) return null
    val fanStages = listOf(Stage.FAN_1, Stage.FAN_2, Stage.FAN_3)
    return getStagesStatus(portStages, "Fan", fanStages)
}

fun getAuxHeatStatus(portStages: Map<String, Int>): String? {
    val stage1Keys = setOf("Aux Heating Stage 1", "AUX_HEATING_STAGE1")
    val stage2Keys = setOf("Aux Heating Stage 2", "AUX_HEATING_STAGE2")

    val hasStage1 = portStages.keys.any { it in stage1Keys }
    val hasStage2 = portStages.keys.any { it in stage2Keys }

    return when {
        hasStage1 && hasStage2 -> "Aux Heating 1&2 ON"
        hasStage1 -> "Aux Heating 1 ON"
        hasStage2 -> "Aux Heating 2 ON"
        else -> null
    }
}

fun getValveStatus(portStages: Map<String, Int>, analogOutStages: Map<String, Int>): String? {
    val analogs = mutableListOf<String>()
    if (StatusMsgKeys.COOLING_VALVE.name in portStages || StatusMsgKeys.COOLING_VALVE.name in analogOutStages) {
        analogs.add("Cooling Valve ON")
    }
    if (StatusMsgKeys.HEATING_VALVE.name in portStages || StatusMsgKeys.HEATING_VALVE.name in analogOutStages) {
        analogs.add("Heating Valve ON")
    }
    return analogs.joinToString(" | ").takeIf { analogs.isNotEmpty() }
}

fun getAnalogStatus(analogOutStages: HashMap<String, Int>): String? {
    val analogs = mutableListOf<String>()
    if (analogOutStages.containsKey(StatusMsgKeys.COOLING.name)) analogs.add("Cooling Analog ON")
    if (analogOutStages.containsKey(StatusMsgKeys.HEATING.name)) analogs.add("Heating Analog ON")
    if (analogOutStages.containsKey(StatusMsgKeys.FAN_SPEED.name)) analogs.add("Fan Analog ON")
    return analogs.joinToString(" | ").takeIf { it.isNotEmpty() }
}


/**
 * Please run respective test cases in UserIntentStatusUtilKtTest to verify the changes.
 */
fun getStatusMsg(
    portStages: HashMap<String, Int>,
    analogOutStages: HashMap<String, Int>,
    temperatureState: ZoneTempState,
    epidemicState: EpidemicState = EpidemicState.OFF
): String {
    val statusParts = linkedSetOf<String>()

    val tempPrefix = when (temperatureState) {
        ZoneTempState.RF_DEAD -> "RF Signal dead"
        ZoneTempState.TEMP_DEAD -> "Zone Temp Dead"
        ZoneTempState.EMERGENCY -> "Emergency"
        ZoneTempState.FAN_OP_MODE_OFF -> "OFF"
        else -> null
    }
    tempPrefix?.let { statusParts.add(it) }


    if (epidemicState == EpidemicState.PREPURGE) {
        statusParts.add("In Pre Purge ")
    }

    getStagesStatus(
        portStages, "Cooling", listOf(Stage.COOLING_1, Stage.COOLING_2, Stage.COOLING_3)
    )?.let { statusParts.add(it) }

    getStagesStatus(
        portStages, "Heating", listOf(Stage.HEATING_1, Stage.HEATING_2, Stage.HEATING_3)
    )?.let { statusParts.add(it) }

    if (portStages.containsKey("Water Valve") || portStages.containsKey("WATER_VALVE")
        || analogOutStages.containsKey("Water Valve") || analogOutStages.containsKey("WATER_VALVE")) {
        statusParts.add("Water Valve ON")
    }
    getValveStatus(portStages, analogOutStages)?.let { statusParts.add(it) }

    getAuxHeatStatus(portStages)?.let { statusParts.add(it) }

    getFanStatus(temperatureState, portStages)?.let { statusParts.add(it) }


    // Fallback if no fan stages but fan is enabled
    if (!statusParts.any { it.contains("Fan") } && !analogOutStages.containsKey(StatusMsgKeys.FAN_SPEED.name)) {
        if (portStages.containsKey(StatusMsgKeys.FAN_ENABLED.name)) {
            statusParts.add("Fan ON")
        } else if (portStages.containsKey(StatusMsgKeys.EQUIP_ON.name)) {
            statusParts.add("Equipment ON")
        }
    }
    getAnalogStatus(analogOutStages)?.let { statusParts.add(it) }

    if (portStages.containsKey(StatusMsgKeys.DCV_DAMPER.name) || analogOutStages.containsKey(StatusMsgKeys.DCV_DAMPER.name)) {
        statusParts.add("DCV ON")
    }

    var status = statusParts.joinToString(", ")
    if (status.isEmpty()) status = "OFF"
    return status
}


fun updateUserIntentPoints(
    equipRef: String, point: Point, value: Double, who: String
) {
    ExecutorTask.executeAsync({ }, {
        val pointData = point.domainName.readPoint(equipRef)
        if (pointData.containsKey("writable")) {
            point.pointWriteByUser(value, who)
        }
        CcuLog.i(L.TAG_CCU_MSHST, "updated ${point.domainName} value : $value")
        val roomRef = HSUtil.getZoneIdFromEquipId(equipRef)
        DesiredTempDisplayMode.setModeTypeOnUserIntentChange(
            roomRef, CCUHsApi.getInstance()
        )
    })
}
