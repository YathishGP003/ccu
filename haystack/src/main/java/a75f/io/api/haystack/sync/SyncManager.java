package a75f.io.api.haystack.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.util.ExecutorTask;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class SyncManager {

    public static final String TAG = "CCU_HS_SyncManager";

    public static final long SYNC_SCHEDULE_INTERVAL_MILLIS = 30000;

    public static final String SYNC_WORK_TAG = "SYNC_WORK";
    public static final String POINT_WRITE_WORK_TAG = "POINT_WRITE_SYNC_WORK";

    Timer     mSyncTimer     = new Timer();
    TimerTask mSyncTimerTask = null;

    Context appContext;

    public SyncManager(Context context) {
        appContext = context;
    }

    /**
     * Fires a sync work.
     * When workPolicyKeep is true ,sync work is kept in queue and retried until it is successful
     * with linearly increasing back-off delay. Meanwhile if anther work is scheduled with the same policy , it is
     * ignored.
     * When workPolicyKeep is false, any previous sync work in queue is removed and a new work is scheduled immediately.
     */
    public void syncEntities(boolean workPolicyKeep) {

        if(CCUHsApi.getInstance().readDefaultVal("offline and mode and point") > 0) {
            CcuLog.d("CCU_HS"," Skip his sync in offlineMode");
            return;
        }

        CcuLog.d(TAG, "syncEntities");
        if (mSyncTimerTask != null) {
            mSyncTimerTask.cancel();
            mSyncTimerTask = null;
        }

        if (SyncStatusService.getInstance(appContext).isSyncNotRequired()) {
            CcuLog.d(TAG, "<- syncEntities : No pending items to sync");
            return;
        }

        if (SyncWorker.isSyncWorkInProgress()) {
            CcuLog.d(TAG, "<- syncEntities : SyncWork in progress");
            return;
        }

        restartSync(workPolicyKeep);
    }

    public void syncPointArray() {
        if(CCUHsApi.getInstance().readDefaultVal("offline and mode and point") > 0) {
            CcuLog.d("CCU_HS"," Skip his sync in offlineMode");
            return;
        }

        CcuLog.d(TAG, "syncPointArray : Migration not required");
        WorkManager.getInstance(appContext).beginUniqueWork(POINT_WRITE_WORK_TAG,
                                                            ExistingWorkPolicy.REPLACE,
                                                            getPointWriteWorkRequest())
                                            .enqueue();

    }

    /**
     * Queue a sync work followed by pointWrite work replacing any pending work in queue.
     */
    public void syncEntitiesWithPointWrite() {
        if(CCUHsApi.getInstance().readDefaultVal("offline and mode and point") > 0) {
            CcuLog.d("CCU_HS"," Skip his sync in offlineMode");
            return;
        }

        CcuLog.d(TAG, "syncEntitiesWithPointWrite");
        syncEntitiesWithPointWriteWithDelay(0);
    }

    public void syncEntitiesWithPointWriteWithDelay(long delaySeconds) {
        if(CCUHsApi.getInstance().readDefaultVal("offline and mode and point") > 0) {
            CcuLog.d("CCU_HS"," Skip his sync in offlineMode");
            return;
        }

        if (!CCUHsApi.getInstance().isCCURegistered()) {
            CcuLog.e(TAG, "Skip Entity Sync : CCU Not registered");
            return;
        }

        CcuLog.d(TAG, "syncEntities");
        if (mSyncTimerTask != null) {
            mSyncTimerTask.cancel();
            mSyncTimerTask = null;
        }

        WorkManager.getInstance(appContext).beginUniqueWork(SYNC_WORK_TAG,
                        ExistingWorkPolicy.REPLACE,
                        getSyncWorkRequestWithDelay(delaySeconds))
                .then(getPointWriteWorkRequest())
                .enqueue();
    }



    private Constraints getSyncConstraints() {
        return new Constraints.Builder()
                   .setRequiredNetworkType(NetworkType.CONNECTED)
                   .build();
    }

    private OneTimeWorkRequest getSyncWorkRequest() {
        return getSyncWorkRequestBuilder().build();
    }

    private OneTimeWorkRequest.Builder getSyncWorkRequestBuilder() {
        return new OneTimeWorkRequest.Builder(SyncWorker.class)
                   .setConstraints(getSyncConstraints())
                   .setBackoffCriteria(
                       BackoffPolicy.LINEAR,
                       OneTimeWorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS,
                       TimeUnit.MILLISECONDS)
                   .addTag(SYNC_WORK_TAG);
    }

    private OneTimeWorkRequest getSyncWorkRequestWithDelay(long delaySeconds) {
        return getSyncWorkRequestBuilder().setInitialDelay(delaySeconds, TimeUnit.SECONDS).build();
    }

    private OneTimeWorkRequest getPointWriteWorkRequest() {
        return new OneTimeWorkRequest.Builder(PointWriteWorker.class)
                                        .setConstraints(getSyncConstraints())
                                        .setBackoffCriteria(
                                            BackoffPolicy.LINEAR,
                                            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                            TimeUnit.MILLISECONDS)
                                        .addTag(POINT_WRITE_WORK_TAG)
                                        .build();
    }
    
    public void scheduleSync() {
        if(CCUHsApi.getInstance().readDefaultVal("offline and mode and point") > 0) {
            CcuLog.d("CCU_HS"," Skip his sync in offlineMode");
            return;
        }

        if (mSyncTimerTask != null) {
            return;
        }
        
        mSyncTimerTask = new TimerTask() {
            public void run() {
                CcuLog.i(TAG, "Entity Sync Scheduled");
                syncEntities(false);
                mSyncTimerTask = null;
            }
        };
        mSyncTimer.schedule(mSyncTimerTask, SYNC_SCHEDULE_INTERVAL_MILLIS);
    }

    public boolean isEntitySyncProgress() {
        return SyncWorker.isSyncWorkInProgress();
    }

    public void syncNow() {
        CcuLog.d(TAG, "syncNow called on invoked on thread");
        ExecutorTask.executeBackground(() -> SyncWorker.performSyncWork(SyncStatusService.getInstance(appContext)));
    }

    public void restartSync(boolean workPolicyKeep) {
        WorkManager wm = WorkManager.getInstance(appContext);
        wm.cancelUniqueWork(SYNC_WORK_TAG);
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                        wm.beginUniqueWork(
                                SYNC_WORK_TAG,
                                ExistingWorkPolicy.KEEP,
                                getSyncWorkRequest()
                        ).enqueue(),
                500
        );
    }
}
