package a75f.io.messaging.handler;

import com.google.gson.JsonObject;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.vav.VavProfile;

public class VAVZonePriorityHandler {
    public static void updateVAVZonePriority(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        double value = msgObject.get("val").getAsDouble();
        if((int)value < 0 || (int)value > 3) {
            CcuLog.d(L.TAG_CCU_PUBNUB, "updateVAVZonePriority - Message is not handled");
            return;
        }
        Short address = Short.parseShort(configPoint.getGroup());
        VavProfile profile = (VavProfile) L.getProfile((short) address);
        profile.updateZonePriority((int)value);
    }
}
