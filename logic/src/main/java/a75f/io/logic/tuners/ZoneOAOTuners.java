package a75f.io.logic.tuners;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;

public class ZoneOAOTuners
{
    
    public static void addDefaultTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                        String tz) {
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
        hayStack.writePointForCcuUser(co2DamperOpeningRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.OAO_CO2_DAMPER_OPENING_RATE, 0);
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
        hayStack.writePointForCcuUser(enthalpyDuctCompensationOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.OAO_ENTHALPY_DUCT_COMPENSATION_OFFSET, 0);
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
        hayStack.writePointForCcuUser(economizingMinTemperatureId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.OAO_ECONOMIZING_MIN_TEMP, 0);
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
        hayStack.writePointForCcuUser(economizingMaxTemperatureId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.OAO_ECONOMIZING_MAX_TEMP, 0);
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
        hayStack.writePointForCcuUser(economizingMinHumidityId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.OAO_ECONOMIZING_MIN_HUMIDITY, 0);
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
        hayStack.writePointForCcuUser(economizingMaxHumidityId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.OAO_ECONOMIZING_MAX_HUMIDITY, 0);
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
        hayStack.writePointForCcuUser(outsideDamperMixedAirTargetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.OAO_OA_DAMPER_MAT_TARGET, 0);
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
        hayStack.writePointForCcuUser(outsideDamperMixedAirMinimumId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.OAO_OA_DAMPER_MAT_MIN, 0);
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
        hayStack.writePointForCcuUser(economizingToMainCoolingLoopMapId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.OAO_ECONOMIZING_TO_MAIN_COOLING_LOOP_MAP, 0);
        hayStack.writeHisValById(economizingToMainCoolingLoopMapId, TunerConstants.OAO_ECONOMIZING_TO_MAIN_COOLING_LOOP_MAP);
        
        updateNewTuners(hayStack, siteRef,equipRef, equipDis,tz,true);
    }

    public static void updateNewTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis, String tz,
                                       boolean isNewSite){

        if(isNewSite || !verifyPointsAvailability("default","economizing and dry and bulb and threshold",equipRef)) {
            
            Point economizingDryBulbThreshold  = new Point.Builder()
                                                     .setDisplayName(equipDis+"-OAO-"+"economizingDryBulbThreshold")
                                                     .setSiteRef(siteRef)
                                                     .setEquipRef(equipRef).setHisInterpolate("cov")
                                                     .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his")
                                                     .addMarker("economizing").addMarker("dry").addMarker("bulb").addMarker("threshold")
                                                     .setMinVal("0").setMaxVal("70").setIncrementVal("0.5").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                                                     .setUnit("\u00B0F")
                                                     .setTz(tz)
                                                     .build();
            String economizingDryBulbThresholdId = hayStack.addPoint(economizingDryBulbThreshold);
            hayStack.writePointForCcuUser(economizingDryBulbThresholdId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.OAO_ECONOMIZING_DRY_BULB_THRESHOLD, 0);
            hayStack.writeHisValById(economizingDryBulbThresholdId, TunerConstants.OAO_ECONOMIZING_DRY_BULB_THRESHOLD);
        
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
    public static void updateOaoSystemTuners(CCUHsApi hayStack, String siteRef, String equipref, String equipdis,
                                             String tz,String systemProfile) {
        
        if (!verifyPointsAvailability("not default","co2 and damper and opening and rate",equipref)) {
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
        if (!verifyPointsAvailability("not default","economizing and min and temp",equipref)) {
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
                    .addMarker("outside").addMarker("damper").addMarker("mat").addMarker("min").addMarker("system")
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
    
        if (!verifyPointsAvailability("not default","economizing and dry and bulb and threshold",equipref)) {
            Point economizingDryBulbThreshold = new Point.Builder()
                                                       .setDisplayName(equipdis + "-OAO-" + "economizingDryBulbThreshold")
                                                       .setSiteRef(siteRef)
                                                       .setEquipRef(equipref).setHisInterpolate("cov")
                                                       .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his")
                                                       .addMarker("economizing").addMarker("dry").addMarker("bulb").addMarker("threshold").addMarker("system")
                                                       .setMinVal("0").setMaxVal("70").setIncrementVal("0.5").setTunerGroup(TunerConstants.OAO_TUNER_GROUP)
                                                       .setUnit("\u00B0F")
                                                       .setTz(tz)
                                                       .build();
            String economizingDryBulbThresholdId = hayStack.addPoint(economizingDryBulbThreshold);
            HashMap economizingDryBulbThresholdPoint = hayStack.read("point and tuner and default and oao and " +
                                                                        "economizing and dry and bulb and threshold");
            //Just in case BuildingTuner is not initialized during upgrades on non-primary CCUs, initialize with
            // default values
            if (economizingDryBulbThresholdPoint.isEmpty()) {
                hayStack.writePointForCcuUser(economizingDryBulbThresholdId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.OAO_ECONOMIZING_DRY_BULB_THRESHOLD, 0);
                hayStack.writeHisValById(economizingDryBulbThresholdId, TunerConstants.OAO_ECONOMIZING_DRY_BULB_THRESHOLD);
            } else {
                ArrayList<HashMap> economizingDryBulbThresholdPointArr = hayStack.readPoint(economizingDryBulbThresholdPoint.get("id").toString());
                for (HashMap valMap : economizingDryBulbThresholdPointArr) {
                    if (valMap.get("val") != null) {
                        hayStack.pointWrite(HRef.copy(economizingDryBulbThresholdId), (int) Double.parseDouble(valMap.get("level").toString()),
                                            valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                    }
                }
                hayStack.writeHisValById(economizingDryBulbThresholdId, HSUtil.getPriorityVal(economizingDryBulbThresholdId));
            }
    
        }
        
    }
    private static void deleteNonUsableSystemPoints(String tags, String equipref){
        HashMap deletablePoint = CCUHsApi.getInstance().read("point and tuner and system and oao and "+tags+" and equipRef == \"" + equipref + "\"");
        if (deletablePoint != null && deletablePoint.size() > 0) {
            CCUHsApi.getInstance().deleteEntity(deletablePoint.get("id").toString());
        }
    }
}
