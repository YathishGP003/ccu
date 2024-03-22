package a75f.io.renatus.profiles.bypass

import a75f.io.logic.bo.building.bypassdamper.BypassDamperProfileConfiguration
import a75f.io.logic.bo.building.vav.VavProfileConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class BypassConfigViewState {

    var damperType by mutableStateOf (0.0)
    var damperMinPosition by mutableStateOf (0.0)
    var damperMaxPosition by mutableStateOf (0.0)

    var pressureSensorType by mutableStateOf (0.0)
    var sensorMinVoltage by mutableStateOf (0.0)
    var sensorMaxVoltage by mutableStateOf (0.0)
    var pressureSensorMinVal by mutableStateOf (0.0)
    var pressureSensorMaxVal by mutableStateOf (0.0)
    
    var satMinThreshold by mutableStateOf (0.0)
    var satMaxThreshold by mutableStateOf (0.0)

    var pressureSetpoint by mutableStateOf(0.0)
    var expectedPressureError by mutableStateOf (0.0)
    
    companion object {
        fun fromBypassDamperProfileConfig(config : BypassDamperProfileConfiguration) : BypassConfigViewState {
            return BypassConfigViewState().apply {
                this.damperType = config.damperType.currentVal
                this.damperMinPosition = config.damperMinPosition.currentVal
                this.damperMaxPosition = config.damperMaxPosition.currentVal

                this.pressureSensorType = config.pressureSensorType.currentVal
                this.sensorMinVoltage = config.sensorMinVoltage.currentVal
                this.sensorMaxVoltage = config.sensorMaxVoltage.currentVal
                this.pressureSensorMinVal = config.pressureSensorMinVal.currentVal
                this.pressureSensorMaxVal = config.pressureSensorMaxVal.currentVal

                this.satMinThreshold = config.satMinThreshold.currentVal
                this.satMaxThreshold = config.satMaxThreshold.currentVal

                this.pressureSetpoint = config.pressureSetpoint.currentVal
                this.expectedPressureError = config.expectedPressureError.currentVal
            }
        }
    }

    fun updateConfigFromViewState(config : BypassDamperProfileConfiguration) {
         config.damperType.currentVal = this.damperType
         config.damperMinPosition.currentVal = this.damperMinPosition
         config.damperMaxPosition.currentVal = this.damperMaxPosition

         config.pressureSensorType.currentVal = this.pressureSensorType
         config.sensorMinVoltage.currentVal = this.sensorMinVoltage
         config.sensorMaxVoltage.currentVal = this.sensorMaxVoltage
         config.pressureSensorMinVal.currentVal = this.pressureSensorMinVal
         config.pressureSensorMaxVal.currentVal = this.pressureSensorMaxVal

         config.satMinThreshold.currentVal = this.satMinThreshold
         config.satMaxThreshold.currentVal = this.satMaxThreshold

         config.pressureSetpoint.currentVal = this.pressureSetpoint
         config.expectedPressureError.currentVal = this.expectedPressureError
    }
    
    fun equalsViewState(otherViewState: BypassConfigViewState) : Boolean {
        return (otherViewState.damperType == this.damperType &&
            otherViewState.damperMinPosition == this.damperMinPosition &&
            otherViewState.damperMaxPosition == this.damperMaxPosition &&
            otherViewState.pressureSensorType == this.pressureSensorType &&
            otherViewState.sensorMinVoltage == this.sensorMinVoltage &&
            otherViewState.sensorMaxVoltage == this.sensorMaxVoltage &&
            otherViewState.pressureSensorMinVal == this.pressureSensorMinVal &&
            otherViewState.pressureSensorMaxVal == this.pressureSensorMaxVal &&
            otherViewState.satMinThreshold == this.satMinThreshold &&
            otherViewState.satMaxThreshold == this.satMaxThreshold &&
            otherViewState.pressureSetpoint == this.pressureSetpoint &&
            otherViewState.expectedPressureError == this.expectedPressureError)
    }
    
}