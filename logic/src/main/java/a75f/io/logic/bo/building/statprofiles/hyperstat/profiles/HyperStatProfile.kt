package a75f.io.logic.bo.building.statprofiles.hyperstat.profiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.ZoneProfile
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.statprofiles.statcontrollers.HyperStatControlFactory
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.HyperStatProfileTuners
import a75f.io.logic.bo.building.statprofiles.util.StagesCounts
import a75f.io.logic.bo.building.statprofiles.util.StatLoopController
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.isHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMediumUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.updateLoopOutputs
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.controlcomponents.util.isSoftOccupied
import a75f.io.logic.util.uiutils.HyperStatUserIntentHandler
import a75f.io.logic.util.uiutils.HyperStatUserIntentHandler.Companion.hyperStatStatus
import a75f.io.logic.util.uiutils.updateUserIntentPoints


/**
 * Created by Manjunath K on 11-07-2022.
 */

abstract class HyperStatProfile(val logTag: String) : ZoneProfile() {


    private val haystack = CCUHsApi.getInstance()
    var coolingLoopOutput = 0
    var heatingLoopOutput = 0
    var fanLoopOutput = 0
    var dcvLoopOutput = 0
    var compressorLoopOutput = 0
    var stageCounts = StagesCounts()
    // These are require parameter for CPU profile
    var defaultFanLoopOutput = 0.0
    var previousFanLoopVal = 0
    var previousFanLoopValStaged = 0
    var fanLoopCounter = 0
    var hasZeroFanLoopBeenHandled = false

    var previousOccupancyStatus: Occupancy = Occupancy.NONE
    var occupancyBeforeDoorWindow: Occupancy = Occupancy.NONE
    var curState = ZoneState.DEADBAND
    val derivedFanLoopOutput = CalibratedPoint(DomainName.fanLoopOutput ,"",0.0)
    var zoneOccupancyState = CalibratedPoint(DomainName.zoneOccupancy, "", 0.0)

    var logicalPointsList = HashMap<Port, String>()
    var occupancyStatus: Occupancy = Occupancy.NONE

    var doorWindowSensorOpenStatus = false
    var runFanLowDuringDoorWindow = false

    var fanEnabledStatus = false
    var lowestStageFanLow = false
    var lowestStageFanMedium = false
    var lowestStageFanHigh = false

    val loopController = StatLoopController()

    fun evaluateCoolingLoop(userIntents: UserIntents) {
        coolingLoopOutput = loopController.calculateCoolingLoopOutput(
            currentTemp, userIntents.coolingDesiredTemp
        ).toInt().coerceAtLeast(0)
    }

    fun evaluateHeatingLoop(userIntents: UserIntents) {
        heatingLoopOutput = loopController.calculateHeatingLoopOutput(
            userIntents.heatingDesiredTemp, currentTemp
        ).toInt().coerceAtLeast(0)
    }

    open fun evaluateFanOutput(
        basicSettings: BasicSettings,
        tuners: HyperStatProfileTuners
    ) {
        val mode = basicSettings.conditioningMode
        val multiplier = tuners.analogFanSpeedMultiplier

        fanLoopOutput = when {
            coolingLoopOutput > 0 &&
                    (mode == StandaloneConditioningMode.COOL_ONLY || mode == StandaloneConditioningMode.AUTO) -> {
                compressorLoopOutput = coolingLoopOutput
                (coolingLoopOutput * multiplier).toInt().coerceAtMost(100)
            }

            heatingLoopOutput > 0 &&
                    (mode == StandaloneConditioningMode.HEAT_ONLY || mode == StandaloneConditioningMode.AUTO) -> {
                compressorLoopOutput = heatingLoopOutput
                (heatingLoopOutput * multiplier).toInt().coerceAtMost(100)
            }

            else -> 0
        }
    }

    fun evaluateDcvLoop(equip: HyperStatEquip, config: HyperStatConfiguration) {
        val rawCo2 = equip.zoneCo2.readHisVal().toInt()
        val deltaCo2 = rawCo2 - config.zoneCO2Threshold.currentVal
        dcvLoopOutput = (deltaCo2 / config.zoneCO2DamperOpeningRate.currentVal).toInt().coerceIn(0, 100)
    }

    fun resetFanLowestFanStatus() {
        lowestStageFanLow = false
        lowestStageFanMedium = false
        lowestStageFanHigh = false
    }

    fun resetLoopOutputs() {
        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        dcvLoopOutput = 0
        compressorLoopOutput = 0
    }

    private fun resetControllers(factory: HyperStatControlFactory, statEquip: HyperStatEquip) {
        // Add controller here if any new stage controller is added
        listOf(
            ControllerNames.COOLING_STAGE_CONTROLLER,
            ControllerNames.HEATING_STAGE_CONTROLLER,
            ControllerNames.FAN_SPEED_CONTROLLER,
            ControllerNames.COMPRESSOR_RELAY_CONTROLLER,
        ).forEach {
            val controller = factory.getController(it, this)
            controller?.resetController()
        }

        defaultFanLoopOutput = 0.0
        previousFanLoopVal = 0
        previousFanLoopValStaged = 0
        fanLoopCounter = 0
        hasZeroFanLoopBeenHandled = false
        resetLoopOutputs()
        resetLogicalPoints()
        statEquip.analogOutStages.clear()
        statEquip.relayStages.clear()

    }

