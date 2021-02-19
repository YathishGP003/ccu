package a75f.io.logic.tuners;

import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

import static a75f.io.logic.tuners.TunerConstants.DEFAULT_MODE_CHANGEOVER_HYSTERESIS;
import static a75f.io.logic.tuners.TunerConstants.DEFAULT_STAGE_DOWN_TIMER_COUNTER;
import static a75f.io.logic.tuners.TunerConstants.DEFAULT_STAGE_UP_TIMER_COUNTER;

public class DabTuners {
    
    public static void addDefaultDabTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                           String tz){
    
        HashMap tuner = CCUHsApi.getInstance().read("point and tuner and default and dab");
        if (tuner != null && tuner.size() > 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM,"Default DAB Tuner points already exist");
            return;
        }
        CcuLog.d(L.TAG_CCU_SYSTEM,"Default DAB Tuner  does not exist. Create Now");
        Point zonePrioritySpread = new Point.Builder()
                                       .setDisplayName(equipDis+"-DAB-"+"zonePrioritySpread")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                                       .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                                       .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                       .setTz(tz)
                                       .build();
        String zonePrioritySpreadId = hayStack.addPoint(zonePrioritySpread);
        hayStack.writePoint(zonePrioritySpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_PRIORITY_SPREAD, 0);
        hayStack.writeHisValById(zonePrioritySpreadId, TunerConstants.ZONE_PRIORITY_SPREAD);

        Point zonePriorityMultiplier = new Point.Builder()
                                           .setDisplayName(equipDis+"-DAB-"+"zonePriorityMultiplier")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                                           .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                           .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                           .setTz(tz)
                                           .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        hayStack.writePoint(zonePriorityMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_PRIORITY_MULTIPLIER, 0);
        hayStack.writeHisValById(zonePriorityMultiplierId, TunerConstants.ZONE_PRIORITY_MULTIPLIER);

        Point coolingDb = new Point.Builder()
                              .setDisplayName(equipDis+"-DAB-"+"coolingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                              .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                              .setUnit("\u00B0F")
                              .setTz(tz)
                              .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        hayStack.writePoint(coolingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_COOLING_DB, 0);
        hayStack.writeHisValById(coolingDbId, TunerConstants.VAV_COOLING_DB);

        Point coolingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipDis+"-DAB-"+"coolingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                                        .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        hayStack.writePoint(coolingDbMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_COOLING_DB_MULTPLIER, 0);
        hayStack.writeHisValById(coolingDbMultiplierId, TunerConstants.VAV_COOLING_DB_MULTPLIER);

        Point heatingDb = new Point.Builder()
                              .setDisplayName(equipDis+"-DAB-"+"heatingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                              .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                              .setUnit("\u00B0F")
                              .setTz(tz)
                              .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        hayStack.writePoint(heatingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_HEATING_DB, 0);
        hayStack.writeHisValById(heatingDbId, TunerConstants.VAV_HEATING_DB);

        Point heatingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipDis+"-DAB-"+"heatingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                                        .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        hayStack.writePoint(heatingDbMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_HEATING_DB_MULTIPLIER, 0);
        hayStack.writeHisValById(heatingDbMultiplierId, TunerConstants.VAV_HEATING_DB_MULTIPLIER);

        Point propGain = new Point.Builder()
                             .setDisplayName(equipDis+"-DAB-"+"proportionalKFactor ")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                             .addMarker("pgain").addMarker("sp")
                             .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                             .setTz(tz)
                             .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePoint(pgainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_GAIN, 0);
        hayStack.writeHisValById(pgainId, TunerConstants.VAV_PROPORTIONAL_GAIN);

        Point integralGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-DAB-"+"integralKFactor ")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                                 .addMarker("igain").addMarker("sp")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                 .setTz(tz)
                                 .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePoint(igainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_GAIN, 0);
        hayStack.writeHisValById(igainId, TunerConstants.VAV_INTEGRAL_GAIN);

        Point propSpread = new Point.Builder()
                               .setDisplayName(equipDis+"-DAB-"+"temperatureProportionalRange ")
                               .setSiteRef(siteRef)
                               .setEquipRef(equipRef).setHisInterpolate("cov")
                               .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                               .addMarker("pspread").addMarker("sp")
                               .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                               .setTz(tz)
                               .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        hayStack.writePoint(pSpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_SPREAD, 0);
        hayStack.writeHisValById(pSpreadId, TunerConstants.VAV_PROPORTIONAL_SPREAD);

        Point integralTimeout = new Point.Builder()
                                    .setDisplayName(equipDis+"-DAB-"+"temperatureIntegralTime ")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef).setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                                    .addMarker("itimeout").addMarker("sp")
                                    .setUnit("m")
                                    .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                    .setTz(tz)
                                    .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePoint(iTimeoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_TIMEOUT, 0);
        hayStack.writeHisValById(iTimeoutId, TunerConstants.VAV_INTEGRAL_TIMEOUT);

        Point zoneCO2Target  = new Point.Builder()
                                   .setDisplayName(equipDis+"-DAB-"+"zoneCO2Target")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef).setHisInterpolate("cov")
                                   .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                                   .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                   .setUnit("ppm")
                                   .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                   .setTz(tz)
                                   .build();
        String zoneCO2TargetId = hayStack.addPoint(zoneCO2Target);
        hayStack.writePoint(zoneCO2TargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_CO2_TARGET, 0);
        hayStack.writeHisValById(zoneCO2TargetId, TunerConstants.ZONE_CO2_TARGET);

        Point zoneCO2Threshold  = new Point.Builder()
                                      .setDisplayName(equipDis+"-DAB-"+"zoneCO2Threshold")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                                      .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                      .setUnit("ppm")
                                      .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                      .setTz(tz)
                                      .build();
        String zoneCO2ThresholdId = hayStack.addPoint(zoneCO2Threshold);
        hayStack.writePoint(zoneCO2ThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_CO2_THRESHOLD, 0);
        hayStack.writeHisValById(zoneCO2ThresholdId, TunerConstants.ZONE_CO2_THRESHOLD);

        Point zoneVOCTarget  = new Point.Builder()
                                   .setDisplayName(equipDis+"-DAB-"+"zoneVOCTarget")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef).setHisInterpolate("cov")
                                   .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                                   .addMarker("zone").addMarker("voc").addMarker("target").addMarker("sp")
                                   .setUnit("ppb")
                                   .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                   .setTz(tz)
                                   .build();
        String zoneVOCTargetId = hayStack.addPoint(zoneVOCTarget);
        hayStack.writePoint(zoneVOCTargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_VOC_TARGET, 0);
        hayStack.writeHisValById(zoneVOCTargetId, TunerConstants.ZONE_VOC_TARGET);

        Point zoneVOCThreshold  = new Point.Builder()
                                      .setDisplayName(equipDis+"-DAB-"+"zoneVOCThreshold")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                                      .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                      .setUnit("ppb")
                                      .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                      .setTz(tz)
                                      .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);
        hayStack.writePoint(zoneVOCThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_VOC_THRESHOLD, 0);
        hayStack.writeHisValById(zoneVOCThresholdId, TunerConstants.ZONE_VOC_THRESHOLD);

        addDefaultDabSystemTuners(hayStack, siteRef, equipRef, equipDis, tz);
    }
    
    public static void addDefaultDabSystemTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                                 String tz)
    {
        Point targetCumulativeDamper = new Point.Builder()
                                           .setDisplayName(equipDis+"-DAB-"+"targetCumulativeDamper")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                                           .addMarker("target").addMarker("cumulative").addMarker("damper").addMarker("sp")
                                           .setUnit("%")
                                           .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                           .setTz(tz)
                                           .build();
        String targetCumulativeDamperId = hayStack.addPoint(targetCumulativeDamper);
        hayStack.writePoint(targetCumulativeDamperId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.TARGET_CUMULATIVE_DAMPER, 0);
        hayStack.writeHisValById(targetCumulativeDamperId, TunerConstants.TARGET_CUMULATIVE_DAMPER);

        Point analogFanSpeedMultiplier = new Point.Builder()
                                             .setDisplayName(equipDis+"-DAB-"+"analogFanSpeedMultiplier")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipRef).setHisInterpolate("cov")
                                             .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                                             .addMarker("analog").addMarker("fan").addMarker("speed").addMarker("multiplier").addMarker("sp")
                                             .setMinVal("0.1").setMaxVal("3.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                             .setTz(tz)
                                             .build();
        String analogFanSpeedMultiplierId = hayStack.addPoint(analogFanSpeedMultiplier);
        hayStack.writePoint(analogFanSpeedMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ANALOG_FANSPEED_MULTIPLIER, 0);
        hayStack.writeHisValById(analogFanSpeedMultiplierId, TunerConstants.ANALOG_FANSPEED_MULTIPLIER);

        Point humidityHysteresis = new Point.Builder()
                                       .setDisplayName(equipDis+"-DAB-"+"humidityHysteresis")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                                       .addMarker("humidity").addMarker("hysteresis").addMarker("sp")
                                       .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                       .setUnit("%")
                                       .setTz(tz)
                                       .build();
        String humidityHysteresisId = hayStack.addPoint(humidityHysteresis);
        hayStack.writePoint(humidityHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.HUMIDITY_HYSTERESIS_PERCENT, 0);
        hayStack.writeHisValById(humidityHysteresisId, TunerConstants.HUMIDITY_HYSTERESIS_PERCENT);

        Point relayDeactivationHysteresis = new Point.Builder()
                                                .setDisplayName(equipDis+"-DAB-"+"relayDeactivationHysteresis")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipRef).setHisInterpolate("cov")
                                                .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his")
                                                .addMarker("relay").addMarker("deactivation").addMarker("hysteresis").addMarker("sp")
                                                .setMinVal("0").setMaxVal("10").setIncrementVal("0.5").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                .setUnit("%")
                                                .setTz(tz)
                                                .build();
        String relayDeactivationHysteresisId = hayStack.addPoint(relayDeactivationHysteresis);
        hayStack.writePoint(relayDeactivationHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.RELAY_DEACTIVATION_HYSTERESIS, 0);
        hayStack.writeHisValById(relayDeactivationHysteresisId, TunerConstants.RELAY_DEACTIVATION_HYSTERESIS);


        Point rebalanceHoldTime = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"rebalanceHoldTime")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                      .addMarker("rebalance").addMarker("hold").addMarker("time").addMarker("sp")
                                      .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                      .setUnit("m")
                                      .setTz(tz)
                                      .build();
        String rebalanceHoldTimeId = hayStack.addPoint(rebalanceHoldTime);
        hayStack.writePoint(rebalanceHoldTimeId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.REBALANCE_HOLD_TIME, 0);
        hayStack.writeHisValById(rebalanceHoldTimeId, TunerConstants.REBALANCE_HOLD_TIME);
    
        Point modeChangeoverHysteresis = new Point.Builder().setDisplayName(equipDis + "-DAB-" + "modeChangeoverHysteresis")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipRef)
                                                .setHisInterpolate("cov")
                                                .addMarker("tuner").addMarker("dab")
                                                .addMarker("default").addMarker("writable").addMarker("his")
                                                .addMarker("his").addMarker("mode").addMarker("changeover")
                                                .addMarker("hysteresis").addMarker("sp")
                                                .setMinVal("0")
                                                .setMaxVal("5")
                                                .setIncrementVal("0.5")
                                                .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                .setTz(tz)
                                                .build();
        String modeChangeoverHysteresisId = hayStack.addPoint(modeChangeoverHysteresis);
        hayStack.writePointForCcuUser(modeChangeoverHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                      DEFAULT_MODE_CHANGEOVER_HYSTERESIS, 0);
        hayStack.writeHisValById(modeChangeoverHysteresisId, DEFAULT_MODE_CHANGEOVER_HYSTERESIS);
    
        Point stageUpTimerCounter = new Point.Builder().setDisplayName(equipDis + "-DAB-" + "stageUpTimerCounter")
                                                       .setSiteRef(siteRef)
                                                       .setEquipRef(equipRef)
                                                       .setHisInterpolate("cov")
                                                       .addMarker("tuner").addMarker("dab")
                                                       .addMarker("default").addMarker("writable").addMarker("his")
                                                       .addMarker("stageUp")
                                                       .addMarker("timer").addMarker("counter").addMarker("sp")
                                                       .setMinVal("0")
                                                       .setMaxVal("30")
                                                       .setIncrementVal("1")
                                                       .setUnit("m")
                                                       .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                       .setTz(tz)
                                                       .build();
        String stageUpTimerCounterId = hayStack.addPoint(stageUpTimerCounter);
        hayStack.writePointForCcuUser(stageUpTimerCounterId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                      DEFAULT_STAGE_UP_TIMER_COUNTER, 0);
        hayStack.writeHisValById(stageUpTimerCounterId, DEFAULT_STAGE_UP_TIMER_COUNTER);
    
        Point stageDownTimerCounter = new Point.Builder().setDisplayName(equipDis + "-DAB-" +
                                                                         "stageDownTimerCounter")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef)
                                                         .setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("dab")
                                                         .addMarker("default").addMarker("writable").addMarker("his")
                                                         .addMarker("stageDown")
                                                         .addMarker("timer").addMarker("counter").addMarker("sp")
                                                         .setMinVal("0")
                                                         .setMaxVal("30")
                                                         .setIncrementVal("1")
                                                         .setUnit("m")
                                                         .setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                         .setTz(tz)
                                                         .build();
        String stageDownTimerCounterId = hayStack.addPoint(stageDownTimerCounter);
        hayStack.writePointForCcuUser(stageDownTimerCounterId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                      DEFAULT_STAGE_DOWN_TIMER_COUNTER, 0);
        hayStack.writeHisValById(stageDownTimerCounterId, DEFAULT_STAGE_DOWN_TIMER_COUNTER);
    }
    
    public static void addEquipDabTuners(CCUHsApi hayStack, String siteRef, String equipdis, String equipref,
                                         String roomRef, String floorRef, String tz) {
        
        Log.d("CCU", "addEquipDabTuners for " + equipdis);
        ZoneTuners.addZoneTunersForEquip(hayStack, siteRef, equipdis, equipref, roomRef, floorRef, tz);

        Point zonePrioritySpread = new Point.Builder()
                                       .setDisplayName(equipdis+"-"+"zonePrioritySpread")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipref)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                                       .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                                       .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                       .setTz(tz)
                                       .build();
        String zonePrioritySpreadId = hayStack.addPoint(zonePrioritySpread);
        BuildingTunerUtil.updateTunerLevels(zonePrioritySpreadId, roomRef, hayStack);
        hayStack.writeHisValById(zonePrioritySpreadId, HSUtil.getPriorityVal(zonePrioritySpreadId));

        Point zonePriorityMultiplier = new Point.Builder()
                                           .setDisplayName(equipdis+"-"+"zonePriorityMultiplier")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipref)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                                           .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                           .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                           .setTz(tz)
                                           .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        BuildingTunerUtil.updateTunerLevels(zonePriorityMultiplierId, roomRef, hayStack);
        hayStack.writeHisValById(zonePriorityMultiplierId, HSUtil.getPriorityVal(zonePriorityMultiplierId));

        Point coolingDb = new Point.Builder()
                              .setDisplayName(equipdis+"-"+"coolingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipref)
                              .setRoomRef(roomRef)
                              .setFloorRef(floorRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                              .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                              .setTz(tz)
                              .setUnit("\u00B0F")
                              .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        BuildingTunerUtil.updateTunerLevels(coolingDbId, roomRef, hayStack);
        hayStack.writeHisValById(coolingDbId, HSUtil.getPriorityVal(coolingDbId));

        Point coolingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"coolingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .setRoomRef(roomRef)
                                        .setFloorRef(floorRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                                        .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        BuildingTunerUtil.updateTunerLevels(coolingDbMultiplierId, roomRef, hayStack);
        hayStack.writeHisValById(coolingDbMultiplierId, HSUtil.getPriorityVal(coolingDbMultiplierId));

        Point heatingDb = new Point.Builder()
                              .setDisplayName(equipdis+"-"+"heatingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipref)
                              .setRoomRef(roomRef)
                              .setFloorRef(floorRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                              .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                              .setTz(tz)
                              .setUnit("\u00B0F")
                              .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        BuildingTunerUtil.updateTunerLevels(heatingDbId, roomRef, hayStack);
        hayStack.writeHisValById(heatingDbId, HSUtil.getPriorityVal(heatingDbId));

        Point heatingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"heatingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .setRoomRef(roomRef)
                                        .setFloorRef(floorRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                                        .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        BuildingTunerUtil.updateTunerLevels(heatingDbMultiplierId, roomRef, hayStack);
        hayStack.writeHisValById(heatingDbMultiplierId, HSUtil.getPriorityVal(heatingDbMultiplierId));

        Point propGain = new Point.Builder()
                             .setDisplayName(equipdis+"-"+"proportionalKFactor")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipref)
                             .setRoomRef(roomRef)
                             .setFloorRef(floorRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                             .addMarker("pgain").addMarker("sp")
                             .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                             .setTz(tz)
                             .build();
        String pgainId = hayStack.addPoint(propGain);
        BuildingTunerUtil.updateTunerLevels(pgainId, roomRef, hayStack);
        hayStack.writeHisValById(pgainId, HSUtil.getPriorityVal(pgainId));

        Point integralGain = new Point.Builder()
                                 .setDisplayName(equipdis+"-"+"integralKFactor")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .setRoomRef(roomRef)
                                 .setFloorRef(floorRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                                 .addMarker("igain").addMarker("sp")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                 .setTz(tz)
                                 .build();
        String igainId = hayStack.addPoint(integralGain);
        BuildingTunerUtil.updateTunerLevels(igainId, roomRef, hayStack);
        hayStack.writeHisValById(igainId, HSUtil.getPriorityVal(igainId));

        Point propSpread = new Point.Builder()
                               .setDisplayName(equipdis+"-"+"temperatureProportionalRange")
                               .setSiteRef(siteRef)
                               .setEquipRef(equipref)
                               .setRoomRef(roomRef)
                               .setFloorRef(floorRef).setHisInterpolate("cov")
                               .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                               .addMarker("pspread").addMarker("sp")
                               .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                               .setTz(tz)
                               .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        BuildingTunerUtil.updateTunerLevels(pSpreadId, roomRef, hayStack);
        hayStack.writeHisValById(pSpreadId, HSUtil.getPriorityVal(pSpreadId));

        Point integralTimeout = new Point.Builder()
                                    .setDisplayName(equipdis+"-"+"temperatureIntegralTime")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipref)
                                    .setRoomRef(roomRef)
                                    .setFloorRef(floorRef).setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                                    .addMarker("itimeout").addMarker("sp")
                                    .setUnit("m")
                                    .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                    .setTz(tz)
                                    .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        BuildingTunerUtil.updateTunerLevels(iTimeoutId, roomRef, hayStack);
        hayStack.writeHisValById(iTimeoutId, HSUtil.getPriorityVal(iTimeoutId));

        Point zoneCO2Target = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"zoneCO2Target")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                                  .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                  .setUnit("ppm")
                                  .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                  .setTz(tz)
                                  .build();
        String zoneCO2TargetId = hayStack.addPoint(zoneCO2Target);
        BuildingTunerUtil.updateTunerLevels(zoneCO2TargetId, roomRef, hayStack);
        hayStack.writeHisValById(zoneCO2TargetId, HSUtil.getPriorityVal(zoneCO2TargetId));

        Point zoneCO2Threshold = new Point.Builder()
                                     .setDisplayName(equipdis+"-"+"zoneCO2Threshold")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .setRoomRef(roomRef)
                                     .setFloorRef(floorRef).setHisInterpolate("cov")
                                     .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                                     .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                     .setUnit("ppm")
                                     .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                     .setTz(tz)
                                     .build();
        String zoneCO2ThresholdId = hayStack.addPoint(zoneCO2Threshold);
        BuildingTunerUtil.updateTunerLevels(zoneCO2ThresholdId, roomRef, hayStack);
        hayStack.writeHisValById(zoneCO2ThresholdId, HSUtil.getPriorityVal(zoneCO2ThresholdId));

        Point zoneVOCTarget = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"zoneVOCTarget")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("sp")
                                  .addMarker("zone").addMarker("voc").addMarker("target")
                                  .setUnit("ppb")
                                  .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                  .setTz(tz)
                                  .build();
        String zoneVOCTargetId = hayStack.addPoint(zoneVOCTarget);
        BuildingTunerUtil.updateTunerLevels(zoneVOCTargetId, roomRef, hayStack);
        hayStack.writeHisValById(zoneVOCTargetId, HSUtil.getPriorityVal(zoneVOCTargetId));

        Point zoneVOCThreshold = new Point.Builder()
                                     .setDisplayName(equipdis+"-"+"zoneVOCThreshold")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .setRoomRef(roomRef)
                                     .setFloorRef(floorRef).setHisInterpolate("cov")
                                     .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his")
                                     .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                     .setUnit("ppb")
                                     .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                     .setTz(tz)
                                     .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);
        BuildingTunerUtil.updateTunerLevels(zoneVOCThresholdId, roomRef, hayStack);
        hayStack.writeHisValById(zoneVOCThresholdId, HSUtil.getPriorityVal(zoneVOCThresholdId));
    }

    /***
     * When BuildingTuner fails to fetch a point from remote , it should fall back to below values
     * on non-primary CCUs.
     * @param tags
     * @return
     */
    public static double getDefaultTunerVal(String tags) {

        List<String> tagList = Arrays.asList(tags.split(HSUtil.QUERY_JOINER));
        Optional<Double> defaultVal = getDefaultTagsValMap().entrySet().stream()
                                                            .filter(m -> {
                                                                for (String tag : tagList) {
                                                                    if (!m.getKey().contains(tag)) {
                                                                        return false;
                                                                    }
                                                                }
                                                                return true;
                                                            })
                                                            .map(m -> m.getValue())
                                                            .findFirst();
        return defaultVal.isPresent() ? defaultVal.get() : 0;
    }


    public static HashMap<String, Double> getDefaultTagsValMap() {
        HashMap<String, Double> tagsValMap = new HashMap<>();
        tagsValMap.put("priority,spread",TunerConstants.ZONE_PRIORITY_SPREAD);
        tagsValMap.put("priority,multiplier",TunerConstants.ZONE_PRIORITY_MULTIPLIER);
        tagsValMap.put("cooling,deadband",TunerConstants.VAV_COOLING_DB);
        tagsValMap.put("cooling,deadband,multiplier",TunerConstants.VAV_COOLING_DB_MULTPLIER);
        tagsValMap.put("heating,deadband",TunerConstants.VAV_HEATING_DB);
        tagsValMap.put("heating,deadband,multiplier",TunerConstants.VAV_HEATING_DB_MULTIPLIER);
        tagsValMap.put("pgain",TunerConstants.VAV_PROPORTIONAL_GAIN);
        tagsValMap.put("pspread",TunerConstants.VAV_PROPORTIONAL_SPREAD);
        tagsValMap.put("igain",TunerConstants.VAV_INTEGRAL_GAIN);
        tagsValMap.put("itimeout",TunerConstants.VAV_INTEGRAL_TIMEOUT);
        tagsValMap.put("zone,co2,target",TunerConstants.ZONE_CO2_TARGET);
        tagsValMap.put("zone,co2,threshold",TunerConstants.ZONE_CO2_THRESHOLD);
        tagsValMap.put("zone,voc,target",TunerConstants.ZONE_VOC_TARGET);
        tagsValMap.put("zone,voc,threshold",TunerConstants.ZONE_VOC_THRESHOLD);
        return tagsValMap;
    }



}
