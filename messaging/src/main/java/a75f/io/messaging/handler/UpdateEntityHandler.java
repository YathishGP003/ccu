package a75f.io.messaging.handler;

import static a75f.io.api.haystack.CCUHsApi.INTENT_POINT_DELETED;
import static a75f.io.api.haystack.CCUTagsDb.BROADCAST_BACNET_POINT_ADDED;
import static a75f.io.logic.L.TAG_CCU_BACNET;
import static a75f.io.logic.L.TAG_CCU_MESSAGING;
import static a75f.io.logic.bo.util.CustomScheduleUtilKt.updateWritableDataUponCustomControlChanges;
import static a75f.io.messaging.handler.DataSyncHandler.isCloudEntityHasLatestValue;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.client.HClient;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.observer.HisWriteObservable;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.interfaces.BuildingOccupancyListener;
import a75f.io.logic.interfaces.ZoneDataInterface;
import a75f.io.logic.util.CommonTimeSlotFinder;
import a75f.io.messaging.MessageHandler;

public class UpdateEntityHandler implements MessageHandler {
    public static final String CMD = "updateEntity";
    private static ZoneDataInterface zoneDataInterface = null;
    private static BuildingOccupancyListener buildingOccupancyListener = null;


    public static void updateEntity(JsonObject msgObject, long timeToken){
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        List<HRef> entityIds = new ArrayList<>();
        msgObject.get("ids").getAsJsonArray().forEach( msgJson -> {
            CcuLog.i(L.TAG_CCU_MESSAGING, " UpdateEntityHandler "+msgJson.toString());
            String uid = msgJson.toString().replaceAll("\"", "");
            HashMap<Object,Object> entity = CCUHsApi.getInstance().read("id == " + HRef.make(uid));
            if (entity.get(Tags.MODBUS) == null && !isCloudEntityHasLatestValue(entity, timeToken)) {
                CcuLog.i("ccu_read_changes", "CCU HAS LATEST VALUE ");
                return;
            }
            if(entity.get(Tags.MODBUS) != null && entity.get(Tags.EQUIP) != null){
                updatePipeRefForModbusEquip(uid, entity);
            }
            else if(entity.get("room") != null){
                updateNamedSchedule(entity, uid, ccuHsApi);
                HisWriteObservable.INSTANCE.notifyChange("schedule", 0);
            }
            else if (entity.get("floor") != null) {
                HDictBuilder b = new HDictBuilder().add("id", HRef.copy(uid));
                HDict[] dictArr  = {b.toDict()};
                String response = HttpUtil.executePost(ccuHsApi.getHSUrl() + "read",
                        HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
                if (response != null) {
                    HZincReader hZincReader = new HZincReader(response);
                    Iterator hZincReaderIterator = hZincReader.readGrid().iterator();
                    while (hZincReaderIterator.hasNext()) {
                        HRow row = (HRow) hZincReaderIterator.next();
                        Floor floor = new Floor.Builder()
                                .setDisplayName(row.get("dis").toString())
                                .setSiteRef(row.get("siteRef").toString())
                                .build();
                        floor.setId(row.get("id").toString());
                        floor.setOrientation(Double.parseDouble(row.get("orientation").toString()));
                        floor.setFloorNum(Double.parseDouble(row.get("floorNum").toString()));
                        floor.setCreatedDateTime(getCreatedDateTime(row));
                        floor.setLastModifiedDateTime(getLastModifiedTime(row));
                        floor.setLastModifiedBy(getLastModifiedBy(row, ccuHsApi));
                        ccuHsApi.updateFloorLocally(floor, floor.getId());
                    }
                }
            } else if (entity.containsKey(Tags.BUILDING) && entity.containsKey(Tags.OCCUPANCY)) {
                updateBuildingOccupancy(uid,ccuHsApi);
            } else if (entity.containsKey(Tags.POINT) || entity.containsKey(Tags.EQUIP) ||
                    entity.containsKey(Tags.DEVICE)) {
                //check for points or equips and store them
                entityIds.add(HRef.copy(StringUtils.prependIfMissing(uid, "@")));
            }
        });

        if (!entityIds.isEmpty()){
            remoteFetchEntities(entityIds);
        }

    }

    private static void remoteFetchEntities(List<HRef> entityIds) {

        CcuLog.i(L.TAG_CCU_MESSAGING, "remoteFetchEntities >> ");

        HRef[] ids = entityIds.toArray(new HRef[0]);
        HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HGrid entitiesGrid = hClient.readByIds(ids, true);
        if (entitiesGrid == null) {
            CcuLog.e(L.TAG_CCU_MESSAGING, "remoteFetchEntities: null ");
            return;
        }

        Iterator it = entitiesGrid.iterator();
        while (it.hasNext()) {
            HRow row = (HRow) it.next();
            String entityId = row.get("id").toString();

            if(!CCUHsApi.getInstance().isIdValid(entityId)) {
                CcuLog.d(TAG_CCU_MESSAGING, "Skipping update entity for invalid id");
                continue;
            }

            if (isModbusInputOrDiscreteInput(row)) {
                CcuLog.e(L.TAG_CCU_MESSAGING, "Ignore Message !"+ entityId);
                continue;
            }

            if (row.has("equip")) {
                Equip equip = new Equip.Builder().setHDict(row).build();
                CCUHsApi.getInstance().tagsDb.updateEquip(equip, entityId);
            } else if(row.has("device")) {
                Device device = new Device.Builder().setHDict(row).build();
                CCUHsApi.getInstance().tagsDb.updateDevice(device, entityId);
            } else if (row.has("physical") && row.get("physical") != null) {
                RawPoint point = new RawPoint.Builder().setHDict((HDict) row).build();
                CCUHsApi.getInstance().tagsDb.updatePoint(point, entityId);
            } else {
                Point point = new Point.Builder().setHDict(row).build();
                if(isExternalPoint(point)) {
                    updateWritableDataUponCustomControlChanges(point);
                }
                updateEquipRefIfRequired(point);
                updatePointToBacnet(point, entityId);
                CCUHsApi.getInstance().tagsDb.updatePoint(point, entityId);
                CcuLog.d(L.TAG_CCU_MESSAGING, "UpdateEntityHandler: point updated--> "+row);
            }

            CcuLog.i(L.TAG_CCU_MESSAGING,"<< Entities Imported");
        }
    }

    /**
     * Local building tuner equip and cloud building tuner equip are different for every CCU
     * So when CCU receives Update entity Check if the point is a building tuner then
     * update equip ref of the tuner with local building tuner equip id
     * @param point
     * @return
     */
    private static void updateEquipRefIfRequired(Point point) {
        if (isBuildingTuner(point)) {
            Equip equip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().readEntity("building and equip")).build();
            if (equip != null) {
                point.setEquipRef(equip.getId());
                CcuLog.d(L.TAG_CCU_MESSAGING, "domainName "+ point.getDomainName() + " updated with local building tuner equip id: "+equip.getId());
            } else {
                CcuLog.d(L.TAG_CCU_MESSAGING, "Local Building Tuner Equip not found");
            }
        }
    }

