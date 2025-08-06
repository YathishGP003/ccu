package a75f.io.renatus.profiles.hss

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

open class HyperStatSplitState {

    var temperatureOffset by mutableDoubleStateOf(0.0)

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

    var disableTouch by mutableStateOf(false)
    var backLight by mutableStateOf(false)
    var enableBrightness by mutableStateOf(false)

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

    var conditioningModePinEnable by mutableStateOf(false)
    var conditioningModePassword by mutableStateOf("")
    var installerPassword by mutableStateOf("")
    var installerPinEnable by mutableStateOf(false)

    var desiredTemp by mutableStateOf(false)
    var spaceTemp by mutableStateOf(false)

    fun equalsViewState ( state: HyperStatSplitState, otherViewState : HyperStatSplitState) : Boolean {
        return (
                state.temperatureOffset == otherViewState.temperatureOffset &&
                        state.autoForceOccupied == otherViewState.autoForceOccupied &&
                        state.autoAway == otherViewState.autoAway &&
                        state.enableOutsideAirOptimization == otherViewState.enableOutsideAirOptimization &&
                        state.prePurge == otherViewState.prePurge &&

                        state.sensorAddress0.enabled == otherViewState.sensorAddress0.enabled &&
                        state.sensorAddress0.association == otherViewState.sensorAddress0.association &&
                        state.pressureSensorAddress0.enabled == otherViewState.pressureSensorAddress0.enabled &&
                        state.pressureSensorAddress0.association == otherViewState.pressureSensorAddress0.association &&
                        state.sensorAddress1.enabled == otherViewState.sensorAddress1.enabled &&
                        state.sensorAddress1.association == otherViewState.sensorAddress1.association &&
                        state.sensorAddress2.enabled == otherViewState.sensorAddress2.enabled &&
                        state.sensorAddress2.association == otherViewState.sensorAddress2.association &&

                        state.universalIn1Config.enabled == otherViewState.universalIn1Config.enabled &&
                        state.universalIn2Config.enabled == otherViewState.universalIn2Config.enabled &&
                        state.universalIn3Config.enabled == otherViewState.universalIn3Config.enabled &&
                        state.universalIn4Config.enabled == otherViewState.universalIn4Config.enabled &&
                        state.universalIn5Config.enabled == otherViewState.universalIn5Config.enabled &&
                        state.universalIn6Config.enabled == otherViewState.universalIn6Config.enabled &&
                        state.universalIn7Config.enabled == otherViewState.universalIn7Config.enabled &&
                        state.universalIn8Config.enabled == otherViewState.universalIn8Config.enabled &&
                        state.universalIn1Config.association == otherViewState.universalIn1Config.association &&
                        state.universalIn2Config.association == otherViewState.universalIn2Config.association &&
                        state.universalIn3Config.association == otherViewState.universalIn3Config.association &&
                        state.universalIn4Config.association == otherViewState.universalIn4Config.association &&
                        state.universalIn5Config.association == otherViewState.universalIn5Config.association &&
                        state.universalIn6Config.association == otherViewState.universalIn6Config.association &&
                        state.universalIn7Config.association == otherViewState.universalIn7Config.association &&
                        state.universalIn8Config.association == otherViewState.universalIn8Config.association &&

                        state.relay1Config.enabled == otherViewState.relay1Config.enabled &&
                        state.relay2Config.enabled == otherViewState.relay2Config.enabled &&
                        state.relay3Config.enabled == otherViewState.relay3Config.enabled &&
                        state.relay4Config.enabled == otherViewState.relay4Config.enabled &&
                        state.relay5Config.enabled == otherViewState.relay5Config.enabled &&
                        state.relay6Config.enabled == otherViewState.relay6Config.enabled &&
                        state.relay7Config.enabled == otherViewState.relay7Config.enabled &&
                        state.relay8Config.enabled == otherViewState.relay8Config.enabled &&
                        state.relay1Config.association == otherViewState.relay1Config.association &&
                        state.relay2Config.association == otherViewState.relay2Config.association &&
                        state.relay3Config.association == otherViewState.relay3Config.association &&
                        state.relay4Config.association == otherViewState.relay4Config.association &&
                        state.relay5Config.association == otherViewState.relay5Config.association &&
                        state.relay6Config.association == otherViewState.relay6Config.association &&
                        state.relay7Config.association == otherViewState.relay7Config.association &&
                        state.relay8Config.association == otherViewState.relay8Config.association &&

                        state.analogOut1Enabled == otherViewState.analogOut1Enabled &&
                        state.analogOut2Enabled == otherViewState.analogOut2Enabled &&
                        state.analogOut3Enabled == otherViewState.analogOut3Enabled &&
                        state.analogOut4Enabled == otherViewState.analogOut4Enabled &&
                        state.analogOut1Association == otherViewState.analogOut1Association &&
                        state.analogOut2Association == otherViewState.analogOut2Association &&
                        state.analogOut3Association == otherViewState.analogOut3Association &&
                        state.analogOut4Association == otherViewState.analogOut4Association &&

                        state.outsideDamperMinOpenDuringRecirc == otherViewState.outsideDamperMinOpenDuringRecirc &&
                        state.outsideDamperMinOpenDuringConditioning == otherViewState.outsideDamperMinOpenDuringConditioning &&
                        state.outsideDamperMinOpenDuringFanLow == otherViewState.outsideDamperMinOpenDuringFanLow &&
                        state.outsideDamperMinOpenDuringFanMedium == otherViewState.outsideDamperMinOpenDuringFanMedium &&
                        state.outsideDamperMinOpenDuringFanHigh == otherViewState.outsideDamperMinOpenDuringFanHigh &&
                        state.exhaustFanStage1Threshold == otherViewState.exhaustFanStage1Threshold &&
                        state.exhaustFanStage2Threshold == otherViewState.exhaustFanStage2Threshold &&
                        state.exhaustFanHysteresis == otherViewState.exhaustFanHysteresis &&
                        state.prePurgeOutsideDamperOpen == otherViewState.prePurgeOutsideDamperOpen &&
                        state.zoneCO2DamperOpeningRate == otherViewState.zoneCO2DamperOpeningRate &&
                        state.zoneCO2Threshold == otherViewState.zoneCO2Threshold &&
                        state.zoneCO2Target == otherViewState.zoneCO2Target &&
                        state.zonePM2p5Target == otherViewState.zonePM2p5Target &&
                        state.displayHumidity == otherViewState.displayHumidity &&
                        state.displayCO2 == otherViewState.displayCO2 &&
                        state.displayPM2p5 == otherViewState.displayPM2p5 &&
                        state.testStateRelay1 == otherViewState.testStateRelay1 &&
                        state.testStateRelay2 == otherViewState.testStateRelay2 &&
                        state.testStateRelay3 == otherViewState.testStateRelay3 &&
                        state.testStateRelay4 == otherViewState.testStateRelay4 &&
                        state.testStateRelay5 == otherViewState.testStateRelay5 &&
                        state.testStateRelay6 == otherViewState.testStateRelay6 &&
                        state.testStateRelay7 == otherViewState.testStateRelay7 &&
                        state.testStateRelay8 == otherViewState.testStateRelay8 &&
                        state.testStateAnalogOut1 == otherViewState.testStateAnalogOut1 &&
                        state.testStateAnalogOut2 == otherViewState.testStateAnalogOut2 &&
                        state.testStateAnalogOut3 == otherViewState.testStateAnalogOut3 &&
                        state.testStateAnalogOut4 == otherViewState.testStateAnalogOut4 &&
                        state.disableTouch == otherViewState.disableTouch &&
                        state.enableBrightness == otherViewState.enableBrightness &&
                        state.installerPinEnable == otherViewState.installerPinEnable&&
                        state.conditioningModePinEnable == otherViewState.conditioningModePinEnable &&
                        state.conditioningModePassword == otherViewState.conditioningModePassword &&
                        state.installerPassword == otherViewState.installerPassword &&
                        state.backLight == otherViewState.backLight &&
                        state.desiredTemp == otherViewState.desiredTemp &&
                        state.spaceTemp == otherViewState.spaceTemp

                )
    }

}

class ConfigState(enabled: Boolean,association: Int) {
    var enabled by mutableStateOf(enabled)
    var association by mutableStateOf(association)
}