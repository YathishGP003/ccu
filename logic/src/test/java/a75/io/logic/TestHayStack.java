package a75.io.logic;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.client.HClient;
import org.projecthaystack.server.HStdOps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.haystack.device.SmartNode;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class TestHayStack
{

    @Test
    public void testHaystackAPI(){

        HClient hClient = new HClient(HttpUtil.HAYSTACK_URL, "testhaystack", "testpassword");

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
    public void testHayStack()
    {
        Site s = new Site.Builder()
                         .setDisplayName("75F")
                         .addMarker("site")
                         .setGeoCity("Burnsville")
                         .setGeoState("MN")
                         .setTz("Chicago")
                         .setArea(1000).build();
        
        
        Equip a = new Equip.Builder()
                        .setSiteRef("75F")
                        .setDisplayName("AHU_RP1455-1")
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
                                .setUnit("\u00B0F").build();

        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        SmartNode node = new SmartNode(7000, siteRef);
        
        String dtRef = CCUHsApi.getInstance().addPoint(dtPoint);
        CCUHsApi.getInstance().addPoint(dPoint);
        
        node.analog1In.setPointRef(dtRef);
        CCUHsApi.getInstance().addPoint(node.analog1In);
        
        //HashMap data = CCUHsApi.getInstance().tagsDb.getDbMap();
        //System.out.println(data);
        
        //HGrid r = hayStackClient.readAll("point and deviceRef == \""+node.deviceRef+"\"");
        //HGrid r = hayStackClient.readAll("point");
        //System.out.println(HZincWriter.gridToString(r));
        
        HashMap mm = CCUHsApi.getInstance().read("site");
        System.out.println(mm);
    }
    
    @Test
    public void testVAVPoints(){
    
        //Add site point if not done already.
        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() == 0) {
            //TODO - demo
            Site s75f = new Site.Builder()
                                .setDisplayName("75F")
                                .addMarker("site")
                                .setGeoCity("Burnsville")
                                .setGeoState("MN")
                                .setTz("Chicago")
                                .setArea(10000).build();
            CCUHsApi.getInstance().addSite(s75f);
        }
    
        //Create Logical points
        int nodeAddr = 7000;
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        Equip v = new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName(siteDis+"-VAV-"+nodeAddr)
                          .addMarker("equip")
                          .addMarker("vav")
                          .build();
        String equipRef = CCUHsApi.getInstance().addEquip(v);
    
        Point datPoint = new Point.Builder()
                                 .setDisplayName(siteDis+"VAV-"+nodeAddr+"-DischargeAirTemp")
                                 .setEquipRef(equipRef)
                                 .setSiteRef(siteRef)
                                 .addMarker("discharge")
                                 .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                 .setUnit("\u00B0F")
                                 .setGroup(String.valueOf(nodeAddr))
                                 .build();
    
        String datID = CCUHsApi.getInstance().addPoint(datPoint);
    
        Point eatPoint = new Point.Builder()
                                 .setDisplayName(siteDis+"VAV-"+nodeAddr+"-EnteringAirTemp")
                                 .setEquipRef(equipRef)
                                 .setSiteRef(siteRef)
                                 .addMarker("entering")
                                 .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                 .setUnit("\u00B0F")
                                 .setGroup(String.valueOf(nodeAddr))
                                 .build();
        String eatID = CCUHsApi.getInstance().addPoint(eatPoint);
    
        Point damperPos = new Point.Builder()
                                  .setDisplayName(siteDis+"VAV-"+nodeAddr+"-DamperPos")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .addMarker("air")
                                  .addMarker("damper").addMarker("cmd").addMarker("writable")
                                  .setUnit("\u00B0F")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .build();
    
        String dpID = CCUHsApi.getInstance().addPoint(damperPos);
    
        Point reheatPos = new Point.Builder()
                                  .setDisplayName(siteDis+"VAV-"+nodeAddr+"-ReheatPos")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .addMarker("reheat")
                                  .addMarker("water").addMarker("valve").addMarker("cmd").addMarker("writable")
                                  .setUnit("\u00B0F")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .build();
        String rhID = CCUHsApi.getInstance().addPoint(reheatPos);
    
        Point currentTemp = new Point.Builder()
                                    .setDisplayName(siteDis+"VAV-"+nodeAddr+"-currentTemp")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .addMarker("zone").addMarker("current")
                                    .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                    .setUnit("\u00B0F")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .build();
        String ctID = CCUHsApi.getInstance().addPoint(currentTemp);
    
        Point desiredTemp = new Point.Builder()
                                    .setDisplayName(siteDis+"VAV-"+nodeAddr+"-desiredTemp")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .addMarker("zone")
                                    .addMarker("air").addMarker("temp").addMarker("desired").addMarker("sp").addMarker("writable")
                                    .setUnit("\u00B0F")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .build();
        String dtID = CCUHsApi.getInstance().addPoint(desiredTemp);
    
    
        //Create Physical points and map
        SmartNode device = new SmartNode(nodeAddr, siteRef);
        device.th1In.setPointRef(datID);
        CCUHsApi.getInstance().addPoint(device.th1In);
        device.th2In.setPointRef(eatID);
        CCUHsApi.getInstance().addPoint(device.th2In);
        device.analog1Out.setPointRef(dpID);
        CCUHsApi.getInstance().addPoint(device.analog1Out);
        device.analog2Out.setPointRef(rhID);
        CCUHsApi.getInstance().addPoint(device.analog2Out);
        device.currentTemp.setPointRef(ctID);
        CCUHsApi.getInstance().addPoint(device.currentTemp);
    
        //HashMap data = CCUHsApi.getInstance().tagsDb.getDbMap();
        //System.out.println(data);
        ArrayList points = CCUHsApi.getInstance().readAll("point and desired and group == \"7000\"");
        //System.out.println(points);
    
        for (Object a : points) {
            HashMap m1 = (HashMap)a;
            System.out.println(a);
        }
        
        /*HashMap m1 = (HashMap)points.get(0);
        String id = m1.get("id").toString();
        
        System.out.println(id);
        CCUHsApi.getInstance().writePoint(id,9,"test",75.0,30);
        ArrayList b = CCUHsApi.getInstance().readPoint(id);
    
        for (Object a : b) {
            System.out.println(a);
        }*/
        
        setRoomTemp(74.0);
        System.out.println(getRoomTemp());
        
        setDesiredTemp(72.0);
        System.out.println(getDesiredTemp());
        
    }
    
    @Test
    public void testAPI() {
    
        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() == 0) {
            Site s75f = new Site.Builder()
                                .setDisplayName("75F")
                                .addMarker("site")
                                .setGeoCity("Burnsville")
                                .setGeoState("MN")
                                .setTz("Chicago")
                                .setArea(20000).build();
            CCUHsApi.getInstance().addSite(s75f);
        }
        HashMap s = CCUHsApi.getInstance().read("site");
        System.out.print(s);
    
        /*Site s = new Site.Builder()
                         .setDisplayName("75F")
                         .addMarker("site")
                         .setGeoCity("Burnsville")
                         .setGeoState("MN")
                         .setTz("Chicago")
                         .setArea(1000).build();
        CCUHsApi.getInstance().addSite(s);
        
    
        HDict dict = CCUHsApi.getInstance().hsClient.read("site");
    
        //ArrayList<HashMap> rowList = new ArrayList<>();
        HashMap<Object, Object> map = new HashMap<>();
        Iterator it = dict.iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            map.put(entry.getKey().toString(), entry.getValue().toString());
        }
        
        System.out.println(map);
    
        //HDict dict1 = CCUHsApi.getInstance().hsClient.read("site");
        HashMap<Object, Object> map1 = new HashMap<>();
        Iterator it1 = dict.iterator();
        while(it1.hasNext()) {
            Map.Entry entry = (Map.Entry)it1.next();
            map1.put(entry.getKey(), entry.getValue());
        }
        
        System.out.println(map1);*/
        
        
    }
    
    @Test
    public void testSerialization() {
        
    
        Site s = new Site.Builder()
                         .setDisplayName("75F")
                         .addMarker("site")
                         .setGeoCity("Burnsville")
                         .setGeoState("MN")
                         .setTz("Chicago")
                         .setArea(1000).build();
        CCUHsApi.getInstance().addSite(s);
        
        System.out.println(CCUHsApi.getInstance().tagsDb);
        System.out.println(CCUHsApi.getInstance().tagsDb.tagsMap);
    
       // System.out.println(toJson(CCUHsApi.getInstance().tagsDb.tagsMap));
        
    }
    
    private String toJson(HashMap mm){
        ObjectMapper m  = new ObjectMapper();
        try
        {
            //m.disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
            //m.disable(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);
            //m.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
            
            return m.writerWithDefaultPrettyPrinter().writeValueAsString(mm);
            
            
        }catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public double getRoomTemp()
    {
        return CCUHsApi.getInstance().readDefaultVal("point and air and temp and sensor and current and group == \""+7000+"\"");
        
    }
    public void setRoomTemp(double roomTemp)
    {
        String query ="point and air and temp and sensor and current and group == \""+7000+"\"";
        CCUHsApi.getInstance().writeDefaultVal(query, roomTemp);
    }
    
    public double getDesiredTemp()
    {
        return CCUHsApi.getInstance().readDefaultVal("point and air and temp and desired and sp and group == \""+7000+"\"");
    }
    public void setDesiredTemp(double desiredTemp)
    {
        String query ="point and air and temp and desired and sp and group == \""+7000+"\"";
        CCUHsApi.getInstance().writeDefaultVal(query, desiredTemp);
    }
    
    @Test
    public void testValTypes() {
        CCUHsApi hayStack = new CCUHsApi();
        hayStack.tagsDb.tagsMap = new HashMap<>();
        hayStack.tagsDb.writeArrays = new HashMap<>();
        int nodeAddr = 7000;
        
        Site s = new Site.Builder()
                         .setDisplayName("Name")
                         .addMarker("site")
                         .setGeoCity("Burnsville")
                         .setGeoState("MN")
                         .setTz("Chicago")
                         .setArea(1000).build();
        hayStack.addSite(s);
        
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        
        Equip v = new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName(siteDis+"-VAV-"+nodeAddr)
                          .setRoomRef("room")
                          .setFloorRef("floor")
                          .addMarker("equip")
                          .addMarker("vav")
                          .setGroup(String.valueOf(nodeAddr))
                          .build();
        String equipRef = hayStack.addEquip(v);
        
        Point testPoint = new Point.Builder()
                                  .setDisplayName(siteDis+"AHU_RP1455-"+nodeAddr+"-TestTemp")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef("room")
                                  .setFloorRef("floor")
                                  .addMarker("discharge")
                                  .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("\u00B0F")
                                  .build();
        
        String datID = CCUHsApi.getInstance().addPoint(testPoint);
        
        String deviceRef = new Device.Builder()
                                   .setDisplayName("SN-"+nodeAddr)
                                   .addMarker("network")
                                   .setAddr(nodeAddr)
                                   .build();
        RawPoint analog1Out  = new RawPoint.Builder()
                                       .setDisplayName("Analog1Out-"+nodeAddr)
                                       .setDeviceRef(deviceRef)
                                       .setPort(Port.ANALOG_OUT_ONE.toString())
                                       .setType("2-10v")
                                       .addMarker("output")
                                       .build();
        analog1Out.setPointRef(datID);
        
        
        
    }
}