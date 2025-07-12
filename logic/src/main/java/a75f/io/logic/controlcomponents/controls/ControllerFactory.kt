package a75f.io.logic.controlcomponents.controls

import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.controlcomponents.handlers.AuxHeatingController
import a75f.io.logic.controlcomponents.handlers.DcvDamperController
import a75f.io.logic.controlcomponents.handlers.DehumidifierController
import a75f.io.logic.controlcomponents.handlers.EnableController
import a75f.io.logic.controlcomponents.handlers.FanEnableController
import a75f.io.logic.controlcomponents.handlers.FanRunCommandController
import a75f.io.logic.controlcomponents.handlers.HumidifierController
import a75f.io.logic.controlcomponents.handlers.OccupiedEnabledController
import a75f.io.logic.controlcomponents.handlers.StageControlHandler
import a75f.io.logic.controlcomponents.handlers.WaterValveController
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.controlcomponents.util.logIt

/**
 * Created by Manjunath K on 07-05-2025.
 */

open class ControllerFactory {

    fun addStageController(
        controllerName: String,
        controllers: HashMap<String, Any>,
        loopOutput: Point,
        totalStages: CalibratedPoint,
        activationHysteresis: Point,
        onConstrains: Map<Int, Constraint> = emptyMap(),
        offConstrains: Map<Int, Constraint> = emptyMap(),
        stageUpTimer: Point = Point("stageUpTimer", ""),
        stageDownTimer: Point = Point("stageUpTimer", ""),
        economizingAvailable: CalibratedPoint = CalibratedPoint("economizingAvailable", "", 0.0),
        logTag: String
    ): StageControlHandler {
        if (!controllers.containsKey(controllerName)) {
            val stageController = StageControlHandler(
                controllerName = controllerName,
                loopOutput = loopOutput,
                totalStages = totalStages,
                hysteresis = activationHysteresis,
                stageUpTimer = stageUpTimer,
                stageDownTimer = stageDownTimer,
                economizingAvailable = economizingAvailable,
                logTag = logTag
            )
            controllers[controllerName] = stageController
            addConstraintsIfExist(stageController, onConstrains, offConstrains)
            logIt(logTag, "Controller added with name: $controllerName")
        }
        return controllers[controllerName] as StageControlHandler
    }

    fun addFanEnableController(
        controllers: HashMap<String, Any>,
        fanLoopOutput: Point,
        occupancy: CalibratedPoint,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): FanEnableController {
        if (!controllers.containsKey(ControllerNames.FAN_ENABLED)) {
            val controller = FanEnableController(
                fanLoopPoint = fanLoopOutput, occupancy = occupancy, logTag = logTag
            )
            if (onConstrains.isNotEmpty()) {
                onConstrains.forEach { constraint ->
                    controller.addOnConstraint(constraint)
                }
            }
            if (offConstrains.isNotEmpty()) {
                offConstrains.forEach { constraint ->
                    controller.addOffConstraint(constraint)
                }
            }
            controllers[ControllerNames.FAN_ENABLED] = controller
        }
        return controllers[ControllerNames.FAN_ENABLED] as FanEnableController
    }


    fun addOccupiedEnableController(
        controllers: HashMap<String, Any>,
        occupancy: CalibratedPoint,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): OccupiedEnabledController {
        if (!controllers.containsKey(ControllerNames.OCCUPIED_ENABLED)) {
            val controller = OccupiedEnabledController(occupancy = occupancy, logTag)
            if (onConstrains.isNotEmpty()) {
                onConstrains.forEach { constraint ->
                    controller.addOnConstraint(constraint)
                }
            }
            if (offConstrains.isNotEmpty()) {
                offConstrains.forEach { constraint ->
                    controller.addOffConstraint(constraint)
                }
            }
            controllers[ControllerNames.OCCUPIED_ENABLED] = controller
            logIt(logTag, "Controller added with name: OccupiedEnabledController")
        }
        return controllers[ControllerNames.OCCUPIED_ENABLED] as OccupiedEnabledController
    }


