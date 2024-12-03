package a75f.io.logic.bo.building.hyperstat.profiles.cpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.hyperstat.CpuV2Equip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.common.BasicSettings
import a75f.io.logic.bo.building.hyperstat.common.FanModeCacheStorage
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil
import a75f.io.logic.bo.building.hyperstat.common.HyperStatEquipToBeDeleted
import a75f.io.logic.bo.building.hyperstat.common.HyperStatLoopController
import a75f.io.logic.bo.building.hyperstat.common.HyperStatProfileTuners
import a75f.io.logic.bo.building.hyperstat.common.UserIntents
import a75f.io.logic.bo.building.hyperstat.profiles.HyperStatPackageUnitProfile
import a75f.io.logic.bo.building.hyperstat.profiles.util.fetchBasicSettings
import a75f.io.logic.bo.building.hyperstat.profiles.util.fetchHyperStatTuners
import a75f.io.logic.bo.building.hyperstat.profiles.util.fetchUserIntents
import a75f.io.logic.bo.building.hyperstat.profiles.util.getConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.util.getLogicalPointList
import a75f.io.logic.bo.building.hyperstat.profiles.util.getPercentFromVolt
import a75f.io.logic.bo.building.hyperstat.profiles.util.updateAllLoopOutput
import a75f.io.logic.bo.building.hyperstat.profiles.util.updateOccupancyDetection
import a75f.io.logic.bo.building.hyperstat.profiles.util.updateOperatingMode
import a75f.io.logic.bo.building.hyperstat.v2.configs.AnalogInputAssociation
import a75f.io.logic.bo.building.hyperstat.v2.configs.AnalogOutConfigs
import a75f.io.logic.bo.building.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsCpuAnalogOutMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsCpuRelayMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.Th2InputAssociation
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.jobs.HyperStatUserIntentHandler
import kotlin.math.roundToInt


/**
 * @author Manjunath K
 * Created on 7/7/21.
 */
class HyperStatCpuProfile : HyperStatPackageUnitProfile() {

    private var coolingLoopOutput = 0
    private var heatingLoopOutput = 0
    private var fanLoopOutput = 0
    private var doorWindowSensorOpenStatus = false
    private var runFanLowDuringDoorWindow = false


    // Flags for keeping tab of occupancy during linear fan operation(Only to be used in doAnalogFanActionCpu())
    private var previousOccupancyStatus: Occupancy = Occupancy.NONE
    private var occupancyBeforeDoorWindow: Occupancy = Occupancy.UNOCCUPIED
    private var previousFanStageStatus: StandaloneFanStage = StandaloneFanStage.OFF
    private val cpuDeviceMap: MutableMap<Int, CpuV2Equip> = mutableMapOf()

    private val defaultFanLoopOutput = 0.0
    private var previousFanLoopVal = 0
    private var previousFanLoopValStaged = 0
    private var fanLoopCounter = 0

    private val cpuAlgorithm = HyperStatLoopController()
    override lateinit var occupancyStatus: Occupancy
    private lateinit var curState: ZoneState

    override fun getProfileType() = ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT

    override fun updateZonePoints() {

        cpuDeviceMap.forEach { (nodeAddress, equip) ->
            cpuDeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as CpuV2Equip
            CcuLog.d(L.TAG_CCU_HSCPU,"Process CPU Equip: node ${equip.nodeAddress} equipRef =  ${equip.equipRef}")
            processHyperStatCPUProfile(equip)
        }
    }

    fun addEquip(equipRef: String) {
        val equip = CpuV2Equip(equipRef)
        cpuDeviceMap[equip.nodeAddress] = equip
    }

    /**
     * Returns the value of the lowest activated heating stage based on the fan loop points.
     * If no heating stage is activated, returns the default fan loop output value.
     *
     * @return the value of the lowest activated heating stage or the default fan loop output value.
     */
    private fun getLowestHeatingStateActivated(equip: CpuV2Equip): Double {
        return listOf(
                equip.fanOutHeatingStage1.readPriorityVal(),
                equip.fanOutHeatingStage2.readPriorityVal(),
                equip.fanOutHeatingStage3.readPriorityVal()
        ).firstOrNull { it > 0 } ?: defaultFanLoopOutput
    }


