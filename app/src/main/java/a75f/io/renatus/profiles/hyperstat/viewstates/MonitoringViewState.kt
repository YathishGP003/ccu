package a75f.io.renatus.profiles.hyperstat.viewstates

import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.MonitoringConfiguration
import a75f.io.renatus.profiles.viewstates.ProfileViewState

class MonitoringViewState : ProfileViewState() {

    companion object {
        fun fromMonitoringConfigToState(configuration: MonitoringConfiguration): MonitoringViewState {
            return MonitoringViewState().apply {
                this.temperatureOffset = configuration.temperatureOffset.currentVal

                this.thermistor1Config.enabled = configuration.thermistor1Enabled.enabled
                this.thermistor2Config.enabled = configuration.thermistor2Enabled.enabled
                this.analogIn1Config.enabled = configuration.analogIn1Enabled.enabled
                this.analogIn2Config.enabled = configuration.analogIn2Enabled.enabled

                this.thermistor1Config.association =
                    configuration.thermistor1Association.associationVal
                this.thermistor2Config.association =
                    configuration.thermistor2Association.associationVal
                this.analogIn1Config.association = configuration.analogIn1Association.associationVal
                this.analogIn2Config.association = configuration.analogIn2Association.associationVal
                this.zoneCO2Target = configuration.zoneCO2Target.currentVal
                this.zonePM2p5Target = configuration.zonePM2p5Target.currentVal
                this.zonePM10Target = configuration.zonePM10Target.currentVal

                this.displayHumidity = configuration.displayHumidity.enabled
                this.displayCO2 = configuration.displayCO2.enabled
                this.displayPM2p5 = configuration.displayPM2p5.enabled

                this.disableTouch = configuration.disableTouch.enabled
                this.enableBrightness = configuration.enableBrightness.enabled
            }
        }

        fun monitoringStateToConfig(
            state: MonitoringViewState,
            configuration: MonitoringConfiguration
        ) {
            configuration.apply {
                this.temperatureOffset.currentVal = state.temperatureOffset

                this.thermistor1Enabled.enabled = state.thermistor1Config.enabled
                this.thermistor2Enabled.enabled = state.thermistor2Config.enabled
                this.analogIn1Enabled.enabled = state.analogIn1Config.enabled
                this.analogIn2Enabled.enabled = state.analogIn2Config.enabled

                this.thermistor1Association.associationVal = state.thermistor1Config.association
                this.thermistor2Association.associationVal = state.thermistor2Config.association
                this.analogIn1Association.associationVal = state.analogIn1Config.association
                this.analogIn2Association.associationVal = state.analogIn2Config.association
                this.zoneCO2Target.currentVal = state.zoneCO2Target
                this.zonePM2p5Target.currentVal = state.zonePM2p5Target
                this.zonePM10Target.currentVal = state.zonePM10Target

                this.displayHumidity.enabled = state.displayHumidity
                this.displayCO2.enabled = state.displayCO2
                this.displayPM2p5.enabled = state.displayPM2p5

                this.disableTouch.enabled = state.disableTouch
                this.enableBrightness.enabled = state.enableBrightness
            }
        }
    }
}