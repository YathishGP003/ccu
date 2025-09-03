package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon

import a75f.io.domain.api.Domain
import a75f.io.domain.api.Point
import a75f.io.domain.equips.unitVentilator.HsSplitCpuEquip
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.EpidemicState
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitProfile
import a75f.io.logic.bo.building.statprofiles.statcontrollers.SplitControllerFactory
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.canWeDoConditioning
import a75f.io.logic.bo.building.statprofiles.util.canWeDoCooling
import a75f.io.logic.bo.building.statprofiles.util.canWeDoHeating
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getPercentFromVolt
import a75f.io.logic.bo.building.statprofiles.util.getSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getSplitTuners
import a75f.io.logic.bo.building.statprofiles.util.isHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMediumUserIntentFanMode
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.handlers.doAnalogOperation
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.util.uiutils.HyperStatSplitUserIntentHandler
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


/**
 * @author tcase@75f.io (HyperStat CPU)
 * Created on 7/7/21.
 *
 * Created for HyperStat Split CPU/Econ by Nick P on 07-24-2023.
 */
class HyperStatSplitCpuEconProfile(private val equipRef: String, nodeAddress: Short) : HyperStatSplitProfile(equipRef, nodeAddress, L.TAG_CCU_HSSPLIT_CPUECON) {

    lateinit var hssEquip: HsSplitCpuEquip

