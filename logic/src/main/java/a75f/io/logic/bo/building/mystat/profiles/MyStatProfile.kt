package a75f.io.logic.bo.building.mystat.profiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZoneProfile
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.MyStatFanStages
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.mystat.profiles.util.MyStatBasicSettings
import a75f.io.logic.bo.building.mystat.profiles.util.MyStatFanModeCacheStorage
import a75f.io.logic.bo.building.mystat.profiles.util.MyStatLoopController
import a75f.io.logic.bo.building.mystat.profiles.util.MyStatUserIntents
import a75f.io.logic.bo.building.mystat.profiles.util.updateAllLoopOutput
import a75f.io.logic.bo.building.mystat.profiles.util.updateLogicalPoint
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.util.uiutils.MyStatUserIntentHandler
import a75f.io.logic.util.uiutils.MyStatUserIntentHandler.Companion.myStatStatus
import a75f.io.logic.util.uiutils.updateUserIntentPoints

/**
 * Created by Manjunath K on 16-01-2025.
 */

abstract class MyStatProfile: ZoneProfile() {
    var logicalPointsList = HashMap<Port, String>()
    open var occupancyStatus: Occupancy = Occupancy.OCCUPIED
    private val haystack = CCUHsApi.getInstance()


    var occupancyBeforeDoorWindow = Occupancy.UNOCCUPIED
    var doorWindowSensorOpenStatus = false
    var runFanLowDuringDoorWindow = false

    var coolingLoopOutput = 0
    var heatingLoopOutput = 0
    var fanLoopOutput = 0
    var dcvLoopOutput = 0
    var compressorLoopOutput = 0 // used in HPU

    fun doFanEnabled(currentState: ZoneState, whichPort: Port, fanLoopOutput: Int, relayStages: HashMap<String, Int>, isFanLoopCounterEnabled : Boolean = false) {
        // Then Relay will be turned On when the zone is in occupied mode Or
        // any conditioning is happening during an unoccupied schedule
        CcuLog.d(L.TAG_CCU_MSCPU," Relay : $whichPort ,  isFanLoopCounterEnabled $isFanLoopCounterEnabled ")
        if (isOccupancyModeIsOccupied(occupancyStatus) || fanLoopOutput > 0) {
            updateLogicalPoint(logicalPointsList[whichPort]!!, 1.0)
            relayStages[AnalogOutput.FAN_ENABLED.name] = 1
        } else if (occupancyStatus != Occupancy.OCCUPIED || (currentState == ZoneState.COOLING || currentState == ZoneState.HEATING)) {
            // In order to protect the fan, persist the fan for few cycles when there is a sudden change in
            // occupancy and decrease in fan loop output
            if (isFanLoopCounterEnabled ) {
                updateLogicalPoint(logicalPointsList[whichPort]!!, 1.0)
                return
            }
            updateLogicalPoint(logicalPointsList[whichPort]!!, 0.0)
        }
    }

    fun doOccupiedEnabled(relayPort: Port) {
        // Relay will be turned on when module is in occupied state
        updateLogicalPoint(logicalPointsList[relayPort]!!, if (isOccupancyModeIsOccupied(occupancyStatus)) 1.0 else 0.0)
    }

    fun doHumidifierOperation(
        relayPort: Port,
        humidityHysteresis: Int,
        targetMinInsideHumidity: Double,
        currentHumidity: Double
    ) {
        // The humidifier is turned on whenever the humidity level drops below the targetMinInsideHumidty.
        // The humidifier will be turned off after being turned on when humidity goes humidityHysteresis above
        // the targetMinInsideHumidity. turns off when it drops 5% below threshold
        val currentPortStatus = haystack.readHisValById(logicalPointsList[relayPort]!!)
        logIt(
            "doHumidifierOperation: currentHumidity : $currentHumidity " +
                    "currentPortStatus : $currentPortStatus " +
                    "targetMinInsideHumidity : $targetMinInsideHumidity " +
                    "Hysteresis : $humidityHysteresis \n"
        )

        var relayStatus = 0.0
        if (currentHumidity > 0 && isOccupancyModeIsOccupied(occupancyStatus)) {
            if (currentHumidity < targetMinInsideHumidity) {
                relayStatus = 1.0
            } else if (currentPortStatus > 0) {
                relayStatus =
                    if (currentHumidity > (targetMinInsideHumidity + humidityHysteresis)) 0.0 else 1.0
            }
        } else relayStatus = 0.0
        updateLogicalPoint(logicalPointsList[relayPort]!!, relayStatus)
    }

