package a75f.io.logic.bo.building.statprofiles.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.hyperstat.HsCpuEquip
import a75f.io.domain.equips.hyperstat.HsHpuEquip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.hyperstat.MonitoringEquip
import a75f.io.domain.equips.hyperstat.HsPipe2Equip
import a75f.io.domain.equips.hyperstat.HsPipe4Equip
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsCpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsCpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsHpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsHpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe2AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe2RelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe4AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe4RelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.MonitoringConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Pipe2Configuration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe4Configuration
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created by Manjunath K on 28-04-2025.
 */

class HsFanConstants {
    companion object {
        const val OFF = 0
        const val AUTO = 1
        const val LOW = 6
        const val MED = 7
        const val HIGH = 8
        const val LOW_MED = 13
        const val LOW_HIGH = 14
        const val MED_HIGH = 15
        const val LOW_MED_HIGH = 21
    }
}

fun logResults(config: HyperStatConfiguration, tag: String, logicalPointsList: HashMap<Port, String>) {
    val haystack = CCUHsApi.getInstance()
    listOf(
        Triple(config.relay1Enabled.enabled, config.relay1Association.associationVal, Port.RELAY_ONE),
        Triple(config.relay2Enabled.enabled, config.relay2Association.associationVal, Port.RELAY_TWO),
        Triple(config.relay3Enabled.enabled, config.relay3Association.associationVal, Port.RELAY_THREE),
        Triple(config.relay4Enabled.enabled, config.relay4Association.associationVal, Port.RELAY_FOUR),
        Triple(config.relay5Enabled.enabled, config.relay5Association.associationVal, Port.RELAY_FIVE),
        Triple(config.relay6Enabled.enabled, config.relay6Association.associationVal, Port.RELAY_SIX)
    ).forEach { (enabled, association, port) ->
        if (enabled && logicalPointsList[port] != null) {
            val logicalPointValue = haystack.readHisValById(logicalPointsList[port])
            when (config) {
                is CpuConfiguration -> CcuLog.d(
                    tag, "$port = ${HsCpuRelayMapping.values()[association]} : $logicalPointValue"
                )

                is HpuConfiguration -> CcuLog.d(
                    tag, "$port = ${HsHpuRelayMapping.values()[association]} : $logicalPointValue"
                )

                is Pipe2Configuration -> CcuLog.d(
                    tag, "$port = ${HsPipe2RelayMapping.values()[association]} : $logicalPointValue"
                )
                is HsPipe4Configuration -> CcuLog.d(
                    tag, "$port = ${HsPipe4RelayMapping.values()[association]} : $logicalPointValue"
                )
            }

        }
    }

    listOf(
        Triple(config.analogOut1Enabled.enabled, config.analogOut1Association.associationVal, Port.ANALOG_OUT_ONE),
        Triple(config.analogOut2Enabled.enabled, config.analogOut2Association.associationVal, Port.ANALOG_OUT_TWO),
        Triple(config.analogOut3Enabled.enabled, config.analogOut3Association.associationVal, Port.ANALOG_OUT_THREE)
    ).forEach { (enabled, association, port) ->
        if (enabled && logicalPointsList[port] != null) {
            val logicalPointsValue = haystack.readHisValById(logicalPointsList[port])
            when (config) {
                is CpuConfiguration -> CcuLog.d(
                    tag, "$port = ${HsCpuAnalogOutMapping.values()[association]} : $logicalPointsValue"
                )

                is HpuConfiguration -> CcuLog.d(
                    tag, "$port = ${HsHpuAnalogOutMapping.values()[association]} : $logicalPointsValue"
                )

                is Pipe2Configuration -> CcuLog.d(
                    tag, "$port = ${HsPipe2AnalogOutMapping.values()[association]} : $logicalPointsValue"
                )

                is HsPipe4Configuration -> CcuLog.d(
                    tag, "$port = ${HsPipe4AnalogOutMapping.values()[association]} : $logicalPointsValue"
                )
            }
        }
    }
}

