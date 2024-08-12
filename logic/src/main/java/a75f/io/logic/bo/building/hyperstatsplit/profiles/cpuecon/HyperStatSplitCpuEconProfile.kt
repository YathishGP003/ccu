package a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.EpidemicState
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstatsplit.common.BasicSettings
import a75f.io.logic.bo.building.hyperstatsplit.common.FanModeCacheStorage
import a75f.io.logic.bo.building.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getActualFanMode
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitProfileTuners
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperstatSplitLoopController
import a75f.io.logic.bo.building.hyperstatsplit.common.UserIntents
import a75f.io.logic.bo.building.hyperstatsplit.profiles.HyperStatSplitPackageUnitProfile
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.logic.jobs.HyperStatSplitUserIntentHandler
import a75f.io.logic.util.PreferenceUtil
import com.fasterxml.jackson.annotation.JsonIgnore
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlin.math.pow
import kotlin.math.roundToInt


/**
 * @author tcase@75f.io (HyperStat CPU)
 * Created on 7/7/21.
 *
 * Created for HyperStat Split CPU/Econ by Nick P on 07-24-2023.
 */
class HyperStatSplitCpuEconProfile(equipRef: String, nodeAddress: Short) : HyperStatSplitPackageUnitProfile(equipRef, nodeAddress) {

