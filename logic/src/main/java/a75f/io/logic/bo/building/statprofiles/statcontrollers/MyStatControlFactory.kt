package a75f.io.logic.bo.building.statprofiles.statcontrollers

import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.logic.L
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2RelayMapping
import a75f.io.logic.bo.building.statprofiles.util.getInCalibratedPointPoint
import a75f.io.logic.controlcomponents.controls.ControllerFactory
import a75f.io.logic.controlcomponents.util.ControllerNames


/**
 * Created by Manjunath K on 09-06-2025.
 */

class MyStatControlFactory(
    var equip: MyStatEquip
) {
    private val controllerFactory = ControllerFactory()

    fun addControllers(config: MyStatConfiguration) {
        when (config) {
            is MyStatCpuConfiguration -> addCpuControllers(config)
            is MyStatPipe2Configuration -> addPipe2Controllers(config)
            is MyStatHpuConfiguration -> addHpuControllers(config)
            else -> {
                /** DO NOTHING */
            }
        }
    }

    private fun addCpuControllers(config: MyStatCpuConfiguration) {
        val mappings = config.getRelayEnabledAssociations()
        mappings.forEach {
            val mapping = MyStatCpuRelayMapping.values()[it.second]
            when (mapping) {
                MyStatCpuRelayMapping.COOLING_STAGE_1, MyStatCpuRelayMapping.COOLING_STAGE_2 -> {
                    controllerFactory.addStageController(
                        ControllerNames.COOLING_STAGE_CONTROLLER,
                        equip, equip.coolingLoopOutput,
                        getInCalibratedPointPoint(config.getHighestCoolingStageCount()),
                        equip.standaloneRelayActivationHysteresis,
                        stageDownTimer = equip.stageDownTimer,
                        stageUpTimer = equip.stageUpTimer,
                        logTag = L.TAG_CCU_MSCPU

                    )
                }

                MyStatCpuRelayMapping.HEATING_STAGE_1, MyStatCpuRelayMapping.HEATING_STAGE_2 -> {
                    controllerFactory.addStageController(
                        ControllerNames.HEATING_STAGE_CONTROLLER,
                        equip,
                        equip.heatingLoopOutput,
                        getInCalibratedPointPoint(config.getHighestHeatingStageCount()),
                        equip.standaloneRelayActivationHysteresis,
                        stageDownTimer = equip.stageDownTimer,
                        stageUpTimer = equip.stageUpTimer,
                        logTag = L.TAG_CCU_MSCPU
                    )
                }

                MyStatCpuRelayMapping.FAN_LOW_SPEED, MyStatCpuRelayMapping.FAN_HIGH_SPEED -> {
                    controllerFactory.addStageController(
                        ControllerNames.FAN_SPEED_CONTROLLER,
                        equip,
                        equip.derivedFanLoopOutput,
                        getInCalibratedPointPoint(config.getHighestFanStageCount()),
                        equip.standaloneRelayActivationHysteresis,
                        stageDownTimer = equip.stageDownTimer,
                        stageUpTimer = equip.stageUpTimer,
                        logTag = L.TAG_CCU_MSCPU
                    )
                }

                MyStatCpuRelayMapping.FAN_ENABLED -> {
                    controllerFactory.addFanEnableController(
                        equip,
                        equip.fanLoopOutput,
                        equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_MSCPU
                    )
                }

                MyStatCpuRelayMapping.OCCUPIED_ENABLED -> {
                    controllerFactory.addOccupiedEnableController(
                        equip, equip.zoneOccupancyState, logTag = L.TAG_CCU_MSCPU
                    )
                }

                MyStatCpuRelayMapping.HUMIDIFIER -> {
                    controllerFactory.addHumidifierController(
                        equip,
                        equip.zoneHumidity,
                        equip.targetHumidifier,
                        equip.standaloneRelayActivationHysteresis,
                        occupancy = equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_MSCPU
                    )
                }

                MyStatCpuRelayMapping.DEHUMIDIFIER -> {
                    controllerFactory.addDeHumidifierController(
                        equip,
                        equip.zoneHumidity,
                        equip.targetDehumidifier,
                        equip.standaloneRelayActivationHysteresis,
                        occupancy = equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_MSCPU
                    )
                }

                MyStatCpuRelayMapping.DCV_DAMPER -> {
                    controllerFactory.addDcvDamperController(
                        equip,
                        equip.dcvLoopOutput,
                        equip.standaloneRelayActivationHysteresis,
                        equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_MSCPU
                    )
                }

                MyStatCpuRelayMapping.EXTERNALLY_MAPPED -> {}
            }
        }
    }

    private fun addPipe2Controllers(config: MyStatPipe2Configuration) {
        val mappings = config.getRelayEnabledAssociations()
        mappings.forEach {
            val mapping = MyStatPipe2RelayMapping.values()[it.second]
            when (mapping) {
                MyStatPipe2RelayMapping.FAN_LOW_SPEED, MyStatPipe2RelayMapping.FAN_HIGH_SPEED -> {
                    controllerFactory.addStageController(
                        ControllerNames.FAN_SPEED_CONTROLLER,
                        equip,
                        equip.derivedFanLoopOutput,
                        getInCalibratedPointPoint(config.getHighestFanStageCount()),
                        equip.standaloneRelayActivationHysteresis,
                        stageDownTimer = equip.stageDownTimer,
                        stageUpTimer = equip.stageUpTimer,
                        logTag = L.TAG_CCU_MSPIPE2
                    )
                }

                MyStatPipe2RelayMapping.AUX_HEATING_STAGE1 -> {
                    controllerFactory.addAuxHeatingStage1Controller(
                        equip,
                        equip.currentTemp,
                        equip.desiredTempHeating,
                        (equip as MyStatPipe2Equip).auxHeating1Activate,
                        logTag = L.TAG_CCU_MSPIPE2
                    )
                }

                MyStatPipe2RelayMapping.WATER_VALVE -> {
                    controllerFactory.addWaterValveController(
                        equip = equip,
                        waterValveLoop = (equip as MyStatPipe2Equip).waterValveLoop,
                        actionHysteresis = equip.standaloneRelayActivationHysteresis,
                        logTag = L.TAG_CCU_HSPIPE2
                    )
                }

                MyStatPipe2RelayMapping.FAN_ENABLED -> {
                    controllerFactory.addFanEnableController(
                        equip,
                        equip.fanLoopOutput,
                        equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_MSPIPE2
                    )
                }

                MyStatPipe2RelayMapping.OCCUPIED_ENABLED -> {
                    controllerFactory.addOccupiedEnableController(
                        equip, equip.zoneOccupancyState, logTag = L.TAG_CCU_MSPIPE2
                    )
                }

                MyStatPipe2RelayMapping.HUMIDIFIER -> {
                    controllerFactory.addHumidifierController(
                        equip,
                        equip.zoneHumidity,
                        equip.targetHumidifier,
                        equip.standaloneRelayActivationHysteresis,
                        occupancy = equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_MSPIPE2
                    )
                }

                MyStatPipe2RelayMapping.DEHUMIDIFIER -> {
                    controllerFactory.addDeHumidifierController(
                        equip,
                        equip.zoneHumidity,
                        equip.targetDehumidifier,
                        equip.standaloneRelayActivationHysteresis,
                        occupancy = equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_MSPIPE2
                    )
                }

                MyStatPipe2RelayMapping.DCV_DAMPER -> {
                    controllerFactory.addDcvDamperController(
                        equip,
                        equip.dcvLoopOutput,
                        equip.standaloneRelayActivationHysteresis,
                        equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_MSPIPE2
                    )
                }

                MyStatPipe2RelayMapping.EXTERNALLY_MAPPED -> {}
            }
        }
    }

    private fun addHpuControllers(config: MyStatHpuConfiguration) {
        val mappings = config.getRelayEnabledAssociations()
        mappings.forEach {
            val mapping = MyStatHpuRelayMapping.values()[it.second]
            when (mapping) {
                MyStatHpuRelayMapping.COMPRESSOR_STAGE1, MyStatHpuRelayMapping.COMPRESSOR_STAGE2 -> {
                    controllerFactory.addStageController(
                        ControllerNames.COMPRESSOR_RELAY_CONTROLLER,
                        equip = equip,
                        loopOutput = (equip as MyStatHpuEquip).compressorLoopOutput,
                        activationHysteresis = equip.standaloneRelayActivationHysteresis,
                        totalStages = getInCalibratedPointPoint(config.highestCompressorStages()),
                        stageDownTimer = equip.stageDownTimer,
                        stageUpTimer = equip.stageUpTimer,
                        logTag = L.TAG_CCU_MSHPU
                    )
                }

                MyStatHpuRelayMapping.AUX_HEATING_STAGE1 -> {
                    controllerFactory.addAuxHeatingStage1Controller(
                        equip,
                        equip.currentTemp,
                        equip.desiredTempHeating,
                        (equip as MyStatHpuEquip).mystatAuxHeating1Activate,
                        logTag = L.TAG_CCU_MSHPU
                    )
                }

                MyStatHpuRelayMapping.FAN_LOW_SPEED, MyStatHpuRelayMapping.FAN_HIGH_SPEED -> {
                    controllerFactory.addStageController(
                        ControllerNames.FAN_SPEED_CONTROLLER,
                        equip,
                        equip.derivedFanLoopOutput,
                        getInCalibratedPointPoint(config.getHighestFanStageCount()),
                        equip.standaloneRelayActivationHysteresis,
                        stageDownTimer = equip.stageDownTimer,
                        stageUpTimer = equip.stageUpTimer,
                        logTag = L.TAG_CCU_MSHPU
                    )
                }

                MyStatHpuRelayMapping.FAN_ENABLED -> {
                    controllerFactory.addFanEnableController(
                        equip,
                        equip.fanLoopOutput,
                        equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_MSHPU
                    )
                }

                MyStatHpuRelayMapping.OCCUPIED_ENABLED -> {
                    controllerFactory.addOccupiedEnableController(
                        equip, equip.zoneOccupancyState, logTag = L.TAG_CCU_MSHPU
                    )
                }

                MyStatHpuRelayMapping.HUMIDIFIER -> {
                    controllerFactory.addHumidifierController(
                        equip,
                        equip.zoneHumidity,
                        equip.targetHumidifier,
                        equip.standaloneRelayActivationHysteresis,
                        occupancy = equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_MSHPU
                    )
                }

                MyStatHpuRelayMapping.DEHUMIDIFIER -> {
                    controllerFactory.addDeHumidifierController(
                        equip,
                        equip.zoneHumidity,
                        equip.targetDehumidifier,
                        equip.standaloneRelayActivationHysteresis,
                        occupancy = equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_MSHPU
                    )
                }

                MyStatHpuRelayMapping.CHANGE_OVER_O_COOLING -> {
                    controllerFactory.addChangeOverCoolingController(
                        equip, equip.coolingLoopOutput, logTag = L.TAG_CCU_HSHPU
                    )
                }

                MyStatHpuRelayMapping.CHANGE_OVER_B_HEATING -> {
                    controllerFactory.addChangeOverHeatingController(
                        equip, equip.heatingLoopOutput, logTag = L.TAG_CCU_HSHPU
                    )
                }

                MyStatHpuRelayMapping.DCV_DAMPER -> {
                    controllerFactory.addDcvDamperController(
                        equip,
                        equip.dcvLoopOutput,
                        equip.standaloneRelayActivationHysteresis,
                        equip.zoneOccupancyState,
                        logTag = L.TAG_CCU_MSHPU
                    )
                }

                MyStatHpuRelayMapping.EXTERNALLY_MAPPED -> {}
            }

        }
    }

}