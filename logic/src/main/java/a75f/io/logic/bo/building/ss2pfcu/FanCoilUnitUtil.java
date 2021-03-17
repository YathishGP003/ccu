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
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.hvac.StandaloneFanStage;
import a75f.io.logic.bo.haystack.device.SmartStat;

/**
 * Util class to handle configuration changes of FCU profiles via Pubnub (Reconfig)
 * Config changes from CCU UI are handled in TwoPipeFanCoilUnitEquip.java and FourPipeFanCoilUnitEquip.java classes.
 * Both 2 Pipe and 4 Pipes are handled here as it mostly the same.
 */
public class FanCoilUnitUtil {
    
    /**
     * Public method thats is invoked from pubnub handler module.
     *
     * @param configPoint -  Point entity thats being updated.
     * @param msgObject    - The JsonObject extracted from pubnub
     * @param hayStack
     */
    public static void updateFCUProfile(Point configPoint, JsonObject msgObject,
                                             CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateFCUProfile " + configPoint);
        try {
            double configVal = msgObject.get("val").getAsDouble();
            if (configPoint.getMarkers().contains(Tags.CONFIG)) {
                updateConfig(configVal, configPoint, msgObject, hayStack);
            } else if (configPoint.getMarkers().contains(Tags.ENABLE) && configPoint.getMarkers().contains(
                Tags.OCCUPANCY) && configPoint.getMarkers().contains(Tags.CONTROL)) {
                updateOccupancyPoint(configVal, configPoint, msgObject, hayStack);
            } else if (configPoint.getMarkers().contains(Tags.USERINTENT)) {
                updateUserIntent(configVal, configPoint, msgObject, hayStack);
            } else {
                writePointFromJson(configPoint.getId(), configVal, msgObject, hayStack);
            }
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to update : "+configPoint.getDisplayName()+" ; "+msgObject+" "+
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
        } else if (configPoint.getMarkers().contains(Tags.TH1)) {
            SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.TH1_IN.name(),
                                      configVal > 0 ? true : false);
        } else if (configPoint.getMarkers().contains(Tags.TH2)) {
            SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.TH2_IN.name(),
                                      configVal > 0 ? true : false);
        }
        writePointFromJson(configPoint.getId(), configVal, msgObject, hayStack);
        hayStack.syncPointEntityTree();
        
        /**
         * Relay selection changes might impact available FanModes and conditioning modes.
         * Adjust them here if required.
         */
        adjustFCUFanMode(configPoint,hayStack);
        if (configPoint.getMarkers().contains(Tags.PIPE4)) {
            adjust4PFCUConditioningMode(configPoint, hayStack);
        }
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
        writePointFromJson( configPoint.getId(), configVal, msgObject, hayStack);
    }
    
    private static void updateUserIntent(double configVal, Point configPoint, JsonObject msgObject, CCUHsApi hayStack) {
    
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
        
        StandaloneFanStage maxFanSpeed = getMaxAvailableFanSpeed(configPoint, hayStack);
        
        double fanSpeedVal = configVal;
        if (configVal > maxFanSpeed.ordinal() && maxFanSpeed.ordinal() > StandaloneFanStage.OFF.ordinal()) {
            fanSpeedVal = maxFanSpeed.ordinal();
        } else if (configVal > maxFanSpeed.ordinal()) {
            fanSpeedVal = StandaloneFanStage.OFF.ordinal();
        }
        if (fanSpeedVal != configVal) {
            hayStack.writeDefaultVal(configPoint.getId(), fanSpeedVal);
        }
        writePointFromJson( configPoint.getId(), configVal, msgObject, hayStack);
        hayStack.writeHisValById(configPoint.getId(), configVal);
    }
    
    private static void adjustFCUFanMode(Point configPoint, CCUHsApi hayStack) {
        StandaloneFanStage maxFanSpeed = getMaxAvailableFanSpeed(configPoint, hayStack);
        double curFanSpeed = hayStack.readDefaultVal("point and zone and userIntent and fan and " +
                                                         "mode and equipRef == \"" + configPoint.getEquipRef() + "\"");
        CcuLog.i(L.TAG_CCU_PUBNUB, "adjustFCUFanMode "+curFanSpeed+" -> "+maxFanSpeed);
        /**
         * When currently available fanSpeed configuration is not OFF , set fanSpeed to AUTO
         * When none of fan configuration is enabled, Set fanSpeed to OFF.
         */
        double fallbackFanSpeed = curFanSpeed;
        if (curFanSpeed > maxFanSpeed.ordinal() && maxFanSpeed.ordinal() > StandaloneFanStage.OFF.ordinal()) {
            fallbackFanSpeed = StandaloneFanStage.AUTO.ordinal();
        } else if (curFanSpeed > maxFanSpeed.ordinal()) {
            fallbackFanSpeed = StandaloneFanStage.OFF.ordinal();
        }
        
        if (curFanSpeed != fallbackFanSpeed) {
            hayStack.writeDefaultVal("point and zone and userIntent and fan and " +
                                    "mode and equipRef == \"" + configPoint.getEquipRef() + "\"",
                                     fallbackFanSpeed);
        }
    }
    
    private static void adjust4PFCUConditioningMode(Point configPoint, CCUHsApi hayStack) {
        
        String conditioningModeId = CCUHsApi.getInstance().readId("point and zone and userIntent and conditioning and" +
                                                                  " mode and equipRef == \"" + configPoint.getEquipRef() + "\"");
        if (conditioningModeId.isEmpty()) {
            CcuLog.e(L.TAG_CCU_ZONE, "ConditioningMode point does not exist for update : "+configPoint.getDisplayName());
            return;
        }
        double curCondMode = CCUHsApi.getInstance().readDefaultValById(conditioningModeId);
        
        double isCoolingOn = hayStack.readDefaultVal("point and zone and config and enable and " +
                                                     "relay6 and equipRef == \"" + configPoint.getEquipRef() + "\"");
        double isHeatingOn = hayStack.readDefaultVal("point and zone and config and enable and " +
                                                     "relay4 and equipRef == \"" + configPoint.getEquipRef() + "\"");
    
        double conditioningMode = curCondMode;
        if (Math.abs(isHeatingOn) < 0.01) {
            if (curCondMode == StandaloneConditioningMode.AUTO.ordinal() || curCondMode == StandaloneConditioningMode.HEAT_ONLY.ordinal() ) {
                conditioningMode = StandaloneConditioningMode.OFF.ordinal();
            }
        }
        if (Math.abs(isCoolingOn) < 0.01){
            if (curCondMode == StandaloneConditioningMode.AUTO.ordinal() || curCondMode == StandaloneConditioningMode.COOL_ONLY.ordinal() ) {
                conditioningMode = StandaloneConditioningMode.OFF.ordinal();
            }
        }
        CcuLog.i(L.TAG_CCU_PUBNUB, "adjust4PFCUConditioningMode "+curCondMode+" -> "+conditioningMode);
        if (curCondMode != conditioningMode) {
            hayStack.writeDefaultValById(conditioningModeId, conditioningMode);
            hayStack.writeHisValById(conditioningModeId, conditioningMode);
        }
    }
    
    
    private static StandaloneFanStage getMaxAvailableFanSpeed(Point configPoint, CCUHsApi hayStack) {
        
        double isFanLowEnabled = hayStack.readDefaultVal("point and zone and config and enable and " +
                                                         "relay3 and equipRef == \"" + configPoint.getEquipRef() +
                                                         "\"");
        double isFanMediumEnabled = hayStack.readDefaultVal("point and zone and config and enable and relay1 and equipRef == \"" +
                                                            configPoint.getEquipRef() + "\"");
        double isFanHighEnabled = hayStack.readDefaultVal("point and zone and config and enable and relay2 and equipRef == \"" +
                                                          configPoint.getEquipRef() + "\"");
    
        StandaloneFanStage maxFanSpeed = StandaloneFanStage.OFF;
        if (isFanHighEnabled > 0) {
            maxFanSpeed = StandaloneFanStage.HIGH_ALL_TIME;
        } else if (isFanMediumEnabled > 0) {
            maxFanSpeed = StandaloneFanStage.MEDIUM_ALL_TIME;
        } else if (isFanLowEnabled > 0) {
            maxFanSpeed = StandaloneFanStage.LOW_ALL_TIME;
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
            if (Math.abs(isHeatingOn) < 0.01) {
                if (configVal == StandaloneConditioningMode.AUTO.ordinal() || configVal == StandaloneConditioningMode.HEAT_ONLY.ordinal() ) {
                    conditioningMode = StandaloneConditioningMode.OFF.ordinal();
                }
            } else if (Math.abs(isCoolingOn) < 0.01){
                if (configVal == StandaloneConditioningMode.AUTO.ordinal() || configVal == StandaloneConditioningMode.COOL_ONLY.ordinal() ) {
                    conditioningMode = StandaloneConditioningMode.OFF.ordinal();
                }
            }
            writePointFromJson(configPoint.getId(), conditioningMode, msgObject, hayStack);
            hayStack.writeHisValById(configPoint.getId(), conditioningMode);
            
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
}
