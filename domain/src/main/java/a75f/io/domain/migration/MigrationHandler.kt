package a75f.io.domain.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.*
import a75f.io.domain.api.Domain.getEquipDetailsByDomain
import a75f.io.domain.api.Domain.getSystemEquipByDomainName
import a75f.io.domain.config.DefaultProfileConfiguration
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ExternalAhuConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.EquipBuilderConfig
import a75f.io.domain.logic.PointBuilderConfig

import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.logic.TunerEquipBuilder
import a75f.io.logger.CcuLog
import android.util.Log
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.ModelPointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective
import io.seventyfivef.domainmodeler.common.point.AssociationConfiguration
import io.seventyfivef.domainmodeler.common.point.ComparisonType
import io.seventyfivef.domainmodeler.common.point.DependentConfiguration
import io.seventyfivef.domainmodeler.common.point.PointConfiguration

/**
 * Created by Manjunath K on 16-06-2023.
 */
class MigrationHandler(var haystack: CCUHsApi, var listener: DiffManger.OnMigrationCompletedListener) {

    fun migrateModel(entityData: EntityConfiguration,oldModel: ModelDirective, newModel: ModelDirective, siteRef: String) {
        if (newModel is SeventyFiveFTunerDirective) {
            CcuLog.printLongMessage(Domain.LOG_TAG,
                "Building equip model upgrade detected : Run migration to $newModel. modelId: ${newModel.id} "
            )
            val tunerEquipBuilder = TunerEquipBuilder(haystack)
            tunerEquipBuilder.updateEquipAndPoints(newModel,entityData, siteRef)
            tunerEquipBuilder.updateBackendBuildingTuner(siteRef, haystack)
            listener.onMigrationCompletedCompleted(haystack)
        } else {
            val equips: List<Equip> = if (Domain.readEquip(newModel.id)["roomRef"].toString() == "SYSTEM") {
                val equip = getSystemEquipByDomainName(newModel.domainName)
                if (equip != null) listOf(equip) else emptyList()
            } else {
                getEquipDetailsByDomain(newModel.domainName)
            }
            CcuLog.printLongMessage(Domain.LOG_TAG,
                "Equip model upgrade detected : Run migration to $newModel; equip size ${equips.size}"
            )
            Log.d("CCU_MODEL", "tobeAdded: size:${entityData.tobeAdded.size}")
            entityData.tobeAdded.forEach { item ->
                Log.d("CCU_DOMAIN", "tobeAdded: item:${item.domainName}")
            }
            Log.d("CCU_DOMAIN", "tobeDeleted: size:${entityData.tobeDeleted.size}")
            entityData.tobeDeleted.forEach { item ->
                Log.d("CCU_MODEL", "tobeDeleted: item:${item.domainName}")
            }
            Log.d("CCU_DOMAIN", "tobeUpdated: size:${entityData.tobeUpdated.size}")
            entityData.tobeUpdated.forEach { item ->
                Log.d("CCU_MODEL", "tobeUpdated: item:${item.domainName}")
            }
            if(equips.isNotEmpty()) {
                addEntityData(entityData.tobeAdded, newModel, equips, siteRef)
                removeEntityData(entityData.tobeDeleted, oldModel, equips, siteRef)
                updateEntityData(entityData.tobeUpdated, newModel, equips, siteRef)
                updateEquipVersion(newModel, equips, siteRef)
            }
        }
    }

    private fun updateEquipVersion(newModel: ModelDirective, equips: List<Equip>, siteRef: String) {
        val equipBuilder = ProfileEquipBuilder (haystack)
        equips.forEach {
            val equipMap = haystack.readMapById(it.id)
            val profileConfiguration = getProfileConfig(equipMap["profile"].toString())
            updateRef(equipMap, profileConfiguration)
            val hayStackEquip = equipBuilder.buildEquip(EquipBuilderConfig(newModel, profileConfiguration, siteRef,
                haystack.timeZone, equipMap["dis"].toString()))
            if (Domain.readEquip(newModel.id)["roomRef"].toString() == "SYSTEM") {
                hayStackEquip.roomRef = "SYSTEM"
                hayStackEquip.floorRef = "SYSTEM"
                haystack.updateEquip(hayStackEquip, it.id)
                DomainManager.addSystemEquip(Domain.hayStack, Domain.hayStack.ccuId)
            }else{
                hayStackEquip.ahuRef = equipMap["ahuRef"]?.toString()
                hayStackEquip.gatewayRef = equipMap["gatewayRef"]?.toString()
                haystack.updateEquip(hayStackEquip, it.id)
                hayStackEquip.id = it.id
                DomainManager.addEquip(hayStackEquip)
            }
        }
    }

