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
    
    public MigrationWorker(Context appContext, WorkerParameters workerParams) {
        super(appContext, workerParams);
        syncStatusService = SyncStatusService.getInstance(getApplicationContext());
    }
    
    @NonNull
    @Override
    public Result doWork() {
        CcuLog.i(TAG, " doWork ");
        processUnSyncedItems();
        processUpdatedItems();
        processDeletedItems();
        CcuLog.i(TAG, " doWork success");
        return Result.success();
    }
    
    private void processUnSyncedItems() {
       
        HGrid allEntitiesGrid = CCUHsApi.getInstance()
                                        .readGrid("site or floor or room or equip or device or point or schedule");
        
        Iterator<HDict> gridIterator = allEntitiesGrid.iterator();
    
        while (gridIterator.hasNext()) {
            HDict entity = gridIterator.next();
            String entityId = entity.get(Tags.ID).toString();
            String entityGUID = CCUHsApi.getInstance().getGUID(entityId);
            if ( entityGUID != null) {
                HDictBuilder updatedEntity = new HDictBuilder().add(entity);
                updatedEntity.add(Tags.ID, entityGUID);
                CCUHsApi.getInstance().addEntity(updatedEntity.toDict());
                CCUHsApi.getInstance().getIdMap().remove(entityId);
            } else {
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
        ConcurrentHashMap<String, String> updateIdMap = CCUHsApi.getInstance().getRemoveIdMap();
        for (Map.Entry removeEntry : updateIdMap.entrySet()) {
            syncStatusService.addUpdatedEntity(removeEntry.getKey().toString());
            updateIdMap.remove(removeEntry.getKey());
        }
    }
    
}

