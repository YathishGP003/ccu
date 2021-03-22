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
        HashMap site = CCUHsApi.getInstance().read("site");
        if (site == null || site.size() == 0 || !CCUHsApi.getInstance().isCCURegistered()) {
            CcuLog.d("CCU_ALERTS","No Site Registered or CCU is not registered (" + CCUHsApi.getInstance().isCCURegistered() + ")" + " <-AlertProcessJob ");
            return;
        }
        try
        {
            AlertManager.getInstance().processAlerts();
        }catch (Exception e) {
            CcuLog.d("CCU_ALERTS", "AlertProcessJob Exception: "+e.getMessage());
            e.printStackTrace();
        }
        CcuLog.d("CCU_ALERTS", "<-AlertProcessJob ");
    }
}
