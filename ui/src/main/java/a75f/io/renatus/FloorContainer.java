package a75f.io.renatus;

import android.content.SharedPreferences;

import java.util.ArrayList;

import a75f.io.util.Globals;

/**
 * Created by samjithsadasivan on 8/8/17.
 */

public class FloorContainer
{
	
	/*public class Floor
	{
		
		private int             mFloorId;
		private String          mFloorName;
		private String          mKinveyWebId;
		private ArrayList<Room> mRoomList;
		
		private Floor(int floorId, String webId, String floor)
		{
			mFloorId = floorId;
			mKinveyWebId = webId;
			mFloorName = floor;
			mRoomList = new ArrayList<Room>();
		}
	}*/
	
	
	private static FloorContainer sInstance = null;
	
	private ArrayList<Floor> mFloorList = null;
	private DataArrayAdapter<Floor> mFloorListAdapter = null;
	
	
	private FloorContainer(){
		mFloorList = new ArrayList<>();
		mFloorListAdapter = new DataArrayAdapter<Floor>(Globals.getInstance().getApplicationContext(),
				                                               R.layout.listviewitem, mFloorList);
	}
	
	public static FloorContainer getInstance() {
		if (sInstance == null) {
			sInstance = new FloorContainer();
		}
		return sInstance;
	}
	
	public int addFloor(String floorName) {
		int fID = mFloorList.size();
		String sWebId = null; //CCUKinveyInterface.getKinveyId() + "_" + UUID.randomUUID().toString();
		Floor floor = new Floor(fID, sWebId, floorName);
		mFloorList.add(fID, floor);
		mFloorListAdapter.notifyDataSetChanged();
		return fID;
	}
	
	public void deleteFloor(Floor f) {
		mFloorList.remove(f);
		mFloorListAdapter.notifyDataSetChanged();
		//CCUKinveyInterface.updateFloor(floorData, true);
	}
	
	public ArrayList<Floor> getFloorList() {
		return mFloorList;
	}
	
	public DataArrayAdapter<Floor> getFloorListAdapter() {
		return mFloorListAdapter;
	}
	
	public void loadData() {
		//mFloorDataLoaded = true;
		mFloorList.clear();
		//		FloorData.context = context;
		SharedPreferences spFloorData =  Globals.getInstance().getApplicationContext().getSharedPreferences("floordata",0);
		int nNumFloors = spFloorData.getInt("NumFloors", 0);
		for (int nIndex = 0; nIndex < nNumFloors; nIndex++)
		{
			String sFloorId = String.valueOf(nIndex);
			String sFloorName = spFloorData.getString(Floor.FLOOR_NAME + sFloorId, "");
			String sFloorWebID = spFloorData.getString(Floor.FLOOR_WEBID + sFloorId, "");
			if (sFloorWebID.isEmpty()) {
				//sFloorWebID = CCUKinveyInterface.getKinveyId() + "_" + UUID.randomUUID().toString();
				spFloorData.edit().putString("FloorWebId" + sFloorId, sFloorWebID).commit();
			}
			if (!sFloorName.isEmpty())
			{
				Floor floorData = new Floor(nIndex, sFloorWebID, sFloorName);
				mFloorList.add(nIndex, floorData);
				int nNumRooms = spFloorData.getInt("NumRooms" + sFloorId, 0);
				for (int nRoomIndex = 0; nRoomIndex < nNumRooms; nRoomIndex++)
				{
					String sRoomId = String.valueOf(nRoomIndex);
					String sRoomName = spFloorData.getString("RoomName" + sFloorId + sRoomId, "");
					if (!sRoomName.isEmpty())
					{
						Room room = floorData.addRoom(sRoomName);
						/*room.loadData(spFloorData, sFloorId + sRoomId);
						int nNumFSVs = spFloorData.getInt("NumFSV" + sFloorId + sRoomId, 0);
						for (int nFSVIndex = 0; nFSVIndex < nNumFSVs; nFSVIndex++)
						{
							String sFSVId = String.valueOf(nFSVIndex);
							FSVData fsvData = FSVData.loadFSV(spFloorData, sFloorId + sRoomId + sFSVId);
							if (fsvData != null)
							{
								roomData.addFSV(fsvData);
							}
						}
						roomData.sendZoneScheduleToCloud();
						roomData.sendSettingsToWeb("Init fsv update");*/
					}
				}
			}
		}
		mFloorListAdapter = new DataArrayAdapter<Floor>(Globals.getInstance().getApplicationContext(),
				                                               R.layout.listviewitem, mFloorList);
		/*DCVSensorData.getHandle().loadData();
		PressureSensorData.getHandle().loadData();
		NO2SensorData.getHandle().loadData();
		COSensorData.getHandle().loadData();
		startCloudSync();*/
	}
	
	public void saveData() {
		
		SharedPreferences floorData = Globals.getInstance().getApplicationContext().getSharedPreferences("floordata",0);
		SharedPreferences.Editor editor = floorData.edit();
		editor.putInt("NumFloors", mFloorList.size());
		
		for (Floor f : mFloorList) {
			f.saveData(editor);
			ArrayList<Room> roomList = f.getRoomList();
			editor.putInt("NumRooms" + f.getID(), roomList.size());
			for (Room r : roomList) {
				r.saveData(editor, f.getID());
				//TODO - save all paired smart node data here.
			}
			
		}
		
		editor.commit();
	}
}
