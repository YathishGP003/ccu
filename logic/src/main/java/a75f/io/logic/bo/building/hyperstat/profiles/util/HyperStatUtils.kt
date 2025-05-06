package a75f.io.logic.bo.building.hyperstat.profiles.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.devices.HyperStatDevice
import a75f.io.domain.equips.hyperstat.CpuV2Equip
import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.hyperstat.MonitoringEquip
import a75f.io.domain.equips.hyperstat.Pipe2V2Equip
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.common.BasicSettings
import a75f.io.logic.bo.building.hyperstat.common.HyperStatProfileTuners
import a75f.io.logic.bo.building.hyperstat.common.PossibleConditioningMode
import a75f.io.logic.bo.building.hyperstat.common.PossibleFanMode
import a75f.io.logic.bo.building.hyperstat.common.UserIntents
import a75f.io.logic.bo.building.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsCpuAnalogOutMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsCpuRelayMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsHpuAnalogOutMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsHpuRelayMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsPipe2AnalogOutMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsPipe2RelayMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.MonitoringConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.Pipe2Configuration
import a75f.io.logic.tuners.TunerUtil
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlin.math.roundToInt

/**
 * Created by Manjunath K on 25-10-2024.
 */

const val OFF = 0
const val AUTO = 1
const val LOW = 6
const val MED = 7
const val HIGH = 8
const val LOW_MED = 13
const val LOW_HIGH = 14
const val MED_HIGH = 15
const val LOW_MED_HIGH = 21

fun getPercentFromVolt(voltage: Int, min: Int = 0, max: Int = 10) = (((voltage - min).toDouble() / (max - min)) * 100).roundToInt()

fun fetchUserIntents(equip: HyperStatEquip): UserIntents {
    return UserIntents(currentTemp = equip.currentTemp.readHisVal(), zoneCoolingTargetTemperature = equip.desiredTempCooling.readPriorityVal(), zoneHeatingTargetTemperature = equip.desiredTempHeating.readPriorityVal(), targetMinInsideHumidity = equip.targetHumidifier.readPriorityVal(), targetMaxInsideHumidity = equip.targetDehumidifier.readPriorityVal())
}

fun fetchHyperStatTuners(equip: HyperStatEquip): HyperStatProfileTuners {

    /**
     * Consider that
     * proportionalGain = proportionalKFactor
     * integralGain = integralKFactor
     * proportionalSpread = temperatureProportionalRange
     * integralMaxTimeout = temperatureIntegralTime
     */

    // These are generic tuners
    val hsTuners = HyperStatProfileTuners()
    hsTuners.proportionalGain = TunerUtil.getProportionalGain(equip.equipRef)
    hsTuners.integralGain = TunerUtil.getIntegralGain(equip.equipRef)
    hsTuners.proportionalSpread = TunerUtil.getProportionalSpread(equip.equipRef)
    hsTuners.integralMaxTimeout = TunerUtil.getIntegralTimeout(equip.equipRef).toInt()
    hsTuners.humidityHysteresis = TunerUtil.getHysteresisPoint("domainName ==\"${DomainName.standaloneHumidityHysteresis}\"", equip.equipRef).toInt()
    hsTuners.relayActivationHysteresis = TunerUtil.getHysteresisPoint("domainName ==\"${DomainName.standaloneRelayActivationHysteresis}\"", equip.equipRef).toInt()
    hsTuners.analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery("domainName ==\"${DomainName.standaloneAnalogFanSpeedMultiplier}\"", equip.equipRef)

    // These are specific tuners
    when (equip) {

        is CpuV2Equip -> {
            hsTuners.minFanRuntimePostConditioning = TunerUtil.readTunerValByQuery("domainName ==\"${DomainName.minFanRuntimePostConditioning}\"", equip.equipRef).toInt()
        }

        is HpuV2Equip -> {
            hsTuners.auxHeating1Activate = TunerUtil.readTunerValByQuery("domainName ==\"${DomainName.auxHeating1Activate}\"", equip.equipRef)
            hsTuners.auxHeating2Activate = TunerUtil.readTunerValByQuery("domainName ==\"${DomainName.auxHeating2Activate}\"", equip.equipRef)
        }

        is Pipe2V2Equip -> {
            hsTuners.auxHeating1Activate = TunerUtil.readTunerValByQuery("domainName ==\"${DomainName.auxHeating1Activate}\"", equip.equipRef)
            hsTuners.auxHeating2Activate = TunerUtil.readTunerValByQuery("domainName ==\"${DomainName.auxHeating2Activate}\"", equip.equipRef)
            hsTuners.heatingThreshold = TunerUtil.readTunerValByQuery("domainName ==\"${DomainName.hyperstatPipe2FancoilHeatingThreshold}\"", equip.equipRef)
            hsTuners.coolingThreshold = TunerUtil.readTunerValByQuery("domainName ==\"${DomainName.hyperstatPipe2FancoilCoolingThreshold}\"", equip.equipRef)
            hsTuners.waterValveSamplingOnTime = TunerUtil.readTunerValByQuery("domainName ==\"${DomainName.waterValveSamplingOnTime}\"", equip.equipRef).toInt()
            hsTuners.waterValveSamplingWaitTime = TunerUtil.readTunerValByQuery("domainName ==\"${DomainName.waterValveSamplingWaitTime}\"", equip.equipRef).toInt()
            hsTuners.waterValveSamplingDuringLoopDeadbandOnTime = TunerUtil.readTunerValByQuery("domainName ==\"${DomainName.waterValveSamplingLoopDeadbandOnTime}\"", equip.equipRef).toInt()
            hsTuners.waterValveSamplingDuringLoopDeadbandWaitTime =  TunerUtil.readTunerValByQuery("domainName ==\"${DomainName.waterValveSamplingLoopDeadbandWaitTime}\"", equip.equipRef).toInt()
        }

    }

    return hsTuners
}

