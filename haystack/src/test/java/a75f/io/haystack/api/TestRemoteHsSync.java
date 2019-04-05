package a75f.io.haystack.api;

import org.junit.Test;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.client.HClient;
import org.projecthaystack.io.HZincWriter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.SettingPoint;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.sync.EntityParser;
import a75f.io.api.haystack.sync.EntityPullHandler;
import a75f.io.api.haystack.sync.EntitySyncHandler;
import a75f.io.api.haystack.sync.HttpUtil;

/**
 * Created by samjithsadasivan on 10/11/18.
 */

public class TestRemoteHsSync
{
    @Test
    public void testRemoteHaystack() {
        CCUHsApi api = new CCUHsApi();
        Site s = new Site.Builder()
                         .setDisplayName("Name")
                         .addMarker("site")
                         .setGeoCity("Burnsville")
                         .setGeoState("MN")
                         .setTz("Chicago")
                         .setArea(1000).build();
        CCUHsApi.getInstance().addSite(s);

        String urlString = "https://renatusv2.azurewebsites.net/";
        
        //String response = getResult(urlString+"about");
        //System.out.println(response);
    
        HDict[] site = new HDict[1];
        site[0] =  CCUHsApi.getInstance().readHDict("site");
        System.out.println(site);
        HGrid grid = HGridBuilder.dictsToGrid(site);
        
        System.out.println(HZincWriter.gridToString(grid));
        
        String addCmd = "ver:\"3.0\" \n" + "geoCity,area,point,dis\n" + "\"Chicago\",76,\"m\",\"75F\"\n" + "\"Miami\",52,\"m\",\"75G\"";
        
        String presponse = executePost(urlString+"addEntity", addCmd);
        System.out.println("\n"+presponse);
    }
    
    @Test
    public void testNav() {
        
        CCUHsApi hayStack = new CCUHsApi();
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
                                  .setDisplayName(siteDis+"AHU-"+nodeAddr+"-TestTemp")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef("room")
                                  .setFloorRef("floor")
                                  .addMarker("discharge")
                                  .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                  .setTz("Chicago")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("\u00B0F")
                                  .build();
        
        String datID = CCUHsApi.getInstance().addPoint(testPoint);
        
        ArrayList<HashMap> equips = CCUHsApi.getInstance().nav(siteRef);
        //String res = HZincWriter.gridToString(grid);
        //System.out.println(res);
        
        for (HashMap o : equips) {
            System.out.println("Equip :"+o);
            ArrayList<HashMap> points = CCUHsApi.getInstance().nav(o.get("navId").toString());
            for (Object p: points) {
                System.out.println("Point :"+p);
            }
        }
        //HashMap map = CCUHsApi.getInstance().read("equip and siteRef==\""+siteRef+"\"");
        //System.out.println(map);
        
    }
    
    
    @Test
    public void testTagSync() {
        CCUHsApi hayStack = new CCUHsApi();
        int nodeAddr = 7000;

        EntitySyncHandler entitySyncHandler = new EntitySyncHandler();
        System.out.println("entitySyncHandler.isSyncNeeded " + entitySyncHandler.isSyncNeeded());
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
    
        Equip v1 = new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName(siteDis+"-VAV-"+(nodeAddr+1))
                          .setRoomRef("room1")
                          .setFloorRef("floor")
                          .addMarker("equip")
                          .addMarker("vav")
                          .setGroup(String.valueOf(nodeAddr+1))
                          .build();
        String equipRef1 = hayStack.addEquip(v1);
    
        Point testPoint = new Point.Builder()
                                  .setDisplayName(siteDis+"AHU-"+nodeAddr+"-TestTemp")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef("room")
                                  .setFloorRef("floor")
                                  .addMarker("discharge")
                                  .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                  .setTz("Chicago")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("\u00B0F")
                                  .build();
    
        String datID = CCUHsApi.getInstance().addPoint(testPoint);
        /*HashMap sMap = CCUHsApi.getInstance().read("site");
        String luid = (String) sMap.remove("id");
        site[0] = HSUtil.mapToHDict(sMap);*/
    
        System.out.println("entitySyncHandler.isSyncNeeded " + entitySyncHandler.isSyncNeeded());
        entitySyncHandler.sync();
        System.out.println("entitySyncHandler.isSyncNeeded " + entitySyncHandler.isSyncNeeded());
    
        System.out.println(CCUHsApi.getInstance().tagsDb.idMap);
        
        CCUHsApi.getInstance().tagsDb.saveString();
        CCUHsApi.getInstance().tagsDb.init();
        
        System.out.println(CCUHsApi.getInstance().tagsDb.idMap);
        
        
    }
    
