package a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon

import a75f.io.domain.HyperStatSplitEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstatsplit.profiles.HyperStatSplitProfileConfiguration
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags
import java.util.Collections

class HyperStatSplitCpuProfileConfiguration (nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String, profileType : ProfileType, model : SeventyFiveFProfileDirective) :
    HyperStatSplitProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType, model) {

    lateinit var analogOut1Voltage: AnalogOutVoltage
    lateinit var analogOut2Voltage: AnalogOutVoltage
    lateinit var analogOut3Voltage: AnalogOutVoltage
    lateinit var analogOut4Voltage: AnalogOutVoltage
    lateinit var stagedFanVoltages: StagedFanVoltages

    fun getActiveConfiguration() : HyperStatSplitCpuProfileConfiguration {
        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val hssEquip = HyperStatSplitEquip(equip[Tags.ID].toString())

        getDefaultConfiguration()
        getActiveEnableConfigs(hssEquip)
        getActiveAssociationConfigs(hssEquip)
        getGenericZoneConfigs(hssEquip)
        getActiveDynamicConfigs(hssEquip)

        isDefault = false
        return this
    }

    private fun getActiveEnableConfigs(hssEquip : HyperStatSplitEquip) {
        apply {
            address0Enabled.enabled = hssEquip.sensorBusAddress0Enable.readDefaultVal() > 0
            sensorBusPressureEnable.enabled = hssEquip.sensorBusPressureEnable.readDefaultVal() > 0
            address1Enabled.enabled = hssEquip.sensorBusAddress1Enable.readDefaultVal() > 0
            address2Enabled.enabled = hssEquip.sensorBusAddress2Enable.readDefaultVal() > 0

            relay1Enabled.enabled = hssEquip.relay1OutputEnable.readDefaultVal() > 0.0
            relay2Enabled.enabled = hssEquip.relay2OutputEnable.readDefaultVal() > 0.0
            relay3Enabled.enabled = hssEquip.relay3OutputEnable.readDefaultVal() > 0.0
            relay4Enabled.enabled = hssEquip.relay4OutputEnable.readDefaultVal() > 0.0
            relay5Enabled.enabled = hssEquip.relay5OutputEnable.readDefaultVal() > 0.0
            relay6Enabled.enabled = hssEquip.relay6OutputEnable.readDefaultVal() > 0.0
            relay7Enabled.enabled = hssEquip.relay7OutputEnable.readDefaultVal() > 0.0
            relay8Enabled.enabled = hssEquip.relay8OutputEnable.readDefaultVal() > 0.0

            analogOut1Enabled.enabled = hssEquip.analog1OutputEnable.readDefaultVal() > 0.0
            analogOut2Enabled.enabled = hssEquip.analog2OutputEnable.readDefaultVal() > 0.0
            analogOut3Enabled.enabled = hssEquip.analog3OutputEnable.readDefaultVal() > 0.0
            analogOut4Enabled.enabled = hssEquip.analog4OutputEnable.readDefaultVal() > 0.0

            universal1InEnabled.enabled = hssEquip.universalIn1Enable.readDefaultVal() > 0.0
            universal2InEnabled.enabled = hssEquip.universalIn2Enable.readDefaultVal() > 0.0
            universal3InEnabled.enabled = hssEquip.universalIn3Enable.readDefaultVal() > 0.0
            universal4InEnabled.enabled = hssEquip.universalIn4Enable.readDefaultVal() > 0.0
            universal5InEnabled.enabled = hssEquip.universalIn5Enable.readDefaultVal() > 0.0
            universal6InEnabled.enabled = hssEquip.universalIn6Enable.readDefaultVal() > 0.0
            universal7InEnabled.enabled = hssEquip.universalIn7Enable.readDefaultVal() > 0.0
            universal8InEnabled.enabled = hssEquip.universalIn8Enable.readDefaultVal() > 0.0
        }
    }

    private fun getActiveAssociationConfigs(hssEquip: HyperStatSplitEquip) {
        getSensorAssociationConfigs(hssEquip)

        apply {
            if (relay1Enabled.enabled) {
                relay1Association.associationVal = hssEquip.relay1OutputAssociation.readDefaultVal().toInt()
            }
            if (relay2Enabled.enabled) {
                relay2Association.associationVal = hssEquip.relay2OutputAssociation.readDefaultVal().toInt()
            }
            if (relay3Enabled.enabled) {
                relay3Association.associationVal = hssEquip.relay3OutputAssociation.readDefaultVal().toInt()
            }
            if (relay4Enabled.enabled) {
                relay4Association.associationVal = hssEquip.relay4OutputAssociation.readDefaultVal().toInt()
            }
            if (relay5Enabled.enabled) {
                relay5Association.associationVal = hssEquip.relay5OutputAssociation.readDefaultVal().toInt()
            }
            if (relay6Enabled.enabled) {
                relay6Association.associationVal = hssEquip.relay6OutputAssociation.readDefaultVal().toInt()
            }
            if (relay7Enabled.enabled) {
                relay7Association.associationVal = hssEquip.relay7OutputAssociation.readDefaultVal().toInt()
            }
            if (relay8Enabled.enabled) {
                relay8Association.associationVal = hssEquip.relay8OutputAssociation.readDefaultVal().toInt()
            }

            if (analogOut1Enabled.enabled) {
                analogOut1Association.associationVal = hssEquip.analog1OutputAssociation.readDefaultVal().toInt()
            }
            if (analogOut2Enabled.enabled) {
                analogOut2Association.associationVal = hssEquip.analog2OutputAssociation.readDefaultVal().toInt()
            }
            if (analogOut3Enabled.enabled) {
                analogOut3Association.associationVal = hssEquip.analog3OutputAssociation.readDefaultVal().toInt()
            }
            if (analogOut4Enabled.enabled) {
                analogOut4Association.associationVal = hssEquip.analog4OutputAssociation.readDefaultVal().toInt()
            }

            if (universal1InEnabled.enabled) {
                universal1InAssociation.associationVal = hssEquip.universalIn1Association.readDefaultVal().toInt()
            }
            if (universal2InEnabled.enabled) {
                universal2InAssociation.associationVal = hssEquip.universalIn2Association.readDefaultVal().toInt()
            }
            if (universal3InEnabled.enabled) {
                universal3InAssociation.associationVal = hssEquip.universalIn3Association.readDefaultVal().toInt()
            }
            if (universal4InEnabled.enabled) {
                universal4InAssociation.associationVal = hssEquip.universalIn4Association.readDefaultVal().toInt()
            }
            if (universal5InEnabled.enabled) {
                universal5InAssociation.associationVal = hssEquip.universalIn5Association.readDefaultVal().toInt()
            }
            if (universal6InEnabled.enabled) {
                universal6InAssociation.associationVal = hssEquip.universalIn6Association.readDefaultVal().toInt()
            }
            if (universal7InEnabled.enabled) {
                universal7InAssociation.associationVal = hssEquip.universalIn7Association.readDefaultVal().toInt()
            }
            if (universal8InEnabled.enabled) {
                universal8InAssociation.associationVal = hssEquip.universalIn8Association.readDefaultVal().toInt()
            }
        }
    }

    private fun getSensorAssociationConfigs(hssEquip: HyperStatSplitEquip) {
        apply {

            if (address0Enabled.enabled) {
                address0SensorAssociation.temperatureAssociation.associationVal = hssEquip.temperatureSensorBusAdd0.readDefaultVal().toInt()
                address0SensorAssociation.humidityAssociation.associationVal = hssEquip.humiditySensorBusAdd0.readDefaultVal().toInt()
            }

            if (sensorBusPressureEnable.enabled) {
                pressureAddress0SensorAssociation.associationVal = hssEquip.pressureSensorBusAdd0.readDefaultVal().toInt()
            }

            if (address1Enabled.enabled) {
                address1SensorAssociation.temperatureAssociation.associationVal = hssEquip.temperatureSensorBusAdd1.readDefaultVal().toInt()
                address1SensorAssociation.humidityAssociation.associationVal = hssEquip.humiditySensorBusAdd1.readDefaultVal().toInt()
            }

            if (address2Enabled.enabled) {
                address2SensorAssociation.temperatureAssociation.associationVal = hssEquip.temperatureSensorBusAdd2.readDefaultVal().toInt()
                address2SensorAssociation.humidityAssociation.associationVal = hssEquip.humiditySensorBusAdd2.readDefaultVal().toInt()
            }

        }
    }

    private fun getGenericZoneConfigs(hssEquip: HyperStatSplitEquip) {
        apply {
            temperatureOffset.currentVal = hssEquip.temperatureOffset.readDefaultVal()

            autoForceOccupied.enabled = hssEquip.autoForceOccupied.readDefaultVal() > 0.0
            autoAway.enabled = hssEquip.autoAway.readDefaultVal() > 0.0
            prePurge.enabled = hssEquip.prePurgeEnable.readDefaultVal() > 0.0

            zoneCO2Threshold.currentVal = hssEquip.co2Threshold.readDefaultVal()
            zoneCO2Target.currentVal = hssEquip.co2Target.readDefaultVal()

            zonePM2p5Target.currentVal = hssEquip.pm25Target.readDefaultVal()

            if (prePurge.enabled) prePurgeOutsideDamperOpen.currentVal = hssEquip.prePurgeOutsideDamperOpen.readDefaultVal()

            displayHumidity.enabled = hssEquip.enableHumidityDisplay.readDefaultVal() > 0.0
            displayCO2.enabled = hssEquip.enableCO2Display.readDefaultVal() > 0.0
            displayPM2p5.enabled = hssEquip.enablePm25Display.readDefaultVal() > 0.0

            disableTouch.enabled = hssEquip.disableTouch.readDefaultVal() > 0.0
            enableBrightness.enabled = hssEquip.enableBrightness.readDefaultVal() > 0.0

            enableOutsideAirOptimization.enabled = hssEquip.enableOutsideAirOptimization.readDefaultVal() > 0.0
        }
    }

    private fun getActiveDynamicConfigs(hssEquip: HyperStatSplitEquip) {
        apply {

            analogOut1Voltage.coolingMinVoltage.currentVal = getDefault(hssEquip.analog1AtMinCooling, hssEquip, analogOut1Voltage.coolingMinVoltage)
            analogOut1Voltage.coolingMaxVoltage.currentVal = getDefault(hssEquip.analog1AtMaxCooling, hssEquip, analogOut1Voltage.coolingMaxVoltage)
            analogOut1Voltage.heatingMinVoltage.currentVal = getDefault(hssEquip.analog1AtMinHeating, hssEquip, analogOut1Voltage.heatingMinVoltage)
            analogOut1Voltage.heatingMaxVoltage.currentVal = getDefault(hssEquip.analog1AtMaxHeating, hssEquip, analogOut1Voltage.heatingMaxVoltage)
            analogOut1Voltage.oaoDamperMinVoltage.currentVal = getDefault(hssEquip.analog1AtMinOAODamper, hssEquip, analogOut1Voltage.oaoDamperMinVoltage)
            analogOut1Voltage.oaoDamperMaxVoltage.currentVal = getDefault(hssEquip.analog1AtMaxOAODamper, hssEquip, analogOut1Voltage.oaoDamperMaxVoltage)
            analogOut1Voltage.returnDamperMinVoltage.currentVal = getDefault(hssEquip.analog1AtMinReturnDamper, hssEquip, analogOut1Voltage.returnDamperMinVoltage)
            analogOut1Voltage.returnDamperMaxVoltage.currentVal = getDefault(hssEquip.analog1AtMaxReturnDamper, hssEquip, analogOut1Voltage.returnDamperMaxVoltage)
            analogOut1Voltage.linearFanMinVoltage.currentVal = getDefault(hssEquip.analog1AtMinLinearFanSpeed, hssEquip, analogOut1Voltage.linearFanMinVoltage)
            analogOut1Voltage.linearFanMaxVoltage.currentVal = getDefault(hssEquip.analog1AtMaxLinearFanSpeed, hssEquip, analogOut1Voltage.linearFanMaxVoltage)
            analogOut1Voltage.linearFanAtFanLow.currentVal = getDefault(hssEquip.analog1FanLow, hssEquip, analogOut1Voltage.linearFanAtFanLow)
            analogOut1Voltage.linearFanAtFanMedium.currentVal = getDefault(hssEquip.analog1FanMedium, hssEquip, analogOut1Voltage.linearFanAtFanMedium)
            analogOut1Voltage.linearFanAtFanHigh.currentVal = getDefault(hssEquip.analog1FanHigh, hssEquip, analogOut1Voltage.linearFanAtFanHigh)
            
            analogOut2Voltage.coolingMinVoltage.currentVal = getDefault(hssEquip.analog2AtMinCooling, hssEquip, analogOut2Voltage.coolingMinVoltage)
            analogOut2Voltage.coolingMaxVoltage.currentVal = getDefault(hssEquip.analog2AtMaxCooling, hssEquip, analogOut2Voltage.coolingMaxVoltage)
            analogOut2Voltage.heatingMinVoltage.currentVal = getDefault(hssEquip.analog2AtMinHeating, hssEquip, analogOut2Voltage.heatingMinVoltage)
            analogOut2Voltage.heatingMaxVoltage.currentVal = getDefault(hssEquip.analog2AtMaxHeating, hssEquip, analogOut2Voltage.heatingMaxVoltage)
            analogOut2Voltage.oaoDamperMinVoltage.currentVal = getDefault(hssEquip.analog2AtMinOAODamper, hssEquip, analogOut2Voltage.oaoDamperMinVoltage)
            analogOut2Voltage.oaoDamperMaxVoltage.currentVal = getDefault(hssEquip.analog2AtMaxOAODamper, hssEquip, analogOut2Voltage.oaoDamperMaxVoltage)
            analogOut2Voltage.returnDamperMinVoltage.currentVal = getDefault(hssEquip.analog2AtMinReturnDamper, hssEquip, analogOut2Voltage.returnDamperMinVoltage)
            analogOut2Voltage.returnDamperMaxVoltage.currentVal = getDefault(hssEquip.analog2AtMaxReturnDamper, hssEquip, analogOut2Voltage.returnDamperMaxVoltage)
            analogOut2Voltage.linearFanMinVoltage.currentVal = getDefault(hssEquip.analog2AtMinLinearFanSpeed, hssEquip, analogOut2Voltage.linearFanMinVoltage)
            analogOut2Voltage.linearFanMaxVoltage.currentVal = getDefault(hssEquip.analog2AtMaxLinearFanSpeed, hssEquip, analogOut2Voltage.linearFanMaxVoltage)
            analogOut2Voltage.linearFanAtFanLow.currentVal = getDefault(hssEquip.analog2FanLow, hssEquip, analogOut2Voltage.linearFanAtFanLow)
            analogOut2Voltage.linearFanAtFanMedium.currentVal = getDefault(hssEquip.analog2FanMedium, hssEquip, analogOut2Voltage.linearFanAtFanMedium)
            analogOut2Voltage.linearFanAtFanHigh.currentVal = getDefault(hssEquip.analog2FanHigh, hssEquip, analogOut2Voltage.linearFanAtFanHigh)
            
            analogOut3Voltage.coolingMinVoltage.currentVal = getDefault(hssEquip.analog3AtMinCooling, hssEquip, analogOut3Voltage.coolingMinVoltage)
            analogOut3Voltage.coolingMaxVoltage.currentVal = getDefault(hssEquip.analog3AtMaxCooling, hssEquip, analogOut3Voltage.coolingMaxVoltage)
            analogOut3Voltage.heatingMinVoltage.currentVal = getDefault(hssEquip.analog3AtMinHeating, hssEquip, analogOut3Voltage.heatingMinVoltage)
            analogOut3Voltage.heatingMaxVoltage.currentVal = getDefault(hssEquip.analog3AtMaxHeating, hssEquip, analogOut3Voltage.heatingMaxVoltage)
            analogOut3Voltage.oaoDamperMinVoltage.currentVal = getDefault(hssEquip.analog3AtMinOAODamper, hssEquip, analogOut3Voltage.oaoDamperMinVoltage)
            analogOut3Voltage.oaoDamperMaxVoltage.currentVal = getDefault(hssEquip.analog3AtMaxOAODamper, hssEquip, analogOut3Voltage.oaoDamperMaxVoltage)
            analogOut3Voltage.returnDamperMinVoltage.currentVal = getDefault(hssEquip.analog3AtMinReturnDamper, hssEquip, analogOut3Voltage.returnDamperMinVoltage)
            analogOut3Voltage.returnDamperMaxVoltage.currentVal = getDefault(hssEquip.analog3AtMaxReturnDamper, hssEquip, analogOut3Voltage.returnDamperMaxVoltage)
            analogOut3Voltage.linearFanMinVoltage.currentVal = getDefault(hssEquip.analog3AtMinLinearFanSpeed, hssEquip, analogOut3Voltage.linearFanMinVoltage)
            analogOut3Voltage.linearFanMaxVoltage.currentVal = getDefault(hssEquip.analog3AtMaxLinearFanSpeed, hssEquip, analogOut3Voltage.linearFanMaxVoltage)
            analogOut3Voltage.linearFanAtFanLow.currentVal = getDefault(hssEquip.analog3FanLow, hssEquip, analogOut3Voltage.linearFanAtFanLow)
            analogOut3Voltage.linearFanAtFanMedium.currentVal = getDefault(hssEquip.analog3FanMedium, hssEquip, analogOut3Voltage.linearFanAtFanMedium)
            analogOut3Voltage.linearFanAtFanHigh.currentVal = getDefault(hssEquip.analog3FanHigh, hssEquip, analogOut3Voltage.linearFanAtFanHigh)
            
            analogOut4Voltage.coolingMinVoltage.currentVal = getDefault(hssEquip.analog4AtMinCooling, hssEquip, analogOut4Voltage.coolingMinVoltage)
            analogOut4Voltage.coolingMaxVoltage.currentVal = getDefault(hssEquip.analog4AtMaxCooling, hssEquip, analogOut4Voltage.coolingMaxVoltage)
            analogOut4Voltage.heatingMinVoltage.currentVal = getDefault(hssEquip.analog4AtMinHeating, hssEquip, analogOut4Voltage.heatingMinVoltage)
            analogOut4Voltage.heatingMaxVoltage.currentVal = getDefault(hssEquip.analog4AtMaxHeating, hssEquip, analogOut4Voltage.heatingMaxVoltage)
            analogOut4Voltage.oaoDamperMinVoltage.currentVal = getDefault(hssEquip.analog4AtMinOAODamper, hssEquip, analogOut4Voltage.oaoDamperMinVoltage)
            analogOut4Voltage.oaoDamperMaxVoltage.currentVal = getDefault(hssEquip.analog4AtMaxOAODamper, hssEquip, analogOut4Voltage.oaoDamperMaxVoltage)
            analogOut4Voltage.returnDamperMinVoltage.currentVal = getDefault(hssEquip.analog4AtMinReturnDamper, hssEquip, analogOut4Voltage.returnDamperMinVoltage)
            analogOut4Voltage.returnDamperMaxVoltage.currentVal = getDefault(hssEquip.analog4AtMaxReturnDamper, hssEquip, analogOut4Voltage.returnDamperMaxVoltage)
            analogOut4Voltage.linearFanMinVoltage.currentVal = getDefault(hssEquip.analog4AtMinLinearFanSpeed, hssEquip, analogOut4Voltage.linearFanMinVoltage)
            analogOut4Voltage.linearFanMaxVoltage.currentVal = getDefault(hssEquip.analog4AtMaxLinearFanSpeed, hssEquip, analogOut4Voltage.linearFanMaxVoltage)
            analogOut4Voltage.linearFanAtFanLow.currentVal = getDefault(hssEquip.analog4FanLow, hssEquip, analogOut4Voltage.linearFanAtFanLow)
            analogOut4Voltage.linearFanAtFanMedium.currentVal = getDefault(hssEquip.analog4FanMedium, hssEquip, analogOut4Voltage.linearFanAtFanMedium)
            analogOut4Voltage.linearFanAtFanHigh.currentVal = getDefault(hssEquip.analog4FanHigh, hssEquip, analogOut4Voltage.linearFanAtFanHigh)
            
            stagedFanVoltages.recircVoltage.currentVal = getDefault(hssEquip.fanOutRecirculate, hssEquip, stagedFanVoltages.recircVoltage)
            stagedFanVoltages.economizerVoltage.currentVal = getDefault(hssEquip.fanOutEconomizer, hssEquip, stagedFanVoltages.economizerVoltage)
            stagedFanVoltages.heatStage1Voltage.currentVal = getDefault(hssEquip.fanOutHeatingStage1, hssEquip, stagedFanVoltages.heatStage1Voltage)
            stagedFanVoltages.heatStage2Voltage.currentVal = getDefault(hssEquip.fanOutHeatingStage2, hssEquip, stagedFanVoltages.heatStage2Voltage)
            stagedFanVoltages.heatStage3Voltage.currentVal = getDefault(hssEquip.fanOutHeatingStage3, hssEquip, stagedFanVoltages.heatStage3Voltage)
            stagedFanVoltages.coolStage1Voltage.currentVal = getDefault(hssEquip.fanOutCoolingStage1, hssEquip, stagedFanVoltages.coolStage1Voltage)
            stagedFanVoltages.coolStage2Voltage.currentVal = getDefault(hssEquip.fanOutCoolingStage2, hssEquip, stagedFanVoltages.coolStage2Voltage)
            stagedFanVoltages.coolStage3Voltage.currentVal = getDefault(hssEquip.fanOutCoolingStage3, hssEquip, stagedFanVoltages.coolStage3Voltage)

            outsideDamperMinOpenDuringRecirc.currentVal = getDefault(hssEquip.outsideDamperMinOpenDuringRecirculation, hssEquip, outsideDamperMinOpenDuringRecirc)
            outsideDamperMinOpenDuringConditioning.currentVal = getDefault(hssEquip.outsideDamperMinOpenDuringConditioning, hssEquip, outsideDamperMinOpenDuringConditioning)
            outsideDamperMinOpenDuringFanLow.currentVal = getDefault(hssEquip.outsideDamperMinOpenDuringFanLow, hssEquip, outsideDamperMinOpenDuringFanLow)
            outsideDamperMinOpenDuringFanMedium.currentVal = getDefault(hssEquip.outsideDamperMinOpenDuringFanMedium, hssEquip, outsideDamperMinOpenDuringFanMedium)
            outsideDamperMinOpenDuringFanHigh.currentVal = getDefault(hssEquip.outsideDamperMinOpenDuringFanHigh, hssEquip, outsideDamperMinOpenDuringFanHigh)

            exhaustFanStage1Threshold.currentVal = getDefault(hssEquip.exhaustFanStage1Threshold, hssEquip, exhaustFanStage1Threshold)
            exhaustFanStage2Threshold.currentVal = getDefault(hssEquip.exhaustFanStage2Threshold, hssEquip, exhaustFanStage2Threshold)
            exhaustFanHysteresis.currentVal = getDefault(hssEquip.exhaustFanHysteresis, hssEquip, exhaustFanHysteresis)

            zoneCO2DamperOpeningRate.currentVal = getDefault(hssEquip.co2DamperOpeningRate, hssEquip, zoneCO2DamperOpeningRate)
        }
    }

    /**
     * Function to get the point value if config exist else return the current value model default value
     */
    private fun getDefault(point: Point, equip: HyperStatSplitEquip, valueConfig: ValueConfig): Double {
        return if(Domain.readPointForEquip(point.domainName,equip.equipRef).isEmpty())
            valueConfig.currentVal
        else
            point.readDefaultVal()
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return super.getValueConfigs().toMutableList().apply {
            Collections.addAll(addValueConfig(analogOut1Voltage, this))
            Collections.addAll(addValueConfig(analogOut2Voltage, this))
            Collections.addAll(addValueConfig(analogOut3Voltage, this))
            Collections.addAll(addValueConfig(analogOut4Voltage, this))
            Collections.addAll(addValueConfig(stagedFanVoltages, this))
        }
    }

    /**
     * Add all value configs to the list
     */
    private fun addValueConfig(
        analogConfig: AnalogOutVoltage, list: MutableList<ValueConfig>
    ): MutableList<ValueConfig> {
        return list.apply {
            add(analogConfig.coolingMinVoltage)
            add(analogConfig.coolingMaxVoltage)
            add(analogConfig.heatingMinVoltage)
            add(analogConfig.heatingMaxVoltage)
            add(analogConfig.oaoDamperMinVoltage)
            add(analogConfig.oaoDamperMaxVoltage)
            add(analogConfig.returnDamperMinVoltage)
            add(analogConfig.returnDamperMaxVoltage)
            add(analogConfig.linearFanMinVoltage)
            add(analogConfig.linearFanMaxVoltage)
            add(analogConfig.linearFanAtFanLow)
            add(analogConfig.linearFanAtFanMedium)
            add(analogConfig.linearFanAtFanHigh)
        }
    }

    /**
     * Add all value configs to the list
     */
    private fun addValueConfig(
        stagedFanConfig: StagedFanVoltages, list: MutableList<ValueConfig>
    ): MutableList<ValueConfig> {
        return list.apply {
            add(stagedFanConfig.recircVoltage)
            add(stagedFanConfig.economizerVoltage)
            add(stagedFanConfig.heatStage1Voltage)
            add(stagedFanConfig.coolStage1Voltage)
            add(stagedFanConfig.heatStage2Voltage)
            add(stagedFanConfig.coolStage2Voltage)
            add(stagedFanConfig.heatStage3Voltage)
            add(stagedFanConfig.coolStage3Voltage)
        }
    }

    override fun getDefaultConfiguration(): HyperStatSplitCpuProfileConfiguration {
        super.getDefaultConfiguration()
        getAnalogOut1Voltage()
        getAnalogOut2Voltage()
        getAnalogOut3Voltage()
        getAnalogOut4Voltage()
        getStagedFanVoltages()
        return this
    }

    /**
     * Get the default enable config for the domain name
     */
    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            Collections.addAll(addValueConfig(analogOut1Voltage, this))
            Collections.addAll(addValueConfig(analogOut2Voltage, this))
            Collections.addAll(addValueConfig(analogOut3Voltage, this))
            Collections.addAll(addValueConfig(analogOut4Voltage, this))
            Collections.addAll(addValueConfig(stagedFanVoltages, this))
        }
    }
    
    private fun getAnalogOut1Voltage() {
        analogOut1Voltage = AnalogOutVoltage(
            coolingMinVoltage = getDefaultValConfig(DomainName.analog1MinCooling, model),
            coolingMaxVoltage = getDefaultValConfig(DomainName.analog1MaxCooling, model),
            heatingMinVoltage = getDefaultValConfig(DomainName.analog1MinHeating, model),
            heatingMaxVoltage = getDefaultValConfig(DomainName.analog1MaxHeating, model),
            oaoDamperMinVoltage = getDefaultValConfig(DomainName.analog1MinOAODamper, model),
            oaoDamperMaxVoltage = getDefaultValConfig(DomainName.analog1MaxOAODamper, model),
            returnDamperMinVoltage = getDefaultValConfig(DomainName.analog1MinReturnDamper, model),
            returnDamperMaxVoltage = getDefaultValConfig(DomainName.analog1MaxReturnDamper, model),
            linearFanMinVoltage = getDefaultValConfig(DomainName.analog1MinLinearFanSpeed, model),
            linearFanMaxVoltage = getDefaultValConfig(DomainName.analog1MaxLinearFanSpeed, model),
            linearFanAtFanLow = getDefaultValConfig(DomainName.analog1FanLow, model),
            linearFanAtFanMedium = getDefaultValConfig(DomainName.analog1FanMedium, model),
            linearFanAtFanHigh = getDefaultValConfig(DomainName.analog1FanHigh, model)
        )
    }

    private fun getAnalogOut2Voltage() {
        analogOut2Voltage = AnalogOutVoltage(
            coolingMinVoltage = getDefaultValConfig(DomainName.analog2MinCooling, model),
            coolingMaxVoltage = getDefaultValConfig(DomainName.analog2MaxCooling, model),
            heatingMinVoltage = getDefaultValConfig(DomainName.analog2MinHeating, model),
            heatingMaxVoltage = getDefaultValConfig(DomainName.analog2MaxHeating, model),
            oaoDamperMinVoltage = getDefaultValConfig(DomainName.analog2MinOAODamper, model),
            oaoDamperMaxVoltage = getDefaultValConfig(DomainName.analog2MaxOAODamper, model),
            returnDamperMinVoltage = getDefaultValConfig(DomainName.analog2MinReturnDamper, model),
            returnDamperMaxVoltage = getDefaultValConfig(DomainName.analog2MaxReturnDamper, model),
            linearFanMinVoltage = getDefaultValConfig(DomainName.analog2MinLinearFanSpeed, model),
            linearFanMaxVoltage = getDefaultValConfig(DomainName.analog2MaxLinearFanSpeed, model),
            linearFanAtFanLow = getDefaultValConfig(DomainName.analog2FanLow, model),
            linearFanAtFanMedium = getDefaultValConfig(DomainName.analog2FanMedium, model),
            linearFanAtFanHigh = getDefaultValConfig(DomainName.analog2FanHigh, model)
        )
    }

    private fun getAnalogOut3Voltage() {
        analogOut3Voltage = AnalogOutVoltage(
            coolingMinVoltage = getDefaultValConfig(DomainName.analog3MinCooling, model),
            coolingMaxVoltage = getDefaultValConfig(DomainName.analog3MaxCooling, model),
            heatingMinVoltage = getDefaultValConfig(DomainName.analog3MinHeating, model),
            heatingMaxVoltage = getDefaultValConfig(DomainName.analog3MaxHeating, model),
            oaoDamperMinVoltage = getDefaultValConfig(DomainName.analog3MinOAODamper, model),
            oaoDamperMaxVoltage = getDefaultValConfig(DomainName.analog3MaxOAODamper, model),
            returnDamperMinVoltage = getDefaultValConfig(DomainName.analog3MinReturnDamper, model),
            returnDamperMaxVoltage = getDefaultValConfig(DomainName.analog3MaxReturnDamper, model),
            linearFanMinVoltage = getDefaultValConfig(DomainName.analog3MinLinearFanSpeed, model),
            linearFanMaxVoltage = getDefaultValConfig(DomainName.analog3MaxLinearFanSpeed, model),
            linearFanAtFanLow = getDefaultValConfig(DomainName.analog3FanLow, model),
            linearFanAtFanMedium = getDefaultValConfig(DomainName.analog3FanMedium, model),
            linearFanAtFanHigh = getDefaultValConfig(DomainName.analog3FanHigh, model)
        )
    }

    private fun getAnalogOut4Voltage() {
        analogOut4Voltage = AnalogOutVoltage(
            coolingMinVoltage = getDefaultValConfig(DomainName.analog4MinCooling, model),
            coolingMaxVoltage = getDefaultValConfig(DomainName.analog4MaxCooling, model),
            heatingMinVoltage = getDefaultValConfig(DomainName.analog4MinHeating, model),
            heatingMaxVoltage = getDefaultValConfig(DomainName.analog4MaxHeating, model),
            oaoDamperMinVoltage = getDefaultValConfig(DomainName.analog4MinOAODamper, model),
            oaoDamperMaxVoltage = getDefaultValConfig(DomainName.analog4MaxOAODamper, model),
            returnDamperMinVoltage = getDefaultValConfig(DomainName.analog4MinReturnDamper, model),
            returnDamperMaxVoltage = getDefaultValConfig(DomainName.analog4MaxReturnDamper, model),
            linearFanMinVoltage = getDefaultValConfig(DomainName.analog4MinLinearFanSpeed, model),
            linearFanMaxVoltage = getDefaultValConfig(DomainName.analog4MaxLinearFanSpeed, model),
            linearFanAtFanLow = getDefaultValConfig(DomainName.analog4FanLow, model),
            linearFanAtFanMedium = getDefaultValConfig(DomainName.analog4FanMedium, model),
            linearFanAtFanHigh = getDefaultValConfig(DomainName.analog4FanHigh, model)
        )
    }

    private fun getStagedFanVoltages() {
        stagedFanVoltages = StagedFanVoltages(
            recircVoltage = getDefaultValConfig(DomainName.fanOutRecirculate, model),
            economizerVoltage = getDefaultValConfig(DomainName.fanOutEconomizer, model),
            heatStage1Voltage = getDefaultValConfig(DomainName.fanOutHeatingStage1, model),
            coolStage1Voltage = getDefaultValConfig(DomainName.fanOutCoolingStage1, model),
            heatStage2Voltage = getDefaultValConfig(DomainName.fanOutHeatingStage2, model),
            coolStage2Voltage = getDefaultValConfig(DomainName.fanOutCoolingStage2, model),
            heatStage3Voltage = getDefaultValConfig(DomainName.fanOutHeatingStage3, model),
            coolStage3Voltage = getDefaultValConfig(DomainName.fanOutCoolingStage3, model)
        )
    }

    override fun toString(): String {
        return super.toString() +
                "\nanalogOut1Voltage $analogOut1Voltage" +
                "\nanalogOut2Voltage $analogOut2Voltage" +
                "\nanalogOut3Voltage $analogOut3Voltage" +
                "\nanalogOut4Voltage $analogOut4Voltage"
    }

    override fun analogOut1TypeToString(): String {
        return when (analogOut1Association.associationVal) {
            CpuControlType.COOLING.ordinal -> {
                analogOut1Voltage.coolingMinVoltage.currentVal.toInt().toString() + "-" + analogOut1Voltage.coolingMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.HEATING.ordinal -> {
                analogOut1Voltage.heatingMinVoltage.currentVal.toInt().toString() + "-" + analogOut1Voltage.heatingMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.LINEAR_FAN.ordinal -> {
                analogOut1Voltage.linearFanMinVoltage.currentVal.toInt().toString() + "-" + analogOut1Voltage.linearFanMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.OAO_DAMPER.ordinal -> {
                analogOut1Voltage.oaoDamperMinVoltage.currentVal.toInt().toString() + "-" + analogOut1Voltage.oaoDamperMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.RETURN_DAMPER.ordinal -> {
                analogOut1Voltage.returnDamperMinVoltage.currentVal.toInt().toString() + "-" + analogOut1Voltage.returnDamperMaxVoltage.currentVal.toInt().toString() + "v"
            }
            else -> super.analogOut1TypeToString()
        }
    }

    override fun analogOut2TypeToString(): String {
        return when (analogOut2Association.associationVal) {
            CpuControlType.COOLING.ordinal -> {
                analogOut2Voltage.coolingMinVoltage.currentVal.toInt().toString() + "-" + analogOut2Voltage.coolingMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.HEATING.ordinal -> {
                analogOut2Voltage.heatingMinVoltage.currentVal.toInt().toString() + "-" + analogOut2Voltage.heatingMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.LINEAR_FAN.ordinal -> {
                analogOut2Voltage.linearFanMinVoltage.currentVal.toInt().toString() + "-" + analogOut2Voltage.linearFanMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.OAO_DAMPER.ordinal -> {
                analogOut2Voltage.oaoDamperMinVoltage.currentVal.toInt().toString() + "-" + analogOut2Voltage.oaoDamperMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.RETURN_DAMPER.ordinal -> {
                analogOut2Voltage.returnDamperMinVoltage.currentVal.toInt().toString() + "-" + analogOut2Voltage.returnDamperMaxVoltage.currentVal.toInt().toString() + "v"
            }
            else -> super.analogOut2TypeToString()
        }
    }

    override fun analogOut3TypeToString(): String {
        return when (analogOut3Association.associationVal) {
            CpuControlType.COOLING.ordinal -> {
                analogOut3Voltage.coolingMinVoltage.currentVal.toInt().toString() + "-" + analogOut3Voltage.coolingMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.HEATING.ordinal -> {
                analogOut3Voltage.heatingMinVoltage.currentVal.toInt().toString() + "-" + analogOut3Voltage.heatingMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.LINEAR_FAN.ordinal -> {
                analogOut3Voltage.linearFanMinVoltage.currentVal.toInt().toString() + "-" + analogOut3Voltage.linearFanMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.OAO_DAMPER.ordinal -> {
                analogOut3Voltage.oaoDamperMinVoltage.currentVal.toInt().toString() + "-" + analogOut3Voltage.oaoDamperMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.RETURN_DAMPER.ordinal -> {
                analogOut3Voltage.returnDamperMinVoltage.currentVal.toInt().toString() + "-" + analogOut3Voltage.returnDamperMaxVoltage.currentVal.toInt().toString() + "v"
            }
            else -> super.analogOut3TypeToString()
        }
    }

    override fun analogOut4TypeToString(): String {
        return when (analogOut4Association.associationVal) {
            CpuControlType.COOLING.ordinal -> {
                analogOut4Voltage.coolingMinVoltage.currentVal.toInt().toString() + "-" + analogOut4Voltage.coolingMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.HEATING.ordinal -> {
                analogOut4Voltage.heatingMinVoltage.currentVal.toInt().toString() + "-" + analogOut4Voltage.heatingMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.LINEAR_FAN.ordinal -> {
                analogOut4Voltage.linearFanMinVoltage.currentVal.toInt().toString() + "-" + analogOut4Voltage.linearFanMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.OAO_DAMPER.ordinal -> {
                analogOut4Voltage.oaoDamperMinVoltage.currentVal.toInt().toString() + "-" + analogOut4Voltage.oaoDamperMaxVoltage.currentVal.toInt().toString() + "v"
            }
            CpuControlType.RETURN_DAMPER.ordinal -> {
                analogOut4Voltage.returnDamperMinVoltage.currentVal.toInt().toString() + "-" + analogOut4Voltage.returnDamperMaxVoltage.currentVal.toInt().toString() + "v"
            }
            else -> super.analogOut4TypeToString()
        }
    }

    fun isCoolStage1Enabled() : Boolean {
        return this.relay1Enabled.enabled && this.relay1Association.associationVal == CpuRelayType.COOLING_STAGE1.ordinal ||
                this.relay2Enabled.enabled && this.relay2Association.associationVal == CpuRelayType.COOLING_STAGE1.ordinal ||
                this.relay3Enabled.enabled && this.relay3Association.associationVal == CpuRelayType.COOLING_STAGE1.ordinal ||
                this.relay4Enabled.enabled && this.relay4Association.associationVal == CpuRelayType.COOLING_STAGE1.ordinal ||
                this.relay5Enabled.enabled && this.relay5Association.associationVal == CpuRelayType.COOLING_STAGE1.ordinal ||
                this.relay6Enabled.enabled && this.relay6Association.associationVal == CpuRelayType.COOLING_STAGE1.ordinal ||
                this.relay7Enabled.enabled && this.relay7Association.associationVal == CpuRelayType.COOLING_STAGE1.ordinal ||
                this.relay8Enabled.enabled && this.relay8Association.associationVal == CpuRelayType.COOLING_STAGE1.ordinal
    }

    fun isCoolStage2Enabled() : Boolean {
        return this.relay1Enabled.enabled && this.relay1Association.associationVal == CpuRelayType.COOLING_STAGE2.ordinal ||
                this.relay2Enabled.enabled && this.relay2Association.associationVal == CpuRelayType.COOLING_STAGE2.ordinal ||
                this.relay3Enabled.enabled && this.relay3Association.associationVal == CpuRelayType.COOLING_STAGE2.ordinal ||
                this.relay4Enabled.enabled && this.relay4Association.associationVal == CpuRelayType.COOLING_STAGE2.ordinal ||
                this.relay5Enabled.enabled && this.relay5Association.associationVal == CpuRelayType.COOLING_STAGE2.ordinal ||
                this.relay6Enabled.enabled && this.relay6Association.associationVal == CpuRelayType.COOLING_STAGE2.ordinal ||
                this.relay7Enabled.enabled && this.relay7Association.associationVal == CpuRelayType.COOLING_STAGE2.ordinal ||
                this.relay8Enabled.enabled && this.relay8Association.associationVal == CpuRelayType.COOLING_STAGE2.ordinal
    }

    fun isCoolStage3Enabled() : Boolean {
        return this.relay1Enabled.enabled && this.relay1Association.associationVal == CpuRelayType.COOLING_STAGE3.ordinal ||
                this.relay2Enabled.enabled && this.relay2Association.associationVal == CpuRelayType.COOLING_STAGE3.ordinal ||
                this.relay3Enabled.enabled && this.relay3Association.associationVal == CpuRelayType.COOLING_STAGE3.ordinal ||
                this.relay4Enabled.enabled && this.relay4Association.associationVal == CpuRelayType.COOLING_STAGE3.ordinal ||
                this.relay5Enabled.enabled && this.relay5Association.associationVal == CpuRelayType.COOLING_STAGE3.ordinal ||
                this.relay6Enabled.enabled && this.relay6Association.associationVal == CpuRelayType.COOLING_STAGE3.ordinal ||
                this.relay7Enabled.enabled && this.relay7Association.associationVal == CpuRelayType.COOLING_STAGE3.ordinal ||
                this.relay8Enabled.enabled && this.relay8Association.associationVal == CpuRelayType.COOLING_STAGE3.ordinal
    }

    fun isHeatStage1Enabled() : Boolean {
        return this.relay1Enabled.enabled && this.relay1Association.associationVal == CpuRelayType.HEATING_STAGE1.ordinal ||
                this.relay2Enabled.enabled && this.relay2Association.associationVal == CpuRelayType.HEATING_STAGE1.ordinal ||
                this.relay3Enabled.enabled && this.relay3Association.associationVal == CpuRelayType.HEATING_STAGE1.ordinal ||
                this.relay4Enabled.enabled && this.relay4Association.associationVal == CpuRelayType.HEATING_STAGE1.ordinal ||
                this.relay5Enabled.enabled && this.relay5Association.associationVal == CpuRelayType.HEATING_STAGE1.ordinal ||
                this.relay6Enabled.enabled && this.relay6Association.associationVal == CpuRelayType.HEATING_STAGE1.ordinal ||
                this.relay7Enabled.enabled && this.relay7Association.associationVal == CpuRelayType.HEATING_STAGE1.ordinal ||
                this.relay8Enabled.enabled && this.relay8Association.associationVal == CpuRelayType.HEATING_STAGE1.ordinal
    }

    fun isHeatStage2Enabled() : Boolean {
        return this.relay1Enabled.enabled && this.relay1Association.associationVal == CpuRelayType.HEATING_STAGE2.ordinal ||
                this.relay2Enabled.enabled && this.relay2Association.associationVal == CpuRelayType.HEATING_STAGE2.ordinal ||
                this.relay3Enabled.enabled && this.relay3Association.associationVal == CpuRelayType.HEATING_STAGE2.ordinal ||
                this.relay4Enabled.enabled && this.relay4Association.associationVal == CpuRelayType.HEATING_STAGE2.ordinal ||
                this.relay5Enabled.enabled && this.relay5Association.associationVal == CpuRelayType.HEATING_STAGE2.ordinal ||
                this.relay6Enabled.enabled && this.relay6Association.associationVal == CpuRelayType.HEATING_STAGE2.ordinal ||
                this.relay7Enabled.enabled && this.relay7Association.associationVal == CpuRelayType.HEATING_STAGE2.ordinal ||
                this.relay8Enabled.enabled && this.relay8Association.associationVal == CpuRelayType.HEATING_STAGE2.ordinal
    }

    fun isHeatStage3Enabled() : Boolean {
        return this.relay1Enabled.enabled && this.relay1Association.associationVal == CpuRelayType.HEATING_STAGE3.ordinal ||
                this.relay2Enabled.enabled && this.relay2Association.associationVal == CpuRelayType.HEATING_STAGE3.ordinal ||
                this.relay3Enabled.enabled && this.relay3Association.associationVal == CpuRelayType.HEATING_STAGE3.ordinal ||
                this.relay4Enabled.enabled && this.relay4Association.associationVal == CpuRelayType.HEATING_STAGE3.ordinal ||
                this.relay5Enabled.enabled && this.relay5Association.associationVal == CpuRelayType.HEATING_STAGE3.ordinal ||
                this.relay6Enabled.enabled && this.relay6Association.associationVal == CpuRelayType.HEATING_STAGE3.ordinal ||
                this.relay7Enabled.enabled && this.relay7Association.associationVal == CpuRelayType.HEATING_STAGE3.ordinal ||
                this.relay8Enabled.enabled && this.relay8Association.associationVal == CpuRelayType.HEATING_STAGE3.ordinal
    }

    fun isFanLowRelayEnabled() : Boolean {
        return this.relay1Enabled.enabled && this.relay1Association.associationVal == CpuRelayType.FAN_LOW_SPEED.ordinal ||
                this.relay2Enabled.enabled && this.relay2Association.associationVal == CpuRelayType.FAN_LOW_SPEED.ordinal ||
                this.relay3Enabled.enabled && this.relay3Association.associationVal == CpuRelayType.FAN_LOW_SPEED.ordinal ||
                this.relay4Enabled.enabled && this.relay4Association.associationVal == CpuRelayType.FAN_LOW_SPEED.ordinal ||
                this.relay5Enabled.enabled && this.relay5Association.associationVal == CpuRelayType.FAN_LOW_SPEED.ordinal ||
                this.relay6Enabled.enabled && this.relay6Association.associationVal == CpuRelayType.FAN_LOW_SPEED.ordinal ||
                this.relay7Enabled.enabled && this.relay7Association.associationVal == CpuRelayType.FAN_LOW_SPEED.ordinal ||
                this.relay8Enabled.enabled && this.relay8Association.associationVal == CpuRelayType.FAN_LOW_SPEED.ordinal
    }

    fun isFanMediumRelayEnabled() : Boolean {
        return this.relay1Enabled.enabled && this.relay1Association.associationVal == CpuRelayType.FAN_MEDIUM_SPEED.ordinal ||
                this.relay2Enabled.enabled && this.relay2Association.associationVal == CpuRelayType.FAN_MEDIUM_SPEED.ordinal ||
                this.relay3Enabled.enabled && this.relay3Association.associationVal == CpuRelayType.FAN_MEDIUM_SPEED.ordinal ||
                this.relay4Enabled.enabled && this.relay4Association.associationVal == CpuRelayType.FAN_MEDIUM_SPEED.ordinal ||
                this.relay5Enabled.enabled && this.relay5Association.associationVal == CpuRelayType.FAN_MEDIUM_SPEED.ordinal ||
                this.relay6Enabled.enabled && this.relay6Association.associationVal == CpuRelayType.FAN_MEDIUM_SPEED.ordinal ||
                this.relay7Enabled.enabled && this.relay7Association.associationVal == CpuRelayType.FAN_MEDIUM_SPEED.ordinal ||
                this.relay8Enabled.enabled && this.relay8Association.associationVal == CpuRelayType.FAN_MEDIUM_SPEED.ordinal
    }

    fun isFanHighRelayEnabled() : Boolean {
        return this.relay1Enabled.enabled && this.relay1Association.associationVal == CpuRelayType.FAN_HIGH_SPEED.ordinal ||
                this.relay2Enabled.enabled && this.relay2Association.associationVal == CpuRelayType.FAN_HIGH_SPEED.ordinal ||
                this.relay3Enabled.enabled && this.relay3Association.associationVal == CpuRelayType.FAN_HIGH_SPEED.ordinal ||
                this.relay4Enabled.enabled && this.relay4Association.associationVal == CpuRelayType.FAN_HIGH_SPEED.ordinal ||
                this.relay5Enabled.enabled && this.relay5Association.associationVal == CpuRelayType.FAN_HIGH_SPEED.ordinal ||
                this.relay6Enabled.enabled && this.relay6Association.associationVal == CpuRelayType.FAN_HIGH_SPEED.ordinal ||
                this.relay7Enabled.enabled && this.relay7Association.associationVal == CpuRelayType.FAN_HIGH_SPEED.ordinal ||
                this.relay8Enabled.enabled && this.relay8Association.associationVal == CpuRelayType.FAN_HIGH_SPEED.ordinal
    }

    fun isAnalogCoolingEnabled() : Boolean {
        return this.analogOut1Enabled.enabled && this.analogOut1Association.associationVal == CpuControlType.COOLING.ordinal ||
                this.analogOut2Enabled.enabled && this.analogOut2Association.associationVal == CpuControlType.COOLING.ordinal ||
                this.analogOut3Enabled.enabled && this.analogOut3Association.associationVal == CpuControlType.COOLING.ordinal ||
                this.analogOut4Enabled.enabled && this.analogOut4Association.associationVal == CpuControlType.COOLING.ordinal
    }

    fun isAnalogHeatingEnabled() : Boolean {
        return this.analogOut1Enabled.enabled && this.analogOut1Association.associationVal == CpuControlType.HEATING.ordinal ||
                this.analogOut2Enabled.enabled && this.analogOut2Association.associationVal == CpuControlType.HEATING.ordinal ||
                this.analogOut3Enabled.enabled && this.analogOut3Association.associationVal == CpuControlType.HEATING.ordinal ||
                this.analogOut4Enabled.enabled && this.analogOut4Association.associationVal == CpuControlType.HEATING.ordinal
    }

    fun isLinearFanEnabled() : Boolean {
        return this.analogOut1Enabled.enabled && this.analogOut1Association.associationVal == CpuControlType.LINEAR_FAN.ordinal ||
                this.analogOut2Enabled.enabled && this.analogOut2Association.associationVal == CpuControlType.LINEAR_FAN.ordinal ||
                this.analogOut3Enabled.enabled && this.analogOut3Association.associationVal == CpuControlType.LINEAR_FAN.ordinal ||
                this.analogOut4Enabled.enabled && this.analogOut4Association.associationVal == CpuControlType.LINEAR_FAN.ordinal
    }

    fun isStagedFanEnabled() : Boolean {
        return this.analogOut1Enabled.enabled && this.analogOut1Association.associationVal == CpuControlType.STAGED_FAN.ordinal ||
                this.analogOut2Enabled.enabled && this.analogOut2Association.associationVal == CpuControlType.STAGED_FAN.ordinal ||
                this.analogOut3Enabled.enabled && this.analogOut3Association.associationVal == CpuControlType.STAGED_FAN.ordinal ||
                this.analogOut4Enabled.enabled && this.analogOut4Association.associationVal == CpuControlType.STAGED_FAN.ordinal
    }

}

data class AnalogOutVoltage(
    var coolingMinVoltage: ValueConfig, var coolingMaxVoltage: ValueConfig,
    var heatingMinVoltage: ValueConfig, var heatingMaxVoltage: ValueConfig,
    var oaoDamperMinVoltage: ValueConfig, var oaoDamperMaxVoltage: ValueConfig,
    var returnDamperMinVoltage: ValueConfig, var returnDamperMaxVoltage: ValueConfig,
    var linearFanMinVoltage: ValueConfig, var linearFanMaxVoltage: ValueConfig,
    var linearFanAtFanLow: ValueConfig, var linearFanAtFanMedium: ValueConfig,
    var linearFanAtFanHigh: ValueConfig
)

data class StagedFanVoltages(
    var recircVoltage: ValueConfig, var economizerVoltage: ValueConfig,
    var heatStage1Voltage: ValueConfig, var coolStage1Voltage: ValueConfig,
    var heatStage2Voltage: ValueConfig, var coolStage2Voltage: ValueConfig,
    var heatStage3Voltage: ValueConfig, var coolStage3Voltage: ValueConfig
)

/**
 * Following enum classes are used to define the input/output types for the HyperStat Split CPU Profile
 * These enum lists are picked from the model & Need to be updated when any changes are made in the model for the enum
 */
enum class CpuControlType {
    COOLING, LINEAR_FAN, HEATING, OAO_DAMPER, STAGED_FAN, RETURN_DAMPER, EXTERNALLY_MAPPED
}

enum class CpuRelayType {
    COOLING_STAGE1, COOLING_STAGE2, COOLING_STAGE3, HEATING_STAGE1, HEATING_STAGE2, HEATING_STAGE3, FAN_LOW_SPEED, FAN_MEDIUM_SPEED, FAN_HIGH_SPEED, FAN_ENABLED, OCCUPIED_ENABLED, HUMIDIFIER, DEHUMIDIFIER, EX_FAN_STAGE1, EX_FAN_STAGE2, DCV_DAMPER, EXTERNALLY_MAPPED
}

enum class CpuSensorBusType {
    SUPPLY_AIR, MIXED_AIR, OUTSIDE_AIR
}

enum class CpuUniInType {
    NONE, VOLTAGE_INPUT, THERMISTOR_INPUT, BUILDING_STATIC_PRESSURE1, BUILDING_STATIC_PRESSURE2, BUILDING_STATIC_PRESSURE10, INDEX_6, INDEX_7, INDEX_8, INDEX_9,
    INDEX_10, INDEX_11, INDEX_12, INDEX_13, SUPPLY_AIR_TEMPERATURE, INDEX_15, DUCT_STATIC_PRESSURE1_1, DUCT_STATIC_PRESSURE1_2, DUCT_STATIC_PRESSURE1_10, INDEX_19, INDEX_20,
    INDEX_21, INDEX_22, INDEX_23, INDEX_24, INDEX_25, INDEX_26, INDEX_27, MIXED_AIR_TEMPERATURE, OUTSIDE_AIR_DAMPER_FEEDBACK, INDEX_30,
    INDEX_31, INDEX_32, OUTSIDE_AIR_TEMPERATURE, INDEX_34, INDEX_35, INDEX_36, INDEX_37, INDEX_38, INDEX_39, INDEX_40,
    CURRENT_TX_10, CURRENT_TX_20, CURRENT_TX_30, CURRENT_TX_50, CURRENT_TX_60, CURRENT_TX_100, CURRENT_TX_120, CURRENT_TX_150, CURRENT_TX_200, INDEX_50,
    INDEX_51, INDEX_52, DISCHARGE_FAN_AM_STATUS, DISCHARGE_FAN_RUN_STATUS, DISCHARGE_FAN_TRIP_STATUS, INDEX_56, INDEX_57, EXHAUST_FAN_RUN_STATUS, EXHAUST_FAN_TRIP_STATUS, FILTER_STATUS_NO,
    FILTER_STATUS_NC, INDEX_62, INDEX_63, FIRE_ALARM_STATUS, INDEX_65, INDEX_66, INDEX_67, INDEX_68, INDEX_69, INDEX_70,
    INDEX_71, INDEX_72, HIGH_DIFFERENTIAL_PRESSURE_SWITCH, LOW_DIFFERENTIAL_PRESSURE_SWITCH, INDEX_75, INDEX_76, INDEX_77, INDEX_78, INDEX_79, INDEX_80,
    INDEX_81, INDEX_82, CONDENSATE_STATUS_NO, CONDENSATE_STATUS_NC, INDEX_85, INDEX_86, INDEX_87, INDEX_88, INDEX_89, INDEX_90,
    EMERGENCY_SHUTOFF_NO, EMERGENCY_SHUTOFF_NC, GENERIC_ALARM_NO, GENERIC_ALARM_NC, DOOR_WINDOW_SENSOR_NC, DOOR_WINDOW_SENSOR, DOOR_WINDOW_SENSOR_TITLE24_NC, DOOR_WINDOW_SENSOR_TITLE24, INDEX_99, INDEX_100
}


