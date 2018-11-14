package haystacktest.android.com.a75;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.sync.HttpUtil;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class SiteTest
{
    @Test
    public void useAppContext() throws Exception
    {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("haystacktest.android.com.a75.test", appContext.getPackageName());
    }



    @Test
    public void getToken()
    {

        String scope = "";


        String tokenJson = HttpUtil.authorizeToken(HttpUtil.CLIENT_ID, scope, HttpUtil.CLIENT_SECRET, HttpUtil.TENANT_ID);
        System.out.println("Token : " + tokenJson);
    }


    /**
     * Create a site
     * and query that the site exists locally.
     */
    @Test
    public void addSite()
    {
        new CCUHsApi(InstrumentationRegistry.getTargetContext());

        Site s75f = new Site.Builder()
                .setDisplayName("siteName")
                .addMarker("site")
                .addMarker("orphan")
                .setGeoCity("siteCity")
                .setGeoState("MN")
                .setTz("Chicago")
                .setGeoZip("56701")
                .setArea(10000).build();
        String localSiteId = CCUHsApi.getInstance().addSite(s75f);
        HashMap site = CCUHsApi.getInstance().read("site");
        logSite(site);
        Assert.assertNotNull(site);
    }


    @Test
    public void addCCU()
    {
        

    }

    private void logSite(HashMap site) {
        Log.i("Test", "Test");
        Iterator it = site.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Log.i("Test", pair.getKey() + " = " + pair.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }
    }

}
