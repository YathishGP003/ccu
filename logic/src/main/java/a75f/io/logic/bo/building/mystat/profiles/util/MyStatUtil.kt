package a75f.io.logic.bo.building.mystat.profiles.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.devices.MyStatDevice
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.MyStatFanStages
import a75f.io.logic.bo.building.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.mystat.configs.MyStatCpuAnalogOutMapping
import a75f.io.logic.bo.building.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.mystat.configs.MyStatCpuRelayMapping
import a75f.io.logic.bo.building.mystat.configs.MyStatHpuAnalogOutMapping
import a75f.io.logic.bo.building.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.mystat.configs.MyStatHpuRelayMapping
import a75f.io.logic.bo.building.mystat.configs.MyStatPipe2AnalogOutMapping
import a75f.io.logic.bo.building.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.mystat.configs.MyStatPipe2RelayMapping
import a75f.io.logic.tuners.TunerUtil
import android.util.Log
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import org.projecthaystack.UnknownRecException

/**
 * Created by Manjunath K on 16-01-2025.
 */

const val OFF = 0
const val AUTO = 1
const val LOW = 6
const val HIGH = 8
const val LOW_HIGH = 14

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
        CCUHsApi.getInstance().writeHisValById(pointId, value)
    } else {
        CcuLog.i(L.TAG_CCU_MSHST, "updateLogicalPointIdValue: But point id is null !!")
    }
}

fun updateAllLoopOutput(
    equip: MyStatEquip,
    coolingLoop: Int,
    heatingLoop: Int,
    fanLoop: Int,
    isHpuProfile: Boolean = false,
    compressorLoop: Int = 0
) {
    equip.apply {
        coolingLoopOutput.writePointValue(coolingLoop.toDouble())
        heatingLoopOutput.writePointValue(heatingLoop.toDouble())
        fanLoopOutput.writePointValue(fanLoop.toDouble())
        if (isHpuProfile) {
            (equip as MyStatHpuEquip).compressorLoopOutput.writePointValue(compressorLoop.toDouble())
        }
    }
}

fun putPointToMap(point: Point, outputPointMap: HashMap<Int, String>, mapping: Int) {
    try {
        if (point.pointExists()) outputPointMap[mapping] = point.id
    } catch (e: UnknownRecException) {
        CcuLog.e(L.TAG_CCU_MSHST, "logical point not found ${point.domainName}", e)
    }

}

fun getMyStatLogicalPointList(
    equip: MyStatEquip,
    config: MyStatConfiguration
): HashMap<Port, String> {

    val device = Domain.getEquipDevices()[equip.equipRef] as MyStatDevice
    val logicalPoints = HashMap<Port, String>()

    if (device.relay1.readPoint().pointRef != null && device.relay1.readPoint().pointRef.isNotEmpty()) {
        if (config.relay1Enabled.enabled) logicalPoints[Port.RELAY_ONE] =
            device.relay1.readPoint().pointRef
    }
    if (device.relay2.readPoint().pointRef != null && device.relay2.readPoint().pointRef.isNotEmpty()) {
        if (config.relay2Enabled.enabled) logicalPoints[Port.RELAY_TWO] =
            device.relay2.readPoint().pointRef
    }
    if (device.relay3.readPoint().pointRef != null && device.relay3.readPoint().pointRef.isNotEmpty()) {
        if (config.relay3Enabled.enabled) logicalPoints[Port.RELAY_THREE] =
            device.relay3.readPoint().pointRef
    }
    if (device.relay4.readPoint().pointRef != null && device.relay4.readPoint().pointRef.isNotEmpty()) {
        if (config.relay4Enabled.enabled) logicalPoints[Port.RELAY_FOUR] =
            device.relay4.readPoint().pointRef
    }

    if (device.analog1Out.readPoint().pointRef != null && device.analog1Out.readPoint().pointRef.isNotEmpty()) {
        if (config.analogOut1Enabled.enabled) logicalPoints[Port.ANALOG_OUT_ONE] =
            device.analog1Out.readPoint().pointRef
    }

    CcuLog.d(L.TAG_CCU_MSHST, "Logical Points: for ${equip.equipRef} $logicalPoints")
    return logicalPoints
}

