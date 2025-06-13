package a75f.io.logic.bo.building.statprofiles.mystat.profiles.fancoilunit.pipe2

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2RelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.profiles.fancoilunit.MyStatFanCoilUnit
import a75f.io.logic.bo.building.statprofiles.util.MyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.MyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.StatLoopController
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.fetchMyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.fetchMyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getMyStatAnalogOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatLogicalPointList
import a75f.io.logic.bo.building.statprofiles.util.getMyStatRelayOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.milliToMin
import a75f.io.logic.bo.building.statprofiles.util.updateLogicalPoint
import a75f.io.logic.bo.building.statprofiles.util.updateLoopOutputs
import a75f.io.logic.bo.building.statprofiles.util.updateOperatingMode
import a75f.io.logic.util.uiutils.MyStatUserIntentHandler

/**
 * Created by Manjunath K on 16-01-2025.
 */

class MyStatPipe2Profile: MyStatFanCoilUnit() {


    private var supplyWaterTempTh2 = 0.0
    private var heatingThreshold = 85.0
    private var coolingThreshold = 65.0

    private val pipe2DeviceMap: MutableMap<Int, MyStatPipe2Equip> = mutableMapOf()

    private val myStatLoopController = StatLoopController()
    override lateinit var occupancyStatus: Occupancy
    private lateinit var curState: ZoneState

    private var analogLogicalPoints: HashMap<Int, String> = HashMap()
    private var relayLogicalPoints: HashMap<Int, String> = HashMap()

    override fun getProfileType() = ProfileType.MYSTAT_PIPE2

