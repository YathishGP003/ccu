package a75f.io.logic.pubnub;

import com.google.gson.JsonObject;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ss2pfcu.TwoPipeFanCoilUnitUtil;

public class StandaloneConfigHandler {
    
    public static void updateConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateStandaloneConfigPoint " + msgObject.toString());
        
        int val = msgObject.get("val").getAsInt();
        if (configPoint.getMarkers().contains(Tags.FCU)) {
            TwoPipeFanCoilUnitUtil.updateRelayConfig(val, configPoint, hayStack);
        }
        writePointFromJson(configPoint.getId(), msgObject, hayStack);
        
    }
    
    private static void writePointFromJson(String id, JsonObject msgObject, CCUHsApi hayStack) {
        try {
            String val = msgObject.get(HayStackConstants.WRITABLE_ARRAY_VAL).getAsString();
            String who = msgObject.get(HayStackConstants.WRITABLE_ARRAY_WHO).getAsString();
            double value = Double.parseDouble(val);
            int level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt();
            int duration = msgObject.get(HayStackConstants.WRITABLE_ARRAY_DURATION) != null ? msgObject.get(
                HayStackConstants.WRITABLE_ARRAY_DURATION).getAsInt() : 0;
            hayStack.writePointLocal(id, level, who, value, duration);
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : "+msgObject+" ; "+e.getMessage());
        }
    }
    
   
}
