package a75f.io.logic.bo.building.schedules.occupancy;

import static a75f.io.logic.bo.building.schedules.Occupancy.PRECONDITIONING;

import android.util.Log;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.schedules.ScheduleUtil;
import a75f.io.logic.tuners.TunerUtil;

public class Preconditioning implements OccupancyTrigger {
    
    CCUHsApi hayStack;
    String   equipRef;
    
    public Preconditioning(CCUHsApi hayStack, String equipRef) {
        this.hayStack = hayStack;
        this.equipRef = equipRef;
    }
    public boolean isEnabled() {
        return true;
    }
    
    public boolean hasTriggered() {
        Equip equip = HSUtil.getEquip(hayStack, equipRef);
        Occupied occupiedSchedule = ScheduleUtil.getOccupied(equipRef);
        
        if (occupiedSchedule == null) {
            return false;
        }
        
        boolean isStandaloneEquip = (equip.getMarkers().contains("smartstat") ||
                                     equip.getMarkers().contains("sse") ||
                                     equip.getMarkers().contains("hyperstat"));

        if (isStandaloneEquip && isZoneRequiresPreconditioning(equipRef, occupiedSchedule)) {
            return true;
        } else if (!isStandaloneEquip && ScheduleManager.getInstance().getSystemOccupancy() == PRECONDITIONING){
            return true;
        }
        return false;
        
    }
    
    public static boolean isZoneRequiresPreconditioning(String equipId, Occupied occupied){

        if (occupied != null && occupied.getVacation() != null) {
            return false;
        }
        double currentTemp = CCUHsApi.getInstance().readHisValByQuery("current and air and temp and equipRef == \""+equipId+"\"");
        double desiredTemp = CCUHsApi.getInstance().readHisValByQuery("desired and air and temp and average and equipRef == \""+equipId+"\"");
        double tempDiff = currentTemp - desiredTemp;
        double preconRate = TunerUtil.readTunerValByQuery("standalone and preconditioning and rate and " +
                                                          (tempDiff >= 0 ? "cooling" : "heating"),
                                                          equipId);
    
        /*
         *Initial tempDiff based on average temp is used to determine heating/cooling preconditioning required.
         *Then calculate the absolute tempDiff to determine the preconditioning time.
         */
        if (tempDiff > 0) {
            tempDiff = currentTemp - occupied.getCoolingVal();
        } else {
            tempDiff = occupied.getHeatingVal() - currentTemp;
        }
    
        Log.d(L.TAG_CCU_SCHEDULER,
              "isZonePreconditioningActive = " + preconRate + "," + tempDiff + "," + occupied.getMillisecondsUntilNextChange()
              + "," + currentTemp + "," + desiredTemp + "," + occupied.isPreconditioning());
    
        if(currentTemp == 0) {
            occupied.setPreconditioning(false);
            return false;
        }
    
        if ((occupied.getMillisecondsUntilNextChange() > 0)
            && (tempDiff > 0)
            && (tempDiff * preconRate * 60 * 1000 >= occupied.getMillisecondsUntilNextChange())) {
            occupied.setPreconditioning(true);
            return true;
        }
        return false;
    }
}
