package a75f.io.logic.bo.building.system.vav;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.system.SystemProfile;
import a75f.io.logic.bo.building.system.SystemState;
import a75f.io.logic.tuners.TunerConstants;

/**
 * Created by samjithsadasivan on 1/10/19.
 */

public abstract class VavSystemProfile extends SystemProfile
{
    
    public void addVavSystemTuners(String equipref) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        Point targetCumulativeDamper = new Point.Builder()
                                               .setDisplayName(HSUtil.getDis(equipref)+ "-" + "targetCumulativeDamper")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                               .addMarker("target").addMarker("cumulative").addMarker("damper")
                                               .build();
        String targetCumulativeDamperId = hayStack.addPoint(targetCumulativeDamper);
        HashMap targetCumulativeDamperP = hayStack.read("point and tuner and default and vav and target and cumulative and damper");
        ArrayList<HashMap> targetCumulativeDamperArr = hayStack.readPoint(targetCumulativeDamperP.get("id").toString());
        for (HashMap valMap : targetCumulativeDamperArr) {
            if (valMap.get("val") != null)
            {
                hayStack.getHSClient().pointWrite(HRef.copy(targetCumulativeDamperId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
    
        Point analogFanSpeedMultiplier = new Point.Builder()
                                               .setDisplayName(HSUtil.getDis(equipref)+ "-" + "analogFanSpeedMultiplier")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                               .addMarker("analog").addMarker("fan").addMarker("speed").addMarker("multiplier")
                                               .build();
        String analogFanSpeedMultiplierId = hayStack.addPoint(analogFanSpeedMultiplier);
        HashMap analogFanSpeedMultiplierP = hayStack.read("point and tuner and default and vav and analog and fan and speed and multiplier");
        ArrayList<HashMap> analogFanSpeedMultiplierArr = hayStack.readPoint(analogFanSpeedMultiplierP.get("id").toString());
        for (HashMap valMap : analogFanSpeedMultiplierArr) {
            if (valMap.get("val") != null)
            {
                hayStack.getHSClient().pointWrite(HRef.copy(analogFanSpeedMultiplierId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
    
        Point humidityHysteresis = new Point.Builder()
                                                 .setDisplayName(HSUtil.getDis(equipref)+ "-" + "analogFanSpeedMultiplier")
                                                 .setSiteRef(siteRef)
                                                 .setEquipRef(equipref)
                                                 .addMarker("tuner").addMarker("vav").addMarker("writable").addMarker("his")
                                                 .addMarker("humidity").addMarker("hysteresis")
                                                 .build();
        String humidityHysteresisId = hayStack.addPoint(humidityHysteresis);
        HashMap humidityHysteresisPoint = hayStack.read("point and tuner and default and vav and humidity and hysteresis");
        ArrayList<HashMap> humidityHysteresisArr = hayStack.readPoint(humidityHysteresisPoint.get("id").toString());
        for (HashMap valMap : humidityHysteresisArr) {
            if (valMap.get("val") != null)
            {
                hayStack.getHSClient().pointWrite(HRef.copy(humidityHysteresisId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
    }
    
    protected void addUserIntentPoints(String equipref) {
        
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        
        Point desiredCI = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"desiredCI")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("ci").addMarker("desired")
                                  .build();
        String desiredCIId = CCUHsApi.getInstance().addPoint(desiredCI);
        CCUHsApi.getInstance().writePoint(desiredCIId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_DEFAULT_CI, 0);
        
        Point systemState = new Point.Builder()
                                    .setDisplayName(equipDis+"-"+"systemMode")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipref)
                                    .addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("rtu").addMarker("mode")
                                    .build();
        String systemStateId = CCUHsApi.getInstance().addPoint(systemState);
        CCUHsApi.getInstance().writePoint(systemStateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", (double) SystemState.OFF.ordinal(), 0);
    
        Point targetMaxInsideHumidty  = new Point.Builder()
                                    .setDisplayName(equipDis+"-"+"targetMaxInsideHumidty")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipref)
                                    .addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("target").addMarker("max").addMarker("inside").addMarker("humidity")
                                    .build();
        String targetMaxInsideHumidtyId = CCUHsApi.getInstance().addPoint(targetMaxInsideHumidty);
        CCUHsApi.getInstance().writePoint(targetMaxInsideHumidtyId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.TARGET_MAX_INSIDE_HUMIDITY, 0);
    
        Point targetMinInsideHumidty  = new Point.Builder()
                                                .setDisplayName(equipDis+"-"+"targetMinInsideHumidty")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipref)
                                                .addMarker("system").addMarker("userIntent").addMarker("writable").addMarker("target").addMarker("min").addMarker("inside").addMarker("humidity")
                                                .build();
        String targetMinInsideHumidtyId = CCUHsApi.getInstance().addPoint(targetMinInsideHumidty);
        CCUHsApi.getInstance().writePoint(targetMinInsideHumidtyId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.TARGET_MIN_INSIDE_HUMIDITY, 0);
    
        Point enableHumidifier  = new Point.Builder()
                                                .setDisplayName(equipDis+"-"+"enableHumidifier")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipref)
                                                .addMarker("system").addMarker("userIntent").addMarker("writable")
                                                .addMarker("enable").addMarker("humidifier")
                                                .build();
        String enableHumidifierId = CCUHsApi.getInstance().addPoint(enableHumidifier);
        CCUHsApi.getInstance().writePoint(enableHumidifierId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu",0.0, 0);
    
        Point enableDehumidifier  = new Point.Builder()
                                          .setDisplayName(equipDis+"-"+"enableDehumidifier")
                                          .setSiteRef(siteRef)
                                          .setEquipRef(equipref)
                                          .addMarker("system").addMarker("userIntent").addMarker("writable")
                                          .addMarker("enable").addMarker("dehumidifier")
                                          .build();
        String enableDehumidifierId = CCUHsApi.getInstance().addPoint(enableDehumidifier);
        CCUHsApi.getInstance().writePoint(enableDehumidifierId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu",0.0, 0);
    
    }
    
    public void updateAhuRef(String systemEquipId) {
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone");
    
        for (HashMap m : equips)
        {
            Equip q = new Equip.Builder().setHashMap(m).setAhuRef(systemEquipId).build();
            CCUHsApi.getInstance().updateEquip(q, q.getId());
        }
    }
    
    public double getUserIntentVal(String tags) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and userIntent and "+tags);
    
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
    
    public void setUserIntentVal(String tags, double val) {
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and userIntent and "+tags);
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", val, 0);
    }
    
}
