package a75f.io.bo.building;

import java.util.ArrayList;

/**
 * Created by Yinten on 8/15/2017.
 */
//Also known as room.
public class Zone
{
	public String                 roomName     = "Default Zone";
	public ArrayList<ZoneProfile> zoneProfiles = new ArrayList<ZoneProfile>();
	
	
	//Also known as zone name.
	public Zone(String roomName)
	{
		this.roomName = roomName;
	}
}
