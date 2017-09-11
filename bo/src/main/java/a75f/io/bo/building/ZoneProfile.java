package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import a75f.io.bo.building.definitions.LScheduleAction;
import a75f.io.bo.building.definitions.ScheduledItem;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;

/**
 * Created by Yinten isOn 8/15/2017.
 */
@JsonDeserialize(as = LightProfile.class)
@JsonSerialize(as = LightProfile.class)
public abstract class ZoneProfile
{
	@JsonIgnore
	public UUID uuid = UUID.randomUUID();
	public String mModuleName;
	public ArrayList<Schedule>   mSchedules        = new ArrayList<>();
	public List<Sensor>          sensors          = new ArrayList<Sensor>();
	public List<SmartNodeInput>  smartNodeInputs  = new ArrayList<SmartNodeInput>();
	public List<SmartNodeOutput> smartNodeOutputs = new ArrayList<SmartNodeOutput>();
	
	
	public ZoneProfile()
	{
	}
	
	
	public ZoneProfile(String name)
	{
		this.mModuleName = name;
	}
	
	
	@JsonIgnore
	public abstract List<CcuToCmOverUsbSnControlsMessage_t> getControlsMessage();
	
	@JsonIgnore
	public ScheduledItem getNextActiveScheduledTime() throws Exception
	{
		Schedule nextActiveScheduleTime = null;
		// Go through the zone profiles schedules.
		for (Schedule schedule : mSchedules)
		{
			nextActiveScheduleTime = getSchedule(nextActiveScheduleTime, schedule);
		}
		for (SmartNodeOutput smartNodeOutput : smartNodeOutputs)
		{
			for (Schedule schedule : smartNodeOutput.mSchedules)
			{
				nextActiveScheduleTime = getSchedule(nextActiveScheduleTime, schedule);
			}
		}
		//No schedules
		if (nextActiveScheduleTime == null)
		{
			return null;
		}
		else
		{
			ScheduledItem scheduledItem = new ScheduledItem();
			scheduledItem.mUuid = this.uuid;
			scheduledItem.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
			scheduledItem.mTimeStamp = nextActiveScheduleTime.getNextScheduleTransistionTime();
			return scheduledItem;
		}
	}
	
	@JsonIgnore
	private Schedule getSchedule(Schedule nextActiveScheduleTime, Schedule schedule)
			throws Exception
	{
		if (nextActiveScheduleTime == null || schedule.getNextScheduleTransistionTime().isBefore(nextActiveScheduleTime
				                                                                                         .getNextScheduleTransistionTime())
		                                      )
		{
			
			nextActiveScheduleTime = schedule;
		}
		return nextActiveScheduleTime;
	}
	
	@JsonIgnore
	public boolean hasSchedules()
	{
		return !mSchedules.isEmpty();
	}
	
	
	@JsonIgnore
	public String toString()
	{
		return mModuleName;
	}
}
