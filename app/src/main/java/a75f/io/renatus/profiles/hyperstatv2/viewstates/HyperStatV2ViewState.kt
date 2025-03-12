package a75f.io.renatus.profiles.hyperstatv2.viewstates

import a75f.io.renatus.profiles.hyperstatv2.util.ConfigState
import a75f.io.renatus.profiles.hyperstatv2.util.ThresholdTargetConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Created by Manjunath K on 26-09-2024.
 */

open class HyperStatV2ViewState {

    var temperatureOffset by mutableStateOf(0.0)
    var isEnableAutoForceOccupied by mutableStateOf(false)
    var isEnableAutoAway by mutableStateOf(false)

    var relay1Config by mutableStateOf(ConfigState(false, 0))
    var relay2Config by mutableStateOf(ConfigState(false, 0))
    var relay3Config by mutableStateOf(ConfigState(false, 0))
    var relay4Config by mutableStateOf(ConfigState(false, 0))
    var relay5Config by mutableStateOf(ConfigState(false, 0))
    var relay6Config by mutableStateOf(ConfigState(false, 0))

    var analogOut1Enabled by mutableStateOf(false)
    var analogOut2Enabled by mutableStateOf(false)
    var analogOut3Enabled by mutableStateOf(false)

    var analogOut1Association by mutableStateOf(0)
    var analogOut2Association by mutableStateOf(0)
    var analogOut3Association by mutableStateOf(0)

    var analogIn1Config by mutableStateOf(ConfigState(false, 0))
    var analogIn2Config by mutableStateOf(ConfigState(false, 0))

    var thermistor1Config by mutableStateOf(ConfigState(false, 0))
    var thermistor2Config by mutableStateOf(ConfigState(false, 0))

    var co2Config by mutableStateOf(ThresholdTargetConfig(0.0, 0.0))
    var damperOpeningRate by mutableStateOf(0)
    var pm2p5Config by mutableStateOf(ThresholdTargetConfig(0.0, 0.0))
    var pm10Config by mutableStateOf(ThresholdTargetConfig(0.0, 0.0))

    var humidityDisplay by mutableStateOf(false)
    var co2Display by mutableStateOf(false)
    var pm25Display by mutableStateOf(false)

    var testRelay1 by mutableStateOf(false)
    var testRelay2 by mutableStateOf(false)
    var testRelay3 by mutableStateOf(false)
    var testRelay4 by mutableStateOf(false)
    var testRelay5 by mutableStateOf(false)
    var testRelay6 by mutableStateOf(false)

    var testAnalogOut1 by mutableStateOf(0)
    var testAnalogOut2 by mutableStateOf(0)
    var testAnalogOut3 by mutableStateOf(0)

    var disableTouch by mutableStateOf(false)
    var enableBrightness by mutableStateOf(false)

    open fun isDcvMapped() = false

    fun isAnyRelayMapped(mapping: Int, ignoreSelection: ConfigState): Boolean {

        fun checkSelection(config: ConfigState): Boolean {
            config.apply { return (ignoreSelection != this && association == mapping) }
        }

        if (checkSelection(relay1Config)) return true
        if (checkSelection(relay2Config)) return true
        if (checkSelection(relay3Config)) return true
        if (checkSelection(relay4Config)) return true
        if (checkSelection(relay5Config)) return true
        if (checkSelection(relay6Config)) return true

        return false
    }
}