package a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.hpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Point
import a75f.io.domain.equips.hyperstat.HsHpuEquip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.HyperStatProfile
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HpuAnalogOutConfigs
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsHpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsHpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.statprofiles.statcontrollers.HyperStatControlFactory
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.HyperStatProfileTuners
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.canWeDoConditioning
import a75f.io.logic.bo.building.statprofiles.util.canWeRunFan
import a75f.io.logic.bo.building.statprofiles.util.fetchBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.fetchHyperStatTuners
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getHSLogicalPointList
import a75f.io.logic.bo.building.statprofiles.util.getHSRelayStatus
import a75f.io.logic.bo.building.statprofiles.util.getHsConfiguration
import a75f.io.logic.bo.building.statprofiles.util.isHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMediumUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.keyCardIsInSlot
import a75f.io.logic.bo.building.statprofiles.util.logResults
import a75f.io.logic.bo.building.statprofiles.util.runLowestFanSpeedDuringDoorOpen
import a75f.io.logic.bo.building.statprofiles.util.updateLoopOutputs
import a75f.io.logic.bo.building.statprofiles.util.updateOccupancyDetection
import a75f.io.logic.bo.building.statprofiles.util.updateOperatingMode
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.util.uiutils.HyperStatUserIntentHandler

/**
 * Created by Manjunath K on 02-01-2023.
 */

class HyperStatHpuProfile : HyperStatProfile(L.TAG_CCU_HSHPU) {

    private val hpuDeviceMap: MutableMap<Int, HsHpuEquip> = mutableMapOf()

    override fun getProfileType() = ProfileType.HYPERSTAT_HEAT_PUMP_UNIT

    override fun updateZonePoints() {
        hpuDeviceMap.forEach { (nodeAddress, equip) ->
            hpuDeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as HsHpuEquip
            logIt("Process HPU Equip: node ${equip.nodeAddress} equipRef =  ${equip.equipRef}")
            processHyperStatHpuProfile(equip)
        }
    }

    fun addEquip(equipRef: String) {
        val equip = HsHpuEquip(equipRef)
        hpuDeviceMap[equip.nodeAddress] = equip
    }

    // Run the profile logic and algorithm for an equip.
    fun processHyperStatHpuProfile(equip: HsHpuEquip) {

        if (Globals.getInstance().isTestMode) {
            logIt("Test mode is on: ${equip.nodeAddress}")
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

        val config = getHsConfiguration(equip.equipRef)
        val relayOutputPoints = getHSRelayStatus(equip)
        val hyperStatTuners = fetchHyperStatTuners(equip) as HyperStatProfileTuners
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val fanModeSaved = FanModeCacheStorage.getHyperStatFanModeCache().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchBasicSettings(equip)
        val controllerFactory = HyperStatControlFactory(equip, controllers, stageCounts, derivedFanLoopOutput, zoneOccupancyState)

        logicalPointsList = getHSLogicalPointList(equip, config!!)
        curState = ZoneState.DEADBAND

        if (equipOccupancyHandler != null) {
            occupancyStatus = equipOccupancyHandler.currentOccupiedMode
            zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        }

        logIt("Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        logIt("After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}")

        loopController.initialise(tuners = hyperStatTuners)
        loopController.dumpLogs()
        handleChangeOfDirection(currentTemp, userIntents, controllerFactory, equip)

        resetEquip(equip)
        evaluateLoopOutputs(userIntents, basicSettings, hyperStatTuners, config, equip)
        updateOccupancyDetection(equip)

        doorWindowSensorOpenStatus = runForDoorWindowSensor(equip)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)

        keyCardIsInSlot(equip)
        updateLoopOutputs(
            coolingLoopOutput, equip.coolingLoopOutput,
            heatingLoopOutput, equip.heatingLoopOutput,
            fanLoopOutput, equip.fanLoopOutput,
            dcvLoopOutput, equip.dcvLoopOutput,
            isHpuProfile = true,
            compressorLoopOutput, equip.compressorLoopOutput
        )

        coolingLoopOutput = equip.coolingLoopOutput.readHisVal().toInt()
        heatingLoopOutput = equip.heatingLoopOutput.readHisVal().toInt()
        fanLoopOutput = equip.fanLoopOutput.readHisVal().toInt()
        dcvLoopOutput = equip.dcvLoopOutput.readHisVal().toInt()
        compressorLoopOutput = equip.compressorLoopOutput.readHisVal().toInt()

        if (canWeRunFan(basicSettings) && (doorWindowSensorOpenStatus.not())) {
            operateRelays(config as HpuConfiguration, basicSettings, equip, controllerFactory)
            operateAnalogOutputs(config, equip, basicSettings, relayOutputPoints)
            val analogFanType = operateAuxBasedFan(equip, basicSettings)
            runSpecifiedAnalogFanSpeed(equip, analogFanType, config.getAnalogOutsConfigurationMapping(),config.getFanConfiguration())
        } else {
            resetLogicalPoints(equip)
            if (isDoorOpenFromTitle24 && canWeRunFan(basicSettings)) {
                runLowestFanSpeedDuringDoorOpen(equip, L.TAG_CCU_HSHPU)
            }
        }

        updateOperatingMode(
            currentTemp,
            averageDesiredTemp,
            basicSettings.conditioningMode,
            equip.operatingMode
        )
        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())

        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState =
            ZoneTempState.EMERGENCY
        if (occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        printStatus(hyperStatTuners, basicSettings,userIntents,equip,config)
        HyperStatUserIntentHandler.updateHyperStatStatus(temperatureState, equip, L.TAG_CCU_HSHPU)
        logIt("----------------------------------------------------------")
    }


