package a75f.io.logic.bo.building.hyperstat.profiles.cpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
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
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getActualFanMode
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil
import a75f.io.logic.bo.building.hyperstat.common.HyperStatEquip
import a75f.io.logic.bo.building.hyperstat.common.HyperStatProfileTuners
import a75f.io.logic.bo.building.hyperstat.common.HyperstatLoopController
import a75f.io.logic.bo.building.hyperstat.common.UserIntents
import a75f.io.logic.bo.building.hyperstat.profiles.HyperStatPackageUnitProfile
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.jobs.HyperStatUserIntentHandler
import a75f.io.logic.jobs.HyperStatUserIntentHandler.Companion.hyperStatStatus
import a75f.io.logic.tuners.TunerUtil
import com.fasterxml.jackson.annotation.JsonIgnore
import kotlin.math.roundToInt


/**
 * @author tcase@75f.io
 * Created on 7/7/21.
 */
class HyperStatCpuProfile : HyperStatPackageUnitProfile() {

    // One zone can have many hyperstat devices.  Each has its own address and equip representation
    private val cpuDeviceMap: MutableMap<Short, HyperStatCpuEquip> = mutableMapOf()

    private var coolingLoopOutput = 0
    private var heatingLoopOutput = 0
    private var fanLoopOutput = 0
    private var doorWindowSensorOpenStatus = false
    private var runFanLowDuringDoorWindow = false
    private val defaultFanLoopOutput = 0.0

    override lateinit var occupancyStatus: Occupancy

    // Flags for keeping tab of occupancy during linear fan operation(Only to be used in doAnalogFanActionCpu())
    private var previousOccupancyStatus: Occupancy = Occupancy.NONE
    private var occupancyBeforeDoorWindow: Occupancy = Occupancy.UNOCCUPIED
    private var previousFanStageStatus: StandaloneFanStage = StandaloneFanStage.OFF
    private var previousFanLoopVal = 0
    private var previousFanLoopValStaged = 0
    private var fanLoopCounter = 0

    private val hyperstatCPUAlgorithm = HyperstatLoopController()

    private lateinit var curState: ZoneState

