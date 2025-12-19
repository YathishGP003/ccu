package a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.pipe2

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.hyperstat.HsPipe2Equip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.HyperStatProfile
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe2AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe2RelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Pipe2AnalogOutConfigs
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Pipe2Configuration
import a75f.io.logic.bo.building.statprofiles.statcontrollers.HyperStatControlFactory
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.HyperStatProfileTuners
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.canWeDoConditioning
import a75f.io.logic.bo.building.statprofiles.util.canWeRunFan
import a75f.io.logic.bo.building.statprofiles.util.fetchBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.fetchHyperStatTuners
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getHSAnalogOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.getHSLogicalPointList
import a75f.io.logic.bo.building.statprofiles.util.getHSRelayOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.getHsConfiguration
import a75f.io.logic.bo.building.statprofiles.util.isFanGoodRun
import a75f.io.logic.bo.building.statprofiles.util.isHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMediumUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isSupplyOppositeToConditioning
import a75f.io.logic.bo.building.statprofiles.util.keyCardIsInSlot
import a75f.io.logic.bo.building.statprofiles.util.logResults
import a75f.io.logic.bo.building.statprofiles.util.milliToMin
import a75f.io.logic.bo.building.statprofiles.util.runLowestFanSpeedDuringDoorOpen
import a75f.io.logic.bo.building.statprofiles.util.updateLoopOutputs
import a75f.io.logic.bo.building.statprofiles.util.updateOccupancyDetection
import a75f.io.logic.bo.building.statprofiles.util.updateOperatingMode
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.util.uiutils.HyperStatUserIntentHandler
import java.util.Date


/**
 * Created by Manjunath K on 01-08-2022.
 */


class HyperStatPipe2Profile : HyperStatProfile(L.TAG_CCU_HSPIPE2) {

    private var supplyWaterTempTh2 = 0.0
    private var heatingThreshold = 85.0
    private var coolingThreshold = 65.0
    private var lastWaterValveTurnedOnTime: Long = System.currentTimeMillis()
    private var waterSamplingStartTime: Long = 0

    private var waterValveLoop = CalibratedPoint(DomainName.waterValve, "waterValveLoop",0.0)

    private val pipe2DeviceMap: MutableMap<Int, HsPipe2Equip> = mutableMapOf()
    private var isWaterValveActiveDueToLoop = false
    private var analogLogicalPoints: HashMap<Int, String> = HashMap()
    private var relayLogicalPoints: HashMap<Int, String> = HashMap()

    override fun getProfileType() = ProfileType.HYPERSTAT_TWO_PIPE_FCU

    override fun updateZonePoints() {
        pipe2DeviceMap.forEach { (nodeAddress, equip) ->
            pipe2DeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as HsPipe2Equip
            logIt("Process Pipe2: equipRef =  ${equip.nodeAddress}")
            processHyperStatPipeProfile(equip)
        }
    }

    fun addEquip(equipRef: String) {
        val equip = HsPipe2Equip(equipRef)
        pipe2DeviceMap[equip.nodeAddress] = equip
    }

