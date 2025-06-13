package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HSUtil
import a75f.io.domain.HyperStatSplitEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint
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
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitPackageUnitProfile
import a75f.io.logic.bo.building.statprofiles.statcontrollers.SplitControllerFactory
import a75f.io.logic.bo.building.statprofiles.util.BaseStatTuners
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.StatLoopController
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.canWeDoCooling
import a75f.io.logic.bo.building.statprofiles.util.canWeDoHeating
import a75f.io.logic.bo.building.statprofiles.util.canWeRunFan
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getAirEnthalpy
import a75f.io.logic.bo.building.statprofiles.util.getPercentFromVolt
import a75f.io.logic.bo.building.statprofiles.util.getSplitTuners
import a75f.io.logic.bo.building.statprofiles.util.isHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMediumUserIntentFanMode
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.handlers.doAnalogOperation
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.util.PreferenceUtil
import a75f.io.logic.util.uiutils.HyperStatSplitUserIntentHandler
import a75f.io.logic.util.uiutils.HyperStatSplitUserIntentHandler.Companion.hyperStatSplitStatus
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


/**
 * @author tcase@75f.io (HyperStat CPU)
 * Created on 7/7/21.
 *
 * Created for HyperStat Split CPU/Econ by Nick P on 07-24-2023.
 */
class HyperStatSplitCpuEconProfile(private val equipRef: String, nodeAddress: Short) : HyperStatSplitPackageUnitProfile(equipRef, nodeAddress) {

    private var wasCondensateTripped = false

    private var coolingLoopOutput = 0
    private var heatingLoopOutput = 0
    private var fanLoopOutput = 0
    private var compressorLoopOutput = 0
    private val defaultFanLoopOutput = 0.0
    private var economizingLoopOutput = 0
    private var dcvLoopOutput = 0
    private var outsideAirCalculatedMinDamper = 0
    private var outsideAirLoopOutput = 0
    private var outsideAirFinalLoopOutput = 0
    override lateinit var occupancyStatus: Occupancy
    private var zoneOccupancyState = CalibratedPoint(DomainName.zoneOccupancy, hssEquip.equipRef, 0.0)
    // Flags for keeping tab of occupancy during linear fan operation(Only to be used in doAnalogFanActionCpu())
    private var previousOccupancyStatus: Occupancy = Occupancy.NONE
    private var previousFanStageStatus: StandaloneFanStage = StandaloneFanStage.OFF
    private var previousFanLoopVal = 0
    private var previousFanLoopValStaged = 0
    private var fanLoopCounter = 0

    private val loopController = StatLoopController()

    private var economizingAvailable = false
    private var dcvAvailable = false
    private var matThrottle = false

    private lateinit var curState: ZoneState
    private var epidemicState = EpidemicState.OFF
    private var prePurgeEnabled = false
    private var prePurgeOpeningValue = 0.0

    private var controllerFactory = SplitControllerFactory(hssEquip, zoneOccupancyState)

    override fun updateZonePoints() {
        if (Globals.getInstance().isTestMode) {
            logIt("Test mode is on: $nodeAddress")
            return
        }

        if (mInterface != null) mInterface.refreshView()

        if (isRFDead) {
            handleRFDead()
            return
        } else if (isZoneDead) {
            handleDeadZone()
            return
        }
        updateDomainEquip(equipRef)
        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode
        val config = domainProfileConfiguration
        resetEquip()
        val hyperStatSplitTuners = getSplitTuners(hssEquip)
        val userIntents = fetchUserIntents(hssEquip)
        val averageDesiredTemp = getAverageTemp(userIntents)

        val outsideDamperMinOpen = getEffectiveOutsideDamperMinOpen()

        val fanModeSaved =
            FanModeCacheStorage.getHyperStatSplitFanModeCache().getFanModeFromCache(equipRef)

        val isCondensateTripped: Boolean =
            hssEquip.condensateStatusNC.readHisVal() > 0.0 || hssEquip.condensateStatusNO.readHisVal() > 0.0
        if (isCondensateTripped) CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "Condensate overflow detected")

        // At this point, Conditioning Mode will be set to OFF if Condensate Overflow is detected
        // It will revert to previous value when Condensate returns to normal
        val basicSettings = fetchBasicSettings(wasCondensateTripped, isCondensateTripped)

