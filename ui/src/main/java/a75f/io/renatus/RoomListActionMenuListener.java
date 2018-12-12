package a75f.io.renatus;

import android.os.AsyncTask;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;

import java.util.ArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.logic.L;


class RoomListActionMenuListener implements MultiChoiceModeListener
{
	
	/**
	 *
	 */
	final private FloorPlanFragment floorPlanActivity;
	private Menu            mMenu        = null;
	private ArrayList<Zone> selectedRoom = new ArrayList<Zone>();
	
	
	/**
	 * @param floorPlanFragment
	 */
	RoomListActionMenuListener(FloorPlanFragment floorPlanFragment)
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
		selectedRoom.clear();
		mode.setTitle("Select Rooms");
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
				deleteSelectedRooms();
				selectedRoom.clear();
				mode.finish(); // Action picked, so close the CAB
				return true;
			case R.id.renameSelection:
				renameSelectedRoom();
				selectedRoom.clear();
				mode.finish(); // Action picked, so close the CAB
				return true;
			case R.id.restartSelection:
				restartSelectedRoomModules();
				selectedRoom.clear();
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
		selectedRoom.clear();
		mMenu = null;
	}
	
	
	private void deleteSelectedRooms()
	{
		for (int nCount = 0; nCount < selectedRoom.size(); nCount++)
		{
			Zone sZone = selectedRoom.get(nCount);
			for (Device d : HSUtil.getDevices(sZone.getId())) {
				L.removeHSDeviceEntities(Short.parseShort(d.getAddr()));
			}
			CCUHsApi.getInstance().deleteEntity(sZone.getId());
			floorPlanActivity.refreshScreen();
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
	
	
	private void renameSelectedRoom()
	{
		//L.ccu().getFloors().get(0).mZoneList.remove(selectedRoom.get(0));
		L.saveCCUState();
		floorPlanActivity.refreshScreen();
	}
	
	
	private void restartSelectedRoomModules()
	{
		/*for (int nCount = 0; nCount < selectedRoom.size(); nCount++)
		{
			RoomData roomData = selectedRoom.get(nCount);
			for (FSVData fsv : roomData.getFSVData())
				fsv.sendSettingsToDevice(false, true);
		}*/
	}
	
	
	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
	{
		Zone roomData = (Zone) floorPlanActivity.mRoomListAdapter.getItem(position);
		if (roomData == null)
		{
			return;
		}
		if (checked)
		{
			floorPlanActivity.mRoomListAdapter.addSelected(position);
			selectedRoom.add(roomData);
		}
		else
		{
			floorPlanActivity.mRoomListAdapter.removeSelected(position);
			selectedRoom.remove(roomData);
		}
		final int checkedCount = selectedRoom.size();
		switch (checkedCount)
		{
			case 0:
				mode.setSubtitle(null);
				break;
			case 1:
				mode.setSubtitle("One room selected");
				mMenu.findItem(R.id.renameSelection).setVisible(true);
				break;
			default:
				mode.setSubtitle("" + checkedCount + " rooms selected");
				mMenu.findItem(R.id.renameSelection).setVisible(false);
				break;
		}
		//mMenu.findItem(R.id.restartSelection).setVisible(((checkedCount > 0) && (CCULicensing.getHandle().UseDevMode() *//*SystemSettingsData.getTier() == CCU_TIER.DEV*//*)));
		floorPlanActivity.mRoomListAdapter.notifyDataSetChanged();
	}
}