package a75f.io.logic.tuners;

/**
 * Created by samjithsadasivan on 11/7/18.
 */

public class SystemTunerUtil
{
    /**
     *  Get the priority resolved analog1 value
     * @param name
     * @param minMax
     * @return
     */
    /*public static double getTuner(String name, String minMax) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and system and "+name+" and "+minMax);
    
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
    
    *//**
     *  Get analog1 val from a specific level
     * @param name
     * @param minMax
     * @return
     *//*
    public static double getTuner(String name, String minMax, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and system and "+name+" and "+minMax);
        
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(level-1));
            if (valMap.get("val") != null) {
                return Double.parseDouble(valMap.get("val").toString());
            }
        }
        return 0;
    }
    
    *//**
     *  Set analog val at a specific level
     * @param name
     * @param minMax
     * @param level
     * @param val
     *//*
    public static void setTuner(String name, String minMax, int level, double val) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and system and "+name+" and "+minMax);
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", val, 0);
        
    }
    
    public static double getDesiredCI() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and system and ci and desired");
        
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
    
    public static void setDesiredCI(int level, double val) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and system and ci and desired");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", val, 0);
        
    }*/
}
