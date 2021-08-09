package a75f.io.logic.pubnub;

import com.google.gson.JsonObject;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.SystemProfile;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtu;
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu;
import a75f.io.logic.bo.building.system.vav.VavIERtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Handles remote config updates specific to System profile.
 */
class ConfigPointUpdateHandler {
    
    public static void updateConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigPoint "+msgObject.toString());
        if (configPoint.getMarkers().contains(Tags.IE)) {
            updateIEConfig(msgObject, configPoint, hayStack);
        }else if (configPoint.getMarkers().contains(Tags.ENABLED)) {
            updateConfigEnabled(msgObject, configPoint, hayStack);
        } else if ((configPoint.getMarkers().contains(Tags.ASSOCIATION) )
            || configPoint.getMarkers().contains(Tags.HUMIDIFIER)) {
            updateConfigAssociation(msgObject, configPoint, hayStack);
        }
    }
    
    private static void updateConfigEnabled(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigEnabled "+configPoint.getDisplayName());
        writePointFromJson(configPoint.getId(), msgObject, hayStack);
        updateConditioningMode();
    }
    
    private static void updateIEConfig(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateIEConfig "+configPoint.getDisplayName());
        VavIERtu systemProfile =  (VavIERtu) L.ccu().systemProfile;
        double val = msgObject.get("val").getAsDouble();
        if (configPoint.getMarkers().contains(Tags.MULTI_ZONE)) {
            systemProfile.handleMultiZoneEnable(val);
        } else {
            String userIntent = getUserIntentType(configPoint);
            if (userIntent != null) {
                systemProfile.setConfigEnabled(userIntent, val);
            }
        }
        writePointFromJson(configPoint.getId(), msgObject, hayStack);
    }
    
    private static void updateConfigAssociation(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigAssociation "+configPoint.getDisplayName());
        
        String relayType = getRelayTagFromConfig(configPoint);
        if (relayType == null) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Invalid config point update "+configPoint.toString());
        }
        
        SystemProfile systemProfile = L.ccu().systemProfile;
        double val = msgObject.get("val").getAsDouble();
        
        if (systemProfile instanceof DabFullyModulatingRtu) {
            ((DabFullyModulatingRtu) systemProfile).setHumidifierConfigVal(relayType+" and humidifier and type", val);
        } else if (systemProfile instanceof DabStagedRtu) {
            ((DabStagedRtu) systemProfile).setConfigAssociation(relayType, val);
        } else if (systemProfile instanceof VavFullyModulatingRtu) {
            ((VavFullyModulatingRtu) systemProfile).setHumidifierConfigVal(relayType+" and humidifier and type", val);
        } else if (systemProfile instanceof VavStagedRtu) {
            ((VavStagedRtu) systemProfile).setConfigAssociation(relayType, val);
        }
        writePointFromJson(configPoint.getId(), msgObject, hayStack);
    }
    
    private static void updateIEAddress(JsonObject msgObject, Point configPoint, CCUHsApi haystack) {
        writePointStrValFromJson(configPoint.getId(), msgObject, haystack );
    }
    
    /***
     * When cooling/heating is disabled remotely via reconfiguration, and the system cannot operate any more
     * in the selected conditioning mode, we should turn off the system.
     */
    public static void updateConditioningMode() {
        SystemMode systemMode = SystemMode.values()[(int) HSUtil.getSystemUserIntentVal("conditioning and mode")];
        if (systemMode == SystemMode.OFF) {
            return;
        }
        SystemProfile systemProfile = L.ccu().systemProfile;
        
        if ((systemMode == SystemMode.AUTO && (!systemProfile.isCoolingAvailable() || !systemProfile.isHeatingAvailable()))
            || (systemMode == SystemMode.COOLONLY && !systemProfile.isCoolingAvailable())
            || (systemMode == SystemMode.HEATONLY && !systemProfile.isHeatingAvailable())) {
            
            CcuLog.i(L.TAG_CCU_PUBNUB, "Reconfig disabling conditioning mode !");
            TunerUtil.writeSystemUserIntentVal("conditioning and mode", SystemMode.OFF.ordinal());
        }
    }
    
    private static void writePointFromJson(String id, JsonObject msgObject, CCUHsApi hayStack) {
        String who = msgObject.get("who").getAsString();
        double val = msgObject.get("val").getAsDouble();
        int duration = msgObject.get("duration") != null ? msgObject.get("duration").getAsInt() : 0;
        int level = msgObject.get("level").getAsInt();
        hayStack.writePointLocal(id, level, who, val, duration);
    }
    
    private static void writePointStrValFromJson(String id, JsonObject msgObject, CCUHsApi hayStack) {
        String who = msgObject.get("who").getAsString();
        String val = msgObject.get("val").getAsString().replaceAll("\"", "").trim();
        int duration = msgObject.get("duration") != null ? msgObject.get("duration").getAsInt() : 0;
        int level = msgObject.get("level").getAsInt();
        hayStack.writePointStrValLocal(id, level, who, val, duration);
    }
    
    private static String getRelayTagFromConfig(Point configPoint) {
        
        if (configPoint.getMarkers().contains(Tags.RELAY1)) {
            return Tags.RELAY1;
        } if (configPoint.getMarkers().contains(Tags.RELAY2)) {
            return Tags.RELAY2;
        } if (configPoint.getMarkers().contains(Tags.RELAY3)) {
            return Tags.RELAY3;
        } if (configPoint.getMarkers().contains(Tags.RELAY4)) {
            return Tags.RELAY4;
        } if (configPoint.getMarkers().contains(Tags.RELAY5)) {
            return Tags.RELAY5;
        } if (configPoint.getMarkers().contains(Tags.RELAY6)) {
            return Tags.RELAY6;
        } if (configPoint.getMarkers().contains(Tags.RELAY7)) {
            return Tags.RELAY7;
        }
        return null;
    }
    
    private static String getUserIntentType(Point configPoint) {
        if (configPoint.getMarkers().contains(Tags.COOLING)) {
            return Tags.COOLING;
        } else if (configPoint.getMarkers().contains(Tags.HEATING)) {
            return Tags.HEATING;
        } else if (configPoint.getMarkers().contains(Tags.FAN)) {
            return Tags.FAN;
        }
        return null;
    }
}

