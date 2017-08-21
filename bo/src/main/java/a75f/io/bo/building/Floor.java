package a75f.io.bo.building;

import java.util.ArrayList;

/**
 * Created by samjithsadasivan on 8/8/17.
 */

public class Floor
{
	
	public static final String FLOOR_NAME  = "FloorName";
	public static final String FLOOR_WEBID = "FloorWebId";
	
	private int             mFloorId;
	private String          mFloorName;
	private String          mKinveyWebId;
	private ArrayList<Zone> mRoomList;
	
	
	public Floor(int floorId, String webId, String floor)
	{
		mFloorId = floorId;
		mKinveyWebId = webId;
		mFloorName = floor;
		mRoomList = new ArrayList<Zone>();
	}
	
	
	public String getName()
	{
		return mFloorName;
	}
	
	
	public void setName(String nName)
	{
		this.mFloorName = nName;
	}
	
	
	public Integer getID()
	{
		return mFloorId;
	}
	
	
	public String getWebID()
	{
		return mKinveyWebId;
	}
	
	
	public String toString()
	{
		return mFloorName;
	}
	
	
	public ArrayList<Zone> getRoomList()
	{
		return mRoomList;
	}
	
	
	public Zone addZone(String room)
	{
		Zone r = new Zone(room);
		mRoomList.add(r);
		return r;
	}
	
	
//	public void deleteRoom(int roomId)
//	{
//		mRoomList.remove(roomId);
//	}
//
//
//	public void saveData(SharedPreferences.Editor editor)
//	{
//		editor.putString(FLOOR_NAME + mFloorId, mFloorName);
//		editor.putString(FLOOR_WEBID + mFloorId, mKinveyWebId);
//	}
}
