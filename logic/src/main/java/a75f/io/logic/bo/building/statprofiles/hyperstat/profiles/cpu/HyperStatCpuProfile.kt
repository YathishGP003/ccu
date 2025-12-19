package a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.cpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Point
import a75f.io.domain.equips.hyperstat.HsCpuEquip
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
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.AnalogOutConfigs
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsCpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsCpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.statprofiles.statcontrollers.HyperStatControlFactory
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.HyperStatProfileTuners
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.canWeDoConditioning
import a75f.io.logic.bo.building.statprofiles.util.canWeDoCooling
import a75f.io.logic.bo.building.statprofiles.util.canWeDoHeating
import a75f.io.logic.bo.building.statprofiles.util.canWeRunFan
import a75f.io.logic.bo.building.statprofiles.util.fetchBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.fetchHyperStatTuners
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getHSLogicalPointList
import a75f.io.logic.bo.building.statprofiles.util.getHsConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getPercentFromVolt
import a75f.io.logic.bo.building.statprofiles.util.isHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMediumUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.keyCardIsInSlot
import a75f.io.logic.bo.building.statprofiles.util.logResults
import a75f.io.logic.bo.building.statprofiles.util.runLowestFanSpeedDuringDoorOpen
import a75f.io.logic.bo.building.statprofiles.util.updateLoopOutputs
import a75f.io.logic.bo.building.statprofiles.util.updateOccupancyDetection
import a75f.io.logic.bo.building.statprofiles.util.updateOperatingMode
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.handlers.doAnalogOperation
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.util.uiutils.HyperStatUserIntentHandler
import kotlin.math.roundToInt


/**
 * @author Manjunath K
 * Created on 7/7/21.
 */
class HyperStatCpuProfile : HyperStatProfile(L.TAG_CCU_HSCPU) {


    private var previousFanStageStatus: StandaloneFanStage = StandaloneFanStage.OFF
    private val cpuDeviceMap: MutableMap<Int, HsCpuEquip> = mutableMapOf()

    override fun getProfileType() = ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT

    override fun updateZonePoints() {

        cpuDeviceMap.forEach { (nodeAddress, equip) ->
            cpuDeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as HsCpuEquip
            logIt("Process CPU Equip: node ${equip.nodeAddress} equipRef =  ${equip.equipRef}")
            processHyperStatCPUProfile(equip)
        }
    }

    fun addEquip(equipRef: String) {
        val equip = HsCpuEquip(equipRef)
        cpuDeviceMap[equip.nodeAddress] = equip
    }

    /**
     * Returns the value of the lowest activated heating stage based on the fan loop points.
     * If no heating stage is activated, returns the default fan loop output value.
     *
     * @return the value of the lowest activated heating stage or the default fan loop output value.
     */
    private fun getLowestHeatingStateActivated(equip: HsCpuEquip): Double {
        return listOf(
                equip.fanOutHeatingStage1.readDefaultVal(),
                equip.fanOutHeatingStage2.readDefaultVal(),
                equip.fanOutHeatingStage3.readDefaultVal()
        ).firstOrNull { it > 0 } ?: defaultFanLoopOutput
    }

    /**
     * Returns the value of the lowest activated cooling stage based on the fan loop points.
     * If no cooling stage is activated, returns the default fan loop output value.
     *
     * @return the value of the lowest activated cooling stage or the default fan loop output value.
     */
    private fun getLowestCoolingStateActivated(equip: HsCpuEquip): Double {
        return listOf(
                equip.fanOutCoolingStage1.readDefaultVal(),
                equip.fanOutCoolingStage2.readDefaultVal(),
                equip.fanOutCoolingStage3.readDefaultVal()
        ).firstOrNull { it > 0 } ?: defaultFanLoopOutput
    }

