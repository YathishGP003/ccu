package a75f.io.api.haystack.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

public class SyncStatusService {
    
    private static final String PREFS_ID_LIST_UNSYNCED = "unsyncedIdList";
    private static final String PREFS_ID_LIST_UPDATED  = "updatedIdList";
    private static final String PREFS_ID_LIST_DELETED  = "deletedIdList";
    
    Context applicationContext;
    private SharedPreferences preferences;
    
    private ArrayList<String> unsyncedIdList = new ArrayList<>();
    private ArrayList<String> updatedIdList = new ArrayList<>();
    private ArrayList<String> deletedIdList = new ArrayList<>();
    
    private static SyncStatusService instance = null;
    
    public static HashSet<String> refTypes = new HashSet<>();
    
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
    }
    
    public void saveSyncStatus() {
        putListString(PREFS_ID_LIST_UNSYNCED, unsyncedIdList);
        putListString(PREFS_ID_LIST_UPDATED, updatedIdList);
        putListString(PREFS_ID_LIST_DELETED, deletedIdList);
    }
    
    public void clearSyncStatus() {
        unsyncedIdList.clear();
        updatedIdList.clear();
        deletedIdList.clear();
        saveSyncStatus();
    }
    
    public boolean isSyncNotRequired() {
        return unsyncedIdList.size() == 0 && updatedIdList.size() == 0 && deletedIdList.size() == 0;
    }
    
    public void addUnSyncedEntity(String id) {
        unsyncedIdList.add(id);
    }
    
    public void addUpdatedEntity(String id) {
        updatedIdList.add(id);
    }
    
    public void addDeletedEntity(String id) {
        deletedIdList.add(id);
    }
    
    public void setUnSyncedEntitySynced(String id) {
        unsyncedIdList.remove(id);
    }
    
    public void setUpdatedEntitySynced(String id) {
        updatedIdList.remove(id);
    }
    
    public void setDeletedEntitySynced(String id) {
        deletedIdList.remove(id);
    }
    
    public void setEntitySynced(String id) {
        //TODO - Handle the case where an entity exists in both unsynced and updated lists
        if (unsyncedIdList.contains(id)) {
            unsyncedIdList.remove(id);
        } else if (updatedIdList.contains(id)) {
            updatedIdList.remove(id);
        } else if (deletedIdList.contains(id)) {
            deletedIdList.remove(id);
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
        return unsyncedIdList.size() > 0;
    }
    
    public boolean hasUpdatedData() {
        return updatedIdList.size() > 0;
    }
    
    public boolean hasDeletedData() {
        return deletedIdList.size() > 0;
    }
    
    public HGridIterator getUnSyncedData() {
    
        /*HGrid unsyncedGridData = CCUHsApi.getInstance()
                                            .hsClient
                                            .readByIds(getHRefArrayFromStringList(unsyncedIdList));*/
    
        //TODO- Entities currently have refs stored as strings which the backend does not allow.
        //Changing refs across app has a larger scope. So as part of ID Migration , just changing the HStr refs to
        //HRef before sending them. This is should bs removed to use above code once it is done.
        
        ArrayList<HDict> unsyncedDictList = new ArrayList<>();
        
        for (String id : unsyncedIdList) {
            HDict entity = CCUHsApi.getInstance().readHDictById(id);
            HDictBuilder builder = new HDictBuilder();
            builder.add(entity);
            updateRefs(entity, builder);
            unsyncedDictList.add(builder.toDict());
        }
        HGrid unsyncedGridData = HGridBuilder.dictsToGrid(unsyncedDictList.toArray(new HDict[0]));
        
        CcuLog.d("CCU_HS_Sync", " Unsynced Data :\n" + HZincWriter.gridToString(unsyncedGridData));
        return new HGridIterator(unsyncedGridData);
    }
    
    public HGridIterator getUpdatedData() {
        
        /*HGrid updatedGridData = CCUHsApi.getInstance()
                                        .hsClient
                                        .readByIds(getHRefArrayFromStringList(updatedIdList));*/
    
        ArrayList<HDict> updatedDictList = new ArrayList<>();
    
        for (String id : updatedIdList) {
            HDict entity = CCUHsApi.getInstance().readHDictById(id);
            HDictBuilder builder = new HDictBuilder();
            builder.add(entity);
            updateRefs(entity, builder);
            updatedDictList.add(builder.toDict());
        }
        HGrid updatedGridData = HGridBuilder.dictsToGrid(updatedDictList.toArray(new HDict[0]));
        CcuLog.d("CCU_HS_Sync", " Updated Data :\n" + HZincWriter.gridToString(updatedGridData));
        return new HGridIterator(updatedGridData);
    }
    
    private HRef[] getHRefArrayFromStringList(List<String> strList) {
        return strList.stream()
                      .map(HRef::copy)
                      .toArray(HRef[]::new);
    }
    
    public ArrayList<String> getDeletedData() {
        return deletedIdList;
    }
    
    private ArrayList<String> getListString(String key) {
        return new ArrayList<>(Arrays.asList(TextUtils.split(preferences.getString(key, ""), "‚‗‚")));
    }
    
    private void putListString(String key, ArrayList<String> stringList) {
        String[] stringArr = stringList.toArray(new String[0]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", stringArr)).apply();
    }
    
    public boolean updateRefs(HDict entity, HDictBuilder builder) {
        Iterator<String> iterator = refTypes.iterator();
        while(iterator.hasNext()) {
            String hRef = iterator.next();
            if (entity.has(hRef) && !entity.get(hRef).toString().equals("SYSTEM")) {
                builder.add(hRef, HRef.copy(entity.get(hRef).toString()));
            }
        }
        return true;
    }
}
