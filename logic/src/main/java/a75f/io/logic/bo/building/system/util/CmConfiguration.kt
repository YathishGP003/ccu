package a75f.io.logic.bo.building.system.util

import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef

/**
 * Created by Manjunath K on 05-04-2024.
 */

open class CmConfiguration(open val model: SeventyFiveFProfileDirective) :
    ProfileConfiguration(99, "", 0, "SYSTEM", "SYSTEM", model.domainName) {

    lateinit var address0Enabled: EnableConfig
    lateinit var address1Enabled: EnableConfig
    lateinit var address2Enabled: EnableConfig
    lateinit var address3Enabled: EnableConfig
    lateinit var sensorBus0PressureEnabled: EnableConfig

    lateinit var address0SensorAssociation: SensorAssociationConfig
    lateinit var address1SensorAssociation: SensorAssociationConfig
    lateinit var address2SensorAssociation: SensorAssociationConfig
    lateinit var address3SensorAssociation: SensorAssociationConfig

    lateinit var analog1InEnabled: EnableConfig
    lateinit var analog2InEnabled: EnableConfig
    lateinit var analog1InAssociation: AssociationConfig
    lateinit var analog2InAssociation: AssociationConfig

    lateinit var thermistor1Enabled: EnableConfig
    lateinit var thermistor2Enabled: EnableConfig
    lateinit var thermistor1Association: AssociationConfig
    lateinit var thermistor2Association: AssociationConfig

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

    lateinit var pressureControlAssociation: AssociationConfig
    lateinit var staticMinPressure: ValueConfig
    lateinit var staticMaxPressure: ValueConfig

    lateinit var satControlAssociation: AssociationConfig
    lateinit var systemSatCoolingMin: ValueConfig
    lateinit var systemSatCoolingMax: ValueConfig
    lateinit var systemSatHeatingMin: ValueConfig
    lateinit var systemSatHeatingMax: ValueConfig

    lateinit var damperControlAssociation: AssociationConfig
    lateinit var co2Threshold: ValueConfig
    lateinit var co2Target: ValueConfig
    lateinit var damperOpeningRate: ValueConfig

    lateinit var analog1MinMaxVoltage: AnalogOutMinMaxVoltage
    lateinit var analog2MinMaxVoltage: AnalogOutMinMaxVoltage
    lateinit var analog3MinMaxVoltage: AnalogOutMinMaxVoltage
    lateinit var analog4MinMaxVoltage: AnalogOutMinMaxVoltage


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

            add(analog1InEnabled)
            add(analog2InEnabled)
            add(thermistor1Enabled)
            add(thermistor2Enabled)

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
        }
    }


    /**
     * Get a list of domainNames of all associations
     *
     */
    override fun getAssociationConfigs(): List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
            add(analog1InAssociation)
            add(analog2InAssociation)
            add(thermistor1Association)
            add(thermistor2Association)
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
            add(pressureControlAssociation)
            add(satControlAssociation)
            add(damperControlAssociation)
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
            add(staticMinPressure)
            add(staticMaxPressure)
            add(systemSatCoolingMin)
            add(systemSatCoolingMax)
            add(systemSatHeatingMin)
            add(systemSatHeatingMax)
            add(co2Threshold)
            add(co2Target)
            add(damperOpeningRate)
            addAll(addValueConfig(analog1MinMaxVoltage, this))
            addAll(addValueConfig(analog2MinMaxVoltage, this))
            addAll(addValueConfig(analog3MinMaxVoltage, this))
            addAll(addValueConfig(analog4MinMaxVoltage, this))
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
     * Get the default configuration
     */
    fun getDefaultConfiguration(): CmConfiguration {
        getAddressEnableConfig()
        getAddressAssociations()
        getAnalogInConfig()
        getThermistorConfig()
        getRelayEnableConfig()
        getRelayAssociationConfig()
        getAnalogOutEnableConfig()
        getAnalogOutAssociationConfig()
        getPressureConfig()
        getSatControlConfig()
        getDamperControlConfig()
        getAnalogOut1MinMaxVoltage()
        getAnalogOut2MinMaxVoltage()
        getAnalogOut3MinMaxVoltage()
        getAnalogOut4MinMaxVoltage()
        return this
    }

    /**
     * Get the default enable config for the domain name
     */
    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(staticMinPressure)
            add(staticMaxPressure)
            add(systemSatCoolingMin)
            add(systemSatCoolingMax)
            add(systemSatHeatingMin)
            add(systemSatHeatingMax)
            add(co2Threshold)
            add(co2Target)
            add(damperOpeningRate)
            addValueConfig(analog1MinMaxVoltage, this)
            addValueConfig(analog2MinMaxVoltage, this)
            addValueConfig(analog3MinMaxVoltage, this)
            addValueConfig(analog4MinMaxVoltage, this)
        }
    }

    override fun getConfigByDomainName(domainName: String): Any? {
        getValueConfigs().find { it.domainName == domainName }?.let { return it }
        getDependencies().find { it.domainName == domainName }?.let { return it }
        getAssociationConfigs().find { it.domainName == domainName }?.let { return it }
        return ValueConfig(domainName, 0.0)
    }

    override fun getCustomPoints(): List<Pair<SeventyFiveFProfilePointDef, Any>> {
        val customPoints = mutableListOf<Pair<SeventyFiveFProfilePointDef, Any>>()
        customPoints.add(Pair(model.points.find { it.domainName == DomainName.supplyAirTempControlOn }!!, satControlAssociation))
        return customPoints
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
            pressureAssociation = null // No pressure sensor for address 2
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
            pressureAssociation = null // No pressure sensor for address 3
        )
    }

    /**
     * Get the default enable config for the domain name
     */
    private fun getAnalogInConfig() {
        analog1InEnabled = getDefaultEnableConfig(DomainName.analog1InputEnable, model)
        analog2InEnabled = getDefaultEnableConfig(DomainName.analog2InputEnable, model)
        analog1InAssociation =
            getDefaultAssociationConfig(DomainName.analog1InputAssociation, model)
        analog2InAssociation =
            getDefaultAssociationConfig(DomainName.analog2InputAssociation, model)
    }

    /**
     * Get the default enable config for the domain name
     */
    private fun getThermistorConfig() {
        thermistor1Enabled = getDefaultEnableConfig(DomainName.thermistor1InputEnable, model)
        thermistor2Enabled = getDefaultEnableConfig(DomainName.thermistor2InputEnable, model)
        thermistor1Association =
            getDefaultAssociationConfig(DomainName.thermistor1InputAssociation, model)
        thermistor2Association =
            getDefaultAssociationConfig(DomainName.thermistor2InputAssociation, model)
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
    private fun getPressureConfig() {
        pressureControlAssociation =
            getDefaultAssociationConfig(DomainName.pressureBasedFanControlOn, model)
        staticMinPressure = getDefaultValConfig(DomainName.staticPressureMin, model)
        staticMaxPressure = getDefaultValConfig(DomainName.staticPressureMax, model)
    }

    /**
     * Get the default enable config for the domain name
     */
    private fun getSatControlConfig() {
        satControlAssociation = getDefaultAssociationConfig(DomainName.supplyAirTempControlOn, model)
        systemSatCoolingMin = getDefaultValConfig(DomainName.systemCoolingSatMin, model)
        systemSatCoolingMax = getDefaultValConfig(DomainName.systemCoolingSatMax, model)
        systemSatHeatingMin = getDefaultValConfig(DomainName.systemHeatingSatMin, model)
        systemSatHeatingMax = getDefaultValConfig(DomainName.systemHeatingSatMax, model)
    }

    /**
     * Get the default enable config for the domain name
     */
    private fun getDamperControlConfig() {
        damperControlAssociation =
            getDefaultAssociationConfig(DomainName.co2BasedDamperControlOn, model)
        co2Threshold = getDefaultValConfig(DomainName.co2Threshold, model)
        co2Target = getDefaultValConfig(DomainName.co2Target, model)
        damperOpeningRate = getDefaultValConfig(DomainName.co2DamperOpeningRate, model)
    }

    /**
     * Analog Voltage configuation
     */
    private fun getAnalogOut1MinMaxVoltage() {
        analog1MinMaxVoltage = AnalogOutMinMaxVoltage(
            staticPressureMinVoltage = getDefaultValConfig(DomainName.analog1MinStaticPressure, model),
            staticPressureMaxVoltage = getDefaultValConfig(DomainName.analog1MaxStaticPressure, model),
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
            compressorMaxVoltage = getDefaultValConfig(DomainName.analog1MaxCompressorSpeed, model)
        )
    }

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
        return "CmConfiguration(address0Enabled=$address0Enabled, address1Enabled=$address1Enabled," +
                "\n address2Enabled=$address2Enabled, address3Enabled=$address3Enabled, sensorBus0PressureEnabled=$sensorBus0PressureEnabled," +
                "\n address0SensorAssociation=$address0SensorAssociation, address1SensorAssociation=$address1SensorAssociation," +
                "\n address2SensorAssociation=$address2SensorAssociation, address3SensorAssociation=$address3SensorAssociation," +
                "\n analog1InEnabled=$analog1InEnabled, analog2InEnabled=$analog2InEnabled, analog1InAssociation=$analog1InAssociation, " +
                "\n analog2InAssociation=$analog2InAssociation, thermistor1Enabled=$thermistor1Enabled, thermistor2Enabled=$thermistor2Enabled, " +
                "\n thermistor1Association=$thermistor1Association, thermistor2Association=$thermistor2Association, relay1Enabled=$relay1Enabled, " +
                "\n relay2Enabled=$relay2Enabled, relay3Enabled=$relay3Enabled, relay4Enabled=$relay4Enabled, relay5Enabled=$relay5Enabled, " +
                "\n relay6Enabled=$relay6Enabled, relay7Enabled=$relay7Enabled, relay8Enabled=$relay8Enabled, relay1Association=$relay1Association," +
                "\n relay2Association=$relay2Association, relay3Association=$relay3Association, relay4Association=$relay4Association," +
                "\n relay5Association=$relay5Association, relay6Association=$relay6Association, relay7Association=$relay7Association," +
                "\n relay8Association=$relay8Association, analogOut1Enabled=$analogOut1Enabled, analogOut2Enabled=$analogOut2Enabled," +
                "\n analogOut3Enabled=$analogOut3Enabled, analogOut4Enabled=$analogOut4Enabled, analogOut1Association=$analogOut1Association," +
                "\n analogOut2Association=$analogOut2Association, analogOut3Association=$analogOut3Association, " +
                "\n analogOut4Association=$analogOut4Association, pressureControlAssociation=$pressureControlAssociation," +
                "\n staticMinPressure=$staticMinPressure, staticMaxPressure=$staticMaxPressure, satControlAssociation=$satControlAssociation," +
                "\n systemSatCoolingMin=$systemSatCoolingMin, systemSatCoolingMax=$systemSatCoolingMax, systemSatHeatingMin=$systemSatHeatingMin," +
                "\n systemSatHeatingMax=$systemSatHeatingMax, damperControlAssociation=$damperControlAssociation, co2Threshold=$co2Threshold," +
                " co2Target=$co2Target, damperOpeningRate=$damperOpeningRate," +
                " analog1MinMaxVoltage= ( $analog1MinMaxVoltage, )" +
                " analog2MinMaxVoltage=$analog2MinMaxVoltage," +
                " analog3MinMaxVoltage=$analog3MinMaxVoltage," +
                " analog4MinMaxVoltage=$analog4MinMaxVoltage)"
    }
}

