package a75f.io.logic.tuners;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;

/**
 * Created by samjithsadasivan on 10/5/18.
 */

public class BuildingTuners
{
    
    private String equipRef;
    private String equipDis;
    private String siteRef;
    private String tz;
    CCUHsApi hayStack;
    
    private static BuildingTuners instance = null;
    private BuildingTuners(){
        addBuildingTunerEquip();
    }
    
    public static BuildingTuners getInstance() {
        if (instance == null) {
            instance = new BuildingTuners();
        }
        return instance;
    }
    
    public void addBuildingTunerEquip() {
        hayStack = CCUHsApi.getInstance();
        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        if (tuner != null && tuner.size() > 0) {
            equipRef = tuner.get("id").toString();
            equipDis = tuner.get("dis").toString();
            HashMap siteMap = hayStack.read(Tags.SITE);
            siteRef = siteMap.get(Tags.ID).toString();
            tz = siteMap.get("tz").toString();
            CcuLog.d(L.TAG_CCU_SYSTEM,"BuildingTuner equip already present");
            return;
        }
        CcuLog.d(L.TAG_CCU_SYSTEM,"BuildingTuner Equip does not exist. Create Now");
        HashMap siteMap = hayStack.read(Tags.SITE);
        siteRef = siteMap.get(Tags.ID).toString();
        String siteDis = siteMap.get("dis").toString();
        Equip tunerEquip= new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName(siteDis+"-BuildingTuner")
                          .addMarker("equip").addMarker("tuner").addMarker("equipHis")
                          .setTz(siteMap.get("tz").toString())
                          .build();
        equipRef = hayStack.addEquip(tunerEquip);
        equipDis = siteDis+"-BuildingTuner";
        tz = siteMap.get("tz").toString();
        
        addSettingTuners();
        addDefaultSystemTuners();
        addDefaultZoneTuners();
        addDefaultVavTuners();
        addDefaultPlcTuners();
        addDefaultStandaloneTuners();
        addDefaultDabTuners();
        
    }
    
