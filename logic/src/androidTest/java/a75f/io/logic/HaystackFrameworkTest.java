package a75f.io.logic;


import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.projecthaystack.HDict;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Site;

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


        Schedule siteSchedule = CCUHsApi.getInstance().getSiteSchedule();


        HGrid grid = HGridBuilder.dictToGrid(siteSchedule.getScheduleHDict());
        String systemScheduleGrid = HZincWriter.gridToString(grid);
        System.out.println("Grid: " + systemScheduleGrid);

        HZincReader reader = new HZincReader(systemScheduleGrid);
        Log.i(TAG, "######Reader Dump######");
        HGrid hGrid = reader.readGrid();

        HDict hDict = hGrid.row(0);
        Schedule schedule = new Schedule.Builder().setHDict(hDict).build();

        CCUHsApi.getInstance().updateSchedule(schedule);
        Schedule siteSchedule2 = CCUHsApi.getInstance().getSiteSchedule();


        HGrid grid2 = HGridBuilder.dictToGrid(siteSchedule2.getScheduleHDict());
        String systemScheduleGrid2 = HZincWriter.gridToString(grid2);
        System.out.println("Grid: " + systemScheduleGrid2);

        HZincReader reader2 = new HZincReader(systemScheduleGrid2);
        Log.i(TAG, "######Reader2 Dump######");
        HGrid hGrid2 = reader2.readGrid();

        HDict hDict2 = hGrid2.row(0);
        Schedule schedule2 = new Schedule.Builder().setHDict(hDict2).build();



        //Schedule siteSchedule = CCUHsApi.getInstance().getSiteSchedule();
        //Assert.assertTrue(postHDict.equals(siteSchedule));

        //build.getScheduledValue();
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
