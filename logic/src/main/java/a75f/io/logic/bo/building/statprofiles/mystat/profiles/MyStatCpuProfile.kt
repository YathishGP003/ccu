package a75f.io.logic.bo.building.statprofiles.mystat.profiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Point
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.equips.mystat.MyStatEquip
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
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutConfigs
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.statcontrollers.MyStatControlFactory
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.MyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.MyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.canWeDoCooling
import a75f.io.logic.bo.building.statprofiles.util.canWeDoHeating
import a75f.io.logic.bo.building.statprofiles.util.fetchMyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.fetchMyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatLogicalPointList
import a75f.io.logic.bo.building.statprofiles.util.getPercentFromVolt
import a75f.io.logic.bo.building.statprofiles.util.isMyStatHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMyStatLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.logMsResults
import a75f.io.logic.bo.building.statprofiles.util.updateLogicalPoint
import a75f.io.logic.bo.building.statprofiles.util.updateLoopOutputs
import a75f.io.logic.bo.building.statprofiles.util.updateOperatingMode
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.util.uiutils.MyStatUserIntentHandler
import kotlin.math.roundToInt

/**
 * Created by Manjunath K on 16-01-2025.
 */

class MyStatCpuProfile: MyStatProfile(L.TAG_CCU_MSCPU) {
    private val cpuDeviceMap: MutableMap<Int, MyStatCpuEquip> = mutableMapOf()
    private var hasZeroFanLoopBeenHandled = false

    private lateinit var curState: ZoneState
    override lateinit var occupancyStatus: Occupancy


    override fun updateZonePoints() {
        cpuDeviceMap.forEach { (nodeAddress, equip) ->
            cpuDeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as MyStatCpuEquip
            logIt("Process CPU: equipRef =  ${equip.nodeAddress}")
            processCpuProfile(equip)
        }
    }

    fun processCpuProfile(equip: MyStatCpuEquip) {
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

        val myStatTuners = fetchMyStatTuners(equip) as MyStatTuners
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val fanModeSaved = FanModeCacheStorage.getMyStatFanModeCache().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchMyStatBasicSettings(equip)
        val config = getMyStatConfiguration(equip.equipRef) as MyStatCpuConfiguration
        val controllerFactory = MyStatControlFactory(equip, controllers, stageCounts, derivedFanLoopOutput, zoneOccupancyState)

        curState = ZoneState.DEADBAND
        logicalPointsList = getMyStatLogicalPointList(equip, config)

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
        updateOperatingMode(currentTemp, averageDesiredTemp, basicSettings.conditioningMode, equip.operatingMode)

        resetEquip(equip)
        evaluateLoopOutputs(userIntents, basicSettings, myStatTuners, config, equip)
        updateOccupancyDetection(equip)

        doorWindowSensorOpenStatus =
            runForDoorWindowSensor(config, equip, equip.analogOutStages, equip.relayStages)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)
        if (occupancyStatus == Occupancy.WINDOW_OPEN) resetLoopOutputs()
        runForKeyCardSensor(config, equip)

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