    fun addHumidifierController(
        controllers: HashMap<String, Any>,
        zoneHumidity: Point,
        targetHumidifier: Point,
        activationHysteresis: Point,
        onConstrains: List<Constraint> = emptyList(), // additional constrains if required
        offConstrains: List<Constraint> = emptyList(), /// additional constrains if required
        occupancy: CalibratedPoint,
        logTag: String
    ): HumidifierController {
        if (!controllers.containsKey(ControllerNames.HUMIDIFIER_CONTROLLER)) {
            val controller =
                HumidifierController(zoneHumidity, targetHumidifier, activationHysteresis, logTag, occupancy = occupancy)
            if (onConstrains.isNotEmpty()) {
                onConstrains.forEach { constraint ->
                    controller.addOnConstraint(constraint)
                }
            }
            if (offConstrains.isNotEmpty()) {
                offConstrains.forEach { constraint ->
                    controller.addOffConstraint(constraint)
                }
            }
            controllers[ControllerNames.HUMIDIFIER_CONTROLLER] = controller
            logIt(logTag, "Controller added with name: HumidifierController")
        }
        return controllers[ControllerNames.HUMIDIFIER_CONTROLLER] as HumidifierController
    }

    fun addDeHumidifierController(
        controllers: HashMap<String, Any>,
        zoneHumidity: Point,
        targetDehumidifier: Point,
        activationHysteresis: Point,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        occupancy: CalibratedPoint,
        logTag: String
    ): DehumidifierController {
        if (!controllers.containsKey(ControllerNames.DEHUMIDIFIER_CONTROLLER)) {
            val controller = DehumidifierController(
                zoneHumidity,
                targetDehumidifier,
                activationHysteresis,
                logTag,
                occupancy
            )
            if (onConstrains.isNotEmpty()) {
                onConstrains.forEach { constraint ->
                    controller.addOnConstraint(constraint)
                }
            }
            if (offConstrains.isNotEmpty()) {
                offConstrains.forEach { constraint ->
                    controller.addOffConstraint(constraint)
                }
            }
            controllers[ControllerNames.DEHUMIDIFIER_CONTROLLER] = controller
            logIt(logTag, "Controller added with name: DeHumidifierController")
        }
        return controllers[ControllerNames.DEHUMIDIFIER_CONTROLLER] as DehumidifierController
    }


    fun addDcvDamperController(
        controllers: HashMap<String, Any>,
        dcvLoopOutput: Point,
        actionHysteresis: Point,
        currentOccupancy: CalibratedPoint,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): DcvDamperController {
        if (!controllers.containsKey(ControllerNames.DAMPER_RELAY_CONTROLLER)) {
            val controller =
                DcvDamperController(dcvLoopOutput, actionHysteresis, currentOccupancy, logTag)
            if (onConstrains.isNotEmpty()) {
                onConstrains.forEach { constraint ->
                    controller.addOnConstraint(constraint)
                }
            }
            if (offConstrains.isNotEmpty()) {
                offConstrains.forEach { constraint ->
                    controller.addOffConstraint(constraint)
                }
            }
            controllers[ControllerNames.DAMPER_RELAY_CONTROLLER] = controller
            logIt(logTag, "Controller added with name: DcvDamperController")
        }
        return controllers[ControllerNames.DAMPER_RELAY_CONTROLLER] as DcvDamperController
    }

    fun addChangeOverCoolingController(
        controllers: HashMap<String, Any>,
        coolingLoopOutput: Point,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): EnableController {
        if (!controllers.containsKey(ControllerNames.CHANGE_OVER_O_COOLING)) {
            val controller = EnableController(
                ControllerNames.CHANGE_OVER_O_COOLING, coolingLoopOutput, logTag
            )
            if (onConstrains.isNotEmpty()) {
                onConstrains.forEach { constraint ->
                    controller.addOnConstraint(constraint)
                }
            }
            if (offConstrains.isNotEmpty()) {
                offConstrains.forEach { constraint ->
                    controller.addOffConstraint(constraint)
                }
            }
            controllers[ControllerNames.CHANGE_OVER_O_COOLING] = controller
            logIt(logTag, "Controller added with name: ChangeOverCoolingController")
        }
        return controllers[ControllerNames.CHANGE_OVER_O_COOLING] as EnableController

    }

    fun addChangeOverHeatingController(
        controllers: HashMap<String, Any>,
        heatingLoopOutput: Point,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): EnableController {
        if (!controllers.containsKey(ControllerNames.CHANGE_OVER_B_HEATING)) {
            val controller = EnableController(
                ControllerNames.CHANGE_OVER_B_HEATING, heatingLoopOutput, logTag
            )
            if (onConstrains.isNotEmpty()) {
                onConstrains.forEach { constraint ->
                    controller.addOnConstraint(constraint)
                }
            }
            if (offConstrains.isNotEmpty()) {
                offConstrains.forEach { constraint ->
                    controller.addOffConstraint(constraint)
                }
            }
            controllers[ControllerNames.CHANGE_OVER_B_HEATING] = controller
            logIt(logTag, "Controller added with name: ChangeOverHeatingController")
        }
        return controllers[ControllerNames.CHANGE_OVER_B_HEATING] as EnableController
    }


