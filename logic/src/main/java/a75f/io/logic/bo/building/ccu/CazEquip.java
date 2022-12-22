package a75f.io.logic.bo.building.ccu;

import static a75f.io.api.haystack.CCUHsApi.TAG;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Kind;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.heartbeat.HeartBeat;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.sse.SingleStageEquipUtil;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.tuners.TITuners;
import a75f.io.logic.util.RxTask;

/**
 * Created by Anilkumar on 8/19/19.
 */

public class CazEquip
{
    public int nodeAddr;
    ProfileType profileType;
    String equipRef = null;
    CCUHsApi mHayStack = CCUHsApi.getInstance();

    public CazEquip(ProfileType type, int node)
    {
        profileType = type;
        nodeAddr = node;
    }

    public void init() {
        HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \"" + nodeAddr + "\"");

        if (equipMap != null && equipMap.size() > 0)
        {
            equipRef = equipMap.get("id").toString();
        }

    }

    public void createEntities(CazProfileConfig config, String floorRef, String roomRef)
    {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis+"-TI-"+nodeAddr;
        String ahuRef = null;
        HashMap systemEquip = CCUHsApi.getInstance().read("equip and system");
        if (systemEquip != null && systemEquip.size() > 0) {
            ahuRef = systemEquip.get("id").toString();
        }
        Equip.Builder b = new Equip.Builder()
                .setSiteRef(siteRef)
                .setDisplayName(equipDis)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setProfile(profileType.name())
                .setPriority(config.getPriority().name())
                .addMarker("equip").addMarker("ti").addMarker("zone")
                .setAhuRef(ahuRef)
                .setTz(tz)
                .setGroup(String.valueOf(nodeAddr));
        equipRef = CCUHsApi.getInstance().addEquip(b.build());
    
        RxTask.executeAsync(() -> TITuners.addEquipTiTuners( CCUHsApi.getInstance(),
                                                             siteRef,
                                                             siteDis + "-TI-" + nodeAddr,
                                                             equipRef,
                                                             roomRef,
                                                             floorRef,
                                                             tz));
        createCcuConfigPoints(config, equipRef);

        Point currentTemp = new Point.Builder()
                .setDisplayName(siteDis+"-TI-"+nodeAddr+"-currentTemp")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("zone").addMarker("ti")
                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String ctID = CCUHsApi.getInstance().addPoint(currentTemp);

        Point humidity = new Point.Builder()
                .setDisplayName(siteDis+"-CAZ-"+nodeAddr+"-humidity")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("zone").addMarker("ti")
                .addMarker("air").addMarker("humidity").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("%")
                .setTz(tz)
                .build();
        String humidityId = CCUHsApi.getInstance().addPoint(humidity);


        Point desiredTemp = new Point.Builder()
                .setDisplayName(siteDis+"-TI-"+nodeAddr+"-desiredTemp")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("ti").addMarker("average")
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String dtId = CCUHsApi.getInstance().addPoint(desiredTemp);

        Point desiredTempCooling = new Point.Builder()
                .setDisplayName(siteDis+"-TI-"+nodeAddr+"-desiredTempCooling")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("ti").addMarker("cooling")
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(desiredTempCooling);

        Point desiredTempHeating = new Point.Builder()
                .setDisplayName(siteDis+"-TI-"+nodeAddr+"-desiredTempHeating")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("ti").addMarker("heating")
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(desiredTempHeating);

        Point equipStatus = new Point.Builder()
                .setDisplayName(siteDis+"-TI-"+nodeAddr+"-equipStatus")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("status").addMarker("vav").addMarker("his").addMarker("ti").addMarker("logical").addMarker("zone")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("deadband,cooling,heating,tempdead")
                .setTz(tz)
                .build();
        String equipStatusId = CCUHsApi.getInstance().addPoint(equipStatus);
        CCUHsApi.getInstance().writeHisValById(equipStatusId, 0.0);

        Point equipStatusMessage = new Point.Builder()
                .setDisplayName(siteDis+"-TI-"+nodeAddr+"-equipStatusMessage")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("status").addMarker("message").addMarker("ti").addMarker("writable").addMarker("logical").addMarker("zone")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .setKind(Kind.STRING)
                .build();
        String equipStatusMessageLd = CCUHsApi.getInstance().addPoint(equipStatusMessage);
        Point equipScheduleStatus = new Point.Builder()
                .setDisplayName(siteDis+"-TI-"+nodeAddr+"-equipScheduleStatus")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("scheduleStatus").addMarker("logical").addMarker("ti").addMarker("zone").addMarker("writable").addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .setKind(Kind.STRING)
                .build();
        String equipScheduleStatusId = CCUHsApi.getInstance().addPoint(equipScheduleStatus);

        Point equipScheduleType = new Point.Builder()
                .setDisplayName(siteDis+"-TI-"+nodeAddr+"-scheduleType")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("zone").addMarker("ti").addMarker("scheduleType").addMarker("writable").addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("building,zone,named")
                .setTz(tz)
                .build();
        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(equipScheduleType);
        CCUHsApi.getInstance().writeDefaultValById(equipScheduleTypeId, 0.0);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(equipScheduleTypeId, 0.0);

        Point zoneDynamicPriorityPoint = new Point.Builder()
            .setDisplayName(equipDis+"-zoneDynamicPriority")
            .setEquipRef(equipRef)
            .setSiteRef(siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef).setHisInterpolate("cov")
            .addMarker("ti").addMarker("zone").addMarker("dynamic").addMarker("priority").addMarker("writable")
            .addMarker("sp").addMarker("his").addMarker("logical")
            .setGroup(String.valueOf(nodeAddr))
            .setTz(tz)
            .build();
        String zoneDynamicPriorityPointID = CCUHsApi.getInstance().addPoint(zoneDynamicPriorityPoint);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(zoneDynamicPriorityPointID, 10.0);

        Point occupancy = new Point.Builder()
                .setDisplayName(siteDis+"-TI-"+nodeAddr+"-occupancy")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("ti").addMarker("occupancy").addMarker("mode").addMarker("zone").addMarker("writable").addMarker("his")
                .setEnums(Occupancy.getEnumStringDefinition())
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String occupancyId = CCUHsApi.getInstance().addPoint(occupancy);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(occupancyId, 0.0);

        Point point = null;

        String heartBeatId = CCUHsApi.getInstance().addPoint(HeartBeat.getHeartBeatPoint(equipDis, equipRef,
                siteRef, roomRef, floorRef, nodeAddr, "ti", tz, false));

        //4 TODO map system device?
        ControlMote device = new ControlMote(nodeAddr,siteRef, floorRef, roomRef, equipRef);
        if (config.enableThermistor2) {
            if (config.thermistor2association == 0) {
                point = new Point.Builder()
                        .setDisplayName(equipDis+"-external10kTempSensor")
                        .setEquipRef(equipRef)
                        .setSiteRef(siteRef)
                        .setRoomRef(roomRef)
                        .setFloorRef(floorRef).setHisInterpolate("cov")
                        .addMarker("ti").addMarker("temp").addMarker("external").addMarker("cur").addMarker("sensor")
                        .addMarker("logical").addMarker("zone").addMarker("his").addMarker("air")
                        .setGroup(String.valueOf(nodeAddr))
                        .setUnit("\u00B0F")
                        .setTz(tz)
                        .build();
            } else {
                point = new Point.Builder()
                        .setDisplayName(equipDis+"-supplyAirTemperature")
                        .setEquipRef(equipRef)
                        .setSiteRef(siteRef)
                        .setRoomRef(roomRef)
                        .setFloorRef(floorRef).setHisInterpolate("cov")
                        .addMarker("ti").addMarker("temp").addMarker("supply").addMarker("cur").addMarker("sensor")
                        .addMarker("logical").addMarker("zone").addMarker("his").addMarker("air").addMarker("discharge")
                        .setGroup(String.valueOf(nodeAddr))
                        .setUnit("\u00B0F")
                        .setTz(tz)
                        .build();

            }
            String ID = CCUHsApi.getInstance().addPoint(point);
            CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(ID,0.0);
            device.th2In.setPointRef(ID);
            device.th2In.setEnabled(true);
            device.th2In.setType(String.valueOf(config.thermistor2association));
        }
        device.currentTemp.setPointRef(ctID);
        device.rssi.setPointRef(heartBeatId);
        device.rssi.setEnabled(true);


        device.addSensor(Port.SENSOR_RH, humidityId);

        device.addPointsToDb();


        setCurrentTemp(0);
        setDesiredTempCooling(74.0);
        setDesiredTemp(72.0);
        setDesiredTempHeating(70.0);
        setDesiredTempHeating(70.0);
        setHumidity(0);

        CCUHsApi.getInstance().syncEntityTree();
        updateCurrentTemp(config.isEnableMain,config.enableThermistor1,config.enableThermistor2);
        CCUHsApi.getInstance().scheduleSync();
    }

    String getId(){
        return equipRef;
    }

    public void createCcuConfigPoints(CazProfileConfig config, String equipRef) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis+"-TI-"+nodeAddr;
        String tz = siteMap.get("tz").toString();


        Point zonePriority = new Point.Builder()
                .setDisplayName(equipDis+"-zonePriority")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setHisInterpolate("cov")
                .addMarker("config").addMarker("ti").addMarker("writable").addMarker("zone")
                .addMarker("priority").addMarker("sp").addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("none,low,normal,high")
                .setTz(tz)
                .build();
        String zonePriorityId = CCUHsApi.getInstance().addPoint(zonePriority);
        CCUHsApi.getInstance().writeDefaultValById(zonePriorityId, (double)config.getPriority().ordinal());
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(zonePriorityId, (double)config.getPriority().ordinal());

        Point temperatureOffset = new Point.Builder()
                .setDisplayName(equipDis+"-temperatureOffset")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .addMarker("config").addMarker("ti").addMarker("writable").addMarker("zone")
                .addMarker("temperature").addMarker("offset").addMarker("sp")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String temperatureOffsetId = CCUHsApi.getInstance().addPoint(temperatureOffset);
        CCUHsApi.getInstance().writeDefaultValById(temperatureOffsetId, (double)config.temperaturOffset);

        Point mainSensor = new Point.Builder()
                .setDisplayName(equipDis+"-mainTemperatureSensor")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .addMarker("config").addMarker("ti").addMarker("writable").addMarker("zone")
                .addMarker("main").addMarker("current").addMarker("temperature").addMarker("sp")
                .addMarker("enable")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String mainSensorId = CCUHsApi.getInstance().addPoint(mainSensor);
        CCUHsApi.getInstance().writeDefaultValById(mainSensorId, config.isEnableMain ? 1.0:0.0);

        Point th1Config = new Point.Builder()
                .setDisplayName(equipDis+"-th1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .addMarker("config").addMarker("ti").addMarker("writable").addMarker("zone")
                .addMarker("th1").addMarker("sp").addMarker("enable")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String th1ConfigId = CCUHsApi.getInstance().addPoint(th1Config);
        CCUHsApi.getInstance().writeDefaultValById(th1ConfigId, config.enableThermistor1 ? 1.0:0.0);

        Point th2Config = new Point.Builder()
                .setDisplayName(equipDis+"-th2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .addMarker("config").addMarker("ti").addMarker("writable").addMarker("zone")
                .addMarker("th2").addMarker("sp").addMarker("enable")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String th2ConfigId =CCUHsApi.getInstance().addPoint(th2Config);
        CCUHsApi.getInstance().writeDefaultValById(th2ConfigId, config.enableThermistor2 ? 1.0:0.0);

        Point thermistor2association = new Point.Builder()
                .setDisplayName(equipDis+"-thermistor2association")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .addMarker("config").addMarker("ti").addMarker("writable").addMarker("zone")
                .addMarker("th2").addMarker("sp").addMarker("association")
                .setGroup(String.valueOf(nodeAddr)).setEnums("external10kTempSensor, supplyAirTemperature")
                .setTz(tz)
                .build();
        String thermistor2associationId =CCUHsApi.getInstance().addPoint(thermistor2association);
        CCUHsApi.getInstance().writeDefaultValById(thermistor2associationId, Double.valueOf(config.thermistor2association));
    }

    public void updateCurrentTemp(boolean isMain,boolean isEnableth1, boolean isEnableth2) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<Object,Object> device = hayStack.readEntity("cm and device ");
        HashMap<Object,Object> currentTemp = hayStack.readEntity("point and current and " +
                "temp and equipRef == \""+equipRef+"\"");
        if (!device.isEmpty() && !currentTemp.isEmpty()) {
            if (isEnableth1) {
                ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name(), true);
                ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name(), false);
                ControlMote.setPointEnabled(nodeAddr,Port.SENSOR_RT.name(), false);
                ControlMote.updatePhysicalPointRef(nodeAddr, Port.TH1_IN.name(), currentTemp.get(
                        "id").toString());
            } else if (isEnableth2) {
                ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name(), false);
                ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name(), true);
                ControlMote.setPointEnabled(nodeAddr,Port.SENSOR_RT.name(), false);
                ControlMote.updatePhysicalPointRef(nodeAddr, Port.TH2_IN.name(), currentTemp.get(
                        "id").toString());
                mapPhysicalToLogicalPoint();
            } else if (isMain) {
                ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name(), false);
                ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name(), false);
                ControlMote.updatePhysicalPointRef(nodeAddr, Port.SENSOR_RT.name(), currentTemp.get("id").toString());
            }
        }
    }

    private void mapPhysicalToLogicalPoint() {

        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<Object,Object> th2Point;
        if (getProfileConfiguration().thermistor2association == 0) {
            th2Point = hayStack.readEntity("point and external and " +
                    "temp and equipRef == \""+equipRef+"\"");
        } else {
            th2Point = hayStack.readEntity("point and supply and " +
                    "temp and equipRef == \""+equipRef+"\"");
        }
        if (!th2Point.isEmpty()) {
            ControlMote.updatePhysicalPointRef(nodeAddr, Port.TH2_IN.name(), th2Point.get(
                    "id").toString());
        }

    }

    public CazProfileConfig getProfileConfiguration() {
        CazProfileConfig config = new CazProfileConfig();
        config.setPriority(ZonePriority.values()[(int)getZonePriorityValue()]);
        config.temperaturOffset = getConfigNumVal("temperature and offset");
        config.isEnableMain = getConfigNumVal("main and current and temperature and enable") > 0;
        config.enableThermistor1 = getConfigNumVal("th1 and enable") > 0;
        config.enableThermistor2 = getConfigNumVal("th2 and enable") > 0;
        config.thermistor2association = (int) getConfigNumVal("association and th2");
        return config;
    }

    public void updateCcuAsZoneConfig(CazProfileConfig config, String floorRef, String zoneRef) {

        ControlMote.setPointEnabled(nodeAddr, Port.TH2_IN.name(), config.enableThermistor2);
        ControlMote.setPointEnabled(nodeAddr, Port.TH1_IN.name(), config.enableThermistor1);

        if ((config.enableThermistor2) && config.thermistor2association != getConfigNumVal("association and th2")) {
            HashMap thermistor2associationPoint =
                    CCUHsApi.getInstance().read("point and association and th2 and group == \"" + nodeAddr + "\"");
            Point thermistor2association = new Point.Builder().setHashMap(thermistor2associationPoint).build();
            CazEquipUtil.updateThermistor2Association(config.thermistor2association, thermistor2association);
        }
        setConfigNumVal("priority",config.getPriority().ordinal());
        setHisVal("priority",config.getPriority().ordinal());
        setConfigNumVal("temperature and offset",config.temperaturOffset);
        setConfigNumVal("enable and th2",config.enableThermistor2 == true ? 1.0 : 0);
        setConfigNumVal("enable and main and current and temperature",config.isEnableMain ? 1.0 : 0);
        setConfigNumVal("enable and th1",config.enableThermistor1 ? 1.0 : 0);
        setConfigNumVal("enable and th2",config.enableThermistor2 ? 1.0 : 0);
        setConfigNumVal("association and th2",Double.parseDouble(String.valueOf(config.thermistor2association)));
        updateCurrentTemp(config.isEnableMain,config.enableThermistor1,config.enableThermistor2);

    }

    public void setConfigNumVal(String tags,double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and zone and config and ti and "+tags+" and group == \""+nodeAddr+"\"", val);
    }

    public double getConfigNumVal(String tags) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and ti and "+tags+" and group == \""+nodeAddr+"\"");
    }

    public void setHisVal(String tags,double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and zone and config and ti and "+tags+" and group == \""+nodeAddr+"\"", val);
    }

    public double getCurrentTemp()
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and group == \""+nodeAddr+"\"");
    }
    public void setCurrentTemp(double roomTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and current and group == \""+nodeAddr+"\"", roomTemp);
    }

    public double getHumidity()
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and humidity and sensor and current and group == \""+nodeAddr+"\"");
    }
    public void setHumidity(double humidity)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and humidity and sensor and current and group == \""+nodeAddr+"\"", humidity);
    }

    public double getDesiredTemp()
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and average and sp and group == \"" + nodeAddr + "\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        return CCUHsApi.getInstance().readDefaultValById(id);
    }
    public void setDesiredTemp(double desiredTemp)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and average and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }

    public double getDesiredTempCooling()
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and cooling and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
    public void setDesiredTempCooling(double desiredTemp)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and cooling and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        //CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make(desiredTemp), HNum.make(0));
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }

    public double getDesiredTempHeating()
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and heating and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }

    public void setDesiredTempHeating(double desiredTemp)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and heating and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        //CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make(desiredTemp), HNum.make(0));
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }

    public double getStatus() {
        return CCUHsApi.getInstance().readHisValByQuery("point and status and his and group == \""+nodeAddr+"\"");
    }

    public void setStatus(double status, boolean emergency) {
        if (getStatus() != status )
        {
            CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + nodeAddr + "\"", status);
        }

        String message;
        if (emergency) {
            message = (status == 0 ? "Recirculating Air" : status == 1 ? "Emergency Cooling" : "Emergency Heating");
        } else
        {
            if (ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.PRECONDITIONING) {
                message = "In Preconditioning ";
            } else
            {
                message = (status == 0 ? "Recirculating Air" : status == 1 ? "Cooling Space" : "Warming Space");
            }
        }

        String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \""+nodeAddr+"\"");
        if (!curStatus.equals(message))
        {
            CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + nodeAddr + "\"", message);
        }
    }

    public void setScheduleStatus(String status)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and scheduleStatus and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, status);
    }
    public double getZonePriorityValue(){
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+nodeAddr+"\"");
        return CCUHsApi.getInstance().readPointPriorityValByQuery("zone and priority and config and equipRef == \""+equip.get("id")+"\"");
    }
}

