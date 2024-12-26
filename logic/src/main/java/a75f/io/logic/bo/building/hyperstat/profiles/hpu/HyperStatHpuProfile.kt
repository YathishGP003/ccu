package a75f.io.logic.bo.building.hyperstat.profiles.hpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.common.BasicSettings
import a75f.io.logic.bo.building.hyperstat.common.FanModeCacheStorage
import a75f.io.logic.bo.building.hyperstat.common.HyperStatLoopController
import a75f.io.logic.bo.building.hyperstat.common.HyperStatProfileTuners
import a75f.io.logic.bo.building.hyperstat.common.UserIntents
import a75f.io.logic.bo.building.hyperstat.profiles.HyperStatPackageUnitProfile
import a75f.io.logic.bo.building.hyperstat.profiles.util.fetchBasicSettings
import a75f.io.logic.bo.building.hyperstat.profiles.util.fetchHyperStatTuners
import a75f.io.logic.bo.building.hyperstat.profiles.util.fetchUserIntents
import a75f.io.logic.bo.building.hyperstat.profiles.util.getConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.util.getLogicalPointList
import a75f.io.logic.bo.building.hyperstat.profiles.util.updateAllLoopOutput
import a75f.io.logic.bo.building.hyperstat.profiles.util.updateOccupancyDetection
import a75f.io.logic.bo.building.hyperstat.profiles.util.updateOperatingMode
import a75f.io.logic.bo.building.hyperstat.v2.configs.AnalogInputAssociation
import a75f.io.logic.bo.building.hyperstat.v2.configs.FanConfig
import a75f.io.logic.bo.building.hyperstat.v2.configs.HpuAnalogOutConfigs
import a75f.io.logic.bo.building.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsHpuAnalogOutMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsHpuRelayMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.Th2InputAssociation
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.jobs.HyperStatUserIntentHandler

/**
 * Created by Manjunath K on 02-01-2023.
 */

class HyperStatHpuProfile : HyperStatPackageUnitProfile(){

    private var coolingLoopOutput = 0
    private var heatingLoopOutput = 0
    private var fanLoopOutput = 0
    private var doorWindowSensorOpenStatus = false
    private var runFanLowDuringDoorWindow = false


    private val hpuDeviceMap: MutableMap<Int, HpuV2Equip> = mutableMapOf()
    private var occupancyBeforeDoorWindow = Occupancy.UNOCCUPIED

    private var compressorLoopOutput = 0
    private val hyperStatHpuAlgorithm = HyperStatLoopController()
    override lateinit var occupancyStatus: Occupancy
    private lateinit var curState: ZoneState

    override fun getProfileType() = ProfileType.HYPERSTAT_HEAT_PUMP_UNIT

    override fun updateZonePoints() {
        hpuDeviceMap.forEach { (nodeAddress, equip) ->
            hpuDeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as HpuV2Equip
            CcuLog.i(L.TAG_CCU_HSHPU,"Process HPU Equip: node ${equip.nodeAddress} equipRef =  ${equip.equipRef}")
            processHyperStatHpuProfile(equip)
        }
    }

    fun addEquip(equipRef: String) {
        val equip = HpuV2Equip(equipRef)
        hpuDeviceMap[equip.nodeAddress] = equip
    }


