package a75f.io.logic.bo.haystack.device;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.firmware.FirmwareVersion;
import a75f.io.logic.bo.building.heartbeat.HeartBeat;

public class OTN {
    private RawPoint th1In;
    private RawPoint th2In;
    private RawPoint currentTemp;
    private RawPoint rssi;
    private RawPoint firmWareVersion;

    private int otnAddres;
    private String deviceRef;
    private String siteRef;
    private String floorRef;
    private String roomRef;
    private String tz;

    public RawPoint getCurrentTemp() {
        return currentTemp;
    }

    public void setCurrentTemp(RawPoint currentTemp) {
        this.currentTemp = currentTemp;
    }

    public RawPoint getRssi() {
        return rssi;
    }

    public void setRssi(RawPoint rssi) {
        this.rssi = rssi;
    }

    public OTN(int address, String site, String floor, String room, String equipRef) {
        Device d = new Device.Builder()
                .setDisplayName("OTN-"+address)
                .addMarker("network").addMarker("node").addMarker("otn").addMarker("his")
                .setAddr(address)
                .setSiteRef(site)
                .setFloorRef(floor)
                .setEquipRef(equipRef)
                .setRoomRef(room)
                .build();
        deviceRef = CCUHsApi.getInstance().addDevice(d);
        otnAddres = address;
        siteRef = site;
        floorRef = floor;
        roomRef = room;

        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        tz = siteMap.get("tz").toString();

        createPoints();
    }
    private void createPoints() {
        th1In = new RawPoint.Builder()
                .setDisplayName("Th1In-"+otnAddres)
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
                .setDisplayName("Th2In-"+otnAddres)
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

        currentTemp = new RawPoint.Builder()
                .setDisplayName("currentTemp-"+otnAddres)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
                .setPort(Port.SENSOR_RT.toString())
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();

        rssi = HeartBeat.getHeartBeatRawPoint(otnAddres, deviceRef, siteRef, roomRef, floorRef, tz);
        firmWareVersion = FirmwareVersion.getFirmwareVersion(otnAddres, deviceRef, siteRef, floorRef, roomRef,
                tz);
    }

    public void addPointsToDb() {
        CCUHsApi.getInstance().addPoint(th1In);
        CCUHsApi.getInstance().addPoint(th2In);
        CCUHsApi.getInstance().addPoint(currentTemp);
        CCUHsApi.getInstance().addPoint(rssi);
        CCUHsApi.getInstance().addPoint(firmWareVersion);
    }

    public void addSensor(Port p, String pointRef) {
        RawPoint sensor = new RawPoint.Builder()
                .setDisplayName(p.toString()+"-"+otnAddres)
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
