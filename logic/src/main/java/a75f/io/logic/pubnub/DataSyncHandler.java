package a75f.io.logic.pubnub;

import static a75f.io.logic.pubnub.UpdateScheduleHandler.refreshIntrinsicSchedulesScreen;
import static a75f.io.logic.pubnub.UpdateScheduleHandler.refreshSchedulesScreen;
import static a75f.io.logic.pubnub.UpdateScheduleHandler.trimZoneSchedules;

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

public class DataSyncHandler {
    private static final Object DELETED_BY = "deletedBy";
    private static final Object PRIORITY_ARRAY = "priorityArray";
    private static final int PAGE_SIZE = 100;
    private static final long MESSAGE_EXPIRY_TIME_FOR_DEV_QA = 3600000;
    private static final long MESSAGE_EXPIRY_TIME_FOR_STAG_PROD = 129600000;

    public void initialiseDataSync(long startDateTime, CCUHsApi ccuHsApi, long messageExpiryTime) {
        logIt("Initialise Data Sync ");
        logIt("start time " + new Date(startDateTime) + " End time " + new Date(System.currentTimeMillis() - messageExpiryTime));
        logIt("Build config " +BuildConfig.BUILD_TYPE + " messageExpiryTime "+ messageExpiryTime);
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
            logIt(" All Entities are in sync with cloud or received" +
                    " null response from readChanges API");
            return;
        }
        readChangesResponse.add(readChanges);
        int responsePageSize = getResponsePageSize(readChanges);
        logIt("responsePageSize " + responsePageSize);
        if (responsePageSize > 0) {
            for (pageNo = 1; pageNo <= responsePageSize; pageNo++) {
                HGrid nextReadChangesResponse = hClient.call("readChanges", req, pageNo, PAGE_SIZE);
                readChangesResponse.add(nextReadChangesResponse);
                logIt("iteration response size " + readChangesResponse.size());
            }
        }
        logIt("total response size " + readChangesResponse.size());

