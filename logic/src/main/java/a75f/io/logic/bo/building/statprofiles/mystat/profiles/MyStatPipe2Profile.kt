package a75f.io.logic.bo.building.statprofiles.mystat.profiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Point
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2RelayMapping
import a75f.io.logic.bo.building.statprofiles.statcontrollers.MyStatControlFactory
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.MyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.MyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.fetchMyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.fetchMyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getMyStatAnalogOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatLogicalPointList
import a75f.io.logic.bo.building.statprofiles.util.getMyStatRelayOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.isMyStatHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMyStatLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.logMsResults
import a75f.io.logic.bo.building.statprofiles.util.milliToMin
import a75f.io.logic.bo.building.statprofiles.util.updateLogicalPoint
import a75f.io.logic.bo.building.statprofiles.util.updateLoopOutputs
import a75f.io.logic.bo.building.statprofiles.util.updateOperatingMode
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.util.uiutils.MyStatUserIntentHandler

/**
 * Created by Manjunath K on 16-01-2025.
 */

class MyStatPipe2Profile: MyStatProfile(L.TAG_CCU_MSPIPE2) {

    private var supplyWaterTempTh2 = 0.0
    private var heatingThreshold = 85.0
    private var coolingThreshold = 65.0

    private val pipe2DeviceMap: MutableMap<Int, MyStatPipe2Equip> = mutableMapOf()

    override lateinit var occupancyStatus: Occupancy
    private lateinit var curState: ZoneState

    private var analogLogicalPoints: HashMap<Int, String> = HashMap()
    private var relayLogicalPoints: HashMap<Int, String> = HashMap()

    override fun getProfileType() = ProfileType.MYSTAT_PIPE2

    override fun updateZonePoints() {
        pipe2DeviceMap.forEach { (nodeAddress, equip) ->
            pipe2DeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as MyStatPipe2Equip
            logIt("Process Pipe2: equipRef =  ${equip.nodeAddress}")
            processPipe2Profile(equip)
        }
    }

    fun addEquip(equipRef: String) {
        val equip = MyStatPipe2Equip(equipRef)
        pipe2DeviceMap[equip.nodeAddress] = equip
    }

