package a75f.io.logic.tuners;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class DualDuctTuners {
    
    public static void addDefaultTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis, String tz){
        
        HashMap tuner = hayStack.read("point and tuner and default and dualDuct");
        if (tuner != null && tuner.size() > 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "Default DualDuct Tuner points already exist");
            return;
        }
        CcuLog.d(L.TAG_CCU_SYSTEM,"Default DualDuct Tuner does not exist. Create Now");
    
        Point zonePrioritySpread = new Point.Builder()
                                       .setDisplayName(equipDis+"-DualDuct-"+"zonePrioritySpread")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("default").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                       .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                                       .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                       .setTz(tz)
                                       .build();
        String zonePrioritySpreadId = hayStack.addPoint(zonePrioritySpread);
        hayStack.writePointForCcuUser(zonePrioritySpreadId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.ZONE_PRIORITY_SPREAD, 0);
        hayStack.writeHisValById(zonePrioritySpreadId, TunerConstants.ZONE_PRIORITY_SPREAD);
    
        Point zonePriorityMultiplier = new Point.Builder()
                                           .setDisplayName(equipDis+"-DualDuct-"+"zonePriorityMultiplier")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                           .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                           .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                           .setTz(tz)
                                           .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        hayStack.writePointForCcuUser(zonePriorityMultiplierId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.ZONE_PRIORITY_MULTIPLIER, 0);
        hayStack.writeHisValById(zonePriorityMultiplierId, TunerConstants.ZONE_PRIORITY_MULTIPLIER);
        
        Point coolingDb = new Point.Builder()
                              .setDisplayName(equipDis+"-DualDuct-"+"coolingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("default").addMarker("dualDuct").addMarker("writable").addMarker("his")
                              .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                              .setUnit("\u00B0F")
                              .setTz(tz)
                              .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        hayStack.writePointForCcuUser(coolingDbId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.DEFAULT_COOLING_DB, 0);
        hayStack.writeHisValById(coolingDbId, TunerConstants.DEFAULT_COOLING_DB);
        
        Point coolingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipDis+"-DualDuct-"+"coolingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                        .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        hayStack.writePointForCcuUser(coolingDbMultiplierId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.DEFAULT_COOLING_DB_MULTPLIER, 0);
        hayStack.writeHisValById(coolingDbMultiplierId, TunerConstants.DEFAULT_COOLING_DB_MULTPLIER);
        
        Point heatingDb = new Point.Builder()
                              .setDisplayName(equipDis+"-DualDuct-"+"heatingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("default").addMarker("dualDuct").addMarker("writable").addMarker("his")
                              .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                              .setUnit("\u00B0F")
                              .setTz(tz)
                              .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        hayStack.writePointForCcuUser(heatingDbId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.DEFAULT_HEATING_DB, 0);
        hayStack.writeHisValById(heatingDbId, TunerConstants.DEFAULT_HEATING_DB);
        
        Point heatingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipDis+"-DualDuct-"+"heatingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                        .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        hayStack.writePointForCcuUser(heatingDbMultiplierId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.DEFAULT_HEATING_DB_MULTIPLIER, 0);
        hayStack.writeHisValById(heatingDbMultiplierId, TunerConstants.DEFAULT_HEATING_DB_MULTIPLIER);
        
        Point propGain = new Point.Builder()
                             .setDisplayName(equipDis+"-DualDuct-"+"proportionalKFactor ")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("default").addMarker("dualDuct").addMarker("writable").addMarker("his")
                             .addMarker("pgain").addMarker("sp")
                             .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                             .setTz(tz)
                             .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePointForCcuUser(pgainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.DEFAULT_PROPORTIONAL_GAIN, 0);
        hayStack.writeHisValById(pgainId, TunerConstants.DEFAULT_PROPORTIONAL_GAIN);
        
        Point integralGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-DualDuct-"+"integralKFactor ")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("default").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                 .addMarker("igain").addMarker("sp")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                 .setTz(tz)
                                 .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePointForCcuUser(igainId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.DEFAULT_INTEGRAL_GAIN, 0);
        hayStack.writeHisValById(igainId, TunerConstants.DEFAULT_INTEGRAL_GAIN);
        
        Point propSpread = new Point.Builder()
                               .setDisplayName(equipDis+"-DualDuct-"+"temperatureProportionalRange ")
                               .setSiteRef(siteRef)
                               .setEquipRef(equipRef).setHisInterpolate("cov")
                               .addMarker("tuner").addMarker("default").addMarker("dualDuct").addMarker("writable").addMarker("his")
                               .addMarker("pspread").addMarker("sp")
                               .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                               .setTz(tz)
                               .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        hayStack.writePointForCcuUser(pSpreadId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.DEFAULT_PROPORTIONAL_SPREAD, 0);
        hayStack.writeHisValById(pSpreadId, TunerConstants.DEFAULT_PROPORTIONAL_SPREAD);
        
        Point integralTimeout = new Point.Builder()
                                    .setDisplayName(equipDis+"-DualDuct-"+"temperatureIntegralTime ")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef).setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker("default").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                    .addMarker("itimeout").addMarker("sp")
                                    .setUnit("m")
                                    .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                    .setTz(tz)
                                    .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePointForCcuUser(iTimeoutId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.DEFAULT_INTEGRAL_TIMEOUT, 0);
        hayStack.writeHisValById(iTimeoutId, TunerConstants.DEFAULT_INTEGRAL_TIMEOUT);
        
        Point zoneCO2Target  = new Point.Builder()
                                   .setDisplayName(equipDis+"-DualDuct-"+"zoneCO2Target")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef).setHisInterpolate("cov")
                                   .addMarker("tuner").addMarker("default").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                   .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                   .setUnit("ppm")
                                   .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                   .setTz(tz)
                                   .build();
        String zoneCO2TargetId = hayStack.addPoint(zoneCO2Target);
        hayStack.writePointForCcuUser(zoneCO2TargetId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.ZONE_CO2_TARGET, 0);
        hayStack.writeHisValById(zoneCO2TargetId, TunerConstants.ZONE_CO2_TARGET);
        
        Point zoneCO2Threshold  = new Point.Builder()
                                      .setDisplayName(equipDis+"-DualDuct-"+"zoneCO2Threshold")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("default").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                      .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                      .setUnit("ppm")
                                      .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                      .setTz(tz)
                                      .build();
        String zoneCO2ThresholdId = hayStack.addPoint(zoneCO2Threshold);
        hayStack.writePointForCcuUser(zoneCO2ThresholdId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.ZONE_CO2_THRESHOLD, 0);
        hayStack.writeHisValById(zoneCO2ThresholdId, TunerConstants.ZONE_CO2_THRESHOLD);
        
        Point zoneVOCTarget  = new Point.Builder()
                                   .setDisplayName(equipDis+"-DualDuct-"+"zoneVOCTarget")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef).setHisInterpolate("cov")
                                   .addMarker("tuner").addMarker("default").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                   .addMarker("zone").addMarker("voc").addMarker("target").addMarker("sp")
                                   .setUnit("ppb")
                                   .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                   .setTz(tz)
                                   .build();
        String zoneVOCTargetId = hayStack.addPoint(zoneVOCTarget);
        hayStack.writePointForCcuUser(zoneVOCTargetId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.ZONE_VOC_TARGET, 0);
        hayStack.writeHisValById(zoneVOCTargetId, TunerConstants.ZONE_VOC_TARGET);
        
        Point zoneVOCThreshold  = new Point.Builder()
                                      .setDisplayName(equipDis+"-DualDuct-"+"zoneVOCThreshold")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("default").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                      .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                      .setUnit("ppb")
                                      .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                      .setTz(tz)
                                      .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);
        hayStack.writePointForCcuUser(zoneVOCThresholdId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.ZONE_VOC_THRESHOLD, 0);
        hayStack.writeHisValById(zoneVOCThresholdId, TunerConstants.ZONE_VOC_THRESHOLD);
    }
    
    
    public static void addEquipTuners(CCUHsApi hayStack, String siteRef, String equipdis, String equipref,
                                      String roomRef, String floorRef, String tz) {
        
        Log.d("CCU", "addEquipDualDuctTuners for " + equipdis);
        
        ZoneTuners.addZoneTunersForEquip(hayStack, siteRef, equipdis, equipref, roomRef, floorRef, tz);
    
        List<HisItem> hisItems = new ArrayList<>();
        Point zonePrioritySpread = new Point.Builder()
                                       .setDisplayName(equipdis+"-"+"zonePrioritySpread")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipref)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                       .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                                       .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                       .setTz(tz)
                                       .build();
        String zonePrioritySpreadId = hayStack.addPoint(zonePrioritySpread);
        BuildingTunerUtil.updateTunerLevels(zonePrioritySpreadId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(zonePrioritySpreadId));
    
        Point zonePriorityMultiplier = new Point.Builder()
                                           .setDisplayName(equipdis+"-"+"zonePriorityMultiplier")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipref)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                           .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                           .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                           .setTz(tz)
                                           .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        BuildingTunerUtil.updateTunerLevels(zonePriorityMultiplierId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(zonePriorityMultiplierId));
    
        
        Point coolingDb = new Point.Builder()
                              .setDisplayName(equipdis+"-"+"coolingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipref)
                              .setRoomRef(roomRef)
                              .setFloorRef(floorRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("dualDuct").addMarker("writable").addMarker("his")
                              .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                              .setTz(tz)
                              .setUnit("\u00B0F")
                              .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        BuildingTunerUtil.updateTunerLevels(coolingDbId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(coolingDbId));
        
        Point coolingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"coolingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .setRoomRef(roomRef)
                                        .setFloorRef(floorRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                        .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        BuildingTunerUtil.updateTunerLevels(coolingDbMultiplierId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(coolingDbMultiplierId));
        
        Point heatingDb = new Point.Builder()
                              .setDisplayName(equipdis+"-"+"heatingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipref)
                              .setRoomRef(roomRef)
                              .setFloorRef(floorRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("dualDuct").addMarker("writable").addMarker("his")
                              .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                              .setTz(tz)
                              .setUnit("\u00B0F")
                              .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        BuildingTunerUtil.updateTunerLevels(heatingDbId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(heatingDbId));
        
        Point heatingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"heatingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .setRoomRef(roomRef)
                                        .setFloorRef(floorRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                        .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        BuildingTunerUtil.updateTunerLevels(heatingDbMultiplierId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(heatingDbMultiplierId));
        
        Point propGain = new Point.Builder()
                             .setDisplayName(equipdis+"-"+"proportionalKFactor")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipref)
                             .setRoomRef(roomRef)
                             .setFloorRef(floorRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("dualDuct").addMarker("writable").addMarker("his")
                             .addMarker("pgain").addMarker("sp")
                             .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                             .setTz(tz)
                             .build();
        String pgainId = hayStack.addPoint(propGain);
        BuildingTunerUtil.updateTunerLevels(pgainId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(pgainId));
        
        Point integralGain = new Point.Builder()
                                 .setDisplayName(equipdis+"-"+"integralKFactor")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .setRoomRef(roomRef)
                                 .setFloorRef(floorRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                 .addMarker("igain").addMarker("sp")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                 .setTz(tz)
                                 .build();
        String igainId = hayStack.addPoint(integralGain);
        BuildingTunerUtil.updateTunerLevels(igainId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(igainId));
        
        Point propSpread = new Point.Builder()
                               .setDisplayName(equipdis+"-"+"temperatureProportionalRange")
                               .setSiteRef(siteRef)
                               .setEquipRef(equipref)
                               .setRoomRef(roomRef)
                               .setFloorRef(floorRef).setHisInterpolate("cov")
                               .addMarker("tuner").addMarker("dualDuct").addMarker("writable").addMarker("his")
                               .addMarker("pspread").addMarker("sp")
                               .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                               .setTz(tz)
                               .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        BuildingTunerUtil.updateTunerLevels(pSpreadId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(pSpreadId));
        
        Point integralTimeout = new Point.Builder()
                                    .setDisplayName(equipdis+"-"+"temperatureIntegralTime")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipref)
                                    .setRoomRef(roomRef)
                                    .setFloorRef(floorRef).setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                    .addMarker("itimeout").addMarker("sp")
                                    .setUnit("m")
                                    .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                    .setTz(tz)
                                    .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        BuildingTunerUtil.updateTunerLevels(iTimeoutId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(iTimeoutId));
        
        Point zoneCO2Target = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"zoneCO2Target")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                  .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                  .setUnit("ppm")
                                  .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                  .setTz(tz)
                                  .build();
        String zoneCO2TargetId = hayStack.addPoint(zoneCO2Target);
        BuildingTunerUtil.updateTunerLevels(zoneCO2TargetId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(zoneCO2TargetId));
        
        Point zoneCO2Threshold = new Point.Builder()
                                     .setDisplayName(equipdis+"-"+"zoneCO2Threshold")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .setRoomRef(roomRef)
                                     .setFloorRef(floorRef).setHisInterpolate("cov")
                                     .addMarker("tuner").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                     .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                     .setUnit("ppm")
                                     .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                     .setTz(tz)
                                     .build();
        String zoneCO2ThresholdId = hayStack.addPoint(zoneCO2Threshold);
        BuildingTunerUtil.updateTunerLevels(zoneCO2ThresholdId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(zoneCO2ThresholdId));
        
        Point zoneVOCTarget = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"zoneVOCTarget")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("dualDuct").addMarker("writable").addMarker("his").addMarker("sp")
                                  .addMarker("zone").addMarker("voc").addMarker("target")
                                  .setUnit("ppb")
                                  .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                  .setTz(tz)
                                  .build();
        String zoneVOCTargetId = hayStack.addPoint(zoneVOCTarget);
        BuildingTunerUtil.updateTunerLevels(zoneVOCTargetId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(zoneVOCTargetId));
        
        Point zoneVOCThreshold = new Point.Builder()
                                     .setDisplayName(equipdis+"-"+"zoneVOCThreshold")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .setRoomRef(roomRef)
                                     .setFloorRef(floorRef).setHisInterpolate("cov")
                                     .addMarker("tuner").addMarker("dualDuct").addMarker("writable").addMarker("his")
                                     .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                     .setUnit("ppb")
                                     .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.DUAL_DUCT_TUNER_GROUP)
                                     .setTz(tz)
                                     .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);
        BuildingTunerUtil.updateTunerLevels(zoneVOCThresholdId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(zoneVOCThresholdId));
    
        hayStack.writeHisValueByIdWithoutCOV(hisItems);
    }
}
