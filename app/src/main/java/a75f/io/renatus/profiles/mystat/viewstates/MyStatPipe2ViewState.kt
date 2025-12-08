package a75f.io.renatus.profiles.mystat.viewstates

import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2RelayMapping
import a75f.io.renatus.profiles.hyperstat.viewstates.Pipe2AnalogOutMinMaxConfig
import a75f.io.renatus.profiles.viewstates.MinMaxConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Created by Manjunath K on 15-01-2025.
 */

class MyStatPipe2ViewState: MyStatViewState() {
    var analogOut1MinMax by mutableStateOf(Pipe2AnalogOutMinMaxConfig(MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)))
    var analogOut1FanConfig by mutableStateOf(MsFanSpeedConfig(70 ,100))
    var analogOut2MinMax by mutableStateOf(Pipe2AnalogOutMinMaxConfig(MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)))
    var analogOut2FanConfig by mutableStateOf(MsFanSpeedConfig(70 ,100))

    override fun isDcvMapped(): Boolean {
        return (isAnyRelayEnabledAndMapped(MyStatPipe2RelayMapping.DCV_DAMPER.ordinal)
                || (universalOut1.enabled && universalOut1.association == MyStatPipe2AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal)
                || (universalOut2.enabled && universalOut2.association == MyStatPipe2AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal))
    }
}