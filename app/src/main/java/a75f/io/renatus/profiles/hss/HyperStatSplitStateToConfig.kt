package a75f.io.renatus.profiles.hss

import a75f.io.logic.bo.building.hyperstatsplit.profiles.HyperStatSplitProfileConfiguration
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuProfileConfiguration
import a75f.io.renatus.profiles.hss.cpu.HyperStatSplitCpuState

fun updateHyperStatSplitConfigFromState(config: HyperStatSplitProfileConfiguration, state: HyperStatSplitState) {
    config.temperatureOffset.currentVal = state.temperatureOffset

    config.autoForceOccupied.enabled = state.autoForceOccupied
    config.autoAway.enabled = state.autoAway
    config.enableOutsideAirOptimization.enabled = state.enableOutsideAirOptimization
    config.prePurge.enabled = state.prePurge

    config.address0Enabled.enabled = state.sensorAddress0.enabled
    config.sensorBusPressureEnable.enabled = state.pressureSensorAddress0.enabled
    config.address1Enabled.enabled = state.sensorAddress1.enabled
    config.address2Enabled.enabled = state.sensorAddress2.enabled

    config.address0SensorAssociation.temperatureAssociation.associationVal = state.sensorAddress0.association
    config.address0SensorAssociation.humidityAssociation.associationVal = state.sensorAddress0.association
    config.pressureAddress0SensorAssociation.associationVal = state.pressureSensorAddress0.association
    config.address1SensorAssociation.temperatureAssociation.associationVal = state.sensorAddress1.association
    config.address1SensorAssociation.humidityAssociation.associationVal = state.sensorAddress1.association
    config.address2SensorAssociation.temperatureAssociation.associationVal = state.sensorAddress2.association
    config.address2SensorAssociation.humidityAssociation.associationVal = state.sensorAddress2.association

    config.relay1Enabled.enabled = state.relay1Config.enabled
    config.relay2Enabled.enabled = state.relay2Config.enabled
    config.relay3Enabled.enabled = state.relay3Config.enabled
    config.relay4Enabled.enabled = state.relay4Config.enabled
    config.relay5Enabled.enabled = state.relay5Config.enabled
    config.relay6Enabled.enabled = state.relay6Config.enabled
    config.relay7Enabled.enabled = state.relay7Config.enabled
    config.relay8Enabled.enabled = state.relay8Config.enabled

    config.relay1Association.associationVal = state.relay1Config.association
    config.relay2Association.associationVal = state.relay2Config.association
    config.relay3Association.associationVal = state.relay3Config.association
    config.relay4Association.associationVal = state.relay4Config.association
    config.relay5Association.associationVal = state.relay5Config.association
    config.relay6Association.associationVal = state.relay6Config.association
    config.relay7Association.associationVal = state.relay7Config.association
    config.relay8Association.associationVal = state.relay8Config.association

    config.analogOut1Enabled.enabled = state.analogOut1Enabled
    config.analogOut2Enabled.enabled = state.analogOut2Enabled
    config.analogOut3Enabled.enabled = state.analogOut3Enabled
    config.analogOut4Enabled.enabled = state.analogOut4Enabled

    config.analogOut1Association.associationVal = state.analogOut1Association
    config.analogOut2Association.associationVal = state.analogOut2Association
    config.analogOut3Association.associationVal = state.analogOut3Association
    config.analogOut4Association.associationVal = state.analogOut4Association

    config.universal1InEnabled.enabled = state.universalIn1Config.enabled
    config.universal2InEnabled.enabled = state.universalIn2Config.enabled
    config.universal3InEnabled.enabled = state.universalIn3Config.enabled
    config.universal4InEnabled.enabled = state.universalIn4Config.enabled
    config.universal5InEnabled.enabled = state.universalIn5Config.enabled
    config.universal6InEnabled.enabled = state.universalIn6Config.enabled
    config.universal7InEnabled.enabled = state.universalIn7Config.enabled
    config.universal8InEnabled.enabled = state.universalIn8Config.enabled

    config.universal1InAssociation.associationVal = state.universalIn1Config.association
    config.universal2InAssociation.associationVal = state.universalIn2Config.association
    config.universal3InAssociation.associationVal = state.universalIn3Config.association
    config.universal4InAssociation.associationVal = state.universalIn4Config.association
    config.universal5InAssociation.associationVal = state.universalIn5Config.association
    config.universal6InAssociation.associationVal = state.universalIn6Config.association
    config.universal7InAssociation.associationVal = state.universalIn7Config.association
    config.universal8InAssociation.associationVal = state.universalIn8Config.association

    config.outsideDamperMinOpenDuringRecirc.currentVal = state.outsideDamperMinOpenDuringRecirc
    config.outsideDamperMinOpenDuringConditioning.currentVal = state.outsideDamperMinOpenDuringConditioning
    config.outsideDamperMinOpenDuringFanLow.currentVal = state.outsideDamperMinOpenDuringFanLow
    config.outsideDamperMinOpenDuringFanMedium.currentVal = state.outsideDamperMinOpenDuringFanMedium
    config.outsideDamperMinOpenDuringFanHigh.currentVal = state.outsideDamperMinOpenDuringFanHigh

    config.exhaustFanStage1Threshold.currentVal = state.exhaustFanStage1Threshold
    config.exhaustFanStage2Threshold.currentVal = state.exhaustFanStage2Threshold
    config.exhaustFanHysteresis.currentVal = state.exhaustFanHysteresis

    config.prePurgeOutsideDamperOpen.currentVal = state.prePurgeOutsideDamperOpen

    config.zoneCO2DamperOpeningRate.currentVal = state.zoneCO2DamperOpeningRate
    config.zoneCO2Threshold.currentVal = state.zoneCO2Threshold
    config.zoneCO2Target.currentVal = state.zoneCO2Target

    config.zonePM2p5Target.currentVal = state.zonePM2p5Target

    config.displayHumidity.enabled = state.displayHumidity
    config.displayCO2.enabled = state.displayCO2
    config.displayPM2p5.enabled = state.displayPM2p5
}

fun updateDynamicPoints(config: HyperStatSplitCpuProfileConfiguration, state: HyperStatSplitCpuState) {
    config.analogOut1Voltage.coolingMinVoltage.currentVal = state.analogOut1MinMax.coolingMinVoltage.toDouble()
    config.analogOut1Voltage.coolingMaxVoltage.currentVal = state.analogOut1MinMax.coolingMaxVoltage.toDouble()
    config.analogOut1Voltage.heatingMinVoltage.currentVal = state.analogOut1MinMax.heatingMinVoltage.toDouble()
    config.analogOut1Voltage.heatingMaxVoltage.currentVal = state.analogOut1MinMax.heatingMaxVoltage.toDouble()
    config.analogOut1Voltage.oaoDamperMinVoltage.currentVal = state.analogOut1MinMax.oaoDamperMinVoltage.toDouble()
    config.analogOut1Voltage.oaoDamperMaxVoltage.currentVal = state.analogOut1MinMax.oaoDamperMaxVoltage.toDouble()
    config.analogOut1Voltage.returnDamperMinVoltage.currentVal = state.analogOut1MinMax.returnDamperMinVoltage.toDouble()
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
}

