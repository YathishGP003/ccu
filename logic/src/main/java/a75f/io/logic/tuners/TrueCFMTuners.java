package a75f.io.logic.tuners;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;

public class TrueCFMTuners {
    
    public static void createDefaultTrueCfmTuners(CCUHsApi hayStack, Equip equip, String profileTag, String tunerGroup){
        
        Point airflowCFMProportionalRange  = new Point.Builder()
                                                 .setDisplayName(equip.getDisplayName()+"-"+"airflowCFMProportionalRange")
                                                 .setSiteRef(equip.getSiteRef())
                                                 .setEquipRef(equip.getId()).setHisInterpolate("cov")
                                                 .addMarker("tuner").addMarker("airflow").addMarker("writable").addMarker("his")
                                                 .addMarker("prange").addMarker("trueCfm").addMarker("default").addMarker(profileTag)
                                                 .setMinVal("0").setMaxVal("1500").setIncrementVal("10").setTunerGroup(tunerGroup)
                                                 .setTz(equip.getTz())
                                                 .build();
        
        String airflowCFMProportionalRangeId = hayStack.addPoint(airflowCFMProportionalRange);
        hayStack.writePointForCcuUser(airflowCFMProportionalRangeId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_RANGE, 0);
        hayStack.writeHisValById(airflowCFMProportionalRangeId, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_RANGE);
        
        Point airflowCFMIntegralTime  = new Point.Builder()
                                            .setDisplayName(equip.getDisplayName()+"-"+"airflowCFMIntegralTime")
                                            .setSiteRef(equip.getSiteRef())
                                            .setEquipRef(equip.getId()).setHisInterpolate("cov")
                                            .addMarker("tuner").addMarker("airflow").addMarker("writable").addMarker("his")
                                            .addMarker("zone").addMarker("trueCfm").addMarker("itimeout").addMarker("time")
                                            .addMarker("default").addMarker(profileTag)
                                            .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(tunerGroup)
                                            .setTz(equip.getTz())
                                            .build();
        
        String airflowCFMIntegralTimeId = hayStack.addPoint(airflowCFMIntegralTime);
        hayStack.writePointForCcuUser(airflowCFMIntegralTimeId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_INTEGRAL_TIME, 0);
        hayStack.writeHisValById(airflowCFMIntegralTimeId, TunerConstants.AIR_FLOW_CFM_INTEGRAL_TIME);
        
        
        Point airflowCFMProportionalKFactor  = new Point.Builder()
                                                   .setDisplayName(equip.getDisplayName()+"-"+"airflowCFMProportionalKFactor")
                                                   .setSiteRef(equip.getSiteRef())
                                                   .setEquipRef(equip.getId()).setHisInterpolate("cov")
                                                   .addMarker("tuner").addMarker("airflow").addMarker("writable").addMarker("his")
                                                   .addMarker("trueCfm").addMarker("pgain")
                                                   .addMarker("default").addMarker(profileTag)
                                                   .setMinVal("0").setMaxVal("1").setIncrementVal(".1").setTunerGroup(tunerGroup)
                                                   .setTz(equip.getTz())
                                                   .build();
        
        String airflowCFMProportionalKFactorId = hayStack.addPoint(airflowCFMProportionalKFactor);
        hayStack.writePointForCcuUser(airflowCFMProportionalKFactorId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_K_FACTOR, 0);
        hayStack.writeHisValById(airflowCFMProportionalKFactorId, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_K_FACTOR);
        
        
        Point airflowCFMIntegralKFactor  = new Point.Builder()
                                               .setDisplayName(equip.getDisplayName()+"-"+"airflowCFMIntegralKFactor")
                                               .setSiteRef(equip.getSiteRef())
                                               .setEquipRef(equip.getId()).setHisInterpolate("cov")
                                               .addMarker("tuner").addMarker("airflow").addMarker("writable").addMarker("his")
                                               .addMarker("zone").addMarker("trueCfm").addMarker("igain")
                                               .addMarker("default").addMarker(profileTag)
                                               .setMinVal("0").setMaxVal("1").setIncrementVal(".1").setTunerGroup(tunerGroup)
                                               .setTz(equip.getTz())
                                               .build();
        
        String airflowCFMIntegralKFactorId = hayStack.addPoint(airflowCFMIntegralKFactor);
        hayStack.writePointForCcuUser(airflowCFMIntegralKFactorId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_INTEGRAL_K_FACTOR, 0);
        hayStack.writeHisValById(airflowCFMIntegralKFactorId, TunerConstants.AIR_FLOW_CFM_INTEGRAL_K_FACTOR);
        
    }

