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
import a75f.io.logic.util.RxTask;

public class ZoneTuners {
    
    public static void addZoneTunersForEquip(CCUHsApi hayStack, String siteRef, String equipdis, String equipref,
                                             String roomRef, String floorRef, String tz ) {
        Log.d("CCU", "addZoneTunersForEquip for " + equipdis);
        List<HisItem> hisItems = new ArrayList<>();
        Point unoccupiedZoneSetback = new Point.Builder()
                                          .setDisplayName(equipdis+"-"+"unoccupiedZoneSetback")
                                          .setSiteRef(siteRef)
                                          .setEquipRef(equipref)
                                          .setRoomRef(roomRef)
                                          .setFloorRef(floorRef).setHisInterpolate("cov")
                                          .addMarker("tuner").addMarker("writable").addMarker("his")
                                          .addMarker("zone").addMarker("unoccupied").addMarker("setback").addMarker("sp")
                                          .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                          .setUnit("\u00B0F")
                                          .setTz(tz)
                                          .build();
        String unoccupiedZoneSetbackId = hayStack.addPoint(unoccupiedZoneSetback);
        BuildingTunerUtil.updateTunerLevels(unoccupiedZoneSetbackId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(unoccupiedZoneSetbackId));

        Point zoneDeadTime = new Point.Builder()
                                 .setDisplayName(equipdis+"-"+"zoneDeadTime")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .setRoomRef(roomRef)
                                 .setFloorRef(floorRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("writable").addMarker("his")
                                 .setMinVal("1").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                                 .addMarker("zone").addMarker("dead").addMarker("time").addMarker("sp")
                                 .setUnit("m")
                                 .setTz(tz)
                                 .build();
        String zoneDeadTimeId = hayStack.addPoint(zoneDeadTime);
        BuildingTunerUtil.updateTunerLevels(zoneDeadTimeId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(zoneDeadTimeId));

        Point autoAwayTime = new Point.Builder()
                                 .setDisplayName(equipdis+"-"+"autoAwayTime")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .setRoomRef(roomRef)
                                 .setFloorRef(floorRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("writable").addMarker("his")
                                 .setMinVal("40").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                                 .addMarker("zone").addMarker("auto").addMarker("away").addMarker("time").addMarker("sp")
                                 .setUnit("m")
                                 .setTz(tz)
                                 .build();
        String autoAwayTimeId = hayStack.addPoint(autoAwayTime);
        BuildingTunerUtil.updateTunerLevels(autoAwayTimeId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(autoAwayTimeId));

        Point forcedOccupiedTime = new Point.Builder()
                                       .setDisplayName(equipdis+"-"+"forcedOccupiedTime")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipref)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("writable").addMarker("his")
                                       .setMinVal("30").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                                       .addMarker("zone").addMarker("forced").addMarker("occupied").addMarker("time").addMarker("sp")
                                       .setUnit("m")
                                       .setTz(tz)
                                       .build();
        String forcedOccupiedTimeId = hayStack.addPoint(forcedOccupiedTime);
        BuildingTunerUtil.updateTunerLevels(forcedOccupiedTimeId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(forcedOccupiedTimeId));

        Point adrCoolingDeadband = new Point.Builder()
                                       .setDisplayName(equipdis+"-"+"adrCoolingDeadband")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipref)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("writable").addMarker("his")
                                       .addMarker("zone").addMarker("adr").addMarker("cooling").addMarker("deadband").addMarker("sp")
                                       .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                                       .setUnit("\u00B0F")
                                       .setTz(tz)
                                       .build();
        String adrCoolingDeadbandId = hayStack.addPoint(adrCoolingDeadband);
        BuildingTunerUtil.updateTunerLevels(adrCoolingDeadbandId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(adrCoolingDeadbandId));

        Point adrHeatingDeadband = new Point.Builder()
                                       .setDisplayName(equipdis+"-"+"adrHeatingDeadband")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipref)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("writable").addMarker("his")
                                       .addMarker("zone").addMarker("adr").addMarker("heating").addMarker("deadband").addMarker("sp")
                                       .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                                       .setUnit("\u00B0F")
                                       .setTz(tz)
                                       .build();
        String adrHeatingDeadbandId = hayStack.addPoint(adrHeatingDeadband);
        BuildingTunerUtil.updateTunerLevels(adrHeatingDeadbandId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(adrHeatingDeadbandId));

        Point snCoolingAirflowTemp = new Point.Builder()
                                         .setDisplayName(equipdis+"-"+"snCoolingAirflowTemp")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipref)
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("writable").addMarker("his")
                                         .addMarker("zone").addMarker("sn").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("sp")
                                         .setMinVal("35").setMaxVal("75").setIncrementVal("1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                                         .setUnit("\u00B0F")
                                         .setTz(tz)
                                         .build();
        String snCoolingAirflowTempId = hayStack.addPoint(snCoolingAirflowTemp);
        BuildingTunerUtil.updateTunerLevels(snCoolingAirflowTempId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(snCoolingAirflowTempId));

        Point snHeatingAirflowTemp = new Point.Builder()
                                         .setDisplayName(equipdis+"-"+"snHeatingAirflowTemp")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipref)
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("writable").addMarker("his")
                                         .addMarker("zone").addMarker("sn").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("sp")
                                         .setMinVal("65").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.TEMPERATURE_LIMIT)
                                         .setUnit("\u00B0F")
                                         .setTz(tz)
                                         .build();
        String snHeatingAirflowTempId = hayStack.addPoint(snHeatingAirflowTemp);
        BuildingTunerUtil.updateTunerLevels(snHeatingAirflowTempId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(snHeatingAirflowTempId));

        Point constantTempAlertTime = new Point.Builder()
                                          .setDisplayName(equipdis+"-"+"constantTempAlertTime")
                                          .setSiteRef(siteRef)
                                          .setEquipRef(equipref)
                                          .setRoomRef(roomRef)
                                          .setFloorRef(floorRef).setHisInterpolate("cov")
                                          .addMarker("tuner").addMarker("writable").addMarker("his")
                                          .addMarker("zone").addMarker("constant").addMarker("temp").addMarker("alert").addMarker("time").addMarker("sp")
                                          .setMinVal("0").setMaxVal("360").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                          .setUnit("m")
                                          .setTz(tz)
                                          .build();
        String constantTempAlertTimeId = hayStack.addPoint(constantTempAlertTime);
        BuildingTunerUtil.updateTunerLevels(constantTempAlertTimeId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(constantTempAlertTimeId));

        Point abnormalCurTempRiseTrigger = new Point.Builder()
                                               .setDisplayName(equipdis+"-"+"abnormalCurTempRiseTrigger")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .setRoomRef(roomRef)
                                               .setFloorRef(floorRef).setHisInterpolate("cov")
                                               .addMarker("tuner").addMarker("writable").addMarker("his")
                                               .addMarker("zone").addMarker("abnormal").addMarker("cur").addMarker("temp").addMarker("rise").addMarker("trigger").addMarker("sp")
                                               .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                               .setUnit("\u00B0F")
                                               .setTz(tz)
                                               .build();
        String abnormalCurTempRiseTriggerId = hayStack.addPoint(abnormalCurTempRiseTrigger);
        BuildingTunerUtil.updateTunerLevels(abnormalCurTempRiseTriggerId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(abnormalCurTempRiseTriggerId));
        hayStack.writeHisValueByIdWithoutCOV(hisItems);
    }
}
