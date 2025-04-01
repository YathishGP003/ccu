package a75f.io.renatus.profiles.mystat.viewstates

import a75f.io.renatus.profiles.hyperstatv2.util.MinMaxConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Created by Manjunath K on 15-01-2025.
 */

class MyStatCpuViewState : MyStatViewState() {
    var analogOut1MinMax by mutableStateOf(
        CpuAnalogOutMinMaxConfig(
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10),
        )
    )
    var analogOut1FanConfig by mutableStateOf(FanSpeedConfig(70, 100))
    // val stagedConfig by mutableStateOf(StagedConfig(7, 10, 10)) // TODO add staged config
}

data class CpuAnalogOutMinMaxConfig(
    val coolingConfig: MinMaxConfig,
    val heatingConfig: MinMaxConfig,
    val linearFanSpeedConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)