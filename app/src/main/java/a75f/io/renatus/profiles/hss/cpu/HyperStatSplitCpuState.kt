package a75f.io.renatus.profiles.hss.cpu

import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.renatus.profiles.hss.ConfigState
import a75f.io.renatus.profiles.hss.HyperStatSplitState
import a75f.io.renatus.profiles.hss.configAnalogOut
import a75f.io.renatus.profiles.hss.configMisc
import a75f.io.renatus.profiles.hss.configRelay
import a75f.io.renatus.profiles.hss.configSensorAddress
import a75f.io.renatus.profiles.hss.configUniversalIn
import a75f.io.renatus.profiles.hss.updateDynamicPoints
import a75f.io.renatus.profiles.hss.updateHyperStatSplitConfigFromState
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
                configRelay(config, this)
                configMisc(config, this)
            }
        }
    }

    fun updateConfigFromViewState(config: HyperStatSplitCpuConfiguration) {
        return updateHyperStatSplitConfigFromState(config, this).apply {
            updateDynamicPoints(config, this@HyperStatSplitCpuState)
        }
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