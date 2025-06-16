package a75f.io.logic.bo.haystack.device;
import java.util.ArrayList;
import java.util.HashMap;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.api.Point;
import a75f.io.domain.util.CommonQueries;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.UtilKt;
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
        
        ArrayList<HashMap<Object,Object>> devices = CCUHsApi.getInstance().readAllEntities("device and cm");

        if (!devices.isEmpty()) {
            devices.forEach(device -> {
                CcuLog.d(Domain.LOG_TAG," Delete CM device "+device);
                CCUHsApi.getInstance().deleteEntityTree(device.get("id").toString());
            });
        }
        site = new Site.Builder().setHashMap(CCUHsApi.getInstance().read(Tags.SITE)).build();
        siteRef = site.getId();
        Device d = new Device.Builder()
                           .setDisplayName("CM-device")
                           .addMarker("network")
                           .addMarker("cm")
                           .setSiteRef(siteRef)
                           .setEquipRef(systemEquipRef)
                           .build();
        deviceRef = CCUHsApi.getInstance().addDevice(d);
        createPoints();
        addFirmwareVersionPoint();
    }
	//For CCU as a zone part
    public ControlMote(int address, String site, String floor, String room, String equipRef) {

        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+address+"\"");
        if (device != null && !device.isEmpty()) {
            CcuLog.d(L.TAG_CCU_DEVICE," CM device exists");
            return;
        }
        Device d = new Device.Builder()
                .setDisplayName("TI-"+address)
                .addMarker("network").addMarker("node").addMarker("ti")
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
                .setUnit("Kilo Ohm")
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
                .setUnit("Kilo Ohm")
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

    public static HashMap<Object, Object> getTestPoint(String analog) {
        return CCUHsApi.getInstance().readEntity("point and his and system and out and " + analog);
    }

    public static HashMap getRelayTestPoint(String relay) {
        return CCUHsApi.getInstance().read("point and his and system and state and "+relay);
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
    public static void setRelayState(String relay, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and his and system and state and "+relay, val);
    }

    public static double getRelayState(Point point) {
        return CCUHsApi.getInstance().readHisValByQuery("point and his and system and state and "
                +getRelayStringFromEnablePoint(point));
    }
    public static void setRelayState(Point point, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and his and system and state and "
                +getRelayStringFromEnablePoint(point), val);
    }

    public static String getRelayStringFromEnablePoint(Point point) {
        if (point.getDomainName().equals(DomainName.relay1OutputEnable)) {
            return DomainName.relay1;
        } else if (point.getDomainName().equals(DomainName.relay2OutputEnable)) {
            return DomainName.relay2;
        } if (point.getDomainName().equals(DomainName.relay3OutputEnable)) {
            return DomainName.relay3;
        } if (point.getDomainName().equals(DomainName.relay4OutputEnable)) {
            return DomainName.relay4;
        } if (point.getDomainName().equals(DomainName.relay5OutputEnable)) {
            return DomainName.relay5;
        } if (point.getDomainName().equals(DomainName.relay6OutputEnable)) {
            return DomainName.relay6;
        } if (point.getDomainName().equals(DomainName.relay7OutputEnable)) {
            return DomainName.relay7;
        }
        CcuLog.e(L.TAG_CCU_DEVICE, "Invalid relay domain point "+point.getDomainName());
        return "";
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

    public void addPointsToDb() {
        CCUHsApi.getInstance().addPoint(analog1In);
        CCUHsApi.getInstance().addPoint(analog2In);
        CCUHsApi.getInstance().addPoint(th1In);
        CCUHsApi.getInstance().addPoint(th2In);
        CCUHsApi.getInstance().addPoint(currentTemp);
        CCUHsApi.getInstance().addPoint(rssi);
        CCUHsApi.getInstance().addPoint(firmWareVersion);
    }
    
    public static void setAnalog1Out(double val) {
        ControlMote.setAnalogOut(Tags.ANALOG1, val);
    }
    public static void setAnalog2Out(double val) {
        ControlMote.setAnalogOut(Tags.ANALOG2, val);
    }
    public static void setAnalog3Out(double val) {
        ControlMote.setAnalogOut(Tags.ANALOG3, val);
    }
    public static void setAnalog4Out(double val) {
        ControlMote.setAnalogOut(Tags.ANALOG4, val);
    }
    public static int getAnalog1Out() {
        return (int)ControlMote.getAnalogOut(Tags.ANALOG1);
    }
    public static int getAnalog2Out() {
        return (int)ControlMote.getAnalogOut(Tags.ANALOG2);
    }
    public static int getAnalog3Out() {
        return (int)ControlMote.getAnalogOut(Tags.ANALOG3);
    }
    public static int getAnalog4Out() {
        return (int)ControlMote.getAnalogOut(Tags.ANALOG4);
    }
    public static void setRelay1(double val) {
        ControlMote.setRelayState(Tags.RELAY1, val);
    }
    public static void setRelay2(double val) {
        ControlMote.setRelayState(Tags.RELAY2, val);
    }
    public static void setRelay3(double val) {
        ControlMote.setRelayState(Tags.RELAY3, val);
    }
    public static void setRelay4(double val) {
        ControlMote.setRelayState(Tags.RELAY4, val);
    }
    public static void setRelay5(double val) {
        ControlMote.setRelayState(Tags.RELAY5, val);
    }
    public static void setRelay6(double val) {
        ControlMote.setRelayState(Tags.RELAY6, val);
    }
    public static void setRelay7(double val) {
        ControlMote.setRelayState(Tags.RELAY7, val);
    }
    
    public static boolean getRelay1() {
        return ControlMote.getRelayState(Tags.RELAY1) > 0.01;
    }
    public static boolean getRelay2() {
        return ControlMote.getRelayState(Tags.RELAY2) > 0.01;
    }
    public static boolean getRelay3() {
        return ControlMote.getRelayState(Tags.RELAY3) > 0.01;
    }
    public static boolean getRelay4() {
        return ControlMote.getRelayState(Tags.RELAY4) > 0.01;
    }
    public static boolean getRelay5() {
        return ControlMote.getRelayState(Tags.RELAY5) > 0.01;
    }
    public static boolean getRelay6() {
        return ControlMote.getRelayState(Tags.RELAY6) > 0.01;
    }
    public static boolean getRelay7() {
        return ControlMote.getRelayState(Tags.RELAY7) > 0.01;
    }


    public static void updatePhysicalPointRef(int addr, String port, String pointRef) {
        CcuLog.d("CCU"," Update Physical point "+port);

        HashMap<Object,Object> device = CCUHsApi.getInstance().readEntity("device and addr == \""+addr+"\"");
        if (device == null)
        {
            return ;
        }

        String portName = UtilKt.getPortName(port);
        HashMap<Object,Object> point = CCUHsApi.getInstance().readEntity(
                "point and physical and deviceRef == \""
                        + device.get("id").toString() + "\""+" and (port == \""+port+"\" or port == \""+portName+"\")");
        RawPoint p = new RawPoint.Builder().setHashMap(point).setPointRef(pointRef).build();
        CCUHsApi.getInstance().updatePoint(p,p.getId());
    }

    public static void setPointEnabled(int addr, String port, boolean enabled) {
        CcuLog.d("CCU"," Enabled Physical point for CM "+port+" "+enabled);

        HashMap<Object,Object> device = CCUHsApi.getInstance().readEntity("device and addr == \""+addr+"\"");
        if (device == null)
        {
            return ;
        }
        String portName = UtilKt.getPortName(port);
        HashMap<Object,Object> point = CCUHsApi.getInstance().readEntity(
                "point and physical and deviceRef == \"" + device.get("id").toString() +
                        "\""+" and (port == \""+port+"\" or port == \""+portName+"\")");
        if (point != null && !point.isEmpty())
        {
            RawPoint p = new RawPoint.Builder().setHashMap(point).build();
            p.setEnabled(enabled);
            CCUHsApi.getInstance().updatePoint(p,p.getId());
            CCUHsApi.getInstance().writeHisValById(p.getId(), 0.0);
        }
    }


    public static void updateOnSiteNameChange(){
       CCUHsApi hsApi =  CCUHsApi.getInstance();
       HashMap device = hsApi.readEntity("device and cm");
       if(device == null || device.isEmpty()) return;
       String  deviceId = device.get("id").toString();
       ArrayList<HashMap<Object, Object>> points = hsApi.readAllEntities("point and deviceRef == \"" + deviceId + "\"");
       String siteName = hsApi.readEntity("site").get("dis").toString();

        for (HashMap rawPoint : points) {
            RawPoint.Builder point = new RawPoint.Builder().setHashMap(rawPoint);
            String dis = rawPoint.get("dis").toString();
            String[] tokens = dis.split("-");
            if(tokens.length == 2)  point.setDisplayName(siteName+"-"+tokens[1]);
            if(tokens.length == 3)  point.setDisplayName(siteName+"-"+tokens[1]+"-"+tokens[2]);
            hsApi.updatePoint(point.build(),point.build().getId());
        }

    }

    public static void setCMPointEnabled(String port, boolean enabled) {
        CcuLog.d("CCU"," Enabled Physical point "+port+" "+enabled);

        HashMap<Object,Object> device = CCUHsApi.getInstance().readEntity("device and cm");
        if (device == null)
        {
            return ;
        }

        HashMap<Object,Object> point = CCUHsApi.getInstance().readEntity(
                "point and th1 and physical and deviceRef == \"" + device.get("id").toString() + "\"");
        if (point != null && !point.isEmpty())
        {
            RawPoint p = new RawPoint.Builder().setHashMap(point).build();
            p.setEnabled(enabled);
            CCUHsApi.getInstance().updatePoint(p,p.getId());
            CCUHsApi.getInstance().writeHisValById(p.getId(), 0.0);
        }
    }

    /*Null pointer exception occurs at the time of cut-over migration, because In Domain CM Device
     * is not yet loaded*/
    public static HashMap<String, Boolean> getCMUnusedPorts(CCUHsApi ccuHsApi){
        HashMap<Object, Object> systemEquip = ccuHsApi.readEntity(CommonQueries.SYSTEM_PROFILE);
        HashMap<String, String> cmPortsWithSystemEquipDomainName;
        try {
             cmPortsWithSystemEquipDomainName = getSystemEquipPointsDomainNameWithCmPortsDisName();
        } catch (NullPointerException e){
            CcuLog.e(Domain.LOG_TAG,"Failed to fetch CM Unused ports");
            e.printStackTrace();
            return new HashMap<>();
        }
        ArrayList<HashMap<Object, Object>> systemEquipEnablePoints = ccuHsApi.readAllEntities("point and enable and equipRef == \"" + systemEquip.get("id").toString() + "\"");
        for (HashMap<Object, Object> systemEquipEnablePoint : systemEquipEnablePoints) {
            Object domainName = systemEquipEnablePoint.get(Tags.DOMAIN_NAME);
            if (domainName != null) {
                Object cmPort = cmPortsWithSystemEquipDomainName.get(domainName.toString());
                if (cmPort != null && ccuHsApi.readDefaultValById(systemEquipEnablePoint.get("id").toString()) == 1) {
                    cmPortsWithSystemEquipDomainName.remove(domainName.toString());
                }
            }
        }

        HashMap<String, Boolean> cmUnusedPorts = new HashMap<>();
        HashMap<String, RawPoint> portsList = Domain.cmBoardDevice.getPortsDomainNameWithPhysicalPoint();
        HashMap<String, String> portsNameWithDomainName = getCmPortsDisplayNameWithDomainName();
        for (String value : cmPortsWithSystemEquipDomainName.values()) {
            RawPoint cmPortPoint = portsList.get(portsNameWithDomainName.get(value));
            cmUnusedPorts.put(value, cmPortPoint.getMarkers().contains(Tags.UNUSED));
        }
        CcuLog.i(L.TAG_CCU_DOMAIN, "Got unused ports+ "+cmUnusedPorts);
        return cmUnusedPorts;
    }

    public static HashMap<String, String> getSystemEquipPointsDomainNameWithCmPortsDisName(){
        HashMap<String, String> systemEquipPointsDomainNameWithCmPortsDisName = new HashMap<>();
        HashMap<String, String> cmPortsDisNameWithDomainName = getCmPortsDisplayNameByDomainName();
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay1OutputEnable, cmPortsDisNameWithDomainName.get(DomainName.relay1));
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay2OutputEnable, cmPortsDisNameWithDomainName.get(DomainName.relay2));
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay3OutputEnable, cmPortsDisNameWithDomainName.get(DomainName.relay3));
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay4OutputEnable, cmPortsDisNameWithDomainName.get(DomainName.relay4));
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay5OutputEnable, cmPortsDisNameWithDomainName.get(DomainName.relay5));
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay6OutputEnable, cmPortsDisNameWithDomainName.get(DomainName.relay6));
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay7OutputEnable, cmPortsDisNameWithDomainName.get(DomainName.relay7));
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay8OutputEnable, cmPortsDisNameWithDomainName.get(DomainName.relay8));

        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.analog1OutputEnable, cmPortsDisNameWithDomainName.get(DomainName.analog1Out));
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.analog2OutputEnable, cmPortsDisNameWithDomainName.get(DomainName.analog2Out));
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.analog3OutputEnable, cmPortsDisNameWithDomainName.get(DomainName.analog3Out));
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.analog4OutputEnable, cmPortsDisNameWithDomainName.get(DomainName.analog4Out));
        return systemEquipPointsDomainNameWithCmPortsDisName;
    }
    public static HashMap<String, String> getSystemEquipPointsDomainNameWithCmPortsDomainName(){
        HashMap<String, String> systemEquipPointsDomainNameWithCmPortsDisName = new HashMap<>();
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay1OutputEnable, DomainName.relay1);
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay2OutputEnable, DomainName.relay2);
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay3OutputEnable, DomainName.relay3);
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay4OutputEnable, DomainName.relay4);
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay5OutputEnable, DomainName.relay5);
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay6OutputEnable, DomainName.relay6);
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay7OutputEnable, DomainName.relay7);
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.relay8OutputEnable, DomainName.relay8);

        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.analog1OutputEnable, DomainName.analog1Out);
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.analog2OutputEnable, DomainName.analog2Out);
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.analog3OutputEnable, DomainName.analog3Out);
        systemEquipPointsDomainNameWithCmPortsDisName.put(DomainName.analog4OutputEnable, DomainName.analog4Out);
        return systemEquipPointsDomainNameWithCmPortsDisName;
    }
    public static HashMap<String, String> getCmPortsDisplayNameWithDomainName(){
        HashMap<String, String> cmPortsDisNameWithDomainName = new HashMap<>();
        HashMap<String, String> disNameWithDomainName = getCmPortsDisplayNameByDomainName();
        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.relay1), DomainName.relay1);
        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.relay2), DomainName.relay2);
        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.relay3), DomainName.relay3);
        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.relay4), DomainName.relay4);
        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.relay5), DomainName.relay5);
        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.relay6), DomainName.relay6);
        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.relay7), DomainName.relay7);
        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.relay8), DomainName.relay8);

        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.analog1Out), DomainName.analog1Out);
        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.analog2Out), DomainName.analog2Out);
        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.analog3Out), DomainName.analog3Out);
        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.analog4Out), DomainName.analog4Out);

        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.analog1In), DomainName.analog1In);
        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.analog2In),DomainName.analog2In);

        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.th1In), DomainName.th1In);
        cmPortsDisNameWithDomainName.put(disNameWithDomainName.get(DomainName.th2In), DomainName.th2In);
        return cmPortsDisNameWithDomainName;
    }
    public static HashMap<String, String> getCmPortsDisplayNameByDomainName(){
        HashMap<String, String> cmPortsDisNameWithDomainName = new HashMap<>();
        cmPortsDisNameWithDomainName.put(DomainName.relay1, Domain.cmBoardDevice.getRelay1().readPoint().getDisplayName());
        cmPortsDisNameWithDomainName.put(DomainName.relay2, Domain.cmBoardDevice.getRelay2().readPoint().getDisplayName());
        cmPortsDisNameWithDomainName.put(DomainName.relay3, Domain.cmBoardDevice.getRelay3().readPoint().getDisplayName());
        cmPortsDisNameWithDomainName.put(DomainName.relay4, Domain.cmBoardDevice.getRelay4().readPoint().getDisplayName());
        cmPortsDisNameWithDomainName.put(DomainName.relay5, Domain.cmBoardDevice.getRelay5().readPoint().getDisplayName());
        cmPortsDisNameWithDomainName.put(DomainName.relay6, Domain.cmBoardDevice.getRelay6().readPoint().getDisplayName());
        cmPortsDisNameWithDomainName.put(DomainName.relay7, Domain.cmBoardDevice.getRelay7().readPoint().getDisplayName());
        cmPortsDisNameWithDomainName.put(DomainName.relay8, Domain.cmBoardDevice.getRelay8().readPoint().getDisplayName());

        cmPortsDisNameWithDomainName.put(DomainName.analog1Out, Domain.cmBoardDevice.getAnalog1Out().readPoint().getDisplayName());
        cmPortsDisNameWithDomainName.put(DomainName.analog2Out, Domain.cmBoardDevice.getAnalog2Out().readPoint().getDisplayName());
        cmPortsDisNameWithDomainName.put(DomainName.analog3Out, Domain.cmBoardDevice.getAnalog3Out().readPoint().getDisplayName());
        cmPortsDisNameWithDomainName.put(DomainName.analog4Out, Domain.cmBoardDevice.getAnalog4Out().readPoint().getDisplayName());

        cmPortsDisNameWithDomainName.put(DomainName.analog1In, Domain.cmBoardDevice.getAnalog1In().readPoint().getDisplayName());
        cmPortsDisNameWithDomainName.put(DomainName.analog2In, Domain.cmBoardDevice.getAnalog2In().readPoint().getDisplayName());

        cmPortsDisNameWithDomainName.put(DomainName.th1In, Domain.cmBoardDevice.getTh1In().readPoint().getDisplayName());
        cmPortsDisNameWithDomainName.put(DomainName.th2In, Domain.cmBoardDevice.getTh2In().readPoint().getDisplayName());
        return cmPortsDisNameWithDomainName;
    }

    public static HashMap<String, Boolean> getAllUnusedPorts(){
        CcuLog.i(L.TAG_CCU_DOMAIN, "Getting all unused ports");
        HashMap<String, Boolean> cmdPoints = new HashMap<>(12);
        for (int i = 1; i <= 8; i++) {
            cmdPoints.put("Relay " + i, false);
        }
        for (int i = 1; i <= 4; i++) {
            cmdPoints.put("Analog " + i + " Output", false);
        }
        return cmdPoints;
    }
}