    override fun getEquip(): Equip? {
        for (nodeAddress in pipe2DeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    fun processPipe2Profile(equip: MyStatPipe2Equip) {

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

        val config = getMyStatConfiguration(equip.equipRef)
        val myStatTuners = fetchMyStatTuners(equip) as MyStatTuners
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val fanModeSaved = FanModeCacheStorage.getMyStatFanModeCache().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchMyStatBasicSettings(equip)

        logicalPointsList = getMyStatLogicalPointList(equip, config!!)
        relayLogicalPoints = getMyStatRelayOutputPoints(equip)
        analogLogicalPoints = getMyStatAnalogOutputPoints(equip)
        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode
        heatingThreshold = myStatTuners.heatingThreshold
        coolingThreshold = myStatTuners.coolingThreshold

        logIt( "Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        logIt( "After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")

        loopController.initialise(tuners = myStatTuners)
        loopController.dumpLogs()
        handleChangeOfDirection(currentTemp, userIntents)
        updateOperatingMode(currentTemp, averageDesiredTemp, basicSettings.conditioningMode, equip.operatingMode)

        resetEquip(equip)
        evaluateLoopOutputs(userIntents, basicSettings, myStatTuners, config, equip)
        updateOccupancyDetection(equip)

        doorWindowSensorOpenStatus = runForDoorWindowSensor(config, equip, equip.analogOutStages, equip.relayStages)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)
        supplyWaterTempTh2 = equip.leavingWaterTemperature.readHisVal()

        if (occupancyStatus == Occupancy.WINDOW_OPEN) resetLoopOutputs()
        updateLoopOutputs(
            coolingLoopOutput, equip.coolingLoopOutput,
            heatingLoopOutput, equip.heatingLoopOutput,
            fanLoopOutput, equip.fanLoopOutput,
            dcvLoopOutput, equip.dcvLoopOutput,
        )

        operateRelays(config as MyStatPipe2Configuration, basicSettings, equip, userIntents)
        handleAnalogOutState(config, equip, basicSettings, equip.analogOutStages, userIntents)
        processForWaterSampling(equip, myStatTuners, config, equip.relayStages, basicSettings)
        runAlgorithm(equip, basicSettings, equip.relayStages, equip.analogOutStages, config)

        // Run the title 24 fan operation after the reset of all PI output is done
        doFanOperationTitle24(basicSettings, equip, config)

        updateOperatingMode(currentTemp, averageDesiredTemp, basicSettings.conditioningMode, equip.operatingMode)
        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())
        logIt(
            "Fan speed multiplier:  ${myStatTuners.analogFanSpeedMultiplier} " +
                    "AuxHeating1Activate: ${myStatTuners.auxHeating1Activate} " +
                    "waterValveSamplingOnTime: ${myStatTuners.waterValveSamplingOnTime}  waterValveSamplingWaitTime : ${myStatTuners.waterValveSamplingWaitTime} \n" +
                    "waterValveSamplingDuringLoopDeadbandOnTime: ${myStatTuners.waterValveSamplingDuringLoopDeadbandOnTime}  waterValveSamplingDuringLoopDeadbandWaitTime : ${myStatTuners.waterValveSamplingDuringLoopDeadbandWaitTime} \n" +
                    "Current Occupancy: ${Occupancy.values()[equip.occupancyMode.readHisVal().toInt()]} \n" +
                    "supplyWaterTempTh2 : $supplyWaterTempTh2 \n" +
                    "Fan Mode : ${basicSettings.fanMode} Conditioning Mode ${basicSettings.conditioningMode} \n" +
                    "heatingThreshold: $heatingThreshold  coolingThreshold : $coolingThreshold \n" +
                    "Current Temp : $currentTemp Desired (Heating: ${userIntents.heatingDesiredTemp}" +
                    " Cooling: ${userIntents.coolingDesiredTemp})\n" +
                    "Loop Outputs: (Heating Loop: $heatingLoopOutput Cooling Loop: $coolingLoopOutput Fan Loop: $fanLoopOutput DCVLoop: $dcvLoopOutput) \n"
        )
        logMsResults(config, L.TAG_CCU_MSPIPE2, logicalPointsList)
        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY
        MyStatUserIntentHandler.updateMyStatStatus(temperatureState, equip, L.TAG_CCU_MSPIPE2)
        if (occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        logIt( "----------------------------------------------------------")
    }


    private fun evaluateLoopOutputs(
        userIntents: UserIntents,
        basicSettings: MyStatBasicSettings,
        tuner: MyStatTuners,
        config: MyStatConfiguration,
        equip: MyStatEquip
    ) {
        this.resetLoopOutputs()

        when (state) {
            ZoneState.COOLING -> evaluateCoolingLoop(userIntents)
            ZoneState.HEATING -> evaluateHeatingLoop(userIntents)
            else -> logIt("Zone is in deadband")
        }
        evaluateFanOutput(basicSettings, tuner)
        evaluateDcvLoop(equip, config)
    }

    override fun evaluateFanOutput(
        basicSettings: MyStatBasicSettings,
        tuners: MyStatTuners
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

    private fun runForDoorWindowSensor(
        config: MyStatConfiguration, equip: MyStatEquip,
        analogOutStages: HashMap<String, Int>, relayStages: HashMap<String, Int>
    ): Boolean {

        val isDoorOpen = isDoorOpenState(config, equip)
        logIt(
            " is Door Open ? $isDoorOpen")
        if (isDoorOpen) {
            resetLoopOutputs()
            resetLogicalPoints()
            analogOutStages.clear()
            relayStages.clear()
        }
        return isDoorOpen
    }

    private fun operateRelays(
        config: MyStatPipe2Configuration,
        basicSettings: MyStatBasicSettings,
        equip: MyStatPipe2Equip,
        userIntents: UserIntents
    ) {
        val controllerFactory = MyStatControlFactory(equip)
        controllerFactory.addControllers(config)
        runControllers(equip, basicSettings, config, userIntents)
    }

    private fun runControllers(
        equip: MyStatPipe2Equip, basicSettings: MyStatBasicSettings,
        config: MyStatPipe2Configuration, userIntents: UserIntents
    ) {
        equip.waterValveLoop.data = waterValveLoop(userIntents).toDouble()
        equip.derivedFanLoopOutput.data = equip.fanLoopOutput.readHisVal()
        equip.zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        equip.stageDownTimer.data = equip.mystatStageUpTimerCounter.readPriorityVal()
        equip.stageUpTimer.data = equip.mystatStageUpTimerCounter.readPriorityVal()

        equip.controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            val result = controller.runController()
            updateRelayStatus(controllerName, result, equip, basicSettings, config)
        }
    }
    private fun updateRelayStatus(
        controllerName: String, result: Any, equip: MyStatPipe2Equip,
        basicSettings: MyStatBasicSettings, config: MyStatPipe2Configuration
    ) {

        fun updateRelayStage(stageName: String, isActive: Boolean, point: Point) {
            if (point.pointExists()) {
                val status = if (isActive) 1.0 else 0.0
                if (isActive) {
                    equip.relayStages[stageName] = status.toInt()
                } else {
                    equip.relayStages.remove(stageName)
                }
                point.writeHisVal(status)
            }
        }

        fun updateStatus(point: Point, result: Any, status: String? = null) {
            point.writeHisVal(if (result as Boolean) 1.0 else 0.0)
            if (status != null && result) {
                equip.relayStages[status] = 1
            } else {
                equip.relayStages.remove(status)
            }
        }

        when (controllerName) {

            ControllerNames.WATER_VALVE_CONTROLLER -> {
                if (equip.waterSamplingStartTime == 0L && basicSettings.conditioningMode != StandaloneConditioningMode.OFF) {
                    updateRelayStage(
                        StatusMsgKeys.WATER_VALVE.name,
                        result as Boolean,
                        equip.waterValve
                    )
                }
            }

            ControllerNames.FAN_SPEED_CONTROLLER -> {

                runTitle24Rule(config)

                fun checkUserIntentAction(stage: Int): Boolean {
                    val mode = equip.fanOpMode
                    return when (stage) {
                        0 -> isMyStatLowUserIntentFanMode(mode)
                        2 -> isMyStatHighUserIntentFanMode(mode)
                        else -> false
                    }
                }
                val isFanGoodToRun = isFanGoodRun(doorWindowSensorOpenStatus)

                fun isStageActive(
                    stage: Int, currentState: Boolean, isLowestStageActive: Boolean
                ): Boolean {
                    val mode = equip.fanOpMode.readPriorityVal().toInt()
                    return if (isFanGoodToRun && mode == MyStatFanStages.AUTO.ordinal) {
                        (basicSettings.fanMode != MyStatFanStages.OFF && currentState ||
                                (fanEnabledStatus && fanLoopOutput > 0 && isLowestStageActive) ||
                                (isLowestStageActive && runFanLowDuringDoorWindow))
                    } else {
                        checkUserIntentAction(stage)
                    }
                }

                val fanStages = result as List<Pair<Int, Boolean>>

                val highExist =
                    fanStages.find { it.first == MyStatPipe2RelayMapping.FAN_HIGH_SPEED.ordinal }
                val lowExist =
                    fanStages.find { it.first == MyStatPipe2RelayMapping.FAN_LOW_SPEED.ordinal }

                var isHighActive = false

                if (highExist != null) {
                    isHighActive = isStageActive(highExist.first, highExist.second, lowestStageFanHigh)
                    updateRelayStage(Stage.FAN_2.displayName, isHighActive, equip.fanHighSpeed)
                }

                if (lowExist != null) {
                    val isLowActive = if (isHighActive) false else isStageActive(lowExist.first, lowExist.second, lowestStageFanLow)
                    updateRelayStage(Stage.FAN_1.displayName, isLowActive, equip.fanLowSpeed)
                }
            }
            ControllerNames.AUX_HEATING_STAGE1 -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode != StandaloneConditioningMode.AUTO
                    && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
                    status = false
                }
                updateStatus(equip.auxHeatingStage1, status, StatusMsgKeys.AUX_HEATING_STAGE1.name)
            }
            ControllerNames.FAN_ENABLED -> updateStatus(equip.fanEnable, result)
            ControllerNames.OCCUPIED_ENABLED -> updateStatus(equip.occupiedEnable, result)
            ControllerNames.HUMIDIFIER_CONTROLLER -> updateStatus(equip.humidifierEnable, result)
            ControllerNames.DEHUMIDIFIER_CONTROLLER -> updateStatus(equip.dehumidifierEnable, result)
            ControllerNames.DAMPER_RELAY_CONTROLLER -> updateStatus(equip.dcvDamper, result)
            else -> {
                logIt("Unknown controller: $controllerName")
            }
        }
    }

