package a75f.io.logic.bo.building.sshpu;

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
import a75f.io.logic.bo.building.definitions.Consts;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.SmartStatFanRelayType;
import a75f.io.logic.bo.building.definitions.SmartStatHeatPumpChangeOverType;
import a75f.io.logic.bo.building.heartbeat.HeartBeat;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.hvac.StandaloneFanStage;
import a75f.io.logic.bo.building.sscpu.ConventionalPackageUnitUtil;
import a75f.io.logic.bo.building.sscpu.ConventionalUnitConfiguration;
import a75f.io.logic.bo.haystack.device.SmartStat;
import a75f.io.logic.tuners.StandAloneTuners;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.util.RxTask;

public class HeatPumpUnitEquip{


    int nodeAddr;
    ProfileType profileType;
    HeatPumpUnitProfile smartStatHpuUnit;

    double      currentTemp;
    double      humidity;
    double desiredTemp;
    public HeatPumpUnitEquip(ProfileType T, int node) {

        profileType = T;
        smartStatHpuUnit = new HeatPumpUnitProfile();
        nodeAddr = node;
    }

    public void createHaystackPoints(HeatPumpUnitConfiguration config, String floor, String room) {

        //Create Logical points
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis+"-HPU-"+nodeAddr;
        String profile = "hpu";
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
                .addMarker("equip").addMarker("standalone").addMarker("smartstat").addMarker("zone")
                .setGatewayRef(gatewayRef)
                .setTz(tz)
                .setGroup(String.valueOf(nodeAddr));
        b.setDisplayName(equipDis);
        b.addMarker(profile);
        String equipRef = CCUHsApi.getInstance().addEquip(b.build());
    
        RxTask.executeAsync(() -> StandAloneTuners.addEquipStandaloneTuners(CCUHsApi.getInstance(),
                                                                            siteRef,
                                                                            siteDis + "-HPU-" + nodeAddr,
                                                                            equipRef,
                                                                            room,
                                                                            floor,
                                                                            tz));

        createHeatPumpConfigPoints(config, equipRef,floor,room);

        addProfilePoints(siteRef,equipRef,equipDis,tz,floor,room);
        Point currentTemp = new Point.Builder()
                .setDisplayName(equipDis+"-currentTemp")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("cur")
                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String ctID = CCUHsApi.getInstance().addPoint(currentTemp);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(ctID, 0.0);

        Point humidity = new Point.Builder()
                .setDisplayName(equipDis+"-humidity")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("cur")
                .addMarker("air").addMarker("humidity").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("%")
                .setTz(tz)
                .build();
        String humidityId = CCUHsApi.getInstance().addPoint(humidity);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(humidityId, 0.0);

        Point co2 = new Point.Builder()
                .setDisplayName(equipDis+"-co2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("cur")
                .addMarker("air").addMarker("co2").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String co2Id = CCUHsApi.getInstance().addPoint(co2);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(co2Id, 0.0);

        Point voc = new Point.Builder()
                .setDisplayName(equipDis+"-voc")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("cur")
                .addMarker("air").addMarker("voc").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("ppb")
                .setTz(tz)
                .build();
        String vocId = CCUHsApi.getInstance().addPoint(voc);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(vocId, 0.0);


        Point occSensing = new Point.Builder()
                .setDisplayName(equipDis+"-occupancySensor")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("occupancy").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical")
                .setEnums("off,on")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String occSensingId = CCUHsApi.getInstance().addPoint(occSensing);

        Point sound = new Point.Builder()
                .setDisplayName(equipDis+"-sound")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("cur")
                .addMarker("air").addMarker("sound").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("dB")
                .setTz(tz)
                .build();
        String soundId = CCUHsApi.getInstance().addPoint(sound);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(soundId, 0.0);

        Point uvi = new Point.Builder()
                .setDisplayName(equipDis+"-uvi")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("cur")
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
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("cur")
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
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("cur")
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
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("cur")
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
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("cur")
                .addMarker("air").addMarker("pressure").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit(Consts.PRESSURE_UNIT)
                .setTz(tz)
                .build();
        String psId = CCUHsApi.getInstance().addPoint(ps);

        Point illu = new Point.Builder()
                .setDisplayName(equipDis+"-"+Port.SENSOR_ILLUMINANCE.name())
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("standalone").addMarker(profile).addMarker("cur")
                .addMarker("air").addMarker("illuminance").addMarker("sensor").addMarker("current").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("lux")
                .setTz(tz)
                .build();
        String illuId = CCUHsApi.getInstance().addPoint(illu);

        Point desiredTemp = new Point.Builder()
                .setDisplayName(equipDis+"-desiredTemp")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("standalone").addMarker("average")
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent").addMarker(profile)
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
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent").addMarker(profile)
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
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent").addMarker(profile)
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
                .addMarker("standalone").addMarker(profile).addMarker("cur").addMarker("discharge")
                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("th1").addMarker("his").addMarker("logical").addMarker("zone")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();

        String datID = CCUHsApi.getInstance().addPoint(datPoint);
        CCUHsApi.getInstance().writeHisValById(datID, 0.0);
        Point eatPoint = new Point.Builder()
                .setDisplayName(equipDis+"-external10kTempSensorTh2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("standalone").addMarker(profile).addMarker("his")
                .addMarker("air").addMarker("temp").addMarker("th2").addMarker("sensor").addMarker("logical").addMarker("zone")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String eatID = CCUHsApi.getInstance().addPoint(eatPoint);

        Point compressorStage1 = new Point.Builder()
                .setDisplayName(equipDis+"-compressorStage1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("compressor").addMarker("stage1").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("cmd").addMarker("runtime")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r1ID = CCUHsApi.getInstance().addPoint(compressorStage1);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(r1ID, 0.0);

        Point compressorStage2 = new Point.Builder()
                .setDisplayName(equipDis+"-compressorStage2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("compressor").addMarker("stage2").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("cmd").addMarker("runtime")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r2ID = CCUHsApi.getInstance().addPoint(compressorStage2);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(r2ID, 0.0);

        Point heatingStage1 = new Point.Builder()
                .setDisplayName(equipDis+"-auxHeating")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("aux").addMarker("heating").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("cmd").addMarker("runtime")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r4ID = CCUHsApi.getInstance().addPoint(heatingStage1);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(r4ID, 0.0);

        Point fanStage1 = new Point.Builder()
                .setDisplayName(equipDis+"-fanStage1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("fan").addMarker("stage1").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker(profile).addMarker("cmd").addMarker("runtime")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r3ID = CCUHsApi.getInstance().addPoint(fanStage1);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(r3ID, 0.0);

        Point equipStatus = new Point.Builder()
                .setDisplayName(equipDis+"-equipStatus")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("status").addMarker("hpu").addMarker("his").addMarker("zone")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("deadband,cooling,heating,tempdead")
                .setTz(tz)
                .build();
        String equipStatusId = CCUHsApi.getInstance().addPoint(equipStatus);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(equipStatusId, 0.0);

        Point equipStatusMessage = new Point.Builder()
                .setDisplayName(equipDis+"-equipStatusMessage")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("status").addMarker("message").addMarker(profile).addMarker("writable").addMarker("zone")
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
                .addMarker("scheduleStatus").addMarker(profile).addMarker("logical").addMarker("zone").addMarker("writable").addMarker("his")
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
                .addMarker("zone").addMarker(profile).addMarker("scheduleType").addMarker("writable").addMarker("zone").addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("building,zone,named")
                .setTz(tz)
                .build();
        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(equipScheduleType);
        CCUHsApi.getInstance().writeDefaultValById(equipScheduleTypeId, 0.0);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(equipScheduleTypeId, 0.0);

        String heartBeatId = CCUHsApi.getInstance().addPoint(HeartBeat.getHeartBeatPoint(equipDis, equipRef,
                siteRef, room, floor, nodeAddr, profile, tz));
        //Create Physical points and map
        SmartStat device = new SmartStat(nodeAddr, siteRef, floor, room,equipRef,profile);
        //TODO Need to set it for default if not enabled, currently set it as enabled //kumar

        device.th1In.setPointRef(datID);
        device.th1In.setEnabled(config.enableThermistor1);
        device.th2In.setPointRef(eatID);
        device.th2In.setEnabled(config.enableThermistor2);
        device.currentTemp.setPointRef(ctID);
        device.currentTemp.setEnabled(true);
        device.rssi.setPointRef(heartBeatId);
        device.rssi.setEnabled(true);
		device.addSensor(Port.SENSOR_RH, humidityId);
        device.addSensor(Port.SENSOR_CO2, co2Id);
        device.addSensor(Port.SENSOR_VOC, vocId);
		device.addSensor(Port.SENSOR_OCCUPANCY,occSensingId);
		device.addSensor(Port.SENSOR_SOUND,soundId);
		device.addSensor(Port.SENSOR_UVI,uviId);
        device.addSensor(Port.SENSOR_CO,coId);
        device.addSensor(Port.SENSOR_CO2_EQUIVALENT,co2EqId);
        device.addSensor(Port.SENSOR_NO,no2Id);
        device.addSensor(Port.SENSOR_ILLUMINANCE,illuId);
        device.addSensor(Port.SENSOR_PRESSURE,psId);
        device.desiredTemp.setPointRef(dtId);
        device.desiredTemp.setEnabled(true);
        device.relay1.setPointRef(r1ID);
        device.relay2.setPointRef(r2ID);
        device.relay3.setPointRef(r3ID);
        device.relay4.setPointRef(r4ID);
        //device.relay5.setPointRef(r5ID);
        //device.relay6.setPointRef(r6ID);
        device.relay1.setEnabled(config.isOpConfigured(Port.RELAY_ONE));
        device.relay2.setEnabled(config.isOpConfigured(Port.RELAY_TWO));
        device.relay3.setEnabled(config.isOpConfigured(Port.RELAY_THREE));
        device.relay4.setEnabled(config.isOpConfigured(Port.RELAY_FOUR));
        device.relay5.setEnabled(config.isOpConfigured(Port.RELAY_FIVE));
        device.relay6.setEnabled(config.isOpConfigured(Port.RELAY_SIX));

        SmartStatFanRelayType fanRelayType = SmartStatFanRelayType.values()[config.fanRelay5Type];
        switch (fanRelayType){
            case FAN_STAGE2:
                Point fanStage2 = new Point.Builder().setDisplayName(equipDis+"-fanStage2").setEquipRef(equipRef).setSiteRef(siteRef).setRoomRef(room).setFloorRef(floor).setHisInterpolate("cov")
                        .addMarker("standalone").addMarker("fan").addMarker("stage2").addMarker("his").addMarker("zone")
                        .addMarker("logical").addMarker(profile).addMarker("cmd").addMarker("runtime").addMarker("hpu")
                        .setGroup(String.valueOf(nodeAddr))
                        .setTz(tz)
                        .build();
                String r5ID = CCUHsApi.getInstance().addPoint(fanStage2);
                CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(r5ID, 0.0);
                device.relay5.setPointRef(r5ID);
                break;
            case HUMIDIFIER:
                Point humidifier = new Point.Builder().setDisplayName(equipDis+"-humidifier").setEquipRef(equipRef).setSiteRef(siteRef).setRoomRef(room).setFloorRef(floor).setHisInterpolate("cov")
                        .addMarker("standalone").addMarker("humidifier").addMarker("his").addMarker("zone").addMarker("logical").addMarker(profile).addMarker("cmd").addMarker("hpu")
                        .setEnums("off,on")
                        .setGroup(String.valueOf(nodeAddr))
                        .setTz(tz)
                        .build();
                String r5HumidID = CCUHsApi.getInstance().addPoint(humidifier);
                CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(r5HumidID, 0.0);
                device.relay5.setPointRef(r5HumidID);
                break;
            case DE_HUMIDIFIER:
                Point dehumidifier = new Point.Builder().setDisplayName(equipDis+"-deHumidifier").setEquipRef(equipRef).setSiteRef(siteRef).setRoomRef(room).setFloorRef(floor).setHisInterpolate("cov")
                        .addMarker("standalone").addMarker("dehumidifier").addMarker("his").addMarker("zone").addMarker("logical").addMarker(profile).addMarker("cmd").addMarker("hpu")
                        .setEnums("off,on")
                        .setGroup(String.valueOf(nodeAddr))
                        .setTz(tz)
                        .build();
                String r5DeHumidID = CCUHsApi.getInstance().addPoint(dehumidifier);
                CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(r5DeHumidID, 0.0);
                device.relay5.setPointRef(r5DeHumidID);
                break;
            default:
                break;
        }

        SmartStatHeatPumpChangeOverType changeOverType = SmartStatHeatPumpChangeOverType.values()[config.changeOverRelay6Type];
        switch (changeOverType){
            case ENERGIZE_IN_COOLING:
                Point heatpumpChangeoverCooling = new Point.Builder()
                        .setDisplayName(equipDis+"-heatpumpChangeoverCooling")
                        .setEquipRef(equipRef)
                        .setSiteRef(siteRef)
                        .setRoomRef(room)
                        .setFloorRef(floor).setHisInterpolate("cov")
                        .addMarker("standalone").addMarker("heatpump").addMarker("cooling").addMarker("changeover").addMarker("stage1").addMarker("his").addMarker("zone")
                        .addMarker("logical").addMarker(profile).addMarker("cmd")
                        .setEnums("off,on")
                        .setGroup(String.valueOf(nodeAddr))
                        .setTz(tz)
                        .build();
                String r6ID = CCUHsApi.getInstance().addPoint(heatpumpChangeoverCooling);
                CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(r6ID, 0.0);
                device.relay6.setPointRef(r6ID);
                break;
            case ENERGIZE_IN_HEATING:
                Point heatpumpChangeoverHeating = new Point.Builder()
                        .setDisplayName(equipDis+"-heatpumpChangeoverHeating")
                        .setEquipRef(equipRef)
                        .setSiteRef(siteRef)
                        .setRoomRef(room)
                        .setFloorRef(floor).setHisInterpolate("cov")
                        .addMarker("standalone").addMarker("heatpump").addMarker("heating").addMarker("changeover").addMarker("stage1").addMarker("his").addMarker("zone")
                        .addMarker("logical").addMarker(profile).addMarker("cmd")
                        .setEnums("off,on")
                        .setGroup(String.valueOf(nodeAddr))
                        .setTz(tz)
                        .build();
                String r6HeatID = CCUHsApi.getInstance().addPoint(heatpumpChangeoverHeating);
                CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(r6HeatID, 0.0);
                device.relay6.setPointRef(r6HeatID);
                break;
            default:
                break;
        }
        device.addPointsToDb();
        //initialize with default values if schedule fetch is null
        double coolingVal = 74.0;
        double heatingVal = 70.0;
        double defaultDesiredTemp = 72;
        setScheduleStatus("");
        setSmartStatStatus("OFF"); //Intialize with off
        Schedule schedule = Schedule.getScheduleByEquipId(equipRef);
        Occupied curOccupied = schedule.getCurrentValues();
        if(schedule != null && curOccupied != null) {
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
        Point cpuOccupancy = new Point.Builder()
                .setDisplayName(equipDis + "-" + "occupancy")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("occupancy").addMarker("mode").addMarker("his").addMarker("sp").addMarker("zone").addMarker("hpu")
                .setEnums("unoccupied,occupied,preconditioning,forcedoccupied,vacation,occupancysensing,autoforceoccupy,autoaway")
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(cpuOccupancy);

        Point hpuOperatingMode = new Point.Builder()
                .setDisplayName(equipDis + "-" + "OperatingMode")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("temp").addMarker("operating").addMarker("mode").addMarker("his").addMarker("sp").addMarker("zone").addMarker("hpu")
                .setEnums("off,cooling,heating,tempdead")
                .setTz(tz)
                .build();
        String condModeId = CCUHsApi.getInstance().addPoint(hpuOperatingMode);
        CCUHsApi.getInstance().writeHisValById(condModeId, 0.0);


    }

    public void setProfilePoint(String tags, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and zone and standalone and his and "+tags+" and group == \""+nodeAddr+"\"", val);
    }
    public void createHeatPumpConfigPoints(HeatPumpUnitConfiguration config, String equipRef, String floor, String room) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis+"-HPU-"+nodeAddr;
        String tz = siteMap.get("tz").toString();
        String profile = "hpu";
        Point enableOccupancyControl = new Point.Builder()
                .setDisplayName(equipDis + "-enableOccupancyControl")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("enable").addMarker("occupancy").addMarker("control").addMarker("sp").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
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
                    .addMarker("occupancy").addMarker("detection").addMarker("hpu").addMarker("his").addMarker("zone")
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
                .addMarker("temperature").addMarker("offset").addMarker("sp").addMarker(profile)
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
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone").addMarker("sp").addMarker("enable").addMarker(profile).addMarker("th1")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableAirflowId = CCUHsApi.getInstance().addPoint(airflowTh1);
        CCUHsApi.getInstance().writeDefaultValById(enableAirflowId, (double)(config.enableThermistor1 == true? 1.0 : 0));

        Point external10KProbeTh2 = new Point.Builder()
                .setDisplayName(equipDis+"-enableExternal10kTempSensorTh2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("th2").addMarker("sp").addMarker("enable").addMarker(profile).addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableexternal10KProbeTh2Id = CCUHsApi.getInstance().addPoint(external10KProbeTh2);
        CCUHsApi.getInstance().writeDefaultValById(enableexternal10KProbeTh2Id, (double)(config.enableThermistor2 == true? 1.0 : 0));

        Point enableRelay1 = new Point.Builder()
                .setDisplayName(equipDis+"-enableCompressorStage1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay1").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableRelay1Id = CCUHsApi.getInstance().addPoint(enableRelay1);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay1Id, (double)(config.enableRelay1 == true? 1.0 : 0));

        Point enableRelay2 = new Point.Builder()
                .setDisplayName(equipDis+"-enableCompressorStage2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay2").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableRelay2Id = CCUHsApi.getInstance().addPoint(enableRelay2);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay2Id, (double)(config.enableRelay2 == true? 1.0 : 0));

        Point enableRelay3 = new Point.Builder()
                .setDisplayName(equipDis+"-enableFanStage1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay3").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
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
                .addMarker("relay4").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableRelay4Id = CCUHsApi.getInstance().addPoint(enableRelay4);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay4Id, (double)(config.enableRelay4 == true? 1.0 : 0));

        Point enableRelay5 = new Point.Builder()
                .setDisplayName(equipDis+"-enableFanStage2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay5").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableRelay5Id = CCUHsApi.getInstance().addPoint(enableRelay5);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay5Id, (double)(config.enableRelay5 == true? 1.0 : 0));

        Point enableRelay6 = new Point.Builder()
                .setDisplayName(equipDis+"-enableHeatPumpChangeOver")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay6").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String enableRelay6Id = CCUHsApi.getInstance().addPoint(enableRelay6);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay6Id, (double)(config.enableRelay6 == true? 1.0 : 0));

//TODO need to consider once new UI is done...

        Point relay6Type = new Point.Builder()
                .setDisplayName(equipDis+"-heatPumpChangeoverType")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay6").addMarker("changeover").addMarker("type").addMarker("sp").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
				.setEnums("na,cooling,heating")
                .setTz(tz)
                .build();
        String relay6TypeID = CCUHsApi.getInstance().addPoint(relay6Type);
        CCUHsApi.getInstance().writeDefaultValById(relay6TypeID, (double)config.changeOverRelay6Type);
        Point relay5FanType = new Point.Builder()
                .setDisplayName(equipDis+"-fanHighRelayType")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone")
                .addMarker("relay5").addMarker("fan").addMarker("type").addMarker("sp").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("na,fanhigh,humidifier,dehumidifier")
                .setTz(tz)
                .build();
        String relay5FanTypeId = CCUHsApi.getInstance().addPoint(relay5FanType);
        CCUHsApi.getInstance().writeDefaultValById(relay5FanTypeId, (double)config.fanRelay5Type);
        addUserIntentPoints(equipRef,equipDis,room,floor, config);


        setConfigNumVal("enable and relay1",config.enableRelay1 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay2",config.enableRelay2 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay3",config.enableRelay3 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay4",config.enableRelay4 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay5",config.enableRelay5 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay6",config.enableRelay6 == true ? 1.0 : 0);
        setConfigNumVal("relay6 and type",config.changeOverRelay6Type);
        setConfigNumVal("relay5 and type",config.fanRelay5Type);
        setConfigNumVal("enable and occupancy",config.enableOccupancyControl == true ? 1.0 : 0);
        setConfigNumVal("temperature and offset",config.temperatureOffset);
        setConfigNumVal("enable and th1",config.enableThermistor1 == true ? 1.0 : 0);
        setConfigNumVal("enable and th2",config.enableThermistor2 == true ? 1.0 : 0);

    }

    public void updateHaystackPoints(HeatPumpUnitConfiguration config) {
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
        double relay5Type = getConfigNumVal("relay5 and type");
        if (config.enableRelay5 && config.fanRelay5Type != relay5Type) {
            HeatPumpPackageUnitUtil.manageRelay5AssociatedPoints(config.fanRelay5Type, equip);
        }
    
        double relay6Type = getConfigNumVal("relay6 and type");
        if (config.enableRelay6 && config.changeOverRelay6Type != relay6Type) {
            HeatPumpPackageUnitUtil.manageRelay6AssociatedPoints(config.changeOverRelay6Type, equip);
        }
        
        HeatPumpPackageUnitUtil.updateOccupancyPoint(config.enableOccupancyControl ? 1.0 : 0, equip,
                                                         CCUHsApi.getInstance());
        
        
        setConfigNumVal("enable and relay1",config.enableRelay1 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay2",config.enableRelay2 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay3",config.enableRelay3 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay4",config.enableRelay4 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay5",config.enableRelay5 == true ? 1.0 : 0);
        setConfigNumVal("enable and relay6",config.enableRelay6 == true ? 1.0 : 0);
        setConfigNumVal("enable and occupancy",config.enableOccupancyControl == true ? 1.0 : 0);
        setConfigNumVal("temperature and offset",config.temperatureOffset);
        setConfigNumVal("enable and th1",config.enableThermistor1 == true ? 1.0 : 0);
        setConfigNumVal("enable and th2",config.enableThermistor2 == true ? 1.0 : 0);
        setConfigNumVal("relay6 and type",config.changeOverRelay6Type);
        setConfigNumVal("relay5 and type",config.fanRelay5Type);
    
        updateFanMode(equip, config, CCUHsApi.getInstance());
        updateConditioningMode(equip, config, CCUHsApi.getInstance());
    }



    public HeatPumpUnitConfiguration getProfileConfiguration() {
        HeatPumpUnitConfiguration config = new HeatPumpUnitConfiguration();
        config.enableOccupancyControl = getConfigNumVal("enable and occupancy") > 0 ? true : false ;
        config.temperatureOffset = getConfigNumVal("temperature and offset");
        config.enableThermistor1 = getConfigNumVal("enable and th1") >  0 ? true : false;
        config.enableThermistor2 = getConfigNumVal("enable and th2") > 0 ? true : false;
        config.setNodeType(NodeType.SMART_STAT);//TODO - revisit
        config.fanRelay5Type = (int)getConfigNumVal("relay5 and type");
        config.changeOverRelay6Type = (int)getConfigNumVal("relay6 and type");


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


    public double getOccupancySensor()
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and occupancy and sensor and current and group == \""+nodeAddr+"\"");
    }
    public void setOccupancySensor(double occupancySensor)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and occupancy and sensor and current and group == \""+nodeAddr+"\"", occupancySensor);
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
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(id, desiredTemp);
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
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(id, desiredTemp);
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
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(id, desiredTemp);
    }

    public void setConfigNumVal(String tags,double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and zone and config and standalone and hpu and "+tags+" and group == \""+nodeAddr+"\"", val);
    }

    public double getConfigNumVal(String tags) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and standalone and hpu and "+tags+" and group == \""+nodeAddr+"\"");
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
                                       HeatPumpUnitConfiguration config) {
    
        CcuLog.i("CCU_HPU", "addUserIntentPoints ");
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
                .addMarker("hpu").addMarker("zone")
                .setEnums("off,auto,low,high")
                .setTz(tz)
                .build();
        String fanOpModeId = CCUHsApi.getInstance().addPoint(fanOpMode);
        StandaloneFanStage defaultFanStage = getDefaultFanSpeed(config);
        CCUHsApi.getInstance().writePointForCcuUser(fanOpModeId, TunerConstants.UI_DEFAULT_VAL_LEVEL,
                                                    (double) defaultFanStage.ordinal(), 0);
        CCUHsApi.getInstance().writeHisValById(fanOpModeId, (double) defaultFanStage.ordinal());

        Point conditionalMode = new Point.Builder()
                .setDisplayName(equipDis+"-"+"ConditioningMode")
                .setSiteRef(siteRef)
                .setFloorRef(floor)
                .setRoomRef(room)
                .setEquipRef(equipref).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("userIntent").addMarker("writable").addMarker("conditioning").addMarker("mode").addMarker("zone").addMarker("his")
                .addMarker("hpu").addMarker("temp")
                .setEnums("off,auto,heatonly,coolonly")
                .setTz(tz)
                .build();
        String conditionalModeId = CCUHsApi.getInstance().addPoint(conditionalMode);
        StandaloneConditioningMode defaultConditioning = getDefaultConditioningMode(config);
        CCUHsApi.getInstance().writePointForCcuUser(conditionalModeId, TunerConstants.UI_DEFAULT_VAL_LEVEL,
                                                    (double) defaultConditioning.ordinal(), 0);
        CCUHsApi.getInstance().writeHisValById(conditionalModeId, (double)defaultConditioning.ordinal());


        Point targetDehumidifier = new Point.Builder()
                .setDisplayName(equipDis + "-" + "targetDehumidifier")
                .setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("userIntent").addMarker("writable").addMarker("target").addMarker("his")
                .setTz(tz).addMarker("dehumidifier").addMarker("sp").build();
        String targetDehumidifierId = CCUHsApi.getInstance().addPoint(targetDehumidifier);
        CCUHsApi.getInstance().writePointForCcuUser(targetDehumidifierId, TunerConstants.UI_DEFAULT_VAL_LEVEL, TunerConstants.STANDALONE_TARGET_DEHUMIDIFIER, 0);
        CCUHsApi.getInstance().writeHisValById(targetDehumidifierId, TunerConstants.STANDALONE_TARGET_DEHUMIDIFIER);
        Point targetHumidty = new Point.Builder()
                .setDisplayName(equipDis + "-" + "targetHumidity")
                .setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("userIntent").addMarker("writable").addMarker("target").addMarker("humidity").addMarker("sp")
                .addMarker("his").setTz(tz).build();
        String targetHumidtyId = CCUHsApi.getInstance().addPoint(targetHumidty);
        CCUHsApi.getInstance().writePointForCcuUser(targetHumidtyId, TunerConstants.UI_DEFAULT_VAL_LEVEL, TunerConstants.STANDALONE_TARGET_HUMIDITY, 0);
        CCUHsApi.getInstance().writeHisValById(targetHumidtyId, TunerConstants.STANDALONE_TARGET_HUMIDITY);
    }
    
    private StandaloneFanStage getDefaultFanSpeed(HeatPumpUnitConfiguration config) {
        if (config.enableRelay3 || config.enableRelay4|| config.enableRelay5) {
            return StandaloneFanStage.AUTO;
        } else {
            return StandaloneFanStage.OFF;
        }
    }
    
    private StandaloneConditioningMode getDefaultConditioningMode(HeatPumpUnitConfiguration config) {
        if (config.enableRelay1 || config.enableRelay2) {
            return StandaloneConditioningMode.AUTO;
        } else {
            return StandaloneConditioningMode.OFF;
        }
    }
    
    private void updateConditioningMode(Equip equip, HeatPumpUnitConfiguration config, CCUHsApi hayStack) {
        
        String conditioningModeId = CCUHsApi.getInstance().readId("point and zone and userIntent and conditioning and" +
                                                                  " mode and equipRef == \"" + equip.getId() + "\"");
        if (conditioningModeId.isEmpty()) {
            CcuLog.e(L.TAG_CCU_ZONE, "ConditioningMode point does not exist for update : " + equip.getDisplayName());
            return;
        }
        double curCondMode = CCUHsApi.getInstance().readDefaultValById(conditioningModeId);
        
        double conditioningMode = curCondMode;
        if (!config.enableRelay1 && !config.enableRelay2) {
            conditioningMode = StandaloneConditioningMode.OFF.ordinal();
        }
        
        CcuLog.i(L.TAG_CCU_ZONE, "adjustHPUConditioningMode " + curCondMode + " -> " + conditioningMode);
        if (curCondMode != conditioningMode) {
            hayStack.writeDefaultValById(conditioningModeId, conditioningMode);
            hayStack.writeHisValById(conditioningModeId, conditioningMode);
        }
    }
    
    
    private void updateFanMode(Equip equip, HeatPumpUnitConfiguration config, CCUHsApi hayStack) {
        
        double curFanSpeed = hayStack.readDefaultVal("point and zone and userIntent and fan and " +
                                                     "mode and equipRef == \"" + equip.getId() + "\"");
        
        double fallbackFanSpeed = StandaloneFanStage.OFF.ordinal();
        if (config.enableRelay5) {
            if (config.fanRelay5Type == SmartStatFanRelayType.FAN_STAGE2.ordinal()) {
                fallbackFanSpeed = curFanSpeed; //Nothing to do.
            } else {
                fallbackFanSpeed = StandaloneFanStage.LOW_ALL_TIME.ordinal();
            }
        }
        if (config.enableRelay3) {
            fallbackFanSpeed = StandaloneFanStage.LOW_ALL_TIME.ordinal();
        }
        CcuLog.i(L.TAG_CCU_ZONE, "adjustHPUFanMode "+curFanSpeed+" -> "+fallbackFanSpeed);
        if (curFanSpeed > fallbackFanSpeed) {
            //Fallback to AUTO or OFF depending atleast one fan is available.
            fallbackFanSpeed = fallbackFanSpeed > StandaloneFanStage.OFF.ordinal() ?
                                   StandaloneFanStage.AUTO.ordinal() : StandaloneFanStage.OFF.ordinal();
            hayStack.writeDefaultVal("point and zone and userIntent and fan and " +
                                     "mode and equipRef == \"" + equip.getId() + "\"",
                                     fallbackFanSpeed);
        }
    }
}