fun getLogicalPointList(equip: HyperStatEquip, config: HyperStatConfiguration): HashMap<Port, String> {

    val device = Domain.getEquipDevices()[equip.equipRef] as HyperStatDevice
    val logicalPoints = HashMap<Port, String>()

    if (device.relay1.readPoint().pointRef != null && device.relay1.readPoint().pointRef.isNotEmpty()) {
        if (config.relay1Enabled.enabled) logicalPoints[Port.RELAY_ONE] = device.relay1.readPoint().pointRef
    }
    if (device.relay2.readPoint().pointRef != null && device.relay2.readPoint().pointRef.isNotEmpty()) {
        if (config.relay2Enabled.enabled) logicalPoints[Port.RELAY_TWO] = device.relay2.readPoint().pointRef
    }
    if (device.relay3.readPoint().pointRef != null && device.relay3.readPoint().pointRef.isNotEmpty()) {
        if (config.relay3Enabled.enabled) logicalPoints[Port.RELAY_THREE] = device.relay3.readPoint().pointRef
    }
    if (device.relay4.readPoint().pointRef != null && device.relay4.readPoint().pointRef.isNotEmpty()) {
        if (config.relay4Enabled.enabled) logicalPoints[Port.RELAY_FOUR] = device.relay4.readPoint().pointRef
    }
    if (device.relay5.readPoint().pointRef != null && device.relay5.readPoint().pointRef.isNotEmpty()) {
        if (config.relay5Enabled.enabled) logicalPoints[Port.RELAY_FIVE] = device.relay5.readPoint().pointRef
    }
    if (device.relay6.readPoint().pointRef != null && device.relay6.readPoint().pointRef.isNotEmpty()) {
        if (config.relay6Enabled.enabled) logicalPoints[Port.RELAY_SIX] = device.relay6.readPoint().pointRef
    }
    if (device.analog1Out.readPoint().pointRef != null && device.analog1Out.readPoint().pointRef.isNotEmpty()) {
        if (config.analogOut1Enabled.enabled) logicalPoints[Port.ANALOG_OUT_ONE] = device.analog1Out.readPoint().pointRef
    }
    if (device.analog2Out.readPoint().pointRef != null && device.analog2Out.readPoint().pointRef.isNotEmpty()) {
        if (config.analogOut2Enabled.enabled) logicalPoints[Port.ANALOG_OUT_TWO] = device.analog2Out.readPoint().pointRef
    }
    if (device.analog3Out.readPoint().pointRef != null && device.analog3Out.readPoint().pointRef.isNotEmpty()) {
        if (config.analogOut3Enabled.enabled) logicalPoints[Port.ANALOG_OUT_THREE] = device.analog3Out.readPoint().pointRef
    }

    CcuLog.d(L.TAG_CCU_HSHST, "Logical Points: for ${equip.equipRef} $logicalPoints")
    return logicalPoints
}

