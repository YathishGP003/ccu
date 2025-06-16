package a75f.io.api.haystack.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.logger.CcuLog;

public class SyncStatusService {
    
    private static final String PREFS_ID_LIST_UNSYNCED = "unsyncedIdList";
    private static final String PREFS_ID_LIST_UPDATED  = "updatedIdList";
    private static final String PREFS_ID_LIST_DELETED  = "deletedIdList";
    
    Context applicationContext;
    private final SharedPreferences preferences;
    
    private List<String> unsyncedIdList;
    private List<String> updatedIdList;
    private List<String> deletedIdList;
    
    private static SyncStatusService instance = null;
    
    public static HashSet<String> refTypes = new HashSet<>();
    
    private Timer fileSaveDelayTimer;
    
    static
    {
        refTypes.add("siteRef");
        refTypes.add("equipRef");
        refTypes.add("deviceRef");
        refTypes.add("pointRef");
        refTypes.add("floorRef");
        refTypes.add("roomRef");
        refTypes.add("ahuRef");
        refTypes.add("gatewayRef");
        refTypes.add("scheduleRef");
        refTypes.add("ccuRef");
        
    }
    //Only for Unit testing
    protected SyncStatusService() {
        preferences = null;
    }
    private SyncStatusService(Context appContext) {
        applicationContext = appContext;
        preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        initializeLists();
    }
    
    public static SyncStatusService getInstance(Context context) {
        if(instance == null) {
            instance = new SyncStatusService(context);
        }
        return instance;
    }
    
    private void initializeLists() {
        unsyncedIdList = getListString(PREFS_ID_LIST_UNSYNCED);
        updatedIdList = getListString(PREFS_ID_LIST_UPDATED);
        deletedIdList = getListString(PREFS_ID_LIST_DELETED);
        CcuLog.i(HayStackConstants.LOG_TAG," unsyncedIdList " + unsyncedIdList.size() + " updatedIdList " + updatedIdList.size()
                                    + " deletedIdList " + deletedIdList.size());
    }
    
    public void saveSyncStatus() {
        putListString(PREFS_ID_LIST_UNSYNCED, unsyncedIdList);
        putListString(PREFS_ID_LIST_UPDATED, updatedIdList);
        putListString(PREFS_ID_LIST_DELETED, deletedIdList);
    }

    public boolean isSyncNotRequired() {
        return unsyncedIdList.isEmpty() && updatedIdList.isEmpty() && deletedIdList.isEmpty();
    }
    
    public void addUnSyncedEntity(String id) {
        CcuLog.i(HayStackConstants.LOG_TAG," addUnSyncedEntity "+id);
        if(unsyncedIdList.contains(id)) {
            CcuLog.i(HayStackConstants.LOG_TAG," addUnSyncedEntity already present "+id);
            return;
        }
        unsyncedIdList.add(id);
        if (fileSaveDelayTimer == null) {
            scheduleSyncDataSaveTimer();
        }
    }
    
    public void addUpdatedEntity(String id) {
        CcuLog.i(HayStackConstants.LOG_TAG," addUpdatedEntity "+id);
        if(!(updatedIdList.contains(id))) {
            updatedIdList.add(id);
            //This is expensive but can avoid sync-data crash due to an app-crash or tablet reboot.
            putListString(PREFS_ID_LIST_UPDATED, updatedIdList);
        }
    }
    
    public void addDeletedEntity(String id, boolean saveImmediate) {
        CcuLog.i(HayStackConstants.LOG_TAG," addDeletedEntity "+id);
        if (hasEntitySynced(id)) {
            deletedIdList.add(id);
            if (saveImmediate) {
                putListString(PREFS_ID_LIST_DELETED, deletedIdList);
            }
        }
        if (updatedIdList.contains(id)) {
            updatedIdList.remove(id);
            if (saveImmediate) {
                putListString(PREFS_ID_LIST_UPDATED, updatedIdList);
            }
        }
        if (unsyncedIdList.contains(id)) {
            unsyncedIdList.remove(id);
            if (saveImmediate) {
                putListString(PREFS_ID_LIST_UNSYNCED, unsyncedIdList);
            }
        }
    }

