package a75f.io.logic.tuners;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;

public class ZoneTuners {
    
    public static void addZoneTunersForEquip(String siteRef, String equipdis, String equipref, String roomRef,
                                       String floorRef, String tz ) {
        Log.d("CCU", "addZoneTunersForEquip for " + equipdis);
        CCUHsApi hayStack = CCUHsApi.getInstance();
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
        HashMap
            unoccupiedZoneSetbackPoint = hayStack.read("point and tuner and default and zone and unoccupied and setback");
        ArrayList<HashMap> unoccupiedZoneSetbackArr = hayStack.readPoint(unoccupiedZoneSetbackPoint.get("id").toString());
        for (HashMap valMap : unoccupiedZoneSetbackArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(unoccupiedZoneSetbackId), (int) Double.parseDouble(valMap.get("level").toString()),
                                    valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(unoccupiedZoneSetbackId, HSUtil.getPriorityVal(unoccupiedZoneSetbackId));
        
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
        HashMap zoneDeadTimePoint = hayStack.read("point and tuner and default and zone and dead and time");
        ArrayList<HashMap> zoneDeadTimeArr = hayStack.readPoint(zoneDeadTimePoint.get("id").toString());
        for (HashMap valMap : zoneDeadTimeArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(zoneDeadTimeId), (int) Double.parseDouble(valMap.get("level").toString()),
                                    valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(zoneDeadTimeId, HSUtil.getPriorityVal(zoneDeadTimeId));
        
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
        HashMap autoAwayTimePoint = hayStack.read("point and tuner and default and auto and away and time");
        ArrayList<HashMap> autoAwayTimeArr = hayStack.readPoint(autoAwayTimePoint.get("id").toString());
        for (HashMap valMap : autoAwayTimeArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(autoAwayTimeId), (int) Double.parseDouble(valMap.get("level").toString()),
                                    valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(autoAwayTimeId, HSUtil.getPriorityVal(autoAwayTimeId));
        
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
        HashMap forcedOccupiedTimePoint = hayStack.read("point and tuner and default and forced and occupied and time");
        ArrayList<HashMap> forcedOccupiedTimeArr = hayStack.readPoint(forcedOccupiedTimePoint.get("id").toString());
        for (HashMap valMap : forcedOccupiedTimeArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(forcedOccupiedTimeId), (int) Double.parseDouble(valMap.get("level").toString()),
                                    valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(forcedOccupiedTimeId, HSUtil.getPriorityVal(forcedOccupiedTimeId));
        
        Point adrCoolingDeadband = new Point.Builder()
                                       .setDisplayName(equipdis+"-"+"adrCoolingDeadband")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipref)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("writable").addMarker("his")
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
                hayStack.pointWrite(HRef.copy(adrCoolingDeadbandId), (int) Double.parseDouble(valMap.get("level").toString()),
                                    valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(adrCoolingDeadbandId, HSUtil.getPriorityVal(adrCoolingDeadbandId));
        
        Point adrHeatingDeadband = new Point.Builder()
                                       .setDisplayName(equipdis+"-"+"adrHeatingDeadband")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipref)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("writable").addMarker("his")
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
                hayStack.pointWrite(HRef.copy(adrHeatingDeadbandId), (int) Double.parseDouble(valMap.get("level").toString()),
                                    valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(adrHeatingDeadbandId, HSUtil.getPriorityVal(adrHeatingDeadbandId));
        
        Point snCoolingAirflowTemp = new Point.Builder()
                                         .setDisplayName(equipdis+"-"+"snCoolingAirflowTemp")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipref)
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("writable").addMarker("his")
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
                hayStack.pointWrite(HRef.copy(snCoolingAirflowTempId), (int) Double.parseDouble(valMap.get("level").toString()),
                                    valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(snCoolingAirflowTempId, HSUtil.getPriorityVal(snCoolingAirflowTempId));
        
        Point snHeatingAirflowTemp = new Point.Builder()
                                         .setDisplayName(equipdis+"-"+"snHeatingAirflowTemp")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipref)
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                         .addMarker("tuner").addMarker("writable").addMarker("his")
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
                hayStack.pointWrite(HRef.copy(snHeatingAirflowTempId), (int) Double.parseDouble(valMap.get("level").toString()),
                                    valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(snHeatingAirflowTempId, HSUtil.getPriorityVal(snHeatingAirflowTempId));
        
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
        HashMap constantTempAlertTimePoint = hayStack.read("point and tuner and default and constant and temp and alert and time");
        ArrayList<HashMap> constantTempAlertTimeArr = hayStack.readPoint(constantTempAlertTimePoint.get("id").toString());
        for (HashMap valMap : constantTempAlertTimeArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(constantTempAlertTimeId), (int) Double.parseDouble(valMap.get("level").toString()),
                                    valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(constantTempAlertTimeId, HSUtil.getPriorityVal(constantTempAlertTimeId));
        
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
        HashMap abnormalCurTempRiseTriggerPoint = hayStack.read("point and tuner and default and abnormal and cur and temp and rise and trigger");
        ArrayList<HashMap> abnormalCurTempRiseTriggerArr = hayStack.readPoint(abnormalCurTempRiseTriggerPoint.get("id").toString());
        for (HashMap valMap : abnormalCurTempRiseTriggerArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(abnormalCurTempRiseTriggerId), (int) Double.parseDouble(valMap.get("level").toString()),
                                    valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(abnormalCurTempRiseTriggerId, HSUtil.getPriorityVal(abnormalCurTempRiseTriggerId));
    }
}
