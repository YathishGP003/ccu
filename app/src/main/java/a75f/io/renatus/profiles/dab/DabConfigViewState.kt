package a75f.io.renatus.profiles.dab

import a75f.io.logic.bo.building.dab.DabProfileConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DabConfigViewState {

    var damper1Type by mutableStateOf (0.0)
    var damper1Size by mutableStateOf (0.0)
    var damper1Shape by mutableStateOf (0.0)
    var damper2Type by mutableStateOf (0.0)
    var damper2Size by mutableStateOf (0.0)
    var damper2Shape by mutableStateOf (0.0)
    var reheatType by mutableStateOf (0.0)
    var zonePriority by mutableStateOf (0.0)

    var autoAway by mutableStateOf (false)
    var autoForceOccupied by mutableStateOf (false)
    var enableCo2Control by mutableStateOf (false)
    var enableIAQControl by mutableStateOf (false)
    var enableCFMControl by mutableStateOf (false)

    var temperatureOffset by mutableStateOf (0.0)

    var minCFMForIAQ by mutableStateOf(0.0)
    var minReheatDamperPos by mutableStateOf(0.0)
    var maxCoolingDamperPos by mutableStateOf(0.0)
    var minCoolingDamperPos by mutableStateOf(0.0)
    var maxHeatingDamperPos by mutableStateOf(0.0)
    var minHeatingDamperPos by mutableStateOf(0.0)

    var kFactor by mutableStateOf(0.0)
    var unusedPortState by mutableStateOf(hashMapOf<String, Boolean>())
    companion object {
        fun fromDabProfileConfig(config: DabProfileConfiguration): DabConfigViewState {
            return DabConfigViewState().apply {
                this.temperatureOffset = config.temperatureOffset.currentVal
                this.damper1Type = config.damper1Type.currentVal
                this.damper1Shape = config.damper1Shape.currentVal
                this.damper1Size = config.damper1Size.currentVal
                this.damper2Type = config.damper2Type.currentVal
                this.damper2Shape = config.damper2Shape.currentVal
                this.damper2Size = config.damper2Size.currentVal

                this.reheatType =   config.reheatType.currentVal
                this.zonePriority =  config.zonePriority.currentVal

                this.enableCo2Control = config.enableCo2Control.enabled
                this.enableIAQControl = config.enableIAQControl.enabled
                this.autoAway = config.autoAway.enabled
                this.autoForceOccupied = config.autoForceOccupied.enabled

                this.enableCFMControl = config.enableCFMControl.enabled
                this.kFactor = config.kFactor.currentVal

                this.minCFMForIAQ = config.minCFMForIAQ.currentVal
                this.minReheatDamperPos = config.minReheatDamperPos.currentVal
                this.maxCoolingDamperPos = config.maxCoolingDamperPos.currentVal
                this.minCoolingDamperPos = config.minCoolingDamperPos.currentVal
                this.maxHeatingDamperPos = config.maxHeatingDamperPos.currentVal
                this.minHeatingDamperPos = config.minHeatingDamperPos.currentVal
                this.unusedPortState = config.unusedPorts

            }
        }
    }

    fun updateConfigFromViewState(config : DabProfileConfiguration) {
        config.temperatureOffset.currentVal = this.temperatureOffset

        config.damper1Type.currentVal = this.damper1Type
        config.damper1Size.currentVal = this.damper1Size
        config.damper1Shape.currentVal = this.damper1Shape
        config.damper2Type.currentVal = this.damper2Type
        config.damper2Size.currentVal = this.damper2Size
        config.damper2Shape.currentVal = this.damper2Shape
        config.reheatType.currentVal = this.reheatType
        config.zonePriority.currentVal = this.zonePriority

        config.enableCo2Control.enabled = this.enableCo2Control
        config.enableIAQControl.enabled = this.enableIAQControl
        config.autoAway.enabled = this.autoAway
        config.autoForceOccupied.enabled = this.autoForceOccupied

        config.enableCFMControl.enabled = this.enableCFMControl
        config.kFactor.currentVal = this.kFactor

        config.minCFMForIAQ.currentVal = this.minCFMForIAQ
        config.minReheatDamperPos.currentVal = this.minReheatDamperPos
        config.maxCoolingDamperPos.currentVal = this.maxCoolingDamperPos
        config.minCoolingDamperPos.currentVal = this.minCoolingDamperPos
        config.maxHeatingDamperPos.currentVal = this.maxHeatingDamperPos
        config.minHeatingDamperPos.currentVal = this.minHeatingDamperPos
        config.unusedPorts = this.unusedPortState
    }
}