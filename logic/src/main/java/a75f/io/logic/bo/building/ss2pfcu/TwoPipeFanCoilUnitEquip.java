package a75f.io.logic.bo.building.ss2pfcu;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Kind;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.heartbeat.HeartBeat;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.hvac.StandaloneFanStage;
import a75f.io.logic.bo.haystack.device.SmartStat;
import a75f.io.logic.tuners.StandAloneTuners;
import a75f.io.logic.tuners.TunerConstants;

public class TwoPipeFanCoilUnitEquip {
    int nodeAddr;
    ProfileType profileType;
    TwoPipeFanCoilUnitProfile ss2PfcUnit;

    double      currentTemp;
    double      humidity;
    double desiredTemp;
    double supplyWaterTemp;
    double dischargeTemp;
    long waterValveLastOnTime;
    long waterValvePeriodicTimer;
    public TwoPipeFanCoilUnitEquip(ProfileType T, int node) {

        profileType = T;
        ss2PfcUnit = new TwoPipeFanCoilUnitProfile();
        nodeAddr = node;
        waterValveLastOnTime = 0;
        waterValvePeriodicTimer = 0;
    }


    public void createHaystackPoints(TwoPipeFanCoilUnitConfiguration config, String floor, String room) {

        //Create Logical points
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis+"-2PFCU-"+nodeAddr;
        String profile = "pipe2";
        String gatewayRef = null;
        HashMap systemEquip = CCUHsApi.getInstance().read("equip and system");
        if (systemEquip != null && systemEquip.size() > 0) {
            gatewayRef = systemEquip.get("id").toString();
        }
        Equip.Builder b = new Equip.Builder()
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .setProfile(profileType.name())
                .setPriority(config.getPriority().name())
                .addMarker("equip").addMarker("standalone").addMarker("smartstat").addMarker("zone").addMarker("fcu")
                .setGatewayRef(gatewayRef)
                .setTz(tz)
                .setGroup(String.valueOf(nodeAddr));
        b.setDisplayName(equipDis);
        b.addMarker(profile);

        String equipRef = CCUHsApi.getInstance().addEquip(b.build());

        StandAloneTuners.addEquipStandaloneTuners(CCUHsApi.getInstance(), siteRef,siteDis+"-2PFCU-"+nodeAddr, equipRef
                                                                                        , room, floor, tz);
        StandAloneTuners.addTwoPipeFanEquipStandaloneTuners(CCUHsApi.getInstance(), siteRef, siteDis + "-2PFCU-" + nodeAddr,
                                                            equipRef, room, floor, tz);

        createTwoPipeConfigPoints(config, equipRef,floor,room);

        addProfilePoints(siteRef,equipRef,equipDis,tz,floor,room);
        Point currentTemp = new Point.Builder()
                .setDisplayName(equipDis+"-currentTemp")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("fcu").addMarker("cur")
                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String ctID = CCUHsApi.getInstance().addPoint(currentTemp);
        CCUHsApi.getInstance().writeHisValById(ctID, 0.0);

        Point humidity = new Point.Builder()
                .setDisplayName(equipDis+"-humidity")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("fcu").addMarker("cur")
                .addMarker("air").addMarker("humidity").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("%")
                .setTz(tz)
                .build();
        String humidityId = CCUHsApi.getInstance().addPoint(humidity);
        CCUHsApi.getInstance().writeHisValById(humidityId, 0.0);

        Point co2 = new Point.Builder()
                .setDisplayName(equipDis+"-co2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("fcu").addMarker("cur")
                .addMarker("air").addMarker("co2").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String co2Id = CCUHsApi.getInstance().addPoint(co2);
        CCUHsApi.getInstance().writeHisValById(co2Id, 0.0);

        Point voc = new Point.Builder()
                .setDisplayName(equipDis+"-voc")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("fcu").addMarker("cur")
                .addMarker("air").addMarker("voc").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("ppb")
                .setTz(tz)
                .build();
        String vocId = CCUHsApi.getInstance().addPoint(voc);
        CCUHsApi.getInstance().writeHisValById(vocId, 0.0);

        Point sound = new Point.Builder()
                .setDisplayName(equipDis+"-sound")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("fcu").addMarker("cur")
                .addMarker("air").addMarker("sound").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("dB")
                .setTz(tz)
                .build();
        String soundId = CCUHsApi.getInstance().addPoint(sound);

        Point uvi = new Point.Builder()
                .setDisplayName(equipDis+"-uvi")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("fcu").addMarker("cur")
                .addMarker("air").addMarker("uvi").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String uviId = CCUHsApi.getInstance().addPoint(uvi);

        Point co = new Point.Builder()
                .setDisplayName(equipDis+"-"+Port.SENSOR_CO.name())
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("fcu").addMarker("cur")
                .addMarker("air").addMarker("co").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String coId = CCUHsApi.getInstance().addPoint(co);

        Point co2Eq = new Point.Builder()
                .setDisplayName(equipDis+"-"+Port.SENSOR_CO2_EQUIVALENT.name())
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("fcu").addMarker("cur")
                .addMarker("air").addMarker("co2Equivalent").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String co2EqId = CCUHsApi.getInstance().addPoint(co2Eq);

        Point no2 = new Point.Builder()
                .setDisplayName(equipDis+"-"+Port.SENSOR_NO.name())
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("fcu").addMarker("cur")
                .addMarker("air").addMarker("no").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String no2Id = CCUHsApi.getInstance().addPoint(no2);

        Point ps = new Point.Builder()
                .setDisplayName(equipDis+"-"+Port.SENSOR_PRESSURE.name())
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("fcu").addMarker("cur")
                .addMarker("air").addMarker("pressure").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("inch wc")
                .setTz(tz)
                .build();
        String psId = CCUHsApi.getInstance().addPoint(ps);

        Point illu = new Point.Builder()
                .setDisplayName(equipDis+"-"+Port.SENSOR_ILLUMINANCE.name())
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("fcu").addMarker("cur")
                .addMarker("air").addMarker("illuminance").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("lux")
                .setTz(tz)
                .build();
        String illuId = CCUHsApi.getInstance().addPoint(illu);

        Point occSensing = new Point.Builder()
                .setDisplayName(equipDis+"-occupancySensor")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("fcu").addMarker("occupancy").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical")
                .setEnums("off,on")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String occSensingId = CCUHsApi.getInstance().addPoint(occSensing);
        Point desiredTemp = new Point.Builder()
                .setDisplayName(equipDis+"-desiredTemp")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("standalone").addMarker("average")
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent").addMarker(profile).addMarker("fcu")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String dtId = CCUHsApi.getInstance().addPoint(desiredTemp);

        Point desiredTempCooling = new Point.Builder()
                .setDisplayName(equipDis+"-desiredTempCooling")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("standalone").addMarker("cooling")
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent").addMarker(profile).addMarker("fcu")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(desiredTempCooling);

        Point desiredTempHeating = new Point.Builder()
                .setDisplayName(equipDis+"-desiredTempHeating")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("standalone").addMarker("heating")
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent").addMarker(profile).addMarker("fcu")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(desiredTempHeating);

        Point datPoint = new Point.Builder()
                .setDisplayName(equipDis+"-airflowTempSensorTh1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("standalone").addMarker(profile).addMarker("fcu").addMarker("cur").addMarker("discharge")
                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("th1").addMarker("his").addMarker("logical").addMarker("zone")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();

        String datID = CCUHsApi.getInstance().addPoint(datPoint);
        CCUHsApi.getInstance().writeHisValById(datID, 0.0);

        Point eatPoint = new Point.Builder()
                .setDisplayName(equipDis+"-supplyWaterTempSensorTh2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("standalone").addMarker(profile).addMarker("fcu").addMarker("cur")
                .addMarker("supply").addMarker("water").addMarker("temp").addMarker("th2").addMarker("sensor").addMarker("his").addMarker("logical").addMarker("zone")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String eatID = CCUHsApi.getInstance().addPoint(eatPoint);
        CCUHsApi.getInstance().writeHisValById(eatID, 0.0);

        Point fanMedium = new Point.Builder()
                .setDisplayName(equipDis+"-fanMedium")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("fan").addMarker("medium").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("fcu").addMarker("cmd").addMarker("runtime")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r1ID = CCUHsApi.getInstance().addPoint(fanMedium);

        Point fanHigh = new Point.Builder()
                .setDisplayName(equipDis+"-fanHigh")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("fan").addMarker("high").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("fcu").addMarker("cmd").addMarker("runtime")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r2ID = CCUHsApi.getInstance().addPoint(fanHigh);

        Point heatingStage1 = new Point.Builder()
                .setDisplayName(equipDis+"-auxHeating")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("aux").addMarker("heating").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("fcu").addMarker("cmd").addMarker("runtime")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r4ID = CCUHsApi.getInstance().addPoint(heatingStage1);
        CCUHsApi.getInstance().writeHisValById(r4ID, 0.0);

        Point waterValve = new Point.Builder()
                .setDisplayName(equipDis+"-waterValve")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("water").addMarker("valve").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("fcu").addMarker("cmd").addMarker("runtime")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r6ID = CCUHsApi.getInstance().addPoint(waterValve);
        CCUHsApi.getInstance().writeHisValById(r6ID, 0.0);

        Point fanStage1 = new Point.Builder()
                .setDisplayName(equipDis+"-fanLow")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("fan").addMarker("low").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("fcu").addMarker("cmd").addMarker("runtime")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r3ID = CCUHsApi.getInstance().addPoint(fanStage1);
        Point r5NotUsed = new Point.Builder()
                .setDisplayName(equipDis+"-relay5NotUsed")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("standalone").addMarker("zone").addMarker("logical").addMarker(profile).addMarker("fcu").addMarker("cmd")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r5ID = CCUHsApi.getInstance().addPoint(r5NotUsed);
        Point equipStatus = new Point.Builder()
                .setDisplayName(equipDis+"-equipStatus")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("status").addMarker("his").addMarker("zone").addMarker(profile).addMarker("fcu")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("deadband,cooling,heating,tempdead")
                .setTz(tz)
                .build();
        String equipStatusId = CCUHsApi.getInstance().addPoint(equipStatus);
        CCUHsApi.getInstance().writeHisValById(equipStatusId, 0.0);

        Point equipStatusMessage = new Point.Builder()
                .setDisplayName(equipDis+"-equipStatusMessage")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("status").addMarker("message").addMarker(profile).addMarker("fcu").addMarker("writable").addMarker("zone")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .setKind(Kind.STRING)
                .build();
        String equipStatusMessageLd = CCUHsApi.getInstance().addPoint(equipStatusMessage);
        Point equipScheduleStatus = new Point.Builder()
                .setDisplayName(equipDis+"-equipScheduleStatus")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("scheduleStatus").addMarker(profile).addMarker("fcu").addMarker("logical").addMarker("zone").addMarker("writable").addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .setKind(Kind.STRING)
                .build();
        String equipScheduleStatusId = CCUHsApi.getInstance().addPoint(equipScheduleStatus);

        Point equipScheduleType = new Point.Builder()
                .setDisplayName(equipDis+"-scheduleType")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("scheduleType").addMarker("writable").addMarker("zone").addMarker("his").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("building,zone,named")
                .setTz(tz)
                .build();
        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(equipScheduleType);
        String heartBeatId = CCUHsApi.getInstance().addPoint(HeartBeat.getHeartBeatPoint(equipDis, equipRef,
                siteRef, room, floor, nodeAddr, profile, tz ,"2fcu"));
        //TODO, what if already equip exists in a zone and its schedule is zone or named? Kumar
        CCUHsApi.getInstance().writeDefaultValById(equipScheduleTypeId, 0.0);
        CCUHsApi.getInstance().writeHisValById(equipScheduleTypeId, 0.0);


        //Create Physical points and map
        SmartStat device = new SmartStat(nodeAddr, siteRef, floor, room,equipRef,profile);
        
        device.th1In.setPointRef(datID);
        device.th1In.setEnabled(config.enableThermistor1);
        device.th2In.setPointRef(eatID);
        device.th2In.setEnabled(true);
        device.currentTemp.setPointRef(ctID);
        device.currentTemp.setEnabled(true);
        device.rssi.setPointRef(heartBeatId);
        device.rssi.setEnabled(true);
        device.addSensor(Port.SENSOR_RH,humidityId);
        device.addSensor(Port.SENSOR_CO2,co2Id);
        device.addSensor(Port.SENSOR_VOC,vocId);
        device.addSensor(Port.SENSOR_SOUND,soundId);
        device.addSensor(Port.SENSOR_UVI,uviId);
        device.addSensor(Port.SENSOR_CO,coId);
        device.addSensor(Port.SENSOR_CO2_EQUIVALENT,co2EqId);
        device.addSensor(Port.SENSOR_NO,no2Id);
        device.addSensor(Port.SENSOR_ILLUMINANCE,illuId);
        device.addSensor(Port.SENSOR_PRESSURE,psId);
        device.addSensor(Port.SENSOR_OCCUPANCY,occSensingId);
        device.desiredTemp.setPointRef(dtId);
        device.desiredTemp.setEnabled(true);
        device.relay1.setPointRef(r1ID);
        device.relay2.setPointRef(r2ID);
        device.relay3.setPointRef(r3ID);
        device.relay4.setPointRef(r4ID);
        device.relay5.setPointRef(r5ID);
        device.relay6.setPointRef(r6ID);
        device.relay1.setEnabled(config.isOpConfigured(Port.RELAY_ONE));
        device.relay2.setEnabled(config.isOpConfigured(Port.RELAY_TWO));
        device.relay3.setEnabled(config.isOpConfigured(Port.RELAY_THREE));
        device.relay4.setEnabled(config.isOpConfigured(Port.RELAY_FOUR));
        device.relay5.setEnabled(false);
        device.relay6.setEnabled(config.isOpConfigured(Port.RELAY_SIX));

        device.addPointsToDb();
//initialize with default values if schedule fetch is null
        double coolingVal = 74.0;
        double heatingVal = 70.0;
        double defaultDesiredTemp = 72;
        setCurrentTemp(0);
        setHumidity(0);
        setCO2(0);
        setVOC(0);
        setScheduleStatus("");
        setSmartStatStatus("OFF"); //Intialize with off
        Schedule schedule = Schedule.getScheduleByEquipId(equipRef);
        Occupied curOccupied = schedule.getCurrentValues();
        if(schedule != null &&  curOccupied != null) {
            defaultDesiredTemp = (curOccupied.getCoolingVal() + curOccupied.getHeatingVal()) / 2.0;
            coolingVal = curOccupied.getCoolingVal();
            heatingVal = curOccupied.getHeatingVal();
        }

        setDesiredTempCooling(coolingVal);
        setDesiredTemp(defaultDesiredTemp);
        setDesiredTempHeating(heatingVal);
        CCUHsApi.getInstance().syncEntityTree();


    }
    private void addProfilePoints(String siteRef, String equipref, String equipDis, String tz,String floor, String room) {
        Point twoPfcuOccupancy = new Point.Builder()
                .setDisplayName(equipDis + "-" + "occupancy")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("occupancy").addMarker("mode").addMarker("his").addMarker("sp").addMarker("zone").addMarker("pipe2").addMarker("fcu")
                .setEnums("unoccupied,occupied,preconditioning,forcedoccupied,vacation,occupancysensing")
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(twoPfcuOccupancy);

        Point twoPfcuConditioningMode = new Point.Builder()
                .setDisplayName(equipDis + "-" + "OperatingMode")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .setEnums("off,cooling,heating,tempdead")
                .addMarker("standalone").addMarker("temp").addMarker("operating").addMarker("mode").addMarker("his").addMarker("sp").addMarker("zone").addMarker("pipe2").addMarker("fcu")
                .setTz(tz)
                .build();
        String condModeId = CCUHsApi.getInstance().addPoint(twoPfcuConditioningMode);
        CCUHsApi.getInstance().writeHisValById(condModeId, 0.0);


    }

    public void setProfilePoint(String tags, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and zone and standalone and his and "+tags+" and group == \""+nodeAddr+"\"", val);
    }
    public void createTwoPipeConfigPoints(TwoPipeFanCoilUnitConfiguration config, String equipRef, String floor, String room) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis+"-2PFCU-"+nodeAddr;
        String tz = siteMap.get("tz").toString();
        String profile = "pipe2";
        Point enableOccupancyControl = new Point.Builder()
                .setDisplayName(equipDis + "-enableOccupancyControl")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("enable").addMarker("occupancy").addMarker("control").addMarker("sp").addMarker(profile).addMarker("fcu")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String enableOccupancyControlId = CCUHsApi.getInstance().addPoint(enableOccupancyControl);
        CCUHsApi.getInstance().writeDefaultValById(enableOccupancyControlId, config.enableOccupancyControl == true ? 1.0 : 0);

        if(config.enableOccupancyControl){

            Point occupancyDetection = new Point.Builder()
                    .setDisplayName(equipDis+"-occupancyDetection")
                    .setEquipRef(equipRef)
                    .setSiteRef(siteRef)
                    .setRoomRef(room)
                    .setFloorRef(floor).setHisInterpolate("cov")
                    .addMarker("occupancy").addMarker("detection").addMarker("fcu").addMarker(profile).addMarker("his").addMarker("zone")
                    .setGroup(String.valueOf(nodeAddr))
                    .setEnums("false,true")
                    .setTz(tz)
                    .build();
            String occupancyDetectionId = CCUHsApi.getInstance().addPoint(occupancyDetection);
            CCUHsApi.getInstance().writeHisValById(occupancyDetectionId, 0.0);
        }
        Point temperatureOffset = new Point.Builder()
                .setDisplayName(equipDis+"-temperatureOffset")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("temperature").addMarker("offset").addMarker("sp").addMarker(profile).addMarker("fcu")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String temperatureOffsetId = CCUHsApi.getInstance().addPoint(temperatureOffset);
        CCUHsApi.getInstance().writeDefaultValById(temperatureOffsetId, (double)config.temperatureOffset);

        Point airflowTh1 = new Point.Builder()
                .setDisplayName(equipDis+"-enableAirflowTh1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone").addMarker("sp").addMarker("enable").addMarker(profile).addMarker("fcu").addMarker("th1")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableAirflowId = CCUHsApi.getInstance().addPoint(airflowTh1);
        CCUHsApi.getInstance().writeDefaultValById(enableAirflowId, (double)(config.enableThermistor1 == true? 1.0 : 0));

        Point external10KProbeTh2 = new Point.Builder()
                .setDisplayName(equipDis+"-enableSupplyWaterTempTh2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone").addMarker("th2").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableexternal10KProbeTh2Id = CCUHsApi.getInstance().addPoint(external10KProbeTh2);
        CCUHsApi.getInstance().writeDefaultValById(enableexternal10KProbeTh2Id, 1.0);

        Point enableRelay1 = new Point.Builder()
                .setDisplayName(equipDis+"-enableFanMedium")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay1").addMarker("sp").addMarker("enable").addMarker(profile).addMarker("fcu")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String enableRelay1Id = CCUHsApi.getInstance().addPoint(enableRelay1);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay1Id, (double)(config.enableRelay1 == true? 1.0 : 0));

        Point enableRelay2 = new Point.Builder()
                .setDisplayName(equipDis+"-enableFanHigh")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay2").addMarker("sp").addMarker("enable").addMarker(profile).addMarker("fcu")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String enableRelay2Id = CCUHsApi.getInstance().addPoint(enableRelay2);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay2Id, (double)(config.enableRelay2 == true? 1.0 : 0));

        Point enableRelay3 = new Point.Builder()
                .setDisplayName(equipDis+"-enableFanLow")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay3").addMarker("sp").addMarker("enable").addMarker(profile).addMarker("fcu")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String enableRelay3Id = CCUHsApi.getInstance().addPoint(enableRelay3);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay3Id, (double)(config.enableRelay3 == true? 1.0 : 0));

        Point enableRelay4 = new Point.Builder()
                .setDisplayName(equipDis+"-enableAuxHeating")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay4").addMarker("sp").addMarker("enable").addMarker(profile).addMarker("fcu")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String enableRelay4Id = CCUHsApi.getInstance().addPoint(enableRelay4);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay4Id, (double)(config.enableRelay4 == true? 1.0 : 0));
        Point enableRelay5 = new Point.Builder()
                .setDisplayName(equipDis+"-enableRelay5")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay5").addMarker("sp").addMarker("enable").addMarker(profile).addMarker("fcu")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String enableRelay5Id = CCUHsApi.getInstance().addPoint(enableRelay5);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay5Id, 0.0);
        Point enableRelay6 = new Point.Builder()
                .setDisplayName(equipDis+"-enableWaterValve")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay6").addMarker("sp").addMarker("enable").addMarker(profile).addMarker("fcu")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String enableRelay6Id = CCUHsApi.getInstance().addPoint(enableRelay6);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay6Id, (double)(config.enableRelay6 == true? 1.0 : 0));


        addUserIntentPoints(equipRef,equipDis,room,floor, config);

        setConfigNumVal("enable and relay1",config.enableRelay1 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay2",config.enableRelay2 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay3",config.enableRelay3 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay4",config.enableRelay4 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay5",0);
        setConfigNumVal("enable and relay6",config.enableRelay6 == true ? 1.0 : 0);
        setConfigNumVal("enable and occupancy",config.enableOccupancyControl == true ? 1.0 : 0);
        setConfigNumVal("temperature and offset",config.temperatureOffset);
        setConfigNumVal("enable and th1",config.enableThermistor1 == true ? 1.0 : 0);
        setConfigNumVal("enable and th2",1.0);

    }

    public void updateHaystackPoints(TwoPipeFanCoilUnitConfiguration config) {
        for (Output op : config.getOutputs()) {
            switch (op.getPort()) {
                case RELAY_ONE:
                case RELAY_TWO:
                case RELAY_THREE:
                case RELAY_FOUR:
                case RELAY_FIVE:
                case RELAY_SIX:
                    SmartStat.updatePhysicalPointType(nodeAddr, op.getPort().toString(), op.getRelayActuatorType());
                    break;
            }
        }
        SmartStat.setPointEnabled(nodeAddr, Port.RELAY_ONE.name(), config.isOpConfigured(Port.RELAY_ONE) );
        SmartStat.setPointEnabled(nodeAddr, Port.RELAY_TWO.name(), config.isOpConfigured(Port.RELAY_TWO) );
        SmartStat.setPointEnabled(nodeAddr, Port.RELAY_THREE.name(), config.isOpConfigured(Port.RELAY_THREE) );
        SmartStat.setPointEnabled(nodeAddr, Port.RELAY_FOUR.name(), config.isOpConfigured(Port.RELAY_FOUR) );
        SmartStat.setPointEnabled(nodeAddr, Port.RELAY_FIVE.name(), config.isOpConfigured(Port.RELAY_FIVE) );
        SmartStat.setPointEnabled(nodeAddr, Port.RELAY_SIX.name(), config.isOpConfigured(Port.RELAY_SIX) );
        SmartStat.setPointEnabled(nodeAddr, Port.TH1_IN.name(),config.enableThermistor1);
        SmartStat.setPointEnabled(nodeAddr, Port.TH2_IN.name(), config.enableThermistor2);


        HashMap equipHash = CCUHsApi.getInstance().read("equip and group == \"" + config.getNodeAddress() + "\"");
        Equip equip = new Equip.Builder().setHashMap(equipHash).build();
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-HPU-" + nodeAddr;
        if(config.enableOccupancyControl){
            Point occupancyDetection = new Point.Builder()
                    .setDisplayName(equipDis+"-occupancyDetection")
                    .setEquipRef(equip.getId())
                    .setSiteRef(siteRef)
                    .setRoomRef(equip.getRoomRef())
                    .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                    .addMarker("occupancy").addMarker("detection").addMarker("fcu").addMarker("pipe2").addMarker("his").addMarker("zone")
                    .setGroup(String.valueOf(nodeAddr))
                    .setEnums("false,true")
                    .setTz(tz)
                    .build();
            String occupancyDetectionId = CCUHsApi.getInstance().addPoint(occupancyDetection);
            CCUHsApi.getInstance().writeHisValById(occupancyDetectionId, 0.0);
        }else {
            HashMap occDetPoint = CCUHsApi.getInstance().read("point and occupancy and detection and fcu and pipe2 and his and equipRef== \"" + equip.getId() + "\"");
            if ((occDetPoint != null) && (occDetPoint.size() > 0))
                CCUHsApi.getInstance().deleteEntityTree(occDetPoint.get("id").toString());
        }
        setConfigNumVal("enable and relay1",config.enableRelay1 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay2",config.enableRelay2 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay3",config.enableRelay3 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay4",config.enableRelay4 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay5",0);
        setConfigNumVal("enable and relay6",config.enableRelay6 == true ? 1.0 : 0);
        setConfigNumVal("enable and occupancy",config.enableOccupancyControl == true ? 1.0 : 0);
        setConfigNumVal("temperature and offset",config.temperatureOffset);
        setConfigNumVal("enable and th1",config.enableThermistor1 == true ? 1.0 : 0);
        setConfigNumVal("enable and th2",1.0);
        
        updateUserIntentPoints(config, equip.getId());
    }



    public TwoPipeFanCoilUnitConfiguration getProfileConfiguration() {
        TwoPipeFanCoilUnitConfiguration config = new TwoPipeFanCoilUnitConfiguration();
        config.enableOccupancyControl = getConfigNumVal("enable and occupancy") > 0 ? true : false ;
        config.temperatureOffset = getConfigNumVal("temperature and offset");
        config.enableThermistor1 = getConfigNumVal("enable and th1") >  0 ? true : false;
        config.enableThermistor2 = true;
        config.setNodeType(NodeType.SMART_STAT);


        RawPoint r1 = SmartStat.getPhysicalPoint(nodeAddr, Port.RELAY_ONE.toString());
        if (r1 != null && r1.getEnabled()) {
            Output relay1 = new Output();
            relay1.setAddress((short)nodeAddr);
            relay1.setPort(Port.RELAY_ONE);
            relay1.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r1.getType());
            config.getOutputs().add(relay1);
        }

        RawPoint r2 = SmartStat.getPhysicalPoint(nodeAddr, Port.RELAY_TWO.toString());
        if (r2 != null && r2.getEnabled()) {
            Output relay2 = new Output();
            relay2.setAddress((short)nodeAddr);
            relay2.setPort(Port.RELAY_TWO);
            relay2.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r2.getType());
            config.getOutputs().add(relay2);
        }

