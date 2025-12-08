package a75f.io.renatus.profiles.viewstates

import a75f.io.logic.bo.building.statprofiles.StatProfileConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.renatus.profiles.hss.HyperStatSplitState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Author: Manjunath Kundaragi
 * Created on: 13-10-2025
 */
open class ProfileViewState {

    var temperatureOffset by mutableDoubleStateOf(0.0)
    var isEnableAutoForceOccupied by mutableStateOf(false)
    var isEnableAutoAway by mutableStateOf(false)
    
    var enableOutsideAirOptimization by mutableStateOf(false)
    var prePurge by mutableStateOf(false)

    var sensorAddress0 by mutableStateOf(ConfigState(false, 0))
    var pressureSensorAddress0 by mutableStateOf(ConfigState(false, 0))
    var sensorAddress1 by mutableStateOf(ConfigState(false, 0))
    var sensorAddress2 by mutableStateOf(ConfigState(false, 0))

    var relay1Config by mutableStateOf(ConfigState(false, 0))
    var relay2Config by mutableStateOf(ConfigState(false, 0))
    var relay3Config by mutableStateOf(ConfigState(false, 0))
    var relay4Config by mutableStateOf(ConfigState(false, 0))
    var relay5Config by mutableStateOf(ConfigState(false, 0))
    var relay6Config by mutableStateOf(ConfigState(false, 0))
    var relay7Config by mutableStateOf(ConfigState(false, 0))
    var relay8Config by mutableStateOf(ConfigState(false, 0))
    
    var analogOut1Enabled by mutableStateOf(false)
    var analogOut2Enabled by mutableStateOf(false)
    var analogOut3Enabled by mutableStateOf(false)
    var analogOut4Enabled by mutableStateOf(false)

    var analogOut1Association by mutableIntStateOf(0)
    var analogOut2Association by mutableIntStateOf(0)
    var analogOut3Association by mutableIntStateOf(0)
    var analogOut4Association by mutableIntStateOf(0)

    var universalOut1 by mutableStateOf(ConfigState(false, 0))
    var universalOut2 by mutableStateOf(ConfigState(false, 0))

    var analogIn1Config by mutableStateOf(ConfigState(false, 0))
    var analogIn2Config by mutableStateOf(ConfigState(false, 0))

    var thermistor1Config by mutableStateOf(ConfigState(false, 0))
    var thermistor2Config by mutableStateOf(ConfigState(false, 0))

    var universalIn1Config by mutableStateOf(ConfigState(false, 0))
    var universalIn2Config by mutableStateOf(ConfigState(false, 0))
    var universalIn3Config by mutableStateOf(ConfigState(false, 0))
    var universalIn4Config by mutableStateOf(ConfigState(false, 0))
    var universalIn5Config by mutableStateOf(ConfigState(false, 0))
    var universalIn6Config by mutableStateOf(ConfigState(false, 0))
    var universalIn7Config by mutableStateOf(ConfigState(false, 0))
    var universalIn8Config by mutableStateOf(ConfigState(false, 0))

    var zoneCO2DamperOpeningRate by mutableIntStateOf(0)
    var zoneCO2Threshold by mutableDoubleStateOf(0.0)
    var zoneCO2Target by mutableDoubleStateOf(0.0)
    var zonePM2p5Target by mutableDoubleStateOf(0.0)
    var zonePM10Target by mutableDoubleStateOf(0.0)
    var zonePM2p5Threshold by mutableDoubleStateOf(0.0)

    var outsideDamperMinOpenDuringRecirc by mutableDoubleStateOf(0.0)
    var outsideDamperMinOpenDuringConditioning by mutableDoubleStateOf(0.0)
    var outsideDamperMinOpenDuringFanLow by mutableDoubleStateOf(0.0)
    var outsideDamperMinOpenDuringFanMedium by mutableDoubleStateOf(0.0)
    var outsideDamperMinOpenDuringFanHigh by mutableDoubleStateOf(0.0)

    var exhaustFanStage1Threshold by mutableDoubleStateOf(0.0)
    var exhaustFanStage2Threshold by mutableDoubleStateOf(0.0)
    var exhaustFanHysteresis by mutableDoubleStateOf(0.0)
    var prePurgeOutsideDamperOpen by mutableDoubleStateOf(0.0)

