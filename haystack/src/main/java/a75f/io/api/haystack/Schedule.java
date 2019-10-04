package a75f.io.api.haystack;

import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

import a75f.io.logger.CcuLog;

/***
 * Supports Schedule to HDict
 * HDict to Schedule
 *
 * Raw creation requires id field is set to  String localId = UUID.randomUUID().toString();
 *
 */
public class Schedule extends Entity
{
    public boolean isBuildingSchedule()
    {
        return getMarkers().contains("building");
    }

    public boolean isZoneSchedule()
    {
        return getMarkers().contains("zone");
    }

    public boolean isNamedSchedule()
    {
        return getMarkers().contains("named");
    }


    public static String getZoneIdByEquipId(String equipId)
    {
        HashMap equipHashMap = CCUHsApi.getInstance().readMapById(equipId);
        Equip   equip        = new Equip.Builder().setHashMap(equipHashMap).build();
        return equip.getRoomRef();
    }

    public static Schedule getScheduleByEquipId(String equipId)
    {
        HashMap equipHashMap = CCUHsApi.getInstance().readMapById(equipId);
        Equip   equip        = new Equip.Builder().setHashMap(equipHashMap).build();

        return getScheduleForZone(equip.getRoomRef().replace("@", ""), false);
    }

    public static Schedule getVacationByEquipId(String equipId)
    {
        HashMap equipHashMap = CCUHsApi.getInstance().readMapById(equipId);
        Equip   equip        = new Equip.Builder().setHashMap(equipHashMap).build();

        return getScheduleForZone(equip.getRoomRef().replace("@", ""), true);

    }

    public static Schedule getScheduleForZone(String zoneId, boolean vacation)
    {
        
        HashMap zoneHashMap = CCUHsApi.getInstance().readMapById(zoneId);

        Zone build = new Zone.Builder().setHashMap(zoneHashMap).build();

        String ref = null;
        if (vacation)
            ref = build.getVacationRef();
        else
            ref = build.getScheduleRef();

        if (ref != null && !ref.equals(""))
        {
            Schedule schedule = CCUHsApi.getInstance().getScheduleById(ref);
            
            if (schedule != null && (!schedule.mMarkers.contains("disabled") || vacation))
            {
                CcuLog.d("Schedule", "Zone Schedule: for "+build.getDisplayName()+" : "+ schedule.toString());
                return schedule;
            }
        }
    
        CcuLog.d("Schedule", " Zone Schedule disabled:  get Building Schedule");
        ArrayList<Schedule> retVal = CCUHsApi.getInstance().getSystemSchedule(vacation);
        if (retVal != null && retVal.size() > 0) {
            CcuLog.d("Schedule", "Building Schedule:  "+retVal.get(0).toString());
            return retVal.get(0);
    
        }
        
        return null;
    }

    public static Zone getZoneforEquipId(String equipId)
    {
        HashMap equipHashMap = CCUHsApi.getInstance().readMapById(equipId);
        Equip   equip        = new Equip.Builder().setHashMap(equipHashMap).build();
        HashMap zoneHashMap  = CCUHsApi.getInstance().readMapById(equip.getRoomRef().replace("@", ""));

        Zone build = new Zone.Builder().setHashMap(zoneHashMap).build();

        return build;
    }


    public static Schedule disableScheduleForZone(String zoneId, boolean enabled)
    {
        HashMap zoneHashMap       = CCUHsApi.getInstance().readMapById(zoneId);
        Zone    build             = new Zone.Builder().setHashMap(zoneHashMap).build();
        boolean currentlyDisabled = build.getMarkers().contains("disabled");

        return null;
    }


    public boolean checkIntersection(ArrayList<Days> daysArrayList)
    {

        ArrayList<Interval> intervalsOfAdditions = getScheduledIntervals(daysArrayList);
        ArrayList<Interval> intervalsOfCurrent   = getScheduledIntervals(getDaysSorted());

        for (Interval additions : intervalsOfAdditions)
        {
            for (Interval current : intervalsOfCurrent)
            {
                boolean hasOverlap = additions.overlaps(current);
                if (hasOverlap)
                {
                    Log.d("CCU_UI"," hasOverlap "+" additions "+additions+" current "+current);
                    return true;
                }
                //If current day is monday , it could conflict with next week's multi-day sunday schedule.
                if (current.getStart().getDayOfWeek() == 7 && additions.getStart().getDayOfWeek() == 1) {
                    DateTime start = new DateTime(additions.getStartMillis()+ 7*24*60*60*1000);
                    DateTime end = new DateTime(additions.getEndMillis()+ 7*24*60*60*1000);
                    additions = new Interval(start, end);
                    hasOverlap = current.overlaps(additions);
                    if (hasOverlap) {
                        return true;
                    }
                } else if (current.getStart().getDayOfWeek() == 1 && additions.getStart().getDayOfWeek() == 7) {
                    DateTime start = new DateTime(current.getStartMillis()+ 7*24*60*60*1000);
                    DateTime end = new DateTime(current.getEndMillis()+ 7*24*60*60*1000);
                    current = new Interval(start, end);
                    hasOverlap = additions.overlaps(current);
                    if (hasOverlap) {
                        return true;
                    }
                }
                
            }
            
        }

        return false;
    }
    
