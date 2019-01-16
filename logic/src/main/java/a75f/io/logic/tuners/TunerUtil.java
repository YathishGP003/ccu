package a75f.io.logic.tuners;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;

/**
 * Created by samjithsadasivan on 1/16/19.
 */

public class TunerUtil
{
    public static double readTunerValByQuery(String query) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap valveStartDamperPoint = hayStack.read("point and tuner and "+query);
        ArrayList values = hayStack.readPoint(valveStartDamperPoint.get("id").toString());
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
    
    public static String readTunerStrByQuery(String query) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap valveStartDamperPoint = hayStack.read("point and tuner and "+query);
        ArrayList values = hayStack.readPoint(valveStartDamperPoint.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return valMap.get("val").toString();
                }
            }
        }
        return "";
    }
}
