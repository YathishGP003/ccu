package a75f.io.renatus.profiles.hyperstat.util

import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.CpuMinMaxConfig
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.CpuStagedConfig
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HpuMinMaxConfig
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Pipe2Configuration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Pipe2MinMaxConfig
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe4Configuration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Pipe4MinMaxConfig
import a75f.io.logic.bo.building.statprofiles.util.FanConfig
import a75f.io.renatus.profiles.hyperstat.viewstates.CpuAnalogOutMinMaxConfig
import a75f.io.renatus.profiles.hyperstat.viewstates.CpuViewState
import a75f.io.renatus.profiles.hyperstat.viewstates.HpuAnalogOutMinMaxConfig
import a75f.io.renatus.profiles.hyperstat.viewstates.HpuViewState
import a75f.io.renatus.profiles.hyperstat.viewstates.Pipe2AnalogOutMinMaxConfig
import a75f.io.renatus.profiles.hyperstat.viewstates.Pipe2ViewState
import a75f.io.renatus.profiles.hyperstat.viewstates.Pipe4AnalogOutMinMaxConfig
import a75f.io.renatus.profiles.hyperstat.viewstates.Pipe4ViewState
import a75f.io.renatus.profiles.viewstates.FanSpeedConfig
import a75f.io.renatus.profiles.viewstates.StagedConfig
import a75f.io.renatus.profiles.viewstates.updateConfigFromState
import a75f.io.renatus.profiles.viewstates.updateStateFromConfig

/**
 * Created by Manjunath K on 26-09-2024.
 */


