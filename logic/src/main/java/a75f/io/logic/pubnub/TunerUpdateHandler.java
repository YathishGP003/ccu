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
    
        CcuLog.i(L.TAG_CCU_PUBNUB, "BuildingTuner level "+level+" update received for point : " + pointGuid);
        if (level != TunerConstants.TUNER_BUILDING_VAL_LEVEL) {
            return;
        }
        
        String pointLuid = CCUHsApi.getInstance().getLUID("@" + pointGuid);
        
        //Do a local write to building level of BuildingTuner equip point
        writePointFromJson(pointLuid, msgObject, hayStack, true);
    
        //Propagate the updated tuner value to building level of corresponding system/zone equips
        propagateTuner(pointLuid, msgObject, hayStack);
        
    }
    
    /**
     * Building tuner updates for building level has to be propagated to corresponding
     * equip/system tuner points.
     */
    private static void propagateTuner(String pointId, JsonObject msgObject, CCUHsApi hayStack) {
    
        HashMap pointMap = CCUHsApi.getInstance().readMapById(pointId);
        Point tunerPoint = new Point.Builder().setHashMap(pointMap).build();
        
        tunerPoint.getMarkers().remove(Tags.DEFAULT);
        HSUtil.removeGenericMarkerTags(tunerPoint.getMarkers());
        String tunerQuery = HSUtil.getQueryFromMarkers(tunerPoint.getMarkers());
        tunerQuery = HSUtil.appendMarkerToQuery(tunerQuery, "not "+Tags.DEFAULT);
        
        ArrayList<HashMap<Object, Object>> equipTuners = CCUHsApi.getInstance()
                                                                .readAllEntities(tunerQuery);
    
        CcuLog.i(L.TAG_CCU_PUBNUB,
                 "Propagate tuners for point : "+tunerPoint.getDisplayName()+" to "+equipTuners.size()+" equips");
        Observable.fromIterable(equipTuners)
                  .subscribeOn(Schedulers.io())
                  .map(map -> map.get(Tags.ID).toString())
                  .subscribe(id -> writePointFromJson(id, msgObject, hayStack, false));
    }
    
    private static void writePointFromJson(String id, JsonObject msgObject, CCUHsApi hayStack,
                                           boolean local) {
        try {
            String val = msgObject.get(HayStackConstants.WRITABLE_ARRAY_VAL).getAsString();
            
            if (val.isEmpty()) {
                //When a level is deleted, it currently ends up in a pubnub with empty value.
                hayStack.deletePointArrayLevel(id, TunerConstants.TUNER_BUILDING_VAL_LEVEL);
                return;
            }
    
            String who = msgObject.get(HayStackConstants.WRITABLE_ARRAY_WHO).getAsString();
            double value = Double.parseDouble(val);
            int duration = msgObject.get(HayStackConstants.WRITABLE_ARRAY_DURATION) != null ? msgObject.get(
                                        HayStackConstants.WRITABLE_ARRAY_DURATION).getAsInt() : 0;
            if (local) {
                hayStack.writePointLocal(id, TunerConstants.TUNER_BUILDING_VAL_LEVEL, who, value, duration);
            } else {
                hayStack.writePoint(id, TunerConstants.TUNER_BUILDING_VAL_LEVEL,
                                    CCUHsApi.getInstance().getCCUUserName(), value, duration);
            }
            hayStack.writeHisValById(id, value);
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : "+msgObject+" ; "+e.getMessage());
        }
    }
}
