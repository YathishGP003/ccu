package a75f.io.logic.bo.building.hyperstat.profiles.pipe2

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.hyperstat.Pipe2V2Equip
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.common.BasicSettings
import a75f.io.logic.bo.building.hyperstat.common.FanModeCacheStorage
import a75f.io.logic.bo.building.hyperstat.common.HyperStatLoopController
import a75f.io.logic.bo.building.hyperstat.common.HyperStatProfileTuners
import a75f.io.logic.bo.building.hyperstat.common.UserIntents
import a75f.io.logic.bo.building.hyperstat.profiles.HyperStatFanCoilUnit
import a75f.io.logic.bo.building.hyperstat.profiles.util.fetchBasicSettings
import a75f.io.logic.bo.building.hyperstat.profiles.util.fetchHyperStatTuners
import a75f.io.logic.bo.building.hyperstat.profiles.util.fetchUserIntents
import a75f.io.logic.bo.building.hyperstat.profiles.util.getConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.util.getLogicalPointList
import a75f.io.logic.bo.building.hyperstat.profiles.util.updateAllLoopOutput
import a75f.io.logic.bo.building.hyperstat.profiles.util.updateOccupancyDetection
import a75f.io.logic.bo.building.hyperstat.profiles.util.updateOperatingMode
import a75f.io.logic.bo.building.hyperstat.v2.configs.AnalogInputAssociation
import a75f.io.logic.bo.building.hyperstat.v2.configs.FanConfig
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsPipe2AnalogOutMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsPipe2RelayMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.Pipe2AnalogOutConfigs
import a75f.io.logic.bo.building.hyperstat.v2.configs.Pipe2Configuration
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.util.uiutils.HyperStatUserIntentHandler


/**
 * Created by Manjunath K on 01-08-2022.
 */


class HyperStatPipe2Profile : HyperStatFanCoilUnit() {

    private var coolingLoopOutput = 0
    private var heatingLoopOutput = 0
    private var fanLoopOutput = 0
    private var supplyWaterTempTh2 = 0.0
    private var heatingThreshold = 85.0
    private var coolingThreshold = 65.0
    
    
    private var doorWindowSensorOpenStatus = false
    private var runFanLowDuringDoorWindow = false
    
    private val pipe2DeviceMap: MutableMap<Int, Pipe2V2Equip> = mutableMapOf()
    private var occupancyBeforeDoorWindow = Occupancy.UNOCCUPIED
    
    private val hyperStatLoopController = HyperStatLoopController()
    override lateinit var occupancyStatus: Occupancy
    private lateinit var curState: ZoneState
    
    private var analogLogicalPoints: HashMap<Int, String> = HashMap()
    private var relayLogicalPoints: HashMap<Int, String> = HashMap()

    override fun getProfileType() = ProfileType.HYPERSTAT_TWO_PIPE_FCU    


    override fun updateZonePoints() {
        pipe2DeviceMap.forEach { (nodeAddress, equip) ->
            pipe2DeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as Pipe2V2Equip
            CcuLog.d( L.TAG_CCU_HSPIPE2,"Process Pipe2: equipRef =  ${equip.nodeAddress}")
            processHyperStatPipeProfile(equip)
        }
    }
    
    fun addEquip(equipRef: String) {
        val equip = Pipe2V2Equip(equipRef)
        pipe2DeviceMap[equip.nodeAddress] = equip
    }
    
