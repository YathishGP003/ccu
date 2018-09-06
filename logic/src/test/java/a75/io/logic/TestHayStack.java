package a75.io.logic;

import org.junit.Test;
import org.projecthaystack.HGrid;
import org.projecthaystack.io.HZincWriter;

import java.util.HashMap;

import a75f.io.logic.bo.haystack.Equip;
import a75f.io.logic.bo.haystack.Point;
import a75f.io.logic.bo.haystack.Site;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.haystack.AndroidHSClient;
import a75f.io.logic.haystack.CCUHsApi;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class TestHayStack
{
    @Test
    public void testHaystckAPI(){
        AndroidHSClient hayStackClient = new AndroidHSClient();
        
        String cmd = "about";//formats,ops
        HGrid resGrid = hayStackClient.call(cmd, null);
    
        //String filter = ""; // read,nav
        //hayStackClient.readAll(filter);
        
        System.out.println(HZincWriter.gridToString(resGrid));
        
        
    }
    
    @Test
    public void testHayStack()
    {
        Site s = new Site.Builder()
                         .setDisplayName("75F")
                         .addMarker("site")
                         .setGeoCity("Burnsville")
                         .setGeoState("MN")
                         .setTz("Chicago")
                         .setArea(1000).build();
        CCUHsApi.getInstance().addSite(s);
        
        Equip a = new Equip.Builder()
                        .setSiteRef("75F")
                        .setDisplayName("AHU-1")
                        .addMarker("equip")
                        .addMarker("ahu")
                        .build();
        CCUHsApi.getInstance().addEquip(a);
        
        Equip v = new Equip.Builder()
                          .setSiteRef("75F")
                          .setDisplayName("VAV")
                          .addMarker("equip")
                          .addMarker("vav")
                          .build();
        CCUHsApi.getInstance().addEquip(v);
        
        Point dtPoint = new Point.Builder()
                                .setDisplayName("DischargeTemp")
                                .setEquipRef("VAV")
                                .setSiteRef("75F")
                                .addMarker("discharge")
                                .addMarker("air").addMarker("temp").addMarker("sensor")
                                .setUnit("\u00B0F")
                                .build();
    
        Point dPoint = new Point.Builder()
                                .setDisplayName("DesiredTemp")
                                .setEquipRef("VAV")
                                .setSiteRef("75F")
                                .addMarker("zone")
                                .addMarker("air").addMarker("temp").addMarker("desired").addMarker("sp")
                                .setUnit("\u00B0F")
                                .build();
        SmartNode node = new SmartNode(7000);
        
        String dtRef = CCUHsApi.getInstance().addPoint(dtPoint);
        CCUHsApi.getInstance().addPoint(dPoint);
        
        node.analog1In.setPointRef(dtRef);
        CCUHsApi.getInstance().addPoint(node.analog1In);
        
        HashMap data = CCUHsApi.getInstance().tagsDb.getDbMap();
        System.out.println(data);
    
    
        AndroidHSClient hayStackClient = CCUHsApi.getInstance().hsClient;
        HGrid r = hayStackClient.readAll("point and deviceRef == \""+node.deviceRef+"\"");
        System.out.println(HZincWriter.gridToString(r));
    }
}