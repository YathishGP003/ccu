package a75f.io.renatus.profiles.system.advancedahu.vav

import a75f.io.logic.bo.building.system.vav.config.VavAdvancedHybridAhuConfig
import a75f.io.renatus.profiles.system.advancedahu.AdvancedHybridAhuState
import a75f.io.renatus.profiles.system.advancedahu.viewstate.configAnalogIn
import a75f.io.renatus.profiles.system.advancedahu.viewstate.configAnalogOut
import a75f.io.renatus.profiles.system.advancedahu.viewstate.configConnectAnalogOut
import a75f.io.renatus.profiles.system.advancedahu.viewstate.configConnectRelay
import a75f.io.renatus.profiles.system.advancedahu.viewstate.configConnectSensorAddress
import a75f.io.renatus.profiles.system.advancedahu.viewstate.configConnectUniversalIn
import a75f.io.renatus.profiles.system.advancedahu.viewstate.configRelay
import a75f.io.renatus.profiles.system.advancedahu.viewstate.configSensorAddress
import a75f.io.renatus.profiles.system.advancedahu.viewstate.configThermistor
import a75f.io.renatus.profiles.system.advancedahu.viewstate.updateAnalogIn
import a75f.io.renatus.profiles.system.advancedahu.viewstate.updateAnalogOut
import a75f.io.renatus.profiles.system.advancedahu.viewstate.updateConnectAnalogOut
import a75f.io.renatus.profiles.system.advancedahu.viewstate.updateConnectDynamicPoints
import a75f.io.renatus.profiles.system.advancedahu.viewstate.updateConnectRelay
import a75f.io.renatus.profiles.system.advancedahu.viewstate.updateConnectSensorAddress
import a75f.io.renatus.profiles.system.advancedahu.viewstate.updateConnectUniversalIn
import a75f.io.renatus.profiles.system.advancedahu.viewstate.updateDynamicPoints
import a75f.io.renatus.profiles.system.advancedahu.viewstate.updateRelay
import a75f.io.renatus.profiles.system.advancedahu.viewstate.updateSensorAddress
import a75f.io.renatus.profiles.system.advancedahu.viewstate.updateThermistor

/**
 * Created by Manjunath K on 02-04-2024.
 */

class VavAdvancedAhuState : AdvancedHybridAhuState() {
    companion object {
        fun fromProfileConfigToState(config: VavAdvancedHybridAhuConfig): VavAdvancedAhuState {
            return VavAdvancedAhuState().apply {
                isConnectEnabled = config.connectConfiguration.connectEnabled
                configSensorAddress(config, this)
                configAnalogIn(config, this)
                configThermistor(config, this)
                configAnalogOut(config, this)
                configRelay(config, this)
                configConnectSensorAddress(config,this)
                configConnectUniversalIn(config,this)
                configConnectAnalogOut(config,this)
                configConnectRelay(config,this)
                configOAOState(config, this)
            }
        }

        private fun configOAOState(config: VavAdvancedHybridAhuConfig, State: AdvancedHybridAhuState) {
            State.analog1MinOaoDamper = config.connectConfiguration.analog1MinOaoDamper.currentVal
            State.analog1MaxOaoDamper = config.connectConfiguration.analog1MaxOaoDamper.currentVal
            State.analog2MinOaoDamper = config.connectConfiguration.analog2MinOaoDamper.currentVal
            State.analog2MaxOaoDamper = config.connectConfiguration.analog2MaxOaoDamper.currentVal
            State.analog3MinOaoDamper = config.connectConfiguration.analog3MinOaoDamper.currentVal
            State.analog3MaxOaoDamper = config.connectConfiguration.analog3MaxOaoDamper.currentVal
            State.analog4MinOaoDamper = config.connectConfiguration.analog4MinOaoDamper.currentVal
            State.analog4MaxOaoDamper = config.connectConfiguration.analog4MaxOaoDamper.currentVal

            State.analog1MinReturnDamper = config.connectConfiguration.analog1MinReturnDamper.currentVal
            State.analog1MaxReturnDamper = config.connectConfiguration.analog1MaxReturnDamper.currentVal
            State.analog2MinReturnDamper = config.connectConfiguration.analog2MinReturnDamper.currentVal
            State.analog2MaxReturnDamper = config.connectConfiguration.analog2MaxReturnDamper.currentVal
            State.analog3MinReturnDamper = config.connectConfiguration.analog3MinReturnDamper.currentVal
            State.analog3MaxReturnDamper = config.connectConfiguration.analog3MaxReturnDamper.currentVal
            State.analog4MinReturnDamper = config.connectConfiguration.analog4MinReturnDamper.currentVal
            State.analog4MaxReturnDamper = config.connectConfiguration.analog4MaxReturnDamper.currentVal

            State.usePerRoomCO2SensingState = config.connectConfiguration.usePerRoomCO2Sensing.enabled
            State.enableOutsideAirOptimization = config.connectConfiguration.enableOutsideAirOptimization.enabled
            State.outsideDamperMinOpenDuringRecirculationPos = config.connectConfiguration.outsideDamperMinOpenDuringRecirculation.currentVal
            State.outsideDamperMinOpenDuringConditioningPos = config.connectConfiguration.outsideDamperMinOpenDuringConditioning.currentVal
            State.outsideDamperMinOpenDuringFanLowPos = config.connectConfiguration.outsideDamperMinOpenDuringFanLow.currentVal
            State.outsideDamperMinOpenDuringFanMediumPos = config.connectConfiguration.outsideDamperMinOpenDuringFanMedium.currentVal
            State.outsideDamperMinOpenDuringFanHighPos = config.connectConfiguration.outsideDamperMinOpenDuringFanHigh.currentVal
            State.returnDamperMinOpenPos = config.connectConfiguration.returnDamperMinOpen.currentVal
            State.exhaustFanStage1ThresholdPos = config.connectConfiguration.exhaustFanStage1Threshold.currentVal
            State.exhaustFanStage2ThresholdPos = config.connectConfiguration.exhaustFanStage2Threshold.currentVal
            State.currentTransformerTypePos = config.connectConfiguration.currentTransformerType.currentVal
            State.oaoCo2ThresholdVal = config.connectConfiguration.co2Threshold.currentVal
            State.exhaustFanHysteresisPos = config.connectConfiguration.exhaustFanHysteresis.currentVal
            State.systemPurgeOutsideDamperMinPos = config.connectConfiguration.systemPurgeOutsideDamperMinPos.currentVal
            State.enhancedVentilationOutsideDamperMinOpenPos = config.connectConfiguration.enhancedVentilationOutsideDamperMinOpen.currentVal

        }

        fun connectConfigToState(config: VavAdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
            config.connectConfiguration.getDefaultConfiguration()
            state.apply {
                configConnectSensorAddress(config, this)
                configConnectUniversalIn(config, this)
                configConnectAnalogOut(config, this)
                configConnectRelay(config, this)
                configOAOState(config, this)
            }
        }
    }

