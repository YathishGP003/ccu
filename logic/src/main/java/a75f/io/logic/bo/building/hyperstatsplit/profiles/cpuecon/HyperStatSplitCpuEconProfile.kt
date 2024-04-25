package a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HSUtil
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.EpidemicState
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstatsplit.common.BasicSettings
import a75f.io.logic.bo.building.hyperstatsplit.common.FanModeCacheStorage
import a75f.io.logic.bo.building.hyperstatsplit.common.HSSplitHaystackUtil
import a75f.io.logic.bo.building.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getActualFanMode
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitEquip
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitProfileTuners
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperstatSplitLoopController
import a75f.io.logic.bo.building.hyperstatsplit.common.UserIntents
import a75f.io.logic.bo.building.hyperstatsplit.profiles.HyperStatSplitPackageUnitProfile
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.schedules.OccupancyHandler
import a75f.io.logic.bo.building.schedules.occupancy.OccupancyUtil
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.logic.jobs.HyperStatSplitUserIntentHandler
import a75f.io.logic.tuners.TunerUtil
import a75f.io.logic.util.PreferenceUtil
import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnore
import kotlin.math.roundToInt


/**
 * @author tcase@75f.io (HyperStat CPU)
 * Created on 7/7/21.
 *
 * Created for HyperStat Split CPU/Econ by Nick P on 07-24-2023.
 */
class HyperStatSplitCpuEconProfile : HyperStatSplitPackageUnitProfile() {

