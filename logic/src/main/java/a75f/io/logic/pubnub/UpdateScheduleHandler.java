package a75f.io.logic.pubnub;

import android.util.Log;

import com.google.gson.JsonObject;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.jobs.ScheduleProcessJob;

public class UpdateScheduleHandler
{
    public static final String CMD = "updateSchedule";
    private static BuildingScheduleListener scheduleListener = null;
    
    public static void handleMessage(JsonObject msgObject)
    {
        String uid = msgObject.get("id").getAsString();
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
            if (CCUHsApi.getInstance().isEntityExisting("@" + uid))
            {
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
                        new Thread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                CCUHsApi.getInstance().updateScheduleNoSync(s, null);
                                trimZoneSchedules(s);
                                CCUHsApi.getInstance().scheduleSync();
                            }
                        }).start();
                        
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
                Schedule s = new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build();
                s.setmSiteId(CCUHsApi.getInstance().getSiteIdRef().toString());
                s.setId(uid);
                if (s.getRoomRef() != null && s.isZoneSchedule())
                {
                    CCUHsApi.getInstance().addSchedule(uid, s.getZoneScheduleHDict(s.getRoomRef()));
                }
                else if (s.isBuildingSchedule()&& !s.isZoneSchedule())
                {
                    CCUHsApi.getInstance().addSchedule(uid, s.getScheduleHDict());
                }
                CCUHsApi.getInstance().setSynced("@" + uid);
            }
            ScheduleProcessJob.updateSchedules();
        }
        refreshSchedulesScreen();
    }
    
    public static void trimZoneSchedules(Schedule buildingSchedule)
    {
        ArrayList<HashMap> zones = CCUHsApi.getInstance().readAll("room");
        for (HashMap m : zones)
        {
            if(m.containsKey("scheduleRef")) {
                ArrayList<Interval> intervalSpills = new ArrayList<>();
                Schedule zoneSchedule = CCUHsApi.getInstance().getScheduleById(m.get("scheduleRef").toString());
                CcuLog.d(L.TAG_CCU_PUBNUB, "Zone " + m + " " + zoneSchedule.toString());
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
                Log.d(L.TAG_CCU_PUBNUB, "Trimmed Zone Schedule " + zoneSchedule.toString());
                if (zoneSchedule.getRoomRef()!= null)
                CCUHsApi.getInstance().updateZoneSchedule(zoneSchedule, zoneSchedule.getRoomRef());
            }
        }
    }
    
    public static void refreshSchedulesScreen() {
        if (scheduleListener != null) {
            scheduleListener.refreshScreen();
        }
    }
    
    public static void setBuildingScheduleListener(BuildingScheduleListener listener) {
        scheduleListener = listener;
    }
}
