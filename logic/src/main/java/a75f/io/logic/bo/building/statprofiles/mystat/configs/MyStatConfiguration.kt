package a75f.io.logic.bo.building.statprofiles.mystat.configs

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.devices.MyStatDevice
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.domain.equips.mystat.MyStatPipe4Equip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.StatProfileConfiguration
import a75f.io.logic.bo.building.statprofiles.util.MyStatDeviceType
import a75f.io.logic.bo.building.statprofiles.util.getMyStatDevice
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import org.projecthaystack.HDict

/**
 * Created by Manjunath K on 16-01-2025.
 */

abstract class MyStatConfiguration(
    nodeAddress: Int,
    nodeType: String,
    priority: Int,
    roomRef: String,
    floorRef: String,
    profileType: ProfileType,
    model: SeventyFiveFProfileDirective
) : StatProfileConfiguration(
    nodeAddress = nodeAddress,
    nodeType = nodeType,
    priority = priority,
    roomRef = roomRef,
    floorRef = floorRef,
    profileType = profileType,
    model
) {
    abstract fun getActiveConfiguration(): MyStatConfiguration

    abstract fun getAnalogStartIndex(): Int

    fun isRelayConfig(association: Int) = association < getAnalogStartIndex()

    override fun getDefaultConfiguration(): MyStatConfiguration {
     val configuration = super.getDefaultConfiguration() as MyStatConfiguration
     configuration.universal1InAssociation = if (this is MyStatPipe2Configuration) {
         AssociationConfig(DomainName.universalIn1Association, 0)
     } else {
         getDefaultAssociationConfig(DomainName.universalIn1Association, model)
     }
     isDefault = true
     return configuration
 }

    fun universalInUnit(deviceRef: String) {
        val unit: String
        if (this is MyStatPipe2Configuration) {
            unit = "kΩ"
        } else {
            val association = this.universal1InAssociation.associationVal
            unit = when (UniversalMapping.values()[association]) {
                UniversalMapping.AN_KEY_CARD_SENSOR -> "mV"
                UniversalMapping.AN_DOOR_WINDOW_SENSOR_TITLE24 -> "mV"
                else -> "kΩ"
            }
        }
        val device = MyStatDevice(deviceRef)
        if (device.universal1In.readPoint().unit != unit) {
            val rawPoint = RawPoint.Builder().setHDict(
                Domain.hayStack.readHDictById(device.universal1In.readPoint().id)
            ).setUnit(unit).build()
            Domain.hayStack.updatePoint(rawPoint, rawPoint.id)
            CcuLog.d(L.TAG_CCU_MSHST, "universal in unit updated to $unit")
        }
        mapOf(device.universalOut1 to this.universalOut1Association, device.universalOut2 to this.universalOut2Association)
            .map { port ->
                RawPoint.Builder()
                    .setHDict(Domain.hayStack.readHDictById(port.key.readPoint().id))
                    .setUnit(if (this.isRelayConfig(port.value.associationVal).not()) "dV" else "")
                    .build()
            }
            .forEach { point ->
                Domain.hayStack.updatePoint(point, point.id)
            }
    }

    fun updateEnumConfigs(equip: MyStatEquip, deviceType: String) {
        if (deviceType.equals(MyStatDeviceType.MYSTAT_V2.name, true)) return
        fun updateEnum(enum: String, point: Point) {
            val dbPoint = a75f.io.api.haystack.Point.Builder()
                .setHDict(Domain.hayStack.readHDictById(point.getPoint()["id"].toString()))
                .setEnums(enum).build()
            Domain.hayStack.updatePoint(dbPoint, dbPoint.id)
        }
        var relayEnum = ""
        var analogEnum = ""
        when(equip) {
            is MyStatCpuEquip -> {
                relayEnum = "Cooling Stage 1=0,Cooling Stage 2=1,Heating Stage 1=2,Heating Stage 2=3,Fan Low Speed=4,Fan High Speed=5,Fan Enable=6,Occupied Enable=7,Humidifier=8,Dehumidifier=9,Externally Mapped - Relay=10,DCV Damper=11,NA=12,NA=13,NA=14,NA=15,NA=16,NA=17"
                analogEnum = "NA=0,NA=1,NA=2,NA=3,NA=4,NA=5,NA=6,NA=7,NA=8,NA=9,NA=10,NA=11,Cooling=12,Linear Fan Speed=13,Heating=14,Staged Fan Speed=15,Externally Mapped - Analog=16,DCV Modulating Damper=17"
            }
            is MyStatHpuEquip -> {
                relayEnum = "Compressor Stage 1=0,Compressor Stage 2=1,Aux Heating=2,Fan Low Speed=3,Fan High Speed=4,Fan Enable=5,Occupied Enable=6,Humidifier=7,Dehumidifier=8,O - Energize in Cooling=9,B - Energize in Heating=10,Externally Mapped - Relay=11,DCV Damper=12,NA=13,NA=14,NA=15,NA=16"
                analogEnum = "NA=0,NA=1,NA=2,NA=3,NA=4,NA=5,NA=6,NA=7,NA=8,NA=9,NA=10,NA=11,NA=12,Compressor Speed=13,Fan Speed=14,Externally Mapped - Analog=15,DCV Modulating Damper=16"
            }
            is MyStatPipe2Equip -> {
                relayEnum = "Fan Low Speed=0,Fan High Speed=1,Aux Heating=2,Water Valve=3,Fan Enable=4,Occupied Enable=5,Humidifier=6,Dehumidifier=7,Externally Mapped - Relay=8,DCV Damper=9,Fan Low Speed - Ventilation=10,NA=11,NA=12,NA=13,NA=14"
                analogEnum = "NA=0,NA=1,NA=2,NA=3,NA=4,NA=5,NA=6,NA=7,NA=8,NA=9,NA=10,Water Modulating Valve=11,Fan Speed=12,Externally Mapped - Analog=13,DCV Modulating Damper=14"
            }
            is MyStatPipe4Equip -> {
                relayEnum = "Fan Low Speed=0,Fan High Speed=1,Aux Heating=2,Cooling Water Valve=3,Heating Water Valve=4,Fan Enable=5,Occupied Enable=6,Humidifier=7,Dehumidifier=8,Externally Mapped - Relay=9,DCV Damper=10,Fan Low Speed - Ventilation=11,NA=12,NA=13,NA=14,NA=15,NA"
                analogEnum = "NA=0,NA=1,NA=2,NA=3,NA=4,NA=5,NA=6,NA=7,NA=8,NA=9,DCV Damper=10,NA=11,Cooling Modulating Valve=12,Heating Modulating Valve=13,Fan Speed=14,Externally Mapped - Analog=15,DCV Modulating Damper=16"
            }
        }

        if (equip.universalOut2Association.pointExists() && relayEnum.isNotEmpty()) {
            updateEnum(relayEnum, equip.universalOut2Association)
        }
        if (equip.universalOut1Association.pointExists() && analogEnum.isNotEmpty()) {
            updateEnum(analogEnum, equip.universalOut1Association)
        }
    }

    fun setPortConfiguration(
        nodeAddress: Int,
        relays: Map<String, Boolean>,
        analogOuts: Map<String, Pair<Boolean, String>>
    ) {

        val hayStack = CCUHsApi.getInstance()
        val device = getMyStatDevice(nodeAddress)
        val deviceRef = device[Tags.ID].toString()

        fun getPortDict(portName: String): HDict? {
            return hayStack.readHDict("point and deviceRef == \"$deviceRef\" and domainName == \"$portName\"")
        }

        fun updatePort(portDict: HDict, type: String, isWritable: Boolean) {
            val port = RawPoint.Builder().setHDict(portDict)
            port.setType(type)
            if (isWritable) {
                port.addMarker(Tags.WRITABLE)
                port.addMarker(Tags.UNUSED)
            } else if(portDict.has(Tags.UNUSED)) {
                port.removeMarkerIfExists(Tags.WRITABLE)
                port.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(port.build().id)
            }
            val buildPoint = port.build()
            hayStack.updatePoint(buildPoint, buildPoint.id)
        }

        relays.forEach { (relayName, externallyMapped) ->
            val portDict = getPortDict(relayName)
            if (portDict != null && !portDict.isEmpty) {
                updatePort(
                    portDict, OutputRelayActuatorType.NormallyOpen.displayName, externallyMapped
                )
            }
        }

        analogOuts.forEach { (analogName, config) ->
            val portDict = getPortDict(analogName)
            if (portDict != null && !portDict.isEmpty) {
                updatePort(portDict, config.second, config.first)
            }
        }
    }

    fun getRelayEnabledAssociations(): List<Pair<Boolean, Int>> {
        return mutableListOf<Pair<Boolean, Int>>().apply {
            if (relay1Enabled.enabled) add(Pair(true, relay1Association.associationVal))
            if (relay2Enabled.enabled) add(Pair(true, relay2Association.associationVal))
            if (relay3Enabled.enabled) add(Pair(true, relay3Association.associationVal))
            if (universalOut1.enabled) add(Pair(true, universalOut1Association.associationVal))
            if (universalOut2.enabled) add(Pair(true, universalOut2Association.associationVal))
        }
    }

    abstract fun getHighestFanStageCount(): Int

    fun isAnyRelayEnabledAssociated(relays: List<Pair<Boolean, Int>> = emptyList(), association: Int): Boolean {
        fun isMapped(list: List<Pair<Boolean, Int>>) = list.any { it.first && it.second == association }
        if (relays.isNotEmpty()) return isMapped(relays)
        return isMapped(getRelayEnabledAssociations())
    }

    fun isAnalogEnabledAssociated(association: Int): Boolean {
        return (universalOut1.enabled && universalOut1Association.associationVal == association ||
                universalOut2.enabled && universalOut2Association.associationVal == association)
    }

    private fun availableHighestStages(stage1: Int, stage2: Int): Pair<Boolean, Boolean> {
        var isStage1Selected = false
        var isStage2Selected = false

        getRelayEnabledAssociations().forEach { (enabled, associated) ->
            if (enabled) {
                if (associated == stage1) isStage1Selected = true
                if (associated == stage2) isStage2Selected = true
            }
        }
        return Pair(isStage1Selected, isStage2Selected)
    }

    protected fun getHighestStage(stage1: Int, stage2: Int): Int {
        val availableStages = availableHighestStages(stage1, stage2)
        return if (availableStages.second) stage2
        else if (availableStages.first) stage1
        else -1
    }

    protected fun getLowestStage(stage1: Int, stage2: Int): Int {
        val availableStages = availableHighestStages(stage1, stage2)
        return if (availableStages.first) stage1
        else if (availableStages.second) stage2
        else -1
    }

    enum class UniversalMapping {
        TH_SUPPLY_AIR_TEMPERATURE,
        TH_GENERIC_ALARM_NOO,
        TH_GENERIC_ALARM_NC,
        AN_KEY_CARD_SENSOR,
        TH_DOOR_WINDOW_SENSOR_NC_TITLE24,
        AN_DOOR_WINDOW_SENSOR_TITLE24,
        TH_FAN_RUN_SENSOR_NO,
        TH_FAN_RUN_SENSOR_NC,
        TH_DOOR_WINDOW_SENSOR_NC,
        TH_DOOR_WINDOW_SENSOR_NO,
        TH_KEY_CARD_SENSOR_NO,
        TH_KEY_CARD_SENSOR_NC,
        TH_CHILLED_WATER_SUPPLY_TEMP,
        TH_HOT_WATER_SUPPLY_TEMP,
        AI_CURRENT_TX_10,
        AI_CURRENT_TX_20,
        AI_CURRENT_TX_50,
        AI_GENERIC_VOLTAGE_INPUT,
        AI_GENERIC_THERMISTOR_INPUT
    }


    fun getAnalogOutDefaultValueForMyStatV1(config: MyStatConfiguration, devicesVersion: String) {
        if(devicesVersion == MyStatDeviceType.MYSTAT_V2.name) return

        if (!config.universalOut1.enabled) {
            config.universalOut1Association.associationVal = when (config) {
                is MyStatCpuConfiguration -> {
                    MyStatCpuAnalogOutMapping.COOLING.ordinal
                }

                is MyStatHpuConfiguration -> {
                    MyStatHpuAnalogOutMapping.COMPRESSOR_SPEED.ordinal
                }

                is MyStatPipe2Configuration -> {
                    MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal
                }
                is MyStatPipe4Configuration ->{
                    MyStatPipe4AnalogOutMapping.CHILLED_MODULATING_VALUE.ordinal
                }

                else -> 0
            }
        }
    }
}

data class MyStatFanConfig(val low: ValueConfig, val high: ValueConfig)
