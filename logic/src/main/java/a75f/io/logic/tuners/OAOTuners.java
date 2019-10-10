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
                                             .setEquipRef(equipRef)
                                             .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                             .addMarker("co2").addMarker("damper").addMarker("opening").addMarker("rate")
                                             .setTz(tz)
                                             .build();
        String co2DamperOpeningRateId = hayStack.addPoint(co2DamperOpeningRate);
        hayStack.writePoint(co2DamperOpeningRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_CO2_DAMPER_OPENING_RATE, 0);
        hayStack.writeHisValById(co2DamperOpeningRateId, TunerConstants.OAO_CO2_DAMPER_OPENING_RATE);
        
        Point enthalpyDuctCompensationOffset = new Point.Builder()
                                                       .setDisplayName(equipDis+"-OAO-"+"enthalpyDuctCompensationOffset")
                                                       .setSiteRef(siteRef)
                                                       .setEquipRef(equipRef)
                                                       .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                       .addMarker("enthalpy").addMarker("duct").addMarker("compensation").addMarker("offset")
                                                       .setTz(tz)
                                                       .build();
        String enthalpyDuctCompensationOffsetId = hayStack.addPoint(enthalpyDuctCompensationOffset);
        hayStack.writePoint(enthalpyDuctCompensationOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_ENTHALPY_DUCT_COMPENSATION_OFFSET, 0);
        hayStack.writeHisValById(enthalpyDuctCompensationOffsetId, TunerConstants.OAO_ENTHALPY_DUCT_COMPENSATION_OFFSET);
        
        Point economizingMinTemperature = new Point.Builder()
                                                  .setDisplayName(equipDis+"-OAO-"+"economizingMinTemperature")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipRef)
                                                  .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                  .addMarker("economizing").addMarker("min").addMarker("temp")
                                                  .setUnit("\u00B0F")
                                                  .setTz(tz)
                                                  .build();
        String economizingMinTemperatureId = hayStack.addPoint(economizingMinTemperature);
        hayStack.writePoint(economizingMinTemperatureId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_ECONOMIZING_MIN_TEMP, 0);
        hayStack.writeHisValById(economizingMinTemperatureId, TunerConstants.OAO_ECONOMIZING_MIN_TEMP);
        
        Point economizingMaxTemperature = new Point.Builder()
                                                  .setDisplayName(equipDis+"-OAO-"+"economizingMaxTemperature")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipRef)
                                                  .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                  .addMarker("economizing").addMarker("max").addMarker("temp")
                                                  .setUnit("\u00B0F")
                                                  .setTz(tz)
                                                  .build();
        String economizingMaxTemperatureId = hayStack.addPoint(economizingMaxTemperature);
        hayStack.writePoint(economizingMaxTemperatureId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_ECONOMIZING_MAX_TEMP, 0);
        hayStack.writeHisValById(economizingMaxTemperatureId, TunerConstants.OAO_ECONOMIZING_MAX_TEMP);
        
        Point economizingMinHumidity = new Point.Builder()
                                               .setDisplayName(equipDis+"-OAO-"+"economizingMinHumidity")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef)
                                               .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .addMarker("economizing").addMarker("min").addMarker("humidity")
                                               .setUnit("%")
                                               .setTz(tz)
                                               .build();
        String economizingMinHumidityId = hayStack.addPoint(economizingMinHumidity);
        hayStack.writePoint(economizingMinHumidityId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_ECONOMIZING_MIN_HUMIDITY, 0);
        hayStack.writeHisValById(economizingMinHumidityId, TunerConstants.OAO_ECONOMIZING_MIN_HUMIDITY);
        
        Point economizingMaxHumidity = new Point.Builder()
                                               .setDisplayName(equipDis+"-OAO-"+"economizingMaxHumidity")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef)
                                               .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .addMarker("economizing").addMarker("max").addMarker("humidity")
                                               .setUnit("%")
                                               .setTz(tz)
                                               .build();
        String economizingMaxHumidityId = hayStack.addPoint(economizingMaxHumidity);
        hayStack.writePoint(economizingMaxHumidityId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_ECONOMIZING_MAX_HUMIDITY, 0);
        hayStack.writeHisValById(economizingMaxHumidityId, TunerConstants.OAO_ECONOMIZING_MAX_HUMIDITY);
        
        Point outsideDamperMixedAirTarget  = new Point.Builder()
                                                     .setDisplayName(equipDis+"-OAO-"+"outsideDamperMixedAirTarget")
                                                     .setSiteRef(siteRef)
                                                     .setEquipRef(equipRef)
                                                     .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                     .addMarker("outside").addMarker("damper").addMarker("mat").addMarker("target")
                                                     .setUnit("\u00B0F")
                                                     .setTz(tz)
                                                     .build();
        String outsideDamperMixedAirTargetId = hayStack.addPoint(outsideDamperMixedAirTarget);
        hayStack.writePoint(outsideDamperMixedAirTargetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_OA_DAMPER_MAT_TARGET, 0);
        hayStack.writeHisValById(outsideDamperMixedAirTargetId, TunerConstants.OAO_OA_DAMPER_MAT_TARGET);
        
        Point outsideDamperMixedAirMinimum  = new Point.Builder()
                                                      .setDisplayName(equipDis+"-OAO-"+"outsideDamperMixedAirMinimum")
                                                      .setSiteRef(siteRef)
                                                      .setEquipRef(equipRef)
                                                      .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                      .addMarker("outside").addMarker("damper").addMarker("mat").addMarker("min")
                                                      .setUnit("\u00B0F")
                                                      .setTz(tz)
                                                      .build();
        String outsideDamperMixedAirMinimumId = hayStack.addPoint(outsideDamperMixedAirMinimum);
        hayStack.writePoint(outsideDamperMixedAirMinimumId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_OA_DAMPER_MAT_MIN, 0);
        hayStack.writeHisValById(outsideDamperMixedAirMinimumId, TunerConstants.OAO_OA_DAMPER_MAT_MIN);
        
        Point economizingToMainCoolingLoopMap  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-OAO-"+"economizingToMainCoolingLoopMap")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef)
                                                         .addMarker("tuner").addMarker("default").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                         .addMarker("economizing").addMarker("main").addMarker("cooling").addMarker("loop").addMarker("map")
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String economizingToMainCoolingLoopMapId = hayStack.addPoint(economizingToMainCoolingLoopMap);
        hayStack.writePoint(economizingToMainCoolingLoopMapId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.OAO_ECONOMIZING_TO_MAIN_COOLING_LOOP_MAP, 0);
        hayStack.writeHisValById(economizingToMainCoolingLoopMapId, TunerConstants.OAO_ECONOMIZING_TO_MAIN_COOLING_LOOP_MAP);
        
    }
    
    public static void addEquipTuners(String equipdis, String siteRef, String equipref, String tz) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Point co2DamperOpeningRate = new Point.Builder()
                                           .setDisplayName(equipdis+"-"+"co2DamperOpeningRate")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipref)
                                           .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("co2").addMarker("damper").addMarker("opening").addMarker("rate")
                                           .setTz(tz)
                                           .build();
        String co2DamperOpeningRateId = hayStack.addPoint(co2DamperOpeningRate);
        HashMap co2DamperOpeningRatePoint = hayStack.read("point and tuner and default and oao and co2 and damper and opening and rate");
        ArrayList<HashMap> co2DamperOpeningRatePointArr = hayStack.readPoint(co2DamperOpeningRatePoint.get("id").toString());
        for (HashMap valMap : co2DamperOpeningRatePointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(co2DamperOpeningRateId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(co2DamperOpeningRateId, HSUtil.getPriorityVal(co2DamperOpeningRateId));
        
        Point enthalpyDuctCompensationOffset = new Point.Builder()
                                             .setDisplayName(equipdis+"-"+"enthalpyDuctCompensationOffset")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                             .addMarker("enthalpy").addMarker("duct").addMarker("compensation").addMarker("offset")
                                             .setTz(tz)
                                             .build();
        String enthalpyDuctCompensationOffsetId = hayStack.addPoint(enthalpyDuctCompensationOffset);
        HashMap enthalpyDuctCompensationOffsetPoint = hayStack.read("point and tuner and default and oao and enthalpy and duct and compensation and offset");
        ArrayList<HashMap> enthalpyDuctCompensationOffsetPointArr = hayStack.readPoint(enthalpyDuctCompensationOffsetPoint.get("id").toString());
        for (HashMap valMap : enthalpyDuctCompensationOffsetPointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(enthalpyDuctCompensationOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(enthalpyDuctCompensationOffsetId, HSUtil.getPriorityVal(enthalpyDuctCompensationOffsetId));
    
        Point economizingMinTemperature = new Point.Builder()
                                                       .setDisplayName(equipdis+"-"+"economizingMinTemperature")
                                                       .setSiteRef(siteRef)
                                                       .setEquipRef(equipref)
                                                       .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                       .addMarker("economizing").addMarker("min").addMarker("temp")
                                                       .setUnit("\u00B0F")
                                                       .setTz(tz)
                                                       .build();
        String economizingMinTemperatureId = hayStack.addPoint(economizingMinTemperature);
        HashMap economizingMinTemperaturePoint = hayStack.read("point and tuner and default and oao and economizing and min and temp");
        ArrayList<HashMap> economizingMinTemperaturePointArr = hayStack.readPoint(economizingMinTemperaturePoint.get("id").toString());
        for (HashMap valMap : economizingMinTemperaturePointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(economizingMinTemperatureId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(economizingMinTemperatureId, HSUtil.getPriorityVal(economizingMinTemperatureId));
    
        Point economizingMaxTemperature = new Point.Builder()
                                                  .setDisplayName(equipdis+"-"+"economizingMaxTemperature")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipref)
                                                  .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                  .addMarker("economizing").addMarker("max").addMarker("temp")
                                                  .setUnit("\u00B0F")
                                                  .setTz(tz)
                                                  .build();
        String economizingMaxTemperatureId = hayStack.addPoint(economizingMaxTemperature);
        HashMap economizingMaxTemperaturePoint = hayStack.read("point and tuner and default and oao and economizing and max and temp");
        ArrayList<HashMap> economizingMaxTemperaturePointArr = hayStack.readPoint(economizingMaxTemperaturePoint.get("id").toString());
        for (HashMap valMap : economizingMaxTemperaturePointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(economizingMaxTemperatureId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(economizingMaxTemperatureId, HSUtil.getPriorityVal(economizingMaxTemperatureId));
    
        Point economizingMinHumidity = new Point.Builder()
                                                  .setDisplayName(equipdis+"-"+"economizingMinHumidity")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipref)
                                                  .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                  .addMarker("economizing").addMarker("min").addMarker("humidity")
                                                  .setUnit("%")
                                                  .setTz(tz)
                                                  .build();
        String economizingMinHumidityId = hayStack.addPoint(economizingMinHumidity);
        HashMap economizingMinHumidityPoint = hayStack.read("point and tuner and default and oao and economizing and min and humidity");
        ArrayList<HashMap> economizingMinHumidityPointArr = hayStack.readPoint(economizingMinHumidityPoint.get("id").toString());
        for (HashMap valMap : economizingMinHumidityPointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(economizingMinHumidityId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(economizingMinHumidityId, HSUtil.getPriorityVal(economizingMinHumidityId));
    
        Point economizingMaxHumidity = new Point.Builder()
                                               .setDisplayName(equipdis+"-"+"economizingMaxHumidity")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .addMarker("economizing").addMarker("max").addMarker("humidity")
                                               .setUnit("%")
                                               .setTz(tz)
                                               .build();
        String economizingMaxHumidityId = hayStack.addPoint(economizingMaxHumidity);
        HashMap economizingMaxHumidityPoint = hayStack.read("point and tuner and default and oao and economizing and max and humidity");
        ArrayList<HashMap> economizingMaxHumidityPointArr = hayStack.readPoint(economizingMaxHumidityPoint.get("id").toString());
        for (HashMap valMap : economizingMaxHumidityPointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(economizingMaxHumidityId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(economizingMaxHumidityId, HSUtil.getPriorityVal(economizingMaxHumidityId));
    
        Point outsideDamperMixedAirTarget = new Point.Builder()
                                               .setDisplayName(equipdis+"-"+"outsideDamperMixedAirTarget")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .addMarker("outside").addMarker("damper").addMarker("mat").addMarker("target")
                                               .setUnit("\u00B0F")
                                               .setTz(tz)
                                               .build();
        String outsideDamperMixedAirTargetId = hayStack.addPoint(outsideDamperMixedAirTarget);
        HashMap outsideDamperMixedAirTargetPoint = hayStack.read("point and tuner and default and oao and outside and damper and mat and target");
        ArrayList<HashMap> outsideDamperMixedAirTargetPointArr = hayStack.readPoint(outsideDamperMixedAirTargetPoint.get("id").toString());
        for (HashMap valMap : outsideDamperMixedAirTargetPointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(outsideDamperMixedAirTargetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(outsideDamperMixedAirTargetId, HSUtil.getPriorityVal(outsideDamperMixedAirTargetId));
    
        Point outsideDamperMixedAirMinimum = new Point.Builder()
                                                    .setDisplayName(equipdis+"-"+"outsideDamperMixedAirMinimum")
                                                    .setSiteRef(siteRef)
                                                    .setEquipRef(equipref)
                                                    .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                    .addMarker("outside").addMarker("damper").addMarker("mat").addMarker("minimum")
                                                    .setUnit("\u00B0F")
                                                    .setTz(tz)
                                                    .build();
        String outsideDamperMixedAirMinimumId = hayStack.addPoint(outsideDamperMixedAirMinimum);
        HashMap outsideDamperMixedAirMinimumPoint = hayStack.read("point and tuner and default and oao and outside and damper and mat and min");
        ArrayList<HashMap> outsideDamperMixedAirMinimumPointArr = hayStack.readPoint(outsideDamperMixedAirMinimumPoint.get("id").toString());
        for (HashMap valMap : outsideDamperMixedAirMinimumPointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(outsideDamperMixedAirMinimumId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(outsideDamperMixedAirMinimumId, HSUtil.getPriorityVal(outsideDamperMixedAirMinimumId));
    
        Point economizingToMainCoolingLoopMap = new Point.Builder()
                                                     .setDisplayName(equipdis+"-"+"economizingToMainCoolingLoopMap")
                                                     .setSiteRef(siteRef)
                                                     .setEquipRef(equipref)
                                                     .addMarker("tuner").addMarker("oao").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                     .addMarker("economizing").addMarker("main").addMarker("cooling").addMarker("loop").addMarker("map")
                                                     .setUnit("%")
                                                     .setTz(tz)
                                                     .build();
        String economizingToMainCoolingLoopMapId = hayStack.addPoint(economizingToMainCoolingLoopMap);
        HashMap economizingToMainCoolingLoopMapPoint = hayStack.read("point and tuner and default and oao and economizing and main and cooling and loop and map");
        ArrayList<HashMap> economizingToMainCoolingLoopMapPointArr = hayStack.readPoint(economizingToMainCoolingLoopMapPoint.get("id").toString());
        for (HashMap valMap : economizingToMainCoolingLoopMapPointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(economizingToMainCoolingLoopMapId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(economizingToMainCoolingLoopMapId, HSUtil.getPriorityVal(economizingToMainCoolingLoopMapId));
        
    }
}