    private fun isFanGoodRun(isDoorWindowOpen: Boolean): Boolean {
        return if (isDoorWindowOpen || heatingLoopOutput > 0 && supplyWaterTempTh2 > coolingThreshold) {
            // If current direction is heating then check allow only when valve or heating is available
            (isConfigPresent(MyStatPipe2RelayMapping.WATER_VALVE)
                    || isConfigPresent(MyStatPipe2RelayMapping.AUX_HEATING_STAGE1))
        } else if (isDoorWindowOpen || coolingLoopOutput > 0 && supplyWaterTempTh2 < coolingThreshold) {
            isConfigPresent(MyStatPipe2RelayMapping.WATER_VALVE)
        } else {
            false
        }
    }

    private fun runTitle24Rule(config: MyStatPipe2Configuration) {
        resetFanLowestFanStatus()
        fanEnabledStatus =
            config.isAnyRelayEnabledAssociated(association = MyStatCpuRelayMapping.FAN_ENABLED.ordinal)
        val lowestStage = config.getLowestFanSelected()
        when (lowestStage) {
            MyStatPipe2RelayMapping.FAN_LOW_SPEED -> lowestStageFanLow = true
            MyStatPipe2RelayMapping.FAN_HIGH_SPEED -> lowestStageFanHigh = true
            else -> {}
        }
    }

