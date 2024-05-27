package a75f.io.alerts;

import static a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS;

import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

public class AlertProcessJob
{
    private static final String             jobName = "AlertProcessJob";
    Context mContext;
    
    public AlertProcessJob(Context c) {
        mContext = c;
    }
    
    public Runnable getJobRunnable()
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                CcuLog.d(TAG_CCU_ALERTS, "Job Scheduling: " +jobName);
                doJob();
            }
        };
    }
    
    public void doJob()
    {
        CcuLog.d(TAG_CCU_ALERTS, "AlertProcessJob -> ");


        double isSafeMode = CCUHsApi.getInstance().readHisValByQuery("point and safe and mode and diag and his");
        boolean isSafeModeAlertActive = false;

        if(isSafeMode == 1){
            List<Alert> allAlertList = AlertManager.getInstance().getActiveAlerts();
            isSafeModeAlertActive = allAlertList.stream().anyMatch(alert -> Objects.equals(alert.mTitle, "CCU IN SAFE MODE"));
        }
        
        if (!CCUHsApi.getInstance().isCcuReady()) {
            if(isSafeMode == 1) {
                if (isSafeModeAlertActive){
                    CcuLog.i(TAG_CCU_ALERTS, "CCU not ready! <-AlertProcessJob ");
                    return;
                }
            }else {
                CcuLog.i(TAG_CCU_ALERTS, "CCU not ready! <-AlertProcessJob ");
                return;
            }
        }
        HashMap site = CCUHsApi.getInstance().read("site");
    
        CcuLog.d(TAG_CCU_ALERTS,"logAlert");
        CcuLog.d(TAG_CCU_ALERTS,"ActiveAlerts : "+AlertManager.getInstance().getActiveAlerts().size());
        CcuLog.d(TAG_CCU_ALERTS,"AllAlerts : "+AlertManager.getInstance().getAllAlertsOldestFirst().size());
        CcuLog.d(TAG_CCU_ALERTS,"AllExternalAlerts "+AlertManager.getInstance().getAllAlertsNotInternal().size());
        
        if (site == null || site.isEmpty() || !CCUHsApi.getInstance().isCCURegistered()) {
            CcuLog.d(TAG_CCU_ALERTS,"No Site Registered or CCU is not registered (" + CCUHsApi.getInstance().isCCURegistered() + ")" + " <-AlertProcessJob ");
            return;
        }
        try
        {
            AlertManager alertManager = AlertManager.getInstance();
            alertManager.fetchPredefinedAlertsIfEmpty();
            alertManager.processAlerts();
            alertManager.processAlertBox();
        } catch (Exception e) {
            CcuLog.e(TAG_CCU_ALERTS, "AlertProcessJob Exception: " + e.getMessage(), e);
            e.printStackTrace();
        }
        CcuLog.d(TAG_CCU_ALERTS, "<-AlertProcessJob ");
    }
}
