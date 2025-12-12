package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator

import a75f.io.domain.api.Domain
import a75f.io.domain.api.Point
import a75f.io.domain.equips.hyperstatsplit.Pipe4UVEquip
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.EpidemicState
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.statprofiles.statcontrollers.SplitControllerFactory
import a75f.io.logic.bo.building.statprofiles.util.AuxActiveStages
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings
import a75f.io.logic.bo.building.statprofiles.util.ControlVia
import a75f.io.logic.bo.building.statprofiles.util.FAN_HIGHEST_STAGE
import a75f.io.logic.bo.building.statprofiles.util.FAN_LOWEST_STAGE
import a75f.io.logic.bo.building.statprofiles.util.FAN_MEDIUM_STAGE
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.UvTuners
import a75f.io.logic.bo.building.statprofiles.util.canWeDoConditioning
import a75f.io.logic.bo.building.statprofiles.util.canWeDoCooling
import a75f.io.logic.bo.building.statprofiles.util.canWeDoHeating
import a75f.io.logic.bo.building.statprofiles.util.canWeOperate
import a75f.io.logic.bo.building.statprofiles.util.canWeRunFan
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getUnitVentilatorTuners
import a75f.io.logic.bo.building.statprofiles.util.isHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMediumUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.keyCardIsInSlot
import a75f.io.logic.bo.building.statprofiles.util.runLowestFanSpeedDuringDoorOpen
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.handlers.doAnalogOperation
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.util.uiutils.HyperStatSplitUserIntentHandler

/**
 * Author: Manjunath Kundaragi
 * Created on: 24-07-2025
 */
