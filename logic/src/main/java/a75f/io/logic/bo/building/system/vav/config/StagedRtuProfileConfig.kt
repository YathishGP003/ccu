package a75f.io.logic.bo.building.system.vav.config

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.StagedRtuEquip
import a75f.io.domain.util.CommonQueries
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.bo.haystack.device.ControlMote.getAllUnusedPorts
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

open class StagedRtuProfileConfig(val model : SeventyFiveFProfileDirective)
    : ProfileConfiguration(99, "",0, "SYSTEM","SYSTEM", model.domainName) {

    lateinit var relay1Enabled: EnableConfig
    lateinit var relay2Enabled: EnableConfig
    lateinit var relay3Enabled: EnableConfig
    lateinit var relay4Enabled: EnableConfig
    lateinit var relay5Enabled: EnableConfig
    lateinit var relay6Enabled: EnableConfig
    lateinit var relay7Enabled: EnableConfig

    lateinit var thermistor1Enabled : EnableConfig
    lateinit var thermistor2Enabled : EnableConfig
    lateinit var analogIn1Enabled : EnableConfig
    lateinit var analogIn2Enabled : EnableConfig

    lateinit var relay1Association: AssociationConfig
    lateinit var relay2Association: AssociationConfig
    lateinit var relay3Association: AssociationConfig
    lateinit var relay4Association: AssociationConfig
    lateinit var relay5Association: AssociationConfig
    lateinit var relay6Association: AssociationConfig
    lateinit var relay7Association: AssociationConfig

    lateinit var thermistor1InAssociation: AssociationConfig
    lateinit var thermistor2InAssociation: AssociationConfig
    lateinit var analogIn1Association: AssociationConfig
    lateinit var analogIn2Association: AssociationConfig



    lateinit var unusedPorts: HashMap<String, Boolean>

    open fun getDefaultConfiguration() : StagedRtuProfileConfig {
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
        thermistor1InAssociation = getDefaultAssociationConfig(DomainName.thermistor1InputAssociation, model)
        thermistor2InAssociation = getDefaultAssociationConfig(DomainName.thermistor2InputAssociation, model)
        analogIn1Association = getDefaultAssociationConfig(DomainName.analog1InputAssociation, model)
        analogIn2Association = getDefaultAssociationConfig(DomainName.analog2InputAssociation, model)


        unusedPorts = getAllUnusedPorts()

        isDefault = true
        return this
    }

    open fun getActiveConfiguration() : StagedRtuProfileConfig {

        val equip = Domain.hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
        if (equip.isEmpty()) {
            return this
        }
        val stagedRtuEquip = StagedRtuEquip(equip[Tags.ID].toString())

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
            add(thermistor1InAssociation)
            add(thermistor2InAssociation)
            add(analogIn1Association)
            add(analogIn2Association)
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
            add(thermistor1Enabled)
            add(thermistor2Enabled)
            add(analogIn1Enabled)
            add(analogIn2Enabled)
        }
    }

    override fun toString(): String {
        return " relay1Enabled ${relay1Enabled.enabled} relay2Enabled ${relay2Enabled.enabled} relay3Enabled ${relay3Enabled.enabled}" +
                " relay4Enabled ${relay4Enabled.enabled} relay5Enabled ${relay5Enabled.enabled} relay6Enabled ${relay6Enabled.enabled}" +
                " relay7Enabled ${relay7Enabled.enabled}  relay1Association ${relay1Association.associationVal}" +
                " relay2Association ${relay2Association.associationVal} relay3Association ${relay3Association.associationVal}" +
                " relay4Association ${relay4Association.associationVal} relay5Association ${relay5Association.associationVal}" +
                " relay6Association ${relay6Association.associationVal} relay7Association ${relay7Association.associationVal} "
    }


    fun isAnyRelayEnabledAndMapped(mapping: Int): Boolean {
        return (relay1Enabled.enabled && relay1Association.associationVal == mapping) ||
                (relay2Enabled.enabled && relay2Association.associationVal == mapping) ||
                (relay3Enabled.enabled && relay3Association.associationVal == mapping) ||
                (relay4Enabled.enabled && relay4Association.associationVal == mapping) ||
                (relay5Enabled.enabled && relay5Association.associationVal == mapping) ||
                (relay6Enabled.enabled && relay6Association.associationVal == mapping) ||
                (relay7Enabled.enabled && relay7Association.associationVal == mapping)
    }
}