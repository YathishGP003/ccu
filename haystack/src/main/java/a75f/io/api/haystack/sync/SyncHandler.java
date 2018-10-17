package a75f.io.api.haystack.sync;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
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

public class SyncHandler
{
    
    CCUHsApi hayStack;
    
    public SyncHandler(CCUHsApi api) {
        hayStack = api;
    }
    
    public synchronized void sync() {
        if (isSyncNeeded())
        {
            doSyncSite();
        }
    }
    
    public synchronized void doSync() {
        doSyncSite();
    }
    
    private void doSyncSite() {
        
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
        }
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
                m.put("siteRef", hayStack.getGUID(siteLUID));
                entities.add(HSUtil.mapToHDict(m));
            }
        }
    
        HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
        String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
    
        HZincReader zReader = new HZincReader(response);
        Iterator it = zReader.readGrid().iterator();
        String equipGUID = "";
        int index = 0;
        System.out.println("Response: \n" + response);
        
        while (it.hasNext())
        {
            HRow row = (HRow) it.next();
            equipGUID = row.get("id").toString();
            if (equipGUID != "")
            {
                CCUHsApi.getInstance().putUIDMap(equipLUIDList.get(index++), equipGUID); //TODO- Check ordering
            }
        }
    
        System.out.println("Synced Equips: "+hayStack.tagsDb.idMap);
    
        doSyncPoints(siteLUID, equipLUIDList);
        
        
    }
    
    private void doSyncPoints(String siteLUID, ArrayList<String> equipLUIDList) {
        
        for (String equipLUID : equipLUIDList)
        {
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
                    m.put("siteRef", hayStack.getGUID(siteLUID));
                    m.put("equipRef", hayStack.getGUID(equipLUID));
                    entities.add(HSUtil.mapToHDict(m));
                }
                System.out.println(m);
            }
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            String guid;
            int index = 0;
            System.out.println("Response: \n" + response);
            while (it.hasNext())
            {
                HRow row = (HRow) it.next();
                guid = row.get("id").toString();
                CCUHsApi.getInstance().putUIDMap(pointLUIDList.get(index++), guid); //TODO- Check ordering
            }
            System.out.println("Synced Points: "+hayStack.tagsDb.idMap);
        }
    }
    
    
    
    public boolean isSyncNeeded() {
        ArrayList<HashMap> sites = hayStack.readAll("site");
        for (Map s: sites) {
            if (hayStack.getGUID(s.get("id").toString()) == null) {
                return true;
            }
        }
        ArrayList<HashMap> equips = hayStack.readAll("equip");
        for (Map q: equips) {
            if (hayStack.getGUID(q.get("id").toString()) == null) {
                return true;
            }
        }
        ArrayList<HashMap> points = hayStack.readAll("point");
        for (Map p: points) {
            if (hayStack.getGUID(p.get("id").toString()) == null) {
                return true;
            }
        }
        return false;
    }
}
