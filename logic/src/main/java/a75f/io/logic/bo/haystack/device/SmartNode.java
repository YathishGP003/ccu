package a75f.io.logic.bo.haystack.device;

import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
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
    public RawPoint humidity;
    public RawPoint co2;
    public RawPoint voc;
    public RawPoint desiredTemp;
    
    public String deviceRef;
    public String siteRef;
    public String floorRef;
    public String zoneRef;
    
    
    public SmartNode(int address, String site, String floor, String zone, String equipRef) {
        Device d = new Device.Builder()
                .setDisplayName("SN-"+address)
                .addMarker("network").addMarker("node").addMarker("smartnode").addMarker("equipHis")
                .setAddr(address)
                .setSiteRef(site)
                .setFloorRef(floor)
                .setEquipRef(equipRef)
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
                                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                                .setZoneRef(zoneRef)
                                .setFloorRef(floorRef)
                                .addMarker("sensor").addMarker("his")
                                .setTz(tz)
                                .build();
    
        
        analog2In = new RawPoint.Builder()
                            .setDisplayName("Analog2In-"+smartNodeAddress)
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setPort(Port.ANALOG_IN_TWO.toString())
                            .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                            .setZoneRef(zoneRef)
                            .setFloorRef(floorRef)
                            .addMarker("sensor").addMarker("his")
                            .setTz(tz)
                            .build();
    
        th1In = new RawPoint.Builder()
                            .setDisplayName("Th1In-"+smartNodeAddress)
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setPort(Port.TH1_IN.toString())
                            .setZoneRef(zoneRef)
                            .setFloorRef(floorRef)
                            .addMarker("sensor").addMarker("his")
                            .setTz(tz)
                            .build();
    
        th2In = new RawPoint.Builder()
                        .setDisplayName("Th2In-"+smartNodeAddress)
                        .setDeviceRef(deviceRef)
                        .setSiteRef(siteRef)
                        .setPort(Port.TH2_IN.toString())
                        .setZoneRef(zoneRef)
                        .setFloorRef(floorRef)
                        .addMarker("sensor").addMarker("his")
                        .setTz(tz)
                        .build();
    
        analog1Out = new RawPoint.Builder()
                            .setDisplayName("Analog1Out-"+smartNodeAddress)
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setPort(Port.ANALOG_OUT_ONE.toString())
                            .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                            .setZoneRef(zoneRef)
                            .setFloorRef(floorRef)
                            .addMarker("cmd").addMarker("his")
                            .setTz(tz)
                            .build();
    
        analog2Out = new RawPoint.Builder()
                             .setDisplayName("Analog2Out-"+smartNodeAddress)
                             .setDeviceRef(deviceRef)
                             .setSiteRef(siteRef)
                             .setPort(Port.ANALOG_OUT_TWO.toString())
                             .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                             .setZoneRef(zoneRef)
                             .setFloorRef(floorRef)
                             .addMarker("cmd").addMarker("his")
                             .setTz(tz)
                             .build();
    
        relay1 = new RawPoint.Builder()
                             .setDisplayName("relay1-"+smartNodeAddress)
                             .setDeviceRef(deviceRef)
                             .setSiteRef(siteRef)
                             .setZoneRef(zoneRef)
                             .setFloorRef(floorRef)
                             .setPort(Port.RELAY_ONE.toString())
                             .setType(OutputRelayActuatorType.NormallyOpen.displayName)
                             .addMarker("cmd").addMarker("his")
                             .setTz(tz)
                             .build();
    
        relay2 = new RawPoint.Builder()
                         .setDisplayName("relay2-"+smartNodeAddress)
                         .setDeviceRef(deviceRef)
                         .setSiteRef(siteRef)
                         .setZoneRef(zoneRef)
                         .setFloorRef(floorRef)
                         .setPort(Port.RELAY_TWO.toString())
                         .setType(OutputRelayActuatorType.NormallyOpen.displayName)
                         .addMarker("cmd").addMarker("his")
                         .setTz(tz)
                         .build();
    
        currentTemp = new RawPoint.Builder()
                         .setDisplayName("currentTemp-"+smartNodeAddress)
                         .setDeviceRef(deviceRef)
                         .setSiteRef(siteRef)
                         .setZoneRef(zoneRef)
                         .setFloorRef(floorRef)
                         .addMarker("sensor").addMarker("his")
                         .setPort(Port.SENSOR_RT.toString())
                         .setTz(tz)
                         .build();
    
        desiredTemp = new RawPoint.Builder()
                              .setDisplayName("desiredTemp-"+smartNodeAddress)
                              .setDeviceRef(deviceRef)
                              .setSiteRef(siteRef)
                              .setZoneRef(zoneRef)
                              .setFloorRef(floorRef)
                              .addMarker("sensor").addMarker("his")
                              .setPort(Port.DESIRED_TEMP.toString())
                              .setTz(tz)
                              .build();
    
        humidity = new RawPoint.Builder()
                              .setDisplayName("humidity-"+smartNodeAddress)
                              .setDeviceRef(deviceRef)
                              .setSiteRef(siteRef)
                              .setZoneRef(zoneRef)
                              .setFloorRef(floorRef)
                              .addMarker("sensor").addMarker("his")
                              .setPort(Port.SENSOR_RH.toString())
                              .setTz(tz)
                              .build();
    
        co2 = new RawPoint.Builder()
                           .setDisplayName("co2-"+smartNodeAddress)
                           .setDeviceRef(deviceRef)
                           .setSiteRef(siteRef)
                           .setZoneRef(zoneRef)
                           .setFloorRef(floorRef)
                           .addMarker("sensor").addMarker("his")
                           .setPort(Port.SENSOR_CO2.toString())
                           .setTz(tz)
                           .build();
    
        voc = new RawPoint.Builder()
                           .setDisplayName("voc-"+smartNodeAddress)
                           .setDeviceRef(deviceRef)
                           .setSiteRef(siteRef)
                           .setZoneRef(zoneRef)
                           .setFloorRef(floorRef)
                           .addMarker("sensor").addMarker("his")
                           .setPort(Port.SENSOR_VOC.toString())
                           .setTz(tz)
                           .build();
    }
    
    public void addPointsToDb() {
        CCUHsApi.getInstance().addPoint(analog1In);
        CCUHsApi.getInstance().addPoint(analog2In);
        CCUHsApi.getInstance().addPoint(th1In);
        CCUHsApi.getInstance().addPoint(th2In);
        CCUHsApi.getInstance().addPoint(analog1Out);
        CCUHsApi.getInstance().addPoint(analog2Out);
        CCUHsApi.getInstance().addPoint(relay1);
        CCUHsApi.getInstance().addPoint(relay2);
        CCUHsApi.getInstance().addPoint(currentTemp);
        CCUHsApi.getInstance().addPoint(humidity);
        CCUHsApi.getInstance().addPoint(co2);
        CCUHsApi.getInstance().addPoint(voc);
        CCUHsApi.getInstance().addPoint(desiredTemp);
    }
    
    public static void updatePhysicalPoint(int addr, String port, String type) {
        Log.d("CCU"," Update Physical point "+port+" "+type);
    
        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+addr+"\"");
        if (device == null)
        {
            return ;
        }
        
        HashMap point = CCUHsApi.getInstance().read("point and physical and deviceRef == \"" + device.get("id").toString() + "\""+" and port == \""+port+"\"");
        if (!point.get("type").equals(type))
        {
            RawPoint p = new RawPoint.Builder().setHashMap(point).build();
            p.setType(type);
            CCUHsApi.getInstance().updatePoint(p,p.getId());
        }
    }
    
    public static void setPointEnabled(int addr, String port, boolean enabled) {
        Log.d("CCU"," Enabled Physical point "+port+" "+enabled);
        
        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+addr+"\"");
        if (device == null)
        {
            return ;
        }
        
        HashMap point = CCUHsApi.getInstance().read("point and physical and deviceRef == \"" + device.get("id").toString() + "\""+" and port == \""+port+"\"");
        if (point != null && point.size() > 0)
        {
            RawPoint p = new RawPoint.Builder().setHashMap(point).build();
            p.setEnabled(enabled);
            CCUHsApi.getInstance().updatePoint(p,p.getId());
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
