package a75f.io.logic.jobs;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import org.greenrobot.essentials.StringUtils;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.Globals;
import a75f.io.logic.L;

public class BearerTokenManagementJob extends JobService {
    
    private static final int BEARER_TOKEN_JOB_ID = 1000;
    private static final long JOB_INTERVAL = 24 * 60 * 60000 ;
    
    @Override
    public boolean onStartJob(JobParameters params) {
        if (!CCUHsApi.getInstance().getJwt().isEmpty()) {
            refreshToken();
        }
        return true;
    }
    
    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
    
    private void refreshToken() {
        Log.d(L.TAG_CCU_JOB, " BearerTokenManagementJob : fetchToken");
    }
    
    public static void scheduleJob() {
        Log.d(L.TAG_CCU_JOB, " BearerTokenManagementJob : scheduleJob");
        Context appContext = Globals.getInstance().getApplicationContext();
        JobScheduler jobScheduler = (JobScheduler) appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(new JobInfo.Builder(BEARER_TOKEN_JOB_ID,
                                                  new ComponentName(appContext, BearerTokenManagementJob.class))
                                  .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                                  .setPeriodic(JOB_INTERVAL)
                                  .build());
    }
}
