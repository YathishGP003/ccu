package a75f.io.api.haystack;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import org.projecthaystack.HBin;
import org.projecthaystack.HBool;
import org.projecthaystack.HCoord;
import org.projecthaystack.HDate;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDateTimeRange;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HHisItem;
import org.projecthaystack.HList;
import org.projecthaystack.HMarker;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.HStr;
import org.projecthaystack.HTime;
import org.projecthaystack.HUri;
import org.projecthaystack.HVal;
import org.projecthaystack.HWatch;
import org.projecthaystack.MapImpl;
import org.projecthaystack.server.HOp;
import org.projecthaystack.server.HServer;
import org.projecthaystack.server.HStdOps;

import java.io.File;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.DebugFlags;
import io.objectbox.query.QueryBuilder;

/**
 * Created by samjithsadasivan on 8/31/18.
 */

public class CCUTagsDb extends HServer {

    private static final String PREFS_TAGS_DB = "ccu_tags";
    private static final String PREFS_TAGS_MAP = "tagsMap";
    private static final String PREFS_TAGS_WA = "writeArrayMap";
    private static final String PREFS_ID_MAP = "idMap";
    private static final String PREFS_REMOVE_ID_MAP = "removeIdMap";
    private static final String PREFS_UPDATE_ID_MAP = "updateIdMap";

    public ConcurrentHashMap<String, HDict> tagsMap;
    public ConcurrentHashMap<String, WriteArray>      writeArrays;

    public boolean unitTestMode = false;

    private Context appContext = null;

    public String tagsString;
    public String waString;

    private BoxStore boxStore;
    private Box<HisItem> hisBox;
    private static final File TEST_DIRECTORY = new File("objectbox-example/test-db");

    public ConcurrentHashMap<String, String> idMap;
    public String idMapString;

    public ConcurrentHashMap<String, String> removeIdMap;
    public String removeIdMapString;

    public ConcurrentHashMap<String, String> updateIdMap;
    public String updateIdMapString;

    private class TimeZoneInstanceCreator implements InstanceCreator<TimeZone> {
        public TimeZone createInstance(Type type) {
            return TimeZone.getDefault();
        }
    }

    //public String tagsString = null;
    RuntimeTypeAdapterFactory<HVal> hsTypeAdapter =
            RuntimeTypeAdapterFactory.of(TimeZone.class)
                    .of(HVal.class)
                    .registerSubtype(HBin.class)
                    .registerSubtype(HBool.class)
                    .registerSubtype(HCoord.class)
                    .registerSubtype(HDate.class)
                    .registerSubtype(HDict.class)
                    .registerSubtype(HGrid.class)
                    .registerSubtype(HHisItem.class)
                    .registerSubtype(HList.class)
                    .registerSubtype(HMarker.class)
                    .registerSubtype(HNum.class)
                    .registerSubtype(HRef.class)
                    .registerSubtype(HRow.class)
                    .registerSubtype(HStr.class)
                    .registerSubtype(HTime.class)
                    .registerSubtype(HUri.class)
                    .registerSubtype(MapImpl.class)
                    .registerSubtype(HDateTime.class);

    public CCUTagsDb() {

    }

