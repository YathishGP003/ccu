package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HSUtil
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.HyperStatSplitEquip
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.bo.building.EpidemicState
import a75f.io.logic.bo.building.ZoneProfile
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil
import a75f.io.logic.bo.building.statprofiles.statcontrollers.SplitControllerFactory
import a75f.io.logic.bo.building.statprofiles.util.BaseStatTuners
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.StagesCounts
import a75f.io.logic.bo.building.statprofiles.util.StatLoopController
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.canWeDoCooling
import a75f.io.logic.bo.building.statprofiles.util.canWeDoHeating
import a75f.io.logic.bo.building.statprofiles.util.getAirEnthalpy
import a75f.io.logic.bo.building.statprofiles.util.isHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMediumUserIntentFanMode
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.controlcomponents.util.isSoftOccupied
import a75f.io.logic.util.isOfflineMode
import a75f.io.logic.util.uiutils.HyperStatSplitUserIntentHandler.Companion.hyperStatSplitStatus
import a75f.io.logic.util.uiutils.updateUserIntentPoints
import android.content.Context
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


/**
 * Created by Manjunath K for HyperStat on 11-07-2022.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */

abstract class HyperStatSplitProfile(equipRef: String, var nodeAddress: Short, var tag: String) : ZoneProfile() {

    var coolingLoopOutput = 0
    var heatingLoopOutput = 0
    var fanLoopOutput = 0
    var compressorLoopOutput = 0
    val defaultFanLoopOutput = 0.0
    var economizingLoopOutput = 0
    var dcvLoopOutput = 0
    var outsideAirCalculatedMinDamper = 0
    var outsideAirLoopOutput = 0
    var outsideAirFinalLoopOutput = 0
    var prePurgeOpeningValue = 0.0
    var previousFanLoopVal = 0
    var previousFanLoopValStaged = 0
    var fanLoopCounter = 0

    var economizingAvailable = false
    private var dcvAvailable = false
    private var matThrottle = false
    var wasCondensateTripped = false
    var prePurgeEnabled = false
    val haystack: CCUHsApi = CCUHsApi.getInstance()
    var stageCounts = StagesCounts()
    val derivedFanLoopOutput = CalibratedPoint(DomainName.fanLoopOutput, equipRef, 0.0)
    var zoneOccupancyState = CalibratedPoint("zoneOccupancyState", equipRef, 0.0)
    val isEconAvailable: CalibratedPoint = CalibratedPoint("economizingAvailable", "", 0.0)
    val fanLowVentilationAvailable: CalibratedPoint = CalibratedPoint("fanLowVentilation", "", 0.0)

    val loopController = StatLoopController()
    private var previousOccupancyStatus: Occupancy = Occupancy.NONE
    private var previousFanStageStatus: StandaloneFanStage = StandaloneFanStage.OFF
    var epidemicState = EpidemicState.OFF

    private var hasZeroFanLoopBeenHandled = false
    var fanEnabledStatus = false
    var lowestStageFanLow = false
    var lowestStageFanMedium = false
    var lowestStageFanHigh = false

    var isDoorOpen = false
    var isDoorOpenFromTitle24 = false

    lateinit var  controllerFactory: SplitControllerFactory
    lateinit var curState: ZoneState
    var occupancyStatus: Occupancy = Occupancy.NONE

    override fun getEquip(): Equip? {
        val equip = CCUHsApi.getInstance().readHDict("equip and group == \"$nodeAddress\"")
        return Equip.Builder().setHDict(equip).build()
    }

    fun resetFanStatus() {
        fanEnabledStatus = false
        lowestStageFanLow = false
        lowestStageFanMedium = false
        lowestStageFanHigh = false
    }
    fun resetEquip(equip: HyperStatSplitEquip) {
        equip.relayStages.clear()
        equip.analogOutStages.clear()
        derivedFanLoopOutput.data = 0.0
    }

