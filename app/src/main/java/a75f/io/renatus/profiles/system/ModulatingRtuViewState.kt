package a75f.io.renatus.profiles.system

import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuAnalogOutMinMaxConfig
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuAnalogOutMinMaxState
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

    var analog1OutputAssociation by mutableStateOf (0)
    var analog2OutputAssociation by mutableStateOf (0)
    var analog3OutputAssociation by mutableStateOf (0)
    var analog4OutputAssociation by mutableStateOf (0)
    var relay3Association by mutableStateOf (0)
    var relay7Association by mutableStateOf (0)

    var thermistor1Enabled by mutableStateOf(false)
    var thermistor2Enabled by mutableStateOf(false)
    var thermistor1Association by mutableStateOf(0)
    var thermistor2Association by mutableStateOf(0)
    var analogIn1Enabled by mutableStateOf(false)
    var analogIn2Enabled by mutableStateOf(false)
    var analogIn1Association by mutableStateOf(0)
    var analogIn2Association by mutableStateOf(0)

    /*var analogOut1CoolingMin by mutableStateOf(2)
    var analogOut1CoolingMax by mutableStateOf(10)
    var analogOut2StaticPressureMin by mutableStateOf(2)
    var analogOut2StaticPressureMax by mutableStateOf(10)
    var analogOut3HeatingMin by mutableStateOf(2)
    var analogOut3HeatingMax by mutableStateOf(10)
    var analogOut4FreshAirMin by mutableStateOf(2)
    var analogOut4FreshAirMax by mutableStateOf(10)*/

    var unusedPortState by mutableStateOf(hashMapOf<String, Boolean>())
    var analogOut1CoolingTestSignal by  mutableStateOf (0.0)
    var analogOut2FanSpeedTestSignal by mutableStateOf (0.0)
    var analogOut3HeatingTestSignal by mutableStateOf (0.0)
    var analogOut4OutSideAirTestSignal by mutableStateOf (0.0)

    var analog1OutMinMaxConfig by mutableStateOf( ModulatingRtuAnalogOutMinMaxConfig())
    var analog2OutMinMaxConfig by mutableStateOf( ModulatingRtuAnalogOutMinMaxConfig())
    var analog3OutMinMaxConfig by mutableStateOf( ModulatingRtuAnalogOutMinMaxConfig())
    var analog4OutMinMaxConfig by mutableStateOf( ModulatingRtuAnalogOutMinMaxConfig())


    var relay3Test by mutableStateOf (false)
    var relay7Test by mutableStateOf (false)

    var isStateChanged by mutableStateOf(false)
    var isSaveRequired by mutableStateOf(false)

    companion object {
        fun fromProfileConfig(config: ModulatingRtuProfileConfig): ModulatingRtuViewState {
            return ModulatingRtuViewState().apply {
                this.isAnalog1OutputEnabled = config.analog1OutputEnable.enabled
                this.isAnalog2OutputEnabled = config.analog2OutputEnable.enabled
                this.isAnalog3OutputEnabled = config.analog3OutputEnable.enabled
                this.isAnalog4OutputEnabled = config.analog4OutputEnable.enabled
                this.isRelay3OutputEnabled = config.relay3OutputEnable.enabled
                this.isRelay7OutputEnabled = config.relay7OutputEnable.enabled

                this.analog1OutputAssociation = config.analog1OutputAssociation.associationVal
                this.analog2OutputAssociation = config.analog2OutputAssociation.associationVal
                this.analog3OutputAssociation = config.analog3OutputAssociation.associationVal
                this.analog4OutputAssociation = config.analog4OutputAssociation.associationVal
                this.relay3Association = config.relay3Association.associationVal
                this.relay7Association = config.relay7Association.associationVal

                /*this.analogOut1CoolingMin = config.analogOut1CoolingMin.currentVal.toInt()
                this.analogOut1CoolingMax = config.analogOut1CoolingMax.currentVal.toInt()
                this.analogOut2StaticPressureMin = config.analogOut2StaticPressureMin.currentVal.toInt()
                this.analogOut2StaticPressureMax = config.analogOut2StaticPressureMax.currentVal.toInt()
                this.analogOut3HeatingMin = config.analogOut3HeatingMin.currentVal.toInt()
                this.analogOut3HeatingMax = config.analogOut3HeatingMax.currentVal.toInt()
                this.analogOut4FreshAirMin = config.analogOut4FreshAirMin.currentVal.toInt()
                this.analogOut4FreshAirMax = config.analogOut4FreshAirMax.currentVal.toInt()*/

                this.thermistor1Enabled = config.thermistor1Enabled.enabled
                this.thermistor2Enabled = config.thermistor2Enabled.enabled
                this.thermistor1Association = config.thermistor1InAssociation.associationVal
                this.thermistor2Association = config.thermistor2InAssociation.associationVal
                this.analogIn1Enabled = config.analogIn1Enabled.enabled
                this.analogIn2Enabled = config.analogIn2Enabled.enabled
                this.analogIn1Association = config.analogIn1Association.associationVal
                this.analogIn2Association = config.analogIn2Association.associationVal

                this.analog1OutMinMaxConfig = config.analog1OutMinMaxConfig
                this.analog2OutMinMaxConfig = config.analog2OutMinMaxConfig
                this.analog3OutMinMaxConfig = config.analog3OutMinMaxConfig
                this.analog4OutMinMaxConfig = config.analog4OutMinMaxConfig

                this.unusedPortState = config.unusedPorts
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

        config.analog1OutputAssociation.associationVal = this.analog1OutputAssociation
        config.analog2OutputAssociation.associationVal = this.analog2OutputAssociation
        config.analog3OutputAssociation.associationVal = this.analog3OutputAssociation
        config.analog4OutputAssociation.associationVal = this.analog4OutputAssociation
        config.relay3Association.associationVal = this.relay3Association
        config.relay7Association.associationVal = this.relay7Association

        config.thermistor1Enabled.enabled = this.thermistor1Enabled
        config.thermistor2Enabled.enabled = this.thermistor2Enabled
        config.analogIn1Enabled.enabled = this.analogIn1Enabled
        config.analogIn2Enabled.enabled = this.analogIn2Enabled
        config.thermistor1InAssociation.associationVal = this.thermistor1Association
        config.thermistor2InAssociation.associationVal = this.thermistor2Association
        config.analogIn1Association.associationVal = this.analogIn1Association
        config.analogIn2Association.associationVal = this.analogIn2Association
/*
        config.analogOut1CoolingMin.currentVal = this.analogOut1CoolingMin.toDouble()
        config.analogOut1CoolingMax.currentVal = this.analogOut1CoolingMax.toDouble()
        config.analogOut2StaticPressureMin.currentVal = this.analogOut2StaticPressureMin.toDouble()
        config.analogOut2StaticPressureMax.currentVal = this.analogOut2StaticPressureMax.toDouble()
        config.analogOut3HeatingMin.currentVal = this.analogOut3HeatingMin.toDouble()
        config.analogOut3HeatingMax.currentVal = this.analogOut3HeatingMax.toDouble()
        config.analogOut4FreshAirMin.currentVal = this.analogOut4FreshAirMin.toDouble()
        config.analogOut4FreshAirMax.currentVal = this.analogOut4FreshAirMax.toDouble()*/

        config.analog1OutMinMaxConfig = this.analog1OutMinMaxConfig
        config.analog2OutMinMaxConfig = this.analog2OutMinMaxConfig
        config.analog3OutMinMaxConfig = this.analog3OutMinMaxConfig
        config.analog4OutMinMaxConfig = this.analog4OutMinMaxConfig

        config.unusedPorts = this.unusedPortState
    }
}