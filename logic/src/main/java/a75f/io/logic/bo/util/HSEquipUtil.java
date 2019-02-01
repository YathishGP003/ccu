package a75f.io.logic.bo.util;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;

/**
 * Created by samjithsadasivan on 1/31/19.
 */

public class HSEquipUtil
{
    public static double getCurrentTemp(String equipRef)
    {
        return CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and equipRef == \""+equipRef+"\"");
    }
    public static void setCurrentTemp(String equipRef, double roomTemp)
    {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and current and equipRef == \""+equipRef+"\"", roomTemp);
    }
    
    public static double getDesiredTempCooling(String equipRef)
    {
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and cooling and sp and equipRef == \""+equipRef+"\"");
        if (point == null || point.size() == 0) {
            throw new IllegalArgumentException();
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(point.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
        
    }
    //Sets to the default level 17. Should use CCUHsApi.writePoint() to write to other levels
    public static void setDesiredTempCooling(String equipRef, double desiredTemp)
    {
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and cooling and sp and equipRef == \""+equipRef+"\"");
        if (point == null || point.size() == 0) {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(point.get("id").toString(), desiredTemp);
        CCUHsApi.getInstance().writeHisValById(point.get("id").toString(), desiredTemp);
    }
    
    public static double getDesiredTempHeating(String equipRef)
    {
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and heating and sp and equipRef == \""+equipRef+"\"");
        if (point == null || point.size() == 0) {
            throw new IllegalArgumentException();
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(point.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
    public static void setDesiredTempHeating(String equipRef, double desiredTemp)
    {
        HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and heating and sp and equipRef == \""+equipRef+"\"");
        if (point == null || point.size() == 0) {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(point.get("id").toString(), desiredTemp);
        CCUHsApi.getInstance().writeHisValById(point.get("id").toString(), desiredTemp);
    }
}
