package a75f.io.renatus.schedules;

import android.util.Log;

import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.renatus.util.Marker2;

public class ScheduleUtil
{
    public static void trimZoneSchedules(HashMap<String, ArrayList<Interval>> spillsMap) {
        
        for (String zoneId : spillsMap.keySet()) {
            
            Zone z = new Zone.Builder().setHashMap(CCUHsApi.getInstance().readMapById(zoneId)).build();
            Schedule zoneSchedule = CCUHsApi.getInstance().getScheduleById(z.getScheduleRef());
            ArrayList<Interval> spills = spillsMap.get(zoneId);
            
            Iterator daysIterator = zoneSchedule.getDays().iterator();
            while(daysIterator.hasNext()) {
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
            
            Log.d("CCU_UI "," Trimmed Zone Schedule"+zoneSchedule.toString());
            CCUHsApi.getInstance().updateZoneSchedule(zoneSchedule, zoneSchedule.getRoomRef());
        }
    }
    
    public static void trimZoneSchedule(Schedule s, HashMap<String, ArrayList<Interval>> spillsMap) {
    
        ArrayList<Interval> spills = spillsMap.get(s.getRoomRef());
        ArrayList<Interval> validSpills = new ArrayList<>();
        CopyOnWriteArrayList<Schedule.Days> days = new CopyOnWriteArrayList<>(s.getDays());
        for(Schedule.Days d: days) {
            Interval i = s.getScheduledInterval(d);

            for (Interval spill : spills) {
                if (!i.contains(spill)) {
                    continue;
                }
                if (spill.getStartMillis() <= i.getStartMillis() &&
                    spill.getEndMillis() >= i.getEndMillis()) {
                    continue;
                }
                validSpills.clear();
                validSpills.addAll(disconnectedIntervals(spills, i));
                /*if (spill.getStartMillis() <= i.getStartMillis()) {
                    d.setSthh(spill.getEnd().getHourOfDay());
                    d.setStmm(spill.getEnd().getMinuteOfHour());
                } else if (i.getEndMillis() >= spill.getStartMillis()) {
                    d.setEthh(spill.getStart().getHourOfDay());
                    d.setEtmm(spill.getStart().getMinuteOfHour());
                }*/
            }
            for (Interval in:validSpills){
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
                s.getDays().remove(d);
                s.getDays().add(dayBO);

                Log.d("CCU_UI "," Trimmed Zone Schedule"+s.toString());
                CCUHsApi.getInstance().updateZoneSchedule(s, s.getRoomRef());
            }
        }

    }

    public static List<Interval> disconnectedIntervals(List<Interval> intervals, Interval r) {
        List<Interval> result = new ArrayList<>();

        ArrayList<Marker2> markers = new ArrayList<>();

        for (Interval i : intervals) {
            markers.add(new Marker2(i.getStartMillis(), true));
            markers.add(new Marker2(i.getEndMillis(), false));
        }

        Collections.sort(markers, (a, b) -> Long.compare(a.val, b.val));


        int overlap = 0;
        boolean endReached = false;

        if (markers.get(0).val > r.getStartMillis()) {
            result.add(new Interval(r.getStartMillis(), markers.get(0).val));
        }

        for (int i = 0; i < markers.size() - 1; i++) {
            Marker2 m = markers.get(i);

            overlap += m.start ? 1 : -1;
            Marker2 next = markers.get(i + 1);

            if (m.val != next.val && overlap == 0 && next.val > r.getStartMillis()) {
                long start = m.val > r.getStartMillis() ? m.val : r.getStartMillis();
                long end = next.val;
                if (next.val > r.getEndMillis()) {
                    end = r.getEndMillis();
                    endReached = true;
                }
                if (start != end) {
                    result.add(new Interval(start, end));
                }
                if (endReached)
                    break;
            }
        }

        if (!endReached) {
            Marker2 m = markers.get(markers.size() - 1);
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
