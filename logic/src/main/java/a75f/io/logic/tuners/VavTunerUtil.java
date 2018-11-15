package a75f.io.logic.tuners;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;

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
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
            //HashMap valMap = ((HashMap) values.get(VAV_DEFAULT_VAL_LEVEL-1));
            //return Double.parseDouble(valMap.get("val").toString());
        }
        return 0;
    }
    
    public static void setCoolingDeadband(double dbVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and deadband and cooling");
    
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dbVal, 0);
    
    }
    
    public static double getHeatingDeadband() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and deadband and heating");
    
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
            //HashMap valMap = ((HashMap) values.get(VAV_DEFAULT_VAL_LEVEL-1));
            //return Double.parseDouble(valMap.get("val").toString());
        }
        return 0;
    }
    
    public static void setHeatingDeadband(double dbVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and deadband and heating");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dbVal, 0);
        
    }
    
    public static double getProportionalGain() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and pgain");
        
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
            //HashMap valMap = ((HashMap) values.get(VAV_DEFAULT_VAL_LEVEL-1));
            //return Double.parseDouble(valMap.get("val").toString());
        }
        return 0;
    }
    
    public static void setProportionalGain(double pgVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and pgain");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", pgVal, 0);
        
    }
    
    public static double getIntegralGain() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and igain");
        
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
            //HashMap valMap = ((HashMap) values.get(VAV_DEFAULT_VAL_LEVEL-1));
            //return Double.parseDouble(valMap.get("val").toString());
        }
        return 0;
    }
    
    public static void setIntegralGain(double igVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and igain");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", igVal, 0);
        
    }
    
    public static double getProportionalSpread() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and pspread");
        
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
            //HashMap valMap = ((HashMap) values.get(VAV_DEFAULT_VAL_LEVEL-1));
            //return Double.parseDouble(valMap.get("val").toString());
        }
        return 0;
    }
    
    public static void setProportionalSpread(double psVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and pspread");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", psVal, 0);
        
    }
    
    public static double getIntegralTimeout() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and itimeout");
        
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
            //HashMap valMap = ((HashMap) values.get(VAV_DEFAULT_VAL_LEVEL-1));
            //return Double.parseDouble(valMap.get("val").toString());
        }
        return 0;
    }
    
    public static void setIntegralTimeout(double itVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and itimeout");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", itVal, 0);
        
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
