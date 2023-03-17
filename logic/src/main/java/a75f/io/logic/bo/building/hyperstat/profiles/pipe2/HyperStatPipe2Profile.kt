package a75f.io.logic.bo.building.hyperstat.profiles.pipe2

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
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
import a75f.io.logic.bo.building.hyperstat.common.*
import a75f.io.logic.bo.building.hyperstat.profiles.HyperStatFanCoilUnit
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.jobs.HyperStatUserIntentHandler
import a75f.io.logic.tuners.TunerUtil
import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnore


/**
 * Created by Manjunath K on 01-08-2022.
 */


class HyperStatPipe2Profile : HyperStatFanCoilUnit() {

    private val pipe2DeviceMap: MutableMap<Short, HyperStatPipe2Equip> = mutableMapOf()
    private val hyperStatLoopController = HyperstatLoopController()
    private var supplyWaterTempTh2 = 0.0
    private var heatingThreshold = 85.0
    private var coolingThreshold = 65.0

    private var coolingLoopOutput = 0
    private var heatingLoopOutput = 0
    private var fanLoopOutput = 0
    lateinit var curState: ZoneState


    private var analogOutputPoints: HashMap<Int, String> = HashMap()
    private var relayOutputPoints: HashMap<Int, String> = HashMap()

    override fun getHyperStatEquip(node: Short): HyperStatEquip {
        return pipe2DeviceMap[node] as HyperStatPipe2Equip
    }

    override fun addNewEquip(
        node: Short,
        room: String,
        floor: String,
        baseConfig: BaseProfileConfiguration
    ) {
        val equip = addEquip(node)
        val configuration =
            equip.initializePoints(baseConfig as HyperStatPipe2Configuration, room, floor, node)
        hsHaystackUtil = equip.hsHaystackUtil
        return configuration
    }

    override fun updateZonePoints() {
        pipe2DeviceMap.forEach { (_, equip) ->
            logIt( "Process Pipe2: equipRef =  ${equip.nodeAddress}")
            processHyperStatPipeProfile(equip)
        }
    }

