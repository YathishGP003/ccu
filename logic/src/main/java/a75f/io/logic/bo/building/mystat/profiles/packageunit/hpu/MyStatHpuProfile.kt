package a75f.io.logic.bo.building.mystat.profiles.packageunit.hpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.MyStatFanStages
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.mystat.configs.MyStatFanConfig
import a75f.io.logic.bo.building.mystat.configs.MyStatHpuAnalogOutMapping
import a75f.io.logic.bo.building.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.mystat.configs.MyStatHpuRelayMapping
import a75f.io.logic.bo.building.mystat.profiles.packageunit.MyStatPackageUnitProfile
import a75f.io.logic.bo.building.mystat.profiles.packageunit.getMyStatHpuAnalogOutputPoints
import a75f.io.logic.bo.building.mystat.profiles.packageunit.getMyStatHpuRelayOutputPoints
import a75f.io.logic.bo.building.mystat.profiles.util.MyStatBasicSettings
import a75f.io.logic.bo.building.mystat.profiles.util.MyStatFanModeCacheStorage
import a75f.io.logic.bo.building.mystat.profiles.util.MyStatLoopController
import a75f.io.logic.bo.building.mystat.profiles.util.MyStatTuners
import a75f.io.logic.bo.building.mystat.profiles.util.MyStatUserIntents
import a75f.io.logic.bo.building.mystat.profiles.util.fetchMyStatBasicSettings
import a75f.io.logic.bo.building.mystat.profiles.util.fetchMyStatTuners
import a75f.io.logic.bo.building.mystat.profiles.util.fetchMyStatUserIntents
import a75f.io.logic.bo.building.mystat.profiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.mystat.profiles.util.getMyStatLogicalPointList
import a75f.io.logic.bo.building.mystat.profiles.util.updateLogicalPoint
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.util.uiutils.MyStatUserIntentHandler

/**
 * Created by Manjunath K on 16-01-2025.
 */

class MyStatHpuProfile : MyStatPackageUnitProfile() {

    private val hpuDeviceMap: MutableMap<Int, MyStatHpuEquip> = mutableMapOf()

    private val myStatLoopController = MyStatLoopController()
    private lateinit var curState: ZoneState
    override lateinit var occupancyStatus: Occupancy

    private var analogLogicalPoints: HashMap<Int, String> = HashMap()
    private var relayLogicalPoints: HashMap<Int, String> = HashMap()


    override fun updateZonePoints() {
        hpuDeviceMap.forEach { (nodeAddress, equip) ->
            hpuDeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as MyStatHpuEquip
            CcuLog.d(L.TAG_CCU_MSHPU, "Process HPU: equipRef =  ${equip.nodeAddress}")
            processHpuProfile(equip)
        }
    }

