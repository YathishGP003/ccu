package a75f.io.logic.jobs;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BaseJob;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.L.TAG_CCU_JOB;
import static a75f.io.logic.bo.building.Occupancy.OCCUPIED;
import static a75f.io.logic.bo.building.Occupancy.PRECONDITIONING;
import static a75f.io.logic.bo.building.Occupancy.UNOCCUPIED;

/*
    The scheduler needs to maintain the state of things, so it doesn't write

    It needs to maintain a cache by ID of each equip & zone of
    current mode, scheduled values, status text, overrides

    It needs to notify registered listening components when a change occurs to any of these ie. UI.

    It needs to be able to manage overrides being sent to it.   Other parts of the code will send
    in an override and the scheduler will be responsible for handling it.

    It needs to update the haystack database as needed.

    The UI should have a test screen were an override can be sent to test.
    There should also be a diagnostics screen to see the current state of the scheduler.

    If the application restarts the scheduler should be able to rebuild it overrides and cache etc.

    Each minute the scheduler will check for updates, so if the cache is extremely stale -- it will be overridden.

    The scheduler cache should be queryable by ID.
 */
public class ScheduleProcessJob extends BaseJob {

    private static final String TAG = "ScheduleProcessJob";

    static HashMap<String, Occupied> occupiedHashMap = new HashMap<String, Occupied>();

    private static Occupancy systemOccupancy = null;
    private static Occupied currOccupied = null;
    private static Occupied nextOccupied = null;
    private static boolean systemVacation = false;
    
    private static Schedule activeSystemVacation = null;
    private static Occupied activeZoneVacation = null;

    public static Occupied getOccupiedModeCache(String id) {
        if(!occupiedHashMap.containsKey(id))
        {
            return null;
        }
        return occupiedHashMap.get(id);
    }


    /*
     *  If the occupied mode is different when putting it in the cache return true.
     *
     *  If it is the same return false.
     */
    public static boolean putOccupiedModeCache(String id, Occupied occupied)
    {
        if(!occupiedHashMap.containsKey(id))
        {
            Log.i("SchedulerCache", "Putting in new key");
            occupiedHashMap.put(id, occupied);
            return true;
        }

        Occupied currentOccupiedMode = occupiedHashMap.get(id);
        if(currentOccupiedMode != null)
        {
            if(occupied.equals(currentOccupiedMode))
            {
                Log.i("SchedulerCache", "Reusing old occupied values");
                return false;
            }
            else
            {

                Log.i("SchedulerCache", "Putting in new occupied values");
                occupiedHashMap.put(id, occupied);
                return true;
            }
        }

        return false;

    }


    @Override
    public void doJob() {


        CcuLog.d(TAG_CCU_JOB,"ScheduleProcessJob->");

        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() == 0) {
            CcuLog.d(TAG_CCU_JOB,"No Site Registered ! <-ScheduleProcessJob ");
            return;
        }

        HashMap ccu = CCUHsApi.getInstance().read("ccu");
        if (ccu.size() == 0) {
            CcuLog.d(TAG_CCU_JOB,"No CCU Registered ! <-ScheduleProcessJob ");
            return;
        }



        ArrayList<Schedule> activeVacationSchedules = CCUHsApi.getInstance().getSystemSchedule(true);
    
        activeSystemVacation = getActiveVacation(activeVacationSchedules);
        /* The systemSchedule isn't initiated yet, so schedules shouldn't be ran*/
    
        Log.d(L.TAG_CCU_JOB, " activeSystemVacation "+activeSystemVacation);

