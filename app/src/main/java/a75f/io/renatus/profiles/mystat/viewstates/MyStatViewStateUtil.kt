package a75f.io.renatus.profiles.mystat.viewstates

import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4Configuration
import a75f.io.renatus.profiles.viewstates.updateConfigFromState
import a75f.io.renatus.profiles.viewstates.updateStateFromConfig

/**
 * Created by Manjunath K on 17-01-2025.
 */

class MyStatViewStateUtil {

    companion object {

        fun pipe2ConfigToState(config: MyStatPipe2Configuration, viewState: MyStatPipe2ViewState): MyStatPipe2ViewState {
            updateStateFromConfig(viewState,config)
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
            updateConfigFromState(configuration,state)
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

        fun cpuConfigToState(config: MyStatCpuConfiguration, viewState: MyStatCpuViewState): MyStatCpuViewState {
            updateStateFromConfig(viewState, config)
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
            updateStateFromConfig(viewState, config)
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
            updateConfigFromState(configuration, state)
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

        fun cpuStateToConfig(state: MyStatCpuViewState, configuration: MyStatCpuConfiguration) {
            updateConfigFromState(configuration, state)
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

        fun pipe4StateToConfig(state: MyStatPipe4ViewState, configuration: MyStatPipe4Configuration
        ) {
            updateConfigFromState(configuration, state)

            configuration.analogOut1MinMaxConfig.apply {
                hotWaterValve.min.currentVal = state.analogOut1MinMax.hotWaterValve.min.toDouble()
                hotWaterValve.max.currentVal = state.analogOut1MinMax.hotWaterValve.max.toDouble()
                chilledWaterValve.min.currentVal =
                    state.analogOut1MinMax.chilledWaterValve.min.toDouble()
                chilledWaterValve.max.currentVal =
                    state.analogOut1MinMax.chilledWaterValve.max.toDouble()
                fanSpeedConfig.min.currentVal = state.analogOut1MinMax.fanSpeedConfig.min.toDouble()
                fanSpeedConfig.max.currentVal = state.analogOut1MinMax.fanSpeedConfig.max.toDouble()
                dcvDamperConfig.min.currentVal =
                    state.analogOut1MinMax.dcvDamperConfig.min.toDouble()
                dcvDamperConfig.max.currentVal =
                    state.analogOut1MinMax.dcvDamperConfig.max.toDouble()
            }

            configuration.analogOut2MinMaxConfig.apply {
                hotWaterValve.min.currentVal = state.analogOut2MinMax.hotWaterValve.min.toDouble()
                hotWaterValve.max.currentVal = state.analogOut2MinMax.hotWaterValve.max.toDouble()
                chilledWaterValve.min.currentVal =
                    state.analogOut2MinMax.chilledWaterValve.min.toDouble()
                chilledWaterValve.max.currentVal =
                    state.analogOut2MinMax.chilledWaterValve.max.toDouble()
                fanSpeedConfig.min.currentVal = state.analogOut2MinMax.fanSpeedConfig.min.toDouble()
                fanSpeedConfig.max.currentVal = state.analogOut2MinMax.fanSpeedConfig.max.toDouble()
                dcvDamperConfig.min.currentVal =
                    state.analogOut2MinMax.dcvDamperConfig.min.toDouble()
                dcvDamperConfig.max.currentVal =
                    state.analogOut2MinMax.dcvDamperConfig.max.toDouble()

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

        fun pipe4ConfigToState(config: MyStatPipe4Configuration, viewState: MyStatPipe4ViewState): MyStatPipe4ViewState {
            updateStateFromConfig(viewState, config)

            viewState.analogOut1MinMax.apply {
                hotWaterValve.min =
                    config.analogOut1MinMaxConfig.hotWaterValve.min.currentVal.toInt()
                hotWaterValve.max =
                    config.analogOut1MinMaxConfig.hotWaterValve.max.currentVal.toInt()
                chilledWaterValve.min =
                    config.analogOut1MinMaxConfig.chilledWaterValve.min.currentVal.toInt()
                chilledWaterValve.max =
                    config.analogOut1MinMaxConfig.chilledWaterValve.max.currentVal.toInt()
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
                hotWaterValve.min =
                    config.analogOut2MinMaxConfig.hotWaterValve.min.currentVal.toInt()
                hotWaterValve.max =
                    config.analogOut2MinMaxConfig.hotWaterValve.max.currentVal.toInt()
                chilledWaterValve.min =
                    config.analogOut2MinMaxConfig.chilledWaterValve.min.currentVal.toInt()
                chilledWaterValve.max =
                    config.analogOut2MinMaxConfig.chilledWaterValve.max.currentVal.toInt()
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


    }
}