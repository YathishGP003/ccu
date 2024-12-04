package a75f.io.renatus.profiles.otn

import a75f.io.logic.bo.building.otn.OtnProfileConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class OtnConfigViewState {
    var zonePriority by mutableStateOf (0.0)

    var autoAway by mutableStateOf (false)
    var autoForceOccupied by mutableStateOf (false)

    var temperatureOffset by mutableStateOf (0.0)


    companion object {
        fun fromOtnProfileConfig(config : OtnProfileConfiguration) : OtnConfigViewState {
            return OtnConfigViewState().apply {
                this.zonePriority = config.zonePriority.currentVal
                this.autoAway = config.autoAway.enabled
                this.autoForceOccupied = config.autoForceOccupied.enabled
                this.temperatureOffset = config.temperatureOffset.currentVal
            }
        }
    }

    fun updateConfigFromViewState(config : OtnProfileConfiguration) {
        config.zonePriority.currentVal = this.zonePriority
        config.autoAway.enabled = this.autoAway
        config.autoForceOccupied.enabled = this.autoForceOccupied
        config.temperatureOffset.currentVal = this.temperatureOffset
    }
}