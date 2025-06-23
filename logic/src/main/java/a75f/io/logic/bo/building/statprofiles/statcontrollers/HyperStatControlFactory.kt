package a75f.io.logic.bo.building.statprofiles.statcontrollers

import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.hyperstat.Pipe2V2Equip
import a75f.io.logic.L
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsCpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsHpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe2RelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Pipe2Configuration
import a75f.io.logic.bo.building.statprofiles.util.getInCalibratedPointPoint
import a75f.io.logic.controlcomponents.controls.ControllerFactory
import a75f.io.logic.controlcomponents.util.ControllerNames

/**
 * Created by Manjunath K on 12-05-2025.
 */

class HyperStatControlFactory(
    var equip: HyperStatEquip
) {
    private val controllerFactory = ControllerFactory()
    fun addControllers(config: HyperStatConfiguration) {
        when (config) {
            is CpuConfiguration -> addHsCpuControllers(config)
            is HpuConfiguration -> addHpuControllers(config)
            is Pipe2Configuration -> addPipe2Controllers(config)
            else -> {
                /** DO NOTHING */
            }
        }
    }



    private fun addHsCpuControllers(config: CpuConfiguration) {
        val mappings = config.getRelayEnabledAssociations() // only enabled mappings
        mappings.forEach {
            val mapping = HsCpuRelayMapping.values()[it.second]
            when (mapping) {
                HsCpuRelayMapping.COOLING_STAGE_1, HsCpuRelayMapping.COOLING_STAGE_2, HsCpuRelayMapping.COOLING_STAGE_3 -> {
                    controllerFactory.addStageController(
                        ControllerNames.COOLING_STAGE_CONTROLLER,
                        equip,
                        equip.coolingLoopOutput,
                        getInCalibratedPointPoint(config.getHighestCoolingStageCount()),
                        equip.standaloneRelayActivationHysteresis,
                        logTag = L.TAG_CCU_HSCPU
                    )
                }

                HsCpuRelayMapping.HEATING_STAGE_1, HsCpuRelayMapping.HEATING_STAGE_2, HsCpuRelayMapping.HEATING_STAGE_3 -> {
                    controllerFactory.addStageController(
                        ControllerNames.HEATING_STAGE_CONTROLLER,
                        equip,
                        equip.heatingLoopOutput,
                        getInCalibratedPointPoint(config.getHighestHeatingStageCount()),
                        equip.standaloneRelayActivationHysteresis,
                        logTag = L.TAG_CCU_HSCPU
                    )
                }

                HsCpuRelayMapping.FAN_LOW_SPEED, HsCpuRelayMapping.FAN_MEDIUM_SPEED, HsCpuRelayMapping.FAN_HIGH_SPEED -> {
                    controllerFactory.addStageController(
                        ControllerNames.FAN_SPEED_CONTROLLER,
                        equip,
                        equip.derivedFanLoopOutput,
                        getInCalibratedPointPoint(config.getHighestFanStageCount()),
                        equip.standaloneRelayActivationHysteresis,
                        logTag = L.TAG_CCU_HSCPU
                    )
                }

                HsCpuRelayMapping.FAN_ENABLED -> {
                    controllerFactory.addFanEnableController(
                        equip,
                        equip.fanLoopOutput,
                        equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_HSCPU
                    )
                }

                HsCpuRelayMapping.OCCUPIED_ENABLED -> {
                    controllerFactory.addOccupiedEnableController(
                        equip, equip.zoneOccupancyState, logTag = L.TAG_CCU_HSCPU
                    )
                }

                HsCpuRelayMapping.HUMIDIFIER -> {
                    controllerFactory.addHumidifierController(
                        equip,
                        equip.zoneHumidity,
                        equip.targetHumidifier,
                        equip.standaloneRelayActivationHysteresis,
                        logTag = L.TAG_CCU_HSCPU
                    )
                }

                HsCpuRelayMapping.DEHUMIDIFIER -> {
                    controllerFactory.addDeHumidifierController(
                        equip,
                        equip.zoneHumidity,
                        equip.targetDehumidifier,
                        equip.standaloneRelayActivationHysteresis,
                        logTag = L.TAG_CCU_HSCPU
                    )
                }

                HsCpuRelayMapping.DCV_DAMPER -> {
                    controllerFactory.addDcvDamperController(
                        equip,
                        equip.dcvLoopOutput,
                        equip.standaloneRelayActivationHysteresis,
                        equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_HSCPU
                    )
                }

                HsCpuRelayMapping.EXTERNALLY_MAPPED -> {
                    /** DO NOTHING */
                }

            }
        }
    }

    private fun addHpuControllers(config: HpuConfiguration) {
        val mappings = config.getRelayEnabledAssociations() // only enabled mappings
        mappings.forEach {
            val mapping = HsHpuRelayMapping.values()[it.second]
            when (mapping) {
                HsHpuRelayMapping.COMPRESSOR_STAGE1, HsHpuRelayMapping.COMPRESSOR_STAGE2, HsHpuRelayMapping.COMPRESSOR_STAGE3 -> {
                    controllerFactory.addStageController(
                        ControllerNames.COMPRESSOR_RELAY_CONTROLLER,
                        equip = equip,
                        loopOutput = (equip as HpuV2Equip).compressorLoopOutput,
                        activationHysteresis = equip.standaloneRelayActivationHysteresis,
                        totalStages = getInCalibratedPointPoint(config.highestCompressorStages()),
                        logTag = L.TAG_CCU_HSHPU
                    )
                }

                HsHpuRelayMapping.FAN_LOW_SPEED, HsHpuRelayMapping.FAN_MEDIUM_SPEED, HsHpuRelayMapping.FAN_HIGH_SPEED -> {
                    controllerFactory.addStageController(
                        ControllerNames.FAN_SPEED_CONTROLLER,
                        equip,
                        equip.derivedFanLoopOutput,
                        getInCalibratedPointPoint(config.getHighestFanStageCount()),
                        equip.standaloneRelayActivationHysteresis,
                        logTag = L.TAG_CCU_HSHPU
                    )
                }

                HsHpuRelayMapping.AUX_HEATING_STAGE1 -> {
                    controllerFactory.addAuxHeatingStage1Controller(
                        equip,
                        equip.currentTemp,
                        equip.desiredTempHeating,
                        (equip as HpuV2Equip).auxHeating1Activate,
                        logTag = L.TAG_CCU_HSHPU
                    )
                }

                HsHpuRelayMapping.AUX_HEATING_STAGE2 -> {
                    controllerFactory.addAuxHeatingStage2Controller(
                        equip,
                        equip.currentTemp,
                        equip.desiredTempHeating,
                        (equip as HpuV2Equip).auxHeating2Activate,
                        logTag = L.TAG_CCU_HSHPU
                    )
                }

                HsHpuRelayMapping.FAN_ENABLED -> {
                    controllerFactory.addFanEnableController(
                        equip,
                        equip.fanLoopOutput,
                        equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_HSHPU
                    )
                }

                HsHpuRelayMapping.OCCUPIED_ENABLED -> {
                    controllerFactory.addOccupiedEnableController(
                        equip, equip.zoneOccupancyState, logTag = L.TAG_CCU_HSHPU
                    )
                }

                HsHpuRelayMapping.HUMIDIFIER -> {
                    controllerFactory.addHumidifierController(
                        equip,
                        equip.zoneHumidity,
                        equip.targetHumidifier,
                        equip.standaloneRelayActivationHysteresis,
                        logTag = L.TAG_CCU_HSHPU
                    )
                }

                HsHpuRelayMapping.DEHUMIDIFIER -> {
                    controllerFactory.addDeHumidifierController(
                        equip,
                        equip.zoneHumidity,
                        equip.targetDehumidifier,
                        equip.standaloneRelayActivationHysteresis,
                        logTag = L.TAG_CCU_HSHPU
                    )
                }

                HsHpuRelayMapping.CHANGE_OVER_O_COOLING -> {
                    controllerFactory.addChangeOverCoolingController(
                        equip, equip.coolingLoopOutput, logTag = L.TAG_CCU_HSHPU
                    )
                }

                HsHpuRelayMapping.CHANGE_OVER_B_HEATING -> {
                    controllerFactory.addChangeOverHeatingController(
                        equip, equip.heatingLoopOutput, logTag = L.TAG_CCU_HSHPU
                    )
                }

                HsHpuRelayMapping.EXTERNALLY_MAPPED -> {
                    /** DO NOTHING */
                }

                HsHpuRelayMapping.DCV_DAMPER -> {
                    controllerFactory.addDcvDamperController(
                        equip,
                        equip.dcvLoopOutput,
                        equip.standaloneRelayActivationHysteresis,
                        equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_HSHPU
                    )
                }
            }
        }

    }

    private fun addPipe2Controllers(config: Pipe2Configuration) {
        val mappings = config.getRelayEnabledAssociations() // only enabled mappings
        mappings.forEach {
            val mapping = HsPipe2RelayMapping.values()[it.second]
            when (mapping) {
                HsPipe2RelayMapping.WATER_VALVE -> {
                    controllerFactory.addWaterValveController(
                        equip = equip as Pipe2V2Equip,
                        waterValveLoop = (equip as Pipe2V2Equip).waterValveLoop,
                        actionHysteresis = equip.standaloneRelayActivationHysteresis,
                        logTag = L.TAG_CCU_HSPIPE2
                    )
                }

                HsPipe2RelayMapping.FAN_LOW_SPEED, HsPipe2RelayMapping.FAN_MEDIUM_SPEED, HsPipe2RelayMapping.FAN_HIGH_SPEED -> {
                    controllerFactory.addStageController(
                        ControllerNames.FAN_SPEED_CONTROLLER,
                        equip,
                        equip.derivedFanLoopOutput,
                        getInCalibratedPointPoint(config.getHighestFanStageCount()),
                        equip.standaloneRelayActivationHysteresis,
                        logTag = L.TAG_CCU_HSPIPE2
                    )
                }

                HsPipe2RelayMapping.AUX_HEATING_STAGE1 -> {
                    controllerFactory.addAuxHeatingStage1Controller(
                        equip,
                        equip.currentTemp,
                        equip.desiredTempHeating,
                        (equip as Pipe2V2Equip).auxHeating1Activate,
                        logTag = L.TAG_CCU_HSPIPE2
                    )
                }

                HsPipe2RelayMapping.AUX_HEATING_STAGE2 -> {
                    controllerFactory.addAuxHeatingStage2Controller(
                        equip,
                        equip.currentTemp,
                        equip.desiredTempHeating,
                        (equip as Pipe2V2Equip).auxHeating2Activate,
                        logTag = L.TAG_CCU_HSPIPE2
                    )
                }

                HsPipe2RelayMapping.FAN_ENABLED -> {
                    controllerFactory.addFanEnableController(
                        equip,
                        equip.fanLoopOutput,
                        equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_HSPIPE2
                    )
                }

                HsPipe2RelayMapping.OCCUPIED_ENABLED -> {
                    controllerFactory.addOccupiedEnableController(
                        equip, equip.zoneOccupancyState, logTag = L.TAG_CCU_HSPIPE2
                    )
                }

                HsPipe2RelayMapping.HUMIDIFIER -> {
                    controllerFactory.addHumidifierController(
                        equip,
                        equip.zoneHumidity,
                        equip.targetHumidifier,
                        equip.standaloneRelayActivationHysteresis,
                        logTag = L.TAG_CCU_HSPIPE2
                    )
                }

                HsPipe2RelayMapping.DEHUMIDIFIER -> {
                    controllerFactory.addDeHumidifierController(
                        equip,
                        equip.zoneHumidity,
                        equip.targetDehumidifier,
                        equip.standaloneRelayActivationHysteresis,
                        logTag = L.TAG_CCU_HSPIPE2
                    )
                }

                HsPipe2RelayMapping.DCV_DAMPER -> {
                    controllerFactory.addDcvDamperController(
                        equip,
                        equip.dcvLoopOutput,
                        equip.standaloneRelayActivationHysteresis,
                        equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_HSPIPE2
                    )
                }

                else -> {}
            }
        }
    }

}