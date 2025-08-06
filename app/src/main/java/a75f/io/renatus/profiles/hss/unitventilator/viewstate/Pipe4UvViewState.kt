package a75f.io.renatus.profiles.hss.unitventilator.viewstate

import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVConfiguration
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

class Pipe4UvViewState : HyperStatSplitState() {

    var analogOut1MinMax by mutableStateOf(
        AnalogOutVoltage(2,10,70,80,100,2,10,2,10,2,10,2,10,2,10

        )
    )

    var analogOut2MinMax by mutableStateOf(
        AnalogOutVoltage(2,10,70,80,100,2,10,2,10,2,10,2,10,2,10

        )
    )

    var analogOut3MinMax by mutableStateOf(
        AnalogOutVoltage(2,10,70,80,100,2,10,2,10,2,10,2,10,2,10

        )
    )

    var analogOut4MinMax by mutableStateOf(
        AnalogOutVoltage(2,10,70,80,100,2,10,2,10,2,10,2,10,2,10

        )
    )


    var controlVia by mutableIntStateOf(0)
    var saTempering by mutableStateOf(false)
    var fanRecirculate by mutableIntStateOf(0)
    var fanEconomizer by mutableIntStateOf(0)

    companion object{
        fun fromProfileConfigToState(config: Pipe4UVConfiguration): Pipe4UvViewState {
            return Pipe4UvViewState().apply {
                configSensorAddress(config, this)
                configUniversalIn(config, this)
                configAnalogOut(config, this)
                configRelay(config, this)
                configMisc(config, this)
                configGenericConfig(config, this)
            }

        }
    }


    private fun compareAnalogOutMinMax(
        state: AnalogOutVoltage,
        otherViewState: AnalogOutVoltage
    ): Boolean {
        return ( state.fanMin == otherViewState.fanMin &&
                state.fanMax == otherViewState.fanMax &&
                state.oaoDamperMinVoltage == otherViewState.oaoDamperMinVoltage &&
                state.oaoDamperMaxVoltage == otherViewState.oaoDamperMaxVoltage &&
                state.hotWaterValveMinVoltage == otherViewState.hotWaterValveMinVoltage &&
                state.hotWaterValveMaxVoltage == otherViewState.hotWaterValveMaxVoltage &&
                state.coolingWaterValveMinVoltage == otherViewState.coolingWaterValveMinVoltage &&
                state.coolingWaterValveMaxVoltage == otherViewState.coolingWaterValveMaxVoltage &&
                state.dcvModulationMinVoltage == otherViewState.dcvModulationMinVoltage &&
                state.dcvModulationMaxVoltage == otherViewState.dcvModulationMaxVoltage &&
                state.faceAndBypassDamperMin == otherViewState.faceAndBypassDamperMin &&
                state.faceAndBypassDamperMax == otherViewState.faceAndBypassDamperMax &&
                state.fanAtLow == otherViewState.fanAtLow &&
                state.fanAtMedium == otherViewState.fanAtMedium &&
                state.fanAtHigh == otherViewState.fanAtHigh)
    }

     fun equalsViewStatePipe4(
        otherViewState: HyperStatSplitState
    ): Boolean {
        return super.equalsViewState(this, otherViewState) &&
                compareAnalogOutMinMax(this.analogOut1MinMax ,(otherViewState as Pipe4UvViewState).analogOut1MinMax) &&
                compareAnalogOutMinMax(this.analogOut2MinMax, otherViewState.analogOut2MinMax) &&
                compareAnalogOutMinMax(this.analogOut3MinMax, otherViewState.analogOut3MinMax) &&
                compareAnalogOutMinMax(this.analogOut4MinMax, otherViewState.analogOut4MinMax) &&
                this.controlVia == otherViewState.controlVia && this.saTempering == otherViewState.saTempering &&
                this.fanRecirculate == otherViewState.fanRecirculate && this.fanEconomizer == otherViewState.fanEconomizer
    }

    fun updateConfigFromViewState (config: Pipe4UVConfiguration) {
        return updateHyperStatSplitConfigFromState(config, this@Pipe4UvViewState).apply {
            updateDynamicPoints(config,this@Pipe4UvViewState)
            updateGenericConfigState(config,this@Pipe4UvViewState)

        }
    }

    private fun updateGenericConfigState(config: Pipe4UVConfiguration, uvProfileStat: Pipe4UvViewState) {
        config.controlVia.currentVal = uvProfileStat.controlVia.toDouble()
        config.saTempering.enabled = uvProfileStat.saTempering

    }

