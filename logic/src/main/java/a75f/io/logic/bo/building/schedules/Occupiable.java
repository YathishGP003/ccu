package a75f.io.logic.bo.building.schedules;

import a75f.io.api.haystack.Schedule;

/**
 * Type of an entity that can be Occupied.
 * Specific Ports/Circuits/Profiles/Equips/Zones/Building could implement this behavior as required.
 */
public interface Occupiable {
    
    //Trigger than can influence the occupied mode.
    OccupiedTrigger getOccupiedTrigger(boolean skipDemandResponse);
    
    //Trigger that can influence the unoccupied mode.
    UnoccupiedTrigger getUnoccupiedTrigger(boolean skipDemandResponse);

    void prepareOccupied();

    void prepareUnoccupied();
}
