package a75f.io.logic.jobs;

import static a75f.io.logic.L.TAG_CCU_JOB;

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
