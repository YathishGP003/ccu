package a75f.io.api.haystack;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.projecthaystack.HDate;
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
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;
import org.projecthaystack.server.HStdOps;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import a75f.io.api.haystack.sync.EntityParser;
import a75f.io.api.haystack.sync.EntitySyncHandler;
import a75f.io.api.haystack.sync.HisSyncHandler;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.api.haystack.sync.InfluxDbUtil;
import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 9/3/18.
 */
public class CCUHsApi
{

    public static  boolean  DEBUG_CCUHS = true;
    public static boolean CACHED_HIS_QUERY = false ;
    private static CCUHsApi instance;

    public AndroidHSClient hsClient;
    public CCUTagsDb       tagsDb;

    public EntitySyncHandler entitySyncHandler;
    public HisSyncHandler    hisSyncHandler;

    public boolean testHarnessEnabled = false;
    
    public boolean unitTestingEnabled = true;
    
    Context cxt;
    
    String hayStackUrl = "";
    String influxUrl = "";
    String careTakerUrl ="";
    
    HRef tempWeatherRef = null;
    HRef humidityWeatherRef = null;

    public static CCUHsApi getInstance()
    {
        if (instance == null)
        {
            throw new IllegalStateException("Hay stack api is not initialized");
        }
        return instance;
    }

    public CCUHsApi(Context c)
    {
        if (instance != null)
        {
            throw new IllegalStateException("Api instance already created , use getInstance()");
        }
        cxt = c;
        hsClient = new AndroidHSClient();
        tagsDb = (CCUTagsDb) hsClient.db();
        tagsDb.init(cxt);
        instance = this;
        entitySyncHandler = new EntitySyncHandler();
        hisSyncHandler = new HisSyncHandler(this);
    }

    //TODO - Temp for Unit test
    public CCUHsApi()
    {
        if (instance != null)
        {
            throw new IllegalStateException("Api instance already created , use getInstance()");
        }
        hsClient = new AndroidHSClient();
        tagsDb = (CCUTagsDb) hsClient.db();
        tagsDb.init();
        instance = this;
        entitySyncHandler = new EntitySyncHandler();
        hisSyncHandler = new HisSyncHandler(this);
    }

    public HClient getHSClient()
    {
        return hsClient;
    }
    
    public String getHSUrl() {
        if (hayStackUrl.equals(""))
        {
            SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(cxt);
            switch (sprefs.getString("SERVER_ENV", "")) {
                case "QA":
                    hayStackUrl = "https://haystack-75f-service-qa.azurewebsites.net/";
                    break;
                case "DEV":
                    hayStackUrl = "https://haystack-75f-service-dev.azurewebsites.net/";
                    break;

                case "STAGING":
                    hayStackUrl = "https://haystack-75f-service-staging.azurewebsites.net/";
                    break;
                case "PROD":
                default:
                    hayStackUrl = "https://haystack-75f-service.azurewebsites.net/";
                    break;
                 
            }
        }
        
        return hayStackUrl;
    }
    public String getAuthenticationUrl() {
        if (careTakerUrl.equals(""))
        {
            SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(cxt);
            switch (sprefs.getString("SERVER_ENV", "")) {
                case "QA":
                    careTakerUrl = "https://caretaker-75f-service-qa.azurewebsites.net/";
                    break;
                case "DEV":
                    careTakerUrl = "https://caretaker-75f-service-dev.azurewebsites.net/";
                    break;

                case "STAGING":
                    careTakerUrl = "https://caretaker-75f-service-staging.azurewebsites.net/";
                    break;
                case "PROD":
                default:
                    careTakerUrl = "https://caretaker-75f-service.azurewebsites.net/";
                    break;

            }
        }
        Log.d("PRODUCT FLAVOUR","careTakerUrl url="+careTakerUrl);
        return careTakerUrl;
    }
    public String getInfluxUrl() {
        if (influxUrl.equals(""))
        {
            SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(cxt);
            
            switch (sprefs.getString("SERVER_ENV", "")) {
                case "QA":
                    influxUrl = new InfluxDbUtil.URLBuilder().setProtocol(InfluxDbUtil.HTTP).setHost("influx-75f-objectstore-qa.northcentralus.cloudapp.azure.com").setPort(8086).setOp(InfluxDbUtil.WRITE).setDatabse("haystack").setUser("75f@75f.io").setPassword("7575").buildUrl();
                    break;
                case "DEV":
                    //influxUrl = new InfluxDbUtil.URLBuilder().setProtocol(InfluxDbUtil.HTTP).setHost("influx.northcentralus.cloudapp.azure.com").setPort(8086).setOp(InfluxDbUtil.WRITE).setDatabse("haystack").setUser("ccu").setPassword("7575").buildUrl();
                    influxUrl = new InfluxDbUtil.URLBuilder().setProtocol(InfluxDbUtil.HTTP).setHost("influx-75f-objectstore-dev.northcentralus.cloudapp.azure.com").setPort(8086).setOp(InfluxDbUtil.WRITE).setDatabse("haystack").setUser("75f@75f.io").setPassword("7575").buildUrl();
                    break;
                case "STAGING":
                    influxUrl = new InfluxDbUtil.URLBuilder().setProtocol(InfluxDbUtil.HTTP).setHost("influx-75f-objectstore-staging.northcentralus.cloudapp.azure.com").setPort(8086).setOp(InfluxDbUtil.WRITE).setDatabse("haystack").setUser("75f@75f.io").setPassword("7575").buildUrl();
                    break;
                case "PROD":
                default:
                    influxUrl = new InfluxDbUtil.URLBuilder().setProtocol(InfluxDbUtil.HTTP).setHost("influx-75f-objectstore-prod.northcentralus.cloudapp.azure.com").setPort(8086).setOp(InfluxDbUtil.WRITE).setDatabse("haystack").setUser("75f@75f.io").setPassword("7575").buildUrl();
                    break;
            }
        }
    
        return influxUrl;
        
    }
    