    public ArrayList<Interval> getOverLapInterval(Days day) {
        ArrayList<Interval> intervalsOfCurrent   = getScheduledIntervals(getDaysSorted());
        Interval intervalOfAddition = getScheduledInterval(day);
        ArrayList<Interval> overLaps = new ArrayList<>();
        for (Interval current : intervalsOfCurrent)
        {
            boolean hasOverlap = intervalOfAddition.overlaps(current);
            if (hasOverlap)
            {
                Log.d("CCU_UI"," Current "+current+" new "+intervalOfAddition+" overlaps "+hasOverlap);
                if (current.getStart().minuteOfDay().get() < current.getEnd().minuteOfDay().get())
                {
                    overLaps.add(current.overlap(intervalOfAddition));
                } else {
                    //Multi-day schedule
                    if (intervalOfAddition.getEndMillis() > current.getStartMillis() &&
                                                intervalOfAddition.getStartMillis() < current.getStartMillis())
                    {
                        overLaps.add(new Interval(current.getStartMillis(), intervalOfAddition.getEndMillis()));
                    }
                    else if (intervalOfAddition.getStartMillis() < current.getEndMillis())
                    {
                        overLaps.add(new Interval(intervalOfAddition.getStartMillis(), current.getEndMillis()));
                    }
                }
            }
    
            //If current day is monday , it could conflict with next week's multi-day sunday schedule.
            if (current.getStart().getDayOfWeek() == 7
                && intervalOfAddition.getStart().getDayOfWeek() == 1) {
                DateTime start = new DateTime(intervalOfAddition.getStartMillis()+ 7*24*60*60*1000);
                DateTime end = new DateTime(intervalOfAddition.getEndMillis()+ 7*24*60*60*1000);
                Interval addition = new Interval(start, end);
                hasOverlap = current.overlaps(addition);
                if (hasOverlap) {
                    overLaps.add(current.overlap(addition));
                }
            }else if (current.getStart().getDayOfWeek() == 1 && intervalOfAddition.getStart().getDayOfWeek() == 7) {
                DateTime start = new DateTime(current.getStartMillis()+ 7*24*60*60*1000);
                DateTime end = new DateTime(current.getEndMillis()+ 7*24*60*60*1000);
                current = new Interval(start, end);
                hasOverlap = current.overlaps(intervalOfAddition);
                if (hasOverlap) {
                    overLaps.add(current.overlap(intervalOfAddition));
                }
            }
        }
        return overLaps;
    }
    
    public ArrayList<Interval> getMergedIntervals() {
        return getMergedIntervals(getDaysSorted());
    }
    public ArrayList<Interval> getMergedIntervals(ArrayList<Days> daysSorted) {
    
        ArrayList<Interval> intervals   = getScheduledIntervalsForDays(daysSorted);
        Collections.sort(intervals, new Comparator<Interval>() {
                    public int compare(Interval p1, Interval p2) {
                        return Long.compare(p1.getStartMillis(), p2.getStartMillis());
                    }
                }
                        );
        
        Stack<Interval> stack=new Stack<>();
        if (intervals.size() > 0)
        {
            stack.push(intervals.get(0));
            for (int i = 1; i < intervals.size(); i++)
            {
                Interval top = stack.peek();
                Interval interval = intervals.get(i);
                if (top.getEndMillis() < interval.getStartMillis())
                {
                    stack.push(interval);
                }
                else if (top.getEndMillis() < interval.getEndMillis())
                {
                    Interval t = new Interval(top.getStartMillis(), interval.getEndMillis());
                    stack.pop();
                    stack.push(t);
                }
            }
        }
        return new ArrayList<>(stack);
        
    }
    
    private DateTime getTime()
    {
        return new DateTime(MockTime.getInstance().getMockTime());
    }

