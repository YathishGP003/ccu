package a75f.io.messaging.handler;

import com.google.gson.JsonObject;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;
import java.util.HashMap;
import java.util.Iterator;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ScheduleType;

public class UpdateEntityHandler {
    public static final String CMD = "updateEntity";
    public static void updateEntity(JsonObject msgObject){
        String uid = msgObject.get("ids").getAsJsonArray().get(0).toString().replaceAll("\"", "");
        HashMap<Object,Object> entity = CCUHsApi.getInstance().read("id == " + HRef.make(uid));
        if(entity.get("room") != null){
            updateNamedSchedule(entity,uid);
        }
        else if (entity.get("floor") != null) {
            HDictBuilder b = new HDictBuilder().add("id", HRef.copy(uid));
            HDict[] dictArr  = {b.toDict()};
            String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "read",
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
                    CCUHsApi.getInstance().updateFloorLocally(floor, floor.getId());
                }
            }
        }
    }

    private static void updateNamedSchedule(HashMap<Object,Object> entity,String uid) {
        HDictBuilder b = new HDictBuilder().add("id", HRef.copy(uid));
        HDict[] dictArr  = {b.toDict()};
        String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "read",
                HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
        CcuLog.i(L.TAG_CCU_MESSAGING, "Updated Zone Fetched: "+response);
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
                CCUHsApi.getInstance().updateZoneLocally(zone, entity.get("id").toString());
            }
        }
    }
}
