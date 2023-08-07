package a75f.io.domain

import a75f.io.api.haystack.Floor
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Zone
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import org.junit.After
import org.junit.Before
import org.junit.Test

class DeviceBuilderTest {

    private lateinit var dmModel: SeventyFiveFDeviceDirective
    private val mockHayStack = MockCcuHsApi()
    @Before
    fun setUp() {
        dmModel = ResourceHelper.loadDeviceModelDefinition("DeviceBuilder_TestModel.json")
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

        dmModel?.let {
            val profileModel = ResourceHelper.loadProfileModelDefinition("EquipBuilder_TestModel.json")
            val entityMapper = EntityMapper(profileModel)
            val equipBuilder = ProfileEquipBuilder(mockHayStack)
            val profileConfig = getTestProfileConfig()
            profileConfig.floorRef = floorRef
            profileConfig.roomRef = zoneRef

            val equipId = equipBuilder.buildEquipAndPoints(profileConfig, profileModel)

            val deviceBuilder = DeviceBuilder(mockHayStack, entityMapper)
            deviceBuilder.buildDeviceAndPoints(profileConfig, dmModel, equipId)
        }

        TestUtil.dumpDomain()
        val device = mockHayStack.readAllEntities("device")
        assert(device.size == 1)

        val thermistor = mockHayStack.readEntity("point and domainName == \"thermistor1\"")
        val supplyAirTemp = mockHayStack.readEntity("point and domainName == \"supplyAirTemp\"")
        println(thermistor)
        println(supplyAirTemp)
        assert(thermistor["pointRef"].toString() == supplyAirTemp["id"].toString())

    }

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

        val equipId = equipBuilder.buildEquipAndPoints(profileConfig, profileModel)

        val deviceBuilder = DeviceBuilder(mockHayStack, entityMapper)
        deviceBuilder.buildDeviceAndPoints(profileConfig, dmModel, equipId)

        TestUtil.dumpDomain()

        val relay1 = mockHayStack.readEntity("point and domainName == \"relay1Output\"")
        println(relay1)

        val updatedConfig = HyperStat2PfcuDeviceUpdateConfiguration(1000,"HS",0, "","")
        equipBuilder.updateEquipAndPoints(updatedConfig, profileModel)
        deviceBuilder.updateDeviceAndPoints(updatedConfig, dmModel, equipId)

        val relay1Op = mockHayStack.readEntity("point and domainName == \"relay1Output\"")
        println(relay1Op)
    }

    private fun getTestProfileConfig() : ProfileConfiguration {
        val profile = HyperStat2pfcuTestConfiguration(1000,"HS",0, "","")

        profile.autoForcedOccupied.enabled = true
        profile.autoAway.enabled = true

        return profile
    }
}