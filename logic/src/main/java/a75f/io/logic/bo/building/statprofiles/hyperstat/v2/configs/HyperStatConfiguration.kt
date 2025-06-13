package a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.hyperstat.Pipe2V2Equip
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType
import a75f.io.logic.bo.building.statprofiles.util.getHyperStatDevice
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created by Manjunath K on 26-09-2024.
 */

abstract class HyperStatConfiguration(
        nodeAddress: Int, nodeType: String, priority: Int, roomRef: String, floorRef: String, profileType: ProfileType, val model: SeventyFiveFProfileDirective)
    : ProfileConfiguration(nodeAddress = nodeAddress, nodeType = nodeType, priority = priority, roomRef = roomRef, floorRef = floorRef, profileType = profileType.name) {

    lateinit var temperatureOffset: ValueConfig
    lateinit var autoForceOccupied: EnableConfig
    lateinit var autoAway: EnableConfig

    lateinit var relay1Enabled: EnableConfig
    lateinit var relay2Enabled: EnableConfig
    lateinit var relay3Enabled: EnableConfig
    lateinit var relay4Enabled: EnableConfig
    lateinit var relay5Enabled: EnableConfig
    lateinit var relay6Enabled: EnableConfig

    lateinit var relay1Association: AssociationConfig
    lateinit var relay2Association: AssociationConfig
    lateinit var relay3Association: AssociationConfig
    lateinit var relay4Association: AssociationConfig
    lateinit var relay5Association: AssociationConfig
    lateinit var relay6Association: AssociationConfig

    lateinit var analogOut1Enabled: EnableConfig
    lateinit var analogOut2Enabled: EnableConfig
    lateinit var analogOut3Enabled: EnableConfig

    lateinit var analogOut1Association: AssociationConfig
    lateinit var analogOut2Association: AssociationConfig
    lateinit var analogOut3Association: AssociationConfig

    lateinit var analogIn1Enabled: EnableConfig
    lateinit var analogIn2Enabled: EnableConfig

    lateinit var analogIn1Association: AssociationConfig
    lateinit var analogIn2Association: AssociationConfig

    lateinit var thermistor1Enabled: EnableConfig
    lateinit var thermistor2Enabled: EnableConfig

    lateinit var thermistor1Association: AssociationConfig
    lateinit var thermistor2Association: AssociationConfig

    lateinit var zoneCO2DamperOpeningRate: ValueConfig
    lateinit var zoneCO2Threshold: ValueConfig
    lateinit var zoneCO2Target: ValueConfig

    lateinit var zonePM2p5Threshold: ValueConfig
    lateinit var zonePM2p5Target: ValueConfig

    lateinit var zonePM10Target: ValueConfig

    lateinit var displayHumidity: EnableConfig
    lateinit var displayCO2: EnableConfig
    lateinit var displayPM2p5: EnableConfig

    lateinit var disableTouch : EnableConfig
    lateinit var enableBrightness : EnableConfig

    abstract fun getActiveConfiguration(): HyperStatConfiguration

    override fun getEnableConfigs(): List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(autoForceOccupied)
            add(autoAway)

            add(relay1Enabled)
            add(relay2Enabled)
            add(relay3Enabled)
            add(relay4Enabled)
            add(relay5Enabled)
            add(relay6Enabled)

            add(analogOut1Enabled)
            add(analogOut2Enabled)
            add(analogOut3Enabled)

            add(analogIn1Enabled)
            add(analogIn2Enabled)

            add(thermistor1Enabled)
            add(thermistor2Enabled)

            add(displayHumidity)
            add(displayCO2)
            add(displayPM2p5)

            add(disableTouch)
            add(enableBrightness)

        }
    }

    override fun getAssociationConfigs(): List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
            add(relay1Association)
            add(relay2Association)
            add(relay3Association)
            add(relay4Association)
            add(relay5Association)
            add(relay6Association)

            add(analogOut1Association)
            add(analogOut2Association)
            add(analogOut3Association)

            add(analogIn1Association)
            add(analogIn2Association)

            add(thermistor1Association)
            add(thermistor2Association)

        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(temperatureOffset)
            add(zoneCO2DamperOpeningRate)
            add(zoneCO2Threshold)
            add(zoneCO2Target)
            add(zonePM2p5Threshold)
            add(zonePM2p5Target)
            add(zonePM10Target)
        }
    }

    private fun baseConfigs(equip: HyperStatEquip) {
        temperatureOffset.currentVal = equip.temperatureOffset.readDefaultVal()
        autoForceOccupied.enabled = equip.autoForceOccupied.readDefaultVal() == 1.0
        autoAway.enabled = equip.autoAway.readDefaultVal() == 1.0
        displayHumidity.enabled = equip.enableHumidityDisplay.readDefaultVal() == 1.0
        displayCO2.enabled = equip.enableCo2Display.readDefaultVal() == 1.0
        displayPM2p5.enabled = equip.enablePm25Display.readDefaultVal() == 1.0
        zoneCO2DamperOpeningRate.currentVal = equip.co2DamperOpeningRate.readDefaultVal()
        zoneCO2Threshold.currentVal = equip.co2Threshold.readDefaultVal()
        zoneCO2Target.currentVal = equip.co2Target.readDefaultVal()
        zonePM2p5Target.currentVal = equip.pm25Target.readDefaultVal()
        disableTouch.enabled = equip.disableTouch.readDefaultVal() == 1.0
        enableBrightness.enabled = equip.enableBrightness.readDefaultVal() == 1.0
    }

    private fun relayConfiguration(equip: HyperStatEquip) {
        relay1Enabled.enabled = equip.relay1OutputEnable.readDefaultVal() == 1.0
        relay2Enabled.enabled = equip.relay2OutputEnable.readDefaultVal() == 1.0
        relay3Enabled.enabled = equip.relay3OutputEnable.readDefaultVal() == 1.0
        relay4Enabled.enabled = equip.relay4OutputEnable.readDefaultVal() == 1.0
        relay5Enabled.enabled = equip.relay5OutputEnable.readDefaultVal() == 1.0
        relay6Enabled.enabled = equip.relay6OutputEnable.readDefaultVal() == 1.0
        if (relay1Enabled.enabled) relay1Association.associationVal = equip.relay1OutputAssociation.readDefaultVal().toInt()
        if (relay2Enabled.enabled) relay2Association.associationVal = equip.relay2OutputAssociation.readDefaultVal().toInt()
        if (relay3Enabled.enabled) relay3Association.associationVal = equip.relay3OutputAssociation.readDefaultVal().toInt()
        if (relay4Enabled.enabled) relay4Association.associationVal = equip.relay4OutputAssociation.readDefaultVal().toInt()
        if (relay5Enabled.enabled) relay5Association.associationVal = equip.relay5OutputAssociation.readDefaultVal().toInt()
        if (relay6Enabled.enabled) relay6Association.associationVal = equip.relay6OutputAssociation.readDefaultVal().toInt()
    }

    private fun analogOutConfiguration(equip: HyperStatEquip) {
        analogOut1Enabled.enabled = equip.analog1OutputEnable.readDefaultVal() == 1.0
        analogOut2Enabled.enabled = equip.analog2OutputEnable.readDefaultVal() == 1.0
        analogOut3Enabled.enabled = equip.analog3OutputEnable.readDefaultVal() == 1.0

        if (analogOut1Enabled.enabled) analogOut1Association.associationVal = equip.analog1OutputAssociation.readDefaultVal().toInt()
        if (analogOut2Enabled.enabled) analogOut2Association.associationVal = equip.analog2OutputAssociation.readDefaultVal().toInt()
        if (analogOut3Enabled.enabled) analogOut3Association.associationVal = equip.analog3OutputAssociation.readDefaultVal().toInt()
    }

    private fun analogInConfiguration(equip: HyperStatEquip) {
        analogIn1Enabled.enabled = equip.analog1InputEnable.readDefaultVal() == 1.0
        analogIn2Enabled.enabled = equip.analog2InputEnable.readDefaultVal() == 1.0

        if (analogIn1Enabled.enabled) analogIn1Association.associationVal = equip.analog1InputAssociation.readDefaultVal().toInt()
        if (analogIn2Enabled.enabled) analogIn2Association.associationVal = equip.analog2InputAssociation.readDefaultVal().toInt()
    }

    private fun thermistorConfiguration(equip: HyperStatEquip) {
        thermistor1Enabled.enabled = equip.thermistor1InputEnable.readDefaultVal() == 1.0
        thermistor2Enabled.enabled = equip.thermistor2InputEnable.readDefaultVal() == 1.0

        if (thermistor1Enabled.enabled) thermistor1Association.associationVal = equip.thermistor1InputAssociation.readDefaultVal().toInt()
        if (equip !is Pipe2V2Equip) {
            if (thermistor2Enabled.enabled) thermistor2Association.associationVal = equip.thermistor2InputAssociation.readDefaultVal().toInt()
        }
    }

    fun getActiveConfiguration(equip: HyperStatEquip): HyperStatConfiguration {
        baseConfigs(equip)
        relayConfiguration(equip)
        analogOutConfiguration(equip)
        analogInConfiguration(equip)
        thermistorConfiguration(equip)
        isDefault = false
        return this
    }

    open fun getDefaultConfiguration(): HyperStatConfiguration {
        temperatureOffset = getDefaultValConfig(DomainName.temperatureOffset, model)
        autoForceOccupied = getDefaultEnableConfig(DomainName.autoForceOccupied, model)
        autoAway = getDefaultEnableConfig(DomainName.autoAway, model)

        relay1Enabled = getDefaultEnableConfig(DomainName.relay1OutputEnable, model)
        relay2Enabled = getDefaultEnableConfig(DomainName.relay2OutputEnable, model)
        relay3Enabled = getDefaultEnableConfig(DomainName.relay3OutputEnable, model)
        relay4Enabled = getDefaultEnableConfig(DomainName.relay4OutputEnable, model)
        relay5Enabled = getDefaultEnableConfig(DomainName.relay5OutputEnable, model)
        relay6Enabled = getDefaultEnableConfig(DomainName.relay6OutputEnable, model)

        relay1Association = getDefaultAssociationConfig(DomainName.relay1OutputAssociation, model)
        relay2Association = getDefaultAssociationConfig(DomainName.relay2OutputAssociation, model)
        relay3Association = getDefaultAssociationConfig(DomainName.relay3OutputAssociation, model)
        relay4Association = getDefaultAssociationConfig(DomainName.relay4OutputAssociation, model)
        relay5Association = getDefaultAssociationConfig(DomainName.relay5OutputAssociation, model)
        relay6Association = getDefaultAssociationConfig(DomainName.relay6OutputAssociation, model)

        analogOut1Enabled = getDefaultEnableConfig(DomainName.analog1OutputEnable, model)
        analogOut2Enabled = getDefaultEnableConfig(DomainName.analog2OutputEnable, model)
        analogOut3Enabled = getDefaultEnableConfig(DomainName.analog3OutputEnable, model)

        analogOut1Association = getDefaultAssociationConfig(DomainName.analog1OutputAssociation, model)
        analogOut2Association = getDefaultAssociationConfig(DomainName.analog2OutputAssociation, model)
        analogOut3Association = getDefaultAssociationConfig(DomainName.analog3OutputAssociation, model)

        thermistor1Enabled = getDefaultEnableConfig(DomainName.thermistor1InputEnable, model)
        thermistor2Enabled = getDefaultEnableConfig(DomainName.thermistor2InputEnable, model)

        thermistor1Association = getDefaultAssociationConfig(DomainName.thermistor1InputAssociation, model)
        thermistor2Association = getDefaultAssociationConfig(DomainName.thermistor2InputAssociation, model)

        analogIn1Enabled = getDefaultEnableConfig(DomainName.analog1InputEnable, model)
        analogIn2Enabled = getDefaultEnableConfig(DomainName.analog2InputEnable, model)

        analogIn1Association = getDefaultAssociationConfig(DomainName.analog1InputAssociation, model)
        analogIn2Association = getDefaultAssociationConfig(DomainName.analog2InputAssociation, model)

        zoneCO2DamperOpeningRate = getDefaultValConfig(DomainName.co2DamperOpeningRate, model)
        zoneCO2Threshold = getDefaultValConfig(DomainName.co2Threshold, model)
        zoneCO2Target = getDefaultValConfig(DomainName.co2Target, model)

        zonePM2p5Threshold = getDefaultValConfig(DomainName.pm25Threshold, model)
        zonePM2p5Target = getDefaultValConfig(DomainName.pm25Target, model)

        zonePM10Target = getDefaultValConfig(DomainName.pm10Target, model)

        displayHumidity = getDefaultEnableConfig(DomainName.enableHumidityDisplay, model)
        displayCO2 = getDefaultEnableConfig(DomainName.enableCO2Display, model)
        displayPM2p5 = getDefaultEnableConfig(DomainName.enablePm25Display, model)

        disableTouch = getDefaultEnableConfig(DomainName.disableTouch, model)
        enableBrightness = getDefaultEnableConfig(DomainName.enableBrightness, model)

        isDefault = true
        return this
    }

    // Utility functions

    fun getRelayEnabledAssociations(): List<Pair<Boolean, Int>> {
        return mutableListOf<Pair<Boolean, Int>>().apply {
            if (relay1Enabled.enabled) add(Pair(true, relay1Association.associationVal))
            if (relay2Enabled.enabled) add(Pair(true, relay2Association.associationVal))
            if (relay3Enabled.enabled) add(Pair(true, relay3Association.associationVal))
            if (relay4Enabled.enabled) add(Pair(true, relay4Association.associationVal))
            if (relay5Enabled.enabled) add(Pair(true, relay5Association.associationVal))
            if (relay6Enabled.enabled) add(Pair(true, relay6Association.associationVal))
        }
    }

    private fun getAnalogOutEnabledAssociations(): List<Pair<Boolean, Int>> {
        return mutableListOf<Pair<Boolean, Int>>().apply {
            if (analogOut1Enabled.enabled) add(Pair(true, analogOut1Association.associationVal))
            if (analogOut2Enabled.enabled) add(Pair(true, analogOut2Association.associationVal))
            if (analogOut3Enabled.enabled) add(Pair(true, analogOut3Association.associationVal))
        }
    }

    fun isAnyRelayEnabledAssociated(relays: List<Pair<Boolean, Int>> = emptyList(), association: Int): Boolean {
        fun isMapped(list: List<Pair<Boolean, Int>>) = list.any { it.first && it.second == association }
        if (relays.isNotEmpty()) return isMapped(relays)
        return isMapped(getRelayEnabledAssociations())
    }

    fun isAnyAnalogOutEnabledAssociated(analogOuts: List<Pair<Boolean, Int>> = emptyList(), association: Int): Boolean {
        fun isMapped(list: List<Pair<Boolean, Int>>) = list.any { it.first && it.second == association }
        if (analogOuts.isNotEmpty()) return isMapped(analogOuts)
        return isMapped(getAnalogOutEnabledAssociations())
    }

    private fun availableHighestStages(stage1: Int, stage2: Int, stage3: Int): Triple<Boolean, Boolean, Boolean> {
        var isStage1Selected = false
        var isStage2Selected = false
        var isStage3Selected = false

        getRelayEnabledAssociations().forEach { (enabled, associated) ->
            if (enabled) {
                if (associated == stage1) isStage1Selected = true
                if (associated == stage2) isStage2Selected = true
                if (associated == stage3) isStage3Selected = true
            }
        }
        return Triple(isStage1Selected, isStage2Selected, isStage3Selected)
    }

    protected fun getHighestStage(stage1: Int, stage2: Int, stage3: Int): Int {
        val availableStages = availableHighestStages(stage1, stage2, stage3)
        return if (availableStages.third) stage3
        else if (availableStages.second) stage2
        else if (availableStages.first) stage1
        else -1
    }

    protected fun getLowestStage(stage1: Int, stage2: Int, stage3: Int): Int {
        val availableStages = availableHighestStages(stage1, stage2, stage3)
        return if (availableStages.first) stage1
        else if (availableStages.second) stage2
        else if (availableStages.third) stage3
        else -1
    }

    fun isEnabledAndAssociated(enableConfig: EnableConfig, association: AssociationConfig, mapping: Int) = isEnabledAndAssociated(enableConfig.enabled, association.associationVal, mapping)

    private fun isEnabledAndAssociated(enabled: Boolean, association: Int, mapping: Int) = enabled && association == mapping

    abstract fun getRelayMap(): Map<String, Boolean>

    abstract fun getAnalogMap(): Map<String, Pair<Boolean, String>>

    fun setPortConfiguration(nodeAddress: Int, relays: Map<String, Boolean>, analogOuts: Map<String, Pair<Boolean, String>>) {

        val hayStack = CCUHsApi.getInstance()
        val device = getHyperStatDevice(nodeAddress)
        val deviceRef = device[Tags.ID].toString()

        fun getPort(portName: String): RawPoint.Builder? {
            val port = hayStack.readHDict("point and deviceRef == \"$deviceRef\" and domainName == \"$portName\"")
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
            if (port != null) updatePort(port, OutputRelayActuatorType.NormallyOpen.displayName, externallyMapped)
        }

        analogOuts.forEach { (analogName, config) ->
            val port = getPort(analogName)
            if (port != null) {
                updatePort(port, config.second, config.first)
            }
        }
    }
}

