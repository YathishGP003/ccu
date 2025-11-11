package a75f.io.logic.bo.building.statprofiles.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.devices.MyStatDevice
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.domain.equips.mystat.MyStatPipe4Equip
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2RelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4RelayMapping
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created by Manjunath K on 28-04-2025.
 */


class MsFanConstants {
    companion object {
        const val OFF = 0
        const val AUTO = 1
        const val LOW = 6
        const val HIGH = 8
        const val LOW_HIGH = 14
    }
}

fun getMyStatConfiguration(equipRef: String): MyStatConfiguration? {

    when (val equip = Domain.getDomainEquip(equipRef) as MyStatEquip) {
        is MyStatCpuEquip -> {
            val cpuModel = ModelLoader.getMyStatCpuModel() as SeventyFiveFProfileDirective
            return MyStatCpuConfiguration(
                nodeAddress = equip.nodeAddress,
                nodeType = NodeType.MYSTAT.name,
                priority = 0,
                roomRef = equip.roomRef!!,
                floorRef = equip.floorRef!!,
                profileType = ProfileType.MYSTAT_CPU,
                model = cpuModel
            ).getActiveConfiguration()

        }

        is MyStatPipe2Equip -> {
            val pipe2Model = ModelLoader.getMyStatPipe2Model() as SeventyFiveFProfileDirective
            return MyStatPipe2Configuration(
                nodeAddress = equip.nodeAddress,
                nodeType = NodeType.MYSTAT.name,
                priority = 0,
                roomRef = equip.roomRef!!,
                floorRef = equip.floorRef!!,
                profileType = ProfileType.MYSTAT_PIPE2,
                model = pipe2Model
            ).getActiveConfiguration()
        }

        is MyStatHpuEquip -> {
            val hpuModel = ModelLoader.getMyStatHpuModel() as SeventyFiveFProfileDirective
            return MyStatHpuConfiguration(
                nodeAddress = equip.nodeAddress,
                nodeType = NodeType.MYSTAT.name,
                priority = 0,
                roomRef = equip.roomRef!!,
                floorRef = equip.floorRef!!,
                profileType = ProfileType.MYSTAT_HPU,
                model = hpuModel
            ).getActiveConfiguration()
        }

        is MyStatPipe4Equip -> {
            val pipe4Model = ModelLoader.getMyStatPipe4Model() as SeventyFiveFProfileDirective
            return MyStatPipe4Configuration(
                nodeAddress = equip.nodeAddress,
                nodeType = NodeType.MYSTAT.name,
                priority = 0,
                roomRef = equip.roomRef!!,
                floorRef = equip.floorRef!!,
                profileType = ProfileType.MYSTAT_PIPE4,
                model = pipe4Model
            ).getActiveConfiguration()
        }

        else -> {
            return null
        }
    }
}

fun getMyStatDevice(nodeAddress: Int): HashMap<Any, Any> {
    return CCUHsApi.getInstance()
        .readEntity("domainName == \"${DomainName.mystatDevice}\" and addr == \"$nodeAddress\"")
}

fun updateLogicalPoint(pointId: String?, value: Double) {
    if (pointId != null) {
        CCUHsApi.getInstance().writePointValue(CCUHsApi.getInstance().readEntity("id == $pointId"), value)
    } else {
        CcuLog.i(L.TAG_CCU_MSHST, "updateLogicalPointIdValue: But point id is null !!")
    }
}


fun fetchMyStatBasicSettings(equip: MyStatEquip) = MyStatBasicSettings(
    conditioningMode = StandaloneConditioningMode.values()[equip.conditioningMode.readPriorityVal()
        .toInt()], fanMode = MyStatFanStages.values()[equip.fanOpMode.readPriorityVal().toInt()]
)


fun getMyStatFanLevel(config: MyStatConfiguration): Int {
    when (config) {
        is MyStatCpuConfiguration -> return getMyStatCpuFanLevel(config)
        is MyStatPipe2Configuration -> return getMyStatPipe2FanLevel(config)
        is MyStatHpuConfiguration -> return getMyStatHpuFanLevel(config)
        is MyStatPipe4Configuration -> return getMyStatPipe4FanLevel(config)
    }
    return -1
}

