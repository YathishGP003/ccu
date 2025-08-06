package a75f.io.logic.bo.building.statprofiles.statcontrollers

import a75f.io.domain.api.Point
import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.hyperstat.Pipe2V2Equip
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZoneProfile
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Pipe2Configuration
import a75f.io.logic.bo.building.statprofiles.util.StagesCounts
import a75f.io.logic.controlcomponents.controls.ControllerFactory
import a75f.io.logic.controlcomponents.handlers.StageControlHandler
import a75f.io.logic.controlcomponents.util.ControllerNames

/**
 * Created by Manjunath K on 12-05-2025.
 */

class HyperStatControlFactory(
    var equip: HyperStatEquip, var controllers: HashMap<String, Any>, var counts: StagesCounts,
    var derivedFanLoopOutput: CalibratedPoint, var zoneOccupancyState: CalibratedPoint,
) {
    private val controllerFactory = ControllerFactory()

    fun addHsCpuControllers(config: CpuConfiguration) {
        addCoolingController(config)
        addHeatingControllers(config)
        addFanSpeedControllers(config, L.TAG_CCU_HSCPU)
        addFanEnableIfRequired(L.TAG_CCU_HSCPU)
        addOccupiedEnabledIfRequired(L.TAG_CCU_HSCPU)
        addHumidifierIfRequired(L.TAG_CCU_HSCPU)
        addDehumidifierIfRequired(L.TAG_CCU_HSCPU)
        addDcvDamperIfRequired(L.TAG_CCU_HSCPU)
    }

    fun addHpuControllers(config: HpuConfiguration) {
        val hpuEquip = equip as HpuV2Equip
        addCompressorControllers(config)
        addFanSpeedControllers(config, L.TAG_CCU_HSHPU)
        addAuxStage1Controller(
            L.TAG_CCU_HSHPU, hpuEquip.auxHeatingStage1, hpuEquip.auxHeating1Activate
        )
        addAuxStage2Controller(
            L.TAG_CCU_HSHPU, hpuEquip.auxHeatingStage2, hpuEquip.auxHeating2Activate
        )
        addChangeOverCoolingIfRequired()
        addChangeOverHeatingIfRequired()
        addFanEnableIfRequired(L.TAG_CCU_HSHPU)
        addOccupiedEnabledIfRequired(L.TAG_CCU_HSHPU)
        addHumidifierIfRequired(L.TAG_CCU_HSHPU)
        addDehumidifierIfRequired(L.TAG_CCU_HSHPU)
        addDcvDamperIfRequired(L.TAG_CCU_HSHPU)
    }

    fun addPipe2Controllers(config: Pipe2Configuration, waterValveLoop: CalibratedPoint) {
        val pipe2Equip = equip as Pipe2V2Equip
        addFanSpeedControllers(config, L.TAG_CCU_HSPIPE2)
        addAuxStage1Controller(
            L.TAG_CCU_HSPIPE2, pipe2Equip.auxHeatingStage1, pipe2Equip.auxHeating1Activate
        )
        addAuxStage2Controller(
            L.TAG_CCU_HSPIPE2, pipe2Equip.auxHeatingStage2, pipe2Equip.auxHeating2Activate
        )
        addWaterValveControllerIfRequired(waterValveLoop)
        addFanEnableIfRequired(L.TAG_CCU_HSPIPE2)
        addOccupiedEnabledIfRequired(L.TAG_CCU_HSPIPE2)
        addHumidifierIfRequired(L.TAG_CCU_HSPIPE2)
        addDehumidifierIfRequired(L.TAG_CCU_HSPIPE2)
        addDcvDamperIfRequired(L.TAG_CCU_HSPIPE2)
    }

    private fun addCoolingController(config: CpuConfiguration) {
        counts.coolingStages.data = config.getHighestCoolingStageCount().toDouble()
        if (counts.coolingStages.data > 0) {
            controllerFactory.addStageController(
                ControllerNames.COOLING_STAGE_CONTROLLER,
                controllers,
                equip.coolingLoopOutput,
                counts.coolingStages,
                equip.standaloneRelayActivationHysteresis,
                stageDownTimer = equip.hyperstatStageDownTimerCounter,
                stageUpTimer = equip.hyperstatStageUpTimerCounter,
                logTag = L.TAG_CCU_HSCPU
            )
        } else {
            removeController(ControllerNames.COOLING_STAGE_CONTROLLER)
        }
    }

    private fun addHeatingControllers(config: CpuConfiguration) {
        counts.heatingStages.data = config.getHighestHeatingStageCount().toDouble()
        if (counts.heatingStages.data > 0) {
            controllerFactory.addStageController(
                ControllerNames.HEATING_STAGE_CONTROLLER,
                controllers,
                equip.heatingLoopOutput,
                counts.heatingStages,
                equip.standaloneRelayActivationHysteresis,
                stageDownTimer = equip.hyperstatStageDownTimerCounter,
                stageUpTimer = equip.hyperstatStageUpTimerCounter,
                logTag = L.TAG_CCU_HSCPU
            )
        } else {
            removeController(ControllerNames.HEATING_STAGE_CONTROLLER)
        }
    }

    private fun addFanSpeedControllers(config: HyperStatConfiguration, tag: String) {
        counts.fanStages.data = config.getHighestFanStageCount().toDouble()
        if (counts.fanStages.data > 0) {
            controllerFactory.addStageController(
                ControllerNames.FAN_SPEED_CONTROLLER,
                controllers,
                derivedFanLoopOutput,
                counts.fanStages,
                equip.standaloneRelayActivationHysteresis,
                stageDownTimer = equip.hyperstatStageDownTimerCounter,
                stageUpTimer = equip.hyperstatStageUpTimerCounter,
                logTag = tag
            )
        } else {
            removeController(ControllerNames.FAN_SPEED_CONTROLLER)
        }
    }

    private fun addCompressorControllers(config: HpuConfiguration) {
        counts.compressorStages.data = config.getHighestCompressorStages().toDouble()
        if (counts.compressorStages.data > 0) {
            controllerFactory.addStageController(
                ControllerNames.COMPRESSOR_RELAY_CONTROLLER,
                controllers,
                loopOutput = (equip as HpuV2Equip).compressorLoopOutput,
                activationHysteresis = equip.standaloneRelayActivationHysteresis,
                totalStages = counts.compressorStages,
                stageDownTimer = equip.hyperstatStageDownTimerCounter,
                stageUpTimer = equip.hyperstatStageUpTimerCounter,
                logTag = L.TAG_CCU_HSHPU
            )
        } else {
            removeController(ControllerNames.COMPRESSOR_RELAY_CONTROLLER)
        }
    }

    private fun addAuxStage1Controller(
        tag: String, auxHeatingStage1: Point, auxHeating1Activate: Point
    ) {
        if (auxHeatingStage1.pointExists()) {
            controllerFactory.addAuxHeatingStage1Controller(
                controllers,
                equip.currentTemp,
                equip.desiredTempHeating,
                auxHeating1Activate,
                logTag = tag
            )
        } else {
            removeController(ControllerNames.AUX_HEATING_STAGE1)
        }
    }

    private fun addAuxStage2Controller(
        tag: String, auxHeatingStage2: Point, auxHeating2Activate: Point
    ) {
        if (auxHeatingStage2.pointExists()) {
            controllerFactory.addAuxHeatingStage2Controller(
                controllers,
                equip.currentTemp,
                equip.desiredTempHeating,
                auxHeating2Activate,
                logTag = tag
            )
        } else {
            removeController(ControllerNames.AUX_HEATING_STAGE2)
        }
    }

    private fun addFanEnableIfRequired(tag: String) {
        if (equip.fanEnable.pointExists()) {
            controllerFactory.addFanEnableController(
                controllers, equip.fanLoopOutput, zoneOccupancyState, logTag = tag
            )
        } else {
            removeController(ControllerNames.FAN_ENABLED)
        }
    }

    private fun addOccupiedEnabledIfRequired(tag: String) {
        if (equip.occupiedEnable.pointExists()) {
            controllerFactory.addOccupiedEnableController(
                controllers, zoneOccupancyState, logTag = tag
            )
        } else {
            removeController(ControllerNames.OCCUPIED_ENABLED)
        }
    }

    private fun addHumidifierIfRequired(tag: String) {
        if (equip.humidifierEnable.pointExists()) {
            controllerFactory.addHumidifierController(
                controllers,
                equip.zoneHumidity,
                equip.targetHumidifier,
                equip.standaloneHumidityHysteresis,
                occupancy = zoneOccupancyState,
                logTag = tag
            )
        } else {
            removeController(ControllerNames.HUMIDIFIER_CONTROLLER)
        }
    }

    private fun addDehumidifierIfRequired(tag: String) {
        if (equip.dehumidifierEnable.pointExists()) {
            controllerFactory.addDeHumidifierController(
                controllers,
                equip.zoneHumidity,
                equip.targetDehumidifier,
                equip.standaloneHumidityHysteresis,
                occupancy = zoneOccupancyState,
                logTag = tag
            )
        } else {
            removeController(ControllerNames.DEHUMIDIFIER_CONTROLLER)
        }
    }

    private fun addDcvDamperIfRequired(tag: String) {
        if (equip.dcvDamper.pointExists()) {
            controllerFactory.addDcvDamperController(
                controllers,
                equip.dcvLoopOutput,
                equip.standaloneRelayActivationHysteresis,
                zoneOccupancyState,
                logTag = tag
            )
        } else {
            removeController(ControllerNames.DAMPER_RELAY_CONTROLLER)
        }
    }

    private fun addChangeOverCoolingIfRequired() {
        if ((equip as HpuV2Equip).changeOverCooling.pointExists()) {
            controllerFactory.addChangeOverCoolingController(
                controllers, equip.coolingLoopOutput, logTag = L.TAG_CCU_HSHPU
            )
        } else {
            removeController(ControllerNames.CHANGE_OVER_O_COOLING)
        }
    }

    private fun addChangeOverHeatingIfRequired() {
        if ((equip as HpuV2Equip).changeOverHeating.pointExists()) {
            controllerFactory.addChangeOverHeatingController(
                controllers, equip.heatingLoopOutput, logTag = L.TAG_CCU_HSHPU
            )
        } else {
            removeController(ControllerNames.CHANGE_OVER_B_HEATING)
        }
    }

    private fun addWaterValveControllerIfRequired(waterValveLoop: CalibratedPoint) {
        if ((equip as Pipe2V2Equip).waterValve.pointExists()) {
            controllerFactory.addWaterValveController(
                controllers,
                waterValveLoop = waterValveLoop,
                actionHysteresis = equip.standaloneRelayActivationHysteresis,
                logTag = L.TAG_CCU_HSPIPE2
            )
        } else {
            removeController(ControllerNames.WATER_VALVE_CONTROLLER)
        }
    }

    fun getController(controllerName: String, profile: ZoneProfile): StageControlHandler? {
        return if (profile.controllers.containsKey(controllerName)) profile.controllers[controllerName] as StageControlHandler
        else {
            return null
        }
    }

    private fun removeController(controllerName: String) {
        if (controllers.containsKey(controllerName)) controllers.remove(controllerName)
    }

}