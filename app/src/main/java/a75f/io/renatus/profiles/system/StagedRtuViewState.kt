package a75f.io.renatus.profiles.system

import a75f.io.logic.bo.building.system.vav.config.StagedRtuProfileConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

open class StagedRtuViewState {

    var relay1Enabled by mutableStateOf (false)
    var relay2Enabled by mutableStateOf (false)
    var relay3Enabled by mutableStateOf (false)
    var relay4Enabled by mutableStateOf (false)
    var relay5Enabled by mutableStateOf (false)
    var relay6Enabled by mutableStateOf (false)
    var relay7Enabled by mutableStateOf (false)


    var relay1Association by mutableStateOf (0)
    var relay2Association by mutableStateOf (0)
    var relay3Association by mutableStateOf (0)
    var relay4Association by mutableStateOf (0)
    var relay5Association by mutableStateOf (0)
    var relay6Association by mutableStateOf (0)
    var relay7Association by mutableStateOf (0)

    var relay1Test by mutableStateOf (false)
    var relay2Test by mutableStateOf (false)
    var relay3Test by mutableStateOf (false)
    var relay4Test by mutableStateOf (false)
    var relay5Test by mutableStateOf (false)
    var relay6Test by mutableStateOf (false)
    var relay7Test by mutableStateOf (false)
    var unusedPortState by mutableStateOf(hashMapOf<String, Boolean>())
    var isStateChanged by mutableStateOf(false)
    var isSaveRequired by mutableStateOf(false)
    companion object {
        fun fromProfileConfig(config: StagedRtuProfileConfig): StagedRtuViewState {
            return StagedRtuViewState().apply {
                this.relay1Enabled = config.relay1Enabled.enabled
                this.relay2Enabled = config.relay2Enabled.enabled
                this.relay3Enabled = config.relay3Enabled.enabled
                this.relay4Enabled = config.relay4Enabled.enabled
                this.relay5Enabled = config.relay5Enabled.enabled
                this.relay6Enabled = config.relay6Enabled.enabled
                this.relay7Enabled = config.relay7Enabled.enabled

                this.relay1Association = config.relay1Association.associationVal
                this.relay2Association = config.relay2Association.associationVal
                this.relay3Association = config.relay3Association.associationVal
                this.relay4Association = config.relay4Association.associationVal
                this.relay5Association = config.relay5Association.associationVal
                this.relay6Association = config.relay6Association.associationVal
                this.relay7Association = config.relay7Association.associationVal
                this.unusedPortState = config.unusedPorts
            }
        }
    }

    fun updateConfigFromViewState(config : StagedRtuProfileConfig ) {
        config.relay1Enabled.enabled = this.relay1Enabled
        config.relay2Enabled.enabled = this.relay2Enabled
        config.relay3Enabled.enabled = this.relay3Enabled
        config.relay4Enabled.enabled = this.relay4Enabled
        config.relay5Enabled.enabled = this.relay5Enabled
        config.relay6Enabled.enabled = this.relay6Enabled
        config.relay7Enabled.enabled = this.relay7Enabled

        config.relay1Association.associationVal = this.relay1Association
        config.relay2Association.associationVal = this.relay2Association
        config.relay3Association.associationVal = this.relay3Association
        config.relay4Association.associationVal = this.relay4Association
        config.relay5Association.associationVal = this.relay5Association
        config.relay6Association.associationVal = this.relay6Association
        config.relay7Association.associationVal = this.relay7Association
        config.unusedPorts = this.unusedPortState
    }
}