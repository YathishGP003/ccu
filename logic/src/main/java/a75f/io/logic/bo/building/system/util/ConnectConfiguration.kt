package a75f.io.logic.bo.building.system.util

import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created by Manjunath K on 11-04-2024.
 */

class ConnectConfiguration(open val model: SeventyFiveFProfileDirective, nodeAddress: Int):
    ProfileConfiguration(nodeAddress, "", 0, "SYSTEM", "SYSTEM", model.domainName){
    var connectEnabled: Boolean = false

    lateinit var address0Enabled: EnableConfig
    lateinit var address1Enabled: EnableConfig
    lateinit var address2Enabled: EnableConfig
    lateinit var address3Enabled: EnableConfig
    lateinit var sensorBus0PressureEnabled: EnableConfig

    lateinit var address0SensorAssociation: SensorAssociationConfig
    lateinit var address1SensorAssociation: SensorAssociationConfig
    lateinit var address2SensorAssociation: SensorAssociationConfig
    lateinit var address3SensorAssociation: SensorAssociationConfig

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

    lateinit var damperControlAssociation: AssociationConfig
    lateinit var co2Threshold: ValueConfig
    lateinit var co2Target: ValueConfig
    lateinit var damperOpeningRate: ValueConfig

    lateinit var analog1MinMaxVoltage: AnalogOutMinMaxVoltage
    lateinit var analog2MinMaxVoltage: AnalogOutMinMaxVoltage
    lateinit var analog3MinMaxVoltage: AnalogOutMinMaxVoltage
    lateinit var analog4MinMaxVoltage: AnalogOutMinMaxVoltage

    // OAO configuration
    lateinit var analog1MinOaoDamper:ValueConfig
    lateinit var analog1MaxOaoDamper:ValueConfig
    lateinit var analog2MinOaoDamper:ValueConfig
    lateinit var analog2MaxOaoDamper:ValueConfig
    lateinit var analog3MinOaoDamper:ValueConfig
    lateinit var analog3MaxOaoDamper:ValueConfig
    lateinit var analog4MinOaoDamper:ValueConfig
    lateinit var analog4MaxOaoDamper:ValueConfig

    lateinit var analog1MinReturnDamper:ValueConfig
    lateinit var analog1MaxReturnDamper:ValueConfig
    lateinit var analog2MinReturnDamper:ValueConfig
    lateinit var analog2MaxReturnDamper:ValueConfig
    lateinit var analog3MinReturnDamper:ValueConfig
    lateinit var analog3MaxReturnDamper:ValueConfig
    lateinit var analog4MinReturnDamper:ValueConfig
    lateinit var analog4MaxReturnDamper:ValueConfig

    lateinit var outsideDamperMinOpenDuringRecirculation: ValueConfig
    lateinit var outsideDamperMinOpenDuringConditioning: ValueConfig
    lateinit var outsideDamperMinOpenDuringFanLow: ValueConfig
    lateinit var outsideDamperMinOpenDuringFanMedium: ValueConfig
    lateinit var outsideDamperMinOpenDuringFanHigh: ValueConfig
    lateinit var returnDamperMinOpen: ValueConfig
    lateinit var exhaustFanStage1Threshold: ValueConfig
    lateinit var exhaustFanStage2Threshold: ValueConfig
    lateinit var currentTransformerType: ValueConfig
    lateinit var exhaustFanHysteresis: ValueConfig
    lateinit var usePerRoomCO2Sensing: EnableConfig
    lateinit var enableOutsideAirOptimization: EnableConfig
    lateinit var systemPurgeOutsideDamperMinPos: ValueConfig
    lateinit var enhancedVentilationOutsideDamperMinOpen: ValueConfig

    /**
     * Get a list of domainNames of all base-configs
     * This need not have all base points.
     * Only configs which are configured via UI.
     *
     */
    override fun getEnableConfigs(): List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(address0Enabled)
            add(address1Enabled)
            add(address2Enabled)
            add(address3Enabled)
            add(sensorBus0PressureEnabled)

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
           // OAO configuration
            add(usePerRoomCO2Sensing)
            add(enableOutsideAirOptimization)
        }
    }

    /**
     * Get a list of domainNames of all associations
     *
     */
    override fun getAssociationConfigs(): List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
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
            addAll(addSensorAssociation(address0SensorAssociation, this))
            addAll(addSensorAssociation(address1SensorAssociation, this))
            addAll(addSensorAssociation(address2SensorAssociation, this))
            addAll(addSensorAssociation(address3SensorAssociation, this))

        }
    }

    /**
     * Get a list of domainNames of all dependencies
     *
     */
    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(co2Threshold)
            add(co2Target)
            add(damperOpeningRate)
            addAll(addValueConfig(analog1MinMaxVoltage, this))
            addAll(addValueConfig(analog2MinMaxVoltage, this))
            addAll(addValueConfig(analog3MinMaxVoltage, this))
            addAll(addValueConfig(analog4MinMaxVoltage, this))
            //OAO configurations
            addValueConfig(analog1MinOaoDamper, this)
            addValueConfig(analog1MaxOaoDamper, this)
            addValueConfig(analog2MinOaoDamper, this)
            addValueConfig(analog2MaxOaoDamper, this)
            addValueConfig(analog3MinOaoDamper, this)
            addValueConfig(analog3MaxOaoDamper, this)
            addValueConfig(analog4MinOaoDamper, this)
            addValueConfig(analog4MaxOaoDamper, this)
            addValueConfig(analog1MinReturnDamper, this)
            addValueConfig(analog1MaxReturnDamper, this)
            addValueConfig(analog2MinReturnDamper, this)
            addValueConfig(analog2MaxReturnDamper, this)
            addValueConfig(analog3MinReturnDamper, this)
            addValueConfig(analog3MaxReturnDamper, this)
            addValueConfig(analog4MinReturnDamper, this)
            addValueConfig(analog4MaxReturnDamper, this)
            addValueConfig(outsideDamperMinOpenDuringRecirculation, this)
            addValueConfig(outsideDamperMinOpenDuringConditioning, this)
            addValueConfig(outsideDamperMinOpenDuringFanLow, this)
            addValueConfig(outsideDamperMinOpenDuringFanMedium, this)
            addValueConfig(outsideDamperMinOpenDuringFanHigh, this)
            addValueConfig(returnDamperMinOpen, this)
            addValueConfig(exhaustFanStage1Threshold, this)
            addValueConfig(exhaustFanStage2Threshold, this)
            addValueConfig(currentTransformerType, this)
            addValueConfig(co2Threshold, this)
            addValueConfig(exhaustFanHysteresis, this)
            addValueConfig(systemPurgeOutsideDamperMinPos, this)
            addValueConfig(enhancedVentilationOutsideDamperMinOpen, this)
        }
    }

    /**
     * Add all value configs to the list
     */
    private fun addValueConfig(
            analogConfig: AnalogOutMinMaxVoltage, list: MutableList<ValueConfig>
    ): MutableList<ValueConfig> {
        return list.apply {
            add(analogConfig.staticPressureMinVoltage)
            add(analogConfig.staticPressureMaxVoltage)
            add(analogConfig.satCoolingMinVoltage)
            add(analogConfig.satCoolingMaxVoltage)
            add(analogConfig.satHeatingMinVoltage)
            add(analogConfig.satHeatingMaxVoltage)
            add(analogConfig.heatingMinVoltage)
            add(analogConfig.heatingMaxVoltage)
            add(analogConfig.coolingMinVoltage)
            add(analogConfig.coolingMaxVoltage)
            add(analogConfig.compositeCoolingMinVoltage)
            add(analogConfig.compositeCoolingMaxVoltage)
            add(analogConfig.compositeHeatingMinVoltage)
            add(analogConfig.compositeHeatingMaxVoltage)
            add(analogConfig.fanMinVoltage)
            add(analogConfig.fanMaxVoltage)
            add(analogConfig.damperPosMinVoltage)
            add(analogConfig.damperPosMaxVoltage)
            add(analogConfig.compressorMinVoltage)
            add(analogConfig.compressorMaxVoltage)
        }

    }

    private fun addValueConfig(
        oaoConfiguration: ValueConfig, list: MutableList<ValueConfig>
    ): MutableList<ValueConfig> {
        return list.apply {
            add(oaoConfiguration)
        }
    }


    /**
     * Add all sensor associations to the list
     */
    private fun addSensorAssociation(
            addressBus: SensorAssociationConfig, list: MutableList<AssociationConfig>
    ): MutableList<AssociationConfig> {
        return list.apply {
            add(addressBus.temperatureAssociation)
            add(addressBus.humidityAssociation)
            add(addressBus.occupancyAssociation)
            add(addressBus.co2Association)
            addressBus.pressureAssociation?.let { add(it) }
        }
    }

    /**
     * Get a list of domainNames of all dependencies
     */
    fun getDefaultConfiguration(): ConnectConfiguration {
        getAddressEnableConfig()
        getAddressAssociations()
        getUniversalInConfig()
        getRelayEnableConfig()
        getRelayAssociationConfig()
        getAnalogOutEnableConfig()
        getAnalogOutAssociationConfig()
        getDamperControlConfig()
        getAnalogOut1MinMaxVoltage()
        getAnalogOut2MinMaxVoltage()
        getAnalogOut3MinMaxVoltage()
        getAnalogOut4MinMaxVoltage()
        getOAOConfiguration()
        return this
    }

    private fun getOAOConfiguration() {

        analog1MinOaoDamper = getDefaultValConfig(DomainName.analog1MinOAODamper, model)
        analog1MaxOaoDamper = getDefaultValConfig(DomainName.analog1MaxOAODamper, model)
        analog2MinOaoDamper = getDefaultValConfig(DomainName.analog2MinOAODamper, model)
        analog2MaxOaoDamper = getDefaultValConfig(DomainName.analog2MaxOAODamper, model)
        analog3MinOaoDamper = getDefaultValConfig(DomainName.analog3MinOAODamper, model)
        analog3MaxOaoDamper = getDefaultValConfig(DomainName.analog3MaxOAODamper, model)
        analog4MinOaoDamper = getDefaultValConfig(DomainName.analog4MinOAODamper, model)
        analog4MaxOaoDamper = getDefaultValConfig(DomainName.analog4MaxOAODamper, model)

        analog1MinReturnDamper = getDefaultValConfig(DomainName.analog1MinReturnDamper, model)
        analog1MaxReturnDamper = getDefaultValConfig(DomainName.analog1MaxReturnDamper, model)
        analog2MinReturnDamper = getDefaultValConfig(DomainName.analog2MinReturnDamper, model)
        analog2MaxReturnDamper = getDefaultValConfig(DomainName.analog2MaxReturnDamper, model)
        analog3MinReturnDamper = getDefaultValConfig(DomainName.analog3MinReturnDamper, model)
        analog3MaxReturnDamper = getDefaultValConfig(DomainName.analog3MaxReturnDamper, model)
        analog4MinReturnDamper = getDefaultValConfig(DomainName.analog4MinReturnDamper, model)
        analog4MaxReturnDamper = getDefaultValConfig(DomainName.analog4MaxReturnDamper, model)

        outsideDamperMinOpenDuringRecirculation = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringRecirculation, model)
        outsideDamperMinOpenDuringConditioning = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringConditioning, model)
        outsideDamperMinOpenDuringFanLow = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringFanLow, model)
        outsideDamperMinOpenDuringFanMedium = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringFanMedium, model)
        outsideDamperMinOpenDuringFanHigh = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringFanHigh, model)
        returnDamperMinOpen = getDefaultValConfig(DomainName.returnDamperMinOpen, model)
        exhaustFanStage1Threshold = getDefaultValConfig(DomainName.exhaustFanStage1Threshold, model)
        exhaustFanStage2Threshold = getDefaultValConfig(DomainName.exhaustFanStage2Threshold, model)
        currentTransformerType = getDefaultValConfig(DomainName.currentTransformerType, model)
        co2Threshold = getDefaultValConfig(DomainName.co2Threshold, model)
        exhaustFanHysteresis = getDefaultValConfig(DomainName.exhaustFanHysteresis, model)
        usePerRoomCO2Sensing = getDefaultEnableConfig(DomainName.usePerRoomCO2Sensing, model)
        enableOutsideAirOptimization = getDefaultEnableConfig(DomainName.enableOutsideAirOptimization, model)

        systemPurgeOutsideDamperMinPos = getDefaultValConfig(DomainName.systemPurgeOutsideDamperMinPos, model)
        enhancedVentilationOutsideDamperMinOpen = getDefaultValConfig(DomainName.enhancedVentilationOutsideDamperMinOpen, model)
    }

    /**
     * Get the default enable config for the domain name
     */
    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(co2Threshold)
            add(co2Target)
            add(damperOpeningRate)
            addValueConfig(analog1MinMaxVoltage, this)
            addValueConfig(analog2MinMaxVoltage, this)
            addValueConfig(analog3MinMaxVoltage, this)
            addValueConfig(analog4MinMaxVoltage, this)
        }
    }

    private fun getDamperControlConfig() {
        damperControlAssociation =
            getDefaultAssociationConfig(DomainName.co2BasedDamperControlOn, model)
        co2Threshold = getDefaultValConfig(DomainName.co2Threshold, model)
        co2Target = getDefaultValConfig(DomainName.co2Target, model)
        damperOpeningRate = getDefaultValConfig(DomainName.co2DamperOpeningRate, model)
    }

    private fun getAddressAssociations() {
        address0SensorAssociation = SensorAssociationConfig(
            temperatureAssociation = getDefaultAssociationConfig(
                DomainName.temperatureSensorBusAdd0, model
            ),
            humidityAssociation = getDefaultAssociationConfig(
                DomainName.humiditySensorBusAdd0, model
            ),
            occupancyAssociation = getDefaultAssociationConfig(
                DomainName.occupancySensorBusAdd0, model
            ),
            co2Association = getDefaultAssociationConfig(DomainName.co2SensorBusAdd0, model),
            pressureAssociation = getDefaultAssociationConfig(
                DomainName.pressureSensorBusAdd0, model
            )
        )
        address1SensorAssociation = SensorAssociationConfig(
            temperatureAssociation = getDefaultAssociationConfig(
                DomainName.temperatureSensorBusAdd1, model
            ),
            humidityAssociation = getDefaultAssociationConfig(
                DomainName.humiditySensorBusAdd1, model
            ),
            occupancyAssociation = getDefaultAssociationConfig(
                DomainName.occupancySensorBusAdd1, model
            ),
            co2Association = getDefaultAssociationConfig(DomainName.co2SensorBusAdd1, model),
            pressureAssociation = null // No pressure sensor for address 1
        )
        address2SensorAssociation = SensorAssociationConfig(
            temperatureAssociation = getDefaultAssociationConfig(
                DomainName.temperatureSensorBusAdd2, model
            ),
            humidityAssociation = getDefaultAssociationConfig(
                DomainName.humiditySensorBusAdd2, model
            ),
            occupancyAssociation = getDefaultAssociationConfig(
                DomainName.occupancySensorBusAdd2, model
            ),
            co2Association = getDefaultAssociationConfig(DomainName.co2SensorBusAdd2, model),
            pressureAssociation = null // No pressure sensor for address 1
        )
        address3SensorAssociation = SensorAssociationConfig(
            temperatureAssociation = getDefaultAssociationConfig(
                DomainName.temperatureSensorBusAdd3, model
            ),
            humidityAssociation = getDefaultAssociationConfig(
                DomainName.humiditySensorBusAdd3, model
            ),
            occupancyAssociation = getDefaultAssociationConfig(
                DomainName.occupancySensorBusAdd3, model
            ),
            co2Association = getDefaultAssociationConfig(DomainName.co2SensorBusAdd3, model),
            pressureAssociation = null // No pressure sensor for address 1
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
        address1Enabled = getDefaultEnableConfig(DomainName.sensorBusAddress1Enable, model)
        address2Enabled = getDefaultEnableConfig(DomainName.sensorBusAddress2Enable, model)
        address3Enabled = getDefaultEnableConfig(DomainName.sensorBusAddress3Enable, model)
        sensorBus0PressureEnabled =
            getDefaultEnableConfig(DomainName.sensorBusPressureEnable, model)
    }

    /**
     * Get the default enable config for the domain name
     */
    private fun getAnalogOut1MinMaxVoltage() {
        analog1MinMaxVoltage = AnalogOutMinMaxVoltage(
            staticPressureMinVoltage = getDefaultValConfig(
                DomainName.analog1MinStaticPressure, model
            ),
            staticPressureMaxVoltage = getDefaultValConfig(
                DomainName.analog1MaxStaticPressure, model
            ),
            satCoolingMinVoltage = getDefaultValConfig(DomainName.analog1MinSatCooling, model),
            satCoolingMaxVoltage = getDefaultValConfig(DomainName.analog1MaxSatCooling, model),
            satHeatingMinVoltage = getDefaultValConfig(DomainName.analog1MinSatHeating, model),
            satHeatingMaxVoltage = getDefaultValConfig(DomainName.analog1MaxSatHeating, model),
            heatingMinVoltage = getDefaultValConfig(DomainName.analog1MinHeating, model),
            heatingMaxVoltage = getDefaultValConfig(DomainName.analog1MaxHeating, model),
            coolingMinVoltage = getDefaultValConfig(DomainName.analog1MinCooling, model),
            coolingMaxVoltage = getDefaultValConfig(DomainName.analog1MaxCooling, model),
            compositeCoolingMinVoltage = getDefaultValConfig(DomainName.analog1MinCoolingComposite, model),
            compositeCoolingMaxVoltage = getDefaultValConfig(DomainName.analog1MaxCoolingComposite, model),
            compositeHeatingMinVoltage = getDefaultValConfig(DomainName.analog1MinHeatingComposite, model),
            compositeHeatingMaxVoltage = getDefaultValConfig(DomainName.analog1MaxHeatingComposite, model),
            fanMinVoltage = getDefaultValConfig(DomainName.analog1MinFan, model),
            fanMaxVoltage = getDefaultValConfig(DomainName.analog1MaxFan, model),
            damperPosMinVoltage = getDefaultValConfig(DomainName.analog1MinDamperPos, model),
            damperPosMaxVoltage = getDefaultValConfig(DomainName.analog1MaxDamperPos, model),
            compressorMinVoltage = getDefaultValConfig(DomainName.analog1MinCompressorSpeed, model),
            compressorMaxVoltage = getDefaultValConfig(DomainName.analog1MaxCompressorSpeed, model),
        )
    }

    /**
     * Get the default enable config for the domain name
     */
    private fun getAnalogOut2MinMaxVoltage() {
        analog2MinMaxVoltage = AnalogOutMinMaxVoltage(
            staticPressureMinVoltage = getDefaultValConfig(
                DomainName.analog2MinStaticPressure, model
            ),
            staticPressureMaxVoltage = getDefaultValConfig(
                DomainName.analog2MaxStaticPressure, model
            ),
            satCoolingMinVoltage = getDefaultValConfig(DomainName.analog2MinSatCooling, model),
            satCoolingMaxVoltage = getDefaultValConfig(DomainName.analog2MaxSatCooling, model),
            satHeatingMinVoltage = getDefaultValConfig(DomainName.analog2MinSatHeating, model),
            satHeatingMaxVoltage = getDefaultValConfig(DomainName.analog2MaxSatHeating, model),
            heatingMinVoltage = getDefaultValConfig(DomainName.analog2MinHeating, model),
            heatingMaxVoltage = getDefaultValConfig(DomainName.analog2MaxHeating, model),
            coolingMinVoltage = getDefaultValConfig(DomainName.analog2MinCooling, model),
            coolingMaxVoltage = getDefaultValConfig(DomainName.analog2MaxCooling, model),
            compositeCoolingMinVoltage = getDefaultValConfig(DomainName.analog2MinCoolingComposite, model),
            compositeCoolingMaxVoltage = getDefaultValConfig(DomainName.analog2MaxCoolingComposite, model),
            compositeHeatingMinVoltage = getDefaultValConfig(DomainName.analog2MinHeatingComposite, model),
            compositeHeatingMaxVoltage = getDefaultValConfig(DomainName.analog2MaxHeatingComposite, model),
            fanMinVoltage = getDefaultValConfig(DomainName.analog2MinFan, model),
            fanMaxVoltage = getDefaultValConfig(DomainName.analog2MaxFan, model),
            damperPosMinVoltage = getDefaultValConfig(DomainName.analog2MinDamperPos, model),
            damperPosMaxVoltage = getDefaultValConfig(DomainName.analog2MaxDamperPos, model),
            compressorMinVoltage = getDefaultValConfig(DomainName.analog2MinCompressorSpeed, model),
            compressorMaxVoltage = getDefaultValConfig(DomainName.analog2MaxCompressorSpeed, model)
        )
    }

    /**
     * Get the default enable config for the domain name
     */
    private fun getAnalogOut3MinMaxVoltage() {
        analog3MinMaxVoltage = AnalogOutMinMaxVoltage(
            staticPressureMinVoltage = getDefaultValConfig(
                DomainName.analog3MinStaticPressure, model
            ),
            staticPressureMaxVoltage = getDefaultValConfig(
                DomainName.analog3MaxStaticPressure, model
            ),
            satCoolingMinVoltage = getDefaultValConfig(DomainName.analog3MinSatCooling, model),
            satCoolingMaxVoltage = getDefaultValConfig(DomainName.analog3MaxSatCooling, model),
            satHeatingMinVoltage = getDefaultValConfig(DomainName.analog3MinSatHeating, model),
            satHeatingMaxVoltage = getDefaultValConfig(DomainName.analog3MaxSatHeating, model),
            heatingMinVoltage = getDefaultValConfig(DomainName.analog3MinHeating, model),
            heatingMaxVoltage = getDefaultValConfig(DomainName.analog3MaxHeating, model),
            coolingMinVoltage = getDefaultValConfig(DomainName.analog3MinCooling, model),
            coolingMaxVoltage = getDefaultValConfig(DomainName.analog3MaxCooling, model),
            compositeCoolingMinVoltage = getDefaultValConfig(DomainName.analog3MinCoolingComposite, model),
            compositeCoolingMaxVoltage = getDefaultValConfig(DomainName.analog3MaxCoolingComposite, model),
            compositeHeatingMinVoltage = getDefaultValConfig(DomainName.analog3MinHeatingComposite, model),
            compositeHeatingMaxVoltage = getDefaultValConfig(DomainName.analog3MaxHeatingComposite, model),
            fanMinVoltage = getDefaultValConfig(DomainName.analog3MinFan, model),
            fanMaxVoltage = getDefaultValConfig(DomainName.analog3MaxFan, model),
            damperPosMinVoltage = getDefaultValConfig(DomainName.analog3MinDamperPos, model),
            damperPosMaxVoltage = getDefaultValConfig(DomainName.analog3MaxDamperPos, model),
            compressorMinVoltage = getDefaultValConfig(DomainName.analog3MinCompressorSpeed, model),
            compressorMaxVoltage = getDefaultValConfig(DomainName.analog3MaxCompressorSpeed, model)
        )
    }

    /**
     * Get the default enable config for the domain name
     */
    private fun getAnalogOut4MinMaxVoltage() {
        analog4MinMaxVoltage = AnalogOutMinMaxVoltage(
            staticPressureMinVoltage = getDefaultValConfig(
                DomainName.analog4MinStaticPressure, model
            ),
            staticPressureMaxVoltage = getDefaultValConfig(
                DomainName.analog4MaxStaticPressure, model
            ),
            satCoolingMinVoltage = getDefaultValConfig(DomainName.analog4MinSatCooling, model),
            satCoolingMaxVoltage = getDefaultValConfig(DomainName.analog4MaxSatCooling, model),
            satHeatingMinVoltage = getDefaultValConfig(DomainName.analog4MinSatHeating, model),
            satHeatingMaxVoltage = getDefaultValConfig(DomainName.analog4MaxSatHeating, model),
            heatingMinVoltage = getDefaultValConfig(DomainName.analog4MinHeating, model),
            heatingMaxVoltage = getDefaultValConfig(DomainName.analog4MaxHeating, model),
            coolingMinVoltage = getDefaultValConfig(DomainName.analog4MinCooling, model),
            coolingMaxVoltage = getDefaultValConfig(DomainName.analog4MaxCooling, model),
            compositeCoolingMinVoltage = getDefaultValConfig(DomainName.analog4MinCoolingComposite, model),
            compositeCoolingMaxVoltage = getDefaultValConfig(DomainName.analog4MaxCoolingComposite, model),
            compositeHeatingMinVoltage = getDefaultValConfig(DomainName.analog4MinHeatingComposite, model),
            compositeHeatingMaxVoltage = getDefaultValConfig(DomainName.analog4MaxHeatingComposite, model),
            fanMinVoltage = getDefaultValConfig(DomainName.analog4MinFan, model),
            fanMaxVoltage = getDefaultValConfig(DomainName.analog4MaxFan, model),
            damperPosMinVoltage = getDefaultValConfig(DomainName.analog4MinDamperPos, model),
            damperPosMaxVoltage = getDefaultValConfig(DomainName.analog4MaxDamperPos, model),
            compressorMinVoltage = getDefaultValConfig(DomainName.analog4MinCompressorSpeed, model),
            compressorMaxVoltage = getDefaultValConfig(DomainName.analog4MaxCompressorSpeed, model)
        )
    }

    override fun toString(): String {
        return "AdvancedAhuConnectConfig( " + "\naddress0Enabled=${address0Enabled.enabled}," +
                " address1Enabled=${address1Enabled.enabled}," + "\naddress2Enabled=${address2Enabled.enabled}, " +
                "address3Enabled=${address3Enabled.enabled}," + " pressureEnabled: $sensorBus0PressureEnabled" +
                "\nuniversal1InEnabled=${universal1InEnabled.enabled}," + " universal2InEnabled=${universal2InEnabled.enabled}, " +
                "\nuniversal1InAssociation=${universal1InAssociation.associationVal}, " + "universal2InAssociation=${universal2InAssociation.associationVal}," +
                "\nuniversal3InAssociation=${universal3InAssociation.associationVal}, " + "universal4InAssociation=${universal4InAssociation.associationVal}," +
                "\nuniversal5InAssociation=${universal5InAssociation.associationVal}, " + "universal6InAssociation=${universal6InAssociation.associationVal}," +
                "\nuniversal7InAssociation=${universal7InAssociation.associationVal}, " + "universal8InAssociation=${universal8InAssociation.associationVal}," +
                " relay1Enabled=${relay1Enabled.enabled}," + " relay2Enabled=${relay2Enabled.enabled}," +
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
                "" + "\nanalogOut3Association=${analogOut3Association.associationVal}," + "\nanalogOut4Association=${analogOut4Association.associationVal})"+
                "damper association $damperControlAssociation"+
                "\nco2Threshold=${co2Threshold.currentVal}, co2Target=${co2Target.currentVal}, damperOpeningRate=${damperOpeningRate.currentVal}," +
                "\nOAO enable = ${enableOutsideAirOptimization.enabled}" +
                "\n analog1MinOAODamper =  ${analog1MinOaoDamper.currentVal}, analog1MaxOAODamper = ${analog1MaxOaoDamper.currentVal},"+
                "\n analog2MinOAODamper = ${analog2MinOaoDamper.currentVal}, analog2MaxOAODamper = ${analog2MaxOaoDamper.currentVal},"+
                "\n analog3MinOAODamper = ${analog3MinOaoDamper.currentVal}, analog3MaxOAODamper = ${analog3MaxOaoDamper.currentVal},"+
                "\n analog4MinOAODamper = ${analog4MinOaoDamper.currentVal}, analog4MaxOAODamper = ${analog4MaxOaoDamper.currentVal},"+
                "\n analog1MinReturnDamper = ${analog1MinReturnDamper.currentVal}, analog1MaxReturnDamper = ${analog1MaxReturnDamper.currentVal},"+
                "\n analog2MinReturnDamper = ${analog2MinReturnDamper.currentVal}, analog2MaxReturnDamper = ${analog2MaxReturnDamper.currentVal},"+
                "\n analog3MinReturnDamper = ${analog3MinReturnDamper.currentVal}, analog3MaxReturnDamper = ${analog3MaxReturnDamper.currentVal},"+
                "\n analog4MinReturnDamper = ${analog4MinReturnDamper.currentVal}, analog4MaxReturnDamper = ${analog4MaxReturnDamper.currentVal},"+
                ""+"\n outsideDamperMinOpenDuringRecirculation = ${outsideDamperMinOpenDuringRecirculation.currentVal},"+"\noutsideDamperMinOpenDuringConditioning = ${outsideDamperMinOpenDuringConditioning.currentVal} "+
                "\n outsideDamperMinOpenDuringFanLow = ${outsideDamperMinOpenDuringFanLow.currentVal}" + " \noutsideDamperMinOpenDuringFanMedium = ${outsideDamperMinOpenDuringFanMedium.currentVal}"+
                " \n outsideDamperMinOpenDuringFanHigh = ${outsideDamperMinOpenDuringFanHigh.currentVal}"+"\n returnDamperMinOpen = ${returnDamperMinOpen.currentVal}"+
                "\n exhaustFanStage1Threshold = ${exhaustFanStage1Threshold.currentVal}"+"\n exhaustFanStage2Threshold = ${exhaustFanStage2Threshold.currentVal}"+
                "\n currentTransformerType = ${currentTransformerType.currentVal}"+"\n OAOCo2Threshold = ${co2Threshold.currentVal}"+
                "\n exhaustFanHysteresis = ${exhaustFanHysteresis.currentVal}"+" \nusePerRoomCO2Sensing = ${usePerRoomCO2Sensing.enabled}"+
                " \nsystemPurgeOutsideDamperMinPos = ${systemPurgeOutsideDamperMinPos.currentVal}"+"\n enhancedVentilationOutsideDamperMinOpen = ${enhancedVentilationOutsideDamperMinOpen.currentVal}"
    }

}