    fun doDeHumidifierOperation(
        relayPort: Port, humidityHysteresis: Int,
        targetMaxInsideHumidity: Double, currentHumidity: Double
    ) {
        // If the dehumidifier is turned on whenever the humidity level goes above the targetMaxInsideHumidity.
        // The humidifier will be turned off after being turned on when humidity drops humidityHysteresis below
        // the targetMaxInsideHumidity. Turns off when it crosses over 5% above the threshold

        val currentPortStatus = haystack.readHisValById(logicalPointsList[relayPort]!!)
        logIt(
            "doDeHumidifierOperation currentHumidity : $currentHumidity " +
                    "| currentPortStatus : $currentPortStatus targetMaxInsideHumidity : $targetMaxInsideHumidity  Hysteresis : $humidityHysteresis \n"
        )
        var relayStatus = 0.0
        if (currentHumidity > 0 && isOccupancyModeIsOccupied(occupancyStatus)) {
            if (currentHumidity > targetMaxInsideHumidity) {
                relayStatus = 1.0
            } else if (currentPortStatus > 0) {
                relayStatus =
                    if (currentHumidity < (targetMaxInsideHumidity - humidityHysteresis)) 0.0 else 1.0
            }
        } else relayStatus = 0.0
        updateLogicalPoint(logicalPointsList[relayPort]!!, relayStatus)
    }

