package a75f.io.logic.bo.building.dab

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.DabEquip
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.haystack.device.DeviceUtil
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

class DabProfileConfiguration (nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String, profileType : ProfileType, val model : SeventyFiveFProfileDirective)
    : ProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType.name) {

    lateinit var temperatureOffset: ValueConfig
    lateinit var damper1Type: ValueConfig
    lateinit var damper1Size: ValueConfig
    lateinit var damper1Shape: ValueConfig
    lateinit var damper2Type: ValueConfig
    lateinit var damper2Size: ValueConfig
    lateinit var damper2Shape: ValueConfig
    lateinit var reheatType: ValueConfig
    lateinit var zonePriority: ValueConfig


    lateinit var enableCo2Control: EnableConfig
    lateinit var enableIAQControl: EnableConfig
    lateinit var autoForceOccupied: EnableConfig
    lateinit var autoAway: EnableConfig

    lateinit var enableCFMControl: EnableConfig
    lateinit var kFactor: ValueConfig

    lateinit var minCFMForIAQ: ValueConfig
    lateinit var minReheatDamperPos: ValueConfig
    lateinit var maxCoolingDamperPos: ValueConfig
    lateinit var minCoolingDamperPos: ValueConfig
    lateinit var maxHeatingDamperPos: ValueConfig
    lateinit var minHeatingDamperPos: ValueConfig
    lateinit var unusedPorts: HashMap<String, Boolean>

    fun getDefaultConfiguration() : DabProfileConfiguration {
        temperatureOffset = getDefaultValConfig(DomainName.temperatureOffset, model)
        damper1Type = getDefaultValConfig(DomainName.damper1Type, model)
        damper1Size = getDefaultValConfig(DomainName.damper1Size, model)
        damper1Shape = getDefaultValConfig(DomainName.damper1Shape, model)
        damper2Type = getDefaultValConfig(DomainName.damper2Type, model)
        damper2Size = getDefaultValConfig(DomainName.damper2Size, model)
        damper2Shape = getDefaultValConfig(DomainName.damper2Shape, model)
        reheatType = getDefaultValConfig(DomainName.reheatType, model)
        zonePriority = getDefaultValConfig(DomainName.zonePriority, model)

        enableCo2Control = getDefaultEnableConfig(DomainName.enableCo2Control, model)
        enableIAQControl = getDefaultEnableConfig(DomainName.enableIAQControl, model)

        autoForceOccupied = getDefaultEnableConfig(DomainName.autoForceOccupied, model)
        autoAway = getDefaultEnableConfig(DomainName.autoAway, model)
        enableCFMControl = getDefaultEnableConfig(DomainName.enableCFMControl, model)
        kFactor = getDefaultValConfig(DomainName.kFactor, model)

        minCFMForIAQ = getDefaultValConfig(DomainName.minCFMIAQ, model)
        minReheatDamperPos = getDefaultValConfig(DomainName.minReheatDamperPos, model)
        maxCoolingDamperPos = getDefaultValConfig(DomainName.maxCoolingDamperPos, model)
        minCoolingDamperPos = getDefaultValConfig(DomainName.minCoolingDamperPos, model)
        maxHeatingDamperPos = getDefaultValConfig(DomainName.maxHeatingDamperPos, model)
        minHeatingDamperPos = getDefaultValConfig(DomainName.minHeatingDamperPos, model)

        if (L.ccu().bypassDamperProfile != null) {
            minCoolingDamperPos.currentVal = 10.0
            minHeatingDamperPos.currentVal = 10.0
        }
        unusedPorts = hashMapOf()
        isDefault = true

        return this
    }

    fun getActiveConfiguration() : DabProfileConfiguration {
        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val dabEquip = DabEquip(equip[Tags.ID].toString())
        getDefaultConfiguration()

        temperatureOffset.currentVal = dabEquip.temperatureOffset.readDefaultVal()
        damper1Type.currentVal = dabEquip.damper1Type.readDefaultVal()
        damper1Size.currentVal = dabEquip.damper1Size.readDefaultVal()
        damper1Shape.currentVal = dabEquip.damper1Shape.readDefaultVal()
        damper2Type.currentVal = dabEquip.damper2Type.readDefaultVal()
        damper2Size.currentVal = dabEquip.damper2Size.readDefaultVal()
        damper2Shape.currentVal = dabEquip.damper2Shape.readDefaultVal()
        reheatType.currentVal = dabEquip.reheatType.readDefaultVal()
        zonePriority.currentVal = dabEquip.zonePriority.readPriorityVal()
        enableCo2Control.enabled = dabEquip.enableCo2Control.readDefaultVal() > 0
        enableIAQControl.enabled = dabEquip.enableIAQControl.readDefaultVal() > 0
        autoAway.enabled = dabEquip.autoAway.readDefaultVal() > 0
        autoForceOccupied.enabled = dabEquip.autoForceOccupied.readDefaultVal() > 0
        enableCFMControl.enabled = dabEquip.enableCFMControl.readDefaultVal() > 0

        if (enableCFMControl.enabled) {
            kFactor.currentVal = dabEquip.kFactor.readDefaultVal()
            minCFMForIAQ.currentVal = dabEquip.minCFMIAQ.readDefaultVal()
            minReheatDamperPos.currentVal = dabEquip.minReheatDamperPos.readDefaultVal()
            maxHeatingDamperPos.currentVal = dabEquip.maxHeatingDamperPos.readDefaultVal()
            minCoolingDamperPos.currentVal = dabEquip.minCoolingDamperPos.readDefaultVal()
            maxCoolingDamperPos.currentVal = dabEquip.maxCoolingDamperPos.readDefaultVal()
            minHeatingDamperPos.currentVal = dabEquip.minHeatingDamperPos.readDefaultVal()
            if (L.ccu().bypassDamperProfile != null) {
                minCoolingDamperPos.currentVal = 10.0
                minHeatingDamperPos.currentVal = 10.0
            }

        }else{
            kFactor = getDefaultValConfig(DomainName.kFactor, model)
            minCFMForIAQ.currentVal = 100.0
            minReheatDamperPos.currentVal = dabEquip.minReheatDamperPos.readDefaultVal()
            maxHeatingDamperPos.currentVal = dabEquip.maxHeatingDamperPos.readDefaultVal()
            minCoolingDamperPos.currentVal = dabEquip.minCoolingDamperPos.readPriorityVal()
            maxCoolingDamperPos.currentVal = dabEquip.maxCoolingDamperPos.readDefaultVal()
            minHeatingDamperPos.currentVal = dabEquip.minHeatingDamperPos.readPriorityVal()
        }

        isDefault = false
        val devicePorts = DeviceUtil.getUnusedPortsForDevice(nodeAddress.toShort(), Domain.hayStack)
        devicePorts?.forEach { disabledPort ->
            unusedPorts[disabledPort.displayName] = disabledPort.markers.contains(Tags.WRITABLE)
        }
        return this
    }


    override fun getDependencies(): List<ValueConfig> {
        TODO("Not yet implemented")
    }

    override fun getEnableConfigs() : List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(autoAway)
            add(autoForceOccupied)
            add(enableCo2Control)
            add(enableIAQControl)
            add(enableCFMControl)
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(damper1Type)
            add(damper1Size)
            add(damper1Shape)
            add(damper2Type)
            add(damper2Size)
            add(damper2Shape)
            add(reheatType)
            add(zonePriority)
            add(temperatureOffset)
            add(minCFMForIAQ)
            add(minReheatDamperPos)
            add(maxCoolingDamperPos)
            add(minCoolingDamperPos)
            add(maxHeatingDamperPos)
            add(minHeatingDamperPos)
            add(kFactor)
        }
    }

    override fun toString(): String {
        return " temperatureOffset: ${temperatureOffset.currentVal} damper1Type: ${damper1Type.currentVal}  damper1Size: ${damper1Size.currentVal}" +
                " damper1Shape: ${damper1Shape.currentVal} damper2Type: ${damper2Type.currentVal} damper2Size: " +
                "${damper2Size.currentVal} damper2Shape: ${damper2Shape.currentVal} reheatType: ${reheatType.currentVal} zonePriority: ${zonePriority.currentVal}" +
                "enableCo2Control: ${enableCo2Control.enabled} enableIAQControl: ${enableIAQControl.enabled}" +
                " autoForceOccupied: ${autoForceOccupied.enabled} autoAway: ${autoAway.enabled} enableCFMControl: " +
                " ${enableCFMControl.enabled} minReheatDamperPos: ${minReheatDamperPos.currentVal} minCFMForIAQ: ${minCFMForIAQ.currentVal} maxCoolingDamperPos " +
                "${maxCoolingDamperPos.currentVal} minCoolingDamperPos: ${minCoolingDamperPos.currentVal} maxHeatingDamperPos: " +
                "${maxHeatingDamperPos.currentVal} minHeatingDamperPos: ${minHeatingDamperPos.currentVal} kFactor: " +
                "${kFactor.currentVal}"
    }

}