package a75f.io.logic.tuners;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;

public class StandaloneTunerUtil {
    public static double getStandaloneCoolingDeadband(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and deadband and cooling and equipRef == \""+equipRef+"\"");

        if((cdb != null) && (cdb.get("id") != null) ) {
            ArrayList values = hayStack.readPoint(cdb.get("id").toString());
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

    public static void setStandaloneCoolingDeadband(String equipRef, double dbVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and deadband and cooling and equipRef == \""+equipRef+"\"");

        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dbVal, 0);

    }

    public static double getStandaloneHeatingDeadband(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and deadband and heating and equipRef == \""+equipRef+"\"");

        if((cdb != null) && (cdb.get("id") != null) ) {

            ArrayList values = hayStack.readPoint(cdb.get("id").toString());
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


    public static double getStandaloneStage1Hysteresis(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and hysteresis and stage1 and equipRef == \""+equipRef+"\"");

        if((cdb != null) && (cdb.get("id") != null) ) {

            ArrayList values = hayStack.readPoint(cdb.get("id").toString());
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
    public static void setStandaloneHeatingDeadband(String equipRef, double dbVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and deadband and heating and equipRef == \""+equipRef+"\"");

        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", dbVal, 0);

    }
    public static double readTunerValByQuery(String query) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap tunerPoint = hayStack.read("point and tuner and "+query);
        if((tunerPoint != null) && (tunerPoint.get("id") != null)) {
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
    public static double readTunerValByQuery(String query, String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap tunerPoint = hayStack.read("point and tuner and "+query+" and equipRef == \""+equipRef+"\"");
        if((tunerPoint != null) && (tunerPoint.get("id") != null)) {
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

    public static double readStandaloneUserIntentVal(String tags) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and standalone and userIntent and "+tags);
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

    public static void writeStandaloneUserIntentVal(String tags, double val) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and standalone and userIntent and "+tags);

        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, TunerConstants.UI_DEFAULT_VAL_LEVEL, "ccu", val, 0);
        hayStack.writeHisValById(id, val);
    }
}
