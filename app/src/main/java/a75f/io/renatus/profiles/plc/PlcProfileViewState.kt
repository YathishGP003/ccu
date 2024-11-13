package a75f.io.renatus.profiles.plc

import a75f.io.logic.bo.building.plc.PlcProfileConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PlcProfileViewState {

    var analog1InputType by mutableStateOf (0.0)
    var pidTargetValue by mutableStateOf (0.0)
    var thermistor1InputType by mutableStateOf (0.0)
    var pidProportionalRange by mutableStateOf (0.0)
    var nativeSensorType by mutableStateOf (0.0)


    var expectZeroErrorAtMidpoint by mutableStateOf (false)
    var invertControlLoopoutput by mutableStateOf (false)
    var useAnalogIn2ForSetpoint by mutableStateOf (false)

    var analog2InputType by mutableStateOf (0.0)
    var setpointSensorOffset by mutableStateOf (0.0)
    var analog1MinOutput by mutableStateOf (0.0)
    var analog1MaxOutput by mutableStateOf (0.0)

    var relay1OutputEnable by mutableStateOf (false)
    var relay2OutputEnable by mutableStateOf (false)
    
    var relay1OnThreshold by mutableStateOf (0.0)
    var relay2OnThreshold by mutableStateOf (0.0)
    var relay1OffThreshold by mutableStateOf (0.0)
    var relay2OffThreshold by mutableStateOf (0.0)


    companion object {
        fun fromPlcProfileConfig(config : PlcProfileConfig) : PlcProfileViewState {
            return PlcProfileViewState().apply {
                this.analog1InputType = config.analog1InputType.currentVal
                this.pidTargetValue = config.pidTargetValue.currentVal
                this.thermistor1InputType = config.thermistor1InputType.currentVal
                this.pidProportionalRange = config.pidProportionalRange.currentVal
                this.nativeSensorType = config.nativeSensorType.currentVal

                this.expectZeroErrorAtMidpoint = config.expectZeroErrorAtMidpoint.enabled
                this.invertControlLoopoutput = config.invertControlLoopoutput.enabled
                this.useAnalogIn2ForSetpoint = config.useAnalogIn2ForSetpoint.enabled

                this.analog2InputType = config.analog2InputType.currentVal
                this.setpointSensorOffset = config.setpointSensorOffset.currentVal
                this.analog1MinOutput = config.analog1MinOutput.currentVal
                this.analog1MaxOutput = config.analog1MaxOutput.currentVal

                this.relay1OutputEnable = config.relay1OutputEnable.enabled
                this.relay2OutputEnable = config.relay2OutputEnable.enabled

                this.relay1OnThreshold = config.relay1OnThreshold.currentVal
                this.relay2OnThreshold = config.relay2OnThreshold.currentVal
                this.relay1OffThreshold = config.relay1OffThreshold.currentVal
                this.relay2OffThreshold = config.relay2OffThreshold.currentVal
            }
        }
    }

    fun updateConfigFromViewState(config : PlcProfileConfig) {
        config.analog1InputType.currentVal = this.analog1InputType
        config.pidTargetValue.currentVal = this.pidTargetValue
        config.thermistor1InputType.currentVal = this.thermistor1InputType
        config.pidProportionalRange.currentVal = this.pidProportionalRange
        config.nativeSensorType.currentVal = this.nativeSensorType

        config.expectZeroErrorAtMidpoint.enabled = this.expectZeroErrorAtMidpoint
        config.invertControlLoopoutput.enabled = this.invertControlLoopoutput
        config.useAnalogIn2ForSetpoint.enabled = this.useAnalogIn2ForSetpoint

        config.analog2InputType.currentVal = this.analog2InputType
        config.setpointSensorOffset.currentVal = this.setpointSensorOffset
        config.analog1MinOutput.currentVal = this.analog1MinOutput
        config.analog1MaxOutput.currentVal = this.analog1MaxOutput

        config.relay1OutputEnable.enabled = this.relay1OutputEnable
        config.relay2OutputEnable.enabled = this.relay2OutputEnable

        config.relay1OnThreshold.currentVal = this.relay1OnThreshold
        config.relay2OnThreshold.currentVal = this.relay2OnThreshold
        config.relay1OffThreshold.currentVal = this.relay1OffThreshold
        config.relay2OffThreshold.currentVal = this.relay2OffThreshold
    }
}