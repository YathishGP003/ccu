package a75f.io.logic.bo.building.system

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.ConditioningStages
import a75f.io.logger.CcuLog
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.ControllerNames

/**
 * Created by Manjunath K on 12-05-2025.
 */

class SystemStageHandler(private var conditioningStages: ConditioningStages) {

    fun runControllersAndUpdateStatus(controllers: HashMap<String,Any>, conditioningMode : Int, conditioningStages: ConditioningStages) {
        this.conditioningStages = conditioningStages
        controllers.forEach { (controllerName, value) ->
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
                DomainName.loadCoolingStage1 -> conditioningStages.loadCoolingStage1
                DomainName.loadCoolingStage2 -> conditioningStages.loadCoolingStage2
                DomainName.loadCoolingStage3 -> conditioningStages.loadCoolingStage3
                DomainName.loadCoolingStage4 -> conditioningStages.loadCoolingStage4
                DomainName.loadCoolingStage5 -> conditioningStages.loadCoolingStage5
                DomainName.loadHeatingStage1 -> conditioningStages.loadHeatingStage1
                DomainName.loadHeatingStage2 -> conditioningStages.loadHeatingStage2
                DomainName.loadHeatingStage3 -> conditioningStages.loadHeatingStage3
                DomainName.loadHeatingStage4 -> conditioningStages.loadHeatingStage4
                DomainName.loadHeatingStage5 -> conditioningStages.loadHeatingStage5
                DomainName.loadFanStage1 -> conditioningStages.loadFanStage1
                DomainName.loadFanStage2 -> conditioningStages.loadFanStage2
                DomainName.loadFanStage3 -> conditioningStages.loadFanStage3
                DomainName.loadFanStage4 -> conditioningStages.loadFanStage4
                DomainName.loadFanStage5 -> conditioningStages.loadFanStage5
                DomainName.satCoolingStage1 -> conditioningStages.satCoolingStage1
                DomainName.satCoolingStage2 -> conditioningStages.satCoolingStage2
                DomainName.satCoolingStage3 -> conditioningStages.satCoolingStage3
                DomainName.satCoolingStage4 -> conditioningStages.satCoolingStage4
                DomainName.satCoolingStage5 -> conditioningStages.satCoolingStage5
                DomainName.satHeatingStage1 -> conditioningStages.satHeatingStage1
                DomainName.satHeatingStage2 -> conditioningStages.satHeatingStage2
                DomainName.satHeatingStage3 -> conditioningStages.satHeatingStage3
                DomainName.satHeatingStage4 -> conditioningStages.satHeatingStage4
                DomainName.satHeatingStage5 -> conditioningStages.satHeatingStage5
                DomainName.fanPressureStage1 -> conditioningStages.fanPressureStage1
                DomainName.fanPressureStage2 -> conditioningStages.fanPressureStage2
                DomainName.fanPressureStage3 -> conditioningStages.fanPressureStage3
                DomainName.fanPressureStage4 -> conditioningStages.fanPressureStage4
                DomainName.fanPressureStage5 -> conditioningStages.fanPressureStage5
                DomainName.ahuFreshAirFanRunCommand -> conditioningStages.ahuFreshAirFanRunCommand
                DomainName.exhaustFanStage1 -> conditioningStages.exhaustFanStage1
                DomainName.exhaustFanStage2 -> conditioningStages.exhaustFanStage2
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

            ControllerNames.LOAD_COOLING_STAGE_CONTROLLER -> {
                val coolingStages = result as List<Pair<Int, Boolean>>
                coolingStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateRelayStatus(DomainName.loadCoolingStage1, isActive)
                        1 -> updateRelayStatus(DomainName.loadCoolingStage2, isActive)
                        2 -> updateRelayStatus(DomainName.loadCoolingStage3, isActive)
                        3 -> updateRelayStatus(DomainName.loadCoolingStage4, isActive)
                        4 -> updateRelayStatus(DomainName.loadCoolingStage5, isActive)
                    }
                }
            }
            ControllerNames.SAT_COOLING_STAGE_CONTROLLER -> {
                val coolingStages = result as List<Pair<Int, Boolean>>
                coolingStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateRelayStatus(DomainName.satCoolingStage1, isActive)
                        1 -> updateRelayStatus(DomainName.satCoolingStage2, isActive)
                        2 -> updateRelayStatus(DomainName.satCoolingStage3, isActive)
                        3 -> updateRelayStatus(DomainName.satCoolingStage4, isActive)
                        4 -> updateRelayStatus(DomainName.satCoolingStage5, isActive)
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
            ControllerNames.LOAD_HEATING_STAGE_CONTROLLER -> {
                val heatingStages = result as List<Pair<Int, Boolean>>
                heatingStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateRelayStatus(DomainName.loadHeatingStage1, isActive)
                        1 -> updateRelayStatus(DomainName.loadHeatingStage2, isActive)
                        2 -> updateRelayStatus(DomainName.loadHeatingStage3, isActive)
                        3 -> updateRelayStatus(DomainName.loadHeatingStage4, isActive)
                        4 -> updateRelayStatus(DomainName.loadHeatingStage5, isActive)
                    }
                }
            }
            ControllerNames.SAT_HEATING_STAGE_CONTROLLER -> {
                val heatingStages = result as List<Pair<Int, Boolean>>
                heatingStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateRelayStatus(DomainName.satHeatingStage1, isActive)
                        1 -> updateRelayStatus(DomainName.satHeatingStage2, isActive)
                        2 -> updateRelayStatus(DomainName.satHeatingStage3, isActive)
                        3 -> updateRelayStatus(DomainName.satHeatingStage4, isActive)
                        4 -> updateRelayStatus(DomainName.satHeatingStage5, isActive)
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
            ControllerNames.LOAD_FAN_STAGE_CONTROLLER -> {
                val fanStages = result as List<Pair<Int, Boolean>>
                fanStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateRelayStatus(DomainName.loadFanStage1, isActive)
                        1 -> updateRelayStatus(DomainName.loadFanStage2, isActive)
                        2 -> updateRelayStatus(DomainName.loadFanStage3, isActive)
                        3 -> updateRelayStatus(DomainName.loadFanStage4, isActive)
                        4 -> updateRelayStatus(DomainName.loadFanStage5, isActive)
                    }
                }
            }

            ControllerNames.PRESSURE_FAN_STAGE_CONTROLLER -> {
                val fanStages = result as List<Pair<Int, Boolean>>
                fanStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateRelayStatus(DomainName.fanPressureStage1, isActive)
                        1 -> updateRelayStatus(DomainName.fanPressureStage2, isActive)
                        2 -> updateRelayStatus(DomainName.fanPressureStage3, isActive)
                        3 -> updateRelayStatus(DomainName.fanPressureStage4, isActive)
                        4 -> updateRelayStatus(DomainName.fanPressureStage5, isActive)
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

            ControllerNames.HUMIDIFIER_CONTROLLER -> {
                updateStatus(
                    DomainName.humidifierEnable,
                    result as Boolean
                )
            }

            ControllerNames.DEHUMIDIFIER_CONTROLLER -> {
                updateStatus(DomainName.dehumidifierEnable, result)
            }
            ControllerNames.FAN_ENABLED -> {
                updateStatus(DomainName.fanEnable, result as Boolean)
            }
            ControllerNames.OCCUPIED_ENABLED -> {
                updateStatus(DomainName.occupiedEnable, result)
            }
            ControllerNames.DAMPER_RELAY_CONTROLLER -> {
                updateStatus(DomainName.dcvDamper, result)
            }
            ControllerNames.FAN_RUN_COMMAND_CONTROLLER -> {
                updateStatus(DomainName.ahuFreshAirFanRunCommand, result as Boolean)
            }

        }

    }
}