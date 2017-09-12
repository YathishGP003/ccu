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
	public    ScheduleMode       mode;
	/*The value when scheduled parameters are active. */
	protected short              val;
	protected ArrayList<Integer> days;
	protected boolean mValidSchedule = false;
	
	ArrayList<Interval> scheduledIntervals = new ArrayList<Interval>();
	public int stHour, stMinute, etHour, etMinute;
	
	
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
		if (isValidSchedule())
		{
			long time = MockTime.getInstance().getMockTime();
			if (scheduledIntervals.isEmpty() && days != null && days.size() > 0)
			{
				buildIntervals();
			}
			for (Interval interval : scheduledIntervals)
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
					scheduledIntervals.add(scheduledInterval);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	
//	/***
//	 *
//	 * @param time this is a formatted time value of 00:00
//	 * @return the time in hours for example 01:12, will return an integer value of 1
//	 * @throws Exception
//	 */
//	@JsonIgnore
//	public int getStringTimeHours(String time) throws Exception
//	{
//		String[] split = time.split("[:]");
//		return Integer.parseInt(split[0]);
//	}
//
//
//	/***
//	 *
//	 * @param time this is a formatted time value of 00:00
//	 * @return the time in minutes for example 01:12, will return an integer value of 12
//	 * @throws Exception
//	 */
//	@JsonIgnore
//	public int getStringTimeMinutes(String time) throws Exception
//	{
//		String[] split = time.split("[:]");
//		return Integer.parseInt(split[1]);
//	}
//
	
	public short getVal()
	{
		return this.val;
	}
	
	
	public void setVal(short val)
	{
		this.val = val;
	}
	
	
//	public String getSt()
//	{
//		return st;
//	}
//
//
//	public void setSt(String st)
//	{
//		this.st = st;
//	}
//
//
//	@JsonIgnore
//	public int getStAsShort() throws Exception
//	{
//		return getStringMMSSAsShort(st);
//	}
//
//
//	@JsonIgnore
//	public int getStringMMSSAsShort(String string)
//	{
//		String[] split = et.split("[:]");
//		return Integer.parseInt(split[0] + split[1]);
//	}
//
//
//	@JsonIgnore
//	public int getEtAsShort() throws Exception
//	{
//		return getStringMMSSAsShort(et);
//	}
//
//
//	public String getEt()
//	{
//		return et;
//	}
//
//
//	public void setEt(String et)
//	{
//		this.et = et;
//	}
//
	
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
		if (scheduledIntervals.isEmpty() && days != null && days.size() > 0)
		{
			buildIntervals();
		}
		DateTime now = new DateTime(MockTime.getInstance().getMockTime());
		for (int i = 0; i < scheduledIntervals.size(); i++)
		{
			Interval interval = scheduledIntervals.get(i);
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
}
