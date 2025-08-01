package a75f.io.logic.bo.building.system

import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.L
import a75f.io.logic.controlcomponents.controls.ControllerFactory
import a75f.io.logic.controlcomponents.handlers.ExhaustFanController
import a75f.io.logic.controlcomponents.handlers.StageControlHandler
import a75f.io.logic.controlcomponents.util.ControllerNames

/**
 * Created by Manjunath K on 12-05-2025.
 */

class SystemControllerFactory(var controllers: HashMap<String, Any>) {
    private val factory = ControllerFactory()

    fun addCoolingControllers(
        coolingLoopOutput: Point,
        activationHysteresis: Point,
        stageUpTimerCounter: Point,
        stageDownTimerCounter: Point,
        economizationAvailable: CalibratedPoint,
        coolingLockOutActive: CalibratedPoint,
        coolingStages: CalibratedPoint
    ) {
        if (coolingStages.data > 0) {
            factory.addStageController(
                controllerName = ControllerNames.COOLING_STAGE_CONTROLLER,
                controllers,
                loopOutput = coolingLoopOutput,
                totalStages = coolingStages,
                activationHysteresis = activationHysteresis,
                stageUpTimer = stageUpTimerCounter,
                stageDownTimer = stageDownTimerCounter,
                economizingAvailable = economizationAvailable,
                lockOutActive = coolingLockOutActive,
                logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.COOLING_STAGE_CONTROLLER)
        }
    }

    fun addHeatingControllers(
        heatingLoopOutput: Point,
        activationHysteresis: Point,
        stageUpTimerCounter: Point,
        stageDownTimerCounter: Point,
        heatingLockOutActive: CalibratedPoint,
        heatingStages: CalibratedPoint
    ) {
        if (heatingStages.data > 0) {
            factory.addStageController(
                controllerName = ControllerNames.HEATING_STAGE_CONTROLLER,
                controllers,
                loopOutput = heatingLoopOutput,
                totalStages = heatingStages,
                activationHysteresis = activationHysteresis,
                stageUpTimer = stageUpTimerCounter,
                stageDownTimer = stageDownTimerCounter,
                lockOutActive = heatingLockOutActive,
                logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.HEATING_STAGE_CONTROLLER)
        }
    }

    fun addFanControllers(
        loopOutput: Point,
        activationHysteresis: Point,
        stageUpTimerCounter: Point,
        stageDownTimerCounter: Point,
        fanStages: CalibratedPoint
    ) {
        if (fanStages.data > 0) {
            factory.addStageController(
                controllerName = ControllerNames.FAN_SPEED_CONTROLLER,
                controllers,
                loopOutput = loopOutput,
                totalStages = fanStages,
                activationHysteresis = activationHysteresis,
                stageUpTimer = stageUpTimerCounter,
                stageDownTimer = stageDownTimerCounter,
                logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.FAN_SPEED_CONTROLLER)
        }
    }

    fun addCompressorControllers(
        compressorOutput: Point,
        activationHysteresis: Point,
        stageUpTimerCounter: Point,
        stageDownTimerCounter: Point,
        economizationAvailable: CalibratedPoint,
        compressorStages: CalibratedPoint,
        compressorLockout: CalibratedPoint
    ) {
        if (compressorStages.data > 0) {
            factory.addStageController(
                controllerName = ControllerNames.COMPRESSOR_RELAY_CONTROLLER,
                controllers,
                loopOutput = compressorOutput,
                totalStages = compressorStages,
                activationHysteresis = activationHysteresis,
                stageUpTimer = stageUpTimerCounter,
                stageDownTimer = stageDownTimerCounter,
                economizingAvailable = economizationAvailable,
                lockOutActive = compressorLockout,
                logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.COMPRESSOR_RELAY_CONTROLLER)
        }
    }

    fun addHumidifierController(
        zoneHumidity: Point,
        targetHumidifier: Point,
        activationHysteresis: Point,
        occupancyMode: CalibratedPoint,
        isControllerRequired: Boolean
    ) {
        if (isControllerRequired) {
            factory.addHumidifierController(
                controllers,
                zoneHumidity = zoneHumidity,
                targetHumidifier = targetHumidifier,
                activationHysteresis = activationHysteresis,
                occupancy = occupancyMode,
                logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.HUMIDIFIER_CONTROLLER)
        }
    }

    fun addDeHumidifierController(
        zoneHumidity: Point,
        targetDeHumidifier: Point,
        activationHysteresis: Point,
        occupancyMode: CalibratedPoint,
        isControllerRequired: Boolean
    ) {
        if (isControllerRequired) {
            factory.addDeHumidifierController(
                controllers,
                zoneHumidity = zoneHumidity,
                targetDehumidifier = targetDeHumidifier,
                activationHysteresis = activationHysteresis,
                occupancy = occupancyMode,
                logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.DEHUMIDIFIER_CONTROLLER)
        }
    }

