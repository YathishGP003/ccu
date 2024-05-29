package a75f.io.logic.bo.building.system.vav.config

import a75f.io.domain.api.Domain
import a75f.io.domain.api.Point
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.ConnectModuleEquip
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip
import a75f.io.logic.L
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

/**
 * Created by Manjunath K on 02-04-2024.
 */

open class AdvancedHybridAhuConfig(val cmModel: SeventyFiveFProfileDirective, connectModel: SeventyFiveFProfileDirective) {
    val cmConfiguration = CmConfiguration(cmModel)
    val connectConfiguration = ConnectConfiguration(connectModel, L.ccu().smartNodeAddressBand + 98)
    init {
        cmConfiguration.getDefaultConfiguration()
        connectConfiguration.getDefaultConfiguration()
    }

    /**
     * Get Active configuration values from model
     */
    open fun getActiveConfiguration(): AdvancedHybridAhuConfig {
        val cmEquip = Domain.hayStack.readEntity("domainName == \"" + DomainName.vavAdvancedHybridAhuV2 + "\"")
        if (cmEquip.isEmpty()) {
            return this
        }

        val connectEquip = Domain.hayStack.readEntity("domainName == \"" + DomainName.vavAdvancedHybridAhuV2_connectModule + "\"")
        val connectEquipRef = if (connectEquip.isNotEmpty()) connectEquip[Tags.ID].toString() else ""

        val advancedHybridAhuEquip = VavAdvancedHybridSystemEquip(cmEquip[Tags.ID].toString(), connectEquipRef)
        cmConfiguration.getDefaultConfiguration()
        getActiveEnabledConfigs(advancedHybridAhuEquip)
        getActiveAssociationConfigs(advancedHybridAhuEquip)
        getActiveDynamicConfigs(advancedHybridAhuEquip)

        connectConfiguration.getDefaultConfiguration()

        if (advancedHybridAhuEquip.connectEquip1.getId() != "") {
            connectConfiguration.connectEnabled = true
            connectConfiguration.nodeAddress = Integer.parseInt(connectEquip["group"].toString())
            getActiveEnabledConfigs(advancedHybridAhuEquip.connectEquip1)
            getActiveAssociationConfigs(advancedHybridAhuEquip.connectEquip1)
            getActiveDynamicConfigs(advancedHybridAhuEquip.connectEquip1)
        }

        cmConfiguration.isDefault = false
        connectConfiguration.isDefault = false
        return this
    }

    /**
     * Get active enabled configuration values from model
     */
    private fun getActiveEnabledConfigs(equip: VavAdvancedHybridSystemEquip) {
        cmConfiguration.apply {
            address0Enabled.enabled = equip.sensorBusAddress0Enable.readDefaultVal() > 0
            address1Enabled.enabled = equip.sensorBusAddress1Enable.readDefaultVal() > 0
            address2Enabled.enabled = equip.sensorBusAddress2Enable.readDefaultVal() > 0
            address3Enabled.enabled = equip.sensorBusAddress3Enable.readDefaultVal() > 0
            sensorBus0PressureEnabled.enabled = equip.sensorBus0PressureEnable.readDefaultVal() > 0

            analog1InEnabled.enabled = equip.analog1InputEnable.readDefaultVal() > 0
            analog2InEnabled.enabled = equip.analog2InputEnable.readDefaultVal() > 0

            thermistor1Enabled.enabled = equip.thermistor1InputEnable.readDefaultVal() > 0
            thermistor2Enabled.enabled = equip.thermistor2InputEnable.readDefaultVal() > 0

            relay1Enabled.enabled = equip.relay1OutputEnable.readDefaultVal() > 0
            relay2Enabled.enabled = equip.relay2OutputEnable.readDefaultVal() > 0
            relay3Enabled.enabled = equip.relay3OutputEnable.readDefaultVal() > 0
            relay4Enabled.enabled = equip.relay4OutputEnable.readDefaultVal() > 0
            relay5Enabled.enabled = equip.relay5OutputEnable.readDefaultVal() > 0
            relay6Enabled.enabled = equip.relay6OutputEnable.readDefaultVal() > 0
            relay7Enabled.enabled = equip.relay7OutputEnable.readDefaultVal() > 0
            relay8Enabled.enabled = equip.relay8OutputEnable.readDefaultVal() > 0

            analogOut1Enabled.enabled = equip.analog1OutputEnable.readDefaultVal() > 0
            analogOut2Enabled.enabled = equip.analog2OutputEnable.readDefaultVal() > 0
            analogOut3Enabled.enabled = equip.analog3OutputEnable.readDefaultVal() > 0
            analogOut4Enabled.enabled = equip.analog4OutputEnable.readDefaultVal() > 0
        }
    }