fun getMyStatPipe2FanLevel(config: MyStatPipe2Configuration): Int {
    var fanLevel = 0
    var fanEnabledStages: Pair<Boolean, Boolean> = Pair(first = false, second = false)

    if ((config.universalOut1.enabled && config.universalOut1Association.associationVal == MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal)
        || (config.universalOut2.enabled && config.universalOut2Association.associationVal == MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal)
    ) {
        return MsFanConstants.LOW_HIGH // All options are enabled due to analog fan speed
    }

    val relays = config.getRelayEnabledAssociations()
    for ((enabled, association) in relays) {
        if (enabled && association == MyStatPipe2RelayMapping.FAN_LOW_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(first = true)
        }
        if (enabled && association == MyStatPipe2RelayMapping.FAN_LOW_VENTILATION.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(first = true)
        }
        if (enabled && association == MyStatPipe2RelayMapping.FAN_LOW_VENTILATION.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(first = true)
        }
        if (enabled && association == MyStatPipe2RelayMapping.FAN_HIGH_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(second = true)
        }
    }

    if (fanEnabledStages.first) fanLevel += MsFanConstants.LOW
    if (fanEnabledStages.second) fanLevel += MsFanConstants.HIGH
    if (fanLevel == 0 && (config.isAnyRelayEnabledAssociated(association = MyStatPipe2RelayMapping.FAN_ENABLED.ordinal)
                || config.isAnyRelayEnabledAssociated(association = MyStatPipe2RelayMapping.OCCUPIED_ENABLED.ordinal))) {
        fanLevel = MsFanConstants.AUTO
    }

    return fanLevel
}

fun getMyStatPipe4FanLevel(config: MyStatPipe4Configuration): Int {
    var fanLevel = 0
    var fanEnabledStages: Pair<Boolean, Boolean> = Pair(first = false, second = false)

    if ((config.universalOut1.enabled && config.universalOut1Association.associationVal == MyStatPipe4AnalogOutMapping.FAN_SPEED.ordinal)
        || (config.universalOut2.enabled && config.universalOut2Association.associationVal == MyStatPipe4AnalogOutMapping.FAN_SPEED.ordinal)
    ) {
        return MsFanConstants.LOW_HIGH // All options are enabled due to analog fan speed
    }

    val relays = config.getRelayEnabledAssociations()
    for ((enabled, association) in relays) {
        if (enabled && association == MyStatPipe4RelayMapping.FAN_LOW_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(first = true)
        }
        if (enabled && association == MyStatPipe4RelayMapping.FAN_LOW_VENTILATION.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(first = true)
        }
        if (enabled && association == MyStatPipe4RelayMapping.FAN_HIGH_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(second = true)
        }
    }

    if (fanEnabledStages.first) fanLevel += MsFanConstants.LOW
    if (fanEnabledStages.second) fanLevel += MsFanConstants.HIGH
    if (fanLevel == 0 && (config.isAnyRelayEnabledAssociated(association = MyStatPipe4RelayMapping.FAN_ENABLED.ordinal)
                || config.isAnyRelayEnabledAssociated(association = MyStatPipe4RelayMapping.OCCUPIED_ENABLED.ordinal))
    ) {
        fanLevel = MsFanConstants.AUTO
    }

    return fanLevel
}

fun getMyStatHpuFanLevel(config: MyStatHpuConfiguration): Int {
    var fanLevel = 0
    var fanEnabledStages: Pair<Boolean, Boolean> = Pair(first = false, second = false)

    if ((config.universalOut1.enabled && config.universalOut1Association.associationVal == MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal)
        || (config.universalOut2.enabled && config.universalOut2Association.associationVal == MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal)) {
        return MsFanConstants.LOW_HIGH // All options are enabled due to analog fan speed
    }

    val relays = config.getRelayEnabledAssociations()
    for ((enabled, association) in relays) {
        if (enabled && association == MyStatHpuRelayMapping.FAN_LOW_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(first = true)
        }
        if (enabled && association == MyStatHpuRelayMapping.FAN_HIGH_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(second = true)
        }
    }

    if (fanEnabledStages.first) fanLevel += MsFanConstants.LOW
    if (fanEnabledStages.second) fanLevel += MsFanConstants.HIGH
    if (fanLevel == 0 && (config.isAnyRelayEnabledAssociated(association = MyStatHpuRelayMapping.FAN_ENABLED.ordinal)
                || config.isAnyRelayEnabledAssociated(association = MyStatHpuRelayMapping.OCCUPIED_ENABLED.ordinal))) {
        fanLevel = MsFanConstants.AUTO
    }

    return fanLevel
}

