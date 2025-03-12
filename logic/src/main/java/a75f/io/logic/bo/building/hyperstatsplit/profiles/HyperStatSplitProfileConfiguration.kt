package a75f.io.logic.bo.building.hyperstatsplit.profiles

import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

open class HyperStatSplitProfileConfiguration (nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String, profileType : ProfileType, val model : SeventyFiveFProfileDirective) :
    ProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType.name) {

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

    // In Kotlin, inheritance is not supported for Data Classes.
    // So, AnalogOutMinMaxVoltage will need to be implemented separately for each profile.

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

    lateinit var zoneVOCTarget: ValueConfig

    lateinit var zonePM2p5Target: ValueConfig

    lateinit var displayHumidity: EnableConfig
    lateinit var displayCO2: EnableConfig
    lateinit var displayPM2p5: EnableConfig

    lateinit var disableTouch: EnableConfig
    lateinit var enableBrightness: EnableConfig

    // This point did not exist in old HSS CPU profile. In DM framework, dependencies work a lot better if there
    // is a boolean config "Enable" point.
    lateinit var enableOutsideAirOptimization: EnableConfig

    /**
     * Get a list of domainNames of all base-configs
     * This need not have all base points.
     * Only configs which are configured via UI.
     *
     */
    override fun getEnableConfigs(): List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(autoAway)
            add(autoForceOccupied)
            add(prePurge)

            add(address0Enabled)
            add(sensorBusPressureEnable)
            add(address1Enabled)
            add(address2Enabled)

            add(universal1InEnabled)
            add(universal2InEnabled)
            add(universal3InEnabled)
            add(universal4InEnabled)
            add(universal5InEnabled)
            add(universal6InEnabled)
            add(universal7InEnabled)
            add(universal8InEnabled)

            add(relay1Enabled)
            add(relay2Enabled)
            add(relay3Enabled)
            add(relay4Enabled)
            add(relay5Enabled)
            add(relay6Enabled)
            add(relay7Enabled)
            add(relay8Enabled)

            add(analogOut1Enabled)
            add(analogOut2Enabled)
            add(analogOut3Enabled)
            add(analogOut4Enabled)

            add(displayHumidity)
            add(displayCO2)
            add(displayPM2p5)

            add(disableTouch)
            add(enableBrightness)

            add(enableOutsideAirOptimization)
        }
    }

    /**
     * Get a list of domainNames of all associations
     *
     */
    override fun getAssociationConfigs(): List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
            addAll(addSensorTempHumidityAssociation(address0SensorAssociation, this))
            add(pressureAddress0SensorAssociation)
            addAll(addSensorTempHumidityAssociation(address1SensorAssociation, this))
            addAll(addSensorTempHumidityAssociation(address2SensorAssociation, this))

            add(universal1InAssociation)
            add(universal2InAssociation)
            add(universal3InAssociation)
            add(universal4InAssociation)
            add(universal5InAssociation)
            add(universal6InAssociation)
            add(universal7InAssociation)
            add(universal8InAssociation)

            add(relay1Association)
            add(relay2Association)
            add(relay3Association)
            add(relay4Association)
            add(relay5Association)
            add(relay6Association)
            add(relay7Association)
            add(relay8Association)

            add(analogOut1Association)
            add(analogOut2Association)
            add(analogOut3Association)
            add(analogOut4Association)
        }
    }

    /**
     * Get a list of domainNames of all dependencies
     *
     */
    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(temperatureOffset)

            add(outsideDamperMinOpenDuringRecirc)
            add(outsideDamperMinOpenDuringConditioning)
            add(outsideDamperMinOpenDuringFanLow)
            add(outsideDamperMinOpenDuringFanMedium)
            add(outsideDamperMinOpenDuringFanHigh)

            add(exhaustFanStage1Threshold)
            add(exhaustFanStage2Threshold)
            add(exhaustFanHysteresis)

            add(prePurgeOutsideDamperOpen)
            add(zoneCO2DamperOpeningRate)
            add(zoneCO2Threshold)
            add(zoneCO2Target)

            add(zoneVOCTarget)

            add(zonePM2p5Target)
        }
    }

    /**
     * Add all sensor associations to the list
     */
    private fun addSensorTempHumidityAssociation(
        addressBus: SensorTempHumidityAssociationConfig, list: MutableList<AssociationConfig>
    ): MutableList<AssociationConfig> {
        return list.apply {
            add(addressBus.temperatureAssociation)
            add(addressBus.humidityAssociation)
        }
    }

    /**
     * Get a list of domainNames of all dependencies
     */
    open fun getDefaultConfiguration(): HyperStatSplitProfileConfiguration {
        isDefault = true
        getAddressEnableConfig()
        getAddressAssociations()
        getUniversalInConfig()
        getRelayEnableConfig()
        getRelayAssociationConfig()
        getAnalogOutEnableConfig()
        getAnalogOutAssociationConfig()
        getEconomizerConfigs()
        getGenericZoneConfigs()
        return this
    }

    /**
     * Get the default enable config for the domain name
     */
    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf<ValueConfig>()
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

    /**
     * Get the default enable config for the domain name
     */
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

    /**
     * Get the default enable config for the domain name
     */
    private fun getAnalogOutEnableConfig() {
        analogOut1Enabled = getDefaultEnableConfig(DomainName.analog1OutputEnable, model)
        analogOut2Enabled = getDefaultEnableConfig(DomainName.analog2OutputEnable, model)
        analogOut3Enabled = getDefaultEnableConfig(DomainName.analog3OutputEnable, model)
        analogOut4Enabled = getDefaultEnableConfig(DomainName.analog4OutputEnable, model)
    }

    /**
     * Get the default enable config for the domain name
     */
    private fun getAnalogOutAssociationConfig() {
        analogOut1Association =
            getDefaultAssociationConfig(DomainName.analog1OutputAssociation, model)
        analogOut2Association =
            getDefaultAssociationConfig(DomainName.analog2OutputAssociation, model)
        analogOut3Association =
            getDefaultAssociationConfig(DomainName.analog3OutputAssociation, model)
        analogOut4Association =
            getDefaultAssociationConfig(DomainName.analog4OutputAssociation, model)
    }

    /**
     * Get the default enable config for the domain name
     */
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

    /**
     * Get the default enable config for the domain name
     */
    private fun getAddressEnableConfig() {
        address0Enabled = getDefaultEnableConfig(DomainName.sensorBusAddress0Enable, model)
        sensorBusPressureEnable = getDefaultEnableConfig(DomainName.sensorBusPressureEnable, model)
        address1Enabled = getDefaultEnableConfig(DomainName.sensorBusAddress1Enable, model)
        address2Enabled = getDefaultEnableConfig(DomainName.sensorBusAddress2Enable, model)
    }

    /**
     * Get the default enable config for the domain name
     */
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

    /**
     * Get the default enable config for the domain name
     */
    private fun getGenericZoneConfigs() {
        temperatureOffset = getDefaultValConfig(DomainName.temperatureOffset, model)
        autoForceOccupied = getDefaultEnableConfig(DomainName.autoForceOccupied, model)
        autoAway = getDefaultEnableConfig(DomainName.autoAway, model)
        prePurge = getDefaultEnableConfig(DomainName.prePurgeEnable, model)

        prePurgeOutsideDamperOpen = getDefaultValConfig(DomainName.prePurgeOutsideDamperOpen, model)
        zoneCO2DamperOpeningRate = getDefaultValConfig(DomainName.co2DamperOpeningRate, model)
        zoneCO2Threshold = getDefaultValConfig(DomainName.co2Threshold, model)
        zoneCO2Target = getDefaultValConfig(DomainName.co2Target, model)

        zoneVOCTarget = getDefaultValConfig(DomainName.vocTarget, model)

        zonePM2p5Target = getDefaultValConfig(DomainName.pm25Target, model)

        displayHumidity = getDefaultEnableConfig(DomainName.enableHumidityDisplay, model)
        displayCO2 = getDefaultEnableConfig(DomainName.enableCO2Display, model)
        displayPM2p5 = getDefaultEnableConfig(DomainName.enablePm25Display, model)

        disableTouch = getDefaultEnableConfig(DomainName.disableTouch, model)
        enableBrightness = getDefaultEnableConfig(DomainName.enableBrightness, model)

        enableOutsideAirOptimization = getDefaultEnableConfig(DomainName.enableOutsideAirOptimization, model)
    }

    override fun toString(): String {
        return "HyperStatSplitProfileConfiguration( " +
                "\naddress0Enabled=${address0Enabled.enabled}," + " address1Association=${address0SensorAssociation.temperatureAssociation}," +
                " address1Enabled=${address1Enabled.enabled}," + " address1Association=${address1SensorAssociation.temperatureAssociation}," +
                "\naddress2Enabled=${address2Enabled.enabled}, " + " address2Association=${address2SensorAssociation.temperatureAssociation}," +
                "\nuniversal1InEnabled=${universal1InEnabled.enabled}," + " universal2InEnabled=${universal2InEnabled.enabled}, " +
                "\nuniversal1InAssociation=${universal1InAssociation.associationVal}, " + "universal2InAssociation=${universal2InAssociation.associationVal}," +
                "\nuniversal3InAssociation=${universal3InAssociation.associationVal}, " + "universal4InAssociation=${universal4InAssociation.associationVal}," +
                "\nuniversal5InAssociation=${universal5InAssociation.associationVal}, " + "universal6InAssociation=${universal6InAssociation.associationVal}," +
                "\nuniversal7InAssociation=${universal7InAssociation.associationVal}, " + "universal8InAssociation=${universal8InAssociation.associationVal}," +
                "\nrelay1Enabled=${relay1Enabled.enabled}," + " relay2Enabled=${relay2Enabled.enabled}," +
                "\nrelay3Enabled=${relay3Enabled.enabled}, " + "relay4Enabled=${relay4Enabled.enabled}, " +
                "\nrelay5Enabled=${relay5Enabled.enabled}, " + "relay6Enabled=${relay6Enabled.enabled}," +
                "\nrelay7Enabled=${relay7Enabled.enabled}," + " relay8Enabled=${relay8Enabled.enabled}," +
                "\nrelay1Association=${relay1Association.associationVal}, " + "relay2Association=${relay2Association.associationVal}," +
                "\nrelay3Association=${relay3Association.associationVal}," + " relay4Association=${relay4Association.associationVal}," +
                "\nrelay5Association=${relay5Association.associationVal}," + " relay6Association=${relay6Association.associationVal}," +
                "\nrelay7Association=${relay7Association.associationVal}, " + "relay8Association=${relay8Association.associationVal}," +
                "\nanalogOut1Enabled=${analogOut1Enabled.enabled}, " + "analogOut2Enabled=${analogOut2Enabled.enabled}," +
                "\nanalogOut3Enabled=${analogOut3Enabled.enabled}, " + "analogOut4Enabled=${analogOut4Enabled.enabled}," +
                "\nanalogOut1Association=${analogOut1Association.associationVal}," + " " + "\nanalogOut2Association=${analogOut2Association.associationVal}," +
                "" + "\nanalogOut3Association=${analogOut3Association.associationVal}," + "\nanalogOut4Association=${analogOut4Association.associationVal})" +
                "\ntemperatureOffset=${temperatureOffset.currentVal}, " + "\nautoForceOccupied=${autoForceOccupied.enabled}," +
                "\nautoAway=${autoAway.enabled}, " + "\noutsideDamperMinOpenDuringRecirc=${outsideDamperMinOpenDuringRecirc.currentVal}," +
                "\noutsideDamperMinOpenDuringConditioning=${outsideDamperMinOpenDuringConditioning.currentVal}," +
                "\noutsideDamperMinOpenDuringFanLow=${outsideDamperMinOpenDuringFanLow.currentVal}," +
                "\noutsideDamperMinOpenDuringFanMedium=${outsideDamperMinOpenDuringFanMedium.currentVal}," +
                "\noutsideDamperMinOpenDuringFanHigh=${outsideDamperMinOpenDuringFanHigh.currentVal}," +
                "\nexhaustFanStage1Threshold=${exhaustFanStage1Threshold.currentVal}," +
                "\nexhaustFanStage2Threshold=${exhaustFanStage2Threshold.currentVal}," +
                "\nexhaustFanHysteresis=${exhaustFanHysteresis.currentVal}," +
                "\nzoneCO2DamperOpeningRate=${zoneCO2DamperOpeningRate.currentVal}," +
                "\nzoneCO2Threshold=${zoneCO2Threshold.currentVal}," + "\nzoneCO2Target=${zoneCO2Target.currentVal}," +
                "\n" + "\nzoneVOCTarget=${zoneVOCTarget.currentVal}," +
                "\nzonePM2p5Target=${zonePM2p5Target.currentVal}," +
                "\ndisplayHumidity=${displayHumidity.enabled}," + "\ndisplayCO2=${displayCO2.enabled}," +
                "\n" + "\ndisplayPM2p5=${displayPM2p5.enabled}), " +
                "\ndisableTouch=${disableTouch.enabled}, " + "\nenableBrightness=${enableBrightness.enabled},"
    }

    open fun analogOut1TypeToString(): String { return "0-10v" }
    open fun analogOut2TypeToString(): String { return "0-10v" }
    open fun analogOut3TypeToString(): String { return "0-10v" }
    open fun analogOut4TypeToString(): String { return "0-10v" }

}

data class SensorTempHumidityAssociationConfig(
    var temperatureAssociation: AssociationConfig,
    var humidityAssociation: AssociationConfig
)

