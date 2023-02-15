package a75f.io.logic.bo.building.hyperstat.profiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.ZoneProfile
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.actions.AnalogOutActions
import a75f.io.logic.bo.building.hyperstat.actions.DoorWindowKeycardActions
import a75f.io.logic.bo.building.hyperstat.actions.RelayActions
import a75f.io.logic.bo.building.hyperstat.common.BasicSettings
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil
import a75f.io.logic.bo.building.hyperstat.common.HyperStatEquip
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.HyperStatPipe2Equip
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.jobs.HyperStatUserIntentHandler
import android.util.Log


/**
 * Created by Manjunath K on 11-07-2022.
 */

abstract class HyperStatProfile : ZoneProfile(),RelayActions, AnalogOutActions, DoorWindowKeycardActions {

    lateinit var hsHaystackUtil: HSHaystackUtil
    var logicalPointsList: HashMap<Any, String> = HashMap()
    open var occupancyStatus: Occupancy = Occupancy.OCCUPIED
    private val haystack = CCUHsApi.getInstance()


    abstract fun getHyperStatEquip(node: Short): HyperStatEquip?
    abstract fun addNewEquip(node: Short, room: String, floor: String, baseConfig: BaseProfileConfiguration)

    override fun doCoolingStage1(
        port: Port,
        coolingLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>
    ) {
        var relayState = -1.0
        if (coolingLoopOutput > relayActivationHysteresis)
            relayState = 1.0
        if (coolingLoopOutput == 0)
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, relayState)
            if (relayState == 1.0) {
                relayStages[Stage.COOLING_1.displayName] = 1
            }
            logIt("$port = CoolingStage1:  $relayState")
        }
    }

    override fun doCoolingStage2(
        port: Port,
        coolingLoopOutput: Int,
        relayActivationHysteresis: Int,
        divider: Int,
        relayStages: HashMap<String, Int>
    ) {
        var relayState = -1.0
        if (coolingLoopOutput > (divider + (relayActivationHysteresis / 2)))
            relayState = 1.0
        if (coolingLoopOutput <= (divider - (relayActivationHysteresis / 2)))
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, relayState)
            if (relayState == 1.0) {
                relayStages[Stage.COOLING_2.displayName] = 1
            }
            logIt( "$port = CoolingStage2:  $relayState")
        }
    }

    override fun doCoolingStage3(
        port: Port,
        coolingLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>
    ) {
        var relayState = -1.0
        if (coolingLoopOutput > (66 + (relayActivationHysteresis / 2)))
            relayState = 1.0
        if (coolingLoopOutput <= (66 - (relayActivationHysteresis / 2)))
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, relayState)
            if (relayState == 1.0) {
                relayStages[Stage.COOLING_3.displayName] = 1
            }
            logIt("$port = CoolingStage3:  $relayState")
        }
    }


    override fun doCompressorStage1(
        port: Port,
        compressorLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>,
        zoneMode: ZoneState
    ) {
        var relayState = -1.0
        if (compressorLoopOutput > relayActivationHysteresis)
            relayState = 1.0
        if (compressorLoopOutput == 0)
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, relayState)
            if (relayState == 1.0) {
                if(zoneMode == ZoneState.COOLING)
                    relayStages[Stage.COOLING_1.displayName] = 1
                if(zoneMode == ZoneState.HEATING)
                    relayStages[Stage.HEATING_1.displayName] = 1
            }
            logIt("$port = COMPRESSOR_STAGE1:  $relayState")
        }
    }

    override fun doCompressorStage2(
        port: Port,
        compressorLoopOutput: Int,
        relayActivationHysteresis: Int,
        divider: Int,
        relayStages: HashMap<String, Int>,
        zoneMode: ZoneState
    ) {
        var relayState = -1.0
        if (compressorLoopOutput > (divider + (relayActivationHysteresis / 2)))
            relayState = 1.0
        if (compressorLoopOutput <= (divider - (relayActivationHysteresis / 2)))
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, relayState)
            if (relayState == 1.0) {
                if(zoneMode == ZoneState.COOLING)
                    relayStages[Stage.COOLING_2.displayName] = 1
                if(zoneMode == ZoneState.HEATING)
                    relayStages[Stage.HEATING_2.displayName] = 1
            }
            logIt( "$port = COMPRESSOR_STAGE2:  $relayState")
        }
    }

    override fun doCompressorStage3(
        port: Port,
        compressorLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>,
        zoneMode: ZoneState
    ) {
        var relayState = -1.0
        if (compressorLoopOutput > (66 + (relayActivationHysteresis / 2)))
            relayState = 1.0
        if (compressorLoopOutput <= (66 - (relayActivationHysteresis / 2)))
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, relayState)
            if (relayState == 1.0) {
                if(zoneMode == ZoneState.COOLING)
                    relayStages[Stage.COOLING_3.displayName] = 1
                if(zoneMode == ZoneState.HEATING)
                    relayStages[Stage.HEATING_3.displayName] = 1
            }
            logIt("$port = COMPRESSOR_STAGE3:  $relayState")
        }
    }


    override fun doHeatingStage1(
        port: Port,
        heatingLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>
    ) {

        var relayState = -1.0
        if (heatingLoopOutput > relayActivationHysteresis)
            relayState = 1.0
        if (heatingLoopOutput == 0)
            relayState = 0.0

        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, relayState)
            if (relayState == 1.0) {
                relayStages[Stage.HEATING_1.displayName] = 1
            }
            logIt("$port = HeatingStage1: $relayState")
        }

    }

    override fun doHeatingStage2(
        port: Port,
        heatingLoopOutput: Int,
        relayActivationHysteresis: Int,
        divider: Int,
        relayStages: HashMap<String, Int>
    ) {
        var relayState = -1.0
        if (heatingLoopOutput > (divider + (relayActivationHysteresis / 2)))
            relayState = 1.0
        if (heatingLoopOutput <= (divider - (relayActivationHysteresis / 2)))
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, relayState)
            if (relayState == 1.0) {
                relayStages[Stage.HEATING_2.displayName] = 1
            }
            logIt("$port = HeatingStage2:  $relayState")
        }
    }

    override fun doHeatingStage3(
        port: Port,
        heatingLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>
    ) {
        var relayState = -1.0
        if (heatingLoopOutput > (66 + (relayActivationHysteresis / 2)))
            relayState = 1.0
        if (heatingLoopOutput <= (66 - (relayActivationHysteresis / 2)))
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, relayState)
            if (relayState == 1.0) {
                relayStages[Stage.HEATING_3.displayName] = 1
            }
            logIt("$port = HeatingStage3  $relayState")
        }
    }

    override fun doFanEnabled(currentState: ZoneState, whichPort: Port, fanLoopOutput: Int) {
        // Then Relay will be turned On when the zone is in occupied mode Or
        // any conditioning is happening during an unoccupied schedule

        if (occupancyStatus == Occupancy.OCCUPIED || fanLoopOutput > 0) {
            updateLogicalPointIdValue(logicalPointsList[whichPort]!!, 1.0)
        } else if (occupancyStatus != Occupancy.OCCUPIED || (currentState == ZoneState.COOLING || currentState == ZoneState.HEATING)) {
            updateLogicalPointIdValue(logicalPointsList[whichPort]!!, 0.0)
        }
    }

    override fun doOccupiedEnabled(relayPort: Port) {
        // Relay will be turned on when module is in occupied state
        updateLogicalPointIdValue(
            logicalPointsList[relayPort]!!,
            if (occupancyStatus == Occupancy.OCCUPIED) 1.0 else 0.0
        )
        logIt("$relayPort = DeHumidifier  ${if (occupancyStatus == Occupancy.OCCUPIED) 1.0 else 0.0}")

    }

    override fun doHumidifierOperation(
        relayPort: Port,
        humidityHysteresis: Int,
        targetMinInsideHumidity: Double
    ) {
        // The humidifier is turned on whenever the humidity level drops below the targetMinInsideHumidty.
        // The humidifier will be turned off after being turned on when humidity goes humidityHysteresis above
        // the targetMinInsideHumidity. turns off when it drops 5% below threshold


        val currentHumidity = hsHaystackUtil.getHumidity()
        val currentPortStatus = haystack.readHisValById(logicalPointsList[relayPort]!!)
        logIt(
            "doHumidifierOperation: currentHumidity : $currentHumidity \n" +
                    "currentPortStatus : $currentPortStatus \n" +
                    "targetMinInsideHumidity : $targetMinInsideHumidity \n" +
                    "Hysteresis : $humidityHysteresis \n"
         )

        var relayStatus = 0.0
        if (currentHumidity > 0 && occupancyStatus == Occupancy.OCCUPIED) {
            if (currentHumidity < targetMinInsideHumidity) {
                relayStatus = 1.0
            } else if (currentPortStatus > 0) {
                relayStatus =
                    if (currentHumidity > (targetMinInsideHumidity + humidityHysteresis)) 0.0 else 1.0
            }
        } else relayStatus = 0.0

        updateLogicalPointIdValue(logicalPointsList[relayPort]!!, relayStatus)
        logIt( "$relayPort = Humidifier  $relayStatus")
    }

    override fun doDeHumidifierOperation(
        relayPort: Port,
        humidityHysteresis: Int,
        targetMaxInsideHumidity: Double
    ) {
        // If the dehumidifier is turned on whenever the humidity level goes above the targetMaxInsideHumidity.
        // The humidifier will be turned off after being turned on when humidity drops humidityHysteresis below
        // the targetMaxInsideHumidity. Turns off when it crosses over 5% above the threshold


        val currentHumidity = hsHaystackUtil.getHumidity()
        val currentPortStatus = haystack.readHisValById(logicalPointsList[relayPort]!!)
        logIt(
            "doDeHumidifierOperation currentHumidity : $currentHumidity \n" +
                    "| currentPortStatus : $currentPortStatus \n" +
                    "|targetMaxInsideHumidity : $targetMaxInsideHumidity \n" +
                    "| Hysteresis : $humidityHysteresis \n"
        )
        var relayStatus = 0.0
        if (currentHumidity > 0 && occupancyStatus == Occupancy.OCCUPIED) {
            if (currentHumidity > targetMaxInsideHumidity) {
                relayStatus = 1.0
            } else if (currentPortStatus > 0) {
                relayStatus =
                    if (currentHumidity < (targetMaxInsideHumidity - humidityHysteresis)) 0.0 else 1.0
            }
        } else relayStatus = 0.0

        updateLogicalPointIdValue(logicalPointsList[relayPort]!!, relayStatus)
        logIt("$relayPort = DeHumidifier  $relayStatus")
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
            if (coolingLoopOutput > 0) analogOutStages[AnalogOutput.COOLING.name] =
                coolingLoopOutput
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
            if (heatingLoopOutput > 0) analogOutStages[AnalogOutput.HEATING.name] =
                heatingLoopOutput
        } else {
            updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
        }
    }

    override fun doRelayWaterValveOperation(
        equip: HyperStatPipe2Equip,
        port: Port,
        basicSettings: BasicSettings,
        loopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>
    ) {
        var relayState = -1.0
        if (basicSettings.conditioningMode != StandaloneConditioningMode.OFF && basicSettings.fanMode != StandaloneFanStage.OFF) {
            if(loopOutput > relayActivationHysteresis) {
                relayState = 1.0
                equip.lastWaterValveTurnedOnTime = System.currentTimeMillis()
            }
            else if (loopOutput == 0)
                relayState = 0.0
        }else{
            relayState = 0.0
        }
        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, relayState)
        }
        // show status message
        if ( getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            relayStages[AnalogOutput.WATER_VALVE.name] = 1
        }
    }

    override fun doAnalogWaterValveAction(
        port: Port,
        fanMode: StandaloneFanStage,
        basicSettings: BasicSettings,
        loopOutput: Int,
        analogOutStages: HashMap<String, Int>
    ) {
        if(basicSettings.conditioningMode != StandaloneConditioningMode.OFF
            && basicSettings.fanMode != StandaloneFanStage.OFF) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, loopOutput.toDouble())
            if (loopOutput > 0) analogOutStages[AnalogOutput.WATER_VALVE.name] = 1
        }else{
            updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
        }
    }




    // Analog Fan operation is common for all the modules
    override fun doAnalogDCVAction(
        port: Port,
        analogOutStages: HashMap<String, Int>,
        zoneCO2Threshold: Double,
        zoneCO2DamperOpeningRate: Double,
        isDoorOpen: Boolean
    ) {
        val currentOperatingMode = hsHaystackUtil.getOccupancyModePointValue().toInt()
        val co2Value = hsHaystackUtil.readCo2Value()

        if (co2Value > 0 && co2Value > zoneCO2Threshold
            && !isDoorOpen && (currentOperatingMode == Occupancy.OCCUPIED.ordinal ||
                    currentOperatingMode == Occupancy.AUTOFORCEOCCUPIED.ordinal ||
                    currentOperatingMode == Occupancy.PRECONDITIONING.ordinal ||
                    currentOperatingMode == Occupancy.FORCEDOCCUPIED.ordinal)
        ) {
            var damperOperationPercent = (co2Value - zoneCO2Threshold) / zoneCO2DamperOpeningRate
            if (damperOperationPercent > 100) damperOperationPercent = 100.0
            updateLogicalPointIdValue(logicalPointsList[port]!!, damperOperationPercent)
            if (damperOperationPercent > 0) analogOutStages[AnalogOutput.DCV_DAMPER.name] =
                damperOperationPercent.toInt()
            logIt("$port = OutDCVDamper  analogSignal  $damperOperationPercent")

        } else if (co2Value < zoneCO2Threshold || currentOperatingMode == Occupancy.AUTOAWAY.ordinal ||
            currentOperatingMode == Occupancy.VACATION.ordinal ||
            currentOperatingMode == Occupancy.UNOCCUPIED.ordinal || isDoorOpen
        ) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
        }
    }
    override fun doAnalogCompressorSpeed(
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        compressorLoopOutput: Int,
        zoneMode: ZoneState
    ) {
        if (conditioningMode !=  StandaloneConditioningMode.OFF) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, compressorLoopOutput.toDouble())
            if (compressorLoopOutput > 0){
                if(zoneMode == ZoneState.COOLING)
                    analogOutStages[AnalogOutput.COOLING.name] = compressorLoopOutput
                if(zoneMode == ZoneState.HEATING)
                    analogOutStages[AnalogOutput.HEATING.name] = compressorLoopOutput
            }
        } else {
            updateLogicalPointIdValue(logicalPointsList[port]!!, 0.0)
        }
    }
    override fun doorWindowIsOpen(doorWindowEnabled: Double, doorWindowSensor: Double) {
        hsHaystackUtil.updateDoorWindowValues(doorWindowEnabled,doorWindowSensor)
    }

    override fun keyCardIsInSlot(keycardEnabled: Double, keycardSensor: Double) {
        hsHaystackUtil.updateKeycardValues(keycardEnabled,keycardSensor)
    }

    fun updateLogicalPointIdValue(pointId: String?, value: Double) {
        if(pointId != null) {
            hsHaystackUtil.writeHisValueByID(pointId, value)
        }else{
            logIt("updateLogicalPointIdValue: But point id is null !!")
        }
    }

    fun resetPort(port: Port){
        updateLogicalPointIdValue(logicalPointsList[port]!!,0.0)
    }
    fun resetLogicalPoint(pointId: String?){
        if(pointId != null) {
            updateLogicalPointIdValue(pointId, 0.0)
        }else{
            logIt("resetLogicalPoint: But point id is null !!")
        }
    }

    fun getCurrentPortStatus(port: Port): Double {
        return haystack.readHisValById(logicalPointsList[port]!!)
    }
    fun getCurrentLogicalPointStatus(pointId: String): Double {
        return haystack.readHisValById(pointId)
    }

    fun resetAllLogicalPointValues(){
        logicalPointsList.forEach { (_, pointId) -> haystack.writeHisValById(pointId, 0.0) }
    }

    fun fallBackFanMode(
        equip: HyperStatEquip, equipRef: String, fanModeSaved: Int,
        actualFanMode: Int, basicSettings: BasicSettings
    ): StandaloneFanStage {

        val currentOperatingMode = equip.hsHaystackUtil.getOccupancyModePointValue().toInt()
        logIt("Fan Details :$occupancyStatus  ${basicSettings.fanMode}  $fanModeSaved")
        if (isEligibleToAuto(basicSettings,currentOperatingMode)) {
            logIt("Resetting the Fan status back to  AUTO: ")
            HyperStatUserIntentHandler.updateHyperStatUIPoints(
                equipRef = equipRef,
                command = "zone and sp and fan and operation and mode",
                value = StandaloneFanStage.AUTO.ordinal.toDouble()
            )
            return StandaloneFanStage.AUTO
        }

        if ((occupancyStatus == Occupancy.OCCUPIED|| Occupancy.values()[currentOperatingMode] == Occupancy.PRECONDITIONING )
            && basicSettings.fanMode == StandaloneFanStage.AUTO && fanModeSaved != 0) {
            logIt("Resetting the Fan status back to ${StandaloneFanStage.values()[fanModeSaved]}")
            HyperStatUserIntentHandler.updateHyperStatUIPoints(
                equipRef = equipRef,
                command = "zone and sp and fan and operation and mode",
                value = actualFanMode.toDouble()
            )
            return StandaloneFanStage.values()[actualFanMode]
        }
        return  StandaloneFanStage.values()[equip.hsHaystackUtil.getCurrentFanMode().toInt()]
    }

    private fun isEligibleToAuto(basicSettings: BasicSettings, currentOperatingMode: Int ): Boolean{
        return (occupancyStatus != Occupancy.OCCUPIED
            && Occupancy.values()[currentOperatingMode] != Occupancy.PRECONDITIONING
            && basicSettings.fanMode != StandaloneFanStage.OFF
            && basicSettings.fanMode != StandaloneFanStage.AUTO
            && basicSettings.fanMode != StandaloneFanStage.LOW_ALL_TIME
            && basicSettings.fanMode != StandaloneFanStage.MEDIUM_ALL_TIME
            && basicSettings.fanMode != StandaloneFanStage.HIGH_ALL_TIME
        )
    }

    fun setOperatingMode(currentTemp: Double,averageDesiredTemp: Double,basicSettings: BasicSettings,equip: HyperStatEquip){
        var zoneOperatingMode = ZoneState.DEADBAND.ordinal
        if(currentTemp < averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.COOL_ONLY) {
            zoneOperatingMode = ZoneState.HEATING.ordinal
        }
        else if(currentTemp >= averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
            zoneOperatingMode = ZoneState.COOLING.ordinal
        }
        logIt("averageDesiredTemp $averageDesiredTemp" + "zoneOperatingMode ${ZoneState.values()[zoneOperatingMode]}")
        equip.hsHaystackUtil.setProfilePoint("operating and mode", zoneOperatingMode.toDouble())
    }

    private fun logIt(msg: String){
        Log.i(L.TAG_CCU_HSHST, msg)
    }

    // To run specific fan speed while running aux heating
    enum class FanSpeed {
        OFF,LOW,MEDIUM,HIGH
    }

}