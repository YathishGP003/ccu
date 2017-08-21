package a75f.io.renatus;

import java.util.ArrayList;

import a75f.io.bo.building.Floor;
import a75f.io.util.Globals;
import a75f.io.util.prefs.LocalStorage;

/**
 * Created by samjithsadasivan on 8/8/17.
 */

public class FloorContainer
{
	
	private static FloorContainer sInstance = null;
	
	private ArrayList<Floor>        mFloorList        = null;
	private DataArrayAdapter<Floor> mFloorListAdapter = null;
	
	
	private FloorContainer()
	{
		mFloorList = new ArrayList<>();
		mFloorListAdapter = new DataArrayAdapter<Floor>(Globals.getInstance().getApplicationContext(), R.layout.listviewitem, mFloorList);
	}
	
	
	public static FloorContainer getInstance()
	{
		if (sInstance == null)
		{
			sInstance = new FloorContainer();
		}
		return sInstance;
	}
	
	
	public int addFloor(String floorName)
	{
		int fID = mFloorList.size();
		String sWebId = null; //CCUKinveyInterface.getKinveyId() + "_" + UUID.randomUUID().toString();
		Floor floor = new Floor(fID, sWebId, floorName);
		mFloorList.add(fID, floor);
		mFloorListAdapter.notifyDataSetChanged();
		return fID;
	}
	
	
	public void deleteFloor(Floor f)
	{
		mFloorList.remove(f);
		mFloorListAdapter.notifyDataSetChanged();
		//CCUKinveyInterface.updateFloor(floorData, true);
	}
	
	
	public ArrayList<Floor> getFloorList()
	{
		return mFloorList;
	}
	
	
	public DataArrayAdapter<Floor> getFloorListAdapter()
	{
		return mFloorListAdapter;
	}
	
	
	public void loadData()
	{
		mFloorList.clear();
		mFloorListAdapter = new DataArrayAdapter<Floor>(Globals.getInstance().getApplicationContext(), R.layout.listviewitem, Globals.getInstance().getCCUApplication().floors);
		/*DCVSensorData.getHandle().loadData();
		PressureSensorData.getHandle().loadData();
		NO2SensorData.getHandle().loadData();
		COSensorData.getHandle().loadData();
		startCloudSync();*/
	}
	
	
	public void saveData()
	{
		//Apply changes
		Globals.getInstance().getCCUApplication().floors = mFloorList;
		//Save
		LocalStorage.setApplicationSettings(Globals.getInstance().getCCUApplication());
	}
}
