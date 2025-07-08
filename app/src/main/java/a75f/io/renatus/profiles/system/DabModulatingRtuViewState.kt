package a75f.io.renatus.profiles.system

import a75f.io.logic.bo.building.system.vav.config.DabModulatingRtuProfileConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DabModulatingRtuViewState : ModulatingRtuViewState(){

   /* var isAnalog1OutputEnabled by mutableStateOf (false)
    var isAnalog2OutputEnabled by mutableStateOf (false)
    var isAnalog3OutputEnabled by mutableStateOf (false)
    var isAnalog4OutputEnabled by mutableStateOf (false)
    var isRelay3OutputEnabled by mutableStateOf (false)
    var isRelay7OutputEnabled by mutableStateOf (false)*/
    var isAdaptiveDeltaEnabled by mutableStateOf (false)
    var ismaximizedExitWaterTempEnable by mutableStateOf (false)
    var isDcwbEnabled by mutableStateOf (false)
    //var relay7Association by mutableStateOf (0)
    //var analog4Association by mutableStateOf (0)

    /*var analogOut1CoolingMin by mutableStateOf(2)
    var analogOut1CoolingMax by mutableStateOf(10)
    var analogOut2StaticPressureMin by mutableStateOf(2)
    var analogOut2StaticPressureMax by mutableStateOf(10)
    var analogOut4FreshAirMin by mutableStateOf(2)
    var analogOut4FreshAirMax by mutableStateOf(10)*/
    var chilledWaterTargetDelta by mutableStateOf(15.0)
    var chilledWaterExitTemperatureMargin by mutableStateOf(4.0)
    var chilledWaterExitTemperatureTarget by mutableStateOf(4.0)
    var chilledWaterMaxFlowRate by mutableStateOf(100.0)
    var analog1ValveClosedPosition by mutableStateOf(2)
    var analog1ValveFullPosition by mutableStateOf(10)
    var analog2MinFan by mutableStateOf(2)
    var analog2MaxFan by mutableStateOf(10)
    var analog3MinHeating by mutableStateOf(2)
    var analog3MaxHeating by mutableStateOf(10)
    var analogOut4MinCoolingLoop by mutableStateOf(2)
    var analogOut4MaxCoolingLoop by mutableStateOf(10)

    /*var unusedPortState by mutableStateOf(hashMapOf<String, Boolean>())
    var analogOut1CoolingTestSignal by  mutableStateOf (0.0)
    var analogOut2FanSpeedTestSignal by mutableStateOf (0.0)
    var analogOut3HeatingTestSignal by mutableStateOf (0.0)
    var analogOut4OutSideAirTestSignal by mutableStateOf (0.0)

    var relay3Test by mutableStateOf (false)
    var relay7Test by mutableStateOf (false)

    var isStateChanged by mutableStateOf(false)
    var isSaveRequired by mutableStateOf(false)*/

    var chilledWaterMaxFlowRateInc by mutableStateOf(1.0)

    companion object {
        fun fromProfileConfig(config: DabModulatingRtuProfileConfig): DabModulatingRtuViewState {
            return DabModulatingRtuViewState().apply {
                this.isAnalog1OutputEnabled = config.analog1OutputEnable.enabled
                this.isAnalog2OutputEnabled = config.analog2OutputEnable.enabled
                this.isAnalog3OutputEnabled = config.analog3OutputEnable.enabled
                this.isAnalog4OutputEnabled = config.analog4OutputEnable.enabled
                this.isRelay3OutputEnabled = config.relay3OutputEnable.enabled
                this.isRelay7OutputEnabled = config.relay7OutputEnable.enabled
                this.isAdaptiveDeltaEnabled = config.adaptiveDeltaEnable.enabled
                this.ismaximizedExitWaterTempEnable = config.maximizedExitWaterTempEnable.enabled
                this.isDcwbEnabled = config.dcwbEnable.enabled
                this.chilledWaterTargetDelta = config.chilledWaterTargetDelta.currentVal
                this.chilledWaterExitTemperatureMargin = config.chilledWaterExitTemperatureMargin.currentVal
                this.chilledWaterExitTemperatureTarget = config.chilledWaterExitTemperatureTarget.currentVal
                this.chilledWaterMaxFlowRate = config.chilledWaterMaxFlowRate.currentVal
                this.chilledWaterMaxFlowRateInc = config.chilledWaterMaxFlowRate.incVal

                this.analog1OutputAssociation = config.analog1OutputAssociation.associationVal
                this.analog2OutputAssociation = config.analog2OutputAssociation.associationVal
                this.analog3OutputAssociation = config.analog3OutputAssociation.associationVal
                this.analog4OutputAssociation = config.analog4OutputAssociation.associationVal
                this.relay3Association = config.relay3Association.associationVal

                this.relay7Association = config.relay7Association.associationVal
                //this.analog4Association = config.analog4Association.associationVal

                /*this.analogOut1CoolingMin = config.analogOut1CoolingMin.currentVal.toInt()
                this.analogOut1CoolingMax = config.analogOut1CoolingMax.currentVal.toInt()
                this.analogOut2StaticPressureMin = config.analogOut2StaticPressureMin.currentVal.toInt()
                this.analogOut2StaticPressureMax = config.analogOut2StaticPressureMax.currentVal.toInt()
                this.analogOut4FreshAirMin = config.analogOut4FreshAirMin.currentVal.toInt()
                this.analogOut4FreshAirMax = config.analogOut4FreshAirMax.currentVal.toInt()
                this.analog1ValveClosedPosition = config.analog1ValveClosedPosition.currentVal.toInt()
                this.analog1ValveFullPosition = config.analog1ValveFullPosition.currentVal.toInt()
                this.analog2MinFan = config.analog2MinFan.currentVal.toInt()
                this.analog2MaxFan = config.analog2MaxFan.currentVal.toInt()
                this.analog3MinHeating = config.analog3MinHeating.currentVal.toInt()
                this.analog3MaxHeating = config.analog3MaxHeating.currentVal.toInt()
                this.analogOut4MinCoolingLoop = config.analogOut4MinCoolingLoop.currentVal.toInt()
                this.analogOut4MaxCoolingLoop = config.analogOut4MaxCoolingLoop.currentVal.toInt()*/

                this.thermistor1Enabled = config.thermistor1Enabled.enabled
                this.thermistor2Enabled = config.thermistor2Enabled.enabled
                this.thermistor1Association = config.thermistor1InAssociation.associationVal
                this.thermistor2Association = config.thermistor2InAssociation.associationVal
                this.analogIn1Enabled = config.analogIn1Enabled.enabled
                this.analogIn2Enabled = config.analogIn2Enabled.enabled
                this.analogIn1Association = config.analogIn1Association.associationVal
                this.analogIn2Association = config.analogIn2Association.associationVal

                this.analog1ValveClosedPosition = config.analog1ValveClosedPosition.currentVal.toInt()
                this.analog1ValveFullPosition = config.analog1ValveFullPosition.currentVal.toInt()

                this.analog1OutMinMaxConfig = config.analog1OutMinMaxConfig
                this.analog2OutMinMaxConfig = config.analog2OutMinMaxConfig
                this.analog3OutMinMaxConfig = config.analog3OutMinMaxConfig
                this.analog4OutMinMaxConfig = config.analog4OutMinMaxConfig

                this.unusedPortState = config.unusedPorts
            }
        }
    }


    fun updateConfigFromViewState(config : DabModulatingRtuProfileConfig ) {
        config.analog1OutputEnable.enabled = this.isAnalog1OutputEnabled
        config.analog2OutputEnable.enabled = this.isAnalog2OutputEnabled
        config.analog3OutputEnable.enabled = this.isAnalog3OutputEnabled
        config.analog4OutputEnable.enabled = this.isAnalog4OutputEnabled
        config.relay3OutputEnable.enabled = this.isRelay3OutputEnabled
        config.relay7OutputEnable.enabled = this.isRelay7OutputEnabled
        config.adaptiveDeltaEnable.enabled = this.isAdaptiveDeltaEnabled
        config.maximizedExitWaterTempEnable.enabled = this.ismaximizedExitWaterTempEnable
        config.dcwbEnable.enabled = this.isDcwbEnabled
        config.chilledWaterTargetDelta.currentVal = this.chilledWaterTargetDelta
        config.chilledWaterExitTemperatureMargin.currentVal = this.chilledWaterExitTemperatureMargin
        config.chilledWaterExitTemperatureTarget.currentVal = this.chilledWaterExitTemperatureTarget
        config.chilledWaterMaxFlowRate.currentVal = this.chilledWaterMaxFlowRate

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

        //config.analog4Association.associationVal = this.analog4Association

        /*config.analogOut1CoolingMin.currentVal = this.analogOut1CoolingMin.toDouble()
        config.analogOut1CoolingMax.currentVal = this.analogOut1CoolingMax.toDouble()
        config.analogOut2StaticPressureMin.currentVal = this.analogOut2StaticPressureMin.toDouble()
        config.analogOut2StaticPressureMax.currentVal = this.analogOut2StaticPressureMax.toDouble()
        config.analogOut4FreshAirMin.currentVal = this.analogOut4FreshAirMin.toDouble()
        config.analogOut4FreshAirMax.currentVal = this.analogOut4FreshAirMax.toDouble()
        config.analog1ValveClosedPosition.currentVal = this.analog1ValveClosedPosition.toDouble()
        config.analog1ValveFullPosition.currentVal = this.analog1ValveFullPosition.toDouble()
        config.analog2MinFan.currentVal = this.analog2MinFan.toDouble()
        config.analog2MaxFan.currentVal = this.analog2MaxFan.toDouble()
        config.analog3MinHeating.currentVal = this.analog3MinHeating.toDouble()
        config.analog3MaxHeating.currentVal = this.analog3MaxHeating.toDouble()
        config.analogOut4MinCoolingLoop.currentVal = this.analogOut4MinCoolingLoop.toDouble()
        config.analogOut4MaxCoolingLoop.currentVal = this.analogOut4MaxCoolingLoop.toDouble()*/

        config.analog1ValveClosedPosition.currentVal = this.analog1ValveClosedPosition.toDouble()
        config.analog1ValveFullPosition.currentVal = this.analog1ValveFullPosition.toDouble()
        config.analog1OutMinMaxConfig = this.analog1OutMinMaxConfig
        config.analog2OutMinMaxConfig = this.analog2OutMinMaxConfig
        config.analog3OutMinMaxConfig = this.analog3OutMinMaxConfig
        config.analog4OutMinMaxConfig = this.analog4OutMinMaxConfig

        config.unusedPorts = this.unusedPortState
    }
}