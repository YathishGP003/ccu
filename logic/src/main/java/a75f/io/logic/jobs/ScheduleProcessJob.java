package a75f.io.logic.jobs;

import static a75f.io.logic.L.TAG_CCU_JOB;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BaseJob;
import a75f.io.logic.Globals;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.watchdog.WatchdogMonitor;

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

    static HashMap<String, Occupied> occupiedHashMap = new HashMap<String, Occupied>();
    public static Occupied getOccupiedModeCache(String id) {
        if(!occupiedHashMap.containsKey(id))
        {
            return null;
        }
        return occupiedHashMap.get(id);
    }

    boolean watchdogMonitor = false;
    
    private Lock jobLock = new ReentrantLock();

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
                if(!currentOccupiedMode.isOccupied()){
                    checkforOccUpdate(occupied);
                }
                Log.i("SchedulerCache", "Putting in new occupied values");
                occupiedHashMap.put(id, occupied);
                return true;
            }
        }

        return false;

    }

    private static void checkforOccUpdate(Occupied occupied) {
        if(occupied.isOccupied() ){
            //Read all OTN equips
            ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone and otn");
            for(HashMap hs : equips)
            {
                HashMap ocupancyDetection = CCUHsApi.getInstance().read(
                        "point and  otn and occupancy and detection and his and equipRef  ==" +
                                " \"" + hs.get("id") + "\"");
                if (ocupancyDetection.get("id") != null) {
                    double val = CCUHsApi.getInstance().readHisValById(ocupancyDetection.get(
                            "id").toString());
                    CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(ocupancyDetection.get(
                            "id").toString(),
                            val);
                }

            }

        }
    }


    @Override
    public void doJob() {

        CcuLog.d(TAG_CCU_JOB,"ScheduleProcessJob-> ");
        watchdogMonitor = false;
        
        if (!CCUHsApi.getInstance().isCCURegistered() ||
                            !CCUHsApi.getInstance().isCCUConfigured() || Globals.getInstance().isRecoveryMode() ||
                Globals.getInstance().isSafeMode()) {
            CcuLog.d(TAG_CCU_JOB,"ScheduleProcessJob <- CCU not configured ");
            return;
        }
        
        if (jobLock.tryLock()) {
            try {
                ScheduleManager.getInstance().processSchedules();
                CcuLog.d(TAG_CCU_JOB,"<- ScheduleProcessJob");
            } catch (Exception e) {
                CcuLog.d(TAG_CCU_JOB,"ScheduleProcessJob Failed ", e);
            } finally {
                jobLock.unlock();
            }
        } else {
            CcuLog.d(TAG_CCU_JOB,"ScheduleProcessJob<- Job instance running ");
        }
    }
}