    /**
     * Get active enabled configuration values from model
     */
    private fun getActiveEnabledConfigs(equip: ConnectModuleEquip) {
        connectConfiguration.apply {
            address0Enabled.enabled = equip.sensorBusAddress0Enable.readDefaultVal() > 0
            address1Enabled.enabled = equip.sensorBusAddress1Enable.readDefaultVal() > 0
            address2Enabled.enabled = equip.sensorBusAddress2Enable.readDefaultVal() > 0
            address3Enabled.enabled = equip.sensorBusAddress3Enable.readDefaultVal() > 0
            sensorBus0PressureEnabled.enabled = equip.sensorBus0PressureEnable.readDefaultVal() > 0

            universal1InEnabled.enabled = equip.universalIn1Enable.readDefaultVal() > 0
            universal2InEnabled.enabled = equip.universalIn2Enable.readDefaultVal() > 0
            universal3InEnabled.enabled = equip.universalIn3Enable.readDefaultVal() > 0
            universal4InEnabled.enabled = equip.universalIn4Enable.readDefaultVal() > 0
            universal5InEnabled.enabled = equip.universalIn5Enable.readDefaultVal() > 0
            universal6InEnabled.enabled = equip.universalIn6Enable.readDefaultVal() > 0
            universal7InEnabled.enabled = equip.universalIn7Enable.readDefaultVal() > 0
            universal8InEnabled.enabled = equip.universalIn8Enable.readDefaultVal() > 0

            relay1Enabled.enabled = equip.relay1OutputEnable.readDefaultVal() > 0
            relay2Enabled.enabled = equip.relay2OutputEnable.readDefaultVal() > 0
            relay3Enabled.enabled = equip.relay3OutputEnable.readDefaultVal() > 0
            relay4Enabled.enabled = equip.relay4OutputEnable.readDefaultVal() > 0
            relay5Enabled.enabled = equip.relay5OutputEnable.readDefaultVal() > 0
            relay6Enabled.enabled = equip.relay6OutputEnable.readDefaultVal() > 0
            relay7Enabled.enabled = equip.relay7OutputEnable.readDefaultVal() > 0
            relay8Enabled.enabled = equip.relay8OutputEnable.readDefaultVal() > 0

            analogOut1Enabled.enabled = equip.analog1OutputEnable.readDefaultVal() > 0
            analogOut2Enabled.enabled = equip.analog2OutputEnable.readDefaultVal() > 0
            analogOut3Enabled.enabled = equip.analog3OutputEnable.readDefaultVal() > 0
            analogOut4Enabled.enabled = equip.analog4OutputEnable.readDefaultVal() > 0
        }
    }

    /**
     * Get Active configuration values from model
     */
    private fun getActiveAssociationConfigs(equip: VavAdvancedHybridSystemEquip) {
        getSensorAssociationConfigs(equip)

        cmConfiguration.apply {
            if (analog1InEnabled.enabled) {
                analog1InAssociation.associationVal =
                    equip.analog1InputAssociation.readDefaultVal().toInt()
            }
            if (analog2InEnabled.enabled) {
                analog2InAssociation.associationVal =
                    equip.analog2InputAssociation.readDefaultVal().toInt()
            }
            if (thermistor1Enabled.enabled) {
                thermistor1Association.associationVal =
                    equip.thermistor1InputAssociation.readDefaultVal().toInt()
            }
            if (thermistor2Enabled.enabled) {
                thermistor2Association.associationVal =
                    equip.thermistor2InputAssociation.readDefaultVal().toInt()
            }
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
            pressureControlAssociation.associationVal =
                equip.pressureBasedFanControlOn.readDefaultVal().toInt()
            satControlAssociation.associationVal =
                equip.supplyAirTempControlOn.readDefaultVal().toInt()
            damperControlAssociation.associationVal =
                equip.co2BasedDamperControlOn.readDefaultVal().toInt()
        }
    }

