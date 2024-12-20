package a75f.io.renatus.profiles.hyperstatv2.util

import a75f.io.logic.bo.building.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.CpuMinMaxConfig
import a75f.io.logic.bo.building.hyperstat.v2.configs.CpuStagedConfig
import a75f.io.logic.bo.building.hyperstat.v2.configs.FanConfig
import a75f.io.logic.bo.building.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.renatus.profiles.hyperstatv2.viewstates.CpuAnalogOutMinMaxConfig
import a75f.io.renatus.profiles.hyperstatv2.viewstates.CpuViewState
import a75f.io.renatus.profiles.hyperstatv2.viewstates.HyperStatV2ViewState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Created by Manjunath K on 26-09-2024.
 */

class ConfigState(enabled: Boolean, association: Int) {
    var enabled by mutableStateOf(enabled)
    var association by mutableStateOf(association)
}

class MinMaxConfig(min: Int, max: Int) {
    var min by mutableStateOf(min)
    var max by mutableStateOf(max)
}

class FanSpeedConfig(low: Int, medium: Int, high: Int) {
    var low by mutableStateOf(low)
    var medium by mutableStateOf(medium)
    var high by mutableStateOf(high)
}

class StagedConfig(stage1: Int, stage2: Int, stage3: Int) {
    var stage1 by mutableStateOf(stage1)
    var stage2 by mutableStateOf(stage2)
    var stage3 by mutableStateOf(stage3)
}

class RecirculateConfig(analogOut1: Int, analogOut2: Int, analogOut3: Int) {
    var analogOut1 by mutableStateOf(analogOut1)
    var analogOut2 by mutableStateOf(analogOut2)
    var analogOut3 by mutableStateOf(analogOut3)
}

class ThresholdTargetConfig(threshold: Double, target: Double) {
    var threshold by mutableStateOf(threshold)
    var target by mutableStateOf(target)
}

