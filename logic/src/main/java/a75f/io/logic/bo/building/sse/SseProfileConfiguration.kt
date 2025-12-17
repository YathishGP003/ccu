package a75f.io.logic.bo.building.sse

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.Point
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.devices.HyperStatSplitDevice
import a75f.io.domain.equips.SseEquip
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.haystack.device.DeviceUtil
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

open class SseProfileConfiguration(
    nodeAddress: Int, nodeType: String, priority: Int, roomRef: String,
    floorRef: String, profileType: ProfileType, val model: SeventyFiveFProfileDirective
) : ProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType.name) {

    lateinit var temperatureOffset: ValueConfig
    lateinit var relay1EnabledState: EnableConfig
    lateinit var relay2EnabledState: EnableConfig
    lateinit var th1EnabledState: EnableConfig
    lateinit var th2EnabledState: EnableConfig
    lateinit var autoForcedOccupiedEnabledState: EnableConfig
    lateinit var autoAwayEnabledState: EnableConfig
    lateinit var analog1InEnabledState: EnableConfig

    lateinit var relay1Association: AssociationConfig
    lateinit var relay2Association: AssociationConfig
    lateinit var analog1InAssociation: AssociationConfig

    lateinit var unusedPorts: HashMap<String, Boolean>

    open fun getDefaultConfiguration(): SseProfileConfiguration {
        temperatureOffset = getDefaultValConfig(DomainName.temperatureOffset, model)
        relay1EnabledState = getDefaultEnableConfig(DomainName.relay1OutputEnable, model)
        relay2EnabledState = getDefaultEnableConfig(DomainName.relay2OutputEnable, model)
        th1EnabledState = getDefaultEnableConfig(DomainName.thermistor1InputEnable, model)
        th2EnabledState = getDefaultEnableConfig(DomainName.thermistor2InputEnable, model)
        autoForcedOccupiedEnabledState = getDefaultEnableConfig(DomainName.autoForceOccupied, model)
        autoAwayEnabledState = getDefaultEnableConfig(DomainName.autoAway, model)
        analog1InEnabledState = getDefaultEnableConfig(DomainName.analog1InputEnable, model)

        relay1Association = getDefaultAssociationConfig(DomainName.relay1OutputAssociation, model)
        relay2Association = getDefaultAssociationConfig(DomainName.relay2OutputAssociation, model)
        analog1InAssociation =
            getDefaultAssociationConfig(DomainName.analog1InputAssociation, model)

        unusedPorts = hashMapOf()
        isDefault = true
        return this
    }

    open fun getActiveConfiguration(): SseProfileConfiguration {

        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val sseEquip = SseEquip(equip[Tags.ID].toString())
        getDefaultConfiguration()
        temperatureOffset.currentVal = sseEquip.temperatureOffset.readDefaultVal()
        relay1EnabledState.enabled = sseEquip.relay1OutputState.readDefaultVal() > 0
        relay2EnabledState.enabled = sseEquip.relay2OutputState.readDefaultVal() > 0
        th1EnabledState.enabled = sseEquip.thermistor1InputEnable.readDefaultVal() > 0
        th2EnabledState.enabled = sseEquip.thermistor2InputEnable.readDefaultVal() > 0
        autoForcedOccupiedEnabledState.enabled = sseEquip.autoForceOccupied.readDefaultVal() > 0
        autoAwayEnabledState.enabled = sseEquip.autoAway.readDefaultVal() > 0
        analog1InEnabledState.enabled = sseEquip.analog1InputEnable.readDefaultVal() > 0

        relay1Association.associationVal = if (relay1EnabledState.enabled) {
            sseEquip.relay1OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay1OutputAssociation, model).associationVal
        }

        relay2Association.associationVal = if (relay2EnabledState.enabled) {
            sseEquip.relay2OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay2OutputAssociation, model).associationVal
        }

        analog1InAssociation.associationVal = if(analog1InEnabledState.enabled){
            sseEquip.analog1InputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.analog1InputAssociation, model).associationVal
        }
        val devicePorts = DeviceUtil.getUnusedPortsForDevice(nodeAddress.toShort(), Domain.hayStack)
        devicePorts?.forEach { disabledPort ->
            unusedPorts[disabledPort.displayName] = disabledPort.markers.contains(a75f.io.api.haystack.Tags.UNUSED)
        }

        isDefault = false
        return this
    }

    override fun getDependencies(): List<ValueConfig> {
        TODO("Not yet implemented")
    }

    override fun getAssociationConfigs(): List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
            add(relay1Association)
            add(relay2Association)
            add(analog1InAssociation)
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(temperatureOffset)
        }
    }

    override fun getEnableConfigs(): List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(relay1EnabledState)
            add(relay2EnabledState)
            add(th1EnabledState)
            add(th2EnabledState)
            add(autoForcedOccupiedEnabledState)
            add(autoAwayEnabledState)
            add(analog1InEnabledState)
        }
    }

    fun getRelayLogicalPhysicalMap(equipId: String): Map<Point, PhysicalPoint> {
        val map: MutableMap<Point, PhysicalPoint> = HashMap()
        val equip = SseEquip(equipId)
        Domain.hayStack.readId("device and equipRef == \"${equipId}\"")?.let { deviceId ->
            map[equip.relay1OutputState] = PhysicalPoint(DomainName.relay1, deviceId)
            map[equip.relay2OutputState] = PhysicalPoint(DomainName.relay2, deviceId)
        }
        return map
    }

    fun getAnalogOutLogicalPhysicalMap(equipId: String): Map<Point, PhysicalPoint> {
        return mutableMapOf()
    }
}