    /**
     * Get Active configuration values from model
     */
    private fun getActiveAssociationConfigs(equip: ConnectModuleEquip) {
        getSensorAssociationConfigs(equip)

        connectConfiguration.apply {
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
            damperControlAssociation.associationVal =
                equip.co2BasedDamperControlOn.readDefaultVal().toInt()
        }
    }

    /**
     * Get active configuration values from model
     */
    private fun getSensorAssociationConfigs(equip: VavAdvancedHybridSystemEquip) {
        cmConfiguration.apply {
            address0SensorAssociation.temperatureAssociation.associationVal =
                equip.temperatureSensorBusAdd0.readDefaultVal().toInt()
            address0SensorAssociation.humidityAssociation.associationVal =
                equip.humiditySensorBusAdd0.readDefaultVal().toInt()
            address0SensorAssociation.occupancyAssociation.associationVal =
                equip.occupancySensorBusAdd0.readDefaultVal().toInt()
            address0SensorAssociation.co2Association.associationVal =
                equip.co2SensorBusAdd0.readDefaultVal().toInt()
            address0SensorAssociation.pressureAssociation?.associationVal =
                equip.sensorBus0PressureAssociation.readDefaultVal().toInt()

            address1SensorAssociation.temperatureAssociation.associationVal =
                equip.temperatureSensorBusAdd1.readDefaultVal().toInt()
            address1SensorAssociation.humidityAssociation.associationVal =
                equip.humiditySensorBusAdd1.readDefaultVal().toInt()
            address1SensorAssociation.occupancyAssociation.associationVal =
                equip.occupancySensorBusAdd1.readDefaultVal().toInt()
            address1SensorAssociation.co2Association.associationVal =
                equip.co2SensorBusAdd1.readDefaultVal().toInt()

            address2SensorAssociation.temperatureAssociation.associationVal =
                equip.temperatureSensorBusAdd2.readDefaultVal().toInt()
            address2SensorAssociation.humidityAssociation.associationVal =
                equip.humiditySensorBusAdd2.readDefaultVal().toInt()
            address2SensorAssociation.occupancyAssociation.associationVal =
                equip.occupancySensorBusAdd2.readDefaultVal().toInt()
            address2SensorAssociation.co2Association.associationVal =
                equip.co2SensorBusAdd2.readDefaultVal().toInt()

            address3SensorAssociation.temperatureAssociation.associationVal =
                equip.temperatureSensorBusAdd3.readDefaultVal().toInt()
            address3SensorAssociation.humidityAssociation.associationVal =
                equip.humiditySensorBusAdd3.readDefaultVal().toInt()
            address3SensorAssociation.occupancyAssociation.associationVal =
                equip.occupancySensorBusAdd3.readDefaultVal().toInt()
            address3SensorAssociation.co2Association.associationVal =
                equip.co2SensorBusAdd3.readDefaultVal().toInt()
        }


    }

