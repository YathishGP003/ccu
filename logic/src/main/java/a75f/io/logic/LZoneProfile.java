package a75f.io.logic;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import a75f.io.bo.building.Schedule;
import a75f.io.bo.building.SmartNodeOutput;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.building.definitions.LScheduleAction;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;

import static a75f.io.logic.L.ccu;
import static a75f.io.logic.LLog.Logd;

/**
 * Created by Yinten on 9/10/2017.
 */

class LZoneProfile implements IScheduleAction
{
	private static final String TAG = "ZoneProfile";
	LScheduler zoneProfileLScheduler = new LScheduler(this);
	
	
	@WorkerThread
	public SmartNodeOutput findPort(ArrayList<SmartNodeOutput> smartNodeOutputs, Port port, short smartNodeAddress)
	{
		for (SmartNodeOutput smartNodeOutput : smartNodeOutputs)
		{
			if (smartNodeOutput.mSmartNodePort == port)
			{
				smartNodeOutput.mConfigured = true;
				return smartNodeOutput;
			}
		}
		SmartNodeOutput smartNodeOutput = new SmartNodeOutput();
		smartNodeOutput.mSmartNodePort = port;
		smartNodeOutput.mSmartNodeAddress = smartNodeAddress;
		smartNodeOutput.mConfigured = false;
		return smartNodeOutput;
	}
	@Override
	public void takeAction(ScheduledItem action)
	{
		handleZoneProfileScheduledEvent(action);
	}
	@WorkerThread
	private void handleZoneProfileScheduledEvent(@NonNull ScheduledItem scheduledItem)
	{
		Logd("handleZoneProfileScheduledEvent()");
		Logd(scheduledItem.toString());
		
		ZoneProfile zoneProfileByUUID = ccu().findZoneProfileByUUID(scheduledItem.mUuid);
		if (zoneProfileByUUID != null)
		{
			for (SmartNodeOutput smartNodeOutput : zoneProfileByUUID.smartNodeOutputs)
			{
				smartNodeOutput.setOverride(false);
			}
			Logd(zoneProfileByUUID.toString());
			List<CcuToCmOverUsbSnControlsMessage_t> controlsMessage = zoneProfileByUUID.getControlsMessage();
			if (controlsMessage != null)
			{
				for (CcuToCmOverUsbSnControlsMessage_t controlMessage_t : controlsMessage)
				{
					Logd(controlMessage_t.toString());
					LSerial.getInstance().sendSerialStruct(controlMessage_t);
				}
			}
			try
			{
				scheduleProfiles();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	@WorkerThread
	public void scheduleProfiles() throws Exception
	{
		
		ArrayList<ZoneProfile> allZoneProfiles = ccu().findAllZoneProfiles();
		Log.i(TAG, "allZoneProfiles available: " + Arrays.toString(allZoneProfiles.toArray()));
		for (ZoneProfile zp : allZoneProfiles)
		{
			ScheduledItem nextActiveScheduledTime = getNextActiveScheduledTime(zp);
			Log.i(TAG, "Zone Profile Next Active Schedule Time: " + nextActiveScheduledTime.toString());
			if (nextActiveScheduledTime != null)
			{
				zoneProfileLScheduler.add(nextActiveScheduledTime);
			}
		}
	}
	@WorkerThread
	private ScheduledItem getNextActiveScheduledTime(ZoneProfile zoneProfile) throws Exception
	{
		Schedule nextActiveScheduleTime = null;
		// Go through the zone profiles schedules.
		SmartNodeOutput nextScheduledSmartNodeOutput = null;
		for (Schedule schedule : zoneProfile.mSchedules)
		{
			if (compareSchedule(nextActiveScheduleTime, schedule))
			{
				nextActiveScheduleTime = schedule;
			}
		}
		List<SmartNodeOutput> smartNodeOutputs = zoneProfile.smartNodeOutputs;
		for (SmartNodeOutput smartNodeOutput : smartNodeOutputs)
		{
			for (Schedule schedule : smartNodeOutput.mSchedules)
			{
				if (compareSchedule(nextActiveScheduleTime, schedule))
				{
					nextScheduledSmartNodeOutput = smartNodeOutput;
					nextActiveScheduleTime = schedule;
				}
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
			scheduledItem.mUuid = zoneProfile.uuid;
			scheduledItem.lScheduleAction = LScheduleAction.CONTROLS_UPDATE;
			scheduledItem.mTimeStamp = new DateTime(nextActiveScheduleTime.getNextScheduleTransistionTime());
			return scheduledItem;
		}
	}
	
	
	private boolean compareSchedule(Schedule nextActiveScheduleTime, Schedule schedule) throws Exception
	{
		if (nextActiveScheduleTime == null || schedule.getNextScheduleTransistionTime() < nextActiveScheduleTime.getNextScheduleTransistionTime())
		{
			return true;
		}
		return false;
	}
}