    public Occupied getCurrentValues()
    {
        Occupied            occupied           = null;
        ArrayList<Days>     daysSorted         = getDaysSorted();
        ArrayList<Interval> scheduledIntervals = getScheduledIntervals(daysSorted);

        for (int i = 0; i < daysSorted.size(); i++)
        {
            if (scheduledIntervals.get(i).contains(getTime().getMillis()) || scheduledIntervals.get(i).isAfter(getTime().getMillis()))
            {

                boolean currentlyOccupied = scheduledIntervals.get(i).contains(getTime().getMillis());
                occupied = new Occupied();
                occupied.setOccupied(currentlyOccupied);
                occupied.setValue(daysSorted.get(i).mVal);
                occupied.setCoolingVal(daysSorted.get(i).mCoolingVal);
                occupied.setHeatingVal(daysSorted.get(i).mHeatingVal);

                if (currentlyOccupied)
                {
                    occupied.setCurrentlyOccupiedSchedule(daysSorted.get(i));
                } else
                {
                    occupied.setNextOccupiedSchedule(daysSorted.get(i));
                }

                DateTime startDateTime = new DateTime(MockTime.getInstance().getMockTime())
                        .withHourOfDay(daysSorted.get(i).getSthh())
                        .withMinuteOfHour(daysSorted.get(i).getStmm())
                        .withDayOfWeek(daysSorted.get(i).getDay() + 1)
                        .withSecondOfMinute(0);
                occupied.setMillisecondsUntilNextChange(startDateTime.getMillis() - MockTime.getInstance().getMockTime());

                return occupied;
            }
        }


        /* In case it runs off the ends of the schedule */
        if (daysSorted.size() > 0)
        {
            occupied = new Occupied();
            occupied.setOccupied(false);
            occupied.setValue(daysSorted.get(0).mVal);
            occupied.setCoolingVal(daysSorted.get(0).mCoolingVal);
            occupied.setHeatingVal(daysSorted.get(0).mHeatingVal);
            occupied.setNextOccupiedSchedule(daysSorted.get(0));
            DateTime startDateTime = new DateTime(MockTime.getInstance().getMockTime())
                    .withHourOfDay(daysSorted.get(0).getSthh())
                    .withMinuteOfHour(daysSorted.get(0).getStmm())
                    .withDayOfWeek(daysSorted.get(0).getDay() + 1)
                    .withSecondOfMinute(0);
            occupied.setMillisecondsUntilNextChange(startDateTime.getMillis() - MockTime.getInstance().getMockTime());
        }

        return occupied;
    }


    /*{stdt:2018-12-18T10:13:55.185-06:00 Chicago
        dis:"Simple Schedule" etdt:2018-12-18T10:13:55.185-06:00 Chicago
        vacation
        temp
        kind:"Number"
        schedule unit:"\\u00B0F"
        days:[{cooling
        days:[
                {ethh:16 sthh:13 day:0.0 val:68},{ethh:16 sthh:9 day:1.0 etmm:12 val:80 stmm:0.0},
                {sunrise:T day:1.0 val:80 sunset:T},{sunrise:F day:3 val:80 sunset:F},
                {sunrise:T day:3 val:80 sunset:T}]},
                {heating
                    days:
                    [{ethh:16 sthh:13 day:0.0 val:68},
                    {ethh:16 sthh:9 day:1.0 etmm:12 val:80 stmm:0.0},
                    {sunrise:T day:1.0 val:80 sunset:T},{sunrise:F day:3 val:80 sunset:F},{sunrise:T day:3 val:80 sunset:T}]}]}*/

    private String          mId;
    private boolean         mIsVacation;
    private String          mDis;
    private HashSet<String> mMarkers;
    private String          mKind;
    private String          mUnit;
    private ArrayList<Days> mDays = new ArrayList<Days>();

    private DateTime mStartDate;
    private DateTime mEndDate;
    
    public String getRoomRef()
    {
        return mRoomRef;
    }
    public void setRoomRef(String roomRef)
    {
        this.mRoomRef = roomRef;
    }
    private String mRoomRef;


    public String getmSiteId()
    {
        return mSiteId;
    }
    
    public void setmSiteId(String mSiteId)
    {
        this.mSiteId = mSiteId;
    }
    private String mSiteId;

    public String getTZ()
    {
        return mTZ;
    }

    private String mTZ;

    public String getId()
    {
        return mId;
    }
    
    public void setId(String mId) {
        this.mId = mId;
    }

    public boolean isVacation()
    {
        return mIsVacation;
    }

    public String getDis()
    {
        return mDis;
    }

    public HashSet<String> getMarkers()
    {
        return mMarkers;
    }

    public String getKind()
    {
        return mKind;
    }

    public String getUnit()
    {
        return mUnit;
    }

    public ArrayList<Days> getDays()
    {
        Collections.sort(mDays, new Comparator<Days>()
        {
            @Override
            public int compare(Days d1, Days d2)
            {
                return d1.mDay - d2.mDay;
            }
        });
        return mDays;
    }
    
    public Days getDay(Days day)
    {
        for (Days d : mDays) {
            if (d.mDay == day.mDay) {
                return d;
            }
        }
        return null;
    }
    
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(mDis).append(" ");
        if (isVacation()) {
            b.append(mStartDate.toString()+"-"+mEndDate.toString());
        }else
        {
            for (Days d : mDays)
            {
                b.append(d.toString());
            }
        }
        for (String m :mMarkers ) {
            b.append(m+" ");
        }
        