    private fun waterValveLoop(userIntents: UserIntents): Int {
        val supplyWaterTempTh2AboveHeating = supplyWaterTempTh2 > heatingThreshold
        val supplyWaterTempTh2BelowCooling = supplyWaterTempTh2 < coolingThreshold

        val zoneExpectingHeat = currentTemp < userIntents.heatingDesiredTemp
        val zoneExpectingCool = currentTemp > userIntents.coolingDesiredTemp

        if (supplyWaterTempTh2AboveHeating && zoneExpectingHeat) {
            // Supply heating
            return heatingLoopOutput
        } else if (supplyWaterTempTh2BelowCooling && zoneExpectingCool) {
            // Supply cooling
            return coolingLoopOutput
        } else {
            // No supply
            if (supplyWaterTempTh2AboveHeating) {
                return if (heatingLoopOutput > 0) heatingLoopOutput else 0
            } else if (supplyWaterTempTh2BelowCooling) {
                return if (coolingLoopOutput > 0) coolingLoopOutput else 0
            }
        }
        return 0
    }


    private fun handleAnalogOutState(
        config: MyStatPipe2Configuration,
        equip: MyStatPipe2Equip,
        basicSettings: MyStatBasicSettings,
        analogOutStages: HashMap<String, Int>,
        userIntents: UserIntents
    ) {
        if (config.analogOut1Enabled.enabled) {
            val analogMapping = MyStatPipe2AnalogOutMapping.values()
                .find { it.ordinal == config.analogOut1Association.associationVal }
            when (analogMapping) {
                MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE -> {
                    if (equip.waterSamplingStartTime == 0L && basicSettings.conditioningMode != StandaloneConditioningMode.OFF) {
                        equip.waterValveLoop.data = waterValveLoop(userIntents).toDouble()
                        if (basicSettings.fanMode != MyStatFanStages.OFF) {
                            updateLogicalPoint(
                                logicalPointsList[Port.ANALOG_OUT_ONE]!!,
                                equip.waterValveLoop.data
                            )
                            if (equip.waterValveLoop.data > 0) analogOutStages[StatusMsgKeys.WATER_VALVE.name] =
                                1
                        } else {
                            updateLogicalPoint(logicalPointsList[Port.ANALOG_OUT_ONE]!!, 0.0)
                        }
                    }
                }

                MyStatPipe2AnalogOutMapping.DCV_DAMPER_MODULATION -> {
                    doAnalogDCVAction(
                        Port.ANALOG_OUT_ONE, analogOutStages, config.co2Threshold.currentVal,
                        config.co2DamperOpeningRate.currentVal,
                        isDoorOpenState(config, equip), equip
                    )
                }

                MyStatPipe2AnalogOutMapping.FAN_SPEED -> {
                    doAnalogFanAction(
                        Port.ANALOG_OUT_ONE, config.analogOut1FanSpeedConfig.low.currentVal.toInt(),
                        config.analogOut1FanSpeedConfig.high.currentVal.toInt(),
                        basicSettings.fanMode, basicSettings.conditioningMode,
                        fanLoopOutput, analogOutStages, isFanGoodRun(doorWindowSensorOpenStatus)
                    )
                }

                else -> {}
            }
        }

    }


