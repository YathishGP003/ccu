package a75f.io.logic.controlcomponents.controls

import a75f.io.domain.api.Point
import a75f.io.domain.equips.DomainEquip
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.controlcomponents.handlers.AuxHeatingController
import a75f.io.logic.controlcomponents.handlers.DcvDamperController
import a75f.io.logic.controlcomponents.handlers.EnableController
import a75f.io.logic.controlcomponents.handlers.FanEnableController
import a75f.io.logic.controlcomponents.handlers.HumidifierController
import a75f.io.logic.controlcomponents.handlers.OccupiedEnabledController
import a75f.io.logic.controlcomponents.handlers.StageControlHandler
import a75f.io.logic.controlcomponents.handlers.ThresholdRelayController
import a75f.io.logic.controlcomponents.handlers.WaterValveController
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.controlcomponents.util.logIt

/**
 * Created by Manjunath K on 07-05-2025.
 */

open class ControllerFactory {

    
    fun addStageController(
        controllerName: String,
        equip: DomainEquip,
        loopOutput: Point,
        totalStages: CalibratedPoint,
        activationHysteresis: Point,
        onConstrains: Map<Int, Constraint> = emptyMap(),
        offConstrains: Map<Int, Constraint> = emptyMap(),
        stageUpTimer: CalibratedPoint = CalibratedPoint("StageUpTimer", "", 0.0),  // Some time these counter will be optional
        stageDownTimer: CalibratedPoint = CalibratedPoint("StageDownTimer", "", 0.0),
        economizingAvailable: CalibratedPoint = CalibratedPoint("economizingAvailable", "", 0.0),
        logTag: String
    ): StageControlHandler {
        if (!equip.controllers.containsKey(controllerName)) {
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
            equip.controllers[controllerName] = stageController
            addConstraintsIfExist(stageController, onConstrains, offConstrains)
            logIt(logTag, "Controller added with name: $controllerName")
        }
        return equip.controllers[controllerName] as StageControlHandler
    }

    fun addFanEnableController(
        equip: DomainEquip,
        fanLoopOutput: Point,
        occupancy: CalibratedPoint,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): FanEnableController {
        if (!equip.controllers.containsKey(ControllerNames.FAN_ENABLED)) {
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
            equip.controllers[ControllerNames.FAN_ENABLED] = controller
        }
        return equip.controllers[ControllerNames.FAN_ENABLED] as FanEnableController
    }


    fun addOccupiedEnableController(
        equip: DomainEquip,
        occupancy: CalibratedPoint,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): OccupiedEnabledController {
        if (!equip.controllers.containsKey(ControllerNames.OCCUPIED_ENABLED)) {
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
            equip.controllers[ControllerNames.OCCUPIED_ENABLED] = controller
            logIt(logTag, "Controller added with name: OccupiedEnabledController")
        }
        return equip.controllers[ControllerNames.OCCUPIED_ENABLED] as OccupiedEnabledController
    }


    fun addHumidifierController(
        equip: DomainEquip,
        zoneHumidity: Point,
        targetHumidifier: Point,
        activationHysteresis: Point,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): HumidifierController {
        if (!equip.controllers.containsKey(ControllerNames.HUMIDIFIER_CONTROLLER)) {
            val controller =
                HumidifierController(zoneHumidity, targetHumidifier, activationHysteresis, logTag)
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
            equip.controllers[ControllerNames.HUMIDIFIER_CONTROLLER] = controller
            logIt(logTag, "Controller added with name: HumidifierController")
        }
        return equip.controllers[ControllerNames.HUMIDIFIER_CONTROLLER] as HumidifierController
    }