    companion object {

        fun getAirEnthalpy(temp: Double, humidity: Double): Double {

            val A = 0.007468 * temp.pow(2.0) - 0.4344 * temp + 11.176
            val B = 0.2372 * temp + 0.1230
            val H = A * 0.01 * humidity + B

            CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "temperature $temp humidity $humidity Enthalpy: $H")
            return CCUUtils.roundToTwoDecimal(H)

        }

    }

    private val equipRef : String = equipRef

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

    private lateinit var curState: ZoneState
    private var epidemicState = EpidemicState.OFF
    private var prePurgeEnabled = false
    private var prePurgeOpeningValue = 0.0

    override fun getProfileType() = ProfileType.HYPERSTATSPLIT_CPU

    override fun updateZonePoints() {

        if (Globals.getInstance().isTestMode) {
            logIt("Test mode is on: ${nodeAddress}")
            return
        }

        if (mInterface != null) mInterface.refreshView()
        val relayStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()

        if(isRFDead){
            handleRFDead()
            return
        } else if (isZoneDead) {
            handleDeadZone()
            return
        }
        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode

        val config = domainProfileConfiguration

        val hyperStatSplitTuners = fetchHyperStatSplitTuners()
        val userIntents = fetchUserIntents()
        val averageDesiredTemp = getAverageTemp(userIntents)

        val exhaustFanStage1Threshold = hssEquip.exhaustFanStage1Threshold.readDefaultVal().toInt()
        val exhaustFanStage2Threshold = hssEquip.exhaustFanStage2Threshold.readDefaultVal().toInt()
        val exhaustFanHysteresis = hssEquip.exhaustFanHysteresis.readDefaultVal().toInt()
        val outsideDamperMinOpen = getEffectiveOutsideDamperMinOpen()

        val fanModeSaved = FanModeCacheStorage().getFanModeFromCache(equipRef)
        val actualFanMode = getActualFanMode(nodeAddress.toString(), fanModeSaved)

        val isCondensateTripped : Boolean = hssEquip.condensateStatusNC.readHisVal() > 0.0 || hssEquip.condensateStatusNO.readHisVal() > 0.0
        if (isCondensateTripped) CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "Condensate overflow detected")

        // At this point, Conditioning Mode will be set to OFF if Condensate Overflow is detected
        // It will revert to previous value when Condensate returns to normal
        val basicSettings = fetchBasicSettings(wasCondensateTripped, isCondensateTripped)

        val updatedFanMode = fallBackFanMode(equipRef, fanModeSaved, actualFanMode, basicSettings)
        basicSettings.fanMode = updatedFanMode

        hyperstatSplitCPUEconAlgorithm.initialise(tuners = hyperStatSplitTuners)
        hyperstatSplitCPUEconAlgorithm.dumpLogs()
        handleChangeOfDirection(userIntents)

        prePurgeEnabled = hssEquip.prePurgeEnable.readDefaultVal() > 0.0
        prePurgeOpeningValue = hssEquip.prePurgeOutsideDamperOpen.readDefaultVal()

        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        economizingLoopOutput = 0
        dcvLoopOutput = 0
        outsideAirCalculatedMinDamper = 0
        outsideAirLoopOutput = 0
        outsideAirFinalLoopOutput = 0
        evaluateLoopOutputs(userIntents, basicSettings, hyperStatSplitTuners, isCondensateTripped, outsideDamperMinOpen)

        if (hssEquip.zoneOccupancy.readHisVal() > 0.0 && hssEquip.occupancyDetection.pointExists()) {
            // That pointExists() call above was NOT redundant.
            // Accessing the .id property does not actually query for the ID. One of the methods on the Point() object needs to be called first to do this.
            haystack.writeHisValueByIdWithoutCOV(hssEquip.occupancyDetection.id, 1.0)
        }

        hssEquip.coolingLoopOutput.writeHisVal(coolingLoopOutput.toDouble())
        hssEquip.heatingLoopOutput.writeHisVal(heatingLoopOutput.toDouble())
        hssEquip.fanLoopOutput.writeHisVal(fanLoopOutput.toDouble())

        hssEquip.economizingLoopOutput.writeHisVal(economizingLoopOutput.toDouble())
        hssEquip.dcvLoopOutput.writeHisVal(dcvLoopOutput.toDouble())
        hssEquip.outsideAirLoopOutput.writeHisVal(outsideAirLoopOutput.toDouble())
        hssEquip.outsideAirFinalLoopOutput.writeHisVal(outsideAirFinalLoopOutput.toDouble())

        val currentOperatingMode = hssEquip.occupancyMode.readHisVal().toInt()

        updateTitle24LoopCounter(hyperStatSplitTuners, basicSettings)

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
            runAnalogOutOperations(config, basicSettings, analogOutStages, isCondensateTripped)
        } else{
            resetAllLogicalPointValues()
        }
        setOperatingMode(currentTemp,averageDesiredTemp,basicSettings)

        if (hssEquip.equipStatus.readHisVal().toInt() != curState.ordinal)
            hssEquip.equipStatus.writeHisVal(curState.ordinal.toDouble())

        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY

        logIt("Equip Running : $curState")

        HyperStatSplitUserIntentHandler.updateHyperStatSplitStatus(
            hssEquip.getId(), relayStages, analogOutStages, temperatureState,
            economizingLoopOutput, dcvLoopOutput, outsideDamperMinOpen, outsideAirFinalLoopOutput,
            if (hssEquip.condensateStatusNC.readHisVal() > 0.0 || hssEquip.condensateStatusNO.readHisVal() > 0.0) 1.0 else 0.0,
            if (hssEquip.filterStatusNC.readHisVal() > 0.0 || hssEquip.filterStatusNO.readHisVal() > 0.0) 1.0 else 0.0,
            basicSettings, epidemicState
        )

        wasCondensateTripped = isCondensateTripped

        CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "processHyperStatSplitCpuEconProfile() complete")
        updateTitle24Flags(basicSettings)

    }

    override fun getDomainProfileConfiguration() : HyperStatSplitCpuProfileConfiguration {
        return HyperStatSplitCpuProfileConfiguration(
            nodeAddress.toInt(),
            NodeType.HYPERSTATSPLIT.toString(),
            ZonePriority.NONE.ordinal,
            equip?.roomRef ?: "",
            equip?.floorRef ?: "",
            ProfileType.HYPERSTATSPLIT_CPU,
            ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
        ).getActiveConfiguration()
    }

    private fun getEffectiveOutsideDamperMinOpen(): Int {
        var outsideDamperMinOpenFromConditioning : Double
        if (isHeatingActive() || isCoolingActive() || hssEquip.economizingLoopOutput.readHisVal() > 0.0) {
            outsideDamperMinOpenFromConditioning = hssEquip.outsideDamperMinOpenDuringConditioning.readDefaultVal()
        } else {
            outsideDamperMinOpenFromConditioning = hssEquip.outsideDamperMinOpenDuringRecirculation.readDefaultVal()
        }

        var outsideDamperMinOpenFromFanStage : Double = 0.0
        if (hssEquip.fanHighSpeed.readHisVal() > 0.0
            || hssEquip.fanOpMode.readPriorityVal().toInt() == StandaloneFanStage.HIGH_ALL_TIME.ordinal
            || hssEquip.fanOpMode.readPriorityVal().toInt() == StandaloneFanStage.HIGH_CUR_OCC.ordinal
            || hssEquip.fanOpMode.readPriorityVal().toInt() == StandaloneFanStage.HIGH_OCC.ordinal
        ) {
            outsideDamperMinOpenFromFanStage = hssEquip.outsideDamperMinOpenDuringFanHigh.readDefaultVal()
        } else if (hssEquip.fanMediumSpeed.readHisVal() > 0.0
            || hssEquip.fanOpMode.readPriorityVal().toInt() == StandaloneFanStage.MEDIUM_ALL_TIME.ordinal
            || hssEquip.fanOpMode.readPriorityVal().toInt() == StandaloneFanStage.MEDIUM_CUR_OCC.ordinal
            || hssEquip.fanOpMode.readPriorityVal().toInt() == StandaloneFanStage.MEDIUM_OCC.ordinal
        ) {
            outsideDamperMinOpenFromFanStage = hssEquip.outsideDamperMinOpenDuringFanMedium.readDefaultVal()
        } else if (hssEquip.fanLowSpeed.readHisVal() > 0.0
            || hssEquip.fanOpMode.readPriorityVal().toInt() == StandaloneFanStage.LOW_ALL_TIME.ordinal
            || hssEquip.fanOpMode.readPriorityVal().toInt() == StandaloneFanStage.LOW_CUR_OCC.ordinal
            || hssEquip.fanOpMode.readPriorityVal().toInt() == StandaloneFanStage.LOW_OCC.ordinal
        ) {
            outsideDamperMinOpenFromFanStage = hssEquip.outsideDamperMinOpenDuringFanLow.readDefaultVal()
        }

        val zoneId = HSUtil.getZoneIdFromEquipId(equip?.id)
        val occ = ScheduleManager.getInstance().getOccupiedModeCache(zoneId)
        val isOccupied = occ?.isOccupied ?: false
        return  if (isOccupied && (hssEquip.conditioningMode.readPriorityVal().toInt() != StandaloneConditioningMode.OFF.ordinal)) {
            Math.max(outsideDamperMinOpenFromConditioning.toInt(), outsideDamperMinOpenFromFanStage.toInt())
        } else {
            0
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

    private fun evaluateLoopOutputs(userIntents: UserIntents, basicSettings: BasicSettings, hyperStatSplitTuners: HyperStatSplitProfileTuners, isCondensateTripped: Boolean, effectiveOutsideDamperMinOpen: Int){

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

        evaluateOAOLoop(basicSettings, isCondensateTripped, effectiveOutsideDamperMinOpen)

    }

    private fun evaluateOAOLoop(basicSettings: BasicSettings, isCondensateTripped: Boolean, effectiveOutsideDamperMinOpen: Int) {

        // If there's not an OAO damper mapped,
        if (!HyperStatSplitAssociationUtil.isAnyAnalogAssociatedToOAO(domainProfileConfiguration)) {

            economizingAvailable = false
            economizingLoopOutput = 0
            dcvAvailable = false
            dcvLoopOutput = 0
            outsideAirLoopOutput = 0
            matThrottle = false
            outsideAirFinalLoopOutput = 0

            // Even if there's not an OAO damper, still calculate enthalpy for portal widgets
            val externalTemp = CCUHsApi.getInstance().readHisValByQuery("system and outside and temp and not lockout")
            val externalHumidity =
                CCUHsApi.getInstance().readHisValByQuery("system and outside and humidity")

            val indoorTemp = hssEquip.currentTemp.readHisVal()
            val indoorHumidity = hssEquip.zoneHumidity.readHisVal()

            val insideEnthalpy = getAirEnthalpy(indoorTemp, indoorHumidity)
            val outsideEnthalpy = getAirEnthalpy(externalTemp, externalHumidity)

            hssEquip.insideEnthalpy.writeHisVal(insideEnthalpy)
            hssEquip.outsideEnthalpy.writeHisVal(outsideEnthalpy)

        } else {

            val oaoDamperMatTarget = hssEquip.standaloneOutsideDamperMixedAirTarget.readPriorityVal()
            val oaoDamperMatMin = hssEquip.standaloneOutsideDamperMixedAirMinimum.readPriorityVal()
            val economizingMaxTemp = hssEquip.standaloneEconomizingMaxTemperature.readPriorityVal()

            val matTemp  = hssEquip.mixedAirTemperature.readHisVal()

            handleSmartPrePurgeControl()
            doEconomizing()
            doDcv(effectiveOutsideDamperMinOpen)

            outsideAirLoopOutput = if (epidemicState == EpidemicState.PREPURGE) {
                Math.max(economizingLoopOutput, outsideAirCalculatedMinDamper)
            } else {
                Math.max(economizingLoopOutput, dcvLoopOutput)
            }

            CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON,"outsideAirLoopOutput "+outsideAirLoopOutput+" oaoDamperMatTarget "+oaoDamperMatTarget+" oaoDamperMatMin "+oaoDamperMatMin
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
                CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "Condensate Switch is tripped. outsideAirFinalLoopOutput set to zero.")
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

                CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "Conditioning Mode is HEAT-ONLY or OFF. outsideAirFinalLoopOutput to consider DCV only.")

            }

            CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON," economizingLoopOutput "+economizingLoopOutput+" dcvLoopOutput "+dcvLoopOutput
                    +" outsideAirFinalLoopOutput "+outsideAirFinalLoopOutput+" effectiveOutsideDamperMinOpen "+effectiveOutsideDamperMinOpen)

            hssEquip.outsideAirFinalLoopOutput.writeHisVal(outsideAirFinalLoopOutput.toDouble())
            hssEquip.oaoDamper.writeHisVal(outsideAirFinalLoopOutput.toDouble())

            val matThrottleNumber = if (matThrottle) 1.0 else 0.0
            hssEquip.matThrottle.writeHisVal(matThrottleNumber)

        }

    }

    private fun doEconomizing() {

        val externalTemp = CCUHsApi.getInstance().readHisValByQuery("system and outside and temp and not lockout")
        val externalHumidity =
            CCUHsApi.getInstance().readHisValByQuery("system and outside and humidity")

        // Check for economizer enable
        if (canDoEconomizing(externalTemp, externalHumidity)) {

            economizingAvailable = true

            val economizingToMainCoolingLoopMap = hssEquip.standaloneEconomizingToMainCoolingLoopMap.readPriorityVal()

            val numberConfiguredCoolingStages = getNumberConfiguredCoolingStages()

            if (numberConfiguredCoolingStages > 0) {
                economizingLoopOutput = Math.min((coolingLoopOutput * (numberConfiguredCoolingStages + 1)), 100)
                CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, (numberConfiguredCoolingStages+1).toString() + " cooling stages available (including economizer); economizingLoopOutput = " + economizingLoopOutput)
            } else {
                CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "coolingLoopOutput = $coolingLoopOutput, economizingToMainCoolingLoopMap = $economizingToMainCoolingLoopMap, economizingLoopOutput = $economizingLoopOutput")
                economizingLoopOutput = Math.min((coolingLoopOutput * (100 / economizingToMainCoolingLoopMap)).toInt(), 100)
            }

        } else {
            economizingAvailable = false
            economizingLoopOutput = 0
        }

        val economizingAvailableNumber = if (economizingAvailable) 1.0 else 0.0
        hssEquip.economizingAvailable.writeHisVal(economizingAvailableNumber)

    }

    /*
        Right now, it is possible to configure stages irregularly. (e.g. Stage 1 and Stage 3, but not Stage 2).

        If a user did this, it would mess with this mapping, but this will be far from the biggest operational issue
        introduced.

        We should probably validate against these kinds of configurations in the future.
     */
    private fun getNumberConfiguredCoolingStages(): Int {
        if (HyperStatSplitAssociationUtil.isAnyRelayAssociatedToCoolingStage3(domainProfileConfiguration)) return 3
        if (HyperStatSplitAssociationUtil.isAnyRelayAssociatedToCoolingStage2(domainProfileConfiguration)) return 2
        if (HyperStatSplitAssociationUtil.isAnyRelayAssociatedToCoolingStage1(domainProfileConfiguration)) return 1
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
    private fun canDoEconomizing(externalTemp: Double, externalHumidity: Double): Boolean {

        val economizingMinTemp = hssEquip.standaloneEconomizingMinTemperature.readPriorityVal()

        val indoorTemp = hssEquip.currentTemp.readHisVal()
        val indoorHumidity = hssEquip.zoneHumidity.readHisVal()

        val insideEnthalpy = getAirEnthalpy(indoorTemp, indoorHumidity)
        val outsideEnthalpy = getAirEnthalpy(externalTemp, externalHumidity)

        hssEquip.insideEnthalpy.writeHisVal(insideEnthalpy)
        hssEquip.outsideEnthalpy.writeHisVal(outsideEnthalpy)

        CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, " canDoEconomizing externalTemp $externalTemp externalHumidity $externalHumidity")

        // If zone isn't in cooling mode, stop right here
        if (state != ZoneState.COOLING) return false

        // First, check local dry-bulb temp
        if (isEconomizingEnabledOnDryBulb(externalTemp, externalHumidity, economizingMinTemp)) {
            CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "Economizer enabled based on dry-bulb temperature.")
            return true
        }

        if (!isEconomizingTempAndHumidityInRange(externalTemp, externalHumidity, economizingMinTemp)) return false

        if (isEconomizingEnabledOnEnthalpy(insideEnthalpy, outsideEnthalpy)) {
            CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "Economizing enabled based on enthalpy.")
            return true
        }

        CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "Economizing disabled based on enthalpy.")
        return false

    }

    /*
        Same dry-bulb enable logic as OAO Profile.

        If outsideTemp is between economizingMinTemp (0°F, adj.) and dryBulbTemperatureThreshold (55°F, adj.), then enable economizing.
        (for outsideTemp, start with systemOutsideTemp, and use Outside Air Temperature sensor if systemOutsideTemp is 0)
     */
    private fun isEconomizingEnabledOnDryBulb(externalTemp: Double, externalHumidity: Double, economizingMinTemp: Double): Boolean {

        val dryBulbTemperatureThreshold = hssEquip.standaloneEconomizingDryBulbThreshold.readPriorityVal()

        var outsideAirTemp = externalTemp

        if (externalHumidity == 0.0 && externalTemp == 0.0) {
            CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "System outside temp and humidity are both zero; using local OAT sensor");
            outsideAirTemp = hssEquip.outsideTemperature.readHisVal()
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
    private fun isEconomizingTempAndHumidityInRange(externalTemp: Double, externalHumidity: Double, economizingMinTemp: Double): Boolean {

        CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "Checking outside temp and humidity against tuner min/max thresholds")

        val economizingMaxTemp = hssEquip.standaloneEconomizingMaxTemperature.readPriorityVal()
        val economizingMinHumidity = hssEquip.standaloneEconomizingMinHumidity.readPriorityVal()
        val economizingMaxHumidity = hssEquip.standaloneEconomizingMaxHumidity.readPriorityVal()

        var outsideTemp = externalTemp
        var outsideHumidity = externalHumidity

        if (
            outsideTemp == 0.0 &&
            outsideHumidity == 0.0 &&
            HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToOutsideAir(domainProfileConfiguration)
        ) {
            outsideTemp = hssEquip.outsideTemperature.readHisVal()
            outsideHumidity = hssEquip.outsideHumidity.readHisVal()
        }

        if (outsideTemp > economizingMinTemp
            && outsideTemp < economizingMaxTemp
            && outsideHumidity > economizingMinHumidity
            && outsideHumidity < economizingMaxHumidity) return true

        CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "Outside air ($outsideTemp°F, $outsideHumidity%RH) out of temp/humidity range from tuners; economizing disabled")

        return false

    }

    /*
        Same enthalpy-enable condition as OAO Profile.

        If systemOutsideEnthalpy < insideEnthalpy + enthalpyDuctCompensationOffset (0 BTU/lb, adj.), enable economizing
        (Start with systemOutsideEnthalpy. If it's not available and OAT/H sensor is on sensor bus, then calculate outsideEnthalpy
        based on sensed Outside Air Temperature & Humidity.
     */
    private fun isEconomizingEnabledOnEnthalpy (insideEnthalpy: Double, outsideEnthalpy: Double): Boolean {

        CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "Checking enthalpy-enable condition: insideEnthalpy $insideEnthalpy, outsideEnthalpy $outsideEnthalpy")

        var outsideEnthalpyToUse = outsideEnthalpy

        // Our enthalpy calc returns a value of 0.12 for zero temp and humidity.
        // We will assume anything less than 1 translates to system weather data being dead
        if (
            outsideEnthalpy < 1 &&
            HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToOutsideAir(domainProfileConfiguration)
        ) {
            CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON,"System outside temp and humidity are both zero; using local outside air temp/humidity sensor")
            val sensorBusOutsideTemp = hssEquip.outsideTemperature.readHisVal()
            val sensorBusOutsideHumidity = hssEquip.outsideHumidity.readHisVal()
            outsideEnthalpyToUse = getAirEnthalpy(sensorBusOutsideTemp, sensorBusOutsideHumidity)
        }

        val enthalpyDuctCompensationOffset = hssEquip.standaloneEnthalpyDuctCompensationOffset.readPriorityVal()

        return insideEnthalpy > outsideEnthalpyToUse + enthalpyDuctCompensationOffset

    }

    /*
        DCV is enabled when:
            ○ Zone is in Occupied Mode AND
            ○ Zone CO2 (sensed on HyperLite) > Zone CO2 Threshold
     */
    private fun doDcv(standaloneOutsideAirDamperMinOpen: Int) {

        dcvAvailable = false
        var zoneSensorCO2 = hssEquip.zoneCO2.readHisVal()
        var zoneCO2Threshold = hssEquip.co2Threshold.readDefaultVal()
        var co2DamperOpeningRate = hssEquip.co2DamperOpeningRate.readDefaultVal()
        CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "zoneSensorCO2: " + zoneSensorCO2 + ", zoneCO2Threshold: " + zoneCO2Threshold + ", co2DamperOpeningRate: " + co2DamperOpeningRate)
        if (occupancyStatus == Occupancy.OCCUPIED || occupancyStatus == Occupancy.FORCEDOCCUPIED || occupancyStatus == Occupancy.AUTOFORCEOCCUPIED) {
            if (zoneSensorCO2 > zoneCO2Threshold) {
                dcvAvailable = true
                dcvLoopOutput = Math.max(0, Math.min(((zoneSensorCO2 - zoneCO2Threshold) / co2DamperOpeningRate).toInt(), 100))
                outsideAirCalculatedMinDamper = Math.max(dcvLoopOutput, standaloneOutsideAirDamperMinOpen)
            } else {
                outsideAirCalculatedMinDamper = standaloneOutsideAirDamperMinOpen
            }
        } else if (epidemicState != EpidemicState.PREPURGE) {
            outsideAirCalculatedMinDamper = 0
        }

        val dcvAvailableNum = if (dcvAvailable) 1.0 else 0.0
        hssEquip.dcvAvailable.writeHisVal(dcvAvailableNum)
        hssEquip.dcvLoopOutput.writeHisVal(dcvLoopOutput.toDouble())
        hssEquip.outsideAirCalculatedMinDamper.writeHisVal(outsideAirCalculatedMinDamper.toDouble())

    }

    private fun fetchBasicSettings(wasCondensateTripped: Boolean, isCondensateTripped: Boolean): BasicSettings {

        /*
            When tripped, condensate sensor will force Conditioning Mode to OFF.
            The last Conditioning Mode set by the user is stored in the var lastUserIntentConditioningMode.
         */

        val appWasJustRestarted = true

        if (isCondensateTripped) {

            // If Condensate just tripped, store whatever the previous Conditioning Mode was as UserIntentConditioningMode.
            // Conditioning Mode will return to this value once condensate returns to normal. User will not be able to change it back until this happens.
            if (!wasCondensateTripped && !appWasJustRestarted)  PreferenceUtil.setLastUserIntentConditioningMode(StandaloneConditioningMode.values()[hssEquip.conditioningMode.readHisVal().toInt()]);

            hssEquip.conditioningMode.writeDefaultVal(StandaloneConditioningMode.OFF.ordinal.toDouble())
            return BasicSettings(
                conditioningMode = StandaloneConditioningMode.OFF,
                fanMode = StandaloneFanStage.values()[hssEquip.fanOpMode.readPriorityVal().toInt()]
            )

        } else {

            /*
                If condensate is not tripped, user is allowed to change the Conditioning Mode.
                Any changes made to the Haystack point (this can be done via CCU UI or at device)
                will be saved to lastUserIntentConditioningMode.
             */
            if (!wasCondensateTripped) PreferenceUtil.setLastUserIntentConditioningMode(StandaloneConditioningMode.values()[hssEquip.conditioningMode.readHisVal().toInt()])
            hssEquip.conditioningMode.writeDefaultVal(PreferenceUtil.getLastUserIntentConditioningMode().ordinal.toDouble())
            return BasicSettings(
                conditioningMode = StandaloneConditioningMode.values()[hssEquip.conditioningMode.readPriorityVal().toInt()],
                fanMode = StandaloneFanStage.values()[hssEquip.fanOpMode.readPriorityVal().toInt()]
            )

        }
    }

    private fun fetchUserIntents(): UserIntents {
        return UserIntents(
            currentTemp = hssEquip.currentTemp.readHisVal(),
            zoneCoolingTargetTemperature = hssEquip.desiredTempCooling.readPriorityVal(),
            zoneHeatingTargetTemperature = hssEquip.desiredTempHeating.readPriorityVal(),
            targetMinInsideHumidity = hssEquip.targetHumidifier.readPriorityVal(),
            targetMaxInsideHumidity = hssEquip.targetDehumidifier.readPriorityVal(),
        )
    }

    private fun fetchHyperStatSplitTuners(): HyperStatSplitProfileTuners {

        /**
         * Consider that
         * proportionalGain = proportionalKFactor
         * integralGain = integralKFactor
         * proportionalSpread = temperatureProportionalRange
         * integralMaxTimeout = temperatureIntegralTime
         */

        val hsSplitTuners = HyperStatSplitProfileTuners()
        hsSplitTuners.proportionalGain = hssEquip.standaloneProportionalKFactor.readPriorityVal()
        hsSplitTuners.integralGain = hssEquip.standaloneIntegralKFactor.readPriorityVal()
        hsSplitTuners.proportionalSpread = hssEquip.standaloneTemperatureProportionalRange.readPriorityVal()
        hsSplitTuners.integralMaxTimeout = hssEquip.standaloneTemperatureIntegralTime.readPriorityVal().toInt()
        hsSplitTuners.relayActivationHysteresis = hssEquip.standaloneRelayActivationHysteresis.readPriorityVal().toInt()
        hsSplitTuners.analogFanSpeedMultiplier = hssEquip.standaloneAnalogFanSpeedMultiplier.readPriorityVal()
        hsSplitTuners.humidityHysteresis = hssEquip.standaloneHumidityHysteresis.readPriorityVal().toInt()
        hsSplitTuners.minFanRuntimePostConditioning = hssEquip.minFanRuntimePostConditioning.readPriorityVal().toInt()
        return hsSplitTuners
    }

    private fun runRelayOperations(
        config: HyperStatSplitCpuProfileConfiguration, tuner: HyperStatSplitProfileTuners, userIntents: UserIntents,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>,
        isCondensateTripped: Boolean,
        exhaustFanStage1Threshold: Int,
        exhaustFanStage2Threshold: Int,
        exhaustFanHysteresis: Int
    ) {
        if (config.relay1Enabled.enabled) {
            handleRelayState(
                config.relay1Association, config, Port.RELAY_ONE,
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
        if (config.relay2Enabled.enabled) {
            handleRelayState(
                config.relay2Association, config, Port.RELAY_TWO,
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
        if (config.relay3Enabled.enabled) {
            handleRelayState(
                config.relay3Association, config, Port.RELAY_THREE,
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
        if (config.relay4Enabled.enabled) {
            handleRelayState(
                config.relay4Association, config, Port.RELAY_FOUR,
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
        if (config.relay5Enabled.enabled) {
            handleRelayState(
                config.relay5Association, config, Port.RELAY_FIVE,
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
        if (config.relay6Enabled.enabled) {
            handleRelayState(
                config.relay6Association, config, Port.RELAY_SIX,
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
        if (config.relay7Enabled.enabled) {
            handleRelayState(
                config.relay7Association, config, Port.RELAY_SEVEN,
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
        if (config.relay8Enabled.enabled) {
            handleRelayState(
                config.relay8Association, config, Port.RELAY_EIGHT,
                tuner, userIntents, basicSettings, relayStages, isCondensateTripped,
                exhaustFanStage1Threshold, exhaustFanStage2Threshold, exhaustFanHysteresis
            )
        }
    }

    private fun runAnalogOutOperations(
        config: HyperStatSplitCpuProfileConfiguration,
        basicSettings: BasicSettings, analogOutStages: HashMap<String, Int>,
        isCondensateTripped: Boolean
    ) {
        if (config.analogOut1Enabled.enabled) {
            handleAnalogOutState(
                config.analogOut1Association, config, Port.ANALOG_OUT_ONE, basicSettings, analogOutStages, isCondensateTripped
            )
        }
        if (config.analogOut2Enabled.enabled) {
            handleAnalogOutState(
                config.analogOut2Association, config, Port.ANALOG_OUT_TWO, basicSettings, analogOutStages, isCondensateTripped
            )
        }
        if (config.analogOut3Enabled.enabled) {
            handleAnalogOutState(
                config.analogOut3Association, config, Port.ANALOG_OUT_THREE, basicSettings, analogOutStages, isCondensateTripped
            )
        }
        if (config.analogOut4Enabled.enabled) {
            handleAnalogOutState(
                config.analogOut4Association, config, Port.ANALOG_OUT_FOUR, basicSettings, analogOutStages, isCondensateTripped
            )
        }
    }

    // This method is only called after confirming that the output is already present in the config.
    private fun handleRelayState(
        relayAssociation: AssociationConfig, config: HyperStatSplitCpuProfileConfiguration, port: Port, tuner: HyperStatSplitProfileTuners,
        userIntents: UserIntents, basicSettings: BasicSettings, relayStages: HashMap<String, Int>, isCondensateTripped : Boolean,
        exhaustFanStage1Threshold: Int, exhaustFanStage2Threshold: Int, exhaustFanHysteresis: Int
    ) {
        when {
            (HyperStatSplitAssociationUtil.isRelayAssociatedToCoolingStage(relayAssociation)) -> {

                if ((basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.COOL_ONLY.ordinal ||
                    basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal) && !isCondensateTripped
                ) {
                    runRelayForCooling(relayAssociation, port, config, tuner, relayStages)
                } else {
                    if (HyperStatSplitAssociationUtil.isRelayAssociatedToCoolingStage1(relayAssociation)) {
                        hssEquip.coolingStage1.writeHisVal(0.0)
                    } else if (HyperStatSplitAssociationUtil.isRelayAssociatedToCoolingStage2(relayAssociation)) {
                        hssEquip.coolingStage2.writeHisVal(0.0)
                    } else if (HyperStatSplitAssociationUtil.isRelayAssociatedToCoolingStage3(relayAssociation)) {
                        hssEquip.coolingStage3.writeHisVal(0.0)
                    }
                }
            }
            (HyperStatSplitAssociationUtil.isRelayAssociatedToHeatingStage(relayAssociation)) -> {

                if ((basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.HEAT_ONLY.ordinal ||
                    basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal) && !isCondensateTripped
                ) {
                    runRelayForHeating(relayAssociation, port, config, tuner, relayStages)
                } else {
                    if (HyperStatSplitAssociationUtil.isRelayAssociatedToHeatingStage1(relayAssociation)) {
                        hssEquip.heatingStage1.writeHisVal(0.0)
                    } else if (HyperStatSplitAssociationUtil.isRelayAssociatedToHeatingStage2(relayAssociation)) {
                        hssEquip.heatingStage2.writeHisVal(0.0)
                    } else if (HyperStatSplitAssociationUtil.isRelayAssociatedToHeatingStage3(relayAssociation)) {
                        hssEquip.heatingStage3.writeHisVal(0.0)
                    }
                }
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToFan(relayAssociation)) -> {

                if (basicSettings.fanMode != StandaloneFanStage.OFF && !isCondensateTripped) {
                    runRelayForFanSpeed(relayAssociation, port, config, tuner, relayStages, basicSettings,
                        previousFanLoopVal, fanLoopCounter)
                } else {
                   if (HyperStatSplitAssociationUtil.isRelayAssociatedToFanLow(relayAssociation)) {
                        hssEquip.fanLowSpeed.writeHisVal(0.0)
                    } else if (HyperStatSplitAssociationUtil.isRelayAssociatedToFanMedium(relayAssociation)) {
                        hssEquip.fanMediumSpeed.writeHisVal(0.0)
                    } else if (HyperStatSplitAssociationUtil.isRelayAssociatedToFanHigh(relayAssociation)) {
                        hssEquip.fanHighSpeed.writeHisVal(0.0)
                    }
                }
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToFanEnabled(relayAssociation)) -> {
                if (!isCondensateTripped) {
                    doFanEnabled( curState,port, fanLoopOutput )
                } else {
                    hssEquip.fanEnable.writeHisVal(0.0)
                }
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToOccupiedEnabled(relayAssociation)) -> {
                doOccupiedEnabled(port)
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToHumidifier(relayAssociation)) -> {
                if (!isCondensateTripped) {
                    doHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMinInsideHumidity)
                } else {
                    hssEquip.humidifierEnable.writeHisVal(0.0)
                }
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToDeHumidifier(relayAssociation)) -> {
                if (!isCondensateTripped) {
                    doDeHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMaxInsideHumidity)
                } else {
                    hssEquip.dehumidifierEnable.writeHisVal(0.0)
                }
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToExhaustFanStage1(relayAssociation)) -> {
                if (!isCondensateTripped) {
                    doExhaustFanStage1(port, outsideAirFinalLoopOutput, exhaustFanStage1Threshold, exhaustFanHysteresis)
                } else {
                    hssEquip.exhaustFanStage1.writeHisVal(0.0)
                }
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToExhaustFanStage2(relayAssociation)) -> {
                if (!isCondensateTripped) {
                    doExhaustFanStage2(port, outsideAirFinalLoopOutput, exhaustFanStage2Threshold, exhaustFanHysteresis)
                } else {
                    hssEquip.exhaustFanStage2.writeHisVal(0.0)
                }

            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToDcvDamper(relayAssociation)) -> {
                if (!isCondensateTripped) {
                    doDcvDamper(port, dcvLoopOutput, tuner.relayActivationHysteresis)
                } else {
                    hssEquip.dcvDamper.writeHisVal(0.0)
                }

            }
        }
    }

    private fun runRelayForCooling(
        relayAssociation: AssociationConfig,
        whichPort: Port,
        config: HyperStatSplitCpuProfileConfiguration,
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

        logIt(" $whichPort: ${CpuRelayType.values()[relayAssociation.associationVal]}")
        when (relayAssociation.associationVal) {
            CpuRelayType.COOLING_STAGE1.ordinal -> {
                doCoolingStage1(
                    whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, s1threshold, relayStages
                )
            }
            CpuRelayType.COOLING_STAGE2.ordinal -> {
               doCoolingStage2(
                   whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, s2threshold,relayStages
               )
            }
            CpuRelayType.COOLING_STAGE3.ordinal -> {
                doCoolingStage3(
                    whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, s3threshold, relayStages
                )
            }
            else -> {}
        }

        if (hssEquip.coolingStage1.readHisVal() > 0.0 || hssEquip.coolingStage2.readHisVal() > 0.0 || hssEquip.coolingStage3.readHisVal() > 0.0) {
            curState = ZoneState.COOLING
        }

    }

    private fun runRelayForHeating(
        relayAssociation: AssociationConfig, whichPort: Port, config: HyperStatSplitCpuProfileConfiguration,
        tuner: HyperStatSplitProfileTuners, relayStages: HashMap<String, Int>
    ) {
        logIt(" $whichPort: ${CpuRelayType.values()[relayAssociation.associationVal]}")
        when (relayAssociation.associationVal) {
            CpuRelayType.HEATING_STAGE1.ordinal -> {
                doHeatingStage1(
                    whichPort,
                    heatingLoopOutput,
                    tuner.relayActivationHysteresis,
                    relayStages
                )
            }
            CpuRelayType.HEATING_STAGE2.ordinal -> {
                val highestStage = HyperStatSplitAssociationUtil.getHighestHeatingStage(config).ordinal
                val divider = if (highestStage == 4) 50 else 33
                doHeatingStage2(
                    whichPort, heatingLoopOutput, tuner.relayActivationHysteresis,
                    divider, relayStages)
            }
            CpuRelayType.HEATING_STAGE3.ordinal -> {
                doHeatingStage3(
                    whichPort, heatingLoopOutput, tuner.relayActivationHysteresis, relayStages)
            }
            else -> {}
        }

        if (hssEquip.heatingStage1.readHisVal() > 0.0 || hssEquip.heatingStage2.readHisVal() > 0.0 || hssEquip.heatingStage3.readHisVal() > 0.0) {
            curState = ZoneState.HEATING
        }
    }

    private fun runRelayForFanSpeed(
        relayAssociation: AssociationConfig, whichPort: Port, config: HyperStatSplitCpuProfileConfiguration,
        tuner: HyperStatSplitProfileTuners, relayStages: HashMap<String, Int>, basicSettings: BasicSettings,
        previousFanLoopVal: Int, fanProtectionCounter: Int
    ) {
        var localFanLoopOutput = fanLoopOutput
        logIt(" $whichPort: ${CpuRelayType.values()[relayAssociation.associationVal]} runRelayForFanSpeed: ${basicSettings.fanMode}")
        if (basicSettings.fanMode == StandaloneFanStage.AUTO
            && basicSettings.conditioningMode == StandaloneConditioningMode.OFF ) {
            logIt("Cond is Off , Fan is Auto  : ")

            if (HyperStatSplitAssociationUtil.isRelayAssociatedToFanLow(relayAssociation)) {
                hssEquip.fanLowSpeed.writeHisVal(0.0)
            } else if (HyperStatSplitAssociationUtil.isRelayAssociatedToFanMedium(relayAssociation)) {
                hssEquip.fanMediumSpeed.writeHisVal(0.0)
            } else if (HyperStatSplitAssociationUtil.isRelayAssociatedToFanHigh(relayAssociation)) {
                hssEquip.fanHighSpeed.writeHisVal(0.0)
            }

            return
        }
        val highestStage = HyperStatSplitAssociationUtil.getHighestFanStage(config)
        val divider = if (highestStage == CpuRelayType.FAN_MEDIUM_SPEED) 50 else 33
        val fanEnabledMapped = HyperStatSplitAssociationUtil.isAnyRelayAssociatedToFanEnabled(config)
        val lowestStage = HyperStatSplitAssociationUtil.getLowestFanStage(config)

        if (fanEnabledMapped) setFanEnabledStatus(true) else setFanEnabledStatus(false)

        setFanLowestFanLowStatus(false)
        setFanLowestFanMediumStatus(false)
        setFanLowestFanHighStatus(false)
        when(lowestStage) {
            CpuRelayType.FAN_LOW_SPEED -> setFanLowestFanLowStatus(true)
            CpuRelayType.FAN_MEDIUM_SPEED -> setFanLowestFanMediumStatus(true)
            CpuRelayType.FAN_HIGH_SPEED -> setFanLowestFanHighStatus(true)
            else -> {}
        }

        // In order to protect the fan, persist the fan for few cycles when there is a sudden change in
        // occupancy and decrease in fan loop output
        if (fanProtectionCounter > 0) {
            localFanLoopOutput = previousFanLoopVal
        }

        when (relayAssociation.associationVal) {
            CpuRelayType.FAN_LOW_SPEED.ordinal -> {
                doFanLowSpeed(
                    basicSettings.fanMode, localFanLoopOutput,
                    tuner.relayActivationHysteresis, relayStages, divider,
                    epidemicState == EpidemicState.PREPURGE)
            }
            CpuRelayType.FAN_MEDIUM_SPEED.ordinal -> {
                doFanMediumSpeed(
                    basicSettings.fanMode, localFanLoopOutput,
                    tuner.relayActivationHysteresis, divider, relayStages,
                    epidemicState == EpidemicState.PREPURGE)
            }
            CpuRelayType.FAN_HIGH_SPEED.ordinal -> {
                doFanHighSpeed(
                    basicSettings.fanMode, localFanLoopOutput,
                    tuner.relayActivationHysteresis, relayStages,
                    epidemicState == EpidemicState.PREPURGE)
            }
            else -> return
        }
    }

    private fun handleAnalogOutState(
        analogOutAssociation: AssociationConfig, config: HyperStatSplitCpuProfileConfiguration,
        port: Port, basicSettings: BasicSettings, analogOutStages: HashMap<String, Int>, isCondensateTripped: Boolean
    ) {

        val activeAnalogOutVoltage = when (port) {
            Port.ANALOG_OUT_ONE -> config.analogOut1Voltage
            Port.ANALOG_OUT_TWO -> config.analogOut2Voltage
            Port.ANALOG_OUT_THREE -> config.analogOut3Voltage
            Port.ANALOG_OUT_FOUR -> config.analogOut4Voltage
            else -> config.analogOut1Voltage
        }

        // If we are in Auto Away mode we no need to Any analog Operations
        when {
            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToCooling(analogOutAssociation)) -> {
                if (!isCondensateTripped) {
                    doAnalogCooling(port,basicSettings.conditioningMode,analogOutStages,coolingLoopOutput)
                } else{
                    hssEquip.coolingSignal.writeHisVal(0.0)
                }
            }
            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToHeating(analogOutAssociation)) -> {
                if (!isCondensateTripped) {
                    doAnalogHeating(port,basicSettings.conditioningMode,analogOutStages,heatingLoopOutput)
                } else {
                    hssEquip.heatingSignal.writeHisVal(0.0)
                }
            }

            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutAssociation)) -> {
                if (!isCondensateTripped) {
                    doAnalogFanActionCpuEcon(
                        port, activeAnalogOutVoltage, basicSettings.fanMode,
                        basicSettings.conditioningMode, fanLoopOutput, analogOutStages, previousFanLoopVal, fanLoopCounter
                    )
                } else {
                    hssEquip.linearFanSpeed.writeHisVal(0.0)
                }
            }

            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToOaoDamper(analogOutAssociation)) -> {
                if (!isCondensateTripped) {
                    doAnalogOAOAction(
                        port,basicSettings.conditioningMode, analogOutStages, outsideAirFinalLoopOutput
                    )
                } else {
                    hssEquip.oaoDamper.writeHisVal(0.0)
                }
            }

            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToReturnDamper(analogOutAssociation)) -> {
                if (!isCondensateTripped) {
                    doAnalogReturnDamperAction(
                        port,basicSettings.conditioningMode, analogOutStages, outsideAirFinalLoopOutput
                    )
                } else {
                    hssEquip.oaoDamper.writeHisVal(100.0)
                }
            }

            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(analogOutAssociation)) -> {
                if (!isCondensateTripped) {
                    doAnalogStagedFanAction(
                        port, activeAnalogOutVoltage, basicSettings.fanMode, HyperStatSplitAssociationUtil.isAnyRelayAssociatedToFanEnabled(config),
                        basicSettings.conditioningMode, fanLoopOutput, analogOutStages, fanLoopCounter
                    )
                } else {
                    hssEquip.stagedFanSpeed.writeHisVal(0.0)
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
        port: Port,
        analogOutVoltage: AnalogOutVoltage,
        fanMode: StandaloneFanStage,
        conditioningMode: StandaloneConditioningMode,
        fanLoopOutput: Int,
        analogOutStages: HashMap<String, Int>,
        previousFanLoopVal: Int,
        fanProtectionCounter: Int
    ) {

        val fanLowPercent = analogOutVoltage.linearFanAtFanLow.currentVal.toInt()
        val fanMediumPercent = analogOutVoltage.linearFanAtFanMedium.currentVal.toInt()
        val fanHighPercent = analogOutVoltage.linearFanAtFanHigh.currentVal.toInt()

        if (fanMode != StandaloneFanStage.OFF) {
            var fanLoopForAnalog = 0
            if (fanMode == StandaloneFanStage.AUTO) {
                if (conditioningMode == StandaloneConditioningMode.OFF) {
                    hssEquip.linearFanSpeed.writeHisVal(0.0)
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
            hssEquip.linearFanSpeed.writeHisVal(fanLoopForAnalog.toDouble())
            CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "$port = Linear Fan Speed  analogSignal   $fanLoopForAnalog")
        }
    }

    private fun isCoolingStateActivated (): Boolean {
        return hssEquip.coolingStage3.readHisVal() > 0 ||
                hssEquip.coolingStage2.readHisVal() > 0 ||
                hssEquip.coolingStage1.readHisVal() > 0
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
        analogOutVoltage: AnalogOutVoltage,
        fanMode: StandaloneFanStage,
        fanEnabledMapped: Boolean,
        conditioningMode: StandaloneConditioningMode,
        fanLoopOutput: Int,
        analogOutStages: HashMap<String, Int>,
        fanProtectionCounter: Int
    ) {

        val fanLowPercent = analogOutVoltage.linearFanAtFanLow.currentVal.toInt()
        val fanMediumPercent = analogOutVoltage.linearFanAtFanMedium.currentVal.toInt()
        val fanHighPercent = analogOutVoltage.linearFanAtFanHigh.currentVal.toInt()

        var fanLoopForAnalog = 0
        var logMsg = "" // This will be overwritten based on priority
        if (fanMode != StandaloneFanStage.OFF) {
            if (fanMode == StandaloneFanStage.AUTO) {
                if (conditioningMode == StandaloneConditioningMode.OFF) {
                    hssEquip.stagedFanSpeed.writeHisVal(0.0)
                    return
                }
                fanLoopForAnalog = fanLoopOutput
                if (conditioningMode == StandaloneConditioningMode.AUTO) {
                    if (hssEquip.operatingMode.readHisVal() == 1.0) {
                        fanLoopForAnalog = getCoolingActivatedAnalogVoltage(fanEnabledMapped, fanLoopOutput)
                        logMsg = "Cooling"
                    } else if (hssEquip.operatingMode.readHisVal() == 2.0) {
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
                if(fanProtectionCounter > 0 && fanLoopForAnalog < previousFanLoopValStaged) {
                    fanLoopForAnalog = previousFanLoopValStaged
                    logMsg = "Fan Protection"
                }
                else previousFanLoopValStaged = fanLoopForAnalog // else indicates we are not in protection mode, so store the fanLoopForAnalog value for protection mode

                // Check Dead-band condition
                if (fanLoopOutput == 0 && fanProtectionCounter == 0 && checkIfInOccupiedMode()) { // When in dead-band, set the fan-loopForAnalog to the recirculate analog value
                    fanLoopForAnalog = getPercentageFromVoltageSelected(getAnalogRecirculateValueActivated().roundToInt())
                    logMsg = "Deadband"
                }
                // The speed at which fan operates during economization is determined by configuration parameter - "Analog-Out During Economizer"
                // We also need to ensure that the currently no cooling stage is activated since we switch on Economization Value only when the
                // cooling stage is not activated
                val analogEconomizerValueActivated = getPercentageFromVoltageSelected(getAnalogEconomizerValueActivated().roundToInt())
                if(economizingLoopOutput != 0 && !isCoolingStateActivated() && fanProtectionCounter == 0) {
                    logMsg = "Economization"
                    fanLoopForAnalog = analogEconomizerValueActivated
                }
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
            hssEquip.stagedFanSpeed.writeHisVal(fanLoopForAnalog.toDouble())
            CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "$port = Staged Fan Speed($logMsg)  analogSignal  $fanLoopForAnalog")
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
            hssEquip.coolingSignal.writeHisVal(coolingLoopOutput.toDouble())
            if (coolingLoopOutput > 0) {
                analogOutStages[AnalogOutput.COOLING.name] = coolingLoopOutput
                curState = ZoneState.COOLING
            }
        } else {
            hssEquip.coolingSignal.writeHisVal(0.0)
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
            hssEquip.heatingSignal.writeHisVal(heatingLoopOutput.toDouble())
            if (heatingLoopOutput > 0) {
                analogOutStages[AnalogOutput.HEATING.name] = heatingLoopOutput
                curState = ZoneState.HEATING
            }
        } else {
            hssEquip.heatingSignal.writeHisVal(0.0)
        }
    }

    /**
     * Handles the smart pre-purge control for the given HyperStatSplitCpuEconEquip.
     * If pre-purge is active, this method sets the value of outsideAirCalculatedMinDamper
     *
     * @param equip The HyperStatSplitCpuEconEquip for which smart pre-purge control is to be handled.
     * @param basicSettings The conditioning and fan settings for the given HyperStatSplitCpuEconEquip.
     *
     */
    private fun handleSmartPrePurgeControl() {
        if(!prePurgeEnabled) {
            epidemicState = EpidemicState.OFF
            return
        }
        val prePurgeRunTime = hssEquip.standalonePrePurgeRuntimeTuner.readPriorityVal()
        val prePurgeOccupiedTimeOffset: Double = hssEquip.standalonePrePurgeOccupiedTimeOffsetTuner.readPriorityVal()

        val oaoDamperMatMin = hssEquip.standaloneOutsideDamperMixedAirMinimum.readPriorityVal()
        val matTemp  = hssEquip.mixedAirTemperature.readHisVal()
        val zoneId = equip?.getRoomRef() ?: ""
        val occuStatus = ScheduleManager.getInstance().getOccupiedModeCache(zoneId)
        val minutesToOccupancy =
            if (occuStatus != null) occuStatus.millisecondsUntilNextChange.toInt() / 60000 else -1
        if (minutesToOccupancy > 0 && prePurgeOccupiedTimeOffset >= minutesToOccupancy && minutesToOccupancy >= prePurgeOccupiedTimeOffset - prePurgeRunTime &&
            matTemp > oaoDamperMatMin && hssEquip.conditioningMode.readPriorityVal().toInt() != StandaloneConditioningMode.OFF.ordinal && hssEquip.fanOpMode.readPriorityVal().toInt() != StandaloneFanStage.OFF.ordinal) {
            outsideAirCalculatedMinDamper = hssEquip.prePurgeOutsideDamperOpen.readPriorityVal().toInt()
            epidemicState = EpidemicState.PREPURGE
            hssEquip.prePurgeStatus.writeHisVal(1.0)
        } else {
            epidemicState = EpidemicState.OFF
            hssEquip.prePurgeStatus.writeHisVal(0.0)
        }
    }


    fun doAnalogOAOAction(
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        outsideAirFinalLoopOutput: Int
    ) {
        hssEquip.oaoDamper.writeHisVal(outsideAirFinalLoopOutput.toDouble())
        if (outsideAirFinalLoopOutput > 0) analogOutStages[AnalogOutput.OAO_DAMPER.name] = outsideAirFinalLoopOutput
    }

    fun doAnalogReturnDamperAction(
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        outsideAirFinalLoopOutput: Int
    ) {
        val returnDamperCmd = 100 - outsideAirFinalLoopOutput
        hssEquip.returnDamperPosition.writeHisVal(returnDamperCmd.toDouble())
        if (returnDamperCmd > 0) analogOutStages[AnalogOutput.RETURN_DAMPER.name] = returnDamperCmd
    }

    private fun handleDeadZone() {

       logIt("Zone is Dead ${nodeAddress}")
        state = ZoneState.TEMPDEAD
        resetAllLogicalPointValues()
        hssEquip.operatingMode.writeHisVal(state.ordinal.toDouble())
        if (hssEquip.equipStatus.readHisVal() != state.ordinal.toDouble())
            hssEquip.equipStatus.writeHisVal(state.ordinal.toDouble())
        val curStatus = hssEquip.equipStatusMessage.readDefaultStrVal()
        if (curStatus != "Zone Temp Dead") {
            hssEquip.equipStatusMessage.writeDefaultVal("Zone Temp Dead")
        }
        hssEquip.equipStatus.writeHisVal(ZoneState.TEMPDEAD.ordinal.toDouble())
    }
    private fun handleRFDead() {
        logIt("RF Signal is Dead ${nodeAddress}")
        state = ZoneState.RFDEAD
        hssEquip.operatingMode.writeHisVal(state.ordinal.toDouble())
        if (hssEquip.equipStatus.readHisVal() != state.ordinal.toDouble())
            hssEquip.equipStatus.writeHisVal(state.ordinal.toDouble())
        val curStatus = hssEquip.equipStatusMessage.readDefaultStrVal()
        if (curStatus != RFDead) {
            hssEquip.equipStatusMessage.writeDefaultVal(RFDead)
        }
        hssEquip.equipStatus.writeHisVal(ZoneState.RFDEAD.ordinal.toDouble())
    }

    override fun getEquip(): Equip? {
        val equip = CCUHsApi.getInstance().readHDict("equip and group == \""+nodeAddress+"\"")
        return Equip.Builder().setHDict(equip).build()
    }

    override fun resetAllLogicalPointValues() {

        super.resetAllLogicalPointValues()

        val outsideDamperMinOpen = getEffectiveOutsideDamperMinOpen()

        HyperStatSplitUserIntentHandler.updateHyperStatSplitStatus(
            equipId = equipRef,
            portStages = HashMap(),
            analogOutStages = HashMap(),
            temperatureState = ZoneTempState.TEMP_DEAD,
            economizingLoopOutput, dcvLoopOutput,
            outsideDamperMinOpen, outsideAirFinalLoopOutput,
            if (hssEquip.condensateStatusNC.readHisVal() > 0.0 || hssEquip.condensateStatusNO.readHisVal() > 0.0) 1.0 else 0.0,
            if (hssEquip.filterStatusNC.readHisVal() > 0.0 || hssEquip.filterStatusNO.readHisVal() > 0.0) 1.0 else 0.0,
            fetchBasicSettings(false, hssEquip.condensateStatusNC.readHisVal() > 0.0 || hssEquip.condensateStatusNO.readHisVal() > 0.0),
            EpidemicState.OFF
        )
    }

    @JsonIgnore
    override fun getCurrentTemp(): Double {
        return hssEquip.currentTemp.readHisVal()
    }

    @JsonIgnore
    override fun getDisplayCurrentTemp(): Double {
        return averageZoneTemp
    }

    @JsonIgnore
    override fun getAverageZoneTemp(): Double {
        return 0.0;
    }

    /**
     * Function just to print logs
     */
    private fun logIt(msg: String){
        CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON,msg)
    }

    private fun getCoolingStateActivated (): Double {
        return if (hssEquip.coolingStage3.readHisVal() == 1.0) {
            hssEquip.fanOutCoolingStage3.readDefaultVal()
        } else if (hssEquip.coolingStage2.readHisVal() == 1.0) {
            hssEquip.fanOutCoolingStage2.readDefaultVal()
        } else if (hssEquip.coolingStage1.readHisVal() == 1.0) {
            hssEquip.fanOutCoolingStage1.readDefaultVal()
        } else if (isEconomizerActive()) {
          // default to Cooling Stage 1 Fan speed if economizer is running
            hssEquip.fanOutCoolingStage1.readDefaultVal()
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
        return if (hssEquip.fanOutRecirculate.pointExists()) {
            hssEquip.fanOutRecirculate.readDefaultVal()
        } else {
            defaultFanLoopOutput
        }
    }

    private fun getAnalogEconomizerValueActivated (): Double {
        return if (hssEquip.fanOutEconomizer.pointExists()) {
            hssEquip.fanOutEconomizer.readDefaultVal()
        } else {
            defaultFanLoopOutput
        }
    }

    private fun getHeatingStateActivated (): Double {
        return if (hssEquip.heatingStage3.readHisVal() == 1.0) {
            hssEquip.fanOutHeatingStage3.readDefaultVal()
        } else if (hssEquip.heatingStage2.readHisVal() == 1.0) {
            hssEquip.fanOutHeatingStage2.readDefaultVal()
        } else if (hssEquip.heatingStage1.readHisVal() == 1.0) {
            hssEquip.fanOutHeatingStage1.readDefaultVal()
        } else {
            defaultFanLoopOutput
        }
    }

    // Get the lowest cooling stage activated
    private fun getLowestCoolingStateActivated (): Double {
        return if (hssEquip.fanOutCoolingStage1.readDefaultVal() != 0.0) {
            hssEquip.fanOutCoolingStage1.readDefaultVal()
        } else if (hssEquip.fanOutCoolingStage2.readDefaultVal() != 0.0) {
            hssEquip.fanOutCoolingStage2.readDefaultVal()
        } else if (hssEquip.fanOutCoolingStage3.readDefaultVal() != 0.0) {
            hssEquip.fanOutCoolingStage3.readDefaultVal()
        } else {
            defaultFanLoopOutput
        }
    }

    private fun getLowestHeatingStateActivated (): Double {
        return if (hssEquip.fanOutHeatingStage1.readDefaultVal() != 0.0) {
            hssEquip.fanOutHeatingStage1.readDefaultVal()
        } else if (hssEquip.fanOutHeatingStage2.readDefaultVal() != 0.0) {
            hssEquip.fanOutHeatingStage2.readDefaultVal()
        } else if (hssEquip.fanOutHeatingStage3.readDefaultVal() != 0.0) {
            hssEquip.fanOutHeatingStage3.readDefaultVal()
        } else {
            defaultFanLoopOutput
        }
    }

    private fun isEconomizerActive(): Boolean {
        return hssEquip.economizingLoopOutput.readHisVal() > 0.0
    }

    private fun getPercentageFromVoltageSelected(voltageSelected: Int): Int {
        val minVoltage = 0
        val maxVoltage = 10
        return (((voltageSelected - minVoltage).toDouble() / (maxVoltage - minVoltage)) * 100).roundToInt()
    }

    override fun isHeatingActive(): Boolean {
        return hssEquip.heatingStage1.readHisVal() == 1.0
                || hssEquip.heatingStage2.readHisVal() == 1.0
                || hssEquip.heatingStage3.readHisVal() == 1.0
                || hssEquip.heatingSignal.readHisVal() > 0.0
    }

    override fun isCoolingActive(): Boolean {
        return hssEquip.coolingStage1.readHisVal() == 1.0
                || hssEquip.coolingStage2.readHisVal() == 1.0
                || hssEquip.coolingStage3.readHisVal() == 1.0
                || hssEquip.coolingSignal.readHisVal() > 0.0
    }

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        return BaseProfileConfiguration() as T
    }

}