    /**
     * It will check the point is building tuner point or not (Not ccu ref is added because some of
     * the non dm hyperstat tuner points has default tags)
     *
     * @param point
     * @return
     */
    private static boolean isBuildingTuner(Point point) {
        return (point.getMarkers().contains(Tags.POINT) && point.getMarkers().contains(Tags.DEFAULT) && (point.getCcuRef() == null) && (point.getMarkers().contains(Tags.TUNER) || point.getMarkers().contains(Tags.SCHEDULABLE)));
    }

    private static boolean isModbusInputOrDiscreteInput(HRow row) {

        if((row.has("registerType") && (row.get("registerType").toString().equals("inputRegister")
                || row.get("registerType").toString().equals("discreteInput"))) &&
                (row.has("writable") )){
            return true;
        } else if (row.has("logical")) {
            HashMap<Object, Object> physicalEntity = CCUHsApi.getInstance().readEntity
                    ("physical and pointRef == \"" + row.get("id").toString() + "\"");
            if (physicalEntity != null && physicalEntity.containsKey("registerType") &&
                    (physicalEntity.get("registerType").toString().equals("inputRegister")
                    || physicalEntity.get("registerType").toString().equals("discreteInput")) &&
                    (row.has("writable"))) {
                return true;
            }
        }
        return false;
    }

