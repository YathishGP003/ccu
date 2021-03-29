package a75f.io.logic.tuners;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

import static a75f.io.logic.tuners.TunerConstants.DEFAULT_STAGE_DOWN_TIMER_COUNTER;
import static a75f.io.logic.tuners.TunerConstants.DEFAULT_STAGE_UP_TIMER_COUNTER;

public class VavTuners {
    
    public static void addDefaultVavTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                           String tz) {

        HashMap tuner = CCUHsApi.getInstance().read("point and tuner and default and vav");
        if (tuner != null && tuner.size() > 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM,"Default VAV Tuner points already exist");
            return;
        }
        CcuLog.d(L.TAG_CCU_SYSTEM,"Default VAV Tuner  does not exist. Create Now");

        Point zonePrioritySpread = new Point.Builder()
                                       .setDisplayName(equipDis+"-VAV-"+"zonePrioritySpread")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("his")
                                       .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                                       .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                       .setTz(tz)
                                       .build();
        String zonePrioritySpreadId = hayStack.addPoint(zonePrioritySpread);
        hayStack.writePointForCcuUser(zonePrioritySpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.ZONE_PRIORITY_SPREAD, 0);
        hayStack.writeHisValById(zonePrioritySpreadId, TunerConstants.ZONE_PRIORITY_SPREAD);

        Point zonePriorityMultiplier = new Point.Builder()
                                           .setDisplayName(equipDis+"-VAV-"+"zonePriorityMultiplier")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("his")
                                           .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                           .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                           .setTz(tz)
                                           .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        hayStack.writePointForCcuUser(zonePriorityMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.ZONE_PRIORITY_MULTIPLIER, 0);
        hayStack.writeHisValById(zonePriorityMultiplierId, TunerConstants.ZONE_PRIORITY_MULTIPLIER);

        Point coolingDb = new Point.Builder()
                              .setDisplayName(equipDis+"-VAV-"+"coolingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("his")
                              .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10").setIncrementVal("0.5").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                              .setUnit("\u00B0F")
                              .setTz(tz)
                              .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        hayStack.writePointForCcuUser(coolingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.VAV_COOLING_DB, 0);
        hayStack.writeHisValById(coolingDbId, TunerConstants.VAV_COOLING_DB);

        Point coolingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipDis+"-VAV-"+"coolingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("his")
                                        .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        hayStack.writePointForCcuUser(coolingDbMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.VAV_COOLING_DB_MULTPLIER, 0);
        hayStack.writeHisValById(coolingDbMultiplierId, TunerConstants.VAV_COOLING_DB_MULTPLIER);

        Point heatingDb = new Point.Builder()
                              .setDisplayName(equipDis+"-VAV-"+"heatingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("his")
                              .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                              .setUnit("\u00B0F")
                              .setTz(tz)
                              .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        hayStack.writePointForCcuUser(heatingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.VAV_HEATING_DB, 0);
        hayStack.writeHisValById(heatingDbId, TunerConstants.VAV_HEATING_DB);

        Point heatingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipDis+"-VAV-"+"heatingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("his")
                                        .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        hayStack.writePointForCcuUser(heatingDbMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.VAV_HEATING_DB_MULTIPLIER, 0);
        hayStack.writeHisValById(heatingDbMultiplierId, TunerConstants.VAV_HEATING_DB_MULTIPLIER);

        Point propGain = new Point.Builder()
                             .setDisplayName(equipDis+"-VAV-"+"proportionalKFactor")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("his")
                             .addMarker("pgain").addMarker("sp")
                             .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                             .setTz(tz)
                             .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePointForCcuUser(pgainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.VAV_PROPORTIONAL_GAIN, 0);
        hayStack.writeHisValById(pgainId, TunerConstants.VAV_PROPORTIONAL_GAIN);

        Point integralGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-VAV-"+"integralKFactor")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("his")
                                 .addMarker("igain").addMarker("sp")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                 .setTz(tz)
                                 .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePointForCcuUser(igainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.VAV_INTEGRAL_GAIN, 0);
        hayStack.writeHisValById(igainId, TunerConstants.VAV_INTEGRAL_GAIN);

        Point propSpread = new Point.Builder()
                               .setDisplayName(equipDis+"-VAV-"+"temperatureProportionalRange")
                               .setSiteRef(siteRef)
                               .setEquipRef(equipRef).setHisInterpolate("cov")
                               .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("his")
                               .addMarker("pspread").addMarker("sp")
                               .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                               .setTz(tz)
                               .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        hayStack.writePointForCcuUser(pSpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.VAV_PROPORTIONAL_SPREAD, 0);
        hayStack.writeHisValById(pSpreadId, TunerConstants.VAV_PROPORTIONAL_SPREAD);

        Point integralTimeout = new Point.Builder()
                                    .setDisplayName(equipDis+"-VAV-"+"temperatureIntegralTime")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef).setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("his")
                                    .addMarker("itimeout").addMarker("sp")
                                    .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                    .setUnit("m")
                                    .setTz(tz)
                                    .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePointForCcuUser(iTimeoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.VAV_INTEGRAL_TIMEOUT, 0);
        hayStack.writeHisValById(iTimeoutId, TunerConstants.VAV_INTEGRAL_TIMEOUT);

        Point valveStartDamper  = new Point.Builder()
                                      .setDisplayName(equipDis+"-VAV-"+"valveActuationStartDamperPosDuringSysHeating")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("his")
                                      .addMarker("valve").addMarker("start").addMarker("damper").addMarker("sp")
                                      .setMinVal("1").setMaxVal("100").setIncrementVal("5").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                      .setTz(tz)
                                      .build();
        String valveStartDamperId = hayStack.addPoint(valveStartDamper);
        hayStack.writePointForCcuUser(valveStartDamperId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.VALVE_START_DAMPER, 0);
        hayStack.writeHisValById(valveStartDamperId, TunerConstants.VALVE_START_DAMPER);

        Point zoneCO2Target  = new Point.Builder()
                                   .setDisplayName(equipDis+"-VAV-"+"zoneCO2Target")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef).setHisInterpolate("cov")
                                   .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("his")
                                   .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                   .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                   .setUnit("ppm")
                                   .setTz(tz)
                                   .build();
        String zoneCO2TargetId = hayStack.addPoint(zoneCO2Target);
        hayStack.writePointForCcuUser(zoneCO2TargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.ZONE_CO2_TARGET, 0);
        hayStack.writeHisValById(zoneCO2TargetId, TunerConstants.ZONE_CO2_TARGET);

        Point zoneCO2Threshold  = new Point.Builder()
                                      .setDisplayName(equipDis+"-VAV-"+"zoneCO2Threshold")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("his")
                                      .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                      .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                      .setUnit("ppm")
                                      .setTz(tz)
                                      .build();
        String zoneCO2ThresholdId = hayStack.addPoint(zoneCO2Threshold);
        hayStack.writePointForCcuUser(zoneCO2ThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.ZONE_CO2_THRESHOLD, 0);
        hayStack.writeHisValById(zoneCO2ThresholdId, TunerConstants.ZONE_CO2_THRESHOLD);

        Point zoneVOCTarget  = new Point.Builder()
                                   .setDisplayName(equipDis+"-VAV-"+"zoneVOCTarget")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef).setHisInterpolate("cov")
                                   .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his")
                                   .addMarker("zone").addMarker("voc").addMarker("target").addMarker("sp")
                                   .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                   .setUnit("ppb")
                                   .setTz(tz)
                                   .build();
        String zoneVOCTargetId = hayStack.addPoint(zoneVOCTarget);
        hayStack.writePointForCcuUser(zoneVOCTargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.ZONE_VOC_TARGET, 0);
        hayStack.writeHisValById(zoneVOCTargetId, TunerConstants.ZONE_VOC_TARGET);

        Point zoneVOCThreshold  = new Point.Builder()
                                      .setDisplayName(equipDis+"-VAV-"+"zoneVOCThreshold")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his")
                                      .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                      .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                      .setUnit("ppb")
                                      .setTz(tz)
                                      .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);
        hayStack.writePointForCcuUser(zoneVOCThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.ZONE_VOC_THRESHOLD, 0);
        hayStack.writeHisValById(zoneVOCThresholdId, TunerConstants.ZONE_VOC_THRESHOLD);


        Point co2IgnoreRequest = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"co2IgnoreRequest")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef).setHisInterpolate("cov")
                                     .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his")
                                     .addMarker("co2").addMarker("ignoreRequest").addMarker("sp")
                                     .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                     .setTz(tz)
                                     .build();
        String co2IgnoreRequestId = hayStack.addPoint(co2IgnoreRequest);
        hayStack.writePointForCcuUser(co2IgnoreRequestId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,2.0, 0);
        hayStack.writeHisValById(co2IgnoreRequestId,2.0);

        Point co2SPInit = new Point.Builder()
                              .setDisplayName(equipDis+"-"+"co2SPInit")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his")
                              .addMarker("co2").addMarker("spinit").addMarker("sp")
                              .setMinVal("0").setMaxVal("1500").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                              .setUnit("ppm")
                              .setTz(tz)
                              .build();
        String co2SPInitId = hayStack.addPoint(co2SPInit);
        hayStack.writePointForCcuUser(co2SPInitId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,800.0, 0);
        hayStack.writeHisValById(co2SPInitId,800.0);

        Point co2SPMax = new Point.Builder()
                             .setDisplayName(equipDis+"-"+"co2SPMax")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("tr").addMarker("vav").addMarker("writable").addMarker("his")
                             .addMarker("default").addMarker("co2").addMarker("spmax").addMarker("sp")
                             .setMinVal("100").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                             .setUnit("ppm")
                             .setTz(tz)
                             .build();
        String co2SPMaxId = hayStack.addPoint(co2SPMax);
        hayStack.writePointForCcuUser(co2SPMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,1000.0, 0);
        hayStack.writeHisValById(co2SPMaxId,1000.0);

        Point co2SPMin = new Point.Builder()
                             .setDisplayName(equipDis+"-"+"co2SPMin")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                             .addMarker("co2").addMarker("spmin").addMarker("writable").addMarker("his")
                             .setMinVal("0").setMaxVal("1500").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                             .setUnit("ppm")
                             .setTz(tz)
                             .build();
        String co2SPMinId = hayStack.addPoint(co2SPMin);
        hayStack.writePointForCcuUser(co2SPMinId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,800.0,0 );
        hayStack.writeHisValById(co2SPMinId,800.0);

        Point co2SPRes = new Point.Builder()
                             .setDisplayName(equipDis+"-"+"co2SPRes")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                             .addMarker("co2").addMarker("spres").addMarker("writable").addMarker("his")
                             .setMinVal("-30.0").setMaxVal("-1.0").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                             .setUnit("ppm")
                             .setTz(tz)
                             .build();
        String co2SPResId = hayStack.addPoint(co2SPRes);
        hayStack.writePointForCcuUser(co2SPResId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-10.0,0 );
        hayStack.writeHisValById(co2SPResId,-10.0);

        Point co2SPResMax = new Point.Builder()
                                .setDisplayName(equipDis+"-"+"co2SPResMax")
                                .setSiteRef(siteRef)
                                .setEquipRef(equipRef).setHisInterpolate("cov")
                                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                .addMarker("co2").addMarker("spresmax").addMarker("writable").addMarker("his")
                                .setMinVal("-50.0").setMaxVal("-1.0").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                .setUnit("ppm")
                                .setTz(tz)
                                .build();
        String co2SPResMaxId = hayStack.addPoint(co2SPResMax);
        hayStack.writePointForCcuUser(co2SPResMaxId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-30.0,0 );
        hayStack.writeHisValById(co2SPResMaxId,-30.0);

        Point co2SPTrim = new Point.Builder()
                              .setDisplayName(equipDis+"-"+"co2SPTrim")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                              .addMarker("co2").addMarker("sptrim").addMarker("writable").addMarker("his")
                              .setMinVal("0").setMaxVal("50").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                              .setUnit("ppm")
                              .setTz(tz)
                              .build();
        String co2SPTrimId = hayStack.addPoint(co2SPTrim);
        hayStack.writePointForCcuUser(co2SPTrimId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,20.0,0 );
        hayStack.writeHisValById(co2SPTrimId,20.0);

        Point co2TimeDelay = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"co2TimeDelay")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                 .addMarker("co2").addMarker("timeDelay").addMarker("writable").addMarker("his")
                                 .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                 .setUnit("m")
                                 .setTz(tz)
                                 .build();
        String co2TimeDelayId = hayStack.addPoint(co2TimeDelay);
        hayStack.writePointForCcuUser(co2TimeDelayId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,2.0,0 );
        hayStack.writeHisValById(co2TimeDelayId,2.0);

        Point co2TimeInterval = new Point.Builder()
                                    .setDisplayName(equipDis+"-"+"co2TimeInterval")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef).setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                    .addMarker("co2").addMarker("timeInterval").addMarker("writable").addMarker("his")
                                    .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                    .setUnit("m")
                                    .setTz(tz)
                                    .build();
        String co2TimeIntervalId = hayStack.addPoint(co2TimeInterval);
        hayStack.writePointForCcuUser(co2TimeIntervalId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,2.0,0 );
        hayStack.writeHisValById(co2TimeIntervalId,2.0);

        Point satIgnoreRequest = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"satIgnoreRequest")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef).setHisInterpolate("cov")
                                     .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                     .addMarker("sat").addMarker("ignoreRequest").addMarker("writable").addMarker("his")
                                     .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                     .setTz(tz)
                                     .build();
        String satIgnoreRequestId = hayStack.addPoint(satIgnoreRequest);
        hayStack.writePointForCcuUser(satIgnoreRequestId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,2.0,0 );
        hayStack.writeHisValById(satIgnoreRequestId,2.0);

        Point satSPInit = new Point.Builder()
                              .setDisplayName(equipDis+"-"+"satSPInit")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                              .addMarker("sat").addMarker("spinit").addMarker("writable").addMarker("his")
                              .setMinVal("50").setMaxVal("70").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                              .setUnit("\u00B0F")
                              .setTz(tz)
                              .build();
        String satSPInitId = hayStack.addPoint(satSPInit);
        hayStack.writePointForCcuUser(satSPInitId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,65.0,0 );
        hayStack.writeHisValById(satSPInitId,65.0);

        Point satSPMax = new Point.Builder()
                             .setDisplayName(equipDis+"-"+"satSPMax")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                             .addMarker("sat").addMarker("spmax").addMarker("writable").addMarker("his")
                             .setMinVal("55").setMaxVal("75").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                             .setUnit("\u00B0F")
                             .setTz(tz)
                             .build();
        String satSPMaxId = hayStack.addPoint(satSPMax);
        hayStack.writePointForCcuUser(satSPMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,65.0,0 );
        hayStack.writeHisValById(satSPMaxId,65.0);

        Point satSPMin = new Point.Builder()
                             .setDisplayName(equipDis+"-"+"satSPMin")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                             .addMarker("sat").addMarker("spmin").addMarker("writable").addMarker("his")
                             .setMinVal("45").setMaxVal("65").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                             .setUnit("\u00B0F")
                             .setTz(tz)
                             .build();
        String satSPMinId = hayStack.addPoint(satSPMin);
        hayStack.writePointForCcuUser(satSPMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,55.0,0 );
        hayStack.writeHisValById(satSPMinId,55.0);

        Point satSPRes = new Point.Builder()
                             .setDisplayName(equipDis+"-"+"satSPRes")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                             .addMarker("sat").addMarker("spres").addMarker("writable").addMarker("his")
                             .setMaxVal("-0.1").setMinVal("-2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                             .setUnit("\u00B0F")
                             .setTz(tz)
                             .build();
        String satSPResId = hayStack.addPoint(satSPRes);
        hayStack.writePointForCcuUser(satSPResId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-0.3,0 );
        hayStack.writeHisValById(satSPResId,-0.3);

        Point satSPResMax = new Point.Builder()
                                .setDisplayName(equipDis+"-"+"satSPResMax")
                                .setSiteRef(siteRef)
                                .setEquipRef(equipRef).setHisInterpolate("cov")
                                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                .addMarker("sat").addMarker("spresmax").addMarker("writable").addMarker("his")
                                .setMaxVal("-0.1").setMinVal("-3.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                .setUnit("\u00B0F")
                                .setTz(tz)
                                .build();
        String satSPResMaxId = hayStack.addPoint(satSPResMax);
        hayStack.writePointForCcuUser(satSPResMaxId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-1.0,0 );
        hayStack.writeHisValById(satSPResMaxId,-1.0);

        Point satSPTrim = new Point.Builder()
                              .setDisplayName(equipDis+"-"+"satSPTrim")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                              .addMarker("sat").addMarker("sptrim").addMarker("writable").addMarker("his")
                              .setMinVal("-0.5").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                              .setUnit("\u00B0F")
                              .setTz(tz)
                              .build();
        String satSPTrimId = hayStack.addPoint(satSPTrim);
        hayStack.writePointForCcuUser(satSPTrimId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,0.2,0 );
        hayStack.writeHisValById(satSPTrimId,0.2);

        Point satTimeDelay = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"satTimeDelay")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                 .addMarker("sat").addMarker("timeDelay").addMarker("writable").addMarker("his")
                                 .setMinVal("0").setMaxVal("30").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                 .setUnit("m")
                                 .setTz(tz)
                                 .build();
        String satTimeDelayId = hayStack.addPoint(satTimeDelay);
        hayStack.writePointForCcuUser(satTimeDelayId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,2.0,0);
        hayStack.writeHisValById(satTimeDelayId,2.0);

        Point satTimeInterval = new Point.Builder()
                                    .setDisplayName(equipDis+"-"+"satTimeInterval")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef).setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                    .addMarker("sat").addMarker("timeInterval").addMarker("writable").addMarker("his")
                                    .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                    .setUnit("m")
                                    .setTz(tz)
                                    .build();
        String satTimeIntervalId = hayStack.addPoint(satTimeInterval);
        hayStack.writePointForCcuUser(satTimeIntervalId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,2.0,0 );
        hayStack.writeHisValById(satTimeIntervalId,2.0);

        Point staticPressureIgnoreRequest = new Point.Builder()
                                                .setDisplayName(equipDis+"-"+"staticPressureIgnoreRequest")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipRef).setHisInterpolate("cov")
                                                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                                .addMarker("staticPressure").addMarker("ignoreRequest").addMarker("writable").addMarker("his")
                                                .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                .setTz(tz)
                                                .build();
        String staticPressureIgnoreRequestId = hayStack.addPoint(staticPressureIgnoreRequest);
        hayStack.writePointForCcuUser(staticPressureIgnoreRequestId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,2.0,0 );
        hayStack.writeHisValById(staticPressureIgnoreRequestId,2.0);

        Point staticPressureSPInit = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"staticPressureSPInit")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                         .addMarker("staticPressure").addMarker("spinit").addMarker("writable").addMarker("his")
                                         .setMinVal("0.1").setMaxVal("2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                         .setUnit("inch wc")
                                         .setTz(tz)
                                         .build();
        String staticPressureSPInitId = hayStack.addPoint(staticPressureSPInit);
        hayStack.writePointForCcuUser(staticPressureSPInitId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,0.2,0);
        hayStack.writeHisValById(staticPressureSPInitId,0.2);

        Point staticPressureSPMax = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"staticPressureSPMax")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                        .addMarker("staticPressure").addMarker("spmax").addMarker("writable").addMarker("his")
                                        .setMinVal("0.1").setMaxVal("2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                        .setUnit("inch wc")
                                        .setTz(tz)
                                        .build();
        String staticPressureSPMaxId = hayStack.addPoint(staticPressureSPMax);
        hayStack.writePointForCcuUser(staticPressureSPMaxId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,1.0,0 );
        hayStack.writeHisValById(staticPressureSPMaxId,1.0);

        Point staticPressureSPMin = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"staticPressureSPMin")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                        .addMarker("staticPressure").addMarker("spmin").addMarker("writable").addMarker("his")
                                        .setMinVal("0.1").setMaxVal("2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                        .setUnit("inch wc")
                                        .setTz(tz)
                                        .build();
        String staticPressureSPMinId = hayStack.addPoint(staticPressureSPMin);
        hayStack.writePointForCcuUser(staticPressureSPMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,0.2 ,0);
        hayStack.writeHisValById(staticPressureSPMinId,0.2);

        Point staticPressureSPRes = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"staticPressureSPRes")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                        .addMarker("staticPressure").addMarker("spres").addMarker("writable").addMarker("his")
                                        .setMinVal("0.01").setMaxVal("0.2").setIncrementVal("0.01").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                        .setUnit("inch wc")
                                        .setTz(tz)
                                        .build();
        String staticPressureSPResId = hayStack.addPoint(staticPressureSPRes);
        hayStack.writePointForCcuUser(staticPressureSPResId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,0.05,0 );
        hayStack.writeHisValById(staticPressureSPResId,0.05);

        Point staticPressureSPResMax = new Point.Builder()
                                           .setDisplayName(equipDis+"-"+"staticPressureSPResMax")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                           .addMarker("staticPressure").addMarker("spresmax").addMarker("writable").addMarker("his")
                                           .setMinVal("0.05").setMaxVal("0.5").setIncrementVal("0.05").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                           .setUnit("inch wc")
                                           .setTz(tz)
                                           .build();
        String staticPressureSPResMaxId = hayStack.addPoint(staticPressureSPResMax);
        hayStack.writePointForCcuUser(staticPressureSPResMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,0.1,0 );
        hayStack.writeHisValById(staticPressureSPResMaxId,0.1);

        Point staticPressureSPTrim = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"staticPressureSPTrim")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                         .addMarker("staticPressure").addMarker("sptrim").addMarker("writable").addMarker("his")
                                         .setMinVal("-0.01").setMaxVal("-0.5").setIncrementVal("-0.01").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                         .setUnit("inch wc")
                                         .setTz(tz)
                                         .build();
        String staticPressureSPTrimId = hayStack.addPoint(staticPressureSPTrim);
        hayStack.writePointForCcuUser(staticPressureSPTrimId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-0.02 ,0);
        hayStack.writeHisValById(staticPressureSPTrimId,-0.02);

        Point staticPressureTimeDelay = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"staticPressureTimeDelay")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef).setHisInterpolate("cov")
                                            .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                            .addMarker("staticPressure").addMarker("timeDelay").addMarker("writable").addMarker("his")
                                            .setMinVal("0").setMaxVal("30").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                            .setUnit("m")
                                            .setTz(tz)
                                            .build();
        String staticPressureTimeDelayId = hayStack.addPoint(staticPressureTimeDelay);
        hayStack.writePointForCcuUser(staticPressureTimeDelayId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,2.0,0 );
        hayStack.writeHisValById(staticPressureTimeDelayId,2.0);

        Point staticPressureTimeInterval = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"staticPressureTimeInterval")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef).setHisInterpolate("cov")
                                               .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                                               .addMarker("staticPressure").addMarker("timeInterval").addMarker("writable").addMarker("his")
                                               .setMinVal("0").setMaxVal("30").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                               .setUnit("m")
                                               .setTz(tz)
                                               .build();
        String staticPressureTimeIntervalId = hayStack.addPoint(staticPressureTimeInterval);
        hayStack.writePointForCcuUser(staticPressureTimeIntervalId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,2.0 ,0);
        hayStack.writeHisValById(staticPressureTimeIntervalId,2.0);
        addDefaultVavSystemTuners(hayStack, siteRef, equipRef, equipDis, tz);
    
        Point fanControlOnFixedTimeDelay  = new Point.Builder()
                                                .setDisplayName(equipDis + "-VAV-"+"fanControlOnFixedTimeDelay ")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipRef)
                                                .setHisInterpolate("cov")
                                                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his")
                                                .addMarker("fan").addMarker("control").addMarker("time").addMarker("delay").addMarker("sp")
                                                .setMinVal("0")
                                                .setMaxVal("10")
                                                .setIncrementVal("1")
                                                .setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                .setUnit("m")
                                                .setTz(tz)
                                                .build();
        String fanControlOnFixedTimeDelayId = CCUHsApi.getInstance().addPoint(fanControlOnFixedTimeDelay);
        CCUHsApi.getInstance().writeDefaultValById(fanControlOnFixedTimeDelayId, TunerConstants.DEFAULT_FAN_ON_CONTROL_DELAY);
        CCUHsApi.getInstance().writeHisValById(fanControlOnFixedTimeDelayId, TunerConstants.DEFAULT_FAN_ON_CONTROL_DELAY);
    
        Point reheatZoneToDATMinDifferential  = createReheatZoneToDATMinDifferentialTuner(true, equipDis, equipRef,
                                                                                          siteRef, tz);
        String reheatZoneToDATMinDifferentialId = CCUHsApi.getInstance().addPoint(reheatZoneToDATMinDifferential);
        CCUHsApi.getInstance().writeDefaultValById(reheatZoneToDATMinDifferentialId, TunerConstants.DEFAULT_REHEAT_ZONE_DAT_MIN_DIFFERENTIAL);
        CCUHsApi.getInstance().writeHisValById(reheatZoneToDATMinDifferentialId, TunerConstants.DEFAULT_REHEAT_ZONE_DAT_MIN_DIFFERENTIAL);
    }
    
    public static void addDefaultVavSystemTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                                 String tz) {

        Point targetCumulativeDamper = new Point.Builder()
                                           .setDisplayName(equipDis+"-VAV-"+"targetCumulativeDamper")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his")
                                           .addMarker("target").addMarker("cumulative").addMarker("damper").addMarker("sp")
                                           .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                           .setUnit("%")
                                           .setTz(tz)
                                           .build();
        String targetCumulativeDamperId = hayStack.addPoint(targetCumulativeDamper);
        hayStack.writePointForCcuUser(targetCumulativeDamperId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.TARGET_CUMULATIVE_DAMPER, 0);
        hayStack.writeHisValById(targetCumulativeDamperId, TunerConstants.TARGET_CUMULATIVE_DAMPER);

        Point analogFanSpeedMultiplier = new Point.Builder()
                                             .setDisplayName(equipDis+"-VAV-"+"analogFanSpeedMultiplier")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipRef).setHisInterpolate("cov")
                                             .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his")
                                             .addMarker("analog").addMarker("fan").addMarker("speed").addMarker("multiplier").addMarker("sp")
                                             .setMinVal("0.1").setMaxVal("3.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                             .setTz(tz)
                                             .build();
        String analogFanSpeedMultiplierId = hayStack.addPoint(analogFanSpeedMultiplier);
        hayStack.writePointForCcuUser(analogFanSpeedMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.ANALOG_FANSPEED_MULTIPLIER, 0);
        hayStack.writeHisValById(analogFanSpeedMultiplierId, TunerConstants.ANALOG_FANSPEED_MULTIPLIER);

        Point humidityHysteresis = new Point.Builder()
                                       .setDisplayName(equipDis+"-VAV-"+"humidityHysteresis")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his")
                                       .addMarker("humidity").addMarker("hysteresis").addMarker("sp")
                                       .setMinVal("0").setMaxVal("100").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                       .setUnit("%")
                                       .setTz(tz)
                                       .build();
        String humidityHysteresisId = hayStack.addPoint(humidityHysteresis);
        hayStack.writePointForCcuUser(humidityHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.HUMIDITY_HYSTERESIS_PERCENT, 0);
        hayStack.writeHisValById(humidityHysteresisId, TunerConstants.HUMIDITY_HYSTERESIS_PERCENT);

        Point relayDeactivationHysteresis = new Point.Builder()
                                                .setDisplayName(equipDis+"-VAV-"+"relayDeactivationHysteresis")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipRef).setHisInterpolate("cov")
                                                .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his")
                                                .addMarker("relay").addMarker("deactivation").addMarker("hysteresis").addMarker("sp")
                                                .setMinVal("0").setMaxVal("100").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                .setUnit("%")
                                                .setTz(tz)
                                                .build();
        String relayDeactivationHysteresisId = hayStack.addPoint(relayDeactivationHysteresis);
        hayStack.writePointForCcuUser(relayDeactivationHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.RELAY_DEACTIVATION_HYSTERESIS, 0);
        hayStack.writeHisValById(relayDeactivationHysteresisId, TunerConstants.RELAY_DEACTIVATION_HYSTERESIS);
    
        Point stageUpTimerCounter = new Point.Builder().setDisplayName(equipDis + "-VAV-" + "stageUpTimerCounter")
                                                       .setSiteRef(siteRef)
                                                       .setEquipRef(equipRef)
                                                       .setHisInterpolate("cov")
                                                       .addMarker("tuner").addMarker("vav")
                                                       .addMarker("default").addMarker("writable").addMarker("his")
                                                       .addMarker("stageUp")
                                                       .addMarker("timer").addMarker("counter").addMarker("sp")
                                                       .setMinVal("0")
                                                       .setMaxVal("30")
                                                       .setIncrementVal("1")
                                                       .setUnit("m")
                                                       .setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                       .setTz(tz)
                                                       .build();
        String stageUpTimerCounterId = hayStack.addPoint(stageUpTimerCounter);
        hayStack.writePointForCcuUser(stageUpTimerCounterId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                      DEFAULT_STAGE_UP_TIMER_COUNTER, 0);
        hayStack.writeHisValById(stageUpTimerCounterId, DEFAULT_STAGE_UP_TIMER_COUNTER);
    
        Point stageDownTimerCounter = new Point.Builder().setDisplayName(equipDis + "-VAV-" + "stageDownTimerCounter")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef)
                                                         .setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("vav")
                                                         .addMarker("default").addMarker("writable").addMarker("his")
                                                         .addMarker("stageDown")
                                                         .addMarker("timer").addMarker("counter").addMarker("sp")
                                                         .setMinVal("0")
                                                         .setMaxVal("30")
                                                         .setIncrementVal("1")
                                                         .setUnit("m")
                                                         .setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                         .setTz(tz)
                                                         .build();
        String stageDownTimerCounterId = hayStack.addPoint(stageDownTimerCounter);
        hayStack.writePointForCcuUser(stageDownTimerCounterId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                      DEFAULT_STAGE_DOWN_TIMER_COUNTER, 0);
        hayStack.writeHisValById(stageDownTimerCounterId, DEFAULT_STAGE_DOWN_TIMER_COUNTER);
    }
    
    public static void addVavEquipTuners(CCUHsApi hayStack, String siteRef, String equipdis, String equipref,
                                         String roomRef, String floorRef, String tz) {
        
        Log.d("CCU", "addVavEquipTuners for " + equipdis);
    
        ZoneTuners.addZoneTunersForEquip(hayStack, siteRef, equipdis, equipref, roomRef, floorRef, tz);
        Point zonePrioritySpread = new Point.Builder()
                                       .setDisplayName(equipdis+"-"+"zonePrioritySpread")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipref)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                       .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                                       .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                           .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                           .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                           .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                              .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                              .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10").setIncrementVal("0.5").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                        .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                        .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                              .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                              .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                        .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                        .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                             .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                             .addMarker("pgain").addMarker("sp")
                             .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                 .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                 .addMarker("igain").addMarker("sp")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                               .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                               .addMarker("pspread").addMarker("sp")
                               .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                    .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                    .addMarker("itimeout").addMarker("sp")
                                    .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                    .setUnit("m")
                                    .setTz(tz)
                                    .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        BuildingTunerUtil.updateTunerLevels(iTimeoutId, roomRef, hayStack);
        hayStack.writeHisValById(iTimeoutId, HSUtil.getPriorityVal(iTimeoutId));

        Point valveStartDamper = new Point.Builder()
                                     .setDisplayName(equipdis+"-"+"valveActuationStartDamperPosDuringSysHeating")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .setRoomRef(roomRef)
                                     .setFloorRef(floorRef).setHisInterpolate("cov")
                                     .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                     .addMarker("valve").addMarker("start").addMarker("damper").addMarker("sp")
                                     .setMinVal("0").setMaxVal("100").setIncrementVal("5").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                     .setUnit("%")
                                     .setTz(tz)
                                     .build();
        String valveStartDamperId = hayStack.addPoint(valveStartDamper);
        BuildingTunerUtil.updateTunerLevels(valveStartDamperId, roomRef, hayStack);
        hayStack.writeHisValById(valveStartDamperId, HSUtil.getPriorityVal(valveStartDamperId));

        Point zoneCO2Target = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"zoneCO2Target")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                  .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                  .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setUnit("ppm")
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
                                     .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                     .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                     .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                     .setUnit("ppm")
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
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("sp")
                                  .addMarker("zone").addMarker("voc").addMarker("target")
                                  .setUnit("ppb")
                                  .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                     .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                     .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                     .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                     .setUnit("ppb")
                                     .setTz(tz)
                                     .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);
        BuildingTunerUtil.updateTunerLevels(zoneVOCThresholdId, roomRef, hayStack);
        hayStack.writeHisValById(zoneVOCThresholdId, HSUtil.getPriorityVal(zoneVOCThresholdId));
        
        Point reheatZoneToDATMinDifferential  = createReheatZoneToDATMinDifferentialTuner(false, equipdis, equipref,
                                                                                          siteRef, tz );
        String reheatZoneToDATMinDifferentialId = hayStack.addPoint(reheatZoneToDATMinDifferential);
        BuildingTunerUtil.updateTunerLevels(reheatZoneToDATMinDifferentialId, roomRef, hayStack);
        hayStack.writeHisValById(reheatZoneToDATMinDifferentialId, HSUtil.getPriorityVal(reheatZoneToDATMinDifferentialId));
    }
    
    public static Point createReheatZoneToDATMinDifferentialTuner(boolean defaultTuner, String equipDis,
                                                                  String equipRef, String siteRef, String tz) {
        Point reheatZoneToDATMinDifferential  = new Point.Builder()
                                                    .setDisplayName(equipDis + "-VAV-"+"reheatZoneToDATMinDifferential ")
                                                    .setSiteRef(siteRef)
                                                    .setEquipRef(equipRef)
                                                    .setHisInterpolate("cov")
                                                    .addMarker("tuner").addMarker("writable").addMarker("his")
                                                    .addMarker("reheat").addMarker("dat").addMarker("min").addMarker("differential").addMarker("sp")
                                                    .addMarker(defaultTuner ? Tags.DEFAULT : Tags.VAV)
                                                    .setMinVal("0")
                                                    .setMaxVal("20")
                                                    .setIncrementVal("0.5")
                                                    .setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                    .setUnit("\u00B0F")
                                                    .setTz(tz)
                                                    .build();
        
        return reheatZoneToDATMinDifferential;
    }
}
