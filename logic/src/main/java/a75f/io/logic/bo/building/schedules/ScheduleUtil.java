package a75f.io.logic.bo.building.schedules;

import static a75f.io.api.haystack.util.TimeUtil.getEndHour;
import static a75f.io.api.haystack.util.TimeUtil.getEndMinute;
import static a75f.io.logic.L.TAG_CCU_SCHEDULER;
import static a75f.io.logic.bo.building.schedules.Occupancy.AUTOAWAY;
import static a75f.io.logic.bo.building.schedules.Occupancy.AUTOFORCEOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.DEMAND_RESPONSE_OCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.DEMAND_RESPONSE_UNOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.EMERGENCY_CONDITIONING;
import static a75f.io.logic.bo.building.schedules.Occupancy.FORCEDOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.KEYCARD_AUTOAWAY;
import static a75f.io.logic.bo.building.schedules.Occupancy.NO_CONDITIONING;
import static a75f.io.logic.bo.building.schedules.Occupancy.OCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.VACATION;
import a75f.io.logic.schedule.Marker;

import android.os.StrictMode;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.projecthaystack.HDict;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.L;
import a75f.io.logic.schedule.SpecialSchedule;
import a75f.io.logic.tuners.TunerUtil;

public class ScheduleUtil {
    
    public static final String ACTION_STATUS_CHANGE = "status_change";
    
    public static Occupied getOccupied(String equipRef) {
        String zoneId = HSUtil.getZoneIdFromEquipId(equipRef);
        return ScheduleManager.getInstance().getOccupiedModeCache(zoneId);
    }
    
    public static boolean isOccupied(String equipRef) {
        Occupied occ = getOccupied(equipRef);
        return occ != null && occ.isOccupied();
    }
    
    public static Schedule getActiveVacation(List<Schedule> activeVacationSchedules) {
        
        if(activeVacationSchedules == null)
            return null;
        
        for(Schedule schedule : activeVacationSchedules) {
            if(schedule.isVacation() && schedule.isActiveVacation()) {
                return schedule;
            }
        }
        return null;
    }
    
    public static double getForcedOccupiedTime(String equipRef, CCUHsApi hayStack) {
        return TunerUtil.readTunerValByQuery("forced and occupied and time", equipRef, hayStack);
    }
    
