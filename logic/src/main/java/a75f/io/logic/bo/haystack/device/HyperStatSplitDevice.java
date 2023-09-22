package a75f.io.logic.bo.haystack.device;

import static a75f.io.api.haystack.Tags.UNIVERSAL1;
import static a75f.io.api.haystack.Tags.UNIVERSAL2;
import static a75f.io.api.haystack.Tags.UNIVERSAL3;
import static a75f.io.api.haystack.Tags.UNIVERSAL4;
import static a75f.io.api.haystack.Tags.UNIVERSAL5;
import static a75f.io.api.haystack.Tags.UNIVERSAL6;
import static a75f.io.api.haystack.Tags.UNIVERSAL7;
import static a75f.io.api.haystack.Tags.UNIVERSAL8;
import static a75f.io.logic.BacnetUtilKt.ANALOG_VALUE;
import static a75f.io.logic.BacnetUtilKt.BINARY_VALUE;
import static a75f.io.logic.BacnetUtilKt.CO2;
import static a75f.io.logic.BacnetUtilKt.CO2EQUIVALENT;
import static a75f.io.logic.BacnetUtilKt.HUMIDITY;
import static a75f.io.logic.BacnetUtilKt.ILLUMINANCE;
import static a75f.io.logic.BacnetUtilKt.OCCUPANCY;
import static a75f.io.logic.BacnetUtilKt.SOUND;
import static a75f.io.logic.BacnetUtilKt.VOC;
import static a75f.io.logic.BacnetUtilKt.addBacnetTags;

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
import a75f.io.logic.bo.building.hyperstat.common.HyperstatProfileNames;
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil;
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperstatSplitProfileNames;
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconConfiguration;

/**
 * Models a HyperStat Split device Haystack entity.
 */
public class HyperStatSplitDevice {

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

    public RawPoint supplyAirTempSensor;
    public RawPoint supplyAirHumiditySensor;
    public RawPoint mixedAirTempSensor;
    public RawPoint mixedAirHumiditySensor;
    public RawPoint outsideAirTempSensor;
    public RawPoint outsideAirHumiditySensor;
    public RawPoint ductStaticPressureSensor;

