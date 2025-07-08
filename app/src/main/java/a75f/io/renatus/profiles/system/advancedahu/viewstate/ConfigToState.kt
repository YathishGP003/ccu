package a75f.io.renatus.profiles.system.advancedahu.viewstate

import a75f.io.logic.bo.building.system.util.AdvancedHybridAhuConfig
import a75f.io.logic.bo.building.system.util.AnalogOutMinMaxVoltage
import a75f.io.renatus.profiles.system.advancedahu.AdvancedHybridAhuState
import a75f.io.renatus.profiles.system.advancedahu.ConfigState
import a75f.io.renatus.profiles.system.advancedahu.MinMaxVoltage
import a75f.io.renatus.profiles.system.advancedahu.SensorState

/**
 * Created by Manjunath K on 15-04-2024.
 */
/**
 * Configures the sensor address
 */
fun configSensorAddress(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    state.sensorAddress0 = SensorState(config.cmConfiguration.address0Enabled.enabled)
    state.sensorAddress1 = SensorState(config.cmConfiguration.address1Enabled.enabled)
    state.sensorAddress2 = SensorState(config.cmConfiguration.address2Enabled.enabled)
    state.sensorAddress3 = SensorState(config.cmConfiguration.address3Enabled.enabled)
    state.sensorBusPressureEnable = config.cmConfiguration.sensorBus0PressureEnabled.enabled

    state.sensorAddress0.apply {
        temperatureAssociation =
            config.cmConfiguration.address0SensorAssociation.temperatureAssociation.associationVal
        humidityAssociation =
            config.cmConfiguration.address0SensorAssociation.humidityAssociation.associationVal
        co2Association =
            config.cmConfiguration.address0SensorAssociation.co2Association.associationVal
        occupancyAssociation =
            config.cmConfiguration.address0SensorAssociation.occupancyAssociation.associationVal
        pressureAssociation =
            config.cmConfiguration.address0SensorAssociation.pressureAssociation?.associationVal!!
    }
    state.sensorAddress1.apply {
        temperatureAssociation =
            config.cmConfiguration.address1SensorAssociation.temperatureAssociation.associationVal
        humidityAssociation =
            config.cmConfiguration.address1SensorAssociation.humidityAssociation.associationVal
        co2Association =
            config.cmConfiguration.address1SensorAssociation.co2Association.associationVal
        occupancyAssociation =
            config.cmConfiguration.address1SensorAssociation.occupancyAssociation.associationVal
    }
    state.sensorAddress2.apply {
        temperatureAssociation =
            config.cmConfiguration.address2SensorAssociation.temperatureAssociation.associationVal
        humidityAssociation =
            config.cmConfiguration.address2SensorAssociation.humidityAssociation.associationVal
        co2Association =
            config.cmConfiguration.address2SensorAssociation.co2Association.associationVal
        occupancyAssociation =
            config.cmConfiguration.address2SensorAssociation.occupancyAssociation.associationVal
    }
    state.sensorAddress3.apply {
        temperatureAssociation =
            config.cmConfiguration.address3SensorAssociation.temperatureAssociation.associationVal
        humidityAssociation =
            config.cmConfiguration.address3SensorAssociation.humidityAssociation.associationVal
        co2Association =
            config.cmConfiguration.address3SensorAssociation.co2Association.associationVal
        occupancyAssociation =
            config.cmConfiguration.address3SensorAssociation.occupancyAssociation.associationVal
    }
}


/**
 * Configures the analog in
 */
fun configAnalogIn(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    state.analogIn1Config = ConfigState(
        config.cmConfiguration.analog1InEnabled.enabled,
        config.cmConfiguration.analog1InAssociation.associationVal
    )
    state.analogIn2Config = ConfigState(
        config.cmConfiguration.analog2InEnabled.enabled,
        config.cmConfiguration.analog2InAssociation.associationVal
    )
}

/**
 * Configures the thermistor
 */
fun configThermistor(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    state.thermistor1Config = ConfigState(
        config.cmConfiguration.thermistor1Enabled.enabled,
        config.cmConfiguration.thermistor1Association.associationVal
    )
    state.thermistor2Config = ConfigState(
        config.cmConfiguration.thermistor2Enabled.enabled,
        config.cmConfiguration.thermistor2Association.associationVal
    )
}

/**
 * Configures the analog out
 */
