package a75f.io.logic.bo.building.dab;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75.io.algos.CO2Loop;
import a75.io.algos.GenericPIController;
import a75.io.algos.VOCLoop;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Created by samjithsadasivan on 3/13/19.
 */

public class DabEquip
{
    public int nodeAddr;
    ProfileType profileType;
    
    double damperPos= 0;
    
    GenericPIController damperController;
    CCUHsApi hayStack = CCUHsApi.getInstance();
    String equipRef = null;
    
    CO2Loop co2Loop;
    VOCLoop  vocLoop;
    
    double   co2Target = TunerConstants.ZONE_CO2_TARGET;
    double   co2Threshold = TunerConstants.ZONE_CO2_THRESHOLD;
    double   vocTarget = TunerConstants.ZONE_VOC_TARGET;
    double   vocThreshold = TunerConstants.ZONE_VOC_THRESHOLD;
    
    public DabEquip(ProfileType type, int node)
    {
        profileType = type;
        nodeAddr = node;
        co2Loop = new CO2Loop();
        vocLoop = new VOCLoop();
    }
    
    public void init() {
        HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \"" + nodeAddr + "\"");
    
        if (equipMap != null && equipMap.size() > 0)
        {
            equipRef = equipMap.get("id").toString();
            damperController = new GenericPIController();
            damperController.setMaxAllowedError(TunerUtil.readTunerValByQuery("dab and pspread and equipRef == \"" + equipRef + "\""));
            damperController.setIntegralGain(TunerUtil.readTunerValByQuery("dab and igain and equipRef == \"" + equipRef + "\""));
            damperController.setProportionalGain(TunerUtil.readTunerValByQuery("dab and pgain and equipRef == \"" + equipRef + "\""));
            damperController.setIntegralMaxTimeout((int) TunerUtil.readTunerValByQuery("dab and itimeout and equipRef == \"" + equipRef + "\""));
    
            co2Target = (int) TunerUtil.readTunerValByQuery("zone and dab and co2 and target and equipRef == \""+equipRef+"\"");
            co2Threshold = (int) TunerUtil.readTunerValByQuery("zone and dab and co2 and threshold and equipRef == \""+equipRef+"\"");
            vocTarget = (int) TunerUtil.readTunerValByQuery("zone and dab and voc and target and equipRef == \""+equipRef+"\"");
            vocThreshold = (int) TunerUtil.readTunerValByQuery("zone and dab and voc and threshold and equipRef == \""+equipRef+"\"");
        }
    
    }
    
    public void createEntities(DabProfileConfiguration config, String floorRef, String roomRef)
    {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis+"-DAB-"+nodeAddr;
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
                                  .addMarker("equip").addMarker("dab").addMarker("zone").addMarker("equipHis")
                                  .setAhuRef(ahuRef)
                                  .setTz(tz)
                                  .setGroup(String.valueOf(nodeAddr));
        equipRef = CCUHsApi.getInstance().addEquip(b.build());
        BuildingTuners.getInstance().addEquipDabTuners(siteDis + "-DAB-" + nodeAddr, equipRef, roomRef, floorRef);
        createDabConfigPoints(config, equipRef);
    
        Point damper1Pos = new Point.Builder()
                                  .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-damper1Pos")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef)
                                  .addMarker("damper").addMarker("primary").addMarker("dab").addMarker("base").addMarker("cmd").addMarker("his").addMarker("logical").addMarker("zone").addMarker("equipHis")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setTz(tz)
                                  .build();
        String dpID = CCUHsApi.getInstance().addPoint(damper1Pos);
    
        Point damper2Pos = new Point.Builder()
                                  .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-damper2Pos")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef)
                                  .addMarker("damper").addMarker("secondary").addMarker("dab").addMarker("base").addMarker("cmd").addMarker("his").addMarker("logical").addMarker("zone").addMarker("equipHis")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setTz(tz)
                                  .build();
        String dp1ID = CCUHsApi.getInstance().addPoint(damper2Pos);
        
    
        Point normalizedDamper1Pos = new Point.Builder()
                                            .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-normalizedDamper1Pos")
                                            .setEquipRef(equipRef)
                                            .setSiteRef(siteRef)
                                            .setRoomRef(roomRef)
                                            .setFloorRef(floorRef)
                                            .addMarker("damper").addMarker("primary").addMarker("dab").addMarker("normalized").addMarker("cmd").addMarker("his").addMarker("logical").addMarker("zone").addMarker("equipHis")
                                            .setGroup(String.valueOf(nodeAddr))
                                            .setTz(tz)
                                            .build();
        String normalizedDamper1PosId = CCUHsApi.getInstance().addPoint(normalizedDamper1Pos);
    
        Point normalizedDamper2Pos = new Point.Builder()
                                             .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-normalizedDamper2Pos")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setRoomRef(roomRef)
                                             .setFloorRef(floorRef)
                                             .addMarker("damper").addMarker("secondary").addMarker("dab").addMarker("normalized").addMarker("cmd").addMarker("his").addMarker("logical").addMarker("zone").addMarker("equipHis")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setTz(tz)
                                             .build();
        String normalizedDamper2PosId = CCUHsApi.getInstance().addPoint(normalizedDamper2Pos);
    
        Point currentTemp = new Point.Builder()
                                    .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-currentTemp")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(roomRef)
                                    .setFloorRef(floorRef)
                                    .addMarker("zone").addMarker("dab")
                                    .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical").addMarker("equipHis")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setUnit("\u00B0F")
                                    .setTz(tz)
                                    .build();
        String ctID = CCUHsApi.getInstance().addPoint(currentTemp);
    
        Point humidity = new Point.Builder()
                                 .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-humidity")
                                 .setEquipRef(equipRef)
                                 .setSiteRef(siteRef)
                                 .setRoomRef(roomRef)
                                 .setFloorRef(floorRef)
                                 .addMarker("zone").addMarker("dab")
                                 .addMarker("air").addMarker("humidity").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical").addMarker("equipHis")
                                 .setGroup(String.valueOf(nodeAddr))
                                 .setTz(tz)
                                 .build();
        String humidityId = CCUHsApi.getInstance().addPoint(humidity);
    
        Point co2 = new Point.Builder()
                            .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-co2")
                            .setEquipRef(equipRef)
                            .setSiteRef(siteRef)
                            .setRoomRef(roomRef)
                            .setFloorRef(floorRef)
                            .addMarker("zone").addMarker("dab")
                            .addMarker("air").addMarker("co2").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical").addMarker("equipHis")
                            .setGroup(String.valueOf(nodeAddr))
                            .setTz(tz)
                            .build();
        String co2Id = CCUHsApi.getInstance().addPoint(co2);
    
        Point voc = new Point.Builder()
                            .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-voc")
                            .setEquipRef(equipRef)
                            .setSiteRef(siteRef)
                            .setRoomRef(roomRef)
                            .setFloorRef(floorRef)
                            .addMarker("zone").addMarker("dab")
                            .addMarker("air").addMarker("voc").addMarker("sensor").addMarker("current").addMarker("his").addMarker("cur").addMarker("logical").addMarker("equipHis")
                            .setGroup(String.valueOf(nodeAddr))
                            .setTz(tz)
                            .build();
        String vocId = CCUHsApi.getInstance().addPoint(voc);
    
        Point desiredTemp = new Point.Builder()
                                    .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-desiredTemp")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(roomRef)
                                    .setFloorRef(floorRef)
                                    .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("dab").addMarker("average")
                                    .addMarker("sp").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("userIntent")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setUnit("\u00B0F")
                                    .setTz(tz)
                                    .build();
        String dtId = CCUHsApi.getInstance().addPoint(desiredTemp);
    
        Point desiredTempCooling = new Point.Builder()
                                           .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-desiredTempCooling")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("dab").addMarker("cooling")
                                           .addMarker("sp").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("userIntent")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setUnit("\u00B0F")
                                           .setTz(tz)
                                           .build();
        CCUHsApi.getInstance().addPoint(desiredTempCooling);
    
        Point desiredTempHeating = new Point.Builder()
                                           .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-desiredTempHeating")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("dab").addMarker("heating")
                                           .addMarker("sp").addMarker("writable").addMarker("his").addMarker("equipHis").addMarker("userIntent")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setUnit("\u00B0F")
                                           .setTz(tz)
                                           .build();
        CCUHsApi.getInstance().addPoint(desiredTempHeating);
    
        Point equipStatus = new Point.Builder()
                                    .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-equipStatus")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(roomRef)
                                    .setFloorRef(floorRef)
                                    .addMarker("status").addMarker("vav").addMarker("his").addMarker("dab").addMarker("logical").addMarker("zone").addMarker("equipHis")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setTz(tz)
                                    .build();
        String equipStatusId = CCUHsApi.getInstance().addPoint(equipStatus);
    
        Point equipStatusMessage = new Point.Builder()
                                           .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-equipStatusMessage")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("status").addMarker("message").addMarker("dab").addMarker("writable").addMarker("logical").addMarker("zone").addMarker("equipHis")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setTz(tz)
                                           .setKind("string")
                                           .build();
        String equipStatusMessageLd = CCUHsApi.getInstance().addPoint(equipStatusMessage);
        Point equipScheduleStatus = new Point.Builder()
                                            .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-equipScheduleStatus")
                                            .setEquipRef(equipRef)
                                            .setSiteRef(siteRef)
                                            .setRoomRef(roomRef)
                                            .setFloorRef(floorRef)
                                            .addMarker("scheduleStatus").addMarker("logical").addMarker("dab").addMarker("zone").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .setGroup(String.valueOf(nodeAddr))
                                            .setTz(tz)
                                            .setKind("string")
                                            .build();
        String equipScheduleStatusId = CCUHsApi.getInstance().addPoint(equipScheduleStatus);
    
        Point equipScheduleType = new Point.Builder()
                                          .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-scheduleType")
                                          .setEquipRef(equipRef)
                                          .setSiteRef(siteRef)
                                          .setRoomRef(roomRef)
                                          .setFloorRef(floorRef)
                                          .addMarker("zone").addMarker("dab").addMarker("scheduleType").addMarker("writable").addMarker("his").addMarker("equipHis")
                                          .setGroup(String.valueOf(nodeAddr))
                                          .setTz(tz)
                                          .build();
        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(equipScheduleType);
        CCUHsApi.getInstance().writeDefaultValById(equipScheduleTypeId, 0.0);
        CCUHsApi.getInstance().writeHisValById(equipScheduleTypeId, 0.0);
    
        Point dischargeAirTemp1 = new Point.Builder()
                                    .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-dischargeAirTemp1")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(roomRef)
                                    .setFloorRef(floorRef)
                                    .addMarker("zone").addMarker("dab")
                                    .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("discharge").addMarker("primary").addMarker("his").addMarker("logical").addMarker("equipHis")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setUnit("\u00B0F")
                                    .setTz(tz)
                                    .build();
        String dat1Id = CCUHsApi.getInstance().addPoint(dischargeAirTemp1);
        CCUHsApi.getInstance().writeHisValById(dat1Id, 0.0);
    
        Point dischargeAirTemp2 = new Point.Builder()
                                       .setDisplayName(siteDis+"-DAB-"+nodeAddr+"-dischargeAirTemp2")
                                       .setEquipRef(equipRef)
                                       .setSiteRef(siteRef)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef)
                                       .addMarker("zone").addMarker("dab")
                                       .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("discharge").addMarker("secondary").addMarker("his").addMarker("logical").addMarker("equipHis")
                                       .setGroup(String.valueOf(nodeAddr))
                                       .setUnit("\u00B0F")
                                       .setTz(tz)
                                       .build();
        String dat2Id = CCUHsApi.getInstance().addPoint(dischargeAirTemp2);
        CCUHsApi.getInstance().writeHisValById(dat2Id, 0.0);
    
        Point occupancy = new Point.Builder()
                                  .setDisplayName(siteDis+"-VAV-"+nodeAddr+"-occupancy")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef)
                                  .addMarker("dab").addMarker("occupancy").addMarker("status").addMarker("zone").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setTz(tz)
                                  .build();
        String occupancyId = CCUHsApi.getInstance().addPoint(occupancy);
        CCUHsApi.getInstance().writeHisValById(occupancyId, 0.0);
        
        SmartNode device = new SmartNode(nodeAddr, siteRef, floorRef, roomRef, equipRef);
        device.currentTemp.setPointRef(ctID);
        device.currentTemp.setEnabled(true);
        device.desiredTemp.setPointRef(dtId);
        device.desiredTemp.setEnabled(true);
        device.th1In.setPointRef(dat1Id);
        device.th1In.setEnabled(true);
        device.th2In.setPointRef(dat2Id);
        device.th2In.setEnabled(true);
        
        for (Output op : config.getOutputs()) {
            switch (op.getPort()) {
                case ANALOG_OUT_ONE:
                    device.analog1Out.setType(op.getAnalogActuatorType());
                    break;
                case ANALOG_OUT_TWO:
                    device.analog1Out.setType(op.getAnalogActuatorType());
                    break;
            }
        }
        device.analog1Out.setEnabled(config.isOpConfigured(Port.ANALOG_OUT_ONE));
        device.analog1Out.setPointRef(normalizedDamper1PosId);
        device.analog2Out.setEnabled(config.isOpConfigured(Port.ANALOG_OUT_TWO));
        device.analog2Out.setPointRef(normalizedDamper2PosId);
    
        device.addSensor(Port.SENSOR_RH, humidityId);
        device.addSensor(Port.SENSOR_CO2, co2Id);
        device.addSensor(Port.SENSOR_VOC, vocId);
        
        device.addPointsToDb();
    
        
        setCurrentTemp(0);
        setDamperPos(0, "primary");
        setDamperPos(0, "secondary");
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
    
    public void createDabConfigPoints(DabProfileConfiguration config, String equipRef) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis+"-DAB-"+nodeAddr;
        String tz = siteMap.get("tz").toString();
        
        Point damper1Type = new Point.Builder()
                                   .setDisplayName(equipDis+"-damper1Type")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                   .addMarker("damper").addMarker("primary").addMarker("type").addMarker("sp")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setTz(tz)
                                   .build();
        String damperTypeId = CCUHsApi.getInstance().addPoint(damper1Type);
        CCUHsApi.getInstance().writeDefaultValById(damperTypeId, (double)config.damper1Type);
        
        Point damper1Size = new Point.Builder()
                                   .setDisplayName(equipDis+"-damper1Size")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                   .addMarker("damper").addMarker("primary").addMarker("size").addMarker("sp")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setTz(tz)
                                   .build();
        String damper1SizeId = CCUHsApi.getInstance().addPoint(damper1Size);
        CCUHsApi.getInstance().writeDefaultValById(damper1SizeId, (double)config.damper1Size);
        
        Point damper1Shape = new Point.Builder()
                                    .setDisplayName(equipDis+"-damper1Shape")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                    .addMarker("damper").addMarker("primary").addMarker("shape").addMarker("sp")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setTz(tz)
                                    .build();
        String damper1ShapeId = CCUHsApi.getInstance().addPoint(damper1Shape);
        CCUHsApi.getInstance().writeDefaultValById(damper1ShapeId, (double)config.damper1Shape);
    
    
        Point damper2Type = new Point.Builder()
                                   .setDisplayName(equipDis+"-damper2Type")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                   .addMarker("damper").addMarker("secondary").addMarker("type").addMarker("sp")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setTz(tz)
                                   .build();
        String damper2TypeId = CCUHsApi.getInstance().addPoint(damper2Type);
        CCUHsApi.getInstance().writeDefaultValById(damper2TypeId, (double)config.damper2Type);
    
        Point damper2Size = new Point.Builder()
                                   .setDisplayName(equipDis+"-damper2Size")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                   .addMarker("damper").addMarker("secondary").addMarker("size").addMarker("sp")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setTz(tz)
                                   .build();
        String damper2SizeId = CCUHsApi.getInstance().addPoint(damper2Size);
        CCUHsApi.getInstance().writeDefaultValById(damper2SizeId, (double)config.damper1Size);
    
        Point damper2Shape = new Point.Builder()
                                    .setDisplayName(equipDis+"-damper2Shape")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                    .addMarker("damper").addMarker("secondary").addMarker("shape").addMarker("sp")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setTz(tz)
                                    .build();
        String damper2ShapeId = CCUHsApi.getInstance().addPoint(damper2Shape);
        CCUHsApi.getInstance().writeDefaultValById(damper2ShapeId, (double)config.damper2Shape);
        
        
        
        Point enableOccupancyControl = new Point.Builder()
                                               .setDisplayName(equipDis+"-enableOccupancyControl")
                                               .setEquipRef(equipRef)
                                               .setSiteRef(siteRef)
                                               .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                               .addMarker("enable").addMarker("occupancy").addMarker("control").addMarker("his").addMarker("equipHis").addMarker("sp")
                                               .setGroup(String.valueOf(nodeAddr))
                                               .setTz(tz)
                                               .build();
        String enableOccupancyControlId = CCUHsApi.getInstance().addPoint(enableOccupancyControl);
        CCUHsApi.getInstance().writeDefaultValById(enableOccupancyControlId, config.enableOccupancyControl == true ? 1.0 :0);
        CCUHsApi.getInstance().writeHisValById(enableOccupancyControlId, config.enableOccupancyControl == true ? 1.0 :0);
        
        Point enableCO2Control = new Point.Builder()
                                         .setDisplayName(equipDis+"-enableCO2Control")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                         .addMarker("enable").addMarker("co2").addMarker("control").addMarker("sp").addMarker("his").addMarker("equipHis")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String enableCO2ControlId = CCUHsApi.getInstance().addPoint(enableCO2Control);
        CCUHsApi.getInstance().writeDefaultValById(enableCO2ControlId, config.enableCO2Control == true ? 1.0 :0);
        CCUHsApi.getInstance().writeHisValById(enableCO2ControlId, config.enableCO2Control == true ? 1.0 :0);
        
        Point enableIAQControl = new Point.Builder()
                                         .setDisplayName(equipDis+"-enableIAQControl")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                         .addMarker("enable").addMarker("iaq").addMarker("control").addMarker("sp").addMarker("his").addMarker("equipHis")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String enableIAQControlId = CCUHsApi.getInstance().addPoint(enableIAQControl);
        CCUHsApi.getInstance().writeDefaultValById(enableIAQControlId, config.enableIAQControl == true ? 1.0 :0);
        CCUHsApi.getInstance().writeHisValById(enableIAQControlId, config.enableIAQControl == true ? 1.0 :0);
        
        Point zonePriority = new Point.Builder()
                                     .setDisplayName(equipDis+"-zonePriority")
                                     .setEquipRef(equipRef)
                                     .setSiteRef(siteRef)
                                     .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                     .addMarker("priority").addMarker("sp").addMarker("his").addMarker("equipHis")
                                     .setGroup(String.valueOf(nodeAddr))
                                     .setTz(tz)
                                     .build();
        String zonePriorityId = CCUHsApi.getInstance().addPoint(zonePriority);
        CCUHsApi.getInstance().writeDefaultValById(zonePriorityId, (double)config.getPriority().ordinal());
        CCUHsApi.getInstance().writeHisValById(zonePriorityId, (double)config.getPriority().ordinal());
        
        Point temperatureOffset = new Point.Builder()
                                          .setDisplayName(equipDis+"-temperatureOffset")
                                          .setEquipRef(equipRef)
                                          .setSiteRef(siteRef)
                                          .addMarker("config").addMarker("dab").addMarker("writable").addMarker("zone")
                                          .addMarker("temperature").addMarker("offset").addMarker("sp")
                                          .setGroup(String.valueOf(nodeAddr))
                                          .setTz(tz)
                                          .build();
        String temperatureOffsetId = CCUHsApi.getInstance().addPoint(temperatureOffset);
        CCUHsApi.getInstance().writeDefaultValById(temperatureOffsetId, (double)config.temperaturOffset);
        
        Point damperMinCooling = new Point.Builder()
                                         .setDisplayName(equipDis+"-minCoolingDamperPos")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("dab").addMarker("damper").addMarker("min").addMarker("cooling").addMarker("pos")
                                         .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his").addMarker("equipHis")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String damperMinCoolingId = CCUHsApi.getInstance().addPoint(damperMinCooling);
        CCUHsApi.getInstance().writeDefaultValById(damperMinCoolingId, (double)config.minDamperCooling);
        CCUHsApi.getInstance().writeHisValById(damperMinCoolingId, (double)config.minDamperCooling);
        
        Point damperMaxCooling = new Point.Builder()
                                         .setDisplayName(equipDis+"-maxCoolingDamperPos")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("dab").addMarker("damper").addMarker("max").addMarker("cooling").addMarker("pos")
                                         .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his").addMarker("equipHis")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String damperMaxCoolingId = CCUHsApi.getInstance().addPoint(damperMaxCooling);
        CCUHsApi.getInstance().writeDefaultValById(damperMaxCoolingId, (double)config.maxDamperCooling);
        CCUHsApi.getInstance().writeHisValById(damperMaxCoolingId, (double)config.maxDamperCooling);
        
        
        Point damperMinHeating = new Point.Builder()
                                         .setDisplayName(equipDis+"-minHeatingDamperPos")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("dab").addMarker("damper").addMarker("min").addMarker("heating").addMarker("pos")
                                         .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his").addMarker("equipHis")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String damperMinHeatingId = CCUHsApi.getInstance().addPoint(damperMinHeating);
        CCUHsApi.getInstance().writeDefaultValById(damperMinHeatingId, (double)config.minDamperHeating);
        CCUHsApi.getInstance().writeHisValById(damperMinHeatingId, (double)config.minDamperHeating);
        
        Point damperMaxHeating = new Point.Builder()
                                         .setDisplayName(equipDis+"-maxHeatingDamperPos")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .addMarker("config").addMarker("dab").addMarker("damper").addMarker("max").addMarker("heating").addMarker("pos")
                                         .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his").addMarker("equipHis")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String damperMaxHeatingId = CCUHsApi.getInstance().addPoint(damperMaxHeating);
        CCUHsApi.getInstance().writeDefaultValById(damperMaxHeatingId, (double) config.maxDamperHeating);
        CCUHsApi.getInstance().writeHisValById(damperMaxHeatingId, (double) config.maxDamperHeating);
    }
    
    public DabProfileConfiguration getProfileConfiguration() {
        DabProfileConfiguration config = new DabProfileConfiguration();
        config.minDamperCooling = ((int)getDamperLimit("cooling","min"));
        config.maxDamperCooling = ((int)getDamperLimit("cooling","max"));
        config.minDamperHeating = ((int)getDamperLimit("heating","min"));
        config.maxDamperHeating = ((int)getDamperLimit("heating","max"));
    
        config.damper1Type = (int)getConfigNumVal("damper and primary and type");
        config.damper1Size = (int)getConfigNumVal("damper and primary and size");
        config.damper1Shape = (int)getConfigNumVal("damper and primary and shape");
    
        config.damper2Type = (int)getConfigNumVal("damper and secondary and type");
        config.damper2Size = (int)getConfigNumVal("damper and secondary and size");
        config.damper2Shape = (int)getConfigNumVal("damper and secondary and shape");
        
        config.enableOccupancyControl = getConfigNumVal("enable and occupancy") > 0 ? true : false ;
        config.enableCO2Control = getConfigNumVal("enable and co2") > 0 ? true : false ;
        config.enableIAQControl = getConfigNumVal("enable and iaq") > 0 ? true : false ;
        //config.setPriority(ZonePriority.values()[(int)getConfigNumVal("priority")]);
        config.setPriority(ZonePriority.values()[(int)getZonePriorityValue()]);
        config.temperaturOffset = getConfigNumVal("temperature and offset");
    
        config.setNodeType(NodeType.SMART_NODE);//TODO - revisit
    
    
        RawPoint a1 = SmartNode.getPhysicalPoint(nodeAddr, Port.ANALOG_OUT_ONE.toString());
        if (a1 != null && a1.getEnabled()) {
            Output analogOne = new Output();
            analogOne.setAddress((short)nodeAddr);
            analogOne.setPort(Port.ANALOG_OUT_ONE);
            analogOne.mOutputAnalogActuatorType = OutputAnalogActuatorType.getEnum(a1.getType());
            config.getOutputs().add(analogOne);
        }
    
        RawPoint a2 = SmartNode.getPhysicalPoint(nodeAddr, Port.ANALOG_OUT_TWO.toString());
        if (a2 != null && a2.getEnabled()) {
            Output analogTwo = new Output();
            analogTwo.setAddress((short)nodeAddr);
            analogTwo.setPort(Port.ANALOG_OUT_TWO);
            analogTwo.mOutputAnalogActuatorType = OutputAnalogActuatorType.getEnum(a2.getType());
            config.getOutputs().add(analogTwo);
        }
        
        return config;
    }
    
    public void update(DabProfileConfiguration config) {
        for (Output op : config.getOutputs()) {
            switch (op.getPort()) {
                case ANALOG_OUT_ONE:
                    CcuLog.d(L.TAG_CCU_ZONE, " Update analog" + op.getPort() + " type " + op.getAnalogActuatorType());
                    SmartNode.updatePhysicalPointType(nodeAddr, op.getPort().toString(), op.getAnalogActuatorType());
                    break;
                case ANALOG_OUT_TWO:
                    CcuLog.d(L.TAG_CCU_ZONE, " Update analog" + op.getPort() + " type " + op.getAnalogActuatorType());
                    SmartNode.updatePhysicalPointType(nodeAddr, op.getPort().toString(), op.getAnalogActuatorType());
                    break;
            }
        }
        
    
        setConfigNumVal("damper and type and primary",config.damper1Type);
        setConfigNumVal("damper and size and primary",config.damper1Size);
        setConfigNumVal("damper and shape and primary",config.damper1Shape);
        setConfigNumVal("damper and type and secondary",config.damper2Type);
        setConfigNumVal("damper and size and secondary",config.damper2Size);
        setConfigNumVal("damper and shape and secondary",config.damper2Shape);
        
        setConfigNumVal("enable and occupancy",config.enableOccupancyControl == true ? 1.0 : 0);
        setHisVal("enable and occupancy",config.enableOccupancyControl == true ? 1.0 : 0);
        setConfigNumVal("enable and co2",config.enableCO2Control == true ? 1.0 : 0);
        setHisVal("enable and co2",config.enableCO2Control == true ? 1.0 : 0);
        setConfigNumVal("enable and co2",config.enableCO2Control == true ? 1.0 : 0);
        setHisVal("enable and co2",config.enableCO2Control == true ? 1.0 : 0);
        setConfigNumVal("priority",config.getPriority().ordinal());
        setHisVal("priority",config.getPriority().ordinal());
        setConfigNumVal("temperature and offset",config.temperaturOffset);
        setDamperLimit("cooling","min",config.minDamperCooling);
        setHisVal("cooling and min and damper and pos",config.minDamperCooling);
        setDamperLimit("cooling","max",config.maxDamperCooling);
        setHisVal("cooling and max and damper and pos",config.maxDamperCooling);
        setDamperLimit("heating","min",config.minDamperHeating);
        setHisVal("heating and min and damper and pos",config.minDamperHeating);
        setDamperLimit("heating","max",config.maxDamperHeating);
        setHisVal("heating and max and damper and pos",config.maxDamperHeating);
    }
    
    public void setConfigNumVal(String tags,double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and zone and config and dab and "+tags+" and group == \""+nodeAddr+"\"", val);
    }
    
    public double getConfigNumVal(String tags) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and dab and "+tags+" and group == \""+nodeAddr+"\"");
    }
    
    public void setHisVal(String tags,double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and zone and config and dab and "+tags+" and group == \""+nodeAddr+"\"", val);
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
    
    public CO2Loop getCo2Loop()
    {
        return co2Loop;
    }
    public VOCLoop getVOCLoop()
    {
        return vocLoop;
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
        //CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, "ccu", HNum.make(desiredTemp), HNum.make(0));
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }
    
    public double getDamperLimit(String coolHeat, String minMax)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and config and damper and pos and "+coolHeat+" and "+minMax+" and group == \""+nodeAddr+"\"");
        if (points.size() == 0) {
            return 0;
        }
        
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        return CCUHsApi.getInstance().readDefaultValById(id);
    }
    public void setDamperLimit(String coolHeat, String minMax, double val)
    {
        ArrayList points = CCUHsApi.getInstance().readAll("point and damper and pos and "+coolHeat+" and "+minMax+" and group == \""+nodeAddr+"\"");
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, val);
        CCUHsApi.getInstance().writeHisValById(id, val);
    }
    
    public double getDamperPos()
    {
        return damperPos;
    }
    public void setDamperPos(double damperPos, String damper)
    {
        this.damperPos = damperPos;
        CCUHsApi.getInstance().writeHisValByQuery("point and damper and base and cmd and "+damper+" and group == \""+nodeAddr+"\"", damperPos);
    }
    
    public void setNormalizedDamperPos(double damperPos, String damper)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and damper and normalized and cmd and "+damper+" and group == \""+nodeAddr+"\"", damperPos);
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
            if (ScheduleProcessJob.getSystemOccupancy() == Occupancy.PRECONDITIONING) {
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
