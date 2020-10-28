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

import static a75f.io.logic.tuners.TunerConstants.DEFAULT_MODE_CHANGEOVER_HYSTERESIS;
import static a75f.io.logic.tuners.TunerConstants.DEFAULT_STAGE_DOWN_TIMER_COUNTER;
import static a75f.io.logic.tuners.TunerConstants.DEFAULT_STAGE_UP_TIMER_COUNTER;

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
                          .addMarker("equip").addMarker("tuner").addMarker("his")
                          .setTz(siteMap.get("tz").toString())
                          .build();
        equipRef = hayStack.addEquip(tunerEquip);
        equipDis = siteDis+"-BuildingTuner";
        tz = siteMap.get("tz").toString();
        
        //addSettingTuners();
        addDefaultBuildingTuners();
        VavTuners.addDefaultVavTuners(siteRef, equipRef, equipDis, tz);
        PlcTuners.addDefaultPlcTuners(siteRef, equipRef, equipDis, tz);
        StandAloneTuners.addDefaultStandaloneTuners(siteRef, equipRef, equipDis, tz);
        DabTuners.addDefaultDabTuners(siteRef, equipRef, equipDis ,tz);
        addDefaultTiTuners();
        OAOTuners.addDefaultTuners(siteRef, equipRef, equipDis, tz);
        DualDuctTuners.addDefaultTuners(siteRef, equipRef, equipDis, tz);
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    /**
     * All the new tuners with are being added here.
     * This should be done neatly.
     */
    public void updateBuildingTuners() {
        addDefaultTiTuners();
        DualDuctTuners.addDefaultTuners(siteRef, equipRef, equipDis, tz);
        OAOTuners.updateNewTuners(siteRef,equipRef, equipDis,tz,false);
        updateDabBuildingTuners();
        updateVavBuildingTuners();
    }
    
    public void addDefaultBuildingTuners() {
        Point heatingPreconditioingRate = new Point.Builder()
                                           .setDisplayName(equipDis+"-"+"heatingPreconditioningRate")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                           .addMarker("system").addMarker("heating").addMarker("precon").addMarker("rate").addMarker("sp")
                                           .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                           .setTz(tz)
                                           .build();
        String heatingPreconditioingRateId = hayStack.addPoint(heatingPreconditioingRate);
        hayStack.writePointForCcuUser(heatingPreconditioingRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.SYSTEM_PRECONDITION_RATE, 0);
        hayStack.writeHisValById(heatingPreconditioingRateId, TunerConstants.SYSTEM_PRECONDITION_RATE);
    
        Point coolingPreconditioingRate = new Point.Builder()
                                                  .setDisplayName(equipDis+"-"+"coolingPreconditioningRate")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                                  .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                  .addMarker("system").addMarker("cooling").addMarker("precon").addMarker("rate").addMarker("sp")
                                                  .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                  .setTz(tz)
                                                  .build();
        String coolingPreconditioingRateId = hayStack.addPoint(coolingPreconditioingRate);
        hayStack.writePointForCcuUser(coolingPreconditioingRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.SYSTEM_PRECONDITION_RATE, 0);
        hayStack.writeHisValById(coolingPreconditioingRateId, TunerConstants.SYSTEM_PRECONDITION_RATE);
    
        Point userLimitSpread = new Point.Builder()
                                                  .setDisplayName(equipDis+"-"+"userLimitSpread")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                                  .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                  .addMarker("system").addMarker("user").addMarker("limit").addMarker("spread").addMarker("sp")
                                                  .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                                                  .setUnit("\u00B0F")
                                                  .setTz(tz)
                                                  .build();
        String userLimitSpreadId = hayStack.addPoint(userLimitSpread);
        hayStack.writePointForCcuUser(userLimitSpreadId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.USER_LIMIT_SPREAD, 0);
        hayStack.writeHisValById(userLimitSpreadId, TunerConstants.USER_LIMIT_SPREAD);
    
        Point buildingLimitMin = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"buildingLimitMin")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                        .addMarker("system").addMarker("building").addMarker("limit").addMarker("min").addMarker("sp")
                                        .setMinVal("50").setMaxVal("90").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                        .setUnit("\u00B0F")
                                        .setTz(tz)
                                        .build();
        String buildingLimitMinId = hayStack.addPoint(buildingLimitMin);
        hayStack.writePointForCcuUser(buildingLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.BUILDING_LIMIT_MIN, 0);
        hayStack.writeHisValById(buildingLimitMinId, TunerConstants.BUILDING_LIMIT_MIN);
    
        Point buildingLimitMax = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"buildingLimitMax")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                         .addMarker("system").addMarker("building").addMarker("limit").addMarker("max").addMarker("sp")
                                         .setMinVal("50").setMaxVal("90").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                         .setUnit("\u00B0F")
                                         .setTz(tz)
                                         .build();
        String buildingLimitMaxId = hayStack.addPoint(buildingLimitMax);
        hayStack.writePointForCcuUser(buildingLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.BUILDING_LIMIT_MAX, 0);
        hayStack.writeHisValById(buildingLimitMaxId, TunerConstants.BUILDING_LIMIT_MAX);
    
        Point buildingToZoneDifferential = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"buildingToZoneDifferential")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                         .addMarker("system").addMarker("building").addMarker("zone").addMarker("differential").addMarker("sp")
                                         .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                         .setTz(tz)
                                         .build();
        String buildingToZoneDifferentialId = hayStack.addPoint(buildingToZoneDifferential);
        hayStack.writePointForCcuUser(buildingToZoneDifferentialId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.BUILDING_TO_ZONE_DIFFERENTIAL, 0);
        hayStack.writeHisValById(buildingToZoneDifferentialId, TunerConstants.BUILDING_TO_ZONE_DIFFERENTIAL);
    
        Point zoneTemperatureDeadLeeway = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"zoneTemperatureDeadLeeway")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipRef).setHisInterpolate("cov")
                                                   .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                   .addMarker("temp").addMarker("dead").addMarker("leeway").addMarker("sp")
                                                   .setMinVal("0").setMaxVal("20").setIncrementVal("0.5").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                   .setUnit("\u00B0F")
                                                   .setTz(tz)
                                                   .build();
        String zoneTemperatureDeadLeewayId = hayStack.addPoint(zoneTemperatureDeadLeeway);
        hayStack.writePointForCcuUser(zoneTemperatureDeadLeewayId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_TEMP_DEAD_LEEWAY, 0);
        hayStack.writeHisValById(zoneTemperatureDeadLeewayId, TunerConstants.ZONE_TEMP_DEAD_LEEWAY);


        Point unoccupiedZoneSetback  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"unoccupiedZoneSetback")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("zone").addMarker("unoccupied").addMarker("setback").addMarker("sp")
                .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String unoccupiedZoneSetbackId = hayStack.addPoint(unoccupiedZoneSetback);
        hayStack.writePointForCcuUser(unoccupiedZoneSetbackId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_UNOCCUPIED_SETBACK, 0);
        hayStack.writeHisValById(unoccupiedZoneSetbackId, TunerConstants.ZONE_UNOCCUPIED_SETBACK);

        Point heatingUserLimitMin  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"heatingUserLimitMin")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("zone").addMarker("heating").addMarker("user").addMarker("limit").addMarker("min").addMarker("sp")
                .setMinVal("60").setMaxVal("70").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String heatingUserLimitMinId = hayStack.addPoint(heatingUserLimitMin);
        hayStack.writePointForCcuUser(heatingUserLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_HEATING_USERLIMIT_MIN, 0);
        hayStack.writeHisValById(heatingUserLimitMinId, TunerConstants.ZONE_HEATING_USERLIMIT_MIN);

        Point heatingUserLimitMax  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"heatingUserLimitMax")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("zone").addMarker("heating").addMarker("user").addMarker("limit").addMarker("max").addMarker("sp")
                .setMinVal("65").setMaxVal("75").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String heatingUserLimitMaxId = hayStack.addPoint(heatingUserLimitMax);
        hayStack.writePointForCcuUser(heatingUserLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_HEATING_USERLIMIT_MAX, 0);
        hayStack.writeHisValById(heatingUserLimitMaxId, TunerConstants.ZONE_HEATING_USERLIMIT_MAX);

        Point coolingUserLimitMin  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"coolingUserLimitMin")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his").setMinVal("70").setMaxVal("77").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .addMarker("zone").addMarker("cooling").addMarker("user").addMarker("limit").addMarker("min").addMarker("sp")
                .setMinVal("70").setMaxVal("77").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String coolingUserLimitMinId = hayStack.addPoint(coolingUserLimitMin);
        hayStack.writePointForCcuUser(coolingUserLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_COOLING_USERLIMIT_MIN, 0);
        hayStack.writeHisValById(coolingUserLimitMinId, TunerConstants.ZONE_COOLING_USERLIMIT_MIN);

        Point coolingUserLimitMax  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"coolingUserLimitMax")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("zone").addMarker("cooling").addMarker("user").addMarker("limit").addMarker("max").addMarker("sp")
                .setMinVal("72").setMaxVal("80").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String coolingUserLimitMaxId = hayStack.addPoint(coolingUserLimitMax);
        hayStack.writePointForCcuUser(coolingUserLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_COOLING_USERLIMIT_MAX, 0);
        hayStack.writeHisValById(coolingUserLimitMaxId, TunerConstants.ZONE_COOLING_USERLIMIT_MAX);
        Point humidityCompensationOffset = new Point.Builder()
                                                  .setDisplayName(equipDis+"-"+"humidityCompensationOffset")
                                                  .setSiteRef(siteRef)
                                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                                  .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                  .addMarker("system").addMarker("humidity").addMarker("compensation").addMarker("offset").addMarker("sp")
                                                  .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                  .setTz(tz)
                                                  .build();
        String humidityCompensationOffsetId = hayStack.addPoint(humidityCompensationOffset);
        hayStack.writePointForCcuUser(humidityCompensationOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 0.0, 0);
        hayStack.writeHisValById(humidityCompensationOffsetId, 0.0);

        Point percentOfDeadZonesAllowed = new Point.Builder()
                                                .setDisplayName(equipDis+"-"+"cmTempPercentDeadZonesAllowed")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipRef).setHisInterpolate("cov")
                                                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                .addMarker("dead").addMarker("percent").addMarker("influence").addMarker("sp")
                                                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                .setTz(tz)
                                                .build();
        String percentOfDeadZonesAllowedId = hayStack.addPoint(percentOfDeadZonesAllowed);
        hayStack.writePointForCcuUser(percentOfDeadZonesAllowedId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.CM_TEMP_INFLU_PERCENTILE_ZONE_DEAD, 0);
        hayStack.writeHisValById(percentOfDeadZonesAllowedId, TunerConstants.CM_TEMP_INFLU_PERCENTILE_ZONE_DEAD);


        Point zoneDeadTime  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"zoneDeadTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("zone").addMarker("dead").addMarker("time").addMarker("sp")
                .setMinVal("1").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String zoneDeadTimeId = hayStack.addPoint(zoneDeadTime);
        hayStack.writePointForCcuUser(zoneDeadTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 15.0, 0);
        hayStack.writeHisValById(zoneDeadTimeId, 15.0);

        Point autoAwayTime  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"autoAwayTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("auto").addMarker("away").addMarker("time").addMarker("sp")
                .setMinVal("40").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String autoAwayTimeId = hayStack.addPoint(autoAwayTime);
        hayStack.writePointForCcuUser(autoAwayTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 60.0, 0);
        hayStack.writeHisValById(autoAwayTimeId, 60.0);

        Point forcedOccupiedTime  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"forcedOccupiedTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("forced").addMarker("occupied").addMarker("time").addMarker("sp")
                .setMinVal("30").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String forcedOccupiedTimeId = hayStack.addPoint(forcedOccupiedTime);
        hayStack.writePointForCcuUser(forcedOccupiedTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 120.0, 0);
        hayStack.writeHisValById(forcedOccupiedTimeId, 120.0);

        Point cmResetCommand  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"cmResetCommandTimer")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("reset").addMarker("command").addMarker("time").addMarker("sp")
                .setMinVal("0").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String cmResetCommandId = hayStack.addPoint(cmResetCommand);
        hayStack.writePointForCcuUser(cmResetCommandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 90.0, 0);
        hayStack.writeHisValById(cmResetCommandId, 90.0);

        Point adrCoolingDeadband  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"adrCoolingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("adr").addMarker("cooling").addMarker("deadband").addMarker("sp")
                .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String adrCoolingDeadbandId = hayStack.addPoint(adrCoolingDeadband);
        hayStack.writePointForCcuUser(adrCoolingDeadbandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 3.0, 0);
        hayStack.writeHisValById(adrCoolingDeadbandId, 3.0);

        Point adrHeatingDeadband  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"adrHeatingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("adr").addMarker("heating").addMarker("deadband").addMarker("sp")
                .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String adrHeatingDeadbandId = hayStack.addPoint(adrHeatingDeadband);
        hayStack.writePointForCcuUser(adrHeatingDeadbandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 3.0, 0);
        hayStack.writeHisValById(adrHeatingDeadbandId, 3.0);

        Point snCoolingAirflowTemperature  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"snCoolingAirflowTemp")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("sn").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("sp")
                .setMinVal("35").setMaxVal("75").setIncrementVal("1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String snCoolingAirflowTemperatureId = hayStack.addPoint(snCoolingAirflowTemperature);
        hayStack.writePointForCcuUser(snCoolingAirflowTemperatureId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 60.0, 0);
        hayStack.writeHisValById(snCoolingAirflowTemperatureId, 60.0);

        Point snHeatingAirflowTemperature  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"snHeatingAirflowTemp")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("sn").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("sp")
                .setMinVal("65").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String snHeatingAirflowTemperatureId = hayStack.addPoint(snHeatingAirflowTemperature);
        hayStack.writePointForCcuUser(snHeatingAirflowTemperatureId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 105.0, 0);
        hayStack.writeHisValById(snHeatingAirflowTemperatureId, 105.0);

        Point buildingLimitAlertTimer  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"buildingLimitAlertTimer")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("building").addMarker("limit").addMarker("alert").addMarker("timer").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String buildingLimitAlertTimerId = hayStack.addPoint(buildingLimitAlertTimer);
        hayStack.writePointForCcuUser(buildingLimitAlertTimerId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 45.0, 0);
        hayStack.writeHisValById(buildingLimitAlertTimerId, 45.0);

        Point constantTempAlertTime  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"constantTempAlertTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("constant").addMarker("temp").addMarker("alert").addMarker("time").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("5").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String constantTempAlertTimeId = hayStack.addPoint(constantTempAlertTime);
        hayStack.writePointForCcuUser(constantTempAlertTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 40.0, 0);
        hayStack.writeHisValById(constantTempAlertTimeId, 40.0);

        Point abnormalCurTempRiseTrigger  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"abnormalCurTempRiseTrigger")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("abnormal").addMarker("cur").addMarker("temp").addMarker("rise").addMarker("trigger").addMarker("sp")
                .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String abnormalCurTempRiseTriggerId = hayStack.addPoint(abnormalCurTempRiseTrigger);
        hayStack.writePointForCcuUser(abnormalCurTempRiseTriggerId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 4.0, 0);
        hayStack.writeHisValById(abnormalCurTempRiseTriggerId, 4.0);

        Point airflowSampleWaitTime  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"airflowSampleWaitTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("airflow").addMarker("sample").addMarker("wait").addMarker("time").addMarker("sp")
                .setMinVal("1").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("m")
                .setTz(tz)
                .build();
        String airflowSampleWaitTimeId = hayStack.addPoint(airflowSampleWaitTime);
        hayStack.writePointForCcuUser(airflowSampleWaitTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 5.0, 0);
        hayStack.writeHisValById(airflowSampleWaitTimeId, 5.0);

        Point stage1CoolingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage1CoolingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage1").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage1CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage1CoolingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage1CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, -20.0, 0);
        hayStack.writeHisValById(stage1CoolingAirflowTempLowerOffsetId, -20.0);

        Point stage1CoolingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage1CoolingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage1").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage1CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage1CoolingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage1CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, -8.0, 0);
        hayStack.writeHisValById(stage1CoolingAirflowTempUpperOffsetId, -8.0);

        Point stage1HeatingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage1HeatingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage1").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage1HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage1HeatingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage1HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 40.0, 0);
        hayStack.writeHisValById(stage1HeatingAirflowTempUpperOffsetId, 40.0);

        Point stage1HeatingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage1HeatingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage1").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage1HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage1HeatingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage1HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 25.0, 0);
        hayStack.writeHisValById(stage1HeatingAirflowTempLowerOffsetId, 25.0);

        Point stage2CoolingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage2CoolingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage2").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage2CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage2CoolingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage2CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, -25.0, 0);
        hayStack.writeHisValById(stage2CoolingAirflowTempLowerOffsetId, -25.0);

        Point stage2CoolingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage2CoolingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage2").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage2CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage2CoolingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage2CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, -12.0, 0);
        hayStack.writeHisValById(stage2CoolingAirflowTempUpperOffsetId, -12.0);

        Point stage2HeatingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage2HeatingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage2").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage2HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage2HeatingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage2HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 50.0, 0);
        hayStack.writeHisValById(stage2HeatingAirflowTempUpperOffsetId, 50.0);

        Point stage2HeatingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage2HeatingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage2").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage2HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage2HeatingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage2HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 35.0, 0);
        hayStack.writeHisValById(stage2HeatingAirflowTempLowerOffsetId, 35.0);

        Point stage3CoolingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage3CoolingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage3").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage3CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage3CoolingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage3CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, -25.0, 0);
        hayStack.writeHisValById(stage3CoolingAirflowTempLowerOffsetId, -25.0);

        Point stage3CoolingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage3CoolingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage3").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage3CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage3CoolingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage3CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, -12.0, 0);
        hayStack.writeHisValById(stage3CoolingAirflowTempUpperOffsetId, -12.0);

        Point stage3HeatingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage3HeatingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage3").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage3HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage3HeatingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage3HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 50.0, 0);
        hayStack.writeHisValById(stage3HeatingAirflowTempUpperOffsetId, 50.0);

        Point stage3HeatingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage3HeatingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage3").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage3HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage3HeatingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage3HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 35.0, 0);
        hayStack.writeHisValById(stage3HeatingAirflowTempLowerOffsetId, 35.0);

        Point stage4CoolingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage4CoolingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage4").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage4CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage4CoolingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage4CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, -25.0, 0);
        hayStack.writeHisValById(stage1CoolingAirflowTempLowerOffsetId, -25.0);

        Point stage4CoolingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage4CoolingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage4").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage4CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage4CoolingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage4CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, -12.0, 0);
        hayStack.writeHisValById(stage4CoolingAirflowTempUpperOffsetId, -12.0);

        Point stage4HeatingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage4HeatingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage4").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage4HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage4HeatingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage4HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 50.0, 0);
        hayStack.writeHisValById(stage4HeatingAirflowTempUpperOffsetId, 50.0);

        Point stage4HeatingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage4HeatingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage4").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage4HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage4HeatingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage4HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 35.0, 0);
        hayStack.writeHisValById(stage4HeatingAirflowTempLowerOffsetId, 35.0);

        Point stage5CoolingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage5CoolingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage5").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage5CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage5CoolingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage5CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, -25.0, 0);
        hayStack.writeHisValById(stage5CoolingAirflowTempLowerOffsetId, -25.0);

        Point stage5CoolingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage5CoolingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage5").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage5CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage5CoolingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage5CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, -12.0, 0);
        hayStack.writeHisValById(stage5CoolingAirflowTempUpperOffsetId, -12.0);

        Point stage5HeatingAirflowTempUpperOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage5HeatingAirflowTempUpperOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage5").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage5HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage5HeatingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage5HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 50.0, 0);
        hayStack.writeHisValById(stage5HeatingAirflowTempUpperOffsetId, 50.0);

        Point stage5HeatingAirflowTempLowerOffset  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"stage5HeatingAirflowTempLowerOffset")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("stage5").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String stage5HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage5HeatingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage5HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 35.0, 0);
        hayStack.writeHisValById(stage5HeatingAirflowTempLowerOffsetId, 35.0);

        Point clockUpdateInterval  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"clockUpdateInterval")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("clock").addMarker("update").addMarker("interval").addMarker("sp")
                .setMinVal("1").setMaxVal("120").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String clockUpdateIntervalId = hayStack.addPoint(clockUpdateInterval);
        hayStack.writePointForCcuUser(clockUpdateIntervalId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 15.0, 0);
        hayStack.writeHisValById(clockUpdateIntervalId, 15.0);

        Point perDegreeHumidityFactor  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"perDegreeHumidityFactor")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("per").addMarker("degree").addMarker("humidity").addMarker("factor").addMarker("sp")
                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("%")
                .setTz(tz)
                .build();
        String perDegreeHumidityFactorId = hayStack.addPoint(perDegreeHumidityFactor);
        hayStack.writePointForCcuUser(perDegreeHumidityFactorId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 10.0, 0);
        hayStack.writeHisValById(perDegreeHumidityFactorId, 10.0);

        Point ccuAlarmVolumeLevel  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"ccuAlarmVolumeLevel")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("alarm").addMarker("volume").addMarker("level").addMarker("sp")
                .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz)
                .build();
        String ccuAlarmVolumeLevelId = hayStack.addPoint(ccuAlarmVolumeLevel);
        hayStack.writePointForCcuUser(ccuAlarmVolumeLevelId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 0.0, 0);
        hayStack.writeHisValById(ccuAlarmVolumeLevelId, 0.0);

        Point cmHeartBeatInterval  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"cmHeartBeatInterval")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("cm").addMarker("heart").addMarker("beat").addMarker("interval").addMarker("sp")
                .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String cmHeartBeatIntervalId = hayStack.addPoint(cmHeartBeatInterval);
        hayStack.writePointForCcuUser(cmHeartBeatIntervalId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 1.0, 0);
        hayStack.writeHisValById(cmHeartBeatIntervalId, 1.0);

        Point heartBeatsToSkip  = new Point.Builder()
                .setDisplayName(equipDis+"-"+"heartBeatsToSkip")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("heart").addMarker("beats").addMarker("to").addMarker("skip").addMarker("sp")
                .setMinVal("3").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setTz(tz)
                .build();
        String heartBeatsToSkipId = hayStack.addPoint(heartBeatsToSkip);
        hayStack.writePointForCcuUser(heartBeatsToSkipId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 5.0, 0);
        hayStack.writeHisValById(heartBeatsToSkipId, 5.0);

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
        hayStack.writePointForCcuUser(rebalanceHoldTimeId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, 20.0, 0);
        hayStack.writeHisValById(rebalanceHoldTimeId, 20.0);
        CCUHsApi.getInstance().syncEntityTree();
    }

    public void addEquipTiTuners(String equipdis, String equipref, String roomRef, String floorRef) {
        Log.d("CCU","addEquipTiTuners for "+equipdis);

        ZoneTuners.addZoneTunersForEquip(siteRef, equipdis, equipref, roomRef, floorRef, tz);

        Point zonePrioritySpread = new Point.Builder()
                .setDisplayName(equipdis+"-"+"zonePrioritySpread")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his")
                .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                .setTz(tz)
                .build();
        String zonePrioritySpreadId = hayStack.addPoint(zonePrioritySpread);
        HashMap zonePrioritySpreadPoint = hayStack.read("point and tuner and default and ti and zone and priority and spread");
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
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his")
                .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                .setTz(tz)
                .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        HashMap zonePriorityMultiplierPoint = hayStack.read("point and tuner and default and ti and zone and priority and multiplier");
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
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his")
                .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                .setTz(tz)
                .setUnit("\u00B0F")
                .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        HashMap defCdbPoint = hayStack.read("point and tuner and default and ti and cooling and deadband and base");
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
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his")
                .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                .setTz(tz)
                .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        HashMap coolingDbMultiplierPoint = hayStack.read("point and tuner and default and ti and cooling and deadband and multiplier");
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
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his")
                .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                .setTz(tz)
                .setUnit("\u00B0F")
                .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        HashMap defHdbPoint = hayStack.read("point and tuner and default and ti and heating and deadband and base");
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
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his")
                .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                .setTz(tz)
                .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        HashMap heatingDbMultiplierPoint = hayStack.read("point and tuner and default and ti and heating and deadband and multiplier");
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
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his")
                .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                .addMarker("pgain").addMarker("sp")
                .setTz(tz)
                .build();
        String pgainId = hayStack.addPoint(propGain);
        HashMap defPgainPoint = hayStack.read("point and tuner and default and ti and pgain");
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
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his")
                .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                .addMarker("igain").addMarker("sp")
                .setTz(tz)
                .build();
        String igainId = hayStack.addPoint(integralGain);
        HashMap defIgainPoint = hayStack.read("point and tuner and default and ti and igain");
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
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his")
                .addMarker("pspread").addMarker("sp")
                .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                .setTz(tz)
                .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        HashMap defPSpreadPoint = hayStack.read("point and tuner and default and ti and pspread");
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
                .addMarker("tuner").addMarker("ti").addMarker("writable").addMarker("his")
                .addMarker("itimeout").addMarker("sp")
                .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        HashMap defITPoint = hayStack.read("point and tuner and default and ti and itimeout");
        ArrayList<HashMap> iTDefPointArr = hayStack.readPoint(defITPoint.get("id").toString());
        for (HashMap valMap : iTDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(iTimeoutId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(iTimeoutId, HSUtil.getPriorityVal(iTimeoutId));


    }

    public void addEquipStandaloneTuners(String equipdis, String equipref, String roomRef, String floorRef){
        ZoneTuners.addZoneTunersForEquip(siteRef, equipdis, equipref, roomRef, floorRef, tz);
        StandAloneTuners.addEquipZoneStandaloneTuners(siteRef, equipdis,equipref, roomRef, floorRef, tz);
    }

    private void updateDabBuildingTuners() {
        HashMap<Object, Object> modeChangeoverHysteresisPoint = CCUHsApi.getInstance()
                                                                        .readEntity("tuner and default and mode and " +
                                                                                    "changeover and hysteresis");
        if (modeChangeoverHysteresisPoint.isEmpty()) {
            Point modeChangeoverHysteresis = new Point.Builder().setDisplayName(equipDis + "-DAB-" +
                                                                                "modeChangeoverHysteresis")
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
        }

        HashMap<Object, Object> stageUpTimerCounterPoint = CCUHsApi.getInstance()
                                                                        .readEntity("tuner and default and dab and " +
                                                                                    "stageUp and timer and counter");
        if (stageUpTimerCounterPoint.isEmpty()) {
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
        }

        HashMap<Object, Object> stageDownTimerCounterPoint = CCUHsApi.getInstance()
                                                                   .readEntity("tuner and dab and default and " +
                                                                               "stageDown and timer and counter");
        if (stageDownTimerCounterPoint.isEmpty()) {
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
    }

    private void updateVavBuildingTuners() {

        HashMap<Object, Object> fanControlOnFixedTimeDelayPoint = CCUHsApi.getInstance()
                                                                          .read("tuner and default and fan and " +
                                                                                "control and time and delay");

        if (fanControlOnFixedTimeDelayPoint.isEmpty()) {
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
            CCUHsApi.getInstance().writeDefaultValById(fanControlOnFixedTimeDelayId, 1.0);
            CCUHsApi.getInstance().writeHisValById(fanControlOnFixedTimeDelayId, 1.0);
        }

        HashMap<Object, Object> stageUpTimerCounterPoint = CCUHsApi.getInstance()
                                                                   .readEntity("tuner and vav and default and stageUp" +
                                                                               " and timer and counter");
        if (stageUpTimerCounterPoint.isEmpty()) {
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
        }

        HashMap<Object, Object> stageDownTimerCounterPoint = CCUHsApi.getInstance()
                                                                 .readEntity("tuner and vav and default and stageDown" +
                                                                             " and timer and counter");
        if (stageDownTimerCounterPoint.isEmpty()) {
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
    }
}
