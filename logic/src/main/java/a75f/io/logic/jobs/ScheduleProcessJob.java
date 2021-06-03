package a75f.io.logic.jobs;

import android.content.Intent;
import android.os.StrictMode;
import android.util.Log;

import org.joda.time.DateTime;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BaseJob;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ScheduleType;
import a75f.io.logic.bo.building.sensors.NativeSensor;
import a75f.io.logic.bo.building.sensors.SensorManager;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.pubnub.ZoneDataInterface;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.watchdog.WatchdogMonitor;

import static a75f.io.logic.L.TAG_CCU_JOB;
import static a75f.io.logic.L.TAG_CCU_SCHEDULER;
import static a75f.io.logic.bo.building.Occupancy.FORCEDOCCUPIED;
import static a75f.io.logic.bo.building.Occupancy.OCCUPANCYSENSING;
import static a75f.io.logic.bo.building.Occupancy.OCCUPIED;
import static a75f.io.logic.bo.building.Occupancy.PRECONDITIONING;
import static a75f.io.logic.bo.building.Occupancy.UNOCCUPIED;
import static a75f.io.logic.bo.building.Occupancy.VACATION;

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
public class ScheduleProcessJob extends BaseJob implements WatchdogMonitor
{

    private static final String TAG = "ScheduleProcessJob";
    public static final String ACTION_STATUS_CHANGE = "status_change";

    static HashMap<String, Occupied> occupiedHashMap = new HashMap<String, Occupied>();

    private static Occupancy systemOccupancy = null;
    private static Occupied currOccupied = null;
    private static Occupied nextOccupied = null;
    private static boolean systemVacation = false;

    private static Schedule activeSystemVacation = null;
    private static Occupied activeZoneVacation = null;
    private static ZoneDataInterface scheduleDataInterface = null;
    private static ZoneDataInterface zoneDataInterface = null;
    public static Occupied getOccupiedModeCache(String id) {
        if(!occupiedHashMap.containsKey(id))
        {
            return null;
        }
        return occupiedHashMap.get(id);
    }

    boolean watchdogMonitor = false;

    @Override
    public void bark() {
        watchdogMonitor = true;
    }

    @Override
    public boolean pet() {
        return watchdogMonitor;
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

        CcuLog.d(TAG_CCU_JOB,"ScheduleProcessJob-> "+CCUHsApi.getInstance());

        watchdogMonitor = false;

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
        processSchedules();
        CcuLog.d(TAG_CCU_JOB,"<- ScheduleProcessJob");
    }

