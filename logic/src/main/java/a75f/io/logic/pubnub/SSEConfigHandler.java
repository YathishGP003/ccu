package a75f.io.logic.pubnub;

import com.google.gson.JsonObject;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.sse.SingleStageEquipUtil;

class SSEConfigHandler {
    
    public static void updateConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateSSEConfigPoint " + msgObject.toString());
    
        int val = msgObject.get("val").getAsInt();
        if (configPoint.getMarkers().contains(Tags.RELAY1)) {
            SingleStageEquipUtil.updateRelay1Config( val, configPoint);
        } else if (configPoint.getMarkers().contains(Tags.RELAY1)) {
            SingleStageEquipUtil.updateRelay2Config( val, configPoint);
        }
    }
}
