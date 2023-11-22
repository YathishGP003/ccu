package a75f.io.logic.bo.building.schedules;

import static a75f.io.logic.L.TAG_CCU_SCHEDULER;
import static a75f.io.logic.bo.building.schedules.Occupancy.AUTOAWAY;
import static a75f.io.logic.bo.building.schedules.Occupancy.AUTOFORCEOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.NONE;
import static a75f.io.logic.bo.building.schedules.Occupancy.NO_CONDITIONING;
import static a75f.io.logic.bo.building.schedules.Occupancy.EMERGENCY_CONDITIONING;
import static a75f.io.logic.bo.building.schedules.Occupancy.FORCEDOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.KEYCARD_AUTOAWAY;
import static a75f.io.logic.bo.building.schedules.Occupancy.OCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.PRECONDITIONING;
import static a75f.io.logic.bo.building.schedules.Occupancy.UNOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.VACATION;
import static a75f.io.logic.bo.building.schedules.Occupancy.WINDOW_OPEN;
import static a75f.io.logic.bo.building.schedules.ScheduleUtil.ACTION_STATUS_CHANGE;
import static a75f.io.logic.bo.building.schedules.ScheduleUtil.isCurrentMinuteUnderSpecialSchedule;

import android.content.Intent;
import android.util.Log;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.util.TimeUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.interfaces.ZoneDataInterface;
import a75f.io.logic.autocommission.AutoCommissioningUtil;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneTempState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hyperstatmonitoring.HyperStatMonitoringProfile;
import a75f.io.logic.bo.building.modbus.ModbusProfile;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.util.DesiredTempDisplayMode;
import a75f.io.logic.bo.util.TemperatureMode;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.L.TAG_CCU_SCHEDULER;
import static a75f.io.logic.bo.building.schedules.Occupancy.AUTOAWAY;
import static a75f.io.logic.bo.building.schedules.Occupancy.AUTOFORCEOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.EMERGENCY_CONDITIONING;
import static a75f.io.logic.bo.building.schedules.Occupancy.FORCEDOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.KEYCARD_AUTOAWAY;
import static a75f.io.logic.bo.building.schedules.Occupancy.OCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.PRECONDITIONING;
import static a75f.io.logic.bo.building.schedules.Occupancy.UNOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.VACATION;
import static a75f.io.logic.bo.building.schedules.Occupancy.WINDOW_OPEN;
import static a75f.io.logic.bo.building.schedules.ScheduleUtil.ACTION_STATUS_CHANGE;
import static a75f.io.logic.bo.building.schedules.ScheduleUtil.isCurrentMinuteUnderSpecialSchedule;

public class ScheduleManager {
    
    
    private static ScheduleManager instance = null;
    
    
    private final HashMap<String, Occupied> occupiedHashMap = new HashMap<>();
    private final Map<String, Occupancy> zoneOccupancy = new HashMap<>();
    private final Map<String, OccupancyData> equipOccupancy = new ConcurrentHashMap<>();
    
    private Occupied currentOccupiedInfo = null;
    private Occupied nextOccupiedInfo = null;
    private Occupancy systemOccupancy = null;
    
    //TODO-Schedules - To be moved
    private ZoneDataInterface scheduleDataInterface = null;
    private ZoneDataInterface zoneDataInterface     = null;
    
    public static ScheduleManager getInstance() {
        if (instance == null) {
            synchronized(ScheduleManager.class) {
                if (instance == null) {
                    instance = new ScheduleManager();
                }
            }
        }
        return instance;
    }
    
    public void setScheduleDataInterface(ZoneDataInterface in) { scheduleDataInterface = in; }
    public void setZoneDataInterface(ZoneDataInterface in){
        zoneDataInterface = in;
    }
    
    
    public Occupied getOccupiedModeCache(String id) {
        return occupiedHashMap.get(id);
    }
    
    /**
     * This could be removed in future. All the other modules should read occupancy points from the Damain/haystack
     * and stop using getOccupiedModeCache directly.
     * But currently there are too many references, so keeping leaving it as is during the current Schedule refactor
     * (June-2022).
     */
    public void updateOccupiedSchedule(Equip equip, Schedule equipSchedule, Schedule vacation) {
        
        
        CcuLog.i(TAG_CCU_SCHEDULER, "updateOccupiedSchedule: " + equip.getDisplayName()+" "+equipSchedule);
        
        Occupied occ = equipSchedule.getCurrentValues();
        
        //When schedule is deleted
        if (occ == null) {
            occupiedHashMap.remove(equip.getRoomRef());
            CcuLog.i(TAG_CCU_SCHEDULER, " Equip not occupied "+equip.getDisplayName());
            return;
        }else if(occ.getCoolingVal() == null || occ.getHeatingVal() == null){
            CcuLog.i(TAG_CCU_SCHEDULER, " occ.getCoolingVal(): "+occ.getCoolingVal()+" occ.getHeatingVal(): "+occ.getHeatingVal());
            return;
        }
        
        CcuLog.i(TAG_CCU_SCHEDULER, "updateOccupiedSchedule: NextOcc "+occ.getNextOccupiedSchedule()+" "+
                                    "Current occ "+occ.getCurrentlyOccupiedSchedule());
        
        
        if(vacation != null)
            occ.setOccupied(false);
        
        occ.setVacation(vacation);
        occ.setSystemZone(true);
        
        double occuStatus = CCUHsApi.getInstance().readHisValByQuery("point and occupancy and mode and equipRef == \""+equip.getId()+"\"");

        double heatingDeadBand ;
        double coolingDeadBand ;
        double setback ;

        ArrayList<HashMap<Object , Object>> isSchedulableAvailable = CCUHsApi.getInstance().readAllSchedulable();
        HashMap<Object,Object> hDBMap = CCUHsApi.getInstance().readEntity("zone and heating and deadband and roomRef == \"" + equip.getRoomRef() + "\"");
        if (!isSchedulableAvailable.isEmpty() && !hDBMap.isEmpty()) {
            heatingDeadBand = CCUHsApi.getInstance().readPointPriorityValByQuery("zone and heating and deadband and roomRef == \"" + equip.getRoomRef() + "\"");
            coolingDeadBand = CCUHsApi.getInstance().readPointPriorityValByQuery("zone and cooling and deadband and roomRef == \"" + equip.getRoomRef() + "\"");
            setback = CCUHsApi.getInstance().readPointPriorityValByQuery("zone and unoccupied and setback and roomRef == \"" + equip.getRoomRef() + "\"");
        }else{
            heatingDeadBand = TunerUtil.readTunerValByQuery("heating and deadband and base", equip.getId());
            coolingDeadBand = TunerUtil.readTunerValByQuery("cooling and deadband and base", equip.getId());
            setback = TunerUtil.readTunerValByQuery("unoccupied and setback", equip.getId());
        }
        occ.setHeatingDeadBand(heatingDeadBand);
        occ.setCoolingDeadBand(coolingDeadBand);
        occ.setUnoccupiedZoneSetback(setback);
        Occupancy curOccupancy = Occupancy.values()[(int)occuStatus];
        if(curOccupancy == Occupancy.PRECONDITIONING)
            occ.setPreconditioning(true);
        else if(curOccupancy == Occupancy.FORCEDOCCUPIED)
            occ.setForcedOccupied(true);
        if (putOccupiedModeCache(equip.getRoomRef(), occ)) {
            double deadband = (occ.getCoolingVal() - occ.getHeatingVal()) / 2.0;
            occ.setCoolingDeadBand(deadband);
            occ.setHeatingDeadBand(deadband);
        }
        CcuLog.i(TAG_CCU_SCHEDULER, "updateOccupiedSchedule: put occ for "+equip.getRoomRef());
    }

