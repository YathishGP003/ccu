package a75f.io.logic.bo.building.schedules;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.schedules.occupancy.AutoAway;
import a75f.io.logic.bo.building.schedules.occupancy.AutoForcedOccupied;
import a75f.io.logic.bo.building.schedules.occupancy.EmergencyConditioning;
import a75f.io.logic.bo.building.schedules.occupancy.ForcedOccupied;
import a75f.io.logic.bo.building.schedules.occupancy.KeyCard;
import a75f.io.logic.bo.building.schedules.occupancy.OccupancyTrigger;
import a75f.io.logic.bo.building.schedules.occupancy.OccupancyUtil;
import a75f.io.logic.bo.building.schedules.occupancy.Preconditioning;
import a75f.io.logic.bo.building.schedules.occupancy.Vacation;
import a75f.io.logic.bo.building.schedules.occupancy.WindowSensor;

/**
 * Generic implementation of Occupiable interface.
 *
 */

public class OccupancyHandler implements Occupiable {
    
    String           equipRef;
    CCUHsApi         hayStack;
    
    OccupancyTrigger keyCardSensor;
    OccupancyTrigger windowSensor;
    OccupancyTrigger autoAway;
    OccupancyTrigger emergencyConditioning;
    OccupancyTrigger forcedOccupied;
    OccupancyTrigger autoForcedOccupied;
    OccupancyTrigger preconditioning;
    OccupancyTrigger vacation;
    OccupancyUtil    occupancyUtil;
    
    public OccupancyHandler(CCUHsApi hayStack, String equipRef, OccupancyUtil occupancyUtil) {
        this.hayStack = hayStack;
        this.equipRef = equipRef;
        this.occupancyUtil = occupancyUtil;
        
        keyCardSensor = new KeyCard(occupancyUtil);
        windowSensor = new WindowSensor(occupancyUtil);
        autoAway = new AutoAway(occupancyUtil);
        emergencyConditioning = new EmergencyConditioning(hayStack, equipRef);
        forcedOccupied = new ForcedOccupied(equipRef);
        autoForcedOccupied = new AutoForcedOccupied(occupancyUtil);
        preconditioning = new Preconditioning(hayStack, equipRef);
        vacation = new Vacation(hayStack, equipRef);
    }
    
    public Schedule getSchedule() {
        return null;
    }
    
    //Triggers that can influence the occupied mode.
    public OccupiedTrigger getOccupiedTrigger() {
        
        if (windowSensor.hasTriggered()) {
            return OccupiedTrigger.DoorWindowInput;
        }
        if (keyCardSensor.hasTriggered()) {
            return OccupiedTrigger.KeyCardInput;
        }
        if (autoAway.hasTriggered()) {
            return OccupiedTrigger.Autoaway;
        }
        if (emergencyConditioning.hasTriggered()) {
            return OccupiedTrigger.EmergencyConditioning;
        }
        
        return OccupiedTrigger.Occupied;
    }
    
    //Triggers that can influence the unoccupied mode.
    public UnoccupiedTrigger getUnoccupiedTrigger() {
        
        if (windowSensor.hasTriggered()) {
            return UnoccupiedTrigger.DoorWindowInput;
        }
        if (keyCardSensor.hasTriggered()) {
            return UnoccupiedTrigger.KeyCardInput;
        }
        if (preconditioning.hasTriggered()) {
            return UnoccupiedTrigger.Preconditioning;
        }
        if (forcedOccupied.hasTriggered()) {
            return UnoccupiedTrigger.ForcedOccupied;
        }
        if (autoForcedOccupied.hasTriggered()) {
            return UnoccupiedTrigger.AutoForcedOccupied;
        }
        if (emergencyConditioning.hasTriggered()) {
            return UnoccupiedTrigger.EmergencyConditioning;
        }
        if (vacation.hasTriggered()) {
            return UnoccupiedTrigger.Vacation;
        }
        return UnoccupiedTrigger.Unoccupied;
    }
}
