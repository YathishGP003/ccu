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
        
        //addSettingTuners();
        addDefaultBuildingTuners();
        addDefaultVavTuners();
        addDefaultPlcTuners();
        addDefaultStandaloneTuners();
        addDefaultDabTuners();
        OAOTuners.addDefaultTuners(equipDis, siteRef, equipRef, tz);
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    public void addDefaultBuildingTuners() {
        Point heatingPreconditioingRate = new Point.Builder()
                                           .setDisplayName(equipDis+"-"+"heatingPreconditioningRate")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("system").addMarker("heating").addMarker("precon").addMarker("rate").addMarker("sp")
                                           .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                           .setTz(tz)
                                           .build();
        String heatingPreconditioingRateId = hayStack.addPoint(heatingPreconditioingRate);
        hayStack.writePoint(heatingPreconditioingRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_PRECONDITION_RATE, 0);
        hayStack.writeHisValById(heatingPreconditioingRateId, TunerConstants.SYSTEM_PRECONDITION_RATE);
    
        Point coolingPreconditioingRate = new Point.Builder()
                                                  .setDisplayName(equipDis+"-"+"coolingPreconditioningRate")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                                  .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                  .addMarker("system").addMarker("cooling").addMarker("precon").addMarker("rate").addMarker("sp")
                                                  .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                  .setTz(tz)
                                                  .build();
        String coolingPreconditioingRateId = hayStack.addPoint(coolingPreconditioingRate);
        hayStack.writePoint(coolingPreconditioingRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_PRECONDITION_RATE, 0);
        hayStack.writeHisValById(coolingPreconditioingRateId, TunerConstants.SYSTEM_PRECONDITION_RATE);
    
        Point userLimitSpread = new Point.Builder()
                                                  .setDisplayName(equipDis+"-"+"userLimitSpread")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                                  .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                  .addMarker("system").addMarker("user").addMarker("limit").addMarker("spread").addMarker("sp")
                                                  .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                                                  .setUnit("\u00B0F")
                                                  .setTz(tz)
                                                  .build();
        String userLimitSpreadId = hayStack.addPoint(userLimitSpread);
        hayStack.writePoint(userLimitSpreadId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.USER_LIMIT_SPREAD, 0);
        hayStack.writeHisValById(userLimitSpreadId, TunerConstants.USER_LIMIT_SPREAD);
    
        Point buildingLimitMin = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"buildingLimitMin")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("system").addMarker("building").addMarker("limit").addMarker("min").addMarker("sp")
                                        .setMinVal("50").setMaxVal("90").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                        .setUnit("\u00B0F")
                                        .setTz(tz)
                                        .build();
        String buildingLimitMinId = hayStack.addPoint(buildingLimitMin);
        hayStack.writePoint(buildingLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.BUILDING_LIMIT_MIN, 0);
        hayStack.writeHisValById(buildingLimitMinId, TunerConstants.BUILDING_LIMIT_MIN);
    
        Point buildingLimitMax = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"buildingLimitMax")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .addMarker("system").addMarker("building").addMarker("limit").addMarker("max").addMarker("sp")
                                         .setMinVal("50").setMaxVal("90").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                         .setUnit("\u00B0F")
                                         .setTz(tz)
                                         .build();
        String buildingLimitMaxId = hayStack.addPoint(buildingLimitMax);
        hayStack.writePoint(buildingLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.BUILDING_LIMIT_MAX, 0);
        hayStack.writeHisValById(buildingLimitMaxId, TunerConstants.BUILDING_LIMIT_MAX);
    
        Point buildingToZoneDifferential = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"buildingToZoneDifferential")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .addMarker("system").addMarker("building").addMarker("zone").addMarker("differential").addMarker("sp")
                                         .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                         .setTz(tz)
                                         .build();
        String buildingToZoneDifferentialId = hayStack.addPoint(buildingToZoneDifferential);
        hayStack.writePoint(buildingToZoneDifferentialId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.BUILDING_TO_ZONE_DIFFERENTIAL, 0);
        hayStack.writeHisValById(buildingToZoneDifferentialId, TunerConstants.BUILDING_TO_ZONE_DIFFERENTIAL);
    
        Point zoneTemperatureDeadLeeway = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"zoneTemperatureDeadLeeway")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipRef).setHisInterpolate("cov")
                                                   .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                   .addMarker("temp").addMarker("dead").addMarker("leeway").addMarker("sp")
                                                   .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                   .setUnit("\u00B0F")
                                                   .setTz(tz)
                                                   .build();
        String zoneTemperatureDeadLeewayId = hayStack.addPoint(zoneTemperatureDeadLeeway);
        hayStack.writePoint(zoneTemperatureDeadLeewayId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_TEMP_DEAD_LEEWAY, 0);
        hayStack.writeHisValById(zoneTemperatureDeadLeewayId, TunerConstants.ZONE_TEMP_DEAD_LEEWAY);


        Point unoccupiedZoneSetback  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"unoccupiedZoneSetback")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("unoccupied").addMarker("setback").addMarker("sp")
                .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String unoccupiedZoneSetbackId = hayStack.addPoint(unoccupiedZoneSetback);
        hayStack.writePoint(unoccupiedZoneSetbackId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_UNOCCUPIED_SETBACK, 0);
        hayStack.writeHisValById(unoccupiedZoneSetbackId, TunerConstants.ZONE_UNOCCUPIED_SETBACK);

        Point heatingUserLimitMin  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"heatingUserLimitMin")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("heating").addMarker("user").addMarker("limit").addMarker("min").addMarker("sp")
                .setMinVal("60").setMaxVal("70").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String heatingUserLimitMinId = hayStack.addPoint(heatingUserLimitMin);
        hayStack.writePoint(heatingUserLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_HEATING_USERLIMIT_MIN, 0);
        hayStack.writeHisValById(heatingUserLimitMinId, TunerConstants.ZONE_HEATING_USERLIMIT_MIN);

        Point heatingUserLimitMax  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"heatingUserLimitMax")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("heating").addMarker("user").addMarker("limit").addMarker("max").addMarker("sp")
                .setMinVal("65").setMaxVal("75").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String heatingUserLimitMaxId = hayStack.addPoint(heatingUserLimitMax);
        hayStack.writePoint(heatingUserLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_HEATING_USERLIMIT_MAX, 0);
        hayStack.writeHisValById(heatingUserLimitMaxId, TunerConstants.ZONE_HEATING_USERLIMIT_MAX);

        Point coolingUserLimitMin  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"coolingUserLimitMin")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis").setMinVal("70").setMaxVal("77").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .addMarker("zone").addMarker("cooling").addMarker("user").addMarker("limit").addMarker("min").addMarker("sp")
                .setMinVal("70").setMaxVal("77").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String coolingUserLimitMinId = hayStack.addPoint(coolingUserLimitMin);
        hayStack.writePoint(coolingUserLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_COOLING_USERLIMIT_MIN, 0);
        hayStack.writeHisValById(coolingUserLimitMinId, TunerConstants.ZONE_COOLING_USERLIMIT_MIN);

        Point coolingUserLimitMax  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"coolingUserLimitMax")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("cooling").addMarker("user").addMarker("limit").addMarker("max").addMarker("sp")
                .setMinVal("72").setMaxVal("80").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String coolingUserLimitMaxId = hayStack.addPoint(coolingUserLimitMax);
        hayStack.writePoint(coolingUserLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_COOLING_USERLIMIT_MAX, 0);
        hayStack.writeHisValById(coolingUserLimitMaxId, TunerConstants.ZONE_COOLING_USERLIMIT_MAX);
        Point humidityCompensationOffset = new Point.Builder()
                                                  .setDisplayName(equipDis+"-"+"humidityCompensationOffset")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                                  .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                  .addMarker("system").addMarker("humidity").addMarker("compensation").addMarker("offset").addMarker("sp")
                                                  .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                  .setTz(tz)
                                                  .build();
        String humidityCompensationOffsetId = hayStack.addPoint(humidityCompensationOffset);
        hayStack.writePoint(humidityCompensationOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 0.0, 0);
        hayStack.writeHisValById(humidityCompensationOffsetId, 0.0);

        Point percentOfDeadZonesAllowed = new Point.Builder()
                                                .setDisplayName(equipDis+"-"+"cmTempPercentDeadZonesAllowed")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipRef).setHisInterpolate("cov")
                                                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                .addMarker("dead").addMarker("percent").addMarker("influence").addMarker("sp")
                                                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                .setTz(tz)
                                                .build();
        String percentOfDeadZonesAllowedId = hayStack.addPoint(percentOfDeadZonesAllowed);
        hayStack.writePoint(percentOfDeadZonesAllowedId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.CM_TEMP_INFLU_PERCENTILE_ZONE_DEAD, 0);
        hayStack.writeHisValById(percentOfDeadZonesAllowedId, TunerConstants.CM_TEMP_INFLU_PERCENTILE_ZONE_DEAD);


        Point zoneDeadTime  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"zoneDeadTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("dead").addMarker("time").addMarker("sp")
                .setMinVal("1").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String zoneDeadTimeId = hayStack.addPoint(zoneDeadTime);
        hayStack.writePoint(zoneDeadTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 15.0, 0);
        hayStack.writeHisValById(zoneDeadTimeId, 15.0);

        Point autoAwayTime  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"autoAwayTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("auto").addMarker("away").addMarker("time").addMarker("sp")
                .setMinVal("40").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String autoAwayTimeId = hayStack.addPoint(autoAwayTime);
        hayStack.writePoint(autoAwayTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 60.0, 0);
        hayStack.writeHisValById(autoAwayTimeId, 60.0);

        Point forcedOccupiedTime  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"forcedOccupiedTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("forced").addMarker("occupied").addMarker("time").addMarker("sp")
                .setMinVal("30").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String forcedOccupiedTimeId = hayStack.addPoint(forcedOccupiedTime);
        hayStack.writePoint(forcedOccupiedTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 120.0, 0);
        hayStack.writeHisValById(forcedOccupiedTimeId, 120.0);

        Point cmResetCommand  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"cmResetCommandTimer")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("reset").addMarker("command").addMarker("time").addMarker("sp")
                .setMinVal("0").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String cmResetCommandId = hayStack.addPoint(cmResetCommand);
        hayStack.writePoint(cmResetCommandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 90.0, 0);
        hayStack.writeHisValById(cmResetCommandId, 90.0);

        Point adrCoolingDeadband  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"adrCoolingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("adr").addMarker("cooling").addMarker("deadband").addMarker("sp")
                .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String adrCoolingDeadbandId = hayStack.addPoint(adrCoolingDeadband);
        hayStack.writePoint(adrCoolingDeadbandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 3.0, 0);
        hayStack.writeHisValById(adrCoolingDeadbandId, 3.0);

        Point adrHeatingDeadband  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"adrHeatingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("adr").addMarker("heating").addMarker("deadband").addMarker("sp")
                .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String adrHeatingDeadbandId = hayStack.addPoint(adrHeatingDeadband);
        hayStack.writePoint(adrHeatingDeadbandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 3.0, 0);
        hayStack.writeHisValById(adrHeatingDeadbandId, 3.0);

        Point snCoolingAirflowTemperature  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"snCoolingAirflowTemp")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("sn").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("sp")
                .setMinVal("35").setMaxVal("70").setIncrementVal("1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String snCoolingAirflowTemperatureId = hayStack.addPoint(snCoolingAirflowTemperature);
        hayStack.writePoint(snCoolingAirflowTemperatureId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 60.0, 0);
        hayStack.writeHisValById(snCoolingAirflowTemperatureId, 60.0);

        Point snHeatingAirflowTemperature  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"snHeatingAirflowTemp")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("sn").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("sp")
                .setMinVal("80").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String snHeatingAirflowTemperatureId = hayStack.addPoint(snHeatingAirflowTemperature);
        hayStack.writePoint(snHeatingAirflowTemperatureId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 105.0, 0);
        hayStack.writeHisValById(snHeatingAirflowTemperatureId, 105.0);

        Point buildingLimitAlertTimer  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"buildingLimitAlertTimer")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("building").addMarker("limit").addMarker("alert").addMarker("timer").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String buildingLimitAlertTimerId = hayStack.addPoint(buildingLimitAlertTimer);
        hayStack.writePoint(buildingLimitAlertTimerId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 45.0, 0);
        hayStack.writeHisValById(buildingLimitAlertTimerId, 45.0);

        Point constantTempAlertTime  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"constantTempAlertTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("constant").addMarker("temp").addMarker("alert").addMarker("time").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("5").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String constantTempAlertTimeId = hayStack.addPoint(constantTempAlertTime);
        hayStack.writePoint(constantTempAlertTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 40.0, 0);
        hayStack.writeHisValById(constantTempAlertTimeId, 40.0);

        Point abnormalCurTempRiseTrigger  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"abnormalCurTempRiseTrigger")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("abnormal").addMarker("cur").addMarker("temp").addMarker("rise").addMarker("trigger").addMarker("sp")
                .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String abnormalCurTempRiseTriggerId = hayStack.addPoint(abnormalCurTempRiseTrigger);
        hayStack.writePoint(abnormalCurTempRiseTriggerId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 4.0, 0);
        hayStack.writeHisValById(abnormalCurTempRiseTriggerId, 4.0);

        Point airflowSampleWaitTime  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"airflowSampleWaitTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("airflow").addMarker("sample").addMarker("wait").addMarker("time").addMarker("sp")
                .setMinVal("1").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String airflowSampleWaitTimeId = hayStack.addPoint(airflowSampleWaitTime);
        hayStack.writePoint(airflowSampleWaitTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 5.0, 0);
        hayStack.writeHisValById(airflowSampleWaitTimeId, 5.0);

        Point stage1CoolingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage1CoolingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage1").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-120").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage1CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage1CoolingAirflowTempLowerOffset);
        hayStack.writePoint(stage1CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", -20.0, 0);
        hayStack.writeHisValById(stage1CoolingAirflowTempLowerOffsetId, -20.0);

        Point stage1CoolingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage1CoolingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage1").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage1CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage1CoolingAirflowTempUpperOffset);
        hayStack.writePoint(stage1CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", -8.0, 0);
        hayStack.writeHisValById(stage1CoolingAirflowTempUpperOffsetId, -8.0);

        Point stage1HeatingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage1HeatingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage1").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage1HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage1HeatingAirflowTempUpperOffset);
        hayStack.writePoint(stage1HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 40.0, 0);
        hayStack.writeHisValById(stage1HeatingAirflowTempUpperOffsetId, 40.0);

        Point stage1HeatingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage1HeatingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage1").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage1HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage1HeatingAirflowTempLowerOffset);
        hayStack.writePoint(stage1HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 25.0, 0);
        hayStack.writeHisValById(stage1HeatingAirflowTempLowerOffsetId, 25.0);

        Point stage2CoolingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage2CoolingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage2").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage2CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage2CoolingAirflowTempLowerOffset);
        hayStack.writePoint(stage2CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", -25.0, 0);
        hayStack.writeHisValById(stage2CoolingAirflowTempLowerOffsetId, -25.0);

        Point stage2CoolingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage2CoolingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage2").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage2CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage2CoolingAirflowTempUpperOffset);
        hayStack.writePoint(stage2CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", -12.0, 0);
        hayStack.writeHisValById(stage2CoolingAirflowTempUpperOffsetId, -12.0);

        Point stage2HeatingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage2HeatingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage2").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage2HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage2HeatingAirflowTempUpperOffset);
        hayStack.writePoint(stage2HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 50.0, 0);
        hayStack.writeHisValById(stage2HeatingAirflowTempUpperOffsetId, 50.0);

        Point stage2HeatingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage2HeatingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage2").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage2HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage2HeatingAirflowTempLowerOffset);
        hayStack.writePoint(stage2HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 35.0, 0);
        hayStack.writeHisValById(stage2HeatingAirflowTempLowerOffsetId, 35.0);

        Point stage3CoolingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage3CoolingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage3").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage3CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage3CoolingAirflowTempLowerOffset);
        hayStack.writePoint(stage3CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", -25.0, 0);
        hayStack.writeHisValById(stage3CoolingAirflowTempLowerOffsetId, -25.0);

        Point stage3CoolingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage3CoolingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage3").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage3CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage3CoolingAirflowTempUpperOffset);
        hayStack.writePoint(stage3CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", -12.0, 0);
        hayStack.writeHisValById(stage3CoolingAirflowTempUpperOffsetId, -12.0);

        Point stage3HeatingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage3HeatingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage3").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage3HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage3HeatingAirflowTempUpperOffset);
        hayStack.writePoint(stage3HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 50.0, 0);
        hayStack.writeHisValById(stage3HeatingAirflowTempUpperOffsetId, 50.0);

        Point stage3HeatingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage3HeatingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage3").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage3HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage3HeatingAirflowTempLowerOffset);
        hayStack.writePoint(stage3HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 35.0, 0);
        hayStack.writeHisValById(stage3HeatingAirflowTempLowerOffsetId, 35.0);

        Point stage4CoolingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage4CoolingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage4").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage4CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage4CoolingAirflowTempLowerOffset);
        hayStack.writePoint(stage4CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", -25.0, 0);
        hayStack.writeHisValById(stage1CoolingAirflowTempLowerOffsetId, -25.0);

        Point stage4CoolingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage4CoolingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage4").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage4CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage4CoolingAirflowTempUpperOffset);
        hayStack.writePoint(stage4CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", -12.0, 0);
        hayStack.writeHisValById(stage4CoolingAirflowTempUpperOffsetId, -12.0);

        Point stage4HeatingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage4HeatingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage4").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage4HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage4HeatingAirflowTempUpperOffset);
        hayStack.writePoint(stage4HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 50.0, 0);
        hayStack.writeHisValById(stage4HeatingAirflowTempUpperOffsetId, 50.0);

        Point stage4HeatingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage4HeatingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage4").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage4HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage4HeatingAirflowTempLowerOffset);
        hayStack.writePoint(stage4HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 35.0, 0);
        hayStack.writeHisValById(stage4HeatingAirflowTempLowerOffsetId, 35.0);

        Point stage5CoolingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage5CoolingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage5").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage5CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage5CoolingAirflowTempLowerOffset);
        hayStack.writePoint(stage5CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", -25.0, 0);
        hayStack.writeHisValById(stage5CoolingAirflowTempLowerOffsetId, -25.0);

        Point stage5CoolingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage5CoolingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage5").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage5CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage5CoolingAirflowTempUpperOffset);
        hayStack.writePoint(stage5CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", -12.0, 0);
        hayStack.writeHisValById(stage5CoolingAirflowTempUpperOffsetId, -12.0);

        Point stage5HeatingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage5HeatingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage5").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage5HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage5HeatingAirflowTempUpperOffset);
        hayStack.writePoint(stage5HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 50.0, 0);
        hayStack.writeHisValById(stage5HeatingAirflowTempUpperOffsetId, 50.0);

        Point stage5HeatingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage5HeatingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage5").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage5HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage5HeatingAirflowTempLowerOffset);
        hayStack.writePoint(stage5HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 35.0, 0);
        hayStack.writeHisValById(stage5HeatingAirflowTempLowerOffsetId, 35.0);

        /*Point lightingIntensityOccupancyDetect  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"lightingIntensityOccupancyDetect")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("lighting").addMarker("intensity").addMarker("occupancy").addMarker("detect").addMarker("sp")
                .setMinVal("0").setMaxVal("100").setIncrementVal("5").setTunerGroup(TunerConstants.LCM_TUNER)
                .setUnit("%")
                .setTz(tz)
                .build();
        String lightingIntensityOccupancyDetectId = hayStack.addPoint(lightingIntensityOccupancyDetect);
        hayStack.writePoint(lightingIntensityOccupancyDetectId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 75.0, 0);
        hayStack.writeHisValById(lightingIntensityOccupancyDetectId, 75.0);

        Point minLightingControlOverride  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"minLightingControlOverride")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("min").addMarker("lighting").addMarker("control").addMarker("override").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("5").setTunerGroup(TunerConstants.LCM_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String minLightingControlOverrideId = hayStack.addPoint(minLightingControlOverride);
        hayStack.writePoint(minLightingControlOverrideId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 20.0, 0);
        hayStack.writeHisValById(minLightingControlOverrideId, 20.0);*/

        Point clockUpdateInterval  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"clockUpdateInterval")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("clock").addMarker("update").addMarker("interval").addMarker("sp")
                .setMinVal("1").setMaxVal("120").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String clockUpdateIntervalId = hayStack.addPoint(clockUpdateInterval);
        hayStack.writePoint(clockUpdateIntervalId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 15.0, 0);
        hayStack.writeHisValById(clockUpdateIntervalId, 15.0);

        Point perDegreeHumidityFactor  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"perDegreeHumidityFactor")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("per").addMarker("degree").addMarker("humidity").addMarker("factor").addMarker("sp")
                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("%")
                .setTz(tz)
                .build();
        String perDegreeHumidityFactorId = hayStack.addPoint(perDegreeHumidityFactor);
        hayStack.writePoint(perDegreeHumidityFactorId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 10.0, 0);
        hayStack.writeHisValById(perDegreeHumidityFactorId, 10.0);

        Point ccuAlarmVolumeLevel  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"ccuAlarmVolumeLevel")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("alarm").addMarker("volume").addMarker("level").addMarker("sp")
                .setMinVal("0").setMaxVal("7").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz)
                .build();
        String ccuAlarmVolumeLevelId = hayStack.addPoint(ccuAlarmVolumeLevel);
        hayStack.writePoint(ccuAlarmVolumeLevelId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 0.0, 0);
        hayStack.writeHisValById(ccuAlarmVolumeLevelId, 0.0);

        Point cmHeartBeatInterval  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"cmHeartBeatInterval")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("cm").addMarker("heart").addMarker("beat").addMarker("interval").addMarker("sp")
                .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String cmHeartBeatIntervalId = hayStack.addPoint(cmHeartBeatInterval);
        hayStack.writePoint(cmHeartBeatIntervalId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 1.0, 0);
        hayStack.writeHisValById(cmHeartBeatIntervalId, 1.0);

        Point heartBeatsToSkip  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"heartBeatsToSkip")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("heart").addMarker("beats").addMarker("to").addMarker("skip").addMarker("sp")
                .setMinVal("3").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz)
                .build();
        String heartBeatsToSkipId = hayStack.addPoint(heartBeatsToSkip);
        hayStack.writePoint(heartBeatsToSkipId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 5.0, 0);
        hayStack.writeHisValById(heartBeatsToSkipId, 5.0);

        Point rebalanceHoldTime = new Point.Builder()
                .setDisplayName(equipDis+"-"+"rebalanceHoldTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("rebalance").addMarker("hold").addMarker("time").addMarker("sp")
                .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String rebalanceHoldTimeId = hayStack.addPoint(rebalanceHoldTime);
        hayStack.writePoint(rebalanceHoldTimeId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", 20.0, 0);
        hayStack.writeHisValById(rebalanceHoldTimeId, 20.0);

        CCUHsApi.getInstance().syncEntityTree();
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
                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                                  .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setTz(tz)
                                  .build();
        String zonePrioritySpreadId = hayStack.addPoint(zonePrioritySpread);
        hayStack.writePoint(zonePrioritySpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_PRIORITY_SPREAD, 0);
        hayStack.writeHisValById(zonePrioritySpreadId, TunerConstants.ZONE_PRIORITY_SPREAD);
    
        Point zonePriorityMultiplier = new Point.Builder()
                                           .setDisplayName(equipDis+"-VAV-"+"zonePriorityMultiplier")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                           .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                           .setTz(tz)
                                           .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        hayStack.writePoint(zonePriorityMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_PRIORITY_MULTIPLIER, 0);
        hayStack.writeHisValById(zonePriorityMultiplierId, TunerConstants.ZONE_PRIORITY_MULTIPLIER);
        
        Point coolingDb = new Point.Builder()
                                  .setDisplayName(equipDis+"-VAV-"+"coolingDeadband")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        hayStack.writePoint(coolingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_COOLING_DB, 0);
        hayStack.writeHisValById(coolingDbId, TunerConstants.VAV_COOLING_DB);
    
        Point coolingDbMultiplier = new Point.Builder()
                                  .setDisplayName(equipDis+"-VAV-"+"coolingDeadbandMultiplier")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                  .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setTz(tz)
                                  .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        hayStack.writePoint(coolingDbMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_COOLING_DB_MULTPLIER, 0);
        hayStack.writeHisValById(coolingDbMultiplierId, TunerConstants.VAV_COOLING_DB_MULTPLIER);
        
        Point heatingDb = new Point.Builder()
                                  .setDisplayName(equipDis+"-VAV-"+"heatingDeadband")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        hayStack.writePoint(heatingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_HEATING_DB, 0);
        hayStack.writeHisValById(heatingDbId, TunerConstants.VAV_HEATING_DB);
    
        Point heatingDbMultiplier = new Point.Builder()
                                  .setDisplayName(equipDis+"-VAV-"+"heatingDeadbandMultiplier")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                  .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setTz(tz)
                                  .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        hayStack.writePoint(heatingDbMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_HEATING_DB_MULTIPLIER, 0);
        hayStack.writeHisValById(heatingDbMultiplierId, TunerConstants.VAV_HEATING_DB_MULTIPLIER);
        
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-VAV-"+"proportionalKFactor ")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .addMarker("pgain").addMarker("sp")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                 .setTz(tz)
                                 .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePoint(pgainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_GAIN, 0);
        hayStack.writeHisValById(pgainId, TunerConstants.VAV_PROPORTIONAL_GAIN);
        
        Point integralGain = new Point.Builder()
                                     .setDisplayName(equipDis+"-VAV-"+"integralKFactor ")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef).setHisInterpolate("cov")
                                     .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .addMarker("igain").addMarker("sp")
                                     .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                     .setTz(tz)
                                     .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePoint(igainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_GAIN, 0);
        hayStack.writeHisValById(igainId, TunerConstants.VAV_INTEGRAL_GAIN);
        
        Point propSpread = new Point.Builder()
                                   .setDisplayName(equipDis+"-VAV-"+"temperatureProportionalRange ")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef).setHisInterpolate("cov")
                                   .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                   .addMarker("pspread").addMarker("sp")
                                   .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                   .setTz(tz)
                                   .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        hayStack.writePoint(pSpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_SPREAD, 0);
        hayStack.writeHisValById(pSpreadId, TunerConstants.VAV_PROPORTIONAL_SPREAD);
        
        Point integralTimeout = new Point.Builder()
                                        .setDisplayName(equipDis+"-VAV-"+"temperatureIntegralTime ")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("itimeout").addMarker("sp")
                                        .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                        .setUnit("m")
                                        .setTz(tz)
                                        .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePoint(iTimeoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_TIMEOUT, 0);
        hayStack.writeHisValById(iTimeoutId, TunerConstants.VAV_INTEGRAL_TIMEOUT);
    
        Point valveStartDamper  = new Point.Builder()
                                        .setDisplayName(equipDis+"-VAV-"+"valveActuationStartDamperPosDuringSysHeating")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("valve").addMarker("start").addMarker("damper").addMarker("sp")
                                        .setMinVal("1").setMaxVal("100").setIncrementVal("5").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String valveStartDamperId = hayStack.addPoint(valveStartDamper);
        hayStack.writePoint(valveStartDamperId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VALVE_START_DAMPER, 0);
        hayStack.writeHisValById(valveStartDamperId, TunerConstants.VALVE_START_DAMPER);
    
        Point zoneCO2Target  = new Point.Builder()
                                          .setDisplayName(equipDis+"-VAV-"+"zoneCO2Target")
                                          .setSiteRef(siteRef)
                                          .setEquipRef(equipRef).setHisInterpolate("cov")
                                          .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                          .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                          .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                          .setUnit("ppm")
                                          .setTz(tz)
                                          .build();
        String zoneCO2TargetId = hayStack.addPoint(zoneCO2Target);
        hayStack.writePoint(zoneCO2TargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_CO2_TARGET, 0);
        hayStack.writeHisValById(zoneCO2TargetId, TunerConstants.ZONE_CO2_TARGET);
    
        Point zoneCO2Threshold  = new Point.Builder()
                                       .setDisplayName(equipDis+"-VAV-"+"zoneCO2Threshold")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                       .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                       .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                       .setUnit("ppm")
                                       .setTz(tz)
                                       .build();
        String zoneCO2ThresholdId = hayStack.addPoint(zoneCO2Threshold);
        hayStack.writePoint(zoneCO2ThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_CO2_THRESHOLD, 0);
        hayStack.writeHisValById(zoneCO2ThresholdId, TunerConstants.ZONE_CO2_THRESHOLD);
    
        Point zoneVOCTarget  = new Point.Builder()
                                       .setDisplayName(equipDis+"-VAV-"+"zoneVOCTarget")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                       .addMarker("zone").addMarker("voc").addMarker("target").addMarker("sp")
                                       .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                       .setUnit("ppb")
                                       .setTz(tz)
                                       .build();
        String zoneVOCTargetId = hayStack.addPoint(zoneVOCTarget);
        hayStack.writePoint(zoneVOCTargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_VOC_TARGET, 0);
        hayStack.writeHisValById(zoneVOCTargetId, TunerConstants.ZONE_VOC_TARGET);
    
        Point zoneVOCThreshold  = new Point.Builder()
                                          .setDisplayName(equipDis+"-VAV-"+"zoneVOCThreshold")
                                          .setSiteRef(siteRef)
                                          .setEquipRef(equipRef).setHisInterpolate("cov")
                                          .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                          .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                          .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                          .setUnit("ppb")
                                          .setTz(tz)
                                          .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);
        hayStack.writePoint(zoneVOCThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_VOC_THRESHOLD, 0);
        hayStack.writeHisValById(zoneVOCThresholdId, TunerConstants.ZONE_VOC_THRESHOLD);


        Point co2IgnoreRequest = new Point.Builder()
                .setDisplayName(equipDis+"-"+"co2IgnoreRequest")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("co2").addMarker("ignoreRequest").addMarker("sp")
                .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setTz(tz)
                .build();
        String co2IgnoreRequestId = hayStack.addPoint(co2IgnoreRequest);
        hayStack.writePoint(co2IgnoreRequestId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 2.0, 0);
        hayStack.writeHisValById(co2IgnoreRequestId,2.0);

        Point co2SPInit = new Point.Builder()
                .setDisplayName(equipDis+"-"+"co2SPInit")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("co2").addMarker("spinit").addMarker("sp")
                .setMinVal("0").setMaxVal("1500").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String co2SPInitId = hayStack.addPoint(co2SPInit);
        hayStack.writePoint(co2SPInitId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 800.0, 0);
        hayStack.writeHisValById(co2SPInitId,800.0);

        Point co2SPMax = new Point.Builder()
                .setDisplayName(equipDis+"-"+"co2SPMax")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("default").addMarker("co2").addMarker("spmax").addMarker("sp")
                .setMinVal("100").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String co2SPMaxId = hayStack.addPoint(co2SPMax);
        hayStack.writePoint(co2SPMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 1000.0, 0);
        hayStack.writeHisValById(co2SPMaxId,1000.0);

        Point co2SPMin = new Point.Builder()
                .setDisplayName(equipDis+"-"+"co2SPMin")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("co2").addMarker("spmin").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0").setMaxVal("1500").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String co2SPMinId = hayStack.addPoint(co2SPMin);
        hayStack.writePoint(co2SPMinId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 800.0,0 );
        hayStack.writeHisValById(co2SPMinId,800.0);

        Point co2SPRes = new Point.Builder()
                .setDisplayName(equipDis+"-"+"co2SPRes")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("co2").addMarker("spres").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("-30.0").setMaxVal("-1.0").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String co2SPResId = hayStack.addPoint(co2SPRes);
        hayStack.writePoint(co2SPResId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu",-10.0,0 );
        hayStack.writeHisValById(co2SPResId,-10.0);

        Point co2SPResMax = new Point.Builder()
                .setDisplayName(equipDis+"-"+"co2SPResMax")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("co2").addMarker("spresmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("-50.0").setMaxVal("-1.0").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String co2SPResMaxId = hayStack.addPoint(co2SPResMax);
        hayStack.writePoint(co2SPResMaxId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", -30.0,0 );
        hayStack.writeHisValById(co2SPResMaxId,-30.0);

        Point co2SPTrim = new Point.Builder()
                .setDisplayName(equipDis+"-"+"co2SPTrim")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("co2").addMarker("sptrim").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0").setMaxVal("50").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String co2SPTrimId = hayStack.addPoint(co2SPTrim);
        hayStack.writePoint(co2SPTrimId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 20.0,0 );
        hayStack.writeHisValById(co2SPTrimId,20.0);

        Point co2TimeDelay = new Point.Builder()
                .setDisplayName(equipDis+"-"+"co2TimeDelay")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("co2").addMarker("timeDelay").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String co2TimeDelayId = hayStack.addPoint(co2TimeDelay);
        hayStack.writePoint(co2TimeDelayId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu",2.0,0 );
        hayStack.writeHisValById(co2TimeDelayId,2.0);

        Point co2TimeInterval = new Point.Builder()
                .setDisplayName(equipDis+"-"+"co2TimeInterval")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("co2").addMarker("timeInterval").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String co2TimeIntervalId = hayStack.addPoint(co2TimeInterval);
        hayStack.writePoint(co2TimeIntervalId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 2.0,0 );
        hayStack.writeHisValById(co2TimeIntervalId,2.0);

        Point satIgnoreRequest = new Point.Builder()
                .setDisplayName(equipDis+"-"+"satIgnoreRequest")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("sat").addMarker("ignoreRequest").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setTz(tz)
                .build();
        String satIgnoreRequestId = hayStack.addPoint(satIgnoreRequest);
        hayStack.writePoint(satIgnoreRequestId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 2.0,0 );
        hayStack.writeHisValById(satIgnoreRequestId,2.0);

        Point satSPInit = new Point.Builder()
                .setDisplayName(equipDis+"-"+"satSPInit")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("sat").addMarker("spinit").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("50").setMaxVal("70").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String satSPInitId = hayStack.addPoint(satSPInit);
        hayStack.writePoint(satSPInitId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 65.0,0 );
        hayStack.writeHisValById(satSPInitId,65.0);

        Point satSPMax = new Point.Builder()
                .setDisplayName(equipDis+"-"+"satSPMax")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("sat").addMarker("spmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("55").setMaxVal("75").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String satSPMaxId = hayStack.addPoint(satSPMax);
        hayStack.writePoint(satSPMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu",65.0,0 );
        hayStack.writeHisValById(satSPMaxId,65.0);

        Point satSPMin = new Point.Builder()
                .setDisplayName(equipDis+"-"+"satSPMin")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("sat").addMarker("spmin").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("45").setMaxVal("65").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String satSPMinId = hayStack.addPoint(satSPMin);
        hayStack.writePoint(satSPMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu",55.0,0 );
        hayStack.writeHisValById(satSPMinId,55.0);

        Point satSPRes = new Point.Builder()
                .setDisplayName(equipDis+"-"+"satSPRes")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("sat").addMarker("spres").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMaxVal("-0.1").setMinVal("-2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String satSPResId = hayStack.addPoint(satSPRes);
        hayStack.writePoint(satSPResId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", -0.3,0 );
        hayStack.writeHisValById(satSPResId,-0.3);

        Point satSPResMax = new Point.Builder()
                .setDisplayName(equipDis+"-"+"satSPResMax")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("sat").addMarker("spresmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMaxVal("-0.1").setMinVal("-3.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String satSPResMaxId = hayStack.addPoint(satSPResMax);
        hayStack.writePoint(satSPResMaxId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", -1.0,0 );
        hayStack.writeHisValById(satSPResMaxId,-1.0);

        Point satSPTrim = new Point.Builder()
                .setDisplayName(equipDis+"-"+"satSPTrim")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("sat").addMarker("sptrim").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("-0.5").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String satSPTrimId = hayStack.addPoint(satSPTrim);
        hayStack.writePoint(satSPTrimId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 0.2,0 );
        hayStack.writeHisValById(satSPTrimId,0.2);

        Point satTimeDelay = new Point.Builder()
                .setDisplayName(equipDis+"-"+"satTimeDelay")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("sat").addMarker("timeDelay").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0").setMaxVal("30").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String satTimeDelayId = hayStack.addPoint(satTimeDelay);
        hayStack.writePoint(satTimeDelayId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 2.0,0);
        hayStack.writeHisValById(satTimeDelayId,2.0);

        Point satTimeInterval = new Point.Builder()
                .setDisplayName(equipDis+"-"+"satTimeInterval")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("sat").addMarker("timeInterval").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String satTimeIntervalId = hayStack.addPoint(satTimeInterval);
        hayStack.writePoint(satTimeIntervalId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 2.0,0 );
        hayStack.writeHisValById(satTimeIntervalId,2.0);

        Point staticPressureIgnoreRequest = new Point.Builder()
                .setDisplayName(equipDis+"-"+"staticPressureIgnoreRequest")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("staticPressure").addMarker("ignoreRequest").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setTz(tz)
                .build();
        String staticPressureIgnoreRequestId = hayStack.addPoint(staticPressureIgnoreRequest);
        hayStack.writePoint(staticPressureIgnoreRequestId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu",2.0,0 );
        hayStack.writeHisValById(staticPressureIgnoreRequestId,2.0);

        Point staticPressureSPInit = new Point.Builder()
                .setDisplayName(equipDis+"-"+"staticPressureSPInit")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("staticPressure").addMarker("spinit").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0.1").setMaxVal("2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("inch wc")
                .setTz(tz)
                .build();
        String staticPressureSPInitId = hayStack.addPoint(staticPressureSPInit);
        hayStack.writePoint(staticPressureSPInitId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu",0.2,0);
        hayStack.writeHisValById(staticPressureSPInitId,0.2);

        Point staticPressureSPMax = new Point.Builder()
                .setDisplayName(equipDis+"-"+"staticPressureSPMax")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("staticPressure").addMarker("spmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0.1").setMaxVal("2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("inch wc")
                .setTz(tz)
                .build();
        String staticPressureSPMaxId = hayStack.addPoint(staticPressureSPMax);
        hayStack.writePoint(staticPressureSPMaxId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 1.0,0 );
        hayStack.writeHisValById(staticPressureSPMaxId,1.0);

        Point staticPressureSPMin = new Point.Builder()
                .setDisplayName(equipDis+"-"+"staticPressureSPMin")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("staticPressure").addMarker("spmin").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0.1").setMaxVal("2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("inch wc")
                .setTz(tz)
                .build();
        String staticPressureSPMinId = hayStack.addPoint(staticPressureSPMin);
        hayStack.writePoint(staticPressureSPMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu",0.2 ,0);
        hayStack.writeHisValById(staticPressureSPMinId,0.2);

        Point staticPressureSPRes = new Point.Builder()
                .setDisplayName(equipDis+"-"+"staticPressureSPRes")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("staticPressure").addMarker("spres").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0.01").setMaxVal("0.2").setIncrementVal("0.01").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("inch wc")
                .setTz(tz)
                .build();
        String staticPressureSPResId = hayStack.addPoint(staticPressureSPRes);
        hayStack.writePoint(staticPressureSPResId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu",0.05,0 );
        hayStack.writeHisValById(staticPressureSPResId,0.05);

        Point staticPressureSPResMax = new Point.Builder()
                .setDisplayName(equipDis+"-"+"staticPressureSPResMax")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("staticPressure").addMarker("spresmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0.05").setMaxVal("0.5").setIncrementVal("0.05").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("inch wc")
                .setTz(tz)
                .build();
        String staticPressureSPResMaxId = hayStack.addPoint(staticPressureSPResMax);
        hayStack.writePoint(staticPressureSPResMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu",0.1,0 );
        hayStack.writeHisValById(staticPressureSPResMaxId,0.1);

        Point staticPressureSPTrim = new Point.Builder()
                .setDisplayName(equipDis+"-"+"staticPressureSPTrim")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("staticPressure").addMarker("sptrim").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("-0.01").setMaxVal("-0.5").setIncrementVal("-0.01").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("inch wc")
                .setTz(tz)
                .build();
        String staticPressureSPTrimId = hayStack.addPoint(staticPressureSPTrim);
        hayStack.writePoint(staticPressureSPTrimId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", -0.02 ,0);
        hayStack.writeHisValById(staticPressureSPTrimId,-0.02);

        Point staticPressureTimeDelay = new Point.Builder()
                .setDisplayName(equipDis+"-"+"staticPressureTimeDelay")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("staticPressure").addMarker("timeDelay").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0").setMaxVal("30").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String staticPressureTimeDelayId = hayStack.addPoint(staticPressureTimeDelay);
        hayStack.writePoint(staticPressureTimeDelayId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu",2.0,0 );
        hayStack.writeHisValById(staticPressureTimeDelayId,2.0);

        Point staticPressureTimeInterval = new Point.Builder()
                .setDisplayName(equipDis+"-"+"staticPressureTimeInterval")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("tr").addMarker("default").addMarker("vav").addMarker("sp")
                .addMarker("staticPressure").addMarker("timeInterval").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0").setMaxVal("30").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String staticPressureTimeIntervalId = hayStack.addPoint(staticPressureTimeInterval);
        hayStack.writePoint(staticPressureTimeIntervalId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu",2.0 ,0);
        hayStack.writeHisValById(staticPressureTimeIntervalId,2.0);
        addDefaultVavSystemTuners();
        
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    public void addDefaultVavSystemTuners()
    {
        Point targetCumulativeDamper = new Point.Builder()
                                               .setDisplayName(equipDis+"-VAV-"+"targetCumulativeDamper")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef).setHisInterpolate("cov")
                                               .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .addMarker("target").addMarker("cumulative").addMarker("damper").addMarker("sp")
                                               .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                               .setUnit("%")
                                               .setTz(tz)
                                               .build();
        String targetCumulativeDamperId = hayStack.addPoint(targetCumulativeDamper);
        hayStack.writePoint(targetCumulativeDamperId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.TARGET_CUMULATIVE_DAMPER, 0);
        hayStack.writeHisValById(targetCumulativeDamperId, TunerConstants.TARGET_CUMULATIVE_DAMPER);
    
        Point analogFanSpeedMultiplier = new Point.Builder()
                                               .setDisplayName(equipDis+"-VAV-"+"analogFanSpeedMultiplier")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef).setHisInterpolate("cov")
                                               .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .addMarker("analog").addMarker("fan").addMarker("speed").addMarker("multiplier").addMarker("sp")
                                               .setMinVal("0.1").setMaxVal("3.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                               .setTz(tz)
                                               .build();
        String analogFanSpeedMultiplierId = hayStack.addPoint(analogFanSpeedMultiplier);
        hayStack.writePoint(analogFanSpeedMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ANALOG_FANSPEED_MULTIPLIER, 0);
        hayStack.writeHisValById(analogFanSpeedMultiplierId, TunerConstants.ANALOG_FANSPEED_MULTIPLIER);
    
        Point humidityHysteresis = new Point.Builder()
                                                 .setDisplayName(equipDis+"-VAV-"+"humidityHysteresis")
                                                 .setSiteRef(siteRef)
                                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                                 .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                 .addMarker("humidity").addMarker("hysteresis").addMarker("sp")
                                                 .setMinVal("0").setMaxVal("100").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                 .setUnit("%")
                                                 .setTz(tz)
                                                 .build();
        String humidityHysteresisId = hayStack.addPoint(humidityHysteresis);
        hayStack.writePoint(humidityHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.HUMIDITY_HYSTERESIS_PERCENT, 0);
        hayStack.writeHisValById(humidityHysteresisId, TunerConstants.HUMIDITY_HYSTERESIS_PERCENT);
    
        Point relayDeactivationHysteresis = new Point.Builder()
                                           .setDisplayName(equipDis+"-VAV-"+"relayDeactivationHysteresis")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("relay").addMarker("deactivation").addMarker("hysteresis").addMarker("sp")
                                            .setMinVal("0").setMaxVal("100").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                           .setUnit("%")
                                           .setTz(tz)
                                           .build();
        String relayDeactivationHysteresisId = hayStack.addPoint(relayDeactivationHysteresis);
        hayStack.writePoint(relayDeactivationHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.RELAY_DEACTIVATION_HYSTERESIS, 0);
        hayStack.writeHisValById(relayDeactivationHysteresisId, TunerConstants.RELAY_DEACTIVATION_HYSTERESIS);
        
    }
    
    public void addEquipZoneTuners(String equipdis, String equipref, String roomRef, String floorRef) {
        Point unoccupiedZoneSetback = new Point.Builder()
                                           .setDisplayName(equipdis+"-"+"unoccupiedZoneSetback")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipref)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("zone").addMarker("unoccupied").addMarker("setback").addMarker("sp")
                                           .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                           .setUnit("\u00B0F")
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

        Point zoneDeadTime = new Point.Builder()
                .setDisplayName(equipdis+"-"+"zoneDeadTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("1").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                .addMarker("zone").addMarker("dead").addMarker("time").addMarker("sp")
                .setUnit("m")
                .setTz(tz)
                .build();
        String zoneDeadTimeId = hayStack.addPoint(zoneDeadTime);
        HashMap zoneDeadTimePoint = hayStack.read("point and tuner and default and zone and dead and time");
        ArrayList<HashMap> zoneDeadTimeArr = hayStack.readPoint(zoneDeadTimePoint.get("id").toString());
        for (HashMap valMap : zoneDeadTimeArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(zoneDeadTimeId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(zoneDeadTimeId, HSUtil.getPriorityVal(zoneDeadTimeId));

        Point autoAwayTime = new Point.Builder()
                .setDisplayName(equipdis+"-"+"autoAwayTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("40").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                .addMarker("zone").addMarker("auto").addMarker("away").addMarker("time").addMarker("sp")
                .setUnit("m")
                .setTz(tz)
                .build();
        String autoAwayTimeId = hayStack.addPoint(autoAwayTime);
        HashMap autoAwayTimePoint = hayStack.read("point and tuner and default and auto and away and time");
        ArrayList<HashMap> autoAwayTimeArr = hayStack.readPoint(autoAwayTimePoint.get("id").toString());
        for (HashMap valMap : autoAwayTimeArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(autoAwayTimeId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(autoAwayTimeId, HSUtil.getPriorityVal(autoAwayTimeId));

        Point forcedOccupiedTime = new Point.Builder()
                .setDisplayName(equipdis+"-"+"forcedOccupiedTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("30").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                .addMarker("zone").addMarker("forced").addMarker("occupied").addMarker("time").addMarker("sp")
                .setUnit("m")
                .setTz(tz)
                .build();
        String forcedOccupiedTimeId = hayStack.addPoint(forcedOccupiedTime);
        HashMap forcedOccupiedTimePoint = hayStack.read("point and tuner and default and forced and occupied and time");
        ArrayList<HashMap> forcedOccupiedTimeArr = hayStack.readPoint(forcedOccupiedTimePoint.get("id").toString());
        for (HashMap valMap : forcedOccupiedTimeArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(forcedOccupiedTimeId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(forcedOccupiedTimeId, HSUtil.getPriorityVal(forcedOccupiedTimeId));

        Point adrCoolingDeadband = new Point.Builder()
                .setDisplayName(equipdis+"-"+"adrCoolingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("adr").addMarker("cooling").addMarker("deadband").addMarker("sp")
                .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String adrCoolingDeadbandId = hayStack.addPoint(adrCoolingDeadband);
        HashMap adrCoolingDeadbandPoint = hayStack.read("point and tuner and default and adr and cooling and deadband");
        ArrayList<HashMap> adrCoolingDeadbandArr = hayStack.readPoint(adrCoolingDeadbandPoint.get("id").toString());
        for (HashMap valMap : adrCoolingDeadbandArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(adrCoolingDeadbandId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(adrCoolingDeadbandId, HSUtil.getPriorityVal(adrCoolingDeadbandId));

        Point adrHeatingDeadband = new Point.Builder()
                .setDisplayName(equipdis+"-"+"adrHeatingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("adr").addMarker("heating").addMarker("deadband").addMarker("sp")
                .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String adrHeatingDeadbandId = hayStack.addPoint(adrHeatingDeadband);
        HashMap adrHeatingDeadbandPoint = hayStack.read("point and tuner and default and adr and heating and deadband");
        ArrayList<HashMap> adrHeatingDeadbandArr = hayStack.readPoint(adrHeatingDeadbandPoint.get("id").toString());
        for (HashMap valMap : adrHeatingDeadbandArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(adrHeatingDeadbandId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(adrHeatingDeadbandId, HSUtil.getPriorityVal(adrHeatingDeadbandId));

        Point snCoolingAirflowTemp = new Point.Builder()
                .setDisplayName(equipdis+"-"+"snCoolingAirflowTemp")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("sn").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("sp")
                .setMinVal("35").setMaxVal("70").setIncrementVal("1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String snCoolingAirflowTempId = hayStack.addPoint(snCoolingAirflowTemp);
        HashMap snCoolingAirflowTempPoint = hayStack.read("point and tuner and default and sn and cooling and airflow and temp");
        ArrayList<HashMap> snCoolingAirflowTempArr = hayStack.readPoint(snCoolingAirflowTempPoint.get("id").toString());
        for (HashMap valMap : snCoolingAirflowTempArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(snCoolingAirflowTempId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(snCoolingAirflowTempId, HSUtil.getPriorityVal(snCoolingAirflowTempId));

        Point snHeatingAirflowTemp = new Point.Builder()
                .setDisplayName(equipdis+"-"+"snHeatingAirflowTemp")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("sn").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("sp")
                .setMinVal("80").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String snHeatingAirflowTempId = hayStack.addPoint(snHeatingAirflowTemp);
        HashMap snHeatingAirflowTempPoint = hayStack.read("point and tuner and default and sn and heating and airflow and temp");
        ArrayList<HashMap> snHeatingAirflowTempArr = hayStack.readPoint(snHeatingAirflowTempPoint.get("id").toString());
        for (HashMap valMap : snHeatingAirflowTempArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(snHeatingAirflowTempId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(snHeatingAirflowTempId, HSUtil.getPriorityVal(snHeatingAirflowTempId));

        Point constantTempAlertTime = new Point.Builder()
                .setDisplayName(equipdis+"-"+"constantTempAlertTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("constant").addMarker("temp").addMarker("alert").addMarker("time").addMarker("sp")
                .setMinVal("0").setMaxVal("360").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String constantTempAlertTimeId = hayStack.addPoint(constantTempAlertTime);
        HashMap constantTempAlertTimePoint = hayStack.read("point and tuner and default and constant and temp and alert and time");
        ArrayList<HashMap> constantTempAlertTimeArr = hayStack.readPoint(constantTempAlertTimePoint.get("id").toString());
        for (HashMap valMap : constantTempAlertTimeArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(constantTempAlertTimeId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(constantTempAlertTimeId, HSUtil.getPriorityVal(constantTempAlertTimeId));

        Point abnormalCurTempRiseTrigger = new Point.Builder()
                .setDisplayName(equipdis+"-"+"abnormalCurTempRiseTrigger")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("abnormal").addMarker("cur").addMarker("temp").addMarker("rise").addMarker("trigger").addMarker("sp")
                .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String abnormalCurTempRiseTriggerId = hayStack.addPoint(abnormalCurTempRiseTrigger);
        HashMap abnormalCurTempRiseTriggerPoint = hayStack.read("point and tuner and default and abnormal and cur and temp and rise and trigger");
        ArrayList<HashMap> abnormalCurTempRiseTriggerArr = hayStack.readPoint(abnormalCurTempRiseTriggerPoint.get("id").toString());
        for (HashMap valMap : abnormalCurTempRiseTriggerArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(abnormalCurTempRiseTriggerId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(abnormalCurTempRiseTriggerId, HSUtil.getPriorityVal(abnormalCurTempRiseTriggerId));

       /* Point lightingIntensityOccupancyDetect = new Point.Builder()
                .setDisplayName(equipdis+"-"+"lightingIntensityOccupancyDetect")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("lighting").addMarker("intensity").addMarker("occupancy").addMarker("detect").addMarker("sp")
                .setMinVal("0").setMaxVal("100").setIncrementVal("5").setTunerGroup(TunerConstants.LCM_TUNER)
                .setUnit("%")
                .setTz(tz)
                .build();
        String lightingIntensityOccupancyDetectId = hayStack.addPoint(lightingIntensityOccupancyDetect);
        HashMap lightingIntensityOccupancyDetectPoint = hayStack.read("point and tuner and default and lighting and intensity and occupancy and detect");
        ArrayList<HashMap> lightingIntensityOccupancyDetectArr = hayStack.readPoint(lightingIntensityOccupancyDetectPoint.get("id").toString());
        for (HashMap valMap : lightingIntensityOccupancyDetectArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(lightingIntensityOccupancyDetectId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(lightingIntensityOccupancyDetectId, HSUtil.getPriorityVal(lightingIntensityOccupancyDetectId));

        Point minLightingControlOverride = new Point.Builder()
                .setDisplayName(equipdis+"-"+"minLightingControlOverride")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("min").addMarker("lighting").addMarker("control").addMarker("override").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("5").setTunerGroup(TunerConstants.LCM_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String minLightingControlOverrideId = hayStack.addPoint(minLightingControlOverride);
        HashMap minLightingControlOverridePoint = hayStack.read("point and tuner and default and min and lighting and control and override");
        ArrayList<HashMap> minLightingControlOverrideArr = hayStack.readPoint(minLightingControlOverridePoint.get("id").toString());
        for (HashMap valMap : minLightingControlOverrideArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(minLightingControlOverrideId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(minLightingControlOverrideId, HSUtil.getPriorityVal(minLightingControlOverrideId));
        */
    }
    
    public void addVavEquipTuners(String equipdis, String equipref, String roomRef, String floorRef) {
    
        Log.d("CCU","addVavEquipTuners for "+equipdis);
    
        addEquipZoneTuners(equipdis, equipref, roomRef, floorRef);
        
        Point zonePrioritySpread = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"zonePrioritySpread")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                                  .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                           .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                  .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                  .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                 .setRoomRef(roomRef)
                                 .setFloorRef(floorRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .addMarker("pgain").addMarker("sp")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                     .setRoomRef(roomRef)
                                     .setFloorRef(floorRef).setHisInterpolate("cov")
                                     .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .addMarker("igain").addMarker("sp")
                                     .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                   .setRoomRef(roomRef)
                                   .setFloorRef(floorRef).setHisInterpolate("cov")
                                   .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                   .addMarker("pspread").addMarker("sp")
                                   .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                        .setRoomRef(roomRef)
                                        .setFloorRef(floorRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("itimeout").addMarker("sp")
                                        .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                        .setUnit("m")
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
                                        .setRoomRef(roomRef)
                                        .setFloorRef(floorRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("valve").addMarker("start").addMarker("damper").addMarker("sp")
                                        .setMinVal("0").setMaxVal("100").setIncrementVal("5").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                        .setUnit("%")
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
                                      .setRoomRef(roomRef)
                                      .setFloorRef(floorRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                      .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                      .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                      .setUnit("ppm")
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
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                         .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                         .setUnit("ppm")
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
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("sp").addMarker("equipHis")
                                         .addMarker("zone").addMarker("voc").addMarker("target")
                                         .setUnit("ppb")
                                         .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
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
                                        .setRoomRef(roomRef)
                                        .setFloorRef(floorRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                        .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                        .setUnit("ppb")
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
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("default").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                 .addMarker("pgain").addMarker("sp")
                                 .setTz(tz)
                                 .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePoint(pgainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_GAIN, 0);
        hayStack.writeHisValById(pgainId, TunerConstants.VAV_PROPORTIONAL_GAIN);
    
        Point integralGain = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"integralKFactor")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef).setHisInterpolate("cov")
                                     .addMarker("tuner").addMarker("default").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                     .addMarker("igain").addMarker("sp")
                                     .setTz(tz)
                                     .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePoint(igainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_GAIN, 0);
        hayStack.writeHisValById(igainId, TunerConstants.VAV_INTEGRAL_GAIN);
    
        Point integralTimeout = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"pidIntegralTime")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                        .addMarker("itimeout").addMarker("sp")
                                        .setUnit("m")
                                        .setTz(tz)
                                        .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePoint(iTimeoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_TIMEOUT, 0);
        hayStack.writeHisValById(iTimeoutId, TunerConstants.VAV_INTEGRAL_TIMEOUT);
    }
    
    public void addPlcEquipTuners(String equipdis, String equipref, String roomRef, String floorRef){
        
        //addEquipZoneTuners(equipdis, equipref);
        
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipdis+"-"+"proportionalKFactor")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .setRoomRef(roomRef)
                                 .setFloorRef(floorRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
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
                                     .setRoomRef(roomRef)
                                     .setFloorRef(floorRef).setHisInterpolate("cov")
                                     .addMarker("tuner").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
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
                                        .setRoomRef(roomRef)
                                        .setFloorRef(floorRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                        .addMarker("itimeout").addMarker("sp")
                                        .setUnit("m")
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
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                                               .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                               .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                               .setTz(tz)
                                               .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        hayStack.writePoint(zonePriorityMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_PRIORITY_MULTIPLIER, 0);
        hayStack.writeHisValById(zonePriorityMultiplierId, TunerConstants.ZONE_PRIORITY_MULTIPLIER);
    
        Point coolingDb = new Point.Builder()
                                  .setDisplayName(equipDis+"-DAB-"+"coolingDeadband")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                            .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                                  .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                            .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                                 .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                                     .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                                   .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                                        .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                                       .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                                          .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                                       .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                                          .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                          .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                          .setUnit("ppb")
                                          .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                               .setEquipRef(equipRef).setHisInterpolate("cov")
                                               .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                                                 .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                                           .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                                                    .addMarker("tuner").addMarker("default").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                    .addMarker("relay").addMarker("deactivation").addMarker("hysteresis").addMarker("sp")
                                                    .setMinVal("0").setMaxVal("10").setIncrementVal("0.5").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                                                    .setUnit("%")
                                                    .setTz(tz)
                                                    .build();
        String relayDeactivationHysteresisId = hayStack.addPoint(relayDeactivationHysteresis);
        hayStack.writePoint(relayDeactivationHysteresisId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.RELAY_DEACTIVATION_HYSTERESIS, 0);
        hayStack.writeHisValById(relayDeactivationHysteresisId, TunerConstants.RELAY_DEACTIVATION_HYSTERESIS);

    }
    
    public void addEquipDabTuners(String equipdis, String equipref, String roomRef, String floorRef) {
        Log.d("CCU","addEquipDabTuners for "+equipdis);
    
        addEquipZoneTuners(equipdis, equipref, roomRef, floorRef);
        
        Point zonePrioritySpread = new Point.Builder()
                                           .setDisplayName(equipdis+"-"+"zonePrioritySpread")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipref)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                                           .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                               .setRoomRef(roomRef)
                                               .setFloorRef(floorRef).setHisInterpolate("cov")
                                               .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                               .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                            .setRoomRef(roomRef)
                                            .setFloorRef(floorRef).setHisInterpolate("cov")
                                            .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                            .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef).setHisInterpolate("cov")
                                  .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                                  .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                            .setRoomRef(roomRef)
                                            .setFloorRef(floorRef).setHisInterpolate("cov")
                                            .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                            .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                 .setRoomRef(roomRef)
                                 .setFloorRef(floorRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .addMarker("pgain").addMarker("sp")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                     .setRoomRef(roomRef)
                                     .setFloorRef(floorRef).setHisInterpolate("cov")
                                     .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .addMarker("igain").addMarker("sp")
                                     .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                   .setRoomRef(roomRef)
                                   .setFloorRef(floorRef).setHisInterpolate("cov")
                                   .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                   .addMarker("pspread").addMarker("sp")
                                   .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                        .setRoomRef(roomRef)
                                        .setFloorRef(floorRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("itimeout").addMarker("sp")
                                        .setUnit("m")
                                        .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                      .setRoomRef(roomRef)
                                      .setFloorRef(floorRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                      .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                      .setUnit("ppm")
                                      .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                         .setUnit("ppm")
                                         .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                      .setRoomRef(roomRef)
                                      .setFloorRef(floorRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("sp").addMarker("equipHis")
                                      .addMarker("zone").addMarker("voc").addMarker("target")
                                      .setUnit("ppb")
                                      .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                         .setUnit("ppb")
                                         .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
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

        /*Point rebalanceHoldTime = new Point.Builder()
                .setDisplayName(equipDis+"-DAB-"+"rebalanceHoldTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .addMarker("tuner").addMarker("dab").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("rebalance").addMarker("hold").addMarker("time").addMarker("sp")
                .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.DAB_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String rebalanceHoldTimeId = hayStack.addPoint(rebalanceHoldTime);

        HashMap rebalanceHoldTimePoint = hayStack.read("point and tuner and default and rebalance and hold and time");
        ArrayList<HashMap> rebalanceHoldTimeArr = hayStack.readPoint(rebalanceHoldTimePoint.get("id").toString());
        for (HashMap valMap : rebalanceHoldTimeArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(rebalanceHoldTimeId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(rebalanceHoldTimeId, HSUtil.getPriorityVal(rebalanceHoldTimeId));*/
    
    }
    public void addEquipTiTuners(String equipdis, String equipref, String roomRef, String floorRef) {
        Log.d("CCU","addEquipTiTuners for "+equipdis);

        addEquipZoneTuners(equipdis, equipref, roomRef, floorRef);

        Point zonePrioritySpread = new Point.Builder()
                .setDisplayName(equipdis+"-"+"zonePrioritySpread")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
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
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his").addMarker("equipHis")
                .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
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
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his").addMarker("equipHis")
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
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("itimeout").addMarker("sp")
                .setUnit("m")
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


    }
    public void addEquipZoneStandaloneTuners(String equipdis, String equipref, String roomRef, String floorRef) {
        Point saHeatingDeadBand = new Point.Builder()
                .setDisplayName(equipdis+"-"+"standaloneHeatingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("standalone").addMarker("heating").addMarker("deadband").addMarker("sp")
                .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
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
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("cooling").addMarker("deadband").addMarker("sp")
                .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
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
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage1").addMarker("hysteresis").addMarker("sp")
                .setMinVal("0.5").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("%")
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

        Point standaloneCoolingPreconditioningRate = new Point.Builder()
                .setDisplayName(equipdis+"-"+"standaloneCoolingPreconditioningRate")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("cooling").addMarker("preconditioning").addMarker("rate").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String standaloneCoolingPreconditioningRateId = hayStack.addPoint(standaloneCoolingPreconditioningRate);
        HashMap standaloneCoolingPreconditioningRatePoint = hayStack.read("point and tuner and default and base and standalone and cooling and preconditioning and rate");
        ArrayList<HashMap> standaloneCoolingPreconditioningRateArr = hayStack.readPoint(standaloneCoolingPreconditioningRatePoint.get("id").toString());
        for (HashMap valMap : standaloneCoolingPreconditioningRateArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(standaloneCoolingPreconditioningRateId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(standaloneCoolingPreconditioningRateId, HSUtil.getPriorityVal(standaloneCoolingPreconditioningRateId));

        Point standaloneHeatingPreconditioningRate = new Point.Builder()
                .setDisplayName(equipdis+"-"+"standaloneHeatingPreconditioningRate")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("heating").addMarker("preconditioning").addMarker("rate").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String standaloneHeatingPreconditioningRateId = hayStack.addPoint(standaloneHeatingPreconditioningRate);
        HashMap standaloneHeatingPreconditioningRatePoint = hayStack.read("point and tuner and default and base and standalone and heating and preconditioning and rate");
        ArrayList<HashMap> standaloneHeatingPreconditioningRateArr = hayStack.readPoint(standaloneHeatingPreconditioningRatePoint.get("id").toString());
        for (HashMap valMap : standaloneHeatingPreconditioningRateArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(standaloneHeatingPreconditioningRateId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(standaloneHeatingPreconditioningRateId, HSUtil.getPriorityVal(standaloneHeatingPreconditioningRateId));

        Point standaloneCoolingAirflowTempLowerOffset = new Point.Builder()
                .setDisplayName(equipdis+"-"+"standaloneCoolingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String standaloneCoolingAirflowTempLowerOffsetId = hayStack.addPoint(standaloneCoolingAirflowTempLowerOffset);
        HashMap standaloneCoolingAirflowTempLowerOffsetPoint = hayStack.read("point and tuner and default and base and standalone and cooling and airflow and temp and lower and offset");
        ArrayList<HashMap> standaloneCoolingAirflowTempLowerOffsetArr = hayStack.readPoint(standaloneCoolingAirflowTempLowerOffsetPoint.get("id").toString());
        for (HashMap valMap : standaloneCoolingAirflowTempLowerOffsetArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(standaloneCoolingAirflowTempLowerOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(standaloneCoolingAirflowTempLowerOffsetId, HSUtil.getPriorityVal(standaloneCoolingAirflowTempLowerOffsetId));

        Point standaloneCoolingAirflowTempUpperOffset = new Point.Builder()
                .setDisplayName(equipdis+"-"+"standaloneCoolingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String standaloneCoolingAirflowTempUpperOffsetId = hayStack.addPoint(standaloneCoolingAirflowTempUpperOffset);
        HashMap standaloneCoolingAirflowTempUpperOffsetPoint = hayStack.read("point and tuner and default and base and standalone and cooling and airflow and temp and upper and offset");
        ArrayList<HashMap> standaloneCoolingAirflowTempUpperOffsetArr = hayStack.readPoint(standaloneCoolingAirflowTempUpperOffsetPoint.get("id").toString());
        for (HashMap valMap : standaloneCoolingAirflowTempUpperOffsetArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(standaloneCoolingAirflowTempUpperOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(standaloneCoolingAirflowTempUpperOffsetId, HSUtil.getPriorityVal(standaloneCoolingAirflowTempUpperOffsetId));

        Point standaloneHeatingAirflowTempLowerOffset = new Point.Builder()
                .setDisplayName(equipdis+"-"+"standaloneHeatingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String standaloneHeatingAirflowTempLowerOffsetId = hayStack.addPoint(standaloneHeatingAirflowTempLowerOffset);
        HashMap standaloneHeatingAirflowTempLowerOffsetPoint = hayStack.read("point and tuner and default and base and standalone and heating and airflow and temp and lower and offset");
        ArrayList<HashMap> standaloneHeatingAirflowTempLowerOffsetArr = hayStack.readPoint(standaloneHeatingAirflowTempLowerOffsetPoint.get("id").toString());
        for (HashMap valMap : standaloneHeatingAirflowTempLowerOffsetArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(standaloneHeatingAirflowTempLowerOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(standaloneHeatingAirflowTempLowerOffsetId, HSUtil.getPriorityVal(standaloneHeatingAirflowTempLowerOffsetId));

        Point standaloneHeatingAirflowTempUpperOffset = new Point.Builder()
                .setDisplayName(equipdis+"-"+"standaloneHeatingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String standaloneHeatingAirflowTempUpperOffsetId = hayStack.addPoint(standaloneHeatingAirflowTempUpperOffset);
        HashMap standaloneHeatingAirflowTempUpperOffsetPoint = hayStack.read("point and tuner and default and base and standalone and heating and airflow and temp and upper and offset");
        ArrayList<HashMap> standaloneHeatingAirflowTempUpperOffsetArr = hayStack.readPoint(standaloneHeatingAirflowTempUpperOffsetPoint.get("id").toString());
        for (HashMap valMap : standaloneHeatingAirflowTempUpperOffsetArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(standaloneHeatingAirflowTempUpperOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(standaloneHeatingAirflowTempUpperOffsetId, HSUtil.getPriorityVal(standaloneHeatingAirflowTempUpperOffsetId));

        Point standaloneAirflowSampleWaitTime = new Point.Builder()
                .setDisplayName(equipdis+"-"+"standaloneAirflowSampleWaitTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("airflow").addMarker("sample").addMarker("wait").addMarker("time").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String standaloneAirflowSampleWaitTimeId = hayStack.addPoint(standaloneAirflowSampleWaitTime);
        HashMap standaloneAirflowSampleWaitTimePoint = hayStack.read("point and tuner and default and base and standalone and airflow and sample and wait and time");
        ArrayList<HashMap> standaloneAirflowSampleWaitTimeArr = hayStack.readPoint(standaloneAirflowSampleWaitTimePoint.get("id").toString());
        for (HashMap valMap : standaloneAirflowSampleWaitTimeArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(standaloneAirflowSampleWaitTimeId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(standaloneAirflowSampleWaitTimeId, HSUtil.getPriorityVal(standaloneAirflowSampleWaitTimeId));


        Point zoneCO2Target  = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneCO2Target")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String zoneCO2TargetId = hayStack.addPoint(zoneCO2Target);

        HashMap zoneCO2TargetIdPoint = hayStack.read("point and tuner and default and base and standalone and zone and co2 and target and sp");
        if(zoneCO2TargetIdPoint != null && zoneCO2TargetIdPoint.get("id") != null) {
            ArrayList<HashMap> zoneCO2TargetIdArr = hayStack.readPoint(zoneCO2TargetIdPoint.get("id").toString());
            for (HashMap valMap : zoneCO2TargetIdArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(zoneCO2TargetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(zoneCO2TargetId, HSUtil.getPriorityVal(zoneCO2TargetId));
        }
        Point zoneCO2Threshold  = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneCO2Threshold")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String zoneCO2ThresholdId = hayStack.addPoint(zoneCO2Threshold);

        HashMap zoneCO2ThresholdIdPoint = hayStack.read("point and tuner and default and base and standalone and zone and co2 and threshold and sp");
        if(zoneCO2ThresholdIdPoint != null && zoneCO2ThresholdIdPoint.get("id") != null) {
            ArrayList<HashMap> zoneCO2ThresholdIdArr = hayStack.readPoint(zoneCO2ThresholdIdPoint.get("id").toString());
            for (HashMap valMap : zoneCO2ThresholdIdArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(zoneCO2ThresholdId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(zoneCO2ThresholdId, HSUtil.getPriorityVal(zoneCO2ThresholdId));
        }

        Point zoneVOCTarget  = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneVOCTarget")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("voc").addMarker("target").addMarker("sp")
                .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("ppb")
                .setTz(tz)
                .build();
        String zoneVOCTargetId = hayStack.addPoint(zoneVOCTarget);

        HashMap zoneVOCTargetIdPoint = hayStack.read("point and tuner and default and base and standalone and zone and voc and target and sp");
       if (zoneVOCTargetIdPoint != null && zoneVOCTargetIdPoint.get("id") != null) {
           ArrayList<HashMap> zoneVOCTargetIdArr = hayStack.readPoint(zoneVOCTargetIdPoint.get("id").toString());
           for (HashMap valMap : zoneVOCTargetIdArr) {
               if (valMap.get("val") != null) {
                   System.out.println(valMap);
                   hayStack.pointWrite(HRef.copy(zoneVOCTargetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
               }
           }
           hayStack.writeHisValById(zoneVOCTargetId, HSUtil.getPriorityVal(zoneVOCTargetId));
       }

        Point zoneVOCThreshold  = new Point.Builder()
                .setDisplayName(equipDis+"-StandaloneVOCThreshold")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("ppb")
                .setTz(tz)
                .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);

        HashMap zoneVOCThresholdIdPoint = hayStack.read("point and tuner and default and base and standalone and zone and voc and target and sp");
       if (zoneVOCThresholdIdPoint != null && zoneVOCThresholdIdPoint.get("id") != null) {
           ArrayList<HashMap> zoneVOCThresholdIdArr = hayStack.readPoint(zoneVOCThresholdIdPoint.get("id").toString());
           for (HashMap valMap : zoneVOCThresholdIdArr) {
               if (valMap.get("val") != null) {
                   System.out.println(valMap);
                   hayStack.pointWrite(HRef.copy(zoneVOCThresholdId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
               }
           }
           hayStack.writeHisValById(zoneVOCThresholdId, HSUtil.getPriorityVal(zoneVOCThresholdId));
       }
    }
    public void addEquipStandaloneTuners(String equipdis, String equipref, String roomRef, String floorRef){
        addEquipZoneTuners(equipdis,equipref, roomRef, floorRef);
        addEquipZoneStandaloneTuners(equipdis,equipref, roomRef, floorRef);
    }
    public void addTwoPipeFanEquipStandaloneTuners(String equipDis, String equipRef, String roomRef, String floorRef){
        Point sa2PfcHeatingThreshold = new Point.Builder()
                .setDisplayName(equipDis+"-2PipeFancoilHeatingThreshold")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("standalone").addMarker("heating").addMarker("threshold").addMarker("pipe2").addMarker("fcu").addMarker("sp")
                .setMinVal("80").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String sa2PfcHeatingThresholdId = hayStack.addPoint(sa2PfcHeatingThreshold);
        hayStack.writePoint(sa2PfcHeatingThresholdId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_THRESHOLD_2PFCU_DEFAULT, 0);
        hayStack.writeHisValById(sa2PfcHeatingThresholdId, TunerConstants.STANDALONE_HEATING_THRESHOLD_2PFCU_DEFAULT);

        Point sa2PfcCoolingThreshold = new Point.Builder()
                .setDisplayName(equipDis+"-2PipeFancoilCoolingThreshold")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("standalone").addMarker("cooling").addMarker("threshold").addMarker("pipe2").addMarker("fcu").addMarker("sp")
                .setMinVal("35").setMaxVal("70").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String sa2PfcCoolingThresholdId = hayStack.addPoint(sa2PfcCoolingThreshold);
        hayStack.writePoint(sa2PfcCoolingThresholdId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_THRESHOLD_2PFCU_DEFAULT, 0);
        hayStack.writeHisValById(sa2PfcCoolingThresholdId, TunerConstants.STANDALONE_COOLING_THRESHOLD_2PFCU_DEFAULT);
    }
    public void addDefaultStandaloneTuners()
    {
        Point saHeatingDeadBand = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneHeatingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("standalone").addMarker("heating").addMarker("deadband").addMarker("sp")
                .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String saHeatingDeadBandId = hayStack.addPoint(saHeatingDeadBand);
        hayStack.writePoint(saHeatingDeadBandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_DEADBAND_DEFAULT, 0);
        hayStack.writeHisValById(saHeatingDeadBandId, TunerConstants.STANDALONE_HEATING_DEADBAND_DEFAULT);

        Point saCoolingDeadBand = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneCoolingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("cooling").addMarker("deadband").addMarker("sp")
                .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String saCoolingDeadBandId = hayStack.addPoint(saCoolingDeadBand);
        hayStack.writePoint(saCoolingDeadBandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_DEADBAND_DEFAULT, 0);
        hayStack.writeHisValById(saCoolingDeadBandId, TunerConstants.STANDALONE_COOLING_DEADBAND_DEFAULT);
        Point saStage1Hysteresis = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneStage1Hysteresis")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("stage1").addMarker("hysteresis").addMarker("sp")
                .setMinVal("0.5").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz)
                .build();
        String saStage1HysteresisId = hayStack.addPoint(saStage1Hysteresis);
        hayStack.writePoint(saStage1HysteresisId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_STAGE1_HYSTERESIS_DEFAULT, 0);
        hayStack.writeHisValById(saStage1HysteresisId, TunerConstants.STANDALONE_STAGE1_HYSTERESIS_DEFAULT);

        Point saAirflowSampleWaitTime = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneAirflowSampleWaitTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("standalone").addMarker("base").addMarker("writable").addMarker("his")
                .addMarker("airflow").addMarker("sample").addMarker("wait").addMarker("time").addMarker("sp")
                .setMinVal("1").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setTz(tz)
                .build();
        String saAirflowSampleWaitTimeId = hayStack.addPoint(saAirflowSampleWaitTime);
        hayStack.writePoint(saAirflowSampleWaitTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_AIRFLOW_SAMPLE_WAIT_TIME, 0);
        hayStack.writeHisValById(saAirflowSampleWaitTimeId, TunerConstants.STANDALONE_AIRFLOW_SAMPLE_WAIT_TIME);

        Point saStage1CoolingLowerOffset = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneStage1CoolingLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("stage1").addMarker("cooling").addMarker("sp").addMarker("lower").addMarker("offset")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setTz(tz)
                .build();
        String saStage1CoolingLowerOffsetId = hayStack.addPoint(saStage1CoolingLowerOffset);
        hayStack.writePoint(saStage1CoolingLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_STAGE1_LOWER_OFFSET, 0);
        hayStack.writeHisValById(saStage1CoolingLowerOffsetId, TunerConstants.STANDALONE_COOLING_STAGE1_LOWER_OFFSET);

        Point saStage1CoolingUpperOffset = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneStage1CoolingUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("stage1").addMarker("cooling").addMarker("sp").addMarker("upper").addMarker("offset")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setTz(tz)
                .build();
        String saStage1CoolingUpperOffsetId = hayStack.addPoint(saStage1CoolingUpperOffset);
        hayStack.writePoint(saStage1CoolingUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_STAGE1_UPPER_OFFSET, 0);
        hayStack.writeHisValById(saStage1CoolingUpperOffsetId, TunerConstants.STANDALONE_COOLING_STAGE1_UPPER_OFFSET);

        Point saStage1HeatingLowerOffset = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneStage1HeatingLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("stage1").addMarker("heating").addMarker("sp").addMarker("lower").addMarker("offset")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setTz(tz)
                .build();
        String saStage1HeatingLowerOffsetId = hayStack.addPoint(saStage1HeatingLowerOffset);
        hayStack.writePoint(saStage1HeatingLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_STAGE1_LOWER_OFFSET, 0);
        hayStack.writeHisValById(saStage1HeatingLowerOffsetId, TunerConstants.STANDALONE_HEATING_STAGE1_LOWER_OFFSET);

        Point saStage1HeatingUpperOffset = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneStage1HeatingUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("stage1").addMarker("heating").addMarker("sp").addMarker("upper").addMarker("offset")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setTz(tz)
                .build();
        String saStage1HeatingUpperOffsetId = hayStack.addPoint(saStage1HeatingUpperOffset);
        hayStack.writePoint(saStage1HeatingUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_STAGE1_UPPER_OFFSET, 0);
        hayStack.writeHisValById(saStage1HeatingUpperOffsetId, TunerConstants.STANDALONE_HEATING_STAGE1_UPPER_OFFSET);

        Point standaloneCoolingPreconditioningRate  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"standaloneCoolingPreconditioningRate")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("cooling").addMarker("preconditioning").addMarker("rate").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String standaloneCoolingPreconditioningRateId = hayStack.addPoint(standaloneCoolingPreconditioningRate);
        hayStack.writePoint(standaloneCoolingPreconditioningRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_PRECONDITIONING_RATE, 0);
        hayStack.writeHisValById(standaloneCoolingPreconditioningRateId, TunerConstants.STANDALONE_COOLING_PRECONDITIONING_RATE);

        Point standaloneHeatingPreconditioningRate  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"standaloneHeatingPreconditioningRate")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("heating").addMarker("preconditioning").addMarker("rate").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String standaloneHeatingPreconditioningRateId = hayStack.addPoint(standaloneHeatingPreconditioningRate);
        hayStack.writePoint(standaloneHeatingPreconditioningRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_PRECONDITIONING_RATE, 0);
        hayStack.writeHisValById(standaloneHeatingPreconditioningRateId, TunerConstants.STANDALONE_HEATING_PRECONDITIONING_RATE);


        Point zoneCO2Target  = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneCO2Target")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String zoneCO2TargetId = hayStack.addPoint(zoneCO2Target);
        hayStack.writePoint(zoneCO2TargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_CO2_TARGET, 0);
        hayStack.writeHisValById(zoneCO2TargetId, TunerConstants.ZONE_CO2_TARGET);

        Point zoneCO2Threshold  = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneCO2Threshold")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("ppm")
                .setTz(tz)
                .build();
        String zoneCO2ThresholdId = hayStack.addPoint(zoneCO2Threshold);
        hayStack.writePoint(zoneCO2ThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_CO2_THRESHOLD, 0);
        hayStack.writeHisValById(zoneCO2ThresholdId, TunerConstants.ZONE_CO2_THRESHOLD);

        Point zoneVOCTarget  = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneVOCTarget")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("voc").addMarker("target").addMarker("sp")
                .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("ppb")
                .setTz(tz)
                .build();
        String zoneVOCTargetId = hayStack.addPoint(zoneVOCTarget);
        hayStack.writePoint(zoneVOCTargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_VOC_TARGET, 0);
        hayStack.writeHisValById(zoneVOCTargetId, TunerConstants.ZONE_VOC_TARGET);

        Point zoneVOCThreshold  = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneVOCThreshold")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his").addMarker("equipHis")
                .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("ppb")
                .setTz(tz)
                .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);
        hayStack.writePoint(zoneVOCThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_VOC_THRESHOLD, 0);
        hayStack.writeHisValById(zoneVOCThresholdId, TunerConstants.ZONE_VOC_THRESHOLD);
        Point standaloneCoolingAirflowTempLowerOffset = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneStage2CoolingLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("cooling").addMarker("airflow").addMarker("lower").addMarker("sp").addMarker("temp").addMarker("offset").addMarker("stage2")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String standaloneCoolingAirflowTempLowerOffsetId = hayStack.addPoint(standaloneCoolingAirflowTempLowerOffset);
        hayStack.writePoint(standaloneCoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_STAGE2_LOWER_OFFSET, 0);
        hayStack.writeHisValById(standaloneCoolingAirflowTempLowerOffsetId, TunerConstants.STANDALONE_COOLING_STAGE2_LOWER_OFFSET);

        Point standaloneCoolingAirflowTempUpperOffset = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneStage2CoolingUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("cooling").addMarker("airflow").addMarker("upper").addMarker("sp").addMarker("temp").addMarker("offset").addMarker("stage2")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String standaloneCoolingAirflowTempUpperOffsetId = hayStack.addPoint(standaloneCoolingAirflowTempUpperOffset);
        hayStack.writePoint(standaloneCoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_STAGE2_UPPER_OFFSET, 0);
        hayStack.writeHisValById(standaloneCoolingAirflowTempUpperOffsetId, TunerConstants.STANDALONE_COOLING_STAGE2_UPPER_OFFSET);

        Point standaloneHeatingAirflowTempUpperOffset = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneStage2HeatingUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("heating").addMarker("airflow").addMarker("upper").addMarker("sp").addMarker("temp").addMarker("offset").addMarker("stage2")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String standaloneHeatingAirflowTempUpperOffsetId = hayStack.addPoint(standaloneHeatingAirflowTempUpperOffset);
        hayStack.writePoint(standaloneHeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_STAGE2_UPPER_OFFSET, 0);
        hayStack.writeHisValById(standaloneHeatingAirflowTempUpperOffsetId, TunerConstants.STANDALONE_HEATING_STAGE2_UPPER_OFFSET);

        Point standaloneHeatingAirflowTempLowerOffset = new Point.Builder()
                .setDisplayName(equipDis+"-standaloneStage2HeatingLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("heating").addMarker("airflow").addMarker("lower").addMarker("sp").addMarker("temp").addMarker("offset").addMarker("stage2")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String standaloneHeatingAirflowTempLowerOffsetId = hayStack.addPoint(standaloneHeatingAirflowTempLowerOffset);
        hayStack.writePoint(standaloneHeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_STAGE1_LOWER_OFFSET, 0);
        hayStack.writeHisValById(standaloneHeatingAirflowTempLowerOffsetId, TunerConstants.STANDALONE_HEATING_STAGE2_LOWER_OFFSET);

        CCUHsApi.getInstance().syncEntityTree();
    }


}