    public static void processSchedules() {

        if (!CCUHsApi.getInstance().isCCURegistered()){
            return;
        }

        ArrayList<Schedule> activeVacationSchedules = CCUHsApi.getInstance().getSystemSchedule(true);

        activeSystemVacation = getActiveVacation(activeVacationSchedules);

        Log.d(L.TAG_CCU_JOB, " activeSystemVacation "+activeSystemVacation);

        //Read all equips
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone");
        for(HashMap hs : equips)
        {
            Equip equip = new Equip.Builder().setHashMap(hs).build();
            if(equip != null) {

                Log.d(L.TAG_CCU_JOB, " Equip "+equip.getDisplayName());
                Schedule equipSchedule = Schedule.getScheduleForZone(equip.getRoomRef().replace("@", ""), false);
                if(equipSchedule == null || equip.getRoomRef().contains("SYSTEM"))
                {
                    CcuLog.d(L.TAG_CCU_JOB,"<- *no schedule*");
                    continue;
                }

                //If building vacation is not active, check zone vacations.
                if (activeSystemVacation == null )
                {
                    Log.e(TAG, "processSchedules: "+equip.getRoomRef() );
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

        systemVacation = activeSystemVacation != null || isAllZonesInVacation();
        updateSystemOccupancy();
    }

    public static void processZoneEquipSchedule(Equip equip){
        if(equip != null) {

            Log.d(L.TAG_CCU_JOB, " Equip "+equip.getDisplayName());

            Schedule equipSchedule = Schedule.getScheduleForZone(equip.getRoomRef().replace("@", ""), false);

            if(equipSchedule == null || equip.getRoomRef().contains("SYSTEM"))
            {
                CcuLog.d(L.TAG_CCU_JOB,"<- *no schedule*");
                return;
            }

            //If building vacation is not active, check zone vacations.
            if (activeSystemVacation == null )
            {
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

        systemVacation = activeSystemVacation != null || isAllZonesInVacation();
        updateSystemOccupancy();
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
        if((equip.getMarkers().contains("vav") || equip.getMarkers().contains("dab") || equip.getMarkers().contains("dualDuct")
                || equip.getMarkers().contains("ti")) && !equip.getMarkers().contains("system")) {

            VAVScheduler.processEquip(equip, equipSchedule, vacation, systemOccupancy);
        } else if (equip.getMarkers().contains("pid")
                   || equip.getMarkers().contains("emr")
                   || equip.getMarkers().contains("modbus")) {
            Occupied occ = equipSchedule.getCurrentValues();
            if (occ != null) {
                putOccupiedModeCache(equip.getRoomRef(), occ);
            } else {
                ScheduleProcessJob.occupiedHashMap.remove(equip.getRoomRef());
            }
        }
        if( !equip.getMarkers().contains("system") && (equip.getMarkers().contains("standalone") || equip.getMarkers().contains("sse")))
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

    public static String getZoneStatusString(String zoneId, String equipId){


        Equip equip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().readMapById(equipId)).build();
        boolean isZoneHasStandaloneEquip = (equip.getMarkers().contains("smartstat") || equip.getMarkers().contains("sse") );
        double curOccuMode = CCUHsApi.getInstance().readHisValByQuery("point and occupancy and mode and equipRef == \""+equip.getId()+"\"");
        Occupancy curOccupancyMode = Occupancy.values()[(int)curOccuMode];
        Occupied cachedOccupied = getOccupiedModeCache(zoneId);
        if(cachedOccupied == null)
        {
            Schedule currentSchedule = Schedule.getScheduleForZone(zoneId.replace("@", ""), false);
            Log.d(L.TAG_CCU_JOB, "currentSchedule.getDays().size() "+currentSchedule.getDays().size());
            if (currentSchedule.getDays().size() == 0) {
                return "No schedule configured";
            }
            return "No schedule configured";
        }
        Log.d("ZoneSchedule","zoneStatusString = "+equip.getDisplayName()+","+isZoneHasStandaloneEquip+",occ="+cachedOccupied.isOccupied()+",precon="+cachedOccupied.isPreconditioning()+",fc="+cachedOccupied.isForcedOccupied());
        if (!isZoneHasStandaloneEquip && (systemOccupancy == PRECONDITIONING) ) {
            return "In Preconditioning ";
        }else if(curOccupancyMode == PRECONDITIONING) {//zone is in preconditioning even if any one equip

            return "In Preconditioning";
        }
        //{Current Mode}, Changes to Energy Saving Range of %.1f-%.1fF at %s
        if(curOccupancyMode == OCCUPIED)
        {
            if (cachedOccupied.getCurrentlyOccupiedSchedule() == null){
                return "No schedule configured";
            }
            return String.format("In %s, changes to Energy saving range of %.1f-%.1fF at %02d:%02d", "Occupied mode",
                    cachedOccupied.getHeatingVal() - cachedOccupied.getUnoccupiedZoneSetback(),
                    cachedOccupied.getCoolingVal() + cachedOccupied.getUnoccupiedZoneSetback(),
                    cachedOccupied.getCurrentlyOccupiedSchedule().getEthh(),
                    cachedOccupied.getCurrentlyOccupiedSchedule().getEtmm());
        }
        else {
            if(curOccupancyMode == FORCEDOCCUPIED) {
                long th = getTemporaryHoldExpiry(equip);
                if (th > 0) {
                    DateTime et = new DateTime(th);
                    int min = et.getMinuteOfHour();
                    return String.format("In Temporary Hold | till %s", et.getHourOfDay() + ":" + (min < 10 ? "0" + min : min));
                }
            }
            String statusString = "";

            if(cachedOccupied.getVacation() != null)
            {
                statusString = String.format("In Energy saving %s till %s", "Vacation",
                        cachedOccupied.getVacation().getEndDateString());

            } else
            {
                if(curOccupancyMode == PRECONDITIONING) {//Currently handled only for standalone
                    if (cachedOccupied.getNextOccupiedSchedule() == null){
                        return "No schedule configured";
                    }
                    statusString = String.format("In %s, changes to Energy saving range of %.1f-%.1fF at %02d:%02d", "Preconditioning",
                            cachedOccupied.getHeatingVal() - cachedOccupied.getUnoccupiedZoneSetback(),
                            cachedOccupied.getCoolingVal() + cachedOccupied.getUnoccupiedZoneSetback(),
                            cachedOccupied.getNextOccupiedSchedule().getEthh(),
                            cachedOccupied.getNextOccupiedSchedule().getEtmm());

                }else {
                    if (cachedOccupied.getNextOccupiedSchedule() == null){
                        return "No schedule configured";
                    }
                    statusString = String.format("In Energy saving %s, changes to %.1f-%.1fF at %02d:%02d", "Unoccupied mode",
                            cachedOccupied.getHeatingVal(),
                            cachedOccupied.getCoolingVal(),
                            cachedOccupied.getNextOccupiedSchedule().getSthh(),
                            cachedOccupied.getNextOccupiedSchedule().getStmm());
                }
            }
            return statusString;
        }
    }

    public static double getSystemCoolingDesiredTemp(){
        double setback = TunerUtil.readTunerValByQuery("default and unoccupied and setback");
        if(currOccupied != null)
            return (((systemOccupancy == UNOCCUPIED) || (systemOccupancy == VACATION)) ? currOccupied.getCoolingVal() + setback : currOccupied.getCoolingVal());
        else if(nextOccupied != null)
            return (((systemOccupancy == UNOCCUPIED) || (systemOccupancy == VACATION)) ? nextOccupied.getCoolingVal() + setback : nextOccupied.getCoolingVal());
        else return 0;
    }

    public static double getSystemHeatingDesiredTemp(){
        double setback = TunerUtil.readTunerValByQuery("default and unoccupied and setback");
        if(currOccupied != null)
            return (((systemOccupancy == UNOCCUPIED) || (systemOccupancy == VACATION)) ? currOccupied.getHeatingVal() - setback : currOccupied.getHeatingVal());
        else if (nextOccupied != null)
            return (((systemOccupancy == UNOCCUPIED) || (systemOccupancy == VACATION)) ? nextOccupied.getHeatingVal() - setback : nextOccupied.getHeatingVal());
        else return 0;
    }
    public static String getSystemStatusString() {

        if(L.ccu().systemProfile instanceof DefaultSystem)
            return "No Central equipment connected.";
        if(systemOccupancy == null)
        {
            return "No schedule configured";
        }

        if (systemVacation == true && systemOccupancy != FORCEDOCCUPIED) {
            if (activeSystemVacation != null) {
                return "In Energy saving Vacation till "+activeSystemVacation.getEndDateString();
            } else if (activeZoneVacation != null) {
                return "In Energy saving Vacation till "+activeZoneVacation.getVacation().getEndDateString();
            }
            return "In Energy saving Vacation";
        }
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and mode and state and equipRef ==\""+L.ccu().systemProfile.getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        String epidemicString = (epidemicState != EpidemicState.OFF) ? "["+epidemicState.name()+"] " : "";
        if (L.ccu().systemProfile.getSystemController().isEmergencyMode()) {
            if (L.ccu().systemProfile.getSystemController().getSystemState() == SystemController.State.HEATING) {
                return "Building Limit Breach | Emergency Heating turned ON";
            } else if (L.ccu().systemProfile.getSystemController().getSystemState() == SystemController.State.COOLING) {
                return "Building Limit Breach | Emergency Cooling turned ON";
            }
        }

        switch (systemOccupancy) {
            case OCCUPIED:
                if (currOccupied == null || currOccupied.getCurrentlyOccupiedSchedule() == null){
                    return "No schedule configured";
                }
                return String.format("%sIn %s | Changes to Energy saving Unoccupied mode at %02d:%02d", epidemicString,"Occupied mode",
                        currOccupied.getCurrentlyOccupiedSchedule().getEthh(),
                        currOccupied.getCurrentlyOccupiedSchedule().getEtmm());

            case PRECONDITIONING:
                return String.format("In Preconditioning");

            case UNOCCUPIED:
                if (nextOccupied == null || nextOccupied.getNextOccupiedSchedule() == null ){
                    return "No schedule configured";
                }
                return String.format("%sIn Energy saving %s | Changes to %.1f-%.1fF at %02d:%02d",epidemicString, "Unoccupied mode",
                        nextOccupied.getHeatingVal(),
                        nextOccupied.getCoolingVal(),
                        nextOccupied.getNextOccupiedSchedule().getSthh(),
                        nextOccupied.getNextOccupiedSchedule().getStmm());
            case FORCEDOCCUPIED:
                DateTime et = new DateTime(getSystemTemporaryHoldExpiry());
                int min = et.getMinuteOfHour();
                return String.format("%sIn Temporary Hold | till %s",epidemicString, et.getHourOfDay()+":"+(min < 10 ? "0"+min : min));


        }

        return "";
    }


    public static String getVacationStateString(String zoneId)
    {
        Occupied cachedOccupied = getOccupiedModeCache(zoneId);

        if(cachedOccupied == null)
        {
            return "No schedule configured";
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

    public static void updateSystemOccupancy() {
        if (L.ccu().systemProfile == null || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT) {
            Log.d(TAG_CCU_JOB, " Skip updateSystemOccupancy for Default System Profile ");
            return;
        }

        systemOccupancy = UNOCCUPIED;

        if (systemVacation) {
            if (getSystemTemporaryHoldExpiry() > 0) {
                systemOccupancy = FORCEDOCCUPIED;
            }else {
                systemOccupancy = VACATION;
            }
            CCUHsApi.getInstance().writeHisValByQuery("point and system and his and occupancy and mode", (double) systemOccupancy.ordinal());
            Log.d(TAG_CCU_JOB, " In SystemVacation : systemOccupancy : "+systemOccupancy);
            return;
        }

        Occupied curr = null;
        for (Occupied occ : occupiedHashMap.values()) {
            if (occ.isOccupied())
            {
                systemOccupancy = OCCUPIED;
                Schedule.Days occDay = occ.getCurrentlyOccupiedSchedule();
                if (curr == null || occDay.getEthh() > curr.getCurrentlyOccupiedSchedule().getEthh()
                        || (occDay.getEthh() == curr.getCurrentlyOccupiedSchedule().getEthh() && occDay.getEtmm() > curr.getCurrentlyOccupiedSchedule().getEtmm()) )
                {
                    Log.d(TAG_CCU_JOB, " Occupied Schedule "+occ.getCurrentlyOccupiedSchedule().toString());
                    curr = occ;
                }
            }
        }
        currOccupied = curr;

        long millisToOccupancy = 0;
        if (systemOccupancy == UNOCCUPIED) {
            Occupied next = null;
            for (Occupied occ : occupiedHashMap.values()) {
                if (!occ.isSystemZone()) {
                    if (next == null)
                    {
                        //Required when the CCU only has non-system equips like PID.
                        next = occ;
                    }
                    continue;
                }
                if (millisToOccupancy == 0) {
                    millisToOccupancy = occ.getMillisecondsUntilNextChange();
                    next = occ;
                } else if (occ.getMillisecondsUntilNextChange()  < millisToOccupancy){
                    millisToOccupancy = occ.getMillisecondsUntilNextChange();
                    next = occ;
                }
            }
            nextOccupied = next;
            Log.d(TAG_CCU_JOB, "millisToOccupancy: "+millisToOccupancy);
        } else {
            nextOccupied = null;
        }

        if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT) {
            CcuLog.d(TAG_CCU_JOB, "systemOccupancy status : " + systemOccupancy);
            return;
        }

        if (systemOccupancy == UNOCCUPIED)
        {
            double preconDegree = 0;
            double preconRate = CCUHsApi.getInstance().getPredictedPreconRate(L.ccu().systemProfile.getSystemEquipRef());
            SystemMode systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("conditioning and mode")];
            if (nextOccupied != null) {
                if (L.ccu().systemProfile.getAverageTemp() > 0)
                {
                    if (L.ccu().systemProfile.getSystemController().getConditioningForecast(nextOccupied) == SystemController.State.COOLING)
                    {
                        if(preconRate == 0)
                            preconRate = TunerUtil.readTunerValByQuery("cooling and precon and rate", L.ccu().systemProfile.getSystemEquipRef());
                        preconDegree = L.ccu().systemProfile.getAverageTemp() - nextOccupied.getCoolingVal();
                    }
                    else if (L.ccu().systemProfile.getSystemController().getConditioningForecast(nextOccupied) == SystemController.State.HEATING)
                    {
                        if(preconRate == 0)
                            preconRate = TunerUtil.readTunerValByQuery("heating and precon and rate", L.ccu().systemProfile.getSystemEquipRef());
                        preconDegree = nextOccupied.getHeatingVal() - L.ccu().systemProfile.getAverageTemp();
                    }
                }
            }
            if ((systemMode != SystemMode.OFF) && (preconDegree > 0) && (millisToOccupancy > 0) && (preconDegree * preconRate * 60 * 1000 >= millisToOccupancy))
            {
                systemOccupancy = PRECONDITIONING;
            }else{
                double sysOccValue = CCUHsApi.getInstance().readHisValByQuery("point and system and his and occupancy and mode");
                Occupancy prevOccuStatus = Occupancy.values()[(int)sysOccValue];
                if((prevOccuStatus == PRECONDITIONING) && (systemMode != SystemMode.OFF))
                    systemOccupancy = PRECONDITIONING;
            }
            CcuLog.d(L.TAG_CCU_JOB, "preconRate : "+preconRate+" preconDegree: "+preconDegree+","+systemOccupancy.name());
        }

        if ((systemOccupancy == UNOCCUPIED )&& (getSystemTemporaryHoldExpiry() > 0)) {
            systemOccupancy = FORCEDOCCUPIED;
        }

        double systemOccupancyValue = CCUHsApi.getInstance().readHisValByQuery("point and system and his and occupancy and mode");
        if (systemOccupancyValue != systemOccupancy.ordinal()){
            Globals.getInstance().getApplicationContext().sendBroadcast(new Intent(ACTION_STATUS_CHANGE));
        }

        CCUHsApi.getInstance().writeHisValByQuery("point and system and his and occupancy and mode",(double)systemOccupancy.ordinal());
        CcuLog.d(TAG_CCU_JOB, "systemOccupancy status : " + systemOccupancy.name());
    }
    public static Occupied getNextOccupiedTimeInMillis(){
        return nextOccupied;
    }
    public static Occupied getPrevOccupiedTimeInMillis(){
        if(currOccupied != null)
            return currOccupied;
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
    public static Occupancy getSystemOccupancy() {
        return systemOccupancy == null ? UNOCCUPIED : systemOccupancy;
    }

    public static boolean isAllZonesInVacation() {

        activeZoneVacation = null;

        for (Occupied occ : occupiedHashMap.values())
        {
            if (occ.getVacation() != null)
            {
                if (activeZoneVacation == null) {
                    activeZoneVacation = occ;
                } else if (activeZoneVacation.getVacation().getEndDate().getMillis() > occ.getVacation().getEndDate().getMillis()){
                    activeZoneVacation = occ;
                }
            } else {
                return false;
            }
        }

        CcuLog.d(TAG_CCU_JOB, "isAllZonesInVacation vacation: "+ (activeZoneVacation == null ? " NO " : activeZoneVacation.getVacation().getEndDateString()));

        return true;
    }

    //Update Schedules instantly
    public static void updateSchedules() {
        CcuLog.d(TAG_CCU_JOB,"updateSchedules ->");

        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() == 0) {
            CcuLog.d(TAG_CCU_JOB,"No Site Registered ! <-updateSchedules ");
            return;
        }

        new Thread() {
            @Override
            public void run() {
                processSchedules();
                CcuLog.d(TAG_CCU_JOB,"<- updateSchedules");
            }
        }.start();

    }
    public static void updateSchedules(final Equip equip) {
        CcuLog.d(TAG_CCU_JOB,"updateSchedules ->"+equip.getDisplayName());

        new Thread() {
            @Override
            public void run() {
                processZoneEquipSchedule(equip);
                CcuLog.d(TAG_CCU_JOB,"<- updateSchedules for equip done"+equip.getDisplayName());
            }
        }.start();

    }
    public static void updateEquipScheduleStatus(Equip equip) {
        Occupancy zoneOccupancy = getZoneStatus(equip);
        ArrayList points = CCUHsApi.getInstance().readAll("point and scheduleStatus and equipRef == \""+equip.getId()+"\"");
        if (points != null && points.size() > 0)
        {
            String id = ((HashMap) points.get(0)).get("id").toString();
            String hisZoneStatus = CCUHsApi.getInstance().readDefaultStrValById(id);
            String currentZoneStatus = getZoneStatusString(equip.getRoomRef(), equip.getId());
            if (!hisZoneStatus.equals(currentZoneStatus))
            {
                CCUHsApi.getInstance().writeDefaultValById(id, currentZoneStatus);
                if(scheduleDataInterface !=null){
                    String zoneId = Schedule.getZoneIdByEquipId(equip.getId());
                    scheduleDataInterface.refreshScreenbySchedule(equip.getGroup(),equip.getId(),zoneId);
                }
            } else {
                Log.d(L.TAG_CCU_JOB, "ScheduleStatus not changed for  "+equip.getDisplayName());
            }
        }
    }
    public static HashMap getDABEquipPoints(String equipID) {
        HashMap dabPoints = new HashMap();
        dabPoints.put("Profile","DAB");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        //double damperPosPoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and damper and base and equipRef == \""+equipID+"\"");
        double damperPosPoint = CCUHsApi.getInstance().readHisValByQuery("point and damper and normalized and cmd and equipRef == \""+equipID+"\"");
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and discharge and air and temp and primary and equipRef == \""+equipID+"\"");
        if (equipStatusPoint.length() > 0)
        {
            dabPoints.put("Status",equipStatusPoint);
        }else{
            dabPoints.put("Status","OFF");
        }
        if (damperPosPoint > 0)
        {
            dabPoints.put("Damper",(int)damperPosPoint+"% Open");
        }else{
            dabPoints.put("Damper",0+"% Open");
        }
        if (dischargePoint  != 0)
        {
            dabPoints.put("Discharge Airflow",dischargePoint+" \u2109");
        }else{
            dabPoints.put("Discharge Airflow",0+" \u2109");
        }
        return dabPoints;
    }

    public static HashMap getTIEquipPoints(String equipID) {

        HashMap tiPoints = new HashMap();
        tiPoints.put("Profile","TEMP_INFLUENCE");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        if (equipStatusPoint.length() > 0)
        {
            tiPoints.put("Status",equipStatusPoint);
        }else{
            tiPoints.put("Status","OFF");
        }
        return tiPoints;
    }
    public static HashMap getSSEEquipPoints(String equipID) {

        HashMap ssePoints = new HashMap();
        ssePoints.put("Profile","SSE");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and discharge and air and temp and equipRef == \""+equipID+"\"");
        if (equipStatusPoint.length() > 0)
        {
            ssePoints.put("Status",equipStatusPoint);
        }else{
            ssePoints.put("Status","OFF");
        }
        if (dischargePoint  != 0)
        {

            ssePoints.put("Discharge Airflow",dischargePoint+" \u2109");
        }else{
            ssePoints.put("Discharge Airflow",0+" \u2109");
        }
        return ssePoints;
    }

    public static HashMap getVAVEquipPoints(String equipID) {
        HashMap vavPoints = new HashMap();

        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        //double damperPosPoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and damper and base and equipRef == \""+equipID+"\"");
        double damperPosPoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and damper and normalized and cmd and equipRef == \""+equipID+"\"");
        double reheatPoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and reheat and cmd and equipRef == \""+equipID+"\"");
        double enteringAirPoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and entering and air and temp and equipRef == \""+equipID+"\"");
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and discharge and air and temp and vav and equipRef == \""+equipID+"\"");

