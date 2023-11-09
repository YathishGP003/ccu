package a75f.io.logic.tuners;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

import static a75f.io.api.haystack.HayStackConstants.DEFAULT_INIT_VAL_LEVEL;

/**
 * Created by samjithsadasivan on 1/16/19.
 */

public class TunerUtil
{
    public static double readTunerValByQuery(String query, CCUHsApi hayStack) {
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
    
    public static double readTunerValByQuery(String query) {
        return readTunerValByQuery(query, CCUHsApi.getInstance());
    }
    
    public static double readTunerValByQuery(String query, String equipRef, CCUHsApi hayStack) {
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
    
    public static double readTunerValByQuery(String query, String equipRef) {
        return readTunerValByQuery(query, equipRef, CCUHsApi.getInstance());
    }
    
    public static double readBuildingTunerValByQuery(String query) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap tunerPoint = hayStack.read("point and tuner and "+query+" and siteRef == \""+hayStack.getSiteIdRef()+"\"");
        if(tunerPoint != null && (tunerPoint.get("id" )!= null)) {
            ArrayList values = hayStack.readPoint(tunerPoint.get("id").toString());
            if (values != null && values.size() > 0) {
                for (int l = 16; l <= values.size(); l++) {
                    HashMap valMap = ((HashMap) values.get(l - 1));
                    if (valMap.get("val") != null) {
                        return Double.parseDouble(valMap.get("val").toString());
                    }
                }
            }
        }
        return BuildingTunerFallback.getDefaultTunerVal(query);
    }
    
    public static double readSystemUserIntentVal(String tags) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<Object, Object> userIntent = hayStack.readEntity("point and system and userIntent and "+tags);
        if (userIntent.isEmpty()) {
            CcuLog.e(L.TAG_CCU, "!!!! Value Not Read : User Intent does not exist for "+tags);
            return 0;
        }
        ArrayList values = hayStack.readPoint(userIntent.get("id").toString());
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
        HashMap<Object, Object> userIntent = hayStack.readEntity("point and system and userIntent and "+tags);

        if (userIntent.isEmpty()) {
            CcuLog.e(L.TAG_CCU, "!!!! Value Not set : User Intent does not exist for "+tags);
            return;
        }
        String id = userIntent.get("id").toString();
        hayStack.pointWriteForCcuUser(HRef.copy(id), TunerConstants.UI_DEFAULT_VAL_LEVEL, HNum.make(val), HNum.make(0,"ms"));
        hayStack.writeHisValById(id, val);
    }
    
    /**
     * Get building level cooling deadband.
     * @param equipRef ID - of Building tuner equip
     * @return
     */
    public static double getCoolingDeadband(String equipRef) {
    
        double standaloneDbVal = TunerUtil.readTunerValByQuery("point and tuner and deadband and standalone and " +
                                                               "cooling and not adr and not multiplier and equipRef == " +
                                                               "\"" + equipRef + "\"");
        double vavDbVal = TunerUtil.readTunerValByQuery("point and tuner and deadband and vav and " +
                                                        "cooling and not adr and not multiplier and equipRef == " +
                                                        "\"" + equipRef + "\"");
        double dabDbVal = TunerUtil.readTunerValByQuery("point and tuner and deadband and dab and " +
                                                        "cooling and not adr and not multiplier and equipRef == " +
                                                        "\"" + equipRef + "\"");
        return Math.max(standaloneDbVal, Math.max(vavDbVal, dabDbVal));
    }

