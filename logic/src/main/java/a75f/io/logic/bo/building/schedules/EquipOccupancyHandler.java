package a75f.io.logic.bo.building.schedules;

import java.util.HashMap;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Schedule;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.occupancy.AutoForcedOccupied;
import a75f.io.logic.bo.building.schedules.occupancy.ForcedOccupied;
import a75f.io.logic.bo.building.schedules.occupancy.OccupancyUtil;
import a75f.io.logic.bo.util.DemandResponseMode;

import static a75f.io.logic.L.TAG_CCU_SCHEDULER;

/**
 * Currently schedules are handled for each equip. Zone level or building level scheduling is done
 * by aggregating equip level schedules. It need not be done that way forever.
 * This could be extended to have Zone level or building level schedule handlers.
 */
public class EquipOccupancyHandler {
    
    private CCUHsApi hayStack;
    private String           equipRef;
    private OccupancyHandler occupancyHandler;
    private OccupancyUtil    occupancyUtil;
    
    private OccupiedTrigger occupiedTrigger;
    private OccupiedTrigger occupiedTriggerDR = OccupiedTrigger.Occupied;
    private UnoccupiedTrigger unoccupiedTrigger;
    private UnoccupiedTrigger unoccupiedTriggerDR = UnoccupiedTrigger.Unoccupied;
    private boolean scheduleOccupied = false;
    
    private Occupancy updatedOccupancy;
    private Occupancy updatedOccupancyDR = Occupancy.NONE;

    public EquipOccupancyHandler(CCUHsApi hayStack, String equipRef) {
        this.hayStack = hayStack;
        this.equipRef = equipRef;
        occupancyUtil = new OccupancyUtil(hayStack, equipRef);
        occupancyHandler = new OccupancyHandler(hayStack, equipRef, occupancyUtil);
    }

    public EquipOccupancyHandler() {
    }

    public String getEquipRef() {
        return equipRef;
    }
    
    public Schedule getSchedule() {
        return null;
    }
    
    /**
     * Handles occupancy transitions and updates desired temps.
     */
    public void updateOccupancy(boolean drActivated) {
        Occupied scheduleOccupancy = ScheduleUtil.getOccupied(equipRef);
        scheduleOccupied = scheduleOccupancy != null ? scheduleOccupancy.isOccupied() : false;
        Occupancy currentOccupancy = occupancyUtil.getCurrentOccupiedMode();
        if (currentOccupancy == Occupancy.OCCUPIED && !scheduleOccupied) {
            occupancyHandler.prepareUnoccupied();
        }

        if(isUnoccupiedToOccupiedTransitionRequired(currentOccupancy, scheduleOccupied,
                        ForcedOccupied.isZoneForcedOccupied(equipRef),
                        AutoForcedOccupied.isZoneInAutoForcedOccupied(occupancyUtil))) {
            occupancyHandler.prepareOccupied();
        }
        if (scheduleOccupied) {
            occupiedTrigger = occupancyHandler.getOccupiedTrigger(false);
            updatedOccupancy = occupiedTrigger.toOccupancy();
            if(drActivated) {
                occupiedTriggerDR = occupancyHandler.getOccupiedTrigger(true);
                if (occupiedTriggerDR != null) {
                    updatedOccupancyDR = occupiedTriggerDR.toOccupancy();
                }
            }
            CcuLog.i(TAG_CCU_SCHEDULER, "updateOccupancy : occupiedTrigger "+occupiedTrigger);
        } else {
            unoccupiedTrigger = occupancyHandler.getUnoccupiedTrigger(false);
            updatedOccupancy = unoccupiedTrigger.toOccupancy();
            CcuLog.i(TAG_CCU_SCHEDULER, "updateOccupancy : unoccupiedTrigger "+unoccupiedTrigger);
            if(drActivated) {
                unoccupiedTriggerDR = occupancyHandler.getUnoccupiedTrigger(true);
                if (unoccupiedTriggerDR != null) {
                    updatedOccupancyDR = unoccupiedTriggerDR.toOccupancy();
                }
            }
        }
    }

    /**
     * Retrieves the occupancy mode during Demand Response.
     *
     * This method obtains the occupancy mode based on the demand response status
     * by invoking the occupancy handler.
     *
     * @return The OccupiedTrigger instance representing the current Demand Response occupancy mode.
     */
    public OccupiedTrigger getDRModeOccupiedTrigger() {
        return occupiedTriggerDR;
    }
    public UnoccupiedTrigger getDRModeUnoccupiedTrigger() {
        return unoccupiedTriggerDR;
    }

