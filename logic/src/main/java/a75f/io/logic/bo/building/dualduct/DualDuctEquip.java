package a75f.io.logic.bo.building.dualduct;

import java.util.HashMap;

import a75.io.algos.CO2Loop;
import a75.io.algos.GenericPIController;
import a75.io.algos.VOCLoop;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.dab.DabProfileConfiguration;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;

class DualDuctEquip {
    
    public int nodeAddr;
    ProfileType profileType;
    
    double damperPos= 0;
    
    GenericPIController damperController;
    CCUHsApi            hayStack = CCUHsApi.getInstance();
    String              equipRef = null;
    
    CO2Loop co2Loop;
    VOCLoop vocLoop;
    
    double   co2Target = TunerConstants.ZONE_CO2_TARGET;
    double   co2Threshold = TunerConstants.ZONE_CO2_THRESHOLD;
    double   vocTarget = TunerConstants.ZONE_VOC_TARGET;
    double   vocThreshold = TunerConstants.ZONE_VOC_THRESHOLD;
    
    public DualDuctEquip(ProfileType type, int node)
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
            damperController.setMaxAllowedError(
                TunerUtil.readTunerValByQuery("dualDuct and pspread and equipRef == \"" + equipRef + "\""));
            damperController.setIntegralGain(TunerUtil.readTunerValByQuery("dualDuct and igain and equipRef == \"" + equipRef + "\""));
            damperController.setProportionalGain(TunerUtil.readTunerValByQuery("dualDuct and pgain and equipRef == \"" + equipRef + "\""));
            damperController.setIntegralMaxTimeout((int) TunerUtil.readTunerValByQuery("dualDuct and itimeout and equipRef == \"" + equipRef + "\""));
            
