package a75f.io.renatus.profiles.hss.cpu

import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.renatus.profiles.hss.HyperStatSplitState
import a75f.io.renatus.profiles.hss.configAnalogOut
import a75f.io.renatus.profiles.hss.configMisc
import a75f.io.renatus.profiles.hss.configRelay
import a75f.io.renatus.profiles.hss.configSensorAddress
import a75f.io.renatus.profiles.hss.configUniversalIn
import a75f.io.renatus.profiles.hss.updateDynamicPoints
import a75f.io.renatus.profiles.hss.updateHyperStatSplitConfigFromState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
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

    fun updateConfigFromViewState (config: HyperStatSplitCpuConfiguration) {
        return updateHyperStatSplitConfigFromState(config, this).apply {
            updateDynamicPoints(config, this@HyperStatSplitCpuState)
        }
    }

    fun equalsViewState (otherViewState : HyperStatSplitCpuState) : Boolean {
        return (
            this.temperatureOffset == otherViewState.temperatureOffset &&
            this.autoForceOccupied == otherViewState.autoForceOccupied &&
            this.autoAway == otherViewState.autoAway &&
            this.enableOutsideAirOptimization == otherViewState.enableOutsideAirOptimization &&
            this.prePurge == otherViewState.prePurge &&

            this.sensorAddress0.enabled == otherViewState.sensorAddress0.enabled &&
            this.sensorAddress0.association == otherViewState.sensorAddress0.association &&
            this.pressureSensorAddress0.enabled == otherViewState.pressureSensorAddress0.enabled &&
            this.pressureSensorAddress0.association == otherViewState.pressureSensorAddress0.association &&
            this.sensorAddress1.enabled == otherViewState.sensorAddress1.enabled &&
            this.sensorAddress1.association == otherViewState.sensorAddress1.association &&
            this.sensorAddress2.enabled == otherViewState.sensorAddress2.enabled &&
            this.sensorAddress2.association == otherViewState.sensorAddress2.association &&

            this.universalIn1Config.enabled == otherViewState.universalIn1Config.enabled &&
            this.universalIn2Config.enabled == otherViewState.universalIn2Config.enabled &&
            this.universalIn3Config.enabled == otherViewState.universalIn3Config.enabled &&
            this.universalIn4Config.enabled == otherViewState.universalIn4Config.enabled &&
            this.universalIn5Config.enabled == otherViewState.universalIn5Config.enabled &&
            this.universalIn6Config.enabled == otherViewState.universalIn6Config.enabled &&
            this.universalIn7Config.enabled == otherViewState.universalIn7Config.enabled &&
            this.universalIn8Config.enabled == otherViewState.universalIn8Config.enabled &&
            this.universalIn1Config.association == otherViewState.universalIn1Config.association &&
            this.universalIn2Config.association == otherViewState.universalIn2Config.association &&
            this.universalIn3Config.association == otherViewState.universalIn3Config.association &&
            this.universalIn4Config.association == otherViewState.universalIn4Config.association &&
            this.universalIn5Config.association == otherViewState.universalIn5Config.association &&
            this.universalIn6Config.association == otherViewState.universalIn6Config.association &&
            this.universalIn7Config.association == otherViewState.universalIn7Config.association &&
            this.universalIn8Config.association == otherViewState.universalIn8Config.association &&

            this.relay1Config.enabled == otherViewState.relay1Config.enabled &&
            this.relay2Config.enabled == otherViewState.relay2Config.enabled &&
            this.relay3Config.enabled == otherViewState.relay3Config.enabled &&
            this.relay4Config.enabled == otherViewState.relay4Config.enabled &&
            this.relay5Config.enabled == otherViewState.relay5Config.enabled &&
            this.relay6Config.enabled == otherViewState.relay6Config.enabled &&
            this.relay7Config.enabled == otherViewState.relay7Config.enabled &&
            this.relay8Config.enabled == otherViewState.relay8Config.enabled &&
            this.relay1Config.association == otherViewState.relay1Config.association &&
            this.relay2Config.association == otherViewState.relay2Config.association &&
            this.relay3Config.association == otherViewState.relay3Config.association &&
            this.relay4Config.association == otherViewState.relay4Config.association &&
            this.relay5Config.association == otherViewState.relay5Config.association &&
            this.relay6Config.association == otherViewState.relay6Config.association &&
            this.relay7Config.association == otherViewState.relay7Config.association &&
            this.relay8Config.association == otherViewState.relay8Config.association &&

            this.analogOut1Enabled == otherViewState.analogOut1Enabled &&
            this.analogOut2Enabled == otherViewState.analogOut2Enabled &&
            this.analogOut3Enabled == otherViewState.analogOut3Enabled &&
            this.analogOut4Enabled == otherViewState.analogOut4Enabled &&
            this.analogOut1Association == otherViewState.analogOut1Association &&
            this.analogOut2Association == otherViewState.analogOut2Association &&
            this.analogOut3Association == otherViewState.analogOut3Association &&
            this.analogOut4Association == otherViewState.analogOut4Association &&

            this.analogOut1MinMax.coolingMinVoltage == otherViewState.analogOut1MinMax.coolingMinVoltage &&
            this.analogOut1MinMax.coolingMaxVoltage == otherViewState.analogOut1MinMax.coolingMaxVoltage &&
            this.analogOut1MinMax.heatingMinVoltage == otherViewState.analogOut1MinMax.heatingMinVoltage &&
            this.analogOut1MinMax.heatingMaxVoltage == otherViewState.analogOut1MinMax.heatingMaxVoltage &&
            this.analogOut1MinMax.oaoDamperMinVoltage == otherViewState.analogOut1MinMax.oaoDamperMinVoltage &&
            this.analogOut1MinMax.oaoDamperMaxVoltage == otherViewState.analogOut1MinMax.oaoDamperMaxVoltage &&
            this.analogOut1MinMax.returnDamperMinVoltage == otherViewState.analogOut1MinMax.returnDamperMinVoltage &&
            this.analogOut1MinMax.returnDamperMaxVoltage == otherViewState.analogOut1MinMax.returnDamperMaxVoltage &&
            this.analogOut1MinMax.linearFanMinVoltage == otherViewState.analogOut1MinMax.linearFanMinVoltage &&
            this.analogOut1MinMax.linearFanMaxVoltage == otherViewState.analogOut1MinMax.linearFanMaxVoltage &&
            this.analogOut1MinMax.linearFanAtFanLow == otherViewState.analogOut1MinMax.linearFanAtFanLow &&
            this.analogOut1MinMax.linearFanAtFanMedium == otherViewState.analogOut1MinMax.linearFanAtFanMedium &&
            this.analogOut1MinMax.linearFanAtFanHigh == otherViewState.analogOut1MinMax.linearFanAtFanHigh &&

            this.analogOut2MinMax.coolingMinVoltage == otherViewState.analogOut2MinMax.coolingMinVoltage &&
            this.analogOut2MinMax.coolingMaxVoltage == otherViewState.analogOut2MinMax.coolingMaxVoltage &&
            this.analogOut2MinMax.heatingMinVoltage == otherViewState.analogOut2MinMax.heatingMinVoltage &&
            this.analogOut2MinMax.heatingMaxVoltage == otherViewState.analogOut2MinMax.heatingMaxVoltage &&
            this.analogOut2MinMax.oaoDamperMinVoltage == otherViewState.analogOut2MinMax.oaoDamperMinVoltage &&
            this.analogOut2MinMax.oaoDamperMaxVoltage == otherViewState.analogOut2MinMax.oaoDamperMaxVoltage &&
            this.analogOut2MinMax.returnDamperMinVoltage == otherViewState.analogOut2MinMax.returnDamperMinVoltage &&
            this.analogOut2MinMax.returnDamperMaxVoltage == otherViewState.analogOut2MinMax.returnDamperMaxVoltage &&
            this.analogOut2MinMax.linearFanMinVoltage == otherViewState.analogOut2MinMax.linearFanMinVoltage &&
            this.analogOut2MinMax.linearFanMaxVoltage == otherViewState.analogOut2MinMax.linearFanMaxVoltage &&
            this.analogOut2MinMax.linearFanAtFanLow == otherViewState.analogOut2MinMax.linearFanAtFanLow &&
            this.analogOut2MinMax.linearFanAtFanMedium == otherViewState.analogOut2MinMax.linearFanAtFanMedium &&
            this.analogOut2MinMax.linearFanAtFanHigh == otherViewState.analogOut2MinMax.linearFanAtFanHigh &&

            this.analogOut3MinMax.coolingMinVoltage == otherViewState.analogOut3MinMax.coolingMinVoltage &&
            this.analogOut3MinMax.coolingMaxVoltage == otherViewState.analogOut3MinMax.coolingMaxVoltage &&
            this.analogOut3MinMax.heatingMinVoltage == otherViewState.analogOut3MinMax.heatingMinVoltage &&
            this.analogOut3MinMax.heatingMaxVoltage == otherViewState.analogOut3MinMax.heatingMaxVoltage &&
            this.analogOut3MinMax.oaoDamperMinVoltage == otherViewState.analogOut3MinMax.oaoDamperMinVoltage &&
            this.analogOut3MinMax.oaoDamperMaxVoltage == otherViewState.analogOut3MinMax.oaoDamperMaxVoltage &&
            this.analogOut3MinMax.returnDamperMinVoltage == otherViewState.analogOut3MinMax.returnDamperMinVoltage &&
            this.analogOut3MinMax.returnDamperMaxVoltage == otherViewState.analogOut3MinMax.returnDamperMaxVoltage &&
            this.analogOut3MinMax.linearFanMinVoltage == otherViewState.analogOut3MinMax.linearFanMinVoltage &&
            this.analogOut3MinMax.linearFanMaxVoltage == otherViewState.analogOut3MinMax.linearFanMaxVoltage &&
            this.analogOut3MinMax.linearFanAtFanLow == otherViewState.analogOut3MinMax.linearFanAtFanLow &&
            this.analogOut3MinMax.linearFanAtFanMedium == otherViewState.analogOut3MinMax.linearFanAtFanMedium &&
            this.analogOut3MinMax.linearFanAtFanHigh == otherViewState.analogOut3MinMax.linearFanAtFanHigh &&

            this.analogOut4MinMax.coolingMinVoltage == otherViewState.analogOut4MinMax.coolingMinVoltage &&
            this.analogOut4MinMax.coolingMaxVoltage == otherViewState.analogOut4MinMax.coolingMaxVoltage &&
            this.analogOut4MinMax.heatingMinVoltage == otherViewState.analogOut4MinMax.heatingMinVoltage &&
            this.analogOut4MinMax.heatingMaxVoltage == otherViewState.analogOut4MinMax.heatingMaxVoltage &&
            this.analogOut4MinMax.oaoDamperMinVoltage == otherViewState.analogOut4MinMax.oaoDamperMinVoltage &&
            this.analogOut4MinMax.oaoDamperMaxVoltage == otherViewState.analogOut4MinMax.oaoDamperMaxVoltage &&
            this.analogOut4MinMax.returnDamperMinVoltage == otherViewState.analogOut4MinMax.returnDamperMinVoltage &&
            this.analogOut4MinMax.returnDamperMaxVoltage == otherViewState.analogOut4MinMax.returnDamperMaxVoltage &&
            this.analogOut4MinMax.linearFanMinVoltage == otherViewState.analogOut4MinMax.linearFanMinVoltage &&
            this.analogOut4MinMax.linearFanMaxVoltage == otherViewState.analogOut4MinMax.linearFanMaxVoltage &&
            this.analogOut4MinMax.linearFanAtFanLow == otherViewState.analogOut4MinMax.linearFanAtFanLow &&
            this.analogOut4MinMax.linearFanAtFanMedium == otherViewState.analogOut4MinMax.linearFanAtFanMedium &&
            this.analogOut4MinMax.linearFanAtFanHigh == otherViewState.analogOut4MinMax.linearFanAtFanHigh &&

            this.stagedFanVoltages.coolStage1Voltage == otherViewState.stagedFanVoltages.coolStage1Voltage &&
            this.stagedFanVoltages.coolStage2Voltage == otherViewState.stagedFanVoltages.coolStage2Voltage &&
            this.stagedFanVoltages.coolStage3Voltage == otherViewState.stagedFanVoltages.coolStage3Voltage &&
            this.stagedFanVoltages.economizerVoltage == otherViewState.stagedFanVoltages.economizerVoltage &&
            this.stagedFanVoltages.heatStage1Voltage == otherViewState.stagedFanVoltages.heatStage1Voltage &&
            this.stagedFanVoltages.heatStage2Voltage == otherViewState.stagedFanVoltages.heatStage2Voltage &&
            this.stagedFanVoltages.heatStage3Voltage == otherViewState.stagedFanVoltages.heatStage3Voltage &&
            this.stagedFanVoltages.recircVoltage == otherViewState.stagedFanVoltages.recircVoltage &&

            this.outsideDamperMinOpenDuringRecirc == otherViewState.outsideDamperMinOpenDuringRecirc &&
            this.outsideDamperMinOpenDuringConditioning == otherViewState.outsideDamperMinOpenDuringConditioning &&
            this.outsideDamperMinOpenDuringFanLow == otherViewState.outsideDamperMinOpenDuringFanLow &&
            this.outsideDamperMinOpenDuringFanMedium == otherViewState.outsideDamperMinOpenDuringFanMedium &&
            this.outsideDamperMinOpenDuringFanHigh == otherViewState.outsideDamperMinOpenDuringFanHigh &&
            this.exhaustFanStage1Threshold == otherViewState.exhaustFanStage1Threshold &&
            this.exhaustFanStage2Threshold == otherViewState.exhaustFanStage2Threshold &&
            this.exhaustFanHysteresis == otherViewState.exhaustFanHysteresis &&
            this.prePurgeOutsideDamperOpen == otherViewState.prePurgeOutsideDamperOpen &&
            this.zoneCO2DamperOpeningRate == otherViewState.zoneCO2DamperOpeningRate &&
            this.zoneCO2Threshold == otherViewState.zoneCO2Threshold &&
            this.zoneCO2Target == otherViewState.zoneCO2Target &&
            this.zonePM2p5Target == otherViewState.zonePM2p5Target &&
            this.displayHumidity == otherViewState.displayHumidity &&
            this.displayCO2 == otherViewState.displayCO2 &&
            this.displayPM2p5 == otherViewState.displayPM2p5 &&
            this.testStateRelay1 == otherViewState.testStateRelay1 &&
            this.testStateRelay2 == otherViewState.testStateRelay2 &&
            this.testStateRelay3 == otherViewState.testStateRelay3 &&
            this.testStateRelay4 == otherViewState.testStateRelay4 &&
            this.testStateRelay5 == otherViewState.testStateRelay5 &&
            this.testStateRelay6 == otherViewState.testStateRelay6 &&
            this.testStateRelay7 == otherViewState.testStateRelay7 &&
            this.testStateRelay8 == otherViewState.testStateRelay8 &&
            this.testStateAnalogOut1 == otherViewState.testStateAnalogOut1 &&
            this.testStateAnalogOut2 == otherViewState.testStateAnalogOut2 &&
            this.testStateAnalogOut3 == otherViewState.testStateAnalogOut3 &&
            this.testStateAnalogOut4 == otherViewState.testStateAnalogOut4
        )
    }
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