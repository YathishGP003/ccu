package a75f.io.messaging.handler;

import static a75f.io.logic.util.PreferenceUtil.setDataSyncStopped;
import static a75f.io.messaging.handler.UpdateScheduleHandler.refreshIntrinsicSchedulesScreen;
import static a75f.io.messaging.handler.UpdateScheduleHandler.refreshSchedulesScreen;
import static a75f.io.messaging.handler.UpdateScheduleHandler.trimZoneSchedules;

import android.util.Log;

import com.google.gson.JsonObject;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HRow;
import org.projecthaystack.HVal;
import org.projecthaystack.MapImpl;
import org.projecthaystack.client.HClient;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.jobs.SystemScheduleUtil;
import a75f.io.logic.util.PreferenceUtil;

public class DataSyncHandler {
    private static final Object DELETED_BY = "deletedBy";
    private static final Object PRIORITY_ARRAY = "priorityArray";
    private static final int PAGE_SIZE = 100;
    private static final long MESSAGE_EXPIRY_TIME_FOR_DEV_QA = 129600000; // 36 hours
    private static final long MESSAGE_EXPIRY_TIME_FOR_PRODUCTION = 259200000; //3 days
    private static final long MESSAGE_EXPIRY_TIME_FOR_STAGING = 129600000; // 36 hours
    private static final long DELAY_FOR_DATA_SYNC = 240000; // 4 minutes
    private enum SyncStatus {
        NULL_RESPONSE, COMPLETED
    }

    public SyncStatus syncData(long startDateTime, CCUHsApi ccuHsApi, long messageExpiryTime) {
        logIt("Initialise Data Sync "+"start time " + new Date(startDateTime) + " End time " + new Date(System.currentTimeMillis()
                - messageExpiryTime)+"Build config " +BuildConfig.BUILD_TYPE + " messageExpiryTime "+ messageExpiryTime);
        int pageNo = 0;
        long endDateTime = System.currentTimeMillis();
        HGridBuilder b = new HGridBuilder();
        b.addCol("siteRef");
        b.addCol("ccuRef");
        b.addCol("startDateTime");
        b.addCol("endDateTime");
        b.addRow(new HVal[]{ccuHsApi.getSiteIdRef(), ccuHsApi.getCcuRef(), HDateTime.make(startDateTime), HDateTime.make(endDateTime - messageExpiryTime)});
        HGrid req = b.toGrid();
        HClient hClient = new HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);

        HGrid readChanges = hClient.call("readChanges", req, pageNo, PAGE_SIZE);
        List<HGrid> readChangesResponse = new ArrayList<>();
        if (readChanges == null) {
            logIt(" Received null response from readChanges API");
            setDataSyncStopped();
            return SyncStatus.NULL_RESPONSE;
        }
        readChangesResponse.add(readChanges);
        int responsePageSize = getResponsePageSize(readChanges);
        logIt("Response page size " + responsePageSize);
        if (responsePageSize > 0) {
            for (pageNo = 1; pageNo <= responsePageSize; pageNo++) {
                HGrid nextReadChangesResponse = hClient.call("readChanges", req, pageNo, PAGE_SIZE);
                if(nextReadChangesResponse == null){
                    logIt(" Received null response from readChanges API while fetching next page");
                    setDataSyncStopped();
                    return SyncStatus.NULL_RESPONSE;
                }else {
                    readChangesResponse.add(nextReadChangesResponse);
                    logIt("iteration response size " + readChangesResponse.size());
                }
            }
        }

        logIt("Total response size " + readChangesResponse.size());

