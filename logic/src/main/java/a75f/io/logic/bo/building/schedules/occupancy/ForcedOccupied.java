package a75f.io.logic.bo.building.schedules.occupancy;

import java.util.HashMap;
import java.util.Map;

import a75f.io.api.haystack.HSUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.ScheduleUtil;

public class ForcedOccupied implements OccupancyTrigger {
    
    private String equipRef;
    
    public ForcedOccupied(String equipRef) {
        this.equipRef = equipRef;
    }
    
    public boolean isEnabled() {
        return true;
    }
    
    public boolean hasTriggered() {
        Map<Object, Object> forcedOccupiedLevel = ScheduleUtil.getForcedOccupiedLevel(equipRef);
        if (forcedOccupiedLevel != null &&
            forcedOccupiedLevel.get("duration") != null &&
            forcedOccupiedLevel.get("val") != null &&
            forcedOccupiedLevel.get("who") != null) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "ForcedOccupied : active override "+forcedOccupiedLevel );
            return Double.parseDouble(forcedOccupiedLevel.get("duration").toString()) > 0 &&
                   !forcedOccupiedLevel.get("who").toString().equals("OccupancySensor");
        }
        return false;
    }
}
