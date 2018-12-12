package a75f.io.haystack.api;

import org.junit.Test;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
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

import javax.net.ssl.HttpsURLConnection;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;
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
        api.tagsDb.tagsMap = new HashMap<>();
        api.tagsDb.writeArrays = new HashMap<>();
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
                          .setZoneRef("room")
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
                                  .setZoneRef("room")
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
        hayStack.tagsDb.tagsMap = new HashMap<>();
        hayStack.tagsDb.writeArrays = new HashMap<>();
        hayStack.tagsDb.idMap = new HashMap<>();
        hayStack.tagsDb.removeIdMap = new HashMap<>();
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
                          .setZoneRef("room")
                          .setFloorRef("floor")
                          .addMarker("equip")
                          .addMarker("vav")
                          .setGroup(String.valueOf(nodeAddr))
                          .build();
        String equipRef = hayStack.addEquip(v);
    
        Equip v1 = new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName(siteDis+"-VAV-"+(nodeAddr+1))
                          .setZoneRef("room1")
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
                                  .setZoneRef("room")
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
        hayStack.tagsDb.tagsMap = new HashMap<>();
        hayStack.tagsDb.writeArrays = new HashMap<>();
        hayStack.tagsDb.idMap = new HashMap<>();
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
                          .setZoneRef("room")
                          .setFloorRef("floor")
                          .addMarker("equip")
                          .addMarker("vav")
                          .setGroup(String.valueOf(nodeAddr))
                          .build();
        String equipRef = hayStack.addEquip(v);
        
        Equip v1 = new Equip.Builder()
                           .setSiteRef(siteRef)
                           .setDisplayName(siteDis+"-VAV-"+(nodeAddr+1))
                           .setZoneRef("room1")
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
                                  .setZoneRef("room")
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
        hayStack.tagsDb.tagsMap = new HashMap<>();
        hayStack.tagsDb.writeArrays = new HashMap<>();
        hayStack.tagsDb.idMap = new HashMap<>();
        hayStack.tagsDb.removeIdMap = new HashMap<>();
        hayStack.tagsDb.updateIdMap = new HashMap<>();
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
                          .setZoneRef("SYSTEM")
                          .setFloorRef("SYSTEM")
                          .addMarker("equip")
                          .addMarker("vav")
                          .setGroup(String.valueOf(nodeAddr))
                          .build();
        String equipRef = hayStack.addEquip(v);
    
        Equip v1 = new Equip.Builder()
                           .setSiteRef(siteRef)
                           .setDisplayName(siteDis+"-VAV-"+(nodeAddr+1))
                           .setZoneRef("SYSTEM")
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
                                  .setZoneRef("SYSTEM")
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
                                  .setZoneRef("SYSTEM")
                                  .setFloorRef("SYSTEM")
                                  .addMarker("discharge")
                                  .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                  .setTz("Chicago")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setUnit("\u00B0F")
                                  .build();
        
        String tpID = CCUHsApi.getInstance().addPoint(testPoint);
        String tpID1 = CCUHsApi.getInstance().addPoint(testPoint1);
        entitySyncHandler.sync();
        
        hayStack.writePoint(tpID, 8,"samjith", 75.0, 120);
        hayStack.writePoint(tpID1, 8,"samjith", 76.0, 120);
        
        
    }
    
    @Test
    public void testHisSync(){
        
        CCUHsApi hayStack = new CCUHsApi();
        hayStack.tagsDb.init();
        hayStack.tagsDb.tagsMap = new HashMap<>();
        hayStack.tagsDb.writeArrays = new HashMap<>();
        hayStack.tagsDb.idMap = new HashMap<>();
        hayStack.tagsDb.removeIdMap = new HashMap<>();
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
                          .setZoneRef("room")
                          .setFloorRef("floor")
                          .addMarker("equip")
                          .addMarker("vav")
                          .setGroup(String.valueOf(nodeAddr))
                          .build();
        String equipRef = hayStack.addEquip(v);
    
        Equip v1 = new Equip.Builder()
                           .setSiteRef(siteRef)
                           .setDisplayName(siteDis+"-VAV-"+(nodeAddr+1))
                           .setZoneRef("room1")
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
                                  .setZoneRef("room")
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
                                   .setZoneRef("room")
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
    
        CCUHsApi hayStack = new CCUHsApi();
        hayStack.tagsDb.init();
        hayStack.tagsDb.tagsMap = new HashMap<>();
        hayStack.tagsDb.writeArrays = new HashMap<>();
        hayStack.tagsDb.idMap = new HashMap<>();
    
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
        
        System.out.println(b.getRef("siteRef"));
    }

    @Test
    public void testGettingSite()
    {

        HClient hClient = new HClient(HttpUtil.HAYSTACK_URL, "ryan", "ryan");
        HDict navIdDict = new HDictBuilder().add("navId", HRef.make("5be9af1c02743900e9e762f8")).toDict();
        HGrid hGrid = HGridBuilder.dictToGrid(navIdDict);

        HGrid sync = hClient.call("sync", hGrid);

        sync.dump();

    }
}
