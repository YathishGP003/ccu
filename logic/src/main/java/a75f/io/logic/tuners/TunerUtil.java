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
        if(tunerPoint != null && (tunerPoint.get("id" )!= null)) {
            ArrayList values = hayStack.readPoint(tunerPoint.get("id").toString());
            if (values != null && values.size() > 0) {
                for (int l = 1; l <= values.size(); l++) {
                    HashMap valMap = ((HashMap) values.get(l - 1));
                    if (valMap.get("val") != null) {
                        return Double.parseDouble(valMap.get("val").toString());
                    }
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

    public static void writeTunerValByQuery(String query,String equipRef, double dbVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and "+query +" and equipRef == \""+equipRef+"\"");

        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dbVal, 0);

    }
    
    public static double getCoolingDeadband(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and deadband and base and cooling and equipRef == \""+equipRef+"\"");
        
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
    
    public static void setCoolingDeadband(String equipRef, double dbVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and deadband and base and cooling and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dbVal, 0);
        
    }
    
    public static double getHeatingDeadband(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and deadband and base and heating and equipRef == \""+equipRef+"\"");
        
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
    
    public static void setHeatingDeadband(String equipRef, double dbVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and deadband and base and heating and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dbVal, 0);
        
    }
    
    public static double getProportionalGain(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and pgain and equipRef == \""+equipRef+"\"");
        
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
    
    public static void setProportionalGain(String equipRef, double pgVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and pgain and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", pgVal, 0);
        
    }
    
    public static double getIntegralGain(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and igain and equipRef == \""+equipRef+"\"");
        
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
    
    public static void setIntegralGain(String equipRef, double igVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and igain and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", igVal, 0);
        
    }
    
    public static double getProportionalSpread(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and pspread and equipRef == \""+equipRef+"\"");
        
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
    
    public static void setProportionalSpread(String equipRef, double psVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and pspread and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", psVal, 0);
        
    }
    
    public static double getIntegralTimeout(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and itimeout and equipRef == \""+equipRef+"\"");
        
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
    
    public static void setIntegralTimeout(String equipRef, double itVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and itimeout and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", itVal, 0);
        
    }
    public static double getMinCoolingDamperPos(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and config and min and damper and cooling and pos and equipRef == \""+equipRef+"\"");
        
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
    
    public static void setMinCoolingDamperPos(String equipRef, double dVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and config and min and damper and cooling and pos and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dVal, 0);
        
    }
    
    public static double getMaxCoolingDamperPos(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and config and max and damper and cooling and pos and equipRef == \""+equipRef+"\"");
        
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
    
    public static void setMaxCoolingDamperPos(String equipRef, double dVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and config and max and damper and cooling and pos and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dVal, 0);
        
    }
    
    public static double getMinHeatingDamperPos(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and config and min and damper and heating and pos and equipRef == \""+equipRef+"\"");
        
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
    
    public static void setMinHeatingDamperPos(String equipRef, double dVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and config and min and damper and heating and pos and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dVal, 0);
        
    }
    
    public static double getMaxHeatingDamperPos(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and config and max and damper and heating and pos and equipRef == \""+equipRef+"\"");
        
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
    
    public static void setMaxHeatingDamperPos(String equipRef, double dVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and config and max and damper and heating and pos and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dVal, 0);
        
    }
}