    override fun getProfileType() = ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        val equip = cpuDeviceMap[address]
        return equip?.getConfiguration() as T
    }

    override fun updateZonePoints() {
        cpuDeviceMap.forEach { (_, equip) ->
            CcuLog.d(L.TAG_CCU_HSCPU,"Process Equip: equipRef =  ${equip.equipRef}")
            processHyperStatCPUProfile(equip)
        }
    }

    fun addEquip(node: Short): HyperStatEquip {
        val equip = HyperStatCpuEquip(node)
        CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "From addEquip(), calling initEquipReference()...")
        equip.initEquipReference(node)
        cpuDeviceMap[node] = equip
        return equip
    }

    override fun addNewEquip(node: Short, room: String, floor: String, baseConfig: BaseProfileConfiguration) {
        val equip = addEquip(node)
        val configuration = equip.initializePoints(baseConfig as HyperStatCpuConfiguration, room, floor, node)
        hsHaystackUtil = equip.hsHaystackUtil
        CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "configuration returned by addNewEquip for Cpu: $configuration")
        return configuration
    }

    override fun getHyperStatEquip(node: Short): HyperStatEquip {
        return cpuDeviceMap[node] as HyperStatCpuEquip
    }

    // Run the profile logic and algorithm for an equip.
    fun processHyperStatCPUProfile(equip: HyperStatCpuEquip) {

        if (Globals.getInstance().isTestMode) {
            CcuLog.i(L.TAG_CCU_HSCPU,"Test mode is on: ${equip.nodeAddress}")
            return
        }

        if (mInterface != null) mInterface.refreshView()
        val relayStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()

        logicalPointsList = equip.getLogicalPointList()
        hsHaystackUtil = HSHaystackUtil(equip.equipRef!!, CCUHsApi.getInstance())
        handleFanConditioningModes(equip.equipRef!!)
        if(isRFDead){
            handleRFDead(equip)
            return
        } else if (isZoneDead) {
            handleDeadZone(equip)
            return
        }
        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode

        val config = equip.getConfiguration()
        val hyperStatTuners = fetchHyperStatTuners(equip)
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)

        val fanModeSaved = FanModeCacheStorage().getFanModeFromCache(equip.equipRef!!)
        val basicSettings = fetchBasicSettings(equip)
        CcuLog.d(L.TAG_CCU_HSCPU,"Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef!!, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        CcuLog.d(L.TAG_CCU_HSCPU,"After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")
        hyperstatCPUAlgorithm.initialise(tuners = hyperStatTuners)
        hyperstatCPUAlgorithm.dumpLogs()
        handleChangeOfDirection(userIntents)

        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        evaluateLoopOutputs(userIntents, basicSettings, hyperStatTuners)

        equip.hsHaystackUtil.updateOccupancyDetection()
        doorWindowSensorOpenStatus = runForDoorWindowSensor(config, equip)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(equip)
        // Updating the occupancy status happens one cycle later after update of doorWindowSensorOpenStatus.
        // Once it is detected then reset the loop output values
        if(occupancyStatus == Occupancy.WINDOW_OPEN) resetLoopOutputValues()
        runForKeyCardSensor(config, equip)
        equip.hsHaystackUtil.updateAllLoopOutput(coolingLoopOutput,heatingLoopOutput,fanLoopOutput,false,0)
        val currentOperatingMode = equip.hsHaystackUtil.getOccupancyModePointValue().toInt()

        updateTitle24LoopCounter(hyperStatTuners, basicSettings)

        CcuLog.i(L.TAG_CCU_HSCPU,
            "Analog Fan speed multiplier  ${hyperStatTuners.analogFanSpeedMultiplier} \n"+
                 "Current Working mode : ${Occupancy.values()[currentOperatingMode]} \n"+
                 "Current Temp : $currentTemp \n"+
                 "Desired Heating: ${userIntents.zoneHeatingTargetTemperature} \n"+
                 "Desired Cooling: ${userIntents.zoneCoolingTargetTemperature} \n"+
                 "Heating Loop Output: $heatingLoopOutput \n"+
                 "Cooling Loop Output:: $coolingLoopOutput \n"+
                 "Fan Loop Output:: $fanLoopOutput \n"
        )

        if (basicSettings.fanMode != StandaloneFanStage.OFF) {
            runRelayOperations(config, hyperStatTuners, userIntents, basicSettings, relayStages)
            runAnalogOutOperations(equip, config, basicSettings, analogOutStages)
        }else{
            resetAllLogicalPointValues()
        }
        setOperatingMode(currentTemp,averageDesiredTemp,basicSettings,equip)

        if (equip.hsHaystackUtil.getStatus() != curState.ordinal.toDouble())
            equip.hsHaystackUtil.setStatus(curState.ordinal.toDouble())

        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY

        CcuLog.i(L.TAG_CCU_HSCPU,"Equip Running : $curState")
        HyperStatUserIntentHandler.updateHyperStatStatus(
            equip.equipRef!!, relayStages, analogOutStages, temperatureState
        )
        updateTitle24Flags(basicSettings)
    }

    /**
     * Updates the Title 24 flags by storing the current occupancy status and fan-loop output.
     * The previous occupancy status and fan-loop output are updated for use in the next loop iteration.
     * If the fan loop counter is greater than 0, it decrements the counter.
     *
     * @param basicSettings settings containing fan mode state.
     */
    private fun updateTitle24Flags(basicSettings: BasicSettings) {
        // Store the fan-loop output/occupancy for usage in next loop
        previousOccupancyStatus = occupancyStatus
        if(occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        // Store the fan status so that when the zone goes from user defined fan state to unoccupied state, the fan protection can be offered
        previousFanStageStatus = basicSettings.fanMode
        // Store the fanloop output when the zone was not in UNOCCUPIED state
        if(fanLoopCounter == 0) previousFanLoopVal = fanLoopOutput
        if (fanLoopCounter > 0) fanLoopCounter--
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
        if((occupancyStatus != previousOccupancyStatus && fanLoopOutput < previousFanLoopVal) ||
            (basicSettings.fanMode != previousFanStageStatus && fanLoopOutput < previousFanLoopVal)) {
            fanLoopCounter = tuners.minFanRuntimePostConditioning
        }
        else if(occupancyStatus != previousOccupancyStatus && fanLoopOutput > previousFanLoopVal)
            fanLoopCounter = 0 // Reset the counter if the fan-loop output is greater than the previous value
    }

    private fun handleChangeOfDirection(userIntents: UserIntents){
        if (currentTemp > userIntents.zoneCoolingTargetTemperature && state != ZoneState.COOLING) {
            hyperstatCPUAlgorithm.resetCoolingControl()
            state = ZoneState.COOLING
            CcuLog.d(L.TAG_CCU_HSCPU,"Resetting cooling")
        } else if (currentTemp < userIntents.zoneHeatingTargetTemperature && state != ZoneState.HEATING) {
            hyperstatCPUAlgorithm.resetHeatingControl()
            state = ZoneState.HEATING
            CcuLog.d(L.TAG_CCU_HSCPU,"Resetting heating")
        }
    }

    private fun evaluateLoopOutputs(userIntents: UserIntents, basicSettings: BasicSettings, hyperStatTuners: HyperStatProfileTuners){
        when (state) {
            //Update coolingLoop when the zone is in cooling or it was in cooling and no change over happened yet.
            ZoneState.COOLING -> coolingLoopOutput = hyperstatCPUAlgorithm.calculateCoolingLoopOutput(
                currentTemp, userIntents.zoneCoolingTargetTemperature
            ).toInt().coerceAtLeast(0)

            //Update heatingLoop when the zone is in heating or it was in heating and no change over happened yet.
            ZoneState.HEATING -> heatingLoopOutput = hyperstatCPUAlgorithm.calculateHeatingLoopOutput(
                userIntents.zoneHeatingTargetTemperature, currentTemp
            ).toInt().coerceAtLeast(0)

            else -> CcuLog.d(L.TAG_CCU_HSCPU," Zone is in deadband")
        }

        if (coolingLoopOutput > 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY
                    ||basicSettings.conditioningMode == StandaloneConditioningMode.AUTO) ) {
            fanLoopOutput = ((coolingLoopOutput * hyperStatTuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
        }
        else if (heatingLoopOutput > 0  && (basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY
                    ||basicSettings.conditioningMode == StandaloneConditioningMode.AUTO)) {
            fanLoopOutput = ((heatingLoopOutput * hyperStatTuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
        }
    }

    private fun fetchBasicSettings(equip: HyperStatCpuEquip) = BasicSettings(
            conditioningMode = StandaloneConditioningMode.values()[equip.hsHaystackUtil.getCurrentConditioningMode().toInt()],
            fanMode = StandaloneFanStage.values()[equip.hsHaystackUtil.getCurrentFanMode().toInt()]
        )

    private fun fetchUserIntents(equip: HyperStatCpuEquip): UserIntents {
        return UserIntents(
            currentTemp = equip.getCurrentTemp(),
            zoneCoolingTargetTemperature = equip.hsHaystackUtil.getDesiredTempCooling(),
            zoneHeatingTargetTemperature = equip.hsHaystackUtil.getDesiredTempHeating(),
            targetMinInsideHumidity = equip.hsHaystackUtil.getTargetMinInsideHumidity(),
            targetMaxInsideHumidity = equip.hsHaystackUtil.getTargetMaxInsideHumidity(),
        )
    }

    private fun fetchHyperStatTuners(equip: HyperStatCpuEquip): HyperStatProfileTuners {

        /**
         * Consider that
         * proportionalGain = proportionalKFactor
         * integralGain = integralKFactor
         * proportionalSpread = temperatureProportionalRange
         * integralMaxTimeout = temperatureIntegralTime
         */

        val hsTuners = HyperStatProfileTuners()
        hsTuners.proportionalGain = TunerUtil.getProportionalGain(equip.equipRef!!)
        hsTuners.integralGain = TunerUtil.getIntegralGain(equip.equipRef!!)
        hsTuners.proportionalSpread = TunerUtil.getProportionalSpread(equip.equipRef!!)
        hsTuners.integralMaxTimeout = TunerUtil.getIntegralTimeout(equip.equipRef!!).toInt()
        hsTuners.relayActivationHysteresis = TunerUtil.getHysteresisPoint("relay and  activation", equip.equipRef!!).toInt()
        hsTuners.analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery("analog and fan and speed and multiplier", equip.equipRef!!)
        hsTuners.humidityHysteresis = TunerUtil.getHysteresisPoint("humidity", equip.equipRef!!).toInt()
        hsTuners.minFanRuntimePostConditioning = TunerUtil.readTunerValByQuery("fan and cur and runtime and postconditioning and min", equip.equipRef!!).toInt()
        return hsTuners
    }

    private fun runRelayOperations(
        config: HyperStatCpuConfiguration, tuner: HyperStatProfileTuners, userIntents: UserIntents,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>
    ) {
        if (config.relay1State.enabled) {
            handleRelayState(
                config.relay1State, config, Port.RELAY_ONE, 
                tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay2State.enabled) {
            handleRelayState(
                config.relay2State, config, Port.RELAY_TWO, 
                tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay3State.enabled) {
            handleRelayState(
                config.relay3State, config, Port.RELAY_THREE,
                tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay4State.enabled) {
            handleRelayState(
                config.relay4State, config, Port.RELAY_FOUR,
                tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay5State.enabled) {
            handleRelayState(
                config.relay5State, config, Port.RELAY_FIVE,
                tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay6State.enabled) {
            handleRelayState(
                config.relay6State, config, Port.RELAY_SIX,
                tuner, userIntents, basicSettings, relayStages
            )
        }
    }

    private fun runAnalogOutOperations(
        equip: HyperStatCpuEquip, config: HyperStatCpuConfiguration,
        basicSettings: BasicSettings, analogOutStages: HashMap<String, Int>
    ) {
        if (config.analogOut1State.enabled) {
            handleAnalogOutState(
                config.analogOut1State, equip, config, Port.ANALOG_OUT_ONE, basicSettings, analogOutStages
            )
        }
        if (config.analogOut2State.enabled) {
            handleAnalogOutState(
                config.analogOut2State, equip, config, Port.ANALOG_OUT_TWO, basicSettings, analogOutStages
            )
        }
        if (config.analogOut3State.enabled) {
            handleAnalogOutState(
                config.analogOut3State, equip, config, Port.ANALOG_OUT_THREE, basicSettings, analogOutStages
            )
        }
    }

    private fun handleRelayState(
        relayState: RelayState, config: HyperStatCpuConfiguration, port: Port, tuner: HyperStatProfileTuners,
        userIntents: UserIntents, basicSettings: BasicSettings, relayStages: HashMap<String, Int>
    ) {
        when {
            (HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(relayState)) -> {

                if (basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.COOL_ONLY.ordinal ||
                    basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
                ) {
                    runRelayForCooling(relayState, port, config, tuner, relayStages)
                } else {
                    resetPort(port)
                }
            }
            (HyperStatAssociationUtil.isRelayAssociatedToHeatingStage(relayState)) -> {

                if (basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.HEAT_ONLY.ordinal ||
                    basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
                ) {
                    runRelayForHeating(relayState, port, config, tuner, relayStages)
                } else {
                    resetPort(port)
                }
            }
            (HyperStatAssociationUtil.isRelayAssociatedToFan(relayState)) -> {

                if (basicSettings.fanMode != StandaloneFanStage.OFF) {
                    runRelayForFanSpeed(relayState, port, config, tuner, relayStages, basicSettings,
                        previousFanLoopVal, fanLoopCounter)
                } else {
                   resetPort(port)
                }
            }
            (HyperStatAssociationUtil.isRelayAssociatedToFanEnabled(relayState)) -> {
                doFanEnabled( curState,port, fanLoopOutput )
            }

            (HyperStatAssociationUtil.isRelayAssociatedToOccupiedEnabled(relayState)) -> {
                doOccupiedEnabled(port)
            }
            (HyperStatAssociationUtil.isRelayAssociatedToHumidifier(relayState)) -> {
                doHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMinInsideHumidity)
            }
            (HyperStatAssociationUtil.isRelayAssociatedToDeHumidifier(relayState)) -> {
                doDeHumidifierOperation(port,tuner.humidityHysteresis,userIntents.targetMaxInsideHumidity)
            }
        }
    }

    private fun runRelayForCooling(
        relayAssociation: RelayState,
        whichPort: Port,
        config: HyperStatCpuConfiguration,
        tuner: HyperStatProfileTuners,
        relayStages: HashMap<String, Int>
    ) {
        CcuLog.i(L.TAG_CCU_HSCPU," $whichPort: ${relayAssociation.association}")
        when (relayAssociation.association) {
            CpuRelayAssociation.COOLING_STAGE_1 -> {
                doCoolingStage1(
                    whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, relayStages
                )
            }
            CpuRelayAssociation.COOLING_STAGE_2 -> {
                val highestStage = HyperStatAssociationUtil.getHighestCoolingStage(config).ordinal
                val divider = if (highestStage == 1) 50 else 33
               doCoolingStage2(
                   whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, divider,relayStages
               )
            }
            CpuRelayAssociation.COOLING_STAGE_3 -> {
                doCoolingStage3(
                    whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, relayStages
                )
            }
            else -> {}
        }
        if(getCurrentPortStatus(whichPort) == 1.0)
            curState = ZoneState.COOLING

    }

    private fun runRelayForHeating(
        relayAssociation: RelayState, whichPort: Port, config: HyperStatCpuConfiguration,
        tuner: HyperStatProfileTuners, relayStages: HashMap<String, Int>
    ) {
        CcuLog.i(L.TAG_CCU_HSCPU," $whichPort: ${relayAssociation.association}")
        when (relayAssociation.association) {
            CpuRelayAssociation.HEATING_STAGE_1 -> {
                doHeatingStage1(
                    whichPort,
                    heatingLoopOutput,
                    tuner.relayActivationHysteresis,
                    relayStages
                )
            }
            CpuRelayAssociation.HEATING_STAGE_2 -> {
                val highestStage = HyperStatAssociationUtil.getHighestHeatingStage(config).ordinal
                val divider = if (highestStage == 4) 50 else 33
                doHeatingStage2(
                    whichPort, heatingLoopOutput, tuner.relayActivationHysteresis,
                    divider, relayStages)
            }
            CpuRelayAssociation.HEATING_STAGE_3 -> {
                doHeatingStage3(
                    whichPort, heatingLoopOutput, tuner.relayActivationHysteresis, relayStages)
            }
            else -> {}
        }
        if(getCurrentPortStatus(whichPort) == 1.0)
            curState = ZoneState.HEATING
    }

    private fun runRelayForFanSpeed(
        relayAssociation: RelayState, whichPort: Port, config: HyperStatCpuConfiguration,
        tuner: HyperStatProfileTuners, relayStages: HashMap<String, Int>, basicSettings: BasicSettings,
        previousFanLoopVal: Int, fanProtectionCounter: Int
    ) {
        var localFanLoopOutput = fanLoopOutput
        CcuLog.i(L.TAG_CCU_HSCPU," $whichPort: ${relayAssociation.association} runRelayForFanSpeed: ${basicSettings.fanMode}")
        if (basicSettings.fanMode == StandaloneFanStage.AUTO
            && basicSettings.conditioningMode == StandaloneConditioningMode.OFF ) {
            CcuLog.d(L.TAG_CCU_HSCPU,"Cond is Off , Fan is Auto  : ")
            resetPort(whichPort)
            return
        }
        val highestStage = HyperStatAssociationUtil.getHighestFanStage(config)
        val divider = if (highestStage == CpuRelayAssociation.FAN_MEDIUM_SPEED) 50 else 33
        val fanEnabledMapped = HyperStatAssociationUtil.isAnyRelayAssociatedToFanEnabled(config)
        val lowestStage = HyperStatAssociationUtil.getLowestFanStage(config)

        if (fanEnabledMapped) setFanEnabledStatus(true) else setFanEnabledStatus(false)

        setFanLowestFanLowStatus(false)
        setFanLowestFanMediumStatus(false)
        setFanLowestFanHighStatus(false)
        when(lowestStage) {
            CpuRelayAssociation.FAN_LOW_SPEED -> setFanLowestFanLowStatus(true)
            CpuRelayAssociation.FAN_MEDIUM_SPEED -> setFanLowestFanMediumStatus(true)
            CpuRelayAssociation.FAN_HIGH_SPEED -> setFanLowestFanHighStatus(true)
            else -> {
                // Do nothing
            }
        }

        // In order to protect the fan, persist the fan for few cycles when there is a sudden change in
        // occupancy and decrease in fan loop output
        if (fanProtectionCounter > 0) {
            localFanLoopOutput = previousFanLoopVal
        }

        when (relayAssociation.association) {
            CpuRelayAssociation.FAN_LOW_SPEED -> {
                doFanLowSpeed(
                    logicalPointsList[whichPort]!!,null,null, basicSettings.fanMode,
                    localFanLoopOutput,tuner.relayActivationHysteresis,relayStages,divider,getDoorWindowFanOperationStatus())
            }
            CpuRelayAssociation.FAN_MEDIUM_SPEED -> {
                doFanMediumSpeed(
                    logicalPointsList[whichPort]!!,null,basicSettings.fanMode,
                    localFanLoopOutput,tuner.relayActivationHysteresis,divider,relayStages,getDoorWindowFanOperationStatus())
            }
            CpuRelayAssociation.FAN_HIGH_SPEED -> {
                doFanHighSpeed(
                    logicalPointsList[whichPort]!!,basicSettings.fanMode,
                    localFanLoopOutput,tuner.relayActivationHysteresis,relayStages,getDoorWindowFanOperationStatus())
            }
            else -> return
        }
    }

    private fun handleAnalogOutState(
        analogOutState: AnalogOutState, equip: HyperStatCpuEquip, config: HyperStatCpuConfiguration,
        port: Port, basicSettings: BasicSettings, analogOutStages: HashMap<String, Int>
    ) {
        // If we are in Auto Away mode we no need to Any analog Operations
        when {
            (HyperStatAssociationUtil.isAnalogOutAssociatedToCooling(analogOutState)) -> {
                doAnalogCooling(port,basicSettings.conditioningMode,analogOutStages,coolingLoopOutput)
            }
            (HyperStatAssociationUtil.isAnalogOutAssociatedToHeating(analogOutState)) -> {
                doAnalogHeating(port,basicSettings.conditioningMode,analogOutStages,heatingLoopOutput)
            }
            (HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState)) -> {
                doAnalogFanActionCpu(
                    port, analogOutState.perAtFanLow.toInt(), analogOutState.perAtFanMedium.toInt(),
                    analogOutState.perAtFanHigh.toInt(), basicSettings.fanMode,
                    basicSettings.conditioningMode, fanLoopOutput, analogOutStages, previousFanLoopVal, fanLoopCounter
                )
            }
            (HyperStatAssociationUtil.isAnalogOutAssociatedToDcvDamper(analogOutState)) -> {
                doAnalogDCVAction(
                    port,analogOutStages,config.zoneCO2Threshold,config.zoneCO2DamperOpeningRate,isDoorOpenState(config,equip)
                )
            }
            (HyperStatAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(analogOutState)) -> {
                doAnalogStagedFanAction(
                    port, analogOutState.perAtFanLow.toInt(), analogOutState.perAtFanMedium.toInt(),
                    analogOutState.perAtFanHigh.toInt(), basicSettings.fanMode, HyperStatAssociationUtil.isAnyRelayAssociatedToFanEnabled(config),
                    basicSettings.conditioningMode, fanLoopOutput, analogOutStages, fanLoopCounter
                )
            }
        }
    }

    private fun getCoolingActivatedAnalogVoltage(fanEnabledMapped: Boolean, fanLoopOutput: Int): Int {
        var voltage = getPercentageFromVoltageSelected(getCoolingStateActivated().roundToInt())
        // For title 24 compliance, check if fanEnabled is mapped, then set the fanloopForAnalog to the lowest cooling state activated
        // and check if staged fan is inactive(fanLoopForAnalog == 0)
        if(fanEnabledMapped && voltage == 0 && fanLoopOutput > 0) {
            voltage = getPercentageFromVoltageSelected(getLowestCoolingStateActivated().roundToInt())
        }
        return voltage
    }


    private fun getHeatingActivatedAnalogVoltage(fanEnabledMapped: Boolean, fanLoopOutput: Int): Int {
        var voltage = getPercentageFromVoltageSelected(getHeatingStateActivated().roundToInt())
        // For title 24 compliance, check if fanEnabled is mapped, then set the fanloopForAnalog to the lowest heating state activated
        // and check if staged fan is inactive(fanLoopForAnalog == 0)
        if (fanEnabledMapped && voltage == 0 && fanLoopOutput > 0) {
            voltage = getPercentageFromVoltageSelected(getLowestHeatingStateActivated().roundToInt())
        }
        return voltage
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
        fanProtectionCounter: Int
    ) {
        if (fanMode != StandaloneFanStage.OFF) {
            var fanLoopForAnalog = 0
            if (fanMode == StandaloneFanStage.AUTO) {
                if (conditioningMode == StandaloneConditioningMode.OFF) {
                    updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
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
            if (fanLoopForAnalog > 0) analogOutStages[AnalogOutput.FAN_SPEED.name] =
                fanLoopForAnalog
            updateLogicalPointIdValue(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
            CcuLog.i(L.TAG_CCU_HSCPU, "$port = Linear Fan Speed  analogSignal   $fanLoopForAnalog")
        }
    }

    /**
     * Check if currently in any of the occupied mode
     *
     * @return true if in occupied,forced occupied etc, else false.
     */
    private fun checkIfInOccupiedMode(): Boolean {
        return  occupancyStatus != Occupancy.UNOCCUPIED &&
                occupancyStatus != Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
                occupancyStatus != Occupancy.VACATION
    }

     private fun doAnalogStagedFanAction(
        port: Port,
        fanLowPercent: Int,
        fanMediumPercent: Int,
        fanHighPercent: Int,
        fanMode: StandaloneFanStage,
        fanEnabledMapped: Boolean,
        conditioningMode: StandaloneConditioningMode,
        fanLoopOutput: Int,
        analogOutStages: HashMap<String, Int>,
        fanProtectionCounter: Int
    ) {
        var fanLoopForAnalog = 0
        var logMsg = "" // This will be overwritten based on priority

        if (fanMode != StandaloneFanStage.OFF) {
            if (fanMode == StandaloneFanStage.AUTO) {
                if (conditioningMode == StandaloneConditioningMode.OFF) {
                    updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
                    return
                }
                fanLoopForAnalog = fanLoopOutput
                if (conditioningMode == StandaloneConditioningMode.AUTO) {
                    if (getOperatingMode() == 1.0) {
                        fanLoopForAnalog = getCoolingActivatedAnalogVoltage(fanEnabledMapped, fanLoopOutput)
                        logMsg = "Cooling"
                    } else if (getOperatingMode() == 2.0) {
                        fanLoopForAnalog = getHeatingActivatedAnalogVoltage(fanEnabledMapped, fanLoopOutput)
                        logMsg = "Heating"
                    }
                } else if (conditioningMode == StandaloneConditioningMode.COOL_ONLY) {
                    fanLoopForAnalog = getCoolingActivatedAnalogVoltage(fanEnabledMapped, fanLoopOutput)
                    logMsg = "Cooling"
                } else if (conditioningMode == StandaloneConditioningMode.HEAT_ONLY) {
                    fanLoopForAnalog = getHeatingActivatedAnalogVoltage(fanEnabledMapped, fanLoopOutput)
                    logMsg = "Heating"
                }
                // Check if we need fan protection
                if(fanProtectionCounter > 0  && fanLoopForAnalog < previousFanLoopValStaged) {
                    fanLoopForAnalog = previousFanLoopValStaged
                    logMsg = "Fan Protection"
                } else {
                    // else indicates we are not in protection mode, so store the fanLoopForAnalog value for protection mdoe
                    previousFanLoopValStaged = fanLoopForAnalog
                }
                // When in dead-band, set the fan-loopForAnalog to the recirculate analog value. Also ensure fan protection is not ON
                if ((fanLoopOutput == 0 || getDoorWindowFanOperationStatus()) && fanProtectionCounter == 0 && checkIfInOccupiedMode()) {
                    fanLoopForAnalog = getPercentageFromVoltageSelected(getAnalogRecirculateValueActivated().roundToInt())
                    logMsg = "Deadband"
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
                // Store just in case when we switch from current occupied to unoccupied mode
                previousFanLoopValStaged = fanLoopForAnalog
            }
            if (fanLoopForAnalog > 0) analogOutStages[AnalogOutput.FAN_SPEED.name] =
                fanLoopForAnalog
            updateLogicalPointIdValue(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
            CcuLog.i(L.TAG_CCU_HSCPU, "$port = Staged Fan Speed($logMsg)  analogSignal  $fanLoopForAnalog")
        }
    }

    private fun getOperatingMode(): Double {

        return hsHaystackUtil.readHisVal("point and operating and mode")

    }

    private fun handleDeadZone(equip: HyperStatCpuEquip) {

       CcuLog.d(L.TAG_CCU_HSCPU,"Zone is Dead ${equip.node}")
        state = ZoneState.TEMPDEAD
        resetAllLogicalPointValues(equip)
        equip.hsHaystackUtil.setProfilePoint("operating and mode", state.ordinal.toDouble())
        if (equip.hsHaystackUtil.getEquipStatus() != state.ordinal.toDouble())
            equip.hsHaystackUtil.setEquipStatus(state.ordinal.toDouble())
        val curStatus = equip.hsHaystackUtil.getEquipLiveStatus()
        if (curStatus != ZoneTempDead) {
            equip.hsHaystackUtil.writeDefaultVal("status and message and writable", ZoneTempDead)
        }
        hyperStatStatus[equip.equipRef.toString()] = ZoneTempDead
        equip.haystack.writeHisValByQuery(
            "point and not ota and status and his and group == \"${equip.node}\"",
            ZoneState.TEMPDEAD.ordinal.toDouble()
        )
    }
    private fun handleRFDead(equip: HyperStatCpuEquip) {
        CcuLog.d(L.TAG_CCU_HSCPU,"RF Signal is Dead ${equip.node}")
        state = ZoneState.RFDEAD
        equip.hsHaystackUtil.setProfilePoint("operating and mode", state.ordinal.toDouble())
        if (equip.hsHaystackUtil.getEquipStatus() != state.ordinal.toDouble()) {
            equip.hsHaystackUtil.setEquipStatus(state.ordinal.toDouble())
        }
        val curStatus = equip.hsHaystackUtil.getEquipLiveStatus()
        if (curStatus != RFDead) {
            equip.hsHaystackUtil.writeDefaultVal("status and message and writable", RFDead)
        }
        hyperStatStatus[equip.equipRef.toString()] = RFDead
        equip.haystack.writeHisValByQuery(
            "point and not ota and status and his and group == \"${equip.node}\"",
            ZoneState.RFDEAD.ordinal.toDouble()
        )
    }

    /**
     * Check for the flag which allows fan operation during door window
     * @return true or false
     */
    private fun getDoorWindowFanOperationStatus(): Boolean {
        return runFanLowDuringDoorWindow
    }

    /**
     * Check if we should allow fan operation even when the door window is open
     * @param equip HyperStatCpuEquip
     * @return true if the door or window is open and the occupancy status is not UNOCCUPIED, otherwise false.
     */
    private fun checkFanOperationAllowedDoorWindow(equip: HyperStatCpuEquip): Boolean {
        return if(currentTemp < fetchUserIntents(equip).zoneCoolingTargetTemperature && currentTemp > fetchUserIntents(equip).zoneHeatingTargetTemperature) {
            doorWindowSensorOpenStatus &&
                    occupancyBeforeDoorWindow != Occupancy.UNOCCUPIED &&
                    occupancyBeforeDoorWindow != Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
                    occupancyBeforeDoorWindow != Occupancy.VACATION
        } else {
            doorWindowSensorOpenStatus
        }
    }

    private fun runForDoorWindowSensor(config: HyperStatCpuConfiguration, equip: HyperStatCpuEquip): Boolean {
        val isDoorOpen = isDoorOpenState(config,equip)
       CcuLog.i(L.TAG_CCU_HSCPU," is Door Open ? $isDoorOpen")
        return isDoorOpen
    }
    
    private fun isDoorOpenState(config: HyperStatCpuConfiguration, equip: HyperStatCpuEquip): Boolean{

        // If thermistor value less than 10000 ohms door is closed (0) else door is open (1)
        // If analog in value is less than 2v door is closed(0) else door is open (1)
        var isDoorOpen = false

        var th2SensorEnabled = false
        var analog1SensorEnabled = false
        var analog2SensorEnabled = false

        // Thermistor 2 is always mapped to door window sensor
        if (isDoorWindowSensorOnTh2(config)) {
            val sensorValue = equip.hsHaystackUtil.getSensorPointValue(
                "door and window and logical and sensor"
            )
           CcuLog.d(L.TAG_CCU_HSCPU,"TH2 Door Window sensor value : Door is $sensorValue")
            if (sensorValue.toInt() == 1) isDoorOpen = true
            th2SensorEnabled = true
        }

        if (config.analogIn1State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToDoorWindowSensor(config.analogIn1State)
        ) {
            val sensorValue = equip.hsHaystackUtil.getSensorPointValue(
                "door and window2 and logical and sensor"
            )
           CcuLog.d(L.TAG_CCU_HSCPU,"Analog In 1 Door Window sensor value : Door is $sensorValue")
            if (sensorValue.toInt() == 1) isDoorOpen = true
            analog1SensorEnabled = true
        }

        if (config.analogIn2State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToDoorWindowSensor(config.analogIn2State)
        ) {
            val sensorValue = equip.hsHaystackUtil.getSensorPointValue(
                "door and window3 and logical and sensor"
            )
           CcuLog.d(L.TAG_CCU_HSCPU,"Analog In 2 Door Window sensor value : Door is $sensorValue")
            if (sensorValue.toInt() == 1) isDoorOpen = true
            analog2SensorEnabled = true
        }
        doorWindowIsOpen(
            if (th2SensorEnabled || analog1SensorEnabled || analog2SensorEnabled) 1.0 else 0.0,
            if (isDoorOpen) 1.0 else 0.0
        )
        return isDoorOpen
    }

    private fun isDoorWindowSensorOnTh2(config: HyperStatCpuConfiguration): Boolean {
        return config.thermistorIn2State.enabled && config.thermistorIn2State.association.equals(Th2InAssociation.DOOR_WINDOW_SENSOR)
    }

    private fun resetLoopOutputValues() {
       CcuLog.d(L.TAG_CCU_HSCPU,"Resetting all the loop output values: ")
        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
    }

    private fun runForKeyCardSensor(config: HyperStatCpuConfiguration, equip: HyperStatCpuEquip) {

        var analog1KeycardEnabled = false
        var analog2KeycardEnabled = false

        var analog1Sensor = 0.0
        var analog2Sensor = 0.0

        if (config.analogIn1State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToKeyCardSensor(config.analogIn1State)
        ) {
            analog1KeycardEnabled = true
            analog1Sensor = equip.hsHaystackUtil.getSensorPointValue("keycard and sensor and logical")
        }

        if (config.analogIn2State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToKeyCardSensor(config.analogIn2State)
        ) {
            analog2KeycardEnabled = true
            analog2Sensor = equip.hsHaystackUtil.getSensorPointValue("keycard2 and sensor and logical")
        }

        CcuLog.d(L.TAG_CCU_HSCPU,"Keycard Enable Value "+  if (analog1KeycardEnabled || analog2KeycardEnabled) 1.0 else 0.0)
        CcuLog.d(L.TAG_CCU_HSCPU,"Keycard sensor Value "+ if (analog1Sensor > 0 || analog2Sensor > 0) 1.0 else 0.0)

        keyCardIsInSlot(
            if (analog1KeycardEnabled || analog2KeycardEnabled) 1.0 else 0.0,
            if (analog1Sensor > 0 || analog2Sensor > 0) 1.0 else 0.0
        )
    }

    override fun getEquip(): Equip? {
        for (nodeAddress in cpuDeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    @JsonIgnore
    override fun getNodeAddresses(): Set<Short?> {
        return cpuDeviceMap.keys
    }

    private fun resetAllLogicalPointValues(equip: HyperStatCpuEquip) {
        equip.hsHaystackUtil.updateAllLoopOutput(0,0,0,false,0)
        resetAllLogicalPointValues()
        HyperStatUserIntentHandler.updateHyperStatStatus(
            equipId = equip.equipRef!!,
            portStages = HashMap(),
            analogOutStages = HashMap(),
            temperatureState = ZoneTempState.TEMP_DEAD
        )
    }

    @JsonIgnore
    override fun getCurrentTemp(): Double {
        for (nodeAddress in cpuDeviceMap.keys) {
            return cpuDeviceMap[nodeAddress]!!.getCurrentTemp()
        }
        return 0.0
    }

    @JsonIgnore
    override fun getDisplayCurrentTemp(): Double {
        return averageZoneTemp
    }

    @JsonIgnore
    override fun getAverageZoneTemp(): Double {
        var tempTotal = 0.0
        var nodeCount = 0
        for (nodeAddress in cpuDeviceMap.keys) {
            if (cpuDeviceMap[nodeAddress] == null) {
                continue
            }
            if (cpuDeviceMap[nodeAddress]!!.getCurrentTemp() > 0) {
                tempTotal += cpuDeviceMap[nodeAddress]!!.getCurrentTemp()
                nodeCount++
            }
        }
        return if (nodeCount == 0) 0.0 else tempTotal / nodeCount
    }

    private fun getCoolingStateActivated (): Double {
        return if (stageActive("cooling and runtime and stage3")) {
            hsHaystackUtil.readPointValue("fan and cooling and stage3")
        } else if (stageActive("cooling and runtime and stage2")) {
            hsHaystackUtil.readPointValue("fan and cooling and stage2")
        } else if (stageActive("cooling and runtime and stage1")) {
            hsHaystackUtil.readPointValue("fan and cooling and stage1")
        } else {
            defaultFanLoopOutput
        }
    }


    private fun getHeatingStateActivated (): Double {
        return if (stageActive("heating and runtime and stage3")) {
            hsHaystackUtil.readPointValue("fan and heating and stage3")
        } else if (stageActive("heating and runtime and stage2")) {
            hsHaystackUtil.readPointValue("fan and heating and stage2")
        } else if (stageActive("heating and runtime and stage1")){
            hsHaystackUtil.readPointValue("fan and heating and stage1")
        } else {
            defaultFanLoopOutput
        }
    }

    /**
     * Returns the value of the non zero activated analog recirculate stage based on the fan loop points.
     * If no recirculate stage is activated, returns the default fan loop output value.
     *
     * @return the value of the first non zero activated recirculate stage or the default fan loop output value.
     */
    private fun getAnalogRecirculateValueActivated (): Double {
        return if (hsHaystackUtil.readPointValue("recirculate and analog1") != 0.0) {
            hsHaystackUtil.readPointValue("recirculate and analog1")
        } else if (hsHaystackUtil.readPointValue("recirculate and analog2") != 0.0) {
            hsHaystackUtil.readPointValue("recirculate and analog2")
        } else if (hsHaystackUtil.readPointValue("recirculate and analog3") != 0.0) {
            hsHaystackUtil.readPointValue("recirculate and analog3")
        } else {
            defaultFanLoopOutput
        }
    }

    /**
     * Returns the value of the lowest activated cooling stage based on the fan loop points.
     * If no cooling stage is activated, returns the default fan loop output value.
     *
     * @return the value of the lowest activated cooling stage or the default fan loop output value.
     */
    private fun getLowestCoolingStateActivated (): Double {
        return if (hsHaystackUtil.readPointValue("fan and cooling and stage1") != 0.0) {
            hsHaystackUtil.readPointValue("fan and cooling and stage1")
        } else if (hsHaystackUtil.readPointValue("fan and cooling and stage2") != 0.0) {
            hsHaystackUtil.readPointValue("fan and cooling and stage2")
        } else if (hsHaystackUtil.readPointValue("fan and cooling and stage3") != 0.0) {
            hsHaystackUtil.readPointValue("fan and cooling and stage3")
        } else {
            defaultFanLoopOutput
        }
    }

    /**
     * Returns the value of the lowest activated heating stage based on the fan loop points.
     * If no heating stage is activated, returns the default fan loop output value.
     *
     * @return the value of the lowest activated heating stage or the default fan loop output value.
     */
    private fun getLowestHeatingStateActivated (): Double {
        return if (hsHaystackUtil.readPointValue("fan and heating and stage1") != 0.0) {
            hsHaystackUtil.readPointValue("fan and heating and stage1")
        } else if (hsHaystackUtil.readPointValue("fan and heating and stage2") != 0.0) {
            hsHaystackUtil.readPointValue("fan and heating and stage2")
        } else if (hsHaystackUtil.readPointValue("fan and heating and stage3") != 0.0) {
            hsHaystackUtil.readPointValue("fan and heating and stage3")
        } else {
            defaultFanLoopOutput
        }
    }

    private fun stageActive(fanStage: String): Boolean {
        return hsHaystackUtil.readHisVal(fanStage) == 1.0
    }

    private fun getPercentageFromVoltageSelected(voltageSelected: Int): Int {
        val minVoltage = 0
        val maxVoltage = 10
        return (((voltageSelected - minVoltage).toDouble() / (maxVoltage - minVoltage)) * 100).roundToInt()
    }
}