    fun configGenericConfig(config: Pipe4UVConfiguration, state: Pipe4UvViewState) {
        state.controlVia = config.controlVia.currentVal.toInt()
        state.saTempering = config.saTempering.enabled
    }


    private fun updateDynamicPoints(config: Pipe4UVConfiguration, state: Pipe4UvViewState) {

        config.analogOut1Voltage.oaoDamperMinVoltage.currentVal = state.analogOut1MinMax.oaoDamperMinVoltage.toDouble()
        config.analogOut1Voltage.oaoDamperMaxVoltage.currentVal = state.analogOut1MinMax.oaoDamperMaxVoltage.toDouble()
        config.analogOut1Voltage.dcvModulationMinVoltage.currentVal = state.analogOut1MinMax.dcvModulationMinVoltage.toDouble()
        config.analogOut1Voltage.dcvModulationMaxVoltage.currentVal = state.analogOut1MinMax.dcvModulationMaxVoltage.toDouble()
        config.analogOut1Voltage.fanMax.currentVal = state.analogOut1MinMax.fanMax.toDouble()
        config.analogOut1Voltage.fanMin.currentVal = state.analogOut1MinMax.fanMin.toDouble()
        config.analogOut1Voltage.faceAndBypassDamperMin.currentVal = state.analogOut1MinMax.faceAndBypassDamperMin.toDouble()
        config.analogOut1Voltage.faceAndBypassDamperMax.currentVal = state.analogOut1MinMax.faceAndBypassDamperMax.toDouble()
        config.analogOut1Voltage.hotWaterValveMinVoltage.currentVal = state.analogOut1MinMax.hotWaterValveMinVoltage.toDouble()
        config.analogOut1Voltage.hotWaterValveMaxVoltage.currentVal = state.analogOut1MinMax.hotWaterValveMaxVoltage.toDouble()
        config.analogOut1Voltage.coolingWaterValveMinVoltage.currentVal = state.analogOut1MinMax.coolingWaterValveMinVoltage.toDouble()
        config.analogOut1Voltage.coolingWaterValveMaxVoltage.currentVal = state.analogOut1MinMax.coolingWaterValveMaxVoltage.toDouble()
        config.analogOut1Voltage.fanAtLow.currentVal = state.analogOut1MinMax.fanAtLow.toDouble()
        config.analogOut1Voltage.fanAtMedium.currentVal = state.analogOut1MinMax.fanAtMedium.toDouble()
        config.analogOut1Voltage.fanAtHigh.currentVal = state.analogOut1MinMax.fanAtHigh.toDouble()


        config.analogOut2Voltage.oaoDamperMinVoltage.currentVal = state.analogOut2MinMax.oaoDamperMinVoltage.toDouble()
        config.analogOut2Voltage.oaoDamperMaxVoltage.currentVal = state.analogOut2MinMax.oaoDamperMaxVoltage.toDouble()
        config.analogOut2Voltage.dcvModulationMinVoltage.currentVal = state.analogOut2MinMax.dcvModulationMinVoltage.toDouble()
        config.analogOut2Voltage.dcvModulationMaxVoltage.currentVal = state.analogOut2MinMax.dcvModulationMaxVoltage.toDouble()
        config.analogOut2Voltage.fanMax.currentVal = state.analogOut2MinMax.fanMax.toDouble()
        config.analogOut2Voltage.fanMin.currentVal = state.analogOut2MinMax.fanMin.toDouble()
        config.analogOut2Voltage.faceAndBypassDamperMin.currentVal = state.analogOut2MinMax.faceAndBypassDamperMin.toDouble()
        config.analogOut2Voltage.faceAndBypassDamperMax.currentVal = state.analogOut2MinMax.faceAndBypassDamperMax.toDouble()
        config.analogOut2Voltage.hotWaterValveMinVoltage.currentVal = state.analogOut2MinMax.hotWaterValveMinVoltage.toDouble()
        config.analogOut2Voltage.hotWaterValveMaxVoltage.currentVal = state.analogOut2MinMax.hotWaterValveMaxVoltage.toDouble()
        config.analogOut2Voltage.coolingWaterValveMinVoltage.currentVal = state.analogOut2MinMax.coolingWaterValveMinVoltage.toDouble()
        config.analogOut2Voltage.coolingWaterValveMaxVoltage.currentVal = state.analogOut2MinMax.coolingWaterValveMaxVoltage.toDouble()
        config.analogOut2Voltage.fanAtLow.currentVal = state.analogOut2MinMax.fanAtLow.toDouble()
        config.analogOut2Voltage.fanAtMedium.currentVal = state.analogOut2MinMax.fanAtMedium.toDouble()
        config.analogOut2Voltage.fanAtHigh.currentVal = state.analogOut2MinMax.fanAtHigh.toDouble()



        config.analogOut3Voltage.oaoDamperMinVoltage.currentVal = state.analogOut3MinMax.oaoDamperMinVoltage.toDouble()
        config.analogOut3Voltage.oaoDamperMaxVoltage.currentVal = state.analogOut3MinMax.oaoDamperMaxVoltage.toDouble()
        config.analogOut3Voltage.dcvModulationMinVoltage.currentVal = state.analogOut3MinMax.dcvModulationMinVoltage.toDouble()
        config.analogOut3Voltage.dcvModulationMaxVoltage.currentVal = state.analogOut3MinMax.dcvModulationMaxVoltage.toDouble()
        config.analogOut3Voltage.fanMax.currentVal = state.analogOut3MinMax.fanMax.toDouble()
        config.analogOut3Voltage.fanMin.currentVal = state.analogOut3MinMax.fanMin.toDouble()
        config.analogOut3Voltage.faceAndBypassDamperMin.currentVal = state.analogOut3MinMax.faceAndBypassDamperMin.toDouble()
        config.analogOut3Voltage.faceAndBypassDamperMax.currentVal = state.analogOut3MinMax.faceAndBypassDamperMax.toDouble()
        config.analogOut3Voltage.hotWaterValveMaxVoltage.currentVal = state.analogOut3MinMax.hotWaterValveMaxVoltage.toDouble()
        config.analogOut3Voltage.hotWaterValveMinVoltage.currentVal = state.analogOut3MinMax.hotWaterValveMinVoltage.toDouble()
        config.analogOut3Voltage.coolingWaterValveMinVoltage.currentVal = state.analogOut3MinMax.coolingWaterValveMinVoltage.toDouble()
        config.analogOut3Voltage.coolingWaterValveMaxVoltage.currentVal = state.analogOut3MinMax.coolingWaterValveMaxVoltage.toDouble()
        config.analogOut3Voltage.fanAtLow.currentVal = state.analogOut3MinMax.fanAtLow.toDouble()
        config.analogOut3Voltage.fanAtMedium.currentVal = state.analogOut3MinMax.fanAtMedium.toDouble()
        config.analogOut3Voltage.fanAtHigh.currentVal = state.analogOut3MinMax.fanAtHigh.toDouble()



        config.analogOut4Voltage.oaoDamperMinVoltage.currentVal = state.analogOut4MinMax.oaoDamperMinVoltage.toDouble()
        config.analogOut4Voltage.oaoDamperMaxVoltage.currentVal = state.analogOut4MinMax.oaoDamperMaxVoltage.toDouble()
        config.analogOut4Voltage.dcvModulationMinVoltage.currentVal = state.analogOut4MinMax.dcvModulationMinVoltage.toDouble()
        config.analogOut4Voltage.dcvModulationMaxVoltage.currentVal = state.analogOut4MinMax.dcvModulationMaxVoltage.toDouble()
        config.analogOut4Voltage.fanMax.currentVal = state.analogOut4MinMax.fanMax.toDouble()
        config.analogOut4Voltage.fanMin.currentVal = state.analogOut4MinMax.fanMin.toDouble()
        config.analogOut4Voltage.faceAndBypassDamperMin.currentVal = state.analogOut4MinMax.faceAndBypassDamperMin.toDouble()
        config.analogOut4Voltage.faceAndBypassDamperMax.currentVal = state.analogOut4MinMax.faceAndBypassDamperMax.toDouble()
        config.analogOut4Voltage.hotWaterValveMinVoltage.currentVal = state.analogOut4MinMax.hotWaterValveMinVoltage.toDouble()
        config.analogOut4Voltage.hotWaterValveMaxVoltage.currentVal = state.analogOut4MinMax.hotWaterValveMaxVoltage.toDouble()
        config.analogOut4Voltage.coolingWaterValveMinVoltage.currentVal = state.analogOut4MinMax.coolingWaterValveMinVoltage.toDouble()
        config.analogOut4Voltage.coolingWaterValveMaxVoltage.currentVal = state.analogOut4MinMax.coolingWaterValveMaxVoltage.toDouble()
        config.analogOut4Voltage.fanAtLow.currentVal = state.analogOut4MinMax.fanAtLow.toDouble()
        config.analogOut4Voltage.fanAtMedium.currentVal = state.analogOut4MinMax.fanAtMedium.toDouble()
        config.analogOut4Voltage.fanAtHigh.currentVal = state.analogOut4MinMax.fanAtHigh.toDouble()

        config.fanRecirculate.currentVal = state.fanRecirculate.toDouble()
        config.fanEconomizer.currentVal = state.fanEconomizer.toDouble()

    }
    fun configAnalogOut(config: Pipe4UVConfiguration, state: Pipe4UvViewState) {
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

    private fun updateAnalogOutDynamicConfig(config: Pipe4UVConfiguration, state: Pipe4UvViewState) {

        state.analogOut1MinMax = AnalogOutVoltage(
            fanMin = config.analogOut1Voltage.fanMin.currentVal.toInt(),
            fanMax = config.analogOut1Voltage.fanMax.currentVal.toInt(),
            oaoDamperMinVoltage = config.analogOut1Voltage.oaoDamperMinVoltage.currentVal.toInt(),
            oaoDamperMaxVoltage = config.analogOut1Voltage.oaoDamperMaxVoltage.currentVal.toInt(),
            hotWaterValveMinVoltage = config.analogOut1Voltage.hotWaterValveMinVoltage.currentVal.toInt(),
            hotWaterValveMaxVoltage = config.analogOut1Voltage.hotWaterValveMaxVoltage.currentVal.toInt(),
            coolingWaterValveMinVoltage = config.analogOut1Voltage.coolingWaterValveMinVoltage.currentVal.toInt(),
            coolingWaterValveMaxVoltage = config.analogOut1Voltage.coolingWaterValveMaxVoltage.currentVal.toInt(),
            dcvModulationMinVoltage = config.analogOut1Voltage.dcvModulationMinVoltage.currentVal.toInt(),
            dcvModulationMaxVoltage = config.analogOut1Voltage.dcvModulationMaxVoltage.currentVal.toInt(),
            faceAndBypassDamperMin = config.analogOut1Voltage.faceAndBypassDamperMin.currentVal.toInt(),
            faceAndBypassDamperMax = config.analogOut1Voltage.faceAndBypassDamperMax.currentVal.toInt(),
            fanAtLow = config.analogOut1Voltage.fanAtLow.currentVal.toInt(),
            fanAtMedium = config.analogOut1Voltage.fanAtMedium.currentVal.toInt(),
            fanAtHigh = config.analogOut1Voltage.fanAtHigh.currentVal.toInt()
        )

        state.analogOut2MinMax = AnalogOutVoltage(
            fanMin = config.analogOut2Voltage.fanMin.currentVal.toInt(),
            fanMax = config.analogOut1Voltage.fanMax.currentVal.toInt(),
            oaoDamperMinVoltage = config.analogOut2Voltage.oaoDamperMinVoltage.currentVal.toInt(),
            oaoDamperMaxVoltage = config.analogOut2Voltage.oaoDamperMaxVoltage.currentVal.toInt(),
            hotWaterValveMinVoltage = config.analogOut2Voltage.hotWaterValveMinVoltage.currentVal.toInt(),
            hotWaterValveMaxVoltage = config.analogOut2Voltage.hotWaterValveMaxVoltage.currentVal.toInt(),
            coolingWaterValveMinVoltage = config.analogOut2Voltage.coolingWaterValveMinVoltage.currentVal.toInt(),
            coolingWaterValveMaxVoltage = config.analogOut2Voltage.coolingWaterValveMaxVoltage.currentVal.toInt(),
            dcvModulationMinVoltage = config.analogOut2Voltage.dcvModulationMinVoltage.currentVal.toInt(),
            dcvModulationMaxVoltage = config.analogOut2Voltage.dcvModulationMaxVoltage.currentVal.toInt(),
            faceAndBypassDamperMin = config.analogOut2Voltage.faceAndBypassDamperMin.currentVal.toInt(),
            faceAndBypassDamperMax = config.analogOut2Voltage.faceAndBypassDamperMax.currentVal.toInt(),
            fanAtLow = config.analogOut2Voltage.fanAtLow.currentVal.toInt(),
            fanAtMedium = config.analogOut2Voltage.fanAtMedium.currentVal.toInt(),
            fanAtHigh = config.analogOut2Voltage.fanAtHigh.currentVal.toInt()
        )



        state.analogOut3MinMax = AnalogOutVoltage(
            fanMin = config.analogOut3Voltage.fanMin.currentVal.toInt(),
            fanMax = config.analogOut3Voltage.fanMax.currentVal.toInt(),
            oaoDamperMinVoltage = config.analogOut3Voltage.oaoDamperMinVoltage.currentVal.toInt(),
            oaoDamperMaxVoltage = config.analogOut3Voltage.oaoDamperMaxVoltage.currentVal.toInt(),
            hotWaterValveMinVoltage = config.analogOut3Voltage.hotWaterValveMinVoltage.currentVal.toInt(),
            hotWaterValveMaxVoltage = config.analogOut3Voltage.hotWaterValveMaxVoltage.currentVal.toInt(),
            coolingWaterValveMinVoltage = config.analogOut3Voltage.coolingWaterValveMinVoltage.currentVal.toInt(),
            coolingWaterValveMaxVoltage = config.analogOut3Voltage.coolingWaterValveMaxVoltage.currentVal.toInt(),
            dcvModulationMinVoltage = config.analogOut3Voltage.dcvModulationMinVoltage.currentVal.toInt(),
            dcvModulationMaxVoltage = config.analogOut3Voltage.dcvModulationMaxVoltage.currentVal.toInt(),
            faceAndBypassDamperMin = config.analogOut3Voltage.faceAndBypassDamperMin.currentVal.toInt(),
            faceAndBypassDamperMax = config.analogOut3Voltage.faceAndBypassDamperMax.currentVal.toInt(),
            fanAtLow = config.analogOut3Voltage.fanAtLow.currentVal.toInt(),
            fanAtMedium = config.analogOut3Voltage.fanAtMedium.currentVal.toInt(),
            fanAtHigh = config.analogOut3Voltage.fanAtHigh.currentVal.toInt()
        )

        state.analogOut4MinMax = AnalogOutVoltage(
            fanMin = config.analogOut4Voltage.fanMin.currentVal.toInt(),
            fanMax = config.analogOut4Voltage.fanMax.currentVal.toInt(),
            oaoDamperMinVoltage = config.analogOut4Voltage.oaoDamperMinVoltage.currentVal.toInt(),
            oaoDamperMaxVoltage = config.analogOut4Voltage.oaoDamperMaxVoltage.currentVal.toInt(),
            hotWaterValveMinVoltage = config.analogOut4Voltage.hotWaterValveMinVoltage.currentVal.toInt(),
            hotWaterValveMaxVoltage = config.analogOut4Voltage.hotWaterValveMaxVoltage.currentVal.toInt(),
            coolingWaterValveMinVoltage = config.analogOut4Voltage.coolingWaterValveMinVoltage.currentVal.toInt(),
            coolingWaterValveMaxVoltage = config.analogOut4Voltage.coolingWaterValveMaxVoltage.currentVal.toInt(),
            dcvModulationMinVoltage = config.analogOut4Voltage.dcvModulationMinVoltage.currentVal.toInt(),
            dcvModulationMaxVoltage = config.analogOut4Voltage.dcvModulationMaxVoltage.currentVal.toInt(),
            faceAndBypassDamperMin = config.analogOut4Voltage.faceAndBypassDamperMin.currentVal.toInt(),
            faceAndBypassDamperMax = config.analogOut4Voltage.faceAndBypassDamperMax.currentVal.toInt(),
            fanAtLow = config.analogOut4Voltage.fanAtLow.currentVal.toInt(),
            fanAtMedium = config.analogOut4Voltage.fanAtMedium.currentVal.toInt(),
            fanAtHigh = config.analogOut4Voltage.fanAtHigh.currentVal.toInt()
        )

        state.fanEconomizer = config.fanEconomizer.currentVal.toInt()
        state.fanRecirculate = config.fanRecirculate.currentVal.toInt()
    }

    data class AnalogOutVoltage(
        var fanMin: Int, var fanMax: Int,
        var fanAtLow:Int, var fanAtMedium :Int, var fanAtHigh :Int,
        var oaoDamperMinVoltage: Int, var oaoDamperMaxVoltage: Int,
        var hotWaterValveMinVoltage: Int, var hotWaterValveMaxVoltage: Int,
        var coolingWaterValveMinVoltage: Int, var coolingWaterValveMaxVoltage: Int,
        var dcvModulationMinVoltage: Int, var dcvModulationMaxVoltage: Int,
        var faceAndBypassDamperMin :Int, var faceAndBypassDamperMax : Int
    )

    class ConfigState(enabled: Boolean,association: Int) {
        var enabled by mutableStateOf(enabled)
        var association by mutableIntStateOf(association)
    }

}
