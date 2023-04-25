package a75f.io.logic.diag

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.mock.MockCcuHsApi
import a75f.io.logic.diag.otastatus.OtaStatusDiagPoint
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Created by Manjunath K on 28-02-2023.
 */

class OtaStatusDiagPointKtTest {

    lateinit var hayStack: CCUHsApi
    @Before
    fun setUp() {
        hayStack = MockCcuHsApi()
        val s75f = Site.Builder()
            .setDisplayName("testSite")
            .addMarker("site")
            .setGeoCity("siteCity")
            .setGeoState("siteState")
            .setTz("Calcutta")
            .setGeoZip("580118")
            .setGeoCountry("india")
            .setOrgnization("org")
            .setInstaller("inst.@gm.cm")
            .setFcManager("fc@gm.cm")
            .setGeoAddress("bangalre")
            .setGeoFence("2.0")
            .setArea(10000).build()

        hayStack.addSite(s75f)
    }

    @After
    fun tearDown() {
        hayStack.tagsDb.boxStore.close()
    }


    @Test
    fun createOtaStatusDiagPoint() {
       val point =  OtaStatusDiagPoint.createOtaStatusDiagPoint("equipDis","equipRef","siteRef","tz")
        Assert.assertTrue(point != null)
    }


    @Test
    fun addOtaStatusPoint(){
        OtaStatusDiagPoint.addOTAStatusPoint("equipDis","equipRef","siteRef","roomref","floorRef",1000,"tz",
            hayStack
        )
        val point = hayStack.readEntity("ota and status")
        Assert.assertTrue(point != null)
    }

}