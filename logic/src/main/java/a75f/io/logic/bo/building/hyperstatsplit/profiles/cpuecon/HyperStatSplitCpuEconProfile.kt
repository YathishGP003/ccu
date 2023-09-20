package a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon

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
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstatsplit.common.*
import a75f.io.logic.bo.building.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getActualFanMode
import a75f.io.logic.bo.building.hyperstatsplit.profiles.HyperStatSplitPackageUnitProfile
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.logic.jobs.HyperStatSplitUserIntentHandler
import a75f.io.logic.tuners.TunerUtil
import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnore
import kotlin.math.roundToInt


/**
 * @author tcase@75f.io (HyperStat CPU)
 * Created on 7/7/21.
 *
 * Created for HyperStat Split CPU/Econ by Nick P on 07-24-2023.
 */
class HyperStatSplitCpuEconProfile : HyperStatSplitPackageUnitProfile() {

    companion object {

        fun getAirEnthalpy(temp: Double, humidity: Double): Double {

            val A = 0.007468 * Math.pow(temp,2.0) - 0.4344 * temp + 11.176
            val B = 0.2372 * temp + 0.1230;
            val H = A * 0.01 * humidity + B;

            Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "temperature "+temp+" humidity "+humidity+" Enthalpy: "+H);
            return CCUUtils.roundToTwoDecimal(H)

        }

    }

    // One zone can have many hyperstat devices.  Each has its own address and equip representation
    private val cpuEconDeviceMap: MutableMap<Short, HyperStatSplitCpuEconEquip> = mutableMapOf()

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
    private val hyperstatSplitCPUEconAlgorithm = HyperstatSplitLoopController()

    private var economizingAvailable = false
    private var dcvAvailable = false
    private var matThrottle = false

    lateinit var curState: ZoneState

    override fun getProfileType() = ProfileType.HYPERSTATSPLIT_CPU

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        val equip = cpuEconDeviceMap[address]
        return equip?.getConfiguration() as T
    }

    override fun updateZonePoints() {
        cpuEconDeviceMap.forEach { (_, equip) ->
            logIt("Process Equip: equipRef =  ${equip.equipRef}")
            processHyperStatSplitCPUEconProfile(equip)
        }
    }

    fun addEquip(node: Short): HyperStatSplitEquip {
        val equip = HyperStatSplitCpuEconEquip(node)
        equip.initEquipReference(node)
        cpuEconDeviceMap[node] = equip
        return equip
    }

    override fun addNewEquip(node: Short, room: String, floor: String, baseConfig: BaseProfileConfiguration) {

        val equip = addEquip(node)

        val configuration = equip.initializePoints(baseConfig as HyperStatSplitCpuEconConfiguration, room, floor, node)
        hsSplitHaystackUtil = equip.hsSplitHaystackUtil

        return configuration
    }

    override fun getHyperStatSplitEquip(node: Short): HyperStatSplitEquip {
        return cpuEconDeviceMap[node] as HyperStatSplitCpuEconEquip
    }

    // Run the profile logic and algorithm for an equip.
    fun processHyperStatSplitCPUEconProfile(equip: HyperStatSplitCpuEconEquip) {

        if (Globals.getInstance().isTestMode) {
            logIt("Test mode is on: ${equip.nodeAddress}")
            return
        }

        if (mInterface != null) mInterface.refreshView()
        val relayStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()
        logicalPointsList = equip.getLogicalPointList()

        hsSplitHaystackUtil = HSSplitHaystackUtil(equip.equipRef!!, CCUHsApi.getInstance())

        if (isZoneDead) {
            handleDeadZone(equip)
            return
        }
        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode

        val config = equip.getConfiguration()
        val hyperStatSplitTuners = fetchHyperStatSplitTuners(equip)
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)

        val fanModeSaved = FanModeCacheStorage().getFanModeFromCache(equip.equipRef!!)
        val actualFanMode = getActualFanMode(equip.node.toString(), fanModeSaved)

        // At this point, effectiveConditioningMode will be set to OFF if Condensate Overflow is detected
        val basicSettings = fetchBasicSettings(equip)

        val updatedFanMode = fallBackFanMode(equip, equip.equipRef!!, fanModeSaved, actualFanMode, basicSettings)
        basicSettings.fanMode = updatedFanMode

        hyperstatSplitCPUEconAlgorithm.initialise(tuners = hyperStatSplitTuners)
        hyperstatSplitCPUEconAlgorithm.dumpLogs()
        handleChangeOfDirection(userIntents)

        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        economizingLoopOutput = 0
        dcvLoopOutput = 0
        outsideAirCalculatedMinDamper = 0
        outsideAirLoopOutput = 0
        outsideAirFinalLoopOutput = 0
        evaluateLoopOutputs(equip, userIntents, basicSettings, hyperStatSplitTuners)

        equip.hsSplitHaystackUtil.updateOccupancyDetection()
        equip.hsSplitHaystackUtil.updateConditioningLoopOutput(coolingLoopOutput,heatingLoopOutput,fanLoopOutput,false,0)
        equip.hsSplitHaystackUtil.updateOaoLoopOutput(economizingLoopOutput, dcvLoopOutput, outsideAirLoopOutput, outsideAirFinalLoopOutput)

        val currentOperatingMode = equip.hsSplitHaystackUtil.getOccupancyModePointValue().toInt()

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
            runRelayOperations(config, hyperStatSplitTuners, userIntents, basicSettings, relayStages)
            runAnalogOutOperations(equip, config, basicSettings, analogOutStages)
        } else{
            resetAllLogicalPointValues()
        }
        setOperatingMode(currentTemp,averageDesiredTemp,basicSettings,equip)

        if (equip.hsSplitHaystackUtil.getStatus() != curState.ordinal.toDouble())
            equip.hsSplitHaystackUtil.setStatus(curState.ordinal.toDouble())

        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY

        logIt("Equip Running : $curState")

        HyperStatSplitUserIntentHandler.updateHyperStatSplitStatus(
            equip.equipRef!!, relayStages, analogOutStages, temperatureState, economizingLoopOutput,
            equip.hsSplitHaystackUtil.getCondensateOverflowStatus(), equip.hsSplitHaystackUtil.getFilterStatus()
        )
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "processHyperStatSplitCpuEconProfile() complete")

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

    private fun evaluateLoopOutputs(equip: HyperStatSplitCpuEconEquip, userIntents: UserIntents, basicSettings: BasicSettings, hyperStatSplitTuners: HyperStatSplitProfileTuners){

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

        if (coolingLoopOutput > 0 && (basicSettings.effectiveConditioningMode == StandaloneConditioningMode.COOL_ONLY
                    || basicSettings.effectiveConditioningMode == StandaloneConditioningMode.AUTO) ) {
            fanLoopOutput = ((coolingLoopOutput * hyperStatSplitTuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
        }
        else if (heatingLoopOutput > 0  && (basicSettings.effectiveConditioningMode == StandaloneConditioningMode.HEAT_ONLY
                    ||basicSettings.effectiveConditioningMode == StandaloneConditioningMode.AUTO)) {
            fanLoopOutput = ((heatingLoopOutput * hyperStatSplitTuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
        }

        evaluateOAOLoop(equip)

    }

    private fun evaluateOAOLoop(equip: HyperStatSplitCpuEconEquip) {

        // If there's not an OAO damper mapped,
        if (!HyperStatSplitAssociationUtil.isAnyAnalogAssociatedToOAO(equip.getConfiguration())) {

            economizingAvailable = false
            economizingLoopOutput = 0
            dcvAvailable = false
            dcvLoopOutput = 0
            outsideAirLoopOutput = 0
            matThrottle = false
            outsideAirFinalLoopOutput = 0

        } else {

            val outsideDamperMinOpen = hsSplitHaystackUtil.getOutsideDamperMinOpen().toInt()
            Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "outsideDamperMinOpen: " + outsideDamperMinOpen)
            doEconomizing(equip)
            doDcv(equip, outsideDamperMinOpen)

            outsideAirLoopOutput = Math.max(economizingLoopOutput, dcvLoopOutput)

            val exhaustFanStage1Threshold = TunerUtil.readTunerValByQuery("exhaust and fan and stage1 and threshold", equip.equipRef)
            val exhaustFanStage2Threshold = TunerUtil.readTunerValByQuery("exhaust and fan and stage2 and threshold", equip.equipRef)
            val exhaustFanHysteresis = TunerUtil.readTunerValByQuery("exhaust and fan and hysteresis", equip.equipRef)
            val oaoDamperMatTarget = TunerUtil.readTunerValByQuery("oao and outside and damper and mat and target",equip.equipRef)
            val oaoDamperMatMin = TunerUtil.readTunerValByQuery("oao and outside and damper and mat and min",equip.equipRef)
            val economizingMaxTemp = TunerUtil.readTunerValByQuery("oao and economizing and max and temp", equip.equipRef)

            val matTemp  = hsSplitHaystackUtil.getMixedAirTemp()

            Log.d(L.TAG_CCU_HSSPLIT_CPUECON,"outsideAirLoopOutput "+outsideAirLoopOutput+" oaoDamperMatTarget "+oaoDamperMatTarget+" oaoDamperMatMin "+oaoDamperMatMin
                    +" matTemp "+matTemp)

            matThrottle = false

            if (outsideAirLoopOutput > outsideDamperMinOpen) {
                if (matTemp < oaoDamperMatTarget && matTemp > oaoDamperMatMin) {
                    outsideAirFinalLoopOutput = (outsideAirLoopOutput - outsideAirLoopOutput * ((oaoDamperMatTarget - matTemp) / (oaoDamperMatTarget - oaoDamperMatMin))).toInt()
                } else {
                    outsideAirFinalLoopOutput = if (matTemp <= oaoDamperMatMin || matTemp > economizingMaxTemp) outsideDamperMinOpen else outsideAirLoopOutput
                }
                if (matTemp < oaoDamperMatTarget || matTemp > economizingMaxTemp) matThrottle = true
            } else {
                outsideAirFinalLoopOutput = outsideDamperMinOpen
            }

            outsideAirFinalLoopOutput = Math.max(outsideAirFinalLoopOutput , outsideDamperMinOpen)
            outsideAirFinalLoopOutput = Math.min(outsideAirFinalLoopOutput , 100)

            Log.d(L.TAG_CCU_HSSPLIT_CPUECON," economizingLoopOutput "+economizingLoopOutput+" dcvLoopOutput "+dcvLoopOutput
                    +" outsideAirFinalLoopOutput "+outsideAirFinalLoopOutput+" outsideDamperMinOpen "+outsideDamperMinOpen);

            equip.setHisVal("outside and air and final and loop", outsideAirFinalLoopOutput.toDouble())
            equip.setHisVal("oao and zone and logical and damper and actuator and cmd", outsideAirFinalLoopOutput.toDouble())

            if (outsideAirFinalLoopOutput > exhaustFanStage1Threshold) {
                equip.setHisVal("cmd and exhaust and fan and stage1", 1.0)
            } else if (outsideAirFinalLoopOutput < (exhaustFanStage1Threshold - exhaustFanHysteresis)) {
                equip.setHisVal("cmd and exhaust and fan and stage1",0.0);
            }

            if (outsideAirFinalLoopOutput > exhaustFanStage2Threshold) {
                equip.setHisVal("cmd and exhaust and fan and stage2",1.0)
            } else if (outsideAirFinalLoopOutput < (exhaustFanStage2Threshold - exhaustFanHysteresis)) {
                equip.setHisVal("cmd and exhaust and fan and stage2",0.0)
            }

            val matThrottleNumber = if (matThrottle) 1.0 else 0.0
            equip.setHisVal("mat and available", matThrottleNumber)

        }

    }

    private fun doEconomizing(equip: HyperStatSplitCpuEconEquip) {

        val externalTemp = CCUHsApi.getInstance().readHisValByQuery("system and outside and temp")
        val externalHumidity =
            CCUHsApi.getInstance().readHisValByQuery("system and outside and humidity")

        // Check for economizer enable
        if (canDoEconomizing(equip, externalTemp, externalHumidity)) {

            economizingAvailable = true

            val economizingToMainCoolingLoopMap = TunerUtil.readTunerValByQuery("oao and economizing and main and cooling and loop and map", equip.equipRef)

            val numberConfiguredCoolingStages = getNumberConfiguredCoolingStages(equip)

            if (numberConfiguredCoolingStages > 0) {
                economizingLoopOutput = Math.min((coolingLoopOutput * (numberConfiguredCoolingStages + 1)).toInt(), 100)
                Log.d(L.TAG_CCU_HSSPLIT_CPUECON, (numberConfiguredCoolingStages+1).toString() + " cooling stages available (including economizer); economizingLoopOutput = " + economizingLoopOutput);
            } else {
                Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "coolingLoopOutput = " + coolingLoopOutput + ", economizingToMainCoolingLoopMap = " + economizingToMainCoolingLoopMap + ", economizingLoopOutput = " + economizingLoopOutput);
                economizingLoopOutput = Math.min((coolingLoopOutput * (100 / economizingToMainCoolingLoopMap)).toInt(), 100)
            }

        } else {
            economizingAvailable = false
            economizingLoopOutput = 0
        }

        val economizingAvailableNumber = if (economizingAvailable) 1.0 else 0.0
        equip.setHisVal("economizing and available", economizingAvailableNumber)

    }

    /*
        Right now, it is possible to configure stages irregularly. (e.g. Stage 1 and Stage 3, but not Stage 2).

        If a user did this, it would mess with this mapping, but this will be far from the biggest operational issue
        introduced.

        We should probably validate against these kinds of configurations in the future.
     */
    private fun getNumberConfiguredCoolingStages(equip: HyperStatSplitCpuEconEquip): Int {
        if (HyperStatSplitAssociationUtil.isAnyRelayAssociatedToCoolingStage3(equip.getConfiguration())) return 3
        if (HyperStatSplitAssociationUtil.isAnyRelayAssociatedToCoolingStage2(equip.getConfiguration())) return 2
        if (HyperStatSplitAssociationUtil.isAnyRelayAssociatedToCoolingStage1(equip.getConfiguration())) return 1
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
    private fun canDoEconomizing(equip: HyperStatSplitCpuEconEquip, externalTemp: Double, externalHumidity: Double): Boolean {

        val economizingMinTemp = TunerUtil.readTunerValByQuery("oao and economizing and min and temp",equip.equipRef)

        val indoorTemp = hsSplitHaystackUtil.getCurrentTemp()
        val indoorHumidity = hsSplitHaystackUtil.getHumidity()

        val insideEnthalpy = getAirEnthalpy(indoorTemp, indoorHumidity)
        val outsideEnthalpy = getAirEnthalpy(externalTemp, externalHumidity)

        equip.setHisVal("inside and enthalpy", insideEnthalpy)
        equip.setHisVal("outside and enthalpy", outsideEnthalpy)

        Log.d(L.TAG_CCU_HSSPLIT_CPUECON," canDoEconomizing externalTemp "+externalTemp+" externalHumidity "+externalHumidity)

        // If zone isn't in cooling mode, stop right here
        if (state != ZoneState.COOLING) return false

        // First, check local dry-bulb temp
        if (isEconomizingEnabledOnDryBulb(equip, externalTemp, externalHumidity, economizingMinTemp)) {
            Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "Economizer enabled based on dry-bulb temperature.")
            return true
        }

        if (!isEconomizingTempAndHumidityInRange(equip, externalTemp, externalHumidity, economizingMinTemp)) return false

        if (isEconomizingEnabledOnEnthalpy(equip, insideEnthalpy, outsideEnthalpy)) {
            Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "Economizing enabled based on enthalpy.")
            return true
        }

        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "Economizing disabled based on enthalpy.")
        return false

    }

    /*
        Same dry-bulb enable logic as OAO Profile.

        If outsideTemp is between economizingMinTemp (0°F, adj.) and dryBulbTemperatureThreshold (55°F, adj.), then enable economizing.
        (for outsideTemp, start with systemOutsideTemp, and use Outside Air Temperature sensor if systemOutsideTemp is 0)
     */
    private fun isEconomizingEnabledOnDryBulb(equip: HyperStatSplitCpuEconEquip, externalTemp: Double, externalHumidity: Double, economizingMinTemp: Double): Boolean {

        var dryBulbTemperatureThreshold = TunerUtil.readTunerValByQuery("economizing and dry and bulb and threshold", equip.equipRef)

        var outsideAirTemp = externalTemp

        if (externalHumidity == 0.0 && externalTemp == 0.0) {
            Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "System outside temp and humidity are both zero; using local OAT sensor");
            outsideAirTemp = equip.getOutsideAirTempSensor()
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
    private fun isEconomizingTempAndHumidityInRange(equip: HyperStatSplitCpuEconEquip, externalTemp: Double, externalHumidity: Double, economizingMinTemp: Double): Boolean {

        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "Checking outside temp and humidity against tuner min/max thresholds");

        val economizingMaxTemp = TunerUtil.readTunerValByQuery("economizing and max and " +
                "temp",equip.equipRef)
        val economizingMinHumidity = TunerUtil.readTunerValByQuery("economizing and min and " +
                "humidity",equip.equipRef)
        val economizingMaxHumidity = TunerUtil.readTunerValByQuery("economizing and max and " +
                "humidity",equip.equipRef)

        var outsideTemp = externalTemp
        var outsideHumidity = externalHumidity

        if (
            outsideTemp == 0.0 &&
            outsideHumidity == 0.0 &&
            HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToOutsideAir(
                equip.getConfiguration().address0State,
                equip.getConfiguration().address1State,
                equip.getConfiguration().address2State)
        ) {
            outsideTemp = equip.getOutsideAirTempSensor()
            outsideHumidity = equip.getOutsideAirHumiditySensor()
        }

        if (outsideTemp > economizingMinTemp
            && outsideTemp < economizingMaxTemp
            && outsideHumidity > economizingMinHumidity
            && outsideHumidity < economizingMaxHumidity) return true

        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "Outside air (" + outsideTemp + "°F, " + externalHumidity + "%RH) out of temp/humidity range from tuners; economizing disabled");

        return false

    }

    /*
        Same enthalpy-enable condition as OAO Profile.

        If systemOutsideEnthalpy < insideEnthalpy + enthalpyDuctCompensationOffset (0 BTU/lb, adj.), enable economizing
        (Start with systemOutsideEnthalpy. If it's not available and OAT/H sensor is on sensor bus, then calculate outsideEnthalpy
        based on sensed Outside Air Temperature & Humidity.
     */
    private fun isEconomizingEnabledOnEnthalpy (equip: HyperStatSplitCpuEconEquip, insideEnthalpy: Double, outsideEnthalpy: Double): Boolean {

        Log.d(L.TAG_CCU_HSSPLIT_CPUECON,"Checking enthalpy-enable condition: insideEnthalpy "+insideEnthalpy+", outsideEnthalpy "+ outsideEnthalpy)

        var outsideEnthalpyToUse = outsideEnthalpy

        // Our enthalpy calc returns a value of 0.12 for zero temp and humidity.
        // We will assume anything less than 1 translates to system weather data being dead
        if (
            outsideEnthalpy < 1 &&
            HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToOutsideAir(
                equip.getConfiguration().address0State,
                equip.getConfiguration().address1State,
                equip.getConfiguration().address2State)
        ) {
            Log.d(L.TAG_CCU_HSSPLIT_CPUECON,"System outside temp and humidity are both zero; using local outside air temp/humidity sensor")
            val sensorBusOutsideTemp = equip.getOutsideAirTempSensor()
            val sensorBusOutsideHumidity = equip.getOutsideAirHumiditySensor()
            outsideEnthalpyToUse = getAirEnthalpy(sensorBusOutsideTemp, sensorBusOutsideHumidity)
        }

        val enthalpyDuctCompensationOffset = TunerUtil.readTunerValByQuery("oao and enthalpy and duct and compensation and offset",equip.equipRef)

        return insideEnthalpy > outsideEnthalpyToUse + enthalpyDuctCompensationOffset

    }

    /*
        DCV is enabled when:
            ○ Zone is in Occupied Mode AND
            ○ Zone CO2 (sensed on HyperLite) > Zone CO2 Threshold
     */
    private fun doDcv(equip: HyperStatSplitCpuEconEquip, standaloneOutsideAirDamperMinOpen: Int) {

        dcvAvailable = false
        var zoneSensorCO2 = hsSplitHaystackUtil.getZoneCO2()
        var zoneCO2Threshold = hsSplitHaystackUtil.getZoneCO2Threshold()
        var co2DamperOpeningRate = hsSplitHaystackUtil.getCO2DamperOpeningRate()
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "zoneSensorCO2: " + zoneSensorCO2 + ", zoneCO2Threshold: " + zoneCO2Threshold + ", co2DamperOpeningRate: " + co2DamperOpeningRate)
        if (occupancyStatus == Occupancy.OCCUPIED || occupancyStatus == Occupancy.FORCEDOCCUPIED || occupancyStatus == Occupancy.AUTOFORCEOCCUPIED) {
            if (zoneSensorCO2 > zoneCO2Threshold) {
                dcvAvailable = true
                dcvLoopOutput = ((zoneSensorCO2 - zoneCO2Threshold) / co2DamperOpeningRate).toInt()
                outsideAirCalculatedMinDamper = Math.max(dcvLoopOutput, standaloneOutsideAirDamperMinOpen)
            } else {
                outsideAirCalculatedMinDamper = standaloneOutsideAirDamperMinOpen
            }
        } else {
            outsideAirCalculatedMinDamper = 0
        }

        val dcvAvailableNum = if (dcvAvailable) 1.0 else 0.0
        equip.setHisVal("dcv and available", dcvAvailableNum)
        equip.setHisVal("dcv and loop and output", dcvLoopOutput.toDouble())
        equip.setHisVal("outside and air and min and damper and calculated", outsideAirCalculatedMinDamper.toDouble())

    }

    private fun fetchBasicSettings(equip: HyperStatSplitCpuEconEquip): BasicSettings {

        val userIntentConditioningMode = StandaloneConditioningMode.values()[equip.hsSplitHaystackUtil.getCurrentUserIntentConditioningMode().toInt()]

        return BasicSettings(
            userIntentConditioningMode = userIntentConditioningMode,
            effectiveConditioningMode = getEffectiveConditioningMode(equip, userIntentConditioningMode),
            fanMode = StandaloneFanStage.values()[equip.hsSplitHaystackUtil.getCurrentFanMode().toInt()]
        )
    }

    private fun fetchUserIntents(equip: HyperStatSplitCpuEconEquip): UserIntents {
        return UserIntents(
            currentTemp = equip.getCurrentTemp(),
            zoneCoolingTargetTemperature = equip.hsSplitHaystackUtil.getDesiredTempCooling(),
            zoneHeatingTargetTemperature = equip.hsSplitHaystackUtil.getDesiredTempHeating(),
            targetMinInsideHumidity = equip.hsSplitHaystackUtil.getTargetMinInsideHumidity(),
            targetMaxInsideHumidity = equip.hsSplitHaystackUtil.getTargetMaxInsideHumidity(),
        )
    }

    private fun fetchHyperStatSplitTuners(equip: HyperStatSplitCpuEconEquip): HyperStatSplitProfileTuners {

        /**
         * Consider that
         * proportionalGain = proportionalKFactor
         * integralGain = integralKFactor
         * proportionalSpread = temperatureProportionalRange
         * integralMaxTimeout = temperatureIntegralTime
         */

        val hsSplitTuners = HyperStatSplitProfileTuners()
        hsSplitTuners.proportionalGain = TunerUtil.getProportionalGain(equip.equipRef!!)
        hsSplitTuners.integralGain = TunerUtil.getIntegralGain(equip.equipRef!!)
        hsSplitTuners.proportionalSpread = TunerUtil.getProportionalSpread(equip.equipRef!!)
        hsSplitTuners.integralMaxTimeout = TunerUtil.getIntegralTimeout(equip.equipRef!!).toInt()
        hsSplitTuners.relayActivationHysteresis = TunerUtil.getHysteresisPoint("relay and  activation", equip.equipRef!!).toInt()
        hsSplitTuners.analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery("analog and fan and speed and multiplier", equip.equipRef!!)
        hsSplitTuners.humidityHysteresis = TunerUtil.getHysteresisPoint("humidity", equip.equipRef!!).toInt()
        return hsSplitTuners
    }

    private fun runRelayOperations(
        config: HyperStatSplitCpuEconConfiguration, tuner: HyperStatSplitProfileTuners, userIntents: UserIntents,
        basicSettings: BasicSettings,
        relayStages: HashMap<String, Int>
    ) {
        if (config.relay1State.enabled) {
            handleRelayState(
                config.relay1State, config, Port.RELAY_ONE, 
                tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay2State.enabled) {
            handleRelayState(
                config.relay2State, config, Port.RELAY_TWO, 
                tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay3State.enabled) {
            handleRelayState(
                config.relay3State, config, Port.RELAY_THREE,
                tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay4State.enabled) {
            handleRelayState(
                config.relay4State, config, Port.RELAY_FOUR,
                tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay5State.enabled) {
            handleRelayState(
                config.relay5State, config, Port.RELAY_FIVE,
                tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay6State.enabled) {
            handleRelayState(
                config.relay6State, config, Port.RELAY_SIX,
                tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay7State.enabled) {
            handleRelayState(
                config.relay7State, config, Port.RELAY_SEVEN,
                tuner, userIntents, basicSettings, relayStages
            )
        }
        if (config.relay8State.enabled) {
            handleRelayState(
                config.relay8State, config, Port.RELAY_EIGHT,
                tuner, userIntents, basicSettings, relayStages
            )
        }
    }

    private fun runAnalogOutOperations(
        equip: HyperStatSplitCpuEconEquip, config: HyperStatSplitCpuEconConfiguration,
        basicSettings: BasicSettings, analogOutStages: HashMap<String, Int>
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
        if (config.analogOut4State.enabled) {
            handleAnalogOutState(
                config.analogOut4State, equip, config, Port.ANALOG_OUT_FOUR, basicSettings, analogOutStages
            )
        }
    }

    private fun handleRelayState(
        relayState: RelayState, config: HyperStatSplitCpuEconConfiguration, port: Port, tuner: HyperStatSplitProfileTuners,
        userIntents: UserIntents, basicSettings: BasicSettings, relayStages: HashMap<String, Int>
    ) {
        when {
            (HyperStatSplitAssociationUtil.isRelayAssociatedToCoolingStage(relayState)) -> {

                if (basicSettings.effectiveConditioningMode.ordinal == StandaloneConditioningMode.COOL_ONLY.ordinal ||
                    basicSettings.effectiveConditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
                ) {
                    runRelayForCooling(relayState, port, config, tuner, relayStages)
                } else {
                    resetPort(port)
                }
            }
            (HyperStatSplitAssociationUtil.isRelayAssociatedToHeatingStage(relayState)) -> {

                if (basicSettings.effectiveConditioningMode.ordinal == StandaloneConditioningMode.HEAT_ONLY.ordinal ||
                    basicSettings.effectiveConditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
                ) {
                    runRelayForHeating(relayState, port, config, tuner, relayStages)
                } else {
                    resetPort(port)
                }
            }
            (HyperStatSplitAssociationUtil.isRelayAssociatedToFan(relayState)) -> {

                if (basicSettings.fanMode != StandaloneFanStage.OFF) {
                    runRelayForFanSpeed(relayState, port, config, tuner, relayStages, basicSettings)
                } else {
                   resetPort(port)
                }
            }
            (HyperStatSplitAssociationUtil.isRelayAssociatedToFanEnabled(relayState)) -> {
                doFanEnabled( curState,port, fanLoopOutput )
            }

            (HyperStatSplitAssociationUtil.isRelayAssociatedToOccupiedEnabled(relayState)) -> {
                doOccupiedEnabled(port)
            }
            (HyperStatSplitAssociationUtil.isRelayAssociatedToHumidifier(relayState)) -> {
                doHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMinInsideHumidity)
            }
            (HyperStatSplitAssociationUtil.isRelayAssociatedToDeHumidifier(relayState)) -> {
                doDeHumidifierOperation(port,tuner.humidityHysteresis,userIntents.targetMaxInsideHumidity)
            }
            (HyperStatSplitAssociationUtil.isRelayAssociatedToExhaustFanStage1(relayState)) -> {
                doExhaustFanStage1(port, outsideAirFinalLoopOutput, tuner.exhaustFanStage1Threshold, tuner.exhaustFanHysteresis)
            }
            (HyperStatSplitAssociationUtil.isRelayAssociatedToExhaustFanStage2(relayState)) -> {
                doExhaustFanStage2(port, outsideAirFinalLoopOutput, tuner.exhaustFanStage2Threshold, tuner.exhaustFanHysteresis)
            }
        }
    }

    private fun runRelayForCooling(
        relayAssociation: RelayState,
        whichPort: Port,
        config: HyperStatSplitCpuEconConfiguration,
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

        logIt(" $whichPort: ${relayAssociation.association}")
        when (relayAssociation.association) {
            CpuEconRelayAssociation.COOLING_STAGE_1 -> {
                doCoolingStage1(
                    whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, s1threshold, relayStages
                )
            }
            CpuEconRelayAssociation.COOLING_STAGE_2 -> {
               doCoolingStage2(
                   whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, s2threshold,relayStages
               )
            }
            CpuEconRelayAssociation.COOLING_STAGE_3 -> {
                doCoolingStage3(
                    whichPort, coolingLoopOutput, tuner.relayActivationHysteresis, s3threshold, relayStages
                )
            }
            else -> {}
        }
        if(getCurrentPortStatus(whichPort) == 1.0)
            curState = ZoneState.COOLING

    }

    private fun runRelayForHeating(
        relayAssociation: RelayState, whichPort: Port, config: HyperStatSplitCpuEconConfiguration,
        tuner: HyperStatSplitProfileTuners, relayStages: HashMap<String, Int>
    ) {
        logIt(" $whichPort: ${relayAssociation.association}")
        when (relayAssociation.association) {
            CpuEconRelayAssociation.HEATING_STAGE_1 -> {
                doHeatingStage1(
                    whichPort,
                    heatingLoopOutput,
                    tuner.relayActivationHysteresis,
                    relayStages
                )
            }
            CpuEconRelayAssociation.HEATING_STAGE_2 -> {
                val highestStage = HyperStatSplitAssociationUtil.getHighestHeatingStage(config).ordinal
                val divider = if (highestStage == 4) 50 else 33
                doHeatingStage2(
                    whichPort, heatingLoopOutput, tuner.relayActivationHysteresis,
                    divider, relayStages)
            }
            CpuEconRelayAssociation.HEATING_STAGE_3 -> {
                doHeatingStage3(
                    whichPort, heatingLoopOutput, tuner.relayActivationHysteresis, relayStages)
            }
            else -> {}
        }
        if(getCurrentPortStatus(whichPort) == 1.0) curState = ZoneState.HEATING
    }

    private fun runRelayForFanSpeed(
        relayAssociation: RelayState, whichPort: Port, config: HyperStatSplitCpuEconConfiguration,
        tuner: HyperStatSplitProfileTuners, relayStages: HashMap<String, Int>, basicSettings: BasicSettings,
    ) {
        logIt(" $whichPort: ${relayAssociation.association} runRelayForFanSpeed: ${basicSettings.fanMode}")
        if (basicSettings.fanMode == StandaloneFanStage.AUTO
            && basicSettings.effectiveConditioningMode == StandaloneConditioningMode.OFF ) {
            logIt("Cond is Off , Fan is Auto  : ")
            resetPort(whichPort)
            return
        }
        val highestStage = HyperStatSplitAssociationUtil.getHighestFanStage(config)
        val divider = if (highestStage == CpuEconRelayAssociation.FAN_MEDIUM_SPEED) 50 else 33

        when (relayAssociation.association) {
            CpuEconRelayAssociation.FAN_LOW_SPEED -> {
                doFanLowSpeed(
                    whichPort,
                    logicalPointsList[whichPort]!!,null,null, basicSettings.fanMode,
                    fanLoopOutput,tuner.relayActivationHysteresis,relayStages,divider)
            }
            CpuEconRelayAssociation.FAN_MEDIUM_SPEED -> {
                doFanMediumSpeed(
                    whichPort,
                    logicalPointsList[whichPort]!!,null,basicSettings.fanMode,
                    fanLoopOutput,tuner.relayActivationHysteresis,divider,relayStages)
            }
            CpuEconRelayAssociation.FAN_HIGH_SPEED -> {
                doFanHighSpeed(
                    whichPort,
                    logicalPointsList[whichPort]!!,basicSettings.fanMode,
                    fanLoopOutput,tuner.relayActivationHysteresis,relayStages)
            }
            else -> return
        }
    }

    private fun handleAnalogOutState(
        analogOutState: AnalogOutState, equip: HyperStatSplitCpuEconEquip, config: HyperStatSplitCpuEconConfiguration,
        port: Port, basicSettings: BasicSettings, analogOutStages: HashMap<String, Int>
    ) {
        // If we are in Auto Away mode we no need to Any analog Operations
        when {
            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToCooling(analogOutState)) -> {
                doAnalogCooling(port,basicSettings.effectiveConditioningMode,analogOutStages,coolingLoopOutput)
            }
            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToHeating(analogOutState)) -> {
                doAnalogHeating(port,basicSettings.effectiveConditioningMode,analogOutStages,heatingLoopOutput)
            }
            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState)) -> {
                doAnalogFanAction(
                    port, analogOutState.perAtFanLow.toInt(), analogOutState.perAtFanMedium.toInt(),
                    analogOutState.perAtFanHigh.toInt(), basicSettings.fanMode,
                    basicSettings.effectiveConditioningMode, fanLoopOutput, analogOutStages
                )
            }
            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToOaoDamper(analogOutState)) -> {
                doAnalogOAOAction(
                    port,basicSettings.effectiveConditioningMode, analogOutStages, outsideAirFinalLoopOutput
                )
            }
            (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(analogOutState)) -> {
                doAnalogStagedFanAction(
                    port, analogOutState.perAtFanLow.toInt(), analogOutState.perAtFanMedium.toInt(),
                    analogOutState.perAtFanHigh.toInt(), basicSettings.fanMode,
                    basicSettings.effectiveConditioningMode, fanLoopOutput, analogOutStages,
                )
            }
        }
    }

    private fun doAnalogStagedFanAction(
        port: Port,
        fanLowPercent: Int,
        fanMediumPercent: Int,
        fanHighPercent: Int,
        fanMode: StandaloneFanStage,
        conditioningMode: StandaloneConditioningMode,
        fanLoopOutput: Int,
        analogOutStages: HashMap<String, Int>,
    ) {
        if (fanMode != StandaloneFanStage.OFF) {
            var fanLoopForAnalog = 0
            if (fanMode == StandaloneFanStage.AUTO) {
                if (conditioningMode == StandaloneConditioningMode.OFF) {
                    updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
                    return
                }
                fanLoopForAnalog = fanLoopOutput
                if (conditioningMode == StandaloneConditioningMode.AUTO) {
                    if (getOperatingMode() == 1.0) {
                        fanLoopForAnalog =
                            getPercentageFromVoltageSelected(getCoolingStateActivated().roundToInt())
                    } else if (getOperatingMode() == 2.0) {
                        fanLoopForAnalog =
                            getPercentageFromVoltageSelected(getHeatingStateActivated().roundToInt())
                    }
                } else if (conditioningMode == StandaloneConditioningMode.COOL_ONLY) {
                    fanLoopForAnalog =
                        getPercentageFromVoltageSelected(getCoolingStateActivated().roundToInt())
                } else if (conditioningMode == StandaloneConditioningMode.HEAT_ONLY) {
                    fanLoopForAnalog =
                        getPercentageFromVoltageSelected(getHeatingStateActivated().roundToInt())
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
            updateLogicalPointIdValue(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
            Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "$port = Staged Fan Speed  analogSignal  $fanLoopForAnalog")
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
            updateLogicalPointIdValue(logicalPointsList[port]!!, coolingLoopOutput.toDouble())
            if (coolingLoopOutput > 0) {
                analogOutStages[AnalogOutput.COOLING.name] = coolingLoopOutput
                curState = ZoneState.COOLING
            }
        } else {
            updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
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
            updateLogicalPointIdValue(logicalPointsList[port]!!, heatingLoopOutput.toDouble())
            if (heatingLoopOutput > 0) {
                analogOutStages[AnalogOutput.HEATING.name] = heatingLoopOutput
                curState = ZoneState.HEATING
            }
        } else {
            updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
        }
    }

    private fun getOperatingMode(): Double {

        return hsSplitHaystackUtil.readHisVal("point and operating and mode")

    }

    fun doAnalogOAOAction(
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        outsideAirFinalLoopOutput: Int,
        dcvLoopOutput: Int
    ) {
        // If cooling is enabled, OAO damper should follow Economizer or DCV Loop (whichever is greater)
        if (conditioningMode.ordinal != StandaloneConditioningMode.COOL_ONLY.ordinal ||
            conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
        ) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, outsideAirFinalLoopOutput.toDouble())
            if (outsideAirFinalLoopOutput > 0) analogOutStages[AnalogOutput.OAO_DAMPER.name] = outsideAirFinalLoopOutput
        }
        // If Conditioning mode is HEAT_ONLY, OAO damper should operate only for DCV
        else if (conditioningMode.ordinal == StandaloneConditioningMode.HEAT_ONLY.ordinal) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, dcvLoopOutput.toDouble())
            if (outsideAirFinalLoopOutput > 0) analogOutStages[AnalogOutput.OAO_DAMPER.name] = dcvLoopOutput
        }
    }


    private fun handleDeadZone(equip: HyperStatSplitCpuEconEquip) {

       logIt("Zone is Dead ${equip.node}")
        state = ZoneState.TEMPDEAD
        resetAllLogicalPointValues(equip)
        equip.hsSplitHaystackUtil.setProfilePoint("operating and mode", 0.0)
        if (equip.hsSplitHaystackUtil.getEquipStatus() != state.ordinal.toDouble())
            equip.hsSplitHaystackUtil.setEquipStatus(state.ordinal.toDouble())
        val curStatus = equip.hsSplitHaystackUtil.getEquipLiveStatus()
        if (curStatus != "Zone Temp Dead") {
            equip.hsSplitHaystackUtil.writeDefaultVal("status and message and writable", "Zone Temp Dead")
        }
        equip.haystack.writeHisValByQuery(
            "point and not ota and status and his and group == \"${equip.node}\"",
            ZoneState.TEMPDEAD.ordinal.toDouble()
        )
    }

    /*
        effectiveConditioningMode is forced OFF if Condensate Overflow is detected.
        Upon a return to normal, revert effectiveConditioningMode to userIntentConditioningMode.
    */
    private fun getEffectiveConditioningMode(equip: HyperStatSplitCpuEconEquip, userIntentConditioningMode: StandaloneConditioningMode): StandaloneConditioningMode {
        val condensateOverflowStatus = hsSplitHaystackUtil.getCondensateOverflowStatus()
        if (condensateOverflowStatus == 1.0) return StandaloneConditioningMode.OFF

        return userIntentConditioningMode
    }

    override fun getEquip(): Equip? {
        for (nodeAddress in cpuEconDeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    @JsonIgnore
    override fun getNodeAddresses(): Set<Short?> {
        return cpuEconDeviceMap.keys
    }

    private fun resetAllLogicalPointValues(equip: HyperStatSplitCpuEconEquip) {
        equip.hsSplitHaystackUtil.updateConditioningLoopOutput(0,0,0,false,0)
        equip.hsSplitHaystackUtil.updateOaoLoopOutput(0,0,0,0)
        resetAllLogicalPointValues()
        HyperStatSplitUserIntentHandler.updateHyperStatSplitStatus(
            equipId = equip.equipRef!!,
            portStages = HashMap(),
            analogOutStages = HashMap(),
            temperatureState = ZoneTempState.TEMP_DEAD,
            economizingLoopOutput,
            hsSplitHaystackUtil.getCondensateOverflowStatus(),
            hsSplitHaystackUtil.getFilterStatus()
        )
    }

    @JsonIgnore
    override fun getCurrentTemp(): Double {
        for (nodeAddress in cpuEconDeviceMap.keys) {
            return cpuEconDeviceMap[nodeAddress]!!.getCurrentTemp()
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
        for (nodeAddress in cpuEconDeviceMap.keys) {
            if (cpuEconDeviceMap[nodeAddress] == null) {
                continue
            }
            if (cpuEconDeviceMap[nodeAddress]!!.getCurrentTemp() > 0) {
                tempTotal += cpuEconDeviceMap[nodeAddress]!!.getCurrentTemp()
                nodeCount++
            }
        }
        return if (nodeCount == 0) 0.0 else tempTotal / nodeCount
    }

    /**
     * Function just to print logs
     */
    private fun logIt(msg: String){
        Log.i(L.TAG_CCU_HSSPLIT_CPUECON,msg)
    }

    private fun getCoolingStateActivated (): Double {
        return if (stageActive("cooling and runtime and stage3")) {
            hsSplitHaystackUtil.readPointValue("fan and cooling and stage3")
        } else if (stageActive("cooling and runtime and stage2")) {
            hsSplitHaystackUtil.readPointValue("fan and cooling and stage2")
        } else if (stageActive("cooling and runtime and stage1")) {
            hsSplitHaystackUtil.readPointValue("fan and cooling and stage1")
        } else {
            defaultFanLoopOutput
        }
    }


    private fun getHeatingStateActivated (): Double {
        return if (stageActive("heating and runtime and stage3")) {
            hsSplitHaystackUtil.readPointValue("fan and heating and stage3")
        } else if (stageActive("heating and runtime and stage2")) {
            hsSplitHaystackUtil.readPointValue("fan and heating and stage2")
        } else if (stageActive("heating and runtime and stage1")){
            hsSplitHaystackUtil.readPointValue("fan and heating and stage1")
        } else {
            defaultFanLoopOutput
        }
    }

    private fun stageActive(fanStage: String): Boolean {
        return hsSplitHaystackUtil.readHisVal(fanStage) == 1.0
    }

    private fun getPercentageFromVoltageSelected(voltageSelected: Int): Int {
        val minVoltage = 0
        val maxVoltage = 10
        return (((voltageSelected - minVoltage).toDouble() / (maxVoltage - minVoltage)) * 100).roundToInt()
    }

}