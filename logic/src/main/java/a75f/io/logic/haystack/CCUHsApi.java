package a75f.io.logic.haystack;

import org.projecthaystack.HDict;
import org.projecthaystack.HGrid;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.UnknownRecException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import a75f.io.logic.bo.haystack.Device;
import a75f.io.logic.bo.haystack.Equip;
import a75f.io.logic.bo.haystack.Point;
import a75f.io.logic.bo.haystack.RawPoint;
import a75f.io.logic.bo.haystack.Site;

/**
 * Created by samjithsadasivan on 9/3/18.
 */

public class CCUHsApi
{
    private static CCUHsApi instance = new CCUHsApi();
    
    public AndroidHSClient hsClient;
    public CCUTagsDb tagsDb;
    
    public static CCUHsApi getInstance(){
        return instance;
    }
    private CCUHsApi() {
        hsClient = new AndroidHSClient();
        tagsDb = (CCUTagsDb) hsClient.db();
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
    
    /**
     * Write to a 'writable' point
     */
    public void writePoint(String id, int level, String who, Double val, int duration) {
        hsClient.pointWrite(HRef.make(id), level, who, HNum.make(val), HNum.make(duration));
        tagsDb.saveMap();
    }
    
    /**
     * Write to a 'writable' point
     * with default level  - 9
     * default user - ""
     */
    public void writePoint(String id, Double val) {
        hsClient.pointWrite(HRef.make(id), 9, "", HNum.make(val), HNum.make(0));
        tagsDb.saveMap();
    }
    
    /**
     * Returns an arrayList of point vals hashmaps for all levels in write array.
     * @param id
     * @return
     */
    public ArrayList<HashMap> readPoint(String id) {
        HGrid pArr = hsClient.pointWriteArray(HRef.make(id));
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
}