class HyperStatViewStateUtil {
    companion object {
        private fun hyperStatConfigToState(configuration: HyperStatConfiguration, viewState: HyperStatV2ViewState): HyperStatV2ViewState {
            return viewState.apply {
                temperatureOffset = configuration.temperatureOffset.currentVal
                isEnableAutoForceOccupied = configuration.autoForceOccupied.enabled
                isEnableAutoAway = configuration.autoAway.enabled
                relay1Config = ConfigState(configuration.relay1Enabled.enabled, configuration.relay1Association.associationVal)
                relay2Config = ConfigState(configuration.relay2Enabled.enabled, configuration.relay2Association.associationVal)
                relay3Config = ConfigState(configuration.relay3Enabled.enabled, configuration.relay3Association.associationVal)
                relay4Config = ConfigState(configuration.relay4Enabled.enabled, configuration.relay4Association.associationVal)
                relay5Config = ConfigState(configuration.relay5Enabled.enabled, configuration.relay5Association.associationVal)
                relay6Config = ConfigState(configuration.relay6Enabled.enabled, configuration.relay6Association.associationVal)

                analogOut1Enabled = configuration.analogOut1Enabled.enabled
                analogOut2Enabled = configuration.analogOut2Enabled.enabled
                analogOut3Enabled = configuration.analogOut3Enabled.enabled

                analogOut1Association = configuration.analogOut1Association.associationVal
                analogOut2Association = configuration.analogOut2Association.associationVal
                analogOut3Association = configuration.analogOut3Association.associationVal

                analogIn1Config = ConfigState(configuration.analogIn1Enabled.enabled, configuration.analogIn1Association.associationVal)
                analogIn2Config = ConfigState(configuration.analogIn2Enabled.enabled, configuration.analogIn2Association.associationVal)

                thermistor1Config = ConfigState(configuration.thermistor1Enabled.enabled, configuration.thermistor1Association.associationVal)
                thermistor2Config = ConfigState(configuration.thermistor2Enabled.enabled, configuration.thermistor2Association.associationVal)

                co2Config = ThresholdTargetConfig(configuration.zoneCO2Threshold.currentVal, configuration.zoneCO2Target.currentVal)
                pm2p5Config = ThresholdTargetConfig(configuration.zonePM2p5Threshold.currentVal, configuration.zonePM2p5Target.currentVal)
                pm10Config = ThresholdTargetConfig(0.0, configuration.zonePM10Target.currentVal)
                damperOpeningRate = configuration.zoneCO2DamperOpeningRate.currentVal.toInt()

                humidityDisplay = configuration.displayHumidity.enabled
                co2Display = configuration.displayCO2.enabled
                pm25Display = configuration.displayPM2p5.enabled
            }
        }

        private fun hyperStatStateToConfig(state: HyperStatV2ViewState, configuration: HyperStatConfiguration) {
            configuration.apply {
                temperatureOffset.currentVal = state.temperatureOffset
                autoForceOccupied.enabled = state.isEnableAutoForceOccupied
                autoAway.enabled = state.isEnableAutoAway

                relay1Enabled.enabled = state.relay1Config.enabled
                relay2Enabled.enabled = state.relay2Config.enabled
                relay3Enabled.enabled = state.relay3Config.enabled
                relay4Enabled.enabled = state.relay4Config.enabled
                relay5Enabled.enabled = state.relay5Config.enabled
                relay6Enabled.enabled = state.relay6Config.enabled

                relay1Association.associationVal = state.relay1Config.association
                relay2Association.associationVal = state.relay2Config.association
                relay3Association.associationVal = state.relay3Config.association
                relay4Association.associationVal = state.relay4Config.association
                relay5Association.associationVal = state.relay5Config.association
                relay6Association.associationVal = state.relay6Config.association

                analogOut1Enabled.enabled = state.analogOut1Enabled
                analogOut2Enabled.enabled = state.analogOut2Enabled
                analogOut3Enabled.enabled = state.analogOut3Enabled

                analogOut1Association.associationVal = state.analogOut1Association
                analogOut2Association.associationVal = state.analogOut2Association
                analogOut3Association.associationVal = state.analogOut3Association

                analogIn1Enabled.enabled = state.analogIn1Config.enabled
                analogIn2Enabled.enabled = state.analogIn2Config.enabled

                analogIn1Association.associationVal = state.analogIn1Config.association
                analogIn2Association.associationVal = state.analogIn2Config.association

                thermistor1Enabled.enabled = state.thermistor1Config.enabled
                thermistor2Enabled.enabled = state.thermistor2Config.enabled

                thermistor1Association.associationVal = state.thermistor1Config.association
                thermistor2Association.associationVal = state.thermistor2Config.association

                zonePM2p5Threshold.currentVal = state.pm2p5Config.threshold
                zonePM2p5Target.currentVal = state.pm2p5Config.target

                zonePM10Target.currentVal = state.pm10Config.target

                zoneCO2Threshold.currentVal = state.co2Config.threshold
                zoneCO2Target.currentVal = state.co2Config.target
                zoneCO2DamperOpeningRate.currentVal = state.damperOpeningRate.toDouble()

                displayHumidity.enabled = state.humidityDisplay
                displayCO2.enabled = state.co2Display
                displayPM2p5.enabled = state.pm25Display
            }
        }

        fun cpuStateToConfig(state: CpuViewState, configuration: CpuConfiguration) {
            hyperStatStateToConfig(state, configuration)
            configuration.apply {
                minMaxConfigFromState(analogOut1MinMaxConfig, state.analogOut1MinMax)
                minMaxConfigFromState(analogOut2MinMaxConfig, state.analogOut2MinMax)
                minMaxConfigFromState(analogOut3MinMaxConfig, state.analogOut3MinMax)

                fanConfigFromState(analogOut1FanSpeedConfig, state.analogOut1FanConfig)
                fanConfigFromState(analogOut2FanSpeedConfig, state.analogOut2FanConfig)
                fanConfigFromState(analogOut3FanSpeedConfig, state.analogOut3FanConfig)

                stagedConfigFromState(coolingStageFanConfig, state.coolingStageFanConfig)
                stagedConfigFromState(heatingStageFanConfig, state.heatingStageFanConfig)
                recirculateFanConfig.apply {
                    analogOut1.currentVal = state.recirculateFanConfig.analogOut1.toDouble()
                    analogOut2.currentVal = state.recirculateFanConfig.analogOut2.toDouble()
                    analogOut3.currentVal = state.recirculateFanConfig.analogOut3.toDouble()
                }

            }
        }

        private fun minMaxConfigFromState(minMaxConfig: CpuMinMaxConfig, minMaxState: CpuAnalogOutMinMaxConfig) {
            minMaxConfig.apply {
                coolingConfig.min.currentVal = minMaxState.coolingConfig.min.toDouble()
                coolingConfig.max.currentVal = minMaxState.coolingConfig.max.toDouble()
                linearFanSpeedConfig.min.currentVal = minMaxState.linearFanSpeedConfig.min.toDouble()
                linearFanSpeedConfig.max.currentVal = minMaxState.linearFanSpeedConfig.max.toDouble()
                heatingConfig.min.currentVal = minMaxState.heatingConfig.min.toDouble()
                heatingConfig.max.currentVal = minMaxState.heatingConfig.max.toDouble()
                dcvDamperConfig.min.currentVal = minMaxState.dcvDamperConfig.min.toDouble()
                dcvDamperConfig.max.currentVal = minMaxState.dcvDamperConfig.max.toDouble()
                stagedFanSpeedConfig.min.currentVal = minMaxState.stagedFanSpeedConfig.min.toDouble()
                stagedFanSpeedConfig.max.currentVal = minMaxState.stagedFanSpeedConfig.max.toDouble()
            }
        }

        private fun fanConfigFromState(fanConfig: FanConfig, fanState: FanSpeedConfig) {
            fanConfig.apply {
                low.currentVal = fanState.low.toDouble()
                medium.currentVal = fanState.medium.toDouble()
                high.currentVal = fanState.high.toDouble()
            }
        }

        private fun stagedConfigFromState(stagedConfig: CpuStagedConfig, stagedState: StagedConfig) {
            stagedConfig.apply {
                stage1.currentVal = stagedState.stage1.toDouble()
                stage2.currentVal = stagedState.stage2.toDouble()
                stage3.currentVal = stagedState.stage3.toDouble()
            }
        }


        /**
         * Convert the CpuConfiguration to CpuViewState
         */

        fun cpuConfigToState(configuration: CpuConfiguration): CpuViewState {
            val cpuState = hyperStatConfigToState(configuration, CpuViewState()) as CpuViewState
            cpuState.apply {
                configMinMaxToState(analogOut1MinMax, configuration.analogOut1MinMaxConfig)
                configMinMaxToState(analogOut2MinMax, configuration.analogOut2MinMaxConfig)
                configMinMaxToState(analogOut3MinMax, configuration.analogOut3MinMaxConfig)

                configFanState(analogOut1FanConfig, configuration.analogOut1FanSpeedConfig)
                configFanState(analogOut2FanConfig, configuration.analogOut2FanSpeedConfig)
                configFanState(analogOut3FanConfig, configuration.analogOut3FanSpeedConfig)

                configStagedState(coolingStageFanConfig, configuration.coolingStageFanConfig)
                configStagedState(heatingStageFanConfig, configuration.heatingStageFanConfig)
                recirculateFanConfig.apply {
                    analogOut1 = configuration.recirculateFanConfig.analogOut1.currentVal.toInt()
                    analogOut2 = configuration.recirculateFanConfig.analogOut2.currentVal.toInt()
                    analogOut3 = configuration.recirculateFanConfig.analogOut3.currentVal.toInt()
                }

            }
            return cpuState
        }

        private fun configMinMaxToState(minMaxState: CpuAnalogOutMinMaxConfig, minMaxConfig: CpuMinMaxConfig) {
            minMaxState.apply {
                coolingConfig.min = minMaxConfig.coolingConfig.min.currentVal.toInt()
                coolingConfig.max = minMaxConfig.coolingConfig.max.currentVal.toInt()
                linearFanSpeedConfig.min = minMaxConfig.linearFanSpeedConfig.min.currentVal.toInt()
                linearFanSpeedConfig.max = minMaxConfig.linearFanSpeedConfig.max.currentVal.toInt()
                heatingConfig.min = minMaxConfig.heatingConfig.min.currentVal.toInt()
                heatingConfig.max = minMaxConfig.heatingConfig.max.currentVal.toInt()
                dcvDamperConfig.min = minMaxConfig.dcvDamperConfig.min.currentVal.toInt()
                dcvDamperConfig.max = minMaxConfig.dcvDamperConfig.max.currentVal.toInt()
                stagedFanSpeedConfig.min = minMaxConfig.stagedFanSpeedConfig.min.currentVal.toInt()
                stagedFanSpeedConfig.max = minMaxConfig.stagedFanSpeedConfig.max.currentVal.toInt()
            }

        }

        private fun configFanState(fanState: FanSpeedConfig, fanConfig: FanConfig) {
            fanState.apply {
                low = fanConfig.low.currentVal.toInt()
                medium = fanConfig.medium.currentVal.toInt()
                high = fanConfig.high.currentVal.toInt()
            }
        }

        private fun configStagedState(stagedState: StagedConfig, stagedConfig: CpuStagedConfig) {
            stagedState.apply {
                stage1 = stagedConfig.stage1.currentVal.toInt()
                stage2 = stagedConfig.stage2.currentVal.toInt()
                stage3 = stagedConfig.stage3.currentVal.toInt()
            }
        }

    }
}

