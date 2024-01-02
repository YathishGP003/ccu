package a75f.io.domain

import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.ProfileEquipBuilder
import io.seventyfivef.domainmodeler.client.ModelDirective
import org.junit.After
import org.junit.Before
import org.junit.Test

class ProfileEquipBuilderTest {
     private lateinit var dmModel: ModelDirective

    private var mockHayStack = MockCcuHsApi()
    @Before
    fun setUp() {
        dmModel = ResourceHelper.loadProfileModelDefinition("EquipBuilder_TestModel.json")
    }

    @After
    fun tearDown() {
        mockHayStack.closeDb()
        mockHayStack.clearDb()
    }

    @Test
    fun testCreateEquip() {

        val equipBuilder = ProfileEquipBuilder(mockHayStack)
        equipBuilder.buildEquipAndPoints(getTestProfileConfig(), dmModel, "@TestSiteRef")
        val equip = mockHayStack.readAllEntities("equip")
        assert(equip.size == 1)

        val points = mockHayStack.readAllEntities("point")
        points.forEach{ println(it) }

    }

    @Test
    fun testCreatePoints() {

        val equipBuilder = ProfileEquipBuilder(mockHayStack)
        equipBuilder.buildEquipAndPoints(getTestProfileConfig(), dmModel, "@TestSiteRef")

        val points = mockHayStack.readAllEntities("point")
        assert(points.size == 5)

    }

    /**
     * Update configuration remaps coolingStage1 to coolingStage2 and disables dcwbEnabled.
     * So update should delete coolingStage1 , dcwbValueLoopOuput points and create coolingStage2 point.
     */
    @Test
    fun testUpdatePoints() {
        val equipBuilder = ProfileEquipBuilder(mockHayStack)

        equipBuilder.buildEquipAndPoints(getTestProfileConfig(), dmModel, "@TestSiteRef")

        val points = mockHayStack.readAllEntities("point")
        points.forEach{
            println(it)
            println(mockHayStack.readDefaultValById(it["id"].toString()))
        }

        val profile = HyperStat2PfcuUpdateConfiguration(1000,"HS",0, "","")
        equipBuilder.updateEquipAndPoints(profile, dmModel, "@TestSiteRef")

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
    fun testUpdatePointsDomain() {
        val equipBuilder = ProfileEquipBuilder(mockHayStack)

        equipBuilder.buildEquipAndPoints(getTestProfileConfig(), dmModel, "@TestSiteRef")
        val points = mockHayStack.readAllEntities("point")
        points.forEach{
            println(it)
            println(mockHayStack.readDefaultValById(it["id"].toString()))
        }

        val profile = HyperStat2PfcuUpdateConfiguration(1000,"HS",0, "","")
        equipBuilder.updateEquipAndPoints(profile, dmModel, "@TestSiteRef")

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

        val equipBuilder = ProfileEquipBuilder(mockHayStack)
        equipBuilder.buildEquipAndPoints(getTestProfileConfig(), dmModel, "@TestSiteRef")
        val equipDict = mockHayStack.readHDict("equip")

        val equip = Equip.Builder().setHDict(equipDict).build()

        //assert(equip.tags.isNotEmpty())
        equip.tags.entries.forEach{println(it)}

    }

    @Test
    fun testVerifyPointTagValType() {
        val equipBuilder = ProfileEquipBuilder(mockHayStack)
        equipBuilder.buildEquipAndPoints(getTestProfileConfig(), dmModel, "@TestSiteRef")
        val pointDict = mockHayStack.readHDict("point and domainName == \"coolingStage1"+"\"")

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