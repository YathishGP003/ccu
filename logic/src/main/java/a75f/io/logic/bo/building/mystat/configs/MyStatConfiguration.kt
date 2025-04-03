package a75f.io.logic.bo.building.mystat.configs

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.mystat.profiles.util.getMyStatDevice
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

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
    val model: SeventyFiveFProfileDirective
) : ProfileConfiguration(
    nodeAddress = nodeAddress,
    nodeType = nodeType,
    priority = priority,
    roomRef = roomRef,
    floorRef = floorRef,
    profileType = profileType.name
) {

    lateinit var temperatureOffset: ValueConfig
    lateinit var autoForceOccupied: EnableConfig
    lateinit var autoAway: EnableConfig

    lateinit var relay1Enabled: EnableConfig
    lateinit var relay2Enabled: EnableConfig
    lateinit var relay3Enabled: EnableConfig
    lateinit var relay4Enabled: EnableConfig

    lateinit var relay1Association: AssociationConfig
    lateinit var relay2Association: AssociationConfig
    lateinit var relay3Association: AssociationConfig
    lateinit var relay4Association: AssociationConfig

    lateinit var analogOut1Enabled: EnableConfig
    lateinit var analogOut1Association: AssociationConfig

    lateinit var universalIn1Enabled: EnableConfig
    lateinit var universalIn1Association: AssociationConfig

    lateinit var co2DamperOpeningRate: ValueConfig
    lateinit var co2Threshold: ValueConfig
    lateinit var co2Target: ValueConfig
    lateinit var enableCo2Display: EnableConfig

    abstract fun getActiveConfiguration(): MyStatConfiguration

    override fun getEnableConfigs(): List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(autoForceOccupied)
            add(autoAway)

            add(relay1Enabled)
            add(relay2Enabled)
            add(relay3Enabled)
            add(relay4Enabled)

            add(analogOut1Enabled)
            add(universalIn1Enabled)
            add(enableCo2Display)
            add(enableCo2Display)
        }
    }

    override fun getAssociationConfigs(): List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
            add(relay1Association)
            add(relay2Association)
            add(relay3Association)
            add(relay4Association)
            add(analogOut1Association)
            add(universalIn1Association)
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(temperatureOffset)
            add(co2DamperOpeningRate)
            add(co2Threshold)
            add(co2Target)
        }
    }

    private fun baseConfigs(equip: MyStatEquip) {
        temperatureOffset.currentVal = equip.temperatureOffset.readDefaultVal()
        co2Target.currentVal = equip.co2Target.readDefaultVal()
        co2Threshold.currentVal = equip.co2Threshold.readDefaultVal()
        co2DamperOpeningRate.currentVal = equip.co2DamperOpeningRate.readDefaultVal()
        autoForceOccupied.enabled = equip.autoForceOccupied.readDefaultVal() == 1.0
        autoAway.enabled = equip.autoAway.readDefaultVal() == 1.0

        enableCo2Display.enabled = equip.enableCo2Display.readDefaultVal() == 1.0
    }

    private fun myStatConfiguration(equip: MyStatEquip) {
        relay1Enabled.enabled = equip.relay1OutputEnable.readDefaultVal() == 1.0
        relay2Enabled.enabled = equip.relay2OutputEnable.readDefaultVal() == 1.0
        relay3Enabled.enabled = equip.relay3OutputEnable.readDefaultVal() == 1.0
        relay4Enabled.enabled = equip.relay4OutputEnable.readDefaultVal() == 1.0
        if (relay1Enabled.enabled) relay1Association.associationVal =
            equip.relay1OutputAssociation.readDefaultVal().toInt()
        if (relay2Enabled.enabled) relay2Association.associationVal =
            equip.relay2OutputAssociation.readDefaultVal().toInt()
        if (relay3Enabled.enabled) relay3Association.associationVal =
            equip.relay3OutputAssociation.readDefaultVal().toInt()
        if (relay4Enabled.enabled) relay4Association.associationVal =
            equip.relay4OutputAssociation.readDefaultVal().toInt()

        analogOut1Enabled.enabled = equip.analog1OutputEnable.readDefaultVal() == 1.0
        if (analogOut1Enabled.enabled) analogOut1Association.associationVal =
            equip.analog1OutputAssociation.readDefaultVal().toInt()

        universalIn1Enabled.enabled = equip.universalIn1Enable.readDefaultVal() == 1.0
        if (universalIn1Enabled.enabled) universalIn1Association.associationVal =
            equip.universalIn1Association.readDefaultVal().toInt()

    }

    fun getActiveConfiguration(equip: MyStatEquip): MyStatConfiguration {
        baseConfigs(equip)
        myStatConfiguration(equip)
        isDefault = false
        return this
    }


    open fun getDefaultConfiguration(): MyStatConfiguration {
        temperatureOffset = getDefaultValConfig(DomainName.temperatureOffset, model)
        co2DamperOpeningRate = getDefaultValConfig(DomainName.co2DamperOpeningRate, model)
        co2Threshold = getDefaultValConfig(DomainName.co2Threshold, model)
        co2Target = getDefaultValConfig(DomainName.co2Target, model)
        autoForceOccupied = getDefaultEnableConfig(DomainName.autoForceOccupied, model)
        autoAway = getDefaultEnableConfig(DomainName.autoAway, model)
        enableCo2Display = getDefaultEnableConfig(DomainName.enableCo2Display, model)

        relay1Enabled = getDefaultEnableConfig(DomainName.relay1OutputEnable, model)
        relay2Enabled = getDefaultEnableConfig(DomainName.relay2OutputEnable, model)
        relay3Enabled = getDefaultEnableConfig(DomainName.relay3OutputEnable, model)
        relay4Enabled = getDefaultEnableConfig(DomainName.relay4OutputEnable, model)

        relay1Association = getDefaultAssociationConfig(DomainName.relay1OutputAssociation, model)
        relay2Association = getDefaultAssociationConfig(DomainName.relay2OutputAssociation, model)
        relay3Association = getDefaultAssociationConfig(DomainName.relay3OutputAssociation, model)
        relay4Association = getDefaultAssociationConfig(DomainName.relay4OutputAssociation, model)

        analogOut1Enabled = getDefaultEnableConfig(DomainName.analog1OutputEnable, model)
        analogOut1Association = getDefaultAssociationConfig(DomainName.analog1OutputAssociation, model)

        universalIn1Enabled = getDefaultEnableConfig(DomainName.universalIn1Enable, model)
        universalIn1Association = if (this is MyStatPipe2Configuration) {
            AssociationConfig(DomainName.universalIn1Association,  0)
        } else {
            getDefaultAssociationConfig(DomainName.universalIn1Association, model)
        }

        isDefault = true
        return this
    }

    fun setPortConfiguration(
        nodeAddress: Int,
        relays: Map<String, Boolean>,
        analogOuts: Map<String, Pair<Boolean, String>>
    ) {

        val hayStack = CCUHsApi.getInstance()
        val device = getMyStatDevice(nodeAddress)
        val deviceRef = device[Tags.ID].toString()

        fun getPort(portName: String): RawPoint.Builder? {
            val port =
                hayStack.readHDict("point and deviceRef == \"$deviceRef\" and domainName == \"$portName\"")
            if (port.isEmpty) return null
            return RawPoint.Builder().setHDict(port)
        }

        fun updatePort(port: RawPoint.Builder, type: String, isWritable: Boolean) {
            port.setType(type)
            if (isWritable) {
                port.addMarker(Tags.WRITABLE)
                port.addMarker(Tags.UNUSED)
            } else {
                port.removeMarkerIfExists(Tags.WRITABLE)
                port.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(port.build().id)
            }
            val buildPoint = port.build()
            hayStack.updatePoint(buildPoint, buildPoint.id)
        }

        relays.forEach { (relayName, externallyMapped) ->
            val port = getPort(relayName)
            if (port != null) updatePort(
                port, OutputRelayActuatorType.NormallyOpen.displayName, externallyMapped
            )
        }

        analogOuts.forEach { (analogName, config) ->
            val port = getPort(analogName)
            if (port != null) {
                updatePort(port, config.second, config.first)
            }
        }
    }

    fun getRelayEnabledAssociations(): List<Pair<Boolean, Int>> {
        return mutableListOf<Pair<Boolean, Int>>().apply {
            if (relay1Enabled.enabled) add(Pair(true, relay1Association.associationVal))
            if (relay2Enabled.enabled) add(Pair(true, relay2Association.associationVal))
            if (relay3Enabled.enabled) add(Pair(true, relay3Association.associationVal))
            if (relay4Enabled.enabled) add(Pair(true, relay4Association.associationVal))
        }
    }


    abstract fun getRelayMap(): Map<String, Boolean>

    abstract fun getAnalogMap(): Map<String, Pair<Boolean, String>>

    fun isAnyRelayEnabledAssociated(relays: List<Pair<Boolean, Int>> = emptyList(), association: Int): Boolean {
        fun isMapped(list: List<Pair<Boolean, Int>>) = list.any { it.first && it.second == association }
        if (relays.isNotEmpty()) return isMapped(relays)
        return isMapped(getRelayEnabledAssociations())
    }

    fun isAnalogEnabledAssociated(association: Int) = analogOut1Enabled.enabled && analogOut1Association.associationVal == association

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
        SUPPLY_AIR_TEMPERATURE,
        GENERIC_ALARM_NO,
        GENERIC_ALARM_NC,
        KEY_CARD_SENSOR,
        DOOR_WINDOW_SENSOR_NC_TITLE24,
        DOOR_WINDOW_SENSOR_TITLE24,
    }
}

data class MyStatFanConfig(val low: ValueConfig, val high: ValueConfig)