    public synchronized void saveTagsData()
    {
        tagsDb.saveTags();
    }

    public String addSite(Site s)
    {
        return tagsDb.addSite(s);
    }

    public String addEquip(Equip q)
    {
        return tagsDb.addEquip(q);
    }

    public String addPoint(Point p)
    {
        return tagsDb.addPoint(p);
    }

    public String addPoint(RawPoint p)
    {
        return tagsDb.addPoint(p);
    }

    public String addPoint(SettingPoint p)
    {
        return tagsDb.addPoint(p);
    }

    public String addDevice(Device d)
    {
        return tagsDb.addDevice(d);
    }

    public String addFloor(Floor f)
    {
        return tagsDb.addFloor(f);
    }

    public String addZone(Zone z)
    {
        return tagsDb.addZone(z);
    }

    public void updateSite(Site s, String id)
    {
        tagsDb.updateSite(s, id);
        if (tagsDb.idMap.get(id) != null)
        {
            tagsDb.updateIdMap.put(id, tagsDb.idMap.get(id));
        }
    }

    public void updateEquip(Equip q, String id)
    {
        tagsDb.updateEquip(q, id);
        if (tagsDb.idMap.get(id) != null)
        {
            tagsDb.updateIdMap.put(id, tagsDb.idMap.get(id));
        }
    }

    public void updatePoint(Point p, String id)
    {
        tagsDb.updatePoint(p, id);
        if (tagsDb.idMap.get(id) != null)
        {
            tagsDb.updateIdMap.put(id, tagsDb.idMap.get(id));
        }
    }

    public void updatePoint(RawPoint r, String id)
    {
        tagsDb.updatePoint(r, id);
        if (tagsDb.idMap.get(id) != null)
        {
            tagsDb.updateIdMap.put(id, tagsDb.idMap.get(id));
        }
    }

    public void updateFloor(Floor r, String id)
    {
        tagsDb.updateFloor(r, id);
        if (tagsDb.idMap.get(id) != null)
        {
            tagsDb.updateIdMap.put(id, tagsDb.idMap.get(id));
        }
    }

    public void updateZone(Zone z, String id)
    {
        tagsDb.updateZone(z, id);
        if (tagsDb.idMap.get(id) != null)
        {
            tagsDb.updateIdMap.put(id, tagsDb.idMap.get(id));
        }
    }

    /**
     * Helper method that converts HGrid to an Array of Hashmap of String.
     */
    public ArrayList<HashMap> readAll(String query)
    {
        //CcuLog.d("CCU_HS", "Read Query: " + query);
        ArrayList<HashMap> rowList = new ArrayList<>();
        try
        {
            HGrid grid = hsClient.readAll(query);
            if (grid != null)
            {
                Iterator it = grid.iterator();
                while (it.hasNext())
                {
                    HashMap<Object, Object> map = new HashMap<>();
                    HRow                    r   = (HRow) it.next();
                    HRow.RowIterator        ri  = (HRow.RowIterator) r.iterator();
                    while (ri.hasNext())
                    {
                        HDict.MapEntry m = (HDict.MapEntry) ri.next();
                        map.put(m.getKey(), m.getValue());
                    }
                    rowList.add(map);
                }
            }
        }
        catch (UnknownRecException e)
        {
            e.printStackTrace();
        }
        return rowList;
    }

