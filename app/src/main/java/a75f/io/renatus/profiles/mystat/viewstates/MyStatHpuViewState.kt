package a75f.io.renatus.profiles.mystat.viewstates

import a75f.io.renatus.profiles.hyperstatv2.util.MinMaxConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Created by Manjunath K on 15-01-2025.
 */

class MyStatHpuViewState: MyStatViewState() {
    var analogOut1MinMax by mutableStateOf(HpuAnalogOutMinMaxConfig(MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)))
    var analogOut1FanConfig by mutableStateOf(FanSpeedConfig(70 ,100))
}
data class HpuAnalogOutMinMaxConfig(
    val compressorSpeed: MinMaxConfig,
    val fanSpeedConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)