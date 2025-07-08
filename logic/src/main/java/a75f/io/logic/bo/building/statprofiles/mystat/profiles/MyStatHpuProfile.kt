package a75f.io.logic.bo.building.statprofiles.mystat.profiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Point
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
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
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatFanConfig
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.statcontrollers.MyStatControlFactory
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.MyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.MyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.fetchMyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.fetchMyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatHpuAnalogOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.getMyStatHpuRelayOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.getMyStatLogicalPointList
import a75f.io.logic.bo.building.statprofiles.util.isMyStatHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMyStatLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.logMsResults
import a75f.io.logic.bo.building.statprofiles.util.updateLogicalPoint
import a75f.io.logic.bo.building.statprofiles.util.updateLoopOutputs
import a75f.io.logic.bo.building.statprofiles.util.updateOperatingMode
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.util.uiutils.MyStatUserIntentHandler

/**
 * Created by Manjunath K on 16-01-2025.
 */

class MyStatHpuProfile : MyStatProfile(L.TAG_CCU_MSHPU) {

    private val hpuDeviceMap: MutableMap<Int, MyStatHpuEquip> = mutableMapOf()

    private lateinit var curState: ZoneState
    override lateinit var occupancyStatus: Occupancy

    private var analogLogicalPoints: HashMap<Int, String> = HashMap()
    private var relayLogicalPoints: HashMap<Int, String> = HashMap()


    override fun updateZonePoints() {
        hpuDeviceMap.forEach { (nodeAddress, equip) ->
            hpuDeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as MyStatHpuEquip
            logIt( "Process HPU: equipRef =  ${equip.nodeAddress}")
            processHpuProfile(equip)
        }
    }