    companion object {

        fun getAirEnthalpy(temp: Double, humidity: Double): Double {

            val A = 0.007468 * Math.pow(temp,2.0) - 0.4344 * temp + 11.176
            val B = 0.2372 * temp + 0.1230;
            val H = A * 0.01 * humidity + B;

            Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "temperature "+temp+" humidity "+humidity+" Enthalpy: "+H);
            return CCUUtils.roundToTwoDecimal(H)

        }

    }

    // One zone can have many hyperstat devices.  Each has its own address and equip representation
    private val cpuEconDeviceMap: MutableMap<Short, HyperStatSplitCpuEconEquip> = mutableMapOf()

    private var wasCondensateTripped = false

    private var coolingLoopOutput = 0
    private var heatingLoopOutput = 0
    private var fanLoopOutput = 0
    private val defaultFanLoopOutput = 0.0
    private var economizingLoopOutput = 0
    private var dcvLoopOutput = 0
    private var outsideAirCalculatedMinDamper = 0
    private var outsideAirLoopOutput = 0
    private var outsideAirFinalLoopOutput = 0
    override lateinit var occupancyStatus: Occupancy

    // Flags for keeping tab of occupancy during linear fan operation(Only to be used in doAnalogFanActionCpu())
    private var previousOccupancyStatus: Occupancy = Occupancy.NONE
    private var previousFanStageStatus: StandaloneFanStage = StandaloneFanStage.OFF
    private var previousFanLoopVal = 0
    private var previousFanLoopValStaged = 0
    private var fanLoopCounter = 0

    private val hyperstatSplitCPUEconAlgorithm = HyperstatSplitLoopController()

    private var economizingAvailable = false
    private var dcvAvailable = false
    private var matThrottle = false

    lateinit var curState: ZoneState
    private var epidemicState = EpidemicState.OFF
    private var prePurgeEnabled = false
    private var prePurgeOpeningValue = 0.0

    override fun getProfileType() = ProfileType.HYPERSTATSPLIT_CPU

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        val equip = cpuEconDeviceMap[address]
        return equip?.getConfiguration() as T
    }

    override fun updateZonePoints() {
        cpuEconDeviceMap.forEach { (_, equip) ->
            logIt("Process Equip: equipRef =  ${equip.equipRef}")
            processHyperStatSplitCPUEconProfile(equip)
        }
    }

    fun addEquip(node: Short): HyperStatSplitEquip {
        val equip = HyperStatSplitCpuEconEquip(node)
        equip.initEquipReference(node)
        cpuEconDeviceMap[node] = equip
        return equip
    }

    override fun addNewEquip(node: Short, room: String, floor: String, baseConfig: BaseProfileConfiguration) {

        val equip = addEquip(node)

        val configuration = equip.initializePoints(baseConfig as HyperStatSplitCpuEconConfiguration, room, floor, node)
        hsSplitHaystackUtil = equip.hsSplitHaystackUtil

        return configuration
    }

    override fun getHyperStatSplitEquip(node: Short): HyperStatSplitEquip {
        return cpuEconDeviceMap[node] as HyperStatSplitCpuEconEquip
    }

    // Run the profile logic and algorithm for an equip.
    fun processHyperStatSplitCPUEconProfile(equip: HyperStatSplitCpuEconEquip) {

        if (Globals.getInstance().isTestMode) {
            logIt("Test mode is on: ${equip.nodeAddress}")
            return
        }

        if (mInterface != null) mInterface.refreshView()
        val relayStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()
        logicalPointsList = equip.getLogicalPointList()

        hsSplitHaystackUtil = HSSplitHaystackUtil(equip.equipRef!!, CCUHsApi.getInstance())
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
        val hyperStatSplitTuners = fetchHyperStatSplitTuners(equip)
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)

        val exhaustFanStage1Threshold = hsSplitHaystackUtil.getExhaustFanStage1Threshold().toInt()
        val exhaustFanStage2Threshold = hsSplitHaystackUtil.getExhaustFanStage2Threshold().toInt()
        val exhaustFanHysteresis = hsSplitHaystackUtil.getExhaustFanHysteresis().toInt()
        val outsideDamperMinOpen = getEffectiveOutsideDamperMinOpen()

        val fanModeSaved = FanModeCacheStorage().getFanModeFromCache(equip.equipRef!!)
        val actualFanMode = getActualFanMode(equip.node.toString(), fanModeSaved)

        val isCondensateTripped : Boolean = hsSplitHaystackUtil.getCondensateOverflowStatus() > 0.0
        if (isCondensateTripped) Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "Condensate overflow detected")

        // At this point, Conditioning Mode will be set to OFF if Condensate Overflow is detected
        // It will revert to previous value when Condensate returns to normal
        val basicSettings = fetchBasicSettings(equip, wasCondensateTripped, isCondensateTripped)

        val updatedFanMode = fallBackFanMode(equip, equip.equipRef!!, fanModeSaved, actualFanMode, basicSettings)
        basicSettings.fanMode = updatedFanMode

        hyperstatSplitCPUEconAlgorithm.initialise(tuners = hyperStatSplitTuners)
        hyperstatSplitCPUEconAlgorithm.dumpLogs()
        handleChangeOfDirection(userIntents)

        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        economizingLoopOutput = 0
        dcvLoopOutput = 0
        outsideAirCalculatedMinDamper = 0
        outsideAirLoopOutput = 0
        outsideAirFinalLoopOutput = 0
        evaluateLoopOutputs(equip, userIntents, basicSettings, hyperStatSplitTuners, isCondensateTripped, outsideDamperMinOpen)

        equip.hsSplitHaystackUtil.updateOccupancyDetection()
        equip.hsSplitHaystackUtil.updateConditioningLoopOutput(coolingLoopOutput,heatingLoopOutput,fanLoopOutput,false,0)
        equip.hsSplitHaystackUtil.updateOaoLoopOutput(economizingLoopOutput, dcvLoopOutput, outsideAirLoopOutput, outsideAirFinalLoopOutput)

        val currentOperatingMode = equip.hsSplitHaystackUtil.getOccupancyModePointValue().toInt()

        updateTitle24LoopCounter(hyperStatSplitTuners, basicSettings)
        prePurgeEnabled = hsSplitHaystackUtil.isPrePurgeEnabled()
        prePurgeOpeningValue = TunerUtil.readTunerValByQuery(
            "prePurge and oao and fan and cur and speed and cpu and standalone and zone",
            equip.equipRef
        )

        logIt(
            "Analog Fan speed multiplier  ${hyperStatSplitTuners.analogFanSpeedMultiplier} \n"+
                 "Current Working mode : ${Occupancy.values()[currentOperatingMode]} \n"+
                 "Current Temp : $currentTemp \n"+
                 "Desired Heating: ${userIntents.zoneHeatingTargetTemperature} \n"+
                 "Desired Cooling: ${userIntents.zoneCoolingTargetTemperature} \n"+
                 "Heating Loop Output: $heatingLoopOutput \n"+
                 "Cooling Loop Output:: $coolingLoopOutput \n"+
                 "Fan Loop Output:: $fanLoopOutput \n"+
                 "Economizing Loop Output:: $economizingLoopOutput \n"+
                 "DCV Loop Output:: $dcvLoopOutput \n"+
                 "Calculated Min OAO Damper:: $outsideAirCalculatedMinDamper \n"+
                 "OAO Loop Output (before MAT Safety):: $outsideAirLoopOutput \n"+
                 "OAO Loop Output (after MAT Safety and outsideDamperMinOpen):: $outsideAirFinalLoopOutput \n"
        )

        if (basicSettings.fanMode != StandaloneFanStage.OFF) {
            runRelayOperations(config, hyperStatSplitTuners, userIntents, basicSettings, relayStages, isCondensateTripped, exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis)
            runAnalogOutOperations(equip, config, basicSettings, analogOutStages, isCondensateTripped)
        } else{
            resetAllLogicalPointValues()
        }
        setOperatingMode(currentTemp,averageDesiredTemp,basicSettings,equip)

        if (equip.hsSplitHaystackUtil.getStatus() != curState.ordinal.toDouble())
            equip.hsSplitHaystackUtil.setStatus(curState.ordinal.toDouble())

        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY

        logIt("Equip Running : $curState")

        HyperStatSplitUserIntentHandler.updateHyperStatSplitStatus(
            equip.equipRef!!, relayStages, analogOutStages, temperatureState,
            economizingLoopOutput, dcvLoopOutput, outsideDamperMinOpen, outsideAirFinalLoopOutput,
            equip.hsSplitHaystackUtil.getCondensateOverflowStatus(), equip.hsSplitHaystackUtil.getFilterStatus(), basicSettings,
            epidemicState
        )

        wasCondensateTripped = isCondensateTripped

        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "processHyperStatSplitCpuEconProfile() complete")
        updateTitle24Flags(basicSettings)
    }

    private fun getEffectiveOutsideDamperMinOpen(): Int {
        var outsideDamperMinOpenFromConditioning : Double
        if (isHeatingActive() || isCoolingActive() || hsSplitHaystackUtil.getEconomizingLoopOutput() > 0.0) {
            outsideDamperMinOpenFromConditioning = hsSplitHaystackUtil.getOutsideDamperMinOpenDuringConditioning()
        } else {
            outsideDamperMinOpenFromConditioning = hsSplitHaystackUtil.getOutsideDamperMinOpenDuringRecirc()
        }

        var outsideDamperMinOpenFromFanStage : Double = 0.0
        if (stageActive("fan and speed and high and cmd and not config")
            || hsSplitHaystackUtil.getCurrentFanMode().toInt().equals(StandaloneFanStage.HIGH_ALL_TIME.ordinal)
            || hsSplitHaystackUtil.getCurrentFanMode().toInt().equals(StandaloneFanStage.HIGH_CUR_OCC.ordinal)
            || hsSplitHaystackUtil.getCurrentFanMode().toInt().equals(StandaloneFanStage.HIGH_OCC.ordinal)
        ) {
            outsideDamperMinOpenFromFanStage = hsSplitHaystackUtil.getOutsideDamperMinOpenDuringFanHigh()
        } else if (stageActive("fan and speed and medium and cmd and not config")
            || hsSplitHaystackUtil.getCurrentFanMode().toInt().equals(StandaloneFanStage.MEDIUM_ALL_TIME.ordinal)
            || hsSplitHaystackUtil.getCurrentFanMode().toInt().equals(StandaloneFanStage.MEDIUM_CUR_OCC.ordinal)
            || hsSplitHaystackUtil.getCurrentFanMode().toInt().equals(StandaloneFanStage.MEDIUM_OCC.ordinal)
        ) {
            outsideDamperMinOpenFromFanStage = hsSplitHaystackUtil.getOutsideDamperMinOpenDuringFanMedium()
        } else if (stageActive("fan and speed and low and cmd and not config")
            || hsSplitHaystackUtil.getCurrentFanMode().toInt().equals(StandaloneFanStage.LOW_ALL_TIME.ordinal)
            || hsSplitHaystackUtil.getCurrentFanMode().toInt().equals(StandaloneFanStage.LOW_CUR_OCC.ordinal)
            || hsSplitHaystackUtil.getCurrentFanMode().toInt().equals(StandaloneFanStage.LOW_OCC.ordinal)
        ) {
            outsideDamperMinOpenFromFanStage = hsSplitHaystackUtil.getOutsideDamperMinOpenDuringFanLow()
        }

        val zoneId = HSUtil.getZoneIdFromEquipId(equip?.id)
        val occ = ScheduleManager.getInstance().getOccupiedModeCache(zoneId)
        val isOccupied = if (occ == null) false else occ.isOccupied()
        if (isOccupied) {
            return Math.max(outsideDamperMinOpenFromConditioning.toInt(), outsideDamperMinOpenFromFanStage.toInt())
        } else {
            return 0
        }

    }

    /**
     * Updates the Title 24 flags by storing the current occupancy status and fan-loop output.
     * The previous occupancy status and fan-loop output are updated for use in the next loop iteration.
     * If the fan loop counter is greater than 0, it decrements the counter.
     */
    private fun updateTitle24Flags(basicSettings: BasicSettings) {
        // Store the fan-loop output/occupancy for usage in next loop
        previousOccupancyStatus = occupancyStatus
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
     */
    private fun updateTitle24LoopCounter(tuners: HyperStatSplitProfileTuners, basicSettings: BasicSettings) {
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
            hyperstatSplitCPUEconAlgorithm.resetCoolingControl()
            state = ZoneState.COOLING
            logIt("Resetting cooling")
        } else if (currentTemp < userIntents.zoneHeatingTargetTemperature && state != ZoneState.HEATING) {
            hyperstatSplitCPUEconAlgorithm.resetHeatingControl()
            state = ZoneState.HEATING
            logIt("Resetting heating")
        }
    }

    private fun evaluateLoopOutputs(equip: HyperStatSplitCpuEconEquip, userIntents: UserIntents, basicSettings: BasicSettings, hyperStatSplitTuners: HyperStatSplitProfileTuners, isCondensateTripped: Boolean, effectiveOutsideDamperMinOpen: Int){

        when (state) {

            //Update coolingLoop when the zone is in cooling or it was in cooling and no change over happened yet.
            ZoneState.COOLING -> coolingLoopOutput = hyperstatSplitCPUEconAlgorithm.calculateCoolingLoopOutput(
                currentTemp, userIntents.zoneCoolingTargetTemperature
            ).toInt().coerceAtLeast(0)

            //Update heatingLoop when the zone is in heating or it was in heating and no change over happened yet.
            ZoneState.HEATING -> heatingLoopOutput = hyperstatSplitCPUEconAlgorithm.calculateHeatingLoopOutput(
                userIntents.zoneHeatingTargetTemperature, currentTemp
            ).toInt().coerceAtLeast(0)

            else -> logIt(" Zone is in deadband")
        }

        if (coolingLoopOutput > 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY
                    || basicSettings.conditioningMode == StandaloneConditioningMode.AUTO) ) {
            fanLoopOutput = ((coolingLoopOutput * hyperStatSplitTuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
        }
        else if (heatingLoopOutput > 0  && (basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY
                    ||basicSettings.conditioningMode == StandaloneConditioningMode.AUTO)) {
            fanLoopOutput = ((heatingLoopOutput * hyperStatSplitTuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
        }

        evaluateOAOLoop(equip, basicSettings, isCondensateTripped, effectiveOutsideDamperMinOpen)

    }

    private fun evaluateOAOLoop(equip: HyperStatSplitCpuEconEquip, basicSettings: BasicSettings, isCondensateTripped: Boolean, effectiveOutsideDamperMinOpen: Int) {

        // If there's not an OAO damper mapped,
        if (!HyperStatSplitAssociationUtil.isAnyAnalogAssociatedToOAO(equip.getConfiguration())) {

            economizingAvailable = false
            economizingLoopOutput = 0
            dcvAvailable = false
            dcvLoopOutput = 0
            outsideAirLoopOutput = 0
            matThrottle = false
            outsideAirFinalLoopOutput = 0

            // Even if there's not an OAO damper, still calculate enthalpy for portal widgets
            val externalTemp = CCUHsApi.getInstance().readHisValByQuery("system and outside and temp")
            val externalHumidity =
                CCUHsApi.getInstance().readHisValByQuery("system and outside and humidity")

            val indoorTemp = hsSplitHaystackUtil.getCurrentTemp()
            val indoorHumidity = hsSplitHaystackUtil.getHumidity()

            val insideEnthalpy = getAirEnthalpy(indoorTemp, indoorHumidity)
            val outsideEnthalpy = getAirEnthalpy(externalTemp, externalHumidity)

            equip.setHisVal("inside and enthalpy", insideEnthalpy)
            equip.setHisVal("outside and enthalpy", outsideEnthalpy)

        } else {

            val oaoDamperMatTarget = TunerUtil.readTunerValByQuery("oao and outside and damper and mat and target",equip.equipRef)
            val oaoDamperMatMin = TunerUtil.readTunerValByQuery("oao and outside and damper and mat and min",equip.equipRef)
            val economizingMaxTemp = TunerUtil.readTunerValByQuery("oao and economizing and max and temp", equip.equipRef)

            val matTemp  = hsSplitHaystackUtil.getMixedAirTemp()

            doEconomizing(equip)
            doDcv(equip, effectiveOutsideDamperMinOpen)

            outsideAirLoopOutput = Math.max(economizingLoopOutput, dcvLoopOutput)

            Log.d(L.TAG_CCU_HSSPLIT_CPUECON,"outsideAirLoopOutput "+outsideAirLoopOutput+" oaoDamperMatTarget "+oaoDamperMatTarget+" oaoDamperMatMin "+oaoDamperMatMin
                    +" matTemp "+matTemp)

            matThrottle = false

            // If Conditioning Mode is AUTO or COOL_ONLY, run full economizer loop algo
            if (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO || basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY) {
                if (outsideAirLoopOutput > effectiveOutsideDamperMinOpen) {
                    if (matTemp < oaoDamperMatTarget && matTemp > oaoDamperMatMin) {
                        outsideAirFinalLoopOutput = (outsideAirLoopOutput - outsideAirLoopOutput * ((oaoDamperMatTarget - matTemp) / (oaoDamperMatTarget - oaoDamperMatMin))).toInt()
                    } else {
                        outsideAirFinalLoopOutput = if (matTemp <= oaoDamperMatMin) 0 else outsideAirLoopOutput
                    }
                    if (matTemp < oaoDamperMatTarget) matThrottle = true
                } else {
                    outsideAirFinalLoopOutput = effectiveOutsideDamperMinOpen
                }

                if (matThrottle) {
                    outsideAirFinalLoopOutput = Math.max(outsideAirFinalLoopOutput , 0)
                } else {
                    outsideAirFinalLoopOutput = Math.max(outsideAirFinalLoopOutput , effectiveOutsideDamperMinOpen)
                }

                outsideAirFinalLoopOutput = Math.min(outsideAirFinalLoopOutput , 100)

            }

            // If Condensate is tripped, outsideAirFinalLoopOutput is zero. Damper will be at minimum signal.
            else if (isCondensateTripped) {
                Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "Condensate Switch is tripped. outsideAirFinalLoopOutput set to zero.")
                outsideAirFinalLoopOutput = 0
            }

            // If Conditioning Mode is HEAT_ONLY or OFF, ignore economizingLoopOutput and do DCV only.
            // Continue to use same the mixed air low-limit and high-limit as full economizing scenario.
            else {
                val dcvOnlyOutsideAirLoopOutput = Math.max(dcvLoopOutput, effectiveOutsideDamperMinOpen)

                if (dcvOnlyOutsideAirLoopOutput > effectiveOutsideDamperMinOpen) {
                    if (matTemp < oaoDamperMatTarget && matTemp > oaoDamperMatMin) {
                        outsideAirFinalLoopOutput = (dcvOnlyOutsideAirLoopOutput - dcvOnlyOutsideAirLoopOutput * ((oaoDamperMatTarget - matTemp) / (oaoDamperMatTarget - oaoDamperMatMin))).toInt()
                    } else {
                        outsideAirFinalLoopOutput = if (matTemp <= oaoDamperMatMin) 0 else dcvOnlyOutsideAirLoopOutput
                    }
                    if (matTemp < oaoDamperMatTarget) matThrottle = true
                } else {
                    outsideAirFinalLoopOutput = effectiveOutsideDamperMinOpen
                }

                if (matThrottle) {
                    outsideAirFinalLoopOutput = Math.max(outsideAirFinalLoopOutput , 0)
                } else {
                    outsideAirFinalLoopOutput = Math.max(outsideAirFinalLoopOutput , effectiveOutsideDamperMinOpen)
                }

                outsideAirFinalLoopOutput = Math.min(outsideAirFinalLoopOutput , 100)

                Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "Conditioning Mode is HEAT-ONLY or OFF. outsideAirFinalLoopOutput to consider DCV only.")

            }

            Log.d(L.TAG_CCU_HSSPLIT_CPUECON," economizingLoopOutput "+economizingLoopOutput+" dcvLoopOutput "+dcvLoopOutput
                    +" outsideAirFinalLoopOutput "+outsideAirFinalLoopOutput+" effectiveOutsideDamperMinOpen "+effectiveOutsideDamperMinOpen);

            equip.setHisVal("outside and air and final and loop", outsideAirFinalLoopOutput.toDouble())
            equip.setHisVal("oao and zone and logical and damper and actuator and cmd", outsideAirFinalLoopOutput.toDouble())

            val matThrottleNumber = if (matThrottle) 1.0 else 0.0
            equip.setHisVal("mat and available", matThrottleNumber)

        }

    }

    private fun doEconomizing(equip: HyperStatSplitCpuEconEquip) {

        val externalTemp = CCUHsApi.getInstance().readHisValByQuery("system and outside and temp and not lockout")
        val externalHumidity =
            CCUHsApi.getInstance().readHisValByQuery("system and outside and humidity")

        // Check for economizer enable
        if (canDoEconomizing(equip, externalTemp, externalHumidity)) {

            economizingAvailable = true

            val economizingToMainCoolingLoopMap = TunerUtil.readTunerValByQuery("oao and economizing and main and cooling and loop and map", equip.equipRef)

            val numberConfiguredCoolingStages = getNumberConfiguredCoolingStages(equip)

            if (numberConfiguredCoolingStages > 0) {
                economizingLoopOutput = Math.min((coolingLoopOutput * (numberConfiguredCoolingStages + 1)).toInt(), 100)
                Log.d(L.TAG_CCU_HSSPLIT_CPUECON, (numberConfiguredCoolingStages+1).toString() + " cooling stages available (including economizer); economizingLoopOutput = " + economizingLoopOutput);
            } else {
                Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "coolingLoopOutput = " + coolingLoopOutput + ", economizingToMainCoolingLoopMap = " + economizingToMainCoolingLoopMap + ", economizingLoopOutput = " + economizingLoopOutput);
                economizingLoopOutput = Math.min((coolingLoopOutput * (100 / economizingToMainCoolingLoopMap)).toInt(), 100)
            }

        } else {
            economizingAvailable = false
            economizingLoopOutput = 0
        }

        val economizingAvailableNumber = if (economizingAvailable) 1.0 else 0.0
        equip.setHisVal("economizing and available", economizingAvailableNumber)

    }

    /*
        Right now, it is possible to configure stages irregularly. (e.g. Stage 1 and Stage 3, but not Stage 2).

        If a user did this, it would mess with this mapping, but this will be far from the biggest operational issue
        introduced.

        We should probably validate against these kinds of configurations in the future.
     */
    private fun getNumberConfiguredCoolingStages(equip: HyperStatSplitCpuEconEquip): Int {
        if (HyperStatSplitAssociationUtil.isAnyRelayAssociatedToCoolingStage3(equip.getConfiguration())) return 3
        if (HyperStatSplitAssociationUtil.isAnyRelayAssociatedToCoolingStage2(equip.getConfiguration())) return 2
        if (HyperStatSplitAssociationUtil.isAnyRelayAssociatedToCoolingStage1(equip.getConfiguration())) return 1
        return 0
    }

    /*
    	Economizing is enabled when:
    	    ○ Zone is in Cooling Mode AND
		        ○ OAT < oaoEconomizingDryBulbTemperatureThreshold OR
		        ○ Weather OAEnthalpy is in range AND Local OAEnthalpy < IndoorEnthalpy - EnthalpyDuctCompensationOffset

        This works exactly the same as OAO profile logic, except that the failsafe logic upon weather
        data loss now includes enthalpy if an OAT/H sensor is on the sensor bus
     */
    private fun canDoEconomizing(equip: HyperStatSplitCpuEconEquip, externalTemp: Double, externalHumidity: Double): Boolean {

        val economizingMinTemp = TunerUtil.readTunerValByQuery("oao and economizing and min and temp",equip.equipRef)

        val indoorTemp = hsSplitHaystackUtil.getCurrentTemp()
        val indoorHumidity = hsSplitHaystackUtil.getHumidity()

        val insideEnthalpy = getAirEnthalpy(indoorTemp, indoorHumidity)
        val outsideEnthalpy = getAirEnthalpy(externalTemp, externalHumidity)

        equip.setHisVal("inside and enthalpy", insideEnthalpy)
        equip.setHisVal("outside and enthalpy", outsideEnthalpy)

        Log.d(L.TAG_CCU_HSSPLIT_CPUECON," canDoEconomizing externalTemp "+externalTemp+" externalHumidity "+externalHumidity)

        // If zone isn't in cooling mode, stop right here
        if (state != ZoneState.COOLING) return false

        // First, check local dry-bulb temp
        if (isEconomizingEnabledOnDryBulb(equip, externalTemp, externalHumidity, economizingMinTemp)) {
            Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "Economizer enabled based on dry-bulb temperature.")
            return true
        }

        if (!isEconomizingTempAndHumidityInRange(equip, externalTemp, externalHumidity, economizingMinTemp)) return false

        if (isEconomizingEnabledOnEnthalpy(equip, insideEnthalpy, outsideEnthalpy)) {
            Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "Economizing enabled based on enthalpy.")
            return true
        }

        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "Economizing disabled based on enthalpy.")
        return false

    }

    /*
        Same dry-bulb enable logic as OAO Profile.

        If outsideTemp is between economizingMinTemp (0°F, adj.) and dryBulbTemperatureThreshold (55°F, adj.), then enable economizing.
        (for outsideTemp, start with systemOutsideTemp, and use Outside Air Temperature sensor if systemOutsideTemp is 0)
     */
    private fun isEconomizingEnabledOnDryBulb(equip: HyperStatSplitCpuEconEquip, externalTemp: Double, externalHumidity: Double, economizingMinTemp: Double): Boolean {

        var dryBulbTemperatureThreshold = TunerUtil.readTunerValByQuery("economizing and dry and bulb and threshold", equip.equipRef)

        var outsideAirTemp = externalTemp

        if (externalHumidity == 0.0 && externalTemp == 0.0) {
            Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "System outside temp and humidity are both zero; using local OAT sensor");
            outsideAirTemp = equip.getOutsideAirTempSensor()
        }

        if (outsideAirTemp > economizingMinTemp) return outsideAirTemp < dryBulbTemperatureThreshold

        return false

    }

    /*
        Same Temp/Humidity Lockouts as OAO Profile.

        If any of the following is true, disable economizing:
            * systemOutsideTemp < economizingMinTemp (0°F, adj.)
            * systemOutsideTemp > economizingMaxTemp (70°F, adj.)
            * systemOutsideHumidity < economizingMinHumidity (0%, adj.)
            * systemOutsideHumidity > economizingMaxHumidity (100%, adj.)
     */
    private fun isEconomizingTempAndHumidityInRange(equip: HyperStatSplitCpuEconEquip, externalTemp: Double, externalHumidity: Double, economizingMinTemp: Double): Boolean {

        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "Checking outside temp and humidity against tuner min/max thresholds");

        val economizingMaxTemp = TunerUtil.readTunerValByQuery("economizing and max and " +
                "temp",equip.equipRef)
        val economizingMinHumidity = TunerUtil.readTunerValByQuery("economizing and min and " +
                "humidity",equip.equipRef)
        val economizingMaxHumidity = TunerUtil.readTunerValByQuery("economizing and max and " +
                "humidity",equip.equipRef)

        var outsideTemp = externalTemp
        var outsideHumidity = externalHumidity

        if (
            outsideTemp == 0.0 &&
            outsideHumidity == 0.0 &&
            HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToOutsideAir(
                equip.getConfiguration().address0State,
                equip.getConfiguration().address1State,
                equip.getConfiguration().address2State)
        ) {
            outsideTemp = equip.getOutsideAirTempSensor()
            outsideHumidity = equip.getOutsideAirHumiditySensor()
        }

        if (outsideTemp > economizingMinTemp
            && outsideTemp < economizingMaxTemp
            && outsideHumidity > economizingMinHumidity
            && outsideHumidity < economizingMaxHumidity) return true

        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "Outside air (" + outsideTemp + "°F, " + outsideHumidity + "%RH) out of temp/humidity range from tuners; economizing disabled");

        return false

    }

    /*
        Same enthalpy-enable condition as OAO Profile.

        If systemOutsideEnthalpy < insideEnthalpy + enthalpyDuctCompensationOffset (0 BTU/lb, adj.), enable economizing
        (Start with systemOutsideEnthalpy. If it's not available and OAT/H sensor is on sensor bus, then calculate outsideEnthalpy
        based on sensed Outside Air Temperature & Humidity.
     */
    private fun isEconomizingEnabledOnEnthalpy (equip: HyperStatSplitCpuEconEquip, insideEnthalpy: Double, outsideEnthalpy: Double): Boolean {

        Log.d(L.TAG_CCU_HSSPLIT_CPUECON,"Checking enthalpy-enable condition: insideEnthalpy "+insideEnthalpy+", outsideEnthalpy "+ outsideEnthalpy)

        var outsideEnthalpyToUse = outsideEnthalpy

        // Our enthalpy calc returns a value of 0.12 for zero temp and humidity.
        // We will assume anything less than 1 translates to system weather data being dead
        if (
            outsideEnthalpy < 1 &&
            HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToOutsideAir(
                equip.getConfiguration().address0State,
                equip.getConfiguration().address1State,
                equip.getConfiguration().address2State)
        ) {
            Log.d(L.TAG_CCU_HSSPLIT_CPUECON,"System outside temp and humidity are both zero; using local outside air temp/humidity sensor")
            val sensorBusOutsideTemp = equip.getOutsideAirTempSensor()
            val sensorBusOutsideHumidity = equip.getOutsideAirHumiditySensor()
            outsideEnthalpyToUse = getAirEnthalpy(sensorBusOutsideTemp, sensorBusOutsideHumidity)
        }

        val enthalpyDuctCompensationOffset = TunerUtil.readTunerValByQuery("oao and enthalpy and duct and compensation and offset",equip.equipRef)

        return insideEnthalpy > outsideEnthalpyToUse + enthalpyDuctCompensationOffset

    }

    /*
        DCV is enabled when:
            ○ Zone is in Occupied Mode AND
            ○ Zone CO2 (sensed on HyperLite) > Zone CO2 Threshold
     */
    private fun doDcv(equip: HyperStatSplitCpuEconEquip, standaloneOutsideAirDamperMinOpen: Int) {

        dcvAvailable = false
        var zoneSensorCO2 = hsSplitHaystackUtil.getZoneCO2()
        var zoneCO2Threshold = hsSplitHaystackUtil.getZoneCO2Threshold()
        var co2DamperOpeningRate = hsSplitHaystackUtil.getCO2DamperOpeningRate()
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "zoneSensorCO2: " + zoneSensorCO2 + ", zoneCO2Threshold: " + zoneCO2Threshold + ", co2DamperOpeningRate: " + co2DamperOpeningRate)
        if (occupancyStatus == Occupancy.OCCUPIED || occupancyStatus == Occupancy.FORCEDOCCUPIED || occupancyStatus == Occupancy.AUTOFORCEOCCUPIED) {
            if (zoneSensorCO2 > zoneCO2Threshold) {
                dcvAvailable = true
                dcvLoopOutput = ((zoneSensorCO2 - zoneCO2Threshold) / co2DamperOpeningRate).toInt()
                outsideAirCalculatedMinDamper = Math.max(dcvLoopOutput, standaloneOutsideAirDamperMinOpen)
            } else {
                outsideAirCalculatedMinDamper = standaloneOutsideAirDamperMinOpen
            }
        } else {
            outsideAirCalculatedMinDamper = 0
        }

        val dcvAvailableNum = if (dcvAvailable) 1.0 else 0.0
        equip.setHisVal("dcv and available", dcvAvailableNum)
        equip.setHisVal("dcv and loop and output", dcvLoopOutput.toDouble())
        equip.setHisVal("outside and air and min and damper and calculated", outsideAirCalculatedMinDamper.toDouble())

    }

    private fun fetchBasicSettings(equip: HyperStatSplitCpuEconEquip, wasCondensateTripped: Boolean, isCondensateTripped: Boolean): BasicSettings {

        /*
            When tripped, condensate sensor will force Conditioning Mode to OFF.
            The last Conditioning Mode set by the user is stored in the var lastUserIntentConditioningMode.
         */

        var appWasJustRestarted: Boolean = true

        if (isCondensateTripped) {

            // If Condensate just tripped, store whatever the previous Conditioning Mode was as UserIntentConditioningMode.
            // Conditioning Mode will return to this value once condensate returns to normal. User will not be able to change it back until this happens.
            if (!wasCondensateTripped && !appWasJustRestarted)  PreferenceUtil.setLastUserIntentConditioningMode(StandaloneConditioningMode.values()[equip.hsSplitHaystackUtil.getCurrentConditioningMode().toInt()]);

            equip.hsSplitHaystackUtil.setConditioningMode(StandaloneConditioningMode.OFF.ordinal.toDouble())
            return BasicSettings(
                conditioningMode = StandaloneConditioningMode.OFF,
                fanMode = StandaloneFanStage.values()[equip.hsSplitHaystackUtil.getCurrentFanMode().toInt()]
            )

        } else {

            /*
                If condensate is not tripped, user is allowed to change the Conditioning Mode.
                Any changes made to the Haystack point (this can be done via CCU UI or at device)
                will be saved to lastUserIntentConditioningMode.
             */
            if (!wasCondensateTripped) PreferenceUtil.setLastUserIntentConditioningMode(StandaloneConditioningMode.values()[equip.hsSplitHaystackUtil.getCurrentConditioningMode().toInt()])
            equip.hsSplitHaystackUtil.setConditioningMode(PreferenceUtil.getLastUserIntentConditioningMode().ordinal.toDouble())
            return BasicSettings(
                conditioningMode = StandaloneConditioningMode.values()[equip.hsSplitHaystackUtil.getCurrentConditioningMode().toInt()],
                fanMode = StandaloneFanStage.values()[equip.hsSplitHaystackUtil.getCurrentFanMode().toInt()]
            )

        }

        appWasJustRestarted = false

    }

    private fun fetchUserIntents(equip: HyperStatSplitCpuEconEquip): UserIntents {
        return UserIntents(
            currentTemp = equip.getCurrentTemp(),
            zoneCoolingTargetTemperature = equip.hsSplitHaystackUtil.getDesiredTempCooling(),
            zoneHeatingTargetTemperature = equip.hsSplitHaystackUtil.getDesiredTempHeating(),
            targetMinInsideHumidity = equip.hsSplitHaystackUtil.getTargetMinInsideHumidity(),
            targetMaxInsideHumidity = equip.hsSplitHaystackUtil.getTargetMaxInsideHumidity(),
        )
    }

    private fun fetchHyperStatSplitTuners(equip: HyperStatSplitCpuEconEquip): HyperStatSplitProfileTuners {

        /**
         * Consider that
         * proportionalGain = proportionalKFactor
         * integralGain = integralKFactor
         * proportionalSpread = temperatureProportionalRange
         * integralMaxTimeout = temperatureIntegralTime
         */

        val hsSplitTuners = HyperStatSplitProfileTuners()
        hsSplitTuners.proportionalGain = TunerUtil.getProportionalGain(equip.equipRef!!)
        hsSplitTuners.integralGain = TunerUtil.getIntegralGain(equip.equipRef!!)
        hsSplitTuners.proportionalSpread = TunerUtil.getProportionalSpread(equip.equipRef!!)
        hsSplitTuners.integralMaxTimeout = TunerUtil.getIntegralTimeout(equip.equipRef!!).toInt()
        hsSplitTuners.relayActivationHysteresis = TunerUtil.getHysteresisPoint("relay and  activation", equip.equipRef!!).toInt()
        hsSplitTuners.analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery("analog and fan and speed and multiplier", equip.equipRef!!)
        hsSplitTuners.humidityHysteresis = TunerUtil.getHysteresisPoint("humidity", equip.equipRef!!).toInt()
        hsSplitTuners.minFanRuntimePostConditioning = TunerUtil.readTunerValByQuery("fan and cur and runtime and postconditioning and min", equip.equipRef!!).toInt()
        return hsSplitTuners
    }

    private fun runRelayOperations(
        config: HyperStatSplitCpuEconConfiguration, tuner: HyperStatSplitProfileTuners, userIntents: UserIntents,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>,
        isCondensateTripped: Boolean,
        exhaustFanStage1Threshold: Int,
        exhaustFanStage2Threshold: Int,
        exhaustFanHysteresis: Int
    ) {
        if (config.relay1State.enabled) {
            handleRelayState(
                config.relay1State, config, Port.RELAY_ONE, 
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
        if (config.relay2State.enabled) {
            handleRelayState(
                config.relay2State, config, Port.RELAY_TWO, 
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
        if (config.relay3State.enabled) {
            handleRelayState(
                config.relay3State, config, Port.RELAY_THREE,
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
        if (config.relay4State.enabled) {
            handleRelayState(
                config.relay4State, config, Port.RELAY_FOUR,
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
        if (config.relay5State.enabled) {
            handleRelayState(
                config.relay5State, config, Port.RELAY_FIVE,
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
        if (config.relay6State.enabled) {
            handleRelayState(
                config.relay6State, config, Port.RELAY_SIX,
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
        if (config.relay7State.enabled) {
            handleRelayState(
                config.relay7State, config, Port.RELAY_SEVEN,
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
        if (config.relay8State.enabled) {
            handleRelayState(
                config.relay8State, config, Port.RELAY_EIGHT,
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
    }

    private fun runAnalogOutOperations(
        equip: HyperStatSplitCpuEconEquip, config: HyperStatSplitCpuEconConfiguration,
        basicSettings: BasicSettings, analogOutStages: HashMap<String, Int>,
        isCondensateTripped: Boolean
    ) {
        if (config.analogOut1State.enabled) {
            handleAnalogOutState(
                config.analogOut1State, equip, config, Port.ANALOG_OUT_ONE, basicSettings, analogOutStages, isCondensateTripped
            )
        }
        if (config.analogOut2State.enabled) {
            handleAnalogOutState(
                config.analogOut2State, equip, config, Port.ANALOG_OUT_TWO, basicSettings, analogOutStages, isCondensateTripped
            )
        }
        if (config.analogOut3State.enabled) {
            handleAnalogOutState(
                config.analogOut3State, equip, config, Port.ANALOG_OUT_THREE, basicSettings, analogOutStages, isCondensateTripped
            )
        }
        if (config.analogOut4State.enabled) {
            handleAnalogOutState(
                config.analogOut4State, equip, config, Port.ANALOG_OUT_FOUR, basicSettings, analogOutStages, isCondensateTripped
            )
        }
    }

    private fun handleRelayState(
        relayState: RelayState, config: HyperStatSplitCpuEconConfiguration, port: Port, tuner: HyperStatSplitProfileTuners,
        userIntents: UserIntents, basicSettings: BasicSettings, relayStages: HashMap<String, Int>, isCondensateTripped : Boolean,
        exhaustFanStage1Threshold: Int, exhaustFanStage2Threshold: Int, exhaustFanHysteresis: Int
    ) {
        when {
            (HyperStatSplitAssociationUtil.isRelayAssociatedToCoolingStage(relayState)) -> {

                if ((basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.COOL_ONLY.ordinal ||
                    basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal) && !isCondensateTripped
                ) {
                    runRelayForCooling(relayState, port, config, tuner, relayStages)
                } else {
                    resetPort(port)
                }
            }
            (HyperStatSplitAssociationUtil.isRelayAssociatedToHeatingStage(relayState)) -> {

                if ((basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.HEAT_ONLY.ordinal ||
                    basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal) && !isCondensateTripped
                ) {
                    runRelayForHeating(relayState, port, config, tuner, relayStages)
                } else {
                    resetPort(port)
                }
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToFan(relayState)) -> {

                if (basicSettings.fanMode != StandaloneFanStage.OFF && !isCondensateTripped) {
                    runRelayForFanSpeed(relayState, port, config, tuner, relayStages, basicSettings,
                        previousFanLoopVal, fanLoopCounter)
                } else {
                   resetPort(port)
                }
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToFanEnabled(relayState)) -> {
                if (!isCondensateTripped) {
                    doFanEnabled( curState,port, fanLoopOutput )
                } else {
                    resetPort(port)
                }
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToOccupiedEnabled(relayState)) -> {
                doOccupiedEnabled(port)
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToHumidifier(relayState)) -> {
                if (!isCondensateTripped) {
                    doHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMinInsideHumidity)
                } else {
                    resetPort(port)
                }
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToDeHumidifier(relayState)) -> {
                if (!isCondensateTripped) {
                    doDeHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMaxInsideHumidity)
                } else {
                    resetPort(port)
                }
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToExhaustFanStage1(relayState)) -> {
                if (!isCondensateTripped) {
                    doExhaustFanStage1(port, outsideAirFinalLoopOutput, exhaustFanStage1Threshold, exhaustFanHysteresis)
                } else {
                    resetPort(port)
                }
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToExhaustFanStage2(relayState)) -> {
                if (!isCondensateTripped) {
                    doExhaustFanStage2(port, outsideAirFinalLoopOutput, exhaustFanStage2Threshold, exhaustFanHysteresis)
                } else {
                    resetPort(port)
                }

            }
        }
    }

    private fun runRelayForCooling(
        relayAssociation: RelayState,
        whichPort: Port,
        config: HyperStatSplitCpuEconConfiguration,
        tuner: HyperStatSplitProfileTuners,
        relayStages: HashMap<String, Int>
    ) {
        val highestStage = HyperStatSplitAssociationUtil.getHighestCoolingStage(config).ordinal
        var s1threshold = 0
        var s2threshold = 0
        var s3threshold = 0
        
        if (economizingAvailable) {
            when (highestStage) {
                0 -> { 
                    s1threshold = 50 
                }
                1 -> { 
                    s1threshold = 33 
                    s2threshold = 67
                }
                2 -> { 
                    s1threshold = 25
                    s2threshold = 50
                    s3threshold = 75
                }
                else -> { }
            }
        } else {
            when (highestStage) {
                0 -> {}
                1 -> { 
                    s2threshold = 50 
                }
                2 -> { 
                    s2threshold = 33 
                    s3threshold = 67
                }
                else -> {}
            }
        }

        logIt(" $whichPort: ${relayAssociation.association}")
        when (relayAssociation.association) {
            CpuEconRelayAssociation.COOLING_STAGE_1 -> {
                doCoolingStage1(
                    whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, s1threshold, relayStages
                )
            }
            CpuEconRelayAssociation.COOLING_STAGE_2 -> {
               doCoolingStage2(
                   whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, s2threshold,relayStages
               )
            }
            CpuEconRelayAssociation.COOLING_STAGE_3 -> {
                doCoolingStage3(
                    whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, s3threshold, relayStages
                )
            }
            else -> {}
        }
        if(getCurrentPortStatus(whichPort) == 1.0)
            curState = ZoneState.COOLING

    }

    private fun runRelayForHeating(
        relayAssociation: RelayState, whichPort: Port, config: HyperStatSplitCpuEconConfiguration,
        tuner: HyperStatSplitProfileTuners, relayStages: HashMap<String, Int>
    ) {
        logIt(" $whichPort: ${relayAssociation.association}")
        when (relayAssociation.association) {
            CpuEconRelayAssociation.HEATING_STAGE_1 -> {
                doHeatingStage1(
                    whichPort,
                    heatingLoopOutput,
                    tuner.relayActivationHysteresis,
                    relayStages
                )
            }
            CpuEconRelayAssociation.HEATING_STAGE_2 -> {
                val highestStage = HyperStatSplitAssociationUtil.getHighestHeatingStage(config).ordinal
                val divider = if (highestStage == 4) 50 else 33
                doHeatingStage2(
                    whichPort, heatingLoopOutput, tuner.relayActivationHysteresis,
                    divider, relayStages)
            }
            CpuEconRelayAssociation.HEATING_STAGE_3 -> {
                doHeatingStage3(
                    whichPort, heatingLoopOutput, tuner.relayActivationHysteresis, relayStages)
            }
            else -> {}
        }
        if(getCurrentPortStatus(whichPort) == 1.0) curState = ZoneState.HEATING
    }

    private fun runRelayForFanSpeed(
        relayAssociation: RelayState, whichPort: Port, config: HyperStatSplitCpuEconConfiguration,
        tuner: HyperStatSplitProfileTuners, relayStages: HashMap<String, Int>, basicSettings: BasicSettings,
        previousFanLoopVal: Int, fanProtectionCounter: Int
    ) {
        var localFanLoopOutput = fanLoopOutput
        logIt(" $whichPort: ${relayAssociation.association} runRelayForFanSpeed: ${basicSettings.fanMode}")
        if (basicSettings.fanMode == StandaloneFanStage.AUTO
            && basicSettings.conditioningMode == StandaloneConditioningMode.OFF ) {
            logIt("Cond is Off , Fan is Auto  : ")
            resetPort(whichPort)
            return
        }
        val highestStage = HyperStatSplitAssociationUtil.getHighestFanStage(config)
        val divider = if (highestStage == CpuEconRelayAssociation.FAN_MEDIUM_SPEED) 50 else 33
        val fanEnabledMapped = HyperStatSplitAssociationUtil.isAnyRelayAssociatedToFanEnabled(config)
        val lowestStage = HyperStatSplitAssociationUtil.getLowestFanStage(config)

        if (fanEnabledMapped) setFanEnabledStatus(true) else setFanEnabledStatus(false)

        when(lowestStage) {
            CpuEconRelayAssociation.FAN_LOW_SPEED -> setFanLowestFanLowStatus(true)
            CpuEconRelayAssociation.FAN_MEDIUM_SPEED -> setFanLowestFanMediumStatus(true)
            CpuEconRelayAssociation.FAN_HIGH_SPEED -> setFanLowestFanHighStatus(true)
            else -> {}
        }

        // In order to protect the fan, persist the fan for few cycles when there is a sudden change in
        // occupancy and decrease in fan loop output
        if (fanProtectionCounter > 0) {
            localFanLoopOutput = previousFanLoopVal
        }

        when (relayAssociation.association) {
            CpuEconRelayAssociation.FAN_LOW_SPEED -> {
                doFanLowSpeed(
                    whichPort,
                    logicalPointsList[whichPort]!!,null,null, basicSettings.fanMode,
                    localFanLoopOutput,tuner.relayActivationHysteresis,relayStages,divider, epidemicState == EpidemicState.PREPURGE)
            }
            CpuEconRelayAssociation.FAN_MEDIUM_SPEED -> {
                doFanMediumSpeed(
                    whichPort,
                    logicalPointsList[whichPort]!!,null,basicSettings.fanMode,
                    localFanLoopOutput,tuner.relayActivationHysteresis,divider,relayStages, epidemicState == EpidemicState.PREPURGE)
            }
            CpuEconRelayAssociation.FAN_HIGH_SPEED -> {
                doFanHighSpeed(
                    whichPort,
                    logicalPointsList[whichPort]!!,basicSettings.fanMode,
                    localFanLoopOutput,tuner.relayActivationHysteresis,relayStages, epidemicState == EpidemicState.PREPURGE)
            }
            else -> return
        }
    }

    private fun handleAnalogOutState(
        analogOutState: AnalogOutState, equip: HyperStatSplitCpuEconEquip, config: HyperStatSplitCpuEconConfiguration,
        port: Port, basicSettings: BasicSettings, analogOutStages: HashMap<String, Int>, isCondensateTripped: Boolean
    ) {
        // If we are in Auto Away mode we no need to Any analog Operations
        when {
            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToCooling(analogOutState)) -> {
                if (!isCondensateTripped) {
                    doAnalogCooling(port,basicSettings.conditioningMode,analogOutStages,coolingLoopOutput)
                } else{
                    resetPort(port)
                }
            }
            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToHeating(analogOutState)) -> {
                if (!isCondensateTripped) {
                    doAnalogHeating(port,basicSettings.conditioningMode,analogOutStages,heatingLoopOutput)
                } else {
                    resetPort(port)
                }
            }

            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState)) -> {
                if (!isCondensateTripped) {
                    doAnalogFanActionCpuEcon(
                        equip, port, analogOutState.perAtFanLow.toInt(), analogOutState.perAtFanMedium.toInt(),
                        analogOutState.perAtFanHigh.toInt(), basicSettings.fanMode,
                        basicSettings.conditioningMode, fanLoopOutput, analogOutStages, previousFanLoopVal, fanLoopCounter
                    )
                } else {
                    resetPort(port)
                }
            }

            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToOaoDamper(analogOutState)) -> {
                if (!isCondensateTripped) {
                    doAnalogOAOAction(
                        equip, port,basicSettings.conditioningMode, analogOutStages, outsideAirFinalLoopOutput
                    )
                } else {
                    resetPort(port)
                }
            }

            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(analogOutState)) -> {
                if (!isCondensateTripped) {
                    doAnalogStagedFanAction(
                        port, analogOutState.perAtFanLow.toInt(), analogOutState.perAtFanMedium.toInt(),
                        analogOutState.perAtFanHigh.toInt(), basicSettings.fanMode, HyperStatSplitAssociationUtil.isAnyRelayAssociatedToFanEnabled(config),
                        basicSettings.conditioningMode, fanLoopOutput, analogOutStages, fanLoopCounter
                    )
                } else {
                    resetPort(port)
                }
            }
        }
    }

    /**
     * Performs analog fan action for the CPU.
     * This function updates the logical point values for fan control based on various parameters such as fan mode, conditioning mode, and occupancy status.
     * It also handles the Title 24 compliance by adjusting the fan runtime when transitioning from OCCUPIED to UNOCCUPIED status.
     *
     * @param port The port associated with the fan action.
     * @param fanLowPercent The percentage of fan speed for the low stage.
     * @param fanMediumPercent The percentage of fan speed for the medium stage.
     * @param fanHighPercent The percentage of fan speed for the high stage.
     * @param fanMode The mode of the fan (e.g., OFF, AUTO, LOW_CUR_OCC).
     * @param conditioningMode The conditioning mode (e.g., OFF, COOLING, HEATING).
     * @param fanLoopOutput The output value for the fan loop.
     * @param analogOutStages A HashMap containing analog output stages.
     * @param fanProtectionCounter In case of non zero value, retain the fan loop to previous value for few cycles.
     */
    private fun doAnalogFanActionCpuEcon(
        equip: HyperStatSplitCpuEconEquip,
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
                if(epidemicState == EpidemicState.PREPURGE) {
                    if(prePurgeEnabled && prePurgeOpeningValue > fanLoopForAnalog)
                        fanLoopForAnalog = prePurgeOpeningValue.roundToInt()
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
            Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "$port = Linear Fan Speed  analogSignal   $fanLoopForAnalog")
        }
    }

    private fun getCoolingActivatedAnalogVoltage(fanEnabledMapped: Boolean): Int {
        var voltage = getPercentageFromVoltageSelected(getCoolingStateActivated().roundToInt())
        // For title 24 compliance, check if fanEnabled is mapped, then set the fanloopForAnalog to the lowest cooling state activated
        // and check if staged fan is inactive(fanLoopForAnalog == 0)
        if(fanEnabledMapped && voltage == 0) {
            voltage = getPercentageFromVoltageSelected(getLowestCoolingStateActivated().roundToInt())
        }
        return voltage
    }


    private fun getHeatingActivatedAnalogVoltage(fanEnabledMapped: Boolean): Int {
        var voltage = getPercentageFromVoltageSelected(getHeatingStateActivated().roundToInt())
        // For title 24 compliance, check if fanEnabled is mapped, then set the fanloopForAnalog to the lowest heating state activated
        // and check if staged fan is inactive(fanLoopForAnalog == 0)
        if (fanEnabledMapped && voltage == 0) {
            voltage = getPercentageFromVoltageSelected(getLowestHeatingStateActivated().roundToInt())
        }
        return voltage
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
                        fanLoopForAnalog = getCoolingActivatedAnalogVoltage(fanEnabledMapped)
                        logMsg = "Cooling"
                    } else if (getOperatingMode() == 2.0) {
                        fanLoopForAnalog = getHeatingActivatedAnalogVoltage(fanEnabledMapped)
                        logMsg = "Heating"
                    }
                } else if (conditioningMode == StandaloneConditioningMode.COOL_ONLY) {
                    fanLoopForAnalog = getCoolingActivatedAnalogVoltage(fanEnabledMapped)
                    logMsg = "Cooling"
                } else if (conditioningMode == StandaloneConditioningMode.HEAT_ONLY) {
                    fanLoopForAnalog = getHeatingActivatedAnalogVoltage(fanEnabledMapped)
                    logMsg = "Heating"
                }
                // Check if we need fan protection
                if(fanProtectionCounter > 0 && fanLoopForAnalog < previousFanLoopValStaged) {
                    fanLoopForAnalog = previousFanLoopValStaged
                    logMsg = "Fan Protection"
                }
                else previousFanLoopValStaged = fanLoopForAnalog // else indicates we are not in protection mode, so store the fanLoopForAnalog value for protection mode

                // Check Dead-band condition
                if (fanLoopForAnalog == 0) { // When in dead-band, set the fan-loopForAnalog to the recirculate analog value
                    fanLoopForAnalog = getPercentageFromVoltageSelected(getAnalogRecirculateValueActivated().roundToInt())
                    logMsg = "Dead-band"
                }
                // The speed at which fan operates is determined by configuration parameter - "Analog-Out During Economizer"
                if(economizingLoopOutput != 0) fanLoopForAnalog = getPercentageFromVoltageSelected(getAnalogEconomizerValueActivated().roundToInt())
                if(epidemicState == EpidemicState.PREPURGE) {
                    if(prePurgeEnabled) {
                        fanLoopForAnalog = prePurgeOpeningValue.roundToInt()
                        logMsg = "Pre-Purge"
                    }
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
            Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "$port = Staged Fan Speed($logMsg)  analogSignal  $fanLoopForAnalog")
        }
    }

    override fun doAnalogCooling(
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        coolingLoopOutput: Int
    ) {
        if (conditioningMode.ordinal == StandaloneConditioningMode.COOL_ONLY.ordinal ||
            conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
        ) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, coolingLoopOutput.toDouble())
            if (coolingLoopOutput > 0) {
                analogOutStages[AnalogOutput.COOLING.name] = coolingLoopOutput
                curState = ZoneState.COOLING
            }
        } else {
            updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
        }

    }

    override fun doAnalogHeating(
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        heatingLoopOutput: Int
    ) {
        if (conditioningMode.ordinal == StandaloneConditioningMode.HEAT_ONLY.ordinal ||
            conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
        ) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, heatingLoopOutput.toDouble())
            if (heatingLoopOutput > 0) {
                analogOutStages[AnalogOutput.HEATING.name] = heatingLoopOutput
                curState = ZoneState.HEATING
            }
        } else {
            updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
        }
    }

    private fun getOperatingMode(): Double {

        return hsSplitHaystackUtil.readHisVal("point and operating and mode")

    }

    /**
     * Handles the smart pre-purge control for the given HyperStatSplitCpuEconEquip.
     * This function calculates the minimum damper open value based on pre-purge settings and occupancy status.
     *
     * @param equip The HyperStatSplitCpuEconEquip for which smart pre-purge control is to be handled.
     * @return The minimum damper open value if the conditions for smart pre-purge control are met, null otherwise.
     */
    private fun handleSmartPrePurgeControl(equip: HyperStatSplitCpuEconEquip): Int? {
        if(!prePurgeEnabled) {
            epidemicState = EpidemicState.OFF
            return null
        }
        val prePurgeRunTime = TunerUtil.readTunerValByQuery(
            "prePurge and cur and runtime and zone and default and cpu and standalone and oao",
            equip.equipRef
        )
        val prePurgeOccupiedTimeOffset: Double = TunerUtil.readTunerValByQuery(
            "prePurge and offset and cur and time and zone and default and occupied and cpu and standalone and oao",
            equip.equipRef
        )
        val oaoDamperMatMin = TunerUtil.readTunerValByQuery("oao and outside and damper and mat and min",equip.equipRef)
        val matTemp  = hsSplitHaystackUtil.getMixedAirTemp()
        val zoneId = equip.getRoomRef()
        val occuStatus = ScheduleManager.getInstance().getOccupiedModeCache(zoneId)
        val minutesToOccupancy =
            if (occuStatus != null) occuStatus.millisecondsUntilNextChange.toInt() / 60000 else -1
        if (minutesToOccupancy != -1 && prePurgeOccupiedTimeOffset >= minutesToOccupancy && minutesToOccupancy >= prePurgeOccupiedTimeOffset - prePurgeRunTime &&
            matTemp > oaoDamperMatMin) {
            val outsideAirCalculatedMinDamper = hsSplitHaystackUtil.getDamperMinOpenConfigValue()
            epidemicState = EpidemicState.PREPURGE
            return outsideAirCalculatedMinDamper.toInt()
        } else {
            epidemicState = EpidemicState.OFF
        }
        return null
    }


    override fun doAnalogOAOAction(
        equip: HyperStatSplitCpuEconEquip,
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        outsideAirFinalLoopOutput: Int
    ) {
        var localFinalLoopOutput: Int = outsideAirFinalLoopOutput
        // Pre purge control
        if(occupancyStatus == Occupancy.UNOCCUPIED || occupancyStatus == Occupancy.VACATION) {
            if(null != handleSmartPrePurgeControl(equip))
                localFinalLoopOutput = handleSmartPrePurgeControl(equip)!!
        }
        // Safeties are handled in the algo, not here.
        // If Condensate is tripped or conditioning mode is not appropriate, it will be reflected in the calculated outsideAirFinalLoopOutput.
        updateLogicalPointIdValue(logicalPointsList[port]!!, localFinalLoopOutput.toDouble())
        if (outsideAirFinalLoopOutput > 0) analogOutStages[AnalogOutput.OAO_DAMPER.name] = localFinalLoopOutput
    }

    private fun handleDeadZone(equip: HyperStatSplitCpuEconEquip) {

       logIt("Zone is Dead ${equip.node}")
        state = ZoneState.TEMPDEAD
        resetAllLogicalPointValues(equip)
        equip.hsSplitHaystackUtil.setProfilePoint("operating and mode", state.ordinal.toDouble())
        if (equip.hsSplitHaystackUtil.getEquipStatus() != state.ordinal.toDouble())
            equip.hsSplitHaystackUtil.setEquipStatus(state.ordinal.toDouble())
        val curStatus = equip.hsSplitHaystackUtil.getEquipLiveStatus()
        if (curStatus != "Zone Temp Dead") {
            equip.hsSplitHaystackUtil.writeDefaultVal("status and message and writable", "Zone Temp Dead")
        }
        equip.haystack.writeHisValByQuery(
            "point and not ota and status and his and group == \"${equip.node}\"",
            ZoneState.TEMPDEAD.ordinal.toDouble()
        )
    }
    private fun handleRFDead(equip: HyperStatSplitCpuEconEquip) {
        logIt("RF Signal is Dead ${equip.node}")
        state = ZoneState.RFDEAD
        equip.hsSplitHaystackUtil.setProfilePoint("operating and mode", state.ordinal.toDouble())
        if (equip.hsSplitHaystackUtil.getEquipStatus() != state.ordinal.toDouble()) {
            equip.hsSplitHaystackUtil.setEquipStatus(state.ordinal.toDouble())
        }
        val curStatus = equip.hsSplitHaystackUtil.getEquipLiveStatus()
        if (curStatus != RFDead) {
            equip.hsSplitHaystackUtil.writeDefaultVal("status and message and writable", RFDead)
        }
        equip.haystack.writeHisValByQuery(
            "point and not ota and status and his and group == \"${equip.node}\"",
            ZoneState.RFDEAD.ordinal.toDouble()
        )
    }
    override fun getEquip(): Equip? {
        for (nodeAddress in cpuEconDeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    @JsonIgnore
    override fun getNodeAddresses(): Set<Short?> {
        return cpuEconDeviceMap.keys
    }

    private fun resetAllLogicalPointValues(equip: HyperStatSplitCpuEconEquip) {
        equip.hsSplitHaystackUtil.updateConditioningLoopOutput(0,0,0,false,0)
        equip.hsSplitHaystackUtil.updateOaoLoopOutput(0,0,0,0)
        resetAllLogicalPointValues()
        val outsideDamperMinOpen = getEffectiveOutsideDamperMinOpen()
        HyperStatSplitUserIntentHandler.updateHyperStatSplitStatus(
            equipId = equip.equipRef!!,
            portStages = HashMap(),
            analogOutStages = HashMap(),
            temperatureState = ZoneTempState.TEMP_DEAD,
            economizingLoopOutput, dcvLoopOutput,
            outsideDamperMinOpen, outsideAirFinalLoopOutput,
            hsSplitHaystackUtil.getCondensateOverflowStatus(),
            hsSplitHaystackUtil.getFilterStatus(),
            fetchBasicSettings(equip, false, hsSplitHaystackUtil.getCondensateOverflowStatus() > 0.0),
            EpidemicState.OFF
        )
    }

    @JsonIgnore
    override fun getCurrentTemp(): Double {
        for (nodeAddress in cpuEconDeviceMap.keys) {
            return cpuEconDeviceMap[nodeAddress]!!.getCurrentTemp()
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
        for (nodeAddress in cpuEconDeviceMap.keys) {
            if (cpuEconDeviceMap[nodeAddress] == null) {
                continue
            }
            if (cpuEconDeviceMap[nodeAddress]!!.getCurrentTemp() > 0) {
                tempTotal += cpuEconDeviceMap[nodeAddress]!!.getCurrentTemp()
                nodeCount++
            }
        }
        return if (nodeCount == 0) 0.0 else tempTotal / nodeCount
    }

    /**
     * Function just to print logs
     */
    private fun logIt(msg: String){
        Log.i(L.TAG_CCU_HSSPLIT_CPUECON,msg)
    }

    private fun getCoolingStateActivated (): Double {
        return if (stageActive("cooling and runtime and stage3")) {
            hsSplitHaystackUtil.readPointValue("fan and cooling and stage3")
        } else if (stageActive("cooling and runtime and stage2")) {
            hsSplitHaystackUtil.readPointValue("fan and cooling and stage2")
        } else if (stageActive("cooling and runtime and stage1")) {
            hsSplitHaystackUtil.readPointValue("fan and cooling and stage1")
        } else if (isEconomizerActive()) {
          // default to Cooling Stage 1 Fan speed if economizer is running
            hsSplitHaystackUtil.readPointValue("fan and cooling and stage1")
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
        return if (hsSplitHaystackUtil.readPointValue("recirculate and analog1") != 0.0) {
            hsSplitHaystackUtil.readPointValue("recirculate and analog1")
        } else if (hsSplitHaystackUtil.readPointValue("recirculate and analog2") != 0.0) {
            hsSplitHaystackUtil.readPointValue("recirculate and analog2")
        } else if (hsSplitHaystackUtil.readPointValue("recirculate and analog3") != 0.0) {
            hsSplitHaystackUtil.readPointValue("recirculate and analog3")
        } else if (hsSplitHaystackUtil.readPointValue("recirculate and analog4") != 0.0) {
            hsSplitHaystackUtil.readPointValue("recirculate and analog4")
        } else {
            defaultFanLoopOutput
        }
    }

    private fun getAnalogEconomizerValueActivated (): Double {
        return if (hsSplitHaystackUtil.readPointValue("economizer and analog1") != 0.0) {
            hsSplitHaystackUtil.readPointValue("economizer and analog1")
        } else if (hsSplitHaystackUtil.readPointValue("economizer and analog2") != 0.0) {
            hsSplitHaystackUtil.readPointValue("economizer and analog2")
        } else if (hsSplitHaystackUtil.readPointValue("economizer and analog3") != 0.0) {
            hsSplitHaystackUtil.readPointValue("economizer and analog3")
        } else if (hsSplitHaystackUtil.readPointValue("economizer and analog4") != 0.0) {
            hsSplitHaystackUtil.readPointValue("economizer and analog4")
        } else {
            defaultFanLoopOutput
        }
    }

    private fun getHeatingStateActivated (): Double {
        return if (stageActive("heating and runtime and stage3")) {
            hsSplitHaystackUtil.readPointValue("fan and heating and stage3")
        } else if (stageActive("heating and runtime and stage2")) {
            hsSplitHaystackUtil.readPointValue("fan and heating and stage2")
        } else if (stageActive("heating and runtime and stage1")){
            hsSplitHaystackUtil.readPointValue("fan and heating and stage1")
        } else {
            defaultFanLoopOutput
        }
    }

    // Get the lowest cooling stage activated
    private fun getLowestCoolingStateActivated (): Double {
        return if (hsSplitHaystackUtil.readPointValue("fan and cooling and stage1") != 0.0) {
            hsSplitHaystackUtil.readPointValue("fan and cooling and stage1")
        } else if (hsSplitHaystackUtil.readPointValue("fan and cooling and stage2") != 0.0) {
            hsSplitHaystackUtil.readPointValue("fan and cooling and stage2")
        } else if (hsSplitHaystackUtil.readPointValue("fan and cooling and stage3") != 0.0) {
            hsSplitHaystackUtil.readPointValue("fan and cooling and stage3")
        } else {
            defaultFanLoopOutput
        }
    }

    private fun getLowestHeatingStateActivated (): Double {
        return if (hsSplitHaystackUtil.readPointValue("fan and heating and stage1") != 0.0) {
            hsSplitHaystackUtil.readPointValue("fan and heating and stage1")
        } else if (hsSplitHaystackUtil.readPointValue("fan and heating and stage2") != 0.0) {
            hsSplitHaystackUtil.readPointValue("fan and heating and stage2")
        } else if (hsSplitHaystackUtil.readPointValue("fan and heating and stage3") != 0.0) {
            hsSplitHaystackUtil.readPointValue("fan and heating and stage3")
        } else {
            defaultFanLoopOutput
        }
    }

    private fun stageActive(fanStage: String): Boolean {
        return hsSplitHaystackUtil.readHisVal(fanStage) == 1.0
    }

    private fun isEconomizerActive(): Boolean {
        return hsSplitHaystackUtil.readHisVal("economizing and loop and output") > 0.0
    }

    private fun getPercentageFromVoltageSelected(voltageSelected: Int): Int {
        val minVoltage = 0
        val maxVoltage = 10
        return (((voltageSelected - minVoltage).toDouble() / (maxVoltage - minVoltage)) * 100).roundToInt()
    }

    override fun isHeatingActive(): Boolean {
        return stageActive("heating and runtime and stage1")
                || stageActive("heating and runtime and stage2")
                || stageActive("heating and runtime and stage3")
                || hsSplitHaystackUtil.readHisVal("modulating and heating and output and not loop") > 0.0
    }

    override fun isCoolingActive(): Boolean {
        return stageActive("cooling and runtime and stage1")
                || stageActive("cooling and runtime and stage2")
                || stageActive("cooling and runtime and stage3")
                || hsSplitHaystackUtil.readHisVal("modulating and cooling and output and not loop") > 0.0
    }

}