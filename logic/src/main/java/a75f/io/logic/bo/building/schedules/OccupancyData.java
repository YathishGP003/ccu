package a75f.io.logic.bo.building.schedules;

/**
 * Class to abstract all the occupancy data for an equip.
 * This would be cached in hashmap during once-time zone processing and then reused to display zone status
 * or any other algorithm.
 */
public class OccupancyData {
    public boolean isOccupied = false;
    public Occupancy occupancy;
    public OccupiedTrigger occupiedTrigger = OccupiedTrigger.Occupied;
    public UnoccupiedTrigger unoccupiedTrigger = UnoccupiedTrigger.Unoccupied;
    public String message;
}
