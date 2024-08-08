package a75f.io.logic.bo.building.schedules.occupancy;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
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

        // Get the conditioning mode for the equip
        double conditioningMode = CCUHsApi.getInstance().
                readDefaultVal("(conditioning and mode and equipRef == \"" + equipId + "\"" + ")"+ " or " +
                        "(point and domainName == \"" + DomainName.conditioningMode + "\"" +  "and equipRef == \"" + equipId + "\"" + ")");

        if (zonePriority != null && zonePriority == 0 && L.ccu().systemProfile.getProfileType() != ProfileType.SYSTEM_DEFAULT) {
            // Check if the system is in heating or cooling range for emergency conditioning to enter
            // Check if the equip is in heating range
            boolean isWithinHeatingRange = currentTemp < buildingLimitMin
                    && currentTemp > buildingLimitMin - tempDeadLeeway
                    && (conditioningMode == StandaloneConditioningMode.AUTO.ordinal()
                    || conditioningMode == StandaloneConditioningMode.HEAT_ONLY.ordinal())
                    && L.ccu().systemProfile.isHeatingActive();

            // Check if the equip is in cooling range
            boolean isWithinCoolingRange = currentTemp > buildingLimitMax
                    && currentTemp < buildingLimitMax + tempDeadLeeway
                    && (conditioningMode == StandaloneConditioningMode.AUTO.ordinal()
                    || conditioningMode == StandaloneConditioningMode.COOL_ONLY.ordinal())
                    && L.ccu().systemProfile.isCoolingActive();

            return isWithinHeatingRange || isWithinCoolingRange;
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