    private void doProcessSchedules() {
        if (!CCUHsApi.getInstance().isCCURegistered()){
            return;
        }
        zoneOccupancy.clear();
        equipOccupancy.clear();
        ArrayList<Schedule> activeVacationSchedules = CCUHsApi.getInstance().getSystemSchedule(true);

        Schedule activeSystemVacation = ScheduleUtil.getActiveVacation(activeVacationSchedules);

        Log.d(TAG_CCU_SCHEDULER, " #### processSchedules activeSystemVacation ####" + activeSystemVacation);

        //Read all equips
        ArrayList<HashMap<Object, Object>> equips = CCUHsApi.getInstance().readAllEntities("equip and zone");
        for(HashMap hs : equips) {
            Equip equip = new Equip.Builder().setHashMap(hs).build();
            if(equip != null) {
                Log.d(L.TAG_CCU_SCHEDULER, " processSchedules "+equip.getDisplayName());
                processScheduleForEquip(equip, activeSystemVacation);
            }
        }

        Set<ZoneProfile> zoneProfiles = new HashSet<>(L.ccu().zoneProfiles);
        updateOccupancy(CCUHsApi.getInstance(), zoneProfiles);
        updateDesiredTemp(zoneProfiles);


        ArrayList<HashMap<Object , Object>> isSchedulableAvailable = CCUHsApi.getInstance().readAllSchedulable();

        if (!isSchedulableAvailable.isEmpty())
            updateLimitsAndDeadBand();



        //TODO-Schedules - Optimize equip creation and need for this method.
        for(HashMap hs : equips) {
            Equip equip = new Equip.Builder().setHashMap(hs).build();
            updateEquipScheduleStatus(equip);
        }
        ScheduleUtil.deleteExpiredVacation();
        ScheduleUtil.deleteExpiredSpecialSchedules();
    
        //TODO - refactor. This can only be done after updating desired temp.
        for (ZoneProfile profile : zoneProfiles) {

            if (profile instanceof ModbusProfile) {
                continue;
            }

            EquipOccupancyHandler occupancyHandler = profile.getEquipOccupancyHandler();
            OccupancyData occupancyData = equipOccupancy.get(occupancyHandler.getEquipRef());
            if (occupancyData != null) {
                occupancyHandler.writeOccupancyMode(occupancyData.occupancy);
            }
        }
    }
    public void processSchedules() {
        try {
            doProcessSchedules();
        } catch (Exception e) {
            //An exception here is mostly result of schedules by deleted from UI/Apps/Portal
            // or a profile/equip itself being deleted/added while it is being processed.
            // The error is recovered in the next iteration.
            CcuLog.e(TAG_CCU_SCHEDULER, "Process Schedules error !"+e);
            e.printStackTrace();
        }

    }

    private void processScheduleForEquip(Equip equip, Schedule activeSystemVacation) {
        Schedule equipSchedule = Schedule.getScheduleForZoneScheduleProcessing(equip.getRoomRef().replace("@", ""));

        if(equipSchedule == null || equip.getRoomRef().contains("SYSTEM")) {
            CcuLog.d(L.TAG_CCU_SCHEDULER,"<- *no schedule*");
            return;
        }
    
        Set<Schedule.Days> combinedSpecialSchedules =  Schedule.combineSpecialSchedules(equip.getRoomRef().
                                                                                                              replace("@", ""));
        if(ScheduleUtil.isCurrentMinuteUnderSpecialSchedule(combinedSpecialSchedules)){
            updateOccupiedSchedule(equip, equipSchedule, null);
        } else if (activeSystemVacation == null ) {
            ArrayList<Schedule> activeZoneVacationSchedules = CCUHsApi.getInstance().getZoneSchedule(equip.getRoomRef(),true);
            Schedule activeZoneVacationSchedule = ScheduleUtil.getActiveVacation(activeZoneVacationSchedules);
            Log.d(L.TAG_CCU_SCHEDULER, "Equip "+equip.getDisplayName()+" activeZoneVacationSchedules "+activeZoneVacationSchedules.size());
            updateOccupiedSchedule(equip, equipSchedule, activeZoneVacationSchedule);
        } else {
            updateOccupiedSchedule(equip, equipSchedule, activeSystemVacation);
        }
    }
    //TODO-Schedules - Merge with above method
    public void processZoneEquipSchedule(Equip equip){
        
        if (equip == null) {
            return;
        }
        Log.d(L.TAG_CCU_SCHEDULER, " Equip "+equip.getDisplayName());
    
        ArrayList<Schedule> activeVacationSchedules = CCUHsApi.getInstance().getSystemSchedule(true);
        Schedule activeSystemVacation = ScheduleUtil.getActiveVacation(activeVacationSchedules);
    
        processScheduleForEquip(equip, activeSystemVacation);
        
        updateEquipScheduleStatus(equip);
        //systemVacation = activeSystemVacation != null || isAllZonesInVacation();
        updateSystemOccupancy(CCUHsApi.getInstance());
    }
    
