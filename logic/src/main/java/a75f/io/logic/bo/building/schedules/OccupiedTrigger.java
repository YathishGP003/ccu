package a75f.io.logic.bo.building.schedules;

/**
 * External triggers which influence the occupied mode of zone.
 */
public enum OccupiedTrigger {
    DoorWindowInput(Occupancy.WINDOW_OPEN),
    KeyCardInput(Occupancy.KEYCARD_AUTOAWAY),
    Autoaway(Occupancy.AUTOAWAY),
    EmergencyConditioning(Occupancy.EMERGENCY_CONDITIONING),
    Occupied (Occupancy.OCCUPIED),
    NoConditioning(Occupancy.NO_CONDITIONING);
    
    private final Occupancy mappedOccupancy;
    private OccupiedTrigger(Occupancy occupancy) {
        this.mappedOccupancy = occupancy;
    }
    
    public Occupancy toOccupancy() {
        return mappedOccupancy;
    }
}
