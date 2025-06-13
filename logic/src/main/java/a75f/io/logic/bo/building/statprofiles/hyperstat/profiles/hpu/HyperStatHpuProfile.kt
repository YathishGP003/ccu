package a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.hpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Point
import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
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
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.AnalogInputAssociation
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HpuAnalogOutConfigs
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsCpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsHpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsHpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Th2InputAssociation
import a75f.io.logic.bo.building.statprofiles.statcontrollers.HyperStatControlFactory
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings
import a75f.io.logic.bo.building.statprofiles.util.FanConfig
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.FanSpeed
import a75f.io.logic.bo.building.statprofiles.util.HyperStatProfileTuners
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.canWeRunFan
import a75f.io.logic.bo.building.statprofiles.util.fetchBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.fetchHyperStatTuners
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getHSAnalogOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.getHSLogicalPointList
import a75f.io.logic.bo.building.statprofiles.util.getHSRelayStatus
import a75f.io.logic.bo.building.statprofiles.util.getHsConfiguration
import a75f.io.logic.bo.building.statprofiles.util.isHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMediumUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.logResults
import a75f.io.logic.bo.building.statprofiles.util.updateLoopOutputs
import a75f.io.logic.bo.building.statprofiles.util.updateOccupancyDetection
import a75f.io.logic.bo.building.statprofiles.util.updateOperatingMode
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.util.uiutils.HyperStatUserIntentHandler

/**
 * Created by Manjunath K on 02-01-2023.
 */

class HyperStatHpuProfile : HyperStatProfile(L.TAG_CCU_HSHPU) {

    private val hpuDeviceMap: MutableMap<Int, HpuV2Equip> = mutableMapOf()

    override fun getProfileType() = ProfileType.HYPERSTAT_HEAT_PUMP_UNIT

    override fun updateZonePoints() {
        hpuDeviceMap.forEach { (nodeAddress, equip) ->
            hpuDeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as HpuV2Equip
            logIt("Process HPU Equip: node ${equip.nodeAddress} equipRef =  ${equip.equipRef}")
            processHyperStatHpuProfile(equip)
        }
    }

    fun addEquip(equipRef: String) {
        val equip = HpuV2Equip(equipRef)
        hpuDeviceMap[equip.nodeAddress] = equip
    }

    // Run the profile logic and algorithm for an equip.
    fun processHyperStatHpuProfile(equip: HpuV2Equip) {

        if (Globals.getInstance().isTestMode) {
            logIt("Test mode is on: ${equip.nodeAddress}")
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

        logicalPointsList = getHSLogicalPointList(equip, config!!)

        val relayOutputPoints = getHSRelayStatus(equip)
        val analogOutputPoints = getHSAnalogOutputPoints(equip)

        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode

        val hyperStatTuners = fetchHyperStatTuners(equip) as HyperStatProfileTuners
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val fanModeSaved = FanModeCacheStorage.getHyperStatFanModeCache().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchBasicSettings(equip)

        logIt("Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        logIt("After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")

        loopController.initialise(tuners = hyperStatTuners)
        loopController.dumpLogs()

        handleChangeOfDirection(currentTemp, userIntents)
        resetEquip(equip)
        evaluateLoopOutputs(userIntents, basicSettings, hyperStatTuners, config, equip)
        updateOccupancyDetection(equip)

        doorWindowSensorOpenStatus = runForDoorWindowSensor(config, equip)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)

        if (occupancyStatus == Occupancy.WINDOW_OPEN) resetLoopOutputs()
        runForKeyCardSensor(config, equip)
        updateLoopOutputs(
            coolingLoopOutput, equip.coolingLoopOutput,
            heatingLoopOutput, equip.heatingLoopOutput,
            fanLoopOutput, equip.fanLoopOutput,
            dcvLoopOutput, equip.dcvLoopOutput,
            isHpuProfile = true,
            compressorLoopOutput, equip.compressorLoopOutput
        )
        if (basicSettings.fanMode != StandaloneFanStage.OFF) {
            operateRelays(config as HpuConfiguration, basicSettings, equip)
            operateAnalogOutputs(
                config,
                equip,
                basicSettings,
                relayOutputPoints
            )
            if (basicSettings.fanMode == StandaloneFanStage.AUTO) {
                runFanOperationBasedOnAuxStages(
                    equip.relayStages,
                    equip.analogOutStages,
                    config,
                    relayOutputPoints,
                    analogOutputPoints
                )
            }
        } else {
            resetLogicalPoints()
        }

        updateOperatingMode(
            currentTemp,
            averageDesiredTemp,
            basicSettings.conditioningMode,
            equip.operatingMode
        )
        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())

        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState =
            ZoneTempState.EMERGENCY
        if (occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        printStatus(hyperStatTuners, basicSettings,userIntents,equip,config)
        HyperStatUserIntentHandler.updateHyperStatStatus(temperatureState, equip, L.TAG_CCU_HSHPU)
        logIt("----------------------------------------------------------")
    }

    private fun runFanOperationBasedOnAuxStages(
            relayStages: HashMap<String, Int>, analogOutStages: HashMap<String, Int>,
            config: HpuConfiguration, relayOutputPoints: HashMap<Int, String>, analogOutputPoints: HashMap<Int, String>
    ) {
        val (aux1AvailableAndActive, aux2AvailableAndActive, isAnalogFanAvailable) = Triple(
                isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE1, relayOutputPoints),
                isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE2, relayOutputPoints),
                analogOutputPoints.containsKey(HsHpuAnalogOutMapping.FAN_SPEED.ordinal)
        )