        if (equipStatusPoint.length() > 0)
        {
            vavPoints.put("Status",equipStatusPoint);
        }else{
            vavPoints.put("Status","OFF");
        }
        if (damperPosPoint > 0)
        {
            vavPoints.put("Damper",(int)damperPosPoint+"% Open");
        }else{
            vavPoints.put("Damper",0+"% Open");
        }
        if (reheatPoint  > 0)
        {
            vavPoints.put("Reheat Coil",reheatPoint+"% Open");
        }else{
            vavPoints.put("Reheat Coil",0);
        }
        if (enteringAirPoint != 0)
        {
            vavPoints.put("Entering Airflow",enteringAirPoint+" \u2109");
        }else{
            vavPoints.put("Entering Airflow",0+" \u2109");
        }
        if (dischargePoint != 0)
        {
            vavPoints.put("Discharge Airflow",dischargePoint+" \u2109");
        }else{
            vavPoints.put("Discharge Airflow",0+" \u2109");
        }

        HashMap equip = CCUHsApi.getInstance().readMapById(equipID);
        if (equip.containsKey("series")) {
            vavPoints.put("Profile","VAV Series Fan");
        } else if (equip.containsKey("parallel")){
            vavPoints.put("Profile","VAV Parallel Fan");
        } else {
            vavPoints.put("Profile", "VAV Reheat - No Fan");
        }