    /**
     * Read the first matching record
     */
    public HashMap read(String query)
    {
        //CcuLog.d("CCU_HS", "Read Query: " + query);
        HashMap<Object, Object> map = new HashMap<>();
        try
        {
            HDict    dict = hsClient.read(query, true);
            Iterator it   = dict.iterator();
            while (it.hasNext())
            {
                Map.Entry entry = (Map.Entry) it.next();
                map.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        catch (UnknownRecException e)
        {
            e.printStackTrace();
        }
        return map;
    }

    public HashMap readMapById(String id)
    {

        HashMap<Object, Object> map = new HashMap<>();
        try
        {
            HDict    dict = hsClient.readById(HRef.copy(id));
            Iterator it   = dict.iterator();
            while (it.hasNext())
            {
                Map.Entry entry = (Map.Entry) it.next();
                map.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        catch (UnknownRecException e)
        {
            e.printStackTrace();
        }
        return map;
    }

    public HDict readHDictById(String id)
    {
        try
        {
            return hsClient.readById(HRef.copy(id));
        }
        catch (UnknownRecException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public HDict readHDict(String query)
    {
        try
        {
            HDict dict = hsClient.read(query);
            return dict;
        }
        catch (UnknownRecException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public HGrid readHGrid(String query)
    {
        try
        {
            HGrid grid = hsClient.readAll(query);
            return grid;
        }
        catch (UnknownRecException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Write to a 'writable' point
     */
    public void writePoint(String id, int level, String who, Double val, int duration)
    {
        pointWrite(HRef.copy(id), level, who, HNum.make(val), HNum.make(duration));
    }

    /**
     * Write to a 'writable' point
     * with default level  - 9
     * default user - ""
     */
    public void writePoint(String id, Double val)
    {
        pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, "ccu", HNum.make(val), HNum.make(0));
    }

    /**
     * Write to the first 'writable' point fetched using query
     * at default level  - 9
     * default user - ""
     */
    public void writeDefaultVal(String query, Double val)
    {
        try {
            ArrayList points = readAll(query);
            String id = ((HashMap) points.get(0)).get("id").toString();
            if (id == null || id == "") {
                throw new IllegalArgumentException();
            }
            pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, "ccu", HNum.make(val), HNum.make(0));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void writeDefaultVal(String query, String val)
    {
        ArrayList points = readAll(query);
        String    id     = ((HashMap) points.get(0)).get("id").toString();
        if (id == null || id == "")
        {
            throw new IllegalArgumentException();
        }
        pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, "ccu", HStr.make(val), HNum.make(0));
    }

    public void writeDefaultValById(String id, Double val)
    {
        pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, "ccu", HNum.make(val), HNum.make(0));
    }

    public void writeDefaultValById(String id, String val)
    {
        pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, "ccu", HStr.make(val), HNum.make(0));
    }

    public void pointWrite(HRef id, int level, String who, HVal val, HNum dur)
    {
        if (!CCUHsApi.getInstance().isCCURegistered()){
            return;
        }

        hsClient.pointWrite(id, level, who, val, dur);

        String guid = getGUID(id.toString());
        if (guid != null)
        {
            if (dur.unit == null) {
                dur = HNum.make(dur.val ,"ms");
            }
            HDictBuilder b = new HDictBuilder().add("id", HRef.copy(guid)).add("level", level).add("who", who).add("val", val).add("duration", dur);
            HDict[] dictArr  = {b.toDict()};
            String  response = HttpUtil.executePost(getHSUrl() + "pointWrite", HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
            CcuLog.d("CCU_HS", "Response: \n" + response);
        }
    }

    /**
     * Write to a 'writable' point
     * with default level  - 9
     * default user - ""
     */
    public Double readDefaultVal(String query)
    {

        ArrayList points = readAll(query);
        if((points != null) && (points.size() > 0)) {
            String id = ((HashMap) points.get(0)).get("id").toString();
            if (id == null || id == "") {
                return 0.0;
            }
            ArrayList values = CCUHsApi.getInstance().readPoint(id);
            if (values != null && values.size() > 0) {
                HashMap valMap = ((HashMap) values.get(HayStackConstants.DEFAULT_POINT_LEVEL - 1));
                CcuLog.d("CCU_HS", "" + valMap);
                return valMap.get("val") == null ? 0 : Double.parseDouble(valMap.get("val").toString());
            } else {
                return null;
            }
        }else return 0.0;
    }

    public Double readDefaultValById(String id)
    {
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(HayStackConstants.DEFAULT_POINT_LEVEL - 1));
            return valMap.get("val") == null ? 0 : Double.parseDouble(valMap.get("val").toString());
        } else
        {
            return null;
        }
    }
    
    public String readDefaultStrValById(String id)
    {
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(HayStackConstants.DEFAULT_POINT_LEVEL - 1));
            return valMap.get("val") == null ? "": valMap.get("val").toString();
        } else
        {
            return "";
        }
    }

    public String readDefaultStrVal(String query)
    {

        ArrayList points = readAll(query);
        String    id     = ((HashMap) points.get(0)).get("id").toString();
        if (id == null || id == "")
        {
            return "";
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(HayStackConstants.DEFAULT_POINT_LEVEL - 1));
            CcuLog.d("CCU_HS", "" + valMap);
            return valMap.get("val") == null ? "" : valMap.get("val").toString();
        } else
        {
            return "";
        }
    }


    /**
     * Returns an arrayList of point vals hashmaps for all levels in write array.
     *
     * @param id
     * @return
     */
    public ArrayList<HashMap> readPoint(String id)
    {
        HGrid              pArr    = hsClient.pointWriteArray(HRef.copy(id));
        ArrayList<HashMap> rowList = new ArrayList<>();
        if (pArr == null || pArr.isEmpty()) {
            return rowList;
        }
        Iterator           it      = pArr.iterator();
        while (it.hasNext())
        {
            HashMap<Object, Object> map = new HashMap<>();
            HRow                    r   = (HRow) it.next();
            HRow.RowIterator        ri  = (HRow.RowIterator) r.iterator();
            while (ri.hasNext())
            {
                HDict.MapEntry m = (HDict.MapEntry) ri.next();
                map.put(m.getKey(), m.getValue());
            }
            rowList.add(map);
        }
        return rowList;
    }
    
    public HGrid readPointGrid(String id) {
        return hsClient.pointWriteArray(HRef.copy(id));
    }
    
    public HGrid readPointArrRemote(String id) {
        HDictBuilder b = new HDictBuilder().add("id", HRef.copy(id));
        HDict[] dictArr  = {b.toDict()};
        String response = HttpUtil.executePost(getHSUrl() + "pointWrite", HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
        CcuLog.d("CCU_HS", "Response : "+response);
      
        return response == null ? null : new HZincReader(response).readGrid();
    }
    
    public HGrid readByIdRemote(String id) {
        HDictBuilder b = new HDictBuilder().add("id", HRef.copy(id));
        HDict[] dictArr  = {b.toDict()};
        String response = HttpUtil.executePost(getHSUrl() + "read", HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
        CcuLog.d("CCU_HS", "Response : "+response);
        return response == null ? null : new HZincReader(response).readGrid();
    }

    public ArrayList<HisItem> hisReadRemote(String id, Object range)
    {
        HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HGrid              resGrid = hClient.hisRead(HRef.copy(id), range);
        ArrayList<HisItem> hisList = new ArrayList<>();
        Iterator           it      = resGrid.iterator();
        while (it.hasNext())
        {
            HRow      r    = (HRow) it.next();
            HDateTime date = (HDateTime) r.get("ts");
            HNum      val  = (HNum) r.get("val");
            hisList.add(new HisItem("", new Date(date.millis()), Double.parseDouble(val.toString())));
        }
        return hisList;
    }
    
    
    public double readPointPriorityVal(String id) {
        
        ArrayList values = readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
    
    public Double readPointPriorityValByQuery(String query)
    {
        ArrayList points = readAll(query);
        String    id     = points.size() == 0 ? null : ((HashMap) points.get(0)).get("id").toString();
        if (id == null || id == "")
        {
            return null;
        }
        
        return readPointPriorityVal(id);
    }
    
    public String readId(String query)
    {
        ArrayList points = readAll(query);
        String    id     = points.size() == 0 ? null : ((HashMap) points.get(0)).get("id").toString();
        if (id == null || id == "")
        {
            return null;
        }
        
        return id;
    }
    
    
    public void hisWrite(ArrayList<HisItem> hisList)
    {
        HHisItem[] array = new HHisItem[hisList.size()];
        for (int itr = 0; itr < array.length; itr++)
        {
            HisItem item = hisList.get(itr);
            array[itr] = HHisItem.make(HDateTime.make(item.date), HNum.make(item.getVal()));
        }
        hsClient.hisWrite(HRef.copy(hisList.get(0).getRec()), array);
    }

    public void hisWrite(HisItem item)
    {
        hsClient.hisWrite(HRef.copy(item.getRec()), new HHisItem[]{HHisItem.make(HDateTime.make(item.date), HNum.make(item.val))});
    }

    public ArrayList<HisItem> hisRead(String id, Object range)
    {
        HGrid              resGrid = hsClient.hisRead(HRef.copy(id), range);
        ArrayList<HisItem> hisList = new ArrayList<>();
        Iterator           it      = resGrid.iterator();
        while (it.hasNext())
        {
            HRow      r    = (HRow) it.next();
            HDateTime date = (HDateTime) r.get("ts");
            HNum      val  = (HNum) r.get("val");
            hisList.add(new HisItem("", new Date(date.millis()), Double.parseDouble(val.toString())));
        }
        return hisList;
    }


    /**
     * Reads most recent value for a his point
     *
     * @param id
     * @return
     */
    public HisItem curRead(String id)
    {
        /*HGrid resGrid = hsClient.hisRead(HRef.copy(id), "current");
        if (resGrid == null || (resGrid != null && resGrid.isEmpty()))
        {
            return null;
        }
        HRow      r    = resGrid.row(resGrid.numRows() - 1);
        HDateTime date = (HDateTime) r.get("ts");
        HNum      val  = (HNum) r.get("val");
        return new HisItem("", new Date(date.millis()), Double.parseDouble(val.toString()));*/
        return tagsDb.getLastHisItem(HRef.copy(id));
    }

    public Double readHisValById(String id)
    {
        HisItem item = curRead(id);
        return item == null ? 0 : item.getVal();
    }

    public Double readHisValByQuery(String query)
    {
        if (CACHED_HIS_QUERY)
        {
            String cachedId = QueryCache.getInstance().get(query);
            if (cachedId == null)
            {
                HashMap p = read(query);
                if (p.size() == 0)
                {
                    return 0.0;
                }
                cachedId = p.get("id").toString();
                QueryCache.getInstance().add(query, cachedId);
            }
            HisItem cachedItem = curRead(cachedId);
            return cachedItem == null ? 0 : cachedItem.getVal();
        } else {
            ArrayList points = readAll(query);
            String    id     = points.size() == 0 ? null : ((HashMap) points.get(0)).get("id").toString();
            if (id == null || id == "")
            {
                return 0.0;
            }
    
            HisItem item = curRead(id);
            return item == null ? 0 : item.getVal();
        }
    }

    public synchronized void writeHisValById(String id, Double val)
    {
        HisItem item = curRead(id);
        Double prevVal = item == null ? 0 : item.getVal();
        if((item == null)|| (!item.initialized) || !prevVal.equals(val))
            hsClient.hisWrite(HRef.copy(id), new HHisItem[]{HHisItem.make(HDateTime.make(System.currentTimeMillis()), HNum.make(val))});
       
    }

    public synchronized void writeHisValByQuery(String query, Double val)
    {
        if (CACHED_HIS_QUERY)
        {
            String cachedId = QueryCache.getInstance().get(query);
            if (cachedId == null)
            {
                HashMap p = read(query);
                if (p.size() == 0)
                {
                    return;
                }
                HisItem previtem = curRead(cachedId);
                Double prevVal = previtem == null ? 0 : previtem.getVal();
                if((previtem == null) || (!previtem.initialized)|| !prevVal.equals(val) || query.contains("and diag")) {
                    cachedId = p.get("id").toString();
                    QueryCache.getInstance().add(query, cachedId);
                    HisItem item = new HisItem(cachedId, new Date(), val);
                    hisWrite(item);
                }
            }
        } else {
            ArrayList points = readAll(query);
            String    id     = points.size() == 0 ? null : ((HashMap) points.get(0)).get("id").toString();
            if (id == null || id == "") {
                CcuLog.d("CCU_HS","write point id is null");
                return;
            }
            HisItem previtem = curRead(id);
            Double prevVal = previtem == null ? 0 : previtem.getVal();
            if((previtem == null) || (!previtem.initialized) || !prevVal.equals(val) || query.contains("and diag")) {
                HisItem item = new HisItem(id, new Date(), val);
                hisWrite(item);
            }
        }
    }

    public ArrayList<HashMap> nav(String id)
    {

        ArrayList<HashMap> rowList = new ArrayList<>();
        try
        {
            HGrid    grid = hsClient.nav(HStr.make(id.replace("@", "")));
            Iterator it   = grid.iterator();
            while (it.hasNext())
            {
                HashMap<Object, Object> map = new HashMap<>();
                HRow                    r   = (HRow) it.next();
                HRow.RowIterator        ri  = (HRow.RowIterator) r.iterator();
                while (ri.hasNext())
                {
                    HDict.MapEntry m = (HDict.MapEntry) ri.next();
                    map.put(m.getKey(), m.getValue());
                }
                rowList.add(map);
            }
        }
        catch (UnknownRecException e)
        {
            e.printStackTrace();
        }
        return rowList;
    }

    public void deleteEntity(String id)
    {
        CcuLog.d("CCU_HS", "deleteEntity " + CCUHsApi.getInstance().readMapById(id).toString());
        tagsDb.tagsMap.remove(id.replace("@", ""));
        if (tagsDb.idMap.get(id) != null)
        {
            tagsDb.removeIdMap.put(id, tagsDb.idMap.remove(id));
        }
    }
    
    //Removes entity , but the operation is not synced to backend
    public void removeEntity(String id) {
        tagsDb.tagsMap.remove(id.replace("@", ""));
    }
    
    public void removeId(String id) {
        tagsDb.removeIdMap.remove(id.replace("@", ""));
    }

    public void deleteWritableArray(String id)
    {
        tagsDb.writeArrays.remove(id.replace("@", ""));
    }

    public void deleteEntityTree(String id)
    {
        CcuLog.d("CCU_HS", "deleteEntityTree " + id);
        HashMap entity = CCUHsApi.getInstance().read("id == " + id);
        if (entity.get("site") != null)
        {
            ArrayList<HashMap> floors = readAll("floor");
            for (HashMap floor : floors)
            {
                deleteEntityTree(floor.get("id").toString());
            }
            ArrayList<HashMap> equips = readAll("equip and siteRef == \"" + id + "\"");
            for (HashMap equip : equips)
            {
                deleteEntityTree(equip.get("id").toString());
            }
            ArrayList<HashMap> devices = readAll("device and siteRef == \"" + id + "\"");
            for (HashMap device : devices)
            {
                deleteEntityTree(device.get("id").toString());
            }
            ArrayList<HashMap> schedules = readAll("schedule and siteRef == \"" + id + "\"");
            for (HashMap schedule : schedules)
            {
                deleteEntity(schedule.get("id").toString());
            }
            deleteEntity(id);
        }
        else if (entity.get("floor") != null)
        {
            ArrayList<HashMap> rooms = readAll("room and floorRef == \"" + id + "\"");
            for (HashMap room : rooms)
            {
                deleteEntityTree(room.get("id").toString());
            }
            deleteEntity(entity.get("id").toString());
        }
        else if (entity.get("room") != null)
        {
            ArrayList<HashMap> schedules = readAll("schedule and roomRef == "+ id );
            Log.d("CCU","  delete Schedules in room "+schedules.size());
            for (HashMap schedule : schedules)
            {
                deleteEntity(schedule.get("id").toString());
            }
        
             deleteEntity(entity.get("id").toString());
        }else if (entity.get("equip") != null)
        {
            ArrayList<HashMap> points = readAll("point and equipRef == \"" + id + "\"");
            for (HashMap point : points)
            {
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
        } else if (entity.get("point") != null)
        {
            if (entity.get("writable") != null)
            {
                deleteWritableArray(entity.get("id").toString());
            }
            deleteEntity(entity.get("id").toString());
        }
    }

    public void putUIDMap(String luid, String guid)
    {
        tagsDb.idMap.put(luid, guid);
    }

    public String getGUID(String luid)
    {
        return tagsDb.idMap.get(luid);
    }

    public String getLUID(String guid)
    {
        for (Map.Entry<String, String> entry : tagsDb.idMap.entrySet())
        {
            if (entry.getValue().equals(guid))
            {
                return entry.getKey();
            }
        }
        return null;
    }
    
    public String getRemoveMapLUID(String guid)
    {
        for (Map.Entry<String, String> entry : tagsDb.removeIdMap.entrySet())
        {
            if (entry.getValue().equals(guid))
            {
                return entry.getKey();
            }
        }
        return null;
    }

    public void syncEntityTree()
    {
        new Thread()
        {
            @Override
            public void run()
            {
                if (!testHarnessEnabled)
                {
                    if (!entitySyncHandler.isSyncProgress())
                    {
                        entitySyncHandler.sync();
                    }
                } else
                {
                    CcuLog.d("CCU_HS", " Test Harness Enabled , Skip Entity Sync");
                }
            }
        }.start();
    }
    public void syncPointEntityTree()
    {
        new Thread()
        {
            @Override
            public void run()
            {
                if (!testHarnessEnabled)
                {

                        entitySyncHandler.syncPointEntity();

                } else
                {
                    CcuLog.d("CCU_HS", " Test Harness Enabled , Skip PointEntity Sync");
                }
            }
        }.start();
    }
    public void syncEntityWithPointWrite() {
        new Thread()
        {
            @Override
            public void run()
            {
                if (!testHarnessEnabled)
                {
                    entitySyncHandler.doSyncWithWrite();
                } else
                {
                    CcuLog.d("CCU_HS", " Test Harness Enabled , Skip Entity Sync");
                }
            }
        }.start();
    }
    
    //Force-writes local entities to the backend.
    public void forceSync() {
        tagsDb.idMap.clear();
        tagsDb.saveTags();
        tagsDb.init(cxt);
        syncEntityWithPointWrite();
    }
    
    public void scheduleSync() {
        entitySyncHandler.scheduleSync();
    }

    public void syncHisData()
    {
        hisSyncHandler.doSync();
    }

    public boolean syncExistingSite(String siteId)
    {
        HGrid remoteSite = getRemoteSite(siteId);


        if (remoteSite == null || remoteSite.isEmpty() || remoteSite.isErr())
        {
            return false;
        }

        //HGrid remoteSiteDetails = getRemoteSiteDetails(siteId);
        
        EntityParser p = new EntityParser(remoteSite);
        Site s = p.getSite();
        tagsDb.idMap.put("@"+tagsDb.addSite(s), s.getId());
        Log.d("CCU_HS","Added Site "+s.getId());
    
        HClient hClient = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict navIdDict = new HDictBuilder().add("navId", HRef.make(siteId)).toDict();
        HGrid hGrid = HGridBuilder.dictToGrid(navIdDict);
        HGrid syncData = hClient.call("sync", hGrid);
        
        p = new EntityParser(syncData);
        
        //tagsDb.addHGrid(remoteSite);
        //tagsDb.addHGrid(remoteSiteDetails);
    
        p.importSchedules();
        p.importBuildingTuner();
    
        ArrayList<HashMap> writablePoints = CCUHsApi.getInstance().readAll("point and writable");
        for (HashMap m : writablePoints) {
            System.out.println(m);
            HDict pid = new HDictBuilder().add("id",HRef.copy(getGUID(m.get("id").toString()))).toDict();
            HGrid wa = hClient.call("pointWrite",HGridBuilder.dictToGrid(pid));
            wa.dump();
        
            ArrayList<HashMap> valList = new ArrayList<>();
            Iterator it = wa.iterator();
            while (it.hasNext()) {
                HashMap<Object, Object> map = new HashMap<>();
                HRow r = (HRow) it.next();
                HRow.RowIterator ri = (HRow.RowIterator) r.iterator();
                while (ri.hasNext()) {
                    HDict.MapEntry e = (HDict.MapEntry) ri.next();
                    map.put(e.getKey(), e.getValue());
                }
                valList.add(map);
            }
            
            for(HashMap v : valList)
            {
                CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(m.get("id").toString()),
                        Integer.parseInt(v.get("level").toString()), v.get("who").toString(),
                        m.get("kind").toString().equals("string") ? HStr.make(v.get("val").toString()) : HNum.make(Double.parseDouble(v.get("val").toString())),HNum.make(0));
            }
        
        }
        
        tagsDb.log();

        return true;
    }

    public HGrid getRemoteSiteDetails(String siteId)
    {
        /* Sync a site*/
        HClient hClient   = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict   navIdDict = new HDictBuilder().add("navId", HRef.make(siteId)).toDict();
        HGrid   hGrid     = HGridBuilder.dictToGrid(navIdDict);

        HGrid sync = hClient.call("sync", hGrid);

        sync.dump();

        return sync;
    }

    public HGrid getRemoteSite(String siteId)
    {
        /* Sync a site*/
        HClient hClient   = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict   navIdDict = new HDictBuilder().add(HayStackConstants.ID, HRef.make(siteId)).toDict();
        HGrid   hGrid     = HGridBuilder.dictToGrid(navIdDict);

        HGrid sync = hClient.call(HStdOps.read.name(), hGrid);

        sync.dump();

        return sync;
    }


    public void log()
    {
        CcuLog.d("CCU_HS", "" + tagsDb);
    }

    public void addExistingSite(HGrid site)
    {
        tagsDb.addHGrid(site);
    }

    public HGrid getCCUs()
    {
        HDict hDict = new HDictBuilder().add("filter", "ccu").toDict();
        HGrid ccus  = getHSClient().call("read", HGridBuilder.dictToGrid(hDict));
        return ccus;
    }

    public void addOrUpdateConfigProperty(String thisCCU, HRef id)
    {
        tagsDb.updateConfig("currentCCU", id);
    }

    public String createCCU(String ccuName, String installerEmail, String equipRef, String managerEmail)
    {
        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        String ahuRef = equip.size() > 0 ? equip.get("id").toString() : "";
        
        HDictBuilder hDictBuilder = new HDictBuilder();
        CcuLog.d("CCU_HS", "Site Ref: " + getSiteId());
        String localId = UUID.randomUUID().toString();
        hDictBuilder.add("id", HRef.make(localId));
        hDictBuilder.add("ccu");
        hDictBuilder.add("dis", HStr.make(ccuName));
        hDictBuilder.add("installerEmail", HStr.make(installerEmail));
        hDictBuilder.add("fmEmail", HStr.make(managerEmail));
        hDictBuilder.add("siteRef", getSiteId());
        hDictBuilder.add("equipRef", equipRef);
        hDictBuilder.add("createdDate", HDateTime.make(System.currentTimeMillis()).date);
        hDictBuilder.add("gatewayRef", ahuRef);
        hDictBuilder.add("ahuRef", ahuRef);
        hDictBuilder.add("device");
        tagsDb.addHDict(localId, hDictBuilder.toDict());
        return localId;
    }

    public void updateCCU(String ccuName, String installerEmail, String ahuRef, String managerEmail)
    {
        Log.d("CCU_HS","updateCCUahuRef "+ahuRef);
        HashMap ccu = read("device and ccu");

        if (ccu.size() == 0) {
            return;
        }
        final String id = ccu.get("id").toString();

        HDictBuilder hDictBuilder = new HDictBuilder();
        hDictBuilder.add("id", HRef.copy(id));
        hDictBuilder.add("ccu");
        hDictBuilder.add("dis", HStr.make(ccuName));
        hDictBuilder.add("fmEmail", HStr.make(managerEmail));
        hDictBuilder.add("installerEmail", HStr.make(installerEmail));
        hDictBuilder.add("siteRef", getSiteId());
        hDictBuilder.add("equipRef", ccu.get("equipRef").toString());
        hDictBuilder.add("createdDate", HDate.make(ccu.get("createdDate").toString()));
        hDictBuilder.add("gatewayRef", ahuRef);
        hDictBuilder.add("ahuRef", ahuRef);
        hDictBuilder.add("device");
        if (hDictBuilder.has("unregister")){
            hDictBuilder.remove("unregister");
        }
        tagsDb.addHDict(id.replace("@",""), hDictBuilder.toDict());

        if (tagsDb.idMap.get(id) != null)
        {
            tagsDb.updateIdMap.put(id, tagsDb.idMap.get(id));
        }
       CCUHsApi.getInstance().syncEntityTree();
    }


    public void unRegisterCCU(String ccuName, String installerEmail, String ahuRef, String managerEmail)
    {
        Log.d("CCU_HS","updateCCUahuRef "+ahuRef);
        HashMap ccu = read("device and ccu");

        if (ccu.size() == 0) {
            return;
        }
        final String id = ccu.get("id").toString();

        HDictBuilder hDictBuilder = new HDictBuilder();
        hDictBuilder.add("id", HRef.copy(id));
        hDictBuilder.add("ccu");
        hDictBuilder.add("dis", HStr.make(ccuName));
        hDictBuilder.add("fmEmail", HStr.make(managerEmail));
        hDictBuilder.add("installerEmail", HStr.make(installerEmail));
        hDictBuilder.add("siteRef", getSiteId());
        hDictBuilder.add("equipRef", ccu.get("equipRef").toString());
        hDictBuilder.add("createdDate", HDate.make(ccu.get("createdDate").toString()));
        hDictBuilder.add("gatewayRef", ahuRef);
        hDictBuilder.add("ahuRef", ahuRef);
        hDictBuilder.add("device");
        hDictBuilder.add("unregister");
        tagsDb.addHDict(id.replace("@",""), hDictBuilder.toDict());

        if (tagsDb.idMap.get(id) != null)
        {
            tagsDb.updateIdMap.put(id, tagsDb.idMap.get(id));
        }

        CCUHsApi.getInstance().syncEntityTree();
    }

    public void updateCCUahuRef(String ahuRef, String cmDeviceRef) {
        
        Log.d("CCU_HS","updateCCUahuRef "+ahuRef);
        HashMap ccu = read("device and ccu");
        
        if (ccu.size() == 0) {
            return;
        }
        
        String id = ccu.get("id").toString();
    
        HDictBuilder hDictBuilder = new HDictBuilder();
        hDictBuilder.add("id", HRef.copy(id));
        hDictBuilder.add("ccu");
        hDictBuilder.add("dis", HStr.make(ccu.get("dis").toString()));
        hDictBuilder.add("fmEmail", HStr.make(ccu.get("fmEmail").toString()));
        hDictBuilder.add("installerEmail", HStr.make(ccu.get("installerEmail").toString()));
        hDictBuilder.add("siteRef", getSiteId());
        hDictBuilder.add("equipRef", ccu.get("equipRef").toString());
        hDictBuilder.add("createdDate", HDate.make(ccu.get("createdDate").toString()));
        hDictBuilder.add("gatewayRef", ahuRef);
        hDictBuilder.add("ahuRef", ahuRef);
        hDictBuilder.add("deviceRef",cmDeviceRef);
        hDictBuilder.add("device");
        tagsDb.addHDict(id.replace("@",""), hDictBuilder.toDict());
    
        if (tagsDb.idMap.get(id) != null)
        {
            tagsDb.updateIdMap.put(id, tagsDb.idMap.get(id));
        }

    }

    public HRef getSiteId()
    {
        HDict hDict = new HDictBuilder().add("filter", "site").toDict();
        HGrid site  = getHSClient().call("read", HGridBuilder.dictToGrid(hDict));
        return site.row(0).getRef("id");
    }

    public HRef getCcuId() {
        HDict hDict = new HDictBuilder().add("filter", "ccu").toDict();
        HGrid site  = getHSClient().call("read", HGridBuilder.dictToGrid(hDict));
        return site.row(0).getRef("id");
    }

    public Schedule getSiteSchedule()
    {
        Schedule schedule = new Schedule.Builder().setHDict(tagsDb.read("schedule")).build();
        return schedule;
    }

    public ArrayList<Schedule> getSystemSchedule(boolean vacation)
    {
        ArrayList<Schedule> schedules = new ArrayList<>();
        String              filter    = null;
        if (!vacation)
            filter = "schedule and building and not vacation";
        else
            filter = "schedule and building and vacation";

        HGrid scheduleHGrid = tagsDb.readAll(filter);
        for (int i = 0; i < scheduleHGrid.numRows(); i++)
        {
            schedules.add(new Schedule.Builder().setHDict(scheduleHGrid.row(i)).build());
        }

        return schedules;
    }
    
    public ArrayList<Schedule> getZoneSchedule(String zoneId, boolean vacation)
    {
        ArrayList<Schedule> schedules = new ArrayList<>();
        String              filter    = null;
        if (!vacation)
            filter = "schedule and zone and not vacation and roomRef == "+zoneId;
        else
            filter = "schedule and zone and vacation and roomRef == "+zoneId;
        
        Log.d("CCU_HS"," getZoneSchedule : "+filter);
        if(filter != null) {

            HGrid scheduleHGrid = tagsDb.readAll(filter);

            for (int i = 0; i < scheduleHGrid.numRows(); i++) {
                schedules.add(new Schedule.Builder().setHDict(scheduleHGrid.row(i)).build());
            }
        }
        
        return schedules;
    }

    public void addSchedule(String localId, HDict scheduleDict)
    {
        tagsDb.addHDict(localId, scheduleDict);
    }
    
    public void updateSchedule(String localId, HDict scheduleDict)
    {
        addSchedule(localId, scheduleDict);
        
        Log.i("CCH_HS", "updateScheduleDict: " + scheduleDict.toZinc());
        if (tagsDb.idMap.get("@" +localId) != null)
        {
            tagsDb.updateIdMap.put("@" + localId, tagsDb.idMap.get("@" + localId));
        }
    }
    
    public void updateSchedule(Schedule schedule)
    {
        addSchedule(schedule.getId(), schedule.getScheduleHDict());

        Log.i("CCH_HS", "updateSchedule: " + schedule.getScheduleHDict().toZinc());
        if (tagsDb.idMap.get("@" +schedule.getId()) != null)
        {
            tagsDb.updateIdMap.put("@" + schedule.getId(), tagsDb.idMap.get("@" + schedule.getId()));
        }
    }
    
    public void updateZoneSchedule(Schedule schedule, String zoneId)
    {
        
        addSchedule(schedule.getId(), schedule.getZoneScheduleHDict(zoneId));
        
        Log.i("CCU_HS", "updateZoneSchedule: " + schedule.getZoneScheduleHDict(zoneId).toZinc());
        if (tagsDb.idMap.get("@" +schedule.getId()) != null)
        {
            tagsDb.updateIdMap.put("@" + schedule.getId(), tagsDb.idMap.get("@" + schedule.getId()));
        }
    }
    
    public void updateScheduleNoSync(Schedule schedule, String zoneId) {
        addSchedule(schedule.getId(), (zoneId == null ? schedule.getScheduleHDict() : schedule.getZoneScheduleHDict(zoneId)));
        Log.i("CCU_HS", "updateScheduleNoSync: "+schedule.getId()+" " + (zoneId == null ? schedule.getScheduleHDict().toZinc(): schedule.getZoneScheduleHDict(zoneId).toZinc()));
    }
    
    public Schedule getScheduleById(String scheduleRef)
    {
        if (scheduleRef == null)
            return null;
        
        HDict hDict = tagsDb.readById(HRef.copy(scheduleRef));
        //Log.d("CCU_HS", " getScheduleById " +hDict.toZinc() );
        return new Schedule.Builder().setHDict(hDict).build();
    }

    public void loadTagsData(Context c)
    {
        tagsDb.init(c);
    }
    
    public double getPredictedPreconRate(String ahuRef) {
        HClient hClient   = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
    
        try
        {
            HDict hDict = new HDictBuilder().add("filter", "equip and virtual and ahuRef == " + getGUID(ahuRef)).toDict();
            HGrid virtualEquip = hClient.call("read", HGridBuilder.dictToGrid(hDict));
            if (virtualEquip != null && virtualEquip.numRows() > 0)
            {
                HDict pDict = new HDictBuilder().add("filter", "point and predicted and rate and equipRef == " + virtualEquip.row(0).get("id").toString()).toDict();
                HGrid preconPoint = hClient.call("read", HGridBuilder.dictToGrid(pDict));
                if (preconPoint != null && preconPoint.numRows() > 0)
                {
                    HGrid hisGrid = hClient.hisRead(HRef.copy(preconPoint.row(0).get("id").toString()), "today");
                    if (hisGrid != null && hisGrid.numRows() > 0)
                    {
                        HRow r = hisGrid.row(hisGrid.numRows() - 1);
                        HDateTime date = (HDateTime) r.get("ts");
                        double preconVal = Double.parseDouble(r.get("val").toString());
                        Log.d("CCU_HS", "RemotePreconRate , " + date + " : " + preconVal);
                        return preconVal;
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
            Log.d("CCU_HS","getPredictedPreconRate Failed : Fall back to default precon rate");
        }
        
        return 0;
    }

    public double getExternalTemp() {
    
        HClient hClient = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        if (tempWeatherRef == null)
        {
            HDict navIdDict = new HDictBuilder().add(HayStackConstants.ID, HRef.copy(getGUID(getSiteId().toString()))).toDict();
            HGrid hGrid = HGridBuilder.dictToGrid(navIdDict);
            HGrid site = hClient.call(HStdOps.read.name(), hGrid);
            HDict tDict = new HDictBuilder().add("filter", "weatherPoint and air and temp and weatherRef == " + site.row(0).get("weatherRef")).toDict();
            HGrid weatherPoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
            weatherPoint.dump();
            if (weatherPoint != null && weatherPoint.numRows() > 0)
            {
                tempWeatherRef = weatherPoint.row(0).getRef("id");
            }
        }
        HGrid hisGrid = hClient.hisRead(tempWeatherRef, "current");
        hisGrid.dump();
        if (hisGrid != null && hisGrid.numRows() > 0)
        {
            HRow r = hisGrid.row(hisGrid.numRows() - 1);
            HDateTime date = (HDateTime) r.get("ts");
            double tempVal = Double.parseDouble(r.get("val").toString());
            Log.d("CCU_OAO",date+" External Temp: "+tempVal);
            return tempVal;
        
        }
        return 0;
    }
    
    //In percentage
    public double getExternalHumidity() {
        
        HClient hClient = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        if (humidityWeatherRef == null)
        {
            HDict navIdDict = new HDictBuilder().add(HayStackConstants.ID, HRef.copy(getGUID(getSiteId().toString()))).toDict();
            HGrid hGrid = HGridBuilder.dictToGrid(navIdDict);
            HGrid site = hClient.call(HStdOps.read.name(), hGrid);
            HDict tDict = new HDictBuilder().add("filter", "weatherPoint and humidity and weatherRef == " + site.row(0).get("weatherRef")).toDict();
            HGrid weatherPoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
            weatherPoint.dump();
            if (weatherPoint != null && weatherPoint.numRows() > 0)
            {
                humidityWeatherRef = weatherPoint.row(0).getRef("id");
            }
        }
        HGrid hisGrid = hClient.hisRead(humidityWeatherRef, "current");
        hisGrid.dump();
        if (hisGrid != null && hisGrid.numRows() > 0)
        {
            HRow r = hisGrid.row(hisGrid.numRows() - 1);
            HDateTime date = (HDateTime) r.get("ts");
            double humidityVal = Double.parseDouble(r.get("val").toString());
            Log.d("CCU_OAO",date+" External Humidity: "+humidityVal);
            return 100 * humidityVal;
        
        }
        return 0;
    }
    
    /**
     *  Get N Number of most recent his entries
     * @param id
     * @param offset
     * @param limit
     * @return
     */
    public List<HisItem> getHisItems(String id, int offset, int limit) {
        return tagsDb.getHisItems(HRef.copy(id), offset, limit);
    }
    
    public void deletePointArray(String id) {
        tagsDb.deletePointArray(HRef.copy(id));
    }
    
    public void deleteHistory() {
        ArrayList<HashMap> points = readAll("point and his");
        if (points.size() == 0) {
            return;
        }
        for (Map m : points)
        {
            tagsDb.removeAllHisItems(HRef.copy(m.get("id").toString()));
        }

        //tagsDb.dropDbAndUpdate();
    }
    
    public ConcurrentHashMap<String, String> getIDMap() {
        return tagsDb.idMap ;
    }

  public boolean isCCURegistered(){
      SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(cxt);

      return spDefaultPrefs.getBoolean("registered", false);
  }
    public boolean isNetworkConnected() {

        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(cxt);

        return spDefaultPrefs.getBoolean("75fNetworkAvailable", false);
    }

}
