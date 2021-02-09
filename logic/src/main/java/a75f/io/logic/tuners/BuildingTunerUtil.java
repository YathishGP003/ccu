package a75f.io.logic.tuners;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

import static a75f.io.logic.tuners.TunerConstants.DEFAULT_VAL_LEVEL;
import static a75f.io.logic.tuners.TunerConstants.TUNER_BUILDING_VAL_LEVEL;
import static a75f.io.logic.tuners.TunerConstants.TUNER_SYSTEM_VAL_LEVEL;
import static a75f.io.logic.tuners.TunerConstants.TUNER_ZONE_VAL_LEVEL;

class BuildingTunerUtil {
    
    public static void updateTunerLevels(String tunerPointId, String zoneRef, CCUHsApi hayStack) {
    
        Point tunerPoint = new Point.Builder()
                               .setHashMap(hayStack.readMapById(tunerPointId))
                               .build();
        String queryString = HSUtil.getHQueryFromMarkers(tunerPoint.getMarkers());
        
        //If all the level are successfully copied from a zone tuner, no need to go futher.
        if (copyFromZoneTuner(tunerPoint.getId(), zoneRef, queryString, hayStack)) {
            return;
        }
    
        if (copyFromSystemTuner(tunerPoint.getId(), queryString, hayStack)) {
            return;
        }
    
        if (copyFromBuildingTuner(tunerPoint.getId(), queryString, hayStack)) {
            return;
        }
        CcuLog.e(L.TAG_CCU_TUNER, " Tuner initialization is not complete : "+tunerPoint.getDisplayName());
    }
    
    private static boolean copyFromZoneTuner(String dstPointId, String zoneRef, String queryString, CCUHsApi hayStack) {
    
        String equipQuery = queryString+" and zoneRef == \""+zoneRef+"\"";
        HashMap zoneTunerPoint = hayStack.read(equipQuery);
        
        if (zoneTunerPoint.isEmpty()) {
            return false;
        }
        
        ArrayList<HashMap> zoneTunerPointArray = hayStack.readPoint(zoneTunerPoint.get("id").toString());
        
        if (copyTunerLevel(dstPointId, zoneTunerPointArray, TUNER_ZONE_VAL_LEVEL, hayStack)
            && copyTunerLevel(dstPointId, zoneTunerPointArray, TUNER_SYSTEM_VAL_LEVEL, hayStack)
            && copyTunerLevel(dstPointId, zoneTunerPointArray, TUNER_BUILDING_VAL_LEVEL, hayStack)
            && copyTunerLevel(dstPointId, zoneTunerPointArray, DEFAULT_VAL_LEVEL, hayStack))  {
            return true;
        }
        
        return false;
    }
    
    private static boolean copyFromSystemTuner(String dstPointId, String queryString, CCUHsApi hayStack) {
    
        HashMap systemTunerPoint = hayStack.read(queryString);
        if (systemTunerPoint.isEmpty()) {
            return false;
        }
        
        ArrayList<HashMap> systemTunerPointArray = hayStack.readPoint(systemTunerPoint.get("id").toString());
    
        if (copyTunerLevel(dstPointId, systemTunerPointArray, TUNER_SYSTEM_VAL_LEVEL, hayStack)
            && copyTunerLevel(dstPointId, systemTunerPointArray, TUNER_BUILDING_VAL_LEVEL, hayStack)
            && copyTunerLevel(dstPointId, systemTunerPointArray, DEFAULT_VAL_LEVEL, hayStack))  {
            return true;
        }
    
        return false;
    }
    
    private static boolean copyFromBuildingTuner(String dstPointId, String queryString, CCUHsApi hayStack) {
    
        String buildingQuery = HSUtil.appendMarkerToQuery(queryString, Tags.DEFAULT);
        HashMap buildingTunerPoint = hayStack.read(buildingQuery);
        if (buildingTunerPoint.isEmpty()) {
            return false;
        }
    
        ArrayList<HashMap> buildingTunerPointArray = hayStack.readPoint(buildingTunerPoint.get("id").toString());
        
        if (copyTunerLevel(dstPointId, buildingTunerPointArray, TUNER_BUILDING_VAL_LEVEL, hayStack)
            && copyTunerLevel(dstPointId, buildingTunerPointArray, DEFAULT_VAL_LEVEL, hayStack))  {
            return true;
        }
    
        return false;
    }
    
    private static boolean copyTunerLevel(String dstPointId, ArrayList<HashMap> srcArray, int level, CCUHsApi hayStack) {
        HashMap levelMap = srcArray.get(level - 1 );
        if (levelMap != null && levelMap.get("val") != null) {
            hayStack.pointWrite(HRef.copy(dstPointId), level,
                                levelMap.get("who").toString(), HNum.make(Double.parseDouble(levelMap.get("val").toString())), HNum.make(0));
            return true;
        }
        return false;
    }
}
