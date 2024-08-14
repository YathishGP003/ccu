package a75f.io.logic.bo.building.schedules;

import org.projecthaystack.HDict;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.constants.WhoFiledConstants;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.occupancy.AutoAway;
import a75f.io.logic.bo.building.schedules.occupancy.AutoForcedOccupied;
import a75f.io.logic.bo.building.schedules.occupancy.ForcedOccupied;
import a75f.io.logic.bo.building.schedules.occupancy.KeyCard;
import a75f.io.logic.bo.building.schedules.occupancy.OccupancyUtil;
import a75f.io.logic.bo.building.schedules.occupancy.Preconditioning;
import a75f.io.logic.bo.building.schedules.occupancy.WindowSensor;
import a75f.io.logic.bo.util.DemandResponseMode;
import a75f.io.logic.tuners.BuildingTunerCache;
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

        CcuLog.d(L.TAG_CCU_SCHEDULER, "updateDesiredTemp: "+updatedOccupancy);
        //Write to Level 8 all the time.
        if (updatedOccupancy == Occupancy.OCCUPIED ||
                updatedOccupancy == Occupancy.PRECONDITIONING ||
                updatedOccupancy == Occupancy.AUTOAWAY ||
                updatedOccupancy == Occupancy.UNOCCUPIED ||
                updatedOccupancy == Occupancy.AUTOFORCEOCCUPIED ||
                updatedOccupancy == Occupancy.VACATION ||
                updatedOccupancy == Occupancy.DEMAND_RESPONSE_OCCUPIED ||
                updatedOccupancy == Occupancy.WINDOW_OPEN ||
                updatedOccupancy == Occupancy.DEMAND_RESPONSE_UNOCCUPIED ||
                updatedOccupancy == Occupancy.KEYCARD_AUTOAWAY) {
            updateScheduleDesiredTemp(schedule, updatedOccupancy);
        }

        //Write to Level 4 when AutoAway
        if (updatedOccupancy == Occupancy.AUTOAWAY || updatedOccupancy == Occupancy.KEYCARD_AUTOAWAY) {
            updateDesiredTempForAutoAway();
        }
        if ((currentOccupancy == Occupancy.DEMAND_RESPONSE_OCCUPIED||currentOccupancy ==
                Occupancy.DEMAND_RESPONSE_UNOCCUPIED) && updatedOccupancy != currentOccupancy) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "Clear Demand response overrides");
            ScheduleUtil.clearTempOverrideAtLevel(equipRef, HayStackConstants.DEMAND_RESPONSE_LEVEL);
        }
        //Write to Level 5 when AutoForcedOccupied
        if (updatedOccupancy == Occupancy.AUTOFORCEOCCUPIED) {

            double forcedOccupiedMinutes = ScheduleUtil.getForcedOccupiedTime(equipRef, hayStack);
            if(!occupancyData.isOccupied && occupancyData.unoccupiedTrigger.ordinal() == UnoccupiedTrigger.KeyCardInput.ordinal()){
                updateDesiredTempAutoForceOccupied(forcedOccupiedMinutes);
                return;
            }

            Date lastOccupancy = getLastOccupancyForMultiModule();
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

        if (currentOccupancy == Occupancy.AUTOFORCEOCCUPIED && updatedOccupancy != currentOccupancy && updatedOccupancy != Occupancy.FORCEDOCCUPIED) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "Clear AutoForcedOccupied overrides");
            ScheduleUtil.clearTempOverrideAtLevel(equipRef, HayStackConstants.FORCE_OVERRIDE_LEVEL);
        }


        if (updatedOccupancy == Occupancy.DEMAND_RESPONSE_OCCUPIED ||
                updatedOccupancy == Occupancy.DEMAND_RESPONSE_UNOCCUPIED) {
            updateDesiredTempForDemandResponse(updatedOccupancy, new OccupancyUtil(hayStack, equipRef), occupancyData);
        }
    }

    private void updateDesiredTempForDemandResponse(Occupancy updatedOccupancy, OccupancyUtil occupancyUtil, OccupancyData occupancyData) {
        Double setback = null;
        Schedule schedule = Schedule.getScheduleByEquipId(equipRef);
        if(!schedule.getMarkers().contains(Tags.FOLLOW_BUILDING))
            setback  = schedule.getUnoccupiedZoneSetback();
        if(setback == null ) {
            setback = CCUHsApi.getInstance().readPointPriorityValByQuery
                    ("zone and unoccupied and setback and roomRef == \"" + HSUtil.getZoneIdFromEquipId(equipRef) + "\"");
        }
        double demandResponseSetback = TunerUtil.readTunerValByQuery("demand and response and setback", equipRef);

        String coolingDtId = getCoolingDesiredTempId();
        String heatingDtId = getHeatingDesiredTempId();
        double buildingLimitMin = BuildingTunerCache.getInstance().getBuildingLimitMin();
        double buildingLimitMax = BuildingTunerCache.getInstance().getBuildingLimitMax();
        double coolingDT = CCUHsApi.readPointPriorityValFromOffset(coolingDtId, HayStackConstants.AUTO_AWAY_LEVEL);
        double heatingDT = CCUHsApi.readPointPriorityValFromOffset(heatingDtId, HayStackConstants.AUTO_AWAY_LEVEL);

        CcuLog.i(L.TAG_CCU_SCHEDULER, "Demand response setback value: " + demandResponseSetback +
                "Cooling desired temp: "+coolingDT + "Heating desired temp: "+heatingDT + "updated occupancy: "+updatedOccupancy
                +"Setback: "+setback);
        if(updatedOccupancy == Occupancy.DEMAND_RESPONSE_OCCUPIED && occupancyData.occupancyDR == Occupancy.WINDOW_OPEN){
            double coolingSetBack = DemandResponseMode.getCoolingSetBack(coolingDT + demandResponseSetback, buildingLimitMax);
            double heatingSetBack = DemandResponseMode.getHeatingSetBack(heatingDT - demandResponseSetback, buildingLimitMin);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, coolingDtId, HayStackConstants.DEMAND_RESPONSE_LEVEL,
                    coolingSetBack, 0, WhoFiledConstants.SCHEDULER_WHO);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, heatingDtId, HayStackConstants.DEMAND_RESPONSE_LEVEL,
                    heatingSetBack, 0, WhoFiledConstants.SCHEDULER_WHO);
        } else if(updatedOccupancy == Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
                occupancyData.occupancyDR == Occupancy.AUTOFORCEOCCUPIED){
            double coolingSetBack = DemandResponseMode.getCoolingSetBack(coolingDT - setback +
                     demandResponseSetback, buildingLimitMax);
            double heatingSetBack = DemandResponseMode.getHeatingSetBack(heatingDT + setback -
                     demandResponseSetback, buildingLimitMin);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, coolingDtId,
                    HayStackConstants.DEMAND_RESPONSE_LEVEL, coolingSetBack, 0, WhoFiledConstants.SCHEDULER_WHO);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, heatingDtId, HayStackConstants.DEMAND_RESPONSE_LEVEL,
                    heatingSetBack, 0, WhoFiledConstants.SCHEDULER_WHO);

        } else if(updatedOccupancy == Occupancy.DEMAND_RESPONSE_OCCUPIED &&
                occupancyData.occupancyDR == Occupancy.FORCEDOCCUPIED) {
            /*For forced occupied we do not clear Forced occupied levels so add DR setback on top of it*/
            double coolingSetBack = DemandResponseMode.getCoolingSetBack(coolingDT +
                    demandResponseSetback, buildingLimitMax);
            double heatingSetBack = DemandResponseMode.getHeatingSetBack(heatingDT -
                    demandResponseSetback, buildingLimitMin);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, coolingDtId,
                    HayStackConstants.DEMAND_RESPONSE_LEVEL, coolingSetBack, 0, WhoFiledConstants.SCHEDULER_WHO);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, heatingDtId, HayStackConstants.DEMAND_RESPONSE_LEVEL,
                    heatingSetBack, 0, WhoFiledConstants.SCHEDULER_WHO);
        } else if (updatedOccupancy == Occupancy.DEMAND_RESPONSE_OCCUPIED && (occupancyData.occupancyDR == Occupancy.AUTOAWAY
                || occupancyData.occupancyDR == Occupancy.KEYCARD_AUTOAWAY)) {
            double autoAwaySetback = TunerUtil.readTunerValByQuery("auto and away and setback", equipRef);
            double coolingSetBack = DemandResponseMode.getCoolingSetBack(coolingDT +
                    demandResponseSetback + autoAwaySetback, buildingLimitMax);
            double heatingSetBack = DemandResponseMode.getHeatingSetBack(heatingDT -
                    demandResponseSetback - autoAwaySetback, buildingLimitMin);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, coolingDtId,
            HayStackConstants.DEMAND_RESPONSE_LEVEL, coolingSetBack, 0, WhoFiledConstants.SCHEDULER_WHO);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, heatingDtId, HayStackConstants.DEMAND_RESPONSE_LEVEL,
                    heatingSetBack, 0, WhoFiledConstants.SCHEDULER_WHO);
        } else if (updatedOccupancy == Occupancy.DEMAND_RESPONSE_UNOCCUPIED &&
                occupancyData.occupancyDR == Occupancy.PRECONDITIONING) {

            double coolingSetBack = DemandResponseMode.getCoolingSetBack(coolingDT +
                    demandResponseSetback - setback, buildingLimitMax);
            double heatingSetBack = DemandResponseMode.getHeatingSetBack(heatingDT -
                    demandResponseSetback + setback, buildingLimitMin);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, coolingDtId,
                    HayStackConstants.DEMAND_RESPONSE_LEVEL, coolingSetBack, 0, WhoFiledConstants.SCHEDULER_WHO);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, heatingDtId, HayStackConstants.DEMAND_RESPONSE_LEVEL,
                    heatingSetBack, 0, WhoFiledConstants.SCHEDULER_WHO);
        } else if (updatedOccupancy == Occupancy.DEMAND_RESPONSE_UNOCCUPIED) {

            double coolingSetBack = DemandResponseMode.getCoolingSetBack(coolingDT +
                    demandResponseSetback, buildingLimitMax);
            double heatingSetBack = DemandResponseMode.getHeatingSetBack(heatingDT -
                    demandResponseSetback, buildingLimitMin);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, coolingDtId,
                    HayStackConstants.DEMAND_RESPONSE_LEVEL, coolingSetBack , 0,
                    WhoFiledConstants.SCHEDULER_WHO);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, heatingDtId,
                    HayStackConstants.DEMAND_RESPONSE_LEVEL, heatingSetBack , 0,
                    WhoFiledConstants.SCHEDULER_WHO);

        } else {
            double coolingSetBack = DemandResponseMode.getCoolingSetBack(coolingDT +
                    demandResponseSetback, buildingLimitMax);
            double heatingSetBack = DemandResponseMode.getHeatingSetBack(heatingDT -
                    demandResponseSetback, buildingLimitMin);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, coolingDtId, HayStackConstants.DEMAND_RESPONSE_LEVEL,
                    coolingSetBack, 0, WhoFiledConstants.SCHEDULER_WHO);
            ScheduleUtil.setDesiredTempAtLevel(hayStack, heatingDtId, HayStackConstants.DEMAND_RESPONSE_LEVEL,
                    heatingSetBack, 0, WhoFiledConstants.SCHEDULER_WHO);
        }
    }

    private void updateScheduleDesiredTemp(Schedule schedule, Occupancy updatedOccupancy) {
        if (schedule == null) {
            CcuLog.i(L.TAG_CCU_SCHEDULER,"Skip updateScheduleDesiredTemp :" +
                    "Schedule is null ");
            return;
        }
        Occupied occupiedSchedule = schedule.getCurrentValues();
        if (occupiedSchedule == null) {
            CcuLog.i(L.TAG_CCU_SCHEDULER,"Skip updateScheduleDesiredTemp :" +
                                         "Invalid occupied values for "+schedule);
            return;
        }

        Double setback = null;
        if(!schedule.getMarkers().contains(Tags.FOLLOW_BUILDING))
            setback  = schedule.getUnoccupiedZoneSetback();
        if(setback == null ) {
            setback = CCUHsApi.getInstance().readPointPriorityValByQuery
                    ("schedulable and zone and unoccupied and setback and roomRef == \"" + HSUtil.getZoneIdFromEquipId(equipRef) + "\"");
        }

        double avgTemp = (occupiedSchedule.getCoolingVal() + occupiedSchedule.getHeatingVal()) / 2.0;

        double heatingDt;
        double coolingDt;
        if (updatedOccupancy == Occupancy.OCCUPIED || updatedOccupancy == Occupancy.PRECONDITIONING
                || updatedOccupancy == Occupancy.AUTOAWAY || updatedOccupancy == Occupancy.KEYCARD_AUTOAWAY ||
                updatedOccupancy == Occupancy.DEMAND_RESPONSE_OCCUPIED) {
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
            String zone = HSUtil.getZoneIdFromEquipId(equipRef);
            double coolingDeadBand = CCUHsApi.getInstance().readPointPriorityValByQuery
                    ("zone and cooling and deadband and roomRef == \""+zone+"\"");
            coolingDesiredTemp = averageDt + coolingDeadBand;
            double heatingDeadBand = CCUHsApi.getInstance().readPointPriorityValByQuery
                    ("zone and heating and deadband and roomRef == \""+zone+"\"");
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
                                                                  "(avg or average) and sp and equipRef == \"" + equipRef +"\"");
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

    private Date getLastOccupancyForMultiModule() {

        Date lastOccupancy = null;
        String zoneId = HSUtil.getZoneIdFromEquipId(equipRef);
        ArrayList<Equip> equips = HSUtil.getEquips(zoneId);
        if ( equips.size() == 1 ) {
            return new OccupancyUtil(hayStack, equipRef).getLastOccupancyDetectionTime();
        }
        for(Equip equip : equips){
            Date equipOccupancyUtil = new OccupancyUtil(hayStack, equip.getId()).getLastOccupancyDetectionTime();
            if(equipOccupancyUtil != null){
                lastOccupancy = equipOccupancyUtil;
                break;
            }
        }
        return lastOccupancy;
    }
    
}