    @Test
    public void testTagRemoteNav() {
        CCUHsApi hayStack = new CCUHsApi();
        int nodeAddr = 7000;
        
        EntitySyncHandler entitySyncHandler = new EntitySyncHandler();
        System.out.println("entitySyncHandler.isSyncNeeded " +
                entitySyncHandler.isSyncNeeded());
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
        
        Equip v1 = new Equip.Builder()
                           .setSiteRef(siteRef)
                           .setDisplayName(siteDis+"-VAV-"+(nodeAddr+1))
                           .setRoomRef("room1")
                           .setFloorRef("floor")
                           .addMarker("equip")
                           .addMarker("vav")
                           .setGroup(String.valueOf(nodeAddr+1))
                           .build();
        String equipRef1 = hayStack.addEquip(v1);
        
        Point testPoint = new Point.Builder()
                                  .setDisplayName(siteDis+"AHU-"+nodeAddr+"-TestTemp")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef("room")
                                  .setFloorRef("floor")
                                  .addMarker("discharge")
                                  .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                  .setTz("Chicago")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("\u00B0F")
                                  .build();
        
        String datID = CCUHsApi.getInstance().addPoint(testPoint);
        /*HashMap sMap = CCUHsApi.getInstance().read("site");
        String luid = (String) sMap.remove("id");
        site[0] = HSUtil.mapToHDict(sMap);*/
        
        System.out.println("entitySyncHandler.isSyncNeeded " + entitySyncHandler.isSyncNeeded());
        entitySyncHandler.sync();
        System.out.println("entitySyncHandler.isSyncNeeded " + entitySyncHandler.isSyncNeeded());
        
        System.out.println(CCUHsApi.getInstance().tagsDb.idMap);
        
        CCUHsApi.getInstance().tagsDb.saveString();
        CCUHsApi.getInstance().tagsDb.init();
        
        System.out.println(CCUHsApi.getInstance().tagsDb.idMap);
        
        
    }
    
    @Test
    public void testWritePoint() {
        CCUHsApi hayStack = new CCUHsApi();
        int nodeAddr = 7000;
    
        EntitySyncHandler entitySyncHandler = new EntitySyncHandler();
        System.out.println("entitySyncHandler.isSyncNeeded " + entitySyncHandler.isSyncNeeded());
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
                          .setRoomRef("SYSTEM")
                          .setFloorRef("SYSTEM")
                          .addMarker("equip")
                          .addMarker("vav")
                          .setGroup(String.valueOf(nodeAddr))
                          .build();
        String equipRef = hayStack.addEquip(v);
    
        Equip v1 = new Equip.Builder()
                           .setSiteRef(siteRef)
                           .setDisplayName(siteDis+"-VAV-"+(nodeAddr+1))
                           .setRoomRef("SYSTEM")
                           .setFloorRef("SYSTEM")
                           .addMarker("equip")
                           .addMarker("vav")
                           .setGroup(String.valueOf(nodeAddr+1))
                           .build();
        String equipRef1 = hayStack.addEquip(v1);
    
        Point testPoint = new Point.Builder()
                                  .setDisplayName(siteDis+"AHU-"+nodeAddr+"-TestTemp")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef("SYSTEM")
                                  .setFloorRef("SYSTEM")
                                  .addMarker("discharge")
                                  .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                  .setTz("Chicago")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("\u00B0F")
                                  .build();
    
        Point testPoint1 = new Point.Builder()
                                  .setDisplayName(siteDis+"AHU-"+nodeAddr+"-TestTemp1")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef("SYSTEM")
                                  .setFloorRef("SYSTEM")
                                  .addMarker("discharge")
                                  .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                  .setTz("Chicago")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("\u00B0F")
                                  .build();
        
        String tpID = CCUHsApi.getInstance().addPoint(testPoint);
        String tpID1 = CCUHsApi.getInstance().addPoint(testPoint1);
        //entitySyncHandler.sync();
        
        hayStack.getHSClient().pointWrite(HRef.copy(tpID), 8,"samjith", HNum.make(72.0), HNum.make(120000, "ms"));
        //hayStack.getHSClient().pointWrite(HRef.copy(tpID), 1,"samjith", HNum.make(75.0), HNum.make(0));
        //hayStack.getHSClient().pointWrite(HRef.copy(tpID1), 8,"samjith", HNum.make(76.0), HNum.make(120000, "ms"));
    
        ArrayList values1 = CCUHsApi.getInstance().readPoint(tpID);
        if (values1 != null && values1.size() > 0)
        {
            for (int l = 1; l <= values1.size(); l++)
            {
                HashMap valMap = ((HashMap) values1.get(l - 1));
                System.out.println("TP: "+valMap);
            }
        }
    
        /*values1 = CCUHsApi.getInstance().readPoint(tpID1);
        if (values1 != null && values1.size() > 0)
        {
            for (int l = 1; l <= values1.size(); l++)
            {
                HashMap valMap = ((HashMap) values1.get(l - 1));
                System.out.println("TP1: "+valMap);
            }
        }*/
    
        hayStack.getHSClient().pointWrite(HRef.copy(tpID), 9,"samjith", HNum.make(70.0), HNum.make(0));
        
        System.out.println("WWWWWWWWWWWWWWWWWWWWWWWWWW");
    
        values1 = CCUHsApi.getInstance().readPoint(tpID);
        if (values1 != null && values1.size() > 0)
        {
            for (int l = 1; l <= values1.size(); l++)
            {
                HashMap valMap = ((HashMap) values1.get(l - 1));
                System.out.println("TP: "+valMap);
            }
        }
    
        /*values1 = CCUHsApi.getInstance().readPoint(tpID1);
        if (values1 != null && values1.size() > 0)
        {
            for (int l = 1; l <= values1.size(); l++)
            {
                HashMap valMap = ((HashMap) values1.get(l - 1));
                System.out.println("TP1: "+valMap);
            }
        }*/
        
    }
    
