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
