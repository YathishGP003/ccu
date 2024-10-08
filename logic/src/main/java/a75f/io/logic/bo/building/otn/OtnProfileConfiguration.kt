package a75f.io.logic.bo.building.otn

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.OtnEquip
import a75f.io.domain.equips.VavEquip
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.vav.VavProfileConfiguration
import a75f.io.logic.bo.haystack.device.DeviceUtil
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

class OtnProfileConfiguration(nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String, profileType : ProfileType, val model : SeventyFiveFProfileDirective)
    : ProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType.name)  {

    lateinit var temperatureOffset: ValueConfig
    lateinit var zonePriority: ValueConfig

    lateinit var autoForceOccupied: EnableConfig
    lateinit var autoAway: EnableConfig


    fun getDefaultConfiguration() : OtnProfileConfiguration {
        zonePriority = getDefaultValConfig(DomainName.zonePriority, model)
        autoAway = getDefaultEnableConfig(DomainName.autoawayEnable, model) //TODO
        autoForceOccupied = getDefaultEnableConfig(DomainName.autoForceOccupiedEnable, model) //TODO
        temperatureOffset = getDefaultValConfig(DomainName.temperatureOffset, model)
        isDefault = true

        return this
    }


    fun getActiveConfiguration() : OtnProfileConfiguration {

        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val otnEquip = OtnEquip(equip[Tags.ID].toString())

        getDefaultConfiguration()
        zonePriority.currentVal = otnEquip.zonePriority.readPriorityVal()

        autoAway.enabled = otnEquip.autoAway.readDefaultVal() > 0
        autoForceOccupied.enabled = otnEquip.autoForceOccupied.readDefaultVal() > 0

        temperatureOffset.currentVal = otnEquip.temperatureOffset.readDefaultVal()
        isDefault = false
        return this
    }


    override fun getAssociationConfigs() : List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
        }
    }

    override fun getDependencies(): List<ValueConfig> {
        TODO("Not yet implemented")
    }

    override fun getEnableConfigs() : List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(autoAway)
            add(autoForceOccupied)
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(zonePriority)
            add(temperatureOffset)
        }
    }
}