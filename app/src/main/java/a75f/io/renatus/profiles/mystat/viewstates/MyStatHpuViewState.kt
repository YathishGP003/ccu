package a75f.io.renatus.profiles.mystat.viewstates

import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuRelayMapping
import a75f.io.renatus.profiles.hyperstatv2.util.MinMaxConfig
import a75f.io.renatus.profiles.hyperstatv2.viewstates.HpuAnalogOutMinMaxConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Created by Manjunath K on 15-01-2025.
 */

class MyStatHpuViewState : MyStatViewState() {

    var analogOut1MinMax by mutableStateOf(
        HpuAnalogOutMinMaxConfig(
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10)
        )
    )
    var analogOut2MinMax by mutableStateOf(
        HpuAnalogOutMinMaxConfig(
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10)
        )
    )

    var analogOut1FanConfig by mutableStateOf(FanSpeedConfig(70, 100))
    var analogOut2FanConfig by mutableStateOf(FanSpeedConfig(70, 100))

    override fun isDcvMapped(): Boolean {
        return (isAnyRelayEnabledAndMapped(MyStatHpuRelayMapping.DCV_DAMPER.ordinal)
                || (universalOut1.enabled && universalOut1.association == MyStatHpuAnalogOutMapping.DCV_DAMPER_MODULATION.ordinal)
                || (universalOut2.enabled && universalOut2.association == MyStatHpuAnalogOutMapping.DCV_DAMPER_MODULATION.ordinal))
    }
}
