package a75f.io.logic.bo.haystack.device;

import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Consts;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.firmware.FirmwareVersion;
import a75f.io.logic.bo.building.heartbeat.HeartBeat;

/**
 * Models a HyperConnect device Haystack entity.
 *
 * I
 */
public class HyperConnectDevice {

    int hyperStatNodeAddress;

    public RawPoint universal1In;
    public RawPoint universal2In;
    public RawPoint universal3In;
    public RawPoint universal4In;
    public RawPoint universal5In;
    public RawPoint universal6In;
    public RawPoint universal7In;
    public RawPoint universal8In;
    public RawPoint relay1;
    public RawPoint relay2;
    public RawPoint relay3;
    public RawPoint relay4;
    public RawPoint relay5;
    public RawPoint relay6;
    public RawPoint relay7;
    public RawPoint relay8;
    public RawPoint analog1Out;
    public RawPoint analog2Out;
    public RawPoint analog3Out;
    public RawPoint analog4Out;
    public RawPoint rssi;
    private RawPoint firmWareVersion;

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
    public HyperConnectDevice(int address, String site, String floor, String room, String equipRef, String profile) {
        Device d = new Device.Builder()
                       .setDisplayName("HC-"+address)
                       .addMarker("network").addMarker("node").addMarker(profile)
                       .addMarker("hyperconnect")
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
     * Reconstruct a HyperConnectDevice from haystack database using the address.
     * @param address
     */
    public HyperConnectDevice(int address) {
        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+address+"\" and hyperconnect");
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
        universal1In = new RawPoint.Builder()
                        .setDisplayName("Universal1In-"+hyperStatNodeAddress)
                        .setDeviceRef(deviceRef)
                        .setSiteRef(siteRef)
                        .setPort(Port.UNIVERSAL_IN_ONE.toString())
                        // TODO: this is now a UI
                        .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                        .setRoomRef(roomRef)
                        .setFloorRef(floorRef)
                        .addMarker("sensor").addMarker("his")
                        .setUnit("mV")
                        .setTz(tz)
                        .build();

        universal2In = new RawPoint.Builder()
                .setDisplayName("Universal2In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_TWO.toString())
                // TODO: this is now a UI
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setUnit("mV")
                .setTz(tz)
                .build();

        universal3In = new RawPoint.Builder()
                .setDisplayName("Universal3In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_THREE.toString())
                // TODO: this is now a UI
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setUnit("mV")
                .setTz(tz)
                .build();

        universal4In = new RawPoint.Builder()
                .setDisplayName("Universal4In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_FOUR.toString())
                // TODO: this is now a UI
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setUnit("mV")
                .setTz(tz)
                .build();

        universal5In = new RawPoint.Builder()
                .setDisplayName("Universal5In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_FIVE.toString())
                // TODO: this is now a UI
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setUnit("mV")
                .setTz(tz)
                .build();

        universal6In = new RawPoint.Builder()
                .setDisplayName("Universal6In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_SIX.toString())
                // TODO: this is now a UI
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setUnit("mV")
                .setTz(tz)
                .build();

        universal7In = new RawPoint.Builder()
                .setDisplayName("Universal7In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_SEVEN.toString())
                // TODO: this is now a UI
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setUnit("mV")
                .setTz(tz)
                .build();

        universal8In = new RawPoint.Builder()
                .setDisplayName("Universal8In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_EIGHT.toString())
                // TODO: this is now a UI
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setUnit("mV")
                .setTz(tz)
                .build();
        
        
        relay1 = createRelayPoint(Port.RELAY_ONE, Tags.RELAY1);
        relay2 = createRelayPoint(Port.RELAY_TWO, Tags.RELAY2);
        relay3 = createRelayPoint(Port.RELAY_THREE, Tags.RELAY3);
        relay4 = createRelayPoint(Port.RELAY_FOUR, Tags.RELAY4);
        relay5 = createRelayPoint(Port.RELAY_FIVE, Tags.RELAY5);
        relay6 = createRelayPoint(Port.RELAY_SIX, Tags.RELAY6);
        relay7 = createRelayPoint(Port.RELAY_SEVEN, Tags.RELAY7);
        relay8 = createRelayPoint(Port.RELAY_EIGHT, Tags.RELAY8);
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON,"HyperConnect deviceRef: " + deviceRef);
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "relay1: " + relay1 + " | port: " + relay1.getPort() + " | deviceRef: " + relay1.getDeviceRef() + " | type: " + relay1.getType());
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "relay2: " + relay2 + " | port: " + relay2.getPort() + " | deviceRef: " + relay2.getDeviceRef() + " | type: " + relay2.getType());
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "relay3: " + relay3 + " | port: " + relay3.getPort() + " | deviceRef: " + relay3.getDeviceRef() + " | type: " + relay3.getType());
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "relay4: " + relay4 + " | port: " + relay4.getPort() + " | deviceRef: " + relay4.getDeviceRef() + " | type: " + relay4.getType());
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "relay5: " + relay5 + " | port: " + relay5.getPort() + " | deviceRef: " + relay5.getDeviceRef() + " | type: " + relay5.getType());
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "relay6: " + relay6 + " | port: " + relay6.getPort() + " | deviceRef: " + relay6.getDeviceRef() + " | type: " + relay6.getType());
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "relay7: " + relay7 + " | port: " + relay7.getPort() + " | deviceRef: " + relay7.getDeviceRef() + " | type: " + relay7.getType());
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "relay8: " + relay8 + " | port: " + relay8.getPort() + " | deviceRef: " + relay8.getDeviceRef() + " | type: " + relay8.getType());

        analog1Out = createAnalogOutPoint(Port.ANALOG_OUT_ONE, "analog1Out");
        analog2Out = createAnalogOutPoint(Port.ANALOG_OUT_TWO, "analog2Out");
        analog3Out = createAnalogOutPoint(Port.ANALOG_OUT_THREE, "analog3Out");
        analog4Out = createAnalogOutPoint(Port.ANALOG_OUT_FOUR, "analog4Out");
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "analog1Out: " + analog1Out + " | port: " + analog1Out.getPort() + " | deviceRef: " + analog1Out.getDeviceRef() + " | type: " + analog1Out.getType());
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "analog2Out: " + analog2Out + " | port: " + analog2Out.getPort() + " | deviceRef: " + analog2Out.getDeviceRef() + " | type: " + analog2Out.getType());
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "analog3Out: " + analog3Out + " | port: " + analog3Out.getPort() + " | deviceRef: " + analog3Out.getDeviceRef() + " | type: " + analog3Out.getType());
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "analog4Out: " + analog4Out + " | port: " + analog4Out.getPort() + " | deviceRef: " + analog4Out.getDeviceRef() + " | type: " + analog4Out.getType());

        rssi = HeartBeat.getHeartBeatRawPoint(hyperStatNodeAddress, deviceRef, siteRef, roomRef, floorRef, tz);
        firmWareVersion = FirmwareVersion.getFirmwareVersion(hyperStatNodeAddress, deviceRef, siteRef, floorRef, roomRef,
                tz);
    }
    
    private RawPoint createRelayPoint(Port relayPort, String name) {
        return new RawPoint.Builder()
                            .setDisplayName(name+"-"+hyperStatNodeAddress)
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
                   .setDisplayName(name+"-"+hyperStatNodeAddress)
                   .setDeviceRef(deviceRef)
                   .setSiteRef(siteRef)
                   .setRoomRef(roomRef)
                   .setFloorRef(floorRef)
                   .setPort(analogPort.toString())
                   .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                   .addMarker("cmd").addMarker("his")
                   .setUnit("dV")
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

    // TODO: Implementation. I don't think these will be needed.
    public RawPoint createSensorPoints(Port p) {
        Equip equip = new Equip.Builder().setHashMap(CCUHsApi.getInstance()
                                                         .read("equip and group == \""+hyperStatNodeAddress+"\"")).build();
        String sensorUnit = "";
        boolean isOccupancySensor = false;
        boolean hasAirMarker = false;
        switch (p){
            case SENSOR_NO:
            case SENSOR_CO2_EQUIVALENT:
            case SENSOR_CO:
            case SENSOR_CO2:
                sensorUnit = "ppm";
                hasAirMarker = true;
                break;
            case SENSOR_ILLUMINANCE:
                sensorUnit = "lux";
                break;
            case SENSOR_PRESSURE:
                sensorUnit = Consts.PRESSURE_UNIT;
                break;
            case SENSOR_SOUND:
                sensorUnit = "dB";
                break;
            case SENSOR_VOC:
                sensorUnit = "ppb";
                hasAirMarker = true;
                break;
            case SENSOR_PM2P5:
            case SENSOR_PM10:
                sensorUnit =  "ug/\u33A5";
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

        if (hasAirMarker) {
            equipSensor.addMarker(Tags.AIR);
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
        CCUHsApi.getInstance().addPoint(universal1In);
        CCUHsApi.getInstance().addPoint(universal2In);
        CCUHsApi.getInstance().addPoint(universal3In);
        CCUHsApi.getInstance().addPoint(universal4In);
        CCUHsApi.getInstance().addPoint(universal5In);
        CCUHsApi.getInstance().addPoint(universal6In);
        CCUHsApi.getInstance().addPoint(universal7In);
        CCUHsApi.getInstance().addPoint(universal8In);
        CCUHsApi.getInstance().addPoint(relay1);
        CCUHsApi.getInstance().addPoint(relay2);
        CCUHsApi.getInstance().addPoint(relay3);
        CCUHsApi.getInstance().addPoint(relay4);
        CCUHsApi.getInstance().addPoint(relay5);
        CCUHsApi.getInstance().addPoint(relay6);
        CCUHsApi.getInstance().addPoint(relay7);
        CCUHsApi.getInstance().addPoint(relay8);
        CCUHsApi.getInstance().addPoint(analog1Out);
        CCUHsApi.getInstance().addPoint(analog2Out);
        CCUHsApi.getInstance().addPoint(analog3Out);
        CCUHsApi.getInstance().addPoint(analog4Out);
        CCUHsApi.getInstance().addPoint(rssi);
        CCUHsApi.getInstance().addPoint(firmWareVersion);
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
