package a75f.io.renatus;

import android.os.AsyncTask;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.Toast;

import java.util.ArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.L;

class ModuleListActionMenuListener implements MultiChoiceModeListener
{
	
	/**
	 *
	 */
	private final FloorPlanFragment floorPlanActivity;
	private ArrayList<Short> seletedModules = new ArrayList<>();
	
	
	/**
	 * @param floorPlanFragment
	 */
	ModuleListActionMenuListener(FloorPlanFragment floorPlanFragment)
	{
		this.floorPlanActivity = floorPlanFragment;
	}
	
	
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu)
	{
		floorPlanActivity.mModuleListAdapter.setMultiSelectMode(true);
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.action_menu, menu);
		menu.findItem(R.id.renameSelection).setVisible(false);
		seletedModules.clear();
		mode.setTitle("Select Modules");
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
				if (true)
				{ //TODO check prefconfigured profiels
					
					deleteSelectedFSV();
					seletedModules.clear();
                    floorPlanActivity.refreshScreen();
					mode.finish(); // Action picked, so close the CAB
                    
				}
				else
				{
					Toast.makeText(this.floorPlanActivity
							               .getActivity(), "Cannot delete pre configured module", Toast.LENGTH_SHORT)
					     .show();
					mode.finish();
				}
				return true;
			default:
				return false;
		}
	}
	
	
	@Override
	public void onDestroyActionMode(ActionMode mode)
	{
		floorPlanActivity.mModuleListAdapter.setMultiSelectMode(false);
		seletedModules.clear();
	}
	
	
	private void deleteSelectedFSV()
	{
		for(Short selectedModule : seletedModules)
		{
			L.removeHSDeviceEntities(selectedModule);
		}
		
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground( final Void ... params ) {
				CCUHsApi.getInstance().syncEntityTree();
				L.saveCCUState();
				return null;
			}
			
			@Override
			protected void onPostExecute( final Void result ) {
				// continue what you are doing...
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
		
    }
	
	
	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
	{
		Short smartNodeID = Short.parseShort(floorPlanActivity.mModuleListAdapter.getItem(position));
		if (checked)
		{
			seletedModules.add(smartNodeID);
			floorPlanActivity.mModuleListAdapter.addSelected(position);
		}
		else
		{
			seletedModules.remove(smartNodeID);
			floorPlanActivity.mModuleListAdapter.removeSelected(position);
		}
		final int checkedCount = seletedModules.size();
		switch (checkedCount)
		{
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
}