    @Test
    public void testHisSync(){
        
        CCUHsApi hayStack = new CCUHsApi();
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
    
        Equip v1 = new Equip.Builder()
                           .setSiteRef(siteRef)
                           .setDisplayName(siteDis+"-VAV-"+(nodeAddr+1))
                           .setRoomRef("room1")
                           .setFloorRef("floor")
                           .addMarker("equip")
                           .addMarker("vav")
                           .setGroup(String.valueOf(nodeAddr+1))
                           .build();
        String equipRef1 = hayStack.addEquip(v1);
    
        Point testPoint = new Point.Builder()
                                  .setDisplayName(siteDis+"AHU-"+nodeAddr+"-TestTemp")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef("room")
                                  .setFloorRef("floor")
                                  .addMarker("discharge")
                                  .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                  .setTz("Chicago").addMarker("his")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("\u00B0F")
                                  .build();
    
        Point testPoint1 = new Point.Builder()
                                   .setDisplayName(siteDis+"AHU-"+nodeAddr+"-TestTemp1")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .setRoomRef("room")
                                   .setFloorRef("floor")
                                   .addMarker("discharge")
                                   .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                   .setTz("Chicago").addMarker("his")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setUnit("\u00B0F")
                                   .build();
    
        /*String deviceRef = new Device.Builder()
                            .setDisplayName("SN-"+7000)
                            .addMarker("network")
                            .setAddr(7000)
                            .build();*/
        
        String tpID = CCUHsApi.getInstance().addPoint(testPoint);
        String tpID1 = CCUHsApi.getInstance().addPoint(testPoint1);
        
        hayStack.entitySyncHandler.sync();
    
        ArrayList<HisItem> hislist = new ArrayList<>();
        Date now = new Date();
    
        hislist.add(new HisItem(tpID, now, 75.0));
        hislist.add(new HisItem(tpID, new Date(now.getTime() + 300000), 73.0));
    
        hayStack.hisWrite(hislist);
        
        hayStack.hisSyncHandler.doSync();
        
        ArrayList<HisItem> hisItemsNew = (ArrayList<HisItem>) CCUHsApi.getInstance().tagsDb.getAllHisItems(HRef.copy(tpID));
    
        for (HisItem i : hisItemsNew) {
            i.dump();
        }
        
    }
    
    private String executePost(String targetURL, String urlParameters)
    {
        URL url;
        HttpsURLConnection connection = null;
        try {
            //Create connection
            url = new URL(targetURL);
            connection = (HttpsURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "text/zinc");
            
            System.out.println(targetURL);
            System.out.println(urlParameters);
            connection.setRequestProperty("Content-Length", "" +
                                                            Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");
            
            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            
            //Send request
            DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
            wr.writeBytes (urlParameters);
            wr.flush ();
            wr.close ();
            
            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\n');
            }
            rd.close();
            return response.toString();
            
        } catch (Exception e) {
            
            e.printStackTrace();
            return null;
            
        } finally {
            
            if(connection != null) {
                connection.disconnect();
            }
        }
    }
    
    
    
    private String getResult(String url){
        
        StringBuilder result = new StringBuilder();
        
        HttpURLConnection urlConnection = null;
        
        try {
            URL restUrl = new URL(url);
            urlConnection = (HttpURLConnection) restUrl.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            
        }catch( Exception e) {
            e.printStackTrace();
        }
        finally {
            urlConnection.disconnect();
        }
        return result.toString();
    }
    
