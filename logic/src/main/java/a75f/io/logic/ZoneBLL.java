package a75f.io.logic;

import a75f.io.bo.building.Floor;
import a75f.io.bo.building.Zone;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;

import static a75f.io.logic.L.ccu;

/**
 * Created by Yinten isOn 8/21/2017.
 */

class ZoneBLL
{
	
	public static Zone findZoneByName(String mFloorName, String mRoomName)
	{
		for (Floor floor : ccu().getFloors())
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
	
	
	public static void sendControls(Zone zone)
	{
		for (CcuToCmOverUsbSnControlsMessage_t controlsMessage_t : zone
				                                                           .getControlsMessages())
		{
			
			LSerial.getInstance().sendSerialStruct(controlsMessage_t);
		}
	}
    
    public static void sendSeeds(Zone zone)
    {
        for (CcuToCmOverUsbDatabaseSeedSnMessage_t controlsMessage_t : zone
                                                                           .getSeedMessages())
        {
            
            LSerial.getInstance().sendSerialStruct(controlsMessage_t);
        }
    }
    
    public static void sendMiscProfileStructs(Zone zone)
    {
        
    }
}