    public RawPoint rssi;
    private RawPoint firmWareVersion;
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
    public HyperStatSplitDevice(int address, String site, String floor, String room, String equipRef, String profile) {
        Device d = new Device.Builder()
                .setDisplayName("HSS-" + address)
                .addMarker("network").addMarker("node")
                .addMarker(Tags.HYPERSTATSPLIT)
                .setEquipRef(equipRef)
                .setAddr(address)
                .setSiteRef(site)
                .setFloorRef(floor)
                .setRoomRef(room)
                .setProfileType(HyperstatSplitProfileNames.HSSPLIT_CPUECON)
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
     * Reconstruct a HyperStatSplitDevice from haystack database using the address.
     * @param address
     */
    public HyperStatSplitDevice(int address) {
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

    /**
     * Universal Input points will be set up as 0-10V inputs by default.
     * If configured as a digital/thermistor input:
     *  - "type" will be changed to "0"
     *  - "unit" will be changed to "Kilo Ohm"
     */
    // TODO: make these updates
    private void createPoints() {
        universal1In = new RawPoint.Builder()
                .setDisplayName("universal1In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_ONE.toString())
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .addMarker("cur").addMarker("zone")
                .addMarker(UNIVERSAL1)
                .setUnit("mV")
                .setTz(tz)
                .build();

        universal2In = new RawPoint.Builder()
                .setDisplayName("universal2In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_TWO.toString())
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .addMarker("cur").addMarker("zone")
                .addMarker(UNIVERSAL2)
                .setUnit("mV")
                .setTz(tz)
                .build();

        universal3In = new RawPoint.Builder()
                .setDisplayName("universal3In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_THREE.toString())
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .addMarker("cur").addMarker("zone")
                .addMarker(UNIVERSAL3)
                .setUnit("mV")
                .setTz(tz)
                .build();

        universal4In = new RawPoint.Builder()
                .setDisplayName("universal4In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_FOUR.toString())
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .addMarker("cur").addMarker("zone")
                .addMarker(UNIVERSAL4)
                .setUnit("mV")
                .setTz(tz)
                .build();

        universal5In = new RawPoint.Builder()
                .setDisplayName("universal5In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_FIVE.toString())
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .addMarker("cur").addMarker("zone")
                .addMarker(UNIVERSAL5)
                .setUnit("mV")
                .setTz(tz)
                .build();

        universal6In = new RawPoint.Builder()
                .setDisplayName("universal6In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_SIX.toString())
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .addMarker("cur").addMarker("zone")
                .addMarker(UNIVERSAL6)
                .setUnit("mV")
                .setTz(tz)
                .build();

        universal7In = new RawPoint.Builder()
                .setDisplayName("universal7In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_SEVEN.toString())
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .addMarker("cur").addMarker("zone")
                .addMarker(UNIVERSAL7)
                .setUnit("mV")
                .setTz(tz)
                .build();

        universal8In = new RawPoint.Builder()
                .setDisplayName("universal8In-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.UNIVERSAL_IN_EIGHT.toString())
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .addMarker("cur").addMarker("zone")
                .addMarker(UNIVERSAL8)
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

        analog1Out = createAnalogOutPoint(Port.ANALOG_OUT_ONE, "analog1Out");
        analog2Out = createAnalogOutPoint(Port.ANALOG_OUT_TWO, "analog2Out");
        analog3Out = createAnalogOutPoint(Port.ANALOG_OUT_THREE, "analog3Out");
        analog4Out = createAnalogOutPoint(Port.ANALOG_OUT_FOUR, "analog4Out");

        supplyAirTempSensor = new RawPoint.Builder()
                .setDisplayName("supplyAirTempSensor-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setPort(Port.SENSOR_SAT.name())
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();

        supplyAirHumiditySensor = new RawPoint.Builder()
                .setDisplayName("supplyAirHumiditySensor-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setPort(Port.SENSOR_SAH.name())
                .setUnit("%")
                .setTz(tz)
                .build();

        mixedAirTempSensor = new RawPoint.Builder()
                .setDisplayName("mixedAirTempSensor-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setPort(Port.SENSOR_MAT.name())
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();

        mixedAirHumiditySensor = new RawPoint.Builder()
                .setDisplayName("mixedAirHumiditySensor-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setPort(Port.SENSOR_MAH.name())
                .setUnit("%")
                .setTz(tz)
                .build();

        outsideAirTempSensor = new RawPoint.Builder()
                .setDisplayName("outsideAirTempSensor-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setPort(Port.SENSOR_OAT.name())
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();

        outsideAirHumiditySensor = new RawPoint.Builder()
                .setDisplayName("outsideAirHumiditySensor-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setPort(Port.SENSOR_OAH.name())
                .setUnit("%")
                .setTz(tz)
                .build();

        ductStaticPressureSensor = new RawPoint.Builder()
                .setDisplayName("ductStaticPressureSensor-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setPort(Port.SENSOR_PRESSURE.name())
                .setUnit("inH₂O")
                .setTz(tz)
                .build();

        currentTemp = new RawPoint.Builder()
                .setDisplayName("currentTemp-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his").addMarker("zone")
                .setPort(Port.SENSOR_RT.name())
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();

        desiredTemp = new RawPoint.Builder()
                .setDisplayName("desiredTemp-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his").addMarker("zone")
                .setPort(Port.DESIRED_TEMP.name())
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();


        rssi = HeartBeat.getHeartBeatRawPoint(hyperStatNodeAddress, deviceRef, siteRef, roomRef, floorRef, tz);
        firmWareVersion = FirmwareVersion.getFirmwareVersion(hyperStatNodeAddress, deviceRef, siteRef, floorRef, roomRef,
                tz);

        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "Finished adding device points");

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
                .addMarker("cur").addMarker("zone")
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
                .addMarker("cur").addMarker("zone")
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
        if (equip.getMarkers().contains(Tags.MONITORING)) {
            equipSensor.addMarker(Tags.MONITORING);
        }

        if (hasAirMarker) {
            equipSensor.addMarker(Tags.AIR);
        }

        Point sensorPoint = equipSensor.build();

        switch (p.getPortSensor()){
            case OCCUPANCY:
                addBacnetTags(sensorPoint, 40, BINARY_VALUE, hyperStatNodeAddress);
                break;
            case HUMIDITY:
                addBacnetTags(sensorPoint, 38, ANALOG_VALUE, hyperStatNodeAddress);
                break;
            case ILLUMINANCE:
                addBacnetTags(sensorPoint, 39, ANALOG_VALUE, hyperStatNodeAddress);
                break;
            case CO2:
                addBacnetTags(sensorPoint, 4, ANALOG_VALUE, hyperStatNodeAddress);
                break;
            case VOC:
                addBacnetTags(sensorPoint, 45, ANALOG_VALUE, hyperStatNodeAddress);
                break;
            case CO2EQUIVALENT:
                addBacnetTags(sensorPoint, 5, ANALOG_VALUE, hyperStatNodeAddress);
                break;
            case SOUND:
                addBacnetTags(sensorPoint, 44, ANALOG_VALUE, hyperStatNodeAddress);
                break;
        }

        String pointRef = CCUHsApi.getInstance().addPoint(sensorPoint);

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
        CCUHsApi.getInstance().addPoint(currentTemp);
        CCUHsApi.getInstance().addPoint(desiredTemp);
        CCUHsApi.getInstance().addPoint(rssi);
        CCUHsApi.getInstance().addPoint(firmWareVersion);
    }

    public void addSensor(Port p, String pointRef) {
        RawPoint sensor = new RawPoint.Builder()
                .setDisplayName(p.getPortSensor()+"Sensor-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setPointRef(pointRef)
                .setEnabled(true)
                .addMarker("sensor").addMarker("his")
                .addMarker("cur").addMarker("zone")
                .setPort(p.toString())
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(sensor);
    }

    public void addSensor(Port p, String pointRef, String unit) {
        RawPoint sensor = new RawPoint.Builder()
                .setDisplayName(p.getPortSensor()+"Sensor-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setPointRef(pointRef)
                .setEnabled(true)
                .addMarker("sensor").addMarker("his")
                .addMarker("cur").addMarker("zone")
                .setPort(p.toString())
                .setUnit(unit)
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(sensor);
    }

    public void addSensor(Port p, String pointRef, String unit, boolean enabled) {
        RawPoint sensor = new RawPoint.Builder()
                .setDisplayName(p.getPortSensor()+"Sensor-"+hyperStatNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setPointRef(pointRef)
                .setEnabled(enabled)
                .addMarker("sensor").addMarker("his")
                .addMarker("cur").addMarker("zone")
                .setPort(p.toString())
                .setUnit(unit)
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(sensor);
    }

}