    @Test
    public void testCode(){
    
        /*CCUHsApi hayStack = new CCUHsApi();
    
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
        
        HDict b = new HDictBuilder().add("id","1").add("siteRef", HRef.make(siteRef.substring(1))).toDict();
        
        System.out.println(b.getRef("siteRef"));*/
        
        double x = 5 - (5-2) * 76/100.0;
        
        System.out.println(x);
    }

    @Test
    public void testGettingSite()
    {

        CCUHsApi api = new CCUHsApi();
        HClient hClient = new HClient(HttpUtil.HAYSTACK_URL, "ryan", "ryan");
        HDict navIdDict = new HDictBuilder().add("navId", HRef.make("5ca42d50a7b11b00f45d642d")).toDict();
        HGrid hGrid = HGridBuilder.dictToGrid(navIdDict);
        HGrid sync = hClient.call("sync", hGrid);
    
        HDict siteId = new HDictBuilder().add("id",HRef.make("5ca42d50a7b11b00f45d642d")).toDict();
        HGrid site = hClient.call("read",HGridBuilder.dictToGrid(siteId));
        site.dump();
        sync.dump();
        
        EntityParser s = new EntityParser(site);
        System.out.println("SITE ----->");
        System.out.println(s.getSite().getDisplayName());
        
        EntityParser p = new EntityParser(sync);
    
        System.out.println("FLOORS ----->");
        for (Floor q : p.getFloors()) {
            System.out.println(q.getDisplayName());
        }
    
        System.out.println("ZONES ----->");
        for (Zone q : p.getZones()) {
            System.out.println(q.getDisplayName()+" roomRef :"+q.getId());
        }
        
        System.out.println("EQUIPS ----->");
        for (Equip q : p.getEquips()) {
            System.out.println(q.getDisplayName()+" roomRef :"+q.getRoomRef());
        }
        System.out.println("POINTS ---->");
        for (Point p1 : p.getPoints()) {
            System.out.println(p1.getDisplayName());
        }
    
        System.out.println("PHY POINTS ---->");
        for (RawPoint p1 : p.getPhyPoints()) {
            System.out.println(p1.getDisplayName());
        }
    
        System.out.println("SETTINGS POINTS ---->");
        for (SettingPoint p1 : p.getSettingPoints()) {
            System.out.println(p1.getDisplayName());
        }
    
        System.out.println("DEVICES ---->");
        for (Device p1 : p.getDevices()) {
            System.out.println(p1.getDisplayName());
        }
    
        System.out.println("SCHEDULES ---->");
        for (Schedule s1 : p.getSchedules()) {
            System.out.println(s1.getId()+" Start "+s1.getStartDateString());
        }
        
        EntityPullHandler h = new EntityPullHandler();
        h.doPullSite(site);
        
        h.doPullFloorTree(api.read("site").get("id").toString(), sync);
        
        System.out.println(api.tagsDb.tagsMap);
    
    
        System.out.println("FLOORS ----->");
        for (Floor q : HSUtil.getFloors())
        {
            System.out.println(q.getDisplayName());
            System.out.println("ZONES ----->");
            for (Zone z : HSUtil.getZones(q.getId()))
            {
                System.out.println(z.getDisplayName() + " zoneRef :" + z.getId());
                System.out.println("EQUIPS ----->");
                for (Equip e : HSUtil.getEquips(z.getId()))
                {
                    System.out.println(e.getDisplayName() + " equipRef :" + e.getId());
                }
            }
        }
        ArrayList<HashMap> writablePoints = CCUHsApi.getInstance().readAll("point and writable");
        for (HashMap m : writablePoints) {
            HDict pid = new HDictBuilder().add("id",HRef.copy(api.getGUID(m.get("id").toString()))).toDict();
            HGrid wa = hClient.call("pointWrite",HGridBuilder.dictToGrid(pid));
            wa.dump();
    
            ArrayList<HashMap> valList = new ArrayList<>();
            Iterator it = wa.iterator();
            while (it.hasNext()) {
                HashMap<Object, Object> map = new HashMap<>();
                HRow r = (HRow) it.next();
                HRow.RowIterator ri = (HRow.RowIterator) r.iterator();
                while (ri.hasNext()) {
                    HDict.MapEntry e = (HDict.MapEntry) ri.next();
                    map.put(e.getKey(), e.getValue());
                }
                valList.add(map);
            }
            
            for(HashMap v : valList)
            {
                CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(m.get("id").toString()),
                        Integer.parseInt(v.get("level").toString()), v.get("who").toString(),
                        HNum.make(Double.parseDouble(v.get("val").toString())),HNum.make(0));
            }
            
        }
        }
}
