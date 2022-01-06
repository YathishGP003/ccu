package a75f.io.api.haystack.sync;

import android.content.Context;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MigrationWorker extends Worker {
    
    private static final String TAG = "CCU_HS_MigrationWorker";
    SyncStatusService syncStatusService;
    Context appContext;
    public MigrationWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
        appContext = context;
        syncStatusService = SyncStatusService.getInstance(getApplicationContext());
    }
    
    @NonNull
    @Override
    public Result doWork() {
        CcuLog.i(TAG, " doWork Migration");
        processUnSyncedItems();
        processUpdatedItems();
        processDeletedItems();
        syncStatusService.saveSyncStatus();
        CCUHsApi.getInstance().saveTagsData();
        PreferenceUtil.setUuidMigrationCompleted(true, appContext);
        CcuLog.i(TAG, " doWork Migration Success");
        return Result.success();
    }
    
    private void processUnSyncedItems() {
       
        HGrid allEntitiesGrid = CCUHsApi.getInstance()
                                        .readGrid("site or floor or room or equip or device or point or schedule");
        
        Iterator<HDict> gridIterator = allEntitiesGrid.iterator();
    
        ConcurrentHashMap<String, String> idMap = CCUHsApi.getInstance().getIdMap();
        while (gridIterator.hasNext()) {
            HDict entity = gridIterator.next();
            String entityId = entity.get(Tags.ID).toString();
            
            if (idMap.containsKey(entityId)) {
                //Commenting this temporarily. Removing idmap will make it impossible to downgrade to a lower version.
                //CCUHsApi.getInstance().getIdMap().remove(entityId);
            } else {
                CcuLog.i(TAG, "Migration Unsynced data "+entity.get("dis"));
                syncStatusService.addUnSyncedEntity(entityId);
            }
        }
        
    }
    
    private void processUpdatedItems() {
        ConcurrentHashMap<String, String> updateIdMap = CCUHsApi.getInstance().getUpdateIdMap();
        for (Map.Entry updateEntry : updateIdMap.entrySet()) {
            syncStatusService.addUpdatedEntity(updateEntry.getKey().toString());
            updateIdMap.remove(updateEntry.getKey());
        }
    }
    
    private void processDeletedItems() {
        ConcurrentHashMap<String, String> removeIdMap = CCUHsApi.getInstance().getRemoveIdMap();
        for (Map.Entry removeEntry : removeIdMap.entrySet()) {
            syncStatusService.addDeletedEntity(removeEntry.getKey().toString(), true);
            removeIdMap.remove(removeEntry.getKey());
        }
        syncStatusService.saveSyncStatus();
    }
    
}

