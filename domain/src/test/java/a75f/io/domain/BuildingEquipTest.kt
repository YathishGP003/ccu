package a75f.io.domain

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.logic.TunerEquipBuilder
import a75f.io.domain.migration.DiffManger
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective
import io.seventyfivef.ph.core.Tags
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

        val s = Site.Builder()
            .setDisplayName("75F")
            .addMarker("site")
            .setGeoCity("Burnsville")
            .setGeoState("MN")
            .setTz("Chicago")
            .setArea(1000).build()
        val siteRef = mockHayStack.addSite(s)

        val equipBuilder = TunerEquipBuilder(mockHayStack)
        equipBuilder.buildTunerEquipAndPoints(dmModel, siteRef)

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
        equipBuilder.buildTunerEquipAndPoints(dmModel, "@TestSiteRef")

        val pointOrig = Domain.readPoint("forcedOccupiedTime")
        assert(pointOrig.isNotEmpty())

        val pointArrayOrig = mockHayStack.readPoint(pointOrig["id"].toString())
        assert(pointArrayOrig[16]["val"].toString().toInt() == 120)

        val updatedModel = ResourceHelper.loadModel("building_tuner_equip_2.0.json") as SeventyFiveFTunerDirective

        val tunerEquip = mockHayStack.readEntity("equip and tuner")
        equipBuilder.updateEquipAndPoints(updatedModel, equipBuilder.getEntityConfigForUpdate(updatedModel, tunerEquip[Tags.ID].toString()) ,"@TestSiteRef")
        val addedPoint = Domain.readPoint("forcedOccupiedTimeTest")
        println(addedPoint.isNotEmpty())
        val updatedPointArr = mockHayStack.readPoint(addedPoint["id"].toString())
        assert(updatedPointArr[16]["val"].toString().toInt() == 220)
        val deletedPoint = Domain.readPoint("forcedOccupiedTime")
        assert(deletedPoint.isEmpty())
    }

    @Test
    fun testPackagedModelLoader() {
        val dmModel = ModelLoader.getBuildingEquipModelDef()
        assert(dmModel != null)
        println(dmModel.toString())

        assert(dmModel.points.isNotEmpty())
        dmModel.points.forEach{
            println(it)
        }
        assert(dmModel.domainName.isNotEmpty())
        println(dmModel.domainName)

        println(dmModel.id)
    }

    @Test
    fun testRegistrationJson() {
        val equipBuilder = TunerEquipBuilder(mockHayStack)
        equipBuilder.buildTunerEquipAndPoints(dmModel, "@TestSiteRef")

        println(mockHayStack.readEntity("tuner and equip"))
        println(mockHayStack.getCcuRegisterJson("a", "b", "c", "d", "e", "f", "g", "h", null))
    }
    // TODO : Need to fix this test
    fun tunerEquipUpgradeTest() {

        val equipBuilder = TunerEquipBuilder(mockHayStack)
        equipBuilder.buildTunerEquipAndPoints(dmModel, "@TestSiteRef")
        println(mockHayStack.readEntity("tuner and equip"))
        val diffManger = DiffManger(null)
        diffManger.processModelMigration("@TestSiteRef", null,"")
        val updatedPoint = Domain.readPoint("forcedOccupiedTime")
        println(updatedPoint)
        println(mockHayStack.readPointArr(updatedPoint.getId()))
    }

    private fun doCutOverMigrationIfRequired(haystack: CCUHsApi) {
        val buildingEquip = haystack.readEntity("equip and tuner");
        if (buildingEquip["domainName"]?.toString()?.isNotEmpty() == true) {
            CcuLog.i(Domain.LOG_TAG, "Building equip cut-over migration is complete.")
        } else {
            val equipBuilder = TunerEquipBuilder(haystack)
            val equipId = buildingEquip["id"].toString()
            equipBuilder.migrateBuildingTunerPointsForCutOver(equipId, haystack.site!!)
        }
    }

    @Test
    fun tunerEquipCutOverMigrationTest() {
        val s = Site.Builder()
            .setDisplayName("75F")
            .addMarker("site")
            .setGeoCity("Burnsville")
            .setGeoState("MN")
            .setTz("Chicago")
            .setArea(1000).build()
        mockHayStack.addSite(s)

        val siteMap: HashMap<*, *> = mockHayStack.read(a75f.io.api.haystack.Tags.SITE)
        val siteRef = siteMap[a75f.io.api.haystack.Tags.ID] as String?

        val tunerEquip = Equip.Builder()
            .setSiteRef(siteRef)
            .setDisplayName("75F-BuildingTuner")
            .addMarker("equip").addMarker("tuner")
            .setTz(siteMap["tz"].toString())
            .build()

        val equipRef = mockHayStack.addEquip(tunerEquip)

        val heatingPreconditioingRate = Point.Builder()
            .setDisplayName(tunerEquip.displayName + "-heatingPreconditioningRate")
            .setSiteRef(siteRef)
            .setEquipRef(equipRef).setHisInterpolate("cov")
            .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his")
            .addMarker("his")
            .addMarker("system").addMarker("heating").addMarker("precon").addMarker("rate")
            .addMarker("sp")
            .setMinVal("0").setMaxVal("60").setIncrementVal("1")
            .setTunerGroup("GENERIC")
            .setTz(siteMap["tz"].toString())
            .build()
        val heatingPreconditioingRateId = mockHayStack.addPoint(heatingPreconditioingRate)
        mockHayStack.writePointForCcuUser(
            heatingPreconditioingRateId,
            16,
            15.0,
            0
        )
        mockHayStack.writeHisValById(
            heatingPreconditioingRateId,
            15.0
        )

        val tunerEquipMap = mockHayStack.readEntity("tuner and equip")
        println(tunerEquip)
        val id = tunerEquipMap["id"].toString()
        println("Tuner Equip id $id")
        val tunerPoint = mockHayStack.readAllEntities("point and equipRef == \"$id\"")
        tunerPoint.forEach { println(it) }

        println("### Do CutOver Migration ###")
        doCutOverMigrationIfRequired(mockHayStack)

        val tunerPoints = mockHayStack.readAllEntities("point and equipRef == \"$id\"")
        println(tunerPoints.size)
        tunerPoints.forEach { println(it) }
    }
    private fun Map<Any, Any>.getId() : String {
        return this[Tags.ID].toString()
    }


}