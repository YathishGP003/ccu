package a75f.io.logic.bo.building.schedules.occupancy;

import org.projecthaystack.UnknownRecException;

import java.util.Date;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleUtil;

public class AutoAway implements OccupancyTrigger {
    private OccupancyUtil occupancyUtil;
    
    public AutoAway(OccupancyUtil occupancyUtil) {
        this.occupancyUtil = occupancyUtil;
    }
    
    public boolean isEnabled() {
        try {
            boolean isEnabled = occupancyUtil.isConfigEnabledByDomain(DomainName.autoAway);
            return isEnabled || occupancyUtil.isConfigEnabled("auto and away");
        } catch (UnknownRecException e) {
            return false;
        }
    }
    
    public boolean hasTriggered() {
        return isZoneInAutoAwayMode(occupancyUtil);
    }

    public static boolean isZoneInAutoAwayMode(OccupancyUtil occupancyUtil) {
        if (!isAutoAwayEnabled(occupancyUtil) || occupancyUtil.getSensorStatus("occupancy") ) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "AutoAway not enabled");
            return false;
        }

        Date lastOccupancy = occupancyUtil.getLastOccupancyDetectionTime();
        Occupancy occupancyMode = occupancyUtil.getCurrentOccupiedMode();
        //TODO-Schedules - check how this is handled now.
        if (lastOccupancy == null) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "Occupancy detection not active");
            //It safe to initialize occupancy Detection when we are already occupied. Needed when a profile is
            //created during occupancy and there is no history of detection.
            if (occupancyMode == Occupancy.OCCUPIED) {
                ScheduleUtil.setOccupancyDetection(CCUHsApi.getInstance(), occupancyUtil.getEquipRef(), true);
            }
            return false;
        }

        CcuLog.i(L.TAG_CCU_SCHEDULER, "Current occupancyMode : "+occupancyMode);
        if (occupancyMode == Occupancy.FORCEDOCCUPIED ||
                occupancyMode == Occupancy.AUTOFORCEOCCUPIED ||
                occupancyMode == Occupancy.UNOCCUPIED ||
                occupancyMode == Occupancy.VACATION) {
            return false;
        }

        double autoAwayTimeMinutes = occupancyUtil.getAutoAwayTime();
        double timeToAutoAway =
            (lastOccupancy.getTime() + autoAwayTimeMinutes * 60 * 1000) - System.currentTimeMillis();
        CcuLog.i(L.TAG_CCU_SCHEDULER, "autoAwayTimeMinutes "+autoAwayTimeMinutes+" : seconds Left "+ (timeToAutoAway /1000));

        return timeToAutoAway <= 0;
    }

    private static boolean isAutoAwayEnabled(OccupancyUtil occupancyUtil) {
        boolean isEnabled = occupancyUtil.isConfigEnabledByDomain(DomainName.autoAway);
        return isEnabled || occupancyUtil.isConfigEnabled("auto and away");
    }
}
