package a75f.io.logic.bo.building.schedules;

import android.util.Log;

import org.projecthaystack.HDict;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.Date;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.constants.WhoFiledConstants;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.occupancy.OccupancyUtil;
import a75f.io.logic.tuners.TunerUtil;

public class EquipScheduleHandler implements Schedulable {
    
    CCUHsApi         hayStack;
    String           equipRef;
    
    public EquipScheduleHandler(CCUHsApi hayStack, String equipRef) {
        this.hayStack = hayStack;
        this.equipRef = equipRef;
    }
    
    public Schedule getSchedule() {
        return null;
    }
    
    public void updateDesiredTemp(Occupancy currentOccupancy, Occupancy updatedOccupancy, Schedule schedule,OccupancyData occupancyData) {
        //This is required to avoid zone immediately switching to autoway due lack of occupancy detection.
        if (updatedOccupancy == Occupancy.OCCUPIED && currentOccupancy != updatedOccupancy) {
            initOccupancyDetection(true);
        }
    
        //This is required to avoid an existing occupancy detection triggering "AutoForcedOccupied' during a transition
        //from occupied to unoccupied.
        if ((updatedOccupancy == Occupancy.UNOCCUPIED || updatedOccupancy == Occupancy.VACATION)
                 && (currentOccupancy == Occupancy.OCCUPIED || currentOccupancy == Occupancy.AUTOAWAY)) {
            initOccupancyDetection(false);
        }

        Log.i("CCU_SCHEDULER", "updateDesiredTemp: "+updatedOccupancy);
        //Write to Level 8 all the time.
        if (updatedOccupancy == Occupancy.OCCUPIED ||
            updatedOccupancy == Occupancy.PRECONDITIONING ||
            updatedOccupancy == Occupancy.AUTOAWAY ||
            updatedOccupancy == Occupancy.UNOCCUPIED ||
            updatedOccupancy == Occupancy.AUTOFORCEOCCUPIED ||
            updatedOccupancy == Occupancy.VACATION ) {
            updateScheduleDesiredTemp(schedule, updatedOccupancy);
        }
        
        //Write to Level 3 when AutoAway
        if (updatedOccupancy == Occupancy.AUTOAWAY ||updatedOccupancy == Occupancy.KEYCARD_AUTOAWAY) {
            updateDesiredTempForAutoAway();
        }


        
        //Write to Level 4 when AutoForcedOccupied
        if (updatedOccupancy == Occupancy.AUTOFORCEOCCUPIED) {

            double forcedOccupiedMinutes = ScheduleUtil.getForcedOccupiedTime(equipRef, hayStack);
            if(!occupancyData.isOccupied && occupancyData.unoccupiedTrigger.ordinal() == UnoccupiedTrigger.KeyCardInput.ordinal()){
                updateDesiredTempAutoForceOccupied(forcedOccupiedMinutes);
                return;
            }

            Date lastOccupancy = new OccupancyUtil(hayStack, equipRef).getLastOccupancyDetectionTime();
            if (lastOccupancy == null) {
                return;
            }

            if (forcedOccupiedMinutes > 0) {
                double currentExpiry = ScheduleUtil.getTemporaryHoldExpiry(equipRef);
                CcuLog.i(L.TAG_CCU_SCHEDULER, "AutoForcedOccupied OccDet time "+lastOccupancy.getTime());
                double timeToForcedOccupiedExpiry = lastOccupancy.getTime() + forcedOccupiedMinutes * 60 * 1000;
    
                if (timeToForcedOccupiedExpiry > currentExpiry) {
                    CcuLog.i(L.TAG_CCU_SCHEDULER, "Update AutoForcedOccupied time currentExpiry "+currentExpiry+
                                                  " timeToForcedOccupiedExpiry "+timeToForcedOccupiedExpiry);
                    updateDesiredTempAutoForceOccupied(forcedOccupiedMinutes);
                }
            } else {
                CcuLog.i(L.TAG_CCU_SCHEDULER,
                         "AutoForcedOccupied temp not updated forcedOccupiedMinutes "+forcedOccupiedMinutes);
            }
            
        }

        if ((currentOccupancy == Occupancy.AUTOAWAY||currentOccupancy == Occupancy.KEYCARD_AUTOAWAY) && updatedOccupancy != currentOccupancy) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "Clear AutoAway overrides");
            ScheduleUtil.clearTempOverrideAtLevel(equipRef, HayStackConstants.AUTO_AWAY_LEVEL);
        }
        
        if (currentOccupancy == Occupancy.AUTOFORCEOCCUPIED && updatedOccupancy != currentOccupancy) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "Clear AutoForcedOccupied overrides");
            ScheduleUtil.clearTempOverrideAtLevel(equipRef, HayStackConstants.FORCE_OVERRIDE_LEVEL);
        }
    }
    
    private void updateScheduleDesiredTemp(Schedule schedule, Occupancy updatedOccupancy) {
        Occupied occupiedSchedule = schedule.getCurrentValues();
        if (occupiedSchedule == null) {
            CcuLog.i(L.TAG_CCU_SCHEDULER,"Skip updateScheduleDesiredTemp :" +
                                         "Invalid occupied values for "+schedule);
            return;
        }
        double setback = TunerUtil.readTunerValByQuery("unoccupied and setback", equipRef);
    
        double avgTemp = (occupiedSchedule.getCoolingVal() + occupiedSchedule.getHeatingVal()) / 2.0;

        double heatingDt;
        double coolingDt;
        if (updatedOccupancy == Occupancy.OCCUPIED || updatedOccupancy == Occupancy.PRECONDITIONING
                || updatedOccupancy == Occupancy.AUTOAWAY || updatedOccupancy == Occupancy.KEYCARD_AUTOAWAY) {
            coolingDt = occupiedSchedule.getCoolingVal();
            heatingDt = occupiedSchedule.getHeatingVal();
        } else {
            coolingDt = occupiedSchedule.getCoolingVal() + setback;
            heatingDt = occupiedSchedule.getHeatingVal() - setback;
        }
    
        String coolingDtId = getCoolingDesiredTempId();
        String heatingDtId = getHeatingDesiredTempId();
        String averageDtId = getAverageDesiredTempId();

        // Donot set temperature if null
        if(coolingDtId != null && heatingDtId != null && averageDtId != null){
            ScheduleUtil.setDesiredTempAtLevel(hayStack, coolingDtId, HayStackConstants.CCU_USER_WRITE_LEVEL,
                    coolingDt, 0, WhoFiledConstants.SCHEDULER_WHO);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, heatingDtId, HayStackConstants.CCU_USER_WRITE_LEVEL,
                    heatingDt, 0, WhoFiledConstants.SCHEDULER_WHO);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, averageDtId, HayStackConstants.CCU_USER_WRITE_LEVEL, avgTemp
                    , 0, WhoFiledConstants.SCHEDULER_WHO);
        }

    }
    
    private void updateDesiredTempForAutoAway() {
        double autoAwaySetback = TunerUtil.readTunerValByQuery("auto and away and setback",equipRef);
        CcuLog.i(L.TAG_CCU_SCHEDULER, "autoAwaySetback value: " + autoAwaySetback);
        String coolingDtId = getCoolingDesiredTempId();
        String heatingDtId = getHeatingDesiredTempId();

        double coolingDT = hayStack.readPointPriorityValFromOffset(coolingDtId, HayStackConstants.FORCE_OVERRIDE_LEVEL);
        double heatingDT = hayStack.readPointPriorityValFromOffset(heatingDtId, HayStackConstants.FORCE_OVERRIDE_LEVEL);
    
        ScheduleUtil.setDesiredTempAtLevel(hayStack, coolingDtId, HayStackConstants.AUTO_AWAY_LEVEL ,
                              coolingDT + autoAwaySetback, 0, getWhoForPointWrite());
    
        ScheduleUtil.setDesiredTempAtLevel(hayStack, heatingDtId, HayStackConstants.AUTO_AWAY_LEVEL ,
                              heatingDT - autoAwaySetback, 0, getWhoForPointWrite());
    }
    private String getWhoForPointWrite(){
        HDict equipHDict = hayStack.readHDictById(equipRef);
        if(equipHDict.has(Tags.HYPERSTAT)){
           return WhoFiledConstants.HYPERSTAT_OCC_SENSOR_WHO;
        }
        else if(equipHDict.has(Tags.HELIO_NODE)){
            return WhoFiledConstants.HELIONODE_OCC_SENSOR_WHO;
        }
        else if(equipHDict.has(Tags.SMART_NODE)){
            return WhoFiledConstants.SMARTNODE_OCC_SENSOR_WHO;
        }
        else if(equipHDict.has(Tags.OTN)){
            return WhoFiledConstants.OTN_OCC_SENSOR_WHO;
        }
        else if(equipHDict.has(Tags.SMART_STAT)){
            return WhoFiledConstants.SMARTSTAT_OCC_SENSOR_WHO;
        }
        return WhoFiledConstants.OCCUPANCY_SENSOR_WHO;
    }

    private void updateDesiredTempAutoForceOccupied(double forcedOccupiedMins) {

        Occupied occ = ScheduleManager.getInstance().getOccupiedModeCache(HSUtil.getZoneIdFromEquipId(equipRef));

        double heatingDesiredTemp;
        double coolingDesiredTemp;

        if (occ != null) {
            heatingDesiredTemp = occ.getHeatingVal();
            coolingDesiredTemp = occ.getCoolingVal();
        } else {
            double averageDt = hayStack.readDefaultVal("average and desired and temp and equipRef == \""+equipRef+"\"");
            double coolingDeadBand = TunerUtil.readTunerValByQuery("cooling and deadband and base", equipRef, hayStack);
            coolingDesiredTemp = averageDt + coolingDeadBand;
            double heatingDeadBand = TunerUtil.readTunerValByQuery("heating and deadband and base", equipRef, hayStack);
            heatingDesiredTemp = averageDt - heatingDeadBand;
        }

        String coolingPointId = getCoolingDesiredTempId();
        hayStack.pointWrite(HRef.copy(coolingPointId),
                            HayStackConstants.FORCE_OVERRIDE_LEVEL,
                            getWhoForPointWrite(),
                            HNum.make(coolingDesiredTemp) ,
                            HNum.make(forcedOccupiedMins * 60 * 1000, "ms"));
        hayStack.writeHisValById(coolingPointId, HSUtil.getPriorityVal(coolingPointId, hayStack));
    
        String heatingPointId = getHeatingDesiredTempId();
        hayStack.pointWrite(HRef.copy(heatingPointId),
                            HayStackConstants.FORCE_OVERRIDE_LEVEL,
                            getWhoForPointWrite(),
                            HNum.make(heatingDesiredTemp) ,
                            HNum.make(forcedOccupiedMins * 60 * 1000, "ms"));
        hayStack.writeHisValById(heatingPointId, HSUtil.getPriorityVal(heatingPointId, hayStack));
    
        CcuLog.i(L.TAG_CCU_SCHEDULER, "AutoForceOccupied updated cooling dt "+HSUtil.getPriorityLevel(coolingPointId,
                                HayStackConstants.FORCE_OVERRIDE_LEVEL));
        CcuLog.i(L.TAG_CCU_SCHEDULER, "AutoForceOccupied updated heating dt "+HSUtil.getPriorityLevel(heatingPointId,
                                                                                          HayStackConstants.FORCE_OVERRIDE_LEVEL));
    }
    
    private void checkAndUpdateDesiredTempForcedOccupied(double forceOccupiedMins) {
        HashMap<Object, Object> coolingDtPoint = hayStack.readEntity("temp and desired and " +
                                                                     "cooling and sp and equipRef == \""+equipRef+"\"");
        
        if (!coolingDtPoint.isEmpty()) {
            HashMap<Object, Object> overrideLevel = HSUtil.getPriorityLevel(coolingDtPoint.get("id").toString(),
                                                                            HayStackConstants.FORCE_OVERRIDE_LEVEL,
                                                                            hayStack);
            if (!overrideLevel.isEmpty()) {
                CcuLog.d(L.TAG_CCU_SCHEDULER, "updateDesiredTempForcedOccupied not required coolingDt: " + overrideLevel);
                return;
            }
            updateDesiredTempAutoForceOccupied(forceOccupiedMins);
        }
    }
    
    private String getCoolingDesiredTempId() {
        HashMap<Object, Object> desiredTemp = hayStack.readEntity("temp and desired and " +
                                            "cooling and sp and equipRef == \"" + equipRef + "\"");
        if (desiredTemp.isEmpty()) {
            return null;
        }
        return desiredTemp.get("id").toString();
    }
    
    private String getHeatingDesiredTempId() {
        HashMap<Object, Object> desiredTemp = hayStack.readEntity("temp and desired and " +
                                                                  "heating and sp and equipRef == \"" + equipRef + "\"");
        if (desiredTemp.isEmpty()) {
            return null;
        }
        return desiredTemp.get("id").toString();
    }
    
    private String getAverageDesiredTempId() {
        HashMap<Object, Object> desiredTemp = hayStack.readEntity("temp and desired and " +
                                                                  "average and sp and equipRef == \"" + equipRef +"\"");
        if (desiredTemp.isEmpty()) {
            return null;
        }
        return desiredTemp.get("id").toString();
    }
    
    private void initOccupancyDetection(boolean val) {
        HashMap<Object, Object> occupancyDetection = hayStack.readEntity(
            "occupancy and detection and equipRef  == \"" + equipRef + "\"");
        if (!occupancyDetection.isEmpty()) {
            hayStack.writeHisValueByIdWithoutCOV(occupancyDetection.get("id").toString(), val ? 1.0 : 0);
        } else {
            CcuLog.e(L.TAG_CCU_SCHEDULER, "Occupancy detection point does not exist" );
        }
    }
    
}
