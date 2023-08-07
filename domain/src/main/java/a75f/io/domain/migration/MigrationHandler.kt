package a75f.io.domain.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain.getEquipDetailsByDomain
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.HyperStat2pfcuConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.ProfileEquipBuilder
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created by Manjunath K on 16-06-2023.
 */
class MigrationHandler(var haystack: CCUHsApi) {

    fun migrateModel(entityData: EntityConfiguration,newModel: SeventyFiveFProfileDirective) {
        val equipDetails = getEquipDetailsByDomain(newModel.domainName)
        addEntityData(entityData.tobeAdded,newModel,equipDetails)
        removeEntityData(entityData.tobeDeleted,newModel,equipDetails)
        updateEntityData(entityData.tobeUpdated,newModel,equipDetails)
    }
    private fun addEntityData(tobeAdded: MutableList<EntityConfig>, newModel: SeventyFiveFProfileDirective, equips: List<a75f.io.domain.api.Equip>) {
        val equipBuilder = ProfileEquipBuilder (haystack)
        val profileConfiguration = getTestProfileConfig()
        tobeAdded.forEach { diffDomain ->
            // updated Equip
            if (diffDomain.domainName == newModel.domainName) {
                val hayStackEquip = equipBuilder.buildEquip(newModel, profileConfiguration)
                equips.forEach {
                    haystack.updateEquip(hayStackEquip, it.id)
                    DomainManager.addEquip(hayStackEquip)
                }
            }
            equips.forEach {equip ->
                val equipDetails = haystack.readMapById(equip.id)
                profileConfiguration.roomRef = equipDetails["roomRef"].toString()
                profileConfiguration.floorRef = equipDetails["floorRef"].toString()
                val modelPointDef = newModel.points.find { it.domainName == diffDomain.domainName }
                modelPointDef?.run {
                    val hayStackPoint = equipBuilder.buildPoint(modelPointDef, profileConfiguration, equip.id)
                    val pointId = haystack.addPoint(hayStackPoint)
                    hayStackPoint.id = pointId
                    DomainManager.addPoint(hayStackPoint)
                }
            }
        }
    }
    private fun removeEntityData(tobeRemove: MutableList<EntityConfig>, newModel: SeventyFiveFProfileDirective, equips: List<a75f.io.domain.api.Equip>) {
    // TODO remove change set
        println(tobeRemove)
    }
    private fun updateEntityData(tobeUpdate: MutableList<EntityConfig>, newModel: SeventyFiveFProfileDirective, equips: List<a75f.io.domain.api.Equip>) {
        val equipBuilder = ProfileEquipBuilder (haystack)
        val profileConfiguration = getTestProfileConfig()
        tobeUpdate.forEach { diffDomain ->
            // updated Equip
            if (diffDomain.domainName == newModel.domainName) {
                val hayStackEquip = equipBuilder.buildEquip(newModel, profileConfiguration)
                equips.forEach {
                    haystack.updateEquip(hayStackEquip, it.id)
                    DomainManager.addEquip(hayStackEquip)
                }
            }
            equips.forEach {equip ->
                val modelPointDef = newModel.points.find { it.domainName == diffDomain.domainName }
                modelPointDef?.run {
                    val hayStackPoint = equipBuilder.buildPoint(modelPointDef, profileConfiguration, equip.id)
                    val currentPoint = equip.points.filter { it.value.domainName == diffDomain.domainName }
                    val existingId = currentPoint[diffDomain.domainName]?.id
                    hayStackPoint.id = existingId
                    haystack.updatePoint(hayStackPoint,existingId );
                    DomainManager.addPoint(hayStackPoint)
                }
            }
        }
    }

    private fun getTestProfileConfig() : ProfileConfiguration {
        val profile = HyperStat2pfcuConfiguration(1000,"HS",0, "","")

        profile.autoForcedOccupied.enabled = true
        profile.autoAway.enabled = true

        return profile
    }
}