    public void updateOccupancy(CCUHsApi hayStack, Set<ZoneProfile> zoneProfiles) {
        CcuLog.i(TAG_CCU_SCHEDULER, "updateOccupancy : ScheduleManager");
        for (ZoneProfile profile : zoneProfiles) {
            if (profile instanceof ModbusProfile) {
                continue;
            }
            profile.updateOccupancy(hayStack);
            EquipOccupancyHandler occupancyHandler = profile.getEquipOccupancyHandler();
            OccupancyData occupancyData = getOccupancyData(occupancyHandler, CCUHsApi.getInstance());
            CcuLog.i(TAG_CCU_SCHEDULER,
                     "Updated equipOccupancy "+profile.getEquip().getDisplayName()+" : "+occupancyData.occupancy);
            
            equipOccupancy.put(occupancyHandler.getEquipRef(), occupancyData);
        }
        
        updateZoneOccupancy(hayStack);
        updateSystemOccupancy(hayStack);
    }
    
    
    //Update Schedules instantly
    public void updateSchedules() {
        CcuLog.d(TAG_CCU_SCHEDULER,"updateSchedules ->");
        
        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() == 0) {
            CcuLog.d(TAG_CCU_SCHEDULER,"No Site Registered ! <-updateSchedules ");
            return;
        }
        