fun fetchBasicSettings(equip: HyperStatEquip) = BasicSettings(conditioningMode = StandaloneConditioningMode.values()[equip.conditioningMode.readPriorityVal().toInt()], fanMode = StandaloneFanStage.values()[equip.fanOpMode.readPriorityVal().toInt()])

fun getConfiguration(equipRef: String): HyperStatConfiguration? {

    when (val equip = Domain.getDomainEquip(equipRef) as HyperStatEquip) {
        is CpuV2Equip -> {
            val cpuModel = ModelLoader.getHyperStatCpuModel() as SeventyFiveFProfileDirective
            return CpuConfiguration(
                    nodeAddress = equip.nodeAddress, nodeType = NodeType.HYPER_STAT.name,
                    priority = 0,  roomRef = equip.roomRef!!, floorRef = equip.floorRef!!,
                    profileType = ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT, model = cpuModel
            ).getActiveConfiguration()
        }
        is MonitoringEquip -> {
            val monitoringModel = ModelLoader.getHyperStatMonitoringModel() as SeventyFiveFProfileDirective
            return MonitoringConfiguration(
                nodeAddress = equip.nodeAddress, nodeType = NodeType.HYPER_STAT.name,
                priority = 0,  roomRef = equip.roomRef!!, floorRef = equip.floorRef!!,
                profileType = ProfileType.HYPERSTAT_MONITORING, model = monitoringModel
            ).getActiveConfiguration()
        }
        is Pipe2V2Equip -> {
            val pipe2Model = ModelLoader.getHyperStatPipe2Model() as SeventyFiveFProfileDirective
            return Pipe2Configuration(
                    nodeAddress = equip.nodeAddress, nodeType = NodeType.HYPER_STAT.name,
                    priority = 0,  roomRef = equip.roomRef!!, floorRef = equip.floorRef!!,
                    profileType = ProfileType.HYPERSTAT_TWO_PIPE_FCU, model = pipe2Model
            ).getActiveConfiguration()
        }
        is HpuV2Equip -> {
            val hpuModel = ModelLoader.getHyperStatHpuModel() as SeventyFiveFProfileDirective
            return HpuConfiguration(
                    nodeAddress = equip.nodeAddress, nodeType = NodeType.HYPER_STAT.name,
                    priority = 0,  roomRef = equip.roomRef!!, floorRef = equip.floorRef!!,
                    profileType = ProfileType.HYPERSTAT_HEAT_PUMP_UNIT, model= hpuModel
            ).getActiveConfiguration()
        }

        else -> {
            return null
        }
    }
}

fun updateAllLoopOutput(equip: HyperStatEquip, coolingLoop: Int, heatingLoop: Int, fanLoop: Int, isHpuProfile: Boolean = false, compressorLoop: Int = 0) {
    equip.apply {
        coolingLoopOutput.writePointValue(coolingLoop.toDouble())
        heatingLoopOutput.writePointValue(heatingLoop.toDouble())
        fanLoopOutput.writePointValue(fanLoop.toDouble())
        if (isHpuProfile) {
            (equip as HpuV2Equip).compressorLoopOutput.writePointValue(compressorLoop.toDouble())
        }
    }
}

fun updateOccupancyDetection(equip: HyperStatEquip) {
    if (equip.zoneOccupancy.readHisVal() > 0) equip.occupancyDetection.writeHisValueByIdWithoutCOV(1.0)
}

