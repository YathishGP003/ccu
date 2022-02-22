package a75f.io.alerts;

import android.content.Context;

import java.util.HashMap;

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
                CcuLog.d("CCU_ALERTS", "Job Scheduling: " +jobName);
                doJob();
            }
        };
    }
    
    public void doJob()
    {
        CcuLog.d("CCU_ALERTS", "AlertProcessJob -> ");
        Thread.currentThread().setName("AlertProcessJob");
        
        if (!CCUHsApi.getInstance().isCcuReady()) {
            CcuLog.d("CCU_ALERTS", "CCU not ready! <-AlertProcessJob ");
            return;
        }
        HashMap site = CCUHsApi.getInstance().read("site");
    
        CcuLog.d("CCU_ALERTS","logAlert");
        CcuLog.d("CCU_ALERTS","ActiveAlerts : "+AlertManager.getInstance().getActiveAlerts().size());
        CcuLog.d("CCU_ALERTS","AllAlerts : "+AlertManager.getInstance().getAllAlertsOldestFirst().size());
        CcuLog.d("CCU_ALERTS","AllExternalAlerts "+AlertManager.getInstance().getAllAlertsNotInternal().size());
        
        if (site == null || site.size() == 0 || !CCUHsApi.getInstance().isCCURegistered()) {
            CcuLog.d("CCU_ALERTS","No Site Registered or CCU is not registered (" + CCUHsApi.getInstance().isCCURegistered() + ")" + " <-AlertProcessJob ");
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
                    CcuLog.w("CCU_ALERTS", "AlertProcessJob:  Site exists but token is empty");
                    return;
                }
                alertManager.rebuildServiceNewToken(token);
            }

            alertManager.processAlerts();

        }catch (Exception e) {
            CcuLog.w("CCU_ALERTS", "AlertProcessJob Exception: " + e.getMessage(), e);
            e.printStackTrace();
        }
        CcuLog.d("CCU_ALERTS", "<-AlertProcessJob ");
    }
}
