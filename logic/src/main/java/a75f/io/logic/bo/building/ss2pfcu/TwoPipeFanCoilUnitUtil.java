package a75f.io.logic.bo.building.ss2pfcu;

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
import a75f.io.logic.bo.building.hvac.SSEConditioningMode;
import a75f.io.logic.bo.building.hvac.SSEFanStage;
import a75f.io.logic.bo.haystack.device.SmartStat;

public class TwoPipeFanCoilUnitUtil {
    
    public static void updateFCUProfile(Point configPoint, JsonObject msgObject,
                                             CCUHsApi hayStack) {
    
        try {
            double configVal = msgObject.get("val").getAsDouble();
            if (configPoint.getMarkers().contains(Tags.CONFIG)) {
                updateConfig(configVal, configPoint, msgObject, hayStack);
            } else if (configPoint.getMarkers().contains(Tags.ENABLE) && configPoint.getMarkers().contains(
                Tags.OCCUPANCY) && configPoint.getMarkers().contains(Tags.CONTROL)) {
                updateOccupancyPoint(configVal, configPoint, msgObject, hayStack);
            } else if (configPoint.getMarkers().contains(Tags.USERINTENT)) {
                updateUserIntent(configVal, configPoint, msgObject, hayStack);
            }
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to update : "+configPoint.getDisplayName()+" ; "+msgObject+" "+
                                                                                                         e.getMessage());
        }
        
    }
    public static void updateConfig(double configVal, Point configPoint, JsonObject msgObject, CCUHsApi hayStack) {
        
        HashMap equipMap = hayStack.readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String nodeAddr = equip.getGroup();
        CcuLog.i(L.TAG_CCU_PUBNUB,
                 "updateConfig "+nodeAddr+" "+configPoint);
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
        } else if (configPoint.getMarkers().contains(Tags.TH1)) {
            SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.TH1_IN.name(),
                                      configVal > 0 ? true : false);
        } else if (configPoint.getMarkers().contains(Tags.TH2)) {
            SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.TH2_IN.name(),
                                      configVal > 0 ? true : false);
        }
        writePointFromJson(configPoint.getId(), configVal, msgObject, hayStack);
        hayStack.syncPointEntityTree();
        adjustFCUFanMode(configPoint,hayStack);
    }
    
    private static void updateOccupancyPoint(double configVal, Point configPoint,
                                             JsonObject msgObject, CCUHsApi hayStack) {
    
        HashMap equipMap = hayStack.readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        
        HashMap occDetPoint = hayStack.read("point and occupancy and detection and fcu and his and " +
                                                          "equipRef== \"" + equip.getId() + "\"");
    
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateOccupancyPoint "+configVal+" "+occDetPoint);
    
        if(configVal > 0){
            //Create occupancy detection point if it does not exist.
            if (occDetPoint.isEmpty()) {
                String profileTag = configPoint.getMarkers().contains(Tags.PIPE2) ? Tags.PIPE2 : Tags.PIPE4;
                Point occupancyDetection = new Point.Builder()
                                               .setDisplayName(equip.getDisplayName()+"-occupancyDetection")
                                               .setEquipRef(equip.getId())
                                               .setSiteRef(equip.getSiteRef())
                                               .setRoomRef(equip.getRoomRef())
                                               .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                               .addMarker("occupancy").addMarker("detection").addMarker("fcu")
                                               .addMarker(profileTag).addMarker("his").addMarker("zone")
                                               .setGroup(equip.getGroup())
                                               .setEnums("false,true")
                                               .setTz(CCUHsApi.getInstance().getTimeZone())
                                               .build();
                String occupancyDetectionId = hayStack.addPoint(occupancyDetection);
                hayStack.writeHisValById(occupancyDetectionId, 0.0);
            }
            
        } else {
            if (!occDetPoint.isEmpty())
                hayStack.deleteEntityTree(occDetPoint.get("id").toString());
        }
        writePointFromJson(configPoint.getId(), configVal, msgObject, hayStack);
    }
    
    public static void updateUserIntent(double configVal, Point configPoint, JsonObject msgObject, CCUHsApi hayStack) {
    
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateUserIntent "+configVal+" "+configPoint);
        if (configPoint.getMarkers().contains(Tags.FAN)
            && configPoint.getMarkers().contains(Tags.MODE)) {
            
            updateFCUFanMode(configVal, configPoint, msgObject, hayStack);
        } else if (configPoint.getMarkers().contains(Tags.CONDITIONING) &&
                   configPoint.getMarkers().contains(Tags.PIPE2)) {
            update2PFCUConditioningMode(configVal, configPoint, msgObject, hayStack);
        } else if (configPoint.getMarkers().contains(Tags.CONDITIONING) &&
                   configPoint.getMarkers().contains(Tags.PIPE4)) {
            update4PFCUConditioningMode(configVal, configPoint, msgObject, hayStack);
        }
    }
    
    private static void updateFCUFanMode(double configVal, Point configPoint, JsonObject msgObject, CCUHsApi hayStack) {
    
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateFCUFanMode "+configVal+" "+configPoint);
        
        SSEFanStage maxFanSpeed = getMaxAvailableFanSpeed(configPoint, hayStack);
        
        if (configVal > maxFanSpeed.ordinal() && maxFanSpeed.ordinal() > SSEFanStage.OFF.ordinal()) {
            configVal = maxFanSpeed.ordinal();
        } else if (configVal > maxFanSpeed.ordinal()) {
            configVal = SSEFanStage.OFF.ordinal();
        }
        writePointFromJson(configPoint.getId(), configVal, msgObject, hayStack);
        hayStack.writeHisValById(configPoint.getId(), configVal);
    }
    
    private static void adjustFCUFanMode(Point configPoint, CCUHsApi hayStack) {
        SSEFanStage maxFanSpeed = getMaxAvailableFanSpeed(configPoint, hayStack);
        double curFanSpeed = hayStack.readDefaultVal("point and zone and userIntent and fan and " +
                                                         "mode and equipRef == \"" + configPoint.getEquipRef() + "\"");
        CcuLog.i(L.TAG_CCU_PUBNUB, "adjustFCUFanMode "+curFanSpeed+" -> "+maxFanSpeed);
        if (curFanSpeed > maxFanSpeed.ordinal()) {
            hayStack.writeDefaultVal("point and zone and userIntent and fan and " +
                                    "mode and equipRef == \"" + configPoint.getEquipRef() + "\"",
                                     (double)maxFanSpeed.ordinal());
        }
    }
    
    private static SSEFanStage getMaxAvailableFanSpeed(Point configPoint, CCUHsApi hayStack) {
        
        double isFanLowEnabled = hayStack.readDefaultVal("point and zone and config and enable and " +
                                                         "relay3 and equipRef == \"" + configPoint.getEquipRef() +
                                                         "\"");
        double isFanMediumEnabled = hayStack.readDefaultVal("point and zone and config and enable and relay1 and equipRef == \"" +
                                                            configPoint.getEquipRef() + "\"");
        double isFanHighEnabled = hayStack.readDefaultVal("point and zone and config and enable and relay2 and equipRef == \"" +
                                                          configPoint.getEquipRef() + "\"");
    
        SSEFanStage maxFanSpeed = SSEFanStage.OFF;
        if (isFanHighEnabled > 0) {
            maxFanSpeed = SSEFanStage.HIGH_ALL_TIME;
        } else if (isFanMediumEnabled > 0) {
            maxFanSpeed = SSEFanStage.MEDIUM_ALL_TIME;
        } else if (isFanLowEnabled > 0) {
            maxFanSpeed = SSEFanStage.LOW_ALL_TIME;
        }
        return maxFanSpeed;
    }
    
    private static void update2PFCUConditioningMode(double configVal, Point configPoint,
                                                    JsonObject msgObject, CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "update2PFCUConditioningMode "+configVal+" "+configPoint);
        if (configPoint.getMarkers().contains(Tags.CONDITIONING)) {
            double conditioningRelay = hayStack.readDefaultVal("point and zone and config and enable " +
                                                                             "and relay6 and equipRef == \"" + configPoint.getEquipRef() + "\"");
    
            double conditioningMode = conditioningRelay > 0 ? configVal : 0;
            writePointFromJson(configPoint.getId(), conditioningRelay, msgObject, hayStack);
            hayStack.writeHisValById(configPoint.getId(), conditioningMode);
            
        }
    }
    
    private static void update4PFCUConditioningMode(double configVal, Point configPoint,
                                                    JsonObject msgObject, CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "update4PFCUConditioningMode "+configVal+" "+configPoint);
        if (configPoint.getMarkers().contains(Tags.CONDITIONING)) {
            double isCoolingOn = hayStack.readDefaultVal("point and zone and config and enable and " +
                                                                      "relay6 and equipRef == \"" + configPoint.getEquipRef() + "\"");
            double isHeatingOn = hayStack.readDefaultVal("point and zone and config and enable and " +
                                                                      "relay4 and equipRef == \"" + configPoint.getEquipRef() + "\"");
    
            double conditioningMode = configVal;
            if (isHeatingOn == 0) {
                if (configVal == SSEConditioningMode.AUTO.ordinal() || configVal == SSEConditioningMode.HEAT_ONLY.ordinal() ) {
                    conditioningMode = SSEConditioningMode.OFF.ordinal();
                }
            } else if (isCoolingOn == 0){
                if (configVal == SSEConditioningMode.AUTO.ordinal() || configVal == SSEConditioningMode.COOL_ONLY.ordinal() ) {
                    conditioningMode = SSEConditioningMode.OFF.ordinal();
                }
            }
            writePointFromJson(configPoint.getId(), conditioningMode, msgObject, hayStack);
            hayStack.writeHisValById(configPoint.getId(), conditioningMode);
            
        }
    }
    
    private static void writePointFromJson(String id, double val, JsonObject msgObject, CCUHsApi hayStack) {
        try {
            String who = msgObject.get(HayStackConstants.WRITABLE_ARRAY_WHO).getAsString();
            int level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt();
            int duration = msgObject.get(HayStackConstants.WRITABLE_ARRAY_DURATION) != null ? msgObject.get(
                HayStackConstants.WRITABLE_ARRAY_DURATION).getAsInt() : 0;
            hayStack.writePointLocal(id, level, who, val, duration);
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : "+msgObject+" ; "+e.getMessage());
        }
    }
}