fun fetchMyStatTuners(equip: MyStatEquip): MyStatTuners {

    /**
     * Consider that
     * proportionalGain = proportionalKFactor
     * integralGain = integralKFactor
     * proportionalSpread = temperatureProportionalRange
     * integralMaxTimeout = temperatureIntegralTime
     */

    // These are generic tuners
    val myStatTuners = MyStatTuners()
    myStatTuners.proportionalGain = TunerUtil.getProportionalGain(equip.equipRef)
    myStatTuners.integralGain = TunerUtil.getIntegralGain(equip.equipRef)
    myStatTuners.proportionalSpread = TunerUtil.getProportionalSpread(equip.equipRef)
    myStatTuners.integralMaxTimeout = TunerUtil.getIntegralTimeout(equip.equipRef).toInt()
    myStatTuners.humidityHysteresis = TunerUtil.getHysteresisPoint(
        "domainName ==\"${DomainName.standaloneHumidityHysteresis}\"",
        equip.equipRef
    ).toInt()
    myStatTuners.relayActivationHysteresis = TunerUtil.getHysteresisPoint(
        "domainName ==\"${DomainName.standaloneRelayActivationHysteresis}\"",
        equip.equipRef
    ).toInt()
    myStatTuners.analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery(
        "domainName ==\"${DomainName.standaloneAnalogFanSpeedMultiplier}\"",
        equip.equipRef
    )

    // These are specific tuners
    when (equip) {
        is MyStatCpuEquip -> {
            myStatTuners.minFanRuntimePostConditioning = TunerUtil.readTunerValByQuery(
                "domainName ==\"${DomainName.minFanRuntimePostConditioning}\"",
                equip.equipRef
            ).toInt()
        }

        is MyStatHpuEquip -> {
            myStatTuners.auxHeating1Activate = TunerUtil.readTunerValByQuery(
                "domainName ==\"${DomainName.mystatAuxHeating1Activate}\"",
                equip.equipRef
            )
        }

        is MyStatPipe2Equip -> {
            myStatTuners.auxHeating1Activate = TunerUtil.readTunerValByQuery(
                "domainName ==\"${DomainName.mystatAuxHeating1Activate}\"",
                equip.equipRef
            )
            myStatTuners.heatingThreshold = TunerUtil.readTunerValByQuery(
                "domainName ==\"${DomainName.mystatPipe2FancoilHeatingThreshold}\"",
                equip.equipRef
            )
            myStatTuners.coolingThreshold = TunerUtil.readTunerValByQuery(
                "domainName ==\"${DomainName.mystatPipe2FancoilCoolingThreshold}\"",
                equip.equipRef
            )
            myStatTuners.waterValveSamplingOnTime = TunerUtil.readTunerValByQuery(
                "domainName ==\"${DomainName.mystatWaterValveSamplingOnTime}\"",
                equip.equipRef
            ).toInt()
            myStatTuners.waterValveSamplingWaitTime = TunerUtil.readTunerValByQuery(
                "domainName ==\"${DomainName.mystatWaterValveSamplingWaitTime}\"",
                equip.equipRef
            ).toInt()
            myStatTuners.waterValveSamplingDuringLoopDeadbandOnTime = TunerUtil.readTunerValByQuery(
                "domainName ==\"${DomainName.mystatWaterValveSamplingLoopDeadbandOnTime}\"",
                equip.equipRef
            ).toInt()
            myStatTuners.waterValveSamplingDuringLoopDeadbandWaitTime =
                TunerUtil.readTunerValByQuery(
                    "domainName ==\"${DomainName.mystatWaterValveSamplingLoopDeadbandWaitTime}\"",
                    equip.equipRef
                ).toInt()
        }
    }
    return myStatTuners
}

fun fetchMyStatUserIntents(equip: MyStatEquip): MyStatUserIntents {
    return MyStatUserIntents(
        currentTemp = equip.currentTemp.readHisVal(),
        zoneCoolingTargetTemperature = equip.desiredTempCooling.readPriorityVal(),
        zoneHeatingTargetTemperature = equip.desiredTempHeating.readPriorityVal(),
        targetMinInsideHumidity = equip.targetHumidifier.readPriorityVal(),
        targetMaxInsideHumidity = equip.targetDehumidifier.readPriorityVal()
    )
}