    // Run the profile logic and algorithm for an equip.
    fun processHyperStatHpuProfile(equip: HpuV2Equip) {

        if (Globals.getInstance().isTestMode) {
            CcuLog.i(L.TAG_CCU_HSHPU, "Test mode is on: ${equip.nodeAddress}")
            return
        }

        if (mInterface != null) mInterface.refreshView()

        if (isRFDead) {
            handleRFDead(equip)
            return
        } else if (isZoneDead) {
            handleDeadZone(equip)
            return
        }

        val relayStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()

        val config = getConfiguration(equip.equipRef)

        logicalPointsList = getLogicalPointList(equip, config!!)

        val relayOutputPoints = getRelayStatus(equip)
        val analogOutputPoints = getAnalogOutputPoints(equip)

        curState = ZoneState.DEADBAND
        occupancyStatus = equipOccupancyHandler.currentOccupiedMode

        val hyperStatTuners = fetchHyperStatTuners(equip)
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val fanModeSaved = FanModeCacheStorage().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchBasicSettings(equip)

        CcuLog.i(L.TAG_CCU_HSHPU, "Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        CcuLog.i(L.TAG_CCU_HSHPU, "After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")

        hyperStatHpuAlgorithm.initialise(tuners = hyperStatTuners)
        hyperStatHpuAlgorithm.dumpLogs()
        handleChangeOfDirection(userIntents)

        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        compressorLoopOutput = 0

        val currentOperatingMode = equip.occupancyMode.readHisVal().toInt()
        evaluateLoopOutputs(userIntents, basicSettings, hyperStatTuners)
        updateOccupancyDetection(equip)

        doorWindowSensorOpenStatus = runForDoorWindowSensor(config, equip)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)

        if (occupancyStatus == Occupancy.WINDOW_OPEN) resetLoopOutputValues()
        runForKeyCardSensor(config, equip)
        updateAllLoopOutput(equip, coolingLoopOutput, heatingLoopOutput, fanLoopOutput, true, compressorLoopOutput)

        CcuLog.i(L.TAG_CCU_HSHPU,
                "Fan speed multiplier:  ${hyperStatTuners.analogFanSpeedMultiplier} " +
                        "AuxHeating1Activate: ${hyperStatTuners.auxHeating1Activate} " +
                        "AuxHeating2Activate: ${hyperStatTuners.auxHeating2Activate} " +
                        "Current Occupancy: ${Occupancy.values()[currentOperatingMode]} \n" +
                        "Current Temp : $currentTemp Desired (Heating: ${userIntents.zoneHeatingTargetTemperature}" +
                        " Cooling: ${userIntents.zoneCoolingTargetTemperature})\n" +
                        "Loop Outputs: (Heating: $heatingLoopOutput Cooling: $coolingLoopOutput Fan : $fanLoopOutput Compressor: $compressorLoopOutput ) \n"
        )

        if (basicSettings.fanMode != StandaloneFanStage.OFF) {
            operateRelays(config as HpuConfiguration, hyperStatTuners, userIntents, basicSettings, relayStages, relayOutputPoints, equip)
            operateAnalogOutputs(config, equip, basicSettings, analogOutStages, relayOutputPoints)
            if (basicSettings.fanMode == StandaloneFanStage.AUTO) {
                runFanOperationBasedOnAuxStages(relayStages, analogOutStages, config, relayOutputPoints, analogOutputPoints)
            }
        } else {
            resetLogicalPoints()
        }

        updateOperatingMode(currentTemp, averageDesiredTemp, basicSettings, equip)
        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())

        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY
        if (occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        HyperStatUserIntentHandler.updateHyperStatStatus(equip.equipRef, relayStages, analogOutStages, temperatureState, equip)
        CcuLog.i(L.TAG_CCU_HSHPU,"----------------------------------------------------------")
    }

