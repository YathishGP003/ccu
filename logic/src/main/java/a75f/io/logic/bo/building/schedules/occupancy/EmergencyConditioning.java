package a75f.io.logic.bo.building.schedules.occupancy;

import java.util.Date;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.tuners.BuildingTunerCache;

public class EmergencyConditioning implements OccupancyTrigger{
    
    CCUHsApi hayStack;
    String equipRef;
    
    public EmergencyConditioning(CCUHsApi hayStack, String equipRef) {
        this.hayStack = hayStack;
        this.equipRef = equipRef;
    }
    public boolean isEnabled() {
        return true;
    }
    
    public boolean hasTriggered() {
    
        double currentTemp = hayStack.readHisValByQuery("current and temp and sensor and equipRef == \""+equipRef+"\"");
        double buildingLimitMin = BuildingTunerCache.getInstance().getBuildingLimitMin();
        double buildingLimitMax =  BuildingTunerCache.getInstance().getBuildingLimitMax();
        double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();
        CcuLog.i(L.TAG_CCU_SCHEDULER,
                 "EmergencyConditioning - "+currentTemp+" "+buildingLimitMin+"-"+buildingLimitMax+"("+tempDeadLeeway+
                 ")");
        if ((currentTemp < buildingLimitMin && currentTemp > (buildingLimitMin-tempDeadLeeway))
           || (currentTemp > buildingLimitMax && currentTemp < (buildingLimitMax+tempDeadLeeway))) {
            return true;
        }
        return false;
    }
}