    public static void createTrueCfmTuners(CCUHsApi hayStack, Equip equip, String tag, String tunerGroup){

        Point airflowCFMProportionalRange  = new Point.Builder()
                .setDisplayName(equip.getDisplayName()+"-"+"airflowCFMProportionalRange")
                .setSiteRef(equip.getSiteRef())
                .setEquipRef(equip.getId()).setHisInterpolate("cov")
                .setRoomRef(equip.getRoomRef())
                .setFloorRef(equip.getFloorRef())
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("prange").addMarker("trueCfm")
                .setMinVal("0").setMaxVal("1500").setIncrementVal("10").setTunerGroup(tunerGroup)
                .setTz(equip.getTz())
                .build();

        String airflowCFMProportionalRangeId = hayStack.addPoint(airflowCFMProportionalRange);
        BuildingTunerUtil.updateTunerLevels(airflowCFMProportionalRangeId, equip.getRoomRef(), hayStack);
        hayStack.writeHisValById(airflowCFMProportionalRangeId, HSUtil.getPriorityVal(airflowCFMProportionalRangeId));
        
        Point airflowCFMIntegralTime  = new Point.Builder()
                .setDisplayName(equip.getDisplayName()+"-"+"airflowCFMIntegralTime")
                .setSiteRef(equip.getSiteRef())
                .setEquipRef(equip.getId()).setHisInterpolate("cov")
                .setRoomRef(equip.getRoomRef())
                .setFloorRef(equip.getFloorRef())
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("zone").addMarker("trueCfm").addMarker("itimeout").addMarker("time")
                .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(tunerGroup)
                .setTz(equip.getTz())
                .build();
        
        String airflowCFMIntegralTimeId = hayStack.addPoint(airflowCFMIntegralTime);
        BuildingTunerUtil.updateTunerLevels(airflowCFMIntegralTimeId, equip.getRoomRef(), hayStack);
        hayStack.writeHisValById(airflowCFMIntegralTimeId, HSUtil.getPriorityVal(airflowCFMIntegralTimeId));

        Point airflowCFMProportionalKFactor  = new Point.Builder()
                .setDisplayName(equip.getDisplayName()+"-"+"airflowCFMProportionalKFactor")
                .setSiteRef(equip.getSiteRef())
                .setEquipRef(equip.getId()).setHisInterpolate("cov")
                .setRoomRef(equip.getRoomRef())
                .setFloorRef(equip.getFloorRef())
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("trueCfm").addMarker("pgain")
                .setMinVal("0").setMaxVal("1").setIncrementVal(".1").setTunerGroup(tunerGroup)
                .setTz(equip.getTz())
                .build();
        
        String airflowCFMProportionalKFactorId = hayStack.addPoint(airflowCFMProportionalKFactor);
        BuildingTunerUtil.updateTunerLevels(airflowCFMProportionalKFactorId, equip.getRoomRef(), hayStack);
        hayStack.writeHisValById(airflowCFMProportionalKFactorId, HSUtil.getPriorityVal(airflowCFMProportionalKFactorId));

        Point airflowCFMIntegralKFactor  = new Point.Builder()
                .setDisplayName(equip.getDisplayName()+"-"+"airflowCFMIntegralKFactor")
                .setSiteRef(equip.getSiteRef())
                .setEquipRef(equip.getId()).setHisInterpolate("cov")
                .setRoomRef(equip.getRoomRef())
                .setFloorRef(equip.getFloorRef())
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("zone").addMarker("trueCfm").addMarker("igain")
                .setMinVal("0").setMaxVal("1").setIncrementVal(".1").setTunerGroup(tunerGroup)
                .setTz(equip.getTz())
                .build();
        String airflowCFMIntegralKFactorId = hayStack.addPoint(airflowCFMIntegralKFactor);
        BuildingTunerUtil.updateTunerLevels(airflowCFMIntegralKFactorId, equip.getRoomRef(), hayStack);
        hayStack.writeHisValById(airflowCFMIntegralKFactorId, HSUtil.getPriorityVal(airflowCFMIntegralKFactorId));
    }
}