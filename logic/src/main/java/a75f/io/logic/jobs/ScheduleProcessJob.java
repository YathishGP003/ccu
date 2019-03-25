package a75f.io.logic.jobs;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Schedule;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BaseJob;
import a75f.io.logic.L;


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
    
        CcuLog.d(L.TAG_CCU_JOB,"ScheduleProcessJob->");
        
        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() == 0) {
            CcuLog.d(L.TAG_CCU_JOB,"No Site Registered ! <-ScheduleProcessJob ");
            return;
        }
    
        HashMap ccu = CCUHsApi.getInstance().read("ccu");
        if (ccu.size() == 0) {
            CcuLog.d(L.TAG_CCU_JOB,"No CCU Registered ! <-ScheduleProcessJob ");
            return;
        }
        
        Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);

        ArrayList<Schedule> activeVacationSchedules = CCUHsApi.getInstance().getSystemSchedule(true);

        Schedule activeVacationSchedule = getActiveVacation(activeVacationSchedules);
        /* The systemSchedule isn't initiated yet, so schedules shouldn't be ran*/

        if(systemSchedule == null)
            return;
        
        //Read all equips


        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");
        for(HashMap hs : equips)
        {
            Equip equip = new Equip.Builder().setHashMap(hs).build();
            CcuLog.d(L.TAG_CCU_JOB, "Equip Dis: " + equip.toString());


            if(equip != null) {
               
                Schedule equipSchedule = Schedule.getScheduleForZone(equip.getRoomRef().replace("@", ""), false);
                if (equipSchedule != null) {
                    writePointsForEquip(equip, equipSchedule, activeVacationSchedule);
                }
                else
                {
                    writePointsForEquip(equip, systemSchedule, activeVacationSchedule);
                }
            }
        }
        CcuLog.d(L.TAG_CCU_JOB,"<- ScheduleProcessJob");
    }

    private Schedule getActiveVacation(ArrayList<Schedule> activeVacationSchedules)
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

    private void writePointsForEquip(Equip equip, Schedule equipSchedule, Schedule vacation) {
        if(equip.getMarkers().contains("vav"))
        {
            VAVScheduler.processEquip(equip, equipSchedule, vacation);
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

    public static String getSystemStateString(String zoneId)
    {

        Occupied cachedOccupied = getOccupiedModeCache("@" + zoneId);
        Status returnStatus = Status.OCCUPIED;
        double firstTemp = 0;
        double secondTemp = 0;
        if(cachedOccupied == null)
        {
            return "Setting up..";
        }
        //{Current Mode}, Changes to Energy Saving Range of %.1f-%.1fF at %s
        if(cachedOccupied.isOccupied())
        {
            return String.format("In %s, changes to energy saving range of %.1f-%.1fF at %02d:%02d", "occupied mode",
                    cachedOccupied.getHeatingVal() - cachedOccupied.getHeatingDeadBand(),
                    cachedOccupied.getCoolingVal() + cachedOccupied.getCoolingDeadBand(),
                    cachedOccupied.getCurrentlyOccupiedSchedule().getEthh(),
                    cachedOccupied.getCurrentlyOccupiedSchedule().getEtmm());
        }
        else if(cachedOccupied.getVacation() != null)
        {
            return String.format("In %s, changes to energy saving range of %.1f-%.1fF on %s", "vacation mode",
                    cachedOccupied.getHeatingVal(),
                    cachedOccupied.getCoolingVal(),
                    cachedOccupied.getVacation().getEndDateString());
        } else
        {
            return String.format("In %s, changes to energy saving range of %.1f-%.1fF at %02d:%02d", "unoccupied mode",
                                 cachedOccupied.getHeatingVal(),
                                 cachedOccupied.getCoolingVal(),
                                 cachedOccupied.getNextOccupiedSchedule().getSthh(),
                                 cachedOccupied.getNextOccupiedSchedule().getStmm());
        }
    }


    public static String getVacationStateString(String zoneId)
    {
        Occupied cachedOccupied = getOccupiedModeCache("@" + zoneId);

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

    public enum Status
    {
        UNOCCUPIED, OCCUPIED, PRECONDITIONING
    }
}