    public Occupancy getUpdatedOccupancy() {
        return updatedOccupancy;
    }
    
    public Occupancy getCurrentOccupiedMode() {
        return occupancyUtil.getCurrentOccupiedMode();
    }
    
    public OccupiedTrigger getCurrentOccupiedTrigger() {
        return occupiedTrigger;
    }
    
    public UnoccupiedTrigger getCurrentUnoccupiedTrigger() {
        return unoccupiedTrigger;
    }
    
    public boolean isScheduleOccupied() {
        return scheduleOccupied;
    }
    
    private void clearDesiredTempAutoAwayLevel() {
        HashMap<Object, Object> coolingDT = hayStack.readEntity("desired and cooling and temp and " +
                                                                "equipRef == \"" + equipRef +"\"");
        if (!coolingDT.isEmpty()) {
            hayStack.clearPointArrayLevel(Objects.requireNonNull(coolingDT.get("id")).toString(),
                                          HayStackConstants.AUTO_AWAY_LEVEL, false);
        }
        HashMap<Object, Object> heatingDT = hayStack.readEntity("desired and cooling and temp and " +
                                                                "equipRef == \"" + equipRef +"\"");
        if (!heatingDT.isEmpty()) {
            hayStack.clearPointArrayLevel(Objects.requireNonNull(coolingDT.get("id")).toString(),
                                          HayStackConstants.AUTO_AWAY_LEVEL,
                                          false);
        }
    }
    
    public void writeOccupancyMode(Occupancy occupancy) {
        occupancyUtil.setOccupancyMode(occupancy);
    }
    
    
    
    public void handleAutoForceOccupied() {
        //Occupied occStatus = ScheduleUtil.getOccupied(equipRef);
        Occupancy occupancyMode = occupancyUtil.getCurrentOccupiedMode();
        double forcedOccupiedMins = ScheduleUtil.getForcedOccupiedTime(equipRef, hayStack);
        if (forcedOccupiedMins == 0) {
            CcuLog.d(L.TAG_CCU_SCHEDULER, "handleForcedOccupied disabled, forcedOccupiedMins: "+forcedOccupiedMins);
            return;
        }
        boolean occupancySensor = CCUHsApi.getInstance().readHisValByQuery(
            "occupancy and sensor and equipRef  == \"" + equipRef + "\"") > 0;
    
        if (occupancySensor) {
            if (occupancyMode != Occupancy.AUTOFORCEOCCUPIED) {
                //occupancyUtil.setOccupancyMode(Occupancy.AUTOFORCEOCCUPIED);
                CcuLog.i(TAG_CCU_SCHEDULER, "handleAutoForceOccupied: AutoForcedOccupied active");
                updatedOccupancy = Occupancy.AUTOFORCEOCCUPIED;
                //updateDesiredTempForceOccupied(forcedOccupiedMins);
            } else {
                CcuLog.i(TAG_CCU_SCHEDULER, "handleAutoForceOccupied: AutoForcedOccupied continue");
                // We are already in force occupy
                // Just update with latest
                //checkAndUpdateDesiredTempForcedOccupied(forcedOccupiedMins); //TODO-Schedules - Is this required ?
            }
        } else {
            double temporaryHoldTime = ScheduleUtil.getTemporaryHoldExpiry(HSUtil.getEquipInfo(equipRef));
            if (occupancyMode == Occupancy.AUTOFORCEOCCUPIED && System.currentTimeMillis() > temporaryHoldTime) {
                CcuLog.i(TAG_CCU_SCHEDULER, "handleAutoForceOccupied: AutoForcedOccupied reset");
                updatedOccupancy = Occupancy.UNOCCUPIED;
                //resetForceOccupied();
            }
        }
    }
    public boolean isUnoccupiedToOccupiedTransitionRequired(Occupancy currentOccupancy,
                                                            boolean scheduleOccupied,
                                                            boolean zoneForcedOccupied,
                                                            boolean zoneInAutoForcedOccupied) {
        return (scheduleOccupied) && (currentOccupancy == Occupancy.AUTOFORCEOCCUPIED ||
                currentOccupancy == Occupancy.FORCEDOCCUPIED ||
                currentOccupancy == Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
                        (zoneForcedOccupied || zoneInAutoForcedOccupied));
    }

}