    var displayHumidity by mutableStateOf(false)
    var displayCO2 by mutableStateOf(false)
    var displayPM2p5 by mutableStateOf(false)

    var disableTouch by mutableStateOf(false)
    var backLight by mutableStateOf(false)
    var enableBrightness by mutableStateOf(false)

    var conditioningModePinEnable by mutableStateOf(false)
    var conditioningModePassword by mutableStateOf("")
    var installerPassword by mutableStateOf("")
    var installerPinEnable by mutableStateOf(false)

    var desiredTemp by mutableStateOf(false)
    var spaceTemp by mutableStateOf(false)


    // Check if any relay is mapped to the given mapping excluding the ignoreSelection
    // This is useful to check if a mapping is already used by another relay
    // ignoreSelection is the relay config that is being edited currently
    open fun isAnyRelayMapped(mapping: Int, ignoreSelection: ConfigState): Boolean {

        fun checkSelection(config: ConfigState): Boolean {
            config.apply { return (ignoreSelection != this && association == mapping) }
        }

        if (checkSelection(relay1Config)) return true
        if (checkSelection(relay2Config)) return true
        if (checkSelection(relay3Config)) return true
        if (checkSelection(relay4Config)) return true
        if (checkSelection(relay5Config)) return true
        if (checkSelection(relay6Config)) return true

        return false
    }

    fun configSensorAddress(config: StatProfileConfiguration, state: ProfileViewState) {
        config.apply {
            state.sensorAddress0 = ConfigState(address0Enabled.enabled, address0SensorAssociation.temperatureAssociation.associationVal)
            state.pressureSensorAddress0 = ConfigState(sensorBusPressureEnable.enabled, pressureAddress0SensorAssociation.associationVal)
            state.sensorAddress1 = ConfigState(address1Enabled.enabled, address1SensorAssociation.temperatureAssociation.associationVal)
            state.sensorAddress2 = ConfigState(address2Enabled.enabled, address2SensorAssociation.temperatureAssociation.associationVal)
        }
    }
    
    fun configRelay(config: StatProfileConfiguration, state: ProfileViewState) {
        config.apply {
            state.relay1Config = ConfigState(relay1Enabled.enabled, relay1Association.associationVal)
            state.relay2Config = ConfigState(relay2Enabled.enabled, relay2Association.associationVal)
            state.relay3Config = ConfigState(relay3Enabled.enabled, relay3Association.associationVal)
            state.relay4Config = ConfigState(relay4Enabled.enabled, relay4Association.associationVal)
            state.relay5Config = ConfigState(relay5Enabled.enabled, relay5Association.associationVal)
            state.relay6Config = ConfigState(relay6Enabled.enabled, relay6Association.associationVal)
            state.relay7Config = ConfigState(relay7Enabled.enabled, relay7Association.associationVal)
            state.relay8Config = ConfigState(relay8Enabled.enabled, relay8Association.associationVal)
        }
    }

    fun configAnalogOut(config: StatProfileConfiguration, state: ProfileViewState) {
        config.apply {
            state.analogOut1Enabled = analogOut1Enabled.enabled
            state.analogOut2Enabled = analogOut2Enabled.enabled
            state.analogOut3Enabled = analogOut3Enabled.enabled
            state.analogOut4Enabled = analogOut4Enabled.enabled

            state.analogOut1Association = analogOut1Association.associationVal
            state.analogOut2Association = analogOut2Association.associationVal
            state.analogOut3Association = analogOut3Association.associationVal
            state.analogOut4Association = analogOut4Association.associationVal
        }

    }

