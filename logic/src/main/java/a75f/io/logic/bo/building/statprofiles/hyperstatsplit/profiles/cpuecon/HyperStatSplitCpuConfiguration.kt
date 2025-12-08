package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.hyperstatsplit.HsSplitCpuEquip
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags
import java.util.Collections

class HyperStatSplitCpuConfiguration (nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String, profileType : ProfileType, model : SeventyFiveFProfileDirective) :
    HyperStatSplitConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType, model) {

    lateinit var analogOut1Voltage: AnalogOutVoltage
    lateinit var analogOut2Voltage: AnalogOutVoltage
    lateinit var analogOut3Voltage: AnalogOutVoltage
    lateinit var analogOut4Voltage: AnalogOutVoltage
    lateinit var stagedFanVoltages: StagedFanVoltages

    fun getActiveConfiguration() : HyperStatSplitCpuConfiguration {
        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val hssEquip = HsSplitCpuEquip(equip[Tags.ID].toString())
        getDefaultConfiguration()
        getActiveEnableConfigs(hssEquip)
        getActiveAssociationConfigs(hssEquip)
        getGenericZoneConfigs(hssEquip)
        getActiveDynamicConfigs(hssEquip)
        equipId = hssEquip.equipRef
        isDefault = false
        return this
    }

    private fun getActiveDynamicConfigs(hssEquip: HsSplitCpuEquip) {
        apply {

            analogOut1Voltage.coolingMinVoltage.currentVal = getDefault(hssEquip.analog1MinCooling, hssEquip, analogOut1Voltage.coolingMinVoltage)
            analogOut1Voltage.coolingMaxVoltage.currentVal = getDefault(hssEquip.analog1MaxCooling, hssEquip, analogOut1Voltage.coolingMaxVoltage)
            analogOut1Voltage.heatingMinVoltage.currentVal = getDefault(hssEquip.analog1MinHeating, hssEquip, analogOut1Voltage.heatingMinVoltage)
            analogOut1Voltage.heatingMaxVoltage.currentVal = getDefault(hssEquip.analog1MaxHeating, hssEquip, analogOut1Voltage.heatingMaxVoltage)
            analogOut1Voltage.oaoDamperMinVoltage.currentVal = getDefault(hssEquip.analog1MinOAODamper, hssEquip, analogOut1Voltage.oaoDamperMinVoltage)
            analogOut1Voltage.oaoDamperMaxVoltage.currentVal = getDefault(hssEquip.analog1MaxOAODamper, hssEquip, analogOut1Voltage.oaoDamperMaxVoltage)
            analogOut1Voltage.returnDamperMinVoltage.currentVal = getDefault(hssEquip.analog1MinReturnDamper, hssEquip, analogOut1Voltage.returnDamperMinVoltage)
            analogOut1Voltage.returnDamperMaxVoltage.currentVal = getDefault(hssEquip.analog1MaxReturnDamper, hssEquip, analogOut1Voltage.returnDamperMaxVoltage)
            analogOut1Voltage.linearFanMinVoltage.currentVal = getDefault(hssEquip.analog1MinLinearFanSpeed, hssEquip, analogOut1Voltage.linearFanMinVoltage)
            analogOut1Voltage.linearFanMaxVoltage.currentVal = getDefault(hssEquip.analog1MaxLinearFanSpeed, hssEquip, analogOut1Voltage.linearFanMaxVoltage)
            analogOut1Voltage.linearFanAtFanLow.currentVal = getDefault(hssEquip.analog1FanLow, hssEquip, analogOut1Voltage.linearFanAtFanLow)
            analogOut1Voltage.linearFanAtFanMedium.currentVal = getDefault(hssEquip.analog1FanMedium, hssEquip, analogOut1Voltage.linearFanAtFanMedium)
            analogOut1Voltage.linearFanAtFanHigh.currentVal = getDefault(hssEquip.analog1FanHigh, hssEquip, analogOut1Voltage.linearFanAtFanHigh)
            analogOut1Voltage.compressorMinVoltage.currentVal = getDefault(hssEquip.analog1MinCompressorSpeed, hssEquip, analogOut1Voltage.compressorMinVoltage)
            analogOut1Voltage.compressorMaxVoltage.currentVal = getDefault(hssEquip.analog1MaxCompressorSpeed, hssEquip, analogOut1Voltage.compressorMaxVoltage)
            analogOut1Voltage.dcvModulationMinVoltage.currentVal = getDefault(hssEquip.analog1MinDCVDamper, hssEquip, analogOut1Voltage.dcvModulationMinVoltage)
            analogOut1Voltage.dcvModulationMaxVoltage.currentVal = getDefault(hssEquip.analog2MinDCVDamper, hssEquip, analogOut1Voltage.dcvModulationMaxVoltage)
            
            analogOut2Voltage.coolingMinVoltage.currentVal = getDefault(hssEquip.analog2MinCooling, hssEquip, analogOut2Voltage.coolingMinVoltage)
            analogOut2Voltage.coolingMaxVoltage.currentVal = getDefault(hssEquip.analog2MaxCooling, hssEquip, analogOut2Voltage.coolingMaxVoltage)
            analogOut2Voltage.heatingMinVoltage.currentVal = getDefault(hssEquip.analog2MinHeating, hssEquip, analogOut2Voltage.heatingMinVoltage)
            analogOut2Voltage.heatingMaxVoltage.currentVal = getDefault(hssEquip.analog2MaxHeating, hssEquip, analogOut2Voltage.heatingMaxVoltage)
            analogOut2Voltage.oaoDamperMinVoltage.currentVal = getDefault(hssEquip.analog2MinOAODamper, hssEquip, analogOut2Voltage.oaoDamperMinVoltage)
            analogOut2Voltage.oaoDamperMaxVoltage.currentVal = getDefault(hssEquip.analog2MaxOAODamper, hssEquip, analogOut2Voltage.oaoDamperMaxVoltage)
            analogOut2Voltage.returnDamperMinVoltage.currentVal = getDefault(hssEquip.analog2MinReturnDamper, hssEquip, analogOut2Voltage.returnDamperMinVoltage)
            analogOut2Voltage.returnDamperMaxVoltage.currentVal = getDefault(hssEquip.analog2MaxReturnDamper, hssEquip, analogOut2Voltage.returnDamperMaxVoltage)
            analogOut2Voltage.linearFanMinVoltage.currentVal = getDefault(hssEquip.analog2MinLinearFanSpeed, hssEquip, analogOut2Voltage.linearFanMinVoltage)
            analogOut2Voltage.linearFanMaxVoltage.currentVal = getDefault(hssEquip.analog2MaxLinearFanSpeed, hssEquip, analogOut2Voltage.linearFanMaxVoltage)
            analogOut2Voltage.linearFanAtFanLow.currentVal = getDefault(hssEquip.analog2FanLow, hssEquip, analogOut2Voltage.linearFanAtFanLow)
            analogOut2Voltage.linearFanAtFanMedium.currentVal = getDefault(hssEquip.analog2FanMedium, hssEquip, analogOut2Voltage.linearFanAtFanMedium)
            analogOut2Voltage.linearFanAtFanHigh.currentVal = getDefault(hssEquip.analog2FanHigh, hssEquip, analogOut2Voltage.linearFanAtFanHigh)
            analogOut2Voltage.compressorMinVoltage.currentVal = getDefault(hssEquip.analog2MinCompressorSpeed, hssEquip, analogOut2Voltage.compressorMinVoltage)
            analogOut2Voltage.compressorMaxVoltage.currentVal = getDefault(hssEquip.analog2MaxCompressorSpeed, hssEquip, analogOut2Voltage.compressorMaxVoltage)
            analogOut2Voltage.dcvModulationMinVoltage.currentVal = getDefault(hssEquip.analog2MinDCVDamper, hssEquip, analogOut2Voltage.dcvModulationMinVoltage)
            analogOut2Voltage.dcvModulationMaxVoltage.currentVal = getDefault(hssEquip.analog1MaxDCVDamper, hssEquip, analogOut2Voltage.dcvModulationMaxVoltage)
            
            analogOut3Voltage.coolingMinVoltage.currentVal = getDefault(hssEquip.analog3MinCooling, hssEquip, analogOut3Voltage.coolingMinVoltage)
            analogOut3Voltage.coolingMaxVoltage.currentVal = getDefault(hssEquip.analog3MaxCooling, hssEquip, analogOut3Voltage.coolingMaxVoltage)
            analogOut3Voltage.heatingMinVoltage.currentVal = getDefault(hssEquip.analog3MinHeating, hssEquip, analogOut3Voltage.heatingMinVoltage)
            analogOut3Voltage.heatingMaxVoltage.currentVal = getDefault(hssEquip.analog3MaxHeating, hssEquip, analogOut3Voltage.heatingMaxVoltage)
            analogOut3Voltage.oaoDamperMinVoltage.currentVal = getDefault(hssEquip.analog3MinOAODamper, hssEquip, analogOut3Voltage.oaoDamperMinVoltage)
            analogOut3Voltage.oaoDamperMaxVoltage.currentVal = getDefault(hssEquip.analog3MaxOAODamper, hssEquip, analogOut3Voltage.oaoDamperMaxVoltage)
            analogOut3Voltage.returnDamperMinVoltage.currentVal = getDefault(hssEquip.analog3MinReturnDamper, hssEquip, analogOut3Voltage.returnDamperMinVoltage)
            analogOut3Voltage.returnDamperMaxVoltage.currentVal = getDefault(hssEquip.analog3MaxReturnDamper, hssEquip, analogOut3Voltage.returnDamperMaxVoltage)
            analogOut3Voltage.linearFanMinVoltage.currentVal = getDefault(hssEquip.analog3MinLinearFanSpeed, hssEquip, analogOut3Voltage.linearFanMinVoltage)
            analogOut3Voltage.linearFanMaxVoltage.currentVal = getDefault(hssEquip.analog3MaxLinearFanSpeed, hssEquip, analogOut3Voltage.linearFanMaxVoltage)
            analogOut3Voltage.linearFanAtFanLow.currentVal = getDefault(hssEquip.analog3FanLow, hssEquip, analogOut3Voltage.linearFanAtFanLow)
            analogOut3Voltage.linearFanAtFanMedium.currentVal = getDefault(hssEquip.analog3FanMedium, hssEquip, analogOut3Voltage.linearFanAtFanMedium)
            analogOut3Voltage.linearFanAtFanHigh.currentVal = getDefault(hssEquip.analog3FanHigh, hssEquip, analogOut3Voltage.linearFanAtFanHigh)
            analogOut3Voltage.compressorMinVoltage.currentVal = getDefault(hssEquip.analog3MinCompressorSpeed, hssEquip, analogOut3Voltage.compressorMinVoltage)
            analogOut3Voltage.compressorMaxVoltage.currentVal = getDefault(hssEquip.analog3MaxCompressorSpeed, hssEquip, analogOut3Voltage.compressorMaxVoltage)
            analogOut3Voltage.dcvModulationMinVoltage.currentVal = getDefault(hssEquip.analog3MinDCVDamper, hssEquip, analogOut3Voltage.dcvModulationMinVoltage)
            analogOut3Voltage.dcvModulationMaxVoltage.currentVal = getDefault(hssEquip.analog3MaxDCVDamper, hssEquip, analogOut3Voltage.dcvModulationMaxVoltage)
            
            analogOut4Voltage.coolingMinVoltage.currentVal = getDefault(hssEquip.analog4MinCooling, hssEquip, analogOut4Voltage.coolingMinVoltage)
            analogOut4Voltage.coolingMaxVoltage.currentVal = getDefault(hssEquip.analog4MaxCooling, hssEquip, analogOut4Voltage.coolingMaxVoltage)
            analogOut4Voltage.heatingMinVoltage.currentVal = getDefault(hssEquip.analog4MinHeating, hssEquip, analogOut4Voltage.heatingMinVoltage)
            analogOut4Voltage.heatingMaxVoltage.currentVal = getDefault(hssEquip.analog4MaxHeating, hssEquip, analogOut4Voltage.heatingMaxVoltage)
            analogOut4Voltage.oaoDamperMinVoltage.currentVal = getDefault(hssEquip.analog4MinOAODamper, hssEquip, analogOut4Voltage.oaoDamperMinVoltage)
            analogOut4Voltage.oaoDamperMaxVoltage.currentVal = getDefault(hssEquip.analog4MaxOAODamper, hssEquip, analogOut4Voltage.oaoDamperMaxVoltage)
            analogOut4Voltage.returnDamperMinVoltage.currentVal = getDefault(hssEquip.analog4MinReturnDamper, hssEquip, analogOut4Voltage.returnDamperMinVoltage)
            analogOut4Voltage.returnDamperMaxVoltage.currentVal = getDefault(hssEquip.analog4MaxReturnDamper, hssEquip, analogOut4Voltage.returnDamperMaxVoltage)
            analogOut4Voltage.linearFanMinVoltage.currentVal = getDefault(hssEquip.analog4MinLinearFanSpeed, hssEquip, analogOut4Voltage.linearFanMinVoltage)
            analogOut4Voltage.linearFanMaxVoltage.currentVal = getDefault(hssEquip.analog4MaxLinearFanSpeed, hssEquip, analogOut4Voltage.linearFanMaxVoltage)
            analogOut4Voltage.linearFanAtFanLow.currentVal = getDefault(hssEquip.analog4FanLow, hssEquip, analogOut4Voltage.linearFanAtFanLow)
            analogOut4Voltage.linearFanAtFanMedium.currentVal = getDefault(hssEquip.analog4FanMedium, hssEquip, analogOut4Voltage.linearFanAtFanMedium)
            analogOut4Voltage.linearFanAtFanHigh.currentVal = getDefault(hssEquip.analog4FanHigh, hssEquip, analogOut4Voltage.linearFanAtFanHigh)
            analogOut4Voltage.compressorMinVoltage.currentVal = getDefault(hssEquip.analog4MinCompressorSpeed, hssEquip, analogOut4Voltage.compressorMinVoltage)
            analogOut4Voltage.compressorMaxVoltage.currentVal = getDefault(hssEquip.analog4MaxCompressorSpeed, hssEquip, analogOut4Voltage.compressorMaxVoltage)
            analogOut4Voltage.dcvModulationMinVoltage.currentVal = getDefault(hssEquip.analog4MinDCVDamper, hssEquip, analogOut4Voltage.dcvModulationMinVoltage)
            analogOut4Voltage.dcvModulationMaxVoltage.currentVal = getDefault(hssEquip.analog4MaxDCVDamper, hssEquip, analogOut4Voltage.dcvModulationMaxVoltage)
            
            stagedFanVoltages.recircVoltage.currentVal = getDefault(hssEquip.fanOutRecirculate, hssEquip, stagedFanVoltages.recircVoltage)
            stagedFanVoltages.economizerVoltage.currentVal = getDefault(hssEquip.fanOutEconomizer, hssEquip, stagedFanVoltages.economizerVoltage)
            stagedFanVoltages.heatStage1Voltage.currentVal = getDefault(hssEquip.fanOutHeatingStage1, hssEquip, stagedFanVoltages.heatStage1Voltage)
            stagedFanVoltages.heatStage2Voltage.currentVal = getDefault(hssEquip.fanOutHeatingStage2, hssEquip, stagedFanVoltages.heatStage2Voltage)
            stagedFanVoltages.heatStage3Voltage.currentVal = getDefault(hssEquip.fanOutHeatingStage3, hssEquip, stagedFanVoltages.heatStage3Voltage)
            stagedFanVoltages.coolStage1Voltage.currentVal = getDefault(hssEquip.fanOutCoolingStage1, hssEquip, stagedFanVoltages.coolStage1Voltage)
            stagedFanVoltages.coolStage2Voltage.currentVal = getDefault(hssEquip.fanOutCoolingStage2, hssEquip, stagedFanVoltages.coolStage2Voltage)
            stagedFanVoltages.coolStage3Voltage.currentVal = getDefault(hssEquip.fanOutCoolingStage3, hssEquip, stagedFanVoltages.coolStage3Voltage)
            stagedFanVoltages.compressorStage1Voltage.currentVal = getDefault(hssEquip.fanOutCompressorStage1, hssEquip, stagedFanVoltages.compressorStage1Voltage)
            stagedFanVoltages.compressorStage2Voltage.currentVal = getDefault(hssEquip.fanOutCompressorStage2, hssEquip, stagedFanVoltages.compressorStage2Voltage)
            stagedFanVoltages.compressorStage3Voltage.currentVal = getDefault(hssEquip.fanOutCompressorStage3, hssEquip, stagedFanVoltages.compressorStage3Voltage)

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
            add(analogConfig.compressorMinVoltage)
            add(analogConfig.compressorMaxVoltage)
            add(analogConfig.dcvModulationMinVoltage)
            add(analogConfig.dcvModulationMaxVoltage)
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
            add(stagedFanConfig.compressorStage1Voltage)
            add(stagedFanConfig.compressorStage2Voltage)
            add(stagedFanConfig.compressorStage3Voltage)
        }
    }

    override fun getDefaultConfiguration(): HyperStatSplitCpuConfiguration {
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
            compressorMinVoltage = getDefaultValConfig(DomainName.analog1MinCompressorSpeed, model),
            compressorMaxVoltage = getDefaultValConfig(DomainName.analog1MaxCompressorSpeed, model),
            dcvModulationMinVoltage = getDefaultValConfig(DomainName.analog1MinDCVDamper, model),
            dcvModulationMaxVoltage = getDefaultValConfig(DomainName.analog1MaxDCVDamper, model),
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
            compressorMinVoltage = getDefaultValConfig(DomainName.analog2MinCompressorSpeed, model),
            compressorMaxVoltage = getDefaultValConfig(DomainName.analog2MaxCompressorSpeed, model),
            dcvModulationMinVoltage = getDefaultValConfig(DomainName.analog2MinDCVDamper, model),
            dcvModulationMaxVoltage = getDefaultValConfig(DomainName.analog2MaxDCVDamper, model),
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
            compressorMinVoltage = getDefaultValConfig(DomainName.analog3MinCompressorSpeed, model),
            compressorMaxVoltage = getDefaultValConfig(DomainName.analog3MaxCompressorSpeed, model),
            dcvModulationMinVoltage = getDefaultValConfig(DomainName.analog3MinDCVDamper, model),
            dcvModulationMaxVoltage = getDefaultValConfig(DomainName.analog3MaxDCVDamper, model),
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
            compressorMinVoltage = getDefaultValConfig(DomainName.analog4MinCompressorSpeed, model),
            compressorMaxVoltage = getDefaultValConfig(DomainName.analog4MaxCompressorSpeed, model),
            dcvModulationMinVoltage = getDefaultValConfig(DomainName.analog4MinDCVDamper, model),
            dcvModulationMaxVoltage = getDefaultValConfig(DomainName.analog4MaxDCVDamper, model),
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
            compressorStage1Voltage = getDefaultValConfig(DomainName.fanOutCompressorStage1, model),
            heatStage2Voltage = getDefaultValConfig(DomainName.fanOutHeatingStage2, model),
            coolStage2Voltage = getDefaultValConfig(DomainName.fanOutCoolingStage2, model),
            compressorStage2Voltage = getDefaultValConfig(DomainName.fanOutCompressorStage2, model),
            heatStage3Voltage = getDefaultValConfig(DomainName.fanOutHeatingStage3, model),
            coolStage3Voltage = getDefaultValConfig(DomainName.fanOutCoolingStage3, model),
            compressorStage3Voltage = getDefaultValConfig(DomainName.fanOutCompressorStage3, model),
        )
    }

    override fun toString(): String {
        return super.toString() +
                "\nanalogOut1Voltage $analogOut1Voltage" +
                "\nanalogOut2Voltage $analogOut2Voltage" +
                "\nanalogOut3Voltage $analogOut3Voltage" +
                "\nanalogOut4Voltage $analogOut4Voltage"
    }

    fun isCompressorAvailable(): Boolean {
        return (isAnyRelayEnabledAndMapped(this, CpuRelayType.COMPRESSOR_STAGE1.name)
                || isAnyRelayEnabledAndMapped(this, CpuRelayType.COMPRESSOR_STAGE2.name)
                || isAnyRelayEnabledAndMapped(this, CpuRelayType.COMPRESSOR_STAGE3.name)
                || isAnyAnalogEnabledAndMapped(this, CpuAnalogControlType.COMPRESSOR_SPEED.name)
                )
    }
    override fun isCoolingAvailable(): Boolean {
        return (isAnyRelayEnabledAndMapped(this, CpuRelayType.COOLING_STAGE1.name)
                || isAnyRelayEnabledAndMapped(this, CpuRelayType.COOLING_STAGE2.name)
                || isAnyRelayEnabledAndMapped(this, CpuRelayType.COOLING_STAGE2.name)
                || isAnyAnalogEnabledAndMapped(this, CpuAnalogControlType.COOLING.name)
                || isCompressorAvailable()
                )
    }

    override fun isHeatingAvailable(): Boolean {
        return (isAnyRelayEnabledAndMapped(this, CpuRelayType.HEATING_STAGE1.name)
                || isAnyRelayEnabledAndMapped(this, CpuRelayType.HEATING_STAGE2.name)
                || isAnyRelayEnabledAndMapped(this, CpuRelayType.HEATING_STAGE3.name)
                || isAnyAnalogEnabledAndMapped(this, CpuAnalogControlType.HEATING.name)
                || isCompressorAvailable()
                )
    }



    override fun getRelayMap(): Map<String, Boolean> {
        val relays = mutableMapOf<String, Boolean>()
        relays[DomainName.relay1] = isRelayExternalMapped(relay1Enabled,relay1Association)
        relays[DomainName.relay2] = isRelayExternalMapped(relay2Enabled,relay2Association)
        relays[DomainName.relay3] = isRelayExternalMapped(relay3Enabled,relay3Association)
        relays[DomainName.relay4] = isRelayExternalMapped(relay4Enabled,relay4Association)
        relays[DomainName.relay5] = isRelayExternalMapped(relay5Enabled,relay5Association)
        relays[DomainName.relay6] = isRelayExternalMapped(relay6Enabled,relay6Association)
        relays[DomainName.relay7] = isRelayExternalMapped(relay7Enabled,relay7Association)
        relays[DomainName.relay8] = isRelayExternalMapped(relay8Enabled,relay8Association)
        return relays
    }

    override fun getAnalogMap(): Map<String, Pair<Boolean, String>> {
        val analogOuts = mutableMapOf<String, Pair<Boolean, String>>()
        analogOuts[DomainName.analog1Out] = Pair(isAnalogOutExternallyMapped(analogOut1Enabled,analogOut1Association), analogType(analogOut1Enabled))
        analogOuts[DomainName.analog2Out] = Pair(isAnalogOutExternallyMapped(analogOut2Enabled,analogOut2Association), analogType(analogOut2Enabled))
        analogOuts[DomainName.analog3Out] = Pair(isAnalogOutExternallyMapped(analogOut3Enabled,analogOut3Association), analogType(analogOut3Enabled))
        analogOuts[DomainName.analog4Out] = Pair(isAnalogOutExternallyMapped(analogOut4Enabled,analogOut4Association), analogType(analogOut4Enabled))
        return analogOuts
    }

    private fun analogType(analogOutPort : EnableConfig):String{
        return when(analogOutPort) {
            analogOut1Enabled -> getPortType(analogOut1Association, analogOut1Voltage)
            analogOut2Enabled -> getPortType(analogOut2Association, analogOut2Voltage)
            analogOut3Enabled -> getPortType(analogOut3Association, analogOut3Voltage)
            analogOut4Enabled -> getPortType(analogOut4Association, analogOut4Voltage)
            else -> "2-10v"
        }
    }

    private fun getPortType(
        association: AssociationConfig,
        minMaxConfig: AnalogOutVoltage
    ): String {
        val portType = when (association.associationVal) {
            CpuAnalogControlType.COOLING.ordinal -> {
                "${minMaxConfig.coolingMinVoltage.currentVal.toInt()}-${minMaxConfig.coolingMaxVoltage.currentVal.toInt()}v"
            }

            CpuAnalogControlType.HEATING.ordinal -> {
                "${minMaxConfig.heatingMinVoltage.currentVal.toInt()}-${minMaxConfig.heatingMaxVoltage.currentVal.toInt()}v"
            }

            CpuAnalogControlType.LINEAR_FAN.ordinal -> {
                "${minMaxConfig.linearFanMinVoltage.currentVal.toInt()}-${minMaxConfig.linearFanMaxVoltage.currentVal.toInt()}v"
            }

            CpuAnalogControlType.OAO_DAMPER.ordinal -> {
                "${minMaxConfig.oaoDamperMinVoltage.currentVal.toInt()}-${minMaxConfig.oaoDamperMaxVoltage.currentVal.toInt()}v"
            }

            CpuAnalogControlType.RETURN_DAMPER.ordinal -> {
                "${minMaxConfig.returnDamperMinVoltage.currentVal.toInt()}-${minMaxConfig.returnDamperMaxVoltage.currentVal.toInt()}v"

            }
            CpuAnalogControlType.COMPRESSOR_SPEED.ordinal -> {
                "${minMaxConfig.compressorMinVoltage.currentVal.toInt()}-${minMaxConfig.compressorMaxVoltage.currentVal.toInt()}v"
            }

            CpuAnalogControlType.DCV_MODULATING_DAMPER.ordinal -> {
                "${minMaxConfig.dcvModulationMinVoltage.currentVal.toInt()}-${minMaxConfig.oaoDamperMaxVoltage.currentVal.toInt()}v"
            }

            else -> {
                "2-10v"
            }
        }
        return portType
    }


    private fun isRelayExternalMapped(enabled: EnableConfig, association: AssociationConfig) =
        (enabled.enabled && association.associationVal == CpuRelayType.EXTERNALLY_MAPPED.ordinal)

    private  fun isAnalogOutExternallyMapped(enabled: EnableConfig, association: AssociationConfig) =
        (enabled.enabled && association.associationVal == CpuAnalogControlType.EXTERNALLY_MAPPED.ordinal)


    //Function which checks the Relay is Associated  to Fan or Not
    fun isRelayAssociatedToFan(relayConfig: AssociationConfig): Boolean {
        return (relayConfig.associationVal == CpuRelayType.FAN_LOW_SPEED.ordinal
                || relayConfig.associationVal == CpuRelayType.FAN_MEDIUM_SPEED.ordinal
                || relayConfig.associationVal == CpuRelayType.FAN_HIGH_SPEED.ordinal)
    }


    //Function which checks the Relay is Associated  to Cooling Stage
    fun isRelayAssociatedToCoolingStage(relayConfig: AssociationConfig): Boolean {
        return (relayConfig.associationVal == CpuRelayType.COOLING_STAGE1.ordinal
                || relayConfig.associationVal == CpuRelayType.COOLING_STAGE2.ordinal
                || relayConfig.associationVal == CpuRelayType.COOLING_STAGE3.ordinal)
    }

    //Function which checks the Relay is Associated  to Heating Stage
    fun isRelayAssociatedToHeatingStage(relayConfig: AssociationConfig): Boolean {
        return (relayConfig.associationVal == CpuRelayType.HEATING_STAGE1.ordinal
                || relayConfig.associationVal == CpuRelayType.HEATING_STAGE2.ordinal
                || relayConfig.associationVal == CpuRelayType.HEATING_STAGE3.ordinal)

    }

    fun getHighestCoolingStageCount(): Int {
        val stage = getHighestStage(
            CpuRelayType.COOLING_STAGE1.ordinal,
            CpuRelayType.COOLING_STAGE2.ordinal,
            CpuRelayType.COOLING_STAGE3.ordinal
        )
        if (stage == -1) return 0
        return stage + 1
    }

    fun getHighestHeatingStageCount(): Int {
        val stageOrdinal = getHighestStage(
            CpuRelayType.HEATING_STAGE1.ordinal,
            CpuRelayType.HEATING_STAGE2.ordinal,
            CpuRelayType.HEATING_STAGE3.ordinal
        )
        if (stageOrdinal == -1) return 0
        return (stageOrdinal - 2)

    }

    override fun getHighestFanStageCount(): Int {
        val stageOrdinal = getHighestStage(
            CpuRelayType.FAN_LOW_SPEED.ordinal,
            CpuRelayType.FAN_MEDIUM_SPEED.ordinal,
            CpuRelayType.FAN_HIGH_SPEED.ordinal
        )
        if (stageOrdinal == -1) return 0
        return (stageOrdinal - 5)
    }


    fun getHighestCompressorStageCount(): Int {
        val stageOrdinal = getHighestStage(
            CpuRelayType.COMPRESSOR_STAGE1.ordinal,
            CpuRelayType.COMPRESSOR_STAGE2.ordinal,
            CpuRelayType.COMPRESSOR_STAGE3.ordinal
        )
        if (stageOrdinal == -1) return 0
        return (stageOrdinal - 16)
    }

    fun getFanConfiguration(port: Port): AnalogOutVoltage {
        return when (port) {
            Port.ANALOG_OUT_ONE -> analogOut1Voltage
            Port.ANALOG_OUT_TWO -> analogOut2Voltage
            Port.ANALOG_OUT_THREE -> analogOut3Voltage
            Port.ANALOG_OUT_FOUR -> analogOut4Voltage
            else -> analogOut1Voltage
        }
    }

}

