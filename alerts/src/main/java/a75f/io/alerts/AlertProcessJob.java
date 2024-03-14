package a75f.io.alerts;

import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import a75f.io.api.haystack.Alert;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

public class AlertProcessJob
{
    private static final String TAG = "CCU_ALERTS";
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
                CcuLog.d(TAG, "Job Scheduling: " +jobName);
                doJob();
            }
        };
    }
    
    public void doJob()
    {
        CcuLog.d(TAG, "AlertProcessJob -> ");


        double isSafeMode = CCUHsApi.getInstance().readHisValByQuery("point and safe and mode and diag and his");
        boolean isSafeModeAlertActive = false;

        if(isSafeMode == 1){
            List<Alert> allAlertList = AlertManager.getInstance().getActiveAlerts();
            isSafeModeAlertActive = allAlertList.stream().anyMatch(alert -> Objects.equals(alert.mTitle, "CCU IN SAFE MODE"));
        }
        
        if (!CCUHsApi.getInstance().isCcuReady()) {
            if(isSafeMode == 1) {
                if (isSafeModeAlertActive){
                    CcuLog.d(TAG, "CCU not ready! <-AlertProcessJob ");
                    return;
                }
            }else {
                CcuLog.d(TAG, "CCU not ready! <-AlertProcessJob ");
                return;
            }
        }
        HashMap site = CCUHsApi.getInstance().read("site");
    
        CcuLog.d(TAG,"logAlert");
        CcuLog.d(TAG,"ActiveAlerts : "+AlertManager.getInstance().getActiveAlerts().size());
        CcuLog.d(TAG,"AllAlerts : "+AlertManager.getInstance().getAllAlertsOldestFirst().size());
        CcuLog.d(TAG,"AllExternalAlerts "+AlertManager.getInstance().getAllAlertsNotInternal().size());
        
        if (site == null || site.size() == 0 || !CCUHsApi.getInstance().isCCURegistered()) {
            CcuLog.d(TAG,"No Site Registered or CCU is not registered (" + CCUHsApi.getInstance().isCCURegistered() + ")" + " <-AlertProcessJob ");
            return;
        }
        try
        {
            AlertManager alertManager = AlertManager.getInstance();
            alertManager.fetchPredefinedAlertsIfEmpty();
            alertManager.processAlerts();
            alertManager.processAlertBox();
        } catch (Exception e) {
            CcuLog.w(TAG, "AlertProcessJob Exception: " + e.getMessage(), e);
            e.printStackTrace();
        }
        CcuLog.d(TAG, "<-AlertProcessJob ");
    }
}
