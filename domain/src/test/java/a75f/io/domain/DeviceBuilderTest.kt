package a75f.io.domain

import a75f.io.api.haystack.Floor
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Zone
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.EquipBuilder
import io.seventyfivef.domainmodeler.client.SeventyFiveFDeviceDirective
import org.junit.Before
import org.junit.Test

class DeviceBuilderTest {

    private lateinit var dmModel: SeventyFiveFDeviceDirective
    @Before
    fun setUp() {
        dmModel = ResourceHelper.loadDeviceModelDefinition("DeviceBuilder_TestModel.json")
    }

    @Test
    fun testCreateDevice() {
        val mockHayStack = MockCcuHsApi()

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
        val siteDis = siteMap["dis"] as String?

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
            val equipBuilder = EquipBuilder(mockHayStack)
            val profileConfig = getTestProfileConfig()
            profileConfig.floorRef = floorRef
            profileConfig.roomRef = zoneRef

            val equipId = equipBuilder.buildEquipAndPoints(profileConfig, profileModel)

            val deviceBuilder = DeviceBuilder(mockHayStack, entityMapper)
            deviceBuilder.buildDeviceAndPoints(profileConfig, dmModel, equipId)
        }

        dumpDomain()
        val device = mockHayStack.readAllEntities("device")
        assert(device.size == 1)

        val thermistor = mockHayStack.readEntity("point and domainName == \"thermistor1\"")
        val supplyAirTemp = mockHayStack.readEntity("point and domainName == \"supplyAirTemp\"")
        println(thermistor)
        println(supplyAirTemp)
        assert(thermistor["pointRef"].toString() == supplyAirTemp["id"].toString())

    }

    @Test
    fun testCreatePoints() {
        val mockHayStack = MockCcuHsApi()

        dmModel?.let {
            val equipBuilder = EquipBuilder(mockHayStack)
            equipBuilder.buildEquipAndPoints(getTestProfileConfig(), dmModel)
        }

        val points = mockHayStack.readAllEntities("point")
        assert(points.size == 4)

    }

    private fun getTestProfileConfig() : ProfileConfiguration {
        val profile = HyperStat2pfcuTestConfiguration(1000,"HS",0, "","")

        profile.autoForcedOccupied.enabled = true
        profile.autoAway.enabled = true

        return profile
    }

    private fun dumpDomain() {
        println("## Dump Domain start ## ")
        Domain.site?.floors?.entries?.forEach{
            val floor = it.value
            println(floor)
            floor.rooms.entries.forEach{ r ->
                val room =  r.value
                println(room)
                room.equips.entries.forEach{ e ->
                    val equip = e.value
                    println(equip)
                    equip.points.entries.forEach{ point -> println(point) }
                }
                room.devices.entries.forEach{ e ->
                    val device = e.value
                    println(device)
                    device.points.entries.forEach{ point -> println(point) }
                }
            }
        }
        println("## Dump Domain end ## ")
    }

}