    fun addChangeCoolingChangeOverRelay(
        coolingLoopOutput: Point, isControllerRequired: Boolean
    ) {
        if (isControllerRequired) {
            factory.addChangeOverCoolingController(
                controllers, coolingLoopOutput = coolingLoopOutput, logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.CHANGE_OVER_O_COOLING)
        }
    }

    fun addChangeHeatingChangeOverRelay(heatingLoopOutput: Point, isControllerRequired: Boolean) {
        if (isControllerRequired) {
            factory.addChangeOverHeatingController(
                controllers, heatingLoopOutput = heatingLoopOutput, logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.CHANGE_OVER_B_HEATING)
        }
    }

    fun addFanEnableController(
        fanLoopOutput: Point, occupancy: CalibratedPoint, isControllerRequired: Boolean
    ) {
        if (isControllerRequired) {
            factory.addFanEnableController(
                controllers,
                fanLoopOutput = fanLoopOutput,
                occupancy = occupancy,
                logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.FAN_ENABLED)
        }
    }

    fun addOccupiedEnabledController(occupancy: CalibratedPoint, isControllerRequired: Boolean) {

        if (isControllerRequired) {
            factory.addOccupiedEnableController(
                controllers, occupancy = occupancy, logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.OCCUPIED_ENABLED)
        }
    }

    fun addDcvDamperController(
        dcvLoopOutput: Point,
        activationHysteresis: Point,
        occupancy: CalibratedPoint,
        isControllerRequired: Boolean
    ) {
        if (isControllerRequired) {
            factory.addDcvDamperController(
                controllers,
                dcvLoopOutput = dcvLoopOutput,
                actionHysteresis = activationHysteresis,
                currentOccupancy = occupancy,
                logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.DAMPER_RELAY_CONTROLLER)
        }
    }

    fun addFanRunCommandController(
        systemCo2Loop: Point, occupancy: CalibratedPoint, isControllerRequired: Boolean
    ) {
        if (isControllerRequired) {
            factory.addFanRunCommandController(
                controllers, systemCo2Loop, occupancy, logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.FAN_RUN_COMMAND_CONTROLLER)
        }
    }

    fun addExhaustFanStage1Controller(
        outsideAirFinalLoopOutput: Point,
        exhaustFanStage1Threshold: Point,
        exhaustFanHysteresis: Point,
        isControllerRequired: Boolean
    ) {
        if (isControllerRequired) {
            if (controllers.containsKey(ControllerNames.EXHAUST_FAN_STAGE1_CONTROLLER)) return

            val controller = ExhaustFanController(
                outsideAirFinalLoopOutput,
                exhaustFanStage1Threshold,
                exhaustFanHysteresis,
                logTag = L.TAG_CCU_SYSTEM
            )
            controllers[ControllerNames.EXHAUST_FAN_STAGE1_CONTROLLER] = controller
        } else {
            removeController(ControllerNames.EXHAUST_FAN_STAGE1_CONTROLLER)
        }
    }

    fun addExhaustFanStage2Controller(
        outsideAirFinalLoopOutput: Point,
        exhaustFanStage2Threshold: Point,
        exhaustFanHysteresis: Point,
        isControllerRequired: Boolean
    ) {
        if (isControllerRequired) {
            if (controllers.containsKey(ControllerNames.EXHAUST_FAN_STAGE2_CONTROLLER)) return
            val controller = ExhaustFanController(
                outsideAirFinalLoopOutput,
                exhaustFanStage2Threshold,
                exhaustFanHysteresis,
                logTag = L.TAG_CCU_SYSTEM
            )
            controllers[ControllerNames.EXHAUST_FAN_STAGE2_CONTROLLER] = controller
        } else {
            removeController(ControllerNames.EXHAUST_FAN_STAGE2_CONTROLLER)
        }
    }


    fun addLoadCoolingControllers(
        coolingLoopOutput: Point,
        activationHysteresis: Point,
        stageUpTimerCounter: Point,
        stageDownTimerCounter: Point,
        economizationAvailable: CalibratedPoint,
        coolingLockOutActive: CalibratedPoint,
        coolingStages: CalibratedPoint
    ) {
        if (coolingStages.data > 0) {
            factory.addStageController(
                controllerName = ControllerNames.LOAD_COOLING_STAGE_CONTROLLER,
                controllers,
                loopOutput = coolingLoopOutput,
                totalStages = coolingStages,
                activationHysteresis = activationHysteresis,
                stageUpTimer = stageUpTimerCounter,
                stageDownTimer = stageDownTimerCounter,
                economizingAvailable = economizationAvailable,
                lockOutActive = coolingLockOutActive,
                logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.LOAD_COOLING_STAGE_CONTROLLER)
        }
    }