        return vavPoints;
    }



    public static HashMap get2PFCUEquipPoints(String equipID) {
        HashMap p2FCUPoints = new HashMap();


        p2FCUPoints.put("Profile","Smartstat - 2 Pipe FCU");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        double fanopModePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and fan and mode and operation and equipRef == \""+equipID+"\"");
        double condtionModePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and temp and mode and conditioning and equipRef == \""+equipID+"\"");

        boolean isCoolingOn = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay6 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isFanLowEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay3 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isFanMediumEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay1 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isFanHighEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay2 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and discharge and air and temp and equipRef == \""+equipID+"\"");
        if (equipStatusPoint.length() > 0)
        {
            p2FCUPoints.put("Status",equipStatusPoint);
            //vavPoints.add(status);
        }else{
            p2FCUPoints.put("Status","OFF");
        }
        p2FCUPoints.put("Fan Mode",fanopModePoint);
        p2FCUPoints.put("Conditioning Mode",condtionModePoint);
        if (dischargePoint != 0) {
            p2FCUPoints.put("Discharge Airflow", dischargePoint + " \u2109");
        } else {
            p2FCUPoints.put("Discharge Airflow", 0 + " \u2109");
        }

        //We not dont consider auxiliary heating selection for determining available conditioning modes.
        if(!isCoolingOn)
            p2FCUPoints.put("condEnabled","Off");

        if(isFanLowEnabled && isFanMediumEnabled && !isFanHighEnabled)
            p2FCUPoints.put("fanEnabled","No High Fan");
        else if(isFanLowEnabled && !isFanMediumEnabled)
            p2FCUPoints.put("fanEnabled","No Medium High Fan");
        else if(!isFanLowEnabled)
            p2FCUPoints.put("fanEnabled","No Fan");
        return p2FCUPoints;
    }


    public static HashMap get4PFCUEquipPoints(String equipID) {
        HashMap p4FCUPoints = new HashMap();

        p4FCUPoints.put("Profile","Smartstat - 4 Pipe FCU");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        double fanopModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and fan and mode and operation and equipRef == \""+equipID+"\"");
        double condtionModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and temp and mode and conditioning and equipRef == \""+equipID+"\"");

        boolean isCoolingOn = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay6 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isHeatingOn = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay4 and equipRef == \"" + equipID + "\"") > 0 ? true : false;

        boolean isFanLowEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay3 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isFanMediumEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay1 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isFanHighEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay2 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and discharge and air and temp and equipRef == \""+equipID+"\"");
        if (equipStatusPoint.length() > 0)
        {
            p4FCUPoints.put("Status",equipStatusPoint);
        }else{
            p4FCUPoints.put("Status","OFF");
        }
        p4FCUPoints.put("Fan Mode",fanopModePoint);
        p4FCUPoints.put("Conditioning Mode",condtionModePoint);
        if (dischargePoint != 0) {
            p4FCUPoints.put("Discharge Airflow", dischargePoint + " \u2109");
        } else {
            p4FCUPoints.put("Discharge Airflow", 0 + " \u2109");
        }
        if(isCoolingOn && !isHeatingOn)
            p4FCUPoints.put("condEnabled","Cool Only");
        else if(!isCoolingOn && isHeatingOn)
            p4FCUPoints.put("condEnabled","Heat Only");
        else if(!isCoolingOn && !isHeatingOn)
            p4FCUPoints.put("condEnabled","Off");

        if(isFanLowEnabled && isFanMediumEnabled && !isFanHighEnabled)
            p4FCUPoints.put("fanEnabled","No High Fan");
        else if(isFanLowEnabled && !isFanMediumEnabled)
            p4FCUPoints.put("fanEnabled","No Medium High Fan");
        else if(!isFanLowEnabled)
            p4FCUPoints.put("fanEnabled","No Fan");
        return p4FCUPoints;
    }

    public static HashMap getCPUEquipPoints(String equipID) {
        HashMap cpuPoints = new HashMap();

        cpuPoints.put("Profile","Smartstat - Conventional Package Unit");
        ArrayList equipStatusPoint = CCUHsApi.getInstance().readAll("point and status and message and equipRef == \""+equipID+"\"");

        boolean isCooling1On = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay1 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isCooling2On = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay2 and equipRef == \"" + equipID + "\"") > 0 ? true : false;

        boolean isHeating1On = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay4 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isHeating2On = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay5 and equipRef == \"" + equipID + "\"") > 0 ? true : false;

        boolean isFanLowEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay3 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isFanHighEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay6 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and discharge and air and temp and equipRef == \""+equipID+"\"");
        double fanopModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and fan and mode and operation and equipRef == \""+equipID+"\"");
        double conditionModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and temp and mode and conditioning and equipRef == \""+equipID+"\"");
        double fanHighHumdOption = CCUHsApi.getInstance().readDefaultVal("point and zone and config and relay6 and type and equipRef == \"" + equipID + "\"");
        double targetHumidity = 0;
        if (equipStatusPoint != null && equipStatusPoint.size() > 0)
        {
            String id = ((HashMap) equipStatusPoint.get(0)).get("id").toString();
            String status = CCUHsApi.getInstance().readDefaultStrValById(id);
            cpuPoints.put("Status",status);
        }else{
            cpuPoints.put("Status","OFF");
        }
        cpuPoints.put("Fan Mode",fanopModePoint);
        cpuPoints.put("Conditioning Mode",conditionModePoint);
        if (dischargePoint != 0) {
            cpuPoints.put("Discharge Airflow", dischargePoint + " \u2109");
        } else {
            cpuPoints.put("Discharge Airflow", 0 + " \u2109");
        }
        if(fanHighHumdOption > 0){
            if(fanHighHumdOption > 1) isFanHighEnabled = false;
            cpuPoints.put("Fan High Humidity",fanHighHumdOption);
            if(fanHighHumdOption == 2.0) {
                targetHumidity = CCUHsApi.getInstance().readPointPriorityValByQuery("point and standalone and target and humidity and his and equipRef == \"" + equipID + "\"");
                cpuPoints.put("Target Humidity",targetHumidity);
            }else {
                targetHumidity = CCUHsApi.getInstance().readPointPriorityValByQuery("point and standalone and target and dehumidifier and his and equipRef == \"" + equipID + "\"");
                cpuPoints.put("Target Dehumidity",targetHumidity);
            }
        }else{
            cpuPoints.put("Fan High Humidity",0);
        }
        if((isCooling1On || isCooling2On) && (!isHeating1On && !isHeating2On))
            cpuPoints.put("condEnabled","Cool Only");
        else if((!isCooling1On && !isCooling2On) && (isHeating1On || isHeating2On))
            cpuPoints.put("condEnabled","Heat Only");
        else if((!isCooling1On && !isCooling2On) && (!isHeating1On && !isHeating2On))
            cpuPoints.put("condEnabled","Off");
        if(isFanLowEnabled && !isFanHighEnabled)
            cpuPoints.put("fanEnabled","No High Fan");
        else if(!isFanLowEnabled && !isFanHighEnabled)
            cpuPoints.put("fanEnabled","No Fan");
        return cpuPoints;
    }

    public static HashMap getHPUEquipPoints(String equipID) {
        HashMap hpuPoints = new HashMap();



        hpuPoints.put("Profile","Smartstat - Heat Pump Unit");
        ArrayList equipStatusPoint = CCUHsApi.getInstance().readAll("point and status and message and equipRef == \""+equipID+"\"");

        boolean isCompressor1On = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay1 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isCompressor2On = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay2 and equipRef == \"" + equipID + "\"") > 0 ? true : false;

        boolean isFanLowEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay3 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        boolean isFanHighEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and enable and relay5 and equipRef == \"" + equipID + "\"") > 0 ? true : false;
        double fanopModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and fan and mode and operation and equipRef == \""+equipID+"\"");
        double conditionModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and temp and mode and conditioning and equipRef == \""+equipID+"\"");
        double fanHighHumdOption = CCUHsApi.getInstance().readDefaultVal("point and zone and config and relay5 and type and equipRef == \"" + equipID + "\"");
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and sensor and discharge and air and temp and equipRef == \""+equipID+"\"");
        double targetHumidity = 0;
        if (equipStatusPoint != null && equipStatusPoint.size() > 0)
        {
            String id = ((HashMap) equipStatusPoint.get(0)).get("id").toString();
            String status = CCUHsApi.getInstance().readDefaultStrValById(id);
            hpuPoints.put("Status",status);
            hpuPoints.put("StatusTag",id);
            //vavPoints.add(status);
        }else{
            hpuPoints.put("Status","OFF");
        }
        hpuPoints.put("Fan Mode",fanopModePoint);
        hpuPoints.put("Conditioning Mode",conditionModePoint);
        if (dischargePoint != 0) {
            hpuPoints.put("Discharge Airflow", dischargePoint + " \u2109");
        } else {
            hpuPoints.put("Discharge Airflow", 0 + " \u2109");
        }
        if(fanHighHumdOption > 0){
            if(fanHighHumdOption > 1)isFanHighEnabled = false; //Since relay 5 is mapped to humidity or dehumidity
            hpuPoints.put("Fan High Humidity",fanHighHumdOption);
            if(fanHighHumdOption == 2.0) {
                targetHumidity = CCUHsApi.getInstance().readPointPriorityValByQuery("point and standalone and target and humidity and his and equipRef == \"" + equipID + "\"");
                hpuPoints.put("Target Humidity",targetHumidity);
            }else {
                targetHumidity = CCUHsApi.getInstance().readPointPriorityValByQuery("point and standalone and target and dehumidifier and his and equipRef == \"" + equipID + "\"");
                hpuPoints.put("Target Dehumidity",targetHumidity);
            }
        }else{
            hpuPoints.put("Fan High Humidity",0);
        }

        if(!isCompressor1On && !isCompressor2On) {
            hpuPoints.put("condEnabled","Off");
        }

        if(isFanLowEnabled && !isFanHighEnabled)
            hpuPoints.put("fanEnabled","No High Fan");
        else if(!isFanLowEnabled && !isFanHighEnabled)
            hpuPoints.put("fanEnabled","No Fan");
        return hpuPoints;
    }


    public static HashMap getEMEquipPoints(String equipID) {
        HashMap emPoints = new HashMap();

        emPoints.put("Profile","Energy Meter");
        ArrayList equipStatusPoint = CCUHsApi.getInstance().readAll("point and status and message and equipRef == \""+equipID+"\"");
        ArrayList currentRate = CCUHsApi.getInstance().readAll("point and emr and rate and equipRef == \""+equipID+"\"");
        double energyReading = CCUHsApi.getInstance().readHisValByQuery("point and emr and sensor and equipRef == \""+equipID+"\"");

        if (equipStatusPoint != null && equipStatusPoint.size() > 0)
        {
            String id = ((HashMap) equipStatusPoint.get(0)).get("id").toString();
            String status = CCUHsApi.getInstance().readDefaultStrValById(id);
            emPoints.put("Status",status);
        }else{
            emPoints.put("Status","OFF");
        }
        if (currentRate != null && currentRate.size() > 0)
        {
            String id = ((HashMap) currentRate.get(0)).get("id").toString();
            HisItem currentRateHis = CCUHsApi.getInstance().curRead(id);
            double currentRateVal = currentRateHis.getVal();
            emPoints.put("Current Rate",currentRateVal);
        }else{
            emPoints.put("Current Rate",0.0);
        }
        if (energyReading > 0)
        {
            emPoints.put("Energy Reading",energyReading);
        }else{
            emPoints.put("Energy Reading",0.0);
        }

        return emPoints;
    }


    public static HashMap getPiEquipPoints(String equipID) {
        HashMap plcPoints = new HashMap();

        plcPoints.put("Profile","Pi Loop Controller");
        ArrayList equipStatusPoint = CCUHsApi.getInstance().readAll("point and status and message and equipRef == \""+equipID+"\"");
        ArrayList inputValue = CCUHsApi.getInstance().readAll("point and process and logical and variable and equipRef == \""+equipID+"\"");
        ArrayList piSensorValue = CCUHsApi.getInstance().readAll("point and analog1 and config and input and sensor and equipRef == \""+equipID+"\"");
        double dynamicSetpoint = CCUHsApi.getInstance().readDefaultVal("point and analog2 and config and enabled and equipRef == \""+equipID+"\"");
        int th1InputSensor =  CCUHsApi.getInstance().readDefaultVal("point and config and th1 and input and sensor and equipRef == \"" + equipID + "\"").intValue();
        double targetValue = dynamicSetpoint > 0 ? 0: CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and pid and target and config and equipRef == \""+equipID+"\"");
        double analog1sensorType = CCUHsApi.getInstance().readPointPriorityValByQuery("point and analog1 and config and input and sensor and equipRef == \""+equipID+"\"");
        double analog2sensorType = CCUHsApi.getInstance().readPointPriorityValByQuery("point and analog2 and config and input and sensor and equipRef == \""+equipID+"\"");
        double offsetValue = CCUHsApi.getInstance().readDefaultVal("point and config and setpoint and sensor and offset and equipRef == \""+equipID+"\"");
        double loopOutput =
            CCUHsApi.getInstance().readHisValByQuery("point and control and variable and equipRef == \""+equipID+"\"");
    
        if (equipStatusPoint != null && equipStatusPoint.size() > 0)
        {
            String id = ((HashMap) equipStatusPoint.get(0)).get("id").toString();
            String status = CCUHsApi.getInstance().readDefaultStrValById(id);
            plcPoints.put("Status",status);
        }else{
            plcPoints.put("Status","OFF");
        }
        if (inputValue != null && inputValue.size() > 0)
        {
            String id = ((HashMap) inputValue.get(0)).get("id").toString();
            double inputVal = CCUHsApi.getInstance().readHisValById(id);
            plcPoints.put("Input Value",inputVal);
        }
    
        plcPoints.put("LoopOutput",loopOutput);

        plcPoints.put("Offset Value",offsetValue);

        if (piSensorValue != null && piSensorValue.size() > 0)
        {
            String id = ((HashMap) piSensorValue.get(0)).get("id").toString();
            double piSensorVal = CCUHsApi.getInstance().readHisValById(id);
            plcPoints.put("Pi Sensor Value",piSensorVal);
        }
        if(dynamicSetpoint == 1) {
            plcPoints.put("Dynamic Setpoint",true);
            targetValue = CCUHsApi.getInstance().readHisValByQuery("point and dynamic and target and value and equipRef == \""+equipID+"\"");
        }else {
            if(dynamicSetpoint == 0)
                plcPoints.put("Dynamic Setpoint",false);
        }

        plcPoints.put("Target Value",targetValue);
        if (dynamicSetpoint > 0) {
            switch ((int) analog2sensorType) {
                case 0:
                    plcPoints.put("Dynamic Unit Type", "Voltage");
                    plcPoints.put("Dynamic Unit", "V");
                    break;
                case 1:
                case 2:
                    plcPoints.put("Dynamic Unit Type", "Pressure");
                    plcPoints.put("Dynamic Unit", "WC");
                    break;
                case 3:
                    plcPoints.put("Dynamic Unit Type", "Airflow");
                    plcPoints.put("Dynamic Unit", "CFM");
                    break;
                case 4:
                    plcPoints.put("Dynamic Unit Type", "Humidity");
                    plcPoints.put("Dynamic Unit", "%");
                    break;
                case 5:
                    plcPoints.put("Dynamic Unit Type", "CO2");
                    plcPoints.put("Dynamic Unit", "PPM");
                    break;
                case 6:
                    plcPoints.put("Dynamic Unit Type", "CO");
                    plcPoints.put("Dynamic Unit", "PPM");
                    break;
                case 7:
                    plcPoints.put("Dynamic Unit Type", "NO2");
                    plcPoints.put("Dynamic Unit", "PPM");
                    break;
                case 8:
                case 9:
                case 10:
                    plcPoints.put("Dynamic Unit Type", "Current");
                    plcPoints.put("Dynamic Unit", "A");
                    break;
            }
        }

        switch ((int) analog1sensorType) {
            case 0:
            case 1:
                plcPoints.put("Unit Type", "Voltage");
                plcPoints.put("Unit", "V");
                break;
            case 2:
            case 3:
                plcPoints.put("Unit Type", "Pressure");
                plcPoints.put("Unit", "WC");
                break;
            case 4:
                plcPoints.put("Unit Type", "Airflow");
                plcPoints.put("Unit", "CFM");
                break;
            case 5:
                plcPoints.put("Unit Type", "Humidity");
                plcPoints.put("Unit", "%");
                break;
            case 6:
                plcPoints.put("Unit Type", "CO2");
                plcPoints.put("Unit", "PPM");
                break;
            case 7:
                plcPoints.put("Unit Type", "CO");
                plcPoints.put("Unit", "PPM");
                break;
            case 8:
                plcPoints.put("Unit Type", "NO2");
                plcPoints.put("Unit", "PPM");
                break;
            case 9:
            case 10:
            case 11:
                plcPoints.put("Unit Type", "Current");
                plcPoints.put("Unit", "A");
            case 12:
                plcPoints.put("Unit Type", "ION Density");
                plcPoints.put("Unit", "ions/cc");
                break;
        }

        if (th1InputSensor == 1 || th1InputSensor == 2) {
            plcPoints.put("Unit Type", "Temperature");
            plcPoints.put("Unit", "\u00B0F");
        }

        int nativeInputSensor =  CCUHsApi.getInstance().readDefaultVal("point and config and native and input and " +
                "sensor and equipRef == \"" + equipID + "\"").intValue();
        if (nativeInputSensor > 0) {
            NativeSensor selectedSensor = SensorManager.getInstance().getNativeSensorList().get(nativeInputSensor - 1);
            plcPoints.put("Unit Type", selectedSensor.sensorName);
            plcPoints.put("Unit", selectedSensor.engineeringUnit);
        }


        return plcPoints;
    }


    public static Occupancy getZoneStatus(Equip equip)
    {

        Occupied cachedOccupied = getOccupiedModeCache(equip.getRoomRef());
        Occupancy c = UNOCCUPIED;
        ArrayList<String> totEquipsInZone = new ArrayList<>();
        ArrayList occ = CCUHsApi.getInstance().readAll("point and occupancy and mode and equipRef == \""+equip.getId()+"\"");
        ArrayList<HashMap> zonedetails = CCUHsApi.getInstance().readAll("equip and zone and roomRef == \""+equip.getRoomRef()+"\"");
        if((occ != null) && occ.size() > 0)
            totEquipsInZone.add(((HashMap) occ.get(0)).get("id").toString());
        if(zonedetails.size() > 1){
            for(int i = 0; i < zonedetails.size(); i++){
                ArrayList occStatus = CCUHsApi.getInstance().readAll("point and occupancy and mode and equipRef == \""+zonedetails.get(i).get("id").toString()+"\"");
                if((occStatus != null) && occStatus.size() > 0){
                    totEquipsInZone.add(((HashMap) occStatus.get(0)).get("id").toString());
                }
            }
        }
        if (occ != null && occ.size() > 0) {
            String id = ((HashMap) occ.get(0)).get("id").toString();
            double occuStatus = CCUHsApi.getInstance().readHisValById(id);
            if(cachedOccupied != null) {
                if (cachedOccupied != null && cachedOccupied.isOccupied()) {
                    cachedOccupied.setForcedOccupied(false);
                    cachedOccupied.setPreconditioning(false);
                    c = OCCUPIED;
                } else if (getTemporaryHoldExpiry(equip) > 0) {
                    Occupancy prevStatus = Occupancy.values()[(int) occuStatus];
                    if ((prevStatus == OCCUPIED)) {
                        //Reset when schedule is changed from occupied to unoccupied
                        cachedOccupied.setForcedOccupied(false);
                        cachedOccupied.setPreconditioning(false);
                        clearTempOverrides(equip.getId());
                    } else {
                        c = FORCEDOCCUPIED;
                    }
                } else if ((cachedOccupied != null) && cachedOccupied.getVacation() != null) {
                    c = VACATION;
                } else if ((cachedOccupied != null) && cachedOccupied.isOccupancySensed()) {
                    c = OCCUPANCYSENSING;
                } else {
                    Occupancy prevStatus = Occupancy.values()[(int) occuStatus];
                    if ((prevStatus == OCCUPIED)) {
                        //Reset when schedule is changed from occupied to unoccupied
                        cachedOccupied.setForcedOccupied(false);
                        cachedOccupied.setPreconditioning(false);
                        if (zonedetails.size() > 1) {
                            for (int i = 0; i < zonedetails.size(); i++) {
                                clearTempOverrides(zonedetails.get(i).get("id").toString());
                            }
                        } else
                            clearTempOverrides(equip.getId());
                    } else {
                        SystemMode systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("conditioning and mode")];
                        boolean isZoneHasStandaloneEquip = (equip.getMarkers().contains("smartstat") || equip.getMarkers().contains("sse"));
                        
                        //Standalone zones could operate in preconditioning with system being OFF.
                        if (isZonePreconditioningActive(equip.getId(), cachedOccupied, isZoneHasStandaloneEquip)) {
                            c = PRECONDITIONING;
                        }else if ((systemMode != SystemMode.OFF) && cachedOccupied.isPreconditioning()) {
                            c = PRECONDITIONING;
                        } else if (!isZoneHasStandaloneEquip && getSystemOccupancy() == PRECONDITIONING){
                            c = PRECONDITIONING;
                        }else if ((systemMode != SystemMode.OFF) && ((prevStatus == PRECONDITIONING) || cachedOccupied.isPreconditioning())) {
                            c = PRECONDITIONING;
                        }
                    }
                }
            }
            if(totEquipsInZone.size() > 1) {
                for(String ids: totEquipsInZone) {
                    CCUHsApi.getInstance().writeHisValById(ids, (double) c.ordinal());
                }
            }else
                CCUHsApi.getInstance().writeHisValById(id, (double) c.ordinal());
        }
        if((zoneDataInterface != null) && (cachedOccupied != null)){
            zoneDataInterface.refreshDesiredTemp(equip.getGroup(), "","");
        }

        return c;
    }

    public static void handleDesiredTempUpdate(Point point, boolean manual, double val) {

        CcuLog.d(L.TAG_CCU_JOB, "handleDesiredTempUpdate for "+point.getDisplayName());
        Occupied occ = getOccupiedModeCache(point.getRoomRef());

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
                setAppOverrideExpiry(point, overrideExpiry.getMillis());

            }

        }else if (occ!= null && !occ.isOccupied()) {

            double forcedOccupiedMins = TunerUtil.readTunerValByQuery("forced and occupied and time",point.getEquipRef());

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
    }
    public static void handleManualDesiredTempUpdate(Point coolpoint,Point heatpoint, Point avgpoint, double coolval, double heatval, double avgval) {

        CcuLog.d(L.TAG_CCU_JOB, "handleManualDesiredTempUpdate for "+coolpoint.getDisplayName()+","+heatpoint.getDisplayName()+","+coolval+","+heatval+","+avgval);
        Occupied occ = getOccupiedModeCache(coolpoint.getRoomRef());

        if (occ != null && occ.isOccupied()) {
            Schedule equipSchedule = Schedule.getScheduleByEquipId(coolpoint.getEquipRef());

            if(equipSchedule == null)
            {
                CcuLog.d(L.TAG_CCU_JOB,"<- *no schedule* skip handleDesiredTempUpdate");
                return;
            }

            //TODO - change when setting to applyToAllDays enabled.
            if (equipSchedule.isZoneSchedule()) {
                if ((coolpoint != null) && (coolval != 0))
                {
                    equipSchedule.setDaysCoolVal(coolval, false);
                }
                if ((heatpoint != null) && (heatval != 0)) {
                    equipSchedule.setDaysHeatVal(heatval, false);
                }
                setAppOverrideExpiry(coolpoint, System.currentTimeMillis() + 10*1000);
                setAppOverrideExpiry(heatpoint, System.currentTimeMillis() + 10*1000);
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

            double forcedOccupiedMins = TunerUtil.readTunerValByQuery("forced and occupied and time",coolpoint.getEquipRef());

            if((coolpoint != null) && (coolval != 0))
                CCUHsApi.getInstance().pointWrite(HRef.copy(coolpoint.getId()), HayStackConstants.FORCE_OVERRIDE_LEVEL, "manual", HNum.make(coolval) , HNum.make(forcedOccupiedMins * 60 * 1000, "ms"));
            if((heatpoint != null) && (heatval != 0))
                CCUHsApi.getInstance().pointWrite(HRef.copy(heatpoint.getId()), HayStackConstants.FORCE_OVERRIDE_LEVEL, "manual", HNum.make(heatval) , HNum.make(forcedOccupiedMins * 60 * 1000, "ms"));


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

    public static void setAppOverrideExpiry(Point point, long overrRideExpiry) {
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
                        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(point.getId()), l, HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(overrRideExpiry - System.currentTimeMillis(), "ms"));
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

    public static void handleScheduleTypeUpdate(Point p){
        CcuLog.d(L.TAG_CCU_JOB, " ScheduleType handleScheduleTypeUpdate and  clearoverides for "+p.getDisplayName()+","+CCUHsApi.getInstance().readDefaultValById(p.getId()));
        if (p.getRoomRef().contains("SYSTEM")) {
            return;
        }
        Zone zone = new Zone.Builder().setHashMap(CCUHsApi.getInstance().readMapById(p.getRoomRef())).build();
        Schedule schedule = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());

        if (CCUHsApi.getInstance().readDefaultValById(p.getId()) == ScheduleType.ZONE.ordinal()) {
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

    public static long getSystemTemporaryHoldExpiry() {
        long thExpiry = 0;
        for (Floor f: HSUtil.getFloors())
        {
            for (Zone z : HSUtil.getZones(f.getId()))
            {
                Equip q = HSUtil.getEquipFromZone(z.getId());
                if(q.getMarkers().contains("dab") || q.getMarkers().contains("dualDuct")
                        || q.getMarkers().contains("vav" ) || q.getMarkers().contains("ti")) {
                    if (getTemporaryHoldExpiry(q) > thExpiry) {
                        thExpiry = getTemporaryHoldExpiry(q);
                    }
                }
            }
        }

        //Logging temporary hold expiry for debugging.
        if (thExpiry > 0)
        {
            Log.d(TAG_CCU_SCHEDULER, "thExpiry: "+thExpiry);
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

    public static boolean isZonePreconditioningActive(String equipId, Occupied occu, boolean isSmartStat){
        if(isSmartStat){
            double currentTemp = CCUHsApi.getInstance().readHisValByQuery("zone and point and current and air and temp and equipRef == \""+equipId+"\"");
            double desiredTemp = CCUHsApi.getInstance().readHisValByQuery("zone and point and desired and air and temp and average and equipRef == \""+equipId+"\"");
            double tempDiff = currentTemp - desiredTemp;
            double preconRate = TunerUtil.readTunerValByQuery("standalone and preconditioning and rate and "+
                                                                             (tempDiff >= 0 ? "cooling" : "heating"));
            if (preconRate == 0) {
                equipId = L.ccu().systemProfile.getSystemEquipRef();//get System default preconditioning rate
                if (tempDiff >= 0) {
                    preconRate = TunerUtil.readTunerValByQuery("cooling and precon and rate", equipId);
                } else {
                    preconRate = TunerUtil.readTunerValByQuery("heating and precon and rate", equipId);
                }
            }
            
            /*
             *Initial tempDiff based on average temp is used to determine heating/cooling preconditioning required.
             *Then calculate the absolute tempDiff to determine the preconditioning time.
             */
            if (tempDiff > 0) {
                tempDiff = currentTemp - occu.getCoolingVal();
            } else {
                tempDiff = occu.getHeatingVal() - currentTemp;
            }
            
            Log.d("ZoneSchedule","isZone in precon = "+preconRate+","+tempDiff +","+occu.getMillisecondsUntilNextChange()+","+currentTemp+","+desiredTemp+","+occu.isPreconditioning());

            if(currentTemp == 0) {
                occu.setPreconditioning(false);
                return false;
            }
            if(occu.isPreconditioning())
                return true;
            else if ((occu.getMillisecondsUntilNextChange() > 0)&& (tempDiff > 0) && (tempDiff * preconRate * 60 * 1000 >= occu.getMillisecondsUntilNextChange()))
            {
                //zone is in preconditioning which is like occupied
                occu.setPreconditioning(true);
                return true;
            }else {
                occu.setPreconditioning(false);
            }
        }
        return false;
    }

    public static void setScheduleDataInterface(ZoneDataInterface in) { scheduleDataInterface = in; }
    public static void setZoneDataInterface(ZoneDataInterface in){
        zoneDataInterface = in;
    }

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
        systemOccupancy = UNOCCUPIED;
    }
}
