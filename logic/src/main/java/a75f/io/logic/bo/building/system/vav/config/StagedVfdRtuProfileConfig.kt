package a75f.io.logic.bo.building.system.vav.config

import a75f.io.domain.equips.StagedRtuEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.VavStagedSystemEquip
import a75f.io.domain.equips.VavStagedVfdSystemEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.haystack.device.ControlMote
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

open class StagedVfdRtuProfileConfig(modelDef : SeventyFiveFProfileDirective)
                        : StagedRtuProfileConfig(modelDef){

    lateinit var analogOut2Enabled : EnableConfig
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

    override fun getDefaultConfiguration() : StagedVfdRtuProfileConfig {
        relay1Enabled = getDefaultEnableConfig(DomainName.relay1OutputEnable, model)
        relay2Enabled = getDefaultEnableConfig(DomainName.relay2OutputEnable, model)
        relay3Enabled = getDefaultEnableConfig(DomainName.relay3OutputEnable, model)
        relay4Enabled = getDefaultEnableConfig(DomainName.relay4OutputEnable, model)
        relay5Enabled = getDefaultEnableConfig(DomainName.relay5OutputEnable, model)
        relay6Enabled = getDefaultEnableConfig(DomainName.relay6OutputEnable, model)
        relay7Enabled = getDefaultEnableConfig(DomainName.relay7OutputEnable, model)

        relay1Association = getDefaultAssociationConfig(DomainName.relay1OutputAssociation, model)
        relay2Association = getDefaultAssociationConfig(DomainName.relay2OutputAssociation, model)
        relay3Association = getDefaultAssociationConfig(DomainName.relay3OutputAssociation, model)
        relay4Association = getDefaultAssociationConfig(DomainName.relay4OutputAssociation, model)
        relay5Association = getDefaultAssociationConfig(DomainName.relay5OutputAssociation, model)
        relay6Association = getDefaultAssociationConfig(DomainName.relay6OutputAssociation, model)
        relay7Association = getDefaultAssociationConfig(DomainName.relay7OutputAssociation, model)

        analogOut2Enabled = getDefaultEnableConfig(DomainName.analog2OutputEnable, model)
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
        try {
            unusedPorts = ControlMote.getCMUnusedPorts(Domain.hayStack)
        } catch (e : NullPointerException) {
            unusedPorts = hashMapOf()
            CcuLog.e(Domain.LOG_TAG,"Failed to fetch CM Unused ports")
            e.printStackTrace()
        }

        isDefault = true
        return this
    }

    override fun getActiveConfiguration() : StagedVfdRtuProfileConfig {

        val equip = Domain.hayStack.readEntity("system and equip and not modbus and not connectModule")
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

        analogOut2Enabled.enabled = stagedRtuEquip.analog2OutputEnable.readDefaultVal() > 0
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
        analogOut2Economizer.currentVal = stagedRtuEquip.analog2Economizer.readDefaultVal()
        analogOut2Recirculate.currentVal = stagedRtuEquip.analog2Recirculate.readDefaultVal()
        analogOut2CoolStage1.currentVal = stagedRtuEquip.analog2CoolStage1.readDefaultVal()
        analogOut2CoolStage2.currentVal = stagedRtuEquip.analog2CoolStage2.readDefaultVal()
        analogOut2CoolStage3.currentVal = stagedRtuEquip.analog2CoolStage3.readDefaultVal()
        analogOut2CoolStage4.currentVal = stagedRtuEquip.analog2CoolStage4.readDefaultVal()
        analogOut2CoolStage5.currentVal = stagedRtuEquip.analog2CoolStage5.readDefaultVal()
        analogOut2HeatStage1.currentVal = stagedRtuEquip.analog2HeatStage1.readDefaultVal()
        analogOut2HeatStage2.currentVal = stagedRtuEquip.analog2HeatStage2.readDefaultVal()
        analogOut2HeatStage3.currentVal = stagedRtuEquip.analog2HeatStage3.readDefaultVal()
        analogOut2HeatStage4.currentVal = stagedRtuEquip.analog2HeatStage4.readDefaultVal()
        analogOut2HeatStage5.currentVal = stagedRtuEquip.analog2HeatStage5.readDefaultVal()
        analogOut2Default.currentVal = stagedRtuEquip.analog2Default.readDefaultVal()
        try {
            unusedPorts = ControlMote.getCMUnusedPorts(Domain.hayStack)
        } catch (e : NullPointerException) {
            unusedPorts = hashMapOf()
            CcuLog.e(Domain.LOG_TAG,"Failed to fetch CM Unused ports")
            e.printStackTrace()
        }
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
        }
    }

    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf()
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
            add(analogOut2Default)
        }
    }

    override fun toString(): String {
        return " relay1Enabled ${relay1Enabled.enabled} relay2Enabled ${relay2Enabled.enabled} relay3Enabled ${relay3Enabled.enabled}" +
                " relay4Enabled ${relay4Enabled.enabled} relay5Enabled ${relay5Enabled.enabled} relay6Enabled ${relay6Enabled.enabled}" +
                " relay7Enabled ${relay7Enabled.enabled}  relay1Association ${relay1Association.associationVal}" +
                " relay2Association ${relay2Association.associationVal} relay3Association ${relay3Association.associationVal}" +
                " relay4Association ${relay4Association.associationVal} relay5Association ${relay5Association.associationVal}" +
                " relay6Association ${relay6Association.associationVal} relay7Association ${relay7Association.associationVal} " +
                " analogOut2Enabled ${analogOut2Enabled.enabled} analogOut2Economizer ${analogOut2Economizer.currentVal}" +
                " analogOut2Recirculate ${analogOut2Recirculate.currentVal} analogOut2CoolStage1 ${analogOut2CoolStage1.currentVal}" +
                " analogOut2CoolStage2 ${analogOut2CoolStage2.currentVal} analogOut2CoolStage3 ${analogOut2CoolStage3.currentVal}" +
                " analogOut2CoolStage4 ${analogOut2CoolStage4.currentVal} analogOut2CoolStage5 ${analogOut2CoolStage5.currentVal}" +
                " analogOut2HeatStage1 ${analogOut2HeatStage1.currentVal} analogOut2HeatStage2 ${analogOut2HeatStage2.currentVal}" +
                " analogOut2HeatStage3 ${analogOut2HeatStage3.currentVal} analogOut2HeatStage4 ${analogOut2HeatStage4.currentVal}" +
                " analogOut2HeatStage5 ${analogOut2HeatStage5.currentVal} analogOut2Default ${analogOut2Default.currentVal} "
    }

}