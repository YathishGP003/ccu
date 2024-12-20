package a75f.io.logic.bo.building.hyperstatmonitoring

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.equips.hyperstat.MonitoringEquip
import a75f.io.domain.util.ModelLoader.getModelForDomainName
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.ZoneProfile
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.v2.configs.MonitoringConfiguration
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

//TODO-AMAR , Class name needs to be renamed from HyperStatV2MonitoringProfile
// to HyperStatV2MonitoringProfile after temporary override page DM integration is done
class HyperStatV2MonitoringProfile(val equipRef: String, val nodeAddress: Short)
    : ZoneProfile() {
    private var monitoringEquip : MonitoringEquip? = null
    override fun updateZonePoints() {
        CcuLog.i("HyperStatV2MonitoringProfile", "Update zones not required for monitoring profile")
    }

    override fun getProfileType(): ProfileType {
        return ProfileType.HYPERSTAT_MONITORING
    }

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        return BaseProfileConfiguration() as T
    }

    fun addHyperStatMonitoringEquip() {
        monitoringEquip = MonitoringEquip(equipRef)
    }

    override fun getNodeAddresses() : HashSet<Short> {
        val nodeSet : HashSet<Short> = HashSet()
        nodeSet.add(nodeAddress)
        return nodeSet
    }

    override fun getDomainProfileConfiguration(): MonitoringConfiguration {
        val equip = equip
        return  MonitoringConfiguration(
            nodeAddress.toInt(), NodeType.HYPER_STAT.name, 0,
            equip.id, equip.floorRef , profileType,
            (getModelForDomainName(equip.domainName) as SeventyFiveFProfileDirective)
        ).getDefaultConfiguration()
    }

    override fun getEquip(): Equip {
            return Equip.Builder().setHashMap(CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")).build()
    }
}
