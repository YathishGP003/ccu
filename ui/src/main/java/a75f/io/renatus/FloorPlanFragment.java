package a75f.io.renatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;

import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.Floor;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.L;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import butterknife.OnItemClick;

import static a75f.io.logic.L.ccu;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class FloorPlanFragment extends Fragment
{
	public static final String ACTION_BLE_PAIRING_COMPLETED =
			"a75f.io.renatus.BLE_PAIRING_COMPLETED";
	public DataArrayAdapter<Floor> mFloorListAdapter;
	public DataArrayAdapter<Zone>  mRoomListAdapter;
	public DataArrayAdapter<Short> mModuleListAdapter;
	
	@BindView(R.id.addFloorBtn)
	TextView addFloorBtn;
	@BindView(R.id.addRoomBtn)
	TextView addRoomBtn;
	@BindView(R.id.pairModuleBtn)
	TextView pairModuleBtn;
	@BindView(R.id.addFloorEdit)
	EditText    addFloorEdit;
	@BindView(R.id.addRoomEdit)
	EditText    addRoomEdit;
	@BindView(R.id.addModuleEdit)
	EditText    addModuleEdit;
	@BindView(R.id.floorList)
	ListView    floorListView;
	@BindView(R.id.roomList)
	ListView    roomListView;
	@BindView(R.id.moduleList)
	ListView    moduleListView;
	Short[] smartNodeAddresses;
	private final BroadcastReceiver mPairingReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			switch (intent.getAction())
			{
				
				case ACTION_BLE_PAIRING_COMPLETED:
					updateModules(getSelectedZone());
					getActivity().unregisterReceiver(mPairingReceiver);
					break;
			}
		}
	};
	
	
	public FloorPlanFragment()
	{
	}
	
	
	public static FloorPlanFragment newInstance()
	{
		return new FloorPlanFragment();
	}
	
	
	private Zone getSelectedZone()
	{
		
		return getSelectedFloor().mZoneList.get(mRoomListAdapter.getSelectedPostion());
	}
	
	
	private Floor getSelectedFloor()
	{
		return ccu().getFloors().get(mFloorListAdapter.getSelectedPostion());
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.floorplan, container, false);
		ButterKnife.bind(this, rootView);
		return rootView;
	}
	
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		enableFloorButton();
		disableRoomModule();
	}
	
	
	@Override
	public void onStart()
	{
		super.onStart();
		floorListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		floorListView.setMultiChoiceModeListener(new FloorListActionMenuListener(this));
		roomListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		roomListView.setMultiChoiceModeListener(new RoomListActionMenuListener(this));
		moduleListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		moduleListView.setMultiChoiceModeListener(new ModuleListActionMenuListener(this));
		//EventBus.getDefault().register();
	}
	
	
	@Override
	public void onResume()
	{
		super.onResume();
		refreshScreen();
	}
	
	
	@Override
	public void onPause()
	{
		super.onPause();
		saveData();
	}
	
	
	public void saveData()
	{
		//Save
		L.saveCCUState();
		
	}
	
	
	public void refreshScreen()
	{
		
		updateFloors();
	}
	
	
	private void updateFloors()
	{
		mFloorListAdapter =
				new DataArrayAdapter<Floor>(this.getActivity(), R.layout.listviewitem, ccu().getFloors());
		floorListView.setAdapter(mFloorListAdapter);
		enableFloorButton();
		if (mFloorListAdapter.getCount() > 0)
		{
			selectFloor(0);
			enableRoomBtn();
		}
		else
		{
			if (mRoomListAdapter != null)
			{
				mRoomListAdapter.clear();
			}
			if (mModuleListAdapter != null)
			{
				mModuleListAdapter.clear();
			}
			disableRoomModule();
		}
	}
	
	
	private void selectFloor(int position)
	{
		mFloorListAdapter.setSelectedItem(position);
		Floor curSelectedFloor = ccu().getFloors().get(position);
		updateRooms(curSelectedFloor.mZoneList);
	}
	
	
	//
	private void enableRoomBtn()
	{
		addRoomBtn.setVisibility(View.VISIBLE);
		addRoomEdit.setVisibility(View.INVISIBLE);
	}
	
	
	private void updateRooms(ArrayList<Zone> zones)
	{
		mRoomListAdapter = new DataArrayAdapter<>(this.getActivity(), R.layout.listviewitem, zones);
		roomListView.setAdapter(mRoomListAdapter);
		enableRoomBtn();
		if (mRoomListAdapter.getCount() > 0)
		{
			selectRoom(0);
			enableModueButton();
		}
		else
		{
			if (mModuleListAdapter != null)
			{
				/*mModuleListAdapter = new DataArrayAdapter<Short>(this.getActivity(), R
						                                                                     .layout.listviewitem, new Short[]{});
				moduleListView.setAdapter(mModuleListAdapter);*/
				mModuleListAdapter.clear();
				
			}
			disableModuButton();
		}
	}
	
	
	private void selectRoom(int position)
	{
		mRoomListAdapter.setSelectedItem(position);
		Floor floor = ccu().getFloors().get(mFloorListAdapter.getSelectedPostion());
		Zone selectedZone = floor.mZoneList.get(mRoomListAdapter.getSelectedPostion());
		updateModules(selectedZone);
	}
	
	
	private void enableModueButton()
	{
		pairModuleBtn.setVisibility(View.VISIBLE);
	}
	
	
	private void disableModuButton()
	{
		pairModuleBtn.setVisibility(View.INVISIBLE);
	}
	
	
	private void updateModules(Zone zone)
	{
		mModuleListAdapter =
				new DataArrayAdapter<>(getActivity(), R.layout.listviewitem, createIntegerList(
						zone.getNodes()));
		moduleListView.setAdapter(mModuleListAdapter);
	}
	
	private ArrayList<Short> createIntegerList(HashSet<Short> nodes)
	{
		ArrayList<Short> arrayList = new ArrayList<>();
		 
		for(Short address : nodes)
		{
			arrayList.add(address);
			
		}
		return arrayList;
	}
	
	private void enableFloorButton()
	{
		addFloorBtn.setVisibility(View.VISIBLE);
		addFloorEdit.setVisibility(View.INVISIBLE);
	}
	
	
	private void disableRoomModule()
	{
		addRoomBtn.setVisibility(View.INVISIBLE);
		addRoomEdit.setVisibility(View.INVISIBLE);
		pairModuleBtn.setVisibility(View.INVISIBLE);
		addModuleEdit.setVisibility(View.INVISIBLE);
	}
	
	
	@OnClick(R.id.addFloorBtn)
	public void handleFloorBtn()
	{
		enableFloorEdit();
		addFloorEdit.setText("");
		addFloorEdit.requestFocus();
		InputMethodManager mgr =
				(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.showSoftInput(addFloorEdit, InputMethodManager.SHOW_IMPLICIT);
	}
	
	
	private void enableFloorEdit()
	{
		addFloorBtn.setVisibility(View.INVISIBLE);
		addFloorEdit.setVisibility(View.VISIBLE);
	}
	
	
	@OnEditorAction(R.id.addFloorEdit)
	public boolean handleFloorChange(TextView v, int actionId, KeyEvent event)
	{
		if (actionId == EditorInfo.IME_ACTION_DONE)
		{
			int fID = ccu().getFloors().size();
			ccu().getFloors().add(new Floor(fID, "", addFloorEdit.getText().toString()));
			updateFloors();
			selectFloor(fID);
			//CCUKinveyInterface.updateFloor(FloorData.getFloorData().get(nID), false);
			InputMethodManager mgr = (InputMethodManager) getActivity()
					                                              .getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(addFloorEdit.getWindowToken(), 0);
			Toast.makeText(getActivity().getApplicationContext(),
					"Floor " + addFloorEdit.getText() + " added", Toast.LENGTH_SHORT).show();
			return true;
		}
		return false;
	}
	
	
	@OnFocusChange(R.id.addFloorEdit)
	public void handleFloorFocus(View v, boolean hasFocus)
	{
		if (!hasFocus)
		{
			enableFloorButton();
		}
	}
	
	
	@OnClick(R.id.addRoomBtn)
	public void handleRoomBtn()
	{
		enableRoomEdit();
		addRoomEdit.setText("");
		addRoomEdit.requestFocus();
		InputMethodManager mgr =
				(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.showSoftInput(addRoomEdit, InputMethodManager.SHOW_IMPLICIT);
	}
	
	
	private void enableRoomEdit()
	{
		addRoomBtn.setVisibility(View.INVISIBLE);
		addRoomEdit.setVisibility(View.VISIBLE);
	}
	
	
	@OnFocusChange(R.id.addRoomEdit)
	public void handleRoomFocus(View v, boolean hasFocus)
	{
		if (!hasFocus)
		{
			enableRoomBtn();
		}
	}
	
	
	@OnEditorAction(R.id.addRoomEdit)
	public boolean handleRoomChange(TextView v, int actionId, KeyEvent event)
	{
		if (actionId == EditorInfo.IME_ACTION_DONE)
		{
			Toast.makeText(getActivity().getApplicationContext(),
					"Room " + addRoomEdit.getText() + " added", Toast.LENGTH_SHORT).show();
			Floor f = L.ccu().getFloors().get(mFloorListAdapter.getSelectedPostion());
			ArrayList<Zone> mRoomList =f.mZoneList;
			mRoomList.add(new Zone(addRoomEdit.getText().toString(), f));
			updateRooms(mRoomList);
			selectRoom(mRoomList.size() - 1);
			InputMethodManager mgr = (InputMethodManager) getActivity()
					                                              .getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(addRoomEdit.getWindowToken(), 0);
			return true;
		}
		return false;
	}

	@OnClick(R.id.pairModuleBtn)
	public void startPairing()
	{
		//TODO - TEMP
		ccu().setSmartNodeAddressBand((short)7000);
		
		short meshAddress =L.generateSmartNodeAddress();
		Floor floor = ccu().getFloors().get(mFloorListAdapter.getSelectedPostion());
		Zone room = floor.mZoneList.get(mRoomListAdapter.getSelectedPostion());
		
		/* Checks to see if emulated and doesn't popup BLE dialogs */

		//This should be moved to pair button for select device type screen.
		showDialogFragment(FragmentSelectDeviceType.newInstance(meshAddress, room.roomName, floor.mFloorName), FragmentSelectDeviceType.ID);
		/*
		*/
	}
	
	
	private void showDialogFragment(DialogFragment dialogFragment, String id)
	{
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag(id);
		if (prev != null)
		{
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		//TODO: no broadcast recievers
		getActivity()
				.registerReceiver(mPairingReceiver, new IntentFilter(ACTION_BLE_PAIRING_COMPLETED));
		// Create and show the dialog.
		dialogFragment.show(ft, id);
	}
	
	
	@OnItemClick(R.id.floorList)
	public void setFloorListView(AdapterView<?> parent, View view, int position, long id)
	{
		selectFloor(position);
	}
	
	
	@OnItemClick(R.id.roomList)
	public void setRoomListView(AdapterView<?> parent, View view, int position, long id)
	{
		selectRoom(position);
	}
	
	
	@OnItemClick(R.id.moduleList)
	public void setModuleListView(AdapterView<?> parent, View view, int position, long id)
	{
		selectModule(position);
	}
	
	
	private void selectModule(int position)
	{
		Floor floor = ccu().getFloors().get(mFloorListAdapter.getSelectedPostion());
		Zone zone = floor.mZoneList.get(mRoomListAdapter.getSelectedPostion());
		Short nodeAddr = mModuleListAdapter.getItem(position);
		
		
//		DO NOTHING
//		showDialogFragment(LightingZoneProfileFragment
//				                   .newInstance((Short)createIntegerList(zone.getNodes()).get(position),
//										   zone.roomName, mfloor.mFloorName),
//				LightingZoneProfileFragment.ID);
		
		ZoneProfile profile = getProfile(zone, nodeAddr);
		BaseProfileConfiguration config = profile.getProfileConfiguration().get(nodeAddr);
		switch (profile.getProfileType()) {
			case HMP:
				showDialogFragment(FragmentHMPConfiguration
						                   .newInstance(nodeAddr,getSelectedZone().roomName, config.getNodeType(), getSelectedFloor().mFloorName), FragmentHMPConfiguration.ID);
				break;
			case VAV_REHEAT:
			case VAV_SERIES_FAN:
			case VAV_PARALLEL_FAN:
				showDialogFragment(FragmentVAVConfiguration
						                   .newInstance(nodeAddr,getSelectedZone().roomName, config.getNodeType(), getSelectedFloor().mFloorName, profile.getProfileType()), FragmentVAVConfiguration.ID);
			
		}
		
		
	}
	
	
	//TODO - Revisit
	private ZoneProfile getProfile(Zone zone ,Short addr) {
		
		for (ZoneProfile zp : zone.mZoneProfiles) {
			
			BaseProfileConfiguration profConfig = zp.getProfileConfiguration().get(addr);
			if (profConfig != null) {
				return zp;
			}
			
		}
		return null;
	}
}
