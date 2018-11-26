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
            System.out.println("RemoveIDMap : "+CCUHsApi.getInstance().tagsDb.removeIdMap);
            doSyncRemoveIds();
        }
    
        if (CCUHsApi.getInstance().tagsDb.updateIdMap.size() > 0) {
            System.out.println("UpdateIDMap : "+CCUHsApi.getInstance().tagsDb.updateIdMap);
            doSyncUpdateEntities();
        }
    
    }
    
    public synchronized void doSync() {
        doSyncSite();
    }
    
    private void doSyncSite() {
        System.out.println("doSyncSite ->");
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
            System.out.println("Response: \n" + response);
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
            doSyncEquips(siteLUID);
            doSyncDevices(siteLUID);
        }
        
        System.out.println("<- doSyncSite");
    }
    
    private void doSyncEquips(String siteLUID) {
        
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and siteRef == \""+siteLUID+"\"");
        ArrayList<String> equipLUIDList = new ArrayList();
        ArrayList<HDict> entities = new ArrayList<>();
        for (Map m: equips)
        {
            String luid = m.remove("id").toString();
            if (CCUHsApi.getInstance().getGUID(luid) == null) {
                equipLUIDList.add(luid);
                m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                entities.add(HSUtil.mapToHDict(m));
            }
        }
        if (equipLUIDList.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            System.out.println("Response: \n" + response);
            if (response == null)
            {
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
                    CCUHsApi.getInstance().putUIDMap(equipLUIDList.get(index++), equipGUID);
                }
            }
        }
        System.out.println("Synced Equips: "+CCUHsApi.getInstance().tagsDb.idMap);
        doSyncPoints(siteLUID, equipLUIDList);
        
        
    }
    
    private void doSyncPoints(String siteLUID, ArrayList<String> equipLUIDList) {
    
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
                String luid = m.remove("id").toString();
                if (CCUHsApi.getInstance().getGUID(luid) == null)
                {
                    pointLUIDList.add(luid);
                    m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                    m.put("equipRef", HRef.copy(CCUHsApi.getInstance().getGUID(equipLUID)));
                    entities.add(HSUtil.mapToHDict(m));
                }
                System.out.println(m);
            }
            
            if (pointLUIDList.size() > 0)
            {
                HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
                String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
                System.out.println("Response: \n" + response);
                if (response == null)
                {
                    return;
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
                System.out.println("Synced Points: "+CCUHsApi.getInstance().tagsDb.idMap);
            }
        }
    }
    
    private void doSyncDevices(String siteLUID) {
        //ArrayList<HashMap> devices = hayStack.readAll("device and siteRef == \""+siteLUID+"\"");
        ArrayList<HashMap> devices = CCUHsApi.getInstance().readAll("device");
        ArrayList<String> deviceLUIDList = new ArrayList();
        ArrayList<HDict> entities = new ArrayList<>();
        for (Map m: devices)
        {
            String luid = m.remove("id").toString();
            if (CCUHsApi.getInstance().getGUID(luid) == null) {
                deviceLUIDList.add(luid);
                m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                entities.add(HSUtil.mapToHDict(m));
            }
        }
        
        if (deviceLUIDList.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            System.out.println("Response: \n" + response);
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
            System.out.println("Synced devices: " + CCUHsApi.getInstance().tagsDb.idMap);
            doSyncPhyPoints(siteLUID, deviceLUIDList);
        }
    }
    
    private void doSyncPhyPoints(String siteLUID, ArrayList<String> deviceLUIDList) {
        
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
                String luid = m.remove("id").toString();
                if (CCUHsApi.getInstance().getGUID(luid) == null
                         && CCUHsApi.getInstance().getGUID(deviceLUID) != null)
                {
                    pointLUIDList.add(luid);
                    m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                    m.put("deviceRef", HRef.copy(CCUHsApi.getInstance().getGUID(deviceLUID)));
                    entities.add(HSUtil.mapToHDict(m));
                }
                System.out.println(m);
            }
            if (pointLUIDList.size() > 0)
            {
                HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
                String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
                System.out.println("Response: \n" + response);
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
            System.out.println("Synced Phy Points: "+CCUHsApi.getInstance().tagsDb.idMap);
        }
    }
    
    public void doSyncRemoveIds()
    {
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
        System.out.println("Response: \n" + response);
    }
    
    public void doSyncUpdateEntities() {
        ArrayList<HDict> entities = new ArrayList<>();
        for (String luid : CCUHsApi.getInstance().tagsDb.updateIdMap.keySet()) {
            HashMap entity = CCUHsApi.getInstance().readMapById(luid);
            entity.put("id", HRef.copy(CCUHsApi.getInstance().getGUID(luid)));
            entities.add(HSUtil.mapToHDict(entity));
        }
    
        if (entities.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            System.out.println("Response: \n" + response);
            if (response != null)
            {
                System.out.println("Updated Entities: "+CCUHsApi.getInstance().tagsDb.updateIdMap);
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
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");
        for (Map q: equips) {
            if (CCUHsApi.getInstance().getGUID(q.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :Euip not synced :"+ q.get("id"));
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
