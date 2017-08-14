package a75f.io.renatus;

import android.content.SharedPreferences;

import java.util.ArrayList;

import a75f.io.util.Globals;

/**
 * Created by samjithsadasivan on 8/8/17.
 */

public class Floor
{
	
	public static final String FLOOR_NAME = "FloorName";
	public static final String FLOOR_WEBID = "FloorWebId";
	
	private int             mFloorId;
	private String          mFloorName;
	private String          mKinveyWebId;
	private ArrayList<Room> mRoomList;
	private DataArrayAdapter<Room> mRoomListAdapter = null;
	
	
	public Floor(int floorId ,String webId, String floor ) {
		mFloorId = floorId;
		mKinveyWebId = webId;
		mFloorName = floor;
		mRoomList = new ArrayList<Room>();
		mRoomListAdapter = new DataArrayAdapter<Room>(Globals.getInstance().getApplicationContext(),
				                                             R.layout.listviewitem, mRoomList);
		
	}
	
	
	public String getName() {
		return mFloorName;
	}
	public void setName(String nName) {
		this.mFloorName = nName;
	}
	public Integer getID() {
		return mFloorId;
	}
	public String getWebID() {
		return mKinveyWebId;
	}
	public String toString() {
		return mFloorName;
	}
	public ArrayList<Room> getRoomList() {return mRoomList;}
	public DataArrayAdapter<Room> getmRoomListAdapter() { return mRoomListAdapter;}
	public Room addRoom(String room){
		Room r = new Room(mRoomList.size() ,room, mFloorId);
		mRoomList.add(r);
		return r;
	}
	
	public void deleteRoom (int roomId) {
		mRoomList.remove(roomId);
	}
	
	public void saveData(SharedPreferences.Editor editor) {
		editor.putString(FLOOR_NAME+mFloorId, mFloorName);
		editor.putString(FLOOR_WEBID+mFloorId, mKinveyWebId);
	}
	
	
}
