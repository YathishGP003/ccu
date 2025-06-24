package a75f.io.renatus.profiles.system

import a75f.io.logic.bo.building.system.vav.config.StagedVfdRtuProfileConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class StagedRtuVfdViewState : StagedRtuViewState() {

    var analogOut2Enabled by mutableStateOf(false)
    var analogOut2Association by mutableStateOf (0)

    var thermistor1Enabled by mutableStateOf(false)
    var thermistor2Enabled by mutableStateOf(false)
    var thermistor1Association by mutableStateOf(0)
    var thermistor2Association by mutableStateOf(0)

    var analogIn1Enabled by mutableStateOf(false)
    var analogIn2Enabled by mutableStateOf(false)
    var analogIn1Association by mutableStateOf(0)
    var analogIn2Association by mutableStateOf(0)

    var analogOut2Economizer by mutableStateOf(0)
    var analogOut2Recirculate by mutableStateOf(0)
    var analogOut2CoolStage1 by mutableStateOf(0)
    var analogOut2CoolStage2 by mutableStateOf(0)
    var analogOut2CoolStage3 by mutableStateOf(0)
    var analogOut2CoolStage4 by mutableStateOf(0)
    var analogOut2CoolStage5 by mutableStateOf(0)
    var analogOut2HeatStage1 by mutableStateOf(0)
    var analogOut2HeatStage2 by mutableStateOf(0)
    var analogOut2HeatStage3 by mutableStateOf(0)
    var analogOut2HeatStage4 by mutableStateOf(0)
    var analogOut2HeatStage5 by mutableStateOf(0)
    var analogOut2Default by mutableStateOf(0)

    var analogOut2MinCompressor by mutableStateOf(2)
    var analogOut2MaxCompressor by mutableStateOf(10)

    var analogOut2MinDamperModulation by mutableStateOf(2)
    var analogOut2MaxDamperModulation by mutableStateOf(10)

    var analogOut2FanSpeedTestSignal by mutableStateOf (0.0)


    companion object {
        fun fromProfileConfig(config: StagedVfdRtuProfileConfig): StagedRtuVfdViewState {
            return StagedRtuVfdViewState().apply {
                this.relay1Enabled = config.relay1Enabled.enabled
                this.relay2Enabled = config.relay2Enabled.enabled
                this.relay3Enabled = config.relay3Enabled.enabled
                this.relay4Enabled = config.relay4Enabled.enabled
                this.relay5Enabled = config.relay5Enabled.enabled
                this.relay6Enabled = config.relay6Enabled.enabled
                this.relay7Enabled = config.relay7Enabled.enabled
                this.analogOut2Enabled = config.analogOut2Enabled.enabled
                this.analogOut2Association = config.analogOut2Association.associationVal

                this.thermistor1Enabled = config.thermistor1Enabled.enabled
                this.thermistor2Enabled = config.thermistor2Enabled.enabled
                this.thermistor1Association = config.thermistor1InAssociation.associationVal
                this.thermistor2Association = config.thermistor2InAssociation.associationVal
                this.analogIn1Enabled = config.analogIn1Enabled.enabled
                this.analogIn2Enabled = config.analogIn2Enabled.enabled
                this.analogIn1Association = config.analogIn1Association.associationVal
                this.analogIn2Association = config.analogIn2Association.associationVal

                this.relay1Association = config.relay1Association.associationVal
                this.relay2Association = config.relay2Association.associationVal
                this.relay3Association = config.relay3Association.associationVal
                this.relay4Association = config.relay4Association.associationVal
                this.relay5Association = config.relay5Association.associationVal
                this.relay6Association = config.relay6Association.associationVal
                this.relay7Association = config.relay7Association.associationVal
                this.analogOut2Association = config.analogOut2Association.associationVal
                this.analogOut2Economizer = config.analogOut2Economizer.currentVal.toInt()
                this.analogOut2Recirculate = config.analogOut2Recirculate.currentVal.toInt()
                this.analogOut2CoolStage1 = config.analogOut2CoolStage1.currentVal.toInt()
                this.analogOut2CoolStage2 = config.analogOut2CoolStage2.currentVal.toInt()
                this.analogOut2CoolStage3 = config.analogOut2CoolStage3.currentVal.toInt()
                this.analogOut2CoolStage4 = config.analogOut2CoolStage4.currentVal.toInt()
                this.analogOut2CoolStage5 = config.analogOut2CoolStage5.currentVal.toInt()
                this.analogOut2HeatStage1 = config.analogOut2HeatStage1.currentVal.toInt()
                this.analogOut2HeatStage2 = config.analogOut2HeatStage2.currentVal.toInt()
                this.analogOut2HeatStage3 = config.analogOut2HeatStage3.currentVal.toInt()
                this.analogOut2HeatStage4 = config.analogOut2HeatStage4.currentVal.toInt()
                this.analogOut2HeatStage5 = config.analogOut2HeatStage5.currentVal.toInt()
                this.analogOut2Default = config.analogOut2Default.currentVal.toInt()
                this.analogOut2MinCompressor = config.analogOut2MinCompressor.currentVal.toInt()
                this.analogOut2MaxCompressor = config.analogOut2MaxCompressor.currentVal.toInt()
                this.analogOut2MinDamperModulation = config.analogOut2MinDamperModulation.currentVal.toInt()
                this.analogOut2MaxDamperModulation = config.analogOut2MaxDamperModulation.currentVal.toInt()
                this.unusedPortState = config.unusedPorts
            }
        }
    }

    fun updateConfigFromViewState(config: StagedVfdRtuProfileConfig) {
        config.relay1Enabled.enabled = this.relay1Enabled
        config.relay2Enabled.enabled = this.relay2Enabled
        config.relay3Enabled.enabled = this.relay3Enabled
        config.relay4Enabled.enabled = this.relay4Enabled
        config.relay5Enabled.enabled = this.relay5Enabled
        config.relay6Enabled.enabled = this.relay6Enabled
        config.relay7Enabled.enabled = this.relay7Enabled
        config.analogOut2Enabled.enabled = this.analogOut2Enabled
        config.analogOut2Association.associationVal = this.analogOut2Association
        config.thermistor1Enabled.enabled = this.thermistor1Enabled
        config.thermistor2Enabled.enabled = this.thermistor2Enabled
        config.analogIn1Enabled.enabled = this.analogIn1Enabled
        config.analogIn2Enabled.enabled = this.analogIn2Enabled
        config.thermistor1InAssociation.associationVal = this.thermistor1Association
        config.thermistor2InAssociation.associationVal = this.thermistor2Association
        config.analogIn1Association.associationVal = this.analogIn1Association
        config.analogIn2Association.associationVal = this.analogIn2Association

        config.relay1Association.associationVal = this.relay1Association
        config.relay2Association.associationVal = this.relay2Association
        config.relay3Association.associationVal = this.relay3Association
        config.relay4Association.associationVal = this.relay4Association
        config.relay5Association.associationVal = this.relay5Association
        config.relay6Association.associationVal = this.relay6Association
        config.relay7Association.associationVal = this.relay7Association
        config.analogOut2Economizer.currentVal = this.analogOut2Economizer.toDouble()
        config.analogOut2Recirculate.currentVal = this.analogOut2Recirculate.toDouble()
        config.analogOut2CoolStage1.currentVal = this.analogOut2CoolStage1.toDouble()
        config.analogOut2CoolStage2.currentVal = this.analogOut2CoolStage2.toDouble()
        config.analogOut2CoolStage3.currentVal = this.analogOut2CoolStage3.toDouble()
        config.analogOut2CoolStage4.currentVal = this.analogOut2CoolStage4.toDouble()
        config.analogOut2CoolStage5.currentVal = this.analogOut2CoolStage5.toDouble()
        config.analogOut2HeatStage1.currentVal = this.analogOut2HeatStage1.toDouble()
        config.analogOut2HeatStage2.currentVal = this.analogOut2HeatStage2.toDouble()
        config.analogOut2HeatStage3.currentVal = this.analogOut2HeatStage3.toDouble()
        config.analogOut2HeatStage4.currentVal = this.analogOut2HeatStage4.toDouble()
        config.analogOut2HeatStage5.currentVal = this.analogOut2HeatStage5.toDouble()
        config.analogOut2Default.currentVal = this.analogOut2Default.toDouble()
        config.analogOut2MinCompressor.currentVal = this.analogOut2MinCompressor.toDouble()
        config.analogOut2MaxCompressor.currentVal = this.analogOut2MaxCompressor.toDouble()
        config.analogOut2MinDamperModulation.currentVal = this.analogOut2MinDamperModulation.toDouble()
        config.analogOut2MaxDamperModulation.currentVal = this.analogOut2MaxDamperModulation.toDouble()
        config.unusedPorts = this.unusedPortState

    }
}