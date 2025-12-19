package a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.pipe4

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Point
import a75f.io.domain.equips.hyperstat.HsPipe4Equip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.HyperStatProfile
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe4AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe4Configuration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe4RelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Pipe4AnalogOutConfigs
import a75f.io.logic.bo.building.statprofiles.statcontrollers.HyperStatControlFactory
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.HyperStatProfileTuners
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.canWeDoConditioning
import a75f.io.logic.bo.building.statprofiles.util.canWeDoCooling
import a75f.io.logic.bo.building.statprofiles.util.canWeDoHeating
import a75f.io.logic.bo.building.statprofiles.util.canWeRunFan
import a75f.io.logic.bo.building.statprofiles.util.fetchBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.fetchHyperStatTuners
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getHSAnalogOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.getHSLogicalPointList
import a75f.io.logic.bo.building.statprofiles.util.getHSRelayOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.getHsConfiguration
import a75f.io.logic.bo.building.statprofiles.util.isHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMediumUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.keyCardIsInSlot
import a75f.io.logic.bo.building.statprofiles.util.logResults
import a75f.io.logic.bo.building.statprofiles.util.runLowestFanSpeedDuringDoorOpen
import a75f.io.logic.bo.building.statprofiles.util.updateLoopOutputs
import a75f.io.logic.bo.building.statprofiles.util.updateOccupancyDetection
import a75f.io.logic.bo.building.statprofiles.util.updateOperatingMode
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.handlers.doAnalogOperation
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.util.uiutils.HyperStatUserIntentHandler

/**
 * Author: Manjunath Kundaragi
 * Created on: 16-10-2025
 */
class HyperStatPipe4Profile: HyperStatProfile(L.TAG_CCU_HSPIPE4) {


    private var analogLogicalPoints: HashMap<Int, String> = HashMap()
    private var relayLogicalPoints: HashMap<Int, String> = HashMap()

    private val pipe4DeviceMap: MutableMap<Int, HsPipe4Equip> = mutableMapOf()
    override fun getProfileType() = ProfileType.HYPERSTAT_FOUR_PIPE_FCU

    override fun updateZonePoints() {
        pipe4DeviceMap.forEach { (nodeAddress, equip) ->
            pipe4DeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as HsPipe4Equip
            logIt("Process Pipe4: equipRef =  ${equip.nodeAddress}")
            processHyperStatPipe4Profile(equip)
        }
    }

    fun processHyperStatPipe4Profile(equip: HsPipe4Equip) {

        if (Globals.getInstance().isTestMode) {
            logIt("Test mode is on: ${equip.equipRef}")
            return
        }

        if (mInterface != null) mInterface.refreshView()

        if (isRFDead) {
            handleRFDead(equip)
            return
        } else if (isZoneDead) {
            handleDeadZone(equip)
            return
        }

        val config = getHsConfiguration(equip.equipRef)
        val hyperStatTuners = fetchHyperStatTuners(equip) as HyperStatProfileTuners
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val fanModeSaved =
            FanModeCacheStorage.getHyperStatFanModeCache().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchBasicSettings(equip)
        val controllerFactory = HyperStatControlFactory(
            equip,
            controllers,
            stageCounts,
            derivedFanLoopOutput,
            zoneOccupancyState
        )

        logicalPointsList = getHSLogicalPointList(equip, config!!)
        relayLogicalPoints = getHSRelayOutputPoints(equip)
        analogLogicalPoints = getHSAnalogOutputPoints(equip)
        curState = ZoneState.DEADBAND

        if (equipOccupancyHandler != null) {
            occupancyStatus = equipOccupancyHandler.currentOccupiedMode
            zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        }

        logIt("Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        logIt("After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")

        loopController.initialise(tuners = hyperStatTuners)
        loopController.dumpLogs()
        handleChangeOfDirection(currentTemp, userIntents, controllerFactory, equip)
        updateOperatingMode(
            currentTemp,
            averageDesiredTemp,
            basicSettings.conditioningMode,
            equip.operatingMode
        )

        resetEquip(equip)
        evaluateLoopOutputs(userIntents, basicSettings, hyperStatTuners, config, equip)
        updateOccupancyDetection(equip)

        doorWindowSensorOpenStatus = runForDoorWindowSensor(equip)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)

        fanLowVentilationAvailable.data =
            if (equip.fanLowSpeedVentilation.pointExists()) 1.0 else 0.0
        keyCardIsInSlot(equip)
        updateLoopOutputs(
            coolingLoopOutput, equip.coolingLoopOutput,
            heatingLoopOutput, equip.heatingLoopOutput,
            fanLoopOutput, equip.fanLoopOutput,
            dcvLoopOutput, equip.dcvLoopOutput
        )

        coolingLoopOutput = equip.coolingLoopOutput.readHisVal().toInt()
        heatingLoopOutput = equip.heatingLoopOutput.readHisVal().toInt()
        fanLoopOutput = equip.fanLoopOutput.readHisVal().toInt()
        dcvLoopOutput = equip.dcvLoopOutput.readHisVal().toInt()
        if (canWeRunFan(basicSettings) && (doorWindowSensorOpenStatus.not())) {
            operateRelays(config as HsPipe4Configuration, basicSettings, equip, controllerFactory)
            operateAnalogOutputs(config, equip, equip.analogOutStages, basicSettings)
            val analogFanType = operateAuxBasedFan(equip, basicSettings)
            runSpecifiedAnalogFanSpeed(
                equip,
                analogFanType,
                config.getAnalogOutsConfigurationMapping(),
                config.getFanConfiguration()
            )
        } else {
            resetLogicalPoints(equip)
            if (isDoorOpenFromTitle24 && canWeRunFan(basicSettings)) {
                runLowestFanSpeedDuringDoorOpen(equip, L.TAG_CCU_HSPIPE4)
            }
        }
        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())
        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState =
            ZoneTempState.EMERGENCY
        printStatus(hyperStatTuners, basicSettings, userIntents, equip, config)
        HyperStatUserIntentHandler.updateHyperStatStatus(temperatureState, equip, L.TAG_CCU_HSPIPE4)
        if (occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        logIt("----------------------------------------------------------")

    }

