package a75f.io.api.haystack.sync;

import android.util.Log;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
    
    public EntitySyncHandler() {

    }
    
    public synchronized void sync() {

        Log.i(TAG, "isSyncNeeded: " + isSyncNeeded());
        if (isSyncNeeded())
        {
            doSyncSite();
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
    
    private void doSyncSite() {
        CcuLog.i("CCU", "doSyncSite ->");
        HDict sDict =  CCUHsApi.getInstance().readHDict("site");
        HDictBuilder b = new HDictBuilder().add(sDict);
        String siteLUID = b.remove("id").toString();
        String siteGUID = CCUHsApi.getInstance().getGUID(siteLUID);
        if (siteGUID == null)
        {
            ArrayList<HDict> entities = new ArrayList<>();
            entities.add(b.toDict());
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i("CCU", "Response : "+response);
            if (response == null) {
                return;
            }
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            while (it.hasNext())
            {
                HRow row = (HRow) it.next();
                siteGUID = row.get("id").toString();
            }
        }
    
        if (siteGUID != null && siteGUID != "")
        {
            CCUHsApi.getInstance().putUIDMap(siteLUID, siteGUID);
            if (!doSyncFloors(siteLUID)) {
                //Abort Sync as equips and points need valid floorRef and zoneRef
                CcuLog.i("CCU", "Floor Sync failed : abort ");
                return;
            }
            if (!doSyncEquips(siteLUID)) {
                CcuLog.i("CCU", "Floor Sync failed : abort ");
                return;
            }
            doSyncDevices(siteLUID);
            doSyncSchedules(siteLUID);

        }
        
        CcuLog.i("CCU", "<- doSyncSite");
    }



    private boolean doSyncFloors(String siteLUID) {
        CcuLog.i("CCU", "doSyncFloors ->");
        ArrayList<HashMap> floors = CCUHsApi.getInstance().readAll("floor");
        ArrayList<String> floorLUIDList = new ArrayList();
        
        ArrayList<HDict> entities = new ArrayList<>();
        for (Map m: floors)
        {
            CcuLog.i("CCU", m.toString());
            String luid = m.remove("id").toString();
            if (CCUHsApi.getInstance().getGUID(luid) == null) {
                floorLUIDList.add(luid);
                m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                entities.add(HSUtil.mapToHDict(m));
            }
        }
    
        if (floorLUIDList.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i("CCU", "Response: \n" + response);
            if (response == null)
            {
                CcuLog.i("CCU", "Aborting Floor Sync");
                return false;
            }
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            int index = 0;
            while (it.hasNext())
            {
                HRow row = (HRow) it.next();
                String floorGUID = row.get("id").toString();
                if (floorGUID != "")
                {
                    CCUHsApi.getInstance().putUIDMap(floorLUIDList.get(index++), floorGUID);
                }
            }
        }
        return doSyncZones(siteLUID);
        
    }
    
    private boolean doSyncZones(String siteLUID) {
        CcuLog.i("CCU", "doSyncZones ->");
        ArrayList<HashMap> floors = CCUHsApi.getInstance().readAll("floor");
        ArrayList<String> zoneLUIDList = new ArrayList();
        ArrayList<HDict> entities = new ArrayList<>();
        for (Map f: floors)
        {
            ArrayList<HashMap> zones = CCUHsApi.getInstance().readAll("room and floorRef == \""+f.get("id")+"\"");
            for (Map m : zones)
            {
                CcuLog.i("CCU", m.toString());
                String luid = m.remove("id").toString();
                if (CCUHsApi.getInstance().getGUID(luid) == null)
                {
                    zoneLUIDList.add(luid);
                    m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                    m.put("floorRef", HRef.copy(CCUHsApi.getInstance().getGUID(m.get("floorRef").toString())));
                    entities.add(HSUtil.mapToHDict(m));
                }
            }
        }
    
        if (zoneLUIDList.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i("CCU", "Response: \n" + response);
            if (response == null)
            {
                CcuLog.i("CCU", "Aborting Zone Sync");
                return false;
            }
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            int index = 0;
            while (it.hasNext())
            {
                HRow row = (HRow) it.next();
                String zoneGUID = row.get("id").toString();
                if (zoneGUID != "")
                {
                    CCUHsApi.getInstance().putUIDMap(zoneLUIDList.get(index++), zoneGUID);
                }
            }
        }
        return true;
    }

    private boolean doSyncSchedules(String siteLUID) {
        ArrayList<HashMap> schedules = CCUHsApi.getInstance().readAll("schedule");

        ArrayList<String> scheduleLUIDList = new ArrayList<String>();
        ArrayList<HDict> entities = new ArrayList<>();
        for (Map m: schedules)
        {
            CcuLog.i("CCU", "Schedule sync: " + m);
            String luid = m.remove("id").toString();
            if (CCUHsApi.getInstance().getGUID(luid) == null) {
                scheduleLUIDList.add(luid);
                m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                entities.add(HSUtil.mapToHDict(m));
            }
        }

        if (scheduleLUIDList.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i("CCU", "Response: \n" + response);
            if (response == null)
            {
                CcuLog.i("CCU", "Aborting Floor Sync");
                return false;
            }
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            int index = 0;
            while (it.hasNext())
            {
                HRow row = (HRow) it.next();
                String floorGUID = row.get("id").toString();
                if (floorGUID != "")
                {
                    CCUHsApi.getInstance().putUIDMap(scheduleLUIDList.get(index++), floorGUID);
                }
            }
        }

        return true;
    }
    
    private boolean doSyncEquips(String siteLUID) {
        CcuLog.i("CCU", "doSyncEquips ->");
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and siteRef == \""+siteLUID+"\"");
        ArrayList<String> equipLUIDList = new ArrayList();
        ArrayList<HDict> entities = new ArrayList<>();
        for (Map m: equips)
        {
            CcuLog.i("CCU", m.toString());
            String luid = m.remove("id").toString();
            if (CCUHsApi.getInstance().getGUID(luid) == null) {
                equipLUIDList.add(luid);
                m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                if (m.get("floorRef") != null && !m.get("floorRef").toString().equals("SYSTEM"))
                {
                    m.put("floorRef", HRef.copy(CCUHsApi.getInstance().getGUID(m.get("floorRef").toString())));
                }
                if (m.get("zoneRef") != null && !m.get("zoneRef").toString().equals("SYSTEM") )
                {
                    m.put("zoneRef", HRef.copy(CCUHsApi.getInstance().getGUID(m.get("zoneRef").toString())));
                }
                entities.add(HSUtil.mapToHDict(m));
            }
        }
        if (equipLUIDList.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i("CCU", "Response: \n" + response);
            if (response == null)
            {
                return false;
            }
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            String equipGUID = "";
            int index = 0;
            while (it.hasNext())
            {
                HRow row = (HRow) it.next();
                equipGUID = row.get("id").toString();
                if (equipGUID != "")
                {
                    CCUHsApi.getInstance().putUIDMap(equipLUIDList.get(index++), equipGUID);
                }
            }
        }
        return doSyncPoints(siteLUID);
    }
    
    private boolean doSyncPoints(String siteLUID) {
        CcuLog.i("CCU", "doSyncPoints ->");
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and siteRef == \""+siteLUID+"\"");
        for (Map q: equips)
        {
            
            String equipLUID = q.remove("id").toString();
    
            ArrayList<HashMap> points = CCUHsApi.getInstance().readAll("point and equipRef == \"" + equipLUID + "\"");

            if (points.size() == 0) {
                continue;
            }
            ArrayList<String> pointLUIDList = new ArrayList();
            ArrayList<HDict> entities = new ArrayList<>();
            for (Map m : points)
            {
                //CcuLog.i("CCU", m);
                String luid = m.remove("id").toString();
                if (CCUHsApi.getInstance().getGUID(luid) == null)
                {
                    pointLUIDList.add(luid);
                    m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                    m.put("equipRef", HRef.copy(CCUHsApi.getInstance().getGUID(equipLUID)));
                    if (m.get("floorRef") != null && !m.get("floorRef").toString().equals("SYSTEM"))
                    {
                        m.put("floorRef", HRef.copy(CCUHsApi.getInstance().getGUID(m.get("floorRef").toString())));
                    }
                    if (m.get("zoneRef") != null && !m.get("zoneRef").toString().equals("SYSTEM"))
                    {
                        m.put("zoneRef", HRef.copy(CCUHsApi.getInstance().getGUID(m.get("zoneRef").toString())));
                    }
                    entities.add(HSUtil.mapToHDict(m));
                }
            }
            
            if (pointLUIDList.size() > 0)
            {
                HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
                String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
                CcuLog.i("CCU", "Response: \n" + response);
                if (response == null)
                {
                    return false;
                }
                HZincReader zReader = new HZincReader(response);
                Iterator it = zReader.readGrid().iterator();
                int index = 0;
                while (it.hasNext())
                {
                    HRow row = (HRow) it.next();
                    String guid = row.get("id").toString();
                    CCUHsApi.getInstance().putUIDMap(pointLUIDList.get(index++), guid);
                }
            }
        }
        return true;
    }
    
    private void doSyncDevices(String siteLUID) {
        CcuLog.i("CCU", "doSyncDevices ->");
        ArrayList<HashMap> devices = CCUHsApi.getInstance().readAll("device");
        ArrayList<String> deviceLUIDList = new ArrayList();
        ArrayList<HDict> entities = new ArrayList<>();
        for (Map m: devices)
        {
            CcuLog.i("CCU", m.toString());
            String luid = m.remove("id").toString();
            if (CCUHsApi.getInstance().getGUID(luid) == null) {
                deviceLUIDList.add(luid);
                m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                if (m.get("floorRef") != null && !m.get("floorRef").toString().equals("SYSTEM"))
                {
                    m.put("floorRef", HRef.copy(CCUHsApi.getInstance().getGUID(m.get("floorRef").toString())));
                }
                if (m.get("zoneRef") != null && !m.get("zoneRef").toString().equals("SYSTEM"))
                {
                    m.put("zoneRef", HRef.copy(CCUHsApi.getInstance().getGUID(m.get("zoneRef").toString())));
                }
                entities.add(HSUtil.mapToHDict(m));
            }
        }
        
        if (deviceLUIDList.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i("CCU", "Response: \n" + response);
            if (response == null) {
                return;
            }
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            String equipGUID = "";
            int index = 0;
            
            while (it.hasNext())
            {
                HRow row = (HRow) it.next();
                equipGUID = row.get("id").toString();
                if (equipGUID != "")
                {
                    CCUHsApi.getInstance().putUIDMap(deviceLUIDList.get(index++), equipGUID);
                }
            }
            doSyncPhyPoints(siteLUID, deviceLUIDList);
        }
    }
    
    private void doSyncPhyPoints(String siteLUID, ArrayList<String> deviceLUIDList) {
        CcuLog.i("CCU", "doSyncPhyPoints ->");
        for (String deviceLUID : deviceLUIDList)
        {
            ArrayList<HashMap> points = CCUHsApi.getInstance().readAll("point and physical and deviceRef == \"" + deviceLUID + "\"");
            if (points.size() == 0) {
                continue;
            }
            ArrayList<String> pointLUIDList = new ArrayList();
            ArrayList<HDict> entities = new ArrayList<>();
            for (Map m : points)
            {
                //CcuLog.i("CCU", m);
                String luid = m.remove("id").toString();
                if (CCUHsApi.getInstance().getGUID(luid) == null
                         && CCUHsApi.getInstance().getGUID(deviceLUID) != null)
                {
                    pointLUIDList.add(luid);
                    m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                    m.put("deviceRef", HRef.copy(CCUHsApi.getInstance().getGUID(deviceLUID)));
                    
                    if (m.get("pointRef") != null)
                    {
                        m.put("pointRef", HRef.copy(CCUHsApi.getInstance().getGUID(m.get("pointRef").toString())));
                    }
                    if (m.get("floorRef") != null && !m.get("floorRef").toString().equals("SYSTEM"))
                    {
                        m.put("floorRef", HRef.copy(CCUHsApi.getInstance().getGUID(m.get("floorRef").toString())));
                    }
                    if (m.get("zoneRef") != null && !m.get("zoneRef").toString().equals("SYSTEM"))
                    {
                        m.put("zoneRef", HRef.copy(CCUHsApi.getInstance().getGUID(m.get("zoneRef").toString())));
                    }
                    entities.add(HSUtil.mapToHDict(m));
                }
            }
            if (pointLUIDList.size() > 0)
            {
                HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
                String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
                CcuLog.i("CCU", "Response: \n" + response);
                if (response == null)
                {
                    return;
                }
                HZincReader zReader = new HZincReader(response);
                Iterator it = zReader.readGrid().iterator();
                String guid;
                int index = 0;
                while (it.hasNext())
                {
                    HRow row = (HRow) it.next();
                    guid = row.get("id").toString();
                    CCUHsApi.getInstance().putUIDMap(pointLUIDList.get(index++), guid);
                }
            }
        }
    }
    
    public void doSyncRemoveIds()
    {
        CcuLog.i("CCU", "doSyncRemoveIds->");
        ArrayList<HDict> entities = new ArrayList<>();
        
        for (String removeId : CCUHsApi.getInstance().tagsDb.removeIdMap.values())
        {
            HDictBuilder b = new HDictBuilder();
            b.add("removeId", removeId.replace("@",""));
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
        ArrayList<HashMap> sites = CCUHsApi.getInstance().readAll("site");

        for (Map s: sites) {
            if (CCUHsApi.getInstance().getGUID(s.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :Site not synced :"+ s.get("id"));
                return true;
            }
        }
    
        ArrayList<HashMap> floors = CCUHsApi.getInstance().readAll("floor");
        for (Map f: floors) {
            if (CCUHsApi.getInstance().getGUID(f.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :Floor not synced :"+ f.get("id"));
                return true;
            }
        }
    
        ArrayList<HashMap> zones = CCUHsApi.getInstance().readAll("zone");
        for (Map z: zones) {
            if (CCUHsApi.getInstance().getGUID(z.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :Zone not synced :"+ z.get("id"));
                return true;
            }
        }
        
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");
        for (Map q: equips) {
            if (CCUHsApi.getInstance().getGUID(q.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :Equip not synced :"+ q.get("id"));
                return true;
            }
        }
        ArrayList<HashMap> devices = CCUHsApi.getInstance().readAll("device");
        for (Map d: devices) {
            if (CCUHsApi.getInstance().getGUID(d.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :device not synced :"+ d.get("id"));
                return true;
            }
        }
        ArrayList<HashMap> points = CCUHsApi.getInstance().readAll("point");
        for (Map p: points) {
            if (CCUHsApi.getInstance().getGUID(p.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :Point not synced :"+ p.get("id"));
                return true;
            }
        }
        return false;
    }
}