    /**
     * Returns the value of the non zero activated analog recirculate stage based on the fan loop points.
     * If no recirculate stage is activated, returns the default fan loop output value.
     * @return the value of the first non zero activated recirculate stage or the default fan loop output value.
     */
    private fun getAnalogRecirculateValueActivated(equip: HsCpuEquip): Double {
        return when {
            equip.analog1FanRecirculate.readDefaultVal() > 0 -> equip.analog1FanRecirculate.readDefaultVal()
            equip.analog2FanRecirculate.readDefaultVal() > 0 -> equip.analog2FanRecirculate.readDefaultVal()
            equip.analog3FanRecirculate.readDefaultVal() > 0 -> equip.analog3FanRecirculate.readDefaultVal()
            else -> defaultFanLoopOutput
        }
    }

    private fun doAnalogStagedFanAction(
            port: Port, fanLowPercent: Int, fanMediumPercent: Int, fanHighPercent: Int,
            fanMode: StandaloneFanStage, fanEnabledMapped: Boolean,
            conditioningMode: StandaloneConditioningMode, fanLoopOutput: Int,
            analogOutStages: HashMap<String, Int>, fanProtectionCounter: Int, equip: HsCpuEquip
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
            // added the new check if the fan loop output is with in relayActivationHysteresis ,sending the Analog recirculate value
            val relayActivationHysteresis = equip.standaloneRelayActivationHysteresis.readPriorityVal()
            if ((fanLoopOutput == 0 || runFanLowDuringDoorWindow || (fanLoopOutput > 0 && fanLoopOutput < relayActivationHysteresis)) && fanProtectionCounter == 0 && isInSoftOccupiedMode()) {
                fanLoopForAnalog = getPercentFromVolt(getAnalogRecirculateValueActivated(equip).roundToInt())
                logMsg = "Deadband"
            }
        } else {
            fanLoopForAnalog = when  {
                (isLowUserIntentFanMode(equip.fanOpMode)) -> fanLowPercent
                (isMediumUserIntentFanMode(equip.fanOpMode))-> fanMediumPercent
                (isHighUserIntentFanMode(equip.fanOpMode))  -> fanHighPercent
                else -> 0
            }
            // Store just in case when we switch from current occupied to unoccupied mode
            previousFanLoopValStaged = fanLoopForAnalog
        }

        if (fanLoopForAnalog > 0) analogOutStages[StatusMsgKeys.FAN_SPEED.name] = fanLoopForAnalog
        updateLogicalPoint(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
        logIt("$port = Staged Fan Speed($logMsg) $fanLoopForAnalog")
    }

