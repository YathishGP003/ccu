package a75f.io.renatus.profiles.vav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class VavConfigViewState {
    var damperType by mutableStateOf (0)
    var damperSize by mutableStateOf (0)
    var damperShape by mutableStateOf (0)
    var reheatType by mutableStateOf (0)
    var zonePriority by mutableStateOf (0)
    var autoAway by mutableStateOf (0)
    var enableIAQControl by mutableStateOf (0)
    var enableCo2Control by mutableStateOf (0)
    var enableCFMControl by mutableStateOf (0)
    var temperatureOffset by mutableStateOf (0.0)

    companion object {
        fun fromVavProfileConfig(config : VavProfileConfiguration) : VavConfigViewState {
            return VavConfigViewState().apply {
                this.damperType = config.damperType.currentVal.toInt()
                this.damperSize = config.damperSize.currentVal.toInt()
                this.damperShape = config.damperShape.currentVal.toInt()
                this.reheatType = config.reheatType.currentVal.toInt()
                this.zonePriority = config.zonePriority.currentVal.toInt()
            }
        }
    }

    fun updateConfigFromViewState(config : VavProfileConfiguration, ) {
        config.damperType.currentVal = this.damperType.toDouble()
        config.damperSize.currentVal = this.damperSize.toDouble()
        config.damperShape.currentVal = this.damperShape.toDouble()
        config.reheatType.currentVal = this.reheatType.toDouble()
        config.zonePriority.currentVal = this.zonePriority.toDouble()
        config.damperType.currentVal = this.damperType.toDouble()
        config.damperType.currentVal = this.damperType.toDouble()
    }
}