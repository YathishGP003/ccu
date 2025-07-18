package a75f.io.renatus.profiles.system

import a75f.io.logic.bo.building.system.vav.config.DabModulatingRtuProfileConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DabModulatingRtuViewState : ModulatingRtuViewState(){

    var isAdaptiveDeltaEnabled by mutableStateOf (false)
    var ismaximizedExitWaterTempEnable by mutableStateOf (false)
    var isDcwbEnabled by mutableStateOf (false)

    var chilledWaterTargetDelta by mutableStateOf(15.0)
    var chilledWaterExitTemperatureMargin by mutableStateOf(4.0)
    var chilledWaterExitTemperatureTarget by mutableStateOf(4.0)
    var chilledWaterMaxFlowRate by mutableStateOf(100.0)
    var analog1ValveClosedPosition by mutableStateOf(2)
    var analog1ValveFullPosition by mutableStateOf(10)

    var chilledWaterMaxFlowRateInc by mutableStateOf(1.0)

    companion object {
        fun fromProfileConfig(config: DabModulatingRtuProfileConfig): DabModulatingRtuViewState {
            return DabModulatingRtuViewState().apply {
                this.isAnalog1OutputEnabled = config.analog1OutputEnable.enabled
                this.isAnalog2OutputEnabled = config.analog2OutputEnable.enabled
                this.isAnalog3OutputEnabled = config.analog3OutputEnable.enabled
                this.isAnalog4OutputEnabled = config.analog4OutputEnable.enabled
                this.isRelay3OutputEnabled = config.relay3OutputEnable.enabled
                this.isRelay7OutputEnabled = config.relay7OutputEnable.enabled
                this.isAdaptiveDeltaEnabled = config.adaptiveDeltaEnable.enabled
                this.ismaximizedExitWaterTempEnable = config.maximizedExitWaterTempEnable.enabled
                this.isDcwbEnabled = config.dcwbEnable.enabled
                this.chilledWaterTargetDelta = config.chilledWaterTargetDelta.currentVal
                this.chilledWaterExitTemperatureMargin = config.chilledWaterExitTemperatureMargin.currentVal
                this.chilledWaterExitTemperatureTarget = config.chilledWaterExitTemperatureTarget.currentVal
                this.chilledWaterMaxFlowRate = config.chilledWaterMaxFlowRate.currentVal
                this.chilledWaterMaxFlowRateInc = config.chilledWaterMaxFlowRate.incVal

                this.analog1OutputAssociation = config.analog1OutputAssociation.associationVal
                this.analog2OutputAssociation = config.analog2OutputAssociation.associationVal
                this.analog3OutputAssociation = config.analog3OutputAssociation.associationVal
                this.analog4OutputAssociation = config.analog4OutputAssociation.associationVal
                this.relay3Association = config.relay3Association.associationVal

                this.relay7Association = config.relay7Association.associationVal

                this.thermistor1Enabled = config.thermistor1Enabled.enabled
                this.thermistor2Enabled = config.thermistor2Enabled.enabled
                this.thermistor1Association = config.thermistor1InAssociation.associationVal
                this.thermistor2Association = config.thermistor2InAssociation.associationVal
                this.analogIn1Enabled = config.analogIn1Enabled.enabled
                this.analogIn2Enabled = config.analogIn2Enabled.enabled
                this.analogIn1Association = config.analogIn1Association.associationVal
                this.analogIn2Association = config.analogIn2Association.associationVal

                this.analog1ValveClosedPosition = config.analog1ValveClosedPosition.currentVal.toInt()
                this.analog1ValveFullPosition = config.analog1ValveFullPosition.currentVal.toInt()

                this.analog1OutMinMaxConfig = config.analog1OutMinMaxConfig
                this.analog2OutMinMaxConfig = config.analog2OutMinMaxConfig
                this.analog3OutMinMaxConfig = config.analog3OutMinMaxConfig
                this.analog4OutMinMaxConfig = config.analog4OutMinMaxConfig

                this.unusedPortState = config.unusedPorts
            }
        }
    }


    fun updateConfigFromViewState(config : DabModulatingRtuProfileConfig ) {
        config.analog1OutputEnable.enabled = this.isAnalog1OutputEnabled
        config.analog2OutputEnable.enabled = this.isAnalog2OutputEnabled
        config.analog3OutputEnable.enabled = this.isAnalog3OutputEnabled
        config.analog4OutputEnable.enabled = this.isAnalog4OutputEnabled
        config.relay3OutputEnable.enabled = this.isRelay3OutputEnabled
        config.relay7OutputEnable.enabled = this.isRelay7OutputEnabled
        config.adaptiveDeltaEnable.enabled = this.isAdaptiveDeltaEnabled
        config.maximizedExitWaterTempEnable.enabled = this.ismaximizedExitWaterTempEnable
        config.dcwbEnable.enabled = this.isDcwbEnabled
        config.chilledWaterTargetDelta.currentVal = this.chilledWaterTargetDelta
        config.chilledWaterExitTemperatureMargin.currentVal = this.chilledWaterExitTemperatureMargin
        config.chilledWaterExitTemperatureTarget.currentVal = this.chilledWaterExitTemperatureTarget
        config.chilledWaterMaxFlowRate.currentVal = this.chilledWaterMaxFlowRate

        config.analog1OutputAssociation.associationVal = this.analog1OutputAssociation
        config.analog2OutputAssociation.associationVal = this.analog2OutputAssociation
        config.analog3OutputAssociation.associationVal = this.analog3OutputAssociation
        config.analog4OutputAssociation.associationVal = this.analog4OutputAssociation
        config.relay3Association.associationVal = this.relay3Association
        config.relay7Association.associationVal = this.relay7Association

        config.thermistor1Enabled.enabled = this.thermistor1Enabled
        config.thermistor2Enabled.enabled = this.thermistor2Enabled
        config.analogIn1Enabled.enabled = this.analogIn1Enabled
        config.analogIn2Enabled.enabled = this.analogIn2Enabled
        config.thermistor1InAssociation.associationVal = this.thermistor1Association
        config.thermistor2InAssociation.associationVal = this.thermistor2Association
        config.analogIn1Association.associationVal = this.analogIn1Association
        config.analogIn2Association.associationVal = this.analogIn2Association

        config.analog1ValveClosedPosition.currentVal = this.analog1ValveClosedPosition.toDouble()
        config.analog1ValveFullPosition.currentVal = this.analog1ValveFullPosition.toDouble()
        config.analog1OutMinMaxConfig = this.analog1OutMinMaxConfig
        config.analog2OutMinMaxConfig = this.analog2OutMinMaxConfig
        config.analog3OutMinMaxConfig = this.analog3OutMinMaxConfig
        config.analog4OutMinMaxConfig = this.analog4OutMinMaxConfig

        config.unusedPorts = this.unusedPortState
    }
}