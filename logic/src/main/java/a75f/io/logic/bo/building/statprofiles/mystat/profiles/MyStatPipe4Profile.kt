package a75f.io.logic.bo.building.statprofiles.mystat.profiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatPipe4Equip
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
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4RelayMapping
import a75f.io.logic.bo.building.statprofiles.statcontrollers.MyStatControlFactory
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.MyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.MyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.canWeDoConditioning
import a75f.io.logic.bo.building.statprofiles.util.canWeDoCooling
import a75f.io.logic.bo.building.statprofiles.util.canWeDoHeating
import a75f.io.logic.bo.building.statprofiles.util.canWeRunFan
import a75f.io.logic.bo.building.statprofiles.util.fetchMyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.fetchMyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatLogicalPointList
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPipe4AnalogOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPipe4RelayOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.isMyStatHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMyStatLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.keyCardIsInSlot
import a75f.io.logic.bo.building.statprofiles.util.logMsResults
import a75f.io.logic.bo.building.statprofiles.util.runLowestFanSpeedDuringDoorOpen
import a75f.io.logic.bo.building.statprofiles.util.updateLogicalPoint
import a75f.io.logic.bo.building.statprofiles.util.updateLoopOutputs
import a75f.io.logic.bo.building.statprofiles.util.updateOperatingMode
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.handlers.doAnalogOperation
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.util.uiutils.MyStatUserIntentHandler

class MyStatPipe4Profile : MyStatProfile(L.TAG_CCU_MSPIPE4) {

    override lateinit var occupancyStatus: Occupancy
    private lateinit var curState: ZoneState

    private var analogLogicalPoints: HashMap<Int, String> = HashMap()
    private var relayLogicalPoints: HashMap<Int, String> = HashMap()
    private val pipe4DeviceMap: MutableMap<Int, MyStatPipe4Equip> = mutableMapOf()

    fun getProfileDomainEquip(node: Int): MyStatPipe4Equip = pipe4DeviceMap[node]!!

    override fun getNodeAddresses(): Set<Short?> = pipe4DeviceMap.keys.map { it.toShort() }.toSet()

    override fun getCurrentTemp(): Double {
        for (nodeAddress in pipe4DeviceMap.keys) {
            return pipe4DeviceMap[nodeAddress]!!.currentTemp.readHisVal()
        }
        return 0.0
    }
    override fun updateZonePoints() {
        pipe4DeviceMap.forEach { (nodeAddress, equip) ->
            pipe4DeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as MyStatPipe4Equip
            logIt("Process Pipe4: equipRef =  ${equip.nodeAddress}")
            processPipe4Profile(equip)
        }
    }

    fun addEquip(equipRef: String) {
        val equip = MyStatPipe4Equip(equipRef)
        pipe4DeviceMap[equip.nodeAddress] = equip
    }

