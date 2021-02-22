package a75f.io.haystack.api;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.projecthaystack.HStr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Zone;

public class ObjectboxPerformanceTest {
    
    private static final int ITEM_COUNT_DB = 10;
    
    @Test @Ignore("an experiment rather than a unit test")
    public void testHisReadPerformance() {
        CCUHsApi hayStack = new CCUHsApi();
    
        Site s = new Site.Builder()
                     .setDisplayName("75F")
                     .addMarker("site")
                     .setGeoCity("Bloomington")
                     .setGeoState("MN")
                     .setTz("Chicago")
                     .setArea(10000).build();
        String siteRef = hayStack.addSite(s);
        
        String siteDis = "75F";
        int nodeAddr = 7000;
        
        Floor f = new Floor.Builder()
                      .setDisplayName("Floor1")
                      .setSiteRef(siteRef)
                      .build();
    
        String floorRef = hayStack.addFloor(f);
    
        Zone z = new Zone.Builder()
                     .setDisplayName("Zone1")
                     .setFloorRef(floorRef)
                     .setSiteRef(siteRef)
                     .build();
        String zoneRef = hayStack.addZone(z);
    
    
        Equip v = new Equip.Builder()
                      .setSiteRef(siteRef)
                      .setDisplayName(siteDis+"-VAV-"+nodeAddr)
                      .setRoomRef(zoneRef)
                      .setFloorRef(floorRef)
                      .addMarker("equip")
                      .addMarker("vav")
                      .setGroup("7000")
                      .setProfile("VAV_ANALOG_RTU")
                      .setGroup(String.valueOf(nodeAddr))
                      .build();
        String equipRef = hayStack.addEquip(v);
        
        
        for (int i = 1; i <= ITEM_COUNT_DB; i++) {
            Point testPoint = new Point.Builder()
                                  .setDisplayName(siteDis+nodeAddr+"TestPoint"+i)
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef(zoneRef)
                                  .setFloorRef(floorRef)
                                  .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("zone")
                                  .addMarker("tag"+i).addMarker("his")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setTz("Chicago")
                                  .setUnit("\u00B0F")
                                  .build();
            hayStack.addPoint(testPoint);
        }
        
        
        for (int j = 0; j < 24*60; j++) {
            doHisWriteToAllPoints(hayStack);
            long startTime = System.currentTimeMillis();
            doHisReadOnAllPoints(hayStack);
            System.out.println("  #################  Iteration time mills - #############  : "+(System.currentTimeMillis()-startTime));
            doUpdateHisItemSynced(hayStack);
        }
        
    }
    
    
    public void doHisWriteToAllPoints(CCUHsApi hayStack) {
        for (int i = 1; i <= ITEM_COUNT_DB; i++) {
            HashMap idMap = hayStack.read("point and tag" + i);
            hayStack.writeHisValById(idMap.get("id").toString(), RandomUtils.nextDouble());
        }
    }
    
    public void doHisReadOnAllPoints(CCUHsApi hayStack) {
        for (int i = 1; i <= ITEM_COUNT_DB; i++) {
            HashMap idMap = hayStack.read("point and tag" + i);
            List<HisItem> unsyncedHisItems = hayStack.tagsDb.getUnsyncedHisItemsOrderDesc(idMap.get("id").toString());
            /*for (HisItem item : unsyncedHisItems) {
                //System.out.println("id " + idMap.get("id") + " val " + item.getVal());
            }*/
        }
    }
    
    public void doUpdateHisItemSynced(CCUHsApi hayStack) {
    
        for (int i = 1; i <= ITEM_COUNT_DB; i++) {
            HashMap idMap = hayStack.read("point and tag" + i);
            List<HisItem> items = hayStack.tagsDb.getUnsyncedHisItemsOrderDesc(idMap.get("id").toString());
            hayStack.tagsDb.updateHisItemSynced(items);
        }
    }
    
    
}
