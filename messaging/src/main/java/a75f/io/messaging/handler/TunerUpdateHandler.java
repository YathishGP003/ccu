package a75f.io.messaging.handler;

import com.google.gson.JsonObject;

import org.projecthaystack.HDict;

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
import a75f.io.logic.tuners.TunerUtil;


class TunerUpdateHandler {

    public static void updateBuildingTuner(final JsonObject msgObject, CCUHsApi hayStack) {
        
        String pointUid = "@" + msgObject.get(HayStackConstants.ID).getAsString();
        int level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt();
    
        CcuLog.i(L.TAG_CCU_PUBNUB, "BuildingTuner level "+level+" update received for point : " + pointUid);
        if (level != TunerConstants.TUNER_BUILDING_VAL_LEVEL) {
            return;
        }

        HashMap<Object, Object> tunerEquip = hayStack.readEntity("tuner and equip");
        if (tunerEquip.isEmpty()) {
            CcuLog.e(L.TAG_CCU_PUBNUB,"Tuner equip does not exist on CCU, Ignore updates");
            return;
        }

        //Do a local write to building level of BuildingTuner equip point
        writePointFromJson(pointUid, msgObject, hayStack, true);

        HDict remotePoint = hayStack.readRemotePoint("id == "+pointUid);
        CcuLog.i(L.TAG_CCU_PUBNUB, "Fetched remote point "+remotePoint);
        if (!remotePoint.has(Tags.DOMAIN_NAME)) {
            String domainName = remotePoint.get(Tags.DOMAIN_NAME).toString();
            propagateTunerByDomainName(domainName, msgObject, hayStack);
        } else {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Remote point does not have domainName : " +
                    "attempt propagation based on tags");
            //Propagate the updated tuner value to building level of corresponding system/zone equips
            propagateTunerByTags(pointUid, msgObject, hayStack);
        }
        TunerUtil.refreshEquipTuners();
    }
    
    /**
     * Building tuner updates for building level has to be propagated to corresponding
     * equip/system tuner points.
     */
    private static void propagateTunerByTags(String pointId, JsonObject msgObject, CCUHsApi hayStack) {
    
        HDict pointMap = CCUHsApi.getInstance().readHDictById(pointId);
        Point tunerPoint = new Point.Builder().setHDict(pointMap).build();
        
        tunerPoint.getMarkers().remove(Tags.DEFAULT);
        HSUtil.removeGenericMarkerTags(tunerPoint.getMarkers());
        String tunerQuery = HSUtil.getQueryFromMarkers(tunerPoint.getMarkers());
        tunerQuery = HSUtil.appendMarkerToQuery(tunerQuery, "not "+Tags.DEFAULT);
        
        ArrayList<HashMap<Object, Object>> equipTuners = CCUHsApi.getInstance()
                                                                .readAllEntities(tunerQuery);
    
        CcuLog.i(L.TAG_CCU_PUBNUB,
                 "Propagate tuners for point : "+tunerPoint.getDisplayName()+" to "+equipTuners.size()+" equips : query "+tunerQuery);
        equipTuners.stream()
                   .map( point -> point.get(Tags.ID).toString())
                   .forEach( id -> writePointFromJson(id, msgObject, hayStack, false));
    }

    private static void propagateTunerByDomainName(String domainName, JsonObject msgObject, CCUHsApi hayStack) {
        ArrayList<HashMap<Object, Object>> equipTuners = hayStack.readAllEntities("tuner and not default and domainName == \""+domainName+"\"");
        CcuLog.e(L.TAG_CCU_PUBNUB,"Tuner count for propagation "+equipTuners.size());
        equipTuners.stream()
                   .map(point -> point.get(Tags.ID).toString())
                   .forEach(id -> writePointFromJson(id, msgObject, hayStack, false));
    }
    
    private static void writePointFromJson(String id, JsonObject msgObject, CCUHsApi hayStack,
                                           boolean local) {
        try {
            String val = msgObject.get(HayStackConstants.WRITABLE_ARRAY_VAL).getAsString();
            
            if (val.isEmpty()) {
                //When a level is deleted, it currently generates a pubnub with empty value.
                //Handle it here.
                hayStack.clearPointArrayLevel(id, TunerConstants.TUNER_BUILDING_VAL_LEVEL, local);
                hayStack.writeHisValById(id, HSUtil.getPriorityVal(id));
                return;
            }
    
            String who = msgObject.get(HayStackConstants.WRITABLE_ARRAY_WHO).getAsString();
            double value = Double.parseDouble(val);
            if (local) {
                double durationDiff = MessageUtil.Companion.returnDurationDiff(msgObject);
                hayStack.writePointLocal(id,TunerConstants.TUNER_BUILDING_VAL_LEVEL, who, value, durationDiff);
                CcuLog.d(L.TAG_CCU_PUBNUB, "Building tuner point  Point : writePointFromJson - level: " +TunerConstants.TUNER_BUILDING_VAL_LEVEL + " who: " + who + " val: " + val  + " durationDiff: " + durationDiff);
            } else {
                int duration = msgObject.get(HayStackConstants.WRITABLE_ARRAY_DURATION) != null ? msgObject.get(
                        HayStackConstants.WRITABLE_ARRAY_DURATION).getAsInt() : 0;
                hayStack.writePoint(id, TunerConstants.TUNER_BUILDING_VAL_LEVEL,
                                    CCUHsApi.getInstance().getCCUUserName(), value, duration);
            }
            hayStack.writeHisValById(id, HSUtil.getPriorityVal(id));
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : "+msgObject+" ; "+e.getMessage());
        }
    }
}
