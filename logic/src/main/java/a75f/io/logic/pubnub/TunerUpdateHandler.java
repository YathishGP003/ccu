package a75f.io.logic.pubnub;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.tuners.TunerConstants;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;


class TunerUpdateHandler {
    
    public static void updateBuildingTuner(final JsonObject msgObject, CCUHsApi hayStack) {
        
        String pointGuid = msgObject.get(HayStackConstants.ID).getAsString();
        int level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt();
        
        String pointLuid = CCUHsApi.getInstance().getLUID("@" + pointGuid);
        writePointFromJson(pointLuid, level, msgObject, hayStack);
        
        if (level == TunerConstants.TUNER_BUILDING_VAL_LEVEL) {
            CcuLog.i(L.TAG_CCU_PUBNUB, "Building Level updated for point : " + pointGuid);
            return;
        }
        
        HashMap pointMap = CCUHsApi.getInstance().readMapById(pointLuid);
        Point tunerPoint = new Point.Builder().setHashMap(pointMap).build();
        
        //Tuner value is propagated to different equips based on what level has been updated.
        if (level == TunerConstants.TUNER_EQUIP_VAL_LEVEL ||
                                    level == TunerConstants.TUNER_ZONE_VAL_LEVEL ) {
            propagateTuner(tunerPoint, msgObject, hayStack, level, Tags.ZONE);
        } else if (level == TunerConstants.TUNER_SYSTEM_VAL_LEVEL) {
            propagateTuner(tunerPoint, msgObject, hayStack, level, Tags.SYSTEM);
        }
        
    }
    
    /**
     * Building tuner updates for equip/zone/system level has to be propagated to corresponding
     * equip/system level points.
     */
    private static void propagateTuner(Point tunerPoint, JsonObject msgObject, CCUHsApi hayStack, int level,
                                                    String typeTag) {
        
        CcuLog.i(L.TAG_CCU_PUBNUB, "Propagate tuners for point : "+tunerPoint.getDisplayName()+" "+typeTag);
        tunerPoint.getMarkers().remove(Tags.DEFAULT);
        tunerPoint.getMarkers().add(typeTag);
        
        String tunerQuery = HSUtil.getHQueryFromMarkers(tunerPoint.getMarkers());
        ArrayList<HashMap<Object, Object>> equipTuners = CCUHsApi.getInstance()
                                                                .readAllEntities(tunerQuery);
    
        Observable.fromIterable(equipTuners)
                  .subscribeOn(Schedulers.io())
                  .map(map -> map.get(Tags.ID).toString())
                  .subscribe(id -> writePointFromJson(id, level, msgObject, hayStack));
    }
    
    private static void writePointFromJson(String id, int level, JsonObject msgObject, CCUHsApi hayStack) {
        String who = msgObject.get(HayStackConstants.WRITABLE_ARRAY_WHO).getAsString();
        double val = msgObject.get(HayStackConstants.WRITABLE_ARRAY_VAL).getAsDouble();
        int duration = msgObject.get(HayStackConstants.WRITABLE_ARRAY_DURATION).getAsInt();
        hayStack.writePoint(id, level, who, val, duration);
    }
}
