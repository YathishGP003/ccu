package a75f.io.api.haystack;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.projecthaystack.HDate;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HHisItem;
import org.projecthaystack.HList;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import a75f.io.api.haystack.sync.EntityParser;
import a75f.io.api.haystack.sync.EntitySyncHandler;
import a75f.io.api.haystack.sync.HisSyncHandler;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.api.haystack.util.MigrateToStr;
import a75f.io.constants.CcuFieldConstants;
import a75f.io.constants.HttpConstants;
import a75f.io.logger.CcuLog;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

public class CCUHsApi
{
    
    public static final String TAG = CCUHsApi.class.getSimpleName();

    public static boolean CACHED_HIS_QUERY = false ;
    private static CCUHsApi instance;
    private static final String PREFS_HAS_MIGRATED_TO_SILO = "hasMigratedToSilo";

    public AndroidHSClient hsClient;
    public CCUTagsDb       tagsDb;

    public EntitySyncHandler entitySyncHandler;
    public HisSyncHandler    hisSyncHandler;

    public boolean testHarnessEnabled = false;
    
    Context context;
    
    private String hayStackUrl = null;
    private String careTakerUrl = null;
    
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

    public CCUHsApi(Context c, String hayStackUrl, String careTakerUrl)
    {
        if (instance != null)
        {
            throw new IllegalStateException("Api instance already created , use getInstance()");
        }
        context = c;
        this.hayStackUrl = hayStackUrl;
        this.careTakerUrl = careTakerUrl;
        hsClient = new AndroidHSClient();
        tagsDb = (CCUTagsDb) hsClient.db();
        tagsDb.init(context);
        instance = this;
        entitySyncHandler = new EntitySyncHandler(PreferenceManager.getDefaultSharedPreferences(context));
        hisSyncHandler = new HisSyncHandler(this);
        checkSiloMigration(c);                  // remove after all sites migrated, post Jan 20 2021
    }

    // Check whether we've migrated kind: "string" to kind: "Str".  If not, run the migration.
    private void checkSiloMigration(Context context) {
        boolean hasMigratedToSilo = PreferenceManager.getDefaultSharedPreferences(context)
                                             .getBoolean(PREFS_HAS_MIGRATED_TO_SILO, false);
        if (!hasMigratedToSilo) {

            CcuLog.i("CCU_HS", "Migrating tags database to Silo.");

            MigrateToStr.migrateTagsDb(tagsDb);

            syncEntityTree();       // used during dev; can remove otherwise if desired.
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                      .putBoolean(PREFS_HAS_MIGRATED_TO_SILO, true)
                      .apply();
        } else {
            CcuLog.i("CCU_HS", "Already migrated tags database to Silo!");
        }
    }

