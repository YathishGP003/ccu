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
                          .addMarker("equip")
                          .addMarker("tuner")
                          .setTz(siteMap.get("tz").toString())
                          .build();
        equipRef = hayStack.addEquip(tunerEquip);
        equipDis = siteDis+"-BuildingTuner";
        tz = siteMap.get("tz").toString();
        //addDefaultSystemTuners();
        
    }
    
    public void addDefaultSystemTuners() {
        Point analog1Min = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"Analog1Min")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("tuner").addMarker("analog1").addMarker("writable").addMarker("system").addMarker("min")
                                  .setUnit("\u00B0F")
                                  .build();
        String analog1MinId = hayStack.addPoint(analog1Min);
        hayStack.writePoint(analog1MinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_ANALOG1_MIN, 0);
    
        Point analog1Max = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"Analog1Max")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("analog1").addMarker("writable").addMarker("system").addMarker("max")
                                   .setUnit("\u00B0F")
                                   .build();
        String analog1MaxId = hayStack.addPoint(analog1Max);
        hayStack.writePoint(analog1MaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_ANALOG1_MAX, 0);
    
        Point analog2Min = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"Analog2Min")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("analog2").addMarker("writable").addMarker("system").addMarker("min")
                                   .setUnit("\u00B0F")
                                   .build();
        String analog2MinId = hayStack.addPoint(analog2Min);
        hayStack.writePoint(analog2MinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_ANALOG2_MIN, 0);
    
        Point analog2Max = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"Analog2Max")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("analog2").addMarker("writable").addMarker("system").addMarker("max")
                                   .setUnit("\u00B0F")
                                   .build();
        String analog2MaxId = hayStack.addPoint(analog2Max);
        hayStack.writePoint(analog2MaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_ANALOG2_MAX, 0);
    
    
        Point analog3Min = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"Analog3Min")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("analog3").addMarker("writable").addMarker("system").addMarker("min")
                                   .setUnit("\u00B0F")
                                   .build();
        String analog3MinId = hayStack.addPoint(analog3Min);
        hayStack.writePoint(analog3MinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_ANALOG3_MIN, 0);
    
        Point analog3Max = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"Analog3Max")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("analog3").addMarker("writable").addMarker("system").addMarker("max")
                                   .setUnit("\u00B0F")
                                   .build();
        String analog3MaxId = hayStack.addPoint(analog3Max);
        hayStack.writePoint(analog3MaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_ANALOG3_MAX, 0);
    
        Point analog4Min = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"Analog4Min")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("analog4").addMarker("writable").addMarker("system").addMarker("min")
                                   .setUnit("\u00B0F")
                                   .build();
        String analog4MinId = hayStack.addPoint(analog4Min);
        hayStack.writePoint(analog4MinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_ANALOG4_MIN, 0);
    
        Point analog4Max = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"Analog4Max")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("analog4").addMarker("writable").addMarker("system").addMarker("max")
                                   .setUnit("\u00B0F")
                                   .build();
        String analog4MaxId = hayStack.addPoint(analog4Max);
        hayStack.writePoint(analog4MaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_ANALOG4_MAX, 0);
        
        Point coolingSatMin = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"coolingSatMin")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("coolingSat").addMarker("writable").addMarker("system").addMarker("min")
                                   .setUnit("\u00B0F")
                                   .build();
        String coolingSatMinId = hayStack.addPoint(coolingSatMin);
        hayStack.writePoint(coolingSatMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_COOLING_SAT_MIN, 0);
    
        Point coolingSatMax = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"coolingSatMax")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("coolingSat").addMarker("writable").addMarker("system").addMarker("max")
                                   .setUnit("\u00B0F")
                                   .build();
        String coolingSatMaxId = hayStack.addPoint(coolingSatMax);
        hayStack.writePoint(coolingSatMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_COOLING_SAT_MAX, 0);
    
        Point heatingSatMin = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"heatingSatMin")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef)
                                      .addMarker("tuner").addMarker("heatingSat").addMarker("writable").addMarker("system").addMarker("min")
                                      .setUnit("\u00B0F")
                                      .build();
        String heatingSatMinId = hayStack.addPoint(heatingSatMin);
        hayStack.writePoint(heatingSatMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_HEATING_SAT_MIN, 0);
    
        Point heatingSatMax = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"heatingSatMax")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef)
                                      .addMarker("tuner").addMarker("heatingSat").addMarker("writable").addMarker("system").addMarker("max")
                                      .setUnit("\u00B0F")
                                      .build();
        String heatingSatMaxId = hayStack.addPoint(heatingSatMax);
        hayStack.writePoint(heatingSatMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_HEATING_SAT_MAX, 0);
    
        Point co2TargetMin = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"co2TargetMin")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef)
                                      .addMarker("tuner").addMarker("co2Target").addMarker("writable").addMarker("system").addMarker("min")
                                      .setUnit("\u00B0F")
                                      .build();
        String co2TargetMinId = hayStack.addPoint(co2TargetMin);
        hayStack.writePoint(co2TargetMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_CO2TARGET_MIN, 0);
    
        Point co2TargetMax = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"co2TargetMax")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef)
                                      .addMarker("tuner").addMarker("co2Target").addMarker("writable").addMarker("system").addMarker("max")
                                      .setUnit("\u00B0F")
                                      .build();
        String co2TargetMaxId = hayStack.addPoint(co2TargetMax);
        hayStack.writePoint(co2TargetMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_CO2TARGET_MAX, 0);
    
        Point spTargetMin = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"spTargetMin")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef)
                                     .addMarker("tuner").addMarker("spTarget").addMarker("writable").addMarker("system").addMarker("min")
                                     .setUnit("\u00B0F")
                                     .build();
        String spTargetMinId = hayStack.addPoint(spTargetMin);
        hayStack.writePoint(spTargetMinId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_SPTARGET_MIN, 0);
    
        Point spTargetMax = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"co2TargetMax")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef)
                                     .addMarker("tuner").addMarker("spTarget").addMarker("writable").addMarker("system").addMarker("max")
                                     .setUnit("\u00B0F")
                                     .build();
        String spTargetMaxId = hayStack.addPoint(spTargetMax);
        hayStack.writePoint(spTargetMaxId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_SPTARGET_MAX, 0);
    
    }
    
    /*public void addDefaultVavTuners() {
        HashMap tuner = CCUHsApi.getInstance().read("point and tuner and default and vav");
        if (tuner != null && tuner.size() == 0) {
            addEquipVavTuners(equipDis, equipRef, TunerConstants.VAV_DEFAULT_VAL_LEVEL);
        } else {
            Log.d("CCU","Vav Tuner points already exist");
        }
    }*/
    
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
                                 .setUnit("\u00B0")
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
                                     .setUnit("\u00B0")
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
                                   .setUnit("\u00B0")
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
                                        .setUnit("\u00B0")
                                        .setTz(tz)
                                        .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePoint(iTimeoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_TIMEOUT, 0);
        hayStack.writeHisValById(iTimeoutId, TunerConstants.VAV_INTEGRAL_TIMEOUT);
        
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
                                 .setUnit("\u00B0F")
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
                                     .setUnit("\u00B0F")
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
                                   .setUnit("\u00B0F")
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
                                        .setUnit("\u00B0F")
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
        CCUHsApi.getInstance().syncEntityTree();
    }
}
