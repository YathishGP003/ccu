package a75f.io.api.haystack;

import android.content.Context;
import android.util.Log;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HHisItem;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.HStr;
import org.projecthaystack.HVal;
import org.projecthaystack.UnknownRecException;
import org.projecthaystack.client.HClient;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import a75f.io.api.haystack.sync.EntitySyncHandler;
import a75f.io.api.haystack.sync.HisSyncHandler;
import a75f.io.api.haystack.sync.HttpUtil;

/**
 * Created by samjithsadasivan on 9/3/18.
 */

public class CCUHsApi
{
    private static CCUHsApi instance;
    
    public AndroidHSClient hsClient;
    public CCUTagsDb tagsDb;
    
    public EntitySyncHandler entitySyncHandler;
    public HisSyncHandler    hisSyncHandler;
    
    public static CCUHsApi getInstance(){
        if (instance == null) {
            throw new IllegalStateException("Hay stack api is not initialized");
        }
        return instance;
    }
    
    public CCUHsApi(Context c) {
        if (instance != null) {
            throw new IllegalStateException("Api instance already created , use getInstance()");
        }
        hsClient = new AndroidHSClient();
        tagsDb = (CCUTagsDb) hsClient.db();
        tagsDb.init(c);
        instance = this;
        entitySyncHandler = new EntitySyncHandler(this);
        hisSyncHandler = new HisSyncHandler(this);
        Log.d("Haystack","Api created");
    }
    
    //TODO - Temp for Unit test
    public CCUHsApi() {
        if (instance != null) {
            throw new IllegalStateException("Api instance already created , use getInstance()");
        }
        hsClient = new AndroidHSClient();
        tagsDb = (CCUTagsDb) hsClient.db();
        tagsDb.setTagsDbMap(new HashMap());
        instance = this;
        entitySyncHandler = new EntitySyncHandler(this);
        hisSyncHandler = new HisSyncHandler(this);
    }
    
    public HClient getHSClient(){
        return hsClient;
    }
    
    public void saveTagsData() {
        tagsDb.saveTags();
    }
    
    public void addSite(Site s) {
        tagsDb.addSite(s);
    }
    
    public String addEquip(Equip q){
        return tagsDb.addEquip(q);
    }
    
    public String addPoint(Point p) {
        return tagsDb.addPoint(p);
    }
    
    public String addPoint(RawPoint p) {
        return tagsDb.addPoint(p);
    }
    
    public String addDevice(Device d) {
        return tagsDb.addDevice(d);
    }
    
    /**
     * Helper method that converts HGrid to an Array of Hashmap of String.
     */
    public ArrayList<HashMap> readAll(String query) {
        
        ArrayList<HashMap> rowList = new ArrayList<>();
        try
        {
            HGrid grid = hsClient.readAll(query);
            Iterator it = grid.iterator();
            while (it.hasNext())
            {
                HashMap<Object, Object> map = new HashMap<>();
                HRow r = (HRow) it.next();
                HRow.RowIterator ri = (HRow.RowIterator) r.iterator();
                while (ri.hasNext())
                {
                    HDict.MapEntry m = (HDict.MapEntry) ri.next();
                    map.put(m.getKey(), m.getValue());
                }
                rowList.add(map);
            }
        } catch (UnknownRecException e) {
            e.printStackTrace();
        }
        return rowList;
    }
    
    /**
     * Read the first matching record
     */
    public HashMap read(String query) {
        
        HashMap<Object, Object> map = new HashMap<>();
        try
        {
            HDict dict = hsClient.read(query);
            Iterator it = dict.iterator();
            while (it.hasNext())
            {
                Map.Entry entry = (Map.Entry) it.next();
                map.put(entry.getKey().toString(), entry.getValue().toString());
            }
        } catch (UnknownRecException e) {
            e.printStackTrace();
        }
        return map;
    }
    