    public void addSettingTuners() {
        Point forcedOccupiedTime = new Point.Builder()
                                                  .setDisplayName(equipDis+"-"+"forcedOccupiedTime")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipRef)
                                                  .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                  .addMarker("system").addMarker("forced").addMarker("occupied").addMarker("time").addMarker("sp")
                                                  .setTz(tz)
                                                  .build();
        String forcedOccupiedTimeId = hayStack.addPoint(forcedOccupiedTime);
        hayStack.writePoint(forcedOccupiedTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 120.0, 0);
        hayStack.writeHisValById(forcedOccupiedTimeId, 120.0);
    }
    
    public void addDefaultSystemTuners() {
        Point heatingPreconditioingRate = new Point.Builder()
                                           .setDisplayName(equipDis+"-"+"heatingPreconditioningRate")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef)
                                           .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("system").addMarker("heating").addMarker("precon").addMarker("rate").addMarker("sp")
                                           .setTz(tz)
                                           .build();
        String heatingPreconditioingRateId = hayStack.addPoint(heatingPreconditioingRate);
        hayStack.writePoint(heatingPreconditioingRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_PRECONDITION_RATE, 0);
        hayStack.writeHisValById(heatingPreconditioingRateId, TunerConstants.SYSTEM_PRECONDITION_RATE);
    
        Point coolingPreconditioingRate = new Point.Builder()
                                                  .setDisplayName(equipDis+"-"+"coolingPreconditioningRate")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipRef)
                                                  .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                  .addMarker("system").addMarker("cooling").addMarker("precon").addMarker("rate").addMarker("sp")
                                                  .setTz(tz)
                                                  .build();
        String coolingPreconditioingRateId = hayStack.addPoint(coolingPreconditioingRate);
        hayStack.writePoint(coolingPreconditioingRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_PRECONDITION_RATE, 0);
        hayStack.writeHisValById(coolingPreconditioingRateId, TunerConstants.SYSTEM_PRECONDITION_RATE);
    
        Point userLimitSpread = new Point.Builder()
                                                  .setDisplayName(equipDis+"-"+"userLimitSpread")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipRef)
                                                  .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                  .addMarker("system").addMarker("user").addMarker("limit").addMarker("spread").addMarker("sp")
                                                  .setTz(tz)
                                                  .build();
        String userLimitSpreadId = hayStack.addPoint(userLimitSpread);
        hayStack.writePoint(userLimitSpreadId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.USER_LIMIT_SPREAD, 0);
        hayStack.writeHisValById(userLimitSpreadId, TunerConstants.USER_LIMIT_SPREAD);
    
        Point buildingLimitMin = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"buildingLimitMin")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef)
                                        .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("system").addMarker("building").addMarker("limit").addMarker("min").addMarker("sp")
                                        .setTz(tz)
                                        .build();
        String buildingLimitMinId = hayStack.addPoint(buildingLimitMin);
        hayStack.writePoint(buildingLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.BUILDING_LIMIT_MIN, 0);
        hayStack.writeHisValById(buildingLimitMinId, TunerConstants.BUILDING_LIMIT_MIN);
    
        Point buildingLimitMax = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"buildingLimitMax")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef)
                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .addMarker("system").addMarker("building").addMarker("limit").addMarker("max").addMarker("sp")
                                         .setTz(tz)
                                         .build();
        String buildingLimitMaxId = hayStack.addPoint(buildingLimitMax);
        hayStack.writePoint(buildingLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.BUILDING_LIMIT_MAX, 0);
        hayStack.writeHisValById(buildingLimitMaxId, TunerConstants.BUILDING_LIMIT_MAX);
    
        Point buildingToZoneDifferential = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"buildingToZoneDifferential")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef)
                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .addMarker("system").addMarker("building").addMarker("zone").addMarker("differential").addMarker("sp")
                                         .setTz(tz)
                                         .build();
        String buildingToZoneDifferentialId = hayStack.addPoint(buildingToZoneDifferential);
        hayStack.writePoint(buildingToZoneDifferentialId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.BUILDING_TO_ZONE_DIFFERENTIAL, 0);
        hayStack.writeHisValById(buildingToZoneDifferentialId, TunerConstants.BUILDING_TO_ZONE_DIFFERENTIAL);
    
        Point zoneTemperatureDeadLeeway = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"zoneTemperatureDeadLeeway")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipRef)
                                                   .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                   .addMarker("system").addMarker("zone").addMarker("temp").addMarker("dead").addMarker("leeway").addMarker("sp")
                                                   .setTz(tz)
                                                   .build();
        String zoneTemperatureDeadLeewayId = hayStack.addPoint(zoneTemperatureDeadLeeway);
        hayStack.writePoint(zoneTemperatureDeadLeewayId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_TEMP_DEAD_LEEWAY, 0);
        hayStack.writeHisValById(zoneTemperatureDeadLeewayId, TunerConstants.ZONE_TEMP_DEAD_LEEWAY);
    
        Point humidityCompensationOffset = new Point.Builder()
                                                  .setDisplayName(equipDis+"-"+"humidityCompensationOffset")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipRef)
                                                  .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                  .addMarker("system").addMarker("humidity").addMarker("compensation").addMarker("offset").addMarker("sp")
                                                  .setTz(tz)
                                                  .build();
        String humidityCompensationOffsetId = hayStack.addPoint(humidityCompensationOffset);
        hayStack.writePoint(humidityCompensationOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 0.0, 0);
        hayStack.writeHisValById(humidityCompensationOffsetId, 0.0);
        
        
        
    }
    
    public void addDefaultZoneTuners() {
        Point unoccupiedZoneSetback  = new Point.Builder()
                                           .setDisplayName(equipDis+"-"+"unoccupiedZoneSetback")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef)
                                           .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("zone").addMarker("unoccupied").addMarker("setback").addMarker("sp")
                                           .setTz(tz)
                                           .build();
        String unoccupiedZoneSetbackId = hayStack.addPoint(unoccupiedZoneSetback);
        hayStack.writePoint(unoccupiedZoneSetbackId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_UNOCCUPIED_SETBACK, 0);
        hayStack.writeHisValById(unoccupiedZoneSetbackId, TunerConstants.ZONE_UNOCCUPIED_SETBACK);
    
        Point heatingUserLimitMin  = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"heatingUserLimitMin")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef)
                                               .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .addMarker("zone").addMarker("heating").addMarker("user").addMarker("limit").addMarker("min").addMarker("sp")
                                               .setTz(tz)
                                               .build();
        String heatingUserLimitMinId = hayStack.addPoint(heatingUserLimitMin);
        hayStack.writePoint(heatingUserLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_HEATING_USERLIMIT_MIN, 0);
        hayStack.writeHisValById(heatingUserLimitMinId, TunerConstants.ZONE_HEATING_USERLIMIT_MIN);
    
        Point heatingUserLimitMax  = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"heatingUserLimitMax")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipRef)
                                             .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                             .addMarker("zone").addMarker("heating").addMarker("user").addMarker("limit").addMarker("max").addMarker("sp")
                                             .setTz(tz)
                                             .build();
        String heatingUserLimitMaxId = hayStack.addPoint(heatingUserLimitMax);
        hayStack.writePoint(heatingUserLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_HEATING_USERLIMIT_MAX, 0);
        hayStack.writeHisValById(heatingUserLimitMaxId, TunerConstants.ZONE_HEATING_USERLIMIT_MAX);
    
        Point coolingUserLimitMin  = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"coolingUserLimitMin")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipRef)
                                             .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                             .addMarker("zone").addMarker("cooling").addMarker("user").addMarker("limit").addMarker("min").addMarker("sp")
                                             .setTz(tz)
                                             .build();
        String coolingUserLimitMinId = hayStack.addPoint(coolingUserLimitMin);
        hayStack.writePoint(coolingUserLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_COOLING_USERLIMIT_MIN, 0);
        hayStack.writeHisValById(coolingUserLimitMinId, TunerConstants.ZONE_COOLING_USERLIMIT_MIN);
    
        Point coolingUserLimitMax  = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"coolingUserLimitMax")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipRef)
                                             .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                             .addMarker("zone").addMarker("cooling").addMarker("user").addMarker("limit").addMarker("max").addMarker("sp")
                                             .setTz(tz)
                                             .build();
        String coolingUserLimitMaxId = hayStack.addPoint(coolingUserLimitMax);
        hayStack.writePoint(coolingUserLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_COOLING_USERLIMIT_MAX, 0);
        hayStack.writeHisValById(coolingUserLimitMaxId, TunerConstants.ZONE_COOLING_USERLIMIT_MAX);
    }
    
    public void addDefaultVavTuners() {
        
        HashMap tuner = CCUHsApi.getInstance().read("point and tuner and default and vav");
        if (tuner != null && tuner.size() > 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM,"Default VAV Tuner points already exist");
            return;
        }
        CcuLog.d(L.TAG_CCU_SYSTEM,"Default VAV Tuner  does not exist. Create Now");
    
        Point zonePrioritySpread = new Point.Builder()
                                  .setDisplayName(equipDis+"-VAV-"+"zonePrioritySpread")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                                  .setTz(tz)
                                  .build();
        String zonePrioritySpreadId = hayStack.addPoint(zonePrioritySpread);
        hayStack.writePoint(zonePrioritySpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_PRIORITY_SPREAD, 0);
        hayStack.writeHisValById(zonePrioritySpreadId, TunerConstants.ZONE_PRIORITY_SPREAD);
    
        Point zonePriorityMultiplier = new Point.Builder()
                                           .setDisplayName(equipDis+"-VAV-"+"zonePriorityMultiplier")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef)
                                           .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                           .setTz(tz)
                                           .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        hayStack.writePoint(zonePriorityMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_PRIORITY_MULTIPLIER, 0);
        hayStack.writeHisValById(zonePriorityMultiplierId, TunerConstants.ZONE_PRIORITY_MULTIPLIER);
        
        Point coolingDb = new Point.Builder()
                                  .setDisplayName(equipDis+"-VAV-"+"coolingDeadband")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        hayStack.writePoint(coolingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_COOLING_DB, 0);
        hayStack.writeHisValById(coolingDbId, TunerConstants.VAV_COOLING_DB);
    
        Point coolingDbMultiplier = new Point.Builder()
                                  .setDisplayName(equipDis+"-VAV-"+"coolingDeadbandMultiplier")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                  .setTz(tz)
                                  .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        hayStack.writePoint(coolingDbMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_COOLING_DB_MULTPLIER, 0);
        hayStack.writeHisValById(coolingDbMultiplierId, TunerConstants.VAV_COOLING_DB_MULTPLIER);
        
        Point heatingDb = new Point.Builder()
                                  .setDisplayName(equipDis+"-VAV-"+"heatingDeadband")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        hayStack.writePoint(heatingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_HEATING_DB, 0);
        hayStack.writeHisValById(heatingDbId, TunerConstants.VAV_HEATING_DB);
    
        Point heatingDbMultiplier = new Point.Builder()
                                  .setDisplayName(equipDis+"-VAV-"+"heatingDeadbandMultiplier")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                  .setTz(tz)
                                  .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        hayStack.writePoint(heatingDbMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_HEATING_DB_MULTIPLIER, 0);
        hayStack.writeHisValById(heatingDbMultiplierId, TunerConstants.VAV_HEATING_DB_MULTIPLIER);
        
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-VAV-"+"proportionalKFactor ")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .addMarker("pgain").addMarker("sp")
                                 .setTz(tz)
                                 .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePoint(pgainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_GAIN, 0);
        hayStack.writeHisValById(pgainId, TunerConstants.VAV_PROPORTIONAL_GAIN);
        
        Point integralGain = new Point.Builder()
                                     .setDisplayName(equipDis+"-VAV-"+"integralKFactor ")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef)
                                     .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .addMarker("igain").addMarker("sp")
                                     .setTz(tz)
                                     .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePoint(igainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_GAIN, 0);
        hayStack.writeHisValById(igainId, TunerConstants.VAV_INTEGRAL_GAIN);
        
        Point propSpread = new Point.Builder()
                                   .setDisplayName(equipDis+"-VAV-"+"temperatureProportionalRange ")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                   .addMarker("pspread").addMarker("sp")
                                   .setTz(tz)
                                   .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        hayStack.writePoint(pSpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_SPREAD, 0);
        hayStack.writeHisValById(pSpreadId, TunerConstants.VAV_PROPORTIONAL_SPREAD);
        
        Point integralTimeout = new Point.Builder()
                                        .setDisplayName(equipDis+"-VAV-"+"temperatureIntegralTime ")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef)
                                        .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("itimeout").addMarker("sp")
                                        .setTz(tz)
                                        .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePoint(iTimeoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_TIMEOUT, 0);
        hayStack.writeHisValById(iTimeoutId, TunerConstants.VAV_INTEGRAL_TIMEOUT);
    
        Point valveStartDamper  = new Point.Builder()
                                        .setDisplayName(equipDis+"-VAV-"+"valveActuationStartDamperPosDuringSysHeating")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef)
                                        .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("valve").addMarker("start").addMarker("damper").addMarker("sp")
                                        .setTz(tz)
                                        .build();
        String valveStartDamperId = hayStack.addPoint(valveStartDamper);
        hayStack.writePoint(valveStartDamperId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VALVE_START_DAMPER, 0);
        hayStack.writeHisValById(valveStartDamperId, TunerConstants.VALVE_START_DAMPER);
    
        Point zoneCO2Target  = new Point.Builder()
                                          .setDisplayName(equipDis+"-VAV-"+"zoneCO2Target")
                                          .setSiteRef(siteRef)
                                          .setEquipRef(equipRef)
                                          .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                          .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                          .setTz(tz)
                                          .build();
        String zoneCO2TargetId = hayStack.addPoint(zoneCO2Target);
        hayStack.writePoint(zoneCO2TargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_CO2_TARGET, 0);
        hayStack.writeHisValById(zoneCO2TargetId, TunerConstants.ZONE_CO2_TARGET);
    
        Point zoneCO2Threshold  = new Point.Builder()
                                       .setDisplayName(equipDis+"-VAV-"+"zoneCO2Threshold")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipRef)
                                       .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                       .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                       .setTz(tz)
                                       .build();
        String zoneCO2ThresholdId = hayStack.addPoint(zoneCO2Threshold);
        hayStack.writePoint(zoneCO2ThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_CO2_THRESHOLD, 0);
        hayStack.writeHisValById(zoneCO2ThresholdId, TunerConstants.ZONE_CO2_THRESHOLD);
    
        Point zoneVOCTarget  = new Point.Builder()
                                       .setDisplayName(equipDis+"-VAV-"+"zoneVOCTarget")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipRef)
                                       .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                       .addMarker("zone").addMarker("voc").addMarker("target").addMarker("sp")
                                       .setTz(tz)
                                       .build();
        String zoneVOCTargetId = hayStack.addPoint(zoneVOCTarget);
        hayStack.writePoint(zoneVOCTargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_VOC_TARGET, 0);
        hayStack.writeHisValById(zoneVOCTargetId, TunerConstants.ZONE_VOC_TARGET);
    
        Point zoneVOCThreshold  = new Point.Builder()
                                          .setDisplayName(equipDis+"-VAV-"+"zoneVOCThreshold")
                                          .setSiteRef(siteRef)
                                          .setEquipRef(equipRef)
                                          .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                          .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                          .setTz(tz)
                                          .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);
        hayStack.writePoint(zoneVOCThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_VOC_THRESHOLD, 0);
        hayStack.writeHisValById(zoneVOCThresholdId, TunerConstants.ZONE_VOC_THRESHOLD);
    
        addDefaultVavSystemTuners();
        
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    public void addDefaultVavSystemTuners()
    {
        Point targetCumulativeDamper = new Point.Builder()
                                               .setDisplayName(equipDis+"-VAV-"+"targetCumulativeDamper")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef)
                                               .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .addMarker("target").addMarker("cumulative").addMarker("damper").addMarker("sp")
                                               .setTz(tz)
                                               .build();
        String targetCumulativeDamperId = hayStack.addPoint(targetCumulativeDamper);
        hayStack.writePoint(targetCumulativeDamperId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.TARGET_CUMULATIVE_DAMPER, 0);
        hayStack.writeHisValById(targetCumulativeDamperId, TunerConstants.TARGET_CUMULATIVE_DAMPER);
    
        Point analogFanSpeedMultiplier = new Point.Builder()
                                               .setDisplayName(equipDis+"-VAV-"+"analogFanSpeedMultiplier")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef)
                                               .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .addMarker("analog").addMarker("fan").addMarker("speed").addMarker("multiplier").addMarker("sp")
                                               .setTz(tz)
                                               .build();
        String analogFanSpeedMultiplierId = hayStack.addPoint(analogFanSpeedMultiplier);
        hayStack.writePoint(analogFanSpeedMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ANALOG_FANSPEED_MULTIPLIER, 0);
        hayStack.writeHisValById(analogFanSpeedMultiplierId, TunerConstants.ANALOG_FANSPEED_MULTIPLIER);
    
        Point humidityHysteresis = new Point.Builder()
                                                 .setDisplayName(equipDis+"-VAV-"+"humidityHysteresis")
                                                 .setSiteRef(siteRef)
                                                 .setEquipRef(equipRef)
                                                 .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                 .addMarker("humidity").addMarker("hysteresis").addMarker("sp")
                                                 .setTz(tz)
                                                 .build();
        String humidityHysteresisId = hayStack.addPoint(humidityHysteresis);
        hayStack.writePoint(humidityHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.HUMIDITY_HYSTERESIS_PERCENT, 0);
        hayStack.writeHisValById(humidityHysteresisId, TunerConstants.HUMIDITY_HYSTERESIS_PERCENT);
    
        Point relayDeactivationHysteresis = new Point.Builder()
                                           .setDisplayName(equipDis+"-VAV-"+"relayDeactivationHysteresis")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef)
                                           .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("relay").addMarker("deactivation").addMarker("hysteresis").addMarker("sp")
                                           .setTz(tz)
                                           .build();
        String relayDeactivationHysteresisId = hayStack.addPoint(relayDeactivationHysteresis);
        hayStack.writePoint(relayDeactivationHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.RELAY_DEACTIVATION_HYSTERESIS, 0);
        hayStack.writeHisValById(relayDeactivationHysteresisId, TunerConstants.RELAY_DEACTIVATION_HYSTERESIS);
        
    }
    
    public void addEquipZoneTuners(String equipdis, String equipref) {
        Point unoccupiedZoneSetback = new Point.Builder()
                                           .setDisplayName(equipdis+"-"+"unoccupiedZoneSetback")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipref)
                                           .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("zone").addMarker("unoccupied").addMarker("setback").addMarker("sp")
                                           .setTz(tz)
                                           .build();
        String unoccupiedZoneSetbackId = hayStack.addPoint(unoccupiedZoneSetback);
        HashMap unoccupiedZoneSetbackPoint = hayStack.read("point and tuner and default and zone and unoccupied and setback");
        ArrayList<HashMap> unoccupiedZoneSetbackArr = hayStack.readPoint(unoccupiedZoneSetbackPoint.get("id").toString());
        for (HashMap valMap : unoccupiedZoneSetbackArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(unoccupiedZoneSetbackId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(unoccupiedZoneSetbackId, HSUtil.getPriorityVal(unoccupiedZoneSetbackId));
        
        Point heatingUserLimitMin = new Point.Builder()
                                              .setDisplayName(equipdis+"-"+"heatingUserLimitMin")
                                              .setSiteRef(siteRef)
                                              .setEquipRef(equipref)
                                              .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                                              .addMarker("zone").addMarker("heating").addMarker("user").addMarker("limit").addMarker("min").addMarker("sp")
                                              .setTz(tz)
                                              .build();
        String heatingUserLimitMinId = hayStack.addPoint(heatingUserLimitMin);
        HashMap heatingUserLimitMinPoint = hayStack.read("point and tuner and default and zone and heating and user and limit and min");
        ArrayList<HashMap> heatingUserLimitMinArr = hayStack.readPoint(heatingUserLimitMinPoint.get("id").toString());
        for (HashMap valMap : heatingUserLimitMinArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(heatingUserLimitMinId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(heatingUserLimitMinId, HSUtil.getPriorityVal(heatingUserLimitMinId));
    
        Point heatingUserLimitMax = new Point.Builder()
                                            .setDisplayName(equipdis+"-"+"heatingUserLimitMax")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .addMarker("zone").addMarker("heating").addMarker("user").addMarker("limit").addMarker("max").addMarker("sp")
                                            .setTz(tz)
                                            .build();
        String heatingUserLimitMaxId = hayStack.addPoint(heatingUserLimitMax);
        HashMap heatingUserLimitMaxPoint = hayStack.read("point and tuner and default and zone and heating and user and limit and max");
        ArrayList<HashMap> heatingUserLimitMaxArr = hayStack.readPoint(heatingUserLimitMaxPoint.get("id").toString());
        for (HashMap valMap : heatingUserLimitMaxArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(heatingUserLimitMaxId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(heatingUserLimitMaxId, HSUtil.getPriorityVal(heatingUserLimitMaxId));
        
        Point coolingUserLimitMin = new Point.Builder()
                                            .setDisplayName(equipdis+"-"+"coolingUserLimitMin")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .addMarker("zone").addMarker("cooling").addMarker("user").addMarker("limit").addMarker("min").addMarker("sp")
                                            .setTz(tz)
                                            .build();
        String coolingUserLimitMinId = hayStack.addPoint(coolingUserLimitMin);
        HashMap coolingUserLimitMinPoint = hayStack.read("point and tuner and default and zone and cooling and user and limit and min");
        ArrayList<HashMap> coolingUserLimitMinArr = hayStack.readPoint(coolingUserLimitMinPoint.get("id").toString());
        for (HashMap valMap : coolingUserLimitMinArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(coolingUserLimitMinId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(coolingUserLimitMinId, HSUtil.getPriorityVal(coolingUserLimitMinId));
        
        Point coolingUserLimitMax = new Point.Builder()
                                            .setDisplayName(equipdis+"-"+"coolingUserLimitMax")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .addMarker("zone").addMarker("cooling").addMarker("user").addMarker("limit").addMarker("max").addMarker("sp")
                                            .setTz(tz)
                                            .build();
        String coolingUserLimitMaxId = hayStack.addPoint(coolingUserLimitMax);
        HashMap coolingUserLimitMaxPoint = hayStack.read("point and tuner and default and zone and cooling and user and limit and max");
        ArrayList<HashMap> coolingUserLimitMaxArr = hayStack.readPoint(coolingUserLimitMaxPoint.get("id").toString());
        for (HashMap valMap : coolingUserLimitMaxArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(coolingUserLimitMaxId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(coolingUserLimitMaxId, HSUtil.getPriorityVal(coolingUserLimitMaxId));
    }
    
    public void addVavEquipTuners(String equipdis, String equipref, VavProfileConfiguration config) {
    
        Log.d("CCU","addVavEquipTuners for "+equipdis);
    
        addEquipZoneTuners(equipdis, equipref);
        
        Point zonePrioritySpread = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"zonePrioritySpread")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                                  .setTz(tz)
                                  .build();
        String zonePrioritySpreadId = hayStack.addPoint(zonePrioritySpread);
        HashMap zonePrioritySpreadPoint = hayStack.read("point and tuner and default and vav and zone and priority and spread");
        ArrayList<HashMap> zonePrioritySpreadPointArr = hayStack.readPoint(zonePrioritySpreadPoint.get("id").toString());
        for (HashMap valMap : zonePrioritySpreadPointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(zonePrioritySpreadId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(zonePrioritySpreadId, HSUtil.getPriorityVal(zonePrioritySpreadId));
        
        Point zonePriorityMultiplier = new Point.Builder()
                                           .setDisplayName(equipdis+"-"+"zonePriorityMultiplier")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipref)
                                           .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                           .setTz(tz)
                                           .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        HashMap zonePriorityMultiplierPoint = hayStack.read("point and tuner and default and vav and zone and priority and multiplier");
        ArrayList<HashMap> zonePrioritySpreadMultiplierArr = hayStack.readPoint(zonePriorityMultiplierPoint.get("id").toString());
        for (HashMap valMap : zonePrioritySpreadMultiplierArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(zonePriorityMultiplierId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(zonePriorityMultiplierId, HSUtil.getPriorityVal(zonePriorityMultiplierId));
        
        Point coolingDb = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"coolingDeadband")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setTz(tz)
                                  .setUnit("\u00B0F")
                                  .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        HashMap defCdbPoint = hayStack.read("point and tuner and default and vav and cooling and deadband and base");
        ArrayList<HashMap> cdbDefPointArr = hayStack.readPoint(defCdbPoint.get("id").toString());
        for (HashMap valMap : cdbDefPointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(coolingDbId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(coolingDbId, HSUtil.getPriorityVal(coolingDbId));
    
        Point coolingDbMultiplier = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"coolingDeadbandMultiplier")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                  .setTz(tz)
                                  .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        HashMap coolingDbMultiplierPoint = hayStack.read("point and tuner and default and vav and cooling and deadband and multiplier");
        ArrayList<HashMap> coolingDbMultiplierPointArr = hayStack.readPoint(coolingDbMultiplierPoint.get("id").toString());
        for (HashMap valMap : coolingDbMultiplierPointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(coolingDbMultiplierId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(coolingDbMultiplierId, HSUtil.getPriorityVal(coolingDbMultiplierId));
        
        Point heatingDb = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"heatingDeadband")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setTz(tz)
                                  .setUnit("\u00B0F")
                                  .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        HashMap defHdbPoint = hayStack.read("point and tuner and default and vav and heating and deadband and base");
        ArrayList<HashMap> hdbDefPointArr = hayStack.readPoint(defHdbPoint.get("id").toString());
        for (HashMap valMap : hdbDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(heatingDbId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(heatingDbId, HSUtil.getPriorityVal(heatingDbId));
    
        Point heatingDbMultiplier = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"heatingDeadbandMultiplier")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                  .setTz(tz)
                                  .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        HashMap heatingDbMultiplierPoint = hayStack.read("point and tuner and default and vav and heating and deadband and multiplier");
        ArrayList<HashMap> heatingDbMultiplierPointArr = hayStack.readPoint(heatingDbMultiplierPoint.get("id").toString());
        for (HashMap valMap : heatingDbMultiplierPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(heatingDbMultiplierId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(heatingDbMultiplierId, HSUtil.getPriorityVal(heatingDbMultiplierId));
        
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipdis+"-"+"proportionalKFactor")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .addMarker("pgain").addMarker("sp")
                                 .setTz(tz)
                                 .build();
        String pgainId = hayStack.addPoint(propGain);
        HashMap defPgainPoint = hayStack.read("point and tuner and default and vav and pgain");
        ArrayList<HashMap> pgainDefPointArr = hayStack.readPoint(defPgainPoint.get("id").toString());
        for (HashMap valMap : pgainDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(pgainId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(pgainId, HSUtil.getPriorityVal(pgainId));
        
        Point integralGain = new Point.Builder()
                                     .setDisplayName(equipdis+"-"+"integralKFactor")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .addMarker("igain").addMarker("sp")
                                     .setTz(tz)
                                     .build();
        String igainId = hayStack.addPoint(integralGain);
        HashMap defIgainPoint = hayStack.read("point and tuner and default and vav and igain");
        ArrayList<HashMap> igainDefPointArr = hayStack.readPoint(defIgainPoint.get("id").toString());
        for (HashMap valMap : igainDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(igainId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(igainId, HSUtil.getPriorityVal(igainId));
        
        Point propSpread = new Point.Builder()
                                   .setDisplayName(equipdis+"-"+"temperatureProportionalRange")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipref)
                                   .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                   .addMarker("pspread").addMarker("sp")
                                   .setTz(tz)
                                   .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        HashMap defPSpreadPoint = hayStack.read("point and tuner and default and vav and pspread");
        ArrayList<HashMap> pspreadDefPointArr = hayStack.readPoint(defPSpreadPoint.get("id").toString());
        for (HashMap valMap : pspreadDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(pSpreadId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(pSpreadId, HSUtil.getPriorityVal(pSpreadId));
        
        Point integralTimeout = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"temperatureIntegralTime")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("itimeout").addMarker("sp")
                                        .setTz(tz)
                                        .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        HashMap defITPoint = hayStack.read("point and tuner and default and vav and itimeout");
        ArrayList<HashMap> iTDefPointArr = hayStack.readPoint(defITPoint.get("id").toString());
        for (HashMap valMap : iTDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(iTimeoutId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(iTimeoutId, HSUtil.getPriorityVal(iTimeoutId));
    
        Point valveStartDamper = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"valveActuationStartDamperPosDuringSysHeating")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("valve").addMarker("start").addMarker("damper").addMarker("sp")
                                        .setTz(tz)
                                        .build();
        String valveStartDamperId = hayStack.addPoint(valveStartDamper);
        HashMap valveStartDamperPoint = hayStack.read("point and tuner and default and vav and valve and start and damper");
        ArrayList<HashMap> valveStartDamperArr = hayStack.readPoint(valveStartDamperPoint.get("id").toString());
        for (HashMap valMap : valveStartDamperArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(valveStartDamperId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(valveStartDamperId, HSUtil.getPriorityVal(valveStartDamperId));
    
        Point zoneCO2Target = new Point.Builder()
                                      .setDisplayName(equipdis+"-"+"zoneCO2Target")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                      .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                      .setTz(tz)
                                      .build();
        String zoneCO2TargetId = hayStack.addPoint(zoneCO2Target);
        HashMap zoneCO2TargetPoint = hayStack.read("point and tuner and default and vav and zone and co2 and target");
        ArrayList<HashMap> zoneCO2TargetPointArr = hayStack.readPoint(zoneCO2TargetPoint.get("id").toString());
        for (HashMap valMap : zoneCO2TargetPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(zoneCO2TargetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(zoneCO2TargetId, HSUtil.getPriorityVal(zoneCO2TargetId));
    
        Point zoneCO2Threshold = new Point.Builder()
                                         .setDisplayName(equipdis+"-"+"zoneCO2Threshold")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipref)
                                         .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                         .setTz(tz)
                                         .build();
        String zoneCO2ThresholdId = hayStack.addPoint(zoneCO2Threshold);
        HashMap zoneCO2ThresholdPoint = hayStack.read("point and tuner and default and vav and zone and co2 and threshold");
        ArrayList<HashMap> zoneCO2ThresholdPointArr = hayStack.readPoint(zoneCO2ThresholdPoint.get("id").toString());
        for (HashMap valMap : zoneCO2ThresholdPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(zoneCO2ThresholdId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(zoneCO2ThresholdId, HSUtil.getPriorityVal(zoneCO2ThresholdId));
        
        Point zoneVOCTarget = new Point.Builder()
                                         .setDisplayName(equipdis+"-"+"zoneVOCTarget")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipref)
                                         .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("sp").addMarker("equipHis")
                                         .addMarker("zone").addMarker("voc").addMarker("target")
                                         .setTz(tz)
                                         .build();
        String zoneVOCTargetId = hayStack.addPoint(zoneVOCTarget);
        HashMap zoneVOCTargetPoint = hayStack.read("point and tuner and default and vav and zone and voc and target");
        ArrayList<HashMap> zoneVOCTargetPointArr = hayStack.readPoint(zoneVOCTargetPoint.get("id").toString());
        for (HashMap valMap : zoneVOCTargetPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(zoneVOCTargetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(zoneVOCTargetId, HSUtil.getPriorityVal(zoneVOCTargetId));
    
        Point zoneVOCThreshold = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"zoneVOCThreshold")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                        .setTz(tz)
                                        .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);
        HashMap zoneVOCThresholdPoint = hayStack.read("point and tuner and default and vav and zone and voc and threshold");
        ArrayList<HashMap> zoneVOCThresholdPointArr = hayStack.readPoint(zoneVOCThresholdPoint.get("id").toString());
        for (HashMap valMap : zoneVOCThresholdPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(zoneVOCThresholdId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(zoneVOCThresholdId, HSUtil.getPriorityVal(zoneVOCThresholdId));
        
    }
    
    public void addDefaultPlcTuners() {
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"proportionalKFactor")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("tuner").addMarker("default").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .addMarker("pgain").addMarker("sp")
                                 .setTz(tz)
                                 .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePoint(pgainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_GAIN, 0);
        hayStack.writeHisValById(pgainId, TunerConstants.VAV_PROPORTIONAL_GAIN);
    
        Point integralGain = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"integralKFactor")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef)
                                     .addMarker("tuner").addMarker("default").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .addMarker("igain").addMarker("sp")
                                     .setTz(tz)
                                     .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePoint(igainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_GAIN, 0);
        hayStack.writeHisValById(igainId, TunerConstants.VAV_INTEGRAL_GAIN);
    
        Point integralTimeout = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"pidIntegralTime")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef)
                                        .addMarker("tuner").addMarker("default").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("itimeout").addMarker("sp")
                                        .setTz(tz)
                                        .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePoint(iTimeoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_TIMEOUT, 0);
        hayStack.writeHisValById(iTimeoutId, TunerConstants.VAV_INTEGRAL_TIMEOUT);
    }
    
    public void addPlcEquipTuners(String equipdis, String equipref){
        
        //addEquipZoneTuners(equipdis, equipref);
        
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipdis+"-"+"proportionalKFactor")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .addMarker("tuner").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .addMarker("pgain").addMarker("sp")
                                 .setTz(tz)
                                 .build();
        String pgainId = hayStack.addPoint(propGain);
        HashMap defPgainPoint = hayStack.read("point and tuner and default and pid and pgain");
        ArrayList<HashMap> pgainDefPointArr = hayStack.readPoint(defPgainPoint.get("id").toString());
        for (HashMap valMap : pgainDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(pgainId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(pgainId, HSUtil.getPriorityVal(pgainId));
    
        Point integralGain = new Point.Builder()
                                     .setDisplayName(equipdis+"-"+"integralKFactor")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .addMarker("tuner").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .addMarker("igain").addMarker("sp")
                                     .setTz(tz)
                                     .build();
        String igainId = hayStack.addPoint(integralGain);
        HashMap defIgainPoint = hayStack.read("point and tuner and default and pid and igain");
        ArrayList<HashMap> igainDefPointArr = hayStack.readPoint(defIgainPoint.get("id").toString());
        for (HashMap valMap : igainDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(igainId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(igainId, HSUtil.getPriorityVal(igainId));
        
        Point integralTimeout = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"pidIntegralTime")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("tuner").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("itimeout").addMarker("sp")
                                        .setTz(tz)
                                        .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        HashMap defITPoint = hayStack.read("point and tuner and default and pid and itimeout");
        ArrayList<HashMap> iTDefPointArr = hayStack.readPoint(defITPoint.get("id").toString());
        for (HashMap valMap : iTDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(iTimeoutId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(iTimeoutId, HSUtil.getPriorityVal(iTimeoutId));
    }
    
    public void addDefaultDabTuners(){
        HashMap tuner = CCUHsApi.getInstance().read("point and tuner and default and dab");
        if (tuner != null && tuner.size() > 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM,"Default DAB Tuner points already exist");
            return;
        }
        CcuLog.d(L.TAG_CCU_SYSTEM,"Default DAB Tuner  does not exist. Create Now");
    
        Point zonePrioritySpread = new Point.Builder()
                                           .setDisplayName(equipDis+"-DAB-"+"zonePrioritySpread")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef)
                                           .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                                           .setTz(tz)
                                           .build();
        String zonePrioritySpreadId = hayStack.addPoint(zonePrioritySpread);
        hayStack.writePoint(zonePrioritySpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_PRIORITY_SPREAD, 0);
        hayStack.writeHisValById(zonePrioritySpreadId, TunerConstants.ZONE_PRIORITY_SPREAD);
    
        Point zonePriorityMultiplier = new Point.Builder()
                                               .setDisplayName(equipDis+"-DAB-"+"zonePriorityMultiplier")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef)
                                               .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                               .setTz(tz)
                                               .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        hayStack.writePoint(zonePriorityMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_PRIORITY_MULTIPLIER, 0);
        hayStack.writeHisValById(zonePriorityMultiplierId, TunerConstants.ZONE_PRIORITY_MULTIPLIER);
    
        Point coolingDb = new Point.Builder()
                                  .setDisplayName(equipDis+"-DAB-"+"coolingDeadband")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        hayStack.writePoint(coolingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_COOLING_DB, 0);
        hayStack.writeHisValById(coolingDbId, TunerConstants.VAV_COOLING_DB);
    
        Point coolingDbMultiplier = new Point.Builder()
                                            .setDisplayName(equipDis+"-DAB-"+"coolingDeadbandMultiplier")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef)
                                            .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                            .setTz(tz)
                                            .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        hayStack.writePoint(coolingDbMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_COOLING_DB_MULTPLIER, 0);
        hayStack.writeHisValById(coolingDbMultiplierId, TunerConstants.VAV_COOLING_DB_MULTPLIER);
    
        Point heatingDb = new Point.Builder()
                                  .setDisplayName(equipDis+"-DAB-"+"heatingDeadband")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        hayStack.writePoint(heatingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_HEATING_DB, 0);
        hayStack.writeHisValById(heatingDbId, TunerConstants.VAV_HEATING_DB);
    
        Point heatingDbMultiplier = new Point.Builder()
                                            .setDisplayName(equipDis+"-DAB-"+"heatingDeadbandMultiplier")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef)
                                            .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                            .setTz(tz)
                                            .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        hayStack.writePoint(heatingDbMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_HEATING_DB_MULTIPLIER, 0);
        hayStack.writeHisValById(heatingDbMultiplierId, TunerConstants.VAV_HEATING_DB_MULTIPLIER);
    
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-DAB-"+"proportionalKFactor ")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .addMarker("pgain").addMarker("sp")
                                 .setTz(tz)
                                 .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePoint(pgainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_GAIN, 0);
        hayStack.writeHisValById(pgainId, TunerConstants.VAV_PROPORTIONAL_GAIN);
    
        Point integralGain = new Point.Builder()
                                     .setDisplayName(equipDis+"-DAB-"+"integralKFactor ")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef)
                                     .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .addMarker("igain").addMarker("sp")
                                     .setTz(tz)
                                     .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePoint(igainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_GAIN, 0);
        hayStack.writeHisValById(igainId, TunerConstants.VAV_INTEGRAL_GAIN);
    
        Point propSpread = new Point.Builder()
                                   .setDisplayName(equipDis+"-DAB-"+"temperatureProportionalRange ")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                   .addMarker("pspread").addMarker("sp")
                                   .setTz(tz)
                                   .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        hayStack.writePoint(pSpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_SPREAD, 0);
        hayStack.writeHisValById(pSpreadId, TunerConstants.VAV_PROPORTIONAL_SPREAD);
    
        Point integralTimeout = new Point.Builder()
                                        .setDisplayName(equipDis+"-DAB-"+"temperatureIntegralTime ")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef)
                                        .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("itimeout").addMarker("sp")
                                        .setTz(tz)
                                        .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePoint(iTimeoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_TIMEOUT, 0);
        hayStack.writeHisValById(iTimeoutId, TunerConstants.VAV_INTEGRAL_TIMEOUT);
    
        Point zoneCO2Target  = new Point.Builder()
                                       .setDisplayName(equipDis+"-DAB-"+"zoneCO2Target")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipRef)
                                       .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                       .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                       .setTz(tz)
                                       .build();
        String zoneCO2TargetId = hayStack.addPoint(zoneCO2Target);
        hayStack.writePoint(zoneCO2TargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_CO2_TARGET, 0);
        hayStack.writeHisValById(zoneCO2TargetId, TunerConstants.ZONE_CO2_TARGET);
    
        Point zoneCO2Threshold  = new Point.Builder()
                                          .setDisplayName(equipDis+"-DAB-"+"zoneCO2Threshold")
                                          .setSiteRef(siteRef)
                                          .setEquipRef(equipRef)
                                          .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                          .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                          .setTz(tz)
                                          .build();
        String zoneCO2ThresholdId = hayStack.addPoint(zoneCO2Threshold);
        hayStack.writePoint(zoneCO2ThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_CO2_THRESHOLD, 0);
        hayStack.writeHisValById(zoneCO2ThresholdId, TunerConstants.ZONE_CO2_THRESHOLD);
    
        Point zoneVOCTarget  = new Point.Builder()
                                       .setDisplayName(equipDis+"-DAB-"+"zoneVOCTarget")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipRef)
                                       .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                       .addMarker("zone").addMarker("voc").addMarker("target").addMarker("sp")
                                       .setTz(tz)
                                       .build();
        String zoneVOCTargetId = hayStack.addPoint(zoneVOCTarget);
        hayStack.writePoint(zoneVOCTargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_VOC_TARGET, 0);
        hayStack.writeHisValById(zoneVOCTargetId, TunerConstants.ZONE_VOC_TARGET);
    
        Point zoneVOCThreshold  = new Point.Builder()
                                          .setDisplayName(equipDis+"-DAB-"+"zoneVOCThreshold")
                                          .setSiteRef(siteRef)
                                          .setEquipRef(equipRef)
                                          .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                          .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                          .setTz(tz)
                                          .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);
        hayStack.writePoint(zoneVOCThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_VOC_THRESHOLD, 0);
        hayStack.writeHisValById(zoneVOCThresholdId, TunerConstants.ZONE_VOC_THRESHOLD);
    
        addDefaultDabSystemTuners();
    }
    
    public void addDefaultDabSystemTuners()
    {
        Point targetCumulativeDamper = new Point.Builder()
                                               .setDisplayName(equipDis+"-DAB-"+"targetCumulativeDamper")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef)
                                               .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .addMarker("target").addMarker("cumulative").addMarker("damper").addMarker("sp")
                                               .setTz(tz)
                                               .build();
        String targetCumulativeDamperId = hayStack.addPoint(targetCumulativeDamper);
        hayStack.writePoint(targetCumulativeDamperId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.TARGET_CUMULATIVE_DAMPER, 0);
        hayStack.writeHisValById(targetCumulativeDamperId, TunerConstants.TARGET_CUMULATIVE_DAMPER);
        
        Point analogFanSpeedMultiplier = new Point.Builder()
                                                 .setDisplayName(equipDis+"-DAB-"+"analogFanSpeedMultiplier")
                                                 .setSiteRef(siteRef)
                                                 .setEquipRef(equipRef)
                                                 .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                 .addMarker("analog").addMarker("fan").addMarker("speed").addMarker("multiplier").addMarker("sp")
                                                 .setTz(tz)
                                                 .build();
        String analogFanSpeedMultiplierId = hayStack.addPoint(analogFanSpeedMultiplier);
        hayStack.writePoint(analogFanSpeedMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ANALOG_FANSPEED_MULTIPLIER, 0);
        hayStack.writeHisValById(analogFanSpeedMultiplierId, TunerConstants.ANALOG_FANSPEED_MULTIPLIER);
        
        Point humidityHysteresis = new Point.Builder()
                                           .setDisplayName(equipDis+"-DAB-"+"humidityHysteresis")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef)
                                           .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("humidity").addMarker("hysteresis").addMarker("sp")
                                           .setTz(tz)
                                           .build();
        String humidityHysteresisId = hayStack.addPoint(humidityHysteresis);
        hayStack.writePoint(humidityHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.HUMIDITY_HYSTERESIS_PERCENT, 0);
        hayStack.writeHisValById(humidityHysteresisId, TunerConstants.HUMIDITY_HYSTERESIS_PERCENT);
        
        Point relayDeactivationHysteresis = new Point.Builder()
                                                    .setDisplayName(equipDis+"-DAB-"+"relayDeactivationHysteresis")
                                                    .setSiteRef(siteRef)
                                                    .setEquipRef(equipRef)
                                                    .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                    .addMarker("relay").addMarker("deactivation").addMarker("hysteresis").addMarker("sp")
                                                    .setTz(tz)
                                                    .build();
        String relayDeactivationHysteresisId = hayStack.addPoint(relayDeactivationHysteresis);
        hayStack.writePoint(relayDeactivationHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.RELAY_DEACTIVATION_HYSTERESIS, 0);
        hayStack.writeHisValById(relayDeactivationHysteresisId, TunerConstants.RELAY_DEACTIVATION_HYSTERESIS);
        
    }
    
    public void addEquipDabTuners(String equipdis, String equipref) {
        Log.d("CCU","addEquipDabTuners for "+equipdis);
    
        addEquipZoneTuners(equipdis, equipref);
        
        Point zonePrioritySpread = new Point.Builder()
                                           .setDisplayName(equipdis+"-"+"zonePrioritySpread")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipref)
                                           .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                                           .setTz(tz)
                                           .build();
        String zonePrioritySpreadId = hayStack.addPoint(zonePrioritySpread);
        HashMap zonePrioritySpreadPoint = hayStack.read("point and tuner and default and dab and zone and priority and spread");
        ArrayList<HashMap> zonePrioritySpreadPointArr = hayStack.readPoint(zonePrioritySpreadPoint.get("id").toString());
        for (HashMap valMap : zonePrioritySpreadPointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(zonePrioritySpreadId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(zonePrioritySpreadId, HSUtil.getPriorityVal(zonePrioritySpreadId));
    
        Point zonePriorityMultiplier = new Point.Builder()
                                               .setDisplayName(equipdis+"-"+"zonePriorityMultiplier")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                               .setTz(tz)
                                               .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        HashMap zonePriorityMultiplierPoint = hayStack.read("point and tuner and default and dab and zone and priority and multiplier");
        ArrayList<HashMap> zonePrioritySpreadMultiplierArr = hayStack.readPoint(zonePriorityMultiplierPoint.get("id").toString());
        for (HashMap valMap : zonePrioritySpreadMultiplierArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(zonePriorityMultiplierId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(zonePriorityMultiplierId, HSUtil.getPriorityVal(zonePriorityMultiplierId));
    
        Point coolingDb = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"coolingDeadband")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setTz(tz)
                                  .setUnit("\u00B0F")
                                  .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        HashMap defCdbPoint = hayStack.read("point and tuner and default and dab and cooling and deadband and base");
        ArrayList<HashMap> cdbDefPointArr = hayStack.readPoint(defCdbPoint.get("id").toString());
        for (HashMap valMap : cdbDefPointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(coolingDbId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(coolingDbId, HSUtil.getPriorityVal(coolingDbId));
    
        Point coolingDbMultiplier = new Point.Builder()
                                            .setDisplayName(equipdis+"-"+"coolingDeadbandMultiplier")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                            .setTz(tz)
                                            .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        HashMap coolingDbMultiplierPoint = hayStack.read("point and tuner and default and dab and cooling and deadband and multiplier");
        ArrayList<HashMap> coolingDbMultiplierPointArr = hayStack.readPoint(coolingDbMultiplierPoint.get("id").toString());
        for (HashMap valMap : coolingDbMultiplierPointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(coolingDbMultiplierId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(coolingDbMultiplierId, HSUtil.getPriorityVal(coolingDbMultiplierId));
    
        Point heatingDb = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"heatingDeadband")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setTz(tz)
                                  .setUnit("\u00B0F")
                                  .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        HashMap defHdbPoint = hayStack.read("point and tuner and default and dab and heating and deadband and base");
        ArrayList<HashMap> hdbDefPointArr = hayStack.readPoint(defHdbPoint.get("id").toString());
        for (HashMap valMap : hdbDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(heatingDbId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(heatingDbId, HSUtil.getPriorityVal(heatingDbId));
    
        Point heatingDbMultiplier = new Point.Builder()
                                            .setDisplayName(equipdis+"-"+"heatingDeadbandMultiplier")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                            .setTz(tz)
                                            .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        HashMap heatingDbMultiplierPoint = hayStack.read("point and tuner and default and dab and heating and deadband and multiplier");
        ArrayList<HashMap> heatingDbMultiplierPointArr = hayStack.readPoint(heatingDbMultiplierPoint.get("id").toString());
        for (HashMap valMap : heatingDbMultiplierPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(heatingDbMultiplierId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(heatingDbMultiplierId, HSUtil.getPriorityVal(heatingDbMultiplierId));
    
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipdis+"-"+"proportionalKFactor")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .addMarker("pgain").addMarker("sp")
                                 .setTz(tz)
                                 .build();
        String pgainId = hayStack.addPoint(propGain);
        HashMap defPgainPoint = hayStack.read("point and tuner and default and dab and pgain");
        ArrayList<HashMap> pgainDefPointArr = hayStack.readPoint(defPgainPoint.get("id").toString());
        for (HashMap valMap : pgainDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(pgainId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(pgainId, HSUtil.getPriorityVal(pgainId));
    
        Point integralGain = new Point.Builder()
                                     .setDisplayName(equipdis+"-"+"integralKFactor")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .addMarker("igain").addMarker("sp")
                                     .setTz(tz)
                                     .build();
        String igainId = hayStack.addPoint(integralGain);
        HashMap defIgainPoint = hayStack.read("point and tuner and default and dab and igain");
        ArrayList<HashMap> igainDefPointArr = hayStack.readPoint(defIgainPoint.get("id").toString());
        for (HashMap valMap : igainDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(igainId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(igainId, HSUtil.getPriorityVal(igainId));
    
        Point propSpread = new Point.Builder()
                                   .setDisplayName(equipdis+"-"+"temperatureProportionalRange")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipref)
                                   .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                   .addMarker("pspread").addMarker("sp")
                                   .setTz(tz)
                                   .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        HashMap defPSpreadPoint = hayStack.read("point and tuner and default and dab and pspread");
        ArrayList<HashMap> pspreadDefPointArr = hayStack.readPoint(defPSpreadPoint.get("id").toString());
        for (HashMap valMap : pspreadDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(pSpreadId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(pSpreadId, HSUtil.getPriorityVal(pSpreadId));
    
        Point integralTimeout = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"temperatureIntegralTime")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("itimeout").addMarker("sp")
                                        .setTz(tz)
                                        .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        HashMap defITPoint = hayStack.read("point and tuner and default and dab and itimeout");
        ArrayList<HashMap> iTDefPointArr = hayStack.readPoint(defITPoint.get("id").toString());
        for (HashMap valMap : iTDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(iTimeoutId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(iTimeoutId, HSUtil.getPriorityVal(iTimeoutId));
    
        Point zoneCO2Target = new Point.Builder()
                                      .setDisplayName(equipdis+"-"+"zoneCO2Target")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                      .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                      .setTz(tz)
                                      .build();
        String zoneCO2TargetId = hayStack.addPoint(zoneCO2Target);
        HashMap zoneCO2TargetPoint = hayStack.read("point and tuner and default and dab and zone and co2 and target");
        ArrayList<HashMap> zoneCO2TargetPointArr = hayStack.readPoint(zoneCO2TargetPoint.get("id").toString());
        for (HashMap valMap : zoneCO2TargetPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(zoneCO2TargetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(zoneCO2TargetId, HSUtil.getPriorityVal(zoneCO2TargetId));
    
        Point zoneCO2Threshold = new Point.Builder()
                                         .setDisplayName(equipdis+"-"+"zoneCO2Threshold")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipref)
                                         .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                         .setTz(tz)
                                         .build();
        String zoneCO2ThresholdId = hayStack.addPoint(zoneCO2Threshold);
        HashMap zoneCO2ThresholdPoint = hayStack.read("point and tuner and default and dab and zone and co2 and threshold");
        ArrayList<HashMap> zoneCO2ThresholdPointArr = hayStack.readPoint(zoneCO2ThresholdPoint.get("id").toString());
        for (HashMap valMap : zoneCO2ThresholdPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(zoneCO2ThresholdId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(zoneCO2ThresholdId, HSUtil.getPriorityVal(zoneCO2ThresholdId));
    
        Point zoneVOCTarget = new Point.Builder()
                                      .setDisplayName(equipdis+"-"+"zoneVOCTarget")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("sp").addMarker("equipHis")
                                      .addMarker("zone").addMarker("voc").addMarker("target")
                                      .setTz(tz)
                                      .build();
        String zoneVOCTargetId = hayStack.addPoint(zoneVOCTarget);
        HashMap zoneVOCTargetPoint = hayStack.read("point and tuner and default and dab and zone and voc and target");
        ArrayList<HashMap> zoneVOCTargetPointArr = hayStack.readPoint(zoneVOCTargetPoint.get("id").toString());
        for (HashMap valMap : zoneVOCTargetPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(zoneVOCTargetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(zoneVOCTargetId, HSUtil.getPriorityVal(zoneVOCTargetId));
    
        Point zoneVOCThreshold = new Point.Builder()
                                         .setDisplayName(equipdis+"-"+"zoneVOCThreshold")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipref)
                                         .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                         .setTz(tz)
                                         .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);
        HashMap zoneVOCThresholdPoint = hayStack.read("point and tuner and default and dab and zone and voc and threshold");
        ArrayList<HashMap> zoneVOCThresholdPointArr = hayStack.readPoint(zoneVOCThresholdPoint.get("id").toString());
        for (HashMap valMap : zoneVOCThresholdPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(zoneVOCThresholdId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(zoneVOCThresholdId, HSUtil.getPriorityVal(zoneVOCThresholdId));
    
    }

    public void addEquipZoneStandaloneTuners(String equipdis, String equipref) {
        Point saHeatingDeadBand = new Point.Builder()
                .setDisplayName(equipdis+"-"+"standaloneHeatingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .addMarker("tuner").addMarker("base").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("standalone").addMarker("heating").addMarker("deadband").addMarker("sp")
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String saHeatingDeadBandId = hayStack.addPoint(saHeatingDeadBand);
        HashMap saHeatingDeadBandPoint = hayStack.read("point and tuner and default and base and standalone and heating and deadband");
        ArrayList<HashMap> saHeatingDeadBandArr = hayStack.readPoint(saHeatingDeadBandPoint.get("id").toString());
        for (HashMap valMap : saHeatingDeadBandArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(saHeatingDeadBandId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(saHeatingDeadBandId, HSUtil.getPriorityVal(saHeatingDeadBandId));
        
        Point saCoolingDeadBand = new Point.Builder()
                .setDisplayName(equipdis+"-"+"standaloneCoolingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("cooling").addMarker("deadband").addMarker("sp")
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String saCoolingDeadBandId = hayStack.addPoint(saCoolingDeadBand);
        HashMap saCoolingDeadBandPoint = hayStack.read("point and tuner and default and base and standalone and cooling and deadband");
        ArrayList<HashMap> saCoolingDeadBandArr = hayStack.readPoint(saCoolingDeadBandPoint.get("id").toString());
        for (HashMap valMap : saCoolingDeadBandArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(saCoolingDeadBandId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(saCoolingDeadBandId, HSUtil.getPriorityVal(saCoolingDeadBandId));
        
        Point saStage1Hysteresis = new Point.Builder()
                .setDisplayName(equipdis+"-"+"standaloneStage1Hysteresis")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage1").addMarker("hysteresis").addMarker("sp")
                .setTz(tz)
                .build();
        String saStage1HysteresisId = hayStack.addPoint(saStage1Hysteresis);
        HashMap saStage1HysteresisPoint = hayStack.read("point and tuner and default and base and standalone and stage1 and hysteresis");
        ArrayList<HashMap> saStage1HysteresisArr = hayStack.readPoint(saStage1HysteresisPoint.get("id").toString());
        for (HashMap valMap : saStage1HysteresisArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(saStage1HysteresisId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(saStage1HysteresisId, HSUtil.getPriorityVal(saStage1HysteresisId));
        
    }
    public void addEquipStandaloneTuners(String equipdis, String equipref){
        addEquipZoneTuners(equipdis,equipref);
        addEquipZoneStandaloneTuners(equipdis,equipref);
    }
    public void addDefaultStandaloneTuners()
    {
        Point saHeatingDeadBand = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneHeatingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("standalone").addMarker("heating").addMarker("deadband").addMarker("sp")
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String saHeatingDeadBandId = hayStack.addPoint(saHeatingDeadBand);
        hayStack.writePoint(saHeatingDeadBandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_DEADBAND_DEFAULT, 0);
        hayStack.writeHisValById(saHeatingDeadBandId, TunerConstants.STANDALONE_HEATING_DEADBAND_DEFAULT);

        Point saCoolingDeadBand = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneCoolingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("cooling").addMarker("deadband").addMarker("sp")
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String saCoolingDeadBandId = hayStack.addPoint(saCoolingDeadBand);
        hayStack.writePoint(saCoolingDeadBandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_DEADBAND_DEFAULT, 0);
        hayStack.writeHisValById(saCoolingDeadBandId, TunerConstants.STANDALONE_COOLING_DEADBAND_DEFAULT);
        Point saStage1Hysteresis = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneStage1Hysteresis")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage1").addMarker("hysteresis").addMarker("sp")
                .setTz(tz)
                .build();
        String saStage1HysteresisId = hayStack.addPoint(saStage1Hysteresis);
        hayStack.writePoint(saStage1HysteresisId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_STAGE1_HYSTERESIS_DEFAULT, 0);
        hayStack.writeHisValById(saStage1HysteresisId, TunerConstants.STANDALONE_STAGE1_HYSTERESIS_DEFAULT);

        Point saAirflowSampleWaitTime = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneAirflowSampleWaitTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .addMarker("tuner").addMarker("default").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("airflow").addMarker("sample").addMarker("wait").addMarker("time").addMarker("sp")
                .setTz(tz)
                .build();
        String saAirflowSampleWaitTimeId = hayStack.addPoint(saAirflowSampleWaitTime);
        hayStack.writePoint(saAirflowSampleWaitTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_AIRFLOW_SAMPLE_WAIT_TIME, 0);
        hayStack.writeHisValById(saAirflowSampleWaitTimeId, TunerConstants.STANDALONE_AIRFLOW_SAMPLE_WAIT_TIME);

        Point saStage1CoolingLowerOffset = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneStage1CoolingLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .addMarker("tuner").addMarker("default").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("stage1").addMarker("cooling").addMarker("sp").addMarker("lower").addMarker("offset")
                .setTz(tz)
                .build();
        String saStage1CoolingLowerOffsetId = hayStack.addPoint(saStage1CoolingLowerOffset);
        hayStack.writePoint(saStage1CoolingLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_STAGE1_LOWER_OFFSET, 0);
        hayStack.writeHisValById(saStage1CoolingLowerOffsetId, TunerConstants.STANDALONE_COOLING_STAGE1_LOWER_OFFSET);

        Point saStage1CoolingUpperOffset = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneStage1CoolingUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .addMarker("tuner").addMarker("default").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("stage1").addMarker("cooling").addMarker("sp").addMarker("upper").addMarker("offset")
                .setTz(tz)
                .build();
        String saStage1CoolingUpperOffsetId = hayStack.addPoint(saStage1CoolingUpperOffset);
        hayStack.writePoint(saStage1CoolingUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_STAGE1_UPPER_OFFSET, 0);
        hayStack.writeHisValById(saStage1CoolingUpperOffsetId, TunerConstants.STANDALONE_COOLING_STAGE1_UPPER_OFFSET);
        //TODO Still need to add heating and stage 2 tuners //kumar

        CCUHsApi.getInstance().syncEntityTree();
    }

}
