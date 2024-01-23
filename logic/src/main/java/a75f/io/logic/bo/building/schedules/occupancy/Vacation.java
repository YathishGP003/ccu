package a75f.io.logic.bo.building.schedules.occupancy;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Occupied;
import a75f.io.logic.bo.building.schedules.ScheduleManager;

public class Vacation implements OccupancyTrigger{
    
    CCUHsApi hayStack;
    Equip   equip;
    
    public Vacation(CCUHsApi hayStack, String equipRef) {
        this.hayStack = hayStack;
        equip = new Equip.Builder().setHDict(hayStack.readHDictById(equipRef))
                                   .build();
    }
    public boolean isEnabled() {
        return true;
    }
    
    public boolean hasTriggered() {
    
        Occupied occupied = ScheduleManager.getInstance().getOccupiedModeCache(equip.getRoomRef());
        return occupied != null && occupied.getVacation() != null;
        
        //TODO - Optimize , should be read only once.
       /* ArrayList<Schedule> systemVacation = hayStack.getSystemSchedule(true);
        if (ScheduleUtil.getActiveVacation(systemVacation) != null) {
            return true;
        }
        ArrayList<Schedule> zoneVacations = hayStack.getZoneSchedule(equip.getRoomRef(), true);
        return ScheduleUtil.getActiveVacation(zoneVacations) != null;*/
    }
}