    fun doDcvDamperOperation(
        equip: MyStatEquip,
        port: Port,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>,
        cO2Threshold: Double,
        isDoorOpen: Boolean,
    ) {
        var relayState = -1.0
        val currentOccupancy = equip.occupancyMode.readHisVal().toInt()
        val co2Value = equip.zoneCo2.readHisVal()

        if (isDcvEligibleToOn(co2Value, cO2Threshold, occupancyStatus, isDoorOpen)) {
            if (dcvLoopOutput > relayActivationHysteresis) {
                relayState = 1.0
            }
        } else if (isDcvEligibleToOff(co2Value, cO2Threshold, currentOccupancy, isDoorOpen)) {
            relayState = 0.0
        }
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointsList[port]!!, relayState)
        }
        if (getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            relayStages[AnalogOutput.DCV_DAMPER.name] = 1
        }
    }

    fun doAnalogDCVAction(
        port: Port,
        analogOutStages: HashMap<String, Int>,
        cO2Threshold: Double,
        damperOpeningRate: Double,
        isDoorOpen: Boolean,
        equip: MyStatEquip,
    ) {
        val currentOccupancy = equip.occupancyMode.readHisVal().toInt()
        val co2Value = equip.zoneCo2.readHisVal()
        CcuLog.d(
            L.TAG_CCU_MSHST,
            "doAnalogDCVAction: co2Value : $co2Value zoneCO2Threshold: $cO2Threshold zoneCO2DamperOpeningRate $damperOpeningRate"
        )
        if (isDcvEligibleToOn(co2Value, cO2Threshold, occupancyStatus, isDoorOpen)) {
            updateLogicalPoint(logicalPointsList[port]!!, dcvLoopOutput.toDouble())
            analogOutStages[AnalogOutput.DCV_DAMPER.name] = dcvLoopOutput
        } else if (isDcvEligibleToOff(co2Value, cO2Threshold, currentOccupancy, isDoorOpen)) {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }

    private fun isDcvEligibleToOff(
        co2Value: Double, zoneCO2Threshold: Double, currentOccupancy: Int, isDoorOpen: Boolean
    ): Boolean {
        return (dcvLoopOutput == 0 || co2Value < zoneCO2Threshold || isDoorOpen
                || isOccupancyModeIsUnOccupied(occupancyStatus))
    }

    private fun isDcvEligibleToOn(
        co2Value: Double, zoneCO2Threshold: Double, currentOccupancyMode: Occupancy, isDoorOpen: Boolean
    ): Boolean {
        return (co2Value > 0 && co2Value > zoneCO2Threshold && dcvLoopOutput > 0 && !isDoorOpen
                && (isOccupancyModeIsOccupied(currentOccupancyMode)))
    }

    fun doorWindowIsOpen(doorWindowEnabled: Double, doorWindowSensor: Double, equip: MyStatEquip) {
        equip.doorWindowSensingEnable.writePointValue(doorWindowEnabled)
        equip.doorWindowSensorInput.writePointValue(doorWindowSensor)
    }

    fun keyCardIsInSlot(keycardEnabled: Double, keycardSensor: Double, equip: MyStatEquip) {
        equip.keyCardSensingEnable.writePointValue(keycardEnabled)
        equip.keyCardSensorInput.writePointValue(keycardSensor)
    }

    fun resetPort(port: Port){
        updateLogicalPoint(logicalPointsList[port]!!,0.0)
    }

    fun resetLogicalPoint(pointId: String?) {
        if (pointId != null) {
            updateLogicalPoint(pointId, 0.0)
        } else {
            logIt("resetLogicalPoint: But point id is null !!")
        }
    }

    fun getCurrentPortStatus(port: Port): Double {
        return haystack.readHisValById(logicalPointsList[port]!!)
    }

    fun getCurrentLogicalPointStatus(pointId: String): Double = haystack.readHisValById(pointId)

    fun resetLogicalPoints(){
        logicalPointsList.forEach { (_, pointId) -> haystack.writeHisValById(pointId, 0.0) }
    }

    fun handleChangeOfDirection(userIntents: MyStatUserIntents, myStatLoopController: MyStatLoopController) {
        if (currentTemp > userIntents.zoneCoolingTargetTemperature && state != ZoneState.COOLING) {
            myStatLoopController.resetCoolingControl()
            state = ZoneState.COOLING
            CcuLog.d(L.TAG_CCU_MSHST, "Resetting cooling")
        } else if (currentTemp < userIntents.zoneHeatingTargetTemperature && state != ZoneState.HEATING) {
            myStatLoopController.resetHeatingControl()
            state = ZoneState.HEATING
            CcuLog.d(L.TAG_CCU_MSHST, "Resetting heating")
        }
    }

    fun evaluateLoopOutputs(userIntents: MyStatUserIntents, myStatLoopController: MyStatLoopController) {
        when (state) {
            //Update coolingLoop when the zone is in cooling or it was in cooling and no change over happened yet.
            ZoneState.COOLING -> coolingLoopOutput =
                myStatLoopController.calculateCoolingLoopOutput(
                    currentTemp, userIntents.zoneCoolingTargetTemperature).toInt().coerceAtLeast(0)

            //Update heatingLoop when the zone is in heating or it was in heating and no change over happened yet.
            ZoneState.HEATING -> heatingLoopOutput =
                myStatLoopController.calculateHeatingLoopOutput(
                    userIntents.zoneHeatingTargetTemperature, currentTemp
                ).toInt().coerceAtLeast(0)

            else -> CcuLog.d(L.TAG_CCU_MSHST, " Zone is in deadband")
        }
    }

    fun calculateDcvLoop(equip: MyStatEquip, zoneCO2Threshold: Double, zoneCO2DamperOpeningRate: Double) {
        val co2Value = equip.zoneCo2.readHisVal().toInt()
        dcvLoopOutput = ((co2Value - zoneCO2Threshold) / zoneCO2DamperOpeningRate).toInt().coerceIn(0, 100)
    }

    fun fallBackFanMode(
        equip: MyStatEquip, equipRef: String, fanModeSaved: Int, basicSettings: MyStatBasicSettings
    ): MyStatFanStages {
        logIt("FanModeSaved in Shared Preference $fanModeSaved")
        val currentOccupancy = equip.occupancyMode.readHisVal().toInt()
        // If occupancy is unoccupied and the fan mode is current occupied then remove the fan mode from cache
        if ((occupancyStatus == Occupancy.UNOCCUPIED || occupancyStatus == Occupancy.DEMAND_RESPONSE_UNOCCUPIED ) && isFanModeCurrentOccupied(fanModeSaved)) {
            MyStatFanModeCacheStorage().removeFanModeFromCache(equipRef)
        }

        logIt("Fall back fan mode " + basicSettings.fanMode + " conditioning mode " + basicSettings.conditioningMode)
        logIt("Fan Details :$occupancyStatus  ${basicSettings.fanMode}  $fanModeSaved")
        if (isEligibleToAuto(basicSettings,currentOccupancy)) {
            logIt("Resetting the Fan status back to  AUTO: ")
            updateUserIntentPoints(
                equipRef = equipRef,
                equip.fanOpMode,
                value = MyStatFanStages.AUTO.ordinal.toDouble(),
                CCUHsApi.getInstance().ccuUserName
            )
            return MyStatFanStages.AUTO
        }

        if ((occupancyStatus == Occupancy.OCCUPIED
                    || occupancyStatus == Occupancy.AUTOFORCEOCCUPIED
                    || occupancyStatus == Occupancy.FORCEDOCCUPIED
                    || Occupancy.values()[currentOccupancy] == Occupancy.PRECONDITIONING
                    || occupancyStatus == Occupancy.DEMAND_RESPONSE_OCCUPIED)
            && basicSettings.fanMode == MyStatFanStages.AUTO && fanModeSaved != 0) {
            logIt("Resetting the Fan status back to ${MyStatFanStages.values()[fanModeSaved]}")
            updateUserIntentPoints(
                equipRef = equipRef,
                equip.fanOpMode,
                value = fanModeSaved.toDouble(),
                CCUHsApi.getInstance().ccuUserName
            )
            return MyStatFanStages.values()[fanModeSaved]
        }
        return MyStatFanStages.values()[equip.fanOpMode.readPriorityVal().toInt()]
    }

    private fun isFanModeCurrentOccupied(value : Int): Boolean {
        val basicSettings = MyStatFanStages.values()[value]
        return (basicSettings == MyStatFanStages.LOW_CUR_OCC || basicSettings == MyStatFanStages.HIGH_CUR_OCC)
    }

    private fun isEligibleToAuto(basicSettings: MyStatBasicSettings, currentOperatingMode: Int): Boolean {
        return (occupancyStatus != Occupancy.OCCUPIED
                && occupancyStatus != Occupancy.AUTOFORCEOCCUPIED
                && occupancyStatus != Occupancy.FORCEDOCCUPIED
                && occupancyStatus != Occupancy.DEMAND_RESPONSE_OCCUPIED
                && Occupancy.values()[currentOperatingMode] != Occupancy.PRECONDITIONING
                && basicSettings.fanMode != MyStatFanStages.OFF
                && basicSettings.fanMode != MyStatFanStages.AUTO
                && basicSettings.fanMode != MyStatFanStages.LOW_ALL_TIME
                && basicSettings.fanMode != MyStatFanStages.HIGH_ALL_TIME
                )
    }

    fun getAverageTemp(userIntents: MyStatUserIntents) = (userIntents.zoneCoolingTargetTemperature + userIntents.zoneHeatingTargetTemperature) / 2.0

    fun handleRFDead(equip: MyStatEquip) {
        state = ZoneState.RFDEAD
        equip.operatingMode.writeHisVal(state.ordinal.toDouble())
        equip.equipStatus.writeHisVal(state.ordinal.toDouble())
        equip.equipStatusMessage.writeDefaultVal(RFDead)
        myStatStatus[equip.equipRef] = RFDead
        CcuLog.d(L.TAG_CCU_MSHST, "RF Signal is Dead ${equip.nodeAddress}")
    }

    fun handleDeadZone(equip: MyStatEquip) {
        state = ZoneState.TEMPDEAD
        resetPoints(equip)
        equip.operatingMode.writeHisVal(state.ordinal.toDouble())
        equip.equipStatus.writeHisVal(state.ordinal.toDouble())
        equip.equipStatusMessage.writeDefaultVal(ZoneTempDead)
        myStatStatus[equip.equipRef] = ZoneTempDead
    }

    private fun resetPoints(equip: MyStatEquip) {
        updateAllLoopOutput(equip, 0, 0, 0)
        resetLogicalPoints()
        MyStatUserIntentHandler.updateMyStatStatus(equip.equipRef, HashMap(), HashMap(), ZoneTempState.TEMP_DEAD, equip)
    }

    fun updateOccupancyDetection(equip: MyStatEquip) {
        if (equip.zoneOccupancy.readHisVal() > 0) equip.occupancyDetection.writeHisValueByIdWithoutCOV(1.0)
    }

    fun resetLoops() {
        CcuLog.d(L.TAG_CCU_MSHST, "Resetting all the loop output values: ")
        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        compressorLoopOutput = 0
    }

    fun checkFanOperationAllowedDoorWindow(userIntents: MyStatUserIntents): Boolean {
        return if (currentTemp < userIntents.zoneCoolingTargetTemperature && currentTemp > userIntents.zoneHeatingTargetTemperature) {
            doorWindowSensorOpenStatus &&
                    occupancyBeforeDoorWindow != Occupancy.UNOCCUPIED &&
                    occupancyBeforeDoorWindow != Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
                    occupancyBeforeDoorWindow != Occupancy.VACATION
        } else {
            doorWindowSensorOpenStatus
        }
    }

    fun updateLoopOutputs(equip: MyStatEquip, coolingLoop: Int, heatingLoop: Int, fanLoop: Int, dcvLoop: Int, isHpuProfile: Boolean = false, compressorLoop: Int = 0) {
        equip.apply {
            coolingLoopOutput.writePointValue(coolingLoop.toDouble())
            heatingLoopOutput.writePointValue(heatingLoop.toDouble())
            fanLoopOutput.writePointValue(fanLoop.toDouble())
            dcvLoopOutput.writePointValue(dcvLoop.toDouble())
            dcvAvailable.writePointValue(if (dcvLoop > 0 )1.0 else 0.0)
            if (isHpuProfile) {
                (equip as MyStatHpuEquip).compressorLoopOutput.writePointValue(compressorLoop.toDouble())
            }
        }
    }



    fun updateOperatingMode(currentTemp: Double, averageDesiredTemp: Double, basicSettings: MyStatBasicSettings, equip: MyStatEquip) {
        val zoneOperatingMode = when {
            currentTemp < averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.COOL_ONLY -> ZoneState.HEATING.ordinal
            currentTemp >= averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY -> ZoneState.COOLING.ordinal
            else -> ZoneState.DEADBAND.ordinal
        }
        equip.operatingMode.writeHisVal(zoneOperatingMode.toDouble())
    }

    open fun isDoorOpenState(config: MyStatConfiguration, equip: MyStatEquip): Boolean {
        var isDoorWindowMapped = 0.0
        var doorWindowSensorValue =  0.0

        if (config.universalIn1Enabled.enabled) {
            val universalMapping = MyStatConfiguration.UniversalMapping.values().find { it.ordinal == config.universalIn1Association.associationVal }
            when (universalMapping) {
                MyStatConfiguration.UniversalMapping.DOOR_WINDOW_SENSOR_NC_TITLE24 -> {
                    isDoorWindowMapped = 1.0
                    doorWindowSensorValue = equip.doorWindowSensorNCTitle24.readHisVal()
                }
                MyStatConfiguration.UniversalMapping.DOOR_WINDOW_SENSOR_TITLE24 -> {
                    isDoorWindowMapped = 1.0
                    doorWindowSensorValue = equip.doorWindowSensorTitle24.readHisVal()
                }
                else -> { }
            }
        }
        doorWindowIsOpen(isDoorWindowMapped, doorWindowSensorValue, equip)
        return isDoorWindowMapped == 1.0 && doorWindowSensorValue > 0
    }


    enum class MyStatFanSpeed {
        OFF,LOW,HIGH
    }

    fun logIt(msg: String) {
        CcuLog.d(L.TAG_CCU_MSHST, msg)
    }
}