    private void scheduleSyncDataSaveTimer() {
        fileSaveDelayTimer = new Timer();
        fileSaveDelayTimer.schedule(new TimerTask() {
            @Override
            public void run() {
            CcuLog.i("CCU_PROFILING", "Save sync data");
            putListString(PREFS_ID_LIST_UNSYNCED, unsyncedIdList);
                try {
                    if (fileSaveDelayTimer == null) {
                        return;
                    }
                    fileSaveDelayTimer.cancel();
                    fileSaveDelayTimer = null;
                } catch (IllegalStateException e) {
                    CcuLog.i(HayStackConstants.LOG_TAG, "Timer already cancelled", e);
                }
            }
        }, 15000);
    }
    public void clearSyncStatus() {
        CcuLog.i(HayStackConstants.LOG_TAG,"clearSyncStatus");
        if (unsyncedIdList.size() > 100) {
            preferences.edit().remove(PREFS_ID_LIST_UNSYNCED).apply();
        } else {
            unsyncedIdList.clear();
        }
        saveSyncStatus();
    }

    public void setDeletedEntitySynced(String id) {
        CcuLog.i(HayStackConstants.LOG_TAG,"setDeletedEntitySynced "+id);
        deletedIdList.remove(id);
        putListString(PREFS_ID_LIST_DELETED, deletedIdList);
        if (updatedIdList.contains(id)) {
            updatedIdList.remove(id);
            putListString(PREFS_ID_LIST_UPDATED, updatedIdList);
        }
        if (unsyncedIdList.contains(id)) {
            unsyncedIdList.remove(id);
            putListString(PREFS_ID_LIST_UNSYNCED, unsyncedIdList);
        }
    }
    
    public void setEntitySynced(String id) {
        CcuLog.i(HayStackConstants.LOG_TAG,"Set entity synced "+id);
        if (unsyncedIdList.contains(id)) {
            unsyncedIdList.remove(id);
            if (fileSaveDelayTimer == null) {
                scheduleSyncDataSaveTimer();
            }
        }
        
        if (updatedIdList.contains(id)) {
            updatedIdList.remove(id);
            putListString(PREFS_ID_LIST_UPDATED, updatedIdList);
        }
        
    }
    
    /**
     *  Check if entity was ever synced
     */
    public boolean hasEntitySynced(String id) {
        return !unsyncedIdList.contains(id);
    }
    
    /**
     *  Check if entity requires sync.
     *  Both unsynced and updated entities are eligible for sync
     */
    public boolean isEligibleForSync(String id) {
        return unsyncedIdList.contains(id) || updatedIdList.contains(id);
    }
    
    public boolean hasUnSyncedData() {
        return !unsyncedIdList.isEmpty();
    }
    
    public boolean hasUpdatedData() {
        return !updatedIdList.isEmpty();
    }
    
    public boolean hasDeletedData() {
        return !deletedIdList.isEmpty();
    }
    
    public HGridIterator getUnSyncedData() {

        //TODO- Entities currently have refs stored as strings which the backend does not allow.
        //Changing refs across app has a larger scope. So as part of ID Migration , just changing the HStr refs to
        //HRef before sending them. This is should bs removed to use above code once it is done.
        
        ArrayList<HDict> unsyncedDictList = new ArrayList<>();
        CcuLog.d("CCU_HS_SYNC", " Unsynced Data : " + unsyncedIdList.size());
        ListIterator<String> unSyncItr = unsyncedIdList.listIterator();
        while(unSyncItr.hasNext()) {
            String id = unSyncItr.next();
            HDict entity = CCUHsApi.getInstance().readHDictById(id);
            if (entity == null) {
                CcuLog.e("CCU_HS_SyncHandler","Invalid unsynced entity for sync "+id);
                //Entity might have been deleted.
                unSyncItr.remove();
                continue;
            }
            HDictBuilder builder = new HDictBuilder();
            builder.add(entity);
            updateRefs(entity, builder);
            updateLastModifiedDateTime(entity, builder);
            unsyncedDictList.add(builder.toDict());
        }
        HGrid unsyncedGridData = HGridBuilder.dictsToGrid(unsyncedDictList.toArray(new HDict[0]));

        return new HGridIterator(unsyncedGridData);
    }
    
