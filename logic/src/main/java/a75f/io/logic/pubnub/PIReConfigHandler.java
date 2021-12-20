package a75f.io.logic.pubnub;

import com.google.gson.JsonObject;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.plc.PlcRelayConfigHandler;


/*
 * created by spoorthidev on 26-Oct-2021
 */

public class PIReConfigHandler {

    private static final String INPUT_TAG = "input";
    private static final String TARGET_TAG = "target";
    private static final String RANGE_TAG = "prange";
    private static final String MIDPOINT_TAG = "midpoint";
    private static final String INVERSION_TAG = "inversion";
    private static final String ANALOG2_TAG = "analog2";
    private static final String ENABLED_TAG = "enabled";
    private static final String SETPOINT_TAG = "setpoint";
    private static final String SENSOR_TAG = "sensor";
    private static final String ANALOG1_TAG = "analog1";
    private static final String OUTPUT_TAG = "output";
    private static final String RELAY1_TAG = "relay1";
    private static final String RELAY2_TAG = "relay2";


    public static void updateConfigPoint(JsonObject msgObject, Point configPoint) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "updatePIConfigPoint " + configPoint + " " + msgObject.toString()
                + " Markers =" + configPoint.getMarkers());

        if(configPoint.getMarkers().contains(INPUT_TAG)){
            PlcRelayConfigHandler.updateInputSensor(msgObject,configPoint);
        }else if(configPoint.getMarkers().contains(TARGET_TAG)){
            PlcRelayConfigHandler.updateTargetValue(msgObject,configPoint);
        }else if(configPoint.getMarkers().contains(RANGE_TAG)){
            PlcRelayConfigHandler.updateRangeValue(msgObject,configPoint);
        }else if(configPoint.getMarkers().contains(MIDPOINT_TAG)){
            PlcRelayConfigHandler.updateMidpointValue(msgObject,configPoint);
        }else if(configPoint.getMarkers().contains(INVERSION_TAG)){
            PlcRelayConfigHandler.updateInversionValue(msgObject,configPoint);
        }else if(configPoint.getMarkers().contains(ANALOG2_TAG) &&
                configPoint.getMarkers().contains(ENABLED_TAG)) {
            PlcRelayConfigHandler.updateEnableAn2Value(msgObject, configPoint);
        }else if(configPoint.getMarkers().contains(SETPOINT_TAG) &&
                configPoint.getMarkers().contains(SENSOR_TAG)) {
            PlcRelayConfigHandler.updateAn2SetpointValue(msgObject, configPoint);
        }else if(configPoint.getMarkers().contains(ANALOG1_TAG) &&
                configPoint.getMarkers().contains(OUTPUT_TAG)) {
            PlcRelayConfigHandler.updateAn1OutputValue(msgObject, configPoint);
        }else if(configPoint.getMarkers().contains(RELAY1_TAG)
        ||configPoint.getMarkers().contains(RELAY2_TAG)) {
            PlcRelayConfigHandler.updateRelayValue(msgObject, configPoint);
        }
        writePointFromJson(configPoint, msgObject, CCUHsApi.getInstance());
    }

    private static void writePointFromJson(Point configPoint, JsonObject msgObject, CCUHsApi hayStack) {
        try {
            String who = msgObject.get(HayStackConstants.WRITABLE_ARRAY_WHO).getAsString();
            int level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt();
            double val = msgObject.get(HayStackConstants.WRITABLE_ARRAY_VAL).getAsDouble();
            int duration = msgObject.get(HayStackConstants.WRITABLE_ARRAY_DURATION) != null ? msgObject.get(
                    HayStackConstants.WRITABLE_ARRAY_DURATION).getAsInt() : 0;
            hayStack.writePointLocal(configPoint.getId(), level, who, val, duration);
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.getMessage());
        }
    }
}