    public static boolean areAllZonesInVacation(Map<String, OccupancyData> equipOccupancy) {
        for (OccupancyData occupancyData : equipOccupancy.values()) {
            if (occupancyData.occupancy != VACATION) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean areAllZonesInAutoAway(Map<String, OccupancyData> equipOccupancy) {
        for (OccupancyData occupancyData : equipOccupancy.values()) {
            if (occupancyData.occupancy != AUTOAWAY) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean areAllZonesKeyCardAutoAway(Map<String, OccupancyData> equipOccupancy) {
        for (OccupancyData occupancyData : equipOccupancy.values()) {
            if (occupancyData.occupancy != KEYCARD_AUTOAWAY) {
                return false;
            }
        }
        return true;
    }
    public static boolean isAnyZoneInDemandResponse(Map<String, OccupancyData> equipOccupancy) {
        for (OccupancyData occupancyData : equipOccupancy.values()) {
            if (occupancyData.occupancy == DEMAND_RESPONSE_UNOCCUPIED ||
                    occupancyData.occupancy == DEMAND_RESPONSE_OCCUPIED) {
                return true;
            }
        }
        return false;
    }

    public static Occupancy getDemandResponseMode(Map<String, OccupancyData> equipOccupancy) {
        boolean unoccupiedFound = false;
        for (OccupancyData occupancyData : equipOccupancy.values()) {
            if (occupancyData.occupancy == DEMAND_RESPONSE_OCCUPIED) {
                // Highest priority, return immediately
                return DEMAND_RESPONSE_OCCUPIED;
            } else if (occupancyData.occupancy == DEMAND_RESPONSE_UNOCCUPIED) {
                // Lower priority, mark that we found it
                unoccupiedFound = true;
            }
        }
        // If occupied wasn't found but unoccupied was, return that
        if (unoccupiedFound) {
            return DEMAND_RESPONSE_UNOCCUPIED;
        }
        return null;
    }
    /**
     * Zone is in AutoAway if at least one equip is in AutoAway.
     */
    public static boolean isZoneAutoAway(String roomRef, CCUHsApi hayStack, Map<String, OccupancyData> equipOccupancy) {
        ArrayList<HashMap<Object, Object>> equipsInZone = hayStack
                                                              .readAllEntities("equip and roomRef == \""+roomRef+"\"");

        for (HashMap<Object, Object> equip : equipsInZone) {
            OccupancyData occupancyData = equipOccupancy.get(equip.get("id").toString());
            if (occupancyData == null) {
                continue;
            }
            if (occupancyData.occupancy == AUTOAWAY) {
                CcuLog.i(TAG_CCU_SCHEDULER, "Zone " + roomRef + " is in AutoAway " + " via " + equip);
                return true;
            }
        }
        return false;
    }
    
    public static boolean areAllZonesForcedOccupied(Map<String, OccupancyData> equipOccupancy) {
        for (OccupancyData occupancyData : equipOccupancy.values()) {
            if (occupancyData.occupancy != FORCEDOCCUPIED) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean isAnyZoneForcedOccupied(Map<String, OccupancyData> equipOccupancy) {
        for (OccupancyData occupancyData : equipOccupancy.values()) {
            if (occupancyData.occupancy == FORCEDOCCUPIED) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isAnyZoneEmergencyConditioning(Map<String, OccupancyData> equipOccupancy) {
        for (OccupancyData occupancyData : equipOccupancy.values()) {
            if (occupancyData.occupancy == EMERGENCY_CONDITIONING) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isAnyZoneAutoForcedOccupied(Map<String, OccupancyData> equipOccupancy) {
        for (OccupancyData occupancyData : equipOccupancy.values()) {
            if (occupancyData.occupancy == AUTOFORCEOCCUPIED) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean areAllZonesAutoForcedOccupied(Map<String, OccupancyData> equipOccupancy) {
        for (OccupancyData occupancyData : equipOccupancy.values()) {
            if (occupancyData.occupancy != AUTOFORCEOCCUPIED) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean isAnyZoneAutoAway(Map<String, OccupancyData> equipOccupancy) {
        for (OccupancyData occupancyData : equipOccupancy.values()) {
            if (occupancyData.occupancy == AUTOAWAY) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isAnyZoneOccupiedOrAutoAway(Map<String, OccupancyData> equipOccupancy) {
        for (OccupancyData occupancyData : equipOccupancy.values()) {
            if (occupancyData.occupancy == OCCUPIED || occupancyData.occupancy == AUTOAWAY) {
                return true;
            }
        }
        return false;
    }
    
    public static Occupied getCurrentOccupied(HashMap<String, Occupied> occupiedHashMap) {
        Occupied currOccupied = null;
        for (Map.Entry occEntry : occupiedHashMap.entrySet()) {
            String roomRef = occEntry.getKey().toString();
            if (!isAHUServedZone(roomRef, CCUHsApi.getInstance())) {
                CcuLog.i(TAG_CCU_SCHEDULER, " Zone does not contain AHU served equip "+roomRef);
                continue;
            }
            Occupied occ = (Occupied) occEntry.getValue();
            //CcuLog.i(TAG_CCU_SCHEDULER, " Occupied for "+roomRef+" "+occ.toString());
            if (occ.isOccupied()/* && !isZoneAutoAway(roomRef, CCUHsApi.getInstance(), equipOccupancy)*/) {
                Schedule.Days occDay = occ.getCurrentlyOccupiedSchedule();
                if (currOccupied == null || occDay.getEthh() > currOccupied.getCurrentlyOccupiedSchedule().getEthh()
                    || (occDay.getEthh() == currOccupied.getCurrentlyOccupiedSchedule().getEthh() &&
                        occDay.getEtmm() > currOccupied.getCurrentlyOccupiedSchedule().getEtmm()) ) {
                    currOccupied = occ;
                }
            }
        }
        return currOccupied;
    }
    
    public static Occupied getNextOccupied(HashMap<String, Occupied> occupiedHashMap) {
        Occupied next = null;
        long millisToOccupancy = 0;
        for (Map.Entry<String, Occupied> occEntry : occupiedHashMap.entrySet()) {
            String roomRef = occEntry.getKey();
            if (!isAHUServedZone(roomRef, CCUHsApi.getInstance())) {
                continue;
            }
            Occupied occ = occEntry.getValue();
            if (millisToOccupancy == 0) {
                millisToOccupancy = occ.getMillisecondsUntilNextChange();
                next = occ;
            } else if (occ.getMillisecondsUntilNextChange() < millisToOccupancy) {
                millisToOccupancy = occ.getMillisecondsUntilNextChange();
                next = occ;
            }
        }
        return next;
    }
    
    /**
     * Checks if the zone has a system-served equip like dab/vav/bpos/ti/dualduct etc.
     * This should be more explicit so that this method can be edited when a new system-module is added.
     */
    public static boolean isAHUServedZone(String roomRef, CCUHsApi hayStack) {
        ArrayList<HashMap<Object, Object>> equips = hayStack
                                                        .readAllEntities("equip and roomRef == \""+roomRef+"\"");
        
        for (HashMap<Object, Object> equip : equips) {
            if (isAHUServedEquip(equip)) {
                return true;
            }
        }
        
        return false;
        
    }
    
    public static boolean isAHUServedEquip(HashMap<Object, Object> equip) {
        return (equip.containsKey("vav") || equip.containsKey("dab") || equip.containsKey("dualDuct") ||
                equip.containsKey("ti") || equip.containsKey("otn") );
    }
    
    public static Schedule getActiveSystemVacation() {
        List<Schedule> activeSystemVacation = CCUHsApi.getInstance().getSystemSchedule(true);
        return ScheduleUtil.getActiveVacation(activeSystemVacation);
    }
    
    public static boolean isCurrentMinuteUnderSpecialSchedule(Set<Schedule.Days> combinedSpecialSchedules){
        boolean isSpecialScheduleMin = false;
        for(Schedule.Days specialSchedule : combinedSpecialSchedules){
            DateTime dateTime = new DateTime();
            DateTime beginTime = new DateTime().withTime(specialSchedule.getSthh(), specialSchedule.getStmm(),0,0);
            DateTime endTime = new DateTime().withTime(getEndHour(specialSchedule.getEthh()), getEndMinute(specialSchedule.getEthh(), specialSchedule.getEtmm()),59,0);
            if(specialSchedule.getDay() == dateTime.getDayOfWeek()-1 &&
                    dateTime.getMinuteOfDay() >= beginTime.getMinuteOfDay() &&
                    dateTime.getMinuteOfDay() <= endTime.getMinuteOfDay()){
                isSpecialScheduleMin = true;
                break;
            }
        }
        return isSpecialScheduleMin;
    }
    
    /*This method will delete expired vacation from the internal portal as well, done by Aniket*/
    public static void deleteExpiredVacation(){
        DateTime now = new DateTime();
        //by below check, outdated vacations are deleted once in an hour
        if((now.getMinuteOfDay() % 60) != 0){
            return;
        }
        ArrayList<Schedule> getAllVacationSchedules = CCUHsApi.getInstance().getAllVacationSchedules();
        for (int i=0; i<getAllVacationSchedules.size(); i++){
            if (getAllVacationSchedules.get(i).getEndDate().getMillis() < System.currentTimeMillis()){
                CCUHsApi.getInstance().deleteEntity("@" + getAllVacationSchedules.get(i).getId());
                CCUHsApi.getInstance().scheduleSync();
            }
        }
    }
    
    public static void deleteExpiredSpecialSchedules(){
        DateTime now = new DateTime();
        //by below check, outdated special schedules are deleted once in an hour
        if((now.getMinuteOfDay() % 60) != 0){
            return;
        }
        List<HashMap<Object, Object>> specialScheduleList = CCUHsApi.getInstance().getAllSpecialSchedules();
        for(HashMap<Object, Object> specialSchedule : specialScheduleList){
            HDict range = (HDict) specialSchedule.get(Tags.RANGE);
            int endHour = Schedule.getInt(range.get(Tags.ETHH).toString());
            endHour  = endHour == 24 ? 23 : endHour;
            int endMin = Schedule.getInt(range.get(Tags.ETMM).toString());
            endMin = Schedule.getInt(range.get(Tags.ETHH).toString()) == 24 ? 59 : endMin;
            int endSec = Schedule.getInt(range.get(Tags.ETHH).toString()) == 24 ? 59 : 0;
            DateTime endDateTime = SpecialSchedule.SS_DATE_TIME_FORMATTER
                    .parseDateTime(range.get(Tags.ETDT).toString())
                    .withHourOfDay(endHour)
                    .withMinuteOfHour(endMin)
                    .withSecondOfMinute(endSec);
            if(endDateTime.getMillis() < System.currentTimeMillis()){
                CCUHsApi.getInstance().deleteEntity(specialSchedule.get(Tags.ID).toString());
                CCUHsApi.getInstance().scheduleSync();
            }
        }
    }
    
    //TODO-Schedules - revisit usage in ScheduleProcessJon.getZoneStatus
    public static void clearTempOverrides(String equipId) {
        if (BuildConfig.DEBUG)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        HashMap coolDT = CCUHsApi.getInstance().read("point and desired and cooling and temp and equipRef == \"" + equipId + "\"");
        HashMap heatDT = CCUHsApi.getInstance().read("point and desired and heating and temp and equipRef == \"" + equipId + "\"");
        HashMap averageDT = CCUHsApi.getInstance().read("point and desired and average and temp and equipRef == \"" + equipId + "\"");
        
        CCUHsApi.getInstance().pointWrite(HRef.copy(coolDT.get("id").toString()), 4, "manual", HNum.make(0), HNum.make(1, "ms"));
        CCUHsApi.getInstance().pointWrite(HRef.copy(heatDT.get("id").toString()), 4, "manual", HNum.make(0), HNum.make(1, "ms"));
        if (!averageDT.isEmpty()) {
            CCUHsApi.getInstance().pointWrite(HRef.copy(averageDT.get("id").toString()), 4, "manual", HNum.make(0), HNum.make(1, "ms"));
        }
        //systemOccupancy = UNOCCUPIED;
    }
    
    public static void clearTempOverrideAtLevel(String equipId, int level) {
        
        HashMap<Object, Object> coolDT = CCUHsApi.getInstance().readEntity("point and desired and cooling and " +
                                                                           "temp and equipRef == \"" + equipId + "\"");
        HashMap<Object, Object> heatDT = CCUHsApi.getInstance().read("point and desired and heating and " +
                                                                     "temp and equipRef == \"" + equipId + "\"");
        HashMap averageDT =CCUHsApi.getInstance().read("point and desired and (average or avg) and temp and equipRef == \"" + equipId + "\"");
        
        CCUHsApi.getInstance().clearPointArrayLevel(coolDT.get("id").toString(), level, false);
        CCUHsApi.getInstance().clearPointArrayLevel(heatDT.get("id").toString(), level, false);
        
        if (!averageDT.isEmpty()) {
            CCUHsApi.getInstance().clearPointArrayLevel(averageDT.get("id").toString(), level, false);
        }
    }
    public static long getSystemTemporaryHoldExpiry() {
        long thExpiry = 0;
        for (Floor f: HSUtil.getFloors())
        {
            for (Zone z : HSUtil.getZones(f.getId()))
            {
                ArrayList<HashMap<Object, Object>> equips = CCUHsApi.getInstance().readAllEntities("equip and roomRef == \""+z.getId()+"\"");
                if(equips.isEmpty()) continue;
                Equip q = HSUtil.getEquipFromZone(z.getId());
                if(q.getMarkers().contains("dab") || q.getMarkers().contains("dualDuct")
                   || q.getMarkers().contains("vav" ) || q.getMarkers().contains("ti")
                   || q.getMarkers().contains("otn") || q.getMarkers().contains("sse")) {
                    if (getTemporaryHoldExpiry(q) > thExpiry) {
                        thExpiry = getTemporaryHoldExpiry(q);
                    }
                }
            }
        }
        
        //Logging temporary hold expiry for debugging.
        if (thExpiry > 0)
        {
            CcuLog.d(TAG_CCU_SCHEDULER, "thExpiry: " + thExpiry);
        }
        return thExpiry;
    }
    
    public static long getTemporaryHoldExpiry(Equip q) {
        
        HashMap coolDT = CCUHsApi.getInstance().read("point and desired and cooling and temp and equipRef == \""+q.getId()+"\"");
        if (coolDT.size() > 0) {
            HashMap thMap = HSUtil.getPriorityLevel(coolDT.get("id").toString(), HayStackConstants.FORCE_OVERRIDE_LEVEL);
            if (thMap != null && thMap.get("duration") != null && thMap.get("val") != null )
            {
                return (long) Double.parseDouble(thMap.get("duration").toString());
            }
        }
        HashMap heatDT = CCUHsApi.getInstance().read("point and desired and heating and temp and equipRef == \""+q.getId()+"\"");
        if (heatDT.size() > 0 && HSUtil.getPriorityLevelVal(heatDT.get("id").toString(), HayStackConstants.FORCE_OVERRIDE_LEVEL) > 0) {
            HashMap thMap = HSUtil.getPriorityLevel(heatDT.get("id").toString(), HayStackConstants.FORCE_OVERRIDE_LEVEL);
            if (thMap != null && thMap.get("duration") != null && thMap.get("val") != null )
            {
                return (long) Double.parseDouble(thMap.get("duration").toString());
            }
        }
        HashMap avgDt = CCUHsApi.getInstance().read("point and desired and average and temp and equipRef == \""+q.getId()+"\"");
        if (avgDt.size() > 0 && HSUtil.getPriorityLevelVal(avgDt.get("id").toString(), HayStackConstants.FORCE_OVERRIDE_LEVEL) > 0) {
            HashMap thMap = HSUtil.getPriorityLevel(avgDt.get("id").toString(), HayStackConstants.FORCE_OVERRIDE_LEVEL);
            if (thMap != null && thMap.get("duration") != null && thMap.get("val") != null )
            {
                return (long) Double.parseDouble(thMap.get("duration").toString());
            }
        }
        return 0;
    }
    
    public static double getTemporaryHoldExpiry(String equipRef) {
        
        List<HashMap<Object, Object>> desiredTempPoints = CCUHsApi.getInstance()
                                              .readAllEntities("desired and temp and equipRef == \""+equipRef+"\"");
        for (HashMap<Object, Object> desiredTemp : desiredTempPoints) {
            HashMap level4Val = HSUtil.getPriorityLevel(desiredTemp.get("id").toString(),
                                                        HayStackConstants.FORCE_OVERRIDE_LEVEL);
            if (level4Val != null && level4Val.get("duration") != null && level4Val.get("val") != null ) {
                return Double.parseDouble(level4Val.get("duration").toString());
            }
        }
        return 0;
    }
    
    public static HashMap<Object, Object> getForcedOccupiedLevel(String equipRef) {
        
        List<HashMap<Object, Object>> desiredTempPoints = CCUHsApi.getInstance()
                                                                  .readAllEntities("desired and temp and equipRef == \""+equipRef+"\"");
        for (HashMap<Object, Object> desiredTemp : desiredTempPoints) {
            HashMap level4Val = HSUtil.getPriorityLevel(desiredTemp.get("id").toString(),
                                                        HayStackConstants.FORCE_OVERRIDE_LEVEL);
            if (level4Val != null && level4Val.get("duration") != null && level4Val.get("val") != null ) {
                return level4Val;
            }
        }
        return null;
    }
    
    public static void setDesiredTempAtLevel(CCUHsApi hayStack, String pointId, int level, double desiredTemp,
                                             long duration, String who) {
        if (HSUtil.getPriorityLevelVal(pointId,level) == desiredTemp) {
            CcuLog.i(L.TAG_CCU_SCHEDULER, " DesiredTemp not changed : Skip PointWrite at level "
                                          + level + " val : " + desiredTemp + " : " + pointId);
            hayStack.writeHisValById(pointId, HSUtil.getPriorityVal(pointId));
            return;
        } else {
            CcuLog.i(L.TAG_CCU_SCHEDULER, " setDesiredTempAtLevel " +level+" val : "+desiredTemp+" : "
                                          +"currentVal "+HSUtil.getPriorityLevelVal(pointId,level)+" : "
                                          +hayStack.readMapById(pointId).get("dis"));
        }

        hayStack.pointWrite(HRef.make(pointId.replace("@","")),
                            level, who,
                            HNum.make(desiredTemp),
                            HNum.make(duration));
        hayStack.writeHisValById(pointId, HSUtil.getPriorityVal(pointId));
    }
    
    public static void resetOccupancyDetection(CCUHsApi hayStack, String equipRef) {
        setOccupancyDetection(hayStack, equipRef, false);
    }

    public static void setOccupancyDetection(CCUHsApi hayStack, String equipRef, boolean occupancy) {
        HashMap<Object, Object> occupancyDetection = hayStack.readEntity(
                "occupancy and detection and equipRef  == \"" + equipRef + "\"");
        if (!occupancyDetection.isEmpty()) {
            hayStack.writeHisValueByIdWithoutCOV(occupancyDetection.get("id").toString(), occupancy ? 1.0 : 0);
        } else {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "Occupancy detection does not exist for "+equipRef);
        }
    }

    public static boolean isZoneOccupied(CCUHsApi hayStack, String roomRef, Occupancy occupancyType) {
        return hayStack.readHisValByQuery("point and occupancy and state and " +
                            "roomRef == \""+roomRef+"\"").intValue() == occupancyType.ordinal();
    }


    public static boolean isSystemProfile(CCUHsApi hayStack,String equipId){
        HashMap<Object, Object> equip = hayStack.readMapById(equipId);
        return equip.containsKey("vav") || equip.containsKey("dab");
    }

    public static boolean areAllZonesBuildingLimitsBreached(Map<String, OccupancyData> equipOccupancy ){
        for (OccupancyData occupancyData : equipOccupancy.values()) {
            if (occupancyData.occupancy != NO_CONDITIONING) {
                return false;
            }
        }
        return true;
    }

    public static Schedule trimZoneSchedule(Schedule schedule, HashMap<String, ArrayList<Interval>> spillsMap) {

        ArrayList<Interval> spills = spillsMap.get(schedule.getRoomRef());
        if (spills == null) {
            CcuLog.d(L.TAG_CCU_UI, "Schedule spills invalid for " + schedule + " in " + spillsMap);
            return schedule;
        }
        CcuLog.d(L.TAG_CCU_UI, "Trim spills for " + schedule + " in " + spillsMap);
        HashMap<Schedule.Days, ArrayList<Interval>> validSpills = new HashMap<>();
        CopyOnWriteArrayList<Schedule.Days> days = new CopyOnWriteArrayList<>(schedule.getDays());
        CopyOnWriteArrayList<Schedule.Days> conflictDays = new CopyOnWriteArrayList<>();
        for (Schedule.Days d : days) {
            Interval i = schedule.getScheduledInterval(d);

            for (Interval spill : spills) {
                if (!i.contains(spill)) {
                    continue;
                }
                if (spill.getStartMillis() <= i.getStartMillis() &&
                        spill.getEndMillis() >= i.getEndMillis()) {
                    conflictDays.add(d);
                    continue;
                }
                validSpills.put(d, disconnectedIntervals(spills, i));
                conflictDays.add(d);
            }
        }
        for (Map.Entry<Schedule.Days, ArrayList<Interval>> entry : validSpills.entrySet()) {
            for (Interval in : entry.getValue()) {
                Schedule.Days d = entry.getKey();
                Schedule.Days dayBO = new Schedule.Days();
                if(in.getStart().getHourOfDay() == 23 && in.getStart().getMinuteOfHour() == 59 &&
                        in.getEnd().getHourOfDay() == 0 && in.getEnd().getMinuteOfHour() == 0){
                    continue;
                }
                dayBO.setSthh(in.getStart().getHourOfDay());
                dayBO.setStmm(in.getStart().getMinuteOfHour());

                if(in.getEnd().getHourOfDay() == 0){
                    dayBO.setEthh(24);
                    dayBO.setEtmm(0);
                }else {
                    dayBO.setEthh(in.getEnd().getHourOfDay());
                    dayBO.setEtmm(in.getEnd().getMinuteOfHour());
                }
                dayBO.setHeatingVal(d.getHeatingVal());
                dayBO.setCoolingVal(d.getCoolingVal());
                dayBO.setSunset(false);
                dayBO.setSunrise(false);
                dayBO.setDay(in.getStart().getDayOfWeek() - 1);
                dayBO.setHeatingUserLimitMin(d.getHeatingUserLimitMin());
                dayBO.setHeatingUserLimitMax(d.getHeatingUserLimitMax());
                dayBO.setCoolingUserLimitMin(d.getCoolingUserLimitMin());
                dayBO.setCoolingUserLimitMax(d.getCoolingUserLimitMax());
                dayBO.setHeatingDeadBand(d.getHeatingDeadBand());
                dayBO.setCoolingDeadBand(d.getCoolingDeadBand());
                schedule.getDays().remove(d);
                schedule.getDays().add(dayBO);
            }
        }

        for (Schedule.Days d : conflictDays) {
            schedule.getDays().remove(d);
        }
        return schedule;
    }

    public static ArrayList<Interval> disconnectedIntervals(List<Interval> intervals, Interval r) {
        ArrayList<Interval> result = new ArrayList<>();

        ArrayList<Marker> markers = new ArrayList<>();

        for (Interval i : intervals) {
            markers.add(new Marker(i.getStartMillis(), true));
            markers.add(new Marker(i.getEndMillis(), false));
        }

        markers.sort((a, b) -> Long.compare(a.val, b.val));


        int overlap = 0;
        boolean endReached = false;

        if (markers.get(0).val > r.getStartMillis()) {
            result.add(new Interval(r.getStartMillis(), markers.get(0).val));
        }

        for (int i = 0; i < markers.size() - 1; i++) {
            Marker m = markers.get(i);

            overlap += m.start ? 1 : -1;
            Marker next = markers.get(i + 1);

            if (m.val != next.val && overlap == 0 && next.val > r.getStartMillis()) {
                long start = Math.max(m.val, r.getStartMillis());
                long end = next.val;
                if (next.val > r.getEndMillis()) {
                    end = r.getEndMillis();
                    endReached = true;
                }
                // End instance must be greater than start instance
                if (start != end && end > start) {
                    result.add(new Interval(start, end));
                }
                if (endReached)
                    break;
            }
        }

        if (!endReached) {
            Marker m = markers.get(markers.size() - 1);
            if (m.val != r.getEndMillis() && m.val < r.getEndMillis()) {
                result.add(new Interval(m.val, r.getEndMillis()));
            }
        }

        return result;
    }
    public static HashMap<String, ArrayList<Interval>> getScheduleSpills(ArrayList<Schedule.Days> daysArrayList, Schedule schedule) {

        LinkedHashMap<String, ArrayList<Interval>> spillsMap = new LinkedHashMap<>();
        if (schedule.isZoneSchedule() || schedule.isNamedSchedule()) {
            Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);
            ArrayList<Interval> intervalSpills = new ArrayList<>();
            ArrayList<Interval> systemIntervals = systemSchedule.getMergedIntervals(daysArrayList);

            for (Interval v : systemIntervals) {
                CcuLog.d(L.TAG_CCU_UI, "Merged System interval " + v);
            }

            ArrayList<Interval> zoneIntervals = schedule.getScheduledIntervals(daysArrayList);

            int size = zoneIntervals.size();
            if(!daysArrayList.isEmpty() && ( daysArrayList.get(0).getSthh() > daysArrayList.get(0).getEthh() ||
                    ( daysArrayList.get(0).getSthh() == daysArrayList.get(0).getEthh() && daysArrayList.get(0).getStmm() > daysArrayList.get(0).getEtmm() ))) {
                for(int i =0; i<size; i++){
                    Interval it = zoneIntervals.get(i);
                    DateTime initialEnding = it.getStart().withTime(23,59,59, 0);
                    DateTime subsequentStart = it.getEnd().withTime(0,0,0,0);
                    Interval iStart = new Interval(it.getStart(),initialEnding);
                    Interval iEnd = new Interval(subsequentStart,it.getEnd());
                    zoneIntervals.set(i, iStart);
                    zoneIntervals.add(iEnd);
                }

                zoneIntervals.sort((p1, p2) -> Long.compare(p1.getStartMillis(), p2.getStartMillis()));
            }

            for (Interval v : zoneIntervals) {
                CcuLog.d(L.TAG_CCU_UI, "Zone interval " + v);
            }

            for (Interval z : zoneIntervals) {
                boolean add = true;
                for (Interval s : systemIntervals) {
                    if (s.contains(z)) {
                        add = false;
                        break;
                    } else if (s.overlaps(z)) {
                        add = false;
                        for (Interval i : disconnectedIntervals(systemIntervals, z)) {
                            if (!intervalSpills.contains(i)) {
                                intervalSpills.add(i);
                            }
                        }
                    }
                }
                if (add) {
                    intervalSpills.add(z);
                    CcuLog.d(L.TAG_CCU_UI, " Zone Interval not contained " + z);
                }
            }
            if (!intervalSpills.isEmpty()) {
                spillsMap.put(schedule.getRoomRef(), intervalSpills);
            }

        }
        return spillsMap;
    }
}
