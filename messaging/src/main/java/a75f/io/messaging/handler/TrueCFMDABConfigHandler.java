package a75f.io.messaging.handler;

import com.google.gson.JsonObject;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.domain.logic.ProfileEquipBuilder;
import a75f.io.domain.util.ModelLoader;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.dab.DabProfile;
import a75f.io.logic.bo.building.dab.DabProfileConfiguration;
import a75f.io.logic.bo.building.truecfm.TrueCFMUtil;

public class TrueCFMDABConfigHandler {
    public static void updateDABConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        double value = msgObject.get("val").getAsDouble();
        if(value == CCUHsApi.getInstance().readDefaultValById(configPoint.getId())) {
            CcuLog.d(L.TAG_CCU_PUBNUB, "updateDABConfigPoint - Message is not handled");
            return;
        }
        short address = Short.parseShort(configPoint.getGroup());
        DabProfile profile = (DabProfile) L.getProfile(address);
        Equip equip = profile.getEquip();
        DabProfileConfiguration config = (DabProfileConfiguration) profile.getDomainProfileConfiguration();
        config.enableCFMControl.setEnabled(value > 0);
        ProfileEquipBuilder equipBuilder = new ProfileEquipBuilder(hayStack);
        equipBuilder.updateEquipAndPoints(config,
                ModelLoader.INSTANCE.getModelForDomainName(equip.getDomainName()),
                equip.getSiteRef(),
                equip.getDisplayName(), true);
    }

    public static void updatePointVal(Point localPoint, JsonObject msgObject){
        CcuLog.d(L.TAG_CCU_MESSAGING, "updatePointVal DAB: " + msgObject);
        String val = msgObject.get("val").getAsString();
        if(!val.isEmpty()) {
            int value = (int) TrueCFMUtil.getDamperSizeFromEnum((Integer.parseInt(val)));
            msgObject.addProperty("val", value);
        }
        writePointFromJson(localPoint, msgObject, CCUHsApi.getInstance());
    }

    private static void writePointFromJson(Point configPoint, JsonObject msgObject, CCUHsApi hayStack) {
        CcuLog.d(L.TAG_CCU_MESSAGING, "writePointFromJson DAB: " + msgObject);
        String val_String = msgObject.get(HayStackConstants.WRITABLE_ARRAY_VAL).getAsString();
        int level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt();
        String id = configPoint.getId();
        if (val_String.isEmpty()) {
            hayStack.clearPointArrayLevel(id, level, false);
            hayStack.writeHisValById(id, HSUtil.getPriorityVal(id));
            return;
        }
        String who = msgObject.get(HayStackConstants.WRITABLE_ARRAY_WHO).getAsString();
        double val = msgObject.get(HayStackConstants.WRITABLE_ARRAY_VAL).getAsDouble();
        int duration = msgObject.get(HayStackConstants.WRITABLE_ARRAY_DURATION) != null ? msgObject.get(
                HayStackConstants.WRITABLE_ARRAY_DURATION).getAsInt() : 0;
        hayStack.pointWrite(HRef.copy(id), level, who, HNum.make(val), HNum.make(duration));
    }
}
