package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.HyperStatSplitEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Point
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZoneProfile
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.util.uiutils.updateUserIntentPoints


/**
 * Created by Manjunath K for HyperStat on 11-07-2022.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */

abstract class HyperStatSplitProfile(equipRef: String, var nodeAddress: Short) : ZoneProfile() {

    var hssEquip : HyperStatSplitEquip = HyperStatSplitEquip(equipRef)

    var fanEnabledStatus = false
    var lowestStageFanLow = false
    var lowestStageFanMedium = false
    var lowestStageFanHigh = false

    fun resetFanStatus() {
        fanEnabledStatus = false
        lowestStageFanLow = false
        lowestStageFanMedium = false
        lowestStageFanHigh = false
    }

    open var occupancyStatus: Occupancy = Occupancy.NONE
    protected val haystack: CCUHsApi = CCUHsApi.getInstance()

    abstract fun isHeatingActive() : Boolean
    abstract fun isCoolingActive() : Boolean

    private fun resetPoint(point: Point) {
        if (point.pointExists()) {
            point.writeHisVal(0.0)
        }
    }

    open fun resetRelayLogicalPoints() {
        hssEquip.apply {
            listOf(
                coolingStage1, coolingStage2, coolingStage3,
                heatingStage1, heatingStage2, heatingStage3,
                fanLowSpeed, fanMediumSpeed, fanHighSpeed,
                fanEnable, occupiedEnable, humidifierEnable, dehumidifierEnable,
                exhaustFanStage1, exhaustFanStage2, dcvDamper, compressorStage1,
                compressorStage2, compressorStage3, auxHeatingStage1, auxHeatingStage2,
                changeOverCooling, changeOverHeating
            ).forEach { resetPoint(it) }
        }
    }

    open fun resetAnalogOutLogicalPoints() {
        hssEquip.apply {
            listOf(
                coolingSignal, heatingSignal, linearFanSpeed, oaoDamper,
                stagedFanSpeed, compressorSpeed, dcvDamperModulating
            ).forEach { resetPoint(it) }
        }
    }

    open fun resetLoops() {
        hssEquip.apply {
            listOf(
                coolingLoopOutput, heatingLoopOutput, compressorLoopOutput,
                fanLoopOutput, economizingLoopOutput, dcvLoopOutput,
                outsideAirLoopOutput, outsideAirFinalLoopOutput
            ).forEach { resetPoint(it) }
        }
        hssEquip.derivedFanLoopOutput.data = 0.0
    }

    open fun resetAllLogicalPointValues() {
        resetLoops()
        resetRelayLogicalPoints()
        resetAnalogOutLogicalPoints()
    }

    fun fallBackFanMode(
        equip: HyperStatSplitEquip , fanModeSaved: Int , basicSettings: BasicSettings
    ): StandaloneFanStage {

        logIt("FanModeSaved in Shared Preference $fanModeSaved")
        val currentOperatingMode = hssEquip.occupancyMode.readHisVal().toInt()

        // If occupancy is unoccupied or demand response unoccupied and the fan mode is current occupied then remove the fan mode from cache
        if ((occupancyStatus == Occupancy.UNOCCUPIED || occupancyStatus == Occupancy.DEMAND_RESPONSE_UNOCCUPIED ) && isFanModeCurrentOccupied(fanModeSaved)) {
            logIt("Clearing FanModeSaved in Shared Preference $fanModeSaved")
            FanModeCacheStorage.getHyperStatSplitFanModeCache().removeFanModeFromCache(equip.equipRef)
            logIt("Clearing FanModeSaved in Shared Preference ${FanModeCacheStorage.getHyperStatSplitFanModeCache().getFanModeFromCache(equip.equipRef)}")
        }
        logIt("Fall back fan mode " + basicSettings.fanMode + " conditioning mode " + basicSettings.conditioningMode)
        logIt("Fan Details :$occupancyStatus  ${basicSettings.fanMode}  $fanModeSaved")
        if (isEligibleToAuto(basicSettings,currentOperatingMode)) {
            logIt("Resetting the Fan status back to  AUTO: ")
            updateUserIntentPoints(
                equipRef = equip.equipRef,
                point = equip.fanOpMode,
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
            updateUserIntentPoints(
                equipRef = equip.equipRef,
                point = equip.fanOpMode,
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
        return (userIntents.coolingDesiredTemp + userIntents.heatingDesiredTemp) / 2.0
    }

    override fun getNodeAddresses() : HashSet<Short> {
        val nodeSet : HashSet<Short> = HashSet()
        nodeSet.add(nodeAddress)
        return nodeSet
    }

    fun updateDomainEquip(equipRef: String) {
        if (Domain.equips.containsKey(equipRef) && hssEquip == Domain.equips[equipRef]) {
            hssEquip = Domain.equips[hssEquip.equipRef] as HyperStatSplitEquip
        } else {
            hssEquip = HyperStatSplitEquip(equipRef)
            Domain.equips[equipRef] = hssEquip
        }
    }

}