        //Read all equips
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone");
        for(HashMap hs : equips)
        {
            Equip equip = new Equip.Builder().setHashMap(hs).build();
            if(equip != null) {
    
                Log.d(L.TAG_CCU_JOB, " Equip "+equip.getDisplayName());
                
                Schedule equipSchedule = Schedule.getScheduleForZone(equip.getRoomRef().replace("@", ""), false);

                if(equipSchedule == null)
                {
                    CcuLog.d(L.TAG_CCU_JOB,"<- *no schedule* ScheduleProcessJob");
                    continue;
                }

                //If building vacation is not active, check zone vacations.
                if (activeSystemVacation == null ) {
                    
                    ArrayList<Schedule> activeZoneVacationSchedules = CCUHsApi.getInstance().getZoneSchedule(equip.getRoomRef(),true);
                    Schedule activeZoneVacationSchedule = getActiveVacation(activeZoneVacationSchedules);
                    Log.d(L.TAG_CCU_JOB, "Equip "+equip.getDisplayName()+" activeZoneVacationSchedules "+activeZoneVacationSchedules.size()+" activeSystemVacation "+activeSystemVacation);
                    writePointsForEquip(equip, equipSchedule, activeZoneVacationSchedule);
                } else
                {
                    writePointsForEquip(equip, equipSchedule, activeSystemVacation);
                }
                
                updateEquipScheduleStatus(equip);

            }
        }

