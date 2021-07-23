package a75f.io.logic.pubnub;


import com.google.gson.JsonObject;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

import a75f.io.logic.bo.building.hyperstatsense.HyperStatSenseUtil;


/*
 * created by spoorthidev on 20-July-2021
 */

class HyperStatSenseConfigHandler {

    public static void updateConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateHyperStatSenseConfigPoint " + configPoint + " " + msgObject.toString()
                + " Markers =" + configPoint.getMarkers());

        if (configPoint.getMarkers().contains(Tags.ENABLED)) {
            HyperStatSenseUtil.updateConfigEnabled(msgObject, configPoint, hayStack);
        } else if (configPoint.getMarkers().contains("offset")) {
            HyperStatSenseUtil.updatetempOffset(msgObject, configPoint, hayStack);
        } else {
            HyperStatSenseUtil.updateConfig(msgObject, configPoint, hayStack);
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
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.getMessage());
        }
    }
}
