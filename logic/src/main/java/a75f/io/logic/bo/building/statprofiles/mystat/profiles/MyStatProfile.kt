package a75f.io.logic.bo.building.statprofiles.mystat.profiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZoneProfile
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.statcontrollers.MyStatControlFactory
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.MyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.MyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.StagesCounts
import a75f.io.logic.bo.building.statprofiles.util.StatLoopController
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.updateLogicalPoint
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.controlcomponents.util.isSoftOccupied
import a75f.io.logic.util.uiutils.MyStatUserIntentHandler
import a75f.io.logic.util.uiutils.MyStatUserIntentHandler.Companion.myStatStatus
import a75f.io.logic.util.uiutils.updateUserIntentPoints

/**
 * Created by Manjunath K on 16-01-2025.
 */

abstract class MyStatProfile(val logTag: String) : ZoneProfile() {

    var fanEnabledStatus = false
    var lowestStageFanLow = false
    var lowestStageFanHigh = false

    var logicalPointsList = HashMap<Port, String>()
    open var occupancyStatus: Occupancy = Occupancy.NONE
    private val haystack = CCUHsApi.getInstance()

    var stageCounts = StagesCounts()

    var occupancyBeforeDoorWindow = Occupancy.NONE
    var doorWindowSensorOpenStatus = false
    var runFanLowDuringDoorWindow = false
    val derivedFanLoopOutput = CalibratedPoint(DomainName.fanLoopOutput ,"",0.0)
    var zoneOccupancyState = CalibratedPoint(DomainName.zoneOccupancy, "", 0.0)

    var coolingLoopOutput = 0
    var heatingLoopOutput = 0
    var fanLoopOutput = 0
    var dcvLoopOutput = 0
    var compressorLoopOutput = 0 // used in HPU

    var previousOccupancyStatus: Occupancy = Occupancy.NONE
    var previousFanStageStatus: MyStatFanStages = MyStatFanStages.OFF
    var defaultFanLoopOutput = 0.0
    var previousFanLoopVal = 0
    var previousFanLoopValStaged = 0
    var fanLoopCounter = 0

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
        basicSettings: MyStatBasicSettings,
        tuners: MyStatTuners
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

    fun evaluateDcvLoop(equip: MyStatEquip, config: MyStatConfiguration) {
        val rawCo2 = equip.zoneCo2.readHisVal().toInt()
        val deltaCo2 = rawCo2 - config.co2Threshold.currentVal
        dcvLoopOutput = (deltaCo2 / config.co2DamperOpeningRate.currentVal).toInt().coerceIn(0, 100)
    }

    fun resetLoopOutputs() {
        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        dcvLoopOutput = 0
        compressorLoopOutput = 0
    }

    fun resetFanLowestFanStatus() {
        lowestStageFanLow = false
        lowestStageFanHigh = false
        fanEnabledStatus = false
    }

    private fun resetControllers(factory: MyStatControlFactory, myStatEquip: MyStatEquip) {
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
        resetLoopOutputs()
        resetLogicalPoints()
        myStatEquip.analogOutStages.clear()
        myStatEquip.relayStages.clear()
    }

    fun handleChangeOfDirection(
        currentTemp: Double, userIntents: UserIntents,
        factory: MyStatControlFactory, equip: MyStatEquip
    ) {
        if (currentTemp > userIntents.coolingDesiredTemp && state != ZoneState.COOLING) {
            loopController.resetCoolingControl()
            state = ZoneState.COOLING
            resetControllers(factory, equip)
        } else if (currentTemp < userIntents.heatingDesiredTemp && state != ZoneState.HEATING) {
            loopController.resetHeatingControl()
            resetControllers(factory, equip)
            state = ZoneState.HEATING
        }
    }

