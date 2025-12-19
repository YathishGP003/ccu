package a75f.io.api.haystack.sync;

import static a75f.io.api.haystack.CCUTagsDb.TAG_CCU_HS;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.logger.CcuLog;

public class SyncStatusService {
    
    private static final String PREFS_ID_LIST_UNSYNCED = "unsyncedIdList";
    private static final String PREFS_ID_MAP_UPDATED = "updatedIdMap";
    private static final String PREFS_ID_LIST_DELETED  = "deletedIdList";
    
    Context applicationContext;
    private final SharedPreferences preferences;
    
    private List<String> unsyncedIdList;
    private Map<String, Long> updatedIdMap;
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
        updatedIdMap = getTimedMap(PREFS_ID_MAP_UPDATED);
        deletedIdList = getListString(PREFS_ID_LIST_DELETED);
        CcuLog.i(HayStackConstants.LOG_TAG," unsyncedIdList " + unsyncedIdList.size() + " updatedIdList " + updatedIdMap.size()
                                    + " deletedIdList " + deletedIdList.size());
    }
    
    public void saveSyncStatus() {
        putListString(PREFS_ID_LIST_UNSYNCED, unsyncedIdList);
        putTimedMap(PREFS_ID_MAP_UPDATED, updatedIdMap);
        putListString(PREFS_ID_LIST_DELETED, deletedIdList);
    }

    public boolean isSyncNotRequired() {
        return unsyncedIdList.isEmpty() && updatedIdMap.isEmpty() && deletedIdList.isEmpty();
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
        updatedIdMap.put(id, System.currentTimeMillis());
        putTimedMap(PREFS_ID_MAP_UPDATED, updatedIdMap);
    }
    
    public void addDeletedEntity(String id, boolean saveImmediate) {
        CcuLog.i(HayStackConstants.LOG_TAG," addDeletedEntity "+id);
        if (hasEntitySynced(id)) {
            deletedIdList.add(id);
            if (saveImmediate) {
                putListString(PREFS_ID_LIST_DELETED, deletedIdList);
            }
        }
        if (updatedIdMap.containsKey(id)) {
            updatedIdMap.remove(id);
            if (saveImmediate) {
                putTimedMap(PREFS_ID_MAP_UPDATED, updatedIdMap);
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
            CcuLog.i(HayStackConstants.LOG_TAG,"More than 100 unsynced entities, clearing all unsynced data");
            if(preferences.edit().remove(PREFS_ID_LIST_UNSYNCED).commit()) {
                CcuLog.i(HayStackConstants.LOG_TAG," successfully Cleared unsynced data");
            } else {
                CcuLog.e(HayStackConstants.LOG_TAG,"Failed to clear unsynced data");
            }
            unsyncedIdList = new ArrayList<>();
        } else {
            unsyncedIdList.clear();
        }
        saveSyncStatus();
    }

    public void setDeletedEntitySynced(String id) {
        CcuLog.i(HayStackConstants.LOG_TAG,"setDeletedEntitySynced "+id);
        deletedIdList.remove(id);
        putListString(PREFS_ID_LIST_DELETED, deletedIdList);
        if (updatedIdMap.containsKey(id)) {
            updatedIdMap.remove(id);
            putTimedMap(PREFS_ID_MAP_UPDATED, updatedIdMap);
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
        if (updatedIdMap.containsKey(id)) {
            updatedIdMap.remove(id);
            putTimedMap(PREFS_ID_MAP_UPDATED, updatedIdMap);
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
        return unsyncedIdList.contains(id) || updatedIdMap.containsKey(id);
    }
    
    public boolean hasUnSyncedData() {
        return !unsyncedIdList.isEmpty();
    }
    
    public boolean hasUpdatedData() {
        return !updatedIdMap.isEmpty();
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
        synchronized (updatedIdMap) {
            CcuLog.d("CCU_HS_Sync", " Updated Data : " + updatedIdMap.size());
            ListIterator<String> updatedItr = new ArrayList<>(updatedIdMap.keySet()).listIterator();

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
    
    private Map<String, Long> getTimedMap(String key) {
        String jsonString = preferences.getString(key, null);
        if(jsonString == null) {
            CcuLog.d(TAG_CCU_HS, "No timed map found in preferences for key "+key);
            return Collections.synchronizedMap(new LinkedHashMap<>());
        }

        Map<String, Long> map = new LinkedHashMap<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String id = jsonObject.keys().next();
                map.put(id, jsonObject.getLong(id));
            }
        } catch (JSONException e) {
            CcuLog.e(TAG_CCU_HS, "Error parsing timed map from preferences for key "+key, e);
            e.printStackTrace();
        }
        return Collections.synchronizedMap(map);
    }
    
    private void putListString(String key, List<String> stringList) {
        long time = System.currentTimeMillis();
        String[] stringArr = stringList.toArray(new String[0]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", stringArr)).apply();
        CcuLog.i("CCU_PROFILING", "Time to save "+key+" "+(System.currentTimeMillis() - time));
    }

    private void putTimedMap(String key, Map<String, Long> timedMap) {
        synchronized (timedMap) {
            long time = System.currentTimeMillis();
            try {
                JSONArray jsonArray = new JSONArray();
                for (Map.Entry<String, Long> entry : timedMap.entrySet()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(entry.getKey(), entry.getValue());
                    jsonArray.put(jsonObject);
                }
                preferences.edit().putString(key, jsonArray.toString()).apply();
                CcuLog.i("CCU_PROFILING", "Time to save timed map "+key+" "+(System.currentTimeMillis() - time));
            } catch (JSONException e) {
                CcuLog.e(TAG_CCU_HS, "Error saving timed map to preferences for key "+key, e);
                e.printStackTrace();
            }
        }
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

        boolean isUpdatedIdMapChanged = false;
        for(String removedId: removedSyncIdList) {
            isUpdatedIdMapChanged |= updatedIdMap.remove(removedId) != null;
        }
        if(isUpdatedIdMapChanged) {
            putTimedMap(PREFS_ID_MAP_UPDATED, updatedIdMap);
        }

        if(unsyncedIdList.removeAll(removedSyncIdList)) {
            putListString(PREFS_ID_LIST_UNSYNCED, unsyncedIdList);
        }
    }

    public long getLastUpdatedTime(String id) {
        return updatedIdMap.getOrDefault(id, 0l);
    }

    // This method is to be used only once while deleting old updatedIdList and migrating to updatedIdMap
    public void addUpdatedIdListToUpdatedTimedMap(Map<String, Long> idsToUpdateMap) {
        LinkedHashMap<String, Long> mergedMap = new LinkedHashMap<>(idsToUpdateMap);
        mergedMap.putAll(updatedIdMap);

        updatedIdMap.clear();
        updatedIdMap.putAll(mergedMap);
        CcuLog.d(TAG_CCU_HS, "addUpdatedIdListToUpdatedTimedMap: updatedIdMap size after merge "+updatedIdMap.size());
        putTimedMap(PREFS_ID_MAP_UPDATED, updatedIdMap);
    }
}
