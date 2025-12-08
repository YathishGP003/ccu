package a75f.io.logic.bo.building.statprofiles

import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.StringValueConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.StandAloneEquip
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.SensorTempHumidityAssociationConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Author: Manjunath Kundaragi
 * Created on: 13-10-2025
 */
abstract class StatProfileConfiguration(
    nodeAddress: Int,
    nodeType: String,
    priority: Int,
    roomRef: String,
    floorRef: String,
    profileType: ProfileType,
    val model: SeventyFiveFProfileDirective
) : ProfileConfiguration(
    nodeAddress = nodeAddress,
    nodeType = nodeType,
    priority = priority,
    roomRef = roomRef,
    floorRef = floorRef,
    profileType = profileType.name
) {

    lateinit var temperatureOffset: ValueConfig
    lateinit var autoForceOccupied: EnableConfig
    lateinit var autoAway: EnableConfig
    lateinit var prePurge: EnableConfig

    lateinit var address0Enabled: EnableConfig
    lateinit var sensorBusPressureEnable: EnableConfig
    lateinit var address1Enabled: EnableConfig
    lateinit var address2Enabled: EnableConfig

    lateinit var address0SensorAssociation: SensorTempHumidityAssociationConfig
    lateinit var pressureAddress0SensorAssociation: AssociationConfig
    lateinit var address1SensorAssociation: SensorTempHumidityAssociationConfig
    lateinit var address2SensorAssociation: SensorTempHumidityAssociationConfig

    lateinit var relay1Enabled: EnableConfig
    lateinit var relay2Enabled: EnableConfig
    lateinit var relay3Enabled: EnableConfig
    lateinit var relay4Enabled: EnableConfig
    lateinit var relay5Enabled: EnableConfig
    lateinit var relay6Enabled: EnableConfig
    lateinit var relay7Enabled: EnableConfig
    lateinit var relay8Enabled: EnableConfig

    lateinit var relay1Association: AssociationConfig
    lateinit var relay2Association: AssociationConfig
    lateinit var relay3Association: AssociationConfig
    lateinit var relay4Association: AssociationConfig
    lateinit var relay5Association: AssociationConfig
    lateinit var relay6Association: AssociationConfig
    lateinit var relay7Association: AssociationConfig
    lateinit var relay8Association: AssociationConfig

    lateinit var analogOut1Enabled: EnableConfig
    lateinit var analogOut2Enabled: EnableConfig
    lateinit var analogOut3Enabled: EnableConfig
    lateinit var analogOut4Enabled: EnableConfig

    lateinit var analogOut1Association: AssociationConfig
    lateinit var analogOut2Association: AssociationConfig
    lateinit var analogOut3Association: AssociationConfig
    lateinit var analogOut4Association: AssociationConfig


    lateinit var analogIn1Enabled: EnableConfig
    lateinit var analogIn2Enabled: EnableConfig

    lateinit var analogIn1Association: AssociationConfig
    lateinit var analogIn2Association: AssociationConfig

    lateinit var thermistor1Enabled: EnableConfig
    lateinit var thermistor2Enabled: EnableConfig

    lateinit var thermistor1Association: AssociationConfig
    lateinit var thermistor2Association: AssociationConfig

    lateinit var universalOut1: EnableConfig
    lateinit var universalOut2: EnableConfig

    lateinit var universalOut1Association: AssociationConfig
    lateinit var universalOut2Association: AssociationConfig

    lateinit var universal1InEnabled: EnableConfig
    lateinit var universal2InEnabled: EnableConfig
    lateinit var universal3InEnabled: EnableConfig
    lateinit var universal4InEnabled: EnableConfig
    lateinit var universal5InEnabled: EnableConfig
    lateinit var universal6InEnabled: EnableConfig
    lateinit var universal7InEnabled: EnableConfig
    lateinit var universal8InEnabled: EnableConfig
    lateinit var universal1InAssociation: AssociationConfig
    lateinit var universal2InAssociation: AssociationConfig
    lateinit var universal3InAssociation: AssociationConfig
    lateinit var universal4InAssociation: AssociationConfig
    lateinit var universal5InAssociation: AssociationConfig
    lateinit var universal6InAssociation: AssociationConfig
    lateinit var universal7InAssociation: AssociationConfig
    lateinit var universal8InAssociation: AssociationConfig

    lateinit var outsideDamperMinOpenDuringRecirc: ValueConfig
    lateinit var outsideDamperMinOpenDuringConditioning: ValueConfig
    lateinit var outsideDamperMinOpenDuringFanLow: ValueConfig
    lateinit var outsideDamperMinOpenDuringFanMedium: ValueConfig
    lateinit var outsideDamperMinOpenDuringFanHigh: ValueConfig

    lateinit var exhaustFanStage1Threshold: ValueConfig
    lateinit var exhaustFanStage2Threshold: ValueConfig
    lateinit var exhaustFanHysteresis: ValueConfig

    lateinit var prePurgeOutsideDamperOpen: ValueConfig

    lateinit var zoneCO2DamperOpeningRate: ValueConfig
    lateinit var zoneCO2Threshold: ValueConfig
    lateinit var zoneCO2Target: ValueConfig
    lateinit var zonePM2p5Target: ValueConfig
    lateinit var zonePM2p5Threshold: ValueConfig
    lateinit var zonePM10Target: ValueConfig

    lateinit var displayHumidity: EnableConfig
    lateinit var displayCO2: EnableConfig
    lateinit var displayPM2p5: EnableConfig

    lateinit var disableTouch: EnableConfig
    lateinit var backLight: EnableConfig
    lateinit var enableBrightness: EnableConfig
    lateinit var enableOutsideAirOptimization: EnableConfig

    lateinit var installerPinEnable: EnableConfig
    lateinit var conditioningModePinEnable: EnableConfig

    lateinit var installerPassword: StringValueConfig
    lateinit var conditioningModePassword: StringValueConfig

    lateinit var desiredTemp: EnableConfig
    lateinit var spaceTemp: EnableConfig


    abstract fun isCoolingAvailable(): Boolean
    abstract fun isHeatingAvailable(): Boolean

    abstract fun getRelayMap(): Map<String, Boolean>

    abstract fun getAnalogMap(): Map<String, Pair<Boolean, String>>

    override fun getEnableConfigs(): List<EnableConfig> {
        return listOf(
            autoAway,
            autoForceOccupied,
            prePurge,
            address0Enabled,
            sensorBusPressureEnable,
            address1Enabled,
            address2Enabled,

            universal1InEnabled,
            universal2InEnabled,
            universal3InEnabled,
            universal4InEnabled,
            universal5InEnabled,
            universal6InEnabled,
            universal7InEnabled,
            universal8InEnabled,

            relay1Enabled,
            relay2Enabled,
            relay3Enabled,
            relay4Enabled,
            relay5Enabled,
            relay6Enabled,
            relay7Enabled,
            relay8Enabled,

            analogOut1Enabled,
            analogOut2Enabled,
            analogOut3Enabled,
            analogOut4Enabled,

            analogIn1Enabled,
            analogIn2Enabled,
            thermistor1Enabled,
            thermistor2Enabled,

            universalOut1,
            universalOut2,

            displayHumidity,
            displayCO2,
            displayPM2p5,
            disableTouch,
            enableBrightness,
            backLight,
            enableOutsideAirOptimization,
            installerPinEnable,
            conditioningModePinEnable,
            desiredTemp,
            spaceTemp
        )
    }

    override fun getAssociationConfigs(): List<AssociationConfig> {
        return listOf(
            address0SensorAssociation.temperatureAssociation,
            address0SensorAssociation.humidityAssociation,
            pressureAddress0SensorAssociation,
            address1SensorAssociation.temperatureAssociation,
            address1SensorAssociation.humidityAssociation,
            address2SensorAssociation.temperatureAssociation,
            address2SensorAssociation.humidityAssociation,

            universal1InAssociation,
            universal2InAssociation,
            universal3InAssociation,
            universal4InAssociation,
            universal5InAssociation,
            universal6InAssociation,
            universal7InAssociation,
            universal8InAssociation,

            relay1Association,
            relay2Association,
            relay3Association,
            relay4Association,
            relay5Association,
            relay6Association,
            relay7Association,
            relay8Association,

            analogOut1Association,
            analogOut2Association,
            analogOut3Association,
            analogOut4Association,

            analogIn1Association,
            analogIn2Association,

            thermistor1Association,
            thermistor2Association,

            universalOut1Association,
            universalOut2Association,
        )
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return listOf(
            temperatureOffset,

            outsideDamperMinOpenDuringRecirc,
            outsideDamperMinOpenDuringConditioning,
            outsideDamperMinOpenDuringFanLow,
            outsideDamperMinOpenDuringFanMedium,
            outsideDamperMinOpenDuringFanHigh,

            exhaustFanStage1Threshold,
            exhaustFanStage2Threshold,
            exhaustFanHysteresis,

            prePurgeOutsideDamperOpen,
            zoneCO2DamperOpeningRate,
            zoneCO2Threshold,
            zoneCO2Target,
            zonePM2p5Target,
            zonePM2p5Threshold,
            zonePM10Target

        )
    }

    override fun getStringConfigs(): List<StringValueConfig> {
        return listOf(
            installerPassword, conditioningModePassword
        )
    }

    open fun getDefaultConfiguration(): StatProfileConfiguration {
        isDefault = true
        getAddressEnableConfig()
        getAddressAssociations()
        getUniversalInConfig()
        getRelayEnableConfig()
        getRelayAssociationConfig()
        getUniversalOutEnableConfig()
        getUniversalOutAssociationConfig()
        getAnalogOutEnableConfig()
        getAnalogOutAssociationConfig()
        getThermistorEnabledConfig()
        getThermistorAssociationConfig()
        getAnalogInEnableConfig()
        getAnalogInAssociationConfig()
        getEconomizerConfigs()
        getGenericZoneConfigs()
        return this
    }

    private fun getAddressEnableConfig() {
        address0Enabled = getDefaultEnableConfig(DomainName.sensorBusAddress0Enable, model)
        sensorBusPressureEnable = getDefaultEnableConfig(DomainName.sensorBusPressureEnable, model)
        address1Enabled = getDefaultEnableConfig(DomainName.sensorBusAddress1Enable, model)
        address2Enabled = getDefaultEnableConfig(DomainName.sensorBusAddress2Enable, model)
    }

    private fun getAddressAssociations() {
        address0SensorAssociation = SensorTempHumidityAssociationConfig(
            temperatureAssociation = getDefaultAssociationConfig(DomainName.temperatureSensorBusAdd0, model),
            humidityAssociation = getDefaultAssociationConfig(DomainName.humiditySensorBusAdd0, model)
        )
        pressureAddress0SensorAssociation = getDefaultAssociationConfig(DomainName.pressureSensorBusAdd0, model)
        address1SensorAssociation = SensorTempHumidityAssociationConfig(
            temperatureAssociation = getDefaultAssociationConfig(DomainName.temperatureSensorBusAdd1, model),
            humidityAssociation = getDefaultAssociationConfig(DomainName.humiditySensorBusAdd1, model)
        )
        address2SensorAssociation = SensorTempHumidityAssociationConfig(
            temperatureAssociation = getDefaultAssociationConfig(DomainName.temperatureSensorBusAdd2, model),
            humidityAssociation = getDefaultAssociationConfig(DomainName.humiditySensorBusAdd2, model)
        )
    }

    private fun getUniversalInConfig() {
        universal1InEnabled = getDefaultEnableConfig(DomainName.universalIn1Enable, model)
        universal2InEnabled = getDefaultEnableConfig(DomainName.universalIn2Enable, model)
        universal3InEnabled = getDefaultEnableConfig(DomainName.universalIn3Enable, model)
        universal4InEnabled = getDefaultEnableConfig(DomainName.universalIn4Enable, model)
        universal5InEnabled = getDefaultEnableConfig(DomainName.universalIn5Enable, model)
        universal6InEnabled = getDefaultEnableConfig(DomainName.universalIn6Enable, model)
        universal7InEnabled = getDefaultEnableConfig(DomainName.universalIn7Enable, model)
        universal8InEnabled = getDefaultEnableConfig(DomainName.universalIn8Enable, model)

        universal1InAssociation =
            getDefaultAssociationConfig(DomainName.universalIn1Association, model)
        universal2InAssociation =
            getDefaultAssociationConfig(DomainName.universalIn2Association, model)
        universal3InAssociation =
            getDefaultAssociationConfig(DomainName.universalIn3Association, model)
        universal4InAssociation =
            getDefaultAssociationConfig(DomainName.universalIn4Association, model)
        universal5InAssociation =
            getDefaultAssociationConfig(DomainName.universalIn5Association, model)
        universal6InAssociation =
            getDefaultAssociationConfig(DomainName.universalIn6Association, model)
        universal7InAssociation =
            getDefaultAssociationConfig(DomainName.universalIn7Association, model)
        universal8InAssociation =
            getDefaultAssociationConfig(DomainName.universalIn8Association, model)
    }

    private fun getAnalogOutEnableConfig() {
        analogOut1Enabled = getDefaultEnableConfig(DomainName.analog1OutputEnable, model)
        analogOut2Enabled = getDefaultEnableConfig(DomainName.analog2OutputEnable, model)
        analogOut3Enabled = getDefaultEnableConfig(DomainName.analog3OutputEnable, model)
        analogOut4Enabled = getDefaultEnableConfig(DomainName.analog4OutputEnable, model)
    }

    private fun getAnalogOutAssociationConfig() {
        analogOut1Association = getDefaultAssociationConfig(DomainName.analog1OutputAssociation, model)
        analogOut2Association = getDefaultAssociationConfig(DomainName.analog2OutputAssociation, model)
        analogOut3Association = getDefaultAssociationConfig(DomainName.analog3OutputAssociation, model)
        analogOut4Association = getDefaultAssociationConfig(DomainName.analog4OutputAssociation, model)
    }

    private fun getThermistorEnabledConfig() {
        thermistor1Enabled = getDefaultEnableConfig(DomainName.thermistor1InputEnable, model)
        thermistor2Enabled = getDefaultEnableConfig(DomainName.thermistor2InputEnable, model)
    }

    private fun getThermistorAssociationConfig() {
        thermistor1Association = getDefaultAssociationConfig(DomainName.thermistor1InputAssociation, model)
        thermistor2Association = getDefaultAssociationConfig(DomainName.thermistor2InputAssociation, model)
    }

    private fun getAnalogInEnableConfig() {
        analogIn1Enabled = getDefaultEnableConfig(DomainName.analog1InputEnable, model)
        analogIn2Enabled = getDefaultEnableConfig(DomainName.analog2InputEnable, model)
    }
    private fun getAnalogInAssociationConfig() {
        analogIn1Association = getDefaultAssociationConfig(DomainName.analog1InputAssociation, model)
        analogIn2Association = getDefaultAssociationConfig(DomainName.analog2InputAssociation, model)
    }

    private fun getUniversalOutEnableConfig() {
        universalOut1 = getDefaultEnableConfig(DomainName.universal1OutputEnable, model)
        universalOut2 = getDefaultEnableConfig(DomainName.universal2OutputEnable, model)
    }
    private fun getUniversalOutAssociationConfig() {
        universalOut1Association = getDefaultAssociationConfig(DomainName.universal1OutputAssociation, model)
        universalOut2Association = getDefaultAssociationConfig(DomainName.universal2OutputAssociation, model)
    }

    private fun getRelayEnableConfig() {
        relay1Enabled = getDefaultEnableConfig(DomainName.relay1OutputEnable, model)
        relay2Enabled = getDefaultEnableConfig(DomainName.relay2OutputEnable, model)
        relay3Enabled = getDefaultEnableConfig(DomainName.relay3OutputEnable, model)
        relay4Enabled = getDefaultEnableConfig(DomainName.relay4OutputEnable, model)
        relay5Enabled = getDefaultEnableConfig(DomainName.relay5OutputEnable, model)
        relay6Enabled = getDefaultEnableConfig(DomainName.relay6OutputEnable, model)
        relay7Enabled = getDefaultEnableConfig(DomainName.relay7OutputEnable, model)
        relay8Enabled = getDefaultEnableConfig(DomainName.relay8OutputEnable, model)
    }

    /**
     * Get the default enable config for the domain name
     */
    private fun getRelayAssociationConfig() {
        relay1Association = getDefaultAssociationConfig(DomainName.relay1OutputAssociation, model)
        relay2Association = getDefaultAssociationConfig(DomainName.relay2OutputAssociation, model)
        relay3Association = getDefaultAssociationConfig(DomainName.relay3OutputAssociation, model)
        relay4Association = getDefaultAssociationConfig(DomainName.relay4OutputAssociation, model)
        relay5Association = getDefaultAssociationConfig(DomainName.relay5OutputAssociation, model)
        relay6Association = getDefaultAssociationConfig(DomainName.relay6OutputAssociation, model)
        relay7Association = getDefaultAssociationConfig(DomainName.relay7OutputAssociation, model)
        relay8Association = getDefaultAssociationConfig(DomainName.relay8OutputAssociation, model)
    }

    private fun getEconomizerConfigs() {
        outsideDamperMinOpenDuringRecirc = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringRecirculation, model)
        outsideDamperMinOpenDuringConditioning = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringConditioning, model)
        outsideDamperMinOpenDuringFanLow = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringFanLow, model)
        outsideDamperMinOpenDuringFanMedium = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringFanMedium, model)
        outsideDamperMinOpenDuringFanHigh = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringFanHigh, model)
        exhaustFanStage1Threshold = getDefaultValConfig(DomainName.exhaustFanStage1Threshold, model)
        exhaustFanStage2Threshold = getDefaultValConfig(DomainName.exhaustFanStage2Threshold, model)
        exhaustFanHysteresis = getDefaultValConfig(DomainName.exhaustFanHysteresis, model)
    }

    private fun getGenericZoneConfigs() {
        temperatureOffset = getDefaultValConfig(DomainName.temperatureOffset, model)
        autoForceOccupied = getDefaultEnableConfig(DomainName.autoForceOccupied, model)
        autoAway = getDefaultEnableConfig(DomainName.autoAway, model)
        prePurge = getDefaultEnableConfig(DomainName.prePurgeEnable, model)

        prePurgeOutsideDamperOpen = getDefaultValConfig(DomainName.prePurgeOutsideDamperOpen, model)
        zoneCO2DamperOpeningRate = getDefaultValConfig(DomainName.co2DamperOpeningRate, model)
        zoneCO2Threshold = getDefaultValConfig(DomainName.co2Threshold, model)
        zoneCO2Target = getDefaultValConfig(DomainName.co2Target, model)
        zonePM2p5Target = getDefaultValConfig(DomainName.pm25Target, model)
        zonePM2p5Threshold = getDefaultValConfig(DomainName.pm25Threshold, model)
        zonePM10Target = getDefaultValConfig(DomainName.pm10Target, model)

        displayHumidity = getDefaultEnableConfig(DomainName.enableHumidityDisplay, model)
        displayCO2 = getDefaultEnableConfig(DomainName.enableCo2Display, model)
        displayPM2p5 = getDefaultEnableConfig(DomainName.enablePm25Display, model)

        disableTouch = getDefaultEnableConfig(DomainName.disableTouch, model)
        backLight = getDefaultEnableConfig(DomainName.enableBacklight, model)
        enableBrightness = getDefaultEnableConfig(DomainName.enableBrightness, model)

        enableOutsideAirOptimization = getDefaultEnableConfig(DomainName.enableOutsideAirOptimization, model)

        // Pin enable configs
        installerPinEnable = getDefaultEnableConfig(DomainName.enableInstallerAccess, model)
        conditioningModePinEnable = getDefaultEnableConfig(DomainName.enableConditioningModeFanAccess, model)

        desiredTemp = getDefaultEnableConfig(DomainName.enableDesiredTempDisplay, model)
        spaceTemp = getDefaultEnableConfig(DomainName.enableSpaceTempDisplay, model)

        installerPassword = getDefaultStringConfig(DomainName.pinLockInstallerAccess, model)
        conditioningModePassword = getDefaultStringConfig(DomainName.pinLockConditioningModeFanAccess, model)
    }

    fun getActiveEnableConfigs(equip: StandAloneEquip) {
        address0Enabled.enabled = equip.sensorBusAddress0Enable.readDefaultVal() > 0
        sensorBusPressureEnable.enabled = equip.sensorBusPressureEnable.readDefaultVal() > 0
        address1Enabled.enabled = equip.sensorBusAddress1Enable.readDefaultVal() > 0
        address2Enabled.enabled = equip.sensorBusAddress2Enable.readDefaultVal() > 0

        relay1Enabled.enabled = equip.relay1OutputEnable.readDefaultVal() > 0.0
        relay2Enabled.enabled = equip.relay2OutputEnable.readDefaultVal() > 0.0
        relay3Enabled.enabled = equip.relay3OutputEnable.readDefaultVal() > 0.0
        relay4Enabled.enabled = equip.relay4OutputEnable.readDefaultVal() > 0.0
        relay5Enabled.enabled = equip.relay5OutputEnable.readDefaultVal() > 0.0
        relay6Enabled.enabled = equip.relay6OutputEnable.readDefaultVal() > 0.0
        relay7Enabled.enabled = equip.relay7OutputEnable.readDefaultVal() > 0.0
        relay8Enabled.enabled = equip.relay8OutputEnable.readDefaultVal() > 0.0

        analogOut1Enabled.enabled = equip.analog1OutputEnable.readDefaultVal() > 0.0
        analogOut2Enabled.enabled = equip.analog2OutputEnable.readDefaultVal() > 0.0
        analogOut3Enabled.enabled = equip.analog3OutputEnable.readDefaultVal() > 0.0
        analogOut4Enabled.enabled = equip.analog4OutputEnable.readDefaultVal() > 0.0

        analogIn1Enabled.enabled = equip.analog1InputEnable.readDefaultVal() > 0.0
        analogIn2Enabled.enabled = equip.analog2InputEnable.readDefaultVal() > 0.0

        thermistor1Enabled.enabled = equip.thermistor1InputEnable.readDefaultVal() > 0.0
        thermistor2Enabled.enabled = equip.thermistor2InputEnable.readDefaultVal() > 0.0

        universalOut1.enabled = equip.universalOut1Enable.readDefaultVal() > 0.0
        universalOut2.enabled = equip.universalOut2Enable.readDefaultVal() > 0.0

        universal1InEnabled.enabled = equip.universalIn1Enable.readDefaultVal() > 0.0
        universal2InEnabled.enabled = equip.universalIn2Enable.readDefaultVal() > 0.0
        universal3InEnabled.enabled = equip.universalIn3Enable.readDefaultVal() > 0.0
        universal4InEnabled.enabled = equip.universalIn4Enable.readDefaultVal() > 0.0
        universal5InEnabled.enabled = equip.universalIn5Enable.readDefaultVal() > 0.0
        universal6InEnabled.enabled = equip.universalIn6Enable.readDefaultVal() > 0.0
        universal7InEnabled.enabled = equip.universalIn7Enable.readDefaultVal() > 0.0
        universal8InEnabled.enabled = equip.universalIn8Enable.readDefaultVal() > 0.0
    }

    fun getActiveAssociationConfigs(equip: StandAloneEquip) {
        getAddressAssociationActiveConfigs(equip)
        mapOf(
            relay1Enabled to Pair(relay1Association, equip.relay1OutputAssociation),
            relay2Enabled to Pair(relay2Association, equip.relay2OutputAssociation),
            relay3Enabled to Pair(relay3Association, equip.relay3OutputAssociation),
            relay4Enabled to Pair(relay4Association, equip.relay4OutputAssociation),
            relay5Enabled to Pair(relay5Association, equip.relay5OutputAssociation),
            relay6Enabled to Pair(relay6Association, equip.relay6OutputAssociation),
            relay7Enabled to Pair(relay7Association, equip.relay7OutputAssociation),
            relay8Enabled to Pair(relay8Association, equip.relay8OutputAssociation),
            analogOut1Enabled to Pair(analogOut1Association, equip.analog1OutputAssociation),
            analogOut2Enabled to Pair(analogOut2Association, equip.analog2OutputAssociation),
            analogOut3Enabled to Pair(analogOut3Association, equip.analog3OutputAssociation),
            analogOut4Enabled to Pair(analogOut4Association, equip.analog4OutputAssociation),
            universal1InEnabled to Pair(universal1InAssociation, equip.universalIn1Association),
            universal2InEnabled to Pair(universal2InAssociation, equip.universalIn2Association),
            universal3InEnabled to Pair(universal3InAssociation, equip.universalIn3Association),
            universal4InEnabled to Pair(universal4InAssociation, equip.universalIn4Association),
            universal5InEnabled to Pair(universal5InAssociation, equip.universalIn5Association),
            universal6InEnabled to Pair(universal6InAssociation, equip.universalIn6Association),
            universal7InEnabled to Pair(universal7InAssociation, equip.universalIn7Association),
            universal8InEnabled to Pair(universal8InAssociation, equip.universalIn8Association),
            analogIn1Enabled to Pair(analogIn1Association, equip.analog1InputAssociation),
            analogIn2Enabled to Pair(analogIn2Association, equip.analog2InputAssociation),
            thermistor1Enabled to Pair(thermistor1Association, equip.thermistor1InputAssociation),
            thermistor2Enabled to Pair(thermistor2Association, equip.thermistor2InputAssociation),
            universalOut1 to Pair(universalOut1Association, equip.universalOut1Association),
            universalOut2 to Pair(universalOut2Association, equip.universalOut2Association)
        ).forEach { (enabled, association) ->
            if (enabled.enabled)
                association.first.associationVal = association.second.readDefaultVal().toInt()
        }
    }

    private fun getAddressAssociationActiveConfigs(equip: StandAloneEquip) {
        if (address0Enabled.enabled) {
            address0SensorAssociation.temperatureAssociation.associationVal = equip.temperatureSensorBusAdd0.readDefaultVal().toInt()
            address0SensorAssociation.humidityAssociation.associationVal = equip.humiditySensorBusAdd0.readDefaultVal().toInt()
        }
        
        if (sensorBusPressureEnable.enabled) {
            pressureAddress0SensorAssociation.associationVal = equip.pressureSensorBusAdd0.readDefaultVal().toInt()
        }
        
        if (address1Enabled.enabled) {
            address1SensorAssociation.temperatureAssociation.associationVal = equip.temperatureSensorBusAdd1.readDefaultVal().toInt()
            address1SensorAssociation.humidityAssociation.associationVal = equip.humiditySensorBusAdd1.readDefaultVal().toInt()
        }

        if (address2Enabled.enabled) {
            address2SensorAssociation.temperatureAssociation.associationVal = equip.temperatureSensorBusAdd2.readDefaultVal().toInt()
            address2SensorAssociation.humidityAssociation.associationVal = equip.humiditySensorBusAdd2.readDefaultVal().toInt()
        }
    }

    fun getGenericZoneConfigs(equip: StandAloneEquip) {
        temperatureOffset.currentVal = equip.temperatureOffset.readDefaultVal()
        autoForceOccupied.enabled = equip.autoForceOccupied.readDefaultVal() > 0.0
        autoAway.enabled = equip.autoAway.readDefaultVal() > 0.0
        prePurge.enabled = equip.prePurgeEnable.readDefaultVal() > 0.0
        zoneCO2Threshold.currentVal = equip.co2Threshold.readDefaultVal()
        zoneCO2Target.currentVal = equip.co2Target.readDefaultVal()
        zonePM2p5Target.currentVal = equip.pm25Target.readDefaultVal()
        displayHumidity.enabled = equip.enableHumidityDisplay.readDefaultVal() > 0.0
        displayCO2.enabled = equip.enableCo2Display.readDefaultVal() > 0.0
        displayPM2p5.enabled = equip.enablePm25Display.readDefaultVal() > 0.0
        disableTouch.enabled = equip.disableTouch.readDefaultVal() > 0.0
        enableBrightness.enabled = equip.enableBrightness.readDefaultVal() > 0.0
        backLight.enabled = equip.enableBacklight.readDefaultVal() > 0.0
        zoneCO2DamperOpeningRate.currentVal = equip.co2DamperOpeningRate.readDefaultVal()
        enableOutsideAirOptimization.enabled =
            equip.enableOutsideAirOptimization.readDefaultVal() > 0.0
        installerPinEnable.enabled = equip.installerPinEnable.readDefaultVal() > 0.0
        conditioningModePinEnable.enabled =
            equip.enableConditioningModeFanAccess.readDefaultVal() > 0.0
        desiredTemp.enabled = equip.enableDesiredTempDisplay.readDefaultVal() > 0.0
        spaceTemp.enabled = equip.enableSpaceTempDisplay.readDefaultVal() > 0.0

        if (prePurge.enabled) prePurgeOutsideDamperOpen.currentVal =
            equip.prePurgeOutsideDamperOpen.readDefaultVal()
        installerPassword.currentVal = if (equip.pinLockInstallerAccess.pointExists()) {
            equip.pinLockInstallerAccess.readDefaultStrVal()
        } else {
            "0" // If the point does not exist, return an empty string
        }
        conditioningModePassword.currentVal =
            if (equip.pinLockConditioningModeFanAccess.pointExists()) {
                equip.pinLockConditioningModeFanAccess.readDefaultStrVal()
            } else {
                "0" // If the point does not exist, return an empty string
            }
    }

}
