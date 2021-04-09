package a75f.io.logic.tuners;

import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class DcwbTuners {
    
    public static void addDefaultDcwbTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                           String tz) {
        HashMap tuner = CCUHsApi.getInstance().read("point and tuner and default and dcwb");
        if (tuner != null && tuner.size() > 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "Default DCWB Tuner points already exist");
            return;
        }
        CcuLog.d(L.TAG_CCU_SYSTEM, "Default DCWB Tuner  does not exist. Create Now");
    
        Point chilledWaterProportionalKFactor  = new Point.Builder()
                             .setDisplayName(equipDis+"-DCWB-"+"chilledWaterProportionalKFactor  ")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("default").addMarker("dcwb").addMarker("writable").addMarker("his")
                             .addMarker("pgain").addMarker("sp").addMarker("chilled").addMarker("water")
                             .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                             .setTz(tz)
                             .build();
        String chilledWaterProportionalKFactorId = hayStack.addPoint(chilledWaterProportionalKFactor);
        hayStack.writePointForCcuUser(chilledWaterProportionalKFactorId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                      TunerConstants.VAV_PROPORTIONAL_GAIN, 0);
        hayStack.writeHisValById(chilledWaterProportionalKFactorId, TunerConstants.VAV_PROPORTIONAL_GAIN);
    
        Point chilledWaterIntegralKFactor = new Point.Builder()
                                 .setDisplayName(equipDis+"-DCWB-"+"chilledWaterIntegralKFactor ")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("default").addMarker("dcwb").addMarker("writable").addMarker("his")
                                 .addMarker("igain").addMarker("sp").addMarker("chilled").addMarker("water")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                 .setTz(tz)
                                 .build();
        String chilledWaterIntegralKFactorId = hayStack.addPoint(chilledWaterIntegralKFactor);
        hayStack.writePointForCcuUser(chilledWaterIntegralKFactorId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                      TunerConstants.VAV_INTEGRAL_GAIN, 0);
        hayStack.writeHisValById(chilledWaterIntegralKFactorId, TunerConstants.VAV_INTEGRAL_GAIN);
    
        Point chilledWaterTemperatureProportionalRange = new Point.Builder()
                               .setDisplayName(equipDis+"-DCWB-"+"chilledWaterTemperatureProportionalRange ")
                               .setSiteRef(siteRef)
                               .setEquipRef(equipRef).setHisInterpolate("cov")
                               .addMarker("tuner").addMarker("default").addMarker("dcwb").addMarker("writable").addMarker("his")
                               .addMarker("pspread").addMarker("sp").addMarker("chilled").addMarker("water")
                               .setMinVal("1").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                               .setTz(tz)
                               .build();
        String pSpreadId = hayStack.addPoint(chilledWaterTemperatureProportionalRange);
        hayStack.writePointForCcuUser(pSpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.CHILLED_WATER_TEMP_PROPORTION_SPREAD, 0);
        hayStack.writeHisValById(pSpreadId, TunerConstants.CHILLED_WATER_TEMP_PROPORTION_SPREAD);
    
        Point integralTimeout = new Point.Builder()
                                    .setDisplayName(equipDis+"-DCWB-"+"temperatureIntegralTime ")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef).setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker("default").addMarker("dcwb").addMarker("writable").addMarker("his")
                                    .addMarker("itimeout").addMarker("sp").addMarker("chilled").addMarker("water")
                                    .setUnit("m")
                                    .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                    .setTz(tz)
                                    .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePointForCcuUser(iTimeoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.VAV_INTEGRAL_TIMEOUT, 0);
        hayStack.writeHisValById(iTimeoutId, TunerConstants.VAV_INTEGRAL_TIMEOUT);
    
        Point adaptiveComfortThresholdMargin = new Point.Builder()
                                    .setDisplayName(equipDis+"-DCWB-"+"adaptiveComfortThresholdMargin ")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef).setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker("default").addMarker("dcwb").addMarker("writable").addMarker("his")
                                    .addMarker("sp").addMarker("chilled").addMarker("water")
                                    .addMarker("adaptive").addMarker("comfort").addMarker("threshold").addMarker("margin")
                                    .setUnit("m")
                                    .setMinVal("1").setMaxVal("15").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                    .setTz(tz)
                                    .build();
        String adaptiveComfortThresholdMarginId = hayStack.addPoint(adaptiveComfortThresholdMargin);
        hayStack.writePointForCcuUser(adaptiveComfortThresholdMarginId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                      TunerConstants.ADAPTIVE_COMFORT_THRESHOLD_MARGIN, 0);
        hayStack.writeHisValById(adaptiveComfortThresholdMarginId, TunerConstants.ADAPTIVE_COMFORT_THRESHOLD_MARGIN);
    }
    
    public static void addEquipDcwbTuners(CCUHsApi hayStack, String siteRef, String equipDis, String equipRef,
                                        String tz) {
        Log.d("CCU", "addEquipDcwbTuners for " + equipDis);
    
        Point chilledWaterProportionalKFactor  = new Point.Builder()
                                                     .setDisplayName(equipDis+"-DCWB-"+"chilledWaterProportionalKFactor  ")
                                                     .setSiteRef(siteRef)
                                                     .setEquipRef(equipRef).setHisInterpolate("cov")
                                                     .addMarker("tuner").addMarker("dcwb").addMarker("writable").addMarker("his")
                                                     .addMarker("pgain").addMarker("sp").addMarker("chilled").addMarker("water")
                                                     .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                     .setTz(tz)
                                                     .build();
        String pgainId = hayStack.addPoint(chilledWaterProportionalKFactor);
        BuildingTunerUtil.copyFromBuildingTuner(pgainId, TunerUtil.getQueryString(chilledWaterProportionalKFactor), hayStack);
        hayStack.writeHisValById(pgainId, HSUtil.getPriorityVal(pgainId));
    
        Point chilledWaterIntegralKFactor = new Point.Builder()
                                                .setDisplayName(equipDis+"-DCWB-"+"chilledWaterIntegralKFactor ")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipRef).setHisInterpolate("cov")
                                                .addMarker("tuner").addMarker("dcwb").addMarker("writable").addMarker("his")
                                                .addMarker("igain").addMarker("sp").addMarker("chilled").addMarker("water")
                                                .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                .setTz(tz)
                                                .build();
        String igainId = hayStack.addPoint(chilledWaterIntegralKFactor);
        BuildingTunerUtil.copyFromBuildingTuner(igainId, TunerUtil.getQueryString(chilledWaterIntegralKFactor), hayStack);
        hayStack.writeHisValById(igainId, HSUtil.getPriorityVal(igainId));
    
        Point chilledWaterTemperatureProportionalRange = new Point.Builder()
                                                             .setDisplayName(equipDis+"-DCWB-"+"chilledWaterTemperatureProportionalRange ")
                                                             .setSiteRef(siteRef)
                                                             .setEquipRef(equipRef).setHisInterpolate("cov")
                                                             .addMarker("tuner").addMarker("dcwb").addMarker("writable").addMarker("his")
                                                             .addMarker("pspread").addMarker("sp").addMarker("chilled").addMarker("water")
                                                             .setMinVal("1").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                             .setTz(tz)
                                                             .build();
        String pSpreadId = hayStack.addPoint(chilledWaterTemperatureProportionalRange);
        BuildingTunerUtil.copyFromBuildingTuner(pSpreadId, TunerUtil.getQueryString(chilledWaterTemperatureProportionalRange), hayStack);
        hayStack.writeHisValById(pSpreadId, HSUtil.getPriorityVal(pSpreadId));
    
        Point integralTimeout = new Point.Builder()
                                    .setDisplayName(equipDis+"-DCWB-"+"temperatureIntegralTime ")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef).setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker("dcwb").addMarker("writable").addMarker("his")
                                    .addMarker("itimeout").addMarker("sp").addMarker("chilled").addMarker("water")
                                    .setUnit("m")
                                    .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                    .setTz(tz)
                                    .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        BuildingTunerUtil.copyFromBuildingTuner(iTimeoutId, TunerUtil.getQueryString(integralTimeout), hayStack);
        hayStack.writeHisValById(iTimeoutId, HSUtil.getPriorityVal(iTimeoutId));
    
        Point adaptiveComfortThresholdMargin = new Point.Builder()
                                                   .setDisplayName(equipDis+"-DCWB-"+"adaptiveComfortThresholdMargin ")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipRef).setHisInterpolate("cov")
                                                   .addMarker("tuner").addMarker("dcwb").addMarker("writable").addMarker("his")
                                                   .addMarker("adaptive").addMarker("comfort").addMarker("threshold").addMarker("margin")
                                                   .addMarker("sp").addMarker("chilled").addMarker("water")
                                                   .setUnit("m")
                                                   .setMinVal("1").setMaxVal("15").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                   .setTz(tz)
                                                   .build();
        String adaptiveComfortThresholdMarginId = hayStack.addPoint(adaptiveComfortThresholdMargin);
        BuildingTunerUtil.copyFromBuildingTuner(adaptiveComfortThresholdMarginId, TunerUtil.getQueryString(adaptiveComfortThresholdMargin), hayStack);
        hayStack.writeHisValById(adaptiveComfortThresholdMarginId, HSUtil.getPriorityVal(adaptiveComfortThresholdMarginId));
    }
}