    /**
     * Get active configuration values from model
     */
    private fun getSensorAssociationConfigs(equip: ConnectModuleEquip) {
        connectConfiguration.apply {
            address0SensorAssociation.temperatureAssociation.associationVal =
                equip.temperatureSensorBusAdd0.readDefaultVal().toInt()
            address0SensorAssociation.humidityAssociation.associationVal =
                equip.humiditySensorBusAdd0.readDefaultVal().toInt()
            address0SensorAssociation.occupancyAssociation.associationVal =
                equip.occupancySensorBusAdd0.readDefaultVal().toInt()
            address0SensorAssociation.co2Association.associationVal =
                equip.co2SensorBusAdd0.readDefaultVal().toInt()
            address0SensorAssociation.pressureAssociation?.associationVal =
                equip.sensorBus0PressureAssociation.readDefaultVal().toInt()

            address1SensorAssociation.temperatureAssociation.associationVal =
                equip.temperatureSensorBusAdd1.readDefaultVal().toInt()
            address1SensorAssociation.humidityAssociation.associationVal =
                equip.humiditySensorBusAdd1.readDefaultVal().toInt()
            address1SensorAssociation.occupancyAssociation.associationVal =
                equip.occupancySensorBusAdd1.readDefaultVal().toInt()
            address1SensorAssociation.co2Association.associationVal =
                equip.co2SensorBusAdd1.readDefaultVal().toInt()

            address2SensorAssociation.temperatureAssociation.associationVal =
                equip.temperatureSensorBusAdd2.readDefaultVal().toInt()
            address2SensorAssociation.humidityAssociation.associationVal =
                equip.humiditySensorBusAdd2.readDefaultVal().toInt()
            address2SensorAssociation.occupancyAssociation.associationVal =
                equip.occupancySensorBusAdd2.readDefaultVal().toInt()
            address2SensorAssociation.co2Association.associationVal =
                equip.co2SensorBusAdd2.readDefaultVal().toInt()

            address3SensorAssociation.temperatureAssociation.associationVal =
                equip.temperatureSensorBusAdd3.readDefaultVal().toInt()
            address3SensorAssociation.humidityAssociation.associationVal =
                equip.humiditySensorBusAdd3.readDefaultVal().toInt()
            address3SensorAssociation.occupancyAssociation.associationVal =
                equip.occupancySensorBusAdd3.readDefaultVal().toInt()
            address3SensorAssociation.co2Association.associationVal =
                equip.co2SensorBusAdd3.readDefaultVal().toInt()
        }
    }

