package a75f.io.api.haystack;

import static android.widget.Toast.LENGTH_LONG;
import static a75f.io.api.haystack.CCUTagsDb.TAG_CCU_DOMAIN;
import static a75f.io.api.haystack.CCUTagsDb.TAG_CCU_HS;
import static a75f.io.api.haystack.CCUTagsDb.TAG_CCU_OAO;
import static a75f.io.api.haystack.CCUTagsDb.TAG_CCU_ROOM_DB;
import static a75f.io.api.haystack.Tags.DEVICE;
import static a75f.io.api.haystack.Tags.SYSTEM;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.json.JSONException;
import org.json.JSONObject;
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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import a75f.io.api.haystack.exception.NullHGridException;
import a75f.io.api.haystack.schedule.BuildingOccupancy;
import a75f.io.api.haystack.sync.EntityParser;
import a75f.io.api.haystack.sync.EntitySyncResponse;
import a75f.io.api.haystack.sync.HisSyncHandler;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.api.haystack.sync.PointWriteCache;
import a75f.io.api.haystack.sync.SyncManager;
import a75f.io.api.haystack.sync.SyncStatusService;
import a75f.io.api.haystack.util.BackfillUtil;
import a75f.io.api.haystack.util.DatabaseAction;
import a75f.io.api.haystack.util.DatabaseEvent;
import a75f.io.api.haystack.util.JwtValidationException;
import a75f.io.api.haystack.util.JwtValidator;
import a75f.io.api.haystack.util.Migrations;
import a75f.io.constants.CcuFieldConstants;
import a75f.io.constants.HttpConstants;
import a75f.io.data.entities.EntityDBUtilKt;
import a75f.io.logger.CcuLog;
import a75f.io.util.ExecutorTask;


public class CCUHsApi
{

    public interface EntityDeletedListener {
        void onDeviceDeleted(String deviceRef);
    }

    public static final String TAG = CCUHsApi.class.getSimpleName();
    private SharedPreferences defaultSharedPrefs;
    public static boolean CACHED_HIS_QUERY = false ;
    private static CCUHsApi instance;
    private static final String PREFS_HAS_MIGRATED_TO_SILO = "hasMigratedToSilo";
    private static final String INTENT_POINT_DELETED = "a75f.io.renatus.POINT_DELETED";
    private static final String INTENT_ZONE_DELETED = "a75f.io.renatus.ZONE_DELETED";
    public AndroidHSClient hsClient;
    public CCUTagsDb       tagsDb;

    private HisSyncHandler    hisSyncHandler;

    public boolean testHarnessEnabled = false;

    Context context;

    private String hayStackUrl = null;
    private String careTakerUrl = null;
    private String gatewayServiceUrl = null;

    public static HRef tempWeatherRef = null;
    HRef humidityWeatherRef = null;

    private SyncStatusService syncStatusService;
    private SyncManager       syncManager;
    
    private volatile boolean isCcuReady = false;
    private long appAliveMinutes = 0;

    public Boolean isAuthorized = false;

    private static int ccuLogLevel = -1;

