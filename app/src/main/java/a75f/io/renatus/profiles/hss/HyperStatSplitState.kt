package a75f.io.renatus.profiles.hss

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

open class HyperStatSplitState {

    var temperatureOffset by mutableStateOf(0.0)

    var autoForceOccupied by mutableStateOf(false)
    var autoAway by mutableStateOf(false)
    var enableOutsideAirOptimization by mutableStateOf(false)
    var prePurge by mutableStateOf(false)

    var sensorAddress0 by mutableStateOf(ConfigState(false, 0))
    var pressureSensorAddress0 by mutableStateOf(ConfigState(false, 0))
    var sensorAddress1 by mutableStateOf(ConfigState(false, 0))
    var sensorAddress2 by mutableStateOf(ConfigState(false, 0))

    var universalIn1Config by mutableStateOf(ConfigState(false, 0))
    var universalIn2Config by mutableStateOf(ConfigState(false, 0))
    var universalIn3Config by mutableStateOf(ConfigState(false, 0))
    var universalIn4Config by mutableStateOf(ConfigState(false, 0))
    var universalIn5Config by mutableStateOf(ConfigState(false, 0))
    var universalIn6Config by mutableStateOf(ConfigState(false, 0))
    var universalIn7Config by mutableStateOf(ConfigState(false, 0))
    var universalIn8Config by mutableStateOf(ConfigState(false, 0))

    var relay1Config by mutableStateOf(ConfigState(false, 0))
    var relay2Config by mutableStateOf(ConfigState(false, 0))
    var relay3Config by mutableStateOf(ConfigState(false, 0))
    var relay4Config by mutableStateOf(ConfigState(false, 0))
    var relay5Config by mutableStateOf(ConfigState(false, 0))
    var relay6Config by mutableStateOf(ConfigState(false, 0))
    var relay7Config by mutableStateOf(ConfigState(false, 0))
    var relay8Config by mutableStateOf(ConfigState(false, 0))

    var analogOut1Enabled by mutableStateOf(false)
    var analogOut2Enabled by mutableStateOf(false)
    var analogOut3Enabled by mutableStateOf(false)
    var analogOut4Enabled by mutableStateOf(false)

    var analogOut1Association by mutableStateOf(0)
    var analogOut2Association by mutableStateOf(0)
    var analogOut3Association by mutableStateOf(0)
    var analogOut4Association by mutableStateOf(0)

    var outsideDamperMinOpenDuringRecirc by mutableStateOf(0.0)
    var outsideDamperMinOpenDuringConditioning by mutableStateOf(0.0)
    var outsideDamperMinOpenDuringFanLow by mutableStateOf(0.0)
    var outsideDamperMinOpenDuringFanMedium by mutableStateOf(0.0)
    var outsideDamperMinOpenDuringFanHigh by mutableStateOf(0.0)

    var exhaustFanStage1Threshold by mutableStateOf(0.0)
    var exhaustFanStage2Threshold by mutableStateOf(0.0)
    var exhaustFanHysteresis by mutableStateOf(0.0)

    var prePurgeOutsideDamperOpen by mutableStateOf(0.0)

    var zoneCO2DamperOpeningRate by mutableStateOf(0.0)
    var zoneCO2Threshold by mutableStateOf(0.0)
    var zoneCO2Target by mutableStateOf(0.0)

    var zonePM2p5Target by mutableStateOf(0.0)

    var displayHumidity by mutableStateOf(false)
    var displayCO2 by mutableStateOf(false)
    var displayPM2p5 by mutableStateOf(false)

    var testStateRelay1 by mutableStateOf(false)
    var testStateRelay2 by mutableStateOf(false)
    var testStateRelay3 by mutableStateOf(false)
    var testStateRelay4 by mutableStateOf(false)
    var testStateRelay5 by mutableStateOf(false)
    var testStateRelay6 by mutableStateOf(false)
    var testStateRelay7 by mutableStateOf(false)
    var testStateRelay8 by mutableStateOf(false)

    var testStateAnalogOut1 by mutableStateOf(0.0)
    var testStateAnalogOut2 by mutableStateOf(0.0)
    var testStateAnalogOut3 by mutableStateOf(0.0)
    var testStateAnalogOut4 by mutableStateOf(0.0)

}

class ConfigState(enabled: Boolean,association: Int) {
    var enabled by mutableStateOf(enabled)
    var association by mutableStateOf(association)
}