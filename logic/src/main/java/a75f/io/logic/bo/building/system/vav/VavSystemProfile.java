package a75f.io.logic.bo.building.system.vav;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
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
        HashMap cdb = hayStack.read("point and system and config and "+tags);
        
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", val, 0);
    }
    
}