    private fun runFanOperationBasedOnAuxStages(
            relayStages: HashMap<String, Int>, analogOutStages: HashMap<String, Int>,
            config: HpuConfiguration, relayOutputPoints: HashMap<Int, String>, analogOutputPoints: HashMap<Int, String>
    ) {
        val (aux1AvailableAndActive, aux2AvailableAndActive) = Pair(
                isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE1, relayOutputPoints),
                isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE2, relayOutputPoints))

        CcuLog.i(L.TAG_CCU_HSHPU, "Aux Based fan : aux1AvailableAndActive $aux1AvailableAndActive aux2AvailableAndActive $aux2AvailableAndActive")
        if (aux2AvailableAndActive) operateAuxBasedOnFan(HsHpuRelayMapping.AUX_HEATING_STAGE2, relayStages, relayOutputPoints)
        if (aux1AvailableAndActive) operateAuxBasedOnFan(HsHpuRelayMapping.AUX_HEATING_STAGE1, relayStages, relayOutputPoints)

        // Run the fan speed control if either aux1 or aux2 is available and active
        if ((aux1AvailableAndActive) || (aux2AvailableAndActive)) {
            runSpecificAnalogFanSpeed(config, analogOutStages, relayOutputPoints, analogOutputPoints)
        }
    }


    // New requirement for aux and fan operations If we do not have fan then no aux
    private fun operateAuxBasedOnFan(
            association: HsHpuRelayMapping,
            relayStages: HashMap<String, Int>, relayOutputPoints: HashMap<Int, String>
    ) {

        fun getFanStage(mapping: HsHpuRelayMapping): Stage? {
            return when (mapping) {
                HsHpuRelayMapping.FAN_LOW_SPEED -> Stage.FAN_1
                HsHpuRelayMapping.FAN_MEDIUM_SPEED -> Stage.FAN_2
                HsHpuRelayMapping.FAN_HIGH_SPEED -> Stage.FAN_3
                else -> null
            }
        }

        fun getAvailableFanSpeed(relayOutputPoints: HashMap<Int, String>) = Triple(
                relayOutputPoints.containsKey(HsHpuRelayMapping.FAN_LOW_SPEED.ordinal),
                relayOutputPoints.containsKey(HsHpuRelayMapping.FAN_MEDIUM_SPEED.ordinal),
                relayOutputPoints.containsKey(HsHpuRelayMapping.FAN_HIGH_SPEED.ordinal)
        )

        val (lowAvailable, mediumAvailable, highAvailable) = getAvailableFanSpeed(relayOutputPoints)

        fun deriveFanStage(): HsHpuRelayMapping {
            return when (association) {
                HsHpuRelayMapping.AUX_HEATING_STAGE1 -> {
                    when {
                        mediumAvailable -> HsHpuRelayMapping.FAN_MEDIUM_SPEED
                        highAvailable -> HsHpuRelayMapping.FAN_HIGH_SPEED
                        lowAvailable -> HsHpuRelayMapping.FAN_LOW_SPEED
                        else -> HsHpuRelayMapping.FAN_ENABLED
                    }
                }

                HsHpuRelayMapping.AUX_HEATING_STAGE2 -> {
                    when {
                        highAvailable -> HsHpuRelayMapping.FAN_HIGH_SPEED
                        mediumAvailable -> HsHpuRelayMapping.FAN_MEDIUM_SPEED
                        lowAvailable -> HsHpuRelayMapping.FAN_LOW_SPEED
                        else -> HsHpuRelayMapping.FAN_ENABLED
                    }
                }

                else -> HsHpuRelayMapping.FAN_ENABLED
            }
        }

        if (!lowAvailable && !mediumAvailable && !highAvailable) {
            resetAux(relayStages, relayOutputPoints) // non of the fans are available
        }

        val stage = deriveFanStage()
        val fanStatusMessage = getFanStage(stage)
        CcuLog.i(L.TAG_CCU_HSHPU, "operateAuxBasedOnFan: derived mode is $stage")
        // operate specific fan  (low, medium, high) based on derived stage order
        if (stage != HsHpuRelayMapping.FAN_ENABLED) {
            updateLogicalPoint(relayOutputPoints[stage.ordinal]!!, 1.0)
            relayStages[fanStatusMessage!!.displayName] = 1
        }

        when (stage) {
            HsHpuRelayMapping.FAN_MEDIUM_SPEED -> {
                relayOutputPoints[HsHpuRelayMapping.FAN_LOW_SPEED.ordinal]?.let { point ->
                    updateLogicalPoint(point, 1.0)
                    relayStages[Stage.FAN_2.displayName] = 1
                }

                relayOutputPoints[HsHpuRelayMapping.FAN_HIGH_SPEED.ordinal]?.let { highSpeedPoint ->
                    val auxStage2Inactive = relayOutputPoints[HsHpuRelayMapping.AUX_HEATING_STAGE2.ordinal]
                            ?.let { getCurrentLogicalPointStatus(it) == 0.0 } ?: true
                    if (auxStage2Inactive) {
                        updateLogicalPoint(highSpeedPoint, 0.0)
                        relayStages.remove(Stage.FAN_3.displayName)
                    }
                }
            }

            HsHpuRelayMapping.FAN_HIGH_SPEED -> {
                relayOutputPoints[HsHpuRelayMapping.FAN_MEDIUM_SPEED.ordinal]?.let { point ->
                    updateLogicalPoint(point, 1.0)
                    relayStages[Stage.FAN_2.displayName] = 1
                }

                relayOutputPoints[HsHpuRelayMapping.FAN_LOW_SPEED.ordinal]?.let { point ->
                    updateLogicalPoint(point, 1.0)
                    relayStages[Stage.FAN_1.displayName] = 1
                }
            }

            else -> {
                CcuLog.i(L.TAG_CCU_HSHPU, "operateAuxBasedOnFan: derived mode is invalid")
            }
        }
    }


    private fun runSpecificAnalogFanSpeed(
            config: HpuConfiguration, analogOutStages: HashMap<String, Int>,
            relayOutputPoints: HashMap<Int, String>, analogOutputPoints: HashMap<Int, String>
    ) {

        fun getPercent(fanConfig: FanConfig, fanSpeed: FanSpeed): Double {
            return when (fanSpeed) {
                FanSpeed.HIGH -> fanConfig.high.currentVal
                FanSpeed.MEDIUM -> fanConfig.medium.currentVal
                FanSpeed.LOW -> fanConfig.low.currentVal
                else -> 0.0
            }
        }

        var fanSpeed = FanSpeed.OFF

        if (isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE2, relayOutputPoints)) {
            fanSpeed = FanSpeed.HIGH
        } else if (isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE1, relayOutputPoints)) {
            fanSpeed = FanSpeed.MEDIUM
        }

        config.apply {
            listOf( (HpuAnalogOutConfigs(analogOut1Enabled.enabled, analogOut1Association.associationVal, analogOut1MinMaxConfig, analogOut1FanSpeedConfig)),
                    (HpuAnalogOutConfigs(analogOut2Enabled.enabled, analogOut2Association.associationVal, analogOut2MinMaxConfig, analogOut2FanSpeedConfig)),
                    (HpuAnalogOutConfigs(analogOut3Enabled.enabled, analogOut3Association.associationVal, analogOut3MinMaxConfig, analogOut3FanSpeedConfig)),
            ).forEach { analogOutConfig ->
                if (analogOutConfig.enabled && analogOutConfig.association == HsHpuAnalogOutMapping.FAN_SPEED.ordinal && fanSpeed != FanSpeed.OFF) {
                    val percentage = getPercent(analogOutConfig.fanSpeed, fanSpeed)
                    CcuLog.i(L.TAG_CCU_HSHPU,"Fan Speed : $percentage")
                    updateLogicalPoint(analogOutputPoints[HsHpuAnalogOutMapping.FAN_SPEED.ordinal]!!, percentage)
                    analogOutStages[AnalogOutput.FAN_SPEED.name] = 1
                }
            }
        }
    }

    private fun runForKeyCardSensor(config: HyperStatConfiguration, equip: HyperStatEquip) {
        val isKeyCardEnabled = (config.isEnabledAndAssociated(config.analogIn1Enabled, config.analogIn1Association, AnalogInputAssociation.KEY_CARD_SENSOR.ordinal)
                || config.isEnabledAndAssociated(config.analogIn1Enabled, config.analogIn2Association, AnalogInputAssociation.KEY_CARD_SENSOR.ordinal))
        keyCardIsInSlot((if (isKeyCardEnabled) 1.0 else 0.0), if (equip.keyCardSensor.readHisVal() > 0) 1.0 else 0.0, equip)
    }

    private fun evaluateLoopOutputs(userIntents: UserIntents, basicSettings: BasicSettings, hyperStatTuners: HyperStatProfileTuners) {
        when (state) {
            //Update coolingLoop when the zone is in cooling or it was in cooling and no change over happened yet.
            ZoneState.COOLING -> coolingLoopOutput = hyperStatHpuAlgorithm.calculateCoolingLoopOutput(
                    currentTemp, userIntents.zoneCoolingTargetTemperature
            ).toInt().coerceAtLeast(0)

            //Update heatingLoop when the zone is in heating or it was in heating and no change over happened yet.
            ZoneState.HEATING -> heatingLoopOutput = hyperStatHpuAlgorithm.calculateHeatingLoopOutput(
                    userIntents.zoneHeatingTargetTemperature, currentTemp
            ).toInt().coerceAtLeast(0)

            else -> CcuLog.i(L.TAG_CCU_HSHPU, " Zone is in deadband")
        }

        if (coolingLoopOutput > 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY
                        || basicSettings.conditioningMode == StandaloneConditioningMode.AUTO)) {
            fanLoopOutput = ((coolingLoopOutput * hyperStatTuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
            compressorLoopOutput = coolingLoopOutput
        } else if (heatingLoopOutput > 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY
                        || basicSettings.conditioningMode == StandaloneConditioningMode.AUTO)) {
            fanLoopOutput = ((heatingLoopOutput * hyperStatTuners.analogFanSpeedMultiplier).toInt()).coerceAtMost(100)
            compressorLoopOutput = heatingLoopOutput
        }
    }

    private fun handleChangeOfDirection(userIntents: UserIntents) {
        if (currentTemp > userIntents.zoneCoolingTargetTemperature && state != ZoneState.COOLING) {
            hyperStatHpuAlgorithm.resetCoolingControl()
            state = ZoneState.COOLING
            CcuLog.i(L.TAG_CCU_HSHPU, "Resetting cooling")
        } else if (currentTemp < userIntents.zoneHeatingTargetTemperature && state != ZoneState.HEATING) {
            hyperStatHpuAlgorithm.resetHeatingControl()
            state = ZoneState.HEATING
            CcuLog.i(L.TAG_CCU_HSHPU, "Resetting heating")
        }
    }

    private fun operateAnalogOutputs(
            config: HpuConfiguration, equip: HpuV2Equip,
            basicSettings: BasicSettings, analogOutStages: HashMap<String, Int>, relayOutputPoints: HashMap<Int, String>
    ) {
        config.apply {
            listOf(
                    Pair(HpuAnalogOutConfigs(analogOut1Enabled.enabled, analogOut1Association.associationVal, analogOut1MinMaxConfig, analogOut1FanSpeedConfig), Port.ANALOG_OUT_ONE),
                    Pair(HpuAnalogOutConfigs(analogOut2Enabled.enabled, analogOut2Association.associationVal, analogOut2MinMaxConfig, analogOut2FanSpeedConfig), Port.ANALOG_OUT_TWO),
                    Pair(HpuAnalogOutConfigs(analogOut3Enabled.enabled, analogOut3Association.associationVal, analogOut3MinMaxConfig, analogOut3FanSpeedConfig), Port.ANALOG_OUT_THREE),
            ).forEach { (analogOutConfig, port) ->
                if (analogOutConfig.enabled) {
                    handleAnalogOutState(analogOutConfig, equip, config, port, basicSettings, analogOutStages, relayOutputPoints)
                }
            }
        }
    }


    private fun handleAnalogOutState(
            analogOutState: HpuAnalogOutConfigs, equip: HpuV2Equip, config: HpuConfiguration,
            port: Port, basicSettings: BasicSettings, analogOutStages: HashMap<String, Int>, relayOutputPoints: HashMap<Int, String>
    ) {

        val analogMapping = HsHpuAnalogOutMapping.values().find { it.ordinal == analogOutState.association }
        when (analogMapping) {
            HsHpuAnalogOutMapping.COMPRESSOR_SPEED -> {
                doAnalogCompressorSpeed(port, basicSettings.conditioningMode, analogOutStages, compressorLoopOutput, getZoneMode())
            }

            HsHpuAnalogOutMapping.DCV_DAMPER -> {
                doAnalogDCVAction(port, analogOutStages, config.zoneCO2Threshold.currentVal, config.zoneCO2DamperOpeningRate.currentVal, isDoorOpenState(config, equip), equip)
            }

            HsHpuAnalogOutMapping.FAN_SPEED -> {
                if (isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE1, relayOutputPoints)) return
                if (isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE2, relayOutputPoints)) return
                doAnalogFanAction(
                        port, analogOutState.fanSpeed.low.currentVal.toInt(),
                        analogOutState.fanSpeed.medium.currentVal.toInt(),
                        analogOutState.fanSpeed.high.currentVal.toInt(),
                        basicSettings.fanMode, basicSettings.conditioningMode, fanLoopOutput, analogOutStages
                )
            }
            else -> {}
        }
        if (logicalPointsList.containsKey(port)) {
            CcuLog.i(L.TAG_CCU_HSHPU, "$port = $analogMapping : ${getCurrentLogicalPointStatus(logicalPointsList[port]!!)}")
        }
    }


    private fun operateRelays(
            config: HpuConfiguration, tuner: HyperStatProfileTuners, userIntents: UserIntents,
            basicSettings: BasicSettings, relayStages: HashMap<String, Int>, relayOutputPoints: HashMap<Int, String>, equip: HpuV2Equip
    ) {
        listOf(
                Triple(config.relay1Enabled.enabled, config.relay1Association.associationVal, Port.RELAY_ONE),
                Triple(config.relay2Enabled.enabled, config.relay2Association.associationVal, Port.RELAY_TWO),
                Triple(config.relay3Enabled.enabled, config.relay3Association.associationVal, Port.RELAY_THREE),
                Triple(config.relay4Enabled.enabled, config.relay4Association.associationVal, Port.RELAY_FOUR),
                Triple(config.relay5Enabled.enabled, config.relay5Association.associationVal, Port.RELAY_FIVE),
                Triple(config.relay6Enabled.enabled, config.relay6Association.associationVal, Port.RELAY_SIX)
        ).forEach { (enabled, association, port) ->
            if (enabled) handleRelayState(association, config, port, tuner, userIntents, basicSettings, relayStages, relayOutputPoints, equip)
        }
    }

    private fun handleRelayState(
            association: Int, config: HpuConfiguration, port: Port, tuner: HyperStatProfileTuners,
            userIntents: UserIntents, basicSettings: BasicSettings, relayStages: HashMap<String, Int>,
            relayOutputPoints: HashMap<Int, String>, equip: HpuV2Equip
    ) {

        val relayMapping = HsHpuRelayMapping.values().find { it.ordinal == association }
        when (relayMapping) {
            HsHpuRelayMapping.COMPRESSOR_STAGE1, HsHpuRelayMapping.COMPRESSOR_STAGE2, HsHpuRelayMapping.COMPRESSOR_STAGE3 -> {
                if (basicSettings.conditioningMode != StandaloneConditioningMode.OFF &&
                        compressorLoopOutput != 0) {
                    runRelayForCompressor(relayMapping, port, config, tuner, relayStages)
                } else {
                    resetPort(port)
                }
            }

            HsHpuRelayMapping.AUX_HEATING_STAGE1 -> {
                runAuxHeatingStages(port, relayMapping, userIntents, basicSettings, relayStages, tuner.auxHeating1Activate)
            }

            HsHpuRelayMapping.AUX_HEATING_STAGE2 -> {
                runAuxHeatingStages(port, relayMapping, userIntents, basicSettings, relayStages, tuner.auxHeating2Activate)
            }

            HsHpuRelayMapping.FAN_LOW_SPEED, HsHpuRelayMapping.FAN_MEDIUM_SPEED, HsHpuRelayMapping.FAN_HIGH_SPEED -> {
                runRelayForFanSpeed(relayMapping, port, config, tuner, relayStages, basicSettings, relayOutputPoints)
            }

            HsHpuRelayMapping.CHANGE_OVER_O_COOLING -> {
                if (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO || basicSettings.conditioningMode == StandaloneConditioningMode.COOL_ONLY) {
                    val status = if (coolingLoopOutput > 0) 1.0 else 0.0
                    updateLogicalPoint(logicalPointsList[port]!!, status)
                    if (status == 1.0) relayStages[HsHpuRelayMapping.CHANGE_OVER_O_COOLING.name] = 1
                }
            }

            HsHpuRelayMapping.CHANGE_OVER_B_HEATING -> {
                if (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO || basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY) {
                    val status = if (heatingLoopOutput > 0) 1.0 else 0.0
                    updateLogicalPoint(logicalPointsList[port]!!, status)
                    if (status == 1.0) relayStages[HsHpuRelayMapping.CHANGE_OVER_B_HEATING.name] = 1
                }
            }

            HsHpuRelayMapping.FAN_ENABLED -> doFanEnabled(curState, port, fanLoopOutput)
            HsHpuRelayMapping.OCCUPIED_ENABLED -> doOccupiedEnabled(port)
            HsHpuRelayMapping.HUMIDIFIER -> doHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMinInsideHumidity, equip.zoneHumidity.readHisVal())
            HsHpuRelayMapping.DEHUMIDIFIER -> doDeHumidifierOperation(port, tuner.humidityHysteresis, userIntents.targetMaxInsideHumidity, equip.zoneHumidity.readHisVal())
            else -> {}
        }
        if (logicalPointsList.containsKey(port)) {
            CcuLog.i(L.TAG_CCU_HSHPU, "$port = $relayMapping : ${getCurrentLogicalPointStatus(logicalPointsList[port]!!)}")
        }
    }


    private fun runRelayForCompressor(
            association: HsHpuRelayMapping, whichPort: Port, config: HpuConfiguration,
            tuner: HyperStatProfileTuners, relayStages: HashMap<String, Int>
    ) {
        when (association) {
            HsHpuRelayMapping.COMPRESSOR_STAGE1 -> {
                doCompressorStage1(
                        whichPort, compressorLoopOutput, tuner.relayActivationHysteresis, relayStages, getZoneMode())
            }

            HsHpuRelayMapping.COMPRESSOR_STAGE2 -> {
                val divider = if (config.getHighestCompressorStage() == HsHpuRelayMapping.COMPRESSOR_STAGE2) 50 else 33
                doCompressorStage2(
                        whichPort, compressorLoopOutput, tuner.relayActivationHysteresis, divider, relayStages, getZoneMode())
            }

            HsHpuRelayMapping.COMPRESSOR_STAGE3 -> {
                doCompressorStage3(
                        whichPort, compressorLoopOutput, tuner.relayActivationHysteresis, relayStages, getZoneMode())
            }

            else -> {}
        }

        if (getCurrentPortStatus(whichPort) == 1.0) curState = ZoneState.COOLING
    }

    private fun runAuxHeatingStages(
            port: Port, association: HsHpuRelayMapping, userIntents: UserIntents, basicSettings: BasicSettings,
            relayStages: HashMap<String, Int>, auxHeatingActivateTuner: Double
    ) {

        fun isEligibleForAuxHeatingStage(): Boolean {
            return heatingLoopOutput != 0 && (basicSettings.conditioningMode == StandaloneConditioningMode.AUTO
                    || basicSettings.conditioningMode == StandaloneConditioningMode.HEAT_ONLY)
        }

        if (isEligibleForAuxHeatingStage()) {
            if (currentTemp < (userIntents.zoneHeatingTargetTemperature - auxHeatingActivateTuner)) {
                updateLogicalPoint(logicalPointsList[port]!!, 1.0)
                relayStages[association.name] = 1
            } else if (currentTemp >= (userIntents.zoneHeatingTargetTemperature - (auxHeatingActivateTuner - 1))) {
                updateLogicalPoint(logicalPointsList[port]!!, 0.0)
                relayStages.remove(association.name)
            } else if (hayStack.readHisValById(logicalPointsList[port]!!) == 1.0) {
                relayStages[association.name] = 1
            }

        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
            relayStages.remove(association.name)
        }
    }

    private fun runRelayForFanSpeed(
            relayAssociation: HsHpuRelayMapping, whichPort: Port, config: HpuConfiguration,
            tuner: HyperStatProfileTuners, relayStages: HashMap<String, Int>, basicSettings: BasicSettings,
            relayOutputPoints: HashMap<Int, String>
    ) {
        if (basicSettings.fanMode == StandaloneFanStage.AUTO
                && basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
            CcuLog.i(L.TAG_CCU_HSHPU, "Cond is Off , Fan is Auto   ")
            resetPort(whichPort)
            return
        }

        val divider = if (config.getHighestFanStage() == HsHpuRelayMapping.FAN_MEDIUM_SPEED) 50 else 33
        val lowestStage = config.getLowestFanStage()

        // Check which fan speed is the lowest and set the status(Eg: If FAN_MEDIUM and FAN_HIGH are used, then FAN_MEDIUM is the lowest)
        resetFanLowestFanStatus()
        when (lowestStage) {
            HsHpuRelayMapping.FAN_LOW_SPEED -> setFanLowestFanLowStatus(true)
            HsHpuRelayMapping.FAN_MEDIUM_SPEED -> setFanLowestFanMediumStatus(true)
            HsHpuRelayMapping.FAN_HIGH_SPEED -> setFanLowestFanHighStatus(true)
            else -> {}
        }

        when (relayAssociation) {
            HsHpuRelayMapping.FAN_LOW_SPEED -> {
                doFanLowSpeed(
                        logicalPointsList[whichPort]!!, null, null, basicSettings.fanMode,
                        fanLoopOutput, tuner.relayActivationHysteresis, relayStages, divider, runFanLowDuringDoorWindow)
            }

            HsHpuRelayMapping.FAN_MEDIUM_SPEED -> {

                if (isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE1, relayOutputPoints) && basicSettings.fanMode == StandaloneFanStage.AUTO) return

                doFanMediumSpeed(
                        logicalPointsList[whichPort]!!, null, basicSettings.fanMode,
                        fanLoopOutput, tuner.relayActivationHysteresis, divider, relayStages, runFanLowDuringDoorWindow)
            }

            HsHpuRelayMapping.FAN_HIGH_SPEED -> {

                if (isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE1, relayOutputPoints) && basicSettings.fanMode == StandaloneFanStage.AUTO) return
                if (isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE2, relayOutputPoints) && basicSettings.fanMode == StandaloneFanStage.AUTO) return

                doFanHighSpeed(
                        logicalPointsList[whichPort]!!, basicSettings.fanMode,
                        fanLoopOutput, tuner.relayActivationHysteresis, relayStages, runFanLowDuringDoorWindow)
            }

            else -> {}
        }
    }

    /**
     * Check if we should allow fan operation even when the door window is open
     * @return true if the door or window is open and the occupancy status is not UNOCCUPIED, otherwise false.
     */
    private fun checkFanOperationAllowedDoorWindow(userIntents: UserIntents): Boolean {
        return if (currentTemp < userIntents.zoneCoolingTargetTemperature && currentTemp > userIntents.zoneHeatingTargetTemperature) {
            doorWindowSensorOpenStatus &&
                    occupancyBeforeDoorWindow != Occupancy.UNOCCUPIED &&
                    occupancyBeforeDoorWindow != Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
                    occupancyBeforeDoorWindow != Occupancy.VACATION
        } else {
            doorWindowSensorOpenStatus
        }
    }

    private fun runForDoorWindowSensor(config: HyperStatConfiguration, equip: HyperStatEquip): Boolean {
        val isDoorOpen = isDoorOpenState(config, equip)
        CcuLog.i(L.TAG_CCU_HSHST, " is Door Open ? $isDoorOpen")
        return isDoorOpen
    }

    private fun resetLoopOutputValues() {
        CcuLog.i(L.TAG_CCU_HSHPU, "Resetting all the loop output values: ")
        coolingLoopOutput = 0
        heatingLoopOutput = 0
        fanLoopOutput = 0
        compressorLoopOutput = 0
    }

    private fun resetAux(relayStages: HashMap<String, Int>, relayOutputPoints: HashMap<Int, String>) {
        if (relayOutputPoints.containsKey(HsHpuRelayMapping.AUX_HEATING_STAGE1.ordinal)) {
            resetLogicalPoint(relayOutputPoints[HsHpuRelayMapping.AUX_HEATING_STAGE1.ordinal]!!)
        }
        if (relayOutputPoints.containsKey(HsHpuRelayMapping.AUX_HEATING_STAGE2.ordinal)) {
            resetLogicalPoint(relayOutputPoints[HsHpuRelayMapping.AUX_HEATING_STAGE2.ordinal]!!)
        }
        relayStages.remove(HsHpuRelayMapping.AUX_HEATING_STAGE1.name)
        relayStages.remove(HsHpuRelayMapping.AUX_HEATING_STAGE2.name)
    }

    private fun isAuxAvailableAndActive(mapping: HsHpuRelayMapping, relayOutputPoints: HashMap<Int, String>): Boolean {
        return (relayOutputPoints.containsKey(mapping.ordinal) && getCurrentLogicalPointStatus(relayOutputPoints[mapping.ordinal]!!) == 1.0)
    }

    private fun getZoneMode(): ZoneState {
        return when {
            (coolingLoopOutput > 0) -> ZoneState.COOLING
            (heatingLoopOutput > 0) -> ZoneState.HEATING
            else -> ZoneState.TEMPDEAD
        }
    }

    private fun isDoorOpenState(config: HyperStatConfiguration, equip: HyperStatEquip): Boolean {

        fun isAnalogHasDoorWindowMapping(): Boolean {
            return (config.isEnabledAndAssociated(config.analogOut1Enabled, config.analogIn1Association, AnalogInputAssociation.DOOR_WINDOW_SENSOR_TITLE_24.ordinal)
                    || config.isEnabledAndAssociated(config.analogOut2Enabled, config.analogIn2Association, AnalogInputAssociation.DOOR_WINDOW_SENSOR_TITLE_24.ordinal))
        }

        // If thermistor value less than 10000 ohms door is closed (0) else door is open (1)
        // If analog in value is less than 2v door is closed(0) else door is open (1)
        var isDoorOpen = false
        var th2SensorEnabled = false
        var analogSensorEnabled = false

        if (config.isEnabledAndAssociated(config.thermistor2Enabled, config.thermistor2Association,
                        Th2InputAssociation.DOOR_WINDOW_SENSOR_NC_TITLE_24.ordinal)) {
            val sensorValue = equip.doorWindowSensorNCTitle24.readHisVal().toInt()
            if (sensorValue == 1) isDoorOpen = true
            th2SensorEnabled = true
            CcuLog.i(L.TAG_CCU_HSHPU, "TH1 Door Window sensor value : Door is $sensorValue")
        }

        if (isAnalogHasDoorWindowMapping()) {
            val sensorValue = equip.doorWindowSensorTitle24.readHisVal().toInt()
            if (sensorValue == 1) isDoorOpen = true
            analogSensorEnabled = true
            CcuLog.i(L.TAG_CCU_HSHPU, "Analog Input has mapping Door Window sensor value : Door is $sensorValue")

        }
        doorWindowIsOpen(
                if (th2SensorEnabled || analogSensorEnabled) 1.0 else 0.0,
                if (isDoorOpen) 1.0 else 0.0, equip
        )
        return isDoorOpen
    }

    override fun getDisplayCurrentTemp() = averageZoneTemp

    override fun getAverageZoneTemp(): Double {
        var tempTotal = 0.0
        var nodeCount = 0
        hpuDeviceMap.forEach { (_, cpuDevice) ->
            if (cpuDevice.currentTemp.readHisVal() > 0) {
                tempTotal += cpuDevice.currentTemp.readHisVal()
                nodeCount++
            }
        }
        return if (nodeCount == 0) 0.0 else tempTotal / nodeCount
    }

    override fun getEquip(): Equip? {
        for (nodeAddress in hpuDeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    override fun getNodeAddresses(): Set<Short?> = hpuDeviceMap.keys.map { it.toShort() }.toSet()

    fun getProfileDomainEquip(node: Int): HpuV2Equip = hpuDeviceMap[node]!!

    override fun getCurrentTemp(): Double {
        for (nodeAddress in hpuDeviceMap.keys) {
            return hpuDeviceMap[nodeAddress]!!.currentTemp.readHisVal()
        }
        return 0.0
    }


    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        TODO("Not using now")
    }

}