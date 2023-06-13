package a75f.io.domain

import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.EquipBuilder
import a75f.io.domain.model.ModelDef
import org.junit.Before
import org.junit.Test

class EquipBuilderTest {
     private lateinit var dmModel: ModelDef

    @Before
    fun setUp() {
        dmModel = ResourceHelper.loadModelDefinition("EquipBuilder_TestModel.json")
    }

    @Test
    fun testCreateEquip() {
        val mockHayStack = MockCcuHsApi()

        dmModel?.let {
            val equipBuilder = EquipBuilder(mockHayStack)
            equipBuilder.buildEquipAndPoints(getTestProfileConfig(), dmModel)
        }

        val equip = mockHayStack.readAllEntities("equip")
        assert(equip.size == 1)

        val points = mockHayStack.readAllEntities("point")
        points.forEach{ println(it) }

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

    @Test
    fun testVerifyEquipTagValType() {
        val mockHayStack = MockCcuHsApi()

        dmModel?.let {
            val equipBuilder = EquipBuilder(mockHayStack)
            equipBuilder.buildEquipAndPoints(getTestProfileConfig(), dmModel)
        }

        val equipDict = mockHayStack.readHDict("equip")

        val equip = Equip.Builder().setHDict(equipDict).build()

        //assert(equip.tags.isNotEmpty())
        equip.tags.entries.forEach{println(it)}

    }

    @Test
    fun testVerifyPointTagValType() {
        val mockHayStack = MockCcuHsApi()

        dmModel?.let {
            val equipBuilder = EquipBuilder(mockHayStack)
            equipBuilder.buildEquipAndPoints(getTestProfileConfig(), dmModel)
        }

        val pointDict = mockHayStack.readHDict("point")

        val point = Point.Builder().setHDict(pointDict).build()

        assert(point.tags.isNotEmpty())
        point.tags.entries.forEach{println(it)}

    }


    private fun getTestProfileConfig() : ProfileConfiguration {
        val profile = HyperStat2pfcuTestConfiguration(1000,"HS",0, "","")

        profile.autoForcedOccupied.enabled = true
        profile.autoAway.enabled = true

        return profile
    }
}