    override fun getProfileType() = ProfileType.HYPERSTAT_TWO_PIPE_FCU

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        val equip = pipe2DeviceMap[address]
        return equip?.getConfiguration() as T
    }

    fun addEquip(node: Short): HyperStatEquip {
        val equip = HyperStatPipe2Equip(node)
        equip.initEquipReference(node)
        pipe2DeviceMap[node] = equip
        return equip
    }

    override fun getEquip(): Equip? {
        for (nodeAddress in pipe2DeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    override fun getNodeAddresses(): MutableSet<Short> {
        return pipe2DeviceMap.keys
    }

    fun processHyperStatPipeProfile(equip: HyperStatPipe2Equip) {

        if (Globals.getInstance().isTestMode) {
            logIt( "Test mode is on: ${equip.equipRef}")
                return
        }

        if (mInterface != null) mInterface.refreshView()

        val relayStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()

        logicalPointsList = equip.getLogicalPointList()
        relayOutputPoints = equip.getRelayOutputPoints()
        analogOutputPoints = equip.getAnalogOutputPoints()
        hsHaystackUtil = HSHaystackUtil(equip.equipRef!!, CCUHsApi.getInstance())

        if (isZoneDead) {
            handleDeadZone(equip)
            return
        }

        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode

        val config = equip.getConfiguration()
        val hyperStatTuners = fetchHyperStatTuners(equip)
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = updateAverageTemperature(equip, userIntents)

        val fanModeSaved = FanModeCacheStorage().getFanModeFromCache(equip.equipRef!!)
        val actualFanMode = HSHaystackUtil.getPipe2ActualFanMode(equip.node.toString(), fanModeSaved)
        val basicSettings = fetchBasicSettings(equip)
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef!!, fanModeSaved, actualFanMode, basicSettings)
        basicSettings.fanMode = updatedFanMode

        heatingThreshold = hyperStatTuners.heatingThreshold
        coolingThreshold = hyperStatTuners.coolingThreshold

        hyperStatLoopController.initialise(tuners = hyperStatTuners)
        hyperStatLoopController.dumpLogs()
        handleChangeOfDirection(userIntents)

        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        evaluateLoopOutputs(userIntents, basicSettings, hyperStatTuners)

        supplyWaterTempTh2 = getSupplyWaterTemp(equip.equipRef!!)
        equip.hsHaystackUtil.updateOccupancyDetection()

        equip.hsHaystackUtil.updateAllLoopOutput (
            coolingLoopOutput, heatingLoopOutput,
            fanLoopOutput,false ,0
        )
        val currentOperatingMode = equip.hsHaystackUtil.getOccupancyModePointValue().toInt()

        logIt(
            "Analog Fan speed multiplier  ${hyperStatTuners.analogFanSpeedMultiplier} \n" +
            "Current Working mode : ${Occupancy.values()[currentOperatingMode]} \n" +
            "Current Temp : $currentTemp \n" +
            "Desired Heating: ${userIntents.zoneHeatingTargetTemperature} \n" +
            "Desired Cooling: ${userIntents.zoneCoolingTargetTemperature} \n" +
            "Heating Loop Output: $heatingLoopOutput \n" +
            "Cooling Loop Output:: $coolingLoopOutput \n" +
            "Fan Loop Output:: $fanLoopOutput \n" +
            "supplyWaterTempTh2 : $supplyWaterTempTh2 \n" +
            "Fan Mode : ${basicSettings.fanMode} Conditioning Mode ${basicSettings.conditioningMode} \n" +
            "heatingThreshold: $heatingThreshold  coolingThreshold : $coolingThreshold \n"+
            "waterValveSamplingOnTime: ${hyperStatTuners.waterValveSamplingOnTime}  waterValveSamplingWaitTime : ${hyperStatTuners.waterValveSamplingWaitTime} \n"+
            "waterValveSamplingDuringLoopDeadbandOnTime: ${hyperStatTuners.waterValveSamplingDuringLoopDeadbandOnTime}  waterValveSamplingDuringLoopDeadbandWaitTime : ${hyperStatTuners.waterValveSamplingDuringLoopDeadbandWaitTime} \n",
        )

        runRelayOperations(equip, config, hyperStatTuners, userIntents, basicSettings, relayStages,analogOutStages)
        runAnalogOutOperations(equip, config, basicSettings, analogOutStages,userIntents)
        runAlgorithm(equip, basicSettings, hyperStatTuners, relayStages,analogOutStages, config,userIntents)
        runForDoorWindowSensor(config, equip,analogOutStages,relayStages)
        runForKeycardSensor(config, equip)

        setOperatingMode(currentTemp,averageDesiredTemp,basicSettings,equip)

        if (equip.hsHaystackUtil.getStatus() != curState.ordinal.toDouble()){
                equip.hsHaystackUtil.setStatus(curState.ordinal.toDouble())
        }
        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY

        HyperStatUserIntentHandler.updateHyperStatStatus(
                equip.equipRef!!, relayStages, analogOutStages, temperatureState
        )
        dumpOutput()
    }

    private fun evaluateLoopOutputs(userIntents: UserIntents, basicSettings: BasicSettings, hyperStatTuners: HyperStatProfileTuners){
        when (state) {
            //Update coolingLoop when the zone is in cooling or it was in cooling and no change over happened yet.
            ZoneState.COOLING -> coolingLoopOutput =
                hyperStatLoopController.calculateCoolingLoopOutput(
                    currentTemp, userIntents.zoneCoolingTargetTemperature
                ).toInt().coerceAtLeast(0)

            //Update heatingLoop when the zone is in heating or it was in heating and no change over happened yet.
            ZoneState.HEATING -> heatingLoopOutput =
                hyperStatLoopController.calculateHeatingLoopOutput(
                    userIntents.zoneHeatingTargetTemperature, currentTemp
                ).toInt().coerceAtLeast(0)

            else -> logIt( " Zone is in deadband")
        }

        if (coolingLoopOutput > 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY
                    || basicSettings.conditioningMode == StandaloneConditioningMode.AUTO)) {
            fanLoopOutput = ((coolingLoopOutput * hyperStatTuners.analogFanSpeedMultiplier).coerceAtMost(100.0).toInt())
        }
        else if (heatingLoopOutput > 0 && ((basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY && supplyWaterTempTh2 > coolingThreshold )
                    || (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO && supplyWaterTempTh2 > coolingThreshold))
        ) {
            fanLoopOutput = (heatingLoopOutput * hyperStatTuners.analogFanSpeedMultiplier).coerceAtMost(100.0).toInt()
        }
    }

    private fun handleChangeOfDirection(userIntents: UserIntents){
        if (currentTemp > userIntents.zoneCoolingTargetTemperature && state != ZoneState.COOLING) {
            hyperStatLoopController.resetCoolingControl()
            state = ZoneState.COOLING
            logIt( "Resetting cooling")
        } else if (currentTemp < userIntents.zoneHeatingTargetTemperature && state != ZoneState.HEATING) {
            hyperStatLoopController.resetHeatingControl()
            state = ZoneState.HEATING
            logIt( "Resetting heating")
        }
    }

    private fun updateAverageTemperature(equip: HyperStatPipe2Equip, userIntents: UserIntents): Double{
        val averageDesiredTemp = (userIntents.zoneCoolingTargetTemperature + userIntents.zoneHeatingTargetTemperature) / 2.0
        if (averageDesiredTemp != equip.hsHaystackUtil.getDesiredTemp()) {
            equip.hsHaystackUtil.setDesiredTemp(averageDesiredTemp)
        }
        return averageDesiredTemp
    }

    private fun fetchBasicSettings(equip: HyperStatPipe2Equip) =

        BasicSettings(
            conditioningMode = StandaloneConditioningMode.values()[equip.hsHaystackUtil.getCurrentConditioningMode()
                .toInt()],
            fanMode = StandaloneFanStage.values()[equip.hsHaystackUtil.getCurrentFanMode().toInt()]
        )


    private fun fetchHyperStatTuners(equip: HyperStatPipe2Equip): HyperStatProfileTuners {

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
        hsTuners.relayActivationHysteresis = TunerUtil.getHysteresisPoint(
            "relay and activation", equip.equipRef!!
        ).toInt()
        hsTuners.analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery(
            "analog and fan and speed and multiplier", equip.equipRef!!
        )
        hsTuners.humidityHysteresis =
            TunerUtil.getHysteresisPoint("humidity", equip.equipRef!!).toInt()

        hsTuners.heatingThreshold = TunerUtil.readTunerValByQuery("tuner and heating and threshold and equipRef == \"${equip.equipRef}\"")

        hsTuners.coolingThreshold = TunerUtil.readTunerValByQuery("tuner and cooling and threshold and equipRef == \"${equip.equipRef}\"")

        hsTuners.auxHeating1Activate = TunerUtil.readTunerValByQuery("tuner and heating and aux and stage1 and equipRef == \"${equip.equipRef}\"")

        hsTuners.auxHeating2Activate = TunerUtil.readTunerValByQuery("tuner and heating and aux and stage2 and equipRef == \"${equip.equipRef}\"")

        hsTuners.waterValveSamplingOnTime = TunerUtil.readTunerValByQuery("tuner and samplingrate and water and on and time and not loop and equipRef == \"${equip.equipRef}\"")
            .toInt()

        hsTuners.waterValveSamplingWaitTime = TunerUtil.readTunerValByQuery("tuner and samplingrate and water and wait and time and not loop and equipRef == \"${equip.equipRef}\"")
            .toInt()

        hsTuners.waterValveSamplingDuringLoopDeadbandOnTime = TunerUtil.readTunerValByQuery("tuner and samplingrate and loop and on and time and equipRef == \"${equip.equipRef}\"")
            .toInt()

        hsTuners.waterValveSamplingDuringLoopDeadbandWaitTime =  TunerUtil.readTunerValByQuery("tuner and samplingrate and loop and wait and time and equipRef == \"${equip.equipRef}\"")
            .toInt()

        return hsTuners
    }

    private fun handleDeadZone(equip: HyperStatPipe2Equip) {
        logIt( "Zone is Dead ${equip.equipRef}")
        state = ZoneState.TEMPDEAD
        resetAllLogicalPointValues()
        equip.hsHaystackUtil.setProfilePoint("operating and mode", 0.0)
        if (equip.hsHaystackUtil.getEquipStatus() != state.ordinal.toDouble())
            equip.hsHaystackUtil.setEquipStatus(state.ordinal.toDouble())

        val curStatus = equip.hsHaystackUtil.getEquipLiveStatus()
        if (curStatus != "Zone Temp Dead") {
            equip.hsHaystackUtil.writeDefaultVal(
                "status and message and writable",
                "Zone Temp Dead"
            )
        }
        equip.haystack.writeHisValByQuery(
            "point and status and his and group == \"${equip.node}\"",
            ZoneState.TEMPDEAD.ordinal.toDouble()
        )
    }

    private fun fetchUserIntents(equip: HyperStatPipe2Equip): UserIntents {

        return UserIntents(
            currentTemp = equip.getCurrentTemp(),
            zoneCoolingTargetTemperature = equip.hsHaystackUtil.getDesiredTempCooling(),
            zoneHeatingTargetTemperature = equip.hsHaystackUtil.getDesiredTempHeating(),
            targetMinInsideHumidity = equip.hsHaystackUtil.getTargetMinInsideHumidity(),
            targetMaxInsideHumidity = equip.hsHaystackUtil.getTargetMaxInsideHumidity(),
        )
    }


    private fun runAlgorithm(
        equip: HyperStatPipe2Equip,
        basicSettings: BasicSettings,
        tuner: HyperStatProfileTuners,
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>,
        configuration: HyperStatPipe2Configuration,
        userIntents: UserIntents
    ) {
        // Run water sampling
         processForWaterSampling(equip, tuner, configuration, relayStages)

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
        configuration: HyperStatPipe2Configuration,
        userIntents: UserIntents,
        analogOutStages: HashMap<String, Int>,
        equip: HyperStatPipe2Equip
    ) {
        logIt( "doCoolOnly: mode ")

        if (basicSettings.fanMode == StandaloneFanStage.OFF || supplyWaterTempTh2 > heatingThreshold) {
            logIt( "Resetting WATER_VALVE to OFF")
            resetWaterValue(relayStages,equip)
        }

        if (basicSettings.fanMode != StandaloneFanStage.OFF) {
            if (relayOutputPoints.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal)
                && relayOutputPoints.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal)
            ) {

                if (basicSettings.fanMode == StandaloneFanStage.AUTO) {
                    if (getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal]!!) == 1.0
                        && getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal]!!) == 0.0
                    ) {
                        resetFan(relayStages,analogOutStages,basicSettings)
                        operateAuxBasedOnFan(Pipe2RelayAssociation.AUX_HEATING_STAGE1,relayStages)
                        runSpecificAnalogFanSpeed(configuration,FanSpeed.MEDIUM,analogOutStages)
                        return
                    }
                    if (getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal]!!) == 1.0) {
                        resetFan(relayStages,analogOutStages,basicSettings)
                        operateAuxBasedOnFan(Pipe2RelayAssociation.AUX_HEATING_STAGE2,relayStages)
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
            else if (relayOutputPoints.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal)
                && (getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal]!!) == 1.0)){
                resetFan(relayStages,analogOutStages,basicSettings)
                operateAuxBasedOnFan(Pipe2RelayAssociation.AUX_HEATING_STAGE1,relayStages)
                runSpecificAnalogFanSpeed(configuration,FanSpeed.MEDIUM,analogOutStages)
            }
            else if (relayOutputPoints.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal)
                && (getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal]!!) == 1.0)){
                resetFan(relayStages,analogOutStages,basicSettings)
                operateAuxBasedOnFan(Pipe2RelayAssociation.AUX_HEATING_STAGE2,relayStages)
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
        configuration: HyperStatPipe2Configuration,
        userIntents: UserIntents,
        analogOutStages: HashMap<String, Int>,
        equip: HyperStatPipe2Equip
    ) {
        logIt( "doHeatOnly: mode ")

        // b. Deactivate Water Valve associated relay, if it is enabled and the fan speed is off.
        if (basicSettings.fanMode == StandaloneFanStage.OFF || supplyWaterTempTh2 < coolingThreshold) {
            resetWaterValue(relayStages,equip)
        }

        if (basicSettings.fanMode != StandaloneFanStage.OFF) {
            if (relayOutputPoints.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal)
                && relayOutputPoints.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal)
            ) {
                if (basicSettings.fanMode == StandaloneFanStage.AUTO) {
                    if (getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal]!!) == 1.0
                        && getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal]!!) == 0.0
                    ) {
                        resetFan(relayStages,analogOutStages,basicSettings)
                        operateAuxBasedOnFan(Pipe2RelayAssociation.AUX_HEATING_STAGE1,relayStages)
                        runSpecificAnalogFanSpeed(configuration,FanSpeed.MEDIUM,analogOutStages)
                        return
                    }
                    if (getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal]!!) == 1.0) {
                        resetFan(relayStages,analogOutStages,basicSettings)
                        operateAuxBasedOnFan(Pipe2RelayAssociation.AUX_HEATING_STAGE2,relayStages)
                        runSpecificAnalogFanSpeed(configuration,FanSpeed.HIGH,analogOutStages)
                    } else {
                        if(heatingLoopOutput > 0) {
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
            else if (relayOutputPoints.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal)
                && (getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal]!!) == 1.0)){
                resetFan(relayStages,analogOutStages,basicSettings)
                operateAuxBasedOnFan(Pipe2RelayAssociation.AUX_HEATING_STAGE1,relayStages)
                runSpecificAnalogFanSpeed(configuration,FanSpeed.MEDIUM,analogOutStages)
            }
            else if (relayOutputPoints.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal)
                && (getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal]!!) == 1.0)){
                resetFan(relayStages,analogOutStages,basicSettings)
                operateAuxBasedOnFan(Pipe2RelayAssociation.AUX_HEATING_STAGE2,relayStages)
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
        association: Pipe2RelayAssociation,
        relayStages: HashMap<String, Int>
    ){
        var stage = Pipe2RelayAssociation.AUX_HEATING_STAGE1
        var state = 0
        var fanStatusMessage : Stage? = null
        if(association == Pipe2RelayAssociation.AUX_HEATING_STAGE1){
            if (relayOutputPoints.containsKey(Pipe2RelayAssociation.FAN_MEDIUM_SPEED.ordinal)){
                stage = Pipe2RelayAssociation.FAN_MEDIUM_SPEED
                state = 1
                fanStatusMessage = Stage.FAN_2
            }else if (relayOutputPoints.containsKey(Pipe2RelayAssociation.FAN_HIGH_SPEED.ordinal)){
                stage = Pipe2RelayAssociation.FAN_HIGH_SPEED
                state = 1
                fanStatusMessage = Stage.FAN_3
            }else if (relayOutputPoints.containsKey(Pipe2RelayAssociation.FAN_LOW_SPEED.ordinal)){
                stage = Pipe2RelayAssociation.FAN_LOW_SPEED
                state = 1
                fanStatusMessage = Stage.FAN_1
            }else if (analogOutputPoints.containsKey(Pipe2AnalogOutAssociation.FAN_SPEED.ordinal)) {
                stage = Pipe2RelayAssociation.FAN_ENABLED
                state = 1
            }
        }
        if(association == Pipe2RelayAssociation.AUX_HEATING_STAGE2){
            if (relayOutputPoints.containsKey(Pipe2RelayAssociation.FAN_HIGH_SPEED.ordinal)){
                stage = Pipe2RelayAssociation.FAN_HIGH_SPEED
                state = 1
                fanStatusMessage = Stage.FAN_3
            }else if (relayOutputPoints.containsKey(Pipe2RelayAssociation.FAN_MEDIUM_SPEED.ordinal)){
                stage = Pipe2RelayAssociation.FAN_MEDIUM_SPEED
                state = 1
                fanStatusMessage = Stage.FAN_2
            }else if (relayOutputPoints.containsKey(Pipe2RelayAssociation.FAN_LOW_SPEED.ordinal)){
                stage = Pipe2RelayAssociation.FAN_LOW_SPEED
                state = 1
                fanStatusMessage = Stage.FAN_1
            }else if (analogOutputPoints.containsKey(Pipe2AnalogOutAssociation.FAN_SPEED.ordinal)) {
                stage = Pipe2RelayAssociation.FAN_ENABLED
                state = 1
            }
        }
        if(state == 0){
            resetAux(relayStages)
        }else{
            if(stage != Pipe2RelayAssociation.FAN_ENABLED){
                updateLogicalPointIdValue(relayOutputPoints[stage.ordinal]!!, 1.0)
                relayStages[fanStatusMessage!!.displayName] = 1
            }

        }

    }


    private fun resetWaterValue(relayStages: HashMap<String, Int>, equip: HyperStatPipe2Equip){
        if(equip.waterSamplingStartTime == 0L) {
            if (relayOutputPoints.containsKey(Pipe2RelayAssociation.WATER_VALVE.ordinal)) {
                resetLogicalPoint(relayOutputPoints[Pipe2RelayAssociation.WATER_VALVE.ordinal]!!)
            }
            if (analogOutputPoints.containsKey(Pipe2AnalogOutAssociation.WATER_VALVE.ordinal)) {
                resetLogicalPoint(analogOutputPoints[Pipe2AnalogOutAssociation.WATER_VALVE.ordinal]!!)
            }
            relayStages.remove(AnalogOutput.WATER_VALVE.name)
            logIt( "Resetting WATER_VALVE to OFF")
        }
    }
    private fun resetAux(relayStages: HashMap<String, Int>){
        if(relayOutputPoints.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal)) {
            resetLogicalPoint(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal]!!)
        }
        if(relayOutputPoints.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal)) {
            resetLogicalPoint(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal]!!)
        }
        relayStages.remove(Pipe2RelayAssociation.AUX_HEATING_STAGE1.name)
        relayStages.remove(Pipe2RelayAssociation.AUX_HEATING_STAGE2.name)

    }

    private fun processForWaterSampling(
        equip: HyperStatPipe2Equip,
        tuner: HyperStatProfileTuners,
        config: HyperStatPipe2Configuration,
        relayStages: HashMap<String, Int>,
    ) {
        if(!HyperStatAssociationUtil.isAnyRelayAssociatedToWaterValve(config)
            && !HyperStatAssociationUtil.isAnyPipe2AnalogAssociatedToWaterValve(config))
        {
            logIt( "No mapping for water value")
            return
        }
        logIt( "waterSamplingStarted Time "+ equip.waterSamplingStartTime)

        val waitTimeToDoSampling: Int
        val onTimeToDoSampling: Int
        if (supplyWaterTempTh2 in coolingThreshold..heatingThreshold) {
            waitTimeToDoSampling =  tuner.waterValveSamplingDuringLoopDeadbandWaitTime
            onTimeToDoSampling = tuner.waterValveSamplingDuringLoopDeadbandOnTime
        } else{
            waitTimeToDoSampling = tuner.waterValveSamplingWaitTime
            onTimeToDoSampling = tuner.waterValveSamplingOnTime
        }

        // added on 05-12-2022 If either one of the tuner value is 0 then we will not do water sampling
        if(waitTimeToDoSampling == 0 || onTimeToDoSampling == 0){
            logIt( "No water sampling, because tuner value is zero!")
            return
        }
        logIt( "waitTimeToDoSampling:  $waitTimeToDoSampling onTimeToDoSampling: $onTimeToDoSampling")
        logIt( ":: ${System.currentTimeMillis()}: ${equip.lastWaterValveTurnedOnTime}")
        if (equip.waterSamplingStartTime == 0L) {
            val minutes = milliToMin(System.currentTimeMillis() - equip.lastWaterValveTurnedOnTime)
            logIt( "sampling will start in : ${waitTimeToDoSampling-minutes} current : $minutes")
            if(minutes >= waitTimeToDoSampling){
                doWaterSampling(equip, relayStages)
            }
        }else{
            val samplingSinceFrom =   milliToMin(System.currentTimeMillis() - equip.waterSamplingStartTime )
            logIt( "Water sampling is running since from $samplingSinceFrom minutes")
            if(samplingSinceFrom >= onTimeToDoSampling){
                equip.waterSamplingStartTime = 0
                equip.lastWaterValveTurnedOnTime = System.currentTimeMillis()
                resetWaterValue(relayStages,equip)
                logIt( "Resetting WATER_VALVE to OFF")
            }else{
                relayStages[AnalogOutput.WATER_VALVE.name] = 1
            }
        }

    }

    private fun doWaterSampling(
        equip: HyperStatPipe2Equip,
        relayStages: HashMap<String, Int>,
    ) {
        equip.waterSamplingStartTime = System.currentTimeMillis()
        updateLogicalPointIdValue(relayOutputPoints[Pipe2RelayAssociation.WATER_VALVE.ordinal],1.0)
        updateLogicalPointIdValue(analogOutputPoints[Pipe2AnalogOutAssociation.WATER_VALVE.ordinal],100.0)
        relayStages[AnalogOutput.WATER_VALVE.name] = 1
        logIt( "Turned ON water valve ")
    }


    private fun dumpOutput() {
        relayOutputPoints.forEach { (i, s) ->
            logIt(" ${Pipe2RelayAssociation.values()[i].name} : ${getCurrentLogicalPointStatus(s)}   : $s")
        }
        analogOutputPoints.forEach { (i, s) ->
            logIt(" ${Pipe2AnalogOutAssociation.values()[i].name} : ${getCurrentLogicalPointStatus(s)}   : $s")
        }
    }

    private fun doFanOperation(
        tuner: HyperStatProfileTuners,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>,
        configuration: HyperStatPipe2Configuration,
        userIntents: UserIntents,
        analogOutStages: HashMap<String, Int>,
        equip: HyperStatPipe2Equip
    ) {
      logIt(" Fan operation is running")
        if( basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
            if (basicSettings.fanMode != StandaloneFanStage.AUTO) {
                runFanHigh(tuner, basicSettings, relayStages)
                runFanMedium(tuner, basicSettings, relayStages, configuration)
                runFanLow(tuner, basicSettings, relayStages, configuration)
                runAnalogFanSpeed(configuration,userIntents,analogOutStages,basicSettings)
            } else {
                resetFan(relayStages,analogOutStages,basicSettings)
            }
        } else {

            if ( basicSettings.fanMode == StandaloneFanStage.AUTO
                && supplyWaterTempTh2  > heatingThreshold
                && supplyWaterTempTh2 in coolingThreshold .. heatingThreshold
                && currentTemp > userIntents.zoneCoolingTargetTemperature) {
                resetFan(relayStages,analogOutStages,basicSettings)
            } else {
                if ( basicSettings.fanMode != StandaloneFanStage.AUTO && basicSettings.fanMode != StandaloneFanStage.OFF ) {
                    runFanHigh(tuner, basicSettings, relayStages)
                    runFanMedium(tuner, basicSettings, relayStages, configuration)
                    runFanLow(tuner, basicSettings, relayStages, configuration)
                    runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
                    return
                }
                if (equip.waterSamplingStartTime == 0L && (relayOutputPoints.containsKey(Pipe2RelayAssociation.WATER_VALVE.ordinal) &&
                            getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.WATER_VALVE.ordinal]!!) == 1.0)) {

                    runFanHigh(tuner, basicSettings, relayStages)
                    runFanMedium(tuner, basicSettings, relayStages, configuration)
                    runFanLow(tuner, basicSettings, relayStages, configuration)
                    runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
                }
                else if(equip.waterSamplingStartTime == 0L &&  analogOutputPoints.containsKey(Pipe2AnalogOutAssociation.WATER_VALVE.ordinal) &&
                    getCurrentLogicalPointStatus(analogOutputPoints[Pipe2AnalogOutAssociation.WATER_VALVE.ordinal]!!) > 1.0){
                    runFanHigh(tuner, basicSettings, relayStages)
                    runFanMedium(tuner, basicSettings, relayStages, configuration)
                    runFanLow(tuner, basicSettings, relayStages, configuration)
                    runAnalogFanSpeed(configuration, userIntents, analogOutStages, basicSettings)
                }
                else if (relayOutputPoints.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal)
                    && getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal]!!) == 1.0){
                    runFanHigh(tuner, basicSettings, relayStages)
                    runFanMedium(tuner, basicSettings, relayStages, configuration)
                    runFanLow(tuner, basicSettings, relayStages, configuration)
                    runAnalogFanSpeed(
                        configuration,
                        userIntents,
                        analogOutStages,
                        basicSettings
                    )
                }
                else if (relayOutputPoints.containsKey(Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal)
                    && getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal]!!) == 1.0) {
                    runFanHigh(tuner, basicSettings, relayStages)
                    runFanMedium(tuner, basicSettings, relayStages, configuration)
                    runFanLow(tuner, basicSettings, relayStages, configuration)
                    runAnalogFanSpeed(
                        configuration,
                        userIntents,
                        analogOutStages,
                        basicSettings
                    )
                } else {
                    resetFan(relayStages,analogOutStages,basicSettings)
                }
            }
        }
    }

    // Do fan low speed
    private fun runFanLow(
        tuner: HyperStatProfileTuners,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>,
        configuration: HyperStatPipe2Configuration
    ) {
        if (relayOutputPoints.containsKey(Pipe2RelayAssociation.FAN_LOW_SPEED.ordinal)) {
            val highestStage =
                HyperStatAssociationUtil.getPipe2HighestFanStage(configuration)
            val divider = if (highestStage == Pipe2RelayAssociation.FAN_MEDIUM_SPEED) 50 else 33

            doFanLowSpeed(
                relayOutputPoints[Pipe2RelayAssociation.FAN_LOW_SPEED.ordinal]!!,
                getSuperLogicalPointIfExist(Pipe2RelayAssociation.FAN_MEDIUM_SPEED),
                getSuperLogicalPointIfExist(Pipe2RelayAssociation.FAN_HIGH_SPEED),
                basicSettings.fanMode,
                fanLoopOutput,
                tuner.relayActivationHysteresis,
                relayStages,
                divider
            )
        }
    }

    private fun runFanMedium(
        tuner: HyperStatProfileTuners,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>,
        configuration: HyperStatPipe2Configuration
    ) {
        if (relayOutputPoints.containsKey(Pipe2RelayAssociation.FAN_MEDIUM_SPEED.ordinal)) {

            val highestStage =
                HyperStatAssociationUtil.getPipe2HighestFanStage(configuration).ordinal
            val divider = if (highestStage == 7) 50 else 33

            doFanMediumSpeed(
                relayOutputPoints[Pipe2RelayAssociation.FAN_MEDIUM_SPEED.ordinal]!!,
                getSuperLogicalPointIfExist(Pipe2RelayAssociation.FAN_HIGH_SPEED),
                basicSettings.fanMode,
                fanLoopOutput,
                tuner.relayActivationHysteresis,
                divider,
                relayStages
            )
        }
    }

    private fun runFanHigh(
        tuner: HyperStatProfileTuners,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>
    ) {
        if (relayOutputPoints.containsKey(Pipe2RelayAssociation.FAN_HIGH_SPEED.ordinal)) {
            doFanHighSpeed(
                relayOutputPoints[Pipe2RelayAssociation.FAN_HIGH_SPEED.ordinal]!!,
                basicSettings.fanMode,
                fanLoopOutput,
                tuner.relayActivationHysteresis,
                relayStages
            )
        }
    }


    private fun runAnalogFanSpeed(
        config: HyperStatPipe2Configuration,
        userIntents: UserIntents,
        analogOutStages: HashMap<String, Int>,
        basicSettings: BasicSettings
    ){
        var output = fanLoopOutput
        if( supplyWaterTempTh2  > heatingThreshold
            && currentTemp > userIntents.zoneCoolingTargetTemperature){
            output = 0
        }
        if(config.analogOut1State.enabled && HyperStatAssociationUtil.isAnalogOutMappedToFanSpeed(config.analogOut1State)) {
            doAnalogFanAction(
                Port.ANALOG_OUT_ONE,
                config.analogOut1State.perAtFanLow.toInt(),
                config.analogOut1State.perAtFanMedium.toInt(),
                config.analogOut1State.perAtFanHigh.toInt(),
                basicSettings.fanMode,
                basicSettings.conditioningMode,
                output,
                analogOutStages
            )
        }
        if(config.analogOut2State.enabled && HyperStatAssociationUtil.isAnalogOutMappedToFanSpeed(config.analogOut2State)) {
            doAnalogFanAction(
                Port.ANALOG_OUT_TWO,
                config.analogOut2State.perAtFanLow.toInt(),
                config.analogOut2State.perAtFanMedium.toInt(),
                config.analogOut2State.perAtFanHigh.toInt(),
                basicSettings.fanMode,
                basicSettings.conditioningMode,
                output,
                analogOutStages
            )
        }
        if(config.analogOut3State.enabled && HyperStatAssociationUtil.isAnalogOutMappedToFanSpeed(config.analogOut3State)) {
            doAnalogFanAction(
                Port.ANALOG_OUT_THREE,
                config.analogOut3State.perAtFanLow.toInt(),
                config.analogOut3State.perAtFanMedium.toInt(),
                config.analogOut3State.perAtFanHigh.toInt(),
                basicSettings.fanMode,
                basicSettings.conditioningMode,
                output,
                analogOutStages
            )
        }
    }


    private fun getSuperLogicalPointIfExist(association: Pipe2RelayAssociation ): String? {
        if(relayOutputPoints.containsKey(association.ordinal)){
            return relayOutputPoints[association.ordinal]
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
        equip: HyperStatPipe2Equip

    ) {
        // Reset Relay
            resetFan(relayStages,analogOutStages,basicSettings)
            resetWaterValue(relayStages,equip)
            resetAux(relayStages)

            if (relayOutputPoints.containsKey(Pipe2RelayAssociation.FAN_ENABLED.ordinal)) resetLogicalPoint(
                relayOutputPoints[Pipe2RelayAssociation.FAN_ENABLED.ordinal]!!
            )
            if (relayOutputPoints.containsKey(Pipe2RelayAssociation.OCCUPIED_ENABLED.ordinal)) resetLogicalPoint(
                relayOutputPoints[Pipe2RelayAssociation.OCCUPIED_ENABLED.ordinal]!!
            )

            if (analogOutputPoints.containsKey(Pipe2AnalogOutAssociation.DCV_DAMPER.ordinal)) resetLogicalPoint(
                analogOutputPoints[Pipe2AnalogOutAssociation.DCV_DAMPER.ordinal]!!
            )
    }

    private fun resetFan(
        relayStages: HashMap<String, Int> ,
        analogOutStages: HashMap<String, Int>,
        basicSettings: BasicSettings
    ){
        if(basicSettings.fanMode == StandaloneFanStage.AUTO||basicSettings.fanMode == StandaloneFanStage.OFF) {
            if (relayOutputPoints.containsKey(Pipe2RelayAssociation.FAN_LOW_SPEED.ordinal)) {
                resetLogicalPoint(relayOutputPoints[Pipe2RelayAssociation.FAN_LOW_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_1.displayName)
            }
            if (relayOutputPoints.containsKey(Pipe2RelayAssociation.FAN_MEDIUM_SPEED.ordinal)) {
                resetLogicalPoint(relayOutputPoints[Pipe2RelayAssociation.FAN_MEDIUM_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_2.displayName)
            }
            if (relayOutputPoints.containsKey(Pipe2RelayAssociation.FAN_HIGH_SPEED.ordinal)) {
                resetLogicalPoint(relayOutputPoints[Pipe2RelayAssociation.FAN_HIGH_SPEED.ordinal]!!)
                relayStages.remove(Stage.FAN_3.displayName)
            }
            if (analogOutputPoints.containsKey(Pipe2AnalogOutAssociation.FAN_SPEED.ordinal)) {
                resetLogicalPoint(analogOutputPoints[Pipe2AnalogOutAssociation.FAN_SPEED.ordinal]!!)
                analogOutStages.remove(AnalogOutput.FAN_SPEED.name)
            }
        }
    }

    private fun runRelayOperations(
        equip: HyperStatPipe2Equip,
        config: HyperStatPipe2Configuration,
        tuner: HyperStatProfileTuners,
        userIntents: UserIntents,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>,
        analogOutStages: HashMap<String, Int>
    ) {

        if (config.relay1State.enabled) {
            handleRelayState(
                config.relay1State, equip, Port.RELAY_ONE, tuner,
                userIntents, basicSettings, relayStages, config, analogOutStages
            )
        }
        if (config.relay2State.enabled) {
            handleRelayState(
                config.relay2State, equip, Port.RELAY_TWO, tuner,
                userIntents, basicSettings, relayStages, config, analogOutStages
            )
        }
        if (config.relay3State.enabled) {
            handleRelayState(
                config.relay3State, equip, Port.RELAY_THREE, tuner,
                userIntents, basicSettings, relayStages, config, analogOutStages
            )
        }
        if (config.relay4State.enabled) {
            handleRelayState(
                config.relay4State, equip, Port.RELAY_FOUR, tuner,
                userIntents, basicSettings, relayStages, config, analogOutStages
            )
        }
        if (config.relay5State.enabled) {
            handleRelayState(
                config.relay5State, equip, Port.RELAY_FIVE, tuner,
                userIntents, basicSettings, relayStages, config, analogOutStages
            )
        }
        if (config.relay6State.enabled) {
            handleRelayState(
                config.relay6State, equip, Port.RELAY_SIX, tuner,
                userIntents, basicSettings, relayStages, config, analogOutStages
            )
        }
    }


    private fun runAnalogOutOperations(
        equip: HyperStatPipe2Equip,
        config: HyperStatPipe2Configuration,
        basicSettings: BasicSettings,
        analogOutStages: HashMap<String, Int>,
        userIntents: UserIntents
    ) {

        if (config.analogOut1State.enabled) {
            handleAnalogOutState(
                equip, config, config.analogOut1State,
                Port.ANALOG_OUT_ONE, basicSettings, analogOutStages, userIntents
            )
        }
        if (config.analogOut2State.enabled) {
            handleAnalogOutState(
                equip, config, config.analogOut2State,
                Port.ANALOG_OUT_TWO, basicSettings, analogOutStages, userIntents
            )
        }
        if (config.analogOut3State.enabled) {
            handleAnalogOutState(
                equip, config, config.analogOut3State,
                Port.ANALOG_OUT_THREE, basicSettings, analogOutStages, userIntents
            )
        }
    }


    private fun handleRelayState(
        relayState: Pipe2RelayState,
        equip: HyperStatPipe2Equip,
        port: Port,
        tuner: HyperStatProfileTuners,
        userIntents: UserIntents,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>,
        configuration: HyperStatPipe2Configuration,
        analogOutStages: HashMap<String, Int>
    ) {
        when {

            (HyperStatAssociationUtil.isRelayAuxHeatingStage1(relayState)) -> {
                // Reheat is only used when the module is in cooling mode.

                if( basicSettings.conditioningMode == StandaloneConditioningMode.AUTO
                    || basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY) {
                        if (currentTemp < (userIntents.zoneHeatingTargetTemperature - tuner.auxHeating1Activate)) {
                            updateLogicalPointIdValue( relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal], 1.0)
                            relayStages[Pipe2RelayAssociation.AUX_HEATING_STAGE1.name] = 1
                            runSpecificAnalogFanSpeed(configuration,FanSpeed.MEDIUM,analogOutStages)

                        } else if (currentTemp >= (userIntents.zoneHeatingTargetTemperature - (tuner.auxHeating1Activate - 1))) {
                            updateLogicalPointIdValue(
                                relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal],
                                0.0
                            )
                            runSpecificAnalogFanSpeed(configuration,FanSpeed.OFF,analogOutStages)

                        } else {
                            if(getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal]!!) == 1.0)
                                relayStages[Pipe2RelayAssociation.AUX_HEATING_STAGE1.name] = 1
                        }

                }else{
                    resetAux(relayStages)
                }
            }
            // Reheat is only used when the module is in cooling mode.
            (HyperStatAssociationUtil.isRelayAuxHeatingStage2(relayState)) -> {
                if(basicSettings.conditioningMode == StandaloneConditioningMode.AUTO
                    || basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY) {
                        if (currentTemp < (userIntents.zoneHeatingTargetTemperature - tuner.auxHeating2Activate)) {
                            updateLogicalPointIdValue(
                                relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal],
                                1.0
                            )
                            relayStages[Pipe2RelayAssociation.AUX_HEATING_STAGE2.name] = 1
                            runSpecificAnalogFanSpeed(configuration,FanSpeed.HIGH,analogOutStages)
                        } else if (currentTemp >= (userIntents.zoneHeatingTargetTemperature - (tuner.auxHeating2Activate - 1))) {
                            updateLogicalPointIdValue(
                                relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal],
                                0.0
                            )
                        }else {
                            if(getCurrentLogicalPointStatus(relayOutputPoints[Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal]!!) == 1.0)
                                relayStages[Pipe2RelayAssociation.AUX_HEATING_STAGE2.name] = 1
                        }
                }
            }
            (HyperStatAssociationUtil.isRelayWaterValveStage(relayState)) -> {

                if(equip.waterSamplingStartTime == 0L && basicSettings.conditioningMode != StandaloneConditioningMode.OFF) {
                    doRelayWaterValveOperation(
                        equip, port, basicSettings, waterValveLoop(userIntents),
                        tuner.relayActivationHysteresis, relayStages
                    )
                }
            }
            (HyperStatAssociationUtil.isRelayFanEnabledStage(relayState)) -> {
                doFanEnabled(curState, port, fanLoopOutput)
            }
            (HyperStatAssociationUtil.isRelayOccupiedEnabledStage(relayState)) -> {
                doOccupiedEnabled(port)
            }
            (HyperStatAssociationUtil.isRelayHumidifierEnabledStage(relayState)) -> {
                doHumidifierOperation(
                    port, tuner.humidityHysteresis, userIntents.targetMinInsideHumidity
                )
            }
            (HyperStatAssociationUtil.isRelayDeHumidifierEnabledStage(relayState)) -> {
                doDeHumidifierOperation(
                    port, tuner.humidityHysteresis, userIntents.targetMaxInsideHumidity
                )
            }
        }
    }

    private fun handleAnalogOutState(
        equip: HyperStatPipe2Equip,
        config: HyperStatPipe2Configuration,
        analogOutState: Pipe2AnalogOutState,
        port: Port,
        basicSettings: BasicSettings,
        analogOutStages: HashMap<String, Int>,
        userIntents: UserIntents
    ) {
        // If we are in Auto Away mode we no need to Any analog Operations
        when {
            (HyperStatAssociationUtil.isAnalogOutMappedToWaterValve(analogOutState)) -> {
                // If it not in water sampling state the no need to take action
                if(equip.waterSamplingStartTime == 0L && basicSettings.conditioningMode != StandaloneConditioningMode.OFF) {
                    doAnalogWaterValveAction(
                        port, basicSettings.fanMode, basicSettings,
                        waterValveLoop(userIntents), analogOutStages
                    )
                }
            }

            (HyperStatAssociationUtil.isAnalogOutMappedToDcvDamper(analogOutState)) -> {
                doAnalogDCVAction(
                    port, analogOutStages, config.zoneCO2Threshold,
                    config.zoneCO2DamperOpeningRate, isDoorOpenState(config, equip)
                )
            }
        }
    }

    private fun runSpecificAnalogFanSpeed(config: HyperStatPipe2Configuration, fanSpeed: FanSpeed, analogOutStages: HashMap<String, Int>) {
        var analogOutputsUpdated = 0
        val analogOutputStates = listOf(
            Pair(Port.ANALOG_OUT_ONE, config.analogOut1State),
            Pair(Port.ANALOG_OUT_TWO, config.analogOut2State),
            Pair(Port.ANALOG_OUT_THREE, config.analogOut3State)
        )
        if (analogOutputStates.any { it.second.enabled && HyperStatAssociationUtil.isAnalogOutMappedToFanSpeed(it.second) }) {
            for ((port, analogOutState) in analogOutputStates) {
                if (analogOutState.enabled && HyperStatAssociationUtil.isAnalogOutMappedToFanSpeed(analogOutState)) {
                    updateLogicalPointIdValue(logicalPointsList[port]!!, getPercent(analogOutState, fanSpeed))
                    analogOutputsUpdated++
                }
            }
        }
        if (analogOutputsUpdated > 0 && fanSpeed != FanSpeed.OFF) {
            analogOutStages[AnalogOutput.FAN_SPEED.name] = 1
        }
    }

    private fun getPercent(analogOutState: Pipe2AnalogOutState, fanSpeed: FanSpeed):Double{
        return when(fanSpeed){
            FanSpeed.HIGH -> analogOutState.perAtFanHigh
            FanSpeed.MEDIUM -> analogOutState.perAtFanMedium
            FanSpeed.LOW -> analogOutState.perAtFanLow
            else -> 0.0
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



    private fun isDoorOpenState(
        config: HyperStatPipe2Configuration,
        equip: HyperStatPipe2Equip
    ): Boolean {

        // If analog in value is less than 2v door is closed(0) else door is open (1)
        var isDoorOpen = false

        var analog1SensorEnabled = false
        var analog2SensorEnabled = false

        if (config.analogIn1State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToDoorWindowSensor(config.analogIn1State)
        ) {
            val sensorValue = equip.hsHaystackUtil.getSensorPointValue(
                "door and window2 and sensor"
            )
            logIt( "Analog In 1 Door Window sensor value : Door is $sensorValue")
            if (sensorValue.toInt() == 1) isDoorOpen = true
            analog1SensorEnabled = true
        }

        if (config.analogIn2State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToDoorWindowSensor(config.analogIn2State)
        ) {
            val sensorValue = equip.hsHaystackUtil.getSensorPointValue(
                "door and window3 and sensor"
            )
            logIt( "Analog In 2 Door Window sensor value : Door is $sensorValue")
            if (sensorValue.toInt() == 1) isDoorOpen = true
            analog2SensorEnabled = true
        }

        doorWindowIsOpen(
            if (analog1SensorEnabled || analog2SensorEnabled) 1.0 else 0.0,
            if (isDoorOpen) 1.0 else 0.0
        )

        return isDoorOpen
    }

    private fun runForKeycardSensor(config: HyperStatPipe2Configuration, equip: HyperStatPipe2Equip) {

        var analog1KeycardEnabled = false
        var analog2KeycardEnabled = false

        var analog1Sensor = 0.0
        var analog2Sensor = 0.0

        if (config.analogIn1State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToKeyCardSensor(config.analogIn1State)
        ) {
            analog1KeycardEnabled = true
            analog1Sensor = equip.hsHaystackUtil.getSensorPointValue(
                "logical and keycard and sensor"
            )
        }

        if (config.analogIn2State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToKeyCardSensor(config.analogIn2State)
        ) {
            analog2KeycardEnabled = true
            analog2Sensor = equip.hsHaystackUtil.getSensorPointValue(
                "logical and keycard2 and sensor"
            )
        }

        logIt( "Keycard Enable Value "+  if (analog1KeycardEnabled || analog2KeycardEnabled) 1.0 else 0.0)
        logIt( "Keycard sensor Value "+ if (analog1Sensor > 0 || analog2Sensor > 0) 1.0 else 0.0)

        keyCardIsInSlot(
            if (analog1KeycardEnabled || analog2KeycardEnabled) 1.0 else 0.0,
            if (analog1Sensor > 0 || analog2Sensor > 0) 1.0 else 0.0
        )
    }

    private fun runForDoorWindowSensor(
        config: HyperStatPipe2Configuration,
        equip: HyperStatPipe2Equip,
        analogOutStages: HashMap<String, Int>,
        relayStages: HashMap<String, Int>
    ) {

        val isDoorOpen = isDoorOpenState(config,equip)
        logIt( " is Door Open ? $isDoorOpen")
        if (isDoorOpen){
            resetLoopOutputValues()
            resetAllLogicalPointValues()
            analogOutStages.clear()
            relayStages.clear()
        }
    }

    private fun resetLoopOutputValues() {
        logIt( "Resetting all the loop output values: ")
        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
    }

    private fun getSupplyWaterTemp(equipRef: String): Double {
        return CCUHsApi.getInstance()
            .readHisValByQuery("supply and water and temp and sensor and equipRef == \"$equipRef\"")
    }

    @JsonIgnore
    override fun getCurrentTemp(): Double {
        for (nodeAddress in pipe2DeviceMap.keys) {
            return pipe2DeviceMap[nodeAddress]!!.getCurrentTemp()
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
        for (nodeAddress in pipe2DeviceMap.keys) {
            if (pipe2DeviceMap[nodeAddress] == null) {
                continue
            }
            if (pipe2DeviceMap[nodeAddress]!!.getCurrentTemp() > 0) {
                tempTotal += pipe2DeviceMap[nodeAddress]!!.getCurrentTemp()
                nodeCount++
            }
        }
        return if (nodeCount == 0) 0.0 else tempTotal / nodeCount
    }

    override fun isZoneDead(): Boolean {
        val buildingLimitMax = TunerUtil.readBuildingTunerValByQuery("building and limit and max")
        val buildingLimitMin = TunerUtil.readBuildingTunerValByQuery("building and limit and min")
        val tempDeadLeeway = TunerUtil.readBuildingTunerValByQuery("temp and dead and leeway")
        for (node in pipe2DeviceMap.keys) {
            if (pipe2DeviceMap[node]!!.getCurrentTemp() > buildingLimitMax + tempDeadLeeway
                || pipe2DeviceMap[node]!!.getCurrentTemp() < buildingLimitMin - tempDeadLeeway
            ) {
                return true
            }
        }
        return false
    }

    private fun milliToMin(milliseconds: Long): Long {
        return (milliseconds / (1000 * 60) % 60)
    }

    /**
     * Function just to print logs
     */
    private fun logIt(msg: String){
        Log.i(L.TAG_CCU_HSPIPE2, msg)
    }
}