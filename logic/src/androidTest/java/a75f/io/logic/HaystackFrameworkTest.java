package a75f.io.logic;


import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.projecthaystack.HDict;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;

@RunWith(AndroidJUnit4.class)
public class HaystackFrameworkTest {

    private static final String TAG = "HaystackFrameworkTest";

    @Test
    public void testAddSite() {


        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() == 0) {
            Site s75f = new Site.Builder()
                    .setDisplayName("TestSite")
                    .addMarker("site")
                    .addMarker("orphan")
                    .setGeoCity("TestSiteCity")
                    .setGeoState("MN")
                    .setTz("Chicago")
                    .setGeoZip("TestSiteZip")
                    .setArea(10000).build();
            String localSiteId = CCUHsApi.getInstance().addSite(s75f);
            Log.i(TAG, "LocalSiteID: " + localSiteId);
            CCUHsApi.getInstance().log();

            CCUHsApi.getInstance().syncEntityTree();

        } else {
            Log.i(TAG, "Site ID already exists");
            CCUHsApi.getInstance().log();
        }
    }


    @Test
    public void testAddDefaultScheduleToBusinessObject() {
        new CCUHsApi(InstrumentationRegistry.getContext());

        addSite();
        String testDis = "Default Site Schedule";
        String localId = DefaultSchedules.generateDefaultSchedule();
        System.out.println("Dict To String: " + localId);
        HDict scheduleDict = CCUHsApi.getInstance().readHDictById(localId);
        Schedule build = new Schedule.Builder().setHDict(scheduleDict).build();
        System.out.println("Schedule Zinc: " + scheduleDict.toZinc());
        HDict postHDict = build.getScheduleHDict();
        System.out.println("Schedule Zinc! " + postHDict.toZinc());

        Assert.assertEquals("Default Site Schedule", build.getDis());
        Assert.assertTrue(postHDict.equals(scheduleDict));

        build.getScheduledValue();
    }

    private void addSite() {
        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() == 0) {
            Site s75f = new Site.Builder()
                    .setDisplayName("TestSite")
                    .addMarker("site")
                    .addMarker("orphan")
                    .setGeoCity("TestSiteCity")
                    .setGeoState("MN")
                    .setTz("Chicago")
                    .setGeoZip("TestSiteZip")
                    .setArea(10000).build();
            String localSiteId = CCUHsApi.getInstance().addSite(s75f);
            Log.i(TAG, "LocalSiteID: " + localSiteId);
            CCUHsApi.getInstance().log();
        }
    }

}
