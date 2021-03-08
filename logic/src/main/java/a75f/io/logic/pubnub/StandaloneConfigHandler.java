package a75f.io.logic.pubnub;

import com.google.gson.JsonObject;

import a75f.io.api.haystack.CCUHsApi;
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
            TwoPipeFanCoilUnitUtil.updateFCUProfile(val, configPoint, msgObject, hayStack);
        }
        //CPU & HPU to come here.
    }
    
   
}
