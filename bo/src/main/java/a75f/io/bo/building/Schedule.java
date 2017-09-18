package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.Collections;

import a75f.io.bo.building.definitions.DAYS;
import a75f.io.bo.building.definitions.MockTime;
import a75f.io.bo.building.definitions.ScheduleMode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Schedule
{
    public static final String TAG = "Schedule";
    public ScheduleMode mode;
    public int          stHour, stMinute, etHour, etMinute;
    /*The value when scheduled parameters are active. */
    protected short              val;
    protected ArrayList<Integer> days;
    protected boolean mValidSchedule = false;
    ArrayList<Interval> mScheduledIntervals = new ArrayList<Interval>();
    
    
    public Schedule()
    {
    }
    
    
    public Schedule(short val, int stHour, int stMinute, int etHour, int etMinute,
                    ArrayList<Integer> days)
    {
        this.val = val;
        this.days = days;
        this.stHour = stHour;
        this.stMinute = stMinute;
        this.etHour = etHour;
        this.etMinute = etMinute;
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
                Collections.sort(days);
                DateTime startDateTime =
                        new DateTime(MockTime.getInstance().getMockTime()).withHourOfDay(stHour)
                                                                          .withMinuteOfHour(stMinute);
                DateTime endDateTime =
                        new DateTime(MockTime.getInstance().getMockTime()).withHourOfDay(etHour)
                                                                          .withMinuteOfHour(etMinute);
                //Add the scheduled intervals.
                for (int day : days)
                {
                    Interval scheduledInterval =
                            new Interval(startDateTime.withDayOfWeek(day + 1), endDateTime
                                                                                       .withDayOfWeek(
                                                                                               day +
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
    
    
    public short getVal()
    {
        return this.val;
    }
    
    
    public void setVal(short val)
    {
        this.val = val;
    }
    
    
    public ArrayList<Integer> getDays()
    {
        return days;
    }
    
    
    public void setDays(ArrayList<Integer> days)
    {
        this.days = days;
    }
    
    
    /*****************
     *ONLY USED FOR CIRCUITS BELOW
     ******************/
    @JsonIgnore
    public byte getScheduleDaysBitmap()
    {
        ArrayList<Integer> daysAsArrayList = getDaysAsArrayList();
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
    
    
    @JsonIgnore
    public ArrayList<Integer> getDaysAsArrayList()
    {
        ArrayList<Integer> retVal = new ArrayList<Integer>();
        for (int j = 0; j < days.size(); j++)
        {
            retVal.add(days.get(j));
        }
        return retVal;
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
    
    
    @JsonIgnore
    public void setSt(Integer currentHour, Integer currentMinute)
    {
        stHour = currentHour;
        stMinute = currentMinute;
    }
    
    
    @JsonIgnore
    public void setEt(Integer currentHour, Integer currentMinute)
    {
        etHour = currentHour;
        etMinute = currentMinute;
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
    public boolean crossedBound(long ot)
    {
        boolean isOverrideTimeInSchedule = isInSchedule(ot);
        boolean isCurrentTimeInSchedule = isInSchedule(MockTime.getInstance().getMockTime());
        return isOverrideTimeInSchedule != isCurrentTimeInSchedule;
    }
}
