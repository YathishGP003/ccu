package a75f.io.logic.tuners;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
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
        addDefaultVavTuners();
        addDefaultPlcTuners();
        
    }
    
    
    public void addDefaultVavTuners() {
        
        HashMap tuner = CCUHsApi.getInstance().read("point and tuner and default and vav");
        if (tuner != null && tuner.size() > 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM,"Default VAV Tuner points already exist");
            return;
        }
        CcuLog.d(L.TAG_CCU_SYSTEM,"Default VAV Tuner  does not exist. Create Now");
    
        Point zonePrioritySpread = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"zonePrioritySpread")
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
                                           .setDisplayName(equipDis+"-"+"zonePriorityMultiplier")
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
                                  .setDisplayName(equipDis+"-"+"coolingDeadband")
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
                                  .setDisplayName(equipDis+"-"+"coolingDeadbandMultiplier")
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
                                  .setDisplayName(equipDis+"-"+"heatingDeadband")
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
                                  .setDisplayName(equipDis+"-"+"heatingDeadbandMultiplier")
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
                                 .setDisplayName(equipDis+"-"+"proportionalKFactor ")
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
                                     .setDisplayName(equipDis+"-"+"integralKFactor ")
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
                                   .setDisplayName(equipDis+"-"+"temperatureProportionalRange ")
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
                                        .setDisplayName(equipDis+"-"+"temperatureIntegralTime ")
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
                                        .setDisplayName(equipDis+"-"+"valveActuationStartDamperPosDuringSysHeating")
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
                                          .setDisplayName(equipDis+"-"+"zoneCO2Target")
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
                                       .setDisplayName(equipDis+"-"+"zoneCO2Threshold")
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
                                       .setDisplayName(equipDis+"-"+"zoneVOCTarget")
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
                                          .setDisplayName(equipDis+"-"+"zoneVOCThreshold")
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
                                               .setDisplayName(equipDis+"-"+"targetCumulativeDamper")
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
                                               .setDisplayName(equipDis+"-"+"analogFanSpeedMultiplier")
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
                                                 .setDisplayName(equipDis+"-"+"humidityHysteresis")
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
                                           .setDisplayName(equipDis+"-"+"relayDeactivationHysteresis")
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
    
    public void addEquipVavTuners(String equipdis, String equipref, VavProfileConfiguration config) {
    
        Log.d("CCU","addEquipVavTuners for "+equipdis);
    
        Point zonePrioritySpread = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"zonePrioritySpread")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("zone").addMarker("priority").addMarker("spread").addMarker("sp")
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
    
        Point zonePriorityMultiplier = new Point.Builder()
                                           .setDisplayName(equipdis+"-"+"zonePriorityMultiplier")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipref)
                                           .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                           .addMarker("zone").addMarker("priority").addMarker("multiplier").addMarker("sp")
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
        
        Point coolingDb = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"coolingDeadband")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
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
    
        Point coolingDbMultiplier = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"coolingDeadbandMultiplier")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
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
        
        Point heatingDb = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"heatingDeadband")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
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
    
        Point heatingDbMultiplier = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"heatingDeadbandMultiplier")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
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
        
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipdis+"-"+"proportionalKFactor")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .addMarker("pgain").addMarker("sp")
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
        
        
        Point integralGain = new Point.Builder()
                                     .setDisplayName(equipdis+"-"+"integralKFactor")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .addMarker("igain").addMarker("sp")
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
        
        Point propSpread = new Point.Builder()
                                   .setDisplayName(equipdis+"-"+"temperatureProportionalRange")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipref)
                                   .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                   .addMarker("pspread").addMarker("sp")
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
        
        Point integralTimeout = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"temperatureIntegralTime")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("itimeout").addMarker("sp")
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
    
        Point valveStartDamper = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"valveActuationStartDamperPosDuringSysHeating")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("valve").addMarker("start").addMarker("damper").addMarker("sp")
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
    
    
        Point zoneCO2Target = new Point.Builder()
                                      .setDisplayName(equipdis+"-"+"zoneCO2Target")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                      .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
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
    
        Point zoneCO2Threshold = new Point.Builder()
                                         .setDisplayName(equipdis+"-"+"zoneCO2Threshold")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipref)
                                         .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
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
        
        Point zoneVOCTarget = new Point.Builder()
                                         .setDisplayName(equipdis+"-"+"zoneVOCTarget")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipref)
                                         .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("sp").addMarker("equipHis")
                                         .addMarker("zone").addMarker("voc").addMarker("target")
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
    
        Point zoneVOCThreshold = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"zoneVOCThreshold")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
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
        
    }
    
    public void addDefaultPlcTuners() {
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"proportionalKFactor ")
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
                                     .setDisplayName(equipDis+"-"+"integralKFactor ")
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
                                        .setDisplayName(equipDis+"-"+"pidIntegralTime ")
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
    
    public void addEquipPlcTuners(String equipdis, String equipref){
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipdis+"-"+"proportionalKFactor")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .addMarker("tuner").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .addMarker("pgain").addMarker("sp")
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
    
    
        Point integralGain = new Point.Builder()
                                     .setDisplayName(equipdis+"-"+"integralKFactor")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .addMarker("tuner").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .addMarker("igain").addMarker("sp")
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
    
        Point integralTimeout = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"pidIntegralTime")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("tuner").addMarker("pid").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .addMarker("itimeout").addMarker("sp")
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
    }
}
