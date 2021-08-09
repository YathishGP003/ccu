package a75f.io.logic.pubnub;

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
import a75f.io.api.haystack.sync.HttpUtil;

public class FloorUpdateHandler {
    public static final String CMD = "updateEntity";
    public static void updateFloor(JsonObject msgObject){
        String uid = msgObject.get("ids").getAsJsonArray().get(0).toString().replaceAll("\"", "");
        HashMap entity = CCUHsApi.getInstance().read("id == " + HRef.make(uid));
        if (entity.get("floor") == null) {
            return;
        }
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
                CCUHsApi.getInstance().updateFloor(floor, floor.getId());
            }
        }
    }
}
