package a75f.io.logic.tuners;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

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
        HashMap tunerPoint = hayStack.read("point and tuner and "+query);
        ArrayList values = hayStack.readPoint(tunerPoint.get("id").toString());
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
    
    public static double readTunerValByQuery(String query, String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap tunerPoint = hayStack.read("point and tuner and "+query+" and equipRef == \""+equipRef+"\"");
        ArrayList values = hayStack.readPoint(tunerPoint.get("id").toString());
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
        HashMap tunerPoint = hayStack.read("point and tuner and "+query);
        ArrayList values = hayStack.readPoint(tunerPoint.get("id").toString());
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
    
    public static double readSystemUserIntentVal(String tags) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and userIntent and "+tags);
        if (cdb == null || cdb.size() == 0) {
            return 0;
        }
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
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
    
    public static void writeSystemUserIntentVal(String tags, double val) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and userIntent and "+tags);
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.pointWrite(HRef.copy(id), TunerConstants.UI_DEFAULT_VAL_LEVEL, "ccu", HNum.make(val), HNum.make(0,"ms"));
        hayStack.writeHisValById(id, val);
    }
}
