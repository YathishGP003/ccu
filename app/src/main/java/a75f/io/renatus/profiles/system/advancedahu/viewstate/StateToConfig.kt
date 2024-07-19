package a75f.io.renatus.profiles.system.advancedahu.viewstate

import a75f.io.logic.bo.building.system.util.AdvancedHybridAhuConfig
import a75f.io.logic.bo.building.system.util.AnalogOutMinMaxVoltage
import a75f.io.logic.bo.building.system.util.SensorAssociationConfig
import a75f.io.renatus.profiles.system.advancedahu.AdvancedHybridAhuState
import a75f.io.renatus.profiles.system.advancedahu.MinMaxVoltage
import a75f.io.renatus.profiles.system.advancedahu.SensorState

/**
 * Created by Manjunath K on 15-04-2024.
 */

/**
 * Updates the state from the config
 */
fun updateSensorAddress(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    config.cmConfiguration.apply {
        this.address0Enabled.enabled = state.sensorAddress0.enabled
        this.address1Enabled.enabled = state.sensorAddress1.enabled
        this.address2Enabled.enabled = state.sensorAddress2.enabled
        this.address3Enabled.enabled = state.sensorAddress3.enabled
        this.sensorBus0PressureEnabled.enabled = state.sensorBusPressureEnable
        updateSensorAssociation(this.address0SensorAssociation, state.sensorAddress0)
        updateSensorAssociation(this.address1SensorAssociation, state.sensorAddress1)
        updateSensorAssociation(this.address2SensorAssociation, state.sensorAddress2)
        updateSensorAssociation(this.address3SensorAssociation, state.sensorAddress3)
    }
}
/**
 * Updates the sensor association
 */
fun updateSensorAssociation(
        association: SensorAssociationConfig, sensorState: SensorState
) {
    association.apply {
        temperatureAssociation.associationVal = sensorState.temperatureAssociation
        humidityAssociation.associationVal = sensorState.humidityAssociation
        co2Association.associationVal = sensorState.co2Association
        occupancyAssociation.associationVal = sensorState.occupancyAssociation
        pressureAssociation?.associationVal = sensorState.pressureAssociation
    }
}

/**
 * Updates the analog in
 */
fun updateAnalogIn(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    config.cmConfiguration.apply {
        this.analog1InEnabled.enabled = state.analogIn1Config.enabled
        this.analog1InAssociation.associationVal = state.analogIn1Config.association
        this.analog2InEnabled.enabled = state.analogIn2Config.enabled
        this.analog2InAssociation.associationVal = state.analogIn2Config.association
    }
}

/**
 * Updates the thermistor
 */
fun updateThermistor(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    config.cmConfiguration.apply {
        this.thermistor1Enabled.enabled = state.thermistor1Config.enabled
        this.thermistor2Enabled.enabled = state.thermistor2Config.enabled
        this.thermistor1Association.associationVal = state.thermistor1Config.association
        this.thermistor2Association.associationVal = state.thermistor2Config.association
    }
}

/**
 * Updates the analog out
 */
fun updateAnalogOut(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    config.cmConfiguration.analogOut1Enabled.enabled = state.analogOut1Enabled
    config.cmConfiguration.analogOut2Enabled.enabled = state.analogOut2Enabled
    config.cmConfiguration.analogOut3Enabled.enabled = state.analogOut3Enabled
    config.cmConfiguration.analogOut4Enabled.enabled = state.analogOut4Enabled
    config.cmConfiguration.analogOut1Association.associationVal = state.analogOut1Association
    config.cmConfiguration.analogOut2Association.associationVal = state.analogOut2Association
    config.cmConfiguration.analogOut3Association.associationVal = state.analogOut3Association
    config.cmConfiguration.analogOut4Association.associationVal = state.analogOut4Association
    config.cmConfiguration.pressureControlAssociation.associationVal =
        state.pressureConfig.pressureControlAssociation
    config.cmConfiguration.satControlAssociation.associationVal =
        state.satConfig.satControlAssociation
    config.cmConfiguration.damperControlAssociation.associationVal =
        state.damperConfig.damperControlAssociation
}

/**
 * Updates the relay
 */
fun updateRelay(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    config.cmConfiguration.apply {
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
    }
}

/**
 * Updates analog out the dynamic points
 */
