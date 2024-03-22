package a75f.io.logic.bo.building.schedules.occupancy;

import org.projecthaystack.UnknownRecException;

import java.util.Date;

import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleUtil;

public class AutoForcedOccupied implements OccupancyTrigger {
    
    private OccupancyUtil occupancyUtil;
    
    public AutoForcedOccupied(OccupancyUtil occupancyUtil) {
        this.occupancyUtil = occupancyUtil;
    }
    
    public boolean isEnabled() {
        try {
            return occupancyUtil.isConfigEnabled("auto and forced and occupied") ||
                   occupancyUtil.isConfigEnabled("auto and forced and occupancy");
        } catch (UnknownRecException e) {
            return false;
        }
    }
    
    /**
     * AutoForceOccupied is considered as "Triggered" when we have received an occupancy detection in the last 120
     * minutes and the equip is currently not in any of the state s
     * occupied/autoaway/preconditioning/forced-occupied
     *
     * This return true even when we are already auto away state.
     * It is up to the EquipScheduleHandler to determine if the desiredTemp duration at level 4 has to be updated or
     * not.
     * @return
     */
    public boolean hasTriggered() {
        return isZoneInAutoForcedOccupied(occupancyUtil);
    }

    public static boolean isAutoForceEnabled(OccupancyUtil occupancyUtil) {
        return occupancyUtil.isConfigEnabled("auto and forced and occupied") ||
                occupancyUtil.isConfigEnabled("auto and forced and occupancy");
    }

    public static boolean isZoneInAutoForcedOccupied(OccupancyUtil occupancyUtil) {
        if (!isAutoForceEnabled(occupancyUtil)) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "AutoForcedOccupied not enabled");
            return false;
        }

        Date lastOccupancy = occupancyUtil.getLastOccupancyDetectionTime();
        //TODO-Schedules - check how this is handled now.
        if (lastOccupancy == null) {
            return false;
        }

        Occupied occStatus = ScheduleUtil.getOccupied(occupancyUtil.getEquipRef());
        if (occStatus == null) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "Occupied entry not found , disable auto forced occupied");
            return false;
        }
        Occupancy occupancyMode = occupancyUtil.getCurrentOccupiedMode();

        if ((!occStatus.isOccupied() || occStatus.getVacation() != null)
            && occupancyMode != Occupancy.OCCUPIED
            && occupancyMode != Occupancy.AUTOAWAY
            && occupancyMode != Occupancy.PRECONDITIONING
            && occupancyMode != Occupancy.FORCEDOCCUPIED) {

            double forcedOccupiedTimeMinutes = occupancyUtil.getForcedOccupiedTime();

            double timeToForcedOccupiedExpiry =
                (lastOccupancy.getTime() + forcedOccupiedTimeMinutes * 60 * 1000) - System.currentTimeMillis();
            CcuLog.i(L.TAG_CCU_SCHEDULER, "forcedOccupiedTimeMinutes "+forcedOccupiedTimeMinutes+
                                                                " : timeToForcedOccupiedExpiry "+ timeToForcedOccupiedExpiry);

            return timeToForcedOccupiedExpiry > 0;
        }
        return false;
    }
}
