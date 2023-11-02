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
    lateinit var damperType: ValueConfig
    lateinit var damperSize: ValueConfig
    lateinit var damperShape: ValueConfig
    lateinit var reheatType: ValueConfig
    lateinit var zonePriority: ValueConfig
    lateinit var autoForceOccupied: EnableConfig
    lateinit var autoAway: EnableConfig
    lateinit var enableIAQControl: EnableConfig
    lateinit var enableCo2Control: EnableConfig
    lateinit var enableCFMControl: EnableConfig
    fun getDefaultConfiguration() : VavProfileConfiguration {
        CcuLog.i("CCU_DOMAIN"," getConfig for "+model.domainName+" count "+model.points.size)
        temperatureOffset = getDefaultValConfig(a75f.io.domain.api.temperatureOffset, model)
        damperType = getDefaultValConfig(a75f.io.domain.api.damperType, model)
        damperSize = getDefaultValConfig(a75f.io.domain.api.damperSize, model)
        damperShape = getDefaultValConfig(a75f.io.domain.api.damperShape, model)
        zonePriority = getDefaultValConfig(a75f.io.domain.api.zonePriority, model)
        reheatType = getDefaultValConfig(a75f.io.domain.api.reheatType, model)
        autoForceOccupied = getDefaultEnableConfig(a75f.io.domain.api.autoForceOccupied, model)
        autoAway = getDefaultEnableConfig(a75f.io.domain.api.autoAway, model)
        enableIAQControl = getDefaultEnableConfig(a75f.io.domain.api.enableIAQControl, model)
        enableCo2Control = getDefaultEnableConfig(a75f.io.domain.api.enableCo2Control, model)
        enableCFMControl = getDefaultEnableConfig(a75f.io.domain.api.enableCFMControl, model)
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
