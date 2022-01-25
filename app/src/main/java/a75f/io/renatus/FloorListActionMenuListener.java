package a75f.io.renatus;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HSUtilKtKt;
import a75f.io.api.haystack.Zone;
import a75f.io.device.bacnet.BACnetUtils;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.modbusbox.EquipsManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FloorListActionMenuListener implements MultiChoiceModeListener
{
	
	final private FloorPlanFragment floorPlanActivity;
	private Menu             mMenu         = null;
	private ArrayList<Floor> selectedFloor = new ArrayList<Floor>();
	private Disposable deleteFloorDisposable;

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
		floorPlanActivity.mFloorListAdapter.setMultiSelectMode(true);

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
		if (floorPlanActivity.getContext() != null && floorPlanActivity.getUserVisibleHint()) {
			switch (item.getItemId()) {
				case R.id.deleteSelection:
					deleteSelectedFloors();
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
		} else {
			mode.finish();
			return false;
		}

	}

	@Override
	public void onDestroyActionMode(ActionMode mode)
	{
		floorPlanActivity.mFloorListAdapter.setMultiSelectMode(false);
		mMenu = null;
	}

	public void dispose() {
		if (deleteFloorDisposable != null) {
			deleteFloorDisposable.dispose();
		}
	}

	private void deleteSelectedFloors() {
		floorPlanActivity.showWait(floorPlanActivity.getString(R.string.deleting_floor));

		Completable task = Completable.fromCallable((Callable<Void>) () -> {
			deleteSelectedFloorsAsync();
			return null;
		});

		deleteFloorDisposable = task
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(
					() -> {
						selectedFloor.clear();
						floorPlanActivity.refreshScreen();
						floorPlanActivity.hideWait();
					},
					error -> {
						Toast.makeText(floorPlanActivity.requireContext(),
								"Error deleting floor",
								Toast.LENGTH_LONG)
							 .show();
						floorPlanActivity.hideWait();
						CcuLog.w("CCU_UI", "Unable to delete floor", error);
					});
	}
	
	private void deleteSelectedFloorsAsync()
	{
		for (int nCount = 0; nCount < selectedFloor.size(); nCount++)
		{
			Floor floor = selectedFloor.get(nCount);
			String floorId = floor.getId();
			for (Zone sZone: HSUtil.getZones(floorId))
			{
				for (Device d : HSUtil.getDevices(sZone.getId())) {
					BACnetUtils.removeModule(Short.parseShort(d.getAddr()));
					L.removeHSDeviceEntities(Short.parseShort(d.getAddr()));
				}
                ArrayList<HashMap> schedules = CCUHsApi.getInstance().readAll("schedule and roomRef == "+ sZone.getId() );
                for (HashMap schedule : schedules)
                {
                    CCUHsApi.getInstance().deleteEntity(schedule.get("id").toString());
                }
				CCUHsApi.getInstance().deleteEntity(sZone.getId());
			}
			boolean usedByOtherCcu = HSUtilKtKt.isFloorUsedByOtherCcuAsync(floorId);
			if (usedByOtherCcu) {
				CCUHsApi.getInstance().deleteFloorEntityTreeLeavingRemoteFloorIntact(floorId);
			} else {
				CCUHsApi.getInstance().deleteEntityTree(floorId);
			}
			EquipsManager.getInstance().deleteEquipsByFloor(floor.getId());
		}
		CCUHsApi.getInstance().syncEntityTree();
		L.saveCCUState();
		// continue what you are doing...
		floorPlanActivity.getBuildingFloorsZones("");
	}
	
	
	private void renameSelectedFloor()
	{
		////selectedFloor.get(0).mFloorName = selectedFloor.get(0).mFloorName;
		floorPlanActivity.renameFloor(selectedFloor.get(0));
		floorPlanActivity.getBuildingFloorsZones("");
	}
	
	
	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
	{
		Floor floorData = floorPlanActivity.mFloorListAdapter.getItem(position);
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
				mode.setSubtitle("" + checkedCount + " floors selected");
				mMenu.findItem(R.id.renameSelection).setVisible(false);
				break;
		}
	}
}
