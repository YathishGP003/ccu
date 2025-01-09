package a75f.io.logic.migration


import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.domain.VavAcbEquip
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.VavEquip
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.PointBuilderConfig
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.definitions.ReheatType
import a75f.io.logic.bo.building.vav.AcbProfileConfiguration
import a75f.io.logic.bo.building.vav.VavProfileConfiguration
import a75f.io.logic.bo.haystack.device.DeviceUtil
import a75f.io.logic.util.PreferenceUtil
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef

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
                    val profileConfiguration = if (ProfileType.valueOf(equip.profile) == ProfileType.VAV_ACB) {
                        AcbProfileConfiguration(
                            Integer.parseInt(equip.group),
                            nodeType.name,
                            domainEquip.zonePriority.readPriorityVal().toInt(),
                            equip.roomRef,
                            equip.floorRef,
                            ProfileType.valueOf(equip.profile),
                            deviceModel
                        ).getActiveConfiguration()
                    } else {
                        VavProfileConfiguration(
                            Integer.parseInt(equip.group),
                            nodeType.name,
                            domainEquip.zonePriority.readPriorityVal().toInt(),
                            equip.roomRef,
                            equip.floorRef,
                            ProfileType.valueOf(equip.profile),
                            deviceModel
                        ).getActiveConfiguration()
                    }

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
                    } else if (port.enabled && isAcbValveNotInstalled(port, ccuHsApi, equip)) {
                        port.enabled = false
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

        private fun isAcbValveNotInstalled(
            port: RawPoint,
            hayStack: CCUHsApi,
            equip: Equip
        ): Boolean {
            if (ProfileType.valueOf(equip.profile) == ProfileType.VAV_ACB && port.domainName.equals(DomainName.analog2Out)) {
                val valveTypePoint =
                    hayStack.readEntity("point and group == \"" + equip.group + "\" and domainName == \"" + DomainName.valveType + "\"")
                val valveType =
                    hayStack.readDefaultValById(valveTypePoint["id"].toString()).toInt()
                return valveType == 0
            }
            return false
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

        fun verifyACBIsoValveLogicalPoints(hayStack: CCUHsApi) {
            val equipBuilder = ProfileEquipBuilder(hayStack)
            val site = hayStack.site

            val hnAcbModel = ModelLoader.getHelioNodeVavAcbModelDef() as SeventyFiveFProfileDirective
            val hnAcbEquips = hayStack.readAllEntities("equip and domainName == \"" + DomainName.helionodeActiveChilledBeam + "\"")
            hnAcbEquips.forEach { hnEquip ->
                val acbEquip = VavAcbEquip(hnEquip["id"].toString())
                val device = hayStack.readEntity("device and equipRef == \"" + hnEquip["id"].toString() + "\"")

                val profileConfiguration = AcbProfileConfiguration(
                        Integer.parseInt(hnEquip["group"].toString()),
                        NodeType.HELIO_NODE.name,
                        0,
                        hnEquip["roomRef"].toString(),
                        hnEquip["floorRef"].toString(),
                        ProfileType.VAV_ACB,
                        hnAcbModel
                ).getActiveConfiguration()

                val equipDis = "${site?.displayName}-ACB-${hnEquip["group"]}"


                if (acbEquip.relay1OutputAssociation.readDefaultVal().toInt() == 0 && !acbEquip.chilledWaterValveIsolationCmdPointNC.pointExists()) {
                    val modelPointDef = hnAcbModel.points.find { it.domainName.contentEquals(DomainName.chilledWaterValveIsolationCmdPointNC) } as SeventyFiveFProfilePointDef
                    modelPointDef.let {
                        equipBuilder.createPoint(
                                PointBuilderConfig(
                                        it,
                                        profileConfiguration,
                                        hnEquip["id"].toString(),
                                        site!!.id,
                                        hayStack.timeZone,
                                        equipDis
                                )
                        )
                        // This is wrapped in a pointExists check because getID() will not re-check an empty point ID on its own. pointExists() will force a re-check.
                        if (acbEquip.chilledWaterValveIsolationCmdPointNC.pointExists()) {
                            val relay1 = hayStack.readEntity("point and deviceRef == \"" + device["id"].toString() + "\" and domainName == \"" + DomainName.relay1 + "\"")
                            val relay1Point = RawPoint.Builder().setHashMap(relay1).setPointRef(acbEquip.chilledWaterValveIsolationCmdPointNC.id).build()
                            hayStack.updatePoint(relay1Point, relay1Point.id)
                        }
                    }

                } else if (acbEquip.relay1OutputAssociation.readDefaultVal().toInt() == 1 && !acbEquip.chilledWaterValveIsolationCmdPointNO.pointExists()) {
                    val modelPointDef = hnAcbModel.points.find { it.domainName.contentEquals(DomainName.chilledWaterValveIsolationCmdPointNO) } as SeventyFiveFProfilePointDef
                    modelPointDef.let {
                        equipBuilder.createPoint(
                                PointBuilderConfig(
                                        it,
                                        profileConfiguration,
                                        hnEquip["id"].toString(),
                                        site!!.id,
                                        hayStack.timeZone,
                                        equipDis
                                )
                        )
                        // This is wrapped in a pointExists check because getID() will not re-check an empty point ID on its own. pointExists() will force a re-check.
                        if (acbEquip.chilledWaterValveIsolationCmdPointNO.pointExists()) {
                            val relay1 = hayStack.readEntity("point and deviceRef == \"" + device["id"].toString() + "\" and domainName == \"" + DomainName.relay1 + "\"")
                            val relay1Point = RawPoint.Builder().setHashMap(relay1).setPointRef(acbEquip.chilledWaterValveIsolationCmdPointNO.id).build()
                            hayStack.updatePoint(relay1Point, relay1Point.id)
                        }
                    }

                }

                val oldValveCmdPoint = hayStack.readEntity("point and equipRef == \"" + hnEquip["id"].toString() + "\" and domainName == \"" + "chilledWaterValveIsolationCmdPoint" + "\"")
                if (oldValveCmdPoint.isNotEmpty()) {
                    hayStack.deleteEntity(oldValveCmdPoint["id"].toString())
                }

            }

            val snAcbModel = ModelLoader.getSmartNodeVavAcbModelDef() as SeventyFiveFProfileDirective
            val snAcbEquips = hayStack.readAllEntities("equip and domainName == \"" + DomainName.smartnodeActiveChilledBeam + "\"")
            snAcbEquips.forEach {snEquip ->
                val acbEquip = VavAcbEquip(snEquip["id"].toString())
                val device = hayStack.readEntity("device and equipRef == \"" + snEquip["id"].toString() + "\"")

                val profileConfiguration = AcbProfileConfiguration(
                        Integer.parseInt(snEquip["group"].toString()),
                        NodeType.SMART_NODE.name,
                        0,
                        snEquip["roomRef"].toString(),
                        snEquip["floorRef"].toString(),
                        ProfileType.VAV_ACB,
                        snAcbModel
                ).getActiveConfiguration()

                val equipDis = "${site?.displayName}-ACB-${snEquip["group"]}"

                if (acbEquip.relay1OutputAssociation.readDefaultVal().toInt() == 0 && !acbEquip.chilledWaterValveIsolationCmdPointNC.pointExists()) {
                    val modelPointDef = snAcbModel.points.find { it.domainName.contentEquals(DomainName.chilledWaterValveIsolationCmdPointNC) } as SeventyFiveFProfilePointDef
                    modelPointDef.let {
                        equipBuilder.createPoint(
                                PointBuilderConfig(
                                        it,
                                        profileConfiguration,
                                        snEquip["id"].toString(),
                                        site!!.id,
                                        hayStack.timeZone,
                                        equipDis
                                )
                        )
                        // This is wrapped in a pointExists check because getID() will not re-check an empty point ID on its own. pointExists() will force a re-check.
                        if (acbEquip.chilledWaterValveIsolationCmdPointNC.pointExists()) {
                            val relay1 = hayStack.readEntity("point and deviceRef == \"" + device["id"].toString() + "\" and domainName == \"" + DomainName.relay1 + "\"")
                            val relay1Point = RawPoint.Builder().setHashMap(relay1).setPointRef(acbEquip.chilledWaterValveIsolationCmdPointNC.id).build()
                            hayStack.updatePoint(relay1Point, relay1Point.id)
                        }
                    }

                } else if (acbEquip.relay1OutputAssociation.readDefaultVal().toInt() == 1 && !acbEquip.chilledWaterValveIsolationCmdPointNO.pointExists()) {
                    val modelPointDef = snAcbModel.points.find { it.domainName.contentEquals(DomainName.chilledWaterValveIsolationCmdPointNO) } as SeventyFiveFProfilePointDef
                    modelPointDef.let {
                        equipBuilder.createPoint(
                                PointBuilderConfig(
                                        it,
                                        profileConfiguration,
                                        snEquip["id"].toString(),
                                        site!!.id,
                                        hayStack.timeZone,
                                        equipDis
                                )
                        )
                        // This is wrapped in a pointExists check because getID() will not re-check an empty point ID on its own. pointExists() will force a re-check.
                        if (acbEquip.chilledWaterValveIsolationCmdPointNO.pointExists()) {
                            val relay1 = hayStack.readEntity("point and deviceRef == \"" + device["id"].toString() + "\" and domainName == \"" + DomainName.relay1 + "\"")
                            val relay1Point = RawPoint.Builder().setHashMap(relay1).setPointRef(acbEquip.chilledWaterValveIsolationCmdPointNO.id).build()
                            hayStack.updatePoint(relay1Point, relay1Point.id)
                        }
                    }

                }

                val oldValveCmdPoint = hayStack.readEntity("point and equipRef == \"" + snEquip["id"].toString() + "\" and domainName == \"" + "chilledWaterValveIsolationCmdPoint" + "\"")
                if (oldValveCmdPoint.isNotEmpty()) {
                    hayStack.deleteEntity(oldValveCmdPoint["id"].toString())
                }
            }

            PreferenceUtil.setACBRelayLogicalPointsMigration()
        }

        fun recoverHelioNodeACBTuners(hayStack: CCUHsApi) {
            val equipBuilder = ProfileEquipBuilder(hayStack)
            val site = hayStack.site
            val model = ModelLoader.getHelioNodeVavAcbModelDef() as SeventyFiveFProfileDirective

            val hnAcbEquips = hayStack.readAllEntities("equip and domainName == \"" + DomainName.helionodeActiveChilledBeam + "\"")
            hnAcbEquips.forEach {

                val acbEquip = VavAcbEquip(it["id"].toString())

                val profileConfiguration = AcbProfileConfiguration(
                        Integer.parseInt(it["group"].toString()),
                        NodeType.HELIO_NODE.name ,
                        0,
                        it["roomRef"].toString(),
                        it["floorRef"].toString(),
                        ProfileType.VAV_ACB,
                        model
                ).getActiveConfiguration()

                val equipDis = "${site?.displayName}-ACB-${it["group"]}"

                if (!acbEquip.zoneDeadTime.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.zoneDeadTime) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.autoAwayTime.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.autoAwayTime) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.abnormalCurTempRiseTrigger.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.abnormalCurTempRiseTrigger) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.vavCoolingDeadbandMultiplier.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavCoolingDeadbandMultiplier) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.vavZonePriorityMultiplier.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavZonePriorityMultiplier) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.vavZoneCo2Target.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavZoneCo2Target) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.reheatZoneMaxDischargeTemp.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.reheatZoneMaxDischargeTemp) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.vavTemperatureProportionalRange.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavTemperatureProportionalRange) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.vavHeatingDeadbandMultiplier.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavHeatingDeadbandMultiplier) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.constantTempAlertTime.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.constantTempAlertTime) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.forcedOccupiedTime.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.forcedOccupiedTime) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.valveActuationStartDamperPosDuringSysHeating.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.valveActuationStartDamperPosDuringSysHeating) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.vavZoneCo2Threshold.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavZoneCo2Threshold) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.vavProportionalKFactor.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavProportionalKFactor) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.vavIntegralKfactor.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavIntegralKfactor) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.reheatZoneToDATMinDifferential.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.reheatZoneToDATMinDifferential) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.vavZonePrioritySpread.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavZonePrioritySpread) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.vavZoneVocTarget.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavZoneVocTarget) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.vavTemperatureIntegralTime.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavTemperatureIntegralTime) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.relayActivationHysteresis.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.relayActivationHysteresis) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.vavZoneVocThreshold.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavZoneVocThreshold) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.reheatZoneDischargeTempOffset.pointExists()) {
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.reheatZoneDischargeTempOffset) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                            PointBuilderConfig(
                                    modelPointDef,
                                    profileConfiguration,
                                    it["id"].toString(),
                                    site!!.id,
                                    hayStack.timeZone,
                                    equipDis
                            )
                    )
                }

                if (!acbEquip.demandResponseSetback.pointExists()) {
                    CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "VavAndAcbProfileMigration.recoverHelioNodeACBTuners: demandResponseSetback does not exist")
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.demandResponseSetback) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                        PointBuilderConfig(
                            modelPointDef,
                            profileConfiguration,
                            it["id"].toString(),
                            site!!.id,
                            hayStack.timeZone,
                            equipDis
                        )
                    )
                }

                if(!acbEquip.autoAwaySetback.pointExists()) {
                    CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "VavAndAcbProfileMigration.recoverHelioNodeACBTuners: autoAwaySetback does not exist")
                    val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.autoAwaySetback) } as SeventyFiveFProfilePointDef
                    equipBuilder.createPoint(
                        PointBuilderConfig(
                            modelPointDef,
                            profileConfiguration,
                            it["id"].toString(),
                            site!!.id,
                            hayStack.timeZone,
                            equipDis
                        )
                    )
                }

                if (acbEquip.enableCFMControl.readDefaultVal() > 0) {
                    if (!acbEquip.vavAirflowCFMProportionalRange.pointExists()) {
                        val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavAirflowCFMProportionalRange) } as SeventyFiveFProfilePointDef
                        equipBuilder.createPoint(
                                PointBuilderConfig(
                                        modelPointDef,
                                        profileConfiguration,
                                        it["id"].toString(),
                                        site!!.id,
                                        hayStack.timeZone,
                                        equipDis
                                )
                        )
                    }

                    if (!acbEquip.vavAirflowCFMIntegralKFactor.pointExists()) {
                        val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavAirflowCFMIntegralKFactor) } as SeventyFiveFProfilePointDef
                        equipBuilder.createPoint(
                                PointBuilderConfig(
                                        modelPointDef,
                                        profileConfiguration,
                                        it["id"].toString(),
                                        site!!.id,
                                        hayStack.timeZone,
                                        equipDis
                                )
                        )
                    }

                    if (!acbEquip.vavAirflowCFMIntegralTime.pointExists()) {
                        val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavAirflowCFMIntegralTime) } as SeventyFiveFProfilePointDef
                        equipBuilder.createPoint(
                                PointBuilderConfig(
                                        modelPointDef,
                                        profileConfiguration,
                                        it["id"].toString(),
                                        site!!.id,
                                        hayStack.timeZone,
                                        equipDis
                                )
                        )
                    }

                    if (!acbEquip.vavAirflowCFMProportionalKFactor.pointExists()) {
                        val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.vavAirflowCFMProportionalKFactor) } as SeventyFiveFProfilePointDef
                        equipBuilder.createPoint(
                                PointBuilderConfig(
                                        modelPointDef,
                                        profileConfiguration,
                                        it["id"].toString(),
                                        site!!.id,
                                        hayStack.timeZone,
                                        equipDis
                                )
                        )
                    }
                }

            }

            PreferenceUtil.setRecoverHelioNodeACBTunersMigration()

        }

        fun condensateSensorCleanupMigration(hayStack: CCUHsApi) {
            val hnAcbEquips = hayStack.readAllEntities("equip and domainName == \"" + DomainName.helionodeActiveChilledBeam + "\"")
            hnAcbEquips.forEach {
                val acbEquip = VavAcbEquip(it["id"].toString())
                val device = hayStack.readEntity("device and equipRef == \"" + it["id"].toString() + "\"")
                val th2In = hayStack.readEntity("point and deviceRef == \"" + device["id"].toString() + "\" and domainName == \"" + DomainName.th2In + "\"")
                val th2InPoint = RawPoint.Builder().setHashMap(th2In)
                if (acbEquip.thermistor2Type.readPriorityVal() > 0) {
                    if (acbEquip.condensateNC.pointExists()) {
                        hayStack.updatePoint(th2InPoint.setPointRef(acbEquip.condensateNC.id).build(), th2In["id"].toString())
                    }
                } else {
                    if (acbEquip.condensateNO.pointExists()) {
                        hayStack.updatePoint(th2InPoint.setPointRef(acbEquip.condensateNO.id).build(), th2In["id"].toString())
                    }
                }
            }
            val snAcbEquips = hayStack.readAllEntities("equip and domainName == \"" + DomainName.smartnodeActiveChilledBeam + "\"")
            snAcbEquips.forEach {
                val acbEquip = VavAcbEquip(it["id"].toString())
                val device = hayStack.readEntity("device and equipRef == \"" + it["id"].toString() + "\"")
                val th2In = hayStack.readEntity("point and deviceRef == \"" + device["id"].toString() + "\" and domainName == \"" + DomainName.th2In + "\"")
                val th2InPoint = RawPoint.Builder().setHashMap(th2In)
                if (acbEquip.thermistor2Type.readPriorityVal() > 0) {
                    if (acbEquip.condensateNC.pointExists()) {
                        hayStack.updatePoint(th2InPoint.setPointRef(acbEquip.condensateNC.id).build(), th2In["id"].toString())
                    }
                } else {
                    if (acbEquip.condensateNO.pointExists()) {
                        hayStack.updatePoint(th2InPoint.setPointRef(acbEquip.condensateNO.id).build(), th2In["id"].toString())
                    }
                }
            }
        }

        fun cleanVAVDuplicatePoints(hayStack: CCUHsApi) {
            val domainNames = listOf(
                DomainName.smartnodeVAVReheatNoFan,
                DomainName.smartnodeVAVReheatParallelFan,
                DomainName.smartnodeVAVReheatSeriesFan,
                DomainName.helionodeVAVReheatNoFan,
                DomainName.helionodeVAVReheatParallelFan,
                DomainName.helionodeVAVReheatSeriesFan
            )
            domainNames.forEach { profileName ->
                val equips = hayStack.readAllEntities("equip and domainName == \"$profileName\"")
                equips.forEach { equip ->
                    cleanCFMPoints(hayStack, equip)
                }
            }
        }

        fun cleanACBDuplicatePoints(hayStack: CCUHsApi) {
            val domainNames =
                listOf(DomainName.smartnodeActiveChilledBeam, DomainName.helionodeActiveChilledBeam)
            domainNames.forEach { profileName ->
                val equips = hayStack.readAllEntities("equip and domainName == \"$profileName\"")
                equips.forEach { equip ->
                    val device = hayStack.readEntity("device and equipRef == \"${equip["id"]}\"")
                    cleanCondensatePoints(hayStack, equip, device)
                    cleanRelayEnablePoints(hayStack, equip)
                    cleanValvePoints(hayStack, equip, device)
                    cleanCFMPoints(hayStack, equip)
                }
            }
        }

        private fun cleanCondensatePoints(
            hayStack: CCUHsApi,
            equip: Map<Any, Any>,
            device: Map<Any, Any>
        ) {
            // For ACB Condensate, the correct point is the one mapped to th2In on the device.
            // All other points with this domainName are points that failed to delete earlier.

            val th2In = hayStack.readEntity("point and deviceRef == \"${device["id"]}\" and domainName == \"${DomainName.th2In}\"")
            val correctCondensatePointId = th2In.get("pointRef").toString()

            val allCondensatePoints = hayStack.readAllEntities("point and equipRef == \"${equip["id"]}\" and (domainName == \"${DomainName.condensateNO}\" or domainName == \"${DomainName.condensateNC}\")")
            allCondensatePoints.forEach { condensatePoint ->
                if (condensatePoint["id"].toString() != correctCondensatePointId) {
                    hayStack.deleteEntity(condensatePoint["id"].toString())
                }
            }
        }

        private fun cleanValvePoints(
            hayStack: CCUHsApi,
            equip: Map<Any, Any>,
            device: Map<Any, Any>
        ) {
            // For ACB Condensate, the correct point is the one mapped to th2In on the device.
            // All other points with this domainName are points that failed to delete earlier.

            val relay1 = hayStack.readEntity("point and deviceRef == \"${device["id"]}\" and domainName == \"${DomainName.relay1}\"")
            val correctValvePointId = relay1.get("pointRef").toString()

            val allValvePoints = hayStack.readAllEntities("point and equipRef == \"${equip["id"]}\" and (domainName == \"${DomainName.chilledWaterValveIsolationCmdPointNO}\" or domainName == \"${DomainName.chilledWaterValveIsolationCmdPointNC}\")")
            allValvePoints.forEach { valvePoint ->
                if (valvePoint["id"].toString() != correctValvePointId) {
                    hayStack.deleteEntity(valvePoint["id"].toString())
                }
            }
        }

        private fun cleanRelayEnablePoints(hayStack: CCUHsApi, equip: Map<Any, Any>) {
            val relay1OutputEnableList = hayStack.readAllEntities("point and equipRef == \"${equip["id"]}\" and domainName == \"${DomainName.relay1OutputEnable}\"")
            relay1OutputEnableList.removeFirstOrNull()?.let { enablePoint ->
                hayStack.writeDefaultValById(enablePoint["id"].toString(), 1.0)
            }
            if (relay1OutputEnableList.size > 0) {
                relay1OutputEnableList.forEach { relay1OutputEnable ->
                    hayStack.deleteEntity(relay1OutputEnable["id"].toString())
                }
            }

            val relay2OutputEnableList = hayStack.readAllEntities("point and equipRef == \"${equip["id"]}\" and domainName == \"${DomainName.relay2OutputEnable}\"")
            relay2OutputEnableList.removeFirstOrNull()?.let { enablePoint ->
                hayStack.writeDefaultValById(enablePoint["id"].toString(), 0.0)
            }
            if (relay2OutputEnableList.isNotEmpty()) {
                relay2OutputEnableList.forEach { relay2OutputEnable ->
                    hayStack.deleteEntity(relay2OutputEnable["id"].toString())
                }
            }


            // Relay 2 Output Association should not exist. It only is created if Relay2OutputEnable is created with the defaultVal of 1 during a migration.
            val relay2OutputAssociationList = hayStack.readAllEntities("point and equipRef == \"${equip["id"]}\" and domainName == \"${DomainName.relay2OutputAssociation}\"")
            relay2OutputAssociationList.forEach { relay2OutputAssociation ->
                hayStack.deleteEntity(relay2OutputAssociation["id"].toString())
            }
        }

        private fun cleanCFMPoints(hayStack: CCUHsApi, equip: Map<Any, Any>) {
            val enableCFM = hayStack.readDefaultVal("point and equipRef == \"${equip["id"]}\" and domainName == \"${DomainName.enableCFMControl}\"") > 0
            val pointsToDeleteAll : List<String>
            val pointsToDeleteDuplicates : List<String>

            if (enableCFM) {
                pointsToDeleteAll = listOf(
                    DomainName.minCoolingDamperPos,
                    DomainName.maxCoolingDamperPos
                )

                pointsToDeleteDuplicates = listOf(
                    DomainName.minHeatingDamperPos,
                    DomainName.maxHeatingDamperPos,
                    DomainName.minCFMCooling,
                    DomainName.maxCFMCooling,
                    DomainName.minCFMReheating,
                    DomainName.maxCFMReheating,
                    DomainName.kFactor,
                    DomainName.airVelocity,
                    DomainName.airFlowSensor,
                    DomainName.airFlowSetpoint,
                    DomainName.vavAirflowCFMProportionalRange,
                    DomainName.vavAirflowCFMProportionalKFactor,
                    DomainName.vavAirflowCFMIntegralKFactor,
                    DomainName.vavAirflowCFMIntegralTime
                )
            } else {
                pointsToDeleteAll = listOf(
                    DomainName.minCFMCooling,
                    DomainName.maxCFMCooling,
                    DomainName.minCFMReheating,
                    DomainName.maxCFMReheating,
                    DomainName.kFactor,
                    DomainName.airVelocity,
                    DomainName.airFlowSensor,
                    DomainName.airFlowSetpoint,
                    DomainName.vavAirflowCFMProportionalRange,
                    DomainName.vavAirflowCFMProportionalKFactor,
                    DomainName.vavAirflowCFMIntegralKFactor,
                    DomainName.vavAirflowCFMIntegralTime
                )

                pointsToDeleteDuplicates = listOf(
                    DomainName.minCoolingDamperPos,
                    DomainName.minHeatingDamperPos,
                    DomainName.maxCoolingDamperPos,
                    DomainName.maxHeatingDamperPos
                )
            }

            pointsToDeleteAll.forEach { domainName ->
                val points = hayStack.readAllEntities("point and equipRef == \"${equip["id"]}\" and domainName == \"$domainName\"")
                points.forEach { point ->
                    hayStack.deleteEntity(point["id"].toString())
                }
            }

            pointsToDeleteDuplicates.forEach { domainName ->
                val pointMaps = hayStack.readAllEntities("point and equipRef == \"${equip["id"]}\" and domainName == \"$domainName\"")
                val points = pointMaps.map { Point.Builder().setHashMap(it).build() }

                if (points.size > 1) {
                    var sortedPoints = points.sortedBy { it.createdDateTime }
                    sortedPoints.drop(1).forEach { point -> hayStack.deleteEntity(point.id) }
                }
            }
        }


        fun addMinHeatingDamperPositionMigration(hayStack: CCUHsApi) {
            val domainNames = mapOf(
                DomainName.smartnodeVAVReheatNoFan to ModelLoader.getSmartNodeVavNoFanModelDef() as SeventyFiveFProfileDirective,
                DomainName.smartnodeVAVReheatParallelFan to ModelLoader.getSmartNodeVavParallelFanModelDef() as SeventyFiveFProfileDirective,
                DomainName.smartnodeVAVReheatSeriesFan to ModelLoader.getSmartNodeVavSeriesModelDef() as SeventyFiveFProfileDirective,
                DomainName.smartnodeActiveChilledBeam to ModelLoader.getSmartNodeVavAcbModelDef() as SeventyFiveFProfileDirective,
                DomainName.helionodeVAVReheatNoFan to ModelLoader.getHelioNodeVavNoFanModelDef() as SeventyFiveFProfileDirective,
                DomainName.helionodeVAVReheatParallelFan to ModelLoader.getHelioNodeVavParallelFanModelDef() as SeventyFiveFProfileDirective,
                DomainName.helionodeVAVReheatSeriesFan to ModelLoader.getHelioNodeVavSeriesModelDef() as SeventyFiveFProfileDirective,
                DomainName.helionodeActiveChilledBeam to ModelLoader.getHelioNodeVavAcbModelDef() as SeventyFiveFProfileDirective
            )

            domainNames.forEach {
                val equips = hayStack.readAllEntities("equip and domainName == \"${it.key}\"")
                equips.forEach { equip ->
                    val equipId = equip["id"].toString()
                    val minHeatingDamperPos =
                        hayStack.readEntity("point and equipRef == \"${equipId}\" and domainName == \"${DomainName.minHeatingDamperPos}\"")
                    if (minHeatingDamperPos.isNullOrEmpty()) {
                        val model = it.value
                        val profileConfiguration = if (it.key == DomainName.smartnodeActiveChilledBeam || it.key == DomainName.helionodeActiveChilledBeam) {
                            AcbProfileConfiguration(
                                Integer.parseInt(equip["group"].toString()),
                                NodeType.HELIO_NODE.name,
                                0,
                                equip["roomRef"].toString(),
                                equip["floorRef"].toString(),
                                ProfileType.VAV_ACB,
                                model
                            ).getActiveConfiguration()
                        } else {
                            val profileType = when (it.key) {
                                DomainName.smartnodeVAVReheatNoFan -> ProfileType.VAV_REHEAT
                                DomainName.smartnodeVAVReheatParallelFan -> ProfileType.VAV_PARALLEL_FAN
                                DomainName.smartnodeVAVReheatSeriesFan -> ProfileType.VAV_SERIES_FAN
                                DomainName.helionodeVAVReheatNoFan -> ProfileType.VAV_REHEAT
                                DomainName.helionodeVAVReheatParallelFan -> ProfileType.VAV_PARALLEL_FAN
                                DomainName.helionodeVAVReheatSeriesFan -> ProfileType.VAV_SERIES_FAN
                                else -> ProfileType.VAV_REHEAT
                            }

                            VavProfileConfiguration(
                                Integer.parseInt(equip["group"].toString()),
                                NodeType.HELIO_NODE.name,
                                0,
                                equip["roomRef"].toString(),
                                equip["floorRef"].toString(),
                                profileType,
                                model
                            ).getActiveConfiguration()
                        }

                        val equipDis = if (it.key == DomainName.smartnodeActiveChilledBeam || it.key == DomainName.helionodeActiveChilledBeam) {
                            "${hayStack.site?.displayName}-ACB-${equip["group"]}"
                        } else {
                            "${hayStack.site?.displayName}-VAV-${equip["group"]}"
                        }

                        val modelPointDef = model.points.find { it.domainName.contentEquals(DomainName.minHeatingDamperPos) } as SeventyFiveFProfilePointDef
                        val equipBuilder = ProfileEquipBuilder(hayStack)
                        equipBuilder.createPoint(
                            PointBuilderConfig(
                                modelPointDef,
                                profileConfiguration,
                                equipId,
                                hayStack.site!!.id,
                                hayStack.timeZone,
                                equipDis
                            )
                        )

                        val minHeatingDamperPoint = hayStack.readEntity("point and equipRef == \"${equipId}\" and domainName == \"${DomainName.minHeatingDamperPos}\"")
                        if (minHeatingDamperPoint.isNotEmpty()) {
                            val minHeatingDamperId = minHeatingDamperPoint["id"].toString()
                            minHeatingDamperId.let {
                                hayStack.writeDefaultValById(it, 20.0)
                            }
                        }
                    }
                }
            }
        }
    }
}