        logIt("Aux Based fan : aux1AvailableAndActive $aux1AvailableAndActive aux2AvailableAndActive $aux2AvailableAndActive isAnalogFanAvailable $isAnalogFanAvailable")
        if (aux2AvailableAndActive) operateAuxBasedOnFan(HsHpuRelayMapping.AUX_HEATING_STAGE2, relayStages, relayOutputPoints, isAnalogFanAvailable)
        if (aux1AvailableAndActive) operateAuxBasedOnFan(HsHpuRelayMapping.AUX_HEATING_STAGE1, relayStages, relayOutputPoints, isAnalogFanAvailable)

        // Run the fan speed control if either aux1 or aux2 is available and active
        if ((aux1AvailableAndActive) || (aux2AvailableAndActive)) {
            runSpecificAnalogFanSpeed(config, analogOutStages, relayOutputPoints, analogOutputPoints)
        }
    }


    // New requirement for aux and fan operations If we do not have fan then no aux
    private fun operateAuxBasedOnFan(
        association: HsHpuRelayMapping, relayStages: HashMap<String, Int>,
        relayOutputPoints: HashMap<Int, String>, isAnalogFanExist: Boolean
    ) {

        fun auxResetRequired(): Boolean {
            return (!relayOutputPoints.containsKey(HsHpuRelayMapping.FAN_LOW_SPEED.ordinal) &&
                    !relayOutputPoints.containsKey(HsHpuRelayMapping.FAN_MEDIUM_SPEED.ordinal) &&
                    !relayOutputPoints.containsKey(HsHpuRelayMapping.FAN_HIGH_SPEED.ordinal) &&
                    !relayOutputPoints.containsKey(HsHpuRelayMapping.FAN_ENABLED.ordinal) &&
                    !isAnalogFanExist)
        }

        fun getFanStage(mapping: HsHpuRelayMapping): Stage? {
            return when (mapping) {
                HsHpuRelayMapping.FAN_LOW_SPEED -> Stage.FAN_1
                HsHpuRelayMapping.FAN_MEDIUM_SPEED -> Stage.FAN_2
                HsHpuRelayMapping.FAN_HIGH_SPEED -> Stage.FAN_3
                HsHpuRelayMapping.FAN_ENABLED -> Stage.FAN_4
                else -> null
            }
        }

        fun getAvailableFanSpeed(relayOutputPoints: HashMap<Int, String>) = Triple(
            relayOutputPoints.containsKey(HsHpuRelayMapping.FAN_LOW_SPEED.ordinal),
            relayOutputPoints.containsKey(HsHpuRelayMapping.FAN_MEDIUM_SPEED.ordinal),
            relayOutputPoints.containsKey(HsHpuRelayMapping.FAN_HIGH_SPEED.ordinal)
        )

        val (lowAvailable, mediumAvailable, highAvailable) = getAvailableFanSpeed(relayOutputPoints)
        val fanEnabledExist = relayOutputPoints.containsKey(HsHpuRelayMapping.FAN_ENABLED.ordinal)
        fun deriveFanStage(): HsHpuRelayMapping {
            return when (association) {
                HsHpuRelayMapping.AUX_HEATING_STAGE1 -> {
                    when {
                        mediumAvailable -> HsHpuRelayMapping.FAN_MEDIUM_SPEED
                        highAvailable -> HsHpuRelayMapping.FAN_HIGH_SPEED
                        lowAvailable -> HsHpuRelayMapping.FAN_LOW_SPEED
                        fanEnabledExist -> HsHpuRelayMapping.FAN_ENABLED
                        else -> HsHpuRelayMapping.EXTERNALLY_MAPPED
                    }
                }

                HsHpuRelayMapping.AUX_HEATING_STAGE2 -> {
                    when {
                        highAvailable -> HsHpuRelayMapping.FAN_HIGH_SPEED
                        mediumAvailable -> HsHpuRelayMapping.FAN_MEDIUM_SPEED
                        lowAvailable -> HsHpuRelayMapping.FAN_LOW_SPEED
                        fanEnabledExist -> HsHpuRelayMapping.FAN_ENABLED
                        else -> HsHpuRelayMapping.EXTERNALLY_MAPPED
                    }
                }

                else -> HsHpuRelayMapping.EXTERNALLY_MAPPED
            }
        }

        if (auxResetRequired()) {
            resetAux(relayStages, relayOutputPoints) // non of the fans are available
        }

        val stage = deriveFanStage()
        val fanStatusMessage = getFanStage(stage)
        logIt("operateAuxBasedOnFan: derived mode is $stage")
        // operate specific fan  (low, medium, high) based on derived stage order
        if (stage != HsHpuRelayMapping.EXTERNALLY_MAPPED) {
            updateLogicalPoint(relayOutputPoints[stage.ordinal]!!, 1.0)
            relayStages[fanStatusMessage!!.displayName] = 1
        }

        when (stage) {
            HsHpuRelayMapping.FAN_MEDIUM_SPEED -> {
                relayOutputPoints[HsHpuRelayMapping.FAN_LOW_SPEED.ordinal]?.let { point ->
                    updateLogicalPoint(point, 1.0)
                    relayStages[Stage.FAN_2.displayName] = 1
                }

                relayOutputPoints[HsHpuRelayMapping.FAN_HIGH_SPEED.ordinal]?.let { highSpeedPoint ->
                    val auxStage2Inactive =
                        relayOutputPoints[HsHpuRelayMapping.AUX_HEATING_STAGE2.ordinal]
                            ?.let { getCurrentLogicalPointStatus(it) == 0.0 } ?: true
                    if (auxStage2Inactive) {
                        updateLogicalPoint(highSpeedPoint, 0.0)
                        relayStages.remove(Stage.FAN_3.displayName)
                    }
                }
            }

            HsHpuRelayMapping.FAN_HIGH_SPEED -> {
                relayOutputPoints[HsHpuRelayMapping.FAN_MEDIUM_SPEED.ordinal]?.let { point ->
                    updateLogicalPoint(point, 1.0)
                    relayStages[Stage.FAN_2.displayName] = 1
                }

                relayOutputPoints[HsHpuRelayMapping.FAN_LOW_SPEED.ordinal]?.let { point ->
                    updateLogicalPoint(point, 1.0)
                    relayStages[Stage.FAN_1.displayName] = 1
                }
            }

            HsHpuRelayMapping.FAN_ENABLED -> {
                logIt("operate Aux Based On Fan Enable")
                relayStages[StatusMsgKeys.FAN_ENABLED.name] = 1
            }

            else -> {
                logIt("operateAuxBasedOnFan: Relay fan mapping is not available")
            }
        }
    }


    private fun runSpecificAnalogFanSpeed(
        config: HpuConfiguration, analogOutStages: HashMap<String, Int>,
        relayOutputPoints: HashMap<Int, String>, analogOutputPoints: HashMap<Int, String>
    ) {

        fun getPercent(fanConfig: FanConfig, fanSpeed: FanSpeed): Double {
            return when (fanSpeed) {
                FanSpeed.HIGH -> fanConfig.high.currentVal
                FanSpeed.MEDIUM -> fanConfig.medium.currentVal
                FanSpeed.LOW -> fanConfig.low.currentVal
                else -> 0.0
            }
        }

        var fanSpeed = FanSpeed.OFF

        if (isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE2, relayOutputPoints)) {
            fanSpeed = FanSpeed.HIGH
        } else if (isAuxAvailableAndActive(
                HsHpuRelayMapping.AUX_HEATING_STAGE1,
                relayOutputPoints
            )
        ) {
            fanSpeed = FanSpeed.MEDIUM
        }

        config.apply {
            listOf(
                (HpuAnalogOutConfigs(
                    analogOut1Enabled.enabled,
                    analogOut1Association.associationVal,
                    analogOut1MinMaxConfig,
                    analogOut1FanSpeedConfig
                )),
                (HpuAnalogOutConfigs(
                    analogOut2Enabled.enabled,
                    analogOut2Association.associationVal,
                    analogOut2MinMaxConfig,
                    analogOut2FanSpeedConfig
                )),
                (HpuAnalogOutConfigs(
                    analogOut3Enabled.enabled,
                    analogOut3Association.associationVal,
                    analogOut3MinMaxConfig,
                    analogOut3FanSpeedConfig
                )),
            ).forEach { analogOutConfig ->
                if (analogOutConfig.enabled && analogOutConfig.association == HsHpuAnalogOutMapping.FAN_SPEED.ordinal && fanSpeed != FanSpeed.OFF) {
                    val percentage = getPercent(analogOutConfig.fanSpeed, fanSpeed)
                    logIt("Fan Speed : $percentage")
                    updateLogicalPoint(
                        analogOutputPoints[HsHpuAnalogOutMapping.FAN_SPEED.ordinal]!!, percentage)
                    analogOutStages[StatusMsgKeys.FAN_SPEED.name] = 1
                }
            }
        }
    }

    private fun runForKeyCardSensor(config: HyperStatConfiguration, equip: HyperStatEquip) {
        val isKeyCardEnabled = (config.isEnabledAndAssociated(
            config.analogIn1Enabled,
            config.analogIn1Association,
            AnalogInputAssociation.KEY_CARD_SENSOR.ordinal
        )
                || config.isEnabledAndAssociated(
            config.analogIn1Enabled,
            config.analogIn2Association,
            AnalogInputAssociation.KEY_CARD_SENSOR.ordinal
        ))
        keyCardIsInSlot(
            (if (isKeyCardEnabled) 1.0 else 0.0),
            if (equip.keyCardSensor.readHisVal() > 0) 1.0 else 0.0,
            equip
        )
    }

    private fun evaluateLoopOutputs(
        userIntents: UserIntents,
        basicSettings: BasicSettings,
        hyperStatTuners: HyperStatProfileTuners,
        config: HyperStatConfiguration,
        equip: HyperStatEquip
    ) {
        resetLoopOutputs()

        when (state) {
            ZoneState.COOLING -> evaluateCoolingLoop(userIntents)
            ZoneState.HEATING -> evaluateHeatingLoop(userIntents)
            else -> logIt("Zone is in deadband")
        }
        evaluateFanOutput(basicSettings, hyperStatTuners)
        evaluateDcvLoop(equip, config)
    }

    private fun operateAnalogOutputs(
        config: HpuConfiguration, equip: HpuV2Equip,
        basicSettings: BasicSettings, relayOutputPoints: HashMap<Int, String>
    ) {
        config.apply {
            listOf(
                    Pair(HpuAnalogOutConfigs(analogOut1Enabled.enabled, analogOut1Association.associationVal, analogOut1MinMaxConfig, analogOut1FanSpeedConfig), Port.ANALOG_OUT_ONE),
                    Pair(HpuAnalogOutConfigs(analogOut2Enabled.enabled, analogOut2Association.associationVal, analogOut2MinMaxConfig, analogOut2FanSpeedConfig), Port.ANALOG_OUT_TWO),
                    Pair(HpuAnalogOutConfigs(analogOut3Enabled.enabled, analogOut3Association.associationVal, analogOut3MinMaxConfig, analogOut3FanSpeedConfig), Port.ANALOG_OUT_THREE),
            ).forEach { (analogOutConfig, port) ->
                if (analogOutConfig.enabled) {
                    handleAnalogOutState(
                        analogOutConfig, equip, config, port, basicSettings, relayOutputPoints
                    )
                }
            }
        }
    }


    private fun handleAnalogOutState(
        analogOutState: HpuAnalogOutConfigs,
        equip: HpuV2Equip,
        config: HpuConfiguration,
        port: Port,
        basicSettings: BasicSettings,
        relayOutputPoints: HashMap<Int, String>
    ) {

        val analogMapping =
            HsHpuAnalogOutMapping.values().find { it.ordinal == analogOutState.association }
        when (analogMapping) {
            HsHpuAnalogOutMapping.COMPRESSOR_SPEED -> {
                doAnalogCompressorSpeed(
                    port,
                    basicSettings.conditioningMode,
                    equip.analogOutStages,
                    compressorLoopOutput,
                    getZoneMode()
                )
            }

            HsHpuAnalogOutMapping.DCV_DAMPER -> {
                doAnalogDCVAction(
                    port,
                    equip.analogOutStages,
                    config.zoneCO2Threshold.currentVal,
                    config.zoneCO2DamperOpeningRate.currentVal,
                    isDoorOpenState(config, equip),
                    equip
                )
            }

            HsHpuAnalogOutMapping.FAN_SPEED -> {
                if (isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE1, relayOutputPoints)) return
                if (isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE2, relayOutputPoints)) return
                doAnalogFanAction(
                    port,
                    analogOutState.fanSpeed.low.currentVal.toInt(),
                    analogOutState.fanSpeed.medium.currentVal.toInt(),
                    analogOutState.fanSpeed.high.currentVal.toInt(),
                    equip, basicSettings, fanLoopOutput,
                )
            }

            else -> {}
        }
    }

    private fun doAnalogCompressorSpeed(
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        compressorLoopOutput: Int,
        zoneMode: ZoneState
    ) {
        if (conditioningMode != StandaloneConditioningMode.OFF) {
            updateLogicalPoint(logicalPointsList[port]!!, compressorLoopOutput.toDouble())
            if (compressorLoopOutput > 0) {
                if (zoneMode == ZoneState.COOLING) analogOutStages[StatusMsgKeys.COOLING.name] =
                    compressorLoopOutput
                if (zoneMode == ZoneState.HEATING) analogOutStages[StatusMsgKeys.HEATING.name] =
                    compressorLoopOutput
            }
        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }


    private fun operateRelays(
        config: HpuConfiguration, basicSettings: BasicSettings, equip: HpuV2Equip
    ) {
        val controllerFactory = HyperStatControlFactory(equip)
        controllerFactory.addControllers(config)
        runControllers(equip, basicSettings, config)
    }

    private fun runControllers(equip: HpuV2Equip, basicSettings: BasicSettings, config: HpuConfiguration) {
        equip.derivedFanLoopOutput.data = equip.fanLoopOutput.readHisVal()
        equip.zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        equip.controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            val result = controller.runController()
            updateRelayStatus(controllerName, result, equip, basicSettings, config)
        }
    }

    private fun updateRelayStatus(
        controllerName: String, result: Any, equip: HpuV2Equip, basicSettings: BasicSettings,
        config: HpuConfiguration
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
            ControllerNames.COMPRESSOR_RELAY_CONTROLLER -> {
                val compressorStages = result as List<Pair<Int, Boolean>>
                compressorStages.forEach {
                    val (stage, isActive) = Pair(
                        it.first,
                        if (basicSettings.conditioningMode != StandaloneConditioningMode.OFF) it.second else false
                    )
                    when (stage) {
                        0 -> updateRelayStage(
                            if (coolingLoopOutput > 0) Stage.COOLING_1.displayName else Stage.HEATING_1.displayName,
                            isActive,
                            equip.compressorStage1
                        )

                        1 -> updateRelayStage(
                            if (coolingLoopOutput > 0) Stage.COOLING_2.displayName else Stage.HEATING_2.displayName,
                            isActive,
                            equip.compressorStage2
                        )

                        2 -> updateRelayStage(
                            if (coolingLoopOutput > 0) Stage.COOLING_3.displayName else Stage.HEATING_3.displayName,
                            isActive,
                            equip.compressorStage3
                        )
                    }
                }
            }

            ControllerNames.FAN_SPEED_CONTROLLER -> {

                runTitle24Rule(config)

                fun checkUserIntentAction(stage: Int): Boolean {
                    val mode = equip.fanOpMode
                    return when (stage) {
                        0 -> isHighUserIntentFanMode(mode) || isMediumUserIntentFanMode(mode) || isLowUserIntentFanMode(mode)
                        1 -> isHighUserIntentFanMode(mode) || isMediumUserIntentFanMode(mode)
                        2 -> isHighUserIntentFanMode(mode)
                        else -> false
                    }
                }

                fun isStageActive(
                    stage: Int, currentState: Boolean, isLowestStageActive: Boolean
                ): Boolean {
                    val mode = equip.fanOpMode.readPriorityVal().toInt()
                    return if (mode == StandaloneFanStage.AUTO.ordinal) {
                        (canWeRunFan(basicSettings) && (currentState
                                || (fanEnabledStatus && fanLoopOutput > 0 && isLowestStageActive)
                                || (isLowestStageActive && runFanLowDuringDoorWindow)))
                    } else {
                        checkUserIntentAction(stage)
                    }
                }

                val fanStages = result as List<Pair<Int, Boolean>>
                fanStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateRelayStage(
                            Stage.FAN_1.displayName,
                            isStageActive(stage, isActive, lowestStageFanLow),
                            equip.fanLowSpeed
                        )

                        1 -> updateRelayStage(
                            Stage.FAN_2.displayName,
                            isStageActive(stage, isActive, lowestStageFanMedium),
                            equip.fanMediumSpeed
                        )

                        2 -> updateRelayStage(
                            Stage.FAN_3.displayName,
                            isStageActive(stage, isActive, lowestStageFanHigh),
                            equip.fanHighSpeed
                        )
                    }
                }
            }

            ControllerNames.FAN_ENABLED -> updateStatus(
                equip.fanEnable, result, StatusMsgKeys.FAN_ENABLED.name
            )

            ControllerNames.OCCUPIED_ENABLED -> updateStatus(equip.occupiedEnable, result)
            ControllerNames.HUMIDIFIER_CONTROLLER -> updateStatus(equip.humidifierEnable, result)
            ControllerNames.DEHUMIDIFIER_CONTROLLER -> updateStatus(equip.dehumidifierEnable, result)
            ControllerNames.DAMPER_RELAY_CONTROLLER -> updateStatus(equip.dcvDamper, result)
            ControllerNames.AUX_HEATING_STAGE1 -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode != StandaloneConditioningMode.AUTO && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
                    status = false
                }
                updateStatus(equip.auxHeatingStage1, status, StatusMsgKeys.AUX_HEATING_STAGE1.name)
            }

            ControllerNames.AUX_HEATING_STAGE2 -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode != StandaloneConditioningMode.AUTO && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
                    status = false
                }
                updateStatus(equip.auxHeatingStage2, status, StatusMsgKeys.AUX_HEATING_STAGE2.name)
            }

            ControllerNames.CHANGE_OVER_O_COOLING -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
                    status = false
                }
                updateStatus(equip.changeOverCooling, status)
            }

            ControllerNames.CHANGE_OVER_B_HEATING -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
                    status = false
                }
                updateStatus(equip.changeOverHeating, status)
            }

            else -> {
                logIt("Unknown controller: $controllerName")
            }
        }
    }

    /**
     * Check if we should allow fan operation even when the door window is open
     * @return true if the door or window is open and the occupancy status is not UNOCCUPIED, otherwise false.
     */
    private fun checkFanOperationAllowedDoorWindow(userIntents: UserIntents): Boolean {
        return if (currentTemp < userIntents.coolingDesiredTemp && currentTemp > userIntents.heatingDesiredTemp) {
            doorWindowSensorOpenStatus &&
                    occupancyBeforeDoorWindow != Occupancy.UNOCCUPIED &&
                    occupancyBeforeDoorWindow != Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
                    occupancyBeforeDoorWindow != Occupancy.VACATION
        } else {
            doorWindowSensorOpenStatus
        }
    }

    private fun runForDoorWindowSensor(config: HyperStatConfiguration, equip: HyperStatEquip): Boolean {
        val isDoorOpen = isDoorOpenState(config, equip)
        logIt(" is Door Open ? $isDoorOpen")
        return isDoorOpen
    }

    private fun resetAux(relayStages: HashMap<String, Int>, relayOutputPoints: HashMap<Int, String>) {
        if (relayOutputPoints.containsKey(HsHpuRelayMapping.AUX_HEATING_STAGE1.ordinal)) {
            resetLogicalPoint(relayOutputPoints[HsHpuRelayMapping.AUX_HEATING_STAGE1.ordinal]!!)
        }
        if (relayOutputPoints.containsKey(HsHpuRelayMapping.AUX_HEATING_STAGE2.ordinal)) {
            resetLogicalPoint(relayOutputPoints[HsHpuRelayMapping.AUX_HEATING_STAGE2.ordinal]!!)
        }
        relayStages.remove(HsHpuRelayMapping.AUX_HEATING_STAGE1.name)
        relayStages.remove(HsHpuRelayMapping.AUX_HEATING_STAGE2.name)
    }

    private fun isAuxAvailableAndActive(mapping: HsHpuRelayMapping, relayOutputPoints: HashMap<Int, String>): Boolean {
        return (relayOutputPoints.containsKey(mapping.ordinal) && getCurrentLogicalPointStatus(relayOutputPoints[mapping.ordinal]!!) == 1.0)
    }

    private fun getZoneMode(): ZoneState {
        return when {
            (coolingLoopOutput > 0) -> ZoneState.COOLING
            (heatingLoopOutput > 0) -> ZoneState.HEATING
            else -> ZoneState.TEMPDEAD
        }
    }

    private fun isDoorOpenState(config: HyperStatConfiguration, equip: HyperStatEquip): Boolean {

        fun isAnalogHasDoorWindowMapping(): Boolean {
            return (config.isEnabledAndAssociated(config.analogOut1Enabled, config.analogIn1Association, AnalogInputAssociation.DOOR_WINDOW_SENSOR_TITLE_24.ordinal)
                    || config.isEnabledAndAssociated(config.analogOut2Enabled, config.analogIn2Association, AnalogInputAssociation.DOOR_WINDOW_SENSOR_TITLE_24.ordinal))
        }

        // If thermistor value less than 10000 ohms door is closed (0) else door is open (1)
        // If analog in value is less than 2v door is closed(0) else door is open (1)
        var isDoorOpen = false
        var th2SensorEnabled = false
        var analogSensorEnabled = false

        if (config.isEnabledAndAssociated(config.thermistor2Enabled, config.thermistor2Association,
                        Th2InputAssociation.DOOR_WINDOW_SENSOR_NC_TITLE_24.ordinal)) {
            val sensorValue = equip.doorWindowSensorNCTitle24.readHisVal().toInt()
            if (sensorValue == 1) isDoorOpen = true
            th2SensorEnabled = true
            logIt("TH1 Door Window sensor value : Door is $sensorValue")
        }

        if (isAnalogHasDoorWindowMapping()) {
            val sensorValue = equip.doorWindowSensorTitle24.readHisVal().toInt()
            if (sensorValue == 1) isDoorOpen = true
            analogSensorEnabled = true
            logIt("Analog Input has mapping Door Window sensor value : Door is $sensorValue")

        }
        doorWindowIsOpen(
                if (th2SensorEnabled || analogSensorEnabled) 1.0 else 0.0,
                if (isDoorOpen) 1.0 else 0.0, equip
        )
        return isDoorOpen
    }

    private fun runTitle24Rule(config: HpuConfiguration) {
        resetFanLowestFanStatus()
        fanEnabledStatus = config.isAnyRelayEnabledAssociated(association = HsCpuRelayMapping.FAN_ENABLED.ordinal)
        val lowestStage = config.getLowestFanStage()
        when (lowestStage) {
            HsHpuRelayMapping.FAN_LOW_SPEED -> lowestStageFanLow = true
            HsHpuRelayMapping.FAN_MEDIUM_SPEED -> lowestStageFanMedium = true
            HsHpuRelayMapping.FAN_HIGH_SPEED -> lowestStageFanHigh = true
            else -> {}
        }
    }

    override fun getDisplayCurrentTemp() = averageZoneTemp

    override fun getAverageZoneTemp(): Double {
        var tempTotal = 0.0
        var nodeCount = 0
        hpuDeviceMap.forEach { (_, device) ->
            if (device.currentTemp.readHisVal() > 0) {
                tempTotal += device.currentTemp.readHisVal()
                nodeCount++
            }
        }
        return if (nodeCount == 0) 0.0 else tempTotal / nodeCount
    }

    override fun getEquip(): Equip? {
        for (nodeAddress in hpuDeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    override fun getNodeAddresses(): Set<Short?> = hpuDeviceMap.keys.map { it.toShort() }.toSet()

    fun getProfileDomainEquip(node: Int): HpuV2Equip = hpuDeviceMap[node]!!

    override fun getCurrentTemp(): Double {
        for (nodeAddress in hpuDeviceMap.keys) {
            return hpuDeviceMap[nodeAddress]!!.currentTemp.readHisVal()
        }
        return 0.0
    }

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        TODO("Not using now")
    }

    private fun printStatus(
        tuners: HyperStatProfileTuners,
        settings: BasicSettings,
        userIntents: UserIntents,
        equip: HyperStatEquip,
        config: HyperStatConfiguration
    ) {
        logIt(
            "Fan speed multiplier:  ${tuners.analogFanSpeedMultiplier} " +
                    "AuxHeating1Activate: ${tuners.auxHeating1Activate} " +
                    "AuxHeating2Activate: ${tuners.auxHeating2Activate} \n" +
                    "Current Occupancy: ${Occupancy.values()[equip.occupancyMode.readHisVal().toInt()]} \n" +
                    "Current Temp : $currentTemp Desired (Heating: ${userIntents.heatingDesiredTemp} Cooling: ${userIntents.coolingDesiredTemp})\n" +
                    "Fan Mode: ${settings.fanMode} Conditioning Mode: ${settings.conditioningMode}\n"+
                    "Loop Outputs: (Heating: $heatingLoopOutput Cooling: $coolingLoopOutput " +
                    "Fan : $fanLoopOutput Compressor: $compressorLoopOutput DCV $dcvLoopOutput) \n"
        )
        logResults(config, L.TAG_CCU_HSHPU, logicalPointsList)
    }

}