package a75f.io.logic.bo.building.system

import a75f.io.domain.api.Point
import a75f.io.domain.equips.DomainEquip
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.L
import a75f.io.logic.controlcomponents.controls.ControllerFactory
import a75f.io.logic.controlcomponents.handlers.StageControlHandler
import a75f.io.logic.controlcomponents.util.ControllerNames

/**
 * Created by Manjunath K on 12-05-2025.
 */

class SystemControllerFactory {
    private val factory = ControllerFactory()

    private fun getInCalibratedPointPoint(data: Int): CalibratedPoint {
        return CalibratedPoint(
            "InCalibratedPoint", // dummy name
            "", data.toDouble()
        )
    }

    fun addCoolingControllers(
        equip: DomainEquip,
        coolingLoopOutput: Point,
        activationHysteresis: Point,
        stageUpTimerCounter: Point,
        stageDownTimerCounter: Point,
        economizationAvailable: CalibratedPoint,
        coolingStages: Int
    ) {
        if (coolingStages > 0) {
            factory.addStageController(
                controllerName = ControllerNames.COOLING_STAGE_CONTROLLER,
                equip = equip,
                loopOutput = coolingLoopOutput,
                totalStages = getInCalibratedPointPoint(coolingStages),
                activationHysteresis = activationHysteresis,
                stageUpTimer = stageUpTimerCounter,
                stageDownTimer = stageDownTimerCounter,
                economizingAvailable = economizationAvailable,
                logTag = L.TAG_CCU_SYSTEM
            )
        }
    }

    fun addHeatingControllers(
        equip: DomainEquip,
        heatingLoopOutput: Point,
        activationHysteresis: Point,
        stageUpTimerCounter: Point,
        stageDownTimerCounter: Point,
        heatingStages: Int
    ) {
        if (heatingStages > 0) {
            factory.addStageController(
                controllerName = ControllerNames.HEATING_STAGE_CONTROLLER,
                equip = equip,
                loopOutput = heatingLoopOutput,
                totalStages = getInCalibratedPointPoint(heatingStages),
                activationHysteresis = activationHysteresis,
                stageUpTimer = stageUpTimerCounter,
                stageDownTimer = stageDownTimerCounter,
                logTag = L.TAG_CCU_SYSTEM
            )
        }
    }


    fun addFanControllers(
        equip: DomainEquip,
        heatingLoopOutput: Point,
        activationHysteresis: Point,
        stageUpTimerCounter: Point,
        stageDownTimerCounter: Point,
        fanStages: Int
    ) {
        if (fanStages > 0) {
            factory.addStageController(
                controllerName = ControllerNames.FAN_SPEED_CONTROLLER,
                equip = equip,
                loopOutput = heatingLoopOutput,
                totalStages = getInCalibratedPointPoint(fanStages),
                activationHysteresis = activationHysteresis,
                stageUpTimer = stageUpTimerCounter,
                stageDownTimer = stageDownTimerCounter,
                logTag = L.TAG_CCU_SYSTEM
            )
        }
    }

    fun addCompressorControllers(
        equip: DomainEquip,
        compressorOutput: Point,
        activationHysteresis: Point,
        stageUpTimerCounter: Point,
        stageDownTimerCounter: Point,
        economizationAvailable: CalibratedPoint,
        compressorStages: Int
    ) {
        if (compressorStages > 0) {
            factory.addStageController(
                controllerName = ControllerNames.COMPRESSOR_RELAY_CONTROLLER,
                equip = equip,
                loopOutput = compressorOutput,
                totalStages = getInCalibratedPointPoint(compressorStages),
                activationHysteresis = activationHysteresis,
                stageUpTimer = stageUpTimerCounter,
                stageDownTimer = stageDownTimerCounter,
                economizingAvailable = economizationAvailable,
                logTag = L.TAG_CCU_SYSTEM
            )
        }
    }

    fun addHumidifierController(
        equip: DomainEquip,
        zoneHumidity: Point,
        targetHumidifier: Point,
        activationHysteresis: Point,
        occupancyMode : CalibratedPoint
    ) {
        factory.addHumidifierController(
            equip = equip,
            zoneHumidity = zoneHumidity,
            targetHumidifier = targetHumidifier,
            activationHysteresis = activationHysteresis,
            occupancy = occupancyMode,
            logTag = L.TAG_CCU_SYSTEM
        )
    }

    fun addDeHumidifierController(
        equip: DomainEquip,
        zoneHumidity: Point,
        targetDeHumidifier: Point,
        activationHysteresis: Point,
        occupancyMode: CalibratedPoint
    ) {
        factory.addDeHumidifierController(
            equip = equip,
            zoneHumidity = zoneHumidity,
            targetDehumidifier = targetDeHumidifier,
            activationHysteresis = activationHysteresis,
            occupancy = occupancyMode,
            logTag = L.TAG_CCU_SYSTEM
        )
    }

    fun addChangeCoolingChangeOverRelay(
        equip: DomainEquip, coolingLoopOutput: Point
    ) {
        factory.addChangeOverCoolingController(
            equip = equip, coolingLoopOutput = coolingLoopOutput, logTag = L.TAG_CCU_SYSTEM
        )
    }

    fun addChangeHeatingChangeOverRelay(
        equip: DomainEquip, heatingLoopOutput: Point
    ) {
        factory.addChangeOverHeatingController(
            equip = equip, heatingLoopOutput = heatingLoopOutput, logTag = L.TAG_CCU_SYSTEM
        )
    }

    fun addFanEnableController(
        equip: DomainEquip, fanLoopOutput: Point, occupancy: CalibratedPoint
    ) {
        factory.addFanEnableController(
            equip = equip,
            fanLoopOutput = fanLoopOutput,
            occupancy = occupancy,
            logTag = L.TAG_CCU_SYSTEM
        )
    }

    fun addOccupiedEnabledController(
        equip: DomainEquip, occupancy: CalibratedPoint
    ) {
        factory.addOccupiedEnableController(
            equip = equip, occupancy = occupancy, logTag = L.TAG_CCU_SYSTEM
        )
    }

    fun addDcvDamperController(
        equip: DomainEquip,
        dcvLoopOutput: Point,
        activationHysteresis: Point,
        occupancy: CalibratedPoint
    ) {
        factory.addDcvDamperController(
            equip = equip,
            dcvLoopOutput = dcvLoopOutput,
            actionHysteresis = activationHysteresis,
            currentOccupancy = occupancy,
            logTag = L.TAG_CCU_SYSTEM
        )
    }

    fun getController(controllerName: String, equip: DomainEquip): StageControlHandler? {
        return if (equip.controllers.containsKey(controllerName))
             equip.controllers[controllerName] as StageControlHandler
        else {
            return null
        }
    }
}