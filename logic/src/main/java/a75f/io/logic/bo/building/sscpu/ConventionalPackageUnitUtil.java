package a75f.io.logic.bo.building.sscpu;

import com.google.gson.JsonObject;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.SmartStatFanRelayType;
import a75f.io.logic.bo.building.hvac.SSEConditioningMode;
import a75f.io.logic.bo.building.hvac.SSEFanStage;
import a75f.io.logic.bo.haystack.device.SmartStat;

public class ConventionalPackageUnitUtil {
    
    public static void updateCPUProfile(Point configPoint, JsonObject msgObject,
                                        CCUHsApi hayStack) {
        try {
            double configVal = msgObject.get("val").getAsDouble();
            if (configPoint.getMarkers().contains(Tags.CONFIG)) {
                updateConfig(configVal, configPoint, msgObject, hayStack);
            } else if (configPoint.getMarkers().contains(Tags.ENABLE) && configPoint.getMarkers().contains(
                Tags.OCCUPANCY) && configPoint.getMarkers().contains(Tags.CONTROL)) {
                updateOccupancyPoint(configVal, configPoint, msgObject, hayStack);
            } else if (configPoint.getMarkers().contains(Tags.USERINTENT)) {
                //updateUserIntent(configVal, configPoint, msgObject, hayStack);
            } else {
                writePointFromJson(configPoint.getId(), configVal, msgObject, hayStack);
            }
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to update : " + configPoint.getDisplayName() + " ; " + msgObject + " " +
                                       e.getMessage());
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
        adjustCPUFanMode(equip,hayStack);
        adjustConditioningMode(equip, hayStack);
    }
    
    private static void updateOccupancyPoint(double configVal, Point configPoint,
                                             JsonObject msgObject, CCUHsApi hayStack) {
        
        HashMap equipMap = hayStack.readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        
        HashMap occDetPoint = hayStack.read("point and occupancy and detection and cpu and his and " +
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
                                               .addMarker("occupancy").addMarker("detection").addMarker("cpu").addMarker("his").addMarker("zone")
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
        writePointFromJson(configPoint.getId(), configVal, msgObject, hayStack);
    }
    
    private static void updateRelay6Config(double configVal, Point configPoint, CCUHsApi hayStack) {
        HashMap equipMap = hayStack.readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        
        if (configPoint.getMarkers().contains(Tags.TYPE)) {
            manageRelay6AssociatedPoints(configVal, equip);
        } else if (configPoint.getMarkers().contains(Tags.ENABLE)) {
            if (configVal > 0) {
                double relay6Type = getConfigNumVal("relay6 and type",equip.getGroup());
                manageRelay6AssociatedPoints(relay6Type, equip);
            } else {
                manageRelay6AssociatedPoints(configVal, equip);
            }
        }
        
    }
    
    private static void manageRelay6AssociatedPoints(double configVal, Equip equip ) {
        
        SmartStatFanRelayType relay6Type = SmartStatFanRelayType.values()[(int)configVal];
        switch (relay6Type) {
            case NOT_USED:
                deleteHumidifierPoint(equip.getId());
                deleteDehumidifierPoint(equip.getId());
                deleteFanStage2Point(equip.getId());
                break;
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
            case FAN_STAGE2:
                deleteHumidifierPoint(equip.getId());
                deleteDehumidifierPoint(equip.getId());
                createFanStage2Point(equip);
        
        }
    }
    
    private static void deleteHumidifierPoint(String equipRef) {
        HashMap humidifierPt = CCUHsApi.getInstance().read("point and standalone and humidifier and cpu and cmd and " +
                                                           "his and equipRef== \"" + equipRef + "\"");
        if (!humidifierPt.isEmpty())
            CCUHsApi.getInstance().deleteEntityTree(humidifierPt.get("id").toString());
    }
    
    private static void deleteDehumidifierPoint(String equipRef) {
        HashMap dehumidifierPt = CCUHsApi.getInstance().read("point and standalone and dehumidifier and cpu and cmd " +
                                                             "and his and equipRef== \"" + equipRef + "\"");
        if (!dehumidifierPt.isEmpty())
            CCUHsApi.getInstance().deleteEntityTree(dehumidifierPt.get("id").toString());
    }
    
    private static void deleteFanStage2Point(String equipRef) {
        HashMap fnStg2Pt = CCUHsApi.getInstance().read("point and standalone and fan and stage2 and cpu and cmd and " +
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
                                              .addMarker("zone").addMarker("logical").addMarker("cpu").addMarker("cmd")
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
                                                .addMarker("zone").addMarker("logical").addMarker("cpu").addMarker("cmd")
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
                                             .addMarker("his").addMarker("zone").addMarker("logical").addMarker("cpu")
                                             .addMarker("cmd").addMarker("runtime")
                                             .setGroup(equip.getGroup())
                                             .setTz(equip.getTz())
                                             .build();
        String r6ID = CCUHsApi.getInstance().addPoint(fanStage2);
        CCUHsApi.getInstance().writeHisValById(r6ID, 0.0);
        SmartStat.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()), Port.RELAY_SIX.name(), r6ID);
    }
    