fun fetchBasicSettings(equip: HyperStatEquip) = BasicSettings(
    conditioningMode = StandaloneConditioningMode.values()[equip.conditioningMode.readPriorityVal()
        .toInt()], fanMode = StandaloneFanStage.values()[equip.fanOpMode.readPriorityVal().toInt()]
)

fun getHsConfiguration(equipRef: String): HyperStatConfiguration? {

    when (val equip = Domain.getDomainEquip(equipRef) as HyperStatEquip) {
        is HsCpuEquip -> {
            val cpuModel = ModelLoader.getHyperStatCpuModel() as SeventyFiveFProfileDirective
            return CpuConfiguration(
                nodeAddress = equip.nodeAddress,
                nodeType = NodeType.HYPER_STAT.name,
                priority = 0,
                roomRef = equip.roomRef!!,
                floorRef = equip.floorRef!!,
                profileType = ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT,
                model = cpuModel
            ).getActiveConfiguration()
        }

        is MonitoringEquip -> {
            val monitoringModel =
                ModelLoader.getHyperStatMonitoringModel() as SeventyFiveFProfileDirective
            return MonitoringConfiguration(
                nodeAddress = equip.nodeAddress,
                nodeType = NodeType.HYPER_STAT.name,
                priority = 0,
                roomRef = equip.roomRef!!,
                floorRef = equip.floorRef!!,
                profileType = ProfileType.HYPERSTAT_MONITORING,
                model = monitoringModel
            ).getActiveConfiguration()
        }

        is HsPipe2Equip -> {
            val pipe2Model = ModelLoader.getHyperStatPipe2Model() as SeventyFiveFProfileDirective
            return Pipe2Configuration(
                nodeAddress = equip.nodeAddress,
                nodeType = NodeType.HYPER_STAT.name,
                priority = 0,
                roomRef = equip.roomRef!!,
                floorRef = equip.floorRef!!,
                profileType = ProfileType.HYPERSTAT_TWO_PIPE_FCU,
                model = pipe2Model
            ).getActiveConfiguration()
        }

        is HsHpuEquip -> {
            val hpuModel = ModelLoader.getHyperStatHpuModel() as SeventyFiveFProfileDirective
            return HpuConfiguration(
                nodeAddress = equip.nodeAddress,
                nodeType = NodeType.HYPER_STAT.name,
                priority = 0,
                roomRef = equip.roomRef!!,
                floorRef = equip.floorRef!!,
                profileType = ProfileType.HYPERSTAT_HEAT_PUMP_UNIT,
                model = hpuModel
            ).getActiveConfiguration()
        }
        is HsPipe4Equip -> {
            val pipe4Model = ModelLoader.getHyperStatPipe4Model() as SeventyFiveFProfileDirective
            return HsPipe4Configuration(
                nodeAddress = equip.nodeAddress,
                nodeType = NodeType.HYPER_STAT.name,
                priority = 0,
                roomRef = equip.roomRef!!,
                floorRef = equip.floorRef!!,
                profileType = ProfileType.HYPERSTAT_FOUR_PIPE_FCU,
                model = pipe4Model
            ).getActiveConfiguration()
        }

        else -> {
            return null
        }
    }
}

fun getHSModelByEquipRef(equipRef: String): ModelDirective? {
    return when (Domain.getDomainEquip(equipRef) as HyperStatEquip) {
        is HsCpuEquip -> ModelLoader.getHyperStatCpuModel()
        is HsHpuEquip -> ModelLoader.getHyperStatHpuModel()
        is HsPipe2Equip -> ModelLoader.getHyperStatPipe2Model()
        is HsPipe4Equip -> ModelLoader.getHyperStatPipe4Model()
        else -> null
    }
}


