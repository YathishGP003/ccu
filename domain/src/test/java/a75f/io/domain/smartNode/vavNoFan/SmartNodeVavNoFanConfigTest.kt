package a75f.io.domain.smartNode.vavNoFan

import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

open class VavNoFanTestConfiguration (nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String)
    : ProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef) {

    var damperType = AssociationConfig(a75f.io.domain.api.damperType)
    var damperSize = AssociationConfig(a75f.io.domain.api.damperSize)
    var damperShape = AssociationConfig(a75f.io.domain.api.damperShape)
    var reheatType = AssociationConfig(a75f.io.domain.api.reheatType)
    var zonePriority = AssociationConfig(a75f.io.domain.api.zonePriority)

    var autoAway = EnableConfig(a75f.io.domain.api.autoAway)
    var autoForceOccupied = EnableConfig(a75f.io.domain.api.autoForceOccupied)
    var enableCo2Control = EnableConfig(a75f.io.domain.api.enableCo2Control)
    var enableIAQControl = EnableConfig(a75f.io.domain.api.enableIAQControl)
    var enableCFMControl = EnableConfig(a75f.io.domain.api.enableCFMControl)

    var temperatureOffset = ValueConfig(a75f.io.domain.api.temperatureOffset)

    var maxCoolingDamperPos = ValueConfig(a75f.io.domain.api.maxCoolingDamperPos)
    var minCoolingDamperPos = ValueConfig(a75f.io.domain.api.minCoolingDamperPos)
    var maxHeatingDamperPos = ValueConfig(a75f.io.domain.api.maxHeatingDamperPos)
    var minHeatingDamperPos = ValueConfig(a75f.io.domain.api.minHeatingDamperPos)

    var kFactor = ValueConfig(a75f.io.domain.api.kFactor)
    var maxCFMCooling = ValueConfig(a75f.io.domain.api.maxCFMCooling)
    var minCFMCooling = ValueConfig(a75f.io.domain.api.minCFMCooling)
    var maxCFMReheating = ValueConfig(a75f.io.domain.api.maxCFMReheating)
    var minCFMReheating = ValueConfig(a75f.io.domain.api.minCFMReheating)

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