    fun addAuxHeatingStage1Controller(
        controllers: HashMap<String, Any>,
        currentTemp: Point,
        desiredTempHeating: Point,
        auxHeating1Activate: Point,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): AuxHeatingController {
        if (!controllers.containsKey(ControllerNames.AUX_HEATING_STAGE1)) {

            val controller = AuxHeatingController(
                ControllerNames.AUX_HEATING_STAGE1,
                currentTemp,
                desiredTempHeating,
                auxHeating1Activate,
                logTag
            )
            if (onConstrains.isNotEmpty()) {
                onConstrains.forEach { constraint ->
                    controller.addOnConstraint(constraint)
                }
            }
            if (offConstrains.isNotEmpty()) {
                offConstrains.forEach { constraint ->
                    controller.addOffConstraint(constraint)
                }
            }
            controllers[ControllerNames.AUX_HEATING_STAGE1] = controller
            logIt(logTag, "Controller added with name: AuxHeatingStage1Controller")
        }
        return controllers[ControllerNames.AUX_HEATING_STAGE1] as AuxHeatingController
    }

    fun addAuxHeatingStage2Controller(
        controllers: HashMap<String, Any>,
        currentTemp: Point,
        desiredTempHeating: Point,
        auxHeating2Activate: Point,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): AuxHeatingController {
        if (!controllers.containsKey(ControllerNames.AUX_HEATING_STAGE2)) {
            val controller = AuxHeatingController(
                ControllerNames.AUX_HEATING_STAGE2,
                currentTemp,
                desiredTempHeating,
                auxHeating2Activate,
                logTag
            )
            if (onConstrains.isNotEmpty()) {
                onConstrains.forEach { constraint ->
                    controller.addOnConstraint(constraint)
                }
            }
            if (offConstrains.isNotEmpty()) {
                offConstrains.forEach { constraint ->
                    controller.addOffConstraint(constraint)
                }
            }
            controllers[ControllerNames.AUX_HEATING_STAGE2] = controller
            logIt(logTag, "Controller added with name: AuxHeatingStage2Controller")
        }
        return controllers[ControllerNames.AUX_HEATING_STAGE2] as AuxHeatingController
    }

    fun addWaterValveController(
        controllers: HashMap<String, Any>,
        waterValveLoop: CalibratedPoint,
        actionHysteresis: Point,
        logTag: String
    ): WaterValveController {
        if (!controllers.containsKey(ControllerNames.WATER_VALVE_CONTROLLER)) {
            val controller = WaterValveController(
                ControllerNames.WATER_VALVE_CONTROLLER,
                waterValveLoop,
                actionHysteresis,
                logTag
            )
            controllers[ControllerNames.WATER_VALVE_CONTROLLER] = controller
            logIt(logTag, "Controller added with name: WaterValveController")
        }
        return controllers[ControllerNames.WATER_VALVE_CONTROLLER] as WaterValveController
    }

    fun addFanRunCommandController(
        controllers: HashMap<String, Any>,
        systemCo2Loop: Point,
        occupancy: CalibratedPoint,
        onConstrains: Map<Int, Constraint> = emptyMap(),
        offConstrains: Map<Int, Constraint> = emptyMap(),
        logTag: String
    ): FanRunCommandController {
        if (!controllers.containsKey(ControllerNames.FAN_RUN_COMMAND_CONTROLLER)) {
            val controller = FanRunCommandController(
                isSystemOccupied = occupancy,
                systemCo2Loop = systemCo2Loop,
                logTag = logTag
            )
            addConstraintsIfExist(controller, onConstrains, offConstrains)
            controllers[ControllerNames.FAN_RUN_COMMAND_CONTROLLER] = controller
            logIt(logTag, "Controller added with name: FanRunCommandController")
        }
        return controllers[ControllerNames.FAN_RUN_COMMAND_CONTROLLER] as FanRunCommandController
    }


    private fun addConstraintsIfExist(
        controller: Controller,
        onConstrains: Map<Int, Constraint>,
        offConstrains: Map<Int, Constraint>
    ) {
        if (controller is StageControlHandler) {
            if (onConstrains.isNotEmpty()) {
                onConstrains.forEach { (stage, constraint) ->
                    controller.addOnConstraint(stage, constraint)
                }
            }
            if (offConstrains.isNotEmpty()) {
                offConstrains.forEach { (stage, constraint) ->
                    controller.addOffConstraint(stage, constraint)
                }
            }
        }
    }

}