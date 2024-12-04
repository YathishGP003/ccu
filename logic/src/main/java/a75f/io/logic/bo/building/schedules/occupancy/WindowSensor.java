package a75f.io.logic.bo.building.schedules.occupancy;

import org.projecthaystack.UnknownRecException;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class WindowSensor implements OccupancyTrigger {
    private OccupancyUtil occupancyUtil;
    
    public WindowSensor(OccupancyUtil occupancyUtil) {
        this.occupancyUtil = occupancyUtil;
    }
    
    //TODO : Currently this is deep in the profile. Not exposed as a point
    public boolean isEnabled() {
        try {
            CcuLog.i(L.TAG_CCU_SCHEDULER,"WindowSensor isEnabled: " + occupancyUtil.isConfigEnabledByDomain(DomainName.doorWindowSensingEnable));
            return (occupancyUtil.isConfigEnabledByDomain(DomainName.doorWindowSensingEnable) || occupancyUtil.isConfigEnabled("window and sensing and enabled"));
        } catch (UnknownRecException e) {
            return false;
        }
    }
    
    public boolean hasTriggered() {
    
        if (!isEnabled()) {
            return false;
        }
        return (occupancyUtil.getSensorByDomain(DomainName.doorWindowSensorInput) || occupancyUtil.getSensorStatus("window and input"));
    }
}
