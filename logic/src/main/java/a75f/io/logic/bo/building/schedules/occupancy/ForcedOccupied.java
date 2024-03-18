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
        return isZoneForcedOccupied(equipRef);
    }

    public static boolean isZoneForcedOccupied(String equipRef) {
        Map<Object, Object> forcedOccupiedLevel = ScheduleUtil.getForcedOccupiedLevel(equipRef);
        if (forcedOccupiedLevel != null &&
            forcedOccupiedLevel.get("duration") != null &&
            forcedOccupiedLevel.get("val") != null &&
            forcedOccupiedLevel.get("who") != null) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "ForcedOccupied : active override "+forcedOccupiedLevel );
            return Double.parseDouble(forcedOccupiedLevel.get("duration").toString()) > 0 &&
                    isNotAutoForcedOccupied(forcedOccupiedLevel);
        }
        return false;
    }

    private static boolean isNotAutoForcedOccupied(Map<Object, Object> forcedOccupiedLevel) {
        return !forcedOccupiedLevel.get("who").toString().contains("OccupancySensor") &&
                !forcedOccupiedLevel.get("who").toString().contains("Occupancy Sensor");
    }
}