fun updateDynamicPoints(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    config.cmConfiguration.co2Threshold.currentVal = state.damperConfig.co2Threshold
    config.cmConfiguration.co2Target.currentVal = state.damperConfig.co2Target
    config.cmConfiguration.damperOpeningRate.currentVal = state.damperConfig.openingRate
    config.cmConfiguration.staticMinPressure.currentVal = state.pressureConfig.staticMinPressure
    config.cmConfiguration.staticMaxPressure.currentVal = state.pressureConfig.staticMaxPressure
    config.cmConfiguration.systemSatCoolingMax.currentVal = state.satConfig.systemSatCoolingMax
    config.cmConfiguration.systemSatCoolingMin.currentVal = state.satConfig.systemSatCoolingMin
    config.cmConfiguration.systemSatHeatingMax.currentVal = state.satConfig.systemSatHeatingMax
    config.cmConfiguration.systemSatHeatingMin.currentVal = state.satConfig.systemSatHeatingMin
    configToMinMaxUpdate(config.cmConfiguration.analog1MinMaxVoltage, state.analogOut1MinMax)
    configToMinMaxUpdate(config.cmConfiguration.analog2MinMaxVoltage, state.analogOut2MinMax)
    configToMinMaxUpdate(config.cmConfiguration.analog3MinMaxVoltage, state.analogOut3MinMax)
    configToMinMaxUpdate(config.cmConfiguration.analog4MinMaxVoltage, state.analogOut4MinMax)
}

/**
 * Updates the min max values
 */
private fun configToMinMaxUpdate(analogMinMaxConfig: AnalogOutMinMaxVoltage, minMaxState: MinMaxVoltage) {
    analogMinMaxConfig.apply {
        staticPressureMinVoltage.currentVal = minMaxState.staticPressureMinVoltage.toDouble()
        staticPressureMaxVoltage.currentVal = minMaxState.staticPressureMaxVoltage.toDouble()
        satCoolingMinVoltage.currentVal = minMaxState.satCoolingMinVoltage.toDouble()
        satCoolingMaxVoltage.currentVal = minMaxState.satCoolingMaxVoltage.toDouble()
        satHeatingMinVoltage.currentVal = minMaxState.satHeatingMinVoltage.toDouble()
        satHeatingMaxVoltage.currentVal = minMaxState.satHeatingMaxVoltage.toDouble()
        heatingMinVoltage.currentVal = minMaxState.heatingMinVoltage.toDouble()
        heatingMaxVoltage.currentVal = minMaxState.heatingMaxVoltage.toDouble()
        coolingMinVoltage.currentVal = minMaxState.coolingMinVoltage.toDouble()
        coolingMaxVoltage.currentVal = minMaxState.coolingMaxVoltage.toDouble()
        compositeCoolingMinVoltage.currentVal = minMaxState.compositeCoolingMinVoltage.toDouble()
        compositeCoolingMaxVoltage.currentVal = minMaxState.compositeCoolingMaxVoltage.toDouble()
        compositeHeatingMinVoltage.currentVal = minMaxState.compositeHeatingMinVoltage.toDouble()
        compositeHeatingMaxVoltage.currentVal = minMaxState.compositeHeatingMaxVoltage.toDouble()
        fanMinVoltage.currentVal = minMaxState.fanMinVoltage.toDouble()
        fanMaxVoltage.currentVal = minMaxState.fanMaxVoltage.toDouble()
        damperPosMinVoltage.currentVal = minMaxState.damperPosMinVoltage.toDouble()
        damperPosMaxVoltage.currentVal = minMaxState.damperPosMaxVoltage.toDouble()
    }
}

/**
 * Updates the state from the config
 */
fun updateConnectSensorAddress(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    config.connectConfiguration.apply {
        this.address0Enabled.enabled = state.connectSensorAddress0.enabled
        this.address1Enabled.enabled = state.connectSensorAddress1.enabled
        this.address2Enabled.enabled = state.connectSensorAddress2.enabled
        this.address3Enabled.enabled = state.connectSensorAddress3.enabled
        this.sensorBus0PressureEnabled.enabled = state.connectSensorBusPressureEnable
        updateSensorAssociation(this.address0SensorAssociation, state.connectSensorAddress0)
        updateSensorAssociation(this.address1SensorAssociation, state.connectSensorAddress1)
        updateSensorAssociation(this.address2SensorAssociation, state.connectSensorAddress2)
        updateSensorAssociation(this.address3SensorAssociation, state.connectSensorAddress3)
    }
}

/**
 * Updates the universal in
 */
