package a75f.io.renatus;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;

import java.util.ArrayList;

import a75f.io.bo.building.Floor;

public class FloorListActionMenuListener implements MultiChoiceModeListener
{
	
	final private FloorPlanFragment floorPlanActivity;
	private Menu mMenu = null;
	private ArrayList<Floor> selectedFloor = new ArrayList<Floor>();
	
	
	/**
	 * @param floorPlanFragment
	 */
	FloorListActionMenuListener(FloorPlanFragment floorPlanFragment)
	{
		this.floorPlanActivity = floorPlanFragment;
	}
	
	
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu)
	{
		floorPlanActivity.mRoomListAdapter.setMultiSelectMode(true);
		
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.action_menu, menu);
		mMenu = menu;
		selectedFloor.clear();
		mode.setTitle("Select Floors");
		return true;
	}
	
	
	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu)
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	
	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.deleteSelection:
				deleteSelectedFloors();
				selectedFloor.clear();
				mode.finish(); // Action picked, so close the CAB
				return true;
			case R.id.renameSelection:
				renameSelectedFloor();
				selectedFloor.clear();
				mode.finish(); // Action picked, so close the CAB
				return true;
			default:
				return false;
		}
	}
	
	
	@Override
	public void onDestroyActionMode(ActionMode mode)
	{
		
		floorPlanActivity.mRoomListAdapter.setMultiSelectMode(false);
		selectedFloor.clear();
		mMenu = null;
	}
	
	
	private void deleteSelectedFloors()
	{
		for (int nCount = 0; nCount < selectedFloor.size(); nCount++)
		{
			Floor floorData = selectedFloor.get(nCount);
			floorPlanActivity.ccuApplication.floors.remove(floorData);
		}
	}
	
	
	private void renameSelectedFloor()
	{
		//selectedFloor.get(0).mFloorName = selectedFloor.get(0).mFloorName;
		//floorPlanActivity.renameFloor(selectedFloor.get(0));
	}
	
	
	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
	{
		Floor floorData = (Floor) floorPlanActivity.ccuApplication.floors.get(position);
		if (floorData == null)
		{
			return;
		}
		if (checked)
		{
			floorPlanActivity.mFloorListAdapter.addSelected(position);
			selectedFloor.add(floorData);
		}
		else
		{
			floorPlanActivity.mFloorListAdapter.removeSelected(position);
			selectedFloor.remove(floorData);
		}
		final int checkedCount = selectedFloor.size();
		switch (checkedCount)
		{
			case 0:
				mode.setSubtitle(null);
				break;
			case 1:
				mode.setSubtitle("One floor selected");
				mMenu.findItem(R.id.renameSelection).setVisible(true);
				break;
			default:
				mode.setSubtitle("" + checkedCount + " floor selected");
				mMenu.findItem(R.id.renameSelection).setVisible(false);
				break;
		}
	}
}