    override fun getEquip(): Equip? {
        for (nodeAddress in pipe4DeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    override fun getProfileType(): ProfileType {
        return ProfileType.MYSTAT_PIPE4
    }

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        TODO("Not yet implemented")
    }

    private fun processPipe4Profile(equip: MyStatPipe4Equip) {

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

        val config = getMyStatConfiguration(equip.equipRef)
        val myStatTuners = fetchMyStatTuners(equip) as MyStatTuners
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val fanModeSaved = FanModeCacheStorage.getMyStatFanModeCache().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchMyStatBasicSettings(equip)
        val controllerFactory = MyStatControlFactory(
            equip,
            controllers,
            stageCounts,
            derivedFanLoopOutput,
            zoneOccupancyState
        )

        logicalPointsList = getMyStatLogicalPointList(equip, config!!)
        relayLogicalPoints = getMyStatPipe4RelayOutputPoints(equip)
        analogLogicalPoints = getMyStatPipe4AnalogOutputPoints(equip)
        curState = ZoneState.DEADBAND


        if (equipOccupancyHandler != null) {
            occupancyStatus = equipOccupancyHandler.currentOccupiedMode
            zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        }

        logIt("Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        logIt("After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")

        loopController.initialise(tuners = myStatTuners)
        loopController.dumpLogs()
        handleChangeOfDirection(currentTemp, userIntents, controllerFactory, equip)
        updateOperatingMode(
            currentTemp,
            averageDesiredTemp,
            basicSettings.conditioningMode,
            equip.operatingMode
        )

        resetEquip(equip)
        evaluateLoopOutputs(userIntents, basicSettings, myStatTuners, config, equip)
        updateOccupancyDetection(equip)

        doorWindowSensorOpenStatus = runForDoorWindowSensor(
            equip
        )
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)
        if (occupancyStatus == Occupancy.WINDOW_OPEN) resetLoopOutputs()
        keyCardIsInSlot(equip)
        fanLowVentilationAvailable.data = if (equip.fanLowSpeedVentilation.pointExists()) 1.0 else 0.0
        updateLoopOutputs(
            coolingLoopOutput, equip.coolingLoopOutput,
            heatingLoopOutput, equip.heatingLoopOutput,
            fanLoopOutput, equip.fanLoopOutput,
            dcvLoopOutput, equip.dcvLoopOutput,
        )

        coolingLoopOutput = equip.coolingLoopOutput.readHisVal().toInt()
        heatingLoopOutput = equip.heatingLoopOutput.readHisVal().toInt()
        fanLoopOutput = equip.fanLoopOutput.readHisVal().toInt()
        dcvLoopOutput = equip.dcvLoopOutput.readHisVal().toInt()
        if (canWeRunFan(basicSettings) && (doorWindowSensorOpenStatus.not())) {
            operateRelays(
                config as MyStatPipe4Configuration,
                basicSettings,
                equip,
                controllerFactory
            )
            handleAnalogOutState(config, equip, basicSettings, equip.analogOutStages)
        } else {
            resetLogicalPoints(equip)
            if (isDoorWindowDueTitle24 && canWeRunFan(basicSettings)) {
                runLowestFanSpeedDuringDoorOpen(equip, L.TAG_CCU_MSPIPE4)
            }
        }

        // Run the title 24 fan operation after the reset of all PI output is done
        doFanOperationTitle24(basicSettings, equip, config as MyStatPipe4Configuration)
        triggerFanForAuxIfRequired(basicSettings, config, equip)
        updateOperatingMode(
            currentTemp,
            averageDesiredTemp,
            basicSettings.conditioningMode,
            equip.operatingMode
        )
        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())
        logIt(
            "Fan speed multiplier:  ${myStatTuners.analogFanSpeedMultiplier} " +
                    "AuxHeating1Activate: ${myStatTuners.myStatAuxHeating1Activate} " +
                    "Current Occupancy: ${Occupancy.values()[equip.occupancyMode.readHisVal().toInt()]} \n" +
                    "Fan Mode : ${basicSettings.fanMode} Conditioning Mode ${basicSettings.conditioningMode} \n" +
                    "Current Temp : $currentTemp Desired (Heating: ${userIntents.heatingDesiredTemp}" +
                    " Cooling: ${userIntents.coolingDesiredTemp})\n" +
                    "Loop Outputs: (Heating Loop: $heatingLoopOutput Cooling Loop: $coolingLoopOutput Fan Loop: $fanLoopOutput DCVLoop: $dcvLoopOutput) \n"
        )
        logMsResults(config, L.TAG_CCU_MSPIPE4, logicalPointsList)
        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState =
            ZoneTempState.EMERGENCY
        MyStatUserIntentHandler.updateMyStatStatus(temperatureState, equip, L.TAG_CCU_MSPIPE4)
        if (occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        logIt("----------------------------------------------------------")
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
        val isCoolingAllowed =
            mode == StandaloneConditioningMode.COOL_ONLY || mode == StandaloneConditioningMode.AUTO
        val isHeatingAllowed =
            (mode == StandaloneConditioningMode.HEAT_ONLY || mode == StandaloneConditioningMode.AUTO)

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

    private fun handleAnalogOutState(
        config: MyStatPipe4Configuration,
        equip: MyStatPipe4Equip,
        basicSettings: MyStatBasicSettings,
        analogOutStages: HashMap<String, Int>
    ) {
        config.apply {
            listOf(
                Triple(
                    Pair(config.universalOut1, config.universalOut1Association),
                    config.analogOut1FanSpeedConfig,
                    Port.UNIVERSAL_OUT_ONE
                ),
                Triple(
                    Pair(config.universalOut2, config.universalOut2Association),
                    config.analogOut2FanSpeedConfig,
                    Port.UNIVERSAL_OUT_TWO
                )
            ).forEach { (universalOut, fanConfig, port) ->
                val analogMapping = MyStatPipe4AnalogOutMapping.values()
                    .find { it.ordinal == universalOut.second.associationVal }
                when (analogMapping) {

                    MyStatPipe4AnalogOutMapping.DCV_DAMPER_MODULATION -> {
                        doAnalogDCVAction(
                            port, analogOutStages, config.zoneCO2Threshold.currentVal,
                            config.zoneCO2DamperOpeningRate.currentVal, equip
                        )
                    }

                    MyStatPipe4AnalogOutMapping.FAN_SPEED -> {
                        doAnalogFanAction(
                            port,
                            fanConfig.low.currentVal.toInt(),
                            fanConfig.high.currentVal.toInt(),
                            basicSettings.fanMode,
                            basicSettings.conditioningMode,
                            fanLoopOutput,
                            analogOutStages,
                            isFanGoodRun(doorWindowSensorOpenStatus, equip)
                        )
                    }
                    MyStatPipe4AnalogOutMapping.CHILLED_MODULATING_VALUE -> {
                        doAnalogOperation(
                            (canWeDoCooling(basicSettings.conditioningMode) && (basicSettings.fanMode != MyStatFanStages.OFF)),
                            analogOutStages,
                            StatusMsgKeys.COOLING.name,
                            coolingLoopOutput,
                            equip.chilledWaterModulatingCoolValve
                        )
                    }
                    MyStatPipe4AnalogOutMapping.HOT_MODULATING_VALUE -> {
                        doAnalogOperation(
                            canWeDoHeating(basicSettings.conditioningMode),
                            analogOutStages,
                            StatusMsgKeys.HEATING.name,
                            heatingLoopOutput,
                            equip.hotWaterModulatingHeatValve
                        )
                    }
                    else -> {}

                }
            }
        }
    }

    private fun triggerFanForAuxIfRequired(
        basicSettings: MyStatBasicSettings,
        configuration: MyStatPipe4Configuration,
        equip: MyStatPipe4Equip
    ) {
        if (basicSettings.fanMode == MyStatFanStages.AUTO) {
            resetFanIfRequired(equip, basicSettings)
            if (isAux1Exists() && aux1Active(equip)) {
                resetFan(equip.relayStages, equip.analogOutStages, basicSettings)
                val isRelayActive = operateAuxBasedOnFan(equip)
                val isAnalogAvailable = runSpecificAnalogFanSpeed(configuration, equip.analogOutStages)
                if (isAnalogAvailable.not() && isRelayActive.not()) {
                    logIt("No Fan mapping available for Aux1 operation")
                    resetAux(equip.relayStages)
                }
            }
        }
    }


    private fun resetAux(relayStages: HashMap<String, Int>) {
        if (isConfigPresent(MyStatPipe4RelayMapping.AUX_HEATING_STAGE1)) {
            resetLogicalPoint(relayLogicalPoints[MyStatPipe4RelayMapping.AUX_HEATING_STAGE1.ordinal]!!)
        }
        relayStages.remove(MyStatPipe4RelayMapping.AUX_HEATING_STAGE1.name)
    }

    private fun operateAuxBasedOnFan(equip: MyStatPipe4Equip): Boolean {
        val sequenceMap = mutableMapOf(
            equip.fanHighSpeed to Stage.FAN_2.displayName,
            equip.fanLowSpeed to Stage.FAN_1.displayName,
            equip.fanLowSpeedVentilation to Stage.FAN_1.displayName,
            equip.fanEnable to StatusMsgKeys.FAN_ENABLED.name,
            equip.occupiedEnable to StatusMsgKeys.EQUIP_ON.name
        )
        // Before operating AUX based fan reset all points except fanEnable and occupiedEnable
        sequenceMap.forEach { (point, statusMsg) ->
            if ((point.domainName != DomainName.fanEnable && point.domainName != DomainName.occupiedEnable) && point.pointExists()) {
                point.writeHisVal(0.0)
            }
            equip.relayStages.remove(statusMsg)
        }
        sequenceMap.forEach { (point, statusMsg) ->
            if (point.pointExists()) {
                point.writeHisVal(1.0)
                equip.relayStages[statusMsg] = 1
                logIt("Operating AUX based fan: ${point.domainName}")
                return true
            }
        }
        return false
    }


    private fun resetFanIfRequired(equip: MyStatPipe4Equip, basicSettings: MyStatBasicSettings) {
        if ((isAux1Exists() && aux1Active(equip))) {
            resetFan(equip.relayStages, equip.analogOutStages, basicSettings)
        }
    }

    private fun isAux1Exists() = isConfigPresent(MyStatPipe4RelayMapping.AUX_HEATING_STAGE1)

    private fun aux1Active(equip: MyStatPipe4Equip) = equip.auxHeatingStage1.readHisVal() > 0
    
    private fun resetFan(
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        basicSettings: MyStatBasicSettings
    ) {
        if (basicSettings.fanMode == MyStatFanStages.AUTO || basicSettings.fanMode == MyStatFanStages.OFF) {
            if (isConfigPresent(MyStatPipe4RelayMapping.FAN_LOW_SPEED)) {
                resetLogicalPoint(relayLogicalPoints[MyStatPipe4RelayMapping.FAN_LOW_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_1.displayName)
            }
            if (isConfigPresent(MyStatPipe4RelayMapping.FAN_LOW_VENTILATION)) {
                resetLogicalPoint(relayLogicalPoints[MyStatPipe4RelayMapping.FAN_LOW_VENTILATION.ordinal]!!)
                relayStages.remove(Stage.FAN_1.displayName)
            }
            if (isConfigPresent(MyStatPipe4RelayMapping.FAN_HIGH_SPEED)) {
                resetLogicalPoint(relayLogicalPoints[MyStatPipe4RelayMapping.FAN_HIGH_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_2.displayName)
            }
            if (analogLogicalPoints.containsKey(MyStatPipe4AnalogOutMapping.FAN_SPEED.ordinal)) {
                resetLogicalPoint(analogLogicalPoints[MyStatPipe4AnalogOutMapping.FAN_SPEED.ordinal]!!)
                analogOutStages.remove(StatusMsgKeys.FAN_SPEED.name)
            }
        }
    }
    private fun runSpecificAnalogFanSpeed(
        config: MyStatPipe4Configuration,
        analogOutStages: HashMap<String, Int>
    ): Boolean {
        var isAnalogAvailable = false
        if (config.universalOut1.enabled && config.universalOut1Association.associationVal == MyStatPipe4AnalogOutMapping.FAN_SPEED.ordinal) {
            val fanSpeedValue = config.analogOut1FanSpeedConfig.high.currentVal
            updateLogicalPoint(logicalPointsList[Port.UNIVERSAL_OUT_ONE]!!, fanSpeedValue)
            analogOutStages[StatusMsgKeys.FAN_SPEED.name] = 1
            isAnalogAvailable = true
        }
        if (config.universalOut2.enabled && config.universalOut2Association.associationVal == MyStatPipe4AnalogOutMapping.FAN_SPEED.ordinal) {
            val fanSpeedValue = config.analogOut2FanSpeedConfig.high.currentVal
            updateLogicalPoint(logicalPointsList[Port.UNIVERSAL_OUT_TWO]!!, fanSpeedValue)
            analogOutStages[StatusMsgKeys.FAN_SPEED.name] = 1
            isAnalogAvailable = true
        }
        return isAnalogAvailable
    }

    private fun doFanOperationTitle24(
        basicSettings: MyStatBasicSettings,
        equip: MyStatPipe4Equip,
        configuration: MyStatPipe4Configuration
    ) {
        if (basicSettings.fanMode == MyStatFanStages.AUTO && runFanLowDuringDoorWindow) {
            val lowestStage = configuration.getLowestFanSelected(fanLowVentilationAvailable.readHisVal() > 0)
            if (lowestStage == null) resetFan(equip.relayStages, equip.analogOutStages, basicSettings)
            resetFanLowestFanStatus()
            when (lowestStage) {
                MyStatPipe4RelayMapping.FAN_LOW_SPEED, MyStatPipe4RelayMapping.FAN_LOW_VENTILATION -> lowestStageFanLow = true
                MyStatPipe4RelayMapping.FAN_HIGH_SPEED -> lowestStageFanHigh = true
                else -> {
                    // Do nothing
                }
            }
        }
    }

    private fun operateRelays(
        config: MyStatPipe4Configuration, basicSettings: MyStatBasicSettings,
        equip: MyStatPipe4Equip, controllerFactory: MyStatControlFactory
    ) {
        controllerFactory.addPipe4Controllers(config, fanLowVentilationAvailable)
        runControllers(equip, basicSettings)
    }

    private fun runControllers(
        equip: MyStatPipe4Equip, basicSettings: MyStatBasicSettings
    ) {
        zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            val result = controller.runController()
            updateRelayStatus(controllerName, result, equip, basicSettings)
        }
    }

    private fun updateRelayStatus(
        controllerName: String, result: Any, equip: MyStatPipe4Equip,
        basicSettings: MyStatBasicSettings
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
                        0 -> isMyStatLowUserIntentFanMode(mode)
                        1 -> isMyStatHighUserIntentFanMode(mode)
                        else -> false
                    }
                }

                val isFanGoodToRun = isFanGoodRun(doorWindowSensorOpenStatus, equip)

                fun isStageActive(
                    stage: Int, currentState: Boolean, isLowestStageActive: Boolean
                ): Boolean {
                    val mode = equip.fanOpMode.readPriorityVal().toInt()
                    return if (isFanGoodToRun && mode == MyStatFanStages.AUTO.ordinal) {
                        (basicSettings.fanMode != MyStatFanStages.OFF && currentState || (isLowestStageActive && runFanLowDuringDoorWindow))
                    } else {
                        checkUserIntentAction(stage)
                    }
                }

                val fanStages = result as List<Pair<Int, Boolean>>

                val highExist =
                    fanStages.find { it.first == MyStatPipe4RelayMapping.FAN_HIGH_SPEED.ordinal }
                val lowExist =
                    fanStages.find { it.first == MyStatPipe4RelayMapping.FAN_LOW_SPEED.ordinal }

                var isHighActive = false
                val isHighExist = equip.fanHighSpeed.pointExists()

                if (isHighExist && highExist != null) {
                    isHighActive =
                        isStageActive(highExist.first, highExist.second, lowestStageFanHigh)
                    updateStatus(equip.fanHighSpeed, isHighActive, Stage.FAN_2.displayName)
                }

                if ((equip.fanLowSpeed.pointExists() || equip.fanLowSpeedVentilation.pointExists()) && lowExist != null) {
                    val isLowActive = if (isHighExist && isHighActive) false else isStageActive(
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
                updateStatus(equip.auxHeatingStage1, status, StatusMsgKeys.AUX_HEATING_STAGE1.name)
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

    private fun isFanGoodRun(isDoorWindowOpen: Boolean, equip: MyStatPipe4Equip): Boolean {
        return if (fanLowVentilationAvailable.readHisVal() > 0) true
        else if (isDoorWindowOpen || heatingLoopOutput > 0) {
            // If current direction is heating then check allow only when  heating is available
            (isConfigPresent(MyStatPipe4RelayMapping.HOT_WATER_VALVE) || equip.hotWaterModulatingHeatValve.pointExists()
                    || isConfigPresent(MyStatPipe4RelayMapping.AUX_HEATING_STAGE1))
        } else if (isDoorWindowOpen || coolingLoopOutput > 0) {
            (isConfigPresent(MyStatPipe4RelayMapping.CHILLED_WATER_VALVE) || equip.chilledWaterModulatingCoolValve.pointExists())
        } else {
            false
        }
    }

    private fun isConfigPresent(mapping: MyStatPipe4RelayMapping) = relayLogicalPoints.containsKey(mapping.ordinal)

}