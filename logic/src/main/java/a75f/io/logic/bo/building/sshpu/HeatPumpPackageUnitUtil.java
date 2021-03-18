package a75f.io.logic.bo.building.sshpu;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.SmartStatFanRelayType;
import a75f.io.logic.bo.building.definitions.SmartStatHeatPumpChangeOverType;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.hvac.StandaloneFanStage;
import a75f.io.logic.bo.haystack.device.SmartStat;
import a75f.io.logic.tuners.TunerConstants;

/**
 * Util class to handle remote reconfiguration changes for HPU profile.
 * The the util class is lot similar in implementation with CPU. But relay mapping and tags are different.
 * So separate util class is used.
 */
public class HeatPumpPackageUnitUtil {
    
    public static void updateHPUProfile(Point configPoint, JsonObject msgObject,
                                        CCUHsApi hayStack) {
    
        CcuLog.e(L.TAG_CCU_PUBNUB, " updateHPUProfile : " + Arrays.toString(configPoint.getMarkers().toArray()));
        try {
            String val = msgObject.get(HayStackConstants.WRITABLE_ARRAY_VAL).getAsString();
            if (val.isEmpty()) {
                int level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt();
                //When a level is deleted, it currently generates a pubnub with empty value.
                //Handle it here.
                hayStack.clearPointArrayLevel(configPoint.getId(), level, true);
                hayStack.writeHisValById(configPoint.getId(), HSUtil.getPriorityVal(configPoint.getId()));
                return;
            }
            
            double configVal = msgObject.get("val").getAsDouble();
            if (configPoint.getMarkers().contains(Tags.CONFIG)) {
                updateConfig(configVal, configPoint, msgObject, hayStack);
            } else if (configPoint.getMarkers().contains(Tags.ENABLE)
                       && configPoint.getMarkers().contains(Tags.OCCUPANCY)
                       && configPoint.getMarkers().contains(Tags.CONTROL)) {
                updateOccupancyPoint(configVal, configPoint, msgObject, hayStack);
            } else {
                writePointFromJson(configPoint.getId(), configVal, msgObject, hayStack);
                if (configPoint.getMarkers().contains(Tags.HIS)) {
                    hayStack.writeHisValById(configPoint.getId(), configVal);
                }
            }
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to update : " + configPoint.getDisplayName() + " ; " + msgObject + " " +
                                       e.getMessage());
        }
    }
    
    private static void updateOccupancyPoint(double configVal, Point configPoint,
                                             JsonObject msgObject, CCUHsApi hayStack) {
        
        HashMap equipMap = hayStack.readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        
        updateOccupancyPoint(configVal, equip, hayStack);
        writePointFromJson( configPoint.getId(), configVal, msgObject, hayStack);
    }
    
    public static void updateOccupancyPoint(double configVal, Equip equip, CCUHsApi hayStack) {
        
        HashMap occDetPoint = hayStack.read("point and occupancy and detection and hpu and his and " +
                                            "equipRef== \"" + equip.getId() + "\"");
        
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateOccupancyPoint "+configVal+" "+occDetPoint);
        
        if(configVal > 0){
            //Create occupancy detection point if it does not exist.
            if (occDetPoint.isEmpty()) {
                Point occupancyDetection = new Point.Builder()
                                               .setDisplayName(equip.getDisplayName()+"-occupancyDetection")
                                               .setEquipRef(equip.getId())
                                               .setSiteRef(equip.getSiteRef())
                                               .setRoomRef(equip.getRoomRef())
                                               .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                               .addMarker("occupancy").addMarker("detection").addMarker("hpu").addMarker("his").addMarker("zone")
                                               .setGroup(String.valueOf(equip.getGroup()))
                                               .setEnums("false,true")
                                               .setTz(hayStack.getTimeZone())
                                               .build();
                String occupancyDetectionId = CCUHsApi.getInstance().addPoint(occupancyDetection);
                CCUHsApi.getInstance().writeHisValById(occupancyDetectionId, 0.0);
            }
            
        } else {
            if (!occDetPoint.isEmpty())
                hayStack.deleteEntityTree(occDetPoint.get("id").toString());
        }
    }
    
    private static void updateConfig(double configVal, Point configPoint, JsonObject msgObject, CCUHsApi hayStack) {
        
        HashMap equipMap = hayStack.readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String nodeAddr = equip.getGroup();
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfig " + nodeAddr + " " + configPoint);
        if (configPoint.getMarkers().contains(Tags.RELAY1)) {
            SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.RELAY_ONE.name(),
                                      configVal > 0 ? true : false);
        } else if (configPoint.getMarkers().contains(Tags.RELAY2)) {
            SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.RELAY_TWO.name(),
                                      configVal > 0 ? true : false);
        } else if (configPoint.getMarkers().contains(Tags.RELAY3)) {
            SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.RELAY_THREE.name(),
                                      configVal > 0 ? true : false);
        } else if (configPoint.getMarkers().contains(Tags.RELAY4)) {
            SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.RELAY_FOUR.name(),
                                      configVal > 0 ? true : false);
        } else if (configPoint.getMarkers().contains(Tags.RELAY5)) {
            SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.RELAY_FIVE.name(),
                                      configVal > 0 ? true : false);
            updateRelay5Config(configVal, configPoint, hayStack);
        } else if (configPoint.getMarkers().contains(Tags.RELAY6)) {
            SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.RELAY_SIX.name(),
                                      configVal > 0 ? true : false);
            updateRelay6Config(configVal, configPoint, hayStack);
        } else if (configPoint.getMarkers().contains(Tags.TH1)) {
            SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.TH1_IN.name(),
                                      configVal > 0 ? true : false);
        } else if (configPoint.getMarkers().contains(Tags.TH2)) {
            SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.TH2_IN.name(),
                                      configVal > 0 ? true : false);
        }
        writePointFromJson(configPoint.getId(), configVal, msgObject, hayStack);
        hayStack.syncPointEntityTree();
        adjustHPUFanMode(equip,hayStack);
        adjustConditioningMode(equip, hayStack);
    }
    
    
    private static void updateRelay5Config(double configVal, Point configPoint, CCUHsApi hayStack) {
        HashMap equipMap = hayStack.readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateRelay5Config " + configVal);
        if (configPoint.getMarkers().contains(Tags.TYPE)) {
            double relay5Enabled = getConfigNumVal("enable and relay5", equip.getGroup());
            double relay5Type = getConfigNumVal("relay5 and type",equip.getGroup());
            //We would accept relay5Type changes only when relay5 is enabled.
            if (relay5Enabled > 0 && relay5Type != configVal) {
                manageRelay5AssociatedPoints(configVal, equip);
            }
        }
        hayStack.syncPointEntityTree();
    }
    
    public static void manageRelay5AssociatedPoints(double configVal, Equip equip ) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "manageRelay5AssociatedPoints " + configVal);
        SmartStatFanRelayType relay6Type = SmartStatFanRelayType.values()[(int)configVal];
        switch (relay6Type) {
            case NOT_USED:
                deleteHumidifierPoint(equip.getId());
                deleteDehumidifierPoint(equip.getId());
                deleteFanStage2Point(equip.getId());
                break;
            case FAN_STAGE2:
                deleteHumidifierPoint(equip.getId());
                deleteDehumidifierPoint(equip.getId());
                createFanStage2Point(equip);
            case HUMIDIFIER:
                deleteDehumidifierPoint(equip.getId());
                deleteFanStage2Point(equip.getId());
                createHumidifierPoint(equip);
                break;
            case DE_HUMIDIFIER:
                deleteHumidifierPoint(equip.getId());
                deleteFanStage2Point(equip.getId());
                createDeHumidifierPoint(equip);
                break;
        }
    }
    
    private static void updateRelay6Config(double configVal, Point configPoint, CCUHsApi hayStack) {
        HashMap equipMap = hayStack.readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateRelay6Config " + configVal);
        if (configPoint.getMarkers().contains(Tags.TYPE)) {
            double relay6Enabled = getConfigNumVal("enable and relay6", equip.getGroup());
            double relay6Type = getConfigNumVal("relay6 and type",equip.getGroup());
            //We would accept relay5Type changes only when relay5 is enabled.
            if (relay6Enabled > 0 && relay6Type != configVal) {
                manageRelay6AssociatedPoints(configVal, equip);
            }
        }
        hayStack.syncPointEntityTree();
    }
    
    public static void manageRelay6AssociatedPoints(double configVal, Equip equip ) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "manageRelay6AssociatedPoints " + configVal);
        SmartStatHeatPumpChangeOverType relay6Type = SmartStatHeatPumpChangeOverType.values()[(int) configVal];
        switch (relay6Type) {
            case NOT_USED:
                deleteCoolingChangeOverPoint(equip.getId());
                deleteHeatingChangeOverPoint(equip.getId());
                break;
            case ENERGIZE_IN_COOLING:
                deleteHeatingChangeOverPoint(equip.getId());
                createCoolingChangeOverPoint(equip);
                break;
            case ENERGIZE_IN_HEATING:
                deleteCoolingChangeOverPoint(equip.getId());
                createHeatingChangeOverPoint(equip);
                break;
                
        }
    }
    
    private static void deleteHumidifierPoint(String equipRef) {
        HashMap humidifierPt = CCUHsApi.getInstance().read("point and standalone and humidifier and hpu and cmd and " +
                                                           "his and equipRef== \"" + equipRef + "\"");
        if (!humidifierPt.isEmpty())
            CCUHsApi.getInstance().deleteEntityTree(humidifierPt.get("id").toString());
    }
    
    private static void deleteDehumidifierPoint(String equipRef) {
        HashMap dehumidifierPt = CCUHsApi.getInstance().read("point and standalone and dehumidifier and hpu and cmd " +
                                                             "and his and equipRef== \"" + equipRef + "\"");
        if (!dehumidifierPt.isEmpty())
            CCUHsApi.getInstance().deleteEntityTree(dehumidifierPt.get("id").toString());
    }
    
    private static void deleteFanStage2Point(String equipRef) {
        HashMap fnStg2Pt = CCUHsApi.getInstance().read("point and standalone and fan and stage2 and hpu and cmd and " +
                                                       "his and equipRef== \"" + equipRef + "\"");
        if (!fnStg2Pt.isEmpty())
            CCUHsApi.getInstance().deleteEntityTree(fnStg2Pt.get("id").toString());
    }
    
    private static void createHumidifierPoint(Equip equip) {
        Point humidifier = new Point.Builder().setDisplayName(equip.getDisplayName() + "-humidifier")
                                              .setEquipRef(equip.getId())
                                              .setSiteRef(equip.getSiteRef())
                                              .setRoomRef(equip.getRoomRef())
                                              .setFloorRef(equip.getFloorRef())
                                              .setHisInterpolate("cov")
                                              .addMarker("standalone").addMarker("humidifier").addMarker("his")
                                              .addMarker("zone").addMarker("logical").addMarker("hpu").addMarker("cmd")
                                              .setEnums("off,on")
                                              .setGroup(equip.getGroup())
                                              .setTz(CCUHsApi.getInstance().getTimeZone())
                                              .build();
        String r6HumidID = CCUHsApi.getInstance().addPoint(humidifier);
        CCUHsApi.getInstance().writeHisValById(r6HumidID, 0.0);
        SmartStat.updatePhysicalPointRef(Integer.valueOf(equip.getGroup()), Port.RELAY_SIX.name(), r6HumidID);
    }
    
    private static void createDeHumidifierPoint(Equip equip) {
        Point dehumidifier = new Point.Builder().setDisplayName(equip.getDisplayName() + "-deHumidifier")
                                                .setEquipRef(equip.getId())
                                                .setSiteRef(equip.getSiteRef())
                                                .setRoomRef(equip.getRoomRef())
                                                .setFloorRef(equip.getFloorRef())
                                                .setHisInterpolate("cov")
                                                .addMarker("standalone").addMarker("dehumidifier").addMarker("his")
                                                .addMarker("zone").addMarker("logical").addMarker("hpu").addMarker("cmd")
                                                .setEnums("off,on")
                                                .setGroup(equip.getGroup())
                                                .setTz(CCUHsApi.getInstance().getTimeZone())
                                                .build();
        String r6DeHumidID = CCUHsApi.getInstance().addPoint(dehumidifier);
        CCUHsApi.getInstance().writeHisValById(r6DeHumidID, 0.0);
        SmartStat.updatePhysicalPointRef(Integer.valueOf(equip.getGroup()), Port.RELAY_SIX.name(), r6DeHumidID);
    }
    
    private static void createFanStage2Point(Equip equip) {
        Point fanStage2 = new Point.Builder().setDisplayName(equip.getDisplayName() + "-fanStage2")
                                             .setEquipRef(equip.getId())
                                             .setSiteRef(equip.getSiteRef())
                                             .setRoomRef(equip.getRoomRef())
                                             .setFloorRef(equip.getFloorRef())
                                             .setHisInterpolate("cov")
                                             .addMarker("standalone").addMarker("fan").addMarker("stage2")
                                             .addMarker("his").addMarker("zone").addMarker("logical").addMarker("hpu")
                                             .addMarker("cmd").addMarker("runtime")
                                             .setGroup(equip.getGroup())
                                             .setTz(equip.getTz())
                                             .build();
        String r6ID = CCUHsApi.getInstance().addPoint(fanStage2);
        CCUHsApi.getInstance().writeHisValById(r6ID, 0.0);
        SmartStat.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()), Port.RELAY_SIX.name(), r6ID);
    }
    
    private static void deleteCoolingChangeOverPoint(String equipRef) {
        HashMap coolingCoPoint = CCUHsApi.getInstance().read("point and standalone and cooling and changeover and " +
                                                             "stage1 and hpu and  cmd and his and equipRef== \"" + equipRef + "\"");
        if (!coolingCoPoint.isEmpty())
            CCUHsApi.getInstance().deleteEntityTree(coolingCoPoint.get("id").toString());
    
    }
    
    private static void createHeatingChangeOverPoint(Equip equip) {
        
        Point heatpumpChangeoverHeating = new Point.Builder()
                                              .setDisplayName(equip.getDisplayName() + "-heatpumpChangeoverHeating")
                                              .setEquipRef(equip.getId())
                                              .setSiteRef(equip.getSiteRef())
                                              .setRoomRef(equip.getRoomRef())
                                              .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                              .addMarker("standalone").addMarker("heatpump").addMarker("heating").addMarker("changeover").addMarker("stage1").addMarker("his").addMarker("zone")
                                              .addMarker("logical").addMarker("hpu").addMarker("cmd")
                                              .setEnums("off,on")
                                              .setGroup(equip.getGroup())
                                              .setTz(CCUHsApi.getInstance().getTimeZone())
                                              .build();
        String r6HeatID = CCUHsApi.getInstance().addPoint(heatpumpChangeoverHeating);
        CCUHsApi.getInstance().writeHisValById(r6HeatID, 0.0);
        SmartStat.updatePhysicalPointRef(Integer.valueOf(equip.getGroup()), Port.RELAY_SIX.name(), r6HeatID);
    }
    
    private static void deleteHeatingChangeOverPoint(String equipRef) {
        HashMap heatingCoPoint = CCUHsApi.getInstance().read("point and standalone and heating and changeover and " +
                                                             "stage1 and hpu and  cmd and his and equipRef== \"" + equipRef + "\"");
        if (!heatingCoPoint.isEmpty())
            CCUHsApi.getInstance().deleteEntityTree(heatingCoPoint.get("id").toString());
    
    }
    
    private static void createCoolingChangeOverPoint(Equip equip) {
        
        Point heatpumpChangeoverCooling = new Point.Builder()
                                              .setDisplayName(equip.getDisplayName() + "-heatpumpChangeoverCooling")
                                              .setEquipRef(equip.getId())
                                              .setSiteRef(equip.getSiteRef())
                                              .setRoomRef(equip.getRoomRef())
                                              .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                              .addMarker("standalone").addMarker("heatpump").addMarker("cooling").addMarker("changeover").addMarker("stage1").addMarker("his").addMarker("zone")
                                              .addMarker("logical").addMarker("hpu").addMarker("cmd")
                                              .setEnums("off,on")
                                              .setGroup(equip.getGroup())
                                              .setTz(CCUHsApi.getInstance().getTimeZone())
                                              .build();
        String r6ID = CCUHsApi.getInstance().addPoint(heatpumpChangeoverCooling);
        CCUHsApi.getInstance().writeHisValById(r6ID, 0.0);
        SmartStat.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()), Port.RELAY_SIX.name(), r6ID);
    }
    
    private static void adjustHPUFanMode(Equip equip, CCUHsApi hayStack) {
        
        String fanSpeedPointId = hayStack.readId("point and zone and userIntent and fan and " +
                                                     "mode and equipRef == \"" + equip.getId() + "\"");
        
        double curFanSpeed = hayStack.readPointPriorityVal(fanSpeedPointId);
        /**
         * When currently available fanSpeed configuration is not OFF , set fanSpeed to AUTO
         * When none of fan configuration is enabled, Set fanSpeed to OFF.
         */
    
        StandaloneFanStage maxFanSpeed = getMaxAvailableFanSpeed(equip);
        double fallbackFanSpeed = curFanSpeed;
        if (curFanSpeed > maxFanSpeed.ordinal() && maxFanSpeed.ordinal() > StandaloneFanStage.OFF.ordinal()) {
            fallbackFanSpeed = StandaloneFanStage.AUTO.ordinal();
        } else if (curFanSpeed > maxFanSpeed.ordinal()) {
            fallbackFanSpeed = StandaloneFanStage.OFF.ordinal();
        }
        
        CcuLog.i(L.TAG_CCU_PUBNUB, "adjustHPUFanMode "+curFanSpeed+" -> "+fallbackFanSpeed);
        if (curFanSpeed != fallbackFanSpeed) {
            hayStack.writeDefaultValById(fanSpeedPointId, fallbackFanSpeed);
            hayStack.writeHisValById(fanSpeedPointId, fallbackFanSpeed);
        }
    }
    
    
    private static StandaloneFanStage getMaxAvailableFanSpeed(Equip equip) {
    
        double fanLowEnabled = getConfigNumVal("enable and relay3", equip.getGroup());
        double fanHighEnabled = getConfigNumVal("enable and relay5", equip.getGroup());
        
        StandaloneFanStage maxFanSpeed = StandaloneFanStage.OFF;
        if (fanHighEnabled > 0) {
            double relay5Type = getConfigNumVal("relay5 and type",equip.getGroup());
            if (relay5Type == SmartStatFanRelayType.FAN_STAGE2.ordinal()) {
                maxFanSpeed = StandaloneFanStage.HIGH_ALL_TIME;
            } else {
                maxFanSpeed = StandaloneFanStage.LOW_ALL_TIME;
            }
        } else if (fanLowEnabled > 0) {
            maxFanSpeed = StandaloneFanStage.LOW_ALL_TIME;
        }
        return maxFanSpeed;
    }
    
    private static void adjustConditioningMode(Equip equip, CCUHsApi hayStack) {
        
        String conditioningModeId = CCUHsApi.getInstance().readId("point and zone and userIntent and conditioning and" +
                                                                  " mode and equipRef == \"" + equip.getId() + "\"");
        if (conditioningModeId.isEmpty()) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "ConditioningMode point does not exist for update : "+equip.getDisplayName());
            return;
        }
        double curCondMode = CCUHsApi.getInstance().readPointPriorityVal(conditioningModeId);
        
        double compressorStage1 = getConfigNumVal("enable and relay1", equip.getGroup());
        double compressorStage2 = getConfigNumVal("enable and relay2", equip.getGroup());
        
        double conditioningMode = curCondMode;
        
        if (Math.abs(compressorStage1) < 0.01 && Math.abs(compressorStage2) < 0.01){
            conditioningMode = StandaloneConditioningMode.OFF.ordinal();
        }
        
        CcuLog.i(L.TAG_CCU_PUBNUB, "adjustHPUConditioningMode "+curCondMode+" -> "+conditioningMode);
        if (curCondMode != conditioningMode) {
            hayStack.writeDefaultValById(conditioningModeId, conditioningMode);
            hayStack.writeHisValById(conditioningModeId, conditioningMode);
        }
    }
    
    private static void writePointFromJson(String id, double val, JsonObject msgObject,
                                           CCUHsApi hayStack) {
        try {
            int level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt();
            int duration = msgObject.get(HayStackConstants.WRITABLE_ARRAY_DURATION) != null ? msgObject.get(
                HayStackConstants.WRITABLE_ARRAY_DURATION).getAsInt() : 0;
            hayStack.writePointLocal(id, level, hayStack.getCCUUserName(), val, duration);
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : "+msgObject+" ; "+e.getMessage());
        }
    }
    
    private static double getConfigNumVal(String tags, String nodeAddr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and standalone and hpu and "+tags+" " +
                                                     "and group == \""+nodeAddr+"\"");
    }
}
