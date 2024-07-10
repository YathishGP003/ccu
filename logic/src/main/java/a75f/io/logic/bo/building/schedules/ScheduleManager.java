package a75f.io.logic.bo.building.schedules;

import static a75f.io.domain.api.DomainName.coolingPreconditioningRate;
import static a75f.io.domain.api.DomainName.heatingPreconditioningRate;
import static a75f.io.logic.L.TAG_CCU_SCHEDULER;
import static a75f.io.logic.bo.building.schedules.Occupancy.AUTOAWAY;
import static a75f.io.logic.bo.building.schedules.Occupancy.AUTOFORCEOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.DEMAND_RESPONSE_OCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.DEMAND_RESPONSE_UNOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.EMERGENCY_CONDITIONING;
import static a75f.io.logic.bo.building.schedules.Occupancy.FORCEDOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.KEYCARD_AUTOAWAY;
import static a75f.io.logic.bo.building.schedules.Occupancy.NONE;
import static a75f.io.logic.bo.building.schedules.Occupancy.NO_CONDITIONING;
import static a75f.io.logic.bo.building.schedules.Occupancy.OCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.PRECONDITIONING;
import static a75f.io.logic.bo.building.schedules.Occupancy.UNOCCUPIED;
import static a75f.io.logic.bo.building.schedules.Occupancy.VACATION;
import static a75f.io.logic.bo.building.schedules.Occupancy.WINDOW_OPEN;
import static a75f.io.logic.bo.building.schedules.ScheduleUtil.ACTION_STATUS_CHANGE;
import static a75f.io.logic.bo.building.schedules.ScheduleUtil.isCurrentMinuteUnderSpecialSchedule;

import android.content.Intent;

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
import a75f.io.logic.autocommission.AutoCommissioningUtil;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.erm.EmrProfile;
import a75f.io.logic.bo.building.hyperstatmonitoring.HyperStatMonitoringProfile;
import a75f.io.logic.bo.building.modbus.ModbusProfile;
import a75f.io.logic.bo.building.plc.PlcProfile;
import a75f.io.logic.bo.building.schedules.occupancy.DemandResponse;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabExternalAhu;
import a75f.io.logic.bo.building.system.vav.VavAdvancedHybridRtu;
import a75f.io.logic.bo.building.system.vav.VavExternalAhu;
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtuWithVfd;
import a75f.io.logic.bo.util.DemandResponseMode;
import a75f.io.logic.bo.util.DesiredTempDisplayMode;
import a75f.io.logic.bo.util.TemperatureMode;
import a75f.io.logic.interfaces.BuildingScheduleListener;
import a75f.io.logic.interfaces.ZoneDataInterface;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.util.RxjavaUtil;

public class ScheduleManager {



    private static ScheduleManager instance = null;


    private final HashMap<String, Occupied> occupiedHashMap = new HashMap<>();
    private final Map<String, ZoneOccupancyData> zoneOccupancy = new HashMap<>();
    private final Map<String, OccupancyData> equipOccupancy = new ConcurrentHashMap<>();

    private Occupied currentOccupiedInfo = null;
    private Occupied nextOccupiedInfo = null;
    private Occupancy systemOccupancy = null;

