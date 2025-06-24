package a75f.io.logic.bo.building.system.vav.config

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.VavStagedVfdSystemEquip
import a75f.io.domain.util.CommonQueries
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.bo.haystack.device.ControlMote.getAllUnusedPorts
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

open class StagedVfdRtuProfileConfig(modelDef : SeventyFiveFProfileDirective)
                        : StagedRtuProfileConfig(modelDef){

    lateinit var analogOut2Enabled : EnableConfig
    lateinit var analogOut2Association: AssociationConfig

    lateinit var analogOut2Economizer : ValueConfig
    lateinit var analogOut2Recirculate : ValueConfig
    lateinit var analogOut2CoolStage1 : ValueConfig
    lateinit var analogOut2CoolStage2 : ValueConfig
    lateinit var analogOut2CoolStage3 : ValueConfig
    lateinit var analogOut2CoolStage4 : ValueConfig
    lateinit var analogOut2CoolStage5 : ValueConfig
    lateinit var analogOut2HeatStage1 : ValueConfig
    lateinit var analogOut2HeatStage2 : ValueConfig
    lateinit var analogOut2HeatStage3 : ValueConfig
    lateinit var analogOut2HeatStage4 : ValueConfig
    lateinit var analogOut2HeatStage5 : ValueConfig
    lateinit var analogOut2Default : ValueConfig

    lateinit var analogOut2MinCompressor : ValueConfig
    lateinit var analogOut2MaxCompressor : ValueConfig
    lateinit var analogOut2MinDamperModulation : ValueConfig
    lateinit var analogOut2MaxDamperModulation : ValueConfig

    override fun getDefaultConfiguration() : StagedVfdRtuProfileConfig {
        relay1Enabled = getDefaultEnableConfig(DomainName.relay1OutputEnable, model)
        relay2Enabled = getDefaultEnableConfig(DomainName.relay2OutputEnable, model)
        relay3Enabled = getDefaultEnableConfig(DomainName.relay3OutputEnable, model)
        relay4Enabled = getDefaultEnableConfig(DomainName.relay4OutputEnable, model)
        relay5Enabled = getDefaultEnableConfig(DomainName.relay5OutputEnable, model)
        relay6Enabled = getDefaultEnableConfig(DomainName.relay6OutputEnable, model)
        relay7Enabled = getDefaultEnableConfig(DomainName.relay7OutputEnable, model)

        thermistor1Enabled = getDefaultEnableConfig(DomainName.thermistor1InputEnable, model)
        thermistor2Enabled = getDefaultEnableConfig(DomainName.thermistor2InputEnable, model)
        analogIn1Enabled = getDefaultEnableConfig(DomainName.analog1InputEnable, model)
        analogIn2Enabled = getDefaultEnableConfig(DomainName.analog2InputEnable, model)

        relay1Association = getDefaultAssociationConfig(DomainName.relay1OutputAssociation, model)
        relay2Association = getDefaultAssociationConfig(DomainName.relay2OutputAssociation, model)
        relay3Association = getDefaultAssociationConfig(DomainName.relay3OutputAssociation, model)
        relay4Association = getDefaultAssociationConfig(DomainName.relay4OutputAssociation, model)
        relay5Association = getDefaultAssociationConfig(DomainName.relay5OutputAssociation, model)
        relay6Association = getDefaultAssociationConfig(DomainName.relay6OutputAssociation, model)
        relay7Association = getDefaultAssociationConfig(DomainName.relay7OutputAssociation, model)

        analogOut2Enabled = getDefaultEnableConfig(DomainName.analog2OutputEnable, model)
        analogOut2Association = getDefaultAssociationConfig(DomainName.analog2OutputAssociation, model)


        thermistor1InAssociation = getDefaultAssociationConfig(DomainName.thermistor1InputAssociation, model)
        thermistor2InAssociation = getDefaultAssociationConfig(DomainName.thermistor2InputAssociation, model)
        analogIn1Association = getDefaultAssociationConfig(DomainName.analog1InputAssociation, model)
        analogIn2Association = getDefaultAssociationConfig(DomainName.analog2InputAssociation, model)

        CcuLog.i("manju", "getDefaultValConfig(DomainName.analog2Economizer, model) ${getDefaultValConfig(DomainName.analog2Economizer, model).currentVal}")
        analogOut2Economizer = getDefaultValConfig(DomainName.analog2Economizer, model)
        analogOut2Recirculate = getDefaultValConfig(DomainName.analog2Recirculate, model)
        analogOut2CoolStage1 = getDefaultValConfig(DomainName.analog2CoolStage1, model)
        analogOut2CoolStage2 = getDefaultValConfig(DomainName.analog2CoolStage2, model)
        analogOut2CoolStage3 = getDefaultValConfig(DomainName.analog2CoolStage3, model)
        analogOut2CoolStage4 = getDefaultValConfig(DomainName.analog2CoolStage4, model)
        analogOut2CoolStage5 = getDefaultValConfig(DomainName.analog2CoolStage5, model)
        analogOut2HeatStage1 = getDefaultValConfig(DomainName.analog2HeatStage1, model)
        analogOut2HeatStage2 = getDefaultValConfig(DomainName.analog2HeatStage2, model)
        analogOut2HeatStage3 = getDefaultValConfig(DomainName.analog2HeatStage3, model)
        analogOut2HeatStage4 = getDefaultValConfig(DomainName.analog2HeatStage4, model)
        analogOut2HeatStage5 = getDefaultValConfig(DomainName.analog2HeatStage5, model)
        analogOut2Default = getDefaultValConfig(DomainName.analog2Default, model)
        analogOut2MinCompressor = getDefaultValConfig(DomainName.analog2MinCompressorSpeed, model)
        analogOut2MaxCompressor = getDefaultValConfig(DomainName.analog2MaxCompressorSpeed, model)
        analogOut2MinDamperModulation = getDefaultValConfig(DomainName.analog2MinDCVDamper, model)
        analogOut2MaxDamperModulation = getDefaultValConfig(DomainName.analog2MaxDCVDamper, model)

        unusedPorts = getAllUnusedPorts()
        isDefault = true
        return this
    }

    override fun getActiveConfiguration() : StagedVfdRtuProfileConfig {

        val equip = Domain.hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
        if (equip.isEmpty()) {
            return this
        }
        val stagedRtuEquip = VavStagedVfdSystemEquip(equip[Tags.ID].toString())

        getDefaultConfiguration()
        relay1Enabled.enabled = stagedRtuEquip.relay1OutputEnable.readDefaultVal() > 0
        relay2Enabled.enabled = stagedRtuEquip.relay2OutputEnable.readDefaultVal() > 0
        relay3Enabled.enabled = stagedRtuEquip.relay3OutputEnable.readDefaultVal() > 0
        relay4Enabled.enabled = stagedRtuEquip.relay4OutputEnable.readDefaultVal() > 0
        relay5Enabled.enabled = stagedRtuEquip.relay5OutputEnable.readDefaultVal() > 0
        relay6Enabled.enabled = stagedRtuEquip.relay6OutputEnable.readDefaultVal() > 0
        relay7Enabled.enabled = stagedRtuEquip.relay7OutputEnable.readDefaultVal() > 0


        thermistor1Enabled.enabled = stagedRtuEquip.thermistor1InputEnable.readDefaultVal() > 0
        thermistor2Enabled.enabled = stagedRtuEquip.thermistor2InputEnable.readDefaultVal() > 0
        analogIn1Enabled.enabled = stagedRtuEquip.analog1InputEnable.readDefaultVal() > 0
        analogIn2Enabled.enabled = stagedRtuEquip.analog2InputEnable.readDefaultVal() > 0

        analogOut2Enabled.enabled = stagedRtuEquip.analog2OutputEnable.readDefaultVal() > 0
        if (analogOut2Enabled.enabled) {
            analogOut2Association.associationVal = stagedRtuEquip.analog2OutputAssociation.readDefaultVal().toInt()
        }

        relay1Association.associationVal = if (relay1Enabled.enabled) {
            stagedRtuEquip.relay1OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay1OutputAssociation, model).associationVal
        }
        relay2Association.associationVal = if (relay2Enabled.enabled) {
            stagedRtuEquip.relay2OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay2OutputAssociation, model).associationVal
        }
        relay3Association.associationVal = if (relay3Enabled.enabled) {
            stagedRtuEquip.relay3OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay3OutputAssociation, model).associationVal
        }
        relay4Association.associationVal = if (relay4Enabled.enabled) {
            stagedRtuEquip.relay4OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay4OutputAssociation, model).associationVal
        }
        relay5Association.associationVal = if (relay5Enabled.enabled) {
            stagedRtuEquip.relay5OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay5OutputAssociation, model).associationVal
        }
        relay6Association.associationVal = if (relay6Enabled.enabled) {
            stagedRtuEquip.relay6OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay6OutputAssociation, model).associationVal
        }
        relay7Association.associationVal = if (relay7Enabled.enabled) {
            stagedRtuEquip.relay7OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay7OutputAssociation, model).associationVal
        }
        analogOut2Association.associationVal = if (analogOut2Enabled.enabled) {
            stagedRtuEquip.analog2OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.analog2OutputAssociation, model).associationVal
        }
        thermistor1InAssociation.associationVal = if (thermistor1Enabled.enabled) {
            stagedRtuEquip.thermistor1InputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.thermistor1InputAssociation, model).associationVal
        }
        thermistor2InAssociation.associationVal = if (thermistor2Enabled.enabled) {
            stagedRtuEquip.thermistor2InputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.thermistor2InputAssociation, model).associationVal
        }
        analogIn1Association.associationVal = if (analogIn1Enabled.enabled) {
            stagedRtuEquip.analog1InputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.analog1InputAssociation, model).associationVal
        }
        analogIn2Association.associationVal = if (analogIn2Enabled.enabled) {
            stagedRtuEquip.analog2InputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.analog2InputAssociation, model).associationVal
        }

        analogOut2Economizer.currentVal = getActivePointValue(stagedRtuEquip.analog2Economizer, analogOut2Economizer)
        analogOut2Recirculate.currentVal = getActivePointValue(stagedRtuEquip.analog2Recirculate, analogOut2Recirculate)
        analogOut2CoolStage1.currentVal = getActivePointValue(stagedRtuEquip.analog2CoolStage1, analogOut2CoolStage1)
        analogOut2CoolStage2.currentVal = getActivePointValue(stagedRtuEquip.analog2CoolStage2, analogOut2CoolStage2)
        analogOut2CoolStage3.currentVal = getActivePointValue(stagedRtuEquip.analog2CoolStage3, analogOut2CoolStage3)
        analogOut2CoolStage4.currentVal = getActivePointValue(stagedRtuEquip.analog2CoolStage4, analogOut2CoolStage4)
        analogOut2CoolStage5.currentVal = getActivePointValue(stagedRtuEquip.analog2CoolStage5, analogOut2CoolStage5)
        analogOut2HeatStage1.currentVal = getActivePointValue(stagedRtuEquip.analog2HeatStage1, analogOut2HeatStage1)
        analogOut2HeatStage2.currentVal = getActivePointValue(stagedRtuEquip.analog2HeatStage2, analogOut2HeatStage2)
        analogOut2HeatStage3.currentVal = getActivePointValue(stagedRtuEquip.analog2HeatStage3, analogOut2HeatStage3)
        analogOut2HeatStage4.currentVal = getActivePointValue(stagedRtuEquip.analog2HeatStage4, analogOut2HeatStage4)
        analogOut2HeatStage5.currentVal = getActivePointValue(stagedRtuEquip.analog2HeatStage5, analogOut2HeatStage5)
        analogOut2Default.currentVal = getActivePointValue(stagedRtuEquip.analog2Default, analogOut2Default)

        analogOut2MinCompressor.currentVal = getActivePointValue(stagedRtuEquip.analog2MinCompressorSpeed, analogOut2MinCompressor)
        analogOut2MaxCompressor.currentVal = getActivePointValue(stagedRtuEquip.analog2MaxCompressorSpeed, analogOut2MaxCompressor)
        analogOut2MinDamperModulation.currentVal = getActivePointValue(stagedRtuEquip.analog2MinDCVDamper, analogOut2MinDamperModulation)
        analogOut2MaxDamperModulation.currentVal = getActivePointValue(stagedRtuEquip.analog2MaxDCVDamper, analogOut2MaxDamperModulation)

        unusedPorts = ControlMote.getCMUnusedPorts(Domain.hayStack)
        isDefault = false
        return this
    }


    override fun getAssociationConfigs() : List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
            add(relay1Association)
            add(relay2Association)
            add(relay3Association)
            add(relay4Association)
            add(relay5Association)
            add(relay6Association)
            add(relay7Association)
            add(analogOut2Association)
            add(thermistor1InAssociation)
            add(thermistor2InAssociation)
            add(analogIn1Association)
            add(analogIn2Association)
        }
    }

    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(analogOut2Economizer)
            add(analogOut2Recirculate)
            add(analogOut2CoolStage1)
            add(analogOut2CoolStage2)
            add(analogOut2CoolStage3)
            add(analogOut2CoolStage4)
            add(analogOut2CoolStage5)
            add(analogOut2HeatStage1)
            add(analogOut2HeatStage2)
            add(analogOut2HeatStage3)
            add(analogOut2HeatStage4)
            add(analogOut2HeatStage5)
            add(analogOut2MinCompressor)
            add(analogOut2MaxCompressor)
            add(analogOut2MinDamperModulation)
            add(analogOut2MaxDamperModulation)
            add(analogOut2Default)
        }
    }

    override fun getEnableConfigs() : List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(relay1Enabled)
            add(relay2Enabled)
            add(relay3Enabled)
            add(relay4Enabled)
            add(relay5Enabled)
            add(relay6Enabled)
            add(relay7Enabled)
            add(analogOut2Enabled)
            add(thermistor1Enabled)
            add(thermistor2Enabled)
            add(analogIn1Enabled)
            add(analogIn2Enabled)
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(analogOut2Economizer)
            add(analogOut2Recirculate)
            add(analogOut2CoolStage1)
            add(analogOut2CoolStage2)
            add(analogOut2CoolStage3)
            add(analogOut2CoolStage4)
            add(analogOut2CoolStage5)
            add(analogOut2HeatStage1)
            add(analogOut2HeatStage2)
            add(analogOut2HeatStage3)
            add(analogOut2HeatStage4)
            add(analogOut2HeatStage5)
            add(analogOut2MinCompressor)
            add(analogOut2MaxCompressor)
            add(analogOut2MinDamperModulation)
            add(analogOut2MaxDamperModulation)
            add(analogOut2Default)
        }
    }



    fun isAnyAnalogOut2EnabledAndMapped(mapping: Int): Boolean {
        return (analogOut2Enabled.enabled && analogOut2Association.associationVal == mapping)
    }

    override fun toString(): String {
        return "StagedVfdRtuProfileConfig(" +
                "relay1Enabled=${relay1Enabled.enabled}, relay2Enabled=${relay2Enabled.enabled}, relay3Enabled=${relay3Enabled.enabled}, " +
                "relay4Enabled=${relay4Enabled.enabled}, relay5Enabled=${relay5Enabled.enabled}, relay6Enabled=${relay6Enabled.enabled}, " +
                "relay7Enabled=${relay7Enabled.enabled}, analogOut2Enabled=${analogOut2Enabled.enabled}, " +
                "analogOut2Association=${analogOut2Association.associationVal}, " +
                "thermistor1Enabled=${thermistor1Enabled.enabled}, thermistor2Enabled=${thermistor2Enabled.enabled}, " +
                "thermistor1Association=${thermistor1InAssociation.associationVal}, thermistor2Association=${thermistor2InAssociation.associationVal}, " +
                "analogIn1Enabled=${analogIn1Enabled.enabled}, analogIn2Enabled=${analogIn2Enabled.enabled}, " +
                "analogIn1Association=${analogIn1Association.associationVal}, analogIn2Association=${analogIn2Association.associationVal}, " +
                "analogOut2Economizer=${analogOut2Economizer.currentVal}, analogOut2Recirculate=${analogOut2Recirculate.currentVal}, " +
                "analogOut2CoolStage1=${analogOut2CoolStage1.currentVal}, analogOut2CoolStage2=${analogOut2CoolStage2.currentVal}, " +
                "analogOut2CoolStage3=${analogOut2CoolStage3.currentVal}, analogOut2CoolStage4=${analogOut2CoolStage4.currentVal}, " +
                "analogOut2CoolStage5=${analogOut2CoolStage5.currentVal}, analogOut2HeatStage1=${analogOut2HeatStage1.currentVal}, " +
                "analogOut2HeatStage2=${analogOut2HeatStage2.currentVal}, analogOut2HeatStage3=${analogOut2HeatStage3.currentVal}, " +
                "analogOut2HeatStage4=${analogOut2HeatStage4.currentVal}, analogOut2HeatStage5=${analogOut2HeatStage5.currentVal}, " +
                "analogOut2Default=${analogOut2Default.currentVal}, " +
                "analogOut2MinCompressor=${analogOut2MinCompressor.currentVal}, " +
                "analogOut2MaxCompressor=${analogOut2MaxCompressor.currentVal}, " +
                "analogOut2MinDamperModulation=${analogOut2MinDamperModulation.currentVal}, " +
                "analogOut2MaxDamperModulation=${analogOut2MaxDamperModulation.currentVal}, " +
                "unusedPorts=$unusedPorts, isDefault=$isDefault)"

    }

}