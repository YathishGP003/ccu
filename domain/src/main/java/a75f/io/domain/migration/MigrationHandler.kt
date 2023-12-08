package a75f.io.domain.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.getEquipDetailsByDomain
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.HyperStat2pfcuConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.EquipBuilderConfig
import a75f.io.domain.logic.PointBuilderConfig

import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.logic.TunerEquipBuilder
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective

/**
 * Created by Manjunath K on 16-06-2023.
 */
class MigrationHandler(var haystack: CCUHsApi) {

    fun migrateModel(entityData: EntityConfiguration,newModel: ModelDirective, siteRef: String, profileName: String) {
        if (newModel is SeventyFiveFTunerDirective) {
            CcuLog.printLongMessage(Domain.LOG_TAG,
                "Building equip model upgrade detected : Run migration to $newModel"
            )
            val tunerEquipBuilder = TunerEquipBuilder(haystack)
            tunerEquipBuilder.updateEquipAndPoints(newModel,entityData, siteRef )

            tunerEquipBuilder.updateBackendBuildingTuner(siteRef, haystack)
        } else {
            val equipDetails = getEquipDetailsByDomain(newModel.domainName)
            addEntityData(entityData.tobeAdded, newModel, equipDetails, siteRef, profileName)
            removeEntityData(entityData.tobeDeleted, newModel, equipDetails)
            updateEntityData(entityData.tobeUpdated, newModel, equipDetails, siteRef, profileName)
        }
    }
    private fun addEntityData(tobeAdded: MutableList<EntityConfig>, newModel: ModelDirective,
                              equips: List<a75f.io.domain.api.Equip>, siteRef : String, profileName: String) {
        val equipBuilder = ProfileEquipBuilder (haystack)
        val profileConfiguration = getTestProfileConfig()
        tobeAdded.forEach { diffDomain ->
            // updated Equip
            if (diffDomain.domainName == newModel.domainName) {
                val hayStackEquip = equipBuilder.buildEquip(EquipBuilderConfig(newModel, profileConfiguration, siteRef,
                    haystack.timeZone, haystack.siteName!!))
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
                    val hayStackPoint = equipBuilder.buildPoint(PointBuilderConfig( modelPointDef,
                                        profileConfiguration, equip.id, siteRef, haystack.timeZone, equipDetails["dis"].toString()))
                    val pointId = haystack.addPoint(hayStackPoint)
                    hayStackPoint.id = pointId
                    DomainManager.addPoint(hayStackPoint)
                }
            }
        }
    }
    private fun removeEntityData(tobeRemove: MutableList<EntityConfig>, newModel: ModelDirective, equips: List<a75f.io.domain.api.Equip>) {
    // TODO remove change set
        println(tobeRemove)
    }

    private fun updateEntityData(tobeUpdate: MutableList<EntityConfig>, newModel: ModelDirective,
                                 equips: List<a75f.io.domain.api.Equip>, siteRef: String, profileName: String) {
        val equipBuilder = ProfileEquipBuilder (haystack)
        val profileConfiguration = getTestProfileConfig()
        tobeUpdate.forEach { diffDomain ->
            // updated Equip
            if (diffDomain.domainName == newModel.domainName) {
                val hayStackEquip = equipBuilder.buildEquip(EquipBuilderConfig(newModel, profileConfiguration, siteRef,
                    haystack.timeZone, haystack.siteName!!))
                equips.forEach {
                    haystack.updateEquip(hayStackEquip, it.id)
                    DomainManager.addEquip(hayStackEquip)
                }
            }
            equips.forEach {equip ->
                val equipDetails = haystack.readMapById(equip.id)
                val modelPointDef = newModel.points.find { it.domainName == diffDomain.domainName }
                modelPointDef?.run {
                    val hayStackPoint = equipBuilder.buildPoint(PointBuilderConfig( modelPointDef,
                        profileConfiguration, equip.id, siteRef, haystack.timeZone, equipDetails["dis"].toString()))
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