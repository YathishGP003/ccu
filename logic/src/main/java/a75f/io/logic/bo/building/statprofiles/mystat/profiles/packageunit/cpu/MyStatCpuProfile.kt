package a75f.io.logic.bo.building.statprofiles.mystat.profiles.packageunit.cpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage

import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutConfigs
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.profiles.packageunit.MyStatPackageUnitProfile
import a75f.io.logic.bo.building.statprofiles.util.MyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.MyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.StatLoopController
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.fetchMyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.fetchMyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatCpuAnalogOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.getMyStatCpuRelayOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.getMyStatLogicalPointList
import a75f.io.logic.bo.building.statprofiles.util.getPercentFromVolt
import a75f.io.logic.bo.building.statprofiles.util.updateLogicalPoint
import a75f.io.logic.bo.building.statprofiles.util.updateLoopOutputs
import a75f.io.logic.bo.building.statprofiles.util.updateOperatingMode
import a75f.io.logic.util.uiutils.MyStatUserIntentHandler
import kotlin.math.roundToInt

/**
 * Created by Manjunath K on 16-01-2025.
 */

class MyStatCpuProfile: MyStatPackageUnitProfile() {
    private val cpuDeviceMap: MutableMap<Int, MyStatCpuEquip> = mutableMapOf()
    private var analogLogicalPoints: HashMap<Int, String> = HashMap()
    private var relayLogicalPoints: HashMap<Int, String> = HashMap()
    private var hasZeroFanLoopBeenHandled = false


    private val myStatLoopController = StatLoopController()
    private lateinit var curState: ZoneState
    override lateinit var occupancyStatus: Occupancy

    // Flags for keeping tab of occupancy during linear fan operation(Only to be used in doAnalogFanActionCpu())
    private var previousOccupancyStatus: Occupancy = Occupancy.NONE
    private var previousFanStageStatus: MyStatFanStages = MyStatFanStages.OFF
    private val defaultFanLoopOutput = 0.0
    private var previousFanLoopVal = 0
    private var previousFanLoopValStaged = 0
    private var fanLoopCounter = 0

    override fun updateZonePoints() {
        cpuDeviceMap.forEach { (nodeAddress, equip) ->
            cpuDeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as MyStatCpuEquip
            CcuLog.d(L.TAG_CCU_MSCPU, "Process CPU: equipRef =  ${equip.nodeAddress}")
            processCpuProfile(equip)
        }
    }

