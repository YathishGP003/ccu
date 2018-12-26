package a75f.io.renatus.unittests;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HStr;
import org.projecthaystack.client.HClient;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.CCUTagsDb;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class HSClientTest
{

    Context appContext;


    @Test
    public void useAppContext() throws Exception
    {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("haystacktest.android.com.a75.test", appContext.getPackageName());
        Globals.getInstance().initilize();

    }



    @Test
    public void getToken()
    {

        String scope = "";

        String tokenJson = HttpUtil.authorizeToken(HttpUtil.CLIENT_ID, scope, HttpUtil.CLIENT_SECRET, HttpUtil.TENANT_ID);
        System.out.println("Token : " + tokenJson);
    }

    @Test
    public void addSite()
    {
        String navId = "5be9af1c02743900e9e762f8";
        loadExistingSite(navId);
    }


    public void loadExistingSite(String siteId) {


        //String siteIdAzure = "123";  //Azure ID
        HGrid hGrid = CCUHsApi.getInstance().hsClient.nav(HStr.make(siteId));

        /* Call this seperately. */
        /* Traverse the tree, create from the hashmap site */
        /* Add each mapping a local UUID */

        hGrid.dump();
        //Get this site from azure.
        //Site azureSite = CCUHsApi.getInstance().getSiteFromAzure();

        //Pre-Populate these fields
        //Set it as an orphan site and orphan ccu

    }

    @Test
    public void testGettingSite()
    {

        /* Sync a site*/
        HClient hClient = new HClient(HttpUtil.HAYSTACK_URL, "ryan", "ryan");
        HDict navIdDict = new HDictBuilder().add("navId", HRef.make("5be9af1c02743900e9e762f8")).toDict();
        HGrid hGrid = HGridBuilder.dictToGrid(navIdDict);

        HGrid sync = hClient.call("sync", hGrid);

        sync.dump();
    }

    @Test
    public void testGettingSiteDetails()
    {
        HClient hClient = new HClient(HttpUtil.HAYSTACK_URL, "ryan", "ryan");
        HDict navIdDict = new HDictBuilder().add("id", HRef.make("5be9af1c02743900e9e762f8")).toDict();
        HGrid hGrid = HGridBuilder.dictToGrid(navIdDict);

        HGrid readSite = hClient.call("read", hGrid);
        readSite.dump();

    }


    @Test
    public void testReverseSyncingSite()
    {
        CCUHsApi.getInstance().syncExistingSite("5c101ea60e8e3d00eed0b6bb");
        CCUHsApi.getInstance().tagsDb.log();
        CCUHsApi.getInstance().saveTagsData();

    }


    @Test
    public void testAddDefaultScheduleToBusinessObject()
    {

        String testDis = "Default Site Schedule";
        String localId = DefaultSchedules.generateDefaultSchedule();
        System.out.println("Dict To String: " + localId);

        Schedule build = new Schedule.Builder().setHashMap(CCUHsApi.getInstance().readHDictById(localId)).build();

        Assert.assertEquals("Default Site Schedule", build.getDis());
    }


}
