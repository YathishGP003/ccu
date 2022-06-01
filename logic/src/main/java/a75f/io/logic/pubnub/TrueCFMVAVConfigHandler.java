package a75f.io.logic.pubnub;

import com.google.gson.JsonObject;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.truecfm.TrueCFMPointsHandler;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;
import a75f.io.logic.tuners.TrueCFMTuners;
import a75f.io.logic.tuners.TunerConstants;

public class TrueCFMVAVConfigHandler {
    public static void updateConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        double value = msgObject.get("val").getAsDouble();
        String fanMarker = "";
        if (equip.getProfile().equals(ProfileType.VAV_SERIES_FAN.name())) {
            fanMarker = "series";
        } else if (equip.getProfile().equals(ProfileType.VAV_PARALLEL_FAN.name())) {
            fanMarker = "parallel";
        }
        VavProfileConfiguration vavProfileConfiguration = new VavProfileConfiguration();
            if (value > 0) {
                vavProfileConfiguration.numMaxCFMReheating = 500;
                vavProfileConfiguration.numMinCFMCooling = 100;
                vavProfileConfiguration.nuMaxCFMCooling = 500;
                vavProfileConfiguration.numMinCFMReheating = 100;
                vavProfileConfiguration.kFactor = 2;
                TrueCFMPointsHandler.createTrueCFMVavPoints(hayStack, equip.getId(), vavProfileConfiguration,
                 fanMarker);
                TrueCFMTuners.createTrueCfmTuners(hayStack, equip, Tags.VAV, TunerConstants.VAV_TUNER_GROUP);
                TrueCFMPointsHandler.deleteNonCfmDamperPoints(hayStack, equip.getId());
            } else {
                vavProfileConfiguration.minDamperCooling = 20;
                vavProfileConfiguration.minDamperHeating = 20;
                vavProfileConfiguration.maxDamperCooling = 100;
                TrueCFMPointsHandler.deleteTrueCFMPoints(hayStack, equip.getId());
                TrueCFMPointsHandler.createNonCfmDamperConfigPoints(hayStack, equip, vavProfileConfiguration, fanMarker);
            }
        writePointFromJson(configPoint, msgObject, hayStack);
    }

    public static void updateMinCoolingConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        double maxCfmValue = msgObject.get("val").getAsDouble();
        Double minCfmValue = CCUHsApi.getInstance().readDefaultVal("point and zone and config and vav and trueCfm and min and cooling and equipRef == \""+equip.getId()+"\"");
        if (minCfmValue > maxCfmValue) {
            CCUHsApi.getInstance().writeDefaultVal("vav and trueCfm and min and cooling and group == \""+equip.getGroup()+"\"", maxCfmValue);
        }
        writePointFromJson(configPoint, msgObject, hayStack);
    }

    public static void updateMinReheatingConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        double maxHeatingCfmValue = msgObject.get("val").getAsDouble();
        Double minHeatingCfmValue = CCUHsApi.getInstance().readDefaultVal("point and zone and config and vav and trueCfm and min and heating and equipRef == \""+equip.getId()+"\"");
        if (minHeatingCfmValue > maxHeatingCfmValue) {
            CCUHsApi.getInstance().writeDefaultVal("vav and trueCfm and min and heating and group == \""+equip.getGroup()+"\"", maxHeatingCfmValue);
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