    /**
     * Returns the value of the lowest activated cooling stage based on the fan loop points.
     * If no cooling stage is activated, returns the default fan loop output value.
     *
     * @return the value of the lowest activated cooling stage or the default fan loop output value.
     */
    private fun getLowestCoolingStateActivated(equip: CpuV2Equip): Double {
        return listOf(
                equip.fanOutCoolingStage1.readPriorityVal(),
                equip.fanOutCoolingStage2.readPriorityVal(),
                equip.fanOutCoolingStage3.readPriorityVal()
        ).firstOrNull { it > 0 } ?: defaultFanLoopOutput
    }

    /**
     * Returns the value of the non zero activated analog recirculate stage based on the fan loop points.
     * If no recirculate stage is activated, returns the default fan loop output value.
     * @return the value of the first non zero activated recirculate stage or the default fan loop output value.
     */
    private fun getAnalogRecirculateValueActivated(equip: CpuV2Equip): Double {
        return when {
            equip.analog1FanRecirculate.readHisVal() > 0 -> equip.analog1FanRecirculate.readPriorityVal()
            equip.analog2FanRecirculate.readHisVal() > 0 -> equip.analog2FanRecirculate.readPriorityVal()
            equip.analog3FanRecirculate.readHisVal() > 0 -> equip.analog3FanRecirculate.readPriorityVal()
            else -> defaultFanLoopOutput
        }
    }

