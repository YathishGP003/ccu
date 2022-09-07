package a75f.io.logic.bo.building.schedules.occupancy;

import org.projecthaystack.UnknownRecException;

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
        return occupancyUtil.getSensorStatus("keycard and input");
    }
}
