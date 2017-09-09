package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.Arrays;

import a75f.io.bo.building.definitions.DAYS;
import a75f.io.bo.building.definitions.MockTime;
import a75f.io.bo.building.definitions.ScheduleMode;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class Schedule
{
	public static final String TAG = "Schedule";
	public    ScheduleMode mode;
	/*The value when scheduled parameters are active. */
	protected short        val;
	protected String       st;
	protected String       et;
	protected String[]     days;
	
	private int curVal;
	
	
	public Schedule()
	{
	}
	
	
	public Schedule(short val, String st, String et, String[] days)
	{
		this.val = val;
		this.st = st;
		this.et = et;
		this.days = days;
	}
	
	
	@JsonIgnore
	private int getScheduleValue()
	{
		return val;
	}
	
	
	@JsonIgnore
	public boolean isInSchedule()
	{
		return isInDays() && isInHours();
	}
	
	
	@JsonIgnore
	public boolean isInDays()
	{
		DateTime dateTime = new DateTime(MockTime.getInstance().getMockTime());
		int currentDayOfWeekWithMondayAsStart = dateTime.dayOfWeek().get();
		//TODO: logged not mocked
		System.out.println(TAG + " isInScheduled currentDayOfWeekWithMondayAsStar: " +
		                   currentDayOfWeekWithMondayAsStart);
		//If there are no days return false, nothing will be findable.
		if (days == null)
		{
			return false;
		}
		//TODO: logged not mocked
		System.out.println(TAG + " isInSchedule week days: " + Arrays.toString(days));
		int foundIndex = Arrays.binarySearch(days, String.valueOf
				                                                  (currentDayOfWeekWithMondayAsStart));
		//TODO: logged not mocked
		System.out.println(TAG + " Arrays.binarySearch(days, currentDayOfWeekWithMondayAsStart): " +
		                   foundIndex);
		if (foundIndex > -1)
		{
			System.out.println(TAG + " returning true  ");
			return true;
		}
		return false;
	}
	
	
	/***
	 * Populates two datetimes from system's current time or mockable time.   It then adjusts the
	 * start time date time by the hour and minute in the vairable st.  Following it adjusts the
	 * start time date time by the hour and minute in the variable et time.   It then checks to
	 * see if the current mocked or real system time is between the adjusted start and end date
	 * time.  If an exception in parsing occurs TODO: a log should be generated and sent to
	 * crashlytics.
	 * @return if the current system time (or mocked time) is between the hours and minutes from
	 * st and et.
	 *
	 */
	@JsonIgnore
	public boolean isInHours()
	{
		try
		{
			System.out.println(TAG + " isInHours()  ");
			if (st != null && !st.equalsIgnoreCase("") && et != null && !et.equalsIgnoreCase(""))
			{
				int startTimeHours = getStringTimeHours(st);
				System.out.println(TAG + " startTimeHours:  " + startTimeHours);
				int startTimeMinutes = getStringTimeMinutes(st);
				System.out.println(TAG + " startTimeMinutes:  " + startTimeMinutes);
				int endTimeHours = getStringTimeHours(et);
				System.out.println(TAG + " endTimeHours:  " + endTimeHours);
				int endTimeMinutes = getStringTimeMinutes(et);
				System.out.println(TAG + " endTimeMinutes:  " + endTimeMinutes);
				DateTime startDateTime = new DateTime(MockTime.getInstance().getMockTime())
						                         .withHourOfDay(startTimeHours)
						                         .withMinuteOfHour(startTimeMinutes);
				System.out.println("Start Date Time: " + startDateTime.toString());
				DateTime endDateTime = new DateTime(MockTime.getInstance().getMockTime())
						                       .withHourOfDay(endTimeHours)
						                       .withMinuteOfHour(endTimeMinutes);
				System.out.println("EndD Date Time: " + endDateTime.toString());
				Interval interval =
						new Interval(startDateTime.toInstant(), endDateTime.toInstant());
				System.out.println("Interval: " + interval.toString());
				System.out.println("Interval contains: " +
				                   interval.contains(MockTime.getInstance().getMockTime()));
				return interval.contains(MockTime.getInstance().getMockTime());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	
	/***
	 *
	 * @param time this is a formatted time value of 00:00
	 * @return the time in hours for example 01:12, will return an integer value of 1
	 * @throws Exception
	 */
	@JsonIgnore
	public int getStringTimeHours(String time) throws Exception
	{
		String[] split = time.split("[:]");
		return Integer.parseInt(split[0]);
	}
	
	
	/***
	 *
	 * @param time this is a formatted time value of 00:00
	 * @return the time in minutes for example 01:12, will return an integer value of 12
	 * @throws Exception
	 */
	@JsonIgnore
	public int getStringTimeMinutes(String time) throws Exception
	{
		String[] split = time.split("[:]");
		return Integer.parseInt(split[1]);
	}
	
	
	public short getVal()
	{
		return this.val;
	}
	
	
	public void setVal(short val)
	{
		this.val = val;
	}
	
	
	public String getSt()
	{
		return st;
	}
	
	
	public void setSt(String st)
	{
		this.st = st;
	}
	
	
	@JsonIgnore
	public int getStAsShort() throws Exception
	{
		return getStringMMSSAsShort(st);
	}
	
	
	@JsonIgnore
	public int getStringMMSSAsShort(String string)
	{
		String[] split = et.split("[:]");
		return Integer.parseInt(split[0] + split[1]);
	}
	
	
	@JsonIgnore
	public int getEtAsShort() throws Exception
	{
		return getStringMMSSAsShort(et);
	}
	
	
	public String getEt()
	{
		return et;
	}
	
	
	public void setEt(String et)
	{
		this.et = et;
	}
	
	
	public String[] getDays()
	{
		return days;
	}
	
	
	public void setDays(String[] days)
	{
		this.days = days;
	}
	
	
	/*****************
	 *ONLY USED FOR CIRCUITS BELOW
	 ******************/
	
	public byte getScheduleDaysBitmap()
	{
		ArrayList<Integer> daysAsArrayList = new ArrayList<Integer>();
		for (int j = 0; j < days.length; j++)
		{
			daysAsArrayList.add(Integer.parseInt(days[j]));
		}
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
}
