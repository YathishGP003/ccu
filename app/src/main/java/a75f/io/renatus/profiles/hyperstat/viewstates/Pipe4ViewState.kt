package a75f.io.renatus.profiles.hyperstat.viewstates

import a75f.io.domain.api.DomainName
import a75f.io.domain.config.EnableConfig
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe4AnalogOutMapping
import a75f.io.renatus.profiles.viewstates.FanSpeedConfig
import a75f.io.renatus.profiles.viewstates.MinMaxConfig
import a75f.io.renatus.profiles.viewstates.ProfileViewState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Created by Manjunath K on 26-09-2024.
 */

class Pipe4ViewState : ProfileViewState() {

    var analogOut1MinMax by mutableStateOf(
        Pipe4AnalogOutMinMaxConfig(
            MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)
        )
    )
    var analogOut2MinMax by mutableStateOf(
        Pipe4AnalogOutMinMaxConfig(
            MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)
        )
    )
    var analogOut3MinMax by mutableStateOf(
        Pipe4AnalogOutMinMaxConfig(
            MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)
        )
    )

    var analogOut1FanConfig by mutableStateOf(FanSpeedConfig(70, 80, 100))
    var analogOut2FanConfig by mutableStateOf(FanSpeedConfig(70, 80, 100))
    var analogOut3FanConfig by mutableStateOf(FanSpeedConfig(70, 80, 100))

    override fun isDcvMapped(): Boolean {
        return (analogOut1Enabled && analogOut1Association == HsPipe4AnalogOutMapping.DCV_DAMPER.ordinal || analogOut2Enabled && analogOut2Association == HsPipe4AnalogOutMapping.DCV_DAMPER.ordinal || analogOut3Enabled && analogOut3Association == HsPipe4AnalogOutMapping.DCV_DAMPER.ordinal)
    }
}

data class Pipe4AnalogOutMinMaxConfig(
    val coolingModulatingValue: MinMaxConfig,
    val heatingModulatingValue: MinMaxConfig,
    val fanSpeedConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)
