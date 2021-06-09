package a75f.io.logic.bo.util;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.bo.building.Occupancy;

public class SystemScheduleUtil {
    
    /**
     * Checks if the zone is in forced-occupied mode. Note that all the equips do not have
     * occupied mode point.
     * @param equipRef
     * @return
     */
    public static boolean isZoneForcedOccupied(String equipRef) {
        HashMap point = CCUHsApi.getInstance().read("point and " +
                                                    "occupancy and mode and equipRef == \"" + equipRef + "\"");
        if (!point.isEmpty()) {
            double occStatus = CCUHsApi.getInstance().readHisValById(point.get("id").toString());
            return Occupancy.FORCEDOCCUPIED == Occupancy.values()[(int) occStatus];
        }
        
        return false;
    }
}
