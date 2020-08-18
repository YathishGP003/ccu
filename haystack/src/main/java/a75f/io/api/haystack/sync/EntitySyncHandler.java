package a75f.io.api.haystack.sync;

import android.util.Log;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.HStr;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 10/15/18.
 */

public class EntitySyncHandler
{
    private static final String TAG = "CCU_HS_SYNC";
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
    public boolean syncProgress = false;//TODO- Revisit
    
    Timer mSyncTimer = new Timer();
    TimerTask mSyncTimerTask = null;
    
    
    public void sync() {

        syncProgress = true;
        Log.i(TAG, "syncPending: " + syncPending);
        if (mSyncTimerTask != null) {
            mSyncTimerTask.cancel();
            mSyncTimerTask = null;
        }
        
        if (syncPending || isSyncNeeded())
        {
            if (siteAdapter.onSync() && floorAdapter.onSync() && zoneAdapter.onSync()
                    && equipAdapter.onSync() && pointAdapter.onSync()
                    && deviceAdapter.onSync() && rawPointAdapter.onSync()
                    && scheduleAdapter.onSync())
                     {
                         CcuLog.i(TAG, "Entity Sync Completed");
                         syncPending = false;
            } else {
                CcuLog.i(TAG, "Entity Sync Failed , Schedule Retry");
                scheduleSync();
            }
        }
        
        if (CCUHsApi.getInstance().tagsDb.removeIdMap.size() > 0) {
            CcuLog.i(TAG, "RemoveIDMap : "+CCUHsApi.getInstance().tagsDb.removeIdMap);
            doSyncRemoveIds();
        }
        
        if (CCUHsApi.getInstance().tagsDb.updateIdMap.size() > 0) {
            CcuLog.i(TAG, "UpdateIDMap : "+CCUHsApi.getInstance().tagsDb.updateIdMap);
            doSyncUpdateEntities();
        }
        syncProgress = false;
    }

