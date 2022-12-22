package a75f.io.logic.bo.building.hyperstat.profiles.cpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Occupied
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.profiles.HyperStatPackageUnitProfile
import a75f.io.logic.bo.building.hyperstat.common.*
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getActualFanMode
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.jobs.HyperStatUserIntentHandler
import a75f.io.logic.jobs.HyperStatUserIntentHandler.Companion.updateHyperStatUIPoints
import a75f.io.logic.tuners.TunerUtil
import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnore


/**
 * @author tcase@75f.io
 * Created on 7/7/21.
 */
class HyperStatCpuProfile : HyperStatPackageUnitProfile() {

    // One zone can have many hyperstat devices.  Each has its own address and equip representation
    private val cpuDeviceMap: MutableMap<Short, HyperStatCpuEquip> = mutableMapOf()

    private var coolingLoopOutput = 0
    private var heatingLoopOutput = 0
    private var fanLoopOutput = 0
    override lateinit var occupancyStatus: Occupancy
    private val hyperstatCPUAlgorithm = HyperstatLoopController()

    lateinit var curState: ZoneState

    override fun getProfileType() = ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        val equip = cpuDeviceMap[address]
        return equip?.getConfiguration() as T
    }

    override fun updateZonePoints() {
        cpuDeviceMap.forEach { (_, equip) ->
            Log.i(L.TAG_CCU_HSCPU, "Process Equip: equipRef =  ${equip.equipRef}")
            processHyperStatCPUProfile(equip)
        }
    }

    fun addEquip(node: Short): HyperStatEquip {
        val equip = HyperStatCpuEquip(node)
        equip.initEquipReference(node)
        cpuDeviceMap[node] = equip
        return equip
    }

    override fun addNewEquip(node: Short, room: String, floor: String, baseConfig: BaseProfileConfiguration) {
        val equip = addEquip(node)
        val configuration = equip.initializePoints(baseConfig as HyperStatCpuConfiguration, room, floor, node)
        hsHaystackUtil = equip.hsHaystackUtil
        return configuration
    }

    override fun getHyperStatEquip(node: Short): HyperStatEquip {
        return cpuDeviceMap[node] as HyperStatCpuEquip
    }

    // Run the profile logic and algorithm for an equip.
    fun processHyperStatCPUProfile(equip: HyperStatCpuEquip) {

            if (Globals.getInstance().isTestMode) {
                Log.i(TAG, "Test mode is on: ${equip.equipRef}")
                return
            }

            val relayStages = HashMap<String, Int>()
            val analogOutStages = HashMap<String, Int>()
            logicalPointsList = equip.getLogicalPointList()
            hsHaystackUtil = HSHaystackUtil(equip.equipRef!!, CCUHsApi.getInstance())
            curState = ZoneState.DEADBAND

            if (mInterface != null) mInterface.refreshView()

            if (isZoneDead) {
                handleDeadZone(equip)
                return
            }

            // get current state from data store (e.g. from Equip)
            val config = equip.getConfiguration()

            // get latest tuner points
            val hyperStatTuners = fetchHyperstatTuners(equip)
            val userIntents = fetchUserIntents(equip)
            val averageDesiredTemp = (
                    userIntents.zoneCoolingTargetTemperature + userIntents.zoneHeatingTargetTemperature) / 2.0

            if (averageDesiredTemp != equip.hsHaystackUtil.getDesiredTemp()) {
                equip.hsHaystackUtil.setDesiredTemp(averageDesiredTemp)
            }
            // occuStatus = equip.hsHaystackUtil.getOccupancyStatus()
            occupancyStatus = equipOccupancyHandler.currentOccupiedMode

            //StandaloneFanStage  StandaloneConditioningMode
            var basicSettings = fetchBasicSettings(equip)

            val fanModeSaved = FanModeCacheStorage().getFanModeFromCache(equip.equipRef!!)

            // Updating the current fan mode based on the current occupied status
            updateFanMode(equip, basicSettings, fanModeSaved)

            // Collect the update basic settings
            basicSettings = fetchBasicSettings(equip)

            hyperstatCPUAlgorithm.initialise(tuners = hyperStatTuners)
            hyperstatCPUAlgorithm.dumpLogs()

            if (currentTemp > userIntents.zoneCoolingTargetTemperature && state != ZoneState.COOLING) {
                hyperstatCPUAlgorithm.resetCoolingControl()
                state = ZoneState.COOLING
                Log.i(L.TAG_CCU_HSCPU,"Resetting cooling")
            } else if (currentTemp < userIntents.zoneHeatingTargetTemperature && state != ZoneState.HEATING) {
                hyperstatCPUAlgorithm.resetHeatingControl()
                state = ZoneState.HEATING
                Log.i(L.TAG_CCU_HSCPU,"Resetting heating")
            }
            coolingLoopOutput = 0
            heatingLoopOutput = 0
            fanLoopOutput = 0

            when (state) {
                //Update coolingLoop when the zone is in cooling or it was in cooling and no change over happened yet.
                ZoneState.COOLING -> coolingLoopOutput = hyperstatCPUAlgorithm.calculateCoolingLoopOutput(
                    currentTemp, userIntents.zoneCoolingTargetTemperature
                ).toInt().coerceAtLeast(0)

                //Update heatingLoop when the zone is in heating or it was in heating and no change over happened yet.
                ZoneState.HEATING -> heatingLoopOutput = hyperstatCPUAlgorithm.calculateHeatingLoopOutput(
                    userIntents.zoneHeatingTargetTemperature, currentTemp
                ).toInt().coerceAtLeast(0)

                else -> Log.i(L.TAG_CCU_HSCPU, " Zone is in deadband")
            }

            if (coolingLoopOutput > 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY
                        ||basicSettings.conditioningMode == StandaloneConditioningMode.AUTO) ) {
                fanLoopOutput = ((coolingLoopOutput * hyperStatTuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
            }
            if (heatingLoopOutput > 0  && (basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY
                        ||basicSettings.conditioningMode == StandaloneConditioningMode.AUTO)) {
                fanLoopOutput = ((heatingLoopOutput * hyperStatTuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
            }


            equip.hsHaystackUtil.updateOccupancyDetection()
            runForDoorWindowSensor(config, equip)
            runForKeycardSensor(config, equip)
            equip.hsHaystackUtil.updateAllLoopOutput(coolingLoopOutput,heatingLoopOutput,fanLoopOutput)
            val currentOperatingMode = equip.hsHaystackUtil.getOccupancyModePointValue().toInt()

            Log.i(L.TAG_CCU_HSCPU,
                "Analog Fan speed multiplier  ${hyperStatTuners.analogFanSpeedMultiplier} \n"+
                     "Current Working mode : ${Occupancy.values()[currentOperatingMode]} \n"+
                     "Current Temp : $currentTemp \n"+
                     "Desired Heating: ${userIntents.zoneHeatingTargetTemperature} \n"+
                     "Desired Cooling: ${userIntents.zoneCoolingTargetTemperature} \n"+
                     "Heating Loop Output: $heatingLoopOutput \n"+
                     "Cooling Loop Output:: $coolingLoopOutput \n"+
                     "Fan Loop Output:: $fanLoopOutput \n"
            )
            Log.i("Loop Output", "Current Temp : $currentTemp"+
                    "Desired Heating: ${userIntents.zoneHeatingTargetTemperature}"+
                    "Desired Cooling: ${userIntents.zoneCoolingTargetTemperature} "+
                    "Heating Loop Output: $heatingLoopOutput "+
                    "Cooling Loop Output:: $coolingLoopOutput "+
                    "Fan Loop Output:: $fanLoopOutput")


        if (basicSettings.fanMode != StandaloneFanStage.OFF) {
            runRelayOperations(
                equip,
                config,
                hyperStatTuners,
                userIntents,
                basicSettings,
                relayStages
            )
            runAnalogOutOperations(equip, config, basicSettings, analogOutStages)
        }else{
            resetAllLogicalPointValues()
        }
            var zoneOperatingMode = ZoneState.DEADBAND.ordinal
            if(currentTemp < averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.COOL_ONLY) {
                  zoneOperatingMode = ZoneState.HEATING.ordinal
            }
            if(currentTemp >= averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
                  zoneOperatingMode = ZoneState.COOLING.ordinal
            }
            Log.i(L.TAG_CCU_HSCPU,
                "averageDesiredTemp $averageDesiredTemp" + "zoneOperatingMode ${ZoneState.values()[zoneOperatingMode]}"
            )
            equip.hsHaystackUtil.setProfilePoint("operating and mode", zoneOperatingMode.toDouble())
            if (equip.hsHaystackUtil.getStatus() != curState.ordinal.toDouble())
                equip.hsHaystackUtil.setStatus(curState.ordinal.toDouble())
            var temperatureState = ZoneTempState.NONE
            if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY

            Log.i(L.TAG_CCU_HSCPU, "Equip Running : $curState")
            HyperStatUserIntentHandler.updateHyperStatStatus(
                equipId = equip.equipRef!!,
                portStages = relayStages,
                analogOutStages = analogOutStages,
                temperatureState = temperatureState
            )
            Log.i(L.TAG_CCU_HSCPU, "**********************************************")
    }

    private fun fetchBasicSettings(equip: HyperStatCpuEquip) =

        BasicSettings(
            conditioningMode = StandaloneConditioningMode.values()[equip.hsHaystackUtil.getCurrentConditioningMode().toInt()],
            fanMode = StandaloneFanStage.values()[equip.hsHaystackUtil.getCurrentFanMode().toInt()]
        )

    private fun fetchUserIntents(equip: HyperStatCpuEquip): UserIntents {

        return UserIntents(
            currentTemp = equip.getCurrentTemp(),
            zoneCoolingTargetTemperature = equip.hsHaystackUtil.getDesiredTempCooling(),
            zoneHeatingTargetTemperature = equip.hsHaystackUtil.getDesiredTempHeating(),
            targetMinInsideHumidity = equip.hsHaystackUtil.getTargetMinInsideHumidity(),
            targetMaxInsideHumidity = equip.hsHaystackUtil.getTargetMaxInsideHumidity(),
        )
    }

    private fun fetchHyperstatTuners(equip: HyperStatCpuEquip): HyperStatProfileTuners {

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
            "relay and  activation", equip.equipRef!!
        ).toInt()
        hsTuners.analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery(
            "analog and fan and speed and multiplier", equip.equipRef!!)
        hsTuners.humidityHysteresis = TunerUtil.getHysteresisPoint("humidity", equip.equipRef!!).toInt()
        return hsTuners
    }



    private fun runRelayOperations(
        equip: HyperStatCpuEquip,
        config: HyperStatCpuConfiguration,
        tuner: HyperStatProfileTuners,
        userIntents: UserIntents,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>
    ) {

        if (config.relay1State.enabled) {
            handleRelayState(
                config.relay1State, equip, config, Port.RELAY_ONE, tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay2State.enabled) {
            handleRelayState(
                config.relay2State, equip, config, Port.RELAY_TWO, tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay3State.enabled) {
            handleRelayState(
                config.relay3State, equip, config, Port.RELAY_THREE, tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay4State.enabled) {
            handleRelayState(
                config.relay4State, equip, config, Port.RELAY_FOUR, tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay5State.enabled) {
            handleRelayState(
                config.relay5State, equip, config, Port.RELAY_FIVE, tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay6State.enabled) {
            handleRelayState(
                config.relay6State, equip, config, Port.RELAY_SIX, tuner, userIntents, basicSettings, relayStages
            )
        }
        Log.i(L.TAG_CCU_HSCPU, "================================")
    }

    private fun runAnalogOutOperations(
        equip: HyperStatCpuEquip,
        config: HyperStatCpuConfiguration,
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


    private fun handleRelayState(
        relayState: RelayState,
        equip: HyperStatCpuEquip,
        config: HyperStatCpuConfiguration,
        port: Port,
        tuner: HyperStatProfileTuners,
        userIntents: UserIntents,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>
    ) {
        when {
            (HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(relayState)) -> {

                if (basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.COOL_ONLY.ordinal ||
                    basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
                ) {
                    runRelayForCooling(relayState, port, config, tuner, relayStages)
                }else{
                    resetPort(port)
                }
            }
            (HyperStatAssociationUtil.isRelayAssociatedToHeatingStage(relayState)) -> {

                if (basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.HEAT_ONLY.ordinal ||
                    basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
                ) {
                    runRelayForHeating(relayState, port, config, tuner, relayStages)
                }else{
                    resetPort(port)
                }
            }
            (HyperStatAssociationUtil.isRelayAssociatedToFan(relayState)) -> {

                if (basicSettings.fanMode != StandaloneFanStage.OFF) {
                    runRelayForFanSpeed(relayState, port, config, tuner, relayStages, basicSettings)
                }else{
                   resetPort(port)
                }
            }
            (HyperStatAssociationUtil.isRelayAssociatedToFanEnabled(relayState)) -> {
                doFanEnabled( curState,port, fanLoopOutput )
            }

            (HyperStatAssociationUtil.isRelayAssociatedToOccupiedEnabled(relayState)) -> {
                doOccupiedEnabled(port)
            }
            (HyperStatAssociationUtil.isRelayAssociatedToHumidifier(relayState)) -> {
                doHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMinInsideHumidity)
            }
            (HyperStatAssociationUtil.isRelayAssociatedToDeHumidifier(relayState)) -> {
                doDeHumidifierOperation(port,tuner.humidityHysteresis,userIntents.targetMaxInsideHumidity)
            }

        }

    }

    private fun runRelayForCooling(
        relayAssociation: RelayState,
        whichPort: Port,
        config: HyperStatCpuConfiguration,
        tuner: HyperStatProfileTuners,
        relayStages: HashMap<String, Int>
    ) {

        Log.i(TAG, " $whichPort: ${relayAssociation.association}")
        when (relayAssociation.association) {

            CpuRelayAssociation.COOLING_STAGE_1 -> {
                doCoolingStage1(
                    whichPort,coolingLoopOutput,tuner.relayActivationHysteresis,relayStages
                )
            }
            CpuRelayAssociation.COOLING_STAGE_2 -> {
                val highestStage = HyperStatAssociationUtil.getHighestCoolingStage(config).ordinal
                val divider = if (highestStage == 1) 50 else 33
               doCoolingStage2(
                   whichPort,coolingLoopOutput,tuner.relayActivationHysteresis,divider,relayStages)
            }
            CpuRelayAssociation.COOLING_STAGE_3 -> {
                doCoolingStage3(
                    whichPort,coolingLoopOutput,tuner.relayActivationHysteresis,relayStages)
            }
            else -> {}
        }

        if(getCurrentPortStatus(whichPort) == 1.0)
            curState = ZoneState.COOLING

    }

    private fun runRelayForHeating(
        relayAssociation: RelayState,
        whichPort: Port,
        config: HyperStatCpuConfiguration,
        tuner: HyperStatProfileTuners,
        relayStages: HashMap<String, Int>
    ) {

        Log.i(TAG, " $whichPort: ${relayAssociation.association}")
        when (relayAssociation.association) {

            CpuRelayAssociation.HEATING_STAGE_1 -> {

                doHeatingStage1(
                    whichPort,
                    heatingLoopOutput,
                    tuner.relayActivationHysteresis,
                    relayStages
                )

            }
            CpuRelayAssociation.HEATING_STAGE_2 -> {

                val highestStage = HyperStatAssociationUtil.getHighestHeatingStage(config).ordinal
                val divider = if (highestStage == 4) 50 else 33
                doHeatingStage2(
                    whichPort, heatingLoopOutput, tuner.relayActivationHysteresis,
                    divider, relayStages)
            }
            CpuRelayAssociation.HEATING_STAGE_3 -> {

                doHeatingStage3(
                    whichPort, heatingLoopOutput, tuner.relayActivationHysteresis, relayStages)
            }

            else -> {}
        }
        if(getCurrentPortStatus(whichPort) == 1.0)
            curState = ZoneState.HEATING
    }


    private fun runRelayForFanSpeed(
        relayAssociation: RelayState,
        whichPort: Port,
        config: HyperStatCpuConfiguration,
        tuner: HyperStatProfileTuners,
        relayStages: HashMap<String, Int>,
        basicSettings: BasicSettings,
    ) {
        Log.i(TAG, " $whichPort: ${relayAssociation.association} runRelayForFanSpeed: ${basicSettings.fanMode}")

        if (basicSettings.fanMode == StandaloneFanStage.AUTO
            && basicSettings.conditioningMode == StandaloneConditioningMode.OFF ) {
            Log.i(L.TAG_CCU_HSCPU, "Cond is Off , Fan is Auto  : ")
            resetPort(whichPort)
            return
        }
        val highestStage = HyperStatAssociationUtil.getHighestFanStage(config).ordinal
        val divider = if (highestStage == 7) 50 else 33

        when (relayAssociation.association) {
            CpuRelayAssociation.FAN_LOW_SPEED -> {
                doFanLowSpeed(
                    logicalPointsList[whichPort]!!,null,null, basicSettings.fanMode,
                    fanLoopOutput,tuner.relayActivationHysteresis,relayStages,divider)
            }
            CpuRelayAssociation.FAN_MEDIUM_SPEED -> {
                doFanMediumSpeed(
                    logicalPointsList[whichPort]!!,null,basicSettings.fanMode,
                    fanLoopOutput,tuner.relayActivationHysteresis,divider,relayStages)
            }
            CpuRelayAssociation.FAN_HIGH_SPEED -> {
                doFanHighSpeed(
                    logicalPointsList[whichPort]!!,basicSettings.fanMode,
                    fanLoopOutput,tuner.relayActivationHysteresis,relayStages)
            }
            else -> return
        }
    }


    private fun handleAnalogOutState(
        analogOutState: AnalogOutState,
        equip: HyperStatCpuEquip,
        config: HyperStatCpuConfiguration,
        port: Port,
        basicSettings: BasicSettings,
        analogOutStages: HashMap<String, Int>
    ) {
        // If we are in Auto Away mode we no need to Any analog Operations
        when {
            (HyperStatAssociationUtil.isAnalogOutAssociatedToCooling(analogOutState)) -> {
                doAnalogCooling(port,basicSettings.conditioningMode,analogOutStages,coolingLoopOutput)
            }
            (HyperStatAssociationUtil.isAnalogOutAssociatedToHeating(analogOutState)) -> {
                doAnalogHeating(port,basicSettings.conditioningMode,analogOutStages,heatingLoopOutput)
            }
            (HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState)) -> {

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
            (HyperStatAssociationUtil.isAnalogOutAssociatedToDcvDamper(analogOutState)) -> {

                doAnalogDCVAction(
                    port,analogOutStages,config.zoneCO2Threshold,config.zoneCO2DamperOpeningRate,isDoorOpenState(config,equip)
                )
            }

        }
    }

    private fun handleDeadZone(equip: HyperStatCpuEquip) {

        Log.i(L.TAG_CCU_HSCPU, "updatePointsForEquip: Dead Zone ")
        state = ZoneState.TEMPDEAD
        resetAllLogicalPointValues(equip)
        equip.hsHaystackUtil.setProfilePoint("operating and mode", 0.0)
        if (equip.hsHaystackUtil.getEquipStatus() != state.ordinal.toDouble())
            equip.hsHaystackUtil.setEquipStatus(state.ordinal.toDouble())

        val curStatus = equip.hsHaystackUtil.getEquipLiveStatus()
        if (curStatus != "Zone Temp Dead") {
            equip.hsHaystackUtil.writeDefaultVal("status and message and writable", "Zone Temp Dead")
        }
        equip.haystack.writeHisValByQuery(
            "point and status and his and group == \"${equip.node}\"",
            ZoneState.TEMPDEAD.ordinal.toDouble()
        )
    }

    /**
     * Function which resets the fan modes based on the occupancy status
     */
    private fun updateFanMode(
        equip: HyperStatCpuEquip,
        basicSettings: BasicSettings,
        fanModeSaved: Int
    ) {

        Log.i(L.TAG_CCU_HSCPU, "Fan Details $occupancyStatus  ${basicSettings.fanMode}  $fanModeSaved")
        if (occupancyStatus != Occupancy.OCCUPIED && basicSettings.fanMode != StandaloneFanStage.OFF
            && basicSettings.fanMode != StandaloneFanStage.AUTO
            && basicSettings.fanMode != StandaloneFanStage.LOW_ALL_TIME
            && basicSettings.fanMode != StandaloneFanStage.MEDIUM_ALL_TIME
            && basicSettings.fanMode != StandaloneFanStage.HIGH_ALL_TIME
        ) {
            Log.i(L.TAG_CCU_HSCPU, "Resetting the Fan status back to  AUTO: ")
            updateHyperStatUIPoints(
                equipRef = equip.equipRef!!,
                command = "fan and operation and mode and cpu",
                value = StandaloneFanStage.AUTO.ordinal.toDouble()
            )
        }

        if (occupancyStatus != Occupancy.OCCUPIED && basicSettings.fanMode == StandaloneFanStage.AUTO && fanModeSaved != 0) {
            Log.i(L.TAG_CCU_HSCPU, "Resetting the Fan status back to ${StandaloneFanStage.values()[fanModeSaved]}")

            val actualFanMode = getActualFanMode(equip.node.toString(), fanModeSaved)
            updateHyperStatUIPoints(
                equipRef = equip.equipRef!!,
                command = "fan and operation and mode and cpu",
                value = actualFanMode.toDouble()
            )
        }
    }


    private fun runForDoorWindowSensor(config: HyperStatCpuConfiguration, equip: HyperStatCpuEquip) {

        val isDoorOpen = isDoorOpenState(config,equip)
        Log.i(L.TAG_CCU_HSCPU, " is Door Open ? $isDoorOpen")
        if (isDoorOpen) resetLoopOutputValues()
    }


    private fun isDoorOpenState(config: HyperStatCpuConfiguration, equip: HyperStatCpuEquip): Boolean{

        // If thermistor value less than 10000 ohms door is closed (0) else door is open (1)
        // If analog in value is less than 2v door is closed(0) else door is open (1)
        var isDoorOpen = false

        var th2SensorEnabled = false
        var analog1SensorEnabled = false
        var analog2SensorEnabled = false

        // Thermistor 2 is always mapped to door window sensor
        if (config.isEnableDoorWindowSensor) {
            val sensorValue = equip.hsHaystackUtil.getSensorPointValue(
                "door and window and logical and sensor"
            )
            Log.i(L.TAG_CCU_HSCPU, "TH2 Door Window sensor value : Door is $sensorValue")
            if (sensorValue.toInt() == 1) isDoorOpen = true
            th2SensorEnabled = true
        }

        if (config.analogIn1State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToDoorWindowSensor(config.analogIn1State)
        ) {
            val sensorValue = equip.hsHaystackUtil.getSensorPointValue(
                "door and window2 and logical and sensor"
            )
            Log.i(L.TAG_CCU_HSCPU, "Analog In 1 Door Window sensor value : Door is $sensorValue")
            if (sensorValue.toInt() == 1) isDoorOpen = true
            analog1SensorEnabled = true
        }

        if (config.analogIn2State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToDoorWindowSensor(config.analogIn2State)
        ) {
            val sensorValue = equip.hsHaystackUtil.getSensorPointValue(
                "door and window3 and logical and sensor"
            )
            Log.i(L.TAG_CCU_HSCPU, "Analog In 2 Door Window sensor value : Door is $sensorValue")
            if (sensorValue.toInt() == 1) isDoorOpen = true
            analog2SensorEnabled = true
        }

        doorWindowIsOpen(
            if (th2SensorEnabled || analog1SensorEnabled || analog2SensorEnabled) 1.0 else 0.0,
            if (isDoorOpen) 1.0 else 0.0
        )

        return isDoorOpen
    }

    private fun resetLoopOutputValues() {
        Log.i(L.TAG_CCU_HSCPU, "Resetting all the loop output values: ")
        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
    }

    private fun runForKeycardSensor(config: HyperStatCpuConfiguration, equip: HyperStatCpuEquip) {

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

        Log.i(L.TAG_CCU_HSCPU, "Keycard Enable Value "+  if (analog1KeycardEnabled || analog2KeycardEnabled) 1.0 else 0.0)
        Log.i(L.TAG_CCU_HSCPU, "Keycard sensor Value "+ if (analog1Sensor > 0 || analog2Sensor > 0) 1.0 else 0.0)

        keyCardIsInSlot(
            if (analog1KeycardEnabled || analog2KeycardEnabled) 1.0 else 0.0,
            if (analog1Sensor > 0 || analog2Sensor > 0) 1.0 else 0.0
        )
    }

    override fun getEquip(): Equip? {
        for (nodeAddress in cpuDeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    @JsonIgnore
    override fun getNodeAddresses(): Set<Short?> {
        return cpuDeviceMap.keys
    }

    override fun isZoneDead(): Boolean {
        val buildingLimitMax = TunerUtil.readBuildingTunerValByQuery("building and limit and max")
        val buildingLimitMin = TunerUtil.readBuildingTunerValByQuery("building and limit and min")
        val tempDeadLeeway = TunerUtil.readBuildingTunerValByQuery("temp and dead and leeway")
        for (node in cpuDeviceMap.keys) {
            if (cpuDeviceMap[node]!!.getCurrentTemp() > buildingLimitMax + tempDeadLeeway
                || cpuDeviceMap[node]!!.getCurrentTemp() < buildingLimitMin - tempDeadLeeway
            ) {
                return true
            }
        }
        return false
    }

    private fun resetAllLogicalPointValues(equip: HyperStatCpuEquip) {

        equip.hsHaystackUtil.updateAllLoopOutput(0,0,0)

        resetAllLogicalPointValues()

        HyperStatUserIntentHandler.updateHyperStatStatus(
            equipId = equip.equipRef!!,
            portStages = HashMap(),
            analogOutStages = HashMap(),
            temperatureState = ZoneTempState.TEMP_DEAD
        )

    }

    @JsonIgnore
    override fun getCurrentTemp(): Double {
        for (nodeAddress in cpuDeviceMap.keys) {
            return cpuDeviceMap[nodeAddress]!!.getCurrentTemp()
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
        for (nodeAddress in cpuDeviceMap.keys) {
            if (cpuDeviceMap[nodeAddress] == null) {
                continue
            }
            if (cpuDeviceMap[nodeAddress]!!.getCurrentTemp() > 0) {
                tempTotal += cpuDeviceMap[nodeAddress]!!.getCurrentTemp()
                nodeCount++
            }
        }
        return if (nodeCount == 0) 0.0 else tempTotal / nodeCount
    }

}