    private fun doAnalogFanActionCpu(
        port: Port,
        fanLowPercent: Int,
        fanMediumPercent: Int,
        fanHighPercent: Int,
        fanMode: StandaloneFanStage,
        conditioningMode: StandaloneConditioningMode,
        fanLoopOutput: Int,
        analogOutStages: HashMap<String, Int>,
        previousFanLoopVal: Int,
        fanProtectionCounter: Int,
        equip: HyperStatEquip
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
                    isLowUserIntentFanMode(equip.fanOpMode) -> fanLoopForAnalog = fanLowPercent
                    isMediumUserIntentFanMode(equip.fanOpMode) -> fanLoopForAnalog = fanMediumPercent
                    isHighUserIntentFanMode(equip.fanOpMode) -> fanLoopForAnalog = fanHighPercent
                }
            }
            if (fanLoopForAnalog > 0) analogOutStages[StatusMsgKeys.FAN_SPEED.name] =
                fanLoopForAnalog
            updateLogicalPoint(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
        }
    }

    private fun getCoolingStateActivated(equip: HsCpuEquip): Double {
        return when {
            equip.coolingStage3.readHisVal() > 0 -> equip.fanOutCoolingStage3.readPriorityVal()
            equip.coolingStage2.readHisVal() > 0 -> equip.fanOutCoolingStage2.readPriorityVal()
            equip.coolingStage1.readHisVal() > 0 -> equip.fanOutCoolingStage1.readPriorityVal()
            else -> defaultFanLoopOutput
        }
    }

    private fun getHeatingStateActivated(equip: HsCpuEquip): Double {
        return when {
            equip.heatingStage3.readHisVal() > 0 -> equip.fanOutHeatingStage3.readPriorityVal()
            equip.heatingStage2.readHisVal() > 0 -> equip.fanOutHeatingStage2.readPriorityVal()
            equip.heatingStage1.readHisVal() > 0 -> equip.fanOutHeatingStage1.readPriorityVal()
            else -> defaultFanLoopOutput
        }
    }

    private fun getHeatingActivatedAnalogVoltage(fanEnabledMapped: Boolean, fanLoopOutput: Int, equip: HsCpuEquip): Int {
        // For title 24 compliance, check if fanEnabled is mapped, then set the fanloopForAnalog to the lowest heating state activated
        // and check if staged fan is inactive(fanLoopForAnalog == 0)

        var voltage = getPercentFromVolt(getHeatingStateActivated(equip).roundToInt())
        if (fanEnabledMapped && voltage == 0 && fanLoopOutput > 0) {
            voltage = getPercentFromVolt(getLowestHeatingStateActivated(equip).roundToInt())
        }
        return voltage
    }

    private fun getCoolingActivatedAnalogVoltage(fanEnabledMapped: Boolean, fanLoopOutput: Int, equip: HsCpuEquip): Int {
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

    fun processHyperStatCPUProfile(equip: HsCpuEquip) {

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

        val hyperStatTuners = fetchHyperStatTuners(equip) as HyperStatProfileTuners
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val fanModeSaved = FanModeCacheStorage.getHyperStatFanModeCache().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchBasicSettings(equip)
        val config = getHsConfiguration(equip.equipRef)
        val controllerFactory = HyperStatControlFactory(equip, controllers, stageCounts, derivedFanLoopOutput, zoneOccupancyState)

        logicalPointsList = getHSLogicalPointList(equip, config!!)
        curState = ZoneState.DEADBAND

        if (equipOccupancyHandler != null) {
            occupancyStatus = equipOccupancyHandler.currentOccupiedMode
            zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        }

        logIt( "Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        logIt( "After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")

        loopController.initialise(hyperStatTuners)
        loopController.dumpLogs()
        handleChangeOfDirection(currentTemp, userIntents, controllerFactory, equip)
        updateOperatingMode(currentTemp, averageDesiredTemp, basicSettings.conditioningMode, equip.operatingMode)

        resetEquip(equip)
        evaluateLoopOutputs(userIntents, basicSettings, hyperStatTuners, config, equip)
        updateOccupancyDetection(equip)

        doorWindowSensorOpenStatus = runForDoorWindowSensor(equip)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)

        keyCardIsInSlot(equip)
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

        updateTitle24LoopCounter(hyperStatTuners, basicSettings)

        if (canWeRunFan(basicSettings) && (doorWindowSensorOpenStatus.not())) {
            operateRelays(config as CpuConfiguration, basicSettings, equip, controllerFactory)
            operateAnalogOutputs(config, equip, basicSettings)
        } else {
            resetLogicalPoints(equip)
            if (isDoorOpenFromTitle24 && canWeRunFan(basicSettings)) {
                runLowestFanSpeedDuringDoorOpen(equip, L.TAG_CCU_HSCPU)
            }
        }

        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())

        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState =
            ZoneTempState.EMERGENCY
        printStatus(hyperStatTuners, basicSettings,userIntents,equip,config)
        HyperStatUserIntentHandler.updateHyperStatStatus(temperatureState, equip, L.TAG_CCU_HSCPU)
        updateTitle24Flags(basicSettings)
        logIt( "-------------------------------------------------")
    }

    private fun operateAnalogOutputs(
        config: CpuConfiguration, equip: HsCpuEquip,
        basicSettings: BasicSettings
    ) {
        config.apply {
            listOf(
                Pair(
                    AnalogOutConfigs(
                        analogOut1Enabled.enabled,
                        analogOut1Association.associationVal,
                        analogOut1MinMaxConfig,
                        analogOut1FanSpeedConfig,
                        recirculateFanConfig.analogOut1.currentVal
                    ), Port.ANALOG_OUT_ONE
                ),
                Pair(
                    AnalogOutConfigs(
                        analogOut2Enabled.enabled,
                        analogOut2Association.associationVal,
                        analogOut2MinMaxConfig,
                        analogOut2FanSpeedConfig,
                        recirculateFanConfig.analogOut2.currentVal
                    ), Port.ANALOG_OUT_TWO
                ),
                Pair(
                    AnalogOutConfigs(
                        analogOut3Enabled.enabled,
                        analogOut3Association.associationVal,
                        analogOut3MinMaxConfig,
                        analogOut3FanSpeedConfig,
                        recirculateFanConfig.analogOut3.currentVal
                    ), Port.ANALOG_OUT_THREE
                ),
            ).forEach { (analogOut, port) ->
                if (analogOut.enabled) {
                    val analogOutMapping = HsCpuAnalogOutMapping.values().find { it.ordinal == analogOut.association }
                    when (analogOutMapping) {

                        HsCpuAnalogOutMapping.COOLING -> {
                            doAnalogOperation(
                                canWeDoCooling(basicSettings.conditioningMode),
                                equip.analogOutStages, StatusMsgKeys.COOLING.name,
                                coolingLoopOutput, equip.coolingSignal
                            )
                        }

                        HsCpuAnalogOutMapping.HEATING -> {
                            doAnalogOperation(
                                canWeDoHeating(basicSettings.conditioningMode),
                                equip.analogOutStages, StatusMsgKeys.HEATING.name,
                                heatingLoopOutput, equip.heatingSignal
                            )
                        }

                        HsCpuAnalogOutMapping.LINEAR_FAN_SPEED -> {
                            doAnalogFanActionCpu(
                                port, analogOut.fanSpeed.low.currentVal.toInt(), analogOut.fanSpeed.medium.currentVal.toInt(),
                                analogOut.fanSpeed.high.currentVal.toInt(), basicSettings.fanMode, basicSettings.conditioningMode,
                                fanLoopOutput, equip.analogOutStages, previousFanLoopVal, fanLoopCounter, equip
                            )
                        }

                        HsCpuAnalogOutMapping.STAGED_FAN_SPEED -> {
                            doAnalogStagedFanAction(
                                port, analogOut.fanSpeed.low.currentVal.toInt(), analogOut.fanSpeed.medium.currentVal.toInt(),
                                analogOut.fanSpeed.high.currentVal.toInt(), basicSettings.fanMode,
                                config.isAnyRelayEnabledAssociated(association = HsCpuRelayMapping.FAN_ENABLED.ordinal),
                                basicSettings.conditioningMode, fanLoopOutput, equip.analogOutStages, fanLoopCounter, equip
                            )
                        }

                        HsCpuAnalogOutMapping.DCV_DAMPER -> {
                            doAnalogDCVAction(
                                port, equip.analogOutStages, config.zoneCO2Threshold.currentVal,
                                config.zoneCO2DamperOpeningRate.currentVal, equip
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun operateRelays(
        config: CpuConfiguration, basicSettings: BasicSettings, equip: HsCpuEquip,
        controllerFactory: HyperStatControlFactory
    ) {
        controllerFactory.addHsCpuControllers(config, fanLowVentilationAvailable)
        runControllers(equip, basicSettings)
    }

    private fun runControllers(
        equip: HsCpuEquip,
        basicSettings: BasicSettings
    ) {
        derivedFanLoopOutput.data = equip.fanLoopOutput.readHisVal()
        // This is for title 24 compliance
        if (fanLoopCounter > 0) {
            derivedFanLoopOutput.data = previousFanLoopVal.toDouble()
        }

        controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            val result = controller.runController()
            updateRelayStatus(controllerName, result, equip, basicSettings)
        }
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
        logIt("Occupancy: $occupancyStatus, Fan Loop Output: $fanLoopOutput, Previous Fan Loop Val: $previousFanLoopVal, Fan Loop Counter: $fanLoopCounter , hasZeroFanLoopBeenHandled $hasZeroFanLoopBeenHandled")
        if ((occupancyStatus != previousOccupancyStatus && fanLoopOutput < previousFanLoopVal) || (basicSettings.fanMode != previousFanStageStatus && fanLoopOutput < previousFanLoopVal) ||
            (fanLoopOutput == 0 && fanLoopOutput < previousFanLoopVal && !hasZeroFanLoopBeenHandled)) {
            fanLoopCounter = tuners.minFanRuntimePostConditioning
            hasZeroFanLoopBeenHandled = true
        } else if ((occupancyStatus != previousOccupancyStatus || (hasZeroFanLoopBeenHandled && fanLoopOutput > 0)) && fanLoopOutput > previousFanLoopVal) {
            fanLoopCounter = 0 // Reset the counter if the fan-loop output is greater than the previous value
            hasZeroFanLoopBeenHandled = false
        }
    }

    /**
     * Check if we should allow fan operation even when the door window is open
     * @return true if the door or window is open and the occupancy status is not UNOCCUPIED, otherwise false.
     */



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

    fun getProfileDomainEquip(node: Int): HsCpuEquip = cpuDeviceMap[node]!!

    override fun getCurrentTemp(): Double {
        for (nodeAddress in cpuDeviceMap.keys) {
            return cpuDeviceMap[nodeAddress]!!.currentTemp.readHisVal()
        }
        return 0.0
    }

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T { TODO("Not using now") }


    private fun updateRelayStatus(
        controllerName: String,
        result: Any,
        equip: HsCpuEquip,
        basicSettings: BasicSettings
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
                        0 -> updateStatus(equip.coolingStage1, isActive, Stage.COOLING_1.displayName)
                        1 -> updateStatus(equip.coolingStage2, isActive, Stage.COOLING_2.displayName)
                        2 -> updateStatus(equip.coolingStage3, isActive, Stage.COOLING_3.displayName)
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
                        0 -> updateStatus(equip.heatingStage1, isActive, Stage.HEATING_1.displayName)
                        1 -> updateStatus(equip.heatingStage2, isActive, Stage.HEATING_2.displayName)
                        2 -> updateStatus(equip.heatingStage3, isActive, Stage.HEATING_3.displayName)
                    }
                }
            }


            ControllerNames.FAN_SPEED_CONTROLLER -> {
                runTitle24Rule(equip)

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
                    stage: Int,
                    currentState: Boolean,
                    isLowestStageActive: Boolean
                ): Boolean {
                    val mode = equip.fanOpMode.readPriorityVal().toInt()
                    return if (mode == StandaloneFanStage.AUTO.ordinal) {
                        (canWeDoConditioning(basicSettings) && (currentState
                                || (fanEnabledStatus && fanLoopOutput > 0 && isLowestStageActive) ||
                                (isLowestStageActive && runFanLowDuringDoorWindow)))
                    } else {
                        checkUserIntentAction(stage)
                    }
                }

                val fanStages = result as List<Pair<Int, Boolean>>
                fanStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateStatus(equip.fanLowSpeed, isStageActive(stage, isActive, lowestStageFanLow), Stage.FAN_1.displayName )
                        1 -> updateStatus(equip.fanMediumSpeed, isStageActive(stage, isActive, lowestStageFanMedium), Stage.FAN_2.displayName )
                        2 -> updateStatus(equip.fanHighSpeed, isStageActive(stage, isActive, lowestStageFanHigh), Stage.FAN_3.displayName )
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
                logIt( "Unknown controller: $controllerName")
            }
        }
    }

    private fun printStatus(
        tuners: HyperStatProfileTuners,
        settings: BasicSettings,
        userIntents: UserIntents,
        equip: HyperStatEquip,
        config: HyperStatConfiguration
    ) {
        logIt("Fan speed multiplier  ${tuners.analogFanSpeedMultiplier} \n" +
                "Current Occupancy: ${Occupancy.values()[equip.occupancyMode.readHisVal().toInt()]}\n" +
                "Current Temp : $currentTemp " +
                "Desired (Heating: ${userIntents.heatingDesiredTemp} Cooling: ${userIntents.coolingDesiredTemp})\n" +
                "Operating Mode: ${equip.operatingMode.readHisVal()} \n" +
                "Fan Mode: ${settings.fanMode} Conditioning Mode: ${settings.conditioningMode}\n" +
                "Loop Outputs: (Heating: $heatingLoopOutput Cooling: $coolingLoopOutput " +
                "Fan : $fanLoopOutput DCV: $dcvLoopOutput)\n"
        )
        logResults(config, L.TAG_CCU_HSCPU, logicalPointsList)
    }
}