        RawPoint r3 = SmartStat.getPhysicalPoint(nodeAddr, Port.RELAY_THREE.toString());
        if (r3 != null && r3.getEnabled()) {
            Output relayThree = new Output();
            relayThree.setAddress((short)nodeAddr);
            relayThree.setPort(Port.RELAY_THREE);
            relayThree.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r3.getType());
            config.getOutputs().add(relayThree);
        }

        RawPoint r4 = SmartStat.getPhysicalPoint(nodeAddr, Port.RELAY_FOUR.toString());
        if (r4 != null && r4.getEnabled()) {
            Output relayFour = new Output();
            relayFour.setAddress((short)nodeAddr);
            relayFour.setPort(Port.RELAY_FOUR);
            relayFour.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r4.getType());
            config.getOutputs().add(relayFour);
        }

        RawPoint r5 = SmartStat.getPhysicalPoint(nodeAddr, Port.RELAY_FIVE.toString());
        if (r5 != null && r5.getEnabled()) {
            Output relayFive = new Output();
            relayFive.setAddress((short)nodeAddr);
            relayFive.setPort(Port.RELAY_FIVE);
            relayFive.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r5.getType());
            config.getOutputs().add(relayFive);
        }

        RawPoint r6 = SmartStat.getPhysicalPoint(nodeAddr, Port.RELAY_SIX.toString());
        if (r6 != null && r6.getEnabled()) {
            Output relaySix = new Output();
            relaySix.setAddress((short)nodeAddr);
            relaySix.setPort(Port.RELAY_SIX);
            relaySix.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r6.getType());
            config.getOutputs().add(relaySix);
        }
        return config;
    }
    public double getCurrentTemp()
    {
        currentTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and standalone and group == \""+nodeAddr+"\"");
        return currentTemp;
    }
    public void setCurrentTemp(double roomTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and current and standalone and group == \""+nodeAddr+"\"", roomTemp);
        this.currentTemp = roomTemp;
    }

    public double getHumidity()
    {
        humidity = CCUHsApi.getInstance().readHisValByQuery("point and air and humidity and sensor and current and group == \""+nodeAddr+"\"");
        return humidity;
    }
    public void setHumidity(double humidity)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and humidity and sensor and current and group == \""+nodeAddr+"\"", humidity);
        this.humidity = humidity;
    }

    public double getCO2()
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and co2 and sensor and current and group == \""+nodeAddr+"\"");
    }
    public void setCO2(double co2)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and co2 and sensor and current and group == \""+nodeAddr+"\"", co2);
    }

    public double getVOC()
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and voc and sensor and current and group == \""+nodeAddr+"\"");
    }
    public void setVOC(double voc)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and voc and sensor and current and group == \""+nodeAddr+"\"", voc);
    }


    public double getSupplyWaterTemp()
    {
        supplyWaterTemp = CCUHsApi.getInstance().readHisValByQuery("point and supply and water and temp and sensor and th2 and standalone and group == \""+nodeAddr+"\"");
        return supplyWaterTemp;
    }

    public double getDischargeTemp()
    {
        dischargeTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and th1 and standalone and group == \""+nodeAddr+"\"");
        return dischargeTemp;
    }
    public void setDischargeTemp(double dischargeTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and th1 and standalone and group == \""+nodeAddr+"\"", dischargeTemp);
        this.dischargeTemp = dischargeTemp;
    }
    public double getDesiredTemp()
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired and average and sp and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        desiredTemp = CCUHsApi.getInstance().readDefaultValById(id);
        return desiredTemp;
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
        this.desiredTemp = desiredTemp;
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
        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, HNum.make(desiredTemp), HNum.make(0));
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }

    public void setConfigNumVal(String tags,double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and zone and config and standalone and pipe2 and fcu and "+tags+" and group == \""+nodeAddr+"\"", val);
    }

    public double getConfigNumVal(String tags) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and standalone and pipe2 and fcu and "+tags+" and group == \""+nodeAddr+"\"");
    }

    public long getWaterValveLastOnTime(){
        return waterValveLastOnTime;
    }
    public void setWaterValveLastOnTime(long time){
        waterValveLastOnTime = time;
    }
    public long getWaterValvePeriodicTimer(){
        return waterValvePeriodicTimer;
    }
    public void setWaterValvePeriodicTimer(long time){
        waterValvePeriodicTimer = time;
    }
    public double getStatus() {
        return CCUHsApi.getInstance().readHisValByQuery("point and status and not message and his and group == \""+nodeAddr+"\"");
    }
    public void setStatus(double status) {
        CCUHsApi.getInstance().writeHisValByQuery("point and status and not message and his and group == \""+nodeAddr+"\"", status);
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

    public void setSmartStatStatus(String status)
    {
        CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \""+nodeAddr+"\"", status);
    }
    
    protected void addUserIntentPoints(String equipref, String equipDis, String room, String floor,
                                       TwoPipeFanCoilUnitConfiguration config) {

        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();

        Point fanOpMode = new Point.Builder()
                .setDisplayName(equipDis+"-"+"FanOpMode")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setFloorRef(floor)
                .setRoomRef(room).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("userIntent").addMarker("writable").addMarker("fan").addMarker("operation").addMarker("mode").addMarker("his")
                .addMarker("pipe2").addMarker("fcu").addMarker("zone")
                .setEnums("off,auto,low,medium,high")
                .setTz(tz)
                .build();
        String fanOpModeId = CCUHsApi.getInstance().addPoint(fanOpMode);
    
        StandaloneFanStage defaultFanMode = getDefaultFanSpeed(config);
        CCUHsApi.getInstance().writePointForCcuUser(fanOpModeId, TunerConstants.UI_DEFAULT_VAL_LEVEL,
                                                    (double)defaultFanMode.ordinal(), 0);
        CCUHsApi.getInstance().writeHisValById(fanOpModeId, (double)defaultFanMode.ordinal());

        Point operationalMode = new Point.Builder()
                .setDisplayName(equipDis+"-"+"ConditioningMode")
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .setEquipRef(equipref).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("userIntent").addMarker("writable").addMarker("conditioning").addMarker("mode").addMarker("zone").addMarker("his")
                .addMarker("pipe2").addMarker("fcu").addMarker("temp")
                .setEnums("off,auto,heatonly,coolonly")
                .setTz(tz)
                .build();
        String operationalModeId = CCUHsApi.getInstance().addPoint(operationalMode);
        
        StandaloneConditioningMode defaultConditioningMode = getDefaultConditioningMode();
        CCUHsApi.getInstance().writePointForCcuUser(operationalModeId, TunerConstants.UI_DEFAULT_VAL_LEVEL,
                                                    (double) defaultConditioningMode.ordinal(), 0);
        CCUHsApi.getInstance().writeHisValById(operationalModeId, (double)defaultConditioningMode.ordinal());

	}
	
	private StandaloneFanStage getDefaultFanSpeed(TwoPipeFanCoilUnitConfiguration config) {
        if (config.enableRelay1 || config.enableRelay2 || config.enableRelay3) {
            return StandaloneFanStage.AUTO;
        } else {
            return StandaloneFanStage.OFF;
        }
    }
    
    private StandaloneConditioningMode getDefaultConditioningMode() {
        return StandaloneConditioningMode.AUTO;
    }
    
    private void updateUserIntentPoints(TwoPipeFanCoilUnitConfiguration config, String equipRef) {
        String fanModePointId = CCUHsApi.getInstance().readId("point and zone and userIntent and fan and " +
                                                              "mode and equipRef == \"" + equipRef + "\"");
        if (fanModePointId.isEmpty()) {
            CcuLog.e(L.TAG_CCU_ZONE, "FanMode point does not exist for equip: "+nodeAddr);
            return;
        }
    
        double curFanSpeed = CCUHsApi.getInstance().readDefaultValById(fanModePointId);
    
        double fallbackFanSpeed = curFanSpeed;
        
        StandaloneFanStage maxFanSpeed = getMaxAvailableFanSpeed(config);
        
        if (curFanSpeed > maxFanSpeed.ordinal() && maxFanSpeed.ordinal() > StandaloneFanStage.OFF.ordinal()) {
            fallbackFanSpeed = StandaloneFanStage.AUTO.ordinal();
        } else if (curFanSpeed > maxFanSpeed.ordinal()) {
            fallbackFanSpeed = StandaloneFanStage.OFF.ordinal();
        }
        
        CCUHsApi.getInstance().writeDefaultValById(fanModePointId, fallbackFanSpeed);
        CCUHsApi.getInstance().writeHisValById(fanModePointId, fallbackFanSpeed);
    }
    
    private static StandaloneFanStage getMaxAvailableFanSpeed(TwoPipeFanCoilUnitConfiguration config) {
        
        StandaloneFanStage maxFanSpeed = StandaloneFanStage.OFF;
        if (config.enableRelay2) {
            maxFanSpeed = StandaloneFanStage.HIGH_ALL_TIME;
        } else if (config.enableRelay1) {
            maxFanSpeed = StandaloneFanStage.MEDIUM_ALL_TIME;
        } else if (config.enableRelay3) {
            maxFanSpeed = StandaloneFanStage.LOW_ALL_TIME;
        }
        return maxFanSpeed;
    }
}
