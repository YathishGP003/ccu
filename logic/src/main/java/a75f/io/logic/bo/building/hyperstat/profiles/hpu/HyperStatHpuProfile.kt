package a75f.io.logic.bo.building.hyperstat.profiles.hpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.util.hayStack
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
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil
import a75f.io.logic.bo.building.hyperstat.common.HyperStatEquip
import a75f.io.logic.bo.building.hyperstat.common.HyperStatProfileTuners
import a75f.io.logic.bo.building.hyperstat.common.HyperstatLoopController
import a75f.io.logic.bo.building.hyperstat.common.UserIntents
import a75f.io.logic.bo.building.hyperstat.profiles.HyperStatPackageUnitProfile
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.HyperStatCpuConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.Th2InAssociation
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.Pipe2RelayAssociation
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.jobs.HyperStatUserIntentHandler
import a75f.io.logic.tuners.TunerUtil
import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Created by Manjunath K on 02-01-2023.
 */

class HyperStatHpuProfile : HyperStatPackageUnitProfile(){
    private val hpuDeviceMap: MutableMap<Short, HyperStatHpuEquip> = mutableMapOf()
    private var coolingLoopOutput = 0
    private var heatingLoopOutput = 0
    private var fanLoopOutput = 0
    private var doorWindowSensorOpenStatus = false
    private var runFanLowDuringDoorWindow = false
    private var compressorLoopOutput = 0
    override lateinit var occupancyStatus: Occupancy
    private var occupancyBeforeDoorWindow = Occupancy.UNOCCUPIED
    private val hyperStatHpuAlgorithm = HyperstatLoopController()
    private lateinit var curState: ZoneState

    private var analogOutputPoints: HashMap<Int, String> = HashMap()
    private var relayOutputPoints: HashMap<Int, String> = HashMap()

    override fun updateZonePoints() {
        hpuDeviceMap.forEach { (_, equip) ->
            logIt( "Process Equip: equipRef =  ${equip.equipRef}")
             processHyperStatHpuProfile(equip)
        }
    }