fun fetchMyStatBasicSettings(equip: MyStatEquip) = MyStatBasicSettings(
    conditioningMode = StandaloneConditioningMode.values()[equip.conditioningMode.readPriorityVal()
        .toInt()], fanMode = MyStatFanStages.values()[equip.fanOpMode.readPriorityVal().toInt()]
)

fun milliToMin(milliseconds: Long) = (milliseconds / (1000 * 60) % 60)

fun getMyStatFanLevel(config: MyStatConfiguration): Int {
    when (config) {
        is MyStatCpuConfiguration -> return getMyStatCpuFanLevel(config)
        is MyStatPipe2Configuration -> return getMyStatPipe2FanLevel(config)
        is MyStatHpuConfiguration -> return getMyStatHpuFanLevel(config)
    }
    return -1
}

fun getMyStatPipe2FanLevel(config: MyStatPipe2Configuration): Int {
    var fanLevel = 0
    var fanEnabledStages: Pair<Boolean, Boolean> = Pair(first = false, second = false)

    if (config.analogOut1Enabled.enabled && config.analogOut1Association.associationVal == MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal) {
        return LOW_HIGH // All options are enabled due to analog fan speed
    }

    val relays = config.getRelayEnabledAssociations()
    for ((enabled, association) in relays) {
        if (enabled && association == MyStatPipe2RelayMapping.FAN_LOW_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(first = true)
        }
        if (enabled && association == MyStatPipe2RelayMapping.FAN_HIGH_SPEED.ordinal) {
            fanEnabledStages = fanEnabledStages.copy(second = true)
        }
    }

    if (fanEnabledStages.first) fanLevel += LOW
    if (fanEnabledStages.second) fanLevel += HIGH
    return fanLevel
}

