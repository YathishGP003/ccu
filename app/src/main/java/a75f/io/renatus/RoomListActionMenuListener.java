package a75f.io.renatus;

import android.annotation.SuppressLint;
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
import a75f.io.logic.util.bacnet.BacnetUtilKt;
import a75f.io.util.ExecutorTask;


class RoomListActionMenuListener implements MultiChoiceModeListener
{
	
	/**
	 *
	 */
	final private FloorPlanFragment floorPlanActivity;
	private Menu            mMenu        = null;
	private ActionMode aMode = null ;
	public ArrayList<Zone> selectedRoom = new ArrayList<Zone>();
	private static final String INTENT_ZONE_DELETED = "a75f.io.renatus.ZONE_DELETED";
	
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
		menu.findItem(R.id.copyConfiguration).setVisible(false);
		menu.findItem(R.id.divider).setVisible(false);
		mMenu = menu;
		selectedRoom.clear();
		aMode = mode;
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
		if (floorPlanActivity.getContext() != null && floorPlanActivity.getUserVisibleHint() ) {
			switch (item.getItemId()) {
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
		} else {
			mode.finish();
			return false;
		}

	}
	
	
	@Override
	public void onDestroyActionMode(ActionMode mode)
	{
		floorPlanActivity.mRoomListAdapter.setMultiSelectMode(false);
		selectedRoom.clear();
		mMenu = null;
		aMode = null;
	}
	
	
	@SuppressLint("StaticFieldLeak")
	private void deleteSelectedRooms()
	{
		for (int nCount = 0; nCount < selectedRoom.size(); nCount++)
		{
			Zone sZone = selectedRoom.get(nCount);
			for (Device d : HSUtil.getDevices(sZone.getId())) {

				//Todo Notifies to BACnet Data Layer
				L.removeHSDeviceEntities(Long.parseLong(d.getAddr()));
			}
			CCUHsApi.getInstance().deleteEntityTree(sZone.getId());
			CCUHsApi.getInstance().saveTagsData();
			floorPlanActivity.refreshScreen();
			BacnetUtilKt.sendBroadCast(floorPlanActivity.requireContext(),
					INTENT_ZONE_DELETED,
					sZone.getId());
		}
		ExecutorTask.executeAsync(
			() -> {
                CCUHsApi.getInstance().syncEntityTree();
                L.saveCCUState();
		    },
			()-> floorPlanActivity.getBuildingFloorsZones("")

		);
	}
	
	
	private void renameSelectedRoom()
	{
		L.saveCCUState();
		for (int nCount = 0; nCount < selectedRoom.size(); nCount++)
		{
			Zone sZone = selectedRoom.get(nCount);
			floorPlanActivity.renameZone(sZone);
		}
		floorPlanActivity.getBuildingFloorsZones("");
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
		if (checked && !isRoomSelected(roomData))
		{
			selectedRoom.add(roomData);
			floorPlanActivity.mRoomListAdapter.addSelected(position, new ArrayList<>(), selectedRoom);
		}
		else
		{
			removeRoomSelected(roomData);
			floorPlanActivity.mRoomListAdapter.removeSelected(position, new ArrayList<>(), selectedRoom);
		}
		if(selectedRoom.size() == 0)
		{
			mode.finish();
		}
		floorPlanActivity.mRoomListAdapter.notifyDataSetChanged();
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

	private Boolean isRoomSelected(Zone roomData)
	{
		if(selectedRoom.isEmpty()) {
			return false;
		}
		for(Zone room : selectedRoom) {
			if (room.getDisplayName().equals(roomData.getDisplayName())) {
				return true;
			}
		}
		return false;
	}
	private void removeRoomSelected(Zone roomData) {
		//This method is used to remove the selected room from the selectedRoom list by index
		// default remove method not working because of object structure changed
		for(int i = 0; i < selectedRoom.size(); i++) {
			if (selectedRoom.get(i).getDisplayName().equals(roomData.getDisplayName())) {
				selectedRoom.remove(i);
			}
		}
	}

	public void destroyActionBar() {
		if (aMode != null) {
			aMode.finish();
		}
	}
}