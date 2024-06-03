package a75f.io.logic.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.VavEquip
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.definitions.ReheatType
import a75f.io.logic.bo.building.vav.VavProfileConfiguration
import a75f.io.logic.bo.haystack.device.DeviceUtil
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

class VavAndAcbProfileMigration {
    companion object {
        fun migrateVavAndAcbProfilesToCorrectPortEnabledStatus(ccuHsApi: CCUHsApi) {
            CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "Migration to correct portEnabled status")
            val models = mapOf(
                NodeType.SMART_NODE to mapOf(
                    ProfileType.VAV_SERIES_FAN to ModelLoader.getSmartNodeVavSeriesModelDef(),
                    ProfileType.VAV_PARALLEL_FAN to ModelLoader.getSmartNodeVavParallelFanModelDef(),
                    ProfileType.VAV_REHEAT to ModelLoader.getSmartNodeVavNoFanModelDef(),
                    ProfileType.VAV_ACB to ModelLoader.getSmartNodeVavAcbModelDef()
                ),
                NodeType.HELIO_NODE to mapOf(
                    ProfileType.VAV_SERIES_FAN to ModelLoader.getHelioNodeVavSeriesModelDef(),
                    ProfileType.VAV_PARALLEL_FAN to ModelLoader.getHelioNodeVavParallelFanModelDef(),
                    ProfileType.VAV_REHEAT to ModelLoader.getHelioNodeVavNoFanModelDef(),
                    ProfileType.VAV_ACB to ModelLoader.getHelioNodeVavAcbModelDef()
                )
            )

            val vavEquips = ccuHsApi.readAllEntities("equip and vav and not system")

            vavEquips.forEach { equipMap ->
                CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "Migrate -> " + equipMap["dis"].toString())
                val equip = Equip.Builder().setHashMap(equipMap).build()
                val domainEquip = VavEquip(equip.id)
                val nodeType =
                    if (equip.domainName.contains(Tags.HELIO_NODE)) NodeType.HELIO_NODE else NodeType.SMART_NODE
                val address: Short = equip.group.toShort()
                val devicePorts = DeviceUtil.getPortsForDevice(address, ccuHsApi)
                val deviceModel = models[nodeType]?.get(ProfileType.valueOf(equip.profile))

                devicePorts?.forEach { port ->
                    val entityMapper = EntityMapper(deviceModel as SeventyFiveFProfileDirective)
                    val profileConfiguration = VavProfileConfiguration(
                        Integer.parseInt(equip.group),
                        nodeType.name,
                        domainEquip.zonePriority.readPriorityVal().toInt(),
                        equip.roomRef,
                        equip.floorRef,
                        ProfileType.valueOf(equip.profile),
                        deviceModel
                    ).getActiveConfiguration()

                    val logicalPointRefName = entityMapper.getPhysicalProfilePointRef(
                        profileConfiguration,
                        port.domainName
                    )
                    if (logicalPointRefName == null && port.enabled && !isRelay1PortIsNotEnabled(
                            port)) {
                        port.enabled = false
                        ccuHsApi.updatePoint(port, port.id)
                        CcuLog.i(
                            L.TAG_CCU_MIGRATION_UTIL,
                            "Port disabled for : ${port.displayName}"
                        )
                    } else if (logicalPointRefName != null && !port.enabled &&
                        isAnalogPortMappedToReheatType(port, ccuHsApi, equip)) {
                        port.enabled = true
                        ccuHsApi.updatePoint(port, port.id)
                        CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "Port enabled: ${port.displayName}")
                    }
                }
            }
        }

        /*For Vav-Parallel profile relay1 is enabled but pointRef is null*/
        private fun isRelay1PortIsNotEnabled(port: RawPoint): Boolean {
            return port.domainName == DomainName.relay1 && port.enabled
        }

        /*Analog-out2 is mapped to reheatType all the time, even if reheatType is mapped to relay1*/
        private fun isAnalogPortMappedToReheatType(
            port: RawPoint,
            hayStack: CCUHsApi,
            equip: Equip
        ): Boolean {
            if (port.domainName == DomainName.analog2Out) {
                val reheatTypePoint =
                    hayStack.readEntity("point and group == \"" + equip.group + "\" and domainName == \"" + DomainName.reheatType + "\"")
                val reheatType =
                    hayStack.readDefaultValById(reheatTypePoint["id"].toString()).toInt() - 1
                return reheatType == ReheatType.ZeroToTenV.ordinal ||
                        reheatType == ReheatType.TwoToTenV.ordinal ||
                        reheatType == ReheatType.TenToZeroV.ordinal ||
                        reheatType == ReheatType.TenToTwov.ordinal ||
                        reheatType == ReheatType.Pulse.ordinal
            } else {
                return true
            }
        }
    }
}