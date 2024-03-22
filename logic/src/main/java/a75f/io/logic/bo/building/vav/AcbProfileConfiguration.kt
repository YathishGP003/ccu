package a75f.io.logic.bo.building.vav

import a75f.io.domain.VavAcbEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

class AcbProfileConfiguration (nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String, profileType : ProfileType, val model : SeventyFiveFProfileDirective)
    : ProfileConfiguration (nodeAddress, nodeType, priority, roomRef, floorRef, profileType.name) {

    lateinit var temperatureOffset: ValueConfig

    lateinit var damperType: ValueConfig
    lateinit var damperSize: ValueConfig
    lateinit var damperShape: ValueConfig
    lateinit var valveType: ValueConfig
    lateinit var zonePriority: ValueConfig

    lateinit var condensateSensorType: EnableConfig

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

    fun getDefaultConfiguration() : AcbProfileConfiguration {
        damperType = getDefaultValConfig(DomainName.damperType, model)
        damperSize = getDefaultValConfig(DomainName.damperSize, model)
        damperShape = getDefaultValConfig(DomainName.damperShape, model)
        valveType = getDefaultValConfig(DomainName.valveType, model)
        zonePriority = getDefaultValConfig(DomainName.zonePriority, model)

        condensateSensorType = getDefaultEnableConfig(DomainName.thermistor2Type, model)

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

        if (L.ccu().bypassDamperProfile != null) {
            minCoolingDamperPos.currentVal = 10.0
            minHeatingDamperPos.currentVal = 10.0
        }

        isDefault = true

        return this
    }

    fun getActiveConfiguration() : AcbProfileConfiguration {

        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val vavEquip = VavAcbEquip(equip[Tags.ID].toString())

        getDefaultConfiguration()
        damperType.currentVal = vavEquip.damperType.readDefaultVal()
        damperSize.currentVal = vavEquip.damperSize.readDefaultVal()
        damperShape.currentVal = vavEquip.damperShape.readDefaultVal()
        valveType.currentVal = vavEquip.valveType.readDefaultVal()
        // Zone Priority needs to use readPriorityVal() because portals can write to it at Level 10
        // Other config points are only written to at Level 8 (via CCU or remote access), so readDefaultVal() is appropriate
        zonePriority.currentVal = vavEquip.zonePriority.readPriorityVal()
        condensateSensorType.enabled = vavEquip.thermistor2Type.readDefaultVal() > 0

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

            if (L.ccu().bypassDamperProfile != null) {
                minCoolingDamperPos.currentVal = 10.0
                minHeatingDamperPos.currentVal = 10.0
            }

            maxCFMCooling.currentVal = vavEquip.maxCFMCooling.readDefaultVal()
            minCFMCooling.currentVal = vavEquip.minCFMCooling.readDefaultVal()
            maxCFMReheating.currentVal = vavEquip.maxCFMReheating.readDefaultVal()
            minCFMReheating.currentVal = vavEquip.minCFMReheating.readDefaultVal()
        } else {
            kFactor = getDefaultValConfig(DomainName.kFactor, model)

            minCoolingDamperPos.currentVal = vavEquip.minCoolingDamperPos.readPriorityVal()
            maxCoolingDamperPos.currentVal = vavEquip.maxCoolingDamperPos.readDefaultVal()
            minHeatingDamperPos.currentVal = vavEquip.minHeatingDamperPos.readPriorityVal()

            maxCFMCooling = getDefaultValConfig(DomainName.maxCFMCooling, model)
            minCFMCooling = getDefaultValConfig(DomainName.minCFMCooling, model)
            maxCFMReheating = getDefaultValConfig(DomainName.maxCFMReheating, model)
            minCFMReheating = getDefaultValConfig(DomainName.minCFMReheating, model)

            if (!(maxCFMCooling.currentVal > 0.0)) maxCFMCooling.currentVal = 250.0
            if (!(minCFMCooling.currentVal > 0.0)) minCFMCooling.currentVal = 50.0
            if (!(maxCFMReheating.currentVal > 0.0)) maxCFMReheating.currentVal = 250.0
            if (!(minCFMReheating.currentVal > 0.0)) minCFMReheating.currentVal = 50.0
        }


        /*damperType = ValueConfig(DomainName.damperType, vavEquip.damperType.readDefaultVal())
        damperSize = ValueConfig(DomainName.damperSize, vavEquip.damperSize.readDefaultVal())
        damperShape = ValueConfig(DomainName.damperShape, vavEquip.damperShape.readDefaultVal())
        reheatType = AssociationConfig(DomainName.reheatType, vavEquip.reheatType.readDefaultVal().toInt())
        zonePriority = ValueConfig(DomainName.zonePriority, vavEquip.zonePriority.readDefaultVal())

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
        minCFMReheating = ValueConfig(DomainName.minCFMReheating, vavEquip.minCFMReheating.readDefaultVal())*/

        isDefault = false
        return this
    }

    override fun getAssociationConfigs() : List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
        }
    }

    override fun getDependencies(): List<ValueConfig> {
        TODO("Not yet implemented")
    }

    override fun getEnableConfigs() : List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(condensateSensorType)
            add(autoAway)
            add(autoForceOccupied)
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
            add(valveType)
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
            "damperShape ${damperShape.currentVal} valveType ${valveType.currentVal} condensateSensorType ${condensateSensorType.enabled} " +
                "autoForceOccupied ${autoForceOccupied.enabled} autoAway ${autoAway.enabled} enableIAQControl ${enableIAQControl.enabled} " +
                "enableCo2Control ${enableCo2Control.enabled} enableCFMControl ${enableCFMControl.enabled} maxCoolingDamperPos ${maxCoolingDamperPos.currentVal}" +
                " minCoolingDamperPos ${minCoolingDamperPos.currentVal} maxHeatingDamperPos ${maxHeatingDamperPos.currentVal}" +
                "minHeatingDamperPos ${minHeatingDamperPos.currentVal} kFactor ${kFactor.currentVal} maxCFMCooling ${maxCFMCooling.currentVal}" +
                "minCFMCooling ${minCFMCooling.currentVal} maxCFMReheating ${maxCFMReheating.currentVal} minCFMReheating ${minCFMReheating.currentVal}"

    }
}