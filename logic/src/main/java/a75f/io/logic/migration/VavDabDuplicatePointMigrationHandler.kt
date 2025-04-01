package a75f.io.logic.migration

import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.domain.api.Domain.hayStack
import a75f.io.domain.api.DomainName
import a75f.io.domain.logic.DefaultEquipBuilder
import a75f.io.domain.logic.PointBuilderConfig
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuProfileConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags
import org.projecthaystack.HDateTime
import org.projecthaystack.HDict
import kotlin.math.absoluteValue

class VavDabAndDuplicatePointMigrationHandler(private val systemEquip: HashMap<Any, Any>) : DefaultEquipBuilder() {
    fun createSatSpResPoint() {
        val profileName = systemEquip["profile"]?.toString() ?: run {
            CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Profile not found in System Equip")
            return
        }
        val satSpResPoint =
            hayStack.readEntity("domainName == \"" + DomainName.satSPRes + "\" and equipRef == \"" + systemEquip["id"].toString() + "\"")
        if (satSpResPoint.isEmpty()) {
            CcuLog.d(
                L.TAG_CCU_MIGRATION_UTIL,
                "satSPRes point not exists for ${systemEquip["id"]}"
            )

            val model = when (ProfileType.getProfileTypeForName(profileName)) {
                ProfileType.SYSTEM_DAB_ANALOG_RTU -> ModelLoader.getDabModulatingRtuModelDef()
                ProfileType.SYSTEM_VAV_ANALOG_RTU -> ModelLoader.getVavModulatingRtuModelDef()
                else -> return
            }

            model.points.find { it.domainName == DomainName.satSPRes }?.let { modelPointDef ->
                val equipDis = "${hayStack.site?.displayName}-${model.name}"
                val profileConfig =
                    ModulatingRtuProfileConfig(model as SeventyFiveFProfileDirective).getDefaultConfiguration()

                createPointFromDef(
                    PointBuilderConfig(
                        modelPointDef, profileConfig, systemEquip["id"].toString(),
                        hayStack.siteIdRef.toString(), hayStack.timeZone, equipDis
                    )
                )
            }
        }
    }

    fun deleteSatSPMaxDuplicatePoints() {
        val satSpMaxPoints =
            hayStack.readAllEntities("domainName == \"" + DomainName.satSPResMax + "\" and equipRef == \"" + systemEquip["id"].toString() + "\"")

        val satSpMaxPointsWithDefVal: List<Pair<String, Double>> = satSpMaxPoints.map { it["id"].toString() to hayStack.readDefaultValByLevel(it["id"].toString(), HayStackConstants.DEFAULT_INIT_VAL_LEVEL); }

        if(satSpMaxPointsWithDefVal.size > 1) {
            // Delete the point with least value, we are assuming point with least value is the duplicate point
            val leastValue = satSpMaxPointsWithDefVal.minByOrNull { it.second.absoluteValue }!!
            hayStack.deleteEntity(leastValue.first)
        }
    }

    fun deleteReheatZoneMaxDischargeTemp(buildingEquip: HashMap<Any, Any>) {
        val reheatZoneMaxDischargeTempPoint = hayStack.readEntity(
            "domainName == \"${DomainName.reheatZoneMaxDischargeTemp}\" and equipRef == \"${buildingEquip["id"]}\""
        )

        val defaultValue = hayStack.readDefaultValByLevel(
            reheatZoneMaxDischargeTempPoint["id"].toString(),
            HayStackConstants.DEFAULT_INIT_VAL_LEVEL
        )

        fun deletePointIfExists(pointId: String) {
            if (hayStack.isEntityExisting(pointId)) {
                CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Deleting point with ID: $pointId")
                hayStack.deleteEntity(pointId)
            } else {
                hayStack.deleteRemoteEntity(pointId)
                CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Point with ID: $pointId does not exist locally, deleting from remote")
            }
        }

        fun addPointIfNotExists(pointId: String, remotePointData: HDict) {
            if (!hayStack.isEntityExisting(pointId)) {
                CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Adding point with ID: $pointId")

                val newPoint = Point.Builder().setHDict(remotePointData).build()
                hayStack.addRemotePoint(newPoint, newPoint.id)
                hayStack.writeDefaultTunerValById(newPoint.id, defaultValue)
                hayStack.writeHisValById(newPoint.id, defaultValue)

                CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Point with ID: $pointId added successfully")
            } else {
                CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Point with ID: $pointId already exists locally")
            }
        }

        val reheatZoneMaxDischargeTempPoints = hayStack.readRemoteEntitiesByQuery(
            "domainName == \"${DomainName.reheatZoneMaxDischargeTemp}\" and equipRef == \"${buildingEquip["id"].toString().replace("@", "")}\""
        )

        if (reheatZoneMaxDischargeTempPoints.size > 1) {
            // Create a list of triples containing (pointId, createdDate, fullEntity)
            val pointsWithCreatedDate = reheatZoneMaxDischargeTempPoints.map {
                Triple(it["id"], it[Tags.CREATED_DATE_TIME], it)
            }

            val (pointId1, createdDate1, entity1) = pointsWithCreatedDate[0]
            val (pointId2, createdDate2, entity2) = pointsWithCreatedDate[1]

            val createdDateTime1 = HDateTime.make(createdDate1.toString())
            val createdDateTime2 = HDateTime.make(createdDate2.toString())

            if (createdDateTime1 > createdDateTime2) {
                deletePointIfExists(pointId2.toString())
                addPointIfNotExists(pointId1.toString(), entity1)
            } else {
                deletePointIfExists(pointId1.toString())
                addPointIfNotExists(pointId2.toString(), entity2)
            }
        }
    }
}