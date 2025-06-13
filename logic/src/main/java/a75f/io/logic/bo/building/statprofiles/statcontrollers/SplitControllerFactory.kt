package a75f.io.logic.bo.building.statprofiles.statcontrollers

import a75f.io.domain.HyperStatSplitEquip
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuRelayType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.util.isHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMediumUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isUserIntentFanMode
import a75f.io.logic.controlcomponents.controls.Constraint
import a75f.io.logic.controlcomponents.controls.ControllerFactory
import a75f.io.logic.controlcomponents.handlers.ExhaustFanController
import a75f.io.logic.controlcomponents.util.ControllerNames

/**
 * Created by Manjunath K on 09-05-2025.
 */
class SplitControllerFactory(
    var equip: HyperStatSplitEquip, private val zoneOccupancyState: CalibratedPoint
) {
    private val controlFactory = ControllerFactory()

    fun addControllers(config: HyperStatSplitCpuConfiguration, isPrePurgeActive: () -> Boolean) {
        val mappings = config.getRelayConfigurationMapping()
        mappings.forEach { (enabled, association, _) ->
            if (!enabled) return@forEach

            when (CpuRelayType.values()[association]) {
                CpuRelayType.COOLING_STAGE1, CpuRelayType.COOLING_STAGE2, CpuRelayType.COOLING_STAGE3 -> addCoolingController(
                    config
                )

                CpuRelayType.HEATING_STAGE1, CpuRelayType.HEATING_STAGE2, CpuRelayType.HEATING_STAGE3 -> addHeatingController(
                    config
                )

                CpuRelayType.FAN_LOW_SPEED, CpuRelayType.FAN_MEDIUM_SPEED, CpuRelayType.FAN_HIGH_SPEED -> addFanSpeedController(
                    config,
                    isPrePurgeActive
                )

                CpuRelayType.COMPRESSOR_STAGE1, CpuRelayType.COMPRESSOR_STAGE2, CpuRelayType.COMPRESSOR_STAGE3 -> addCompressorController(
                    config
                )

                CpuRelayType.FAN_ENABLED -> addFanEnabledController()
                CpuRelayType.OCCUPIED_ENABLED -> addOccupiedEnableController()
                CpuRelayType.HUMIDIFIER -> addHumidifierController()
                CpuRelayType.DEHUMIDIFIER -> addDehumidifierController()
                CpuRelayType.EX_FAN_STAGE1 -> addExhaustFanStage1Controller()
                CpuRelayType.EX_FAN_STAGE2 -> addExhaustFanStage2Controller()
                CpuRelayType.DCV_DAMPER -> addDcvDamperController()
                CpuRelayType.CHANGE_OVER_O_COOLING -> addChangeOverCoolingController()
                CpuRelayType.CHANGE_OVER_B_HEATING -> addChangeOverHeatingController()
                CpuRelayType.AUX_HEATING_STAGE1 -> addAuxHeatingStage1Controller()
                CpuRelayType.AUX_HEATING_STAGE2 -> addAuxHeatingStage2Controller()
                CpuRelayType.EXTERNALLY_MAPPED -> {}
            }
        }

        equip.controllers.keys.forEach {
            CcuLog.d(
                L.TAG_CCU_HSSPLIT_CPUECON, "available Controller $it: ${equip.controllers[it]}"
            )
        }
    }

    private fun addHeatingController(config: HyperStatSplitCpuConfiguration) {
        val totalStages = config.getHighestHeatingStageCount()
        if (totalStages == 0) return

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
            equip = equip,
            loopOutput = equip.heatingLoopOutput,
            activationHysteresis = equip.standaloneRelayActivationHysteresis,
            totalStages = equip.getInCalibratedPointPoint(totalStages),
            offConstrains = additionalOffConstraints,
            logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )
    }

    private fun addCoolingController(config: HyperStatSplitCpuConfiguration) {
        val totalStages = config.getHighestCoolingStageCount()
        if (totalStages == 0) return

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
            equip = equip,
            loopOutput = equip.coolingLoopOutput,
            activationHysteresis = equip.standaloneRelayActivationHysteresis,
            totalStages = equip.highestCoolingStages,
            offConstrains = additionalOffConstraints,
            economizingAvailable = equip.isEconAvailable,
            logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )
    }

    private fun addFanSpeedController(
        config: HyperStatSplitCpuConfiguration, isPrePurgeActive: () -> Boolean
    ) {
        val totalStages = config.getHighestFanStageCount()
        if (totalStages == 0) return

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
            equip = equip,
            loopOutput = equip.derivedFanLoopOutput,
            activationHysteresis = equip.standaloneRelayActivationHysteresis,
            totalStages = equip.getInCalibratedPointPoint(totalStages),
            onConstrains = additionalOnConstraints,
            offConstrains = additionalOffConstraints,
            logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )
    }

    private fun addCompressorController(config: HyperStatSplitCpuConfiguration) {
        val totalStages = config.getHighestCompressorStageCount()
        if (totalStages == 0) return

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

        if (equip.highestCompressorStages.data <= 0) {
            equip.highestCompressorStages.data = totalStages.toDouble()
        }

        controlFactory.addStageController(
            ControllerNames.COMPRESSOR_RELAY_CONTROLLER,
            equip = equip,
            loopOutput = equip.compressorLoopOutput,
            activationHysteresis = equip.standaloneRelayActivationHysteresis,
            totalStages = equip.highestCompressorStages,
            offConstrains = additionalOffConstraints,
            economizingAvailable = equip.isEconAvailable,
            logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )
    }

    private fun addFanEnabledController() {
        controlFactory.addFanEnableController(
            equip = equip,
            fanLoopOutput = equip.fanLoopOutput,
            occupancy = zoneOccupancyState,
            offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
            logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )

    }

    private fun addOccupiedEnableController() {
        controlFactory.addOccupiedEnableController(
            equip, zoneOccupancyState, logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )
    }

    private fun addHumidifierController() {
        controlFactory.addHumidifierController(
            equip = equip,
            zoneHumidity = equip.zoneHumidity,
            targetHumidifier = equip.targetHumidifier,
            activationHysteresis = equip.standaloneRelayActivationHysteresis,
            offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
            logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )
    }

    private fun addDehumidifierController() {
        controlFactory.addDeHumidifierController(
            equip = equip,
            zoneHumidity = equip.zoneHumidity,
            targetDehumidifier = equip.targetDehumidifier,
            activationHysteresis = equip.standaloneRelayActivationHysteresis,
            offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
            logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )
    }

    private fun addExhaustFanStage1Controller() {
        if (equip.controllers.containsKey(ControllerNames.EXHAUST_FAN_STAGE1_CONTROLLER)) return

        val controller = ExhaustFanController(
            equip.outsideAirFinalLoopOutput,
            equip.exhaustFanStage1Threshold,
            equip.exhaustFanHysteresis,
            logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )
        controller.addOffConstraint(Constraint { equip.isCondensateTripped() })
        equip.controllers[ControllerNames.EXHAUST_FAN_STAGE1_CONTROLLER] = controller
    }

    private fun addExhaustFanStage2Controller() {
        if (equip.controllers.containsKey(ControllerNames.EXHAUST_FAN_STAGE2_CONTROLLER)) return

        val controller = ExhaustFanController(
            equip.outsideAirFinalLoopOutput,
            equip.exhaustFanStage2Threshold,
            equip.exhaustFanHysteresis,
            logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )
        controller.addOffConstraint(Constraint { equip.isCondensateTripped() })
        equip.controllers[ControllerNames.EXHAUST_FAN_STAGE2_CONTROLLER] = controller
    }

    private fun addDcvDamperController() {
        controlFactory.addDcvDamperController(
            equip = equip,
            dcvLoopOutput = equip.dcvLoopOutput,
            actionHysteresis = equip.standaloneRelayActivationHysteresis,
            currentOccupancy = zoneOccupancyState,
            offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
            logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )
    }

    private fun addChangeOverCoolingController() {
        controlFactory.addChangeOverCoolingController(
            equip = equip,
            coolingLoopOutput = equip.coolingLoopOutput,
            offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
            logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )
    }

    private fun addChangeOverHeatingController() {
        controlFactory.addChangeOverHeatingController(
            equip = equip,
            heatingLoopOutput = equip.heatingLoopOutput,
            offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
            logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )
    }

    private fun addAuxHeatingStage1Controller() {
        controlFactory.addAuxHeatingStage1Controller(
            equip = equip,
            currentTemp = equip.currentTemp,
            desiredTempHeating = equip.desiredTempHeating,
            auxHeating1Activate = equip.auxHeating1Activate,
            offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
            logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )
    }

    private fun addAuxHeatingStage2Controller() {
        controlFactory.addAuxHeatingStage2Controller(
            equip = equip,
            currentTemp = equip.currentTemp,
            desiredTempHeating = equip.desiredTempHeating,
            auxHeating2Activate = equip.auxHeating2Activate,
            offConstrains = ArrayList(listOf(Constraint { equip.isCondensateTripped() })),
            logTag = L.TAG_CCU_HSSPLIT_CPUECON
        )
    }
}


