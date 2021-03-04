package a75f.io.haystack.api;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;

public class TestCcuHsApi {
    
    @Test
    public void testSite()
    {
        CCUHsApi hayStack = new CCUHsApi();
        
        int nodeAddr = 7000;
        
        Site s = new Site.Builder()
                     .setDisplayName("75F")
                     .addMarker("site")
                     .setGeoCity("Burnsville")
                     .setGeoState("MN")
                     .setTz("Chicago")
                     .setArea(1000).build();
        hayStack.addSite(s);
        
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        
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
        
        Equip v1 = new Equip.Builder()
                       .setSiteRef(siteRef)
                       .setDisplayName(siteDis+"-VAV-"+nodeAddr)
                       .setRoomRef(zoneRef)
                       .setFloorRef(floorRef)
                       .addMarker("equip")
                       .addMarker("dab")
                       .setGroup("7000")
                       .setProfile("VAV_ANALOG_RTU")
                       .setGroup(String.valueOf(nodeAddr))
                       .build();
        hayStack.addEquip(v1);
        
        Equip v2 = new Equip.Builder()
                       .setSiteRef(siteRef)
                       .setDisplayName(siteDis+"-VAV-"+nodeAddr)
                       .setRoomRef(zoneRef)
                       .setFloorRef(floorRef)
                       .addMarker("equip")
                       .addMarker("ti")
                       .setGroup("7000")
                       .setProfile("VAV_ANALOG_RTU")
                       .setGroup(String.valueOf(nodeAddr))
                       .build();
        hayStack.addEquip(v2);
        
        Point testPoint = new Point.Builder()
                              .setDisplayName(siteDis+"AHU-"+nodeAddr+"-TestTemp")
                              .setEquipRef(equipRef)
                              .setSiteRef(siteRef)
                              .setRoomRef(zoneRef)
                              .setFloorRef(floorRef)
                              .addMarker("discharge").addMarker("logical")
                              .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                              .setGroup(String.valueOf(nodeAddr))
                              .setTz("Chicago")
                              .setUnit("\u00B0F")
                              .build();
        
        String testPoint1Id = hayStack.addPoint(testPoint);
        
        Point testPoint2 = new Point.Builder()
                               .setDisplayName(siteDis+"AHU-"+nodeAddr+"-TestTemp2")
                               .setEquipRef(equipRef)
                               .setSiteRef(siteRef)
                               .setRoomRef(zoneRef)
                               .setFloorRef(floorRef)
                               .addMarker("discharge").addMarker("logical").addMarker("his").addMarker("sp").addMarker("zone")
                               .addMarker("air").addMarker("temp2").addMarker("sensor").addMarker("writable")
                               .addMarker("dab")
                               .setGroup(String.valueOf(nodeAddr))
                               .setTz("Chicago")
                               .setUnit("\u00B0F")
                               .build();
        
        String testPoint2Id = hayStack.addPoint(testPoint2);
        
        Device d = new Device.Builder()
                       .setSiteRef(siteRef)
                       .setDisplayName(siteDis+"-VAV-"+nodeAddr)
                       .setRoomRef(zoneRef)
                       .setFloorRef(floorRef)
                       .setEquipRef(equipRef)
                       .addMarker("device")
                       .addMarker("smartnode")
                       .build();
        String deviceRef = hayStack.addDevice(d);
        
        RawPoint testRawPoint = new RawPoint.Builder()
                                    .setDisplayName(siteDis+"AHU-"+nodeAddr+"-TestTemp")
                                    .setSiteRef(siteRef)
                                    .setRoomRef(zoneRef)
                                    .setDeviceRef(deviceRef)
                                    .setFloorRef(floorRef)
                                    .addMarker("discharge").addMarker("physical")
                                    .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("writable")
                                    .setTz("Chicago")
                                    .setUnit("\u00B0F")
                                    .build();
        
        hayStack.addPoint(testRawPoint);
        
        ArrayList<HashMap> p = hayStack.readAll("point and temp or temp2 and logical");
        System.out.println(p.size());
        for(HashMap m:p) {
            System.out.println(m);
        }

    /*System.out.println(hayStack.read("site"));
    Site s1 = new Site.Builder().setHashMap(hayStack.read("site")).build();
    s1.setSyncStatus(true);
    hayStack.updateSite(s1, s1.getId());
    System.out.println(hayStack.read("site"));

    System.out.println(hayStack.read("floor"));
    Floor f1 = new Floor.Builder().setHashMap(hayStack.read("floor")).build();
    f1.setSyncStatus(true);
    hayStack.updateFloor(f1, f1.getId());
    System.out.println(hayStack.read("floor"));

    System.out.println(hayStack.read("room"));
    Zone z1 = new Zone.Builder().setHashMap(hayStack.read("room")).build();
    z1.setSyncStatus(true);
    hayStack.updateZone(z1, z1.getId());
    System.out.println(hayStack.read("room"));

    System.out.println(hayStack.read("equip"));
    Equip q1 = new Equip.Builder().setHashMap(hayStack.read("equip")).build();
    q1.setSyncStatus(true);
    hayStack.updateEquip(q1, q1.getId());
    System.out.println(hayStack.read("equip"));

    System.out.println(hayStack.read("point and logical"));
    Point p1 = new Point.Builder().setHashMap(hayStack.read("point and logical")).build();
    p1.setSyncStatus(true);
    hayStack.updatePoint(p1, p1.getId());
    System.out.println(hayStack.read("point and logical"));

    System.out.println(hayStack.read("point and physical"));
    RawPoint r1 = new RawPoint.Builder().setHashMap(hayStack.read("point and physical")).build();
    r1.setSyncStatus(true);
    hayStack.updatePoint(r1, r1.getId());
    System.out.println(hayStack.read("point and physical"));*/


    /*System.out.println(hayStack.read("point and physical"));
    RawPoint p1 = new RawPoint.Builder().setHashMap(hayStack.read("point and physical")).build();
    CCUHsApi.getInstance().setEntitySynced(p1.getId());
    System.out.println(hayStack.read("point and physical"));
    System.out.println("##### Not synced");
    
    for(HashMap m : hayStack.readAll("point and not synced")) {
        System.out.println(m);
    }
    System.out.println("##### synced");
    System.out.println(hayStack.read("point and synced"));*/
        
        
        /*HGrid grid = CCUHsApi.getInstance().hsClient.readAll("(equip and dab) or (equip and ti)");
        
        HGridIterator i = new HGridIterator(grid);
        
        while (i.hasNext()) {
            System.out.println(HZincWriter.gridToString(i.next(1)));
        }*/
        
    /*if (i.hasNext()) {
        System.out.println(HZincWriter.gridToString(i.next(3)));
    }
    if (i.hasNext()) {
        System.out.println(HZincWriter.gridToString(i.next(3)));
    }
    if (i.hasNext()) {
        System.out.println(HZincWriter.gridToString(i.next(3)));
    }*/
    
    
    }
    
}
