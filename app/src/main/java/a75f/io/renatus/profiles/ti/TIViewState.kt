package a75f.io.renatus.profiles.ti

import a75f.io.logic.bo.building.caz.configs.TIConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

open class TIViewState {
    var temperatureOffset by mutableStateOf(0.0)
    var zonePriority by mutableStateOf(0.0)
    var roomTemperatureType by mutableStateOf(0.0)
    var supplyAirTemperatureType by mutableStateOf(0.0)

    companion object {
        fun fromTIProfileConfig(config : TIConfiguration) : TIViewState {
            return TIViewState().apply {
                this.zonePriority = config.zonePriority.currentVal
                this.temperatureOffset = config.temperatureOffset.currentVal
                this.roomTemperatureType = config.roomTemperatureType.currentVal
                this.supplyAirTemperatureType = config.supplyAirTemperatureType.currentVal
            }
        }
    }

    fun updateConfigFromViewState(config : TIConfiguration) {
        config.zonePriority.currentVal = this.zonePriority
        config.roomTemperatureType.currentVal = this.roomTemperatureType
        config.supplyAirTemperatureType.currentVal= this.supplyAirTemperatureType
        config.temperatureOffset.currentVal = this.temperatureOffset

    }
}