fun getMyStatCpuFanLevel(config: MyStatCpuConfiguration): Int {
    var fanLevel = 0
    var fanEnabledStages: Pair<Boolean, Boolean> = Pair(first = false, second = false)

    if ((config.universalOut1.enabled && (config.universalOut1Association.associationVal == MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal
                || config.universalOut1Association.associationVal == MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal))
        || (config.universalOut2.enabled && (config.universalOut2Association.associationVal == MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal
                || config.universalOut2Association.associationVal == MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal))
    ) {
        return MsFanConstants.LOW_HIGH // All options are enabled due to analog fan speed
    }

    val relays = config.getRelayEnabledAssociations()
    for ((enabled, association) in relays) {

        if (enabled && association == MyStatCpuRelayMapping.FAN_LOW_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(first = true)
        }
        if (enabled && association == MyStatCpuRelayMapping.FAN_HIGH_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(second = true)
        }
    }

    if (fanEnabledStages.first) fanLevel += MsFanConstants.LOW
    if (fanEnabledStages.second) fanLevel += MsFanConstants.HIGH

    if (fanLevel == 0 && (config.isAnyRelayEnabledAssociated(association = MyStatCpuRelayMapping.FAN_ENABLED.ordinal)
                || config.isAnyRelayEnabledAssociated(association = MyStatCpuRelayMapping.OCCUPIED_ENABLED.ordinal))) {
        fanLevel = MsFanConstants.AUTO
    }

    return fanLevel
}

fun setConditioningMode(config: MyStatCpuConfiguration, equip: MyStatEquip) {

    val possible = getMyStatPossibleConditionMode(config)
    var newMode = StandaloneConditioningMode.OFF
    if (possible == PossibleConditioningMode.BOTH) newMode = StandaloneConditioningMode.AUTO
    if (possible == PossibleConditioningMode.HEATONLY) newMode =
        StandaloneConditioningMode.HEAT_ONLY
    if (possible == PossibleConditioningMode.COOLONLY) newMode =
        StandaloneConditioningMode.COOL_ONLY
    equip.conditioningMode.writePointValue(newMode.ordinal.toDouble())
}

fun updateConditioningMode(config: MyStatConfiguration, equip: MyStatEquip) {

    val currentMode =
        StandaloneConditioningMode.values()[equip.conditioningMode.readPriorityVal().toInt()]
    val possibleMode = getMyStatPossibleConditionMode(config)

    if (possibleMode == PossibleConditioningMode.OFF) {
        equip.conditioningMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
        return
    }

    if (currentMode == StandaloneConditioningMode.AUTO && (possibleMode == PossibleConditioningMode.HEATONLY || possibleMode == PossibleConditioningMode.COOLONLY)) {
        equip.conditioningMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
        return
    }

    if (currentMode == StandaloneConditioningMode.HEAT_ONLY && possibleMode == PossibleConditioningMode.COOLONLY) {
        equip.conditioningMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
        return
    }

    if (currentMode == StandaloneConditioningMode.COOL_ONLY && possibleMode == PossibleConditioningMode.HEATONLY) {
        equip.conditioningMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
        return
    }
}


