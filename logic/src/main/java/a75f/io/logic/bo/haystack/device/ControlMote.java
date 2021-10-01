package a75f.io.logic.bo.haystack.device;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.firmware.FirmwareVersion;
import a75f.io.logic.bo.building.heartbeat.HeartBeat;

/**
 * Created by samjithsadasivan on 12/6/18.
 */

public class ControlMote
{
    private Site   site;
    private String deviceRef;
    int smartNodeAddress;

    public RawPoint analog1In;
    public RawPoint analog2In;
    public RawPoint th1In;
    public RawPoint th2In;
    public RawPoint currentTemp;
    public RawPoint rssi;
    private RawPoint firmWareVersion;
    public String siteRef;
    public String floorRef;
    public String roomRef;
    public String equipRef;

    String tz;
    public ControlMote(String systemEquipRef) {
        
        HashMap device = CCUHsApi.getInstance().read("device and cm");
        if ((device != null) && (device.size() > 0)) {
            Device d = new Device.Builder().setHashMap(device).build();
            d.setEquipRef(systemEquipRef);
            CCUHsApi.getInstance().updateDevice(d,d.getId());
            createNewCMPointsForUpgrades();
            CcuLog.d(L.TAG_CCU_DEVICE," CM device exists - update equipRef ="+systemEquipRef);
            return;
        }
        site = new Site.Builder().setHashMap(CCUHsApi.getInstance().read(Tags.SITE)).build();
        
        Device d = new Device.Builder()
                           .setDisplayName("CM-device")
                           .addMarker("network")
                           .addMarker("cm")
                           .addMarker("his")
                           .setSiteRef(site.getId())
                           .setEquipRef(systemEquipRef)
                           .build();
        deviceRef = CCUHsApi.getInstance().addDevice(d);
        createPoints();
        addFirmwareVersionPoint();
    }
	//For CCU as a zone part
    public ControlMote(int address, String site, String floor, String room, String equipRef) {

        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+address+"\"");
        if (device != null && device.size() > 0) {
            CcuLog.d(L.TAG_CCU_DEVICE," CM device exists");
            return;
        }
        Device d = new Device.Builder()
                .setDisplayName("TI-"+address)
                .addMarker("network").addMarker("node").addMarker("ti").addMarker("his")
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

