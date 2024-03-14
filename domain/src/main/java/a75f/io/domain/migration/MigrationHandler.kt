package a75f.io.domain.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.*
import a75f.io.domain.api.Domain.getEquipDetailsByDomain
import a75f.io.domain.api.Domain.getSystemEquipByDomainName
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ExternalAhuConfiguration
import a75f.io.domain.config.HyperStat2pfcuConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.EquipBuilderConfig
import a75f.io.domain.logic.PointBuilderConfig

import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.logic.TunerEquipBuilder
import a75f.io.logger.CcuLog
import android.util.Log
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective

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
                removeEntityData(entityData.tobeDeleted, oldModel, newModel, equips, siteRef)
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
            val hayStackEquip = equipBuilder.buildEquip(EquipBuilderConfig(newModel, profileConfiguration, siteRef,
                haystack.timeZone, equipMap["dis"].toString()))
            if (Domain.readEquip(newModel.id)["roomRef"].toString() == "SYSTEM") {
                hayStackEquip.roomRef = "SYSTEM"
                hayStackEquip.floorRef = "SYSTEM"
                haystack.updateEquip(hayStackEquip, it.id)
                DomainManager.addSystemEquip(Domain.hayStack, Domain.hayStack.ccuId)
            }else{
                haystack.updateEquip(hayStackEquip, it.id)
                DomainManager.addEquip(hayStackEquip)
            }
        }
    }

    private fun addEntityData(tobeAdded: MutableList<EntityConfig>, newModel: ModelDirective,
                              equips: List<a75f.io.domain.api.Equip>, siteRef : String) {
        val equipBuilder = ProfileEquipBuilder (haystack)
         // need to revisit this line
        tobeAdded.forEach { diffDomain ->
            // updated Equip
            if (diffDomain.domainName == newModel.domainName) {
                equips.forEach {
                    val equipMap = haystack.readMapById(it.id);
                    val profileConfiguration = getProfileConfig(equipMap["profile"].toString())
                    val hayStackEquip = equipBuilder.buildEquip(EquipBuilderConfig(newModel, profileConfiguration, siteRef,
                        haystack.timeZone, equipMap["dis"].toString()))
                    haystack.updateEquip(hayStackEquip, it.id)
                    if (Domain.readEquip(newModel.id)["roomRef"].toString() == "SYSTEM") {
                        DomainManager.addSystemEquip(Domain.hayStack, Domain.hayStack.ccuId)
                    }else{
                        DomainManager.addEquip(hayStackEquip)
                    }

                }
            }
            equips.forEach {equip ->
                val equipMap = haystack.readMapById(equip.id);
                val profileConfiguration = getProfileConfig(equipMap["profile"].toString())
                val equipDetails = haystack.readMapById(equip.id)
                profileConfiguration.roomRef = equipDetails["roomRef"].toString()
                profileConfiguration.floorRef = equipDetails["floorRef"].toString()
                val modelPointDef = newModel.points.find { it.domainName == diffDomain.domainName }
                modelPointDef?.run {
                    val hayStackPoint = equipBuilder.buildPoint(PointBuilderConfig( modelPointDef,
                                        profileConfiguration, equip.id, siteRef, haystack.timeZone, equipDetails["dis"].toString()))
                    val pointId = haystack.addPoint(hayStackPoint)
                    hayStackPoint.id = pointId
                    DomainManager.addPoint(hayStackPoint)
                }
            }
        }
    }
    private fun removeEntityData(tobeRemove: MutableList<EntityConfig>, oldModel: ModelDirective, newModel: ModelDirective,
                                 equips: List<a75f.io.domain.api.Equip>, siteRef: String) {
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
                                 equips: List<a75f.io.domain.api.Equip>, siteRef: String) {
        val equipBuilder = ProfileEquipBuilder (haystack)
        tobeUpdate.forEach { diffDomain ->
            // updated Equip
            if (diffDomain.domainName == newModel.domainName) {
                equips.forEach {
                    val equipMap = haystack.readMapById(it.id);
                    val profileConfiguration = getProfileConfig(equipMap["profile"].toString())
                    val hayStackEquip = equipBuilder.buildEquip(EquipBuilderConfig(newModel, profileConfiguration, siteRef,
                        haystack.timeZone, equipMap["dis"].toString()))
                    haystack.updateEquip(hayStackEquip, it.id)
                    if (Domain.readEquip(newModel.id)["roomRef"].toString() == "SYSTEM") {
                        DomainManager.addSystemEquip(Domain.hayStack, Domain.hayStack.ccuId)
                    }else{
                        DomainManager.addEquip(hayStackEquip)
                    }
                }
            }
            equips.forEach {equip ->
                val modelPointDef = newModel.points.find { it.domainName == diffDomain.domainName }
                val equipMap = haystack.readMapById(equip.id);
                val profileConfiguration = getProfileConfig(equipMap["profile"].toString())
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
        return if(profileType == "dabExternalAHUController") {
            val profile = ExternalAhuConfiguration(profileType)
            profile
        }else if(profileType == "vavExternalAHUController") {
            val profile = ExternalAhuConfiguration(profileType)
            profile
        }else{
            // this else block needs to be revisited
            val profile = HyperStat2pfcuConfiguration(1000,"HS",0, "","")
            profile.autoForcedOccupied.enabled = true
            profile.autoAway.enabled = true
            profile
        }
    }
}