    fun processCpuProfile(equip: MyStatCpuEquip) {
        if (Globals.getInstance().isTestMode) {
            CcuLog.d(L.TAG_CCU_MSCPU, "Test mode is on: ${equip.equipRef}")
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

        val config = getMyStatConfiguration(equip.equipRef) as MyStatCpuConfiguration
        logicalPointsList = getMyStatLogicalPointList(equip, config)

        relayLogicalPoints = getMyStatCpuRelayOutputPoints(equip)
        analogLogicalPoints = getMyStatCpuAnalogOutputPoints(equip)

        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode

        val myStatTuners = fetchMyStatTuners(equip) as MyStatTuners
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val fanModeSaved = FanModeCacheStorage.getMyStatFanModeCache().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchMyStatBasicSettings(equip)

        CcuLog.d(
            L.TAG_CCU_MSCPU,
            "Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}"
        )
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        CcuLog.d(
            L.TAG_CCU_MSCPU,
            "After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}"
        )
        myStatLoopController.initialise(tuners = myStatTuners)
        myStatLoopController.dumpLogs()
        handleChangeOfDirection(userIntents, myStatLoopController)
        updateOperatingMode(currentTemp, averageDesiredTemp, basicSettings.conditioningMode, equip.operatingMode)

        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0

        val currentOperatingMode = equip.occupancyMode.readHisVal().toInt()
        evaluateLoopOutputs(userIntents, myStatLoopController)
        evaluateLoopOutputs(userIntents, myStatLoopController)
        deriveFanLoopOutput(basicSettings, myStatTuners)
        calculateDcvLoop(equip, config.co2Threshold.currentVal, config.co2DamperOpeningRate.currentVal)
        updateOccupancyDetection(equip)

        doorWindowSensorOpenStatus =
            runForDoorWindowSensor(config, equip, analogOutputStatus, relayOutputStatus)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)
        if (occupancyStatus == Occupancy.WINDOW_OPEN) resetLoops()
        runForKeyCardSensor(config, equip)

        updateLoopOutputs(
            coolingLoopOutput, equip.coolingLoopOutput,
            heatingLoopOutput, equip.heatingLoopOutput,
            fanLoopOutput, equip.fanLoopOutput,
            dcvLoopOutput, equip.dcvDamper,
        )

        updateTitle24LoopCounter(myStatTuners, basicSettings)

        CcuLog.i(
            L.TAG_CCU_MSCPU,
            "Fan speed multiplier:  ${myStatTuners.analogFanSpeedMultiplier} " + "Current Occupancy: ${Occupancy.values()[currentOperatingMode]} \n"
                    + "Fan Mode : ${basicSettings.fanMode} Conditioning Mode ${basicSettings.conditioningMode} \n"
                    + "Current Temp : $currentTemp Desired (Heating: ${userIntents.heatingDesiredTemp}" + " Cooling: ${userIntents.coolingDesiredTemp})\n"
                    + "Loop Outputs: (Heating Loop: $heatingLoopOutput Cooling Loop: $coolingLoopOutput Fan Loop: $fanLoopOutput  dcvLoopOutput : $dcvLoopOutput) \n "
        )
        if (basicSettings.fanMode != MyStatFanStages.OFF) {
            operateRelays(
                config, myStatTuners, userIntents, basicSettings,
                relayOutputStatus, analogOutputStatus, equip
            )
            operateAnalogOutputs(config, basicSettings, analogOutputStatus, equip)
        } else {
            resetLogicalPoints()
        }
        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())
        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY
        MyStatUserIntentHandler.updateMyStatStatus(equip.equipRef, relayOutputStatus, analogOutputStatus, temperatureState, equip)
        updateTitle24Flags(basicSettings)
        CcuLog.i(L.TAG_CCU_MSCPU, "----------------------------------------------------------")
    }

    /**
     * Updates the Title 24 loop counter based on changes in occupancy and fan-loop output.
     * If there is a change in occupancy and the fan-loop output is less than the previous value,
     * the fan loop counter is set to the minimum fan runtime post conditioning specified by the tuners.
     * If there is a change in occupancy and the fan-loop output is greater than the previous value,
     * the fan loop counter is reset to zero.
     *
     * @param tuners The HyperStat profile tuners containing configuration parameters.
     * @param basicSettings settings containing fan mode state.
     */
    private fun updateTitle24LoopCounter(tuners: MyStatTuners, basicSettings: MyStatBasicSettings) {
        // Check if there is change in occupancy and the fan-loop output is less than the previous value,
        // then offer the fan

        CcuLog.d( L.TAG_CCU_MSCPU, "Occupancy: $occupancyStatus, Fan Loop Output: $fanLoopOutput, Previous Fan Loop Val: $previousFanLoopVal, Fan Loop Counter: $fanLoopCounter , hasZeroFanLoopBeenHandled $hasZeroFanLoopBeenHandled")
        if ((occupancyStatus != previousOccupancyStatus && fanLoopOutput < previousFanLoopVal) ||
            (basicSettings.fanMode != previousFanStageStatus && fanLoopOutput < previousFanLoopVal) ||
            (fanLoopOutput == 0 && fanLoopOutput < previousFanLoopVal && !hasZeroFanLoopBeenHandled)) {
            fanLoopCounter = tuners.minFanRuntimePostConditioning
            hasZeroFanLoopBeenHandled = true
        } else if ((occupancyStatus != previousOccupancyStatus || (hasZeroFanLoopBeenHandled && fanLoopOutput > 0)) && fanLoopOutput > previousFanLoopVal) {
            // If the fan loop output is greater than the previous value and the counter is greater than 0, reset the counter
            fanLoopCounter = 0 // Reset the counter if the fan-loop output is greater than the previous value
            hasZeroFanLoopBeenHandled = false
        }

    }

    /**
     * Updates the Title 24 flags by storing the current occupancy status and fan-loop output.
     * The previous occupancy status and fan-loop output are updated for use in the next loop iteration.
     * If the fan loop counter is greater than 0, it decrements the counter.
     * @param basicSettings settings containing fan mode state.
     */
    private fun updateTitle24Flags(basicSettings: MyStatBasicSettings) {
        // Store the fan-loop output/occupancy for usage in next loop
        previousOccupancyStatus = occupancyStatus
        if (occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        // Store the fan status so that when the zone goes from user defined fan state to unoccupied state, the fan protection can be offered
        previousFanStageStatus = basicSettings.fanMode
        // Store the fanloop output when the zone was not in UNOCCUPIED state
        if (fanLoopCounter == 0) previousFanLoopVal = fanLoopOutput
        if (fanLoopCounter > 0) fanLoopCounter--
    }

    private fun operateAnalogOutputs(
        config: MyStatCpuConfiguration,
        basicSettings: MyStatBasicSettings,
        analogOutStages: HashMap<String, Int>,
        equip: MyStatCpuEquip
    ) {
        config.apply {

            if (analogOut1Enabled.enabled) {
                handleAnalogOutState(
                    MyStatCpuAnalogOutConfigs(
                        true,
                        analogOut1Association.associationVal,
                        analogOut1MinMaxConfig,
                        analogOut1FanSpeedConfig,
                        recirculateFanConfig.currentVal
                    ), config, basicSettings, analogOutStages, equip
                )
            }
        }
    }
    private fun  handleAnalogOutState(
        analogOutState: MyStatCpuAnalogOutConfigs,
        config: MyStatCpuConfiguration,
        basicSettings: MyStatBasicSettings,
        analogOutStages: HashMap<String, Int>,
        equip: MyStatCpuEquip
    ) {
        val analogMapping =
            MyStatCpuAnalogOutMapping.values().find { it.ordinal == analogOutState.association }
        when (analogMapping) {
            MyStatCpuAnalogOutMapping.COOLING -> {
                doAnalogCooling(
                    Port.ANALOG_OUT_ONE, basicSettings.conditioningMode, analogOutStages, coolingLoopOutput
                )
            }
            MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED -> {
                doAnalogFanActionCpu(
                    analogOutState.fanSpeed.low.currentVal.toInt(),
                    analogOutState.fanSpeed.high.currentVal.toInt(),
                    basicSettings.fanMode,
                    basicSettings.conditioningMode,
                    fanLoopOutput,
                    analogOutStages,
                    previousFanLoopVal,
                    fanLoopCounter
                )
            }
            MyStatCpuAnalogOutMapping.HEATING -> {
                doAnalogHeating(
                    Port.ANALOG_OUT_ONE, basicSettings.conditioningMode, analogOutStages, heatingLoopOutput
                )
            }
            MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED -> {
                doAnalogStagedFanAction(
                    analogOutState.fanSpeed.low.currentVal.toInt(),
                    analogOutState.fanSpeed.high.currentVal.toInt(),
                    basicSettings.fanMode,
                    config.isAnyRelayEnabledAssociated(association = MyStatCpuRelayMapping.FAN_ENABLED.ordinal),
                    basicSettings.conditioningMode,
                    fanLoopOutput,
                    analogOutStages,
                    fanLoopCounter,
                    equip
                )
            }
            MyStatCpuAnalogOutMapping.DCV_DAMPER -> {
                doAnalogDCVAction(
                    Port.ANALOG_OUT_ONE, analogOutStages, config.co2Threshold.currentVal,
                    config.co2DamperOpeningRate.currentVal,
                    isDoorOpenState(config, equip), equip
                )
            }
            else -> {}
        }
        if (logicalPointsList.containsKey(Port.ANALOG_OUT_ONE)) {
            CcuLog.i(
                L.TAG_CCU_MSCPU,
                "${Port.ANALOG_OUT_ONE} = $analogMapping : ${
                    getCurrentLogicalPointStatus(logicalPointsList[Port.ANALOG_OUT_ONE]!!)
                }"
            )
        }
    }

    private fun doAnalogStagedFanAction(
        fanLowPercent: Int, fanHighPercent: Int, fanMode: MyStatFanStages,
        fanEnabledMapped: Boolean, conditioningMode: StandaloneConditioningMode,
        fanLoopOutput: Int, analogOutStages: HashMap<String, Int>,
        fanProtectionCounter: Int, equip: MyStatCpuEquip
    ) {
        if (fanMode == MyStatFanStages.OFF) return

        var fanLoopForAnalog: Int
        var logMsg = "" // This will be overwritten based on priority

        if (fanMode == MyStatFanStages.AUTO) {

            if (conditioningMode == StandaloneConditioningMode.OFF) {
                updateLogicalPoint(logicalPointsList[Port.ANALOG_OUT_ONE]!!, 0.0)
                return
            }

            fanLoopForAnalog = fanLoopOutput
            val coolingVoltage = getCoolingActivatedAnalogVoltage(fanEnabledMapped, fanLoopOutput, equip)
            val heatingVoltage = getHeatingActivatedAnalogVoltage(fanEnabledMapped, fanLoopOutput, equip)

            if (conditioningMode == StandaloneConditioningMode.AUTO) {

                val operatingMode = equip.operatingMode.readHisVal().toInt()
                if (operatingMode == ZoneState.COOLING.ordinal) {
                    fanLoopForAnalog = coolingVoltage
                    logMsg = "Cooling"
                } else if (operatingMode == ZoneState.HEATING.ordinal) {
                    fanLoopForAnalog = heatingVoltage
                    logMsg = "Heating"
                }
            } else if (conditioningMode == StandaloneConditioningMode.COOL_ONLY) {
                fanLoopForAnalog = coolingVoltage
                logMsg = "Cooling"
            } else if (conditioningMode == StandaloneConditioningMode.HEAT_ONLY) {
                fanLoopForAnalog = heatingVoltage
                logMsg = "Heating"
            }
            // Check if we need fan protection
            if (fanProtectionCounter > 0 && fanLoopForAnalog < previousFanLoopValStaged) {
                fanLoopForAnalog = previousFanLoopValStaged
                logMsg = "Fan Protection"
            } else {
                // else indicates we are not in protection mode, so store the fanLoopForAnalog value for protection mdoe
                previousFanLoopValStaged = fanLoopForAnalog
            }
            // When in dead-band, set the fan-loopForAnalog to the recirculate analog value. Also ensure fan protection is not ON
            // added the new check if the fan loop output is with in relayActivationHysteresis ,sending the Analog recirculate value
            val relayActivationHysteresis = equip.standaloneRelayActivationHysteresis.readPriorityVal()
            if ((fanLoopOutput == 0 || runFanLowDuringDoorWindow || (fanLoopOutput > 0 && fanLoopOutput < relayActivationHysteresis)) && fanProtectionCounter == 0 && isInSoftOccupiedMode()) {
                fanLoopForAnalog = getPercentFromVolt(getAnalogRecirculateValueActivated(equip).roundToInt())
                logMsg = "Deadband"
            }
        } else {
            fanLoopForAnalog = when (fanMode) {
                MyStatFanStages.LOW_CUR_OCC, MyStatFanStages.LOW_OCC, MyStatFanStages.LOW_ALL_TIME -> fanLowPercent
                MyStatFanStages.HIGH_CUR_OCC, MyStatFanStages.HIGH_OCC, MyStatFanStages.HIGH_ALL_TIME -> fanHighPercent
                else -> 0
            }
            // Store just in case when we switch from current occupied to unoccupied mode
            previousFanLoopValStaged = fanLoopForAnalog
        }

        if (fanLoopForAnalog > 0) analogOutStages[StatusMsgKeys.FAN_SPEED.name] = fanLoopForAnalog
        updateLogicalPoint(logicalPointsList[Port.ANALOG_OUT_ONE]!!, fanLoopForAnalog.toDouble())
        CcuLog.i(L.TAG_CCU_MSHST, "${Port.ANALOG_OUT_ONE} = Staged Fan Speed($logMsg) $fanLoopForAnalog")
    }

    private fun getHeatingActivatedAnalogVoltage(fanEnabledMapped: Boolean, fanLoopOutput: Int, equip: MyStatCpuEquip): Int {
        // For title 24 compliance, check if fanEnabled is mapped, then set the fanloopForAnalog to the lowest heating state activated
        // and check if staged fan is inactive(fanLoopForAnalog == 0)

        var voltage = getPercentFromVolt(getHeatingStateActivated(equip).roundToInt())
        if (fanEnabledMapped && voltage == 0 && fanLoopOutput > 0) {
            voltage = getPercentFromVolt(getLowestHeatingStateActivated(equip).roundToInt())
        }
        return voltage
    }

    /**
     * Returns the value of the lowest activated heating stage based on the fan loop points.
     * If no heating stage is activated, returns the default fan loop output value.
     *
     * @return the value of the lowest activated heating stage or the default fan loop output value.
     */
    private fun getLowestHeatingStateActivated(equip: MyStatCpuEquip): Double {
        return listOf(
            equip.fanOutHeatingStage1.readDefaultVal(),
            equip.fanOutHeatingStage2.readDefaultVal()
        ).firstOrNull { it > 0 } ?: defaultFanLoopOutput
    }


    /**
     * Returns the value of the lowest activated cooling stage based on the fan loop points.
     * If no cooling stage is activated, returns the default fan loop output value.
     *
     * @return the value of the lowest activated cooling stage or the default fan loop output value.
     */
    private fun getLowestCoolingStateActivated(equip: MyStatCpuEquip): Double {
        return listOf(
            equip.fanOutCoolingStage1.readDefaultVal(),
            equip.fanOutCoolingStage2.readDefaultVal()
        ).firstOrNull { it > 0 } ?: defaultFanLoopOutput
    }

    private fun getCoolingActivatedAnalogVoltage(fanEnabledMapped: Boolean, fanLoopOutput: Int, equip: MyStatCpuEquip): Int {
        // For title 24 compliance, check if fanEnabled is mapped, then set the fan loop ForAnalog to the lowest cooling state activated
        // and check if staged fan is inactive(fanLoopForAnalog == 0)

        var voltage = getPercentFromVolt(getCoolingStateActivated(equip).roundToInt())
        if (fanEnabledMapped && voltage == 0 && fanLoopOutput > 0) {
            voltage = getPercentFromVolt(getLowestCoolingStateActivated(equip).roundToInt())
        }
        return voltage
    }

    private fun getCoolingStateActivated(equip: MyStatCpuEquip): Double {
        return when {
            equip.coolingStage2.readHisVal() > 0 -> equip.fanOutCoolingStage2.readPriorityVal()
            equip.coolingStage1.readHisVal() > 0 -> equip.fanOutCoolingStage1.readPriorityVal()
            else -> defaultFanLoopOutput
        }
    }

    private fun getHeatingStateActivated(equip: MyStatCpuEquip): Double {
        return when {
            equip.heatingStage2.readHisVal() > 0 -> equip.fanOutHeatingStage2.readPriorityVal()
            equip.heatingStage1.readHisVal() > 0 -> equip.fanOutHeatingStage1.readPriorityVal()
            else -> defaultFanLoopOutput
        }
    }

    /**
     * Returns the value of the non zero activated analog recirculate stage based on the fan loop points.
     * If no recirculate stage is activated, returns the default fan loop output value.
     * @return the value of the first non zero activated recirculate stage or the default fan loop output value.
     */
    private fun getAnalogRecirculateValueActivated(equip: MyStatCpuEquip): Double {
        return when {
            equip.analog1FanRecirculate.readDefaultVal() > 0 -> equip.analog1FanRecirculate.readDefaultVal()
            equip.analog2FanRecirculate.readDefaultVal() > 0 -> equip.analog2FanRecirculate.readDefaultVal()
            else -> defaultFanLoopOutput
        }
    }

    /**
     * Check if currently in any of the occupied mode
     * @return true if in occupied,forced occupied etc, else false.
     */
    private fun isInSoftOccupiedMode(): Boolean {
        return occupancyStatus != Occupancy.UNOCCUPIED &&
                occupancyStatus != Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
                occupancyStatus != Occupancy.VACATION
    }

    private fun doAnalogFanActionCpu(
        fanLowPercent: Int,
        fanHighPercent: Int,
        fanMode: MyStatFanStages,
        conditioningMode: StandaloneConditioningMode,
        fanLoopOutput: Int,
        analogOutStages: HashMap<String, Int>,
        previousFanLoopVal: Int,
        fanProtectionCounter: Int
    ) {
        if (fanMode != MyStatFanStages.OFF) {
            var fanLoopForAnalog = 0
            if (fanMode == MyStatFanStages.AUTO) {
                if (conditioningMode == StandaloneConditioningMode.OFF) {
                    updateLogicalPoint(logicalPointsList[Port.ANALOG_OUT_ONE]!!, 0.0)
                    return
                }
                fanLoopForAnalog = fanLoopOutput
                if (fanProtectionCounter > 0) {
                    fanLoopForAnalog = previousFanLoopVal
                }
            } else {
                when {
                    (fanMode == MyStatFanStages.LOW_CUR_OCC
                            || fanMode == MyStatFanStages.LOW_OCC
                            || fanMode == MyStatFanStages.LOW_ALL_TIME) -> {
                        fanLoopForAnalog = fanLowPercent
                    }

                    (fanMode == MyStatFanStages.HIGH_CUR_OCC
                            || fanMode == MyStatFanStages.HIGH_OCC
                            || fanMode == MyStatFanStages.HIGH_ALL_TIME) -> {
                        fanLoopForAnalog = fanHighPercent
                    }
                }
            }
            if (fanLoopForAnalog > 0) analogOutStages[StatusMsgKeys.FAN_SPEED.name] = fanLoopForAnalog
            updateLogicalPoint(logicalPointsList[Port.ANALOG_OUT_ONE]!!, fanLoopForAnalog.toDouble())
        }
    }


    private fun operateRelays(
        config: MyStatCpuConfiguration,
        tuner: MyStatTuners,
        userIntents: UserIntents,
        basicSettings: MyStatBasicSettings,
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        equip: MyStatCpuEquip
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
                        basicSettings, relayStages, analogOutStages, equip
                    )
                }
            }
        }
    }

    private fun handleRelayState(
        association: Int,
        config: MyStatCpuConfiguration,
        port: Port,
        tuner: MyStatTuners,
        userIntents: UserIntents,
        basicSettings: MyStatBasicSettings,
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        equip: MyStatCpuEquip
    ) {
        val relayMapping = MyStatCpuRelayMapping.values().find { it.ordinal == association }
        var isFanLoopCounterEnabled = false
        if (previousFanLoopVal > 0 && fanLoopCounter > 0) {
            isFanLoopCounterEnabled = true
        }
        when (relayMapping) {
            MyStatCpuRelayMapping.COOLING_STAGE_1, MyStatCpuRelayMapping.COOLING_STAGE_2 -> {
                if (basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY
                    || basicSettings.conditioningMode == StandaloneConditioningMode.AUTO) {
                    runRelayForCooling(relayMapping, port, tuner, relayStages)
                } else {
                    resetPort(port)
                }
            }

            MyStatCpuRelayMapping.HEATING_STAGE_1, MyStatCpuRelayMapping.HEATING_STAGE_2 -> {
                if (basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY
                    || basicSettings.conditioningMode == StandaloneConditioningMode.AUTO) {
                    runRelayForHeating(relayMapping, port, tuner, relayStages)
                } else {
                    resetPort(port)
                }
            }

            MyStatCpuRelayMapping.FAN_LOW_SPEED, MyStatCpuRelayMapping.FAN_HIGH_SPEED -> {
                if (basicSettings.fanMode != MyStatFanStages.OFF) {
                    runRelayForFanSpeed(
                        relayMapping, port, config, tuner, relayStages, basicSettings,
                        previousFanLoopVal, fanLoopCounter
                    )
                } else {
                    resetPort(port)
                }
            }

            MyStatCpuRelayMapping.FAN_ENABLED -> doFanEnabled(curState, port, fanLoopOutput, relayStages,isFanLoopCounterEnabled)
            MyStatCpuRelayMapping.OCCUPIED_ENABLED -> doOccupiedEnabled(port)
            MyStatCpuRelayMapping.HUMIDIFIER -> doHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMinHumidity, equip.zoneHumidity.readHisVal())
            MyStatCpuRelayMapping.DEHUMIDIFIER -> doDeHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMaxHumidity, equip.zoneHumidity.readHisVal())
            MyStatCpuRelayMapping.DCV_DAMPER -> doDcvDamperOperation(equip, port, tuner.relayActivationHysteresis, analogOutStages, config.co2Threshold.currentVal, false)

            else -> {}
        }
        CcuLog.d(L.TAG_CCU_MSCPU,"$port = $relayMapping : ${getCurrentLogicalPointStatus(logicalPointsList[port]!!)}")
    }

    private fun runRelayForFanSpeed(
        relayAssociation: MyStatCpuRelayMapping,
        whichPort: Port,
        config: MyStatCpuConfiguration,
        tuner: MyStatTuners,
        relayStages: HashMap<String, Int>,
        basicSettings: MyStatBasicSettings,
        previousFanLoopVal: Int, fanProtectionCounter: Int
    ) {
        if (basicSettings.fanMode == MyStatFanStages.AUTO && basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
            CcuLog.i(L.TAG_CCU_MSCPU, "Cond is Off , Fan is Auto   ")
            resetPort(whichPort)
            return
        }

        var localFanLoopOutput = fanLoopOutput

        val fanEnabledMapped = config.isAnyRelayEnabledAssociated(association = MyStatCpuRelayMapping.FAN_ENABLED.ordinal)
        val lowestStage = config.getLowestFanSelected()
        if (fanEnabledMapped) setFanEnabledStatus(true) else setFanEnabledStatus(false)

        resetFanLowestFanStatus()

        when (lowestStage) {
            MyStatCpuRelayMapping.FAN_LOW_SPEED -> setFanLowestFanLowStatus(true)
            MyStatCpuRelayMapping.FAN_HIGH_SPEED -> setFanLowestFanHighStatus(true)
            else -> {}
        }

        // In order to protect the fan, persist the fan for few cycles when there is a sudden change in
        // occupancy and decrease in fan loop output
        if (fanProtectionCounter > 0) {
            localFanLoopOutput = previousFanLoopVal
        }


        when (relayAssociation) {
            MyStatCpuRelayMapping.FAN_LOW_SPEED -> {
                doFanLowSpeed(
                    logicalPointsList[whichPort]!!, basicSettings.fanMode,
                    localFanLoopOutput, tuner.relayActivationHysteresis,
                    relayStages, runFanLowDuringDoorWindow
                )
            }

            MyStatCpuRelayMapping.FAN_HIGH_SPEED -> {
                doFanHighSpeed(
                    logicalPointsList[whichPort]!!, basicSettings.fanMode,
                    localFanLoopOutput, tuner.relayActivationHysteresis,
                    relayStages, runFanLowDuringDoorWindow
                )
            }

            else -> {}
        }
    }

    private fun runRelayForHeating(
        association: MyStatCpuRelayMapping, whichPort: Port, tuner: MyStatTuners,
        relayStages: HashMap<String, Int>
    ) {
        when (association) {
            MyStatCpuRelayMapping.HEATING_STAGE_1 -> {
                doHeatingStage1(whichPort, heatingLoopOutput, tuner.relayActivationHysteresis, relayStages)
            }

            MyStatCpuRelayMapping.HEATING_STAGE_2 -> {
                doHeatingStage2(whichPort, heatingLoopOutput, tuner.relayActivationHysteresis, 50, relayStages)
            }

            else -> {}
        }
        if (getCurrentPortStatus(whichPort) == 1.0) curState = ZoneState.HEATING
    }

    private fun runRelayForCooling(
        association: MyStatCpuRelayMapping, whichPort: Port, tuner: MyStatTuners,
        relayStages: HashMap<String, Int>
    ) {
        when (association) {
            MyStatCpuRelayMapping.COOLING_STAGE_1 -> {
                doCoolingStage1(
                    whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, relayStages
                )
            }
            MyStatCpuRelayMapping.COOLING_STAGE_2 -> {
                doCoolingStage2(
                    whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, 50, relayStages
                )
            }
            else -> {}
        }
        if (getCurrentPortStatus(whichPort) == 1.0) curState = ZoneState.COOLING
    }


    private fun runForKeyCardSensor(config: MyStatConfiguration, equip: MyStatEquip) {
        val isKeyCardEnabled = (config.universalIn1Enabled.enabled
                && config.universalIn1Association.associationVal == MyStatConfiguration.UniversalMapping.KEY_CARD_SENSOR.ordinal)
        keyCardIsInSlot((if (isKeyCardEnabled) 1.0 else 0.0), if (equip.keyCardSensor.readHisVal() > 0) 1.0 else 0.0, equip)
    }

    private fun runForDoorWindowSensor(
        config: MyStatConfiguration,
        equip: MyStatEquip,
        analogOutStages: HashMap<String, Int>,
        relayStages: HashMap<String, Int>
    ): Boolean {

        val isDoorOpen = isDoorOpenState(config, equip)
        CcuLog.d(L.TAG_CCU_MSCPU, " is Door Open ? $isDoorOpen")
        if (isDoorOpen) {
            resetLoops()
            resetLogicalPoints()
            analogOutStages.clear()
            relayStages.clear()
        }
        return isDoorOpen
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

    fun getProfileDomainEquip(node: Int): MyStatCpuEquip = cpuDeviceMap[node]!!

    fun addEquip(equipRef: String) {
        val equip = MyStatCpuEquip(equipRef)
        cpuDeviceMap[equip.nodeAddress] = equip
    }

    override fun getEquip(): Equip? {
        for (nodeAddress in cpuDeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }
    override fun getNodeAddresses(): Set<Short?> = cpuDeviceMap.keys.map { it.toShort() }.toSet()

    override fun getCurrentTemp(): Double {
        for (nodeAddress in cpuDeviceMap.keys) {
            return cpuDeviceMap[nodeAddress]!!.currentTemp.readHisVal()
        }
        return 0.0
    }

    override fun getProfileType(): ProfileType {
        return ProfileType.MYSTAT_CPU
    }

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        TODO("Not required")
    }
}