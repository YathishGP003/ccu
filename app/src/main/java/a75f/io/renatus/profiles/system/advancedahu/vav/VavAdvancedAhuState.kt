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
            }
        }

        fun connectConfigToState(config: VavAdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
            config.connectConfiguration.getDefaultConfiguration()
            state.apply {
                configConnectSensorAddress(config, this)
                configConnectUniversalIn(config, this)
                configConnectAnalogOut(config, this)
                configConnectRelay(config, this)
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
    }

}
