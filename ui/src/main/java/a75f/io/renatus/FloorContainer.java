package a75f.io.renatus;

import java.util.ArrayList;

import a75f.io.bo.Floor;
import a75f.io.bo.Room;
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
		//floorDataAdapter.notifyDataSetChanged();
		return fID;
	}
	
	public void deleteFloor(Floor f) {
		mFloorList.remove(f);
		//floorDataAdapter.notifyDataSetChanged();
		//CCUKinveyInterface.updateFloor(floorData, true);
	}
	
	public ArrayList<Floor> getFloorList() {
		return mFloorList;
	}
	
	public DataArrayAdapter<Floor> getFloorListAdapter() {
		return mFloorListAdapter;
	}
	
	
}