            co2Target = (int) TunerUtil.readTunerValByQuery("zone and dualDuct and co2 and target and equipRef == \""+equipRef+"\"");
            co2Threshold = (int) TunerUtil.readTunerValByQuery("zone and dualDuct and co2 and threshold and equipRef == \""+equipRef+"\"");
            vocTarget = (int) TunerUtil.readTunerValByQuery("zone and dualDuct and voc and target and equipRef == \""+equipRef+"\"");
            vocThreshold = (int) TunerUtil.readTunerValByQuery("zone and dualDuct and voc and threshold and equipRef == \""+equipRef+"\"");
        }
        
    }
    
    
    public void createEntities(DualDuctProfileConfiguration config, String floorRef, String roomRef) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-DualDuct-" + nodeAddr;
        String ahuRef = null;
        HashMap systemEquip = CCUHsApi.getInstance().read("equip and system");
        if (systemEquip != null && systemEquip.size() > 0) {
            ahuRef = systemEquip.get("id").toString();
        }
    
        createEquip(siteRef, equipDis, roomRef, floorRef, ahuRef, tz);
    
        createLogicalPoints(siteRef, equipDis, roomRef, floorRef, tz , config);
    
        createUserIntentPoints(siteRef, equipDis, roomRef, floorRef, tz );
        
        createEquipOperationPoints(siteRef, equipDis, roomRef, floorRef, tz );
        
        createConfigPoints(siteRef, equipDis, tz, config);
        
        //BuildingTuners.getInstance().addEquipDabTuners(siteDis + "-DAB-" + nodeAddr, equipRef, roomRef, floorRef);
    }
    
    
    private void createEquip(String siteRef,
                             String equipDis,
                             String roomRef,
                             String floorRef,
                             String ahuRef,
                             String tz) {
        
        Equip.Builder dualDuctEquip = new Equip.Builder()
                                              .setSiteRef(siteRef)
                                              .setDisplayName(equipDis)
                                              .setRoomRef(roomRef)
                                              .setFloorRef(floorRef)
                                              .setProfile(profileType.name())
                                              .setPriority(ZonePriority.NONE.name())
                                              .addMarker("equip").addMarker("dualduct").addMarker("zone")
                                              .setAhuRef(ahuRef)
                                              .setTz(tz)
                                              .setGroup(String.valueOf(nodeAddr));
        equipRef = CCUHsApi.getInstance().addEquip(dualDuctEquip.build());
    }
    
    
    private void createLogicalPoints(String siteRef, String equipDis, String roomRef, String floorRef, String tz ,
                                     DualDuctProfileConfiguration config ) {
        Point currentTemp = new Point.Builder()
                                    .setDisplayName(equipDis+"-currentTemp")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(roomRef)
                                    .setFloorRef(floorRef)
                                    .setHisInterpolate("cov")
                                    .addMarker("zone").addMarker("dualDuct")
                                    .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("current")
                                    .addMarker("his").addMarker("cur").addMarker("logical")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setUnit("\u00B0F")
                                    .setTz(tz)
                                    .build();
        String ctID = CCUHsApi.getInstance().addPoint(currentTemp);
    
        Point humidity = new Point.Builder()
                                 .setDisplayName(equipDis+"-humidity")
                                 .setEquipRef(equipRef)
                                 .setSiteRef(siteRef)
                                 .setRoomRef(roomRef)
                                 .setFloorRef(floorRef)
                                 .setHisInterpolate("cov")
                                 .addMarker("zone").addMarker("dualDuct")
                                 .addMarker("air").addMarker("humidity").addMarker("sensor").addMarker("current")
                                 .addMarker("his").addMarker("cur").addMarker("logical")
                                 .setGroup(String.valueOf(nodeAddr))
                                 .setUnit("%")
                                 .setTz(tz)
                                 .build();
        String humidityId = CCUHsApi.getInstance().addPoint(humidity);
    
        Point co2 = new Point.Builder()
                            .setDisplayName(equipDis+"-co2")
                            .setEquipRef(equipRef)
                            .setSiteRef(siteRef)
                            .setRoomRef(roomRef)
                            .setFloorRef(floorRef)
                            .setHisInterpolate("cov")
                            .addMarker("zone").addMarker("dualDuct")
                            .addMarker("air").addMarker("co2").addMarker("sensor").addMarker("current")
                            .addMarker("his").addMarker("cur").addMarker("logical")
                            .setGroup(String.valueOf(nodeAddr))
                            .setUnit("ppm")
                            .setTz(tz)
                            .build();
        String co2Id = CCUHsApi.getInstance().addPoint(co2);
    
        Point voc = new Point.Builder()
                            .setDisplayName(equipDis+"-voc")
                            .setEquipRef(equipRef)
                            .setSiteRef(siteRef)
                            .setRoomRef(roomRef)
                            .setFloorRef(floorRef)
                            .setHisInterpolate("cov")
                            .addMarker("zone").addMarker("dualDuct")
                            .addMarker("air").addMarker("voc").addMarker("sensor").addMarker("current")
                            .addMarker("his").addMarker("cur").addMarker("logical")
                            .setGroup(String.valueOf(nodeAddr))
                            .setUnit("ppb")
                            .setTz(tz)
                            .build();
        String vocId = CCUHsApi.getInstance().addPoint(voc);
    
        Point damper1Pos = new Point.Builder()
                                   .setDisplayName(equipDis+"-coolingDamperPos")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .setRoomRef(roomRef)
                                   .setFloorRef(floorRef)
                                   .setHisInterpolate("cov")
                                   .addMarker("damper").addMarker("cooling").addMarker("pos").addMarker("dualDuct")
                                   .addMarker("cmd").addMarker("his").addMarker("logical").addMarker("zone")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setUnit("%")
                                   .setTz(tz)
                                   .build();
        String coolingDamperPosId = CCUHsApi.getInstance().addPoint(damper1Pos);
    
        Point damper2Pos = new Point.Builder()
                                   .setDisplayName(equipDis+"-heatingDamperPos")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .setRoomRef(roomRef)
                                   .setFloorRef(floorRef)
                                   .setHisInterpolate("cov")
                                   .addMarker("damper").addMarker("heating").addMarker("pos").addMarker("dualDuct")
                                   .addMarker("cmd").addMarker("his").addMarker("logical").addMarker("zone")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setUnit("%")
                                   .setTz(tz)
                                   .build();
        String heatingDamperPosId = CCUHsApi.getInstance().addPoint(damper2Pos);
    
        Point dischargeAirTemp1 = new Point.Builder()
                                          .setDisplayName(equipDis+"-dischargeAirTemp1")
                                          .setEquipRef(equipRef)
                                          .setSiteRef(siteRef)
                                          .setRoomRef(roomRef)
                                          .setFloorRef(floorRef)
                                          .addMarker("zone").addMarker("dualDuct").setHisInterpolate("cov")
                                          .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("discharge")
                                          .addMarker("primary").addMarker("his").addMarker("logical")
                                          .setGroup(String.valueOf(nodeAddr))
                                          .setUnit("\u00B0F")
                                          .setTz(tz)
                                          .build();
        String dat1Id = CCUHsApi.getInstance().addPoint(dischargeAirTemp1);
        CCUHsApi.getInstance().writeHisValById(dat1Id, 0.0);
    
        Point dischargeAirTemp2 = new Point.Builder()
                                          .setDisplayName(equipDis+"-dischargeAirTemp2")
                                          .setEquipRef(equipRef)
                                          .setSiteRef(siteRef)
                                          .setRoomRef(roomRef)
                                          .setFloorRef(floorRef)
                                          .addMarker("zone").addMarker("dualDuct").setHisInterpolate("cov")
                                          .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("discharge")
                                          .addMarker("secondary").addMarker("his").addMarker("logical")
                                          .setGroup(String.valueOf(nodeAddr))
                                          .setUnit("\u00B0F")
                                          .setTz(tz)
                                          .build();
        String dat2Id = CCUHsApi.getInstance().addPoint(dischargeAirTemp2);
        CCUHsApi.getInstance().writeHisValById(dat2Id, 0.0);
    
        Point desiredTemp = new Point.Builder()
                                .setDisplayName(equipDis+"-desiredTemp")
                                .setEquipRef(equipRef)
                                .setSiteRef(siteRef)
                                .setRoomRef(roomRef)
                                .setFloorRef(floorRef)
                                .setHisInterpolate("cov")
                                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("dualDuct").addMarker("average")
                                .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                                .setGroup(String.valueOf(nodeAddr))
                                .setUnit("\u00B0F")
                                .setTz(tz)
                                .build();
        String dtId = CCUHsApi.getInstance().addPoint(desiredTemp);
        
        
        SmartNode device = new SmartNode(nodeAddr, siteRef, floorRef, roomRef, equipRef);
        device.currentTemp.setPointRef(ctID);
        device.currentTemp.setEnabled(true);
        device.desiredTemp.setPointRef(dtId);
        device.desiredTemp.setEnabled(true);
        device.th1In.setPointRef(dat1Id);
        device.th1In.setEnabled(true);
        device.th2In.setPointRef(dat2Id);
        device.th2In.setEnabled(true);
    
        //TODO-
        for (Output op : config.getOutputs()) {
            switch (op.getPort()) {
                case ANALOG_OUT_ONE:
                    device.analog1Out.setType(op.getAnalogActuatorType());
                    break;
                case ANALOG_OUT_TWO:
                    device.analog2Out.setType(op.getAnalogActuatorType());
                    break;
            }
        }
        device.analog1Out.setEnabled(config.isOpConfigured(Port.ANALOG_OUT_ONE));
        device.analog1Out.setPointRef(coolingDamperPosId);
        device.analog2Out.setEnabled(config.isOpConfigured(Port.ANALOG_OUT_TWO));
        device.analog2Out.setPointRef(heatingDamperPosId);
    
        device.addSensor(Port.SENSOR_RH, humidityId);
        device.addSensor(Port.SENSOR_CO2, co2Id);
        device.addSensor(Port.SENSOR_VOC, vocId);
    
        device.addPointsToDb();
    
    
//        setCurrentTemp(0);
//        setDamperPos(0, "primary");
//        setDamperPos(0, "secondary");
//        setDesiredTempCooling(74.0);
//        setDesiredTemp(72.0);
//        setDesiredTempHeating(70.0);
//        setDesiredTempHeating(70.0);
//        setHumidity(0);
//        setCO2(0);
//        setVOC(0);
    }
    
    private void createUserIntentPoints(String siteRef, String equipDis, String roomRef, String floorRef, String tz) {
    
        Point desiredTempCooling = new Point.Builder()
                                           .setDisplayName(equipDis+"-desiredTempCooling")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .setHisInterpolate("cov")
                                           .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("dualDuct").addMarker("cooling")
                                           .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setUnit("\u00B0F")
                                           .setTz(tz)
                                           .build();
        CCUHsApi.getInstance().addPoint(desiredTempCooling);
    
        Point desiredTempHeating = new Point.Builder()
                                           .setDisplayName(equipDis+"-desiredTempHeating")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .setHisInterpolate("cov")
                                           .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("dualDuct").addMarker("heating")
                                           .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setUnit("\u00B0F")
                                           .setTz(tz)
                                           .build();
        CCUHsApi.getInstance().addPoint(desiredTempHeating);
    
        Point scheduleType = new Point.Builder()
                                      .setDisplayName(equipDis+"-scheduleType")
                                      .setEquipRef(equipRef)
                                      .setSiteRef(siteRef)
                                      .setRoomRef(roomRef)
                                      .setFloorRef(floorRef).setHisInterpolate("cov")
                                      .addMarker("zone").addMarker("dualDuct").addMarker("scheduleType").addMarker("writable").addMarker("his")
                                      .setGroup(String.valueOf(nodeAddr))
                                      .setEnums("building,zone,named")
                                      .setTz(tz)
                                      .build();
        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(scheduleType);
        CCUHsApi.getInstance().writeDefaultValById(equipScheduleTypeId, 0.0);
        CCUHsApi.getInstance().writeHisValById(equipScheduleTypeId, 0.0);
    }
    
    private void createEquipOperationPoints(String siteRef, String equipDis, String roomRef, String floorRef, String tz) {
        
        Point occupancy = new Point.Builder()
                                  .setDisplayName(equipDis+"-occupancy")
                                  .setEquipRef(equipRef)
                                  .setSiteRef(siteRef)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef).setHisInterpolate("cov")
                                  .addMarker("dualDuct").addMarker("occupancy").addMarker("mode").addMarker("zone").addMarker("his")
                                  .setEnums("unoccupied,occupied,preconditioning,forcedoccupied,vacation,occupancysensing")
                                  .setGroup(String.valueOf(nodeAddr))
                                  .setTz(tz)
                                  .build();
        String occupancyId = CCUHsApi.getInstance().addPoint(occupancy);
        CCUHsApi.getInstance().writeHisValById(occupancyId, 0.0);
    
        Point equipStatus = new Point.Builder()
                                    .setDisplayName(equipDis+"-equipStatus")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(roomRef)
                                    .setFloorRef(floorRef).setHisInterpolate("cov")
                                    .addMarker("status").addMarker("his").addMarker("dab").addMarker("logical").addMarker("zone")
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
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("status").addMarker("message").addMarker("dualDuct").addMarker("writable").addMarker("logical").addMarker("zone")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setTz(tz)
                                           .setKind("string")
                                           .build();
        CCUHsApi.getInstance().addPoint(equipStatusMessage);
        Point equipScheduleStatus = new Point.Builder()
                                            .setDisplayName(equipDis+"-equipScheduleStatus")
                                            .setEquipRef(equipRef)
                                            .setSiteRef(siteRef)
                                            .setRoomRef(roomRef)
                                            .setFloorRef(floorRef).setHisInterpolate("cov")
                                            .addMarker("scheduleStatus").addMarker("logical").addMarker("dualDuct").addMarker("zone").addMarker("writable").addMarker("his")
                                            .setGroup(String.valueOf(nodeAddr))
                                            .setTz(tz)
                                            .setKind("string")
                                            .build();
        CCUHsApi.getInstance().addPoint(equipScheduleStatus);
        
        
    }
    
    private void createConfigPoints(String siteRef,
                                    String equipDis,
                                    String tz,
                                    DualDuctProfileConfiguration config) {
        
        Point minHeatingDamperPos = new Point.Builder()
                                             .setDisplayName(equipDis+"-minHeatingDamperPos")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef).setHisInterpolate("cov")
                                             .addMarker("config").addMarker("dualDuct").addMarker("damper").addMarker("min")
                                             .addMarker("heating").addMarker("pos")
                                             .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                             .setKind(DualDuctConstants.KIND_STRING)
                                             .setMinVal(String.valueOf(DualDuctConstants.MIN_DAMPER_POS))
                                             .setMaxVal(String.valueOf(DualDuctConstants.MAX_DAMPER_POS))
                                             .setIncrementVal(String.valueOf(DualDuctConstants.DAMPER_POS_INC))
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setUnit("%")
                                             .setTz(tz)
                                             .build();
        String minHeatingDamperPosId = CCUHsApi.getInstance().addPoint(minHeatingDamperPos);
        CCUHsApi.getInstance().writeDefaultValById(minHeatingDamperPosId, (double)config.getMinHeatingDamperPos());
        CCUHsApi.getInstance().writeHisValById(minHeatingDamperPosId, (double)config.getMinHeatingDamperPos());
    
        Point maxHeatingDamperPos = new Point.Builder()
                                            .setDisplayName(equipDis+"-maxHeatingDamperPos")
                                            .setEquipRef(equipRef)
                                            .setSiteRef(siteRef).setHisInterpolate("cov")
                                            .addMarker("config").addMarker("dualDuct").addMarker("damper").addMarker("max")
                                            .addMarker("heating").addMarker("pos")
                                            .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                            .setKind(DualDuctConstants.KIND_STRING)
                                            .setMinVal(String.valueOf(DualDuctConstants.MIN_DAMPER_POS))
                                            .setMaxVal(String.valueOf(DualDuctConstants.MAX_DAMPER_POS))
                                            .setIncrementVal(String.valueOf(DualDuctConstants.DAMPER_POS_INC))
                                            .setGroup(String.valueOf(nodeAddr))
                                            .setUnit("%")
                                            .setTz(tz)
                                            .build();
        String maxHeatingDamperPosId = CCUHsApi.getInstance().addPoint(maxHeatingDamperPos);
        CCUHsApi.getInstance().writeDefaultValById(maxHeatingDamperPosId, (double)config.getMaxHeatingDamperPos());
        CCUHsApi.getInstance().writeHisValById(maxHeatingDamperPosId, (double)config.getMaxHeatingDamperPos());
    
        Point minCoolingDamperPos = new Point.Builder()
                                            .setDisplayName(equipDis+"-minCoolingDamperPos")
                                            .setEquipRef(equipRef)
                                            .setSiteRef(siteRef).setHisInterpolate("cov")
                                            .addMarker("config").addMarker("dualDuct").addMarker("damper").addMarker("min")
                                            .addMarker("cooling").addMarker("pos")
                                            .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                            .setKind(DualDuctConstants.KIND_STRING)
                                            .setMinVal(String.valueOf(DualDuctConstants.MIN_DAMPER_POS))
                                            .setMaxVal(String.valueOf(DualDuctConstants.MAX_DAMPER_POS))
                                            .setIncrementVal(String.valueOf(DualDuctConstants.DAMPER_POS_INC))
                                            .setGroup(String.valueOf(nodeAddr))
                                            .setUnit("%")
                                            .setTz(tz)
                                            .build();
        String minCoolingDamperPosId = CCUHsApi.getInstance().addPoint(minCoolingDamperPos);
        CCUHsApi.getInstance().writeDefaultValById(minCoolingDamperPosId, (double)config.getMinCoolingDamperPos());
        CCUHsApi.getInstance().writeHisValById(minCoolingDamperPosId, (double)config.getMinCoolingDamperPos());
    
        Point maxCoolingDamperPos = new Point.Builder()
                                            .setDisplayName(equipDis+"-maxCoolingDamperPos")
                                            .setEquipRef(equipRef)
                                            .setSiteRef(siteRef).setHisInterpolate("cov")
                                            .addMarker("config").addMarker("dualDuct").addMarker("damper").addMarker("max")
                                            .addMarker("cooling").addMarker("pos")
                                            .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                            .setKind(DualDuctConstants.KIND_STRING)
                                            .setMinVal(String.valueOf(DualDuctConstants.MIN_DAMPER_POS))
                                            .setMaxVal(String.valueOf(DualDuctConstants.MAX_DAMPER_POS))
                                            .setIncrementVal(String.valueOf(DualDuctConstants.DAMPER_POS_INC))
                                            .setGroup(String.valueOf(nodeAddr))
                                            .setUnit("%")
                                            .setTz(tz)
                                            .build();
        String maxCoolingDamperPosId = CCUHsApi.getInstance().addPoint(maxCoolingDamperPos);
        CCUHsApi.getInstance().writeDefaultValById(maxCoolingDamperPosId, (double)config.getMaxCoolingDamperPos());
        CCUHsApi.getInstance().writeHisValById(maxCoolingDamperPosId, (double)config.getMaxCoolingDamperPos());
    
        Point enableOccupancyControl = new Point.Builder()
                                               .setDisplayName(equipDis+"-enableOccupancyControl")
                                               .setEquipRef(equipRef)
                                               .setSiteRef(siteRef).setHisInterpolate("cov")
                                               .addMarker("config").addMarker("dualDuct").addMarker("writable").addMarker("zone")
                                               .addMarker("enable").addMarker("occupancy").addMarker("control").addMarker("his").addMarker("sp")
                                               .setKind(DualDuctConstants.KIND_STRING)
                                               .setGroup(String.valueOf(nodeAddr))
                                               .setEnums("false,true")
                                               .setTz(tz)
                                               .build();
        String enableOccupancyControlId = CCUHsApi.getInstance().addPoint(enableOccupancyControl);
        CCUHsApi.getInstance().writeDefaultValById(enableOccupancyControlId, config.isEnableOccupancyControl() ? 1.0 :0);
        CCUHsApi.getInstance().writeHisValById(enableOccupancyControlId, config.isEnableOccupancyControl() ? 1.0 :0);
    
        Point enableCO2Control = new Point.Builder()
                                         .setDisplayName(equipDis+"-enableCO2Control")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef).setHisInterpolate("cov")
                                         .addMarker("config").addMarker("dualDuct").addMarker("writable").addMarker("zone")
                                         .addMarker("enable").addMarker("co2").addMarker("control").addMarker("sp").addMarker("his")
                                         .setKind(DualDuctConstants.KIND_STRING)
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setEnums("false,true")
                                         .setTz(tz)
                                         .build();
        String enableCO2ControlId = CCUHsApi.getInstance().addPoint(enableCO2Control);
        CCUHsApi.getInstance().writeDefaultValById(enableCO2ControlId, config.isEnableCO2Control() ? 1.0 :0);
        CCUHsApi.getInstance().writeHisValById(enableCO2ControlId, config.isEnableCO2Control() ? 1.0 :0);
    
        Point enableIAQControl = new Point.Builder()
                                         .setDisplayName(equipDis+"-enableIAQControl")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef).setHisInterpolate("cov")
                                         .addMarker("config").addMarker("dualDuct").addMarker("writable").addMarker("zone")
                                         .addMarker("enable").addMarker("iaq").addMarker("control").addMarker("sp").addMarker("his")
                                         .setKind(DualDuctConstants.KIND_STRING)
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setEnums("false,true")
                                         .setTz(tz)
                                         .build();
        String enableIAQControlId = CCUHsApi.getInstance().addPoint(enableIAQControl);
        CCUHsApi.getInstance().writeDefaultValById(enableIAQControlId, config.isEnableIAQControl() ? 1.0 :0);
        CCUHsApi.getInstance().writeHisValById(enableIAQControlId, config.isEnableIAQControl() ? 1.0 :0);
    
        Point temperatureOffset = new Point.Builder()
                                         .setDisplayName(equipDis+"-temperatureOffset")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef).setHisInterpolate("cov")
                                         .addMarker("config").addMarker("dualDuct").addMarker("writable").addMarker("zone")
                                         .addMarker("temperature").addMarker("offset").addMarker("sp").addMarker("his")
                                         .setKind(DualDuctConstants.KIND_STRING)
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setUnit("\u00B0F")
                                         .setTz(tz)
                                         .build();
        String temperatureOffsetId = CCUHsApi.getInstance().addPoint(temperatureOffset);
        CCUHsApi.getInstance().writeDefaultValById(temperatureOffsetId, config.getTemperatureOffset());
        CCUHsApi.getInstance().writeHisValById(temperatureOffsetId, config.getTemperatureOffset());
    
        createOutputConfigPoints(siteRef, equipDis, tz, config);
        createAnalog1OutputConfigPoints(siteRef, equipDis, tz, config);
        createAnalog2OutputConfigPoints(siteRef, equipDis, tz, config);
        
    }
    
    private void createOutputConfigPoints(String siteRef,
                                                 String equipDis,
                                                 String tz,
                                                 DualDuctProfileConfiguration config) {
    
        Point analog1OutputConfig = new Point.Builder()
                                            .setDisplayName(equipDis+"-analog1OutputConfig")
                                            .setEquipRef(equipRef)
                                            .setSiteRef(siteRef).setHisInterpolate("cov")
                                            .addMarker("config").addMarker("dualDuct").addMarker("writable").addMarker("zone")
                                            .addMarker("analog1").addMarker("output").addMarker("sp").addMarker("his")
                                            .setKind(DualDuctConstants.KIND_STRING)
                                            .setGroup(String.valueOf(nodeAddr))
                                            .setTz(tz)
                                            .build();
        String analog1OutputConfigId = CCUHsApi.getInstance().addPoint(analog1OutputConfig);
        CCUHsApi.getInstance().writeDefaultValById(analog1OutputConfigId, (double)config.getAnalogOut1Config());
        CCUHsApi.getInstance().writeHisValById(analog1OutputConfigId, (double)config.getAnalogOut1Config());
    
        Point analog2OutputConfig = new Point.Builder()
                                            .setDisplayName(equipDis+"-analog1OutputConfig")
                                            .setEquipRef(equipRef)
                                            .setSiteRef(siteRef).setHisInterpolate("cov")
                                            .addMarker("config").addMarker("dualDuct").addMarker("writable").addMarker("zone")
                                            .addMarker("analog2").addMarker("output").addMarker("sp").addMarker("his")
                                            .setKind(DualDuctConstants.KIND_STRING)
                                            .setGroup(String.valueOf(nodeAddr))
                                            .setTz(tz)
                                            .build();
        String analog2OutputConfigId = CCUHsApi.getInstance().addPoint(analog2OutputConfig);
        CCUHsApi.getInstance().writeDefaultValById(analog2OutputConfigId, (double)config.getAnalogOut2Config());
        CCUHsApi.getInstance().writeHisValById(analog2OutputConfigId, (double)config.getAnalogOut2Config());
    
    
        Point thermistor1OutputConfig = new Point.Builder()
                                                .setDisplayName(equipDis+"-thermistor1OutputConfig")
                                                .setEquipRef(equipRef)
                                                .setSiteRef(siteRef).setHisInterpolate("cov")
                                                .addMarker("config").addMarker("dualDuct").addMarker("writable").addMarker("zone")
                                                .addMarker("th1").addMarker("output").addMarker("sp").addMarker("his")
                                                .setKind(DualDuctConstants.KIND_STRING)
                                                .setGroup(String.valueOf(nodeAddr))
                                                .setTz(tz)
                                                .build();
        String thermistor1OutputConfigId = CCUHsApi.getInstance().addPoint(thermistor1OutputConfig);
        CCUHsApi.getInstance().writeDefaultValById(thermistor1OutputConfigId, (double)config.getThermistor1Config());
        CCUHsApi.getInstance().writeHisValById(thermistor1OutputConfigId, (double)config.getThermistor1Config());
    
        Point thermistor2OutputConfig = new Point.Builder()
                                                .setDisplayName(equipDis+"-thermistor2OutputConfig")
                                                .setEquipRef(equipRef)
                                                .setSiteRef(siteRef).setHisInterpolate("cov")
                                                .addMarker("config").addMarker("dualDuct").addMarker("writable").addMarker("zone")
                                                .addMarker("th2").addMarker("output").addMarker("sp").addMarker("his")
                                                .setKind(DualDuctConstants.KIND_STRING)
                                                .setGroup(String.valueOf(nodeAddr))
                                                .setTz(tz)
                                                .build();
        String thermistor2OutputConfigId = CCUHsApi.getInstance().addPoint(thermistor2OutputConfig);
        CCUHsApi.getInstance().writeDefaultValById(thermistor2OutputConfigId, (double)config.getThermistor2Config());
        CCUHsApi.getInstance().writeHisValById(thermistor2OutputConfigId, (double)config.getThermistor2Config());
    
    
    
    }
    
    private void createAnalog1OutputConfigPoints(String siteRef,
                                                 String equipDis,
                                                 String tz,
                                                 DualDuctProfileConfiguration config) {
        
        Point analog1OutputAtMinDamperHeatingPos = new Point.Builder()
                                                       .setDisplayName(equipDis+"-analog1OutputAtMinDamperHeatingPos")
                                                       .setEquipRef(equipRef)
                                                       .setSiteRef(siteRef).setHisInterpolate("cov")
                                                       .addMarker("config").addMarker("dualDuct").addMarker("writable")
                                                       .addMarker("zone").addMarker("analog1").addMarker("output")
                                                       .addMarker("min").addMarker("damper").addMarker("heating")
                                                       .addMarker("pos").addMarker("sp").addMarker("his")
                                                       .setKind(DualDuctConstants.KIND_STRING)
                                                       .setMinVal(String.valueOf(DualDuctConstants.ANALOG_MIN_FOR_DAMPER_POS))
                                                       .setMaxVal(String.valueOf(DualDuctConstants.ANALOG_MAX_FOR_DAMPER_POS))
                                                       .setIncrementVal(String.valueOf(DualDuctConstants.ANALOG_FOR_DAMPER_INC))
                                                       .setGroup(String.valueOf(nodeAddr))
                                                       .setTz(tz)
                                                       .build();
        String analog1OutputAtMinDamperHeatingPosId = CCUHsApi.getInstance().addPoint(analog1OutputAtMinDamperHeatingPos);
        CCUHsApi.getInstance().writeDefaultValById(analog1OutputAtMinDamperHeatingPosId,
                                                   (double)config.getAnalog1OutAtMinDamperHeating());
        CCUHsApi.getInstance().writeHisValById(analog1OutputAtMinDamperHeatingPosId,
                                               (double)config.getAnalog1OutAtMinDamperHeating());
    
    
        Point analog1OutputAtMaxDamperHeatingPos = new Point.Builder()
                                                       .setDisplayName(equipDis+"-analog1OutputAtMaxDamperHeatingPos")
                                                       .setEquipRef(equipRef)
                                                       .setSiteRef(siteRef).setHisInterpolate("cov")
                                                       .addMarker("config").addMarker("dualDuct").addMarker("writable")
                                                       .addMarker("zone").addMarker("analog1").addMarker("output")
                                                       .addMarker("max").addMarker("damper").addMarker("heating")
                                                       .addMarker("pos").addMarker("sp").addMarker("his")
                                                       .setKind(DualDuctConstants.KIND_STRING)
                                                       .setMinVal(String.valueOf(DualDuctConstants.ANALOG_MIN_FOR_DAMPER_POS))
                                                       .setMaxVal(String.valueOf(DualDuctConstants.ANALOG_MAX_FOR_DAMPER_POS))
                                                       .setIncrementVal(String.valueOf(DualDuctConstants.ANALOG_FOR_DAMPER_INC))
                                                       .setGroup(String.valueOf(nodeAddr))
                                                       .setTz(tz)
                                                       .build();
        String analog1OutputAtMaxDamperHeatingPosId = CCUHsApi.getInstance().addPoint(analog1OutputAtMaxDamperHeatingPos);
        CCUHsApi.getInstance().writeDefaultValById(analog1OutputAtMaxDamperHeatingPosId,
                                                   (double)config.getAnalog1OutAtMaxDamperHeating());
        CCUHsApi.getInstance().writeHisValById(analog1OutputAtMaxDamperHeatingPosId,
                                               (double)config.getAnalog1OutAtMaxDamperHeating());
    
        Point analog1OutputAtMinDamperCoolingPos = new Point.Builder()
                                                       .setDisplayName(equipDis+"-analog1OutputAtMinDamperCoolingPos")
                                                       .setEquipRef(equipRef)
                                                       .setSiteRef(siteRef).setHisInterpolate("cov")
                                                       .addMarker("config").addMarker("dualDuct").addMarker("writable")
                                                       .addMarker("zone").addMarker("analog1").addMarker("output")
                                                       .addMarker("min").addMarker("damper").addMarker("cooling")
                                                       .addMarker("pos").addMarker("sp").addMarker("his")
                                                       .setKind(DualDuctConstants.KIND_STRING)
                                                       .setMinVal(String.valueOf(DualDuctConstants.ANALOG_MIN_FOR_DAMPER_POS))
                                                       .setMaxVal(String.valueOf(DualDuctConstants.ANALOG_MAX_FOR_DAMPER_POS))
                                                       .setIncrementVal(String.valueOf(DualDuctConstants.ANALOG_FOR_DAMPER_INC))
                                                       .setGroup(String.valueOf(nodeAddr))
                                                       .setTz(tz)
                                                       .build();
        String analog1OutputAtMinDamperCoolingPosId = CCUHsApi.getInstance().addPoint(analog1OutputAtMinDamperCoolingPos);
        CCUHsApi.getInstance().writeDefaultValById(analog1OutputAtMinDamperCoolingPosId,
                                                   (double)config.getAnalog1OutAtMinDamperCooling());
        CCUHsApi.getInstance().writeHisValById(analog1OutputAtMinDamperCoolingPosId,
                                               (double)config.getAnalog1OutAtMinDamperCooling());
    
    
        Point analog1OutputAtMaxDamperCoolingPos = new Point.Builder()
                                                       .setDisplayName(equipDis+"-analog1OutputAtMaxDamperCoolingPos")
                                                       .setEquipRef(equipRef)
                                                       .setSiteRef(siteRef).setHisInterpolate("cov")
                                                       .addMarker("config").addMarker("dualDuct").addMarker("writable")
                                                       .addMarker("zone").addMarker("analog1").addMarker("output")
                                                       .addMarker("max").addMarker("damper").addMarker("cooling")
                                                       .addMarker("pos").addMarker("sp").addMarker("his")
                                                       .setKind(DualDuctConstants.KIND_STRING)
                                                       .setMinVal(String.valueOf(DualDuctConstants.ANALOG_MIN_FOR_DAMPER_POS))
                                                       .setMaxVal(String.valueOf(DualDuctConstants.ANALOG_MAX_FOR_DAMPER_POS))
                                                       .setIncrementVal(String.valueOf(DualDuctConstants.ANALOG_FOR_DAMPER_INC))
                                                       .setGroup(String.valueOf(nodeAddr))
                                                       .setTz(tz)
                                                       .build();
        String analog1OutputAtMaxDamperCoolingPosId = CCUHsApi.getInstance().addPoint(analog1OutputAtMaxDamperCoolingPos);
        CCUHsApi.getInstance().writeDefaultValById(analog1OutputAtMaxDamperCoolingPosId,
                                                   (double)config.getAnalog1OutAtMaxDamperCooling());
        CCUHsApi.getInstance().writeHisValById(analog1OutputAtMaxDamperCoolingPosId,
                                               (double)config.getAnalog1OutAtMaxDamperCooling());
    
    }
    
    private void createAnalog2OutputConfigPoints(String siteRef,
                                                 String equipDis,
                                                 String tz,
                                                 DualDuctProfileConfiguration config) {
        
        Point analog2OutputAtMinDamperHeatingPos = new Point.Builder()
                                                       .setDisplayName(equipDis+"-analog2OutputAtMinDamperHeatingPos")
                                                       .setEquipRef(equipRef)
                                                       .setSiteRef(siteRef).setHisInterpolate("cov")
                                                       .addMarker("config").addMarker("dualDuct").addMarker("writable")
                                                       .addMarker("zone").addMarker("analog2").addMarker("output")
                                                       .addMarker("min").addMarker("damper").addMarker("heating")
                                                       .addMarker("pos").addMarker("sp").addMarker("his")
                                                       .setKind(DualDuctConstants.KIND_STRING)
                                                       .setMinVal(String.valueOf(DualDuctConstants.ANALOG_MIN_FOR_DAMPER_POS))
                                                       .setMaxVal(String.valueOf(DualDuctConstants.ANALOG_MAX_FOR_DAMPER_POS))
                                                       .setIncrementVal(String.valueOf(DualDuctConstants.ANALOG_FOR_DAMPER_INC))
                                                       .setGroup(String.valueOf(nodeAddr))
                                                       .setTz(tz)
                                                       .build();
        String analog2OutputAtMinDamperHeatingPosId = CCUHsApi.getInstance().addPoint(analog2OutputAtMinDamperHeatingPos);
        CCUHsApi.getInstance().writeDefaultValById(analog2OutputAtMinDamperHeatingPosId,
                                                                        (double)config.getAnalog2OutAtMinDamperHeating());
        CCUHsApi.getInstance().writeHisValById(analog2OutputAtMinDamperHeatingPosId,
                                                                        (double)config.getAnalog2OutAtMinDamperHeating());
        
        
        Point analog2OutputAtMaxDamperHeatingPos = new Point.Builder()
                                                       .setDisplayName(equipDis+"-analog2OutputAtMaxDamperHeatingPos")
                                                       .setEquipRef(equipRef)
                                                       .setSiteRef(siteRef).setHisInterpolate("cov")
                                                       .addMarker("config").addMarker("dualDuct").addMarker("writable")
                                                       .addMarker("zone").addMarker("analog2").addMarker("output")
                                                       .addMarker("max").addMarker("damper").addMarker("heating")
                                                       .addMarker("pos").addMarker("sp").addMarker("his")
                                                       .setKind(DualDuctConstants.KIND_STRING)
                                                       .setMinVal(String.valueOf(DualDuctConstants.ANALOG_MIN_FOR_DAMPER_POS))
                                                       .setMaxVal(String.valueOf(DualDuctConstants.ANALOG_MAX_FOR_DAMPER_POS))
                                                       .setIncrementVal(String.valueOf(DualDuctConstants.ANALOG_FOR_DAMPER_INC))
                                                       .setGroup(String.valueOf(nodeAddr))
                                                       .setTz(tz)
                                                       .build();
        String analog2OutputAtMaxDamperHeatingPosId = CCUHsApi.getInstance().addPoint(analog2OutputAtMaxDamperHeatingPos);
        CCUHsApi.getInstance().writeDefaultValById(analog2OutputAtMaxDamperHeatingPosId,
                                                                        (double)config.getAnalog2OutAtMaxDamperHeating());
        CCUHsApi.getInstance().writeHisValById(analog2OutputAtMaxDamperHeatingPosId,
                                                                        (double)config.getAnalog2OutAtMaxDamperHeating());
        
        Point analog2OutputAtMinDamperCoolingPos = new Point.Builder()
                                                       .setDisplayName(equipDis+"-analog2OutputAtMinDamperCoolingPos")
                                                       .setEquipRef(equipRef)
                                                       .setSiteRef(siteRef).setHisInterpolate("cov")
                                                       .addMarker("config").addMarker("dualDuct").addMarker("writable")
                                                       .addMarker("zone").addMarker("analog2").addMarker("output")
                                                       .addMarker("min").addMarker("damper").addMarker("cooling")
                                                       .addMarker("pos").addMarker("sp").addMarker("his")
                                                       .setKind(DualDuctConstants.KIND_STRING)
                                                       .setMinVal(String.valueOf(DualDuctConstants.ANALOG_MIN_FOR_DAMPER_POS))
                                                       .setMaxVal(String.valueOf(DualDuctConstants.ANALOG_MAX_FOR_DAMPER_POS))
                                                       .setIncrementVal(String.valueOf(DualDuctConstants.ANALOG_FOR_DAMPER_INC))
                                                       .setGroup(String.valueOf(nodeAddr))
                                                       .setTz(tz)
                                                       .build();
        String analog2OutputAtMinDamperCoolingPosId = CCUHsApi.getInstance().addPoint(analog2OutputAtMinDamperCoolingPos);
        CCUHsApi.getInstance().writeDefaultValById(analog2OutputAtMinDamperCoolingPosId,
                                                                        (double)config.getAnalog2OutAtMinDamperCooling());
        CCUHsApi.getInstance().writeHisValById(analog2OutputAtMinDamperCoolingPosId,
                                                                        (double)config.getAnalog2OutAtMinDamperCooling());
        
        
        Point analog2OutputAtMaxDamperCoolingPos = new Point.Builder()
                                                       .setDisplayName(equipDis+"-analog2OutputAtMaxDamperCoolingPos")
                                                       .setEquipRef(equipRef)
                                                       .setSiteRef(siteRef).setHisInterpolate("cov")
                                                       .addMarker("config").addMarker("dualDuct").addMarker("writable")
                                                       .addMarker("zone").addMarker("analog2").addMarker("output")
                                                       .addMarker("max").addMarker("damper").addMarker("cooling")
                                                       .addMarker("pos").addMarker("sp").addMarker("his")
                                                       .setKind(DualDuctConstants.KIND_STRING)
                                                       .setMinVal(String.valueOf(DualDuctConstants.ANALOG_MIN_FOR_DAMPER_POS))
                                                       .setMaxVal(String.valueOf(DualDuctConstants.ANALOG_MAX_FOR_DAMPER_POS))
                                                       .setIncrementVal(String.valueOf(DualDuctConstants.ANALOG_FOR_DAMPER_INC))
                                                       .setGroup(String.valueOf(nodeAddr))
                                                       .setTz(tz)
                                                       .build();
        String analog2OutputAtMaxDamperCoolingPosId = CCUHsApi.getInstance().addPoint(analog2OutputAtMaxDamperCoolingPos);
        CCUHsApi.getInstance().writeDefaultValById(analog2OutputAtMaxDamperCoolingPosId,
                                                                        (double)config.getAnalog2OutAtMaxDamperCooling());
        CCUHsApi.getInstance().writeHisValById(analog2OutputAtMaxDamperCoolingPosId,
                                                                        (double)config.getAnalog2OutAtMaxDamperCooling());
        
    }
    
}
