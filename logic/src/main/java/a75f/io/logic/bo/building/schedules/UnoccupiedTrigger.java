package a75f.io.logic.bo.building.schedules;

/**
 * External triggers which influence the Unoccupied mode of zone.
 */
public enum UnoccupiedTrigger {
    DoorWindowInput(Occupancy.WINDOW_OPEN),
    KeyCardInput(Occupancy.KEYCARD_AUTOAWAY),
    Preconditioning(Occupancy.PRECONDITIONING),
    ForcedOccupied(Occupancy.FORCEDOCCUPIED),
    AutoForcedOccupied(Occupancy.AUTOFORCEOCCUPIED),
    EmergencyConditioning(Occupancy.EMERGENCY_CONDITIONING),
    Vacation(Occupancy.VACATION),
    Unoccupied(Occupancy.UNOCCUPIED);
    
    private final Occupancy mappedOccupancy;
    private UnoccupiedTrigger(Occupancy occupancy) {
        this.mappedOccupancy = occupancy;
    }
    
    public Occupancy toOccupancy() {
        return mappedOccupancy;
    }
}