        createCcuAsZonePoints();
    }
   
    public void createPoints() {
        addRelayStatePoint("relay1");
        addRelayStatePoint("relay2");
        addRelayStatePoint("relay3");
        addRelayStatePoint("relay4");
        addRelayStatePoint("relay5");
        addRelayStatePoint("relay6");
        addRelayStatePoint("relay7");
        addRelayStatePoint("relay8");
    
        addCMOutPortPoint("analog1");
        addCMOutPortPoint("analog2");
        addCMOutPortPoint("analog3");
        addCMOutPortPoint("analog4");
        
        addCMInPortPoint("analog1");
        addCMInPortPoint("analog2");
    
        addCMInPortPoint("th1");
        addCMInPortPoint("th2");

        setRelayState("relay1",0);
        setRelayState("relay2",0);
        setRelayState("relay3",0);
        setRelayState("relay4",0);
        setRelayState("relay5",0);
        setRelayState("relay6",0);
        setRelayState("relay7",0);
        setRelayState("relay8",0);
        
    
        setAnalogOut("analog1",0);
        setAnalogOut("analog2",0);
        setAnalogOut("analog3",0);
        setAnalogOut("analog4",0);
    
        setAnalogThInVal("analog1", 0);
        setAnalogThInVal("analog2", 0);
        setAnalogThInVal("th1", 0);
        setAnalogThInVal("th2", 0);
    }
    
    private void createNewCMPointsForUpgrades() {
        
        addCMInPortPoint("analog1");
        addCMInPortPoint("analog2");
    
        addCMInPortPoint("th1");
        addCMInPortPoint("th2");
        
        setAnalogThInVal("analog1", 0);
        setAnalogThInVal("analog2", 0);
        setAnalogThInVal("th1", 0);
        setAnalogThInVal("th2", 0);
    }

    //For CCU as a zone part
    public void createCcuAsZonePoints(){

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

        analog1In = new RawPoint.Builder()
                .setDisplayName("Analog1In-"+smartNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setPort(Port.ANALOG_IN_ONE.toString())
                .setType(OutputAnalogActuatorType.ZeroToTenV.displayName)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("sensor").addMarker("his")
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
                .addMarker("sensor").addMarker("his")
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
                .addMarker("sensor").addMarker("his")
                .setUnit("Ohm")
                .setTz(tz)
                .build();

        rssi = HeartBeat.getHeartBeatRawPoint(smartNodeAddress, deviceRef, siteRef, roomRef, floorRef, tz);
        firmWareVersion = FirmwareVersion.getFirmwareVersion(smartNodeAddress, deviceRef, siteRef, floorRef, roomRef,
                tz);

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
    }
    public void resetAllOp(){
        for (int i = 1; i <= 8; i++)
        {
            setRelayState("relay" + i, 0);
        }
        for (int i = 1; i <= 4; i++)
        {
            setAnalogOut("analog" + 4, 0);
        }
    }
    
    private void addCMOutPortPoint(String port) {
        
        RawPoint p = new RawPoint.Builder()
                          .setDisplayName(site.getDisplayName()+"-CM-"+port+"Out")
                          .setDeviceRef(deviceRef)
                          .setSiteRef(site.getId())
                          .addMarker(port).addMarker("his").addMarker("system").addMarker("out")
                          .setTz(site.getTz())
                          .setUnit("dV")
                          .build();
        CCUHsApi.getInstance().addPoint(p);
    }
    
    private void addCMInPortPoint(String port) {
    
        HashMap<Object, Object> pointMap =
            CCUHsApi.getInstance().readEntity("point and system and in and "+port);
        if (!pointMap.isEmpty()) {
            CcuLog.i(L.TAG_CCU_DEVICE, "CM Point Exists for "+port );
            return;
        }
        RawPoint p = new RawPoint.Builder()
                         .setDisplayName(site.getDisplayName()+"-CM-"+port+"In")
                         .setDeviceRef(deviceRef)
                         .setSiteRef(site.getId())
                         .addMarker(port).addMarker("his").addMarker("system").addMarker("in")
                         .setTz(site.getTz())
                         .build();
        CCUHsApi.getInstance().addPoint(p);
    }

    private void addFirmwareVersionPoint(){
        HashMap firmwarePoint =
                CCUHsApi.getInstance().read("point and physical and firmware and version and deviceRef == \"" + deviceRef + "\"");
        if(!firmwarePoint.isEmpty()){
            CcuLog.i(L.TAG_CCU_DEVICE, "Firmware Version Point Exists");
            return;
        }
        firmWareVersion = FirmwareVersion.getFirmwareVersion(site.getDisplayName()+"-CM-"+"firmwareVersion",
                deviceRef, siteRef, site.getTz());
        CCUHsApi.getInstance().addPoint(firmWareVersion);
    }
    
    public static double getAnalogOut(String analog)
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and his and system and out and "+analog);
    }
    
    public static void setAnalogOut(String analog, double val)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and his and system and out and "+analog, val);
    }
    
    public static void setAnalogThInVal(String analog, double val)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and his and system and in and "+analog, val);
    }
    
    private void addRelayStatePoint(String relay){
        RawPoint p = new RawPoint.Builder()
                             .setDisplayName(site.getDisplayName()+"-"+relay+"State")
                             .setDeviceRef(deviceRef)
                             .setSiteRef(site.getId())
                             .addMarker(relay).addMarker("his").addMarker("system").addMarker("state")
                             .setTz(site.getTz())
                             .build();
        CCUHsApi.getInstance().addPoint(p);
    }
    
    public static double getRelayState(String relay)
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and his and system and state and "+relay);
    }
    public static void setRelayState(String relay, double val)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and his and system and state and "+relay, val);
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
        CCUHsApi.getInstance().addPoint(currentTemp);
        CCUHsApi.getInstance().addPoint(rssi);
        CCUHsApi.getInstance().addPoint(firmWareVersion);
    }
}
