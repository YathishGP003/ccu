package a75f.io.renatus.profiles.mystat.viewstates

import a75f.io.renatus.profiles.hyperstatv2.util.ConfigState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Created by Manjunath K on 15-01-2025.
 */

open class MyStatViewState {
    var temperatureOffset by mutableStateOf(0.0)
    var isEnableAutoForceOccupied by mutableStateOf(false)
    var isEnableAutoAway by mutableStateOf(false)

    var relay1Config by mutableStateOf(ConfigState(false, 0))
    var relay2Config by mutableStateOf(ConfigState(false, 0))
    var relay3Config by mutableStateOf(ConfigState(false, 0))
    var relay4Config by mutableStateOf(ConfigState(false, 0))

    var analogOut1Enabled by mutableStateOf(false)
    var analogOut1Association by mutableStateOf(0)

    var universalIn1 by mutableStateOf(ConfigState(false, 0))
    var co2Control by mutableStateOf(false)
    var co2Threshold by mutableStateOf(0.0)
    var co2Target by mutableStateOf(0.0)
    var co2DamperOperatingRate by mutableStateOf(0.0)


    open fun isDcvMapped() = false

    fun isAnyRelayEnabledAndMapped(mapping: Int): Boolean {

        fun checkSelection(config: ConfigState): Boolean {
            config.apply { return (enabled && association == mapping) }
        }
        if (checkSelection(relay1Config)) return true
        if (checkSelection(relay2Config)) return true
        if (checkSelection(relay3Config)) return true
        if (checkSelection(relay4Config)) return true

        return false
    }

    fun isAnyRelayMapped(mapping: Int, ignoreSelection: ConfigState): Boolean {

        fun checkSelection(config: ConfigState): Boolean {
            config.apply { return (ignoreSelection != this && association == mapping) }
        }

        if (checkSelection(relay1Config)) return true
        if (checkSelection(relay2Config)) return true
        if (checkSelection(relay3Config)) return true
        if (checkSelection(relay4Config)) return true

        return false
    }
}

class FanSpeedConfig(low: Int, high: Int) {
    var low by mutableStateOf(low)
    var high by mutableStateOf(high)
}

class MyStatStagedConfig(stage1: Int, stage2: Int) {
    var stage1 by mutableStateOf(stage1)
    var stage2 by mutableStateOf(stage2)
}