package a75f.io.logic.tuners;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;

public class TrueCFMTuners {


    private static void createTrueCFMAirflowCFMProportionalRangePoint(CCUHsApi hayStack, Equip equip, String tag, String tunerGroup) {

        Point airflowCFMProportionalRange = new Point.Builder()
                .setDisplayName(equip.getDisplayName() + "-" + "airflowCFMProportionalRange")
                .setSiteRef(equip.getSiteRef())
                .setEquipRef(equip.getId()).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("prange").addMarker("cfm")
                .setMinVal("0").setMaxVal("1500").setIncrementVal("10").setTunerGroup(tunerGroup)
                .setTz(equip.getTz())
                .build();
        if (equip.getRoomRef() != null) {
            airflowCFMProportionalRange.setRoomRef(equip.getRoomRef());
            airflowCFMProportionalRange.setFloorRef(equip.getFloorRef());
        }
        String airflowCFMProportionalRangeId = hayStack.addPoint(airflowCFMProportionalRange);
        hayStack.writePointForCcuUser(airflowCFMProportionalRangeId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_RANGE, 0);
        hayStack.writeHisValById(airflowCFMProportionalRangeId, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_RANGE);
    }

    private static void createTrueCFMAirflowCFMIntegralTimePoint(CCUHsApi hayStack, Equip equip, String tag, String tunerGroup) {
        Point airflowCFMIntegralTime = new Point.Builder()
                .setDisplayName(equip.getDisplayName() + "-" + "airflowCFMIntegralTime")
                .setSiteRef(equip.getSiteRef())
                .setEquipRef(equip.getId()).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("zone").addMarker("cfm").addMarker("itimeout").addMarker("time")
                .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(tunerGroup)
                .setTz(equip.getTz())
                .build();
        if (equip.getRoomRef() != null) {
            airflowCFMIntegralTime.setRoomRef(equip.getRoomRef());
            airflowCFMIntegralTime.setFloorRef(equip.getFloorRef());
        }
        String airflowCFMIntegralTimeId = hayStack.addPoint(airflowCFMIntegralTime);
        hayStack.writePointForCcuUser(airflowCFMIntegralTimeId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_INTEGRAL_TIME, 0);
        hayStack.writeHisValById(airflowCFMIntegralTimeId, TunerConstants.AIR_FLOW_CFM_INTEGRAL_TIME);
    }

    private static void createTrueCFMAirflowCFMProportionalKFactorPoint(CCUHsApi hayStack, Equip equip, String tag, String tunerGroup) {
        Point airflowCFMProportionalKFactor = new Point.Builder()
                .setDisplayName(equip.getDisplayName() + "-" + "airflowCFMProportionalKFactor")
                .setSiteRef(equip.getSiteRef())
                .setEquipRef(equip.getId()).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("cfm").addMarker("pgain")
                .setMinVal("0").setMaxVal("1").setIncrementVal(".1").setTunerGroup(tunerGroup)
                .setTz(equip.getTz())
                .build();
        if (equip.getRoomRef() != null) {
            airflowCFMProportionalKFactor.setRoomRef(equip.getRoomRef());
            airflowCFMProportionalKFactor.setFloorRef(equip.getFloorRef());
        }
        String airflowCFMProportionalKFactorId = hayStack.addPoint(airflowCFMProportionalKFactor);
        hayStack.writePointForCcuUser(airflowCFMProportionalKFactorId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_K_FACTOR, 0);
        hayStack.writeHisValById(airflowCFMProportionalKFactorId, TunerConstants.AIR_FLOW_CFM_PROPORTIONAL_K_FACTOR);
    }

    private static void createTrueCFMAirflowCFMIntegralKFactorPoint(CCUHsApi hayStack, Equip equip, String tag, String tunerGroup) {
        Point airflowCFMIntegralKFactor  = new Point.Builder()
                .setDisplayName(equip.getDisplayName()+"-"+"airflowCFMIntegralKFactor")
                .setSiteRef(equip.getSiteRef())
                .setEquipRef(equip.getId()).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("airflow").addMarker(tag).addMarker("writable").addMarker("his")
                .addMarker("zone").addMarker("cfm").addMarker("igain")
                .setMinVal("0").setMaxVal("1").setIncrementVal(".1").setTunerGroup(tunerGroup)
                .setTz(equip.getTz())
                .build();
        if(equip.getRoomRef() != null){
            airflowCFMIntegralKFactor.setRoomRef(equip.getRoomRef());
            airflowCFMIntegralKFactor.setFloorRef(equip.getFloorRef());
        }
        String airflowCFMIntegralKFactorId = hayStack.addPoint(airflowCFMIntegralKFactor);
        hayStack.writePointForCcuUser(airflowCFMIntegralKFactorId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.AIR_FLOW_CFM_INTEGRAL_K_FACTOR, 0);
        hayStack.writeHisValById(airflowCFMIntegralKFactorId, TunerConstants.AIR_FLOW_CFM_INTEGRAL_K_FACTOR);

    }
    public static void createTrueCFMVavTunerPoints(CCUHsApi hayStack, Equip equip) {

        createTrueCFMAirflowCFMProportionalRangePoint(hayStack,equip,TunerConstants.VAV_TAG,TunerConstants.VAV_TUNER_GROUP );

        createTrueCFMAirflowCFMIntegralTimePoint(hayStack,equip,TunerConstants.VAV_TAG,TunerConstants.VAV_TUNER_GROUP );

        createTrueCFMAirflowCFMProportionalKFactorPoint(hayStack,equip,TunerConstants.VAV_TAG,TunerConstants.VAV_TUNER_GROUP);

        createTrueCFMAirflowCFMIntegralKFactorPoint(hayStack,equip,TunerConstants.VAV_TAG,TunerConstants.VAV_TUNER_GROUP);

    }
}