package a75f.io.logic;


import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Site;

@RunWith(AndroidJUnit4.class)
public class HaystackFrameworkTest {

    private static final String TAG = "HaystackFrameworkTest";

    @Test
    public void testAddSite()
    {
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
}
