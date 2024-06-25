package a75f.io.renatus.schedules;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.schedule.BuildingOccupancy;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.renatus.util.Marker;

public class ScheduleUtil {
    public static void trimZoneSchedules(HashMap<String, ArrayList<Interval>> spillsMap) {

        for (String zoneId : spillsMap.keySet()) {

            Zone z = new Zone.Builder().setHashMap(CCUHsApi.getInstance().readMapById(zoneId)).build();
            HashMap<Object, Object> scheduleHashMap = CCUHsApi.getInstance().readEntity("schedule and not " +
                    "vacation and not special and roomRef == " +z.getId());
            if(scheduleHashMap.isEmpty())
                continue;
            Schedule zoneSchedule = CCUHsApi.getInstance().getScheduleById(scheduleHashMap.get("id").toString());
            ArrayList<Interval> spills = spillsMap.get(zoneId);

            Iterator daysIterator = zoneSchedule.getDays().iterator();
            while (daysIterator.hasNext()) {
                Schedule.Days d = (Schedule.Days) daysIterator.next();
                Interval i = zoneSchedule.getScheduledInterval(d);

                for (Interval spill : spills) {
                    if (!i.contains(spill)) {
                        continue;
                    }
                    if (spill.getStartMillis() <= i.getStartMillis() &&
                            spill.getEndMillis() >= i.getEndMillis()) {
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

            CcuLog.d(L.TAG_CCU_UI, " Trimmed Zone Schedule" + zoneSchedule);
            CCUHsApi.getInstance().updateZoneSchedule(zoneSchedule, zoneSchedule.getRoomRef());
        }
    }

    public static void trimZoneSchedule(Schedule s, HashMap<String, ArrayList<Interval>> spillsMap) {

        ArrayList<Interval> spills = spillsMap.get(s.getRoomRef());
        if (spills == null) {
            CcuLog.d(L.TAG_CCU_UI,"Schedule spills invalid for "+ s +" in "+ spillsMap);
            return;
        }
        CcuLog.d(L.TAG_CCU_UI,"Trim spills for "+ s +" in "+ spillsMap);
        HashMap<Schedule.Days, ArrayList<Interval>> validSpills = new HashMap<>();
        CopyOnWriteArrayList<Schedule.Days> days = new CopyOnWriteArrayList<>(s.getDays());
        CopyOnWriteArrayList<Schedule.Days> conflictDays = new CopyOnWriteArrayList<>();
        for (Schedule.Days d : days) {
            Interval i = s.getScheduledInterval(d);

            for (Interval spill : spills) {
                if (!i.contains(spill)) {
                    continue;
                }
                if (spill.getStartMillis() <= i.getStartMillis() &&
                        spill.getEndMillis() >= i.getEndMillis()) {
                    conflictDays.add(d);
                    continue;
                }
                validSpills.put(d, disconnectedIntervals(spills, i));
                conflictDays.add(d);
            }
        }
        for (Map.Entry<Schedule.Days, ArrayList<Interval>> entry : validSpills.entrySet()) {
            for (Interval in : entry.getValue()) {
                Schedule.Days d = entry.getKey();
                Schedule.Days dayBO = new Schedule.Days();
                dayBO.setEthh(in.getEnd().getHourOfDay());
                dayBO.setSthh(in.getStart().getHourOfDay());
                dayBO.setEtmm(in.getEnd().getMinuteOfHour());
                dayBO.setStmm(in.getStart().getMinuteOfHour());
                dayBO.setHeatingVal(d.getHeatingVal());
                dayBO.setCoolingVal(d.getCoolingVal());
                dayBO.setSunset(false);
                dayBO.setSunrise(false);
                dayBO.setDay(d.getDay());
                dayBO.setHeatingUserLimitMin(d.getHeatingUserLimitMin());
                dayBO.setHeatingUserLimitMax(d.getHeatingUserLimitMax());
                dayBO.setCoolingUserLimitMin(d.getCoolingUserLimitMin());
                dayBO.setCoolingUserLimitMax(d.getCoolingUserLimitMax());
                dayBO.setHeatingDeadBand(d.getHeatingDeadBand());
                dayBO.setCoolingDeadBand(d.getCoolingDeadBand());
                s.getDays().remove(d);
                s.getDays().add(dayBO);
            }

        }

        for (Schedule.Days d : conflictDays) {
            s.getDays().remove(d);
        }

        if(s.isNamedSchedule())
            CCUHsApi.getInstance().updateSchedule(s);
        else
            CCUHsApi.getInstance().updateZoneSchedule(s, s.getRoomRef());
    }

    public static Interval OverNightEnding(Interval Ending)
    {
        DateTime initialEnding = Ending.getStart().withTime(23, 59, 59, 0);
        return new Interval(Ending.getStart(), initialEnding);
    }
    public static Interval OverNightStarting(Interval Start)
    {
        DateTime subsequentStart = Start.getEnd().withTime(0, 0, 0, 0);
        return new Interval(subsequentStart, Start.getEnd());
    }

    public static Interval AddingNextWeekDayForOverNight(Schedule interval)
    {
        ArrayList<Interval> allIntervals = interval.getScheduledIntervals(interval.getDaysSorted());
        Interval AddingNewData = null;
        //overnight scenario
        Interval iv = allIntervals.get(allIntervals.size() - 1);
        DateTime startInterval = iv.getStart();
        DateTime nextDay = startInterval.plusDays(1);
        int nextDayofWeek = nextDay.getDayOfWeek() - 1;
        int monthNext = nextDay.getMonthOfYear();
        int yearNext = nextDay.getYear();
        int dayNext = nextDay.getDayOfMonth();
        BuildingOccupancy boTemp = CCUHsApi.getInstance().getBuildingOccupancy();
        List<BuildingOccupancy.Days> tempDayList = boTemp.getDays();
        BuildingOccupancy.Days individualDay = null;
        for (int i = 0; i < tempDayList.size(); i++) {
            if (nextDayofWeek == tempDayList.get(i).getDay()) {
                individualDay = tempDayList.get(i);
            }
        }
        if (individualDay != null) {
            int startHr = individualDay.getSthh();
            int startMin = individualDay.getStmm();
            int endHr = individualDay.getEthh();
            int endMin = individualDay.getEtmm();
            if (endHr == 24) {
                endHr = 23;
                endMin = 59;
            }
            DateTime startNext = new DateTime(yearNext, monthNext, dayNext, startHr, startMin);
            DateTime endNext = new DateTime(yearNext, monthNext, dayNext, endHr, endMin);
            AddingNewData = new Interval(startNext, endNext);
        }
        return AddingNewData;
    }

    public static ArrayList<Interval> disconnectedIntervals(List<Interval> intervals, Interval r) {
        ArrayList<Interval> result = new ArrayList<>();

        ArrayList<Marker> markers = new ArrayList<>();

        for (Interval i : intervals) {
            markers.add(new Marker(i.getStartMillis(), true));
            markers.add(new Marker(i.getEndMillis(), false));
        }

        markers.sort((a, b) -> Long.compare(a.val, b.val));


        int overlap = 0;
        boolean endReached = false;

        if (markers.get(0).val > r.getStartMillis()) {
            result.add(new Interval(r.getStartMillis(), markers.get(0).val));
        }

        for (int i = 0; i < markers.size() - 1; i++) {
            Marker m = markers.get(i);

            overlap += m.start ? 1 : -1;
            Marker next = markers.get(i + 1);

            if (m.val != next.val && overlap == 0 && next.val > r.getStartMillis()) {
                long start = Math.max(m.val, r.getStartMillis());
                long end = next.val;
                if (next.val > r.getEndMillis()) {
                    end = r.getEndMillis();
                    endReached = true;
                }
                // End instance must be greater than start instance
                if (start != end && end > start) {
                    result.add(new Interval(start, end));
                }
                if (endReached)
                    break;
            }
        }

        if (!endReached) {
            Marker m = markers.get(markers.size() - 1);
            if (m.val != r.getEndMillis() && m.val < r.getEndMillis()) {
                result.add(new Interval(m.val, r.getEndMillis()));
            }
        }

        return result;
    }

    public static String getDayString(int day) {
        switch (day) {
            case 1:
                return "Monday";
            case 2:
                return "Tuesday";
            case 3:
                return "Wednesday";
            case 4:
                return "Thursday";
            case 5:
                return "Friday";
            case 6:
                return "Saturday";
            case 7:
                return "Sunday";
        }
        return "";
    }
}
