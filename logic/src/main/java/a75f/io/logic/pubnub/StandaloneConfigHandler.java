package a75f.io.logic.pubnub;

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

public class StandaloneConfigHandler {
    
    public static void updateConfigPoint(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateStandaloneConfigPoint " + msgObject.toString());
        
        if (configPoint.getMarkers().contains(Tags.FCU)) {
            FanCoilUnitUtil.updateFCUProfile(configPoint, msgObject, hayStack);
        } else if (HSUtil.isCPUEquip(configPoint.getId(), hayStack)) {
            //CPU config points do not seem to have 'cpu' tag. Hence checking the equip type to identify profile.
            ConventionalPackageUnitUtil.updateCPUProfile(configPoint, msgObject, hayStack);
        } else if (HSUtil.isHPUEquip(configPoint.getId(), hayStack)) {
            //HPU config points do not seem to have 'cpu' tag. Hence checking the equip type to identify profile.
            HeatPumpPackageUnitUtil.updateHPUProfile(configPoint, msgObject, hayStack);
        }
        
    }
    
   
}
