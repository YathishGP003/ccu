package a75f.io.logic.bo.building.statprofiles.statcontrollers

import a75f.io.domain.api.Point
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZoneProfile
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.statprofiles.util.StagesCounts
import a75f.io.logic.controlcomponents.controls.ControllerFactory
import a75f.io.logic.controlcomponents.handlers.StageControlHandler
import a75f.io.logic.controlcomponents.util.ControllerNames


/**
 * Created by Manjunath K on 09-06-2025.
 */

class MyStatControlFactory(
    var equip: MyStatEquip, var controllers: HashMap<String, Any>, var counts: StagesCounts,
    var derivedFanLoopOutput: CalibratedPoint, var zoneOccupancyState: CalibratedPoint,
) {
    private val controllerFactory = ControllerFactory()

    fun addCpuControllers(config: MyStatCpuConfiguration) {
        addCoolingController(config)
        addHeatingControllers(config)
        addFanSpeedControllers(config, L.TAG_CCU_MSCPU,derivedFanLoopOutput)
        addFanEnableIfRequired(L.TAG_CCU_MSCPU)
        addOccupiedEnabledIfRequired(L.TAG_CCU_MSCPU)
        addHumidifierIfRequired(L.TAG_CCU_MSCPU)
        addDehumidifierIfRequired(L.TAG_CCU_MSCPU)
        addDcvDamperIfRequired(L.TAG_CCU_MSCPU)
    }

    fun addHpuControllers(config: MyStatHpuConfiguration) {
        val hpuEquip = equip as MyStatHpuEquip
        addCompressorControllers(config)
        addFanSpeedControllers(config, L.TAG_CCU_MSHPU, hpuEquip.fanLoopOutput)
        addAuxStage1Controller(L.TAG_CCU_MSHPU, hpuEquip.auxHeatingStage1, hpuEquip.mystatAuxHeating1Activate)
        addChangeOverCoolingIfRequired()
        addChangeOverHeatingIfRequired()
        addFanEnableIfRequired(L.TAG_CCU_MSHPU)
        addOccupiedEnabledIfRequired(L.TAG_CCU_MSHPU)
        addHumidifierIfRequired(L.TAG_CCU_MSHPU)
        addDehumidifierIfRequired(L.TAG_CCU_MSHPU)
        addDcvDamperIfRequired(L.TAG_CCU_MSHPU)
    }

    fun addPipe2Controllers(config: MyStatPipe2Configuration, waterValveLoop: CalibratedPoint) {
        val pipe2Equip = equip as MyStatPipe2Equip
        addFanSpeedControllers(config, L.TAG_CCU_MSPIPE2, pipe2Equip.fanLoopOutput)
        addAuxStage1Controller(L.TAG_CCU_MSPIPE2, pipe2Equip.auxHeatingStage1, pipe2Equip.auxHeating1Activate)
        addWaterValveControllerIfRequired(waterValveLoop)
        addFanEnableIfRequired(L.TAG_CCU_MSPIPE2)
        addOccupiedEnabledIfRequired(L.TAG_CCU_MSPIPE2)
        addHumidifierIfRequired(L.TAG_CCU_MSPIPE2)
        addDehumidifierIfRequired(L.TAG_CCU_MSPIPE2)
        addDcvDamperIfRequired(L.TAG_CCU_MSPIPE2)
    }

    private fun addCoolingController(config: MyStatCpuConfiguration) {
        counts.coolingStages.data = config.getHighestCoolingStageCount().toDouble()
        if (counts.coolingStages.data > 0) {
            controllerFactory.addStageController(
                ControllerNames.COOLING_STAGE_CONTROLLER,
                controllers,
                equip.coolingLoopOutput,
                counts.coolingStages,
                equip.standaloneRelayActivationHysteresis,
                stageDownTimer = equip.mystatStageDownTimerCounter,
                stageUpTimer = equip.mystatStageUpTimerCounter,
                logTag = L.TAG_CCU_MSCPU
            )
        } else {
            removeController(ControllerNames.COOLING_STAGE_CONTROLLER)
        }
    }

    private fun addHeatingControllers(config: MyStatCpuConfiguration) {
        counts.heatingStages.data = config.getHighestHeatingStageCount().toDouble()
        if (counts.heatingStages.data > 0) {
            controllerFactory.addStageController(
                ControllerNames.HEATING_STAGE_CONTROLLER,
                controllers,
                equip.heatingLoopOutput,
                counts.heatingStages,
                equip.standaloneRelayActivationHysteresis,
                stageDownTimer = equip.mystatStageDownTimerCounter,
                stageUpTimer = equip.mystatStageUpTimerCounter,
                logTag = L.TAG_CCU_MSCPU
            )
        } else {
            removeController(ControllerNames.HEATING_STAGE_CONTROLLER)
        }
    }

    private fun addFanSpeedControllers(config: MyStatConfiguration, tag: String, loopOutput: Point) {
        counts.fanStages.data = config.getHighestFanStageCount().toDouble()
        if (counts.fanStages.data > 0) {
            controllerFactory.addStageController(
                ControllerNames.FAN_SPEED_CONTROLLER,
                controllers,
                loopOutput,
                counts.fanStages,
                equip.standaloneRelayActivationHysteresis,
                stageDownTimer = equip.mystatStageDownTimerCounter,
                stageUpTimer = equip.mystatStageUpTimerCounter,
                logTag = tag
            )
        } else {
            removeController(ControllerNames.FAN_SPEED_CONTROLLER)
        }
    }

    private fun addCompressorControllers(config: MyStatHpuConfiguration) {
        counts.compressorStages.data = config.getHighestCompressorStages().toDouble()
        if (counts.compressorStages.data > 0) {
            controllerFactory.addStageController(
                ControllerNames.COMPRESSOR_RELAY_CONTROLLER,
                controllers,
                loopOutput = (equip as MyStatHpuEquip).compressorLoopOutput,
                activationHysteresis = equip.standaloneRelayActivationHysteresis,
                totalStages = counts.compressorStages,
                stageDownTimer = equip.mystatStageDownTimerCounter,
                stageUpTimer = equip.mystatStageUpTimerCounter,
                logTag = L.TAG_CCU_MSHPU
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
        if ((equip as MyStatHpuEquip).changeOverCooling.pointExists()) {
            controllerFactory.addChangeOverCoolingController(
                controllers, equip.coolingLoopOutput, logTag = L.TAG_CCU_MSHPU
            )
        } else {
            removeController(ControllerNames.CHANGE_OVER_O_COOLING)
        }
    }

    private fun addChangeOverHeatingIfRequired() {
        if ((equip as MyStatHpuEquip).changeOverHeating.pointExists()) {
            controllerFactory.addChangeOverHeatingController(
                controllers, equip.heatingLoopOutput, logTag = L.TAG_CCU_MSHPU
            )
        } else {
            removeController(ControllerNames.CHANGE_OVER_B_HEATING)
        }
    }

    private fun addWaterValveControllerIfRequired(waterValveLoop: CalibratedPoint) {
        if ((equip as MyStatPipe2Equip).waterValve.pointExists()) {
            controllerFactory.addWaterValveController(
                controllers,
                waterValveLoop = waterValveLoop,
                actionHysteresis = equip.standaloneRelayActivationHysteresis,
                logTag = L.TAG_CCU_MSPIPE2
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