    fun addLoadHeatingControllers(
        heatingLoopOutput: Point,
        activationHysteresis: Point,
        stageUpTimerCounter: Point,
        stageDownTimerCounter: Point,
        heatingLockOutActive: CalibratedPoint,
        heatingStages: CalibratedPoint
    ) {
        if (heatingStages.data > 0) {
            factory.addStageController(
                controllerName = ControllerNames.LOAD_HEATING_STAGE_CONTROLLER,
                controllers,
                loopOutput = heatingLoopOutput,
                totalStages = heatingStages,
                activationHysteresis = activationHysteresis,
                stageUpTimer = stageUpTimerCounter,
                stageDownTimer = stageDownTimerCounter,
                lockOutActive = heatingLockOutActive,
                logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.LOAD_HEATING_STAGE_CONTROLLER)
        }
    }

    fun addLoadFanControllers(
        heatingLoopOutput: Point,
        activationHysteresis: Point,
        stageUpTimerCounter: Point,
        stageDownTimerCounter: Point,
        fanStages: CalibratedPoint
    ) {
        if (fanStages.data > 0) {
            factory.addStageController(
                controllerName = ControllerNames.LOAD_FAN_STAGE_CONTROLLER,
                controllers,
                loopOutput = heatingLoopOutput,
                totalStages = fanStages,
                activationHysteresis = activationHysteresis,
                stageUpTimer = stageUpTimerCounter,
                stageDownTimer = stageDownTimerCounter,
                logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.LOAD_FAN_STAGE_CONTROLLER)
        }
    }

    fun addSatCoolingControllers(
        coolingLoopOutput: Point,
        activationHysteresis: Point,
        stageUpTimerCounter: Point,
        stageDownTimerCounter: Point,
        economizationAvailable: CalibratedPoint,
        coolingLockoutActive: CalibratedPoint,
        coolingStages: CalibratedPoint
    ) {
        if (coolingStages.data > 0) {
            factory.addStageController(
                controllerName = ControllerNames.SAT_COOLING_STAGE_CONTROLLER,
                controllers,
                loopOutput = coolingLoopOutput,
                totalStages = coolingStages,
                activationHysteresis = activationHysteresis,
                stageUpTimer = stageUpTimerCounter,
                stageDownTimer = stageDownTimerCounter,
                economizingAvailable = economizationAvailable,
                lockOutActive = coolingLockoutActive,
                logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.SAT_COOLING_STAGE_CONTROLLER)
        }
    }

    fun addSatHeatingControllers(
        heatingLoopOutput: Point,
        activationHysteresis: Point,
        stageUpTimerCounter: Point,
        stageDownTimerCounter: Point,
        heatingLockOutActive: CalibratedPoint,
        heatingStages: CalibratedPoint
    ) {
        if (heatingStages.data > 0) {
            factory.addStageController(
                controllerName = ControllerNames.SAT_HEATING_STAGE_CONTROLLER,
                controllers,
                loopOutput = heatingLoopOutput,
                totalStages = heatingStages,
                activationHysteresis = activationHysteresis,
                stageUpTimer = stageUpTimerCounter,
                stageDownTimer = stageDownTimerCounter,
                lockOutActive = heatingLockOutActive,
                logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.SAT_HEATING_STAGE_CONTROLLER)
        }
    }

    fun addPressureFanControllers(
        heatingLoopOutput: Point,
        activationHysteresis: Point,
        stageUpTimerCounter: Point,
        stageDownTimerCounter: Point,
        fanStages: CalibratedPoint
    ) {
        if (fanStages.data > 0) {
            factory.addStageController(
                controllerName = ControllerNames.PRESSURE_FAN_STAGE_CONTROLLER,
                controllers,
                loopOutput = heatingLoopOutput,
                totalStages = fanStages,
                activationHysteresis = activationHysteresis,
                stageUpTimer = stageUpTimerCounter,
                stageDownTimer = stageDownTimerCounter,
                logTag = L.TAG_CCU_SYSTEM
            )
        } else {
            removeController(ControllerNames.PRESSURE_FAN_STAGE_CONTROLLER)
        }
    }

    fun getController(controllerName: String): StageControlHandler? {
        return if (controllers.containsKey(controllerName)) controllers[controllerName] as StageControlHandler
        else {
            return null
        }
    }

    private fun removeController(controllerName: String) {
        if (controllers.containsKey(controllerName)) controllers.remove(controllerName)
    }
}