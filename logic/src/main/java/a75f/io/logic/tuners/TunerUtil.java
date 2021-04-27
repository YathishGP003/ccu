package a75f.io.logic.tuners;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.logic.L;

import static a75f.io.api.haystack.HayStackConstants.DEFAULT_INIT_VAL_LEVEL;

/**
 * Created by samjithsadasivan on 1/16/19.
 */

public class TunerUtil
{
    public static double readTunerValByQuery(String query) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap tunerPoint = hayStack.read("point and tuner and "+query);
        if (!tunerPoint.isEmpty()) {
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
        return BuildingTunerFallback.getDefaultTunerVal(query);
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
        return BuildingTunerFallback.getDefaultTunerVal(query);
    }
    public static double readBuildingTunerValByQuery(String query) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap tunerPoint = hayStack.read("point and tuner and "+query+" and siteRef == \""+hayStack.getSiteIdRef()+"\"");
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
        return BuildingTunerFallback.getDefaultTunerVal(query);
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
        hayStack.pointWriteForCcuUser(HRef.copy(id), TunerConstants.UI_DEFAULT_VAL_LEVEL, HNum.make(val), HNum.make(0,"ms"));
        hayStack.writeHisValById(id, val);
    }

    public static void writeTunerValByQuery(String query,String equipRef, double dbVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and "+query +" and equipRef == \""+equipRef+"\"");

        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePointForCcuUser(id, level, dbVal, 0);

    }
    
    public static double getCoolingDeadband(String equipRef) {
        String systemProfile = "";
        if (L.ccu().systemProfile != null) {
            if (L.ccu().systemProfile.getProfileName().contains("VAV")) {
                systemProfile = "and vav";
            } else if (L.ccu().systemProfile.getProfileName().contains("DAB")) {
                systemProfile = "and dab";
            } else if (L.ccu().systemProfile.getProfileName().contains("Default")) {
                systemProfile = "and standalone";
            }
        } else {
            systemProfile = "";
        }
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and deadband "+systemProfile+" and cooling and not adr and not multiplier and equipRef == \""+equipRef+"\"");;

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

    public static double getZoneCoolingDeadband(String roomRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<Equip> zoneEquips  = HSUtil.getEquips(roomRef);
        double maxDb = 0;
        boolean isDefault = true;
        for (Equip q : zoneEquips){
            HashMap cdb = hayStack.read("point and tuner and deadband and cooling and not adr and not multiplier and equipRef == \""+q.getId()+"\"");

            ArrayList values = hayStack.readPoint(cdb.get("id").toString());
            if (values != null && values.size() > 0)
            {
                for (int l = 1; l <= values.size() ; l++ ) {
                    HashMap valMap = ((HashMap) values.get(l-1));
                    if (valMap.get("val") != null) {
                        String val = valMap.get("val").toString();
                        if (Integer.parseInt(valMap.get("level").toString()) < DEFAULT_INIT_VAL_LEVEL) {
                            if (maxDb < Double.parseDouble(val)) {
                                maxDb = Double.parseDouble(val);
                                isDefault = false;
                            }
                        } else if (isDefault){
                            if (maxDb < Double.parseDouble(val)) {
                                maxDb = Double.parseDouble(val);
                            }
                        }
                    }
                }
            }
        }

        return maxDb;
    }

    public static double getZoneHeatingDeadband(String roomRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<Equip> zoneEquips  = HSUtil.getEquips(roomRef);
        double maxDb = 0;
        boolean isDefault = true;
        for (Equip q : zoneEquips){
            HashMap hdb = hayStack.read("point and tuner and deadband and heating and not adr and not multiplier and equipRef == \""+q.getId()+"\"");

            ArrayList values = hayStack.readPoint(hdb.get("id").toString());
            if (values != null && values.size() > 0)
            {
                for (int l = 1; l <= values.size() ; l++ ) {
                    HashMap valMap = ((HashMap) values.get(l-1));
                    if (valMap.get("val") != null) {
                        String val = valMap.get("val").toString();
                        if (Integer.parseInt(valMap.get("level").toString()) < DEFAULT_INIT_VAL_LEVEL){
                            if (maxDb < Double.parseDouble(val)){
                                maxDb = Double.parseDouble(val);
                            }
                            isDefault = false;

                        } else if (isDefault){
                            if (maxDb < Double.parseDouble(val)) {
                                maxDb = Double.parseDouble(val);
                            }
                        }
                    }
                }
            }
        }
        
        return maxDb;
    }
    
    public static void setCoolingDeadband(String equipRef, double dbVal, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and deadband and base and cooling and equipRef == \""+equipRef+"\"");
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePointForCcuUser(id, level, dbVal, 0);
        
    }
    
    public static double getHeatingDeadband(String equipRef) {
        String systemProfile = "";
        if (L.ccu().systemProfile != null) {
            if (L.ccu().systemProfile.getProfileName().contains("VAV")) {
                systemProfile = "and vav";
            } else if (L.ccu().systemProfile.getProfileName().contains("DAB")) {
                systemProfile = "and dab";
            } else if (L.ccu().systemProfile.getProfileName().contains("Default")) {
                systemProfile = "and standalone";
            }
        } else {
            systemProfile = "";
        }
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap hdb = hayStack.read("point and tuner and deadband "+systemProfile+" and heating and not adr and not multiplier and equipRef == \""+equipRef+"\"");

        ArrayList values = hayStack.readPoint(hdb.get("id").toString());
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
        hayStack.writePointForCcuUser(id, level, dbVal, 0);
        
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
        hayStack.writePointForCcuUser(id, level, pgVal, 0);
        
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
        hayStack.writePointForCcuUser(id, level, igVal, 0);
        
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
        hayStack.writePointForCcuUser(id, level, psVal, 0);
        
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
    
    /**
     * Creates a hayStack query using point marker tags.
     */
    public static String getQueryString(Point tunerPoint) {
        ArrayList<String> markersFiltered = tunerPoint.getMarkers();
        HSUtil.removeGenericMarkerTags(markersFiltered);
        return HSUtil.getQueryFromMarkers(markersFiltered);
    }
}
