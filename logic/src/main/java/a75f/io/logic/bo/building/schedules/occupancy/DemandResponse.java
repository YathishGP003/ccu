package a75f.io.logic.bo.building.schedules.occupancy;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.bo.util.DemandResponseMode;

public class DemandResponse implements OccupancyTrigger {
    CCUHsApi hayStack;
    String equipRef;
    public DemandResponse(CCUHsApi hayStack, String equipRef) {
        this.hayStack = hayStack;
        this.equipRef = equipRef;
    }

    @Override
    public boolean isEnabled() {
        return DemandResponseMode.isDREnrollmentSelected(hayStack);
    }

    @Override
    public boolean hasTriggered() {
        return isDRModeActivated(hayStack);
    }

    public static boolean isDRModeActivated(CCUHsApi hayStack) {
        if(!DemandResponseMode.isDREnrollmentSelected(hayStack)){
            return false;
        }
        return DemandResponseMode.isDRModeActivated(hayStack);
    }
}