    private fun doAnalogStagedFanAction(
            port: Port, fanLowPercent: Int, fanMediumPercent: Int, fanHighPercent: Int,
            fanMode: StandaloneFanStage, fanEnabledMapped: Boolean,
            conditioningMode: StandaloneConditioningMode, fanLoopOutput: Int,
            analogOutStages: HashMap<String, Int>, fanProtectionCounter: Int, equip: CpuV2Equip
    ) {
        if (fanMode == StandaloneFanStage.OFF) return

        var fanLoopForAnalog: Int
        var logMsg = "" // This will be overwritten based on priority

        if (fanMode == StandaloneFanStage.AUTO) {

            if (conditioningMode == StandaloneConditioningMode.OFF) {
                updateLogicalPoint(logicalPointsList[port]!!, 0.0)
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
            if ((fanLoopOutput == 0 || runFanLowDuringDoorWindow) && fanProtectionCounter == 0 && isInSoftOccupiedMode()) {
                fanLoopForAnalog = getPercentFromVolt(getAnalogRecirculateValueActivated(equip).roundToInt())
                logMsg = "Deadband"
            }
        } else {
            fanLoopForAnalog = when (fanMode) {
                StandaloneFanStage.LOW_CUR_OCC, StandaloneFanStage.LOW_OCC, StandaloneFanStage.LOW_ALL_TIME -> fanLowPercent
                StandaloneFanStage.MEDIUM_CUR_OCC, StandaloneFanStage.MEDIUM_OCC, StandaloneFanStage.MEDIUM_ALL_TIME -> fanMediumPercent
                StandaloneFanStage.HIGH_CUR_OCC, StandaloneFanStage.HIGH_OCC, StandaloneFanStage.HIGH_ALL_TIME -> fanHighPercent
                else -> 0
            }
            // Store just in case when we switch from current occupied to unoccupied mode
            previousFanLoopValStaged = fanLoopForAnalog
        }

        if (fanLoopForAnalog > 0) analogOutStages[AnalogOutput.FAN_SPEED.name] = fanLoopForAnalog
        updateLogicalPoint(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
        CcuLog.i(L.TAG_CCU_HSHST, "$port = Staged Fan Speed($logMsg) $fanLoopForAnalog")
    }

    private fun doAnalogFanActionCpu(
            port: Port, fanLowPercent: Int, fanMediumPercent: Int, fanHighPercent: Int,
            fanMode: StandaloneFanStage, conditioningMode: StandaloneConditioningMode, fanLoopOutput: Int,
            analogOutStages: HashMap<String, Int>, previousFanLoopVal: Int, fanProtectionCounter: Int
    ) {
        if (fanMode != StandaloneFanStage.OFF) {
            var fanLoopForAnalog = 0
            if (fanMode == StandaloneFanStage.AUTO) {
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
                    (fanMode == StandaloneFanStage.LOW_CUR_OCC
                            || fanMode == StandaloneFanStage.LOW_OCC
                            || fanMode == StandaloneFanStage.LOW_ALL_TIME) -> {
                        fanLoopForAnalog = fanLowPercent
                    }

                    (fanMode == StandaloneFanStage.MEDIUM_CUR_OCC
                            || fanMode == StandaloneFanStage.MEDIUM_OCC
                            || fanMode == StandaloneFanStage.MEDIUM_ALL_TIME) -> {
                        fanLoopForAnalog = fanMediumPercent
                    }

                    (fanMode == StandaloneFanStage.HIGH_CUR_OCC
                            || fanMode == StandaloneFanStage.HIGH_OCC
                            || fanMode == StandaloneFanStage.HIGH_ALL_TIME) -> {
                        fanLoopForAnalog = fanHighPercent
                    }
                }
            }
            if (fanLoopForAnalog > 0) analogOutStages[AnalogOutput.FAN_SPEED.name] = fanLoopForAnalog
            updateLogicalPoint(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
        }
    }

    private fun getCoolingStateActivated(equip: CpuV2Equip): Double {
        return when {
            equip.coolingStage3.readHisVal() > 0 -> equip.fanOutCoolingStage3.readPriorityVal()
            equip.coolingStage2.readHisVal() > 0 -> equip.fanOutCoolingStage2.readPriorityVal()
            equip.coolingStage1.readHisVal() > 0 -> equip.fanOutCoolingStage1.readPriorityVal()
            else -> defaultFanLoopOutput
        }
    }

    private fun getHeatingStateActivated(equip: CpuV2Equip): Double {
        return when {
            equip.heatingStage3.readHisVal() > 0 -> equip.fanOutHeatingStage3.readPriorityVal()
            equip.heatingStage2.readHisVal() > 0 -> equip.fanOutHeatingStage2.readPriorityVal()
            equip.heatingStage1.readHisVal() > 0 -> equip.fanOutHeatingStage1.readPriorityVal()
            else -> defaultFanLoopOutput
        }
    }

    private fun getHeatingActivatedAnalogVoltage(fanEnabledMapped: Boolean, fanLoopOutput: Int, equip: CpuV2Equip): Int {
        // For title 24 compliance, check if fanEnabled is mapped, then set the fanloopForAnalog to the lowest heating state activated
        // and check if staged fan is inactive(fanLoopForAnalog == 0)

        var voltage = getPercentFromVolt(getHeatingStateActivated(equip).roundToInt())
        if (fanEnabledMapped && voltage == 0 && fanLoopOutput > 0) {
            voltage = getPercentFromVolt(getLowestHeatingStateActivated(equip).roundToInt())
        }
        return voltage
    }

    private fun getCoolingActivatedAnalogVoltage(fanEnabledMapped: Boolean, fanLoopOutput: Int, equip: CpuV2Equip): Int {
        // For title 24 compliance, check if fanEnabled is mapped, then set the fan loop ForAnalog to the lowest cooling state activated
        // and check if staged fan is inactive(fanLoopForAnalog == 0)

        var voltage = getPercentFromVolt(getCoolingStateActivated(equip).roundToInt())
        if (fanEnabledMapped && voltage == 0 && fanLoopOutput > 0) {
            voltage = getPercentFromVolt(getLowestCoolingStateActivated(equip).roundToInt())
        }
        return voltage
    }

    /**
     * Updates the Title 24 flags by storing the current occupancy status and fan-loop output.
     * The previous occupancy status and fan-loop output are updated for use in the next loop iteration.
     * If the fan loop counter is greater than 0, it decrements the counter.
     * @param basicSettings settings containing fan mode state.
     */
    private fun updateTitle24Flags(basicSettings: BasicSettings) {
        // Store the fan-loop output/occupancy for usage in next loop
        previousOccupancyStatus = occupancyStatus
        if (occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        // Store the fan status so that when the zone goes from user defined fan state to unoccupied state, the fan protection can be offered
        previousFanStageStatus = basicSettings.fanMode
        // Store the fanloop output when the zone was not in UNOCCUPIED state
        if (fanLoopCounter == 0) previousFanLoopVal = fanLoopOutput
        if (fanLoopCounter > 0) fanLoopCounter--
    }

    fun processHyperStatCPUProfile(equip: CpuV2Equip) {

        if (Globals.getInstance().isTestMode) {
            CcuLog.d(L.TAG_CCU_HSCPU, "Test mode is on: ${equip.equipRef}")
            return
        }

        if (mInterface != null) mInterface.refreshView()

        val relayStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()
        val config = getConfiguration(equip.equipRef)

        logicalPointsList = getLogicalPointList(equip, config!!)
        hsHaystackUtil = HSHaystackUtil(equip.equipRef, CCUHsApi.getInstance())

        if (isRFDead) {
            handleRFDead(equip)
            return
        } else if (isZoneDead) {
            handleDeadZone(equip)
            return
        }
        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode


        val hyperStatTuners = fetchHyperStatTuners(equip)
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)

        val fanModeSaved = FanModeCacheStorage().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchBasicSettings(equip)

        CcuLog.d(L.TAG_CCU_HSCPU, "Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        CcuLog.d(L.TAG_CCU_HSCPU, "After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")

        cpuAlgorithm.initialise(hyperStatTuners)
        cpuAlgorithm.dumpLogs()
        handleChangeOfDirection(userIntents)
        updateOperatingMode(currentTemp, averageDesiredTemp, basicSettings, equip)

        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0

        evaluateLoopOutputs(userIntents, basicSettings, hyperStatTuners)
        updateOccupancyDetection(equip)

        doorWindowSensorOpenStatus = runForDoorWindowSensor(config, equip)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)
        // Updating the occupancy status happens one cycle later after update of doorWindowSensorOpenStatus.
        // Once it is detected then reset the loop output values
        if (occupancyStatus == Occupancy.WINDOW_OPEN) resetLoopOutputValues()
        runForKeyCardSensor(config, equip)
        updateAllLoopOutput(equip, coolingLoopOutput, heatingLoopOutput, fanLoopOutput)
        updateTitle24LoopCounter(hyperStatTuners, basicSettings)

        CcuLog.d(
                L.TAG_CCU_HSCPU,"Fan speed multiplier  ${hyperStatTuners.analogFanSpeedMultiplier} " +
                        "Current Occupancy: ${Occupancy.values()[equip.occupancyMode.readHisVal().toInt()]} \n" +
                        "Current Temp : $currentTemp Desired (Heating: ${userIntents.zoneHeatingTargetTemperature} Cooling: ${userIntents.zoneCoolingTargetTemperature})\n" +
                        "Loop Outputs: (Heating: $heatingLoopOutput Cooling: $coolingLoopOutput Fan : $fanLoopOutput) \n"
        )

        if (basicSettings.fanMode != StandaloneFanStage.OFF) {
            operateRelays(config as CpuConfiguration, hyperStatTuners, userIntents, basicSettings, relayStages)
            operateAnalogOutputs(config, equip, basicSettings, analogOutStages)
        } else {
            resetLogicalPoints()
        }

        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())

        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY

        HyperStatUserIntentHandler.updateHyperStatStatus(equip.equipRef, relayStages, analogOutStages, temperatureState, equip)
        updateTitle24Flags(basicSettings)
        CcuLog.d(L.TAG_CCU_HSCPU,"-------------------------------------------------")
    }

    private fun operateAnalogOutputs(
            config: CpuConfiguration, equip: CpuV2Equip,
            basicSettings: BasicSettings, analogOutStages: HashMap<String, Int>
    ) {
        config.apply {
            listOf(
                    Pair(AnalogOutConfigs(analogOut1Enabled.enabled, analogOut1Association.associationVal, analogOut1MinMaxConfig, analogOut1FanSpeedConfig, recirculateFanConfig.analogOut1.currentVal), Port.ANALOG_OUT_ONE),
                    Pair(AnalogOutConfigs(analogOut2Enabled.enabled, analogOut2Association.associationVal, analogOut2MinMaxConfig, analogOut2FanSpeedConfig, recirculateFanConfig.analogOut2.currentVal), Port.ANALOG_OUT_TWO),
                    Pair(AnalogOutConfigs(analogOut3Enabled.enabled, analogOut3Association.associationVal, analogOut3MinMaxConfig, analogOut3FanSpeedConfig, recirculateFanConfig.analogOut3.currentVal), Port.ANALOG_OUT_THREE),
            ).forEach { (analogOutConfig, port) ->
                if (analogOutConfig.enabled) {
                    handleAnalogOutState(analogOutConfig, equip, config, port, basicSettings, analogOutStages)
                }
            }
        }
    }

    private fun handleAnalogOutState(
            analogOut: AnalogOutConfigs, equip: CpuV2Equip, config: CpuConfiguration,
            port: Port, basicSettings: BasicSettings, analogOutStages: HashMap<String, Int>
    ) {
        val analogOutMapping = HsCpuAnalogOutMapping.values().find { it.ordinal == analogOut.association }
        when (analogOutMapping) {
            HsCpuAnalogOutMapping.COOLING -> {
                doAnalogCooling(
                        port, basicSettings.conditioningMode, analogOutStages, coolingLoopOutput
                )
            }

            HsCpuAnalogOutMapping.HEATING -> {
                doAnalogHeating(
                        port, basicSettings.conditioningMode, analogOutStages, heatingLoopOutput
                )
            }

            HsCpuAnalogOutMapping.LINEAR_FAN_SPEED -> {
                doAnalogFanActionCpu(
                        port, analogOut.fanSpeed.low.currentVal.toInt(), analogOut.fanSpeed.medium.currentVal.toInt(),
                        analogOut.fanSpeed.high.currentVal.toInt(), basicSettings.fanMode, basicSettings.conditioningMode,
                        fanLoopOutput, analogOutStages, previousFanLoopVal, fanLoopCounter
                )
            }

            HsCpuAnalogOutMapping.STAGED_FAN_SPEED -> {
                doAnalogStagedFanAction(
                        port, analogOut.fanSpeed.low.currentVal.toInt(), analogOut.fanSpeed.medium.currentVal.toInt(),
                        analogOut.fanSpeed.high.currentVal.toInt(), basicSettings.fanMode,
                        config.isAnyRelayEnabledAssociated(association = HsCpuRelayMapping.FAN_ENABLED.ordinal),
                        basicSettings.conditioningMode, fanLoopOutput, analogOutStages, fanLoopCounter, equip
                )
            }

            HsCpuAnalogOutMapping.DCV_DAMPER -> {
                doAnalogDCVAction(
                        port, analogOutStages, config.zoneCO2Threshold.currentVal,
                        config.zoneCO2DamperOpeningRate.currentVal, isDoorOpenState(config, equip),equip
                )
            }

            else -> {}
        }
        CcuLog.d(L.TAG_CCU_HSCPU,"$port = $analogOutMapping : ${getCurrentLogicalPointStatus(logicalPointsList[port]!!)}")
    }

    private fun operateRelays(
            config: CpuConfiguration, tuner: HyperStatProfileTuners, userIntents: UserIntents,
            basicSettings: BasicSettings,
            relayStages: HashMap<String, Int>
    ) {
        listOf(
                Triple(config.relay1Enabled.enabled, config.relay1Association.associationVal, Port.RELAY_ONE),
                Triple(config.relay2Enabled.enabled, config.relay2Association.associationVal, Port.RELAY_TWO),
                Triple(config.relay3Enabled.enabled, config.relay3Association.associationVal, Port.RELAY_THREE),
                Triple(config.relay4Enabled.enabled, config.relay4Association.associationVal, Port.RELAY_FOUR),
                Triple(config.relay5Enabled.enabled, config.relay5Association.associationVal, Port.RELAY_FIVE),
                Triple(config.relay6Enabled.enabled, config.relay6Association.associationVal, Port.RELAY_SIX)
        ).forEach { (enabled, association, port) ->
            if (enabled) handleRelayState(association, config, port, tuner, userIntents, basicSettings, relayStages)
        }
    }

    private fun handleRelayState(
            association: Int, config: CpuConfiguration, port: Port, tuner: HyperStatProfileTuners,
            userIntents: UserIntents, basicSettings: BasicSettings, relayStages: HashMap<String, Int>
    ) {
        val relayMapping = HsCpuRelayMapping.values().find { it.ordinal == association }
        when (relayMapping) {
            HsCpuRelayMapping.COOLING_STAGE_1, HsCpuRelayMapping.COOLING_STAGE_2, HsCpuRelayMapping.COOLING_STAGE_3 -> {
                if (basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY
                        || basicSettings.conditioningMode == StandaloneConditioningMode.AUTO) {
                    runRelayForCooling(relayMapping, port, config, tuner, relayStages)
                } else {
                    resetPort(port)
                }
            }

            HsCpuRelayMapping.HEATING_STAGE_1, HsCpuRelayMapping.HEATING_STAGE_2, HsCpuRelayMapping.HEATING_STAGE_3 -> {
                if (basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY
                        || basicSettings.conditioningMode == StandaloneConditioningMode.AUTO) {
                    runRelayForHeating(relayMapping, port, config, tuner, relayStages)
                } else {
                    resetPort(port)
                }
            }

            HsCpuRelayMapping.FAN_LOW_SPEED, HsCpuRelayMapping.FAN_MEDIUM_SPEED, HsCpuRelayMapping.FAN_HIGH_SPEED -> {
                if (basicSettings.fanMode != StandaloneFanStage.OFF) {
                    runRelayForFanSpeed(relayMapping, port, config, tuner, relayStages, basicSettings,
                            previousFanLoopVal, fanLoopCounter)
                } else {
                    resetPort(port)
                }
            }

            HsCpuRelayMapping.FAN_ENABLED -> doFanEnabled(curState, port, fanLoopOutput)
            HsCpuRelayMapping.OCCUPIED_ENABLED -> doOccupiedEnabled(port)
            HsCpuRelayMapping.HUMIDIFIER -> doHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMinInsideHumidity)
            HsCpuRelayMapping.DEHUMIDIFIER -> doDeHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMaxInsideHumidity)
            else -> {}
        }
        CcuLog.d(L.TAG_CCU_HSCPU,"$port = $relayMapping : ${getCurrentLogicalPointStatus(logicalPointsList[port]!!)}")
    }

    private fun runRelayForCooling(
            association: HsCpuRelayMapping, whichPort: Port, config: CpuConfiguration,
            tuner: HyperStatProfileTuners, relayStages: HashMap<String, Int>
    ) {
        when (association) {
            HsCpuRelayMapping.COOLING_STAGE_1 -> {
                doCoolingStage1(
                        whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, relayStages
                )
            }

            HsCpuRelayMapping.COOLING_STAGE_2 -> {
                val divider = if (config.getHighestCoolingStage() == HsCpuRelayMapping.COOLING_STAGE_2) 50 else 33
                doCoolingStage2(
                        whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, divider, relayStages
                )
            }

            HsCpuRelayMapping.COOLING_STAGE_3 -> {
                doCoolingStage3(
                        whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, relayStages
                )
            }

            else -> {}
        }

        if (getCurrentPortStatus(whichPort) == 1.0) curState = ZoneState.COOLING
    }

    private fun runRelayForHeating(
            association: HsCpuRelayMapping, whichPort: Port, config: CpuConfiguration,
            tuner: HyperStatProfileTuners, relayStages: HashMap<String, Int>
    ) {
        when (association) {
            HsCpuRelayMapping.HEATING_STAGE_1 -> {
                doHeatingStage1(whichPort, heatingLoopOutput, tuner.relayActivationHysteresis, relayStages)
            }

            HsCpuRelayMapping.HEATING_STAGE_2 -> {
                val divider = if (config.getHighestHeatingStage() == HsCpuRelayMapping.HEATING_STAGE_2) 50 else 33
                doHeatingStage2(whichPort, heatingLoopOutput, tuner.relayActivationHysteresis, divider, relayStages)
            }

            HsCpuRelayMapping.HEATING_STAGE_3 -> {
                doHeatingStage3(whichPort, heatingLoopOutput, tuner.relayActivationHysteresis, relayStages)
            }

            else -> {}
        }
        if (getCurrentPortStatus(whichPort) == 1.0) curState = ZoneState.HEATING
    }

    private fun runRelayForFanSpeed(
            relayAssociation: HsCpuRelayMapping, whichPort: Port, config: CpuConfiguration,
            tuner: HyperStatProfileTuners, relayStages: HashMap<String, Int>, basicSettings: BasicSettings,
            previousFanLoopVal: Int, fanProtectionCounter: Int
    ) {
        if (basicSettings.fanMode == StandaloneFanStage.AUTO
                && basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
            resetPort(whichPort)
            return
        }
        var localFanLoopOutput = fanLoopOutput
        val highestStage = config.getHighestFanSelected()
        val divider = if (highestStage == HsCpuRelayMapping.FAN_MEDIUM_SPEED) 50 else 33
        val fanEnabledMapped = config.isAnyRelayEnabledAssociated(association = HsCpuRelayMapping.FAN_ENABLED.ordinal)
        val lowestStage = config.getLowestFanSelected()

        if (fanEnabledMapped) setFanEnabledStatus(true) else setFanEnabledStatus(false)

        resetFanLowestFanStatus()

        when (lowestStage) {
            HsCpuRelayMapping.FAN_LOW_SPEED -> setFanLowestFanLowStatus(true)
            HsCpuRelayMapping.FAN_MEDIUM_SPEED -> setFanLowestFanMediumStatus(true)
            HsCpuRelayMapping.FAN_HIGH_SPEED -> setFanLowestFanHighStatus(true)
            else -> {}
        }

        // In order to protect the fan, persist the fan for few cycles when there is a sudden change in
        // occupancy and decrease in fan loop output
        if (fanProtectionCounter > 0) {
            localFanLoopOutput = previousFanLoopVal
        }

        when (relayAssociation) {
            HsCpuRelayMapping.FAN_LOW_SPEED -> {
                doFanLowSpeed(
                        logicalPointsList[whichPort]!!, null, null, basicSettings.fanMode,
                        localFanLoopOutput, tuner.relayActivationHysteresis, relayStages, divider, runFanLowDuringDoorWindow)
            }

            HsCpuRelayMapping.FAN_MEDIUM_SPEED -> {
                doFanMediumSpeed(
                        logicalPointsList[whichPort]!!, null, basicSettings.fanMode,
                        localFanLoopOutput, tuner.relayActivationHysteresis, divider, relayStages, runFanLowDuringDoorWindow)
            }

            HsCpuRelayMapping.FAN_HIGH_SPEED -> {
                doFanHighSpeed(
                        logicalPointsList[whichPort]!!, basicSettings.fanMode,
                        localFanLoopOutput, tuner.relayActivationHysteresis, relayStages, runFanLowDuringDoorWindow)
            }

            else -> return
        }
        CcuLog.d(L.TAG_CCU_HSCPU,"$whichPort = $relayAssociation : ${getCurrentLogicalPointStatus(logicalPointsList[whichPort]!!)}")
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
    private fun updateTitle24LoopCounter(tuners: HyperStatProfileTuners, basicSettings: BasicSettings) {
        // Check if there is change in occupancy and the fan-loop output is less than the previous value,
        // then offer the fan protection
        if ((occupancyStatus != previousOccupancyStatus && fanLoopOutput < previousFanLoopVal) ||
                (basicSettings.fanMode != previousFanStageStatus && fanLoopOutput < previousFanLoopVal)) {
            fanLoopCounter = tuners.minFanRuntimePostConditioning
        } else if (occupancyStatus != previousOccupancyStatus && fanLoopOutput > previousFanLoopVal)
            fanLoopCounter = 0 // Reset the counter if the fan-loop output is greater than the previous value
    }

    /**
     * Check if we should allow fan operation even when the door window is open
     * @return true if the door or window is open and the occupancy status is not UNOCCUPIED, otherwise false.
     */
    private fun checkFanOperationAllowedDoorWindow(userIntents: UserIntents): Boolean {
        return if (currentTemp < userIntents.zoneCoolingTargetTemperature && currentTemp > userIntents.zoneHeatingTargetTemperature) {
            doorWindowSensorOpenStatus &&
                    occupancyBeforeDoorWindow != Occupancy.UNOCCUPIED &&
                    occupancyBeforeDoorWindow != Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
                    occupancyBeforeDoorWindow != Occupancy.VACATION
        } else {
            doorWindowSensorOpenStatus
        }
    }

    private fun runForKeyCardSensor(config: HyperStatConfiguration, equip: HyperStatEquip) {
        val isKeyCardEnabled = (config.isEnabledAndAssociated(config.analogIn1Enabled, config.analogIn1Association, AnalogInputAssociation.KEY_CARD_SENSOR.ordinal)
                || config.isEnabledAndAssociated(config.analogIn1Enabled, config.analogIn2Association, AnalogInputAssociation.KEY_CARD_SENSOR.ordinal))
        keyCardIsInSlot((if (isKeyCardEnabled) 1.0 else 0.0), if (equip.keyCardSensor.readHisVal() > 0) 1.0 else 0.0, equip)
    }

    private fun runForDoorWindowSensor(config: HyperStatConfiguration, equip: HyperStatEquip): Boolean {
        val isDoorOpen = isDoorOpenState(config, equip)
        CcuLog.i(L.TAG_CCU_HSHST, " is Door Open ? $isDoorOpen")
        return isDoorOpen
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
            CcuLog.d(L.TAG_CCU_HSHST, "TH1 Door Window sensor value : Door is $sensorValue")
        }

        if (isAnalogHasDoorWindowMapping()) {
            val sensorValue = equip.doorWindowSensorTitle24.readHisVal().toInt()
            if (sensorValue == 1) isDoorOpen = true
            analogSensorEnabled = true
            CcuLog.d(L.TAG_CCU_HSHST, "Analog Input has mapping Door Window sensor value : Door is $sensorValue")

        }
        doorWindowIsOpen(
                if (th2SensorEnabled || analogSensorEnabled) 1.0 else 0.0,
                if (isDoorOpen) 1.0 else 0.0, equip
        )
        return isDoorOpen
    }

    private fun resetLoopOutputValues() {
        CcuLog.d(L.TAG_CCU_HSHST, "Resetting all the loop output values: ")
        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
    }

    private fun evaluateLoopOutputs(userIntents: UserIntents, basicSettings: BasicSettings, hyperStatTuners: HyperStatProfileTuners) {
        when (state) {
            //Update coolingLoop when the zone is in cooling or it was in cooling and no change over happened yet.
            ZoneState.COOLING -> coolingLoopOutput = cpuAlgorithm.calculateCoolingLoopOutput(
                    currentTemp, userIntents.zoneCoolingTargetTemperature
            ).toInt().coerceAtLeast(0)

            //Update heatingLoop when the zone is in heating or it was in heating and no change over happened yet.
            ZoneState.HEATING -> heatingLoopOutput = cpuAlgorithm.calculateHeatingLoopOutput(
                    userIntents.zoneHeatingTargetTemperature, currentTemp
            ).toInt().coerceAtLeast(0)

            else -> CcuLog.d(L.TAG_CCU_HSHST, " Zone is in deadband")
        }

        if (coolingLoopOutput > 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY
                        || basicSettings.conditioningMode == StandaloneConditioningMode.AUTO)) {
            fanLoopOutput = ((coolingLoopOutput * hyperStatTuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
        } else if (heatingLoopOutput > 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY
                        || basicSettings.conditioningMode == StandaloneConditioningMode.AUTO)) {
            fanLoopOutput = ((heatingLoopOutput * hyperStatTuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
        }
    }

    private fun handleChangeOfDirection(userIntents: UserIntents) {
        if (currentTemp > userIntents.zoneCoolingTargetTemperature && state != ZoneState.COOLING) {
            cpuAlgorithm.resetCoolingControl()
            state = ZoneState.COOLING
            CcuLog.d(L.TAG_CCU_HSHST, "Resetting cooling")
        } else if (currentTemp < userIntents.zoneHeatingTargetTemperature && state != ZoneState.HEATING) {
            cpuAlgorithm.resetHeatingControl()
            state = ZoneState.HEATING
            CcuLog.d(L.TAG_CCU_HSHST, "Resetting heating")
        }
    }

    override fun getDisplayCurrentTemp() = averageZoneTemp

    override fun getAverageZoneTemp(): Double {
        var tempTotal = 0.0
        var nodeCount = 0
        cpuDeviceMap.forEach { (_, cpuDevice) ->
            if (cpuDevice.currentTemp.readHisVal() > 0) {
                tempTotal += cpuDevice.currentTemp.readHisVal()
                nodeCount++
            }
        }
        return if (nodeCount == 0) 0.0 else tempTotal / nodeCount
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

    override fun getEquip(): Equip? {
        for (nodeAddress in cpuDeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    override fun getNodeAddresses(): Set<Short?> =  cpuDeviceMap.keys.map { it.toShort() }.toSet()

    fun getProfileDomainEquip(node: Int): CpuV2Equip = cpuDeviceMap[node]!!

    override fun getCurrentTemp(): Double {
        for (nodeAddress in cpuDeviceMap.keys) {
            return cpuDeviceMap[nodeAddress]!!.currentTemp.readHisVal()
        }
        return 0.0
    }

    override fun addNewEquip(node: Short, room: String, floor: String, baseConfig: BaseProfileConfiguration) { TODO("Not using now") }
    override fun getHyperStatEquip(node: Short): HyperStatEquipToBeDeleted { TODO("Not using now revisit once all are migrated") }
    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T { TODO("Not using now") }

}