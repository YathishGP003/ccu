package a75f.io.messaging.handler;

import static a75f.io.messaging.handler.DataSyncHandler.isCloudEntityHasLatestValue;
import android.content.Context;

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
import java.util.concurrent.atomic.AtomicBoolean;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
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
        List<HRef> pointIds = new ArrayList<>();
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
            } else if (entity.containsKey(Tags.POINT)) {
                //check for points and store them
                pointIds.add(HRef.copy(StringUtils.prependIfMissing(uid, "@")));
            }
        });

        if (!pointIds.isEmpty()){
            remoteFetchPoints(pointIds);
        }

    }

    private static void remoteFetchPoints(List<HRef> entityIds) {

        CcuLog.i(L.TAG_CCU_MESSAGING, "remoteFetchPoints >> ");

        HRef[] ids = entityIds.toArray(new HRef[0]);
        HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        HGrid entitiesGrid = hClient.readByIds(ids, true);
        if (entitiesGrid == null) {
            CcuLog.e(L.TAG_CCU_MESSAGING, "remoteFetchPoints: null ");
            return;
        }

        Iterator it = entitiesGrid.iterator();
        while (it.hasNext()) {
            HRow row = (HRow) it.next();
            String entityId = row.get("id").toString();

            if (isModbusInputOrDiscreteInput(row)) {
                CcuLog.e(L.TAG_CCU_MESSAGING, "Ignore Message !"+ entityId);
                return;
            }




            if (row.has("physical") && row.get("physical") != null) {
                RawPoint point = new RawPoint.Builder().setHDict((HDict) row).build();
                CCUHsApi.getInstance().tagsDb.updatePoint(point, entityId);
            }else {
                Point point = new Point.Builder().setHDict((HDict) row).build();
                CCUHsApi.getInstance().tagsDb.updatePoint(point, entityId);
            }

            CcuLog.i(L.TAG_CCU_MESSAGING,"<< Entities Imported");
        }


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
}
