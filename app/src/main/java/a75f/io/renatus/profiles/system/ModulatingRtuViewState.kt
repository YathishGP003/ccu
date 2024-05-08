package a75f.io.renatus.profiles.system

import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuProfileConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

open class ModulatingRtuViewState {

    var isAnalog1OutputEnabled by mutableStateOf (false)
    var isAnalog2OutputEnabled by mutableStateOf (false)
    var isAnalog3OutputEnabled by mutableStateOf (false)
    var isAnalog4OutputEnabled by mutableStateOf (false)
    var isRelay3OutputEnabled by mutableStateOf (false)
    var isRelay7OutputEnabled by mutableStateOf (false)
    var relay7Association by mutableStateOf (0)

    var analogOut1CoolingMin by mutableStateOf(2)
    var analogOut1CoolingMax by mutableStateOf(10)
    var analogOut2StaticPressureMin by mutableStateOf(2)
    var analogOut2StaticPressureMax by mutableStateOf(10)
    var analogOut3HeatingMin by mutableStateOf(2)
    var analogOut3HeatingMax by mutableStateOf(10)
    var analogOut4FreshAirMin by mutableStateOf(2)
    var analogOut4FreshAirMax by mutableStateOf(10)


    var analogOut1CoolingTestSignal by  mutableStateOf (0.0)
    var analogOut2FanSpeedTestSignal by mutableStateOf (0.0)
    var analogOut3HeatingTestSignal by mutableStateOf (0.0)
    var analogOut4OutSideAirTestSignal by mutableStateOf (0.0)


    companion object {
        fun fromProfileConfig(config: ModulatingRtuProfileConfig): ModulatingRtuViewState {
            return ModulatingRtuViewState().apply {
                this.isAnalog1OutputEnabled = config.analog1OutputEnable.enabled
                this.isAnalog2OutputEnabled = config.analog2OutputEnable.enabled
                this.isAnalog3OutputEnabled = config.analog3OutputEnable.enabled
                this.isAnalog4OutputEnabled = config.analog4OutputEnable.enabled
                this.isRelay3OutputEnabled = config.relay3OutputEnable.enabled
                this.isRelay7OutputEnabled = config.relay7OutputEnable.enabled

                this.relay7Association = config.relay7Association.associationVal

                this.analogOut1CoolingMin = config.analogOut1CoolingMin.currentVal.toInt()
                this.analogOut1CoolingMax = config.analogOut1CoolingMax.currentVal.toInt()
                this.analogOut2StaticPressureMin = config.analogOut2StaticPressureMin.currentVal.toInt()
                this.analogOut2StaticPressureMax = config.analogOut2StaticPressureMax.currentVal.toInt()
                this.analogOut3HeatingMin = config.analogOut3HeatingMin.currentVal.toInt()
                this.analogOut3HeatingMax = config.analogOut3HeatingMax.currentVal.toInt()
                this.analogOut4FreshAirMin = config.analogOut4FreshAirMin.currentVal.toInt()
                this.analogOut4FreshAirMax = config.analogOut4FreshAirMax.currentVal.toInt()
            }
        }
    }


    fun updateConfigFromViewState(config : ModulatingRtuProfileConfig ) {
        config.analog1OutputEnable.enabled = this.isAnalog1OutputEnabled
        config.analog2OutputEnable.enabled = this.isAnalog2OutputEnabled
        config.analog3OutputEnable.enabled = this.isAnalog3OutputEnabled
        config.analog4OutputEnable.enabled = this.isAnalog4OutputEnabled
        config.relay3OutputEnable.enabled = this.isRelay3OutputEnabled
        config.relay7OutputEnable.enabled = this.isRelay7OutputEnabled

        config.relay7Association.associationVal = this.relay7Association

        config.analogOut1CoolingMin.currentVal = this.analogOut1CoolingMin.toDouble()
        config.analogOut1CoolingMax.currentVal = this.analogOut1CoolingMax.toDouble()
        config.analogOut2StaticPressureMin.currentVal = this.analogOut2StaticPressureMin.toDouble()
        config.analogOut2StaticPressureMax.currentVal = this.analogOut2StaticPressureMax.toDouble()
        config.analogOut3HeatingMin.currentVal = this.analogOut3HeatingMin.toDouble()
        config.analogOut3HeatingMax.currentVal = this.analogOut3HeatingMax.toDouble()
        config.analogOut4FreshAirMin.currentVal = this.analogOut4FreshAirMin.toDouble()
        config.analogOut4FreshAirMax.currentVal = this.analogOut4FreshAirMax.toDouble()
    }
}