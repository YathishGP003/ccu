package a75f.io.logic.bo.building.schedules.occupancy;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.limits.SchedulabeLimits;
import a75f.io.logic.tuners.BuildingTunerCache;

public class EmergencyConditioning implements OccupancyTrigger {

    CCUHsApi hayStack;
    String equipRef;

    public EmergencyConditioning(CCUHsApi hayStack, String equipRef) {
        this.hayStack = hayStack;
        this.equipRef = equipRef;
    }

    public static boolean isZoneInEmergencyConditioning(CCUHsApi hayStack, String equipId) {
        Double zonePriority = CCUHsApi.getInstance().
                readPointPriorityValByQuery("zone and priority and not dynamic and " +
                        "not spread and not multiplier and equipRef == \"" + equipId + "\"");
        double currentTemp = hayStack.readHisValByQuery("(current or space) and temp and sensor and equipRef == \"" + equipId + "\"");
        double buildingLimitMin = BuildingTunerCache.getInstance().getBuildingLimitMin();
        double buildingLimitMax = BuildingTunerCache.getInstance().getBuildingLimitMax();
        double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();
        String conditioningMode = CCUHsApi.getInstance().
                readDefaultStrVal("status and zone and message and equipRef == \"" + equipId + "\"");

        if (zonePriority != null && zonePriority == 0 && L.ccu().systemProfile.getProfileType() != ProfileType.SYSTEM_DEFAULT) {
            // Ensure we are skipping this check during default system profile
            return (currentTemp < buildingLimitMin && currentTemp > buildingLimitMin - tempDeadLeeway &&
                    ((conditioningMode.contains("Warming") || conditioningMode.contains("HEATING")) && L.ccu().systemProfile.isHeatingActive()) ||
                    currentTemp > buildingLimitMax && currentTemp < buildingLimitMax + tempDeadLeeway &&
                            (conditioningMode.contains("Cooling") && L.ccu().systemProfile.isCoolingActive()));
        }
        CcuLog.i(L.TAG_CCU_SCHEDULER,
                "EmergencyConditioning - " + currentTemp + " " + buildingLimitMin + "-" + buildingLimitMax + "(" + tempDeadLeeway +
                        ")");
        return currentTemp < buildingLimitMin && currentTemp > buildingLimitMin - tempDeadLeeway ||
                currentTemp > buildingLimitMax && currentTemp < buildingLimitMax + tempDeadLeeway;
    }


    public boolean isEnabled() {
        return true;
    }

    public boolean hasTriggered() {
        return isZoneInEmergencyConditioning(hayStack, equipRef);
    }
}
