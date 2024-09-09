package a75f.io.domain

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Floor
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Zone
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.migration.DiffManger
import a75f.io.domain.migration.MAJOR
import a75f.io.domain.migration.MINOR
import a75f.io.domain.migration.MigrationHandler
import a75f.io.domain.migration.ModelMeta
import a75f.io.domain.migration.PATCH

import a75f.io.domain.util.ResourceHelper
import io.seventyfivef.domainmodeler.common.Version
import org.junit.Test

/**
 * Created by Manjunath K on 22-06-2023.
 */

class DiffManagerTest {

    // TODO : Need to fix this test
    fun testDiffManager() {
        val mockHayStack = MockCcuHsApi()
        mokeSiteDetilas(mockHayStack)
        Domain.site?.floors?.entries?.forEach{ it ->
            val floor = it.value
            assert(floor.rooms.size == 1)
            floor.rooms.entries.forEach{ r ->
                val room =  r.value
                assert(room.equips.size == 1)
                room.equips.entries.forEach{ e ->
                    val equip = e.value
                    assert(equip.points.size == 4)
                    equip.points.entries.forEach{ point ->
                        val p = mockHayStack.readMapById(point.value.id)
                        println(p)
                    }
                }
            }
        }
        println("Before===================")
        val diffManger = DiffManger(null)
        /*val migrationHandler = MigrationHandler(mockHayStack)
        val newVersionFiles = getModelFileVersionDetails(DiffManger.NEW_VERSION)*/
        val versionFiles = getModelFileVersionDetails(DiffManger.VERSION)
/*
        diffManger.updateEquipModels(newVersionFiles,versionFiles,migrationHandler, "@TestSiteRef")
*/
        println(versionFiles)
        Domain.site?.floors?.entries?.forEach{
            val floor = it.value
            assert(floor.rooms.size == 1)
            floor.rooms.entries.forEach{ r ->
                val room =  r.value
                assert(room.equips.size == 1)
                room.equips.entries.forEach{ e ->
                    val equip = e.value
                    equip.points.entries.forEach{ point ->
                        val p = mockHayStack.readMapById(point.value.id)
                        println(p)
                    }
                }
            }
        }
    }
    private fun getModelFileVersionDetails(fileName: String): List<ModelMeta> {
        val versionDetails = ResourceHelper.getModelVersion(fileName)
        val models = mutableListOf<ModelMeta>()
        versionDetails.keys().forEach {
            val version = versionDetails.getJSONObject(it as String)
            models.add(ModelMeta(
                modelId = it, Version(
                major = version.getInt(MAJOR),
                minor = version.getInt(MINOR),
                patch = version.getInt(PATCH))))
        }
        return models
    }


    fun mokeSiteDetilas (hayStack: CCUHsApi){

        val s = Site.Builder()
            .setDisplayName("75F")
            .addMarker("site")
            .setGeoCity("Burnsville")
            .setGeoState("MN")
            .setTz("Chicago")
            .setArea(1000).build()
        hayStack.addSite(s)

        val siteMap: HashMap<*, *> = hayStack.read(Tags.SITE)
        val siteRef = siteMap[Tags.ID] as String?
        val siteDis = siteMap["dis"] as String?

        val f = Floor.Builder()
            .setDisplayName("Floor1")
            .setSiteRef(siteRef)
            .build()

        val floorRef = hayStack.addFloor(f)

        val z = Zone.Builder()
            .setDisplayName("Zone1")
            .setFloorRef(floorRef)
            .setSiteRef(siteRef)
            .build()
        val zoneRef = hayStack.addZone(z)
       val dmModel = a75f.io.domain.ResourceHelper.loadProfileModelDefinition("models/645b27a27f70f27a05508363.json")

        dmModel.let {
            val equipBuilder = ProfileEquipBuilder(hayStack)
            val profileConfig = getTestProfileConfig()
            profileConfig.floorRef = floorRef
            profileConfig.roomRef = zoneRef
            equipBuilder.buildEquipAndPoints(profileConfig, dmModel,"@TestSiteRef", "TestEquip")
        }
        DomainManager.buildDomain(hayStack)
    }
    private fun getTestProfileConfig() : ProfileConfiguration {
        val profile = HyperStat2pfcuTestConfiguration(1000,"HS",0, "","")

        profile.autoForcedOccupied.enabled = true
        profile.autoAway.enabled = true

        return profile
    }
}