        updateSystemOccupancy();
        systemVacation = activeSystemVacation != null || isAllZonesInVacation();
        CcuLog.d(TAG_CCU_JOB,"<- ScheduleProcessJob");
    }

    private static Schedule getActiveVacation(ArrayList<Schedule> activeVacationSchedules)
    {

        if(activeVacationSchedules == null)
            return null;

        for(Schedule schedule : activeVacationSchedules)
        {
            if(schedule.isVacation() && schedule.isActiveVacation())
            {
                return schedule;
            }

        }

        return null;

    }

    private static void writePointsForEquip(Equip equip, Schedule equipSchedule, Schedule vacation) {
        if(equip.getMarkers().contains("vav") && !equip.getMarkers().contains("system"))
        {
            VAVScheduler.processEquip(equip, equipSchedule, vacation);
        }else if (equip.getMarkers().contains("pid")) {
            Occupied occ = equipSchedule.getCurrentValues();
            if (occ != null) {
                ScheduleProcessJob.putOccupiedModeCache(equip.getRoomRef(), occ);
            }
        }
		if( !equip.getMarkers().contains("system") && equip.getMarkers().contains("standalone"))
        {
            StandaloneScheduler.processEquip(equip,equipSchedule,vacation);
        }
    }


    /**
     * - Process all zones
     * - Make caches
     * - Write to equips
     *
     * @param zoneId
     * @return
     */
    /*
        Mode Text

        In temporary hold *This is for forced occupied mode
        In vacation, till
        Away
        In OCCUPIED - DR Mode
        In OCCUPIED
        In precondition - DR Mode
        In PRECONDITIONING
        In energy saving UNOCCUPIED


        Non-named schedules

        Heating & Cooling Modes
        {Current Mode}, Changes to {temp}F at {next schedule change}"

        Auto Mode

        {Current Mode}, Changes to Energy Saving Range of %.1f-%.1fF at %s
        In Vacation

         {vacation end time formatted Sep 16}

        Named schedules
        Heating & Cooling Modes

        {Current Mode}, {Named schedule name} changes to {temp}F at {next schedule change}"

        Auto Mode

        {Current Mode}, {Named schedule name} changes to Energy Saving Range of %.1f-%.1fF at %s

        In Vacation

         {vacation end time formatted Sep 16}

        To calculate the status text, get the mode text and append it into the formulas for either the different modes {Heating & Cooling, Auto, Vacation}.
        Each requires a calculation of when the next event is going to occur and what the temperature or temperature range will be at that point.

        One exception is that the date the vacation ends needs to append when the vacation ends.
     */

    public static String getZoneStatusString(String zoneId)
    {

        Occupied cachedOccupied = getOccupiedModeCache(zoneId);
        Occupancy returnStatus = OCCUPIED;
        double firstTemp = 0;
        double secondTemp = 0;
        if(cachedOccupied == null)
        {
            return "Setting up..";
        }
        //{Current Mode}, Changes to Energy Saving Range of %.1f-%.1fF at %s
        if(cachedOccupied.isOccupied())
        {
            return String.format("In %s, changes to Energy saving range of %.1f-%.1fF at %02d:%02d", "Occupied mode",
                    cachedOccupied.getHeatingVal() - cachedOccupied.getUnoccupiedZoneSetback(),
                    cachedOccupied.getCoolingVal() + cachedOccupied.getUnoccupiedZoneSetback(),
                    cachedOccupied.getCurrentlyOccupiedSchedule().getEthh(),
                    cachedOccupied.getCurrentlyOccupiedSchedule().getEtmm());
        }
        else if(cachedOccupied.getVacation() != null)
        {
            /*return String.format("In %s, changes to energy saving range of %.1f-%.1fF on %s", "vacation mode",
                    cachedOccupied.getHeatingVal(),
                    cachedOccupied.getCoolingVal(),
                    cachedOccupied.getVacation().getEndDateString());*/

            return String.format("In Energy saving %s till %s", "Vacation",
                    cachedOccupied.getVacation().getEndDateString());

        } else
        {
            return String.format("In Energy saving %s, changes to %.1f-%.1fF at %02d:%02d", "Unoccupied mode",
                                 cachedOccupied.getHeatingVal(),
                                 cachedOccupied.getCoolingVal(),
                                 cachedOccupied.getNextOccupiedSchedule().getSthh(),
                                 cachedOccupied.getNextOccupiedSchedule().getStmm());
        }
    }

    public static String getSystemStatusString() {

        if(systemOccupancy == null)
        {
            return "Setting up..";
        }
        
        if (systemVacation) {
            if (activeSystemVacation != null) {
                return "In Energy saving Vacation till "+activeSystemVacation.getEndDateString();
            } else if (activeZoneVacation != null) {
                return "In Energy saving Vacation till "+activeZoneVacation.getVacation().getEndDateString();
            }
            return "In Energy saving Vacation";
        }

        switch (systemOccupancy) {
            case OCCUPIED:
                return String.format("In %s | Changes to Energy saving Unoccupied mode at %02d:%02d", "Occupied mode",
                        currOccupied.getCurrentlyOccupiedSchedule().getEthh(),
                        currOccupied.getCurrentlyOccupiedSchedule().getEtmm());

            case PRECONDITIONING:
                return "In Preconditioning";

            case UNOCCUPIED:
                return String.format("In Energy saving  %s | Changes to %.1f-%.1fF at %02d:%02d", "Unoccupied mode",
                        nextOccupied.getHeatingVal(),
                        nextOccupied.getCoolingVal(),
                        nextOccupied.getNextOccupiedSchedule().getSthh(),
                        nextOccupied.getNextOccupiedSchedule().getStmm());
        }

        return "";
    }


    public static String getVacationStateString(String zoneId)
    {
        Occupied cachedOccupied = getOccupiedModeCache(zoneId);

        if(cachedOccupied == null)
        {
            return "Setting up..";
        }
        else if(cachedOccupied.getVacation() != null)
        {
            return "Active Vacation";
        }
        else
        {
            return "No Active Vacation";
        }
    }

    public static long getMillisToOccupancy() {
        if (nextOccupied != null) {
            return  nextOccupied.getMillisecondsUntilNextChange();
        }
        long millisToOccupancy = 0;
        for (Occupied occ : occupiedHashMap.values()) {
            if (occ.isOccupied()) {
                return 0;
            }
            if (millisToOccupancy == 0) {
                millisToOccupancy = occ.getMillisecondsUntilNextChange();
            } else if (occ.getMillisecondsUntilNextChange() < millisToOccupancy){
                millisToOccupancy = occ.getMillisecondsUntilNextChange();
            }
            Log.d(TAG_CCU_JOB, " Occupancy in millis for Equip "+occ.getMillisecondsUntilNextChange());
        }
        Log.d(TAG_CCU_JOB, " millisToOccupancy : "+millisToOccupancy);
        return millisToOccupancy;
    }

    public static void updateSystemOccupancy() {
        if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT) {
            Log.d(TAG_CCU_JOB, " Skip updateSystemOccupancy for Default System Profile ");
            return;
        }

        systemOccupancy = UNOCCUPIED;
        for (Floor f: HSUtil.getFloors())
        {
            for (Zone z : HSUtil.getZones(f.getId()))
            {
                Occupied c = ScheduleProcessJob.getOccupiedModeCache(z.getId());

                if (c!= null && c.isOccupied())
                {
                    systemOccupancy = OCCUPIED;
                    currOccupied = getOccupiedModeCache(z.getId());
                }
            }
        }

        long millisToOccupancy = 0;
        if (systemOccupancy == UNOCCUPIED) {
            Occupied next = null;
            for (Occupied occ : occupiedHashMap.values()) {
                Log.d(TAG_CCU_JOB, " occ: "+occ.toString()+" getMillisecondsUntilNextChange "+occ.getMillisecondsUntilNextChange());
                if (millisToOccupancy == 0) {
                    millisToOccupancy = occ.getMillisecondsUntilNextChange();
                    next = occ;
                } else if (occ.getMillisecondsUntilNextChange()  < millisToOccupancy){
                    millisToOccupancy = occ.getMillisecondsUntilNextChange();
                    next = occ;
                }
            }
            nextOccupied = next;
            Log.d(TAG_CCU_JOB, " millisToOccupancy: "+millisToOccupancy);
        } else {
            nextOccupied = null;
        }
    
        if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT) {
            //CCUHsApi.getInstance().writeHisValByQuery("point and system and his and occupancy and status",(double)systemOccupancy.ordinal());
            CcuLog.d(TAG_CCU_JOB, "systemOccupancy status : " + systemOccupancy);
            return;
        }

        double waCoolingOnlyLoadMA = CCUHsApi.getInstance().readHisValByQuery("system and point and moving and average and cooling and load");
        double waHeatingOnlyLoadMA = CCUHsApi.getInstance().readHisValByQuery("system and point and moving and average and heating and load");
        if (systemOccupancy == UNOCCUPIED)
        {
            double preconDegree = Math.max(waCoolingOnlyLoadMA, waHeatingOnlyLoadMA);
            double preconRate = CCUHsApi.getInstance().getPredictedPreconRate(L.ccu().systemProfile.getSystemEquipRef());
            if (preconRate == 0) {
                //TODO - Revisit , as per vav logic
                if (waCoolingOnlyLoadMA > 0)
                {
                    preconRate = TunerUtil.readTunerValByQuery("cooling and precon and rate", L.ccu().systemProfile.getSystemEquipRef());
                } else if (waHeatingOnlyLoadMA > 0){
                    preconRate = TunerUtil.readTunerValByQuery("heating and precon and rate", L.ccu().systemProfile.getSystemEquipRef());
                }
            }
            if (preconDegree * preconRate * 60 * 1000 >= millisToOccupancy)
            {
                systemOccupancy = PRECONDITIONING;
            }
            CcuLog.d(L.TAG_CCU_SYSTEM, "preconRate : "+preconRate+" preconDegree: "+preconDegree);
        }
        CCUHsApi.getInstance().writeHisValByQuery("point and system and his and occupancy and status",(double)systemOccupancy.ordinal());
        CcuLog.d(TAG_CCU_JOB, "systemOccupancy status : " + systemOccupancy);
    }

    public static Occupancy getSystemOccupancy() {
        return systemOccupancy == null ? UNOCCUPIED : systemOccupancy;
    }
    
    public static boolean isAllZonesInVacation() {
        
        activeZoneVacation = null;
        for (Floor f: HSUtil.getFloors())
        {
            for (Zone z : HSUtil.getZones(f.getId()))
            {
                Occupied c = ScheduleProcessJob.getOccupiedModeCache(z.getId());
                if (c!= null)
                {
                    if (c.getVacation() == null) {
                        return false;
                    }
                    
                    if (activeZoneVacation == null) {
                        activeZoneVacation = c;
                    } else if (activeZoneVacation.getVacation().getEndDate().getMillis() > c.getVacation().getEndDate().getMillis()){
                        activeZoneVacation = c;
                    }
                    
                }
            }
        }
        CcuLog.d(TAG_CCU_JOB, "isAllZonesInVacation vacation: "+ (activeZoneVacation == null ? " NO " : activeZoneVacation.getVacation().getEndDateString()));
        
        return true;
    }
    
    //Update Schedules instanly
    public static void updateSchedules() {
    
        new Thread() {
            @Override
            public void run() {
                ArrayList<Schedule> activeVacationSchedules = CCUHsApi.getInstance().getSystemSchedule(true);
    
                activeSystemVacation = getActiveVacation(activeVacationSchedules);
                /* The systemSchedule isn't initiated yet, so schedules shouldn't be ran*/
    
                Log.d(L.TAG_CCU_JOB, " activeSystemVacation "+activeSystemVacation);
    
                //Read all equips
                ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone");
                for(HashMap hs : equips)
                {
                    Equip equip = new Equip.Builder().setHashMap(hs).build();
                    if(equip != null) {
    
                        Log.d(L.TAG_CCU_JOB, " Equip "+equip.getDisplayName());
                        Schedule equipSchedule = Schedule.getScheduleForZone(equip.getRoomRef().replace("@", ""), false);
            
                        if(equipSchedule == null)
                        {
                            CcuLog.d(L.TAG_CCU_JOB,"<- *no schedule*");
                            continue;
                        }
            
                        //If building vacation is not active, check zone vacations.
                        if (activeSystemVacation == null ) {
                
                            ArrayList<Schedule> activeZoneVacationSchedules = CCUHsApi.getInstance().getZoneSchedule(equip.getRoomRef(),true);
                            Schedule activeZoneVacationSchedule = getActiveVacation(activeZoneVacationSchedules);
                            Log.d(L.TAG_CCU_JOB, "Equip "+equip.getDisplayName()+" activeZoneVacationSchedules "+activeZoneVacationSchedules.size()+" activeSystemVacation "+activeSystemVacation);
                            writePointsForEquip(equip, equipSchedule, activeZoneVacationSchedule);
                        } else
                        {
                            writePointsForEquip(equip, equipSchedule, activeSystemVacation);
                        }
    
                        updateEquipScheduleStatus(equip);
            
                    }
                    
                }
    
                updateSystemOccupancy();
                systemVacation = activeSystemVacation != null || isAllZonesInVacation();
            }
        }.start();
        
    }
    
    public static void updateEquipScheduleStatus(Equip equip) {
        Log.d(L.TAG_CCU_JOB, "updateEquipScheduleStatus "+equip.getDisplayName());
        for (String s : equip.getMarkers()) {
            Log.d(L.TAG_CCU_JOB, "Equip marker "+s);
        }
        Log.d(L.TAG_CCU_JOB, "updateEquipScheduleStatus "+equip.getDisplayName()+" "+getZoneStatusString(equip.getRoomRef()));
        ArrayList points = CCUHsApi.getInstance().readAll("point and scheduleStatus and equipRef == \""+equip.getId()+"\"");
        if (points != null && points.size() > 0)
        {
            String id = ((HashMap) points.get(0)).get("id").toString();
            CCUHsApi.getInstance().writeDefaultValById(id, getZoneStatusString(equip.getRoomRef()));
        }
        
    }
    
}
