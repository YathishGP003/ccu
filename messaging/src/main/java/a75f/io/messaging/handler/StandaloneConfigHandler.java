package a75f.io.messaging.handler;

import static a75f.io.api.haystack.HayStackConstants.WRITABLE_ARRAY_VAL;

import static a75f.io.api.haystack.HayStackConstants.WRITABLE_ARRAY_VAL;

import com.google.gson.JsonObject;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ss2pfcu.FanCoilUnitUtil;
import a75f.io.logic.bo.building.sscpu.ConventionalPackageUnitUtil;
import a75f.io.logic.bo.building.sshpu.HeatPumpPackageUnitUtil;
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage;
import a75f.io.logic.bo.util.DesiredTempDisplayMode;

public class StandaloneConfigHandler {
    
    public static void updateConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateStandaloneConfigPoint " + msgObject.toString());
    
        if (configPoint.getMarkers().contains(Tags.FCU)) {
            FanCoilUnitUtil.updateFCUProfile(configPoint, msgObject, hayStack);
        } else if (configPoint.getMarkers().contains(Tags.CPU)) {
            //CPU config points do not seem to have 'cpu' tag. Hence checking the equip type to identify profile.
            ConventionalPackageUnitUtil.updateCPUProfile(configPoint, msgObject, hayStack);
        } else if (HSUtil.isHPUEquip(configPoint.getId(), hayStack)) {
            //HPU config points do not seem to have 'hpu' tag. Hence checking the equip type to identify profile.
            HeatPumpPackageUnitUtil.updateHPUProfile(configPoint, msgObject, hayStack);
        }


        if (configPoint.getMarkers().contains(Tags.USERINTENT)
                && configPoint.getMarkers().contains(Tags.FAN)
                && configPoint.getMarkers().contains(Tags.MODE) ){
            if (msgObject.get("val").getAsString().isEmpty()) return;
            int configVal = (int) msgObject.get(WRITABLE_ARRAY_VAL).getAsDouble();
            FanModeCacheStorage cache = FanModeCacheStorage.Companion.getSmartStatFanModeCache();
            if ((configVal != 0) && (configVal % 3 == 0)) //Save only Fan occupied period mode alone, else no need.
                cache.saveFanModeInCache(configPoint.getEquipRef(), configVal);
            else
                cache.removeFanModeFromCache(configPoint.getEquipRef());
        }
        if (configPoint.getMarkers().contains(Tags.USERINTENT) && configPoint.getMarkers().contains(Tags.CONDITIONING)) {
            DesiredTempDisplayMode.setModeTypeOnUserIntentChange(configPoint.getRoomRef(), CCUHsApi.getInstance());
        }
        
    }
    
}