    private fun operateAnalogOutputs(
        config: HpuConfiguration, equip: HsHpuEquip,
        basicSettings: BasicSettings, relayOutputPoints: HashMap<Int, String>
    ) {
        config.apply {
            listOf(
                    Pair(HpuAnalogOutConfigs(analogOut1Enabled.enabled, analogOut1Association.associationVal, analogOut1MinMaxConfig, analogOut1FanSpeedConfig), Port.ANALOG_OUT_ONE),
                    Pair(HpuAnalogOutConfigs(analogOut2Enabled.enabled, analogOut2Association.associationVal, analogOut2MinMaxConfig, analogOut2FanSpeedConfig), Port.ANALOG_OUT_TWO),
                    Pair(HpuAnalogOutConfigs(analogOut3Enabled.enabled, analogOut3Association.associationVal, analogOut3MinMaxConfig, analogOut3FanSpeedConfig), Port.ANALOG_OUT_THREE),
            ).forEach { (analogOutConfig, port) ->
                if (analogOutConfig.enabled) {
                    handleAnalogOutState(
                        analogOutConfig, equip, config, port, basicSettings, relayOutputPoints
                    )
                }
            }
        }
    }


    private fun handleAnalogOutState(
        analogOutState: HpuAnalogOutConfigs,
        equip: HsHpuEquip,
        config: HpuConfiguration,
        port: Port,
        basicSettings: BasicSettings,
        relayOutputPoints: HashMap<Int, String>
    ) {

        val analogMapping =
            HsHpuAnalogOutMapping.values().find { it.ordinal == analogOutState.association }
        when (analogMapping) {
            HsHpuAnalogOutMapping.COMPRESSOR_SPEED -> {
                doAnalogCompressorSpeed(
                    port,
                    basicSettings.conditioningMode,
                    equip.analogOutStages,
                    compressorLoopOutput,
                    getZoneMode()
                )
            }

            HsHpuAnalogOutMapping.DCV_DAMPER -> {
                doAnalogDCVAction(
                    port,
                    equip.analogOutStages,
                    config.zoneCO2Threshold.currentVal,
                    config.zoneCO2DamperOpeningRate.currentVal,
                    equip
                )
            }

            HsHpuAnalogOutMapping.FAN_SPEED -> {
                if ((isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE1, relayOutputPoints) ||
                            isAuxAvailableAndActive(HsHpuRelayMapping.AUX_HEATING_STAGE2, relayOutputPoints))
                    && (basicSettings.fanMode.name == StandaloneFanStage.AUTO.name)
                ) return

                doAnalogFanAction(
                    port,
                    analogOutState.fanSpeed.low.currentVal.toInt(),
                    analogOutState.fanSpeed.medium.currentVal.toInt(),
                    analogOutState.fanSpeed.high.currentVal.toInt(),
                    equip, basicSettings, fanLoopOutput,
                )
            }

            else -> {}
        }
    }

    private fun doAnalogCompressorSpeed(
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


    private fun operateRelays(
        config: HpuConfiguration, basicSettings: BasicSettings, equip: HsHpuEquip,
        controllerFactory: HyperStatControlFactory
    ) {
        controllerFactory.addHpuControllers(config, fanLowVentilationAvailable)
        runControllers(equip, basicSettings)
    }

    private fun runControllers(equip: HsHpuEquip, basicSettings: BasicSettings) {
        derivedFanLoopOutput.data = equip.fanLoopOutput.readHisVal()
        controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            val result = controller.runController()
            updateRelayStatus(controllerName, result, equip, basicSettings)
        }
    }

    private fun updateRelayStatus(
        controllerName: String, result: Any, equip: HsHpuEquip, basicSettings: BasicSettings
    ) {

        fun updateStatus(point: Point, result: Any, status: String? = null) {
            if (point.pointExists()) {
                point.writeHisVal(if (result as Boolean) 1.0 else 0.0)
                if (status != null && result) {
                    equip.relayStages[status] = 1
                } else {
                    equip.relayStages.remove(status)
                }
            } else {
                equip.relayStages.remove(status)
            }
        }

        when (controllerName) {
            ControllerNames.COMPRESSOR_RELAY_CONTROLLER -> {
                val compressorStages = result as List<Pair<Int, Boolean>>
                compressorStages.forEach {
                    val (stage, isActive) = Pair(
                        it.first,
                        if (basicSettings.conditioningMode != StandaloneConditioningMode.OFF) it.second else false
                    )
                    when (stage) {
                        0 -> updateStatus(
                            equip.compressorStage1, isActive,
                            if (state == ZoneState.COOLING) Stage.COOLING_1.displayName else if (state == ZoneState.HEATING) Stage.HEATING_1.displayName else "",
                        )

                        1 -> updateStatus(
                            equip.compressorStage2, isActive,
                            if (state == ZoneState.COOLING) Stage.COOLING_2.displayName else if (state == ZoneState.HEATING) Stage.HEATING_2.displayName else "",
                        )

                        2 -> updateStatus(
                            equip.compressorStage3, isActive,
                            if (state == ZoneState.COOLING) Stage.COOLING_3.displayName else if (state == ZoneState.HEATING) Stage.HEATING_3.displayName else "",
                        )
                    }
                }
            }

            ControllerNames.FAN_SPEED_CONTROLLER -> {

                runTitle24Rule(equip)

                fun checkUserIntentAction(stage: Int): Boolean {
                    val mode = equip.fanOpMode
                    return when (stage) {
                        0 -> isHighUserIntentFanMode(mode) || isMediumUserIntentFanMode(mode) || isLowUserIntentFanMode(mode)
                        1 -> isHighUserIntentFanMode(mode) || isMediumUserIntentFanMode(mode)
                        2 -> isHighUserIntentFanMode(mode)
                        else -> false
                    }
                }

                fun isStageActive(
                    stage: Int, currentState: Boolean, isLowestStageActive: Boolean
                ): Boolean {
                    val mode = equip.fanOpMode.readPriorityVal().toInt()
                    return if (mode == StandaloneFanStage.AUTO.ordinal) {
                        (canWeDoConditioning(basicSettings) && (currentState || (isLowestStageActive && runFanLowDuringDoorWindow)))
                    } else {
                        checkUserIntentAction(stage)
                    }
                }

                val fanStages = result as List<Pair<Int, Boolean>>
                fanStages.forEach {
                    val (stage, isActive) = Pair(it.first, it.second)
                    when (stage) {
                        0 -> updateStatus(
                            equip.fanLowSpeed,
                            isStageActive(stage, isActive, lowestStageFanLow),
                            Stage.FAN_1.displayName
                        )

                        1 -> updateStatus(
                            equip.fanMediumSpeed,
                            isStageActive(stage, isActive, lowestStageFanMedium),
                            Stage.FAN_2.displayName
                        )

                        2 -> updateStatus(
                            equip.fanHighSpeed,
                            isStageActive(stage, isActive, lowestStageFanHigh),
                            Stage.FAN_3.displayName
                        )
                    }
                }
            }

            ControllerNames.FAN_ENABLED -> updateStatus(equip.fanEnable, result, StatusMsgKeys.FAN_ENABLED.name)
            ControllerNames.OCCUPIED_ENABLED -> updateStatus(equip.occupiedEnable, result)
            ControllerNames.HUMIDIFIER_CONTROLLER -> updateStatus(equip.humidifierEnable, result)
            ControllerNames.DEHUMIDIFIER_CONTROLLER -> updateStatus(equip.dehumidifierEnable, result)
            ControllerNames.DAMPER_RELAY_CONTROLLER -> updateStatus(equip.dcvDamper, result, StatusMsgKeys.DCV_DAMPER.name)
            ControllerNames.AUX_HEATING_STAGE1 -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode != StandaloneConditioningMode.AUTO
                    && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
                    status = false
                }
                isAuxStage1Active = status
                updateStatus(equip.auxHeatingStage1, status, StatusMsgKeys.AUX_HEATING_STAGE1.name)
            }

            ControllerNames.AUX_HEATING_STAGE2 -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode != StandaloneConditioningMode.AUTO
                    && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
                    status = false
                }
                isAuxStage2Active = status
                updateStatus(equip.auxHeatingStage2, status, StatusMsgKeys.AUX_HEATING_STAGE2.name)
            }

            ControllerNames.CHANGE_OVER_O_COOLING -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
                    status = false
                }
                updateStatus(equip.changeOverCooling, status)
            }

            ControllerNames.CHANGE_OVER_B_HEATING -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode == StandaloneConditioningMode.OFF) {
                    status = false
                }
                updateStatus(equip.changeOverHeating, status)
            }

            else -> {
                logIt("Unknown controller: $controllerName")
            }
        }
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

    override fun getDisplayCurrentTemp() = averageZoneTemp

    override fun getAverageZoneTemp(): Double {
        var tempTotal = 0.0
        var nodeCount = 0
        hpuDeviceMap.forEach { (_, device) ->
            if (device.currentTemp.readHisVal() > 0) {
                tempTotal += device.currentTemp.readHisVal()
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

    fun getProfileDomainEquip(node: Int): HsHpuEquip = hpuDeviceMap[node]!!

    override fun getCurrentTemp(): Double {
        for (nodeAddress in hpuDeviceMap.keys) {
            return hpuDeviceMap[nodeAddress]!!.currentTemp.readHisVal()
        }
        return 0.0
    }

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        TODO("Not using now")
    }

    private fun printStatus(
        tuners: HyperStatProfileTuners,
        settings: BasicSettings,
        userIntents: UserIntents,
        equip: HyperStatEquip,
        config: HyperStatConfiguration
    ) {
        logIt(
            "Fan speed multiplier:  ${tuners.analogFanSpeedMultiplier} " +
                    "AuxHeating1Activate: ${tuners.auxHeating1Activate} " +
                    "AuxHeating2Activate: ${tuners.auxHeating2Activate} \n" +
                    "Current Occupancy: ${Occupancy.values()[equip.occupancyMode.readHisVal().toInt()]} \n" +
                    "Current Temp : $currentTemp Desired (Heating: ${userIntents.heatingDesiredTemp} Cooling: ${userIntents.coolingDesiredTemp})\n" +
                    "Fan Mode: ${settings.fanMode} Conditioning Mode: ${settings.conditioningMode}\n"+
                    "Loop Outputs: (Heating: $heatingLoopOutput Cooling: $coolingLoopOutput " +
                    "Fan : $fanLoopOutput Compressor: $compressorLoopOutput DCV $dcvLoopOutput) \n"
        )
        logResults(config, L.TAG_CCU_HSHPU, logicalPointsList)
    }

}