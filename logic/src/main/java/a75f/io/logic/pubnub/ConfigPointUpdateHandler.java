package a75f.io.logic.pubnub;

import android.content.DialogInterface;

import com.google.gson.JsonObject;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.SystemProfile;
import a75f.io.logic.tuners.TunerUtil;

class ConfigPointUpdateHandler {
    
    public static void updateConfigPoint(JsonObject msgObject) {
        /**
         * There could more handling required as part of reconfiguration here.
         * Startng with updating the conditioning-mode
         */
        updateConditioningMode();
    
    }
    
    /***
     * When cooling/heating is disabled remotely via reconfiguration, and the system cannot operate any more
     * in the selected conditioning mode, we should turn off the system.
     */
    public static void updateConditioningMode() {
        SystemMode systemMode = SystemMode.values()[(int) HSUtil.getSystemUserIntentVal("conditioning and mode")];
        if (systemMode == SystemMode.OFF) {
            return;
        }
        SystemProfile systemProfile = L.ccu().systemProfile;
        
        if ((systemMode == SystemMode.AUTO && (!systemProfile.isCoolingAvailable() || !systemProfile.isHeatingAvailable()))
            || (systemMode == SystemMode.COOLONLY && !systemProfile.isCoolingAvailable())
            || (systemMode == SystemMode.HEATONLY && !systemProfile.isHeatingAvailable())) {
            
            CcuLog.i(L.TAG_CCU_PUBNUB, "Reconfig disabling conditioning mode !");
            TunerUtil.writeSystemUserIntentVal("conditioning and mode", SystemMode.OFF.ordinal());
        }
    }
    
    public static void updateConfigAssociation() {
    
    }
}