fun getMyStatSelectedFanMode(fanLevel: Int, selectedFan: Int): Int {
    try {
        if (selectedFan == MsFanConstants.OFF) return MyStatFanStages.OFF.ordinal // No fan stages are selected so only off can present here
        if (selectedFan == MsFanConstants.AUTO) return MyStatFanStages.AUTO.ordinal // No fan stages are selected so only off can present here
        CcuLog.i(
            L.TAG_CCU_MSHST, "getMyStatSelectedFanMode: fanLevel $fanLevel selectedFan $selectedFan"
        )
        return (when (fanLevel) {
            MsFanConstants.LOW_HIGH, MsFanConstants.LOW -> {
                MyStatFanStages.values()[selectedFan]
            }

            MsFanConstants.HIGH -> {
                when (selectedFan) {
                    in listOf(2, 3, 4) -> MyStatFanStages.values()[selectedFan + 3]
                    in listOf(
                        MyStatFanStages.HIGH_CUR_OCC.ordinal,
                        MyStatFanStages.HIGH_OCC.ordinal,
                        MyStatFanStages.HIGH_ALL_TIME.ordinal
                    ) -> MyStatFanStages.values()[selectedFan - 3]

                    else -> MyStatFanStages.OFF
                }
            }

            else -> MyStatFanStages.OFF

            /*
            DO NOT DELETE THIS CODE HOLDING FOR REFERENCE
            21 -> MyStatFanStages.values()[selectedFan] // All options are available so selected from the list
            6 -> if (selectedFan in 1..4) MyStatFanStages.values()[selectedFan] else MyStatFanStages.OFF // Only fan low are selected
            7 -> if (selectedFan in listOf(4, 8)) MyStatFanStages.OFF else MyStatFanStages.values()[selectedFan - 3] // Only fan medium are selected
            8 -> if (selectedFan in listOf(7, 11)) MyStatFanStages.OFF else MyStatFanStages.values()[selectedFan - 6] // Only fan high are selected
            13 -> MyStatFanStages.values()[selectedFan] // Fan low & mediums are selected
            15 -> MyStatFanStages.values()[selectedFan - 3] // Medium & high fan speeds are selected
            14 -> (if (selectedFan < 5) MyStatFanStages.values()[selectedFan] else MyStatFanStages.values()[selectedFan - 3]) //low high selected
            else -> MyStatFanStages.OFF
            */
        }).ordinal
    } catch (e: ArrayIndexOutOfBoundsException) {
        CcuLog.e(L.TAG_CCU_MSHST, "Error getSelectedFan function ${e.localizedMessage}", e)
    }
    return MyStatFanStages.OFF.ordinal
}

fun getMyStatPossibleFanModeSettings(fanLevel: Int): MyStatPossibleFanMode {
    return when (fanLevel) {
        MsFanConstants.LOW -> MyStatPossibleFanMode.LOW
        MsFanConstants.HIGH -> MyStatPossibleFanMode.HIGH
        MsFanConstants.LOW_HIGH -> MyStatPossibleFanMode.LOW_HIGH
        MsFanConstants.AUTO -> MyStatPossibleFanMode.AUTO
        else -> MyStatPossibleFanMode.OFF
    }
}

fun getMyStatSelectedConditioningMode(
    configuration: MyStatConfiguration, actualConditioningMode: Int
): Int {
    if (actualConditioningMode == 0) return StandaloneConditioningMode.OFF.ordinal
    return if (getMyStatPossibleConditionMode(configuration) == PossibleConditioningMode.BOTH) StandaloneConditioningMode.values()[actualConditioningMode].ordinal
    else 1 // always it will be 1 because possibility is Off,CoolOnly | Off,Heatonly
}

fun getMyStatActualConditioningMode(
    config: MyStatConfiguration, selectedConditioningMode: Int
): Int {
    if (selectedConditioningMode == 0) return StandaloneConditioningMode.OFF.ordinal
    return when (getMyStatPossibleConditionMode(config)) {
        PossibleConditioningMode.BOTH -> StandaloneConditioningMode.values()[selectedConditioningMode].ordinal
        PossibleConditioningMode.COOLONLY -> StandaloneConditioningMode.COOL_ONLY.ordinal
        PossibleConditioningMode.HEATONLY -> StandaloneConditioningMode.HEAT_ONLY.ordinal
        PossibleConditioningMode.OFF -> StandaloneConditioningMode.values()[selectedConditioningMode].ordinal

    }
}


