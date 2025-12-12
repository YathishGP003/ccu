package a75f.io.logic.bo.building.statprofiles.mystat.profiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
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
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.statcontrollers.MyStatControlFactory
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.MyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.MyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.UserIntents
import a75f.io.logic.bo.building.statprofiles.util.canWeDoConditioning
import a75f.io.logic.bo.building.statprofiles.util.canWeRunFan
import a75f.io.logic.bo.building.statprofiles.util.fetchMyStatBasicSettings
import a75f.io.logic.bo.building.statprofiles.util.fetchMyStatTuners
import a75f.io.logic.bo.building.statprofiles.util.fetchUserIntents
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatHpuAnalogOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.getMyStatHpuRelayOutputPoints
import a75f.io.logic.bo.building.statprofiles.util.getMyStatLogicalPointList
import a75f.io.logic.bo.building.statprofiles.util.isMyStatHighUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.isMyStatLowUserIntentFanMode
import a75f.io.logic.bo.building.statprofiles.util.keyCardIsInSlot
import a75f.io.logic.bo.building.statprofiles.util.logMsResults
import a75f.io.logic.bo.building.statprofiles.util.runLowestFanSpeedDuringDoorOpen
import a75f.io.logic.bo.building.statprofiles.util.updateLogicalPoint
import a75f.io.logic.bo.building.statprofiles.util.updateLoopOutputs
import a75f.io.logic.bo.building.statprofiles.util.updateOperatingMode
import a75f.io.logic.controlcomponents.controls.Controller
import a75f.io.logic.controlcomponents.util.ControllerNames
import a75f.io.logic.util.uiutils.MyStatUserIntentHandler

/**
 * Created by Manjunath K on 16-01-2025.
 */

class MyStatHpuProfile : MyStatProfile(L.TAG_CCU_MSHPU) {

    private val hpuDeviceMap: MutableMap<Int, MyStatHpuEquip> = mutableMapOf()

    private lateinit var curState: ZoneState
    override lateinit var occupancyStatus: Occupancy

    private var analogLogicalPoints: HashMap<Int, String> = HashMap()
    private var relayLogicalPoints: HashMap<Int, String> = HashMap()


    override fun updateZonePoints() {
        hpuDeviceMap.forEach { (nodeAddress, equip) ->
            hpuDeviceMap[nodeAddress] = Domain.getDomainEquip(equip.equipRef) as MyStatHpuEquip
            logIt( "Process HPU: equipRef =  ${equip.nodeAddress}")
            processHpuProfile(equip)
        }
    }