        val updatedFanMode = fallBackFanMode(hssEquip, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode

        loopController.initialise(tuners = hyperStatSplitTuners)
        loopController.dumpLogs()
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
        evaluateLoopOutputs(
            userIntents,
            basicSettings,
            hyperStatSplitTuners,
            isCondensateTripped,
            outsideDamperMinOpen,
            config
        )

        if (hssEquip.zoneOccupancy.readHisVal() > 0.0 && hssEquip.occupancyDetection.pointExists()) {
            // That pointExists() call above was NOT redundant.
            // Accessing the .id property does not actually query for the ID. One of the methods on the Point() object needs to be called first to do this.
            haystack.writeHisValueByIdWithoutCOV(hssEquip.occupancyDetection.id, 1.0)
        }

        hssEquip.coolingLoopOutput.writeHisVal(coolingLoopOutput.toDouble())
        hssEquip.heatingLoopOutput.writeHisVal(heatingLoopOutput.toDouble())
        hssEquip.fanLoopOutput.writeHisVal(fanLoopOutput.toDouble())
        hssEquip.compressorLoopOutput.writeHisVal(compressorLoopOutput.toDouble())

        hssEquip.economizingLoopOutput.writeHisVal(economizingLoopOutput.toDouble())
        hssEquip.dcvLoopOutput.writeHisVal(dcvLoopOutput.toDouble())
        hssEquip.outsideAirLoopOutput.writeHisVal(outsideAirLoopOutput.toDouble())
        hssEquip.outsideAirFinalLoopOutput.writeHisVal(outsideAirFinalLoopOutput.toDouble())

        val currentOperatingMode = hssEquip.occupancyMode.readHisVal().toInt()

        updateTitle24LoopCounter(hyperStatSplitTuners, basicSettings)
        logIt("present hssEquip : $hssEquip domain equip ${Domain.equips[equipRef]}")

        if (basicSettings.fanMode != StandaloneFanStage.OFF) {
            runRelayOperations(config, basicSettings)
            runAnalogOutOperations(config, basicSettings, hssEquip.analogOutStages)
        } else {
            resetAllLogicalPointValues()
        }
        setOperatingMode(currentTemp, averageDesiredTemp, basicSettings)

        if (hssEquip.equipStatus.readHisVal().toInt() != curState.ordinal)
            hssEquip.equipStatus.writeHisVal(curState.ordinal.toDouble())

        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState =
            ZoneTempState.EMERGENCY
        logIt(
            "Analog Fan speed multiplier  ${hyperStatSplitTuners.analogFanSpeedMultiplier} \n" +
                    "Current Working mode : ${Occupancy.values()[currentOperatingMode]} \n" +
                    "Current Temp : $currentTemp \n" +
                    "Desired Heating: ${userIntents.heatingDesiredTemp} \n" +
                    "Desired Cooling: ${userIntents.coolingDesiredTemp} \n" +
                    "Heating Loop Output: $heatingLoopOutput \n" +
                    "Cooling Loop Output:: $coolingLoopOutput \n" +
                    "Fan Loop Output:: $fanLoopOutput \n" +
                    "Economizing Loop Output:: $economizingLoopOutput \n" +
                    "DCV Loop Output:: $dcvLoopOutput \n" +
                    "Calculated Min OAO Damper:: $outsideAirCalculatedMinDamper \n" +
                    "OAO Loop Output (before MAT Safety):: $outsideAirLoopOutput \n" +
                    "OAO Loop Output (after MAT Safety and outsideDamperMinOpen):: $outsideAirFinalLoopOutput \n" +
                    "isCondensateTripped:: $isCondensateTripped \n"
        )
        logResults(config)
        logIt("Equip Running : $curState")

        HyperStatSplitUserIntentHandler.updateHyperStatSplitStatus(
            hssEquip.getId(), hssEquip.relayStages, hssEquip.analogOutStages, temperatureState,
            economizingLoopOutput, dcvLoopOutput, outsideDamperMinOpen, outsideAirFinalLoopOutput,
            if (hssEquip.condensateStatusNC.readHisVal() > 0.0 || hssEquip.condensateStatusNO.readHisVal() > 0.0) 1.0 else 0.0,
            if (hssEquip.filterStatusNC.readHisVal() > 0.0 || hssEquip.filterStatusNO.readHisVal() > 0.0) 1.0 else 0.0,
            basicSettings, epidemicState
        )

        wasCondensateTripped = isCondensateTripped

        CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "processHyperStatSplitCpuEconProfile() complete")
        updateTitle24Flags(basicSettings)

    }

    override fun getDomainProfileConfiguration(): HyperStatSplitCpuConfiguration {
        return HyperStatSplitCpuConfiguration(
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
        val outsideDamperMinOpenFromConditioning: Double =
            if (isHeatingActive() || isCoolingActive() || hssEquip.economizingLoopOutput.readHisVal() > 0.0) {
                hssEquip.outsideDamperMinOpenDuringConditioning.readDefaultVal()
            } else {
                hssEquip.outsideDamperMinOpenDuringRecirculation.readDefaultVal()
            }

        var outsideDamperMinOpenFromFanStage = 0.0
        if (hssEquip.fanHighSpeed.readHisVal() > 0.0 || isHighUserIntentFanMode(hssEquip.fanOpMode)) {
            outsideDamperMinOpenFromFanStage =
                hssEquip.outsideDamperMinOpenDuringFanHigh.readDefaultVal()
        } else if (hssEquip.fanMediumSpeed.readHisVal() > 0.0 || isMediumUserIntentFanMode(hssEquip.fanOpMode)) {
            outsideDamperMinOpenFromFanStage =
                hssEquip.outsideDamperMinOpenDuringFanMedium.readDefaultVal()
        } else if (hssEquip.fanLowSpeed.readHisVal() > 0.0 || isLowUserIntentFanMode(hssEquip.fanOpMode)) {
            outsideDamperMinOpenFromFanStage =
                hssEquip.outsideDamperMinOpenDuringFanLow.readDefaultVal()
        }

        val zoneId = HSUtil.getZoneIdFromEquipId(equip?.id)
        val occ = ScheduleManager.getInstance().getOccupiedModeCache(zoneId)
        val isOccupied = occ?.isOccupied ?: false
        return if (isOccupied && (hssEquip.conditioningMode.readPriorityVal()
                .toInt() != StandaloneConditioningMode.OFF.ordinal)
        ) {
            max(
                outsideDamperMinOpenFromConditioning.toInt(),
                outsideDamperMinOpenFromFanStage.toInt()
            )
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
        if (fanLoopCounter == 0) previousFanLoopVal = fanLoopOutput
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
    private fun updateTitle24LoopCounter(tuners: BaseStatTuners, basicSettings: BasicSettings) {
        // Check if there is change in occupancy and the fan-loop output is less than the previous value,
        // then offer the fan protection
        if ((occupancyStatus != previousOccupancyStatus && fanLoopOutput < previousFanLoopVal) ||
            (basicSettings.fanMode != previousFanStageStatus && fanLoopOutput < previousFanLoopVal)
        ) {
            fanLoopCounter = tuners.minFanRuntimePostConditioning
        } else if (occupancyStatus != previousOccupancyStatus && fanLoopOutput > previousFanLoopVal)
            fanLoopCounter =
                0 // Reset the counter if the fan-loop output is greater than the previous value
    }

    private fun handleChangeOfDirection(userIntents: UserIntents) {
        if (currentTemp > userIntents.coolingDesiredTemp && state != ZoneState.COOLING) {
            loopController.resetCoolingControl()
            state = ZoneState.COOLING
            logIt("Resetting cooling")
        } else if (currentTemp < userIntents.heatingDesiredTemp && state != ZoneState.HEATING) {
            loopController.resetHeatingControl()
            state = ZoneState.HEATING
            logIt("Resetting heating")
        }
    }

    private fun evaluateLoopOutputs(
        userIntents: UserIntents, basicSettings: BasicSettings,
        tuners: BaseStatTuners, isCondensateTripped: Boolean,
        effectiveOutsideDamperMinOpen: Int,
        config: HyperStatSplitCpuConfiguration
    ) {

        when (state) {
            //Update coolingLoop when the zone is in cooling or it was in cooling and no change over happened yet.
            ZoneState.COOLING -> {
                coolingLoopOutput = loopController.calculateCoolingLoopOutput(
                    currentTemp,
                    userIntents.coolingDesiredTemp
                ).toInt().coerceAtLeast(0)
            }
            //Update heatingLoop when the zone is in heating or it was in heating and no change over happened yet.
            ZoneState.HEATING -> {
                heatingLoopOutput =
                    loopController.calculateHeatingLoopOutput(
                        userIntents.heatingDesiredTemp, currentTemp
                    ).toInt().coerceAtLeast(0)
            }

            else -> logIt(" Zone is in deadband")
        }

        if (coolingLoopOutput > 0 && canWeDoCooling(basicSettings.conditioningMode)) {
            fanLoopOutput =
                ((coolingLoopOutput * tuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
            compressorLoopOutput = coolingLoopOutput
            curState = ZoneState.COOLING
        } else if (heatingLoopOutput > 0 && canWeDoHeating(basicSettings.conditioningMode)) {
            fanLoopOutput =
                ((heatingLoopOutput * tuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
            compressorLoopOutput = heatingLoopOutput
            curState = ZoneState.HEATING
        } else {
            compressorLoopOutput = 0
        }
        evaluateOAOLoop(basicSettings, isCondensateTripped, effectiveOutsideDamperMinOpen, config)
    }

    private fun evaluateOAOLoop(
        basicSettings: BasicSettings,
        isCondensateTripped: Boolean,
        effectiveOutsideDamperMinOpen: Int,
        config: HyperStatSplitCpuConfiguration
    ) {

        // If there's not an OAO damper mapped,
        if (!HyperStatSplitAssociationUtil.isAnyAnalogAssociatedToOAO(domainProfileConfiguration)) {

            economizingAvailable = false
            economizingLoopOutput = 0
            dcvAvailable = false
            dcvLoopOutput = 0
            if (hssEquip.dcvDamper.pointExists()) doDcvForDcvDamper()
            outsideAirLoopOutput = 0
            matThrottle = false
            outsideAirFinalLoopOutput = 0

            // Even if there's not an OAO damper, still calculate enthalpy for portal widgets
            val externalTemp = CCUHsApi.getInstance()
                .readHisValByQuery("system and outside and temp and not lockout")
            val externalHumidity =
                CCUHsApi.getInstance().readHisValByQuery("system and outside and humidity")

            val indoorTemp = hssEquip.currentTemp.readHisVal()
            val indoorHumidity = hssEquip.zoneHumidity.readHisVal()

            val insideEnthalpy = getAirEnthalpy(indoorTemp, indoorHumidity)
            val outsideEnthalpy = getAirEnthalpy(externalTemp, externalHumidity)

            hssEquip.insideEnthalpy.writeHisVal(insideEnthalpy)
            hssEquip.outsideEnthalpy.writeHisVal(outsideEnthalpy)

        } else {

            val oaoDamperMatTarget =
                hssEquip.standaloneOutsideDamperMixedAirTarget.readPriorityVal()
            val oaoDamperMatMin = hssEquip.standaloneOutsideDamperMixedAirMinimum.readPriorityVal()

            val matTemp = hssEquip.mixedAirTemperature.readHisVal()

            handleSmartPrePurgeControl()
            doEconomizing(config)
            doDcv(effectiveOutsideDamperMinOpen)

            outsideAirLoopOutput = if (epidemicState == EpidemicState.PREPURGE) {
                max(economizingLoopOutput, outsideAirCalculatedMinDamper)
            } else {
                max(economizingLoopOutput, dcvLoopOutput)
            }

            CcuLog.d(
                L.TAG_CCU_HSSPLIT_CPUECON,
                "outsideAirLoopOutput " + outsideAirLoopOutput + " oaoDamperMatTarget " + oaoDamperMatTarget + " oaoDamperMatMin " + oaoDamperMatMin
                        + " matTemp " + matTemp
            )

            matThrottle = false

            // If Conditioning Mode is AUTO or COOL_ONLY, run full economizer loop algo
            if (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO || basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY) {
                if (outsideAirLoopOutput > effectiveOutsideDamperMinOpen) {
                    outsideAirFinalLoopOutput =
                        if (matTemp < oaoDamperMatTarget && matTemp > oaoDamperMatMin) {
                            (outsideAirLoopOutput - outsideAirLoopOutput * ((oaoDamperMatTarget - matTemp) / (oaoDamperMatTarget - oaoDamperMatMin))).toInt()
                        } else {
                            if (matTemp <= oaoDamperMatMin) 0 else outsideAirLoopOutput
                        }
                    if (matTemp < oaoDamperMatTarget) matThrottle = true
                } else {
                    outsideAirFinalLoopOutput = effectiveOutsideDamperMinOpen
                }

                outsideAirFinalLoopOutput = if (matThrottle) {
                    max(outsideAirFinalLoopOutput, 0)
                } else {
                    max(outsideAirFinalLoopOutput, effectiveOutsideDamperMinOpen)
                }

                outsideAirFinalLoopOutput = min(outsideAirFinalLoopOutput, 100)

            }

            // If Condensate is tripped, outsideAirFinalLoopOutput is zero. Damper will be at minimum signal.
            else if (isCondensateTripped) {
                CcuLog.d(
                    L.TAG_CCU_HSSPLIT_CPUECON,
                    "Condensate Switch is tripped. outsideAirFinalLoopOutput set to zero."
                )
                outsideAirFinalLoopOutput = 0
            }

            // If Conditioning Mode is HEAT_ONLY or OFF, ignore economizingLoopOutput and do DCV only.
            // Continue to use same the mixed air low-limit and high-limit as full economizing scenario.
            else {
                val dcvOnlyOutsideAirLoopOutput = max(dcvLoopOutput, effectiveOutsideDamperMinOpen)

                if (dcvOnlyOutsideAirLoopOutput > effectiveOutsideDamperMinOpen) {
                    outsideAirFinalLoopOutput =
                        if (matTemp < oaoDamperMatTarget && matTemp > oaoDamperMatMin) {
                            (dcvOnlyOutsideAirLoopOutput - dcvOnlyOutsideAirLoopOutput * ((oaoDamperMatTarget - matTemp) / (oaoDamperMatTarget - oaoDamperMatMin))).toInt()
                        } else {
                            if (matTemp <= oaoDamperMatMin) 0 else dcvOnlyOutsideAirLoopOutput
                        }
                    if (matTemp < oaoDamperMatTarget) matThrottle = true
                } else {
                    outsideAirFinalLoopOutput = effectiveOutsideDamperMinOpen
                }

                outsideAirFinalLoopOutput = if (matThrottle) {
                    max(outsideAirFinalLoopOutput, 0)
                } else {
                    max(outsideAirFinalLoopOutput, effectiveOutsideDamperMinOpen)
                }
                outsideAirFinalLoopOutput = min(outsideAirFinalLoopOutput, 100)
                CcuLog.d(
                    L.TAG_CCU_HSSPLIT_CPUECON,
                    "Conditioning Mode is HEAT-ONLY or OFF. outsideAirFinalLoopOutput to consider DCV only."
                )
            }

            CcuLog.d(
                L.TAG_CCU_HSSPLIT_CPUECON,
                " economizingLoopOutput " + economizingLoopOutput + " dcvLoopOutput " + dcvLoopOutput
                        + " outsideAirFinalLoopOutput " + outsideAirFinalLoopOutput + " effectiveOutsideDamperMinOpen " + effectiveOutsideDamperMinOpen
            )

            hssEquip.outsideAirFinalLoopOutput.writeHisVal(outsideAirFinalLoopOutput.toDouble())
            hssEquip.oaoDamper.writeHisVal(outsideAirFinalLoopOutput.toDouble())

            val matThrottleNumber = if (matThrottle) 1.0 else 0.0
            hssEquip.matThrottle.writeHisVal(matThrottleNumber)
        }
    }

    private fun doDcvForDcvDamper() {

        dcvAvailable = false
        val zoneSensorCO2 = hssEquip.zoneCO2.readHisVal()
        val zoneCO2Threshold = hssEquip.co2Threshold.readDefaultVal()
        val co2DamperOpeningRate = hssEquip.co2DamperOpeningRate.readDefaultVal()
        CcuLog.d(
            L.TAG_CCU_HSSPLIT_CPUECON,
            "zoneSensorCO2: $zoneSensorCO2, zoneCO2Threshold: $zoneCO2Threshold, co2DamperOpeningRate: $co2DamperOpeningRate"
        )
        if (occupancyStatus == Occupancy.OCCUPIED || occupancyStatus == Occupancy.FORCEDOCCUPIED || occupancyStatus == Occupancy.AUTOFORCEOCCUPIED) {
            if (zoneSensorCO2 > zoneCO2Threshold) {
                dcvAvailable = true
                dcvLoopOutput = max(
                    0,
                    min(((zoneSensorCO2 - zoneCO2Threshold) / co2DamperOpeningRate).toInt(), 100)
                )
            }
        }

        val dcvAvailableNum = if (dcvAvailable) 1.0 else 0.0
        hssEquip.dcvAvailable.writeHisVal(dcvAvailableNum)
        hssEquip.dcvLoopOutput.writeHisVal(dcvLoopOutput.toDouble())

    }

    private fun doEconomizing(config: HyperStatSplitCpuConfiguration) {

        val externalTemp =
            CCUHsApi.getInstance().readHisValByQuery("system and outside and temp and not lockout")
        val externalHumidity =
            CCUHsApi.getInstance().readHisValByQuery("system and outside and humidity")

        // Check for economizer enable
        if (canDoEconomizing(externalTemp, externalHumidity)) {

            economizingAvailable = true

            val economizingToMainCoolingLoopMap =
                hssEquip.standaloneEconomizingToMainCoolingLoopMap.readPriorityVal()
            val numberConfiguredCoolingStages = config.getHighestCoolingStageCount()
            if (numberConfiguredCoolingStages > 0) {
                economizingLoopOutput =
                    min((coolingLoopOutput * (numberConfiguredCoolingStages + 1)), 100)
                CcuLog.d(
                    L.TAG_CCU_HSSPLIT_CPUECON,
                    (numberConfiguredCoolingStages + 1).toString() + " cooling stages available (including economizer); economizingLoopOutput = " + economizingLoopOutput
                )
            } else {
                CcuLog.d(
                    L.TAG_CCU_HSSPLIT_CPUECON,
                    "coolingLoopOutput = $coolingLoopOutput, economizingToMainCoolingLoopMap = $economizingToMainCoolingLoopMap, economizingLoopOutput = $economizingLoopOutput"
                )
                economizingLoopOutput =
                    (coolingLoopOutput * (100 / economizingToMainCoolingLoopMap)).toInt()
                        .coerceAtMost(100)
            }

        } else {
            economizingAvailable = false
            economizingLoopOutput = 0
        }

        val economizingAvailableNumber = if (economizingAvailable) 1.0 else 0.0
        hssEquip.economizingAvailable.writeHisVal(economizingAvailableNumber)

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
            CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "System outside temp and humidity are both zero; using local OAT sensor")
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
    private fun isEconomizingTempAndHumidityInRange(
        externalTemp: Double,
        externalHumidity: Double,
        economizingMinTemp: Double
    ): Boolean {
        CcuLog.d(
            L.TAG_CCU_HSSPLIT_CPUECON,
            "Checking outside temp and humidity against tuner min/max thresholds"
        )
        val economizingMaxTemp = hssEquip.standaloneEconomizingMaxTemperature.readPriorityVal()
        val economizingMinHumidity = hssEquip.standaloneEconomizingMinHumidity.readPriorityVal()
        val economizingMaxHumidity = hssEquip.standaloneEconomizingMaxHumidity.readPriorityVal()
        var outsideTemp = externalTemp
        var outsideHumidity = externalHumidity

        if (
            outsideTemp == 0.0 &&
            outsideHumidity == 0.0 &&
            HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToOutsideAir(
                domainProfileConfiguration
            )
        ) {
            outsideTemp = hssEquip.outsideTemperature.readHisVal()
            outsideHumidity = hssEquip.outsideHumidity.readHisVal()
        }

        if (outsideTemp > economizingMinTemp
            && outsideTemp < economizingMaxTemp
            && outsideHumidity > economizingMinHumidity
            && outsideHumidity < economizingMaxHumidity
        ) return true

        CcuLog.d(
            L.TAG_CCU_HSSPLIT_CPUECON,
            "Outside air ($outsideTemp°F, $outsideHumidity%RH) out of temp/humidity range from tuners; economizing disabled"
        )
        return false
    }

    /*
        Same enthalpy-enable condition as OAO Profile.
        If systemOutsideEnthalpy < insideEnthalpy + enthalpyDuctCompensationOffset (0 BTU/lb, adj.), enable economizing
        (Start with systemOutsideEnthalpy. If it's not available and OAT/H sensor is on sensor bus, then calculate outsideEnthalpy
        based on sensed Outside Air Temperature & Humidity.
     */
    private fun isEconomizingEnabledOnEnthalpy(insideEnthalpy: Double, outsideEnthalpy: Double): Boolean {

        CcuLog.d(
            L.TAG_CCU_HSSPLIT_CPUECON,
            "Checking enthalpy-enable condition: insideEnthalpy $insideEnthalpy, outsideEnthalpy $outsideEnthalpy"
        )

        var outsideEnthalpyToUse = outsideEnthalpy

        // Our enthalpy calc returns a value of 0.12 for zero temp and humidity.
        // We will assume anything less than 1 translates to system weather data being dead
        if (
            outsideEnthalpy < 1 &&
            HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToOutsideAir(
                domainProfileConfiguration
            )
        ) {
            CcuLog.d(
                L.TAG_CCU_HSSPLIT_CPUECON,
                "System outside temp and humidity are both zero; using local outside air temp/humidity sensor"
            )
            val sensorBusOutsideTemp = hssEquip.outsideTemperature.readHisVal()
            val sensorBusOutsideHumidity = hssEquip.outsideHumidity.readHisVal()
            outsideEnthalpyToUse = getAirEnthalpy(sensorBusOutsideTemp, sensorBusOutsideHumidity)
        }

        val enthalpyDuctCompensationOffset =
            hssEquip.standaloneEnthalpyDuctCompensationOffset.readPriorityVal()

        return insideEnthalpy > outsideEnthalpyToUse + enthalpyDuctCompensationOffset

    }

    /*
        DCV is enabled when:
            ○ Zone is in Occupied Mode AND
            ○ Zone CO2 (sensed on HyperLite) > Zone CO2 Threshold
     */
    private fun doDcv(standaloneOutsideAirDamperMinOpen: Int) {

        dcvAvailable = false
        val zoneSensorCO2 = hssEquip.zoneCO2.readHisVal()
        val zoneCO2Threshold = hssEquip.co2Threshold.readDefaultVal()
        val co2DamperOpeningRate = hssEquip.co2DamperOpeningRate.readDefaultVal()
        CcuLog.d(
            L.TAG_CCU_HSSPLIT_CPUECON,
            "zoneSensorCO2: $zoneSensorCO2, zoneCO2Threshold: $zoneCO2Threshold, co2DamperOpeningRate: $co2DamperOpeningRate"
        )
        if (occupancyStatus == Occupancy.OCCUPIED || occupancyStatus == Occupancy.FORCEDOCCUPIED || occupancyStatus == Occupancy.AUTOFORCEOCCUPIED) {
            if (zoneSensorCO2 > zoneCO2Threshold) {
                dcvAvailable = true
                dcvLoopOutput = max(
                    0,
                    min(((zoneSensorCO2 - zoneCO2Threshold) / co2DamperOpeningRate).toInt(), 100)
                )
                outsideAirCalculatedMinDamper =
                    max(dcvLoopOutput, standaloneOutsideAirDamperMinOpen)
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

    private fun fetchBasicSettings(
        wasCondensateTripped: Boolean,
        isCondensateTripped: Boolean
    ): BasicSettings {
        /* When tripped, condensate sensor will force Conditioning Mode to OFF.
           The last Conditioning Mode set by the user is stored in the var lastUserIntentConditioningMode.
        */

        val appWasJustRestarted = true
        if (isCondensateTripped) {

            // If Condensate just tripped, store whatever the previous Conditioning Mode was as UserIntentConditioningMode.
            // Conditioning Mode will return to this value once condensate returns to normal. User will not be able to change it back until this happens.
            if (!wasCondensateTripped && !appWasJustRestarted) PreferenceUtil.setLastUserIntentConditioningMode(
                StandaloneConditioningMode.values()[hssEquip.conditioningMode.readHisVal().toInt()]
            )

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
            if (!wasCondensateTripped) PreferenceUtil.setLastUserIntentConditioningMode(
                StandaloneConditioningMode.values()[hssEquip.conditioningMode.readHisVal().toInt()]
            )
            hssEquip.conditioningMode.writeDefaultVal(PreferenceUtil.getLastUserIntentConditioningMode().ordinal.toDouble())
            return BasicSettings(
                conditioningMode = StandaloneConditioningMode.values()[hssEquip.conditioningMode.readPriorityVal()
                    .toInt()],
                fanMode = StandaloneFanStage.values()[hssEquip.fanOpMode.readPriorityVal().toInt()]
            )
        }
    }


    private fun doAnalogFanActionCpuEcon(
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
                if (epidemicState == EpidemicState.PREPURGE) {
                    if (prePurgeEnabled && prePurgeOpeningValue > fanLoopForAnalog) {
                        fanLoopForAnalog = prePurgeOpeningValue.roundToInt()
                    }
                }
            } else {
                when {
                    (isLowUserIntentFanMode(hssEquip.fanOpMode)) -> fanLoopForAnalog = fanLowPercent
                    (isMediumUserIntentFanMode(hssEquip.fanOpMode)) -> fanLoopForAnalog = fanMediumPercent
                    (isHighUserIntentFanMode(hssEquip.fanOpMode)) -> fanLoopForAnalog = fanHighPercent
                }
            }
            if (fanLoopForAnalog > 0) analogOutStages[StatusMsgKeys.FAN_SPEED.name] = fanLoopForAnalog
            hssEquip.linearFanSpeed.writeHisVal(fanLoopForAnalog.toDouble())
        }
    }

    private fun isCoolingStateActivated(): Boolean {
        hssEquip.apply {
            return (coolingStage3.readHisVal() > 0 ||
                    coolingStage2.readHisVal() > 0 ||
                    coolingStage1.readHisVal() > 0) ||
                    (coolingLoopOutput.readHisVal() > 0 && isCompressorActivated())
        }
    }

    private fun isCompressorActivated(): Boolean {
        hssEquip.apply {
            return (compressorStage3.readHisVal() > 0 ||
                    compressorStage2.readHisVal() > 0 ||
                    compressorStage1.readHisVal() > 0)
        }
    }

    private fun getCoolingActivatedAnalogVoltage(
        fanEnabledMapped: Boolean,
        fanLoopOutput: Int,
        config: HyperStatSplitCpuConfiguration
    ): Int {
        val isCompressorAvailable = config.isCompressorStagesAvailable()
        var voltage = getPercentFromVolt(getCoolingStateActivated().roundToInt())

        if (isCompressorAvailable) {
            voltage = max(voltage, getPercentFromVolt(getCompressorStateActivated().roundToInt()))
        }

        // For title 24 compliance, check if fanEnabled is mapped, then set the fanloopForAnalog to the lowest cooling state activated
        // and check if staged fan is inactive(fanLoopForAnalog == 0)
        if (fanEnabledMapped && voltage == 0 && fanLoopOutput > 0) {
            voltage = getPercentFromVolt(getLowestCoolingStateActivated().roundToInt())
            if (isCompressorAvailable && hssEquip.coolingLoopOutput.readHisVal() > 0) {
                voltage = min(voltage, getPercentFromVolt(getLowestCompressorStateActivated().roundToInt()))
            }
        }
        return voltage
    }


    private fun getHeatingActivatedAnalogVoltage(
        fanEnabledMapped: Boolean,
        fanLoopOutput: Int,
        config: HyperStatSplitCpuConfiguration
    ): Int {
        val isCompressorAvailable = config.isCompressorStagesAvailable()
        var voltage = getPercentFromVolt(getHeatingStateActivated().roundToInt())

        if (isCompressorAvailable) {
            voltage = max(voltage, getPercentFromVolt(getCompressorStateActivated().roundToInt()))
        }

        // For title 24 compliance, check if fanEnabled is mapped, then set the fanloopForAnalog to the lowest heating state activated
        // and check if staged fan is inactive(fanLoopForAnalog == 0)
        if (fanEnabledMapped && voltage == 0 && fanLoopOutput > 0) {
            voltage = getPercentFromVolt(getLowestHeatingStateActivated().roundToInt())
            if(isCompressorAvailable && hssEquip.heatingLoopOutput.readHisVal() > 0) {
                voltage = min(voltage, getPercentFromVolt(getLowestCompressorStateActivated().roundToInt()))
            }
        }
        return voltage
    }

    /**
     * Check if currently in any of the occupied mode
     * @return true if in occupied,forced occupied etc, else false.
     */
    private fun checkIfInOccupiedMode(): Boolean {
        return  occupancyStatus != Occupancy.UNOCCUPIED &&
                occupancyStatus != Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
                occupancyStatus != Occupancy.VACATION
    }

    private fun doAnalogStagedFanAction(
        analogOutVoltage: AnalogOutVoltage,
        fanMode: StandaloneFanStage,
        fanEnabledMapped: Boolean,
        conditioningMode: StandaloneConditioningMode,
        fanLoopOutput: Int,
        analogOutStages: HashMap<String, Int>,
        fanProtectionCounter: Int,
        config: HyperStatSplitCpuConfiguration
    ) {

        var fanLoopForAnalog: Int
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
                        fanLoopForAnalog =
                            getCoolingActivatedAnalogVoltage(fanEnabledMapped, fanLoopOutput, config)
                        logMsg = "Cooling"
                    } else if (hssEquip.operatingMode.readHisVal() == 2.0) {
                        fanLoopForAnalog =
                            getHeatingActivatedAnalogVoltage(fanEnabledMapped, fanLoopOutput, config)
                        logMsg = "Heating"
                    }
                } else if (conditioningMode == StandaloneConditioningMode.COOL_ONLY) {
                    fanLoopForAnalog =
                        getCoolingActivatedAnalogVoltage(fanEnabledMapped, fanLoopOutput, config)
                    logMsg = "Cooling"
                } else if (conditioningMode == StandaloneConditioningMode.HEAT_ONLY) {
                    fanLoopForAnalog =
                        getHeatingActivatedAnalogVoltage(fanEnabledMapped, fanLoopOutput, config)
                    logMsg = "Heating"
                }

                // Check if we need fan protection
                if (fanProtectionCounter > 0 && fanLoopForAnalog < previousFanLoopValStaged) {
                    fanLoopForAnalog = previousFanLoopValStaged
                    logMsg = "Fan Protection"
                } else previousFanLoopValStaged =
                    fanLoopForAnalog // else indicates we are not in protection mode, so store the fanLoopForAnalog value for protection mode

                // Check Dead-band condition
                if (fanLoopOutput == 0 && fanProtectionCounter == 0 && checkIfInOccupiedMode()) { // When in dead-band, set the fan-loopForAnalog to the recirculate analog value
                    fanLoopForAnalog =
                        getPercentFromVolt(getAnalogRecirculateValueActivated().roundToInt())
                    logMsg = "Deadband"
                }
                // The speed at which fan operates during economization is determined by configuration parameter - "Analog-Out During Economizer"
                // We also need to ensure that the currently no cooling stage is activated since we switch on Economization Value only when the
                // cooling stage is not activated
                val analogEconomizerValueActivated =
                    getPercentFromVolt(getAnalogEconomizerValueActivated().roundToInt())
                if (economizingLoopOutput != 0 && !isCoolingStateActivated() && fanProtectionCounter == 0) {
                    logMsg = "Economization"
                    fanLoopForAnalog = analogEconomizerValueActivated
                }
                if (epidemicState == EpidemicState.PREPURGE) {
                    if (prePurgeEnabled) {
                        fanLoopForAnalog = prePurgeOpeningValue.roundToInt()
                        logMsg = "Pre-Purge"
                    }
                }
            } else {
                fanLoopForAnalog = when {
                    (isLowUserIntentFanMode(hssEquip.fanOpMode)) -> analogOutVoltage.linearFanAtFanLow.currentVal.toInt()
                    (isMediumUserIntentFanMode(hssEquip.fanOpMode)) -> analogOutVoltage.linearFanAtFanMedium.currentVal.toInt()
                    (isHighUserIntentFanMode(hssEquip.fanOpMode)) -> analogOutVoltage.linearFanAtFanHigh.currentVal.toInt()
                    else -> { 0 }
                }
                // Store just in case when we switch from current occupied to unoccupied mode
                previousFanLoopValStaged = fanLoopForAnalog
            }
            if (fanLoopForAnalog > 0) analogOutStages[StatusMsgKeys.FAN_SPEED.name] =
                fanLoopForAnalog
            hssEquip.stagedFanSpeed.writeHisVal(fanLoopForAnalog.toDouble())
            CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, " Staged Fan Speed calculated ($logMsg) == $fanLoopForAnalog")
        }
    }

    /**
     * Handles the smart pre-purge control for the given HyperStatSplitCpuEconEquip.
     * If pre-purge is active, this method sets the value of outsideAirCalculatedMinDamper
     */
    private fun handleSmartPrePurgeControl() {
        if (!prePurgeEnabled) {
            epidemicState = EpidemicState.OFF
            return
        }
        val prePurgeRunTime = hssEquip.standalonePrePurgeRuntimeTuner.readPriorityVal()
        val prePurgeOccupiedTimeOffset: Double =
            hssEquip.standalonePrePurgeOccupiedTimeOffsetTuner.readPriorityVal()

        val oaoDamperMatMin = hssEquip.standaloneOutsideDamperMixedAirMinimum.readPriorityVal()
        val matTemp = hssEquip.mixedAirTemperature.readHisVal()
        val zoneId = equip?.roomRef ?: ""
        val occuStatus = ScheduleManager.getInstance().getOccupiedModeCache(zoneId)
        val minutesToOccupancy =
            if (occuStatus != null) occuStatus.millisecondsUntilNextChange.toInt() / 60000 else -1
        if (minutesToOccupancy > 0 && prePurgeOccupiedTimeOffset >= minutesToOccupancy && minutesToOccupancy >= prePurgeOccupiedTimeOffset - prePurgeRunTime &&
            matTemp > oaoDamperMatMin && hssEquip.conditioningMode.readPriorityVal()
                .toInt() != StandaloneConditioningMode.OFF.ordinal && hssEquip.fanOpMode.readPriorityVal()
                .toInt() != StandaloneFanStage.OFF.ordinal
        ) {
            outsideAirCalculatedMinDamper =
                hssEquip.prePurgeOutsideDamperOpen.readPriorityVal().toInt()
            epidemicState = EpidemicState.PREPURGE
            hssEquip.prePurgeStatus.writeHisVal(1.0)
        } else {
            epidemicState = EpidemicState.OFF
            hssEquip.prePurgeStatus.writeHisVal(0.0)
        }
    }


    private fun handleDeadZone() {

        logIt("Zone is Dead $nodeAddress")
        state = ZoneState.TEMPDEAD
        resetAllLogicalPointValues()
        hssEquip.operatingMode.writeHisVal(state.ordinal.toDouble())
        if (hssEquip.equipStatus.readHisVal() != state.ordinal.toDouble())
            hssEquip.equipStatus.writeHisVal(state.ordinal.toDouble())
        val curStatus = hssEquip.equipStatusMessage.readDefaultStrVal()
        if (curStatus != ZoneTempDead) {
            hssEquip.equipStatusMessage.writeDefaultVal(ZoneTempDead)
        }
        hyperStatSplitStatus[hssEquip.getId()] = ZoneTempDead
        hssEquip.equipStatus.writeHisVal(ZoneState.TEMPDEAD.ordinal.toDouble())
    }

    private fun handleRFDead() {
        logIt("RF Signal is Dead $nodeAddress")
        state = ZoneState.RFDEAD
        hssEquip.operatingMode.writeHisVal(state.ordinal.toDouble())
        if (hssEquip.equipStatus.readHisVal() != state.ordinal.toDouble())
            hssEquip.equipStatus.writeHisVal(state.ordinal.toDouble())
        val curStatus = hssEquip.equipStatusMessage.readDefaultStrVal()
        if (curStatus != RFDead) {
            hssEquip.equipStatusMessage.writeDefaultVal(RFDead)
        }
        hyperStatSplitStatus[hssEquip.getId()] = RFDead
        hssEquip.equipStatus.writeHisVal(ZoneState.RFDEAD.ordinal.toDouble())
    }

    override fun getEquip(): Equip? {
        val equip = CCUHsApi.getInstance().readHDict("equip and group == \"$nodeAddress\"")
        return Equip.Builder().setHDict(equip).build()
    }

    override fun resetAllLogicalPointValues() {
        super.resetAllLogicalPointValues()
        HyperStatSplitUserIntentHandler.updateHyperStatSplitStatus(
            equipId = equipRef,
            portStages = HashMap(),
            analogOutStages = HashMap(),
            temperatureState = ZoneTempState.TEMP_DEAD,
            economizingLoopOutput, dcvLoopOutput,
            getEffectiveOutsideDamperMinOpen(), outsideAirFinalLoopOutput,
            if (hssEquip.isCondensateTripped()) 1.0 else 0.0,
            if (hssEquip.filterStatusNC.readHisVal() > 0.0 || hssEquip.filterStatusNO.readHisVal() > 0.0) 1.0 else 0.0,
            fetchBasicSettings(
                false,
                hssEquip.condensateStatusNC.readHisVal() > 0.0 || hssEquip.condensateStatusNO.readHisVal() > 0.0
            ),
            EpidemicState.OFF
        )
    }

    override fun getCurrentTemp() = hssEquip.currentTemp.readHisVal()

    override fun getDisplayCurrentTemp() = averageZoneTemp

    override fun getAverageZoneTemp() = 0.0

    private fun logIt(msg: String){
        CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON,msg)
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

    private fun getAnalogEconomizerValueActivated(): Double {
        return if (hssEquip.fanOutEconomizer.pointExists()) {
            hssEquip.fanOutEconomizer.readDefaultVal()
        } else {
            defaultFanLoopOutput
        }
    }

    private fun isEconomizerActive() = hssEquip.economizingLoopOutput.readHisVal() > 0.0

    private fun getCoolingStateActivated(): Double {
        val stages = listOf(
            hssEquip.coolingStage3.readHisVal() to hssEquip.fanOutCoolingStage3.readDefaultVal(),
            hssEquip.coolingStage2.readHisVal() to hssEquip.fanOutCoolingStage2.readDefaultVal(),
            hssEquip.coolingStage1.readHisVal() to hssEquip.fanOutCoolingStage1.readDefaultVal()
        )

        return stages.firstOrNull { it.first == 1.0 }?.second
            ?: if (isEconomizerActive()) {
                hssEquip.fanOutCoolingStage1.readDefaultVal()
            } else {
                defaultFanLoopOutput
            }
    }

    private fun getHeatingStateActivated(): Double {
        val stages = listOf(
            hssEquip.heatingStage3.readHisVal() to hssEquip.fanOutHeatingStage3.readDefaultVal(),
            hssEquip.heatingStage2.readHisVal() to hssEquip.fanOutHeatingStage2.readDefaultVal(),
            hssEquip.heatingStage1.readHisVal() to hssEquip.fanOutHeatingStage1.readDefaultVal()
        )
        return stages.firstOrNull { it.first == 1.0 }?.second ?: defaultFanLoopOutput
    }

    private fun getCompressorStateActivated(): Double {
        val stages = listOf(
            hssEquip.compressorStage3.readHisVal() to hssEquip.fanOutCompressorStage3.readDefaultVal(),
            hssEquip.compressorStage2.readHisVal() to hssEquip.fanOutCompressorStage2.readDefaultVal(),
            hssEquip.compressorStage1.readHisVal() to hssEquip.fanOutCompressorStage1.readDefaultVal()
        )
        return stages.firstOrNull { it.first == 1.0 }?.second ?: defaultFanLoopOutput
    }

    private fun getLowestCoolingStateActivated(): Double {
        return listOf(
            hssEquip.fanOutCoolingStage1.readDefaultVal(),
            hssEquip.fanOutCoolingStage2.readDefaultVal(),
            hssEquip.fanOutCoolingStage3.readDefaultVal()
        ).firstOrNull { it != 0.0 } ?: defaultFanLoopOutput
    }

    private fun getLowestCompressorStateActivated(): Double {
        return listOf(
            hssEquip.fanOutCompressorStage1.readDefaultVal(),
            hssEquip.fanOutCompressorStage2.readDefaultVal(),
            hssEquip.fanOutCompressorStage3.readDefaultVal()
        ).firstOrNull { it != 0.0 } ?: defaultFanLoopOutput
    }



    private fun getLowestHeatingStateActivated(): Double {
        return listOf(
            hssEquip.fanOutHeatingStage1.readDefaultVal(),
            hssEquip.fanOutHeatingStage2.readDefaultVal(),
            hssEquip.fanOutHeatingStage3.readDefaultVal()
        ).firstOrNull { it != 0.0 } ?: defaultFanLoopOutput
    }


    override fun isHeatingActive(): Boolean {
        return (hssEquip.heatingStage1.readHisVal() == 1.0
                || hssEquip.heatingStage2.readHisVal() == 1.0
                || hssEquip.heatingStage3.readHisVal() == 1.0
                || hssEquip.heatingSignal.readHisVal() > 0.0 ) ||
                (hssEquip.heatingLoopOutput.readHisVal() > 0 && isCompressorActivated())
    }

    override fun isCoolingActive(): Boolean {
        return (hssEquip.coolingStage1.readHisVal() == 1.0
                || hssEquip.coolingStage2.readHisVal() == 1.0
                || hssEquip.coolingStage3.readHisVal() == 1.0
                || hssEquip.coolingSignal.readHisVal() > 0.0)
                || (hssEquip.coolingLoopOutput.readHisVal() > 0 && isCompressorActivated())
    }

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        return BaseProfileConfiguration() as T
    }

    override fun getProfileType() = ProfileType.HYPERSTATSPLIT_CPU

    private fun runRelayOperations(
        config: HyperStatSplitCpuConfiguration,
        basicSettings: BasicSettings
    ) {
        updatePrerequisite(config)
        runControllers(hssEquip, basicSettings, config)
    }

    private fun updatePrerequisite(config: HyperStatSplitCpuConfiguration) {
        if (controllerFactory.equip != hssEquip) {
            controllerFactory.equip = hssEquip
        }
        zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()

        if (coolingLoopOutput > 0) {
            hssEquip.isEconAvailable.data = if (economizingAvailable) 1.0 else 0.0
        } else {
            hssEquip.isEconAvailable.data = 0.0
        }

        if (config.isCoolingStagesAvailable()) {
            hssEquip.highestCoolingStages.data = config.getHighestCoolingStageCount().toDouble()
        } else {
            hssEquip.highestCoolingStages.data = 0.0
        }

        if (config.isCompressorStagesAvailable()) {
            hssEquip.highestCompressorStages.data =
                config.getHighestCompressorStageCount().toDouble()
        } else {
            hssEquip.highestCompressorStages.data = 0.0
        }

        /**
         * This is where we run the controllers for the equipment. they need dynamic loop points values
         * when constraint executes they will calculate the value based on the current loop points
         */
        hssEquip.derivedFanLoopOutput.data = hssEquip.fanLoopOutput.readHisVal()

        // This is for title 24 compliance
        if (fanLoopCounter > 0) {
            hssEquip.derivedFanLoopOutput.data = previousFanLoopVal.toDouble()
        }
        controllerFactory.addControllers(config, ::isPrePurgeActive)
        logIt(" isEconAvailable: ${hssEquip.isEconAvailable.data} " +
                "zoneOccupancyState : ${zoneOccupancyState.data}" +
                " highestCoolingStages : ${hssEquip.highestCoolingStages.data}" +
                " highestCompressorStages : ${hssEquip.highestCompressorStages.data} ")
    }

    private fun runControllers(equip: HyperStatSplitEquip, basicSettings: BasicSettings, config: HyperStatSplitCpuConfiguration) {
        equip.controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            val result = controller.runController()
            updateRelayStatus(controllerName, result, equip, basicSettings, config)
        }
    }

    private fun isPrePurgeActive() = (epidemicState == EpidemicState.PREPURGE)

    private fun runAnalogOutOperations(
        config: HyperStatSplitCpuConfiguration,
        basicSettings: BasicSettings,
        analogOutStages: HashMap<String, Int>
    ) {
        val analogOuts = config.getAnalogOutsConfigurationMapping()
        analogOuts.forEach { (enabled, association, port) ->
            if (enabled) {

                if (hssEquip.isCondensateTripped()) {
                    resetAnalogOutLogicalPoints()
                    return
                }

                val mapping = CpuControlType.values()[association]
                when (mapping) {
                    CpuControlType.COOLING -> {
                        doAnalogOperation(
                            canWeDoCooling(basicSettings.conditioningMode),
                            analogOutStages, StatusMsgKeys.COOLING.name,
                            coolingLoopOutput, hssEquip.coolingSignal
                        )
                    }

                    CpuControlType.HEATING -> {
                        doAnalogOperation(
                            canWeDoHeating(basicSettings.conditioningMode),
                            analogOutStages, StatusMsgKeys.HEATING.name,
                            heatingLoopOutput, hssEquip.heatingSignal
                        )
                    }

                    CpuControlType.LINEAR_FAN -> {
                        doAnalogFanActionCpuEcon(
                            config.getFanConfiguration(port),
                            basicSettings.fanMode,
                            basicSettings.conditioningMode,
                            fanLoopOutput,
                            analogOutStages,
                            previousFanLoopVal,
                            fanLoopCounter
                        )
                    }

                    CpuControlType.STAGED_FAN -> {
                        doAnalogStagedFanAction(
                            config.getFanConfiguration(port),
                            basicSettings.fanMode,
                            HyperStatSplitAssociationUtil.isAnyRelayAssociatedToFanEnabled(config),
                            basicSettings.conditioningMode,
                            fanLoopOutput,
                            analogOutStages,
                            fanLoopCounter,
                            config
                        )
                    }

                    CpuControlType.OAO_DAMPER -> {

                        hssEquip.oaoDamper.writeHisVal(outsideAirFinalLoopOutput.toDouble())
                        if (outsideAirFinalLoopOutput > 0) {
                            analogOutStages[StatusMsgKeys.OAO_DAMPER.name] =
                                outsideAirFinalLoopOutput
                        }
                    }

                    CpuControlType.RETURN_DAMPER -> {
                        val returnDamperCmd = 100 - outsideAirFinalLoopOutput
                        hssEquip.returnDamperPosition.writeHisVal(returnDamperCmd.toDouble())
                        if (returnDamperCmd > 0) {
                            analogOutStages[StatusMsgKeys.RETURN_DAMPER.name] = returnDamperCmd
                        }
                    }

                    CpuControlType.COMPRESSOR_SPEED -> {
                        if (basicSettings.conditioningMode != StandaloneConditioningMode.OFF) {
                            hssEquip.compressorSpeed.writePointValue(hssEquip.compressorLoopOutput.readHisVal())
                            if (compressorLoopOutput > 0) {
                                if (curState == ZoneState.COOLING) analogOutStages[StatusMsgKeys.COOLING.name] = compressorLoopOutput
                                if ( curState == ZoneState.HEATING) analogOutStages[StatusMsgKeys.HEATING.name] = compressorLoopOutput
                            }
                        } else {
                            hssEquip.compressorSpeed.writePointValue(0.0)
                        }
                    }

                    CpuControlType.DCV_MODULATING_DAMPER -> {
                        hssEquip.dcvDamperModulating.writePointValue(dcvLoopOutput.toDouble())
                        if (dcvLoopOutput > 0) {
                            analogOutStages[StatusMsgKeys.DCV_DAMPER.name] = dcvLoopOutput
                        }
                    }

                    CpuControlType.EXTERNALLY_MAPPED -> { /** DO NOTHING  */ }

                }
            }
        }
    }

    private fun runTitle24Rule(config: HyperStatSplitCpuConfiguration) {
        resetFanStatus()
        fanEnabledStatus = (HyperStatSplitAssociationUtil.isAnyRelayAssociatedToFanEnabled(config))
        when (HyperStatSplitAssociationUtil.getLowestFanStage(config)) {
            CpuRelayType.FAN_LOW_SPEED -> lowestStageFanLow = true
            CpuRelayType.FAN_MEDIUM_SPEED -> lowestStageFanMedium = true
            CpuRelayType.FAN_HIGH_SPEED -> lowestStageFanHigh = true
            else -> {}
        }
    }

    private fun updateRelayStatus(
        controllerName: String, result: Any, equip: HyperStatSplitEquip,
        basicSettings: BasicSettings, config: HyperStatSplitCpuConfiguration
    ) {

        fun updateRelayStage(stageName: String, isActive: Boolean, point: Point) {

            if (point.pointExists()) {
                val status = if (isActive) 1.0 else 0.0
                if (isActive) {
                    equip.relayStages[stageName] = status.toInt()
                } else {
                    equip.relayStages.remove(stageName)
                }
                point.writeHisVal(status)
            }
        }

        fun updateStatus(point: Point, result: Any, status: String? = null) {
            point.writeHisVal(if (result as Boolean) 1.0 else 0.0)
            if (status != null && result) {
                equip.relayStages[status] = 1
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
                    if (isActive) curState = ZoneState.COOLING
                    when (stage) {
                        0 -> updateRelayStage(Stage.COOLING_1.displayName, isActive, equip.coolingStage1)
                        1 -> updateRelayStage(Stage.COOLING_2.displayName, isActive, equip.coolingStage2)
                        2 -> updateRelayStage(Stage.COOLING_3.displayName, isActive, equip.coolingStage3)
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
                    if (isActive) curState = ZoneState.HEATING
                    when (stage) {
                        0 -> updateRelayStage(Stage.HEATING_1.displayName, isActive, equip.heatingStage1)
                        1 -> updateRelayStage(Stage.HEATING_2.displayName, isActive, equip.heatingStage2)
                        2 -> updateRelayStage(Stage.HEATING_3.displayName, isActive, equip.heatingStage3)
                    }
                }
            }

            ControllerNames.FAN_SPEED_CONTROLLER -> {
                runTitle24Rule(config)
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
                        canWeRunFan(basicSettings) &&
                                (currentState || (fanEnabledStatus && fanLoopOutput > 0 && isLowestStageActive))
                    } else {
                        checkUserIntentAction(stage)
                    }
                }

                val fanStages = result as List<Pair<Int, Boolean>>
                fanStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateRelayStage(Stage.FAN_1.displayName, isStageActive(stage, isActive, lowestStageFanLow), equip.fanLowSpeed)
                        1 -> updateRelayStage(Stage.FAN_2.displayName, isStageActive(stage, isActive, lowestStageFanMedium), equip.fanMediumSpeed)
                        2 -> updateRelayStage(Stage.FAN_3.displayName, isStageActive(stage, isActive, lowestStageFanHigh), equip.fanHighSpeed)
                    }
                }
            }

            ControllerNames.COMPRESSOR_RELAY_CONTROLLER -> {
                val compressorStages = result as List<Pair<Int, Boolean>>
                compressorStages.forEach {
                    val (stage, isActive) = Pair(
                        it.first,
                        if (basicSettings.conditioningMode != StandaloneConditioningMode.OFF ) it.second else false
                    )
                    when (stage) {
                        0 -> updateRelayStage(if (coolingLoopOutput > 0 ) Stage.COOLING_1.displayName else Stage.HEATING_1.displayName, isActive, equip.compressorStage1)
                        1 -> updateRelayStage(if (coolingLoopOutput > 0 ) Stage.COOLING_2.displayName else Stage.HEATING_2.displayName, isActive, equip.compressorStage2)
                        2 -> updateRelayStage(if (coolingLoopOutput > 0 ) Stage.COOLING_3.displayName else Stage.HEATING_3.displayName, isActive, equip.compressorStage3)
                    }
                }
            }

            ControllerNames.FAN_ENABLED -> updateStatus(equip.fanEnable,result, StatusMsgKeys.FAN_ENABLED.name)
            ControllerNames.OCCUPIED_ENABLED -> updateStatus(equip.occupiedEnable,result)
            ControllerNames.HUMIDIFIER_CONTROLLER -> updateStatus(equip.humidifierEnable,result)
            ControllerNames.DEHUMIDIFIER_CONTROLLER -> updateStatus(equip.dehumidifierEnable,result)
            ControllerNames.EXHAUST_FAN_STAGE1_CONTROLLER -> updateStatus(equip.exhaustFanStage1,result)
            ControllerNames.EXHAUST_FAN_STAGE2_CONTROLLER -> updateStatus(equip.exhaustFanStage2,result)
            ControllerNames.DAMPER_RELAY_CONTROLLER -> updateStatus(equip.dcvDamper,result)
            ControllerNames.AUX_HEATING_STAGE1 -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode != StandaloneConditioningMode.AUTO
                    && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
                    status = false
                }
                updateStatus(equip.auxHeatingStage1, status, StatusMsgKeys.AUX_HEATING_STAGE1.name)
            }

            ControllerNames.AUX_HEATING_STAGE2 -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode != StandaloneConditioningMode.AUTO
                    && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
                    status = false
                }
                updateStatus(equip.auxHeatingStage2, status, StatusMsgKeys.AUX_HEATING_STAGE2.name)
            }
            ControllerNames.CHANGE_OVER_O_COOLING -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
                    status = false
                }
                updateStatus(equip.changeOverCooling,status)
            }
            ControllerNames.CHANGE_OVER_B_HEATING -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
                    status = false
                }
                updateStatus(equip.changeOverHeating,status)
            }
            else -> {
                CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "Unknown controller: $controllerName")
            }
        }
    }

    private fun resetEquip() {
        hssEquip.relayStages.clear()
        hssEquip.analogOutStages.clear()
        hssEquip.derivedFanLoopOutput.data = 0.0
    }

    private fun logResults(config: HyperStatSplitCpuConfiguration) {

        val mappings = config.getRelayConfigurationMapping()
        mappings.forEach { (enabled, association, port) ->
            if (enabled) {
                val mapping = CpuRelayType.values()[association]
                val logicalPoint = when (mapping) {
                    CpuRelayType.COOLING_STAGE1 -> hssEquip.coolingStage1
                    CpuRelayType.COOLING_STAGE2 -> hssEquip.coolingStage2
                    CpuRelayType.COOLING_STAGE3 -> hssEquip.coolingStage3
                    CpuRelayType.HEATING_STAGE1 -> hssEquip.heatingStage1
                    CpuRelayType.HEATING_STAGE2 -> hssEquip.heatingStage2
                    CpuRelayType.HEATING_STAGE3 -> hssEquip.heatingStage3
                    CpuRelayType.FAN_LOW_SPEED -> hssEquip.fanLowSpeed
                    CpuRelayType.FAN_MEDIUM_SPEED -> hssEquip.fanMediumSpeed
                    CpuRelayType.FAN_HIGH_SPEED -> hssEquip.fanHighSpeed
                    CpuRelayType.FAN_ENABLED -> hssEquip.fanEnable
                    CpuRelayType.OCCUPIED_ENABLED -> hssEquip.occupiedEnable
                    CpuRelayType.HUMIDIFIER -> hssEquip.humidifierEnable
                    CpuRelayType.DEHUMIDIFIER -> hssEquip.dehumidifierEnable
                    CpuRelayType.EX_FAN_STAGE1 -> hssEquip.exhaustFanStage1
                    CpuRelayType.EX_FAN_STAGE2 -> hssEquip.exhaustFanStage2
                    CpuRelayType.DCV_DAMPER -> hssEquip.dcvDamper
                    CpuRelayType.COMPRESSOR_STAGE1 -> hssEquip.compressorStage1
                    CpuRelayType.COMPRESSOR_STAGE2 -> hssEquip.compressorStage2
                    CpuRelayType.COMPRESSOR_STAGE3 -> hssEquip.compressorStage3
                    CpuRelayType.CHANGE_OVER_O_COOLING -> hssEquip.changeOverCooling
                    CpuRelayType.CHANGE_OVER_B_HEATING -> hssEquip.changeOverHeating
                    CpuRelayType.AUX_HEATING_STAGE1 -> hssEquip.auxHeatingStage1
                    CpuRelayType.AUX_HEATING_STAGE2 -> hssEquip.auxHeatingStage2
                    CpuRelayType.EXTERNALLY_MAPPED -> { null }
                }
                if (logicalPoint != null) {
                    logIt("$port = $mapping ${logicalPoint.readHisVal()}")
                }
            }
        }

        config.getAnalogOutsConfigurationMapping().forEach {
            val (enabled, association, port) = it
            if (enabled) {
                val mapping = CpuControlType.values()[association]
                val modulation = when (mapping) {
                    CpuControlType.COOLING -> hssEquip.coolingSignal
                    CpuControlType.LINEAR_FAN -> hssEquip.linearFanSpeed
                    CpuControlType.HEATING -> hssEquip.heatingSignal
                    CpuControlType.OAO_DAMPER -> hssEquip.oaoDamper
                    CpuControlType.STAGED_FAN -> hssEquip.stagedFanSpeed
                    CpuControlType.RETURN_DAMPER -> hssEquip.returnDamperPosition
                    CpuControlType.COMPRESSOR_SPEED -> hssEquip.compressorSpeed
                    CpuControlType.DCV_MODULATING_DAMPER -> hssEquip.dcvDamperModulating
                    else-> { null }
                }
                if (modulation != null) {
                    CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "$port = ${mapping.name}  analogSignal  ${modulation.readHisVal()}")
                }
            }
        }

    }

}



