package a75f.io.logic.tuners;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;

import static a75f.io.logic.tuners.TunerDefalutVals.VAV_DEFAULT_VAL_LEVEL;

/**
 * Created by samjithsadasivan on 10/8/18.
 */

public class VavTunerUtil
{
    public static double getCoolingDeadband() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and deadband and cooling");
    
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(VAV_DEFAULT_VAL_LEVEL-1));
            return Double.parseDouble(valMap.get("val").toString());
        }
        return 0;
    }
    
    public static void setCoolingDeadband(double dbVal) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and deadband and cooling");
    
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, TunerDefalutVals.VAV_DEFAULT_VAL_LEVEL, "ccu", dbVal, 0);
    
    }
    
    public static double getHeatingDeadband() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and deadband and heating");
    
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(VAV_DEFAULT_VAL_LEVEL-1));
            return Double.parseDouble(valMap.get("val").toString());
        }
        return 0;
    }
    
    public static void setHeatingDeadband(double dbVal) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and deadband and heating");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, TunerDefalutVals.VAV_DEFAULT_VAL_LEVEL, "ccu", dbVal, 0);
        
    }
    
    public static double getProportionalGain() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and pgain");
        
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(VAV_DEFAULT_VAL_LEVEL-1));
            return Double.parseDouble(valMap.get("val").toString());
        }
        return 0;
    }
    
    public static void setProportionalGain(double dbVal) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and pgain");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, TunerDefalutVals.VAV_DEFAULT_VAL_LEVEL, "ccu", dbVal, 0);
        
    }
    
    public static double getIntegralGain() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and igain");
        
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(VAV_DEFAULT_VAL_LEVEL-1));
            return Double.parseDouble(valMap.get("val").toString());
        }
        return 0;
    }
    
    public static void setIntegralGain(double dbVal) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and igain");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, TunerDefalutVals.VAV_DEFAULT_VAL_LEVEL, "ccu", dbVal, 0);
        
    }
    
    public static double getProportionalSpread() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and pspread");
        
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(VAV_DEFAULT_VAL_LEVEL-1));
            return Double.parseDouble(valMap.get("val").toString());
        }
        return 0;
    }
    
    public static void setProportionalSpread(double dbVal) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and pspread");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, TunerDefalutVals.VAV_DEFAULT_VAL_LEVEL, "ccu", dbVal, 0);
        
    }
    
    public static double getIntegralTimeout() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and itimeout");
        
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(VAV_DEFAULT_VAL_LEVEL-1));
            return Double.parseDouble(valMap.get("val").toString());
        }
        return 0;
    }
    
    public static void setIntegralTimeout(double dbVal) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and itimeout");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, TunerDefalutVals.VAV_DEFAULT_VAL_LEVEL, "ccu", dbVal, 0);
        
    }
    
    public static void dump() {
        System.out.println("VAVCoolingDeadband : "+getCoolingDeadband());
        System.out.println("VAVHeatingDeadband : "+getHeatingDeadband());
        System.out.println("VAVProportionalGain : "+getProportionalGain());
        System.out.println("VAVIntegralGain : "+getIntegralGain());
        System.out.println("VAVProportionalSpread : "+getProportionalSpread());
        System.out.println("VAVIntegralTimeout : "+getIntegralTimeout());
    }
}