fun configAnalogOut(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    state.analogOut1Enabled = config.cmConfiguration.analogOut1Enabled.enabled
    state.analogOut2Enabled = config.cmConfiguration.analogOut2Enabled.enabled
    state.analogOut3Enabled = config.cmConfiguration.analogOut3Enabled.enabled
    state.analogOut4Enabled = config.cmConfiguration.analogOut4Enabled.enabled
    state.analogOut1Association = config.cmConfiguration.analogOut1Association.associationVal
    state.analogOut2Association = config.cmConfiguration.analogOut2Association.associationVal
    state.analogOut3Association = config.cmConfiguration.analogOut3Association.associationVal
    state.analogOut4Association = config.cmConfiguration.analogOut4Association.associationVal
    updateAnalogOutDynamicConfig(config, state)
}

/**
 * Updates the analog out dynamic config
 */
fun updateAnalogOutDynamicConfig(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    state.pressureConfig.apply {
        pressureControlAssociation =
            config.cmConfiguration.pressureControlAssociation.associationVal
        staticMinPressure = config.cmConfiguration.staticMinPressure.currentVal
        staticMaxPressure = config.cmConfiguration.staticMaxPressure.currentVal
    }

    state.satConfig.apply {
        satControlAssociation = config.cmConfiguration.satControlAssociation.associationVal
        systemSatCoolingMax = config.cmConfiguration.systemSatCoolingMax.currentVal
        systemSatCoolingMin = config.cmConfiguration.systemSatCoolingMin.currentVal
        systemSatHeatingMax = config.cmConfiguration.systemSatHeatingMax.currentVal
        systemSatHeatingMin = config.cmConfiguration.systemSatHeatingMin.currentVal
    }

    state.damperConfig.apply {
        damperControlAssociation =
            config.cmConfiguration.damperControlAssociation.associationVal
        co2Target = config.cmConfiguration.co2Target.currentVal
        co2Threshold = config.cmConfiguration.co2Threshold.currentVal
        openingRate = config.cmConfiguration.damperOpeningRate.currentVal
    }
    updateAnalogOutMinMax(state.analogOut1MinMax, config.cmConfiguration.analog1MinMaxVoltage)
    updateAnalogOutMinMax(state.analogOut2MinMax, config.cmConfiguration.analog2MinMaxVoltage)
    updateAnalogOutMinMax(state.analogOut3MinMax, config.cmConfiguration.analog3MinMaxVoltage)
    updateAnalogOutMinMax(state.analogOut4MinMax, config.cmConfiguration.analog4MinMaxVoltage)
}

/**
 * Updates the analog out min max
 */
fun updateAnalogOutMinMax(analogOutMinMax: MinMaxVoltage, analogMinMaxVoltage: AnalogOutMinMaxVoltage) {
    analogOutMinMax.apply {
        staticPressureMinVoltage = analogMinMaxVoltage.staticPressureMinVoltage.currentVal.toInt()
        staticPressureMaxVoltage = analogMinMaxVoltage.staticPressureMaxVoltage.currentVal.toInt()
        satCoolingMinVoltage = analogMinMaxVoltage.satCoolingMinVoltage.currentVal.toInt()
        satCoolingMaxVoltage = analogMinMaxVoltage.satCoolingMaxVoltage.currentVal.toInt()
        satHeatingMinVoltage = analogMinMaxVoltage.satHeatingMinVoltage.currentVal.toInt()
        satHeatingMaxVoltage = analogMinMaxVoltage.satHeatingMaxVoltage.currentVal.toInt()
        heatingMinVoltage = analogMinMaxVoltage.heatingMinVoltage.currentVal.toInt()
        heatingMaxVoltage = analogMinMaxVoltage.heatingMaxVoltage.currentVal.toInt()
        coolingMaxVoltage = analogMinMaxVoltage.coolingMaxVoltage.currentVal.toInt()
        coolingMinVoltage = analogMinMaxVoltage.coolingMinVoltage.currentVal.toInt()
        compositeCoolingMinVoltage = analogMinMaxVoltage.compositeCoolingMinVoltage.currentVal.toInt()
        compositeCoolingMaxVoltage = analogMinMaxVoltage.compositeCoolingMaxVoltage.currentVal.toInt()
        compositeHeatingMinVoltage = analogMinMaxVoltage.compositeHeatingMinVoltage.currentVal.toInt()
        compositeHeatingMaxVoltage = analogMinMaxVoltage.compositeHeatingMaxVoltage.currentVal.toInt()
        fanMinVoltage = analogMinMaxVoltage.fanMinVoltage.currentVal.toInt()
        fanMaxVoltage = analogMinMaxVoltage.fanMaxVoltage.currentVal.toInt()
        damperPosMinVoltage = analogMinMaxVoltage.damperPosMinVoltage.currentVal.toInt()
        damperPosMaxVoltage = analogMinMaxVoltage.damperPosMaxVoltage.currentVal.toInt()
        compressorMinVoltage = analogMinMaxVoltage.compressorMinVoltage.currentVal.toInt()
        compressorMaxVoltage = analogMinMaxVoltage.compressorMaxVoltage.currentVal.toInt()
    }
}

