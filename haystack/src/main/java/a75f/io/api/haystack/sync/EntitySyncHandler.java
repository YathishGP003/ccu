package a75f.io.api.haystack.sync;

import android.util.Log;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 10/15/18.
 */

public class EntitySyncHandler
{
    private static final String TAG = EntitySyncHandler.class.getSimpleName();
    //CCUHsApi hayStack;
    
    EntitySyncAdapter siteAdapter     = new SiteSyncAdapter();
    EntitySyncAdapter floorAdapter    = new FloorSyncAdapter();
    EntitySyncAdapter zoneAdapter     = new ZoneSyncAdapter();
    EntitySyncAdapter equipAdapter    = new EquipSyncAdapter();
    EntitySyncAdapter pointAdapter    = new PointSyncAdapter();
    EntitySyncAdapter deviceAdapter   = new DeviceSyncAdapter();
    EntitySyncAdapter rawPointAdapter = new RawPointSyncAdapter();
    EntitySyncAdapter scheduleAdapter = new ScheduleSyncAdapter();
    
    public boolean syncPending = false;
    
    public synchronized void sync() {

        Log.i(TAG, "syncPending: " + syncPending);
        if (syncPending || isSyncNeeded())
        {
            if (siteAdapter.onSync() && floorAdapter.onSync() && zoneAdapter.onSync()
                    && equipAdapter.onSync() && pointAdapter.onSync()
                    && deviceAdapter.onSync() && rawPointAdapter.onSync()
                    && scheduleAdapter.onSync())
                     {
                         syncPending = false;
            }
        }
        
        if (CCUHsApi.getInstance().tagsDb.removeIdMap.size() > 0) {
            CcuLog.i("CCU", "RemoveIDMap : "+CCUHsApi.getInstance().tagsDb.removeIdMap);
            doSyncRemoveIds();
        }
    
        if (CCUHsApi.getInstance().tagsDb.updateIdMap.size() > 0) {
            CcuLog.i("CCU", "UpdateIDMap : "+CCUHsApi.getInstance().tagsDb.updateIdMap);
            doSyncUpdateEntities();
        }
    
    }
    
    public void doSyncRemoveIds()
    {
        CcuLog.i("CCU", "doSyncRemoveIds->");
        ArrayList<HDict> entities = new ArrayList<>();
        
        for (String removeId : CCUHsApi.getInstance().tagsDb.removeIdMap.values())
        {
            HDictBuilder b = new HDictBuilder();
            b.add("id", HRef.make(removeId.replace("@","")));
            entities.add(b.toDict());
        }
        
        
        HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
        String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "removeEntity", HZincWriter.gridToString(grid));
        if (response != null)
        {
            CCUHsApi.getInstance().tagsDb.removeIdMap.clear();
        }
        CcuLog.i("CCU", "Response: \n" + response);
    }
    
    /**
     * Update request should be sent with GUID as part of the entity.
     */
    public void doSyncUpdateEntities() {
        CcuLog.i("CCU", "doSyncUpdateEntities->");
        ArrayList<HDict> entities = new ArrayList<>();
        for (String luid : CCUHsApi.getInstance().tagsDb.updateIdMap.keySet()) {
            if (CCUHsApi.getInstance().getGUID(luid) == null) {
                continue;
            }
            HashMap entity = CCUHsApi.getInstance().readMapById(luid);
            entity.put("id", HRef.copy(CCUHsApi.getInstance().getGUID(luid)));
            if (entity.get("siteRef") != null && CCUHsApi.getInstance().getGUID(entity.get("siteRef").toString()) != null)
            {
                entity.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(entity.get("siteRef").toString())));
            }
            if (entity.get("equipRef") != null && CCUHsApi.getInstance().getGUID(entity.get("equipRef").toString()) != null)
            {
                entity.put("equipRef", HRef.copy(CCUHsApi.getInstance().getGUID(entity.get("equipRef").toString())));
            }
            if (entity.get("deviceRef") != null && CCUHsApi.getInstance().getGUID(entity.get("deviceRef").toString()) != null)
            {
                entity.put("deviceRef", HRef.copy(CCUHsApi.getInstance().getGUID(entity.get("deviceRef").toString())));
            }
            if (entity.get("pointRef") != null && CCUHsApi.getInstance().getGUID(entity.get("pointRef").toString()) != null)
            {
                entity.put("pointRef", HRef.copy(CCUHsApi.getInstance().getGUID(entity.get("pointRef").toString())));
            }
            if (entity.get("floorRef") != null && CCUHsApi.getInstance().getGUID(entity.get("floorRef").toString()) != null)
            {
                entity.put("floorRef", HRef.copy(CCUHsApi.getInstance().getGUID(entity.get("floorRef").toString())));
            }
            if (entity.get("zoneRef") != null && CCUHsApi.getInstance().getGUID(entity.get("zoneRef").toString()) != null)
            {
                entity.put("zoneRef", HRef.copy(CCUHsApi.getInstance().getGUID(entity.get("zoneRef").toString())));
            }
            if (entity.get("ahuRef") != null && CCUHsApi.getInstance().getGUID(entity.get("ahuRef").toString()) != null)
            {
                entity.put("ahuRef", HRef.copy(CCUHsApi.getInstance().getGUID(entity.get("ahuRef").toString())));
            }
            if (entity.get("scheduleRef") != null && CCUHsApi.getInstance().getGUID(entity.get("scheduleRef").toString()) != null)
            {
                entity.put("scheduleRef", HRef.copy(CCUHsApi.getInstance().getGUID(entity.get("scheduleRef").toString())));
            }
            entities.add(HSUtil.mapToHDict(entity));
        }
    
        if (entities.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i("CCU", "Response: \n" + response);
            if (response != null)
            {
                CcuLog.i("CCU", "Updated Entities: "+CCUHsApi.getInstance().tagsDb.updateIdMap);
                CCUHsApi.getInstance().tagsDb.updateIdMap.clear();
            }
        }
    }
    
    public boolean isSyncNeeded() {
        if (syncPending) {
            return syncPending;
        }
        ArrayList<HashMap> sites = CCUHsApi.getInstance().readAll("site");

        for (Map s: sites) {
            if (CCUHsApi.getInstance().getGUID(s.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :Site not synced :"+ s.get("id"));
                syncPending = true;
                return true;
            }
        }
    
        ArrayList<HashMap> floors = CCUHsApi.getInstance().readAll("floor");
        for (Map f: floors) {
            if (CCUHsApi.getInstance().getGUID(f.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :Floor not synced :"+ f.get("id"));
                syncPending = true;
                return true;
            }
        }
    
        ArrayList<HashMap> zones = CCUHsApi.getInstance().readAll("zone");
        for (Map z: zones) {
            if (CCUHsApi.getInstance().getGUID(z.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :Zone not synced :"+ z.get("id"));
                syncPending = true;
                return true;
            }
        }
        
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");
        for (Map q: equips) {
            if (CCUHsApi.getInstance().getGUID(q.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :Equip not synced :"+ q.get("id"));
                syncPending = true;
                return true;
            }
        }
        ArrayList<HashMap> devices = CCUHsApi.getInstance().readAll("device");
        for (Map d: devices) {
            if (CCUHsApi.getInstance().getGUID(d.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :device not synced :"+ d.get("id"));
                syncPending = true;
                return true;
            }
        }
        ArrayList<HashMap> points = CCUHsApi.getInstance().readAll("point");
        for (Map p: points) {
            if (CCUHsApi.getInstance().getGUID(p.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :Point not synced :"+ p.get("id"));
                syncPending = true;
                return true;
            }
        }
        return false;
    }
}
