package a75f.io.logic.bo.building.hyperstatsplit.profiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZoneProfile
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstatsplit.common.BasicSettings
import a75f.io.logic.bo.building.hyperstatsplit.common.HSSplitHaystackUtil
import a75f.io.domain.HyperStatSplitEquip
import a75f.io.domain.api.DomainName
import a75f.io.logic.bo.building.hyperstatsplit.common.FanModeCacheStorage
import a75f.io.logic.bo.building.hyperstatsplit.common.UserIntents
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.jobs.HyperStatSplitUserIntentHandler



/**
 * Created by Manjunath K for HyperStat on 11-07-2022.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */

abstract class HyperStatSplitProfile(equipRef: String, nodeAddress: Short) : ZoneProfile() {

    var nodeAddress: Short

    init {
        this.nodeAddress = nodeAddress
    }

    var hssEquip : HyperStatSplitEquip = HyperStatSplitEquip(equipRef)
    var hsSplitHaystackUtil: HSSplitHaystackUtil = HSSplitHaystackUtil(equipRef, CCUHsApi.getInstance())

    open var occupancyStatus: Occupancy = Occupancy.OCCUPIED
    protected val haystack = CCUHsApi.getInstance()

    abstract fun isHeatingActive() : Boolean
    abstract fun isCoolingActive() : Boolean

