package a75f.io.logic.tuners;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Queries;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Units;

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

        Point useCelsius = new Point.Builder()
                .setDisplayName(equipDis+"-"+"displayUnit")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his").addMarker("displayUnit")
                .addMarker("system").addMarker("building").addMarker("enabled").addMarker("sp").setIncrementVal("1")
                .setEnums("false,true").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setMinVal("0")
                .setMaxVal("1")
                .setTz(tz)
                .build();
        String useCelsiusId = hayStack.addPoint(useCelsius);
        hayStack.writePointForCcuUser(useCelsiusId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.USE_CELSIUS_FLAG_DISABLED, 0);
        hayStack.writeHisValById(useCelsiusId, TunerConstants.USE_CELSIUS_FLAG_DISABLED);


    

    
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
        hayStack.writePointForCcuUser(zoneTemperatureDeadLeewayId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.ZONE_TEMP_DEAD_LEEWAY, 0);
        hayStack.writeHisValById(zoneTemperatureDeadLeewayId, TunerConstants.ZONE_TEMP_DEAD_LEEWAY);


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
        hayStack.writePointForCcuUser(humidityCompensationOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,0.0, 0);
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
        hayStack.writePointForCcuUser(percentOfDeadZonesAllowedId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.CM_TEMP_INFLU_PERCENTILE_ZONE_DEAD, 0);
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
        hayStack.writePointForCcuUser(ccuAlarmVolumeLevelId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,0.0, 0);
        hayStack.writeHisValById(ccuAlarmVolumeLevelId, 0.0);
    
        Point cmHeartBeatInterval  = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"cmHeartBeatInterval")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                         .addMarker("cm").addMarker("heartbeat").addMarker("interval").addMarker("sp")
                                         .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                         .setUnit("m")
                                         .setTz(tz)
                                         .build();
        String cmHeartBeatIntervalId = hayStack.addPoint(cmHeartBeatInterval);
        hayStack.writePointForCcuUser(cmHeartBeatIntervalId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,1.0, 0);
        hayStack.writeHisValById(cmHeartBeatIntervalId, 1.0);
    
        Point heartBeatsToSkip  = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"heartBeatsToSkip")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                      .addMarker("heartbeat").addMarker("sp")
                                      .setMinVal("3").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                      .setTz(tz)
                                      .build();
        String heartBeatsToSkipId = hayStack.addPoint(heartBeatsToSkip);
        hayStack.writePointForCcuUser(heartBeatsToSkipId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,5.0, 0);
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
        hayStack.writePointForCcuUser(clockUpdateIntervalId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,15.0, 0);
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
        hayStack.writePointForCcuUser(perDegreeHumidityFactorId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,10.0, 0);
        hayStack.writeHisValById(perDegreeHumidityFactorId, 10.0);

        Point autoAwaySetback   = new Point.Builder()
                .setDisplayName(equipDis+"-"+"autoAwaySetback")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("zone").addMarker("auto").addMarker("away").addMarker("setback").addMarker("sp")
                .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String autoAwaySetbackId = hayStack.addPoint(autoAwaySetback);
        hayStack.writePointForCcuUser(autoAwaySetbackId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,2.0, 0);
        hayStack.writeHisValById(autoAwaySetbackId, 2.0);
        createCcuNetworkWatchdogTimeoutTuner(hayStack);
    }

    public static void createCcuNetworkWatchdogTimeoutTuner(CCUHsApi hayStack) {

        ArrayList<HashMap<Object, Object>> watchdogTuner = hayStack.readAllEntities("point and tuner and network" +
                " and watchdog and timeout");
        if (!watchdogTuner.isEmpty()) {
            CcuLog.e(L.TAG_CCU_TUNER, "ccuNetworkWatchdogTimeout exists");
            return;
        }

        CcuLog.e(L.TAG_CCU_TUNER, "create ccuNetworkWatchdogTimeout ");
        //Create the tuner point on building tuner equip.
        HashMap<Object, Object> buildTuner = hayStack.readEntity(Queries.EQUIP_AND_TUNER);
        Equip tunerEquip = new Equip.Builder().setHashMap(buildTuner).build();

        Point ccuNetworkWatchdogTimeout  = new Point.Builder()
                .setDisplayName(tunerEquip.getDisplayName()+"-"+"ccuNetworkWatchdogTimeout")
                .setSiteRef(tunerEquip.getSiteRef())
                .setEquipRef(tunerEquip.getId()).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his")
                .addMarker("network").addMarker("watchdog").addMarker("timeout").addMarker("sp")
                .setMinVal("0").setMaxVal("1440").setIncrementVal("15").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("minute")
                .setTz(tunerEquip.getTz())
                .build();
        String ccuNetworkWatchdogTimeoutId = hayStack.addPoint(ccuNetworkWatchdogTimeout);
        hayStack.writePointForCcuUser(ccuNetworkWatchdogTimeoutId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,45.0, 0);
        hayStack.writeHisValById(ccuNetworkWatchdogTimeoutId, 45.0);
    }
}