    /**
     * Get active dynamic configuration values from model
     */
    private fun getActiveDynamicConfigs(equip: VavAdvancedHybridSystemEquip) {
        cmConfiguration.apply {

            staticMinPressure.currentVal = getDefault(equip.staticPressureMin, equip, staticMinPressure)
            staticMaxPressure.currentVal = getDefault(equip.staticPressureMax, equip, staticMaxPressure)
            systemSatCoolingMin.currentVal = getDefault(equip.systemCoolingSatMin, equip, systemSatCoolingMin)
            systemSatCoolingMax.currentVal = getDefault(equip.systemCoolingSatMax, equip, systemSatCoolingMax)
            systemSatHeatingMin.currentVal = getDefault(equip.systemHeatingSatMin, equip, systemSatHeatingMin)
            systemSatHeatingMax.currentVal = getDefault(equip.systemHeatingSatMax, equip, systemSatHeatingMax)
            co2Target.currentVal = getDefault(equip.co2Target, equip, co2Target)
            co2Threshold.currentVal = getDefault(equip.co2Threshold, equip, co2Threshold)
            damperOpeningRate.currentVal = getDefault(equip.co2DamperOpeningRate, equip, damperOpeningRate)


            analog1MinMaxVoltage.apply {
                staticPressureMinVoltage.currentVal = getDefault(equip.analog1MinStaticPressure, equip, staticPressureMinVoltage)
                staticPressureMaxVoltage.currentVal = getDefault(equip.analog1MaxStaticPressure, equip, staticPressureMaxVoltage)
                satCoolingMinVoltage.currentVal = getDefault(equip.analog1MinSatCooling, equip, satCoolingMinVoltage)
                satCoolingMaxVoltage.currentVal = getDefault(equip.analog1MaxSatCooling, equip, satCoolingMaxVoltage)
                satHeatingMinVoltage.currentVal = getDefault(equip.analog1MinSatHeating, equip, satHeatingMinVoltage)
                satHeatingMaxVoltage.currentVal = getDefault(equip.analog1MaxSatHeating, equip, satHeatingMaxVoltage)
                heatingMinVoltage.currentVal = getDefault(equip.analog1MinHeating, equip, heatingMinVoltage)
                heatingMaxVoltage.currentVal = getDefault(equip.analog1MaxHeating, equip, heatingMaxVoltage)
                coolingMinVoltage.currentVal = getDefault(equip.analog1MinCooling, equip, coolingMinVoltage)
                coolingMaxVoltage.currentVal = getDefault(equip.analog1MaxCooling, equip, coolingMaxVoltage)
                compositeCoolingMinVoltage.currentVal = getDefault(equip.analog1MinCoolingComposite, equip, compositeCoolingMinVoltage)
                compositeCoolingMaxVoltage.currentVal = getDefault(equip.analog1MaxCoolingComposite, equip, compositeCoolingMaxVoltage)
                compositeHeatingMinVoltage.currentVal = getDefault(equip.analog1MinHeatingComposite, equip, compositeHeatingMinVoltage)
                compositeHeatingMaxVoltage.currentVal = getDefault(equip.analog1MaxHeatingComposite, equip, compositeHeatingMaxVoltage)
                fanMinVoltage.currentVal = getDefault(equip.analog1MinFan, equip, fanMinVoltage)
                fanMaxVoltage.currentVal = getDefault(equip.analog1MaxFan, equip, fanMaxVoltage)
                damperPosMinVoltage.currentVal = getDefault(equip.analog1DamperMinPos, equip, damperPosMinVoltage)
                damperPosMaxVoltage.currentVal = getDefault(equip.analog1DamperMaxPos, equip, damperPosMaxVoltage)
            }

            analog2MinMaxVoltage.apply {
                staticPressureMinVoltage.currentVal = getDefault(equip.analog2MinStaticPressure, equip, staticPressureMinVoltage)
                staticPressureMaxVoltage.currentVal = getDefault(equip.analog2MaxStaticPressure, equip, staticPressureMaxVoltage)
                satCoolingMinVoltage.currentVal = getDefault(equip.analog2MinSatCooling, equip, satCoolingMinVoltage)
                satCoolingMaxVoltage.currentVal = getDefault(equip.analog2MaxSatCooling, equip, satCoolingMaxVoltage)
                satHeatingMinVoltage.currentVal = getDefault(equip.analog2MinSatHeating, equip, satHeatingMinVoltage)
                satHeatingMaxVoltage.currentVal = getDefault(equip.analog2MaxSatHeating, equip, satHeatingMaxVoltage)
                heatingMinVoltage.currentVal = getDefault(equip.analog2MinHeating, equip, heatingMinVoltage)
                heatingMaxVoltage.currentVal = getDefault(equip.analog2MaxHeating, equip, heatingMaxVoltage)
                coolingMinVoltage.currentVal = getDefault(equip.analog2MinCooling, equip, coolingMinVoltage)
                coolingMaxVoltage.currentVal = getDefault(equip.analog2MaxCooling, equip, coolingMaxVoltage)
                compositeCoolingMinVoltage.currentVal = getDefault(equip.analog2MinCoolingComposite, equip, compositeCoolingMinVoltage)
                compositeCoolingMaxVoltage.currentVal = getDefault(equip.analog2MaxCoolingComposite, equip, compositeCoolingMaxVoltage)
                compositeHeatingMinVoltage.currentVal = getDefault(equip.analog2MinHeatingComposite, equip, compositeHeatingMinVoltage)
                compositeHeatingMaxVoltage.currentVal = getDefault(equip.analog2MaxHeatingComposite, equip, compositeHeatingMaxVoltage)
                fanMinVoltage.currentVal = getDefault(equip.analog2MinFan, equip, fanMinVoltage)
                fanMaxVoltage.currentVal = getDefault(equip.analog2MaxFan, equip, fanMaxVoltage)
                damperPosMinVoltage.currentVal = getDefault(equip.analog2DamperMinPos, equip, damperPosMinVoltage)
                damperPosMaxVoltage.currentVal = getDefault(equip.analog2DamperMaxPos, equip, damperPosMaxVoltage)
            }

            analog3MinMaxVoltage.apply {
                staticPressureMinVoltage.currentVal = getDefault(equip.analog3MinStaticPressure, equip, staticPressureMinVoltage)
                staticPressureMaxVoltage.currentVal = getDefault(equip.analog3MaxStaticPressure, equip, staticPressureMaxVoltage)
                satCoolingMinVoltage.currentVal = getDefault(equip.analog3MinSatCooling, equip, satCoolingMinVoltage)
                satCoolingMaxVoltage.currentVal = getDefault(equip.analog3MaxSatCooling, equip, satCoolingMaxVoltage)
                satHeatingMinVoltage.currentVal = getDefault(equip.analog3MinSatHeating, equip, satHeatingMinVoltage)
                satHeatingMaxVoltage.currentVal = getDefault(equip.analog3MaxSatHeating, equip, satHeatingMaxVoltage)
                heatingMinVoltage.currentVal = getDefault(equip.analog3MinHeating, equip, heatingMinVoltage)
                heatingMaxVoltage.currentVal = getDefault(equip.analog3MaxHeating, equip, heatingMaxVoltage)
                coolingMinVoltage.currentVal = getDefault(equip.analog3MinCooling, equip, coolingMinVoltage)
                coolingMaxVoltage.currentVal = getDefault(equip.analog3MaxCooling, equip, coolingMaxVoltage)
                compositeCoolingMinVoltage.currentVal = getDefault(equip.analog3MinCoolingComposite, equip, compositeCoolingMinVoltage)
                compositeCoolingMaxVoltage.currentVal = getDefault(equip.analog3MaxCoolingComposite, equip, compositeCoolingMaxVoltage)
                compositeHeatingMinVoltage.currentVal = getDefault(equip.analog3MinHeatingComposite, equip, compositeHeatingMinVoltage)
                compositeHeatingMaxVoltage.currentVal = getDefault(equip.analog3MaxHeatingComposite, equip, compositeHeatingMaxVoltage)
                fanMinVoltage.currentVal = getDefault(equip.analog3MinFan, equip, fanMinVoltage)
                fanMaxVoltage.currentVal = getDefault(equip.analog3MaxFan, equip, fanMaxVoltage)
                damperPosMinVoltage.currentVal = getDefault(equip.analog3DamperMinPos, equip, damperPosMinVoltage)
                damperPosMaxVoltage.currentVal = getDefault(equip.analog3DamperMaxPos, equip, damperPosMaxVoltage)
            }

            analog4MinMaxVoltage.apply {
                staticPressureMinVoltage.currentVal = getDefault(equip.analog4MinStaticPressure, equip, staticPressureMinVoltage)
                staticPressureMaxVoltage.currentVal = getDefault(equip.analog4MaxStaticPressure, equip, staticPressureMaxVoltage)
                satCoolingMinVoltage.currentVal = getDefault(equip.analog4MinSatCooling, equip, satCoolingMinVoltage)
                satCoolingMaxVoltage.currentVal = getDefault(equip.analog4MaxSatCooling, equip, satCoolingMaxVoltage)
                satHeatingMinVoltage.currentVal = getDefault(equip.analog4MinSatHeating, equip, satHeatingMinVoltage)
                satHeatingMaxVoltage.currentVal = getDefault(equip.analog4MaxSatHeating, equip, satHeatingMaxVoltage)
                heatingMinVoltage.currentVal = getDefault(equip.analog4MinHeating, equip, heatingMinVoltage)
                heatingMaxVoltage.currentVal = getDefault(equip.analog4MaxHeating, equip, heatingMaxVoltage)
                coolingMinVoltage.currentVal = getDefault(equip.analog4MinCooling, equip, coolingMinVoltage)
                coolingMaxVoltage.currentVal = getDefault(equip.analog4MaxCooling, equip, coolingMaxVoltage)
                compositeCoolingMinVoltage.currentVal = getDefault(equip.analog4MinCoolingComposite, equip, compositeCoolingMinVoltage)
                compositeCoolingMaxVoltage.currentVal = getDefault(equip.analog4MaxCoolingComposite, equip, compositeCoolingMaxVoltage)
                compositeHeatingMinVoltage.currentVal = getDefault(equip.analog4MinHeatingComposite, equip, compositeHeatingMinVoltage)
                compositeHeatingMaxVoltage.currentVal = getDefault(equip.analog4MaxHeatingComposite, equip, compositeHeatingMaxVoltage)
                fanMinVoltage.currentVal = getDefault(equip.analog4MinFan, equip, fanMinVoltage)
                fanMaxVoltage.currentVal = getDefault(equip.analog4MaxFan, equip, fanMaxVoltage)
                damperPosMinVoltage.currentVal = getDefault(equip.analog4DamperMinPos, equip, damperPosMinVoltage)
                damperPosMaxVoltage.currentVal = getDefault(equip.analog4DamperMaxPos, equip, damperPosMaxVoltage)
            }
        }
    }