fun getMyStatHpuFanLevel(config: MyStatHpuConfiguration): Int {
    var fanLevel = 0
    var fanEnabledStages: Pair<Boolean, Boolean> = Pair(first = false, second = false)

    if (config.analogOut1Enabled.enabled && config.analogOut1Association.associationVal == MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal) {
        return LOW_HIGH // All options are enabled due to analog fan speed
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

    if (fanEnabledStages.first) fanLevel += LOW
    if (fanEnabledStages.second) fanLevel += HIGH
    return fanLevel
}

fun setConditioningMode(config: MyStatCpuConfiguration, equip: MyStatEquip) {

    val possible = getMyStatPossibleConditionMode(config)
    var newMode = StandaloneConditioningMode.OFF
    if (possible == MyStatPossibleConditioningMode.BOTH) newMode = StandaloneConditioningMode.AUTO
    if (possible == MyStatPossibleConditioningMode.HEAT_ONLY) newMode = StandaloneConditioningMode.HEAT_ONLY
    if (possible == MyStatPossibleConditioningMode.COOL_ONLY) newMode = StandaloneConditioningMode.COOL_ONLY
    equip.conditioningMode.writePointValue(newMode.ordinal.toDouble())
}

fun updateConditioningMode(config: MyStatCpuConfiguration, equip: MyStatEquip) {

    val currentMode =
        StandaloneConditioningMode.values()[equip.conditioningMode.readPriorityVal().toInt()]
    val possibleMode = getMyStatPossibleConditionMode(config)

    if (possibleMode == MyStatPossibleConditioningMode.OFF) {
        equip.conditioningMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
        return
    }

    if (currentMode == StandaloneConditioningMode.AUTO &&
        (possibleMode == MyStatPossibleConditioningMode.HEAT_ONLY
                || possibleMode == MyStatPossibleConditioningMode.COOL_ONLY)
        ) {
        equip.conditioningMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
        return
    }

    if (currentMode == StandaloneConditioningMode.HEAT_ONLY && possibleMode == MyStatPossibleConditioningMode.COOL_ONLY) {
        equip.conditioningMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
        return
    }

    if (currentMode == StandaloneConditioningMode.COOL_ONLY && possibleMode == MyStatPossibleConditioningMode.HEAT_ONLY) {
        equip.conditioningMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
        return
    }
}

fun getMyStatCpuFanLevel(config: MyStatCpuConfiguration): Int {
    var fanLevel = 0
    var fanEnabledStages: Pair<Boolean, Boolean> = Pair(first = false, second = false)

    if (config.analogOut1Enabled.enabled && (config.analogOut1Association.associationVal == MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal ||
                config.analogOut1Association.associationVal == MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal)) {
        return LOW_HIGH // All options are enabled due to analog fan speed
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

    if (fanEnabledStages.first) fanLevel += LOW
    if (fanEnabledStages.second) fanLevel += HIGH
    return fanLevel
}

fun getMyStatSelectedFanMode(fanLevel: Int, selectedFan: Int): Int {
    try {
        if (selectedFan == OFF) return MyStatFanStages.OFF.ordinal // No fan stages are selected so only off can present here
        if (selectedFan == AUTO) return MyStatFanStages.AUTO.ordinal // No fan stages are selected so only off can present here
        Log.i(
            L.TAG_CCU_MSHST, "getMyStatSelectedFanMode: fanLevel $fanLevel selectedFan $selectedFan"
        )
        return (when (fanLevel) {
            LOW_HIGH, LOW -> {
                MyStatFanStages.values()[selectedFan]
            }
            HIGH -> {
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
        LOW -> MyStatPossibleFanMode.LOW
        HIGH -> MyStatPossibleFanMode.HIGH
        LOW_HIGH -> MyStatPossibleFanMode.LOW_HIGH
        else -> MyStatPossibleFanMode.OFF
    }
}

fun getMyStatSelectedConditioningMode(
    configuration: MyStatConfiguration,
    actualConditioningMode: Int
): Int {
    if (actualConditioningMode == 0) return StandaloneConditioningMode.OFF.ordinal
    return if (getMyStatPossibleConditionMode(configuration) == MyStatPossibleConditioningMode.BOTH) StandaloneConditioningMode.values()[actualConditioningMode].ordinal
    else 1 // always it will be 1 because possibility is Off,CoolOnly | Off,Heatonly
}

fun getMyStatActualConditioningMode(
    config: MyStatConfiguration,
    selectedConditioningMode: Int
): Int {
    if (selectedConditioningMode == 0) return StandaloneConditioningMode.OFF.ordinal
    return when (getMyStatPossibleConditionMode(config)) {
        MyStatPossibleConditioningMode.BOTH -> StandaloneConditioningMode.values()[selectedConditioningMode].ordinal
        MyStatPossibleConditioningMode.COOL_ONLY -> StandaloneConditioningMode.COOL_ONLY.ordinal
        MyStatPossibleConditioningMode.HEAT_ONLY -> StandaloneConditioningMode.HEAT_ONLY.ordinal
        MyStatPossibleConditioningMode.OFF -> StandaloneConditioningMode.values()[selectedConditioningMode].ordinal

    }
}


fun getMyStatPossibleConditionMode(config: MyStatConfiguration): MyStatPossibleConditioningMode {
    var cooling = false
    var heating = false
    when (config) {
        is MyStatCpuConfiguration -> {
            cooling = config.isCoolingAvailable()
            heating = config.isHeatingAvailable()
        }

        is MyStatPipe2Configuration, is MyStatHpuConfiguration -> return MyStatPossibleConditioningMode.BOTH
    }

    return if (cooling && heating) {
        MyStatPossibleConditioningMode.BOTH
    } else if (cooling) {
        MyStatPossibleConditioningMode.COOL_ONLY
    } else if (heating) {
        MyStatPossibleConditioningMode.HEAT_ONLY
    } else {
        MyStatPossibleConditioningMode.OFF
    }
}

enum class MyStatPossibleConditioningMode {
    OFF, COOL_ONLY, HEAT_ONLY, BOTH
}

enum class MyStatPossibleFanMode {
    OFF, LOW, HIGH, LOW_HIGH
}

fun getMyStatModelByEquipRef(equipRef: String): ModelDirective? {
    return when (Domain.getDomainEquip(equipRef) as MyStatEquip) {
        is MyStatCpuEquip -> ModelLoader.getMyStatCpuModel()
        is MyStatHpuEquip -> ModelLoader.getMyStatHpuModel()
        is MyStatPipe2Equip -> ModelLoader.getMyStatPipe2Model()
        else -> null
    }
}