fun getMyStatPossibleConditionMode(config: MyStatConfiguration): PossibleConditioningMode {
    var cooling = false
    var heating = false
    when (config) {
        is MyStatCpuConfiguration -> {
            cooling = config.isCoolingAvailable()
            heating = config.isHeatingAvailable()
        }

        is MyStatPipe4Configuration -> {
            cooling = config.isCoolingAvailable()
            heating = config.isHeatingAvailable()
        }

        is MyStatPipe2Configuration, is MyStatHpuConfiguration -> return PossibleConditioningMode.BOTH
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


fun getMyStatModelByEquipRef(equipRef: String): ModelDirective? {
    return when (Domain.getDomainEquip(equipRef) as MyStatEquip) {
        is MyStatCpuEquip -> ModelLoader.getMyStatCpuModel()
        is MyStatHpuEquip -> ModelLoader.getMyStatHpuModel()
        is MyStatPipe2Equip -> ModelLoader.getMyStatPipe2Model()
        is MyStatPipe4Equip -> ModelLoader.getMyStatPipe4Model()
        else -> null
    }
}

fun isMyStatLowUserIntentFanMode(fanOpMode: Point): Boolean {
    val fanMode = fanOpMode.readPriorityVal().toInt()
    return fanMode == MyStatFanStages.LOW_CUR_OCC.ordinal
            || fanMode == MyStatFanStages.LOW_OCC.ordinal
            || fanMode == MyStatFanStages.LOW_ALL_TIME.ordinal
}

fun isMyStatHighUserIntentFanMode(fanOpMode: Point): Boolean {
    val fanMode = fanOpMode.readPriorityVal().toInt()
    return fanMode == MyStatFanStages.HIGH_CUR_OCC.ordinal
            || fanMode == MyStatFanStages.HIGH_OCC.ordinal
            || fanMode == MyStatFanStages.HIGH_ALL_TIME.ordinal
}

fun logMsResults(config: MyStatConfiguration, tag: String, logicalPointsList: HashMap<Port, String>) {

    val haystack = CCUHsApi.getInstance()
    listOf(
        Triple(config.relay1Enabled.enabled, config.relay1Association.associationVal, Port.RELAY_ONE),
        Triple(config.relay2Enabled.enabled, config.relay2Association.associationVal, Port.RELAY_TWO),
        Triple(config.relay3Enabled.enabled, config.relay3Association.associationVal, Port.RELAY_THREE),
        Triple(config.universalOut1.enabled, config.universalOut1Association.associationVal, Port.UNIVERSAL_OUT_ONE), // just for printing purpose
        Triple(config.universalOut2.enabled, config.universalOut2Association.associationVal, Port.UNIVERSAL_OUT_TWO)  // just for printing purpose
    ).forEach { (enabled, association, port) ->
        if (enabled) {
            if(config.isRelayConfig(association) && logicalPointsList.containsKey(port)) {
                when (config) {
                    is MyStatCpuConfiguration -> CcuLog.d(
                        tag,
                        "$port = ${MyStatCpuRelayMapping.values()[association]} : ${
                            haystack.readHisValById(
                                logicalPointsList[port]!!
                            )
                        }"
                    )

                    is MyStatPipe2Configuration -> CcuLog.d(
                        tag,
                        "$port = ${MyStatPipe2RelayMapping.values()[association]} : ${
                            haystack.readHisValById(
                                logicalPointsList[port]!!
                            )
                        }"
                    )

                    is MyStatHpuConfiguration -> CcuLog.d(
                        tag,
                        "$port = ${MyStatHpuRelayMapping.values()[association]} : ${
                            haystack.readHisValById(
                                logicalPointsList[port]!!
                            )
                        }"
                    )
                    is MyStatPipe4Configuration -> CcuLog.d(
                        tag,
                        "$port = ${MyStatPipe4RelayMapping.values()[association]} : ${
                            haystack.readHisValById(
                                logicalPointsList[port]!!
                            )
                        }"
                    )
                }
            } else {
                if (logicalPointsList.containsKey(port)) {
                    val mapping = when (config) {
                        is MyStatCpuConfiguration -> MyStatCpuAnalogOutMapping.values()[association]
                        is MyStatHpuConfiguration -> MyStatHpuAnalogOutMapping.values()[association]
                        is MyStatPipe2Configuration -> MyStatPipe2AnalogOutMapping.values()[association]
                        is MyStatPipe4Configuration -> MyStatPipe4AnalogOutMapping.values()[association]
                        else -> null
                    }
                    var analogOutValue = 0.0
                    if (logicalPointsList.containsKey(port)) {
                        analogOutValue = haystack.readHisValById(logicalPointsList[port]!!)
                    }
                    mapping?.let {
                        CcuLog.d(tag, "$port = $it : $analogOutValue")
                    }
                }
            }
        }
    }
}

fun getMyStatDomainEquipByEquipRef(equipRef: String): MyStatEquip {
    return when (val equip = Domain.getDomainEquip(equipRef) as MyStatEquip) {
        is MyStatCpuEquip -> equip
        is MyStatHpuEquip -> equip
        is MyStatPipe2Equip -> equip
        is MyStatPipe4Equip -> equip
        else -> equip
    }
}

fun getMyStatDomainDeviceByEquipRef(equipRef: String): MyStatDevice {
    val device = Domain.getEquipDevices()
    if (device.containsKey(equipRef)) {
        return device[equipRef] as MyStatDevice
    } else {
        Domain.devices[equipRef] = MyStatDevice(equipRef)
    }
    return device[equipRef] as MyStatDevice
}