    private static double mCurrentTemperature =-1;
    private static double mCurrentHumidity =-1;
    private final List<OnCcuRegistrationCompletedListener> onCcuRegistrationCompletedListeners = new ArrayList<>();
    private final List<EntityDeletedListener> entityDeletedListeners = new ArrayList<>();
    public static CCUHsApi getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Hay stack api is not initialized");
        }
        return instance;
    }

    public CCUHsApi(Context c, String hayStackUrl, String careTakerUrl, String gatewayUrl)
    {
        CcuLog.i(TAG_CCU_ROOM_DB, "---CCUHsApi--init started---");
        if (instance != null)
        {
            throw new IllegalStateException("Api instance already created , use getInstance()");
        }
        context = c;
        EventBus.getDefault().register(this);
        this.hayStackUrl = hayStackUrl;
        this.careTakerUrl = careTakerUrl;
        this.gatewayServiceUrl = gatewayUrl;
        hsClient = new AndroidHSClient();
        tagsDb = (CCUTagsDb) hsClient.db();
        tagsDb.init(context);

    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onDatabaseLoad(DatabaseEvent event) {
        CcuLog.i(TAG_CCU_ROOM_DB, "Event Type:: " + event.getSerialAction().name());
        if (event.getSerialAction() == DatabaseAction.MESSAGE_DATABASE_LOADED_SUCCESS) {
            //postProcessingInit();
            CcuLog.i(TAG_CCU_ROOM_DB, "post processing done- launch ui");
            //setCcuDbReady(true);
            finishInitRemainingTasks();

        }
    }

    public void registerEntityDeletedListener(EntityDeletedListener listener) {
        entityDeletedListeners.add(listener);
    }

    private void finishInitRemainingTasks(){
        instance = this;

        hisSyncHandler = new HisSyncHandler(this);

        syncStatusService = SyncStatusService.getInstance(context);
        syncManager = new SyncManager(context);

        checkSiloMigration(context);                  // remove after all sites migrated, post Jan 20 2021
        updateJwtValidity();
        this.defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        CcuLog.i(TAG_CCU_ROOM_DB, "---CCUHsApi--init completed---");


        DatabaseAction databaseAction = DatabaseAction.MESSAGE_DATABASE_LOADED_SUCCESS_INIT_UI;
        DatabaseEvent databaseEvent = new DatabaseEvent(databaseAction);
        EventBus.getDefault().postSticky(databaseEvent);
    }

    public boolean isBacNetEnabled() {
        return defaultSharedPrefs.getBoolean("isBACnetinitialized", false);
    }

    // Check whether we've migrated kind: "string" to kind: "Str".  If not, run the migration.
    private void checkSiloMigration(Context context) {
        boolean hasMigratedToSilo = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_HAS_MIGRATED_TO_SILO, false);
        if (!hasMigratedToSilo) {

            CcuLog.i(TAG_CCU_HS, "Migrating tags database to Silo.");

            Migrations.migrateTagsDb(tagsDb);

            syncEntityTree();       // used during dev; can remove otherwise if desired.
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(PREFS_HAS_MIGRATED_TO_SILO, true)
                    .apply();
        } else {
            CcuLog.i(TAG_CCU_HS, "Already migrated tags database to Silo!");
        }
    }

    //For Unit test
    public CCUHsApi()
    {
        hsClient = new TestHSClient();
        tagsDb = (TestTagsDb) hsClient.db();
        tagsDb.init();
        instance = this;
        //hisSyncHandler = new HisSyncHandler(this);
    }

    public void resetBaseUrls(String hayStackUrl, String careTakerUrl, String gatewayServiceUrl) {
        this.hayStackUrl = hayStackUrl;
        this.careTakerUrl = careTakerUrl;
        this.gatewayServiceUrl = gatewayServiceUrl;
    }

    public HClient getHSClient()
    {
        return hsClient;
    }

    public String getHSUrl() {
        CcuLog.d("Haystack URL: ","url="+hayStackUrl);
        return hayStackUrl;
    }

    public EntitySyncResponse hisWriteManyToHaystackService(HDict hisWriteMetadata, HDict[] hisWritePoints) {


        HGrid hisWriteRequest = hisWriteMetadata != null ? HGridBuilder.dictsToGrid(hisWriteMetadata, hisWritePoints)
                                            : HGridBuilder.dictsToGrid(hisWritePoints);

        return HttpUtil.executeEntitySync(
                CCUHsApi.getInstance().getHSUrl() + "hisWriteMany/",
                HZincWriter.gridToString(hisWriteRequest),
                CCUHsApi.getInstance().getJwt()
        );
    }

    public String getJwt() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("token","");
    }

    @SuppressLint("ApplySharedPref")
    public void setJwt(String jwtToken) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("token", jwtToken);
        editor.commit();
        updateJwtValidity();
    }



    public String getAuthenticationUrl() {
        CcuLog.d("Authentication URL: ","url="+careTakerUrl);
        return careTakerUrl;
    }
    public void trimZoneSchedules(Schedule buildingSchedule) {
        ArrayList<HashMap<Object, Object>> zones = CCUHsApi.getInstance().readAllEntities("room");
        for (HashMap<Object, Object> m : zones) {
            if(m.containsKey("scheduleRef")) {
                ArrayList<Interval> intervalSpills = new ArrayList<>();
                Schedule zoneSchedule = CCUHsApi.getInstance().getScheduleById(m.get("scheduleRef").toString());
                if(zoneSchedule == null) {
                    // Handling Crash here.
                    zoneSchedule = CCUHsApi.getInstance().getRemoteSchedule(m.get("scheduleRef").toString());
                    CcuLog.d("CCU_MESSAGING", "Fetching the schedule from remote " + zoneSchedule);
                    if(zoneSchedule == null) {
                        CcuLog.d("CCU_MESSAGING", "The schedule retrieved from the remote source is also null.");
                        continue;
                    }
                }
                if (zoneSchedule.getMarkers().contains("disabled")) {
                    continue;
                }
                ArrayList<Interval> zoneIntervals = zoneSchedule.getScheduledIntervals();
                for (Interval v : zoneIntervals) {
                    CcuLog.d("CCU_MESSAGING", "Zone interval " + v);
                }
                ArrayList<Interval> systemIntervals = buildingSchedule.getMergedIntervals();
                ArrayList<Interval> splitSchedules = new ArrayList<>();
                for (Interval v : systemIntervals) {
                    //Multiday schedule starting on sunday has monday segment falling in the next week.Split and realign that.
                    if (v.getStart().getDayOfWeek() == 7 && v.getEnd().getDayOfWeek() == 1) {
                        long now = MockTime.getInstance().getMockTime();
                        DateTime startTime = new DateTime(now).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).withDayOfWeek(1);
                        DateTime endTime = new DateTime(now).withHourOfDay(v.getEnd().getHourOfDay()).withMinuteOfHour(v.getEnd().getMinuteOfHour()).withSecondOfMinute(v.getEnd().getSecondOfMinute()).withMillisOfSecond(v.getEnd().getMillisOfSecond()).withDayOfWeek(1);
                        splitSchedules.add(new Interval(startTime, endTime));
                    }
                }
                systemIntervals.addAll(splitSchedules);
                for (Interval v : systemIntervals) {
                    CcuLog.d("CCU_MESSAGING", "Merged System interval " + v);
                }
                for (Interval z : zoneIntervals) {
                    boolean contains = false;
                    for (Interval s : systemIntervals) {
                        if (s.contains(z)) {
                            contains = true;
                            break;
                        }
                    }
                    if (!contains) {
                        for (Interval s : systemIntervals) {
                            if (s.overlaps(z)) {
                                if (z.getStartMillis() < s.getStartMillis()) {
                                    intervalSpills.add(new Interval(z.getStartMillis(), s.getStartMillis()));
                                } else if (z.getEndMillis() > s.getEndMillis()) {
                                    intervalSpills.add(new Interval(s.getEndMillis(), z.getEndMillis()));
                                }
                                contains = true;
                                break;
                            }
                        }
                    }
                    if (!contains) {
                        intervalSpills.add(z);
                        CcuLog.d("CCU_MESSAGING", " Zone Interval not contained " + z);
                    }
                }
                Iterator daysIterator = zoneSchedule.getDays().iterator();
                while (daysIterator.hasNext()) {
                    Schedule.Days d = (Schedule.Days) daysIterator.next();
                    Interval i = zoneSchedule.getScheduledInterval(d);
                    for (Interval spill : intervalSpills) {
                        if (!i.contains(spill)) {
                            continue;
                        }
                        if (spill.getStartMillis() <= i.getStartMillis() && spill.getEndMillis() >= i.getEndMillis()) {
                            daysIterator.remove();
                            continue;
                        }
                        if (spill.getStartMillis() <= i.getStartMillis()) {
                            d.setSthh(spill.getEnd().getHourOfDay());
                            d.setStmm(spill.getEnd().getMinuteOfHour());
                        } else if (i.getEndMillis() >= spill.getStartMillis()) {
                            d.setEthh(spill.getStart().getHourOfDay());
                            d.setEtmm(spill.getStart().getMinuteOfHour());
                        }
                    }
                }
                if (zoneSchedule.getRoomRef()!= null && !intervalSpills.isEmpty()) {
                    CcuLog.d("CCU_MESSAGING", "Trimmed Zone Schedule " + zoneSchedule);
                    CCUHsApi.getInstance().updateZoneSchedule(zoneSchedule, zoneSchedule.getRoomRef());
                }
            }
        }
    }

    public String getGatewayServiceUrl() {
        CcuLog.d("gatewayServiceUrl : ","url="+gatewayServiceUrl);
        return gatewayServiceUrl;
    }

    public synchronized void saveTagsData() {
        saveTagsData(false);
        syncStatusService.saveSyncStatus();
    }

    /**
     * Save all of our data (entities, point arrays, ids to sync) to disk.

     * @param immediate whether the disk write in Shared Prefs should be immediate, synchronous.  If false,
     *                  SharedPrefs will write to memory immediate but write to disk when convenient.
     */
    public synchronized void saveTagsData(boolean immediate) {
        syncStatusService.saveSyncStatus();
        tagsDb.saveTags(immediate);
    }

    public String addSite(Site s) {
        s.setCreatedDateTime(HDateTime.make(System.currentTimeMillis()));
        s.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        s.setLastModifiedBy(CCUHsApi.getInstance().getCCUUserName());
        String siteId = tagsDb.addSite(s);
        CcuLog.i(TAG_CCU_HS," add Site "+siteId);
        syncStatusService.addUnSyncedEntity(StringUtils.prependIfMissing(siteId, "@"));
        return siteId;
    }

    /** For adding site originating from server. Supply the id originating from server, rather
     * then relying on new UUID generation */
    public String addRemoteSite(Site s, String id) {
        return tagsDb.addSiteWithId(s, id);
    }

    private boolean isBuildingTunerEquip(Equip equip){
        return equip.getMarkers().contains("tuner");
    }
    public String addEquip(Equip q) {
        boolean isBuildingTunerEquip = isBuildingTunerEquip(q);
        if(!isBuildingTunerEquip){
            q.setCcuRef(getCcuId());
        }
        if(!isBuildingTunerEquip(q)) {
            if (q.getMarkers() != null && q.getMarkers().contains(SYSTEM)) {
                q.setBacnetId(0);
                q.setBacnetType(DEVICE);
            } else if (q.getRoomRef() != null && q.getRoomRef() != "SYSTEM") {
                q.setBacnetId(HSUtil.generateBacnetId(q.getGroup()));
                q.setBacnetType(DEVICE);
            }
        }



        q.setCreatedDateTime(HDateTime.make(System.currentTimeMillis()));
        q.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        q.setLastModifiedBy(CCUHsApi.getInstance().getCCUUserName());
        String equipId = tagsDb.addEquip(q);
        //BuildingTuner euip will be local to each CCU based its domain model. It should not be synced.
        if (!isBuildingTunerEquip) {
            syncStatusService.addUnSyncedEntity(equipId);
            BackfillUtil.setBackFillDuration(context);
        }
        return equipId;
    }

    /** For adding an equip originating from server, e.g. import tuners */
    public String addRemoteEquip(Equip q, String id) {
        if(!isBuildingTunerEquip(q)){
            q.setCcuRef(getCcuId());
        }
        return tagsDb.addEquipWithId(q, id);
    }

    private boolean isBuildingTunerPoint(Point point){
        if(point.getEquipRef() != null){
            return CCUHsApi.getInstance().readMapById(point.getEquipRef()).containsKey("tuner");
        }
        return false;
    }
    public String addPoint(Point p) {
        boolean isBuildingTunerPoint = isBuildingTunerPoint(p);
        if(!isBuildingTunerPoint){
            p.setCcuRef(getCcuId());
        }
        p.setCreatedDateTime(HDateTime.make(System.currentTimeMillis()));
        p.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        p.setLastModifiedBy(CCUHsApi.getInstance().getCCUUserName());
        String pointId = tagsDb.addPoint(p);

        //BuildingTuner euip will be local to each CCU based its domain model. It should not be synced.
        if(!isBuildingTunerPoint) {
            syncStatusService.addUnSyncedEntity(pointId);
        }
        return pointId;
    }

    public String addBuildingTunerPoint(Point p, boolean syncToServer) {
        p.setCreatedDateTime(HDateTime.make(System.currentTimeMillis()));
        p.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        p.setLastModifiedBy(CCUHsApi.getInstance().getCCUUserName());
        String pointId = tagsDb.addPoint(p);
        //BuildingTuner euip will be local to each CCU based its domain model. It should not be synced.
        if(syncToServer) {
            syncStatusService.addUnSyncedEntity(pointId);
        }
        return pointId;
    }

    /** For adding an point originating from server, e.g. import tuners */
    public String addRemotePoint(Point p, String id) {
        if(!isBuildingTunerPoint(p)){
            p.setCcuRef(getCcuId());
        }
        return tagsDb.addPointWithId(p, id);
    }

    public String addRemotePoint(SettingPoint p, String id) {
        return tagsDb.addPointWithId(p, id);
    }

    public String addPoint(RawPoint p) {
        p.setCcuRef(getCcuId());
        p.setCreatedDateTime(HDateTime.make(System.currentTimeMillis()));
        p.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        p.setLastModifiedBy(CCUHsApi.getInstance().getCCUUserName());
        String rawPointId = tagsDb.addPoint(p);
        syncStatusService.addUnSyncedEntity(rawPointId);
        return rawPointId;
    }

    public String addRemotePoint(RawPoint p, String id) {
        p.setCcuRef(getCcuId());
        return tagsDb.addPointWithId(p, id);
    }

    public String addPoint(SettingPoint p) {
        p.setCreatedDateTime(HDateTime.make(System.currentTimeMillis()));
        p.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        p.setLastModifiedBy(CCUHsApi.getInstance().getCCUUserName());
        p.setCcuRef(getCcuId());
        String pointId = tagsDb.addPoint(p);
        syncStatusService.addUnSyncedEntity(pointId);
        return pointId;
    }

    // From EntityPullHandler
    public String addPointWithId(SettingPoint p, String id) {
        p.setCcuRef(getCcuId());
        String pointId = tagsDb.addPointWithId(p, id);
        syncStatusService.addUnSyncedEntity(pointId);
        return pointId;
    }

    public void updateSettingPoint(SettingPoint p, String id) {
        p.setCcuRef(getCcuId());
        p.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        String pointId = tagsDb.updateSettingPoint(p,id);
        syncStatusService.addUnSyncedEntity(pointId);
    }

    private boolean isDeviceNotCCU(Device device){
        return !device.getMarkers().contains("ccu");
    }

    public String addDevice(Device d) {
        if(isDeviceNotCCU(d)){
            d.setCcuRef(getCcuId());
        }
        d.setCreatedDateTime(HDateTime.make(System.currentTimeMillis()));
        d.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        d.setLastModifiedBy(CCUHsApi.getInstance().getCCUUserName());
        String deviceId = tagsDb.addDevice(d);
        syncStatusService.addUnSyncedEntity(deviceId);
        return deviceId;
    }

    // From EntityPullHandler
    public String addRemoteDevice(Device d, String id) {
        if(isDeviceNotCCU(d)){
            d.setCcuRef(getCcuId());
        }
        return tagsDb.addDeviceWithId(d, id);
    }

    public void updateDevice(Device d, String id) {
        if(isDeviceNotCCU(d)){
            d.setCcuRef(getCcuId());
        }
        d.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        tagsDb.updateDevice(d, id);
        if (syncStatusService.hasEntitySynced(id)) {
            syncStatusService.addUpdatedEntity(id);
        }
    }
    public String addFloor(Floor f) {
        f.setCreatedDateTime(HDateTime.make(System.currentTimeMillis()));
        f.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        f.setLastModifiedBy(CCUHsApi.getInstance().getCCUUserName());
        String floorId = tagsDb.addFloor(f);
        syncStatusService.addUnSyncedEntity(floorId);
        return floorId;
    }

    // From EntityPullHandler
    public String addRemoteFloor(Floor f, String id) {
        return tagsDb.addFloorWithId(f, id);
    }

    //TODO - Replace CCU support
    public void addZoneOccupancyPoint(String zoneRef, Zone zone) {
        Point occupancy = new Point.Builder()
                              .setDisplayName("occupancyState")
                              //.setEquipRef(equipRef)
                              .setSiteRef(zone.getSiteRef())
                              .setRoomRef(zoneRef)
                              .setFloorRef(zone.getFloorRef()).setHisInterpolate("cov")
                              .addMarker("occupancy").addMarker("state")
                              .addMarker("zone").addMarker("his")
                              .setEnums("unoccupied,occupied,preconditioning,forcedoccupied,vacation,occupancysensing,autoforcedoccupied,autoaway," +
                                        "emergencyconditioning,keycardautoaway,windowopen,noconditioning," +
                                      "demandresponseoccupied,demandresponseunoccupied")
                              .setTz(getTimeZone())
                              .build();
        CCUHsApi.getInstance().addPoint(occupancy);
    }

    public String addZone(Zone z) {
        z.setCcuRef(getCcuId());
        z.setCreatedDateTime(HDateTime.make(System.currentTimeMillis()));
        z.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        z.setLastModifiedBy(CCUHsApi.getInstance().getCCUUserName());
        String zoneId = tagsDb.addZone(z);
        syncStatusService.addUnSyncedEntity(zoneId);
        addZoneOccupancyPoint(zoneId, z);
        addZoneTemperatureModePoint(zoneId, z);
        return zoneId;
    }

    public void addZoneTemperatureModePoint(String zoneId, Zone zone) {
        Point ZoneTemperatureMode = new Point.Builder()
                .setDisplayName(Tags.ZONE_HVAC_MODE)
                .setSiteRef(zone.getSiteRef())
                .setRoomRef(zoneId)
                .setFloorRef(zone.getFloorRef()).setHisInterpolate("cov")
                .addMarker(Tags.ZONE).addMarker(Tags.HVAC_MODE)
                .addMarker("his")
                .setEnums("DUAL_TEMP, SINGLE_COOLING, SINGLE_HEATING")
                .setTz(getTimeZone())
                .build();
        String ZoneTemperatureModeId = CCUHsApi.getInstance().addPoint(ZoneTemperatureMode);
        CCUHsApi.getInstance().writeHisValById(ZoneTemperatureModeId, 0.0);
    }

    // From EntityPullHandler
    public String addRemoteZone(Zone z, String id) {
        z.setCcuRef(getCcuId());
        return tagsDb.addZoneWithId(z, id);
    }

    public void updateSite(Site s, String id) {
        s.setCreatedDateTime(HDateTime.make(System.currentTimeMillis()));
        s.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        s.setLastModifiedBy(CCUHsApi.getInstance().getCCUUserName());
        tagsDb.updateSite(s, id);
        if (syncStatusService.hasEntitySynced(id)) {
            syncStatusService.addUpdatedEntity(id);
        }
        updateLocationDataForWeatherUpdate(s);
    }

    //Local site entity may be updated on pubnub notification which does not need to be synced back.
    public void updateSiteLocal(Site s, String id) {
        tagsDb.updateSite(s, id);
        updateLocationDataForWeatherUpdate(s);
    }

    private void updateLocationDataForWeatherUpdate(Site updatedSite) {

        SharedPreferences.Editor spPrefsEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        spPrefsEditor.putString("address", updatedSite.getGeoAddress());
        spPrefsEditor.putString("city", updatedSite.getGeoCity());
        spPrefsEditor.putString("country", updatedSite.getGeoCountry());
        spPrefsEditor.putString("state", updatedSite.getGeoState());

        spPrefsEditor.apply();
    }

    public void updateEquip(Equip q, String id)
    {
        if(!isBuildingTunerEquip(q)){
            q.setCcuRef(getCcuId());
        }
        q.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        tagsDb.updateEquip(q, id);
        if(!isBuildingTunerEquip(q)){
            if (syncStatusService.hasEntitySynced(id)) {
                syncStatusService.addUpdatedEntity(id);
            }
        }
    }

    public void updateBuildingTunerEquip(Equip q, String id, boolean syncToServer)
    {
        CcuLog.e(TAG_CCU_DOMAIN, "invoking updateBuildingTunerEquip");
        q.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        tagsDb.updateEquip(q, id);
        if(syncToServer){
            if (syncStatusService.hasEntitySynced(id)) {
                syncStatusService.addUpdatedEntity(id);
            }
        }
    }

    public void updateEquipLocally(Equip q, String id) {
        tagsDb.updateEquip(q, id);
    }

    public void updatePoint(RawPoint r, String id)
    {
        r.setCcuRef(getCcuId());
        r.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        tagsDb.updatePoint(r, id);
        if (syncStatusService.hasEntitySynced(id)) {
            syncStatusService.addUpdatedEntity(id);
        }
    }

    public void updatePoint(Point point, String id)
    {
        if(!isBuildingTunerPoint(point)){
            point.setCcuRef(getCcuId());
        }
        point.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        tagsDb.updatePoint(point, id);

        if(!isBuildingTunerPoint(point)){
            if (syncStatusService.hasEntitySynced(id)) {
                syncStatusService.addUpdatedEntity(id);
            }
        }
    }

    public void updateBuildingTunerPoint(Point point, String id, boolean syncToServer) {
        point.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        tagsDb.updatePoint(point, id);
        if (syncToServer) {
            if (syncStatusService.hasEntitySynced(id)) {
                syncStatusService.addUpdatedEntity(id);
            } else {
                syncStatusService.addUnSyncedEntity(id);
            }
        }
    }

    public void updateFloor(Floor r, String id)
    {
        r.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        tagsDb.updateFloor(r, id);
        if (syncStatusService.hasEntitySynced(id)) {
            syncStatusService.addUpdatedEntity(id);
        }
    }
    
    public void updateFloorLocally(Floor r, String id)
    {
        tagsDb.updateFloor(r, id);
    }

    public void updateZone(Zone z, String id)
    {
        z.setCcuRef(getCcuId());
        z.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        tagsDb.updateZone(z, id);
        if (syncStatusService.hasEntitySynced(id)) {
            syncStatusService.addUpdatedEntity(id);
        }
    }

    public void updateZoneLocally(Zone z, String id)
    {
        CcuLog.i("ccu_read_changes","updateZoneLocally  "+z.getScheduleRef());
        z.setCcuRef(getCcuId());
        tagsDb.updateZone(z, id);
    }

    /**
     * Helper method that converts HGrid to an Array of Hashmap of String.
     * This should be replaced with parameterized reallAllEntities call.
     */
    @Deprecated
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
     * This should be replaced with parameterized readEntity call.
     */
    @Deprecated
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
            CcuLog.w(TAG_CCU_HS, "Unknown record: " + query);
        }
        return map;
    }

    /**
     * Normally, we use Hashmap from our query but this was needed for proper serialization by
     * EquipSyncAdapter#syncSystemEquip.
     *
     * @return an HDict from a query, or null if the result is empty
     */
    public @Nullable HDict readAsHdict(String query) {
        return hsClient.read(query, false);
    }

    public HashMap<Object, Object> readMapById(String id)
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
            CcuLog.e(TAG_CCU_HS,"Entity does not exist "+id);
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
            CcuLog.e(TAG_CCU_HS,"Entity does not exist "+id);
        }
        return null;
    }
    /**
     * Read the all entities for the given query in HDict format
     */
    public List<HDict> readAllHDictByQuery(String query) {
        List<HDict> hDictList = new ArrayList<>();
        try {
            HGrid grid = hsClient.readAll(query);
            for (int i = 0; i < grid.numRows(); i++) {
                hDictList.add(grid.row(i));
            }
        } catch (Exception e) {
            CcuLog.e(TAG_CCU_HS, "Error fetching entities for query: " + query, e);
        }
        return hDictList;
    }


    public HGrid readHDictByIds(HRef[] ids) {
        try {
            return hsClient.readByIds(ids);
        } catch (UnknownRecException e) {
            CcuLog.e(TAG_CCU_HS, "Entity does not exist ");
        }
        return null;
    }

    public HDict readHDict(String query)
    {
        try
        {
            return hsClient.read(query);
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
        writePointForCcuUser(id, level, val, duration, null);
    }

    public void writePointForCcuUser(String id, int level, Double val, int duration, String reason)
    {
        pointWrite(HRef.copy(id), level, getCCUUserName(), HNum.make(val), HNum.make(duration), reason);
    }

    public void writeTunerPointForCcuUser(String id, int level, Double val, int duration, String reason)
    {
        tunerPointWrite(HRef.copy(id), level, getCCUUserName(), HNum.make(val), HNum.make(duration), reason);
    }

    /**
     * Write to a 'writable' point
     */
    public HGrid writePoint(String id, int level, String who, Double val, int duration)
    {
       return pointWrite(HRef.copy(id), level, who, HNum.make(val), HNum.make(duration));
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
     */
    public void writePointLocal(String id, int level, String who, Double val, int duration) {
        hsClient.pointWrite(HRef.copy(id), level, who, HNum.make(val), HNum.make(duration), HDateTime.make(System.currentTimeMillis()));
    }

    public void writePointStrValLocal(String id, int level, String who, String val, int duration) {
        hsClient.pointWrite(HRef.copy(id), level, who, HStr.make(val), HNum.make(duration),
                HDateTime.make(System.currentTimeMillis()));
    }

    /**
     * Write to the first 'writable' point fetched using query
     * at default level  - 9
     * default user - ""
     */
    public void writeDefaultVal(String query, Double val)
    {
        HashMap<Object, Object> point = readEntity(query);
        if (!point.isEmpty()) {
            String id = point.get("id").toString();
            pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, getCCUUserName(), HNum.make(val), HNum.make(0));
        } else {
            CcuLog.d(TAG_CCU_HS, "Invalid point write attempt: "+query);
        }
    }

    public void writeDefaultVal(String query, String val)
    {
        HashMap<Object, Object> point = readEntity(query);
        if (!point.isEmpty()) {
            String id = point.get("id").toString();
            pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_POINT_LEVEL, getCCUUserName(), HStr.make(val), HNum.make(0));
        } else {
            CcuLog.d(TAG_CCU_HS, "Invalid point write attempt: "+query);
        }
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
        pointWrite(id, level, getCCUUserName(), val, dur, null);
    }


    public HGrid pointWrite(HRef id, int level, String who, HVal val, HNum dur) {
       return pointWrite(id, level, who, val, dur, null);
    }

    public HGrid pointWrite(HRef id, int level, String who, HVal val, HNum dur, String reason) {
        HGrid hGrid = hsClient.pointWrite(id, level, who, val, dur, HDateTime.make(System.currentTimeMillis()));

        HashMap<Object, Object> pointMap = readMapById(id.toString());

        if((readDefaultVal("offline and mode and point") > 0)
                && ((!pointMap.containsKey("offline") && !pointMap.containsKey("watchdog")))){
            CcuLog.d(TAG_CCU_HS," Skip write");
            return hGrid;
        }

        if (CCUHsApi.getInstance().isCCURegistered() && hasEntitySynced(id.toString())) {
            String uid = id.toString();
            if (dur.unit == null) {
                dur = HNum.make(dur.val ,"ms");
            }

            HDictBuilder b = new HDictBuilder().add("id", HRef.copy(uid)).add("level", level).add("who", who).add("val", val).add("duration", dur);
            if (StringUtils.isNotEmpty(reason)) {
                b.add("reason", reason);
            }


            PointWriteCache.Companion.getInstance().writePoint(uid, b.toDict());
            CcuLog.d(TAG_CCU_HS, "PointWrite- "+id+" : "+val);
        }
        return hGrid;
    }

    public void tunerPointWrite(HRef id, int level, String who, HVal val, HNum dur, String reason) {
        hsClient.pointWrite(id, level, who, val, dur, HDateTime.make(System.currentTimeMillis()));

        HashMap<Object, Object> pointMap = readMapById(id.toString());

        if((readDefaultVal("offline and mode and point") > 0)
                && ((!pointMap.containsKey("offline") && !pointMap.containsKey("watchdog")))){
            CcuLog.d(TAG_CCU_HS," Skip write");
            return;
        }

        if (CCUHsApi.getInstance().isCCURegistered()) {
            String uid = id.toString();
            if (dur.unit == null) {
                dur = HNum.make(dur.val ,"ms");
            }

            HDictBuilder b = new HDictBuilder().add("id", HRef.copy(uid)).add("level", level).add("who", who).add("val", val).add("duration", dur);
            if (StringUtils.isNotEmpty(reason)) {
                b.add("reason", reason);
            }
            CcuLog.d(TAG_CCU_HS, "PointWrite- "+id+" : "+val);
            PointWriteCache.Companion.getInstance().writePoint(uid, b.toDict());
        }
    }

    public void clearPointArrayLevel(String id, int level, boolean local) {
        CcuLog.i("CCU_HAYSTACK","clearPointArrayLevel "+id + "level "+level + "local "+local);
        deletePointArrayLevel(id, level);
        if (!local) {
            HDictBuilder b = new HDictBuilder()
                    .add("id", HRef.copy(id))
                    .add("level", level)
                    .add("who", CCUHsApi.getInstance().getCCUUserName())
                    .add("duration", HNum.make(0, "ms"))
                    .add("val", (HVal) null);
            PointWriteCache.Companion.getInstance().writePoint(id, b.toDict());        }

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

        HashMap<Object, Object> point = readEntity(query);
        if(!point.isEmpty()) {
            String id = point.get("id").toString();
            if (id.isEmpty()) {
                return 0.0;
            }
            ArrayList<HashMap> values = CCUHsApi.getInstance().readPoint(id);
            if (values != null && values.size() > 0) {
                HashMap valMap = values.get(HayStackConstants.DEFAULT_POINT_LEVEL - 1);
                return valMap.get("val") == null ? 0 : Double.parseDouble(valMap.get("val").toString());
            } else {
                return 0.0;
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
            return 0.0;
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

        HashMap<Object, Object> point = readEntity(query);
        Object id = point.get("id");
        if (id == null || id == "") {
            return "";
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(id.toString());
        if (values != null && values.size() > 0) {
            HashMap valMap = ((HashMap) values.get(HayStackConstants.DEFAULT_POINT_LEVEL - 1));
            return valMap.get("val") == null ? "" : valMap.get("val").toString();
        } else {
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
        CcuLog.d(TAG_CCU_HS, "Response : "+response);

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

    public String readPointPriorityLatestTime(String id) {

        ArrayList values = readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("lastModifiedDateTime") != null) {
                    return (valMap.get("lastModifiedDateTime").toString());
                }
            }
        }
        return null;
    }

    public Double readPointPriorityValByQuery(String query)
    {
        HashMap<Object, Object> point = readEntity(query);
        Object id = point.get("id");
        if (id == null || id == "") {
            CcuLog.d(TAG_CCU_HS,"readPointPriorityValByQuery point id is null : "+query);
            return 0.0;
        }

        return readPointPriorityVal(id.toString());
    }
    
    /**
     *
     * @param id
     * @param offset Level from which priority evaluation is done.
     * @return
     */
    public static double readPointPriorityValFromOffset(String id, int offset){
        
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0) {
            for (int l = offset; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
        
    }
    
    public String readId(String query)
    {
        HashMap<Object, Object> point = readEntity(query);
        Object id = point.get("id");
        if (id == null || id == "") {
            return null;
        }

        return id.toString();
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
        return id != null ? tagsDb.getLastHisItem(HRef.copy(id)) : null;
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
                CcuLog.d(TAG_CCU_HS,"write point id is null : "+query);
                return 0.0;
            }
            String    id     = point.get("id").toString();
            HisItem item = curRead(id);
            return item == null ? 0 : item.getVal();
        }
    }

    /**
     * Write history value only if the new value is different from current value.
     * @param id
     * @param val
     */
    public void writeHisValById(String id, Double val)
    {
        long time = System.currentTimeMillis();
        HisItem item = curRead(id);
        Double prevVal = item == null ? 0 : item.getVal();
        if((item == null)|| (!item.initialized) || !prevVal.equals(val)) {
            hsClient.hisWrite(HRef.copy(id), new HHisItem[]{HHisItem.make(HDateTime.make(System.currentTimeMillis()), HNum.make(val))});
        } else {
            HDict point = readHDictById(id);
            if (point != null && point.get("sensor", false) != null) {
                hisSyncHandler.addSensorPendingSync(id);
            }
        }
        CcuLog.d(TAG_CCU_HS,"writeHisValById "+id+" timeMS: "+(System.currentTimeMillis()- time)+
                " prevVal: "+prevVal+" currentVal: "+val+" item: "+item);
    }

    /**
     * Writes values without checking the current value.
     * This shall be used all the time while initializing a new point thats just created.
     * @param id
     * @param val
     */
    public void writeHisValueByIdWithoutCOV(String id, Double val)
    {
        tagsDb.putHisItem(id, val);
    }

    /**
     * Writes a list of hisItems.
     * This shall be used all the time while initializing a new point thats just created.
     * @param hisItems
     */
    public void writeHisValueByIdWithoutCOV(List<HisItem> hisItems)
    {
        tagsDb.putHisItems(hisItems);
    }

    public void writeHisValByQuery(String query, Double val)
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
                CcuLog.d(TAG_CCU_HS,"write point id is null : "+query);
                return;
            }
            String    id     = point.get("id").toString();
            HisItem previtem = curRead(id);
            Double prevVal = previtem == null ? 0 : previtem.getVal();
            if((previtem == null) || (!previtem.initialized) || !prevVal.equals(val) || point.containsKey("diag")) {
                HisItem item = new HisItem(id, new Date(), val);
                hisWrite(item);
            }
        }
    }

    public void deleteEntity(String id) {
        HashMap<Object,Object> entity = CCUHsApi.getInstance().readMapById(id);
        CcuLog.d(TAG_CCU_HS, "deleteEntity " + entity.toString());
        tagsDb.tagsMap.remove(id.replace("@", ""));
        EntityDBUtilKt.deleteEntitywithId(id,this.context);
        syncStatusService.addDeletedEntity(id, true);
        tagsDb.clearHistory(HRef.copy(id));
        HisItemCache.getInstance().delete(id);

        if(isBacNetEnabled() && id != null && !id.replace("@","").equals("null")) {
            if (entity.get("equip") != null) {
                sendBroadCastToBacApp(INTENT_ZONE_DELETED,id);
            } else {
                sendBroadCastToBacApp(INTENT_POINT_DELETED,id);
            }
        }
    }

    /**
     * Used while deleteing a batch of entities. No need to persist items after each deletion.
     * The caller has to invoke save sync status separately.
     * @param id
     */
    public void deleteEntityItem(String id) {
        HashMap<Object,Object> entity = CCUHsApi.getInstance().readMapById(id);
        CcuLog.d(TAG_CCU_HS, "deleteEntity " + entity.toString());
        tagsDb.tagsMap.remove(id.replace("@", ""));
        EntityDBUtilKt.deleteEntitywithId(id.replace("@", ""),this.context);
        syncStatusService.addDeletedEntity(id, false);
        tagsDb.clearHistory(HRef.copy(id));
        HisItemCache.getInstance().delete(id);

        if(isBacNetEnabled() && id != null && !id.replace("@","").equals("null"))  {
            if (entity.get("equip") != null) {
                sendBroadCastToBacApp(INTENT_ZONE_DELETED, id);
            } else {
                sendBroadCastToBacApp(INTENT_POINT_DELETED, id);
            }
        }
    }
    private void sendBroadCastToBacApp(String action, String id) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra("message", id);
        context.sendBroadcast(intent);
    }

    public void deleteEntityLocally(String id) {
        tagsDb.tagsMap.remove(id.replace("@", ""));
        EntityDBUtilKt.deleteEntitywithId(id.replace("@", ""),this.context);
        if (tagsDb.idMap.get(id) != null) {
            tagsDb.idMap.remove(id);
        }
    }

    //Removes entity , but the operation is not synced to backend
    public void removeEntity(String id) {
        CcuLog.d(TAG_CCU_HS, "deleteEntity: " + id);
        tagsDb.tagsMap.remove(id.replace("@", ""));
        EntityDBUtilKt.deleteEntitywithId(id.replace("@", ""),this.context);
        tagsDb.clearHistory(HRef.copy(id));
        HisItemCache.getInstance().delete(id);
        removeId(id);
    }

    public void removeId(String id) {
        CcuLog.d(TAG_CCU_HS, "removeId: " + id);
        //tagsDb.removeIdMap.remove(id.replace("@", ""));
        if (syncStatusService.getDeletedData().contains(id)) {
            syncStatusService.setDeletedEntitySynced(id);
        }
    }

    public void deleteWritableArray(String id)
    {
        CcuLog.d(TAG_CCU_HS, "deleteWritableArray: " + id);
        tagsDb.writeArrays.remove(id.replace("@", ""));
    }

    public void deleteWritablePoint(String id) {
        deleteWritableArray(id);
        deleteEntity(id);
    }

    public void deleteWritablePointLocally(String id) {
        deleteWritableArray(id);
        deleteEntityLocally(id);
    }

    public void deleteFloorEntityTreeLeavingRemoteFloorIntact(String id) {
        CcuLog.d(TAG_CCU_HS, "deleteFloorEntityTreeLeavingRemoteFloorIntact: " + id);
        HashMap<Object, Object> entity = CCUHsApi.getInstance().readEntity("id == " + id);
        if (entity.get("floor") == null) {
            // not a floor :-(
            CcuLog.w(TAG_CCU_HS, "Attempt to delete Floor locally with non-floor entity id");
            return;
        }
        ArrayList<HashMap<Object, Object>> rooms = readAllEntities("room and floorRef == \"" + id + "\"");
        for (HashMap<Object, Object> room : rooms)
        {
            deleteEntityTree(room.get("id").toString());
        }
        deleteEntityLocally(entity.get("id").toString());
    }

    public void deleteEntityTree(String id) {
        CcuLog.d(TAG_CCU_HS, "deleteEntityTree " + id);
        HashMap<Object, Object> entity = readEntity("id == " + id);
        Intent intent = null;
        if (entity.get("site") != null) {
            //Deleting site from a CCU should not remove shared entities like site , floor or building tuner.
            ArrayList<HashMap<Object, Object>> equips = readAllEntities("equip and siteRef == \"" + id + "\"");
            for (HashMap<Object, Object> equip : equips) {
                if (!equip.containsKey("tuner"))
                    deleteEntityTree(equip.get("id").toString());
            }

            ArrayList<HashMap<Object, Object>> devices = readAllEntities("device and siteRef == \"" + id + "\"");
            for (HashMap<Object, Object> device : devices) {
                deleteEntityTree(device.get("id").toString());
            }

            ArrayList<HashMap<Object, Object>> schedules = readAllEntities("schedule and siteRef == \"" + id + "\"");
            for (HashMap<Object, Object> schedule : schedules) {
                if (!schedule.containsKey("building"))
                    deleteEntityItem(schedule.get("id").toString());
            }
        } else if (entity.get("floor") != null) {

            ArrayList<HashMap<Object, Object>> rooms = readAllEntities("room and floorRef == \"" + id + "\"");
            for (HashMap<Object, Object> room : rooms) {
                deleteEntityTree(room.get("id").toString());
            }
            deleteEntityItem(entity.get("id").toString());
        } else if (entity.get("room") != null) {

            ArrayList<HashMap<Object, Object>> schedules = readAllEntities("schedule and roomRef == "+ id );
            CcuLog.i(TAG_CCU_HS,"  delete Schedules in room "+schedules.size());
            for (HashMap<Object, Object> schedule : schedules) {
                deleteEntityItem(schedule.get("id").toString());
            }
    
            //TODO - This should be made generic. but querying all points with roomRef needs more thought.
            ArrayList<HashMap<Object, Object>> points =
                readAllEntities("point and occupancy and state and roomRef == \"" + id+"\"");
            CcuLog.i(TAG_CCU_HS,"  delete occupancy state of room "+points.size());
            for (HashMap<Object, Object> point : points) {
                deleteEntityItem(point.get("id").toString());
            }

            ArrayList<HashMap<Object, Object>> schedulablePoints = readAllEntities("schedulable and zone and roomRef == \"" + id+"\"");
            for (HashMap<Object, Object> point : schedulablePoints) {
                if (point.get("writable") != null) {
                    deleteWritableArray(point.get("id").toString());
                }
                deleteEntityItem(point.get("id").toString());
            }

            HashMap<Object, Object> zoneHvacModePoint =
                    readEntity("hvacMode and zone and roomRef == \"" + id+"\"");
            CcuLog.i(TAG_CCU_HS,"  delete TemperatureMode point of room "+zoneHvacModePoint);
                deleteEntityItem(zoneHvacModePoint.get("id").toString());

            deleteEntityItem(entity.get("id").toString());
        }else if (entity.get("equip") != null) {

            ArrayList<HashMap<Object, Object>> points = readAllEntities("point and equipRef == \"" + id + "\"");
            for (HashMap<Object, Object> point : points) {
                if (point.get("writable") != null) {
                    deleteWritableArray(point.get("id").toString());
                }
                deleteEntityItem(point.get("id").toString());
                if(isBacNetEnabled()) {
                    intent = new Intent(INTENT_POINT_DELETED);
                    intent.putExtra("message", point.get("id").toString());
                    context.sendBroadcast(intent);
                }
            }
            deleteEntityItem(id);
        } else if (entity.get("device") != null) {
            ArrayList<HashMap<Object, Object>> points = readAllEntities("point and deviceRef == \"" + id + "\"");
            for (HashMap<Object, Object> point : points) {
                if (point.get("writable") != null) {
                    deleteWritableArray(point.get("id").toString());
                }
                deleteEntityItem(point.get("id").toString());
            }
            deleteEntityItem(id);
            for(EntityDeletedListener listener: entityDeletedListeners) {
                listener.onDeviceDeleted(id);
            }
        } else if (entity.get("point") != null) {
            if (entity.get("writable") != null) {
                deleteWritableArray(entity.get("id").toString());
            }
            deleteEntityItem(entity.get("id").toString());
            hsClient.clearPointFromWatch(HRef.copy(entity.get("id").toString()));
        }
        syncStatusService.saveSyncStatus();
    }

    /**
     * indicate given id is now synced, passing up the id and the remote id returned from the server.
     * The two ids should match.
     *
     * The method is historical, it used to be. putIdMap(luid, guid);
     */
    public void setSynced(String id)  {

        if (id == null || id.isEmpty()) {
            CcuLog.e(TAG_CCU_HS, "id null or empty in set synced");
            return;
        }
        syncStatusService.setEntitySynced(id);
    }

    public boolean entitySynced(String id) {
        return syncStatusService.hasEntitySynced(id);
    }

    public boolean entityExists(String id) {
        return tagsDb.idMap.containsKey(id);
    }

    public boolean siteSynced() {
        return getSite() != null && entitySynced(getSiteIdRef().toString());
    }

    public boolean isEntityDeleted(String id) {
        return syncStatusService.getDeletedData().contains(id);
    }

    public void syncEntityTree()
    {
        if(CCUHsApi.getInstance().readDefaultVal("offline and mode and point") > 0) {
            CcuLog.d(TAG_CCU_HS," Skip his sync in offlineMode");
            return;
        }
        //TODO : Check if sync session is already in progress
        if (syncManager.isEntitySyncProgress()) {
            syncManager.scheduleSync();
        } else {
            syncManager.syncEntities(true);
        }
    }

    public void syncEntityWithPointWrite() {
        syncManager.syncEntitiesWithPointWrite();
    }
    
    public void syncEntityWithPointWriteDelayed(long delaySeconds) {
        syncManager.syncEntitiesWithPointWriteWithDelay(delaySeconds);
    }
    
    public void syncPointArrays() {
        syncManager.syncPointArray();
    }
    
    public void scheduleSync() {
        syncManager.scheduleSync();
    }

    public void syncHisDataWithPeriodicPurge() {
        if(readDefaultVal("offline and mode and point") > 0) {
            CcuLog.d(TAG_CCU_HS," Skip his sync in offlineMode");
            return;
        }

        if (syncManager.isEntitySyncProgress()) {
            CcuLog.d(TAG_CCU_HS," Skip his sync, entity sync in progress");
            return;
        }
        hisSyncHandler.syncHisDataWithPurge();
    }

    public boolean syncExistingSite(String siteId) {
        if(CCUHsApi.getInstance().readDefaultVal("offline and mode and point") > 0) {
            CcuLog.d(TAG_CCU_HS," Cannot sync existing");
            return false;
        }

        siteId = StringUtils.stripStart(siteId,"@");

        if (StringUtils.isBlank(siteId)) {
            return false;
        }

        HGrid remoteSite = getRemoteSite(siteId);

        if (remoteSite == null || remoteSite.isEmpty() || remoteSite.isErr()) {
            return false;
        }

        EntityParser p = new EntityParser(remoteSite);
        Site s = p.getSite();
        addRemoteSite(s, siteId);
        CcuLog.d(TAG_CCU_HS,"Added Site "+s.getId());

        HClient hClient = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);

        //import building schedule data
        importBuildingSchedule(StringUtils.prependIfMissing(siteId, "@"), hClient);
        importBuildingOccupancy(StringUtils.prependIfMissing(siteId, "@"),hClient);

        //import building special schedule
        importBuildingSpecialSchedule(StringUtils.prependIfMissing(siteId, "@"), hClient);

        //import building tuners TODO - Common data feature.
        //importBuildingTuners(StringUtils.prependIfMissing(siteId, "@"), hClient, false);

        //import Named schedule
        importNamedSchedule(hClient);

        //TODO - Common data feature
        /*ArrayList<HashMap<Object, Object>> writablePoints = CCUHsApi.getInstance()
                .readAllEntities("point and writable");
        ArrayList<HDict> hDicts = new ArrayList<>();
        for (HashMap<Object, Object> m : writablePoints) {
            HDict pid = new HDictBuilder().add("id",HRef.copy(m.get("id").toString())).toDict();
            hDicts.add(pid);
        }
        return importPointArrays(hDicts);*/
        return true;
    }

    public void importNamedSchedule(HClient hClient){
        Site site = CCUHsApi.getInstance().getSite();
        importNamedSchedulebySite(hClient,site);
    }
    public void importNamedSchedulebySite(HClient hClient, Site site) {
        if (site != null && site.getOrganization() != null) {
            String org = site.getOrganization();
            importNamedScheduleWithOrg(hClient,org);
        }
    }

    public void importNamedScheduleWithOrg(HClient hClient,String org){
        CcuLog.d(TAG, "org = "+org);
        CcuLog.d(TAG, "hClient = "+hClient);
        HDict nameScheduleDict = new HDictBuilder().add("filter",
                "named and schedule and organization == \""+org+"\"").toDict();
        CcuLog.d(TAG, "nameScheduleDict = "+nameScheduleDict);
        String response = fetchRemoteEntityByQuery("named and schedule and organization == \""+org+"\"");

        if(response == null || response.isEmpty()){
            CcuLog.d(TAG, "Failed to read remote entity : " + response);
            return;
        }
        HGrid sGrid = new HZincReader(response).readGrid();
        Iterator it = sGrid.iterator();

        while (it.hasNext()) {
            HRow row = (HRow) it.next();
            Schedule schedule = new Schedule.Builder().setHDict(new HDictBuilder().add(row).toDict()).build();
            if(schedule.getMarkers().contains("default")
                    && !schedule.getmSiteId().equals(CCUHsApi.getInstance().getSiteIdRef().toString().replace("@", ""))){
                continue;
            }
            tagsDb.addHDict((row.get("id").toString()).replace("@", ""), row);
            CcuLog.i(TAG, "Named schedule Imported - "+ row.get("id").toString());
        }

    }

    private void importBuildingOccupancy(String siteId, HClient hClient){
        HashMap<Object, Object> buildingOccupancyMap =
                CCUHsApi.getInstance().readEntity(Queries.BUILDING_OCCUPANCY);
        if (!buildingOccupancyMap.isEmpty()) {
            //CCU already has a building occupancy.
            CcuLog.i(TAG, " importBuildingOccupancy : buildingOccupancy exists");
            return;
        }

        try {
            HDict buildingDict =
                    new HDictBuilder().add("filter",
                            "building and occupancy and siteRef == " + StringUtils.prependIfMissing(siteId, "@")).toDict();;
            HGrid buildingOcc = invokeWithRetry("read",hClient, HGridBuilder.dictToGrid(buildingDict));

            if (buildingOcc == null) {
                return;
            }


            Iterator it = buildingOcc.iterator();
            while (it.hasNext()) {
                HRow r = (HRow) it.next();
                BuildingOccupancy buildingOccupancy =
                        new BuildingOccupancy.Builder().setHDict(new HDictBuilder().add(r).toDict()).build();

                String guid = buildingOccupancy.getId();
                buildingOccupancy.setSiteRef(siteId);
                CCUHsApi.getInstance().addSchedule(guid, buildingOccupancy.getBuildingOccupancyHDict());
                CCUHsApi.getInstance().setSynced(StringUtils.prependIfMissing(guid, "@"));
                CcuLog.d(TAG, "Import building Occupancy completed");
            }
        } catch (UnknownRecException e) {
            e.printStackTrace();
        }

    }


    public void importBuildingSchedule(String siteId, HClient hClient){

        HashMap currentBuildingSchedule = read("schedule and building and not named and not special");
        if (!currentBuildingSchedule.isEmpty()) {
            //CCU already has a building schedule.
            CcuLog.i(TAG, " importBuildingSchedule : buildingSchedule exists");
            return;
        }

        try {
            HDict buildingDict =
                    new HDictBuilder().add("filter",
                            "building and schedule and vacation and not special and siteRef == " + siteId).toDict();
            HGrid buildingSch = hClient.call("read", HGridBuilder.dictToGrid(buildingDict));

            if (buildingSch == null) {
                return;
            }

            Iterator it = buildingSch.iterator();
            while (it.hasNext()) {
                HRow r = (HRow) it.next();
                Schedule buildingSchedule =  new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build();

                String guid = buildingSchedule.getId();
                buildingSchedule.setmSiteId(siteId);
                CCUHsApi.getInstance().addSchedule(guid, buildingSchedule.getScheduleHDict());
                CCUHsApi.getInstance().setSynced(StringUtils.prependIfMissing(guid, "@"));
            }
        } catch (UnknownRecException e) {
            e.printStackTrace();
        }
    }

    private void importBuildingSpecialSchedule(String siteId, HClient hClient){

        HashMap<Object, Object>  currentBuildingSchedule = readEntity("schedule and building and special");
        if (!currentBuildingSchedule.isEmpty()) {
            //CCU already has a building special schedule.
            CcuLog.i(TAG, " importBuildingSpecialSchedule : building Special Schedule exists");
            return;
        }

        try {
            HDict buildingDict =
                    new HDictBuilder().add("filter", "building and schedule and special and siteRef == "
                            + siteId).toDict();
            HGrid buildingSch = hClient.call("read", HGridBuilder.dictToGrid(buildingDict));

            if (buildingSch == null) {
                return;
            }

            Iterator it = buildingSch.iterator();
            while (it.hasNext()) {
                HRow row = (HRow) it.next();
                tagsDb.addHDict(row.get("id").toString(), row);
                CcuLog.i(TAG,"Building Special schedule Imported");
            }
        } catch (UnknownRecException e) {
            e.printStackTrace();
        }
    }
    
    public HGrid getRemoteSite(String siteId)
    {
        /* Sync a site*/
        HClient hClient   = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict   navIdDict = new HDictBuilder().add(HayStackConstants.ID, HRef.make(siteId)).toDict();
        HGrid   hGrid     = HGridBuilder.dictToGrid(navIdDict);

        HGrid siteGrid = hClient.call(HStdOps.read.name(), hGrid);
        if (siteGrid != null) {
            siteGrid.dump();
        } else {
            CcuLog.e(TAG, "RemoteSite fetch Failed.");
        }

        return siteGrid;
    }

    public Site getRemoteSiteEntity(String siteGuid) {
        HGrid siteGrid = getRemoteSite(siteGuid);
        if (siteGrid != null) {
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
        }
        return null;
    }

    public @Nullable String getSiteName() {
        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() > 0) {
            return site.get("dis").toString();
        } else {
            return null;
        }
    }

    /**
     * Get the Site entity.
     * @return Site
     */
    public @Nullable Site getSite() {
        HashMap site = CCUHsApi.getInstance().read("site");
        if (!site.isEmpty()) {
            return new Site.Builder().setHashMap(site).build();
        }
        return null;
    }

    public @Nullable HashMap getCcu() {
        HashMap ccu = CCUHsApi.getInstance().read("device and ccu");
        if (ccu.size() > 0) {
            return ccu;
        } else {
            return null;
        }
    }
    public @Nullable String getCcuName() {
        HashMap ccu = CCUHsApi.getInstance().read("device and ccu");
        if (ccu.size() > 0) {
            return ccu.get("dis").toString();
        } else {
            return null;
        }
    }

    public void log()
    {
        CcuLog.d(TAG_CCU_HS, "" + tagsDb);
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

    public void addCCURefForDiagAndSystemEntities(){
        List<HashMap<Object, Object>> equipMapList = readAllEntities("equip and (diag or (system and not modbus))");
        for(HashMap<Object, Object> equipMap : equipMapList) {
            Equip equip = new Equip.Builder().setHashMap(equipMap).build();
            CCUHsApi.getInstance().updateEquip(equip, equip.getId());
            if( equip.getCcuRef() == null || equip.getCcuRef().isEmpty()) {
                updateCcuRefForDiagPoints(equip);
            }
            updateCcuRefForDiagPointWhileMigration(equip);
        }
    }

    private void updateCcuRefForDiagPointWhileMigration(Equip equip) {
        ArrayList<HashMap<Object, Object>> equipPoints = readAllEntities("point and equipRef == \"" + equip.getId()+"\"");
        for(HashMap<Object, Object> equipPoint : equipPoints){
            if(equipPoint.get("id") == null) continue;
            HDict pointDict = CCUHsApi.getInstance().readHDictById(equipPoint.get("id").toString());
            Point point = new Point.Builder().setHDict(pointDict).build();
            if( point.getCcuRef() == null || point.getCcuRef().isEmpty()) {
                updatePoint(point, point.getId());
            }
        }
    }

    private void updateCcuRefForDiagPoints(Equip diagEquip){
        ArrayList<HashMap<Object, Object>> equipPoints = readAllEntities("point and equipRef == \"" + diagEquip.getId()+"\"");
        for(HashMap<Object, Object> equipPoint : equipPoints){
            Point point = new Point.Builder().setHashMap(equipPoint).build();
            updatePoint(point, point.getId());
        }
    }

    /**
     * @return SiteId HRef
     */
    public HRef getSiteIdRef()
    {
        HDict hDict = new HDictBuilder().add("filter", "site").toDict();
        HGrid site  = getHSClient().call("read", HGridBuilder.dictToGrid(hDict));
        if (site.isEmpty() || site.numRows() == 0) {
            CcuLog.e(TAG, "getSiteIdRef - Site Not Created");
            return null;
        }
        return site.row(0).getRef("id");
    }

    /**
     * Return the global site id if it exists.
     * Otherwise null.
     * This function is going away, since site GUID is now the site same just site ID.
     *
     * @return site id or null
     */
    @Deprecated
    @Nullable
    public String getRemoteSiteIdWithRefSign() {
        HashMap<Object, Object> site = readEntity("site");

        if (site == null || site.get("id") == null) return null;
        return site.get("id").toString();
    }

    /**
     * This methods returns the Local ccuId, not the global ccuId.
     */
    public HRef getCcuRef() {
        HRef siteRef = null;
        HDict hDict = new HDictBuilder().add("filter", "ccu and device").toDict();
        HGrid site  = getHSClient().call("read", HGridBuilder.dictToGrid(hDict));
        if (site != null && site.numRows() > 0) {
            siteRef = site.row(0).getRef("id");
        }
        return siteRef;
    }

    public String getCcuId() {
        HRef ccuId = getCcuRef();
        return ccuId != null ? ccuId.toString() : null;
    }

    private String getHSQueryToReadSpecialSchedule(String zoneId){
        String query = "schedule and building and special";
        if(!StringUtils.isEmpty(zoneId)){
            query = "schedule and special and zone and roomRef == "+zoneId;
        }
        return query;
    }

    public List<HashMap<Object, Object>> getSpecialSchedules(String zoneId){
        String query = getHSQueryToReadSpecialSchedule(zoneId);
        return CCUHsApi.getInstance().readAllEntities(query);
    }

    public BuildingOccupancy getBuildingOccupancy(){
        BuildingOccupancy.Builder builder = new BuildingOccupancy.Builder().setHDict(
                tagsDb.read(Queries.BUILDING_OCCUPANCY, false));
        if (builder == null) {
            return null;
        } else {
            return builder.build();
        }
    }

    public ArrayList<Schedule> getSystemSchedule(boolean vacation)
    {
        ArrayList<Schedule> schedules = new ArrayList<>();
        String              filter    = null;

        if (!vacation) {
            filter = "building and occupancy";
            HGrid scheduleHGrid = tagsDb.readAll(filter);
            for (int i = 0; i < scheduleHGrid.numRows(); i++) {
                schedules.add(new Schedule.Builder().setHDict(scheduleHGrid.row(i)).build());
            }
        }


        if (schedules.isEmpty() || vacation) {
            if (!vacation)
                filter = "schedule and zone and not named and not special and not vacation";
            else
                filter = "schedule and building and not named and not special and vacation";

            HGrid scheduleHGridbuilding = tagsDb.readAll(filter);
            for (int i = 0; i < scheduleHGridbuilding.numRows(); i++)
            {
                schedules.add(new Schedule.Builder().setHDict(scheduleHGridbuilding.row(i)).build());
            }

        }
        return schedules;
    }

    public ArrayList<Schedule> getBuildingOccupancySchedule() {
        ArrayList<Schedule> schedules = new ArrayList<>();
        String  filter = "building and occupancy";
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
            filter = "schedule and zone and not named and not special and not vacation and roomRef == "+zoneId;
        else
            filter = "schedule and zone and not named and not special and vacation and roomRef == "+zoneId;

        CcuLog.d(TAG_CCU_HS," getZoneSchedule : "+filter);
        if(filter != null) {

            HGrid scheduleHGrid = tagsDb.readAll(filter);

            for (int i = 0; i < scheduleHGrid.numRows(); i++) {
                schedules.add(new Schedule.Builder().setHDict(scheduleHGrid.row(i)).build());
            }
        }

        return schedules;
    }

    public ArrayList<Schedule> getAllVacationSchedules() {
        ArrayList<Schedule> schedules = new ArrayList<>();
        HGrid scheduleHGrid = tagsDb.readAll("schedule and vacation");
        for (int i = 0; i < scheduleHGrid.numRows(); i++) {
            schedules.add(new Schedule.Builder().setHDict(scheduleHGrid.row(i)).build());
        }
        return schedules;
    }

    public List<HashMap<Object, Object>> getAllSpecialSchedules(){
        String query = "special and schedule";
        return CCUHsApi.getInstance().readAllEntities(query);
    }

    public void addSchedule(String localId, HDict scheduleDict)
    {
        tagsDb.addHDict(localId, scheduleDict);
        syncStatusService.addUnSyncedEntity(StringUtils.prependIfMissing(localId, "@"));
    }

    public void updateSchedule(String localId, HDict scheduleDict)
    {
        tagsDb.addHDict(localId, scheduleDict);

        CcuLog.i("CCH_HS", "updateScheduleDict: " + scheduleDict.toZinc());
        syncStatusService.addUnSyncedEntity(StringUtils.prependIfMissing(localId, "@"));
    }

    public void updateSchedule(Schedule schedule)
    {
        schedule.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));

        if(schedule.isNamedSchedule())
            tagsDb.addHDict(schedule.getId(), schedule.getNamedScheduleHDict());
        else
            tagsDb.addHDict(schedule.getId(), schedule.getScheduleHDict());

        CcuLog.i("CCH_HS", "updateSchedule: " + schedule.getScheduleHDict().toZinc());
        syncStatusService.addUpdatedEntity(StringUtils.prependIfMissing(schedule.getId(), "@"));
    }

    public void updateBuildingOccupancy(BuildingOccupancy buildingOccupancy)
    {
        tagsDb.addHDict(buildingOccupancy.getId(), buildingOccupancy.getBuildingOccupancyHDict());

        CcuLog.i("CCH_HS", "updateBuildingOccupancy: " + buildingOccupancy.getBuildingOccupancyHDict().toZinc());
        syncStatusService.addUpdatedEntity(StringUtils.prependIfMissing(buildingOccupancy.getId(), "@"));
    }

    public void updateZoneSchedule(Schedule schedule, String zoneId)
    {
        schedule.setLastModifiedDateTime(HDateTime.make(System.currentTimeMillis()));
        tagsDb.addHDict(schedule.getId(), schedule.getZoneScheduleHDict(zoneId));
        CcuLog.i(TAG_CCU_HS, "updateZoneSchedule: " + schedule.getZoneScheduleHDict(zoneId).toZinc());
        syncStatusService.addUpdatedEntity(StringUtils.prependIfMissing(schedule.getId(), "@"));
    }

    public void updateZoneScheduleWithoutUpdatingLastModifiedTime(Schedule schedule, String zoneId) {
        tagsDb.addHDict(schedule.getId(), schedule.getZoneScheduleHDict(zoneId));
        syncStatusService.addUpdatedEntity(StringUtils.prependIfMissing(schedule.getId(), "@"));
    }

    public void updateScheduleNoSync(Schedule schedule, String zoneId) {
        tagsDb.addHDict(schedule.getId(), (zoneId == null ? schedule.getScheduleHDict() : schedule.getZoneScheduleHDict(zoneId)));
        CcuLog.i(TAG_CCU_HS, "updateScheduleNoSync: "+schedule.getId()+" " + (zoneId == null ? schedule.getScheduleHDict().toZinc(): schedule.getZoneScheduleHDict(zoneId).toZinc()));
    }

    public void updateHDictNoSync(String entityId, HDict scheduleDict){
        tagsDb.addHDict(entityId, scheduleDict);
    }

    public void updateSpecialScheduleNoSync(String entityId, HDict scheduleDict){
        tagsDb.addHDict(entityId, scheduleDict);
    }

    public Schedule getScheduleById(String scheduleRef)
    {
        if (scheduleRef == null)
            return null;
        HDict hDict = null;
        try {
            hDict = tagsDb.readById(HRef.copy(scheduleRef));
        } catch (UnknownRecException e) {
            CcuLog.d(TAG_CCU_HS, " getScheduleById : Schedule not found !! - " +scheduleRef);
            importSchedule(scheduleRef);
        }
        return hDict == null ? null : new Schedule.Builder().setHDict(hDict).build() ;
    }

    public HDict getScheduleDictById(String scheduleId){
        if(scheduleId == null){
            return null;
        }
        return tagsDb.readById(HRef.copy(scheduleId));
    }

    public double getPredictedPreconRate(String ahuRef) {
        HClient hClient   = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);

        try
        {
            HDict hDict = new HDictBuilder().add("filter", "equip and virtual and ahuRef == " + ahuRef).toDict();
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
                        CcuLog.d(TAG_CCU_HS, "RemotePreconRate , " + date + " : " + preconVal);
                        return preconVal;
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
            CcuLog.d(TAG_CCU_HS,"getPredictedPreconRate Failed : Fall back to default precon rate");
        }

        return 0;
    }

    public double getExternalTemp() {
        HClient hClient = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        if (tempWeatherRef == null)
        {
            HDict navIdDict = new HDictBuilder().add(HayStackConstants.ID, getSiteIdRef()).toDict();
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
                            //Remove unicode chars and units. 48.32°F ->48.32
                            double tempVal = Double.parseDouble(r.get("val").toString().replaceAll("[^-?\\d.]", ""));
                            CcuLog.d(TAG_CCU_OAO,date+" External Temp: "+tempVal);
                            CcuLog.d("CCU_OAO",date+" External Temp: "+tempVal);
                            mCurrentTemperature = tempVal;
                            return tempVal;

                        }
                    }
                }
            }
        } else {
            if(((appAliveMinutes % 15) == 0) || (appAliveMinutes == 1)){
                HGrid hisGrid = hClient.hisRead(tempWeatherRef, "current");
                if (hisGrid != null && hisGrid.numRows() > 0) {
                    hisGrid.dump();
                    HRow r = hisGrid.row(hisGrid.numRows() - 1);
                    mCurrentTemperature = Double.parseDouble(r.get("val").toString().replaceAll("[^-?\\d.]", ""));
                    return mCurrentTemperature;
                }
            }else{
                return mCurrentTemperature;
            }
        }
        return 0;
    }

    public double getExternalHumidity() {

        HClient hClient = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        if (humidityWeatherRef == null)
        {
            HDict navIdDict = new HDictBuilder().add(HayStackConstants.ID, getSiteIdRef()).toDict();
            HGrid hGrid = HGridBuilder.dictToGrid(navIdDict);
            HGrid site = hClient.call(HStdOps.read.name(), hGrid);
            if(site != null) {
                HVal weatherRef = site.row(0).get("weatherRef", false);
                if (weatherRef != null) {
                    HDict tDict = new HDictBuilder().add("filter", "weatherPoint and humidity and weatherRef == " + weatherRef).toDict();
                    HGrid weatherPoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
                    if (weatherPoint != null && weatherPoint.numRows() > 0) {
                        weatherPoint.dump();
                        humidityWeatherRef = weatherPoint.row(0).getRef("id");
                        HGrid hisGrid = hClient.hisRead(humidityWeatherRef, "current");
                        hisGrid.dump();
                        if (hisGrid != null && hisGrid.numRows() > 0) {
                            HRow r = hisGrid.row(hisGrid.numRows() - 1);
                            HDateTime date = (HDateTime) r.get("ts");
                            double humidityVal = Double.parseDouble(r.get("val").toString().replaceAll("[^\\d.]", ""));
                            CcuLog.d(TAG_CCU_OAO, date + " External Humidity: " + humidityVal);
                            mCurrentHumidity = 100 * humidityVal;
                            return mCurrentHumidity;

                        }
                    }
                }
            }
        } else {
            if(((appAliveMinutes % 15) == 0) || (appAliveMinutes == 1)) {
                HGrid hisGrid = hClient.hisRead(humidityWeatherRef, "current");
                if (hisGrid != null && hisGrid.numRows() > 0) {
                    hisGrid.dump();
                    HRow r = hisGrid.row(hisGrid.numRows() - 1);
                    double humidityVal = Double.parseDouble(r.get("val").toString().replaceAll("[^\\d.]", ""));
                    mCurrentHumidity = 100 * humidityVal;
                    return mCurrentHumidity;
                }
            } else{
                return mCurrentHumidity;
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
        ArrayList<HashMap<Object, Object>> points = readAllEntities("point and his");
        if (points.size() == 0) {
            return;
        }
        for (Map<Object, Object> m : points)
        {
            CcuLog.d(TAG_CCU_HS," deleteHistory for point "+m.get("id"));
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
        setCcuReady();
        CcuLog.d(TAG_CCU_HS, "CCU Registered");
    }

    public void setCcuUnregistered() {
        CcuLog.d(TAG_CCU_HS,"Invoked CCU Unregistered");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("isCcuRegistered");
        editor.commit();
        resetCcuReady();
        CcuLog.d(TAG_CCU_HS, "CCU Unregistered");
    }

    public boolean isNetworkConnected() {

        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        return spDefaultPrefs.getBoolean("75fNetworkAvailable", false);
    }

    public void registerCcu(String installerEmail) {

        HashMap site = CCUHsApi.getInstance().read("site");
        CcuLog.i("CCURegInfo","registerCcu");
        if (siteSynced() && CCUHsApi.getInstance().isNetworkConnected()) {
            CcuLog.d("CCURegInfo","The CCU is not registered, but the site is created with ID " + getSiteIdRef().toString());
            HashMap<Object, Object> ccu = CCUHsApi.getInstance().readEntity("device and ccu");

            String ccuLuid = Objects.toString(ccu.get(CcuFieldConstants.ID),"");
            if (! entitySynced(ccuLuid)) {
                String facilityManagerEmail = site.get(CcuFieldConstants.FACILITY_MANAGER_EMAIL).toString();
                String installEmail = installerEmail;
                if (StringUtils.isBlank(installEmail)) {
                    installEmail = site.get(CcuFieldConstants.INSTALLER_EMAIL).toString();
                }
                String dis = ccu.get("dis").toString();
                String ahuRef = ccu.get("ahuRef").toString();
                String gatewayRef = ccu.get("gatewayRef").toString();
                String equipRef = ccu.get("equipRef").toString();

                JSONObject ccuRegistrationRequest = getCcuRegisterJson(ccuLuid, getSiteIdRef().toString(),
                        dis, ahuRef, gatewayRef, equipRef, facilityManagerEmail, installEmail, null);
                if (ccuRegistrationRequest != null) {
                    CcuLog.d("CCURegInfo","Sending CCU registration request: " + ccuRegistrationRequest);
                    String ccuRegistrationResponse = HttpUtil.executeJson(
                            CCUHsApi.getInstance().getAuthenticationUrl()+"devices",
                            ccuRegistrationRequest.toString(),
                            BuildConfig.CARETAKER_API_KEY,
                            true,
                            HttpConstants.HTTP_METHOD_POST
                    );
                    CcuLog.d("CCURegInfo","Registration response: " + ccuRegistrationResponse);

                    if (ccuRegistrationResponse != null) {
                        completeRegistration(ccuRegistrationResponse, ccuLuid);
                    }
                }
            } else {
                CcuLog.d("CCURegInfo","The CCU is synced, id: " + ccuLuid + " and the token is " + CCUHsApi.getInstance().getJwt());
                // TODO Matt Rudd - Need mechanism to handle the token being null here but the GUID existing; may happen in edge cases
                CCUHsApi.getInstance().setCcuRegistered();
                if (StringUtils.isBlank(CCUHsApi.getInstance().getJwt())) {
                    CcuLog.e("CCURegInfo", "There was a fatal error registering the CCU. The GUID is set, but the token is unavailable.");
                }
            }
        } else {
            CcuLog.e("CCURegInfo","Registration cannot be completed now  - siteSynced : "+siteSynced());
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "CCU cannot be completed at the moment. Please complete registration by clicking REGISTER button ", LENGTH_LONG).show();
                importNamedSchedule(hsClient);
            });
        }
    }

    /**
     * Get Json for ccu sync call.  id is not needed for an update call and can be null in that case.
     */
    public JSONObject getCcuRegisterJson(
            @Nullable String id,
            String siteRef,
            String ccuDescription,
            String ahuRef,
            String gatewayRef,
            String equipRef,
            String facilityManagerEmail,
            String installerEmail,
            String buildingTuneId) {
        JSONObject ccuJsonRequest = null;

        try {
            ccuJsonRequest = new JSONObject();

            if (id != null) {
                ccuJsonRequest.put(CcuFieldConstants.ID, id);
            }

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

            if (buildingTuneId != null) {
                HashMap<Object, Object> tunerEquip = CCUHsApi.getInstance().readEntity("equip and tuner");
                if (!tunerEquip.isEmpty()) {
                    JSONObject tunerFiled = new JSONObject();
                    tunerFiled.put(CcuFieldConstants.MODEL_ID, tunerEquip.get(CcuFieldConstants.SOURCE_MODEL));
                    tunerFiled.put(CcuFieldConstants.MODEL_VERSION, tunerEquip.get(CcuFieldConstants.SOURCE_MODEL_VERSION));
                    tunerFiled.put(CcuFieldConstants.BUILDING_TUNER_ID, buildingTuneId);
                    ccuJsonRequest.put(CcuFieldConstants.TUNER, tunerFiled);
                }
            }

        } catch (JSONException jsonException) {
            ccuJsonRequest = null;
            CcuLog.e("CCURegInfo","Unable to construct a valid CCU registration request", jsonException);
        }

        return ccuJsonRequest;
    }

    public void completeRegistration(String ccuRegistrationResponse, String ccuId) {
        CcuLog.d("CCURegInfo", "completeRegistration"+ccuRegistrationResponse);
        try {
            JSONObject ccuRegistrationResponseJson = new JSONObject(ccuRegistrationResponse);
            String ccuGuid = ccuRegistrationResponseJson.getString("id");
            String token = ccuRegistrationResponseJson.getString("token");
            setSynced(ccuId);
            setJwt(token);
            setCcuRegistered();
            CcuLog.d("CCURegInfo","CCU was successfully registered with ID " + ccuGuid + "; token " + token);
            defaultSharedPrefs.edit().putLong("ccuRegistrationTimeStamp", System.currentTimeMillis()).apply();
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "CCU Registered Successfully ", LENGTH_LONG).show();
                importNamedSchedule(hsClient);
            });
            publishRegistrationSuccessful();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void publishRegistrationSuccessful() {
        CcuLog.d("CCURegInfo", "RegistrationCompletedListeners count "+onCcuRegistrationCompletedListeners.size());
        onCcuRegistrationCompletedListeners.forEach( listener -> {
            listener.onRegistrationCompleted(this);
            CcuLog.d("CCURegInfo", "RegistrationCompletedListener "+listener);
        } );
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
        String ccuId = getCcuId();
        return ccuId == null ? Tags.CCU : Tags.CCU+"_"+ccuId;
    }

    public String getTimeZone() {
        HashMap siteMap = read(Tags.SITE);
        return siteMap.get("tz").toString();
    }

    /**
     * Converts a Haystack Grid to Standard Java Collection.
     * @param grid
     * @return
     */
    public List<HashMap> HGridToList(HGrid grid) {
        List<HashMap> rowList = new ArrayList<>();
        if (grid != null) {
            Iterator it = grid.iterator();
            while (it != null && it.hasNext()) {
                HashMap<Object, Object> map = new HashMap<>();
                HRow r   = (HRow) it.next();
                HRow.RowIterator        ri  = (HRow.RowIterator) r.iterator();
                while (ri!= null && ri.hasNext()) {
                    HDict.MapEntry m = (HDict.MapEntry) ri.next();
                    map.put(m.getKey(), m.getValue());
                }
                rowList.add(map);
            }
        }
        return rowList;
    }

    public List<HashMap> HGridToListPlainString(HGrid grid) {
        List<HashMap> rowList = new ArrayList<>();
        if (grid != null) {
            Iterator it = grid.iterator();
            while (it != null && it.hasNext()) {
                HashMap<String, String> map = new HashMap<>();
                HRow r   = (HRow) it.next();
                HRow.RowIterator        ri  = (HRow.RowIterator) r.iterator();
                while (ri!= null && ri.hasNext()) {
                    HDict.MapEntry m = (HDict.MapEntry) ri.next();
                    map.put(m.getKey().toString(), m.getValue().toString());
                }
                rowList.add(map);
            }
        }
        return rowList;
    }

    public boolean hasEntitySynced(String id) {
        return syncStatusService.hasEntitySynced(id);
    }

    public boolean isEligibleForSync(String id) {
        return syncStatusService.isEligibleForSync(id);
    }

    public boolean isEntityExisting(String id) {
        return tagsDb.isEntityExisting(HRef.copy(id));
    }

    public void setEntitySynced(String id) {
        syncStatusService.setEntitySynced(id);
    }

    public void addEntity(HDict entity) {
        HRef id = (HRef) entity.get(Tags.ID);
        tagsDb.tagsMap.put(id.toVal(), entity);
    }

    public ConcurrentHashMap<String, String> getIdMap() {
        return tagsDb.idMap;
    }

    public ConcurrentHashMap<String, String> getUpdateIdMap() {
        return tagsDb.updateIdMap;
    }

    public ConcurrentHashMap<String, String> getRemoveIdMap() {
        return tagsDb.removeIdMap;
    }

    public SyncStatusService getSyncStatusService() {
        return syncStatusService;
    }

    public HGrid readGrid(String query) {
        HGrid grid = null;
        try {
            grid = hsClient.readAll(query);
        }
        catch (UnknownRecException e) {
            e.printStackTrace();
        }
        return grid;
    }

    public void trimObjectBoxHisStore() {
        hisSyncHandler.doPurge();
    }


    /*
     * A flag used to indicate CCU is ready for CPU intensive processing of sensor events and hvac algorithms.
     * This could be used as a last ditch effort make CPU available for any fore-ground operation that
     * can result in bad user experience.
     * Currently used while initial loading of zone screen.
     */
    public boolean isCcuReady() {
        return isCcuReady;
    }
    public void setCcuReady() {
        if (isCCURegistered()) {
            isCcuReady = true;
        }
    }

    public void resetCcuReady() {
        isCcuReady = false;
    }

    /**
     * Checks if there is valid Site and CCU entities in database.
     * @return
     */
    public boolean isCCUConfigured() {

        HashMap<Object, Object> site = readEntity("site");
        if (site.isEmpty()) {
            return false;
        }

        HashMap<Object, Object> ccu = readEntity("ccu");
        return !ccu.isEmpty();
    }

    /**
     * Removes CCU Entity from backend.
     * @param ccuId - ccu Ref
     * @return Http response
     */
    public String removeCCURemote(String ccuId) {
        HDictBuilder b = new HDictBuilder()
                             .add("ccuId", HRef.copy(ccuId));
        HDict[] dictArr = {b.toDict()};
        CcuLog.d(TAG_CCU_HS, "removeCCU API call");
        return HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "removeCCU/",
                                    HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
    }

    /**
     * Re-sync all the entities on this CCU except site and ccu-device entities.
     */
    public void resyncSiteTree() {

        markItemsUnSynced(readAllEntities(Tags.DEVICE+" and not "+Tags.CCU));
        markItemsUnSynced(readAllEntities(Tags.EQUIP+" and not "+Tags.TUNER));
        markItemsUnSynced(readAllEntities(Tags.FLOOR));
        markItemsUnSynced(readAllEntities(Tags.ROOM));
        markItemsUnSynced(readAllEntities(Tags.SCHEDULE+" and "+Tags.ZONE));

        ArrayList<HashMap<Object, Object>> pointsForSync = new ArrayList<>();

        readAllEntities(Tags.POINT).forEach( point -> {
            if (point.get("equipRef") != null && !readMapById(point.get("equipRef").toString()).containsKey("tuner")) {
                pointsForSync.add(point);
            }
        });
        markItemsUnSynced(pointsForSync);

        syncStatusService.saveSyncStatus();
        syncEntityTree();
        hisSyncHandler.scheduleSync(true, 60);
    }

    private void markItemsUnSynced(List<HashMap<Object, Object>> entities) {
        entities.forEach( entity -> {
            String entityId = Objects.requireNonNull(entity.get(Tags.ID)).toString();
            if (!entityId.isEmpty()) {
                syncStatusService.addUnSyncedEntity(entityId);
            }
        });
    }

    public void updateNamedSchedule(String scheduleId, HDict scheduleDict){
        tagsDb.addHDict(scheduleId,scheduleDict);
    }

    public List<HashMap<Object, Object>> getAllNamedSchedules(){
        String query = "named and schedule";
        List<HashMap<Object, Object>> sortedNamedSchedules = CCUHsApi.getInstance().readAllEntities(query);

        Collections.sort(sortedNamedSchedules, (o1, o2) -> (o1.get("dis").toString()).compareTo(o2.get("dis").toString()));
        return sortedNamedSchedules;
    }

    public void removeAllNamedSchedule(){
        List<HashMap<Object, Object>> allNamedSchedules = CCUHsApi.getInstance().getAllNamedSchedules();
        for (HashMap<Object, Object> namedSchedule:allNamedSchedules) {
            deleteEntityLocally(namedSchedule.get("id").toString().replace("@",""));
        }
    }
    
    /**
     * This is currently called from BuildingProcessJob which gets scheduled every minute.
     * Avoiding the need for an additional timer thread at the cost of a dependency on logic module.
     */
    public void incrementAppAliveCount() {
        appAliveMinutes++;
    }
    
    /**
     * Returns the number of minutes the app has been alive.
     * @return appAlive minutes
     */
    public long getAppAliveMinutes() {
        return appAliveMinutes;
    }
    
    public HisSyncHandler getHisSyncHandler() {
        return hisSyncHandler;
    }

    public String fetchRemoteEntity(String uid) {
        HDictBuilder b = new HDictBuilder().add("id", HRef.copy(uid));
        HDict[] dictArr = {b.toDict()};
        return HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "read",
                HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));

    }

    public String deleteRemoteEntity(String uid) {
        HDictBuilder b = new HDictBuilder().add("id", HRef.copy(uid));
        HDict[] dictArr = {b.toDict()};
        return HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "removeEntity",
                HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));

    }

    public void importSchedule(String id) {
        ExecutorTask.executeBackground( () -> {
            String response = fetchRemoteEntity(id);
            if (response != null) {
                HZincReader hZincReader = new HZincReader(response);
                Iterator hZincReaderIterator = hZincReader.readGrid().iterator();
                while (hZincReaderIterator.hasNext()) {
                    HRow row = (HRow) hZincReaderIterator.next();
                    tagsDb.addHDict((row.get("id").toString()).replace("@", ""), row);
                    CcuLog.i(TAG_CCU_HS, "Schedule Imported " + row);
                }
            }
        });
    }

    public void updateSchedulable(HGrid zoneScheduleGrid,boolean isZone) {
        if (zoneScheduleGrid != null) {
            CcuLog.d("CCU_SCHEDULABLE", "in updateSchedulabe = " + zoneScheduleGrid.numRows());

            CCUHsApi ccuHsApi = CCUHsApi.getInstance();
            List<HashMap> pointMaps = ccuHsApi.HGridToList(zoneScheduleGrid);
            ArrayList<Point> points = new ArrayList<>();
            pointMaps.forEach(m -> points.add(new Point.Builder().setHashMap(m).build()));

            for (Point p : points) {
                if(isZone) p.setCcuRef(getCcuId());
                String pointLuid = ccuHsApi.addRemotePoint(p, p.getId().replace("@", ""));
                updatePoint(p,pointLuid);
            }
        }
        syncEntityTree();

    }

    public ArrayList<HashMap<Object, Object>> readAllSchedulable(){
        return CCUHsApi.getInstance().readAllEntities("schedulable");
    }

    public boolean importPointArrays(List<HDict> hDicts) {
        ExecutorTask.executeBackground( () -> {
            HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
            if (hClient == null) {
                hClient = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
            }

            int partitionSize = 25;
            List<List<HDict>> partitions = new ArrayList<>();
            for (int i = 0; i < hDicts.size(); i += partitionSize) {
                partitions.add(hDicts.subList(i, Math.min(i + partitionSize, hDicts.size())));
            }

            for (List<HDict> sublist : partitions) {
                HGrid writableArrayPoints = hClient.call("pointWriteMany",
                        HGridBuilder.dictsToGrid(sublist.toArray(new HDict[sublist.size()])));

                //We cannot proceed adding new CCU to existing Site without fetching all the point array values.
                if (writableArrayPoints == null) {
                    CcuLog.e(TAG, "Failed to fetch point array values while importing existing data.");
                    return;
                }

                ArrayList<HDict> hDictList = new ArrayList<>();

                Iterator rowIterator = writableArrayPoints.iterator();
                while (rowIterator.hasNext()) {
                    HRow row = (HRow) rowIterator.next();
                    String id = row.get("id").toString();
                    String kind = row.get("kind").toString();
                    HVal data = row.get("data");
                    //  CcuLog.i(TAG, "Import point array " + row);
                    if (data instanceof HList && ((HList) data).size() > 0) {
                        HList dataList = (HList) data;

                        for (int i = 0; i < dataList.size(); i++) {
                            HDict dataElement = (HDict) dataList.get(i);

                            String who = dataElement.getStr("who");
                            String level = dataElement.get("level").toString();
                            HVal val = dataElement.get("val");
                            Object lastModifiedTimeTag = dataElement.get("lastModifiedDateTime", false);

                            HDictBuilder pid = new HDictBuilder().add("id", HRef.copy(id))
                                    .add("level", Integer.parseInt(level))
                                    .add("who", who)
                                    .add("val", kind.equals(Kind.STRING.getValue()) ?
                                            HStr.make(val.toString()) : val);
                            HDateTime lastModifiedDateTime;
                            if (lastModifiedTimeTag != null) {
                                lastModifiedDateTime = (HDateTime) lastModifiedTimeTag;
                            } else {
                                lastModifiedDateTime = HDateTime.make(System.currentTimeMillis());
                            }
                            pid.add("lastModifiedDateTime", lastModifiedDateTime);
                            hDictList.add(pid.toDict());
                            try {
                                HDict rec = hsClient.readById(HRef.copy(id));
                                //save points on tagsDb
                                tagsDb.onPointWrite(rec, Integer.parseInt(level),
                                        kind.equals(Kind.STRING.getValue()) ? HStr.make(val.toString()) :
                                                val, who, HNum.make(0), rec, lastModifiedDateTime);

                                //save his data to local cache
                                tagsDb.saveHisItems(hsClient.readById(HRef.copy(id)),
                                        new HHisItem[]{HHisItem.make(HDateTime.make(System.currentTimeMillis()),
                                                HStr.make(String.valueOf(HSUtil.getPriorityVal(id))))},
                                        true);
                            } catch (UnknownRecException e) {
                                CcuLog.e(TAG, "Import point array failed " + id, e);
                            }

                        }
                    }

                }
                hClient.call("pointWriteMany", HGridBuilder.dictsToGrid(hDictList.toArray(new HDict[hDictList.size()])));
            }
        });
        return true;
    }

    public boolean getAuthorised(){
        return isAuthorized;
    }
    public void updateTimeZoneInBackground(String tz) {
        CCUHsApi.getInstance().updateTimeZone(tz);
        String[] tzIds = TimeZone.getAvailableIDs();
        for (String timeZone : tzIds) {
            if (timeZone.contains(tz)) {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                am.setTimeZone(timeZone);
                DateTimeZone.setDefault(DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZone)));
                break;
            }
        }
        CCUHsApi.getInstance().syncEntityTree();
    }

    public Schedule getRemoteSchedule(String pointUid){
        String response = CCUHsApi.getInstance().fetchRemoteEntity(pointUid);
        if(response == null || response.isEmpty()){
            CcuLog.d(TAG, "Failed to read remote schedule : " + response);
            return null;
        }
        HGrid sGrid = new HZincReader(response).readGrid();
        Iterator it = sGrid.iterator();
        HRow r = (HRow) it.next();
        HDict scheduleDict = new HDictBuilder().add(r).toDict();
        return new Schedule.Builder().setHDict(scheduleDict).build();
    }

    public HDict readRemotePoint(String query){
        String response = CCUHsApi.getInstance().fetchRemoteEntityByQuery(query);
        if(response == null || response.isEmpty()){
            CcuLog.d(TAG, "Failed to read remote entity : " + response);
            return null;
        }
        HGrid sGrid = new HZincReader(response).readGrid();
        Iterator it = sGrid.iterator();
        HRow r = (HRow) it.next();
        return new HDictBuilder().add(r).toDict();
    }

    public HDict readRemotePointById(String id){
        HGrid sGrid = readPointArrRemote(id);
        if (sGrid != null) {
            Iterator it = sGrid.iterator();
            HRow r = (HRow) it.next();
            return new HDictBuilder().add(r).toDict();
        }
        return null;
    }

    public String fetchRemoteEntityByQuery(String query) {
        HDictBuilder b = new HDictBuilder().add("filter", query);
        HDict[] dictArr = {b.toDict()};
        return HttpUtil.executePost(getHSUrl() + "read",
                HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
    }

    /**
     * Currently updates validity only in terms of expiry.
     * This should be called after every token refresh to update the cached variable isAuthorized.
     * @return
     */
    public void updateJwtValidity() {
        String tokenString = getJwt();
        if (StringUtils.isNotEmpty(tokenString)) {
            try {
                JwtValidator jwt = new JwtValidator(tokenString);
                isAuthorized = jwt.isValid();
            } catch (JwtValidationException e) {
                e.printStackTrace();
                CcuLog.e(TAG, "Failed to validate Jwt", e);
                isAuthorized = true; //Failure to decode token will be considered as "authorized" to
                //reduce business risk.
            }
        } else {
            isAuthorized = false;
            CcuLog.e(TAG, "updateJwtValidity : Token does not exist");
        }
        CcuLog.i(TAG, "updateJwtValidity : "+isAuthorized);
    }

    public Double readDefaultValByLevel(String id, int level)
    {
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(level - 1));
            return valMap.get("val") == null ? 0 : Double.parseDouble(valMap.get("val").toString());
        } else
        {
            return 0.0;
        }
    }
    public void writeDefaultTunerValById(String id, double val) {
        double currentVal = readDefaultValByLevel(id, HayStackConstants.DEFAULT_INIT_VAL_LEVEL);
        if ((currentVal == 0) || (currentVal != val)) {
            pointWrite(HRef.copy(id), HayStackConstants.DEFAULT_INIT_VAL_LEVEL, getCCUUserName(), HNum.make(val), HNum.make(0));
        }
    }
    public String readPointArr(String id) {

        ArrayList values = readPoint(id);
        ArrayList<HashMap> resultPointArray = new ArrayList<>();
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    resultPointArray.add(valMap);
                }
            }
        }else{
            return HZincWriter.gridToString(HGrid.EMPTY);
        }

        HGridBuilder b = new HGridBuilder();
        b.addCol("level");
        b.addCol("val");
        b.addCol("who");
        b.addCol("duration");
        b.addCol("lastModifiedDateTime");

        for (int ii = 0; ii < resultPointArray.size(); ii++) {
            HashMap pointArray = resultPointArray.get(ii);
            int level = Integer.parseInt(pointArray.get("level").toString());
            String val= pointArray.get("val").toString();
            String who= (pointArray.containsKey("who")) ?pointArray.get("who").toString()  : null ;
            b.addRow(new HVal[] {
                    HNum.make(level),
                    HNum.make(Double.parseDouble(val)),
                    HStr.make(who),
                    HNum.make(Double.parseDouble(pointArray.get("duration").toString())),
                    HDateTime.make(pointArray.get("lastModifiedDateTime").toString())
            });
        }
        return HZincWriter.gridToString(b.toGrid());
    }

    public interface OnCcuRegistrationCompletedListener {
        void onRegistrationCompleted(CCUHsApi hsApi);
    }
    public void registerOnCcuRegistrationCompletedListener(OnCcuRegistrationCompletedListener listener) {
        if (isCCURegistered()) {
            CcuLog.i(TAG_CCU_HS,"CCU Already registered "+listener);
            listener.onRegistrationCompleted(this);
        } else {
            onCcuRegistrationCompletedListeners.add(listener);
        }
    }
    public void unRegisterOnCcuRegistrationCompletedListener(OnCcuRegistrationCompletedListener listener) {
        onCcuRegistrationCompletedListeners.remove(listener);
    }

    public Context getContext() {
        return context;
    }
    public HashMap<Object, Object> readDefaultPointByDomainName(String domainName) {
        return readEntity("point and default and domainName == \""+domainName+"\"");
    }
    private HGrid invokeWithRetry(String op, HClient hClient, HGrid req){
        RetryCountCallback retryCountCallback = retryCount -> CcuLog.i("CCU_SCHEDULABLE", "retrying to get CCU list with the retry count " + retryCount);
        HGrid responseHGrid;
        try{
            responseHGrid = hClient.invoke(op, req,retryCountCallback);
        }
        catch(Exception exception){
            try {
                if (exception instanceof SocketTimeoutException) {
                    CcuLog.i(TAG, "SocketTimeoutException occurred, hence retrying " + op);
                    responseHGrid = hClient.invoke(op, req,retryCountCallback);

                } else if (exception instanceof IOException) {
                    CcuLog.i(TAG, exception.getClass().getSimpleName() +" occurred, while " + op + " waiting " +
                            "for 30 seconds.....");
                    Thread.sleep(30000);
                    CcuLog.i(TAG, " retrying after 30 seconds.....");
                    responseHGrid = hClient.invoke(op, req,retryCountCallback);
                }
                else{
                    exception.printStackTrace();
                    CcuLog.i(TAG, "Exception occurred while calling "+op );
                    throw new NullHGridException("Exception occurred while calling "+op);
                }
            }
            catch(Exception ex){
                ex.printStackTrace();
                CcuLog.i(TAG, "Exception occurred while calling "+op);
                throw new NullHGridException("Exception occurred while calling "+op);
            }

        }
        return responseHGrid;
    }

    public void updateLocalTimeZone(){
        String tz = Objects.requireNonNull(CCUHsApi.getInstance().getSite()).getTz();
        String[] tzIds = TimeZone.getAvailableIDs();
        for (String timeZone : tzIds) {
            if (timeZone.contains(tz)) {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                am.setTimeZone(timeZone);
                DateTimeZone.setDefault(DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZone)));
                CcuLog.e(TAG, "updateLocalTimeZone : local timezone updated to "+timeZone);
                break;
            }
        }
    }

    public HashMap<Object, Object> getRemoteBuildingTunerEquip(String siteId) {
        HClient hClient = new HClient(getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);

        HDict tunerEquipDict = new HDictBuilder().add("filter",
                "tuner and equip and siteRef == " + siteId).toDict();
        HGrid tunerEquipGrid = hClient.call("read", HGridBuilder.dictToGrid(tunerEquipDict));
        if (tunerEquipGrid != null) {
            tunerEquipGrid.dump();
            List<HashMap> equipMaps = HGridToList(tunerEquipGrid);
            return equipMaps.get(0);
        }
        CcuLog.e(TAG_CCU_HS, "Failed to fetch BuildingTuner equip");
        return null;
    }

    public void updatePointWithoutUpdatingLastModifiedTime(Point point, String id) {
        if(!isBuildingTunerPoint(point)){
            point.setCcuRef(getCcuId());
        }
        tagsDb.updatePoint(point, id);
        if (syncStatusService.hasEntitySynced(id)) {
            syncStatusService.addUpdatedEntity(id);
        }
    }
    public boolean isBuildingTunerPoint(String id) {
        HashMap<Object, Object> point = readMapById(id);
        if (point.isEmpty()) {
            return false;
        }
        return isBuildingTunerPoint(new Point.Builder().setHashMap(point).build());
    }
    public int getCacheSyncFrequency() {
        return context.getSharedPreferences("ccu_devsetting", Context.MODE_PRIVATE)
                .getInt("cacheSyncFrequency", 1);
    }

    public Schedule getDefaultNamedSchedule() {
        try {
            HDict scheduleHGrid = tagsDb.read("default and named and schedule");
            if(scheduleHGrid != null) {
                return new Schedule.Builder().setHDict(scheduleHGrid).build();
            }
        }catch (UnknownRecException e){
            CcuLog.e(TAG,"Default Named Schedule is empty");
            e.printStackTrace();
        }

        return null;
    }

    public void setCcuLogLevel(double ccuLogLevel) {
        CCUHsApi.ccuLogLevel = (int) ccuLogLevel;
    }
    public int getCcuLogLevel() {
        try {
            if(ccuLogLevel >= 0){
                return ccuLogLevel;
            }
            if(!isCCURegistered())
            return 0;
            HashMap<Object, Object>  logLevel = readEntityByDomainName("logLevel");
            if(logLevel.isEmpty())
                return 0;
            ccuLogLevel =  CCUHsApi.getInstance().readHisValById(logLevel.get("id").toString()).intValue();
            return ccuLogLevel;
        } catch (IllegalStateException e) {
            CcuLog.e("CcuLog", "hayStack is not initialized");
            return 0;
        }

    }

    public void syncEntity(String id) {
            syncStatusService.addUnSyncedEntity(id);
    }
    public void clearAllAvailableLevelsInPoint(String id) {
        if (HSUtil.readPointPriorityValWithNull(id) != null) {
            List<HDict> hDictArrayList = new ArrayList<>();
            for (int i = 1; i <= 16; i++) {
                HDictBuilder hDictBuilder = new HDictBuilder()
                        .add("id", HRef.copy(id))
                        .add("level", i)
                        .add("who", CCUHsApi.getInstance().getCCUUserName())
                        .add("duration", HNum.make(0, "ms"))
                        .add("val", (HVal) null);
                hDictArrayList.add(hDictBuilder.toDict());
                deletePointArrayLevel(id, i);
            }
            HGrid hGrid = HGridBuilder.dictsToGrid(hDictArrayList.toArray(new HDict[hDictArrayList.size()]));
            EntitySyncResponse e = HttpUtil.executeEntitySync(
                    CCUHsApi.getInstance().pointWriteManyTarget(),
                    HZincWriter.gridToString(hGrid), CCUHsApi.getInstance().getJwt());
            CcuLog.d(TAG, "clear All Available Levels In Point : " + "Response " + e.getRespString() + " Error" + e.getErrRespString());
        } else {
            CcuLog.d(TAG, "Available levels are  cleared for point " + id);
        }
    }

    public String defaultStrValByDomainName(String domainName) {
        return readDefaultStrVal("point and domainName == \""+domainName+"\"");
    }
    public void writeDefaultStrValByDomainName(String domainName, String val) {
        writeDefaultVal("point and domainName == \""+domainName+"\"", val);
    }

    public HashMap<Object, Object> readEntityByDomainName (String domainName) {
        return readEntity("domainName == \""+domainName+"\"");
    }

    public List<HDict> readRemoteEntitiesByQuery(String query) {
        String response = CCUHsApi.getInstance().fetchRemoteEntityByQuery(query);
        if(response == null || response.isEmpty()){
            CcuLog.d(TAG, "Failed to read remote entity : " + response);
            return null;
        }
        Iterator it = new HZincReader(response).readGrid().iterator();
        List<HDict> dictList = new ArrayList<>();
        while(it.hasNext()) {
            dictList.add(new HDictBuilder().add((HRow) it.next()).toDict());
        }
        return dictList;
    }

    /**
     * Supports dynamic detection of writable points and updating both writable and his vals.
     * @param pointMap
     */
    public void writePointValue(Map<Object, Object> pointMap, double value) {
        if (pointMap == null || pointMap.isEmpty()) {
            CcuLog.e(TAG, "writePointValue : pointMap is empty , value : " + value);
            return;
        }
        String pointId = pointMap.get("id").toString();
        if (pointMap.containsKey(Tags.WRITABLE)) {
            writeDefaultValById(pointId, value);
            writeHisValById(pointId, readPointPriorityVal(pointId));
        } else {
            writeHisValById(pointId, value);
        }
    }
}
