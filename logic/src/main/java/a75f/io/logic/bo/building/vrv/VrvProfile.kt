package a75f.io.logic.bo.building.vrv

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.ZoneProfile
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.haystack.device.HyperStatDevice
import java.util.*

class VrvProfile : ZoneProfile() {

    lateinit var vrvEquip: VrvEquip

    fun createVrvEquip(hayStack : CCUHsApi,
                              addr : Short,
                              config : VrvProfileConfiguration,
                              roomRef: String,
                              floorRef: String
                              ) {
        vrvEquip = VrvEquip(hayStack, addr)
        vrvEquip.createEntities(config, roomRef, floorRef)
    }

    fun addEquip(hayStack : CCUHsApi,
                        addr : Short
                        ) {
        vrvEquip = VrvEquip(hayStack, addr)
        doMigration(hayStack, addr)
    }

    fun updateEquip(config: VrvProfileConfiguration) {
        vrvEquip.update(config)
    }
    override fun getProfileType(): ProfileType {
        return ProfileType.HYPERSTAT_VRV
    }

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        return vrvEquip.getProfileConfiguration() as T
    }

    override fun getNodeAddresses(): Set<Short?> {
        return object : HashSet<Short?>() {
            init {
                add(vrvEquip.nodeAddr)
            }
        }
    }

    override fun getEquip(): Equip? {
        val equip = CCUHsApi.getInstance().read("equip and group == \"" + vrvEquip.nodeAddr + "\"")
        return Equip.Builder().setHashMap(equip).build()
    }

    override fun updateZonePoints() {
        CcuLog.i(L.TAG_CCU_ZONE, " updateZonePoints VRV "+vrvEquip.nodeAddr)
    }

    /**
     * Deletes occupancy sensor which was incorrectly mapped to occupancy point in the earlier version.
     * This code could be removed once all the CCUs are upgraded to 1.581.
     */
    private fun doMigration(hayStack: CCUHsApi, addr: Short) {
        val node = HyperStatDevice(addr.toInt())
        val occupancyPort = node.getRawPoint(Port.SENSOR_OCCUPANCY)
        occupancyPort?.let {
            val occupancyPoint = hayStack.read("point and zone and occupancy and mode and group == \"$addr\"")
            if (occupancyPoint["id"] == occupancyPort.pointRef) {
                hayStack.deleteEntity(occupancyPort.id)
            }
        }
    }
}