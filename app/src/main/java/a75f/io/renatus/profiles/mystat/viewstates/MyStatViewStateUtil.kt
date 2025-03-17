package a75f.io.renatus.profiles.mystat.viewstates

import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.mystat.configs.MyStatPipe2Configuration
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

                relay1Config = ConfigState(
                    config.relay1Enabled.enabled, config.relay1Association.associationVal
                )
                relay2Config = ConfigState(
                    config.relay2Enabled.enabled, config.relay2Association.associationVal
                )
                relay3Config = ConfigState(
                    config.relay3Enabled.enabled, config.relay3Association.associationVal
                )
                relay4Config = ConfigState(
                    config.relay4Enabled.enabled, config.relay4Association.associationVal
                )

                analogOut1Enabled = config.analogOut1Enabled.enabled
                analogOut1Association = config.analogOut1Association.associationVal

                universalIn1 = ConfigState(
                    config.universalIn1.enabled, config.universalIn1Association.associationVal
                )
            }
        }

        private fun stateToConfig(state: MyStatViewState, configuration: MyStatConfiguration) {
            configuration.apply {
                temperatureOffset.currentVal = state.temperatureOffset
                autoForceOccupied.enabled = state.isEnableAutoForceOccupied
                autoAway.enabled = state.isEnableAutoAway

                relay1Enabled.enabled = state.relay1Config.enabled
                relay2Enabled.enabled = state.relay2Config.enabled
                relay3Enabled.enabled = state.relay3Config.enabled
                relay4Enabled.enabled = state.relay4Config.enabled

                relay1Association.associationVal = state.relay1Config.association
                relay2Association.associationVal = state.relay2Config.association
                relay3Association.associationVal = state.relay3Config.association
                relay4Association.associationVal = state.relay4Config.association

                analogOut1Enabled.enabled = state.analogOut1Enabled
                analogOut1Association.associationVal = state.analogOut1Association

                universalIn1.enabled = state.universalIn1.enabled
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

            viewState.analogOut1FanConfig.apply {
                low = config.analogOut1FanSpeedConfig.low.currentVal.toInt()
                high = config.analogOut1FanSpeedConfig.high.currentVal.toInt()
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
            configuration.analogOut1FanSpeedConfig.apply {
                low.currentVal = state.analogOut1FanConfig.low.toDouble()
                high.currentVal = state.analogOut1FanConfig.high.toDouble()
            }
        }

        fun hpuConfigToState(config: MyStatHpuConfiguration, viewState: MyStatHpuViewState): MyStatHpuViewState {
            configToState(config, viewState) as MyStatHpuViewState
            viewState.analogOut1MinMax.apply {
                compressorSpeed.min =
                    config.analogOut1MinMaxConfig.compressorSpeed.min.currentVal.toInt()
                compressorSpeed.max =
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

            viewState.analogOut1FanConfig.apply {
                low = config.analogOut1FanSpeedConfig.low.currentVal.toInt()
                high = config.analogOut1FanSpeedConfig.high.currentVal.toInt()
            }
            return viewState
        }

        fun hpuStatToConfig(state: MyStatHpuViewState, configuration: MyStatHpuConfiguration) {
            stateToConfig(state, configuration)
            configuration.analogOut1MinMaxConfig.apply {
                compressorSpeed.min.currentVal = state.analogOut1MinMax.compressorSpeed.min.toDouble()
                compressorSpeed.max.currentVal = state.analogOut1MinMax.compressorSpeed.max.toDouble()
                fanSpeedConfig.min.currentVal = state.analogOut1MinMax.fanSpeedConfig.min.toDouble()
                fanSpeedConfig.max.currentVal = state.analogOut1MinMax.fanSpeedConfig.max.toDouble()
                dcvDamperConfig.min.currentVal = state.analogOut1MinMax.dcvDamperConfig.min.toDouble()
                dcvDamperConfig.max.currentVal = state.analogOut1MinMax.dcvDamperConfig.max.toDouble()
            }
            configuration.analogOut1FanSpeedConfig.apply {
                low.currentVal = state.analogOut1FanConfig.low.toDouble()
                high.currentVal = state.analogOut1FanConfig.high.toDouble()
            }
        }


    }

}