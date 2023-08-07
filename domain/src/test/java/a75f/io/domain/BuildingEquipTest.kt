package a75f.io.domain

import a75f.io.api.haystack.mock.MockCcuHsApi
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
        dmModel = ResourceHelper.loadModel("building_tuner_equip.json") as SeventyFiveFTunerDirective
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

        dmModel.points.forEach{println(it.domainName)
            val point = mockHayStack.readEntity("point and domainName == \"${it.domainName}\"")
            println(point)
            assert(point.isNotEmpty())
        }



       /* val equip = mockHayStack.readAllEntities("equip")
        assert(equip.size == 1)

        val points = mockHayStack.readAllEntities("point")
        points.forEach{ println(it) }*/

    }
}