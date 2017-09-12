package a75f.io.bo.building;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import a75f.io.bo.BuildConfig;
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
	
	
	
	public static final String                TAG              = ZoneProfile.class.getSimpleName();
	@JsonIgnore
	public              UUID                  uuid             = UUID.randomUUID();
	public              ArrayList<Schedule>   mSchedules       = new ArrayList<>();
	public              List<SmartNodeInput>  smartNodeInputs  = new ArrayList<>();
	public              List<SmartNodeOutput> smartNodeOutputs = new ArrayList<>();
	
	
	public ZoneProfile()
	{
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
			scheduledItem.mTimeStamp =
					new DateTime(nextActiveScheduleTime.getNextScheduleTransistionTime());
			return scheduledItem;
		}
	}
	
	
	@JsonIgnore
	private Schedule getSchedule(Schedule nextActiveScheduleTime, Schedule schedule)
			throws Exception
	{
		if (nextActiveScheduleTime == null || schedule.getNextScheduleTransistionTime() <
		                                      nextActiveScheduleTime
				                                      .getNextScheduleTransistionTime())
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
	
	
	public ArrayList<SmartNodeOutput> findSmartNodeOutputs(short mSmartNodeAddress)
	{
		ArrayList<SmartNodeOutput> retValArrayList = new ArrayList<>();
		for (SmartNodeOutput smartNodeOutput : smartNodeOutputs)
		{
			if (smartNodeOutput.mSmartNodeAddress == mSmartNodeAddress)
			{
				retValArrayList.add(smartNodeOutput);
			}
		}
		return retValArrayList;
	}
	
	
	public ArrayList<SmartNodeInput> findSmartNodeInputs(short mSmartNodeAddress)
	{
		ArrayList<SmartNodeInput> retValArrayList = new ArrayList<>();
		for (SmartNodeInput smartNodeInput : smartNodeInputs)
		{
			if (smartNodeInput.mSmartNodeAddress == mSmartNodeAddress)
			{
				retValArrayList.add(smartNodeInput);
			}
		}
		return retValArrayList;
	}
	
	
	public void removeCircuit(SmartNodeOutput smartNodeOutput)
	{
		if (BuildConfig.DEBUG)
		{
			Log.w(TAG, "Adding SmartNodeOutput: " + smartNodeOutput);
			Log.d(TAG, "------------TO------------");
			Log.d(TAG, Arrays.toString(smartNodeOutputs.toArray()));
		}
		if (smartNodeOutputs.contains(smartNodeOutput))
		{
			if (BuildConfig.DEBUG)
			{
				Log.w(TAG, "removing smartNodeOutputs.contains(smartNodeOuput): " +
				           smartNodeOutputs.contains(smartNodeOutput));
			}
			smartNodeOutputs.remove(smartNodeOutput);
		}
	}
	
	
	public void addCircuit(SmartNodeOutput smartNodeOuput)
	{
		if (BuildConfig.DEBUG)
		{
			Log.w(TAG, "Adding SmartNodeOutput: " + smartNodeOuput);
			Log.d(TAG, "------------TO------------");
			Log.d(TAG, Arrays.toString(smartNodeOutputs.toArray()));
		}
		if (!smartNodeOutputs.contains(smartNodeOuput))
		{
			if (BuildConfig.DEBUG)
			{
				Log.w(TAG, "!smartNodeOutputs.contains(smartNodeOuput): " +
				           !smartNodeOutputs.contains(smartNodeOuput));
			}
			smartNodeOutputs.add(smartNodeOuput);
		}
	}
	
	
	@Override
	public String toString()
	{
		return "ZoneProfile{" + "uuid=" + uuid + ", mSchedules=" + mSchedules +
		       ", smartNodeInputs=" + smartNodeInputs + ", smartNodeOutputs=" + smartNodeOutputs +
		       '}';
	}
	
}