    // Run the profile logic and algorithm for an equip.
    private fun processHyperStatHpuProfile(equip: HyperStatHpuEquip) {

        if (Globals.getInstance().isTestMode) {
            logIt( "Test mode is on: ${equip.nodeAddress}")
            return
        }
        if (mInterface != null) mInterface.refreshView()

        val relayStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()
        logicalPointsList = equip.getLogicalPointList()
        relayOutputPoints = equip.getRelayOutputPoints()
        analogOutputPoints = equip.getAnalogOutputPoints()
        hsHaystackUtil = HSHaystackUtil(equip.equipRef!!, CCUHsApi.getInstance())
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
        val actualFanMode = HSHaystackUtil.getHpuActualFanMode(equip.node.toString(), fanModeSaved)
        val basicSettings = fetchBasicSettings(equip)
        logIt("Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef!!, fanModeSaved, actualFanMode, basicSettings)
        basicSettings.fanMode = updatedFanMode
        logIt("After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")
        hyperStatHpuAlgorithm.initialise(tuners = hyperStatTuners)
        hyperStatHpuAlgorithm.dumpLogs()
        handleChangeOfDirection(userIntents)
        
        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        compressorLoopOutput = 0
        evaluateLoopOutputs(userIntents, basicSettings, hyperStatTuners)
        
        equip.hsHaystackUtil.updateOccupancyDetection()
        doorWindowSensorOpenStatus = runForDoorWindowSensor(config, equip)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(equip)
        runForKeyCardSensor(config, equip)
        equip.hsHaystackUtil.updateAllLoopOutput(coolingLoopOutput,heatingLoopOutput,fanLoopOutput,true,compressorLoopOutput)
        val currentOperatingMode = equip.hsHaystackUtil.getOccupancyModePointValue().toInt()

        logIt(
            "Analog Fan speed multiplier  ${hyperStatTuners.analogFanSpeedMultiplier} \n"+
                    "Current Working mode : ${Occupancy.values()[currentOperatingMode]} \n"+
                    "Current Temp : $currentTemp \n"+
                    "Fan Mode : ${basicSettings.fanMode} Conditioning Mode ${basicSettings.conditioningMode} \n" +
                    "Desired Heating: ${userIntents.zoneHeatingTargetTemperature} \n"+
                    "Desired Cooling: ${userIntents.zoneCoolingTargetTemperature} \n"+
                    "Heating Loop Output: $heatingLoopOutput \n"+
                    "Cooling Loop Output:: $coolingLoopOutput \n"+
                    "Fan Loop Output:: $fanLoopOutput \n"+
                    "Compressor Loop Output:: $compressorLoopOutput \n"
        )


        if (basicSettings.fanMode != StandaloneFanStage.OFF) {
            runRelayOperations(
                config,
                hyperStatTuners,
                userIntents,
                basicSettings,
                relayStages
            )
            runAnalogOutOperations(equip, config, basicSettings, analogOutStages)
            runFanOperationBasedOnAuxStages(relayStages,analogOutStages,config)
        }else{
            resetAllLogicalPointValues()
        }
        
        setOperatingMode(currentTemp, averageDesiredTemp, basicSettings, equip)
        
        if (equip.hsHaystackUtil.getStatus() != curState.ordinal.toDouble())
            equip.hsHaystackUtil.setStatus(curState.ordinal.toDouble())
        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY

        logIt( "Equip Running : $curState")
        HyperStatUserIntentHandler.updateHyperStatStatus(
            equip.equipRef!!, relayStages, analogOutStages, temperatureState
        )
        if(occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus

        dumpOutput()
    }

    private fun evaluateLoopOutputs(userIntents: UserIntents, basicSettings: BasicSettings, hyperStatTuners: HyperStatProfileTuners){
        when (state) {
            //Update coolingLoop when the zone is in cooling or it was in cooling and no change over happened yet.
            ZoneState.COOLING -> coolingLoopOutput = hyperStatHpuAlgorithm.calculateCoolingLoopOutput(
                currentTemp, userIntents.zoneCoolingTargetTemperature
            ).toInt().coerceAtLeast(0)

            //Update heatingLoop when the zone is in heating or it was in heating and no change over happened yet.
            ZoneState.HEATING -> heatingLoopOutput = hyperStatHpuAlgorithm.calculateHeatingLoopOutput(
                userIntents.zoneHeatingTargetTemperature, currentTemp
            ).toInt().coerceAtLeast(0)

            else -> logIt( " Zone is in deadband")
        }

        if (coolingLoopOutput > 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY
                    ||basicSettings.conditioningMode == StandaloneConditioningMode.AUTO) ) {
            fanLoopOutput = ((coolingLoopOutput * hyperStatTuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
            compressorLoopOutput = coolingLoopOutput
        }
        else if (heatingLoopOutput > 0  && (basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY
                    ||basicSettings.conditioningMode == StandaloneConditioningMode.AUTO)) {
            fanLoopOutput = ((heatingLoopOutput * hyperStatTuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
            compressorLoopOutput = heatingLoopOutput
        }
    }
    
    private fun handleChangeOfDirection(userIntents: UserIntents){
        if (currentTemp > userIntents.zoneCoolingTargetTemperature && state != ZoneState.COOLING) {
            hyperStatHpuAlgorithm.resetCoolingControl()
            state = ZoneState.COOLING
            logIt("Resetting cooling")
        } else if (currentTemp < userIntents.zoneHeatingTargetTemperature && state != ZoneState.HEATING) {
            hyperStatHpuAlgorithm.resetHeatingControl()
            state = ZoneState.HEATING
            logIt("Resetting heating")
        }
    }

    private fun runRelayOperations(
        config: HyperStatHpuConfiguration,
        tuner: HyperStatProfileTuners,
        userIntents: UserIntents,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>
    ) {

        if (config.relay1State.enabled) {
            handleRelayState(
                config.relay1State,
                config,
                Port.RELAY_ONE,
                tuner,
                userIntents,
                basicSettings,
                relayStages
            )
        }
        if (config.relay2State.enabled) {
            handleRelayState(
                config.relay2State,
                config,
                Port.RELAY_TWO,
                tuner,
                userIntents,
                basicSettings,
                relayStages
            )
        }
        if (config.relay3State.enabled) {
            handleRelayState(
                config.relay3State,
                config,
                Port.RELAY_THREE,
                tuner,
                userIntents,
                basicSettings,
                relayStages
            )
        }
        if (config.relay4State.enabled) {
            handleRelayState(
                config.relay4State,
                config,
                Port.RELAY_FOUR,
                tuner,
                userIntents,
                basicSettings,
                relayStages
            )
        }
        if (config.relay5State.enabled) {
            handleRelayState(
                config.relay5State,
                config,
                Port.RELAY_FIVE,
                tuner,
                userIntents,
                basicSettings,
                relayStages
            )
        }
        if (config.relay6State.enabled) {
            handleRelayState(
                config.relay6State,
                config,
                Port.RELAY_SIX,
                tuner,
                userIntents,
                basicSettings,
                relayStages
            )
        }
        logIt( "================================")
    }

    private fun handleRelayState(
        relayState: HpuRelayState,
        config: HyperStatHpuConfiguration,
        port: Port,
        tuner: HyperStatProfileTuners,
        userIntents: UserIntents,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>
    ) {
        logIt( " $port: ${relayState.association}")
        when {
            (HyperStatAssociationUtil.isRelayAssociatedToCompressorStage(relayState)) -> {
                if (basicSettings.conditioningMode != StandaloneConditioningMode.OFF &&
                        compressorLoopOutput != 0) {
                    runRelayForCompressor(relayState, port, config, tuner, relayStages)
                } else {
                    resetPort(port)
                }
            }
            (HyperStatAssociationUtil.isHpuRelayAuxHeatingStage1(relayState)) -> {
                if (heatingLoopOutput != 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO ||
                            basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY)) {
                    
                    if (currentTemp < (userIntents.zoneHeatingTargetTemperature - tuner.auxHeating1Activate)) {
                        updateLogicalPointIdValue(logicalPointsList[port]!!, 1.0)
                        relayStages[ Pipe2RelayAssociation.AUX_HEATING_STAGE1.name] = 1
                    } else if (currentTemp >= (userIntents.zoneHeatingTargetTemperature - (tuner.auxHeating1Activate - 1))) {
                        updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
                        relayStages.remove(Pipe2RelayAssociation.AUX_HEATING_STAGE1.name)
                    } else if(hayStack.readHisValById(logicalPointsList[port]!!) == 1.0) {
                        relayStages[ Pipe2RelayAssociation.AUX_HEATING_STAGE1.name] = 1
                    }
                }else{
                    updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
                    relayStages.remove(Pipe2RelayAssociation.AUX_HEATING_STAGE1.name)
                }
            }

            (HyperStatAssociationUtil.isHpuRelayAuxHeatingStage2(relayState)) -> {
                if(heatingLoopOutput != 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO
                    || basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY)) {

                    if (currentTemp < (userIntents.zoneHeatingTargetTemperature - tuner.auxHeating2Activate)) {
                        updateLogicalPointIdValue(logicalPointsList[port]!!, 1.0)
                        relayStages[Pipe2RelayAssociation.AUX_HEATING_STAGE2.name] = 1
                    } else if (currentTemp >= (userIntents.zoneHeatingTargetTemperature - (tuner.auxHeating2Activate - 1))) {
                        updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
                        relayStages.remove(Pipe2RelayAssociation.AUX_HEATING_STAGE2.name)
                    } else if(hayStack.readHisValById(logicalPointsList[port]!!) == 1.0) {
                        relayStages[ Pipe2RelayAssociation.AUX_HEATING_STAGE2.name] = 1
                    }
                }else{
                    updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
                    relayStages.remove(Pipe2RelayAssociation.AUX_HEATING_STAGE2.name)
                }
            }
            (HyperStatAssociationUtil.isHpuRelayAssociatedToFan(relayState)) -> {
                runRelayForFanSpeed(relayState, port, config, tuner, relayStages, basicSettings)
            }
            (HyperStatAssociationUtil.isHpuRelayChangeOverCooling(relayState)) -> {
                if(basicSettings.conditioningMode == StandaloneConditioningMode.AUTO
                    || basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY) {
                    val status = if (coolingLoopOutput > 0) 1.0 else 0.0
                    updateLogicalPointIdValue(
                        logicalPointsList[port]!!,
                        status
                    )
                    if (status == 1.0) relayStages[HpuRelayAssociation.CHANGE_OVER_O_COOLING.name] = 1
                }
            }
            (HyperStatAssociationUtil.isHpuRelayChangeOverHeating(relayState)) -> {
                if( basicSettings.conditioningMode == StandaloneConditioningMode.AUTO
                    || basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY) {
                    val status = if (heatingLoopOutput > 0) 1.0 else 0.0
                    updateLogicalPointIdValue(
                        logicalPointsList[port]!!,
                        status
                    )
                    if (status == 1.0) relayStages[HpuRelayAssociation.CHANGE_OVER_B_HEATING.name] =
                        1
                }
            }

            (HyperStatAssociationUtil.isHpuRelayFanEnabled(relayState)) -> {
                doFanEnabled( curState,port, fanLoopOutput )
            }
            (HyperStatAssociationUtil.isHpuRelayOccupiedEnabled(relayState)) -> {
                doOccupiedEnabled(port)
            }
            (HyperStatAssociationUtil.isHpuRelayHumidifierEnabled(relayState)) -> {
                doHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMinInsideHumidity)
            }
            (HyperStatAssociationUtil.isHpuRelayDeHumidifierEnabled(relayState)) -> {
                doDeHumidifierOperation(port,tuner.humidityHysteresis,userIntents.targetMaxInsideHumidity)
            }

        }

    }

    private fun runFanOperationBasedOnAuxStages(
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        config: HyperStatHpuConfiguration
    ){

        if(relayOutputPoints.containsKey(HpuRelayAssociation.AUX_HEATING_STAGE1.ordinal)
            && relayOutputPoints.containsKey(HpuRelayAssociation.AUX_HEATING_STAGE2.ordinal)){
            val aux1Status = getCurrentLogicalPointStatus(relayOutputPoints[HpuRelayAssociation.AUX_HEATING_STAGE1.ordinal]!!)
            val aux2Status = getCurrentLogicalPointStatus(relayOutputPoints[HpuRelayAssociation.AUX_HEATING_STAGE2.ordinal]!!)
            if(aux2Status ==  1.0){
                operateAuxBasedOnFan(HpuRelayAssociation.AUX_HEATING_STAGE2, relayStages)
            }else{
                logIt( "aux 2  else ")
            }
            if(aux1Status ==  1.0){
                operateAuxBasedOnFan(HpuRelayAssociation.AUX_HEATING_STAGE1, relayStages)
            }else{
                logIt( "aux 1  else ")
            }
            runSpecificAnalogFanSpeed(config,analogOutStages)
        }
        else if(relayOutputPoints.containsKey(HpuRelayAssociation.AUX_HEATING_STAGE1.ordinal)
            && getCurrentLogicalPointStatus(relayOutputPoints[HpuRelayAssociation.AUX_HEATING_STAGE1.ordinal]!!) == 1.0 ){
            operateAuxBasedOnFan(HpuRelayAssociation.AUX_HEATING_STAGE1, relayStages)
            runSpecificAnalogFanSpeed(config,analogOutStages)
        }
        else if(relayOutputPoints.containsKey(HpuRelayAssociation.AUX_HEATING_STAGE2.ordinal)
            && getCurrentLogicalPointStatus(relayOutputPoints[HpuRelayAssociation.AUX_HEATING_STAGE2.ordinal]!!) == 1.0 ){
            operateAuxBasedOnFan(HpuRelayAssociation.AUX_HEATING_STAGE2, relayStages)
            runSpecificAnalogFanSpeed(config,analogOutStages)
        }

    }


    // New requirement for aux and fan operations If we do not have fan then no aux
    private fun operateAuxBasedOnFan(
        association: HpuRelayAssociation,
        relayStages: HashMap<String, Int>
    ){
        var stage = HpuRelayAssociation.AUX_HEATING_STAGE1
        var fanEnabled = true
        var fanStatusMessage : Stage? = null
        if(association == HpuRelayAssociation.AUX_HEATING_STAGE1){
            if (relayOutputPoints.containsKey(HpuRelayAssociation.FAN_MEDIUM_SPEED.ordinal)){
                stage = HpuRelayAssociation.FAN_MEDIUM_SPEED
                fanStatusMessage = Stage.FAN_2
            }else if (relayOutputPoints.containsKey(HpuRelayAssociation.FAN_HIGH_SPEED.ordinal)){
                stage = HpuRelayAssociation.FAN_HIGH_SPEED
                fanStatusMessage = Stage.FAN_3
            }else if (relayOutputPoints.containsKey(HpuRelayAssociation.FAN_LOW_SPEED.ordinal)){
                stage = HpuRelayAssociation.FAN_LOW_SPEED
                fanStatusMessage = Stage.FAN_1
            }else if (relayOutputPoints.containsKey(HpuAnalogOutAssociation.FAN_SPEED.ordinal)) {
                stage = HpuRelayAssociation.FAN_ENABLED
            } else {
                fanEnabled = false
            }
        }
        if(association == HpuRelayAssociation.AUX_HEATING_STAGE2){
            if (relayOutputPoints.containsKey(HpuRelayAssociation.FAN_HIGH_SPEED.ordinal)){
                stage = HpuRelayAssociation.FAN_HIGH_SPEED
                fanStatusMessage = Stage.FAN_3
            }else if (relayOutputPoints.containsKey(HpuRelayAssociation.FAN_MEDIUM_SPEED.ordinal)){
                stage = HpuRelayAssociation.FAN_MEDIUM_SPEED
                fanStatusMessage = Stage.FAN_2
            }else if (relayOutputPoints.containsKey(HpuRelayAssociation.FAN_LOW_SPEED.ordinal)){
                stage = HpuRelayAssociation.FAN_LOW_SPEED
                fanStatusMessage = Stage.FAN_1
            }else if (relayOutputPoints.containsKey(HpuAnalogOutAssociation.FAN_SPEED.ordinal)) {
                stage = HpuRelayAssociation.FAN_ENABLED
            } else {
                fanEnabled = false
            }
        }
        if(!fanEnabled){
            resetAux(relayStages)
        }else{
            if(stage == HpuRelayAssociation.FAN_LOW_SPEED){
                updateLogicalPointIdValue(relayOutputPoints[HpuRelayAssociation.FAN_LOW_SPEED.ordinal]!!, 1.0)
                relayStages[fanStatusMessage!!.displayName] = 1
                return
            }
            if(stage == HpuRelayAssociation.FAN_MEDIUM_SPEED){
                updateLogicalPointIdValue(relayOutputPoints[HpuRelayAssociation.FAN_MEDIUM_SPEED.ordinal]!!, 1.0)
                relayStages[fanStatusMessage!!.displayName] = 1

                if(relayOutputPoints.containsKey(HpuRelayAssociation.FAN_LOW_SPEED.ordinal)) {
                    updateLogicalPointIdValue(relayOutputPoints[HpuRelayAssociation.FAN_LOW_SPEED.ordinal]!!, 1.0)
                    relayStages[Stage.FAN_2.displayName] = 1
                }
                if(relayOutputPoints.containsKey(HpuRelayAssociation.FAN_HIGH_SPEED.ordinal)
                    && (getCurrentLogicalPointStatus(relayOutputPoints[HpuRelayAssociation.AUX_HEATING_STAGE2.ordinal]!!) == 0.0)
                ) {
                    updateLogicalPointIdValue(relayOutputPoints[HpuRelayAssociation.FAN_HIGH_SPEED.ordinal]!!, 0.0)
                    relayStages.remove(Stage.FAN_3.displayName)
                }
            }
            if(stage == HpuRelayAssociation.FAN_HIGH_SPEED){
                updateLogicalPointIdValue(relayOutputPoints[HpuRelayAssociation.FAN_HIGH_SPEED.ordinal]!!, 1.0)
                relayStages[fanStatusMessage!!.displayName] = 1

                if(relayOutputPoints.containsKey(HpuRelayAssociation.FAN_MEDIUM_SPEED.ordinal)) {
                    updateLogicalPointIdValue(relayOutputPoints[HpuRelayAssociation.FAN_MEDIUM_SPEED.ordinal]!!, 1.0)
                    relayStages[Stage.FAN_2.displayName] = 1
                }

                if(relayOutputPoints.containsKey(HpuRelayAssociation.FAN_LOW_SPEED.ordinal)) {
                    updateLogicalPointIdValue(relayOutputPoints[HpuRelayAssociation.FAN_LOW_SPEED.ordinal]!!, 1.0)
                    relayStages[Stage.FAN_1.displayName] = 1
                }
            }

        }

    }
    private fun resetAux(relayStages: HashMap<String, Int>){
        if(relayOutputPoints.containsKey(HpuRelayAssociation.AUX_HEATING_STAGE1.ordinal)) {
            resetLogicalPoint(relayOutputPoints[HpuRelayAssociation.AUX_HEATING_STAGE1.ordinal]!!)
        }
        if(relayOutputPoints.containsKey(HpuRelayAssociation.AUX_HEATING_STAGE2.ordinal)) {
            resetLogicalPoint(relayOutputPoints[HpuRelayAssociation.AUX_HEATING_STAGE2.ordinal]!!)
        }
        relayStages.remove(Pipe2RelayAssociation.AUX_HEATING_STAGE1.name)
        relayStages.remove(Pipe2RelayAssociation.AUX_HEATING_STAGE2.name)
    }

    private fun runSpecificAnalogFanSpeed(config: HyperStatHpuConfiguration,analogOutStages: HashMap<String, Int>) {
        var fanSpeed = 0.0
        if (relayOutputPoints.containsKey(HpuRelayAssociation.AUX_HEATING_STAGE2.ordinal)
            && getCurrentLogicalPointStatus(relayOutputPoints[HpuRelayAssociation.AUX_HEATING_STAGE2.ordinal]!!) == 1.0){
            fanSpeed = getPercent(config.analogOut1State,FanSpeed.HIGH)
        } else if (relayOutputPoints.containsKey(HpuRelayAssociation.AUX_HEATING_STAGE1.ordinal)
            && getCurrentLogicalPointStatus(relayOutputPoints[HpuRelayAssociation.AUX_HEATING_STAGE1.ordinal]!!) == 1.0){
            fanSpeed = getPercent(config.analogOut1State,FanSpeed.MEDIUM)
        }
        if (fanSpeed != 0.0) {
            var doWeHaveAnalog = false
            if (config.analogOut1State.enabled
                && HyperStatAssociationUtil.isHpuAnalogOutMappedToFanSpeed(config.analogOut1State)) {
                updateLogicalPointIdValue(analogOutputPoints[HpuAnalogOutAssociation.FAN_SPEED.ordinal]!!, fanSpeed)
                doWeHaveAnalog = true
            }
            if (config.analogOut2State.enabled
                && HyperStatAssociationUtil.isHpuAnalogOutMappedToFanSpeed(config.analogOut2State)) {
                updateLogicalPointIdValue(analogOutputPoints[HpuAnalogOutAssociation.FAN_SPEED.ordinal]!!, fanSpeed)
                doWeHaveAnalog = true
            }
            if (config.analogOut3State.enabled
                && HyperStatAssociationUtil.isHpuAnalogOutMappedToFanSpeed(config.analogOut3State)) {
                updateLogicalPointIdValue(analogOutputPoints[HpuAnalogOutAssociation.FAN_SPEED.ordinal]!!, fanSpeed)
                doWeHaveAnalog = true
            }

            if (doWeHaveAnalog) {
                analogOutStages[AnalogOutput.FAN_SPEED.name] = 1
            }
        }
    }

    private fun getPercent(analogOutState: HpuAnalogOutState, fanSpeed: FanSpeed):Double{
        return when(fanSpeed){
            FanSpeed.HIGH -> analogOutState.perAtFanHigh
            FanSpeed.MEDIUM -> analogOutState.perAtFanMedium
            FanSpeed.LOW -> analogOutState.perAtFanLow
            else -> 0.0
        }
    }
    private fun runRelayForCompressor(
        relayAssociation: HpuRelayState,
        whichPort: Port,
        config: HyperStatHpuConfiguration,
        tuner: HyperStatProfileTuners,
        relayStages: HashMap<String, Int>
    ) {

        when (relayAssociation.association) {

            HpuRelayAssociation.COMPRESSOR_STAGE1 -> {
                doCompressorStage1(
                    whichPort,compressorLoopOutput,tuner.relayActivationHysteresis,relayStages,getZoneMode())
            }
            HpuRelayAssociation.COMPRESSOR_STAGE2 -> {
                val highestStage = HyperStatAssociationUtil.getHighestCompressorStage(config).ordinal
                val divider = if (highestStage == 1) 50 else 33
                doCompressorStage2(
                    whichPort,compressorLoopOutput,tuner.relayActivationHysteresis,divider,relayStages,getZoneMode())
            }
            HpuRelayAssociation.COMPRESSOR_STAGE3 -> {
                doCompressorStage3(
                    whichPort,compressorLoopOutput,tuner.relayActivationHysteresis,relayStages,getZoneMode())
            }
            else -> {}
        }

        if(getCurrentPortStatus(whichPort) == 1.0)
            curState = ZoneState.COOLING

    }
    private fun runRelayForFanSpeed(
        relayAssociation: HpuRelayState,
        whichPort: Port,
        config: HyperStatHpuConfiguration,
        tuner: HyperStatProfileTuners,
        relayStages: HashMap<String, Int>,
        basicSettings: BasicSettings,
    ) {
        if (basicSettings.fanMode == StandaloneFanStage.AUTO
            && basicSettings.conditioningMode == StandaloneConditioningMode.OFF ) {
            logIt( "Cond is Off , Fan is Auto  : ")
            resetPort(whichPort)
            return
        }
        val highestStage = HyperStatAssociationUtil.getHpuHighestFanStage(config)
        val divider = if (highestStage == HpuRelayAssociation.FAN_MEDIUM_SPEED) 50 else 33
        val lowestStage = HyperStatAssociationUtil.getHpuLowestFanStage(config)

        // Check which fan speed is the lowest and set the status(Eg: If FAN_MEDIUM and FAN_HIGH are used, then FAN_MEDIUM is the lowest)
        setFanLowestFanLowStatus(false)
        setFanLowestFanMediumStatus(false)
        setFanLowestFanHighStatus(false)
        when(lowestStage) {
            HpuRelayAssociation.FAN_LOW_SPEED -> setFanLowestFanLowStatus(true)
            HpuRelayAssociation.FAN_MEDIUM_SPEED -> setFanLowestFanMediumStatus(true)
            HpuRelayAssociation.FAN_HIGH_SPEED -> setFanLowestFanHighStatus(true)
            else -> {
                // Do nothing
            }
        }

        when (relayAssociation.association) {
            HpuRelayAssociation.FAN_LOW_SPEED -> {
                doFanLowSpeed(
                    logicalPointsList[whichPort]!!,null,null, basicSettings.fanMode,
                    fanLoopOutput,tuner.relayActivationHysteresis,relayStages,divider, getDoorWindowFanOperationStatus())
            }
            HpuRelayAssociation.FAN_MEDIUM_SPEED -> {
                if(relayOutputPoints.containsKey(HpuRelayAssociation.AUX_HEATING_STAGE1.ordinal) &&
                    getCurrentLogicalPointStatus(relayOutputPoints[HpuRelayAssociation.AUX_HEATING_STAGE1.ordinal]!!) == 1.0){
                    return
                }
                doFanMediumSpeed(
                    logicalPointsList[whichPort]!!,null,basicSettings.fanMode,
                    fanLoopOutput,tuner.relayActivationHysteresis,divider,relayStages, getDoorWindowFanOperationStatus())
            }
            HpuRelayAssociation.FAN_HIGH_SPEED -> {
                if(relayOutputPoints.containsKey(HpuRelayAssociation.AUX_HEATING_STAGE2.ordinal) &&
                    getCurrentLogicalPointStatus(relayOutputPoints[HpuRelayAssociation.AUX_HEATING_STAGE2.ordinal]!!) == 1.0){
                    return
                }
                if(relayOutputPoints.containsKey(HpuRelayAssociation.AUX_HEATING_STAGE1.ordinal) &&
                    getCurrentLogicalPointStatus(relayOutputPoints[HpuRelayAssociation.AUX_HEATING_STAGE1.ordinal]!!) == 1.0){
                    return
                }
                doFanHighSpeed(
                    logicalPointsList[whichPort]!!,basicSettings.fanMode,
                    fanLoopOutput,tuner.relayActivationHysteresis,relayStages,getDoorWindowFanOperationStatus())
            }
            else -> return
        }
    }

    private fun runAnalogOutOperations(
        equip: HyperStatHpuEquip,
        config: HyperStatHpuConfiguration,
        basicSettings: BasicSettings,
        analogOutStages: HashMap<String, Int>
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
    private fun getZoneMode(): ZoneState{
        if(coolingLoopOutput > 0)
            return ZoneState.COOLING
        if(heatingLoopOutput > 0)
            return ZoneState.HEATING
        return ZoneState.TEMPDEAD
    }
    private fun handleAnalogOutState(
        analogOutState: HpuAnalogOutState,
        equip: HyperStatHpuEquip,
        config: HyperStatHpuConfiguration,
        port: Port,
        basicSettings: BasicSettings,
        analogOutStages: HashMap<String, Int>
    ) {
        // If we are in Auto Away mode we no need to Any analog Operations
        when {
            (HyperStatAssociationUtil.isHpuAnalogOutMappedToCompressorSpeed(analogOutState)) -> {
               doAnalogCompressorSpeed(port,basicSettings.conditioningMode,analogOutStages,compressorLoopOutput,getZoneMode())
            }
            (HyperStatAssociationUtil.isHpuAnalogOutMappedToFanSpeed(analogOutState)) -> {
                if(relayOutputPoints.containsKey(HpuRelayAssociation.AUX_HEATING_STAGE2.ordinal) &&
                    getCurrentLogicalPointStatus(relayOutputPoints[HpuRelayAssociation.AUX_HEATING_STAGE2.ordinal]!!) == 1.0){
                    return
                }
                if(relayOutputPoints.containsKey(HpuRelayAssociation.AUX_HEATING_STAGE1.ordinal) &&
                    getCurrentLogicalPointStatus(relayOutputPoints[HpuRelayAssociation.AUX_HEATING_STAGE1.ordinal]!!) == 1.0){
                    return
                }
                doAnalogFanAction(
                    port,
                    analogOutState.perAtFanLow.toInt(),
                    analogOutState.perAtFanMedium.toInt(),
                    analogOutState.perAtFanHigh.toInt(),
                    basicSettings.fanMode,
                    basicSettings.conditioningMode,
                    fanLoopOutput,
                    analogOutStages
                )
            }
            (HyperStatAssociationUtil.isHpuAnalogOutMappedToDcvDamper(analogOutState)) -> {
                doAnalogDCVAction(
                    port,analogOutStages,config.zoneCO2Threshold,config.zoneCO2DamperOpeningRate,isDoorOpenState(config,equip)
                )
            }
        }
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
    private fun checkFanOperationAllowedDoorWindow(equip: HyperStatHpuEquip): Boolean {
        return if(currentTemp < fetchUserIntents(equip).zoneCoolingTargetTemperature && currentTemp > fetchUserIntents(equip).zoneHeatingTargetTemperature) {
            doorWindowSensorOpenStatus &&
                    occupancyBeforeDoorWindow != Occupancy.UNOCCUPIED &&
                    occupancyBeforeDoorWindow != Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
                    occupancyBeforeDoorWindow != Occupancy.VACATION
        } else {
            doorWindowSensorOpenStatus
        }
    }

    private fun runForDoorWindowSensor(config: HyperStatHpuConfiguration, equip: HyperStatHpuEquip): Boolean {
        val isDoorOpen = isDoorOpenState(config,equip)
        logIt( " is Door Open ? $isDoorOpen")
        if (isDoorOpen) resetLoopOutputValues()
        return isDoorOpen
    }

    private fun isDoorOpenState(config: HyperStatHpuConfiguration, equip: HyperStatHpuEquip): Boolean{

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
            logIt( "TH2 Door Window sensor value : Door is $sensorValue")
            if (sensorValue.toInt() == 1) isDoorOpen = true
            th2SensorEnabled = true
        }

        if (config.analogIn1State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToDoorWindowSensor(config.analogIn1State)
        ) {
            val sensorValue = equip.hsHaystackUtil.getSensorPointValue(
                "door and window2 and logical and sensor"
            )
            logIt( "Analog In 1 Door Window sensor value : Door is $sensorValue")
            if (sensorValue.toInt() == 1) isDoorOpen = true
            analog1SensorEnabled = true
        }

        if (config.analogIn2State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToDoorWindowSensor(config.analogIn2State)
        ) {
            val sensorValue = equip.hsHaystackUtil.getSensorPointValue(
                "door and window3 and logical and sensor"
            )
            logIt( "Analog In 2 Door Window sensor value : Door is $sensorValue")
            if (sensorValue.toInt() == 1) isDoorOpen = true
            analog2SensorEnabled = true
        }

        doorWindowIsOpen(
            if (th2SensorEnabled || analog1SensorEnabled || analog2SensorEnabled) 1.0 else 0.0,
            if (isDoorOpen) 1.0 else 0.0
        )

        return isDoorOpen
    }

    private fun isDoorWindowSensorOnTh2(config: HyperStatHpuConfiguration): Boolean {
        return config.thermistorIn2State.enabled && config.thermistorIn2State.association.equals(
            Th2InAssociation.DOOR_WINDOW_SENSOR)
    }

    private fun runForKeyCardSensor(config: HyperStatHpuConfiguration, equip: HyperStatHpuEquip) {

        var analog1KeycardEnabled = false
        var analog2KeycardEnabled = false

        var analog1Sensor = 0.0
        var analog2Sensor = 0.0

        if (config.analogIn1State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToKeyCardSensor(config.analogIn1State)
        ) {
            analog1KeycardEnabled = true
            analog1Sensor = equip.hsHaystackUtil.getSensorPointValue(
                "keycard and sensor and logical"
            )
        }

        if (config.analogIn2State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToKeyCardSensor(config.analogIn2State)
        ) {
            analog2KeycardEnabled = true
            analog2Sensor = equip.hsHaystackUtil.getSensorPointValue(
                "keycard2 and sensor and logical"
            )
        }

        logIt( "Keycard Enable Value "+  if (analog1KeycardEnabled || analog2KeycardEnabled) 1.0 else 0.0)
        logIt( "Keycard sensor Value "+ if (analog1Sensor > 0 || analog2Sensor > 0) 1.0 else 0.0)

        keyCardIsInSlot(
            if (analog1KeycardEnabled || analog2KeycardEnabled) 1.0 else 0.0,
            if (analog1Sensor > 0 || analog2Sensor > 0) 1.0 else 0.0
        )
    }

    private fun resetLoopOutputValues() {
        logIt( "Resetting all the loop output values: ")
        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        compressorLoopOutput = 0
    }

    private fun resetAllLogicalPointValues(equip: HyperStatHpuEquip) {

        equip.hsHaystackUtil.updateAllLoopOutput(0,0,0,true,0)

        resetAllLogicalPointValues()

        HyperStatUserIntentHandler.updateHyperStatStatus(
            equipId = equip.equipRef!!,
            portStages = HashMap(),
            analogOutStages = HashMap(),
            temperatureState = ZoneTempState.TEMP_DEAD
        )

    }


    private fun fetchBasicSettings(equip: HyperStatHpuEquip) =

        BasicSettings(
            conditioningMode = StandaloneConditioningMode.values()[equip.hsHaystackUtil.getCurrentConditioningMode().toInt()],
            fanMode = StandaloneFanStage.values()[equip.hsHaystackUtil.getCurrentFanMode().toInt()]
        )

    private fun fetchUserIntents(equip: HyperStatHpuEquip): UserIntents {

        return UserIntents(
            currentTemp = equip.getCurrentTemp(),
            zoneCoolingTargetTemperature = equip.hsHaystackUtil.getDesiredTempCooling(),
            zoneHeatingTargetTemperature = equip.hsHaystackUtil.getDesiredTempHeating(),
            targetMinInsideHumidity = equip.hsHaystackUtil.getTargetMinInsideHumidity(),
            targetMaxInsideHumidity = equip.hsHaystackUtil.getTargetMaxInsideHumidity(),
        )
    }

    private fun fetchHyperStatTuners(equip: HyperStatHpuEquip): HyperStatProfileTuners {

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
        hsTuners.auxHeating1Activate = TunerUtil.readTunerValByQuery("tuner and heating and aux and stage1 and equipRef == \"${equip.equipRef}\"")
        hsTuners.auxHeating2Activate = TunerUtil.readTunerValByQuery("tuner and heating and aux and stage2 and equipRef == \"${equip.equipRef}\"")
        return hsTuners
        
    }
    
    @JsonIgnore
    override fun getNodeAddresses(): Set<Short?> {
        return hpuDeviceMap.keys
    }
    @JsonIgnore
    override fun getCurrentTemp(): Double {
        for (nodeAddress in hpuDeviceMap.keys) {
            return hpuDeviceMap[nodeAddress]!!.getCurrentTemp()
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
        for (nodeAddress in hpuDeviceMap.keys) {
            if (hpuDeviceMap[nodeAddress] == null) {
                continue
            }
            if (hpuDeviceMap[nodeAddress]!!.getCurrentTemp() > 0) {
                tempTotal += hpuDeviceMap[nodeAddress]!!.getCurrentTemp()
                nodeCount++
            }
        }
        return if (nodeCount == 0) 0.0 else tempTotal / nodeCount
    }

    private fun handleDeadZone(equip: HyperStatHpuEquip) {

        logIt( "updatePointsForEquip: Dead Zone ")
        state = ZoneState.TEMPDEAD
        resetAllLogicalPointValues(equip)
        equip.hsHaystackUtil.setProfilePoint("operating and mode", state.ordinal.toDouble())
        if (equip.hsHaystackUtil.getEquipStatus() != state.ordinal.toDouble())
            equip.hsHaystackUtil.setEquipStatus(state.ordinal.toDouble())

        val curStatus = equip.hsHaystackUtil.getEquipLiveStatus()
        if (curStatus != "Zone Temp Dead") {
            equip.hsHaystackUtil.writeDefaultVal("status and message and writable", "Zone Temp Dead")
        }
        equip.haystack.writeHisValByQuery(
            "point and not ota and status and his and group == \"${equip.node}\"",
            ZoneState.TEMPDEAD.ordinal.toDouble()
        )
    }
    private fun handleRFDead(equip: HyperStatHpuEquip) {
        logIt("RF Signal is Dead ${equip.node}")
        state = ZoneState.RFDEAD
        equip.hsHaystackUtil.setProfilePoint("operating and mode", state.ordinal.toDouble())
        if (equip.hsHaystackUtil.getEquipStatus() != state.ordinal.toDouble()) {
            equip.hsHaystackUtil.setEquipStatus(state.ordinal.toDouble())
        }
        val curStatus = equip.hsHaystackUtil.getEquipLiveStatus()
        if (curStatus != RFDead) {
            equip.hsHaystackUtil.writeDefaultVal("status and message and writable", RFDead)
        }
        equip.haystack.writeHisValByQuery(
            "point and not ota and status and his and group == \"${equip.node}\"",
            ZoneState.RFDEAD.ordinal.toDouble()
        )
    }
    override fun getHyperStatEquip(node: Short): HyperStatEquip {
        return hpuDeviceMap[node] as HyperStatHpuEquip
    }

    override fun addNewEquip(
        node: Short,
        room: String,
        floor: String,
        baseConfig: BaseProfileConfiguration
    ) {
        val equip = addEquip(node)
        val configuration = equip.initializePoints(baseConfig as HyperStatHpuConfiguration, room, floor, node)
        hsHaystackUtil = equip.hsHaystackUtil
        return configuration
    }

    fun addEquip(node: Short): HyperStatEquip {
        val equip = HyperStatHpuEquip(node)
        equip.initEquipReference(node)
        hpuDeviceMap[node] = equip
        return equip
    }

    override fun getProfileType() = ProfileType.HYPERSTAT_HEAT_PUMP_UNIT

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        val equip = hpuDeviceMap[address]
        return equip?.getConfiguration() as T
    }
    override fun getEquip(): Equip? {
        for (nodeAddress in hpuDeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    private fun dumpOutput() {
        relayOutputPoints.forEach { (i, s) ->
            logIt(" ${HpuRelayAssociation.values()[i].name} : ${getCurrentLogicalPointStatus(s)}   : $s")
        }
        analogOutputPoints.forEach { (i, s) ->
            logIt(" ${HpuAnalogOutAssociation.values()[i].name} : ${getCurrentLogicalPointStatus(s)}   : $s")
        }
    }
    /**
     * Function just to print logs
     */
    private fun logIt(msg: String){
        CcuLog.i(L.TAG_CCU_HSHPU,msg)
    }
}