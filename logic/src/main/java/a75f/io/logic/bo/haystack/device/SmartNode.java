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
import a75f.io.logic.bo.building.heartbeat.HeartBeat;

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
    public RawPoint rssi;
    
    public String deviceRef;
    public String siteRef;
    public String floorRef;
    public String roomRef;
    
    String tz;
    
    public SmartNode(int address, String site, String floor, String room, String equipRef) {
        Device d = new Device.Builder()
                .setDisplayName("SN-"+address)
                .addMarker("network").addMarker("node").addMarker("smartnode").addMarker("his")
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
                                .setShortDis("Analog1-In Physical Sensor")
                                .setDeviceRef(deviceRef)
                                .setSiteRef(siteRef)
                                .setPort(Port.ANALOG_IN_ONE.toString())
                                .setType(String.valueOf(OutputAnalogActuatorType.ZeroToTenV.ordinal()))
                                .setRoomRef(roomRef)
                                .setFloorRef(floorRef)
                                .addMarker("sensor").addMarker("his")
                                .setUnit("mV")
                                .setMinVal("0")
                                .setMaxVal("1000")
                                .setTz(tz)
                                .build();
    
        
        analog2In = new RawPoint.Builder()
                            .setDisplayName("Analog2In-"+smartNodeAddress)
                            .setShortDis("Analog2-In Physical Sensor")
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setPort(Port.ANALOG_IN_TWO.toString())
                            .setType(String.valueOf(OutputAnalogActuatorType.ZeroToTenV.ordinal()))
                            .setRoomRef(roomRef)
                            .setFloorRef(floorRef)
                            .addMarker("sensor").addMarker("his")
                            .setUnit("mV")
                            .setMinVal("0")
                            .setMaxVal("1000")
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
                            .addMarker("sensor").addMarker("his")
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
                        .addMarker("sensor").addMarker("his")
                        .setUnit("Ohm")
                        .setTz(tz)
                        .build();
    
        analog1Out = new RawPoint.Builder()
                            .setDisplayName("Analog1Out-"+smartNodeAddress)
                            .setShortDis("Analog1-Out")
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setPort(Port.ANALOG_OUT_ONE.toString())
                            .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                            .setRoomRef(roomRef)
                            .setFloorRef(floorRef)
                            .addMarker("cmd").addMarker("his")
                            .setUnit("dV")
                            .setMinVal("0")
                            .setMaxVal("100")
                            .setTz(tz)
                            .build();
    
        analog2Out = new RawPoint.Builder()
                             .setDisplayName("Analog2Out-"+smartNodeAddress)
                             .setShortDis("Analog2-Out")
                             .setDeviceRef(deviceRef)
                             .setSiteRef(siteRef)
                             .setPort(Port.ANALOG_OUT_TWO.toString())
                             .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                             .setRoomRef(roomRef)
                             .setFloorRef(floorRef)
                             .addMarker("cmd").addMarker("his")
                             .setUnit("dV")
                             .setMinVal("0")
                             .setMaxVal("100")
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
                             .addMarker("cmd").addMarker("his")
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
                         .addMarker("cmd").addMarker("his")
                         .setTz(tz)
                         .build();
    
        currentTemp = new RawPoint.Builder()
                         .setDisplayName("currentTemp-"+smartNodeAddress)
                         .setDeviceRef(deviceRef)
                         .setSiteRef(siteRef)
                         .setRoomRef(roomRef)
                         .setFloorRef(floorRef)
                         .addMarker("sensor").addMarker("his")
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
                              .addMarker("sensor").addMarker("his")
                              .setPort(Port.DESIRED_TEMP.toString())
                              .setUnit("\u00B0F")
                              .setTz(tz)
                              .build();

        rssi = HeartBeat.getHeartBeatRawPoint(smartNodeAddress, deviceRef, siteRef, roomRef, floorRef, tz);
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
                                    .addMarker("sensor").addMarker("his")
                                    .setPort(p.toString())
                                    .setTz(tz)
                                    .build();
        CCUHsApi.getInstance().addPoint(sensor);
    }
    
    public RawPoint addSensor(Port p) {
        Equip q = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \""+smartNodeAddress+"\"")).build();
        String sensorUnit = "";
        boolean isOccupancySensor = false;
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
            case SENSOR_OCCUPANCY:
                isOccupancySensor = true;
                break;
            default:
                break;
        }


        Point equipSensor;
        String pointRef;
        if(isOccupancySensor) {

            equipSensor = new Point.Builder()
                    .setDisplayName(q.getDisplayName() + "-" + p.getPortSensor()+"Sensor")
                    .setEquipRef(q.getId())
                    .setSiteRef(siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef).setHisInterpolate("cov")
                    .addMarker("zone").addMarker("sensor").addMarker(p.getPortSensor()).addMarker("his").addMarker("cur").addMarker("current").addMarker("logical")
                    .setEnums("off,on")
                    .setGroup(String.valueOf(smartNodeAddress))
                    .setTz(tz)
                    .build();
             pointRef = CCUHsApi.getInstance().addPoint(equipSensor);
        }else {
            equipSensor = new Point.Builder()
                    .setDisplayName(q.getDisplayName() + "-" + p.getPortSensor())
                    .setEquipRef(q.getId())
                    .setSiteRef(siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef).setHisInterpolate("cov")
                    .addMarker("zone").addMarker("sensor").addMarker(p.getPortSensor()).addMarker("his").addMarker("cur").addMarker("current").addMarker("logical")
                    .setUnit(sensorUnit)
                    .setGroup(String.valueOf(smartNodeAddress))
                    .setTz(tz)
                    .build();
            pointRef = CCUHsApi.getInstance().addPoint(equipSensor);
        }
        RawPoint deviceSensor = new RawPoint.Builder()
                                  .setDisplayName(p.toString()+"-"+smartNodeAddress)
                                  .setDeviceRef(deviceRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef)
                                  .setPointRef(pointRef)
                                  .setEnabled(true)
                                  .addMarker("sensor").addMarker("his")
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
        CCUHsApi.getInstance().addPoint(rssi);
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
            CCUHsApi.getInstance().writeHisValById(p.getId(), 0.0);
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
