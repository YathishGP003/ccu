package a75f.io.messaging.handler;

import static a75f.io.messaging.handler.DataSyncHandler.isCloudEntityHasLatestValue;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.interfaces.ZoneDataInterface;
import a75f.io.messaging.MessageHandler;

public class UpdateEntityHandler implements MessageHandler {
    public static final String CMD = "updateEntity";
    private static ZoneDataInterface zoneDataInterface = null;
    public static void updateEntity(JsonObject msgObject, long timeToken){
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
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
                updateBuildingOccupancy(uid);
            }
        });

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
                    zoneDataInterface.refreshScreen("");
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

    private static void updateBuildingOccupancy(String uid){
        String response = getResponseString(uid);
        if (response != null) {
            HZincReader hZincReader = new HZincReader(response);
            Iterator hZincReaderIterator = hZincReader.readGrid().iterator();
            while (hZincReaderIterator.hasNext()) {
                HDict schedule = (HDict) hZincReaderIterator.next();
                CCUHsApi.getInstance().updateHDictNoSync(uid,
                        new HDictBuilder().add(schedule).toDict());
                final Schedule s = new Schedule.Builder().setHDict(new HDictBuilder().add(schedule).toDict()).build();
                UpdateScheduleHandler.trimZoneSchedules(s);
            }
        }
    }

    private static String getResponseString(String uid) {
        HDictBuilder b = new HDictBuilder().add("id", HRef.copy(uid));
        HDict[] dictArr = {b.toDict()};
        return HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "read",
                HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
    }
}
