package a75f.io.renatus.profiles.mystat.viewstates

import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping
import a75f.io.renatus.profiles.hyperstatv2.util.MinMaxConfig
import a75f.io.renatus.profiles.hyperstatv2.viewstates.CpuAnalogOutMinMaxConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

    var analogOut2MinMax by mutableStateOf(
        CpuAnalogOutMinMaxConfig(
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10),
        )
    )
    var analogOut2FanConfig by mutableStateOf(FanSpeedConfig(70, 100))

    var coolingStageFanConfig by mutableStateOf(MyStatStagedConfig(7, 10))
    var heatingStageFanConfig by mutableStateOf(MyStatStagedConfig(7, 10))
    var universalOut1recirculateFanConfig by mutableIntStateOf(4)
    var universalOut2recirculateFanConfig by mutableIntStateOf(4)
    override fun isDcvMapped(): Boolean {
        return (isAnyRelayEnabledAndMapped(MyStatCpuRelayMapping.DCV_DAMPER.ordinal)
                || (universalOut1.enabled && universalOut1.association == MyStatCpuAnalogOutMapping.DCV_DAMPER_MODULATION.ordinal)
                || (universalOut2.enabled && universalOut2.association == MyStatCpuAnalogOutMapping.DCV_DAMPER_MODULATION.ordinal))
    }
}