fun calculateFanLevel(
    config: HyperStatConfiguration,
    isAnalogFanEnabled: Boolean,
    lowPosition: Int, lowVentilationPosition: Int = -1, mediumPosition: Int, highPosition: Int,
    fanEnabledPosition: Int, occupiedEnabledPosition: Int
): Int {
    var fanLevel = 0
    var fanEnabledStages: Triple<Boolean, Boolean, Boolean> = Triple(
        first = false,  //  Fan low
        second = false, //  Fan Medium
        third = false   //  Fan High
    )

    if (isAnalogFanEnabled) {
        return HsFanConstants.LOW_MED_HIGH // All options are enabled due to analog fan speed
    }

    val relays = config.getRelayEnabledAssociations()
    for ((enabled, association) in relays) {
        if (enabled && (association == lowPosition || association == lowVentilationPosition)) {
            fanEnabledStages = fanEnabledStages.copy(first = true)
        }
        if (enabled && association == mediumPosition) {
            fanEnabledStages = fanEnabledStages.copy(second = true)
        }
        if (enabled && association == highPosition) {
            fanEnabledStages = fanEnabledStages.copy(third = true)
        }
    }

    if (fanEnabledStages.first) fanLevel += HsFanConstants.LOW
    if (fanEnabledStages.second) fanLevel += HsFanConstants.MED
    if (fanEnabledStages.third) fanLevel += HsFanConstants.HIGH

    if (fanLevel == 0 && (config.isAnyRelayEnabledAssociated(association = fanEnabledPosition)
                || config.isAnyRelayEnabledAssociated(association = occupiedEnabledPosition))
    ) {
        fanLevel = HsFanConstants.AUTO
    }
    return fanLevel
}


fun getCpuFanLevel(config: CpuConfiguration): Int {

    return calculateFanLevel(
        config = config,
        isAnalogFanEnabled = config.isAnyAnalogOutEnabledAssociated(association = HsCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal)
                || config.isAnyAnalogOutEnabledAssociated(association = HsCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal),
        lowPosition = HsCpuRelayMapping.FAN_LOW_SPEED.ordinal,
        mediumPosition = HsCpuRelayMapping.FAN_MEDIUM_SPEED.ordinal,
        highPosition = HsCpuRelayMapping.FAN_HIGH_SPEED.ordinal,
        fanEnabledPosition = HsCpuRelayMapping.FAN_ENABLED.ordinal,
        occupiedEnabledPosition = HsCpuRelayMapping.OCCUPIED_ENABLED.ordinal
    )
}
fun getHpuFanLevel(config: HpuConfiguration): Int {
    return calculateFanLevel(
        config = config,
        isAnalogFanEnabled = config.isAnyAnalogOutEnabledAssociated(association = HsHpuAnalogOutMapping.FAN_SPEED.ordinal),
        lowPosition = HsHpuRelayMapping.FAN_LOW_SPEED.ordinal,
        mediumPosition = HsHpuRelayMapping.FAN_MEDIUM_SPEED.ordinal,
        highPosition = HsHpuRelayMapping.FAN_HIGH_SPEED.ordinal,
        fanEnabledPosition = HsHpuRelayMapping.FAN_ENABLED.ordinal,
        occupiedEnabledPosition = HsHpuRelayMapping.OCCUPIED_ENABLED.ordinal
    )
}

fun getHSPipe2FanLevel(config: Pipe2Configuration): Int {
    return calculateFanLevel(
        config = config,
        isAnalogFanEnabled = config.isAnyAnalogOutEnabledAssociated(association = HsPipe2AnalogOutMapping.FAN_SPEED.ordinal),
        lowPosition = HsPipe2RelayMapping.FAN_LOW_SPEED.ordinal,
        lowVentilationPosition = HsPipe2RelayMapping.FAN_LOW_VENTILATION.ordinal,
        mediumPosition = HsPipe2RelayMapping.FAN_MEDIUM_SPEED.ordinal,
        highPosition = HsPipe2RelayMapping.FAN_HIGH_SPEED.ordinal,
        fanEnabledPosition = HsPipe2RelayMapping.FAN_ENABLED.ordinal,
        occupiedEnabledPosition = HsPipe2RelayMapping.OCCUPIED_ENABLED.ordinal
    )
}
fun getHSPipe4FanLevel(config: HsPipe4Configuration): Int {
    return calculateFanLevel(
        config = config,
        isAnalogFanEnabled = config.isAnyAnalogOutEnabledAssociated(association = HsPipe4AnalogOutMapping.FAN_SPEED.ordinal),
        lowPosition = HsPipe4RelayMapping.FAN_LOW_SPEED.ordinal,
        lowVentilationPosition = HsPipe4RelayMapping.FAN_LOW_VENTILATION.ordinal,
        mediumPosition = HsPipe4RelayMapping.FAN_MEDIUM_SPEED.ordinal,
        highPosition = HsPipe4RelayMapping.FAN_HIGH_SPEED.ordinal,
        fanEnabledPosition = HsPipe4RelayMapping.FAN_ENABLED.ordinal,
        occupiedEnabledPosition = HsPipe4RelayMapping.OCCUPIED_ENABLED.ordinal
    )
}