fun updateOperatingMode(currentTemp: Double, averageDesiredTemp: Double, basicSettings: BasicSettings, equip: HyperStatEquip) {
    val zoneOperatingMode = when {
        currentTemp < averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.COOL_ONLY -> ZoneState.HEATING.ordinal
        currentTemp >= averageDesiredTemp && basicSettings.conditioningMode != StandaloneConditioningMode.HEAT_ONLY -> ZoneState.COOLING.ordinal
        else -> ZoneState.DEADBAND.ordinal
    }
    equip.operatingMode.writeHisVal(zoneOperatingMode.toDouble())
}

fun getActualConditioningMode(config: HyperStatConfiguration, selectedConditioningMode: Int): Int {
    if (selectedConditioningMode == 0)
        return StandaloneConditioningMode.OFF.ordinal
    return when (getPossibleConditionMode(config)) {
        PossibleConditioningMode.BOTH -> StandaloneConditioningMode.values()[selectedConditioningMode].ordinal
        PossibleConditioningMode.COOLONLY -> StandaloneConditioningMode.COOL_ONLY.ordinal
        PossibleConditioningMode.HEATONLY -> StandaloneConditioningMode.HEAT_ONLY.ordinal
        PossibleConditioningMode.OFF -> StandaloneConditioningMode.values()[selectedConditioningMode].ordinal

    }
}

fun getSelectedConditioningMode(configuration: HyperStatConfiguration, actualConditioningMode: Int): Int {
    if (actualConditioningMode == 0) return StandaloneConditioningMode.OFF.ordinal
    return if (getPossibleConditionMode(configuration) == PossibleConditioningMode.BOTH)
        StandaloneConditioningMode.values()[actualConditioningMode].ordinal
    else
        1 // always it will be 1 because possibility is Off,CoolOnly | Off,Heatonly
}

fun getPossibleConditionMode(config: HyperStatConfiguration): PossibleConditioningMode {
    var cooling = false
    var heating = false
    when (config) {
        is CpuConfiguration -> {
            cooling = config.isCoolingAvailable()
            heating = config.isHeatingAvailable()
        }

        is Pipe2Configuration -> {
            return PossibleConditioningMode.BOTH
        }

        is HpuConfiguration -> {
            return PossibleConditioningMode.BOTH
        }
    }

    return if (cooling && heating) {
        PossibleConditioningMode.BOTH
    } else if (cooling) {
        PossibleConditioningMode.COOLONLY
    } else if (heating) {
        PossibleConditioningMode.HEATONLY
    } else {
        PossibleConditioningMode.OFF
    }
}

fun getPossibleFanModeSettings(fanLevel: Int): PossibleFanMode {
    return when (fanLevel) {
        AUTO -> PossibleFanMode.AUTO
        LOW -> PossibleFanMode.LOW
        MED -> PossibleFanMode.MED
        HIGH -> PossibleFanMode.HIGH
        LOW_MED -> PossibleFanMode.LOW_MED
        LOW_HIGH -> PossibleFanMode.LOW_HIGH
        MED_HIGH -> PossibleFanMode.MED_HIGH
        LOW_MED_HIGH -> PossibleFanMode.LOW_MED_HIGH
        else -> PossibleFanMode.OFF
    }
}

fun getCpuFanLevel(config: CpuConfiguration): Int {
    var fanLevel = 0
    var fanEnabledStages: Triple<Boolean, Boolean, Boolean> = Triple(
            first = false,  //  Fan low
            second = false, //  Fan Medium
            third = false   //  Fan High
    )

    if (config.isAnyAnalogOutEnabledAssociated(association = HsCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal)
            || config.isAnyAnalogOutEnabledAssociated(association = HsCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal)) {
        return LOW_MED_HIGH // All options are enabled due to analog fan speed
    }

    val relays = config.getRelayEnabledAssociations()
    for ((enabled, association) in relays) {
        if (enabled && association == HsCpuRelayMapping.FAN_LOW_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(first = true)
        }
        if (enabled && association == HsCpuRelayMapping.FAN_MEDIUM_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(second = true)
        }
        if (enabled && association == HsCpuRelayMapping.FAN_HIGH_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(third = true)
        }
    }

    if (fanEnabledStages.first) fanLevel += LOW
    if (fanEnabledStages.second) fanLevel += MED
    if (fanEnabledStages.third) fanLevel += HIGH

    if (fanLevel == 0 && config.isAnyRelayEnabledAssociated(association = HsCpuRelayMapping.FAN_ENABLED.ordinal)) {
        fanLevel = AUTO
    }


    return fanLevel
}

