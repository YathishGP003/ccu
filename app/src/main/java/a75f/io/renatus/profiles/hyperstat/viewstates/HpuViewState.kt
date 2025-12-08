package a75f.io.renatus.profiles.hyperstat.viewstates

import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsHpuAnalogOutMapping
import a75f.io.renatus.profiles.viewstates.FanSpeedConfig
import a75f.io.renatus.profiles.viewstates.MinMaxConfig
import a75f.io.renatus.profiles.viewstates.ProfileViewState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Created by Manjunath K on 26-09-2024.
 */

class HpuViewState: ProfileViewState() {


    var analogOut1MinMax by mutableStateOf(HpuAnalogOutMinMaxConfig(MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)))
    var analogOut2MinMax by mutableStateOf(HpuAnalogOutMinMaxConfig(MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)))
    var analogOut3MinMax by mutableStateOf(HpuAnalogOutMinMaxConfig(MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)))

    var analogOut1FanConfig by mutableStateOf(FanSpeedConfig(70, 80, 100))
    var analogOut2FanConfig by mutableStateOf(FanSpeedConfig(70, 80, 100))
    var analogOut3FanConfig by mutableStateOf(FanSpeedConfig(70, 80, 100))

    override fun isDcvMapped(): Boolean {
        return (analogOut1Enabled && analogOut1Association == HsHpuAnalogOutMapping.DCV_DAMPER.ordinal
                || analogOut2Enabled && analogOut2Association == HsHpuAnalogOutMapping.DCV_DAMPER.ordinal
                || analogOut3Enabled && analogOut3Association == HsHpuAnalogOutMapping.DCV_DAMPER.ordinal)
    }
}

data class HpuAnalogOutMinMaxConfig(
    val compressorConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig,
    val fanSpeedConfig: MinMaxConfig
)