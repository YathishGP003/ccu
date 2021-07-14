package a75f.io.logic.bo.building.hyperstatsense;

import android.util.Log;

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
import a75f.io.logic.bo.haystack.device.DeviceUtil;
import a75f.io.logic.bo.haystack.device.SmartStat;

public class HyperStatSenseUtil {

    private static String LOG_TAG = "HyperStatSenseUtil";

    public static void updateConfigEnabled(JsonObject msgObject, Point configPoint, CCUHsApi hayStack){
        HashMap equipMap = hayStack.readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String nodeAddr = equip.getGroup();
        int configVal = msgObject.get("val").getAsInt();
        if (configPoint.getMarkers().contains(Tags.TH1)) {
            DeviceUtil.setPointEnabled(Integer.parseInt(nodeAddr), Port.TH1_IN.name(),
                    configVal > 0 ? true : false);
        } else if (configPoint.getMarkers().contains(Tags.TH2)) {
            DeviceUtil.setPointEnabled(Integer.parseInt(nodeAddr), Port.TH2_IN.name(),
                    configVal > 0 ? true : false);
        }else if (configPoint.getMarkers().contains(Tags.ANALOG1)) {
            DeviceUtil.setPointEnabled(Integer.parseInt(nodeAddr), Port.ANALOG_IN_ONE.name(),
                    configVal > 0 ? true : false);
        }else if (configPoint.getMarkers().contains(Tags.ANALOG2)) {
            DeviceUtil.setPointEnabled(Integer.parseInt(nodeAddr), Port.ANALOG_IN_TWO.name(),
                    configVal > 0 ? true : false);
        }
        writePointFromJson(configPoint.getId(), configVal, msgObject, hayStack);
        hayStack.syncPointEntityTree();

    }

    public static void updatetempOffset(JsonObject msgObject, Point configPoint, CCUHsApi hayStack){
        double val = msgObject.get("val").getAsDouble();
        hayStack.writeDefaultValById(configPoint.getId(), val);
    }


    public static void updateConfig(JsonObject msgObject, Point configPoint, CCUHsApi hayStack){
        HashMap equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        CCUHsApi haystack = CCUHsApi.getInstance();
        String floorRef = equip.getFloorRef();
        String equipref = configPoint.getEquipRef();
        String nodeAddr = equip.getGroup();
        String roomRef = equip.getRoomRef();




        haystack.syncEntityTree();
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
