package a75f.io.logic.jobs;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.sync.PointWriteCache;
import a75f.io.api.haystack.sync.PointWriteHandler;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BaseJob;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.watchdog.WatchdogMonitor;
import a75f.io.util.ExecutorTask;

/**
 * Created by samjithsadasivan on 9/14/18.
 */

public class BuildingProcessJob extends BaseJob implements WatchdogMonitor
{
    boolean watchdogMonitor = false;

    private final Lock jobLock  = new ReentrantLock();

    private boolean status = false;

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
        CcuLog.d(L.TAG_CCU_JOB,"BuildingProcessJob -> ");
        watchdogMonitor = false;
    
        CCUHsApi.getInstance().incrementAppAliveCount();
        CcuLog.d(L.TAG_CCU_JOB,"AppAliveMinutes : "+CCUHsApi.getInstance().getAppAliveMinutes());
        if (CCUHsApi.getInstance().getAppAliveMinutes() % 15 == 0) {
            CCUHsApi.getInstance().getHisSyncHandler().setNonCovSyncPending();
        }
        
        L.pingCloudServer();
        
        if (!CCUHsApi.getInstance().isCCUConfigured() || Globals.getInstance().isRecoveryMode() ) {
            CcuLog.d(L.TAG_CCU_JOB,"CCU not configured ! <-BuildingProcessJob ");
            return;
        }
        
        if (!CCUHsApi.getInstance().isCcuReady()) {
            if (Globals.getInstance().isSafeMode()) {
                handleSync();
            }
            CcuLog.d(L.TAG_CCU_JOB,"CCU not ready ! <-BuildingProcessJob ");
            return;
        }
        
        if (jobLock.tryLock()) {
            try {
                
                BuildingTunerCache.getInstance().updateTuners();
                
                //handleMessagingRegistration();
    
                runZoneProfilesAlgorithm();
                
                runOAOAlgorithm();

                runBypassDamperAlgorithm();

                runSystemControlAlgorithm();

                syncCachedPointWrites();
                status = true;
                CcuLog.d(L.TAG_CCU_JOB,"<- BuildingProcessJob");

            } catch (Exception e) {
                CcuLog.e(L.TAG_CCU_JOB, "BuildingProcessJob Failed ! ", e);
                status = false;
            } finally {
                jobLock.unlock();
                
            }
            handleSync();
        } else {
            CcuLog.d(L.TAG_CCU_JOB,"<- BuildingProcessJob : Previous Instance of job still running");
        }
    }

    private void runZoneProfilesAlgorithm() {
        for (ZoneProfile profile : L.ccu().zoneProfiles) {
            try {
                profile.updateZonePoints();
            } catch (Exception e) {
                CcuLog.e(L.TAG_CCU_JOB, "runZoneProfilesAlgorithm Failed ! ", e);
                e.printStackTrace();
            }
        }
    }
    
    private void runOAOAlgorithm() {
        try{
            if (L.ccu().oaoProfile != null) {
                L.ccu().oaoProfile.doOAO();
            } else {
                CCUHsApi.getInstance().writeHisValByQuery("point and sp and system and epidemic and mode and state",
                                                          (double) EpidemicState.OFF.ordinal());
            }
        } catch (Exception e){
            CcuLog.e(L.TAG_CCU_JOB, "runOAOAlgorithm Failed ! ", e);
        }
    }

    private void runBypassDamperAlgorithm() {
        try{
            if (L.ccu().bypassDamperProfile != null) {
                L.ccu().bypassDamperProfile.updateZonePoints();
            }
        } catch (Exception e){
            CcuLog.e(L.TAG_CCU_JOB, "runBypassDamperAlgorithm Failed ! ", e);
        }
    }
    
    private void runSystemControlAlgorithm() {
       try {
            if (!Globals.getInstance().isTestMode() && !Globals.getInstance().isTemporaryOverrideMode()) {
                L.ccu().systemProfile.doSystemControl();
            }
       } catch (Exception e) {
           CcuLog.e(L.TAG_CCU_JOB, "runSystemControlAlgorithm Failed ! ", e);
       }
    }
    
    private void handleSync() {
        doHisSync();
        long appAliveMinutes = CCUHsApi.getInstance().getAppAliveMinutes();
        boolean timeForEntitySync = appAliveMinutes % 15 == 0;
        if (timeForEntitySync) {
            CCUHsApi.getInstance().scheduleSync();
        }
        //Save CCU state every other minute. This could be expensive if the local entity count is high.
        if (appAliveMinutes % 2 == 0) {
            L.saveCCUState();
        }
    }
    
    private void doHisSync() {
        ExecutorTask.executeBackground( () -> {
            try {
                CCUHsApi.getInstance().syncHisDataWithPeriodicPurge();
            } catch (Exception e) {
                //This is bad. But the system could still continue to work in standalone mode controlling
                //the hvac system even if there are failures in data synchronization with backend.
                CcuLog.e(L.TAG_CCU_JOB, "His Sync Failed !", e);
            }
        });
    }
    private void syncCachedPointWrites() {
        PointWriteHandler.Companion.getInstance().processPointWrites(
                PointWriteCache.Companion.getInstance().getPointWriteList());
    }
    public boolean getStatus() {
        return status;
    }
}