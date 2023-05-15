package a75f.io.api.haystack;

import android.content.Context;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
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
import org.projecthaystack.server.HStdOps;
import org.projecthaystack.server.HStdOps;

import java.io.IOException;
import java.net.SocketTimeoutException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import a75f.io.api.haystack.exception.NullHGridException;
import a75f.io.api.haystack.sync.EntityParser;
import a75f.io.api.haystack.sync.HisSyncHandler;
import a75f.io.api.haystack.sync.SyncStatusService;
import a75f.io.logger.CcuLog;

public class RestoreCCUHsApi {

    private static RestoreCCUHsApi instance;
    private Context context;
    private AndroidHSClient hsClient;
    private CCUTagsDb tagsDb;
    private SyncStatusService syncStatusService;
    private HisSyncHandler hisSyncHandler;
    private CCUHsApi ccuHsApi;

    public static final String TAG = "CCU_REPLACE";

    public static RestoreCCUHsApi getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Hay stack api is not initialized");
        }
        return instance;
    }

    public RestoreCCUHsApi(){
        if (instance != null) {
            throw new IllegalStateException("Api instance already created , use getInstance()");
        }
        ccuHsApi = CCUHsApi.getInstance();
        context = ccuHsApi.context;
        hsClient = ccuHsApi.hsClient;
        tagsDb = ccuHsApi.tagsDb;
        instance = this;
        //entitySyncHandler = ccuHsApi.entitySyncHandler;

        syncStatusService = SyncStatusService.getInstance(context);

        hisSyncHandler = ccuHsApi.getHisSyncHandler();
    }

    public void importZoneSpecialSchedule(Set<String> zoneRefSet, RetryCountCallback retryCountCallback){
        StringBuilder zoneRefString = new StringBuilder("(");
        int index = 0;
        for(String zoneRef : zoneRefSet){
            zoneRefString.append("roomRef == ");
            zoneRefString.append(StringUtils.prependIfMissing(zoneRef, "@"));
            if(index == zoneRefSet.size()-1){
                zoneRefString.append(" ) ");
            }
            else{
                zoneRefString.append(" or ");
            }
            index++;
        }
        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict zoneScheduleDict = new HDictBuilder().add("filter",
                "schedule and zone and special and " + zoneRefString).toDict();
        HGrid zoneScheduleGrid = invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(zoneScheduleDict),
                retryCountCallback);
        if (zoneScheduleGrid == null) {
            return;
        }
        Iterator it = zoneScheduleGrid.iterator();
        while (it.hasNext()) {
            HRow row = (HRow) it.next();
            String scheduleId = row.get("id").toString();
            tagsDb.addHDict(scheduleId.replace("@", ""),  new HDictBuilder().add(row).toDict());
            syncStatusService.addUnSyncedEntity(StringUtils.prependIfMissing(scheduleId, "@"));
            ccuHsApi.setSynced(StringUtils.prependIfMissing(scheduleId, "@"));
            CcuLog.i(TAG,"Zone Special schedule Imported");
        }
    }

    public void importZoneSchedule(Set<String> zoneRefSet, RetryCountCallback retryCountCallback){
        Log.i(TAG, "Importing Zone  schedule started");
        StringBuffer zoneRefString = new StringBuffer("(");
        int index = 0;
        for(String zoneRef : zoneRefSet){
            zoneRefString.append("roomRef == ");
            zoneRefString.append(StringUtils.prependIfMissing(zoneRef, "@"));
            if(index == zoneRefSet.size()-1){
                zoneRefString.append(" ) ");
            }
            else{
                zoneRefString.append(" or ");
            }
            index++;
        }
        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict zoneScheduleDict = new HDictBuilder().add("filter",
                "schedule and zone and not special and " + zoneRefString).toDict();
        HGrid zoneScheduleGrid = invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(zoneScheduleDict),
                retryCountCallback);
        if (zoneScheduleGrid == null) {
            return;
        }
        Iterator it = zoneScheduleGrid.iterator();
        while (it.hasNext()) {
            HRow r = (HRow) it.next();
            Schedule zoneSchedule =  new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build();
            String guid = zoneSchedule.getId();
            ccuHsApi.addSchedule(guid, zoneSchedule.getZoneScheduleHDict(zoneSchedule.getRoomRef()));
            ccuHsApi.setSynced(StringUtils.prependIfMissing(guid, "@"));
        }
        Log.i(TAG, " Importing Zone  schedule completed");
    }

    public HGrid getAllCCUs(String siteId, RetryCountCallback retryCountCallback){
        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        Log.i("CCU_REPLACE","query "+"ccu and siteRef == " + StringUtils.prependIfMissing(siteId, "@"));
        HDict ccuDict = new HDictBuilder().add("filter",
                "ccu and siteRef == " + StringUtils.prependIfMissing(siteId, "@")).toDict();
        return invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(ccuDict), retryCountCallback);
    }

    public HGrid getAllEquips(String ahuRef, String gatewayRef, RetryCountCallback retryCountCallback){
        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict ccuDict = new HDictBuilder().add("filter",
                "equip and not diag and (gatewayRef == " + StringUtils.prependIfMissing(gatewayRef, "@") +" or ahuRef" +
                        " == "+
                        StringUtils.prependIfMissing(ahuRef, "@")+")").toDict();
        return invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(ccuDict), retryCountCallback);
    }

    public void importFloors(Set<String> floorRefSet, RetryCountCallback retryCountCallback) {
        Log.i(TAG, " Importing floor started");
        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict[] dictArr = new HDict[floorRefSet.size()];
        int index = 0;
        for (String floorRef : floorRefSet) {
            HDictBuilder hDictBuilder = new HDictBuilder().add("id", HRef.copy(floorRef));
            dictArr[index] = hDictBuilder.toDict();
            index++;
        }
        HGrid floorGrid = invokeWithRetry("read", hClient, HGridBuilder.dictsToGrid(dictArr), retryCountCallback);
        List<HashMap> zoneMaps = ccuHsApi.HGridToList(floorGrid);
        List<Floor> floors = new ArrayList<>();
        zoneMaps.forEach(m -> floors.add(new Floor.Builder().setHashMap(m).build()));
        for (Floor floor : floors) {
            String floorLuid = ccuHsApi.addRemoteFloor(floor, floor.getId().replace("@", ""));
            CCUHsApi.getInstance().setSynced(StringUtils.prependIfMissing(floorLuid, "@"));
        }
        Log.i(TAG, " Importing floor completed");
    }

    public void importZones(Set<String> zoneRefSet, RetryCountCallback retryCountCallback){
        Log.i(TAG, " Importing Zone started");
        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict[] dictArr = new HDict[zoneRefSet.size()];
        int index = 0;
        for(String zoneRef : zoneRefSet){
            HDictBuilder hDictBuilder = new HDictBuilder().add("id", HRef.copy(zoneRef));
            dictArr[index] = hDictBuilder.toDict();
            index++;
        }
        HGrid zoneGrid = invokeWithRetry("read", hClient, HGridBuilder.dictsToGrid(dictArr), retryCountCallback);
        List<HashMap> zoneMaps = ccuHsApi.HGridToList(zoneGrid);
        List<Zone> zones = new ArrayList<>();
        zoneMaps.forEach(m -> zones.add(new Zone.Builder().setHashMap(m).build()));
        for(Zone zone : zones){
            String floorLuid = ccuHsApi.addRemoteZone(zone, zone.getId().replace("@", ""));
            CCUHsApi.getInstance().setSynced(StringUtils.prependIfMissing(floorLuid, "@"));
        }
        Log.i(TAG, " Importing Zone completed");
    }

    public Map<String, String> getCCUVersion(List<String> equipRefs){
        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        RetryCountCallback retryCountCallback = retryCount -> Log.i(TAG, "Retry count to get CCU version "+ retryCount);
        List<String> diagEquipPointList = getDiagEquipPointId(equipRefs, hClient, retryCountCallback);
        HDict[] dictArr = new HDict[diagEquipPointList.size()];
        for(int index = 0; index < dictArr.length; index++){
            HDictBuilder hDictBuilder =new HDictBuilder()
                    .add("id", HRef.copy(diagEquipPointList.get(index)));
            dictArr[index] = hDictBuilder.toDict();
        }
        HGrid ccuVersionGrid = invokeWithRetry("pointWriteMany", hClient, HGridBuilder.dictsToGrid(dictArr),
                retryCountCallback);
        Map<String, String> ccuVersionMap = new HashMap<>();
        if(ccuVersionGrid == null){
            CcuLog.i(TAG,"No CCU version found");
            return ccuVersionMap;
        }
        Iterator it = ccuVersionGrid.iterator();
        while (it.hasNext()) {
            HRow row = (HRow) it.next();
            String equipRef = row.get("equipRef").toString();
            HVal data = row.get("data");
            if (data instanceof HList && ((HList) data).size() > 0) {
                HList dataList = (HList) data;
                for (int i = 0; i < dataList.size(); i++) {
                    HDict dataElement = (HDict) dataList.get(i);
                    String val = dataElement.getStr("val");
                    ccuVersionMap.put(equipRef, val);
                    break;
                }
            }
        }
        return ccuVersionMap;
    }

    private List<String> getDiagEquipPointId(List<String> equipRefs, HClient hClient,
                                             RetryCountCallback retryCountCallback) {
        StringBuffer equipRefString = new StringBuffer("(");
        for(int index = 0; index < equipRefs.size(); index++){
            equipRefString.append("equipRef == ");
            equipRefString.append(StringUtils.prependIfMissing(equipRefs.get(index), "@"));
            if(index == equipRefs.size()-1){
                equipRefString.append(" ) ");
            }
            else{
                equipRefString.append(" or ");
            }
        }

        List<String> diagEquipPointList = new LinkedList<>();
        HDict diagVersionDict = new HDictBuilder().add("filter",
                "diag and version and " + equipRefString).toDict();
        HGrid diagVersionGrid = invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(diagVersionDict),
                retryCountCallback);
        if(diagVersionGrid == null){
            return diagEquipPointList;
        }
        Iterator it = diagVersionGrid.iterator();
        while (it.hasNext()) {
            HRow row = (HRow) it.next();
            diagEquipPointList.add(row.get("id").toString());
        }
        return diagEquipPointList;
    }

    public void readCCUDevice(String ccuId, RetryCountCallback retryCountCallback){
        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict ccuDict = new HDictBuilder().add("id", HRef.copy(StringUtils.prependIfMissing(ccuId, "@"))).toDict();
        HGrid ccuGrid = invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(ccuDict), retryCountCallback);
        if(ccuGrid == null){
            throw new NullHGridException("CCU Grid is NULL");
        }
        Iterator it = ccuGrid.iterator();
        while (it.hasNext()) {
            HRow row = (HRow) it.next();
            tagsDb.addHDict(row.get("id").toString(), row);
            CcuLog.i(TAG,"CCU point into tab");

        }
    }

    public HGrid getCCUSystemEquip(String ahuRef, RetryCountCallback retryCountCallback){
        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict ccuDict = new HDictBuilder().add("id", HRef.copy(StringUtils.prependIfMissing(ahuRef, "@"))).toDict();
        return invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(ccuDict), retryCountCallback);
    }

    public HGrid getDevice(String equipId, RetryCountCallback retryCountCallback){
        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict ccuDict = new HDictBuilder().add("filter",
                "device and not ccu and equipRef == " + StringUtils.prependIfMissing(equipId, "@")).toDict();
        return invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(ccuDict), retryCountCallback);
    }

    public HGrid getEquip(String equipId, RetryCountCallback retryCountCallback){
        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict ccuDict = new HDictBuilder().add("filter",
                "equip and id == " + StringUtils.prependIfMissing(equipId, "@")).toDict();
        return invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(ccuDict), retryCountCallback);
    }

    public void importEquip(HRow equipRow, RetryCountCallback retryCountCallback){
        Log.i(TAG, "Import Equip started for "+equipRow.get("dis").toString());
        List<Equip> equips = new ArrayList<>();
        List<HashMap> equipMaps = ccuHsApi.HGridToList(equipRow.grid());
        equipMaps.forEach(m -> equips.add(new Equip.Builder().setHashMap(m).build()));

        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict ccuDict = new HDictBuilder().add("filter",
                "point and equipRef == " + StringUtils.prependIfMissing(equipRow.get("id").toString()
                        , "@")).toDict();
        HGrid pointsGrid = invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(ccuDict), retryCountCallback);
        if(pointsGrid == null){
            throw new NullHGridException("Null occurred while fetching points for the equip Id: "+ equipRow.get("id").toString());
        }

        List<HashMap> pointMaps = ccuHsApi.HGridToList(pointsGrid);
        List<Point> points = new ArrayList<>();
        pointMaps.forEach(m -> points.add(new Point.Builder().setHashMap(m).build()));

        addEquipAndPoints(equips, points);
        writeValueToEquipPoints(equips, hClient, retryCountCallback);
        Log.i(TAG, "Import Equip completed for "+equipRow.get("dis").toString());
    }

    public void importDevice(HRow deviceRow, RetryCountCallback retryCountCallback){
        Log.i(TAG, "Import device started for "+deviceRow.get("dis").toString());
        List<Device> devices = new ArrayList<>();
        List<HashMap> deviceMap = ccuHsApi.HGridToList(deviceRow.grid());
        deviceMap.forEach(equip -> devices.add(new Device.Builder().setHashMap(equip).build()));

        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict ccuDict = new HDictBuilder().add("filter",
                "point and deviceRef == " + StringUtils.prependIfMissing(deviceRow.get("id").toString()
                        , "@")).toDict();
        HGrid systemPointsGrid = invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(ccuDict), retryCountCallback);
        if(systemPointsGrid == null){
            throw new NullHGridException("Null occurred while fetching points for "+deviceRow.get("id").toString());
        }
        List<HashMap> pointMaps = ccuHsApi.HGridToList(systemPointsGrid);
        List<RawPoint> points = new ArrayList<>();
        pointMaps.forEach(m -> points.add(new RawPoint.Builder().setHashMap(m).build()));

        addDeviceAndPoints(devices, points);
        writeValueToDevicePoints(devices, hClient, retryCountCallback);
        Log.i(TAG, "Import device completed for "+deviceRow.get("dis").toString());
    }

    private void addDeviceAndPoints(List<Device> devices, List<RawPoint> points) {
        CCUHsApi hsApi = CCUHsApi.getInstance();
        for (Device device : devices) {
            Log.i(TAG, "Adding points to the device "+ device.getDisplayName() +" started");
            String equipLuid = hsApi.addRemoteDevice(device, device.getId().replace("@", ""));
            hsApi.setSynced(StringUtils.prependIfMissing(equipLuid, "@"));
            //Points
            for (RawPoint point : points) {
                if (point.getDeviceRef().equals(device.getId())) {
                    String guidKey = StringUtils.prependIfMissing(point.getId(), "@");
                    HashMap<Object, Object> p = ccuHsApi.readMapById(point.getId());
                    if (p.isEmpty()) {
                        String pointLuid = hsApi.addRemotePoint(point, point.getId().replace("@", ""));
                        hsApi.setSynced(pointLuid);
                    } else {
                        CcuLog.i(TAG, "Point already imported " + point.getId());
                    }
                }
            }
            Log.i(TAG, "Adding points to the device "+ device.getDisplayName() +" completed");
        }
    }

    private void addEquipAndPoints(List<Equip> equips, List<Point> points) {
        CCUHsApi hsApi = CCUHsApi.getInstance();
        for (Equip equip : equips) {
            Log.i(TAG, "Adding points to the equip "+ equip.getDisplayName() +" started");
            String equipLuid = hsApi.addRemoteEquip(equip, equip.getId().replace("@", ""));
            hsApi.setSynced(equipLuid);
            //Points
            for (Point point : points) {
                if (point.getEquipRef().equals(equip.getId())) {
                    String pointId = StringUtils.prependIfMissing(point.getId(), "@");
                    HashMap<Object, Object> p = ccuHsApi.readMapById(pointId);
                    if (p.isEmpty()) {
                        String pointLuid = hsApi.addRemotePoint(point, point.getId().replace("@", ""));
                        hsApi.setSynced(pointLuid);
                    } else {
                        CcuLog.i(TAG, "Point already imported " + point.getId());
                    }
                }
            }
            Log.i(TAG, "Adding points to the equip "+ equip.getDisplayName() +" completed");
        }
    }

    private void writeValueToDevicePoints(List<Device> devices, HClient hClient, RetryCountCallback retryCountCallback) {
        for(Device device : devices){
            Log.i(TAG, "Writing value to points of the device "+ device.getDisplayName() +" started");
            List<HDict> devicePoints = getDevicePoints(device);
            writeValueToPoints(devicePoints, hClient, retryCountCallback);
            Log.i(TAG, "Writing value to points of the device "+ device.getDisplayName() +" completed");
        }
    }
    private HGrid invokeWithRetry(String op, HClient hClient, HGrid req, RetryCountCallback retryCountCallback){
        return hClient.invoke(op, req, retryCountCallback);
    }

    private void writeValueToPoints(List<HDict> points, HClient hClient, RetryCountCallback retryCountCallback){
        int partitionSize = 15;
        List<List<HDict>> partitions = new ArrayList<>();
        for (int i = 0; i<points.size(); i += partitionSize) {
            partitions.add(points.subList(i, Math.min(i + partitionSize, points.size())));
        }
        for (List<HDict> sublist : partitions) {
            HGrid writableArrayPoints = invokeWithRetry("pointWriteMany", hClient,
                    HGridBuilder.dictsToGrid(sublist.toArray(new HDict[sublist.size()])), retryCountCallback);

            if (writableArrayPoints == null) {
                CcuLog.e(TAG, "Failed to fetch point array values during syncing Profile.");
                throw new NullHGridException("Failed to fetch point array values during syncing Profile.");
            }
            List<HDict> hDictList = new ArrayList<>();
            Iterator rowIterator = writableArrayPoints.iterator();
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
                        HDict rec = hsClient.readById(HRef.copy(id));
                        if(row.has("his") && NumberUtils.isCreatable(val.toString())){
                            //save his data to local cache
                            tagsDb.saveHisItemsToCache(rec,
                                    new HHisItem[]{HHisItem.make(HDateTime.make(System.currentTimeMillis()),
                                            kind.equals(Kind.STRING.getValue()) ? HStr.make(val.toString()) : val)}, true);
                        }
                        //save points on tagsDb
                        tagsDb.onPointWrite(rec, Integer.parseInt(level), kind.equals(Kind.STRING.getValue()) ?
                                HStr.make(val.toString()) : val, who, HNum.make(0), rec, lastModifiedDateTime);
                    }
                }
            }
        }
    }

    private void writeValueToEquipPoints(List<Equip> equips, HClient hClient, RetryCountCallback retryCountCallback) {
        for(Equip equip : equips){
            Log.i(TAG, "Writing value to points of the equip "+ equip.getDisplayName() +" started");
            List<HDict> equipPoints = getEquipPoints(equip);
            writeValueToPoints(equipPoints, hClient, retryCountCallback);
            Log.i(TAG, "Writing value to points of the equip "+ equip.getDisplayName() +" completed");
        }
    }

    private List<HDict> getDevicePoints(Device device) {
        List<HDict> devicePoints = new ArrayList<>();
        List<HashMap> writablePoints =
                CCUHsApi.getInstance().readAll("point and writable and deviceRef == \"" + device.getId() +
                        "\"");
        for (HashMap  writablePoint: writablePoints) {
            HDict pid = new HDictBuilder().add("id", HRef.copy(writablePoint.get("id").toString())).toDict();
            devicePoints.add(pid);
        }
        return devicePoints;
    }

    private List<HDict> getEquipPoints(Equip equip) {
        List<HDict> equipPoints = new ArrayList<>();
        List<HashMap> writablePoints =
                CCUHsApi.getInstance().readAll("point and writable and equipRef == \"" + equip.getId() + "\"");
        for (HashMap  writablePoint: writablePoints) {
            HDict pid = new HDictBuilder().add("id", HRef.copy(writablePoint.get("id").toString())).toDict();
            equipPoints.add(pid);
        }
        return equipPoints;
    }

    /**
     * copied from CCuHsApi
     * @param siteId
     * @param hClient
     */
    private void importBuildingSchedule(String siteId, HClient hClient, RetryCountCallback retryCountCallback){
        Log.i(TAG, "Import building schedule started");
        HashMap<Object, Object> currentBuildingSchedule = ccuHsApi.readEntity("schedule and building and not special");
        if (!currentBuildingSchedule.isEmpty()) {
            //CCU already has a building schedule.
            CcuLog.i(TAG, " importBuildingSchedule : buildingSchedule exists");
            return;
        }

        try {
            HDict buildingDict =
                    new HDictBuilder().add("filter",
                            "building and schedule and not special and siteRef == " +
                                    StringUtils.prependIfMissing(siteId, "@")).toDict();
            HGrid buildingSch = invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(buildingDict), retryCountCallback);

            if (buildingSch == null) {
                throw new NullHGridException("Null occurred while importing building schedule");
            }

            Iterator it = buildingSch.iterator();
            while (it.hasNext())
            {
                HRow r = (HRow) it.next();
                Schedule buildingSchedule =  new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build();

                String guid = buildingSchedule.getId();
                buildingSchedule.setmSiteId(CCUHsApi.getInstance().getSiteIdRef().toString());
                CCUHsApi.getInstance().addSchedule(guid, buildingSchedule.getScheduleHDict());
                CCUHsApi.getInstance().setSynced(StringUtils.prependIfMissing(guid, "@"));
            }
        } catch (UnknownRecException e) {
            Log.i(TAG, "Exception occurred while Importing building schedule "+e.getMessage());
            e.printStackTrace();
        }
        Log.i(TAG, "Import building schedule completed");
    }

    private void importBuildingSpecialSchedule(String siteId, HClient hClient, RetryCountCallback retryCountCallback){

        HashMap<Object, Object>  currentBuildingSchedule = ccuHsApi.readEntity("schedule and building and special");
        if (!currentBuildingSchedule.isEmpty()) {
            //CCU already has a building special schedule.
            CcuLog.i(TAG, " importBuildingSpecialSchedule : building Special Schedule exists");
            return;
        }

        try {
            HDict buildingDict =
                    new HDictBuilder().add("filter", "building and schedule and special and siteRef == "
                            + siteId).toDict();
            HGrid buildingSch = invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(buildingDict), retryCountCallback);

            if (buildingSch == null) {
                return;
            }

            Iterator it = buildingSch.iterator();
            while (it.hasNext()) {
                HRow row = (HRow) it.next();
                String scheduleId = row.get("id").toString();
                tagsDb.addHDict(scheduleId.replace("@", ""),  new HDictBuilder().add(row).toDict());
                syncStatusService.addUnSyncedEntity(StringUtils.prependIfMissing(scheduleId, "@"));
                ccuHsApi.setSynced(StringUtils.prependIfMissing(scheduleId, "@"));
                CcuLog.i(TAG,"Building Special schedule Imported");
            }
        } catch (UnknownRecException e) {
            e.printStackTrace();
        }
    }


    /**
     * copied from CCuHsApi
     * @param siteId
     * @param hClient
     */

    private void importBuildingTuners(String siteId, HClient hClient,RetryCountCallback retryCountCallback) {
        CcuLog.i(TAG, " import BuildingTuners started");
        ArrayList<Equip> equips = new ArrayList<>();
        ArrayList<Point> points = new ArrayList<>();
        try {
            HDict tunerEquipDict = new HDictBuilder().add("filter",
                    "tuner and equip and siteRef == " + StringUtils.prependIfMissing(siteId, "@")).toDict();
            HGrid tunerEquipGrid = invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(tunerEquipDict), retryCountCallback);
            if (tunerEquipGrid == null) {
                throw new NullHGridException("Null occurred while importing building tuner");
            }
            List<HashMap> equipMaps = ccuHsApi.HGridToList(tunerEquipGrid);
            equipMaps.forEach(m -> equips.add(new Equip.Builder().setHashMap(m).build()));

            HDict tunerPointsDict = new HDictBuilder().add("filter",
                    "tuner and point and default and siteRef == " + StringUtils.prependIfMissing(siteId, "@")).toDict();
            HGrid tunerPointsGrid =  invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(tunerPointsDict), retryCountCallback);
            if (tunerPointsGrid == null) {
                throw new NullHGridException("Null occurred while importing building tuner");
            }

            List<HashMap> pointMaps = ccuHsApi.HGridToList(tunerPointsGrid);
            pointMaps.forEach(m -> points.add(new Point.Builder().setHashMap(m).build()));

        } catch (UnknownRecException e) {
            e.printStackTrace();
        }


        CCUHsApi hsApi = CCUHsApi.getInstance();
        for (Equip q : equips) {
            if (q.getMarkers().contains("tuner"))
            {
                String equiUuid;
                HashMap tunerEquip = ccuHsApi.read("tuner and equip");
                if (!tunerEquip.isEmpty()) {
                    equiUuid = tunerEquip.get("id").toString();
                } else {
                    q.setSiteRef(hsApi.getSiteIdRef().toString());
                    q.setFloorRef("@SYSTEM");
                    q.setRoomRef("@SYSTEM");
                    equiUuid = hsApi.addRemoteEquip(q, q.getId().replace("@", ""));
                    hsApi.setSynced(equiUuid);
                }
                //Points
                for (Point p : points)
                {
                    if (p.getEquipRef().equals(q.getId()))
                    {
                        String pointId = StringUtils.prependIfMissing(p.getId(), "@");
                        HashMap<Object, Object> point = ccuHsApi.readMapById(pointId);
                        if (point.isEmpty()) {
                            p.setSiteRef(hsApi.getSiteIdRef().toString());
                            p.setFloorRef("@SYSTEM");
                            p.setRoomRef("@SYSTEM");
                            p.setEquipRef(equiUuid);
                            String pointLuid = hsApi.addRemotePoint(p, p.getId().replace("@", ""));
                            hsApi.setSynced(pointLuid);
                        } else {
                            CcuLog.i(TAG, "Point already imported "+p.getId());
                        }

                    }
                }
            }
        }
        CcuLog.i(TAG," importBuildingTuners Completed");
    }

    public HGrid getRemoteSite(String siteId, RetryCountCallback retryCountCallback) {
        /* Sync a site*/
        HClient hClient   = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict   navIdDict = new HDictBuilder().add(HayStackConstants.ID, HRef.make(siteId)).toDict();
        HGrid   hGrid     = HGridBuilder.dictToGrid(navIdDict);

        HGrid siteGrid = invokeWithRetry(HStdOps.read.name(), hClient, hGrid, retryCountCallback);
        if (siteGrid != null) {
            siteGrid.dump();
        } else {
            CcuLog.e(TAG, "RemoteSite fetch Failed.");
        }

        return siteGrid;
    }

    /**
     * copied from CCuHsApi
     * @param siteId
     */
    public void syncExistingSite(String siteId, RetryCountCallback retryCountCallback) {
        siteId = StringUtils.stripStart(siteId,"@");

        if (StringUtils.isBlank(siteId)) {
            throw new NullHGridException("Failed to fetch point array values during syncing existing site.");
        }

        HGrid remoteSite = getRemoteSite(siteId, retryCountCallback);

        if (remoteSite == null || remoteSite.isEmpty() || remoteSite.isErr())
        {
            throw new NullHGridException("Failed to fetch point array values during syncing existing site.");
        }

        EntityParser p = new EntityParser(remoteSite);
        Site s = p.getSite();
        tagsDb.idMap.put("@"+tagsDb.addSiteWithId(s, siteId), s.getId());
        Log.d("CCU_HS_EXISTINGSITESYNC","Added Site "+s.getId());

        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);

        //import building schedule data
        importBuildingSchedule(siteId, hClient, retryCountCallback);

        //import building special schedule
        importBuildingSpecialSchedule(StringUtils.prependIfMissing(siteId, "@"), hClient, retryCountCallback);

        //import building tuners
        importBuildingTuners(siteId, hClient, retryCountCallback);

        ArrayList<HashMap> writablePoints = CCUHsApi.getInstance().readAll("point and writable");
        ArrayList<HDict> hDicts = new ArrayList<>();
        for (HashMap m : writablePoints) {
            HDict pid = new HDictBuilder().add("id",HRef.copy(m.get("id").toString())).toDict();
            hDicts.add(pid);
        }

        int partitionSize = 15;
        List<List<HDict>> partitions = new ArrayList<>();
        for (int i = 0; i<hDicts.size(); i += partitionSize) {
            partitions.add(hDicts.subList(i, Math.min(i + partitionSize, hDicts.size())));
        }

        for (List<HDict> sublist : partitions) {
            HGrid writableArrayPoints = invokeWithRetry("pointWriteMany", hClient,
                    HGridBuilder.dictsToGrid(sublist.toArray(new HDict[sublist.size()])), retryCountCallback);

            //We cannot proceed replacing an existing CCU without fetching all the point array values.
            if (writableArrayPoints == null) {
                CcuLog.e(TAG, "Failed to fetch point array values during syncing existing site.");
                throw new NullHGridException("Failed to fetch point array values during syncing existing site.");
            }

            ArrayList<HDict> hDictList = new ArrayList<>();

            Iterator rowIterator = writableArrayPoints.iterator();
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

                        //save his data to local cache
                        HDict rec = hsClient.readById(HRef.copy(id));
                        if(row.has("his") && NumberUtils.isCreatable(val.toString())) {
                            tagsDb.saveHisItemsToCache(rec, new HHisItem[]{HHisItem.make(HDateTime.make(System.currentTimeMillis()), kind.equals(Kind.STRING.getValue()) ? HStr.make(val.toString()) : val)}, true);
                        }
                        //save points on tagsDb
                        tagsDb.onPointWrite(rec, Integer.parseInt(level), kind.equals(Kind.STRING.getValue()) ?
                                HStr.make(val.toString()) : val, who, HNum.make(0), rec, lastModifiedDateTime);

                    }

                }

            }
        }
    }

    public void importNamedSchedule(RetryCountCallback retryCountCallback) {
        Log.i(TAG, " Import Named schedule started");
        Site site = CCUHsApi.getInstance().getSite();
        HClient hClient =new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        if (site != null && site.getOrganization() != null) {
            String org = site.getOrganization();
            HDict zoneScheduleDict = new HDictBuilder().add("filter",
                    "named and schedule and organization == \""+org+"\"").toDict();
            HGrid zoneScheduleGrid = invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(zoneScheduleDict),
                    retryCountCallback);

            if (zoneScheduleGrid == null) {
                CcuLog.d(TAG, "zoneScheduleGrid is null");
                return;
            }

            Iterator it = zoneScheduleGrid.iterator();
            while (it.hasNext()) {
                HRow row = (HRow) it.next();
                tagsDb.addHDict((row.get("id").toString()).replace("@", ""), row);
            }
        }
        CcuLog.i(TAG, "Import Named schedule completed");
    }
    public void restoreSettingPointsOfCCUDevice(String ccuDeviceID, RetryCountCallback retryCountCallback){

        HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict settingPoints = new HDictBuilder().add("filter",
                "setting and point and deviceRef == " + StringUtils.prependIfMissing(ccuDeviceID, "@")).toDict();
        HGrid settingPointsGrid =  invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(settingPoints),
                retryCountCallback);

        List<HashMap> pointMaps = ccuHsApi.HGridToList(settingPointsGrid);

        pointMaps.forEach(pointMap -> {
            SettingPoint pointDetails = new SettingPoint.Builder().setHashMap(pointMap).build();
            String pointId = StringUtils.prependIfMissing(pointDetails.getId(), "@");
            HashMap<Object, Object> point = ccuHsApi.readMapById(pointId);
            if (point.isEmpty()) {
                String pointLuid = ccuHsApi.addRemotePoint(pointDetails, pointDetails.getId().replace("@", ""));
                ccuHsApi.setSynced(pointLuid);
            } else {
                CcuLog.i(TAG, "Point already imported "+pointDetails.getId());
            }
        });
    }

    public HGrid getDiagEquipByEquipId(String equipId, RetryCountCallback retryCountCallback){
        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HDict ccuDict = new HDictBuilder().add("id", HRef.copy(StringUtils.prependIfMissing(equipId, "@"))).toDict();

        return invokeWithRetry("read", hClient, HGridBuilder.dictToGrid(ccuDict), retryCountCallback);
    }

}