        new Thread() {
            @Override
            public void run() {
                processSchedules();
                CcuLog.d(TAG_CCU_SCHEDULER,"<- updateSchedules");
            }
        }.start();
        
    }
    public void updateSchedules(final Equip equip) {
        CcuLog.d(TAG_CCU_SCHEDULER,"updateSchedules ->"+equip.getDisplayName());
        
        new Thread() {
            @Override
            public void run() {
                processZoneEquipSchedule(equip);
                CcuLog.d(TAG_CCU_SCHEDULER,"<- updateSchedules for equip done"+equip.getDisplayName());
            }
        }.start();
        
    }
    
    /**
     *  If the occupied mode is different when putting it in the cache return true.
     *  If it is the same return false.
     */
    public boolean putOccupiedModeCache(String id, Occupied occupied) {
        if(!occupiedHashMap.containsKey(id)) {
            CcuLog.i(TAG_CCU_SCHEDULER, "SchedulerCache : Putting in new key");
            occupiedHashMap.put(id, occupied);
            return true;
        }
        
        Occupied currentOccupiedMode = occupiedHashMap.get(id);
        if(currentOccupiedMode != null) {
            if(occupied.equals(currentOccupiedMode)) {
                CcuLog.i(TAG_CCU_SCHEDULER, "SchedulerCache : Reusing old occupied values");
                return false;
            } else {
                CcuLog.i(TAG_CCU_SCHEDULER, "SchedulerCache : Putting in new occupied values");
                occupiedHashMap.put(id, occupied);
                return true;
            }
        }
        return false;
    }
    
    
    
    public void updateDesiredTemp(Set<ZoneProfile> zoneProfiles) {

        for (ZoneProfile profile : zoneProfiles) {
            if (profile instanceof ModbusProfile || profile instanceof HyperStatMonitoringProfile) {
                continue;
            }
            EquipOccupancyHandler occupancyHandler = profile.getEquipOccupancyHandler();
            Occupancy currentOccupiedMode = occupancyHandler.getCurrentOccupiedMode();
            OccupancyData updatedOccupancy = equipOccupancy.get(occupancyHandler.getEquipRef());

            if (updatedOccupancy == null) {
                CcuLog.i(TAG_CCU_SCHEDULER, "Invalid updatedOccupancy for " + occupancyHandler.getEquipRef());
                continue;
            }

                Equip equip = profile.getEquip();
                Schedule equipSchedule = Schedule.getScheduleForZoneScheduleProcessing(equip.getRoomRef()
                        .replace("@", ""));

            CcuLog.i(TAG_CCU_SCHEDULER,
                    " updateDesiredTemp " + equip.getDisplayName() + " : occupancy " + currentOccupiedMode
                            + " -> " + updatedOccupancy.occupancy);
            profile.getEquipScheduleHandler().updateDesiredTemp(currentOccupiedMode, updatedOccupancy.occupancy, equipSchedule,updatedOccupancy);
            if ((zoneDataInterface != null) /*&& (cachedOccupied != null)*/) {
                zoneDataInterface.refreshDesiredTemp(equip.getGroup(), "", "");
            }
        }
    }

    public void updateLimitsAndDeadBand() {
        for (ZoneProfile profile : L.ccu().zoneProfiles) {

            if (profile instanceof ModbusProfile || profile instanceof HyperStatMonitoringProfile) {
                continue;
            }

            Equip equip = profile.getEquip();
            String roomRef = equip.getRoomRef();
            Schedule equipSchedule = Schedule.getScheduleForZoneScheduleProcessing(equip.getRoomRef()
                    .replace("@", ""));
            ArrayList<Schedule.Days> mDays = equipSchedule.getDays();
            if (!equipSchedule.getMarkers().contains("specialschedule")) {
                if (!equipSchedule.getMarkers().contains(Tags.FOLLOW_BUILDING)) {
                    if (equipSchedule.getUnoccupiedZoneSetback() != null)
                        updateUnOccupiedSetBackPoint(equipSchedule.getUnoccupiedZoneSetback(), roomRef);
                    Occupied occ = equipSchedule.getCurrentValues();
                    if (equipSchedule.getUnoccupiedZoneSetback() != null)
                        occ.setUnoccupiedZoneSetback(equipSchedule.getUnoccupiedZoneSetback());
                    int day = DateTime.now().dayOfWeek().get() - 1;
                    Calendar calender = Calendar.getInstance();
                    int hrs = calender.get(Calendar.HOUR_OF_DAY) * 60;
                    int min = calender.get(Calendar.MINUTE);
                    int curTime = hrs + min;
                    for (Schedule.Days d : mDays) {
                        if (d.getDay() == day) {
                            int startSchTime = (d.getSthh() * 60) + d.getStmm();
                            int endSchTime = (d.getEthh() * 60) + d.getEtmm();
                            if (curTime > startSchTime && curTime < endSchTime) {
                                if(isHeatingOrCoolingLimitsNull(d)){
                                    continue;
                                }
                                saveUserLimitChange("max and heating ", (d.getHeatingUserLimitMax()).intValue(), roomRef);
                                saveUserLimitChange("min and heating ", (d.getHeatingUserLimitMin()).intValue(), roomRef);
                                saveUserLimitChange("max and cooling ", (d.getCoolingUserLimitMax()).intValue(), roomRef);
                                saveUserLimitChange("min and cooling ", (d.getCoolingUserLimitMin()).intValue(), roomRef);
                                saveDeadBandChange("heating", d.getHeatingDeadBand(), roomRef);
                                saveDeadBandChange("cooling", d.getCoolingDeadBand(), roomRef);
                            }else{
                                clearLevel10(roomRef);
                            }
                        }
                    }
                }
            } else if (equipSchedule.getMarkers().contains("specialschedule")) {
                Set<Schedule.Days> combinedSpecialSchedules = Schedule.combineSpecialSchedules(equip.getRoomRef().
                        replace("@", ""));
                if (ScheduleUtil.isCurrentMinuteUnderSpecialSchedule(combinedSpecialSchedules)) {
                    for (Schedule.Days splsched : combinedSpecialSchedules) {
                        int day = DateTime.now().dayOfWeek().get() - 1;
                        Calendar calender = Calendar.getInstance();
                        int hrs = calender.get(Calendar.HOUR_OF_DAY) * 60;
                        int min = calender.get(Calendar.MINUTE);
                        int curTime = hrs + min;
                        if (splsched.getDay() == day) {
                            int startSchTime = (splsched.getSthh() * 60) + splsched.getStmm();
                            int endSchTime = (splsched.getEthh() * 60) + splsched.getEtmm();
                            if (curTime > startSchTime && curTime < endSchTime) {
                                saveUserLimitChange("max and heating ", (splsched.getHeatingUserLimitMax()).intValue(), roomRef);
                                saveUserLimitChange("min and heating ", (splsched.getHeatingUserLimitMin()).intValue(), roomRef);
                                saveUserLimitChange("max and cooling ", (splsched.getCoolingUserLimitMax()).intValue(), roomRef);
                                saveUserLimitChange("min and cooling ", (splsched.getCoolingUserLimitMin()).intValue(), roomRef);
                                saveDeadBandChange("heating", splsched.getHeatingDeadBand(), roomRef);
                                saveDeadBandChange("cooling", splsched.getCoolingDeadBand(), roomRef);
                            } else{
                                clearLevel10(roomRef);
                            }
                        }
                    }
                }
            }
        }
    }

    private void clearLevel10(String roomRef) {
        clearUserLimitChange("max and heating ", roomRef);
        clearUserLimitChange("min and heating ", roomRef);
        clearUserLimitChange("max and cooling ", roomRef);
        clearUserLimitChange("min and cooling ", roomRef);
        clearDeadBandChange("heating", roomRef);
        clearDeadBandChange("cooling", roomRef);
    }

    public static boolean isHeatingOrCoolingLimitsNull(Schedule.Days days) {
        return days.getHeatingUserLimitMax() == null || days.getHeatingUserLimitMin() == null ||
                days.getCoolingUserLimitMax() == null || days.getCoolingUserLimitMin() == null;
    }

    public void updateZoneOccupancy(CCUHsApi hayStack) {
        List<HashMap<Object, Object>> rooms = hayStack.readAllEntities("room");
        
        rooms.forEach( room -> {
            List<HashMap<Object, Object>> equips =
                hayStack.readAllEntities("equip and roomRef == \"" + Objects.requireNonNull(room.get("id")) + "\"");
            Occupancy occupancy;
            if (equips.isEmpty()) {
                occupancy = UNOCCUPIED;
            } else {
                Occupied scheduleOccupancy = getOccupiedModeCache(room.get("id").toString());
                boolean zoneOccupied = scheduleOccupancy != null ? scheduleOccupancy.isOccupied() : false;
                OccupiedTrigger occupiedTrigger = OccupiedTrigger.Occupied;
                UnoccupiedTrigger unoccupiedTrigger = UnoccupiedTrigger.Unoccupied;
                for (HashMap<Object, Object> equip : equips) {
                    String equipId = Objects.requireNonNull(equip.get("id")).toString();
                    OccupancyData equipOccData = equipOccupancy.get(equipId);
                    //Modbus equips do not have occupancy.
                    if (equipOccData == null) {
                        continue;
                    }
                    if (zoneOccupied) {
                        if (equipOccData.occupiedTrigger.ordinal() < occupiedTrigger.ordinal()) {
                            occupiedTrigger = equipOccData.occupiedTrigger;
                        }
                    } else {
                        if (equipOccData.unoccupiedTrigger.ordinal() < unoccupiedTrigger.ordinal()) {
                            unoccupiedTrigger = equipOccData.unoccupiedTrigger;
                        }
                    }
                }
                if (zoneOccupied) {
                    CcuLog.i(TAG_CCU_SCHEDULER,
                             "updateZoneOccupancy "+room.get("dis")+" : "+occupiedTrigger.toOccupancy());
                    occupancy = occupiedTrigger.toOccupancy();
                } else {
                    CcuLog.i(TAG_CCU_SCHEDULER, "updateZoneOccupancy "+room.get("dis")+" : "
                                                                    +unoccupiedTrigger.toOccupancy());
                    occupancy = unoccupiedTrigger.toOccupancy();
                }
            }
            if(zoneOccupancy.get(room.get("id").toString()) != occupancy){
                if(occupancy == UNOCCUPIED)
                    clearLevel10(room.get("id").toString());
            }
            zoneOccupancy.put(room.get("id").toString(), occupancy);
            hayStack.writeHisValByQuery("occupancy and state and roomRef == \""+room.get("id")+"\"",
                                        (double)occupancy.ordinal());
        });
    }
    
    
    public void updateSystemOccupancy(CCUHsApi hayStack) {
        
        currentOccupiedInfo = null;
        systemOccupancy = UNOCCUPIED;
        if (L.ccu().systemProfile == null || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT) {
            CcuLog.i(TAG_CCU_SCHEDULER, " Skip updateSystemOccupancy for Default System Profile ");
            return;
        }
        Map<String, OccupancyData> ahuServedEquipsOccupancy = new HashMap<>();
        
        equipOccupancy.keySet().forEach( equipId -> {
            if (ScheduleUtil.isAHUServedEquip(hayStack.readMapById(equipId))) {
                ahuServedEquipsOccupancy.put(equipId, equipOccupancy.get(equipId));
            }
        });

        if(ahuServedEquipsOccupancy.size() == 0){
            systemOccupancy = NONE;
            return;
        }

        if (ScheduleUtil.areAllZonesBuildingLimitsBreached(ahuServedEquipsOccupancy)) {
            systemOccupancy = NO_CONDITIONING;
            postSystemOccupancy(CCUHsApi.getInstance());
            return;
        }
        
        if (ScheduleUtil.isAnyZoneEmergencyConditioning(ahuServedEquipsOccupancy)
                && L.ccu().systemProfile.getSystemController().isEmergencyMode()) {
            systemOccupancy = EMERGENCY_CONDITIONING;
            postSystemOccupancy(CCUHsApi.getInstance());
            return;
        }
        
        if (ScheduleUtil.areAllZonesInVacation(ahuServedEquipsOccupancy)) {
            if (ScheduleUtil.isAnyZoneForcedOccupied(ahuServedEquipsOccupancy)) {
                systemOccupancy = FORCEDOCCUPIED;
            }else {
                systemOccupancy = VACATION;
            }
            CcuLog.i(TAG_CCU_SCHEDULER, " In SystemVacation : systemOccupancy : "+systemOccupancy);
            postSystemOccupancy(CCUHsApi.getInstance());
            return;
        }
        
        if (ScheduleUtil.isAnyZoneOccupiedOrAutoAway(ahuServedEquipsOccupancy)) {
            systemOccupancy = OCCUPIED;
            currentOccupiedInfo = ScheduleUtil.getCurrentOccupied(occupiedHashMap, equipOccupancy);
            CcuLog.i(TAG_CCU_SCHEDULER, "updateSystemOccupancy occupied , currentOccupied "+currentOccupiedInfo);
        }
        
        if (systemOccupancy == UNOCCUPIED) {
            nextOccupiedInfo = ScheduleUtil.getNextOccupied(occupiedHashMap);
            if (nextOccupiedInfo != null) {
                CcuLog.i(TAG_CCU_SCHEDULER, "Next Occupied : "+nextOccupiedInfo);
                systemOccupancy = getSystemPreconditioningStatus(nextOccupiedInfo);
            }
        }
        
        if (systemOccupancy == UNOCCUPIED && ScheduleUtil.isAnyZoneForcedOccupied(ahuServedEquipsOccupancy)) {
            systemOccupancy = FORCEDOCCUPIED;
        }
        
        if (systemOccupancy == UNOCCUPIED && ScheduleUtil.isAnyZoneAutoForcedOccupied(ahuServedEquipsOccupancy)
                                        && !ScheduleUtil.areAllZonesAutoForcedOccupied(ahuServedEquipsOccupancy)) {
            systemOccupancy = AUTOFORCEOCCUPIED;
        }
        
        /*if (ScheduleUtil.areAllZonesInAutoAway(ahuServedEquipsOccupancy)) {
            systemOccupancy = AUTOAWAY;
        }*/
        
        if (ScheduleUtil.areAllZonesKeyCardAutoAway(ahuServedEquipsOccupancy)) {
            systemOccupancy = KEYCARD_AUTOAWAY;
        }
        postSystemOccupancy(CCUHsApi.getInstance());
        CcuLog.i(TAG_CCU_SCHEDULER, "updateSystemOccupancy : " + systemOccupancy);
    }

    private void postSystemOccupancy(CCUHsApi hayStack) {
        double systemOccupancyValue = CCUHsApi.getInstance().readHisValByQuery("point and system and his and occupancy and mode");
        if (systemOccupancyValue != systemOccupancy.ordinal()){
            Globals.getInstance().getApplicationContext().sendBroadcast(new Intent(ACTION_STATUS_CHANGE));
        }
        hayStack.writeHisValByQuery("point and system and his and occupancy and mode",
                (double) systemOccupancy.ordinal());
    }
    
    
    private static Occupancy getSystemPreconditioningStatus(Occupied nextOccupied) {
        
        double preconDegree = 0;
        double preconRate = CCUHsApi.getInstance().getPredictedPreconRate(L.ccu().systemProfile.getSystemEquipRef());
        SystemMode systemMode = SystemMode.values()[(int) TunerUtil.readSystemUserIntentVal("conditioning and mode")];
        
        if (nextOccupied != null) {
            if (L.ccu().systemProfile.getAverageTemp() > 0) {
                if (L.ccu().systemProfile.getSystemController()
                                         .getConditioningForecast(nextOccupied) == SystemController.State.COOLING) {
                    if(preconRate == 0)
                        preconRate = TunerUtil.readTunerValByQuery("cooling and precon and rate",
                                                                   L.ccu().systemProfile.getSystemEquipRef());
                    preconDegree = L.ccu().systemProfile.getAverageTemp() - nextOccupied.getCoolingVal();
                } else if (L.ccu().systemProfile.getSystemController()
                                                .getConditioningForecast(nextOccupied) == SystemController.State.HEATING) {
                    if(preconRate == 0)
                        preconRate = TunerUtil.readTunerValByQuery("heating and precon and rate",
                                                                   L.ccu().systemProfile.getSystemEquipRef());
                    preconDegree = nextOccupied.getHeatingVal() - L.ccu().systemProfile.getAverageTemp();
                }
            }
        }
        long millisToOccupancy = nextOccupied.getMillisecondsUntilNextChange();
        CcuLog.d(L.TAG_CCU_SCHEDULER, "preconRate : "+preconRate+" preconDegree: "+preconDegree);
        if ((systemMode != SystemMode.OFF)
            && (preconDegree > 0)
            && (millisToOccupancy > 0)
            && (preconDegree * preconRate * 60 * 1000 >= millisToOccupancy)) {
            return PRECONDITIONING;
        } else {
            double sysOccValue = CCUHsApi.getInstance().readHisValByQuery("point and system and his and occupancy and mode");
            Occupancy prevOccuStatus = Occupancy.values()[(int)sysOccValue];
            if((prevOccuStatus == PRECONDITIONING) && (systemMode != SystemMode.OFF))
                return PRECONDITIONING;
        }
        CcuLog.d(L.TAG_CCU_SCHEDULER, "getSystemPreconditioningStatus : "+UNOCCUPIED);
        return UNOCCUPIED;
    }
    
    //TODO-Schedules - Make sure the applySchedule has run before algo loops to ensure the cache is updated
    public Occupancy getEquipOccupancy(String equipRef) {
        return equipOccupancy.get(equipRef).occupancy;
    }
    
    public Occupancy getZoneOccupancy(String zoneRef) {
        return zoneOccupancy.get(zoneRef);
    }
    
    public Occupancy getSystemOccupancy() {
        return systemOccupancy;
    }
    
    public Occupied getNextOccupiedTimeInMillis(){
        return nextOccupiedInfo;
    }
    public Occupied getPrevOccupiedTimeInMillis(){
        if(currentOccupiedInfo != null)
            return currentOccupiedInfo;
        else {
            Occupied prevOccupied = null;
            for (Occupied occ : occupiedHashMap.values()) {
                Schedule.Days occDay = occ.getPreviouslyOccupiedSchedule();
                if (prevOccupied == null || ((occDay != null) && (occDay.getEthh() > prevOccupied.getPreviouslyOccupiedSchedule().getEthh()
                                                                  || (occDay.getEthh() == prevOccupied.getPreviouslyOccupiedSchedule().getEthh() && occDay.getEtmm() > prevOccupied.getPreviouslyOccupiedSchedule().getEtmm())) ))
                {
                    prevOccupied = occ;
                }
            }
            return prevOccupied;
        }
    }
    
    /**
     * Public method that returns the zone status string.
     * @param zoneId
     * @param equipId
     * @return
     */
    public String getZoneStatusMessage(String zoneId, String equipId) {

        if(AutoCommissioningUtil.isAutoCommissioningStarted()) {
            CcuLog.i(TAG_CCU_SCHEDULER, "Zone page status - AutoCommissioning is Started ");
            return "In Diagnostic Mode";
        }
        HashMap<Object, Object> equip = CCUHsApi.getInstance().readMapById(equipId);
        OccupancyData equipOccupancyData = equipOccupancy.get(equipId);
        if (equipOccupancyData == null) {
            //Scheduler hasn't run yet.
            String previousStatus = CCUHsApi.getInstance().readDefaultStrVal("point and scheduleStatus and equipRef " +
                                                                            "== \""+equipId+"\"");
            
            return previousStatus.isEmpty() ? "Loading Schedules" : previousStatus;
        }
        String status = equipOccupancyData.message;
        if (equip.containsKey("vrv")) {
            status += ", Group Address "+
                      CCUHsApi.getInstance().readHisValByQuery("groupAddress and equipRef == \""+equipId+"\"").intValue();
        }
        return status;
    }
    
    private OccupancyData getOccupiedData(OccupiedTrigger occupiedTrigger, String equipRef, CCUHsApi hayStack) {
        OccupancyData occupancyData = new OccupancyData();
        occupancyData.isOccupied = true;
        occupancyData.occupiedTrigger = occupiedTrigger;
        occupancyData.occupancy = occupiedTrigger.toOccupancy();
        occupancyData.message = getZoneStatusString(equipRef, occupancyData.occupancy, hayStack);
        return occupancyData;
    }
    
    private OccupancyData getUnoccupiedData(UnoccupiedTrigger unoccupiedTrigger, String equipRef, CCUHsApi hayStack) {
        OccupancyData occupancyData = new OccupancyData();
        occupancyData.isOccupied = false;
        occupancyData.unoccupiedTrigger = unoccupiedTrigger;
        occupancyData.occupancy = unoccupiedTrigger.toOccupancy();
        occupancyData.message = getZoneStatusString(equipRef, occupancyData.occupancy, hayStack);
        return occupancyData;
    }
    
    private OccupancyData getOccupancyData(EquipOccupancyHandler occupancyHandler, CCUHsApi hayStack) {
        if (occupancyHandler.isScheduleOccupied()) {
            return getOccupiedData(occupancyHandler.getCurrentOccupiedTrigger(), occupancyHandler.getEquipRef(),
                                   hayStack);
        } else {
            return getUnoccupiedData(occupancyHandler.getCurrentUnoccupiedTrigger(), occupancyHandler.getEquipRef(),
                                     hayStack);
        }
    }
    
    public void updateEquipScheduleStatus(Equip equip) {
        HashMap<Object, Object> scheduleStatusPoint = CCUHsApi.getInstance()
                            .readEntity("point and scheduleStatus and equipRef == \""+equip.getId()+ "\"");
        if (!scheduleStatusPoint.isEmpty()) {
            String hisZoneStatus = CCUHsApi.getInstance().readDefaultStrValById(scheduleStatusPoint.get("id").toString());
            int modeType = CCUHsApi.getInstance().readHisValByQuery("zone and hvacMode and roomRef" +
                    " == \"" + equip.getRoomRef() + "\"").intValue();
            TemperatureMode temperatureMode = TemperatureMode.values()[modeType];
            String currentZoneStatus = DesiredTempDisplayMode.setPointStatusMessage(getZoneStatusMessage(equip.getRoomRef(), equip.getId()), temperatureMode);
            if (!hisZoneStatus.equals(currentZoneStatus)) {
                CCUHsApi.getInstance().writeDefaultValById(scheduleStatusPoint.get("id").toString(), currentZoneStatus);
                if(scheduleDataInterface !=null){
                    String zoneId = Schedule.getZoneIdByEquipId(equip.getId());
                    scheduleDataInterface.refreshScreenbySchedule(equip.getGroup(),equip.getId(),zoneId);
                }
            } else {
                Log.d(L.TAG_CCU_SCHEDULER, "ScheduleStatus not changed for  "+equip.getDisplayName());
            }
        }
    }
    
    private String getZoneStatusString(String equipId, Occupancy curOccupancyMode, CCUHsApi hayStack){
        Equip equip = HSUtil.getEquip(hayStack, equipId);
        Occupied cachedOccupied = getOccupiedModeCache(equip.getRoomRef());
        CcuLog.i(TAG_CCU_SCHEDULER,
                 " getZoneStatusString "+equip.getDisplayName()+" "+cachedOccupied+" "+curOccupancyMode);

        if(AutoCommissioningUtil.isAutoCommissioningStarted()) {
            CcuLog.i(TAG_CCU_SCHEDULER, "Zone page status - AutoCommissioning is Started ");
            return "In Diagnostic Mode";
        }
        if(cachedOccupied == null) {
            CcuLog.i(TAG_CCU_SCHEDULER, "Occupied schedule not found "+equip.getDisplayName());
            return "No schedule configured";
        }
        if (curOccupancyMode == Occupancy.PRECONDITIONING) {
            return "In Preconditioning";
        }

        if(curOccupancyMode == NO_CONDITIONING){
            return "No Conditioning: Building Limits Breached";
        }
        
        if (curOccupancyMode == EMERGENCY_CONDITIONING) {
            return "In Emergency Conditioning[Building Limits breached]";
        }
        
        if (curOccupancyMode == KEYCARD_AUTOAWAY) {
            return "Keycard Autoaway";
        }
        
        if (curOccupancyMode == WINDOW_OPEN) {
            return "Door/Window Open";
        }
    
        if(curOccupancyMode == AUTOAWAY){
            return String.format("In Auto Away");
        }
    
        if(curOccupancyMode == OCCUPIED) {
            if (cachedOccupied.getCurrentlyOccupiedSchedule() == null){
                CcuLog.i(TAG_CCU_SCHEDULER, " getZoneStatusString , occupied but current schedule null");
                return "No schedule configured";
            }
                return String.format("In %s, changes to Energy saving range of %.1f-%.1f\u00B0F at %02d:%02d", "Occupied mode",
                        cachedOccupied.getHeatingVal() - cachedOccupied.getUnoccupiedZoneSetback(),
                        cachedOccupied.getCoolingVal() + cachedOccupied.getUnoccupiedZoneSetback(),
                        TimeUtil.getEndTimeHr(cachedOccupied.getCurrentlyOccupiedSchedule().getEthh(),cachedOccupied.getCurrentlyOccupiedSchedule().getEtmm()),
                        TimeUtil.getEndTimeMin(cachedOccupied.getCurrentlyOccupiedSchedule().getEthh(),cachedOccupied.getCurrentlyOccupiedSchedule().getEtmm()));
        }
        
        long th = ScheduleUtil.getTemporaryHoldExpiry(equip);
        CcuLog.i(TAG_CCU_SCHEDULER, " th "+th);
        if(curOccupancyMode == AUTOFORCEOCCUPIED) {
        
            if (th > 0) {
                DateTime et = new DateTime(th);
                int min = et.getMinuteOfHour();
                return String.format(Locale.US, "In Temporary Hold(AUTO) | till %s",
                                     et.getHourOfDay() + ":" + (min < 10 ? "0" + min : min));
            }
        }
        else if(curOccupancyMode == FORCEDOCCUPIED ) {
            if (th > 0) {
                DateTime et = new DateTime(th);
                int min = et.getMinuteOfHour();
                return String.format(Locale.US, "In Temporary Hold | till %s", et.getHourOfDay() + ":" + (min < 10 ?
                                                                                                             "0" + min : min));
            }
        }
        String statusString = "";
    
        if(cachedOccupied.getVacation() != null) {
            statusString = String.format(Locale.US, "In Energy saving %s till %s", "Vacation",
                                         cachedOccupied.getVacation().getEndDateString());
        } else {
            boolean isZoneTempDead = hayStack.readHisValByQuery("point and status and not ota and " +
                                                                              "his and  equipRef == \"" + equipId +
                                                                              "\"") == ZoneState.TEMPDEAD.ordinal();
            if(curOccupancyMode == PRECONDITIONING && !isZoneTempDead) {//Currently handled only for standalone
                if (cachedOccupied.getNextOccupiedSchedule() == null){
                    CcuLog.i(TAG_CCU_SCHEDULER,
                             "Preconditioning nextOccupied schedule not found "+equip.getDisplayName());
                    return "No schedule configured";
                }
                    statusString = String.format("In %s, changes to Energy saving range of %.1f-%.1f\u00B0F at %02d:%02d", "Preconditioning",
                            cachedOccupied.getHeatingVal() - cachedOccupied.getUnoccupiedZoneSetback(),
                            cachedOccupied.getCoolingVal() + cachedOccupied.getUnoccupiedZoneSetback(),
                            cachedOccupied.getNextOccupiedSchedule().getEthh(),
                            cachedOccupied.getNextOccupiedSchedule().getEtmm());
            
            } else {
                if (cachedOccupied.getNextOccupiedSchedule() == null) {
                    CcuLog.i(TAG_CCU_SCHEDULER, "Unoccupied schedule not found "+equip.getDisplayName());
                    return "No schedule configured";
                }
                    statusString = String.format("In Energy saving %s, changes to %.1f-%.1f\u00B0F at %02d:%02d", "Unoccupied mode",
                            cachedOccupied.getHeatingVal(),
                            cachedOccupied.getCoolingVal(),
                            cachedOccupied.getNextOccupiedSchedule().getSthh(),
                            cachedOccupied.getNextOccupiedSchedule().getStmm());
            }
        }
        CcuLog.i(TAG_CCU_SCHEDULER, "Invalid zone occupancy status  ");
        return statusString;
    }
    
    /**
     * Public method that returns the zone status string.
     */
    public String getZoneStatusMessage(String equipId) {
        HashMap<Object, Object> equip = CCUHsApi.getInstance().readMapById(equipId);
        String status = equipOccupancy.get( equipId).message;
        if (equip.containsKey("vrv")) {
            status += ", Group Address "+
                      CCUHsApi.getInstance().readHisValByQuery("groupAddress and equipRef == \""+equipId+"\"").intValue();
        }
        return status;
    }
    
    public String getSystemStatusString() {
        
        CcuLog.i(TAG_CCU_SCHEDULER, " getSystemStatusString systemOccupancy "+systemOccupancy+" currentOccupiedInfo "+currentOccupiedInfo);
        //This might happen when getSystemStatusString is called too early, even before the systemProfile is loaded.

        if(AutoCommissioningUtil.isAutoCommissioningStarted()) {
            CcuLog.i(TAG_CCU_SCHEDULER, "System page status - AutoCommissioning is Started");
            return "In Diagnostic Mode";
        }

        if (L.ccu().systemProfile == null) {
            return "Loading...";
        }
        
        if(L.ccu().systemProfile instanceof DefaultSystem)
            return "No Central equipment connected.";
        if(systemOccupancy == null || systemOccupancy == NONE) {
            CcuLog.i(TAG_CCU_SCHEDULER, " system occupancy null");
            return "No schedule configured";
        }

        if(systemOccupancy == NO_CONDITIONING){
            return "No Conditioning - Building Limits breached";
        }
        
        if (systemOccupancy == EMERGENCY_CONDITIONING) {
            return "In Emergency Conditioning[Building Limits breached]";
        }
    
        if (systemOccupancy == KEYCARD_AUTOAWAY) {
            return "Keycard AutoAway";
        }
        
        if (systemOccupancy == VACATION && systemOccupancy != FORCEDOCCUPIED) {
            Schedule activeSystemVacation = ScheduleUtil.getActiveSystemVacation();
            if (activeSystemVacation != null) {
                return "In Energy saving Vacation till "+activeSystemVacation.getEndDateString();
            } /*else if (activeZoneVacation != null) {
                return "In Energy saving Vacation till "+activeZoneVacation.getVacation().getEndDateString();
            }*/
            return "In Energy saving Vacation";
        }
        
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and mode and state and equipRef ==\""+L.ccu().systemProfile.getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        String epidemicString = (epidemicState != EpidemicState.OFF) ? "["+epidemicState.name()+"] " : "";
        if (L.ccu().systemProfile.getSystemController().isEmergencyMode()) {
            if (L.ccu().systemProfile.getSystemController().getSystemState() == SystemController.State.HEATING) {
                //return "Building Limit Breach | Emergency Heating turned ON";
                return "In Emergency Conditioning";
            } else if (L.ccu().systemProfile.getSystemController().getSystemState() == SystemController.State.COOLING) {
               // return "Building Limit Breach | Emergency Cooling turned ON";
                return "In Emergency Conditioning";
            }
        }
        
        switch (systemOccupancy) {
            case OCCUPIED:
                if (currentOccupiedInfo == null || currentOccupiedInfo.getCurrentlyOccupiedSchedule() == null){
                    CcuLog.i(TAG_CCU_SCHEDULER, " Occupied , but info does not exist");
                    return "No schedule configured";
                }
                return String.format(Locale.US, "%sIn %s | Changes to Energy saving Unoccupied mode at %02d:%02d",
                                     epidemicString,"Occupied mode",
                                     TimeUtil.getEndTimeHr(currentOccupiedInfo.getCurrentlyOccupiedSchedule().getEthh(),currentOccupiedInfo.getCurrentlyOccupiedSchedule().getEtmm()),
                                     TimeUtil.getEndTimeMin(currentOccupiedInfo.getCurrentlyOccupiedSchedule().getEthh(),currentOccupiedInfo.getCurrentlyOccupiedSchedule().getEtmm()));
            
            case PRECONDITIONING:
                return "In Preconditioning";
            
            case AUTOAWAY:
                return "In AutoAway";
            
            case UNOCCUPIED:
                if (ScheduleUtil.isAnyZoneAutoAway(equipOccupancy)) {
                    return "In AutoAway";
                }
                if (nextOccupiedInfo == null || nextOccupiedInfo.getNextOccupiedSchedule() == null ){
                    CcuLog.i(TAG_CCU_SCHEDULER, " Unoccupied and info does not exist");
                    return "In Unoccupied Mode";
                }
                    return String.format("%sIn Energy saving %s | Changes to %.1f-%.1f\u00B0F at %02d:%02d",epidemicString, "Unoccupied mode",
                            nextOccupiedInfo.getHeatingVal(),
                            nextOccupiedInfo.getCoolingVal(),
                            nextOccupiedInfo.getNextOccupiedSchedule().getSthh(),
                            nextOccupiedInfo.getNextOccupiedSchedule().getStmm());
            case FORCEDOCCUPIED:
                DateTime et = new DateTime(ScheduleUtil.getSystemTemporaryHoldExpiry());
                int min = et.getMinuteOfHour();
                return String.format("%sIn Temporary Hold | till %s",epidemicString, et.getHourOfDay()+":"+(min < 10 ? "0"+min : min));

            case AUTOFORCEOCCUPIED:
                DateTime tempHoldExpiry = new DateTime(ScheduleUtil.getSystemTemporaryHoldExpiry());
                int mins = tempHoldExpiry.getMinuteOfHour();
                return String.format("%sIn Temporary Hold(AUTO) | till %s",epidemicString, tempHoldExpiry.getHourOfDay()+":"+(mins < 10 ? "0"+mins : mins));
            
            
        }
        return "";
    }
    
    public String getVacationStateString(String zoneId) {
        Occupied cachedOccupied = getOccupiedModeCache(zoneId);
        
        if(cachedOccupied == null) {
            return "No schedule configured";
        }
        else if(cachedOccupied.getVacation() != null) {
            return "Active Vacation";
        }
        else {
            return "No Active Vacation";
        }
    }
    
    public double getSystemCoolingDesiredTemp(){
        double setback = CCUHsApi.getInstance().readPointPriorityValByQuery("default and unocc and setback");
        if(currentOccupiedInfo != null)
            return (systemOccupancy == UNOCCUPIED || systemOccupancy == VACATION) ?
                        currentOccupiedInfo.getCoolingVal() + setback : currentOccupiedInfo.getCoolingVal();
        else if(nextOccupiedInfo != null)
            return (systemOccupancy == UNOCCUPIED || systemOccupancy == VACATION)?
                        nextOccupiedInfo.getCoolingVal() + setback : nextOccupiedInfo.getCoolingVal();
        else return 0;
    }
    
    public double getSystemHeatingDesiredTemp(){
        double setback = CCUHsApi.getInstance().readPointPriorityValByQuery("default and unocc and setback");
        if(currentOccupiedInfo != null)
            return (systemOccupancy == UNOCCUPIED || systemOccupancy == VACATION) ?
                        currentOccupiedInfo.getHeatingVal() - setback : currentOccupiedInfo.getHeatingVal();
        else if (nextOccupiedInfo != null)
            return (systemOccupancy == UNOCCUPIED || systemOccupancy == VACATION) ?
                        nextOccupiedInfo.getHeatingVal() - setback : nextOccupiedInfo.getHeatingVal();
        else return 0;
    }

    /**
     * Below method accepts zoneId and check whether that zone follows
     *  special schedule or not at that minute
     * @param zoneId
     * @return
     */
    public static String getScheduleStateString(String zoneId){
        Set<Schedule.Days> combinedSpecialSchedules =  Schedule.combineSpecialSchedules(zoneId.
                replace("@", ""));
        if(isCurrentMinuteUnderSpecialSchedule(combinedSpecialSchedules)){
            return "Active Schedule";
        }else{
            return "No Active Schedule";
        }
    }

    private void saveUserLimitChange(String tag, int value, String roomRef) {
        HashMap<Object, Object> userLimit =
                CCUHsApi.getInstance().readEntity("schedulable and point and limit and user and " + tag + "and roomRef == \"" + roomRef + "\"" );
        HSUtil.writeValLevel10(userLimit, value);
    }

    private void saveDeadBandChange(String tag, double value, String roomRef) {
        HashMap<Object, Object> deadBand =
                CCUHsApi.getInstance().readEntity("schedulable and point and " +tag+ " and deadband and roomRef == \"" + roomRef + "\"" );
        HSUtil.writeValLevel10(deadBand, value);
    }

    private void updateUnOccupiedSetBackPoint(double value, String roomRef) {
        HashMap<Object, Object> unOccupiedZoneSetBack =
                CCUHsApi.getInstance().readEntity("schedulable and unoccupied and zone and roomRef == \"" + roomRef + "\"" );
        HSUtil.writeValLevel10(unOccupiedZoneSetBack, value);
    }

    private void clearUserLimitChange(String tag,  String roomRef) {
        HashMap<Object, Object> userLimit =
                CCUHsApi.getInstance().readEntity("schedulable and point and limit and user and " + tag + "and roomRef == \"" + roomRef + "\"" );
        CCUHsApi.getInstance().clearPointArrayLevel(userLimit.get("id").toString(), HayStackConstants.USER_APP_WRITE_LEVEL,false);
    }

    private void clearDeadBandChange(String tag, String roomRef) {
        HashMap<Object, Object> deadBand =
                CCUHsApi.getInstance().readEntity("schedulable and point and " +tag+ " and deadband and roomRef == \"" + roomRef + "\"" );
        CCUHsApi.getInstance().clearPointArrayLevel(deadBand.get("id").toString(),HayStackConstants.USER_APP_WRITE_LEVEL,false);
    }
}