    public void init(Context c) {
        appContext = c;

        tagsString = appContext.getSharedPreferences(PREFS_TAGS_DB, Context.MODE_PRIVATE).getString(PREFS_TAGS_MAP, null);
        waString = appContext.getSharedPreferences(PREFS_TAGS_DB, Context.MODE_PRIVATE).getString(PREFS_TAGS_WA, null);
        idMapString = appContext.getSharedPreferences(PREFS_TAGS_DB, Context.MODE_PRIVATE).getString(PREFS_ID_MAP, null);
        removeIdMapString = appContext.getSharedPreferences(PREFS_TAGS_DB, Context.MODE_PRIVATE).getString(PREFS_REMOVE_ID_MAP, null);
        updateIdMapString = appContext.getSharedPreferences(PREFS_TAGS_DB, Context.MODE_PRIVATE).getString(PREFS_UPDATE_ID_MAP, null);

        boxStore = MyObjectBox.builder().androidContext(appContext).build();
        hisBox = boxStore.boxFor(HisItem.class);

        if (tagsString == null) {
            tagsMap = new ConcurrentHashMap<>();
            writeArrays = new ConcurrentHashMap();
            idMap = new ConcurrentHashMap();
            removeIdMap = new ConcurrentHashMap();
            updateIdMap = new ConcurrentHashMap();

        } else {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapterFactory(hsTypeAdapter).registerTypeAdapter(TimeZone.class, new TimeZoneInstanceCreator())
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
            Type listType = new TypeToken<ConcurrentHashMap<String, MapImpl<String, HVal>>>() {
            }.getType();
            tagsMap = gson.fromJson(tagsString, listType);
            Type waType = new TypeToken<ConcurrentHashMap<String, WriteArray>>() {
            }.getType();
            writeArrays = gson.fromJson(waString, waType);
            idMap = gson.fromJson(idMapString, ConcurrentHashMap.class);
            removeIdMap = gson.fromJson(removeIdMapString, ConcurrentHashMap.class);
            updateIdMap = gson.fromJson(updateIdMapString, ConcurrentHashMap.class);
        }
    }




    public void saveTags() {

        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(hsTypeAdapter)
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        Type listType = new TypeToken<Map<String, MapImpl<String, HVal>>>() {
        }.getType();
        tagsString = gson.toJson(tagsMap, listType);
        appContext.getSharedPreferences(PREFS_TAGS_DB, Context.MODE_PRIVATE).edit().putString(PREFS_TAGS_MAP, tagsString).apply();

        Type waType = new TypeToken<Map<String, WriteArray>>() {
        }.getType();
        waString = gson.toJson(writeArrays, waType);
        appContext.getSharedPreferences(PREFS_TAGS_DB, Context.MODE_PRIVATE).edit().putString(PREFS_TAGS_WA, waString).apply();

        idMapString = gson.toJson(idMap);
        appContext.getSharedPreferences(PREFS_TAGS_DB, Context.MODE_PRIVATE).edit().putString(PREFS_ID_MAP, idMapString).apply();

        removeIdMapString = gson.toJson(removeIdMap);
        appContext.getSharedPreferences(PREFS_TAGS_DB, Context.MODE_PRIVATE).edit().putString(PREFS_REMOVE_ID_MAP, removeIdMapString).apply();

        updateIdMapString = gson.toJson(updateIdMap);
        appContext.getSharedPreferences(PREFS_TAGS_DB, Context.MODE_PRIVATE).edit().putString(PREFS_UPDATE_ID_MAP, updateIdMapString).apply();

    }

    //TODO- TEMP for Unit testing
    public Map getDbMap() {
        return tagsMap;
    }

    public void setTagsDbMap() {
        tagsMap = new ConcurrentHashMap<>();
    }

    public void init() {

        if (tagsString == null) {
            tagsMap = new ConcurrentHashMap();
            writeArrays = new ConcurrentHashMap();
            idMap = new ConcurrentHashMap();
            removeIdMap = new ConcurrentHashMap();
            updateIdMap = new ConcurrentHashMap();
        } else
        {
            Gson gson = new GsonBuilder().registerTypeAdapterFactory(hsTypeAdapter).setPrettyPrinting().disableHtmlEscaping().create();
            Type listType = new TypeToken<ConcurrentHashMap<String, MapImpl<String, HVal>>>() {
            }.getType();
            tagsMap = gson.fromJson(tagsString, listType);
            Type waType = new TypeToken<ConcurrentHashMap<String, WriteArray>>() {
            }.getType();
            writeArrays = gson.fromJson(waString, waType);
            idMap = gson.fromJson(idMapString, ConcurrentHashMap.class);
            removeIdMap = gson.fromJson(removeIdMapString, ConcurrentHashMap.class);
            updateIdMap = gson.fromJson(updateIdMapString, ConcurrentHashMap.class);
        }
    
        BoxStore.deleteAllFiles(TEST_DIRECTORY);
        boxStore = MyObjectBox.builder()
                              // add directory flag to change where ObjectBox puts its database files
                              .directory(TEST_DIRECTORY)
                              // optional: add debug flags for more detailed ObjectBox log output
                              .debugFlags(DebugFlags.LOG_QUERIES | DebugFlags.LOG_QUERY_PARAMETERS).build();
        hisBox = boxStore.boxFor(HisItem.class);
    }


