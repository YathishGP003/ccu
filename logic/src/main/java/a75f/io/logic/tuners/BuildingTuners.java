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
            Log.d("CCU","Building Tuner equip already present");
            return;
        }
        System.out.println("Build Tuner Equip does not exist. Create Now");
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
        //addDefaultSystemTuners();
        
    }
    
    
    public void addDefaultVavTuners() {
        
        HashMap tuner = CCUHsApi.getInstance().read("point and tuner and default and vav");
        if (tuner != null && tuner.size() > 0) {
            Log.d("CCU","Default VAV Tuner points already exist");
            return;
        }
        System.out.println("Default VAV Tuner  does not exist. Create Now");
        
        Point coolingDb = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"VAVCoolingDB")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("cooling").addMarker("deadband")
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        hayStack.writePoint(coolingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_COOLING_DB, 0);
        hayStack.writeHisValById(coolingDbId, TunerConstants.VAV_COOLING_DB);
        Point heatingDb = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"VAVHeatingDB")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("heating").addMarker("deadband")
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        hayStack.writePoint(heatingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_HEATING_DB, 0);
        hayStack.writeHisValById(heatingDbId, TunerConstants.VAV_HEATING_DB);
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"VAVProportionalGain")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("pgain")
                                 .setTz(tz)
                                 .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePoint(pgainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_GAIN, 0);
        hayStack.writeHisValById(pgainId, TunerConstants.VAV_PROPORTIONAL_GAIN);
        
        Point integralGain = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"VAVIntegralGain")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef)
                                     .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("igain")
                                     .setTz(tz)
                                     .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePoint(igainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_GAIN, 0);
        hayStack.writeHisValById(igainId, TunerConstants.VAV_INTEGRAL_GAIN);
        
        Point propSpread = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"VAVProportionalSpread")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("pspread")
                                   .setTz(tz)
                                   .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        hayStack.writePoint(pSpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_SPREAD, 0);
        hayStack.writeHisValById(pSpreadId, TunerConstants.VAV_PROPORTIONAL_SPREAD);
        
        Point integralTimeout = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"VAVIntegralTimeout")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef)
                                        .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his").addMarker("itimeout")
                                        .setTz(tz)
                                        .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePoint(iTimeoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_TIMEOUT, 0);
        hayStack.writeHisValById(iTimeoutId, TunerConstants.VAV_INTEGRAL_TIMEOUT);
    
        Point valveStartDamper  = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"vavleActuationStartDamperPosDuringSysHeating")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef)
                                        .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his")
                                        .addMarker("valve").addMarker("start").addMarker("damper")
                                        .setTz(tz)
                                        .build();
        String valveStartDamperId = hayStack.addPoint(valveStartDamper);
        hayStack.writePoint(valveStartDamperId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VALVE_START_DAMPER, 0);
        hayStack.writeHisValById(valveStartDamperId, TunerConstants.VALVE_START_DAMPER);
    
        addDefaultVavSystemTuners();
        
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    public void addDefaultVavSystemTuners()
    {
        Point targetCumulativeDamper = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"targetCumulativeDamper")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef)
                                               .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("his")
                                               .addMarker("target").addMarker("cumulative").addMarker("damper")
                                               .setTz(tz)
                                               .build();
        String targetCumulativeDamperId = hayStack.addPoint(targetCumulativeDamper);
        hayStack.writePoint(targetCumulativeDamperId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.TARGET_CUMULATIVE_DAMPER, 0);
        hayStack.writeHisValById(targetCumulativeDamperId, TunerConstants.VAV_INTEGRAL_TIMEOUT);
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    public void addEquipVavTuners(String equipdis, String equipref, VavProfileConfiguration config) {
    
        Log.d("CCU","addEquipVavTuners for "+equipdis);
        Point coolingDb = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"VAVCoolingDB")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("cooling").addMarker("deadband")
                                  .setUnit("\u00B0F")
                                  .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        HashMap defCdbPoint = hayStack.read("point and tuner and default and vav and cooling and deadband");
        ArrayList<HashMap> cdbDefPointArr = hayStack.readPoint(defCdbPoint.get("id").toString());
        for (HashMap valMap : cdbDefPointArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.getHSClient().pointWrite(HRef.copy(coolingDbId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        
        Point heatingDb = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"VAVHeatingDB")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("heating").addMarker("deadband")
                                  .setUnit("\u00B0F")
                                  .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        HashMap defHdbPoint = hayStack.read("point and tuner and default and vav and heating and deadband");
        ArrayList<HashMap> hdbDefPointArr = hayStack.readPoint(defHdbPoint.get("id").toString());
        for (HashMap valMap : hdbDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.getHSClient().pointWrite(HRef.copy(heatingDbId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipdis+"-"+"VAVProportionalGain")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("pgain")
                                 .build();
        String pgainId = hayStack.addPoint(propGain);
        HashMap defPgainPoint = hayStack.read("point and tuner and default and vav and pgain");
        ArrayList<HashMap> pgainDefPointArr = hayStack.readPoint(defPgainPoint.get("id").toString());
        for (HashMap valMap : pgainDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.getHSClient().pointWrite(HRef.copy(pgainId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        
        
        Point integralGain = new Point.Builder()
                                     .setDisplayName(equipdis+"-"+"VAVIntegralGain")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("igain")
                                     .build();
        String igainId = hayStack.addPoint(integralGain);
        HashMap defIgainPoint = hayStack.read("point and tuner and default and vav and igain");
        ArrayList<HashMap> igainDefPointArr = hayStack.readPoint(defIgainPoint.get("id").toString());
        for (HashMap valMap : igainDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.getHSClient().pointWrite(HRef.copy(igainId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        
        Point propSpread = new Point.Builder()
                                   .setDisplayName(equipdis+"-"+"VAVProportionalSpread")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipref)
                                   .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("pspread")
                                   .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        HashMap defPSpreadPoint = hayStack.read("point and tuner and default and vav and pspread");
        ArrayList<HashMap> pspreadDefPointArr = hayStack.readPoint(defPSpreadPoint.get("id").toString());
        for (HashMap valMap : pspreadDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.getHSClient().pointWrite(HRef.copy(pSpreadId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        
        Point integralTimeout = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"VAVIntegralTimeout")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his").addMarker("itimeout")
                                        .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        HashMap defITPoint = hayStack.read("point and tuner and default and vav and pspread");
        ArrayList<HashMap> iTDefPointArr = hayStack.readPoint(defITPoint.get("id").toString());
        for (HashMap valMap : iTDefPointArr) {
            if (valMap.get("val") != null)
            {
                hayStack.getHSClient().pointWrite(HRef.copy(iTimeoutId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
    
        Point valveStartDamper = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"vavleActuationStartDamperPosDuringSysHeating")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                        .addMarker("valve").addMarker("start").addMarker("damper")
                                        .build();
        String valveStartDamperId = hayStack.addPoint(valveStartDamper);
        HashMap valveStartDamperPoint = hayStack.read("point and tuner and valve and start and damper");
        ArrayList<HashMap> valveStartDamperArr = hayStack.readPoint(valveStartDamperPoint.get("id").toString());
        for (HashMap valMap : valveStartDamperArr) {
            if (valMap.get("val") != null)
            {
                hayStack.getHSClient().pointWrite(HRef.copy(valveStartDamperId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        
        CCUHsApi.getInstance().syncEntityTree();
    }
}
