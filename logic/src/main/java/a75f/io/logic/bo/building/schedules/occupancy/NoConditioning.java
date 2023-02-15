package a75f.io.logic.bo.building.schedules.occupancy;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.tuners.BuildingTunerCache;

public class NoConditioning implements OccupancyTrigger{

    CCUHsApi hayStack;
    String equipRef;

    public NoConditioning(CCUHsApi hayStack, String equipRef) {
        this.hayStack = hayStack;
        this.equipRef = equipRef;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean hasTriggered() {
        HashMap<Object,Object> equip = hayStack.readMapById(equipRef);
        Object profileObj = equip.get("profile");
        if(profileObj == null)
            return false;


        String profile = profileObj.toString();
        if (profile.contains("VAV") || profile.contains("DAB") || profile.contains("HYPERSTAT") ||
                profile.contains("SMARTSTAT")) {
            double zonePriority = hayStack.
                    readPointPriorityValByQuery("zone and priority and not dynamic and " +
                            "not spread and not multiplier and equipRef == \"" + equipRef + "\"");
            String conditioningMode = hayStack.
                    readDefaultStrVal("status and zone and message and equipRef == \"" + equipRef + "\"");
            isZoneAndSystemOnDifferentDirection(conditioningMode);
            double currentTemp = hayStack.readHisValByQuery("current and temp and sensor and equipRef == \"" + equipRef + "\"");
            double buildingLimitMin = BuildingTunerCache.getInstance().getBuildingLimitMin();
            double buildingLimitMax = BuildingTunerCache.getInstance().getBuildingLimitMax();
            double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();
            CcuLog.i(L.TAG_CCU_SCHEDULER,
                    "EmergencyConditioning - " + currentTemp + " " + buildingLimitMin + "-" + buildingLimitMax + "(" + tempDeadLeeway +
                            ")");
            return (((currentTemp < buildingLimitMin && currentTemp > (buildingLimitMin - tempDeadLeeway))
                    || (currentTemp > buildingLimitMax && currentTemp < (buildingLimitMax + tempDeadLeeway)))
                    && zonePriority == 0 && isZoneAndSystemOnDifferentDirection(conditioningMode));

        } else {
            return false;
        }
    }

    private boolean isZoneAndSystemOnDifferentDirection(String conditioningMode) {
        SystemController.State systemState = L.ccu().systemProfile.getSystemController().getSystemState();
        return ((conditioningMode.contains("Warming") || conditioningMode.contains("Heating")) && (systemState == SystemController.State.COOLING)) ||
                ((conditioningMode.contains("Cooling")) && (systemState == SystemController.State.HEATING)) ||
                systemState == SystemController.State.OFF;
    }


}