    fun processHpuProfile(equip: MyStatHpuEquip) {

        if (Globals.getInstance().isTestMode) {
            CcuLog.d(L.TAG_CCU_MSHPU, "Test mode is on: ${equip.equipRef}")
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

        val relayOutputStatus = HashMap<String, Int>()
        val analogOutputStatus = HashMap<String, Int>()

        val config = getMyStatConfiguration(equip.equipRef) as MyStatHpuConfiguration

        logicalPointsList = getMyStatLogicalPointList(equip, config)

        relayLogicalPoints = getMyStatHpuRelayOutputPoints(equip)
        analogLogicalPoints = getMyStatHpuAnalogOutputPoints(equip)

        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode

        val myStatTuners = fetchMyStatTuners(equip)
        val userIntents = fetchMyStatUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val fanModeSaved = MyStatFanModeCacheStorage().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchMyStatBasicSettings(equip)

        CcuLog.d(
            L.TAG_CCU_MSHPU,
            "Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}"
        )
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        CcuLog.d(
            L.TAG_CCU_MSHPU,
            "After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}"
        )

        myStatLoopController.initialise(tuners = myStatTuners)
        myStatLoopController.dumpLogs()
        handleChangeOfDirection(userIntents, myStatLoopController)

        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        compressorLoopOutput = 0
        dcvLoopOutput = 0

        val currentOperatingMode = equip.occupancyMode.readHisVal().toInt()
        evaluateLoopOutputs(userIntents, myStatLoopController)
        deriveFanLoopOutput(basicSettings, myStatTuners)
        calculateDcvLoop(equip, config.co2Threshold.currentVal, config.co2DamperOpeningRate.currentVal)
        updateOccupancyDetection(equip)

        doorWindowSensorOpenStatus = runForDoorWindowSensor(config, equip, analogOutputStatus, relayOutputStatus)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)
        if (occupancyStatus == Occupancy.WINDOW_OPEN) resetLoops()
        runForKeyCardSensor(config, equip)
        updateLoopOutputs(equip, coolingLoopOutput, heatingLoopOutput, fanLoopOutput, dcvLoopOutput,true)

        CcuLog.i(
            L.TAG_CCU_MSHPU,
            "Fan speed multiplier:  ${myStatTuners.analogFanSpeedMultiplier} " +
                    "AuxHeating1Activate: ${myStatTuners.auxHeating1Activate} " +
                    "waterValveSamplingOnTime: ${myStatTuners.waterValveSamplingOnTime}" +
                    "  waterValveSamplingWaitTime : ${myStatTuners.waterValveSamplingWaitTime} \n" +
                    "waterValveSamplingDuringLoopDeadbandOnTime: ${myStatTuners.waterValveSamplingDuringLoopDeadbandOnTime} " +
                    " waterValveSamplingDuringLoopDeadbandWaitTime : ${myStatTuners.waterValveSamplingDuringLoopDeadbandWaitTime} \n"
                    + "Current Occupancy: ${Occupancy.values()[currentOperatingMode]} \n" + "Fan Mode : ${basicSettings.fanMode} " +
                    "Conditioning Mode ${basicSettings.conditioningMode} \n"
                    + "Current Temp : $currentTemp Desired (Heating: ${userIntents.zoneHeatingTargetTemperature}"
                    + " Cooling: ${userIntents.zoneCoolingTargetTemperature})\n"
                    + "Loop Outputs: (Heating Loop: $heatingLoopOutput Cooling Loop:" +
                    " $coolingLoopOutput Fan Loop: $fanLoopOutput  Compressor Loop : $compressorLoopOutput dcvLoopOutput : $dcvLoopOutput)" +
                    " \n"
        )

        if (basicSettings.fanMode != MyStatFanStages.OFF) {
            operateRelays(
                config, myStatTuners, userIntents, basicSettings,
                relayOutputStatus, analogOutputStatus, relayLogicalPoints, equip
            )
            operateAnalogOutputs(config, equip, basicSettings, analogOutputStatus, relayLogicalPoints)
            if (basicSettings.fanMode == MyStatFanStages.AUTO) {
                runFanOperationBasedOnAuxStages(
                    relayOutputStatus, analogOutputStatus, config,
                    relayLogicalPoints, analogLogicalPoints
                )
            }
        } else {
            resetLogicalPoints()
        }

        updateOperatingMode(currentTemp, averageDesiredTemp, basicSettings, equip)
        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())
        showOutputStatus()
        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState =
            ZoneTempState.EMERGENCY
        MyStatUserIntentHandler.updateMyStatStatus(
            equip.equipRef,
            relayOutputStatus,
            analogOutputStatus,
            temperatureState,
            equip
        )
        if (occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        CcuLog.i(L.TAG_CCU_MSHPU, "----------------------------------------------------------")

    }

    private fun operateRelays(
        config: MyStatHpuConfiguration, tuner: MyStatTuners,
        userIntents: MyStatUserIntents, basicSettings: MyStatBasicSettings,
        relayStages: HashMap<String, Int>, analogOutStages: HashMap<String, Int>, relayOutputPoints: HashMap<Int, String>,
        equip: MyStatHpuEquip
    ) {
        config.apply {
            listOf(
                Triple(relay1Enabled.enabled, relay1Association.associationVal, Port.RELAY_ONE),
                Triple(relay2Enabled.enabled, relay2Association.associationVal, Port.RELAY_TWO),
                Triple(relay3Enabled.enabled, relay3Association.associationVal, Port.RELAY_THREE),
                Triple(relay4Enabled.enabled, relay4Association.associationVal, Port.RELAY_FOUR)
            ).forEach { (enabled, association, port) ->
                if (enabled) {
                    handleRelayState(
                        association, config, port, tuner, userIntents,
                        basicSettings, relayStages, analogOutStages, relayOutputPoints, equip
                    )
                }
            }
        }

    }


