package a75f.io.renatus.profiles.hss.cpu

import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.renatus.profiles.hss.HyperStatSplitState
import a75f.io.renatus.profiles.viewstates.updateConfigFromState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class HyperStatSplitCpuState : HyperStatSplitState() {

    var analogOut1MinMax by mutableStateOf(
        AnalogOutVoltage(
            0, 10, 0, 10, 0, 10, 0, 10,0, 10,0,10,0,10, 7, 8, 10
        )
    )

    var analogOut2MinMax by mutableStateOf(
        AnalogOutVoltage(
            0, 10, 0, 10,0, 10, 0, 10, 0, 10, 0, 10,0, 10, 7, 8, 10
        )
    )

    var analogOut3MinMax by mutableStateOf(
        AnalogOutVoltage(
            0, 10, 0, 10,0, 10, 0, 10, 0, 10, 0, 10,0, 10, 7, 8, 10
        )
    )

    var analogOut4MinMax by mutableStateOf(
        AnalogOutVoltage(
            0, 10, 0, 10,0, 10, 0, 10, 0, 10, 0, 10,0, 10, 7, 8, 10
        )
    )

    var stagedFanVoltages by mutableStateOf(
        StagedFanVoltage(
            4, 7, 7,7, 7, 7, 7, 7,10, 10, 10
        )
    )

    companion object {
        fun fromProfileConfigToState(config: HyperStatSplitCpuConfiguration): HyperStatSplitCpuState {
            return HyperStatSplitCpuState().apply {
                configSensorAddress(config, this)
                configUniversalIn(config, this)
                configAnalogOut(config, this)
                updateAnalogOutDynamicConfig(config, this)
                configRelay(config, this)
                configMisc(config, this)
            }
        }
    }

    fun updateAnalogOutDynamicConfig(config: HyperStatSplitCpuConfiguration, state: HyperStatSplitCpuState) {
        state.analogOut1MinMax = AnalogOutVoltage(
            config.analogOut1Voltage.coolingMinVoltage.currentVal.toInt(),
            config.analogOut1Voltage.coolingMaxVoltage.currentVal.toInt(),
            config.analogOut1Voltage.heatingMinVoltage.currentVal.toInt(),
            config.analogOut1Voltage.heatingMaxVoltage.currentVal.toInt(),
            config.analogOut1Voltage.oaoDamperMinVoltage.currentVal.toInt(),
            config.analogOut1Voltage.oaoDamperMaxVoltage.currentVal.toInt(),
            config.analogOut1Voltage.returnDamperMinVoltage.currentVal.toInt(),
            config.analogOut1Voltage.returnDamperMaxVoltage.currentVal.toInt(),
            config.analogOut1Voltage.compressorMinVoltage.currentVal.toInt(),
            config.analogOut1Voltage.compressorMaxVoltage.currentVal.toInt(),
            config.analogOut1Voltage.dcvModulationMinVoltage.currentVal.toInt(),
            config.analogOut1Voltage.dcvModulationMaxVoltage.currentVal.toInt(),
            config.analogOut1Voltage.linearFanMinVoltage.currentVal.toInt(),
            config.analogOut1Voltage.linearFanMaxVoltage.currentVal.toInt(),
            config.analogOut1Voltage.linearFanAtFanLow.currentVal.toInt(),
            config.analogOut1Voltage.linearFanAtFanMedium.currentVal.toInt(),
            config.analogOut1Voltage.linearFanAtFanHigh.currentVal.toInt()
        )

        state.analogOut2MinMax = AnalogOutVoltage(
            config.analogOut2Voltage.coolingMinVoltage.currentVal.toInt(),
            config.analogOut2Voltage.coolingMaxVoltage.currentVal.toInt(),
            config.analogOut2Voltage.heatingMinVoltage.currentVal.toInt(),
            config.analogOut2Voltage.heatingMaxVoltage.currentVal.toInt(),
            config.analogOut2Voltage.oaoDamperMinVoltage.currentVal.toInt(),
            config.analogOut2Voltage.oaoDamperMaxVoltage.currentVal.toInt(),
            config.analogOut2Voltage.returnDamperMinVoltage.currentVal.toInt(),
            config.analogOut2Voltage.returnDamperMaxVoltage.currentVal.toInt(),
            config.analogOut2Voltage.compressorMinVoltage.currentVal.toInt(),
            config.analogOut2Voltage.compressorMaxVoltage.currentVal.toInt(),
            config.analogOut2Voltage.dcvModulationMinVoltage.currentVal.toInt(),
            config.analogOut2Voltage.dcvModulationMaxVoltage.currentVal.toInt(),
            config.analogOut2Voltage.linearFanMinVoltage.currentVal.toInt(),
            config.analogOut2Voltage.linearFanMaxVoltage.currentVal.toInt(),
            config.analogOut2Voltage.linearFanAtFanLow.currentVal.toInt(),
            config.analogOut2Voltage.linearFanAtFanMedium.currentVal.toInt(),
            config.analogOut2Voltage.linearFanAtFanHigh.currentVal.toInt()
        )

        state.analogOut3MinMax = AnalogOutVoltage(
            config.analogOut3Voltage.coolingMinVoltage.currentVal.toInt(),
            config.analogOut3Voltage.coolingMaxVoltage.currentVal.toInt(),
            config.analogOut3Voltage.heatingMinVoltage.currentVal.toInt(),
            config.analogOut3Voltage.heatingMaxVoltage.currentVal.toInt(),
            config.analogOut3Voltage.oaoDamperMinVoltage.currentVal.toInt(),
            config.analogOut3Voltage.oaoDamperMaxVoltage.currentVal.toInt(),
            config.analogOut3Voltage.returnDamperMinVoltage.currentVal.toInt(),
            config.analogOut3Voltage.returnDamperMaxVoltage.currentVal.toInt(),
            config.analogOut3Voltage.compressorMinVoltage.currentVal.toInt(),
            config.analogOut3Voltage.compressorMaxVoltage.currentVal.toInt(),
            config.analogOut3Voltage.dcvModulationMinVoltage.currentVal.toInt(),
            config.analogOut3Voltage.dcvModulationMaxVoltage.currentVal.toInt(),
            config.analogOut3Voltage.linearFanMinVoltage.currentVal.toInt(),
            config.analogOut3Voltage.linearFanMaxVoltage.currentVal.toInt(),
            config.analogOut3Voltage.linearFanAtFanLow.currentVal.toInt(),
            config.analogOut3Voltage.linearFanAtFanMedium.currentVal.toInt(),
            config.analogOut3Voltage.linearFanAtFanHigh.currentVal.toInt()
        )

        state.analogOut4MinMax = AnalogOutVoltage(
            config.analogOut4Voltage.coolingMinVoltage.currentVal.toInt(),
            config.analogOut4Voltage.coolingMaxVoltage.currentVal.toInt(),
            config.analogOut4Voltage.heatingMinVoltage.currentVal.toInt(),
            config.analogOut4Voltage.heatingMaxVoltage.currentVal.toInt(),
            config.analogOut4Voltage.oaoDamperMinVoltage.currentVal.toInt(),
            config.analogOut4Voltage.oaoDamperMaxVoltage.currentVal.toInt(),
            config.analogOut4Voltage.returnDamperMinVoltage.currentVal.toInt(),
            config.analogOut4Voltage.returnDamperMaxVoltage.currentVal.toInt(),
            config.analogOut4Voltage.compressorMinVoltage.currentVal.toInt(),
            config.analogOut4Voltage.compressorMaxVoltage.currentVal.toInt(),
            config.analogOut4Voltage.dcvModulationMinVoltage.currentVal.toInt(),
            config.analogOut4Voltage.dcvModulationMaxVoltage.currentVal.toInt(),
            config.analogOut4Voltage.linearFanMinVoltage.currentVal.toInt(),
            config.analogOut4Voltage.linearFanMaxVoltage.currentVal.toInt(),
            config.analogOut4Voltage.linearFanAtFanLow.currentVal.toInt(),
            config.analogOut4Voltage.linearFanAtFanMedium.currentVal.toInt(),
            config.analogOut4Voltage.linearFanAtFanHigh.currentVal.toInt()
        )

        state.stagedFanVoltages = StagedFanVoltage(
            config.stagedFanVoltages.recircVoltage.currentVal.toInt(),
            config.stagedFanVoltages.economizerVoltage.currentVal.toInt(),
            config.stagedFanVoltages.heatStage1Voltage.currentVal.toInt(),
            config.stagedFanVoltages.coolStage1Voltage.currentVal.toInt(),
            config.stagedFanVoltages.compressorStage1Voltage.currentVal.toInt(),
            config.stagedFanVoltages.heatStage2Voltage.currentVal.toInt(),
            config.stagedFanVoltages.coolStage2Voltage.currentVal.toInt(),
            config.stagedFanVoltages.compressorStage2Voltage.currentVal.toInt(),
            config.stagedFanVoltages.heatStage3Voltage.currentVal.toInt(),
            config.stagedFanVoltages.coolStage3Voltage.currentVal.toInt(),
            config.stagedFanVoltages.compressorStage3Voltage.currentVal.toInt()
        )
    }

    fun updateConfigFromViewState(config: HyperStatSplitCpuConfiguration) {
        updateConfigFromState(config, this).apply {
            updateDynamicPoints(config, this@HyperStatSplitCpuState)
        }
    }

    private fun updateDynamicPoints(config: HyperStatSplitCpuConfiguration, state: HyperStatSplitCpuState) {
        config.analogOut1Voltage.coolingMinVoltage.currentVal = state.analogOut1MinMax.coolingMinVoltage.toDouble()
        config.analogOut1Voltage.coolingMaxVoltage.currentVal = state.analogOut1MinMax.coolingMaxVoltage.toDouble()
        config.analogOut1Voltage.heatingMinVoltage.currentVal = state.analogOut1MinMax.heatingMinVoltage.toDouble()
        config.analogOut1Voltage.heatingMaxVoltage.currentVal = state.analogOut1MinMax.heatingMaxVoltage.toDouble()
        config.analogOut1Voltage.oaoDamperMinVoltage.currentVal = state.analogOut1MinMax.oaoDamperMinVoltage.toDouble()
        config.analogOut1Voltage.oaoDamperMaxVoltage.currentVal = state.analogOut1MinMax.oaoDamperMaxVoltage.toDouble()
        config.analogOut1Voltage.returnDamperMinVoltage.currentVal = state.analogOut1MinMax.returnDamperMinVoltage.toDouble()
        config.analogOut1Voltage.compressorMinVoltage.currentVal = state.analogOut1MinMax.compressorMinVoltage.toDouble()
        config.analogOut1Voltage.compressorMaxVoltage.currentVal = state.analogOut1MinMax.compressorMaxVoltage.toDouble()
        config.analogOut1Voltage.dcvModulationMinVoltage.currentVal = state.analogOut1MinMax.dcvModulationMinVoltage.toDouble()
        config.analogOut1Voltage.dcvModulationMaxVoltage.currentVal = state.analogOut1MinMax.dcvModulationMaxVoltage.toDouble()
        config.analogOut1Voltage.returnDamperMaxVoltage.currentVal = state.analogOut1MinMax.returnDamperMaxVoltage.toDouble()
        config.analogOut1Voltage.linearFanMinVoltage.currentVal = state.analogOut1MinMax.linearFanMinVoltage.toDouble()
        config.analogOut1Voltage.linearFanMaxVoltage.currentVal = state.analogOut1MinMax.linearFanMaxVoltage.toDouble()
        config.analogOut1Voltage.linearFanAtFanLow.currentVal = state.analogOut1MinMax.linearFanAtFanLow.toDouble()
        config.analogOut1Voltage.linearFanAtFanMedium.currentVal = state.analogOut1MinMax.linearFanAtFanMedium.toDouble()
        config.analogOut1Voltage.linearFanAtFanHigh.currentVal = state.analogOut1MinMax.linearFanAtFanHigh.toDouble()

        config.analogOut2Voltage.coolingMinVoltage.currentVal = state.analogOut2MinMax.coolingMinVoltage.toDouble()
        config.analogOut2Voltage.coolingMaxVoltage.currentVal = state.analogOut2MinMax.coolingMaxVoltage.toDouble()
        config.analogOut2Voltage.heatingMinVoltage.currentVal = state.analogOut2MinMax.heatingMinVoltage.toDouble()
        config.analogOut2Voltage.heatingMaxVoltage.currentVal = state.analogOut2MinMax.heatingMaxVoltage.toDouble()
        config.analogOut2Voltage.oaoDamperMinVoltage.currentVal = state.analogOut2MinMax.oaoDamperMinVoltage.toDouble()
        config.analogOut2Voltage.oaoDamperMaxVoltage.currentVal = state.analogOut2MinMax.oaoDamperMaxVoltage.toDouble()
        config.analogOut2Voltage.returnDamperMinVoltage.currentVal = state.analogOut2MinMax.returnDamperMinVoltage.toDouble()
        config.analogOut2Voltage.returnDamperMaxVoltage.currentVal = state.analogOut2MinMax.returnDamperMaxVoltage.toDouble()
        config.analogOut2Voltage.compressorMinVoltage.currentVal = state.analogOut2MinMax.compressorMinVoltage.toDouble()
        config.analogOut2Voltage.compressorMaxVoltage.currentVal = state.analogOut2MinMax.compressorMaxVoltage.toDouble()
        config.analogOut2Voltage.dcvModulationMinVoltage.currentVal = state.analogOut2MinMax.dcvModulationMinVoltage.toDouble()
        config.analogOut2Voltage.dcvModulationMaxVoltage.currentVal = state.analogOut2MinMax.dcvModulationMaxVoltage.toDouble()
        config.analogOut2Voltage.linearFanMinVoltage.currentVal = state.analogOut2MinMax.linearFanMinVoltage.toDouble()
        config.analogOut2Voltage.linearFanMaxVoltage.currentVal = state.analogOut2MinMax.linearFanMaxVoltage.toDouble()
        config.analogOut2Voltage.linearFanAtFanLow.currentVal = state.analogOut2MinMax.linearFanAtFanLow.toDouble()
        config.analogOut2Voltage.linearFanAtFanMedium.currentVal = state.analogOut2MinMax.linearFanAtFanMedium.toDouble()
        config.analogOut2Voltage.linearFanAtFanHigh.currentVal = state.analogOut2MinMax.linearFanAtFanHigh.toDouble()

        config.analogOut3Voltage.coolingMinVoltage.currentVal = state.analogOut3MinMax.coolingMinVoltage.toDouble()
        config.analogOut3Voltage.coolingMaxVoltage.currentVal = state.analogOut3MinMax.coolingMaxVoltage.toDouble()
        config.analogOut3Voltage.heatingMinVoltage.currentVal = state.analogOut3MinMax.heatingMinVoltage.toDouble()
        config.analogOut3Voltage.heatingMaxVoltage.currentVal = state.analogOut3MinMax.heatingMaxVoltage.toDouble()
        config.analogOut3Voltage.oaoDamperMinVoltage.currentVal = state.analogOut3MinMax.oaoDamperMinVoltage.toDouble()
        config.analogOut3Voltage.oaoDamperMaxVoltage.currentVal = state.analogOut3MinMax.oaoDamperMaxVoltage.toDouble()
        config.analogOut3Voltage.returnDamperMinVoltage.currentVal = state.analogOut3MinMax.returnDamperMinVoltage.toDouble()
        config.analogOut3Voltage.returnDamperMaxVoltage.currentVal = state.analogOut3MinMax.returnDamperMaxVoltage.toDouble()

        config.analogOut3Voltage.compressorMinVoltage.currentVal = state.analogOut3MinMax.compressorMinVoltage.toDouble()
        config.analogOut3Voltage.compressorMaxVoltage.currentVal = state.analogOut3MinMax.compressorMaxVoltage.toDouble()
        config.analogOut3Voltage.dcvModulationMinVoltage.currentVal = state.analogOut3MinMax.dcvModulationMinVoltage.toDouble()
        config.analogOut3Voltage.dcvModulationMaxVoltage.currentVal = state.analogOut3MinMax.dcvModulationMaxVoltage.toDouble()
        config.analogOut3Voltage.linearFanMinVoltage.currentVal = state.analogOut3MinMax.linearFanMinVoltage.toDouble()
        config.analogOut3Voltage.linearFanMaxVoltage.currentVal = state.analogOut3MinMax.linearFanMaxVoltage.toDouble()
        config.analogOut3Voltage.linearFanAtFanLow.currentVal = state.analogOut3MinMax.linearFanAtFanLow.toDouble()
        config.analogOut3Voltage.linearFanAtFanMedium.currentVal = state.analogOut3MinMax.linearFanAtFanMedium.toDouble()
        config.analogOut3Voltage.linearFanAtFanHigh.currentVal = state.analogOut3MinMax.linearFanAtFanHigh.toDouble()

        config.analogOut4Voltage.coolingMinVoltage.currentVal = state.analogOut4MinMax.coolingMinVoltage.toDouble()
        config.analogOut4Voltage.coolingMaxVoltage.currentVal = state.analogOut4MinMax.coolingMaxVoltage.toDouble()
        config.analogOut4Voltage.heatingMinVoltage.currentVal = state.analogOut4MinMax.heatingMinVoltage.toDouble()
        config.analogOut4Voltage.heatingMaxVoltage.currentVal = state.analogOut4MinMax.heatingMaxVoltage.toDouble()
        config.analogOut4Voltage.oaoDamperMinVoltage.currentVal = state.analogOut4MinMax.oaoDamperMinVoltage.toDouble()
        config.analogOut4Voltage.oaoDamperMaxVoltage.currentVal = state.analogOut4MinMax.oaoDamperMaxVoltage.toDouble()
        config.analogOut4Voltage.returnDamperMinVoltage.currentVal = state.analogOut4MinMax.returnDamperMinVoltage.toDouble()
        config.analogOut4Voltage.returnDamperMaxVoltage.currentVal = state.analogOut4MinMax.returnDamperMaxVoltage.toDouble()

        config.analogOut4Voltage.compressorMinVoltage.currentVal = state.analogOut4MinMax.compressorMinVoltage.toDouble()
        config.analogOut4Voltage.compressorMaxVoltage.currentVal = state.analogOut4MinMax.compressorMaxVoltage.toDouble()
        config.analogOut4Voltage.dcvModulationMinVoltage.currentVal = state.analogOut4MinMax.dcvModulationMinVoltage.toDouble()
        config.analogOut4Voltage.dcvModulationMaxVoltage.currentVal = state.analogOut4MinMax.dcvModulationMaxVoltage.toDouble()
        config.analogOut4Voltage.linearFanMinVoltage.currentVal = state.analogOut4MinMax.linearFanMinVoltage.toDouble()
        config.analogOut4Voltage.linearFanMaxVoltage.currentVal = state.analogOut4MinMax.linearFanMaxVoltage.toDouble()
        config.analogOut4Voltage.linearFanAtFanLow.currentVal = state.analogOut4MinMax.linearFanAtFanLow.toDouble()
        config.analogOut4Voltage.linearFanAtFanMedium.currentVal = state.analogOut4MinMax.linearFanAtFanMedium.toDouble()
        config.analogOut4Voltage.linearFanAtFanHigh.currentVal = state.analogOut4MinMax.linearFanAtFanHigh.toDouble()

        config.stagedFanVoltages.recircVoltage.currentVal = state.stagedFanVoltages.recircVoltage.toDouble()
        config.stagedFanVoltages.economizerVoltage.currentVal = state.stagedFanVoltages.economizerVoltage.toDouble()
        config.stagedFanVoltages.heatStage1Voltage.currentVal = state.stagedFanVoltages.heatStage1Voltage.toDouble()
        config.stagedFanVoltages.coolStage1Voltage.currentVal = state.stagedFanVoltages.coolStage1Voltage.toDouble()
        config.stagedFanVoltages.heatStage2Voltage.currentVal = state.stagedFanVoltages.heatStage2Voltage.toDouble()
        config.stagedFanVoltages.coolStage2Voltage.currentVal = state.stagedFanVoltages.coolStage2Voltage.toDouble()
        config.stagedFanVoltages.heatStage3Voltage.currentVal = state.stagedFanVoltages.heatStage3Voltage.toDouble()
        config.stagedFanVoltages.coolStage3Voltage.currentVal = state.stagedFanVoltages.coolStage3Voltage.toDouble()
        config.stagedFanVoltages.compressorStage1Voltage.currentVal = state.stagedFanVoltages.compressorStage1Voltage.toDouble()
        config.stagedFanVoltages.compressorStage2Voltage.currentVal = state.stagedFanVoltages.compressorStage2Voltage.toDouble()
        config.stagedFanVoltages.compressorStage3Voltage.currentVal = state.stagedFanVoltages.compressorStage3Voltage.toDouble()
    }



    private fun compareConfigState(
        a: AnalogOutVoltage,
        b: AnalogOutVoltage
    ): Boolean {
        return (a.compressorMaxVoltage == b.compressorMaxVoltage
                && a.compressorMinVoltage == b.compressorMinVoltage
                && a.coolingMaxVoltage == b.coolingMaxVoltage
                && a.coolingMinVoltage == b.coolingMinVoltage
                && a.heatingMaxVoltage == b.heatingMaxVoltage
                && a.heatingMinVoltage == b.heatingMinVoltage
                && a.oaoDamperMaxVoltage == b.oaoDamperMaxVoltage
                && a.oaoDamperMinVoltage == b.oaoDamperMinVoltage
                && a.returnDamperMaxVoltage == b.returnDamperMaxVoltage
                && a.returnDamperMinVoltage == b.returnDamperMinVoltage
                && a.dcvModulationMaxVoltage == b.dcvModulationMaxVoltage
                && a.dcvModulationMinVoltage == b.dcvModulationMinVoltage
                && a.linearFanMaxVoltage == b.linearFanMaxVoltage
                && a.linearFanMinVoltage == b.linearFanMinVoltage
                && a.linearFanAtFanLow == b.linearFanAtFanLow
                && a.linearFanAtFanMedium == b.linearFanAtFanMedium
                && a.linearFanAtFanHigh == b.linearFanAtFanHigh)
    }

    fun equalsViewState(other: HyperStatSplitCpuState): Boolean =
        super.equalsViewState(this, other) &&
                listOf(
                    compareConfigState(analogOut1MinMax, other.analogOut1MinMax),
                    compareConfigState(analogOut2MinMax, other.analogOut2MinMax),
                    compareConfigState(analogOut3MinMax, other.analogOut3MinMax),
                    compareConfigState(analogOut4MinMax, other.analogOut4MinMax)
                ).all { it }

}
    data class AnalogOutVoltage (
    var coolingMinVoltage: Int, var coolingMaxVoltage: Int,
    var heatingMinVoltage: Int, var heatingMaxVoltage: Int,
    var oaoDamperMinVoltage: Int, var oaoDamperMaxVoltage: Int,
    var returnDamperMinVoltage: Int, var returnDamperMaxVoltage: Int,
    var compressorMinVoltage: Int, var compressorMaxVoltage: Int,
    var dcvModulationMinVoltage: Int, var dcvModulationMaxVoltage: Int,
    var linearFanMinVoltage: Int, var linearFanMaxVoltage: Int,
    var linearFanAtFanLow: Int, var linearFanAtFanMedium: Int,
    var linearFanAtFanHigh: Int
)

data class StagedFanVoltage (
    var recircVoltage: Int, var economizerVoltage: Int,
    var heatStage1Voltage: Int, var coolStage1Voltage: Int, var compressorStage1Voltage: Int,
    var heatStage2Voltage: Int, var coolStage2Voltage: Int, var compressorStage2Voltage: Int,
    var heatStage3Voltage: Int, var coolStage3Voltage: Int, var compressorStage3Voltage: Int,
)