    private fun runAlgorithm(
        equip: MyStatPipe2Equip,
        basicSettings: MyStatBasicSettings,
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        configuration: MyStatPipe2Configuration
    ) {

        if ((currentTemp > 0) && (basicSettings.fanMode != MyStatFanStages.OFF)) {
            when (basicSettings.conditioningMode) {
                StandaloneConditioningMode.AUTO -> {
                    if (supplyWaterTempTh2 > heatingThreshold || supplyWaterTempTh2 in coolingThreshold..heatingThreshold)
                        doHeatOnly(basicSettings, configuration, equip)
                    else if (supplyWaterTempTh2 < coolingThreshold)
                        doCoolOnly(basicSettings, configuration, equip)
                }

                StandaloneConditioningMode.COOL_ONLY -> {
                    doCoolOnly(basicSettings, configuration, equip)
                }

                StandaloneConditioningMode.HEAT_ONLY -> {
                    doHeatOnly(basicSettings, configuration, equip)
                }

                else -> {
                    logIt("Conditioning mode is OFF")
                    resetConditioning(relayStages, analogOutStages, basicSettings, equip)
                }
            }
        } else {
            resetConditioning(relayStages, analogOutStages, basicSettings, equip)
        }
    }


    private fun doHeatOnly(
        basicSettings: MyStatBasicSettings,
        configuration: MyStatPipe2Configuration,
        equip: MyStatPipe2Equip
    ) {
        logIt("doHeatOnly: mode ")
        if (supplyWaterTempTh2 < coolingThreshold) {
            resetWaterValve(equip)
        }
        triggerFanForAuxIfRequired(basicSettings, configuration, equip)
    }

    private fun doCoolOnly(
        basicSettings: MyStatBasicSettings,
        configuration: MyStatPipe2Configuration,
        equip: MyStatPipe2Equip
    ) {
        logIt("doCoolOnly: mode ")

        if (supplyWaterTempTh2 > heatingThreshold) {
            resetWaterValve(equip)
        }
        triggerFanForAuxIfRequired(basicSettings, configuration, equip)
    }


