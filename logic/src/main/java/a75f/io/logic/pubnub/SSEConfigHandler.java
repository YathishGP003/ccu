package a75f.io.logic.pubnub;

import android.util.Log;

import com.google.gson.JsonObject;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Input;
import a75f.io.logic.bo.building.sse.InputActuatorType;
import a75f.io.logic.bo.building.sse.SingleStageEquipUtil;

class SSEConfigHandler {
    
    public static void updateConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateSSEConfigPoint " + configPoint+" "+msgObject.toString());
    
        int val = msgObject.get("val").getAsInt();
        if (configPoint.getMarkers().contains(Tags.RELAY1)) {
            SingleStageEquipUtil.updateRelay1Config( val, configPoint);
        } else if (configPoint.getMarkers().contains(Tags.RELAY2)) {
            SingleStageEquipUtil.updateRelay2Config( val, configPoint);
        } else if (configPoint.getMarkers().contains(Tags.TH1)
                    || configPoint.getMarkers().contains(Tags.TH2)) {
            SingleStageEquipUtil.updateThermistorConfig( val, configPoint);
        } else if (configPoint.getMarkers().contains(Tags.ANALOG1)) {
            InputActuatorType inputActuatorType = InputActuatorType.getEnum(String.valueOf(val));
            SingleStageEquipUtil.updateAnalogIn1Config(inputActuatorType,configPoint,true);
        }
        writePointFromJson(configPoint, msgObject, hayStack);
        
        
        
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
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : "+msgObject+" ; "+e.getMessage());
        }
    }
}