    fun configMisc(config: StatProfileConfiguration, state: ProfileViewState)  {

        config.apply {
            state.temperatureOffset = temperatureOffset.currentVal
            state.isEnableAutoForceOccupied = autoForceOccupied.enabled
            state.isEnableAutoAway = autoAway.enabled
            state.enableOutsideAirOptimization = enableOutsideAirOptimization.enabled
            state.prePurge = prePurge.enabled

            state.outsideDamperMinOpenDuringRecirc = outsideDamperMinOpenDuringRecirc.currentVal
            state.outsideDamperMinOpenDuringConditioning = outsideDamperMinOpenDuringConditioning.currentVal
            state.outsideDamperMinOpenDuringFanLow = outsideDamperMinOpenDuringFanLow.currentVal
            state.outsideDamperMinOpenDuringFanMedium = outsideDamperMinOpenDuringFanMedium.currentVal
            state.outsideDamperMinOpenDuringFanHigh = outsideDamperMinOpenDuringFanHigh.currentVal
            state.prePurgeOutsideDamperOpen = prePurgeOutsideDamperOpen.currentVal
            state.exhaustFanStage1Threshold = exhaustFanStage1Threshold.currentVal
            state.exhaustFanStage2Threshold = exhaustFanStage2Threshold.currentVal
            state.exhaustFanHysteresis = exhaustFanHysteresis.currentVal

            state.zoneCO2DamperOpeningRate = zoneCO2DamperOpeningRate.currentVal.toInt()
            state.zoneCO2Threshold = zoneCO2Threshold.currentVal
            state.zoneCO2Target = zoneCO2Target.currentVal
            state.zonePM2p5Target = zonePM2p5Target.currentVal

            state.displayHumidity = displayHumidity.enabled
            state.displayCO2 = displayCO2.enabled
            state.displayPM2p5 = displayPM2p5.enabled

            state.disableTouch = disableTouch.enabled
            state.backLight = backLight.enabled
            state.enableBrightness = enableBrightness.enabled

            state.installerPinEnable = installerPinEnable.enabled
            state.conditioningModePinEnable = conditioningModePinEnable.enabled

            state.spaceTemp = spaceTemp.enabled
            state.desiredTemp = desiredTemp.enabled

            state.installerPassword = installerPassword.currentVal
            state.conditioningModePassword = conditioningModePassword.currentVal
        }
    }

    fun configUniversalIn(config: StatProfileConfiguration, state: ProfileViewState) {
        config.apply {
            state.universalIn1Config = ConfigState(universal1InEnabled.enabled,universal1InAssociation.associationVal)
            state.universalIn2Config = ConfigState(universal2InEnabled.enabled,universal2InAssociation.associationVal)
            state.universalIn3Config = ConfigState(universal3InEnabled.enabled,universal3InAssociation.associationVal)
            state.universalIn4Config = ConfigState(universal4InEnabled.enabled,universal4InAssociation.associationVal)
            state.universalIn5Config = ConfigState(universal5InEnabled.enabled,universal5InAssociation.associationVal)
            state.universalIn6Config = ConfigState(universal6InEnabled.enabled,universal6InAssociation.associationVal)
            state.universalIn7Config = ConfigState(universal7InEnabled.enabled,universal7InAssociation.associationVal)
            state.universalIn8Config = ConfigState(universal8InEnabled.enabled,universal8InAssociation.associationVal)
        }
    }
    // override the function in child classes if needed
    open fun isDcvMapped() = false
}