    override fun getEquip(): Equip? {
        for (nodeAddress in pipe2DeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    fun processHyperStatPipeProfile(equip: Pipe2V2Equip) {

        if (Globals.getInstance().isTestMode) {
            CcuLog.d(L.TAG_CCU_HSPIPE2, "Test mode is on: ${equip.equipRef}")
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

        val relayOutputStatus = HashMap<String, Int>()
        val analogOutputStatus = HashMap<String, Int>()

        val config = getConfiguration(equip.equipRef)

        logicalPointsList = getLogicalPointList(equip, config!!)

        relayLogicalPoints = getRelayOutputPoints(equip)
        analogLogicalPoints = getAnalogOutputPoints(equip)

        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode

        val hyperStatTuners = fetchHyperStatTuners(equip)
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val fanModeSaved = FanModeCacheStorage().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchBasicSettings(equip)

        CcuLog.d(L.TAG_CCU_HSPIPE2, "Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        CcuLog.d(L.TAG_CCU_HSPIPE2, "After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")

        heatingThreshold = hyperStatTuners.heatingThreshold
        coolingThreshold = hyperStatTuners.coolingThreshold

        hyperStatLoopController.initialise(tuners = hyperStatTuners)
        hyperStatLoopController.dumpLogs()
        handleChangeOfDirection(userIntents)

        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0

        val currentOperatingMode = equip.occupancyMode.readHisVal().toInt()
        evaluateLoopOutputs(userIntents, basicSettings, hyperStatTuners)
        updateOccupancyDetection(equip)

        doorWindowSensorOpenStatus = runForDoorWindowSensor(config, equip, analogOutputStatus, relayOutputStatus)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)
        supplyWaterTempTh2 = equip.leavingWaterTemperature.readHisVal()
        if (occupancyStatus == Occupancy.WINDOW_OPEN) resetLoopOutputValues()

        runForKeyCardSensor(config, equip)
        updateAllLoopOutput(equip, coolingLoopOutput, heatingLoopOutput, fanLoopOutput, false)

        CcuLog.i(L.TAG_CCU_HSPIPE2,
                "Fan speed multiplier:  ${hyperStatTuners.analogFanSpeedMultiplier} " +
                        "AuxHeating1Activate: ${hyperStatTuners.auxHeating1Activate} " +
                        "AuxHeating2Activate: ${hyperStatTuners.auxHeating2Activate} " +
                        "waterValveSamplingOnTime: ${hyperStatTuners.waterValveSamplingOnTime}  waterValveSamplingWaitTime : ${hyperStatTuners.waterValveSamplingWaitTime} \n" +
                        "waterValveSamplingDuringLoopDeadbandOnTime: ${hyperStatTuners.waterValveSamplingDuringLoopDeadbandOnTime}  waterValveSamplingDuringLoopDeadbandWaitTime : ${hyperStatTuners.waterValveSamplingDuringLoopDeadbandWaitTime} \n" +
                        "Current Occupancy: ${Occupancy.values()[currentOperatingMode]} \n" +
                        "supplyWaterTempTh2 : $supplyWaterTempTh2 \n" +
                        "Fan Mode : ${basicSettings.fanMode} Conditioning Mode ${basicSettings.conditioningMode} \n" +
                        "heatingThreshold: $heatingThreshold  coolingThreshold : $coolingThreshold \n" +
                        "Current Temp : $currentTemp Desired (Heating: ${userIntents.zoneHeatingTargetTemperature}" +
                        " Cooling: ${userIntents.zoneCoolingTargetTemperature})\n" +
                        "Loop Outputs: (Heating: $heatingLoopOutput Cooling: $coolingLoopOutput Fan : $fanLoopOutput ) \n"
        )

        operateRelays(config as Pipe2Configuration, hyperStatTuners, userIntents, basicSettings, relayOutputStatus, relayLogicalPoints, equip, analogOutputStatus)
        operateAnalogOutputs(config, equip, basicSettings, analogOutputStatus, userIntents)
        runAlgorithm(equip, basicSettings, hyperStatTuners, relayOutputStatus, analogOutputStatus, config, userIntents)

        // Run the title 24 fan operation after the reset of all PI output is done
        doFanOperationTitle24(hyperStatTuners, basicSettings, relayOutputStatus, userIntents, analogOutputStatus, config)
        runForKeyCardSensor(config, equip)

        updateOperatingMode(currentTemp, averageDesiredTemp, basicSettings, equip)
        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())
        showOutputStatus()
        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY
        HyperStatUserIntentHandler.updateHyperStatStatus(equip.equipRef, relayOutputStatus, analogOutputStatus, temperatureState, equip)
        if (occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        CcuLog.i(L.TAG_CCU_HSPIPE2, "----------------------------------------------------------")
    }

    private fun evaluateLoopOutputs(userIntents: UserIntents, basicSettings: BasicSettings, hyperStatTuners: HyperStatProfileTuners) {
        when (state) {
            //Update coolingLoop when the zone is in cooling or it was in cooling and no change over happened yet.
            ZoneState.COOLING -> coolingLoopOutput =
                    hyperStatLoopController.calculateCoolingLoopOutput(
                            currentTemp, userIntents.zoneCoolingTargetTemperature).toInt().coerceAtLeast(0)

            //Update heatingLoop when the zone is in heating or it was in heating and no change over happened yet.
            ZoneState.HEATING -> heatingLoopOutput =
                    hyperStatLoopController.calculateHeatingLoopOutput(
                            userIntents.zoneHeatingTargetTemperature, currentTemp
                    ).toInt().coerceAtLeast(0)

            else -> CcuLog.d(L.TAG_CCU_HSPIPE2, " Zone is in deadband")
        }

        if (coolingLoopOutput > 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY
                        || basicSettings.conditioningMode == StandaloneConditioningMode.AUTO)) {
            fanLoopOutput = ((coolingLoopOutput * hyperStatTuners.analogFanSpeedMultiplier).coerceAtMost(100.0).toInt())
        } else if (heatingLoopOutput > 0 && ((basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY && supplyWaterTempTh2 > coolingThreshold)
                        || (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO && supplyWaterTempTh2 > coolingThreshold))
        ) {
            fanLoopOutput = (heatingLoopOutput * hyperStatTuners.analogFanSpeedMultiplier).coerceAtMost(100.0).toInt()
        }
    }

    private fun handleChangeOfDirection(userIntents: UserIntents) {
        if (currentTemp > userIntents.zoneCoolingTargetTemperature && state != ZoneState.COOLING) {
            hyperStatLoopController.resetCoolingControl()
            state = ZoneState.COOLING
            CcuLog.d(L.TAG_CCU_HSPIPE2, "Resetting cooling")
        } else if (currentTemp < userIntents.zoneHeatingTargetTemperature && state != ZoneState.HEATING) {
            hyperStatLoopController.resetHeatingControl()
            state = ZoneState.HEATING
            CcuLog.d(L.TAG_CCU_HSPIPE2, "Resetting heating")
        }
    }

    private fun runAlgorithm(
            equip: Pipe2V2Equip, basicSettings: BasicSettings, tuner: HyperStatProfileTuners, relayStages: HashMap<String, Int>,
            analogOutStages: HashMap<String, Int>, configuration: Pipe2Configuration, userIntents: UserIntents
    ) {
        // Run water sampling
         processForWaterSampling(equip, tuner, configuration, relayStages, analogOutStages, basicSettings)

        // any specific user intent run the fan operations
        if(basicSettings.fanMode != StandaloneFanStage.OFF && basicSettings.fanMode != StandaloneFanStage.AUTO) {
            doFanOperation(tuner, basicSettings, relayStages, configuration, userIntents, analogOutStages, equip)
        }

        if ((currentTemp > 0) && (basicSettings.fanMode != StandaloneFanStage.OFF)) {
            when (basicSettings.conditioningMode) {
                StandaloneConditioningMode.AUTO -> {
                    if (supplyWaterTempTh2 > heatingThreshold || supplyWaterTempTh2 in coolingThreshold..heatingThreshold)
                        doHeatOnly(tuner, basicSettings, relayStages, configuration,userIntents,analogOutStages,equip)
                    else if (supplyWaterTempTh2 < coolingThreshold)
                        doCoolOnly(tuner, basicSettings, relayStages, configuration,userIntents,analogOutStages,equip)
                }
                StandaloneConditioningMode.COOL_ONLY -> {
                    doCoolOnly(tuner, basicSettings, relayStages, configuration,userIntents,analogOutStages,equip)
                }
                StandaloneConditioningMode.HEAT_ONLY -> {
                    doHeatOnly(tuner, basicSettings, relayStages, configuration,userIntents,analogOutStages,equip)
                }
                StandaloneConditioningMode.OFF -> {
                    doFanOperation(tuner, basicSettings, relayStages, configuration,userIntents, analogOutStages, equip)
                }
            }
        } else {
            resetConditioning(relayStages,analogOutStages,basicSettings,equip)
        }
    }

    private fun doCoolOnly(
            tuner: HyperStatProfileTuners,
            basicSettings: BasicSettings,
            relayStages: HashMap<String, Int>,
            configuration: Pipe2Configuration,
            userIntents: UserIntents,
            analogOutStages: HashMap<String, Int>,
            equip: Pipe2V2Equip
    ) {
        CcuLog.d(L.TAG_CCU_HSPIPE2, "doCoolOnly: mode ")

        if (basicSettings.fanMode == StandaloneFanStage.OFF || supplyWaterTempTh2 > heatingThreshold) {
            CcuLog.d(L.TAG_CCU_HSPIPE2, "Resetting WATER_VALVE to OFF")
            resetWaterValue(relayStages, analogOutStages, equip)
        }

        if (basicSettings.fanMode != StandaloneFanStage.OFF) {
            if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal)
                && relayLogicalPoints.containsKey(HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal)
            ) {

                if (basicSettings.fanMode == StandaloneFanStage.AUTO) {
                    if (getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal]!!) == 1.0
                        && getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal]!!) == 0.0
                    ) {
                        resetFan(relayStages,analogOutStages,basicSettings)
                        operateAuxBasedOnFan(HsPipe2RelayMapping.AUX_HEATING_STAGE1,relayStages)
                        runSpecificAnalogFanSpeed(configuration,FanSpeed.MEDIUM,analogOutStages)
                        return
                    }
                    if (getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal]!!) == 1.0) {
                        resetFan(relayStages,analogOutStages,basicSettings)
                        operateAuxBasedOnFan(HsPipe2RelayMapping.AUX_HEATING_STAGE2,relayStages)
                        runSpecificAnalogFanSpeed(configuration,FanSpeed.HIGH,analogOutStages)
                    } else {
                        if(coolingLoopOutput > 0) {
                            if( supplyWaterTempTh2 > heatingThreshold
                                || supplyWaterTempTh2 in coolingThreshold..heatingThreshold){
                                resetFan(relayStages,analogOutStages,basicSettings)
                                return
                            }
                            doFanOperation(tuner, basicSettings, relayStages, configuration, userIntents, analogOutStages, equip)
                        }
                        if(heatingLoopOutput > 0 || fanLoopOutput == 0){
                            resetFan(relayStages,analogOutStages,basicSettings)
                        }
                    }
                } else {
                    // If Fan has specific user intent
                    doFanOperation(tuner, basicSettings, relayStages, configuration, userIntents, analogOutStages, equip)
                }
            }
            else if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal)
                && (getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal]!!) == 1.0)){
                resetFan(relayStages,analogOutStages,basicSettings)
                operateAuxBasedOnFan(HsPipe2RelayMapping.AUX_HEATING_STAGE1,relayStages)
                runSpecificAnalogFanSpeed(configuration,FanSpeed.MEDIUM,analogOutStages)
            }
            else if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal)
                && (getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal]!!) == 1.0)){
                resetFan(relayStages,analogOutStages,basicSettings)
                operateAuxBasedOnFan(HsPipe2RelayMapping.AUX_HEATING_STAGE2,relayStages)
                runSpecificAnalogFanSpeed(configuration,FanSpeed.HIGH,analogOutStages)
            }
            else {
                // if we don't have aux configuration
                doFanOperation(tuner, basicSettings, relayStages, configuration, userIntents, analogOutStages, equip)
            }
        } else {
            resetFan(relayStages,analogOutStages,basicSettings)
        }
    }

    private fun doHeatOnly(
            tuner: HyperStatProfileTuners,
            basicSettings: BasicSettings,
            relayStages: HashMap<String, Int>,
            configuration: Pipe2Configuration,
            userIntents: UserIntents,
            analogOutStages: HashMap<String, Int>,
            equip: Pipe2V2Equip
    ) {
        CcuLog.d(L.TAG_CCU_HSPIPE2, "doHeatOnly: mode ")

        // b. Deactivate Water Valve associated relay, if it is enabled and the fan speed is off.
        if (basicSettings.fanMode == StandaloneFanStage.OFF || supplyWaterTempTh2 < coolingThreshold) {
            resetWaterValue(relayStages, analogOutStages, equip)
        }

        if (basicSettings.fanMode != StandaloneFanStage.OFF) {
            if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal)
                && relayLogicalPoints.containsKey(HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal)
            ) {
                if (basicSettings.fanMode == StandaloneFanStage.AUTO) {
                    if (getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal]!!) == 1.0
                        && getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal]!!) == 0.0
                    ) {
                        resetFan(relayStages,analogOutStages,basicSettings)
                        operateAuxBasedOnFan(HsPipe2RelayMapping.AUX_HEATING_STAGE1,relayStages)
                        runSpecificAnalogFanSpeed(configuration,FanSpeed.MEDIUM,analogOutStages)
                        return
                    }
                    if (getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal]!!) == 1.0) {
                        resetFan(relayStages,analogOutStages,basicSettings)
                        operateAuxBasedOnFan(HsPipe2RelayMapping.AUX_HEATING_STAGE2,relayStages)
                        runSpecificAnalogFanSpeed(configuration,FanSpeed.HIGH,analogOutStages)
                    } else {
                        // For title 24 compliance when doorwindow is open, run the lowest fan speed
                        if((heatingLoopOutput > 0) || runFanLowDuringDoorWindow) {
                            doFanOperation(tuner, basicSettings, relayStages, configuration, userIntents, analogOutStages, equip)
                        } else {
                            resetFan(relayStages,analogOutStages,basicSettings)
                        }
                    }
                } else {
                    // If Fan has specific user intent
                    doFanOperation(tuner, basicSettings, relayStages, configuration, userIntents, analogOutStages, equip)
                }
            }
            else if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal)
                && (getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal]!!) == 1.0)){
                resetFan(relayStages,analogOutStages,basicSettings)
                operateAuxBasedOnFan(HsPipe2RelayMapping.AUX_HEATING_STAGE1,relayStages)
                runSpecificAnalogFanSpeed(configuration,FanSpeed.MEDIUM,analogOutStages)
            }
            else if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal)
                && (getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal]!!) == 1.0)){
                resetFan(relayStages,analogOutStages,basicSettings)
                operateAuxBasedOnFan(HsPipe2RelayMapping.AUX_HEATING_STAGE2,relayStages)
                runSpecificAnalogFanSpeed(configuration,FanSpeed.HIGH,analogOutStages)
            }
            else {
                // If we have don't have any aux configuration
                doFanOperation(tuner, basicSettings, relayStages, configuration, userIntents, analogOutStages, equip)
            }
        } else {
            resetFan(relayStages,analogOutStages,basicSettings)
        }
    }



    // New requirement for aux and fan operations If we do not have fan then no aux
    private fun operateAuxBasedOnFan(
            association: HsPipe2RelayMapping,
            relayStages: HashMap<String, Int>
    ) {
        var stage = HsPipe2RelayMapping.AUX_HEATING_STAGE1
        var state = 0
        var fanStatusMessage: Stage? = null
        if (association == HsPipe2RelayMapping.AUX_HEATING_STAGE1) {
            if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.FAN_MEDIUM_SPEED.ordinal)) {
                stage = HsPipe2RelayMapping.FAN_MEDIUM_SPEED
                state = 1
                fanStatusMessage = Stage.FAN_2
            } else if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)) {
                stage = HsPipe2RelayMapping.FAN_HIGH_SPEED
                state = 1
                fanStatusMessage = Stage.FAN_3
            } else if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.FAN_LOW_SPEED.ordinal)) {
                stage = HsPipe2RelayMapping.FAN_LOW_SPEED
                state = 1
                fanStatusMessage = Stage.FAN_1
            } else if (analogLogicalPoints.containsKey(HsPipe2AnalogOutMapping.FAN_SPEED.ordinal)) {
                stage = HsPipe2RelayMapping.FAN_ENABLED
                state = 1
            }
        }
        if (association == HsPipe2RelayMapping.AUX_HEATING_STAGE2) {
            if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)) {
                stage = HsPipe2RelayMapping.FAN_HIGH_SPEED
                state = 1
                fanStatusMessage = Stage.FAN_3
            } else if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.FAN_MEDIUM_SPEED.ordinal)) {
                stage = HsPipe2RelayMapping.FAN_MEDIUM_SPEED
                state = 1
                fanStatusMessage = Stage.FAN_2
            } else if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.FAN_LOW_SPEED.ordinal)) {
                stage = HsPipe2RelayMapping.FAN_LOW_SPEED
                state = 1
                fanStatusMessage = Stage.FAN_1
            } else if (analogLogicalPoints.containsKey(HsPipe2AnalogOutMapping.FAN_SPEED.ordinal)) {
                stage = HsPipe2RelayMapping.FAN_ENABLED
                state = 1
            }
        }
        if (state == 0) {
            resetAux(relayStages)
        } else {
            if (stage != HsPipe2RelayMapping.FAN_ENABLED) {
                updateLogicalPoint(relayLogicalPoints[stage.ordinal]!!, 1.0)
                relayStages[fanStatusMessage!!.displayName] = 1
            }
        }

    }


    private fun resetWaterValue(relayStages: HashMap<String, Int>, analogOutStages: HashMap<String, Int>, equip: Pipe2V2Equip) {
        if (equip.waterSamplingStartTime == 0L) {
            if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.WATER_VALVE.ordinal)) {
                resetLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.WATER_VALVE.ordinal]!!)
            }
            if (analogLogicalPoints.containsKey(HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)) {
                resetLogicalPoint(analogLogicalPoints[HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal]!!)
            }
            relayStages.remove(AnalogOutput.WATER_VALVE.name)
            analogOutStages.remove(AnalogOutput.WATER_VALVE.name)
            CcuLog.d(L.TAG_CCU_HSPIPE2, "Resetting WATER_VALVE to OFF")
        }
    }
    private fun resetAux(relayStages: HashMap<String, Int>) {
        if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal)) {
            resetLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal]!!)
        }
        if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal)) {
            resetLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal]!!)
        }
        relayStages.remove(HsPipe2RelayMapping.AUX_HEATING_STAGE1.name)
        relayStages.remove(HsPipe2RelayMapping.AUX_HEATING_STAGE2.name)

    }

    private fun processForWaterSampling(
            equip: Pipe2V2Equip, tuner: HyperStatProfileTuners,
            config: Pipe2Configuration, relayStages: HashMap<String, Int>,
            analogOutStages: HashMap<String, Int>, basicSettings: BasicSettings
    ) {

        if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
            resetWaterValue(relayStages, analogOutStages , equip)
            return
        }

        if (!config.isAnyRelayEnabledAssociated(association = HsPipe2RelayMapping.WATER_VALVE.ordinal) ||
                !config.isAnyAnalogOutEnabledAssociated(association = HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)) {
            CcuLog.d(L.TAG_CCU_HSPIPE2, "No mapping for water value")
            return
        }

        fun resetIsRequired(): Boolean {
            return ((relayLogicalPoints.containsKey(HsPipe2RelayMapping.WATER_VALVE.ordinal) &&
                    (getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.WATER_VALVE.ordinal]!!).toInt() != 0))
                    || (analogLogicalPoints.containsKey(HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal)
                    && (getCurrentLogicalPointStatus(analogLogicalPoints[HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal]!!).toInt() != 0)))
        }

        CcuLog.d(L.TAG_CCU_HSPIPE2, "waterSamplingStarted Time "+ equip.waterSamplingStartTime)

        val waitTimeToDoSampling: Int
        val onTimeToDoSampling: Int
        if (supplyWaterTempTh2 in coolingThreshold..heatingThreshold) {
            waitTimeToDoSampling = tuner.waterValveSamplingDuringLoopDeadbandWaitTime
            onTimeToDoSampling = tuner.waterValveSamplingDuringLoopDeadbandOnTime
        } else {
            waitTimeToDoSampling = tuner.waterValveSamplingWaitTime
            onTimeToDoSampling = tuner.waterValveSamplingOnTime
        }

        // added on 05-12-2022 If either one of the tuner value is 0 then we will not do water sampling
        if (waitTimeToDoSampling == 0 || onTimeToDoSampling == 0) {
            //resetting the water valve value value only when the tuner value is zero
            if (resetIsRequired()) {
                equip.waterSamplingStartTime = 0
                equip.lastWaterValveTurnedOnTime = System.currentTimeMillis()
                resetWaterValue(relayStages, analogOutStages, equip)
            }
            CcuLog.d(L.TAG_CCU_HSPIPE2, "No water sampling, because tuner value is zero!")
            return
        }
        CcuLog.d(L.TAG_CCU_HSPIPE2, "waitTimeToDoSampling:  $waitTimeToDoSampling onTimeToDoSampling: $onTimeToDoSampling")
        CcuLog.d(L.TAG_CCU_HSPIPE2, "Current : ${System.currentTimeMillis()}: Last On: ${equip.lastWaterValveTurnedOnTime}")

        if (equip.waterSamplingStartTime == 0L) {
            val minutes = milliToMin(System.currentTimeMillis() - equip.lastWaterValveTurnedOnTime)
            CcuLog.d(L.TAG_CCU_HSPIPE2, "sampling will start in : ${waitTimeToDoSampling - minutes} current : $minutes")
            if (minutes >= waitTimeToDoSampling) {
                doWaterSampling(equip, relayStages)
            }
        } else {
            val samplingSinceFrom = milliToMin(System.currentTimeMillis() - equip.waterSamplingStartTime)
            CcuLog.d(L.TAG_CCU_HSPIPE2, "Water sampling is running since from $samplingSinceFrom minutes")
            if (samplingSinceFrom >= onTimeToDoSampling) {
                equip.waterSamplingStartTime = 0
                equip.lastWaterValveTurnedOnTime = System.currentTimeMillis()
                resetWaterValue(relayStages, analogOutStages, equip)
                CcuLog.d(L.TAG_CCU_HSPIPE2, "Resetting WATER_VALVE to OFF")
            } else {
                relayStages[AnalogOutput.WATER_VALVE.name] = 1
            }
        }
    }

    private fun doWaterSampling(
            equip: Pipe2V2Equip,
            relayStages: HashMap<String, Int>,
    ) {
        equip.waterSamplingStartTime = System.currentTimeMillis()
        updateLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.WATER_VALVE.ordinal],1.0)
        updateLogicalPoint(analogLogicalPoints[HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal],100.0)
        relayStages[AnalogOutput.WATER_VALVE.name] = 1
        CcuLog.d(L.TAG_CCU_HSPIPE2, "Turned ON water valve ")
    }

    /**
     * Executes fan operations based on Title 24 compliance rules when in AUTO mode and door/window sensors are open.
     * This function checks the lowest configured fan speed and runs the corresponding fan stages accordingly.
     * If the door or window is open and in one of the occupied mode,
     * it ensures that the fan operates at the lowest configured speed to comply with regulations.
     *
     * @param tuner The HyperStat profile tuners containing specific runtime parameters.
     * @param basicSettings The basic settings for the HVAC system.
     * @param relayStages A hashmap of relay stages representing the current state of fan relays.
     * @param userIntents The user intents that may influence fan operation.
     * @param analogOutStages A hashmap of analog output stages representing fan speeds.
     * @param configuration The HyperStat Pipe2 configuration defining the setup.
     */
    private fun doFanOperationTitle24(tuner: HyperStatProfileTuners,
                                      basicSettings: BasicSettings,
                                      relayStages: HashMap<String, Int>,
                                      userIntents: UserIntents,
                                      analogOutStages: HashMap<String, Int>,
                                      configuration: Pipe2Configuration) {
        if (basicSettings.fanMode == StandaloneFanStage.AUTO && runFanLowDuringDoorWindow) {
            val lowestStage = configuration.getLowestFanSelected()

            // Check which fan speed is the lowest and set the status(Eg: If FAN_MEDIUM and FAN_HIGH are used, then FAN_MEDIUM is the lowest)
            resetFanStatus()
            when (lowestStage) {
                HsPipe2RelayMapping.FAN_LOW_SPEED -> setFanLowestFanLowStatus(true)
                HsPipe2RelayMapping.FAN_MEDIUM_SPEED -> setFanLowestFanMediumStatus(true)
                HsPipe2RelayMapping.FAN_HIGH_SPEED -> setFanLowestFanHighStatus(true)
                else -> {
                    // Do nothing
                }
            }

            // When door window open, run the lowest fan speed. Anyhow the fanloop output is 0, this will be
            // handled in the following APIs
            runFanHigh(tuner, basicSettings, relayStages, fanLoopOutput)
            runFanMedium(tuner, basicSettings, relayStages, configuration, fanLoopOutput)
            runFanLow(tuner, basicSettings, relayStages, configuration, fanLoopOutput)
            runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
        }
    }

    private fun doFanOperation(
            tuner: HyperStatProfileTuners, basicSettings: BasicSettings, relayStages: HashMap<String, Int>,
            configuration: Pipe2Configuration, userIntents: UserIntents, analogOutStages: HashMap<String, Int>, equip: Pipe2V2Equip
    ) {
        val lowestStage = configuration.getLowestFanSelected()

        // Check which fan speed is the lowest and set the status(Eg: If FAN_MEDIUM and FAN_HIGH are used, then FAN_MEDIUM is the lowest)
        resetFanStatus()

        when (lowestStage) {
            HsPipe2RelayMapping.FAN_LOW_SPEED -> setFanLowestFanLowStatus(true)
            HsPipe2RelayMapping.FAN_MEDIUM_SPEED -> setFanLowestFanMediumStatus(true)
            HsPipe2RelayMapping.FAN_HIGH_SPEED -> setFanLowestFanHighStatus(true)
            else -> {
                // Do nothing
            }
        }

        if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
            if (basicSettings.fanMode != StandaloneFanStage.AUTO) {
                runFanHigh(tuner, basicSettings, relayStages, fanLoopOutput)
                runFanMedium(tuner, basicSettings, relayStages, configuration, fanLoopOutput)
                runFanLow(tuner, basicSettings, relayStages, configuration, fanLoopOutput)
                runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
            } else {
                resetFan(relayStages, analogOutStages, basicSettings)
            }
        } else {

            if (basicSettings.fanMode == StandaloneFanStage.AUTO
                    && supplyWaterTempTh2 > heatingThreshold
                    && supplyWaterTempTh2 in coolingThreshold..heatingThreshold
                    && currentTemp > userIntents.zoneCoolingTargetTemperature) {
                resetFan(relayStages, analogOutStages, basicSettings)
            } else {
                if (basicSettings.fanMode != StandaloneFanStage.AUTO && basicSettings.fanMode != StandaloneFanStage.OFF) {
                    runFanHigh(tuner, basicSettings, relayStages, fanLoopOutput)
                    runFanMedium(tuner, basicSettings, relayStages, configuration, fanLoopOutput)
                    runFanLow(tuner, basicSettings, relayStages, configuration, fanLoopOutput)
                    runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
                    return
                }
                if (equip.waterSamplingStartTime == 0L && (relayLogicalPoints.containsKey(HsPipe2RelayMapping.WATER_VALVE.ordinal) &&
                                getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.WATER_VALVE.ordinal]!!) == 1.0)) {

                    runFanHigh(tuner, basicSettings, relayStages, fanLoopOutput)
                    runFanMedium(tuner, basicSettings, relayStages, configuration, fanLoopOutput)
                    runFanLow(tuner, basicSettings, relayStages, configuration, fanLoopOutput)
                    runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
                } else if (equip.waterSamplingStartTime == 0L && analogLogicalPoints.containsKey(HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal) &&
                        getCurrentLogicalPointStatus(analogLogicalPoints[HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal]!!) > 1.0) {
                    runFanHigh(tuner, basicSettings, relayStages, fanLoopOutput)
                    runFanMedium(tuner, basicSettings, relayStages, configuration, fanLoopOutput)
                    runFanLow(tuner, basicSettings, relayStages, configuration, fanLoopOutput)
                    runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
                } else if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal)
                        && getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE1.ordinal]!!) == 1.0) {
                    runFanHigh(tuner, basicSettings, relayStages, fanLoopOutput)
                    runFanMedium(tuner, basicSettings, relayStages, configuration, fanLoopOutput)
                    runFanLow(tuner, basicSettings, relayStages, configuration, fanLoopOutput)
                    runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
                } else if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal)
                        && getCurrentLogicalPointStatus(relayLogicalPoints[HsPipe2RelayMapping.AUX_HEATING_STAGE2.ordinal]!!) == 1.0) {
                    runFanHigh(tuner, basicSettings, relayStages, fanLoopOutput)
                    runFanMedium(tuner, basicSettings, relayStages, configuration, fanLoopOutput)
                    runFanLow(tuner, basicSettings, relayStages, configuration, fanLoopOutput)
                    runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
                } else {
                    resetFan(relayStages, analogOutStages, basicSettings)
                }
            }
        }
    }

    // Do fan low speed
    private fun runFanLow(
            tuner: HyperStatProfileTuners, basicSettings: BasicSettings,
            relayStages: HashMap<String, Int>, configuration: Pipe2Configuration, fanLoop: Int
    ) {
        if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.FAN_LOW_SPEED.ordinal)) {
            val highestStage = configuration.getHighestFanSelected()
            val divider = if (highestStage == HsPipe2RelayMapping.FAN_MEDIUM_SPEED) 50 else 33

            doFanLowSpeed(
                    relayLogicalPoints[HsPipe2RelayMapping.FAN_LOW_SPEED.ordinal]!!,
                    getSuperLogicalPointIfExist(HsPipe2RelayMapping.FAN_MEDIUM_SPEED),
                    getSuperLogicalPointIfExist(HsPipe2RelayMapping.FAN_HIGH_SPEED),
                    basicSettings.fanMode, fanLoop, tuner.relayActivationHysteresis,
                    relayStages, divider, runFanLowDuringDoorWindow
            )
        }
    }

    private fun runFanMedium(
            tuner: HyperStatProfileTuners, basicSettings: BasicSettings,
            relayStages: HashMap<String, Int>, configuration: Pipe2Configuration, fanLoop: Int
    ) {
        if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.FAN_MEDIUM_SPEED.ordinal)) {

            val highestStage = configuration.getHighestFanSelected()
            val divider = if (highestStage == HsPipe2RelayMapping.FAN_MEDIUM_SPEED) 50 else 33

            doFanMediumSpeed(
                    relayLogicalPoints[HsPipe2RelayMapping.FAN_MEDIUM_SPEED.ordinal]!!,
                    getSuperLogicalPointIfExist(HsPipe2RelayMapping.FAN_HIGH_SPEED),
                    basicSettings.fanMode, fanLoop, tuner.relayActivationHysteresis,
                    divider, relayStages, runFanLowDuringDoorWindow
            )
        }
    }

    private fun runFanHigh(
            tuner: HyperStatProfileTuners,
            basicSettings: BasicSettings,
            relayStages: HashMap<String, Int>,
            fanLoop: Int,
    ) {
        if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)) {
            doFanHighSpeed(
                    relayLogicalPoints[HsPipe2RelayMapping.FAN_HIGH_SPEED.ordinal]!!,
                    basicSettings.fanMode, fanLoop, tuner.relayActivationHysteresis,
                    relayStages, runFanLowDuringDoorWindow
            )
        }
    }


    private fun runAnalogFanSpeed(
            config: Pipe2Configuration,
            userIntents: UserIntents,
            analogOutStages: HashMap<String, Int>,
            basicSettings: BasicSettings
    ){
        var output = fanLoopOutput
        if (supplyWaterTempTh2 > heatingThreshold && currentTemp > userIntents.zoneCoolingTargetTemperature) {
            output = 0
        }

        config.apply {
            listOf(
                    Pair(Pipe2AnalogOutConfigs(analogOut1Enabled.enabled, analogOut1Association.associationVal, analogOut1MinMaxConfig, analogOut1FanSpeedConfig), Port.ANALOG_OUT_ONE),
                    Pair(Pipe2AnalogOutConfigs(analogOut2Enabled.enabled, analogOut2Association.associationVal, analogOut2MinMaxConfig, analogOut2FanSpeedConfig), Port.ANALOG_OUT_TWO),
                    Pair(Pipe2AnalogOutConfigs(analogOut3Enabled.enabled, analogOut3Association.associationVal, analogOut3MinMaxConfig, analogOut3FanSpeedConfig), Port.ANALOG_OUT_THREE),
            ).forEach { (analogOutConfig, port) ->
                if (analogOutConfig.enabled && analogOutConfig.association == HsPipe2AnalogOutMapping.FAN_SPEED.ordinal) {
                    doAnalogFanAction(
                            port, analogOutConfig.fanSpeed.low.currentVal.toInt(),
                            analogOutConfig.fanSpeed.medium.currentVal.toInt(),
                            analogOutConfig.fanSpeed.high.currentVal.toInt(),
                            basicSettings.fanMode, basicSettings.conditioningMode,
                            output, analogOutStages
                    )

                }
            }
        }
    }


    private fun getSuperLogicalPointIfExist(association: HsPipe2RelayMapping ): String? {
        if(relayLogicalPoints.containsKey(association.ordinal)){
            return relayLogicalPoints[association.ordinal]
        }
        return null
    }


    // If Fan is off we need to reset all the
    /**
    Operation in Off Mode
    All relays associated with fan, water valve, Aux Heating Stage 1, Aux Heating Stage 1,
    Fan enable, Occupied enable will be not functional and analog-outs will be reset to their minimum.
     */
    private fun resetConditioning(
        relayStages: HashMap<String, Int> ,
        analogOutStages: HashMap<String, Int>,
        basicSettings: BasicSettings,
        equip: Pipe2V2Equip

    ) {
        // Reset Relay
            resetFan(relayStages,analogOutStages,basicSettings)
            resetWaterValue(relayStages, analogOutStages, equip)
            resetAux(relayStages)

            if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.FAN_ENABLED.ordinal)) resetLogicalPoint(
                relayLogicalPoints[HsPipe2RelayMapping.FAN_ENABLED.ordinal]!!
            )
            if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.OCCUPIED_ENABLED.ordinal)) resetLogicalPoint(
                relayLogicalPoints[HsPipe2RelayMapping.OCCUPIED_ENABLED.ordinal]!!
            )

            if (analogLogicalPoints.containsKey(HsPipe2AnalogOutMapping.DCV_DAMPER.ordinal)) resetLogicalPoint(
                analogLogicalPoints[HsPipe2AnalogOutMapping.DCV_DAMPER.ordinal]!!
            )
    }

    private fun resetFan(
        relayStages: HashMap<String, Int> ,
        analogOutStages: HashMap<String, Int>,
        basicSettings: BasicSettings
    ){
        if(basicSettings.fanMode == StandaloneFanStage.AUTO||basicSettings.fanMode == StandaloneFanStage.OFF) {
            if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.FAN_LOW_SPEED.ordinal)) {
                resetLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.FAN_LOW_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_1.displayName)
            }
            if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.FAN_MEDIUM_SPEED.ordinal)) {
                resetLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.FAN_MEDIUM_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_2.displayName)
            }
            if (relayLogicalPoints.containsKey(HsPipe2RelayMapping.FAN_HIGH_SPEED.ordinal)) {
                resetLogicalPoint(relayLogicalPoints[HsPipe2RelayMapping.FAN_HIGH_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_3.displayName)
            }
            if (analogLogicalPoints.containsKey(HsPipe2AnalogOutMapping.FAN_SPEED.ordinal)) {
                resetLogicalPoint(analogLogicalPoints[HsPipe2AnalogOutMapping.FAN_SPEED.ordinal]!!)
                analogOutStages.remove(AnalogOutput.FAN_SPEED.name)
            }
        }
    }

    private fun handleAnalogOutState(
            analogOutState: Pipe2AnalogOutConfigs, equip: Pipe2V2Equip, config: Pipe2Configuration,
            port: Port, basicSettings: BasicSettings, analogOutStages: HashMap<String, Int>, userIntents: UserIntents
    ) {
        val analogMapping = HsPipe2AnalogOutMapping.values().find { it.ordinal == analogOutState.association }
        when (analogMapping) {
            HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE -> {
                if (equip.waterSamplingStartTime == 0L && basicSettings.conditioningMode != StandaloneConditioningMode.OFF) {
                    doAnalogWaterValveAction(
                            port, basicSettings.fanMode, basicSettings,
                            waterValveLoop(userIntents), analogOutStages
                    )
                }
            }

            HsPipe2AnalogOutMapping.DCV_DAMPER -> {
                doAnalogDCVAction(
                        port, analogOutStages, config.zoneCO2Threshold.currentVal,
                        config.zoneCO2DamperOpeningRate.currentVal,
                        isDoorOpenState(config, equip), equip
                )
            }

            else -> {}
        }
    }


    private fun runSpecificAnalogFanSpeed(config: Pipe2Configuration, fanSpeed: FanSpeed, analogOutStages: HashMap<String, Int>) {
        var analogOutputsUpdated = 0

        fun getPercent(fanConfig: FanConfig, fanSpeed: FanSpeed): Double {
            return when (fanSpeed) {
                FanSpeed.HIGH -> fanConfig.high.currentVal
                FanSpeed.MEDIUM -> fanConfig.medium.currentVal
                FanSpeed.LOW -> fanConfig.low.currentVal
                else -> 0.0
            }
        }

        config.apply {
            listOf(
                    Pair(Pipe2AnalogOutConfigs(analogOut1Enabled.enabled, analogOut1Association.associationVal, analogOut1MinMaxConfig, analogOut1FanSpeedConfig), Port.ANALOG_OUT_ONE),
                    Pair(Pipe2AnalogOutConfigs(analogOut2Enabled.enabled, analogOut2Association.associationVal, analogOut2MinMaxConfig, analogOut2FanSpeedConfig), Port.ANALOG_OUT_TWO),
                    Pair(Pipe2AnalogOutConfigs(analogOut3Enabled.enabled, analogOut3Association.associationVal, analogOut3MinMaxConfig, analogOut3FanSpeedConfig), Port.ANALOG_OUT_THREE),
            ).forEach { (analogOutConfig, port) ->
                if (analogOutConfig.enabled && analogOutConfig.association == HsPipe2AnalogOutMapping.FAN_SPEED.ordinal) {
                    updateLogicalPoint(logicalPointsList[port]!!, getPercent(analogOutConfig.fanSpeed, fanSpeed))
                    analogOutputsUpdated++
                }
            }
        }
        if (analogOutputsUpdated > 0 && fanSpeed != FanSpeed.OFF) {
            analogOutStages[AnalogOutput.FAN_SPEED.name] = 1
        }
    }



    private fun waterValveLoop(userIntents: UserIntents): Int {
        val supplyWaterTempTh2AboveHeating = supplyWaterTempTh2 > heatingThreshold
        val supplyWaterTempTh2BelowCooling = supplyWaterTempTh2 < coolingThreshold

        val zoneExpectingHeat = currentTemp < userIntents.zoneHeatingTargetTemperature
        val zoneExpectingCool = currentTemp > userIntents.zoneCoolingTargetTemperature

        if (supplyWaterTempTh2AboveHeating && zoneExpectingHeat) {
            // Supply heating
            return heatingLoopOutput
        } else if (supplyWaterTempTh2BelowCooling && zoneExpectingCool) {
            // Supply cooling
            return coolingLoopOutput
        } else {
            // No supply
            if (supplyWaterTempTh2AboveHeating) {
                return if (heatingLoopOutput > 0) heatingLoopOutput else 0
            } else if (supplyWaterTempTh2BelowCooling) {
                return if (coolingLoopOutput > 0) coolingLoopOutput else 0
            }
        }
        return 0
    }

    
    private fun isDoorOpenState(config: HyperStatConfiguration, equip: HyperStatEquip): Boolean {

        fun isAnalogHasDoorWindowMapping(): Boolean {
            return (config.isEnabledAndAssociated(config.analogOut1Enabled, config.analogIn1Association, AnalogInputAssociation.DOOR_WINDOW_SENSOR_TITLE_24.ordinal)
                    || config.isEnabledAndAssociated(config.analogOut2Enabled, config.analogIn2Association, AnalogInputAssociation.DOOR_WINDOW_SENSOR_TITLE_24.ordinal))
        }

        var isDoorOpen = false
        var analogSensorEnabled = false

        if (isAnalogHasDoorWindowMapping()) {
            val sensorValue = equip.doorWindowSensorTitle24.readHisVal().toInt()
            if (sensorValue == 1) isDoorOpen = true
            analogSensorEnabled = true
            CcuLog.i(L.TAG_CCU_HSPIPE2, "Analog Input has mapping Door Window sensor value : Door is $sensorValue")

        }
        doorWindowIsOpen(
                if (analogSensorEnabled) 1.0 else 0.0,
                if (isDoorOpen) 1.0 else 0.0, equip
        )
        return isDoorOpen
    }

    private fun runForKeyCardSensor(config: HyperStatConfiguration, equip: HyperStatEquip) {
        val isKeyCardEnabled = (config.isEnabledAndAssociated(config.analogIn1Enabled, config.analogIn1Association, AnalogInputAssociation.KEY_CARD_SENSOR.ordinal)
                || config.isEnabledAndAssociated(config.analogIn1Enabled, config.analogIn2Association, AnalogInputAssociation.KEY_CARD_SENSOR.ordinal))
        keyCardIsInSlot((if (isKeyCardEnabled) 1.0 else 0.0), if (equip.keyCardSensor.readHisVal() > 0) 1.0 else 0.0, equip)
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

    private fun runForDoorWindowSensor(
            config: HyperStatConfiguration, equip: HyperStatEquip,
            analogOutStages: HashMap<String, Int>, relayStages: HashMap<String, Int>
    ): Boolean {

        val isDoorOpen = isDoorOpenState(config, equip)
        CcuLog.d(L.TAG_CCU_HSPIPE2, " is Door Open ? $isDoorOpen")
        if (isDoorOpen) {
            resetLoopOutputValues()
            resetLogicalPoints()
            analogOutStages.clear()
            relayStages.clear()
        }
        return isDoorOpen
    }

    private fun resetLoopOutputValues() {
        CcuLog.d(L.TAG_CCU_HSPIPE2, "Resetting all the loop output values: ")
        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
    }


    override fun getCurrentTemp(): Double {
        for (nodeAddress in pipe2DeviceMap.keys) {
            return pipe2DeviceMap[nodeAddress]!!.currentTemp.readHisVal()
        }
        return 0.0
    }
    
    override fun getDisplayCurrentTemp() = averageZoneTemp

    override fun getAverageZoneTemp(): Double {
        var tempTotal = 0.0
        var nodeCount = 0
        pipe2DeviceMap.forEach { (_, device) ->
            if (device.currentTemp.readHisVal() > 0) {
                tempTotal += device.currentTemp.readHisVal()
                nodeCount++
            }
        }
        return if (nodeCount == 0) 0.0 else tempTotal / nodeCount
    }

    private fun milliToMin(milliseconds: Long) = (milliseconds / (1000 * 60) % 60)

    private fun operateAnalogOutputs(
            config: Pipe2Configuration, equip: Pipe2V2Equip, basicSettings: BasicSettings,
            analogOutStages: HashMap<String, Int>, userIntents: UserIntents
    ) {
        config.apply {
            listOf(
                    Pair(Pipe2AnalogOutConfigs(analogOut1Enabled.enabled, analogOut1Association.associationVal, analogOut1MinMaxConfig, analogOut1FanSpeedConfig), Port.ANALOG_OUT_ONE),
                    Pair(Pipe2AnalogOutConfigs(analogOut2Enabled.enabled, analogOut2Association.associationVal, analogOut2MinMaxConfig, analogOut2FanSpeedConfig), Port.ANALOG_OUT_TWO),
                    Pair(Pipe2AnalogOutConfigs(analogOut3Enabled.enabled, analogOut3Association.associationVal, analogOut3MinMaxConfig, analogOut3FanSpeedConfig), Port.ANALOG_OUT_THREE),
            ).forEach { (analogOutConfig, port) ->
                if (analogOutConfig.enabled) handleAnalogOutState(analogOutConfig, equip, config, port, basicSettings, analogOutStages, userIntents)
            }
        }
    }

    private fun operateRelays(
            config: Pipe2Configuration, tuner: HyperStatProfileTuners, userIntents: UserIntents, basicSettings: BasicSettings,
            relayStages: HashMap<String, Int>, relayOutputPoints: HashMap<Int, String>, equip: Pipe2V2Equip, analogOutStages: HashMap<String, Int>
    ) {
        listOf(
                Triple(config.relay1Enabled.enabled, config.relay1Association.associationVal, Port.RELAY_ONE),
                Triple(config.relay2Enabled.enabled, config.relay2Association.associationVal, Port.RELAY_TWO),
                Triple(config.relay3Enabled.enabled, config.relay3Association.associationVal, Port.RELAY_THREE),
                Triple(config.relay4Enabled.enabled, config.relay4Association.associationVal, Port.RELAY_FOUR),
                Triple(config.relay5Enabled.enabled, config.relay5Association.associationVal, Port.RELAY_FIVE),
                Triple(config.relay6Enabled.enabled, config.relay6Association.associationVal, Port.RELAY_SIX)
        ).forEach { (enabled, association, port) ->
            if (enabled) handleRelayState(association, config, port, tuner, userIntents, basicSettings, relayStages, relayOutputPoints, equip, analogOutStages)
        }
    }


    private fun isEligibleForHeating(conditioningMode: StandaloneConditioningMode): Boolean {
        return  conditioningMode == StandaloneConditioningMode.AUTO || conditioningMode == StandaloneConditioningMode.HEAT_ONLY
    }

    private fun handleRelayState(
            association: Int, config: Pipe2Configuration, port: Port, tuner: HyperStatProfileTuners,
            userIntents: UserIntents, basicSettings: BasicSettings, relayStages: HashMap<String, Int>,
            relayOutputPoints: HashMap<Int, String>, equip: Pipe2V2Equip, analogOutStages: HashMap<String, Int>
    ) {


        val relayMapping = HsPipe2RelayMapping.values().find { it.ordinal == association }
        when (relayMapping) {

            HsPipe2RelayMapping.AUX_HEATING_STAGE1 -> {
                operateAuxStageHeating(
                        relayMapping, relayOutputPoints, relayStages, tuner.auxHeating1Activate,
                        userIntents, config, analogOutStages, FanSpeed.MEDIUM, basicSettings
                )
            }

            HsPipe2RelayMapping.AUX_HEATING_STAGE2 -> {
                operateAuxStageHeating(
                        relayMapping, relayOutputPoints, relayStages, tuner.auxHeating2Activate,
                        userIntents, config, analogOutStages, FanSpeed.HIGH, basicSettings
                )
            }

            HsPipe2RelayMapping.WATER_VALVE -> {
                if (equip.waterSamplingStartTime == 0L && basicSettings.conditioningMode != StandaloneConditioningMode.OFF) {
                    doRelayWaterValveOperation(
                            equip, port, basicSettings, waterValveLoop(userIntents),
                            tuner.relayActivationHysteresis, relayStages
                    )
                }
            }

            HsPipe2RelayMapping.FAN_ENABLED -> doFanEnabled(curState, port, fanLoopOutput, relayStages)
            HsPipe2RelayMapping.OCCUPIED_ENABLED -> doOccupiedEnabled(port)
            HsPipe2RelayMapping.HUMIDIFIER -> doHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMinInsideHumidity, equip.zoneHumidity.readHisVal())
            HsPipe2RelayMapping.DEHUMIDIFIER -> doDeHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMaxInsideHumidity, equip.zoneHumidity.readHisVal())
            else -> {}
        }
    }


    private fun operateAuxStageHeating(
            auxMapping: HsPipe2RelayMapping, relayOutputPoints: HashMap<Int, String>,
            relayStages: HashMap<String, Int>, auxHeatingActivate: Double, userIntents: UserIntents,
            config: Pipe2Configuration, analogOutStages: HashMap<String, Int>, fanSpeed: FanSpeed,
            basicSettings: BasicSettings
    ) {
        if (!isEligibleForHeating(basicSettings.conditioningMode)) {
            resetAux(relayStages)
            return
        }

        if (currentTemp < userIntents.zoneHeatingTargetTemperature - auxHeatingActivate) {
            updateLogicalPoint(relayOutputPoints[auxMapping.ordinal], 1.0)
            relayStages[auxMapping.name] = 1
            runSpecificAnalogFanSpeed(config, fanSpeed, analogOutStages)

        } else if (currentTemp >= userIntents.zoneHeatingTargetTemperature - (auxHeatingActivate - 1)) {
            updateLogicalPoint(relayOutputPoints[auxMapping.ordinal], 0.0)
            runSpecificAnalogFanSpeed(config, FanSpeed.OFF, analogOutStages)
        } else {
            if (getCurrentLogicalPointStatus(relayOutputPoints[auxMapping.ordinal]!!) == 1.0) relayStages[auxMapping.name] = 1
        }
    }

    private fun showOutputStatus() {
        logicalPointsList.toSortedMap().forEach { (port, logicalPointId) ->
            var mapping: String? = null
            when (port) {
                Port.RELAY_ONE, Port.RELAY_TWO, Port.RELAY_THREE, Port.RELAY_FOUR, Port.RELAY_FIVE, Port.RELAY_SIX -> {
                    val enumPos = relayLogicalPoints.entries.find { it.value.contentEquals(logicalPointId) }?.key
                    mapping = HsPipe2RelayMapping.values().find { it.ordinal == enumPos }?.name
                }

                Port.ANALOG_OUT_ONE, Port.ANALOG_OUT_TWO, Port.ANALOG_OUT_THREE -> {
                    val enumPos = analogLogicalPoints.entries.find { it.value.contentEquals(logicalPointId) }?.key
                    mapping = HsPipe2AnalogOutMapping.values().find { it.ordinal == enumPos }?.name
                }
                else -> {}
            }

            CcuLog.i(L.TAG_CCU_HSPIPE2, "$port = $mapping : ${getCurrentLogicalPointStatus(logicalPointsList[port]!!)}")
        }
    }


    fun getProfileDomainEquip(node: Int): Pipe2V2Equip = pipe2DeviceMap[node]!!

    override fun getNodeAddresses(): Set<Short?> = pipe2DeviceMap.keys.map { it.toShort() }.toSet()

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        TODO("Not using now")
    }

    fun supplyDirection(): String {
        return if (supplyWaterTempTh2 > heatingThreshold
            || supplyWaterTempTh2 in coolingThreshold..heatingThreshold) {
            "Heating"
        } else {
            "Cooling"
        }
    }

}