        return b.toString();
    }
    
    //Get existing intervals for selected days
    public ArrayList<Interval> getScheduledIntervalsForDays(ArrayList<Days> daysSorted) {
        ArrayList<Interval> daysIntervals = new ArrayList<Interval>();
        ArrayList<Interval> allIntervals = getScheduledIntervals(getDaysSorted());
        for (Interval i : allIntervals)
        {
            for (Days d : daysSorted)
            {
                if (d.mDay == i.getStart().getDayOfWeek()-1) {
                    daysIntervals.add(i);
                } else if (d.mDay == i.getEnd().getDayOfWeek()-1) {
                    long now = MockTime.getInstance().getMockTime();
                    DateTime startTime = new DateTime(now)
                                                   .withHourOfDay(0)
                                                   .withMinuteOfHour(0)
                                                   .withSecondOfMinute(0).withMillisOfSecond(0).withDayOfWeek(i.getEnd().getDayOfWeek());
                    if (d.mDay == DAYS.MONDAY.ordinal()) {
                        DateTime endTime = new DateTime(now).withHourOfDay(i.getEnd().getHourOfDay())
                                                            .withMinuteOfHour(i.getEnd().getMinuteOfHour())
                                                            .withSecondOfMinute(i.getEnd().getSecondOfMinute())
                                                            .withMillisOfSecond(i.getEnd().getMillisOfSecond()).withDayOfWeek(d.mDay+1);
                        daysIntervals.add(i.toInterval().withStartMillis(startTime.getMillis()).withEndMillis(endTime.getMillis()));
                    }else
                    {
                        daysIntervals.add(i.toInterval().withStartMillis(startTime.getMillis()));
                    }
                }else if (d.mDay == DAYS.SUNDAY.ordinal() &&((d.getSthh()*60 + d.getStmm()) > (d.getEthh()*60+d.getEtmm()))
                                                    && i.getStart().getDayOfWeek() == 1) {
                    if (i.getEnd().getMinuteOfDay() < (d.getEthh()*60+d.getEtmm()))
                    {
                        daysIntervals.add(i);
                    }else if (i.getStart().getMinuteOfDay() < (d.getEthh()*60+d.getEtmm()) ){
                        DateTime endTime = i.getEnd().withHourOfDay(d.getEthh()).withMinuteOfHour(d.getEtmm());
                        daysIntervals.add(i.toInterval().withEndMillis(endTime.getMillis()));
                    }
                }
                
                
            }
        }
        for(Interval i : daysIntervals) {
            Log.d("CCU_UI", "Scheduled interval for days"+i);
        }
        return daysIntervals;
    }
    
    public ArrayList<Interval> getScheduledIntervals() {
        return getScheduledIntervals(getDaysSorted());
    }
    
    public ArrayList<Interval> getScheduledIntervals(ArrayList<Days> daysSorted)
    {
        ArrayList<Interval> intervals = new ArrayList<Interval>();

        for (Days day : daysSorted)
        {

            long now = MockTime.getInstance().getMockTime();

            DateTime startDateTime = new DateTime(now)
                    .withHourOfDay(day.getSthh())
                    .withMinuteOfHour(day.getStmm())
                    .withDayOfWeek(day.getDay() + 1)
                    .withSecondOfMinute(0)
                    .withMillisOfSecond(0);
            DateTime endDateTime = new DateTime(now)
                    .withHourOfDay(day.getEthh())
                    .withMinuteOfHour(day.getEtmm())
                    .withSecondOfMinute(0).withMillisOfSecond(0).withDayOfWeek(
                            day.getDay() +
                                    1);
            Interval scheduledInterval = null;
            if (startDateTime.isAfter(endDateTime))
            {
                if (day.getDay() == DAYS.SUNDAY.ordinal()) {
                    scheduledInterval = new Interval(startDateTime, endDateTime.withWeekOfWeekyear(startDateTime.getWeekOfWeekyear()+1)
                                                                               .withDayOfWeek(DAYS.values()[day.getDay()].getNextDay().ordinal() + 1));
                } else {
                    scheduledInterval = new Interval(startDateTime, endDateTime.withDayOfWeek(DAYS.values()[day.getDay()].getNextDay().ordinal() + 1));
                }
            } else
            {
                scheduledInterval =
                        new Interval(startDateTime, endDateTime);
            }


            intervals.add(scheduledInterval);
        }

        return intervals;
    }
    
    public Interval getScheduledInterval(Days day)
    {
        
        long now = MockTime.getInstance().getMockTime();
        
        DateTime startDateTime = new DateTime(now)
                                         .withHourOfDay(day.getSthh())
                                         .withMinuteOfHour(day.getStmm())
                                         .withDayOfWeek(day.getDay() + 1)
                                         .withSecondOfMinute(0)
                                         .withMillisOfSecond(0);
        DateTime endDateTime = new DateTime(now)
                                       .withHourOfDay(day.getEthh())
                                       .withMinuteOfHour(day.getEtmm())
                                       .withSecondOfMinute(0).withMillisOfSecond(0).withDayOfWeek(
                        day.getDay() +
                        1);
        
        if (startDateTime.isAfter(endDateTime))
        {
            if (day.getDay() == DAYS.SUNDAY.ordinal()) {
                return new Interval(startDateTime, endDateTime.withWeekOfWeekyear(startDateTime.getWeekOfWeekyear()+1)
                                                                           .withDayOfWeek(DAYS.values()[day.getDay()].getNextDay().ordinal() + 1));
            } else {
                return new Interval(startDateTime, endDateTime.withDayOfWeek(DAYS.values()[day.getDay()].getNextDay().ordinal() + 1));
            }
        } else
        {
            return new Interval(startDateTime, endDateTime);
        }
        
    }
    
    /**
     * Sorts the days by MM, then by HH, then by DD
     *
     * @return Sorted list of days
     */
    public ArrayList<Days> getDaysSorted()
    {

        Collections.sort(mDays, new Comparator<Days>()
        {
            @Override
            public int compare(Days o1, Days o2)
            {
                return Integer.valueOf(o1.getStmm()).compareTo(Integer.valueOf(o2.getStmm()));
            }
        });

        Collections.sort(mDays, new Comparator<Days>()
        {
            @Override
            public int compare(Days o1, Days o2)
            {
                return Integer.valueOf(o1.getSthh()).compareTo(Integer.valueOf(o2.getSthh()));
            }
        });

        Collections.sort(mDays, new Comparator<Days>()
        {
            @Override
            public int compare(Days o1, Days o2)
            {
                return Integer.valueOf(o1.mDay).compareTo(Integer.valueOf(o2.mDay));
            }
        });

        return mDays;
    }

    public void populateIntersections()
    {
        ArrayList<Interval> scheduledIntervals = getScheduledIntervals(getDays());

        for (int i = 0; i < scheduledIntervals.size(); i++)
        {
            for (int ii = 0; ii < scheduledIntervals.size(); ii++)
            {
                if (scheduledIntervals.get(i).getEndMillis() == scheduledIntervals.get(ii).getStartMillis())
                {
                    this.mDays.get(i).setIntersection(true);
                }else if((scheduledIntervals.get(i).getEnd().getDayOfWeek() == scheduledIntervals.get(ii).getStart().getDayOfWeek())
                        && (scheduledIntervals.get(i).getEnd().getHourOfDay() == scheduledIntervals.get(ii).getStart().getHourOfDay())
                        && (scheduledIntervals.get(i).getEnd().getMinuteOfHour() == scheduledIntervals.get(ii).getStart().getMinuteOfHour())){
                    //Multi day schedule intersection check
                    this.mDays.get(i).setIntersection(true);
                }
            }
        }

    }

    public String getEndDateString()
    {
        return mEndDate != null ? mEndDate.toString("y-M-d") : "No end date";
    }

    public String getStartDateString()
    {
        return mStartDate != null ? mStartDate.toString("y-M-d") : "No start date";
    }

    public DateTime getStartDate()
    {
        return mStartDate;
    }

    public DateTime getEndDate()
    {
        return mEndDate;
    }

    public boolean isActiveVacation()
    {
        Log.d("CCU_JOB","isActiveVacation  vacStart "+getStartDate().getMillis()+" vacEnd "+getEndDate().getMillis()+" Curr "+MockTime.getInstance().getMockTime());
        Interval interval = new Interval(getStartDate(), getEndDate());
        return interval.contains(MockTime.getInstance().getMockTime());
    }

    public void setDisabled(boolean disabled)
    {

        if (disabled) mMarkers.add("disabled");
        else mMarkers.remove("disabled");
    }
    
    public void setDaysCoolVal(double val, boolean alldays)
    {
        if (alldays) {
            for (Days d : mDays) {
                d.mCoolingVal = val;
            }
        } else
        {
            int day = DateTime.now().dayOfWeek().get() - 1;
            for (Days d : mDays) {
                if (d.mDay == day)
                {
                    d.mCoolingVal = val;
                    Log.d("CCU_JOB", " Set mCoolingVal : "+val+" day "+day);
                    break;
                }
            }
            
        }
    }
    
    public void setDaysHeatVal(double val, boolean alldays)
    {
        if (alldays) {
            for (Days d : mDays) {
                d.mHeatingVal = val;
            }
        } else
        {
            int day = DateTime.now().dayOfWeek().get() - 1;
            for (Days d : mDays) {
                if (d.mDay == day)
                {
                    d.mHeatingVal = val;
                    Log.d("CCU_JOB", " Set mHeatingVal : "+val+" day "+day);
                    break;
                }
            }
        }
    }


    public static class Builder
    {
        private String mId;

        private boolean         mIsVacation;
        private String          mDis;
        private HashSet<String> mMarkers = new HashSet<String>();
        private String          mKind;
        private String          mUnit;
        private ArrayList<Days> mDays    = new ArrayList<Days>();
        private String          mTZ;
        private String          mSiteId;
        private DateTime        mStartDate;
        private DateTime        mEndDate;
        private String mRoomRef;
    
    
        public Schedule.Builder setmRoomRef(String mRoomRef)
        {
            this.mRoomRef = mRoomRef;
            return this;
        }
        
        public Schedule.Builder setId(String id)
        {
            this.mId = id;
            return this;
        }

        public Schedule.Builder setVacation(boolean vacation)
        {
            this.mIsVacation = vacation;
            return this;
        }

        public Schedule.Builder setDisplayName(String displayName)
        {
            this.mDis = displayName;
            return this;
        }

        public Schedule.Builder setMarkers(HashSet<String> markers)
        {
            this.mMarkers = markers;
            return this;
        }

        public Schedule.Builder addMarker(String marker)
        {
            this.mMarkers.add(marker);
            return this;
        }

        public Schedule.Builder setKind(String kind)
        {
            this.mKind = kind;
            return this;
        }

        //TODO make unit enum / strings
        public Schedule.Builder setUnit(String unit)
        {
            this.mUnit = unit;
            return this;
        }

        public Schedule.Builder setDays(ArrayList<Days> days)
        {
            this.mDays = days;
            return this;
        }

        public Schedule.Builder setTz(String tz)
        {
            this.mTZ = tz;
            return this;
        }

        public Schedule build()
        {
            Schedule s = new Schedule();
            s.mId = this.mId;
            s.mMarkers = this.mMarkers;
            s.mIsVacation = this.mIsVacation;
            s.mDis = this.mDis;
            s.mKind = this.mKind;
            s.mSiteId = this.mSiteId;
            s.mUnit = this.mUnit;
            s.mDays = this.mDays;
            s.mTZ = this.mTZ;
            s.mStartDate = this.mStartDate;
            s.mEndDate = this.mEndDate;
            s.mRoomRef = this.mRoomRef;
            return s;
        }


        public Schedule.Builder setHDict(HDict schedule)
        {

            Iterator it = schedule.iterator();
            while (it.hasNext())
            {
                Map.Entry pair = (Map.Entry) it.next();
                //System.out.println(pair.getKey() + " = " + pair.getValue());
                if (pair.getKey().equals("id"))
                {
                    this.mId = pair.getValue().toString().replaceAll("@", "");
                } else if (pair.getKey().equals("dis"))
                {
                    this.mDis = pair.getValue().toString();
                } else if (pair.getKey().equals("vacation"))
                {
                    this.mIsVacation = true;
                } else if (pair.getKey().equals("kind"))
                {
                    this.mKind = pair.getValue().toString();
                } else if (pair.getKey().equals("unit"))
                {
                    this.mUnit = pair.getValue().toString();
                } else if (pair.getKey().equals("tz"))
                {
                    this.mTZ = pair.getValue().toString();
                } else if (pair.getKey().equals("days"))
                {
                    this.mDays = Days.parse((HList) pair.getValue());
                } else if (pair.getKey().equals("siteRef"))
                {
                    this.mSiteId = schedule.getRef("siteRef").val;
                } else if (pair.getKey().equals("roomRef"))
                {
                    this.mRoomRef = schedule.getRef("roomRef").toString();
                }
                else if(pair.getKey().equals("range"))
                {
                    HDict range = (HDict) schedule.get("range");
                    this.mStartDate = new DateTime((HDateTime.make(range.get("stdt").toString()).millisDefaultTZ()));
                    this.mEndDate = new DateTime((HDateTime.make(range.get("etdt").toString()).millisDefaultTZ()));
                    
                }
                else if (pair.getKey().equals("stdt"))
                {
                    this.mStartDate = new DateTime(((HDateTime) schedule.get("stdt")).millisDefaultTZ());
                } else if (pair.getKey().equals("etdt"))
                {
                    this.mEndDate = new DateTime(((HDateTime) schedule.get("etdt")).millisDefaultTZ());
                } else
                {
                    this.mMarkers.add(pair.getKey().toString());
                }
            }
            return this;
        }
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Schedule s = (Schedule) o;
        if (this.mDays.size() !=  s.mDays.size()) return false;
        
        for(Days day : this.mDays) {
            if (s.getDay(day).equals(day)) return false;
        }
        
        if (!this.getMarkers().equals(s.getMarkers())) return false;
        
        return true;
    }

    public static class Days
    {


        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Days days = (Days) o;
            return mSthh == days.mSthh &&
                    mStmm == days.mStmm &&
                    mDay == days.mDay &&
                    mEtmm == days.mEtmm &&
                    mEthh == days.mEthh &&
                    mSunrise == days.mSunrise &&
                    mSunset == days.mSunset &&
                    Objects.equals(mVal, days.mVal) &&
                    Objects.equals(mHeatingVal, days.mHeatingVal) &&
                    Objects.equals(mCoolingVal, days.mCoolingVal);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(mSthh, mStmm, mDay, mVal, mHeatingVal, mCoolingVal, mEtmm, mEthh, mSunrise, mSunset);
        }


        private boolean mIntersection;
        private int     mSthh;
        private int     mStmm;
        private int     mDay;
        private Double  mVal;
        private Double  mHeatingVal;
        private Double  mCoolingVal;
        private int     mEtmm;
        private int     mEthh;
        private boolean mSunrise;
        private boolean mSunset;


        public int getSthh()
        {
            return mSthh;
        }

        public void setSthh(int sthh)
        {
            this.mSthh = sthh;
        }

        public int getStmm()
        {
            return mStmm;
        }

        public void setStmm(int stmm)
        {
            this.mStmm = stmm;
        }

        public int getDay()
        {
            return mDay;
        }

        public void setDay(int day)
        {
            this.mDay = day;
        }

        public Double getVal()
        {
            return mVal;
        }

        public void setVal(Double val)
        {
            this.mVal = val;
        }

        public int getEtmm()
        {
            return mEtmm;
        }

        public void setEtmm(int etmm)
        {
            this.mEtmm = etmm;
        }

        public int getEthh()
        {
            return mEthh;
        }

        public void setEthh(int ethh)
        {
            this.mEthh = ethh;
        }

        public boolean isSunrise()
        {
            return mSunrise;
        }

        public void setSunrise(boolean sunrise)
        {
            this.mSunrise = sunrise;
        }

        public boolean isSunset()
        {
            return mSunset;
        }

        public void setSunset(boolean sunset)
        {
            this.mSunset = sunset;
        }
        
        public static ArrayList<Days> parse(HList value)
        {
            ArrayList<Days> days = new ArrayList<Days>();
            for (int i = 0; i < value.size(); i++)
            {
                days.add(parseSingleDay((HDict) value.get(i)));
            }

            return days;
        }

        private static Days parseSingleDay(HDict hDict)
        {
            Days days = new Days();

            days.mDay = hDict.getInt("day");
            days.mEthh = hDict.has("ethh") ? hDict.getInt("ethh") : -1;
            days.mEtmm = hDict.has("etmm") ? hDict.getInt("etmm") : -1;
            days.mSthh = hDict.has("sthh") ? hDict.getInt("sthh") : -1;
            days.mStmm = hDict.has("stmm") ? hDict.getInt("stmm") : -1;
            days.mSunrise = hDict.has("sunrise");
            days.mSunset = hDict.has("sunset");

            days.mVal = hDict.has("curVal") ? hDict.getDouble("curVal") : null;
            days.mCoolingVal = hDict.has("coolVal") ? hDict.getDouble("coolVal") : null;
            days.mHeatingVal = hDict.has("heatVal") ? hDict.getDouble("heatVal") : null;

            return days;
        }

        public Double getHeatingVal()
        {
            return mHeatingVal;
        }

        public void setHeatingVal(Double heatingVal)
        {
            this.mHeatingVal = heatingVal;
        }

        public Double getCoolingVal()
        {
            return mCoolingVal;
        }

        public void setCoolingVal(Double coolingVal)
        {
            this.mCoolingVal = coolingVal;
        }


        public boolean isIntersection()
        {
            return mIntersection;
        }

        public void setIntersection(boolean mIntersection)
        {
            this.mIntersection = mIntersection;
        }
        
        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append(" {mDay: "+mDay);
            str.append(" Time "+mSthh+":"+mStmm+" - "+mEthh+":"+mEtmm);
            str.append(" heatingVal "+mHeatingVal+" coolingVal "+mCoolingVal+"}");
            return str.toString();
        }
    }

    public HDict getScheduleHDict()
    {
        if (isVacation()) {
    
            //range,building,dis,vacation,id,heating,temp,siteRef,schedule,cooling
            //{stdt:2019-07-04T05:00:00Z Rel etdt:2019-07-14T04:59:59Z Rel},M,"vaca1",M,@5d0bd3a5f987526c76b06132 "vaca1",M,M,@5d0ba7e5d099b1630edee18e,M,M
            HDict hDict = new HDictBuilder()
                                  .add("stdt", HDateTime.make(mStartDate.getMillis()))
                                  .add("etdt", HDateTime.make(mEndDate.getMillis())).toDict();
            
            HDict vacationSchedule = new HDictBuilder()
                                            .add("id", HRef.copy(getId()))
                                            .add("temp")
                                            .add("schedule")
                                            .add("building")
                                            .add("vacation")
                                            .add("cooling")
                                            .add("heating")
                                            .add("range", hDict)
                                            .add("dis", getDis())
                                            .add("siteRef", HRef.copy(mSiteId))
                                            .toDict();
            return vacationSchedule;
        }
        
        HDict[] days = new HDict[getDays().size()];

        for (int i = 0; i < getDays().size(); i++)
        {
            Days day = mDays.get(i);
            HDictBuilder hDictDay = new HDictBuilder()
                    .add("day", HNum.make(day.mDay))
                    .add("sthh", HNum.make(day.mSthh))
                    .add("stmm", HNum.make(day.mStmm))
                    .add("ethh", HNum.make(day.mEthh))
                    .add("etmm", HNum.make(day.mEtmm));
            if (day.mHeatingVal != null)
                hDictDay.add("heatVal", HNum.make(day.getHeatingVal()));
            if (day.mCoolingVal != null)
                hDictDay.add("coolVal", HNum.make(day.getCoolingVal()));
            if (day.mVal != null)
                hDictDay.add("curVal", HNum.make(day.getVal()));

            //need boolean & string support
            if (day.mSunset) hDictDay.add("sunset", day.mSunset);
            if (day.mSunrise) hDictDay.add("sunrise", day.mSunrise);

            days[i] = hDictDay.toDict();
        }

        HList hList = HList.make(days);
        HDictBuilder defaultSchedule = new HDictBuilder()
                .add("id", HRef.copy(getId()))
                .add("unit", getUnit())
                .add("kind", getKind())
                .add("dis", "Building Schedule")
                .add("days", hList)
                .add("siteRef", HRef.copy(mSiteId));
        
        for (String marker : getMarkers())
        {
            defaultSchedule.add(marker);
        }

        return defaultSchedule.toDict();
    }
    
    public HDict getZoneScheduleHDict(String roomRef)
    {
        if (isVacation()) {
            //range,building,dis,vacation,id,heating,temp,siteRef,schedule,cooling
            //{stdt:2019-07-04T05:00:00Z Rel etdt:2019-07-14T04:59:59Z Rel},M,"vaca1",M,@5d0bd3a5f987526c76b06132 "vaca1",M,M,@5d0ba7e5d099b1630edee18e,M,M
            HDict hDict = new HDictBuilder()
                                  .add("stdt", HDateTime.make(mStartDate.getMillis()))
                                  .add("etdt", HDateTime.make(mEndDate.getMillis())).toDict();
        
            HDict vacationSchedule = new HDictBuilder()
                                             .add("id", HRef.copy(getId()))
                                             .add("temp")
                                             .add("schedule")
                                             .add("zone")
                                             .add("vacation")
                                             .add("cooling")
                                             .add("heating")
                                             .add("range", hDict)
                                             .add("dis", getDis())
                                             .add("siteRef", HRef.copy(mSiteId))
                                             .add("roomRef", HRef.copy(roomRef))
                                             .toDict();
            return vacationSchedule;
        }
        
        HDict[] days = new HDict[getDays().size()];
        
        for (int i = 0; i < getDays().size(); i++)
        {
            Days day = mDays.get(i);
            HDictBuilder hDictDay = new HDictBuilder()
                                            .add("day", HNum.make(day.mDay))
                                            .add("sthh", HNum.make(day.mSthh))
                                            .add("stmm", HNum.make(day.mStmm))
                                            .add("ethh", HNum.make(day.mEthh))
                                            .add("etmm", HNum.make(day.mEtmm));
            if (day.mHeatingVal != null)
                hDictDay.add("heatVal", HNum.make(day.getHeatingVal()));
            if (day.mCoolingVal != null)
                hDictDay.add("coolVal", HNum.make(day.getCoolingVal()));
            if (day.mVal != null)
                hDictDay.add("curVal", HNum.make(day.getVal()));
            
            //need boolean & string support
            if (day.mSunset) hDictDay.add("sunset", day.mSunset);
            if (day.mSunrise) hDictDay.add("sunrise", day.mSunrise);
            
            days[i] = hDictDay.toDict();
        }
        
        HList hList = HList.make(days);
        HDictBuilder defaultSchedule = new HDictBuilder()
                                               .add("id", HRef.copy(getId()))
                                               .add("unit", getUnit())
                                               .add("kind", getKind())
                                               .add("dis", "Zone Schedule")
                                               .add("days", hList)
                                               .add("roomRef",HRef.copy(roomRef))
                                               .add("siteRef", HRef.copy(mSiteId));
        
        for (String marker : getMarkers())
        {
            defaultSchedule.add(marker);
        }
        
        return defaultSchedule.toDict();
    }

}
