package a75f.io.api.haystack.sync;

import android.content.Context;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
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
    public static final String POINT_WRITE_WORK_TAG = "SYNC_WORK";
    
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
        
        if (isMigrationRequired()) {
            CcuLog.d(TAG, "Migration Required");
            WorkManager.getInstance(appContext).beginUniqueWork(SYNC_WORK_TAG,
                                                workPolicyKeep ? ExistingWorkPolicy.KEEP : ExistingWorkPolicy.REPLACE,
                                                getMigrationWorkRequest())
                                                .then(getSyncWorkRequest())
                                                .enqueue();
        } else {
            CcuLog.d(TAG, "Migration not Required");
            WorkManager.getInstance(appContext).beginUniqueWork(SYNC_WORK_TAG,
                                                workPolicyKeep ? ExistingWorkPolicy.KEEP : ExistingWorkPolicy.REPLACE,
                                                getSyncWorkRequest())
                                                .enqueue();
        }
    }
    
    public void syncPointArray() {
    
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
        CcuLog.d(TAG, "syncEntitiesWithPointWrite");
        
        if (!CCUHsApi.getInstance().isCCURegistered()) {
            CcuLog.e(TAG, "Skip Entity Sync : CCU Not registered");
            return;
        }
    
        CcuLog.d(TAG, "syncEntities");
        if (mSyncTimerTask != null) {
            mSyncTimerTask.cancel();
            mSyncTimerTask = null;
        }
    
        if (isMigrationRequired()) {
            CcuLog.d(TAG, "Migration Required");
            WorkManager.getInstance(appContext).beginUniqueWork(SYNC_WORK_TAG,
                                                                ExistingWorkPolicy.REPLACE,
                                                                getMigrationWorkRequest())
                       .then(getSyncWorkRequest())
                       .then(getPointWriteWorkRequest())
                       .enqueue();
        } else {
            CcuLog.d(TAG, "Migration not Required");
            WorkManager.getInstance(appContext).beginUniqueWork(SYNC_WORK_TAG,
                                                                ExistingWorkPolicy.REPLACE,
                                                                getSyncWorkRequest())
                       .then(getPointWriteWorkRequest())
                       .enqueue();
        }
    }
    
    
    
    private Constraints getSyncConstraints() {
        return new Constraints.Builder()
                   .setRequiredNetworkType(NetworkType.CONNECTED)
                   .build();
    }
    
    private OneTimeWorkRequest getMigrationWorkRequest() {
        
        return new OneTimeWorkRequest.Builder(MigrationWorker.class)
                   .setConstraints(getSyncConstraints())
                   .setBackoffCriteria(
                       BackoffPolicy.LINEAR,
                       OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                       TimeUnit.MILLISECONDS)
                   .addTag(SYNC_WORK_TAG)
                   .build();
        
    }
    
    private OneTimeWorkRequest getSyncWorkRequest() {
        
        return new OneTimeWorkRequest.Builder(SyncWorker.class)
                                        .setConstraints(getSyncConstraints())
                                        .setBackoffCriteria(
                                            BackoffPolicy.LINEAR,
                                            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                            TimeUnit.MILLISECONDS)
                                        .addTag(SYNC_WORK_TAG)
                                        .build();
        
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
    
    
    
    private boolean isMigrationRequired() {
        boolean migration = false;
        if (CCUHsApi.getInstance().getIdMap().size() > 0) {
            migration = true;
        }
        if (CCUHsApi.getInstance().getUpdateIdMap().size() > 0) {
            migration = true;
        }
        if (CCUHsApi.getInstance().getRemoveIdMap().size() > 0) {
            migration = true;
        }
        return migration;
    }
    
    public void scheduleSync() {
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
    
    /*@Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSyncEvent(SyncEvent event) {
        CcuLog.i(TAG, "onSyncEvent : "+event.getSyncStatus());
        if (event.getSyncStatus() == SyncEvent.SyncStatus.FAILED ||
                                event.getSyncStatus() == SyncEvent.SyncStatus.COMPLETED) {
            EventBus.getDefault().unregister(this);
        }
        
        if (event.getSyncStatus() == SyncEvent.SyncStatus.FAILED) {
            scheduleSync();
        }
        
    }*/
    
    public boolean isEntitySyncProgress() {
        return SyncWorker.isSyncWorkInProgress();
    }
}
