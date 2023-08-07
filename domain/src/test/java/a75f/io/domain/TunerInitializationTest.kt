package a75f.io.domain

import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Floor
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Zone
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.ProfileEquipBuilder
import io.seventyfivef.domainmodeler.client.ModelDirective
import org.junit.After
import org.junit.Before
import org.junit.Test

class TunerInitializationTest {

    private lateinit var dmModel: ModelDirective
    private val hayStack = MockCcuHsApi()
    @Before
    fun setUp() {
        dmModel = ResourceHelper.loadProfileModelDefinition("Tuner_TestModel.json")
    }

    @After
    fun tearDown() {
        hayStack.closeDb()
        hayStack.clearDb()
    }

    @Test
    fun testTunerInitialization() {

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

        val buildingTunerEquip = Equip.Builder()
            .setSiteRef(siteRef)
            .setDisplayName("Building Tuner")
            .setDomainName("buildingTuner")
            .setRoomRef(zoneRef)
            .setFloorRef(floorRef)
            .addMarker("building")
            .addMarker("tuner")
            .build()
        val equipRef = hayStack.addEquip(buildingTunerEquip)

        val testTuner = Point.Builder()
            .setDisplayName("$siteDis-TestTuner")
            .setDomainName("testTuner")
            .setEquipRef(equipRef)
            .setSiteRef(siteRef)
            .addMarker("default").addMarker("test").addMarker("tuner")
            .addMarker("writable").addMarker("his")
            .setTz("Chicago")
            .setUnit("\u00B0F")
            .build()

        val pointId = hayStack.addPoint(testTuner)
        hayStack.writePointLocal(pointId, 17, "TunerInitialzationTest", 500.0,0 )
        DomainManager.buildDomain(hayStack)

        dmModel?.let {
            val equipBuilder = ProfileEquipBuilder(hayStack)
            val profileConfig = getTestProfileConfig()
            profileConfig.floorRef = floorRef
            profileConfig.roomRef = zoneRef
            equipBuilder.buildEquipAndPoints(profileConfig, dmModel)
            TestUtil.dumpDomain()
            val testTunerPoint = hayStack.readEntity("point and domainName == \"testTuner\" and roomRef == \"$zoneRef\"")
            val testTuner = hayStack.readPoint(testTunerPoint["id"].toString())
            println(testTuner)
        }





    }

    private fun getTestProfileConfig() : ProfileConfiguration {
        val profile = HyperStat2pfcuTestConfiguration(7000,"HS",0, "","")

        profile.autoForcedOccupied.enabled = true
        profile.autoAway.enabled = true

        return profile
    }
}