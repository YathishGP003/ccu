package a75f.io.alerts;

import android.content.Context;

import java.util.HashMap;

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
        
        if (!CCUHsApi.getInstance().isCcuReady()) {
            CcuLog.d(TAG, "CCU not ready! <-AlertProcessJob ");
            return;
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
            // We may need to initialize alerts service here (via AlertManager) if this is the first
            // alerts sync after registration.
            if (!alertManager.hasService()) {
                String token = CCUHsApi.getInstance().getJwt();
                if (token.isEmpty()) {
                    CcuLog.w(TAG, "AlertProcessJob:  Site exists but token is empty");
                    return;
                }
                alertManager.rebuildServiceNewToken(token);
            }

            alertManager.processAlerts();

        }catch (Exception e) {
            CcuLog.w(TAG, "AlertProcessJob Exception: " + e.getMessage(), e);
            e.printStackTrace();
        }
        CcuLog.d(TAG, "<-AlertProcessJob ");
    }
}
