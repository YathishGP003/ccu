package a75f.io.logic.tuners;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;

public class OAOTuners
{
    
    public static void addDefaultTuners(String equipDis, String siteRef, String equipRef, String tz) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Point co2DamperOpeningRate = new Point.Builder()
                                             .setDisplayName(equipDis+"-OAO-"+"co2DamperOpeningRate")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipRef).setHisInterpolate("cov")
                                             .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                                             .addMarker("co2").addMarker("damper").addMarker("opening").addMarker("rate")
                                             .setMinVal("0").setMaxVal("200").setIncrementVal("10").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                                             .setTz(tz)
                                             .build();
        String co2DamperOpeningRateId = hayStack.addPoint(co2DamperOpeningRate);
        hayStack.writePoint(co2DamperOpeningRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_CO2_DAMPER_OPENING_RATE, 0);
        hayStack.writeHisValById(co2DamperOpeningRateId, TunerConstants.OAO_CO2_DAMPER_OPENING_RATE);
        
        Point enthalpyDuctCompensationOffset = new Point.Builder()
                                                       .setDisplayName(equipDis+"-OAO-"+"enthalpyDuctCompensationOffset")
                                                       .setSiteRef(siteRef)
                                                       .setEquipRef(equipRef).setHisInterpolate("cov")
                                                       .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                                                       .addMarker("enthalpy").addMarker("duct").addMarker("compensation").addMarker("offset")
                                                       .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                                                       .setTz(tz)
                                                       .build();
        String enthalpyDuctCompensationOffsetId = hayStack.addPoint(enthalpyDuctCompensationOffset);
        hayStack.writePoint(enthalpyDuctCompensationOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_ENTHALPY_DUCT_COMPENSATION_OFFSET, 0);
        hayStack.writeHisValById(enthalpyDuctCompensationOffsetId, TunerConstants.OAO_ENTHALPY_DUCT_COMPENSATION_OFFSET);
        
        Point economizingMinTemperature = new Point.Builder()
                                                  .setDisplayName(equipDis+"-OAO-"+"economizingMinTemperature")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                                  .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                                                  .addMarker("economizing").addMarker("min").addMarker("temp")
                                                  .setMinVal("-50").setMaxVal("80").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                                                  .setUnit("\u00B0F")
                                                  .setTz(tz)
                                                  .build();
        String economizingMinTemperatureId = hayStack.addPoint(economizingMinTemperature);
        hayStack.writePoint(economizingMinTemperatureId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_ECONOMIZING_MIN_TEMP, 0);
        hayStack.writeHisValById(economizingMinTemperatureId, TunerConstants.OAO_ECONOMIZING_MIN_TEMP);
        
        Point economizingMaxTemperature = new Point.Builder()
                                                  .setDisplayName(equipDis+"-OAO-"+"economizingMaxTemperature")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                                  .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                                                  .addMarker("economizing").addMarker("max").addMarker("temp")
                                                  .setMinVal("-50").setMaxVal("120").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                                                  .setUnit("\u00B0F")
                                                  .setTz(tz)
                                                  .build();
        String economizingMaxTemperatureId = hayStack.addPoint(economizingMaxTemperature);
        hayStack.writePoint(economizingMaxTemperatureId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_ECONOMIZING_MAX_TEMP, 0);
        hayStack.writeHisValById(economizingMaxTemperatureId, TunerConstants.OAO_ECONOMIZING_MAX_TEMP);
        
        Point economizingMinHumidity = new Point.Builder()
                                               .setDisplayName(equipDis+"-OAO-"+"economizingMinHumidity")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef).setHisInterpolate("cov")
                                               .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                                               .addMarker("economizing").addMarker("min").addMarker("humidity")
                                               .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                                               .setUnit("%")
                                               .setTz(tz)
                                               .build();
        String economizingMinHumidityId = hayStack.addPoint(economizingMinHumidity);
        hayStack.writePoint(economizingMinHumidityId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_ECONOMIZING_MIN_HUMIDITY, 0);
        hayStack.writeHisValById(economizingMinHumidityId, TunerConstants.OAO_ECONOMIZING_MIN_HUMIDITY);
        
        Point economizingMaxHumidity = new Point.Builder()
                                               .setDisplayName(equipDis+"-OAO-"+"economizingMaxHumidity")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef).setHisInterpolate("cov")
                                               .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                                               .addMarker("economizing").addMarker("max").addMarker("humidity")
                                               .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                                               .setUnit("%")
                                               .setTz(tz)
                                               .build();
        String economizingMaxHumidityId = hayStack.addPoint(economizingMaxHumidity);
        hayStack.writePoint(economizingMaxHumidityId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_ECONOMIZING_MAX_HUMIDITY, 0);
        hayStack.writeHisValById(economizingMaxHumidityId, TunerConstants.OAO_ECONOMIZING_MAX_HUMIDITY);
        
        Point outsideDamperMixedAirTarget  = new Point.Builder()
                                                     .setDisplayName(equipDis+"-OAO-"+"outsideDamperMixedAirTarget")
                                                     .setSiteRef(siteRef)
                                                     .setEquipRef(equipRef).setHisInterpolate("cov")
                                                     .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                                                     .addMarker("outside").addMarker("damper").addMarker("mat").addMarker("target")
                                                     .setMinVal("30").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                                                     .setUnit("\u00B0F")
                                                     .setTz(tz)
                                                     .build();
        String outsideDamperMixedAirTargetId = hayStack.addPoint(outsideDamperMixedAirTarget);
        hayStack.writePoint(outsideDamperMixedAirTargetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_OA_DAMPER_MAT_TARGET, 0);
        hayStack.writeHisValById(outsideDamperMixedAirTargetId, TunerConstants.OAO_OA_DAMPER_MAT_TARGET);
        
        Point outsideDamperMixedAirMinimum  = new Point.Builder()
                                                      .setDisplayName(equipDis+"-OAO-"+"outsideDamperMixedAirMinimum")
                                                      .setSiteRef(siteRef)
                                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                                      .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                                                      .addMarker("outside").addMarker("damper").addMarker("mat").addMarker("min")
                                                      .setMinVal("30").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                                                      .setUnit("\u00B0F")
                                                      .setTz(tz)
                                                      .build();
        String outsideDamperMixedAirMinimumId = hayStack.addPoint(outsideDamperMixedAirMinimum);
        hayStack.writePoint(outsideDamperMixedAirMinimumId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_OA_DAMPER_MAT_MIN, 0);
        hayStack.writeHisValById(outsideDamperMixedAirMinimumId, TunerConstants.OAO_OA_DAMPER_MAT_MIN);
        
        Point economizingToMainCoolingLoopMap  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-OAO-"+"economizingToMainCoolingLoopMap")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                                                         .addMarker("economizing").addMarker("main").addMarker("cooling").addMarker("loop").addMarker("map")
                                                         .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String economizingToMainCoolingLoopMapId = hayStack.addPoint(economizingToMainCoolingLoopMap);
        hayStack.writePoint(economizingToMainCoolingLoopMapId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_ECONOMIZING_TO_MAIN_COOLING_LOOP_MAP, 0);
        hayStack.writeHisValById(economizingToMainCoolingLoopMapId, TunerConstants.OAO_ECONOMIZING_TO_MAIN_COOLING_LOOP_MAP);
        updateNewTuners( siteRef,equipRef, equipDis,tz,true);
    }

    public static void updateNewTuners(String siteRef, String equipRef, String equipDis, String tz,boolean isNewSite){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        if(isNewSite || !verifyPointsAvailability("default","prePurge and runtime",equipRef)) {
            Point smartPrePurgeRuntime  = new Point.Builder()
                    .setDisplayName(equipDis+"-OAO-"+"systemPrePurgeRuntimeTuner")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipRef).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("prePurge").addMarker("runtime")
                    .setMinVal("0").setMaxVal("360").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("min")
                    .setTz(tz)
                    .build();
            String smartPrePurgeRuntimeId = hayStack.addPoint(smartPrePurgeRuntime);
            hayStack.writePoint(smartPrePurgeRuntimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_SMART_PURGE_RUNTIME_DEFAULT, 0);
            hayStack.writeHisValById(smartPrePurgeRuntimeId, TunerConstants.OAO_SMART_PURGE_RUNTIME_DEFAULT);
        }

        if(isNewSite || !verifyPointsAvailability("default","prePurge and occupied and time and offset",equipRef)) {
            Point smartPrePurgeStartTimeOffset = new Point.Builder()
                    .setDisplayName(equipDis + "-OAO-" + "systemPrePurgeOccupiedTimeOffsetTuner")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipRef).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("prePurge").addMarker("occupied").addMarker("time").addMarker("offset")
                    .setMinVal("0").setMaxVal("360").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("min")
                    .setTz(tz)
                    .build();
            String smartPrePurgeStartTimeOffsetId = hayStack.addPoint(smartPrePurgeStartTimeOffset);
            hayStack.writePoint(smartPrePurgeStartTimeOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_SMART_PRE_PURGE_START_TIME_OFFSET, 0);
            hayStack.writeHisValById(smartPrePurgeStartTimeOffsetId, TunerConstants.OAO_SMART_PRE_PURGE_START_TIME_OFFSET);
        }

        if(isNewSite || !verifyPointsAvailability("default","prePurge and fan and speed",equipRef)) {
            Point smartPrePurgeFanSpeed = new Point.Builder()
                    .setDisplayName(equipDis + "-OAO-" + "systemPrePurgeFanSpeedTuner")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipRef).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("prePurge").addMarker("fan").addMarker("speed")
                    .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String smartPrePurgeFanSpeedId = hayStack.addPoint(smartPrePurgeFanSpeed);
            hayStack.writePoint(smartPrePurgeFanSpeedId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_SMART_PURGE_FAN_SPEED, 0);
            hayStack.writeHisValById(smartPrePurgeFanSpeedId, TunerConstants.OAO_SMART_PURGE_FAN_SPEED);
        }

        if(isNewSite || !verifyPointsAvailability("default","postPurge and runtime",equipRef)) {
            Point smartPostPurgeRuntime = new Point.Builder()
                    .setDisplayName(equipDis + "-OAO-" + "systemPostPurgeRuntimeTuner")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipRef).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("postPurge").addMarker("runtime")
                    .setMinVal("0").setMaxVal("360").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("min")
                    .setTz(tz)
                    .build();
            String smartPostPurgeRuntimeId = hayStack.addPoint(smartPostPurgeRuntime);
            hayStack.writePoint(smartPostPurgeRuntimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_SMART_PURGE_RUNTIME_DEFAULT, 0);
            hayStack.writeHisValById(smartPostPurgeRuntimeId, TunerConstants.OAO_SMART_PURGE_RUNTIME_DEFAULT);
        }

        if(isNewSite || !verifyPointsAvailability("default","postPurge and occupied and time and offset",equipRef)) {
            Point smartPostPurgeStartTimeOffset = new Point.Builder()
                    .setDisplayName(equipDis + "-OAO-" + "systemPostPurgeOccupiedTimeOffsetTuner")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipRef).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("postPurge").addMarker("occupied").addMarker("time").addMarker("offset")
                    .setMinVal("0").setMaxVal("360").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("min")
                    .setTz(tz)
                    .build();
            String smartPostPurgeStartTimeOffsetId = hayStack.addPoint(smartPostPurgeStartTimeOffset);
            hayStack.writePoint(smartPostPurgeStartTimeOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_SMART_POST_PURGE_START_TIME_OFFSET, 0);
            hayStack.writeHisValById(smartPostPurgeStartTimeOffsetId, TunerConstants.OAO_SMART_POST_PURGE_START_TIME_OFFSET);
        }

        if(isNewSite || !verifyPointsAvailability("default","postPurge and fan and speed",equipRef)) {
            Point smartPostPurgeFanSpeed = new Point.Builder()
                    .setDisplayName(equipDis + "-OAO-" + "systemPostPurgeFanSpeedTuner")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipRef).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("postPurge").addMarker("fan").addMarker("speed")
                    .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String smartPostPurgeFanSpeedId = hayStack.addPoint(smartPostPurgeFanSpeed);
            hayStack.writePoint(smartPostPurgeFanSpeedId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_SMART_PURGE_FAN_SPEED, 0);
            hayStack.writeHisValById(smartPostPurgeFanSpeedId, TunerConstants.OAO_SMART_PURGE_FAN_SPEED);
        }

        if(isNewSite || !verifyPointsAvailability("default","purge and dab and fan and loop and output and min",equipRef)) {
            Point smartPurgeDabFanLoopOutput = new Point.Builder()
                    .setDisplayName(equipDis + "-OAO-" + "systemPurgeDabFanLoopOutput")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipRef).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("purge").addMarker("dab").addMarker("fan").addMarker("loop").addMarker("output").addMarker("min")
                    .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String smartPurgeDabFanLoopOutputId = hayStack.addPoint(smartPurgeDabFanLoopOutput);
            hayStack.writePoint(smartPurgeDabFanLoopOutputId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_SMART_PURGE_FAN_LOOP_OUTPUT_MIN, 0);
            hayStack.writeHisValById(smartPurgeDabFanLoopOutputId, TunerConstants.OAO_SMART_PURGE_FAN_LOOP_OUTPUT_MIN);
        }

        if(isNewSite || !verifyPointsAvailability("default","purge and vav and fan and loop and output and min",equipRef)) {
            Point smartPurgeVavFanLoopOutput = new Point.Builder()
                    .setDisplayName(equipDis + "-OAO-" + "systemPurgeVavFanLoopOutput")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipRef).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("purge").addMarker("oao").addMarker("vav").addMarker("fan").addMarker("loop").addMarker("output").addMarker("min")
                    .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String smartPurgeVavFanLoopOutputId = hayStack.addPoint(smartPurgeVavFanLoopOutput);
            hayStack.writePoint(smartPurgeVavFanLoopOutputId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_SMART_PURGE_FAN_LOOP_OUTPUT_MIN, 0);
            hayStack.writeHisValById(smartPurgeVavFanLoopOutputId, TunerConstants.OAO_SMART_PURGE_FAN_LOOP_OUTPUT_MIN);
        }

        if(isNewSite || !verifyPointsAvailability("default","purge and dab and damper and pos and multiplier and min",equipRef)) {
            Point smartPurgeDabDamperMinOpenMultiplier = new Point.Builder()
                    .setDisplayName(equipDis + "-OAO-" + "systemPurgeDabDamperMinOpenMultiplier")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipRef).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("purge").addMarker("dab").addMarker("damper").addMarker("pos").addMarker("multiplier").addMarker("min")
                    .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String smartPurgeDabDamperMinOpenMultiplierId = hayStack.addPoint(smartPurgeDabDamperMinOpenMultiplier);
            hayStack.writePoint(smartPurgeDabDamperMinOpenMultiplierId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_SMART_PURGE_MIN_DAMPER_OPEN_MULTIPLIER, 0);
            hayStack.writeHisValById(smartPurgeDabDamperMinOpenMultiplierId, TunerConstants.OAO_SMART_PURGE_MIN_DAMPER_OPEN_MULTIPLIER);
        }

        if(isNewSite || !verifyPointsAvailability("default","purge and vav and damper and pos and multiplier and min",equipRef)) {
            Point smartPurgeVavDamperMinOpenMultiplier = new Point.Builder()
                    .setDisplayName(equipDis + "-OAO-" + "systemPurgeVavDamperMinOpenMultiplier")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipRef).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("purge").addMarker("vav").addMarker("damper").addMarker("pos").addMarker("multiplier").addMarker("min")
                    .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String smartPurgeVavDamperMinOpenMultiplierId = hayStack.addPoint(smartPurgeVavDamperMinOpenMultiplier);
            hayStack.writePoint(smartPurgeVavDamperMinOpenMultiplierId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_SMART_PURGE_MIN_DAMPER_OPEN_MULTIPLIER, 0);
            hayStack.writeHisValById(smartPurgeVavDamperMinOpenMultiplierId, TunerConstants.OAO_SMART_PURGE_MIN_DAMPER_OPEN_MULTIPLIER);
        }

    }
    private static boolean verifyPointsAvailability(String defaulttuner, String tags, String equipref){
        HashMap verifyablePoint = CCUHsApi.getInstance().read("point and tuner and "+defaulttuner+" and oao and "+tags+" and equipRef == \"" + equipref + "\"");
        if (verifyablePoint != null && verifyablePoint.size() > 0) {
            if(defaulttuner.equals("not default")) {
                Point p = new Point.Builder().setHashMap(verifyablePoint).build();
                if (!p.getMarkers().contains("system"))
                    p.getMarkers().add("system");
            }
            return true;
        }
        return false;
    }
    public static void updateOaoSystemTuners(String siteRef, String equipref, String equipdis, String tz) {

        CCUHsApi hayStack = CCUHsApi.getInstance();
        if (!verifyPointsAvailability("not default","co2 and damper and opening and rate and multiplier and min",equipref)) {
            Point co2DamperOpeningRate = new Point.Builder()
                    .setDisplayName(equipdis + "-" + "co2DamperOpeningRate")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("co2").addMarker("damper").addMarker("opening").addMarker("rate").addMarker("system")
                    .setMinVal("0").setMaxVal("200").setIncrementVal("10").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setTz(tz)
                    .build();
            String co2DamperOpeningRateId = hayStack.addPoint(co2DamperOpeningRate);
            HashMap co2DamperOpeningRatePoint = hayStack.read("point and tuner and default and oao and co2 and damper and opening and rate");
            ArrayList<HashMap> co2DamperOpeningRatePointArr = hayStack.readPoint(co2DamperOpeningRatePoint.get("id").toString());
            for (HashMap valMap : co2DamperOpeningRatePointArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(co2DamperOpeningRateId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(co2DamperOpeningRateId, HSUtil.getPriorityVal(co2DamperOpeningRateId));
        }
        if (!verifyPointsAvailability("not default","enthalpy and duct and compensation and offset",equipref)) {
            Point enthalpyDuctCompensationOffset = new Point.Builder()
                    .setDisplayName(equipdis + "-" + "enthalpyDuctCompensationOffset")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("enthalpy").addMarker("duct").addMarker("compensation").addMarker("offset").addMarker("system")
                    .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setTz(tz)
                    .build();
            String enthalpyDuctCompensationOffsetId = hayStack.addPoint(enthalpyDuctCompensationOffset);
            HashMap enthalpyDuctCompensationOffsetPoint = hayStack.read("point and tuner and default and oao and enthalpy and duct and compensation and offset");
            ArrayList<HashMap> enthalpyDuctCompensationOffsetPointArr = hayStack.readPoint(enthalpyDuctCompensationOffsetPoint.get("id").toString());
            for (HashMap valMap : enthalpyDuctCompensationOffsetPointArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(enthalpyDuctCompensationOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(enthalpyDuctCompensationOffsetId, HSUtil.getPriorityVal(enthalpyDuctCompensationOffsetId));
        }
        if (!verifyPointsAvailability("not default","enthalpy and duct and compensation and offset",equipref)) {
            Point economizingMinTemperature = new Point.Builder()
                    .setDisplayName(equipdis + "-" + "economizingMinTemperature")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("economizing").addMarker("min").addMarker("temp").addMarker("system")
                    .setMinVal("-50").setMaxVal("80").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("\u00B0F")
                    .setTz(tz)
                    .build();
            String economizingMinTemperatureId = hayStack.addPoint(economizingMinTemperature);
            HashMap economizingMinTemperaturePoint = hayStack.read("point and tuner and default and oao and economizing and min and temp");
            ArrayList<HashMap> economizingMinTemperaturePointArr = hayStack.readPoint(economizingMinTemperaturePoint.get("id").toString());
            for (HashMap valMap : economizingMinTemperaturePointArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(economizingMinTemperatureId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(economizingMinTemperatureId, HSUtil.getPriorityVal(economizingMinTemperatureId));
        }
        if(!verifyPointsAvailability("not default","economizing and max and temp",equipref)) {
            Point economizingMaxTemperature = new Point.Builder()
                    .setDisplayName(equipdis + "-" + "economizingMaxTemperature")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("economizing").addMarker("max").addMarker("temp").addMarker("system")
                    .setMinVal("-50").setMaxVal("120").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("\u00B0F")
                    .setTz(tz)
                    .build();
            String economizingMaxTemperatureId = hayStack.addPoint(economizingMaxTemperature);
            HashMap economizingMaxTemperaturePoint = hayStack.read("point and tuner and default and oao and economizing and max and temp");
            ArrayList<HashMap> economizingMaxTemperaturePointArr = hayStack.readPoint(economizingMaxTemperaturePoint.get("id").toString());
            for (HashMap valMap : economizingMaxTemperaturePointArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(economizingMaxTemperatureId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(economizingMaxTemperatureId, HSUtil.getPriorityVal(economizingMaxTemperatureId));
        }
        if(!verifyPointsAvailability("not default","economizing and min and humidity",equipref)) {
            Point economizingMinHumidity = new Point.Builder()
                    .setDisplayName(equipdis + "-" + "economizingMinHumidity")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("economizing").addMarker("min").addMarker("humidity").addMarker("system")
                    .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String economizingMinHumidityId = hayStack.addPoint(economizingMinHumidity);
            HashMap economizingMinHumidityPoint = hayStack.read("point and tuner and default and oao and economizing and min and humidity");
            ArrayList<HashMap> economizingMinHumidityPointArr = hayStack.readPoint(economizingMinHumidityPoint.get("id").toString());
            for (HashMap valMap : economizingMinHumidityPointArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(economizingMinHumidityId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(economizingMinHumidityId, HSUtil.getPriorityVal(economizingMinHumidityId));
        }
        if(!verifyPointsAvailability("not default","economizing and max and humidity",equipref)) {
            Point economizingMaxHumidity = new Point.Builder()
                    .setDisplayName(equipdis + "-" + "economizingMaxHumidity")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("economizing").addMarker("max").addMarker("humidity").addMarker("system")
                    .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String economizingMaxHumidityId = hayStack.addPoint(economizingMaxHumidity);
            HashMap economizingMaxHumidityPoint = hayStack.read("point and tuner and default and oao and economizing and max and humidity");
            ArrayList<HashMap> economizingMaxHumidityPointArr = hayStack.readPoint(economizingMaxHumidityPoint.get("id").toString());
            for (HashMap valMap : economizingMaxHumidityPointArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(economizingMaxHumidityId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(economizingMaxHumidityId, HSUtil.getPriorityVal(economizingMaxHumidityId));
        }
        if(!verifyPointsAvailability("not default","outside and damper and mat and target",equipref)) {
            Point outsideDamperMixedAirTarget = new Point.Builder()
                    .setDisplayName(equipdis + "-" + "outsideDamperMixedAirTarget")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("outside").addMarker("damper").addMarker("mat").addMarker("target").addMarker("system")
                    .setMinVal("30").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("\u00B0F")
                    .setTz(tz)
                    .build();
            String outsideDamperMixedAirTargetId = hayStack.addPoint(outsideDamperMixedAirTarget);
            HashMap outsideDamperMixedAirTargetPoint = hayStack.read("point and tuner and default and oao and outside and damper and mat and target");
            ArrayList<HashMap> outsideDamperMixedAirTargetPointArr = hayStack.readPoint(outsideDamperMixedAirTargetPoint.get("id").toString());
            for (HashMap valMap : outsideDamperMixedAirTargetPointArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(outsideDamperMixedAirTargetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(outsideDamperMixedAirTargetId, HSUtil.getPriorityVal(outsideDamperMixedAirTargetId));
        }
        if(!verifyPointsAvailability("not default","outside and damper and mat and min",equipref)) {
            Point outsideDamperMixedAirMinimum = new Point.Builder()
                    .setDisplayName(equipdis + "-" + "outsideDamperMixedAirMinimum")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("outside").addMarker("damper").addMarker("mat").addMarker("minimum").addMarker("system")
                    .setMinVal("30").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("\u00B0F")
                    .setTz(tz)
                    .build();
            String outsideDamperMixedAirMinimumId = hayStack.addPoint(outsideDamperMixedAirMinimum);
            HashMap outsideDamperMixedAirMinimumPoint = hayStack.read("point and tuner and default and oao and outside and damper and mat and min");
            ArrayList<HashMap> outsideDamperMixedAirMinimumPointArr = hayStack.readPoint(outsideDamperMixedAirMinimumPoint.get("id").toString());
            for (HashMap valMap : outsideDamperMixedAirMinimumPointArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(outsideDamperMixedAirMinimumId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(outsideDamperMixedAirMinimumId, HSUtil.getPriorityVal(outsideDamperMixedAirMinimumId));
        }
        if(!verifyPointsAvailability("not default","economizing and main and cooling and loop and map",equipref)) {
            Point economizingToMainCoolingLoopMap = new Point.Builder()
                    .setDisplayName(equipdis + "-" + "economizingToMainCoolingLoopMap")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his").addMarker("system")
                    .addMarker("economizing").addMarker("main").addMarker("cooling").addMarker("loop").addMarker("map")
                    .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String economizingToMainCoolingLoopMapId = hayStack.addPoint(economizingToMainCoolingLoopMap);
            HashMap economizingToMainCoolingLoopMapPoint = hayStack.read("point and tuner and default and oao and economizing and main and cooling and loop and map");
            ArrayList<HashMap> economizingToMainCoolingLoopMapPointArr = hayStack.readPoint(economizingToMainCoolingLoopMapPoint.get("id").toString());
            for (HashMap valMap : economizingToMainCoolingLoopMapPointArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(economizingToMainCoolingLoopMapId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(economizingToMainCoolingLoopMapId, HSUtil.getPriorityVal(economizingToMainCoolingLoopMapId));
        }


        if(!verifyPointsAvailability("not default","prePurge and runtime",equipref)) {
            Point smartPrePurgeRuntime = new Point.Builder()
                    .setDisplayName(equipdis + "-OAO-" + "systemPrePurgeRuntimeTuner")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("prePurge").addMarker("runtime").addMarker("system")
                    .setMinVal("0").setMaxVal("360").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("min")
                    .setTz(tz)
                    .build();
            String smartPrePurgeRuntimeId = hayStack.addPoint(smartPrePurgeRuntime);
            HashMap smartPrePurgeRuntimePoint = hayStack.read("point and tuner and default and oao and prePurge and runtime");
            ArrayList<HashMap> smartPrePurgeRuntimePointArr = hayStack.readPoint(smartPrePurgeRuntimePoint.get("id").toString());
            for (HashMap valMap : smartPrePurgeRuntimePointArr) {
                if (valMap.get("val") != null) {
                    hayStack.pointWrite(HRef.copy(smartPrePurgeRuntimeId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(smartPrePurgeRuntimeId, HSUtil.getPriorityVal(smartPrePurgeRuntimeId));
        }
        if(!verifyPointsAvailability("not default","prePurge and occupied and time and offset",equipref)) {
            Point smartPrePurgeStartTimeOffset = new Point.Builder()
                    .setDisplayName(equipdis + "-OAO-" + "systemPrePurgeOccupiedTimeOffsetTuner")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("system").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("prePurge").addMarker("occupied").addMarker("time").addMarker("offset")
                    .setMinVal("0").setMaxVal("360").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("min")
                    .setTz(tz)
                    .build();
            String smartPrePurgeStartTimeOffsetId = hayStack.addPoint(smartPrePurgeStartTimeOffset);
            HashMap smartPrePurgeStartTimeOffsetPoint = hayStack.read("point and tuner and default and oao and prePurge and occupied and time and offset");
            ArrayList<HashMap> smartPrePurgeStartTimeOffsetPointArr = hayStack.readPoint(smartPrePurgeStartTimeOffsetPoint.get("id").toString());
            for (HashMap valMap : smartPrePurgeStartTimeOffsetPointArr) {
                if (valMap.get("val") != null) {
                    hayStack.pointWrite(HRef.copy(smartPrePurgeStartTimeOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(smartPrePurgeStartTimeOffsetId, HSUtil.getPriorityVal(smartPrePurgeStartTimeOffsetId));
        }
        if(!verifyPointsAvailability("not default","prePurge and fan and speed",equipref)) {
            Point smartPrePurgeFanSpeed = new Point.Builder()
                    .setDisplayName(equipdis + "-OAO-" + "systemPrePurgeFanSpeedTuner")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("system").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("prePurge").addMarker("fan").addMarker("speed")
                    .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String smartPrePurgeFanSpeedId = hayStack.addPoint(smartPrePurgeFanSpeed);
            HashMap smartPrePurgeFanSpeedPoint = hayStack.read("point and tuner and default and oao and prePurge and fan and speed");
            ArrayList<HashMap> smartPrePurgeFanSpeedPointArr = hayStack.readPoint(smartPrePurgeFanSpeedPoint.get("id").toString());
            for (HashMap valMap : smartPrePurgeFanSpeedPointArr) {
                if (valMap.get("val") != null) {
                    hayStack.pointWrite(HRef.copy(smartPrePurgeFanSpeedId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(smartPrePurgeFanSpeedId, HSUtil.getPriorityVal(smartPrePurgeFanSpeedId));
        }
        if(!verifyPointsAvailability("not default","postPurge and runtime",equipref)) {
            Point smartPostPurgeRuntime = new Point.Builder()
                    .setDisplayName(equipdis + "-OAO-" + "systemPostPurgeRuntimeTuner")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("system").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("postPurge").addMarker("runtime")
                    .setMinVal("0").setMaxVal("360").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("min")
                    .setTz(tz)
                    .build();
            String smartPostPurgeRuntimeId = hayStack.addPoint(smartPostPurgeRuntime);
            HashMap smartPostPurgeRuntimePoint = hayStack.read("point and tuner and default and oao and postPurge and runtime");
            ArrayList<HashMap> smartPostPurgeRuntimePointArr = hayStack.readPoint(smartPostPurgeRuntimePoint.get("id").toString());
            for (HashMap valMap : smartPostPurgeRuntimePointArr) {
                if (valMap.get("val") != null) {
                    hayStack.pointWrite(HRef.copy(smartPostPurgeRuntimeId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(smartPostPurgeRuntimeId, HSUtil.getPriorityVal(smartPostPurgeRuntimeId));
        }
        if(!verifyPointsAvailability("not default","postPurge and occupied and time and offset",equipref)) {
            Point smartPostPurgeStartTimeOffset = new Point.Builder()
                    .setDisplayName(equipdis + "-OAO-" + "systemPostPurgeOccupiedTimeOffsetTuner")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("system").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("postPurge").addMarker("occupied").addMarker("time").addMarker("offset")
                    .setMinVal("0").setMaxVal("360").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("min")
                    .setTz(tz)
                    .build();
            String smartPostPurgeStartTimeOffsetId = hayStack.addPoint(smartPostPurgeStartTimeOffset);
            HashMap smartPostPurgeStartTimeOffsetPoint = hayStack.read("point and tuner and default and oao and postPurge and occupied and time and offset");
            ArrayList<HashMap> smartPostPurgeStartTimeOffsetPointArr = hayStack.readPoint(smartPostPurgeStartTimeOffsetPoint.get("id").toString());
            for (HashMap valMap : smartPostPurgeStartTimeOffsetPointArr) {
                if (valMap.get("val") != null) {
                    hayStack.pointWrite(HRef.copy(smartPostPurgeStartTimeOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(smartPostPurgeStartTimeOffsetId, HSUtil.getPriorityVal(smartPostPurgeStartTimeOffsetId));
        }
        if(!verifyPointsAvailability("not default","postPurge and fan and speed",equipref)) {
            Point smartPostPurgeFanSpeed = new Point.Builder()
                    .setDisplayName(equipdis + "-OAO-" + "systemPostPurgeFanSpeedTuner")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("system").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("postPurge").addMarker("fan").addMarker("speed")
                    .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String smartPostPurgeFanSpeedId = hayStack.addPoint(smartPostPurgeFanSpeed);
            HashMap smartPostPurgeFanSpeedPoint = hayStack.read("point and tuner and default and oao and postPurge and fan and speed");
            ArrayList<HashMap> smartPostPurgeFanSpeedPointArr = hayStack.readPoint(smartPostPurgeFanSpeedPoint.get("id").toString());
            for (HashMap valMap : smartPostPurgeFanSpeedPointArr) {
                if (valMap.get("val") != null) {
                    hayStack.pointWrite(HRef.copy(smartPostPurgeFanSpeedId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(smartPostPurgeFanSpeedId, HSUtil.getPriorityVal(smartPostPurgeFanSpeedId));
        }
        if(!verifyPointsAvailability("not default","purge and dab and fan and loop and output and min",equipref)) {
            Point smartPurgeDabFanLoopOutput = new Point.Builder()
                    .setDisplayName(equipdis + "-OAO-" + "systemPurgeDabFanLoopOutput")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("system").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("purge").addMarker("dab").addMarker("fan").addMarker("loop").addMarker("output").addMarker("min")
                    .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String smartPurgeDabFanLoopOutputId = hayStack.addPoint(smartPurgeDabFanLoopOutput);
            HashMap smartPurgeDabFanLoopOutputPoint = hayStack.read("point and tuner and default and oao and purge and dab and fan and loop and output and min");
            ArrayList<HashMap> smartPurgeDabFanLoopOutputPointArr = hayStack.readPoint(smartPurgeDabFanLoopOutputPoint.get("id").toString());
            for (HashMap valMap : smartPurgeDabFanLoopOutputPointArr) {
                if (valMap.get("val") != null) {
                    hayStack.pointWrite(HRef.copy(smartPurgeDabFanLoopOutputId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(smartPurgeDabFanLoopOutputId, HSUtil.getPriorityVal(smartPurgeDabFanLoopOutputId));
        }
        if(!verifyPointsAvailability("not default","purge and vav and fan and loop and output and min",equipref)) {
            Point smartPurgeVavFanLoopOutput = new Point.Builder()
                    .setDisplayName(equipdis + "-OAO-" + "systemPurgeVavFanLoopOutput")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("system").addMarker("oao").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("purge").addMarker("vav").addMarker("fan").addMarker("loop").addMarker("output").addMarker("min")
                    .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String smartPurgeVavFanLoopOutputId = hayStack.addPoint(smartPurgeVavFanLoopOutput);
            HashMap smartPurgeVavFanLoopOutputPoint = hayStack.read("point and tuner and default and purge and vav and fan and loop and output and min");
            ArrayList<HashMap> smartPurgeVavFanLoopOutputPointArr = hayStack.readPoint(smartPurgeVavFanLoopOutputPoint.get("id").toString());
            for (HashMap valMap : smartPurgeVavFanLoopOutputPointArr) {
                if (valMap.get("val") != null) {
                    hayStack.pointWrite(HRef.copy(smartPurgeVavFanLoopOutputId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(smartPurgeVavFanLoopOutputId, HSUtil.getPriorityVal(smartPurgeVavFanLoopOutputId));
        }
        if(!verifyPointsAvailability("not default","purge and dab and damper and pos and multiplier and min",equipref)) {
            Point smartPurgeDabDamperMinOpenMultiplier = new Point.Builder()
                    .setDisplayName(equipdis + "-OAO-" + "systemPurgeDabDamperMinOpenMultiplier")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("system").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("purge").addMarker("dab").addMarker("damper").addMarker("pos").addMarker("multiplier").addMarker("min")
                    .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String smartPurgeDabDamperMinOpenMultiplierId = hayStack.addPoint(smartPurgeDabDamperMinOpenMultiplier);
            HashMap smartPurgeDabDamperMinOpenMultiplierPoint = hayStack.read("point and tuner and default and purge and dab and damper and pos and multiplier and min");
            ArrayList<HashMap> smartPurgeDabDamperMinOpenMultiplierPointArr = hayStack.readPoint(smartPurgeDabDamperMinOpenMultiplierPoint.get("id").toString());
            for (HashMap valMap : smartPurgeDabDamperMinOpenMultiplierPointArr) {
                if (valMap.get("val") != null) {
                    hayStack.pointWrite(HRef.copy(smartPurgeDabDamperMinOpenMultiplierId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(smartPurgeDabDamperMinOpenMultiplierId, HSUtil.getPriorityVal(smartPurgeDabDamperMinOpenMultiplierId));
        }
        if(!verifyPointsAvailability("not default","purge and vav and damper and pos and multiplier and min",equipref)) {

            Point smartPurgeVavDamperMinOpenMultiplier = new Point.Builder()
                    .setDisplayName(equipdis + "-OAO-" + "systemPurgeVavDamperMinOpenMultiplier")
                    .setSiteRef(siteRef)
                    .setEquipRef(equipref).setHisInterpolate("cov")
                    .addMarker("tuner").addMarker("system").addMarker("writable").addMarker("his")
                    .addMarker("sp").addMarker("purge").addMarker("vav").addMarker("damper").addMarker("pos").addMarker("multiplier").addMarker("min")
                    .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                    .setUnit("%")
                    .setTz(tz)
                    .build();
            String smartPurgeVavDamperMinOpenMultiplierId = hayStack.addPoint(smartPurgeVavDamperMinOpenMultiplier);
            HashMap smartPurgeVavDamperMinOpenMultiplierPoint = hayStack.read("point and tuner and default and purge and vav and damper and pos and multiplier and min");
            ArrayList<HashMap> smartPurgeVavDamperMinOpenMultiplierPointArr = hayStack.readPoint(smartPurgeVavDamperMinOpenMultiplierPoint.get("id").toString());
            for (HashMap valMap : smartPurgeVavDamperMinOpenMultiplierPointArr) {
                if (valMap.get("val") != null) {
                    hayStack.pointWrite(HRef.copy(smartPurgeVavDamperMinOpenMultiplierId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(smartPurgeVavDamperMinOpenMultiplierId, HSUtil.getPriorityVal(smartPurgeVavDamperMinOpenMultiplierId));
        }
    }
}