    private fun operateRelays(
        config: HsPipe4Configuration, basicSettings: BasicSettings,
        equip: HsPipe4Equip, controllerFactory: HyperStatControlFactory
    ) {
        controllerFactory.addPipe4Controllers(config, fanLowVentilationAvailable)
        runControllers(equip, basicSettings)
    }

    private fun runControllers(equip: HsPipe4Equip, basicSettings: BasicSettings) {
        derivedFanLoopOutput.data = equip.fanLoopOutput.readHisVal()
        zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            val result = controller.runController()
            updateRelayStatus(controllerName, result, equip, basicSettings)
        }
    }

    private fun updateRelayStatus(
        controllerName: String, result: Any, equip: HsPipe4Equip, basicSettings: BasicSettings
    ) {

        fun updateStatus(point: Point, result: Any, status: String? = null) {
            if (point.pointExists()) {
                point.writeHisVal(if (result as Boolean) 1.0 else 0.0)
                if (status != null && result) {
                    equip.relayStages[status] = 1
                } else {
                    equip.relayStages.remove(status)
                }
            } else {
                equip.relayStages.remove(status)
            }
        }

        when (controllerName) {
            ControllerNames.COOLING_VALVE_CONTROLLER -> {
                var status = result as Boolean
                if (canWeDoCooling(basicSettings.conditioningMode).not()) {
                    status = false
                }
                updateStatus(equip.chilledWaterCoolValve, status, StatusMsgKeys.COOLING_VALVE.name)
            }

            ControllerNames.HEATING_VALVE_CONTROLLER -> {
                var status = result as Boolean
                if (canWeDoHeating(basicSettings.conditioningMode).not()) {
                    status = false
                }
                updateStatus(equip.hotWaterHeatValve, status, StatusMsgKeys.HEATING_VALVE.name)
            }

            ControllerNames.FAN_SPEED_CONTROLLER -> {

                runTitle24Rule(equip)

                fun checkUserIntentAction(stage: Int): Boolean {
                    val mode = equip.fanOpMode
                    return when (stage) {
                        0 -> isLowUserIntentFanMode(mode)
                        1 -> isMediumUserIntentFanMode(mode)
                        2 -> isHighUserIntentFanMode(mode)
                        else -> false
                    }
                }

                val isFanGoodToRun = isFanGoodRun(doorWindowSensorOpenStatus, equip)

                fun isStageActive(
                    stage: Int, currentState: Boolean, isLowestStageActive: Boolean
                ): Boolean {
                    val mode = equip.fanOpMode.readPriorityVal().toInt()
                    return if (isFanGoodToRun && mode == StandaloneFanStage.AUTO.ordinal) {
                        (currentState || (isLowestStageActive && runFanLowDuringDoorWindow))
                    } else {
                        checkUserIntentAction(stage)
                    }
                }

                val fanStages = result as List<Pair<Int, Boolean>>

                val highExist =
                    fanStages.find { it.first == HsPipe4RelayMapping.FAN_HIGH_SPEED.ordinal }
                val mediumExist =
                    fanStages.find { it.first == HsPipe4RelayMapping.FAN_MEDIUM_SPEED.ordinal }
                val lowExist =
                    fanStages.find { (it.first == HsPipe4RelayMapping.FAN_LOW_SPEED.ordinal || it.first == HsPipe4RelayMapping.FAN_LOW_VENTILATION.ordinal)}

                var isHighActive = false
                var isMediumActive = false

                if (equip.fanHighSpeed.pointExists() && highExist != null) {
                    isHighActive =
                        isStageActive(highExist.first, highExist.second, lowestStageFanHigh)
                    updateStatus(equip.fanHighSpeed, isHighActive, Stage.FAN_3.displayName)
                }

                if (equip.fanMediumSpeed.pointExists() && mediumExist != null) {
                    isMediumActive =
                        if (isHighActive && isConfigPresent(HsPipe4RelayMapping.FAN_HIGH_SPEED)) false else isStageActive(
                            mediumExist.first,
                            mediumExist.second,
                            lowestStageFanMedium
                        )
                    updateStatus(equip.fanMediumSpeed, isMediumActive, Stage.FAN_2.displayName)
                }

                if ((equip.fanLowSpeed.pointExists() || equip.fanLowSpeedVentilation.pointExists()) && lowExist != null) {
                    val isLowActive =
                        if ((isHighActive && isConfigPresent(HsPipe4RelayMapping.FAN_HIGH_SPEED))
                            || (isConfigPresent(HsPipe4RelayMapping.FAN_MEDIUM_SPEED) && isMediumActive)
                        ) false else isStageActive(
                            lowExist.first,
                            lowExist.second,
                            lowestStageFanLow
                        )
                    updateStatus(
                        if (fanLowVentilationAvailable.readHisVal() > 0) equip.fanLowSpeedVentilation else equip.fanLowSpeed,
                        isLowActive, Stage.FAN_1.displayName
                    )
                }
            }

            ControllerNames.AUX_HEATING_STAGE1 -> {
                var status = result as Boolean
                if (canWeDoHeating(basicSettings.conditioningMode).not()) {
                    status = false
                }
                isAuxStage1Active = status
                updateStatus(equip.auxHeatingStage1, status, StatusMsgKeys.AUX_HEATING_STAGE1.name)
            }

            ControllerNames.AUX_HEATING_STAGE2 -> {
                var status = result as Boolean
                if (canWeDoHeating(basicSettings.conditioningMode).not()) {
                    status = false
                }
                isAuxStage2Active = status
                updateStatus(equip.auxHeatingStage2, status, StatusMsgKeys.AUX_HEATING_STAGE2.name)
            }

            ControllerNames.FAN_ENABLED -> updateStatus(
                equip.fanEnable,
                result,
                StatusMsgKeys.FAN_ENABLED.name
            )

            ControllerNames.OCCUPIED_ENABLED -> updateStatus(equip.occupiedEnable, result)
            ControllerNames.HUMIDIFIER_CONTROLLER -> updateStatus(equip.humidifierEnable, result)
            ControllerNames.DEHUMIDIFIER_CONTROLLER -> updateStatus(
                equip.dehumidifierEnable,
                result
            )

            ControllerNames.DAMPER_RELAY_CONTROLLER -> updateStatus(
                equip.dcvDamper,
                result,
                StatusMsgKeys.DCV_DAMPER.name
            )

            else -> {
                logIt("Unknown controller: $controllerName")
            }
        }
    }

    private fun isFanGoodRun(isDoorWindowOpen: Boolean, equip: HsPipe4Equip): Boolean {
        return if (fanLowVentilationAvailable.readHisVal() > 0) true
        else if (isDoorWindowOpen || heatingLoopOutput > 0) {
            // If current direction is heating then check allow only when  heating is available
            (equip.hotWaterModulatingHeatValve.pointExists() || equip.hotWaterHeatValve.pointExists() || equip.auxHeatingStage1.pointExists() || equip.auxHeatingStage2.pointExists())
        } else if (isDoorWindowOpen || coolingLoopOutput > 0) {
            (equip.chilledWaterModulatingCoolValve.pointExists() || equip.chilledWaterCoolValve.pointExists())
        } else {
            false
        }
    }


    private fun operateAnalogOutputs(
        config: HsPipe4Configuration, equip: HsPipe4Equip, analogOutStages: HashMap<String, Int>, basicSettings: BasicSettings) {
        config.apply {
            listOf(
                Pair(
                    Pipe4AnalogOutConfigs(
                        analogOut1Enabled.enabled,
                        analogOut1Association.associationVal,
                        analogOut1MinMaxConfig,
                        analogOut1FanSpeedConfig,
                    ), Port.ANALOG_OUT_ONE
                ),
                Pair(
                    Pipe4AnalogOutConfigs(
                        analogOut2Enabled.enabled,
                        analogOut2Association.associationVal,
                        analogOut2MinMaxConfig,
                        analogOut2FanSpeedConfig,
                    ), Port.ANALOG_OUT_TWO
                ),
                Pair(
                    Pipe4AnalogOutConfigs(
                        analogOut3Enabled.enabled,
                        analogOut3Association.associationVal,
                        analogOut3MinMaxConfig,
                        analogOut3FanSpeedConfig,
                    ), Port.ANALOG_OUT_THREE
                ),
            ).forEach { (analogOut, port) ->
                if (analogOut.enabled) {
                    val analogMapping = HsPipe4AnalogOutMapping.values()
                        .find { it.ordinal == analogOut.association }
                    when (analogMapping) {
                        HsPipe4AnalogOutMapping.COOLING_MODULATING_VALUE -> {
                            doAnalogOperation(
                                canWeDoCooling(basicSettings.conditioningMode),
                                analogOutStages,
                                StatusMsgKeys.COOLING.name,
                                coolingLoopOutput,
                                equip.chilledWaterModulatingCoolValve
                            )
                        }

                        HsPipe4AnalogOutMapping.HEATING_MODULATING_VALUE -> {
                            doAnalogOperation(
                                canWeDoHeating(basicSettings.conditioningMode),
                                analogOutStages,
                                StatusMsgKeys.HEATING.name,
                                heatingLoopOutput,
                                equip.hotWaterModulatingHeatValve
                            )
                        }

                        HsPipe4AnalogOutMapping.FAN_SPEED -> {
                            doAnalogFanAction(
                                port,
                                analogOut.fanSpeed.low.currentVal.toInt(),
                                analogOut.fanSpeed.medium.currentVal.toInt(),
                                analogOut.fanSpeed.high.currentVal.toInt(),
                                equip,
                                basicSettings,
                                fanLoopOutput,
                                isFanGoodRun(doorWindowSensorOpenStatus, equip)
                            )
                        }

                        HsPipe4AnalogOutMapping.DCV_DAMPER -> {
                            doAnalogDCVAction(
                                port, analogOutStages, config.zoneCO2Threshold.currentVal,
                                config.zoneCO2DamperOpeningRate.currentVal, equip
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun printStatus(
        tuners: HyperStatProfileTuners,
        settings: BasicSettings,
        userIntents: UserIntents,
        equip: HyperStatEquip,
        config: HyperStatConfiguration
    ) {

        CcuLog.i(
            L.TAG_CCU_HSPIPE4,
            "Fan speed multiplier:  ${tuners.analogFanSpeedMultiplier} " +
                    "AuxHeating1Activate: ${tuners.auxHeating1Activate} " +
                    "AuxHeating2Activate: ${tuners.auxHeating2Activate}  " +
                    "Current Occupancy: ${Occupancy.values()[equip.occupancyMode.readHisVal().toInt()]}\n" +
                    "Fan Mode : ${settings.fanMode} Conditioning Mode ${settings.conditioningMode} \n" +
                    "Current Temp : $currentTemp Desired (Heating: ${userIntents.heatingDesiredTemp} " +
                    "Cooling: ${userIntents.coolingDesiredTemp})\n" +
                    "Loop Outputs: (Heating: $heatingLoopOutput Cooling: $coolingLoopOutput " +
                    "Fan : $fanLoopOutput DCV $dcvLoopOutput) \n"
        )

        logResults(config, L.TAG_CCU_HSPIPE4, logicalPointsList)
    }


    override fun getAverageZoneTemp(): Double {
        var tempTotal = 0.0
        var nodeCount = 0
        pipe4DeviceMap.forEach { (_, device) ->
            if (device.currentTemp.readHisVal() > 0) {
                tempTotal += device.currentTemp.readHisVal()
                nodeCount++
            }
        }
        return if (nodeCount == 0) 0.0 else tempTotal / nodeCount
    }

    fun addEquip(equipRef: String) {
        val equip = HsPipe4Equip(equipRef)
        pipe4DeviceMap[equip.nodeAddress] = equip
    }

    override fun getEquip(): Equip? {
        for (nodeAddress in pipe4DeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    private fun isConfigPresent(mapping: HsPipe4RelayMapping) = relayLogicalPoints.containsKey(mapping.ordinal)

    fun getProfileDomainEquip(node: Int): HsPipe4Equip = pipe4DeviceMap[node]!!

    override fun getNodeAddresses(): Set<Short?> = pipe4DeviceMap.keys.map { it.toShort() }.toSet()

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        TODO("Not yet implemented")
    }
    override fun getCurrentTemp(): Double {
        for (nodeAddress in pipe4DeviceMap.keys) {
            return pipe4DeviceMap[nodeAddress]!!.currentTemp.readHisVal()
        }
        return 0.0
    }
}