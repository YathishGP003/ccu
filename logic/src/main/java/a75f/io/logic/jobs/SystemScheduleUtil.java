package a75f.io.logic.jobs;

import android.util.Log;

import org.joda.time.DateTime;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ScheduleType;
import a75f.io.logic.tuners.TunerUtil;

public class SystemScheduleUtil {
    
    /**
     * Handles a desired temp update from CCU or Portal or Node Modules
     * @param point
     * @param manual
     * @param val
     */
    public static void handleDesiredTempUpdate(Point point, boolean manual, double val) {
        
        CcuLog.d(L.TAG_CCU_JOB, "handleDesiredTempUpdate for "+point.getDisplayName());
        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(point.getRoomRef());
        
        if (occ != null && occ.isOccupied()) {
            Schedule equipSchedule = Schedule.getScheduleByEquipId(point.getEquipRef());
            
            if(equipSchedule == null)
            {
                CcuLog.d(L.TAG_CCU_JOB,"<- *no schedule* skip handleDesiredTempUpdate");
                return;
            }
            
            if (!manual) {
                HashMap overrideLevel = getAppOverride(point.getId());
                Log.d(L.TAG_CCU_JOB, " OverrideLevel : "+overrideLevel);
                if (overrideLevel == null) {
                    return;
                }
                val = Double.parseDouble(overrideLevel.get("val").toString());
                
            }
            
            //TODO - change when setting to applyToAllDays enabled.
            if (equipSchedule.isZoneSchedule()) {
                if (point.getMarkers().contains("cooling"))
                {
                    equipSchedule.setDaysCoolVal(val, false);
                } else if (point.getMarkers().contains("heating")) {
                    equipSchedule.setDaysHeatVal(val, false);
                }
                HashMap forceOverride = HSUtil.getPriorityLevel(point.getId(), HayStackConstants.FORCE_OVERRIDE_LEVEL);
                if (forceOverride != null) {
                    CCUHsApi.getInstance().clearPointArrayLevel(point.getId(),
                                                                HayStackConstants.FORCE_OVERRIDE_LEVEL,
                                                                false);
                }
                setAppOverrideExpiry(point, System.currentTimeMillis() + 10*1000);
                CCUHsApi.getInstance().updateZoneSchedule(equipSchedule, equipSchedule.getRoomRef());
                CCUHsApi.getInstance().syncEntityTree();
            } else {
                Schedule.Days day = occ.getCurrentlyOccupiedSchedule();
                
                DateTime overrideExpiry = new DateTime(MockTime.getInstance().getMockTime())
                                              .withHourOfDay(day.getEthh())
                                              .withMinuteOfHour(day.getEtmm())
                                              .withDayOfWeek(day.getDay() + 1)
                                              .withSecondOfMinute(0);
                
                CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(point.getId()), HayStackConstants.FORCE_OVERRIDE_LEVEL, HNum.make(val), HNum.make(overrideExpiry.getMillis()
                                                                                                                                                        - System.currentTimeMillis(), "ms"));
                setAppOverrideExpiry(point, 10);


            }
            
        }else if (occ!= null && !occ.isOccupied()) {
            
            double forcedOccupiedMins = TunerUtil.readTunerValByQuery("forced and occupied and time",point.getEquipRef());
    
            if (forcedOccupiedMins == 0) {
                CcuLog.d(L.TAG_CCU_JOB, "handleDesiredTempUpdate skipped forcedOccupiedMins "+forcedOccupiedMins);
                return;
            }
            
            if (manual) {
                CCUHsApi.getInstance().pointWrite(HRef.copy(point.getId()), HayStackConstants.FORCE_OVERRIDE_LEVEL, "manual", HNum.make(val) , HNum.make(forcedOccupiedMins * 60 * 1000, "ms"));
            } else
            {
                HashMap overrideLevel = getAppOverride(point.getId());
                Log.d(L.TAG_CCU_JOB, " Desired Temp OverrideLevel : " + overrideLevel);
                if (overrideLevel == null) {
                    return;
                }
                double dur = Double.parseDouble(overrideLevel.get("duration").toString());
                CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(point.getId()), HayStackConstants.FORCE_OVERRIDE_LEVEL, HNum.make(Double.parseDouble(overrideLevel.get("val").toString())), HNum.make(dur == 0 ? forcedOccupiedMins * 60 * 1000 : dur - System.currentTimeMillis(), "ms"));
                //Write to level 9/10
                ArrayList values = CCUHsApi.getInstance().readPoint(point.getId());
                if (values != null && values.size() > 0)
                {
                    for (int l = 9; l <= values.size(); l++)
                    {
                        HashMap valMap = ((HashMap) values.get(l - 1));
                        Log.d(L.TAG_CCU_JOB, " Desired Temp Override : " + valMap);
                        if (valMap.get("duration") != null && valMap.get("val") != null)
                        {
                            long d = (long) Double.parseDouble(valMap.get("duration").toString());
                            if (d == 0)
                            {
                                CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(point.getId()), l, HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(dur == 0 ? forcedOccupiedMins * 60 * 1000 : dur - System.currentTimeMillis(), "ms"));
                            }
                        }
                    }
                }
            }
        }

        HashMap equipid = CCUHsApi.getInstance().read("equip and group == \"" + point.getGroup() + "\"");

        Equip equip =  new Equip.Builder().setHashMap(equipid).build();
        ScheduleProcessJob.processZoneEquipSchedule(equip);

        CCUHsApi.getInstance().writeHisValById(point.getId(), HSUtil.getPriorityVal(point.getId()));
    }
    
    /**
     * Handle cooling, heating and average desired temp updates in a single method.
     * @param coolpoint
     * @param heatpoint
     * @param avgpoint
     * @param coolval
     * @param heatval
     * @param avgval
     */
    public static void handleManualDesiredTempUpdate(Point coolpoint, Point heatpoint, Point avgpoint, double coolval, double heatval, double avgval) {
        
        CcuLog.d(L.TAG_CCU_JOB, "handleManualDesiredTempUpdate for " + coolpoint.getDisplayName() + "," + heatpoint.getDisplayName() + "," + coolval + "," + heatval + "," + avgval);
        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(coolpoint.getRoomRef());
        
        if (occ != null && occ.isOccupied()) {
            Schedule equipSchedule = Schedule.getScheduleByEquipId(coolpoint.getEquipRef());
            
            if(equipSchedule == null) {
                CcuLog.d(L.TAG_CCU_JOB,"<- *no schedule* skip handleDesiredTempUpdate");
                return;
            }
            
            //TODO - change when setting to applyToAllDays enabled.
            if (equipSchedule.isZoneSchedule()) {
                if ((coolpoint != null) && (coolval != 0)) {
                    equipSchedule.setDaysCoolVal(coolval, false);
                    //If the zone is currently in forced occupied, update the temperature to level 4 too.
                    clearOverrideAndWriteToCcuLevel(coolpoint, coolval);
                }
                if ((heatpoint != null) && (heatval != 0)) {
                    equipSchedule.setDaysHeatVal(heatval, false);
                    clearOverrideAndWriteToCcuLevel(heatpoint, heatval);
                }
                
                setAppOverrideExpiry(coolpoint, 10);
                setAppOverrideExpiry(heatpoint, 10);
                CCUHsApi.getInstance().updateZoneSchedule(equipSchedule, equipSchedule.getRoomRef());
                CCUHsApi.getInstance().syncEntityTree();
            } else {
                Schedule.Days day = occ.getCurrentlyOccupiedSchedule();
                
                DateTime overrideExpiry = new DateTime(MockTime.getInstance().getMockTime())
                                              .withHourOfDay(day.getEthh())
                                              .withMinuteOfHour(day.getEtmm())
                                              .withDayOfWeek(day.getDay() + 1)
                                              .withSecondOfMinute(0);
                
                if((coolpoint != null) && (coolval != 0)) {
                    CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(coolpoint.getId()), HayStackConstants.FORCE_OVERRIDE_LEVEL, HNum.make(coolval), HNum.make(overrideExpiry.getMillis()
                                                                                                                                                                    - System.currentTimeMillis(), "ms"));
                    setAppOverrideExpiry(coolpoint, overrideExpiry.getMillis());
                }
                if((heatpoint != null) && (heatval != 0)){
                    CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(heatpoint.getId()), HayStackConstants.FORCE_OVERRIDE_LEVEL, HNum.make(heatval), HNum.make(overrideExpiry.getMillis()
                                                                                                                                                                    - System.currentTimeMillis(), "ms"));
                    setAppOverrideExpiry(heatpoint, overrideExpiry.getMillis());
                }
                if(avgpoint != null){
                    CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(avgpoint.getId()), HayStackConstants.FORCE_OVERRIDE_LEVEL, HNum.make(avgval), HNum.make(overrideExpiry.getMillis()
                                                                                                                                                                  - System.currentTimeMillis(), "ms"));
                    setAppOverrideExpiry(avgpoint, overrideExpiry.getMillis());
                }
            }
            
        }else if (occ!= null && !occ.isOccupied()) {
            
            double forcedOccupiedMins = TunerUtil.readTunerValByQuery("forced and occupied and time", coolpoint.getEquipRef());
            
            if (forcedOccupiedMins == 0) {
                CcuLog.d(L.TAG_CCU_JOB, "handleManualDesiredTempUpdate skipped forcedOccupiedMins "+forcedOccupiedMins);
                return;
            }
            if((coolpoint != null) && (coolval != 0))
                CCUHsApi.getInstance().pointWrite(HRef.copy(coolpoint.getId()), HayStackConstants.FORCE_OVERRIDE_LEVEL, "manual", HNum.make(coolval) , HNum.make(forcedOccupiedMins * 60 * 1000, "ms"));
            if((heatpoint != null) && (heatval != 0))
                CCUHsApi.getInstance().pointWrite(HRef.copy(heatpoint.getId()), HayStackConstants.FORCE_OVERRIDE_LEVEL, "manual", HNum.make(heatval) , HNum.make(forcedOccupiedMins * 60 * 1000, "ms"));
            
            
        }
        
        if (coolpoint != null) {
            CCUHsApi.getInstance().writeHisValById(coolpoint.getId(), HSUtil.getPriorityVal(coolpoint.getId()));
        }
        
        if (heatpoint != null) {
            CCUHsApi.getInstance().writeHisValById(heatpoint.getId(), HSUtil.getPriorityVal(heatpoint.getId()));
        }
    }
    
    private static void clearOverrideAndWriteToCcuLevel(Point point, double val) {
        HashMap heatOverride = HSUtil.getPriorityLevel(point.getId(),
                                                       HayStackConstants.FORCE_OVERRIDE_LEVEL);
        if (heatOverride != null) {
            CCUHsApi.getInstance().clearPointArrayLevel(point.getId(),
                                                        HayStackConstants.FORCE_OVERRIDE_LEVEL,
                                                        false);
            CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(point.getId()),
                                                        HayStackConstants.CCU_USER_WRITE_LEVEL,
                                                        HNum.make(val),
                                                        HNum.make(0));
            
        }
    }
    
    private static void writeOverRideLevel(Point point, double dur, double forcedOccupiedMins){
        //Write to level 9/10
        ArrayList values = CCUHsApi.getInstance().readPoint(point.getId());
        if (values != null && values.size() > 0)
        {
            for (int l = 9; l <= values.size(); l++)
            {
                HashMap valMap = ((HashMap) values.get(l - 1));
                Log.d(L.TAG_CCU_JOB, " Desired Temp Override : " + valMap);
                if (valMap.get("duration") != null && valMap.get("val") != null)
                {
                    long d = (long) Double.parseDouble(valMap.get("duration").toString());
                    if (d == 0)
                    {
                        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(point.getId()), l, HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(dur == 0 ? forcedOccupiedMins * 60 * 1000 : dur - System.currentTimeMillis(), "ms"));
                    }
                }
            }
        }
    }
    
    /**
     *
     * Returns if any app-level override exists for the point.
     * Portals and apps write to level higher than 8.If there is a valid value above level 8 in the writable array,
     * it means this point has an existing override and it should be considered while taking decisions about the
     * setpoint updates.
     * @param id
     * @return
     */
    public static HashMap getAppOverride(String id) {
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        long duration = -1;
        int level = 0;
        if (values != null && values.size() > 0)
        {
            for (int l = 9; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                Log.d(L.TAG_CCU_JOB, "getAppOverride : "+valMap);
                if (valMap.get("duration") != null && valMap.get("val") != null ) {
                    long dur = (long) Double.parseDouble(valMap.get("duration").toString());
                    if (dur == 0) {
                        return valMap;
                    }
                    if (dur > duration) {
                        level = l;
                        duration = dur;
                    }
                }
            }
            return duration == -1 ? null : (HashMap) values.get(level-1);
        }
        return null;
    }
    
    /**
     * Updates any existing app-override levels to the point's writable array.
     * @param point
     * @param overrRideExpiryseconds
     */
    public static void setAppOverrideExpiry(Point point, long overrRideExpiryseconds) {
        HashMap overrideLevel = getAppOverride(point.getId());
        Log.d(L.TAG_CCU_JOB, " setAppOverrideExpiry : overrideLevel " + overrideLevel);
        if (overrideLevel == null) {
            return;
        }
        
        ArrayList values = CCUHsApi.getInstance().readPoint(point.getId());
        if (values != null && values.size() > 0)
        {
            for (int l = 9; l <= values.size(); l++)
            {
                HashMap valMap = ((HashMap) values.get(l - 1));
                Log.d(L.TAG_CCU_JOB, "setAppOverrideExpiry : " + valMap);
                if (valMap.get("duration") != null && valMap.get("val") != null)
                {
                    long d = (long) Double.parseDouble(valMap.get("duration").toString());
                    if (d == 0)
                    {
                        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(point.getId()), l, HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(overrRideExpiryseconds * 1000, "ms"));
                    }
                }
            }
        }
    }
    
    public static void clearOverrides(String id) {
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ )
            {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (l != 8 && valMap.get("duration") != null && valMap.get("val") != null)
                {
                    CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(id), l, HNum.make(0), HNum.make(1, "ms"));
                }
            }
        }
    }
    
    /**
     * Update schedule type change.
     * Every zone has a zoneSchedule. If the zone is following the buildingSchedule, we add an additional marker tag
     * "disabled" to its zoneSchedule. And when schedule type is changed to zoneSchedule , the "disabled" marker is
     * removed and zoneSchedule is activated for the zone.
     * @param p
     */
    public static void handleScheduleTypeUpdate(Point p){
        CcuLog.d(L.TAG_CCU_SCHEDULER, " ScheduleType handleScheduleTypeUpdate "+p.getDisplayName());
    
        HSUtil.printPointArr(p, L.TAG_CCU_SCHEDULER);//TODO- Added 12/01: Remove once zone schedule issue is fixed.
        if (p.getRoomRef() == null || p.getRoomRef().contains("SYSTEM")) {
            CcuLog.d(L.TAG_CCU_SCHEDULER, " Abort , invalid roomRef "+p.getRoomRef());
            return;
        }
        Zone zone = new Zone.Builder().setHashMap(CCUHsApi.getInstance().readMapById(p.getRoomRef())).build();
        Schedule schedule = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
        
        if (schedule == null) {
            CcuLog.d(L.TAG_CCU_SCHEDULER, "Failed to read schedule : schedule type update cannot be completed for "
                     +zone+" scheduleRef "+zone.getScheduleRef());
        }
        if (CCUHsApi.getInstance().readPointPriorityVal(p.getId()) == ScheduleType.ZONE.ordinal()) {
            schedule.setDisabled(false);
        } else {
            schedule.setDisabled(true);
        }
        if (schedule.isZoneSchedule() && schedule.getRoomRef()!= null){
            CCUHsApi.getInstance().updateScheduleNoSync(schedule, schedule.getRoomRef());
        } else {
            CCUHsApi.getInstance().updateScheduleNoSync(schedule, null);
        }
    
        HashMap coolDT = CCUHsApi.getInstance().read("point and desired and cooling and temp and equipRef == \""+p.getEquipRef()+"\"");
        clearOverrides(coolDT.get("id").toString());
        HashMap heatDT = CCUHsApi.getInstance().read("point and desired and heating and temp and equipRef == \""+p.getEquipRef()+"\"");
        clearOverrides(heatDT.get("id").toString());
        HashMap avgDt = CCUHsApi.getInstance().read("point and desired and average and temp and equipRef == \""+p.getEquipRef()+"\"");
        clearOverrides(avgDt.get("id").toString());
        
    }
}
