package a75f.io.renatus;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.Toast;

import java.util.ArrayList;

import a75f.io.bo.building.ZoneProfile;

class ModuleListActionMenuListener implements MultiChoiceModeListener {

	/**
	 * 
	 */
	private final FloorPlanFragment floorPlanActivity;

	/**
	 * @param floorPlanFragment
	 */
	ModuleListActionMenuListener(FloorPlanFragment floorPlanFragment) {
		this.floorPlanActivity = floorPlanFragment;
	}

	private ArrayList<ZoneProfile> seletedModules = new ArrayList<ZoneProfile>();
	
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		floorPlanActivity.mModuleListAdapter.setMultiSelectMode(true);
		MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        menu.findItem(R.id.renameSelection).setVisible(false);
		seletedModules.clear();
        mode.setTitle("Select Modules");
        return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.deleteSelection:
				if (true) { //TODO check prefconfigured profiels
					deleteSelectedFSV();
					seletedModules.clear();
					mode.finish(); // Action picked, so close the CAB
				}else{
					Toast.makeText(this.floorPlanActivity.getActivity(),"Cannot delete pre configured module",Toast.LENGTH_SHORT).show();
					mode.finish();
				}
            return true;
        default:
            return false;
		}
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		floorPlanActivity.mModuleListAdapter.setMultiSelectMode(false);
		seletedModules.clear();
		
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position,
			long id, boolean checked) {
		ZoneProfile profile = floorPlanActivity.mModuleListAdapter.getItem(position);
		
		if (checked)
		{
			seletedModules.add(profile);
			floorPlanActivity.mModuleListAdapter.addSelected(position);
		}
		else
		{
			seletedModules.remove(profile);
			floorPlanActivity.mModuleListAdapter.removeSelected(position);
		}
		final int checkedCount = seletedModules.size();
        switch (checkedCount) {
            case 0:
                mode.setSubtitle(null);
                break;
            case 1:
                mode.setSubtitle("One module selected");
                break;
            default:
                mode.setSubtitle("" + checkedCount + " modules selected");
                break;
        }
	}
	
	private void deleteSelectedFSV() {
		ArrayList<ZoneProfile> profList = FloorContainer.getInstance().getFloorList().get(floorPlanActivity.mCurFloorIndex)
				                           .mRoomList.get(floorPlanActivity.mCurRoomIndex).zoneProfiles;
		for (int nCount = 0; nCount < seletedModules.size(); nCount++)
		{
			ZoneProfile p = seletedModules.get(nCount);
			if(p != null) profList.remove(p);
		}
	}
	
}