fun updateConnectUniversalIn(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    config.connectConfiguration.apply {
        this.universal1InEnabled.enabled = state.connectUniversalIn1Config.enabled
        this.universal1InAssociation.associationVal = state.connectUniversalIn1Config.association
        this.universal2InEnabled.enabled = state.connectUniversalIn2Config.enabled
        this.universal2InAssociation.associationVal = state.connectUniversalIn2Config.association
        this.universal3InEnabled.enabled = state.connectUniversalIn3Config.enabled
        this.universal3InAssociation.associationVal = state.connectUniversalIn3Config.association
        this.universal4InEnabled.enabled = state.connectUniversalIn4Config.enabled
        this.universal4InAssociation.associationVal = state.connectUniversalIn4Config.association
        this.universal5InEnabled.enabled = state.connectUniversalIn5Config.enabled
        this.universal5InAssociation.associationVal = state.connectUniversalIn5Config.association
        this.universal6InEnabled.enabled = state.connectUniversalIn6Config.enabled
        this.universal6InAssociation.associationVal = state.connectUniversalIn6Config.association
        this.universal7InEnabled.enabled = state.connectUniversalIn7Config.enabled
        this.universal7InAssociation.associationVal = state.connectUniversalIn7Config.association
        this.universal8InEnabled.enabled = state.connectUniversalIn8Config.enabled
        this.universal8InAssociation.associationVal = state.connectUniversalIn8Config.association
    }
}

/**
 * Updates the analog out
 */
fun updateConnectAnalogOut(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    config.connectConfiguration.analogOut1Enabled.enabled = state.connectAnalogOut1Enabled
    config.connectConfiguration.analogOut2Enabled.enabled = state.connectAnalogOut2Enabled
    config.connectConfiguration.analogOut3Enabled.enabled = state.connectAnalogOut3Enabled
    config.connectConfiguration.analogOut4Enabled.enabled = state.connectAnalogOut4Enabled
    config.connectConfiguration.analogOut1Association.associationVal = state.connectAnalogOut1Association
    config.connectConfiguration.analogOut2Association.associationVal = state.connectAnalogOut2Association
    config.connectConfiguration.analogOut3Association.associationVal = state.connectAnalogOut3Association
    config.connectConfiguration.analogOut4Association.associationVal = state.connectAnalogOut4Association
    config.connectConfiguration.damperControlAssociation.associationVal =
        state.connectDamperConfig.damperControlAssociation
}

/**
 * Updates the relay
 */
fun updateConnectRelay(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    config.connectConfiguration.apply {
        this.relay1Enabled.enabled = state.connectRelay1Config.enabled
        this.relay1Association.associationVal = state.connectRelay1Config.association
        this.relay2Enabled.enabled = state.connectRelay2Config.enabled
        this.relay2Association.associationVal = state.connectRelay2Config.association
        this.relay3Enabled.enabled = state.connectRelay3Config.enabled
        this.relay3Association.associationVal = state.connectRelay3Config.association
        this.relay4Enabled.enabled = state.connectRelay4Config.enabled
        this.relay4Association.associationVal = state.connectRelay4Config.association
        this.relay5Enabled.enabled = state.connectRelay5Config.enabled
        this.relay5Association.associationVal = state.connectRelay5Config.association
        this.relay6Enabled.enabled = state.connectRelay6Config.enabled
        this.relay6Association.associationVal = state.connectRelay6Config.association
        this.relay7Enabled.enabled = state.connectRelay7Config.enabled
        this.relay7Association.associationVal = state.connectRelay7Config.association
        this.relay8Enabled.enabled = state.connectRelay8Config.enabled
        this.relay8Association.associationVal = state.connectRelay8Config.association
    }
}
fun updateConnectDynamicPoints(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    config.connectConfiguration.apply {
        co2Threshold.currentVal = state.connectDamperConfig.co2Threshold
        co2Target.currentVal = state.connectDamperConfig.co2Target
        damperOpeningRate.currentVal = state.connectDamperConfig.openingRate
    }

    configToMinMaxUpdate(config.connectConfiguration.analog1MinMaxVoltage, state.connectAnalogOut1MinMax)
    configToMinMaxUpdate(config.connectConfiguration.analog2MinMaxVoltage, state.connectAnalogOut2MinMax)
    configToMinMaxUpdate(config.connectConfiguration.analog3MinMaxVoltage, state.connectAnalogOut3MinMax)
    configToMinMaxUpdate(config.connectConfiguration.analog4MinMaxVoltage, state.connectAnalogOut4MinMax)
}
