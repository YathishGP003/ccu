package a75f.io.renatus.profiles.mystat.viewstates

import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4RelayMapping
import a75f.io.renatus.profiles.viewstates.MinMaxConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MyStatPipe4ViewState  : MyStatViewState() {

    var analogOut1MinMax by mutableStateOf(
        Pipe4AnalogOutMinMaxConfig(
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10)
        )
    )
    var analogOut1FanConfig by mutableStateOf(MsFanSpeedConfig(70, 100))
    var analogOut2MinMax by mutableStateOf(
        Pipe4AnalogOutMinMaxConfig(
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10),
            MinMaxConfig(2, 10)
        )
    )
    var analogOut2FanConfig by mutableStateOf(MsFanSpeedConfig(70, 100))

    override fun isDcvMapped(): Boolean {
        return (isAnyRelayEnabledAndMapped(MyStatPipe4RelayMapping.DCV_DAMPER.ordinal)
                || (universalOut1.enabled && universalOut1.association == MyStatPipe4AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal)
                || (universalOut2.enabled && universalOut2.association == MyStatPipe4AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal))
    }
}

data class Pipe4AnalogOutMinMaxConfig(
    val hotWaterValve: MinMaxConfig,
    val chilledWaterValve: MinMaxConfig,
    val fanSpeedConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)