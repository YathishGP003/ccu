package a75f.io.messaging.handler;

import static a75f.io.messaging.handler.DataSyncHandler.isCloudScheduleHasLatestValue;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.interfaces.BuildingScheduleListener;
import a75f.io.logic.interfaces.IntrinsicScheduleListener;
import a75f.io.logic.util.CommonTimeSlotFinder;
import a75f.io.messaging.MessageHandler;
import a75f.io.util.ExecutorTask;

public class UpdateScheduleHandler implements MessageHandler
{
    public static final String CMD = "updateSchedule";
    public static final String ADD_SCHEDULE = "addSchedule";
    public static final String DELETE_SCHEDULE = "deleteSchedule";
    private static BuildingScheduleListener scheduleListener;
    private static IntrinsicScheduleListener intrinsicScheduleListener;

    public static void handleMessage(JsonObject msgObject) {
        String uid = msgObject.get("id").getAsString();
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        HashMap<Object,Object> scheduleEntity = ccuHsApi.read("id == " + HRef.make(uid));
        HDictBuilder b = new HDictBuilder().add("id", HRef.copy(uid));
        HDict[] dictArr = {b.toDict()};
        String response = HttpUtil.executePost(ccuHsApi.getHSUrl() + "read", HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
        CcuLog.d(L.TAG_CCU_PUBNUB, "Read Schedule : " + response);
        if(response == null || response.isEmpty()){
            CcuLog.d(L.TAG_CCU_PUBNUB, "Failed to read remote schedule : " + response);
            return;
        }
        HGrid sGrid = new HZincReader(response).readGrid();
        if (sGrid == null)
        {
            CcuLog.d(L.TAG_CCU_PUBNUB, "Failed to read remote schedule : " + uid);
            return;
        }
        Iterator it = sGrid.iterator();
        while (it.hasNext())
        {
            HRow r = (HRow) it.next();
            HDateTime lastModifiedDateTime;
            Object lastModifiedTimeTag = r.get("lastModifiedDateTime", false);
            if (lastModifiedTimeTag != null) {
                lastModifiedDateTime = (HDateTime) lastModifiedTimeTag;
            } else {
                lastModifiedDateTime = null;
            }
            if(!isCloudScheduleHasLatestValue(scheduleEntity, lastModifiedDateTime)){
                CcuLog.i(L.TAG_CCU_READ_CHANGES,"CCU HAS LATEST VALUE ");
                return;
            }
            if (ccuHsApi.isEntityExisting("@" + uid))
            {
                HDict scheduleDict = new HDictBuilder().add(r).toDict();
                CcuLog.d(L.TAG_CCU_PUBNUB,"scheduleDict = "+ scheduleDict);
                if(scheduleDict.has("named")){
                    ccuHsApi.updateNamedSchedule(uid,scheduleDict);

                    List<HashMap<Object, Object>> scheds =  ccuHsApi.getAllNamedSchedules();

                    for (HashMap<Object, Object> a:
                            scheds ) {
                        CcuLog.d(L.TAG_CCU_PUBNUB, "named sched = " + a.get("dis"));
                    }


                    return;
                }
                if(scheduleDict.has(Tags.SPECIAL)){
                    ccuHsApi.updateHDictNoSync(uid, scheduleDict);
                    break;
                }
                final Schedule s = new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build();
                s.setId(uid);
                s.setmSiteId(ccuHsApi.getSiteIdRef().toString());
                if (s.isVacation()) {
                    ccuHsApi.updateScheduleNoSync(s, null);
                    if (s.getRoomRef()!= null)
                        ccuHsApi.updateScheduleNoSync(s, s.getRoomRef());
                }
                else if (s.getMarkers().contains("building") && !s.getMarkers().contains("zone"))
                {
                    ccuHsApi.updateScheduleNoSync(s, null);
                    Schedule systemSchedule = ccuHsApi.getSystemSchedule(false).get(0);
                    if (!systemSchedule.equals(s))
                    {
                        CcuLog.d(L.TAG_CCU_PUBNUB, "Building Schedule Changed : Trim Zones Schedules!!");
                        ccuHsApi.updateScheduleNoSync(s, null);
                        CommonTimeSlotFinder commonTimeSlotFinder = new CommonTimeSlotFinder();
                        commonTimeSlotFinder.forceTrimScheduleTowardsCommonTimeslot(ccuHsApi);
                    }
                }
                else if (s.getMarkers().contains("zone") && !s.getMarkers().contains("building") && s.getRoomRef() != null)
                {
                    ccuHsApi.updateScheduleNoSync(s, s.getRoomRef());
                }
            }
            else
            {
                //New schedule/vacation added by apps.
                HDict scheduleDict = new HDictBuilder().add(r).toDict();
                if(scheduleDict.has("named")){
                    ccuHsApi.updateNamedSchedule(uid,scheduleDict);
                    List<HashMap<Object, Object>> scheds =  ccuHsApi.getAllNamedSchedules();
                    for (HashMap<Object, Object> a:
                            scheds ) {
                        CcuLog.d(L.TAG_CCU_PUBNUB, "named sched = " + a.get("dis"));
                    }
                    return;
                }
                if(scheduleDict.has(Tags.SPECIAL)){
                    ccuHsApi.addSchedule(uid, scheduleDict);
                    ccuHsApi.setSynced("@" + uid);
                    break;
                }
                Schedule s = new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build();
                s.setmSiteId(ccuHsApi.getSiteIdRef().toString());
                s.setId(uid);
                if (s.getRoomRef() != null && s.isZoneSchedule() && ccuHsApi.isEntityExisting(s.getRoomRef()))
                {
                    CcuLog.d(L.TAG_CCU_PUBNUB, "Add zone schedule "+s);
                    ccuHsApi.addSchedule(uid, s.getZoneScheduleHDict(s.getRoomRef()));
                }
                else if (s.isBuildingSchedule()&& !s.isZoneSchedule())
                {
                    ccuHsApi.addSchedule(uid, s.getScheduleHDict());
                }
                ccuHsApi.setSynced("@" + uid);
            }
            ScheduleManager.getInstance().updateSchedules();
        }
        refreshSchedulesScreen();
        refreshIntrinsicSchedulesScreen();
    }
    
    public static void refreshSchedulesScreen() {
        if (scheduleListener != null) {
            scheduleListener.refreshScreen();
        }
    }

    public static void refreshIntrinsicSchedulesScreen() {
        if (intrinsicScheduleListener != null) {
            intrinsicScheduleListener.updateIntrinsicSchedule();
        }
    }
    
    public static void setBuildingScheduleListener(BuildingScheduleListener listener) {
        scheduleListener = listener;
    }

    public static void setIntrinsicScheduleListener(IntrinsicScheduleListener listener) {
        intrinsicScheduleListener = listener;
    }

    @NonNull
    @Override
    public List<String> getCommand() {
        return Arrays.asList(CMD,ADD_SCHEDULE, DELETE_SCHEDULE);
    }

    @Override
    public void handleMessage(@NonNull JsonObject jsonObject, @NonNull Context context) {
        if (jsonObject.get("command").getAsString().equals(DELETE_SCHEDULE) && jsonObject.get("id") != null) {
            CCUHsApi.getInstance().removeEntity(jsonObject.get("id").getAsString());
            return;
        }
        handleMessage(jsonObject);
    }

    @Override
    public boolean ignoreMessage(@NonNull JsonObject jsonObject, @NonNull Context context) {
        return false;
    }
}
