package a75f.io.api.haystack.sync;

import android.content.Context;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.xml.transform.Result;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SyncWorker extends Worker {
    
    public static final String TAG = "CCU_HS_SyncWork";
    public static final int ENTITY_SYNC_BATCH_SIZE = 25;
    public static final int DELETE_ENTITY_BATCH_SIZE = 50;
    
    public static final String ENDPOINT_ADD_ENTITY = "addEntity";
    public static final String ENDPOINT_REMOVE_ENTITY = "removeEntity";
    
    SiteRegistrationHandler siteHandler    = new SiteRegistrationHandler();
    CcuRegistrationHandler  ccuSyncHandler = new CcuRegistrationHandler();
    
    SyncStatusService syncStatusService;
    
    private static boolean isSyncWorkInProgress = false;
    
    public SyncWorker(Context appContext, WorkerParameters workerParams) {
        super(appContext, workerParams);
        syncStatusService = SyncStatusService.getInstance(getApplicationContext());
    }
    
    @NonNull
    @Override
    public Result doWork() {
    
        CcuLog.i(TAG, " doSyncWork ");
    
        isSyncWorkInProgress = true;
        try {
            if (!siteHandler.doSync()) {
                CcuLog.e(TAG, "Site sync failed");
                return Result.retry();
            }
            if (!CCUHsApi.getInstance().isCCURegistered()) {
                CcuLog.e(TAG, "Abort SyncWork : CCU Not registered");
                return Result.failure();
            }
            if (!ccuSyncHandler.doSync()) {
                CcuLog.e(TAG, "CCU sync failed");
                return Result.retry();
            }
            if (!syncUnSyncedEntities()) {
                CcuLog.e(TAG, "Unsynced entity sync failed");
                return Result.retry();
            }
            if (!syncUpdatedEntities()) {
                CcuLog.e(TAG, "Updated entity sync failed");
                return Result.retry();
            }
            if (!syncDeletedEntities()) {
                CcuLog.e(TAG, "Deleted entity sync failed");
                return Result.retry();
            }
            CcuLog.i(TAG, " doSyncWork success");
            syncStatusService.saveSyncStatus();
        } catch (Exception e) {
            CcuLog.i(TAG, " doSyncWork Failed ", e);
            return Result.failure();
        }
        isSyncWorkInProgress = false;
        return Result.success();
    }
    
    public static boolean isSyncWorkInProgress() {
        return isSyncWorkInProgress;
    }
    private boolean syncUnSyncedEntities() {
        
        if (!syncStatusService.hasUnSyncedData()) {
            return true;
        }
        HGridIterator unsyncedEntities = syncStatusService.getUnSyncedData();
        
        while (unsyncedEntities.hasNext()) {
            HGrid gridData = unsyncedEntities.next(ENTITY_SYNC_BATCH_SIZE);
            String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() +
                                                   ENDPOINT_ADD_ENTITY, HZincWriter.gridToString(gridData));
            CcuLog.d(TAG, "AddEntity Response : "+response);
            if (response != null) {
                updateSyncStatus(response, true);
            } else {
                return false;
            }
            
        }
        
        return true;
        
    }
    
    private boolean syncUpdatedEntities() {
        
        if (!syncStatusService.hasUpdatedData()) {
            return true;
        }
        CcuLog.d(TAG, "has updated data : "+syncStatusService.hasUpdatedData());
        HGridIterator updateEntities = syncStatusService.getUpdatedData();
        while (updateEntities.hasNext()) {
            HGrid gridData = updateEntities.next(ENTITY_SYNC_BATCH_SIZE);
            String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() +
                                                   ENDPOINT_ADD_ENTITY, HZincWriter.gridToString(gridData));
            CcuLog.d(TAG, "UpdateEntity Response : "+response);
            if (response != null) {
                updateSyncStatus(response, false);
            } else {
                return false;
            }
        }
        return true;
    }
    
    private boolean syncDeletedEntities() {
        
        if (!syncStatusService.hasDeletedData()) {
            return true;
        }
        
        List<List<String>> pointListBatches = ListUtils.partition(syncStatusService.getDeletedData()
                                                                        , DELETE_ENTITY_BATCH_SIZE);
        
        List<String> deletedItems = new ArrayList<>();
        pointListBatches.forEach( entityList -> {
            ArrayList<HDict> entities = new ArrayList<>();
            for (String deletedId : entityList) {
                HDictBuilder b = new HDictBuilder();
                b.add("id", HRef.make(deletedId.replace("@", "")));
                entities.add(b.toDict());
            }
            HGrid gridData = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() +
                                                   ENDPOINT_REMOVE_ENTITY, HZincWriter.gridToString(gridData));
    
            CcuLog.d(TAG, "RemoveEntity Response : "+response);
            if (response == null) {
                return;
            }
            deletedItems.addAll(entityList);
        });
    
        updateDeleteStatus(deletedItems);
        return true;
    }
    
    /**
     * First time syncing entities require point arrays to be written to backend.
     * @param response
     * @param pointWriteRequired
     */
    private void updateSyncStatus(String response, boolean pointWriteRequired) {
        ArrayList<String> syncedIds = retrieveIdsFromResponse(response);
        for (String id : syncedIds) {
            syncStatusService.setEntitySynced(id);
        }
        syncStatusService.saveSyncStatus();
        
        if (pointWriteRequired) {
            PointWriteUtil.sendPointArrayData(syncedIds);
        }
    }
    
    private void updateDeleteStatus(List<String> entityList) {
        ListIterator<String> deletedIr = entityList.listIterator();
        while(deletedIr.hasNext()) {
            syncStatusService.setDeletedEntitySynced(deletedIr.next());
        }
        syncStatusService.saveSyncStatus();
    }
    
    private ArrayList<String> retrieveIdsFromResponse(String response) {
        HZincReader zReader = new HZincReader(response);
        Iterator it = zReader.readGrid().iterator();
        ArrayList<String> syncedEntities = new ArrayList<>();
        while (it.hasNext()) {
            HRow row = (HRow) it.next();
            String syncedEntityId = row.get("id").toString();
            if (StringUtils.isNotBlank(syncedEntityId)) {
                syncedEntities.add(syncedEntityId);
            }
        }
        return syncedEntities;
    }
}
