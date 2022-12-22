package a75f.io.logic.bo.building.schedules;

import a75f.io.api.haystack.Schedule;

/**
 * Type of an entity that can be Occupied.
 * Specific Ports/Circuits/Profiles/Equips/Zones/Building could implement this behavior as required.
 */
public interface Schedulable {
    //Currently active schedule.
    Schedule getSchedule();
   
    void updateDesiredTemp(Occupancy current, Occupancy updated, Schedule schedule,OccupancyData occupancyData);
}
