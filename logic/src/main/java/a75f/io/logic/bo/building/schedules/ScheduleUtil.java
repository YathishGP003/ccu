package a75f.io.logic.bo.building.schedules;

import android.os.StrictMode;
import android.util.Log;

import org.joda.time.DateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import static a75f.io.logic.L.TAG_CCU_SCHEDULER;
import static a75f.io.logic.bo.building.schedules.Occupancy.AUTOAWAY;
import static a75f.io.logic.bo.building.schedules.Occupancy.AUTOFORCEOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.EMERGENCY_CONDITIONING;
import static a75f.io.logic.bo.building.schedules.Occupancy.FORCEDOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.KEYCARD_AUTOAWAY;
import static a75f.io.logic.bo.building.schedules.Occupancy.OCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.VACATION;

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
    
    /**
     * Zone is in AutoAway if at least one equip is in AutoAway.
     */
    public static boolean isZoneAutoAway(String roomRef, CCUHsApi hayStack, Map<String, OccupancyData> equipOccupancy) {
        ArrayList<HashMap<Object, Object>> equipsInZone = hayStack
                                                              .readAllEntities("equip and roomRef == \""+roomRef+"\"");
        
        for (HashMap<Object, Object> equip : equipsInZone) {
            if (equipOccupancy.get(equip.get("id").toString()).occupancy == AUTOAWAY) {
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
    
    public static boolean isAnyZoneOccupied(Map<String, OccupancyData> equipOccupancy) {
        for (OccupancyData occupancyData : equipOccupancy.values()) {
            if (occupancyData.occupancy == OCCUPIED) {
                return true;
            }
        }
        return false;
    }
    
    public static Occupied getCurrentOccupied(HashMap<String, Occupied> occupiedHashMap,
                                                        Map<String, OccupancyData> equipOccupancy) {
        Occupied currOccupied = null;
        for (Map.Entry occEntry : occupiedHashMap.entrySet()) {
            String roomRef = occEntry.getKey().toString();
            if (!isAHUServedZone(roomRef, CCUHsApi.getInstance())) {
                CcuLog.i(TAG_CCU_SCHEDULER, " Zone does not contain AHU served equip "+roomRef);
                continue;
            }
            Occupied occ = (Occupied) occEntry.getValue();
            //CcuLog.i(TAG_CCU_SCHEDULER, " Occupied for "+roomRef+" "+occ.toString());
            if (occ.isOccupied() && !isZoneAutoAway(roomRef, CCUHsApi.getInstance(), equipOccupancy)) {
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
            Occupied occ = (Occupied) occEntry.getValue();
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
                equip.containsKey("ti") || equip.containsKey("bpos") );
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
            DateTime endTime = new DateTime().withTime(specialSchedule.getEthh(), specialSchedule.getEtmm(),59,0);
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
        //HashMap averageDT =CCUHsApi.getInstance().read("point and desired and average and temp and equipRef == \"" + equipId + "\"");
        
        CCUHsApi.getInstance().clearPointArrayLevel(coolDT.get("id").toString(), level, false);
        CCUHsApi.getInstance().clearPointArrayLevel(heatDT.get("id").toString(), level, false);
        
        /*if (!averageDT.isEmpty()) {
            CCUHsApi.getInstance().pointWrite(HRef.copy(averageDT.get("id").toString()), 4, "manual", HNum.make(0), HNum.make(1, "ms"));
        }
        systemOccupancy = UNOCCUPIED; */
    }
    public static long getSystemTemporaryHoldExpiry() {
        long thExpiry = 0;
        for (Floor f: HSUtil.getFloors())
        {
            for (Zone z : HSUtil.getZones(f.getId()))
            {
                Equip q = HSUtil.getEquipFromZone(z.getId());
                if(q.getMarkers().contains("dab") || q.getMarkers().contains("dualDuct")
                   || q.getMarkers().contains("vav" ) || q.getMarkers().contains("ti")
                   || q.getMarkers().contains("bpos") || q.getMarkers().contains("sse")) {
                    if (getTemporaryHoldExpiry(q) > thExpiry) {
                        thExpiry = getTemporaryHoldExpiry(q);
                    }
                }
            }
        }
        
        //Logging temporary hold expiry for debugging.
        if (thExpiry > 0)
        {
            Log.d(TAG_CCU_SCHEDULER, "thExpiry: " + thExpiry);
        }
        return thExpiry;
    }
    
    public static long getTemporaryHoldExpiry(Equip q) {
        
        HashMap coolDT = CCUHsApi.getInstance().read("point and desired and cooling and temp and equipRef == \""+q.getId()+"\"");
        if (coolDT.size() > 0) {
            HashMap thMap = HSUtil.getPriorityLevel(coolDT.get("id").toString(), 4);
            if (thMap != null && thMap.get("duration") != null && thMap.get("val") != null )
            {
                return (long) Double.parseDouble(thMap.get("duration").toString());
            }
        }
        HashMap heatDT = CCUHsApi.getInstance().read("point and desired and heating and temp and equipRef == \""+q.getId()+"\"");
        if (heatDT.size() > 0 && HSUtil.getPriorityLevelVal(heatDT.get("id").toString(), 4) > 0) {
            HashMap thMap = HSUtil.getPriorityLevel(heatDT.get("id").toString(), 4);
            if (thMap != null && thMap.get("duration") != null && thMap.get("val") != null )
            {
                return (long) Double.parseDouble(thMap.get("duration").toString());
            }
        }
        HashMap avgDt = CCUHsApi.getInstance().read("point and desired and average and temp and equipRef == \""+q.getId()+"\"");
        if (avgDt.size() > 0 && HSUtil.getPriorityLevelVal(avgDt.get("id").toString(), 4) > 0) {
            HashMap thMap = HSUtil.getPriorityLevel(avgDt.get("id").toString(), 4);
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
                                             long duration) {
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
                            level, "Scheduler",
                            HNum.make(desiredTemp),
                            HNum.make(duration));
        hayStack.writeHisValById(pointId, HSUtil.getPriorityVal(pointId));
    }
    
    public static void resetOccupancyDetection(CCUHsApi hayStack, String equipRef) {
        HashMap<Object, Object> occupancyDetection = hayStack.readEntity(
            "occupancy and detection and equipRef  == \"" + equipRef + "\"");
        if (!occupancyDetection.isEmpty()) {
            hayStack.writeHisValueByIdWithoutCOV(occupancyDetection.get("id").toString(), 0.0);
        } else {
            CcuLog.i(L.TAG_CCU_SCHEDULER, "Occupancy detection does not exist for "+equipRef);
        }
    }
}
