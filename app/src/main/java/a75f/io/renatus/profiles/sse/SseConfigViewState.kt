package a75f.io.renatus.profiles.sse

import a75f.io.logic.bo.building.sse.SseProfileConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SseConfigViewState {
    var relay1State = mutableStateOf(false)
    var relay2State = mutableStateOf(false)
    var th1State = mutableStateOf(false)
    var th2State = mutableStateOf(false)
    var autoForcedOccupiedState = mutableStateOf(false)
    var autoAwayState = mutableStateOf(false)
    var analog1InState = mutableStateOf(false)
    var temperatureOffset = mutableStateOf(0.0)

    var relay1AssociationIndex = mutableStateOf(0)
    var relay2AssociationIndex = mutableStateOf(0)
    var analog1InAssociationIndex = mutableStateOf(0)

    var unusedPortState by mutableStateOf(hashMapOf<String, Boolean>())


    companion object {
        fun fromSseProfileConfig(config: SseProfileConfiguration): SseConfigViewState {
            return SseConfigViewState().apply {
                this.temperatureOffset.value = config.temperatureOffset.currentVal
                this.relay1State.value = config.relay1EnabledState.enabled
                this.relay2State.value = config.relay2EnabledState.enabled
                this.th1State.value = config.th1EnabledState.enabled
                this.th2State.value = config.th2EnabledState.enabled
                this.autoForcedOccupiedState.value = config.autoForcedOccupiedEnabledState.enabled
                this.autoAwayState.value = config.autoAwayEnabledState.enabled
                this.analog1InState.value = config.analog1InEnabledState.enabled

                this.relay1AssociationIndex.value = config.relay1Association.associationVal
                this.relay2AssociationIndex.value = config.relay2Association.associationVal
                this.analog1InAssociationIndex.value = config.analog1InAssociation.associationVal
                this.unusedPortState = config.unusedPorts
            }
        }
    }

    fun updateConfigFromViewState(config: SseProfileConfiguration) {
        config.temperatureOffset.currentVal = temperatureOffset.value
        config.relay1EnabledState.enabled = relay1State.value
        config.relay2EnabledState.enabled = relay2State.value
        config.th1EnabledState.enabled = th1State.value
        config.th2EnabledState.enabled = th2State.value
        config.autoForcedOccupiedEnabledState.enabled = autoForcedOccupiedState.value
        config.autoAwayEnabledState.enabled = autoAwayState.value
        config.analog1InEnabledState.enabled = analog1InState.value

        config.relay1Association.associationVal = relay1AssociationIndex.value
        config.relay2Association.associationVal = relay2AssociationIndex.value
        config.analog1InAssociation.associationVal = analog1InAssociationIndex.value

        config.unusedPorts = this.unusedPortState
    }
}