package a75f.io.renatus.profiles.acb

import a75f.io.logic.bo.building.vav.AcbProfileConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AcbConfigViewState {

    var damperType by mutableStateOf (0.0)
    var damperSize by mutableStateOf (0.0)
    var damperShape by mutableStateOf (0.0)
    var valveType by mutableStateOf (0.0)
    var zonePriority by mutableStateOf (0.0)

    var relay1Config by mutableStateOf(ConfigState(false, 0))
    var relay2Config by mutableStateOf(ConfigState(false, 0))

    var condensateSensorType by mutableStateOf(false)

    var autoAway by mutableStateOf (false)
    var autoForceOccupied by mutableStateOf (false)
    var enableCo2Control by mutableStateOf (false)
    var enableIAQControl by mutableStateOf (false)
    var enableCFMControl by mutableStateOf (false)

    var temperatureOffset by mutableStateOf (0.0)

    var maxCoolingDamperPos by mutableStateOf(0.0)
    var minCoolingDamperPos by mutableStateOf(0.0)
    var maxHeatingDamperPos by mutableStateOf(0.0)
    var minHeatingDamperPos by mutableStateOf(0.0)

    var kFactor by mutableStateOf(0.0)

    var maxCFMCooling by mutableStateOf(0.0)
    var minCFMCooling by mutableStateOf(0.0)
    var maxCFMReheating by mutableStateOf(0.0)
    var minCFMReheating by mutableStateOf(0.0)

    companion object {
        fun fromAcbProfileConfig(config : AcbProfileConfiguration) : AcbConfigViewState {
            return AcbConfigViewState().apply {
                this.damperType = config.damperType.currentVal
                this.damperSize = config.damperSize.currentVal
                this.damperShape = config.damperShape.currentVal
                this.valveType = config.valveType.currentVal
                this.zonePriority = config.zonePriority.currentVal

                this.relay1Config = ConfigState(config.relay1Enable.enabled, config.relay1Association.associationVal)
                this.relay2Config = ConfigState(config.relay2Enable.enabled, config.relay2Association.associationVal)

                this.condensateSensorType = config.condensateSensorType.enabled

                this.autoAway = config.autoAway.enabled
                this.autoForceOccupied = config.autoForceOccupied.enabled
                this.enableCo2Control = config.enableCo2Control.enabled
                this.enableIAQControl = config.enableIAQControl.enabled
                this.enableCFMControl = config.enableCFMControl.enabled

                this.temperatureOffset = config.temperatureOffset.currentVal

                this.maxCoolingDamperPos = config.maxCoolingDamperPos.currentVal
                this.minCoolingDamperPos = config.minCoolingDamperPos.currentVal
                this.maxHeatingDamperPos = config.maxHeatingDamperPos.currentVal
                this.minHeatingDamperPos = config.minHeatingDamperPos.currentVal

                this.kFactor = config.kFactor.currentVal

                this.maxCFMCooling = config.maxCFMCooling.currentVal
                this.minCFMCooling = config.minCFMCooling.currentVal
                this.maxCFMReheating = config.maxCFMReheating.currentVal
                this.minCFMReheating = config.minCFMReheating.currentVal
            }
        }
    }

    fun updateConfigFromViewState(config : AcbProfileConfiguration, ) {
        config.damperType.currentVal = this.damperType
        config.damperSize.currentVal = this.damperSize
        config.damperShape.currentVal = this.damperShape
        config.valveType.currentVal = this.valveType
        config.zonePriority.currentVal = this.zonePriority

        config.relay1Enable.enabled = this.relay1Config.enabled
        config.relay1Association.associationVal = this.relay1Config.association
        config.relay2Enable.enabled = this.relay2Config.enabled
        config.relay2Association.associationVal = this.relay2Config.association

        config.condensateSensorType.enabled = this.condensateSensorType

        config.autoAway.enabled = this.autoAway
        config.autoForceOccupied.enabled = this.autoForceOccupied
        config.enableCo2Control.enabled = this.enableCo2Control
        config.enableIAQControl.enabled = this.enableIAQControl
        config.enableCFMControl.enabled = this.enableCFMControl

        config.temperatureOffset.currentVal = this.temperatureOffset

        config.maxCoolingDamperPos.currentVal = this.maxCoolingDamperPos
        config.minCoolingDamperPos.currentVal = this.minCoolingDamperPos
        config.maxHeatingDamperPos.currentVal = this.maxHeatingDamperPos
        config.minHeatingDamperPos.currentVal = this.minHeatingDamperPos

        config.kFactor.currentVal = this.kFactor

        config.maxCFMCooling.currentVal = this.maxCFMCooling
        config.minCFMCooling.currentVal = this.minCFMCooling
        config.maxCFMReheating.currentVal = this.maxCFMReheating
        config.minCFMReheating.currentVal = this.minCFMReheating
    }
}

class ConfigState(enabled: Boolean,association: Int) {
    var enabled by mutableStateOf(enabled)
    var association by mutableStateOf(association)
}