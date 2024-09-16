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
        HashMap<Object,Object> scheduleEntity = CCUHsApi.getInstance().read("id == " + HRef.make(uid));
        HDictBuilder b = new HDictBuilder().add("id", HRef.copy(uid));
        HDict[] dictArr = {b.toDict()};
        String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "read", HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
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
            if (CCUHsApi.getInstance().isEntityExisting("@" + uid))
            {
                HDict scheduleDict = new HDictBuilder().add(r).toDict();
                CcuLog.d(L.TAG_CCU_PUBNUB,"scheduleDict = "+ scheduleDict);
                if(scheduleDict.has("named")){
                    CCUHsApi.getInstance().updateNamedSchedule(uid,scheduleDict);

                    List<HashMap<Object, Object>> scheds =  CCUHsApi.getInstance().getAllNamedSchedules();

                    for (HashMap<Object, Object> a:
                            scheds ) {
                        CcuLog.d(L.TAG_CCU_PUBNUB, "named sched = " + a.get("dis"));
                    }


                    return;
                }
                if(scheduleDict.has(Tags.SPECIAL)){
                    CCUHsApi.getInstance().updateHDictNoSync(uid, scheduleDict);
                    break;
                }
                final Schedule s = new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build();
                s.setId(uid);
                s.setmSiteId(CCUHsApi.getInstance().getSiteIdRef().toString());
                if (s.isVacation()) {
                    CCUHsApi.getInstance().updateScheduleNoSync(s, null);
                    if (s.getRoomRef()!= null)
                        CCUHsApi.getInstance().updateScheduleNoSync(s, s.getRoomRef());
                }
                else if (s.getMarkers().contains("building") && !s.getMarkers().contains("zone"))
                {
                    CCUHsApi.getInstance().updateScheduleNoSync(s, null);
                    Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);
                    if (!systemSchedule.equals(s))
                    {
                        CcuLog.d(L.TAG_CCU_PUBNUB, "Building Schedule Changed : Trim Zones Schedules!!");
                        ExecutorTask.executeBackground( () -> {
                            CCUHsApi.getInstance().updateScheduleNoSync(s, null);
                            trimZoneSchedules(s);
                            CCUHsApi.getInstance().scheduleSync();
                        });
                    }
                }
                else if (s.getMarkers().contains("zone") && !s.getMarkers().contains("building") && s.getRoomRef() != null)
                {
                    CCUHsApi.getInstance().updateScheduleNoSync(s, s.getRoomRef());
                }
            }
            else
            {
                //New schedule/vacation added by apps.
                HDict scheduleDict = new HDictBuilder().add(r).toDict();
                if(scheduleDict.has("named")){
                    CCUHsApi.getInstance().updateNamedSchedule(uid,scheduleDict);
                    List<HashMap<Object, Object>> scheds =  CCUHsApi.getInstance().getAllNamedSchedules();
                    for (HashMap<Object, Object> a:
                            scheds ) {
                        CcuLog.d(L.TAG_CCU_PUBNUB, "named sched = " + a.get("dis"));
                    }
                    return;
                }
                if(scheduleDict.has(Tags.SPECIAL)){
                    CCUHsApi.getInstance().addSchedule(uid, scheduleDict);
                    CCUHsApi.getInstance().setSynced("@" + uid);
                    break;
                }
                Schedule s = new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build();
                s.setmSiteId(CCUHsApi.getInstance().getSiteIdRef().toString());
                s.setId(uid);
                if (s.getRoomRef() != null && s.isZoneSchedule() && CCUHsApi.getInstance().isEntityExisting(s.getRoomRef()))
                {
                    CcuLog.d(L.TAG_CCU_PUBNUB, "Add zone schedule "+s);
                    CCUHsApi.getInstance().addSchedule(uid, s.getZoneScheduleHDict(s.getRoomRef()));
                }
                else if (s.isBuildingSchedule()&& !s.isZoneSchedule())
                {
                    CCUHsApi.getInstance().addSchedule(uid, s.getScheduleHDict());
                }
                CCUHsApi.getInstance().setSynced("@" + uid);
            }
            ScheduleManager.getInstance().updateSchedules();
        }
        refreshSchedulesScreen();
        refreshIntrinsicSchedulesScreen();
    }
    
    public static void trimZoneSchedules(Schedule buildingSchedule)
    {
        ArrayList<HashMap> zones = CCUHsApi.getInstance().readAll("room");
        for (HashMap m : zones)
        {
            if(m.containsKey("scheduleRef")) {
                ArrayList<Interval> intervalSpills = new ArrayList<>();
                Schedule zoneSchedule = CCUHsApi.getInstance().getScheduleById(m.get("scheduleRef").toString());
                if(zoneSchedule == null) {
                    // Handling Crash here.
                    zoneSchedule = CCUHsApi.getInstance().getRemoteSchedule(m.get("scheduleRef").toString());
                    CcuLog.d(L.TAG_CCU_PUBNUB, "Fetching the schedule from remote " + zoneSchedule);
                    if(zoneSchedule == null) {
                        CcuLog.d(L.TAG_CCU_PUBNUB, "The schedule retrieved from the remote source is also null.");
                        continue;
                    }
                }
                if (zoneSchedule.getMarkers().contains("disabled")) {
                    continue;
                }
                ArrayList<Interval> zoneIntervals = zoneSchedule.getScheduledIntervals();
                for (Interval v : zoneIntervals) {
                    CcuLog.d(L.TAG_CCU_PUBNUB, "Zone interval " + v);
                }
                ArrayList<Interval> systemIntervals = buildingSchedule.getMergedIntervals();
                ArrayList<Interval> splitSchedules = new ArrayList<>();
                for (Interval v : systemIntervals) {
                    //Multiday schedule starting on sunday has monday segment falling in the next week.Split and realign that.
                    if (v.getStart().getDayOfWeek() == 7 && v.getEnd().getDayOfWeek() == 1) {
                        long now = MockTime.getInstance().getMockTime();
                        DateTime startTime = new DateTime(now).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).withDayOfWeek(1);
                        DateTime endTime = new DateTime(now).withHourOfDay(v.getEnd().getHourOfDay()).withMinuteOfHour(v.getEnd().getMinuteOfHour()).withSecondOfMinute(v.getEnd().getSecondOfMinute()).withMillisOfSecond(v.getEnd().getMillisOfSecond()).withDayOfWeek(1);
                        splitSchedules.add(new Interval(startTime, endTime));
                    }
                }
                systemIntervals.addAll(splitSchedules);
                for (Interval v : systemIntervals) {
                    CcuLog.d(L.TAG_CCU_PUBNUB, "Merged System interval " + v);
                }
                for (Interval z : zoneIntervals) {
                    boolean contains = false;
                    for (Interval s : systemIntervals) {
                        if (s.contains(z)) {
                            contains = true;
                            break;
                        }
                    }
                    if (!contains) {
                        for (Interval s : systemIntervals) {
                            if (s.overlaps(z)) {
                                if (z.getStartMillis() < s.getStartMillis()) {
                                    intervalSpills.add(new Interval(z.getStartMillis(), s.getStartMillis()));
                                } else if (z.getEndMillis() > s.getEndMillis()) {
                                    intervalSpills.add(new Interval(s.getEndMillis(), z.getEndMillis()));
                                }
                                contains = true;
                                break;
                            }
                        }
                    }
                    if (!contains) {
                        intervalSpills.add(z);
                        CcuLog.d(L.TAG_CCU_PUBNUB, " Zone Interval not contained " + z);
                    }
                }
                Iterator daysIterator = zoneSchedule.getDays().iterator();
                while (daysIterator.hasNext()) {
                    Schedule.Days d = (Schedule.Days) daysIterator.next();
                    Interval i = zoneSchedule.getScheduledInterval(d);
                    for (Interval spill : intervalSpills) {
                        if (!i.contains(spill)) {
                            continue;
                        }
                        if (spill.getStartMillis() <= i.getStartMillis() && spill.getEndMillis() >= i.getEndMillis()) {
                            daysIterator.remove();
                            continue;
                        }
                        if (spill.getStartMillis() <= i.getStartMillis()) {
                            d.setSthh(spill.getEnd().getHourOfDay());
                            d.setStmm(spill.getEnd().getMinuteOfHour());
                        } else if (i.getEndMillis() >= spill.getStartMillis()) {
                            d.setEthh(spill.getStart().getHourOfDay());
                            d.setEtmm(spill.getStart().getMinuteOfHour());
                        }
                    }
                }
                if (zoneSchedule.getRoomRef()!= null && !intervalSpills.isEmpty()) {
                    CcuLog.d(L.TAG_CCU_PUBNUB, "Trimmed Zone Schedule " + zoneSchedule);
                    CCUHsApi.getInstance().updateZoneSchedule(zoneSchedule, zoneSchedule.getRoomRef());
                }
            }
        }
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