fun getHpuFanLevel(config: HpuConfiguration): Int {
    var fanLevel = 0
    var fanEnabledStages: Triple<Boolean, Boolean, Boolean> = Triple(
            first = false,  //  Fan low
            second = false, //  Fan Medium
            third = false   //  Fan High
    )

    if (config.isAnyAnalogOutEnabledAssociated(association = HsHpuAnalogOutMapping.FAN_SPEED.ordinal)) {
        return LOW_MED_HIGH // All options are enabled due to analog fan speed
    }

    val relays = config.getRelayEnabledAssociations()
    for ((enabled, association) in relays) {
        if (enabled && association == HsHpuRelayMapping.FAN_LOW_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(first = true)
        }
        if (enabled && association == HsHpuRelayMapping.FAN_MEDIUM_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(second = true)
        }
        if (enabled && association == HsHpuRelayMapping.FAN_HIGH_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(third = true)
        }
    }

    if (fanEnabledStages.first) fanLevel += LOW
    if (fanEnabledStages.second) fanLevel += MED
    if (fanEnabledStages.third) fanLevel += HIGH

    if (fanLevel == 0 && config.isAnyRelayEnabledAssociated(association = HsHpuRelayMapping.FAN_ENABLED.ordinal)) {
        fanLevel = AUTO
    }

    return fanLevel
}

fun getHyperStatDevice(nodeAddress: Int): HashMap<Any, Any> {
    return CCUHsApi.getInstance().readEntity("domainName == \"${DomainName.hyperstatDevice}\" and addr == \"$nodeAddress\"")
}


fun getHsFanLevel(config: HyperStatConfiguration): Int {
    return when (config) {
        is CpuConfiguration -> getCpuFanLevel(config)
        is HpuConfiguration -> getHpuFanLevel(config)
        is Pipe2Configuration -> getPipe2FanLevel(config)
        else -> 0
    }
}

fun getPipe2FanLevel(config: Pipe2Configuration): Int {
    var fanLevel = 0
    var fanEnabledStages: Triple<Boolean, Boolean, Boolean> = Triple(
            first = false,  //  Fan low
            second = false, //  Fan Medium
            third = false   //  Fan High
    )

    if (config.isAnyAnalogOutEnabledAssociated(association = HsPipe2AnalogOutMapping.FAN_SPEED.ordinal)) {
        return LOW_MED_HIGH // All options are enabled due to analog fan speed
    }

    val relays = config.getRelayEnabledAssociations()
    for ((enabled, association) in relays) {
        if (enabled && association == HsPipe2RelayMapping.FAN_LOW_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(first = true)
        }
        if (enabled && association == HsPipe2RelayMapping.FAN_MEDIUM_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(second = true)
        }
        if (enabled && association == HsPipe2RelayMapping.FAN_HIGH_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(third = true)
        }
    }

    if (fanEnabledStages.first) fanLevel += LOW
    if (fanEnabledStages.second) fanLevel += MED
    if (fanEnabledStages.third) fanLevel += HIGH

    if (fanLevel == 0 && config.isAnyRelayEnabledAssociated(association = HsPipe2RelayMapping.FAN_ENABLED.ordinal)) {
        fanLevel = AUTO
    }


    return fanLevel
}




/**
 * Test cases are added to this function
 *  Highly recommended to run test cases when change is required
 */
