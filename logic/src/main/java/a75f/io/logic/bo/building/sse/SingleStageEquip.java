package a75f.io.logic.bo.building.sse;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.SSEStage;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.BuildingTuners;

public class SingleStageEquip {

    int nodeAddr;
    ProfileType profileType;
    SingleStageProfile sseProfile;

    double      currentTemp;
    double      humidity;
    double desiredTemp;
    double supplyAirTemp;
    double dischargeTemp;

    String equipRef = null;


    public SingleStageEquip(ProfileType T, int node) {

        profileType = T;
        sseProfile = new SingleStageProfile();
        nodeAddr = node;
    }
    public void createHaystackPoints(SingleStageConfig config, String floorRef, String roomRef) {

        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis+"-SSE-"+nodeAddr;
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
                .addMarker("equip").addMarker("sse").addMarker("zone")
                .setAhuRef(ahuRef)
                .setTz(tz)
                .setGroup(String.valueOf(nodeAddr));
        equipRef = CCUHsApi.getInstance().addEquip(b.build());

        BuildingTuners.getInstance().addEquipStandaloneTuners(siteDis+"-SSE-"+nodeAddr, equipRef, roomRef, floorRef);
        createSSEConfigPoints(config, equipRef,floorRef,roomRef);


        Point currentTemp = new Point.Builder()
                .setDisplayName(siteDis+"-SSE-"+nodeAddr+"-currentTemp")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("sse")
                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String ctID = CCUHsApi.getInstance().addPoint(currentTemp);

        Point humidity = new Point.Builder()
                .setDisplayName(siteDis+"-SSE-"+nodeAddr+"-humidity")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("zone").addMarker("sse").setHisInterpolate("cov")
                .addMarker("air").addMarker("humidity").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("%")
                .setTz(tz)
                .build();
        String humidityId = CCUHsApi.getInstance().addPoint(humidity);

        Point co2 = new Point.Builder()
                .setDisplayName(siteDis+"-SSE-"+nodeAddr+"-co2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("zone").addMarker("sse").setHisInterpolate("cov")
                .addMarker("air").addMarker("co2").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String co2Id = CCUHsApi.getInstance().addPoint(co2);

        Point voc = new Point.Builder()
                .setDisplayName(siteDis+"-SSE-"+nodeAddr+"-voc")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("sse")
                .addMarker("air").addMarker("voc").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("ppb")
                .setTz(tz)
                .build();
        String vocId = CCUHsApi.getInstance().addPoint(voc);

        Point desiredTemp = new Point.Builder()
                .setDisplayName(siteDis+"-SSE-"+nodeAddr+"-desiredTemp")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("sse").addMarker("average")
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String dtId = CCUHsApi.getInstance().addPoint(desiredTemp);

        Point desiredTempCooling = new Point.Builder()
                .setDisplayName(siteDis+"-SSE-"+nodeAddr+"-desiredTempCooling")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("sse").addMarker("cooling")
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(desiredTempCooling);

        Point desiredTempHeating = new Point.Builder()
                .setDisplayName(siteDis+"-SSE-"+nodeAddr+"-desiredTempHeating")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("sse").addMarker("heating")
                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(desiredTempHeating);


        Point fanStage1 = new Point.Builder()
                .setDisplayName(equipDis+"-fanStage1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("fan").addMarker("stage1").addMarker("his").addMarker("zone")
                .addMarker("logical").addMarker("sse").addMarker("cmd")
                .setEnums("off,on")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String r2ID = CCUHsApi.getInstance().addPoint(fanStage1);
        Point equipStatus = new Point.Builder()
                .setDisplayName(siteDis+"-SSE-"+nodeAddr+"-equipStatus")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("status").addMarker("his").addMarker("sse").addMarker("logical").addMarker("zone")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("deadband,cooling,heating,tempdead")
                .setTz(tz)
                .build();
        String equipStatusId = CCUHsApi.getInstance().addPoint(equipStatus);
        CCUHsApi.getInstance().writeHisValById(equipStatusId, 0.0);

        Point equipStatusMessage = new Point.Builder()
                .setDisplayName(siteDis+"-SSE-"+nodeAddr+"-equipStatusMessage")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("status").addMarker("message").addMarker("sse").addMarker("writable").addMarker("logical").addMarker("zone")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .setKind("string")
                .build();
        String equipStatusMessageLd = CCUHsApi.getInstance().addPoint(equipStatusMessage);
        Point equipScheduleStatus = new Point.Builder()
                .setDisplayName(siteDis+"-SSE-"+nodeAddr+"-equipScheduleStatus")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("scheduleStatus").addMarker("logical").addMarker("sse").addMarker("zone").addMarker("writable").addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .setKind("string")
                .build();
        String equipScheduleStatusId = CCUHsApi.getInstance().addPoint(equipScheduleStatus);

        Point equipScheduleType = new Point.Builder()
                .setDisplayName(siteDis+"-SSE-"+nodeAddr+"-scheduleType")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("sse").addMarker("scheduleType").addMarker("writable").addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("building,zone,named")
                .setTz(tz)
                .build();
        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(equipScheduleType);
        CCUHsApi.getInstance().writeDefaultValById(equipScheduleTypeId, 0.0);
        CCUHsApi.getInstance().writeHisValById(equipScheduleTypeId, 0.0);

        Point dischargeAirTemp1 = new Point.Builder()
                .setDisplayName(siteDis+"-SSE-"+nodeAddr+"-airflowTempSensorTh1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("sse").addMarker("discharge")
                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String dat1Id = CCUHsApi.getInstance().addPoint(dischargeAirTemp1);
        CCUHsApi.getInstance().writeHisValById(dat1Id, 0.0);

        Point eatPoint = new Point.Builder()
                .setDisplayName(equipDis+"-external10kTempSensorTh2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("sse").addMarker("temp").addMarker("th2").addMarker("sensor")
                .addMarker("logical").addMarker("zone").addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String eatID = CCUHsApi.getInstance().addPoint(eatPoint);
        //CCUHsApi.getInstance().writeHisValById(eatID, 0.0);
        Point occupancy = new Point.Builder()
                .setDisplayName(equipDis + "-" + "occupancy")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("standalone").addMarker("occupancy").addMarker("mode").addMarker("his").addMarker("sp").addMarker("zone").addMarker("sse")
                .setEnums("unoccupied,occupied,preconditioning,forcedoccupied,vacation,occupancysensing")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(occupancy);
        
		SmartNode device = new SmartNode(nodeAddr, siteRef, floorRef, roomRef, equipRef);
        device.currentTemp.setPointRef(ctID);
        device.currentTemp.setEnabled(true);
        device.desiredTemp.setPointRef(dtId);
        device.desiredTemp.setEnabled(true);
        device.th1In.setPointRef(dat1Id);
        device.th1In.setEnabled(config.enableThermistor1);
        device.th2In.setPointRef(eatID);
        device.th2In.setEnabled(config.enableThermistor2);
        //device.relay1.setPointRef(r1coolID);
        device.relay2.setPointRef(r2ID);

        device.relay1.setEnabled(config.enableRelay1 > 0 ? true : false);
        device.relay2.setEnabled(config.enableRelay1 > 0 ? true : false);

        device.addSensor(Port.SENSOR_RH, humidityId);
        device.addSensor(Port.SENSOR_CO2, co2Id);
        device.addSensor(Port.SENSOR_VOC, vocId);

        SSEStage sseStage = SSEStage.values()[config.enableRelay1];
        switch (sseStage){
            case COOLING:
                Point coolingStage = new Point.Builder()
                        .setDisplayName(equipDis+"-coolingStage1")
                        .setEquipRef(equipRef)
                        .setSiteRef(siteRef)
                        .setRoomRef(roomRef)
                        .setFloorRef(floorRef).setHisInterpolate("cov")
                        .addMarker("standalone").addMarker("cooling").addMarker("stage1").addMarker("his").addMarker("zone")
                        .addMarker("logical").addMarker("sse").addMarker("cmd")
                        .setEnums("off,on")
                        .setGroup(String.valueOf(nodeAddr))
                        .setTz(tz)
                        .build();
                String r1coolID = CCUHsApi.getInstance().addPoint(coolingStage);

                CCUHsApi.getInstance().writeHisValById(r1coolID, 0.0);
                device.relay1.setPointRef(r1coolID);
                break;
            case HEATING:
                Point heatingStage = new Point.Builder()
                        .setDisplayName(equipDis+"-heatingStage1")
                        .setEquipRef(equipRef)
                        .setSiteRef(siteRef)
                        .setRoomRef(roomRef)
                        .setFloorRef(floorRef).setHisInterpolate("cov")
                        .addMarker("standalone").addMarker("heating").addMarker("stage1").addMarker("his").addMarker("zone")
                        .addMarker("logical").addMarker("sse").addMarker("cmd")
                        .setEnums("off,on")
                        .setGroup(String.valueOf(nodeAddr))
                        .setTz(tz)
                        .build();
                String r1heatID = CCUHsApi.getInstance().addPoint(heatingStage);
                    CCUHsApi.getInstance().writeHisValById(r1heatID, 0.0);
                device.relay1.setPointRef(r1heatID);
                break;

        }
        device.addPointsToDb();


        setCurrentTemp(0);
        setDesiredTempCooling(74.0);
        setDesiredTemp(72.0);
        setDesiredTempHeating(70.0);
        setDesiredTempHeating(70.0);
        setHumidity(0);
        setCO2(0);
        setVOC(0);

        CCUHsApi.getInstance().syncEntityTree();


    }

    String getId(){
        return equipRef;
    }
    private void createSSEConfigPoints(SingleStageConfig config, String equipRef, String floorRef, String roomRef) {

        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis+"-SSE-"+nodeAddr;
        String tz = siteMap.get("tz").toString();
        String profile = "sse";

        Point zonePriority = new Point.Builder()
                .setDisplayName(equipDis+"-zonePriority")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef).setHisInterpolate("cov")
                .addMarker("config").addMarker("sse").addMarker("writable").addMarker("zone")
                .addMarker("priority").addMarker("sp").addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("none,low,normal,high")
                .setTz(tz)
                .build();
        String zonePriorityId = CCUHsApi.getInstance().addPoint(zonePriority);
        CCUHsApi.getInstance().writeDefaultValById(zonePriorityId, (double)config.getPriority().ordinal());
        CCUHsApi.getInstance().writeHisValById(zonePriorityId, (double)config.getPriority().ordinal());

        Point temperatureOffset = new Point.Builder()
                .setDisplayName(equipDis+"-temperatureOffset")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .addMarker("config").addMarker("sse").addMarker("writable").addMarker("zone")
                .addMarker("temperature").addMarker("offset").addMarker("sp")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String temperatureOffsetId = CCUHsApi.getInstance().addPoint(temperatureOffset);
        CCUHsApi.getInstance().writeDefaultValById(temperatureOffsetId, (double)config.temperaturOffset);

        Point external10KProbeTh2 = new Point.Builder()
                .setDisplayName(equipDis+"-enableExternal10kTempSensorTh2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floorRef)
                .setRoomRef(roomRef)
                .addMarker("config").addMarker("standalone").addMarker("writable").addMarker("zone").addMarker("th2").addMarker("sp")
                .addMarker("enable").addMarker(profile).addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("Ohm")
                .setTz(tz)
                .build();
        String enableexternal10KProbeTh2Id = CCUHsApi.getInstance().addPoint(external10KProbeTh2);
        CCUHsApi.getInstance().writeDefaultValById(enableexternal10KProbeTh2Id, (double)(config.enableThermistor2 == true? 1.0 : 0));

        Point enableRelay1 = new Point.Builder()
                .setDisplayName(equipDis+"-enableRelay1")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floorRef)
                .setRoomRef(roomRef)
                .addMarker("config").addMarker("sse").addMarker("writable").addMarker("zone")
                .addMarker("relay1").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("notenabled,heating,cooling")
                .setTz(tz)
                .build();
        String enableRelay1Id = CCUHsApi.getInstance().addPoint(enableRelay1);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay1Id, config.isOpConfigured(Port.RELAY_ONE) ? (double)config.enableRelay1 : 0);

        Point enableRelay2 = new Point.Builder()
                .setDisplayName(equipDis+"-enableFanStageRelay2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floorRef)
                .setRoomRef(roomRef)
                .addMarker("config").addMarker("sse").addMarker("writable").addMarker("zone")
                .addMarker("relay2").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("notenabled,fan")
                .setTz(tz)
                .build();
        String enableRelay2Id = CCUHsApi.getInstance().addPoint(enableRelay2);
        CCUHsApi.getInstance().writeDefaultValById(enableRelay2Id, config.isOpConfigured(Port.RELAY_TWO) ? (double)config.enableRelay2 : 0);
        Point enableTh1 = new Point.Builder()
                .setDisplayName(equipDis+"-enableAirflowTempSensor")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floorRef)
                .setRoomRef(roomRef)
                .addMarker("config").addMarker("sse").addMarker("writable").addMarker("zone")
                .addMarker("th1").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String enableTh1Id = CCUHsApi.getInstance().addPoint(enableTh1);
        CCUHsApi.getInstance().writeDefaultValById(enableTh1Id, (config.enableThermistor1 ? 1.0 : 0));
        Point enableTh2 = new Point.Builder()
                .setDisplayName(equipDis+"-enableExternal10KThermistor2")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setFloorRef(floorRef)
                .setRoomRef(roomRef)
                .addMarker("config").addMarker("sse").addMarker("writable").addMarker("zone")
                .addMarker("th2").addMarker("sp").addMarker("enable").addMarker(profile)
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String enableTh2Id = CCUHsApi.getInstance().addPoint(enableTh2);
        CCUHsApi.getInstance().writeDefaultValById(enableTh2Id, (config.enableThermistor2 ? 1.0 : 0));
        setConfigNumVal("enable and relay1",config.isOpConfigured(Port.RELAY_ONE) ? (double)config.enableRelay1 : 0);
        setConfigNumVal("enable and relay2",config.isOpConfigured(Port.RELAY_TWO) ? (double)config.enableRelay2 : 0);
        setConfigNumVal("enable and th2",config.enableThermistor2 == true ? 1.0 : 0);
        setConfigNumVal("enable and th1",config.enableThermistor1 == true ? 1.0 : 0);
        setConfigNumVal("temperature and offset",config.temperaturOffset);
    }

    public void setProfilePoint(String tags, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and zone and standalone and his and "+tags, val);
    }

    public void updateHaystackPoints(SingleStageConfig config) {
        for (Output op : config.getOutputs()) {
            switch (op.getPort()) {
                case RELAY_ONE:
                case RELAY_TWO:
                    SmartNode.updatePhysicalPointType(nodeAddr, op.getPort().toString(), op.getRelayActuatorType());
                    break;
            }
        }
        SmartNode.setPointEnabled(nodeAddr, Port.RELAY_ONE.name(), config.isOpConfigured(Port.RELAY_ONE) );
        SmartNode.setPointEnabled(nodeAddr, Port.RELAY_TWO.name(), config.isOpConfigured(Port.RELAY_TWO) );
        SmartNode.setPointEnabled(nodeAddr, Port.TH2_IN.name(), config.enableThermistor2);
        SmartNode.setPointEnabled(nodeAddr, Port.TH1_IN.name(), config.enableThermistor1);
        HashMap equipHash = CCUHsApi.getInstance().read("equip and group == \"" + config.getNodeAddress() + "\"");
        Equip equip = new Equip.Builder().setHashMap(equipHash).build();
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-SSE-" + nodeAddr;
        double prevConfigR1Val = getConfigNumVal("enable and relay1");
        SSEStage ssePrevStage = SSEStage.values()[(int)prevConfigR1Val];
        SSEStage sseStage = SSEStage.values()[(int)config.enableRelay1];
        if(ssePrevStage != sseStage) {
            switch (sseStage) {
                case COOLING:

                    HashMap heatingPt = CCUHsApi.getInstance().read("point and standalone and heating and stage1 and  sse and equipRef== \"" + equip.getId() + "\"");
                    if ((heatingPt != null) && (heatingPt.size() > 0))
                        CCUHsApi.getInstance().deleteEntity(heatingPt.get("id").toString());
                    Point coolingStage = new Point.Builder()
                            .setDisplayName(equipDis + "-coolingStage1")
                            .setEquipRef(equipRef)
                            .setSiteRef(siteRef)
                            .setRoomRef(equip.getRoomRef())
                            .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                            .addMarker("standalone").addMarker("cooling").addMarker("stage1").addMarker("his").addMarker("zone")
                            .addMarker("logical").addMarker("sse").addMarker("cmd")
                            .setEnums("off,on")
                            .setGroup(String.valueOf(nodeAddr))
                            .setTz(tz)
                            .build();
                    String r1coolID = CCUHsApi.getInstance().addPoint(coolingStage);
                    CCUHsApi.getInstance().writeHisValById(r1coolID, 0.0);
                    SmartNode.updatePhysicalPointRef(nodeAddr,Port.RELAY_ONE.name(),r1coolID);
                    break;
                case HEATING:
					HashMap coolingPt = CCUHsApi.getInstance().read("point and standalone and cooling and stage1 and  sse and equipRef== \"" + equip.getId() + "\"");
                    if ((coolingPt != null) && (coolingPt.size() > 0))
                        CCUHsApi.getInstance().deleteEntity(coolingPt.get("id").toString());
                    Point heatingStage = new Point.Builder()
                            .setDisplayName(equipDis + "-heatingStage1")
                            .setEquipRef(equipRef)
                            .setSiteRef(siteRef)
                            .setRoomRef(equip.getRoomRef())
                            .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                            .addMarker("standalone").addMarker("heating").addMarker("stage1").addMarker("his").addMarker("zone")
                            .addMarker("logical").addMarker("sse").addMarker("cmd")
                            .setEnums("off,on")
                            .setGroup(String.valueOf(nodeAddr))
                            .setTz(tz)
                            .build();
                    String r1heatID = CCUHsApi.getInstance().addPoint(heatingStage);
                    CCUHsApi.getInstance().writeHisValById(r1heatID, 0.0);
                    SmartNode.updatePhysicalPointRef(nodeAddr,Port.RELAY_ONE.name(),r1heatID);
                    break;
                case NOT_INSTALLED:
                    HashMap heatPt = CCUHsApi.getInstance().read("point and standalone and heating and stage1 and  sse and equipRef== \"" + equip.getId() + "\"");
                    if ((heatPt != null) && (heatPt.size() > 0))
                        CCUHsApi.getInstance().deleteEntity(heatPt.get("id").toString());

                    HashMap coolPt = CCUHsApi.getInstance().read("point and standalone and cooling and stage1 and  sse and equipRef== \"" + equip.getId() + "\"");
                    if ((coolPt != null) && (coolPt.size() > 0))
                        CCUHsApi.getInstance().deleteEntity(coolPt.get("id").toString());
                    break;

            }
        }

        CCUHsApi.getInstance().syncPointEntityTree();
		setConfigNumVal("enable and relay1",config.isOpConfigured(Port.RELAY_ONE) ? (double)config.enableRelay1 : 0);
        setConfigNumVal("enable and relay2",config.isOpConfigured(Port.RELAY_TWO) ? (double)config.enableRelay2 : 0);
        setConfigNumVal("temperature and offset",config.temperaturOffset);
        setConfigNumVal("enable and th2",config.enableThermistor2 == true ? 1.0 : 0);
        setConfigNumVal("enable and th1",config.enableThermistor1 == true ? 1.0 : 0);
    }



    public SingleStageConfig getProfileConfiguration() {
        SingleStageConfig config = new SingleStageConfig();
        config.temperaturOffset = getConfigNumVal("temperature and offset");
        config.enableThermistor1 = getConfigNumVal("enable and th1") > 0 ? true : false;
        config.enableThermistor2 = getConfigNumVal("enable and th2") > 0 ? true : false;
        config.enableRelay1 = (int)getConfigNumVal("enable and relay1");
        config.enableRelay2 = (int)getConfigNumVal("enable and relay2");
        config.setNodeType(NodeType.SMART_NODE);


        RawPoint r1 = SmartNode.getPhysicalPoint(nodeAddr, Port.RELAY_ONE.toString());
        if (r1 != null && r1.getEnabled()) {
            Output relay1 = new Output();
            relay1.setAddress((short)nodeAddr);
            relay1.setPort(Port.RELAY_ONE);
            relay1.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r1.getType());
            config.getOutputs().add(relay1);
        }

        RawPoint r2 = SmartNode.getPhysicalPoint(nodeAddr, Port.RELAY_TWO.toString());
        if (r2 != null && r2.getEnabled()) {
            Output relay2 = new Output();
            relay2.setAddress((short)nodeAddr);
            relay2.setPort(Port.RELAY_TWO);
            relay2.mOutputRelayActuatorType = OutputRelayActuatorType.getEnum(r2.getType());
            config.getOutputs().add(relay2);
        }
        return config;
    }
    public double getCurrentTemp()
    {
        currentTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and sse and group == \""+nodeAddr+"\"");
        return currentTemp;
    }
    public void setCurrentTemp(double roomTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and current and sse and group == \""+nodeAddr+"\"", roomTemp);
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
        CCUHsApi.getInstance().pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, "ccu", HNum.make(desiredTemp), HNum.make(0));
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
        CCUHsApi.getInstance().pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, "ccu", HNum.make(desiredTemp), HNum.make(0));
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
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

    public void setConfigNumVal(String tags,double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and zone and config and sse and "+tags+" and group == \""+nodeAddr+"\"", val);
    }

    public double getConfigNumVal(String tags) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and sse and "+tags+" and group == \""+nodeAddr+"\"");
    }


    public double getStatus() {
        return CCUHsApi.getInstance().readHisValByQuery("point and status and not message and his and group == \""+nodeAddr+"\"");
    }
    public void setStatus(String sseStatus, double status, boolean emergency) {
        CCUHsApi.getInstance().writeHisValByQuery("point and status and not message and his and group == \"" + nodeAddr + "\"", status);


        String message;
        if (emergency) {
            message = (status == 0 ? "Recirculating Air" : status == 1 ? "Emergency Cooling" : "Emergency Heating");
        } else
        {
                message = (status == 0 ? "Recirculating Air" : status == 1 ? "Cooling Space" : "Warming Space");
                if(!sseStatus.isEmpty()){
                    if(sseStatus.equals("Fan ON"))
                        message = "Recirculating Air, " + sseStatus;
                    else
                        message = message + ","+sseStatus;
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
