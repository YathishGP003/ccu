package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.StringValueConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.HyperStatSplitEquip
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuAnalogControlType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuRelayType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UVRelayControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UvAnalogOutControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVRelayControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UvAnalogOutControls
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

abstract class HyperStatSplitConfiguration (nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String, profileType : ProfileType, val model : SeventyFiveFProfileDirective) :
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
    lateinit var zonePM2p5Target: ValueConfig

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
            add(backLight)

            add(enableOutsideAirOptimization)
            add(installerPinEnable)
            add(conditioningModePinEnable)

            add(desiredTemp)
            add(spaceTemp)
        }
    }

    override fun getStringConfigs(): List<StringValueConfig> {
        return mutableListOf<StringValueConfig>().apply {
            add(installerPassword)
            add(conditioningModePassword)
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
    open fun getDefaultConfiguration(): HyperStatSplitConfiguration {
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
        return mutableListOf()
    }

    private fun getAddressAssociations() {
        address0SensorAssociation = SensorTempHumidityAssociationConfig(
            temperatureAssociation = getDefaultAssociationConfig(
                DomainName.temperatureSensorBusAdd0,
                model
            ),
            humidityAssociation = getDefaultAssociationConfig(
                DomainName.humiditySensorBusAdd0,
                model
            )
        )
        pressureAddress0SensorAssociation =
            getDefaultAssociationConfig(DomainName.pressureSensorBusAdd0, model)
        address1SensorAssociation = SensorTempHumidityAssociationConfig(
            temperatureAssociation = getDefaultAssociationConfig(
                DomainName.temperatureSensorBusAdd1,
                model
            ),
            humidityAssociation = getDefaultAssociationConfig(
                DomainName.humiditySensorBusAdd1,
                model
            )
        )
        address2SensorAssociation = SensorTempHumidityAssociationConfig(
            temperatureAssociation = getDefaultAssociationConfig(
                DomainName.temperatureSensorBusAdd2,
                model
            ),
            humidityAssociation = getDefaultAssociationConfig(
                DomainName.humiditySensorBusAdd2,
                model
            )
        )
    }


    fun getActiveEnableConfigs(equip: HyperStatSplitEquip) {
        apply {
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

            universal1InEnabled.enabled = equip.universalIn1Enable.readDefaultVal() > 0.0
            universal2InEnabled.enabled = equip.universalIn2Enable.readDefaultVal() > 0.0
            universal3InEnabled.enabled = equip.universalIn3Enable.readDefaultVal() > 0.0
            universal4InEnabled.enabled = equip.universalIn4Enable.readDefaultVal() > 0.0
            universal5InEnabled.enabled = equip.universalIn5Enable.readDefaultVal() > 0.0
            universal6InEnabled.enabled = equip.universalIn6Enable.readDefaultVal() > 0.0
            universal7InEnabled.enabled = equip.universalIn7Enable.readDefaultVal() > 0.0
            universal8InEnabled.enabled = equip.universalIn8Enable.readDefaultVal() > 0.0
        }
    }

    fun getActiveAssociationConfigs(equip: HyperStatSplitEquip) {
        getSensorAssociationConfigs(equip)

        apply {
            if (relay1Enabled.enabled) {
                relay1Association.associationVal =
                    equip.relay1OutputAssociation.readDefaultVal().toInt()
            }
            if (relay2Enabled.enabled) {
                relay2Association.associationVal =
                    equip.relay2OutputAssociation.readDefaultVal().toInt()
            }
            if (relay3Enabled.enabled) {
                relay3Association.associationVal =
                    equip.relay3OutputAssociation.readDefaultVal().toInt()
            }
            if (relay4Enabled.enabled) {
                relay4Association.associationVal =
                    equip.relay4OutputAssociation.readDefaultVal().toInt()
            }
            if (relay5Enabled.enabled) {
                relay5Association.associationVal =
                    equip.relay5OutputAssociation.readDefaultVal().toInt()
            }
            if (relay6Enabled.enabled) {
                relay6Association.associationVal =
                    equip.relay6OutputAssociation.readDefaultVal().toInt()
            }
            if (relay7Enabled.enabled) {
                relay7Association.associationVal =
                    equip.relay7OutputAssociation.readDefaultVal().toInt()
            }
            if (relay8Enabled.enabled) {
                relay8Association.associationVal =
                    equip.relay8OutputAssociation.readDefaultVal().toInt()
            }

            if (analogOut1Enabled.enabled) {
                analogOut1Association.associationVal =
                    equip.analog1OutputAssociation.readDefaultVal().toInt()
            }
            if (analogOut2Enabled.enabled) {
                analogOut2Association.associationVal =
                    equip.analog2OutputAssociation.readDefaultVal().toInt()
            }
            if (analogOut3Enabled.enabled) {
                analogOut3Association.associationVal =
                    equip.analog3OutputAssociation.readDefaultVal().toInt()
            }
            if (analogOut4Enabled.enabled) {
                analogOut4Association.associationVal =
                    equip.analog4OutputAssociation.readDefaultVal().toInt()
            }

            if (universal1InEnabled.enabled) {
                universal1InAssociation.associationVal =
                    equip.universalIn1Association.readDefaultVal().toInt()
            }
            if (universal2InEnabled.enabled) {
                universal2InAssociation.associationVal =
                    equip.universalIn2Association.readDefaultVal().toInt()
            }
            if (universal3InEnabled.enabled) {
                universal3InAssociation.associationVal =
                    equip.universalIn3Association.readDefaultVal().toInt()
            }
            if (universal4InEnabled.enabled) {
                universal4InAssociation.associationVal =
                    equip.universalIn4Association.readDefaultVal().toInt()
            }
            if (universal5InEnabled.enabled) {
                universal5InAssociation.associationVal =
                    equip.universalIn5Association.readDefaultVal().toInt()
            }
            if (universal6InEnabled.enabled) {
                universal6InAssociation.associationVal =
                    equip.universalIn6Association.readDefaultVal().toInt()
            }
            if (universal7InEnabled.enabled) {
                universal7InAssociation.associationVal =
                    equip.universalIn7Association.readDefaultVal().toInt()
            }
            if (universal8InEnabled.enabled) {
                universal8InAssociation.associationVal =
                    equip.universalIn8Association.readDefaultVal().toInt()
            }
        }
    }

    private fun getSensorAssociationConfigs(equip: HyperStatSplitEquip) {
        apply {

            if (address0Enabled.enabled) {
                address0SensorAssociation.temperatureAssociation.associationVal =
                    equip.temperatureSensorBusAdd0.readDefaultVal().toInt()
                address0SensorAssociation.humidityAssociation.associationVal =
                    equip.humiditySensorBusAdd0.readDefaultVal().toInt()
            }

            if (sensorBusPressureEnable.enabled) {
                pressureAddress0SensorAssociation.associationVal =
                    equip.pressureSensorBusAdd0.readDefaultVal().toInt()
            }

            if (address1Enabled.enabled) {
                address1SensorAssociation.temperatureAssociation.associationVal =
                    equip.temperatureSensorBusAdd1.readDefaultVal().toInt()
                address1SensorAssociation.humidityAssociation.associationVal =
                    equip.humiditySensorBusAdd1.readDefaultVal().toInt()
            }

            if (address2Enabled.enabled) {
                address2SensorAssociation.temperatureAssociation.associationVal =
                    equip.temperatureSensorBusAdd2.readDefaultVal().toInt()
                address2SensorAssociation.humidityAssociation.associationVal =
                    equip.humiditySensorBusAdd2.readDefaultVal().toInt()
            }

        }
    }

    fun getGenericZoneConfigs(equip: HyperStatSplitEquip) {
        apply {
            temperatureOffset.currentVal = equip.temperatureOffset.readDefaultVal()

            autoForceOccupied.enabled = equip.autoForceOccupied.readDefaultVal() > 0.0
            autoAway.enabled = equip.autoAway.readDefaultVal() > 0.0
            prePurge.enabled = equip.prePurgeEnable.readDefaultVal() > 0.0

            zoneCO2Threshold.currentVal = equip.co2Threshold.readDefaultVal()
            zoneCO2Target.currentVal = equip.co2Target.readDefaultVal()

            zonePM2p5Target.currentVal = equip.pm25Target.readDefaultVal()

            if (prePurge.enabled) prePurgeOutsideDamperOpen.currentVal =
                equip.prePurgeOutsideDamperOpen.readDefaultVal()

            displayHumidity.enabled = equip.enableHumidityDisplay.readDefaultVal() > 0.0
            displayCO2.enabled = equip.enableCO2Display.readDefaultVal() > 0.0
            displayPM2p5.enabled = equip.enablePm25Display.readDefaultVal() > 0.0

            disableTouch.enabled = equip.disableTouch.readDefaultVal() > 0.0
            enableBrightness.enabled = equip.enableBrightness.readDefaultVal() > 0.0
            backLight.enabled = equip.enableBacklight.readDefaultVal() > 0.0

            enableOutsideAirOptimization.enabled =
                equip.enableOutsideAirOptimization.readDefaultVal() > 0.0

            installerPinEnable.enabled = equip.installerPinEnable.readDefaultVal() > 0.0
            conditioningModePinEnable.enabled =
                equip.enableConditioningModeFanAccess.readDefaultVal() > 0.0
            desiredTemp.enabled = equip.enableDesiredTempDisplay.readDefaultVal() > 0.0
            spaceTemp.enabled = equip.enableSpaceTempDisplay.readDefaultVal() > 0.0

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
        outsideDamperMinOpenDuringRecirc =
            getDefaultValConfig(DomainName.outsideDamperMinOpenDuringRecirculation, model)
        outsideDamperMinOpenDuringConditioning =
            getDefaultValConfig(DomainName.outsideDamperMinOpenDuringConditioning, model)
        outsideDamperMinOpenDuringFanLow =
            getDefaultValConfig(DomainName.outsideDamperMinOpenDuringFanLow, model)
        outsideDamperMinOpenDuringFanMedium =
            getDefaultValConfig(DomainName.outsideDamperMinOpenDuringFanMedium, model)
        outsideDamperMinOpenDuringFanHigh =
            getDefaultValConfig(DomainName.outsideDamperMinOpenDuringFanHigh, model)

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
        zonePM2p5Target = getDefaultValConfig(DomainName.pm25Target, model)

        displayHumidity = getDefaultEnableConfig(DomainName.enableHumidityDisplay, model)
        displayCO2 = getDefaultEnableConfig(DomainName.enableCO2Display, model)
        displayPM2p5 = getDefaultEnableConfig(DomainName.enablePm25Display, model)

        disableTouch = getDefaultEnableConfig(DomainName.disableTouch, model)
        backLight = getDefaultEnableConfig(DomainName.enableBacklight, model)
        enableBrightness = getDefaultEnableConfig(DomainName.enableBrightness, model)

        enableOutsideAirOptimization =
            getDefaultEnableConfig(DomainName.enableOutsideAirOptimization, model)

        // Pin enable configs
        installerPinEnable = getDefaultEnableConfig(DomainName.enableInstallerAccess, model)
        conditioningModePinEnable =
            getDefaultEnableConfig(DomainName.enableConditioningModeFanAccess, model)

        desiredTemp = getDefaultEnableConfig(DomainName.enableDesiredTempDisplay, model)
        spaceTemp = getDefaultEnableConfig(DomainName.enableSpaceTempDisplay, model)

        installerPassword = getDefaultStringConfig(DomainName.pinLockInstallerAccess, model)
        conditioningModePassword =
            getDefaultStringConfig(DomainName.pinLockConditioningModeFanAccess, model)
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
                "\nautoAway=${autoAway.enabled} " + "\noutsideDamperMinOpenDuringRecirc=${outsideDamperMinOpenDuringRecirc.currentVal}," +
                "\noutsideDamperMinOpenDuringConditioning=${outsideDamperMinOpenDuringConditioning.currentVal}," +
                "\noutsideDamperMinOpenDuringFanLow=${outsideDamperMinOpenDuringFanLow.currentVal}," +
                "\noutsideDamperMinOpenDuringFanMedium=${outsideDamperMinOpenDuringFanMedium.currentVal}," +
                "\noutsideDamperMinOpenDuringFanHigh=${outsideDamperMinOpenDuringFanHigh.currentVal}," +
                "\nexhaustFanStage1Threshold=${exhaustFanStage1Threshold.currentVal}," +
                "\nexhaustFanStage2Threshold=${exhaustFanStage2Threshold.currentVal}," +
                "\nexhaustFanHysteresis=${exhaustFanHysteresis.currentVal}," +
                "\nzoneCO2DamperOpeningRate=${zoneCO2DamperOpeningRate.currentVal}," +
                "\nzoneCO2Threshold=${zoneCO2Threshold.currentVal}," + "\nzoneCO2Target=${zoneCO2Target.currentVal}," +
                "\nzonePM2p5Target=${zonePM2p5Target.currentVal}," +
                "\ndisplayHumidity=${displayHumidity.enabled}," + "\ndisplayCO2=${displayCO2.enabled}," +
                "\ndisplayPM2p5=${displayPM2p5.enabled}),  installer pin Enable =${installerPinEnable.enabled} , conditioning mode pin enable =${conditioningModePinEnable}" +
                "\ninstallerPassword=${installerPassword.currentVal}, conditioningModePassword=${conditioningModePassword.currentVal}" +
                "\ndisableTouch=${disableTouch.enabled}, backlight =${backLight.enabled} " + "\nenableBrightness=${enableBrightness.enabled}"
    }

    fun getHighestStage(stage1: Int, stage2: Int, stage3: Int): Int {
        val availableStages = availableHighestStages(stage1, stage2, stage3)
        return if (availableStages.third) stage3
        else if (availableStages.second) stage2
        else if (availableStages.first) stage1
        else -1
    }

    private fun availableHighestStages(
        stage1: Int,
        stage2: Int,
        stage3: Int
    ): Triple<Boolean, Boolean, Boolean> {
        var isStage1Selected = false
        var isStage2Selected = false
        var isStage3Selected = false

        getRelayEnabledAssociations().forEach { (enabled, associated) ->
            if (enabled) {
                if (associated == stage1) isStage1Selected = true
                if (associated == stage2) isStage2Selected = true
                if (associated == stage3) isStage3Selected = true
            }
        }
        return Triple(isStage1Selected, isStage2Selected, isStage3Selected)
    }

    protected fun getLowestStage(stage1: Int, stage2: Int, stage3: Int): Int {
        val availableStages = availableHighestStages(stage1, stage2, stage3)
        return if (availableStages.first) stage1
        else if (availableStages.second) stage2
        else if (availableStages.third) stage3
        else -1
    }

    fun getRelayConfigurationMapping(): List<Triple<Boolean, Int, Port>> {
        return listOf(
            Triple(relay1Enabled.enabled, relay1Association.associationVal, Port.RELAY_ONE),
            Triple(relay2Enabled.enabled, relay2Association.associationVal, Port.RELAY_TWO),
            Triple(relay3Enabled.enabled, relay3Association.associationVal, Port.RELAY_THREE),
            Triple(relay4Enabled.enabled, relay4Association.associationVal, Port.RELAY_FOUR),
            Triple(relay5Enabled.enabled, relay5Association.associationVal, Port.RELAY_FIVE),
            Triple(relay6Enabled.enabled, relay6Association.associationVal, Port.RELAY_SIX),
            Triple(relay7Enabled.enabled, relay7Association.associationVal, Port.RELAY_SEVEN),
            Triple(relay8Enabled.enabled, relay8Association.associationVal, Port.RELAY_EIGHT),
        )
    }

    fun getAnalogOutsConfigurationMapping(): List<Triple<Boolean, Int, Port>> {
        return listOf(
            Triple(
                analogOut1Enabled.enabled,
                analogOut1Association.associationVal,
                Port.ANALOG_OUT_ONE
            ),
            Triple(
                analogOut2Enabled.enabled,
                analogOut2Association.associationVal,
                Port.ANALOG_OUT_TWO
            ),
            Triple(
                analogOut3Enabled.enabled,
                analogOut3Association.associationVal,
                Port.ANALOG_OUT_THREE
            ),
            Triple(
                analogOut4Enabled.enabled,
                analogOut4Association.associationVal,
                Port.ANALOG_OUT_FOUR
            )
        )
    }

    open fun analogOut1TypeToString(): String {
        return "0-10v"
    }

    open fun analogOut2TypeToString(): String {
        return "0-10v"
    }

    open fun analogOut3TypeToString(): String {
        return "0-10v"
    }

    open fun analogOut4TypeToString(): String {
        return "0-10v"
    }

    abstract fun getHighestFanStageCount(): Int

    fun getRelayEnabledAssociations(): List<Pair<Boolean, Int>> = buildList {
        listOf(
            relay1Enabled to relay1Association,
            relay2Enabled to relay2Association,
            relay3Enabled to relay3Association,
            relay4Enabled to relay4Association,
            relay5Enabled to relay5Association,
            relay6Enabled to relay6Association,
            relay7Enabled to relay7Association,
            relay8Enabled to relay8Association
        ).forEach { (enabled, association) ->
            if (enabled.enabled) add(true to association.associationVal)
        }
    }

    fun getAnalogEnabledAssociations(): List<Pair<Boolean, Int>> = buildList {
        listOf(
            analogOut1Enabled to analogOut1Association,
            analogOut2Enabled to analogOut2Association,
            analogOut3Enabled to analogOut3Association,
            analogOut4Enabled to analogOut4Association
        ).forEach { (enable, association) ->
            if (enable.enabled) add(true to association.associationVal)
        }
    }

    /**
     * Function to get the point value if config exist else return the current value model default value
     */
    fun getDefault(point: Point, equip: HyperStatSplitEquip, valueConfig: ValueConfig): Double {
        return if (Domain.readPointForEquip(point.domainName, equip.equipRef).isEmpty())
            valueConfig.currentVal
        else
            point.readDefaultVal()
    }

    fun isFanEnabled(
        config: HyperStatSplitConfiguration,
        relayControl: String = HyperStatSplitControlType.FAN_ENABLED.name
    ) = isAnyRelayEnabledAndMapped(config, relayControl)


    fun isAnyRelayEnabledAndMapped(
        config: HyperStatSplitConfiguration,
        relayType: String
    ): Boolean {
        return when (config) {
            is Pipe4UVConfiguration -> {
                val target = Pipe4UVRelayControls.valueOf(relayType)
                config.getRelayEnabledAssociations().any { (enabled, type) ->
                    enabled && type == target.ordinal
                }
            }

            is HyperStatSplitCpuConfiguration -> {
                val target = CpuRelayType.valueOf(relayType)
                config.getRelayEnabledAssociations().any { (enabled, type) ->
                    enabled && type == target.ordinal
                }
            }

            is Pipe2UVConfiguration -> {
                val target = Pipe4UVRelayControls.valueOf(relayType)
                config.getRelayEnabledAssociations().any { (enabled, type) ->
                    enabled && type == target.ordinal
                }
            }

            else -> false
        }
    }

    // getting the profile based enum value
    fun getProfileBasedEnumValueAnalogType(
        enumName: String,
        profileConfiguration: HyperStatSplitConfiguration
    ): Int {
        return when (profileConfiguration) {
            is HyperStatSplitCpuConfiguration -> CpuAnalogControlType.valueOf(enumName).ordinal
            is Pipe4UVConfiguration -> Pipe4UvAnalogOutControls.valueOf(enumName).ordinal
            is Pipe2UVConfiguration -> Pipe2UvAnalogOutControls.valueOf(enumName).ordinal
            else -> CpuAnalogControlType.valueOf(enumName).ordinal
        }
    }

    // getting the profile based enum value
    fun getProfileBasedEnumValueRelayType(
        enumName: String,
        profileConfiguration: HyperStatSplitConfiguration
    ): Int {
        return when (profileConfiguration) {
            is HyperStatSplitCpuConfiguration -> CpuRelayType.valueOf(enumName).ordinal
            is Pipe4UVConfiguration -> Pipe4UVRelayControls.valueOf(enumName).ordinal
            is Pipe2UVConfiguration -> Pipe2UVRelayControls.valueOf(enumName).ordinal
            else -> {
                HyperStatSplitControlType.valueOf(enumName).ordinal
            }
        }
    }
}

    /// this enum is used only  for string reference
    enum class HyperStatSplitControlType {
        HEATING_WATER_VALVE,COOLING_WATER_VALVE,FACE_DAMPER_VALVE ,OAO_DAMPER, DCV_MODULATING_DAMPER, EXTERNALLY_MAPPED, COOLING,
        LINEAR_FAN, HEATING, STAGED_FAN, RETURN_DAMPER, COMPRESSOR_SPEED,FAN_ENABLED , FAN_SPEED
        ,HEATING_WATER_MODULATING_VALVE ,COOLING_WATER_MODULATING_VALVE ,FAN_LOW_SPEED_VENTILATION  ,FAN_LOW_SPEED,WATER_VALVE ,WATER_MODULATING_VALVE

    }


data class SensorTempHumidityAssociationConfig(
    var temperatureAssociation: AssociationConfig,
    var humidityAssociation: AssociationConfig
)

enum class UniversalInputs {
    NONE, VOLTAGE_INPUT, THERMISTOR_INPUT, BUILDING_STATIC_PRESSURE1, BUILDING_STATIC_PRESSURE2, BUILDING_STATIC_PRESSURE10, INDEX_6, INDEX_7, INDEX_8, INDEX_9,
    INDEX_10, INDEX_11, INDEX_12, INDEX_13, SUPPLY_AIR_TEMPERATURE, INDEX_15, DUCT_STATIC_PRESSURE1_1, DUCT_STATIC_PRESSURE1_2, DUCT_STATIC_PRESSURE1_10, INDEX_19, INDEX_20,
    INDEX_21, INDEX_22, INDEX_23, INDEX_24, INDEX_25, INDEX_26, INDEX_27, MIXED_AIR_TEMPERATURE, OUTSIDE_AIR_DAMPER_FEEDBACK, INDEX_30,
    INDEX_31, INDEX_32, OUTSIDE_AIR_TEMPERATURE, INDEX_34, INDEX_35, INDEX_36, INDEX_37, INDEX_38, INDEX_39, INDEX_40,
    CURRENT_TX_10, CURRENT_TX_20, CURRENT_TX_30, CURRENT_TX_50, CURRENT_TX_60, CURRENT_TX_100, CURRENT_TX_120, CURRENT_TX_150, CURRENT_TX_200, INDEX_50,
    INDEX_51, INDEX_52, DISCHARGE_FAN_AM_STATUS, DISCHARGE_FAN_RUN_STATUS, DISCHARGE_FAN_TRIP_STATUS, INDEX_56, INDEX_57, EXHAUST_FAN_RUN_STATUS, EXHAUST_FAN_TRIP_STATUS, FILTER_STATUS_NO,
    FILTER_STATUS_NC, INDEX_62, INDEX_63, FIRE_ALARM_STATUS, INDEX_65, INDEX_66, INDEX_67, INDEX_68, INDEX_69, INDEX_70,
    INDEX_71, INDEX_72, HIGH_DIFFERENTIAL_PRESSURE_SWITCH, LOW_DIFFERENTIAL_PRESSURE_SWITCH, INDEX_75, INDEX_76, INDEX_77, INDEX_78, INDEX_79, INDEX_80,
    INDEX_81, INDEX_82, CONDENSATE_STATUS_NO, CONDENSATE_STATUS_NC, INDEX_85, INDEX_86, INDEX_87, INDEX_88, INDEX_89, INDEX_90,
    EMERGENCY_SHUTOFF_NO, EMERGENCY_SHUTOFF_NC, GENERIC_ALARM_NO, GENERIC_ALARM_NC, DOOR_WINDOW_SENSOR_NC, DOOR_WINDOW_SENSOR, DOOR_WINDOW_SENSOR_TITLE24_NC, DOOR_WINDOW_SENSOR_TITLE24, RUN_FAN_STATUS_NO, RUN_FAN_STATUS_NC,
    FIRE_ALARM_STATUS_NC, DOOR_WINDOW_SENSOR_NO, DOOR_WINDOW_SENSOR_NO_TITLE_24, KEYCARD_SENSOR_NO, KEYCARD_SENSOR_NC, CHILLED_WATER_SUPPLY_TEMPERATURE, HOT_WATER_SUPPLY_TEMPERATURE,SUPPLY_WATER_TEMPERATURE

}

enum class CpuSensorBusType {
    SUPPLY_AIR, MIXED_AIR, OUTSIDE_AIR
}

enum class CpuEconSensorBusTempAssociation {
   SUPPLY_AIR_TEMPERATURE_HUMIDITY,
   MIXED_AIR_TEMPERATURE_HUMIDITY,
   OUTSIDE_AIR_TEMPERATURE_HUMIDITY
}