    open fun doCoolingStage1(
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
                val currentPortStatus: Double = hssEquip.coolingStage1.readHisVal()
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
                val currentPortStatus: Double = hssEquip.coolingStage1.readHisVal()
                relayState = if (currentPortStatus > 0) 1.0 else 0.0
            }

        }

        if (relayState != -1.0) {
            hssEquip.coolingStage1.writeHisVal(relayState)
            if (relayState == 1.0) {
                relayStages[Stage.COOLING_1.displayName] = 1
            }
            logIt( "$port = CoolingStage1:  $relayState")
        }
    }

    open fun doCoolingStage2(
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
            val currentPortStatus: Double = hssEquip.coolingStage2.readHisVal()
            relayState = if (currentPortStatus > 0) 1.0 else 0.0
        }

        if (relayState != -1.0) {
            hssEquip.coolingStage2.writeHisVal(relayState)
            if (relayState == 1.0) {
                relayStages[Stage.COOLING_2.displayName] = 1
            }
            logIt( "$port = CoolingStage2:  $relayState")
        }
    }

    open fun doCoolingStage3(
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
            val currentPortStatus: Double = hssEquip.coolingStage3.readHisVal()
            relayState = if (currentPortStatus > 0) 1.0 else 0.0
        }

        if (relayState != -1.0) {
            hssEquip.coolingStage3.writeHisVal(relayState)
            if (relayState == 1.0) {
                relayStages[Stage.COOLING_3.displayName] = 1
            }
            logIt( "$port = CoolingStage3:  $relayState")
        }
    }

    open fun doHeatingStage1(
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
            val currentPortStatus: Double = hssEquip.heatingStage1.readHisVal()
            relayState = if (currentPortStatus > 0) 1.0 else 0.0
        }

        if (relayState != -1.0) {
            hssEquip.heatingStage1.writeHisVal(relayState)
            if (relayState == 1.0) {
                relayStages[Stage.HEATING_1.displayName] = 1
            }
            logIt("$port = HeatingStage1: $relayState")
        }

    }

    open fun doHeatingStage2(
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
            val currentPortStatus: Double = hssEquip.heatingStage2.readHisVal()
            relayState = if (currentPortStatus > 0) 1.0 else 0.0
        }

        if (relayState != -1.0) {
            hssEquip.heatingStage2.writeHisVal(relayState)
            if (relayState == 1.0) {
                relayStages[Stage.HEATING_2.displayName] = 1
            }
            logIt("$port = HeatingStage2:  $relayState")
        }
    }

    open fun doHeatingStage3(
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
            val currentPortStatus: Double = hssEquip.heatingStage3.readHisVal()
            relayState = if (currentPortStatus > 0) 1.0 else 0.0
        }

        if (relayState != -1.0) {
            hssEquip.heatingStage3.writeHisVal(relayState)
            if (relayState == 1.0) {
                relayStages[Stage.HEATING_3.displayName] = 1
            }
            logIt("$port = HeatingStage3  $relayState")
        }
    }

    open fun doFanEnabled(currentState: ZoneState, whichPort: Port, fanLoopOutput: Int) {
        // Then Relay will be turned On when the zone is in occupied mode Or
        // any conditioning is happening during an unoccupied schedule

        if (occupancyStatus == Occupancy.OCCUPIED || fanLoopOutput > 0) {
            hssEquip.fanEnable.writeHisVal(1.0)
        } else if (occupancyStatus != Occupancy.OCCUPIED || (currentState == ZoneState.COOLING || currentState == ZoneState.HEATING)) {
            hssEquip.fanEnable.writeHisVal(0.0)
        }
    }

    open fun doOccupiedEnabled(relayPort: Port) {
        // Relay will be turned on when module is in occupied state
        hssEquip.occupiedEnable.writeHisVal(
            if (occupancyStatus == Occupancy.OCCUPIED) 1.0 else 0.0
        )
        logIt("$relayPort = OccupiedEnabled  ${if (occupancyStatus == Occupancy.OCCUPIED) 1.0 else 0.0}")

    }

    open fun doHumidifierOperation(
        relayPort: Port,
        humidityHysteresis: Int,
        targetMinInsideHumidity: Double
    ) {
        // The humidifier is turned on whenever the humidity level drops below the targetMinInsideHumidty.
        // The humidifier will be turned off after being turned on when humidity goes humidityHysteresis above
        // the targetMinInsideHumidity. turns off when it drops 5% below threshold


        val currentHumidity = hssEquip.zoneHumidity.readHisVal()
        val currentPortStatus = hssEquip.humidifierEnable.readHisVal()
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

        hssEquip.humidifierEnable.writeHisVal(relayStatus)
        logIt( "$relayPort = Humidifier  $relayStatus")
    }

    open fun doDeHumidifierOperation(
        relayPort: Port,
        humidityHysteresis: Int,
        targetMaxInsideHumidity: Double
    ) {
        // If the dehumidifier is turned on whenever the humidity level goes above the targetMaxInsideHumidity.
        // The humidifier will be turned off after being turned on when humidity drops humidityHysteresis below
        // the targetMaxInsideHumidity. Turns off when it crosses over 5% above the threshold


        val currentHumidity = hssEquip.zoneHumidity.readHisVal()
        val currentPortStatus = hssEquip.dehumidifierEnable.readHisVal()
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

        hssEquip.dehumidifierEnable.writeHisVal(relayStatus)
        logIt("$relayPort = DeHumidifier  $relayStatus")
    }

    open fun doExhaustFanStage1(
        relayPort: Port,
        outsideAirFinalLoopOutput: Int,
        exhaustFanStage1Threshold: Int,
        exhaustFanHysteresis: Int
    ) {
        // Exhaust Fan Stage 1 is turned on whenever the oaoFinalLoopOutput goes above the exhaustFanStage1Threshold.
        // Exhaust Fan Stage 1 is turned off whenever the oaoFinalLoopOutput goes below the
        // exhaustFanStage1Threshold - exhaustFanHysteresis

        val currentPortStatus = hssEquip.exhaustFanStage1.readHisVal()
        var relayStatus = 0.0
        if (outsideAirFinalLoopOutput > exhaustFanStage1Threshold) {
            relayStatus = 1.0
        } else if (currentPortStatus > 0
            && outsideAirFinalLoopOutput > exhaustFanStage1Threshold - exhaustFanHysteresis) {
            relayStatus = 1.0
        }

        hssEquip.exhaustFanStage1.writeHisVal(relayStatus)
        logIt("$relayPort = ExhaustFanStage1  $relayStatus")

    }

    open fun doExhaustFanStage2(
        relayPort: Port,
        outsideAirFinalLoopOutput: Int,
        exhaustFanStage2Threshold: Int,
        exhaustFanHysteresis: Int
    ) {
        // Exhaust Fan Stage 2 is turned on whenever the oaoFinalLoopOutput goes above the exhaustFanStage2Threshold.
        // Exhaust Fan Stage 2 is turned off whenever the oaoFinalLoopOutput goes below the
        // exhaustFanStage2Threshold - exhaustFanHysteresis

        val currentPortStatus = hssEquip.exhaustFanStage2.readHisVal()
        var relayStatus = 0.0
        if (outsideAirFinalLoopOutput > exhaustFanStage2Threshold) {
            relayStatus = 1.0
        } else if (currentPortStatus > 0
            && outsideAirFinalLoopOutput > exhaustFanStage2Threshold - exhaustFanHysteresis) {
            relayStatus = 1.0
        }

        hssEquip.exhaustFanStage2.writeHisVal(relayStatus)
        logIt("$relayPort = ExhaustFanStage2  $relayStatus")

    }

    open fun doDcvDamper(
        relayPort: Port,
        dcvLoopOutput: Int,
        relayActivationHysteresis: Int
    ) {
        // DCV Damper is turned on whenever the dcvLoopOutput goes above relayActivationHysteresis
        // DCV Damper is turned off whenever the dcvLoopOutput is zero

        val currentPortStatus = hssEquip.dcvDamper.readHisVal()
        var relayStatus = 0.0
        if (dcvLoopOutput > relayActivationHysteresis) {
            relayStatus = 1.0
        } else if (currentPortStatus > 0
            && dcvLoopOutput > 0) {
            relayStatus = 1.0
        }

        hssEquip.dcvDamper.writeHisVal(relayStatus)
        logIt("$relayPort = DcvDamper  $relayStatus")

    }


    open fun doAnalogCooling(
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        coolingLoopOutput: Int
    ) {
        if (conditioningMode.ordinal == StandaloneConditioningMode.COOL_ONLY.ordinal ||
            conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
        ) {
            hssEquip.coolingSignal.writeHisVal(coolingLoopOutput.toDouble())
            if (coolingLoopOutput > 0) analogOutStages[AnalogOutput.COOLING.name] =
                coolingLoopOutput
        } else {
            hssEquip.coolingSignal.writeHisVal(0.0)
        }
    }

    open fun doAnalogHeating(
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        heatingLoopOutput: Int
    ) {
        if (conditioningMode.ordinal == StandaloneConditioningMode.HEAT_ONLY.ordinal ||
            conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
        ) {
            hssEquip.heatingSignal.writeHisVal(heatingLoopOutput.toDouble())
            if (heatingLoopOutput > 0) analogOutStages[AnalogOutput.HEATING.name] =
                heatingLoopOutput
        } else {
            hssEquip.heatingSignal.writeHisVal(0.0)
        }
    }

    fun updateLogicalPointIdValue(pointId: String?, value: Double) {
        if(pointId != null) {
            hsSplitHaystackUtil.writeHisValueByID(pointId, value)
        }else{
            logIt("updateLogicalPointIdValue: But point id is null !!")
        }
    }

    fun getCurrentLogicalPointStatus(pointId: String): Double {
        return haystack.readHisValById(pointId)
    }

    open fun resetAllLogicalPointValues() {

        if (hssEquip.coolingLoopOutput.pointExists()) { hssEquip.coolingLoopOutput.writeHisVal(0.0) }
        if (hssEquip.heatingLoopOutput.pointExists()) { hssEquip.heatingLoopOutput.writeHisVal(0.0) }
        if (hssEquip.fanLoopOutput.pointExists()) { hssEquip.fanLoopOutput.writeHisVal(0.0) }

        if (hssEquip.economizingLoopOutput.pointExists()) { hssEquip.economizingLoopOutput.writeHisVal(0.0) }
        if (hssEquip.dcvLoopOutput.pointExists()) { hssEquip.dcvLoopOutput.writeHisVal(0.0) }
        if (hssEquip.outsideAirLoopOutput.pointExists()) { hssEquip.outsideAirLoopOutput.writeHisVal(0.0) }
        if (hssEquip.outsideAirFinalLoopOutput.pointExists()) { hssEquip.outsideAirFinalLoopOutput.writeHisVal(0.0) }

        if (hssEquip.coolingStage1.pointExists()) hssEquip.coolingStage1.writeHisVal(0.0)
        if (hssEquip.coolingStage2.pointExists()) hssEquip.coolingStage2.writeHisVal(0.0)
        if (hssEquip.coolingStage3.pointExists()) hssEquip.coolingStage3.writeHisVal(0.0)
        if (hssEquip.heatingStage1.pointExists()) hssEquip.heatingStage1.writeHisVal(0.0)
        if (hssEquip.heatingStage2.pointExists()) hssEquip.heatingStage2.writeHisVal(0.0)
        if (hssEquip.heatingStage3.pointExists()) hssEquip.heatingStage3.writeHisVal(0.0)
        if (hssEquip.fanLowSpeed.pointExists()) hssEquip.fanLowSpeed.writeHisVal(0.0)
        if (hssEquip.fanMediumSpeed.pointExists()) hssEquip.fanMediumSpeed.writeHisVal(0.0)
        if (hssEquip.fanHighSpeed.pointExists()) hssEquip.fanHighSpeed.writeHisVal(0.0)
        if (hssEquip.fanEnable.pointExists()) hssEquip.fanEnable.writeHisVal(0.0)
        if (hssEquip.occupiedEnable.pointExists()) hssEquip.occupiedEnable.writeHisVal(0.0)
        if (hssEquip.humidifierEnable.pointExists()) hssEquip.humidifierEnable.writeHisVal(0.0)
        if (hssEquip.dehumidifierEnable.pointExists()) hssEquip.dehumidifierEnable.writeHisVal(0.0)
        if (hssEquip.exhaustFanStage1.pointExists()) hssEquip.exhaustFanStage1.writeHisVal(0.0)
        if (hssEquip.exhaustFanStage2.pointExists()) hssEquip.exhaustFanStage2.writeHisVal(0.0)

        if (hssEquip.coolingSignal.pointExists()) hssEquip.coolingSignal.writeHisVal(0.0)
        if (hssEquip.heatingSignal.pointExists()) hssEquip.heatingSignal.writeHisVal(0.0)
        if (hssEquip.linearFanSpeed.pointExists()) hssEquip.linearFanSpeed.writeHisVal(0.0)
        if (hssEquip.oaoDamper.pointExists()) hssEquip.oaoDamper.writeHisVal(0.0)
        if (hssEquip.stagedFanSpeed.pointExists()) hssEquip.stagedFanSpeed.writeHisVal(0.0)

    }

    fun fallBackFanMode(
        equipRef: String , fanModeSaved: Int , basicSettings: BasicSettings
    ): StandaloneFanStage {

        logIt("FanModeSaved in Shared Preference $fanModeSaved")
        val currentOperatingMode = hssEquip.occupancyMode.readHisVal().toInt()

        // If occupancy is unoccupied or demand response unoccupied and the fan mode is current occupied then remove the fan mode from cache
        if ((occupancyStatus == Occupancy.UNOCCUPIED || occupancyStatus == Occupancy.DEMAND_RESPONSE_UNOCCUPIED ) && isFanModeCurrentOccupied(fanModeSaved)) {
            FanModeCacheStorage().removeFanModeFromCache(equipRef)
        }
        logIt("Fall back fan mode " + basicSettings.fanMode + " conditioning mode " + basicSettings.conditioningMode)
        logIt("Fan Details :$occupancyStatus  ${basicSettings.fanMode}  $fanModeSaved")
        if (isEligibleToAuto(basicSettings,currentOperatingMode)) {
            logIt("Resetting the Fan status back to  AUTO: ")
            HyperStatSplitUserIntentHandler.updateHyperStatSplitUIPoints(
                equipRef = equipRef,
                command = "domainName == \"" + DomainName.fanOpMode + "\"",
                value = StandaloneFanStage.AUTO.ordinal.toDouble(),
                CCUHsApi.getInstance().ccuUserName
            )
            return StandaloneFanStage.AUTO
        }

        if ((occupancyStatus == Occupancy.OCCUPIED
                    || occupancyStatus == Occupancy.AUTOFORCEOCCUPIED
                    || occupancyStatus == Occupancy.FORCEDOCCUPIED
                    || Occupancy.values()[currentOperatingMode] == Occupancy.PRECONDITIONING
                    || occupancyStatus == Occupancy.DEMAND_RESPONSE_OCCUPIED)
            && basicSettings.fanMode == StandaloneFanStage.AUTO && fanModeSaved != 0) {
            logIt("Resetting the Fan status back to ${StandaloneFanStage.values()[fanModeSaved]}")
            HyperStatSplitUserIntentHandler.updateHyperStatSplitUIPoints(
                equipRef = equipRef,
                command = "domainName == \"" + DomainName.fanOpMode + "\"",
                value = fanModeSaved.toDouble(),
                CCUHsApi.getInstance().ccuUserName
            )
            return StandaloneFanStage.values()[fanModeSaved]
        }
        return  StandaloneFanStage.values()[hssEquip.fanOpMode.readHisVal().toInt()]
    }

    private fun isFanModeCurrentOccupied(value : Int): Boolean {
        val basicSettings = StandaloneFanStage.values()[value]
        return (basicSettings == StandaloneFanStage.LOW_CUR_OCC || basicSettings == StandaloneFanStage.MEDIUM_CUR_OCC || basicSettings == StandaloneFanStage.HIGH_CUR_OCC)
    }

    private fun isEligibleToAuto(basicSettings: BasicSettings, currentOperatingMode: Int ): Boolean{
        return (occupancyStatus != Occupancy.OCCUPIED
            && occupancyStatus != Occupancy.AUTOFORCEOCCUPIED
            && occupancyStatus != Occupancy.FORCEDOCCUPIED
            && occupancyStatus != Occupancy.DEMAND_RESPONSE_OCCUPIED
            && Occupancy.values()[currentOperatingMode] != Occupancy.PRECONDITIONING
            && basicSettings.fanMode != StandaloneFanStage.OFF
            && basicSettings.fanMode != StandaloneFanStage.AUTO
            && basicSettings.fanMode != StandaloneFanStage.LOW_ALL_TIME
            && basicSettings.fanMode != StandaloneFanStage.MEDIUM_ALL_TIME
            && basicSettings.fanMode != StandaloneFanStage.HIGH_ALL_TIME
        )
    }

    fun setOperatingMode(currentTemp: Double, averageDesiredTemp: Double, basicSettings: BasicSettings){
        var zoneOperatingMode = ZoneState.DEADBAND.ordinal
        if(currentTemp < averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.COOL_ONLY) {
            zoneOperatingMode = ZoneState.HEATING.ordinal
        }
        else if(currentTemp >= averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
            zoneOperatingMode = ZoneState.COOLING.ordinal
        }
        logIt("averageDesiredTemp $averageDesiredTemp" + "zoneOperatingMode ${ZoneState.values()[zoneOperatingMode]}")
        hssEquip.operatingMode.writeHisVal(zoneOperatingMode.toDouble())
    }

    private fun logIt(msg: String){
        CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, msg)
    }

    fun getAverageTemp(userIntents: UserIntents): Double{
        return (userIntents.zoneCoolingTargetTemperature + userIntents.zoneHeatingTargetTemperature) / 2.0
    }

    override fun getNodeAddresses() : HashSet<Short> {
        val nodeSet : HashSet<Short> = HashSet()
        nodeSet.add(nodeAddress)
        return nodeSet
    }

    fun refreshEquip() {
        hssEquip = HyperStatSplitEquip(hssEquip.equipRef)
    }

}