    private fun handleRelayState(
        association: Int,
        config: MyStatHpuConfiguration,
        port: Port,
        tuner: MyStatTuners,
        userIntents: MyStatUserIntents,
        basicSettings: MyStatBasicSettings,
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        relayOutputPoints: HashMap<Int, String>,
        equip: MyStatHpuEquip
    ) {
        val relayMapping = MyStatHpuRelayMapping.values().find { it.ordinal == association }
        when (relayMapping) {
            MyStatHpuRelayMapping.COMPRESSOR_STAGE1, MyStatHpuRelayMapping.COMPRESSOR_STAGE2 -> {
                if (basicSettings.conditioningMode != StandaloneConditioningMode.OFF && compressorLoopOutput != 0) {
                    runRelayForCompressor(relayMapping, port, tuner, relayStages)
                } else {
                    resetPort(port)
                }
            }

            MyStatHpuRelayMapping.AUX_HEATING_STAGE1 -> {
                runAuxHeatingStages(
                    port, userIntents, basicSettings, relayStages, tuner.auxHeating1Activate
                )
            }

            MyStatHpuRelayMapping.FAN_LOW_SPEED, MyStatHpuRelayMapping.FAN_HIGH_SPEED -> {
                runRelayForFanSpeed(
                    relayMapping, port, config, tuner, relayStages, basicSettings, relayOutputPoints
                )
            }

            MyStatHpuRelayMapping.CHANGE_OVER_O_COOLING -> {
                if (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO
                    || basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY) {
                    val status = if (coolingLoopOutput > 0) 1.0 else 0.0
                    updateLogicalPoint(logicalPointsList[port]!!, status)
                    if (status == 1.0) relayStages[MyStatHpuRelayMapping.CHANGE_OVER_O_COOLING.name] = 1
                }
            }

            MyStatHpuRelayMapping.CHANGE_OVER_B_HEATING -> {
                if (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO
                    || basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY) {
                    val status = if (heatingLoopOutput > 0) 1.0 else 0.0
                    updateLogicalPoint(logicalPointsList[port]!!, status)
                    if (status == 1.0) relayStages[MyStatHpuRelayMapping.CHANGE_OVER_B_HEATING.name] = 1
                }
            }

            /**
             * intentionally we are sending analogOutStages for dcv to handle status message
             */
            MyStatHpuRelayMapping.DCV_DAMPER -> doDcvDamperOperation(equip, port, tuner.relayActivationHysteresis, analogOutStages, config.co2Threshold.currentVal, false)
            MyStatHpuRelayMapping.FAN_ENABLED -> doFanEnabled(curState, port, fanLoopOutput)
            MyStatHpuRelayMapping.OCCUPIED_ENABLED -> doOccupiedEnabled(port)
            MyStatHpuRelayMapping.HUMIDIFIER -> doHumidifierOperation(
                port, tuner.humidityHysteresis,
                userIntents.targetMinInsideHumidity,
                equip.zoneHumidity.readHisVal()
            )

            MyStatHpuRelayMapping.DEHUMIDIFIER -> doDeHumidifierOperation(
                port, tuner.humidityHysteresis,
                userIntents.targetMaxInsideHumidity,
                equip.zoneHumidity.readHisVal()
            )

            else -> {}
        }
    }

    private fun runAuxHeatingStages(
        port: Port,
        userIntents: MyStatUserIntents,
        basicSettings: MyStatBasicSettings,
        relayStages: HashMap<String, Int>,
        auxHeatingActivateTuner: Double
    ) {

        fun isEligibleForAuxHeatingStage(): Boolean {
            return heatingLoopOutput != 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO || basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY)
        }