fun getSelectedFanMode(fanLevel: Int, selectedFan: Int): Int {
    try {
        if (selectedFan == OFF) return StandaloneFanStage.OFF.ordinal // No fan stages are selected so only off can present here
        if (selectedFan == AUTO) return StandaloneFanStage.AUTO.ordinal // No fan stages are selected so only off can present here

        return (when (fanLevel) {
            LOW -> {
                if (selectedFan <= StandaloneFanStage.LOW_ALL_TIME.ordinal) {
                    StandaloneFanStage.values()[selectedFan]
                } else {
                    StandaloneFanStage.OFF
                }
            }

            LOW_MED -> {
                if (selectedFan <= StandaloneFanStage.MEDIUM_ALL_TIME.ordinal) {
                    StandaloneFanStage.values()[selectedFan]
                } else {
                    StandaloneFanStage.OFF
                }
            }

            LOW_MED_HIGH -> StandaloneFanStage.values()[selectedFan]
            MED -> {
                when (selectedFan) {
                    in listOf(2, 3, 4) -> StandaloneFanStage.values()[selectedFan + 3]
                    in listOf(
                        StandaloneFanStage.MEDIUM_CUR_OCC.ordinal,
                        StandaloneFanStage.MEDIUM_OCC.ordinal,
                        StandaloneFanStage.MEDIUM_ALL_TIME.ordinal
                    ) -> StandaloneFanStage.values()[selectedFan - 3]
                    else -> StandaloneFanStage.OFF
                }
            }
            MED_HIGH -> {
                when (selectedFan) {
                    in listOf(2, 3, 4) -> StandaloneFanStage.values()[selectedFan + 6]
                    in listOf(
                        StandaloneFanStage.MEDIUM_CUR_OCC.ordinal,
                        StandaloneFanStage.MEDIUM_OCC.ordinal,
                        StandaloneFanStage.MEDIUM_ALL_TIME.ordinal,
                        StandaloneFanStage.HIGH_CUR_OCC.ordinal,
                        StandaloneFanStage.HIGH_OCC.ordinal,
                        StandaloneFanStage.HIGH_ALL_TIME.ordinal
                    ) -> StandaloneFanStage.values()[selectedFan - 3]
                    else -> StandaloneFanStage.OFF
                }
            }
            HIGH -> {
                when (selectedFan) {
                    in listOf(2, 3, 4) -> StandaloneFanStage.values()[selectedFan + 6]
                    in listOf(
                        StandaloneFanStage.HIGH_CUR_OCC.ordinal,
                        StandaloneFanStage.HIGH_OCC.ordinal,
                        StandaloneFanStage.HIGH_ALL_TIME.ordinal
                    ) -> StandaloneFanStage.values()[selectedFan - 6]
                    else -> StandaloneFanStage.OFF
                }
            }
            LOW_HIGH -> {
                when (selectedFan) {
                    in listOf(2, 3, 4) -> StandaloneFanStage.values()[selectedFan]
                    in listOf(5, 6, 7) -> StandaloneFanStage.values()[selectedFan + 3]
                    in listOf(
                        StandaloneFanStage.HIGH_CUR_OCC.ordinal,
                        StandaloneFanStage.HIGH_OCC.ordinal,
                        StandaloneFanStage.HIGH_ALL_TIME.ordinal
                    ) -> StandaloneFanStage.values()[selectedFan - 3]
                    else -> StandaloneFanStage.OFF
                }
            }
            else -> StandaloneFanStage.OFF


        }).ordinal
    } catch (e: ArrayIndexOutOfBoundsException) {
        CcuLog.e(L.TAG_CCU_HSHST, "Error getSelectedFan function ${e.localizedMessage}", e)
    }
    return StandaloneFanStage.OFF.ordinal
}

fun getModelByEquipRef(equipRef: String): ModelDirective? {
    return when (Domain.getDomainEquip(equipRef) as HyperStatEquip) {
        is CpuV2Equip -> ModelLoader.getHyperStatCpuModel()
        is HpuV2Equip -> ModelLoader.getHyperStatHpuModel()
        is Pipe2V2Equip -> ModelLoader.getHyperStatPipe2Model()
        else -> null
    }
}