data class AnalogOutVoltage(
    var coolingMinVoltage: ValueConfig, var coolingMaxVoltage: ValueConfig,
    var heatingMinVoltage: ValueConfig, var heatingMaxVoltage: ValueConfig,
    var oaoDamperMinVoltage: ValueConfig, var oaoDamperMaxVoltage: ValueConfig,
    var returnDamperMinVoltage: ValueConfig, var returnDamperMaxVoltage: ValueConfig,
    var compressorMinVoltage: ValueConfig, var compressorMaxVoltage: ValueConfig,
    var dcvModulationMinVoltage: ValueConfig, var dcvModulationMaxVoltage: ValueConfig,
    var linearFanMinVoltage: ValueConfig, var linearFanMaxVoltage: ValueConfig,
    var linearFanAtFanLow: ValueConfig, var linearFanAtFanMedium: ValueConfig,
    var linearFanAtFanHigh: ValueConfig
)

data class StagedFanVoltages(
    var recircVoltage: ValueConfig, var economizerVoltage: ValueConfig,
    var heatStage1Voltage: ValueConfig, var coolStage1Voltage: ValueConfig, val compressorStage1Voltage: ValueConfig,
    var heatStage2Voltage: ValueConfig, var coolStage2Voltage: ValueConfig, val compressorStage2Voltage: ValueConfig,
    var heatStage3Voltage: ValueConfig, var coolStage3Voltage: ValueConfig, val compressorStage3Voltage: ValueConfig
)

