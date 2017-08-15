package a75f.io.renatus;

import android.content.SharedPreferences;

import a75f.io.util.Globals;

/**
 * Created by samjithsadasivan on 8/15/17.
 */

public class Module
{
	public static final String MODULE_NAME = "ModuleName";
	public static final String MODULE_ADDRESS = "ModuleAddress";
	public static final String ROOM_ID = "RoomId";
	
	private int mRoomId;
	private String mModuleName;
	private short mModuleAddress;
	private int mModuleId;
	
	public Module(int roomId, String name, int moduleId, short moduleAddress) {
		mRoomId = roomId;
		mModuleName = name;
		mModuleId = moduleId;
		mModuleAddress = moduleAddress;
	}
	
	public int getmRoomId() {
		return mRoomId;
	}
	public String getModuleName() {
		return mModuleName;
	}
	public int getModuleId() {
		return mModuleId;
	}
	public short getmModuleAddress() {
		return mModuleAddress;
	}
	
	public String toString() {
		return mModuleName;
	}
	
	public void saveData(SharedPreferences.Editor editor, int floorId) {
		editor.putString(MODULE_NAME+mRoomId +mModuleId, mModuleName);
		editor.putInt(ROOM_ID+ mRoomId+mModuleId, mRoomId);
		editor.putInt(MODULE_ADDRESS+mRoomId+mModuleId, (int)mModuleAddress);
	}
}