    fun handleChangeOfDirection(
        currentTemp: Double, userIntents: UserIntents,
        factory: HyperStatControlFactory, equip: HyperStatEquip
        ) {
        if (currentTemp > userIntents.coolingDesiredTemp && state != ZoneState.COOLING) {
            loopController.resetCoolingControl()
            state = ZoneState.COOLING
            resetControllers(factory, equip)
        } else if (currentTemp < userIntents.heatingDesiredTemp && state != ZoneState.HEATING) {
            loopController.resetHeatingControl()
            state = ZoneState.HEATING
            resetControllers(factory, equip)
        }
    }


    fun doAnalogFanAction(
        port: Port,
        fanLowPercent: Int,
        fanMediumPercent: Int,
        fanHighPercent: Int,
        equip: HyperStatEquip,
        basicSettings: BasicSettings,
        fanLoopOutput: Int,
        isFanGoodToRUn: Boolean = true
    ) {
        val mode = equip.fanOpMode
        if (basicSettings.fanMode != StandaloneFanStage.OFF) {
            var fanLoopForAnalog = 0
            if (isFanGoodToRUn && basicSettings.fanMode == StandaloneFanStage.AUTO) {
                if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
                    updateLogicalPoint(logicalPointsList[port]!!, 0.0)
                    return
                }
                fanLoopForAnalog = fanLoopOutput
            } else {
                when {
                    isLowUserIntentFanMode(mode) -> fanLoopForAnalog = fanLowPercent
                    isMediumUserIntentFanMode(mode) -> fanLoopForAnalog = fanMediumPercent
                    isHighUserIntentFanMode(mode) -> fanLoopForAnalog = fanHighPercent
                }
            }
            if (fanLoopForAnalog > 0) equip.analogOutStages[StatusMsgKeys.FAN_SPEED.name] =
                1 else equip.analogOutStages.remove(StatusMsgKeys.FAN_SPEED.name)
            updateLogicalPoint(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
        }
    }