    public HDict readHDict(String query) {
        try
        {
            HDict dict = hsClient.read(query);
            return dict;
        } catch (UnknownRecException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public HGrid readHGrid(String query) {
        try
        {
            HGrid grid = hsClient.readAll(query);
            return grid;
        } catch (UnknownRecException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Write to a 'writable' point
     */
    public void writePoint(String id, int level, String who, Double val, int duration) {
        pointWrite(HRef.copy(id), level, who, HNum.make(val), HNum.make(duration,"ms"));
    }
    
    /**
     * Write to a 'writable' point
     * with default level  - 9
     * default user - ""
     */
    public void writePoint(String id, Double val) {
        pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, "", HNum.make(val), HNum.make(0));
    }
    
    /**
     * Write to the first 'writable' point fetched using query
     * at default level  - 9
     * default user - ""
     */
    public void writeDefaultVal(String query, Double val) {
        ArrayList points = readAll(query);
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, "", HNum.make(val), HNum.make(0));
    }
    
    public void writeDefaultValById(String id, Double val) {
        pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, "", HNum.make(val), HNum.make(0));
    }
    
    public void pointWrite(HRef id, int level, String who, HVal val, HNum dur) {
        hsClient.pointWrite(id,level,who,val,dur);
        
        String guid = getGUID(id.toString());
        if (guid != null)
        {
            HDictBuilder b = new HDictBuilder().add("id", HRef.copy(guid)).add("level", level).add("who", who).add("val", val).add("duration", dur);
            HDict[] dictArr = {b.toDict()};
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "pointWrite", HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
            System.out.println("Response: \n" + response);
        }
    }
    
    /**
     * Write to a 'writable' point
     * with default level  - 9
     * default user - ""
     */
    public Double readDefaultVal(String query) {
    
        ArrayList points = readAll(query);
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            return null;
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(HayStackConstants.DEFAULT_POINT_LEVEL - 1));
            return Double.parseDouble(valMap.get("val").toString());
        } else {
            return null;
        }
    }
    
    public Double readDefaultValById(String id) {
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(HayStackConstants.DEFAULT_POINT_LEVEL - 1));
            return Double.parseDouble(valMap.get("val").toString());
        } else {
            return null;
        }
    }
    
    
    /**
     * Returns an arrayList of point vals hashmaps for all levels in write array.
     * @param id
     * @return
     */
    public ArrayList<HashMap> readPoint(String id) {
        HGrid pArr = hsClient.pointWriteArray(HRef.copy(id));
        ArrayList<HashMap> rowList = new ArrayList<>();
        Iterator it = pArr.iterator();
        while(it.hasNext()) {
            HashMap<Object, Object> map = new HashMap<>();
            HRow r = (HRow)it.next();
            HRow.RowIterator ri = (HRow.RowIterator) r.iterator();
            while (ri.hasNext()) {
                HDict.MapEntry m = (HDict.MapEntry)ri.next();
                map.put(m.getKey(), m.getValue());
            }
            rowList.add(map);
        }
        return rowList;
    }
    
    public void hisWrite(ArrayList<HisItem> hisList) {
        HHisItem[] array = new HHisItem[hisList.size()];
        for (int itr = 0; itr< array.length; itr++) {
            HisItem item = hisList.get(itr);
            array[itr] = HHisItem.make(HDateTime.make(item.date.getTime()),HNum.make(item.getVal()));
        }
        hsClient.hisWrite(HRef.copy(hisList.get(0).getRec()), array);
    }
    public void hisWrite(HisItem item) {
        hsClient.hisWrite(HRef.copy(item.getRec()), new HHisItem[]{HHisItem.make(HDateTime.make(item.date.getTime()),HNum.make(item.val))});
    }
    
    public ArrayList<HisItem> hisRead(String id, Object range) {
        HGrid resGrid = hsClient.hisRead(HRef.copy(id),range);
        ArrayList<HisItem> hisList = new ArrayList<>();
        Iterator it = resGrid.iterator();
        while(it.hasNext()) {
            HisItem item = new HisItem();
            HRow r = (HRow)it.next();
            HDateTime date = (HDateTime) r.get("ts");
            HNum val = (HNum) r.get("val");
            hisList.add(new HisItem("",new Date(date.millis()),Double.parseDouble(val.toString())));
        }
        return hisList;
    }
    
    /**
     * Reads most recent value for a his point
     * @param id
     * @return
     */
    public HisItem hisRead(String id) {
        HGrid resGrid = hsClient.hisRead(HRef.copy(id),"today");
        if (resGrid.numRows() == 0) {
            return null;
        }
        HRow r = resGrid.row(resGrid.numRows()-1);
        HDateTime date = (HDateTime) r.get("ts");
        HNum val = (HNum) r.get("val");
        return new HisItem("",new Date(date.millis()),Double.parseDouble(val.toString()));
    }
    
    public Double readHisValById(String id) {
        HGrid resGrid = hsClient.hisRead(HRef.copy(id),"today");//TODO
        if (resGrid.numRows() == 0) {
            return 0.0;
        }
        HRow r = resGrid.row(resGrid.numRows()-1);
        return Double.parseDouble(r.get("val").toString());
    }
    
    public void writeHisValById(String id, Double val) {
        hsClient.hisWrite(HRef.copy(id), new HHisItem[]{HHisItem.make(HDateTime.make(System.currentTimeMillis()),HNum.make(val))});
    }
    public Double readHisValByQuery(String query) {
        ArrayList points = readAll(query);
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            return null;
        }
        
        HisItem item = hisRead(id);
        
        return item == null ? 0 : item.getVal();
        
    }
    
    public void writeHisValByQuery(String query, Double val) {
        
        ArrayList points = readAll(query);
        String id = ((HashMap)points.get(0)).get("id").toString();
        if (id == null || id == "") {
            return;
        }
        
        HisItem item = new HisItem(id, new Date(), val );
        hisWrite(item);
    }
    
    public ArrayList<HashMap> nav(String id) {
        
        ArrayList<HashMap> rowList = new ArrayList<>();
        try
        {
            HGrid grid = hsClient.nav(HStr.make(id.replace("@","")));
            Iterator it = grid.iterator();
            while (it.hasNext())
            {
                HashMap<Object, Object> map = new HashMap<>();
                HRow r = (HRow) it.next();
                HRow.RowIterator ri = (HRow.RowIterator) r.iterator();
                while (ri.hasNext())
                {
                    HDict.MapEntry m = (HDict.MapEntry) ri.next();
                    map.put(m.getKey(), m.getValue());
                }
                rowList.add(map);
            }
        } catch (UnknownRecException e) {
            e.printStackTrace();
        }
        return rowList;
    }
    
    public void deleteEntity(String id) {
        Log.d("CCU", "deleteEntity "+id);
        tagsDb.tagsMap.remove(id.replace("@",""));
        if (tagsDb.idMap.get(id) != null)
        {
            System.out.println(id);
            tagsDb.removeIdMap.put(id, tagsDb.idMap.remove(id));
        }
    }
    
    public void deleteWritableArray(String id) {
        tagsDb.writeArrays.remove(id.replace("@",""));
    }
    
    public void deleteEntityTree(String id) {
        
        Log.d("CCU", "deleteEntityTree "+id);
        
        HashMap entity = CCUHsApi.getInstance().read("id == "+id);
        if (entity.get("site") != null) {
            ArrayList<HashMap> equips = readAll("equip and siteRef == \""+id+"\"");
            for (HashMap equip : equips) {
                deleteEntityTree(equip.get("id").toString());
            }
    
            ArrayList<HashMap> devices = readAll("device and siteRef == \""+id+"\"");
            for (HashMap device : devices) {
                deleteEntityTree(device.get("id").toString());
            }
            deleteEntity(id);
            
        } else if (entity.get("equip") != null) {
            ArrayList<HashMap> points = readAll("point and equipRef == \""+id+"\"");
            for (HashMap point : points) {
                if (point.get("writable") != null)
                {
                    deleteWritableArray(point.get("id").toString());
                }
                deleteEntity(point.get("id").toString());
            }
            deleteEntity(id);
        } else if (entity.get("device") != null)
        {
            ArrayList<HashMap> points = readAll("point and deviceRef == \"" + id + "\"");
            for (HashMap point : points)
            {
                if (point.get("writable") != null)
                {
                    deleteWritableArray(point.get("id").toString());
                }
                deleteEntity(point.get("id").toString());
            }
            deleteEntity(id);
        } else if (entity.get("point") != null) {
            if (entity.get("writable") != null)
            {
                deleteWritableArray(entity.get("id").toString());
            }
            deleteEntity(entity.get("id").toString());
        }
    }
    
    public void putUIDMap(String luid, String guid){
        tagsDb.idMap.put(luid, guid);
    }
    
    public String getGUID(String luid){
        return tagsDb.idMap.get(luid);
    }
    
    public void syncEntityTree() {
        entitySyncHandler.sync();
    }
    
    public void syncHisData() {
        hisSyncHandler.doSync();
    }
}