    override fun updateZonePoints() {
        hssEquip = Domain.getEquip(equipRef) as HsSplitCpuEquip

        if (Globals.getInstance().isTestMode) {
            logIt("Test mode is on: $nodeAddress")
            return
        }

        if (mInterface != null) mInterface.refreshView()

        if (isRFDead) {
            handleRFDead(hssEquip)
            return
        } else if (isZoneDead) {
            handleDeadZone(hssEquip)
            return
        }

        controllerFactory = SplitControllerFactory(
            hssEquip, L.TAG_CCU_HSSPLIT_CPUECON, controllers, stageCounts,
            isEconAvailable, derivedFanLoopOutput, zoneOccupancyState
        )
        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode
        val config = getSplitConfiguration(equipRef) as HyperStatSplitCpuConfiguration
        resetEquip(hssEquip)
        val hyperStatSplitTuners = getSplitTuners(hssEquip)
        val userIntents = fetchUserIntents(hssEquip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val outsideDamperMinOpen = getEffectiveOutsideDamperMinOpen(hssEquip, isHeatingActive(), isCoolingActive())
        val fanModeSaved = FanModeCacheStorage.getHyperStatSplitFanModeCache().getFanModeFromCache(equipRef)

        val isCondensateTripped: Boolean =
            hssEquip.condensateStatusNC.readHisVal() > 0.0 || hssEquip.condensateStatusNO.readHisVal() > 0.0
        if (isCondensateTripped) logIt( "Condensate overflow detected")

        // At this point, Conditioning Mode will be set to OFF if Condensate Overflow is detected
        // It will revert to previous value when Condensate returns to normal
        val basicSettings = fetchBasicSettings(hssEquip)

        val updatedFanMode = fallBackFanMode(hssEquip, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode

        loopController.initialise(tuners = hyperStatSplitTuners)
        loopController.dumpLogs()
        handleChangeOfDirection(userIntents, controllerFactory, hssEquip)
        doorWindowIsOpen(hssEquip)
        keyCardIsInSlot(hssEquip)
        prePurgeEnabled = hssEquip.prePurgeEnable.readDefaultVal() > 0.0
        prePurgeOpeningValue = hssEquip.standalonePrePurgeFanSpeedTuner.readPriorityVal()

        resetLoopOutputs()
        evaluateLoopOutputs(userIntents, basicSettings, hyperStatSplitTuners, hssEquip)
        val highestCoolingStages = config.getHighestCoolingStageCount()
        doDcv(hssEquip, outsideDamperMinOpen)
        evaluateOAOLoop(basicSettings, isCondensateTripped, outsideDamperMinOpen, config, highestCoolingStages, hssEquip.oaoDamper.pointExists(), hssEquip)

        updateOccupancyDetection(hssEquip)
        updateLoopOutputs(hssEquip)

        if (equipOccupancyHandler != null) {
            occupancyStatus = equipOccupancyHandler.currentOccupiedMode
            zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        }
        updateTitle24LoopCounter(hyperStatSplitTuners, basicSettings)
        if (isEmergencyShutoffActive(hssEquip).not() && isDoorOpen.not() && isCondensateTripped.not()) {
            if (basicSettings.fanMode != StandaloneFanStage.OFF) {
                runRelayOperations(config, basicSettings)
                runAnalogOutOperations(config, basicSettings, hssEquip.analogOutStages)
            } else {
                resetAllLogicalPointValues()
            }
        } else {
            resetAllLogicalPointValues()
            if (isDoorOpenFromTitle24) {
                runLowestFanSpeedDuringDoorOpen(hssEquip)
            }

        }
        setOperatingMode(currentTemp, averageDesiredTemp, basicSettings, hssEquip)

        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState =
            ZoneTempState.EMERGENCY
        logIt(
            "Analog Fan speed multiplier  ${hyperStatSplitTuners.analogFanSpeedMultiplier} \n" +
                    "Current Working mode : $occupancyStatus\n" +
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
                    "isCondensateTripped:: $isCondensateTripped \n"+
                    "Econ Active $economizingAvailable \n"
        )
        logResults(config)
        logIt("Equip Running : $curState")

        HyperStatSplitUserIntentHandler.updateHyperStatSplitStatus(
            hssEquip.getId(), hssEquip.relayStages, hssEquip.analogOutStages, temperatureState,
            economizingLoopOutput, dcvLoopOutput, outsideDamperMinOpen, outsideAirFinalLoopOutput,
            if (hssEquip.condensateStatusNC.readHisVal() > 0.0 || hssEquip.condensateStatusNO.readHisVal() > 0.0) 1.0 else 0.0,
            if (hssEquip.filterStatusNC.readHisVal() > 0.0 || hssEquip.filterStatusNO.readHisVal() > 0.0) 1.0 else 0.0,
            basicSettings, epidemicState, isEmergencyShutoffActive(hssEquip), tag
        )

        wasCondensateTripped = isCondensateTripped

        logIt( "processHyperStatSplitCpuEconProfile() complete")
        updateTitle24Flags(basicSettings)
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
                // added the new check if the fan loop output is with in relayActivationHysteresis ,sending the Analog recirculate value
                val relayActivationHysteresis = hssEquip.standaloneRelayActivationHysteresis.readPriorityVal()
                if ((fanLoopOutput == 0 ||  (fanLoopOutput > 0 && fanLoopOutput < relayActivationHysteresis) ) && fanProtectionCounter == 0 && checkIfInOccupiedMode()) { // When in dead-band, set the fan-loopForAnalog to the recirculate analog value
                    fanLoopForAnalog = getPercentFromVolt(getAnalogRecirculateValueActivated().roundToInt())
                    logMsg = "Deadband"
                }
                // The speed at which fan operates during economization is determined by configuration parameter - "Analog-Out During Economizer"
                // We also need to ensure that the currently no cooling stage is activated since we switch on Economization Value only when the
                // cooling stage is not activated
                val analogEconomizerValueActivated =
                    getPercentFromVolt(getAnalogEconomizerValueActivated(hssEquip).roundToInt())
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
            logIt( " Staged Fan Speed calculated ($logMsg) == $fanLoopForAnalog")
        }
    }


   override fun resetRelayLogicalPoints() {
        hssEquip.apply {
            listOf(
                coolingStage1, coolingStage2, coolingStage3,
                heatingStage1, heatingStage2, heatingStage3,
                fanLowSpeed, fanMediumSpeed, fanHighSpeed,
                fanEnable, occupiedEnable, humidifierEnable, dehumidifierEnable,
                exhaustFanStage1, exhaustFanStage2, dcvDamper, compressorStage1,
                compressorStage2, compressorStage3, auxHeatingStage1, auxHeatingStage2,
                changeOverCooling, changeOverHeating
            ).forEach { resetPoint(it) }
        }
    }

    override fun resetAnalogOutLogicalPoints() {
        hssEquip.apply {
            listOf(
                coolingSignal, heatingSignal, linearFanSpeed, oaoDamper,
                stagedFanSpeed, compressorSpeed, dcvDamperModulating
            ).forEach { resetPoint(it) }
        }
    }

    override fun resetAllLogicalPointValues() {
        resetLoops(hssEquip)
        resetRelayLogicalPoints()
        resetAnalogOutLogicalPoints()
        HyperStatSplitUserIntentHandler.updateHyperStatSplitStatus(
            equipId = equipRef,
            portStages = HashMap(),
            analogOutStages = HashMap(),
            temperatureState = ZoneTempState.TEMP_DEAD,
            economizingLoopOutput, dcvLoopOutput,
            getEffectiveOutsideDamperMinOpen(hssEquip, isHeatingActive(), isCoolingActive()), outsideAirFinalLoopOutput,
            if (hssEquip.isCondensateTripped()) 1.0 else 0.0,
            if (hssEquip.filterStatusNC.readHisVal() > 0.0 || hssEquip.filterStatusNO.readHisVal() > 0.0) 1.0 else 0.0,
            fetchBasicSettings(hssEquip),
            EpidemicState.OFF, isEmergencyShutoffActive(hssEquip), tag
        )
    }

    override fun getCurrentTemp() = hssEquip.currentTemp.readHisVal()

    override fun getDisplayCurrentTemp() = averageZoneTemp

    override fun getAverageZoneTemp() = 0.0

    private fun getCoolingStateActivated(): Double {
        val stages = listOf(
            hssEquip.coolingStage3.readHisVal() to hssEquip.fanOutCoolingStage3.readDefaultVal(),
            hssEquip.coolingStage2.readHisVal() to hssEquip.fanOutCoolingStage2.readDefaultVal(),
            hssEquip.coolingStage1.readHisVal() to hssEquip.fanOutCoolingStage1.readDefaultVal()
        )

        return stages.firstOrNull { it.first == 1.0 }?.second
            ?: if (isEconomizerActive(hssEquip)) {
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

    fun isHeatingActive(): Boolean {
        return (hssEquip.heatingStage1.readHisVal() == 1.0
                || hssEquip.heatingStage2.readHisVal() == 1.0
                || hssEquip.heatingStage3.readHisVal() == 1.0
                || hssEquip.heatingSignal.readHisVal() > 0.0 ) ||
                (hssEquip.heatingLoopOutput.readHisVal() > 0 && isCompressorActivated())
    }

    fun isCoolingActive(): Boolean {
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

        if (coolingLoopOutput > 0) {
            isEconAvailable.data = if (economizingAvailable) 1.0 else 0.0
        } else {
            isEconAvailable.data = 0.0
        }
        fanLowVentilationAvailable.data = if (hssEquip.fanLowSpeedVentilation.pointExists()) 1.0 else 0.0
        /**
         * This is where we run the controllers for the equipment. they need dynamic loop points values
         * when constraint executes they will calculate the value based on the current loop points
         */
        derivedFanLoopOutput.data = hssEquip.fanLoopOutput.readHisVal()

        // This is for title 24 compliance
        if (fanLoopCounter > 0) {
            derivedFanLoopOutput.data = previousFanLoopVal.toDouble()
        }
        logIt("derivedFanLoopOutput $derivedFanLoopOutput")
        controllerFactory.addCpuEconControllers(config, ::isPrePurgeActive, fanLowVentilationAvailable)
        logIt(" isEconAvailable: ${isEconAvailable.data} " +
                "zoneOccupancyState : ${zoneOccupancyState.data}")
    }

    private fun runControllers(equip: HsSplitCpuEquip, basicSettings: BasicSettings, config: HyperStatSplitCpuConfiguration) {
        controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            val result = controller.runController()
            updateRelayStatus(controllerName, result, equip, basicSettings, config)
        }
    }

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

                val mapping = CpuAnalogControlType.values()[association]
                when (mapping) {
                    CpuAnalogControlType.COOLING -> {
                        doAnalogOperation(
                            canWeDoCooling(basicSettings.conditioningMode),
                            analogOutStages, StatusMsgKeys.COOLING.name,
                            coolingLoopOutput, hssEquip.coolingSignal
                        )
                    }

                    CpuAnalogControlType.HEATING -> {
                        doAnalogOperation(
                            canWeDoHeating(basicSettings.conditioningMode),
                            analogOutStages, StatusMsgKeys.HEATING.name,
                            heatingLoopOutput, hssEquip.heatingSignal
                        )
                    }

                    CpuAnalogControlType.LINEAR_FAN -> {
                        val voltage = config.getFanConfiguration(port)
                        val fanLowPercent = voltage.linearFanAtFanLow.currentVal.toInt()
                        val fanMediumPercent = voltage.linearFanAtFanMedium.currentVal.toInt()
                        val fanHighPercent = voltage.linearFanAtFanHigh.currentVal.toInt()
                        doAnalogFanAction(
                            fanLowPercent, fanMediumPercent, fanHighPercent,
                            basicSettings,
                            fanLoopOutput,
                            analogOutStages,
                            previousFanLoopVal,
                            fanLoopCounter,
                            hssEquip,
                            hssEquip.linearFanSpeed
                        )
                    }

                    CpuAnalogControlType.STAGED_FAN -> {
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

                    CpuAnalogControlType.OAO_DAMPER -> {

                        hssEquip.oaoDamper.writeHisVal(outsideAirFinalLoopOutput.toDouble())
                        if (outsideAirFinalLoopOutput > 0) {
                            analogOutStages[StatusMsgKeys.OAO_DAMPER.name] =
                                outsideAirFinalLoopOutput
                        }
                    }

                    CpuAnalogControlType.RETURN_DAMPER -> {
                        val returnDamperCmd = 100 - outsideAirFinalLoopOutput
                        hssEquip.returnDamperPosition.writeHisVal(returnDamperCmd.toDouble())
                        if (returnDamperCmd > 0) {
                            analogOutStages[StatusMsgKeys.RETURN_DAMPER.name] = returnDamperCmd
                        }
                    }

                    CpuAnalogControlType.COMPRESSOR_SPEED -> {
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

                    CpuAnalogControlType.DCV_MODULATING_DAMPER -> {
                        hssEquip.dcvDamperModulating.writePointValue(dcvLoopOutput.toDouble())
                        if (dcvLoopOutput > 0) {
                            analogOutStages[StatusMsgKeys.DCV_DAMPER.name] = dcvLoopOutput
                        }
                    }

                    CpuAnalogControlType.EXTERNALLY_MAPPED -> { /** DO NOTHING  */ }

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
        controllerName: String, result: Any, equip: HsSplitCpuEquip,
        basicSettings: BasicSettings, config: HyperStatSplitCpuConfiguration
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
                    if (isActive) curState = ZoneState.COOLING
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
                    if (isActive) curState = ZoneState.HEATING
                    when (stage) {
                        0 -> updateStatus(equip.heatingStage1, isActive, Stage.HEATING_1.displayName)
                        1 -> updateStatus(equip.heatingStage2, isActive, Stage.HEATING_2.displayName)
                        2 -> updateStatus(equip.heatingStage3, isActive, Stage.HEATING_3.displayName)
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
                        canWeDoConditioning(basicSettings) &&
                                (currentState || (fanEnabledStatus && fanLoopOutput > 0 && isLowestStageActive))
                    } else {
                        checkUserIntentAction(stage)
                    }
                }

                val fanStages = result as List<Pair<Int, Boolean>>
                fanStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateStatus(equip.fanLowSpeed, isStageActive(stage, isActive, lowestStageFanLow), Stage.FAN_1.displayName)
                        1 -> updateStatus(equip.fanMediumSpeed, isStageActive(stage, isActive, lowestStageFanMedium), Stage.FAN_2.displayName)
                        2 -> updateStatus(equip.fanHighSpeed, isStageActive(stage, isActive, lowestStageFanHigh), Stage.FAN_3.displayName)
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
                        0 -> updateStatus(equip.compressorStage1, isActive, if (state == ZoneState.COOLING) Stage.COOLING_1.displayName else if (state == ZoneState.HEATING) Stage.HEATING_1.displayName else "")
                        1 -> updateStatus(equip.compressorStage2, isActive, if (state == ZoneState.COOLING) Stage.COOLING_2.displayName else if (state == ZoneState.HEATING) Stage.HEATING_2.displayName else "")
                        2 -> updateStatus(equip.compressorStage3, isActive, if (state == ZoneState.COOLING) Stage.COOLING_3.displayName else if (state == ZoneState.HEATING) Stage.HEATING_3.displayName else "")
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
            ControllerNames.OCCUPIED_ENABLED -> updateStatus(equip.occupiedEnable,result, StatusMsgKeys.EQUIP_ON.name)
            ControllerNames.HUMIDIFIER_CONTROLLER -> updateStatus(equip.humidifierEnable,result)
            ControllerNames.DEHUMIDIFIER_CONTROLLER -> updateStatus(equip.dehumidifierEnable,result)
            ControllerNames.EXHAUST_FAN_STAGE1_CONTROLLER -> updateStatus(equip.exhaustFanStage1,result)
            ControllerNames.EXHAUST_FAN_STAGE2_CONTROLLER -> updateStatus(equip.exhaustFanStage2,result)
            ControllerNames.DAMPER_RELAY_CONTROLLER -> updateStatus(equip.dcvDamper,result, StatusMsgKeys.DCV_DAMPER.name)
            ControllerNames.AUX_HEATING_STAGE1 -> {
                var status = result as Boolean
                if (canWeDoHeating(basicSettings.conditioningMode).not()) {
                    status = false
                }
                updateStatus(equip.auxHeatingStage1, status, StatusMsgKeys.AUX_HEATING_STAGE1.name)
            }

            ControllerNames.AUX_HEATING_STAGE2 -> {
                var status = result as Boolean
                if (canWeDoHeating(basicSettings.conditioningMode).not()) {
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
                logIt( "Unknown controller: $controllerName")
            }
        }
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
                val mapping = CpuAnalogControlType.values()[association]
                val modulation = when (mapping) {
                    CpuAnalogControlType.COOLING -> hssEquip.coolingSignal
                    CpuAnalogControlType.LINEAR_FAN -> hssEquip.linearFanSpeed
                    CpuAnalogControlType.HEATING -> hssEquip.heatingSignal
                    CpuAnalogControlType.OAO_DAMPER -> hssEquip.oaoDamper
                    CpuAnalogControlType.STAGED_FAN -> hssEquip.stagedFanSpeed
                    CpuAnalogControlType.RETURN_DAMPER -> hssEquip.returnDamperPosition
                    CpuAnalogControlType.COMPRESSOR_SPEED -> hssEquip.compressorSpeed
                    CpuAnalogControlType.DCV_MODULATING_DAMPER -> hssEquip.dcvDamperModulating
                    else-> { null }
                }
                if (modulation != null) {
                    logIt( "$port = ${mapping.name}  analogSignal  ${modulation.readHisVal()}")
                }
            }
        }

    }

}



