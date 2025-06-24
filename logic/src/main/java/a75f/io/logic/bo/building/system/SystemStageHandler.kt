package a75f.io.logic.bo.building.system

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.ConditioningStages
import a75f.io.domain.equips.DomainEquip
import a75f.io.domain.equips.SystemEquip
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.ControllerNames

/**
 * Created by Manjunath K on 12-05-2025.
 */

class SystemStageHandler(val conditioningStages: ConditioningStages) {

    fun runControllersAndUpdateStatus(equip: DomainEquip, conditioningMode : Int) {
        equip.controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            if (conditioningMode == SystemMode.OFF.ordinal) {
                controller.resetController()
            }
            val result = controller.runController()
            updateRelayStatus(controllerName, result, conditioningMode)
        }
    }

    private fun updateRelayStatus(
        controllerName: String, result: Any, conditioningMode :Int
    ) {

        fun getPointByStage(domainName :String): Point {
            return when (domainName) {
                DomainName.coolingStage1 -> conditioningStages.coolingStage1
                DomainName.coolingStage2 -> conditioningStages.coolingStage2
                DomainName.coolingStage3 -> conditioningStages.coolingStage3
                DomainName.coolingStage4 -> conditioningStages.coolingStage4
                DomainName.coolingStage5 -> conditioningStages.coolingStage5
                DomainName.heatingStage1 -> conditioningStages.heatingStage1
                DomainName.heatingStage2 -> conditioningStages.heatingStage2
                DomainName.heatingStage3 -> conditioningStages.heatingStage3
                DomainName.heatingStage4 -> conditioningStages.heatingStage4
                DomainName.heatingStage5 -> conditioningStages.heatingStage5
                DomainName.fanStage1 -> conditioningStages.fanStage1
                DomainName.fanStage2 -> conditioningStages.fanStage2
                DomainName.fanStage3 -> conditioningStages.fanStage3
                DomainName.fanStage4 -> conditioningStages.fanStage4
                DomainName.fanStage5 -> conditioningStages.fanStage5
                DomainName.humidifierEnable -> conditioningStages.humidifierEnable
                DomainName.dehumidifierEnable -> conditioningStages.dehumidifierEnable
                DomainName.compressorStage1 -> conditioningStages.compressorStage1
                DomainName.compressorStage2 -> conditioningStages.compressorStage2
                DomainName.compressorStage3 -> conditioningStages.compressorStage3
                DomainName.compressorStage4 -> conditioningStages.compressorStage4
                DomainName.compressorStage5 -> conditioningStages.compressorStage5
                DomainName.changeOverCooling -> conditioningStages.changeOverCooling
                DomainName.changeOverHeating -> conditioningStages.changeOverHeating
                DomainName.fanEnable -> conditioningStages.fanEnable
                DomainName.occupiedEnable -> conditioningStages.occupiedEnabled
                DomainName.dcvDamper -> conditioningStages.dcvDamper
                else -> throw IllegalArgumentException("Unknown domain name: $domainName")
            }
        }

        fun updateRelayStatus(domainName: String, isActive: Boolean) {
            val point = getPointByStage(domainName)
            if (point.pointExists()) {
                val status = if (isActive) 1.0 else 0.0
                point.writeHisVal(status)
            }
        }

        fun updateStatus(domainName: String, result: Any) {
            val point = getPointByStage(domainName)
            point.writeHisVal(if ((result as Boolean) && conditioningMode != 0) 1.0 else 0.0)
        }

        when (controllerName) {
            ControllerNames.COOLING_STAGE_CONTROLLER -> {
                val coolingStages = result as List<Pair<Int, Boolean>>
                coolingStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateRelayStatus(DomainName.coolingStage1, isActive)
                        1 -> updateRelayStatus(DomainName.coolingStage2, isActive)
                        2 -> updateRelayStatus(DomainName.coolingStage3, isActive)
                        3 -> updateRelayStatus(DomainName.coolingStage4, isActive)
                        4 -> updateRelayStatus(DomainName.coolingStage5, isActive)
                    }
                }
            }

            ControllerNames.HEATING_STAGE_CONTROLLER -> {
                val heatingStages = result as List<Pair<Int, Boolean>>
                heatingStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateRelayStatus(DomainName.heatingStage1, isActive)
                        1 -> updateRelayStatus(DomainName.heatingStage2, isActive)
                        2 -> updateRelayStatus(DomainName.heatingStage3, isActive)
                        3 -> updateRelayStatus(DomainName.heatingStage4, isActive)
                        4 -> updateRelayStatus(DomainName.heatingStage5, isActive)
                    }
                }
            }

            ControllerNames.FAN_SPEED_CONTROLLER -> {
                val fanStages = result as List<Pair<Int, Boolean>>
                fanStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateRelayStatus(DomainName.fanStage1, isActive)
                        1 -> updateRelayStatus(DomainName.fanStage2, isActive)
                        2 -> updateRelayStatus(DomainName.fanStage3, isActive)
                        3 -> updateRelayStatus(DomainName.fanStage4, isActive)
                        4 -> updateRelayStatus(DomainName.fanStage5, isActive)
                    }
                }
            }

            ControllerNames.COMPRESSOR_RELAY_CONTROLLER -> {
                val compressorStages = result as List<Pair<Int, Boolean>>
                compressorStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateRelayStatus(DomainName.compressorStage1, isActive)
                        1 -> updateRelayStatus(DomainName.compressorStage2, isActive)
                        2 -> updateRelayStatus(DomainName.compressorStage3, isActive)
                        3 -> updateRelayStatus(DomainName.compressorStage4, isActive)
                        4 -> updateRelayStatus(DomainName.compressorStage5, isActive)
                    }
                }
            }

            ControllerNames.CHANGE_OVER_O_COOLING -> {
                var status = result as Boolean
                if (conditioningMode == SystemMode.OFF.ordinal) {
                    status = false
                }
                updateStatus(DomainName.changeOverCooling, status)
            }

            ControllerNames.CHANGE_OVER_B_HEATING -> {
                var status = result as Boolean
                if (conditioningMode == SystemMode.OFF.ordinal) {
                    status = false
                }
                updateStatus(DomainName.changeOverHeating, status)
            }

            ControllerNames.HUMIDIFIER_CONTROLLER -> updateStatus(
                DomainName.humidifierEnable,
                result as Boolean
            )

            ControllerNames.DEHUMIDIFIER_CONTROLLER -> updateStatus(DomainName.dehumidifierEnable, result)
            ControllerNames.FAN_ENABLED -> updateStatus(DomainName.fanEnable, result as Boolean)
            ControllerNames.OCCUPIED_ENABLED -> updateStatus(DomainName.occupiedEnable, result)
            ControllerNames.DAMPER_RELAY_CONTROLLER -> updateStatus(DomainName.dcvDamper, result)

        }

    }
}