        for (HGrid readChangesGrid : readChangesResponse) {
            syncReadChangesApiResponseToCCU(readChangesGrid, ccuHsApi);
        }
        PreferenceUtil.setLastCCUUpdatedTime(System.currentTimeMillis());
        return SyncStatus.COMPLETED;
    }

    private int getResponsePageSize(HGrid readChanges) {
        HDict meta = readChanges.meta();
        if (meta.has("total")) {
            int entitySize = (int) Double.parseDouble(meta.get("total").toString());
            logIt("Total Entity size " + entitySize);
            return entitySize / PAGE_SIZE;
        }
        logIt("No total field in metadata");
        return 0;
    }

    private void syncReadChangesApiResponseToCCU(HGrid readChanges, CCUHsApi ccuHsApi) {
        List<HashMap> entitiesToSync = ccuHsApi.HGridToList(readChanges);
        logIt("Entities size " + entitiesToSync.size() + "EntitiesToSync  " + entitiesToSync);
        List<HashMap> pointsToSync = new ArrayList<>();
        List<HashMap<Object, Object>> deletedEntities = new ArrayList<>();
        List<HashMap<Object, Object>> roomEntities = new ArrayList<>();
        List<HashMap<Object, Object>> floorEntities = new ArrayList<>();

        List<HDict> schedulesToSync = getSchedulesToSync(readChanges);

        entitiesToSync.forEach(entityToSync -> {
            if (isPointToBeSynced(entityToSync)) {
                pointsToSync.add(entityToSync);
            }
            if (entityToSync.containsKey(DELETED_BY)) {
                deletedEntities.add(entityToSync);
            }
            if(entityToSync.containsKey(Tags.ROOM)){
                roomEntities.add(entityToSync);
            }
            if(entityToSync.containsKey(Tags.FLOOR)){
                floorEntities.add(entityToSync);
            }
        });
        if (roomEntities.size() > 0){
            syncRoom(roomEntities, ccuHsApi);
        }
        if (pointsToSync.size() > 0) {
            syncPoint(pointsToSync, ccuHsApi);
        }

        if (floorEntities.size() > 0){
            syncFloor(floorEntities, ccuHsApi);
        }
        if (schedulesToSync.size() > 0) {
            syncSchedule(schedulesToSync, ccuHsApi);
        }
        if (deletedEntities.size() > 0) {
            removeEntities(deletedEntities, ccuHsApi);
        }
    }

    private boolean isPointToBeSynced(HashMap entityToSync) {
        return entityToSync.containsKey(Tags.POINT)
                && entityToSync.containsKey(PRIORITY_ARRAY) && entityToSync.get(PRIORITY_ARRAY) != null
                && !entityToSync.containsKey(DELETED_BY);
    }

    private List<HDict> getSchedulesToSync(HGrid readChanges) {
        Iterator iterator = readChanges.iterator();
        List<HDict> schedulesToSync = new ArrayList<>();
        while ((iterator.hasNext())) {
            HRow row = (HRow) iterator.next();
            logIt("row" + row);
            if (row.has(Tags.SCHEDULE) && !row.has(DELETED_BY.toString())) {
                HDict scheduleDict = new HDictBuilder().add(row).toDict();
                schedulesToSync.add(scheduleDict);
            }
        }
        return schedulesToSync;
    }

    private void syncFloor(List<HashMap<Object, Object>> floorEntities, CCUHsApi ccuHsApi) {
        for(HashMap<Object, Object> floorEntity : floorEntities) {
            logIt("Sync floor entity >> "+floorEntity);
            HashMap<Object, Object> localFloor= ccuHsApi.readMapById(floorEntity.get(Tags.ID).toString());
            if (isFloorEntityValid(floorEntity, ccuHsApi)) {
                if (isCloudEntityHasLatestValue(localFloor, floorEntity)) {
                    Floor floor = new Floor.Builder()
                            .setDisplayName(floorEntity.get("dis").toString())
                            .setSiteRef(floorEntity.get("siteRef").toString())
                            .build();
                    floor.setId(floorEntity.get("id").toString());
                    floor.setOrientation(Double.parseDouble(floorEntity.get("orientation").toString()));
                    floor.setFloorNum(Double.parseDouble(floorEntity.get("floorNum").toString()));
                    floor.setCreatedDateTime(getCreatedDateTime(floorEntity));
                    floor.setLastModifiedDateTime(getLastModifiedTime(floorEntity));
                    floor.setLastModifiedBy(getLastModifiedBy(floorEntity, ccuHsApi));
                    ccuHsApi.updateFloorLocally(floor, floor.getId());
                }
            } else {
                logIt("Floor entity is not valid >> "+floorEntity);
            }
        }
    }
    private boolean isCloudScheduleHasLatestValue(HDict localSchedule, HDict lastModifiedTimeInCloud) {
        if (localSchedule.has(Tags.LAST_MODIFIED_TIME) && lastModifiedTimeInCloud.has(Tags.LAST_MODIFIED_TIME)) {
            HDateTime lastModifiedDateTimeInCCU = HDateTime.make(localSchedule.get(Tags.LAST_MODIFIED_TIME).toString());
            HDateTime lastModifiedDateTimeInCloud = HDateTime.make(lastModifiedTimeInCloud.get(Tags.LAST_MODIFIED_TIME).toString());
            logIt("lastModifiedDateTimeInCCU milli seconds >  " + lastModifiedDateTimeInCCU+
                    "lastModifiedTimeInCloud milli seconds >  " + lastModifiedDateTimeInCloud);
            logIt("Is CCU has latest value ? schedule " + (lastModifiedDateTimeInCCU.millis() > lastModifiedDateTimeInCloud.millis()));
            return lastModifiedDateTimeInCloud.millis() > lastModifiedDateTimeInCCU.millis();
        }
        logIt("lastModifiedDateTimeInCCU is null");
        return true;
    }

    private boolean isCloudPointHasLatestValue(HashMap<Object, Object> localPoint, Integer level, MapImpl map, CCUHsApi ccuHsApi) {
        String lastModifiedTimeInCCU = HSUtil.getLevelEntityOfPoint(Objects.requireNonNull(
                localPoint.get(Tags.ID)).toString(), level, Tags.LAST_MODIFIED_TIME, ccuHsApi);
        if (lastModifiedTimeInCCU != null & map.has(Tags.LAST_MODIFIED_TIME)) {
            HDateTime lastModifiedDateTimeInCCU = HDateTime.make(lastModifiedTimeInCCU);
            HDateTime lastModifiedDateTimeInCloud = HDateTime.make(map.get(Tags.LAST_MODIFIED_TIME).toString());
            logIt("lastModifiedDateTimeInCCU milli seconds >  " + lastModifiedDateTimeInCCU+
                    "lastModifiedTimeInCloud milli seconds >  " + lastModifiedDateTimeInCloud);
            logIt("Is CCU has latest value point ? " + (lastModifiedDateTimeInCCU.millis() > lastModifiedDateTimeInCloud.millis()));
            return lastModifiedDateTimeInCloud.millis() > lastModifiedDateTimeInCCU.millis();
        }
        logIt("last Modified DateTime In CCU for this point"+localPoint.get(Tags.ID)+ "is null");
        return true;
    }

    private boolean isCloudPointHasLatestValue(HashMap<Object, Object> localPoint, MapImpl map, CCUHsApi ccuHsApi) {
        String lastModifiedTimeInCCU = ccuHsApi.readPointPriorityLatestTime(localPoint.get(Tags.ID).toString());
        logIt("lastModifiedTimeInCCU aaa >  " + lastModifiedTimeInCCU);
        if (lastModifiedTimeInCCU != null && map.has(Tags.LAST_MODIFIED_TIME)) {
            HDateTime lastModifiedDateTimeInCCU = HDateTime.make(lastModifiedTimeInCCU);
            HDateTime lastModifiedDateTimeInCloud = HDateTime.make(map.get(Tags.LAST_MODIFIED_TIME).toString());
            logIt("lastModifiedTimeInCloud milli seconds >  " + lastModifiedDateTimeInCloud);
            logIt("Is CCU has latest value point ? " + (lastModifiedDateTimeInCCU.millis() > lastModifiedDateTimeInCloud.millis()));
            return lastModifiedDateTimeInCloud.millis() > lastModifiedDateTimeInCCU.millis();
        }
        logIt("last Modified DateTime In CCU for this point"+localPoint.get(Tags.ID)+ "is null");
        return true;
    }
    public static boolean isCloudEntityHasLatestValue(HashMap<Object, Object> entity, Long timeToken) {
        CcuLog.i(L.TAG_CCU_READ_CHANGES,"Reconfiguration changes -> localEntity: "+entity+" ||  timeToken: "+timeToken);
        if (entity.containsKey(Tags.LAST_MODIFIED_TIME) && timeToken != null) {
            String lastModifiedTimeInCCU = entity.get(Tags.LAST_MODIFIED_TIME).toString();
            HDateTime lastModifiedDateTimeInMessage = HDateTime.make(timeToken);
            CcuLog.i(L.TAG_CCU_READ_CHANGES,"lastModifiedTimeInCCU: "+lastModifiedTimeInCCU+
                    " ||lastModifiedDateTimeInMessage "+lastModifiedDateTimeInMessage);
            HDateTime lastModifiedDateTimeInCCU = HDateTime.make(lastModifiedTimeInCCU);
            CcuLog.i(L.TAG_CCU_READ_CHANGES,"Is cloud has latest value ? " + (lastModifiedDateTimeInCCU.millis() < lastModifiedDateTimeInMessage.millis()));;
            return lastModifiedDateTimeInCCU.millis() < lastModifiedDateTimeInMessage.millis();
        }
        return true;
    }
    public static boolean isCloudEntityHasLatestValue(HashMap<Object, Object> localEntity, HashMap<Object, Object> cloudEntity) {
        CcuLog.i(L.TAG_CCU_READ_CHANGES,"DataSync changes -> localEntity: "+localEntity+" ||  cloudEntity: "+cloudEntity);
        if (localEntity.containsKey(Tags.LAST_MODIFIED_TIME) && cloudEntity.containsKey(Tags.LAST_MODIFIED_TIME)) {
            String lastModifiedTimeInCCU = localEntity.get(Tags.LAST_MODIFIED_TIME).toString();
            HDateTime lastModifiedDateTimeInMessage = HDateTime.make(cloudEntity.get(Tags.LAST_MODIFIED_TIME).toString());
            CcuLog.i(L.TAG_CCU_READ_CHANGES,"lastModifiedTimeInCCU "+ lastModifiedTimeInCCU+
                    "||  lastModifiedDateTimeInMessage: "+lastModifiedDateTimeInMessage);
            HDateTime lastModifiedDateTimeInCCU = HDateTime.make(lastModifiedTimeInCCU);
            CcuLog.i(L.TAG_CCU_READ_CHANGES,"Is cloud has latest value ? " +
                    (lastModifiedDateTimeInCCU.millis() < lastModifiedDateTimeInMessage.millis()));;
            return lastModifiedDateTimeInCCU.millis() < lastModifiedDateTimeInMessage.millis();
        }
        return true;
    }

    private void syncRoom(List<HashMap<Object, Object>> roomEntities, CCUHsApi ccuHsApi) {
        for (HashMap<Object, Object> roomEntity : roomEntities) {
            logIt("Sync room entity >> " + roomEntity);
            HashMap<Object, Object> localRoom = ccuHsApi.readMapById(roomEntity.get(Tags.ID).toString());
            if (isRoomEntityValid(roomEntity)) {
                if (isCloudEntityHasLatestValue(localRoom, roomEntity)) {
                    Zone zone = new Zone.Builder()
                            .setDisplayName(Objects.requireNonNull(roomEntity.get("dis")).toString())
                            .setSiteRef(Objects.requireNonNull(roomEntity.get("siteRef")).toString())
                            .setFloorRef(Objects.requireNonNull(roomEntity.get("floorRef")).toString())
                            .build();
                    zone.setId(Objects.requireNonNull(roomEntity.get("id")).toString());
                    zone.setScheduleRef(Objects.requireNonNull(roomEntity.get("scheduleRef")).toString());
                    zone.setCreatedDateTime(getCreatedDateTime(roomEntity));
                    zone.setLastModifiedDateTime(getLastModifiedTime(roomEntity));
                    zone.setLastModifiedBy(getLastModifiedBy(roomEntity, ccuHsApi));
                    ccuHsApi.updateZoneLocally(zone, Objects.requireNonNull(roomEntity.get("id")).toString());
                }
            } else {
                logIt("room entity is not valid >> " + roomEntity);
            }
        }
    }
    private boolean isRoomEntityValid(HashMap<Object, Object> roomEntity) {
        return roomEntity.containsKey("id") && roomEntity.containsKey("dis") && roomEntity.containsKey("siteRef")
                && roomEntity.containsKey("floorRef") && roomEntity.containsKey("scheduleRef");
    }
    private boolean isFloorEntityValid(HashMap<Object, Object> floorEntity, CCUHsApi ccuHsApi) {
        return floorEntity.containsKey("dis") && floorEntity.containsKey("siteRef") && floorEntity.containsKey("id")
                && floorEntity.containsKey("orientation") && floorEntity.containsKey("floorNum") &&
                ccuHsApi.isEntityExisting(floorEntity.get("id").toString());
    }
    private void removeEntities(List<HashMap<Object, Object>> hashMapList, CCUHsApi ccuHsApi) {
        hashMapList.forEach(deletedEntity -> {
            logIt("Deleted id" + Objects.requireNonNull(deletedEntity.get(Tags.ID)).toString());
            ccuHsApi.removeEntity(Objects.requireNonNull(deletedEntity.get(Tags.ID)).toString());
        });
    }

    private void syncSchedule(List<HDict> schedulesToSync, CCUHsApi ccuHsApi) {
        schedulesToSync.forEach(scheduleDict -> {
            logIt(" Schedules to sync " + scheduleDict);
            if (scheduleDict != null) {
                String scheduleId = scheduleDict.get(Tags.ID).toString();
                logIt("Schedule Id  " + scheduleId);
                if (ccuHsApi.isEntityExisting(scheduleId)) {
                    HDict schedule = ccuHsApi.getScheduleDictById(scheduleDict.get(Tags.ID).toString());
                    if (isCloudScheduleHasLatestValue(schedule, scheduleDict)) {
                        updateSchedule(scheduleDict, ccuHsApi);
                    }
                } else {
                    addSchedule(scheduleDict, ccuHsApi);
                    ccuHsApi.setSynced(scheduleId);
                }
                ScheduleManager.getInstance().updateSchedules();
            }
            refreshSchedulesScreen();
            refreshIntrinsicSchedulesScreen();
        });
    }

    private void addSchedule(HDict scheduleDict, CCUHsApi ccuHsApi) {
        logIt("new schedule added " + scheduleDict);
        String scheduleId = scheduleDict.get(Tags.ID).toString();
        if (scheduleDict.has(Tags.NAMED)) {
            ccuHsApi.updateNamedSchedule(scheduleId.replace("@", ""), scheduleDict);
        }
        if (scheduleDict.has(Tags.SPECIAL)) {
            ccuHsApi.addSchedule(scheduleId.replace("@", ""), scheduleDict);
            return;
        }
        Schedule schedule = new Schedule.Builder().setHDict(scheduleDict).build();
        if (scheduleDict.has(Tags.BUILDING) && scheduleDict.has(Tags.VACATION)) {
            ccuHsApi.addSchedule(scheduleId.replace("@", ""), schedule.getScheduleHDict());
        }
        if (scheduleDict.has(Tags.ZONE) && scheduleDict.has(Tags.VACATION)) {
            ccuHsApi.addSchedule(scheduleId.replace("@", ""),
                    schedule.getZoneScheduleHDict(schedule.getRoomRef()));
        }
    }

    private void updateSchedule(HDict scheduleDict, CCUHsApi ccuHsApi) {
        logIt("update schedule   " + scheduleDict);
        String scheduleId = scheduleDict.get(Tags.ID).toString();
        if (scheduleDict.has(Tags.NAMED)) {
            ccuHsApi.updateNamedSchedule(scheduleId.replace("@",""), scheduleDict);
        }
        if (scheduleDict.has(Tags.SPECIAL)) {
            ccuHsApi.updateSpecialScheduleNoSync(scheduleId.replace("@",""), scheduleDict);
            return;
        }
        Schedule schedule = new Schedule.Builder().setHDict(scheduleDict).build();
        if (schedule.isVacation()) {
            updateVacation(schedule, schedule.getRoomRef(), ccuHsApi);
        } else if (schedule.isBuildingSchedule()) {
            updateBuildingSchedule(schedule, ccuHsApi);
        } else if (schedule.isZoneSchedule()) {
            ccuHsApi.updateScheduleNoSync(schedule, schedule.getRoomRef());
        }
    }

    private void updateBuildingSchedule(Schedule schedule, CCUHsApi ccuHsApi) {
        ccuHsApi.updateScheduleNoSync(schedule, null);
        Schedule systemSchedule = ccuHsApi.getSystemSchedule(false).get(0); // check it
        if (!systemSchedule.equals(schedule)) {
            ccuHsApi.updateScheduleNoSync(schedule, null);
            trimZoneSchedules(schedule);
            ccuHsApi.scheduleSync();
        }
    }

    private void updateVacation(Schedule schedule, String roomRef, CCUHsApi ccuHsApi) {
        ccuHsApi.updateScheduleNoSync(schedule, null);
        if (schedule.getRoomRef() != null)
            ccuHsApi.updateScheduleNoSync(schedule, roomRef);
    }

    private void syncPoint(List<HashMap> pointsToSync, CCUHsApi ccuHsApi) {
        pointsToSync.forEach(pointToSync -> {
            logIt("Point to sync " + pointToSync);
            Object priorityArrayObj = pointToSync.get(PRIORITY_ARRAY);
            HList hList = ((HList) priorityArrayObj);
            logIt(" hList " + hList);
            if (hList != null && hList.size() > 0) {
                for (int priorityArrayIndex = 0; priorityArrayIndex < hList.size(); priorityArrayIndex++) {
                    MapImpl priorityArrayMap = (MapImpl) hList.get(priorityArrayIndex);
                    logIt("Priority Array Map " + priorityArrayMap);
                    if(isMapValid(priorityArrayMap)) {
                        setVal(priorityArrayMap, pointToSync, ccuHsApi);
                    }else {
                        logIt("Priority Array Map is not valid "+priorityArrayMap);
                    }
                }
            }
        });
    }

    private boolean isMapValid(MapImpl priorityArrayMap) {
        return priorityArrayMap.has(HayStackConstants.WRITABLE_ARRAY_LEVEL) && priorityArrayMap.has(HayStackConstants.WRITABLE_ARRAY_VAL) &&
                priorityArrayMap.has(HayStackConstants.WRITABLE_ARRAY_WHO);
    }
    private void setVal(MapImpl priorityArrayMap, HashMap<Object, Object> pointHash, CCUHsApi ccuHsApi) {
        int level = Integer.parseInt(priorityArrayMap.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).toString());
        String value = priorityArrayMap.get(HayStackConstants.WRITABLE_ARRAY_VAL).toString();
        String who = priorityArrayMap.get(HayStackConstants.WRITABLE_ARRAY_WHO).toString();
        String localValue = HSUtil.getLevelEntityOfPoint(Objects.requireNonNull(
                pointHash.get(Tags.ID)).toString(), level, HayStackConstants.WRITABLE_ARRAY_VAL, ccuHsApi);
        String pointRef = Objects.requireNonNull(pointHash.get(Tags.ID)).toString();
        logIt("LEVEL " + level + " VALUE FROM READ_CHANGES CALL " + value + " WHO " + who + " PointRef " +
                pointRef + " LOCAL_VALUE " + localValue);
        if (!value.equals(localValue)) {
            HashMap<Object, Object> localPointHash = ccuHsApi.readMapById(pointHash.get(Tags.ID).toString());
            if (pointHash.containsKey("scheduleType") && !pointHash.containsKey("modbus")) {
                if(isCloudPointHasLatestValue(localPointHash, priorityArrayMap, ccuHsApi)){
                    updateScheduleType(pointRef, level, value, pointHash, who, ccuHsApi);
                }
                return;
            }
            if (pointHash.containsKey("desired") && !pointHash.containsKey("modbus")) {
                if(isCloudPointHasLatestValue(localPointHash, priorityArrayMap, ccuHsApi)) {
                    updateDesiredTemperature(pointRef, level, value, pointHash, who, ccuHsApi);
                }
                return;
            }
            if (isCloudPointHasLatestValue(pointHash, level, priorityArrayMap, ccuHsApi)) {
                updatePoint(pointRef, pointHash, ccuHsApi, priorityArrayMap);
                CcuLog.i("CCU_READ_CHANGES", " Synced Point " + pointRef);
            }
        }
    }


    private void updatePoint(String pointRef, HashMap<Object, Object>
            pointHash, CCUHsApi ccuHsApi, MapImpl priorityArrayMap) {
        int level = Integer.parseInt(priorityArrayMap.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).toString());
        String value = priorityArrayMap.get(HayStackConstants.WRITABLE_ARRAY_VAL).toString();
        String who = priorityArrayMap.get(HayStackConstants.WRITABLE_ARRAY_WHO).toString();
        String duration = priorityArrayMap.get("duration").toString();
        try {
            double doubleValue = Double.parseDouble(value);
            double doubleDuration = Double.parseDouble(duration);
            if (pointHash.containsKey(Tags.WRITABLE)) {
                JsonObject body = new JsonObject();
                body.addProperty("val", (int) doubleValue);
                body.addProperty("level", level);
                body.addProperty("id", pointRef.replace("@", ""));
                body.addProperty("command", "updatePoint");
                body.addProperty("who", who);
                body.addProperty("lastModifiedDateTime", priorityArrayMap.get(Tags.LAST_MODIFIED_TIME).toString());
                body.addProperty("duration", (int) doubleDuration);
                UpdatePointHandler.handlePointUpdateMessage(body, null, true);
            }else {
                ccuHsApi.writePointLocal(pointRef, level,
                        who, doubleValue, 0);
                Log.i("CCU_READ_CHANGES", " Synced Point val Non writable" + doubleValue);
            }
            Log.i("CCU_READ_CHANGES", " Synced Point val " + doubleValue);
        } catch (NumberFormatException e) {
            logIt(" NumberFormatException " + e);
            ccuHsApi.writePointStrValLocal(pointRef, level, who, value, 0);
            Log.i("CCU_READ_CHANGES", " Synced Point val with exception " + value);
        }
    }

    private void updateDesiredTemperature(String pointRef, int level, String value, HashMap<Object, Object> pointHash, String who, CCUHsApi ccuHsApi) {
        CcuLog.i(L.TAG_CCU_READ_CHANGES, "Desired Temp Update ");
        CCUHsApi.getInstance().deletePointArray(pointRef);
        ccuHsApi.writePointLocal(pointRef, level,
                who, Double.parseDouble(value), 0);
        Point desiredTempPoint = new Point.Builder().setHashMap(pointHash).build();
        SystemScheduleUtil.handleDesiredTempUpdate(desiredTempPoint, false, 0);
    }

    private void updateScheduleType(String pointRef, int level, String value, HashMap<Object, Object> pointHash, String who, CCUHsApi ccuHsApi) {
        logIt(" scheduleType update " + pointRef);
        CCUHsApi.getInstance().deletePointArray(pointRef);
        ccuHsApi.writePointLocal(pointRef, level,
                who, Double.parseDouble(value), 0);
        Point scheduleTypePoint = new Point.Builder().setHashMap(pointHash).build();
        SystemScheduleUtil.handleScheduleTypeUpdate(scheduleTypePoint);
    }
    /*
     * TTL (Time to live) for message in dev and qa is 1hr
     * In staging 36hrs
     * In production 7days
     *  */
    public static long getMessageExpiryTime() {
        if(BuildConfig.BUILD_TYPE.equals("dev") || BuildConfig.BUILD_TYPE.equals("qa")){
            return MESSAGE_EXPIRY_TIME_FOR_DEV_QA;
        }else if(BuildConfig.BUILD_TYPE.equals("staging")) {
            return MESSAGE_EXPIRY_TIME_FOR_STAGING;
        }else {
            return MESSAGE_EXPIRY_TIME_FOR_PRODUCTION;
        }
    }
    public void syncCCUData(long lastCCUUpdateTime) {
        setDataSyncRunning(lastCCUUpdateTime);
        CcuLog.i(L.TAG_CCU_READ_CHANGES, "Call Data Sync ");
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(syncData(lastCCUUpdateTime, CCUHsApi.getInstance(),
                        getMessageExpiryTime()) == SyncStatus.COMPLETED) {
                    Log.i(L.TAG_CCU_READ_CHANGES, " All Entities are Synced from cloud and starting to sync local data");
                    CCUHsApi.getInstance().syncEntityWithPointWrite();
                    setDataSyncStopped();
                }
            }
        }, DELAY_FOR_DATA_SYNC);
    }
    private void setDataSyncRunning(long lastCCUUpdateTime) {
        Log.i("CCU_READ_CHANGES","setDataSyncRunning  "+new Date(lastCCUUpdateTime));
        a75f.io.logic.util.PreferenceUtil.setDataSyncRunning();
        a75f.io.logic.util.PreferenceUtil.setSyncStartTime(lastCCUUpdateTime);
    }

    private static String getLastModifiedBy(HashMap<Object, Object> entity, CCUHsApi ccuHsApi) {
        if(entity.containsKey("lastModifiedBy")){
            return Objects.requireNonNull(entity.get("lastModifiedBy")).toString();
        }else {
            return ccuHsApi.getCCUUserName();
        }
    }
    private static HDateTime getLastModifiedTime(HashMap<Object, Object> entity) {
        if(entity.containsKey(Tags.LAST_MODIFIED_TIME)){
            return HDateTime.make(Objects.requireNonNull(entity.get(Tags.LAST_MODIFIED_TIME)).toString());
        }else {
            return HDateTime.make(System.currentTimeMillis());
        }
    }
    private static HDateTime getCreatedDateTime(HashMap<Object, Object> entity) {
        if(entity.containsKey("createdDateTime")){
            return HDateTime.make(Objects.requireNonNull(entity.get("createdDateTime")).toString());
        }else {
            return HDateTime.make(System.currentTimeMillis());
        }
    }
    public static boolean isMessageTimeExpired(long lastCCUUpdateTime) {
        if (lastCCUUpdateTime != 0) {
            return lastCCUUpdateTime < (System.currentTimeMillis() - getMessageExpiryTime());
        }
        return false;
    }
    private void logIt(String message) {
        CcuLog.i(L.TAG_CCU_READ_CHANGES, message);
    }
}