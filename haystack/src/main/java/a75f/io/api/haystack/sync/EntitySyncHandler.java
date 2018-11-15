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
    
    CCUHsApi hayStack;
    
    public EntitySyncHandler(CCUHsApi api) {
        hayStack = api;
    }
    
    public synchronized void sync() {
        
        if (isSyncNeeded())
        {
            doSyncSite();
        }
        
        if (hayStack.tagsDb.removeIdMap.size() > 0) {
            System.out.println("RemoveIDMap : "+hayStack.tagsDb.removeIdMap);
            doSyncRemoveIds();
        }
    
    }
    
    public synchronized void doSync() {
        doSyncSite();
    }
    
    private void doSyncSite() {
        System.out.println("doSyncSite ->");
        HDict sDict =  hayStack.readHDict("site");
        HDictBuilder b = new HDictBuilder().add(sDict);
        String siteLUID = b.remove("id").toString();
        String siteGUID = hayStack.getGUID(siteLUID);
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
            hayStack.putUIDMap(siteLUID, siteGUID);
            doSyncEquips(siteLUID);
            doSyncDevices(siteLUID);
        }
        
        System.out.println("<- doSyncSite");
    }
    
    private void doSyncEquips(String siteLUID) {
        
        ArrayList<HashMap> equips = hayStack.readAll("equip and siteRef == \""+siteLUID+"\"");
        ArrayList<String> equipLUIDList = new ArrayList();
        ArrayList<HDict> entities = new ArrayList<>();
        for (Map m: equips)
        {
            String luid = m.remove("id").toString();
            if (hayStack.getGUID(luid) == null) {
                equipLUIDList.add(luid);
                m.put("siteRef", HRef.copy(hayStack.getGUID(siteLUID)));
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
                    hayStack.putUIDMap(equipLUIDList.get(index++), equipGUID);
                }
            }
        }
        System.out.println("Synced Equips: "+hayStack.tagsDb.idMap);
        doSyncPoints(siteLUID, equipLUIDList);
        
        
    }
    
    private void doSyncPoints(String siteLUID, ArrayList<String> equipLUIDList) {
    
        ArrayList<HashMap> equips = hayStack.readAll("equip and siteRef == \""+siteLUID+"\"");
        for (Map q: equips)
        {
            String equipLUID = q.remove("id").toString();
    
            ArrayList<HashMap> points = hayStack.readAll("point and equipRef == \"" + equipLUID + "\"");
            if (points.size() == 0) {
                continue;
            }
            ArrayList<String> pointLUIDList = new ArrayList();
            ArrayList<HDict> entities = new ArrayList<>();
            for (Map m : points)
            {
                String luid = m.remove("id").toString();
                if (hayStack.getGUID(luid) == null)
                {
                    pointLUIDList.add(luid);
                    m.put("siteRef", HRef.copy(hayStack.getGUID(siteLUID)));
                    m.put("equipRef", HRef.copy(hayStack.getGUID(equipLUID)));
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
                    hayStack.putUIDMap(pointLUIDList.get(index++), guid);
                }
                System.out.println("Synced Points: "+hayStack.tagsDb.idMap);
            }
            
        }
    }
    
    private void doSyncDevices(String siteLUID) {
        //ArrayList<HashMap> devices = hayStack.readAll("device and siteRef == \""+siteLUID+"\"");
        ArrayList<HashMap> devices = hayStack.readAll("device");
        ArrayList<String> deviceLUIDList = new ArrayList();
        ArrayList<HDict> entities = new ArrayList<>();
        for (Map m: devices)
        {
            String luid = m.remove("id").toString();
            if (hayStack.getGUID(luid) == null) {
                deviceLUIDList.add(luid);
                m.put("siteRef", HRef.copy(hayStack.getGUID(siteLUID)));
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
                    hayStack.putUIDMap(deviceLUIDList.get(index++), equipGUID);
                }
            }
            System.out.println("Synced devices: " + hayStack.tagsDb.idMap);
            doSyncPhyPoints(siteLUID, deviceLUIDList);
        }
    }
    
    private void doSyncPhyPoints(String siteLUID, ArrayList<String> deviceLUIDList) {
        
        for (String deviceLUID : deviceLUIDList)
        {
            ArrayList<HashMap> points = hayStack.readAll("point and physical and deviceRef == \"" + deviceLUID + "\"");
            if (points.size() == 0) {
                continue;
            }
            ArrayList<String> pointLUIDList = new ArrayList();
            ArrayList<HDict> entities = new ArrayList<>();
            for (Map m : points)
            {
                String luid = m.remove("id").toString();
                if (hayStack.getGUID(luid) == null
                         && hayStack.getGUID(deviceLUID) != null)
                {
                    pointLUIDList.add(luid);
                    m.put("siteRef", HRef.copy(hayStack.getGUID(siteLUID)));
                    m.put("deviceRef", HRef.copy(hayStack.getGUID(deviceLUID)));
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
                    hayStack.putUIDMap(pointLUIDList.get(index++), guid);
                }
            }
            System.out.println("Synced Phy Points: "+hayStack.tagsDb.idMap);
        }
    }
    
    public void doSyncRemoveIds()
    {
        ArrayList<HDict> entities = new ArrayList<>();
        
        for (String removeId : hayStack.tagsDb.removeIdMap.values())
        {
            HDictBuilder b = new HDictBuilder();
            b.add("removeId", removeId.replace("@",""));
            entities.add(b.toDict());
        }
        
        
        HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
        String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "removeEntity", HZincWriter.gridToString(grid));
        if (response != null)
        {
            hayStack.tagsDb.removeIdMap.clear();
        }
        System.out.println("Response: \n" + response);
    }
    
    public boolean isSyncNeeded() {
        ArrayList<HashMap> sites = hayStack.readAll("site");
        for (Map s: sites) {
            if (hayStack.getGUID(s.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :Site not synced :"+ s.get("id"));
                return true;
            }
        }
        ArrayList<HashMap> equips = hayStack.readAll("equip");
        for (Map q: equips) {
            if (hayStack.getGUID(q.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :Euip not synced :"+ q.get("id"));
                return true;
            }
        }
        ArrayList<HashMap> devices = hayStack.readAll("device");
        for (Map d: devices) {
            if (hayStack.getGUID(d.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :device not synced :"+ d.get("id"));
                return true;
            }
        }
        ArrayList<HashMap> points = hayStack.readAll("point");
        for (Map p: points) {
            if (hayStack.getGUID(p.get("id").toString()) == null) {
                Log.d("CCU","Entity sync required :Point not synced :"+ p.get("id"));
                return true;
            }
        }
        return false;
    }
}
