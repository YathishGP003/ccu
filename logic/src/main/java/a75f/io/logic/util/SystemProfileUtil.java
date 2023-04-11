package a75f.io.logic.util;

import java.util.HashMap;
import a75f.io.api.haystack.CCUHsApi;
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
