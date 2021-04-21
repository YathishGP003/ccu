package a75f.io.haystack.api;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;

public class TestCcuHsApi {
    
    @Test @Ignore("Not a unit test; only for visualizing data with printlns")
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
        
        for (int i = 0; i < 5000 ; i++) {
        Point testPoint = new Point.Builder()
                              .setDisplayName(siteDis+"AHU-"+nodeAddr+"-TestTemp")
                              .setEquipRef(equipRef)
                              .setSiteRef(siteRef)
                              .setRoomRef(zoneRef)
                              .setFloorRef(floorRef)
                              .addMarker("discharge").addMarker("logical")
                              .addMarker("air").addMarker("temp"+i).addMarker("sensor").addMarker("writable")
                              .setGroup(String.valueOf(nodeAddr))
                              .setTz("Chicago")
                              .setUnit("\u00B0F")
                              .build();
        
        String testPoint1Id = hayStack.addPoint(testPoint);
        
        }
        
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
        
        long time1 = System.currentTimeMillis();
        ArrayList<HashMap> p = hayStack.readAll("point and temp4000 and logical");
        long time2 = System.currentTimeMillis();
        HashMap p1 = hayStack.read("point and temp4000 and logical");
    
        long time3 = System.currentTimeMillis();
        
        System.out.println(time2-time1);
        System.out.println(time3-time2);
    
    }
    
}
