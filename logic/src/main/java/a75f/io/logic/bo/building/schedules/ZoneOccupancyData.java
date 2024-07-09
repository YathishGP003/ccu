package a75f.io.logic.bo.building.schedules;

public class ZoneOccupancyData {
    public Occupancy occupancy;
    public Occupancy occupancyDR = Occupancy.NONE;
    public OccupiedTrigger occupiedTrigger = OccupiedTrigger.Occupied;
    public OccupiedTrigger occupiedTriggerDR = OccupiedTrigger.Occupied;
    public UnoccupiedTrigger unoccupiedTrigger = UnoccupiedTrigger.Unoccupied;
    public UnoccupiedTrigger unoccupiedTriggerDR = UnoccupiedTrigger.Unoccupied;
    public String message;
}