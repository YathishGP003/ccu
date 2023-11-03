package a75f.io.renatus.profiles.vav

import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

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
        damperType = getDefaultAssociationConfig(a75f.io.domain.api.damperType, model)
        damperSize = getDefaultAssociationConfig(a75f.io.domain.api.damperSize, model)
        damperShape = getDefaultAssociationConfig(a75f.io.domain.api.damperShape, model)
        reheatType = getDefaultAssociationConfig(a75f.io.domain.api.reheatType, model)
        zonePriority = getDefaultAssociationConfig(a75f.io.domain.api.zonePriority, model)

        autoAway = getDefaultEnableConfig(a75f.io.domain.api.autoAway, model)
        autoForceOccupied = getDefaultEnableConfig(a75f.io.domain.api.autoForceOccupied, model)
        enableCo2Control = getDefaultEnableConfig(a75f.io.domain.api.enableCo2Control, model)
        enableIAQControl = getDefaultEnableConfig(a75f.io.domain.api.enableIAQControl, model)
        enableCFMControl = getDefaultEnableConfig(a75f.io.domain.api.enableCFMControl, model)

        temperatureOffset = getDefaultValConfig(a75f.io.domain.api.temperatureOffset, model)

        maxCoolingDamperPos = getDefaultValConfig(a75f.io.domain.api.maxCoolingDamperPos, model)
        minCoolingDamperPos = getDefaultValConfig(a75f.io.domain.api.minCoolingDamperPos, model)
        maxHeatingDamperPos = getDefaultValConfig(a75f.io.domain.api.maxHeatingDamperPos, model)
        minHeatingDamperPos = getDefaultValConfig(a75f.io.domain.api.minHeatingDamperPos, model)

        kFactor = getDefaultValConfig(a75f.io.domain.api.kFactor, model)

        maxCFMCooling = getDefaultValConfig(a75f.io.domain.api.maxCFMCooling, model)
        minCFMCooling = getDefaultValConfig(a75f.io.domain.api.minCFMCooling, model)
        maxCFMReheating = getDefaultValConfig(a75f.io.domain.api.maxCFMReheating, model)
        minCFMReheating = getDefaultValConfig(a75f.io.domain.api.minCFMReheating, model)

        isDefault = true

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
        return " temperatureOffset $temperatureOffset damperType $damperType damperSize $damperSize"+
        "damperShape $damperShape reheatType $reheatType autoForceOccupied $autoForceOccupied"+
                "autoAway $autoAway enableIAQControl $enableIAQControl enableCo2Control $enableCo2Control"+
                        " enableCFMControl $enableCFMControl"
    }
}
