package a75f.io.domain

import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.logic.TunerEquipBuilder
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective
import org.junit.After
import org.junit.Before
import org.junit.Test

class BuildingEquipTest {

    private lateinit var dmModel: SeventyFiveFTunerDirective

    private var mockHayStack = MockCcuHsApi()
    @Before
    fun setUp() {
        dmModel = ResourceHelper.loadModel("building_tuner_equip_1.0.json") as SeventyFiveFTunerDirective
    }

    @After
    fun tearDown() {
        mockHayStack.closeDb()
        mockHayStack.clearDb()
    }

    @Test
    fun testCreateEquip() {

        println(dmModel)

        val equipBuilder = TunerEquipBuilder(mockHayStack)
        equipBuilder.buildTunerEquipAndPoints(dmModel)

        val tunerEquip = mockHayStack.readEntity("equip and tuner")
        assert(tunerEquip.isNotEmpty())
        dmModel.points.forEach{println(it.domainName)
            val point = Domain.readPoint(it.domainName)
            println(point)
            assert(point.isNotEmpty())
        }
    }

    /**
     * Creates a tuner with model version 1.0
     * Updates it with versio 2.0 where forcedOccupiedTime (val 120) point is deleted and forcedOccupiedTimeTest
     * added with a different value (220).
     */
    @Test
    fun testUpdateEquip() {

        println(dmModel)

        val equipBuilder = TunerEquipBuilder(mockHayStack)
        equipBuilder.buildTunerEquipAndPoints(dmModel)

        val pointOrig = Domain.readPoint("forcedOccupiedTime")
        assert(pointOrig.isNotEmpty())

        val pointArrayOrig = mockHayStack.readPoint(pointOrig["id"].toString())
        assert(pointArrayOrig[16]["val"].toString().toInt() == 120)

        val updatedModel = ResourceHelper.loadModel("building_tuner_equip_2.0.json") as SeventyFiveFTunerDirective

        val tunerEquip = mockHayStack.readEntity("equip and tuner")
        equipBuilder.updateEquipAndPoints(updatedModel, tunerEquip["id"].toString())
        val addedPoint = Domain.readPoint("forcedOccupiedTimeTest")
        println(addedPoint.isNotEmpty())
        val updatedPointArr = mockHayStack.readPoint(addedPoint["id"].toString())
        assert(updatedPointArr[16]["val"].toString().toInt() == 220)
        val deletedPoint = Domain.readPoint("forcedOccupiedTime")
        assert(deletedPoint.isEmpty())
    }
}