class HyperStatViewStateUtil {
    companion object {

        fun cpuStateToConfig(state: CpuViewState, configuration: CpuConfiguration) {
            updateConfigFromState(configuration, state)
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
            val cpuState = CpuViewState()
            updateStateFromConfig(cpuState, configuration)
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


        fun hpuConfigToState(configuration: HpuConfiguration): HpuViewState {

            val hpuState = HpuViewState()
            updateStateFromConfig(hpuState,configuration)
            hpuState.apply {
                configMinMaxToStateHPU(analogOut1MinMax, configuration.analogOut1MinMaxConfig)
                configMinMaxToStateHPU(analogOut2MinMax, configuration.analogOut2MinMaxConfig)
                configMinMaxToStateHPU(analogOut3MinMax, configuration.analogOut3MinMaxConfig)

                configFanState(analogOut1FanConfig, configuration.analogOut1FanSpeedConfig)
                configFanState(analogOut2FanConfig, configuration.analogOut2FanSpeedConfig)
                configFanState(analogOut3FanConfig, configuration.analogOut3FanSpeedConfig)
            }
            return hpuState
        }

        private fun configMinMaxToStateHPU(minMaxState: HpuAnalogOutMinMaxConfig, minMaxConfig: HpuMinMaxConfig) {
            minMaxState.apply {
                compressorConfig.min = minMaxConfig.compressorConfig.min.currentVal.toInt()
                compressorConfig.max = minMaxConfig.compressorConfig.max.currentVal.toInt()
                dcvDamperConfig.min = minMaxConfig.dcvDamperConfig.min.currentVal.toInt()
                dcvDamperConfig.max = minMaxConfig.dcvDamperConfig.max.currentVal.toInt()
                fanSpeedConfig.min = minMaxConfig.fanSpeedConfig.min.currentVal.toInt()
                fanSpeedConfig.max = minMaxConfig.fanSpeedConfig.max.currentVal.toInt()
            }

        }

        fun hpuStateToConfig(state: HpuViewState, configuration: HpuConfiguration) {
            updateConfigFromState(configuration, state)
            configuration.apply {
                minMaxConfigFromStateHPU(analogOut1MinMaxConfig, state.analogOut1MinMax)
                minMaxConfigFromStateHPU(analogOut2MinMaxConfig, state.analogOut2MinMax)
                minMaxConfigFromStateHPU(analogOut3MinMaxConfig, state.analogOut3MinMax)

                fanConfigFromState(analogOut1FanSpeedConfig, state.analogOut1FanConfig)
                fanConfigFromState(analogOut2FanSpeedConfig, state.analogOut2FanConfig)
                fanConfigFromState(analogOut3FanSpeedConfig, state.analogOut3FanConfig)


            }
        }

        private fun minMaxConfigFromStateHPU(minMaxConfig: HpuMinMaxConfig, minMaxState: HpuAnalogOutMinMaxConfig) {
            minMaxConfig.apply {
                compressorConfig.min.currentVal = minMaxState.compressorConfig.min.toDouble()
                compressorConfig.max.currentVal = minMaxState.compressorConfig.max.toDouble()
                fanSpeedConfig.min.currentVal = minMaxState.fanSpeedConfig.min.toDouble()
                fanSpeedConfig.max.currentVal = minMaxState.fanSpeedConfig.max.toDouble()
                dcvDamperConfig.min.currentVal = minMaxState.dcvDamperConfig.min.toDouble()
                dcvDamperConfig.max.currentVal = minMaxState.dcvDamperConfig.max.toDouble()
            }
        }

        fun pipe2ConfigToState(configuration: Pipe2Configuration): Pipe2ViewState {
            val pipe2ViewState = Pipe2ViewState()
            updateStateFromConfig(pipe2ViewState,configuration)
            pipe2ViewState.apply {
                configMinMaxToStatePipe2(analogOut1MinMax, configuration.analogOut1MinMaxConfig)
                configMinMaxToStatePipe2(analogOut2MinMax, configuration.analogOut2MinMaxConfig)
                configMinMaxToStatePipe2(analogOut3MinMax, configuration.analogOut3MinMaxConfig)

                configFanState(analogOut1FanConfig, configuration.analogOut1FanSpeedConfig)
                configFanState(analogOut2FanConfig, configuration.analogOut2FanSpeedConfig)
                configFanState(analogOut3FanConfig, configuration.analogOut3FanSpeedConfig)

                thermistor2EnableConfig.enabled = configuration.thermistor2EnableConfig.enabled
            }
            return pipe2ViewState
        }

        private fun configMinMaxToStatePipe2(minMaxState: Pipe2AnalogOutMinMaxConfig, minMaxConfig: Pipe2MinMaxConfig) {
            minMaxState.apply {
                waterModulatingValue.min = minMaxConfig.waterModulatingValue.min.currentVal.toInt()
                waterModulatingValue.max = minMaxConfig.waterModulatingValue.max.currentVal.toInt()
                dcvDamperConfig.min = minMaxConfig.dcvDamperConfig.min.currentVal.toInt()
                dcvDamperConfig.max = minMaxConfig.dcvDamperConfig.max.currentVal.toInt()
                fanSpeedConfig.min = minMaxConfig.fanSpeedConfig.min.currentVal.toInt()
                fanSpeedConfig.max = minMaxConfig.fanSpeedConfig.max.currentVal.toInt()
            }
        }
        fun pipe2StateToConfig(state: Pipe2ViewState, configuration: Pipe2Configuration) {
            updateConfigFromState(configuration, state)
            configuration.apply {
                minMaxConfigFromStatePipe2(analogOut1MinMaxConfig, state.analogOut1MinMax)
                minMaxConfigFromStatePipe2(analogOut2MinMaxConfig, state.analogOut2MinMax)
                minMaxConfigFromStatePipe2(analogOut3MinMaxConfig, state.analogOut3MinMax)

                fanConfigFromState(analogOut1FanSpeedConfig, state.analogOut1FanConfig)
                fanConfigFromState(analogOut2FanSpeedConfig, state.analogOut2FanConfig)
                fanConfigFromState(analogOut3FanSpeedConfig, state.analogOut3FanConfig)
                thermistor2EnableConfig.enabled = state.thermistor2EnableConfig.enabled

            }
        }
        private fun minMaxConfigFromStatePipe2(minMaxConfig: Pipe2MinMaxConfig, minMaxState: Pipe2AnalogOutMinMaxConfig) {
            minMaxConfig.apply {
                waterModulatingValue.min.currentVal = minMaxState.waterModulatingValue.min.toDouble()
                waterModulatingValue.max.currentVal = minMaxState.waterModulatingValue.max.toDouble()
                fanSpeedConfig.min.currentVal = minMaxState.fanSpeedConfig.min.toDouble()
                fanSpeedConfig.max.currentVal = minMaxState.fanSpeedConfig.max.toDouble()
                dcvDamperConfig.min.currentVal = minMaxState.dcvDamperConfig.min.toDouble()
                dcvDamperConfig.max.currentVal = minMaxState.dcvDamperConfig.max.toDouble()
            }
        }

        private fun configMinMaxToStatePipe4(minMaxState: Pipe4AnalogOutMinMaxConfig, minMaxConfig: Pipe4MinMaxConfig) {
            minMaxState.apply {
                coolingModulatingValue.min = minMaxConfig.coolingModulatingValue.min.currentVal.toInt()
                coolingModulatingValue.max = minMaxConfig.coolingModulatingValue.max.currentVal.toInt()
                heatingModulatingValue.min = minMaxConfig.heatingModulatingValue.min.currentVal.toInt()
                heatingModulatingValue.max = minMaxConfig.heatingModulatingValue.max.currentVal.toInt()
                dcvDamperConfig.min = minMaxConfig.dcvDamperConfig.min.currentVal.toInt()
                dcvDamperConfig.max = minMaxConfig.dcvDamperConfig.max.currentVal.toInt()
                fanSpeedConfig.min = minMaxConfig.fanSpeedConfig.min.currentVal.toInt()
                fanSpeedConfig.max = minMaxConfig.fanSpeedConfig.max.currentVal.toInt()
            }
        }

        fun pipe4ConfigToState(configuration: HsPipe4Configuration): Pipe4ViewState {
            val pipe4ViewState = Pipe4ViewState()
            updateStateFromConfig(pipe4ViewState, configuration)
            pipe4ViewState.apply {
                configMinMaxToStatePipe4(analogOut1MinMax, configuration.analogOut1MinMaxConfig)
                configMinMaxToStatePipe4(analogOut2MinMax, configuration.analogOut2MinMaxConfig)
                configMinMaxToStatePipe4(analogOut3MinMax, configuration.analogOut3MinMaxConfig)

                configFanState(analogOut1FanConfig, configuration.analogOut1FanSpeedConfig)
                configFanState(analogOut2FanConfig, configuration.analogOut2FanSpeedConfig)
                configFanState(analogOut3FanConfig, configuration.analogOut3FanSpeedConfig)
            }
            return pipe4ViewState
        }

        private fun minMaxConfigFromStatePipe4(minMaxConfig: Pipe4MinMaxConfig, minMaxState: Pipe4AnalogOutMinMaxConfig) {
            minMaxConfig.apply {
                coolingModulatingValue.min.currentVal = minMaxState.coolingModulatingValue.min.toDouble()
                coolingModulatingValue.max.currentVal = minMaxState.coolingModulatingValue.max.toDouble()
                heatingModulatingValue.min.currentVal = minMaxState.heatingModulatingValue.min.toDouble()
                heatingModulatingValue.max.currentVal = minMaxState.heatingModulatingValue.max.toDouble()
                fanSpeedConfig.min.currentVal = minMaxState.fanSpeedConfig.min.toDouble()
                fanSpeedConfig.max.currentVal = minMaxState.fanSpeedConfig.max.toDouble()
                dcvDamperConfig.min.currentVal = minMaxState.dcvDamperConfig.min.toDouble()
                dcvDamperConfig.max.currentVal = minMaxState.dcvDamperConfig.max.toDouble()
            }
        }

        fun pipe4StateToConfig(state: Pipe4ViewState, configuration: HsPipe4Configuration) {
            updateConfigFromState(configuration, state)
            configuration.apply {
                minMaxConfigFromStatePipe4(analogOut1MinMaxConfig, state.analogOut1MinMax)
                minMaxConfigFromStatePipe4(analogOut2MinMaxConfig, state.analogOut2MinMax)
                minMaxConfigFromStatePipe4(analogOut3MinMaxConfig, state.analogOut3MinMax)

                fanConfigFromState(analogOut1FanSpeedConfig, state.analogOut1FanConfig)
                fanConfigFromState(analogOut2FanSpeedConfig, state.analogOut2FanConfig)
                fanConfigFromState(analogOut3FanSpeedConfig, state.analogOut3FanConfig)

            }
        }

    }
}