    private fun triggerFanForAuxIfRequired(
        basicSettings: MyStatBasicSettings,
        configuration: MyStatPipe2Configuration,
        equip: MyStatPipe2Equip
    ) {
        if (basicSettings.fanMode == MyStatFanStages.AUTO) {

            resetFanIfRequired(equip, basicSettings)

            if (isAux1Exists() && aux1Active(equip)) {
                resetFan(equip.relayStages, equip.analogOutStages, basicSettings)
                operateAuxBasedOnFan(equip.relayStages)
                runSpecificAnalogFanSpeed(configuration, MyStatFanSpeed.HIGH, equip.analogOutStages)
            }
        }
    }

    private fun resetFanIfRequired(equip: MyStatPipe2Equip, basicSettings: MyStatBasicSettings) {
        if ((isAux1Exists() && aux1Active(equip))) {
            resetFan(equip.relayStages, equip.analogOutStages, basicSettings)
        }
    }

    private fun isAux1Exists() = isConfigPresent(MyStatPipe2RelayMapping.AUX_HEATING_STAGE1)

    private fun aux1Active(equip: MyStatPipe2Equip) = equip.auxHeatingStage1.readHisVal() > 0

    private fun runSpecificAnalogFanSpeed(
        config: MyStatPipe2Configuration,
        fanSpeed: MyStatFanSpeed,
        analogOutStages: HashMap<String, Int>
    ) {
        if (config.analogOut1Enabled.enabled && config.analogOut1Association.associationVal == MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal) {

            val fanSpeedValue = when (fanSpeed) {
                MyStatFanSpeed.HIGH -> config.analogOut1FanSpeedConfig.high.currentVal
                MyStatFanSpeed.LOW -> config.analogOut1FanSpeedConfig.low.currentVal
                else -> 0.0
            }
            updateLogicalPoint(logicalPointsList[Port.ANALOG_OUT_ONE]!!, fanSpeedValue)
            analogOutStages[StatusMsgKeys.FAN_SPEED.name] = 1
        }
    }


    private fun processForWaterSampling(
        equip: MyStatPipe2Equip, tuner: MyStatTuners,
        config: MyStatPipe2Configuration, relayStages: HashMap<String, Int>,
        basicSettings: MyStatBasicSettings
    ) {

        if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
            resetWaterValve(equip)
            return
        }

        if (!config.isAnyRelayEnabledAssociated(association = MyStatPipe2RelayMapping.WATER_VALVE.ordinal) &&
            !config.isAnalogEnabledAssociated(MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)
        ) {
            logIt("No mapping for water value")
            return
        }

        fun resetIsRequired(): Boolean {
            return ((isConfigPresent(MyStatPipe2RelayMapping.WATER_VALVE) &&
                    (getCurrentLogicalPointStatus(relayLogicalPoints[MyStatPipe2RelayMapping.WATER_VALVE.ordinal]!!).toInt() != 0))
                    || (analogLogicalPoints.containsKey(MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)
                    && (getCurrentLogicalPointStatus(analogLogicalPoints[MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal]!!).toInt() != 0)))
        }