    override fun updateZonePoints() {
        pipe2DeviceMap.forEach { (nodeAddress, equip) ->
            pipe2DeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as MyStatPipe2Equip
            CcuLog.d( L.TAG_CCU_MSPIPE2,"Process Pipe2: equipRef =  ${equip.nodeAddress}")
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
            CcuLog.d(L.TAG_CCU_MSPIPE2, "Test mode is on: ${equip.equipRef}")
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

        val config = getMyStatConfiguration(equip.equipRef)

        logicalPointsList = getMyStatLogicalPointList(equip, config!!)

        relayLogicalPoints = getMyStatRelayOutputPoints(equip)
        analogLogicalPoints = getMyStatAnalogOutputPoints(equip)

        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode

        val myStatTuners = fetchMyStatTuners(equip) as MyStatTuners
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val fanModeSaved = FanModeCacheStorage.getMyStatFanModeCache().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchMyStatBasicSettings(equip)

        CcuLog.d(L.TAG_CCU_MSPIPE2, "Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        CcuLog.d(L.TAG_CCU_MSPIPE2, "After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")

        heatingThreshold = myStatTuners.heatingThreshold
        coolingThreshold = myStatTuners.coolingThreshold

        myStatLoopController.initialise(tuners = myStatTuners)
        myStatLoopController.dumpLogs()
        handleChangeOfDirection(userIntents, myStatLoopController)

        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        dcvLoopOutput = 0

        val currentOperatingMode = equip.occupancyMode.readHisVal().toInt()
        evaluateLoopOutputs(userIntents, myStatLoopController)
        deriveFanLoopOutput(basicSettings, myStatTuners)
        calculateDcvLoop(equip, config.co2Threshold.currentVal, config.co2DamperOpeningRate.currentVal)
        updateOccupancyDetection(equip)

        doorWindowSensorOpenStatus = runForDoorWindowSensor(config, equip, analogOutputStatus, relayOutputStatus)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)
        supplyWaterTempTh2 = equip.leavingWaterTemperature.readHisVal()
        if (occupancyStatus == Occupancy.WINDOW_OPEN) resetLoops()
        updateLoopOutputs(
            coolingLoopOutput, equip.coolingLoopOutput,
            heatingLoopOutput, equip.heatingLoopOutput,
            fanLoopOutput, equip.fanLoopOutput,
            dcvLoopOutput, equip.dcvLoopOutput,
        )


        CcuLog.i(L.TAG_CCU_MSPIPE2,
            "Fan speed multiplier:  ${myStatTuners.analogFanSpeedMultiplier} " +
                    "AuxHeating1Activate: ${myStatTuners.auxHeating1Activate} " +
                    "waterValveSamplingOnTime: ${myStatTuners.waterValveSamplingOnTime}  waterValveSamplingWaitTime : ${myStatTuners.waterValveSamplingWaitTime} \n" +
                    "waterValveSamplingDuringLoopDeadbandOnTime: ${myStatTuners.waterValveSamplingDuringLoopDeadbandOnTime}  waterValveSamplingDuringLoopDeadbandWaitTime : ${myStatTuners.waterValveSamplingDuringLoopDeadbandWaitTime} \n" +
                    "Current Occupancy: ${Occupancy.values()[currentOperatingMode]} \n" +
                    "supplyWaterTempTh2 : $supplyWaterTempTh2 \n" +
                    "Fan Mode : ${basicSettings.fanMode} Conditioning Mode ${basicSettings.conditioningMode} \n" +
                    "heatingThreshold: $heatingThreshold  coolingThreshold : $coolingThreshold \n" +
                    "Current Temp : $currentTemp Desired (Heating: ${userIntents.heatingDesiredTemp}" +
                    " Cooling: ${userIntents.coolingDesiredTemp})\n" +
                    "Loop Outputs: (Heating Loop: $heatingLoopOutput Cooling Loop: $coolingLoopOutput Fan Loop: $fanLoopOutput DCVLoop: $dcvLoopOutput) \n"
        )

        operateRelays(config as MyStatPipe2Configuration, myStatTuners, userIntents, basicSettings, relayOutputStatus, relayLogicalPoints, equip, analogOutputStatus)
        handleAnalogOutState(config, equip, basicSettings, analogOutputStatus, userIntents)
        runAlgorithm(equip, basicSettings, myStatTuners, relayOutputStatus, analogOutputStatus, config, userIntents)

        // Run the title 24 fan operation after the reset of all PI output is done
        doFanOperationTitle24(myStatTuners, basicSettings, relayOutputStatus, userIntents, analogOutputStatus, config)

        updateOperatingMode(currentTemp, averageDesiredTemp, basicSettings.conditioningMode, equip.operatingMode)
        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())
        showOutputStatus()
        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY
        MyStatUserIntentHandler.updateMyStatStatus(equip.equipRef, relayOutputStatus, analogOutputStatus, temperatureState, equip)
        if (occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        CcuLog.i(L.TAG_CCU_MSPIPE2, "----------------------------------------------------------")
    }


    private fun deriveFanLoopOutput (basicSettings: MyStatBasicSettings, tuners: MyStatTuners) {
        if (coolingLoopOutput > 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY
                    || basicSettings.conditioningMode == StandaloneConditioningMode.AUTO)) {
            fanLoopOutput = ((coolingLoopOutput * tuners.analogFanSpeedMultiplier).coerceAtMost(100.0).toInt())
        } else if (heatingLoopOutput > 0 && ((basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY && supplyWaterTempTh2 > coolingThreshold)
                    || (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO && supplyWaterTempTh2 > coolingThreshold))
        ) {
            fanLoopOutput = (heatingLoopOutput * tuners.analogFanSpeedMultiplier).coerceAtMost(100.0).toInt()
        }
    }

    private fun runForDoorWindowSensor(
        config: MyStatConfiguration, equip: MyStatEquip,
        analogOutStages: HashMap<String, Int>, relayStages: HashMap<String, Int>
    ): Boolean {

        val isDoorOpen = isDoorOpenState(config, equip)
        CcuLog.d(L.TAG_CCU_MSPIPE2, " is Door Open ? $isDoorOpen")
        if (isDoorOpen) {
            resetLoops()
            resetLogicalPoints()
            analogOutStages.clear()
            relayStages.clear()
        }
        return isDoorOpen
    }

    private fun operateRelays(
        config: MyStatPipe2Configuration, tuner: MyStatTuners, userIntents: UserIntents, basicSettings: MyStatBasicSettings,
        relayStages: HashMap<String, Int>, relayOutputPoints: HashMap<Int, String>, equip: MyStatPipe2Equip, analogOutStages: HashMap<String, Int>
    ) {
        listOf(
            Triple(config.relay1Enabled.enabled, config.relay1Association.associationVal, Port.RELAY_ONE),
            Triple(config.relay2Enabled.enabled, config.relay2Association.associationVal, Port.RELAY_TWO),
            Triple(config.relay3Enabled.enabled, config.relay3Association.associationVal, Port.RELAY_THREE),
            Triple(config.relay4Enabled.enabled, config.relay4Association.associationVal, Port.RELAY_FOUR)
        ).forEach { (enabled, association, port) ->
            if (enabled) handleRelayState(association, config, port, tuner, userIntents, basicSettings, relayStages, relayOutputPoints, equip, analogOutStages)
        }
    }

