package a75f.io.logic.tuners;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;

class GenericTuners {
    
    public static void addDefaultGenericTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                             String tz) {
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
        hayStack.writePoint(heatingPreconditioingRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_PRECONDITION_RATE, 0);
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
        hayStack.writePoint(coolingPreconditioingRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_PRECONDITION_RATE, 0);
        hayStack.writeHisValById(coolingPreconditioingRateId, TunerConstants.SYSTEM_PRECONDITION_RATE);
    
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
        hayStack.writePoint(buildingLimitMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.BUILDING_LIMIT_MIN, 0);
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
        hayStack.writePoint(buildingLimitMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.BUILDING_LIMIT_MAX, 0);
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
        hayStack.writePoint(buildingToZoneDifferentialId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.BUILDING_TO_ZONE_DIFFERENTIAL, 0);
        hayStack.writeHisValById(buildingToZoneDifferentialId, TunerConstants.BUILDING_TO_ZONE_DIFFERENTIAL);
    
        Point zoneTemperatureDeadLeeway = new Point.Builder()
                                              .setDisplayName(equipDis+"-"+"zoneTemperatureDeadLeeway")
                                              .setSiteRef(siteRef)
                                              .setEquipRef(equipRef).setHisInterpolate("cov")
                                              .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
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
                                           .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
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
                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
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
                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
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
                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his").setMinVal("70").setMaxVal("77")
                                         .setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
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
                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
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
                                               .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
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
                                              .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                              .addMarker("dead").addMarker("percent").addMarker("influence").addMarker("sp")
                                              .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                              .setTz(tz)
                                              .build();
        String percentOfDeadZonesAllowedId = hayStack.addPoint(percentOfDeadZonesAllowed);
        hayStack.writePoint(percentOfDeadZonesAllowedId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.CM_TEMP_INFLU_PERCENTILE_ZONE_DEAD, 0);
        hayStack.writeHisValById(percentOfDeadZonesAllowedId, TunerConstants.CM_TEMP_INFLU_PERCENTILE_ZONE_DEAD);
    
        Point ccuAlarmVolumeLevel  = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"ccuAlarmVolumeLevel")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
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
                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
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
                                      .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                      .addMarker("heart").addMarker("beats").addMarker("to").addMarker("skip").addMarker("sp")
                                      .setMinVal("3").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                      .setTz(tz)
                                      .build();
        String heartBeatsToSkipId = hayStack.addPoint(heartBeatsToSkip);
        hayStack.writePoint(heartBeatsToSkipId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 5.0, 0);
        hayStack.writeHisValById(heartBeatsToSkipId, 5.0);
    
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
        hayStack.writePoint(clockUpdateIntervalId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 15.0, 0);
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
        hayStack.writePoint(perDegreeHumidityFactorId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", 10.0, 0);
        hayStack.writeHisValById(perDegreeHumidityFactorId, 10.0);
    
    }
}