    fun doAnalogCooling(
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        coolingLoopOutput: Int
    ) {
        if (conditioningMode.ordinal == StandaloneConditioningMode.COOL_ONLY.ordinal || conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal) {
            updateLogicalPoint(logicalPointsList[port]!!, coolingLoopOutput.toDouble())
            if (coolingLoopOutput > 0) analogOutStages[StatusMsgKeys.COOLING.name] =
                coolingLoopOutput
        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }

    fun doAnalogHeating(
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        heatingLoopOutput: Int
    ) {
        if (conditioningMode.ordinal == StandaloneConditioningMode.HEAT_ONLY.ordinal || conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal) {
            updateLogicalPoint(logicalPointsList[port]!!, heatingLoopOutput.toDouble())
            if (heatingLoopOutput > 0) analogOutStages[StatusMsgKeys.HEATING.name] =
                heatingLoopOutput
        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }

    fun doAnalogCompressorSpeed(
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        compressorLoopOutput: Int,
        zoneMode: ZoneState
    ) {
        if (conditioningMode != StandaloneConditioningMode.OFF) {
            updateLogicalPoint(logicalPointsList[port]!!, compressorLoopOutput.toDouble())
            if (compressorLoopOutput > 0) {
                if (zoneMode == ZoneState.COOLING) analogOutStages[StatusMsgKeys.COOLING.name] =
                    compressorLoopOutput
                if (zoneMode == ZoneState.HEATING) analogOutStages[StatusMsgKeys.HEATING.name] =
                    compressorLoopOutput
            }
        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }

    fun doAnalogFanAction(
        port: Port,
        fanLowPercent: Int,
        fanHighPercent: Int,
        fanMode: MyStatFanStages,
        conditioningMode: StandaloneConditioningMode,
        fanLoopOutput: Int,
        analogOutStages: HashMap<String, Int>,
        isFanGoodToRUn: Boolean = true
    ) {
        if (fanMode != MyStatFanStages.OFF) {
            var fanLoopForAnalog = 0
            if (isFanGoodToRUn && fanMode == MyStatFanStages.AUTO) {
                if (conditioningMode == StandaloneConditioningMode.OFF) {
                    updateLogicalPoint(logicalPointsList[port]!!, 0.0)
                    return
                }
                fanLoopForAnalog = fanLoopOutput
            } else {
                when {
                    (fanMode == MyStatFanStages.LOW_CUR_OCC || fanMode == MyStatFanStages.LOW_OCC || fanMode == MyStatFanStages.LOW_ALL_TIME) -> {
                        fanLoopForAnalog = fanLowPercent
                    }

                    (fanMode == MyStatFanStages.HIGH_CUR_OCC || fanMode == MyStatFanStages.HIGH_OCC || fanMode == MyStatFanStages.HIGH_ALL_TIME) -> {
                        fanLoopForAnalog = fanHighPercent
                    }
                }
            }
            if (fanLoopForAnalog > 0) analogOutStages[StatusMsgKeys.FAN_SPEED.name] =
                1 else analogOutStages.remove(StatusMsgKeys.FAN_SPEED.name)
            updateLogicalPoint(logicalPointsList[port]!!, fanLoopForAnalog.toDouble())
        }
    }

    fun doAnalogDCVAction(
        port: Port,
        analogOutStages: HashMap<String, Int>,
        cO2Threshold: Double,
        damperOpeningRate: Double,
        isDoorOpen: Boolean,
        equip: MyStatEquip
    ) {
        val co2Value = equip.zoneCo2.readHisVal()
        logIt(
            "doAnalogDCVAction: co2Value : $co2Value zoneCO2Threshold: $cO2Threshold zoneCO2DamperOpeningRate $damperOpeningRate"
        )

        updateLogicalPoint(logicalPointsList[port]!!, dcvLoopOutput.toDouble())
        if (dcvLoopOutput > 0) {
            analogOutStages[StatusMsgKeys.DCV_DAMPER.name] = dcvLoopOutput
        }
    }


    fun doorWindowIsOpen(doorWindowEnabled: Double, doorWindowSensor: Double, equip: MyStatEquip) {
        equip.doorWindowSensingEnable.writePointValue(doorWindowEnabled)
        equip.doorWindowSensorInput.writePointValue(doorWindowSensor)
    }

    fun keyCardIsInSlot(keycardEnabled: Double, keycardSensor: Double, equip: MyStatEquip) {
        equip.keyCardSensingEnable.writePointValue(keycardEnabled)
        equip.keyCardSensorInput.writePointValue(keycardSensor)
    }


    fun resetLogicalPoint(pointId: String?) {
        if (pointId != null) {
            updateLogicalPoint(pointId, 0.0)
        }
    }

    fun getCurrentLogicalPointStatus(pointId: String): Double = haystack.readHisValById(pointId)

    fun resetLogicalPoints() {
        logicalPointsList.forEach { (_, pointId) -> haystack.writeHisValById(pointId, 0.0) }
    }

    fun fallBackFanMode(
        equip: MyStatEquip, equipRef: String, fanModeSaved: Int, basicSettings: MyStatBasicSettings
    ): MyStatFanStages {
        logIt("FanModeSaved in Shared Preference $fanModeSaved")
        val currentOccupancy = equip.occupancyMode.readHisVal().toInt()
        // If occupancy is unoccupied and the fan mode is current occupied then remove the fan mode from cache
        if ((occupancyStatus == Occupancy.UNOCCUPIED || occupancyStatus == Occupancy.DEMAND_RESPONSE_UNOCCUPIED) && isFanModeCurrentOccupied(
                fanModeSaved
            )
        ) {
            FanModeCacheStorage.getMyStatFanModeCache().removeFanModeFromCache(equipRef)
        }

        logIt("Fall back fan mode " + basicSettings.fanMode + " conditioning mode " + basicSettings.conditioningMode
                +"\n Fan Details :$occupancyStatus  ${basicSettings.fanMode}  $fanModeSaved")
        if (isEligibleToAuto(basicSettings, equip)) {
            logIt("Resetting the Fan status back to  AUTO: ")
            updateUserIntentPoints(
                equipRef = equipRef,
                equip.fanOpMode,
                value = MyStatFanStages.AUTO.ordinal.toDouble(),
                CCUHsApi.getInstance().ccuUserName
            )
            return MyStatFanStages.AUTO
        }

        if (isSoftOccupied(equip.occupancyMode) && basicSettings.fanMode == MyStatFanStages.AUTO && fanModeSaved != 0) {
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

    private fun isFanModeCurrentOccupied(value: Int): Boolean {
        val basicSettings = MyStatFanStages.values()[value]
        return (basicSettings == MyStatFanStages.LOW_CUR_OCC || basicSettings == MyStatFanStages.HIGH_CUR_OCC)
    }

    private fun isEligibleToAuto(
        basicSettings: MyStatBasicSettings,
        equip: MyStatEquip
    ): Boolean {
        return (!isSoftOccupied(equip.occupancyMode) // should not be occupied
                && basicSettings.fanMode != MyStatFanStages.OFF
                && basicSettings.fanMode != MyStatFanStages.AUTO
                && basicSettings.fanMode != MyStatFanStages.LOW_ALL_TIME
                && basicSettings.fanMode != MyStatFanStages.HIGH_ALL_TIME
                )
    }

    fun getAverageTemp(userIntents: UserIntents) =
        if (haystack.isScheduleSlotExitsForRoom(equip!!.id)) {
            CcuLog.d(TAG, " Schedule slot not  exists for MyStat room ${equip!!.id} nodeAddress ${equip!!.group}")
            (CCUUtils.DEFAULT_HEATING_DESIRED + CCUUtils.DEFAULT_COOLING_DESIRED + haystack.getUnoccupiedSetback(equip!!.id)) / 2.0
        } else (userIntents.coolingDesiredTemp + userIntents.heatingDesiredTemp) / 2.0

    fun handleRFDead(equip: MyStatEquip) {
        state = ZoneState.RFDEAD
        equip.operatingMode.writeHisVal(state.ordinal.toDouble())
        equip.equipStatus.writeHisVal(state.ordinal.toDouble())
        equip.equipStatusMessage.writeDefaultVal(RFDead)
        myStatStatus[equip.equipRef] = RFDead
        logIt("RF Signal is Dead ${equip.nodeAddress}")
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
        this.resetLoopOutputs()
        resetLogicalPoints()
        MyStatUserIntentHandler.updateMyStatStatus(
            ZoneTempState.TEMP_DEAD,
            equip, L.TAG_CCU_MSHST
        )
    }

    fun updateOccupancyDetection(equip: MyStatEquip) {
        if (equip.zoneOccupancy.readHisVal() > 0) equip.occupancyDetection.writeHisValueByIdWithoutCOV(
            1.0
        )
    }


    fun checkFanOperationAllowedDoorWindow(userIntents: UserIntents): Boolean {
        return if (currentTemp < userIntents.coolingDesiredTemp && currentTemp > userIntents.heatingDesiredTemp) {
            doorWindowSensorOpenStatus &&
                    occupancyBeforeDoorWindow != Occupancy.UNOCCUPIED &&
                    occupancyBeforeDoorWindow != Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
                    occupancyBeforeDoorWindow != Occupancy.VACATION
        } else {
            doorWindowSensorOpenStatus
        }
    }

    open fun isDoorOpenState(config: MyStatConfiguration, equip: MyStatEquip): Boolean {
        var isDoorWindowMapped = 0.0
        var doorWindowSensorValue = 0.0

        if (config.universalIn1Enabled.enabled) {
            val universalMapping = MyStatConfiguration.UniversalMapping.values()
                .find { it.ordinal == config.universalIn1Association.associationVal }
            when (universalMapping) {
                MyStatConfiguration.UniversalMapping.DOOR_WINDOW_SENSOR_NC_TITLE24 -> {
                    isDoorWindowMapped = 1.0
                    doorWindowSensorValue = equip.doorWindowSensorNCTitle24.readHisVal()
                }

                MyStatConfiguration.UniversalMapping.DOOR_WINDOW_SENSOR_TITLE24 -> {
                    isDoorWindowMapped = 1.0
                    doorWindowSensorValue = equip.doorWindowSensorTitle24.readHisVal()
                }

                else -> {}
            }
        }
        doorWindowIsOpen(isDoorWindowMapped, doorWindowSensorValue, equip)
        return isDoorWindowMapped == 1.0 && doorWindowSensorValue > 0
    }


    enum class MyStatFanSpeed {
        OFF, LOW, HIGH
    }

    fun logIt(msg: String) {
        CcuLog.d(logTag, msg)
    }

    fun resetEquip(equip: MyStatEquip) {
        equip.relayStages.clear()
        equip.analogOutStages.clear()
    }
}

