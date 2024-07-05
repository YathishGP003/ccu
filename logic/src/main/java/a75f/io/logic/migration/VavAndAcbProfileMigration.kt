package a75f.io.logic.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.RawPoint
import a75f.io.domain.VavAcbEquip
import a75f.io.domain.api.DomainName
import a75f.io.domain.logic.PointBuilderConfig
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.vav.AcbProfileConfiguration
import a75f.io.logic.util.PreferenceUtil
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef

class VavAndAcbProfileMigration {
    companion object {

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

                //
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
                val device = hayStack.readEntity("device and equipRef == \"" + it["group"].toString() + "\"")
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
                val device = hayStack.readEntity("device and equipRef == \"" + it["group"].toString() + "\"")
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
    }
}