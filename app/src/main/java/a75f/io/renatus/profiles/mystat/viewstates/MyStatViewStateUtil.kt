package a75f.io.renatus.profiles.mystat.viewstates

import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2Configuration
import a75f.io.renatus.profiles.hyperstatv2.util.ConfigState

/**
 * Created by Manjunath K on 17-01-2025.
 */

class MyStatViewStateUtil {

    companion object {

        private fun configToState(config: MyStatConfiguration, viewState: MyStatViewState): MyStatViewState {
            return viewState.apply {
                temperatureOffset = config.temperatureOffset.currentVal
                isEnableAutoForceOccupied = config.autoForceOccupied.enabled
                isEnableAutoAway = config.autoAway.enabled
                co2Control = config.enableCo2Display.enabled
                co2Threshold = config.co2Threshold.currentVal
                co2Target = config.co2Target.currentVal
                co2DamperOperatingRate = config.co2DamperOpeningRate.currentVal

                installerPinEnable = config.installerPinEnable.enabled
                conditioningModePinEnable = config.conditioningModePinEnable.enabled

                spaceTemp = config.spaceTemp.enabled
                desiredTemp = config.desiredTemp.enabled

                installerPassword = config.installerPassword.currentVal
                conditioningModePassword = config.conditioningModePassword.currentVal

                relay1Config = ConfigState(
                    config.relay1Enabled.enabled, config.relay1Association.associationVal
                )
                relay2Config = ConfigState(
                    config.relay2Enabled.enabled, config.relay2Association.associationVal
                )
                relay3Config = ConfigState(
                    config.relay3Enabled.enabled, config.relay3Association.associationVal
                )
                universalOut1 = ConfigState(
                    config.universalOut1.enabled, config.universalOut1Association.associationVal
                )
                universalOut2 = ConfigState(
                    config.universalOut2.enabled, config.universalOut2Association.associationVal
                )

                universalIn1 = ConfigState(
                    config.universalIn1Enabled.enabled, config.universalIn1Association.associationVal
                )
            }
        }

        private fun stateToConfig(state: MyStatViewState, configuration: MyStatConfiguration) {
            configuration.apply {
                temperatureOffset.currentVal = state.temperatureOffset
                autoForceOccupied.enabled = state.isEnableAutoForceOccupied
                autoAway.enabled = state.isEnableAutoAway
                enableCo2Display.enabled = state.co2Control
                co2Threshold.currentVal = state.co2Threshold
                co2Target.currentVal = state.co2Target
                co2DamperOpeningRate.currentVal = state.co2DamperOperatingRate

                installerPassword.currentVal = state.installerPassword
                conditioningModePassword.currentVal = state.conditioningModePassword
                installerPinEnable.enabled = state.installerPinEnable
                conditioningModePinEnable.enabled = state.conditioningModePinEnable

                desiredTemp.enabled = state.desiredTemp
                spaceTemp.enabled = state.spaceTemp

                relay1Enabled.enabled = state.relay1Config.enabled
                relay2Enabled.enabled = state.relay2Config.enabled
                relay3Enabled.enabled = state.relay3Config.enabled
                universalOut1.enabled = state.universalOut1.enabled
                universalOut2.enabled = state.universalOut2.enabled

                relay1Association.associationVal = state.relay1Config.association
                relay2Association.associationVal = state.relay2Config.association
                relay3Association.associationVal = state.relay3Config.association
                universalOut1Association.associationVal = state.universalOut1.association
                universalOut2Association.associationVal = state.universalOut2.association
                universalIn1Enabled.enabled = state.universalIn1.enabled
                universalIn1Association.associationVal = state.universalIn1.association
            }
        }

        fun pipe2ConfigToState(config: MyStatPipe2Configuration, viewState: MyStatPipe2ViewState): MyStatPipe2ViewState {
            configToState(config, viewState) as MyStatPipe2ViewState
            viewState.analogOut1MinMax.apply {
                waterModulatingValue.min =
                    config.analogOut1MinMaxConfig.waterModulatingValue.min.currentVal.toInt()
                waterModulatingValue.max =
                    config.analogOut1MinMaxConfig.waterModulatingValue.max.currentVal.toInt()
                fanSpeedConfig.min =
                    config.analogOut1MinMaxConfig.fanSpeedConfig.min.currentVal.toInt()
                fanSpeedConfig.max =
                    config.analogOut1MinMaxConfig.fanSpeedConfig.max.currentVal.toInt()
                dcvDamperConfig.min =
                    config.analogOut1MinMaxConfig.dcvDamperConfig.min.currentVal.toInt()
                dcvDamperConfig.max =
                    config.analogOut1MinMaxConfig.dcvDamperConfig.max.currentVal.toInt()
            }
            viewState.analogOut2MinMax.apply {
                waterModulatingValue.min =
                    config.analogOut2MinMaxConfig.waterModulatingValue.min.currentVal.toInt()
                waterModulatingValue.max =
                    config.analogOut2MinMaxConfig.waterModulatingValue.max.currentVal.toInt()
                fanSpeedConfig.min =
                    config.analogOut2MinMaxConfig.fanSpeedConfig.min.currentVal.toInt()
                fanSpeedConfig.max =
                    config.analogOut2MinMaxConfig.fanSpeedConfig.max.currentVal.toInt()
                dcvDamperConfig.min =
                    config.analogOut2MinMaxConfig.dcvDamperConfig.min.currentVal.toInt()
                dcvDamperConfig.max =
                    config.analogOut2MinMaxConfig.dcvDamperConfig.max.currentVal.toInt()
            }

            viewState.analogOut1FanConfig.apply {
                low = config.analogOut1FanSpeedConfig.low.currentVal.toInt()
                high = config.analogOut1FanSpeedConfig.high.currentVal.toInt()
            }
            viewState.analogOut2FanConfig.apply {
                low = config.analogOut2FanSpeedConfig.low.currentVal.toInt()
                high = config.analogOut2FanSpeedConfig.high.currentVal.toInt()
            }
            return viewState
        }

        fun pipe2StateToConfig(state: MyStatPipe2ViewState, configuration: MyStatPipe2Configuration) {
            stateToConfig(state, configuration)
            configuration.analogOut1MinMaxConfig.apply {
                waterModulatingValue.min.currentVal = state.analogOut1MinMax.waterModulatingValue.min.toDouble()
                waterModulatingValue.max.currentVal = state.analogOut1MinMax.waterModulatingValue.max.toDouble()
                fanSpeedConfig.min.currentVal = state.analogOut1MinMax.fanSpeedConfig.min.toDouble()
                fanSpeedConfig.max.currentVal = state.analogOut1MinMax.fanSpeedConfig.max.toDouble()
                dcvDamperConfig.min.currentVal = state.analogOut1MinMax.dcvDamperConfig.min.toDouble()
                dcvDamperConfig.max.currentVal = state.analogOut1MinMax.dcvDamperConfig.max.toDouble()
            }
            configuration.analogOut2MinMaxConfig.apply {
                waterModulatingValue.min.currentVal = state.analogOut2MinMax.waterModulatingValue.min.toDouble()
                waterModulatingValue.max.currentVal = state.analogOut2MinMax.waterModulatingValue.max.toDouble()
                fanSpeedConfig.min.currentVal = state.analogOut2MinMax.fanSpeedConfig.min.toDouble()
                fanSpeedConfig.max.currentVal = state.analogOut2MinMax.fanSpeedConfig.max.toDouble()
                dcvDamperConfig.min.currentVal = state.analogOut2MinMax.dcvDamperConfig.min.toDouble()
                dcvDamperConfig.max.currentVal = state.analogOut2MinMax.dcvDamperConfig.max.toDouble()
            }
            configuration.analogOut1FanSpeedConfig.apply {
                low.currentVal = state.analogOut1FanConfig.low.toDouble()
                high.currentVal = state.analogOut1FanConfig.high.toDouble()
            }
            configuration.analogOut2FanSpeedConfig.apply {
                low.currentVal = state.analogOut2FanConfig.low.toDouble()
                high.currentVal = state.analogOut2FanConfig.high.toDouble()
            }
        }

        fun cpuConfigToState(config: a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration, viewState: MyStatCpuViewState): MyStatCpuViewState {
            configToState(config, viewState) as MyStatCpuViewState
            viewState.analogOut1MinMax.apply {
                coolingConfig.min =
                    config.analogOut1MinMaxConfig.cooling.min.currentVal.toInt()
                coolingConfig.max =
                    config.analogOut1MinMaxConfig.cooling.max.currentVal.toInt()
                linearFanSpeedConfig.min =
                    config.analogOut1MinMaxConfig.linearFanSpeed.min.currentVal.toInt()
                linearFanSpeedConfig.max =
                    config.analogOut1MinMaxConfig.linearFanSpeed.max.currentVal.toInt()
                heatingConfig.min =
                    config.analogOut1MinMaxConfig.heating.min.currentVal.toInt()
                heatingConfig.max =
                    config.analogOut1MinMaxConfig.heating.max.currentVal.toInt()
                dcvDamperConfig.min =
                    config.analogOut1MinMaxConfig.dcvDamperConfig.min.currentVal.toInt()
                dcvDamperConfig.max =
                    config.analogOut1MinMaxConfig.dcvDamperConfig.max.currentVal.toInt()
            }
            viewState.analogOut2MinMax.apply {
                coolingConfig.min =
                    config.analogOut2MinMaxConfig.cooling.min.currentVal.toInt()
                coolingConfig.max =
                    config.analogOut2MinMaxConfig.cooling.max.currentVal.toInt()
                linearFanSpeedConfig.min =
                    config.analogOut2MinMaxConfig.linearFanSpeed.min.currentVal.toInt()
                linearFanSpeedConfig.max =
                    config.analogOut2MinMaxConfig.linearFanSpeed.max.currentVal.toInt()
                heatingConfig.min =
                    config.analogOut2MinMaxConfig.heating.min.currentVal.toInt()
                heatingConfig.max =
                    config.analogOut2MinMaxConfig.heating.max.currentVal.toInt()
                dcvDamperConfig.min =
                    config.analogOut2MinMaxConfig.dcvDamperConfig.min.currentVal.toInt()
                dcvDamperConfig.max =
                    config.analogOut2MinMaxConfig.dcvDamperConfig.max.currentVal.toInt()
            }

            viewState.coolingStageFanConfig.apply {
                stage1 = config.coolingStageFanConfig.stage1.currentVal.toInt()
                stage2 = config.coolingStageFanConfig.stage2.currentVal.toInt()
            }
            viewState.heatingStageFanConfig.apply {
                stage1 = config.heatingStageFanConfig.stage1.currentVal.toInt()
                stage2 = config.heatingStageFanConfig.stage2.currentVal.toInt()
            }
            viewState.universalOut1recirculateFanConfig = config.universalOut1recircFanConfig.currentVal.toInt()
            viewState.universalOut2recirculateFanConfig = config.universalOut2recircFanConfig.currentVal.toInt()

            viewState.analogOut1FanConfig.apply {
                low = config.analogOut1FanSpeedConfig.low.currentVal.toInt()
                high = config.analogOut1FanSpeedConfig.high.currentVal.toInt()
            }
            viewState.analogOut2FanConfig.apply {
                low = config.analogOut2FanSpeedConfig.low.currentVal.toInt()
                high = config.analogOut2FanSpeedConfig.high.currentVal.toInt()
            }
            return viewState
        }

        fun hpuConfigToState(config: MyStatHpuConfiguration, viewState: MyStatHpuViewState): MyStatHpuViewState {
            configToState(config, viewState) as MyStatHpuViewState
            viewState.analogOut1MinMax.apply {
                compressorConfig.min =
                    config.analogOut1MinMaxConfig.compressorSpeed.min.currentVal.toInt()
                compressorConfig.max =
                    config.analogOut1MinMaxConfig.compressorSpeed.max.currentVal.toInt()
                fanSpeedConfig.min =
                    config.analogOut1MinMaxConfig.fanSpeedConfig.min.currentVal.toInt()
                fanSpeedConfig.max =
                    config.analogOut1MinMaxConfig.fanSpeedConfig.max.currentVal.toInt()
                dcvDamperConfig.min =
                    config.analogOut1MinMaxConfig.dcvDamperConfig.min.currentVal.toInt()
                dcvDamperConfig.max =
                    config.analogOut1MinMaxConfig.dcvDamperConfig.max.currentVal.toInt()
            }
            viewState.analogOut2MinMax.apply {
                compressorConfig.min =
                    config.analogOut2MinMaxConfig.compressorSpeed.min.currentVal.toInt()
                compressorConfig.max =
                    config.analogOut2MinMaxConfig.compressorSpeed.max.currentVal.toInt()
                fanSpeedConfig.min =
                    config.analogOut2MinMaxConfig.fanSpeedConfig.min.currentVal.toInt()
                fanSpeedConfig.max =
                    config.analogOut2MinMaxConfig.fanSpeedConfig.max.currentVal.toInt()
                dcvDamperConfig.min =
                    config.analogOut2MinMaxConfig.dcvDamperConfig.min.currentVal.toInt()
                dcvDamperConfig.max =
                    config.analogOut2MinMaxConfig.dcvDamperConfig.max.currentVal.toInt()
            }

            viewState.analogOut1FanConfig.apply {
                low = config.analogOut1FanSpeedConfig.low.currentVal.toInt()
                high = config.analogOut1FanSpeedConfig.high.currentVal.toInt()
            }
            viewState.analogOut2FanConfig.apply {
                low = config.analogOut2FanSpeedConfig.low.currentVal.toInt()
                high = config.analogOut2FanSpeedConfig.high.currentVal.toInt()
            }
            return viewState
        }

        fun hpuStateToConfig(state: MyStatHpuViewState, configuration: MyStatHpuConfiguration) {
            stateToConfig(state, configuration)
            configuration.analogOut1MinMaxConfig.apply {
                compressorSpeed.min.currentVal = state.analogOut1MinMax.compressorConfig.min.toDouble()
                compressorSpeed.max.currentVal = state.analogOut1MinMax.compressorConfig.max.toDouble()
                fanSpeedConfig.min.currentVal = state.analogOut1MinMax.fanSpeedConfig.min.toDouble()
                fanSpeedConfig.max.currentVal = state.analogOut1MinMax.fanSpeedConfig.max.toDouble()
                dcvDamperConfig.min.currentVal = state.analogOut1MinMax.dcvDamperConfig.min.toDouble()
                dcvDamperConfig.max.currentVal = state.analogOut1MinMax.dcvDamperConfig.max.toDouble()
            }
            configuration.analogOut2MinMaxConfig.apply {
                compressorSpeed.min.currentVal = state.analogOut2MinMax.compressorConfig.min.toDouble()
                compressorSpeed.max.currentVal = state.analogOut2MinMax.compressorConfig.max.toDouble()
                fanSpeedConfig.min.currentVal = state.analogOut2MinMax.fanSpeedConfig.min.toDouble()
                fanSpeedConfig.max.currentVal = state.analogOut2MinMax.fanSpeedConfig.max.toDouble()
                dcvDamperConfig.min.currentVal = state.analogOut2MinMax.dcvDamperConfig.min.toDouble()
                dcvDamperConfig.max.currentVal = state.analogOut2MinMax.dcvDamperConfig.max.toDouble()
            }
            configuration.analogOut1FanSpeedConfig.apply {
                low.currentVal = state.analogOut1FanConfig.low.toDouble()
                high.currentVal = state.analogOut1FanConfig.high.toDouble()
            }
            configuration.analogOut2FanSpeedConfig.apply {
                low.currentVal = state.analogOut2FanConfig.low.toDouble()
                high.currentVal = state.analogOut2FanConfig.high.toDouble()
            }
        }

        fun cpuStateToConfig(state: MyStatCpuViewState, configuration: a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration) {
            stateToConfig(state, configuration)
            configuration.analogOut1MinMaxConfig.apply {
                cooling.min.currentVal = state.analogOut1MinMax.coolingConfig.min.toDouble()
                cooling.max.currentVal = state.analogOut1MinMax.coolingConfig.max.toDouble()
                heating.min.currentVal = state.analogOut1MinMax.heatingConfig.min.toDouble()
                heating.max.currentVal = state.analogOut1MinMax.heatingConfig.max.toDouble()
                linearFanSpeed.min.currentVal = state.analogOut1MinMax.linearFanSpeedConfig.min.toDouble()
                linearFanSpeed.max.currentVal = state.analogOut1MinMax.linearFanSpeedConfig.max.toDouble()
                dcvDamperConfig.min.currentVal = state.analogOut1MinMax.dcvDamperConfig.min.toDouble()
                dcvDamperConfig.max.currentVal = state.analogOut1MinMax.dcvDamperConfig.max.toDouble()
            }
            configuration.analogOut2MinMaxConfig.apply {
                cooling.min.currentVal = state.analogOut2MinMax.coolingConfig.min.toDouble()
                cooling.max.currentVal = state.analogOut2MinMax.coolingConfig.max.toDouble()
                heating.min.currentVal = state.analogOut2MinMax.heatingConfig.min.toDouble()
                heating.max.currentVal = state.analogOut2MinMax.heatingConfig.max.toDouble()
                linearFanSpeed.min.currentVal = state.analogOut2MinMax.linearFanSpeedConfig.min.toDouble()
                linearFanSpeed.max.currentVal = state.analogOut2MinMax.linearFanSpeedConfig.max.toDouble()
                dcvDamperConfig.min.currentVal = state.analogOut2MinMax.dcvDamperConfig.min.toDouble()
                dcvDamperConfig.max.currentVal = state.analogOut2MinMax.dcvDamperConfig.max.toDouble()
            }
            configuration.analogOut1FanSpeedConfig.apply {
                low.currentVal = state.analogOut1FanConfig.low.toDouble()
                high.currentVal = state.analogOut1FanConfig.high.toDouble()
            }
            configuration.analogOut2FanSpeedConfig.apply {
                low.currentVal = state.analogOut2FanConfig.low.toDouble()
                high.currentVal = state.analogOut2FanConfig.high.toDouble()
            }
            configuration.coolingStageFanConfig.apply {
                stage1.currentVal = state.coolingStageFanConfig.stage1.toDouble()
                stage2.currentVal = state.coolingStageFanConfig.stage2.toDouble()
            }
            configuration.heatingStageFanConfig.apply {
                stage1.currentVal = state.heatingStageFanConfig.stage1.toDouble()
                stage2.currentVal = state.heatingStageFanConfig.stage2.toDouble()
            }
            configuration.universalOut1recircFanConfig.currentVal = state.universalOut1recirculateFanConfig.toDouble()
            configuration.universalOut2recircFanConfig.currentVal = state.universalOut2recirculateFanConfig.toDouble()
        }

    }

}