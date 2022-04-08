package a75f.io.logic.tuners;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;

public class TrueCFMTuners {
    public static void createTrueCfmTuners(CCUHsApi hayStack, String siteRef, String equipDis, String equipRef,
                                           String roomRef, String floorRef, String tz, String tag, String tunerGroup){

        Point airflowCFMProportionalRange  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"airflowCFMProportionalRange")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("prange").addMarker("cfm")
                .setMinVal("0").setMaxVal("1500").setIncrementVal("10").setTunerGroup(tunerGroup)
                .setTz(tz)
                .build();
        if(roomRef != null){
            airflowCFMProportionalRange.setRoomRef(roomRef);
            airflowCFMProportionalRange.setFloorRef(floorRef);
        }
        String airflowCFMProportionalRangeId = hayStack.addPoint(airflowCFMProportionalRange);
        hayStack.writePointForCcuUser(airflowCFMProportionalRangeId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_RANGE, 0);
        hayStack.writeHisValById(airflowCFMProportionalRangeId, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_RANGE);


        Point airflowCFMIntegralTime  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"airflowCFMIntegralTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("zone").addMarker("cfm").addMarker("itimeout").addMarker("time")
                .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(tunerGroup)
                .setTz(tz)
                .build();
        if(roomRef != null){
            airflowCFMIntegralTime.setRoomRef(roomRef);
            airflowCFMIntegralTime.setFloorRef(floorRef);
        }
        String airflowCFMIntegralTimeId = hayStack.addPoint(airflowCFMIntegralTime);
        hayStack.writePointForCcuUser(airflowCFMIntegralTimeId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_INTEGRAL_TIME, 0);
        hayStack.writeHisValById(airflowCFMIntegralTimeId, TunerConstants.AIR_FLOW_CFM_INTEGRAL_TIME);


        Point airflowCFMProportionalKFactor  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"airflowCFMProportionalKFactor")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("cfm").addMarker("pgain")
                .setMinVal("0").setMaxVal("1").setIncrementVal(".1").setTunerGroup(tunerGroup)
                .setTz(tz)
                .build();
        if(roomRef != null){
            airflowCFMProportionalKFactor.setRoomRef(roomRef);
            airflowCFMProportionalKFactor.setFloorRef(floorRef);
        }
        String airflowCFMProportionalKFactorId = hayStack.addPoint(airflowCFMProportionalKFactor);
        hayStack.writePointForCcuUser(airflowCFMProportionalKFactorId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_K_FACTOR, 0);
        hayStack.writeHisValById(airflowCFMProportionalKFactorId, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_K_FACTOR);


        Point airflowCFMIntegralKFactor  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"airflowCFMIntegralKFactor")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("zone").addMarker("cfm").addMarker("igain")
                .setMinVal("0").setMaxVal("1").setIncrementVal(".1").setTunerGroup(tunerGroup)
                .setTz(tz)
                .build();
        if(roomRef != null){
            airflowCFMIntegralKFactor.setRoomRef(roomRef);
            airflowCFMIntegralKFactor.setFloorRef(floorRef);
        }
        String airflowCFMIntegralKFactorId = hayStack.addPoint(airflowCFMIntegralKFactor);
        hayStack.writePointForCcuUser(airflowCFMIntegralKFactorId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_INTEGRAL_K_FACTOR, 0);
        hayStack.writeHisValById(airflowCFMIntegralKFactorId, TunerConstants.AIR_FLOW_CFM_INTEGRAL_K_FACTOR);

    }
}