    fun processHpuProfile(equip: MyStatHpuEquip) {

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

        val config = getMyStatConfiguration(equip.equipRef) as MyStatHpuConfiguration

        logicalPointsList = getMyStatLogicalPointList(equip, config)
        relayLogicalPoints = getMyStatHpuRelayOutputPoints(equip)
        analogLogicalPoints = getMyStatHpuAnalogOutputPoints(equip)

        curState = ZoneState.DEADBAND

        if (equipOccupancyHandler != null) {
            occupancyStatus = equipOccupancyHandler.currentOccupiedMode
            equip.zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        }

        val myStatTuners = fetchMyStatTuners(equip) as MyStatTuners
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val fanModeSaved = FanModeCacheStorage.getMyStatFanModeCache().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchMyStatBasicSettings(equip)
        val controllerFactory = MyStatControlFactory(equip)

        logIt(
            "Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}"
        )
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        logIt(
            "After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}"
        )

        loopController.initialise(tuners = myStatTuners)
        loopController.dumpLogs()
        handleChangeOfDirection(currentTemp, userIntents, controllerFactory, equip)
        updateOperatingMode(currentTemp, averageDesiredTemp, basicSettings.conditioningMode, equip.operatingMode)

        resetEquip(equip)
        evaluateLoopOutputs(userIntents, basicSettings, myStatTuners, config, equip)
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
            true, compressorLoopOutput, equip.compressorLoopOutput
        )
        if (basicSettings.fanMode != MyStatFanStages.OFF) {
            operateRelays(config,  basicSettings, equip, controllerFactory)
            operateAnalogOutputs(config, equip, basicSettings, equip.analogOutStages, relayLogicalPoints)
            if (basicSettings.fanMode == MyStatFanStages.AUTO) {
                runFanOperationBasedOnAuxStages(equip.relayStages, equip.analogOutStages, config, relayLogicalPoints, analogLogicalPoints)
            }
        } else {
            resetLogicalPoints()
        }

        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())
        logIt(
            "Fan speed multiplier:  ${myStatTuners.analogFanSpeedMultiplier} " +
                    "AuxHeating1Activate: ${myStatTuners.auxHeating1Activate} " +
                    "waterValveSamplingOnTime: ${myStatTuners.waterValveSamplingOnTime}" +
                    "  waterValveSamplingWaitTime : ${myStatTuners.waterValveSamplingWaitTime} \n" +
                    "waterValveSamplingDuringLoopDeadbandOnTime: ${myStatTuners.waterValveSamplingDuringLoopDeadbandOnTime} " +
                    " waterValveSamplingDuringLoopDeadbandWaitTime : ${myStatTuners.waterValveSamplingDuringLoopDeadbandWaitTime} \n"
                    + "Current Occupancy: ${Occupancy.values()[equip.occupancyMode.readHisVal().toInt()]} \n" + "Fan Mode : ${basicSettings.fanMode} " +
                    "Conditioning Mode ${basicSettings.conditioningMode} \n"
                    + "Current Temp : $currentTemp Desired (Heating: ${userIntents.heatingDesiredTemp}"
                    + " Cooling: ${userIntents.coolingDesiredTemp})\n"
                    + "Loop Outputs: (Heating Loop: $heatingLoopOutput Cooling Loop:" +
                    " $coolingLoopOutput Fan Loop: $fanLoopOutput  Compressor Loop : $compressorLoopOutput dcvLoopOutput : $dcvLoopOutput)" +
                    " \n"
        )
        logMsResults(config, L.TAG_CCU_MSHPU, logicalPointsList)
        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY
        MyStatUserIntentHandler.updateMyStatStatus(
            temperatureState,
            equip, L.TAG_CCU_MSHPU
        )
        if (occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        logIt(  "----------------------------------------------------------")
    }

    private fun evaluateLoopOutputs(
        userIntents: UserIntents,
        basicSettings: MyStatBasicSettings,
        hyperStatTuners: MyStatTuners,
        config: MyStatConfiguration,
        equip: MyStatEquip
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

    private fun operateRelays(
        config: MyStatHpuConfiguration, basicSettings: MyStatBasicSettings,
        equip: MyStatHpuEquip, controllerFactory: MyStatControlFactory
    ) {
        controllerFactory.addControllers(config)
        runControllers(equip, basicSettings, config)
    }

    private fun runControllers(equip: MyStatHpuEquip, basicSettings: MyStatBasicSettings, config: MyStatHpuConfiguration) {
        equip.derivedFanLoopOutput.data = equip.fanLoopOutput.readHisVal()
        equip.stageDownTimer.data = equip.mystatStageUpTimerCounter.readPriorityVal()
        equip.stageUpTimer.data = equip.mystatStageUpTimerCounter.readPriorityVal()

        equip.controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            val result = controller.runController()
            updateRelayStatus(controllerName, result, equip, basicSettings, config)
        }
    }

    private fun updateRelayStatus(
        controllerName: String,
        result: Any,
        equip: MyStatHpuEquip,
        basicSettings: MyStatBasicSettings,
        config: MyStatHpuConfiguration
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
                    }
                }
            }
            ControllerNames.FAN_SPEED_CONTROLLER -> {
                runTitle24Rule(config)

                fun checkUserIntentAction(stage: Int): Boolean {
                    val mode = equip.fanOpMode
                    return when (stage) {
                        0 -> isMyStatHighUserIntentFanMode(mode) || isMyStatLowUserIntentFanMode(mode)
                        1 -> isMyStatHighUserIntentFanMode(mode)
                        else -> false
                    }
                }

                fun isStageActive(
                    stage: Int, currentState: Boolean, isLowestStageActive: Boolean
                ): Boolean {
                    val mode = equip.fanOpMode.readPriorityVal().toInt()
                    return if (mode == StandaloneFanStage.AUTO.ordinal) {
                        (basicSettings.conditioningMode != StandaloneConditioningMode.OFF) &&
                                (currentState || (isLowestStageActive && runFanLowDuringDoorWindow))
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
                            isStageActive(stage, isActive, lowestStageFanHigh),
                            equip.fanHighSpeed
                        )
                    }
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

            ControllerNames.FAN_ENABLED -> updateStatus(equip.fanEnable, result, StatusMsgKeys.FAN_ENABLED.name)
            ControllerNames.OCCUPIED_ENABLED -> updateStatus(equip.occupiedEnable, result)
            ControllerNames.HUMIDIFIER_CONTROLLER -> updateStatus(equip.humidifierEnable, result)
            ControllerNames.DEHUMIDIFIER_CONTROLLER -> updateStatus(equip.dehumidifierEnable, result)
            ControllerNames.DAMPER_RELAY_CONTROLLER -> updateStatus(equip.dcvDamper, result)
            else -> {
                logIt( "Unknown controller: $controllerName")
            }
        }
    }



    private fun isAuxAvailableAndActive(relayOutputPoints: HashMap<Int, String>): Boolean {
        return (relayOutputPoints.containsKey(MyStatHpuRelayMapping.AUX_HEATING_STAGE1.ordinal) && getCurrentLogicalPointStatus(
            relayOutputPoints[MyStatHpuRelayMapping.AUX_HEATING_STAGE1.ordinal]!!
        ) == 1.0)
    }

    private fun runTitle24Rule(config: MyStatHpuConfiguration) {
        resetFanLowestFanStatus()
        fanEnabledStatus =
            config.isAnyRelayEnabledAssociated(association = MyStatCpuRelayMapping.FAN_ENABLED.ordinal)
        val lowestStage = config.getLowestFanSelected()
        when (lowestStage) {
            MyStatHpuRelayMapping.FAN_LOW_SPEED -> lowestStageFanLow = true
            MyStatHpuRelayMapping.FAN_HIGH_SPEED -> lowestStageFanHigh = true
            else -> {}
        }
    }

    private fun operateAnalogOutputs(
        config: MyStatHpuConfiguration,
        equip: MyStatHpuEquip,
        basicSettings: MyStatBasicSettings,
        analogOutStages: HashMap<String, Int>,
        relayOutputPoints: HashMap<Int, String>
    ) {
        config.apply {
            if (analogOut1Enabled.enabled) {

                val analogMapping = MyStatHpuAnalogOutMapping.values().find { it.ordinal == analogOut1Association.associationVal }
                when (analogMapping) {
                    MyStatHpuAnalogOutMapping.COMPRESSOR_SPEED -> {
                        doAnalogCompressorSpeed(
                            Port.ANALOG_OUT_ONE,
                            basicSettings.conditioningMode,
                            analogOutStages,
                            compressorLoopOutput,
                            getZoneMode()
                        )
                    }

                    MyStatHpuAnalogOutMapping.FAN_SPEED -> {
                        if (isAuxAvailableAndActive(relayOutputPoints)) return
                        doAnalogFanAction(
                            Port.ANALOG_OUT_ONE,
                            analogOut1FanSpeedConfig.low.currentVal.toInt(),
                            analogOut1FanSpeedConfig.high.currentVal.toInt(),
                            basicSettings.fanMode,
                            basicSettings.conditioningMode,
                            fanLoopOutput,
                            analogOutStages
                        )
                    }

                    MyStatHpuAnalogOutMapping.DCV_DAMPER_MODULATION -> {
                        doAnalogDCVAction(
                            Port.ANALOG_OUT_ONE, analogOutStages, config.co2Threshold.currentVal,
                            co2DamperOpeningRate.currentVal,
                            isDoorOpenState(config, equip), equip
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    private fun runFanOperationBasedOnAuxStages(
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        config: MyStatHpuConfiguration,
        relayOutputPoints: HashMap<Int, String>,
        analogOutputPoints: HashMap<Int, String>
    ) {
        val aux1AvailableAndActive = isAuxAvailableAndActive(relayOutputPoints)
        val isAnalogFanAvailable = analogOutputPoints.containsKey(MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal)

        logIt(
            "Aux Based fan : aux1AvailableAndActive $aux1AvailableAndActive isAnalogFanAvailable $isAnalogFanAvailable"
        )
        if (aux1AvailableAndActive) operateAuxBasedOnFan(relayStages, relayOutputPoints, isAnalogFanAvailable)

        // Run the fan speed control if either aux1 or aux2 is available and active
        if ((aux1AvailableAndActive)) {
            runSpecificAnalogFanSpeed(
                config,
                analogOutStages,
                relayOutputPoints,
                analogOutputPoints,
            )
        }
    }

    private fun runSpecificAnalogFanSpeed(
        config: MyStatHpuConfiguration, analogOutStages: HashMap<String, Int>,
        relayOutputPoints: HashMap<Int, String>, analogOutputPoints: HashMap<Int, String>
    ) {

        fun getPercent(fanConfig: MyStatFanConfig, fanSpeed: MyStatFanSpeed): Double {
            return when (fanSpeed) {
                MyStatFanSpeed.HIGH -> fanConfig.high.currentVal
                MyStatFanSpeed.LOW -> fanConfig.low.currentVal
                else -> 0.0
            }
        }

        var fanSpeed = MyStatFanSpeed.OFF

        if (isAuxAvailableAndActive(relayOutputPoints)) {
            fanSpeed = MyStatFanSpeed.HIGH
        }
        config.apply {
            if (analogOut1Enabled.enabled
                && analogOut1Association.associationVal == MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal && fanSpeed != MyStatFanSpeed.OFF
            ) {
                val percentage = getPercent(analogOut1FanSpeedConfig, fanSpeed)
                logIt(  "Fan Speed : $percentage")
                updateLogicalPoint(
                    analogOutputPoints[MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal]!!, percentage
                )
                analogOutStages[StatusMsgKeys.FAN_SPEED.name] = 1
            }
        }
    }


    // New requirement for aux and fan operations If we do not have fan then no aux
    private fun operateAuxBasedOnFan(
        relayStages: HashMap<String, Int>, relayOutputPoints: HashMap<Int, String>, isAnalogFanAvailable: Boolean
    ) {

        fun getFanStage(mapping: MyStatHpuRelayMapping): Stage? {
            return when (mapping) {
                MyStatHpuRelayMapping.FAN_LOW_SPEED -> Stage.FAN_1
                MyStatHpuRelayMapping.FAN_HIGH_SPEED -> Stage.FAN_2
                else -> null
            }
        }

        fun getAvailableFanSpeed(relayOutputPoints: HashMap<Int, String>) = Triple(
            relayOutputPoints.containsKey(MyStatHpuRelayMapping.FAN_LOW_SPEED.ordinal),
            relayOutputPoints.containsKey(MyStatHpuRelayMapping.FAN_HIGH_SPEED.ordinal),
            relayOutputPoints.containsKey(MyStatHpuRelayMapping.FAN_ENABLED.ordinal)
        )

        val (lowAvailable, highAvailable, fanEnable) = getAvailableFanSpeed(relayOutputPoints)

        fun deriveFanStage(): MyStatHpuRelayMapping {
            return when {
                highAvailable -> MyStatHpuRelayMapping.FAN_HIGH_SPEED
                lowAvailable -> MyStatHpuRelayMapping.FAN_LOW_SPEED
                fanEnable -> MyStatHpuRelayMapping.FAN_ENABLED
                else -> MyStatHpuRelayMapping.OCCUPIED_ENABLED
            }
        }

        if (!lowAvailable && !highAvailable && !isAnalogFanAvailable && !fanEnable) {
            resetAux(relayStages, relayOutputPoints) // non of the fans are available
        }

        val stage = deriveFanStage()
        val fanStatusMessage = getFanStage(stage)
        logIt( "operateAuxBasedOnFan: derived mode is $stage")
        // operate specific fan  (low, medium, high) based on derived stage order
        if (fanStatusMessage != null) {
            updateLogicalPoint(relayOutputPoints[stage.ordinal]!!, 1.0)
            relayStages[fanStatusMessage.displayName] = 1
        }

        when (stage) {
            MyStatHpuRelayMapping.FAN_HIGH_SPEED -> {
                relayOutputPoints[MyStatHpuRelayMapping.FAN_HIGH_SPEED.ordinal]?.let { point ->
                    updateLogicalPoint(point, 1.0)
                    relayStages[Stage.FAN_2.displayName] = 1
                }

                relayOutputPoints[MyStatHpuRelayMapping.FAN_LOW_SPEED.ordinal]?.let { point ->
                    updateLogicalPoint(point, 1.0)
                    relayStages[Stage.FAN_1.displayName] = 1
                }
            }

            MyStatHpuRelayMapping.FAN_LOW_SPEED -> {
                relayOutputPoints[MyStatHpuRelayMapping.FAN_LOW_SPEED.ordinal]?.let { point ->
                    updateLogicalPoint(point, 1.0)
                    relayStages[Stage.FAN_1.displayName] = 1
                }
            }
            MyStatHpuRelayMapping.FAN_ENABLED -> {
                relayOutputPoints[MyStatHpuRelayMapping.FAN_ENABLED.ordinal]?.let { point ->
                    updateLogicalPoint(point, 1.0)
                    relayStages[StatusMsgKeys.FAN_ENABLED.name] = 1
                }
            }

            else -> {
                logIt( "operateAuxBasedOnFan: Relay fan mapping is not available")
            }
        }
    }

    private fun resetAux(
        relayStages: HashMap<String, Int>,
        relayOutputPoints: HashMap<Int, String>
    ) {
        if (relayOutputPoints.containsKey(MyStatHpuRelayMapping.AUX_HEATING_STAGE1.ordinal)) {
            resetLogicalPoint(relayOutputPoints[MyStatHpuRelayMapping.AUX_HEATING_STAGE1.ordinal]!!)
        }
        relayStages.remove(MyStatHpuRelayMapping.AUX_HEATING_STAGE1.name)
    }

    private fun getZoneMode(): ZoneState {
        return when {
            (coolingLoopOutput > 0) -> ZoneState.COOLING
            (heatingLoopOutput > 0) -> ZoneState.HEATING
            else -> ZoneState.TEMPDEAD
        }
    }

    private fun runForDoorWindowSensor(config: MyStatConfiguration, equip: MyStatEquip): Boolean {

        val isDoorOpen = isDoorOpenState(config, equip)
        logIt( " is Door Open ? $isDoorOpen")
        if (isDoorOpen) {
            resetLoopOutputs()
            resetLogicalPoints()
            equip.analogOutStages.clear()
            equip.relayStages.clear()
        }
        return isDoorOpen
    }

    private fun runForKeyCardSensor(config: MyStatConfiguration, equip: MyStatEquip) {
        val isKeyCardEnabled = (config.universalIn1Enabled.enabled
                && config.universalIn1Association.associationVal == MyStatConfiguration.UniversalMapping.KEY_CARD_SENSOR.ordinal)
        keyCardIsInSlot((if (isKeyCardEnabled) 1.0 else 0.0), if (equip.keyCardSensor.readHisVal() > 0) 1.0 else 0.0, equip)
    }

    fun addEquip(equipRef: String) {
        val equip = MyStatHpuEquip(equipRef)
        hpuDeviceMap[equip.nodeAddress] = equip
    }

    override fun getEquip(): Equip? {
        for (nodeAddress in hpuDeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    fun getProfileDomainEquip(node: Int): MyStatHpuEquip = hpuDeviceMap[node]!!

    override fun getProfileType() = ProfileType.MYSTAT_HPU

    override fun getNodeAddresses(): Set<Short?> = hpuDeviceMap.keys.map { it.toShort() }.toSet()

    override fun getCurrentTemp(): Double {
        for (nodeAddress in hpuDeviceMap.keys) {
            return hpuDeviceMap[nodeAddress]!!.currentTemp.readHisVal()
        }
        return 0.0
    }

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        TODO("Not required")
    }
}