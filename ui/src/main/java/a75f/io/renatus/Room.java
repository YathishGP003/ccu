package a75f.io.renatus;

import android.content.SharedPreferences;

import java.util.ArrayList;

import a75f.io.util.Globals;

/**
 * Created by samjithsadasivan on 8/8/17.
 */

public class Room
{
	public static final String ROOM_NAME = "RoomName";
	public static final String ROOM_WEBID = "RoomWebId";
	public static final String FLOOR_ID = "FloorId";
	
	private int mRoomId;
	private String mRoomName;
	private int mFloorId;
	
	private ArrayList<Module> mModuleList;
	private DataArrayAdapter<Module> mModuleListAdapter = null;
	
	
	
	public Room(int roomId, String room, int floorId) {
		mRoomId = roomId;
		mRoomName = room;
		mFloorId = floorId;
		mModuleList = new ArrayList<>();
		mModuleListAdapter = new DataArrayAdapter<Module>(Globals.getInstance().getApplicationContext(),
				                                               R.layout.listviewitem, mModuleList);
	}
	
	public int getmRoomId() {
		return mRoomId;
	}
	
	public String getRoomName() {
		return mRoomName;
	}
	
	public int getFloorId() {
		return mFloorId;
	}
	
	public String toString() {
		return mRoomName;
	}
	
	public Module addModule(String moduleName ,short address) {
		Module m = new Module(mRoomId, moduleName, mModuleList.size(), address);
		mModuleList.add(m);
		return m;
	}
	
	public void saveData(SharedPreferences.Editor editor, int floorId) {
		editor.putString(ROOM_NAME+ floorId+ mRoomId, mRoomName);
		editor.putInt(FLOOR_ID+ floorId+mRoomId, mFloorId);
		//editor.putString(ROOM_NAME+mRoomId, mKinveyWebId);
	}
	
}
