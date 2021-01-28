package a75f.io.logic.tuners;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class TITuners {
    
    public static void addDefaultTiTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                          String tz) {
        HashMap tuner = CCUHsApi.getInstance().read("point and tuner and default and ti");
        if (tuner != null && tuner.size() > 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "Default TI Tuner points already exist");
            return;
        }
        CcuLog.d(L.TAG_CCU_SYSTEM,"Default TI Tuner  does not exist. Create Now");
        Point zonePrioritySpread = new Point.Builder()
                                       .setDisplayName(equipDis+"-TI-"+"zonePrioritySpread")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("default").addMarker("ti").addMarker("writable").addMarker("his")
                                       .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
                                       .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                                       .setTz(tz)
                                       .build();
        String zonePrioritySpreadId = hayStack.addPoint(zonePrioritySpread);
        hayStack.writePoint(zonePrioritySpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_PRIORITY_SPREAD, 0);
        hayStack.writeHisValById(zonePrioritySpreadId, TunerConstants.ZONE_PRIORITY_SPREAD);
        
        Point zonePriorityMultiplier = new Point.Builder()
                                           .setDisplayName(equipDis+"-TI-"+"zonePriorityMultiplier")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("ti").addMarker("writable").addMarker("his")
                                           .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
                                           .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                                           .setTz(tz)
                                           .build();
        String zonePriorityMultiplierId = hayStack.addPoint(zonePriorityMultiplier);
        hayStack.writePoint(zonePriorityMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_PRIORITY_MULTIPLIER, 0);
        hayStack.writeHisValById(zonePriorityMultiplierId, TunerConstants.ZONE_PRIORITY_MULTIPLIER);
        
        Point coolingDb = new Point.Builder()
                              .setDisplayName(equipDis+"-TI-"+"coolingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("default").addMarker("ti").addMarker("writable").addMarker("his")
                              .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                              .setUnit("\u00B0F")
                              .setTz(tz)
                              .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        hayStack.writePoint(coolingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_COOLING_DB, 0);
        hayStack.writeHisValById(coolingDbId, TunerConstants.VAV_COOLING_DB);
        
        Point coolingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipDis+"-TI-"+"coolingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("ti").addMarker("writable").addMarker("his")
                                        .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        hayStack.writePoint(coolingDbMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_COOLING_DB_MULTPLIER, 0);
        hayStack.writeHisValById(coolingDbMultiplierId, TunerConstants.VAV_COOLING_DB_MULTPLIER);
        
        Point heatingDb = new Point.Builder()
                              .setDisplayName(equipDis+"-TI-"+"heatingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("default").addMarker("ti").addMarker("writable").addMarker("his")
                              .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                              .setUnit("\u00B0F")
                              .setTz(tz)
                              .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        hayStack.writePoint(heatingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_HEATING_DB, 0);
        hayStack.writeHisValById(heatingDbId, TunerConstants.VAV_HEATING_DB);
        
        Point heatingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipDis+"-TI-"+"heatingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("ti").addMarker("writable").addMarker("his")
                                        .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        hayStack.writePoint(heatingDbMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_HEATING_DB_MULTIPLIER, 0);
        hayStack.writeHisValById(heatingDbMultiplierId, TunerConstants.VAV_HEATING_DB_MULTIPLIER);
        
        Point propGain = new Point.Builder()
                             .setDisplayName(equipDis+"-TI-"+"proportionalKFactor ")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("default").addMarker("ti").addMarker("writable").addMarker("his")
                             .addMarker("pgain").addMarker("sp")
                             .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                             .setTz(tz)
                             .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePoint(pgainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_GAIN, 0);
        hayStack.writeHisValById(pgainId, TunerConstants.VAV_PROPORTIONAL_GAIN);
        
        Point integralGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-TI-"+"integralKFactor ")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("default").addMarker("ti").addMarker("writable").addMarker("his")
                                 .addMarker("igain").addMarker("sp")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                                 .setTz(tz)
                                 .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePoint(igainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_GAIN, 0);
        hayStack.writeHisValById(igainId, TunerConstants.VAV_INTEGRAL_GAIN);
        
        Point propSpread = new Point.Builder()
                               .setDisplayName(equipDis+"-TI-"+"temperatureProportionalRange ")
                               .setSiteRef(siteRef)
                               .setEquipRef(equipRef).setHisInterpolate("cov")
                               .addMarker("tuner").addMarker("default").addMarker("ti").addMarker("writable").addMarker("his")
                               .addMarker("pspread").addMarker("sp")
                               .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                               .setTz(tz)
                               .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        hayStack.writePoint(pSpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_SPREAD, 0);
        hayStack.writeHisValById(pSpreadId, TunerConstants.VAV_PROPORTIONAL_SPREAD);
        
        Point integralTimeout = new Point.Builder()
                                    .setDisplayName(equipDis+"-TI-"+"temperatureIntegralTime ")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef).setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker("default").addMarker("ti").addMarker("writable").addMarker("his")
                                    .addMarker("itimeout").addMarker("sp")
                                    .setUnit("m")
                                    .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
                                    .setTz(tz)
                                    .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePoint(iTimeoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_TIMEOUT, 0);
        hayStack.writeHisValById(iTimeoutId, TunerConstants.VAV_INTEGRAL_TIMEOUT);
        
    }
    
    public static void addEquipTiTuners(CCUHsApi hayStack, String siteRef, String equipdis, String equipref,
                                        String roomRef, String floorRef, String tz) {
        Log.d("CCU", "addEquipTiTuners for " + equipdis);
        //addEquipZoneTuners(equipdis, equipref, roomRef, floorRef);
        ZoneTuners.addZoneTunersForEquip(hayStack, siteRef, equipdis, equipref, roomRef, floorRef, tz);
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
                                           .setMinVal("0").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
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
                              .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
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
                              .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.TI_TUNER_GROUP)
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
}