/**
 * Following enum classes are used to define the input/output types for the HyperStat Split CPU Profile
 * These enum lists are picked from the model & Need to be updated when any changes are made in the model for the enum
 */
enum class CpuAnalogControlType {
    COOLING, LINEAR_FAN, HEATING, OAO_DAMPER, STAGED_FAN, RETURN_DAMPER, EXTERNALLY_MAPPED, COMPRESSOR_SPEED, DCV_MODULATING_DAMPER,
}

enum class CpuRelayType {
    COOLING_STAGE1,
    COOLING_STAGE2,
    COOLING_STAGE3,
    HEATING_STAGE1,
    HEATING_STAGE2,
    HEATING_STAGE3,
    FAN_LOW_SPEED,
    FAN_MEDIUM_SPEED,
    FAN_HIGH_SPEED,
    FAN_ENABLED,
    OCCUPIED_ENABLED,
    HUMIDIFIER,
    DEHUMIDIFIER,
    EX_FAN_STAGE1,
    EX_FAN_STAGE2,
    DCV_DAMPER,
    EXTERNALLY_MAPPED,
    COMPRESSOR_STAGE1,
    COMPRESSOR_STAGE2,
    COMPRESSOR_STAGE3,
    CHANGE_OVER_O_COOLING,
    CHANGE_OVER_B_HEATING,
    AUX_HEATING_STAGE1,
    AUX_HEATING_STAGE2
}

