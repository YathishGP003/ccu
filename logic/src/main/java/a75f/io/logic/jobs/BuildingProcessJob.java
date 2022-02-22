package a75f.io.logic.jobs;

import android.util.Log;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BaseJob;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.diag.DiagEquip;
import a75f.io.logic.pubnub.PbSubscriptionHandler;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.watchdog.WatchdogMonitor;

/**
 * Created by samjithsadasivan on 9/14/18.
 */

public class BuildingProcessJob extends BaseJob implements WatchdogMonitor
{
    boolean watchdogMonitor = false;

    private final Lock jobLock  = new ReentrantLock();

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
        CcuLog.d(L.TAG_CCU_JOB,"BuildingProcessJob -> "+CCUHsApi.getInstance());
        Thread.currentThread().setName("BuildingProcessJobs");
        watchdogMonitor = false;
        
        L.pingCloudServer();
    
        if (!CCUHsApi.getInstance().isCcuReady()) {
            CcuLog.d(L.TAG_CCU_JOB,"CCU not ready ! <-BuildingProcessJob ");
            return;
        }
        
        if (jobLock.tryLock()) {
            try {
                HashMap<Object, Object> site = CCUHsApi.getInstance().readEntity("site");
                if (site.isEmpty()) {
                    CcuLog.d(L.TAG_CCU_JOB,"No Site Registered ! <-BuildingProcessJob ");
                    return;
                }

                HashMap<Object, Object> ccu = CCUHsApi.getInstance().readEntity("ccu");
                
                if (ccu.isEmpty()) {
                    CcuLog.d(L.TAG_CCU_JOB,"No CCU Registered ! <-BuildingProcessJob ");
                    return;
                }
                
                BuildingTunerCache.getInstance().updateTuners();
                
                for (ZoneProfile profile : L.ccu().zoneProfiles) {
                    profile.updateZonePoints();
                }
                
                boolean useMessagingApi = Globals.getInstance().isAckdMessagingEnabled();
                if (!useMessagingApi && !PbSubscriptionHandler.getInstance().isPubnubSubscribed()) {
                    CCUHsApi.getInstance().syncEntityTree();
                    if (CCUHsApi.getInstance().siteSynced()) {
                        String siteUID = CCUHsApi.getInstance().getSiteIdRef().toString();
                        PbSubscriptionHandler.getInstance().registerSite(Globals.getInstance().getApplicationContext(),
                                                                         siteUID);
                    }
                }
                if (L.ccu().oaoProfile != null) {
                    L.ccu().oaoProfile.doOAO();
                } else {
                    CCUHsApi.getInstance().writeHisValByQuery("point and sp and system and epidemic and mode and state",
                                                              (double) EpidemicState.OFF.ordinal());
                }

                if (!Globals.getInstance().isTestMode() && !Globals.getInstance().isTemporaryOverrideMode()) {
                    L.ccu().systemProfile.doSystemControl();
                }
                handleSync();
    
                CcuLog.d(L.TAG_CCU_JOB,"<- BuildingProcessJob");
            } catch (Exception e) {
                CcuLog.e(L.TAG_CCU_JOB, "BuildingProcessJob Failed ! ", e);
            } finally {
                jobLock.unlock();
            }
        } else {
            CcuLog.d(L.TAG_CCU_JOB,"<- BuildingProcessJob : Previous Instance of job still running");
        }
    }
    
    private void handleSync() {

        new Thread() {
            @Override
            public void run() {
                try {
                    CCUHsApi.getInstance().syncHisData();
                } catch (Exception e) {
                    //This is bad. But the system could still continue to work in standalone mode controlling
                    //the hvac system even if there are failures in data synchronization with backend.
                    CcuLog.e(L.TAG_CCU_JOB, "His Sync Failed !", e);
                }
            }
        }.start();

        DateTime now = new DateTime();
        boolean timeForEntitySync = now.getMinuteOfDay() % 15 == 0;
        if (timeForEntitySync) {
            L.saveCCUState();
            CCUHsApi.getInstance().scheduleSync();
        }
        //Save CCU state every other minute. This could be expensive if the local entity count is high.
        if (now.getMinuteOfDay() % 2 == 0) {
            L.saveCCUState();
        }
    }
}