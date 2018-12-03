package a75f.io.logic.tuners;

import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;

/**
 * Created by samjithsadasivan on 10/5/18.
 */

public class BuildingTuners
{
    
    private String equipRef;
    private String equipDis;
    private String siteRef;
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
            equipRef = (String)tuner.get("id");
            equipDis = (String)tuner.get("dis");
            HashMap siteMap = hayStack.read(Tags.SITE);
            siteRef = (String) siteMap.get(Tags.ID);
            Log.d("CCU","Building Tuner equip already present");
            return;
        }
        System.out.println("Build Tuner Equip does not exist. Create Now");
        HashMap siteMap = hayStack.read(Tags.SITE);
        siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        Equip tunerEquip= new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName(siteDis+"-BuildingTuner")
                          .addMarker("equip")
                          .addMarker("tuner")
                          .build();
        equipRef = hayStack.addEquip(tunerEquip);
        equipDis = siteDis+"-BuildingTuner";
        
        addDefaultSystemTuners();
        
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
    
        Point desiredCI = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"DesiredCI")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("writable").addMarker("system").addMarker("ci").addMarker("desired")
                                   .setUnit("\u00B0F")
                                   .build();
        String desiredCIId = hayStack.addPoint(desiredCI);
        hayStack.writePoint(desiredCIId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_DEFAULT_CI, 0);
    
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
                                  .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("cooling").addMarker("deadband")
                                  .setUnit("\u00B0F")
                                  .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        hayStack.writePoint(coolingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_COOLING_DB, 0);
        
        Point heatingDb = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"VAVCoolingDB")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("heating").addMarker("deadband")
                                  .setUnit("\u00B0F")
                                  .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        hayStack.writePoint(heatingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_HEATING_DB, 0);
        
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"VAVProportionalGain")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("pgain")
                                 .setUnit("\u00B0F")
                                 .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePoint(pgainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_GAIN, 0);
        
        
        Point integralGain = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"VAVIntegralGain")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef)
                                     .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("igain")
                                     .setUnit("\u00B0F")
                                     .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePoint(igainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_GAIN, 0);
        
        Point propSpread = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"VAVProportionalSpread")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("pspread")
                                   .setUnit("\u00B0F")
                                   .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        hayStack.writePoint(pSpreadId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_PROPORTIONAL_SPREAD, 0);
        
        Point integralTimeout = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"VAVIntegralTimeout")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef)
                                        .addMarker("tuner").addMarker("default").addMarker("vav").addMarker("writable").addMarker("itimeout")
                                        .setUnit("\u00B0F")
                                        .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePoint(iTimeoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.VAV_INTEGRAL_TIMEOUT, 0);
        
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    public void addEquipVavTuners(String equipdis, String equipref, int level) {
    
        Log.d("CCU","addEquipVavTuners for "+equipdis);
        Point coolingDb = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"VAVCoolingDB")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("cooling").addMarker("deadband")
                                  .setUnit("\u00B0F")
                                  .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        hayStack.writePoint(coolingDbId, level, "ccu", TunerConstants.VAV_COOLING_DB, 0);
        
        Point heatingDb = new Point.Builder()
                                  .setDisplayName(equipdis+"-"+"VAVCoolingDB")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("heating").addMarker("deadband")
                                  .setUnit("\u00B0F")
                                  .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        hayStack.writePoint(heatingDbId, level, "ccu", TunerConstants.VAV_HEATING_DB, 0);
        
        Point propGain = new Point.Builder()
                                 .setDisplayName(equipdis+"-"+"VAVProportionalGain")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("pgain")
                                 .setUnit("\u00B0F")
                                 .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePoint(pgainId, level, "ccu", TunerConstants.VAV_PROPORTIONAL_GAIN, 0);
        
        
        Point integralGain = new Point.Builder()
                                     .setDisplayName(equipdis+"-"+"VAVIntegralGain")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("igain")
                                     .setUnit("\u00B0F")
                                     .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePoint(igainId, level, "ccu", TunerConstants.VAV_INTEGRAL_GAIN, 0);
        
        Point propSpread = new Point.Builder()
                                   .setDisplayName(equipdis+"-"+"VAVProportionalSpread")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipref)
                                   .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("pspread")
                                   .setUnit("\u00B0F")
                                   .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        hayStack.writePoint(pSpreadId, level, "ccu", TunerConstants.VAV_PROPORTIONAL_SPREAD, 0);
        
        Point integralTimeout = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"VAVIntegralTimeout")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("itimeout")
                                        .setUnit("\u00B0F")
                                        .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePoint(iTimeoutId, level, "ccu", TunerConstants.VAV_INTEGRAL_TIMEOUT, 0);
        
        CCUHsApi.getInstance().syncEntityTree();
    }
}