    /**
     * Updates the config from the state
     */
    fun fromStateToProfileConfig(config: VavAdvancedHybridAhuConfig) {
        config.connectConfiguration.connectEnabled = isConnectEnabled

        updateSensorAddress(config, this@VavAdvancedAhuState)
        updateAnalogIn(config, this@VavAdvancedAhuState)
        updateThermistor(config, this@VavAdvancedAhuState)
        updateAnalogOut(config, this@VavAdvancedAhuState)
        updateRelay(config, this@VavAdvancedAhuState)
        updateDynamicPoints(config, this@VavAdvancedAhuState)

        updateConnectSensorAddress(config,this@VavAdvancedAhuState)
        updateConnectUniversalIn(config,this@VavAdvancedAhuState)
        updateConnectAnalogOut(config,this@VavAdvancedAhuState)
        updateConnectRelay(config,this@VavAdvancedAhuState)
        updateConnectDynamicPoints(config,this@VavAdvancedAhuState)
        updateStateOAO(config, this@VavAdvancedAhuState)
    }

    private fun updateStateOAO(config: VavAdvancedHybridAhuConfig, State: VavAdvancedAhuState) {

        config.connectConfiguration.apply {

            this.analog1MinOaoDamper.currentVal = State.analog1MinOaoDamper
            this.analog1MaxOaoDamper.currentVal = State.analog1MaxOaoDamper
            this.analog2MinOaoDamper.currentVal = State.analog2MinOaoDamper
            this.analog2MaxOaoDamper.currentVal = State.analog2MaxOaoDamper
            this.analog3MinOaoDamper.currentVal = State.analog3MinOaoDamper
            this.analog3MaxOaoDamper.currentVal = State.analog3MaxOaoDamper
            this.analog4MinOaoDamper.currentVal = State.analog4MinOaoDamper
            this.analog4MaxOaoDamper.currentVal = State.analog4MaxOaoDamper

            this.analog1MinReturnDamper.currentVal = State.analog1MinReturnDamper
            this.analog1MaxReturnDamper.currentVal = State.analog1MaxReturnDamper
            this.analog2MinReturnDamper.currentVal = State.analog2MinReturnDamper
            this.analog2MaxReturnDamper.currentVal = State.analog2MaxReturnDamper
            this.analog3MinReturnDamper.currentVal = State.analog3MinReturnDamper
            this.analog3MaxReturnDamper.currentVal = State.analog3MaxReturnDamper
            this.analog4MinReturnDamper.currentVal = State.analog4MinReturnDamper
            this.analog4MaxReturnDamper.currentVal = State.analog4MaxReturnDamper

            this.usePerRoomCO2Sensing.enabled = State.usePerRoomCO2SensingState
            this.enableOutsideAirOptimization.enabled = State.enableOutsideAirOptimization
            this.outsideDamperMinOpenDuringRecirculation.currentVal = State.outsideDamperMinOpenDuringRecirculationPos
            this.outsideDamperMinOpenDuringConditioning.currentVal = State.outsideDamperMinOpenDuringConditioningPos
            this.outsideDamperMinOpenDuringFanLow.currentVal = State.outsideDamperMinOpenDuringFanLowPos
            this.outsideDamperMinOpenDuringFanMedium.currentVal = State.outsideDamperMinOpenDuringFanMediumPos
            this.outsideDamperMinOpenDuringFanHigh.currentVal = State.outsideDamperMinOpenDuringFanHighPos
            this.returnDamperMinOpen.currentVal = State.returnDamperMinOpenPos
            this.exhaustFanStage1Threshold.currentVal = State.exhaustFanStage1ThresholdPos
            this.exhaustFanStage2Threshold.currentVal = State.exhaustFanStage2ThresholdPos
            this.currentTransformerType.currentVal = State.currentTransformerTypePos
            this.co2Threshold.currentVal = State.oaoCo2ThresholdVal
            this.exhaustFanHysteresis.currentVal = State.exhaustFanHysteresisPos
            this.systemPurgeOutsideDamperMinPos.currentVal = State.systemPurgeOutsideDamperMinPos
            this.enhancedVentilationOutsideDamperMinOpen.currentVal = State.enhancedVentilationOutsideDamperMinOpenPos

        }


    }

}
