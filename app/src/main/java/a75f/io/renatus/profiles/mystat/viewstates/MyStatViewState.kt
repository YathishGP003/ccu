package a75f.io.renatus.profiles.mystat.viewstates

import a75f.io.renatus.profiles.viewstates.ConfigState
import a75f.io.renatus.profiles.viewstates.ProfileViewState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Created by Manjunath K on 15-01-2025.
 */

open class MyStatViewState: ProfileViewState() {

    fun isAnyRelayEnabledAndMapped(mapping: Int): Boolean {

        fun checkSelection(config: ConfigState): Boolean {
            config.apply { return (enabled && association == mapping) }
        }
        if (checkSelection(relay1Config)) return true
        if (checkSelection(relay2Config)) return true
        if (checkSelection(relay3Config)) return true
        if (checkSelection(universalOut1)) return true
        if (checkSelection(universalOut2)) return true

        return false
    }

    override fun isAnyRelayMapped(mapping: Int, ignoreSelection: ConfigState): Boolean {

        fun checkSelection(config: ConfigState): Boolean {
            config.apply { return (ignoreSelection != this && association == mapping) }
        }

        if (checkSelection(relay1Config)) return true
        if (checkSelection(relay2Config)) return true
        if (checkSelection(relay3Config)) return true
        if (checkSelection(universalOut1)) return true
        if (checkSelection(universalOut2)) return true

        return false
    }
}

class MsFanSpeedConfig(low: Int, high: Int) {
    var low by mutableIntStateOf(low)
    var high by mutableIntStateOf(high)
}

class MyStatStagedConfig(stage1: Int, stage2: Int) {
    var stage1 by mutableIntStateOf(stage1)
    var stage2 by mutableIntStateOf(stage2)
}