    private static void adjustCPUFanMode(Equip equip, CCUHsApi hayStack) {
        
        double curFanSpeed = hayStack.readDefaultVal("point and zone and userIntent and fan and " +
                                                     "mode and equipRef == \"" + equip.getId() + "\"");
        /**
         * When currently available fanSpeed configuration is not OFF , set fanSpeed to AUTO
         * When none of fan configuration is enabled, Set fanSpeed to OFF.
         */
        double fanLowEnabled = getConfigNumVal("enable and relay3", equip.getGroup());
        double fanHighEnabled = getConfigNumVal("enable and relay6", equip.getGroup());
    
        double fallbackFanSpeed = SSEFanStage.OFF.ordinal();
        if (fanHighEnabled > 0) {
            double relay6Type = getConfigNumVal("relay6 and type",equip.getGroup());
            if (relay6Type == SmartStatFanRelayType.FAN_STAGE2.ordinal()) {
                fallbackFanSpeed = curFanSpeed; //Nothing to do.
            } else {
                fallbackFanSpeed = SSEFanStage.LOW_ALL_TIME.ordinal();
            }
        }
        if (fanLowEnabled > 0) {
            fallbackFanSpeed = SSEFanStage.LOW_ALL_TIME.ordinal();
        }
        CcuLog.i(L.TAG_CCU_PUBNUB, "adjustCPUFanMode "+curFanSpeed+" -> "+fallbackFanSpeed);
        if (curFanSpeed > fallbackFanSpeed) {
            hayStack.writeDefaultVal("point and zone and userIntent and fan and " +
                                     "mode and equipRef == \"" + equip.getId() + "\"",
                                     fallbackFanSpeed);
        }
    }
    
    private static void adjustConditioningMode(Equip equip, CCUHsApi hayStack) {
        
        String conditioningModeId = CCUHsApi.getInstance().readId("point and zone and userIntent and conditioning and" +
                                                                  " mode and equipRef == \"" + equip.getId() + "\"");
        if (conditioningModeId.isEmpty()) {
            CcuLog.e(L.TAG_CCU_ZONE, "ConditioningMode point does not exist for update : "+equip.getDisplayName());
            return;
        }
        double curCondMode = CCUHsApi.getInstance().readDefaultValById(conditioningModeId);
    
        double isCoolingOn = getConfigNumVal("enable and relay3", equip.getGroup());
        double isHeatingOn = getConfigNumVal("enable and relay6", equip.getGroup());
        
        double conditioningMode = curCondMode;
        if (isHeatingOn == 0) {
            if (curCondMode == SSEConditioningMode.AUTO.ordinal() || curCondMode == SSEConditioningMode.HEAT_ONLY.ordinal() ) {
                conditioningMode = SSEConditioningMode.OFF.ordinal();
            }
        }
        if (isCoolingOn == 0){
            if (curCondMode == SSEConditioningMode.AUTO.ordinal() || curCondMode == SSEConditioningMode.COOL_ONLY.ordinal() ) {
                conditioningMode = SSEConditioningMode.OFF.ordinal();
            }
        }
        CcuLog.i(L.TAG_CCU_PUBNUB, "adjust4PFCUConditioningMode "+curCondMode+" -> "+conditioningMode);
        if (curCondMode != conditioningMode) {
            hayStack.writeDefaultValById(conditioningModeId, conditioningMode);
            hayStack.writeHisValById(conditioningModeId, conditioningMode);
        }
    }
    
    
    private static void writePointFromJson(String id, double val, JsonObject msgObject, CCUHsApi hayStack) {
        try {
            int level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt();
            int duration = msgObject.get(HayStackConstants.WRITABLE_ARRAY_DURATION) != null ? msgObject.get(
                HayStackConstants.WRITABLE_ARRAY_DURATION).getAsInt() : 0;
            hayStack.writePointForCcuUser(id, level, val, duration);
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : "+msgObject+" ; "+e.getMessage());
        }
    }
    
    private static double getConfigNumVal(String tags, String nodeAddr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and standalone and cpu and "+tags+" and group == \""+nodeAddr+"\"");
    }
}