    public static double getZoneCoolingDeadband(String roomRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<Equip> zoneEquips  = HSUtil.getEquips(roomRef);
        double maxDb = 0;
        boolean isDefault = true;
        for (Equip q : zoneEquips){
            HashMap cdb = hayStack.read("point and deadband and cooling and not adr and not multiplier and roomRef == \""+roomRef+"\"");

            if (!cdb.isEmpty()) {
                ArrayList values = hayStack.readPoint(cdb.get("id").toString());
                if (values != null && values.size() > 0) {
                    for (int l = 1; l <= values.size(); l++) {
                        HashMap valMap = ((HashMap) values.get(l - 1));
                        if (valMap.get("val") != null) {
                            String val = valMap.get("val").toString();
                            if (Integer.parseInt(valMap.get("level").toString()) < DEFAULT_INIT_VAL_LEVEL) {
                                if (maxDb < Double.parseDouble(val)) {
                                    maxDb = Double.parseDouble(val);
                                    isDefault = false;
                                }
                            } else if (isDefault) {
                                if (maxDb < Double.parseDouble(val)) {
                                    maxDb = Double.parseDouble(val);
                                }
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

        for (Equip q : zoneEquips) {
            HashMap hdb = hayStack.read("point and deadband and heating and not adr and not multiplier and roomref == \"" + roomRef + "\"");

            if (!hdb.isEmpty()) {
                ArrayList values = hayStack.readPoint(hdb.get("id").toString());
                if (values != null && values.size() > 0) {
                    for (int l = 1; l <= values.size(); l++) {
                        HashMap valMap = ((HashMap) values.get(l - 1));
                        if (valMap.get("val") != null) {
                            String val = valMap.get("val").toString();
                            if (Integer.parseInt(valMap.get("level").toString()) < DEFAULT_INIT_VAL_LEVEL) {
                                if (maxDb < Double.parseDouble(val)) {
                                    maxDb = Double.parseDouble(val);
                                }
                                isDefault = false;

                            } else if (isDefault) {
                                if (maxDb < Double.parseDouble(val)) {
                                    maxDb = Double.parseDouble(val);
                                }
                            }
                        }
                    }
                }
            }
        }
        return maxDb;
    }
    
    /**
     * Get building level heating deadband.
     * @param equipRef ID - of Building tuner equip
     * @return
     */
    public static double getHeatingDeadband(String equipRef) {
    
        double standaloneDbVal = TunerUtil.readTunerValByQuery("point and tuner and deadband and standalone and " +
                                                               "heating and not adr and not multiplier and equipRef == " +
                                                               "\"" + equipRef + "\"");
        double vavDbVal = TunerUtil.readTunerValByQuery("point and tuner and deadband and vav and " +
                                                        "heating and not adr and not multiplier and equipRef == " +
                                                        "\"" + equipRef + "\"");
        double dabDbVal = TunerUtil.readTunerValByQuery("point and tuner and deadband and dab and " +
                                                        "heating and not adr and not multiplier and equipRef == " +
                                                        "\"" + equipRef + "\"");
        return Math.max(standaloneDbVal, Math.max(vavDbVal, dabDbVal));
        
    }
    
    public static double getProportionalGain(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap pgain = hayStack.read("point and tuner and pgain and equipRef == \""+equipRef+"\"");
        if (!pgain.isEmpty()) {
            ArrayList values = hayStack.readPoint(pgain.get("id").toString());
            if (values != null && values.size() > 0) {
                for (int l = 1; l <= values.size(); l++) {
                    HashMap valMap = ((HashMap) values.get(l - 1));
                    if (valMap.get("val") != null) {
                        return Double.parseDouble(valMap.get("val").toString());
                    }
                }
            }
        }
        return BuildingTunerFallback.getDefaultTunerVal("pgain");
    }
    
    public static double getIntegralGain(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap integralGain = hayStack.read("point and tuner and igain and equipRef == \""+equipRef+"\"");
        
        if (!integralGain.isEmpty()) {
            ArrayList values = hayStack.readPoint(integralGain.get("id").toString());
            if (values != null && values.size() > 0) {
                for (int l = 1; l <= values.size(); l++) {
                    HashMap valMap = ((HashMap) values.get(l - 1));
                    if (valMap.get("val") != null) {
                        return Double.parseDouble(valMap.get("val").toString());
                    }
                }
            }
        }
        return BuildingTunerFallback.getDefaultTunerVal("igain");
    }
    
    public static double getProportionalSpread(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap proportionalSpread = hayStack.read("point and tuner and pspread and equipRef == \""+equipRef+"\"");
        
        if (!proportionalSpread.isEmpty()) {
            ArrayList values = hayStack.readPoint(proportionalSpread.get("id").toString());
            if (values != null && values.size() > 0) {
                for (int l = 1; l <= values.size(); l++) {
                    HashMap valMap = ((HashMap) values.get(l - 1));
                    if (valMap.get("val") != null) {
                        return Double.parseDouble(valMap.get("val").toString());
                    }
                }
            }
        }
        return BuildingTunerFallback.getDefaultTunerVal("pspread");
    }
    
    public static double getIntegralTimeout(String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap itimeout = hayStack.read("point and tuner and itimeout and equipRef == \""+equipRef+"\"");
        
        if (!itimeout.isEmpty()) {
            ArrayList values = hayStack.readPoint(itimeout.get("id").toString());
            if (values != null && values.size() > 0) {
                for (int l = 1; l <= values.size(); l++) {
                    HashMap valMap = ((HashMap) values.get(l - 1));
                    if (valMap.get("val") != null) {
                        return Double.parseDouble(valMap.get("val").toString());
                    }
                }
            }
        }
        return BuildingTunerFallback.getDefaultTunerVal("itimeout");
    }
    
    /**
     * Creates a hayStack query using point marker tags.
     */
    public static String getQueryString(Point tunerPoint) {
        ArrayList<String> markersFiltered = tunerPoint.getMarkers();
        HSUtil.removeGenericMarkerTags(markersFiltered);
        return HSUtil.getQueryFromMarkers(markersFiltered);
    }

    public static double getHysteresisPoint(String markers , String equipRef) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and tuner and hysteresis and "+markers+" and equipRef == \""+equipRef+"\"");

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

    public static double getTuner(String id) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> values = hayStack.readPoint(id);
        if (values != null && !values.isEmpty()) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap<Object,Object> valMap =  values.get(l - 1);
                if (valMap.containsKey("val")) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }

    /**
     * TODO - Optmize - commondata
     * @param systemPointId
     * @param domainName
     */
    public static void copyDefaultBuildingTunerVal(String systemPointId, String domainName, CCUHsApi hayStack) {
        HashMap<Object, Object> buildingPoint = hayStack.readDefaultPointByDomainName(domainName);
        if (buildingPoint.isEmpty()) {
            CcuLog.e(L.TAG_CCU_TUNER, "!! Default point does not exist for "+domainName);
            //TODO- write default val?
            return;
        }
        ArrayList<HashMap> buildingPointArray = hayStack.readPoint(buildingPoint.get(Tags.ID).toString());
        for (HashMap valMap : buildingPointArray) {
            if (valMap.get("val") != null) {
                hayStack.pointWrite(HRef.copy(systemPointId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        CcuLog.e(L.TAG_CCU_TUNER, "Copy default value for "+domainName+" "+buildingPointArray);
        hayStack.writeHisValById(systemPointId, HSUtil.getPriorityVal(systemPointId));
    }

}
