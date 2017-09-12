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
		FloorContainer.getInstance().getFloorListAdapter().setMultiSelectMode(true);
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
		FloorContainer.getInstance().getFloorListAdapter().setMultiSelectMode(false);
		selectedFloor.clear();
		mMenu = null;
	}
	
	
	private void deleteSelectedFloors()
	{
		for (int nCount = 0; nCount < selectedFloor.size(); nCount++)
		{
			Floor floorData = selectedFloor.get(nCount);
			FloorContainer.getInstance().deleteFloor(floorData);
		}
	}
	
	
	private void renameSelectedFloor()
	{
		//floorPlanActivity.renameFloor(selectedFloor.get(0));
	}
	
	
	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
	{
		Floor floorData = (Floor) FloorContainer.getInstance().getFloorList().get(position);
		if (floorData == null)
		{
			return;
		}
		if (checked)
		{
			FloorContainer.getInstance().getFloorListAdapter().addSelected(position);
			selectedFloor.add(floorData);
		}
		else
		{
			FloorContainer.getInstance().getFloorListAdapter().removeSelected(position);
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
