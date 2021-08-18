package a75f.io.logic.jobs;

import android.util.Log;

import org.joda.time.DateTime;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BaseJob;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.diag.DiagEquip;
import a75f.io.logic.pubnub.PbSubscriptionHandler;
import a75f.io.logic.watchdog.WatchdogMonitor;

/**
 * Created by samjithsadasivan on 9/14/18.
 */

public class BuildingProcessJob extends BaseJob implements WatchdogMonitor
{
    boolean watchdogMonitor = false;
    
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
        
        watchdogMonitor = false;
        
        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() == 0) {
            CcuLog.d(L.TAG_CCU_JOB,"No Site Registered ! <-BuildingProcessJob ");
            return;
        }
    
        HashMap ccu = CCUHsApi.getInstance().read("ccu");
        if (ccu.size() == 0) {
            CcuLog.d(L.TAG_CCU_JOB,"No CCU Registered ! <-BuildingProcessJob ");
            return;
        }
        L.pingCloudServer();
        
        DiagEquip.getInstance().updatePoints();

        try {
            //TODO Crash here causing issues in Analytics portal #RENATUS-396 kumar
            for (ZoneProfile profile : L.ccu().zoneProfiles) {
                profile.updateZonePoints();
            }

            if (!PbSubscriptionHandler.getInstance().isPubnubSubscribed()) {
                CCUHsApi.getInstance().syncEntityTree();
                if (CCUHsApi.getInstance().siteSynced()) {
                    String siteUID = CCUHsApi.getInstance().getSiteIdRef().toString();
                    PbSubscriptionHandler.getInstance().registerSite(Globals.getInstance().getApplicationContext(), siteUID);
                }
            }

            if (L.ccu().oaoProfile != null) {
                L.ccu().oaoProfile.doOAO();
            } else {
                CCUHsApi.getInstance().writeHisValByQuery("point and sp and system and epidemic and mode and state", (double) EpidemicState.OFF.ordinal());
            }

            if (!Globals.getInstance().isTestMode() && !Globals.getInstance().isTemproryOverrideMode()) {
                L.ccu().systemProfile.doSystemControl();
            }

            L.saveCCUState();

            new Thread() {
                @Override
                public void run() {
                    try {
                        CCUHsApi.getInstance().syncHisData();
                    } catch (Exception e) {
                        //We do understand the consequences of doing this.
                        //But the system could still continue to work in standalone mode controlling the hvac system
                        //even if there are failures in data synchronization with backend.
                        CcuLog.e(L.TAG_CCU_JOB, "His Sync Failed !", e);
                    }
                }
            }.start();

            DateTime now = new DateTime();
            boolean timeForEntitySync = now.getMinuteOfDay() % 15 == 0 ? true : false;
            if (timeForEntitySync) {
                CCUHsApi.getInstance().scheduleSync();
            }

        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_JOB, "BuildingProcessJob Failed ! ", e);
        }

        CcuLog.d(L.TAG_CCU_JOB,"<- BuildingProcessJob");
    }
}