    fun handleDeadZone(hssEquip: HyperStatSplitEquip) {
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

    fun handleRFDead(hssEquip: HyperStatSplitEquip) {
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

    fun resetPoint(point: Point) {
        if (point.pointExists()) {
            point.writeHisVal(0.0)
        }
    }
    abstract fun resetAllLogicalPointValues()
    abstract fun resetRelayLogicalPoints()
    abstract fun resetAnalogOutLogicalPoints()
    fun runLowestFanSpeedDuringDoorOpen(hssEquip: HyperStatSplitEquip) {
        mutableMapOf(
            hssEquip.fanLowSpeedVentilation to Stage.FAN_1.displayName,
            hssEquip.fanLowSpeed to Stage.FAN_1.displayName,
            hssEquip.fanMediumSpeed to Stage.FAN_2.displayName,
            hssEquip.fanHighSpeed to Stage.FAN_3.displayName,
            hssEquip.fanEnable to StatusMsgKeys.FAN_ENABLED.name
        ).forEach {
            if (it.key.pointExists()) {
                it.key.writeHisVal(1.0)
                hssEquip.relayStages[it.value] = 1
                logIt( "Lowest Fan Speed is running due to Title 24 Door Open open state: ${it.key.domainName}")
                return
            }
        }
    }

    fun resetLoops(hssEquip : HyperStatSplitEquip) {
        hssEquip.apply {
            listOf(
                coolingLoopOutput, heatingLoopOutput, compressorLoopOutput,
                fanLoopOutput, economizingLoopOutput, dcvLoopOutput,
                outsideAirLoopOutput, outsideAirFinalLoopOutput
            ).forEach { resetPoint(it) }
        }
        derivedFanLoopOutput.data = 0.0
    }

    fun resetLoopOutputs() {
        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        compressorLoopOutput = 0
        economizingLoopOutput = 0
        dcvLoopOutput = 0
        outsideAirCalculatedMinDamper = 0
        outsideAirLoopOutput = 0
        outsideAirFinalLoopOutput = 0
    }

    fun handleChangeOfDirection(userIntents: UserIntents, factory: SplitControllerFactory, equip: HyperStatSplitEquip) {
        if (currentTemp > userIntents.coolingDesiredTemp && state != ZoneState.COOLING) {
            loopController.resetCoolingControl()
            state = ZoneState.COOLING
            resetControllers(factory, equip)
            logIt("Resetting cooling")
        } else if (currentTemp < userIntents.heatingDesiredTemp && state != ZoneState.HEATING) {
            loopController.resetHeatingControl()
            state = ZoneState.HEATING
            resetControllers(factory, equip)
            logIt("Resetting heating")
        }
    }

    private fun resetControllers(factory: SplitControllerFactory, equip: HyperStatSplitEquip) {
        // Add controller here if any new stage controller is added
        listOf(
            ControllerNames.COOLING_STAGE_CONTROLLER,
            ControllerNames.HEATING_STAGE_CONTROLLER,
            ControllerNames.FAN_SPEED_CONTROLLER,
            ControllerNames.COMPRESSOR_RELAY_CONTROLLER,
        ).forEach {
            val controller = factory.getController(it)
            controller?.resetController()
        }

        previousFanLoopVal = 0
        previousFanLoopValStaged = 0
        fanLoopCounter = 0
        hasZeroFanLoopBeenHandled = false
        resetAllLogicalPointValues()
        equip.analogOutStages.clear()
        equip.relayStages.clear()
    }

    fun fallBackFanMode(
        equip: HyperStatSplitEquip, fanModeSaved: Int, basicSettings: BasicSettings
    ): StandaloneFanStage {

        logIt("FanModeSaved in Shared Preference $fanModeSaved")
        val currentOperatingMode = equip.occupancyMode.readHisVal().toInt()

        // If occupancy is unoccupied or demand response unoccupied and the fan mode is current occupied then remove the fan mode from cache
        if ((occupancyStatus == Occupancy.UNOCCUPIED || occupancyStatus == Occupancy.DEMAND_RESPONSE_UNOCCUPIED ) && isFanModeCurrentOccupied(fanModeSaved)) {
            logIt("Clearing FanModeSaved in Shared Preference $fanModeSaved")
            FanModeCacheStorage.getHyperStatSplitFanModeCache().removeFanModeFromCache(equip.equipRef)
            logIt("Clearing FanModeSaved in Shared Preference ${FanModeCacheStorage.getHyperStatSplitFanModeCache().getFanModeFromCache(equip.equipRef)}")
        }
        logIt("Fall back fan mode " + basicSettings.fanMode + " conditioning mode " + basicSettings.conditioningMode)
        logIt("Fan Details :$occupancyStatus  ${basicSettings.fanMode}  $fanModeSaved")
        if (isEligibleToAuto(basicSettings, equip)) {
            logIt("Resetting the Fan status back to  AUTO: ")
            updateUserIntentPoints(
                equipRef = equip.equipRef,
                point = equip.fanOpMode,
                value = StandaloneFanStage.AUTO.ordinal.toDouble(),
                CCUHsApi.getInstance().ccuUserName
            )
            return StandaloneFanStage.AUTO
        }

        if (isSoftOccupied(equip.occupancyMode) && basicSettings.fanMode == StandaloneFanStage.AUTO && fanModeSaved != 0) {
            logIt("Resetting the Fan status back to ${StandaloneFanStage.values()[fanModeSaved]}")
            updateUserIntentPoints(
                equipRef = equip.equipRef,
                point = equip.fanOpMode,
                value = fanModeSaved.toDouble(),
                CCUHsApi.getInstance().ccuUserName
            )
            return StandaloneFanStage.values()[fanModeSaved]
        }
        return StandaloneFanStage.values()[equip.fanOpMode.readPriorityVal().toInt()]
    }

    private fun isFanModeCurrentOccupied(value : Int): Boolean {
        val basicSettings = StandaloneFanStage.values()[value]
        return (basicSettings == StandaloneFanStage.LOW_CUR_OCC || basicSettings == StandaloneFanStage.MEDIUM_CUR_OCC || basicSettings == StandaloneFanStage.HIGH_CUR_OCC)
    }

    private fun isEligibleToAuto(basicSettings: BasicSettings, equip: HyperStatSplitEquip): Boolean{
        return (!isSoftOccupied(equip.occupancyMode) // should not be occupied
            && basicSettings.fanMode != StandaloneFanStage.OFF
            && basicSettings.fanMode != StandaloneFanStage.AUTO
            && basicSettings.fanMode != StandaloneFanStage.LOW_ALL_TIME
            && basicSettings.fanMode != StandaloneFanStage.MEDIUM_ALL_TIME
            && basicSettings.fanMode != StandaloneFanStage.HIGH_ALL_TIME
        )
    }

    fun setOperatingMode(currentTemp: Double, averageDesiredTemp: Double, basicSettings: BasicSettings, hssEquip: HyperStatSplitEquip) {
        var zoneOperatingMode = ZoneState.DEADBAND.ordinal
        if(currentTemp < averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.COOL_ONLY) {
            zoneOperatingMode = ZoneState.HEATING.ordinal
        }
        else if(currentTemp >= averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
            zoneOperatingMode = ZoneState.COOLING.ordinal
        }
        logIt("averageDesiredTemp $averageDesiredTemp" + "zoneOperatingMode ${ZoneState.values()[zoneOperatingMode]}")
        hssEquip.equipStatus.writeHisVal(curState.ordinal.toDouble())
        hssEquip.operatingMode.writeHisVal(zoneOperatingMode.toDouble())
    }

    fun logIt(msg: String){
        CcuLog.d(tag, msg)
    }

    fun getAverageTemp(userIntents: UserIntents): Double{
        return (userIntents.coolingDesiredTemp + userIntents.heatingDesiredTemp) / 2.0
    }

    fun updateOccupancyDetection(hssEquip: HyperStatSplitEquip) {
        if (hssEquip.zoneOccupancy.readHisVal() > 0.0 && hssEquip.occupancyDetection.pointExists()) {
            // That pointExists() call above was NOT redundant.
            // Accessing the .id property does not actually query for the ID. One of the methods on the Point() object needs to be called first to do this.
            haystack.writeHisValueByIdWithoutCOV(hssEquip.occupancyDetection.id, 1.0)
        }
    }

    fun updateLoopOutputs(hssEquip: HyperStatSplitEquip) {
        hssEquip.coolingLoopOutput.writeHisVal(coolingLoopOutput.toDouble())
        hssEquip.heatingLoopOutput.writeHisVal(heatingLoopOutput.toDouble())
        hssEquip.fanLoopOutput.writeHisVal(fanLoopOutput.toDouble())
        hssEquip.compressorLoopOutput.writeHisVal(compressorLoopOutput.toDouble())

        hssEquip.economizingLoopOutput.writeHisVal(economizingLoopOutput.toDouble())
        hssEquip.dcvLoopOutput.writeHisVal(dcvLoopOutput.toDouble())
        hssEquip.outsideAirLoopOutput.writeHisVal(outsideAirLoopOutput.toDouble())
        hssEquip.outsideAirFinalLoopOutput.writeHisVal(outsideAirFinalLoopOutput.toDouble())
    }

    override fun getNodeAddresses() : HashSet<Short> {
        val nodeSet : HashSet<Short> = HashSet()
        nodeSet.add(nodeAddress)
        return nodeSet
    }

    fun doAnalogFanAction(
        fanLowPercent: Int, fanMediumPercent: Int, fanHighPercent: Int,
        basicSettings: BasicSettings,
        fanLoopOutput: Int,
        analogOutStages: HashMap<String, Int>,
        previousFanLoopVal: Int,
        fanProtectionCounter: Int,
        hssEquip: HyperStatSplitEquip,
        fanLogicalPoint: Point
    ) {
        if (basicSettings.fanMode != StandaloneFanStage.OFF) {
            var fanLoopForAnalog = 0
            if (basicSettings.fanMode == StandaloneFanStage.AUTO) {
                if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
                    fanLogicalPoint.writeHisVal(0.0)
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
            if (fanLoopForAnalog > 0) analogOutStages[StatusMsgKeys.FAN_SPEED.name] =
                fanLoopForAnalog
            fanLogicalPoint.writeHisVal(fanLoopForAnalog.toDouble())
        }
    }

    fun isPrePurgeActive() = (epidemicState == EpidemicState.PREPURGE)

    fun getEffectiveOutsideDamperMinOpen(hssEquip: HyperStatSplitEquip, isHeatingActive: Boolean, isCoolingActive: Boolean): Int {
        val outsideDamperMinOpenFromConditioning: Double =
            if (isHeatingActive || isCoolingActive || hssEquip.economizingLoopOutput.readHisVal() > 0.0) {
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

    fun evaluateLoopOutputs(
        userIntents: UserIntents, basicSettings: BasicSettings, tuners: BaseStatTuners, hssEquip: HyperStatSplitEquip
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

        if (epidemicState == EpidemicState.PREPURGE) {
            fanLoopOutput = hssEquip.standalonePrePurgeFanSpeedTuner.readPriorityVal().toInt()
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
     */
    fun updateTitle24LoopCounter(tuners: BaseStatTuners, basicSettings: BasicSettings) {
        // Check if there is change in occupancy and the fan-loop output is less than the previous value,
        // then offer the fan protection
        logIt("Occupancy: $occupancyStatus, Fan Loop Output: $fanLoopOutput, Previous Fan Loop Val: $previousFanLoopVal, Fan Loop Counter: $fanLoopCounter , hasZeroFanLoopBeenHandled $hasZeroFanLoopBeenHandled")

        if((occupancyStatus != previousOccupancyStatus && fanLoopOutput < previousFanLoopVal) || (basicSettings.fanMode != previousFanStageStatus && fanLoopOutput < previousFanLoopVal) ||
            (fanLoopOutput == 0 && fanLoopOutput < previousFanLoopVal && !hasZeroFanLoopBeenHandled)) {
            fanLoopCounter = tuners.minFanRuntimePostConditioning
            hasZeroFanLoopBeenHandled = true
        }
        else if((occupancyStatus != previousOccupancyStatus || (hasZeroFanLoopBeenHandled && fanLoopOutput > 0)) && fanLoopOutput > previousFanLoopVal) {
            fanLoopCounter = 0 // Reset the counter if the fan-loop output is greater than the previous value
            hasZeroFanLoopBeenHandled = false
        }
    }

    /**
     * Updates the Title 24 flags by storing the current occupancy status and fan-loop output.
     * The previous occupancy status and fan-loop output are updated for use in the next loop iteration.
     * If the fan loop counter is greater than 0, it decrements the counter.
     */
    fun updateTitle24Flags(basicSettings: BasicSettings) {
        // Store the fan-loop output/occupancy for usage in next loop
        previousOccupancyStatus = occupancyStatus
        // Store the fan status so that when the zone goes from user defined fan state to unoccupied state, the fan protection can be offered
        previousFanStageStatus = basicSettings.fanMode
        // Store the fanloop output when the zone was not in UNOCCUPIED state
        if (fanLoopCounter == 0) previousFanLoopVal = fanLoopOutput
        if (fanLoopCounter > 0) fanLoopCounter--
    }


    fun getAnalogEconomizerValueActivated(hssEquip: HyperStatSplitEquip): Double {
        return if (hssEquip.fanOutEconomizer.pointExists()) {
            hssEquip.fanOutEconomizer.readDefaultVal()
        } else {
            defaultFanLoopOutput
        }
    }

    fun isEconomizerActive(hssEquip: HyperStatSplitEquip) = hssEquip.economizingLoopOutput.readHisVal() > 0.0

    fun fetchBasicSettings(
        hssEquip: HyperStatSplitEquip
    ): BasicSettings {

        return BasicSettings(
            conditioningMode = StandaloneConditioningMode.values()[hssEquip.conditioningMode.readPriorityVal().toInt()],
            fanMode = StandaloneFanStage.values()[hssEquip.fanOpMode.readPriorityVal().toInt()]
        )
    }


    /*
    	Economizing is enabled when:
    	    ○ Zone is in Cooling Mode AND
		        ○ OAT < oaoEconomizingDryBulbTemperatureThreshold OR
		        ○ Weather OAEnthalpy is in range AND Local OAEnthalpy < IndoorEnthalpy - EnthalpyDuctCompensationOffset

        This works exactly the same as OAO profile logic, except that the failsafe logic upon weather
        data loss now includes enthalpy if an OAT/H sensor is on the sensor bus
     */
    private fun canDoEconomizing(
        externalTemp: Double,
        externalHumidity: Double,
        config: HyperStatSplitConfiguration,
        hssEquip: HyperStatSplitEquip
    ): Boolean {

        val economizingMinTemp = hssEquip.standaloneEconomizingMinTemperature.readPriorityVal()

        val indoorTemp = hssEquip.currentTemp.readHisVal()
        val indoorHumidity = hssEquip.zoneHumidity.readHisVal()

        val insideEnthalpy = getAirEnthalpy(indoorTemp, indoorHumidity)
        val outsideEnthalpy = getAirEnthalpy(externalTemp, externalHumidity)

        hssEquip.insideEnthalpy.writeHisVal(insideEnthalpy)
        hssEquip.outsideEnthalpy.writeHisVal(outsideEnthalpy)

        logIt(" canDoEconomizing externalTemp $externalTemp externalHumidity $externalHumidity")

        // If zone isn't in cooling mode, stop right here
        if (state != ZoneState.COOLING) return false

        // First, check local dry-bulb temp
        if (isEconomizingEnabledOnDryBulb(
                externalTemp,
                externalHumidity,
                economizingMinTemp,
                hssEquip
            )
        ) {
            logIt("Economizer enabled based on dry-bulb temperature.")
            return true
        }

        if (!isEconomizingTempAndHumidityInRange(
                externalTemp,
                externalHumidity,
                economizingMinTemp,
                config,
                hssEquip
            )
        ) return false

        if (isEconomizingEnabledOnEnthalpy(insideEnthalpy, outsideEnthalpy, config, hssEquip)) {
            logIt("Economizing enabled based on enthalpy.")
            return true
        }

        logIt("Economizing disabled based on enthalpy.")
        return false

    }

    /*
         DCV is enabled when:
         ○ Zone is in Occupied Mode AND
         ○ Zone CO2 (sensed on HyperLite) > Zone CO2 Threshold
     */
    private fun doDcv(hssEquip: HyperStatSplitEquip, standaloneOutsideAirDamperMinOpen: Int) {
        dcvAvailable = false
        val zoneSensorCO2 = hssEquip.zoneCO2.readHisVal()
        val zoneCO2Threshold = hssEquip.co2Threshold.readDefaultVal()
        val co2DamperOpeningRate = hssEquip.co2DamperOpeningRate.readDefaultVal()
        logIt(
            "zoneSensorCO2: $zoneSensorCO2, zoneCO2Threshold: $zoneCO2Threshold, co2DamperOpeningRate: $co2DamperOpeningRate"
        )
        if (isSoftOccupied(zoneOccupancyState)) {
            if (zoneSensorCO2 > zoneCO2Threshold) {
                dcvAvailable = true
                dcvLoopOutput = max(
                    0,
                    min(((zoneSensorCO2 - zoneCO2Threshold) / co2DamperOpeningRate).toInt(), 100)
                )
                outsideAirCalculatedMinDamper = max(dcvLoopOutput, standaloneOutsideAirDamperMinOpen)
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

    private fun doDcvForDcvDamper(hssEquip: HyperStatSplitEquip) {
        dcvAvailable = false
        val zoneSensorCO2 = hssEquip.zoneCO2.readHisVal()
        val zoneCO2Threshold = hssEquip.co2Threshold.readDefaultVal()
        val co2DamperOpeningRate = hssEquip.co2DamperOpeningRate.readDefaultVal()
        logIt(
            "zoneSensorCO2: $zoneSensorCO2, zoneCO2Threshold: $zoneCO2Threshold, co2DamperOpeningRate: $co2DamperOpeningRate"
        )
        if (isSoftOccupied(zoneOccupancyState)) {
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
        economizingMinTemp: Double,
        config: HyperStatSplitConfiguration,
        hssEquip: HyperStatSplitEquip
    ): Boolean {
        val economizingMaxTemp = hssEquip.standaloneEconomizingMaxTemperature.readPriorityVal()
        val economizingMinHumidity = hssEquip.standaloneEconomizingMinHumidity.readPriorityVal()
        val economizingMaxHumidity = hssEquip.standaloneEconomizingMaxHumidity.readPriorityVal()
        var outsideTemp = externalTemp
        var outsideHumidity = externalHumidity

        if (outsideTemp == 0.0 && outsideHumidity == 0.0 &&
            HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToOutsideAir(config)
        ) {
            outsideTemp = hssEquip.outsideTemperature.readHisVal()
            outsideHumidity = hssEquip.outsideHumidity.readHisVal()
        }

        if (outsideTemp > economizingMinTemp && outsideTemp < economizingMaxTemp
            && outsideHumidity > economizingMinHumidity && outsideHumidity < economizingMaxHumidity
        ) return true

        logIt("Outside air ($outsideTemp°F, $outsideHumidity%RH) out of temp/humidity range from tuners; economizing disabled")
        return false
    }

    /*
        Same enthalpy-enable condition as OAO Profile.
        If systemOutsideEnthalpy < insideEnthalpy + enthalpyDuctCompensationOffset (0 BTU/lb, adj.), enable economizing
        (Start with systemOutsideEnthalpy. If it's not available and OAT/H sensor is on sensor bus, then calculate outsideEnthalpy
        based on sensed Outside Air Temperature & Humidity.
     */
    private fun isEconomizingEnabledOnEnthalpy(
        insideEnthalpy: Double,
        outsideEnthalpy: Double,
        config: HyperStatSplitConfiguration,
        hssEquip: HyperStatSplitEquip
    ): Boolean {

        logIt(
            "Checking enthalpy-enable condition: insideEnthalpy $insideEnthalpy, outsideEnthalpy $outsideEnthalpy"
        )

        var outsideEnthalpyToUse = outsideEnthalpy

        // Our enthalpy calc returns a value of 0.12 for zero temp and humidity.
        // We will assume anything less than 1 translates to system weather data being dead
        if (outsideEnthalpy < 1 &&
            HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToOutsideAir(config)
        ) {
            logIt(
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
        Same dry-bulb enable logic as OAO Profile.
        If outsideTemp is between economizingMinTemp (0°F, adj.) and dryBulbTemperatureThreshold (55°F, adj.), then enable economizing.
        (for outsideTemp, start with systemOutsideTemp, and use Outside Air Temperature sensor if systemOutsideTemp is 0)
     */
    private fun isEconomizingEnabledOnDryBulb(
        externalTemp: Double,
        externalHumidity: Double,
        economizingMinTemp: Double,
        hssEquip: HyperStatSplitEquip
    ): Boolean {

        var outsideAirTemp = externalTemp
        if (externalHumidity == 0.0 && externalTemp == 0.0) {
            logIt("System outside temp and humidity are both zero; using local OAT sensor")
            outsideAirTemp = hssEquip.outsideTemperature.readHisVal()
        }
        if (outsideAirTemp > economizingMinTemp) {
            return outsideAirTemp < hssEquip.standaloneEconomizingDryBulbThreshold.readPriorityVal()
        }
        return false
    }

    /**
     * Handles the smart pre-purge control for the given HyperStatSplitCpuEconEquip.
     * If pre-purge is active, this method sets the value of outsideAirCalculatedMinDamper
     */
    private fun handleSmartPrePurgeControl(hssEquip: HyperStatSplitEquip) {
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

    private fun doEconomizing(numberConfiguredCoolingStages: Int, config: HyperStatSplitConfiguration, hssEquip: HyperStatSplitEquip) {

        val sharedPreferences = Globals.getInstance().applicationContext.getSharedPreferences(
            "ccu_devsetting",
            Context.MODE_PRIVATE
        )
        val isTestModeEnabled = Globals.getInstance().isWeatherTest

        val externalTemp: Double
        val externalHumidity: Double

        if (isTestModeEnabled) {
            externalTemp = sharedPreferences.getInt("outside_temp", 0).toDouble()
            externalHumidity = sharedPreferences.getInt("outside_humidity", 0).toDouble()
        } else if (isOfflineMode()) {
            externalTemp = CCUHsApi.getInstance()
                .readHisValByQuery("domainName == \"outsideTemperature\" and equipRef == \"${hssEquip.equipRef}\"")
            externalHumidity = CCUHsApi.getInstance()
                .readHisValByQuery("domainName == \"outsideHumidity\" and equipRef == \"${hssEquip.equipRef}\"")
        } else {
            externalTemp = CCUHsApi.getInstance()
                .readHisValByQuery("system and outside and temp and not lockout")
            externalHumidity = CCUHsApi.getInstance()
                .readHisValByQuery("system and outside and humidity")
        }

        // Check for economizer enable
        if (canDoEconomizing(externalTemp, externalHumidity, config, hssEquip)) {

            economizingAvailable = true

            val economizingToMainCoolingLoopMap =
                hssEquip.standaloneEconomizingToMainCoolingLoopMap.readPriorityVal()
            if (numberConfiguredCoolingStages > 0) {
                economizingLoopOutput =
                    min((coolingLoopOutput * (numberConfiguredCoolingStages + 1)), 100)
                logIt(
                    (numberConfiguredCoolingStages + 1).toString() + " cooling stages available (including economizer); economizingLoopOutput = " + economizingLoopOutput
                )
            } else {
                logIt(
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

    fun evaluateOAOLoop(
        basicSettings: BasicSettings,
        isCondensateTripped: Boolean,
        effectiveOutsideDamperMinOpen: Int,
        config: HyperStatSplitConfiguration,
        numberConfiguredCoolingStages: Int,
        isAnalogOutHasOAOMapping: Boolean,
        hssEquip: HyperStatSplitEquip
    ) {

        // If there's not an OAO damper mapped,
        if (!isAnalogOutHasOAOMapping) {
            val sharedPreferences = Globals.getInstance().applicationContext.getSharedPreferences(
                "ccu_devsetting",
                Context.MODE_PRIVATE
            )
            val isTestModeEnabled = Globals.getInstance().isWeatherTest
            economizingAvailable = false
            economizingLoopOutput = 0
            dcvAvailable = false
            dcvLoopOutput = 0
            if (hssEquip.dcvDamper.pointExists()) doDcvForDcvDamper(hssEquip)
            outsideAirLoopOutput = 0
            matThrottle = false
            outsideAirFinalLoopOutput = 0

            val externalTemp: Double
            val externalHumidity: Double
            // Even if there's not an OAO damper, still calculate enthalpy for portal widgets
            if (isTestModeEnabled) {
                externalTemp = sharedPreferences.getInt("outside_temp", 0).toDouble()
                externalHumidity = sharedPreferences.getInt("outside_humidity", 0).toDouble()

            } else if (isOfflineMode()) {
                externalTemp = CCUHsApi.getInstance()
                    .readHisValByQuery("domainName == \"outsideTemperature\" and equipRef == \"${hssEquip.equipRef}\"")
                externalHumidity = CCUHsApi.getInstance()
                    .readHisValByQuery("domainName == \"outsideHumidity\" and equipRef == \"${hssEquip.equipRef}\"")
            } else {
                externalTemp = CCUHsApi.getInstance()
                    .readHisValByQuery("system and outside and temp and not lockout")
                externalHumidity = CCUHsApi.getInstance()
                    .readHisValByQuery("system and outside and humidity")
            }

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

            handleSmartPrePurgeControl(hssEquip)

            doEconomizing(numberConfiguredCoolingStages, config, hssEquip)
            doDcv(hssEquip, effectiveOutsideDamperMinOpen)

            outsideAirLoopOutput = if (epidemicState == EpidemicState.PREPURGE) {
                max(economizingLoopOutput, outsideAirCalculatedMinDamper)
            } else {
                max(economizingLoopOutput, dcvLoopOutput)
            }

            logIt(
                "outsideAirLoopOutput " + outsideAirLoopOutput + " oaoDamperMatTarget " + oaoDamperMatTarget + " oaoDamperMatMin " + oaoDamperMatMin
                        + " matTemp " + matTemp
            )

            matThrottle = false

            // If Conditioning Mode is AUTO or COOL_ONLY, run full economizer loop algo
            if (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO || basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY) {
                if (matTemp < oaoDamperMatTarget) matThrottle = true
                outsideAirFinalLoopOutput = if (outsideAirLoopOutput > effectiveOutsideDamperMinOpen) {
                    if (matTemp < oaoDamperMatTarget && matTemp > oaoDamperMatMin) {
                        (outsideAirLoopOutput - outsideAirLoopOutput * ((oaoDamperMatTarget - matTemp) / (oaoDamperMatTarget - oaoDamperMatMin))).toInt()
                    } else {
                        if (matTemp <= oaoDamperMatMin) 0 else outsideAirLoopOutput
                    }
                } else {
                    effectiveOutsideDamperMinOpen
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
                logIt(
                    "Condensate Switch is tripped. outsideAirFinalLoopOutput set to zero."
                )
                outsideAirFinalLoopOutput = 0
            }

            // If Conditioning Mode is HEAT_ONLY or OFF, ignore economizingLoopOutput and do DCV only.
            // Continue to use same the mixed air low-limit and high-limit as full economizing scenario.
            else {
                val dcvOnlyOutsideAirLoopOutput = max(dcvLoopOutput, effectiveOutsideDamperMinOpen)
                if (matTemp < oaoDamperMatTarget) matThrottle = true

                outsideAirFinalLoopOutput = if (dcvOnlyOutsideAirLoopOutput > effectiveOutsideDamperMinOpen) {
                    if (matTemp < oaoDamperMatTarget && matTemp > oaoDamperMatMin) {
                        (dcvOnlyOutsideAirLoopOutput - dcvOnlyOutsideAirLoopOutput * ((oaoDamperMatTarget - matTemp) / (oaoDamperMatTarget - oaoDamperMatMin))).toInt()
                    } else {
                        if (matTemp <= oaoDamperMatMin) 0 else dcvOnlyOutsideAirLoopOutput
                    }
                } else {
                    effectiveOutsideDamperMinOpen
                }

                outsideAirFinalLoopOutput = if (matThrottle) {
                    max(outsideAirFinalLoopOutput, 0)
                } else {
                    max(outsideAirFinalLoopOutput, effectiveOutsideDamperMinOpen)
                }
                outsideAirFinalLoopOutput = min(outsideAirFinalLoopOutput, 100)
                logIt(
                    "Conditioning Mode is HEAT-ONLY or OFF. outsideAirFinalLoopOutput to consider DCV only."
                )
            }

            logIt(
                " economizingLoopOutput " + economizingLoopOutput + " dcvLoopOutput " + dcvLoopOutput
                        + " outsideAirFinalLoopOutput " + outsideAirFinalLoopOutput + " effectiveOutsideDamperMinOpen " + effectiveOutsideDamperMinOpen
            )

            hssEquip.outsideAirFinalLoopOutput.writeHisVal(outsideAirFinalLoopOutput.toDouble())
            hssEquip.oaoDamper.writeHisVal(outsideAirFinalLoopOutput.toDouble())

            val matThrottleNumber = if (matThrottle) 1.0 else 0.0
            hssEquip.matThrottle.writeHisVal(matThrottleNumber)
        }
    }

    fun isEmergencyShutoffActive(hssEquip: HyperStatSplitEquip): Boolean {
        return (hssEquip.emergencyShutoffNC.pointExists() && hssEquip.emergencyShutoffNC.readHisVal() > 0.0
                || hssEquip.emergencyShutoffNO.pointExists() && hssEquip.emergencyShutoffNO.readHisVal() > 0.0)
    }

    fun doorWindowIsOpen(equip: HyperStatSplitEquip) {
        var doorWindowEnabled = 0.0
        var doorWindowSensor = 0.0
        isDoorOpenFromTitle24 = false
        isDoorOpen = false
        listOf(
            equip.doorWindowSensorNCTitle24,
            equip.doorWindowSensorTitle24,
            equip.doorWindowSensorNOTitle24,
            equip.doorWindowSensorNC,
            equip.doorWindowSensor,
            equip.doorWindowSensorNO,
        ).forEach {
            if (it.pointExists()) {
                if (doorWindowEnabled != 1.0) {
                    doorWindowEnabled = 1.0
                }
                if (it.readHisVal() > 0) {
                    doorWindowSensor = 1.0
                    if (it == equip.doorWindowSensorNCTitle24 ||
                        it == equip.doorWindowSensorTitle24 ||
                        it == equip.doorWindowSensorNOTitle24
                    ) {
                        isDoorOpenFromTitle24 = true
                    }
                }
            }
        }
        equip.doorWindowSensingEnable.writePointValue(doorWindowEnabled)
        equip.doorWindowSensorInput.writePointValue(doorWindowSensor)
        isDoorOpen =  (doorWindowSensor == 1.0 && occupancyStatus == Occupancy.WINDOW_OPEN)
    }

    fun keyCardIsInSlot(
        equip: HyperStatSplitEquip
    ) {
        var keycardEnabled = 0.0
        var keycardSensor = 0.0
        if (equip.keyCardSensorNO.pointExists() || equip.keyCardSensorNC.pointExists()) {
            keycardEnabled = 1.0
            if (equip.keyCardSensorNO.readHisVal() > 0) {
                keycardSensor = 1.0
            }
            if (equip.keyCardSensorNC.readHisVal() > 0) {
                keycardSensor = 1.0
            }
        }
        equip.keyCardSensingEnable.writePointValue(keycardEnabled)
        equip.keyCardSensorInput.writePointValue(keycardSensor)
    }

}