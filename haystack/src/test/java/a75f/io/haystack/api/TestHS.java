package a75f.io.haystack.api;

import org.junit.Test;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HGrid;
import org.projecthaystack.HHisItem;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.client.HClient;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;

/**
 * Created by samjithsadasivan on 9/17/18.
 */

public class TestHS
{
    
    
    @Test
    public void testSerialization(){
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
    
        Site s1 = new Site.Builder()
                         .setDisplayName("aaaa")
                         .addMarker("bbbb")
                         .setGeoCity("cccc")
                         .setGeoState("dddd")
                         .setTz("eeee")
                         .setArea(1000).build();
        CCUHsApi.getInstance().addSite(s1);
        
        Map m1 = api.tagsDb.tagsMap;
        System.out.println(m1);
        api.tagsDb.saveString();
        System.out.println(api.tagsDb.tagsString);
        api.tagsDb.init();
        System.out.println(api.tagsDb.tagsMap);
        /*Map m2 = api.tagsDb.tagsMap;
        Iterator i1 = m1.values().iterator();
        Iterator i2 = m2.keySet().iterator();
        
    
        while (i1.hasNext() )
        {
            System.out.println(i1.next());
        }
        
        System.out.println("########################");
    
        HashMap newTagsMap = new HashMap();
        while (i2.hasNext() )
        {
            Object mapKey = i2.next();
            Map h = (Map) m2.get(mapKey);
            System.out.println(h);
    
            Map newMap = new HashMap();
            
            for (Iterator i = h.keySet().iterator(); i.hasNext();) {
                String key = (String)i.next();
                if (key.equals("map"))
                {
                    Map dmap = (Map) h.get(key);
                    for (Iterator g = dmap.keySet().iterator();g.hasNext();) {
                        String k = (String) g.next();
                        newMap.put(k,((Map)dmap.get(k)).get("val"));
                    }
                }
            }
    
            //System.out.println(newMap);
            
            newTagsMap.put(mapKey,newMap);
            
            //System.out.println(b.toDict());
        }
        System.out.println(newTagsMap);*/
    
        HashMap site = CCUHsApi.getInstance().read("site");
        System.out.print(site);
        
    }
    
    @Test
    public void testHisReadWrite(){
        CCUHsApi hayStack = new CCUHsApi();
        hayStack.tagsDb.init();
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
                                  .setDisplayName(siteDis+"AHU-"+nodeAddr+"-TestTemp")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef("room")
                                  .setFloorRef("floor")
                                  .addMarker("discharge")
                                  .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setTz("Chicago")
                                  .setUnit("\u00B0F")
                                  .build();
    
        String datID = CCUHsApi.getInstance().addPoint(testPoint);
        HClient hsClient = hayStack.getHSClient();
        ArrayList<HHisItem> hislist = new ArrayList<>();
        HHisItem[] hisArray = new HHisItem[3];
        long now = System.currentTimeMillis();
        hisArray[0] = HHisItem.make(HDateTime.make(now), HNum.make(75.0));
        hisArray[1] = HHisItem.make(HDateTime.make(now+300000), HNum.make(74.0));
        hisArray[2] = HHisItem.make(HDateTime.make(now-300000), HNum.make(73.0));
    
        hislist.add(hisArray[0]);
        hislist.add(hisArray[2]);
        
        hsClient.hisWrite(HRef.copy(datID),hislist.toArray(new HHisItem[hislist.size()]));
    
        HGrid res = hsClient.hisRead(HRef.copy(datID),"today");
        
        System.out.println("$$$$$$$$$ "+HZincWriter.gridToString(res));
        
    }
    
    @Test
    public void testHisRWApi() {
        CCUHsApi hayStack = new CCUHsApi();
        hayStack.tagsDb.init();
        hayStack.tagsDb.tagsMap = new HashMap<>();
        hayStack.tagsDb.writeArrays = new HashMap<>();
        hayStack.tagsDb.idMap = new HashMap<>();
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
                                  .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setTz("Chicago")
                                  .setUnit("\u00B0F")
                                  .build();
    
        String datID = CCUHsApi.getInstance().addPoint(testPoint);
        
        ArrayList<HisItem> hislist = new ArrayList<>();
        
        
        Date now = new Date();
        
        hislist.add(new HisItem(datID, now, 75.0));
        hislist.add(new HisItem(datID, new Date(now.getTime() + 300000), 73.0));
    
        hayStack.hisWrite(hislist);
    
        ArrayList<HisItem> res = hayStack.hisRead(datID,"today");
    
        for (HisItem h : res) {
            h.dump();
        }
        
        hayStack.hisWrite(new HisItem(datID, new Date(now.getTime() - 600000), 70.0));
    
        HisItem i = hayStack.hisRead(datID);
        System.out.println(i.getDate()+"    :::: "+i.getVal());
        
        i.dump();
    }
    
}