    public void saveString() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(hsTypeAdapter)
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        Type listType = new TypeToken<Map<String, MapImpl<String, HVal>>>() {
        }.getType();
        tagsString = gson.toJson(tagsMap, listType);

        Type waType = new TypeToken<Map<String, WriteArray>>() {
        }.getType();
        waString = gson.toJson(writeArrays, waType);
        idMapString = gson.toJson(idMap);
        removeIdMapString = gson.toJson(removeIdMap);
        removeIdMapString = gson.toJson(updateIdMap);
    }


    public void addHGrid(HGrid hGrid) {
        for (int i = 0; i < hGrid.numRows(); i++) {
            HRef ref = hGrid.row(i).getRef("id");
            System.out.println("Ref: " + ref.val);

            tagsMap.put(ref.val, new HDictBuilder().add(hGrid.row(i)).toDict());
        }
    }

    public HDict addSite(String dis, String geoCity, String geoState, String timeZone, int area) {
        HDict site = new HDictBuilder()
                .add("id", HRef.make(dis))
                .add("dis", dis)
                .add("site", HMarker.VAL)
                .add("geoCity", geoCity)
                .add("geoState", geoState)
                .add("geoAddr", "" + geoCity + "," + geoState)
                .add("tz", timeZone)
                .add("area", HNum.make(area, "ft\u00B2"))
                .toDict();
        tagsMap.put(dis, site);
        return site;
    }

    public HDict getSite(String ref) {
        return (HDict) tagsMap.get(ref);
    }


    public String addSite(Site s) {
        HDictBuilder site = new HDictBuilder()
                .add("id", HRef.make(UUID.randomUUID().toString()))
                .add("dis", s.getDisplayName())
                .add("site", HMarker.VAL)
                .add("geoCity", s.getGeoCity())
                .add("geoState", s.getGeoState())
                .add("geoAddr", "" + s.getGeoCity() + "," + s.getGeoState())
                .add("tz", s.getTz())
                .add("area", HNum.make(s.getArea(), "ft\u00B2"));

        for (String m : s.getMarkers()) {
            site.add(m);
        }

        HRef id = (HRef) site.get("id");
        tagsMap.put(id.toVal(), site.toDict());
        return id.toVal();
    }

    public void updateSite(Site s, String i) {
        HDictBuilder site = new HDictBuilder()
                .add("id", HRef.copy(i))
                .add("dis", s.getDisplayName())
                .add("site", HMarker.VAL)
                .add("geoCity", s.getGeoCity())
                .add("geoState", s.getGeoState())
                .add("geoAddr", "" + s.getGeoCity() + "," + s.getGeoState())
                .add("tz", s.getTz())
                .add("area", HNum.make(s.getArea(), "ft\u00B2"));

        for (String m : s.getMarkers()) {
            site.add(m);
        }

        HRef id = (HRef) site.get("id");
        tagsMap.put(id.toVal(), site.toDict());
    }

    public void log() {
        int i = 0;
        for (Iterator it = iterator(); it.hasNext(); ) {
            i++;
            HDict rec = (HDict) it.next();
            System.out.println("Rec " + i + ":" + rec.toString());
        }
    }


    public String addEquip(Equip q) {
        HDictBuilder equip = new HDictBuilder()
                                     .add("id",      HRef.make(UUID.randomUUID().toString()))
                                     .add("dis",     q.getDisplayName())
                                     .add("equip",     HMarker.VAL)
                                     .add("siteRef", q.getSiteRef())
                                     .add("zoneRef",  q.getZoneRef() != null ? q.getZoneRef() : "SYSTEM")
                                     .add("floorRef", q.getFloorRef() != null ? q.getFloorRef() : "SYSTEM")
                                     .add("profile", q.getProfile())
                                     .add("priority", q.getPriority())
                                     .add("tz",q.getTz())
                                     .add("group",q.getGroup());
        for (String m : q.getMarkers()) {
            equip.add(m);
        }
        HRef id = (HRef) equip.get("id");
        tagsMap.put(id.toVal(), equip.toDict());
        return id.toCode();
    }

    public void updateEquip(Equip q, String i) {
        HDictBuilder equip = new HDictBuilder()
                                     .add("id",      HRef.copy(i))
                                     .add("dis",     q.getDisplayName())
                                     .add("equip",     HMarker.VAL)
                                     .add("siteRef", q.getSiteRef())
                                     .add("zoneRef",  q.getZoneRef())
                                     .add("floorRef", q.getFloorRef())
                                     .add("profile", q.getProfile())
                                     .add("priority", q.getPriority())
                                     .add("tz",q.getTz())
                                     .add("group",q.getGroup());
        for (String m : q.getMarkers()) {
            Log.d("CCU"," Add marker "+m);
            equip.add(m);
        }
        HRef id = (HRef) equip.get("id");
        tagsMap.put(id.toVal(), equip.toDict());
    }


    public HDict getEquip(String dis) {
        return (HDict) tagsMap.get(dis);
    }

    public String addPoint(Point p) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(UUID.randomUUID().toString()))
                .add("dis", p.getDisplayName())
                .add("point", HMarker.VAL)
                .add("siteRef", p.getSiteRef())
                .add("equipRef", p.getEquipRef())
                .add("zoneRef", p.getZoneRef() != null ? p.getZoneRef() : "SYSTEM")
                .add("floorRef", p.getFloorRef() != null ? p.getFloorRef() : "SYSTEM")
                .add("group", p.getGroup())
                .add("kind", p.getKind() == null ? "Number" : p.getKind())
                .add("tz", p.getTz());
        if (p.getUnit() != null) b.add("unit", p.getUnit());

        for (String m : p.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
        return id.toCode();
    }

    public void updatePoint(Point p, String i) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.copy(i))
                .add("dis", p.getDisplayName())
                .add("point", HMarker.VAL)
                .add("siteRef", p.getSiteRef())
                .add("equipRef", p.getEquipRef())
                .add("zoneRef", p.getZoneRef())
                .add("floorRef", p.getFloorRef())
                .add("group", p.getGroup())
                .add("kind", p.getKind() == null ? "Number" : p.getKind())
                .add("tz", p.getTz());
        if (p.getUnit() != null) b.add("unit", p.getUnit());

        for (String m : p.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }

    public String addPoint(RawPoint p) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(UUID.randomUUID().toString()))
                .add("dis", p.getDisplayName())
                .add("point", HMarker.VAL)
                .add("physical", HMarker.VAL)
                .add("deviceRef", p.getDeviceRef())
                .add("siteRef", p.getSiteRef())
                .add("pointRef", p.getPointRef())
                .add("port", p.getPort())
                .add("type", p.getType())
                .add("kind", p.getKind() == null ? "Number" : p.getKind())
                .add("enabled",p.getEnabled() ? "true":"false")
                .add("tz", p.getTz());
        if (p.getUnit() != null) b.add("unit", p.getUnit());
        
        for (String m : p.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
        return id.toCode();
    }

    public void updatePoint(RawPoint p, String i) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.copy(i))
                .add("dis", p.getDisplayName())
                .add("point", HMarker.VAL)
                .add("physical", HMarker.VAL)
                .add("deviceRef", p.getDeviceRef())
                .add("siteRef", p.getSiteRef())
                .add("pointRef", p.getPointRef())
                .add("port", p.getPort())
                .add("type", p.getType())
                .add("kind", p.getKind() == null ? "Number" : p.getKind())
                .add("enabled",p.getEnabled() ? "true":"false")
                .add("tz", p.getTz());
        if (p.getUnit() != null) b.add("unit", p.getUnit());
        for (String m : p.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }
    
    public String addPoint(SettingPoint p) {
        HDictBuilder b = new HDictBuilder()
                                 .add("id", HRef.make(UUID.randomUUID().toString()))
                                 .add("dis", p.getDisplayName())
                                 .add("point", HMarker.VAL)
                                 .add("setting", HMarker.VAL)
                                 .add("deviceRef", p.getDeviceRef())
                                 .add("siteRef", p.getSiteRef())
                                 .add("val", p.getVal())
                                 .add("kind", p.getKind() == null ? "Number" : p.getKind());
                
        if (p.getUnit() != null) b.add("unit", p.getUnit());
        
        for (String m : p.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
        return id.toCode();
    }
    
    public void updatePoint(SettingPoint p, String i) {
        HDictBuilder b = new HDictBuilder()
                                 .add("id", HRef.copy(i))
                                 .add("dis", p.getDisplayName())
                                 .add("point", HMarker.VAL)
                                 .add("setting", HMarker.VAL)
                                 .add("deviceRef", p.getDeviceRef())
                                 .add("siteRef", p.getSiteRef())
                                 .add("val", p.getVal())
                                 .add("kind", p.getKind() == null ? "Number" : p.getKind());
        
        if (p.getUnit() != null) b.add("unit", p.getUnit());
        
        for (String m : p.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }
    
    public String addDevice(Device d) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(UUID.randomUUID().toString()))
                .add("dis", d.getDisplayName())
                .add("device", HMarker.VAL)
                .add("his", HMarker.VAL)
                .add("addr", d.getAddr())
                .add("siteRef", d.getSiteRef())
                .add("equipRef", d.getEquipRef())
                .add("zoneRef", d.getZoneRef())
                .add("floorRef", d.getFloorRef());

        for (String m : d.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
        return id.toCode();
    }

    public void updateDevice(Device d, String i) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.copy(i))
                .add("dis", d.getDisplayName())
                .add("device", HMarker.VAL)
                .add("his", HMarker.VAL)
                .add("addr", d.getAddr())
                .add("siteRef", d.getSiteRef())
                .add("equipRef", d.getEquipRef())
                .add("zoneRef", d.getZoneRef())
                .add("floorRef", d.getFloorRef());

        for (String m : d.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }

    public String addFloor(Floor f) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(UUID.randomUUID().toString()))
                .add("dis", f.getDisplayName())
                .add("floor", HMarker.VAL)
                .add("siteRef", f.getSiteRef());

        for (String m : f.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
        return id.toCode();
    }

    public void updateFloor(Floor f, String i) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.copy(i))
                .add("dis", f.getDisplayName())
                .add("floor", HMarker.VAL)
                .add("siteRef", f.getSiteRef());

        for (String m : f.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }

    public String addZone(Zone z) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(UUID.randomUUID().toString()))
                .add("dis", z.getDisplayName())
                .add("room", HMarker.VAL)
                .add("floorRef", z.getFloorRef());

        for (String m : z.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
        return id.toCode();
    }

    public void updateZone(Zone z, String i) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.copy(i))
                .add("dis", z.getDisplayName())
                .add("room", HMarker.VAL)
                .add("floorRef", z.getFloorRef());

        for (String m : z.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }


    //////////////////////////////////////////////////////////////////////////
    // Ops
    //////////////////////////////////////////////////////////////////////////

    public HOp[] ops() {
        return new HOp[]{
                HStdOps.about,
                HStdOps.ops,
                HStdOps.formats,
                HStdOps.read,
                HStdOps.nav,
                HStdOps.pointWrite,
                HStdOps.hisWrite,
                HStdOps.hisRead,
                HStdOps.invokeAction,
        };
    }

    public HDict onAbout() {
        return about;
    }

    private final HDict about = new HDictBuilder()
            .add("serverName", hostName())
            .add("vendorName", "Haystack Java Toolkit")
            .add("vendorUri", HUri.make("http://project-haystack.org/"))
            .add("productName", "Haystack Java Toolkit")
            .add("productVersion", "2.0.0")
            .add("productUri", HUri.make("http://project-haystack.org/"))
            .toDict();

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Reads
    //////////////////////////////////////////////////////////////////////////

    protected HDict onReadById(HRef id) {
        return (HDict) tagsMap.get(id.val);
    }

    protected Iterator iterator() {
        return tagsMap.values().iterator();
    }

    //////////////////////////////////////////////////////////////////////////
    // Navigation
    //////////////////////////////////////////////////////////////////////////

    protected HGrid onNav(String navId) {
        // test database navId is record id
        HDict base = null;
        if (navId != null) base = readById(HRef.make(navId));

        // map base record to site, equip, or point
        String filter = "site";
        if (base != null) {
            if (base.has("site")) filter = "equip and siteRef==\"" + base.id().toCode() + "\"";
            else if (base.has("equip"))
                filter = "point and equipRef==\"" + base.id().toCode() + "\"";
            else filter = "navNoChildren";
        }

        // read children of base record
        HGrid grid = readAll(filter);

        // add navId column to results
        HDict[] rows = new HDict[grid.numRows()];
        Iterator it = grid.iterator();
        for (int i = 0; it.hasNext(); ) rows[i++] = (HDict) it.next();
        for (int i = 0; i < rows.length; ++i)
            rows[i] = new HDictBuilder().add(rows[i]).add("navId", rows[i].id().val).toDict();
        return HGridBuilder.dictsToGrid(rows);
    }

    protected HDict onNavReadByUri(HUri uri) {
        return null;
    }

    //////////////////////////////////////////////////////////////////////////
    // Watches
    //////////////////////////////////////////////////////////////////////////

    protected HWatch onWatchOpen(String dis, HNum lease) {
        throw new UnsupportedOperationException();
    }

    protected HWatch[] onWatches() {
        throw new UnsupportedOperationException();
    }

    protected HWatch onWatch(String id) {
        throw new UnsupportedOperationException();
    }

    //////////////////////////////////////////////////////////////////////////
    // Point Write
    //////////////////////////////////////////////////////////////////////////

    protected HGrid onPointWriteArray(HDict rec) {
        CCUTagsDb.WriteArray array = (CCUTagsDb.WriteArray) writeArrays.get(rec.id().toVal());
        if (array == null) array = new CCUTagsDb.WriteArray();

        HGridBuilder b = new HGridBuilder();
        b.addCol("level");
        b.addCol("levelDis");
        b.addCol("val");
        b.addCol("who");

        for (int i = 0; i < 17; ++i)
            b.addRow(new HVal[]{
                    HNum.make(i + 1),
                    HStr.make("" + (i + 1)),
                    array.val[i],
                    HStr.make(array.who[i]),
            });
        return b.toGrid();
    }

    protected void onPointWrite(HDict rec, int level, HVal val, String who, HNum dur, HDict opts) {
        System.out.println("onPointWrite: " + rec.dis() + "  " + val + " @ " + level + " [" + who + "]");
        CCUTagsDb.WriteArray array = (CCUTagsDb.WriteArray) writeArrays.get(rec.id());
        if (array == null) writeArrays.put(rec.id().toVal(), array = new CCUTagsDb.WriteArray());
        array.val[level - 1] = val;
        array.who[level - 1] = who;
    }

    public HDict getConfig() {
        if (!tagsMap.containsKey("config")) {
            HDict hDict = new HDictBuilder().add("nosync").add("localconfig").toDict();
            tagsMap.put("config", hDict);
        }
        return tagsMap.get("config");
    }

    public HDict updateConfig(String propertyName, HVal hVal) {
        HDict config = getConfig();
        HDict hDict = new HDictBuilder().add(config).add(propertyName, hVal).toDict();
        tagsMap.put("config", hDict);
        return hDict;
    }

    public void addHDict(String localId, HDict hDict) {
        tagsMap.put(localId, hDict);
    }

    static class WriteArray {
        final HVal[] val = new HVal[17];
        final String[] who = new String[17];
    }

    //////////////////////////////////////////////////////////////////////////
    // History
    //////////////////////////////////////////////////////////////////////////

    public HHisItem[] onHisRead(HDict entity, HDateTimeRange range) {
        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, entity.get("id").toString())
                .greater(HisItem_.date, range.start.millis())
                .less(HisItem_.date, range.end.millis())
                .order(HisItem_.date);

        List<HisItem> hisList = hisQuery.build().find();

        boolean isBool = ((HStr) entity.get("kind")).val.equals("Bool");
        ArrayList acc = new ArrayList();
        for (HisItem item : hisList) {
            HVal val = isBool ? HBool.make(item.val > 0) : HNum.make(item.val);
            HDict hsItem = HHisItem.make(HDateTime.make(item.getDate().getTime()), val);
            if (item.getDate().getTime() != range.start.millis()) {
                acc.add(hsItem);
            }
        }
        return (HHisItem[]) acc.toArray(new HHisItem[acc.size()]);
    }

    public HHisItem[] onHisRead(HDict entity) {
        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, entity.get("id").toString())
                .order(HisItem_.date, QueryBuilder.DESCENDING);

        HisItem item = hisQuery.build().findFirst();

        boolean isBool = ((HStr) entity.get("kind")).val.equals("Bool");
        ArrayList acc = new ArrayList();

        HVal val = isBool ? HBool.make(item.val > 0) : HNum.make(item.val);
        HDict hsItem = HHisItem.make(HDateTime.make(item.getDate().getTime()), val);
        acc.add(hsItem);
        return (HHisItem[]) acc.toArray(new HHisItem[acc.size()]);
    }

    public void onHisWrite(HDict rec, HHisItem[] items) {
        for (HHisItem item : items) {
            HisItem hisItem = new HisItem();
            hisItem.setDate(new Date(item.ts.millis()));
            hisItem.setRec(rec.get("id").toString());
            hisItem.setVal(Double.parseDouble(item.val.toString()));
            hisItem.setSyncStatus(false);
            hisBox.put(hisItem);
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Actions
    //////////////////////////////////////////////////////////////////////////

    public HGrid onInvokeAction(HDict rec, String action, HDict args) {
        System.out.println("-- invokeAction \"" + rec.dis() + "." + action + "\" " + args);
        return HGrid.EMPTY;
    }

    public List<HisItem> getUnSyncedHisItems(HRef id) {

        HDict entity = readById(id);

        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, entity.get("id").toString())
                .equal(HisItem_.syncStatus, false)
                //.greater(HisItem_.date,range.start.millis())
                //.less(HisItem_.date,range.end.millis())
                .order(HisItem_.date);

        return hisQuery.build().find();
    }

    public void setHisItemSyncStatus(ArrayList<HisItem> hisItems) {
        for (HisItem item : hisItems) {
            hisBox.put(item);
        }
    }

    public List<HisItem> getAllHisItems(HRef id) {

        HDict entity = readById(id);

        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, entity.get("id").toString())
                .order(HisItem_.date);

        return hisQuery.build().find();
    }
}