        if (isEligibleForAuxHeatingStage()) {
            if (currentTemp < (userIntents.zoneHeatingTargetTemperature - auxHeatingActivateTuner)) {
                updateLogicalPoint(logicalPointsList[port]!!, 1.0)
                relayStages[MyStatHpuRelayMapping.AUX_HEATING_STAGE1.name] = 1
            } else if (currentTemp >= (userIntents.zoneHeatingTargetTemperature - (auxHeatingActivateTuner - 1))) {
                updateLogicalPoint(logicalPointsList[port]!!, 0.0)
                relayStages.remove(MyStatHpuRelayMapping.AUX_HEATING_STAGE1.name)
            } else if (hayStack.readHisValById(logicalPointsList[port]!!) == 1.0) {
                relayStages[MyStatHpuRelayMapping.AUX_HEATING_STAGE1.name] = 1
            }

        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
            relayStages.remove(MyStatHpuRelayMapping.AUX_HEATING_STAGE1.name)
        }
    }

    private fun runRelayForCompressor(
        association: MyStatHpuRelayMapping,
        whichPort: Port,
        tuner: MyStatTuners,
        relayStages: HashMap<String, Int>
    ) {
        when (association) {
            MyStatHpuRelayMapping.COMPRESSOR_STAGE1 -> {
                doCompressorStage1(
                    whichPort, compressorLoopOutput, tuner.relayActivationHysteresis,
                    relayStages,getZoneMode()
                )
            }

            MyStatHpuRelayMapping.COMPRESSOR_STAGE2 -> {
                doCompressorStage2(
                    whichPort, compressorLoopOutput, tuner.relayActivationHysteresis,
                    50, relayStages, getZoneMode()
                )
            }

            else -> {}
        }

        if (getCurrentPortStatus(whichPort) == 1.0) curState = ZoneState.COOLING
    }

    private fun runRelayForFanSpeed(
        relayAssociation: MyStatHpuRelayMapping,
        whichPort: Port,
        config: MyStatHpuConfiguration,
        tuner: MyStatTuners,
        relayStages: HashMap<String, Int>,
        basicSettings: MyStatBasicSettings,
        relayOutputPoints: HashMap<Int, String>
    ) {
        if (basicSettings.fanMode == MyStatFanStages.AUTO && basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
            CcuLog.i(L.TAG_CCU_HSHPU, "Cond is Off , Fan is Auto   ")
            resetPort(whichPort)
            return
        }

        val lowestStage = config.getLowestFanSelected()
        resetFanLowestFanStatus()

        when (lowestStage) {
            MyStatHpuRelayMapping.FAN_LOW_SPEED -> setFanLowestFanLowStatus(true)
            MyStatHpuRelayMapping.FAN_HIGH_SPEED -> setFanLowestFanHighStatus(true)
            else -> {}
        }

        when (relayAssociation) {
            MyStatHpuRelayMapping.FAN_LOW_SPEED -> {
                doFanLowSpeed(
                    logicalPointsList[whichPort]!!,
                    basicSettings.fanMode,
                    fanLoopOutput,
                    tuner.relayActivationHysteresis,
                    relayStages,
                    runFanLowDuringDoorWindow
                )
            }

            MyStatHpuRelayMapping.FAN_HIGH_SPEED -> {
                if (isAuxAvailableAndActive(relayOutputPoints) && basicSettings.fanMode == MyStatFanStages.AUTO) return
                doFanHighSpeed(
                    logicalPointsList[whichPort]!!,
                    basicSettings.fanMode,
                    fanLoopOutput,
                    tuner.relayActivationHysteresis,
                    relayStages,
                    runFanLowDuringDoorWindow
                )
            }

            else -> {}
        }
    }

    private fun isAuxAvailableAndActive(relayOutputPoints: HashMap<Int, String>): Boolean {
        return (relayOutputPoints.containsKey(MyStatHpuRelayMapping.AUX_HEATING_STAGE1.ordinal) && getCurrentLogicalPointStatus(
            relayOutputPoints[MyStatHpuRelayMapping.AUX_HEATING_STAGE1.ordinal]!!
        ) == 1.0)
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
                if (logicalPointsList.containsKey(Port.ANALOG_OUT_ONE)) {
                    CcuLog.i(
                        L.TAG_CCU_HSHPU,
                        "${Port.ANALOG_OUT_ONE} = $analogMapping : ${
                            getCurrentLogicalPointStatus(logicalPointsList[Port.ANALOG_OUT_ONE]!!)
                        }"
                    )
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

        CcuLog.i(
            L.TAG_CCU_HSHPU,
            "Aux Based fan : aux1AvailableAndActive $aux1AvailableAndActive aux2AvailableAndActive "
        )
        if (aux1AvailableAndActive) operateAuxBasedOnFan(relayStages, relayOutputPoints)

        // Run the fan speed control if either aux1 or aux2 is available and active
        if ((aux1AvailableAndActive)) {
            runSpecificAnalogFanSpeed(
                config,
                analogOutStages,
                relayOutputPoints,
                analogOutputPoints
            )
        }
    }

    private fun runSpecificAnalogFanSpeed(
        config: MyStatHpuConfiguration,
        analogOutStages: HashMap<String, Int>,
        relayOutputPoints: HashMap<Int, String>,
        analogOutputPoints: HashMap<Int, String>
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
                CcuLog.i(L.TAG_CCU_HSHPU, "Fan Speed : $percentage")
                updateLogicalPoint(
                    analogOutputPoints[MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal]!!, percentage
                )
                analogOutStages[AnalogOutput.FAN_SPEED.name] = 1
            }
        }
    }


    // New requirement for aux and fan operations If we do not have fan then no aux
    private fun operateAuxBasedOnFan(
        relayStages: HashMap<String, Int>, relayOutputPoints: HashMap<Int, String>
    ) {

        fun getFanStage(mapping: MyStatHpuRelayMapping): Stage? {
            return when (mapping) {
                MyStatHpuRelayMapping.FAN_LOW_SPEED -> Stage.FAN_1
                MyStatHpuRelayMapping.FAN_HIGH_SPEED -> Stage.FAN_2
                else -> null
            }
        }

        fun getAvailableFanSpeed(relayOutputPoints: HashMap<Int, String>) = Pair(
            relayOutputPoints.containsKey(MyStatHpuRelayMapping.FAN_LOW_SPEED.ordinal),
            relayOutputPoints.containsKey(MyStatHpuRelayMapping.FAN_HIGH_SPEED.ordinal)
        )

        val (lowAvailable, highAvailable) = getAvailableFanSpeed(relayOutputPoints)

        fun deriveFanStage(): MyStatHpuRelayMapping {
            return when {
                highAvailable -> MyStatHpuRelayMapping.FAN_HIGH_SPEED
                lowAvailable -> MyStatHpuRelayMapping.FAN_LOW_SPEED
                else -> MyStatHpuRelayMapping.FAN_ENABLED
            }
        }

        if (!lowAvailable && !highAvailable) {
            resetAux(relayStages, relayOutputPoints) // non of the fans are available
        }

        val stage = deriveFanStage()
        val fanStatusMessage = getFanStage(stage)
        CcuLog.i(L.TAG_CCU_HSHPU, "operateAuxBasedOnFan: derived mode is $stage")
        // operate specific fan  (low, medium, high) based on derived stage order
        if (stage != MyStatHpuRelayMapping.FAN_ENABLED) {
            updateLogicalPoint(relayOutputPoints[stage.ordinal]!!, 1.0)
            relayStages[fanStatusMessage!!.displayName] = 1
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

            else -> {
                CcuLog.i(L.TAG_CCU_HSHPU, "operateAuxBasedOnFan: derived mode is invalid")
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

    private fun runForDoorWindowSensor(
        config: MyStatConfiguration,
        equip: MyStatEquip,
        analogOutStages: HashMap<String, Int>,
        relayStages: HashMap<String, Int>
    ): Boolean {

        val isDoorOpen = isDoorOpenState(config, equip)
        CcuLog.d(L.TAG_CCU_MSHPU, " is Door Open ? $isDoorOpen")
        if (isDoorOpen) {
            resetLoops()
            resetLogicalPoints()
            analogOutStages.clear()
            relayStages.clear()
        }
        return isDoorOpen
    }

    private fun runForKeyCardSensor(config: MyStatConfiguration, equip: MyStatEquip) {
        val isKeyCardEnabled = (config.universalIn1Enabled.enabled
                && config.universalIn1Association.associationVal == MyStatConfiguration.UniversalMapping.KEY_CARD_SENSOR.ordinal)
        keyCardIsInSlot((if (isKeyCardEnabled) 1.0 else 0.0), if (equip.keyCardSensor.readHisVal() > 0) 1.0 else 0.0, equip)
    }



    private fun deriveFanLoopOutput(basicSettings: MyStatBasicSettings, tuners: MyStatTuners) {
        if (coolingLoopOutput > 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY || basicSettings.conditioningMode == StandaloneConditioningMode.AUTO)) {
            fanLoopOutput =
                ((coolingLoopOutput * tuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
            compressorLoopOutput = coolingLoopOutput
        } else if (heatingLoopOutput > 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY || basicSettings.conditioningMode == StandaloneConditioningMode.AUTO)) {
            fanLoopOutput =
                ((heatingLoopOutput * tuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
            compressorLoopOutput = heatingLoopOutput
        }
    }

    private fun showOutputStatus() {
        logicalPointsList.toSortedMap().forEach { (port, logicalPointId) ->
            var mapping: String? = null
            when (port) {
                Port.RELAY_ONE, Port.RELAY_TWO, Port.RELAY_THREE, Port.RELAY_FOUR -> {
                    val enumPos =
                        relayLogicalPoints.entries.find { it.value.contentEquals(logicalPointId) }?.key
                    mapping = MyStatHpuRelayMapping.values().find { it.ordinal == enumPos }?.name
                }

                Port.ANALOG_OUT_ONE -> {
                    val enumPos =
                        analogLogicalPoints.entries.find { it.value.contentEquals(logicalPointId) }?.key
                    mapping =
                        MyStatHpuAnalogOutMapping.values().find { it.ordinal == enumPos }?.name
                }

                else -> {}
            }

            CcuLog.i(
                L.TAG_CCU_MSHPU,
                "$port = $mapping : ${getCurrentLogicalPointStatus(logicalPointsList[port]!!)}"
            )
        }
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