    public void syncPointEntity(){
        pointAdapter.onSync();

        if (CCUHsApi.getInstance().tagsDb.removeIdMap.size() > 0) {
            CcuLog.i(TAG, "RemoveIDMap : "+CCUHsApi.getInstance().tagsDb.removeIdMap);
            doSyncRemoveIds();
        }
        if (CCUHsApi.getInstance().tagsDb.updateIdMap.size() > 0) {
            CcuLog.i(TAG, "UpdateIDMap : "+CCUHsApi.getInstance().tagsDb.updateIdMap);
            doSyncUpdateEntities();
        }
    }
    public void doSyncWithWrite(){
        sync();
    
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");
        for (Map equip: equips)
        {
            if (CCUHsApi.getInstance().getGUID(equip.get("id").toString()) == null) {
                continue;
            }
            ArrayList<HDict> equipDict = new ArrayList<>();
            ArrayList<HashMap> points = CCUHsApi.getInstance().readAll("point and writable and equipRef == \"" + equip.get("id") + "\"");
            for (Map point : points)
            {
                String luid = point.get("id").toString();
                String guid = CCUHsApi.getInstance().getGUID(luid);
                if (guid != null)
                {
                    ArrayList<HDict> dictList = getWriteArrDict(luid, guid);
                    if (dictList.size() > 0)
                    {
                        equipDict.addAll(dictList);
                    } else {
                        CcuLog.d("CCU_HS"," Writable point not initialized "+point.get("dis"));
                    }
                }
            }
            if (equipDict.size() > 0)
            {
                if (!CCUHsApi.getInstance().isCCURegistered()){
                    return;
                }
                HDict nosyncMeta = new HDictBuilder().add("nosync").toDict();
                String r = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "pointWriteMany",
                                        HZincWriter.gridToString(HGridBuilder.dictsToGrid(nosyncMeta,equipDict.toArray(new HDict[equipDict.size()]))));
                CcuLog.d("CCU_HS", "Response: \n" + r);
            }
        }
    }
    
    private ArrayList<HDict> getWriteArrDict(String luid, String guid) {
        ArrayList<HashMap> pointArr = CCUHsApi.getInstance().readPoint(luid);
        ArrayList<HDict> dictArr = new ArrayList<>();
        for (HashMap valMap : pointArr) {
            if (valMap.get("val") != null)
            {
                boolean isDouble = false;
                Double val = 0.0;
                try
                {
                    val = Double.parseDouble(valMap.get("val").toString());
                    isDouble = true;
                }
                catch (NumberFormatException e)
                {
                    CcuLog.d("CCU_HS", "Writable Val is not Double " + valMap.get("val").toString());
                }
    
                HDictBuilder b = new HDictBuilder().add("id", HRef.copy(guid))
                                                     .add("level", (int) Double.parseDouble(valMap.get("level").toString()))
                                                     .add("who", valMap.get("who").toString())
                                                     .add("val", isDouble? HNum.make(val) : HStr.make(valMap.get("val").toString()));
                dictArr.add(b.toDict());
            }
        }
        return dictArr;
    }
    
    public boolean isSyncProgress() {
        return syncProgress;
    }
    
    public void doSyncRemoveIds()
    {
        CcuLog.i(TAG, "doSyncRemoveIds->");

        if (!CCUHsApi.getInstance().isCCURegistered()){
            return;
        }
        ArrayList<HDict> entities = new ArrayList<>();
        
        for (String removeId : CCUHsApi.getInstance().tagsDb.removeIdMap.values())
        {
            HDictBuilder b = new HDictBuilder();
            b.add("id", HRef.make(removeId.replace("@","")));
            entities.add(b.toDict());
        }

        int partitionSize = 20;
        ArrayList<List<HDict>> partitions = new ArrayList<>();
        for (int i = 0; i<entities.size(); i += partitionSize) {
            partitions.add(entities.subList(i, Math.min(i + partitionSize, entities.size())));
        }

        for (List<HDict> subEntities : partitions) {
            HGrid grid = HGridBuilder.dictsToGrid(subEntities.toArray(new HDict[subEntities.size()]));
            String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "removeEntity", HZincWriter.gridToString(grid));
            if (response != null)
            {
                HZincReader zReader = new HZincReader(response);
                Iterator it = zReader.readGrid().iterator();
                while (it.hasNext()) {
                    HRow row = (HRow) it.next();
                    String id = row.get("id").toString();
                    CCUHsApi.getInstance().tagsDb.removeIdMap.values().remove(id);
                }
            }
            CcuLog.i(TAG, "Response: \n" + response);
        }
    }

    public static HashSet<String> ref = new HashSet<>();

    static
    {
        ref.add("siteRef");
        ref.add("equipRef");
        ref.add("deviceRef");
        ref.add("pointRef");
        ref.add("floorRef");
        ref.add("roomRef");
        ref.add("ahuRef");
        ref.add("gatewayRef");
        ref.add("scheduleRef");
        
    }

    /**
     * Update request should be sent with GUID as part of the entity.
     */
    public void doSyncUpdateEntities() {
        CcuLog.i(TAG, "doSyncUpdateEntities->");
        if (!CCUHsApi.getInstance().isCCURegistered()){
            return ;
        }
        ArrayList<HDict> entities = new ArrayList<>();
        for (String luid : CCUHsApi.getInstance().tagsDb.updateIdMap.keySet()) {
            if (CCUHsApi.getInstance().getGUID(luid) == null) {
                continue;
            }

            HDict entity = CCUHsApi.getInstance().readHDictById(luid);
            HDictBuilder builder = new HDictBuilder();
            builder.add(entity);
            builder.add("id", HRef.copy(CCUHsApi.getInstance().getGUID(luid)));
            if (!updateRefs(entity, builder)) {
                scheduleSync();
                continue;
            }
            
            entities.add(builder.toDict());
        }

        updateEntities(entities);

    }

    public boolean updateRefs(HDict entity, HDictBuilder builder) {
        Iterator<String> iterator = ref.iterator();
        while(iterator.hasNext())
        {
            String hRef = iterator.next();
            if (entity.has(hRef) && !entity.get(hRef).toString().equals("SYSTEM"))
            {
                String guid = CCUHsApi.getInstance().getGUID(entity.get(hRef) instanceof HRef ? ((HRef) entity.get(hRef)).toCode()
                        : entity.getStr(hRef));
                if (guid == null) {
                    Log.d(TAG,"Entity not synced for "+hRef +" : "+entity.toZinc());
                    return false;
                }
                builder.add(hRef, HRef.copy(guid));
            }
        }
        return true;
    }

    private void updateEntities(ArrayList<HDict> hDIcts)
    {
        if (hDIcts != null && hDIcts.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(hDIcts.toArray(new HDict[hDIcts.size()]));
            String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i(TAG, "Response: \n" + response);
            if (response != null)
            {
                CcuLog.i(TAG, "Updated Entities: "+CCUHsApi.getInstance().tagsDb.updateIdMap);
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
                CcuLog.d(TAG,"Entity sync required :Site not synced :"+ s.get("id"));
                syncPending = true;
                return true;
            }
        }
    
        ArrayList<HashMap> floors = CCUHsApi.getInstance().readAll("floor");
        for (Map f: floors) {
            if (CCUHsApi.getInstance().getGUID(f.get("id").toString()) == null) {
                CcuLog.d(TAG,"Entity sync required :Floor not synced :"+ f.get("id"));
                syncPending = true;
                return true;
            }
        }
    
        ArrayList<HashMap> zones = CCUHsApi.getInstance().readAll("zone");
        for (Map z: zones) {
            if (CCUHsApi.getInstance().getGUID(z.get("id").toString()) == null) {
                CcuLog.d(TAG,"Entity sync required :Zone not synced :"+ z.get("id"));
                syncPending = true;
                return true;
            }
        }
        
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");
        for (Map q: equips) {
            if (CCUHsApi.getInstance().getGUID(q.get("id").toString()) == null) {
                CcuLog.d(TAG,"Entity sync required :Equip not synced :"+ q.get("id"));
                syncPending = true;
                return true;
            }
        }
        ArrayList<HashMap> devices = CCUHsApi.getInstance().readAll("device");
        for (Map d: devices) {
            if (CCUHsApi.getInstance().getGUID(d.get("id").toString()) == null) {
                CcuLog.d(TAG,"Entity sync required :device not synced :"+ d.get("id"));
                syncPending = true;
                return true;
            }
        }
        ArrayList<HashMap> points = CCUHsApi.getInstance().readAll("point");
        for (Map p: points) {
            if (CCUHsApi.getInstance().getGUID(p.get("id").toString()) == null) {
                CcuLog.d(TAG,"Entity sync required :Point not synced :"+ p.get("id"));
                syncPending = true;
                return true;
            }
        }
        ArrayList<HashMap> schedules = CCUHsApi.getInstance().readAll("schedule");
        for (Map s: schedules) {
            if (CCUHsApi.getInstance().getGUID(s.get("id").toString()) == null) {
                CcuLog.d(TAG,"Entity sync required :Schedule not synced :"+ s.get("id"));
                syncPending = true;
                return true;
            }
        }
        return false;
    }
    
    //retry sync after 30 sec
    public void scheduleSync() {
        if (mSyncTimerTask != null) {
            return;
        }
        mSyncTimerTask  = new TimerTask() {
            public void run() {
                CcuLog.i(TAG, "Entity Sync Scheduled");
                if (!syncProgress)
                {
                    mSyncTimerTask = null;
                    sync();
                }
            }};
        mSyncTimer.schedule(mSyncTimerTask, 30000);
    }
}
