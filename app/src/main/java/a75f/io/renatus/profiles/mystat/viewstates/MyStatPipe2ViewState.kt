package a75f.io.renatus.profiles.mystat.viewstates

import a75f.io.renatus.profiles.hyperstatv2.util.MinMaxConfig
import a75f.io.renatus.profiles.hyperstatv2.viewstates.Pipe2AnalogOutMinMaxConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Created by Manjunath K on 15-01-2025.
 */

class MyStatPipe2ViewState: MyStatViewState() {
    var analogOut1MinMax by mutableStateOf(Pipe2AnalogOutMinMaxConfig(MinMaxConfig(2, 10), MinMaxConfig(2, 10), MinMaxConfig(2, 10)))
    var analogOut1FanConfig by mutableStateOf(FanSpeedConfig(70 ,100))
}