package a75f.io.logic.tuners;

import  android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class BPOSTuners {

    public static void addDefaultBPOSTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                          String tz) {
        HashMap tuner = CCUHsApi.getInstance().read("point and tuner and default and bpos");
        if (tuner != null && tuner.size() > 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "Default BPOS Tuner points already exist");
            return;
        }
        CcuLog.d(L.TAG_CCU_SYSTEM,"Default BPOS Tuner  does not exist. Create Now");
        Point zonePrioritySpread = new Point.Builder()
                .setDisplayName(equipDis+"-BPOS-"+"zonePrioritySpread")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("bpos").addMarker("writable")
                .addMarker("his")
                .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String zonePrioritySpreadId = hayStack.addPoint(zonePrioritySpread);
        hayStack.writePointForCcuUser(zonePrioritySpreadId, TunerConstants.DEFAULT_VAL_LEVEL,TunerConstants.ZONE_PRIORITY_SPREAD, 0);
        hayStack.writeHisValById(zonePrioritySpreadId, TunerConstants.ZONE_PRIORITY_SPREAD);

        Point zonePriorityMultiplier = new Point.Builder()
                .setDisplayName(equipDis+"-BPOS-"+"zonePriorityMultiplier")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("bpos").addMarker("writable")
                .addMarker("his")
                .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        hayStack.writePointForCcuUser(zonePriorityMultiplierId, TunerConstants.DEFAULT_VAL_LEVEL,TunerConstants.ZONE_PRIORITY_MULTIPLIER, 0);
        hayStack.writeHisValById(zonePriorityMultiplierId, TunerConstants.ZONE_PRIORITY_MULTIPLIER);

        Point coolingDb = new Point.Builder()
                .setDisplayName(equipDis+"-BPOS-"+"coolingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("bpos").addMarker("writable")
                .addMarker("his")
                .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        hayStack.writePointForCcuUser(coolingDbId, TunerConstants.DEFAULT_VAL_LEVEL,TunerConstants.VAV_COOLING_DB, 0);
        hayStack.writeHisValById(coolingDbId, TunerConstants.VAV_COOLING_DB);

        Point coolingDbMultiplier = new Point.Builder()
                .setDisplayName(equipDis+"-BPOS-"+"coolingDeadbandMultiplier")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("bpos").addMarker("writable")
                .addMarker("his")
                .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        hayStack.writePointForCcuUser(coolingDbMultiplierId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.VAV_COOLING_DB_MULTPLIER, 0);
        hayStack.writeHisValById(coolingDbMultiplierId, TunerConstants.VAV_COOLING_DB_MULTPLIER);

        Point heatingDb = new Point.Builder()
                .setDisplayName(equipDis+"-BPOS-"+"heatingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("bpos").addMarker("writable")
                .addMarker("his").addMarker("system")
                .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        hayStack.writePointForCcuUser(heatingDbId, TunerConstants.DEFAULT_VAL_LEVEL,TunerConstants.VAV_HEATING_DB, 0);
        hayStack.writeHisValById(heatingDbId, TunerConstants.VAV_HEATING_DB);

        Point heatingDbMultiplier = new Point.Builder()
                .setDisplayName(equipDis+"-BPOS-"+"heatingDeadbandMultiplier")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("bpos").addMarker("writable")
                .addMarker("his")
                .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        hayStack.writePointForCcuUser(heatingDbMultiplierId, TunerConstants.DEFAULT_VAL_LEVEL,TunerConstants.VAV_HEATING_DB_MULTIPLIER, 0);
        hayStack.writeHisValById(heatingDbMultiplierId, TunerConstants.VAV_HEATING_DB_MULTIPLIER);

        Point propGain = new Point.Builder()
                .setDisplayName(equipDis+"-BPOS-"+"proportionalKFactor ")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("bpos").addMarker("writable")
                .addMarker("his")
                .addMarker("pgain").addMarker("sp")
                .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePointForCcuUser(pgainId, TunerConstants.DEFAULT_VAL_LEVEL,TunerConstants.VAV_PROPORTIONAL_GAIN, 0);
        hayStack.writeHisValById(pgainId, TunerConstants.VAV_PROPORTIONAL_GAIN);

        Point integralGain = new Point.Builder()
                .setDisplayName(equipDis+"-BPOS-"+"integralKFactor ")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("bpos").addMarker("writable")
                .addMarker("his")
                .addMarker("igain").addMarker("sp")
                .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePointForCcuUser(igainId, TunerConstants.DEFAULT_VAL_LEVEL, TunerConstants.VAV_INTEGRAL_GAIN, 0);
        hayStack.writeHisValById(igainId, TunerConstants.VAV_INTEGRAL_GAIN);

        Point propSpread = new Point.Builder()
                .setDisplayName(equipDis+"-BPOS-"+"temperatureProportionalRange ")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("bpos").addMarker("writable")
                .addMarker("his")
                .addMarker("pspread").addMarker("sp")
                .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        hayStack.writePointForCcuUser(pSpreadId, TunerConstants.DEFAULT_VAL_LEVEL,TunerConstants.VAV_PROPORTIONAL_SPREAD, 0);
        hayStack.writeHisValById(pSpreadId, TunerConstants.VAV_PROPORTIONAL_SPREAD);

        Point integralTimeout = new Point.Builder()
                .setDisplayName(equipDis+"-BPOS-"+"temperatureIntegralTime ")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("bpos").addMarker("writable").addMarker("his")
                .addMarker("itimeout").addMarker("sp")
                .setUnit("m")
                .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePointForCcuUser(iTimeoutId, TunerConstants.DEFAULT_VAL_LEVEL,TunerConstants.DEFAULT_INTEGRAL_TIMEOUT, 0);
        hayStack.writeHisValById(iTimeoutId, TunerConstants.DEFAULT_INTEGRAL_TIMEOUT);


        Point forcedOccupiedTimer = new Point.Builder()
                .setDisplayName(equipDis+"-BPOS-"+"forcedOccupiedTimer")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("bpos").addMarker("writable")
                .addMarker("his")
                .addMarker("forced").addMarker("occupied").addMarker("time").addMarker("sp")
                .setUnit("m")
                .setMinVal("1").setMaxVal("120").setIncrementVal("1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String forcedOccupiedTimerid = hayStack.addPoint(forcedOccupiedTimer);
        hayStack.writePointForCcuUser(forcedOccupiedTimerid, TunerConstants.DEFAULT_VAL_LEVEL,120.0     , 0);
        hayStack.writeHisValById(forcedOccupiedTimerid,120.0 );


        Point autoAwayZoneTimer = new Point.Builder()
                .setDisplayName(equipDis+"-BPOS-"+"autoAwayZoneTimer")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("bpos").addMarker("writable")
                .addMarker("his")
                .addMarker("auto").addMarker("away").addMarker("time").addMarker("sp")
                .setUnit("m")
                .setMinVal("1").setMaxVal("30").setIncrementVal("1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String autoAwayZoneTimerid = hayStack.addPoint(autoAwayZoneTimer);
        hayStack.writePointForCcuUser(autoAwayZoneTimerid, TunerConstants.DEFAULT_VAL_LEVEL,30.0, 0);
        hayStack.writeHisValById(autoAwayZoneTimerid, TunerConstants.DEFAULT_INTEGRAL_TIMEOUT);


        Point autoAwayZoneSetbackTemp = new Point.Builder()
                .setDisplayName(equipDis+"-BPOS-"+"autoAwayZoneSetbackTemp")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("bpos").addMarker("writable")
                .addMarker("his")
                .addMarker("auto").addMarker("away").addMarker("temp").addMarker("sp").addMarker("setback")
                .setUnit("F")
                .setMinVal("1").setMaxVal("2").setIncrementVal("1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String autoAwayZoneSetbackTempid = hayStack.addPoint(autoAwayZoneSetbackTemp);
        hayStack.writePointForCcuUser(autoAwayZoneSetbackTempid, TunerConstants.DEFAULT_VAL_LEVEL,2.0, 0);
        hayStack.writeHisValById(autoAwayZoneSetbackTempid, 2.0);


    }

    public static void addEquipTuners(CCUHsApi hayStack, String siteRef, String equipdis, String equipref,
                                        String roomRef, String floorRef, String tz) {
        Log.d("CCU", "addEquipTuners for " + equipdis);

        ZoneTuners.addZoneTunersForEquip(hayStack, siteRef, equipdis, equipref, roomRef, floorRef, tz);

        Point forcedOccupiedTimer = new Point.Builder()
                .setDisplayName(equipdis+"-"+"forcedOccupiedTimer")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("bpos").addMarker("writable").addMarker("his")
                .addMarker("forced").addMarker("occupied").addMarker("time").addMarker("sp").addMarker("zone")
                .setMinVal("30").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String forcedOccupiedTimerid = hayStack.addPoint(forcedOccupiedTimer);
        BuildingTunerUtil.updateTunerLevels(forcedOccupiedTimerid, roomRef, hayStack);
        hayStack.writePointForCcuUser(forcedOccupiedTimerid, TunerConstants.DEFAULT_VAL_LEVEL,120.0, 0);
        hayStack.writeHisValById(forcedOccupiedTimerid, HSUtil.getPriorityVal(forcedOccupiedTimerid));


        Point autoAwayZoneTimer = new Point.Builder()
                .setDisplayName(equipdis+"-"+"autoAwayZoneTimer")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("bpos").addMarker("writable").addMarker("his")
                .addMarker("auto").addMarker("away").addMarker("time").addMarker("sp").addMarker("zone")
                .setMinVal("1").setMaxVal("30").setIncrementVal("1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String autoAwayZoneTimerid = hayStack.addPoint(autoAwayZoneTimer);
        BuildingTunerUtil.updateTunerLevels(autoAwayZoneTimerid, roomRef, hayStack);
        hayStack.writePointForCcuUser(autoAwayZoneTimerid, TunerConstants.DEFAULT_VAL_LEVEL,30.0, 0);
        hayStack.writeHisValById(autoAwayZoneTimerid, HSUtil.getPriorityVal(autoAwayZoneTimerid));


        Point autoAwayZoneSetbackTemp = new Point.Builder()
                .setDisplayName(equipdis+"-"+"autoAwayZoneSetbackTemp")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("bpos").addMarker("writable").addMarker("his").addMarker("sp")
                .addMarker("auto").addMarker("away").addMarker("temp")
                .addMarker("setback").addMarker("zone")
                .setMinVal("1").setMaxVal("2").setIncrementVal("1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setUnit("F")
                .setTz(tz)
                .build();
        String autoAwayZoneSetbackTempid = hayStack.addPoint(autoAwayZoneSetbackTemp);
        BuildingTunerUtil.updateTunerLevels(autoAwayZoneSetbackTempid, roomRef, hayStack);
        hayStack.writePointForCcuUser(autoAwayZoneSetbackTempid, TunerConstants.DEFAULT_VAL_LEVEL,2.0, 0);
        hayStack.writeHisValById(autoAwayZoneSetbackTempid, HSUtil.getPriorityVal(autoAwayZoneSetbackTempid));

        Point zonePrioritySpread = new Point.Builder()
                .setDisplayName(equipdis+"-"+"zonePrioritySpread")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("bpos").addMarker("writable").addMarker("his")
                .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String zonePrioritySpreadId = hayStack.addPoint(zonePrioritySpread);
        BuildingTunerUtil.updateTunerLevels(zonePrioritySpreadId, roomRef, hayStack);
        hayStack.writePointForCcuUser(zonePrioritySpreadId, TunerConstants.DEFAULT_VAL_LEVEL,2.0, 0);
        hayStack.writeHisValById(zonePrioritySpreadId, HSUtil.getPriorityVal(zonePrioritySpreadId));

        Point zonePriorityMultiplier = new Point.Builder()
                .setDisplayName(equipdis+"-"+"zonePriorityMultiplier")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("bpos").addMarker("writable").addMarker("his")
                .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                .setMinVal("0").setMaxVal("10").setIncrementVal("0.1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        BuildingTunerUtil.updateTunerLevels(zonePriorityMultiplierId, roomRef, hayStack);
        hayStack.writePointForCcuUser(zonePriorityMultiplierId, TunerConstants.DEFAULT_VAL_LEVEL,2.0, 0);
        hayStack.writeHisValById(zonePriorityMultiplierId, HSUtil.getPriorityVal(zonePriorityMultiplierId));

        Point coolingDb = new Point.Builder()
                .setDisplayName(equipdis+"-"+"coolingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("bpos").addMarker("writable").addMarker("his")
                .addMarker("zone").addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .setUnit("\u00B0F")
                .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        BuildingTunerUtil.updateTunerLevels(coolingDbId, roomRef, hayStack);
        hayStack.writePointForCcuUser(coolingDbId, TunerConstants.DEFAULT_VAL_LEVEL,2.0, 0);
        hayStack.writeHisValById(coolingDbId, HSUtil.getPriorityVal(coolingDbId));

        Point coolingDbMultiplier = new Point.Builder()
                .setDisplayName(equipdis+"-"+"coolingDeadbandMultiplier")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("tuner").addMarker("bpos").addMarker("writable").addMarker("his")
                .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        BuildingTunerUtil.updateTunerLevels(coolingDbMultiplierId, roomRef, hayStack);
        hayStack.writePointForCcuUser(coolingDbMultiplierId, TunerConstants.DEFAULT_VAL_LEVEL,2.0, 0);
        hayStack.writeHisValById(coolingDbMultiplierId, HSUtil.getPriorityVal(coolingDbMultiplierId));

        Point heatingDb = new Point.Builder()
                .setDisplayName(equipdis+"-"+"heatingDeadband")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("tuner").addMarker("bpos").addMarker("writable").addMarker("his")
                .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .setUnit("\u00B0F")
                .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        BuildingTunerUtil.updateTunerLevels(heatingDbId, roomRef, hayStack);
        hayStack.writePointForCcuUser(heatingDbId, TunerConstants.DEFAULT_VAL_LEVEL,2.0, 0);
        hayStack.writeHisValById(heatingDbId, HSUtil.getPriorityVal(heatingDbId));

        Point heatingDbMultiplier = new Point.Builder()
                .setDisplayName(equipdis+"-"+"heatingDeadbandMultiplier")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("tuner").addMarker("bpos").addMarker("writable").addMarker("his")
                .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        BuildingTunerUtil.updateTunerLevels(heatingDbMultiplierId, roomRef, hayStack);
        hayStack.writePointForCcuUser(heatingDbMultiplierId, TunerConstants.DEFAULT_VAL_LEVEL,0.5, 0);
        hayStack.writeHisValById(heatingDbMultiplierId, HSUtil.getPriorityVal(heatingDbMultiplierId));

        Point propGain = new Point.Builder()
                .setDisplayName(equipdis+"-"+"proportionalKFactor")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("tuner").addMarker("bpos").addMarker("writable").addMarker("his")
                .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .addMarker("pgain").addMarker("sp")
                .setTz(tz)
                .build();
        String pgainId = hayStack.addPoint(propGain);
        BuildingTunerUtil.updateTunerLevels(pgainId, roomRef, hayStack);
        hayStack.writePointForCcuUser(pgainId, TunerConstants.DEFAULT_VAL_LEVEL,0.5, 0);
        hayStack.writeHisValById(pgainId, HSUtil.getPriorityVal(pgainId));

        Point integralGain = new Point.Builder()
                .setDisplayName(equipdis+"-"+"integralKFactor")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("tuner").addMarker("bpos").addMarker("writable").addMarker("his")
                .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .addMarker("igain").addMarker("sp")
                .setTz(tz)
                .build();
        String igainId = hayStack.addPoint(integralGain);
        BuildingTunerUtil.updateTunerLevels(igainId, roomRef, hayStack);
        hayStack.writePointForCcuUser(igainId, TunerConstants.DEFAULT_VAL_LEVEL,0.5, 0);
        hayStack.writeHisValById(igainId, HSUtil.getPriorityVal(igainId));

        Point propSpread = new Point.Builder()
                .setDisplayName(equipdis+"-"+"temperatureProportionalRanges")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("tuner").addMarker("bpos").addMarker("writable").addMarker("his")
                .addMarker("pspread").addMarker("sp")
                .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setTz(tz)
                .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        BuildingTunerUtil.updateTunerLevels(pSpreadId, roomRef, hayStack);
        hayStack.writePointForCcuUser(pSpreadId, TunerConstants.DEFAULT_VAL_LEVEL,2.0, 0);
        hayStack.writeHisValById(pSpreadId, HSUtil.getPriorityVal(pSpreadId));

        Point integralTimeout = new Point.Builder()
                .setDisplayName(equipdis+"-"+"temperatureIntegralTimes")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("bpos").addMarker("writable").addMarker("his")
                .addMarker("itimeout").addMarker("sp").addMarker("zone")
                .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.BPOS_TUNER_GROUP)
                .setUnit("m")
                .setTz(tz)
                .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        BuildingTunerUtil.updateTunerLevels(iTimeoutId, roomRef, hayStack);
        hayStack.writePointForCcuUser(iTimeoutId, TunerConstants.DEFAULT_VAL_LEVEL,30.0, 0);
        hayStack.writeHisValById(iTimeoutId, HSUtil.getPriorityVal(iTimeoutId));




    }

}
