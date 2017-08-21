package a75f.io.logic;

import a75f.io.bo.building.Floor;
import a75f.io.bo.building.Zone;
import a75f.io.util.Globals;

/**
 * Created by Yinten on 8/21/2017.
 */

public class RoomBLL
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
}
