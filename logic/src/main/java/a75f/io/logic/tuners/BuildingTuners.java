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
    }
    
    public void addVavTuners() {
    
        HashMap tuner = CCUHsApi.getInstance().read("point and tuner and vav");
        if (tuner != null && tuner.size() > 0) {
            Log.d("CCU","Vav Tuner points already created");
            return;
        }
    
        Point coolingDb = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"VAVCoolingDB")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("cooling").addMarker("deadband")
                                 .setUnit("\u00B0F")
                                 .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        hayStack.writePoint(coolingDbId, TunerDefalutVals.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerDefalutVals.VAV_COOLING_DB, 0);
    
        Point heatingDb = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"VAVCoolingDB")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("heating").addMarker("deadband")
                                  .setUnit("\u00B0F")
                                  .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        hayStack.writePoint(heatingDbId, TunerDefalutVals.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerDefalutVals.VAV_HEATING_DB, 0);
        
        Point propGain = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"VAVProportionalGain")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("pgain")
                                   .setUnit("\u00B0F")
                                   .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePoint(pgainId, TunerDefalutVals.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerDefalutVals.VAV_PROPORTIONAL_GAIN, 0);
        
    
        Point integralGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"VAVIntegralGain")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("igain")
                                 .setUnit("\u00B0F")
                                 .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePoint(igainId, TunerDefalutVals.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerDefalutVals.VAV_INTEGRAL_GAIN, 0);
        
        Point propSpread = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"VAVProportionalSpread")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("pspread")
                                 .setUnit("\u00B0F")
                                 .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        hayStack.writePoint(pSpreadId, TunerDefalutVals.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerDefalutVals.VAV_PROPORTIONAL_SPREAD, 0);
    
        Point integralTimeout = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"VAVIntegralTimeout")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef)
                                     .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("itimeout")
                                     .setUnit("\u00B0F")
                                     .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePoint(iTimeoutId, TunerDefalutVals.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerDefalutVals.VAV_INTEGRAL_TIMEOUT, 0);
        
        CCUHsApi.getInstance().syncEntityTree();
    }
    
}
