package a75f.io.logic.bo.building.dualduct;

import java.util.HashMap;

import a75.io.algos.CO2Loop;
import a75.io.algos.GenericPIController;
import a75.io.algos.VOCLoop;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.bo.util.TemperatureProfileUtil;
import a75f.io.logic.tuners.DualDuctTuners;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;

class DualDuctEquip {
    
    public int nodeAddr;
    ProfileType profileType;
    
    GenericPIController coolingDamperController;
    GenericPIController heatingDamperController;
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
        HashMap equipMap = hayStack.read("equip and group == \"" + nodeAddr + "\"");
        
        if (equipMap != null && equipMap.size() > 0)
        {
            equipRef = equipMap.get("id").toString();
            double maxAllowedError = TunerUtil.readTunerValByQuery("dualDuct and pspread and equipRef == \"" + equipRef + "\"");
            double proportialGain = TunerUtil.readTunerValByQuery("dualDuct and pgain and equipRef == \"" + equipRef + "\"");
            double integralGain = TunerUtil.readTunerValByQuery("dualDuct and igain and equipRef == \"" + equipRef + "\"");
            double integralTimeout = TunerUtil.readTunerValByQuery("dualDuct and itimeout and equipRef == \"" + equipRef + "\"");
            
            coolingDamperController = new GenericPIController();
            coolingDamperController.setMaxAllowedError(maxAllowedError);
            coolingDamperController.setProportionalGain(proportialGain);
            coolingDamperController.setIntegralGain(integralGain);
            coolingDamperController.setIntegralMaxTimeout((int)integralTimeout);
    
            heatingDamperController = new GenericPIController();
            heatingDamperController.setMaxAllowedError(maxAllowedError);
            heatingDamperController.setProportionalGain(proportialGain);
            heatingDamperController.setIntegralGain(integralGain);
            heatingDamperController.setIntegralMaxTimeout((int)integralTimeout);
            
            co2Target = (int) TunerUtil.readTunerValByQuery("zone and dualDuct and co2 and target and equipRef == \""+equipRef+"\"");
            co2Threshold = (int) TunerUtil.readTunerValByQuery("zone and dualDuct and co2 and threshold and equipRef == \""+equipRef+"\"");
            vocTarget = (int) TunerUtil.readTunerValByQuery("zone and dualDuct and voc and target and equipRef == \""+equipRef+"\"");
            vocThreshold = (int) TunerUtil.readTunerValByQuery("zone and dualDuct and voc and threshold and equipRef == \""+equipRef+"\"");
        }
        
    }
    
    String getId(){
        return equipRef;
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
        
        DualDuctTuners.addEquipTuners(siteRef, equipDis, equipRef, roomRef, floorRef, tz);
    
        setDefaultValues();
        
        CCUHsApi.getInstance().syncPointEntityTree();
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
                                              .addMarker("equip").addMarker("dualDuct").addMarker("zone")
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
    
        Point dischargeAirTemp = new Point.Builder()
                                          .setDisplayName(equipDis+"-dischargeAirFlowTemp")
                                          .setEquipRef(equipRef)
                                          .setSiteRef(siteRef)
                                          .setRoomRef(roomRef)
                                          .setFloorRef(floorRef)
                                          .addMarker("zone").addMarker("dualDuct").setHisInterpolate("cov")
                                          .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("discharge")
                                          .addMarker("his").addMarker("logical")
                                          .setGroup(String.valueOf(nodeAddr))
                                          .setUnit("\u00B0F")
                                          .setTz(tz)
                                          .build();
        String datId = CCUHsApi.getInstance().addPoint(dischargeAirTemp);
        CCUHsApi.getInstance().writeHisValById(datId, 0.0);
    
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
        
        String supplyAirTempId = createSupplyAirTempPoint(siteRef, equipDis, roomRef, floorRef, tz, config);
        
        
    
        CCUHsApi.getInstance().writeHisValById(datId, 0.0);
        SmartNode device = new SmartNode(nodeAddr, siteRef, floorRef, roomRef, equipRef);
        device.currentTemp.setPointRef(ctID);
        device.currentTemp.setEnabled(true);
        device.desiredTemp.setPointRef(dtId);
        device.desiredTemp.setEnabled(true);
        device.th1In.setPointRef(datId);
        device.th1In.setEnabled(true);
        device.th2In.setPointRef(supplyAirTempId);
        device.th2In.setEnabled(true);
        
        for (Output op : config.getOutputs()) {
            switch (op.getPort()) {
                case ANALOG_OUT_ONE:
                    if (config.getAnalogOut1Config() == DualDuctAnalogActuator.NOT_USED.getVal()) {
                        device.analog1Out.setEnabled(false);
                    } else {
                        device.analog1Out.setEnabled(true);
                        String logicalPointId = createAnalog1LogicalPoint(siteRef, equipDis, roomRef, floorRef, tz, config);
                        
                        if (config.getAnalogOut1Config() == DualDuctAnalogActuator.COOLING.getVal()) {
                            device.analog1Out.setType(config.getAnalog1OutAtMinDamperCooling()+"-"+
                                                                config.getAnalog1OutAtMaxDamperCooling()+"v");
                        } else if (config.getAnalogOut1Config() == DualDuctAnalogActuator.HEATING.getVal()) {
                            device.analog1Out.setType(config.getAnalog1OutAtMinDamperHeating()+"-"+
                                                      config.getAnalog1OutAtMaxDamperHeating()+"v");
                        }
                        device.analog1Out.setPointRef(logicalPointId);
                    }
                    break;
                case ANALOG_OUT_TWO:
                    if (config.getAnalogOut2Config() == DualDuctAnalogActuator.NOT_USED.getVal()) {
                        device.analog2Out.setEnabled(false);
                    } else {
                        device.analog2Out.setEnabled(true);
                        String logicalPointId = createAnalog2LogicalPoint(siteRef, equipDis, roomRef, floorRef, tz, config);
    
                        if (config.getAnalogOut2Config() == DualDuctAnalogActuator.COOLING.getVal()) {
                            device.analog1Out.setType(config.getAnalog2OutAtMinDamperCooling()+"-"+
                                                                    config.getAnalog2OutAtMaxDamperCooling()+"v");
                        } else if (config.getAnalogOut2Config() == DualDuctAnalogActuator.HEATING.getVal()) {
                            device.analog1Out.setType(config.getAnalog2OutAtMinDamperHeating()+"-"+
                                                                    config.getAnalog2OutAtMaxDamperHeating()+"v");
                        }
                        device.analog2Out.setPointRef(logicalPointId);
                    }
                    break;
            }
        }
    
        device.addSensor(Port.SENSOR_RH, humidityId);
        device.addSensor(Port.SENSOR_CO2, co2Id);
        device.addSensor(Port.SENSOR_VOC, vocId);
    
        device.addPointsToDb();
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
                                    .addMarker("status").addMarker("his").addMarker("dualDuct").addMarker("logical").addMarker("zone")
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
                                             .addMarker("heating").addMarker("pos").addMarker("limit")
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
                                            .addMarker("heating").addMarker("pos").addMarker("limit")
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
                                            .addMarker("cooling").addMarker("pos").addMarker("limit")
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
                                            .addMarker("cooling").addMarker("pos").addMarker("limit")
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
                                            .addMarker("analog1").addMarker("output").addMarker("type")
                                            .addMarker("sp").addMarker("his")
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
                                            .addMarker("analog2").addMarker("output").addMarker("type")
                                            .addMarker("sp").addMarker("his")
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
                                                .addMarker("th1").addMarker("output").addMarker("type")
                                                .addMarker("sp").addMarker("his")
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
                                                .addMarker("th2").addMarker("output").addMarker("type")
                                                .addMarker("sp").addMarker("his")
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
    
    private void setDefaultValues() {
        setCurrentTemp(0);
        setDamperPos(0, "cooling");
        setDamperPos(0, "heating");
        setHumidity(0);
        setCO2(0);
        setVOC(0);
        TemperatureProfileUtil.setDesiredTempCooling(nodeAddr, 74.0);
        TemperatureProfileUtil.setDesiredTemp(nodeAddr,72.0);
        TemperatureProfileUtil.setDesiredTempHeating(nodeAddr, 70.0);
    }
    
    public DualDuctProfileConfiguration getProfileConfiguration() {
        DualDuctProfileConfiguration config = new DualDuctProfileConfiguration();
        
        config.setAnalogOut1Config((int)getConfigNumVal("analog1 and output and type"));
        config.setAnalogOut2Config((int)getConfigNumVal("analog2 and output and type"));
        config.setThermistor1Config((int)getConfigNumVal("th1 and output and type"));
        config.setThermistor2Config((int)getConfigNumVal("th2 and output and type"));
        
        config.setMinHeatingDamperPos((int)TemperatureProfileUtil.getDamperLimit(nodeAddr,"heating","min"));
        config.setMaxHeatingDamperPos((int)TemperatureProfileUtil.getDamperLimit(nodeAddr,"heating","max"));
        config.setMinCoolingDamperPos((int)TemperatureProfileUtil.getDamperLimit(nodeAddr,"cooling","min"));
        config.setMaxCoolingDamperPos((int)TemperatureProfileUtil.getDamperLimit(nodeAddr,"cooling","max"));
        
        config.setAnalog1OutAtMinDamperHeating(getConfigNumVal("analog1 and output and min and damper and " +
                                                                    "heating and pos"));
        config.setAnalog1OutAtMaxDamperHeating(getConfigNumVal("analog1 and output and max and damper and " +
                                                                    "heating and pos"));
        config.setAnalog1OutAtMinDamperCooling(getConfigNumVal("analog1 and output and min and damper and " +
                                                                     "cooling and pos"));
        config.setAnalog1OutAtMaxDamperCooling(getConfigNumVal("analog1 and output and max and damper and " +
                                                                     "cooling and pos"));
        config.setAnalog2OutAtMinDamperHeating(getConfigNumVal("analog2 and output and min and damper and " +
                                                                     "heating and pos"));
        config.setAnalog2OutAtMaxDamperHeating(getConfigNumVal("analog2 and output and max and damper and " +
                                                                     "heating and pos"));
        config.setAnalog2OutAtMinDamperCooling(getConfigNumVal("analog2 and output and min and damper and " +
                                                                     "cooling and pos"));
        config.setAnalog2OutAtMaxDamperCooling(getConfigNumVal("analog2 and output and max and damper and " +
                                                                     "cooling and pos"));
        
        config.setEnableOccupancyControl(getConfigNumVal("enable and occupancy") > 0 ? true : false);
        config.setEnableCO2Control(getConfigNumVal("enable and co2") > 0 ? true : false);
        config.setEnableIAQControl(getConfigNumVal("enable and iaq") > 0 ? true : false) ;
        config.setTemperatureOffset(getConfigNumVal("temperature and offset"));
        
        config.setNodeType(NodeType.SMART_NODE);
        
        return config;
    }
    
    private String createDamperLogicalPoint(String siteRef,
                                           String equipDis,
                                           String roomRef,
                                           String floorRef,
                                           String tz,
                                           DualDuctProfileConfiguration config,
                                           String type) {
    
        Point coolingDamperPos = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+type+"DamperPos")
                                     .setEquipRef(equipRef)
                                     .setSiteRef(siteRef)
                                     .setRoomRef(roomRef)
                                     .setFloorRef(floorRef)
                                     .setHisInterpolate("cov")
                                     .addMarker("damper").addMarker(type).addMarker("pos").addMarker("dualDuct")
                                     .addMarker("cmd").addMarker("his").addMarker("logical").addMarker("zone")
                                     .setGroup(String.valueOf(nodeAddr))
                                     .setUnit("%")
                                     .setTz(tz)
                                     .build();
        return CCUHsApi.getInstance().addPoint(coolingDamperPos);
        
    }
    
    private String createAnalog1LogicalPoint(String siteRef,
                                           String equipDis,
                                           String roomRef,
                                           String floorRef,
                                           String tz,
                                           DualDuctProfileConfiguration config) {
        
        if (config.getAnalogOut1Config() == DualDuctAnalogActuator.COOLING.getVal()) {
            
            return createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config, "cooling");
        } else if (config.getAnalogOut1Config() == DualDuctAnalogActuator.HEATING.getVal()) {
            
            return createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config, "heating");
        } else if (config.getAnalogOut1Config() == DualDuctAnalogActuator.COMPOSITE.getVal()) {
            
            createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config, "cooling");
            createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config, "heating");
            return createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config, "composite");
        }
        return null;
    }
    
    private String createAnalog2LogicalPoint(String siteRef,
                                             String equipDis,
                                             String roomRef,
                                             String floorRef,
                                             String tz,
                                             DualDuctProfileConfiguration config) {
        if (config.getAnalogOut2Config() == DualDuctAnalogActuator.COOLING.getVal()) {
        
            String coolingDamperPosId;
            if (config.getAnalogOut1Config() == DualDuctAnalogActuator.COOLING.getVal() ||
                config.getAnalogOut1Config() == DualDuctAnalogActuator.COMPOSITE.getVal()) {
                
                HashMap coolingDamperPoint = CCUHsApi.getInstance().read("point and dualDuct and cooling and " +
                                                                         "damper and pos and cmd and group == \""+nodeAddr+"\"");
                coolingDamperPosId = coolingDamperPoint.get("id").toString();
            } else {
                coolingDamperPosId = createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config,
                                                               "cooling");
            }
            return coolingDamperPosId;
        
        } else if (config.getAnalogOut2Config() == DualDuctAnalogActuator.HEATING.getVal()) {
        
            String heatingDamperPosId;
            if (config.getAnalogOut1Config() == DualDuctAnalogActuator.COOLING.getVal() ||
                config.getAnalogOut1Config() == DualDuctAnalogActuator.COMPOSITE.getVal()) {
                
                HashMap coolingDamperPoint = CCUHsApi.getInstance().read("point and dualDuct and heating and " +
                                                                         "damper and pos and cmd and group == \""+nodeAddr+"\"");
                heatingDamperPosId = coolingDamperPoint.get("id").toString();
            } else {
                heatingDamperPosId = createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config,
                                                               "heating");
            }
            return heatingDamperPosId;
        } else if (config.getAnalogOut2Config() == DualDuctAnalogActuator.COMPOSITE.getVal()) {
    
            String compositeDamperPosId = null;
            if (config.getAnalogOut1Config() == DualDuctAnalogActuator.COMPOSITE.getVal()) {
        
                HashMap compositeDamperPoint = CCUHsApi.getInstance().read("point and dualDuct and composite and " +
                                                                         "damper and pos and cmd and group == \""+nodeAddr+"\"");
                compositeDamperPosId = compositeDamperPoint.get("id").toString();
            } else if (config.getAnalogOut1Config() == DualDuctAnalogActuator.COOLING.getVal()){
                createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config, "heating");
                compositeDamperPosId = createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config,
                                                               "composite");
            } else if (config.getAnalogOut1Config() == DualDuctAnalogActuator.HEATING.getVal()){
                createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config, "cooling");
                compositeDamperPosId = createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config,
                                                                 "composite");
            } else if (config.getAnalogOut1Config() == DualDuctAnalogActuator.NOT_USED.getVal()){
                createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config, "cooling");
                createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config, "heating");
                compositeDamperPosId = createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config,
                                                                 "composite");
            }
            return compositeDamperPosId;
        }
        return null;
    }
    
    private void updateAnalog1LogicalPoint(String siteRef,
                                           String equipDis,
                                           String roomRef,
                                           String floorRef,
                                           String tz,
                                           DualDuctProfileConfiguration config) {
        
        if (config.getAnalogOut1Config() == DualDuctAnalogActuator.NOT_USED.getVal()) {
            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_OUT_ONE.toString(), false);
        } else if (config.getAnalogOut1Config() == DualDuctAnalogActuator.COOLING.getVal()) {
            
            String coolingDamperPosId = createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config, "cooling");
            SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_OUT_ONE.toString(),
                                              config.getAnalog1OutAtMinDamperCooling()+"-"+config.getAnalog1OutAtMaxDamperCooling()+"v");
            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_OUT_ONE.toString(), true);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_OUT_ONE.toString(), coolingDamperPosId);
            
        } else if (config.getAnalogOut1Config() == DualDuctAnalogActuator.HEATING.getVal()) {
            
            String heatingDamperPosId = createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config, "heating");
            SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_OUT_ONE.toString(),
                                              config.getAnalog1OutAtMinDamperHeating()+"-"+config.getAnalog1OutAtMaxDamperHeating()+"v");
            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_OUT_ONE.toString(), true);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_OUT_ONE.toString(), heatingDamperPosId);
        } else if (config.getAnalogOut1Config() == DualDuctAnalogActuator.COMPOSITE.getVal()) {
            String compositeDamperPosId = createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config, "composite");;
            SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_OUT_ONE.toString(),
                                              OutputAnalogActuatorType.ZeroToTenV.displayName);
            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_OUT_ONE.toString(), true);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_OUT_ONE.toString(), compositeDamperPosId);
        }
    
    }
    
    private void updateAnalog1Config(DualDuctProfileConfiguration config) {
 
        HashMap equipMap = hayStack.read("equip and group == \"" + nodeAddr + "\"");
        String siteRef = equipMap.get("siteRef").toString();
        String equipDis = equipMap.get("dis").toString();
        String roomRef = equipMap.get("roomRef").toString();
        String floorRef = equipMap.get("floorRef").toString();
        String tz = equipMap.get("tz").toString();
    
        updateAnalog1LogicalPoint(siteRef, equipDis, roomRef, floorRef, tz, config);
        
    }
    
    private void updateAnalog2LogicalPoint(String siteRef,
                                           String equipDis,
                                           String roomRef,
                                           String floorRef,
                                           String tz,
                                           DualDuctProfileConfiguration config) {
    
        if (config.getAnalogOut2Config() == DualDuctAnalogActuator.NOT_USED.getVal()) {
            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_OUT_TWO.toString(), false);
        } else if (config.getAnalogOut2Config() == DualDuctAnalogActuator.COOLING.getVal()) {
        
            String coolingDamperPosId;
            if (config.getAnalogOut2Config() == DualDuctAnalogActuator.COOLING.getVal()) {
                HashMap coolingDamperPoint = CCUHsApi.getInstance().read("point and dualDuct and cooling and " +
                                                                         "damper and pos and cmd and group == \""+nodeAddr+"\"");
                coolingDamperPosId = coolingDamperPoint.get("id").toString();
            } else {
                coolingDamperPosId = createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config,
                                                                "cooling");
            }
            
            
            SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_OUT_TWO.toString(),
                                              config.getAnalog2OutAtMinDamperCooling()+"-"+config.getAnalog2OutAtMaxDamperCooling()+"v");
            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_OUT_TWO.toString(), true);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_OUT_TWO.toString(), coolingDamperPosId);
        
        } else if (config.getAnalogOut2Config() == DualDuctAnalogActuator.HEATING.getVal()) {
    
            String heatingDamperPosId;
            if (config.getAnalogOut2Config() == DualDuctAnalogActuator.COOLING.getVal()) {
                HashMap coolingDamperPoint = CCUHsApi.getInstance().read("point and dualDuct and heating and " +
                                                                         "damper and pos and cmd and group == \""+nodeAddr+"\"");
                heatingDamperPosId = coolingDamperPoint.get("id").toString();
            } else {
                heatingDamperPosId = createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config,
                                                               "heating");
            }
            SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_OUT_TWO.toString(),
                                              config.getAnalog2OutAtMinDamperHeating()+"-"+config.getAnalog2OutAtMaxDamperHeating()+"v");
            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_OUT_TWO.toString(), true);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_OUT_TWO.toString(), heatingDamperPosId);
        } else if (config.getAnalogOut2Config() == DualDuctAnalogActuator.COMPOSITE.getVal()) {
        
            String compositeDamperPosId = createDamperLogicalPoint (siteRef, equipDis, roomRef, floorRef, tz, config, "composite");;
            SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_OUT_TWO.toString(),
                                              OutputAnalogActuatorType.ZeroToTenV.displayName);
            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_OUT_TWO.toString(), true);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_OUT_TWO.toString(), compositeDamperPosId);
        }
        
    }
    
    private void updateAnalog2Config(DualDuctProfileConfiguration config) {
 
        HashMap equipMap = hayStack.read("equip and group == \"" + nodeAddr + "\"");
        String siteRef = equipMap.get("siteRef").toString();
        String equipDis = equipMap.get("dis").toString();
        String roomRef = equipMap.get("roomRef").toString();
        String floorRef = equipMap.get("floorRef").toString();
        String tz = equipMap.get("tz").toString();
        
        updateAnalog2LogicalPoint(siteRef, equipDis, roomRef, floorRef, tz, config);
        
    }
    
    private String createSupplyAirTempPoint(String siteRef,
                                    String equipDis,
                                    String roomRef,
                                    String floorRef,
                                    String tz,
                                    DualDuctProfileConfiguration config) {
        if (config.getThermistor2Config() == DualDuctThermistorConfig.COOLING_AIRFLOW_TEMP.getVal()) {
            Point coolingAirflowTemp = new Point.Builder()
                                           .setDisplayName(equipDis+"-coolingSupplyAirTemp")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("zone").addMarker("dualDuct").setHisInterpolate("cov")
                                           .addMarker("air").addMarker("temp").addMarker("sensor")
                                           .addMarker("cooling").addMarker("supply")
                                           .addMarker("his").addMarker("logical")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setUnit("\u00B0F")
                                           .setTz(tz)
                                           .build();
            return CCUHsApi.getInstance().addPoint(coolingAirflowTemp);
        } else if (config.getThermistor2Config() == DualDuctThermistorConfig.HEATING_AIRFLOW_TEMP.getVal()){
            Point heatingAirflowTemp = new Point.Builder()
                                           .setDisplayName(equipDis+"-heatingSupplyAirTemp")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("zone").addMarker("dualDuct").setHisInterpolate("cov")
                                           .addMarker("air").addMarker("temp").addMarker("sensor")
                                           .addMarker("heating").addMarker("supply")
                                           .addMarker("his").addMarker("logical")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setUnit("\u00B0F")
                                           .setTz(tz)
                                           .build();
            return CCUHsApi.getInstance().addPoint(heatingAirflowTemp);
        }
        return null;
    }
    
    private void updateThermistorConfig(DualDuctProfileConfiguration config) {
        int currentThermistor2Config = (int)getConfigNumVal("th2 and output and type");
        if (currentThermistor2Config == config.getThermistor2Config()) {
            return;
        }
        RawPoint th2PhysicalPoint = SmartNode.getPhysicalPoint(nodeAddr, Port.TH2_IN.toString());
        CCUHsApi.getInstance().deleteWritablePoint(th2PhysicalPoint.getPointRef());
        
        HashMap equipMap = hayStack.read("equip and group == \"" + nodeAddr + "\"");
        String siteRef = equipMap.get("siteRef").toString();
        String equipDis = equipMap.get("dis").toString();
        String roomRef = equipMap.get("roomRef").toString();
        String floorRef = equipMap.get("floorRef").toString();
        String tz = equipMap.get("tz").toString();
    
        String airflowTempId = createSupplyAirTempPoint(siteRef, equipDis, roomRef, floorRef, tz, config);
        SmartNode.updatePhysicalPointRef(nodeAddr, Port.TH2_IN.toString(), airflowTempId);
    }
    
    private void deleteLogicalPoints() {
        HashMap coolingDamperPoint = CCUHsApi.getInstance().read("point and dualDuct and cooling and " +
                                                                 "damper and pos and cmd and group == \""+nodeAddr+"\"");
        if (!coolingDamperPoint.isEmpty()) {
            CCUHsApi.getInstance().deleteWritablePoint(coolingDamperPoint.get("id").toString());
        }
    
        HashMap heatingDamperPoint = CCUHsApi.getInstance().read("point and dualDuct and heating and " +
                                                                 "damper and pos and cmd and group == \""+nodeAddr+"\"");
        if (!heatingDamperPoint.isEmpty()) {
            CCUHsApi.getInstance().deleteWritablePoint(coolingDamperPoint.get("id").toString());
        }
    
        HashMap compositeDamperPoint = CCUHsApi.getInstance().read("point and dualDuct and composite and " +
                                                                 "damper and pos and cmd and group == \""+nodeAddr+"\"");
        if (!compositeDamperPoint.isEmpty()) {
            CCUHsApi.getInstance().deleteWritablePoint(coolingDamperPoint.get("id").toString());
        }
    }
    
    public void updateEquip(DualDuctProfileConfiguration config) {
    
        DualDuctProfileConfiguration currentConfig = getProfileConfiguration();
        if (currentConfig.equals(config)) {
            CcuLog.i(L.TAG_CCU_ZONE, "Skip updateEquip : Profile Configuration has not changed");
            return;
        }
        
        deleteLogicalPoints();
        updateAnalog1Config(config);
        updateAnalog2Config(config);
        updateThermistorConfig(config);
        
        setConfigNumVal("analog1 and output and type",config.getAnalogOut1Config());
        setConfigNumVal("analog2 and output and type",config.getAnalogOut2Config());
        setConfigNumVal("th1 and output and type",config.getThermistor1Config());
        setConfigNumVal("th2 and output and type",config.getThermistor2Config());
        
        setConfigNumVal("analog1 and output and min and damper and heating and pos",config.getAnalog1OutAtMinDamperHeating());
        setConfigNumVal("analog1 and output and max and damper and heating and pos", config.getAnalog1OutAtMaxDamperHeating());
        setConfigNumVal("analog1 and output and min and damper and cooling and pos", config.getAnalog1OutAtMinDamperCooling());
        setConfigNumVal("analog1 and output and max and damper and cooling and pos", config.getAnalog1OutAtMaxDamperCooling());
    
        setConfigNumVal("analog2 and output and min and damper and heating and pos", config.getAnalog2OutAtMinDamperHeating());
        setConfigNumVal("analog2 and output and max and damper and heating and pos", config.getAnalog2OutAtMaxDamperHeating());
        setConfigNumVal("analog2 and output and min and damper and cooling and pos", config.getAnalog2OutAtMinDamperCooling());
        setConfigNumVal("analog2 and output and max and damper and cooling and pos", config.getAnalog2OutAtMaxDamperCooling());
        
        setConfigNumVal("enable and occupancy",config.isEnableOccupancyControl() ? 1.0 : 0);
        setHisVal("enable and occupancy",config.isEnableOccupancyControl() ? 1.0 : 0);
        setConfigNumVal("enable and co2",config.isEnableCO2Control() ? 1.0 : 0);
        setHisVal("enable and co2",config.isEnableCO2Control() ? 1.0 : 0);
        setConfigNumVal("enable and co2",config.isEnableIAQControl() ? 1.0 : 0);
        setHisVal("enable and co2",config.isEnableIAQControl() ? 1.0 : 0);
        
        setConfigNumVal("temperature and offset",config.getTemperatureOffset());
        TemperatureProfileUtil.setDamperLimit(nodeAddr,"cooling","min",config.getMinCoolingDamperPos());
        setHisVal("cooling and min and damper and pos",config.getMinCoolingDamperPos());
        TemperatureProfileUtil.setDamperLimit(nodeAddr,"cooling","max",config.getMaxCoolingDamperPos());
        setHisVal("cooling and max and damper and pos",config.getMaxCoolingDamperPos());
        TemperatureProfileUtil.setDamperLimit(nodeAddr,"heating","min",config.getMinHeatingDamperPos());
        setHisVal("heating and min and damper and pos",config.getMinHeatingDamperPos());
        TemperatureProfileUtil.setDamperLimit(nodeAddr, "heating","max",config.getMaxHeatingDamperPos());
        setHisVal("heating and max and damper and pos",config.getMaxHeatingDamperPos());
    }
    
    
    public void setConfigNumVal(String tags,double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and zone and config and dualDuct and "+tags+" and group == \""+nodeAddr+"\"", val);
    }
    
    public double getConfigNumVal(String tags) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and dualDuct and "+tags+" and group == \""+nodeAddr+"\"");
    }
    
    public void setHisVal(String tags,double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and zone and config and dualDuct and "+tags+" and group == \""+nodeAddr+"\"", val);
    }
    
    public CO2Loop getCo2Loop()
    {
        return co2Loop;
    }
    public VOCLoop getVOCLoop()
    {
        return vocLoop;
    }
    
    public double getCurrentTemp()
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and group == \"" + nodeAddr + "\"");
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
    
    public void setDamperPos(double damperPos, String damper)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and damper and cmd and "+damper+" and group == \""+nodeAddr+"\"", damperPos);
    }
    public double getDamperPos(String damper)
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and damper and cmd and "+damper+" and group == \""+nodeAddr+"\"");
    }
}
