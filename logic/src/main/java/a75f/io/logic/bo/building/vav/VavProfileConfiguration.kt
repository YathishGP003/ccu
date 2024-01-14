package a75f.io.logic.bo.building.vav

import a75f.io.domain.VavEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

class VavProfileConfiguration (nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String, profileType : ProfileType, val model : SeventyFiveFProfileDirective)
    : ProfileConfiguration (nodeAddress, nodeType, priority, roomRef, floorRef, profileType.name) {

    lateinit var temperatureOffset: ValueConfig

    lateinit var damperType: ValueConfig
    lateinit var damperSize: ValueConfig
    lateinit var damperShape: ValueConfig
    lateinit var reheatType: ValueConfig
    lateinit var zonePriority: ValueConfig

    lateinit var autoForceOccupied: EnableConfig
    lateinit var autoAway: EnableConfig
    lateinit var enableCo2Control: EnableConfig
    lateinit var enableIAQControl: EnableConfig
    lateinit var enableCFMControl: EnableConfig

    lateinit var maxCoolingDamperPos: ValueConfig
    lateinit var minCoolingDamperPos: ValueConfig
    lateinit var maxHeatingDamperPos: ValueConfig
    lateinit var minHeatingDamperPos: ValueConfig

    lateinit var kFactor: ValueConfig

    lateinit var maxCFMCooling: ValueConfig
    lateinit var minCFMCooling: ValueConfig
    lateinit var maxCFMReheating: ValueConfig
    lateinit var minCFMReheating: ValueConfig

    fun getDefaultConfiguration() : VavProfileConfiguration {
        damperType = getDefaultValConfig(DomainName.damperType, model)
        damperSize = getDefaultValConfig(DomainName.damperSize, model)
        damperShape = getDefaultValConfig(DomainName.damperShape, model)
        reheatType = getDefaultValConfig(DomainName.reheatType, model)
        zonePriority = getDefaultValConfig(DomainName.zonePriority, model)

        autoAway = getDefaultEnableConfig(DomainName.autoAway, model)
        autoForceOccupied = getDefaultEnableConfig(DomainName.autoForceOccupied, model)
        enableCo2Control = getDefaultEnableConfig(DomainName.enableCo2Control, model)
        enableIAQControl = getDefaultEnableConfig(DomainName.enableIAQControl, model)
        enableCFMControl = getDefaultEnableConfig(DomainName.enableCFMControl, model)

        temperatureOffset = getDefaultValConfig(DomainName.temperatureOffset, model)

        maxCoolingDamperPos = getDefaultValConfig(DomainName.maxCoolingDamperPos, model)
        minCoolingDamperPos = getDefaultValConfig(DomainName.minCoolingDamperPos, model)
        maxHeatingDamperPos = getDefaultValConfig(DomainName.maxHeatingDamperPos, model)
        minHeatingDamperPos = getDefaultValConfig(DomainName.minHeatingDamperPos, model)

        kFactor = getDefaultValConfig(DomainName.kFactor, model)
        maxCFMCooling = getDefaultValConfig(DomainName.maxCFMCooling, model)
        minCFMCooling = getDefaultValConfig(DomainName.minCFMCooling, model)
        maxCFMReheating = getDefaultValConfig(DomainName.maxCFMReheating, model)
        minCFMReheating = getDefaultValConfig(DomainName.minCFMReheating, model)

        if (!(maxCFMCooling.currentVal > 0.0)) maxCFMCooling.currentVal = 250.0
        if (!(minCFMCooling.currentVal > 0.0)) minCFMCooling.currentVal = 50.0
        if (!(maxCFMReheating.currentVal > 0.0)) maxCFMReheating.currentVal = 250.0
        if (!(minCFMReheating.currentVal > 0.0)) minCFMReheating.currentVal = 50.0

        isDefault = true

        return this
    }

    fun getActiveConfiguration() : VavProfileConfiguration {

        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val vavEquip = VavEquip(equip[Tags.ID].toString())

        getDefaultConfiguration()
        damperType.currentVal = vavEquip.damperType.readDefaultVal()
        damperSize.currentVal = vavEquip.damperSize.readDefaultVal()
        damperShape.currentVal = vavEquip.damperShape.readDefaultVal()
        reheatType.currentVal = vavEquip.reheatType.readDefaultVal()
        zonePriority.currentVal = vavEquip.zonePriority.readDefaultVal()

        autoAway.enabled = vavEquip.autoAway.readDefaultVal() > 0
        autoForceOccupied.enabled = vavEquip.autoForceOccupied.readDefaultVal() > 0
        enableCo2Control.enabled = vavEquip.enableCo2Control.readDefaultVal() > 0
        enableIAQControl.enabled = vavEquip.enableIAQControl.readDefaultVal() > 0
        enableCFMControl.enabled = vavEquip.enableCFMControl.readDefaultVal() > 0

        temperatureOffset.currentVal = vavEquip.temperatureOffset.readDefaultVal()

        maxHeatingDamperPos.currentVal = vavEquip.maxHeatingDamperPos.readDefaultVal()

        if (enableCFMControl.enabled) {
            kFactor.currentVal = vavEquip.kFactor.readDefaultVal()

            minCoolingDamperPos = getDefaultValConfig(DomainName.minCoolingDamperPos, model)
            maxCoolingDamperPos = getDefaultValConfig(DomainName.maxCoolingDamperPos, model)
            minHeatingDamperPos = getDefaultValConfig(DomainName.minHeatingDamperPos, model)

            maxCFMCooling.currentVal = vavEquip.maxCFMCooling.readDefaultVal()
            minCFMCooling.currentVal = vavEquip.minCFMCooling.readDefaultVal()
            maxCFMReheating.currentVal = vavEquip.maxCFMReheating.readDefaultVal()
            minCFMReheating.currentVal = vavEquip.minCFMReheating.readDefaultVal()
        } else {
            kFactor = getDefaultValConfig(DomainName.kFactor, model)

            minCoolingDamperPos.currentVal = vavEquip.minCoolingDamperPos.readDefaultVal()
            maxCoolingDamperPos.currentVal = vavEquip.maxCoolingDamperPos.readDefaultVal()
            minHeatingDamperPos.currentVal = vavEquip.minHeatingDamperPos.readDefaultVal()

            maxCFMCooling = getDefaultValConfig(DomainName.maxCFMCooling, model)
            minCFMCooling = getDefaultValConfig(DomainName.minCFMCooling, model)
            maxCFMReheating = getDefaultValConfig(DomainName.maxCFMReheating, model)
            minCFMReheating = getDefaultValConfig(DomainName.minCFMReheating, model)

            if (!(maxCFMCooling.currentVal > 0.0)) maxCFMCooling.currentVal = 250.0
            if (!(minCFMCooling.currentVal > 0.0)) minCFMCooling.currentVal = 50.0
            if (!(maxCFMReheating.currentVal > 0.0)) maxCFMReheating.currentVal = 250.0
            if (!(minCFMReheating.currentVal > 0.0)) minCFMReheating.currentVal = 50.0
        }
        isDefault = false
        return this
    }

    override fun getAssociationConfigs() : List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
        }
    }
    override fun getEnableConfigs() : List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(autoAway)
            add(autoForceOccupied)
            add(enableCFMControl)
            add(enableCo2Control)
            add(enableIAQControl)
            add(enableCFMControl)
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(damperType)
            add(damperSize)
            add(damperShape)
            add(reheatType)
            add(zonePriority)
            add(temperatureOffset)
            add(maxCoolingDamperPos)
            add(minCoolingDamperPos)
            add(minHeatingDamperPos)
            add(maxHeatingDamperPos)
            add(kFactor)
            add(minCFMCooling)
            add(maxCFMCooling)
            add(minCFMReheating)
            add(maxCFMReheating)
        }
    }
    override fun toString(): String {
        return " temperatureOffset ${temperatureOffset.currentVal} damperType ${damperType.currentVal} damperSize ${damperSize.currentVal}"+
            "damperShape ${damperShape.currentVal} reheatType ${reheatType.currentVal} autoForceOccupied ${autoForceOccupied.enabled}"+
                "autoAway ${autoAway.enabled} enableIAQControl ${enableIAQControl.enabled} enableCo2Control ${enableCo2Control.enabled}"+
                " enableCFMControl ${enableCFMControl.enabled} maxCoolingDamperPos ${maxCoolingDamperPos.currentVal}" +
                " minCoolingDamperPos ${minCoolingDamperPos.currentVal} maxHeatingDamperPos ${maxHeatingDamperPos.currentVal}" +
                "minHeatingDamperPos ${minHeatingDamperPos.currentVal} kFactor ${kFactor.currentVal} maxCFMCooling ${maxCFMCooling.currentVal}" +
                "minCFMCooling ${minCFMCooling.currentVal} maxCFMReheating ${maxCFMReheating.currentVal} minCFMReheating ${minCFMReheating.currentVal}"

    }
}