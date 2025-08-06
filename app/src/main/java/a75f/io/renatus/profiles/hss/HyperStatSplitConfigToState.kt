package a75f.io.renatus.profiles.hss

import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.renatus.profiles.hss.cpu.AnalogOutVoltage
import a75f.io.renatus.profiles.hss.cpu.HyperStatSplitCpuState
import a75f.io.renatus.profiles.hss.cpu.StagedFanVoltage

fun configSensorAddress(config: HyperStatSplitConfiguration, state: HyperStatSplitState) {
    state.sensorAddress0 = ConfigState(config.address0Enabled.enabled, config.address0SensorAssociation.temperatureAssociation.associationVal)
    state.pressureSensorAddress0 = ConfigState(config.sensorBusPressureEnable.enabled, config.pressureAddress0SensorAssociation.associationVal)
    state.sensorAddress1 = ConfigState(config.address1Enabled.enabled, config.address1SensorAssociation.temperatureAssociation.associationVal)
    state.sensorAddress2 = ConfigState(config.address2Enabled.enabled, config.address2SensorAssociation.temperatureAssociation.associationVal)
}

fun configUniversalIn(config: HyperStatSplitConfiguration, state: HyperStatSplitState) {
    state.universalIn1Config = ConfigState(config.universal1InEnabled.enabled, config.universal1InAssociation.associationVal)
    state.universalIn2Config = ConfigState(config.universal2InEnabled.enabled, config.universal2InAssociation.associationVal)
    state.universalIn3Config = ConfigState(config.universal3InEnabled.enabled, config.universal3InAssociation.associationVal)
    state.universalIn4Config = ConfigState(config.universal4InEnabled.enabled, config.universal4InAssociation.associationVal)
    state.universalIn5Config = ConfigState(config.universal5InEnabled.enabled, config.universal5InAssociation.associationVal)
    state.universalIn6Config = ConfigState(config.universal6InEnabled.enabled, config.universal6InAssociation.associationVal)
    state.universalIn7Config = ConfigState(config.universal7InEnabled.enabled, config.universal7InAssociation.associationVal)
    state.universalIn8Config = ConfigState(config.universal8InEnabled.enabled, config.universal8InAssociation.associationVal)
}

fun configAnalogOut(config: HyperStatSplitConfiguration, state: HyperStatSplitState) {
    state.analogOut1Enabled = config.analogOut1Enabled.enabled
    state.analogOut2Enabled = config.analogOut2Enabled.enabled
    state.analogOut3Enabled = config.analogOut3Enabled.enabled
    state.analogOut4Enabled = config.analogOut4Enabled.enabled

    state.analogOut1Association = config.analogOut1Association.associationVal
    state.analogOut2Association = config.analogOut2Association.associationVal
    state.analogOut3Association = config.analogOut3Association.associationVal
    state.analogOut4Association = config.analogOut4Association.associationVal

    if (config is HyperStatSplitCpuConfiguration && state is HyperStatSplitCpuState) {
        updateAnalogOutDynamicConfig(config, state)
    }
}

fun configRelay(config: HyperStatSplitConfiguration, state: HyperStatSplitState) {
    state.relay1Config = ConfigState(config.relay1Enabled.enabled, config.relay1Association.associationVal)
    state.relay2Config = ConfigState(config.relay2Enabled.enabled, config.relay2Association.associationVal)
    state.relay3Config = ConfigState(config.relay3Enabled.enabled, config.relay3Association.associationVal)
    state.relay4Config = ConfigState(config.relay4Enabled.enabled, config.relay4Association.associationVal)
    state.relay5Config = ConfigState(config.relay5Enabled.enabled, config.relay5Association.associationVal)
    state.relay6Config = ConfigState(config.relay6Enabled.enabled, config.relay6Association.associationVal)
    state.relay7Config = ConfigState(config.relay7Enabled.enabled, config.relay7Association.associationVal)
    state.relay8Config = ConfigState(config.relay8Enabled.enabled, config.relay8Association.associationVal)
}

fun configMisc(config: HyperStatSplitConfiguration, state: HyperStatSplitState) {
    state.temperatureOffset = config.temperatureOffset.currentVal

    state.autoForceOccupied = config.autoForceOccupied.enabled
    state.autoAway = config.autoAway.enabled
    state.enableOutsideAirOptimization = config.enableOutsideAirOptimization.enabled
    state.prePurge = config.prePurge.enabled

    state.outsideDamperMinOpenDuringRecirc = config.outsideDamperMinOpenDuringRecirc.currentVal
    state.outsideDamperMinOpenDuringConditioning = config.outsideDamperMinOpenDuringConditioning.currentVal
    state.outsideDamperMinOpenDuringFanLow = config.outsideDamperMinOpenDuringFanLow.currentVal
    state.outsideDamperMinOpenDuringFanMedium = config.outsideDamperMinOpenDuringFanMedium.currentVal
    state.outsideDamperMinOpenDuringFanHigh = config.outsideDamperMinOpenDuringFanHigh.currentVal

    state.exhaustFanStage1Threshold = config.exhaustFanStage1Threshold.currentVal
    state.exhaustFanStage2Threshold = config.exhaustFanStage2Threshold.currentVal
    state.exhaustFanHysteresis = config.exhaustFanHysteresis.currentVal

    state.prePurgeOutsideDamperOpen = config.prePurgeOutsideDamperOpen.currentVal

    state.zoneCO2DamperOpeningRate = config.zoneCO2DamperOpeningRate.currentVal
    state.zoneCO2Threshold = config.zoneCO2Threshold.currentVal
    state.zoneCO2Target = config.zoneCO2Target.currentVal

    state.zonePM2p5Target = config.zonePM2p5Target.currentVal

    state.displayHumidity = config.displayHumidity.enabled
    state.displayCO2 = config.displayCO2.enabled
    state.displayPM2p5 = config.displayPM2p5.enabled

    state.disableTouch = config.disableTouch.enabled
    state.backLight = config.backLight.enabled
    state.enableBrightness = config.enableBrightness.enabled

    state.installerPinEnable = config.installerPinEnable.enabled
    state.conditioningModePinEnable = config.conditioningModePinEnable.enabled

    state.spaceTemp = config.spaceTemp.enabled
    state.desiredTemp = config.desiredTemp.enabled

    state.installerPassword = config.installerPassword.currentVal
    state.conditioningModePassword = config.conditioningModePassword.currentVal
}