        for (HGrid readChangesGrid : readChangesResponse) {
            if (readChangesGrid == null) {
                return;
            }
            syncReadChangesApiResponseToCCU(readChangesGrid, ccuHsApi);
        }
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
            HashMap<Object, Object> localFloor= CCUHsApi.getInstance().readMapById(floorEntity.get(Tags.ID).toString());
            String lastModifiedTimeInCloud = floorEntity.get(Tags.LAST_MODIFIED_TIME).toString();
            if (isCloudEntityHasLatestValue(localFloor, HDateTime.make(lastModifiedTimeInCloud).millis())) {
                Floor floor = new Floor.Builder()
                        .setDisplayName(floorEntity.get("dis").toString())
                        .setSiteRef(floorEntity.get("siteRef").toString())
                        .build();
                floor.setId(floorEntity.get("id").toString());
                floor.setOrientation(Double.parseDouble(floorEntity.get("orientation").toString()));
                floor.setFloorNum(Double.parseDouble(floorEntity.get("floorNum").toString()));
                floor.setCreatedDateTime(HDateTime.make(floorEntity.get("createdDateTime").toString()));
                floor.setLastModifiedDateTime(HDateTime.make(floorEntity.get(Tags.LAST_MODIFIED_TIME).toString()));
                floor.setLastModifiedBy(floorEntity.get("lastModifiedBy").toString());
                ccuHsApi.updateFloorLocally(floor, floor.getId());
            }
        }
    }

    private boolean isCloudScheduleHasLatestValue(HDict localSchedule, String lastModifiedTimeInCloud) {
        if (localSchedule.has(Tags.LAST_MODIFIED_TIME)) {
            HDateTime lastModifiedDateTimeInCCU = HDateTime.make(localSchedule.get(Tags.LAST_MODIFIED_TIME).toString());
            HDateTime lastModifiedDateTimeInCloud = HDateTime.make(lastModifiedTimeInCloud);
            logIt("lastModifiedDateTimeInCCU milli seconds >  " + lastModifiedDateTimeInCCU.millis());
            logIt("lastModifiedTimeInCloud milli seconds >  " + lastModifiedDateTimeInCloud.millis());
            logIt("Is CCU has latest value ? schedule " + (lastModifiedDateTimeInCCU.millis() > lastModifiedDateTimeInCloud.millis()));
            return lastModifiedDateTimeInCloud.millis() > lastModifiedDateTimeInCCU.millis();
        }
        logIt("lastModifiedDateTimeInCCU is null");
        return true;
    }

    private boolean isCloudPointHasLatestValue(HashMap<Object, Object> localPoint, Integer level, String lastModifiedTimeInCloud, CCUHsApi ccuHsApi) {
        String lastModifiedTimeInCCU = HSUtil.getLevelEntityOfPoint(Objects.requireNonNull(
                localPoint.get(Tags.ID)).toString(), level, Tags.LAST_MODIFIED_TIME, ccuHsApi);
        if (lastModifiedTimeInCCU != null) {
            HDateTime lastModifiedDateTimeInCCU = HDateTime.make(lastModifiedTimeInCCU);
            HDateTime lastModifiedDateTimeInCloud = HDateTime.make(lastModifiedTimeInCloud);
            logIt("lastModifiedDateTimeInCCU milli seconds >  " + lastModifiedDateTimeInCCU.millis());
            logIt("lastModifiedTimeInCloud milli seconds >  " + lastModifiedDateTimeInCloud.millis());
            logIt("Is CCU has latest value point ? " + (lastModifiedDateTimeInCCU.millis() > lastModifiedDateTimeInCloud.millis()));
            return lastModifiedDateTimeInCloud.millis() > lastModifiedDateTimeInCCU.millis();
        }
        logIt("last Modified DateTime In CCU for this point"+localPoint.get(Tags.ID)+ "is null");
        return true;
    }

    private boolean isCloudPointHasLatestValue(HashMap<Object, Object> localPoint, String lastModifiedTimeInCloud, CCUHsApi ccuHsApi) {
        String lastModifiedTimeInCCU = ccuHsApi.readPointPriorityLatestTime(localPoint.get(Tags.ID).toString());
        logIt("lastModifiedTimeInCCU aaa >  " + lastModifiedTimeInCCU);

        if (lastModifiedTimeInCCU != null) {
            HDateTime lastModifiedDateTimeInCCU = HDateTime.make(lastModifiedTimeInCCU);
            HDateTime lastModifiedDateTimeInCloud = HDateTime.make(lastModifiedTimeInCloud);
            logIt("lastModifiedDateTimeInCCU milli seconds >  " + lastModifiedDateTimeInCCU.millis());
            logIt("lastModifiedTimeInCloud milli seconds >  " + lastModifiedDateTimeInCloud.millis());
            logIt("Is CCU has latest value point ? " + (lastModifiedDateTimeInCCU.millis() > lastModifiedDateTimeInCloud.millis()));
            return lastModifiedDateTimeInCloud.millis() > lastModifiedDateTimeInCCU.millis();
        }
        logIt("last Modified DateTime In CCU for this point"+localPoint.get(Tags.ID)+ "is null");
        return true;
    }
    public static boolean isCloudEntityHasLatestValue(HashMap<Object, Object> entity, Long timeToken) {
        Log.i("ccu_read_changes","isCloudEntityHasLatestValue");
        Log.i("ccu_read_changes","entity "+entity);
        Log.i("ccu_read_changes","timetoken "+timeToken);
        if(entity.containsKey(Tags.LAST_MODIFIED_TIME) && timeToken != null) {
            String lastModifiedTimeInCCU = entity.get(Tags.LAST_MODIFIED_TIME).toString();
            HDateTime lastModifiedDateTimeInMessage = HDateTime.make(timeToken);
            Log.i("ccu_read_changes","lastModifiedTimeInCCU "+lastModifiedTimeInCCU);
            Log.i("ccu_read_changes","lastModifiedDateTimeInMessage "+lastModifiedDateTimeInMessage);

            HDateTime lastModifiedDateTimeInCCU = HDateTime.make(lastModifiedTimeInCCU);
            Log.i("ccu_read_changes","lastModifiedDateTimeInMessage milli "+lastModifiedDateTimeInMessage.millis());
            Log.i("ccu_read_changes","lastModifiedTimeInCCU milli "+lastModifiedDateTimeInCCU.millis());
            Log.i("ccu_read_changes","Is cloud has latest value ? " + (lastModifiedDateTimeInCCU.millis() < lastModifiedDateTimeInMessage.millis()));

            return lastModifiedDateTimeInCCU.millis() < lastModifiedDateTimeInMessage.millis();
        }
        return true;
    }

    private void syncRoom(List<HashMap<Object, Object>> roomEntities, CCUHsApi ccuHsApi) {
        for (HashMap<Object, Object> roomEntity : roomEntities) {
            logIt("Sync room entity >> " + roomEntity);
            HashMap<Object, Object> localRoom = ccuHsApi.readMapById(roomEntity.get(Tags.ID).toString());
            String lastModifiedTimeInCloud = roomEntity.get(Tags.LAST_MODIFIED_TIME).toString();
            if (isCloudEntityHasLatestValue(localRoom, HDateTime.make(lastModifiedTimeInCloud).millis())) {
                Zone zone = new Zone.Builder()
                        .setDisplayName(roomEntity.get("dis").toString())
                        .setSiteRef(roomEntity.get("siteRef").toString())
                        .setFloorRef(roomEntity.get("floorRef").toString())
                        .build();
                zone.setId(roomEntity.get("id").toString());
                zone.setScheduleRef(roomEntity.get("scheduleRef").toString());
                zone.setCreatedDateTime(HDateTime.make(roomEntity.get("createdDateTime").toString()));
                zone.setLastModifiedDateTime(HDateTime.make(roomEntity.get(Tags.LAST_MODIFIED_TIME).toString()));
                zone.setLastModifiedBy(roomEntity.get("lastModifiedBy").toString());
                ccuHsApi.updateZoneLocally(zone, roomEntity.get("id").toString());
            }
        }
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
                    if (isCloudScheduleHasLatestValue(schedule,
                            scheduleDict.get(Tags.LAST_MODIFIED_TIME).toString())) {
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
                    MapImpl map = (MapImpl) hList.get(priorityArrayIndex);
                    logIt("Priority Array map " + map);
                    setVal(map, pointToSync, ccuHsApi);
                }
            }
        });
    }

    private void setVal(MapImpl map, HashMap<Object, Object> pointHash, CCUHsApi ccuHsApi) {
        int level = Integer.parseInt(map.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).toString());
        String value = map.get(HayStackConstants.WRITABLE_ARRAY_VAL).toString();
        String who = map.get(HayStackConstants.WRITABLE_ARRAY_WHO).toString();
        String localValue = HSUtil.getLevelEntityOfPoint(Objects.requireNonNull(
                pointHash.get(Tags.ID)).toString(), level, HayStackConstants.WRITABLE_ARRAY_VAL, ccuHsApi);
        String pointRef = Objects.requireNonNull(pointHash.get(Tags.ID)).toString();
        logIt("LEVEL " + level + " VALUE FROM READ_CHANGES CALL " + value + " WHO " + who + " PointRef " +
                pointRef + " LOCAL_VALUE " + localValue);
        String lastModifiedTimeInCloud = map.get(Tags.LAST_MODIFIED_TIME).toString();
        if (!value.equals(localValue)) {
            HashMap<Object, Object> localPointHash = ccuHsApi.readMapById(pointHash.get(Tags.ID).toString());
            if (pointHash.containsKey("scheduleType") && !pointHash.containsKey("modbus")) {
                 if(isCloudPointHasLatestValue(localPointHash, lastModifiedTimeInCloud, ccuHsApi)){
                    updateScheduleType(pointRef, level, value, pointHash, who, ccuHsApi);
                }
                 return;
            }
            if (pointHash.containsKey("desired") && !pointHash.containsKey("modbus")) {
                if(isCloudPointHasLatestValue(localPointHash, lastModifiedTimeInCloud, ccuHsApi)) {
                    updateDesiredTemperature(pointRef, level, value, pointHash, who, ccuHsApi);
                }
                return;
            }
            if(isCloudPointHasLatestValue(pointHash, level, lastModifiedTimeInCloud, ccuHsApi)) {
                updatePoint(pointRef, level, value, pointHash, who, ccuHsApi);
                Log.i("CCU_READ_CHANGES", " Synced Point " + pointRef);
            }
        }
    }

    private void updatePoint(String pointRef, int level, String value, HashMap<Object, Object> pointHash, String who, CCUHsApi ccuHsApi) {
        try {
            double doubleValue = Double.parseDouble(value);
            ccuHsApi.writePointLocal(pointRef, level,
                    who, doubleValue, 0);
            if (pointHash.containsKey(Tags.WRITABLE)) {
                JsonObject body = new JsonObject();
                body.addProperty("val", Integer.parseInt(value));
                body.addProperty("level", level);
                body.addProperty("id", pointRef.replace("@", ""));
                body.addProperty("command", "updatePoint");
                body.addProperty("who", who);
                UpdatePointHandler.handleMessage(body, null);
            }
            Log.i("CCU_READ_CHANGES", " Synced Point val " + doubleValue);
        } catch (NumberFormatException e) {
            logIt(" NumberFormatException " + e);
            ccuHsApi.writePointStrValLocal(pointRef, level, who, value, 0);
            Log.i("CCU_READ_CHANGES", " Synced Point val " + value);
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
    public static long getMessageExpiryTime() {
        if(BuildConfig.BUILD_TYPE.equals("dev") || BuildConfig.BUILD_TYPE.equals("qa")){
            return MESSAGE_EXPIRY_TIME_FOR_DEV_QA;
        }else {
            return MESSAGE_EXPIRY_TIME_FOR_STAG_PROD;
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