class Pipe4UnitVentilatorProfile(private val equipRef: String, nodeAddress: Short) :
    UnitVentilatorProfile(equipRef, nodeAddress, L.TAG_CCU_HSSPLIT_PIPE4_UV) {

    lateinit var hssEquip: Pipe4UVEquip
    override fun updateZonePoints() {
        hssEquip = Domain.getEquip(equipRef) as Pipe4UVEquip

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
            hssEquip,
            L.TAG_CCU_HSSPLIT_PIPE4_UV,
            controllers,
            stageCounts,
            isEconAvailable,
            derivedFanLoopOutput,
            zoneOccupancyState
        )

        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode
        controlVia = ControlVia.values()[hssEquip.controlVia.readDefaultVal().toInt()]
        val config = getSplitConfiguration(equipRef) as Pipe4UVConfiguration
        resetEquip(hssEquip)
        val pipe4Tuners = getUnitVentilatorTuners(hssEquip)
        val userIntents = fetchUserIntents(hssEquip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val outsideDamperMinOpen = getEffectiveOutsideDamperMinOpen(hssEquip, isHeatingActive(), isCoolingActive())
        val fanModeSaved =
            FanModeCacheStorage.getHyperStatSplitFanModeCache().getFanModeFromCache(equipRef)
        val isCondensateTripped: Boolean =
            hssEquip.condensateStatusNC.readHisVal() > 0.0 || hssEquip.condensateStatusNO.readHisVal() > 0.0
        if (isCondensateTripped) logIt("Condensate overflow detected")
        // At this point, Conditioning Mode will be set to OFF if Condensate Overflow is detected
        // It will revert to previous value when Condensate returns to normal
        val basicSettings = fetchBasicSettings(hssEquip)
        val updatedFanMode = fallBackFanMode(hssEquip, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode

        loopController.initialise(tuners = pipe4Tuners)
        loopController.dumpLogs()
        handleChangeOfDirection(userIntents, controllerFactory, hssEquip)
        calculateSaTemperingLoop(pipe4Tuners, hssEquip, basicSettings)
        checkDoorWindowSensorStatus(hssEquip)
        keyCardIsInSlot(hssEquip)
        prePurgeEnabled = hssEquip.prePurgeEnable.readDefaultVal() > 0.0
        prePurgeOpeningValue = hssEquip.standalonePrePurgeFanSpeedTuner.readPriorityVal()

        resetLoopOutputs()
        evaluateLoopOutputs(userIntents, basicSettings, pipe4Tuners, hssEquip)
        doDcv(hssEquip, outsideDamperMinOpen)
        evaluateOAOLoop(
            basicSettings,
            isCondensateTripped,
            outsideDamperMinOpen,
            config,
            0, // So no stages are available here
            hssEquip.oaoDamper.pointExists(),
            hssEquip
        )
        updateTitle24LoopCounter(pipe4Tuners, basicSettings)
        updateOccupancyDetection(hssEquip)
        updateLoopOutputs(hssEquip)

        if (equipOccupancyHandler != null) {
            occupancyStatus = equipOccupancyHandler.currentOccupiedMode
            zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        }

        if (isEmergencyShutoffActive(hssEquip).not() && (isDoorOpen.not()) && isCondensateTripped.not()) {
            if (canWeRunFan(basicSettings)) {
                runRelayOperations(config, basicSettings, pipe4Tuners)
                runAnalogOutOperations(config, basicSettings, pipe4Tuners, hssEquip.analogOutStages)
                operateSaTempering(hssEquip, pipe4Tuners, basicSettings)
                val analogFanType = operateAuxBasedFan(hssEquip, basicSettings)
                runSpecifiedAnalogFanSpeed(analogFanType, config)
            } else {
                resetAllLogicalPointValues()
            }
        } else {
            resetAllLogicalPointValues()
            if (isDoorOpenFromTitle24) {
                runLowestFanSpeedDuringDoorOpen(hssEquip, tag)
            }
        }
        setOperatingMode(currentTemp, averageDesiredTemp, basicSettings, hssEquip)

        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState =
            ZoneTempState.EMERGENCY
        logIt(
            "analogFanSpeedMultiplier  ${pipe4Tuners.analogFanSpeedMultiplier} \n" +
                    "Current Working mode : $occupancyStatus" + " Current Temp : $currentTemp  " +
                    "Desired ${userIntents.heatingDesiredTemp} ${userIntents.coolingDesiredTemp} \n" +
                    "Heating Loop: $heatingLoopOutput Cooling Loop: $coolingLoopOutput Fan Loop Output:: $fanLoopOutput \n" +
                    "SATempering Loop Output: $saTemperingLoopOutput \n" +
                    "Economizing Loop Output:: $economizingLoopOutput \n" +
                    "DCV Loop Output:: $dcvLoopOutput \n" +
                    "Calculated Min OAO Damper:: $outsideAirCalculatedMinDamper \n" +
                    "OAO Loop Output (before MAT Safety):: $outsideAirLoopOutput \n" +
                    "OAO Loop Output (after MAT Safety and outsideDamperMinOpen):: $outsideAirFinalLoopOutput \n" +
                    "conditioningMode: ${basicSettings.conditioningMode} fan mode ${basicSettings.fanMode}\n" +
                    "Econ Active $economizingAvailable isCondensateTripped:: $isCondensateTripped \n"
        )
        logResults(config)
        logIt("Equip Running : $curState")

        HyperStatSplitUserIntentHandler.updateHyperStatSplitStatus(
            hssEquip.getId(),
            hssEquip.relayStages,
            hssEquip.analogOutStages,
            temperatureState,
            economizingLoopOutput,
            dcvLoopOutput,
            outsideDamperMinOpen,
            outsideAirFinalLoopOutput,
            if (hssEquip.condensateStatusNC.readHisVal() > 0.0 || hssEquip.condensateStatusNO.readHisVal() > 0.0) 1.0 else 0.0,
            if (hssEquip.filterStatusNC.readHisVal() > 0.0 || hssEquip.filterStatusNO.readHisVal() > 0.0) 1.0 else 0.0,
            basicSettings,
            epidemicState,
            isEmergencyShutoffActive(hssEquip),
            tag
        )

        wasCondensateTripped = isCondensateTripped
        updateTitle24Flags(basicSettings)
        logIt("-------------------------------------------")
    }

    private fun runRelayOperations(
        config: Pipe4UVConfiguration, basicSettings: BasicSettings, tuners: UvTuners
    ) {
        updatePrerequisite(config)
        runControllers(hssEquip, basicSettings, tuners)
    }

    private fun updatePrerequisite(config: Pipe4UVConfiguration) {
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
        controllerFactory.addPipe4Controllers(config, ::isPrePurgeActive , fanLowVentilationAvailable)
        logIt(
            " isEconAvailable: ${isEconAvailable.data} " + "zoneOccupancyState : ${zoneOccupancyState.data}"
        )
    }


    private fun runControllers(
        equip: Pipe4UVEquip,
        basicSettings: BasicSettings,
        tuners: UvTuners
    ) {
        controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            val result = controller.runController()
            updateRelayStatus(controllerName, result, equip, basicSettings, tuners)
        }
    }

    private fun updateRelayStatus(
        controllerName: String,
        result: Any,
        equip: Pipe4UVEquip,
        basicSettings: BasicSettings,
        tuners: UvTuners
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
            ControllerNames.FAN_SPEED_CONTROLLER -> {

                runTitle24Rule(hssEquip)

                fun checkUserIntentAction(stage: Int): Boolean {
                    val mode = equip.fanOpMode
                    return when (stage) {
                        0 -> isLowUserIntentFanMode(mode)
                        1 -> isMediumUserIntentFanMode(mode)
                        2 -> isHighUserIntentFanMode(mode)
                        else -> false
                    }
                }

                fun isStageActive(
                    stage: Int, currentState: Boolean
                ): Boolean {
                    val mode = equip.fanOpMode.readPriorityVal().toInt()
                    return if (mode == StandaloneFanStage.AUTO.ordinal) {
                        (basicSettings.fanMode != StandaloneFanStage.OFF && currentState)
                    } else {
                        checkUserIntentAction(stage)
                    }
                }

                val fanStages = result as List<Pair<Int, Boolean>>

                val highExist = fanStages.find { it.first == FAN_HIGHEST_STAGE }
                val mediumExist = fanStages.find { it.first == FAN_MEDIUM_STAGE }
                val lowExist = fanStages.find { it.first == FAN_LOWEST_STAGE }

                var isHighActive = false
                var isMediumActive = false

                if (equip.fanHighSpeed.pointExists() && highExist != null) {
                    isHighActive =
                        isStageActive(highExist.first, highExist.second)
                    updateStatus(equip.fanHighSpeed, isHighActive, Stage.FAN_3.displayName)
                }

                if (equip.fanMediumSpeed.pointExists() && mediumExist != null) {
                    isMediumActive =
                        if (isHighActive && hssEquip.fanHighSpeed.pointExists()) false else isStageActive(
                            mediumExist.first, mediumExist.second
                        )
                    updateStatus(equip.fanMediumSpeed, isMediumActive, Stage.FAN_2.displayName)
                }

                if ((equip.fanLowSpeed.pointExists() || equip.fanLowSpeedVentilation.pointExists()) && lowExist != null) {
                    val isAnyHighFanActive =
                        ((isHighActive && hssEquip.fanHighSpeed.pointExists()) || (hssEquip.fanMediumSpeed.pointExists() && isMediumActive))
                    val lowFanStatus = isStageActive(lowExist.first, lowExist.second)
                    updateStatus(
                        if (fanLowVentilationAvailable.readHisVal() > 0) equip.fanLowSpeedVentilation else equip.fanLowSpeed,
                        (isAnyHighFanActive.not() && lowFanStatus),
                        Stage.FAN_1.displayName
                    )
                }
            }

            ControllerNames.COOLING_VALVE_CONTROLLER -> {
                var status = result as Boolean
                if (canWeDoCooling(basicSettings.conditioningMode).not() || canWeRunFan(basicSettings).not() || (economizingAvailable
                            && coolingLoopOutput <= tuners.economizingToMainCoolingLoopMap)) {
                    status = false
                }
                updateStatus(equip.chilledWaterCoolValve, status, StatusMsgKeys.COOLING_VALVE.name)
            }

            ControllerNames.HEATING_VALVE_CONTROLLER -> {
                var status = result as Boolean
                if (canWeDoHeating(basicSettings.conditioningMode).not() || canWeRunFan(basicSettings).not()) {
                    status = false
                }
                updateStatus(equip.hotWaterHeatValve, status, StatusMsgKeys.HEATING_VALVE.name)
            }

            ControllerNames.AUX_HEATING_STAGE1 -> {
                var status = result as Boolean
                if (canWeDoHeating(basicSettings.conditioningMode).not()) {
                    status = false
                }
                updateStatus(equip.auxHeatingStage1, status, StatusMsgKeys.AUX_HEATING_STAGE1.name)
                isAuxStage1Active = status
            }

            ControllerNames.AUX_HEATING_STAGE2 -> {
                var status = result as Boolean
                if (canWeDoHeating(basicSettings.conditioningMode).not()) {
                    status = false
                }
                updateStatus(equip.auxHeatingStage2, status, StatusMsgKeys.AUX_HEATING_STAGE2.name)
                isAuxStage2Active = status
            }

            ControllerNames.FACE_BYPASS_CONTROLLER -> {
                var status = result as Boolean

                if (coolingLoopOutput > 0 && (canWeDoCooling(basicSettings.conditioningMode).not()
                            || (economizingAvailable && coolingLoopOutput <= tuners.economizingToMainCoolingLoopMap))
                    || heatingLoopOutput > 0 && canWeDoHeating(basicSettings.conditioningMode).not()
                ) {
                    status = false
                }
                if (fanLoopCounter > 0 && !status) {
                    logIt("Fan Loop Counter is active, Face Bypass Damper Relay holding with previous status")
                    return
                }
                updateStatus(equip.faceBypassDamperCmd, status)
            }

            ControllerNames.FAN_ENABLED -> {
                var isFanLoopCounterEnabled = false
                if (previousFanLoopVal > 0 && fanLoopCounter > 0) {
                    isFanLoopCounterEnabled = true
                }
                // In order to protect the fan, persist the fan for few cycles when there is a sudden change in
                // occupancy and decrease in fan loop output
                var currentStatus = result as Boolean
                if (!currentStatus && isFanLoopCounterEnabled) {
                    currentStatus = true
                }
                updateStatus(equip.fanEnable, currentStatus, StatusMsgKeys.FAN_ENABLED.name)
            }

            ControllerNames.OCCUPIED_ENABLED -> updateStatus(equip.occupiedEnable, result, StatusMsgKeys.EQUIP_ON.name)
            ControllerNames.HUMIDIFIER_CONTROLLER -> updateStatus(equip.humidifierEnable, result)
            ControllerNames.DEHUMIDIFIER_CONTROLLER -> updateStatus(
                equip.dehumidifierEnable, result
            )

            ControllerNames.DAMPER_RELAY_CONTROLLER -> updateStatus(
                equip.dcvDamper, result, StatusMsgKeys.DCV_DAMPER.name
            )

            else -> {
                logIt("Unknown controller: $controllerName")
            }
        }
    }


    private fun runAnalogOutOperations(
        config: Pipe4UVConfiguration,
        basicSettings: BasicSettings,
        tuners: UvTuners,
        analogOutStages: HashMap<String, Int>
    ) {
        val analogOuts = config.getAnalogOutsConfigurationMapping()
        analogOuts.forEach { (enabled, association, port) ->
            if (enabled) {
                if (hssEquip.isCondensateTripped()) {
                    resetAnalogOutLogicalPoints()
                    return
                }
                when (Pipe4UvAnalogOutControls.values()[association]) {
                    Pipe4UvAnalogOutControls.HEATING_WATER_MODULATING_VALVE -> {
                        val modulationValue =
                            if (controlVia == ControlVia.FACE_AND_BYPASS_DAMPER) 0 else heatingLoopOutput
                        doAnalogOperation(
                            canWeDoHeating(basicSettings.conditioningMode),
                            analogOutStages,
                            StatusMsgKeys.HEATING.name,
                            modulationValue,
                            hssEquip.hotWaterModulatingHeatValve
                        )
                    }

                    Pipe4UvAnalogOutControls.COOLING_WATER_MODULATING_VALVE -> {
                        val modulationValue = if ((controlVia == ControlVia.FACE_AND_BYPASS_DAMPER)
                            || economizingAvailable && coolingLoopOutput <= tuners.economizingToMainCoolingLoopMap) 0 else coolingLoopOutput
                        doAnalogOperation(
                            canWeDoCooling(basicSettings.conditioningMode),
                            analogOutStages,
                            StatusMsgKeys.COOLING_VALVE.name,
                            modulationValue,
                            hssEquip.chilledWaterModulatingCoolValve
                        )
                    }

                    Pipe4UvAnalogOutControls.FACE_DAMPER_VALVE -> {
                        val loop = if (canWeDoCooling(basicSettings.conditioningMode) && coolingLoopOutput > 0 && controlVia == ControlVia.FACE_AND_BYPASS_DAMPER) {
                            if ((economizingAvailable && coolingLoopOutput <= tuners.economizingToMainCoolingLoopMap)) 0 else coolingLoopOutput
                        } else if (canWeDoHeating(basicSettings.conditioningMode) && heatingLoopOutput > 0 && controlVia == ControlVia.FACE_AND_BYPASS_DAMPER) {
                            heatingLoopOutput
                        } else 0
                        if (fanLoopCounter > 0 && loop == 0) {
                            logIt("Fan Loop Counter is active, Face Bypass Damper Modulation holding with previous status")
                            return
                        }
                        doAnalogOperation(
                            canWeOperate(basicSettings),
                            analogOutStages,
                            null,
                            loop,
                            hssEquip.faceBypassDamperModulatingCmd
                        )
                    }

                    Pipe4UvAnalogOutControls.FAN_SPEED -> {
                        val voltage = config.getFanConfiguration(port)
                        val fanLowPercent = voltage.fanAtLow.currentVal.toInt()
                        val fanMediumPercent = voltage.fanAtMedium.currentVal.toInt()
                        val fanHighPercent = voltage.fanAtHigh.currentVal.toInt()
                        doAnalogFanAction(
                            fanLowPercent,
                            fanMediumPercent,
                            fanHighPercent,
                            basicSettings,
                            fanLoopOutput,
                            analogOutStages,
                            previousFanLoopVal,
                            fanLoopCounter,
                            hssEquip,
                            hssEquip.fanSignal
                        )
                    }

                    Pipe4UvAnalogOutControls.OAO_DAMPER -> {
                        hssEquip.oaoDamper.writeHisVal(outsideAirFinalLoopOutput.toDouble())
                        if (outsideAirFinalLoopOutput > 0) {
                            analogOutStages[StatusMsgKeys.OAO_DAMPER.name] =
                                outsideAirFinalLoopOutput
                        }
                    }

                    Pipe4UvAnalogOutControls.DCV_MODULATING_DAMPER -> {
                        doDcvAnalogAction(analogOutStages = analogOutStages, equip = hssEquip)
                    }

                    Pipe4UvAnalogOutControls.EXTERNALLY_MAPPED -> {}
                }
            }
        }
    }

    private fun logResults(config: Pipe4UVConfiguration) {
        val mappings = config.getRelayConfigurationMapping()
        mappings.forEach { (enabled, association, port) ->
            if (enabled) {
                val mapping = Pipe4UVRelayControls.values()[association]
                val logicalPoint = when (mapping) {
                    Pipe4UVRelayControls.FAN_LOW_SPEED_VENTILATION -> hssEquip.fanLowSpeedVentilation
                    Pipe4UVRelayControls.FAN_LOW_SPEED -> hssEquip.fanLowSpeed
                    Pipe4UVRelayControls.FAN_MEDIUM_SPEED -> hssEquip.fanMediumSpeed
                    Pipe4UVRelayControls.FAN_HIGH_SPEED -> hssEquip.fanHighSpeed
                    Pipe4UVRelayControls.COOLING_WATER_VALVE -> hssEquip.chilledWaterCoolValve
                    Pipe4UVRelayControls.HEATING_WATER_VALVE -> hssEquip.hotWaterHeatValve
                    Pipe4UVRelayControls.AUX_HEATING_STAGE1 -> hssEquip.auxHeatingStage1
                    Pipe4UVRelayControls.AUX_HEATING_STAGE2 -> hssEquip.auxHeatingStage2
                    Pipe4UVRelayControls.FAN_ENABLED -> hssEquip.fanEnable
                    Pipe4UVRelayControls.OCCUPIED_ENABLED -> hssEquip.occupiedEnable
                    Pipe4UVRelayControls.FACE_BYPASS_DAMPER -> hssEquip.faceBypassDamperCmd
                    Pipe4UVRelayControls.DCV_DAMPER -> hssEquip.dcvDamper
                    Pipe4UVRelayControls.HUMIDIFIER -> hssEquip.humidifierEnable
                    Pipe4UVRelayControls.DEHUMIDIFIER -> hssEquip.dehumidifierEnable
                    Pipe4UVRelayControls.EXTERNALLY_MAPPED -> null
                }
                if (logicalPoint != null) {
                    logIt("$port = $mapping ${logicalPoint.readHisVal()}")
                }
            }
        }

        config.getAnalogOutsConfigurationMapping().forEach {
            val (enabled, association, port) = it
            if (enabled) {
                val mapping = Pipe4UvAnalogOutControls.values()[association]
                val modulation = when (mapping) {
                    Pipe4UvAnalogOutControls.HEATING_WATER_MODULATING_VALVE -> hssEquip.hotWaterModulatingHeatValve
                    Pipe4UvAnalogOutControls.COOLING_WATER_MODULATING_VALVE -> hssEquip.chilledWaterModulatingCoolValve
                    Pipe4UvAnalogOutControls.FACE_DAMPER_VALVE -> hssEquip.faceBypassDamperModulatingCmd
                    Pipe4UvAnalogOutControls.FAN_SPEED -> hssEquip.fanSignal
                    Pipe4UvAnalogOutControls.OAO_DAMPER -> hssEquip.oaoDamper
                    Pipe4UvAnalogOutControls.DCV_MODULATING_DAMPER -> hssEquip.dcvDamperModulating
                    Pipe4UvAnalogOutControls.EXTERNALLY_MAPPED -> null
                }
                if (modulation != null) {
                    logIt("$port = ${mapping.name}  analogSignal  ${modulation.readHisVal()}")
                }
            }
        }
    }

    private fun runSpecifiedAnalogFanSpeed(auxType: AuxActiveStages?, config: Pipe4UVConfiguration) {
        if (auxType != null) {
            val analogOuts = config.getAnalogOutsConfigurationMapping()
            analogOuts.forEach { (enabled, association, port) ->
                if (enabled && Pipe4UvAnalogOutControls.values()[association] == Pipe4UvAnalogOutControls.FAN_SPEED) {
                    val voltage = config.getFanConfiguration(port)
                    val fanSignal = when (auxType) {
                        AuxActiveStages.AUX2, AuxActiveStages.BOTH -> voltage.fanAtHigh.currentVal.toInt()
                        AuxActiveStages.AUX1 -> voltage.fanAtMedium.currentVal.toInt()
                        else -> fanLoopOutput
                    }
                    if (fanSignal > 0) hssEquip.analogOutStages[StatusMsgKeys.FAN_SPEED.name] =
                        fanSignal
                    hssEquip.fanSignal.writeHisVal(fanSignal.toDouble())
                }
            }
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
            economizingLoopOutput,
            dcvLoopOutput,
            getEffectiveOutsideDamperMinOpen(hssEquip, isHeatingActive(), isCoolingActive()),
            outsideAirFinalLoopOutput,
            if (hssEquip.isCondensateTripped()) 1.0 else 0.0,
            if (hssEquip.filterStatusNC.readHisVal() > 0.0 || hssEquip.filterStatusNO.readHisVal() > 0.0) 1.0 else 0.0,
            fetchBasicSettings(hssEquip),
            EpidemicState.OFF,
            isEmergencyShutoffActive(hssEquip),
            tag
        )
    }

    override fun resetRelayLogicalPoints() {
        hssEquip.apply {
            listOf(
                fanLowSpeedVentilation,
                fanLowSpeed,
                fanMediumSpeed,
                fanHighSpeed,
                chilledWaterCoolValve,
                hotWaterHeatValve,
                auxHeatingStage1,
                auxHeatingStage2,
                fanEnable,
                occupiedEnable,
                faceBypassDamperCmd,
                dcvDamper,
                humidifierEnable,
                dehumidifierEnable
            ).forEach { resetPoint(it) }
            hssEquip.relayStages.remove(StatusMsgKeys.FAN_ENABLED.name)
        }
    }

    override fun resetAnalogOutLogicalPoints() {
        hssEquip.apply {
            listOf(
                hotWaterModulatingHeatValve,
                chilledWaterModulatingCoolValve,
                faceBypassDamperModulatingCmd,
                fanSignal,
                oaoDamper,
                dcvDamperModulating
            ).forEach { resetPoint(it) }
        }
    }

    fun isCoolingActive() = coolingLoopOutput > 0

    fun isHeatingActive() = heatingLoopOutput > 0

    override fun getCurrentTemp() = hssEquip.currentTemp.readHisVal()

    override fun getAverageZoneTemp() = getCurrentTemp()

    override fun getProfileType() = ProfileType.HYPERSTATSPLIT_4PIPE_UV

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short) =
        BaseProfileConfiguration() as T
}