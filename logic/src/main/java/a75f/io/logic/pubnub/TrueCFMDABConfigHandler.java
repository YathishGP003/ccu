package a75f.io.logic.pubnub;

import com.google.gson.JsonObject;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.dab.DabProfileConfiguration;
import a75f.io.logic.bo.building.truecfm.TrueCFMPointsHandler;
import a75f.io.logic.tuners.TrueCFMTuners;
import a75f.io.logic.tuners.TunerConstants;

public class TrueCFMDABConfigHandler {
    public static void updateDABConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        double value = msgObject.get("val").getAsDouble();

        DabProfileConfiguration dabProfileConfiguration = new DabProfileConfiguration();
        if (value > 0) {
            dabProfileConfiguration.minCFMForIAQ = 100;
            dabProfileConfiguration.kFactor = 2;
            TrueCFMPointsHandler.createTrueCFMDABPoints(hayStack, equip.getId(), dabProfileConfiguration);
            TrueCFMTuners.createTrueCfmTuners(hayStack, equip, Tags.DAB, TunerConstants.DAB_TUNER_GROUP);
            TrueCFMPointsHandler.createTrueCfmSpPoints(hayStack, equip, Tags.DAB, null);
        } else {
            TrueCFMPointsHandler.deleteTrueCFMPoints(hayStack, equip.getId());
        }
        writePointFromJson(configPoint, msgObject, hayStack);
    }
    private static void writePointFromJson(Point configPoint, JsonObject msgObject, CCUHsApi hayStack) {
        String who = msgObject.get(HayStackConstants.WRITABLE_ARRAY_WHO).getAsString();
        int level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt();
        double val = msgObject.get(HayStackConstants.WRITABLE_ARRAY_VAL).getAsDouble();
        int duration = msgObject.get(HayStackConstants.WRITABLE_ARRAY_DURATION) != null ? msgObject.get(
                HayStackConstants.WRITABLE_ARRAY_DURATION).getAsInt() : 0;
        hayStack.writePointLocal(configPoint.getId(), level, who, val, duration);
    }
}
