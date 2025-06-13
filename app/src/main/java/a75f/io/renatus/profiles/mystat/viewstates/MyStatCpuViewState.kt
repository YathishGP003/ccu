package a75f.io.renatus.profiles.mystat.viewstates

import a75f.io.renatus.profiles.hyperstatv2.util.MinMaxConfig
import a75f.io.renatus.profiles.hyperstatv2.viewstates.CpuAnalogOutMinMaxConfig
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
            MinMaxConfig(2, 10),
        )
    )
    var analogOut1FanConfig by mutableStateOf(FanSpeedConfig(70, 100))
    var coolingStageFanConfig by mutableStateOf(MyStatStagedConfig(7, 10))
    var heatingStageFanConfig by mutableStateOf(MyStatStagedConfig(7, 10))
    var recirculateFanConfig by mutableStateOf(4)
    override fun isDcvMapped(): Boolean {
        return (isAnyRelayEnabledAndMapped(a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping.DCV_DAMPER.ordinal)
                || (analogOut1Enabled && analogOut1Association == a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping.DCV_DAMPER.ordinal))
    }
}