    override fun getEquip(): Equip? {
        for (nodeAddress in pipe2DeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    fun processHyperStatPipeProfile(equip: HsPipe2Equip) {

        if (Globals.getInstance().isTestMode) {
            logIt( "Test mode is on: ${equip.equipRef}")
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
        val fanModeSaved = FanModeCacheStorage.getHyperStatFanModeCache().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchBasicSettings(equip)
        val controllerFactory = HyperStatControlFactory(equip, controllers, stageCounts, derivedFanLoopOutput, zoneOccupancyState)

        logicalPointsList = getHSLogicalPointList(equip, config!!)
        relayLogicalPoints = getHSRelayOutputPoints(equip)
        analogLogicalPoints = getHSAnalogOutputPoints(equip)
        curState = ZoneState.DEADBAND

        heatingThreshold = hyperStatTuners.heatingThreshold
        coolingThreshold = hyperStatTuners.coolingThreshold
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
        updateOperatingMode(currentTemp, averageDesiredTemp, basicSettings.conditioningMode, equip.operatingMode)

        resetEquip(equip)
        supplyWaterTempTh2 = equip.leavingWaterTemperature.readHisVal()
        evaluateLoopOutputs(userIntents, basicSettings, hyperStatTuners, config, equip)
        updateOccupancyDetection(equip)

        doorWindowSensorOpenStatus = runForDoorWindowSensor(equip)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)
        isWaterValveActiveDueToLoop = false

        fanLowVentilationAvailable.data = if (equip.fanLowSpeedVentilation.pointExists()) 1.0 else 0.0
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
            operateRelays(config as Pipe2Configuration, basicSettings, equip, userIntents, controllerFactory)
            operateAnalogOutputs(config, equip, basicSettings, equip.analogOutStages)
            processForWaterSampling(equip, hyperStatTuners, config, equip.relayStages, basicSettings)
            runAlgorithm(equip, basicSettings, equip.relayStages, equip.analogOutStages, config)
        } else {
            resetLogicalPoints(equip)
            if (isDoorOpenFromTitle24 && canWeRunFan(basicSettings)) {
                runLowestFanSpeedDuringDoorOpen(equip, L.TAG_CCU_HSHPU)
            }
        }

        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())
        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState =
            ZoneTempState.EMERGENCY
        printStatus(hyperStatTuners, basicSettings,userIntents,equip,config)
        HyperStatUserIntentHandler.updateHyperStatStatus(temperatureState, equip, L.TAG_CCU_HSPIPE2)
        if (occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        logIt("----------------------------------------------------------")
    }

    override fun evaluateFanOutput(
        basicSettings: BasicSettings,
        tuners: HyperStatProfileTuners
    ) {
        val multiplier = tuners.analogFanSpeedMultiplier
        val mode = basicSettings.conditioningMode
        val isCoolingAllowed = mode == StandaloneConditioningMode.COOL_ONLY || mode == StandaloneConditioningMode.AUTO
        val isHeatingAllowed = (mode == StandaloneConditioningMode.HEAT_ONLY || mode == StandaloneConditioningMode.AUTO) &&
                supplyWaterTempTh2 > coolingThreshold

        fanLoopOutput = when {
            coolingLoopOutput > 0 && isCoolingAllowed -> {
                (coolingLoopOutput * multiplier).coerceAtMost(100.0).toInt()
            }

            heatingLoopOutput > 0 && isHeatingAllowed -> {
                (heatingLoopOutput * multiplier).coerceAtMost(100.0).toInt()
            }

            else -> 0
        }
    }

    private fun runAlgorithm(
        equip: HsPipe2Equip,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        configuration: Pipe2Configuration
    ) {

        if ((currentTemp > 0) && canWeRunFan(basicSettings)) {

            if (canWeDoConditioning(basicSettings)) {
                if (isSupplyOppositeToConditioning(
                        basicSettings.conditioningMode,
                        supplyWaterTempTh2,
                        heatingThreshold,
                        coolingThreshold
                    )
                ) {
                    resetWaterValve(equip)
                }
                triggerFanForAuxIfRequired(basicSettings, configuration, equip)
            } else {
                logIt("Conditioning mode is OFF")
                resetConditioning(relayStages, analogOutStages, basicSettings, equip)
            }
        } else {
            resetConditioning(relayStages, analogOutStages, basicSettings, equip)
        }
    }

    private fun isConfigPresent(mapping: HsPipe2RelayMapping) = relayLogicalPoints.containsKey(mapping.ordinal)

    private fun triggerFanForAuxIfRequired(
        basicSettings: BasicSettings, configuration: Pipe2Configuration, equip: HsPipe2Equip
    ) {
        val analogFanType = operateAuxBasedFan(equip, basicSettings)
        runSpecifiedAnalogFanSpeed(equip, analogFanType, configuration.getAnalogOutsConfigurationMapping(), configuration.getFanConfiguration())
    }


    private fun processForWaterSampling(
        equip: HsPipe2Equip, tuner: HyperStatProfileTuners,
        config: Pipe2Configuration, relayStages: HashMap<String, Int>,
        basicSettings: BasicSettings
    ) {
        if (isWaterValveActiveDueToLoop) {
            logIt("Sampling not required, because water valve is active due to loop")
            return
        }

        if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
            resetWaterValve(equip)
            return
        }

        if (!config.isAnyRelayEnabledAssociated(association = HsPipe2RelayMapping.WATER_VALVE.ordinal) &&
            !config.isAnyAnalogOutEnabledAssociated(association = HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)
        ) {
            logIt( "No mapping for water value")
            return
        }

        fun resetIsRequired(): Boolean {
            return ((isConfigPresent(HsPipe2RelayMapping.WATER_VALVE) &&
                    (getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.WATER_VALVE.ordinal]!!).toInt() != 0))
                    || (analogLogicalPoints.containsKey(HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)
                    && (getCurrentLogicalPointStatus(analogLogicalPoints[HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal]!!).toInt() != 0)))
        }

        logIt("waterSamplingStarted Time $waterSamplingStartTime")

        val waitTimeToDoSampling: Int
        val onTimeToDoSampling: Int
        if (supplyWaterTempTh2 in coolingThreshold..heatingThreshold) {
            waitTimeToDoSampling = tuner.waterValveSamplingDuringLoopDeadbandWaitTime
            onTimeToDoSampling = tuner.waterValveSamplingDuringLoopDeadbandOnTime
        } else {
            waitTimeToDoSampling = tuner.waterValveSamplingWaitTime
            onTimeToDoSampling = tuner.waterValveSamplingOnTime
        }

        // added on 05-12-2022 If either one of the tuner value is 0 then we will not do water sampling
        if (waitTimeToDoSampling == 0 || onTimeToDoSampling == 0) {
            //resetting the water valve value value only when the tuner value is zero
            if (resetIsRequired()) {
                waterSamplingStartTime = 0
                lastWaterValveTurnedOnTime = System.currentTimeMillis()
                resetWaterValve(equip)
            }
            logIt( "No water sampling, because tuner value is zero!")
            return
        }

        logIt("waitTimeToDoSampling:  $waitTimeToDoSampling onTimeToDoSampling: $onTimeToDoSampling\n" +
                "Current : ${Date(System.currentTimeMillis())}: Last On:  ${Date(lastWaterValveTurnedOnTime)}"
        )

        if (waterSamplingStartTime == 0L) {
            val minutes = milliToMin(System.currentTimeMillis() - lastWaterValveTurnedOnTime)
            logIt("sampling will start in : ${waitTimeToDoSampling - minutes} current : $minutes")
            if (minutes >= waitTimeToDoSampling) {
                doWaterSampling(relayStages)
            }
        } else {
            val samplingSinceFrom =
                milliToMin(System.currentTimeMillis() - waterSamplingStartTime)
            logIt("Water sampling is running since from $samplingSinceFrom minutes")
            if (samplingSinceFrom >= onTimeToDoSampling) {
                waterSamplingStartTime = 0
                lastWaterValveTurnedOnTime = System.currentTimeMillis()
                resetWaterValve(equip)
                logIt( "Resetting WATER_VALVE to OFF")
            } else {
                relayStages[StatusMsgKeys.WATER_VALVE.name] = 1
            }
        }
    }

    private fun doWaterSampling(
        relayStages: HashMap<String, Int>,
    ) {
        waterSamplingStartTime = System.currentTimeMillis()
        updateLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.WATER_VALVE.ordinal], 1.0)
        updateLogicalPoint(
            analogLogicalPoints[HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal],
            100.0
        )
        relayStages[StatusMsgKeys.WATER_VALVE.name] = 1
        logIt( "Turned ON water valve ")
    }


    private fun operateAnalogOutputs(
        config: Pipe2Configuration, equip: HsPipe2Equip, basicSettings: BasicSettings,
        analogOutStages: HashMap<String, Int>
    ) {
        config.apply {
            listOf(
                Pair(
                    Pipe2AnalogOutConfigs(
                        analogOut1Enabled.enabled, analogOut1Association.associationVal,
                        analogOut1MinMaxConfig, analogOut1FanSpeedConfig
                    ), Port.ANALOG_OUT_ONE
                ),
                Pair(
                    Pipe2AnalogOutConfigs(
                        analogOut2Enabled.enabled, analogOut2Association.associationVal,
                        analogOut2MinMaxConfig, analogOut2FanSpeedConfig
                    ), Port.ANALOG_OUT_TWO
                ),
                Pair(
                    Pipe2AnalogOutConfigs(
                        analogOut3Enabled.enabled, analogOut3Association.associationVal,
                        analogOut3MinMaxConfig, analogOut3FanSpeedConfig
                    ), Port.ANALOG_OUT_THREE
                ),
            ).forEach { (analogOutConfig, port) ->
                if(analogOutConfig.enabled) {

                    val mapping = HsPipe2AnalogOutMapping.values()[analogOutConfig.association]
                    when (mapping) {
                        HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE -> {
                            if (waterSamplingStartTime == 0L && basicSettings.conditioningMode != StandaloneConditioningMode.OFF) {
                                doAnalogWaterValveAction(
                                    port, basicSettings, analogOutStages
                                )
                            }
                        }

                        HsPipe2AnalogOutMapping.DCV_DAMPER -> {
                            doAnalogDCVAction(
                                port, analogOutStages, config.zoneCO2Threshold.currentVal,
                                config.zoneCO2DamperOpeningRate.currentVal,
                                equip
                            )
                        }

                        HsPipe2AnalogOutMapping.FAN_SPEED -> {
                            doAnalogFanAction(
                                port,
                                analogOutConfig.fanSpeed.low.currentVal.toInt(),
                                analogOutConfig.fanSpeed.medium.currentVal.toInt(),
                                analogOutConfig.fanSpeed.high.currentVal.toInt(),
                                equip,
                                basicSettings,
                                fanLoopOutput,
                                isFanGoodRun(doorWindowSensorOpenStatus, equip, heatingLoopOutput, coolingLoopOutput, fanLowVentilationAvailable)
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun operateRelays(
        config: Pipe2Configuration, basicSettings: BasicSettings,
        equip: HsPipe2Equip, userIntents: UserIntents, controllerFactory: HyperStatControlFactory
    ) {
        controllerFactory.addPipe2Controllers(config, waterValveLoop, fanLowVentilationAvailable)
        waterValveLoop.data = waterValveLoop(userIntents).toDouble()
        runControllers(equip, basicSettings)
    }

    private fun runControllers(equip: HsPipe2Equip, basicSettings: BasicSettings) {
        derivedFanLoopOutput.data = equip.fanLoopOutput.readHisVal()
        zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            val result = controller.runController()
            updateRelayStatus(controllerName, result, equip, basicSettings)
        }
    }

    private fun updateRelayStatus(
        controllerName: String, result: Any, equip: HsPipe2Equip, basicSettings: BasicSettings
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

            ControllerNames.WATER_VALVE_CONTROLLER -> {
                if (waterSamplingStartTime == 0L && canWeDoConditioning(basicSettings)
                    && isSupplyOppositeToConditioning(
                        basicSettings.conditioningMode,
                        supplyWaterTempTh2,
                        heatingThreshold,
                        coolingThreshold
                    ).not()
                ) {
                    updateStatus(
                        equip.waterValve,
                        result as Boolean,
                        StatusMsgKeys.WATER_VALVE.name
                    )
                    if (result) {
                        isWaterValveActiveDueToLoop = true
                        lastWaterValveTurnedOnTime = System.currentTimeMillis()
                    }
                }
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

                val isFanGoodToRun = isFanGoodRun(doorWindowSensorOpenStatus, equip, heatingLoopOutput, coolingLoopOutput, fanLowVentilationAvailable)

                fun isStageActive(
                    stage: Int, currentState: Boolean, isLowestStageActive: Boolean
                ): Boolean {
                    val mode = equip.fanOpMode.readPriorityVal().toInt()
                    return if (isFanGoodToRun && mode == StandaloneFanStage.AUTO.ordinal) {
                        (basicSettings.fanMode != StandaloneFanStage.OFF && (currentState || (isLowestStageActive && runFanLowDuringDoorWindow)))
                    } else {
                        checkUserIntentAction(stage)
                    }
                }
                val fanStages = result as List<Pair<Int, Boolean>>

                val highExist = fanStages.find { it.first == HsPipe2RelayMapping.FAN_HIGH_SPEED.ordinal }
                val mediumExist = fanStages.find { it.first == HsPipe2RelayMapping.FAN_MEDIUM_SPEED.ordinal }
                val lowExist =
                    fanStages.find { (it.first == HsPipe2RelayMapping.FAN_LOW_SPEED.ordinal || it.first == HsPipe2RelayMapping.FAN_LOW_VENTILATION.ordinal)}

                var isHighActive = false
                var isMediumActive = false

                if (equip.fanHighSpeed.pointExists() && highExist != null) {
                    isHighActive = isStageActive(highExist.first, highExist.second, lowestStageFanHigh)
                    updateStatus(equip.fanHighSpeed, isHighActive, Stage.FAN_3.displayName)
                }

                if (equip.fanMediumSpeed.pointExists() && mediumExist != null) {
                    isMediumActive = if (isHighActive && isConfigPresent(HsPipe2RelayMapping.FAN_HIGH_SPEED)) false else isStageActive(mediumExist.first, mediumExist.second, lowestStageFanMedium)
                    updateStatus(equip.fanMediumSpeed, isMediumActive, Stage.FAN_2.displayName)
                }

                if ((equip.fanLowSpeed.pointExists() || equip.fanLowSpeedVentilation.pointExists()) && lowExist != null) {
                    val isLowActive = if ((isHighActive && isConfigPresent(HsPipe2RelayMapping.FAN_HIGH_SPEED)) || (isConfigPresent(HsPipe2RelayMapping.FAN_MEDIUM_SPEED) && isMediumActive)) false else isStageActive(lowExist.first, lowExist.second, lowestStageFanLow)
                    updateStatus(
                        if (fanLowVentilationAvailable.readHisVal() > 0) equip.fanLowSpeedVentilation else equip.fanLowSpeed,
                        isLowActive, Stage.FAN_1.displayName)
                }
            }

            ControllerNames.AUX_HEATING_STAGE1 -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode != StandaloneConditioningMode.AUTO
                    && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
                    status = false
                }
                isAuxStage1Active = status
                updateStatus(equip.auxHeatingStage1, status, StatusMsgKeys.AUX_HEATING_STAGE1.name)
            }

            ControllerNames.AUX_HEATING_STAGE2 -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode != StandaloneConditioningMode.AUTO
                    && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
                    status = false
                }
                isAuxStage2Active = status
                updateStatus(equip.auxHeatingStage2, status, StatusMsgKeys.AUX_HEATING_STAGE2.name)
            }

            ControllerNames.FAN_ENABLED -> updateStatus(equip.fanEnable, result, StatusMsgKeys.FAN_ENABLED.name)
            ControllerNames.OCCUPIED_ENABLED -> updateStatus(equip.occupiedEnable, result)
            ControllerNames.HUMIDIFIER_CONTROLLER -> updateStatus(equip.humidifierEnable, result)
            ControllerNames.DEHUMIDIFIER_CONTROLLER -> updateStatus(equip.dehumidifierEnable, result)
            ControllerNames.DAMPER_RELAY_CONTROLLER -> updateStatus(equip.dcvDamper, result, StatusMsgKeys.DCV_DAMPER.name)

            else -> {
                logIt("Unknown controller: $controllerName")
            }
        }
    }

    private fun waterValveLoop(userIntents: UserIntents): Int {
        val isHeatingAvailable = supplyWaterTempTh2 > heatingThreshold
        val isCoolingAvailable = supplyWaterTempTh2 < coolingThreshold

        val zoneNeedsHeating = currentTemp < userIntents.heatingDesiredTemp
        val zoneNeedsCooling = currentTemp > userIntents.coolingDesiredTemp

        return when {
            isHeatingAvailable && zoneNeedsHeating -> heatingLoopOutput
            isCoolingAvailable && zoneNeedsCooling -> coolingLoopOutput

            // No zone demand, but supply is still available
            isHeatingAvailable -> heatingLoopOutput.takeIf { it > 0 } ?: 0
            isCoolingAvailable -> coolingLoopOutput.takeIf { it > 0 } ?: 0

            else -> 0 // No demand, no supply
        }
    }

    private fun resetConditioning(
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        basicSettings: BasicSettings,
        equip: HsPipe2Equip

    ) {
        resetFan(relayStages, analogOutStages, basicSettings)
        resetWaterValve(equip)
        resetAux(relayStages)

        if (isConfigPresent(HsPipe2RelayMapping.FAN_ENABLED)) {
            resetLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.FAN_ENABLED.ordinal]!!)
            relayStages.remove(StatusMsgKeys.FAN_ENABLED.name)
        }
        if (isConfigPresent(HsPipe2RelayMapping.OCCUPIED_ENABLED)) resetLogicalPoint(
            relayLogicalPoints[HsPipe2RelayMapping.OCCUPIED_ENABLED.ordinal]!!
        )

        if (analogLogicalPoints.containsKey(HsPipe2AnalogOutMapping.DCV_DAMPER.ordinal)) resetLogicalPoint(
            analogLogicalPoints[HsPipe2AnalogOutMapping.DCV_DAMPER.ordinal]!!
        )
    }

    private fun resetAux(relayStages: HashMap<String, Int>) {
        if (isConfigPresent(HsPipe2RelayMapping.AUX_HEATING_STAGE1)) {
            resetLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal]!!)
        }
        if (isConfigPresent(HsPipe2RelayMapping.AUX_HEATING_STAGE2)) {
            resetLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal]!!)
        }
        relayStages.remove(HsPipe2RelayMapping.AUX_HEATING_STAGE1.name)
        relayStages.remove(HsPipe2RelayMapping.AUX_HEATING_STAGE2.name)
    }

    private fun resetFan(
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        basicSettings: BasicSettings
    ) {
        if (basicSettings.fanMode == StandaloneFanStage.AUTO || basicSettings.fanMode == StandaloneFanStage.OFF) {
            if (isConfigPresent(HsPipe2RelayMapping.FAN_LOW_SPEED)) {
                resetLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.FAN_LOW_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_1.displayName)
            }
            if (isConfigPresent(HsPipe2RelayMapping.FAN_MEDIUM_SPEED)) {
                resetLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.FAN_MEDIUM_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_2.displayName)
            }
            if (isConfigPresent(HsPipe2RelayMapping.FAN_HIGH_SPEED)) {
                resetLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.FAN_HIGH_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_3.displayName)
            }
            if (isConfigPresent(HsPipe2RelayMapping.FAN_ENABLED)) {
                resetLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.FAN_ENABLED.ordinal]!!)
                relayStages.remove(StatusMsgKeys.FAN_ENABLED.name)
            }
            if (analogLogicalPoints.containsKey(HsPipe2AnalogOutMapping.FAN_SPEED.ordinal)) {
                resetLogicalPoint(analogLogicalPoints[HsPipe2AnalogOutMapping.FAN_SPEED.ordinal]!!)
                analogOutStages.remove(StatusMsgKeys.FAN_SPEED.name)
            }
        }
    }

    private fun resetWaterValve(equip: HsPipe2Equip) {
        if (waterSamplingStartTime == 0L) {
            if (isConfigPresent(HsPipe2RelayMapping.WATER_VALVE)) {
                resetLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.WATER_VALVE.ordinal]!!)
            }
            if (analogLogicalPoints.containsKey(HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)) {
                resetLogicalPoint(analogLogicalPoints[HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal]!!)
            }
            equip.relayStages.remove(StatusMsgKeys.WATER_VALVE.name)
            equip.analogOutStages.remove(StatusMsgKeys.WATER_VALVE.name)
            logIt( "Resetting WATER_VALVE to OFF")
            isWaterValveActiveDueToLoop = false
        }
    }

    private fun doAnalogWaterValveAction(
        port: Port, basicSettings: BasicSettings, analogOutStages: HashMap<String, Int>
    ) {

        if (waterSamplingStartTime == 0L && canWeDoConditioning(basicSettings) && canWeRunFan(basicSettings)
            && isSupplyOppositeToConditioning(
                basicSettings.conditioningMode,
                supplyWaterTempTh2,
                heatingThreshold,
                coolingThreshold
            ).not()
        ) {
            updateLogicalPoint(logicalPointsList[port]!!, waterValveLoop.data)
            if (waterValveLoop.data > 0) {
                analogOutStages[StatusMsgKeys.WATER_VALVE.name] = 1
                isWaterValveActiveDueToLoop = true
                lastWaterValveTurnedOnTime = System.currentTimeMillis()
            }
        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }

    override fun getCurrentTemp(): Double {
        for (nodeAddress in pipe2DeviceMap.keys) {
            return pipe2DeviceMap[nodeAddress]!!.currentTemp.readHisVal()
        }
        return 0.0
    }

    override fun getDisplayCurrentTemp() = averageZoneTemp

    override fun getAverageZoneTemp(): Double {
        var tempTotal = 0.0
        var nodeCount = 0
        pipe2DeviceMap.forEach { (_, device) ->
            if (device.currentTemp.readHisVal() > 0) {
                tempTotal += device.currentTemp.readHisVal()
                nodeCount++
            }
        }
        return if (nodeCount == 0) 0.0 else tempTotal / nodeCount
    }

    fun getProfileDomainEquip(node: Int): HsPipe2Equip = pipe2DeviceMap[node]!!

    override fun getNodeAddresses(): Set<Short?> = pipe2DeviceMap.keys.map { it.toShort() }.toSet()

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        TODO("Not using now")
    }

    fun supplyDirection(): String {
        return if (supplyWaterTempTh2 > heatingThreshold
            || supplyWaterTempTh2 in coolingThreshold..heatingThreshold
        ) {
            "Heating"
        } else {
            "Cooling"
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
            L.TAG_CCU_HSPIPE2,
            "Fan speed multiplier:  ${tuners.analogFanSpeedMultiplier} " +
                    "AuxHeating1Activate: ${tuners.auxHeating1Activate} " +
                    "AuxHeating2Activate: ${tuners.auxHeating2Activate}  " +
                    "waterValveSamplingOnTime: ${tuners.waterValveSamplingOnTime} " +
                    "waterValveSamplingWaitTime : ${tuners.waterValveSamplingWaitTime} \n" +
                    "waterValveSamplingDuringLoopDeadbandOnTime: ${tuners.waterValveSamplingDuringLoopDeadbandOnTime}  " +
                    "waterValveSamplingDuringLoopDeadbandWaitTime : ${tuners.waterValveSamplingDuringLoopDeadbandWaitTime} \n" +
                    "Current Occupancy: ${Occupancy.values()[equip.occupancyMode.readHisVal().toInt()]}\n" +
                    "supplyWaterTempTh2 : $supplyWaterTempTh2 \n" +
                    "Fan Mode : ${settings.fanMode} Conditioning Mode ${settings.conditioningMode} \n" +
                    "heatingThreshold: $heatingThreshold  coolingThreshold : $coolingThreshold \n" +
                    "Current Temp : $currentTemp " +
                    "Desired (Heating: ${userIntents.heatingDesiredTemp} " +
                    "Cooling: ${userIntents.coolingDesiredTemp})\n" +
                    "Loop Outputs: (Heating: $heatingLoopOutput Cooling: $coolingLoopOutput " +
                    "Fan : $fanLoopOutput DCV $dcvLoopOutput) \n"
        )

        logResults(config, L.TAG_CCU_HSPIPE2, logicalPointsList)
    }
}