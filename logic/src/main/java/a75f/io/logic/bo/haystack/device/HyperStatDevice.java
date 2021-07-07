package a75f.io.logic.bo.haystack.device;

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
 * Models a HyperStat device Haystack entity.
 *
 * I
 */
public class HyperStatDevice {
    
    int hyperStatNodeAddress;
    
    public RawPoint analog1In;
    public RawPoint analog2In;
    public RawPoint th1In;
    public RawPoint th2In;
    public RawPoint relay1;
    public RawPoint relay2;
    public RawPoint relay3;
    public RawPoint relay4;
    public RawPoint relay5;
    public RawPoint relay6;
    public RawPoint analog1Out;
    public RawPoint analog2Out;
    public RawPoint analog3Out;
    public RawPoint currentTemp;
    public RawPoint desiredTemp;
    
    public String deviceRef;
    public String siteRef;
    public String floorRef;
    public String roomRef;
    
    String tz;
    
    /**
     * Constructs a new HaystackDevice instance , and adds the device and all its associated entities to the
     * Haystack device.
     *
     * !This should be called only once for a specific address.!
     *
     * @param address
     * @param site
     * @param floor
     * @param room
     * @param equipRef
     * @param profile
     */
    public HyperStatDevice(int address, String site, String floor, String room, String equipRef, String profile) {
        Device d = new Device.Builder()
                       .setDisplayName("HS-"+address)
                       .addMarker("network").addMarker("node").addMarker(profile)
                       .addMarker("hyperstat")
                       .setEquipRef(equipRef)
                       .setAddr(address)
                       .setSiteRef(site)
                       .setFloorRef(floor)
                       .setRoomRef(room)
                       .setProfileType(profile)
                       .build();
        deviceRef = CCUHsApi.getInstance().addDevice(d);
        hyperStatNodeAddress = address;
        siteRef = site;
        floorRef = floor;
        roomRef = room;
        
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        tz = siteMap.get("tz").toString();
        
        createPoints();
    }
    
    /**
     * Reconstruct a HyperStatDevice from haystack database using the address.
     * @param address
     */
    public HyperStatDevice(int address) {
        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+address+"\"");
        Device d = new Device.Builder().setHashMap(device).build();
        