    //For Unit test
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
        entitySyncHandler = new EntitySyncHandler(null);   // null prefs.
        hisSyncHandler = new HisSyncHandler(this);
    }

    public void resetBaseUrls(String hayStackUrl, String careTakerUrl) {
        this.hayStackUrl = hayStackUrl;
        this.careTakerUrl = careTakerUrl;
    }

    public HClient getHSClient()
    {
        return hsClient;
    }
    
    public String getHSUrl() {
        Log.d("Haystack URL: ","url="+hayStackUrl);
        return hayStackUrl;
    }

    public String hisWriteManyToHaystackService(HDict hisWriteMetadata, HDict[] hisWritePoints) {

        HGrid hisWriteRequest = HGridBuilder.dictsToGrid(hisWriteMetadata, hisWritePoints);

        return HttpUtil.executePost(
                CCUHsApi.getInstance().getHSUrl() + "hisWriteMany/",
                HZincWriter.gridToString(hisWriteRequest),
                CCUHsApi.getInstance().getJwt()
        );
    }

    public String getJwt() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("token","");
    }

    public void setJwt(String jwtToken) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("token", jwtToken);
        editor.commit();
    }

    public String getTunerVersion() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("tunerVersion","");
    }

    public void setTunerVersion(String version) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("tunerVersion", version);
        editor.apply();
    }

    public String getAuthenticationUrl() {
        Log.d("Authentication URL: ","url="+careTakerUrl);
        return careTakerUrl;
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

    public String updateSettingPoint(SettingPoint p, String id)
    {
        return tagsDb.updateSettingPoint(p,id);
    }

    public String addDevice(Device d)
    {
        return tagsDb.addDevice(d);
    }
    public void updateDevice(Device d, String id)
    {
        tagsDb.updateDevice(d, id);
        if (tagsDb.idMap.get(id) != null)
        {
            tagsDb.updateIdMap.put(id, tagsDb.idMap.get(id));
        }
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
        entitySyncHandler.requestSiteSync();
    }
    
    //Local site entity may be updated on pubnub notification which does not need to be synced back.
    public void updateSiteLocal(Site s, String id)
    {
        tagsDb.updateSite(s, id);
    }

    public void updateEquip(Equip q, String id)
    {
        tagsDb.updateEquip(q, id);
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

    public void updatePoint(Point point, String id)
    {
        tagsDb.updatePoint(point, id);
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
                    HRow r   = (HRow) it.next();
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

    /**
     * Normally, we use Hashmap from our query but this was needed for proper serialization by
     * EquipSyncAdapter#syncSystemEquip.
     *
     * @param query
     * @return an HDict from a query, or null if the result is empty
     */
    public @Nullable HDict readAsHdict(String query) {
        return hsClient.read(query, false);
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

    /**
     * Write to a 'writable' point
     * The 'who' field will be populated with ccu_#id where #id is the HRef of haystack device entity for ccu.
     */
    public void writePointForCcuUser(String id, int level, Double val, int duration)
    {
        pointWrite(HRef.copy(id), level, getCCUUserName(), HNum.make(val), HNum.make(duration));
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
        pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, getCCUUserName(), HNum.make(val), HNum.make(0));
    }
    
    /**
     * Write local point array without remote sync.
     * @param id
     * @param level
     * @param who
     * @param val
     * @param duration
     */
    public void writePointLocal(String id, int level, String who, Double val, int duration) {
        hsClient.pointWrite(HRef.copy(id), level, who, HNum.make(val), HNum.make(duration));
    }
    
    public void writePointStrValLocal(String id, int level, String who, String val, int duration) {
        hsClient.pointWrite(HRef.copy(id), level, who, HStr.make(val), HNum.make(duration));
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
            pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, getCCUUserName(), HNum.make(val), HNum.make(0));
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
        pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, getCCUUserName(), HStr.make(val), HNum.make(0));
    }

    public void writeDefaultValById(String id, Double val)
    {
        pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, getCCUUserName(), HNum.make(val), HNum.make(0));
    }

    public void writeDefaultValById(String id, String val)
    {
        pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, getCCUUserName(), HStr.make(val), HNum.make(0));
    }

    /**
     * pointWrite method implements the default pointWrite api of haystack library. It only accepts native haystack
     * type arguments.
     * */
    public void pointWriteForCcuUser(HRef id, int level, HVal val, HNum dur) {
        pointWrite(id, level, getCCUUserName(), val, dur);
    }

    public void pointWrite(HRef id, int level, String who, HVal val, HNum dur) {
        hsClient.pointWrite(id, level, who, val, dur);

        if (CCUHsApi.getInstance().isCCURegistered()) {
            String guid = getGUID(id.toString());
            if (guid != null)
            {
                if (dur.unit == null) {
                    dur = HNum.make(dur.val ,"ms");
                }

                HDictBuilder b = new HDictBuilder().add("id", HRef.copy(guid)).add("level", level).add("who", who).add("val", val).add("duration", dur);
                HDict[] dictArr  = {b.toDict()};
                CcuLog.d("CCU_HS", "PointWrite- "+id+" : "+val);
                HttpUtil.executePostAsync(pointWriteTarget(), HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
            }
        }
    }
    
    public void clearPointArrayLevel(String id, int level, boolean local) {
        CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(id), level,
                                                        CCUHsApi.getInstance().getCCUUserName(),
                                                        HNum.make(0), HNum.make(1));
        if (!local) {
            HDictBuilder b = new HDictBuilder()
                                 .add("id", HRef.copy(CCUHsApi.getInstance().getGUID(id)))
                                 .add("level", level)
                                 .add("who", CCUHsApi.getInstance().getCCUUserName())
                                 .add("duration", HNum.make(0, "ms"))
                                 .add("val", (HVal) null);
            HDict[] dictArr = {b.toDict()};
            HttpUtil.executePost(CCUHsApi.getInstance().pointWriteTarget(), HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
        }
    
    }

    // Feb-08-2021 /pointWrite and /pointWriteMany need to hit silo /v2/.  All other calls needs to stay on v1.
    // todo: This is a temporary workaround.  Resolve this backend.  Do all v1 or all v2, or create two base URLs, one for silo-1, one for silo-2
    public String pointWriteTarget() {
        return getHSUrl().replace("v1/", "v2/") + "pointWrite";
    }

    // Feb-08-2021 /pointWrite and /pointWriteMany need to hit silo /v2/.  All other calls needs to stay on v1.
    public String pointWriteManyTarget() {
        return getHSUrl().replace("v1/", "v2/") + "pointWriteMany";
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
                //CcuLog.d("CCU_HS", "" + valMap);
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
            //CcuLog.d("CCU_HS", "" + valMap);
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
    
    public HGrid readPointArrRemote(String id) {
        HDictBuilder b = new HDictBuilder().add("id", HRef.copy(id));
        HDict[] dictArr  = {b.toDict()};
        String response = HttpUtil.executePost(pointWriteTarget(), HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
        CcuLog.d("CCU_HS", "Response : "+response);
      
        return response == null ? null : new HZincReader(response).readGrid();
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
            HashMap point = read(query);
            if (point.isEmpty()) {
                CcuLog.d("CCU_HS","write point id is null : "+query);
                return 0.0;
            }
            String    id     = point.get("id").toString();
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
            HashMap point = read(query);
            if (point.isEmpty()) {
                CcuLog.d("CCU_HS","write point id is null : "+query);
                return;
            }
            String    id     = point.get("id").toString();
            HisItem previtem = curRead(id);
            Double prevVal = previtem == null ? 0 : previtem.getVal();
            if((previtem == null) || (!previtem.initialized) || !prevVal.equals(val) || query.contains("and diag")) {
                HisItem item = new HisItem(id, new Date(), val);
                hisWrite(item);
            }
        }
    }

    public void deleteEntity(String id) {
        CcuLog.d("CCU_HS", "deleteEntity " + CCUHsApi.getInstance().readMapById(id).toString());
        tagsDb.tagsMap.remove(id.replace("@", ""));
        if (tagsDb.idMap.get(id) != null) {
            String guid = getGUID(id);
            if (guid != null) {
                tagsDb.removeIdMap.put(id, guid);
            }
            tagsDb.idMap.remove(id);
        }
    }

    public void deleteEntityLocally(String id) {
        tagsDb.tagsMap.remove(id.replace("@", ""));
        if (tagsDb.idMap.get(id) != null) {
            tagsDb.idMap.remove(id);
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

    public void deleteWritablePoint(String id) {
        deleteWritableArray(id);
        deleteEntity(id);
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

    /**
     * Get the Global unique ID for a given local Id.
     * @param luid local ID
     * @return the global Id
     */
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
                if (!testHarnessEnabled && !entitySyncHandler.isSyncProgress())
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
        tagsDb.init(context);
        syncEntityWithPointWrite();
    }

    //Reset CCU - Force-writes local entities to the backend.
    public void resetSync() {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground( final Void ... params ) {

                String ccuid = getCcuId().toString();
                String siteId = getSiteId().toString();
                ArrayList<Floor> floors = HSUtil.getFloors();
                ConcurrentHashMap<String, String> removeMap = new ConcurrentHashMap<>();

                // exclude ccu and site for force sync
                for (ConcurrentHashMap.Entry<String, String> pair : tagsDb.idMap.entrySet()){
                    if (ccuid.equals(pair.getKey()) || siteId.equals(pair.getKey())){
                        continue;
                    }

                    removeMap.put(pair.getKey(),pair.getValue());
                }

                // exclude floors for force sync
                for (ConcurrentHashMap.Entry<String,String> map : removeMap.entrySet()){

                    for (Floor f: floors){

                        if (f.getId().equals(map.getKey())){
                            removeMap.remove(f.getId());
                        }
                    }

                    // exclude building schedule for force sync
                    HashMap buildingSchedule = read("schedule and building and not vacation");
                    if (buildingSchedule.get("id").toString().contains(map.getKey())){
                        removeMap.remove(map.getKey());
                    }

                    // exclude building tuners for force sync
                    ArrayList<HashMap> hQList = readAll("equip");
                    for (HashMap h: hQList){
                        Equip equip = new Equip.Builder().setHashMap(h).build();

                        if (equip.getMarkers().contains("tuner") && equip.getId().equals(map.getKey())){
                            removeMap.remove(map.getKey());
                        }
                    }
                }

                // finally clear remaining id's for force sync
                for (ConcurrentHashMap.Entry<String, String> removeKey : removeMap.entrySet()){
                    tagsDb.idMap.remove(removeKey.getKey());
                }

                syncEntityWithPointWrite();

                return null;
            }

            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public void scheduleSync() {
        entitySyncHandler.scheduleSync();
    }

    public void syncHisData()
    {
        if (!entitySyncHandler.isSyncProgress()) {
            hisSyncHandler.syncData();
        } else {
            Log.d("CCU_HS", "EntitySync in progress : Skip HisSync");
        }
    }

    public boolean syncExistingSite(String siteId) {
        siteId = StringUtils.stripStart(siteId,"@");

        if (StringUtils.isBlank(siteId)) {
            return false;
        }

        HGrid remoteSite = getRemoteSite(siteId);

        if (remoteSite == null || remoteSite.isEmpty() || remoteSite.isErr())
        {
            return false;
        }

        EntityParser p = new EntityParser(remoteSite);
        Site s = p.getSite();
        tagsDb.idMap.put("@"+tagsDb.addSite(s), s.getId());
        Log.d("CCU_HS_EXISTINGSITESYNC","Added Site "+s.getId());

        HClient hClient = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);

        //import building schedule data
        importBuildingSchedule(siteId, hClient);

        //import building tuners
        importBuildingTuners(siteId, hClient);

        ArrayList<HashMap> writablePoints = CCUHsApi.getInstance().readAll("point and writable");
        ArrayList<HDict> hDicts = new ArrayList<>();
        for (HashMap m : writablePoints) {
            HDict pid = new HDictBuilder().add("id",HRef.copy(getGUID(m.get("id").toString()))).toDict();
            hDicts.add(pid);
        }

        int partitionSize = 25;
        List<List<HDict>> partitions = new ArrayList<>();
        for (int i = 0; i<hDicts.size(); i += partitionSize) {
            partitions.add(hDicts.subList(i, Math.min(i + partitionSize, hDicts.size())));
        }

        for (List<HDict> sublist : partitions) {
            HGrid wa = hClient.call("pointWriteMany", HGridBuilder.dictsToGrid(sublist.toArray(new HDict[sublist.size()])));
            ArrayList<HDict> hDictList = new ArrayList<>();

            Iterator rowIterator = wa.iterator();
            while (rowIterator.hasNext()) {
                HRow row = (HRow) rowIterator.next();
                String id = row.get("id").toString();
                String kind = row.get("kind").toString();
                HVal data = row.get("data");

                if (data instanceof HList && ((HList) data).size() > 0) {
                    HList dataList = (HList) data;

                    for (int i = 0; i < dataList.size(); i++) {
                        HDict dataElement = (HDict) dataList.get(i);

                        String who = dataElement.getStr("who");
                        String level = dataElement.get("level").toString();
                        HVal val = dataElement.get("val");

                        HDict pid = new HDictBuilder().add("id", HRef.copy(id))
                                .add("level", Integer.parseInt(level))
                                .add("who", who)
                                .add("val", kind.equals(Kind.STRING.getValue()) ? HStr.make(val.toString()) : val).toDict();
                        hDictList.add(pid);

                        //save his data to local cache
                        HDict rec = hsClient.readById(HRef.copy(getLUID(id)));
                        tagsDb.saveHisItemsToCache(rec, new HHisItem[]{HHisItem.make(HDateTime.make(System.currentTimeMillis()), kind.equals(Kind.STRING.getValue()) ? HStr.make(val.toString()) : val)}, true);

                        //save points on tagsDb
                        tagsDb.onPointWrite(rec, Integer.parseInt(level), kind.equals(Kind.STRING.getValue()) ? HStr.make(val.toString()) : val, who, HNum.make(0), rec);

                    }

                }

            }

            HGrid responseGrid = hClient.call("pointWriteMany", HGridBuilder.dictsToGrid(hDictList.toArray(new HDict[hDictList.size()])));
        }
        return true;
    }


    private void importBuildingSchedule(String siteId, HClient hClient){

            try {
                HDict buildingDict = new HDictBuilder().add("filter", "building and schedule and siteRef == " + StringUtils.prependIfMissing(siteId, "@")).toDict();
                HGrid buildingSch = hClient.call("read", HGridBuilder.dictToGrid(buildingDict));

                if (buildingSch == null) {
                    return;
                }

                Iterator it = buildingSch.iterator();
                while (it.hasNext())
                {
                    HRow r = (HRow) it.next();
                    Schedule buildingSchedule =  new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build();

                    String guid = buildingSchedule.getId();
                    buildingSchedule.setmSiteId(CCUHsApi.getInstance().getSiteId().toString());
                    HRef localId = HRef.make(UUID.randomUUID().toString());
                    buildingSchedule.setId(localId.toVal());
                    CCUHsApi.getInstance().addSchedule(localId.toVal(), buildingSchedule.getScheduleHDict());
                    CCUHsApi.getInstance().putUIDMap(localId.toString(), "@" + guid);
                }
            } catch (UnknownRecException e) {
                e.printStackTrace();
            }
    }

    private void importBuildingTuners(String siteId, HClient hClient) {
        CcuLog.i(TAG, " importBuildingTuners");
        ArrayList<Equip> equips = new ArrayList<>();
        ArrayList<Point> points = new ArrayList<>();
        try {
            HDict tunerDict = new HDictBuilder().add("filter", "tuner and siteRef == " + StringUtils.prependIfMissing(siteId, "@")).toDict();
            HGrid tunerGrid = hClient.call("read", HGridBuilder.dictToGrid(tunerDict));
            if (tunerGrid == null) {
                return;
            }
            
            Iterator it = tunerGrid.iterator();
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

                if (map.get("equip") != null) {
                    equips.add(new Equip.Builder().setHashMap(map).build());
                } else if (map.get("point") != null ) {
                    points.add(new Point.Builder().setHashMap(map).build());
                }
            }
        } catch (UnknownRecException e) {
            e.printStackTrace();
        }


        CCUHsApi hsApi = CCUHsApi.getInstance();
        for (Equip q : equips) {
            if (q.getMarkers().contains("tuner"))
            {
                String equipLuid;
                HashMap tunerEquip = read("tuner and equip");
                if (!tunerEquip.isEmpty()) {
                    equipLuid = tunerEquip.get("id").toString();
                } else {
                    q.setSiteRef(hsApi.getSiteId().toString());
                    q.setFloorRef("@SYSTEM");
                    q.setRoomRef("@SYSTEM");
                    equipLuid = hsApi.addEquip(q);
                    hsApi.putUIDMap(equipLuid, q.getId());
                }
                //Points
                for (Point p : points)
                {
                    if (p.getEquipRef().equals(q.getId()))
                    {
                        String guidKey = StringUtils.prependIfMissing(p.getId(), "@");
                        if (getLUID(guidKey) == null) {
                            p.setSiteRef(hsApi.getSiteId().toString());
                            p.setFloorRef("@SYSTEM");
                            p.setRoomRef("@SYSTEM");
                            p.setEquipRef(equipLuid);
                            String pointLuid = hsApi.addPoint(p);
                            hsApi.putUIDMap(pointLuid, p.getId());
                        } else {
                            CcuLog.i(TAG, "Point already imported "+p.getId());
                        }
                        
                    }
                }
            }
        }
        CcuLog.i(TAG," importBuildingTuners Completed");
    }
    
    public void importBuildingTuners() {
        String siteId = getSiteGuid();
        
        if (StringUtils.isBlank(siteId)) {
            CcuLog.e(TAG, " Site ID Invalid : Skip Importing building tuner.");
            return;
        }
        HClient hClient = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        importBuildingTuners(siteId, hClient);
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
    
    public Site getRemoteSiteEntity(String siteGuid) {
        HGrid siteGrid = getRemoteSite(siteGuid);
    
        Iterator it = siteGrid.iterator();
        while (it.hasNext()) {
            HashMap<Object, Object> map = new HashMap<>();
            HRow r = (HRow) it.next();
            HRow.RowIterator ri = (HRow.RowIterator) r.iterator();
            while (ri.hasNext()) {
                HDict.MapEntry m = (HDict.MapEntry) ri.next();
                map.put(m.getKey(), m.getValue());
            }
        
            if (map.get("site") != null) {
                return new Site.Builder().setHashMap(map).build();
            }
        }
        return null;
    }


    public void log()
    {
        CcuLog.d("CCU_HS", "" + tagsDb);
    }

    public HGrid getCCUs()
    {
        HDict hDict = new HDictBuilder().add("filter", "ccu").toDict();
        HGrid ccus  = getHSClient().call("read", HGridBuilder.dictToGrid(hDict));
        return ccus;
    }

    public void addOrUpdateConfigProperty(String ccu, HRef id)
    {
        tagsDb.updateConfig(ccu, id);
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
        tagsDb.addHDict(id.replace("@",""), hDictBuilder.toDict());

        if (tagsDb.idMap.get(id) != null)
        {
            tagsDb.updateIdMap.put(id, tagsDb.idMap.get(id));
        }

        CCUHsApi.getInstance().syncEntityTree();
    }
    public void updateDiagGatewayRef(String systemEquipRef){
        HashMap diag = read("equip and diag");

        if (diag.size() == 0) {
            return;
        }
        Equip q = new Equip.Builder().setHashMap(diag).build();
        q.setGatewayRef(systemEquipRef);
        CCUHsApi.getInstance().updateEquip(q, q.getId());
    }

    public void updateCCUahuRef(String ahuRef) {
        
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
        hDictBuilder.add("device");
        tagsDb.addHDict(id.replace("@",""), hDictBuilder.toDict());
    
        if (tagsDb.idMap.get(id) != null)
        {
            tagsDb.updateIdMap.put(id, tagsDb.idMap.get(id));
        }

    }

    /**
     * This methods returns the Local siteId, not the global siteId.  Also, there is no
     * @ in front of the id here, which you might also want.
     *
     * @return
     */
    public HRef getSiteId()
    {
        HDict hDict = new HDictBuilder().add("filter", "site").toDict();
        HGrid site  = getHSClient().call("read", HGridBuilder.dictToGrid(hDict));
        return site.row(0).getRef("id");
    }
 /**
     * Return the global site id if it exists.
     * Otherwise null.
     * @return site id or null
     */
    @Nullable
    public String getGlobalSiteId() {
        HashMap site = read("site");

        if (site == null || site.get("id") == null) return null;
        String siteLuid = site.get("id").toString();

        return getGUID(siteLuid);
    }

    @Nullable
    public String getGlobalSiteIdNoAtSign() {
        String guid = getGlobalSiteId();
        if (guid == null) return null;

        if (guid.startsWith("@")) {
            return guid.substring(1);
        }
        return guid;
    }


    /**
     * This methods returns the Local ccuId, not the global ccuId.
     */
    public HRef getCcuId() {
        HRef siteRef = null;
        HDict hDict = new HDictBuilder().add("filter", "ccu").toDict();
        HGrid site  = getHSClient().call("read", HGridBuilder.dictToGrid(hDict));
        if (site != null && site.numRows() > 0) {
            siteRef = site.row(0).getRef("id");
        }
        return siteRef;
    }

    public String getCCUGuid() {
        HRef ccuId = getCcuId();
        return ccuId != null ? getGUID(ccuId.toString()) : null;
    }

    /**
     * Return the global ccu id if it exists.
     * Otherwise null.
     * @return ccu id or null
     */
    @Nullable
    public String getGlobalCcuId() {
        HRef ccuId = getCcuId();
        if (ccuId == null) return null;
        else return getGUID(getCcuId().toString());
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
            if (site != null) {
                HVal weatherRef = site.row(0).get("weatherRef", false);
                if (weatherRef != null) {
                    HDict tDict = new HDictBuilder().add("filter", "weatherPoint and air and temp and weatherRef == " + weatherRef).toDict();
                    HGrid weatherPoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));

                    if (weatherPoint != null && weatherPoint.numRows() > 0) {
                        weatherPoint.dump();
                        tempWeatherRef = weatherPoint.row(0).getRef("id");

                        HGrid hisGrid = hClient.hisRead(tempWeatherRef, "current");

                        if (hisGrid != null && hisGrid.numRows() > 0) {
                            hisGrid.dump();
                            HRow r = hisGrid.row(hisGrid.numRows() - 1);
                            HDateTime date = (HDateTime) r.get("ts");
                            double tempVal = Double.parseDouble(r.get("val").toString());
                            Log.d("CCU_OAO",date+" External Temp: "+tempVal);
                            return tempVal;

                        }
                    }
                }
            }
        } else {
            HGrid hisGrid = hClient.hisRead(tempWeatherRef, "current");
            if (hisGrid != null && hisGrid.numRows() > 0) {
                hisGrid.dump();
                HRow r = hisGrid.row(hisGrid.numRows() - 1);
                return Double.parseDouble(r.get("val").toString());
            } else {
                return CCUHsApi.getInstance().readHisValByQuery("system and outside and temp");
            }
        }
        return 0;
    }
    
    public double getExternalHumidity() {
        
        HClient hClient = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        if (humidityWeatherRef == null)
        {
            HDict navIdDict = new HDictBuilder().add(HayStackConstants.ID, HRef.copy(getGUID(getSiteId().toString()))).toDict();
            HGrid hGrid = HGridBuilder.dictToGrid(navIdDict);
            HGrid site = hClient.call(HStdOps.read.name(), hGrid);
            HVal weatherRef = site.row(0).get("weatherRef", false);
            if (weatherRef != null) {
                HDict tDict = new HDictBuilder().add("filter", "weatherPoint and humidity and weatherRef == " + weatherRef).toDict();
                HGrid weatherPoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
                if (weatherPoint != null && weatherPoint.numRows() > 0)
                {
                    weatherPoint.dump();
                    humidityWeatherRef = weatherPoint.row(0).getRef("id");
                    HGrid hisGrid = hClient.hisRead(humidityWeatherRef, "current");
                    hisGrid.dump();
                    if (hisGrid != null && hisGrid.numRows() > 0) {
                        HRow r = hisGrid.row(hisGrid.numRows() - 1);
                        HDateTime date = (HDateTime) r.get("ts");
                        double humidityVal = Double.parseDouble(r.get("val").toString());
                        Log.d("CCU_OAO",date+" External Humidity: "+humidityVal);
                        return 100 * humidityVal;

                    }
                }
            }
        } else {
            HGrid hisGrid = hClient.hisRead(humidityWeatherRef, "current");
            if (hisGrid != null && hisGrid.numRows() > 0) {
                hisGrid.dump();
                HRow r = hisGrid.row(hisGrid.numRows() - 1);
                double humidityVal = Double.parseDouble(r.get("val").toString());
                return 100 * humidityVal;
            } else {
                return CCUHsApi.getInstance().readHisValByQuery("system and outside and humidity");
            }
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
    
    
    public void deletePointArrayLevel(String id, int level) {
        tagsDb.deletePointArrayLevel(HRef.copy(id), level);
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
    }
    
    public boolean isCCURegistered() {
      SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
      return spDefaultPrefs.getBoolean("isCcuRegistered", false);
    }

    public void setCcuRegistered() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isCcuRegistered",true);
        editor.commit();
    }

    public void setCcuUnregistered() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("isCcuRegistered");
        editor.commit();
    }

    public String getSiteGuid() {
        String siteRef = null;

        HashMap site = CCUHsApi.getInstance().read("site");
        String siteLuid = site.get("id").toString();

        if (StringUtils.isNotBlank(siteLuid)) {
            siteRef = CCUHsApi.getInstance().getGUID(siteLuid);
        }

        return siteRef;
    }

    public boolean isNetworkConnected() {

        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        return spDefaultPrefs.getBoolean("75fNetworkAvailable", false);
    }

    public void registerCcu(String installerEmail) {
        registerCcuAsyncTask(installerEmail);
    }

    // TODO Matt Rudd - Too much in this method; need to refactor
    private void registerCcuAsyncTask(final String installerEmail) {
        AsyncTask<Void, Void, Boolean> updateCCUReg = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected synchronized Boolean doInBackground(Void... voids) {
                HashMap site = CCUHsApi.getInstance().read("site");

                boolean ccuSuccessfulRegistration = false;

                Log.d("CCURegInfo","createNewSite Edit backgroundtask");
                String siteGUID = CCUHsApi.getInstance().getGUID(site.get("id").toString());

                if (StringUtils.isNotBlank(siteGUID) && CCUHsApi.getInstance().isNetworkConnected()) {
                    Log.d("CCURegInfo","The CCU is not registered, but the site is created with ID " + siteGUID);
                    HashMap ccu = CCUHsApi.getInstance().read("device and ccu");

                    String ccuLuid = Objects.toString(ccu.get(CcuFieldConstants.ID),"");
                    String ccuGuid = CCUHsApi.getInstance().getGUID(ccuLuid);

                    if (StringUtils.isBlank(ccuGuid)) {
                        Log.d("CCURegInfo","The CCU has a GUID and should be considered registered: " + ccuGuid);
                        String facilityManagerEmail = site.get("fmEmail").toString();
                        String installEmail = installerEmail;
                        if (StringUtils.isBlank(installEmail)) {
                            installEmail = site.get("installerEmail").toString();
                        }
                        String dis = ccu.get("dis").toString();
                        String ahuRef = ccu.get("ahuRef").toString();
                        String gatewayRef = ccu.get("gatewayRef").toString();
                        String equipRef = ccu.get("equipRef").toString();

                        JSONObject ccuRegistrationRequest = getCcuRegisterJson(siteGUID, dis, ahuRef, gatewayRef, equipRef, facilityManagerEmail, installEmail);

                        if (ccuRegistrationRequest != null) {
                            Log.d("CCURegInfo","Sending CCU registration request: " + ccuRegistrationRequest.toString());
                            String ccuRegistrationResponse = HttpUtil.executeJson(
                                    CCUHsApi.getInstance().getAuthenticationUrl()+"devices",
                                    ccuRegistrationRequest.toString(),
                                    BuildConfig.CARETAKER_API_KEY,
                                    true,
                                    HttpConstants.HTTP_METHOD_POST
                            );
                            Log.d("CCURegInfo","Registration response: " + ccuRegistrationResponse);

                            if (ccuRegistrationResponse != null) {
                                try {
                                    JSONObject ccuRegistrationResponseJson = new JSONObject(ccuRegistrationResponse);
                                    ccuGuid = ccuRegistrationResponseJson.getString("id");
                                    String token = ccuRegistrationResponseJson.getString("token");
                                    CCUHsApi.getInstance().putUIDMap(ccuLuid, ccuGuid);
                                    CCUHsApi.getInstance().setJwt(token);
                                    CCUHsApi.getInstance().setCcuRegistered();
                                    ccuSuccessfulRegistration = true;
                                    Log.d("CCURegInfo","CCU was successfully registered with ID " + ccuGuid + "; token " + token);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        Log.d("CCURegInfo","The CCU has a GUID: " + ccuGuid + " and the token is " + CCUHsApi.getInstance().getJwt());
                        // TODO Matt Rudd - Need mechanism to handle the token being null here but the GUID existing; may happen in edge cases
                        CCUHsApi.getInstance().setCcuRegistered();

                        if (StringUtils.isBlank(CCUHsApi.getInstance().getJwt())) {
                            Log.e("CCURegInfo", "There was a fatal error registering the CCU. The GUID is set, but the token is unavailable.");
                        }
                    }
                }
                return ccuSuccessfulRegistration;

            }

            @Override
            protected void onPostExecute(Boolean successfulRegistration) {
                Log.d("CCURegInfo", "createNewSite Edit onPostExecute=" + successfulRegistration);
                if (successfulRegistration) {
                    Toast.makeText(context, "Your CCU was registered successfully", Toast.LENGTH_LONG).show();
                }
            }
        };

        updateCCUReg.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public JSONObject getCcuRegisterJson(
            String siteRef,
            String ccuDescription,
            String ahuRef,
            String gatewayRef,
            String equipRef,
            String facilityManagerEmail,
            String installerEmail) {
        JSONObject ccuJsonRequest = null;

        try {
            ccuJsonRequest = new JSONObject();

            ccuJsonRequest.put(CcuFieldConstants.DESCRIPTION, ccuDescription);
            ccuJsonRequest.put(CcuFieldConstants.SITEREF, siteRef);

            if (StringUtils.isNotBlank(ahuRef)) {
                ccuJsonRequest.put(CcuFieldConstants.AHUREF, ahuRef);
            }

            if (StringUtils.isNotBlank(gatewayRef)) {
                ccuJsonRequest.put(CcuFieldConstants.GATEWAYREF, gatewayRef);
            }

            if (StringUtils.isNotBlank(equipRef)) {
                ccuJsonRequest.put(CcuFieldConstants.EQUIPREF, equipRef);
            }

            ccuJsonRequest.put(CcuFieldConstants.FACILITY_MANAGER_EMAIL, facilityManagerEmail);
            ccuJsonRequest.put(CcuFieldConstants.INSTALLER_EMAIL, installerEmail);

        } catch (JSONException jsonException) {
            ccuJsonRequest = null;
            Log.e("CCURegInfo","Unable to construct a valid CCU registration request", jsonException);
        }

        return ccuJsonRequest;
    }



    /**
     * Returns parameterized HashMaps.
     * Existing readAll() methods returns raw type and generates compiler warnings.
     */
    public ArrayList<HashMap<Object, Object>> readAllEntities(String query)
    {
        ArrayList<HashMap<Object, Object>> rowList = new ArrayList<>();
        try
        {
            HGrid grid = hsClient.readAll(query);
            if (grid != null)
            {
                Iterator it = grid.iterator();
                while (it != null && it.hasNext())
                {
                    HashMap<Object, Object> map = new HashMap<>();
                    HRow r   = (HRow) it.next();
                    HRow.RowIterator        ri  = (HRow.RowIterator) r.iterator();
                    while (ri!= null && ri.hasNext())
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
     * Returns parameterized HashMap.
     * Existing read() method returns raw type and generates compiler warnings.
     */
    public HashMap<Object, Object> readEntity(String query)
    {
        HashMap<Object, Object> map = new HashMap<>();
        try
        {
            HDict    dict = hsClient.read(query, true);
            if (dict != null) {
                Iterator iterator = dict.iterator();
                while (iterator != null && iterator.hasNext()) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    map.put(entry.getKey(), entry.getValue());
                }
            }
        }
        catch (UnknownRecException e)
        {
            e.printStackTrace();
        }
        return map;
    }
    
    //The CCU that creates Site is considered primaryCCU.
    public boolean isPrimaryCcu() {
        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return spDefaultPrefs.getBoolean("isPrimaryCcu", false);
    }
    
    public void setPrimaryCcu(boolean isPrimary) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isPrimaryCcu", isPrimary);
        editor.commit();
    }
    
    public HashSet<String> getSupportedRegions() {
        HashSet<String> regions = new HashSet();
        regions.add("Africa");
        regions.add("America");
        regions.add("Antarctica");
        regions.add("Asia");
        regions.add("Atlantic");
        regions.add("Australia");
        regions.add("Etc");
        regions.add("Europe");
        regions.add("Indian");
        regions.add("Pacific");
        
        return regions;
    }
    
    public void updateTimeZone(String newTz) {
        ArrayList<HashMap<Object, Object>> allPoints = readAllEntities("point");
        for (HashMap<Object, Object> point : allPoints ) {
            if (point.containsKey("physical")) {
                RawPoint updatedPoint = new RawPoint.Builder().setHashMap(point).setTz(newTz).build();
                updatePoint(updatedPoint, updatedPoint.getId());
            } else {
                Point updatedPoint = new Point.Builder().setHashMap(point).setTz(newTz).build();
                updatePoint(updatedPoint, updatedPoint.getId());
            }
            
        }
    
        ArrayList<HashMap<Object, Object>> allEquips = readAllEntities("equip");
        for(HashMap<Object, Object> equip : allEquips) {
            Equip updatedEquip = new Equip.Builder().setHashMap(equip).setTz(newTz).build();
            updateEquip(updatedEquip, updatedEquip.getId());
        }
    }

    public String getCCUUserName() {
        String ccuGuid = getGlobalCcuId();
        return ccuGuid == null ? Tags.CCU : Tags.CCU+"_"+ccuGuid;
    }
    
    public String getTimeZone() {
        HashMap siteMap = read(Tags.SITE);
        return siteMap.get("tz").toString();
    }
}
