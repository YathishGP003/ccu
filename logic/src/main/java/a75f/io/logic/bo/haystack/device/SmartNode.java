package a75f.io.logic.bo.haystack.device;

import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
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
    public RawPoint desiredTemp;
    
    public String deviceRef;
    public String siteRef;
    public String floorRef;
    public String roomRef;
    
    String tz;
    
    public SmartNode(int address, String site, String floor, String room, String equipRef) {
        Device d = new Device.Builder()
                .setDisplayName("SN-"+address)
                .addMarker("network").addMarker("node").addMarker("smartnode").addMarker("equipHis")
                .setAddr(address)
                .setSiteRef(site)
                .setFloorRef(floor)
                .setEquipRef(equipRef)
                .setRoomRef(room)
                .build();
        deviceRef = CCUHsApi.getInstance().addDevice(d);
        smartNodeAddress = address;
        siteRef = site;
        floorRef = floor;
        roomRef = room;
    
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        tz = siteMap.get("tz").toString();
    
        createPoints();
    }
    
    public SmartNode(int address) {
        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+address+"\"");
        Device d = new Device.Builder().setHashMap(device).build();
        
        deviceRef = d.getId();
        smartNodeAddress = Integer.parseInt(d.getAddr());
        siteRef = d.getSiteRef();
        floorRef = d.getFloorRef();
        roomRef = d.getRoomRef();
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        tz = siteMap.get("tz").toString();
    }
    
    private void createPoints() {
        analog1In = new RawPoint.Builder()
                                .setDisplayName("Analog1In-"+smartNodeAddress)
                                .setDeviceRef(deviceRef)
                                .setSiteRef(siteRef)
                                .setPort(Port.ANALOG_IN_ONE.toString())
                                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                                .setRoomRef(roomRef)
                                .setFloorRef(floorRef)
                                .addMarker("sensor").addMarker("his").addMarker("equipHis")
                                .setUnit("mV")
                                .setTz(tz)
                                .build();
    
        
        analog2In = new RawPoint.Builder()
                            .setDisplayName("Analog2In-"+smartNodeAddress)
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setPort(Port.ANALOG_IN_TWO.toString())
                            .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                            .setRoomRef(roomRef)
                            .setFloorRef(floorRef)
                            .addMarker("sensor").addMarker("his").addMarker("equipHis")
                            .setUnit("mV")
                            .setTz(tz)
                            .build();
    
        th1In = new RawPoint.Builder()
                            .setDisplayName("Th1In-"+smartNodeAddress)
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setType("0")
                            .setPort(Port.TH1_IN.toString())
                            .setRoomRef(roomRef)
                            .setFloorRef(floorRef)
                            .addMarker("sensor").addMarker("his").addMarker("equipHis")
                            .setUnit("Ohm")
                            .setTz(tz)
                            .build();
    
        th2In = new RawPoint.Builder()
                        .setDisplayName("Th2In-"+smartNodeAddress)
                        .setDeviceRef(deviceRef)
                        .setSiteRef(siteRef)
                        .setType("0")
                        .setPort(Port.TH2_IN.toString())
                        .setRoomRef(roomRef)
                        .setFloorRef(floorRef)
                        .addMarker("sensor").addMarker("his").addMarker("equipHis")
                        .setUnit("Ohm")
                        .setTz(tz)
                        .build();
    
        analog1Out = new RawPoint.Builder()
                            .setDisplayName("Analog1Out-"+smartNodeAddress)
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setPort(Port.ANALOG_OUT_ONE.toString())
                            .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                            .setRoomRef(roomRef)
                            .setFloorRef(floorRef)
                            .addMarker("cmd").addMarker("his").addMarker("equipHis")
                            .setUnit("dV")
                            .setTz(tz)
                            .build();
    
        analog2Out = new RawPoint.Builder()
                             .setDisplayName("Analog2Out-"+smartNodeAddress)
                             .setDeviceRef(deviceRef)
                             .setSiteRef(siteRef)
                             .setPort(Port.ANALOG_OUT_TWO.toString())
                             .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                             .setRoomRef(roomRef)
                             .setFloorRef(floorRef)
                             .addMarker("cmd").addMarker("his").addMarker("equipHis")
                             .setUnit("dV")
                             .setTz(tz)
                             .build();
    
        relay1 = new RawPoint.Builder()
                             .setDisplayName("relay1-"+smartNodeAddress)
                             .setDeviceRef(deviceRef)
                             .setSiteRef(siteRef)
                             .setRoomRef(roomRef)
                             .setFloorRef(floorRef)
                             .setPort(Port.RELAY_ONE.toString())
                             .setType(OutputRelayActuatorType.NormallyOpen.displayName)
                             .addMarker("cmd").addMarker("his").addMarker("equipHis")
                             .setTz(tz)
                             .build();
    
        relay2 = new RawPoint.Builder()
                         .setDisplayName("relay2-"+smartNodeAddress)
                         .setDeviceRef(deviceRef)
                         .setSiteRef(siteRef)
                         .setRoomRef(roomRef)
                         .setFloorRef(floorRef)
                         .setPort(Port.RELAY_TWO.toString())
                         .setType(OutputRelayActuatorType.NormallyOpen.displayName)
                         .addMarker("cmd").addMarker("his").addMarker("equipHis")
                         .setTz(tz)
                         .build();
    
        currentTemp = new RawPoint.Builder()
                         .setDisplayName("currentTemp-"+smartNodeAddress)
                         .setDeviceRef(deviceRef)
                         .setSiteRef(siteRef)
                         .setRoomRef(roomRef)
                         .setFloorRef(floorRef)
                         .addMarker("sensor").addMarker("his").addMarker("equipHis")
                         .setPort(Port.SENSOR_RT.toString())
                         .setUnit("\u00B0F")
                         .setTz(tz)
                         .build();
    
        desiredTemp = new RawPoint.Builder()
                              .setDisplayName("desiredTemp-"+smartNodeAddress)
                              .setDeviceRef(deviceRef)
                              .setSiteRef(siteRef)
                              .setRoomRef(roomRef)
                              .setFloorRef(floorRef)
                              .addMarker("sensor").addMarker("his").addMarker("equipHis")
                              .setPort(Port.DESIRED_TEMP.toString())
                              .setUnit("\u00B0F")
                              .setTz(tz)
                              .build();
    }
    
    public void addSensor(Port p, String pointRef) {
        RawPoint sensor = new RawPoint.Builder()
                                    .setDisplayName(p.toString()+"-"+smartNodeAddress)
                                    .setDeviceRef(deviceRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(roomRef)
                                    .setFloorRef(floorRef)
                                    .setPointRef(pointRef)
                                    .setEnabled(true)
                                    .addMarker("sensor").addMarker("his").addMarker("equipHis")
                                    .setPort(p.toString())
                                    .setTz(tz)
                                    .build();
        CCUHsApi.getInstance().addPoint(sensor);
    }
    
    public RawPoint addSensor(Port p) {
        Equip q = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \""+smartNodeAddress+"\"")).build();
        String sensorUnit = "";
        switch (p){
            case SENSOR_NO:
            case SENSOR_CO2_EQUIVALENT:
            case SENSOR_CO:
                sensorUnit = "ppm";
                break;
            case SENSOR_ILLUMINANCE:
                sensorUnit = "lux";
                break;
            case SENSOR_PRESSURE:
                sensorUnit = "inch wc";
                break;
            case SENSOR_SOUND:
                sensorUnit = "dB";
                break;
            case SENSOR_VOC:
                sensorUnit = "ppb";
                break;
            default:
                break;
        }
    
        Point equipSensor = new Point.Builder()
                                 .setDisplayName(q.getDisplayName()+"-"+p.getPortSensor())
                                 .setEquipRef(q.getId())
                                 .setSiteRef(siteRef)
                                 .setRoomRef(roomRef)
                                 .setFloorRef(floorRef)
                                 .addMarker("zone").addMarker("sensor").addMarker(p.getPortSensor()).addMarker("his").addMarker("cur").addMarker("current").addMarker("logical").addMarker("equipHis")
                                 .setUnit(sensorUnit)
                                 .setGroup(String.valueOf(smartNodeAddress))
                                 .setTz(tz)
                                 .build();
        String pointRef = CCUHsApi.getInstance().addPoint(equipSensor);
        RawPoint deviceSensor = new RawPoint.Builder()
                                  .setDisplayName(p.toString()+"-"+smartNodeAddress)
                                  .setDeviceRef(deviceRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef)
                                  .setPointRef(pointRef)
                                  .setEnabled(true)
                                  .addMarker("sensor").addMarker("his").addMarker("equipHis")
                                  .setPort(p.toString())
                                  .setTz(tz)
                                  .build();
        deviceSensor.setId(CCUHsApi.getInstance().addPoint(deviceSensor));
        
        CCUHsApi.getInstance().scheduleSync();
        
        return deviceSensor;
    }
    
    public RawPoint getRawPoint(Port p) {
        HashMap sensorPoint = CCUHsApi.getInstance().read("point and sensor and physical and deviceRef == \""+deviceRef+"\""
                                                     +" and port == \""+p.toString()+"\"");
        return sensorPoint.size() > 0 ? new RawPoint.Builder().setHashMap(sensorPoint).build() : null;
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
        CCUHsApi.getInstance().addPoint(desiredTemp);
    }
    
    public static void updatePhysicalPointType(int addr, String port, String type) {
        Log.d("CCU"," Update Physical point "+port+" "+type);
    
        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+addr+"\"");
        if (device == null)
        {
            return ;
        }
        
        HashMap point = CCUHsApi.getInstance().read("point and physical and deviceRef == \"" + device.get("id").toString() + "\""+" and port == \""+port+"\"");
        if (!point.get("analogType" ).equals(type))
        {
            RawPoint p = new RawPoint.Builder().setHashMap(point).build();
            p.setType(type);
            CCUHsApi.getInstance().updatePoint(p,p.getId());
        }
    }
    
    public static void updatePhysicalPointRef(int addr, String port, String pointRef) {
        Log.d("CCU"," Update Physical point "+port);
        
        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+addr+"\"");
        if (device == null)
        {
            return ;
        }
        
        HashMap point = CCUHsApi.getInstance().read("point and physical and deviceRef == \"" + device.get("id").toString() + "\""+" and port == \""+port+"\"");
        RawPoint p = new RawPoint.Builder().setHashMap(point).build();
        p.setPointRef(pointRef);
        CCUHsApi.getInstance().updatePoint(p,p.getId());
       
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
