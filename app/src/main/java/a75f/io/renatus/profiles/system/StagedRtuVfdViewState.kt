package a75f.io.renatus.profiles.system

import a75f.io.domain.api.Domain
import a75f.io.logic.bo.building.system.vav.config.StagedRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.StagedVfdRtuProfileConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class StagedRtuVfdViewState : StagedRtuViewState() {

    var analogOut2Enabled by mutableStateOf(false)
    //var analogOut2 by mutableStateOf (0)

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

    var analogOut2FanSpeedTestSignal by mutableStateOf (Domain.cmBoardDevice.analog2Out.readHisVal())


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

                this.relay1Association = config.relay1Association.associationVal
                this.relay2Association = config.relay2Association.associationVal
                this.relay3Association = config.relay3Association.associationVal
                this.relay4Association = config.relay4Association.associationVal
                this.relay5Association = config.relay5Association.associationVal
                this.relay6Association = config.relay6Association.associationVal
                this.relay7Association = config.relay7Association.associationVal
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
        config.unusedPorts = this.unusedPortState

    }
}