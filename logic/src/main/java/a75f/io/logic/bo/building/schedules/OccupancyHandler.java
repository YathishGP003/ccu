package a75f.io.logic.bo.building.schedules;

import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Schedule;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.occupancy.AutoAway;
import a75f.io.logic.bo.building.schedules.occupancy.AutoForcedOccupied;
import a75f.io.logic.bo.building.schedules.occupancy.DemandResponse;
import a75f.io.logic.bo.building.schedules.occupancy.NoConditioning;
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
    OccupancyTrigger noconditioning;
    OccupancyTrigger demandResponse;
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
        noconditioning = new NoConditioning(hayStack,equipRef);
        demandResponse = new DemandResponse(hayStack, equipRef);
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
        if (demandResponse.hasTriggered()) {
            return OccupiedTrigger.DemandResponseOccupied;
        }
        if (autoAway.hasTriggered()) {
            return OccupiedTrigger.Autoaway;
        }
        if (emergencyConditioning.hasTriggered()) {
            return OccupiedTrigger.EmergencyConditioning;
        }
        if(noconditioning.hasTriggered()){
            return OccupiedTrigger.NoConditioning;
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
        if (demandResponse.hasTriggered()) {
            return UnoccupiedTrigger.DemandResponseUnoccupied;
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
        if (noconditioning.hasTriggered()) {
            return UnoccupiedTrigger.NoConditioning;
        }
        if (vacation.hasTriggered()) {
            return UnoccupiedTrigger.Vacation;
        }
        return UnoccupiedTrigger.Unoccupied;
    }

    /**
     * Do clean up while transitioning from Unoccupied -> occupied
     */
    public void prepareOccupied() {
        Map<Object, Object> forcedOccupiedLevel = ScheduleUtil.getForcedOccupiedLevel(equipRef);
        if (forcedOccupiedLevel != null) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "Clear forced occupied "+forcedOccupiedLevel);
            ScheduleUtil.clearTempOverrideAtLevel(equipRef, HayStackConstants.FORCE_OVERRIDE_LEVEL);
        }
    }

    /**
     * Do clean up while transitioning from occupied -> unoccupied
     */
    public void prepareUnoccupied() {
        Map<Object, Object> forcedOccupiedLevel = ScheduleUtil.getForcedOccupiedLevel(equipRef);
        if (forcedOccupiedLevel != null) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "Clear forced occupied "+forcedOccupiedLevel);
            ScheduleUtil.clearTempOverrideAtLevel(equipRef, HayStackConstants.FORCE_OVERRIDE_LEVEL);
        }
    }
}
