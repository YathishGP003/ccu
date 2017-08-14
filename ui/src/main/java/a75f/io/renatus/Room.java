package a75f.io.renatus;

import android.content.SharedPreferences;

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
	
	public Room(int roomId, String room, int floorId) {
		mRoomId = roomId;
		mRoomName = room;
		mFloorId = floorId;
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
	
	public void saveData(SharedPreferences.Editor editor, int floorId) {
		editor.putString(ROOM_NAME+ floorId+ mRoomId, mRoomName);
		editor.putInt(FLOOR_ID+ floorId+mRoomId, mFloorId);
		//editor.putString(ROOM_NAME+mRoomId, mKinveyWebId);
	}
	
}
