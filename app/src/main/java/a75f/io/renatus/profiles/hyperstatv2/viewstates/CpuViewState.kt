package a75f.io.renatus.profiles.hyperstatv2.viewstates

import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsCpuAnalogOutMapping
import a75f.io.renatus.profiles.hyperstatv2.util.FanSpeedConfig
import a75f.io.renatus.profiles.hyperstatv2.util.MinMaxConfig
import a75f.io.renatus.profiles.hyperstatv2.util.RecirculateConfig
import a75f.io.renatus.profiles.hyperstatv2.util.StagedConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Created by Manjunath K on 26-09-2024.
 */

class CpuViewState : HyperStatV2ViewState() {


    var analogOut1MinMax by mutableStateOf(CpuAnalogOutMinMaxConfig(MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)))
    var analogOut2MinMax by mutableStateOf(CpuAnalogOutMinMaxConfig(MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)))
    var analogOut3MinMax by mutableStateOf(CpuAnalogOutMinMaxConfig(MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)))

    var analogOut1FanConfig by mutableStateOf(FanSpeedConfig(70, 80, 100))
    var analogOut2FanConfig by mutableStateOf(FanSpeedConfig(70, 80, 100))
    var analogOut3FanConfig by mutableStateOf(FanSpeedConfig(70, 80, 100))

    var coolingStageFanConfig by mutableStateOf(StagedConfig(7, 10, 10))
    var heatingStageFanConfig by mutableStateOf(StagedConfig(7, 10, 10))
    var recirculateFanConfig by mutableStateOf(RecirculateConfig(4,4,4))

    override fun isDcvMapped(): Boolean {
        return (analogOut1Enabled && analogOut1Association == HsCpuAnalogOutMapping.DCV_DAMPER.ordinal
                || analogOut2Enabled && analogOut2Association == HsCpuAnalogOutMapping.DCV_DAMPER.ordinal
                || analogOut3Enabled && analogOut3Association == HsCpuAnalogOutMapping.DCV_DAMPER.ordinal)
    }
}


data class CpuAnalogOutMinMaxConfig(
        val coolingConfig: MinMaxConfig,
        val linearFanSpeedConfig: MinMaxConfig,
        val heatingConfig: MinMaxConfig,
        val dcvDamperConfig: MinMaxConfig,
        val stagedFanSpeedConfig: MinMaxConfig
)