    fun addDeHumidifierController(
        equip: DomainEquip,
        zoneHumidity: Point,
        targetDehumidifier: Point,
        activationHysteresis: Point,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): ThresholdRelayController {
        if (!equip.controllers.containsKey(ControllerNames.DEHUMIDIFIER_CONTROLLER)) {
            val controller = ThresholdRelayController(
                zoneHumidity,
                targetDehumidifier,
                activationHysteresis,
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
            equip.controllers[ControllerNames.DEHUMIDIFIER_CONTROLLER] = controller
            logIt(logTag, "Controller added with name: DeHumidifierController")
        }
        return equip.controllers[ControllerNames.DEHUMIDIFIER_CONTROLLER] as ThresholdRelayController
    }


    fun addDcvDamperController(
        equip: DomainEquip,
        dcvLoopOutput: Point,
        actionHysteresis: Point,
        currentOccupancy: CalibratedPoint,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): DcvDamperController {
        if (!equip.controllers.containsKey(ControllerNames.DAMPER_RELAY_CONTROLLER)) {
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
            equip.controllers[ControllerNames.DAMPER_RELAY_CONTROLLER] = controller
            logIt(logTag, "Controller added with name: DcvDamperController")
        }
        return equip.controllers[ControllerNames.DAMPER_RELAY_CONTROLLER] as DcvDamperController
    }

    fun addChangeOverCoolingController(
        equip: DomainEquip,
        coolingLoopOutput: Point,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): EnableController {
        if (!equip.controllers.containsKey(ControllerNames.CHANGE_OVER_O_COOLING)) {
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
            equip.controllers[ControllerNames.CHANGE_OVER_O_COOLING] = controller
            logIt(logTag, "Controller added with name: ChangeOverCoolingController")
        }
        return equip.controllers[ControllerNames.CHANGE_OVER_O_COOLING] as EnableController

    }

    fun addChangeOverHeatingController(
        equip: DomainEquip,
        heatingLoopOutput: Point,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): EnableController {
        if (!equip.controllers.containsKey(ControllerNames.CHANGE_OVER_B_HEATING)) {
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
            equip.controllers[ControllerNames.CHANGE_OVER_B_HEATING] = controller
            logIt(logTag, "Controller added with name: ChangeOverHeatingController")
        }
        return equip.controllers[ControllerNames.CHANGE_OVER_B_HEATING] as EnableController
    }


    fun addAuxHeatingStage1Controller(
        equip: DomainEquip,
        currentTemp: Point,
        desiredTempHeating: Point,
        auxHeating1Activate: Point,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): AuxHeatingController {
        if (!equip.controllers.containsKey(ControllerNames.AUX_HEATING_STAGE1)) {

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
            equip.controllers[ControllerNames.AUX_HEATING_STAGE1] = controller
            logIt(logTag, "Controller added with name: AuxHeatingStage1Controller")
        }
        return equip.controllers[ControllerNames.AUX_HEATING_STAGE1] as AuxHeatingController
    }

    fun addAuxHeatingStage2Controller(
        equip: DomainEquip,
        currentTemp: Point,
        desiredTempHeating: Point,
        auxHeating2Activate: Point,
        onConstrains: List<Constraint> = emptyList(),
        offConstrains: List<Constraint> = emptyList(),
        logTag: String
    ): AuxHeatingController {
        if (!equip.controllers.containsKey(ControllerNames.AUX_HEATING_STAGE2)) {
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
            equip.controllers[ControllerNames.AUX_HEATING_STAGE2] = controller
            logIt(logTag, "Controller added with name: AuxHeatingStage2Controller")
        }
        return equip.controllers[ControllerNames.AUX_HEATING_STAGE2] as AuxHeatingController
    }

    fun addWaterValveController(
        equip: DomainEquip,
        waterValveLoop: CalibratedPoint,
        actionHysteresis: Point,
        logTag: String
    ): WaterValveController {
        if (!equip.controllers.containsKey(ControllerNames.WATER_VALVE_CONTROLLER)) {
            val controller = WaterValveController(
                ControllerNames.WATER_VALVE_CONTROLLER,
                waterValveLoop,
                actionHysteresis,
                logTag
            )
            equip.controllers[ControllerNames.WATER_VALVE_CONTROLLER] = controller
            logIt(logTag, "Controller added with name: WaterValveController")
        }
        return equip.controllers[ControllerNames.WATER_VALVE_CONTROLLER] as WaterValveController
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