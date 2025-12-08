package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.hyperstatsplit.Pipe2UVEquip
import a75f.io.domain.util.CalibratedPoint
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
import a75f.io.logic.bo.building.statprofiles.statcontrollers.SplitControllerFactory
import a75f.io.logic.bo.building.statprofiles.util.AuxActiveStages
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings
import a75f.io.logic.bo.building.statprofiles.util.ControlVia
import a75f.io.logic.bo.building.statprofiles.util.FAN_HIGHEST_STAGE
import a75f.io.logic.bo.building.statprofiles.util.FAN_LOWEST_STAGE
import a75f.io.logic.bo.building.statprofiles.util.FAN_MEDIUM_STAGE
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
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
import a75f.io.logic.bo.building.statprofiles.util.isSupplyOppositeToConditioning
import a75f.io.logic.bo.building.statprofiles.util.milliToMin
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.handlers.doAnalogOperation
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.util.uiutils.HyperStatSplitUserIntentHandler

/**
 * Author: Manjunath Kundaragi
 * Created on: 07-08-2025
 */
class Pipe2UnitVentilatorProfile(private val equipRef: String, nodeAddress: Short) :
    UnitVentilatorProfile(equipRef, nodeAddress, L.TAG_CCU_HSSPLIT_PIPE2_UV) {
    lateinit var hssEquip: Pipe2UVEquip
    private var waterValveLoop = CalibratedPoint(DomainName.waterValve, equipRef,0.0)
    private var supplyWaterTemp = 0.0
    private var heatingThreshold = 85.0
    private var coolingThreshold = 65.0
    private var isWaterValveActiveDueToLoop = false
    private var waterSamplingStartTime: Long = 0


    override fun updateZonePoints() {
        hssEquip = Domain.getEquip(equipRef) as Pipe2UVEquip

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
            L.TAG_CCU_HSSPLIT_PIPE2_UV,
            controllers,
            stageCounts,
            isEconAvailable,
            derivedFanLoopOutput,
            zoneOccupancyState
        )

        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode
        controlVia = ControlVia.values()[hssEquip.controlVia.readDefaultVal().toInt()]
        val config = getSplitConfiguration(equipRef) as Pipe2UVConfiguration
        resetEquip(hssEquip)
        supplyWaterTemp = hssEquip.leavingWaterTemperature.readHisVal()
        val pipe2Tuners = getUnitVentilatorTuners(hssEquip)
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
        isWaterValveActiveDueToLoop = false
        val basicSettings = fetchBasicSettings(hssEquip)
        val updatedFanMode = fallBackFanMode(hssEquip, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        heatingThreshold = pipe2Tuners.heatingThreshold
        coolingThreshold = pipe2Tuners.coolingThreshold
        loopController.initialise(tuners = pipe2Tuners)
        loopController.dumpLogs()
        handleChangeOfDirection(userIntents, controllerFactory, hssEquip)
        calculateSaTemperingLoop(pipe2Tuners, hssEquip, basicSettings)
        checkDoorWindowSensorStatus(hssEquip)
        keyCardIsInSlot(hssEquip)
        prePurgeEnabled = hssEquip.prePurgeEnable.readDefaultVal() > 0.0
        prePurgeOpeningValue = hssEquip.standalonePrePurgeFanSpeedTuner.readPriorityVal()

        resetLoopOutputs()
        evaluateLoopOutputs(userIntents, basicSettings, pipe2Tuners, hssEquip)
        waterValveLoop.data = getWaterValveLoop(userIntents).toDouble()
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
        updateTitle24LoopCounter(pipe2Tuners, basicSettings)
        updateOccupancyDetection(hssEquip)
        updateLoopOutputs(hssEquip)

        if (equipOccupancyHandler != null) {
            occupancyStatus = equipOccupancyHandler.currentOccupiedMode
            zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        }

        if (isEmergencyShutoffActive(hssEquip).not() && (isDoorOpen.not()) && isCondensateTripped.not()) {
            if (canWeDoConditioning(basicSettings) && canWeRunFan(basicSettings)) {
                runRelayOperations(config, basicSettings, pipe2Tuners)
                runAnalogOutOperations(config, basicSettings, pipe2Tuners, hssEquip.analogOutStages)
                runAlgorithm(hssEquip, basicSettings, config)
                processForWaterSampling(hssEquip, pipe2Tuners, basicSettings)
                runSpecifiedAnalogFanSpeed(operateAuxBasedFan(hssEquip, basicSettings), config)
                if (supplyWaterTemp > heatingThreshold) {
                    operateSaTempering(hssEquip, pipe2Tuners, basicSettings)
                }
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
            "Economizing Loop Output:: $economizingLoopOutput \n" +
                    "DCV Loop Output:: $dcvLoopOutput \n" +
                    "Calculated Min OAO Damper:: $outsideAirCalculatedMinDamper \n" +
                    "OAO Loop Output (before MAT Safety):: $outsideAirLoopOutput \n" +
                    "OAO Loop Output (after MAT Safety and outsideDamperMinOpen):: $outsideAirFinalLoopOutput \n" +
                    "conditioningMode: ${basicSettings.conditioningMode} fan mode ${basicSettings.fanMode}\n" +
                    "heatingThreshold : $heatingThreshold" + " coolingThreshold : $coolingThreshold  supplyWaterTemp: $supplyWaterTemp\n" +
                    "Desired ${userIntents.heatingDesiredTemp} ${userIntents.coolingDesiredTemp} Current Temp $currentTemp \n" +
                    "Loop (Heating: $heatingLoopOutput Cooling: $coolingLoopOutput Fan: $fanLoopOutput )\n" +
                    "SATempering Loop Output: $saTemperingLoopOutput | waterValveLoop ${waterValveLoop.data}\n" +
                    "Econ Active: $economizingAvailable | isCondensateTripped: $isCondensateTripped "
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
        config: Pipe2UVConfiguration, basicSettings: BasicSettings, tuners: UvTuners) {
        updatePrerequisite(config)
        runControllers(hssEquip, basicSettings, tuners)
    }

    private fun updatePrerequisite(config: Pipe2UVConfiguration) {
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
        controllerFactory.addPipe2Controllers(config, ::isPrePurgeActive , fanLowVentilationAvailable, waterValveLoop)


        logIt(
            " isEconAvailable: ${isEconAvailable.data} " + "zoneOccupancyState : ${zoneOccupancyState.data}\n" +
                    "derivedFanLoopOutput: ${derivedFanLoopOutput.data} waterValveLoop $waterValveLoop"
        )
    }

    private fun getWaterValveLoop(userIntents: UserIntents): Int {
        val isHeatingAvailable = supplyWaterTemp > heatingThreshold
        val isCoolingAvailable = supplyWaterTemp < coolingThreshold

        val zoneNeedsHeating = currentTemp < userIntents.heatingDesiredTemp
        val zoneNeedsCooling = currentTemp > userIntents.coolingDesiredTemp

        return when {
            isHeatingAvailable && zoneNeedsHeating -> heatingLoopOutput
            isCoolingAvailable && zoneNeedsCooling -> coolingLoopOutput

            // No zone demand, but supply is still available
            isHeatingAvailable -> heatingLoopOutput.takeIf { it > 0 } ?: 0
            isCoolingAvailable -> coolingLoopOutput.takeIf { it > 0 } ?: 0

            else -> 0 // No demand, no supply
        }
    }

    private fun runControllers(
        equip: Pipe2UVEquip,
        basicSettings: BasicSettings,
        tuners: UvTuners
    ) {
        controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            val result = controller.runController()
            updateRelayStatus(controllerName, result, equip, basicSettings, tuners)
        }
    }

    private fun runningAtHeatingDirection() = (heatingLoopOutput > 0 && supplyWaterTemp > coolingThreshold)
    private fun runningAtCoolingDirection() = (coolingLoopOutput > 0 && supplyWaterTemp < coolingThreshold)

    private fun updateRelayStatus(
        controllerName: String,
        result: Any,
        equip: Pipe2UVEquip,
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

            ControllerNames.WATER_VALVE_CONTROLLER -> {
                if (waterSamplingStartTime == 0L) {
                    var status = result as Boolean
                    if (coolingLoopOutput > 0 && (canWeDoCooling(basicSettings.conditioningMode).not() ||
                                (economizingAvailable && coolingLoopOutput <= tuners.economizingToMainCoolingLoopMap))
                    ) {
                        status = false
                    }
                    if (heatingLoopOutput > 0 && canWeDoHeating(basicSettings.conditioningMode).not()) {
                        status = false
                    }
                    updateStatus(equip.waterValve, status, StatusMsgKeys.WATER_VALVE.name)
                    if (status) {
                        isWaterValveActiveDueToLoop = true
                        lastWaterValveTurnedOnTime = System.currentTimeMillis()
                    }

                }
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

                if ((coolingLoopOutput > 0 && (canWeDoCooling(basicSettings.conditioningMode).not() || runningAtCoolingDirection().not()
                            || (economizingAvailable && coolingLoopOutput <= tuners.economizingToMainCoolingLoopMap)))
                    || (heatingLoopOutput > 0 &&( runningAtHeatingDirection().not() || canWeDoHeating(basicSettings.conditioningMode).not()))
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
        config: Pipe2UVConfiguration,
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
                when (Pipe2UvAnalogOutControls.values()[association]) {

                    Pipe2UvAnalogOutControls.FACE_DAMPER_VALVE -> {
                        val loop = if (canWeDoCooling(basicSettings.conditioningMode) && runningAtCoolingDirection() && controlVia == ControlVia.FACE_AND_BYPASS_DAMPER) {
                            if ((economizingAvailable && coolingLoopOutput <= tuners.economizingToMainCoolingLoopMap)) 0 else coolingLoopOutput
                        } else if (canWeDoHeating(basicSettings.conditioningMode) && runningAtHeatingDirection() && controlVia == ControlVia.FACE_AND_BYPASS_DAMPER) {
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

                    Pipe2UvAnalogOutControls.FAN_SPEED -> {
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

                    Pipe2UvAnalogOutControls.OAO_DAMPER -> {
                        hssEquip.oaoDamper.writeHisVal(outsideAirFinalLoopOutput.toDouble())
                        if (outsideAirFinalLoopOutput > 0) {
                            analogOutStages[StatusMsgKeys.OAO_DAMPER.name] =
                                outsideAirFinalLoopOutput
                        }
                    }

                    Pipe2UvAnalogOutControls.DCV_MODULATING_DAMPER -> {
                        doDcvAnalogAction(analogOutStages = analogOutStages, equip = hssEquip)
                    }


                    Pipe2UvAnalogOutControls.WATER_MODULATING_VALVE -> {
                        if (waterSamplingStartTime == 0L && canWeDoConditioning(basicSettings)
                            && isSupplyOppositeToConditioning(
                                basicSettings.conditioningMode, supplyWaterTemp,
                                heatingThreshold, coolingThreshold
                            ).not()
                        ) {
                            if (canWeDoConditioning(basicSettings) && canWeRunFan(basicSettings)) {
                                var modulationValue = waterValveLoop.data
                                if (controlVia != ControlVia.FULLY_MODULATING_VALVE ||
                                    (coolingLoopOutput > 0 && canWeDoCooling(basicSettings.conditioningMode).not()) ||
                                    (economizingAvailable && coolingLoopOutput <= tuners.economizingToMainCoolingLoopMap)
                                ) {
                                    modulationValue = 0.0
                                }
                                if (heatingLoopOutput > 0 && canWeDoHeating(basicSettings.conditioningMode).not()) {
                                    modulationValue = 0.0
                                }

                                hssEquip.modulatingWaterValve.writeHisVal(modulationValue)
                                if (modulationValue > 0) {
                                    hssEquip.analogOutStages[StatusMsgKeys.WATER_VALVE.name] = 1
                                    isWaterValveActiveDueToLoop = true
                                    lastWaterValveTurnedOnTime = System.currentTimeMillis()
                                }
                            } else {
                                hssEquip.modulatingWaterValve.writeHisVal(0.0)
                                hssEquip.analogOutStages.remove(StatusMsgKeys.WATER_VALVE.name)
                            }
                        }
                    }
                    Pipe2UvAnalogOutControls.EXTERNALLY_MAPPED -> {}
                }
            }
        }
    }

    private fun runAlgorithm(
        equip: Pipe2UVEquip,
        basicSettings: BasicSettings,
        config: Pipe2UVConfiguration
    ) {
        if ((currentTemp > 0) && canWeRunFan(basicSettings)) {
            if (canWeDoConditioning(basicSettings)) {
                if (isSupplyOppositeToConditioning(
                        basicSettings.conditioningMode,
                        supplyWaterTemp,
                        heatingThreshold,
                        coolingThreshold
                    )
                ) {
                    resetWaterValve(equip)
                }
                val analogFanType = operateAuxBasedFan(hssEquip, basicSettings)
                runSpecifiedAnalogFanSpeed(analogFanType, config)
            } else {
                logIt("Conditioning mode is OFF")
                resetAllLogicalPointValues()
            }
        } else {
            resetAllLogicalPointValues()
        }
    }

    private fun logResults(config: Pipe2UVConfiguration) {
        val mappings = config.getRelayConfigurationMapping()
        mappings.forEach { (enabled, association, port) ->
            if (enabled) {
                val mapping = Pipe2UVRelayControls.values()[association]
                val logicalPoint = when (mapping) {
                    Pipe2UVRelayControls.FAN_LOW_SPEED_VENTILATION -> hssEquip.fanLowSpeedVentilation
                    Pipe2UVRelayControls.FAN_LOW_SPEED -> hssEquip.fanLowSpeed
                    Pipe2UVRelayControls.FAN_MEDIUM_SPEED -> hssEquip.fanMediumSpeed
                    Pipe2UVRelayControls.FAN_HIGH_SPEED -> hssEquip.fanHighSpeed
                    Pipe2UVRelayControls.AUX_HEATING_STAGE1 -> hssEquip.auxHeatingStage1
                    Pipe2UVRelayControls.AUX_HEATING_STAGE2 -> hssEquip.auxHeatingStage2
                    Pipe2UVRelayControls.WATER_VALVE -> hssEquip.waterValve
                    Pipe2UVRelayControls.FAN_ENABLED -> hssEquip.fanEnable
                    Pipe2UVRelayControls.OCCUPIED_ENABLED -> hssEquip.occupiedEnable
                    Pipe2UVRelayControls.FACE_BYPASS_DAMPER -> hssEquip.faceBypassDamperCmd
                    Pipe2UVRelayControls.DCV_DAMPER -> hssEquip.dcvDamper
                    Pipe2UVRelayControls.HUMIDIFIER -> hssEquip.humidifierEnable
                    Pipe2UVRelayControls.DEHUMIDIFIER -> hssEquip.dehumidifierEnable
                    Pipe2UVRelayControls.EXTERNALLY_MAPPED -> null
                }
                if (logicalPoint != null) {
                    logIt("$port = $mapping ${logicalPoint.readHisVal()}")
                }
            }
        }

        config.getAnalogOutsConfigurationMapping().forEach {
            val (enabled, association, port) = it
            if (enabled) {
                val mapping = Pipe2UvAnalogOutControls.values()[association]
                val modulation = when (mapping) {
                    Pipe2UvAnalogOutControls.WATER_MODULATING_VALVE -> hssEquip.modulatingWaterValve
                    Pipe2UvAnalogOutControls.FACE_DAMPER_VALVE -> hssEquip.faceBypassDamperModulatingCmd
                    Pipe2UvAnalogOutControls.FAN_SPEED -> hssEquip.fanSignal
                    Pipe2UvAnalogOutControls.OAO_DAMPER -> hssEquip.oaoDamper
                    Pipe2UvAnalogOutControls.DCV_MODULATING_DAMPER -> hssEquip.dcvDamperModulating
                    Pipe2UvAnalogOutControls.EXTERNALLY_MAPPED -> null
                }
                if (modulation != null) {
                    logIt("$port = ${mapping.name}  analogSignal  ${modulation.readHisVal()}")
                }
            }
        }
    }

    private fun runSpecifiedAnalogFanSpeed(auxType: AuxActiveStages?, config: Pipe2UVConfiguration) {
        if (auxType != null) {
            val analogOuts = config.getAnalogOutsConfigurationMapping()
            analogOuts.forEach { (enabled, association, port) ->
                if (enabled && Pipe2UvAnalogOutControls.values()[association] == Pipe2UvAnalogOutControls.FAN_SPEED) {
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
            if (isPointExist(hssEquip.faceBypassDamperCmd)) {
                hssEquip.faceBypassDamperCmd.writeHisVal(1.0)
            }
            if(isPointExist(hssEquip.faceBypassDamperModulatingCmd)) {
                hssEquip.faceBypassDamperModulatingCmd.writeHisVal(heatingLoopOutput.toDouble())
            }
        }
    }

    private fun processForWaterSampling(
        equip: Pipe2UVEquip, tuner: UvTuners,
        basicSettings: BasicSettings
    ) {
        if (isWaterValveActiveDueToLoop) {
            logIt("Sampling not required, because water valve is active due to loop")
            return
        }

        if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
            resetWaterValve(equip)
            return
        }

        if (equip.waterValve.pointExists().not() && equip.modulatingWaterValve.pointExists().not()
        ) {
            logIt( "No mapping for water value")
            return
        }

        fun resetIsRequired(): Boolean {
            return (hssEquip.waterValve.readHisVal().toInt() != 0
                    || hssEquip.modulatingWaterValve.readHisVal().toInt() != 0)
        }

        logIt("waterSamplingStarted Time $waterSamplingStartTime")

        val waitTimeToDoSampling: Int
        val onTimeToDoSampling: Int
        if (supplyWaterTemp in coolingThreshold..heatingThreshold) {
            waitTimeToDoSampling = tuner.waterValveSamplingDuringLoopDeadbandWaitTime
            onTimeToDoSampling = tuner.waterValveSamplingDuringLoopDeadbandOnTime
        } else {
            waitTimeToDoSampling = tuner.waterValveSamplingWaitTime
            onTimeToDoSampling = tuner.waterValveSamplingOnTime
        }

        if (waitTimeToDoSampling == 0 || onTimeToDoSampling == 0) {
            //resetting the water valve value value only when the tuner value is zero
            if (resetIsRequired()) {
                waterSamplingStartTime = 0
                lastWaterValveTurnedOnTime = System.currentTimeMillis()
                resetWaterValve(equip)
            }
            logIt( "No water sampling, because tuner value is zero!")
            return
        }
        logIt("waitTimeToDoSampling:  $waitTimeToDoSampling onTimeToDoSampling: $onTimeToDoSampling\n"+
        "Current : ${System.currentTimeMillis()}: Last On: $lastWaterValveTurnedOnTime")

        if (waterSamplingStartTime == 0L) {
            val minutes = milliToMin(System.currentTimeMillis() - lastWaterValveTurnedOnTime)
            logIt("sampling will start in : ${waitTimeToDoSampling - minutes} current : $minutes")
            if (minutes >= waitTimeToDoSampling) {
                doWaterSampling(hssEquip.relayStages)
            }
        } else {
            val samplingSinceFrom =
                milliToMin(System.currentTimeMillis() - waterSamplingStartTime)
            logIt("Water sampling is running since from $samplingSinceFrom minutes")
            if (samplingSinceFrom >= onTimeToDoSampling) {
                waterSamplingStartTime = 0
                lastWaterValveTurnedOnTime = System.currentTimeMillis()
                resetWaterValve(equip)
                logIt( "Resetting WATER_VALVE to OFF")
            } else {
                hssEquip.relayStages[StatusMsgKeys.WATER_VALVE.name] = 1
            }
        }
    }

    private fun doWaterSampling(
        relayStages: HashMap<String, Int>,
    ) {
        waterSamplingStartTime = System.currentTimeMillis()
        hssEquip.waterValve.writeHisVal(1.0)
        hssEquip.modulatingWaterValve.writeHisVal(100.0)
        relayStages[StatusMsgKeys.WATER_VALVE.name] = 1
        logIt( "Turned ON water valve ")
    }

    private fun resetWaterValve(equip: Pipe2UVEquip) {
        if (waterSamplingStartTime == 0L) {
            hssEquip.waterValve.writeHisVal(0.0)
            hssEquip.modulatingWaterValve.writeHisVal(0.0)
            equip.relayStages.remove(StatusMsgKeys.WATER_VALVE.name)
            equip.analogOutStages.remove(StatusMsgKeys.WATER_VALVE.name)
            isWaterValveActiveDueToLoop = false
            logIt( "Resetting WATER_VALVE to OFF")
        }
    }
    fun supplyDirection(): String {
        return if (supplyWaterTemp > heatingThreshold
            || supplyWaterTemp in coolingThreshold..heatingThreshold
        ) {
            "Heating"
        } else {
            "Cooling"
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
                waterValve,
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
                modulatingWaterValve,
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

    override fun getProfileType() = ProfileType.HYPERSTATSPLIT_2PIPE_UV

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short) =
        BaseProfileConfiguration() as T

}