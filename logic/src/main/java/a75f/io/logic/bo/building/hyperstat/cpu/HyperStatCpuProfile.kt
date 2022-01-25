package a75f.io.logic.bo.building.hyperstat.cpu

import a75f.io.api.haystack.*
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.*
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.common.*
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getActualFanMode
import a75f.io.logic.jobs.HyperStatScheduler
import a75f.io.logic.jobs.HyperStatScheduler.Companion.updateHyperstatUIPoints
import a75f.io.logic.jobs.ScheduleProcessJob
import a75f.io.logic.jobs.SystemScheduleUtil
import a75f.io.logic.tuners.TunerUtil
import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnore
import org.joda.time.DateTime
import org.projecthaystack.HNum
import org.projecthaystack.HRef
import java.util.*
import kotlin.collections.HashMap
import a75f.io.api.haystack.CCUHsApi





/**
 * @author tcase@75f.io
 * Created on 7/7/21.
 */
class HyperStatCpuProfile : ZoneProfile() {

    // One zone can have many hyperstat devices.  Each has its own address and equip representation
    private val cpuDeviceMap: MutableMap<Short, HyperStatCpuEquip> = mutableMapOf()

    private var coolingLoopOutput = 0
    private var heatingLoopOutput = 0
    private var fanLoopOutput = 0
    private lateinit var logicalPointsList: HashMap<Any, String>
    private var currentConditioningStatus = StandaloneConditioningMode.OFF
    lateinit var occuStatus: Occupied
    private val hyperstatCPUAlgorithm = HyperstatLoopController()
    lateinit var curState: ZoneState
    override fun getProfileType() = ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        val equip = cpuDeviceMap[address]
        return equip?.getConfiguration() as T
    }

    override fun updateZonePoints() {
        cpuDeviceMap.forEach { (_, equip) ->
            Log.i(L.TAG_CCU_HSCPU, "updateZonePoints: equip.equipRef ${equip.equipRef}")
            runHyperstatCPUAlgorithm(equip)
        }
    }

    @Override
    fun addEquip(node: Short): HyperStatCpuEquip {
        val equip = HyperStatCpuEquip(node)
        equip.initEquipReference()
        cpuDeviceMap[node] = equip
        return equip
    }

    fun addNewEquip(node: Short, room: String, floor: String, config: HyperStatCpuConfiguration) {
        addEquip(node).initializePoints(config, room, floor, node)
    }

    fun getHsCpuEquip(node: Short): HyperStatCpuEquip? {
        return cpuDeviceMap[node]
    }

    // Run the profile logic and algorithm for an equip.
    private fun runHyperstatCPUAlgorithm(equip: HyperStatCpuEquip) {
        Log.i(L.TAG_CCU_HSCPU, "************************ HSCPU Address: ${equip.node}**********************")
        logicalPointsList = equip.getLogicalPointList()

        val relayStages = HashMap<String, Int>()
        curState = ZoneState.DEADBAND

        if (Globals.getInstance().isTestMode) {
            Log.i(TAG, "Test mode is on: ")
            return
        }

        if (mInterface != null) mInterface.refreshView()

        if (isZoneDead) {
            handleDeadZone(equip)
            return
        }

        // get current state from data store (e.g. from Equip)
        val config = equip.getConfiguration()

        // get latest tuner points
        val hyperstatTuners = fetchHyperstatTuners(equip)

        val userIntents = fetchUserIntents(equip)

        val averageDesiredTemp = (
                userIntents.zoneCoolingTargetTemperature + userIntents.zoneHeatingTargetTemperature) / 2.0

        if (averageDesiredTemp != equip.hsHaystackUtil!!.getDesiredTemp()) {
            equip.hsHaystackUtil!!.setDesiredTemp(averageDesiredTemp)
        }

        occuStatus = equip.hsHaystackUtil!!.getOccupancyStatus()

        //StandaloneFanStage  StandaloneConditioningMode
        var basicSettings = fetchBasicSettings(equip)

        val fanModeSaved = FanModeCacheStorage().getFanModeFromCache(equip.equipRef!!)


        // Updating the current fan mode based on the current occupied status
        updateFanMode(equip, basicSettings, fanModeSaved)

        // Collect the update basic settings
        basicSettings = fetchBasicSettings(equip)

        hyperstatCPUAlgorithm.initialise(tuners = hyperstatTuners)
        hyperstatCPUAlgorithm.dumpLogs()
        if (currentTemp > userIntents.zoneCoolingTargetTemperature && state != ZoneState.COOLING) {
            hyperstatCPUAlgorithm.resetCoolingControl()
            state = ZoneState.COOLING
            Log.i(L.TAG_CCU_HSCPU,"Resetting cooling")
        } else if (currentTemp < userIntents.zoneHeatingTargetTemperature && state != ZoneState.HEATING) {
            hyperstatCPUAlgorithm.resetHeatingControl()
            state = ZoneState.HEATING
            Log.i(L.TAG_CCU_HSCPU,"Resetting cooling")
        }

        var loopValue =  hyperstatCPUAlgorithm.calculateCoolingLoopOutput(
            currentTemp, userIntents.zoneCoolingTargetTemperature
        ).toInt()

        Log.i(L.TAG_CCU_HSCPU, "Val cool $loopValue")
        coolingLoopOutput = if ( loopValue < 0 ) 0 else loopValue

        loopValue = hyperstatCPUAlgorithm.calculateHeatingLoopOutput(
            userIntents.zoneHeatingTargetTemperature, currentTemp
        ).toInt()

        Log.i(L.TAG_CCU_HSCPU, "Val heat $loopValue")
        heatingLoopOutput = if ( loopValue < 0 ) 0 else loopValue

        if (coolingLoopOutput == 0 || heatingLoopOutput == 0)
            fanLoopOutput = 0

        if (coolingLoopOutput > 0) {
            fanLoopOutput = ((coolingLoopOutput * hyperstatTuners.analogFanSpeedMultiplier.coerceAtMost(100.0)).toInt())
        }
        if (heatingLoopOutput > 0) {
            fanLoopOutput = (heatingLoopOutput * hyperstatTuners.analogFanSpeedMultiplier.coerceAtMost(100.0)).toInt()
        }

        /**
         * Validating the fan loop output
         * if conditioning mode is selected to heat only if zone asking for cooling fan should be off
         * if conditioning mode is selected to Cool only if zone asking for heating fan should be off
         * This is how fan is working in smartstat profile so followed same.
         */

        validateFanLoopOutPut(basicSettings,userIntents)

        equip.hsHaystackUtil!!.updateOccupancyDetection()

        runForDoorWindowSensor(config, equip)
        runForKeycardSensor(config, equip)
        equip.hsHaystackUtil!!.updateAllLoopOutput(coolingLoopOutput,heatingLoopOutput,fanLoopOutput)
        val currentOperatingMode = equip.hsHaystackUtil!!.getOccupancyModePointValue().toInt()

        Log.i(L.TAG_CCU_HSCPU,
            "Analog Fan speed multiplier  ${hyperstatTuners.analogFanSpeedMultiplier} \n"+
                 "Current Working mode : ${Occupancy.values()[currentOperatingMode]} \n"+
                 "isForcedOccupied : ${occuStatus.isForcedOccupied} \n"+
                 "isOccupancySensed : ${occuStatus.isOccupancySensed} \n"+
                 "isPreconditioning : ${occuStatus.isPreconditioning} \n"+
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
        val forcedOccupiedMinutes = TunerUtil.readTunerValByQuery("forced and occupied and time",
                                                                    equip.equipRef)
        if (config.isEnableAutoForceOccupied && forcedOccupiedMinutes > 0) {
            runAutoForceOccupyOperation(equip)
        } else {
            if (equip.hsHaystackUtil!!.getOccupancyModePointValue().toInt() == Occupancy.AUTOFORCEOCCUPIED.ordinal)
                resetOccupancy(equip)
        }

        if (config.isEnableAutoAway) {
            runAutoAwayOperation(equip)
        } else {
            if (equip.hsHaystackUtil!!.getOccupancyModePointValue().toInt() == Occupancy.AUTOAWAY.ordinal)
                resetOccupancy(equip)
        }

        runRelayOperations(equip, config, hyperstatTuners, userIntents, basicSettings, relayStages)

        runAnalogOutOperations(equip, config, basicSettings)

        var zoneOperatingMode = ZoneState.DEADBAND.ordinal
          if(currentTemp < averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.COOL_ONLY)
              zoneOperatingMode = ZoneState.HEATING.ordinal
          if(currentTemp >= averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY)
              zoneOperatingMode = ZoneState.COOLING.ordinal
        Log.i(L.TAG_CCU_HSCPU,
            "averageDesiredTemp $averageDesiredTemp" +
                "currentTemp $currentTemp"+
                "zoneOperatingMode ${ZoneState.values()[zoneOperatingMode]}"
        )
        equip.hsHaystackUtil!!.setProfilePoint("temp and operating and mode", zoneOperatingMode.toDouble())
        if (equip.hsHaystackUtil!!.getStatus() != curState.ordinal.toDouble())
            equip.hsHaystackUtil!!.setStatus(curState.ordinal.toDouble())
        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY

        Log.i(L.TAG_CCU_HSCPU, "Equip Running : $curState")
        HyperStatScheduler.updateHyperstatStatus(
            equipId = equip.equipRef!!,
            state = curState,
            portStages = relayStages,
            temperatureState = temperatureState
        )
        Log.i(L.TAG_CCU_HSCPU, "**********************************************")
    }

    private fun fetchBasicSettings(equip: HyperStatCpuEquip) =

        BasicSettings(
            conditioningMode = StandaloneConditioningMode.values()[equip.hsHaystackUtil!!.getCurrentConditioningMode().toInt()],
            fanMode = StandaloneFanStage.values()[equip.hsHaystackUtil!!.getCurrentFanMode().toInt()]
        )

    private fun fetchUserIntents(equip: HyperStatCpuEquip): UserIntents {

        return UserIntents(
            currentTemp = equip.getCurrentTemp(),
            zoneCoolingTargetTemperature = equip.hsHaystackUtil!!.getDesiredTempCooling(),
            zoneHeatingTargetTemperature = equip.hsHaystackUtil!!.getDesiredTempHeating(),
            targetMinInsideHumidity = equip.hsHaystackUtil!!.getTargetMinInsideHumidity(),
            targetMaxInsideHumidity = equip.hsHaystackUtil!!.getTargetMaxInsideHumidity(),
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

    private fun runAutoForceOccupyOperation(equip: HyperStatCpuEquip) {
        val occupantDetected = (equip.hsHaystackUtil!!.getOccupancySensorPointValue().toInt() > 0)
        val currentOperatingMode = equip.hsHaystackUtil!!.getOccupancyModePointValue().toInt()
        Log.i(L.TAG_CCU_HSCPU,
            "Auto Force: Detected : $occupantDetected \n"+
                 "Auto Force:cur mode  : ${Occupancy.values()[currentOperatingMode]}")

        val detectionPointDetails = equip.hsHaystackUtil!!.readDetectionPointDetails()


        if ((!occuStatus.isOccupied || occuStatus.vacation != null)
            && currentOperatingMode != Occupancy.AUTOAWAY.ordinal
            && currentOperatingMode != Occupancy.PRECONDITIONING.ordinal
            /**
             * Autoforce occupy and force occupy will only occur in unoccupied state
             * Discussed with Product team on 04.01.2022 Reference
             * Bug 9727: Hyperstat User Intent Override Issue
             */
            && currentOperatingMode != Occupancy.FORCEDOCCUPIED.ordinal
        ) {

            val temporaryHoldTime = ScheduleProcessJob.getTemporaryHoldExpiry(HSUtil.getEquipInfo(equip.equipRef))
            val differenceInMinutes = findDifference(temporaryHoldTime, true)

            if (occupantDetected) {
                // If we are not is in force occupy then fall into force occupy
                if (currentOperatingMode != Occupancy.AUTOFORCEOCCUPIED.ordinal) {

                    equip.hsHaystackUtil!!.setOccupancyMode(Occupancy.AUTOFORCEOCCUPIED.ordinal.toDouble())
                    updateDesiredTemp(equip.hsHaystackUtil!!.getDesiredTemp(), equip, true)
                    Log.i(L.TAG_CCU_HSCPU, "Falling in FORCE Occupy mode")
                } else {
                    Log.i(L.TAG_CCU_HSCPU, "We are already in force occupy")
                    val occupancyHistory: HisItem? = equip.haystack.curRead(detectionPointDetails["id"].toString())
                    if (occupancyHistory == null) {
                        Log.i(L.TAG_CCU_HSCPU, "occupancy History Does not exist..")
                    }
                    val lastUpdatedTime: Date? = occupancyHistory?.date

                    Log.i(L.TAG_CCU_HSCPU,
                        "temporaryHoldTime time $temporaryHoldTime \n"+
                             "occupancy last sensed time $lastUpdatedTime \n"+
                             "Expiry time  ${DateTime(temporaryHoldTime)} \n"+
                             "Cur time  ${DateTime(System.currentTimeMillis())} \n"+
                         "differenceInMinutes : $differenceInMinutes \n")

                    Log.i(L.TAG_CCU_HSCPU, "Occupant Detected so extending the time")
                    val desiredAvgTemp = equip.hsHaystackUtil!!.getDesiredTemp()
                    updateDesiredTemp(desiredAvgTemp, equip, false)

                }

            } else {
                Log.i(
                    L.TAG_CCU_HSCPU,
                    "Occupant not detected in unoccupied mode expires in $differenceInMinutes"
                )
                if (currentOperatingMode == Occupancy.AUTOFORCEOCCUPIED.ordinal && differenceInMinutes <= 0) {
                    resetOccupancy(equip)
                }
            }
        } else {
            // Reset everything if there was force occupied condition
            Log.i(L.TAG_CCU_HSCPU, "We are in to occupied")
            if (currentOperatingMode == Occupancy.AUTOFORCEOCCUPIED.ordinal) {
                Log.i(L.TAG_CCU_HSCPU, "Move to to occupied status")
                resetOccupancy(equip)
            }
        }

    }

    private fun findDifference(expiryTime: Long, isCurrentGreater: Boolean): Long {
        val expiryTimeInDate = Date(expiryTime)
        val currentTime = Date(System.currentTimeMillis())
        val diff: Long =
            if (isCurrentGreater) expiryTimeInDate.time - currentTime.time else currentTime.time - expiryTimeInDate.time
        return ((diff / 1000) / 60)
    }

    private fun resetOccupancy(equip: HyperStatCpuEquip) {
        Log.i(L.TAG_CCU_HSCPU, "Resetting the resetForceOccupy ")
        equip.hsHaystackUtil!!.setOccupancyMode(Occupancy.UNOCCUPIED.ordinal.toDouble())

        val coolDT = equip.haystack
            .read("point and desired and cooling and temp and equipRef == \"${equip.equipRef}\"")
        val heatDT = equip.haystack
            .read("point and desired and heating and temp and equipRef == \"${equip.equipRef}\"")
        val avg = equip.haystack
            .read("point and desired and average and temp and equipRef == \"${equip.equipRef}\"")

        SystemScheduleUtil.clearOverrides(heatDT["id"].toString())
        SystemScheduleUtil.clearOverrides(coolDT["id"].toString())
        SystemScheduleUtil.clearOverrides(avg["id"].toString())

        val heatingPriorityValue = equip.hsHaystackUtil!!.getDesiredTempHeatingPriorityValue(equip.equipRef!!)
        val coolingPriorityValue = equip.hsHaystackUtil!!.getDesiredTempCoolingPriorityValue(equip.equipRef!!)
        val desiredTempPriorityValue = equip.hsHaystackUtil!!.getAverageDesiredTempPriorityValue(equip.equipRef!!)

        Log.i(L.TAG_CCU_HSCPU, "resetOccupancy: equip.equipRef!! ${equip.equipRef!!}  "
        +"\n resetOccupancy: HeatingPriorityValue $heatingPriorityValue  "
        +"\n resetOccupancy: CoolingPriorityValue $coolingPriorityValue  "
        +"\n resetOccupancy: DesiredTempPriorityValue $desiredTempPriorityValue  ")

        equip.haystack.writeHisValById(heatDT["id"].toString(),heatingPriorityValue)
        equip.haystack.writeHisValById(coolDT["id"].toString(),coolingPriorityValue)
        equip.haystack.writeHisValById(avg["id"].toString(),desiredTempPriorityValue)

    }

    private fun runAutoAwayOperation(equip: HyperStatCpuEquip) {
        val occupantDetected = (equip.hsHaystackUtil!!.getOccupancySensorPointValue().toInt() > 0)
        val currentOperatingMode = equip.hsHaystackUtil!!.getOccupancyModePointValue().toInt()
        val occupancy = equip.hsHaystackUtil!!.readDetectionPointDetails()
        Log.i(L.TAG_CCU_HSCPU, "Auto Away :Detection  : $occupantDetected"
                +"\n Auto Away :cur mode  : $currentOperatingMode")
        val detectionPointId = equip.hsHaystackUtil!!.readPointID("occupancy and detection and his")
        if (occuStatus.isOccupied && (currentOperatingMode != Occupancy.AUTOFORCEOCCUPIED.ordinal)) {

            val occupancyHistory: HisItem? = equip.haystack.curRead(occupancy["id"].toString())

            if (occupancyHistory == null) {
                Log.i(L.TAG_CCU_HSCPU, "occupancy History Does not exist..")
            }
            val lastUpdatedTime: Date? = occupancyHistory?.date
            Log.i(L.TAG_CCU_HSCPU, "Last Detected Time : $lastUpdatedTime")
            val differenceInMinutes = findDifference(lastUpdatedTime!!.time, false)
            val autoAwayTime = TunerUtil.readTunerValByQuery(
                " point and tuner and auto and away and time", equip.equipRef
            )
            Log.i(L.TAG_CCU_HSCPU, "findDifference : $differenceInMinutes autoAwayTime Tuner value: $autoAwayTime")
            if (differenceInMinutes > autoAwayTime && currentOperatingMode != Occupancy.AUTOAWAY.ordinal) {
                Log.i(L.TAG_CCU_HSCPU, "Moving to Auto Away state")
                val coolingDtPoint = CCUHsApi.getInstance().read(
                    "point and air and temp and " +
                            "desired and cooling and sp and equipRef  == \"${equip.equipRef}\"")
                if (coolingDtPoint == null || coolingDtPoint.size == 0) {
                    throw java.lang.IllegalArgumentException()
                }
                val heatingDtPoint = CCUHsApi.getInstance().read(
                    ("point and air and temp and " +
                            "desired and heating and sp and equipRef  == \"${equip.equipRef}\"")
                )
                if (heatingDtPoint == null || heatingDtPoint.size == 0) {
                    throw java.lang.IllegalArgumentException()
                }

                equip.haystack.writeHisValueByIdWithoutCOV(detectionPointId, 0.0)
                equip.hsHaystackUtil!!.setOccupancyMode(Occupancy.AUTOAWAY.ordinal.toDouble())
                resetPresentConditioningStatus(equip)
            } else {
                if (occupantDetected && currentOperatingMode != Occupancy.OCCUPIED.ordinal) {
                    equip.hsHaystackUtil!!.setOccupancyMode(Occupancy.OCCUPIED.ordinal.toDouble())
                    clearLevel3DesiredTemp(CCUHsApi.getInstance(),equip.equipRef!!)
                }
            }
        } else{
            Log.i(L.TAG_CCU_HSCPU, "Moving to unoccupied and clearing level 3 override")
            CCUHsApi.getInstance().writeHisValById(detectionPointId, Occupancy.UNOCCUPIED.ordinal.toDouble())
            Log.i(L.TAG_CCU_HSCPU, "clearLevel3DesiredTemp:2 ")

            clearLevel3DesiredTemp(CCUHsApi.getInstance(),equip.equipRef!!)
        }
    }

    private fun updateDesiredTemp(desiredTemp: Double, equip: HyperStatCpuEquip,
                                  isFirstTimeMovingToAutoForceOccupancy: Boolean) {

        val coolingDesiredTemp : Double
        val heatingDesiredTemp : Double

        if (isFirstTimeMovingToAutoForceOccupancy){
            coolingDesiredTemp = occuStatus.coolingVal
            heatingDesiredTemp = occuStatus.heatingVal
        } else {
            coolingDesiredTemp = equip.hsHaystackUtil!!.getDesiredTempCooling()
            heatingDesiredTemp = equip.hsHaystackUtil!!.getDesiredTempHeating()
        }

        val coolingDesiredPointMap = equip.hsHaystackUtil!!.getCoolingDeadbandPoint()
        val heatingDesiredPointMap = equip.hsHaystackUtil!!.getHeatingDeadbandPoint()
        val avgTempPointMap = equip.hsHaystackUtil!!.getAvgDesiredTempPoint()

        if (coolingDesiredPointMap == null || heatingDesiredPointMap == null || avgTempPointMap == null ||
            coolingDesiredPointMap.size == 0 || heatingDesiredPointMap.size == 0 || avgTempPointMap.size == 0
        )
            throw IllegalArgumentException()

        val desiredCoolingPoint = Point.Builder().setHashMap(coolingDesiredPointMap).build()
        val desiredHeatingPoint = Point.Builder().setHashMap(heatingDesiredPointMap).build()
        val avgTempPoint = Point.Builder().setHashMap(avgTempPointMap).build()

        equip.haystack.writeHisValById(desiredCoolingPoint.id, coolingDesiredTemp)
        equip.haystack.writeHisValById(desiredHeatingPoint.id, heatingDesiredTemp)
        equip.haystack.writeHisValById(avgTempPoint.id, desiredTemp)

        Log.i(L.TAG_CCU_HSCPU, " --- Update Desired temp---\n" +
                "coolingDesiredTemp $coolingDesiredTemp heatingDesiredTemp $heatingDesiredTemp desiredTemp $desiredTemp")

        SystemScheduleUtil.handleManualDesiredTempUpdate(
            desiredCoolingPoint, desiredHeatingPoint,
            avgTempPoint, coolingDesiredTemp, heatingDesiredTemp, desiredTemp
        )
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
    ) {

        if (config.analogOut1State.enabled) {
            handleAnalogOutState(config.analogOut1State, equip, config, Port.ANALOG_OUT_ONE, basicSettings)
        }
        if (config.analogOut2State.enabled) {
            handleAnalogOutState(config.analogOut2State, equip, config, Port.ANALOG_OUT_TWO, basicSettings)
        }
        if (config.analogOut3State.enabled) {
            handleAnalogOutState(config.analogOut3State, equip, config, Port.ANALOG_OUT_THREE, basicSettings)
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
        val currentOperatingMode = equip.hsHaystackUtil!!.getOccupancyModePointValue().toInt()
        Log.i(TAG, "Current Mode handleRelayState: $currentOperatingMode")
        when {
            (HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(relayState)) -> {

                if (basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.COOL_ONLY.ordinal ||
                    basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
                ) {
                    runRelayForCooling(relayState, equip, port, config, tuner, relayStages)
                }else{
                    updateLogicalPointIdValue(equip, logicalPointsList[port]!!, 0.0)
                }
            }
            (HyperStatAssociationUtil.isRelayAssociatedToHeatingStage(relayState)) -> {

                if (basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.HEAT_ONLY.ordinal ||
                    basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
                ) {
                    runRelayForHeating(relayState, equip, port, config, tuner, relayStages)
                }else{
                    updateLogicalPointIdValue(equip, logicalPointsList[port]!!, 0.0)
                }
            }
            (HyperStatAssociationUtil.isRelayAssociatedToFan(relayState)) -> {

                if (basicSettings.fanMode != StandaloneFanStage.OFF) {
                    runRelayForFanSpeed(relayState, equip, port, config, tuner, relayStages, basicSettings)
                }else{
                    updateLogicalPointIdValue(equip, logicalPointsList[port]!!, 0.0)
                }
            }
            (HyperStatAssociationUtil.isRelayAssociatedToFanEnabled(relayState)) -> {
                runRelayFanEnabled(relayState, equip, port)
            }

            (HyperStatAssociationUtil.isRelayAssociatedToOccupiedEnabled(relayState)) -> {
                runRelayOccupiedEnabled(equip, port)
            }
            (HyperStatAssociationUtil.isRelayAssociatedToHumidifier(relayState)) -> {
                runRelayHumidifier(equip, port, tuner, userIntents)
            }
            (HyperStatAssociationUtil.isRelayAssociatedToDeHumidifier(relayState)) -> {
                runRelayDeHumidifier(equip, port, tuner, userIntents)
            }

        }

    }

    private fun runRelayForCooling(
        relayAssociation: RelayState,
        equip: HyperStatCpuEquip,
        whichPort: Port,
        config: HyperStatCpuConfiguration,
        tuner: HyperStatProfileTuners,
        relayStages: HashMap<String, Int>
    ) {

        Log.i(TAG, " $whichPort: ${relayAssociation.association}")
        when (relayAssociation.association) {
            CpuRelayAssociation.COOLING_STAGE_1 -> {
                var relayState = -1.0
                if (coolingLoopOutput > tuner.relayActivationHysteresis)
                    relayState = 1.0    // Turn ON relay
                if (coolingLoopOutput == 0)
                    relayState = 0.0   // Turn OFF relay
                if (relayState != -1.0) {
                    updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, relayState)
                    if (relayState == 1.0) {
                        currentConditioningStatus = StandaloneConditioningMode.COOL_ONLY
                        relayStages[Stage.COOLING_1.displayName] = 1
                    }
                    curState = ZoneState.COOLING
                    Log.i(L.TAG_CCU_HSCPU, "$whichPort = ${relayAssociation.association}  $relayState")
                }

            }
            CpuRelayAssociation.COOLING_STAGE_2 -> {
                // possibility 1,2
                val highestStage = HyperStatAssociationUtil.getHighestCoolingStage(config).ordinal
                val divider = if (highestStage == 1) 50 else 33
                var relayState = -1.0
                if (coolingLoopOutput > (divider + (tuner.relayActivationHysteresis / 2)))
                    relayState = 1.0
                if (coolingLoopOutput <= (divider - (tuner.relayActivationHysteresis / 2)))
                    relayState = 0.0
                if (relayState != -1.0) {
                    updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, relayState)
                    if (relayState == 1.0) {
                        currentConditioningStatus = StandaloneConditioningMode.COOL_ONLY
                        relayStages[Stage.COOLING_2.displayName] = 1
                    }
                    Log.i(L.TAG_CCU_HSCPU, "$whichPort = ${relayAssociation.association}  $relayState")
                    curState = ZoneState.COOLING
                }
            }
            CpuRelayAssociation.COOLING_STAGE_3 -> {
                var relayState = -1.0
                if (coolingLoopOutput > (66 + (tuner.relayActivationHysteresis / 2)))
                    relayState = 1.0
                if (coolingLoopOutput <= (66 - (tuner.relayActivationHysteresis / 2)))
                    relayState = 0.0
                if (relayState != -1.0 ) {
                    updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, relayState)
                    if (relayState == 1.0) {
                        currentConditioningStatus = StandaloneConditioningMode.COOL_ONLY
                        relayStages[Stage.COOLING_3.displayName] = 1
                    }
                    Log.i(L.TAG_CCU_HSCPU, "$whichPort = ${relayAssociation.association}  $relayState")
                    curState = ZoneState.COOLING
                }
            }
            else -> return
        }

    }

    private fun runRelayForHeating(
        relayAssociation: RelayState,
        equip: HyperStatCpuEquip,
        whichPort: Port,
        config: HyperStatCpuConfiguration,
        tuner: HyperStatProfileTuners,
        relayStages: HashMap<String, Int>
    ) {

        Log.i(TAG, " $whichPort: ${relayAssociation.association}")
        when (relayAssociation.association) {

            CpuRelayAssociation.HEATING_STAGE_1 -> {
                var relayState = -1.0
                if (heatingLoopOutput > tuner.relayActivationHysteresis)
                    relayState = 1.0
                if (heatingLoopOutput == 0)
                    relayState = 0.0

                if (relayState != -1.0) {
                    updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, relayState)
                    if (relayState == 1.0) {
                        currentConditioningStatus = StandaloneConditioningMode.HEAT_ONLY
                        relayStages[Stage.HEATING_1.displayName] = 1
                    }
                    Log.i(L.TAG_CCU_HSCPU, "$whichPort = ${relayAssociation.association}  $relayState")
                    curState = ZoneState.HEATING
                }
            }
            CpuRelayAssociation.HEATING_STAGE_2 -> {
                // possibility 4,5,
                val highestStage = HyperStatAssociationUtil.getHighestHeatingStage(config).ordinal
                val divider = if (highestStage == 4) 50 else 33

                var relayState = -1.0
                if (heatingLoopOutput > (divider + (tuner.relayActivationHysteresis / 2)))
                    relayState = 1.0
                if (heatingLoopOutput <= (divider - (tuner.relayActivationHysteresis / 2)))
                    relayState = 0.0
                if (relayState != -1.0) {
                    updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, relayState)
                    if (relayState == 1.0) {
                        currentConditioningStatus = StandaloneConditioningMode.HEAT_ONLY
                        relayStages[Stage.HEATING_2.displayName] = 1
                    }
                    Log.i(L.TAG_CCU_HSCPU, "$whichPort = ${relayAssociation.association}  $relayState")
                    curState = ZoneState.HEATING
                }
            }
            CpuRelayAssociation.HEATING_STAGE_3 -> {
                var relayState = -1.0
                if (heatingLoopOutput > (66 + (tuner.relayActivationHysteresis / 2)))
                    relayState = 1.0
                if (heatingLoopOutput <= (66 - (tuner.relayActivationHysteresis / 2)))
                    relayState = 0.0
                if (relayState != -1.0) {
                    updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, relayState)
                    if (relayState == 1.0) {
                        currentConditioningStatus = StandaloneConditioningMode.HEAT_ONLY
                        relayStages[Stage.HEATING_3.displayName] = 1
                    }
                    Log.i(L.TAG_CCU_HSCPU, "$whichPort = ${relayAssociation.association}  $relayState")
                    curState = ZoneState.HEATING
                }
            }
            else -> return
        }

    }


    private fun runRelayForFanSpeed(
        relayAssociation: RelayState,
        equip: HyperStatCpuEquip,
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
            updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, 0.0)
            return
        }

        when (relayAssociation.association) {
            CpuRelayAssociation.FAN_LOW_SPEED -> {

                var relayState = -1.0
                if (basicSettings.fanMode == StandaloneFanStage.AUTO) {
                    if (fanLoopOutput > tuner.relayActivationHysteresis)
                        relayState = 1.0
                    if (fanLoopOutput == 0)
                        relayState = 0.0
                } else {
                    relayState = if (basicSettings.fanMode == StandaloneFanStage.LOW_CUR_OCC
                        || basicSettings.fanMode == StandaloneFanStage.LOW_OCC
                        || basicSettings.fanMode == StandaloneFanStage.LOW_ALL_TIME
                    ) 1.0 else 0.0
                }
                if (relayState != -1.0) {
                    updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, relayState)
                    Log.i(L.TAG_CCU_HSCPU, "$whichPort = ${relayAssociation.association}  $relayState")
                    if (relayState == 1.0) {
                        relayStages[Stage.FAN_1.displayName] = 1
                    }
                }
            }
            CpuRelayAssociation.FAN_MEDIUM_SPEED -> {
                var relayState = -1.0
                if (basicSettings.fanMode == StandaloneFanStage.AUTO) {
                    // possibility 7,8
                    val highestStage = HyperStatAssociationUtil.getHighestFanStage(config).ordinal
                    val divider = if (highestStage == 7) 50 else 33

                    if (fanLoopOutput > (divider + (tuner.relayActivationHysteresis / 2)))
                        relayState = 1.0
                    if (fanLoopOutput <= (divider - (tuner.relayActivationHysteresis / 2)))
                        relayState = 0.0
                } else {
                    relayState = if (basicSettings.fanMode == StandaloneFanStage.MEDIUM_CUR_OCC
                        || basicSettings.fanMode == StandaloneFanStage.MEDIUM_OCC
                        || basicSettings.fanMode == StandaloneFanStage.MEDIUM_ALL_TIME
                    ) 1.0 else 0.0
                }
                if(relayState != -1.0) {
                    updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, relayState)
                    Log.i(L.TAG_CCU_HSCPU, "$whichPort = ${relayAssociation.association}  $relayState")
                    if (relayState == 1.0) {
                        relayStages[Stage.FAN_2.displayName] = 1
                    }
                }
            }
            CpuRelayAssociation.FAN_HIGH_SPEED -> {
                var relayState = -1.0
                if (basicSettings.fanMode == StandaloneFanStage.AUTO) {
                    if (fanLoopOutput > (66 + (tuner.relayActivationHysteresis / 2)))
                        relayState = 1.0
                    if (fanLoopOutput <= (66 - (tuner.relayActivationHysteresis / 2)))
                        relayState = 0.0
                } else {
                    relayState = if (basicSettings.fanMode == StandaloneFanStage.HIGH_CUR_OCC
                        || basicSettings.fanMode == StandaloneFanStage.HIGH_OCC
                        || basicSettings.fanMode == StandaloneFanStage.HIGH_ALL_TIME
                    ) 1.0 else 0.0
                }
                if (relayState != -1.0) {
                    updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, relayState)
                    Log.i(L.TAG_CCU_HSCPU, "$whichPort = ${relayAssociation.association}  $relayState")
                    if (relayState == 1.0) {
                        relayStages[Stage.FAN_3.displayName] = 1
                    }
                }
            }
            else -> return
        }
    }

    private fun runRelayFanEnabled(
        relayAssociation: RelayState,
        equip: HyperStatCpuEquip,
        whichPort: Port,
    ) {
        Log.i(TAG, " $whichPort: ${relayAssociation.association}")
        // Then Relay will be turned On when the zone is in occupied mode Or
        // any conditioning is happening during an unoccupied schedule

        if (occuStatus.isOccupied || fanLoopOutput > 0) {
            updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, 1.0)
            Log.i(L.TAG_CCU_HSCPU, "$whichPort = ${relayAssociation.association}  1.0")
        } else if (!occuStatus.isOccupied || (curState == ZoneState.COOLING && curState == ZoneState.HEATING)) {
            Log.i(L.TAG_CCU_HSCPU, "$whichPort = ${relayAssociation.association}  0.0")
            updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, 0.0)
        }

    }

    private fun runRelayOccupiedEnabled(
        equip: HyperStatCpuEquip,
        whichPort: Port
    ) {
        updateLogicalPointIdValue(
            equip, logicalPointsList[whichPort]!!,
            if (occuStatus.isOccupied) 1.0 else 0.0
        )
        Log.i(L.TAG_CCU_HSCPU, "$whichPort = OccupiedEnabled" + if (occuStatus.isOccupied) 1.0 else 0.0)
    }

    private fun runRelayHumidifier(
        equip: HyperStatCpuEquip,
        whichPort: Port,
        tuner: HyperStatProfileTuners,
        userIntents: UserIntents

    ) {

        /**
        The humidifier is turned on whenever the humidity level for the CPU drops below the targetMinInsideHumidty.
        The humidifier will be turned off after being turned on when humidity goes humidityHysteresis above
        the targetMinInsideHumidty.
        If Relay Humidifier is selected, then show the user a target humidity control in Zone Screen on CCU
        and Facilisight apps. Relay is turned on if humidity crosses over that threshold and
        turns off when it drops 5% below threshold
         */

        val currentHumidity = equip.hsHaystackUtil!!.getHumidity()
        val currentPortStatus = getCurrentPortStatus(equip, logicalPointsList[whichPort]!!)
        Log.i(L.TAG_CCU_HSCPU,
            "runRelayHumidifier: currentHumidity : $currentHumidity \n"+
                "currentPortStatus : $currentPortStatus \n"+
                "targetMinInsideHumidity : ${userIntents.targetMinInsideHumidity} \n"+
                "Hysteresis : ${tuner.humidityHysteresis} \n")

        var relayStatus = 0.0
        if (currentHumidity > 0 && occuStatus.isOccupied) {
            if (currentHumidity < userIntents.targetMinInsideHumidity) {
                relayStatus = 1.0
            } else if (currentPortStatus > 0) {
                relayStatus =
                    if (currentHumidity > userIntents.targetMinInsideHumidity + tuner.humidityHysteresis) 0.0 else 1.0
            }
        } else relayStatus = 0.0

        updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, relayStatus)
        Log.i(L.TAG_CCU_HSCPU, "$whichPort = Humidifier  $relayStatus")
    }

    private fun runRelayDeHumidifier(
        equip: HyperStatCpuEquip,
        whichPort: Port,
        tuner: HyperStatProfileTuners,
        userIntents: UserIntents
    ) {
        /**
        If the dehumidifier is selected, then it is turned on whenever the humidity level for the CPU goes
        above the targetMaxInsideHumidty. The humidifier will be turned off after being turned
        on when humidity drops humidityHysteresis below the targetMaxInsideHumidty  .
        If Relay Dehumidifier is selected, then show the user a target humidity control
        in Zone Screen on CCU and Facilisight apps (but not user app). Relay is turned on
        if humidity drops below that threshold and turns off when it crosses over 5% above the threshold
         */

        val currentHumidity = equip.hsHaystackUtil!!.getHumidity()
        val currentPortStatus = getCurrentPortStatus(equip, logicalPointsList[whichPort]!!)
        Log.i(L.TAG_CCU_HSCPU,
            "currentHumidity : $currentHumidity \n" +
                    "| currentPortStatus : $currentPortStatus \n" +
                    "|targetMaxInsideHumidity : ${userIntents.targetMaxInsideHumidity} \n" +
                    "| Hysteresis : ${tuner.humidityHysteresis} \n")
        var relayStatus = 0.0
        if (currentHumidity > 0 && occuStatus.isOccupied) {
            if (currentHumidity > userIntents.targetMaxInsideHumidity) {
                relayStatus = 1.0
            } else if (currentPortStatus > 0) {
                relayStatus =
                    if (currentHumidity < userIntents.targetMaxInsideHumidity - tuner.humidityHysteresis) 0.0 else 1.0
            }
        } else relayStatus = 0.0

        updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, relayStatus)
        Log.i(L.TAG_CCU_HSCPU, "$whichPort = DeHumidifier  $relayStatus")
    }


    private fun handleAnalogOutState(
        analogOutState: AnalogOutState,
        equip: HyperStatCpuEquip,
        config: HyperStatCpuConfiguration,
        port: Port,
        basicSettings: BasicSettings,
    ) {
        // If we are in Auto Away mode we no need to Any analog Operations
        when {
            (HyperStatAssociationUtil.isAnalogOutAssociatedToCooling(analogOutState)) -> {

                if (basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.COOL_ONLY.ordinal ||
                    basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal) {
                    runForAnalogOutCooling(equip, port)
                }else{
                    updateLogicalPointIdValue(equip, logicalPointsList[port]!!, 0.0)
                }
            }
            (HyperStatAssociationUtil.isAnalogOutAssociatedToHeating(analogOutState)) -> {

                if (basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.HEAT_ONLY.ordinal ||
                    basicSettings.conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal) {
                    runForAnalogOutHeating(equip, port)
                }else{
                    updateLogicalPointIdValue(equip, logicalPointsList[port]!!, 0.0)
                }
            }
            (HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState)) -> {
                if (basicSettings.fanMode != StandaloneFanStage.OFF) {
                    runForAnalogOutFanSpeed(analogOutState, equip, config, port, basicSettings)
                }
            }
            (HyperStatAssociationUtil.isAnalogOutAssociatedToDcvDamper(analogOutState)) -> {
                runForAnalogOutDCVDamper(equip, config, port)
            }

        }
    }

    private fun runForAnalogOutCooling(
        equip: HyperStatCpuEquip,
        whichPort: Port
    ) {

        /**
        If any of the Analog-out is enabled and associated with cooling,cooling output signal will modulate between
        analogOutxAtMinCooling and analogOutxAtMaxCooling as the coolingLoopOp changes from 0% to 100% depending on
        the load conditions.Note: It is not necessary that analogOutxAtMaxCooling is more than analogOutxAtMinCooling.
        e.g. analogOutxAtMaxCooling = 2 and analogOutxAtMinCooling = 10 in that case then the output signal will
        be 10v when no cooling is needed and it will be 2V when maximum cooling is needed.
         */
        updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, coolingLoopOutput.toDouble())
        Log.i(L.TAG_CCU_HSCPU, "$whichPort = Cooling  analogSignal   $coolingLoopOutput")
    }

    private fun runForAnalogOutHeating(
        equip: HyperStatCpuEquip,
        whichPort: Port
    ) {
        /**

        If any of the Analog-out is enabled and associated with heating, , heating output signal will modulate between
        analogOutxAtMinHeating and analogOutxAtMaxHeating as the heatingLoopOp changes from 0% to 100% depending
        on the load conditions.Note: Same as for cooling above, it is not necessary that analogOutxAtMaxHeatingis more
        than analogOutxAtMinHeating . e.g. analogOutxAtMaxHeating =2 and analogOutxAtMinHeating = 10 in that case then
        the output signal will be 10v when no heating is needed and it will be 2V when maximum heating is needed.

         */
        updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, heatingLoopOutput.toDouble())
        Log.i(L.TAG_CCU_HSCPU, "$whichPort = Heating  analogSignal   $heatingLoopOutput")
    }

    private fun runForAnalogOutFanSpeed(
        analogOutState: AnalogOutState,
        equip: HyperStatCpuEquip,
        config: HyperStatCpuConfiguration,
        whichPort: Port,
        basicSettings: BasicSettings,
    ) {

        /**
        If any of the Analog-out is enabled and associated with fan, fan output signal will modulate between
        analogOutxAtMinFan and analogOutxAtMaxFan as the fanLoopOp changes from 0% to 100%
        depending on the coolingLoopOp > 0 or heatingLoopOp > 0.fanLoopOutput = coolingLoopOutput
        (or heatingingLoopOutput depending on whatever is the mode) * analogFanSpeedMultiplier
         */
        HyperStatAssociationUtil.getHighestFanStage(config)
        var fanLoopForAnalog = 0
        if (basicSettings.fanMode == StandaloneFanStage.AUTO) {
            equip.hsHaystackUtil!!.getOccupancyModePointValue().toInt()
            if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF){
                updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, 0.0)
                return
            }
            fanLoopForAnalog = fanLoopOutput
        } else {
            if (basicSettings.fanMode == StandaloneFanStage.LOW_CUR_OCC
                || basicSettings.fanMode == StandaloneFanStage.LOW_OCC
                || basicSettings.fanMode == StandaloneFanStage.LOW_ALL_TIME
            ) {
                fanLoopForAnalog = analogOutState.perAtFanLow.toInt()
            } else if (basicSettings.fanMode == StandaloneFanStage.MEDIUM_CUR_OCC
                || basicSettings.fanMode == StandaloneFanStage.MEDIUM_OCC
                || basicSettings.fanMode == StandaloneFanStage.MEDIUM_ALL_TIME
            ) {
                fanLoopForAnalog = analogOutState.perAtFanMedium.toInt()
            } else if (basicSettings.fanMode == StandaloneFanStage.HIGH_CUR_OCC
                || basicSettings.fanMode == StandaloneFanStage.HIGH_OCC
                || basicSettings.fanMode == StandaloneFanStage.HIGH_ALL_TIME
            ) {
                fanLoopForAnalog = analogOutState.perAtFanHigh.toInt()
            }

        }
        updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, fanLoopForAnalog.toDouble())
        Log.i(L.TAG_CCU_HSCPU, "$whichPort = Fan Speed  analogSignal   $fanLoopForAnalog")
    }

    private fun runForAnalogOutDCVDamper(
        equip: HyperStatCpuEquip,
        config: HyperStatCpuConfiguration,
        whichPort: Port,
    ) {
        /**
        If any of the Analog-out is enabled and associated with DCV Damper as the option, DCV Damper output signal
        will modulate between analogOutxAtMinDCVDamperPos and analogOutxAtMaxDCVDamperPos as the CO2 level of the
        zone is more than the CO2 threshold that has been set.If sensorHyperStatCo2 > zoneCO2Threshold then
        dcvCalculatedDamperPos = (sensorHyperStatCo2 - zoneCO2Threshold )/zoneCO2DamperOpeningRate  .
        Note: Same as for cooling/heating above, it is not necessary that analogOutxAtMaxDCVDamperPos is more
        than analogOutxAtMinDCVDamperPos . e.g. analogOutxAtMaxDCVDamperPos =2 and analogOutxAtMinDCVDamperPos = 10
        in that case then the output signal will be 10v when no ventilation is needed and it will be 2V
        when maximum ventilation is needed.
         */

        val currentOperatingMode = equip.hsHaystackUtil!!.getOccupancyModePointValue().toInt()
        val co2Value = equip.hsHaystackUtil!!.readCo2Value()
        Log.i(L.TAG_CCU_HSCPU, "runForAnalogOutDCVDamper: co2Value $co2Value currentOperatingMode $currentOperatingMode")

        if (co2Value > 0 && co2Value > config.zoneCO2Threshold
            && !isDoorOpenState(config,equip) && ( currentOperatingMode == Occupancy.OCCUPIED.ordinal ||
                    currentOperatingMode == Occupancy.AUTOFORCEOCCUPIED.ordinal||
                    currentOperatingMode == Occupancy.FORCEDOCCUPIED.ordinal )
        ) {
            var damperOperationPercent = (co2Value - config.zoneCO2Threshold) / config.zoneCO2DamperOpeningRate
            if(damperOperationPercent > 100 ) damperOperationPercent = 100.0
            updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, damperOperationPercent)
            Log.i(L.TAG_CCU_HSCPU, "$whichPort = OutDCVDamper  analogSignal  $damperOperationPercent")

            }else if (co2Value < config.zoneCO2Threshold || currentOperatingMode == Occupancy.AUTOAWAY.ordinal ||
                    currentOperatingMode == Occupancy.PRECONDITIONING.ordinal ||
                    currentOperatingMode == Occupancy.VACATION.ordinal ||
                    currentOperatingMode == Occupancy.UNOCCUPIED.ordinal|| isDoorOpenState(config,equip)
                ){
            updateLogicalPointIdValue(equip, logicalPointsList[whichPort]!!, 0.0)
        }
    }

    private fun handleDeadZone(equip: HyperStatCpuEquip) {

        Log.i(L.TAG_CCU_HSCPU, "updatePointsForEquip: Dead Zone ")
        state = ZoneState.TEMPDEAD
        resetAllLogicalPointValues(equip)
        equip.hsHaystackUtil!!.setProfilePoint("temp and operating and mode", 0.0)
        if (equip.hsHaystackUtil!!.getEquipStatus() != state.ordinal.toDouble())
            equip.hsHaystackUtil!!.setEquipStatus(state.ordinal.toDouble())

        val curStatus = equip.hsHaystackUtil!!.getEquipLiveStatus()
        if (curStatus != "Zone Temp Dead") {
            equip.hsHaystackUtil!!.writeDefaultVal("status and message and writable", "Zone Temp Dead")
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

        Log.i(L.TAG_CCU_HSCPU, "Fan Details :${occuStatus.isOccupied}  ${basicSettings.fanMode}  $fanModeSaved")
        if (!occuStatus.isOccupied && basicSettings.fanMode != StandaloneFanStage.OFF
            && basicSettings.fanMode != StandaloneFanStage.AUTO
            && basicSettings.fanMode != StandaloneFanStage.LOW_ALL_TIME
            && basicSettings.fanMode != StandaloneFanStage.MEDIUM_ALL_TIME
            && basicSettings.fanMode != StandaloneFanStage.HIGH_ALL_TIME
        ) {
            Log.i(L.TAG_CCU_HSCPU, "Resetting the Fan status back to  AUTO: ")
            updateHyperstatUIPoints(
                equipRef = equip.equipRef!!,
                command = "fan and operation and mode and cpu",
                value = StandaloneFanStage.AUTO.ordinal.toDouble()
            )
        }

        if (occuStatus.isOccupied && basicSettings.fanMode == StandaloneFanStage.AUTO && fanModeSaved != 0) {
            Log.i(L.TAG_CCU_HSCPU, "Resetting the Fan status back to ${StandaloneFanStage.values()[fanModeSaved]}")

            val actualFanMode = getActualFanMode(equip.node.toString(), fanModeSaved)
            updateHyperstatUIPoints(
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

        // Thermistor 2 is always mapped to door window sensor
        if (config.isEnableDoorWindowSensor) {
            val sensorValue = equip.hsHaystackUtil!!.getSensorPointValue(
                "th2 and logical and window and sensor"
            )
            Log.i(L.TAG_CCU_HSCPU, "TH2 Door Window sensor value : Door is $sensorValue")
            if (sensorValue.toInt() == 1) isDoorOpen = true
        }

        if (config.analogIn1State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToDoorWindowSensor(config.analogIn1State)
        ) {
            val sensorValue = equip.hsHaystackUtil!!.getSensorPointValue(
                "analog1 and in and logical and window and sensor"
            )
            Log.i(L.TAG_CCU_HSCPU, "Analog In 1 Door Window sensor value : Door is $sensorValue")
            if (sensorValue.toInt() == 1) isDoorOpen = true
        }

        if (config.analogIn2State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToDoorWindowSensor(config.analogIn2State)
        ) {
            val sensorValue = equip.hsHaystackUtil!!.getSensorPointValue(
                "analog2 and in and logical and window and sensor"
            )
            Log.i(L.TAG_CCU_HSCPU, "Analog In 2 Door Window sensor value : Door is $sensorValue")
            if (sensorValue.toInt() == 1) isDoorOpen = true
        }
        return isDoorOpen
    }
    private fun resetLoopOutputValues() {
        Log.i(L.TAG_CCU_HSCPU, "Resetting all the loop output values: ")
        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
    }

    private fun runForKeycardSensor(config: HyperStatCpuConfiguration, equip: HyperStatCpuEquip) {

    /*
            For the thermistor input a resistance is detected.
            Normally, the keycard switch is closed when the card is in the slot and the resistance is lower than
            10,000 ohms. This implies occupant is in the room. If the room was slated for occupancy then normal
            operation occurs. If the room was scheduled to be unoccupied, then it goes to forced occupied [auto].
            If the resistance increases beyond 10,000 ohms that implies the keycard is no longer in the switch
            and the occupant has left the room. In such a case, If the zone was scheduled to be unoccupied, then
            it goes to unoccupied from forced occupied. If the zone was scheduled to be occupied,
            the zone enters, 'auto away' state

            For the analog input a voltage is detected (this also needs the jumper on the back board to be
            moved to the AI1 or AI2 pull up position)
            Normally, the keycard switch is closed when the card is in the slot and the voltage is lower than 2.0 volts.
            This implies occupant is in the room. If the room was slated for occupancy then normal operation occurs.
            If the room was scheduled to be unoccupied, then it goes to forced occupied  [auto].
            If the voltage increases beyond 2.0 volts that implies the keycard is no longer in the switch and the occupant
            has left the room. In such a case, If the zone was scheduled to be unoccupied, then it goes to unoccupied from
            forced occupied. If the zone was scheduled to be occupied, the zone enters, 'auto away' state

    */
        val forcedOccupiedMinutes = TunerUtil.readTunerValByQuery("forced and occupied and time",
                                                                                        equip.equipRef)

        if (config.analogIn1State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToKeyCardSensor(config.analogIn1State)
        ) {
            val currentOperatingMode = equip.hsHaystackUtil!!.getOccupancyModePointValue().toInt()
            val sensorValue = equip.hsHaystackUtil!!.getSensorPointValue(
                "analog1 and in and logical and  keycard and sensor"
            )
            Log.i(L.TAG_CCU_HSCPU,
                "runForKeycardSensor 1 : $sensorValue \n + currentOperatingMode : $currentOperatingMode")

            if(sensorValue.toInt() == 1 && forcedOccupiedMinutes > 0) {
                if(currentOperatingMode == Occupancy.UNOCCUPIED.ordinal){
                    equip.hsHaystackUtil!!.setOccupancyMode(Occupancy.AUTOFORCEOCCUPIED.ordinal.toDouble())
                    updateDesiredTemp(equip.hsHaystackUtil!!.getDesiredTemp(), equip, true)
                    Log.i(L.TAG_CCU_HSCPU, "Keycard is sensed so Falling in FORCE Occupy mode")
                    return
                }
                if(currentOperatingMode == Occupancy.AUTOFORCEOCCUPIED.ordinal) {
                    updateDesiredTemp(equip.hsHaystackUtil!!.getDesiredTemp(), equip, false)
                    Log.i(L.TAG_CCU_HSCPU, "Extending the time for keycard auto force occupy")
                }
            }else{

                if(currentOperatingMode == Occupancy.OCCUPIED.ordinal) {
                    Log.i(L.TAG_CCU_HSCPU, "Moving to Auto Away state")
                    val detectionPointId = equip.hsHaystackUtil!!.readPointID("occupancy and detection and his")
                    equip.haystack.writeHisValueByIdWithoutCOV(detectionPointId, 0.0)
                    equip.hsHaystackUtil!!.setOccupancyMode(Occupancy.AUTOAWAY.ordinal.toDouble())
                    resetPresentConditioningStatus(equip)
                }else if(currentOperatingMode == Occupancy.AUTOFORCEOCCUPIED.ordinal
                    || currentOperatingMode == Occupancy.FORCEDOCCUPIED.ordinal) {
                    resetOccupancy(equip)
                }

            }
        }


        if (config.analogIn2State.enabled &&
            HyperStatAssociationUtil.isAnalogInAssociatedToKeyCardSensor(config.analogIn2State)
        ) {
            val currentOperatingMode = equip.hsHaystackUtil!!.getOccupancyModePointValue().toInt()
            val sensorValue = equip.hsHaystackUtil!!.getSensorPointValue(
                "analog2 and in and logical and  keycard and sensor"
            )
            Log.i(L.TAG_CCU_HSCPU,
                "runForKeycardSensor 2 : $sensorValue \n + currentOperatingMode : $currentOperatingMode")
            if(sensorValue.toInt() == 1 && forcedOccupiedMinutes > 0) {
                if(currentOperatingMode == Occupancy.UNOCCUPIED.ordinal){
                    equip.hsHaystackUtil!!.setOccupancyMode(Occupancy.AUTOFORCEOCCUPIED.ordinal.toDouble())
                    updateDesiredTemp(equip.hsHaystackUtil!!.getDesiredTemp(), equip,  true)
                    Log.i(L.TAG_CCU_HSCPU, "Keycard is sensed so Falling in FORCE Occupy mode")
                    return
                }
                if(currentOperatingMode != Occupancy.AUTOFORCEOCCUPIED.ordinal) {
                    updateDesiredTemp(equip.hsHaystackUtil!!.getDesiredTemp(), equip, false)
                    Log.i(L.TAG_CCU_HSCPU, "Extending the time for keycard auto force occupy")
                }
            }else{

                if(currentOperatingMode == Occupancy.OCCUPIED.ordinal) {
                    Log.i(L.TAG_CCU_HSCPU, "Moving to Auto Away state")
                    val detectionPointId = equip.hsHaystackUtil!!.readPointID("occupancy and detection and his")
                    equip.haystack.writeHisValueByIdWithoutCOV(detectionPointId, 0.0)
                    equip.hsHaystackUtil!!.setOccupancyMode(Occupancy.AUTOAWAY.ordinal.toDouble())
                    resetPresentConditioningStatus(equip)
                }else if(currentOperatingMode == Occupancy.AUTOFORCEOCCUPIED.ordinal
                    || currentOperatingMode == Occupancy.FORCEDOCCUPIED.ordinal) {
                    resetOccupancy(equip)
                }

            }
        }
    }

    override fun getEquip(): Equip? {
        for (nodeAddress in cpuDeviceMap.keys) {
            val equip = CCUHsApi.getInstance().read("equip and group == \"$nodeAddress\"")
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

    private fun resetPresentConditioningStatus(equip: HyperStatCpuEquip) {

        Log.i(L.TAG_CCU_HSCPU, "Resetting all the Preset Relays and Analog outs ")
        val logicalPointsList = equip.getLogicalPointList()
        val presentConfiguration = equip.getConfiguration()

        // Reset if any of the Relay mapped to Cooling Heating Fan Stage or Fan Enabled
        if (HyperStatAssociationUtil.isRelayAssociatedToAnyOfConditioningModes(presentConfiguration.relay1State)) {
            resetPoint(equip, logicalPointsList[Port.RELAY_ONE]!!)
        }
        if (HyperStatAssociationUtil.isRelayAssociatedToAnyOfConditioningModes(presentConfiguration.relay2State)) {
            resetPoint(equip, logicalPointsList[Port.RELAY_TWO]!!)
        }
        if (HyperStatAssociationUtil.isRelayAssociatedToAnyOfConditioningModes(presentConfiguration.relay3State)) {
            resetPoint(equip, logicalPointsList[Port.RELAY_THREE]!!)
        }
        if (HyperStatAssociationUtil.isRelayAssociatedToAnyOfConditioningModes(presentConfiguration.relay4State)) {
            resetPoint(equip, logicalPointsList[Port.RELAY_FOUR]!!)
        }
        if (HyperStatAssociationUtil.isRelayAssociatedToAnyOfConditioningModes(presentConfiguration.relay5State)) {
            resetPoint(equip, logicalPointsList[Port.RELAY_FIVE]!!)
        }
        if (HyperStatAssociationUtil.isRelayAssociatedToAnyOfConditioningModes(presentConfiguration.relay6State)) {
            resetPoint(equip, logicalPointsList[Port.RELAY_SIX]!!)
        }

        // Reset all the analog points
        resetPoint(equip, logicalPointsList[Port.ANALOG_OUT_ONE]!!)
        resetPoint(equip, logicalPointsList[Port.ANALOG_OUT_TWO]!!)
        resetPoint(equip, logicalPointsList[Port.ANALOG_OUT_THREE]!!)
    }

    private fun resetPoint(equip: HyperStatCpuEquip, pointId: String) {
      equip.haystack.writeHisValById(pointId, 0.0)
    }

    private fun resetAllLogicalPointValues(equip: HyperStatCpuEquip) {
        equip.hsHaystackUtil!!.updateAllLoopOutput(0,0,0)
        equip.getLogicalPointList().forEach { (_, pointId) -> resetPoint(equip, pointId) }
        HyperStatScheduler.updateHyperstatStatus(
            equipId = equip.equipRef!!,
            state = ZoneState.DEADBAND,
            portStages = HashMap(),
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

    private fun updateLogicalPointIdValue(equip: HyperStatCpuEquip, pointId: String, value: Double) {
        equip.hsHaystackUtil!!.writeHisValueByID(pointId, value)
    }

    private fun getCurrentPortStatus(equip: HyperStatCpuEquip, pointId: String): Double {
        return equip.haystack.readHisValById(pointId)
    }

    /**
     * If we are actually not doing any conditioning due to selected conditioning mode.
     * then we need to reset to loop output to 0 just to ignore invalid loop output for fan
     */
    private fun validateFanLoopOutPut(basicSettings: BasicSettings, userIntents: UserIntents){
        if(basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY
            && currentTemp < userIntents.zoneHeatingTargetTemperature){
            fanLoopOutput = 0
        }
        else if(basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY
            && currentTemp > userIntents.zoneCoolingTargetTemperature){
            fanLoopOutput = 0
        }
    }

    /**
     * Clearing the level 3 desired temp
     */
    private fun clearLevel3DesiredTemp(ccuHsApi: CCUHsApi, equipReff: String){
        Log.i(L.TAG_CCU_HSCPU ,"clearLevel3DesiredTemp: ")

        val coolDT = ccuHsApi.read("point and desired and cooling and temp and equipRef == \"$equipReff\"")
        val heatDT = ccuHsApi.read("point and desired and heating and temp and equipRef == \"$equipReff\"")
        val avgDT = ccuHsApi.read("point and desired and average and temp and equipRef == \"$equipReff\"")


        val coolLevel3Value = HSUtil.getPriorityLevelVal(coolDT["id"].toString(),3)
        val heatLevel3Value = HSUtil.getPriorityLevelVal(heatDT["id"].toString(),3)

        if(coolLevel3Value != 0.0||heatLevel3Value != 0.0 ) {
            ccuHsApi.pointWrite(HRef.copy(coolDT["id"].toString()), 3, "manual", HNum.make(0), HNum.make(1, "ms"))
            ccuHsApi.pointWrite(HRef.copy(heatDT["id"].toString()), 3, "manual", HNum.make(0), HNum.make(1, "ms"))

            if (avgDT.isNotEmpty()) {
                ccuHsApi.pointWrite(
                    HRef.copy(avgDT["id"].toString()),
                    3,
                    "manual",
                    HNum.make(0),
                    HNum.make(1, "ms")
                )
            }
        }
    }

}