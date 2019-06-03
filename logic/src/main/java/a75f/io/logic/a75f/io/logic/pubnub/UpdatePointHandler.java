package a75f.io.logic.a75f.io.logic.pubnub;

import android.util.Log;

import com.google.gson.JsonObject;

import org.projecthaystack.HGrid;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.jobs.ScheduleProcessJob;

public class UpdatePointHandler
{
    public static String getCmd() {
        return"updatePoint";
    }
    
    public static void handleMessage(JsonObject msgObject) {
        String src = msgObject.get("who").getAsString();
        if (src.equals("ccu") || src.equals("Scheduler") || src.equals("manual")) {
            CcuLog.d(L.TAG_CCU_PUBNUB, "PubNub received for CCU write : Ignore");
            return;
        }
        
        String guid = msgObject.get("id").getAsString();
        String luid = CCUHsApi.getInstance().getLUID("@" + guid);
        if (luid != null && luid != "")
        {
            HGrid pointGrid = CCUHsApi.getInstance().readPointArrRemote("@" + guid);
            if (pointGrid == null) {
                CcuLog.d(L.TAG_CCU_PUBNUB, "Failed to read remote point point : " + guid);
                return;
            }
            Iterator it = pointGrid.iterator();
            while (it.hasNext())
            {
                HRow r = (HRow) it.next();
                double level = Double.parseDouble(r.get("level").toString());
                double val = Double.parseDouble(r.get("val").toString());
                String who = r.get("who").toString();
                double duration = Double.parseDouble(r.get("dur").toString());
                double dur = duration - System.currentTimeMillis();
                CcuLog.d(L.TAG_CCU_PUBNUB, "Remote point:  level " + level + " val " + val + " who " + who + " duration "+duration+" dur "+dur);
                //If dur shows it is already expired, then just write 1ms to force-expire it locally.
                CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(luid), (int) level, who, HNum.make(val), HNum.make(duration == 0 ? 0 :
                                                                                                                                                       (dur < 0 ? 1 : duration - System.currentTimeMillis())));
            }
            Point p = new Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(luid)).build();
            ArrayList values = CCUHsApi.getInstance().readPoint(luid);
            if (values != null && values.size() > 0)
            {
                for (int l = 1; l <= values.size(); l++)
                {
                    HashMap valMap = ((HashMap) values.get(l - 1));
                    if (valMap.get("val") != null)
                    {
                        Log.d(L.TAG_CCU_PUBNUB, "Updated point " + p.getDisplayName() + " , level: " + l + " , val :" + Double.parseDouble(valMap.get("val").toString())
                                         + " duration " + valMap.get("duration"));
                    }
                }
            }
        
            if (p.getMarkers().contains("his"))
            {
                CCUHsApi.getInstance().writeHisValById(luid, CCUHsApi.getInstance().readPointPriorityVal(luid));
            }
        
            if (p.getMarkers().contains("desired"))
            {
                ScheduleProcessJob.handleDesiredTempUpdate(p, false, 0);
            }
        
            if (p.getMarkers().contains("scheduleType")) {
                ScheduleProcessJob.handleScheduleTypeUpdate(p);
            }
        }
        else
        {
            CcuLog.d(L.TAG_CCU_PUBNUB, "Received for invalid local point : " + luid);
        }
    }
}
