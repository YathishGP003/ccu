package a75f.io.api.haystack;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;
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

import javax.annotation.Nonnull;

import a75f.io.api.haystack.util.DbStrings;
import a75f.io.api.haystack.util.Migrations;
import a75f.io.logger.CcuLog;
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
    private static final String TAG_CCU_HS = "CCU_HS";
    private static final String PREFS_HAS_MIGRATED_GUID = "hasMigratedGuid";
    
    private static final long MAX_DB_SIZE_IN_KB = 5 * 1024 * 1024;
    
    public ConcurrentHashMap<String, HDict> tagsMap;
    public ConcurrentHashMap<String, WriteArray>      writeArrays;

    private Context appContext = null;

    public String tagsString;
    public String waString;

    private BoxStore boxStore;
    private Box<HisItem> hisBox;
    private static final File TEST_DIRECTORY = new File("objectbox-test/tags-db");

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

        if(boxStore != null && !boxStore.isClosed())
        {
            boxStore.close();
        }
        boxStore = MyObjectBox.builder()
                              .androidContext(appContext)
                              .maxSizeInKByte(MAX_DB_SIZE_IN_KB)
                              .build();
        hisBox = boxStore.boxFor(HisItem.class);

        if (tagsString == null) {
            tagsMap = new ConcurrentHashMap<>();
            writeArrays = new ConcurrentHashMap();
            idMap = new ConcurrentHashMap();
            removeIdMap = new ConcurrentHashMap();
            updateIdMap = new ConcurrentHashMap();

            CcuLog.d("CCU_HS", "New CCU.  No migration needed");
            markGuidMigrationComplete();

        } else {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapterFactory(hsTypeAdapter).registerTypeAdapter(TimeZone.class, new TimeZoneInstanceCreator())
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();

            // Start migration to GUID if that hasn't happened yet
            DbStrings migrationResult = checkAndStartGuidMigration(gson);

            tagsMap = new ConcurrentHashMap<String, HDict>();
            loadGrid(tagsString);
            Type waType = new TypeToken<ConcurrentHashMap<String, WriteArray>>() {
            }.getType();
            writeArrays = gson.fromJson(waString, waType);
            idMap = gson.fromJson(idMapString, ConcurrentHashMap.class);
            removeIdMap = gson.fromJson(removeIdMapString, ConcurrentHashMap.class);
            updateIdMap = gson.fromJson(updateIdMapString, ConcurrentHashMap.class);

            // Finish the migration (if applicable)
            if (migrationResult != null) {
                finishGuidMigration(migrationResult);
            }

            CcuLog.d("CCU_HS", "checking id map for matching l to g ids");
            int matchCount = 0;
            for (String key : idMap.keySet()) {
                if (key.equals(idMap.get(key))) matchCount++;
                else {
                    CcuLog.d("CCU_HS", "Not a match: luid="+key + ", guid="+idMap.get(key));
                    CcuLog.d("CCU_HS", "entity is: " + tagsMap.get(key));
                }
            }
            CcuLog.d("CCU_HS", "Match check: " + matchCount + " match out of " + idMap.size());

        }
    }

    private DbStrings checkAndStartGuidMigration(Gson gson) {
        boolean hasMigratedGuid = PreferenceManager.getDefaultSharedPreferences(appContext)
                                                     .getBoolean(PREFS_HAS_MIGRATED_GUID, false);
        if (!hasMigratedGuid) {

            DbStrings result = Migrations.migrateGuidsToLuids(
                    new DbStrings(idMapString, removeIdMapString, updateIdMapString, tagsString, waString, null),
                    gson
            );
            
            idMapString = result.getIdMapStr();
            removeIdMapString = result.getRemoveIdMapStr();
            updateIdMapString = result.getUpdateIdMapStr();
            tagsString = result.getTagsStr();
            waString = result.getWaStr();

            return result;
        } else {
            CcuLog.i("CCU_HS", "Already migrated Guids");
            return null;
        }
    }

    private void finishGuidMigration(@Nonnull DbStrings migrationResult) {

        Migrations.migrateHisData(this, migrationResult.getMigrationMap());

        saveTags();
        markGuidMigrationComplete();
    }

    private void markGuidMigrationComplete() {
        PreferenceManager.getDefaultSharedPreferences(appContext).edit()
                         .putBoolean(PREFS_HAS_MIGRATED_GUID, true)
                         .apply();
    }
    
    public BoxStore getBoxStore() {
        return boxStore;
    }

    // These three methods created for Testing, since we can't generally mock fields, only methods
    // And, it's good Java practice.

    public ConcurrentHashMap<String, HDict> getTagsMap() {
        return tagsMap;
    }

    public ConcurrentHashMap<String, String> getIdMap() {
        return idMap;
    }

    public ConcurrentHashMap<String, String> getUpdateIdMap() {
        return updateIdMap;
    }

    private void loadGrid(String tagsString) {

        HZincReader hZincReader = new HZincReader(tagsString);
        HGrid hGrid = hZincReader.readGrid();
        hGrid.dump();

        for(int i = 0; i < hGrid.numRows(); i++)
        {
            HRow val = hGrid.row(i);
            CcuLog.d(TAG_CCU_HS, "Zinc: " + val.toZinc());
            if(!val.has("nosync")) {
                String key = val.get("id").toString().replace("@", "");
                tagsMap.put(key, val);
            }
        }
    }

    public void saveTags() {
        saveTags(false);
    }

    @SuppressLint("ApplySharedPref")
    public void saveTags(boolean persistImmediate) {

        SharedPreferences.Editor sharedPrefsEditor = appContext.getSharedPreferences(
                PREFS_TAGS_DB, Context.MODE_PRIVATE).edit();

        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(hsTypeAdapter)
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        tagsString = HZincWriter.gridToString(getGridTagsMap());
        sharedPrefsEditor.putString(PREFS_TAGS_MAP, tagsString);

        Type waType = new TypeToken<Map<String, WriteArray>>() {
        }.getType();
        waString = gson.toJson(writeArrays, waType);
        sharedPrefsEditor.putString(PREFS_TAGS_WA, waString);

        idMapString = gson.toJson(idMap);
        sharedPrefsEditor.putString(PREFS_ID_MAP, idMapString);

        removeIdMapString = gson.toJson(removeIdMap);
        sharedPrefsEditor.putString(PREFS_REMOVE_ID_MAP, removeIdMapString);

        updateIdMapString = gson.toJson(updateIdMap);
        sharedPrefsEditor.putString(PREFS_UPDATE_ID_MAP, updateIdMapString);

        if (persistImmediate) {
            sharedPrefsEditor.commit();
        } else {
            sharedPrefsEditor.apply();
        }
    }

    private HGrid getGridTagsMap() {
        HDict[] hDicts = tagsMap.values().toArray(new HDict[tagsMap.size()]);
        HGrid hGrid = HGridBuilder.dictsToGrid(hDicts);
        return hGrid;
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
    
        if (boxStore == null)
        {
            BoxStore.deleteAllFiles(TEST_DIRECTORY);
            boxStore = MyObjectBox.builder()
                                  // add directory flag to change where ObjectBox puts its database files
                                  .directory(TEST_DIRECTORY)
                                  // optional: add debug flags for more detailed ObjectBox log output
                                  .debugFlags(DebugFlags.LOG_QUERIES | DebugFlags.LOG_QUERY_PARAMETERS).build();
            hisBox = boxStore.boxFor(HisItem.class);
        }
    }

    public HDict addSite(String dis, String geoCity, String geoState, String timeZone, int area, String org, String fcManager,String installer) {
        HDict site = new HDictBuilder()
                .add("id", HRef.make(dis))
                .add("dis", dis)
                .add("site", HMarker.VAL)
                .add("geoCity", geoCity)
                .add("geoState", geoState)
                .add("geoAddr", "" + geoCity + "," + geoState)
                .add("tz", timeZone)
                .add("area", HNum.make(area, "ft\u00B2"))
                .add("organization", org)
                .add("fmEmail", fcManager)
                .add("installerEmail", installer)
                .toDict();
        tagsMap.put(dis, site);
        return site;
    }

    public String addSite(Site s) {
        return addSiteWithId(s, UUID.randomUUID().toString());
    }

    public String addSiteWithId(Site s, String id) {
        HDictBuilder site = new HDictBuilder()
                .add("id", HRef.make(id))
                .add("dis", s.getDisplayName())
                .add("site", HMarker.VAL)
                .add("geoCity", s.getGeoCity())
                .add("geoState", s.getGeoState())
                .add("geoCountry", s.getGeoCountry())
                .add("geoPostalCode", s.getGeoPostalCode())
                .add("geoAddr", "" + s.getGeoAddress())
                .add("tz", s.getTz())
                .add("organization", s.getOrganization())
                .add("fmEmail", s.getFcManagerEmail())
                .add("installerEmail", s.getInstallerEmail())
                .add("area", HNum.make(s.getArea(), "ft\u00B2"));

        for (String m : s.getMarkers()) {
            site.add(m);
        }

        HRef ref = (HRef) site.get("id");
        tagsMap.put(ref.toVal(), site.toDict());
        return ref.toVal();         
    }

    public void updateSite(Site s, String i) {
        HDictBuilder site = new HDictBuilder()
                .add("id", HRef.copy(i))
                .add("dis", s.getDisplayName())
                .add("site", HMarker.VAL)
                .add("geoCity", s.getGeoCity())
                .add("geoState", s.getGeoState())
                .add("geoCountry", s.getGeoCountry())
                .add("geoPostalCode", s.getGeoPostalCode())
                .add("geoAddr", "" + s.getGeoAddress())
                .add("tz", s.getTz())
                .add("organization", s.getOrganization())
                .add("fmEmail", s.getFcManagerEmail())
                .add("installerEmail", s.getInstallerEmail())
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
            CcuLog.d(TAG_CCU_HS,"Rec " + i + ":" + rec.toString());
        }
    }



    public String addEquip(Equip q) {
        return addEquipWithId(q, UUID.randomUUID().toString());
    }

    public String addEquipWithId(Equip q, String id) {
        HDictBuilder equip = new HDictBuilder()
                                     .add("id",      HRef.make(id))
                                     .add("dis",     q.getDisplayName())
                                     .add("equip",     HMarker.VAL)
                                     .add("siteRef", q.getSiteRef())
                                     .add("roomRef",  q.getRoomRef() != null ? q.getRoomRef() : "SYSTEM")
                                     .add("floorRef", q.getFloorRef() != null ? q.getFloorRef() : "SYSTEM")
                                     .add("profile", q.getProfile())
                                     .add("priorityLevel", q.getPriority())
                                     .add("tz",q.getTz())
                                     .add("group",q.getGroup());
        if (q.getAhuRef() != null) {
            equip.add("ahuRef",q.getAhuRef());
        }
        if(q.getGatewayRef() != null){
            equip.add("gatewayRef",q.getGatewayRef());
        }
        if(q.getVendor() != null){
            equip.add("vendor",q.getVendor());
        }
        if(q.getModel() != null){
            equip.add("model",q.getModel());
        }

        for (String m : q.getMarkers()) {
            equip.add(m);
        }
        HRef ref = (HRef) equip.get("id");
        tagsMap.put(ref.toVal(), equip.toDict());
        return ref.toCode();
    }

    public void updateEquip(Equip q, String i) {
        HDictBuilder equip = new HDictBuilder()
                                     .add("id",      HRef.copy(i))
                                     .add("dis",     q.getDisplayName())
                                     .add("equip",     HMarker.VAL)
                                     .add("siteRef", q.getSiteRef())
                                     .add("roomRef",  q.getRoomRef())
                                     .add("floorRef", q.getFloorRef())
                                     .add("profile", q.getProfile())
                                     .add("priorityLevel", q.getPriority())
                                     .add("tz",q.getTz())
                                     .add("group",q.getGroup());
    
        if (q.getAhuRef() != null) {
            equip.add("ahuRef",q.getAhuRef());
        }
		if(q.getGatewayRef() != null){
            equip.add("gatewayRef",q.getGatewayRef());
        }
        if(q.getVendor() != null){
            equip.add("vendor",q.getVendor());
        }
        if(q.getModel() != null){
            equip.add("model",q.getModel());
        }
        for (String m : q.getMarkers()) {
            equip.add(m);
        }
        HRef id = (HRef) equip.get("id");
        tagsMap.put(id.toVal(), equip.toDict());
    }


    public HDict getEquip(String dis) {
        return (HDict) tagsMap.get(dis);
    }

    public String addPoint(Point p) {
        return addPointWithId(p, UUID.randomUUID().toString());
    }

    public String addPointWithId(Point p, String id) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(id))
                .add("dis", p.getDisplayName())
                .add("point", HMarker.VAL)
                .add("siteRef", p.getSiteRef())
                .add("equipRef", p.getEquipRef())
                .add("roomRef", p.getRoomRef() != null ? p.getRoomRef() : "SYSTEM")
                .add("floorRef", p.getFloorRef() != null ? p.getFloorRef() : "SYSTEM")
                .add("group", p.getGroup())
                .add("kind", p.getKind() == null ? "Number" : p.getKind())
                .add("tz", p.getTz());
        if (p.getUnit() != null) b.add("unit", p.getUnit());
        if (p.getEnums() != null) b.add("enum", p.getEnums());
        if (p.getMinVal() != null) b.add("minVal",Double.parseDouble(p.getMinVal()));
        if (p.getMaxVal() != null) b.add("maxVal",Double.parseDouble(p.getMaxVal()));
        if (p.getIncrementVal() != null) b.add("incrementVal",Double.parseDouble(p.getIncrementVal()));
        if (p.getTunerGroup() != null) b.add("tunerGroup",p.getTunerGroup());
        if (p.getHisInterpolate() != null) b.add("hisInterpolate",p.getHisInterpolate());
        if (p.getShortDis() != null) b.add("shortDis",p.getShortDis());

        for (String m : p.getMarkers()) {
            b.add(m);
        }
        HRef ref = (HRef) b.get("id");
        tagsMap.put(ref.toVal(), b.toDict());
        return ref.toCode();
    }

    public void updatePoint(Point p, String i) {
        HDictBuilder b = new HDictBuilder()
                             .add("id", HRef.copy(i))
                             .add("dis", p.getDisplayName())
                             .add("point", HMarker.VAL)
                             .add("siteRef", p.getSiteRef())
                             .add("equipRef", p.getEquipRef())
                             .add("roomRef", p.getRoomRef() != null ? p.getRoomRef() : "SYSTEM")
                             .add("floorRef", p.getFloorRef() != null ? p.getFloorRef() : "SYSTEM")
                             .add("group", p.getGroup())
                             .add("kind", p.getKind() == null ? "Number" : p.getKind())
                             .add("tz", p.getTz());
        if (p.getUnit() != null) b.add("unit", p.getUnit());
        if (p.getEnums() != null) b.add("enum", p.getEnums());
        if (p.getMinVal() != null) b.add("minVal",Double.parseDouble(p.getMinVal()));
        if (p.getMaxVal() != null) b.add("maxVal",Double.parseDouble(p.getMaxVal()));
        if (p.getIncrementVal() != null) b.add("incrementVal",Double.parseDouble(p.getIncrementVal()));
        if (p.getTunerGroup() != null) b.add("tunerGroup",p.getTunerGroup());
        if (p.getHisInterpolate() != null) b.add("hisInterpolate",p.getHisInterpolate());
        if (p.getShortDis() != null) b.add("shortDis",p.getShortDis());

        for (String m : p.getMarkers()) {
            b.add(m);
        }

        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }

    public String addPoint(RawPoint p) {
        return addPointWithId(p, UUID.randomUUID().toString());
    }

    public String addPointWithId(RawPoint p, String id) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(id))
                .add("dis", p.getDisplayName())
                .add("point", HMarker.VAL)
                .add("physical", HMarker.VAL)
                .add("deviceRef", p.getDeviceRef())
                .add("siteRef", p.getSiteRef())
                .add("pointRef", p.getPointRef())
                .add("port", p.getPort())
                .add("analogType", p.getType())
                .add("kind", p.getKind() == null ? "Number" : p.getKind())
                .add("portEnabled",p.getEnabled() ? HBool.TRUE : HBool.FALSE)
                .add("tz", p.getTz());
        if (p.getUnit() != null) b.add("unit", p.getUnit());
        if (p.getShortDis() != null) b.add("shortDis",p.getShortDis());
        if (p.getMinVal() != null) b.add("minVal",Double.parseDouble(p.getMinVal()));
        if (p.getMaxVal() != null) b.add("maxVal",Double.parseDouble(p.getMaxVal()));
        
        if (p.getRegisterAddress() != null) b.add("registerAddress", p.getRegisterAddress());
        if (p.getRegisterNumber() != null) b.add("registerNumber", p.getRegisterNumber());
        if (p.getStartBit() != null) b.add("startBit", p.getStartBit());
        if (p.getEndBit() != null) b.add("endBit", p.getEndBit());
        if (p.getRegisterType() != null) b.add("registerType", p.getRegisterType());
        if (p.getParameterId() != null) b.add("parameterId", p.getParameterId());

        for (String m : p.getMarkers()) {
            b.add(m);
        }
        HRef ref = (HRef) b.get("id");
        tagsMap.put(ref.toVal(), b.toDict());
        return ref.toCode();
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
                .add("analogType", p.getType())
                .add("kind", p.getKind() == null ? "Number" : p.getKind())
                .add("portEnabled",p.getEnabled() ? HBool.TRUE : HBool.FALSE)
                .add("tz", p.getTz());
        if (p.getUnit() != null) b.add("unit", p.getUnit());
        if (p.getShortDis() != null) b.add("shortDis",p.getShortDis());
        if (p.getMinVal() != null) b.add("minVal",Double.parseDouble(p.getMinVal()));
        if (p.getMaxVal() != null) b.add("maxVal",Double.parseDouble(p.getMaxVal()));
    
        if (p.getRegisterAddress() != null) b.add("registerAddress", p.getRegisterAddress());
        if (p.getRegisterNumber() != null) b.add("registerNumber", p.getRegisterNumber());
        if (p.getStartBit() != null) b.add("startBit", p.getStartBit());
        if (p.getEndBit() != null) b.add("endBit", p.getEndBit());
        if (p.getRegisterType() != null) b.add("registerType", p.getRegisterType());
        if (p.getParameterId() != null) b.add("parameterId", p.getParameterId());
        for (String m : p.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }
    
    public String addPoint(SettingPoint p) {
        return addPointWithId(p, UUID.randomUUID().toString());
    }

    public String addPointWithId(SettingPoint p, String id) {
        HDictBuilder b = new HDictBuilder()
                                 .add("id", HRef.make(id))
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
        HRef ref = (HRef) b.get("id");
        tagsMap.put(ref.toVal(), b.toDict());
        return ref.toCode();
    }

    public String updateSettingPoint(SettingPoint p, String i) {
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
        return id.toCode();
    }

    public String addDevice(Device d) {
        return addDeviceWithId(d, UUID.randomUUID().toString());
    }

    public String addDeviceWithId(Device d, String id) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(id))
                .add("dis", d.getDisplayName())
                .add("device", HMarker.VAL)
                .add("addr", d.getAddr())
                .add("siteRef", d.getSiteRef())
                .add("equipRef", d.getEquipRef())
                .add("roomRef", d.getRoomRef() != null ? d.getRoomRef() : "SYSTEM")
                .add("floorRef", d.getFloorRef() != null ? d.getFloorRef() : "SYSTEM");

        for (String m : d.getMarkers()) {
            b.add(m);
        }
        HRef ref = (HRef) b.get("id");
        tagsMap.put(ref.toVal(), b.toDict());
        return ref.toCode();
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
                .add("roomRef", d.getRoomRef())
                .add("floorRef", d.getFloorRef());

        for (String m : d.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }

    public String addFloor(Floor f) {
        return addFloorWithId(f, UUID.randomUUID().toString());
    }

    public String addFloorWithId(Floor f, String id) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(id))
                .add("dis", f.getDisplayName())
                .add("floor", HMarker.VAL)
                .add("siteRef", f.getSiteRef())
                .add("orientation", 0.0)
                .add("floorNum", 0.0);

        for (String m : f.getMarkers()) {
            b.add(m);
        }
        HRef ref = (HRef) b.get("id");
        tagsMap.put(ref.toVal(), b.toDict());
        return ref.toCode();
    }

    public void updateFloor(Floor f, String i) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.copy(i))
                .add("dis", f.getDisplayName())
                .add("floor", HMarker.VAL)
                .add("siteRef", f.getSiteRef())
                .add("orientation", f.getOrientation())
                .add("floorNum", f.getFloorNum());

        for (String m : f.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }

    public String addZone(Zone z) {
        return addZoneWithId(z, UUID.randomUUID().toString());
    }

    public String addZoneWithId(Zone z, String id) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(id))
                .add("dis", z.getDisplayName())
                .add("room", HMarker.VAL)
                .add("siteRef", z.getSiteRef())
                .add("floorRef", z.getFloorRef());

        for (String m : z.getMarkers()) {
            b.add(m);
        }
        HRef ref = (HRef) b.get("id");
        tagsMap.put(ref.toVal(), b.toDict());
        return ref.toCode();
    }
    
    public void updateZone(Zone z, String i) {
        HDictBuilder b = new HDictBuilder()
                                 .add("id", HRef.copy(i))
                                 .add("dis", z.getDisplayName())
                                 .add("room", HMarker.VAL)
                                 .add("siteRef", z.getSiteRef())
                                 .add("floorRef", z.getFloorRef());
        if (z.getScheduleRef() != null) {
            b.add("scheduleRef", z.getScheduleRef());
        }
        
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
        b.addCol("duration");
        
        
        for (int i = 0; i < 17; ++i) {
            
            if (array.duration[i] != 0 && array.duration[i] < System.currentTimeMillis()) {
                array.val[i] = null;
                array.who[i] = null;
                array.duration[i] = 0;
            }
            b.addRow(new HVal[]{
                    HNum.make(i + 1),
                    HStr.make("" + (i + 1)),
                    array.val[i],
                    HStr.make(array.who[i]),
                    HNum.make(array.duration[i] >  System.currentTimeMillis() ? array.duration[i] : 0)
            });
            }
        return b.toGrid();
    }

    protected void onPointWrite(HDict rec, int level, HVal val, String who, HNum dur, HDict opts) {
        CcuLog.d(TAG_CCU_HS,"onPointWrite: " + rec.dis() + "  " + val + " @ " + level + " [" + who + "]"+", duration: "+dur.millis());
        CCUTagsDb.WriteArray array = (CCUTagsDb.WriteArray) writeArrays.get(rec.id().toVal());
        if (array == null) writeArrays.put(rec.id().toVal(), array = new CCUTagsDb.WriteArray());
        array.val[level - 1] = val;
        array.who[level - 1] = who;
        array.duration[level-1] = dur.val > 0 ? System.currentTimeMillis() + dur.millis() : 0;
    }

    public void deletePointArray(HRef id) {
        writeArrays.remove(id.toVal());
    }
    
    public void deletePointArrayLevel(HRef id, int level) {
        CCUTagsDb.WriteArray array = (CCUTagsDb.WriteArray) writeArrays.get(id.toVal());
        if (array != null) {
            array.val[level - 1] = null;
            array.who[level - 1] = null;
            array.duration[level-1] = 0;
        } else {
            CcuLog.d(TAG_CCU_HS," Invalid point array delete command "+id);
        }
        
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
        final long[] duration = new long[17];
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
        saveHisItemsToCache(rec,items, false);
    }

    public void saveHisItemsToCache (HDict rec, HHisItem[] items, boolean syncItems) {
        for (HHisItem item : items) {
            HisItem hisItem = new HisItem();
            hisItem.setDate(new Date(item.ts.millis()));
            hisItem.setRec(rec.get("id").toString());
            hisItem.setVal(Double.parseDouble(item.val.toString()));
            hisItem.setSyncStatus(syncItems);
            CcuLog.d(TAG_CCU_HS,"Write historized value to local DB for point ID " + rec.get("id").toString() + "; description " + rec.get("dis").toString() + "; value "  + item.val.toString());
            hisBox.put(hisItem);
            HisItemCache.getInstance().add(rec.get("id").toString(), hisItem);
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Actions
    //////////////////////////////////////////////////////////////////////////

    public HGrid onInvokeAction(HDict rec, String action, HDict args) {
        CcuLog.d(TAG_CCU_HS,"-- invokeAction \"" + rec.dis() + "." + action + "\" " + args);
        return HGrid.EMPTY;
    }

    public List<HisItem> getUnsyncedHisItemsOrderDesc(String pointId) {
        List<HisItem> validHisItems = new ArrayList<>();

        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, pointId)
                .equal(HisItem_.syncStatus, false)
                .orderDesc(HisItem_.date);
        List<HisItem> hisItems = hisQuery.build().find();
        // TODO Matt Rudd - This shouldn't be necessary, but I was seeing null items in the collection; need to investigate
        for (HisItem hisItem : hisItems) {
            if  (hisItem != null) {
                validHisItems.add(hisItem);
            }
        }
        CcuLog.d(TAG_CCU_HS, "Finding unsynced items for point ID " + pointId+" size: "+validHisItems.size());
        return validHisItems;
    }
    
    public HisItem getLastHisItem(HRef id) {
        
        HisItem retVal = HisItemCache.getInstance().get(id.toString());
        if (retVal == null) {
            retVal = hisBox.query().equal(HisItem_.rec, id.toString())
                           .orderDesc(HisItem_.date).build().findFirst();

            if (retVal != null) {
                HisItemCache.getInstance().add(id.toString(), retVal);
            }
        }
        return retVal;
    }

    public void updateHisItemSynced(List<HisItem> hisItems) {
        for (HisItem item : hisItems) {
            item.setSyncStatus(true);
            hisBox.put(item);
        }
    }

    public void updateHisItems(List<HisItem> hisItems) {
        for (HisItem item : hisItems) {
            hisBox.put(item);
        }
    }

    public List<HisItem> getHisItemsForMigration(HRef id, int offset, int limit) {

        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, id.toString())
                .orderDesc(HisItem_.date);
        return hisQuery.build().find(offset, limit);
    }

    public List<HisItem> getHisItems(HRef id, int offset, int limit) {
        
        HDict entity = readById(id);
        
        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, entity.get("id").toString())
                .orderDesc(HisItem_.date);
        return hisQuery.build().find(offset, limit);
    }
    
    //Delete all the hisItem entries older than 24 hrs.
    public void removeExpiredHisItems(HRef id) {
        HDict entity = readById(id);
    
        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, entity.get("id").toString())
                .less(HisItem_.date, System.currentTimeMillis() - 24*60*60*1000)
                //.or()
                //.equal(HisItem_.syncStatus, true)
                .order(HisItem_.date);
        //Leave one hisItem to make sure his data is not empty if there was no more recent entries
        List<HisItem>  hisItems = hisQuery.build().find();
        if (hisItems.size() > 1)
        {
            hisItems.remove(hisItems.size() - 1);
            hisBox.remove(hisItems);
        }
    }
    
    public void removeAllHisItems(HRef id) {
        HDict entity = readById(id);

        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, entity.get("id").toString())
                .order(HisItem_.date);
        List<HisItem>  hisItems = hisQuery.build().find();
    
        //Leave one hisItem to make sure his data is not empty if there was no more recent entries
        if (hisItems.size() > 1)
        {
            hisItems.remove(hisItems.size() - 1);
            hisBox.remove(hisItems);
        }
    }
}
