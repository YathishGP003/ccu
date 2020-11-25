package a75f.io.haystack.api;

import org.junit.Test;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.client.HClient;
import org.projecthaystack.server.HStdOps;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.sync.HttpUtil;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class TestHayStackClient
{
    @Test
    public void testHaystackAPI(){

        HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), "testhaystack", "testpassword");

        HDict hDict = new HDictBuilder().add("filter", "point").toDict();
        HGrid hGrid = hClient.call(HStdOps.read.name(), HGridBuilder.dictToGrid(hDict));
        hGrid.dump();

        /*AndroidHSClient hayStackClient = new AndroidHSClient();
        
        String cmd = "about";//formats,ops
        HGrid resGrid = hayStackClient.call(cmd, null);
    
        //String filter = ""; // read,nav
        //hayStackClient.readAll(filter);
        
        System.out.println(HZincWriter.gridToString(resGrid));*/


    }

    @Test
    public void testAuth(){

        String response = HttpUtil.executePost("https://renatusv2.azurewebsites.net/about", "");
        System.out.println("Response: " + response);
        /*AndroidHSClient hayStackClient = new AndroidHSClient();

        String cmd = "about";//formats,ops
        HGrid resGrid = hayStackClient.call(cmd, null);

        //String filter = ""; // read,nav
        //hayStackClient.readAll(filter);

        System.out.println(HZincWriter.gridToString(resGrid));*/


    }
    
    @Test
    public void testCode() {
        //String myString = "426FF7A1";
        //Long i = Long.parseLong(myString, 16);
        
        Integer i = 0x42 << 24 | 0x6F << 16 | 0xF7 << 8 | 0xA1;
        Float f = Float.intBitsToFloat(i);
        System.out.println(f);
    
    }
    
}