    fun processHpuProfile(equip: MyStatHpuEquip) {

        if (Globals.getInstance().isTestMode) {
            logIt( "Test mode is on: ${equip.equipRef}")
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

        val config = getMyStatConfiguration(equip.equipRef) as MyStatHpuConfiguration

        logicalPointsList = getMyStatLogicalPointList(equip, config)
        relayLogicalPoints = getMyStatHpuRelayOutputPoints(equip)
        analogLogicalPoints = getMyStatHpuAnalogOutputPoints(equip)

        curState = ZoneState.DEADBAND

        if (equipOccupancyHandler != null) {
            occupancyStatus = equipOccupancyHandler.currentOccupiedMode
            zoneOccupancyState.data = occupancyStatus.ordinal.toDouble()
        }

        val myStatTuners = fetchMyStatTuners(equip) as MyStatTuners
        val userIntents = fetchUserIntents(equip)
        val averageDesiredTemp = getAverageTemp(userIntents)
        val fanModeSaved = FanModeCacheStorage.getMyStatFanModeCache().getFanModeFromCache(equip.equipRef)
        val basicSettings = fetchMyStatBasicSettings(equip)
        val controllerFactory = MyStatControlFactory(equip, controllers, stageCounts, derivedFanLoopOutput, zoneOccupancyState)

        logIt(
            "Before fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}"
        )
        val updatedFanMode = fallBackFanMode(equip, equip.equipRef, fanModeSaved, basicSettings)
        basicSettings.fanMode = updatedFanMode
        logIt(
            "After fall back ${basicSettings.fanMode} ${basicSettings.conditioningMode}"
        )

        loopController.initialise(tuners = myStatTuners)
        loopController.dumpLogs()
        handleChangeOfDirection(currentTemp, userIntents, controllerFactory, equip)
        updateOperatingMode(currentTemp, averageDesiredTemp, basicSettings.conditioningMode, equip.operatingMode)

        resetEquip(equip)
        evaluateLoopOutputs(userIntents, basicSettings, myStatTuners, config, equip)
        updateOccupancyDetection(equip)


        doorWindowSensorOpenStatus = runForDoorWindowSensor(equip)
        runFanLowDuringDoorWindow = checkFanOperationAllowedDoorWindow(userIntents)
        if (occupancyStatus == Occupancy.WINDOW_OPEN) resetLoopOutputs()
        keyCardIsInSlot(equip)

        updateLoopOutputs(
            coolingLoopOutput, equip.coolingLoopOutput,
            heatingLoopOutput, equip.heatingLoopOutput,
            fanLoopOutput, equip.fanLoopOutput,
            dcvLoopOutput, equip.dcvLoopOutput,
            true, compressorLoopOutput, equip.compressorLoopOutput
        )

        coolingLoopOutput = equip.coolingLoopOutput.readHisVal().toInt()
        heatingLoopOutput = equip.heatingLoopOutput.readHisVal().toInt()
        fanLoopOutput = equip.fanLoopOutput.readHisVal().toInt()
        dcvLoopOutput = equip.dcvLoopOutput.readHisVal().toInt()
        compressorLoopOutput = equip.compressorLoopOutput.readHisVal().toInt()

        if (canWeRunFan(basicSettings) && (doorWindowSensorOpenStatus.not())) {
            operateRelays(config,  basicSettings, equip, controllerFactory)
            operateAnalogOutputs(config, equip, basicSettings, equip.analogOutStages, relayLogicalPoints)
            if (basicSettings.fanMode == MyStatFanStages.AUTO) {
                runFanOperationBasedOnAuxStages(equip, config)
            }
        } else {
            resetLogicalPoints(equip)
            if (isDoorWindowDueTitle24) {
                runLowestFanSpeedDuringDoorOpen(equip, L.TAG_CCU_MSHPU)
            }
        }

        equip.equipStatus.writeHisVal(curState.ordinal.toDouble())
        logIt(
            "Fan speed multiplier:  ${myStatTuners.analogFanSpeedMultiplier} " +
                    "AuxHeating1Activate: ${myStatTuners.myStatAuxHeating1Activate} " +
                    "waterValveSamplingOnTime: ${myStatTuners.waterValveSamplingOnTime}" +
                    "  waterValveSamplingWaitTime : ${myStatTuners.waterValveSamplingWaitTime} \n" +
                    "waterValveSamplingDuringLoopDeadbandOnTime: ${myStatTuners.waterValveSamplingDuringLoopDeadbandOnTime} " +
                    " waterValveSamplingDuringLoopDeadbandWaitTime : ${myStatTuners.waterValveSamplingDuringLoopDeadbandWaitTime} \n"
                    + "Current Occupancy: ${Occupancy.values()[equip.occupancyMode.readHisVal().toInt()]} \n" + "Fan Mode : ${basicSettings.fanMode} " +
                    "Conditioning Mode ${basicSettings.conditioningMode} \n"
                    + "Current Temp : $currentTemp Desired (Heating: ${userIntents.heatingDesiredTemp}"
                    + " Cooling: ${userIntents.coolingDesiredTemp})\n"
                    + "Loop Outputs: (Heating Loop: $heatingLoopOutput Cooling Loop:" +
                    " $coolingLoopOutput Fan Loop: $fanLoopOutput  Compressor Loop : $compressorLoopOutput dcvLoopOutput : $dcvLoopOutput)" +
                    " \n"
        )
        logMsResults(config, L.TAG_CCU_MSHPU, logicalPointsList)
        var temperatureState = ZoneTempState.NONE
        if (buildingLimitMinBreached() || buildingLimitMaxBreached()) temperatureState = ZoneTempState.EMERGENCY
        MyStatUserIntentHandler.updateMyStatStatus(
            temperatureState,
            equip, L.TAG_CCU_MSHPU
        )
        if (occupancyStatus != Occupancy.WINDOW_OPEN) occupancyBeforeDoorWindow = occupancyStatus
        logIt(  "----------------------------------------------------------")
    }

    private fun evaluateLoopOutputs(
        userIntents: UserIntents,
        basicSettings: MyStatBasicSettings,
        hyperStatTuners: MyStatTuners,
        config: MyStatConfiguration,
        equip: MyStatEquip
    ) {
        resetLoopOutputs()

        when (state) {
            ZoneState.COOLING -> evaluateCoolingLoop(userIntents)
            ZoneState.HEATING -> evaluateHeatingLoop(userIntents)
            else -> logIt("Zone is in deadband")
        }
        evaluateFanOutput(basicSettings, hyperStatTuners)
        evaluateDcvLoop(equip, config)
    }

    private fun operateRelays(
        config: MyStatHpuConfiguration, basicSettings: MyStatBasicSettings,
        equip: MyStatHpuEquip, controllerFactory: MyStatControlFactory
    ) {
        controllerFactory.addHpuControllers(config, fanLowVentilationAvailable)
        runControllers(equip, basicSettings)
    }

    private fun runControllers(equip: MyStatHpuEquip, basicSettings: MyStatBasicSettings) {
        derivedFanLoopOutput.data = equip.fanLoopOutput.readHisVal()

        controllers.forEach { (controllerName, value) ->
            val controller = value as Controller
            val result = controller.runController()
            updateRelayStatus(controllerName, result, equip, basicSettings)
        }
    }

    private fun updateRelayStatus(
        controllerName: String,
        result: Any,
        equip: MyStatHpuEquip,
        basicSettings: MyStatBasicSettings
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
                            equip.compressorStage1,
                            isActive,
                            if (state == ZoneState.COOLING) Stage.COOLING_1.displayName else if (state == ZoneState.HEATING) Stage.HEATING_1.displayName else "",
                        )

                        1 -> updateStatus(
                            equip.compressorStage2,
                            isActive,
                            if (state == ZoneState.COOLING) Stage.COOLING_2.displayName else if (state == ZoneState.HEATING) Stage.HEATING_2.displayName else "",
                        )
                    }
                }
            }
            ControllerNames.FAN_SPEED_CONTROLLER -> {
                runTitle24Rule(equip)

                fun checkUserIntentAction(stage: Int): Boolean {
                    val mode = equip.fanOpMode
                    return when (stage) {
                        0 -> isMyStatHighUserIntentFanMode(mode) || isMyStatLowUserIntentFanMode(mode)
                        1 -> isMyStatHighUserIntentFanMode(mode)
                        else -> false
                    }
                }

                fun isStageActive(
                    stage: Int, currentState: Boolean, isLowestStageActive: Boolean
                ): Boolean {
                    val mode = equip.fanOpMode.readPriorityVal().toInt()
                    return if (mode == StandaloneFanStage.AUTO.ordinal) {
                        (basicSettings.conditioningMode != StandaloneConditioningMode.OFF) &&
                                (currentState || (isLowestStageActive && runFanLowDuringDoorWindow))
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
                            Stage.FAN_1.displayName,
                        )

                        1 -> updateStatus(
                            equip.fanHighSpeed,
                            isStageActive(stage, isActive, lowestStageFanHigh),
                            Stage.FAN_2.displayName,
                        )
                    }
                }
            }

            ControllerNames.AUX_HEATING_STAGE1 -> {
                var status = result as Boolean
                if (basicSettings.conditioningMode != StandaloneConditioningMode.AUTO
                    && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY) {
                    status = false
                }
                updateStatus(equip.auxHeatingStage1, status, StatusMsgKeys.AUX_HEATING_STAGE1.name)
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

            ControllerNames.FAN_ENABLED -> updateStatus(equip.fanEnable, result, StatusMsgKeys.FAN_ENABLED.name)
            ControllerNames.OCCUPIED_ENABLED -> updateStatus(equip.occupiedEnable, result)
            ControllerNames.HUMIDIFIER_CONTROLLER -> updateStatus(equip.humidifierEnable, result)
            ControllerNames.DEHUMIDIFIER_CONTROLLER -> updateStatus(equip.dehumidifierEnable, result)
            ControllerNames.DAMPER_RELAY_CONTROLLER -> updateStatus(equip.dcvDamper, result)
            else -> {
                logIt( "Unknown controller: $controllerName")
            }
        }
    }

    private fun isAuxAvailableAndActive(relayOutputPoints: HashMap<Int, String>): Boolean {
        return (relayOutputPoints.containsKey(MyStatHpuRelayMapping.AUX_HEATING_STAGE1.ordinal) && getCurrentLogicalPointStatus(
            relayOutputPoints[MyStatHpuRelayMapping.AUX_HEATING_STAGE1.ordinal]!!
        ) == 1.0)
    }

    private fun operateAnalogOutputs(
        config: MyStatHpuConfiguration,
        equip: MyStatHpuEquip,
        basicSettings: MyStatBasicSettings,
        analogOutStages: HashMap<String, Int>,
        relayOutputPoints: HashMap<Int, String>
    ) {
        config.apply {
            listOf(
                Triple(Pair(config.universalOut1, config.universalOut1Association), config.analogOut1FanSpeedConfig, Port.UNIVERSAL_OUT_ONE),
                Triple(Pair(config.universalOut2, config.universalOut2Association), config.analogOut2FanSpeedConfig, Port.UNIVERSAL_OUT_TWO)
            ).forEach { (universalOut, fanConfig, port) ->
                if (universalOut.first.enabled && isRelayConfig(universalOut.second.associationVal).not()) {
                    val analogMapping = MyStatHpuAnalogOutMapping.values().find { it.ordinal == universalOut.second.associationVal }
                    when (analogMapping) {
                        MyStatHpuAnalogOutMapping.COMPRESSOR_SPEED -> {
                            doAnalogCompressorSpeed(
                                port, basicSettings.conditioningMode,
                                analogOutStages, compressorLoopOutput,
                                getZoneMode()
                            )
                        }

                        MyStatHpuAnalogOutMapping.FAN_SPEED -> {
                            if (isAuxAvailableAndActive(relayOutputPoints) && basicSettings.fanMode.name == MyStatFanStages.AUTO.name ) return
                            doAnalogFanAction(
                                port,
                                fanConfig.low.currentVal.toInt(),
                                fanConfig.high.currentVal.toInt(),
                                basicSettings.fanMode, basicSettings.conditioningMode,
                                fanLoopOutput, analogOutStages
                            )
                        }

                        MyStatHpuAnalogOutMapping.DCV_DAMPER_MODULATION -> {
                            doAnalogDCVAction(
                                port, analogOutStages, config.zoneCO2Threshold.currentVal,
                                zoneCO2DamperOpeningRate.currentVal, equip
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun runFanOperationBasedOnAuxStages(
        equip: MyStatHpuEquip, config: MyStatHpuConfiguration,
    ) {
        val aux1AvailableAndActive = isAuxAvailableAndActive(relayLogicalPoints)
        if (aux1AvailableAndActive) {
            val isRelayFanActive = operateAuxBasedOnFan(equip)
            val isAnalogFanAvailable = runSpecificAnalogFanSpeed(config, equip.analogOutStages)
            if (isRelayFanActive.not() && isAnalogFanAvailable.not()) {
                logIt("Fan is not available so resetting aux")
                resetAux(equip.relayStages, relayLogicalPoints)
            }
        }
    }

    private fun runSpecificAnalogFanSpeed(
        config: MyStatHpuConfiguration,
        analogOutStages: HashMap<String, Int>
    ): Boolean {
        var isAnalogAvailable = false
        if (config.universalOut1.enabled && config.universalOut1Association.associationVal == MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal) {
            val fanSpeedValue = config.analogOut1FanSpeedConfig.high.currentVal
            updateLogicalPoint(logicalPointsList[Port.UNIVERSAL_OUT_ONE]!!, fanSpeedValue)
            analogOutStages[StatusMsgKeys.FAN_SPEED.name] = 1
            isAnalogAvailable = true
        }
        if (config.universalOut2.enabled && config.universalOut2Association.associationVal == MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal) {
            val fanSpeedValue = config.analogOut2FanSpeedConfig.high.currentVal
            updateLogicalPoint(logicalPointsList[Port.UNIVERSAL_OUT_TWO]!!, fanSpeedValue)
            analogOutStages[StatusMsgKeys.FAN_SPEED.name] = 1
            isAnalogAvailable = true
        }
        return isAnalogAvailable
    }

    private fun operateAuxBasedOnFan(equip: MyStatHpuEquip): Boolean {
        val sequenceMap = mutableMapOf(
            equip.fanHighSpeed to Stage.FAN_2.displayName,
            equip.fanLowSpeed to Stage.FAN_1.displayName,
            equip.fanEnable to StatusMsgKeys.FAN_ENABLED.name,
            equip.occupiedEnable to StatusMsgKeys.EQUIP_ON.name
        )
        // Before operating AUX based fan reset all points except fanEnable and occupiedEnable
        sequenceMap.forEach { (point, statusMsg) ->
            if ((point.domainName != DomainName.fanEnable && point.domainName != DomainName.occupiedEnable) && point.pointExists()) {
                point.writeHisVal(0.0)
            }
            equip.relayStages.remove(statusMsg)
        }
        sequenceMap.forEach { (point, statusMsg) ->
            if (point.pointExists()) {
                point.writeHisVal(1.0)
                equip.relayStages[statusMsg] = 1
                logIt("Operating AUX based fan: ${point.domainName}")
                return true
            }
        }
        return false
    }

    private fun resetAux(
        relayStages: HashMap<String, Int>,
        relayOutputPoints: HashMap<Int, String>
    ) {
        if (relayOutputPoints.containsKey(MyStatHpuRelayMapping.AUX_HEATING_STAGE1.ordinal)) {
            resetLogicalPoint(relayOutputPoints[MyStatHpuRelayMapping.AUX_HEATING_STAGE1.ordinal]!!)
        }
        relayStages.remove(MyStatHpuRelayMapping.AUX_HEATING_STAGE1.name)
    }

    private fun getZoneMode(): ZoneState {
        return when {
            (coolingLoopOutput > 0) -> ZoneState.COOLING
            (heatingLoopOutput > 0) -> ZoneState.HEATING
            else -> ZoneState.TEMPDEAD
        }
    }

    fun addEquip(equipRef: String) {
        val equip = MyStatHpuEquip(equipRef)
        hpuDeviceMap[equip.nodeAddress] = equip
    }

    override fun getEquip(): Equip? {
        for (nodeAddress in hpuDeviceMap.keys) {
            val equip = CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")
            return Equip.Builder().setHashMap(equip).build()
        }
        return null
    }

    fun getProfileDomainEquip(node: Int): MyStatHpuEquip = hpuDeviceMap[node]!!

    override fun getProfileType() = ProfileType.MYSTAT_HPU

    override fun getNodeAddresses(): Set<Short?> = hpuDeviceMap.keys.map { it.toShort() }.toSet()

    override fun getCurrentTemp(): Double {
        for (nodeAddress in hpuDeviceMap.keys) {
            return hpuDeviceMap[nodeAddress]!!.currentTemp.readHisVal()
        }
        return 0.0
    }

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        TODO("Not required")
    }
}