package a75f.io.domain.smartNode.vavNoFan

import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.domain.ResourceHelper
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.EquipBuilder
import a75f.io.domain.logic.ProfileEquipBuilder
import io.seventyfivef.domainmodeler.client.ModelDirective
import org.junit.After
import org.junit.Before
import org.junit.Test

class SmartNodeVavNoFanEquipBuilderTest {
    private lateinit var dmModel: ModelDirective

    private var mockHayStack = MockCcuHsApi()
    @Before
    fun setUp() {
        dmModel = ResourceHelper.loadProfileModelDefinition("VAVTestModel.json")
    }

    @After
    fun tearDown() {
        mockHayStack.closeDb()
        mockHayStack.clearDb()
    }

    @Test
    fun testCreateEquip() {

        dmModel?.let {
            val equipBuilder = ProfileEquipBuilder(mockHayStack)
            equipBuilder.buildEquipAndPoints(getTestProfileConfig(), dmModel, "@TestSiteRef", "TestEquip")
        }

        val equip = mockHayStack.readAllEntities("equip")
        assert(equip.size == 1)

        val points = mockHayStack.readAllEntities("point")
        assert(points.size == 72)

    }

    @Test
    fun testCreatePoints() {

        dmModel?.let {
            val equipBuilder = ProfileEquipBuilder(mockHayStack)
            equipBuilder.buildEquipAndPoints(getTestProfileConfig(), dmModel, "@TestSiteRef", "TestEquip")
        }

        val points = mockHayStack.readAllEntities("point")
        println("# of points: " + points.size) //assert(points.size == 5)

    }

    private fun getTestProfileConfig() : ProfileConfiguration {
        val profile = VavNoFanTestConfiguration(1000,"SN",0, "@TestRoomRef","@TestFloorRef")

        return profile
    }
}