package a75f.io.renatus;

import java.util.ArrayList;

import a75f.io.bo.building.Floor;
import a75f.io.logic.cache.Globals;
import a75f.io.logic.cache.prefs.LocalStorage;

/**
 * Created by samjithsadasivan isOn 8/8/17.
 */

public class FloorContainer
{
	
	private static FloorContainer sInstance = null;
	
	private DataArrayAdapter<Floor> mFloorListAdapter = null;
	
	
	private FloorContainer()
	{
		mFloorListAdapter = new DataArrayAdapter<Floor>(Globals.getInstance()
		                                                       .getApplicationContext(), R.layout.listviewitem, Globals.getInstance()
		                                                                                                               .getCCUApplication().floors);
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
		int fID = Globals.getInstance().getCCUApplication().floors.size();
		String sWebId =
				null; //CCUKinveyInterface.getKinveyId() + "_" + UUID.randomUUID().toString();
		Floor floor = new Floor(fID, "webid", floorName);
		Globals.getInstance().getCCUApplication().floors.add(fID, floor);
		mFloorListAdapter.notifyDataSetChanged();
		return fID;
	}
	
	
	public void deleteFloor(Floor f)
	{
		Globals.getInstance().getCCUApplication().floors.remove(f);
		mFloorListAdapter.notifyDataSetChanged();
		//CCUKinveyInterface.updateFloor(floorData, true);
	}
	
	
	public ArrayList<Floor> getFloorList()
	{
		return Globals.getInstance().getCCUApplication().floors;
	}
	
	
	public DataArrayAdapter<Floor> getFloorListAdapter()
	{
		mFloorListAdapter = new DataArrayAdapter<Floor>(Globals.getInstance()
		                                                       .getApplicationContext(), R.layout.listviewitem, Globals.getInstance()
		                                                                                                               .getCCUApplication().floors);
		return mFloorListAdapter;
	}
	
	
	public void loadData()
	{
		mFloorListAdapter = new DataArrayAdapter<Floor>(Globals.getInstance()
		                                                       .getApplicationContext(), R.layout.listviewitem, Globals.getInstance()
		                                                                                                               .getCCUApplication().floors);
		
		
		
	}
	
	
	public void saveData()
	{
		//Save
		LocalStorage.setApplicationSettings();
	}
}