    private static void updatePipeRefForModbusEquip(String uid, HashMap<Object, Object> entity) {
        HDictBuilder b = new HDictBuilder().add(Tags.ID, HRef.copy(uid));
        HDict[] dictArr  = {b.toDict()};
        String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "read",
                HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
        if (response != null) {
            HZincReader hZincReader = new HZincReader(response);
            Iterator hZincReaderIterator = hZincReader.readGrid().iterator();
            while (hZincReaderIterator.hasNext()) {
                HRow row = (HRow) hZincReaderIterator.next();
                if(row.has(Tags.PIPEREF)) {
                    entity.put(Tags.PIPEREF, row.get(Tags.PIPEREF).toString());
                    Equip modbusEquip = new Equip.Builder().setHashMap(entity).build();
                    CCUHsApi.getInstance().updateEquipLocally(modbusEquip, modbusEquip.getId());
                }
            }
        }
    }

    private static void updateNamedSchedule(HashMap<Object,Object> entity,String uid, CCUHsApi ccuHsApi) {
        HDictBuilder b = new HDictBuilder().add("id", HRef.copy(uid));
        HDict[] dictArr = {b.toDict()};
        String response = HttpUtil.executePost(ccuHsApi.getHSUrl() + "read",
                HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
        CcuLog.i(L.TAG_CCU_MESSAGING, "Updated Zone Fetched: " + response);
        if (response != null) {
            HZincReader hZincReader = new HZincReader(response);
            Iterator hZincReaderIterator = hZincReader.readGrid().iterator();
            while (hZincReaderIterator.hasNext()) {
                HRow row = (HRow) hZincReaderIterator.next();
                Zone zone = new Zone.Builder()
                        .setDisplayName(row.get("dis").toString())
                        .setSiteRef(row.get("siteRef").toString())
                        .setFloorRef(row.get("floorRef").toString())
                        .build();
                zone.setId(row.get("id").toString());
                zone.setScheduleRef(row.get("scheduleRef").toString());
                zone.setCreatedDateTime(getCreatedDateTime(row));
                zone.setLastModifiedDateTime(getLastModifiedTime(row));
                zone.setLastModifiedBy(getLastModifiedBy(row, ccuHsApi));
                zone.setBacnetId(getBacnetId(row, ccuHsApi));
                zone.setBacnetType(getBacnetType(row, ccuHsApi));
                CCUHsApi.getInstance().updateZoneLocally(zone, entity.get("id").toString());

                if (zoneDataInterface != null) {
                    zoneDataInterface.refreshScreen("", true);
                }
            }
        }
    }

    private static String getBacnetType(HRow row, CCUHsApi ccuHsApi) {
        if(row.has("bacnetType")) {
            return row.get("bacnetType").toString();
        }else {
            Zone zone = new Zone.Builder().setHashMap(ccuHsApi.readMapById(row.get("id").toString())).build();
            return zone.getBacnetType();
        }
    }

    private static int getBacnetId(HRow row, CCUHsApi ccuHsApi) {
        if(row.has("bacnetId")) {
            return Integer.parseInt(row.get("bacnetId").toString());
        }else {
            Zone zone = new Zone.Builder().setHashMap(ccuHsApi.readMapById(row.get("id").toString())).build();
            return zone.getBacnetId();
        }
    }

    private static String getLastModifiedBy(HRow row, CCUHsApi ccuHsApi) {
        Object lastModifiedBy = row.get("lastModifiedBy", false);
        if(lastModifiedBy != null){
            return lastModifiedBy.toString();
        }else {
            return ccuHsApi.getCCUUserName();
        }
    }

    private static HDateTime getLastModifiedTime(HRow row) {
        Object lastModifiedDateTime = row.get("lastModifiedDateTime", false);
        if(lastModifiedDateTime != null){
            return HDateTime.make(lastModifiedDateTime.toString());
        }else {
            return HDateTime.make(System.currentTimeMillis());
        }
    }

    private static HDateTime getCreatedDateTime(HRow row) {
        Object lastModifiedDateTime = row.get("createdDateTime", false);
        if(lastModifiedDateTime != null){
            return HDateTime.make(lastModifiedDateTime.toString());
        }else {
            return HDateTime.make(System.currentTimeMillis());
        }
    }
    public static void setZoneDataInterface(ZoneDataInterface in) { zoneDataInterface = in; }

    @NonNull
    @Override
    public List<String> getCommand() {
        return Collections.singletonList(CMD);
    }

    @Override
    public void handleMessage(@NonNull JsonObject jsonObject, @NonNull Context context) {
        long timeToken = jsonObject.get("timeToken").getAsLong();
        updateEntity(jsonObject, timeToken);
    }

    @Override
    public boolean ignoreMessage(@NonNull JsonObject jsonObject, @NonNull Context context) {

        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        AtomicBoolean validMessage = new AtomicBoolean(true);
        jsonObject.get("ids").getAsJsonArray().forEach( msgJson -> {
            CcuLog.i(L.TAG_CCU_MESSAGING, " UpdateEntityHandler "+msgJson.toString());
            String uid = msgJson.toString().replaceAll("\"", "");

            if(ccuHsApi.isEntityExisting(uid)){
                HashMap<Object,Object> entity = ccuHsApi.readEntity("id == " + HRef.make(uid));
                   if (entity.get(Tags.SCHEDULE) == null){
                       CcuLog.i(L.TAG_CCU_MESSAGING, " UpdateEntityHandler handle updated " + entity);
                       validMessage.set(false);
                }
            }
        });
        return validMessage.get();
    }

    private static void updateBuildingOccupancy(String uid, CCUHsApi ccuHsApi){
        String response = getResponseString(uid);
        if (response != null) {
            HZincReader hZincReader = new HZincReader(response);
            Iterator hZincReaderIterator = hZincReader.readGrid().iterator();
            while (hZincReaderIterator.hasNext()) {
                HDict schedule = (HDict) hZincReaderIterator.next();
                ccuHsApi.updateHDictNoSync(uid,
                        new HDictBuilder().add(schedule).toDict());
                CommonTimeSlotFinder commonTimeSlotFinder = new CommonTimeSlotFinder();
                commonTimeSlotFinder.forceTrimScheduleTowardsCommonTimeslot(ccuHsApi);
            }
        }
        refreshBuildingOccupancyScreen();
    }
    public static void setBuildingOccupancyListener(BuildingOccupancyListener listener) {
        buildingOccupancyListener = listener;
    }
    public static void refreshBuildingOccupancyScreen() {
        if (buildingOccupancyListener != null) {
            buildingOccupancyListener.refreshScreen();
        }
    }
    private static String getResponseString(String uid) {
        HDictBuilder b = new HDictBuilder().add("id", HRef.copy(uid));
        HDict[] dictArr = {b.toDict()};
        return HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "read",
                HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
    }

    private static boolean isExternalPoint(Point point) {
        return point.getMarkers().contains(Tags.ZONE) &&
                (point.getMarkers().contains(Tags.MODBUS) ||
                point.getMarkers().contains(Tags.BACNET_DEVICE_ID) ||
                point.getMarkers().contains(Tags.CONNECTMODULE));
    }

    private static void updatePointToBacnet(Point newEntity, String i) {
        CcuLog.d(TAG_CCU_BACNET, "@@updatePoint: updatePointToBacnet-->"+i);
        if(CCUHsApi.getInstance().tagsDb.isBacNetEnabled()){
            HashMap<Object, Object> oldEntity = CCUHsApi.getInstance().readMapById(i);
            if(bacnetTagsRemoved(newEntity, oldEntity)){
                CcuLog.d(TAG_CCU_BACNET, "@@updatePoint: remove point " + newEntity + " bacnetId: " + newEntity.getBacnetId() + " bacnetType: " + newEntity.getBacnetType() + "tags@-->" + newEntity.getTags().containsKey("bacnetId"));
                bacnetPointDeleteBroadcast(i);
            }else{
                if(writableTagAddedRemoved(newEntity, oldEntity) || bacnetTagsUpdated(newEntity,oldEntity)){
                    CcuLog.d(TAG_CCU_BACNET, "@@updatePoint: writable tag or bacnet tags update found. So remove old point and add new one");
                    bacnetPointDeleteBroadcast(i);

                    // Delay the addition broadcast by 10 seconds (10,000 milliseconds)
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        bacnetPointAddBroadcast(newEntity.getId());
                    }, 10_000);
                }

                if(bacnetTagsAdded(newEntity, oldEntity)){
                    CcuLog.d(TAG_CCU_BACNET, "@@updatePoint: bacnet tags added add this point");
                    bacnetPointAddBroadcast(newEntity.getId());
                }
            }
        }
    }
    private static void bacnetPointAddBroadcast(String id) {
        Intent intentPointAdded = new Intent(BROADCAST_BACNET_POINT_ADDED);
        intentPointAdded.putExtra("message", id);
        CCUHsApi.getInstance().getContext().sendBroadcast(intentPointAdded);
    }

    private static void bacnetPointDeleteBroadcast(String id) {
        Intent intent = new Intent(INTENT_POINT_DELETED);
        intent.putExtra("message", id);
        CCUHsApi.getInstance().getContext().sendBroadcast(intent);
    }
    private static boolean bacnetTagsRemoved(Point newEntity, HashMap<Object, Object> oldEntity){
        boolean bacnetTagsRemoved = false;
        if ((oldEntity.containsKey("bacnetId") || oldEntity.containsKey("bacnetType"))) {
            if (newEntity.getBacnetType() == null) {
                bacnetTagsRemoved = true;
            } else if (newEntity.getBacnetId() == 0 ) {
                bacnetTagsRemoved = true;
            }
        }
        return bacnetTagsRemoved;
    }

    private static boolean bacnetTagsAdded(Point newEntity, HashMap<Object, Object> oldEntity){
        boolean bacnetTagsAdded = false;
        if ((!oldEntity.containsKey("bacnetId") || !oldEntity.containsKey("bacnetType"))) {
            if (newEntity.getBacnetType() != null && newEntity.getBacnetId() != 0) {
                bacnetTagsAdded = true;
            }
        }
        return bacnetTagsAdded;
    }

    private static boolean bacnetTagsUpdated(Point newEntity, HashMap<Object, Object> oldEntity) {
        boolean bacnetTagsUpdated = false;
        if (oldEntity.containsKey("bacnetId") && newEntity.getBacnetId() != 0 &&
                Double.parseDouble((String) Objects.requireNonNull(oldEntity.getOrDefault("bacnetId", 0))) != newEntity.getBacnetId()) {
            bacnetTagsUpdated = true;
        } else if (oldEntity.containsKey("bacnetType") && newEntity.getBacnetType() != null &&
                !Objects.requireNonNull(oldEntity.getOrDefault("bacnetType", "")).toString().equals(newEntity.getBacnetType())) {
            bacnetTagsUpdated = true;
        }


        return bacnetTagsUpdated;
    }

    private static boolean writableTagAddedRemoved(Point newEntity, HashMap<Object, Object> oldEntity){
        boolean isWriteableTagAddedRemoved = false;
        if (!oldEntity.containsKey("writable") && newEntity.getMarkers().contains("writable")) {
            isWriteableTagAddedRemoved = true;
        }

        if (oldEntity.containsKey("writable") && !newEntity.getMarkers().contains("writable")) {
            isWriteableTagAddedRemoved = true;
        }

        return isWriteableTagAddedRemoved;
    }
}