    public HGridIterator getUpdatedData() {

        ArrayList<HDict> updatedDictList = new ArrayList<>();
        CcuLog.d("CCU_HS_Sync", " Updated Data : " + updatedIdList.size());
        ListIterator<String> updatedItr = new ArrayList<>(updatedIdList).listIterator();

        while(updatedItr.hasNext()) {
            String id = updatedItr.next();
            HDict entity = CCUHsApi.getInstance().readHDictById(id);
            if (entity == null || id.equals("null")) {
                CcuLog.e("CCU_HS_SyncHandler","Invalid updated entity for sync "+id);
                //Entity might have been deleted.
                updatedItr.remove();
                continue;
            }
            HDictBuilder builder = new HDictBuilder();
            CcuLog.d("CCU_HS_SYNC", "made new HDictBuilder()");
            builder.add(entity);
            CcuLog.d("CCU_HS_SYNC", "added entity");
            updateRefs(entity, builder);
            CcuLog.d("CCU_HS_SYNC", "updated refs");
            updateLastModifiedDateTime(entity, builder);
            updatedDictList.add(builder.toDict());
            CcuLog.d("CCU_HS_SYNC", "updated dict refs");
        }
        HGrid updatedGridData = HGridBuilder.dictsToGrid(updatedDictList.toArray(new HDict[0]));
        CcuLog.d("CCU_HS", "updated data : "+HZincWriter.gridToString(updatedGridData));
        return new HGridIterator(updatedGridData);
    }
    
    public List<String> getDeletedData() {
        CcuLog.d("CCU_HS_Sync", " Deleted Data : " + Arrays.toString(deletedIdList.toArray()));
        return deletedIdList;
    }
    
    private List<String> getListString(String key) {
        return Collections.synchronizedList(new ArrayList<>(
                        Arrays.asList(TextUtils.split(preferences.getString(key, ""), "‚‗‚"))));
    }
    
    private void putListString(String key, List<String> stringList) {
        long time = System.currentTimeMillis();
        String[] stringArr = stringList.toArray(new String[0]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", stringArr)).apply();
        CcuLog.i("CCU_PROFILING", "Time to save "+key+" "+(System.currentTimeMillis() - time));
    }
    
    public void updateRefs(HDict entity, HDictBuilder builder) {
        for (String hRef : refTypes) {
            if (entity.has(hRef) && !entity.get(hRef).toString().equals("SYSTEM")) {
                builder.add(hRef, HRef.copy(entity.get(hRef).toString()));
            }
        }
    }

    public void updateLastModifiedDateTime(HDict entity, HDictBuilder builder) {
        if (entity.has("lastModifiedDateTime")) {
            builder.add("lastModifiedDateTime", HDateTime.make(System.currentTimeMillis()));
        }
    }

    public void setDeletedEntitySynced(List<String> removedSyncIdList) {
        CcuLog.i(HayStackConstants.LOG_TAG,"setDeletedEntitySynced for ids:"+removedSyncIdList);
        deletedIdList.removeAll(removedSyncIdList);
        putListString(PREFS_ID_LIST_DELETED, deletedIdList);

        if(updatedIdList.removeAll(removedSyncIdList)) {
            putListString(PREFS_ID_LIST_UPDATED, updatedIdList);
        }
        if(unsyncedIdList.removeAll(removedSyncIdList)) {
            putListString(PREFS_ID_LIST_UNSYNCED, unsyncedIdList);
        }
    }
}