        logIt("waterSamplingStarted Time " + equip.waterSamplingStartTime)

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
                equip.waterSamplingStartTime = 0
                equip.lastWaterValveTurnedOnTime = System.currentTimeMillis()
                resetWaterValve(equip)
            }
            logIt("No water sampling, because tuner value is zero!")
            return
        }
        logIt("waitTimeToDoSampling:  $waitTimeToDoSampling onTimeToDoSampling: $onTimeToDoSampling\n" +
                    "Current : ${System.currentTimeMillis()}: Last On: ${equip.lastWaterValveTurnedOnTime}"
        )

        if (equip.waterSamplingStartTime == 0L) {
            val minutes = milliToMin(System.currentTimeMillis() - equip.lastWaterValveTurnedOnTime)
            logIt(
                "sampling will start in : ${waitTimeToDoSampling - minutes} current : $minutes"
            )
            if (minutes >= waitTimeToDoSampling) {
                doWaterSampling(equip, relayStages)
            }
        } else {
            val samplingSinceFrom =
                milliToMin(System.currentTimeMillis() - equip.waterSamplingStartTime)
            logIt(
                "Water sampling is running since from $samplingSinceFrom minutes"
            )
            if (samplingSinceFrom >= onTimeToDoSampling) {
                equip.waterSamplingStartTime = 0
                equip.lastWaterValveTurnedOnTime = System.currentTimeMillis()
                resetWaterValve(equip)
                logIt( "Resetting WATER_VALVE to OFF")
            } else {
                relayStages[StatusMsgKeys.WATER_VALVE.name] = 1
            }
        }
    }

    private fun doWaterSampling(
        equip: MyStatPipe2Equip,
        relayStages: HashMap<String, Int>,
    ) {
        equip.waterSamplingStartTime = System.currentTimeMillis()
        updateLogicalPoint(relayLogicalPoints[MyStatPipe2RelayMapping.WATER_VALVE.ordinal], 1.0)
        updateLogicalPoint(analogLogicalPoints[MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal], 100.0)
        relayStages[StatusMsgKeys.WATER_VALVE.name] = 1
        logIt( "Turned ON water valve ")
    }

    private fun resetFan(
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        basicSettings: MyStatBasicSettings
    ) {
        if (basicSettings.fanMode == MyStatFanStages.AUTO || basicSettings.fanMode == MyStatFanStages.OFF) {
            if (isConfigPresent(MyStatPipe2RelayMapping.FAN_LOW_SPEED)) {
                resetLogicalPoint(relayLogicalPoints[MyStatPipe2RelayMapping.FAN_LOW_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_1.displayName)
            }
            if (isConfigPresent(MyStatPipe2RelayMapping.FAN_HIGH_SPEED)) {
                resetLogicalPoint(relayLogicalPoints[MyStatPipe2RelayMapping.FAN_HIGH_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_2.displayName)
            }
            if (analogLogicalPoints.containsKey(MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal)) {
                resetLogicalPoint(analogLogicalPoints[MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal]!!)
                analogOutStages.remove(StatusMsgKeys.FAN_SPEED.name)
            }
        }
    }

    private fun resetWaterValve(equip: MyStatPipe2Equip) {
        if (equip.waterSamplingStartTime == 0L) {
            if (isConfigPresent(MyStatPipe2RelayMapping.WATER_VALVE)) {
                resetLogicalPoint(relayLogicalPoints[MyStatPipe2RelayMapping.WATER_VALVE.ordinal]!!)
            }
            if (analogLogicalPoints.containsKey(MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)) {
                resetLogicalPoint(analogLogicalPoints[MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal]!!)
            }
            equip.relayStages.remove(StatusMsgKeys.WATER_VALVE.name)
            equip.analogOutStages.remove(StatusMsgKeys.WATER_VALVE.name)
            logIt( "Resetting WATER_VALVE to OFF")
        }
    }

    private fun resetAux(relayStages: HashMap<String, Int>) {
        logIt( "Resetting Aux")
        Thread.dumpStack()
        if (isConfigPresent(MyStatPipe2RelayMapping.AUX_HEATING_STAGE1)) {
            resetLogicalPoint(relayLogicalPoints[MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal]!!)
        }
        relayStages.remove(MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.name)
    }

    // New requirement for aux and fan operations If we do not have fan then no aux
    private fun operateAuxBasedOnFan(relayStages: HashMap<String, Int>) {
        var stage = MyStatPipe2RelayMapping.AUX_HEATING_STAGE1
        var state = 0
        var fanStatusMessage: Stage? = null
        if (isConfigPresent(MyStatPipe2RelayMapping.FAN_HIGH_SPEED)) {
            stage = MyStatPipe2RelayMapping.FAN_HIGH_SPEED
            state = 1
            fanStatusMessage = Stage.FAN_2
        } else if (isConfigPresent(MyStatPipe2RelayMapping.FAN_LOW_SPEED)) {
            stage = MyStatPipe2RelayMapping.FAN_LOW_SPEED
            state = 1
            fanStatusMessage = Stage.FAN_1
        } else if (analogLogicalPoints.containsKey(MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal)
            || isConfigPresent(MyStatPipe2RelayMapping.FAN_ENABLED)) {
            stage = MyStatPipe2RelayMapping.FAN_ENABLED
            state = 1
        }
        if (state == 0) {
            resetAux(relayStages)
        } else {
            if (stage != MyStatPipe2RelayMapping.FAN_ENABLED) {
                updateLogicalPoint(relayLogicalPoints[stage.ordinal]!!, 1.0)
                relayStages[fanStatusMessage!!.displayName] = 1
            }
        }

    }

    private fun doFanOperationTitle24(
        basicSettings: MyStatBasicSettings,
        equip: MyStatPipe2Equip,
        configuration: MyStatPipe2Configuration
    ) {
        if (basicSettings.fanMode == MyStatFanStages.AUTO && runFanLowDuringDoorWindow) {
            val lowestStage = configuration.getLowestFanSelected()
            if (lowestStage == null) resetFan(equip.relayStages, equip.analogOutStages, basicSettings)
            resetFanLowestFanStatus()
            when (lowestStage) {
                MyStatPipe2RelayMapping.FAN_LOW_SPEED -> lowestStageFanLow = true
                MyStatPipe2RelayMapping.FAN_HIGH_SPEED -> lowestStageFanHigh = true
                else -> {
                    // Do nothing
                }
            }
        }
    }
    private fun isConfigPresent(mapping: MyStatPipe2RelayMapping) = relayLogicalPoints.containsKey(mapping.ordinal)

    private fun resetConditioning(
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        basicSettings: MyStatBasicSettings,
        equip: MyStatPipe2Equip
    ) {
        // Reset Relay
        resetFan(relayStages, analogOutStages, basicSettings)
        resetWaterValve(equip)
        resetAux(relayStages)

        if (isConfigPresent(MyStatPipe2RelayMapping.FAN_ENABLED)) resetLogicalPoint(
            relayLogicalPoints[MyStatPipe2RelayMapping.FAN_ENABLED.ordinal]!!
        )
        if (isConfigPresent(MyStatPipe2RelayMapping.OCCUPIED_ENABLED)) resetLogicalPoint(
            relayLogicalPoints[MyStatPipe2RelayMapping.OCCUPIED_ENABLED.ordinal]!!
        )
        if (isConfigPresent(MyStatPipe2RelayMapping.DCV_DAMPER)) resetLogicalPoint(
            relayLogicalPoints[MyStatPipe2RelayMapping.DCV_DAMPER.ordinal]!!
        )
        if (analogLogicalPoints.containsKey(MyStatPipe2AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal)) resetLogicalPoint(
            analogLogicalPoints[MyStatPipe2AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal]!!
        )
        analogOutStages.remove(StatusMsgKeys.DCV_DAMPER.name)
    }

    fun supplyDirection(): String {
        return if (supplyWaterTempTh2 > heatingThreshold
            || supplyWaterTempTh2 in coolingThreshold..heatingThreshold) {
            "Heating"
        } else {
            "Cooling"
        }
    }
    
    override fun isDoorOpenState(config: MyStatConfiguration, equip: MyStatEquip): Boolean {
        return (equip.doorWindowSensorInput.readHisVal() == 1.0)
    }

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        TODO("Not yet implemented")
    }

    fun getProfileDomainEquip(node: Int): MyStatPipe2Equip = pipe2DeviceMap[node]!!

    override fun getNodeAddresses(): Set<Short?> = pipe2DeviceMap.keys.map { it.toShort() }.toSet()

    override fun getCurrentTemp(): Double {
        for (nodeAddress in pipe2DeviceMap.keys) {
            return pipe2DeviceMap[nodeAddress]!!.currentTemp.readHisVal()
        }
        return 0.0
    }
}