/**
 * Created by Manjunath K on 05-04-2024.
 */
data class AnalogOutMinMaxVoltage(
    var staticPressureMinVoltage: ValueConfig, var staticPressureMaxVoltage: ValueConfig,
    var satCoolingMinVoltage: ValueConfig, var satCoolingMaxVoltage: ValueConfig,
    var satHeatingMinVoltage: ValueConfig, var satHeatingMaxVoltage: ValueConfig,
    var heatingMinVoltage: ValueConfig, var heatingMaxVoltage: ValueConfig,
    var coolingMinVoltage: ValueConfig, var coolingMaxVoltage: ValueConfig,
    var compositeCoolingMinVoltage: ValueConfig, var compositeCoolingMaxVoltage: ValueConfig,
    var compositeHeatingMinVoltage: ValueConfig, var compositeHeatingMaxVoltage: ValueConfig,
    var fanMinVoltage: ValueConfig, var fanMaxVoltage: ValueConfig,
    var damperPosMinVoltage: ValueConfig, var damperPosMaxVoltage: ValueConfig,
    var compressorMinVoltage: ValueConfig, var compressorMaxVoltage: ValueConfig,
) {
    override fun toString(): String {
        return "AnalogOutMinMaxVoltage(staticPressureMinVoltage=$staticPressureMinVoltage, staticPressureMaxVoltage=$staticPressureMaxVoltage, satCoolingMinVoltage=$satCoolingMinVoltage, satCoolingMaxVoltage=$satCoolingMaxVoltage, satHeatingMinVoltage=$satHeatingMinVoltage, satHeatingMaxVoltage=$satHeatingMaxVoltage, heatingMinVoltage=$heatingMinVoltage, heatingMaxVoltage=$heatingMaxVoltage, coolingMinVoltage=$coolingMinVoltage, coolingMaxVoltage=$coolingMaxVoltage, fanMinVoltage=$fanMinVoltage, fanMaxVoltage=$fanMaxVoltage, damperPosMinVoltage=$damperPosMinVoltage, damperPosMaxVoltage=$damperPosMaxVoltage)"
    }
}

/**
 * Created by Manjunath K on 05-04-2024.
 */
data class SensorAssociationConfig(
    var temperatureAssociation: AssociationConfig,
    var humidityAssociation: AssociationConfig,
    var occupancyAssociation: AssociationConfig,
    var co2Association: AssociationConfig,
    var pressureAssociation: AssociationConfig?
)