fun updateConfigFromState(config: StatProfileConfiguration, state: ProfileViewState) {
    config.apply {
        temperatureOffset.currentVal = state.temperatureOffset
        autoForceOccupied.enabled = state.isEnableAutoForceOccupied
        autoAway.enabled = state.isEnableAutoAway
        enableOutsideAirOptimization.enabled = state.enableOutsideAirOptimization
        prePurge.enabled = state.prePurge

        address0Enabled.enabled = state.sensorAddress0.enabled
        sensorBusPressureEnable.enabled = state.pressureSensorAddress0.enabled
        address1Enabled.enabled = state.sensorAddress1.enabled
        address2Enabled.enabled = state.sensorAddress2.enabled

        address0SensorAssociation.apply {
            temperatureAssociation.associationVal = state.sensorAddress0.association
            humidityAssociation.associationVal = state.sensorAddress0.association
        }
        pressureAddress0SensorAssociation.associationVal = state.pressureSensorAddress0.association
        address1SensorAssociation.apply {
            temperatureAssociation.associationVal = state.sensorAddress1.association
            humidityAssociation.associationVal = state.sensorAddress1.association
        }
        address2SensorAssociation.apply {
            temperatureAssociation.associationVal = state.sensorAddress2.association
            humidityAssociation.associationVal = state.sensorAddress2.association
        }

        relay1Enabled.enabled = state.relay1Config.enabled
        relay2Enabled.enabled = state.relay2Config.enabled
        relay3Enabled.enabled = state.relay3Config.enabled
        relay4Enabled.enabled = state.relay4Config.enabled
        relay5Enabled.enabled = state.relay5Config.enabled
        relay6Enabled.enabled = state.relay6Config.enabled
        relay7Enabled.enabled = state.relay7Config.enabled
        relay8Enabled.enabled = state.relay8Config.enabled

        relay1Association.associationVal = state.relay1Config.association
        relay2Association.associationVal = state.relay2Config.association
        relay3Association.associationVal = state.relay3Config.association
        relay4Association.associationVal = state.relay4Config.association
        relay5Association.associationVal = state.relay5Config.association
        relay6Association.associationVal = state.relay6Config.association
        relay7Association.associationVal = state.relay7Config.association
        relay8Association.associationVal = state.relay8Config.association

        analogOut1Enabled.enabled = state.analogOut1Enabled
        analogOut2Enabled.enabled = state.analogOut2Enabled
        analogOut3Enabled.enabled = state.analogOut3Enabled
        analogOut4Enabled.enabled = state.analogOut4Enabled

        analogOut1Association.associationVal = state.analogOut1Association
        analogOut2Association.associationVal = state.analogOut2Association
        analogOut3Association.associationVal = state.analogOut3Association
        analogOut4Association.associationVal = state.analogOut4Association

        universalOut1.enabled = state.universalOut1.enabled
        universalOut2.enabled = state.universalOut2.enabled

        universalOut1Association.associationVal = state.universalOut1.association
        universalOut2Association.associationVal = state.universalOut2.association

        analogIn1Enabled.enabled = state.analogIn1Config.enabled
        analogIn2Enabled.enabled = state.analogIn2Config.enabled

        thermistor1Enabled.enabled = state.thermistor1Config.enabled
        thermistor2Enabled.enabled = state.thermistor2Config.enabled

        analogIn1Association.associationVal = state.analogIn1Config.association
        analogIn2Association.associationVal = state.analogIn2Config.association

        thermistor1Association.associationVal = state.thermistor1Config.association
        thermistor2Association.associationVal = state.thermistor2Config.association

        universal1InEnabled.enabled = state.universalIn1Config.enabled
        universal2InEnabled.enabled = state.universalIn2Config.enabled
        universal3InEnabled.enabled = state.universalIn3Config.enabled
        universal4InEnabled.enabled = state.universalIn4Config.enabled
        universal5InEnabled.enabled = state.universalIn5Config.enabled
        universal6InEnabled.enabled = state.universalIn6Config.enabled
        universal7InEnabled.enabled = state.universalIn7Config.enabled
        universal8InEnabled.enabled = state.universalIn8Config.enabled

        universal1InAssociation.associationVal = state.universalIn1Config.association
        universal2InAssociation.associationVal = state.universalIn2Config.association
        universal3InAssociation.associationVal = state.universalIn3Config.association
        universal4InAssociation.associationVal = state.universalIn4Config.association
        universal5InAssociation.associationVal = state.universalIn5Config.association
        universal6InAssociation.associationVal = state.universalIn6Config.association
        universal7InAssociation.associationVal = state.universalIn7Config.association
        universal8InAssociation.associationVal = state.universalIn8Config.association

        outsideDamperMinOpenDuringRecirc.currentVal = state.outsideDamperMinOpenDuringRecirc
        outsideDamperMinOpenDuringConditioning.currentVal = state.outsideDamperMinOpenDuringConditioning
        outsideDamperMinOpenDuringFanLow.currentVal = state.outsideDamperMinOpenDuringFanLow
        outsideDamperMinOpenDuringFanMedium.currentVal = state.outsideDamperMinOpenDuringFanMedium
        outsideDamperMinOpenDuringFanHigh.currentVal = state.outsideDamperMinOpenDuringFanHigh

        exhaustFanStage1Threshold.currentVal = state.exhaustFanStage1Threshold
        exhaustFanStage2Threshold.currentVal = state.exhaustFanStage2Threshold
        exhaustFanHysteresis.currentVal = state.exhaustFanHysteresis

        prePurgeOutsideDamperOpen.currentVal = state.prePurgeOutsideDamperOpen

        zoneCO2DamperOpeningRate.currentVal = state.zoneCO2DamperOpeningRate.toDouble()
        zoneCO2Threshold.currentVal = state.zoneCO2Threshold
        zoneCO2Target.currentVal = state.zoneCO2Target

        zonePM2p5Target.currentVal = state.zonePM2p5Target

        displayHumidity.enabled = state.displayHumidity
        displayCO2.enabled = state.displayCO2
        displayPM2p5.enabled = state.displayPM2p5

        disableTouch.enabled = state.disableTouch
        backLight.enabled = state.backLight
        enableBrightness.enabled = state.enableBrightness

        installerPassword.currentVal = state.installerPassword
        conditioningModePassword.currentVal = state.conditioningModePassword
        installerPinEnable.enabled = state.installerPinEnable
        conditioningModePinEnable.enabled = state.conditioningModePinEnable

        desiredTemp.enabled = state.desiredTemp
        spaceTemp.enabled = state.spaceTemp
    }
}
fun updateStateFromConfig(state: ProfileViewState, config: StatProfileConfiguration) {
    state.apply {
        temperatureOffset = config.temperatureOffset.currentVal
        isEnableAutoForceOccupied = config.autoForceOccupied.enabled
        isEnableAutoAway = config.autoAway.enabled
        enableOutsideAirOptimization = config.enableOutsideAirOptimization.enabled
        prePurge = config.prePurge.enabled

        sensorAddress0.enabled = config.address0Enabled.enabled
        pressureSensorAddress0.enabled = config.sensorBusPressureEnable.enabled
        sensorAddress1.enabled = config.address1Enabled.enabled
        sensorAddress2.enabled = config.address2Enabled.enabled

        sensorAddress0.association = config.address0SensorAssociation.temperatureAssociation.associationVal
        sensorAddress1.association = config.address1SensorAssociation.temperatureAssociation.associationVal
        sensorAddress2.association = config.address2SensorAssociation.temperatureAssociation.associationVal
        pressureSensorAddress0.association = config.pressureAddress0SensorAssociation.associationVal

        relay1Config.enabled = config.relay1Enabled.enabled
        relay2Config.enabled = config.relay2Enabled.enabled
        relay3Config.enabled = config.relay3Enabled.enabled
        relay4Config.enabled = config.relay4Enabled.enabled
        relay5Config.enabled = config.relay5Enabled.enabled
        relay6Config.enabled = config.relay6Enabled.enabled
        relay7Config.enabled = config.relay7Enabled.enabled
        relay8Config.enabled = config.relay8Enabled.enabled

        relay1Config.association = config.relay1Association.associationVal
        relay2Config.association = config.relay2Association.associationVal
        relay3Config.association = config.relay3Association.associationVal
        relay4Config.association = config.relay4Association.associationVal
        relay5Config.association = config.relay5Association.associationVal
        relay6Config.association = config.relay6Association.associationVal
        relay7Config.association = config.relay7Association.associationVal
        relay8Config.association = config.relay8Association.associationVal

        analogOut1Enabled = config.analogOut1Enabled.enabled
        analogOut2Enabled = config.analogOut2Enabled.enabled
        analogOut3Enabled = config.analogOut3Enabled.enabled
        analogOut4Enabled = config.analogOut4Enabled.enabled

        analogOut1Association = config.analogOut1Association.associationVal
        analogOut2Association = config.analogOut2Association.associationVal
        analogOut3Association = config.analogOut3Association.associationVal
        analogOut4Association = config.analogOut4Association.associationVal

        universalOut1.enabled = config.universalOut1.enabled
        universalOut2.enabled = config.universalOut2.enabled

        universalOut1.association = config.universalOut1Association.associationVal
        universalOut2.association = config.universalOut2Association.associationVal

        analogIn1Config.enabled = config.analogIn1Enabled.enabled
        analogIn2Config.enabled = config.analogIn2Enabled.enabled
        analogIn1Config.association = config.analogIn1Association.associationVal
        analogIn2Config.association = config.analogIn2Association.associationVal

        thermistor1Config.enabled = config.thermistor1Enabled.enabled
        thermistor2Config.enabled = config.thermistor2Enabled.enabled
        thermistor1Config.association = config.thermistor1Association.associationVal
        thermistor2Config.association = config.thermistor2Association.associationVal

        universalIn1Config.enabled = config.universal1InEnabled.enabled
        universalIn2Config.enabled = config.universal2InEnabled.enabled
        universalIn3Config.enabled = config.universal3InEnabled.enabled
        universalIn4Config.enabled = config.universal4InEnabled.enabled
        universalIn5Config.enabled = config.universal5InEnabled.enabled
        universalIn6Config.enabled = config.universal6InEnabled.enabled
        universalIn7Config.enabled = config.universal7InEnabled.enabled
        universalIn8Config.enabled = config.universal8InEnabled.enabled

        universalIn1Config.association = config.universal1InAssociation.associationVal
        universalIn2Config.association = config.universal2InAssociation.associationVal
        universalIn3Config.association = config.universal3InAssociation.associationVal
        universalIn4Config.association = config.universal4InAssociation.associationVal
        universalIn5Config.association = config.universal5InAssociation.associationVal
        universalIn6Config.association = config.universal6InAssociation.associationVal
        universalIn7Config.association = config.universal7InAssociation.associationVal
        universalIn8Config.association = config.universal8InAssociation.associationVal

        outsideDamperMinOpenDuringRecirc = config.outsideDamperMinOpenDuringRecirc.currentVal
        outsideDamperMinOpenDuringConditioning = config.outsideDamperMinOpenDuringConditioning.currentVal
        outsideDamperMinOpenDuringFanLow = config.outsideDamperMinOpenDuringFanLow.currentVal
        outsideDamperMinOpenDuringFanMedium = config.outsideDamperMinOpenDuringFanMedium.currentVal
        outsideDamperMinOpenDuringFanHigh = config.outsideDamperMinOpenDuringFanHigh.currentVal

        exhaustFanStage1Threshold = config.exhaustFanStage1Threshold.currentVal
        exhaustFanStage2Threshold = config.exhaustFanStage2Threshold.currentVal
        exhaustFanHysteresis = config.exhaustFanHysteresis.currentVal

        prePurgeOutsideDamperOpen = config.prePurgeOutsideDamperOpen.currentVal

        zoneCO2DamperOpeningRate = config.zoneCO2DamperOpeningRate.currentVal.toInt()
        zoneCO2Threshold = config.zoneCO2Threshold.currentVal
        zoneCO2Target = config.zoneCO2Target.currentVal

        zonePM2p5Target = config.zonePM2p5Target.currentVal

        displayHumidity = config.displayHumidity.enabled
        displayCO2 = config.displayCO2.enabled
        displayPM2p5 = config.displayPM2p5.enabled

        disableTouch = config.disableTouch.enabled
        backLight = config.backLight.enabled
        enableBrightness = config.enableBrightness.enabled

        installerPassword = config.installerPassword.currentVal
        conditioningModePassword = config.conditioningModePassword.currentVal
        installerPinEnable = config.installerPinEnable.enabled
        conditioningModePinEnable = config.conditioningModePinEnable.enabled

        desiredTemp = config.desiredTemp.enabled
        spaceTemp = config.spaceTemp.enabled
    }
}


class ConfigState(enabled: Boolean,association: Int) {
    var enabled by mutableStateOf(enabled)
    var association by mutableIntStateOf(association)
}

class MinMaxConfig(min: Int, max: Int) {
    var min by mutableIntStateOf(min)
    var max by mutableIntStateOf(max)
}

class FanSpeedConfig(low: Int, medium: Int, high: Int) {
    var low by mutableIntStateOf(low)
    var medium by mutableIntStateOf(medium)
    var high by mutableIntStateOf(high)
}

class StagedConfig(stage1: Int, stage2: Int, stage3: Int) {
    var stage1 by mutableIntStateOf(stage1)
    var stage2 by mutableIntStateOf(stage2)
    var stage3 by mutableIntStateOf(stage3)
}

class RecirculateConfig(analogOut1: Int, analogOut2: Int, analogOut3: Int) {
    var analogOut1 by mutableIntStateOf(analogOut1)
    var analogOut2 by mutableIntStateOf(analogOut2)
    var analogOut3 by mutableIntStateOf(analogOut3)
}

class ThresholdTargetConfig(threshold: Double, target: Double) {
    var threshold by mutableDoubleStateOf(threshold)
    var target by mutableDoubleStateOf(target)
}