fun getHsFanLevel(config: HyperStatConfiguration): Int {
    return when (config) {
        is CpuConfiguration -> getCpuFanLevel(config)
        is HpuConfiguration -> getHpuFanLevel(config)
        is Pipe2Configuration -> getHSPipe2FanLevel(config)
        is HsPipe4Configuration -> getHSPipe4FanLevel(config)
        else -> 0
    }
}

fun getHsPossibleFanModeSettings(fanLevel: Int): PossibleFanMode {
    return when (fanLevel) {
        HsFanConstants.AUTO -> PossibleFanMode.AUTO
        HsFanConstants.LOW -> PossibleFanMode.LOW
        HsFanConstants.MED -> PossibleFanMode.MED
        HsFanConstants.HIGH -> PossibleFanMode.HIGH
        HsFanConstants.LOW_MED -> PossibleFanMode.LOW_MED
        HsFanConstants.LOW_HIGH -> PossibleFanMode.LOW_HIGH
        HsFanConstants.MED_HIGH -> PossibleFanMode.MED_HIGH
        HsFanConstants.LOW_MED_HIGH -> PossibleFanMode.LOW_MED_HIGH
        else -> PossibleFanMode.OFF
    }
}

/**
 * Test cases are added to this function
 *  Highly recommended to run test cases when change is required
 */
fun getHSSelectedFanMode(fanLevel: Int, selectedFan: Int): Int {
    try {
        if (selectedFan == HsFanConstants.OFF) return StandaloneFanStage.OFF.ordinal // No fan stages are selected so only off can present here
        if (selectedFan == HsFanConstants.AUTO) return StandaloneFanStage.AUTO.ordinal // No fan stages are selected so only off can present here

        return (when (fanLevel) {
            HsFanConstants.LOW -> {
                if (selectedFan <= StandaloneFanStage.LOW_ALL_TIME.ordinal) {
                    StandaloneFanStage.values()[selectedFan]
                } else {
                    StandaloneFanStage.OFF
                }
            }

            HsFanConstants.LOW_MED -> {
                if (selectedFan <= StandaloneFanStage.MEDIUM_ALL_TIME.ordinal) {
                    StandaloneFanStage.values()[selectedFan]
                } else {
                    StandaloneFanStage.OFF
                }
            }

            HsFanConstants.LOW_MED_HIGH -> StandaloneFanStage.values()[selectedFan]
            HsFanConstants.MED -> {
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

            HsFanConstants.MED_HIGH -> {
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

            HsFanConstants.HIGH -> {
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

            HsFanConstants.LOW_HIGH -> {
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

fun updateOccupancyDetection(equip: HyperStatEquip) {
    if (equip.zoneOccupancy.readHisVal() > 0) equip.occupancyDetection.writeHisValueByIdWithoutCOV(
        1.0
    )
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

fun getSelectedConditioningMode(
    configuration: HyperStatConfiguration,
    actualConditioningMode: Int
): Int {
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

        is HsPipe4Configuration -> {
            cooling = config.isCoolingAvailable()
            heating = config.isHeatingAvailable()
        }

        is Pipe2Configuration, is HpuConfiguration -> {
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

fun getHSDomainEquipByEquipRef(equipRef: String): HyperStatEquip? {
    return when (val equip = Domain.getDomainEquip(equipRef)) {
        is HsCpuEquip -> equip
        is HsHpuEquip -> equip
        is HsPipe2Equip -> equip
        is HsPipe4Equip -> equip
        else -> null
    }
}
