package a75f.io.logic.bo.building.schedules.occupancy;

import org.projecthaystack.UnknownRecException;

import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleUtil;

public class KeyCard implements OccupancyTrigger {
    
    private OccupancyUtil occupancyUtil;
    public KeyCard(OccupancyUtil occupancyUtil) {
        this.occupancyUtil = occupancyUtil;
    }
    
    //TODO : Currently this is deep in the profile. Not exposed as a point
    public boolean isEnabled() {
        try {
            return occupancyUtil.isConfigEnabled("keycard and sensing and enabled");
        } catch (UnknownRecException e) {
            return false;
        }
    }
    
    public boolean hasTriggered() {
        if (!isEnabled()) {
            return false;
        }
        Occupied occStatus = ScheduleUtil.getOccupied(occupancyUtil.getEquipRef());
        if (occStatus == null) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "Occupied entry not found , disable auto forced occupied");
            return false;
        }
        Occupancy occupancyMode = occupancyUtil.getCurrentOccupiedMode();
        if(occStatus.isOccupied() &&
                (occupancyMode == Occupancy.OCCUPIED || occupancyMode == Occupancy.AUTOFORCEOCCUPIED
                        || occupancyMode == Occupancy.KEYCARD_AUTOAWAY || occupancyMode == Occupancy.DEMAND_RESPONSE_OCCUPIED
                        || occupancyMode == Occupancy.AUTOAWAY || occupancyMode == Occupancy.EMERGENCY_CONDITIONING)) {
            return occupancyUtil.getSensorStatus("keycard and input");
        }
        if(!occStatus.isOccupied() &&
                (occupancyMode == Occupancy.UNOCCUPIED ||occupancyMode ==
                        Occupancy.DEMAND_RESPONSE_UNOCCUPIED || occupancyMode == Occupancy.AUTOFORCEOCCUPIED
                        || occupancyMode == Occupancy.EMERGENCY_CONDITIONING)) {
            return (!occupancyUtil.getSensorStatus("keycard and input"));
        }
        return false;
    }
}
