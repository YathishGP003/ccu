package a75f.io.logic.bo.building.system.vav;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
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
                                  .addMarker("system").addMarker("userInput").addMarker("writable").addMarker("ci").addMarker("desired")
                                  .build();
        String desiredCIId = CCUHsApi.getInstance().addPoint(desiredCI);
        CCUHsApi.getInstance().writePoint(desiredCIId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.SYSTEM_DEFAULT_CI, 0);
        
        Point systemState = new Point.Builder()
                                    .setDisplayName(equipDis+"-"+"systemState")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipref)
                                    .addMarker("system").addMarker("userInput").addMarker("writable").addMarker("system").addMarker("state")
                                    .build();
        String systemStateId = CCUHsApi.getInstance().addPoint(systemState);
        CCUHsApi.getInstance().writePoint(systemStateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", (double) SystemState.AUTO.ordinal(), 0);
        
    }
    
    public double getUserInputVal(String tags) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and userInput and "+tags);
    
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
    
    public void setUserInputVal(String tags, int level, double val) {
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and userInput and "+tags);
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", val, 0);
    }
    
}
