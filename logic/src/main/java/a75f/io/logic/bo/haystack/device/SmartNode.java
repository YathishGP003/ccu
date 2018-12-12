package a75f.io.logic.bo.haystack.device;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.definitions.Port;

/**
 * Created by samjithsadasivan on 9/5/18.
 */

public class SmartNode
{
    int smartNodeAddress;
    
    public RawPoint analog1In;
    public RawPoint analog2In;
    public RawPoint th1In;
    public RawPoint th2In;
    public RawPoint analog1Out;
    public RawPoint analog2Out;
    public RawPoint relay1;
    public RawPoint relay2;
    public RawPoint currentTemp;
    
    public String deviceRef;
    public String siteRef;
    public String floorRef;
    public String zoneRef;
    
    
    public SmartNode(int address, String site, String floor, String zone) {
        Device d = new Device.Builder()
                .setDisplayName("SN-"+address)
                .addMarker("network")
                .addMarker("node")
                .setAddr(address)
                .setSiteRef(site)
                .setFloorRef(floor)
                .setZoneRef(zone)
                .build();
        deviceRef = CCUHsApi.getInstance().addDevice(d);
        smartNodeAddress = address;
        siteRef = site;
        floorRef = floor;
        zoneRef = zone;
        createPoints();
    }
    
    private void createPoints() {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String tz = siteMap.get("tz").toString();
        
        analog1In = new RawPoint.Builder()
                                .setDisplayName("Analog1In-"+smartNodeAddress)
                                .setDeviceRef(deviceRef)
                                .setSiteRef(siteRef)
                                .setPort(Port.ANALOG_IN_ONE.toString())
                                .setType("0-10v")
                                .setZoneRef(zoneRef)
                                .setFloorRef(floorRef)
                                .addMarker("input").addMarker("his")
                                .setTz(tz)
                                .build();
    
        
        analog2In = new RawPoint.Builder()
                            .setDisplayName("Analog2In-"+smartNodeAddress)
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setPort(Port.ANALOG_IN_TWO.toString())
                            .setType("0-10v")
                            .setZoneRef(zoneRef)
                            .setFloorRef(floorRef)
                            .addMarker("input").addMarker("his")
                            .setTz(tz)
                            .build();
    
        th1In = new RawPoint.Builder()
                            .setDisplayName("Th1In-"+smartNodeAddress)
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setPort(Port.TH1_IN.toString())
                            .setZoneRef(zoneRef)
                            .setFloorRef(floorRef)
                            .addMarker("input").addMarker("his")
                            .setTz(tz)
                            .build();
    
        th2In = new RawPoint.Builder()
                        .setDisplayName("Th2In-"+smartNodeAddress)
                        .setDeviceRef(deviceRef)
                        .setSiteRef(siteRef)
                        .setPort(Port.TH2_IN.toString())
                        .setZoneRef(zoneRef)
                        .setFloorRef(floorRef)
                        .addMarker("input").addMarker("his")
                        .setTz(tz)
                        .build();
    
        analog1Out = new RawPoint.Builder()
                            .setDisplayName("Analog1Out-"+smartNodeAddress)
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setPort(Port.ANALOG_OUT_ONE.toString())
                            .setType("0-10v")
                            .setZoneRef(zoneRef)
                            .setFloorRef(floorRef)
                            .addMarker("output").addMarker("his")
                            .setTz(tz)
                            .build();
    
        analog2Out = new RawPoint.Builder()
                             .setDisplayName("Analog2Out-"+smartNodeAddress)
                             .setDeviceRef(deviceRef)
                             .setSiteRef(siteRef)
                             .setPort(Port.ANALOG_OUT_TWO.toString())
                             .setType("0-10v")
                             .setZoneRef(zoneRef)
                             .setFloorRef(floorRef)
                             .addMarker("output").addMarker("his")
                             .setTz(tz)
                             .build();
    
        relay1 = new RawPoint.Builder()
                             .setDisplayName("relay1-"+smartNodeAddress)
                             .setDeviceRef(deviceRef)
                             .setSiteRef(siteRef)
                             .setZoneRef(zoneRef)
                             .setFloorRef(floorRef)
                             .setPort(Port.RELAY_ONE.toString())
                             .setType("NO")
                             .addMarker("output").addMarker("his")
                             .setTz(tz)
                             .build();
    
        relay2 = new RawPoint.Builder()
                         .setDisplayName("relay2-"+smartNodeAddress)
                         .setDeviceRef(deviceRef)
                         .setSiteRef(siteRef)
                         .setZoneRef(zoneRef)
                         .setFloorRef(floorRef)
                         .setPort(Port.RELAY_TWO.toString())
                         .setType("NO")
                         .addMarker("output").addMarker("his")
                         .setTz(tz)
                         .build();
    
        currentTemp = new RawPoint.Builder()
                         .setDisplayName("currentTemp-"+smartNodeAddress)
                         .setDeviceRef(deviceRef)
                         .setSiteRef(siteRef)
                         .setZoneRef(zoneRef)
                         .setFloorRef(floorRef)
                         .addMarker("input").addMarker("his")
                         .setPort(Port.RTH.toString())
                         .setTz(tz)
                         .build();
    }
    
    public static void updatePhysicalPoint(int addr, String port, String type) {
    
        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+addr+"\"");
        if (device == null)
        {
            return ;
        }
        
        HashMap point = CCUHsApi.getInstance().read("point and physical and deviceRef == \"" + device.get("id").toString() + "\""+" and port == \""+port+"\"");
        if (!point.get("type").equals(type))
        {
            HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
            RawPoint analog1Out = new RawPoint.Builder()
                                          .setDisplayName(point.get("dis").toString())
                                          .setDeviceRef(point.get("deviceRef").toString())
                                          .setSiteRef(point.get("siteRef").toString())
                                          .setPort(port)
                                          .setType(type)
                                          .addMarker("output").addMarker("his")
                                          .setTz(siteMap.get("tz").toString())
                                          .build();
            CCUHsApi.getInstance().updatePoint(analog1Out,point.get("id").toString());
        }
    }
    
    public static RawPoint getPhysicalPoint(int addr, String port) {
        
        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+addr+"\"");
        if (device == null)
        {
            return null;
        }
        
        HashMap point = CCUHsApi.getInstance().read("point and physical and deviceRef == \"" + device.get("id").toString() + "\""+" and port == \""+port+"\"");
        if (point != null && point.size() > 0)
        {
            return new RawPoint.Builder().setHashMap(point).build();
        }
        return null;
    }

}