    private fun addEntityData(tobeAdded: MutableList<EntityConfig>, newModel: ModelDirective,
                              equips: List<Equip>, siteRef : String) {
        val equipBuilder = ProfileEquipBuilder (haystack)
         // need to revisit this line
        tobeAdded.forEach { diffDomain ->
            // updated Equip
            if (diffDomain.domainName == newModel.domainName) {
                equips.forEach {
                    val equipMap = haystack.readMapById(it.id)
                    val profileConfiguration = getProfileConfig(equipMap["profile"].toString())
                    updateRef(equipMap, profileConfiguration)
                    val hayStackEquip = equipBuilder.buildEquip(EquipBuilderConfig(newModel, profileConfiguration, siteRef,
                        haystack.timeZone, equipMap["dis"].toString()))
                    hayStackEquip.ahuRef = equipMap["ahuRef"]?.toString()
                    hayStackEquip.gatewayRef = equipMap["gatewayRef"]?.toString()
                    haystack.updateEquip(hayStackEquip, it.id)
                    hayStackEquip.id = it.id
                    if (Domain.readEquip(newModel.id)["roomRef"].toString() == "SYSTEM") {
                        DomainManager.addSystemEquip(Domain.hayStack, Domain.hayStack.ccuId)
                    }else{
                        DomainManager.addEquip(hayStackEquip)
                    }

                }
            }
            equips.forEach {equip ->
                val equipMap = haystack.readMapById(equip.id)
                val profileConfiguration = getProfileConfig(equipMap["profile"].toString())
                updateRef(equipMap, profileConfiguration)
                val modelPointDef = newModel.points.find { it.domainName == diffDomain.domainName }
                modelPointDef?.run {
                    if (toBeAddedForEquip(modelPointDef, equip.id)) {
                        equipBuilder.createPoint(
                            PointBuilderConfig(modelPointDef, profileConfiguration, equip.id, siteRef, haystack.timeZone, equipMap["dis"].toString())
                        )
                    }
                }
            }
        }
    }
    private fun removeEntityData(
        tobeRemove: MutableList<EntityConfig>, oldModel: ModelDirective, equips: List<Equip>,
        siteRef: String
    ) {
        val equipBuilder = ProfileEquipBuilder (haystack)
        val profileConfiguration = getProfileConfig("")
        tobeRemove.forEach { diffDomain ->
            equips.forEach {equip ->
                val equipDetails = haystack.readMapById(equip.id)
                val modelPointDef = oldModel.points.find { it.domainName == diffDomain.domainName }
                modelPointDef?.run {
                    val hayStackPoint = equipBuilder.buildPoint(PointBuilderConfig( modelPointDef,
                        profileConfiguration, equip.id, siteRef, haystack.timeZone, equipDetails["dis"].toString()))
                    val point = CCUHsApi.getInstance().readEntity("point and domainName == \"${diffDomain.domainName}\" and equipRef == \"${equip.id}\"")
                    DomainManager.removePoint(hayStackPoint)
                    haystack.deleteEntity(point["id"].toString())
                }
            }
        }
    }

    private fun updateEntityData(tobeUpdate: MutableList<EntityConfig>, newModel: ModelDirective,
                                 equips: List<Equip>, siteRef: String) {
        val equipBuilder = ProfileEquipBuilder (haystack)
        tobeUpdate.forEach { diffDomain ->
            // updated Equip
            if (diffDomain.domainName == newModel.domainName) {
                equips.forEach {
                    val equipMap = haystack.readMapById(it.id)
                    val profileConfiguration = getProfileConfig(equipMap["profile"].toString())
                    updateRef(equipMap, profileConfiguration)
                    val hayStackEquip = equipBuilder.buildEquip(EquipBuilderConfig(newModel, profileConfiguration, siteRef,
                        haystack.timeZone, equipMap["dis"].toString()))
                    hayStackEquip.ahuRef = equipMap["ahuRef"]?.toString()
                    hayStackEquip.gatewayRef = equipMap["gatewayRef"]?.toString()
                    haystack.updateEquip(hayStackEquip, it.id)
                    hayStackEquip.id = it.id
                    if (Domain.readEquip(newModel.id)["roomRef"].toString() == "SYSTEM") {
                        DomainManager.addSystemEquip(Domain.hayStack, Domain.hayStack.ccuId)
                    }else{
                        DomainManager.addEquip(hayStackEquip)
                    }
                }
            }
            equips.forEach {equip ->
                val modelPointDef = newModel.points.find { it.domainName == diffDomain.domainName }
                val equipMap = haystack.readMapById(equip.id)
                val profileConfiguration = getProfileConfig(equipMap["profile"].toString())
                updateRef(equipMap, profileConfiguration)
                modelPointDef?.run {
                    val hayStackPoint = equipBuilder.buildPoint(PointBuilderConfig( modelPointDef,
                        profileConfiguration, equip.id, siteRef, haystack.timeZone, equipMap["dis"].toString()))
                    val point = CCUHsApi.getInstance().readEntity("point and domainName == \"${diffDomain.domainName}\" and equipRef == \"${equip.id}\"")
                   Log.d("CCU_DOMAIN", "updated haystack point: $hayStackPoint")
                    if (Domain.readEquip(newModel.id)["roomRef"].toString() == "SYSTEM") {
                        hayStackPoint.roomRef = "SYSTEM"
                        hayStackPoint.floorRef = "SYSTEM"
                        haystack.updatePoint(hayStackPoint, point["id"].toString())
                    }else {
                        haystack.updatePoint(hayStackPoint, point["id"].toString())
                        DomainManager.addPoint(hayStackPoint)
                    }
                }
            }
        }
    }

