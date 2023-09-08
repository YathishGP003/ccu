package a75f.io.logic.bo.building.truecfm;

import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class DabTrueCfmHandler {
    
    private final int MOVING_AVERAGE_QUEUE_SIZE = 15;
    
    private       Map<String, EvictingQueue> damperPosMAQueueMap = new HashMap<>();
    private final Map<String, EvictingQueue> airflowMAQueueMap   = new HashMap<>();
    
    private static DabTrueCfmHandler instance;
    
    private DabTrueCfmHandler() {}
    
    public static DabTrueCfmHandler getInstance() {
        if (instance == null) {
            instance = new DabTrueCfmHandler();
        }
        return instance;
    }
    
    private double getMovingAverageFromQueue(EvictingQueue<Double> movingAverageQueue) {
        
        if (movingAverageQueue == null || movingAverageQueue.isEmpty()) {
            return 0;
        }
        OptionalDouble averageVal =  movingAverageQueue.stream()
                                                  .filter(val -> val != 0)
                                                  .mapToDouble( d ->d)
                                                  .average();
        
        return averageVal.isPresent() ? averageVal.getAsDouble() : 0;
        
    }
    
    private void copyDamperPos(String equipRef, HashMap<String, Double> dstMap, HashMap<String, Double> srcMap) {
        if (srcMap.get(equipRef) != null) {
            dstMap.put(equipRef, srcMap.get(equipRef));
        }
    }
    
    public void updateDamperPosQueueMap(String damperPointId, double damperPos) {
        EvictingQueue<Double> damperPosMAQueue = damperPosMAQueueMap.get(damperPointId);
        
        if (damperPosMAQueue == null) {
            damperPosMAQueue = EvictingQueue.create(MOVING_AVERAGE_QUEUE_SIZE);
            damperPosMAQueueMap.put(damperPointId, damperPosMAQueue);
        }
        damperPosMAQueue.add(damperPos);
    }
    
    public void updateAirflowQueueMap(String equipRef, double airflow) {
        EvictingQueue<Double> airflowMAQueue = airflowMAQueueMap.get(equipRef);
        
        if (airflowMAQueue == null) {
            airflowMAQueue = EvictingQueue.create(MOVING_AVERAGE_QUEUE_SIZE);
            airflowMAQueueMap.put(equipRef, airflowMAQueue);
        }
        airflowMAQueue.add(airflow);
    }
    
    public HashMap<String, Double> getCfMUpdatedDamperPosMap(ArrayList<HashMap<Object, Object>> dabEquips,
                                           HashMap<String, Double> normalizedDamperMap, CCUHsApi hayStack) {
        
        HashMap<String, Double> cfmAdjustedDamperMap = new HashMap<>();
        for (HashMap<Object, Object> dabEquip : dabEquips) {
            CcuLog.i(L.TAG_CCU_SYSTEM, " UpdateCFMDamper for equip : "+dabEquip.get("dis"));
            String equipRef = Objects.requireNonNull(dabEquip.get("id")).toString();
    
            HashMap<Object, Object> primaryDamperPosPoint = hayStack.readEntity(
                "point and damper and normalized and primary and cmd "
                + "and equipRef == \"" + equipRef + "\"");
    
            HashMap<Object, Object> secondaryDamperPosPoint = hayStack.readEntity(
                "point and damper and normalized and secondary and cmd " +
                "and equipRef == \"" + equipRef + "\"");
            
            // If TrueCFM is not enabled, just copy the current damper pos and move to the
            // next equip.
            if (TrueCFMUtil.cfmControlNotRequired(hayStack, equipRef)) {
                CcuLog.i(L.TAG_CCU_SYSTEM, " CfmUpdatedDamper: cfmControlNotRequired");
                copyDamperPos(primaryDamperPosPoint.get("id").toString(), cfmAdjustedDamperMap, normalizedDamperMap);
                copyDamperPos(secondaryDamperPosPoint.get("id").toString(), cfmAdjustedDamperMap, normalizedDamperMap);
                continue;
            }
    
            
            
            Double primaryDamperVal = getUpdatedDamperVal(hayStack, equipRef,
                                                          primaryDamperPosPoint.get("id").toString(), Tags.PRIMARY,
                                                          normalizedDamperMap.get(primaryDamperPosPoint.get("id").toString()));
            
            if (primaryDamperVal != null) {
                cfmAdjustedDamperMap.put(primaryDamperPosPoint.get("id").toString(), primaryDamperVal);
            } else {
                copyDamperPos(primaryDamperPosPoint.get("id").toString(), cfmAdjustedDamperMap, normalizedDamperMap);
            }
            
            Double secondaryDamperVal = getUpdatedDamperVal(hayStack, equipRef,
                                                            Objects.requireNonNull(secondaryDamperPosPoint.get("id")).toString(),
                                                            Tags.SECONDARY,
                                                            normalizedDamperMap.get(secondaryDamperPosPoint.get("id").toString()));
            
            if (secondaryDamperVal != null) {
                cfmAdjustedDamperMap.put(Objects.requireNonNull(secondaryDamperPosPoint.get("id")).toString(), secondaryDamperVal);
            } else {
                copyDamperPos(Objects.requireNonNull(secondaryDamperPosPoint.get("id")).toString(), cfmAdjustedDamperMap, normalizedDamperMap);
            }
        }
        
        cfmAdjustedDamperMap.forEach( (damperPoint, damperPos) ->
                        CcuLog.i(L.TAG_CCU_SYSTEM,
                                 " CfmUpdatedDamper: damperPoint " + damperPoint + "Updated damperVal " + damperPos));
        return cfmAdjustedDamperMap;
        
    }
    
    private Double getUpdatedDamperVal(CCUHsApi hayStack, String equipRef, String damperPointId, String damperType,
                                       double defaultDamperVal) {
        
        EvictingQueue<Double> damperValQueue = damperPosMAQueueMap.get(damperPointId);
        double damperMAVal = getMovingAverageFromQueue(damperValQueue);
        //Handle the case when queue is empty
        if (damperMAVal == 0) {
            damperMAVal = defaultDamperVal;
            CcuLog.d(L.TAG_CCU_SYSTEM, "getUpdatedDamperVal: damperValQueue not initialized "+defaultDamperVal);
        }
    
        EvictingQueue<Double> cfmValQueue = airflowMAQueueMap.get(damperPointId);
        double cfmMAVal = getMovingAverageFromQueue(cfmValQueue);
        //Handle the case when queue is empty
        if (cfmMAVal == 0) {
            cfmMAVal = TrueCFMUtil.calculateAndUpdateCfm(hayStack, equipRef, damperType);
            CcuLog.d(L.TAG_CCU_SYSTEM, "getUpdatedDamperVal: cfmValQueue not initialized cfmMAVal "+cfmMAVal);
        }
    
        double minCfmIAQ = hayStack.readDefaultVal("min and trueCfm and iaq and config and equipRef == \"" +
                                                   ""+equipRef+ "\"");
    
        CcuLog.i(L.TAG_CCU_SYSTEM,
                 "getUpdatedDamperVal: minCfmIAQ "+minCfmIAQ+" cfmMAVal "+cfmMAVal+" damperMAVal "+damperMAVal);
        if (cfmMAVal != 0 && damperMAVal != 0 && cfmMAVal != minCfmIAQ) {
            return Math.min(minCfmIAQ * damperMAVal / cfmMAVal, 100);
        }
        return null;
    }
    
    public void updateAirflowMAQueue(CCUHsApi hayStack, String equipRef, String damperType, String damperPointId) {
        double currentAirflow = TrueCFMUtil.calculateAndUpdateCfm(hayStack, equipRef, damperType);
        CcuLog.i(L.TAG_CCU_SYSTEM, "updateAirflowMAQueue: "+damperPointId+" "+damperType+" currentAirflow "
                                   +currentAirflow);
        updateAirflowQueueMap(damperPointId, Math.max(currentAirflow, 1));
    }
    
}