/**
 * Configures the relay
 */
fun configRelay(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    state.relay1Config = ConfigState(
        config.cmConfiguration.relay1Enabled.enabled,
        config.cmConfiguration.relay1Association.associationVal
    )
    state.relay2Config = ConfigState(
        config.cmConfiguration.relay2Enabled.enabled,
        config.cmConfiguration.relay2Association.associationVal
    )
    state.relay3Config = ConfigState(
        config.cmConfiguration.relay3Enabled.enabled,
        config.cmConfiguration.relay3Association.associationVal
    )
    state.relay4Config = ConfigState(
        config.cmConfiguration.relay4Enabled.enabled,
        config.cmConfiguration.relay4Association.associationVal
    )
    state.relay5Config = ConfigState(
        config.cmConfiguration.relay5Enabled.enabled,
        config.cmConfiguration.relay5Association.associationVal
    )
    state.relay6Config = ConfigState(
        config.cmConfiguration.relay6Enabled.enabled,
        config.cmConfiguration.relay6Association.associationVal
    )
    state.relay7Config = ConfigState(
        config.cmConfiguration.relay7Enabled.enabled,
        config.cmConfiguration.relay7Association.associationVal
    )
    state.relay8Config = ConfigState(
        config.cmConfiguration.relay8Enabled.enabled,
        config.cmConfiguration.relay8Association.associationVal
    )
}

/**
 * Configures the sensor address
 */
fun configConnectSensorAddress(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    state.connectSensorAddress0 = SensorState(config.connectConfiguration.address0Enabled.enabled)
    state.connectSensorAddress1 = SensorState(config.connectConfiguration.address1Enabled.enabled)
    state.connectSensorAddress2 = SensorState(config.connectConfiguration.address2Enabled.enabled)
    state.connectSensorAddress3 = SensorState(config.connectConfiguration.address3Enabled.enabled)
    state.connectSensorBusPressureEnable = config.connectConfiguration.sensorBus0PressureEnabled.enabled

    state.connectSensorAddress0.apply {
        temperatureAssociation =
            config.connectConfiguration.address0SensorAssociation.temperatureAssociation.associationVal
        humidityAssociation =
            config.connectConfiguration.address0SensorAssociation.humidityAssociation.associationVal
        co2Association =
            config.connectConfiguration.address0SensorAssociation.co2Association.associationVal
        occupancyAssociation =
            config.connectConfiguration.address0SensorAssociation.occupancyAssociation.associationVal
        pressureAssociation =
            config.connectConfiguration.address0SensorAssociation.pressureAssociation?.associationVal!!
    }

    state.connectSensorAddress1.apply {
        temperatureAssociation =
            config.connectConfiguration.address1SensorAssociation.temperatureAssociation.associationVal
        humidityAssociation =
            config.connectConfiguration.address1SensorAssociation.humidityAssociation.associationVal
        co2Association =
            config.connectConfiguration.address1SensorAssociation.co2Association.associationVal
        occupancyAssociation =
            config.connectConfiguration.address1SensorAssociation.occupancyAssociation.associationVal
    }

    state.connectSensorAddress2.apply {
        temperatureAssociation =
            config.connectConfiguration.address2SensorAssociation.temperatureAssociation.associationVal
        humidityAssociation =
            config.connectConfiguration.address2SensorAssociation.humidityAssociation.associationVal
        co2Association =
            config.connectConfiguration.address2SensorAssociation.co2Association.associationVal
        occupancyAssociation =
            config.connectConfiguration.address2SensorAssociation.occupancyAssociation.associationVal
    }

    state.connectSensorAddress3.apply {
        temperatureAssociation =
            config.connectConfiguration.address3SensorAssociation.temperatureAssociation.associationVal
        humidityAssociation =
            config.connectConfiguration.address3SensorAssociation.humidityAssociation.associationVal
        co2Association =
            config.connectConfiguration.address3SensorAssociation.co2Association.associationVal
        occupancyAssociation =
            config.connectConfiguration.address3SensorAssociation.occupancyAssociation.associationVal
    }

}

/**
 * Configures the universal in
 */
