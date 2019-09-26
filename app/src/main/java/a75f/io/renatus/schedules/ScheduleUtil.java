package a75f.io.renatus.schedules;

import android.util.Log;

import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;

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
    
        Iterator daysIterator = s.getDays().iterator();
        while(daysIterator.hasNext()) {
            Schedule.Days d = (Schedule.Days) daysIterator.next();
            Interval i = s.getScheduledInterval(d);
        
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
    
        Log.d("CCU_UI "," Trimmed Zone Schedule"+s.toString());
        CCUHsApi.getInstance().updateZoneSchedule(s, s.getRoomRef());
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
