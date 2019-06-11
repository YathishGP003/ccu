package a75f.io.logic.pubnub;

import com.google.gson.JsonObject;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class UpdateScheduleHandler
{
    public static String getCmd() {
        return "updateSchedule";
    }
    public static void handleMessage(JsonObject msgObject) {
        
        String guid = msgObject.get("id").getAsString();
        String luid = CCUHsApi.getInstance().getLUID("@" + guid);
        if (luid != null && luid != "")
        {
            HDictBuilder b = new HDictBuilder().add("id", HRef.copy(guid));
            HDict[] dictArr = {b.toDict()};
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "read", HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
            CcuLog.d(L.TAG_CCU_PUBNUB, "Read Schedule : " + response);
            HGrid sGrid = new HZincReader(response).readGrid();
            if (sGrid == null)
            {
                CcuLog.d(L.TAG_CCU_PUBNUB, "PubNub Failed to read remote schedule : " + guid);
                return;
            }
            Iterator it = sGrid.iterator();
            while (it.hasNext())
            {
                HRow r = (HRow) it.next();
                Schedule s = new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build();
                s.setId(luid);
                if (s.getMarkers().contains("building")) {
                    CCUHsApi.getInstance().updateScheduleNoSync(s, null);
                } else if (s.getMarkers().contains("zone")) {
                    CCUHsApi.getInstance().updateScheduleNoSync(s, s.getRoomRef());
                }
            }
        } else {
            CcuLog.d(L.TAG_CCU_PUBNUB, " Invalid LUID for Schedule Update "+guid);
        }
    
    }
}