fun configConnectUniversalIn(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState){
    state.connectUniversalIn1Config = ConfigState(
        config.connectConfiguration.universal1InEnabled.enabled,
        config.connectConfiguration.universal1InAssociation.associationVal
    )
    state.connectUniversalIn2Config = ConfigState(
        config.connectConfiguration.universal2InEnabled.enabled,
        config.connectConfiguration.universal2InAssociation.associationVal
    )
    state.connectUniversalIn3Config = ConfigState(
        config.connectConfiguration.universal3InEnabled.enabled,
        config.connectConfiguration.universal3InAssociation.associationVal
    )
    state.connectUniversalIn4Config = ConfigState(
        config.connectConfiguration.universal4InEnabled.enabled,
        config.connectConfiguration.universal4InAssociation.associationVal
    )
    state.connectUniversalIn5Config = ConfigState(
        config.connectConfiguration.universal5InEnabled.enabled,
        config.connectConfiguration.universal5InAssociation.associationVal
    )
    state.connectUniversalIn6Config = ConfigState(
        config.connectConfiguration.universal6InEnabled.enabled,
        config.connectConfiguration.universal6InAssociation.associationVal
    )
    state.connectUniversalIn7Config = ConfigState(
        config.connectConfiguration.universal7InEnabled.enabled,
        config.connectConfiguration.universal7InAssociation.associationVal
    )
    state.connectUniversalIn8Config = ConfigState(
        config.connectConfiguration.universal8InEnabled.enabled,
        config.connectConfiguration.universal8InAssociation.associationVal
    )
}

/**
 * Configures the analog out
 */
fun configConnectAnalogOut(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    state.connectAnalogOut1Enabled = config.connectConfiguration.analogOut1Enabled.enabled
    state.connectAnalogOut2Enabled = config.connectConfiguration.analogOut2Enabled.enabled
    state.connectAnalogOut3Enabled = config.connectConfiguration.analogOut3Enabled.enabled
    state.connectAnalogOut4Enabled = config.connectConfiguration.analogOut4Enabled.enabled
    state.connectAnalogOut1Association = config.connectConfiguration.analogOut1Association.associationVal
    state.connectAnalogOut2Association = config.connectConfiguration.analogOut2Association.associationVal
    state.connectAnalogOut3Association = config.connectConfiguration.analogOut3Association.associationVal
    state.connectAnalogOut4Association = config.connectConfiguration.analogOut4Association.associationVal
    updateConnectAnalogOutDynamicConfig(config, state)
}

private fun updateConnectAnalogOutDynamicConfig(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {

    state.connectDamperConfig.apply {
        damperControlAssociation = config.connectConfiguration.damperControlAssociation.associationVal
        co2Target = config.connectConfiguration.co2Target.currentVal
        co2Threshold = config.connectConfiguration.co2Threshold.currentVal
        openingRate = config.connectConfiguration.damperOpeningRate.currentVal
    }
    updateAnalogOutMinMax(state.connectAnalogOut1MinMax, config.connectConfiguration.analog1MinMaxVoltage)
    updateAnalogOutMinMax(state.connectAnalogOut2MinMax, config.connectConfiguration.analog2MinMaxVoltage)
    updateAnalogOutMinMax(state.connectAnalogOut3MinMax, config.connectConfiguration.analog3MinMaxVoltage)
    updateAnalogOutMinMax(state.connectAnalogOut4MinMax, config.connectConfiguration.analog4MinMaxVoltage)
}

/**
 * Configures the relay
 */
fun configConnectRelay(config: AdvancedHybridAhuConfig, state: AdvancedHybridAhuState) {
    state.connectRelay1Config = ConfigState(
        config.connectConfiguration.relay1Enabled.enabled,
        config.connectConfiguration.relay1Association.associationVal
    )
    state.connectRelay2Config = ConfigState(
        config.connectConfiguration.relay2Enabled.enabled,
        config.connectConfiguration.relay2Association.associationVal
    )
    state.connectRelay3Config = ConfigState(
        config.connectConfiguration.relay3Enabled.enabled,
        config.connectConfiguration.relay3Association.associationVal
    )
    state.connectRelay4Config = ConfigState(
        config.connectConfiguration.relay4Enabled.enabled,
        config.connectConfiguration.relay4Association.associationVal
    )
    state.connectRelay5Config = ConfigState(
        config.connectConfiguration.relay5Enabled.enabled,
        config.connectConfiguration.relay5Association.associationVal
    )
    state.connectRelay6Config = ConfigState(
        config.connectConfiguration.relay6Enabled.enabled,
        config.connectConfiguration.relay6Association.associationVal
    )
    state.connectRelay7Config = ConfigState(
        config.connectConfiguration.relay7Enabled.enabled,
        config.connectConfiguration.relay7Association.associationVal
    )
    state.connectRelay8Config = ConfigState(
        config.connectConfiguration.relay8Enabled.enabled,
        config.connectConfiguration.relay8Association.associationVal
    )
}