    /**
     * Function to get the point value if config exist else return the current value model default value
     */
    private fun getDefault(point: Point, equip: VavAdvancedHybridSystemEquip, valueConfig: ValueConfig): Double {
        return if(Domain.readPointForEquip(point.domainName,equip.equipRef).isEmpty())
            valueConfig.currentVal
        else
            point.readDefaultVal()
    }

    private fun getActiveDynamicConfigs(equip: ConnectModuleEquip) {
        connectConfiguration.apply {
            co2Target.currentVal = getDefault(equip.co2Target, equip, co2Target)
            co2Threshold.currentVal = getDefault(equip.co2Threshold, equip, co2Threshold)
            damperOpeningRate.currentVal = getDefault(equip.co2DamperOpeningRate, equip, damperOpeningRate)

            analog1MinMaxVoltage.apply {
                heatingMinVoltage.currentVal = getDefault(equip.analog1MinHeating, equip, heatingMinVoltage)
                heatingMaxVoltage.currentVal = getDefault(equip.analog1MaxHeating, equip, heatingMaxVoltage)
                coolingMinVoltage.currentVal = getDefault(equip.analog1MinCooling, equip, coolingMinVoltage)
                coolingMaxVoltage.currentVal = getDefault(equip.analog1MaxCooling, equip, coolingMaxVoltage)
                compositeCoolingMinVoltage.currentVal = getDefault(equip.analog1MinCoolingComposite, equip, compositeCoolingMinVoltage)
                compositeCoolingMaxVoltage.currentVal = getDefault(equip.analog1MaxCoolingComposite, equip, compositeCoolingMaxVoltage)
                compositeHeatingMinVoltage.currentVal = getDefault(equip.analog1MinHeatingComposite, equip, compositeHeatingMinVoltage)
                compositeHeatingMaxVoltage.currentVal = getDefault(equip.analog1MaxHeatingComposite, equip, compositeHeatingMaxVoltage)
                fanMinVoltage.currentVal = getDefault(equip.analog1MinFan, equip, fanMinVoltage)
                fanMaxVoltage.currentVal = getDefault(equip.analog1MaxFan, equip, fanMaxVoltage)
                damperPosMinVoltage.currentVal = getDefault(equip.analog1MinDamperPos, equip, damperPosMinVoltage)
                damperPosMaxVoltage.currentVal = getDefault(equip.analog1MaxDamperPos, equip, damperPosMaxVoltage)
            }
            analog2MinMaxVoltage.apply {
                heatingMinVoltage.currentVal = getDefault(equip.analog2MinHeating, equip, heatingMinVoltage)
                heatingMaxVoltage.currentVal = getDefault(equip.analog2MaxHeating, equip, heatingMaxVoltage)
                coolingMinVoltage.currentVal = getDefault(equip.analog2MinCooling, equip, coolingMinVoltage)
                coolingMaxVoltage.currentVal = getDefault(equip.analog2MaxCooling, equip, coolingMaxVoltage)
                compositeCoolingMinVoltage.currentVal = getDefault(equip.analog2MinCoolingComposite, equip, compositeCoolingMinVoltage)
                compositeCoolingMaxVoltage.currentVal = getDefault(equip.analog2MaxCoolingComposite, equip, compositeCoolingMaxVoltage)
                compositeHeatingMinVoltage.currentVal = getDefault(equip.analog2MinHeatingComposite, equip, compositeHeatingMinVoltage)
                compositeHeatingMaxVoltage.currentVal = getDefault(equip.analog2MaxHeatingComposite, equip, compositeHeatingMaxVoltage)
                fanMinVoltage.currentVal = getDefault(equip.analog2MinFan, equip, fanMinVoltage)
                fanMaxVoltage.currentVal = getDefault(equip.analog2MaxFan, equip, fanMaxVoltage)
                damperPosMinVoltage.currentVal = getDefault(equip.analog2MinDamperPos, equip, damperPosMinVoltage)
                damperPosMaxVoltage.currentVal = getDefault(equip.analog2MaxDamperPos, equip, damperPosMaxVoltage)
            }
            analog3MinMaxVoltage.apply {
                heatingMinVoltage.currentVal = getDefault(equip.analog3MinHeating, equip, heatingMinVoltage)
                heatingMaxVoltage.currentVal = getDefault(equip.analog3MaxHeating, equip, heatingMaxVoltage)
                coolingMinVoltage.currentVal = getDefault(equip.analog3MinCooling, equip, coolingMinVoltage)
                coolingMaxVoltage.currentVal = getDefault(equip.analog3MaxCooling, equip, coolingMaxVoltage)
                compositeCoolingMinVoltage.currentVal = getDefault(equip.analog3MinCoolingComposite, equip, compositeCoolingMinVoltage)
                compositeCoolingMaxVoltage.currentVal = getDefault(equip.analog3MaxCoolingComposite, equip, compositeCoolingMaxVoltage)
                compositeHeatingMinVoltage.currentVal = getDefault(equip.analog3MinHeatingComposite, equip, compositeHeatingMinVoltage)
                compositeHeatingMaxVoltage.currentVal = getDefault(equip.analog3MaxHeatingComposite, equip, compositeHeatingMaxVoltage)
                fanMinVoltage.currentVal = getDefault(equip.analog3MinFan, equip, fanMinVoltage)
                fanMaxVoltage.currentVal = getDefault(equip.analog3MaxFan, equip, fanMaxVoltage)
                damperPosMinVoltage.currentVal = getDefault(equip.analog3MinDamperPos, equip, damperPosMinVoltage)
                damperPosMaxVoltage.currentVal = getDefault(equip.analog3MaxDamperPos, equip, damperPosMaxVoltage)
            }
            analog4MinMaxVoltage.apply {
                heatingMinVoltage.currentVal = getDefault(equip.analog4MinHeating, equip, heatingMinVoltage)
                heatingMaxVoltage.currentVal = getDefault(equip.analog4MaxHeating, equip, heatingMaxVoltage)
                coolingMinVoltage.currentVal = getDefault(equip.analog4MinCooling, equip, coolingMinVoltage)
                coolingMaxVoltage.currentVal = getDefault(equip.analog4MaxCooling, equip, coolingMaxVoltage)
                compositeCoolingMinVoltage.currentVal = getDefault(equip.analog4MinCoolingComposite, equip, compositeCoolingMinVoltage)
                compositeCoolingMaxVoltage.currentVal = getDefault(equip.analog4MaxCoolingComposite, equip, compositeCoolingMaxVoltage)
                compositeHeatingMinVoltage.currentVal = getDefault(equip.analog4MinHeatingComposite, equip, compositeHeatingMinVoltage)
                compositeHeatingMaxVoltage.currentVal = getDefault(equip.analog4MaxHeatingComposite, equip, compositeHeatingMaxVoltage)
                fanMinVoltage.currentVal = getDefault(equip.analog4MinFan, equip, fanMinVoltage)
                fanMaxVoltage.currentVal = getDefault(equip.analog4MaxFan, equip, fanMaxVoltage)
                damperPosMinVoltage.currentVal = getDefault(equip.analog4MinDamperPos, equip, damperPosMinVoltage)
                damperPosMaxVoltage.currentVal = getDefault(equip.analog4MaxDamperPos, equip, damperPosMaxVoltage)
            }
        }
    }

    /**
     * Function to get the point value if config exist else return the current value model default value
     */
    private fun getDefault(point: Point, equip: ConnectModuleEquip, valueConfig: ValueConfig): Double {
        return if(Domain.readPointForEquip(point.domainName,equip.equipRef).isEmpty())
            valueConfig.currentVal
        else
            point.readDefaultVal()
    }

}


