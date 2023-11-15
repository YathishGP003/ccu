package a75f.io.domain.smartNode.vavNoFan

import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig

open class VavNoFanTestConfiguration (nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String)
    : ProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef) {

    var damperType = AssociationConfig(DomainName.damperType)
    var damperSize = AssociationConfig(DomainName.damperSize)
    var damperShape = AssociationConfig(DomainName.damperShape)
    var reheatType = AssociationConfig(DomainName.reheatType)
    var zonePriority = AssociationConfig(DomainName.zonePriority)

    var autoAway = EnableConfig(DomainName.autoAway)
    var autoForceOccupied = EnableConfig(DomainName.autoForceOccupied)
    var enableCo2Control = EnableConfig(DomainName.enableCo2Control)
    var enableIAQControl = EnableConfig(DomainName.enableIAQControl)
    var enableCFMControl = EnableConfig(DomainName.enableCFMControl)

    var temperatureOffset = ValueConfig(DomainName.temperatureOffset)

    var maxCoolingDamperPos = ValueConfig(DomainName.maxCoolingDamperPos)
    var minCoolingDamperPos = ValueConfig(DomainName.minCoolingDamperPos)
    var maxHeatingDamperPos = ValueConfig(DomainName.maxHeatingDamperPos)
    var minHeatingDamperPos = ValueConfig(DomainName.minHeatingDamperPos)

    var kFactor = ValueConfig(DomainName.kFactor)
    var maxCFMCooling = ValueConfig(DomainName.maxCFMCooling)
    var minCFMCooling = ValueConfig(DomainName.minCFMCooling)
    var maxCFMReheating = ValueConfig(DomainName.maxCFMReheating)
    var minCFMReheating = ValueConfig(DomainName.minCFMReheating)

    init {
        damperType.associationVal = 1
        damperSize.associationVal = 2
        damperShape.associationVal = 0
        reheatType.associationVal = 1
        zonePriority.associationVal = 2

        autoAway.enabled = true
        autoForceOccupied.enabled = true
        enableCo2Control.enabled = false
        enableIAQControl.enabled = false
        enableCFMControl.enabled = false

        maxCoolingDamperPos.currentVal = 95.0
        minCoolingDamperPos.currentVal = 15.0
        maxHeatingDamperPos.currentVal = 90.0
        minHeatingDamperPos.currentVal = 10.0

        kFactor.currentVal = 2.001
        maxCFMCooling.currentVal = 250.0
        minCFMCooling.currentVal = 60.0
        maxCFMReheating.currentVal = 240.0
        minCFMReheating.currentVal = 70.0
    }

    override fun getAssociationConfigs() : List<AssociationConfig> {
        var associations = mutableListOf<AssociationConfig>()
        return associations
    }
    override fun getEnableConfigs() : List<EnableConfig> {
        var enabled = mutableListOf<EnableConfig>()
        return enabled
    }

    }