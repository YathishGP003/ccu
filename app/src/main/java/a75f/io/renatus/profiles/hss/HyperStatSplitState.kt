package a75f.io.renatus.profiles.hss

import a75f.io.renatus.profiles.viewstates.ProfileViewState

open class HyperStatSplitState: ProfileViewState() {

    fun equalsViewState ( state: HyperStatSplitState, otherViewState : HyperStatSplitState) : Boolean {
        return (
                state.temperatureOffset == otherViewState.temperatureOffset &&
                        state.isEnableAutoForceOccupied == otherViewState.isEnableAutoForceOccupied &&
                        state.isEnableAutoAway == otherViewState.isEnableAutoAway &&
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
