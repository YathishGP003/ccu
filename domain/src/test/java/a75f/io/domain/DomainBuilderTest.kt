package a75f.io.domain

import a75f.io.api.haystack.Floor
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Zone
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.EquipBuilder
import io.seventyfivef.domainmodeler.client.ModelDirective
import org.junit.After
import org.junit.Before
import org.junit.Test

class DomainBuilderTest {

    private lateinit var dmModel: ModelDirective

    val hayStack = MockCcuHsApi()
    @Before
    fun setUp() {
        dmModel = ResourceHelper.loadProfileModelDefinition("DomainBuilder_TestModel.json")
    }

    @After
    fun tearDown() {
        hayStack.closeDb()
        hayStack.clearDb()
    }

    @Test
    fun buildDomain() {

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

        dmModel?.let {
            val equipBuilder = EquipBuilder(hayStack)
            val profileConfig = getTestProfileConfig()
            profileConfig.floorRef = floorRef
            profileConfig.roomRef = zoneRef
            equipBuilder.buildEquipAndPoints(profileConfig, dmModel)
        }

        DomainManager.buildDomain(hayStack)

        assert(Domain.site?.floors?.size  == 1)
        Domain.site?.floors?.entries?.forEach{
            val floor = it.value
            assert(floor.rooms.size == 1)
            floor.rooms.entries.forEach{ r ->
                val room =  r.value
                assert(room.equips.size == 1)
                room.equips.entries.forEach{ e ->
                    val equip = e.value
                    assert(equip.points.size == 4)
                    equip.points.entries.forEach{ point -> println(point) }
                }
            }
        }



    }

    private fun getTestProfileConfig() : ProfileConfiguration {
        val profile = HyperStat2pfcuTestConfiguration(7000,"HS",0, "","")

        profile.autoForcedOccupied.enabled = true
        profile.autoAway.enabled = true

        return profile
    }

}