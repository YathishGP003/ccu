package a75f.io.logic.bo.building.schedules.occupancy;

import java.util.Date;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HisItem;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.tuners.TunerUtil;

public class OccupancyUtil {
    private CCUHsApi hayStack;
    private String equipRef;
    
    public OccupancyUtil(CCUHsApi hayStack, String equipRef) {
        this.hayStack = hayStack;
        this.equipRef = equipRef;
    }
    
    public String getEquipRef() {
        return equipRef;
    }
    
    public boolean isConfigEnabled(String config) {
        return hayStack.readDefaultVal("config and "+config+" and equipRef == \"" + equipRef + "\"") > 0;
    }
    
    public Occupancy getCurrentOccupiedMode() {
        int occupiedMode =  hayStack.readHisValByQuery("(occupancy or occupied) and his and " +
                                                       "mode and equipRef  == \"" + equipRef + "\"").intValue();
        return Occupancy.values()[occupiedMode];
    }
    
    public void setOccupancyMode(Occupancy mode) {
        hayStack.writeHisValByQuery("(occupied or occupancy) and mode and equipRef == \""+equipRef+"\"", (double) mode.ordinal());
    }
    
    public double getAutoAwayTime() {
        return TunerUtil.readTunerValByQuery("auto and away and time", equipRef, hayStack);
    }
    
    public double getForcedOccupiedTime() {
        return TunerUtil.readTunerValByQuery("forced and occupied and time", equipRef, hayStack);
    }
    public Date getLastOccupancyDetectionTime() {
        HashMap<Object, Object> occupancyDetection = hayStack.readEntity(
            "occupancy and detection and equipRef == \"" +equipRef+ "\"");
        if (!occupancyDetection.isEmpty()) {
            HisItem hisItem = hayStack.curRead(occupancyDetection.get("id").toString());
            // '0' value for occupancy detection, means it has been reset due to a state change (
            // as in occupied ->
            // unoccupied)
            if (hisItem != null) {
                CcuLog.i(L.TAG_CCU_SCHEDULER, "lastOccupancy detection " + hisItem);
            }
            return (hisItem == null || hisItem.getVal() == 0) ? null : hisItem.getDate();
        }
        return null;
    }
    
    public boolean getOccupancyDetection() {
        return hayStack.readHisValByQuery("occupancy and detection and equipRef  == \"" + equipRef + "\"") > 0;
    }
    
    public boolean getSensorStatus(String sensor) {
        return hayStack.readHisValByQuery(sensor+" and sensor and equipRef  == \"" + equipRef + "\"") > 0;
    }


}
