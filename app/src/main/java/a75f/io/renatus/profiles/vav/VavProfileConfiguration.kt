package a75f.io.renatus.profiles.vav

import a75f.io.domain.VavEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

class VavProfileConfiguration (nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String, val model : SeventyFiveFProfileDirective)
    : ProfileConfiguration (nodeAddress, nodeType, priority, roomRef, floorRef) {

    lateinit var temperatureOffset: ValueConfig

    lateinit var damperType: AssociationConfig
    lateinit var damperSize: AssociationConfig
    lateinit var damperShape: AssociationConfig
    lateinit var reheatType: AssociationConfig
    lateinit var zonePriority: AssociationConfig

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
        damperType = getDefaultAssociationConfig(DomainName.damperType, model)
        damperSize = getDefaultAssociationConfig(DomainName.damperSize, model)
        damperShape = getDefaultAssociationConfig(DomainName.damperShape, model)
        reheatType = getDefaultAssociationConfig(DomainName.reheatType, model)
        zonePriority = getDefaultAssociationConfig(DomainName.zonePriority, model)

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
        kFactor.currentVal = 2.000 // TODO: remove this once model is updated with a defaultVal for kFactor
        maxCFMCooling = getDefaultValConfig(DomainName.maxCFMCooling, model)
        minCFMCooling = getDefaultValConfig(DomainName.minCFMCooling, model)
        maxCFMReheating = getDefaultValConfig(DomainName.maxCFMReheating, model)
        minCFMReheating = getDefaultValConfig(DomainName.minCFMReheating, model)

        isDefault = true

        return this
    }

    fun getActiveConfiguration() : VavProfileConfiguration {

        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val vavEquip = VavEquip(equip[Tags.ID].toString())
        damperType = AssociationConfig(DomainName.damperType, vavEquip.damperType.readDefaultVal().toInt())

        damperType = AssociationConfig(DomainName.damperType, vavEquip.damperType.readDefaultVal().toInt())
        damperSize = AssociationConfig(DomainName.damperSize, vavEquip.damperSize.readDefaultVal().toInt())
        damperShape = AssociationConfig(DomainName.damperShape, vavEquip.damperShape.readDefaultVal().toInt())
        reheatType = AssociationConfig(DomainName.reheatType, vavEquip.reheatType.readDefaultVal().toInt())
        zonePriority = AssociationConfig(DomainName.zonePriority, vavEquip.zonePriority.readDefaultVal().toInt())

        autoAway = EnableConfig(DomainName.autoAway, vavEquip.autoAway.readDefaultVal() > 0)
        autoForceOccupied = EnableConfig(DomainName.autoForceOccupied, vavEquip.autoForceOccupied.readDefaultVal() > 0)
        enableCo2Control = EnableConfig(DomainName.enableCo2Control, vavEquip.enableCo2Control.readDefaultVal() > 0)
        enableIAQControl = EnableConfig(DomainName.enableIAQControl, vavEquip.enableIAQControl.readDefaultVal() > 0)
        enableCFMControl = EnableConfig(DomainName.enableCFMControl, vavEquip.enableCFMControl.readDefaultVal() > 0)

        temperatureOffset = ValueConfig(DomainName.temperatureOffset, vavEquip.enableCFMControl.readDefaultVal())

        maxCoolingDamperPos = ValueConfig(DomainName.maxCoolingDamperPos, vavEquip.maxCoolingDamperPos.readDefaultVal())
        minCoolingDamperPos = ValueConfig(DomainName.minCoolingDamperPos, vavEquip.minCoolingDamperPos.readDefaultVal())
        maxHeatingDamperPos = ValueConfig(DomainName.maxHeatingDamperPos, vavEquip.maxHeatingDamperPos.readDefaultVal())
        minHeatingDamperPos = ValueConfig(DomainName.minHeatingDamperPos, vavEquip.minHeatingDamperPos.readDefaultVal())

        kFactor = ValueConfig(DomainName.kFactor, vavEquip.kFactor.readDefaultVal())
        maxCFMCooling = ValueConfig(DomainName.maxCFMCooling, vavEquip.maxCFMCooling.readDefaultVal())
        minCFMCooling = ValueConfig(DomainName.minCFMCooling, vavEquip.minCFMCooling.readDefaultVal())
        maxCFMReheating = ValueConfig(DomainName.maxCFMReheating, vavEquip.maxCFMReheating.readDefaultVal())
        minCFMReheating = ValueConfig(DomainName.minCFMReheating, vavEquip.minCFMReheating.readDefaultVal())

        isDefault = false
        return this
    }

    override fun getAssociationConfigs() : List<AssociationConfig> {
        var associations = mutableListOf<AssociationConfig>()
        return associations
    }
    override fun getEnableConfigs() : List<EnableConfig> {
        var enabled = mutableListOf<EnableConfig>()
        return enabled
    }

    override fun toString(): String {
        return " temperatureOffset ${temperatureOffset.currentVal} damperType ${damperType.associationVal} damperSize ${damperSize.associationVal}"+
            "damperShape ${damperShape.associationVal} reheatType ${reheatType.associationVal} autoForceOccupied ${autoForceOccupied.enabled}"+
                "autoAway ${autoAway.enabled} enableIAQControl ${enableIAQControl.enabled} enableCo2Control ${enableCo2Control.enabled}"+
                " enableCFMControl ${enableCFMControl.enabled} maxCoolingDamperPos ${maxCoolingDamperPos.currentVal}" +
                " minCoolingDamperPos ${minCoolingDamperPos.currentVal} maxHeatingDamperPos ${maxHeatingDamperPos.currentVal}" +
                "minHeatingDamperPos ${minHeatingDamperPos.currentVal} kFactor ${kFactor.currentVal} maxCFMCooling ${maxCFMCooling.currentVal}" +
                "minCFMCooling ${minCFMCooling.currentVal} maxCFMReheating ${maxCFMReheating.currentVal} minCFMReheating ${minCFMReheating.currentVal}"

    }
}