    private fun getProfileConfig(profileType: String) : ProfileConfiguration {
        return when(profileType) {
            "dabExternalAHUController", "vavExternalAHUController" -> {
                val profile = ExternalAhuConfiguration(profileType)
                profile
            }else -> {
                /*
                 This is not a robust solution, but it works for now.
                 Right now, existing configuration classes reside in the :logic package and aren't accessible here.
                 Created a DefaultConfiguration class that holds the few fields (group, roomRef, floorRef, profile) that are needed inside the :domain package.
             */
                val profile = DefaultProfileConfiguration(1000, "", 0, "", "", profileType)
                profile
            }
        }
    }

    private fun updateRef(equipMap: HashMap<Any, Any>, profileConfiguration: ProfileConfiguration) {
        if (equipMap.containsKey("roomRef") && equipMap["roomRef"] != null) profileConfiguration.roomRef = equipMap["roomRef"].toString()
        if (equipMap.containsKey("floorRef") && equipMap["floorRef"] != null) profileConfiguration.floorRef = equipMap["floorRef"].toString()
        if (equipMap.containsKey("group") && equipMap["group"] != null) profileConfiguration.nodeAddress = Integer.parseInt(
            equipMap["group"].toString())
        if (equipMap.containsKey("profile") && equipMap["profile"] != null) profileConfiguration.profileType = equipMap["profile"].toString()
    }


    private fun toBeAddedForEquip(pointDef : ModelPointDef, equipRef : String) : Boolean {

        if (pointDef is SeventyFiveFProfilePointDef) {
            return when (pointDef.configuration.configType) {
                PointConfiguration.ConfigType.BASE -> true // always add BASE points
                PointConfiguration.ConfigType.DEPENDENT -> isDependentPointEnabled(pointDef, equipRef)
                PointConfiguration.ConfigType.DYNAMIC_SENSOR -> false // never add DYNAMIC_SENSOR points; they are created from the device layer
                PointConfiguration.ConfigType.ASSOCIATED -> isAssociatedPointEnabled(pointDef)
                PointConfiguration.ConfigType.ASSOCIATION -> isAssociationPointEnabled(pointDef, equipRef)
            }
        }

        return true
    }

    private fun isDependentPointEnabled(pointDef: SeventyFiveFProfilePointDef, equipRef : String) : Boolean {
        // If a point is DEPENDENT, read the point it depends on from Haystack, then evaluate the config
        val pointConfig = pointDef.configuration as DependentConfiguration
        val configValue = haystack.readPointPriorityValByQuery("point and domainName == \"" + pointConfig.domainName + "\" and equipRef == \"" + equipRef + "\"")

        if (configValue != null) {
            return when (pointConfig.comparisonType) {
                ComparisonType.EQUALS -> (pointConfig.value as Int) == configValue.toInt()
                ComparisonType.NOT_EQUALS -> (pointConfig.value as Int) != configValue.toInt()
                ComparisonType.GREATER_THAN -> (pointConfig.value as Int) > configValue.toInt()
                ComparisonType.LESS_THAN -> (pointConfig.value as Int) < configValue.toInt()
                ComparisonType.GREATER_THAN_OR_EQUAL_TO -> (pointConfig.value as Int) >= configValue.toInt()
                ComparisonType.LESS_THAN_OR_EQUAL_TO -> (pointConfig.value as Int) <= configValue.toInt()
            }
        }
        return true
    }

    private fun isAssociatedPointEnabled(pointDef: SeventyFiveFProfilePointDef) : Boolean {
        /*  ASSOCIATED points should not be added as part of a migration.
            They are created only during configuration (when their domainName shows up in the enum of a created ASSOCIATION point).
         */
        return false
    }

    private fun isAssociationPointEnabled(pointDef: SeventyFiveFProfilePointDef, equipRef : String) : Boolean {
        // Expectation is that new ASSOCIATION points would not be added to a model that's already in production
        // But, if one appears, read the point it depends on from Haystack, then evaluate the config. Add only if it is enabled per the config.
        val pointConfig = pointDef.configuration as AssociationConfiguration
        val configValue = haystack.readPointPriorityValByQuery("point and domainName == \"" + pointConfig.domainName + "\" and equipRef == \"" + equipRef + "\"")

        if (configValue != null) {
            return when (pointConfig.comparisonType) {
                ComparisonType.EQUALS -> (pointConfig.value as Int) == configValue.toInt()
                ComparisonType.NOT_EQUALS -> (pointConfig.value as Int) != configValue.toInt()
                ComparisonType.GREATER_THAN -> (pointConfig.value as Int) > configValue.toInt()
                ComparisonType.LESS_THAN -> (pointConfig.value as Int) < configValue.toInt()
                ComparisonType.GREATER_THAN_OR_EQUAL_TO -> (pointConfig.value as Int) >= configValue.toInt()
                ComparisonType.LESS_THAN_OR_EQUAL_TO -> (pointConfig.value as Int) <= configValue.toInt()
            }
        }
        return false
    }

}