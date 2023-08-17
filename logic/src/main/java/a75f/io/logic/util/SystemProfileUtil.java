package a75f.io.logic.util;

import java.util.HashMap;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.building.system.dab.DabAdvancedHybridRtu;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtuWithVfd;
import a75f.io.logic.bo.building.system.vav.VavAdvancedHybridRtu;
import a75f.io.logic.bo.building.system.vav.VavBacnetRtu;
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu;
import a75f.io.logic.bo.building.system.vav.VavIERtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtuWithVfd;
import a75f.io.logic.limits.SchedulabeLimits;
import a75f.io.logic.tuners.BuildingTuners;

public class SystemProfileUtil {

    public static void createSystemProfile() {
        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        boolean isDefaultSystem = false;
        if (equip != null && equip.size() > 0) {
            BuildingTuners.getInstance().addBuildingTunerEquip();
            SchedulabeLimits.Companion.addSchedulableLimits(true,null,null);
            Equip eq = new Equip.Builder().setHashMap(equip).build();
            CcuLog.d(L.TAG_CCU, "Load SystemEquip " + eq.getDisplayName() + " System profile " + eq.getProfile());
            switch (ProfileType.valueOf(eq.getProfile())) {
                case SYSTEM_VAV_ANALOG_RTU:
                    L.ccu().systemProfile = new VavFullyModulatingRtu();
                    break;
                case SYSTEM_VAV_STAGED_RTU:
                    L.ccu().systemProfile = new VavStagedRtu();
                    break;
                case SYSTEM_VAV_STAGED_VFD_RTU:
                    L.ccu().systemProfile = new VavStagedRtuWithVfd();
                    break;
                case SYSTEM_VAV_HYBRID_RTU:
                    L.ccu().systemProfile = new VavAdvancedHybridRtu();
                    break;
                case SYSTEM_VAV_IE_RTU:
                    L.ccu().systemProfile = new VavIERtu();
                    break;
                case SYSTEM_VAV_BACNET_RTU:
                    L.ccu().systemProfile = new VavBacnetRtu();
                    break;
                case SYSTEM_DAB_ANALOG_RTU:
                    L.ccu().systemProfile = new DabFullyModulatingRtu();
                    break;
                case SYSTEM_DAB_STAGED_RTU:
                    L.ccu().systemProfile = new DabStagedRtu();
                    break;
                case SYSTEM_DAB_STAGED_VFD_RTU:
                    L.ccu().systemProfile = new DabStagedRtuWithVfd();
                    break;
                case SYSTEM_DAB_HYBRID_RTU:
                    L.ccu().systemProfile = new DabAdvancedHybridRtu();
                    break;
                default:
                    L.ccu().systemProfile = new DefaultSystem();
                    isDefaultSystem = true;
                    break;
            }
        }
        else {
            CcuLog.d(L.TAG_CCU, "System Equip does not exist.Create Dafault System Profile");
            L.ccu().systemProfile = new DefaultSystem();
            isDefaultSystem = true;

        }
        if(!isDefaultSystem) {
            L.ccu().systemProfile.addSystemEquip();
        }
    }
    public static String isHumidifierOn(){
        HashMap<Object, Object> humidifier = CCUHsApi.getInstance().readEntity("humidifier and cmd");
        double isHumidifierOn = CCUHsApi.getInstance().readHisValByQuery("humidifier and cmd");
        double curHumidifierType = CCUHsApi.getInstance().readDefaultVal("point and system and config and relay7 and enabled");

        if(humidifier.isEmpty() || curHumidifierType == 0.0){
            return "";
        }else {
            if(isHumidifierOn > 0){
                return " | Humidifier ON ";
            }else {
                return " | Humidifier OFF ";
            }
        }
    }

    public static String isDeHumidifierOn(){
        HashMap<Object, Object> deHumidifier = CCUHsApi.getInstance().readEntity("dehumidifier and cmd");
        double isDeHumidifierOn = CCUHsApi.getInstance().readHisValByQuery("dehumidifier and cmd");
        double curHumidifierType = CCUHsApi.getInstance().readDefaultVal("point and system and config and relay7 and enabled");

        if(deHumidifier.isEmpty() || curHumidifierType == 0.0){
            return "";
        }else {
            if(isDeHumidifierOn > 0){
                return " | Dehumidifier ON ";
            }else {
                return " | Dehumidifier OFF ";
            }
        }
    }
}
