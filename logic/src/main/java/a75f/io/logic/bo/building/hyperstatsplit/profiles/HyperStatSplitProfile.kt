package a75f.io.logic.bo.building.hyperstatsplit.profiles

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
import a75f.io.logic.bo.building.hyperstatsplit.actions.AnalogOutActions
import a75f.io.logic.bo.building.hyperstatsplit.actions.DoorWindowKeycardActions
import a75f.io.logic.bo.building.hyperstatsplit.actions.RelayActions
import a75f.io.logic.bo.building.hyperstatsplit.common.BasicSettings
import a75f.io.logic.bo.building.hyperstatsplit.common.HSSplitHaystackUtil
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitEquip
import a75f.io.logic.bo.building.hyperstatsplit.common.UserIntents
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.jobs.HyperStatSplitUserIntentHandler
import android.util.Log


/**
 * Created by Manjunath K for HyperStat on 11-07-2022.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */

abstract class HyperStatSplitProfile : ZoneProfile(), RelayActions, AnalogOutActions, DoorWindowKeycardActions {

    lateinit var hsSplitHaystackUtil: HSSplitHaystackUtil
    var logicalPointsList: HashMap<Any, String> = HashMap()
    open var occupancyStatus: Occupancy = Occupancy.OCCUPIED
    protected val haystack = CCUHsApi.getInstance()


    abstract fun getHyperStatSplitEquip(node: Short): HyperStatSplitEquip?
    abstract fun addNewEquip(node: Short, room: String, floor: String, baseConfig: BaseProfileConfiguration)

