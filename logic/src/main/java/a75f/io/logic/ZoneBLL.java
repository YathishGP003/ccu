package a75f.io.logic;

import a75f.io.bo.building.Floor;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.Zone;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.logic.cache.Globals;

/**
 * Created by Yinten isOn 8/21/2017.
 */

public class ZoneBLL
{
	
	public static Zone findZoneByName(String mFloorName, String mRoomName)
	{
		for (Floor floor : Globals.getInstance().getCCUApplication().floors)
		{
			if (mFloorName.equalsIgnoreCase(floor.mFloorName))
			{
				for (Zone zone : floor.mRoomList)
				{
					if (zone.roomName.equalsIgnoreCase(mRoomName))
					{
						return zone;
					}
				}
			}
		}
		return null;
	}
	
	
	public static void addZoneProfileToZone(Zone zone, LightProfile mLightProfile)
	{
		if (zone.mLightProfile == null)
		{
			zone.mLightProfile = mLightProfile;
		}
		sendControlsMessage(mLightProfile);
	}
	
	
	public static void sendControlsMessage(LightProfile lightProfile)
	{
		for (CcuToCmOverUsbSnControlsMessage_t controlsMessage_t : lightProfile
				                                                           .getControlsMessage())
		{
			SerialBLL.getInstance().sendSerialStruct(controlsMessage_t);
		}
	}
}