        updateTitle24LoopCounter(myStatTuners, basicSettings)
        if (basicSettings.fanMode != MyStatFanStages.OFF) {
            operateRelays(config, basicSettings, equip, controllerFactory)
            operateAnalogOutputs(config, basicSettings, equip.analogOutStages, equip)
        } else {
            resetLogicalPoints()
        }
        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())
        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState =
            ZoneTempState.EMERGENCY
        logIt(
            "Fan speed multiplier:  ${myStatTuners.analogFanSpeedMultiplier} " + "Current Occupancy: ${
                Occupancy.values()[equip.occupancyMode.readHisVal().toInt()]
            } \n"
                    + "Fan Mode : ${basicSettings.fanMode} Conditioning Mode ${basicSettings.conditioningMode} \n"
                    + "Current Temp : $currentTemp Desired (Heating: ${userIntents.heatingDesiredTemp}" + " Cooling: ${userIntents.coolingDesiredTemp})\n"
                    + "Loop Outputs: (Heating Loop: $heatingLoopOutput Cooling Loop: $coolingLoopOutput Fan Loop: $fanLoopOutput  dcvLoopOutput : $dcvLoopOutput) \n "
        )
        logMsResults(config, L.TAG_CCU_MSCPU, logicalPointsList)
        MyStatUserIntentHandler.updateMyStatStatus(temperatureState, equip, L.TAG_CCU_MSCPU)
        updateTitle24Flags(basicSettings)
        logIt("----------------------------------------------------------")
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

    /**
     * Updates the Title 24 loop counter based on changes in occupancy and fan-loop output.
     * If there is a change in occupancy and the fan-loop output is less than the previous value,
     * the fan loop counter is set to the minimum fan runtime post conditioning specified by the tuners.
     * If there is a change in occupancy and the fan-loop output is greater than the previous value,
     * the fan loop counter is reset to zero.
     *
     * @param tuners The MyStat profile tuners containing configuration parameters.
     * @param basicSettings settings containing fan mode state.
     */
    private fun updateTitle24LoopCounter(tuners: MyStatTuners, basicSettings: MyStatBasicSettings) {
        // Check if there is change in occupancy and the fan-loop output is less than the previous value,
        // then offer the fan

        logIt("Occupancy: $occupancyStatus, Fan Loop Output: $fanLoopOutput, Previous Fan Loop Val: $previousFanLoopVal, Fan Loop Counter: $fanLoopCounter , hasZeroFanLoopBeenHandled $hasZeroFanLoopBeenHandled")
        if ((occupancyStatus != previousOccupancyStatus && fanLoopOutput < previousFanLoopVal) ||
            (basicSettings.fanMode != previousFanStageStatus && fanLoopOutput < previousFanLoopVal) ||
            (fanLoopOutput == 0 && fanLoopOutput < previousFanLoopVal && !hasZeroFanLoopBeenHandled)
        ) {
            fanLoopCounter = tuners.minFanRuntimePostConditioning
            hasZeroFanLoopBeenHandled = true
        } else if ((occupancyStatus != previousOccupancyStatus || (hasZeroFanLoopBeenHandled && fanLoopOutput > 0)) && fanLoopOutput > previousFanLoopVal) {
            // If the fan loop output is greater than the previous value and the counter is greater than 0, reset the counter
            fanLoopCounter =
                0 // Reset the counter if the fan-loop output is greater than the previous value
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
            if (universalOut1.enabled && config.isRelayConfig(universalOut1Association.associationVal).not()) {
                handleAnalogOutState(
                    MyStatCpuAnalogOutConfigs(
                        true,
                        universalOut1Association.associationVal,
                        analogOut1MinMaxConfig,
                        analogOut1FanSpeedConfig,
                        universalOut1recircFanConfig.currentVal
                    ), config, basicSettings, analogOutStages, equip, Port.UNIVERSAL_OUT_ONE
                )
            }
            if (universalOut2.enabled && config.isRelayConfig(universalOut2Association.associationVal).not()) {
                handleAnalogOutState(
                    MyStatCpuAnalogOutConfigs(
                        true,
                        universalOut2Association.associationVal,
                        analogOut2MinMaxConfig,
                        analogOut2FanSpeedConfig,
                        universalOut2recircFanConfig.currentVal
                    ), config, basicSettings, analogOutStages, equip,Port.UNIVERSAL_OUT_TWO
                )
            }

        }
    }
    private fun handleAnalogOutState(
        analogOutState: MyStatCpuAnalogOutConfigs,
        config: MyStatCpuConfiguration,
        basicSettings: MyStatBasicSettings,
        analogOutStages: HashMap<String, Int>,
        equip: MyStatCpuEquip,
        port: Port
    ) {
        val analogMapping =
            MyStatCpuAnalogOutMapping.values().find { it.ordinal == analogOutState.association }
        when (analogMapping) {
            MyStatCpuAnalogOutMapping.COOLING -> {
                doAnalogCooling(
                    port,
                    basicSettings.conditioningMode,
                    analogOutStages,
                    coolingLoopOutput
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
                    fanLoopCounter,
                    equip, port
                )
            }

            MyStatCpuAnalogOutMapping.HEATING -> {
                doAnalogHeating(
                    port,
                    basicSettings.conditioningMode,
                    analogOutStages,
                    heatingLoopOutput
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
                    equip, port
                )
            }

            MyStatCpuAnalogOutMapping.DCV_DAMPER_MODULATION -> {
                doAnalogDCVAction(
                    port, analogOutStages, config.co2Threshold.currentVal,
                    config.co2DamperOpeningRate.currentVal,
                    equip
                )
            }

            else -> {}
        }
    }

    private fun doAnalogStagedFanAction(
        fanLowPercent: Int, fanHighPercent: Int, fanMode: MyStatFanStages,
        fanEnabledMapped: Boolean, conditioningMode: StandaloneConditioningMode,
        fanLoopOutput: Int, analogOutStages: HashMap<String, Int>,
        fanProtectionCounter: Int, equip: MyStatCpuEquip, port: Port
    ) {
        if (fanMode == MyStatFanStages.OFF) return

        var fanLoopForAnalog: Int
        var logMsg = "" // This will be overwritten based on priority

        if (fanMode == MyStatFanStages.AUTO) {

            if (conditioningMode == StandaloneConditioningMode.OFF) {
                updateLogicalPoint(logicalPointsList[port]!!, 0.0)
                return
            }

            fanLoopForAnalog = fanLoopOutput
            val coolingVoltage =
                getCoolingActivatedAnalogVoltage(fanEnabledMapped, fanLoopOutput, equip)
            val heatingVoltage =
                getHeatingActivatedAnalogVoltage(fanEnabledMapped, fanLoopOutput, equip)

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
            val relayActivationHysteresis =
                equip.standaloneRelayActivationHysteresis.readPriorityVal()
            if ((fanLoopOutput == 0 || runFanLowDuringDoorWindow || (fanLoopOutput > 0 && fanLoopOutput < relayActivationHysteresis)) && fanProtectionCounter == 0 && isInSoftOccupiedMode()) {
                fanLoopForAnalog =
                    getPercentFromVolt(getAnalogRecirculateValueActivated(equip).roundToInt())
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
        updateLogicalPoint(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
        logIt("$port = Staged Fan Speed($logMsg) $fanLoopForAnalog")
    }

    private fun getHeatingActivatedAnalogVoltage(
        fanEnabledMapped: Boolean,
        fanLoopOutput: Int,
        equip: MyStatCpuEquip
    ): Int {
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

    private fun getCoolingActivatedAnalogVoltage(
        fanEnabledMapped: Boolean,
        fanLoopOutput: Int,
        equip: MyStatCpuEquip
    ): Int {
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
        fanProtectionCounter: Int,
        equip: MyStatCpuEquip,
        port: Port
    ) {
        if (fanMode != MyStatFanStages.OFF) {
            var fanLoopForAnalog = 0
            if (fanMode == MyStatFanStages.AUTO) {
                if (conditioningMode == StandaloneConditioningMode.OFF) {
                    updateLogicalPoint(logicalPointsList[port]!!, 0.0)
                    return
                }
                fanLoopForAnalog = fanLoopOutput
                if (fanProtectionCounter > 0) {
                    fanLoopForAnalog = previousFanLoopVal
                }
            } else {
                when {
                    isMyStatLowUserIntentFanMode(equip.fanOpMode) -> fanLoopForAnalog =
                        fanLowPercent

                    isMyStatHighUserIntentFanMode(equip.fanOpMode) -> fanLoopForAnalog =
                        fanHighPercent
                }
            }
            if (fanLoopForAnalog > 0) analogOutStages[StatusMsgKeys.FAN_SPEED.name] =
                fanLoopForAnalog
            updateLogicalPoint(
                logicalPointsList[port]!!,
                fanLoopForAnalog.toDouble()
            )
        }
    }

    private fun operateRelays(
        config: MyStatCpuConfiguration, basicSettings: MyStatBasicSettings,
        equip: MyStatCpuEquip, controllerFactory: MyStatControlFactory
    ) {
        controllerFactory.addCpuControllers(config, fanLowVentilationAvailable)
        runControllers(equip, basicSettings, config)
    }

    private fun runControllers(
        equip: MyStatCpuEquip,
        basicSettings: MyStatBasicSettings,
        config: MyStatCpuConfiguration
    ) {

        zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        derivedFanLoopOutput.data = equip.fanLoopOutput.readHisVal()
        // This is for title 24 compliance
        if (fanLoopCounter > 0) {
            derivedFanLoopOutput.data = previousFanLoopVal.toDouble()
        }

        controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            val result = controller.runController()
            updateRelayStatus(controllerName, result, equip, basicSettings, config)
        }
    }

    private fun updateRelayStatus(
        controllerName: String,
        result: Any,
        equip: MyStatCpuEquip,
        basicSettings: MyStatBasicSettings,
        config: MyStatCpuConfiguration
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
            ControllerNames.COOLING_STAGE_CONTROLLER -> {
                val coolingStages = result as List<Pair<Int, Boolean>>
                coolingStages.forEach {

                    val (stage, isActive) = Pair(
                        it.first,
                        if (canWeDoCooling(basicSettings.conditioningMode)) it.second else false
                    )
                    when (stage) {
                        0 -> updateStatus(
                            equip.coolingStage1,
                            isActive,
                            Stage.COOLING_1.displayName,
                        )

                        1 -> updateStatus(
                            equip.coolingStage2,
                            isActive,
                            Stage.COOLING_2.displayName,


                        )
                    }
                }
            }

            ControllerNames.HEATING_STAGE_CONTROLLER -> {
                val heatingStages = result as List<Pair<Int, Boolean>>
                heatingStages.forEach {
                    val (stage, isActive) = Pair(
                        it.first,
                        if (canWeDoHeating(basicSettings.conditioningMode)) it.second else false
                    )
                    when (stage) {
                        0 -> updateStatus(
                            equip.heatingStage1,
                            isActive,
                            Stage.HEATING_1.displayName
                        )

                        1 -> updateStatus(
                            equip.heatingStage2,
                            isActive,
                            Stage.HEATING_2.displayName
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
                    stage: Int,
                    currentState: Boolean,
                    isLowestStageActive: Boolean
                ): Boolean {
                    val mode = equip.fanOpMode.readPriorityVal().toInt()
                    return if (mode == MyStatFanStages.AUTO.ordinal) {
                        (basicSettings.conditioningMode != StandaloneConditioningMode.OFF
                                && (currentState || (fanEnabledStatus && fanLoopOutput > 0 && isLowestStageActive)
                                || (isLowestStageActive && runFanLowDuringDoorWindow)))
                    } else {
                        checkUserIntentAction(stage)
                    }
                }

                val fanStages = result as List<Pair<Int, Boolean>>
                fanStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateStatus(
                            equip.fanLowSpeed,
                            isStageActive(stage, isActive, lowestStageFanLow),
                            Stage.FAN_1.displayName,
                        )

                        1 -> updateStatus(
                            equip.fanHighSpeed,
                            isStageActive(stage, isActive, lowestStageFanHigh),
                            Stage.FAN_2.displayName
                        )
                    }
                }
            }

            ControllerNames.FAN_ENABLED -> {
                var isFanLoopCounterEnabled = false
                if (previousFanLoopVal > 0 && fanLoopCounter > 0) {
                    isFanLoopCounterEnabled = true
                }
                // In order to protect the fan, persist the fan for few cycles when there is a sudden change in
                // occupancy and decrease in fan loop output
                var currentStatus = result as Boolean
                if (!currentStatus && isFanLoopCounterEnabled  ) {
                    currentStatus = true
                }
                updateStatus(equip.fanEnable, currentStatus, StatusMsgKeys.FAN_ENABLED.name)
            }
            ControllerNames.OCCUPIED_ENABLED -> updateStatus(equip.occupiedEnable, result)
            ControllerNames.HUMIDIFIER_CONTROLLER -> updateStatus(equip.humidifierEnable, result)
            ControllerNames.DEHUMIDIFIER_CONTROLLER -> updateStatus(equip.dehumidifierEnable, result)
            ControllerNames.DAMPER_RELAY_CONTROLLER -> updateStatus(equip.dcvDamper, result, StatusMsgKeys.DCV_DAMPER.name)
            else -> {
                logIt("Unknown controller: $controllerName")
            }
        }
    }

    private fun runTitle24Rule(config: MyStatCpuConfiguration) {
        resetFanLowestFanStatus()
        fanEnabledStatus =
            config.isAnyRelayEnabledAssociated(association = MyStatCpuRelayMapping.FAN_ENABLED.ordinal)
        val lowestStage = config.getLowestFanSelected()
        when (lowestStage) {
            MyStatCpuRelayMapping.FAN_LOW_SPEED -> lowestStageFanLow = true
            MyStatCpuRelayMapping.FAN_HIGH_SPEED -> lowestStageFanHigh = true
            else -> {}
        }
    }

    private fun runForKeyCardSensor(config: MyStatConfiguration, equip: MyStatEquip) {
        val isKeyCardEnabled = (config.universalIn1Enabled.enabled
                && config.universalIn1Association.associationVal == MyStatConfiguration.UniversalMapping.KEY_CARD_SENSOR.ordinal)
        keyCardIsInSlot(
            (if (isKeyCardEnabled) 1.0 else 0.0),
            if (equip.keyCardSensor.readHisVal() > 0) 1.0 else 0.0,
            equip
        )
    }

    private fun runForDoorWindowSensor(
        config: MyStatConfiguration,
        equip: MyStatEquip,
        analogOutStages: HashMap<String, Int>,
        relayStages: HashMap<String, Int>
    ): Boolean {

        val isDoorOpen = isDoorOpenState(config, equip)
        logIt(" is Door Open ? $isDoorOpen")
        if (isDoorOpen) {
            resetLoopOutputs()
            resetLogicalPoints()
            analogOutStages.clear()
            relayStages.clear()
        }
        return isDoorOpen
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