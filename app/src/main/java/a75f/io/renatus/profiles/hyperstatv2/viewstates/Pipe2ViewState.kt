package a75f.io.renatus.profiles.hyperstatv2.viewstates

import a75f.io.domain.api.DomainName
import a75f.io.domain.config.EnableConfig
import a75f.io.renatus.profiles.hyperstatv2.util.FanSpeedConfig
import a75f.io.renatus.profiles.hyperstatv2.util.MinMaxConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Created by Manjunath K on 26-09-2024.
 */

class Pipe2ViewState: HyperStatV2ViewState(){

    var analogOut1MinMax by mutableStateOf(Pipe2AnalogOutMinMaxConfig(MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)))
    var analogOut2MinMax by mutableStateOf(Pipe2AnalogOutMinMaxConfig(MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)))
    var analogOut3MinMax by mutableStateOf(Pipe2AnalogOutMinMaxConfig(MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)))

    var analogOut1FanConfig by mutableStateOf(FanSpeedConfig(70, 80, 100))
    var analogOut2FanConfig by mutableStateOf(FanSpeedConfig(70, 80, 100))
    var analogOut3FanConfig by mutableStateOf(FanSpeedConfig(70, 80, 100))

    var thermistor2EnableConfig by mutableStateOf(EnableConfig(DomainName.thermistor2InputEnable,true ))

    override fun isDcvMapped(): Boolean {
        return (analogOut1Enabled && analogOut1Association == a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe2AnalogOutMapping.DCV_DAMPER.ordinal
                || analogOut2Enabled && analogOut2Association == a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe2AnalogOutMapping.DCV_DAMPER.ordinal
                || analogOut3Enabled && analogOut3Association == a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe2AnalogOutMapping.DCV_DAMPER.ordinal)
    }
}
data class Pipe2AnalogOutMinMaxConfig(
    val waterModulatingValue: MinMaxConfig,
    val fanSpeedConfig: MinMaxConfig,
    val dcvDamperConfig: MinMaxConfig
)
