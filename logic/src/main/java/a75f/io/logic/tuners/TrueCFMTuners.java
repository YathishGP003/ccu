package a75f.io.logic.tuners;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;

public class TrueCFMTuners {
    
    public static void createDefaultTrueCfmTuners(CCUHsApi hayStack, String siteRef, String equipDis, String equipRef,
                                                                                        String tz, String tunerGroup){
        
        Point airflowCFMProportionalRange  = new Point.Builder()
                                                 .setDisplayName(equipDis+"-"+"airflowCFMProportionalRange")
                                                 .setSiteRef(siteRef)
                                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                                 .addMarker("tuner").addMarker("airflow").addMarker("writable").addMarker("his")
                                                 .addMarker("prange").addMarker("cfm").addMarker("default")
                                                 .setMinVal("0").setMaxVal("1500").setIncrementVal("10").setTunerGroup(tunerGroup)
                                                 .setTz(tz)
                                                 .build();
        
        String airflowCFMProportionalRangeId = hayStack.addPoint(airflowCFMProportionalRange);
        hayStack.writePointForCcuUser(airflowCFMProportionalRangeId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_RANGE, 0);
        hayStack.writeHisValById(airflowCFMProportionalRangeId, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_RANGE);
        
        Point airflowCFMIntegralTime  = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"airflowCFMIntegralTime")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef).setHisInterpolate("cov")
                                            .addMarker("tuner").addMarker("airflow").addMarker("writable").addMarker("his")
                                            .addMarker("zone").addMarker("cfm").addMarker("itimeout").addMarker("time").addMarker("default")
                                            .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(tunerGroup)
                                            .setTz(tz)
                                            .build();
        
        String airflowCFMIntegralTimeId = hayStack.addPoint(airflowCFMIntegralTime);
        hayStack.writePointForCcuUser(airflowCFMIntegralTimeId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_INTEGRAL_TIME, 0);
        hayStack.writeHisValById(airflowCFMIntegralTimeId, TunerConstants.AIR_FLOW_CFM_INTEGRAL_TIME);
        
        
        Point airflowCFMProportionalKFactor  = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"airflowCFMProportionalKFactor")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipRef).setHisInterpolate("cov")
                                                   .addMarker("tuner").addMarker("airflow").addMarker("writable").addMarker("his")
                                                   .addMarker("cfm").addMarker("pgain").addMarker("default")
                                                   .setMinVal("0").setMaxVal("1").setIncrementVal(".1").setTunerGroup(tunerGroup)
                                                   .setTz(tz)
                                                   .build();
        
        String airflowCFMProportionalKFactorId = hayStack.addPoint(airflowCFMProportionalKFactor);
        hayStack.writePointForCcuUser(airflowCFMProportionalKFactorId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_K_FACTOR, 0);
        hayStack.writeHisValById(airflowCFMProportionalKFactorId, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_K_FACTOR);
        
        
        Point airflowCFMIntegralKFactor  = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"airflowCFMIntegralKFactor")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef).setHisInterpolate("cov")
                                               .addMarker("tuner").addMarker("airflow").addMarker("writable").addMarker("his")
                                               .addMarker("zone").addMarker("cfm").addMarker("igain").addMarker("default")
                                               .setMinVal("0").setMaxVal("1").setIncrementVal(".1").setTunerGroup(tunerGroup)
                                               .setTz(tz)
                                               .build();
        
        String airflowCFMIntegralKFactorId = hayStack.addPoint(airflowCFMIntegralKFactor);
        hayStack.writePointForCcuUser(airflowCFMIntegralKFactorId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_INTEGRAL_K_FACTOR, 0);
        hayStack.writeHisValById(airflowCFMIntegralKFactorId, TunerConstants.AIR_FLOW_CFM_INTEGRAL_K_FACTOR);
        
    }
    
    public static void createTrueCfmTuners(CCUHsApi hayStack, String siteRef, String equipDis, String equipRef,
                                           String roomRef, String floorRef, String tz, String tag, String tunerGroup){

        Point airflowCFMProportionalRange  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"airflowCFMProportionalRange")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("prange").addMarker("cfm")
                .setMinVal("0").setMaxVal("1500").setIncrementVal("10").setTunerGroup(tunerGroup)
                .setTz(tz)
                .build();
        
        String airflowCFMProportionalRangeId = hayStack.addPoint(airflowCFMProportionalRange);
        BuildingTunerUtil.updateTunerLevels(airflowCFMProportionalRangeId, roomRef, hayStack);
        hayStack.writeHisValById(airflowCFMProportionalRangeId, HSUtil.getPriorityVal(airflowCFMProportionalRangeId));
        
        Point airflowCFMIntegralTime  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"airflowCFMIntegralTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("zone").addMarker("cfm").addMarker("itimeout").addMarker("time")
                .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(tunerGroup)
                .setTz(tz)
                .build();
        
        String airflowCFMIntegralTimeId = hayStack.addPoint(airflowCFMIntegralTime);
        BuildingTunerUtil.updateTunerLevels(airflowCFMIntegralTimeId, roomRef, hayStack);
        hayStack.writeHisValById(airflowCFMIntegralTimeId, HSUtil.getPriorityVal(airflowCFMIntegralTimeId));

        Point airflowCFMProportionalKFactor  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"airflowCFMProportionalKFactor")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("cfm").addMarker("pgain")
                .setMinVal("0").setMaxVal("1").setIncrementVal(".1").setTunerGroup(tunerGroup)
                .setTz(tz)
                .build();
        
        String airflowCFMProportionalKFactorId = hayStack.addPoint(airflowCFMProportionalKFactor);
        BuildingTunerUtil.updateTunerLevels(airflowCFMProportionalKFactorId, roomRef, hayStack);
        hayStack.writeHisValById(airflowCFMProportionalKFactorId, HSUtil.getPriorityVal(airflowCFMProportionalKFactorId));

        Point airflowCFMIntegralKFactor  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"airflowCFMIntegralKFactor")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("zone").addMarker("cfm").addMarker("igain")
                .setMinVal("0").setMaxVal("1").setIncrementVal(".1").setTunerGroup(tunerGroup)
                .setTz(tz)
                .build();
        
        String airflowCFMIntegralKFactorId = hayStack.addPoint(airflowCFMIntegralKFactor);
        BuildingTunerUtil.updateTunerLevels(airflowCFMIntegralKFactorId, roomRef, hayStack);
        hayStack.writeHisValById(airflowCFMIntegralKFactorId, HSUtil.getPriorityVal(airflowCFMIntegralKFactorId));
    }
}