    //TODO-Schedules - To be moved
    private ZoneDataInterface scheduleDataInterface = null;
    private ZoneDataInterface zoneDataInterface     = null;
    private BuildingScheduleListener buildingScheduleListener = null;

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
    public void setBuildingScheduleListener(BuildingScheduleListener in) {
        buildingScheduleListener = in;
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
        
        double occuStatus = CCUHsApi.getInstance().readHisValByQuery("point and (occupancy or occupied) and mode and equipRef == \""+equip.getId()+"\"");

        double heatingDeadBand ;
        double coolingDeadBand ;
        double setback ;

        ArrayList<HashMap<Object , Object>> isSchedulableAvailable = CCUHsApi.getInstance().readAllSchedulable();
        HashMap<Object,Object> hDBMap = CCUHsApi.getInstance().readEntity("zone and heating and deadband and roomRef == \"" + equip.getRoomRef() + "\"");
        if (!isSchedulableAvailable.isEmpty() && !hDBMap.isEmpty()) {
            heatingDeadBand = CCUHsApi.getInstance().readPointPriorityValByQuery("zone and heating and deadband and roomRef == \"" + equip.getRoomRef() + "\"");
            coolingDeadBand = CCUHsApi.getInstance().readPointPriorityValByQuery("zone and cooling and deadband and roomRef == \"" + equip.getRoomRef() + "\"");
            setback = CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and zone and unoccupied and setback and roomRef == \"" + equip.getRoomRef() + "\"");
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
        equipOccupancy.clear();
        ArrayList<Schedule> activeVacationSchedules = CCUHsApi.getInstance().getSystemSchedule(true);

        Schedule activeSystemVacation = ScheduleUtil.getActiveVacation(activeVacationSchedules);

        CcuLog.d(TAG_CCU_SCHEDULER, " #### processSchedules activeSystemVacation ####" + activeSystemVacation);

        //Read all equips
        ArrayList<HashMap<Object, Object>> equips = CCUHsApi.getInstance()
                .readAllEntities("equip and zone and not pid and not modbus and not emr and not monitoring");
        for(HashMap<Object,Object> hs : equips) {
            try {
                Equip equip = new Equip.Builder().setHashMap(hs).build();
                if (equip != null) {
                    CcuLog.d(L.TAG_CCU_SCHEDULER, " processSchedules " + equip.getDisplayName());
                    processScheduleForEquip(equip, activeSystemVacation);
                }
            } catch (Exception e) {
                CcuLog.e(TAG_CCU_SCHEDULER, "Error in processSchedules for equip " + e);
                e.printStackTrace();
            }
        }

        Set<ZoneProfile> zoneProfiles = new HashSet<>(L.ccu().zoneProfiles);
        updateOccupancy(CCUHsApi.getInstance(), zoneProfiles);
        updateDesiredTemp(zoneProfiles);

        if (buildingScheduleListener != null) {
            buildingScheduleListener.refreshScreen();
        }

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

            if (profile instanceof ModbusProfile
                    || profile instanceof HyperStatMonitoringProfile
                    || profile instanceof PlcProfile
                    || profile instanceof EmrProfile
            ) {
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
            CcuLog.d(L.TAG_CCU_SCHEDULER, "Equip "+equip.getDisplayName()+" activeZoneVacationSchedules "+activeZoneVacationSchedules.size());
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
        CcuLog.d(L.TAG_CCU_SCHEDULER, " Equip "+equip.getDisplayName());

        ArrayList<Schedule> activeVacationSchedules = CCUHsApi.getInstance().getSystemSchedule(true);
        Schedule activeSystemVacation = ScheduleUtil.getActiveVacation(activeVacationSchedules);

        processScheduleForEquip(equip, activeSystemVacation);

        updateEquipScheduleStatus(equip);
        updateSystemOccupancy(CCUHsApi.getInstance());
    }

    public void updateOccupancy(CCUHsApi hayStack, Set<ZoneProfile> zoneProfiles) {
        boolean drActivated = DemandResponseMode.isDRModeActivated(hayStack);
        CcuLog.i(TAG_CCU_SCHEDULER, "updateOccupancy : ScheduleManager");
        for (ZoneProfile profile : zoneProfiles) {
            if (profile instanceof ModbusProfile) {
                continue;
            }
            try {
                profile.updateOccupancy(hayStack, drActivated);
                EquipOccupancyHandler occupancyHandler = profile.getEquipOccupancyHandler();
                OccupancyData occupancyData = getOccupancyData(occupancyHandler, CCUHsApi.getInstance());
                CcuLog.i(TAG_CCU_SCHEDULER,
                        "Updated equipOccupancy "+profile.getEquip().getDisplayName()+" : "+occupancyData.occupancy);

                equipOccupancy.put(occupancyHandler.getEquipRef(), occupancyData);
            }catch (Exception e){
                CcuLog.e(TAG_CCU_SCHEDULER, "Error in updateOccupancy for profile "+e);
                e.printStackTrace();
            }

        }

        updateZoneOccupancy(hayStack, drActivated);
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
            try {
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
                profile.getEquipScheduleHandler().updateDesiredTemp(currentOccupiedMode, updatedOccupancy.occupancy, equipSchedule, updatedOccupancy);
                if ((zoneDataInterface != null) /*&& (cachedOccupied != null)*/) {
                    zoneDataInterface.refreshDesiredTemp(equip.getGroup(), "",
                            "", equip.getRoomRef());
                }
            }catch (Exception e) {
                CcuLog.e(TAG_CCU_SCHEDULER, "Error in updateDesiredTemp for profile " + e);
                e.printStackTrace();
            }
        }
    }

