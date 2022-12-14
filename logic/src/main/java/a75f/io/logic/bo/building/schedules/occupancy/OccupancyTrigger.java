package a75f.io.logic.bo.building.schedules.occupancy;

/**
 * A schedule trigger could be from any entity that can influence the scheduling.
 * Like keycard , occupancySensor , buildingLimit breach etc.
 */
public interface OccupancyTrigger {
    
    boolean isEnabled();
    
    boolean hasTriggered();
}
