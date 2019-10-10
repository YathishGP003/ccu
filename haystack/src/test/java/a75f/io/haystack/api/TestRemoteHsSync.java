package a75f.io.haystack.api;

import org.joda.time.DateTime;
import org.junit.Test;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.HStr;
import org.projecthaystack.HVal;
import org.projecthaystack.client.HClient;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;
import org.projecthaystack.server.HStdOps;

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
import a75f.io.api.haystack.HayStackConstants;
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
        
        //hayStack.getHSClient().pointWrite(HRef.copy(tpID), 8,"samjith", HNum.make(72.0), HNum.make(2000, "ms"));
        //hayStack.getHSClient().pointWrite(HRef.copy(tpID), 1,"samjith", HNum.make(75.0), HNum.make(0));
        //hayStack.getHSClient().pointWrite(HRef.copy(tpID1), 8,"samjith", HNum.make(76.0), HNum.make(120000, "ms"));
    
        hayStack.writeDefaultValById(tpID, " String Write" );
    
        ArrayList values1 = CCUHsApi.getInstance().readPoint(tpID);
        if (values1 != null && values1.size() > 0)
        {
            for (int l = 1; l <= values1.size(); l++)
            {
                HashMap valMap = ((HashMap) values1.get(l - 1));
                System.out.println("TP: "+valMap);
            }
        }
    
        try
        {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        values1 = CCUHsApi.getInstance().readPoint(tpID);
        if (values1 != null && values1.size() > 0)
        {
            for (int l = 1; l <= values1.size(); l++)
            {
                HashMap valMap = ((HashMap) values1.get(l - 1));
                System.out.println("TP1: "+valMap);
            }
        }
    
        /*hayStack.getHSClient().pointWrite(HRef.copy(tpID), 9,"samjith", HNum.make(70.0), HNum.make(0));
        
        System.out.println("WWWWWWWWWWWWWWWWWWWWWWWWWW");
    
        values1 = CCUHsApi.getInstance().readPoint(tpID);
        if (values1 != null && values1.size() > 0)
        {
            for (int l = 1; l <= values1.size(); l++)
            {
                HashMap valMap = ((HashMap) values1.get(l - 1));
                System.out.println("TP: "+valMap);
            }
        }*/
    
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
        HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), "ryan", "ryan");
        HDict navIdDict = new HDictBuilder().add("navId", HRef.make("5cace9bebf7e6c00f5e23c62")).toDict();
        HGrid hGrid = HGridBuilder.dictToGrid(navIdDict);
        HGrid sync = hClient.call("sync", hGrid);
    
        HDict siteId = new HDictBuilder().add("id",HRef.make("5cace9bebf7e6c00f5e23c62")).toDict();
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
            System.out.println(m);
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
            
            System.out.println(" kind "+m.get("kind").toString().equals("string"));
            for(HashMap v : valList)
            {
                CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(m.get("id").toString()),
                        Integer.parseInt(v.get("level").toString()), v.get("who").toString(),
                        m.get("kind").toString().equals("string") ? HStr.make(v.get("val").toString()) : HNum.make(Double.parseDouble(v.get("val").toString())),HNum.make(0));
            }
            
        }
    }
    
    @Test
    public void testPointWrite() {
        String id = "@5ca7c308bf7e6c00f5e22079";
        HDictBuilder b = new HDictBuilder().add("id", HRef.copy(id)).add("level", 1).add("who", "ccu").add("val", "Happy birthday");
        HDict[] dictArr  = {b.toDict()};
        String  response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "pointWrite", HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
        System.out.println(response);
    }
    
    @Test
    public void testPointRead() {
        String id = "@5cabbfabbf7e6c00f5e22fe9";
        HDictBuilder b = new HDictBuilder().add("id", HRef.copy(id));
        HDict[] dictArr  = {b.toDict()};
        System.out.println(HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
        String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "pointWrite", HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
        System.out.println(response);
    
        HGrid pointGrid = new HZincReader(response).readGrid();
    
        Iterator it = pointGrid.iterator();
        while (it.hasNext())
        {
            HRow r = (HRow) it.next();
            HVal level = r.get("level");
            HVal val = r.get("val");
            HVal who = r.get("who");
            HVal duration = r.get("dur");
            
            System.out.println(" level "+level+" val "+val+" who "+who+" duration "+duration);
            
        }
    }
    
    @Test
    public void testGettingSchedules() {
        
            CCUHsApi api = new CCUHsApi();
            HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), "ryan", "ryan");
            HDict navIdDict = new HDictBuilder().add("navId", HRef.make("5cace9bebf7e6c00f5e23c62")).toDict();
            HGrid hGrid = HGridBuilder.dictToGrid(navIdDict);
            HGrid sync = hClient.call("sync", hGrid);
        
            HDict siteId = new HDictBuilder().add("id",HRef.make("5cace9bebf7e6c00f5e23c62")).toDict();
            HGrid site = hClient.call("read",HGridBuilder.dictToGrid(siteId));
            site.dump();
            sync.dump();
        
            EntityParser s = new EntityParser(site);
    
       
            Site ss = s.getSite();
            api.tagsDb.idMap.put("@"+api.tagsDb.addSite(ss), ss.getId());
            
            System.out.println("SITE ----->");
            System.out.println(s.getSite().getDisplayName());
        
            EntityParser p = new EntityParser(sync);
            
            
            p.importSchedules();
            p.importBuildingTuner();
    
            ArrayList<HashMap> writablePoints = CCUHsApi.getInstance().readAll("point and writable");
            for (HashMap m : writablePoints) {
                System.out.println(m);
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
            
                System.out.println(" kind "+m.get("kind").toString().equals("string"));
                for(HashMap v : valList)
                {
                    CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(m.get("id").toString()),
                            Integer.parseInt(v.get("level").toString()), v.get("who").toString(),
                            m.get("kind").toString().equals("string") ? HStr.make(v.get("val").toString()) : HNum.make(Double.parseDouble(v.get("val").toString())),HNum.make(0));
                }
            
            }
        
    }
    
    @Test
    public void testCode1() {
    
        //2019-04-25T23:59:59+05:30 Kolkata,2019-04-26T05:29:59.000+05:30
        System.out.println("2019-04-25T23:59:59-05:00 Chicago");
        HDateTime hdTime = HDateTime.make("2019-04-25T23:59:59-05:00 Chicago");
        System.out.println(hdTime.time);
        DateTime t = new DateTime(hdTime.millis());
        System.out.println(t);
    
        /*DateTime t1 = new DateTime(hdTime.date.year, hdTime.date.month, hdTime.date.day,
                                hdTime.time.hour, hdTime.time.min, hdTime.time.sec, DateTimeZone.forID(hdTime.tz.name));*/
        DateTime t1 = new DateTime(hdTime.millisDefaultTZ());
        System.out.println(t1);
    }
    
    @Test
    public void testWeatherDataFetch() {
        CCUHsApi api = new CCUHsApi();
        HClient hClient   = new HClient("https://renatusv2-qa.azurewebsites.net/", HayStackConstants.USER, HayStackConstants.PASS);
        HDict   navIdDict = new HDictBuilder().add(HayStackConstants.ID, HRef.make("5d7813b43edf896868c8b139")).toDict();
        HGrid   hGrid     = HGridBuilder.dictToGrid(navIdDict);
    
        HGrid site = hClient.call(HStdOps.read.name(), hGrid);
        //site.dump();
        
        HRef tempWeatherRef = null;
        HDict tDict = new HDictBuilder().add("filter", "weatherPoint and air and temp and weatherRef == " + site.row(0).get("weatherRef")).toDict();
        HGrid weatherPoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
        weatherPoint.dump();
        if (weatherPoint != null && weatherPoint.numRows() > 0)
        {
            tempWeatherRef = HRef.copy(weatherPoint.row(0).get("id").toString());
        }
        HGrid hisGrid = hClient.hisRead(tempWeatherRef, "current");
        hisGrid.dump();
        if (hisGrid != null && hisGrid.numRows() > 0)
        {
            HRow r = hisGrid.row(hisGrid.numRows() - 1);
            HDateTime date = (HDateTime) r.get("ts");
            double tempVal = Double.parseDouble(r.get("val").toString());
            System.out.println(date + " External Temp: " + tempVal);
        }
    
        HDict hDict = new HDictBuilder().add("filter", "weatherPoint and humidity and weatherRef == " + site.row(0).get("weatherRef")).toDict();
        HGrid humidityPoint = hClient.call("read", HGridBuilder.dictToGrid(hDict));
        humidityPoint.dump();
        if (humidityPoint != null && humidityPoint.numRows() > 0)
        {
            hisGrid = hClient.hisRead(HRef.copy(humidityPoint.row(0).get("id").toString()), "current");
            hisGrid.dump();
            if (hisGrid != null && hisGrid.numRows() > 0)
            {
                HRow r = hisGrid.row(hisGrid.numRows() - 1);
                HDateTime date = (HDateTime) r.get("ts");
                double humidityVal = Double.parseDouble(r.get("val").toString());
            
                System.out.println(date+" External Humidity: "+humidityVal);
            
            }
        }
    }
    
    @Test
    public void testGetFloors() {
        CCUHsApi api = new CCUHsApi();
        HClient hClient   = new HClient("https://renatusv2.azurewebsites.net/", HayStackConstants.USER, HayStackConstants.PASS);
        HDict tDict = new HDictBuilder().add("filter", "floor and siteRef == @5d7b622a0078582c8705b607").toDict();
        HGrid floors = hClient.call("read", HGridBuilder.dictToGrid(tDict));
        floors.dump();
    }
    
    @Test
    public void testAddEnum() {
        CCUHsApi hayStack = new CCUHsApi();
        int nodeAddr = 7000;
    
        Site s = new Site.Builder()
                         .setDisplayName("Test")
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
        Point testPoint = new Point.Builder()
                                  .setDisplayName(siteDis+"VAV-"+nodeAddr+"-TestTemp")
                                  .setEquipRef(hayStack.addEquip(v))
                                  .setSiteRef(siteRef)
                                  .setRoomRef("room")
                                  .setFloorRef("floor")
                                  .addMarker("system").addMarker("mode")
                                  .setTz("Chicago").addMarker("his")
                                  .setEnums("off,auto,heatonly,coolonly")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("\u00B0F")
                                  .build();
        hayStack.addPoint(testPoint);
    
        System.out.println(HZincWriter.gridToString(CCUHsApi.getInstance().readHGrid("point and system and mode")));
    }
    
    
}