    public void updateLimitsAndDeadBand() {

        RxjavaUtil.executeBackground(()->{
            try {
                for (ZoneProfile profile : L.ccu().zoneProfiles) {
                    Equip equip = profile.getEquip();
                    String roomRef = equip.getRoomRef();

                    if (profile instanceof ModbusProfile
                            || profile instanceof HyperStatMonitoringProfile
                            || profile instanceof PlcProfile
                            || profile instanceof EmrProfile
                    ) {
                        continue;
                    }

                    Schedule equipSchedule = Schedule.getScheduleForZoneScheduleProcessing(equip.getRoomRef()
                            .replace("@", ""));
                    if (equipSchedule == null) {
                        continue;
                    }
                    ArrayList<Schedule.Days> mDays = equipSchedule.getDays();
                    if (!equipSchedule.getMarkers().contains("specialschedule")) {
                        if (!equipSchedule.getMarkers().contains(Tags.FOLLOW_BUILDING)) {
                            if (equipSchedule.getUnoccupiedZoneSetback() != null)
                                updateUnOccupiedSetBackPoint(equipSchedule.getUnoccupiedZoneSetback(), roomRef);
                            Occupied occ = equipSchedule.getCurrentValues();
                            if (occ == null)
                                break;
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
                                        if (isHeatingOrCoolingLimitsNull(d)) {
                                            continue;
                                        }
                                        saveUserLimitChange("max and heating ", (d.getHeatingUserLimitMax()).intValue(), roomRef);
                                        saveUserLimitChange("min and heating ", (d.getHeatingUserLimitMin()).intValue(), roomRef);
                                        saveUserLimitChange("max and cooling ", (d.getCoolingUserLimitMax()).intValue(), roomRef);
                                        saveUserLimitChange("min and cooling ", (d.getCoolingUserLimitMin()).intValue(), roomRef);
                                        saveDeadBandChange("heating", d.getHeatingDeadBand(), roomRef);
                                        saveDeadBandChange("cooling", d.getCoolingDeadBand(), roomRef);
                                    } else {
                                        clearLevel10(roomRef);
                                    }
                                }
                            }
                        } else {
                            clearLevel10(roomRef);
                            clearUnoccupiedSetbackChange(roomRef);
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
                                    } else {
                                        clearLevel10(roomRef);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e){
                CcuLog.e(TAG_CCU_SCHEDULER, "Update Limits and Dead band error !"+e);
                e.printStackTrace();
            }
        });
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

    /**
     * @brief Updates the occupancy status of each zone based on the equipment occupancy within that zone.
     *
     * | Module 1                    | Module 2                    | Module 3                    | Zone Level Status Messages    |
     * |-----------------------------|-----------------------------|-----------------------------|-------------------------------|
     * | Occupied                    | Occupied                    | Occupied                    | In Occupied                   |
     * | Preconditioning             | Preconditioning             | Preconditioning             | In Preconditioning            |
     * | Emergency Conditioning      | Occupied                    | Occupied                    | Emergency Conditioning        |
     * | KeyCard Autoaway            | Occupied                    | Occupied                    | KeyCard Autoaway              |
     * | Door/Window Open            | Occupied                    | Occupied                    | Door/Window Open              |
     * | Autoaway                    | Occupied                    | Occupied                    | In Autoaway                   |
     * | Autoaway                    | Autoaway                    | Autoaway                    | In Autoaway                   |
     * | Auto forced occupied        | Unoccupied                  | Unoccupied                  | Temporary Hold Auto           |
     * | Emergency Conditioning      | Unoccupied                  | Unoccupied                  | Emergency Conditioning        |
     * | Emergency Conditioning      | Unoccupied                  | Unoccupied                  | Emergency Conditioning        |
     * | Auto forced occupied        | Auto forced occupied        | Auto forced occupied        | Temporary Hold,               |
     * | Forced Occupied             | Unoccupied                  | Unoccupied                  | In Temporary Hold Manual      |
     *
     * This method reads all rooms from the CCUHsApi and determines the occupancy status for each zone based on
     * the occupancy status of the equipment within each room. If all equipment in a room are unoccupied, the room
     * is marked as unoccupied. If any equipment in a room is occupied, the room is marked as occupied based on
     * the most significant occupancy trigger. The occupancy status is then logged and written back to the CCUHsApi.
     *
     * @param hayStack An instance of CCUHsApi used to interact with the occupancy data.
     */
    public void updateZoneOccupancy(CCUHsApi hayStack, boolean drModeActive) {
        List<HashMap<Object, Object>> rooms = hayStack.readAllEntities("room");

        rooms.forEach( room -> {
            try {
                ZoneOccupancyData zoneOccupancyData = new ZoneOccupancyData();
                List<HashMap<Object, Object>> equips =
                        hayStack.readAllEntities("equip and roomRef == \"" + Objects.requireNonNull(room.get("id")) + "\"");
                if (equips.isEmpty()) {
                    zoneOccupancyData.occupancy = UNOCCUPIED;
                } else {
                    Occupied scheduleOccupancy = getOccupiedModeCache(room.get("id").toString());
                    boolean zoneOccupied = scheduleOccupancy != null ? scheduleOccupancy.isOccupied() : false;
                    OccupiedTrigger occupiedTrigger = OccupiedTrigger.Occupied;
                    OccupiedTrigger occupiedTriggerDR = OccupiedTrigger.Occupied;
                    UnoccupiedTrigger unoccupiedTrigger = UnoccupiedTrigger.Unoccupied;
                    UnoccupiedTrigger unoccupiedTriggerDR = UnoccupiedTrigger.Unoccupied;
                    for (HashMap<Object, Object> equip : equips) {
                        String equipId = Objects.requireNonNull(equip.get("id")).toString();
                        OccupancyData equipOccData = equipOccupancy.get(equipId);
                        //Modbus equips do not have occupancy.
                        if (equipOccData == null) {
                            continue;
                        }
                        if (zoneOccupied) {
                            if (equipOccData.occupiedTrigger.ordinal() <= occupiedTrigger.ordinal()) {
                                occupiedTrigger = equipOccData.occupiedTrigger;
                                zoneOccupancyData.message = equipOccData.message;
                                zoneOccupancyData.occupiedTrigger = equipOccData.occupiedTrigger;
                            }
                        } else {
                            if (equipOccData.unoccupiedTrigger.ordinal() <= unoccupiedTrigger.ordinal()) {
                                unoccupiedTrigger = equipOccData.unoccupiedTrigger;
                                zoneOccupancyData.message = equipOccData.message;
                                zoneOccupancyData.unoccupiedTrigger = equipOccData.unoccupiedTrigger;
                            }
                        }

                        // During DR mode, we need to update the TriggerDR field in zoneOccupancyData
                        if (drModeActive) {
                            if(zoneOccupied) {
                                if (equipOccData.occupiedTriggerDR.ordinal() <= occupiedTriggerDR.ordinal()) {
                                    occupiedTriggerDR = equipOccData.occupiedTriggerDR;
                                    zoneOccupancyData.occupiedTriggerDR = equipOccData.occupiedTriggerDR;
                                    equipOccData.occupancyDR = zoneOccupancyData.occupiedTriggerDR.toOccupancy();
                                }
                            } else {
                                if(equipOccData.unoccupiedTriggerDR.ordinal() <= unoccupiedTriggerDR.ordinal()) {
                                    unoccupiedTriggerDR = equipOccData.unoccupiedTriggerDR;
                                    zoneOccupancyData.unoccupiedTriggerDR = equipOccData.unoccupiedTriggerDR;
                                    equipOccData.occupancyDR = zoneOccupancyData.unoccupiedTriggerDR.toOccupancy();
                                }
                            }
                        }
                    }
                    if (zoneOccupied) {
                        CcuLog.i(TAG_CCU_SCHEDULER,
                                "updateZoneOccupancy " + room.get("dis") + " : " + occupiedTrigger.toOccupancy());
                        zoneOccupancyData.occupancy = occupiedTrigger.toOccupancy();
                        zoneOccupancyData.occupancyDR = occupiedTriggerDR.toOccupancy();
                    } else {
                        CcuLog.i(TAG_CCU_SCHEDULER, "updateZoneOccupancy " + room.get("dis") + " : "
                                + unoccupiedTrigger.toOccupancy());
                        zoneOccupancyData.occupancy = unoccupiedTrigger.toOccupancy();
                        zoneOccupancyData.occupancyDR = unoccupiedTriggerDR.toOccupancy();
                    }
                    // Check if we need to update occupancy of other modules in case of multi-modules in 1 zone
                    if(equips.size() > 1) {
                        for (HashMap<Object, Object> equip : equips) {
                            String equipId = Objects.requireNonNull(equip.get("id")).toString();
                            OccupancyData equipOccData = equipOccupancy.get(equipId);
                            if(zoneOccupied) {
                                if(zoneOccupancyData.occupiedTrigger.ordinal() <= equipOccData.occupiedTrigger.ordinal()) {
                                    equipOccData.occupancy = zoneOccupancyData.occupancy;
                                    // Skip updating message for now if dr since it will be updated later below
                                    if(!drModeActive) equipOccData.message = zoneOccupancyData.message;
                                    equipOccData.occupiedTrigger = zoneOccupancyData.occupiedTrigger;
                                }
                                // Check if the zone is in demand response mode. Update the zone message accordingly
                                if(drModeActive) {
                                    // Copy the message from the equipment with the highest priority
                                    if(zoneOccupancyData.occupiedTriggerDR.ordinal() == equipOccData.occupiedTriggerDR.ordinal()) {
                                        zoneOccupancyData.message = equipOccData.message;
                                    }
                                    if(zoneOccupancyData.occupiedTriggerDR.ordinal() <= equipOccData.occupiedTriggerDR.ordinal()) {
                                        equipOccData.occupancyDR = zoneOccupancyData.occupancyDR;
                                        // For portals, all the modules should have same message in case of multimodule
                                        equipOccData.message = zoneOccupancyData.message;
                                        equipOccData.occupiedTriggerDR = zoneOccupancyData.occupiedTriggerDR;
                                    }
                                }
                            } else {
                                if(zoneOccupancyData.unoccupiedTrigger.ordinal() <= equipOccData.unoccupiedTrigger.ordinal()) {
                                    equipOccData.occupancy = zoneOccupancyData.occupancy;
                                    // Skip updating message for now if dr since it will be updated later below
                                    if(!drModeActive) equipOccData.message = zoneOccupancyData.message;
                                    equipOccData.unoccupiedTrigger = zoneOccupancyData.unoccupiedTrigger;
                                }
                                // Check if the zone is in demand response mode. Update the zone message accordingly
                                if(drModeActive) {
                                    // Copy the message from the equipment with the highest priority
                                    if(zoneOccupancyData.unoccupiedTriggerDR.ordinal() == equipOccData.unoccupiedTriggerDR.ordinal()) {
                                        zoneOccupancyData.message = equipOccData.message;
                                    }
                                    if (zoneOccupancyData.unoccupiedTriggerDR.ordinal() <= equipOccData.unoccupiedTriggerDR.ordinal()) {
                                        equipOccData.occupancyDR = zoneOccupancyData.occupancyDR;
                                        // For portals, all the modules should have same message in case of multimodule
                                        equipOccData.message = zoneOccupancyData.message;
                                        equipOccData.unoccupiedTriggerDR = zoneOccupancyData.unoccupiedTriggerDR;
                                    }
                                }
                            }
                            // Update the occupancy status of the equipment
                            equipOccupancy.put(equipId, equipOccData);
                        }
                    }
                }

                // Ensure room.get("id") is not null and perform operations safely
                String roomId = room.get("id") != null ? Objects.requireNonNull(room.get("id")).toString() : null;
                if (roomId != null) {
                    ZoneOccupancyData currentOccupancyData = zoneOccupancy.get(roomId);
                    if (currentOccupancyData != null && currentOccupancyData.occupancy != zoneOccupancyData.occupancy) {
                        if (zoneOccupancyData.occupancy == Occupancy.UNOCCUPIED) {
                            clearLevel10(roomId);
                        }
                    }
                    zoneOccupancy.put(roomId, zoneOccupancyData);
                }

                hayStack.writeHisValByQuery("occupancy and state and roomRef == \"" + room.get("id") + "\"",
                        (double) zoneOccupancyData.occupancy.ordinal());
            } catch (Exception e) {
                CcuLog.e(TAG_CCU_SCHEDULER, "Error in updateZoneOccupancy for room " + e);
                e.printStackTrace();
            }
        });
    }


    public void updateSystemOccupancy(CCUHsApi hayStack) {

        currentOccupiedInfo = null;
        systemOccupancy = UNOCCUPIED;
        if (L.ccu().systemProfile == null || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT) {
            CcuLog.i(TAG_CCU_SCHEDULER, " Skip updateSystemOccupancy for Default System Profile ");
            return;
        }
        Map<String, OccupancyData> ahuServedEquipsOccupancy = getAhuServedEquipsOccupancy(
                equipOccupancy, hayStack);


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
            currentOccupiedInfo = ScheduleUtil.getCurrentOccupied(occupiedHashMap);
            CcuLog.i(TAG_CCU_SCHEDULER, "updateSystemOccupancy occupied , currentOccupied "+currentOccupiedInfo);
        }


        if (systemOccupancy == UNOCCUPIED || systemOccupancy == DEMAND_RESPONSE_UNOCCUPIED) {
            nextOccupiedInfo = ScheduleUtil.getNextOccupied(occupiedHashMap);
            if (nextOccupiedInfo != null) {
                CcuLog.i(TAG_CCU_SCHEDULER, "Next Occupied : "+nextOccupiedInfo);
                systemOccupancy = getSystemPreconditioningStatus(nextOccupiedInfo, hayStack);
            }
        }
        if (ScheduleUtil.isAnyZoneInDemandResponse(ahuServedEquipsOccupancy)) {
            currentOccupiedInfo = ScheduleUtil.getCurrentOccupied(occupiedHashMap);
            nextOccupiedInfo = ScheduleUtil.getNextOccupied(occupiedHashMap);
            systemOccupancy = ScheduleUtil.getDemandResponseMode(ahuServedEquipsOccupancy);
            postSystemOccupancy(CCUHsApi.getInstance());
        }
        if (systemOccupancy == UNOCCUPIED && ScheduleUtil.isAnyZoneForcedOccupied(ahuServedEquipsOccupancy)) {
            systemOccupancy = FORCEDOCCUPIED;
        }
        
        if (systemOccupancy == UNOCCUPIED && ScheduleUtil.isAnyZoneAutoForcedOccupied(ahuServedEquipsOccupancy)) {
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

    private Map<String, OccupancyData> getAhuServedEquipsOccupancy(Map<String, OccupancyData> equipOccupancy, CCUHsApi hayStack) {
        Map<String, OccupancyData> ahuServedEquipsOccupancy = new HashMap<>();
        equipOccupancy.keySet().forEach( equipId -> {
            if (ScheduleUtil.isAHUServedEquip(hayStack.readMapById(equipId))) {
                ahuServedEquipsOccupancy.put(equipId, equipOccupancy.get(equipId));
            }
        });
        return ahuServedEquipsOccupancy;
    }

    private void postSystemOccupancy(CCUHsApi hayStack) {
        double systemOccupancyValue = CCUHsApi.getInstance().readHisValByQuery("point and system and his and occupancy and mode");
        if (systemOccupancyValue != systemOccupancy.ordinal()){
            Globals.getInstance().getApplicationContext().sendBroadcast(new Intent(ACTION_STATUS_CHANGE));
        }
        hayStack.writeHisValByQuery("point and system and his and occupancy and mode",
                (double) systemOccupancy.ordinal());
    }


    private static Occupancy getSystemPreconditioningStatus(Occupied nextOccupied, CCUHsApi hayStack) {

        double preconDegree = 0;
        double preconRate = CCUHsApi.getInstance().getPredictedPreconRate(L.ccu().systemProfile.getSystemEquipRef());
        SystemMode systemMode = SystemMode.values()[(int) TunerUtil.readSystemUserIntentVal("conditioning and mode")];

        if (nextOccupied != null) {
            if (L.ccu().systemProfile.getAverageTemp() > 0) {
                if (L.ccu().systemProfile.getSystemController()
                                         .getConditioningForecast(nextOccupied) == SystemController.State.COOLING) {
                    if(preconRate == 0)
                        preconRate = getCoolingPreconditioningRate();
                    preconDegree = L.ccu().systemProfile.getAverageTemp() - nextOccupied.getCoolingVal()
                            - DemandResponseMode.getDemandResponseSetBackIfActive(hayStack);
                } else if (L.ccu().systemProfile.getSystemController()
                                                .getConditioningForecast(nextOccupied) == SystemController.State.HEATING) {
                    if(preconRate == 0)
                        preconRate = getHeatingPreconditioningRate();

                    preconDegree = nextOccupied.getHeatingVal() - L.ccu().systemProfile.getAverageTemp()
                            - DemandResponseMode.getDemandResponseSetBackIfActive(hayStack);
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
        return DemandResponse.isDRModeActivated(hayStack) ? DEMAND_RESPONSE_UNOCCUPIED : UNOCCUPIED;
    }

    //TODO-Schedules - Make sure the applySchedule has run before algo loops to ensure the cache is updated
    public Occupancy getEquipOccupancy(String equipRef) {
        return equipOccupancy.get(equipRef).occupancy;
    }

    public ZoneOccupancyData getZoneOccupancyData(String zoneRef) {
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
     * @brief Retrieves the status message for a multi-module zone.
     *
     * This method checks if the system is currently in auto-commissioning mode. If so,
     * it returns a message indicating that the zone is in diagnostic mode. Otherwise,
     * it fetches the occupancy data for the specified zone and retrieves the associated
     * status message. If no specific message is found, it defaults to "Loading Schedules".
     *
     * @param zoneId The identifier for the zone whose status message is to be retrieved.
     * @return A string representing the status message of the specified zone.
     */
    public String getMultiModuleZoneStatusMessage(String zoneId) {
        if(AutoCommissioningUtil.isAutoCommissioningStarted()) {
            CcuLog.i(TAG_CCU_SCHEDULER, "Zone page status - AutoCommissioning is Started ");
            return "In Diagnostic Mode";
        }
        ZoneOccupancyData zoneOccupancyData = getZoneOccupancyData(zoneId);
        String status;
        if(zoneOccupancyData != null && zoneOccupancyData.message != null) {
            status = zoneOccupancyData.message.toString();
        } else {
            status = "Loading Schedules";
        }
        return status;
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

    private OccupancyData getOccupiedData(OccupiedTrigger occupiedTrigger, OccupiedTrigger drModeOccupiedTrigger, String equipRef, CCUHsApi hayStack) {
        OccupancyData occupancyData = new OccupancyData();
        occupancyData.isOccupied = true;
        occupancyData.occupiedTrigger = occupiedTrigger;
        occupancyData.occupiedTriggerDR = drModeOccupiedTrigger;
        occupancyData.occupancy = occupiedTrigger.toOccupancy();
        occupancyData.occupancyDR = drModeOccupiedTrigger.toOccupancy();
        occupancyData.message = getZoneStatusString(equipRef, occupancyData, hayStack);
        return occupancyData;
    }

    private OccupancyData getUnoccupiedData(UnoccupiedTrigger unoccupiedTrigger, UnoccupiedTrigger drModeOccupiedTrigger, String equipRef, CCUHsApi hayStack) {
        OccupancyData occupancyData = new OccupancyData();
        occupancyData.isOccupied = false;
        occupancyData.unoccupiedTrigger = unoccupiedTrigger;
        occupancyData.unoccupiedTriggerDR = drModeOccupiedTrigger;
        occupancyData.occupancy = unoccupiedTrigger.toOccupancy();
        occupancyData.occupancyDR = drModeOccupiedTrigger.toOccupancy();
        occupancyData.message = getZoneStatusString(equipRef, occupancyData, hayStack);
        return occupancyData;
    }

    private OccupancyData getOccupancyData(EquipOccupancyHandler occupancyHandler, CCUHsApi hayStack) {
        if (occupancyHandler.isScheduleOccupied()) {
            return getOccupiedData(occupancyHandler.getCurrentOccupiedTrigger(), occupancyHandler.getDRModeOccupiedTrigger(),
                    occupancyHandler.getEquipRef(), hayStack);
        } else {
            return getUnoccupiedData(occupancyHandler.getCurrentUnoccupiedTrigger(), occupancyHandler.getDRModeUnoccupiedTrigger(),
                    occupancyHandler.getEquipRef(), hayStack);
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
                CcuLog.d(L.TAG_CCU_SCHEDULER, "ScheduleStatus not changed for  "+equip.getDisplayName());
            }
        }
    }

    private String getZoneStatusString(String equipId, OccupancyData occupancyData, CCUHsApi hayStack){
        Occupancy curOccupancyMode = occupancyData.occupancy;
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
            if(DemandResponse.isDRModeActivated(hayStack)){
                return "In Demand Response | Preconditioning";
            }
            return "In Preconditioning";
        }

        if(curOccupancyMode == NO_CONDITIONING){
            return "No Conditioning: Building Limits Breached";
        }
        if (curOccupancyMode == EMERGENCY_CONDITIONING) {
            return  "In Emergency Conditioning[Building Limits breached]";
        }
        if (curOccupancyMode == KEYCARD_AUTOAWAY) {
            return "Keycard Autoaway";
        }

        if (curOccupancyMode == WINDOW_OPEN) {
            return "Door/Window Open";
        }

        if(curOccupancyMode == AUTOAWAY){
            if(DemandResponse.isDRModeActivated(hayStack)){
                return "In Auto Away | Demand Response";
            }
            return "In Auto Away";
        }

        if(curOccupancyMode == OCCUPIED) {
            if (cachedOccupied.getCurrentlyOccupiedSchedule() == null){
                CcuLog.i(TAG_CCU_SCHEDULER, " getZoneStatusString , occupied but current schedule null");
                return "No schedule configured";
            }
            return getOccupiedStatusMessage(cachedOccupied, "Occupied mode");
        }
        if(curOccupancyMode == DEMAND_RESPONSE_OCCUPIED) {
            if(occupancyData.occupancyDR == WINDOW_OPEN){
                return getOccupiedStatusMessage(cachedOccupied, "Demand Response | Door/Window Open");
            } else if(occupancyData.occupancyDR == KEYCARD_AUTOAWAY){
                return getOccupiedStatusMessage(cachedOccupied, "Demand Response | Keycard Autoaway");
            } else if(occupancyData.occupancyDR == AUTOAWAY){
                 return getOccupiedStatusMessage(cachedOccupied, "Demand Response | Auto Away");
            } else if (occupancyData.occupancyDR == EMERGENCY_CONDITIONING) {
                return getOccupiedStatusMessage(cachedOccupied, "Demand Response Occupied | Emergency Conditioning");
            }
            return getOccupiedStatusMessage(cachedOccupied, "Demand Response Occupied mode");
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
                if(DemandResponse.isDRModeActivated(hayStack)){
                    return String.format(Locale.US, "In Temporary Hold | Demand Response |" +
                            " till %s", et.getHourOfDay() + ":" + (min < 10 ? "0" + min : min));
                }
                return String.format(Locale.US, "In Temporary Hold | till %s", et.getHourOfDay() + ":" + (min < 10 ?
                        "0" + min : min));
            }
        }
        String statusString = "";

        if(cachedOccupied.getVacation() != null) {
            statusString = String.format(Locale.US, "In Energy saving %s till %s", "Vacation",
                    cachedOccupied.getVacation().getEndDateString());
            if(curOccupancyMode == DEMAND_RESPONSE_UNOCCUPIED) {
                statusString = getUnOccupiedStatusMessage(cachedOccupied, "Demand Response Unoccupied mode");
            }
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
                statusString = getUnOccupiedStatusMessage(cachedOccupied, "Preconditioning");

            } else {
                if (cachedOccupied.getNextOccupiedSchedule() == null) {
                    CcuLog.i(TAG_CCU_SCHEDULER, "Unoccupied schedule not found "+equip.getDisplayName());
                    return "No schedule configured";
                }
                if(curOccupancyMode == DEMAND_RESPONSE_UNOCCUPIED){
                    if(occupancyData.occupancyDR == WINDOW_OPEN) {
                        statusString = getUnOccupiedStatusMessage(cachedOccupied, "Demand Response | Door/Window Open");
                    } else if(occupancyData.occupancyDR == KEYCARD_AUTOAWAY) {
                        statusString = getUnOccupiedStatusMessage(cachedOccupied, "Demand Response | Keycard Autoaway");
                    } else if(occupancyData.occupancyDR == AUTOFORCEOCCUPIED){
                        statusString = getUnOccupiedStatusMessage(cachedOccupied, "Demand Response | Auto Forced Occupied");
                    } else if (occupancyData.occupancyDR == FORCEDOCCUPIED){
                        statusString = getUnOccupiedStatusMessage(cachedOccupied, "Demand Response | Forced Occupied");
                    }
                    else if (occupancyData.occupancyDR == EMERGENCY_CONDITIONING){
                        statusString = getUnOccupiedStatusMessage(cachedOccupied, "Demand Response Unoccupied | Emergency Conditioning");
                    } else if (occupancyData.occupancyDR == PRECONDITIONING){
                        return "In Demand Response | Preconditioning";
                    } else if ((equip.getMarkers().contains("vav") || equip.getMarkers().contains("dab")) &&
                            nextOccupiedInfo != null && getSystemPreconditioningStatus(nextOccupiedInfo,
                            hayStack) == PRECONDITIONING) {
                        return "In Demand Response | Preconditioning";
                    } else {
                        statusString = getUnOccupiedStatusMessage(cachedOccupied, "Demand Response Unoccupied mode");
                    }
                }else {
                    statusString = getUnOccupiedStatusMessage(cachedOccupied, "Unoccupied mode");
                }
            }
        }
        if (curOccupancyMode == EMERGENCY_CONDITIONING) {
            statusString = "In Emergency Conditioning[Building Limits breached]";
        }
        CcuLog.i(TAG_CCU_SCHEDULER, "Invalid zone occupancy status  ");
        return statusString;
    }

    /**
     * Generates a status message for unoccupied mode.
     *
     * This method constructs a formatted string indicating the current energy saving mode status,
     * the heating and cooling values, and the start time of the next occupied schedule.
     *
     * @param cachedOccupied The cached Occupied instance containing the current schedule and temperature settings.
     * @param modeStatus The string representing the current mode status.
     * @return A formatted status message string.
     */
    private static String getUnOccupiedStatusMessage(Occupied cachedOccupied, String modeStatus) {
        return String.format("In Energy saving %s, changes to %.1f-%.1f\u00B0F at %02d:%02d", modeStatus,
                cachedOccupied.getHeatingVal(),
                cachedOccupied.getCoolingVal(),
                cachedOccupied.getNextOccupiedSchedule().getSthh(),
                cachedOccupied.getNextOccupiedSchedule().getStmm());
    }

    /**
     * Constructs a status message for the Demand Response mode.
     *
     * This method formats a string message indicating the current mode status,
     * energy-saving temperature range, and the end time of the current schedule.
     *
     * @param cachedOccupied An instance of the Occupied class representing the current occupancy state.
     * @param modeStatus A string representing the current mode status (e.g., "Demand Response | Door/Window Open").
     * @return A formatted string message with the mode status and energy-saving details.
     */
    private static String getOccupiedStatusMessage(Occupied cachedOccupied, String modeStatus) {
        return String.format("In %s, changes to Energy saving range of %.1f-%.1f\u00B0F at %02d:%02d", modeStatus,
                cachedOccupied.getHeatingVal() - cachedOccupied.getUnoccupiedZoneSetback(),
                cachedOccupied.getCoolingVal() + cachedOccupied.getUnoccupiedZoneSetback(),
                TimeUtil.getEndTimeHr(cachedOccupied.getCurrentlyOccupiedSchedule().getEthh(), cachedOccupied.getCurrentlyOccupiedSchedule().getEtmm()),
                TimeUtil.getEndTimeMin(cachedOccupied.getCurrentlyOccupiedSchedule().getEthh(), cachedOccupied.getCurrentlyOccupiedSchedule().getEtmm()));
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

        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
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
                if(DemandResponse.isDRModeActivated(ccuHsApi)){
                    return "In Demand Response | Preconditioning";
                }
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

            case DEMAND_RESPONSE_OCCUPIED:
                if (currentOccupiedInfo == null){
                    CcuLog.i(TAG_CCU_SCHEDULER, "Demand Response Occupied , but info does not exist");
                    return "In Demand Response Occupied Mode";
                }
                return String.format(Locale.US, "%sIn %s | Changes to Energy saving Unoccupied mode at %02d:%02d",
                        epidemicString,"Demand Response Occupied Mode",
                        TimeUtil.getEndTimeHr(currentOccupiedInfo.getCurrentlyOccupiedSchedule().getEthh(),
                                currentOccupiedInfo.getCurrentlyOccupiedSchedule().getEtmm()),
                        TimeUtil.getEndTimeMin(currentOccupiedInfo.getCurrentlyOccupiedSchedule().getEthh(),
                                currentOccupiedInfo.getCurrentlyOccupiedSchedule().getEtmm()));

            case DEMAND_RESPONSE_UNOCCUPIED:
                if (nextOccupiedInfo == null || nextOccupiedInfo.getNextOccupiedSchedule() == null ){
                    CcuLog.i(TAG_CCU_SCHEDULER, " Demand Response Unoccupied and info does not exist");
                    return "In Demand Response Unoccupied Mode";
                }
                if(getSystemPreconditioningStatus(nextOccupiedInfo, ccuHsApi) == PRECONDITIONING) {
                    return "In Demand Response | Preconditioning";
                }
                return String.format("%sIn %s | Changes to Occupied mode at %02d:%02d",epidemicString, "Demand Response Unoccupied Mode",
                        nextOccupiedInfo.getNextOccupiedSchedule().getSthh(),
                        nextOccupiedInfo.getNextOccupiedSchedule().getStmm());

        }
        if (systemOccupancy == EMERGENCY_CONDITIONING) {
            return "In Emergency Conditioning[Building Limits breached]";
        }
        if (L.ccu().systemProfile.getSystemController().isEmergencyMode()) {
            if (L.ccu().systemProfile.getSystemController().getSystemState() == SystemController.State.HEATING) {
                //return "Building Limit Breach | Emergency Heating turned ON";
                return "In Emergency Conditioning";
            } else if (L.ccu().systemProfile.getSystemController().getSystemState() == SystemController.State.COOLING) {
                // return "Building Limit Breach | Emergency Cooling turned ON";
                return "In Emergency Conditioning";
            }
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
        HashMap existingVal = HSUtil.getPriorityLevel((userLimit.get("id")).toString(),HayStackConstants.USER_APP_WRITE_LEVEL);
        if(existingVal.get("val")!=null) {
            CCUHsApi.getInstance().clearPointArrayLevel((userLimit.get("id")).toString(), HayStackConstants.USER_APP_WRITE_LEVEL, false);
        }
    }

    private void clearDeadBandChange(String tag, String roomRef) {
        HashMap<Object, Object> deadBand =
                CCUHsApi.getInstance().readEntity("schedulable and point and " +tag+ " and deadband and roomRef == \"" + roomRef + "\"" );
        HashMap existingVal = HSUtil.getPriorityLevel((deadBand.get("id")).toString(),HayStackConstants.USER_APP_WRITE_LEVEL);
        if(existingVal.get("val")!=null) {
            CCUHsApi.getInstance().clearPointArrayLevel((deadBand.get("id")).toString(), HayStackConstants.USER_APP_WRITE_LEVEL, false);
        }
    }

    private void clearUnoccupiedSetbackChange(String roomRef) {
        HashMap<Object, Object> unoccupiedZoneSetback =
                CCUHsApi.getInstance().readEntity("schedulable and point and setback and unoccupied and roomRef == \"" + roomRef + "\"" );
        CCUHsApi.getInstance().clearPointArrayLevel(unoccupiedZoneSetback.get("id").toString(), HayStackConstants.USER_APP_WRITE_LEVEL, false);
    }


    private static double getCoolingPreconditioningRate() {
        if (isDMSupportProfile()) {
            return getPointByDomain(coolingPreconditioningRate);
        } else {
            return TunerUtil.readTunerValByQuery("cooling and precon and rate", L.ccu().systemProfile.getSystemEquipRef());
        }
    }
    private static double getHeatingPreconditioningRate() {
        if (isDMSupportProfile()) {
            return getPointByDomain(heatingPreconditioningRate);
        } else {
            return TunerUtil.readTunerValByQuery("heating and precon and rate", L.ccu().systemProfile.getSystemEquipRef());
        }
    }
    private static double getPointByDomain(String domainName) {
        return  TunerUtil.readTunerValByQuery("domainName == \""+domainName+"\"", L.ccu().systemProfile.getSystemEquipRef());
    }

    private static boolean isDMSupportProfile() {
        return L.ccu().systemProfile instanceof DabExternalAhu
                || L.ccu().systemProfile instanceof VavExternalAhu
                || (L.ccu().systemProfile instanceof VavStagedRtu && !(L.ccu().systemProfile instanceof VavAdvancedHybridRtu))
                || L.ccu().systemProfile instanceof VavStagedRtuWithVfd
                || L.ccu().systemProfile instanceof VavFullyModulatingRtu;
    }
}
