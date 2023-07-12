package a75f.io.domain

import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.EquipBuilder
import io.seventyfivef.domainmodeler.client.ModelDirective
import org.junit.Before
import org.junit.Test

class EquipBuilderTest {
     private lateinit var dmModel: ModelDirective

    @Before
    fun setUp() {
        dmModel = ResourceHelper.loadProfileModelDefinition("EquipBuilder_TestModel.json")
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

    /**
     * Update configuration remaps coolingStage1 to coolingStage2 and disables dcwbEnabled.
     * So update should delete coolingStage1 , dcwbValueLoopOuput points and create coolingStage2 point.
     */
    @Test
    fun testUpdatePoints() {
        val mockHayStack = MockCcuHsApi()
        val equipBuilder = EquipBuilder(mockHayStack)

        dmModel?.let {
            equipBuilder.buildEquipAndPoints(getTestProfileConfig(), dmModel)
        }

        val points = mockHayStack.readAllEntities("point")
        points.forEach{
            println(it)
            println(mockHayStack.readDefaultValById(it["id"].toString()))
        }

        val profile = HyperStat2PfcuUpdateConfiguration(1000,"HS",0, "","")
        equipBuilder.updateEquipAndPoints(profile, dmModel)

        val dcwbValveLoopOp = mockHayStack.readEntity("domainName == \"dcwbValveLoopOutput\"")
        assert(dcwbValveLoopOp.isEmpty())

        val coolingStage1 = mockHayStack.readEntity("domainName == \"coolingStage1\"")
        assert(coolingStage1.isEmpty())

        val coolingStage2 = mockHayStack.readEntity("domainName == \"coolingStage2\"")
        assert(coolingStage2.isNotEmpty())

        val updatedPoints = mockHayStack.readAllEntities("point")
        println("Updated ############")
        updatedPoints.forEach{
            println(it)
            println(mockHayStack.readDefaultValById(it["id"].toString()))
        }
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