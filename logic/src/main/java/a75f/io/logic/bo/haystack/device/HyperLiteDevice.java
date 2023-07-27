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
public class HyperLiteDevice {

    int hyperStatNodeAddress;

    public RawPoint currentTemp;
    public RawPoint desiredTemp;
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
    public HyperLiteDevice(int address, String site, String floor, String room, String equipRef, String profile) {
        Device d = new Device.Builder()
                       .setDisplayName("HL-"+address)
                       .addMarker("network").addMarker("node").addMarker(profile)
                       .addMarker("hyperlite")
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
     * Reconstruct a HyperLiteDevice from haystack database using the address.
     * @param address
     */
    public HyperLiteDevice(int address) {
        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+address+"\" and hyperlite");
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
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON,"HyperLite deviceRef: " + deviceRef);
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
        CCUHsApi.getInstance().addPoint(currentTemp);
        CCUHsApi.getInstance().addPoint(desiredTemp);
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
