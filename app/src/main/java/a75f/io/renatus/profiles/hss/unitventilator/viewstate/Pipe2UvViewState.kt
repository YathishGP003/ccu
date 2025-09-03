package a75f.io.renatus.profiles.hss.unitventilator.viewstate

import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UVConfiguration
import a75f.io.renatus.profiles.hss.HyperStatSplitState
import a75f.io.renatus.profiles.hss.configMisc
import a75f.io.renatus.profiles.hss.configRelay
import a75f.io.renatus.profiles.hss.configSensorAddress
import a75f.io.renatus.profiles.hss.configUniversalIn
import a75f.io.renatus.profiles.hss.updateHyperStatSplitConfigFromState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Pipe2UvViewState : HyperStatSplitState(){
    var analogOut1MinMax by mutableStateOf(
        AnalogOutVoltage(2,10,70,80,100,2,10,2,10,2,10,2,10,

        )
    )

    var analogOut2MinMax by mutableStateOf(
        AnalogOutVoltage(2,10,70,80,100,2,10,2,10,2,10,2,10,

        )
    )

    var analogOut3MinMax by mutableStateOf(
        AnalogOutVoltage(2,10,70,80,100,2,10,2,10,2,10,2,10,

        )
    )

    var analogOut4MinMax by mutableStateOf(
        AnalogOutVoltage(2,10,70,80,100,2,10,2,10,2,10,2,10,

        )
    )

    var controlVia by mutableIntStateOf(0)
    var saTempering by mutableStateOf(false)
    var fanRecirculate  by mutableIntStateOf(0)

    companion object
    {
        fun fromProfileConfigState(config: Pipe2UVConfiguration):Pipe2UvViewState{
            return Pipe2UvViewState().apply {
                configSensorAddress(config, this)
                configUniversalIn(config, this)
                configAnalogOut(config, this)
                configRelay(config, this)
                configMisc(config, this)
                configGenericConfig(config, this)
            }
        }
    }

    fun updateViewState(config: Pipe2UVConfiguration){
        return updateHyperStatSplitConfigFromState(config, this@Pipe2UvViewState).apply {
            updateDynamicPoints(config, this@Pipe2UvViewState)
            updateGenericConfigState(config, this@Pipe2UvViewState)
        }
    }

    private fun updateGenericConfigState(config: Pipe2UVConfiguration, state: Pipe2UvViewState) {
        config.controlVia.currentVal = state.controlVia.toDouble()
        config.saTempering.enabled = state.saTempering
    }

    fun configGenericConfig(config: Pipe2UVConfiguration, state: Pipe2UvViewState) {
        state.controlVia = config.controlVia.currentVal.toInt()
        state.saTempering = config.saTempering.enabled
    }

    private fun updateDynamicPoints(config: Pipe2UVConfiguration, state: Pipe2UvViewState) {

        config.analogOut1Voltage.oaoDamperMinVoltage.currentVal = state.analogOut1MinMax.oaoDamperMinVoltage.toDouble()
        config.analogOut1Voltage.oaoDamperMaxVoltage.currentVal = state.analogOut1MinMax.oaoDamperMaxVoltage.toDouble()
        config.analogOut1Voltage.dcvModulationMinVoltage.currentVal = state.analogOut1MinMax.dcvModulationMinVoltage.toDouble()
        config.analogOut1Voltage.dcvModulationMaxVoltage.currentVal = state.analogOut1MinMax.dcvModulationMaxVoltage.toDouble()
        config.analogOut1Voltage.fanMax.currentVal = state.analogOut1MinMax.fanMax.toDouble()
        config.analogOut1Voltage.fanMin.currentVal = state.analogOut1MinMax.fanMin.toDouble()
        config.analogOut1Voltage.faceAndBypassDamperMin.currentVal = state.analogOut1MinMax.faceAndBypassDamperMin.toDouble()
        config.analogOut1Voltage.faceAndBypassDamperMax.currentVal = state.analogOut1MinMax.faceAndBypassDamperMax.toDouble()
        config.analogOut1Voltage.fanAtLow.currentVal = state.analogOut1MinMax.fanAtLow.toDouble()
        config.analogOut1Voltage.fanAtMedium.currentVal = state.analogOut1MinMax.fanAtMedium.toDouble()
        config.analogOut1Voltage.fanAtHigh.currentVal = state.analogOut1MinMax.fanAtHigh.toDouble()
        config.analogOut1Voltage.waterValveMin.currentVal = state.analogOut1MinMax.waterValveMinVoltage.toDouble()
        config.analogOut1Voltage.waterValveMax.currentVal = state.analogOut1MinMax.waterValveMaxVoltage.toDouble()


        config.analogOut2Voltage.oaoDamperMinVoltage.currentVal = state.analogOut2MinMax.oaoDamperMinVoltage.toDouble()
        config.analogOut2Voltage.oaoDamperMaxVoltage.currentVal = state.analogOut2MinMax.oaoDamperMaxVoltage.toDouble()
        config.analogOut2Voltage.dcvModulationMinVoltage.currentVal = state.analogOut2MinMax.dcvModulationMinVoltage.toDouble()
        config.analogOut2Voltage.dcvModulationMaxVoltage.currentVal = state.analogOut2MinMax.dcvModulationMaxVoltage.toDouble()
        config.analogOut2Voltage.fanMax.currentVal = state.analogOut2MinMax.fanMax.toDouble()
        config.analogOut2Voltage.fanMin.currentVal = state.analogOut2MinMax.fanMin.toDouble()
        config.analogOut2Voltage.faceAndBypassDamperMin.currentVal = state.analogOut2MinMax.faceAndBypassDamperMin.toDouble()
        config.analogOut2Voltage.faceAndBypassDamperMax.currentVal = state.analogOut2MinMax.faceAndBypassDamperMax.toDouble()
        config.analogOut2Voltage.fanAtLow.currentVal = state.analogOut2MinMax.fanAtLow.toDouble()
        config.analogOut2Voltage.fanAtMedium.currentVal = state.analogOut2MinMax.fanAtMedium.toDouble()
        config.analogOut2Voltage.fanAtHigh.currentVal = state.analogOut2MinMax.fanAtHigh.toDouble()
        config.analogOut2Voltage.waterValveMin.currentVal = state.analogOut2MinMax.waterValveMinVoltage.toDouble()
        config.analogOut2Voltage.waterValveMax.currentVal = state.analogOut2MinMax.waterValveMaxVoltage.toDouble()



        config.analogOut3Voltage.oaoDamperMinVoltage.currentVal = state.analogOut3MinMax.oaoDamperMinVoltage.toDouble()
        config.analogOut3Voltage.oaoDamperMaxVoltage.currentVal = state.analogOut3MinMax.oaoDamperMaxVoltage.toDouble()
        config.analogOut3Voltage.dcvModulationMinVoltage.currentVal = state.analogOut3MinMax.dcvModulationMinVoltage.toDouble()
        config.analogOut3Voltage.dcvModulationMaxVoltage.currentVal = state.analogOut3MinMax.dcvModulationMaxVoltage.toDouble()
        config.analogOut3Voltage.fanMax.currentVal = state.analogOut3MinMax.fanMax.toDouble()
        config.analogOut3Voltage.fanMin.currentVal = state.analogOut3MinMax.fanMin.toDouble()
        config.analogOut3Voltage.faceAndBypassDamperMin.currentVal = state.analogOut3MinMax.faceAndBypassDamperMin.toDouble()
        config.analogOut3Voltage.faceAndBypassDamperMax.currentVal = state.analogOut3MinMax.faceAndBypassDamperMax.toDouble()
        config.analogOut3Voltage.fanAtLow.currentVal = state.analogOut3MinMax.fanAtLow.toDouble()
        config.analogOut3Voltage.fanAtMedium.currentVal = state.analogOut3MinMax.fanAtMedium.toDouble()
        config.analogOut3Voltage.fanAtHigh.currentVal = state.analogOut3MinMax.fanAtHigh.toDouble()
        config.analogOut3Voltage.waterValveMin.currentVal = state.analogOut3MinMax.waterValveMinVoltage.toDouble()
        config.analogOut3Voltage.waterValveMax.currentVal = state.analogOut3MinMax.waterValveMaxVoltage.toDouble()



        config.analogOut4Voltage.oaoDamperMinVoltage.currentVal = state.analogOut4MinMax.oaoDamperMinVoltage.toDouble()
        config.analogOut4Voltage.oaoDamperMaxVoltage.currentVal = state.analogOut4MinMax.oaoDamperMaxVoltage.toDouble()
        config.analogOut4Voltage.dcvModulationMinVoltage.currentVal = state.analogOut4MinMax.dcvModulationMinVoltage.toDouble()
        config.analogOut4Voltage.dcvModulationMaxVoltage.currentVal = state.analogOut4MinMax.dcvModulationMaxVoltage.toDouble()
        config.analogOut4Voltage.fanMax.currentVal = state.analogOut4MinMax.fanMax.toDouble()
        config.analogOut4Voltage.fanMin.currentVal = state.analogOut4MinMax.fanMin.toDouble()
        config.analogOut4Voltage.faceAndBypassDamperMin.currentVal = state.analogOut4MinMax.faceAndBypassDamperMin.toDouble()
        config.analogOut4Voltage.faceAndBypassDamperMax.currentVal = state.analogOut4MinMax.faceAndBypassDamperMax.toDouble()
        config.analogOut4Voltage.fanAtLow.currentVal = state.analogOut4MinMax.fanAtLow.toDouble()
        config.analogOut4Voltage.fanAtMedium.currentVal = state.analogOut4MinMax.fanAtMedium.toDouble()
        config.analogOut4Voltage.fanAtHigh.currentVal = state.analogOut4MinMax.fanAtHigh.toDouble()
        config.analogOut4Voltage.waterValveMin.currentVal = state.analogOut4MinMax.waterValveMinVoltage.toDouble()
        config.analogOut4Voltage.waterValveMax.currentVal = state.analogOut4MinMax.waterValveMaxVoltage.toDouble()


    }

    fun configAnalogOut(config: Pipe2UVConfiguration, state: Pipe2UvViewState) {
        state.analogOut1Enabled = config.analogOut1Enabled.enabled
        state.analogOut2Enabled = config.analogOut2Enabled.enabled
        state.analogOut3Enabled = config.analogOut3Enabled.enabled
        state.analogOut4Enabled = config.analogOut4Enabled.enabled

        state.analogOut1Association = config.analogOut1Association.associationVal
        state.analogOut2Association = config.analogOut2Association.associationVal
        state.analogOut3Association = config.analogOut3Association.associationVal
        state.analogOut4Association = config.analogOut4Association.associationVal

        updateAnalogOutDynamicConfig(config,state)
    }


    private fun updateAnalogOutDynamicConfig(config: Pipe2UVConfiguration, state: Pipe2UvViewState){
        state.analogOut1MinMax = AnalogOutVoltage(
            config.analogOut1Voltage.fanMin.currentVal.toInt(),
            config.analogOut1Voltage.fanMax.currentVal.toInt(),
            config.analogOut1Voltage.fanAtLow.currentVal.toInt(),
            config.analogOut1Voltage.fanAtMedium.currentVal.toInt(),
            config.analogOut1Voltage.fanAtHigh.currentVal.toInt(),
            config.analogOut1Voltage.oaoDamperMinVoltage.currentVal.toInt(),
            config.analogOut1Voltage.oaoDamperMaxVoltage.currentVal.toInt(),
            config.analogOut1Voltage.waterValveMin.currentVal.toInt(),
            config.analogOut1Voltage.waterValveMax.currentVal.toInt(),
            config.analogOut1Voltage.dcvModulationMinVoltage.currentVal.toInt(),
            config.analogOut1Voltage.dcvModulationMaxVoltage.currentVal.toInt(),
            config.analogOut1Voltage.faceAndBypassDamperMin.currentVal.toInt(),
            config.analogOut1Voltage.faceAndBypassDamperMax.currentVal.toInt()
        )
        state.analogOut2MinMax = AnalogOutVoltage(
            config.analogOut2Voltage.fanMin.currentVal.toInt(),
            config.analogOut2Voltage.fanMax.currentVal.toInt(),
            config.analogOut2Voltage.fanAtLow.currentVal.toInt(),
            config.analogOut2Voltage.fanAtMedium.currentVal.toInt(),
            config.analogOut2Voltage.fanAtHigh.currentVal.toInt(),
            config.analogOut2Voltage.oaoDamperMinVoltage.currentVal.toInt(),
            config.analogOut2Voltage.oaoDamperMaxVoltage.currentVal.toInt(),
            config.analogOut2Voltage.waterValveMin.currentVal.toInt(),
            config.analogOut2Voltage.waterValveMax.currentVal.toInt(),
            config.analogOut2Voltage.dcvModulationMinVoltage.currentVal.toInt(),
            config.analogOut2Voltage.dcvModulationMaxVoltage.currentVal.toInt(),
            config.analogOut2Voltage.faceAndBypassDamperMin.currentVal.toInt(),
            config.analogOut2Voltage.faceAndBypassDamperMax.currentVal.toInt()
        )

        state.analogOut3MinMax = AnalogOutVoltage(
            config.analogOut3Voltage.fanMin.currentVal.toInt(),
            config.analogOut3Voltage.fanMax.currentVal.toInt(),
            config.analogOut3Voltage.fanAtLow.currentVal.toInt(),
            config.analogOut3Voltage.fanAtMedium.currentVal.toInt(),
            config.analogOut3Voltage.fanAtHigh.currentVal.toInt(),
            config.analogOut3Voltage.oaoDamperMinVoltage.currentVal.toInt(),
            config.analogOut3Voltage.oaoDamperMaxVoltage.currentVal.toInt(),
            config.analogOut3Voltage.waterValveMin.currentVal.toInt(),
            config.analogOut3Voltage.waterValveMax.currentVal.toInt(),
            config.analogOut3Voltage.dcvModulationMinVoltage.currentVal.toInt(),
            config.analogOut3Voltage.dcvModulationMaxVoltage.currentVal.toInt(),
            config.analogOut3Voltage.faceAndBypassDamperMin.currentVal.toInt(),
            config.analogOut3Voltage.faceAndBypassDamperMax.currentVal.toInt()
        )

        state.analogOut4MinMax = AnalogOutVoltage(
            config.analogOut4Voltage.fanMin.currentVal.toInt(),
            config.analogOut4Voltage.fanMax.currentVal.toInt(),
            config.analogOut4Voltage.fanAtLow.currentVal.toInt(),
            config.analogOut4Voltage.fanAtMedium.currentVal.toInt(),
            config.analogOut4Voltage.fanAtHigh.currentVal.toInt(),
            config.analogOut4Voltage.oaoDamperMinVoltage.currentVal.toInt(),
            config.analogOut4Voltage.oaoDamperMaxVoltage.currentVal.toInt(),
            config.analogOut4Voltage.waterValveMin.currentVal.toInt(),
            config.analogOut4Voltage.waterValveMax.currentVal.toInt(),
            config.analogOut4Voltage.dcvModulationMinVoltage.currentVal.toInt(),
            config.analogOut4Voltage.dcvModulationMaxVoltage.currentVal.toInt(),
            config.analogOut4Voltage.faceAndBypassDamperMin.currentVal.toInt(),
            config.analogOut4Voltage.faceAndBypassDamperMax.currentVal.toInt()
        )

    }


    fun equalsViewStatePipe2(
        otherViewState: HyperStatSplitState
    ): Boolean {
        return super.equalsViewState(this, otherViewState) &&
                compareAnalogOutMinMax(this.analogOut1MinMax ,(otherViewState as Pipe2UvViewState).analogOut1MinMax) &&
                compareAnalogOutMinMax(this.analogOut2MinMax, otherViewState.analogOut2MinMax) &&
                compareAnalogOutMinMax(this.analogOut3MinMax, otherViewState.analogOut3MinMax) &&
                compareAnalogOutMinMax(this.analogOut4MinMax, otherViewState.analogOut4MinMax) &&
                this.controlVia == otherViewState.controlVia && this.saTempering == otherViewState.saTempering &&
                this.fanRecirculate == otherViewState.fanRecirculate
    }

    private fun compareAnalogOutMinMax(state: AnalogOutVoltage, otherState: AnalogOutVoltage): Boolean {
         return  state.fanMax == otherState.fanMax &&
                state.fanMin == otherState.fanMin &&
                state.fanAtLow == otherState.fanAtLow &&
                state.fanAtMedium == otherState.fanAtMedium &&
                state.fanAtHigh == otherState.fanAtHigh &&
                state.oaoDamperMinVoltage == otherState.oaoDamperMinVoltage &&
                state.oaoDamperMaxVoltage == otherState.oaoDamperMaxVoltage &&
                state.waterValveMinVoltage == otherState.waterValveMinVoltage &&
                state.waterValveMaxVoltage == otherState.waterValveMaxVoltage &&
                state.dcvModulationMinVoltage == otherState.dcvModulationMinVoltage &&
                state.dcvModulationMaxVoltage == otherState.dcvModulationMaxVoltage &&
                state.faceAndBypassDamperMin == otherState.faceAndBypassDamperMin &&
                state.faceAndBypassDamperMax == otherState.faceAndBypassDamperMax
    }
}

data class AnalogOutVoltage(
    var fanMin: Int, var fanMax: Int,
    var fanAtLow:Int, var fanAtMedium :Int, var fanAtHigh :Int,
    var oaoDamperMinVoltage: Int, var oaoDamperMaxVoltage: Int,
    var waterValveMinVoltage: Int, var waterValveMaxVoltage: Int,
    var dcvModulationMinVoltage: Int, var dcvModulationMaxVoltage: Int,
    var faceAndBypassDamperMin :Int, var faceAndBypassDamperMax : Int
)