        deviceRef = d.getId();
        hyperStatNodeAddress = Integer.parseInt(d.getAddr());
        siteRef = d.getSiteRef();
        floorRef = d.getFloorRef();
        roomRef = d.getRoomRef();
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        tz = siteMap.get("tz").toString();
    }
    
    private void createPoints() {
        analog1In = new RawPoint.Builder()
                        .setDisplayName("Analog1In-"+hyperStatNodeAddress)
                        .setDeviceRef(deviceRef)
                        .setSiteRef(siteRef)
                        .setPort(Port.ANALOG_IN_ONE.toString())
                        .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                        .setRoomRef(roomRef)
                        .setFloorRef(floorRef)
                        .addMarker("sensor").addMarker("his")
                        .addMarker("mV")
                        .setTz(tz)
                        .build();
        
        
        analog2In = new RawPoint.Builder()
                        .setDisplayName("Analog2In-"+hyperStatNodeAddress)
                        .setDeviceRef(deviceRef)
                        .setSiteRef(siteRef)
                        .setPort(Port.ANALOG_IN_TWO.toString())
                        .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                        .setRoomRef(roomRef)
                        .setFloorRef(floorRef)
                        .addMarker("sensor").addMarker("his")
                        .setUnit("mV")
                        .setTz(tz)
                        .build();
        
        th1In = new RawPoint.Builder()
                    .setDisplayName("Th1In-"+hyperStatNodeAddress)
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
                    .setDisplayName("Th2In-"+hyperStatNodeAddress)
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
        
        
        relay1 = createRelayPoint(Port.RELAY_ONE, Tags.RELAY1);
        relay2 = createRelayPoint(Port.RELAY_TWO, Tags.RELAY2);
        relay3 = createRelayPoint(Port.RELAY_THREE, Tags.RELAY3);
        relay4 = createRelayPoint(Port.RELAY_FOUR, Tags.RELAY4);
        relay5 = createRelayPoint(Port.RELAY_FIVE, Tags.RELAY5);
        relay6 = createRelayPoint(Port.RELAY_SIX, Tags.RELAY6);
    
        analog1Out = createAnalogOutPoint(Port.ANALOG_OUT_ONE, "analog1Out");
        analog2Out = createAnalogOutPoint(Port.ANALOG_OUT_TWO, "analog2Out");
        analog3Out = createAnalogOutPoint(Port.ANALOG_OUT_THREE, "analog3Out");
        
        currentTemp = new RawPoint.Builder()
                          .setDisplayName("currentTemp-"+hyperStatNodeAddress)
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
                          .setDisplayName("desiredTemp-"+hyperStatNodeAddress)
                          .setDeviceRef(deviceRef)
                          .setSiteRef(siteRef)
                          .setRoomRef(roomRef)
                          .setFloorRef(floorRef)
                          .addMarker("sensor").addMarker("his")
                          .setPort(Port.DESIRED_TEMP.toString())
                          .setUnit("\u00B0F")
                          .setTz(tz)
                          .build();
    }
    
    private RawPoint createRelayPoint(Port relayPort, String name) {
        return new RawPoint.Builder()
                            .setDisplayName(hyperStatNodeAddress+"-"+name)
                            .setDeviceRef(deviceRef)
                            .setSiteRef(siteRef)
                            .setRoomRef(roomRef)
                            .setFloorRef(floorRef)
                            .setPort(relayPort.toString())
                            .setType(OutputRelayActuatorType.NormallyOpen.displayName)
                            .addMarker("cmd").addMarker("his")
                            .setTz(tz)
                            .build();
    }
    
    private RawPoint createAnalogOutPoint(Port analogPort, String name) {
        return new RawPoint.Builder()
                   .setDisplayName(hyperStatNodeAddress+"-"+name)
                   .setDeviceRef(deviceRef)
                   .setSiteRef(siteRef)
                   .setRoomRef(roomRef)
                   .setFloorRef(floorRef)
                   .setPort(analogPort.toString())
                   .setType(OutputRelayActuatorType.NormallyOpen.displayName)
                   .addMarker("cmd").addMarker("his")
                   .setTz(tz)
                   .build();
    }
    
    public void createPhysicalSensorPoint(Port p, String pointRef) {
        RawPoint sensor = new RawPoint.Builder()
                              .setDisplayName(p.toString()+"-"+hyperStatNodeAddress)
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
    
    public RawPoint createSensorPoints(Port p) {
        Equip equip = new Equip.Builder().setHashMap(CCUHsApi.getInstance()
                                                         .read("equip and group == \""+hyperStatNodeAddress+"\"")).build();
        String sensorUnit = "";
        boolean isOccupancySensor = false;
        switch (p){
            case SENSOR_NO:
            case SENSOR_CO2_EQUIVALENT:
            case SENSOR_CO:
            case SENSOR_CO2:
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
            case SENSOR_PM2P5:
            case SENSOR_PM10:
                sensorUnit =  "ug/m3";
                break;
            case SENSOR_OCCUPANCY:
                isOccupancySensor = true;
                break;
            default:
                break;
        }
        Point.Builder equipSensor = null;

        if(isOccupancySensor){
            equipSensor = new Point.Builder()
                    .setDisplayName(equip.getDisplayName() + "-" + p.getPortSensor())
                    .setEquipRef(equip.getId())
                    .setSiteRef(siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef).setHisInterpolate("cov")
                    .addMarker("zone").addMarker("sensor").addMarker(p.getPortSensor()).addMarker("his")
                    .addMarker("cur").addMarker("logical").addMarker(Tags.HYPERSTAT)
                    .setGroup(String.valueOf(hyperStatNodeAddress))
                    .setEnums("off,on")
                    .setTz(tz);

        }else {
            equipSensor = new Point.Builder()
                    .setDisplayName(equip.getDisplayName() + "-" + p.getPortSensor())
                    .setEquipRef(equip.getId())
                    .setSiteRef(siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef).setHisInterpolate("cov")
                    .addMarker("zone").addMarker("sensor").addMarker(p.getPortSensor()).addMarker("his")
                    .addMarker("cur").addMarker("logical").addMarker(Tags.HYPERSTAT)
                    .setGroup(String.valueOf(hyperStatNodeAddress))
                    .setUnit(sensorUnit)
                    .setTz(tz);
        }
        if (equip.getMarkers().contains(Tags.SENSE)) {
            equipSensor.addMarker(Tags.SENSE);
        }
        String pointRef = CCUHsApi.getInstance().addPoint(equipSensor.build());


        RawPoint deviceSensor = new RawPoint.Builder()
                                    .setDisplayName(p.toString()+"-"+hyperStatNodeAddress)
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
        CCUHsApi.getInstance().addPoint(relay1);
        CCUHsApi.getInstance().addPoint(relay2);
        CCUHsApi.getInstance().addPoint(relay3);
        CCUHsApi.getInstance().addPoint(relay4);
        CCUHsApi.getInstance().addPoint(relay5);
        CCUHsApi.getInstance().addPoint(relay6);
        CCUHsApi.getInstance().addPoint(analog1Out);
        CCUHsApi.getInstance().addPoint(analog2Out);
        CCUHsApi.getInstance().addPoint(analog3Out);
        CCUHsApi.getInstance().addPoint(currentTemp);
        CCUHsApi.getInstance().addPoint(desiredTemp);
    }

    public void addSensor(Port p, String pointRef) {
        RawPoint sensor = new RawPoint.Builder()
                .setDisplayName(p.toString()+"-"+hyperStatNodeAddress)
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
    
}
