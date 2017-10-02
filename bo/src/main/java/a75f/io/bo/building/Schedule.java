package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import a75f.io.bo.building.definitions.DAYS;
import a75f.io.bo.building.definitions.MockTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Schedule
{
    public static final String         TAG            = "Schedule";
    /*The value when scheduled parameters are active. */
    protected           ArrayList<Day> days           = new ArrayList<Day>();
    protected           boolean        mValidSchedule = false;
    @JsonIgnore
    ArrayList<Interval> mScheduledIntervals = new ArrayList<Interval>();
    
    
    public Schedule()
    {
    }
    
    
    public Schedule(ArrayList<Day> days)
    {
        this.days = days;
        sort();
    }
    
    
    private void sort()
    {
        Collections.sort(days, new Comparator<Day>()
        {
            @Override
            public int compare(Day o1, Day o2)
            {
                return Integer.valueOf(o1.getDay()).compareTo(Integer.valueOf(o2.getDay()));
            }
        });
    }
    
    
    @JsonIgnore
    public boolean isInSchedule()
    {
        return isInSchedule(MockTime.getInstance().getMockTime());
    }
    
    
    @JsonIgnore
    public boolean isInSchedule(long time)
    {
        if (isValidSchedule())
        {
            for (Interval interval : getScheduledIntervals())
            {
                if (interval.contains(time))
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    @JsonIgnore
    public boolean isValidSchedule()
    {
        if (mValidSchedule)
        {
            return true;
        }
        if (days != null && days.size() > 0)
        {
            mValidSchedule = true;
            return mValidSchedule;
        }
        return mValidSchedule = false;
    }
    
    
    @JsonIgnore
    public ArrayList<Interval> getScheduledIntervals()
    {
        if (!mScheduledIntervals.isEmpty())
        {
            return mScheduledIntervals;
        }
        else if (mScheduledIntervals.isEmpty() && days != null && days.size() > 0)
        {
            buildIntervals();
        }
        return mScheduledIntervals;
    }
    
    
    @JsonIgnore
    public void buildIntervals()
    {
        if (isValidSchedule())
        {
            try
            {
            /*
              * Sorts the specified array into ascending numerical order.
			 */
                //Add the scheduled intervals.
                for (Day day : days)
                {
                    DateTime startDateTime = new DateTime(MockTime.getInstance().getMockTime())
                                                     .withHourOfDay(day.getSthh())
                                                     .withMinuteOfHour(day.getStmm());
                    DateTime endDateTime = new DateTime(MockTime.getInstance().getMockTime())
                                                   .withHourOfDay(day.getEthh())
                                                   .withMinuteOfHour(day.getEtmm());
                    Interval scheduledInterval =
                            new Interval(startDateTime.withDayOfWeek(day.getDay() + 1), endDateTime
                                                                                                .withDayOfWeek(
                                                                                                        day.getDay() +
                                                                                                        1));
                    mScheduledIntervals.add(scheduledInterval);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    //
    
    
    /*****************
     *ONLY USED FOR CIRCUITS BELOW
     ******************/
    //TODO: need to implement next.
    @JsonIgnore
    public byte getScheduleDaysBitmap()
    {
        ArrayList<Day> daysAsArrayList = getDays();
        byte dayBytes = (byte) ((daysAsArrayList.contains(DAYS.MONDAY.ordinal()) ? 0x01 : 0x00) |
                                (daysAsArrayList.contains(DAYS.TUESDAY.ordinal()) ? 0x02 : 0x00) |
                                (daysAsArrayList.contains(DAYS.WEDNESDAY.ordinal()) ? 0x04 : 0x00) |
                                (daysAsArrayList.contains(DAYS.THURSDAY.ordinal()) ? 0x08 : 0x00) |
                                (daysAsArrayList.contains(DAYS.FRIDAY.ordinal()) ? 0x10 : 0x00) |
                                (daysAsArrayList.contains(DAYS.SATURDAY.ordinal()) ? 0x20 : 0x00) |
                                (daysAsArrayList.contains(DAYS.SUNDAY.ordinal()) ? 0x40 : 0x00) |
                                0x00);
        return dayBytes;
    }
    
    
    public ArrayList<Day> getDays()
    {
        return days;
    }
    
    
    public void setDays(ArrayList<Day> days)
    {
        this.days = days;
        sort();
    }
    
    
    @JsonIgnore
    public long getNextScheduleTransistionTime() throws Exception
    {
        if (!isValidSchedule())
        {
            throw new Exception("Schedule is invalid");
        }
        
		/* If intervals are not built */
        if (mScheduledIntervals.isEmpty() && days != null && days.size() > 0)
        {
            buildIntervals();
        }
        DateTime now = new DateTime(MockTime.getInstance().getMockTime());
        for (int i = 0; i < mScheduledIntervals.size(); i++)
        {
            Interval interval = mScheduledIntervals.get(i);
            //Before start of first
            if (now.isBefore(interval.getStart()))
            {
                return interval.getStartMillis();
            }
            else if (interval.contains(now))
            {
                return interval.getEndMillis();
            }
        }
        return -1;
    }
    
    
    /****
     *Takes in a time and checks to see if it crossed a boundry.
     * ot = override time.
     * i = in schedule
     * o = out of schedule
     * | i  | o  |  i  | o
     *   ot
     *   ct
     *   No Boundry crossed.
     * | i  | o  |  i  | o
     *   ot
     *       ct
     *   Boundry crossed
     * | i  | o  |  i  | o
     *        ot
     *   ct
     *    Boundry crossed
     * | i  | o  |  i  | o
     *        ot
     *              ct
     * * @param over
     * @return
     */
    @JsonIgnore
    public boolean crossedBound(long ot)
    {
        boolean isOverrideTimeInSchedule = isInSchedule(ot);
        boolean isCurrentTimeInSchedule = isInSchedule(MockTime.getInstance().getMockTime());
        return isOverrideTimeInSchedule != isCurrentTimeInSchedule;
    }
    
    
    public Day getCurrentSchedule()
    {
        long mockTime = MockTime.getInstance().getMockTime();
        for (int i = 0; i < getScheduledIntervals().size(); i++)
        {
            if (getScheduledIntervals().get(i).contains(mockTime))
            {
                return days.get(i);
            }
        }
        return null;
    }
}
