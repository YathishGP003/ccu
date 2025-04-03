package a75f.io.renatus.profiles.mystat.viewstates

import a75f.io.logic.bo.building.mystat.configs.MyStatCpuAnalogOutMapping
import a75f.io.logic.bo.building.mystat.configs.MyStatCpuRelayMapping
import a75f.io.logic.bo.building.mystat.configs.MyStatHpuAnalogOutMapping
import a75f.io.logic.bo.building.mystat.configs.MyStatHpuRelayMapping
import a75f.io.renatus.profiles.hyperstatv2.util.MinMaxConfig
import a75f.io.renatus.profiles.hyperstatv2.util.RecirculateConfig
import a75f.io.renatus.profiles.hyperstatv2.viewstates.CpuAnalogOutMinMaxConfig
import a75f.io.renatus.profiles.hyperstatv2.viewstates.HpuAnalogOutMinMaxConfig
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
        return (isAnyRelayEnabledAndMapped(MyStatCpuRelayMapping.DCV_DAMPER.ordinal)
                || (analogOut1Enabled && analogOut1Association == MyStatCpuAnalogOutMapping.DCV_DAMPER.ordinal))
    }
}
