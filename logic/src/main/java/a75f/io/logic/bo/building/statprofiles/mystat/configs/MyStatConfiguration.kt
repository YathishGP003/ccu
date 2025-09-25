package a75f.io.logic.bo.building.statprofiles.mystat.configs

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.StringValueConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType
import a75f.io.logic.bo.building.definitions.ProfileType
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
    lateinit var universalOut1: EnableConfig
    lateinit var universalOut2: EnableConfig

    lateinit var relay1Association: AssociationConfig
    lateinit var relay2Association: AssociationConfig
    lateinit var relay3Association: AssociationConfig
    lateinit var universalOut1Association: AssociationConfig
    lateinit var universalOut2Association: AssociationConfig

    lateinit var universalIn1Enabled: EnableConfig
    lateinit var universalIn1Association: AssociationConfig

    lateinit var co2DamperOpeningRate: ValueConfig
    lateinit var co2Threshold: ValueConfig
    lateinit var co2Target: ValueConfig
    lateinit var enableCo2Display: EnableConfig

    lateinit var installerPinEnable: EnableConfig
    lateinit var conditioningModePinEnable: EnableConfig

    lateinit var installerPassword: StringValueConfig
    lateinit var conditioningModePassword: StringValueConfig

    lateinit var desiredTemp: EnableConfig
    lateinit var spaceTemp: EnableConfig

    abstract fun getActiveConfiguration(): MyStatConfiguration
    fun isRelayConfig(association: Int) = association < getAnalogStartIndex()
    abstract fun getAnalogStartIndex(): Int
    override fun getEnableConfigs(): List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(autoForceOccupied)
            add(autoAway)

            add(relay1Enabled)
            add(relay2Enabled)
            add(relay3Enabled)
            add(universalOut1)
            add(universalOut2)
            add(universalIn1Enabled)
            add(enableCo2Display)
            add(enableCo2Display)
            add(installerPinEnable)
            add(conditioningModePinEnable)

            add(desiredTemp)
            add(spaceTemp)
        }
    }

    override fun getStringConfigs(): List<StringValueConfig> {
        return mutableListOf<StringValueConfig>().apply {
            add(installerPassword)
            add(conditioningModePassword)
        }
    }

    override fun getAssociationConfigs(): List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
            add(relay1Association)
            add(relay2Association)
            add(relay3Association)
            add(universalOut1Association)
            add(universalOut2Association)
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

        installerPinEnable.enabled = equip.installerPinEnable.readDefaultVal() > 0.0
        conditioningModePinEnable.enabled =
            equip.enableConditioningModeFanAccess.readDefaultVal() > 0.0
        desiredTemp.enabled = equip.enableDesiredTempDisplay.readDefaultVal() > 0.0
        spaceTemp.enabled = equip.enableSpaceTempDisplay.readDefaultVal() > 0.0

        installerPassword.currentVal = if (equip.pinLockInstallerAccess.pointExists()) {
            equip.pinLockInstallerAccess.readDefaultStrVal()
        } else {
            "0" // If the point does not exist, return an empty string
        }
        conditioningModePassword.currentVal =
            if (equip.pinLockConditioningModeFanAccess.pointExists()) {
                equip.pinLockConditioningModeFanAccess.readDefaultStrVal()
            } else {
                "0" // If the point does not exist, return an empty string
            }
    }

    private fun myStatConfiguration(equip: MyStatEquip) {
        relay1Enabled.enabled = equip.relay1OutputEnable.readDefaultVal() == 1.0
        relay2Enabled.enabled = equip.relay2OutputEnable.readDefaultVal() == 1.0
        relay3Enabled.enabled = equip.relay3OutputEnable.readDefaultVal() == 1.0
        universalOut1.enabled = equip.universalOut1Enable.readDefaultVal() == 1.0
        universalOut2.enabled = equip.universalOut2Enable.readDefaultVal() == 1.0
        if (relay1Enabled.enabled) relay1Association.associationVal =
            equip.relay1OutputAssociation.readDefaultVal().toInt()
        if (relay2Enabled.enabled) relay2Association.associationVal =
            equip.relay2OutputAssociation.readDefaultVal().toInt()
        if (relay3Enabled.enabled) relay3Association.associationVal =
            equip.relay3OutputAssociation.readDefaultVal().toInt()
        if (universalOut1.enabled) universalOut1Association.associationVal =
            equip.universalOut1Association.readDefaultVal().toInt()
        if (universalOut2.enabled) universalOut2Association.associationVal =
            equip.universalOut2Association.readDefaultVal().toInt()

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
        // Pin enable configs
        installerPinEnable = getDefaultEnableConfig(DomainName.enableInstallerAccess, model)
        conditioningModePinEnable = getDefaultEnableConfig(DomainName.enableConditioningModeFanAccess, model)

        desiredTemp = getDefaultEnableConfig(DomainName.enableDesiredTempDisplay, model)
        spaceTemp = getDefaultEnableConfig(DomainName.enableSpaceTempDisplay, model)

        installerPassword = getDefaultStringConfig(DomainName.pinLockInstallerAccess, model)
        conditioningModePassword = getDefaultStringConfig(DomainName.pinLockConditioningModeFanAccess, model)
        relay1Enabled = getDefaultEnableConfig(DomainName.relay1OutputEnable, model)
        relay2Enabled = getDefaultEnableConfig(DomainName.relay2OutputEnable, model)
        relay3Enabled = getDefaultEnableConfig(DomainName.relay3OutputEnable, model)
        universalOut1 = getDefaultEnableConfig(DomainName.universal1OutputEnable, model)
        universalOut2 = getDefaultEnableConfig(DomainName.universal2OutputEnable, model)

        relay1Association = getDefaultAssociationConfig(DomainName.relay1OutputAssociation, model)
        relay2Association = getDefaultAssociationConfig(DomainName.relay2OutputAssociation, model)
        relay3Association = getDefaultAssociationConfig(DomainName.relay3OutputAssociation, model)
        universalOut1Association = getDefaultAssociationConfig(DomainName.universal1OutputAssociation, model)
        universalOut2Association = getDefaultAssociationConfig(DomainName.universal2OutputAssociation, model)

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

    abstract fun getRelayMap(): Map<String, Boolean>

    abstract fun getAnalogMap(): Map<String, Pair<Boolean, String>>

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
        SUPPLY_AIR_TEMPERATURE,
        GENERIC_ALARM_NO,
        GENERIC_ALARM_NC,
        KEY_CARD_SENSOR,
        DOOR_WINDOW_SENSOR_NC_TITLE24,
        DOOR_WINDOW_SENSOR_TITLE24,
        FAN_RUN_SENSOR_NO,
        FAN_RUN_SENSOR_NC
    }
}

data class MyStatFanConfig(val low: ValueConfig, val high: ValueConfig)
