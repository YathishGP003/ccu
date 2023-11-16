package a75f.io.domain.smartNode.vavNoFan

import a75f.io.api.haystack.Floor
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Zone
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.domain.ResourceHelper
import a75f.io.domain.TestUtil
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import org.junit.After
import org.junit.Before
import org.junit.Test

class SmartNodeVavNoFanDeviceBuilderTest {

    private lateinit var dmModel: SeventyFiveFDeviceDirective
    private val mockHayStack = MockCcuHsApi()
    @Before
    fun setUp() {
        dmModel = ResourceHelper.loadDeviceModelDefinition("snTestDeviceModel.json")
    }

    @After
    fun tearDown() {
        mockHayStack.closeDb()
        mockHayStack.clearDb()
    }

    @Test
    fun testCreateDevice() {

        val s = Site.Builder()
            .setDisplayName("75F")
            .addMarker("site")
            .setGeoCity("Burnsville")
            .setGeoState("MN")
            .setTz("Chicago")
            .setArea(1000).build()
        mockHayStack.addSite(s)

        val siteMap: HashMap<*, *> = mockHayStack.read(Tags.SITE)
        val siteRef = siteMap[Tags.ID] as String?

        val f = Floor.Builder()
            .setDisplayName("Floor1")
            .setSiteRef(siteRef)
            .build()

        val floorRef = mockHayStack.addFloor(f)

        val z = Zone.Builder()
            .setDisplayName("Zone1")
            .setFloorRef(floorRef)
            .setSiteRef(siteRef)
            .build()
        val zoneRef = mockHayStack.addZone(z)

        DomainManager.buildDomain(mockHayStack)
        val profileModel = ResourceHelper.loadProfileModelDefinition("VAVTestModel.json")
        val entityMapper = EntityMapper(profileModel)
        val equipBuilder = ProfileEquipBuilder(mockHayStack)
        val profileConfig = getTestProfileConfig()
        profileConfig.floorRef = floorRef
        profileConfig.roomRef = zoneRef

        val equipId = equipBuilder.buildEquipAndPoints(profileConfig, profileModel, "@TestSiteRef", "TestEquip")

        val deviceBuilder = DeviceBuilder(mockHayStack, entityMapper)
        deviceBuilder.buildDeviceAndPoints(profileConfig, dmModel, equipId, "@TestSiteRef", "TestSite-SN-1000")
        //TestUtil.dumpDomain()
        val device = mockHayStack.readAllEntities("device and node and addr == \"1000\"")
        println("DEVICE: " + device)
        assert(device.size == 1)

        val points = mockHayStack.readAllEntities("point and deviceRef")
        points.forEach { println(it) }
        assert(points.size == 16)

        /*
        val thermistor = mockHayStack.readEntity("point and domainName == \"currentTemp\"")
        val supplyAirTemp = mockHayStack.readEntity("point and domainName == \"currentTemp\"")
        println("\n")
        println(thermistor)
        println(supplyAirTemp)
        assert(thermistor["pointRef"].toString() == supplyAirTemp["id"].toString())
        */
    }

    /*
    @Test
    fun testUpdateDevice() {
        val s = Site.Builder()
            .setDisplayName("75F")
            .addMarker("site")
            .setGeoCity("Burnsville")
            .setGeoState("MN")
            .setTz("Chicago")
            .setArea(1000).build()
        mockHayStack.addSite(s)

        val siteMap: HashMap<*, *> = mockHayStack.read(Tags.SITE)
        val siteRef = siteMap[Tags.ID] as String?

        val f = Floor.Builder()
            .setDisplayName("Floor1")
            .setSiteRef(siteRef)
            .build()

        val floorRef = mockHayStack.addFloor(f)

        val z = Zone.Builder()
            .setDisplayName("Zone1")
            .setFloorRef(floorRef)
            .setSiteRef(siteRef)
            .build()
        val zoneRef = mockHayStack.addZone(z)

        DomainManager.buildDomain(mockHayStack)

        val profileModel = ResourceHelper.loadProfileModelDefinition("EquipBuilder_TestModel.json")
        val entityMapper = EntityMapper(profileModel)
        val equipBuilder = ProfileEquipBuilder(mockHayStack)

        val profileConfig = getTestProfileConfig()
        profileConfig.floorRef = floorRef
        profileConfig.roomRef = zoneRef

        val equipId = equipBuilder.buildEquipAndPoints(profileConfig, profileModel, "@TestSiteRef", "TestEquip")

        val deviceBuilder = DeviceBuilder(mockHayStack, entityMapper)
        deviceBuilder.buildDeviceAndPoints(profileConfig, dmModel, equipId, "@TestSiteRef")

        TestUtil.dumpDomain()

        val relay1 = mockHayStack.readEntity("point and domainName == \"relay1Output\"")
        println(relay1)

        val updatedConfig = HyperStat2PfcuDeviceUpdateConfiguration(1000,"HS",0, "","")
        equipBuilder.updateEquipAndPoints(updatedConfig, profileModel, "@TestSiteRef", "TestEquip")
        deviceBuilder.updateDeviceAndPoints(updatedConfig, dmModel, equipId, "@TestSiteRef")

        val relay1Op = mockHayStack.readEntity("point and domainName == \"relay1Output\"")
        println(relay1Op)
    }
    */
    private fun getTestProfileConfig() : ProfileConfiguration {
        val profile = VavNoFanTestConfiguration(1000,"SN",0, "@TestRoomRef","@TestFloorRef")

        return profile
    }
}