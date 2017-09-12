package a75f.io.logic;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.Floor;
import a75f.io.bo.building.SmartNodeOutput;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.building.definitions.ScheduledItem;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.logic.cache.Globals;

import static a75f.io.logic.LLog.Logd;

/**
 * Created by Yinten on 9/10/2017.
 */

public class LZoneProfile
{
	private static final String TAG = "ZoneProfile";
	
	
	@WorkerThread
	public static void handleZoneProfileScheduledEvent(@NonNull ScheduledItem scheduledItem)
	{
		Logd("handleZoneProfileScheduledEvent()");
		Logd(scheduledItem.toString());
		CCUApplication ccuApplication = Globals.getInstance().getCCUApplication();
		ZoneProfile zoneProfileByUUID = ccuApplication.findZoneProfileByUUID(scheduledItem.mUuid);
		if (zoneProfileByUUID != null)
		{
			Logd(zoneProfileByUUID.toString());
			List<CcuToCmOverUsbSnControlsMessage_t> controlsMessage =
					zoneProfileByUUID.getControlsMessage();
			if (controlsMessage != null)
			{
				for (CcuToCmOverUsbSnControlsMessage_t controlMessage_t : controlsMessage)
				{
					Logd(controlMessage_t.toString());
					SerialBLL.getInstance().sendSerialStruct(controlMessage_t);
				}
			}
		}
	}
	
	
	@WorkerThread
	public static void scheduleProfiles() throws Exception
	{
		ArrayList<ZoneProfile> allZoneProfiles =
				Globals.getInstance().getCCUApplication().findAllZoneProfiles();
		Log.i(TAG, "allZoneProfiles available: " + Arrays.toString(allZoneProfiles.toArray()));
		for (ZoneProfile zp : allZoneProfiles)
		{
			ScheduledItem nextActiveScheduledTime = zp.getNextActiveScheduledTime();
			Log.i(TAG, "Zone Profile Next Active Schedule Time: " +
			           nextActiveScheduledTime.toString());
			if (nextActiveScheduledTime != null)
			{
				Globals.getInstance().getLScheduler().add(nextActiveScheduledTime);
			}
		}
	}
	
	
	public static SmartNodeOutput findPort(ArrayList<SmartNodeOutput> smartNodeOutputs, Port port,
	                                       short smartNodeAddress)
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
	
	
	public static Zone findZone(ZoneProfile mProfile)
	{
		ArrayList<Floor> floors = Globals.getInstance().getCCUApplication().floors;
		for (Floor floor : floors)
		{
			for (Zone zone : floor.mRoomList)
			{
				if (zone.mLightProfile != null && mProfile.equals(zone.mLightProfile))
				{
					return zone;
				}
			}
		}
		return null;
	}
}