    private fun handleRelayState(
        association: Int, config: MyStatPipe2Configuration, port: Port, tuner: MyStatTuners,
        userIntents: UserIntents, basicSettings: MyStatBasicSettings, relayStages: HashMap<String, Int>,
        relayOutputPoints: HashMap<Int, String>, equip: MyStatPipe2Equip, analogOutStages: HashMap<String, Int>
    ) {

        val relayMapping = MyStatPipe2RelayMapping.values().find { it.ordinal == association }
        when (relayMapping) {

            MyStatPipe2RelayMapping.AUX_HEATING_STAGE1 -> {
                operateAuxStageHeating(
                    relayMapping, relayOutputPoints, relayStages, tuner.auxHeating1Activate,
                    userIntents, config, analogOutStages, basicSettings
                )
            }

            MyStatPipe2RelayMapping.WATER_VALVE -> {
                if (equip.waterSamplingStartTime == 0L && basicSettings.conditioningMode != StandaloneConditioningMode.OFF) {
                    doRelayWaterValveOperation(
                        equip, port, basicSettings, waterValveLoop(userIntents),
                        tuner.relayActivationHysteresis, relayStages
                    )
                }
            }

            /**
             * intentionally we are sending analogOutStages for dcv to handle status message
             */
            MyStatPipe2RelayMapping.DCV_DAMPER -> doDcvDamperOperation(equip, port, tuner.relayActivationHysteresis, analogOutStages, config.co2Threshold.currentVal, false)
            MyStatPipe2RelayMapping.FAN_ENABLED -> doFanEnabled(curState, port, fanLoopOutput, relayStages)
            MyStatPipe2RelayMapping.OCCUPIED_ENABLED -> doOccupiedEnabled(port)
            MyStatPipe2RelayMapping.HUMIDIFIER -> doHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMinHumidity, equip.zoneHumidity.readHisVal())
            MyStatPipe2RelayMapping.DEHUMIDIFIER -> doDeHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMaxHumidity, equip.zoneHumidity.readHisVal())
            else -> {}
        }
    }

    private fun operateAuxStageHeating(
        auxMapping: MyStatPipe2RelayMapping,
        relayOutputPoints: HashMap<Int, String>,
        relayStages: HashMap<String, Int>,
        auxHeatingActivate: Double,
        userIntents: UserIntents,
        config: MyStatPipe2Configuration,
        analogOutStages: HashMap<String, Int>,
        basicSettings: MyStatBasicSettings
    ) {
        if (!isEligibleForHeating(basicSettings.conditioningMode)) {
            resetAux(relayStages)
            return
        }

        if (currentTemp < userIntents.heatingDesiredTemp - auxHeatingActivate) {
            updateLogicalPoint(relayOutputPoints[auxMapping.ordinal], 1.0)
            relayStages[auxMapping.name] = 1
            runSpecificAnalogFanSpeed(config, MyStatFanSpeed.HIGH, analogOutStages)

        } else if (currentTemp >= userIntents.heatingDesiredTemp - (auxHeatingActivate - 1)) {
            updateLogicalPoint(relayOutputPoints[auxMapping.ordinal], 0.0)
            runSpecificAnalogFanSpeed(config, MyStatFanSpeed.OFF, analogOutStages)
        } else {
            if (getCurrentLogicalPointStatus(relayOutputPoints[auxMapping.ordinal]!!) == 1.0) relayStages[auxMapping.name] = 1
        }
    }

    private fun isEligibleForHeating(conditioningMode: StandaloneConditioningMode): Boolean {
        return  conditioningMode == StandaloneConditioningMode.AUTO || conditioningMode == StandaloneConditioningMode.HEAT_ONLY
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
        config: MyStatPipe2Configuration, equip: MyStatPipe2Equip, basicSettings: MyStatBasicSettings, analogOutStages: HashMap<String, Int>, userIntents: UserIntents
    ) {
        if (config.analogOut1Enabled.enabled) {
            val analogMapping = MyStatPipe2AnalogOutMapping.values().find { it.ordinal == config.analogOut1Association.associationVal }
            when (analogMapping) {
                MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE -> {
                    if (equip.waterSamplingStartTime == 0L && basicSettings.conditioningMode != StandaloneConditioningMode.OFF) {
                        doAnalogWaterValveAction(
                            Port.ANALOG_OUT_ONE, basicSettings, waterValveLoop(userIntents),
                            analogOutStages
                        )
                    }
                }
                MyStatPipe2AnalogOutMapping.DCV_DAMPER_MODULATION -> {
                    doAnalogDCVAction(
                        Port.ANALOG_OUT_ONE, analogOutStages, config.co2Threshold.currentVal,
                        config.co2DamperOpeningRate.currentVal,
                        isDoorOpenState(config, equip), equip
                    )
                }
                else -> {}
            }
        }

    }

    private fun runAlgorithm(
        equip: MyStatPipe2Equip, basicSettings: MyStatBasicSettings, tuner: MyStatTuners, relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>, configuration: MyStatPipe2Configuration, userIntents: UserIntents
    ) {
        // Run water sampling
        processForWaterSampling(equip, tuner, configuration, relayStages, analogOutStages, basicSettings)

        // any specific user intent run the fan operations
        if(basicSettings.fanMode != MyStatFanStages.OFF && basicSettings.fanMode != MyStatFanStages.AUTO) {
            doFanOperation(tuner, basicSettings, relayStages, configuration, userIntents, analogOutStages, equip)
        }

        if ((currentTemp > 0) && (basicSettings.fanMode != MyStatFanStages.OFF)) {
            when (basicSettings.conditioningMode) {
                StandaloneConditioningMode.AUTO -> {
                    if (supplyWaterTempTh2 > heatingThreshold || supplyWaterTempTh2 in coolingThreshold..heatingThreshold)
                        doHeatOnly(tuner, basicSettings, relayStages, configuration,userIntents,analogOutStages,equip)
                    else if (supplyWaterTempTh2 < coolingThreshold)
                        doCoolOnly(tuner, basicSettings, relayStages, configuration,userIntents,analogOutStages,equip)
                }
                StandaloneConditioningMode.COOL_ONLY -> {
                    doCoolOnly(tuner, basicSettings, relayStages, configuration,userIntents,analogOutStages,equip)
                }
                StandaloneConditioningMode.HEAT_ONLY -> {
                    doHeatOnly(tuner, basicSettings, relayStages, configuration,userIntents,analogOutStages,equip)
                }
                StandaloneConditioningMode.OFF -> {
                    doFanOperation(tuner, basicSettings, relayStages, configuration,userIntents, analogOutStages, equip)
                }
            }
        } else {
            resetConditioning(relayStages,analogOutStages,basicSettings,equip)
        }
    }


    private fun doCoolOnly(
        tuner: MyStatTuners,
        basicSettings: MyStatBasicSettings,
        relayStages: HashMap<String, Int>,
        configuration: MyStatPipe2Configuration,
        userIntents: UserIntents,
        analogOutStages: HashMap<String, Int>,
        equip: MyStatPipe2Equip
    ) {
        CcuLog.d(L.TAG_CCU_MSPIPE2, "doCoolOnly: mode ")

        if (basicSettings.fanMode == MyStatFanStages.OFF || supplyWaterTempTh2 > heatingThreshold) {
            CcuLog.d(L.TAG_CCU_MSPIPE2, "Resetting WATER_VALVE to OFF")
            resetWaterValue(relayStages, analogOutStages, equip)
        }

        if (basicSettings.fanMode != MyStatFanStages.OFF) {
            if (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal)
                && (getCurrentLogicalPointStatus(relayLogicalPoints[MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal]!!) == 1.0
                && basicSettings.fanMode == MyStatFanStages.AUTO)
            ) {
                resetFan(relayStages, analogOutStages, basicSettings)
                operateAuxBasedOnFan(relayStages)
                runSpecificAnalogFanSpeed(configuration, MyStatFanSpeed.HIGH, analogOutStages)
            } else {
                // if we don't have aux configuration
                doFanOperation(
                    tuner,
                    basicSettings,
                    relayStages,
                    configuration,
                    userIntents,
                    analogOutStages,
                    equip
                )
            }
        } else {
            resetFan(relayStages, analogOutStages, basicSettings)
        }
    }

    private fun doHeatOnly(
        tuner: MyStatTuners,
        basicSettings: MyStatBasicSettings,
        relayStages: HashMap<String, Int>,
        configuration: MyStatPipe2Configuration,
        userIntents: UserIntents,
        analogOutStages: HashMap<String, Int>,
        equip: MyStatPipe2Equip
    ) {
        CcuLog.d(L.TAG_CCU_MSPIPE2, "doHeatOnly: mode ")

        // b. Deactivate Water Valve associated relay, if it is enabled and the fan speed is off.
        if (basicSettings.fanMode == MyStatFanStages.OFF || supplyWaterTempTh2 < coolingThreshold) {
            resetWaterValue(relayStages, analogOutStages, equip)
        }

        if (basicSettings.fanMode != MyStatFanStages.OFF) {
            if (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal)
                && (getCurrentLogicalPointStatus(relayLogicalPoints[MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal]!!) == 1.0
                        && basicSettings.fanMode == MyStatFanStages.AUTO)
            ) {
                resetFan(relayStages, analogOutStages, basicSettings)
                operateAuxBasedOnFan(relayStages)
                runSpecificAnalogFanSpeed(configuration, MyStatFanSpeed.HIGH, analogOutStages)
            } else {
                // If we have don't have any aux configuration
                doFanOperation(
                    tuner,
                    basicSettings,
                    relayStages,
                    configuration,
                    userIntents,
                    analogOutStages,
                    equip
                )
            }
        } else {
            resetFan(relayStages, analogOutStages, basicSettings)
        }
    }

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
        analogOutStages: HashMap<String, Int>, basicSettings: MyStatBasicSettings
    ) {

        if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
            resetWaterValue(relayStages, analogOutStages , equip)
            return
        }

        if (!config.isAnyRelayEnabledAssociated(association = MyStatPipe2RelayMapping.WATER_VALVE.ordinal) &&
            !config.isAnalogEnabledAssociated(MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)) {
            CcuLog.d(L.TAG_CCU_MSPIPE2, "No mapping for water value")
            return
        }

        fun resetIsRequired(): Boolean {
            return ((relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.WATER_VALVE.ordinal) &&
                    (getCurrentLogicalPointStatus(relayLogicalPoints[MyStatPipe2RelayMapping.WATER_VALVE.ordinal]!!).toInt() != 0))
                    || (analogLogicalPoints.containsKey(MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)
                    && (getCurrentLogicalPointStatus(analogLogicalPoints[MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal]!!).toInt() != 0)))
        }

        CcuLog.d(L.TAG_CCU_MSPIPE2, "waterSamplingStarted Time "+ equip.waterSamplingStartTime)

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
                resetWaterValue(relayStages, analogOutStages, equip)
            }
            CcuLog.d(L.TAG_CCU_MSPIPE2, "No water sampling, because tuner value is zero!")
            return
        }
        CcuLog.d(L.TAG_CCU_MSPIPE2, "waitTimeToDoSampling:  $waitTimeToDoSampling onTimeToDoSampling: $onTimeToDoSampling")
        CcuLog.d(L.TAG_CCU_MSPIPE2, "Current : ${System.currentTimeMillis()}: Last On: ${equip.lastWaterValveTurnedOnTime}")

        if (equip.waterSamplingStartTime == 0L) {
            val minutes = milliToMin(System.currentTimeMillis() - equip.lastWaterValveTurnedOnTime)
            CcuLog.d(L.TAG_CCU_MSPIPE2, "sampling will start in : ${waitTimeToDoSampling - minutes} current : $minutes")
            if (minutes >= waitTimeToDoSampling) {
                doWaterSampling(equip, relayStages)
            }
        } else {
            val samplingSinceFrom = milliToMin(System.currentTimeMillis() - equip.waterSamplingStartTime)
            CcuLog.d(L.TAG_CCU_MSPIPE2, "Water sampling is running since from $samplingSinceFrom minutes")
            if (samplingSinceFrom >= onTimeToDoSampling) {
                equip.waterSamplingStartTime = 0
                equip.lastWaterValveTurnedOnTime = System.currentTimeMillis()
                resetWaterValue(relayStages, analogOutStages, equip)
                CcuLog.d(L.TAG_CCU_MSPIPE2, "Resetting WATER_VALVE to OFF")
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
        updateLogicalPoint(relayLogicalPoints[MyStatPipe2RelayMapping.WATER_VALVE.ordinal],1.0)
        updateLogicalPoint(analogLogicalPoints[MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal],100.0)
        relayStages[StatusMsgKeys.WATER_VALVE.name] = 1
        CcuLog.d(L.TAG_CCU_MSPIPE2, "Turned ON water valve ")
    }

    private fun resetFan(
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        basicSettings: MyStatBasicSettings
    ) {
        if (basicSettings.fanMode == MyStatFanStages.AUTO || basicSettings.fanMode == MyStatFanStages.OFF) {
            if (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.FAN_LOW_SPEED.ordinal)) {
                resetLogicalPoint(relayLogicalPoints[MyStatPipe2RelayMapping.FAN_LOW_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_1.displayName)
            }
            if (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)) {
                resetLogicalPoint(relayLogicalPoints[MyStatPipe2RelayMapping.FAN_HIGH_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_2.displayName)
            }
            if (analogLogicalPoints.containsKey(MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal)) {
                resetLogicalPoint(analogLogicalPoints[MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal]!!)
                analogOutStages.remove(StatusMsgKeys.FAN_SPEED.name)
            }
        }
    }

    private fun resetWaterValue(relayStages: HashMap<String, Int>, analogOutStages: HashMap<String, Int>, equip: MyStatPipe2Equip) {
        if (equip.waterSamplingStartTime == 0L) {
            if (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.WATER_VALVE.ordinal)) {
                resetLogicalPoint(relayLogicalPoints[MyStatPipe2RelayMapping.WATER_VALVE.ordinal]!!)
            }
            if (analogLogicalPoints.containsKey(MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)) {
                resetLogicalPoint(analogLogicalPoints[MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal]!!)
            }
            relayStages.remove(StatusMsgKeys.WATER_VALVE.name)
            analogOutStages.remove(StatusMsgKeys.WATER_VALVE.name)
            CcuLog.d(L.TAG_CCU_MSPIPE2, "Resetting WATER_VALVE to OFF")
        }
    }
    private fun resetAux(relayStages: HashMap<String, Int>) {
        CcuLog.d(L.TAG_CCU_MSPIPE2, "Resetting Aux")
        Thread.dumpStack()
        if (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal)) {
            resetLogicalPoint(relayLogicalPoints[MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal]!!)
        }
        relayStages.remove(MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.name)
    }





    private fun doFanOperation(
        tuner: MyStatTuners, basicSettings: MyStatBasicSettings, relayStages: HashMap<String, Int>,
        configuration: MyStatPipe2Configuration, userIntents: UserIntents, analogOutStages: HashMap<String, Int>, equip: MyStatPipe2Equip
    ) {
        val lowestStage = configuration.getLowestFanSelected()

        if (lowestStage == null) resetFan(relayStages, analogOutStages, basicSettings)

        // Check which fan speed is the lowest and set the status(Eg: If FAN_MEDIUM and FAN_HIGH are used, then FAN_MEDIUM is the lowest)
        resetFanStatus()

        when (lowestStage) {
            MyStatPipe2RelayMapping.FAN_LOW_SPEED -> setFanLowestFanLowStatus(true)
            MyStatPipe2RelayMapping.FAN_HIGH_SPEED -> setFanLowestFanHighStatus(true)
            else -> {
                // Do nothing
            }
        }

        if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
            if (basicSettings.fanMode != MyStatFanStages.AUTO) {
                runFanHigh(tuner, basicSettings, relayStages, fanLoopOutput)
                runFanLow(tuner, basicSettings, relayStages, fanLoopOutput)
                runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
            } else {
                resetFan(relayStages, analogOutStages, basicSettings)
            }
        } else {

            if (basicSettings.fanMode == MyStatFanStages.AUTO
                && supplyWaterTempTh2 > heatingThreshold
                && supplyWaterTempTh2 in coolingThreshold..heatingThreshold
                && currentTemp > userIntents.coolingDesiredTemp) {
                resetFan(relayStages, analogOutStages, basicSettings)
            } else {
                if (basicSettings.fanMode != MyStatFanStages.AUTO && basicSettings.fanMode != MyStatFanStages.OFF) {
                    runFanHigh(tuner, basicSettings, relayStages, fanLoopOutput)
                    runFanLow(tuner, basicSettings, relayStages, fanLoopOutput)
                    runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
                    return
                }
                if (equip.waterSamplingStartTime == 0L && (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.WATER_VALVE.ordinal) &&
                            getCurrentLogicalPointStatus(relayLogicalPoints[MyStatPipe2RelayMapping.WATER_VALVE.ordinal]!!) == 1.0)) {

                    runFanHigh(tuner, basicSettings, relayStages, fanLoopOutput)
                    runFanLow(tuner, basicSettings, relayStages, fanLoopOutput)
                    runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
                } else if (equip.waterSamplingStartTime == 0L && analogLogicalPoints.containsKey(MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal) &&
                    getCurrentLogicalPointStatus(analogLogicalPoints[MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal]!!) > 1.0) {
                    runFanHigh(tuner, basicSettings, relayStages, fanLoopOutput)
                    runFanLow(tuner, basicSettings, relayStages, fanLoopOutput)
                    runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
                } else if (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal)
                    && getCurrentLogicalPointStatus(relayLogicalPoints[MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal]!!) == 1.0) {
                    runFanHigh(tuner, basicSettings, relayStages, fanLoopOutput)
                    runFanLow(tuner, basicSettings, relayStages, fanLoopOutput)
                    runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
                } else {
                    resetFan(relayStages, analogOutStages, basicSettings)
                }
            }
        }
    }

    // Do fan low speed
    private fun runFanLow(
        tuner: MyStatTuners, basicSettings: MyStatBasicSettings,
        relayStages: HashMap<String, Int>, fanLoop: Int
    ) {
        if (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.FAN_LOW_SPEED.ordinal)) {
            doFanLowSpeed(
                relayLogicalPoints[MyStatPipe2RelayMapping.FAN_LOW_SPEED.ordinal]!!,
                getHighFanSpeedIfExist(),
                basicSettings.fanMode, fanLoop, tuner.relayActivationHysteresis,
                relayStages,  runFanLowDuringDoorWindow
            )
        }
    }

    private fun runFanHigh(
        tuner: MyStatTuners,
        basicSettings: MyStatBasicSettings,
        relayStages: HashMap<String, Int>,
        fanLoop: Int,
    ) {
        if (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)) {
            doFanHighSpeed(
                relayLogicalPoints[MyStatPipe2RelayMapping.FAN_HIGH_SPEED.ordinal]!!,
                basicSettings.fanMode, fanLoop, tuner.relayActivationHysteresis,
                relayStages, runFanLowDuringDoorWindow
            )
        }
    }


    private fun runAnalogFanSpeed(
        config: MyStatPipe2Configuration,
        userIntents: UserIntents,
        analogOutStages: HashMap<String, Int>,
        basicSettings: MyStatBasicSettings
    ) {
        var output = fanLoopOutput
        if (supplyWaterTempTh2 > heatingThreshold && currentTemp > userIntents.coolingDesiredTemp) {
            output = 0
        }

        if (config.analogOut1Enabled.enabled && config.analogOut1Association.associationVal == MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal) {
            doAnalogFanAction(
                Port.ANALOG_OUT_ONE, config.analogOut1FanSpeedConfig.low.currentVal.toInt(),
                config.analogOut1FanSpeedConfig.high.currentVal.toInt(),
                basicSettings.fanMode, basicSettings.conditioningMode,
                output, analogOutStages
            )

        }
    }


    // New requirement for aux and fan operations If we do not have fan then no aux
    private fun operateAuxBasedOnFan(relayStages: HashMap<String, Int>) {
        var stage = MyStatPipe2RelayMapping.AUX_HEATING_STAGE1
        var state = 0
        var fanStatusMessage: Stage? = null
        if (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)) {
            stage = MyStatPipe2RelayMapping.FAN_HIGH_SPEED
            state = 1
            fanStatusMessage = Stage.FAN_2
        } else if (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.FAN_LOW_SPEED.ordinal)) {
            stage = MyStatPipe2RelayMapping.FAN_LOW_SPEED
            state = 1
            fanStatusMessage = Stage.FAN_1
        } else if (analogLogicalPoints.containsKey(MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal)
            || relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.FAN_ENABLED.ordinal)) {
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
        tuner: MyStatTuners,
        basicSettings: MyStatBasicSettings,
        relayStages: HashMap<String, Int>,
        userIntents: UserIntents,
        analogOutStages: HashMap<String, Int>,
        configuration: MyStatPipe2Configuration
    ) {
        if (basicSettings.fanMode == MyStatFanStages.AUTO && runFanLowDuringDoorWindow) {
            val lowestStage = configuration.getLowestFanSelected()

            if (lowestStage == null) resetFan(relayStages, analogOutStages, basicSettings)

            resetFanStatus()
            when (lowestStage) {
                MyStatPipe2RelayMapping.FAN_LOW_SPEED -> setFanLowestFanLowStatus(true)
                MyStatPipe2RelayMapping.FAN_HIGH_SPEED -> setFanLowestFanHighStatus(true)
                else -> {
                    // Do nothing
                }
            }

            // When door window open, run the lowest fan speed. Anyhow the fanloop output is 0, this will be
            // handled in the following APIs
            runFanHigh(tuner, basicSettings, relayStages, fanLoopOutput)
            runFanLow(tuner, basicSettings, relayStages, fanLoopOutput)
            runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
        }
    }


    private fun showOutputStatus() {
        logicalPointsList.toSortedMap().forEach { (port, logicalPointId) ->
            var mapping: String? = null
            when (port) {
                Port.RELAY_ONE, Port.RELAY_TWO, Port.RELAY_THREE, Port.RELAY_FOUR, Port.RELAY_FIVE, Port.RELAY_SIX -> {
                    val enumPos = relayLogicalPoints.entries.find { it.value.contentEquals(logicalPointId) }?.key
                    mapping = MyStatPipe2RelayMapping.values().find { it.ordinal == enumPos }?.name
                }

                Port.ANALOG_OUT_ONE -> {
                    val enumPos = analogLogicalPoints.entries.find { it.value.contentEquals(logicalPointId) }?.key
                    mapping = MyStatPipe2AnalogOutMapping.values().find { it.ordinal == enumPos }?.name
                }
                else -> {}
            }

            CcuLog.i(L.TAG_CCU_MSPIPE2, "$port = $mapping : ${getCurrentLogicalPointStatus(logicalPointsList[port]!!)}")
        }
    }

    private fun resetConditioning(
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        basicSettings: MyStatBasicSettings,
        equip: MyStatPipe2Equip
    ) {
        // Reset Relay
        resetFan(relayStages, analogOutStages, basicSettings)
        resetWaterValue(relayStages, analogOutStages, equip)
        resetAux(relayStages)

        if (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.FAN_ENABLED.ordinal)) resetLogicalPoint(
            relayLogicalPoints[MyStatPipe2RelayMapping.FAN_ENABLED.ordinal]!!
        )
        if (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.OCCUPIED_ENABLED.ordinal)) resetLogicalPoint(
            relayLogicalPoints[MyStatPipe2RelayMapping.OCCUPIED_ENABLED.ordinal]!!
        )
        if (relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.DCV_DAMPER.ordinal)) resetLogicalPoint(
            relayLogicalPoints[MyStatPipe2RelayMapping.DCV_DAMPER.ordinal]!!
        )

        if (analogLogicalPoints.containsKey(MyStatPipe2AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal)) resetLogicalPoint(
            analogLogicalPoints[MyStatPipe2AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal]!!
        )
        analogOutStages.remove(StatusMsgKeys.DCV_DAMPER.name)
    }

    private fun getHighFanSpeedIfExist(): String? {
        if(relayLogicalPoints.containsKey(MyStatPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)){
            return relayLogicalPoints[MyStatPipe2RelayMapping.FAN_HIGH_SPEED.ordinal]
        }
        return null
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
       // TODO No door window as of now update if we configure it
        return false
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