    fun doAnalogDCVAction(
        port: Port,
        analogOutStages: HashMap<String, Int>,
        zoneCO2Threshold: Double,
        zoneCO2DamperOpeningRate: Double,
        equip: HyperStatEquip
    ) {
        val co2Value = equip.zoneCo2.readHisVal()
        logIt(
            "doAnalogDCVAction: co2Value : $co2Value zoneCO2Threshold: $zoneCO2Threshold zoneCO2DamperOpeningRate $zoneCO2DamperOpeningRate"
        )

        if (isSoftOccupied(zoneOccupancyState)) {
            updateLogicalPoint(logicalPointsList[port]!!, dcvLoopOutput.toDouble())
            if(dcvLoopOutput > 0) { analogOutStages[StatusMsgKeys.DCV_DAMPER.name] = dcvLoopOutput }
        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }

    fun doorWindowIsOpen(
        doorWindowEnabled: Double,
        doorWindowSensor: Double,
        equip: HyperStatEquip
    ) {
        equip.doorWindowSensingEnable.writePointValue(doorWindowEnabled)
        equip.doorWindowSensorInput.writePointValue(doorWindowSensor)
    }

    fun keyCardIsInSlot(
        keycardEnabled: Double,
        keycardSensor: Double,
        equip: HyperStatEquip
    ) {
        equip.keyCardSensingEnable.writePointValue(keycardEnabled)
        equip.keyCardSensorInput.writePointValue(keycardSensor)
    }

    fun updateLogicalPoint(pointId: String?, value: Double) {
        if (pointId != null) {
            CCUHsApi.getInstance().writeHisValById(pointId, value)
        }
    }

    fun resetLogicalPoint(pointId: String?) {
        if (pointId != null) {
            updateLogicalPoint(pointId, 0.0)
        }
    }

    fun getCurrentLogicalPointStatus(pointId: String): Double {
        return haystack.readHisValById(pointId)
    }

    fun resetLogicalPoints() {
        logicalPointsList.forEach { (_, pointId) -> haystack.writeHisValById(pointId, 0.0) }
    }

    fun fallBackFanMode(
        equip: HyperStatEquip, equipRef: String, fanModeSaved: Int, basicSettings: BasicSettings
    ): StandaloneFanStage {

        val currentOccupancy = equip.occupancyMode.readHisVal().toInt()
        logIt("FanModeSaved in Shared Preference $fanModeSaved : current occupancy $currentOccupancy")


        // If occupancy is unoccupied and the fan mode is current occupied then remove the fan mode from cache
        if ((occupancyStatus == Occupancy.UNOCCUPIED || occupancyStatus == Occupancy.DEMAND_RESPONSE_UNOCCUPIED) && isFanModeCurrentOccupied(fanModeSaved)) {
            FanModeCacheStorage.getHyperStatFanModeCache().removeFanModeFromCache(equipRef)
        }

        logIt(
            "Fall back fan mode " + basicSettings.fanMode + " conditioning mode " + basicSettings.conditioningMode
                    + "\nFan Details :$occupancyStatus  ${basicSettings.fanMode}  $fanModeSaved"
        )
        if (isEligibleToAuto(basicSettings, equip)) {
            logIt("Resetting the Fan status back to  AUTO: ")
            updateUserIntentPoints(
                equipRef = equipRef,
                equip.fanOpMode,
                value = StandaloneFanStage.AUTO.ordinal.toDouble(),
                CCUHsApi.getInstance().ccuUserName
            )
            return StandaloneFanStage.AUTO
        }

        if (isSoftOccupied(equip.occupancyMode) && basicSettings.fanMode == StandaloneFanStage.AUTO && fanModeSaved != 0) {
            logIt("Resetting the Fan status back to ${StandaloneFanStage.values()[fanModeSaved]}")
            updateUserIntentPoints(
                equipRef = equipRef,
                equip.fanOpMode,
                value = fanModeSaved.toDouble(),
                CCUHsApi.getInstance().ccuUserName
            )
            return StandaloneFanStage.values()[fanModeSaved]
        }
        return StandaloneFanStage.values()[equip.fanOpMode.readPriorityVal().toInt()]
    }

    private fun isFanModeCurrentOccupied(value: Int): Boolean {
        val basicSettings = StandaloneFanStage.values()[value]
        return (basicSettings == StandaloneFanStage.LOW_CUR_OCC
                || basicSettings == StandaloneFanStage.MEDIUM_CUR_OCC
                || basicSettings == StandaloneFanStage.HIGH_CUR_OCC)
    }

    private fun isEligibleToAuto(basicSettings: BasicSettings, equip: HyperStatEquip): Boolean {
        return (!isSoftOccupied(equip.occupancyMode)
                && basicSettings.fanMode != StandaloneFanStage.OFF
                && basicSettings.fanMode != StandaloneFanStage.AUTO
                && basicSettings.fanMode != StandaloneFanStage.LOW_ALL_TIME
                && basicSettings.fanMode != StandaloneFanStage.MEDIUM_ALL_TIME
                && basicSettings.fanMode != StandaloneFanStage.HIGH_ALL_TIME
                )
    }

    fun getAverageTemp(userIntents: UserIntents): Double {
        if(haystack.isScheduleSlotExitsForRoom(equip!!.id)) {
            CcuLog.d(TAG, " Schedule slot not  exists for HyperStat  room ${equip!!.id} nodeAddress ${equip!!.group}")
            return (CCUUtils.DEFAULT_HEATING_DESIRED + CCUUtils.DEFAULT_COOLING_DESIRED + haystack.getUnoccupiedSetback(equip!!.id)) / 2.0
        }
        return (userIntents.coolingDesiredTemp + userIntents.heatingDesiredTemp) / 2.0
    }


    fun handleRFDead(equip: HyperStatEquip) {
        state = ZoneState.RFDEAD
        equip.operatingMode.writeHisVal(state.ordinal.toDouble())
        equip.equipStatus.writeHisVal(state.ordinal.toDouble())
        equip.equipStatusMessage.writeDefaultVal(RFDead)
        hyperStatStatus[equip.equipRef] = RFDead
        logIt( "RF Signal is Dead ${equip.nodeAddress}")
    }

    fun handleDeadZone(equip: HyperStatEquip) {
        state = ZoneState.TEMPDEAD
        resetPoints(equip)
        equip.operatingMode.writeHisVal(state.ordinal.toDouble())
        equip.equipStatus.writeHisVal(state.ordinal.toDouble())
        equip.equipStatusMessage.writeDefaultVal(ZoneTempDead)
        hyperStatStatus[equip.equipRef] = ZoneTempDead
    }

    private fun resetPoints(equip: HyperStatEquip) {
        updateLoopOutputs(
            0, equip.coolingLoopOutput,
            0, equip.heatingLoopOutput,
            0, equip.fanLoopOutput,
            0, equip.dcvLoopOutput,
            (equip is HpuV2Equip), 0, if (equip is HpuV2Equip) equip.compressorLoopOutput else null,
        )

        coolingLoopOutput = equip.coolingLoopOutput.readHisVal().toInt()
        heatingLoopOutput = equip.heatingLoopOutput.readHisVal().toInt()
        fanLoopOutput = equip.fanLoopOutput.readHisVal().toInt()
        dcvLoopOutput = equip.dcvLoopOutput.readHisVal().toInt()
        if (equip is HpuV2Equip) {
            compressorLoopOutput = equip.compressorLoopOutput.readHisVal().toInt()
        }

        resetLogicalPoints()
        resetEquip(equip)
        HyperStatUserIntentHandler.updateHyperStatStatus(ZoneTempState.TEMP_DEAD, equip, logTag)
    }

    fun resetEquip(equip: HyperStatEquip) {
        equip.relayStages.clear()
        equip.analogOutStages.clear()
    }

    fun logIt(msg: String) {
        CcuLog.d(logTag, msg)
    }
}
