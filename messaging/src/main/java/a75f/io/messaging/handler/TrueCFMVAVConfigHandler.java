package a75f.io.messaging.handler;

import com.google.gson.JsonObject;

import org.projecthaystack.HDict;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.domain.VavAcbEquip;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.equips.VavEquip;
import a75f.io.domain.logic.ProfileEquipBuilder;
import a75f.io.domain.util.ModelLoader;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.vav.AcbProfileConfiguration;
import a75f.io.logic.bo.building.vav.VavAcbProfile;
import a75f.io.logic.bo.building.vav.VavProfile;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;

public class TrueCFMVAVConfigHandler {
    public static void updateVAVConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        double value = msgObject.get("val").getAsDouble();
        if(value == CCUHsApi.getInstance().readDefaultValById(configPoint.getId())) {
            CcuLog.d(L.TAG_CCU_PUBNUB, "updateVAVConfigPoint - Message is not handled");
            return;
        }
        short address = Short.parseShort(configPoint.getGroup());
        VavProfile profile = (VavProfile) L.getProfile(address);

        if (profile instanceof VavAcbProfile) {
            Equip equip = profile.getEquip();
            AcbProfileConfiguration config = (AcbProfileConfiguration) profile.getDomainProfileConfiguration();
            config.enableCFMControl.setEnabled(value > 0);
            ProfileEquipBuilder equipBuilder = new ProfileEquipBuilder(hayStack);
            equipBuilder.updateEquipAndPoints(config,
                    ModelLoader.INSTANCE.getModelForDomainName(equip.getDomainName()),
                    equip.getSiteRef(),
                    equip.getDisplayName(), true);

            ACBConfigHandler.Companion.setMinCfmSetpointMaxVals(hayStack, config);
            ACBConfigHandler.Companion.setAirflowCfmProportionalRange(hayStack, config);
        } else {
            Equip equip = profile.getEquip();
            VavProfileConfiguration config = (VavProfileConfiguration) profile.getDomainProfileConfiguration();
            config.enableCFMControl.setEnabled(value > 0);
            ProfileEquipBuilder equipBuilder = new ProfileEquipBuilder(hayStack);
            equipBuilder.updateEquipAndPoints(config,
                    ModelLoader.INSTANCE.getModelForDomainName(equip.getDomainName()),
                    equip.getSiteRef(),
                    equip.getDisplayName(), true);

            VavConfigHandler.Companion.setMinCfmSetpointMaxVals(hayStack, config);
            VavConfigHandler.Companion.setAirflowCfmProportionalRange(hayStack, config);
        }

        //writePointFromJson(configPoint, msgObject, hayStack);
    }

    /**
     * Keeping these custom logic outside of the framework for now.
     * Need revisit if this is a scenario that needs to be addressed across profiles.
     */
    public static void updateMinCoolingConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        double maxCfmValue = msgObject.get("val").getAsDouble();
        Double minCfmValue = CCUHsApi.getInstance().readDefaultVal("point and domainName == \"" + DomainName.minCFMCooling + "\" and equipRef == \""+equip.getId()+"\"");
        HDict entity = CCUHsApi.getInstance().readHDict("point and domainName == \"" + DomainName.minCFMCooling + "\" and equipRef == \""+equip.getId()+"\"");
        String maxValForMinCFM = String.valueOf(maxCfmValue);
        Point updatedPoint = new Point.Builder().setHDict(entity).setMaxVal(maxValForMinCFM).build();
        CCUHsApi.getInstance().updatePointWithoutUpdatingLastModifiedTime(updatedPoint, updatedPoint.getId());
        if (minCfmValue > maxCfmValue) {
            if (equip.getDomainName().contains("VAV")) {
                VavEquip vavEquip = new VavEquip(equip.getId());
                vavEquip.getMinCFMCooling().writePointValue(maxCfmValue);
            } else {
                VavAcbEquip AcbEquip = new VavAcbEquip(equip.getId());
                AcbEquip.getMinCFMCooling().writePointValue(maxCfmValue);
            }
        }
        writePointFromJson(configPoint, msgObject, hayStack);
    }

    public static void writeHisValue(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> entity = hayStack.readMapById(id);
        if(entity.containsKey("his")) {
            Double val = hayStack.readPointPriorityVal(id);
            hayStack.writeHisValById(id, val);
        }
    }

    public static void updateAirflowCFMProportionalRange(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        VavEquip vavEquip = new VavEquip(configPoint.getEquipRef());
        vavEquip.getVavAirflowCFMProportionalRange().writePointValue(1.5 * msgObject.get("val").getAsDouble());
    }

    public static void updateMinReheatingConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        double maxHeatingCfmValue = msgObject.get("val").getAsDouble();
        Double minHeatingCfmValue = CCUHsApi.getInstance().readDefaultVal("point and domainName == \"" + DomainName.minCFMReheating + "\" and equipRef == \""+equip.getId()+"\"");
        HDict entity = CCUHsApi.getInstance().readHDict("point and domainName == \"" + DomainName.minCFMReheating + "\" and equipRef == \""+equip.getId()+"\"");
        String minHeatingCFMValue = String.valueOf(maxHeatingCfmValue);
        Point updatedPoint = new Point.Builder().setHDict(entity).setMaxVal(minHeatingCFMValue).build();
        CCUHsApi.getInstance().updatePointWithoutUpdatingLastModifiedTime(updatedPoint, updatedPoint.getId());
        if (minHeatingCfmValue > maxHeatingCfmValue) {
            if (equip.getDomainName().contains("VAV")) {
                VavEquip vavEquip = new VavEquip(equip.getId());
                vavEquip.getMinCFMReheating().writePointValue(maxHeatingCfmValue);
            } else {
                VavAcbEquip AcbEquip = new VavAcbEquip(equip.getId());
                AcbEquip.getMinCFMReheating().writePointValue(maxHeatingCfmValue);
            }
        }
        writePointFromJson(configPoint, msgObject, hayStack);
    }

    private static void writePointFromJson(Point configPoint, JsonObject msgObject, CCUHsApi hayStack) {
        String checkValue = msgObject.get("val").getAsString();
        if(checkValue == null || checkValue.isEmpty()) {
            CcuLog.d(L.TAG_CCU_PUBNUB, "updateVAVCFM - Message is not handled");
            return;
        }
        String who = msgObject.get(HayStackConstants.WRITABLE_ARRAY_WHO).getAsString();
        int level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt();
        double val = msgObject.get(HayStackConstants.WRITABLE_ARRAY_VAL).getAsDouble();
        double durationDiff = MessageUtil.Companion.returnDurationDiff(msgObject);
        hayStack.writePointLocal(configPoint.getId(), level, who, val, durationDiff);
        CcuLog.d(L.TAG_CCU_PUBNUB, "VAV : writePointFromJson - level: " + level + " who: " + who + " val: " + val  + " durationDiff: " + durationDiff);
        writeHisValue(configPoint.getId(), hayStack);
    }
}

