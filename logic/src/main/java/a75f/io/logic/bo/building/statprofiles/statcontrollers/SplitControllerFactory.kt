package a75f.io.logic.bo.building.statprofiles.statcontrollers

import a75f.io.domain.equips.hyperstatsplit.HyperStatSplitEquip
import a75f.io.domain.equips.hyperstatsplit.HsSplitCpuEquip
import a75f.io.domain.equips.hyperstatsplit.Pipe2UVEquip
import a75f.io.domain.equips.hyperstatsplit.Pipe4UVEquip
import a75f.io.domain.equips.hyperstatsplit.UnitVentilatorEquip
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.L
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVConfiguration
import a75f.io.logic.bo.building.statprofiles.util.StagesCounts
import a75f.io.logic.bo.building.statprofiles.util.isHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMediumUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isUserIntentFanMode
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.ControllerFactory
import a75f.io.logic.controlcomponents.handlers.ExhaustFanController
import a75f.io.logic.controlcomponents.handlers.StageControlHandler
import a75f.io.logic.controlcomponents.util.ControllerNames

/**
 * Created by Manjunath K on 09-05-2025.
 */
class SplitControllerFactory(
    var equip: HyperStatSplitEquip,
    var tag: String,
    var controllers: HashMap<String, Any>, var counts: StagesCounts,
    private var isEconAvailable: CalibratedPoint,
    var derivedFanLoopOutput: CalibratedPoint,
    var zoneOccupancyState: CalibratedPoint,
) {

    private val controlFactory = ControllerFactory()

    fun addCpuEconControllers(
        config: HyperStatSplitCpuConfiguration, isPrePurgeActive: () -> Boolean, fanLowVentilation: CalibratedPoint) {
        addCoolingController(config)
        addHeatingController(config)
        addFanSpeedController(config, isPrePurgeActive, fanLowVentilation, zoneOccupancyState)
        addCompressorController(config)
        addFanEnabledController()
        addOccupiedEnableController()
        addHumidifierController()
        addDehumidifierController()
        addExhaustFanStage1Controller()
        addExhaustFanStage2Controller()
        addDcvDamperController()
        addChangeOverCoolingController()
        addChangeOverHeatingController()
        addAuxHeatingStage1Controller()
        addAuxHeatingStage2Controller()
    }

    fun addPipe4Controllers(
        config: Pipe4UVConfiguration, isPrePurgeActive: () -> Boolean, fanLowVentilation: CalibratedPoint) {
        addFanSpeedController(config, isPrePurgeActive, fanLowVentilation, zoneOccupancyState)
        addCoolingValveController()
        addHeatingValveController()
        addAuxHeatingStage1Controller()
        addAuxHeatingStage2Controller()
        addFanEnabledController()
        addOccupiedEnableController()
        addFaceBypassController()
        addDcvDamperController()
        addHumidifierController()
        addDehumidifierController()
    }
    fun addPipe2Controllers(
        config: Pipe2UVConfiguration, isPrePurgeActive: () -> Boolean,
        fanLowVentilation: CalibratedPoint, waterValveLoop: CalibratedPoint
    ) {
        addFanSpeedController(config, isPrePurgeActive, fanLowVentilation, zoneOccupancyState)
        addWaterValveController(waterValveLoop)
        addAuxHeatingStage1Controller()
        addAuxHeatingStage2Controller()
        addFanEnabledController()
        addOccupiedEnableController()
        addFaceBypassController()
        addDcvDamperController()
        addHumidifierController()
        addDehumidifierController()
    }

    private fun addCoolingValveController() {
        if ((equip as Pipe4UVEquip).chilledWaterCoolValve.pointExists()) {
            controlFactory.addCoolingValveController(
                controllers,
                equip.coolingLoopOutput,
                offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
                actionHysteresis = equip.standaloneRelayActivationHysteresis,
                logTag = tag
            )
        } else {
            removeController(ControllerNames.COOLING_VALVE_CONTROLLER)
        }
    }

    private fun addHeatingValveController() {
        if ((equip as Pipe4UVEquip).hotWaterHeatValve.pointExists()) {
            controlFactory.addHeatingValveController(
                controllers,
                equip.heatingLoopOutput,
                offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
                actionHysteresis = equip.standaloneRelayActivationHysteresis,
                logTag = tag
            )
        } else {
            removeController(ControllerNames.HEATING_VALVE_CONTROLLER)
        }
    }

    private fun addWaterValveController(waterValveLoop: CalibratedPoint) {
        if ((equip as Pipe2UVEquip).waterValve.pointExists()) {
            controlFactory.addWaterValveController(
                controllers,
                waterValveLoop = waterValveLoop,
                actionHysteresis = equip.standaloneRelayActivationHysteresis,
                logTag = L.TAG_CCU_HSSPLIT_PIPE2_UV
            )
        } else {
            removeController(ControllerNames.WATER_VALVE_CONTROLLER)
        }
    }

    private fun addFaceBypassController() {
        if ((equip as UnitVentilatorEquip).faceBypassDamperCmd.pointExists()) {
            controlFactory.addFaceBypassController(
                controllers,
                equip.coolingLoopOutput,
                equip.heatingLoopOutput,
                equip.faceBypassDamperRelayActivationHysteresis,
                offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
                logTag = tag
            )
        } else {
            removeController(ControllerNames.FACE_BYPASS_CONTROLLER)
        }
    }

    private fun addHeatingController(config: HyperStatSplitCpuConfiguration) {
        counts.heatingStages.data = config.getHighestHeatingStageCount().toDouble()
        if (counts.heatingStages.data > 0) {
            val totalStages = counts.heatingStages.data.toInt()
            val additionalOffConstraints = mutableMapOf<Int, Constraint>()
            if (totalStages >= 1) {
                additionalOffConstraints[0] = Constraint { equip.isCondensateTripped() }
            }
            if (totalStages >= 2) {
                additionalOffConstraints[1] = Constraint { equip.isCondensateTripped() }
            }
            if (totalStages >= 3) {
                additionalOffConstraints[2] = Constraint { equip.isCondensateTripped() }
            }

            controlFactory.addStageController(
                ControllerNames.HEATING_STAGE_CONTROLLER,
                controllers,
                loopOutput = equip.heatingLoopOutput,
                activationHysteresis = equip.standaloneRelayActivationHysteresis,
                totalStages = counts.heatingStages,
                offConstrains = additionalOffConstraints,
                stageDownTimer = equip.hyperstatStageDownTimerCounter,
                stageUpTimer = equip.hyperstatStageUpTimerCounter,
                logTag = tag
            )
        } else {
            removeController(ControllerNames.HEATING_STAGE_CONTROLLER)
        }
    }

    private fun addCoolingController(config: HyperStatSplitCpuConfiguration) {
        counts.coolingStages.data = config.getHighestCoolingStageCount().toDouble()
        if (counts.coolingStages.data > 0) {
            val totalStages = counts.coolingStages.data.toInt()
            val additionalOffConstraints = mutableMapOf<Int, Constraint>()
            if (totalStages >= 1) {
                additionalOffConstraints[0] = Constraint { equip.isCondensateTripped() }
            }
            if (totalStages >= 2) {
                additionalOffConstraints[1] = Constraint { equip.isCondensateTripped() }
            }
            if (totalStages >= 3) {
                additionalOffConstraints[2] = Constraint { equip.isCondensateTripped() }
            }

            controlFactory.addStageController(
                ControllerNames.COOLING_STAGE_CONTROLLER,
                controllers,
                loopOutput = equip.coolingLoopOutput,
                activationHysteresis = equip.standaloneRelayActivationHysteresis,
                totalStages = counts.coolingStages,
                offConstrains = additionalOffConstraints,
                economizingAvailable = isEconAvailable,
                stageDownTimer = equip.hyperstatStageDownTimerCounter,
                stageUpTimer = equip.hyperstatStageUpTimerCounter,
                logTag = tag
            )
        } else {
            removeController(ControllerNames.COOLING_STAGE_CONTROLLER)
        }
    }

    private fun addFanSpeedController(
        config: HyperStatSplitConfiguration, isPrePurgeActive: () -> Boolean, fanLowVentilation: CalibratedPoint,  occupancyMode: CalibratedPoint
    ) {
        counts.fanStages.data = config.getHighestFanStageCount().toDouble()
        if (counts.fanStages.data > 0) {
            val totalStages = counts.fanStages.data
            val additionalOffConstraints = mutableMapOf<Int, Constraint>()
            val additionalOnConstraints = mutableMapOf<Int, Constraint>()

            if (totalStages >= 1) {
                additionalOnConstraints[0] =
                    Constraint { isUserIntentFanMode(equip.fanOpMode) || isPrePurgeActive() }
                additionalOffConstraints[0] =
                    Constraint { equip.isCondensateTripped() || !isUserIntentFanMode(equip.fanOpMode) }
            }
            if (totalStages >= 2) {
                additionalOnConstraints[1] = Constraint {
                    isMediumUserIntentFanMode(equip.fanOpMode) || isHighUserIntentFanMode(equip.fanOpMode) || isPrePurgeActive()
                }
                additionalOffConstraints[1] = Constraint {
                    equip.isCondensateTripped() || (!isMediumUserIntentFanMode(equip.fanOpMode) && !isHighUserIntentFanMode(
                        equip.fanOpMode
                    ))
                }
            }
            if (totalStages >= 3) {
                additionalOnConstraints[2] =
                    Constraint { isHighUserIntentFanMode(equip.fanOpMode) || isPrePurgeActive() }
                additionalOffConstraints[2] =
                    Constraint { equip.isCondensateTripped() || !isHighUserIntentFanMode(equip.fanOpMode) }
            }

            controlFactory.addStageController(
                ControllerNames.FAN_SPEED_CONTROLLER,
                controllers,
                loopOutput = derivedFanLoopOutput,
                activationHysteresis = equip.standaloneRelayActivationHysteresis,
                totalStages = counts.fanStages,
                onConstrains = additionalOnConstraints,
                offConstrains = additionalOffConstraints,
                stageDownTimer = equip.hyperstatStageDownTimerCounter,
                stageUpTimer = equip.hyperstatStageUpTimerCounter,
                fanLowVentilation = fanLowVentilation,
                occupancyPoint = occupancyMode,
                logTag = tag
            )
        } else {
            removeController(ControllerNames.FAN_SPEED_CONTROLLER)
        }
    }

    private fun addCompressorController(config: HyperStatSplitCpuConfiguration) {
        counts.compressorStages.data = config.getHighestCompressorStageCount().toDouble()
        val totalStages = counts.compressorStages.data
        if (totalStages > 0) {

            val additionalOffConstraints = mutableMapOf<Int, Constraint>()
            if (totalStages >= 1) {
                additionalOffConstraints[0] = Constraint { equip.isCondensateTripped() }
            }
            if (totalStages >= 2) {
                additionalOffConstraints[1] = Constraint { equip.isCondensateTripped() }
            }
            if (totalStages >= 3) {
                additionalOffConstraints[2] = Constraint { equip.isCondensateTripped() }
            }
            controlFactory.addStageController(
                ControllerNames.COMPRESSOR_RELAY_CONTROLLER,
                controllers,
                loopOutput = equip.compressorLoopOutput,
                activationHysteresis = equip.standaloneRelayActivationHysteresis,
                totalStages = counts.compressorStages,
                offConstrains = additionalOffConstraints,
                economizingAvailable = isEconAvailable,
                stageDownTimer = equip.hyperstatStageDownTimerCounter,
                stageUpTimer = equip.hyperstatStageUpTimerCounter,
                logTag = tag
            )
        } else {
            removeController(ControllerNames.COMPRESSOR_RELAY_CONTROLLER)
        }
    }

    private fun addFanEnabledController() {
        if (equip.fanEnable.pointExists()) {
            controlFactory.addFanEnableController(
                controllers,
                fanLoopOutput = equip.fanLoopOutput,
                occupancy = zoneOccupancyState,
                offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
                logTag = tag
            )
        } else {
            removeController(ControllerNames.FAN_ENABLED)
        }
    }

    private fun addOccupiedEnableController() {
        if (equip.occupiedEnable.pointExists()) {
            controlFactory.addOccupiedEnableController(
                controllers, zoneOccupancyState, logTag = tag
            )
        } else {
            removeController(ControllerNames.OCCUPIED_ENABLED)
        }
    }

    private fun addHumidifierController() {
        if (equip.humidifierEnable.pointExists()) {
            controlFactory.addHumidifierController(
                controllers,
                zoneHumidity = equip.zoneHumidity,
                targetHumidifier = equip.targetHumidifier,
                activationHysteresis = equip.standaloneHumidityHysteresis,
                occupancy = zoneOccupancyState,
                offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
                logTag = tag
            )
        } else {
            removeController(ControllerNames.HUMIDIFIER_CONTROLLER)
        }
    }

    private fun addDehumidifierController() {
        if (equip.dehumidifierEnable.pointExists()) {
            controlFactory.addDeHumidifierController(
                controllers,
                zoneHumidity = equip.zoneHumidity,
                targetDehumidifier = equip.targetDehumidifier,
                activationHysteresis = equip.standaloneHumidityHysteresis,
                offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
                occupancy = zoneOccupancyState,
                logTag = tag
            )
        } else {
            removeController(ControllerNames.DEHUMIDIFIER_CONTROLLER)
        }
    }

    private fun addExhaustFanStage1Controller() {
        if ((equip as HsSplitCpuEquip).exhaustFanStage1.pointExists()) {
            if (controllers.containsKey(ControllerNames.EXHAUST_FAN_STAGE1_CONTROLLER)) return

            val controller = ExhaustFanController(
                equip.outsideAirFinalLoopOutput,
                equip.exhaustFanStage1Threshold,
                equip.exhaustFanHysteresis,
                logTag = tag
            )
            controller.addOffConstraint(Constraint { equip.isCondensateTripped() })
            controllers[ControllerNames.EXHAUST_FAN_STAGE1_CONTROLLER] = controller
        } else {
            removeController(ControllerNames.EXHAUST_FAN_STAGE1_CONTROLLER)
        }
    }

    private fun addExhaustFanStage2Controller() {
        if ((equip as HsSplitCpuEquip).exhaustFanStage2.pointExists()) {
            if (controllers.containsKey(ControllerNames.EXHAUST_FAN_STAGE2_CONTROLLER)) return

            val controller = ExhaustFanController(
                equip.outsideAirFinalLoopOutput,
                equip.exhaustFanStage2Threshold,
                equip.exhaustFanHysteresis,
                logTag = tag
            )
            controller.addOffConstraint(Constraint { equip.isCondensateTripped() })
            controllers[ControllerNames.EXHAUST_FAN_STAGE2_CONTROLLER] = controller
        } else {
            removeController(ControllerNames.EXHAUST_FAN_STAGE2_CONTROLLER)
        }
    }

    private fun addDcvDamperController() {
        if (equip.dcvDamper.pointExists()) {
            controlFactory.addDcvDamperController(
                controllers,
                dcvLoopOutput = equip.dcvLoopOutput,
                actionHysteresis = equip.standaloneRelayActivationHysteresis,
                currentOccupancy = zoneOccupancyState,
                offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
                logTag = tag
            )
        } else {
            removeController(ControllerNames.DAMPER_RELAY_CONTROLLER)
        }
    }

    private fun addChangeOverCoolingController() {
        if ((equip as HsSplitCpuEquip).changeOverCooling.pointExists()) {
            controlFactory.addChangeOverCoolingController(
                controllers,
                coolingLoopOutput = equip.coolingLoopOutput,
                offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
                logTag = tag
            )
        } else {
            removeController(ControllerNames.CHANGE_OVER_O_COOLING)
        }
    }

    private fun addChangeOverHeatingController() {
        if ((equip as HsSplitCpuEquip).changeOverHeating.pointExists()) {
            controlFactory.addChangeOverHeatingController(
                controllers,
                heatingLoopOutput = equip.heatingLoopOutput,
                offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
                logTag = tag
            )
        } else {
            removeController(ControllerNames.CHANGE_OVER_B_HEATING)
        }
    }

    private fun addAuxHeatingStage1Controller() {
        if (equip.auxHeatingStage1.pointExists()) {
            controlFactory.addAuxHeatingStage1Controller(
                controllers,
                currentTemp = equip.currentTemp,
                desiredTempHeating = equip.desiredTempHeating,
                auxHeating1Activate = equip.auxHeating1Activate,
                offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
                logTag = tag
            )
        } else {
            removeController(ControllerNames.AUX_HEATING_STAGE1)
        }
    }

    private fun addAuxHeatingStage2Controller() {
        if (equip.auxHeatingStage2.pointExists()) {
            controlFactory.addAuxHeatingStage2Controller(
                controllers,
                currentTemp = equip.currentTemp,
                desiredTempHeating = equip.desiredTempHeating,
                auxHeating2Activate = equip.auxHeating2Activate,
                offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
                logTag = tag
            )
        } else {
            removeController(ControllerNames.AUX_HEATING_STAGE2)
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