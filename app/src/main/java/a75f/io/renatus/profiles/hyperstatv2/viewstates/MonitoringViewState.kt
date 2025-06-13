package a75f.io.renatus.profiles.hyperstatv2.viewstates

import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.MonitoringConfiguration

class MonitoringViewState : HyperStatV2ViewState() {

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
                this.co2Config.target = configuration.zoneCO2Target.currentVal
                this.pm2p5Config.target = configuration.zonePM2p5Target.currentVal
                this.pm10Config.target = configuration.zonePM10Target.currentVal

                this.humidityDisplay = configuration.displayHumidity.enabled
                this.co2Display = configuration.displayCO2.enabled
                this.pm25Display = configuration.displayPM2p5.enabled

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
                this.zoneCO2Target.currentVal = state.co2Config.target
                this.zonePM2p5Target.currentVal = state.pm2p5Config.target
                this.zonePM10Target.currentVal = state.pm10Config.target

                this.displayHumidity.enabled = state.humidityDisplay
                this.displayCO2.enabled = state.co2Display
                this.displayPM2p5.enabled = state.pm25Display

                this.disableTouch.enabled = state.disableTouch
                this.enableBrightness.enabled = state.enableBrightness
            }
        }
    }
}