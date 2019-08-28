package a75f.io.logic.jobs;

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
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ScheduleType;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.pubnub.ZoneDataInterface;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.L.TAG_CCU_JOB;
import static a75f.io.logic.L.TAG_CCU_SCHEDULER;
import static a75f.io.logic.bo.building.Occupancy.FORCED_OCCUPIED;
import static a75f.io.logic.bo.building.Occupancy.OCCUPANCY_SENSING;
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
    private static ZoneDataInterface scheduleDataInterface = null;
    private static ZoneDataInterface zoneDataInterface = null;
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


        CcuLog.d(TAG_CCU_JOB,"ScheduleProcessJob-> "+CCUHsApi.getInstance());

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
            
                if(equipSchedule == null)
                {
                    CcuLog.d(L.TAG_CCU_JOB,"<- *no schedule*");
                    continue;
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
        
        }
    
        systemVacation = activeSystemVacation != null || isAllZonesInVacation();
        updateSystemOccupancy();
    }

    public static void processZoneEquipSchedule(Equip equip){
        if(equip != null) {

            Log.d(L.TAG_CCU_JOB, " Equip "+equip.getDisplayName());

            Schedule equipSchedule = Schedule.getScheduleForZone(equip.getRoomRef().replace("@", ""), false);

            if(equipSchedule == null)
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
        if((equip.getMarkers().contains("vav") || equip.getMarkers().contains("dab") || equip.getMarkers().contains("ti")) && !equip.getMarkers().contains("system"))
        {
            VAVScheduler.processEquip(equip, equipSchedule, vacation, systemOccupancy);
        }else if (equip.getMarkers().contains("pid")) {
            Occupied occ = equipSchedule.getCurrentValues();
            if (occ != null) {
                ScheduleProcessJob.putOccupiedModeCache(equip.getRoomRef(), occ);
            } else {
                ScheduleProcessJob.occupiedHashMap.remove(equip.getRoomRef());
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

    public static String getZoneStatusString(String zoneId,String equipId)
    {


        Equip equip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().readMapById(equipId)).build();
        boolean isZoneHasSmartStat = equip.getMarkers().contains("smartstat");
        Occupied cachedOccupied = getOccupiedModeCache(zoneId);
        if(cachedOccupied == null)
        {
            Schedule currentSchedule = Schedule.getScheduleForZone(zoneId.replace("@", ""), false);
            Log.d(L.TAG_CCU_JOB, "currentSchedule.getDays().size() "+currentSchedule.getDays().size());
            if (currentSchedule.getDays().size() == 0) {
                return "Empty Schedule";
            }
            return "Setting up..";
        }
        if ((systemOccupancy == PRECONDITIONING) && !isZoneHasSmartStat) {
            return "In Preconditioning ";
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
        else {
            long th = getTemporaryHoldExpiry(zoneId);
            if (th > 0) {
                DateTime et = new DateTime(th);
                int min = et.getMinuteOfHour();
                cachedOccupied.setForcedOccupied(true);
                return String.format("In Temporary Hold | till %s", et.getHourOfDay()+":"+(min < 10 ? "0"+min : min));
            }
            cachedOccupied.setForcedOccupied(false);
    
            if(cachedOccupied.getVacation() != null)
            {
                return String.format("In Energy saving %s till %s", "Vacation",
                            cachedOccupied.getVacation().getEndDateString());
        
            } else
            {
                if(isZonePreconditioningActive(equipId,cachedOccupied, isZoneHasSmartStat)) {//Currently handled only for smartstat
                    return String.format("In %s, changes to Energy saving range of %.1f-%.1fF at %02d:%02d", "Preconditioning",
                            cachedOccupied.getHeatingVal() - cachedOccupied.getUnoccupiedZoneSetback(),
                            cachedOccupied.getCoolingVal() + cachedOccupied.getUnoccupiedZoneSetback(),
                            cachedOccupied.getNextOccupiedSchedule().getEthh(),
                            cachedOccupied.getNextOccupiedSchedule().getEtmm());

                }else
                    return String.format("In Energy saving %s, changes to %.1f-%.1fF at %02d:%02d", "Unoccupied mode",
                        cachedOccupied.getHeatingVal(),
                        cachedOccupied.getCoolingVal(),
                        cachedOccupied.getNextOccupiedSchedule().getSthh(),
                        cachedOccupied.getNextOccupiedSchedule().getStmm());
            }
        }
    }

    public static String getSystemStatusString() {

        if(L.ccu().systemProfile instanceof DefaultSystem)
            return "No Central equipment connected.";
        if(systemOccupancy == null)
        {
            return "Setting up..";
        }
        
        if (systemVacation == true && systemOccupancy != FORCED_OCCUPIED) {
            if (activeSystemVacation != null) {
                return "In Energy saving Vacation till "+activeSystemVacation.getEndDateString();
            } else if (activeZoneVacation != null) {
                return "In Energy saving Vacation till "+activeZoneVacation.getVacation().getEndDateString();
            }
            return "In Energy saving Vacation";
        }
        
        if (L.ccu().systemProfile.getSystemController().isEmergencyMode()) {
            if (L.ccu().systemProfile.getSystemController().getSystemState() == SystemController.State.HEATING) {
                return "Building Limit Breach | Emergency Heating turned ON";
            } else if (L.ccu().systemProfile.getSystemController().getSystemState() == SystemController.State.COOLING) {
                return "Building Limit Breach | Emergency Cooling turned ON";
            }
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
            case FORCED_OCCUPIED:
                DateTime et = new DateTime(getSystemTemporaryHoldExpiry());
                int min = et.getMinuteOfHour();
                return String.format("In Temporary Hold | till %s", et.getHourOfDay()+":"+(min < 10 ? "0"+min : min));
    
    
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

    public static void updateSystemOccupancy() {
        if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT) {
            Log.d(TAG_CCU_JOB, " Skip updateSystemOccupancy for Default System Profile ");
            return;
        }

        systemOccupancy = UNOCCUPIED;
        
        if (systemVacation) {
            double curOccupancy = CCUHsApi.getInstance().readHisValByQuery("point and system and his and occupancy and status");
            if (getSystemTemporaryHoldExpiry() > 0) {
                systemOccupancy = FORCED_OCCUPIED;
            }
            if (curOccupancy != systemOccupancy.ordinal())
            {
                CCUHsApi.getInstance().writeHisValByQuery("point and system and his and occupancy and status", (double) systemOccupancy.ordinal());
            }
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
            if (preconRate == 0 && nextOccupied != null) {
                if (L.ccu().systemProfile.getAverageTemp() > 0)
                {
                    if (L.ccu().systemProfile.getSystemController().getConditioningForecast(nextOccupied) == SystemController.State.COOLING)
                    {
                        preconRate = TunerUtil.readTunerValByQuery("cooling and precon and rate", L.ccu().systemProfile.getSystemEquipRef());
                        preconDegree = L.ccu().systemProfile.getAverageTemp() - nextOccupied.getCoolingVal();
                    }
                    else if (L.ccu().systemProfile.getSystemController().getConditioningForecast(nextOccupied) == SystemController.State.HEATING)
                    {
                        preconRate = TunerUtil.readTunerValByQuery("heating and precon and rate", L.ccu().systemProfile.getSystemEquipRef());
                        preconDegree = nextOccupied.getHeatingVal() - L.ccu().systemProfile.getAverageTemp();
                    }
                }
            }
            if ( (preconDegree != 0) && (millisToOccupancy > 0) && (preconDegree * preconRate * 60 * 1000 >= millisToOccupancy))
            {
                systemOccupancy = PRECONDITIONING;
            }
            CcuLog.d(L.TAG_CCU_JOB, "preconRate : "+preconRate+" preconDegree: "+preconDegree);
        }
    
        if (systemOccupancy == UNOCCUPIED && getSystemTemporaryHoldExpiry() > 0) {
            systemOccupancy = FORCED_OCCUPIED;
        }
        
        CCUHsApi.getInstance().writeHisValByQuery("point and system and his and occupancy and status",(double)systemOccupancy.ordinal());
        CcuLog.d(TAG_CCU_JOB, "systemOccupancy status : " + systemOccupancy);
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
            String currentState = CCUHsApi.getInstance().readDefaultStrValById(id);
            if (!currentState.equals(getZoneStatusString(equip.getRoomRef(), equip.getId())))
            {
                CCUHsApi.getInstance().writeDefaultValById(id, getZoneStatusString(equip.getRoomRef(),equip.getId()));
                CCUHsApi.getInstance().writeHisValById(id, (double) zoneOccupancy.ordinal());
                if(scheduleDataInterface !=null){
                    String zoneId = Schedule.getZoneIdByEquipId(equip.getId());
                    scheduleDataInterface.refreshScreenbySchedule(equip.getGroup(),equip.getId(),zoneId);
                }
            } else {
                Log.d(L.TAG_CCU_JOB, "ScheduleStatus not changed for  "+equip.getDisplayName());
            }
        }

        ArrayList occ = CCUHsApi.getInstance().readAll("point and occupancy and status and equipRef == \""+equip.getId()+"\"");
        if (occ != null && occ.size() > 0) {
            String id = ((HashMap) points.get(0)).get("id").toString();
            CCUHsApi.getInstance().writeHisValById(id, (double) zoneOccupancy.ordinal());
        }
    }

    public static HashMap getDABEquipPoints(String equipID) {

        HashMap dabPoints = new HashMap();
        dabPoints.put("Profile","DAB");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        double damperPosPoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and damper and base and equipRef == \""+equipID+"\"");
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and discharge and air and temp and equipRef == \""+equipID+"\"");
        if (equipStatusPoint.length() > 0)
        {
            dabPoints.put("Status",equipStatusPoint);
        }else{
            dabPoints.put("Status","OFF");
        }
        if (damperPosPoint > 0)
        {
            dabPoints.put("Damper",damperPosPoint+"% Open");
        }else{
            dabPoints.put("Damper",0+"% Open");
        }
        if (dischargePoint  > 0)
        {

            dabPoints.put("Discharge Airflow",dischargePoint+" \u2109");
        }else{
            dabPoints.put("Discharge Airflow",0+" \u2109");
        }
        return dabPoints;
    }
    public static HashMap getTIEquipPoints(String equipID) {


        //damperPos: ['zone', 'damper', 'base'],
        //equipStatusMessage: ['zone', 'message', 'status'],
        //dischargeAirTemp: ['zone', 'discharge', 'air', 'temp'],

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
    public static HashMap getVAVEquipPoints(String equipID) {
        HashMap vavPoints = new HashMap();


        vavPoints.put("Profile","VAV Reheat - No Fan");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        double damperPosPoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and damper and base and equipRef == \""+equipID+"\"");
        double reheatPoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and reheat and base and equipRef == \""+equipID+"\"");
        double supplyAirPoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and entering and air and temp and equipRef == \""+equipID+"\"");
        double dischargePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and discharge and air and temp and equipRef == \""+equipID+"\"");
        if (equipStatusPoint.length() > 0)
        {
            vavPoints.put("Status",equipStatusPoint);
        }else{
            vavPoints.put("Status","OFF");
        }
        if (damperPosPoint > 0)
        {
            vavPoints.put("Damper",damperPosPoint+"% Open");
        }else{
            vavPoints.put("Damper",0+"% Open");
        }
        if (reheatPoint  > 0)
        {
            vavPoints.put("Reheat Coil",reheatPoint+"% Open");
        }else{
            vavPoints.put("Reheat Coil",0);
        }
        if (supplyAirPoint > 0)
        {
            vavPoints.put("Supply Airfow",supplyAirPoint+" \u2109");
        }else{
            vavPoints.put("Supply Airfow",0+" \u2109");
        }
        if (dischargePoint > 0)
        {
            vavPoints.put("Discharge Airflow",dischargePoint+" \u2109");
        }else{
            vavPoints.put("Discharge Airflow",0+" \u2109");
        }
        return vavPoints;
    }



    public static HashMap get2PFCUEquipPoints(String equipID) {
        HashMap p2FCUPoints = new HashMap();


        p2FCUPoints.put("Profile","Smartstat - 2 Pipe FCU");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        double fanopModePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and fan and mode and operation and equipRef == \""+equipID+"\"");
        double operationModePoint = CCUHsApi.getInstance().readHisValByQuery("point and zone and temp and mode and operation and equipRef == \""+equipID+"\"");

        if (equipStatusPoint.length() > 0)
        {
            p2FCUPoints.put("Status",equipStatusPoint);
            //vavPoints.add(status);
        }else{
            p2FCUPoints.put("Status","OFF");
        }
        if (fanopModePoint > 0)
        {
            p2FCUPoints.put("Fan Mode",fanopModePoint);
            //vavPoints.add(dampPos+"% Open");
        }else{
            p2FCUPoints.put("Fan Mode",0);
            //vavPoints.add(0);
        }
        if (operationModePoint > 0)
        {
            p2FCUPoints.put("Conditioning Mode",operationModePoint);
        }else{
            p2FCUPoints.put("Conditioning Mode",0);
        }
        return p2FCUPoints;
    }


    public static HashMap get4PFCUEquipPoints(String equipID) {
        HashMap p2FCUPoints = new HashMap();



        p2FCUPoints.put("Profile","Smartstat - 4 Pipe FCU");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and equipRef == \""+equipID+"\"");
        double fanopModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and fan and mode and operation and equipRef == \""+equipID+"\"");
        double operationModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and temp and mode and operation and equipRef == \""+equipID+"\"");

        if (equipStatusPoint.length() > 0)
        {
            p2FCUPoints.put("Status",equipStatusPoint);
            //vavPoints.add(status);
        }else{
            p2FCUPoints.put("Status","OFF");
        }
        if (fanopModePoint > 0)
        {
            p2FCUPoints.put("Fan Mode",fanopModePoint);
            //vavPoints.add(dampPos+"% Open");
        }else{
            p2FCUPoints.put("Fan Mode",0);
            //vavPoints.add(0);
        }
        if (operationModePoint > 0)
        {
            p2FCUPoints.put("Conditioning Mode",operationModePoint);
        }else{
            p2FCUPoints.put("Conditioning Mode",0);
        }
        return p2FCUPoints;
    }

     public static HashMap getCPUEquipPoints(String equipID) {
            HashMap cpuPoints = new HashMap();

                    cpuPoints.put("Profile","Smartstat - Convention Package Unit");
            ArrayList equipStatusPoint = CCUHsApi.getInstance().readAll("point and status and message and equipRef == \""+equipID+"\"");
            double fanopModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and fan and mode and operation and equipRef == \""+equipID+"\"");
            double operationModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and temp and mode and operation and equipRef == \""+equipID+"\"");
            double fanHighHumdOption = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and config and relay6 and type and equipRef == \"" + equipID + "\"");
            double targetHumidity = 0;
            if (equipStatusPoint != null && equipStatusPoint.size() > 0)
            {
                String id = ((HashMap) equipStatusPoint.get(0)).get("id").toString();
                String status = CCUHsApi.getInstance().readDefaultStrValById(id);
                cpuPoints.put("Status",status);
            }else{
                cpuPoints.put("Status","OFF");
            }
            if (fanopModePoint > 0)
            {
                cpuPoints.put("Fan Mode",fanopModePoint);
            }else{
                cpuPoints.put("Fan Mode",0);
            }
            if (operationModePoint > 0)
            {
                cpuPoints.put("Conditioning Mode",operationModePoint);
            }else{
                cpuPoints.put("Conditioning Mode",0);
            }
            if(fanHighHumdOption > 0){
                cpuPoints.put("Fan High Humidity",fanHighHumdOption);
                if(fanHighHumdOption == 2.0) {
                    targetHumidity = CCUHsApi.getInstance().readDefaultVal("point and standalone and target and humidity and his and equipRef == \"" + equipID + "\"");
                    cpuPoints.put("Target Humidity",targetHumidity);
                }else {
                    targetHumidity = CCUHsApi.getInstance().readDefaultVal("point and standalone and target and dehumidifier and his and equipRef == \"" + equipID + "\"");
                    cpuPoints.put("Target Dehumidity",targetHumidity);
                }
            }else{
                cpuPoints.put("Fan High Humidity",0);
            }
            return cpuPoints;
    }

    public static HashMap getHPUEquipPoints(String equipID) {
        HashMap cpuPoints = new HashMap();



        cpuPoints.put("Profile","Smartstat - Heat Pump Unit");
        ArrayList equipStatusPoint = CCUHsApi.getInstance().readAll("point and status and message and equipRef == \""+equipID+"\"");
        double fanopModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and fan and mode and operation and equipRef == \""+equipID+"\"");
        double operationModePoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and temp and mode and operation and equipRef == \""+equipID+"\"");
        double fanHighHumdOption = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and config and relay5 and type and equipRef == \"" + equipID + "\"");
        double targetHumidity = 0;
        if (equipStatusPoint != null && equipStatusPoint.size() > 0)
        {
            String id = ((HashMap) equipStatusPoint.get(0)).get("id").toString();
            String status = CCUHsApi.getInstance().readDefaultStrValById(id);
            cpuPoints.put("Status",status);
            cpuPoints.put("StatusTag",id);
            //vavPoints.add(status);
        }else{
            cpuPoints.put("Status","OFF");
        }if (fanopModePoint > 0)
        {
            cpuPoints.put("Fan Mode",fanopModePoint);
        }else{
            cpuPoints.put("Fan Mode",0);
        }
        if (operationModePoint > 0)
        {
            cpuPoints.put("Conditioning Mode",operationModePoint);
        }else{
            cpuPoints.put("Conditioning Mode",0);
        }
        if(fanHighHumdOption > 0){
            cpuPoints.put("Fan High Humidity",fanHighHumdOption);
            if(fanHighHumdOption == 2.0) {
                targetHumidity = CCUHsApi.getInstance().readDefaultVal("point and standalone and target and humidity and his and equipRef == \"" + equipID + "\"");
                cpuPoints.put("Target Humidity",targetHumidity);
            }else {
                targetHumidity = CCUHsApi.getInstance().readDefaultVal("point and standalone and target and dehumidifier and his and equipRef == \"" + equipID + "\"");
                cpuPoints.put("Target Dehumidity",targetHumidity);
            }
        }else{
            cpuPoints.put("Fan High Humidity",0);
        }
        return cpuPoints;
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
        //this.getZoneSettings('piDynamicSetpointValue', this.helperService.getPointIdbyTags(profileObj, ['analog2', 'config', 'enabled'], this.roomRef), 'write', profileObj.name);
        ArrayList equipStatusPoint = CCUHsApi.getInstance().readAll("point and status and message and equipRef == \""+equipID+"\"");
        ArrayList inputValue = CCUHsApi.getInstance().readAll("point and process and logical and variable and equipRef == \""+equipID+"\"");
        ArrayList offsetValue = CCUHsApi.getInstance().readAll("point and setpoint and sensor and offset and equipRef == \""+equipID+"\"");
        //ArrayList targetValue = CCUHsApi.getInstance().readAll("point and zone and pid and target and config and value and equipRef == \""+equipID+"\"");
        ArrayList piSensorValue = CCUHsApi.getInstance().readAll("point and analog1 and config and input and sensor and equipRef == \""+equipID+"\"");
        double dynamicSetpoint = CCUHsApi.getInstance().readPointPriorityValByQuery("point and analog2 and config and enabled and equipRef == \""+equipID+"\"");
        double targetValue = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and pid and target and config and equipRef == \""+equipID+"\"");
        double analog1sensorType = CCUHsApi.getInstance().readPointPriorityValByQuery("point and analog1 and config and input and sensor and equipRef == \""+equipID+"\"");
        double analog2sensorType = CCUHsApi.getInstance().readPointPriorityValByQuery("point and analog2 and config and input and sensor and equipRef == \""+equipID+"\"");

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
        if (offsetValue != null && offsetValue.size() > 0)
        {
            String id = ((HashMap) offsetValue.get(0)).get("id").toString();
            double offsetVal = CCUHsApi.getInstance().readHisValById(id);
            plcPoints.put("Offset Value",offsetVal);
        }
        if (targetValue != 0)
        {
            //String id = ((HashMap) targetValue.get(0)).get("id").toString();
            //double targetVal = CCUHsApi.getInstance().readHisValById(id);
            plcPoints.put("Target Value",targetValue);
        }
        if (piSensorValue != null && piSensorValue.size() > 0)
        {
            String id = ((HashMap) piSensorValue.get(0)).get("id").toString();
            double piSensorVal = CCUHsApi.getInstance().readHisValById(id);
            plcPoints.put("Pi Sensor Value",piSensorVal);
        }
        /*if (dynamicSetpoint != null && dynamicSetpoint.size() > 0)
        {
            ///String id = ((HashMap) dynamicSetpoint.get(0)).get("id").toString();
            //double dynamicSPval = CCUHsApi.getInstance().readPointPriorityVal(id);

        }*/
        if(dynamicSetpoint == 1) {
            plcPoints.put("Dynamic Setpoint",true);
        }else {
            if(dynamicSetpoint == 0)
                plcPoints.put("Dynamic Setpoint",false);
        }
        if(analog1sensorType == 1)
        {
            plcPoints.put("Unit Type","Voltage");
            plcPoints.put("Unit","V");
        }
        if(analog1sensorType == 2 || analog1sensorType == 3)
        {
            plcPoints.put("Unit Type","Pressure");
            plcPoints.put("Unit","WC");
        }
        if(analog1sensorType == 4)
        {
            plcPoints.put("Unit Type","Airflow");
            plcPoints.put("Unit","%");
        }
        if(analog1sensorType == 5)
        {
            plcPoints.put("Unit Type","Humidity");
            plcPoints.put("Unit","%");
        }
        if(analog1sensorType == 6)
        {
            plcPoints.put("Unit Type","CO2");
            plcPoints.put("Unit","PPM");
        }
        if(analog1sensorType == 7)
        {
            plcPoints.put("Unit Type","CO");
            plcPoints.put("Unit","PPM");
        }
        if(analog1sensorType == 8)
        {
            plcPoints.put("Unit Type","NO2");
            plcPoints.put("Unit","PPM");
        }
        if(analog1sensorType == 9 || analog1sensorType == 10 || analog1sensorType == 11)
        {
            plcPoints.put("Unit Type","Current");
            plcPoints.put("Unit","AMPS");
        }
        return plcPoints;
    }


    public static Occupancy getZoneStatus(Equip equip)
    {
        
        Occupied cachedOccupied = getOccupiedModeCache(equip.getRoomRef());
        Occupancy c = UNOCCUPIED;
        
        if (cachedOccupied != null && cachedOccupied.isOccupied())
        {
            c = OCCUPIED;
        } else if (getTemporaryHoldExpiry(equip.getRoomRef()) > 0){
            c = FORCED_OCCUPIED;
        }else if((cachedOccupied != null) && (cachedOccupied.isPreconditioning())) {
            //handle preconditioning??

            c = PRECONDITIONING;
        }else if((cachedOccupied != null) && cachedOccupied.isOccupancySensed()){
            c = OCCUPANCY_SENSING;
        }
        if((zoneDataInterface != null) && (cachedOccupied != null)){
            Log.d("Scheduler","updateZoneOccupancy==>>"+cachedOccupied.isForcedOccupied()+","+cachedOccupied.isOccupied()+","+cachedOccupied.isPreconditioning());
            double coolVal = (c == UNOCCUPIED ? cachedOccupied.getCoolingVal() + cachedOccupied.getUnoccupiedZoneSetback() : cachedOccupied.getCoolingVal());
            double heatVal = (c== UNOCCUPIED ? cachedOccupied.getHeatingVal() - cachedOccupied.getUnoccupiedZoneSetback() : cachedOccupied.getHeatingVal());
            zoneDataInterface.refreshDesiredTemp(equip.getGroup(), String.valueOf(coolVal),String.valueOf(heatVal));
        }
        
        return c;
    }
    
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
    
                CCUHsApi.getInstance().pointWrite(HRef.copy(point.getId()), HayStackConstants.FORCE_OVERRIDE_LEVEL, "ccu", HNum.make(val), HNum.make(overrideExpiry.getMillis()
                                                                                                                                                     - System.currentTimeMillis(), "ms"));
                setAppOverrideExpiry(point, overrideExpiry.getMillis());
                
            }
            
        }else if (occ!= null && !occ.isOccupied()) {
            
            double forcedOccupiedMins = TunerUtil.readTunerValByQuery("forced and occupied and time");
            
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
                CCUHsApi.getInstance().pointWrite(HRef.copy(point.getId()), HayStackConstants.FORCE_OVERRIDE_LEVEL, "ccu", HNum.make(Double.parseDouble(overrideLevel.get("val").toString())), HNum.make(dur == 0 ? forcedOccupiedMins * 60 * 1000 : dur - System.currentTimeMillis(), "ms"));
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
                                CCUHsApi.getInstance().pointWrite(HRef.copy(point.getId()), l, "ccu", HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(dur == 0 ? forcedOccupiedMins * 60 * 1000 : dur - System.currentTimeMillis(), "ms"));
                            }
                        }
                    }
                }
            }
            
            //HSUtil.printPointArr(point);
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
                        CCUHsApi.getInstance().pointWrite(HRef.copy(point.getId()), l, "ccu", HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(overrRideExpiry - System.currentTimeMillis(), "ms"));
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
                    CCUHsApi.getInstance().pointWrite(HRef.copy(id), l, "ccu", HNum.make(0), HNum.make(1, "ms"));
                }
            }
        }
    }
    
    public static void handleScheduleTypeUpdate(Point p){
        
        CcuLog.d(L.TAG_CCU_JOB, "handleScheduleTypeUpdate for "+p.getDisplayName());
        Zone zone = new Zone.Builder().setHashMap(CCUHsApi.getInstance().readMapById(p.getRoomRef())).build();
        Schedule schedule = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
        
        if (CCUHsApi.getInstance().readDefaultValById(p.getId()) == ScheduleType.ZONE.ordinal()) {
            schedule.setDisabled(false);
        } else {
            schedule.setDisabled(true);
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
                if (getTemporaryHoldExpiry(z.getId()) > thExpiry) {
                    thExpiry = getTemporaryHoldExpiry(z.getId());
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
    
    public static long getTemporaryHoldExpiry(String zoneRef) {
        
        Equip q = HSUtil.getEquipFromZone(zoneRef);
        
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
            double preconRate = CCUHsApi.getInstance().getPredictedPreconRate(equipId);
            if (preconRate == 0) {
                //TODO - if no specific equip id precond rate, get system wide precon rate
                equipId = L.ccu().systemProfile.getSystemEquipRef();//get System default preconditioning rate
                if (tempDiff > 0)
                {
                    tempDiff = currentTemp - occu.getCoolingVal();
                    preconRate = TunerUtil.readTunerValByQuery("cooling and precon and rate", equipId);
                } else {
                    tempDiff = occu.getHeatingVal() - currentTemp;
                    preconRate = TunerUtil.readTunerValByQuery("heating and precon and rate", equipId);
                }
            }
            Log.d(TAG_CCU_SCHEDULER,"isZone in precon = "+preconRate+","+tempDiff +","+occu.getMillisecondsUntilNextChange()+","+currentTemp+","+desiredTemp);
            if ((occu.getMillisecondsUntilNextChange() > 0)&&  (tempDiff * preconRate * 60 * 1000 >= occu.getMillisecondsUntilNextChange()))
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
}
