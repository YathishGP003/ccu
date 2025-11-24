package a75f.io.logic.bo.building.system.util

import a75f.io.domain.equips.ConditioningStages
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.system.SystemController
import a75f.io.logic.bo.building.system.SystemProfile
import a75f.io.logic.bo.building.system.dab.DabSystemController

/**
 * Author: Manjunath Kundaragi
 * Created on: 21-11-2025
 */


fun getSystemStatusMsg(
    systemProfile: SystemProfile,
    conditionStages: ConditioningStages,
    isCompressorActive: Boolean = false,
    isEconActive: Boolean
): String {
    val cooling = LinkedHashMap<String, Int>()
    val heating = LinkedHashMap<String, Int>()
    val fan = LinkedHashMap<String, Int>()

    if (systemProfile.isCoolingActive() || (DabSystemController.getInstance().systemState == SystemController.State.COOLING && isCompressorActive)) {
        mapOf(
            Stage.COOLING_1 to conditionStages.coolingStage1,
            Stage.COOLING_2 to conditionStages.coolingStage2,
            Stage.COOLING_3 to conditionStages.coolingStage3,
            Stage.COOLING_4 to conditionStages.coolingStage4,
            Stage.COOLING_5 to conditionStages.coolingStage5
        ).forEach { (stage, point) ->
            if (point.readHisVal().toInt() > 0) cooling[stage.displayName] = 1
        }
    }

    if (systemProfile.isHeatingActive() || (DabSystemController.getInstance().systemState == SystemController.State.HEATING && isCompressorActive)) {
        mapOf(
            Stage.HEATING_1 to conditionStages.heatingStage1,
            Stage.HEATING_2 to conditionStages.heatingStage2,
            Stage.HEATING_3 to conditionStages.heatingStage3,
            Stage.HEATING_4 to conditionStages.heatingStage4,
            Stage.HEATING_5 to conditionStages.heatingStage5
        ).forEach { (stage, point) ->
            if (point.readHisVal().toInt() > 0) heating[stage.displayName] = 1
        }
    }
    mapOf(
        Stage.FAN_1 to conditionStages.fanStage1,
        Stage.FAN_2 to conditionStages.fanStage2,
        Stage.FAN_3 to conditionStages.fanStage3,
        Stage.FAN_4 to conditionStages.fanStage4,
        Stage.FAN_5 to conditionStages.fanStage5
    ).forEach { (stage, point) ->
        if (point.readHisVal().toInt() > 0) fan[stage.displayName] = 1
    }


    val coolingStatus = getStagesStatus(
        cooling,
        "Cooling Stages",
        listOf(Stage.COOLING_1, Stage.COOLING_2, Stage.COOLING_3, Stage.COOLING_4, Stage.COOLING_5)
    )

    val heatingStatus = getStagesStatus(
        heating,
        "Heating Stages",
        listOf(Stage.HEATING_1, Stage.HEATING_2, Stage.HEATING_3, Stage.HEATING_4, Stage.HEATING_5)
    )

    val fanStatus = getStagesStatus(
        fan, "Fan Stages", listOf(Stage.FAN_1, Stage.FAN_2, Stage.FAN_3, Stage.FAN_4, Stage.FAN_5)
    )

    var humidifierStatus: String
    var dehumidifierStatus: String
    var fanEnable = ""
    conditionStages.apply {
        humidifierStatus = if (humidifierEnable.readHisVal() > 0) " Humidifier ON " else " Humidifier OFF "
        dehumidifierStatus = if (dehumidifierEnable.readHisVal() > 0) " Dehumidifier ON " else " Dehumidifier OFF "
        if (fan.isEmpty()) {
            fanEnable = if (conditionStages.fanEnable.readHisVal() > 0) "Fan ON" else ""
        }
    }

    val statusParts = mutableListOf<String>()
    if(isEconActive) statusParts.add("Free Cooling Used")
    if (coolingStatus != null) statusParts.add(coolingStatus)
    if (heatingStatus != null) statusParts.add(heatingStatus)
    if (fanStatus != null) statusParts.add(fanStatus)
    if (fanEnable.isNotEmpty()) statusParts.add(fanEnable.trim())
    if (humidifierStatus.isNotEmpty()) statusParts.add(humidifierStatus.trim())
    if (dehumidifierStatus.isNotEmpty()) statusParts.add(dehumidifierStatus.trim())

    val finalStatusMsg = if (statusParts.isNotEmpty()) {
        statusParts.joinToString(" | ")
    } else {
        "System OFF"
    }

    return finalStatusMsg
}

fun getStagesStatus(
    portStages: Map<String, Int>, prefix: String, stages: List<Stage>
): String? {
    val activeStages = stages.filter { portStages.containsKey(it.displayName) }
    if (activeStages.isEmpty()) return null
    val stageNumbers = activeStages.map { it.displayName.last().digitToInt() }.sorted()
    val formatted = when (stageNumbers.size) {
        1 -> "${stageNumbers[0]}"
        else -> {
            val allButLast = stageNumbers.dropLast(1).joinToString(",")
            val last = stageNumbers.last()
            "$allButLast&$last"
        }
    }
    return "$prefix $formatted ON"
}
