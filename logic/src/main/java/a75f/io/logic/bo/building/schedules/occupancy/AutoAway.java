package a75f.io.logic.bo.building.schedules.occupancy;

import org.projecthaystack.UnknownRecException;

import java.util.Date;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.Occupancy;

public class AutoAway implements OccupancyTrigger {
    private OccupancyUtil occupancyUtil;
    
    public AutoAway(OccupancyUtil occupancyUtil) {
        this.occupancyUtil = occupancyUtil;
    }
    
    public boolean isEnabled() {
        try {
            return occupancyUtil.isConfigEnabled("auto and away");
        } catch (UnknownRecException e) {
            return false;
        }
    }
    
    public boolean hasTriggered() {

        if (!isEnabled() || occupancyUtil.getSensorStatus("occupancy") ) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "AutoAway not enabled");
            return false;
        }
        
        Date lastOccupancy = occupancyUtil.getLastOccupancyDetectionTime();
        //TODO-Schedules - check how this is handled now.
        if (lastOccupancy == null) {
            return false;
        }
    
        Occupancy occupancyMode = occupancyUtil.getCurrentOccupiedMode();
        if (occupancyMode == Occupancy.AUTOFORCEOCCUPIED || occupancyMode == Occupancy.UNOCCUPIED) {
            return false;
        }
        
        double autoAwayTimeMinutes = occupancyUtil.getAutoAwayTime();
        double timeToAutoAway =
            (lastOccupancy.getTime() + autoAwayTimeMinutes * 60 * 1000) - System.currentTimeMillis();
        CcuLog.i(L.TAG_CCU_SCHEDULER, "autoAwayTimeMinutes "+autoAwayTimeMinutes+" : millis Left "+ timeToAutoAway);
       
        return timeToAutoAway <= 0;
    }
    
    
}