/*

   private fun addCoolingController(config: HyperStatSplitCpuConfiguration) {
       if (equip.controllers.containsKey(ControllerNames.COOLING_STAGE_CONTROLLER)) return

       val totalStages = config.getHighestCoolingStageCount()
       val controller = StageControlHandler(
           ControllerNames.COOLING_STAGE_CONTROLLER,
           loopOutput = equip.coolingLoopOutput,
           hysteresis = equip.standaloneRelayActivationHysteresis,
           totalStages = totalStages,
       )
       if (totalStages > 0) {
           controller.addOffConstraint(0, Constraint { equip.isCondensateTripped() })
       }
       if (totalStages >= 1) {
           controller.addOffConstraint(1, Constraint { equip.isCondensateTripped() })
       }
       if (totalStages >= 2) {
           controller.addOffConstraint(2, Constraint { equip.isCondensateTripped() })
       }
       equip.controllers[ControllerNames.COOLING_STAGE_CONTROLLER] = controller
   }

   private fun addHeatingController(config: HyperStatSplitCpuConfiguration) {
       if (equip.controllers.containsKey(ControllerNames.HEATING_STAGE_CONTROLLER)) return
       val totalStages = config.getHighestHeatingStageCount()
       val controller = StageControlHandler(
           ControllerNames.HEATING_STAGE_CONTROLLER,
           loopOutput = equip.heatingLoopOutput,
           hysteresis = equip.standaloneRelayActivationHysteresis,
           totalStages = totalStages
       )

       if (totalStages > 0) {
           controller.addOffConstraint(0, Constraint { equip.isCondensateTripped() })
       }
       if (totalStages >= 1) {
           controller.addOffConstraint(1, Constraint { equip.isCondensateTripped() })
       }
       if (totalStages >= 2) {
           controller.addOffConstraint(2, Constraint { equip.isCondensateTripped() })
       }
       equip.controllers[ControllerNames.HEATING_STAGE_CONTROLLER] = controller
   }


   private fun addFanSpeedController(config: HyperStatSplitCpuConfiguration, isPrePurgeActive: () -> Boolean) {
       if (equip.controllers.containsKey(ControllerNames.FAN_SPEED_CONTROLLER)) return
       val totalStages = config.getHighestFanStageCount()
       val controller = StageControlHandler(
           ControllerNames.FAN_SPEED_CONTROLLER,
           loopOutput = equip.derivedFanLoopOutput,
           hysteresis = equip.standaloneRelayActivationHysteresis,
           totalStages = totalStages
       )
       if (totalStages > 1) {
           controller.addOnConstraint(0, Constraint { isUserIntentFanMode(equip.fanOpMode) || isPrePurgeActive() })
           controller.addOffConstraint(0, Constraint { equip.isCondensateTripped() || !isUserIntentFanMode(equip.fanOpMode) })
       }
       if (totalStages >= 2) {
           controller.addOnConstraint(1, Constraint { isMediumUserIntentFanMode(equip.fanOpMode) || isHighUserIntentFanMode(equip.fanOpMode) || isPrePurgeActive() })
           controller.addOffConstraint(1, Constraint { equip.isCondensateTripped() || (!isMediumUserIntentFanMode(equip.fanOpMode) && !isHighUserIntentFanMode(equip.fanOpMode)) })
       }
       if (totalStages >= 3) {
           controller.addOnConstraint(2, Constraint { isHighUserIntentFanMode(equip.fanOpMode) || isPrePurgeActive() })
           controller.addOffConstraint(2, Constraint { equip.isCondensateTripped() || !isHighUserIntentFanMode(equip.fanOpMode) })
       }
       equip.controllers[ControllerNames.FAN_SPEED_CONTROLLER] = controller
   }


   private fun addFanSpeedController(config: HyperStatSplitCpuConfiguration, isPrePurgeActive: () -> Boolean) {
       if (equip.controllers.containsKey(ControllerNames.FAN_SPEED_CONTROLLER)) return
       val totalStages = config.getHighestFanStageCount()
       val controller = StageControlHandler(
           ControllerNames.FAN_SPEED_CONTROLLER,
           loopOutput = equip.derivedFanLoopOutput,
           hysteresis = equip.standaloneRelayActivationHysteresis,
           totalStages = totalStages
       )
       if (totalStages > 1) {
           controller.addOnConstraint(0, Constraint { isUserIntentFanMode(equip.fanOpMode) || isPrePurgeActive() })
           controller.addOffConstraint(0, Constraint { equip.isCondensateTripped() || !isUserIntentFanMode(equip.fanOpMode) })
       }
       if (totalStages >= 2) {
           controller.addOnConstraint(1, Constraint { isMediumUserIntentFanMode(equip.fanOpMode) || isHighUserIntentFanMode(equip.fanOpMode) || isPrePurgeActive() })
           controller.addOffConstraint(1, Constraint { equip.isCondensateTripped() || (!isMediumUserIntentFanMode(equip.fanOpMode) && !isHighUserIntentFanMode(equip.fanOpMode)) })
       }
       if (totalStages >= 3) {
           controller.addOnConstraint(2, Constraint { isHighUserIntentFanMode(equip.fanOpMode) || isPrePurgeActive() })
           controller.addOffConstraint(2, Constraint { equip.isCondensateTripped() || !isHighUserIntentFanMode(equip.fanOpMode) })
       }
       equip.controllers[ControllerNames.FAN_SPEED_CONTROLLER] = controller
   }

       private fun addCompressorController(config: HyperStatSplitCpuConfiguration) {
        if (equip.controllers.containsKey(ControllerNames.COMPRESSOR_RELAY_CONTROLLER)) return
        val totalStages= config.getHighestCompressorStageCount()
        val controller = StageControlHandler(
            ControllerNames.COMPRESSOR_RELAY_CONTROLLER,
            loopOutput = equip.compressorLoopOutput,
            hysteresis = equip.standaloneRelayActivationHysteresis,
            totalStages = totalStages
        )
        if (totalStages > 0) {
            controller.addOffConstraint(0, Constraint { equip.isCondensateTripped() })
        }
        if (totalStages >= 1) {
            controller.addOffConstraint(1, Constraint { equip.isCondensateTripped() })
        }
        if (totalStages >= 2) {
            controller.addOffConstraint(2, Constraint { equip.isCondensateTripped() })
        }
        equip.controllers[ControllerNames.COMPRESSOR_RELAY_CONTROLLER] = controller
    }

     private fun addChangeOverCoolingController() {
        if (equip.controllers.containsKey(ControllerNames.CHANGE_OVER_O_COOLING)) return

        val controller = EnableController(
            ControllerNames.CHANGE_OVER_O_COOLING,
            equip.coolingLoopOutput
        )
        controller.addOffConstraint(Constraint { equip.isCondensateTripped() })
        equip.controllers[ControllerNames.CHANGE_OVER_O_COOLING] = controller
    }

    private fun addChangeOverHeatingController() {
        if (equip.controllers.containsKey(ControllerNames.CHANGE_OVER_B_HEATING)) return

        val controller = EnableController(
            ControllerNames.CHANGE_OVER_B_HEATING,
            equip.heatingLoopOutput
        )
        controller.addOffConstraint(Constraint { equip.isCondensateTripped() })
        equip.controllers[ControllerNames.CHANGE_OVER_B_HEATING] = controller
    }

    private fun addAuxHeatingStage1Controller() {
        if (equip.controllers.containsKey(ControllerNames.AUX_HEATING_STAGE1)) return

        val controller = AuxHeatingController(
            ControllerNames.AUX_HEATING_STAGE1,
            equip.currentTemp,
            equip.desiredTempHeating,
            equip.auxHeating1Activate
        )
        controller.addOffConstraint(Constraint { equip.isCondensateTripped() })
        equip.controllers[ControllerNames.AUX_HEATING_STAGE1] = controller
    }

    private fun addAuxHeatingStage2Controller() {
        if (equip.controllers.containsKey(ControllerNames.AUX_HEATING_STAGE2)) return

        val controller = AuxHeatingController(
            ControllerNames.AUX_HEATING_STAGE2,
            equip.currentTemp,
            equip.desiredTempHeating,
            equip.auxHeating2Activate
        )
        controller.addOffConstraint(Constraint { equip.isCondensateTripped() })
        equip.controllers[ControllerNames.AUX_HEATING_STAGE2] = controller
    }
   */
