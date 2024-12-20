package a75f.io.logic.bo.building.hyperstat.v2.configs

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.hyperstat.MonitoringEquip
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

class MonitoringConfiguration(
    nodeAddress: Int, nodeType: String, priority: Int, roomRef: String,
    floorRef: String, profileType: ProfileType, model: SeventyFiveFProfileDirective
) : HyperStatConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType, model) {


    override fun getDefaultConfiguration(): MonitoringConfiguration {
        temperatureOffset = getDefaultValConfig(DomainName.temperatureOffset, model)

        thermistor1Enabled = getDefaultEnableConfig(DomainName.thermistor1InputEnable, model)
        thermistor2Enabled = getDefaultEnableConfig(DomainName.thermistor2InputEnable, model)
        analogIn1Enabled = getDefaultEnableConfig(DomainName.analog1InputEnable, model)
        analogIn2Enabled = getDefaultEnableConfig(DomainName.analog2InputEnable, model)

        thermistor1Association =
            getDefaultAssociationConfig(DomainName.thermistor1InputAssociation, model)
        thermistor2Association =
            getDefaultAssociationConfig(DomainName.thermistor2InputAssociation, model)
        analogIn1Association =
            getDefaultAssociationConfig(DomainName.analog1InputAssociation, model)
        analogIn2Association =
            getDefaultAssociationConfig(DomainName.analog2InputAssociation, model)

        zoneCO2Target = getDefaultValConfig(DomainName.co2Target, model)
        zonePM2p5Target = getDefaultValConfig(DomainName.pm25Target, model)
        zonePM10Target = getDefaultValConfig(DomainName.pm10Target, model)
        isDefault = true
        return this
    }

    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf()
    }

    override fun getActiveConfiguration(): MonitoringConfiguration {
        val equipMap = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equipMap.isEmpty()) {
            return this
        }
        val monitoringEquip = MonitoringEquip(equipMap[Tags.ID].toString())
        getDefaultConfiguration()

        temperatureOffset.currentVal = monitoringEquip.tempOffset.readDefaultVal()
        thermistor1Enabled.enabled = monitoringEquip.thermistor1Enabled.readDefaultVal() > 0
        thermistor2Enabled.enabled = monitoringEquip.thermistor2Enabled.readDefaultVal() > 0
        analogIn1Enabled.enabled = monitoringEquip.analogIn1Enabled.readDefaultVal() > 0
        analogIn2Enabled.enabled = monitoringEquip.analogIn2Enabled.readDefaultVal() > 0

        thermistor1Association.associationVal =
            monitoringEquip.thermistor1Association.readDefaultVal().toInt()
        thermistor2Association.associationVal =
            monitoringEquip.thermistor2Association.readDefaultVal().toInt()
        analogIn1Association.associationVal =
            monitoringEquip.analogIn1Association.readDefaultVal().toInt()
        analogIn2Association.associationVal =
            monitoringEquip.analogIn2Association.readDefaultVal().toInt()

        zoneCO2Target.currentVal = monitoringEquip.co2Target.readDefaultVal()
        zonePM2p5Target.currentVal = monitoringEquip.pm25Target.readDefaultVal()
        zonePM10Target.currentVal = monitoringEquip.pm10Target.readDefaultVal()
        isDefault = false
        return this
    }
    override fun getAssociationConfigs() : List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
            add(thermistor1Association)
            add(thermistor2Association)
            add(analogIn1Association)
            add(analogIn2Association)
        }
    }

    override fun getEnableConfigs() : List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(analogIn1Enabled)
            add(analogIn2Enabled)
            add(thermistor1Enabled)
            add(thermistor2Enabled)
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(temperatureOffset)
            add(zoneCO2Target)
            add(zonePM2p5Target)
            add(zonePM10Target)
        }
    }
}