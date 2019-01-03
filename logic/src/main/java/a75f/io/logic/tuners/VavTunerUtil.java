package a75f.io.logic.tuners;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;

/**
 * Created by samjithsadasivan on 10/8/18.
 */

public class VavTunerUtil
{
    public static double getCoolingDeadband(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and deadband and cooling and equipRef == \""+equipRef+"\"");
    
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
    
    public static void setCoolingDeadband(String equipRef, double dbVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and deadband and cooling and equipRef == \""+equipRef+"\"");
    
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dbVal, 0);
    
    }
    
    public static double getHeatingDeadband(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and deadband and heating and equipRef == \""+equipRef+"\"");
    
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
    
    public static void setHeatingDeadband(String equipRef, double dbVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and deadband and heating and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dbVal, 0);
        
    }
    
    public static double getProportionalGain(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and pgain and equipRef == \""+equipRef+"\"");
        
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
    
    public static void setProportionalGain(String equipRef, double pgVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and pgain and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", pgVal, 0);
        
    }
    
    public static double getIntegralGain(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and igain and equipRef == \""+equipRef+"\"");
        
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
    
    public static void setIntegralGain(String equipRef, double igVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and igain and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", igVal, 0);
        
    }
    
    public static double getProportionalSpread(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and pspread and equipRef == \""+equipRef+"\"");
        
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
    
    public static void setProportionalSpread(String equipRef, double psVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and pspread and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", psVal, 0);
        
    }
    
    public static double getIntegralTimeout(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and itimeout and equipRef == \""+equipRef+"\"");
        
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
    
    public static void setIntegralTimeout(String equipRef, double itVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and itimeout and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", itVal, 0);
        
    }
    public static double getMinCoolingDamperPos(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and min and damper and cooling and pos and equipRef == \""+equipRef+"\"");
        
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
        HashMap cdb = hayStack.read("point and tuner and vav and min and damper and cooling and pos and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dVal, 0);
        
    }
    
    public static double getMaxCoolingDamperPos(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and max and damper and cooling and pos and equipRef == \""+equipRef+"\"");
        
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
        HashMap cdb = hayStack.read("point and tuner and vav and max and damper and cooling and pos and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dVal, 0);
        
    }
    
    public static double getMinHeatingDamperPos(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and min and damper and heating and pos and equipRef == \""+equipRef+"\"");
        
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
        HashMap cdb = hayStack.read("point and tuner and vav and min and damper and heating and pos and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dVal, 0);
        
    }
    
    public static double getMaxHeatingDamperPos(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and vav and max and damper and heating and pos and equipRef == \""+equipRef+"\"");
        
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
        HashMap cdb = hayStack.read("point and tuner and vav and max and damper and heating and pos and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dVal, 0);
        
    }
    
    public static void dump(String equipRef) {
        System.out.println("VAVCoolingDeadband : "+getCoolingDeadband(equipRef));
        System.out.println("VAVHeatingDeadband : "+getHeatingDeadband(equipRef));
        System.out.println("VAVProportionalGain : "+getProportionalGain(equipRef));
        System.out.println("VAVIntegralGain : "+getIntegralGain(equipRef));
        System.out.println("VAVProportionalSpread : "+getProportionalSpread(equipRef));
        System.out.println("VAVIntegralTimeout : "+getIntegralTimeout(equipRef));
        System.out.println("VAVMinCoolingDamperPos : "+getMinCoolingDamperPos(equipRef));
        System.out.println("VAVMaxCoolingDamperPos : "+getMaxCoolingDamperPos(equipRef));
        System.out.println("VAVMinHeatingDamperPos : "+getMinHeatingDamperPos(equipRef));
        System.out.println("VAVMinHeatingDamperPos : "+getMaxHeatingDamperPos(equipRef));
    }
}