    override fun doCoolingStage1(
        port: Port,
        coolingLoopOutput: Int,
        relayActivationHysteresis: Int,
        threshold: Int,
        relayStages: HashMap<String, Int>
    ) {
        var relayState = -1.0

        // Need to treat stage 1 cooling as true first stage if economizer is disabled (same on/off conditions as fan)
        // If economizer is enabled and stage 1 cooling is the second stage, follow logic for stage 2
        if (threshold == 0) {
            if (coolingLoopOutput > relayActivationHysteresis)
                relayState = 1.0
            else if (coolingLoopOutput <= 0)
                relayState = 0.0
            else {
                val currentPortStatus: Double = haystack.readHisValById(logicalPointsList[port]!!)
                relayState = if (currentPortStatus > 0) 1.0 else 0.0
            }
        } else {
            if (coolingLoopOutput > (threshold + (relayActivationHysteresis / 2)))
                relayState = 1.0
            else if (coolingLoopOutput <= (threshold - (relayActivationHysteresis / 2)))
                relayState = 0.0
            else if (coolingLoopOutput <= 0)
                relayState = 0.0
            else {
                val currentPortStatus: Double = haystack.readHisValById(logicalPointsList[port]!!)
                relayState = if (currentPortStatus > 0) 1.0 else 0.0
            }

        }

        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, relayState)
            if (relayState == 1.0) {
                relayStages[Stage.COOLING_1.displayName] = 1
            }
            logIt( "$port = CoolingStage1:  $relayState")
        }
    }

    override fun doCoolingStage2(
        port: Port,
        coolingLoopOutput: Int,
        relayActivationHysteresis: Int,
        threshold: Int,
        relayStages: HashMap<String, Int>
    ) {
        var relayState = -1.0

        if (coolingLoopOutput > (threshold + (relayActivationHysteresis / 2)))
            relayState = 1.0
        else if (coolingLoopOutput <= (threshold - (relayActivationHysteresis / 2)))
            relayState = 0.0
        else if (coolingLoopOutput <= 0)
            relayState = 0.0
        else {
            val currentPortStatus: Double = haystack.readHisValById(logicalPointsList[port]!!)
            relayState = if (currentPortStatus > 0) 1.0 else 0.0
        }

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
        threshold: Int,
        relayStages: HashMap<String, Int>
    ) {
        var relayState = -1.0

        if (coolingLoopOutput > (threshold + (relayActivationHysteresis / 2)))
            relayState = 1.0
        else if (coolingLoopOutput <= (threshold - (relayActivationHysteresis / 2)))
            relayState = 0.0
        else if (coolingLoopOutput <= 0)
            relayState = 0.0
        else {
            val currentPortStatus: Double = haystack.readHisValById(logicalPointsList[port]!!)
            relayState = if (currentPortStatus > 0) 1.0 else 0.0
        }

        if (relayState != -1.0) {
            updateLogicalPointIdValue(logicalPointsList[port]!!, relayState)
            if (relayState == 1.0) {
                relayStages[Stage.COOLING_3.displayName] = 1
            }
            logIt( "$port = CoolingStage3:  $relayState")
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
        else if (heatingLoopOutput <= 0)
            relayState = 0.0
        else {
            val currentPortStatus: Double = haystack.readHisValById(logicalPointsList[port]!!)
            relayState = if (currentPortStatus > 0) 1.0 else 0.0
        }

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
        else if (heatingLoopOutput <= (divider - (relayActivationHysteresis / 2)))
            relayState = 0.0
        else {
            val currentPortStatus: Double = haystack.readHisValById(logicalPointsList[port]!!)
            relayState = if (currentPortStatus > 0) 1.0 else 0.0
        }

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
        else if (heatingLoopOutput <= (66 - (relayActivationHysteresis / 2)))
            relayState = 0.0
        else {
            val currentPortStatus: Double = haystack.readHisValById(logicalPointsList[port]!!)
            relayState = if (currentPortStatus > 0) 1.0 else 0.0
        }

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
        logIt("$relayPort = OccupiedEnabled  ${if (occupancyStatus == Occupancy.OCCUPIED) 1.0 else 0.0}")

    }

    override fun doHumidifierOperation(
        relayPort: Port,
        humidityHysteresis: Int,
        targetMinInsideHumidity: Double
    ) {
        // The humidifier is turned on whenever the humidity level drops below the targetMinInsideHumidty.
        // The humidifier will be turned off after being turned on when humidity goes humidityHysteresis above
        // the targetMinInsideHumidity. turns off when it drops 5% below threshold


        val currentHumidity = hsSplitHaystackUtil.getHumidity()
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


        val currentHumidity = hsSplitHaystackUtil.getHumidity()
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

    override fun doExhaustFanStage1(
        relayPort: Port,
        outsideAirFinalLoopOutput: Int,
        exhaustFanStage1Threshold: Int,
        exhaustFanHysteresis: Int
    ) {
        // Exhaust Fan Stage 1 is turned on whenever the oaoFinalLoopOutput goes above the exhaustFanStage1Threshold.
        // Exhaust Fan Stage 1 is turned off whenever the oaoFinalLoopOutput goes below the
        // exhaustFanStage1Threshold - exhaustFanHysteresis

        val currentPortStatus = haystack.readHisValById(logicalPointsList[relayPort]!!)
        var relayStatus = 0.0
        if (outsideAirFinalLoopOutput > exhaustFanStage1Threshold) {
            relayStatus = 1.0
        } else if (currentPortStatus > 0
            && outsideAirFinalLoopOutput > exhaustFanStage1Threshold - exhaustFanHysteresis) {
            relayStatus = 1.0
        }

        updateLogicalPointIdValue(logicalPointsList[relayPort]!!, relayStatus)
        logIt("$relayPort = ExhaustFanStage1  $relayStatus")

    }

    override fun doExhaustFanStage2(
        relayPort: Port,
        outsideAirFinalLoopOutput: Int,
        exhaustFanStage2Threshold: Int,
        exhaustFanHysteresis: Int
    ) {
        // Exhaust Fan Stage 2 is turned on whenever the oaoFinalLoopOutput goes above the exhaustFanStage2Threshold.
        // Exhaust Fan Stage 2 is turned off whenever the oaoFinalLoopOutput goes below the
        // exhaustFanStage2Threshold - exhaustFanHysteresis

        val currentPortStatus = haystack.readHisValById(logicalPointsList[relayPort]!!)
        var relayStatus = 0.0
        if (outsideAirFinalLoopOutput > exhaustFanStage2Threshold) {
            relayStatus = 1.0
        } else if (currentPortStatus > 0
            && outsideAirFinalLoopOutput > exhaustFanStage2Threshold - exhaustFanHysteresis) {
            relayStatus = 1.0
        }

        updateLogicalPointIdValue(logicalPointsList[relayPort]!!, relayStatus)
        logIt("$relayPort = ExhaustFanStage2  $relayStatus")

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

    /*
        Right now, no usages for Door/Window or Key Card sensors in HyperStat Split.

        Keeping these methods in place for future and for inheritance purposes, but they are not called in algos.
     */
    override fun doorWindowIsOpen(doorWindowEnabled: Double, doorWindowSensor: Double) {
        hsSplitHaystackUtil.updateDoorWindowValues(doorWindowEnabled,doorWindowSensor)
    }

    override fun keyCardIsInSlot(keycardEnabled: Double, keycardSensor: Double) {
        hsSplitHaystackUtil.updateKeycardValues(keycardEnabled,keycardSensor)
    }

    fun updateLogicalPointIdValue(pointId: String?, value: Double) {
        if(pointId != null) {
            hsSplitHaystackUtil.writeHisValueByID(pointId, value)
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
        equip: HyperStatSplitEquip, equipRef: String, fanModeSaved: Int,
        actualFanMode: Int, basicSettings: BasicSettings
    ): StandaloneFanStage {

        val currentOperatingMode = equip.hsSplitHaystackUtil.getOccupancyModePointValue().toInt()
        logIt("Fan Details :$occupancyStatus  ${basicSettings.fanMode}  $fanModeSaved")
        if (isEligibleToAuto(basicSettings,currentOperatingMode)) {
            logIt("Resetting the Fan status back to  AUTO: ")
            HyperStatSplitUserIntentHandler.updateHyperStatSplitUIPoints(
                equipRef = equipRef,
                command = "zone and sp and fan and operation and mode",
                value = StandaloneFanStage.AUTO.ordinal.toDouble(),
                CCUHsApi.getInstance().ccuUserName
            )
            return StandaloneFanStage.AUTO
        }

        if ((occupancyStatus == Occupancy.OCCUPIED
                    || occupancyStatus == Occupancy.AUTOFORCEOCCUPIED
                    || occupancyStatus == Occupancy.FORCEDOCCUPIED
                    || Occupancy.values()[currentOperatingMode] == Occupancy.PRECONDITIONING)
            && basicSettings.fanMode == StandaloneFanStage.AUTO && fanModeSaved != 0) {
            logIt("Resetting the Fan status back to ${StandaloneFanStage.values()[fanModeSaved]}")
            HyperStatSplitUserIntentHandler.updateHyperStatSplitUIPoints(
                equipRef = equipRef,
                command = "zone and sp and fan and operation and mode",
                value = actualFanMode.toDouble(),
                CCUHsApi.getInstance().ccuUserName
            )
            return StandaloneFanStage.values()[actualFanMode]
        }
        return  StandaloneFanStage.values()[equip.hsSplitHaystackUtil.getCurrentFanMode().toInt()]
    }

    private fun isEligibleToAuto(basicSettings: BasicSettings, currentOperatingMode: Int ): Boolean{
        return (occupancyStatus != Occupancy.OCCUPIED
            && occupancyStatus != Occupancy.AUTOFORCEOCCUPIED
            && occupancyStatus != Occupancy.FORCEDOCCUPIED
            && Occupancy.values()[currentOperatingMode] != Occupancy.PRECONDITIONING
            && basicSettings.fanMode != StandaloneFanStage.OFF
            && basicSettings.fanMode != StandaloneFanStage.AUTO
            && basicSettings.fanMode != StandaloneFanStage.LOW_ALL_TIME
            && basicSettings.fanMode != StandaloneFanStage.MEDIUM_ALL_TIME
            && basicSettings.fanMode != StandaloneFanStage.HIGH_ALL_TIME
        )
    }

    fun setOperatingMode(currentTemp: Double, averageDesiredTemp: Double, basicSettings: BasicSettings, equip: HyperStatSplitEquip){
        var zoneOperatingMode = ZoneState.DEADBAND.ordinal
        if(currentTemp < averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.COOL_ONLY) {
            zoneOperatingMode = ZoneState.HEATING.ordinal
        }
        else if(currentTemp >= averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
            zoneOperatingMode = ZoneState.COOLING.ordinal
        }
        logIt("averageDesiredTemp $averageDesiredTemp" + "zoneOperatingMode ${ZoneState.values()[zoneOperatingMode]}")
        equip.hsSplitHaystackUtil.setProfilePoint("operating and mode", zoneOperatingMode.toDouble())
    }

    private fun logIt(msg: String){
        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, msg)
    }

    // To run specific fan speed while running aux heating
    enum class FanSpeed {
        OFF,LOW,MEDIUM,HIGH
    }

    fun getAverageTemp(userIntents: UserIntents): Double{
        return (userIntents.zoneCoolingTargetTemperature + userIntents.zoneHeatingTargetTemperature) / 2.0
    }

}