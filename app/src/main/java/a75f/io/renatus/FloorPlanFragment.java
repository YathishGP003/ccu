package a75f.io.renatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.device.mesh.LSerial;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import butterknife.OnItemClick;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class FloorPlanFragment extends Fragment
{
	public static final String ACTION_BLE_PAIRING_COMPLETED =
			"a75f.io.renatus.BLE_PAIRING_COMPLETED";
	public DataArrayAdapter<Floor> mFloorListAdapter;
	public DataArrayAdapter<Zone>  mRoomListAdapter;
	public DataArrayAdapter<String>                      mModuleListAdapter;
	
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

	@BindView(R.id.lt_addfloor)
	LinearLayout addFloorlt;
	@BindView(R.id.lt_addzone)
	LinearLayout    addZonelt;
	@BindView(R.id.lt_addModule)
	LinearLayout    addModulelt;

	@BindView(R.id.rl_systemdevice)
	RelativeLayout rl_systemdevice;
	@BindView(R.id.rl_oao)
	RelativeLayout rl_oao;

	@BindView(R.id.textSystemDevice)
	TextView textViewSystemDevice;
	@BindView(R.id.textOAO)
	TextView textViewOAO;

	ArrayList<Floor> floorList = new ArrayList();
	ArrayList<Zone> roomList = new ArrayList();
	private final BroadcastReceiver mPairingReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			switch (intent.getAction())
			{
				
				case ACTION_BLE_PAIRING_COMPLETED:
					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							if(LSerial.getInstance().isConnected()) //If usb connected and pairing done then reseed
								LSerial.getInstance().setResetSeedMessage(true);
							try{
								if (mRoomListAdapter == null || mRoomListAdapter.getSelectedPostion() == -1) {
									updateOAOModule();
								}else
								{
									updateModules(getSelectedZone());
								}
								//Crash here because of activity null while moving to other fragment and return back here after edit config
								if((getActivity() != null) && (mPairingReceiver != null))
								getActivity().unregisterReceiver(mPairingReceiver);

							}catch (Exception e){
								e.printStackTrace();
							}
						}
					}).start();
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
		return roomList.get(mRoomListAdapter.getSelectedPostion());
	}
	
	
	private Floor getSelectedFloor()
	{
		return floorList.get(mFloorListAdapter.getSelectedPostion());
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
		floorList = HSUtil.getFloors();
		Collections.sort(floorList, new FloorComparator());
		updateFloors();
	}
	
	
	private void updateFloors()
	{
		
		mFloorListAdapter = new DataArrayAdapter<>(this.getActivity(), R.layout.listviewitem,floorList);
		//mFloorListAdapter = new DataArrayAdapter<>(this.getActivity(), R.id.textData,floorList);
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
			//disableRoomModule();
		}

		setSytemUnselection();
		addFloorBtn.setEnabled(true);
		addZonelt.setEnabled(true);

	}
	
	
	private void selectFloor(int position)
	{
		mFloorListAdapter.setSelectedItem(position);
		roomList = HSUtil.getZones(getSelectedFloor().getId());
		Collections.sort(roomList, new ZoneComparator());
		updateRooms(roomList);
		
	}
	
	
	//
	private void enableRoomBtn()
	{
		addZonelt.setVisibility(View.VISIBLE);
		addRoomBtn.setVisibility(View.VISIBLE);
		addRoomEdit.setVisibility(View.INVISIBLE);
	}
	
	
	private void updateRooms(ArrayList<Zone> zones)
	{
		mRoomListAdapter = new DataArrayAdapter<>(this.getActivity(), R.layout.listviewitem,zones);
		//mRoomListAdapter = new DataArrayAdapter<>(this.getActivity(), R.id.textData,zones);
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
		updateModules(getSelectedZone());
	}
	
	
	private void enableModueButton()
	{
		addModulelt.setVisibility(View.VISIBLE);
		pairModuleBtn.setVisibility(View.VISIBLE);
	}
	
	
	private void disableModuButton()
	{
		addModulelt.setVisibility(View.INVISIBLE);
		pairModuleBtn.setVisibility(View.INVISIBLE);
	}
	
	
	private boolean updateOAOModule() {
		ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and oao");
		ArrayList<Equip> equipList = new ArrayList<>();
		for (HashMap m : equips)
		{
			equipList.add(new Equip.Builder().setHashMap(m).build());
		}
		
		if(equipList != null && (equipList.size() > 0)) {
			Log.d("CCU","Show OAO Equip ");
			mModuleListAdapter = new DataArrayAdapter<>(FloorPlanFragment.this.getActivity(), R.layout.listviewitem,createAddressList(equipList));
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					moduleListView.setAdapter(mModuleListAdapter);
				}
			});
			return true;
		} else {
			moduleListView.setAdapter(null);
			Log.d("CCU","Show Equip does not exist ");
			return false;
		}
		
	}
	private void updateModules(Zone zone)
	{
		Log.d("CCU","Zone Selected "+zone.getDisplayName());
		ArrayList<Equip> zoneEquips  = HSUtil.getEquips(zone.getId());
		if(zoneEquips != null && (zoneEquips.size() > 0)) {
			mModuleListAdapter = new DataArrayAdapter<>(FloorPlanFragment.this.getActivity(), R.layout.listviewitem,createAddressList(zoneEquips));
			//mModuleListAdapter = new DataArrayAdapter<>(FloorPlanFragment.this.getActivity(), R.id.textData,createAddressList(zoneEquips));

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					moduleListView.setAdapter(mModuleListAdapter);
				}
			});
		} else {
			moduleListView.setAdapter(null);
		}
	}
	
	private ArrayList<String> createAddressList(ArrayList<Equip> equips)
	{
		Collections.sort(equips, new ModuleComparator());
		ArrayList<String> arrayList = new ArrayList<>();
		 
		for(Equip e : equips)
		{
			arrayList.add(e.getGroup());
			
		}
		return arrayList;
	}
	
	private void enableFloorButton()
	{
		addFloorlt.setVisibility(View.VISIBLE);
		addFloorBtn.setVisibility(View.VISIBLE);
		addFloorEdit.setVisibility(View.INVISIBLE);
	}
	
	
	private void disableRoomModule()
	{
		addFloorlt.setVisibility(View.INVISIBLE);
		addZonelt.setVisibility(View.INVISIBLE);
		addModulelt.setVisibility(View.INVISIBLE);

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

	@OnClick(R.id.lt_addfloor)
	public void addFloorBtn()
	{
		enableFloorEdit();
		addFloorEdit.setText("");
		addFloorEdit.requestFocus();
		InputMethodManager mgr =
				(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.showSoftInput(addFloorEdit, InputMethodManager.SHOW_IMPLICIT);
	}
	


	@OnClick(R.id.rl_systemdevice)
	public void systemDeviceOnClick()
	{

		setSytemSelection();
		if(floorList.size()>0) {
			if(roomList.size()>0) {
				ArrayList<Equip> zoneEquips = HSUtil.getEquips(getSelectedZone().getId());
				if (zoneEquips.size() > 0 && !zoneEquips.isEmpty()) {
					mModuleListAdapter.setSelectedItem(-1);
				}
				mRoomListAdapter.setSelectedItem(-1);
			}
			addZonelt.setEnabled(false);
		}
		mFloorListAdapter.setSelectedItem(-1);
		rl_systemdevice.setEnabled(false);
		rl_oao.setEnabled(false);
		enableModueButton();
	}



	@OnClick(R.id.rl_oao)
	public void oaoOnClick()
	{
		setSytemSelection();
		if(floorList.size()>0) {
			if(roomList.size() >0) {
				ArrayList<Equip> zoneEquips = HSUtil.getEquips(getSelectedZone().getId());
				if (zoneEquips.size() > 0) {
					mModuleListAdapter.setSelectedItem(-1);
				}
				mRoomListAdapter.setSelectedItem(-1);
				addFloorBtn.setEnabled(false);
				addZonelt.setEnabled(false);
			}
		}
		mFloorListAdapter.setSelectedItem(-1);
		if (updateOAOModule())
		{
			moduleListView.setVisibility(View.VISIBLE);
		} else {
			enableModueButton();
		}
	}

	private void setSytemSelection()
	{
		rl_systemdevice.setBackground(getResources().getDrawable(R.drawable.ic_listselector));
		rl_oao.setBackground(getResources().getDrawable(R.drawable.ic_listselector));
		textViewSystemDevice.setTextColor(Color.WHITE);
		textViewOAO.setTextColor(Color.WHITE);
		rl_oao.setEnabled(false);
		rl_systemdevice.setEnabled(false);
		roomListView.setVisibility(View.GONE);
		moduleListView.setVisibility(View.GONE);
	}
	private void setSytemUnselection()
	{
		rl_systemdevice.setBackgroundColor(Color.WHITE);
		rl_oao.setBackgroundColor(Color.WHITE);
		textViewSystemDevice.setTextColor(getContext().getResources().getColor(R.color.text_color));
		textViewOAO.setTextColor(getContext().getResources().getColor(R.color.text_color));
		rl_systemdevice.setEnabled(true);
		rl_oao.setEnabled(true);
		roomListView.setVisibility(View.VISIBLE);
		moduleListView.setVisibility(View.VISIBLE);
	}

	private void enableFloorEdit()
	{
		addFloorlt.setVisibility(View.INVISIBLE);
		addFloorBtn.setVisibility(View.INVISIBLE);
		addFloorEdit.setVisibility(View.VISIBLE);
	}
	
	
	@OnEditorAction(R.id.addFloorEdit)
	public boolean handleFloorChange(TextView v, int actionId, KeyEvent event)
	{
		if (actionId == EditorInfo.IME_ACTION_DONE)
		{
			if(addFloorEdit.getText().toString().length() > 0) {
				for (Floor f : HSUtil.getFloors()) {
					if (f.getDisplayName().equals(addFloorEdit.getText().toString())) {
						Toast.makeText(getActivity().getApplicationContext(), "Floor already exists : " + addRoomEdit.getText(), Toast.LENGTH_SHORT).show();
						return true;
					}
				}

				HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
				Floor hsFloor = new Floor.Builder()
						.setDisplayName(addFloorEdit.getText().toString())
						.setSiteRef(siteMap.get("id").toString())
						.build();
				hsFloor.setId(CCUHsApi.getInstance().addFloor(hsFloor));
				floorList.add(hsFloor);
				Collections.sort(floorList, new FloorComparator());
				updateFloors();
				selectFloor(HSUtil.getFloors().size() - 1);

				InputMethodManager mgr = (InputMethodManager) getActivity()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.hideSoftInputFromWindow(addFloorEdit.getWindowToken(), 0);
				Toast.makeText(getActivity().getApplicationContext(),
						"Floor " + addFloorEdit.getText() + " added", Toast.LENGTH_SHORT).show();
				return true;
			}
			else {
				Toast.makeText(getActivity().getApplicationContext(), "Floor cannot be empty", Toast.LENGTH_SHORT).show();
			}
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



	@OnClick(R.id.lt_addzone)
	public void addRoomBtn()
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
		addZonelt.setVisibility(View.INVISIBLE);
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
			if(addRoomEdit.getText().toString().length() > 0) {
				for (Floor f : HSUtil.getFloors()) {
					for (Zone z : HSUtil.getZones(f.getId())) {
						if (z.getDisplayName().equals(addRoomEdit.getText().toString())) {
							Toast.makeText(getActivity().getApplicationContext(), "Zone already exists : " + addRoomEdit.getText(), Toast.LENGTH_SHORT).show();
							return true;
						}
					}
				}

				Toast.makeText(getActivity().getApplicationContext(),
						"Room " + addRoomEdit.getText() + " added", Toast.LENGTH_SHORT).show();
				Floor floor = floorList.get(mFloorListAdapter.getSelectedPostion());
				HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);

				//Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);

				Zone hsZone = new Zone.Builder()
						.setDisplayName(addRoomEdit.getText().toString())
						.setFloorRef(floor.getId())
						.setSiteRef(siteMap.get("id").toString())
						.build();
				String zoneId = CCUHsApi.getInstance().addZone(hsZone);
				hsZone.setId(zoneId);
				hsZone.setScheduleRef(DefaultSchedules.generateDefaultSchedule(true, zoneId));
				CCUHsApi.getInstance().updateZone(hsZone, zoneId);
				CCUHsApi.getInstance().syncEntityTree();
				roomList.add(hsZone);
				Collections.sort(roomList, new ZoneComparator());
				updateRooms(roomList);
				selectRoom(roomList.indexOf(hsZone));

				InputMethodManager mgr = (InputMethodManager) getActivity()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.hideSoftInputFromWindow(addRoomEdit.getWindowToken(), 0);
				return true;
			}
			else {
				Toast.makeText(getActivity().getApplicationContext(), "Room cannot be empty", Toast.LENGTH_SHORT).show();
			}
		}
		return false;
	}

	@OnClick(R.id.pairModuleBtn)
	public void startPairing()
	{
		short meshAddress = L.generateSmartNodeAddress();
		if(mFloorListAdapter.getSelectedPostion() == -1)
		{
			if (L.ccu().oaoProfile != null) {
				Toast.makeText(getActivity(),"OAO Module already paired",Toast.LENGTH_LONG).show();
			} else
			{
				showDialogFragment(FragmentBLEInstructionScreen.getInstance(meshAddress, "SYSTEM", "SYSTEM", ProfileType.OAO, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
				//DialogOAOProfile oaoProfiling = DialogOAOProfile.newInstance(Short.parseShort(nodeAddr), "SYSTEM", "SYSTEM");
				//showDialogFragment(oaoProfiling, DialogOAOProfile.ID);
			}
		}
		else {
			/* Checks to see if emulated and doesn't popup BLE dialogs */

			//This should be moved to pair button for select device type screen.
			showDialogFragment(FragmentSelectDeviceType.newInstance(meshAddress, getSelectedZone().getId(), getSelectedFloor().getId()), FragmentSelectDeviceType.ID);
		}
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
		getActivity().registerReceiver(mPairingReceiver, new IntentFilter(ACTION_BLE_PAIRING_COMPLETED));
		// Create and show the dialog.
		dialogFragment.show(ft, id);
	}
	
	
	@OnItemClick(R.id.floorList)
	public void setFloorListView(AdapterView<?> parent, View view, int position, long id)
	{
		selectFloor(position);
		setSytemUnselection();
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
		String nodeAddr = mModuleListAdapter.getItem(position);
		if (((mFloorListAdapter.getSelectedPostion() == -1) && (mRoomListAdapter.getSelectedPostion() != -1))|| (mRoomListAdapter == null || mRoomListAdapter.getSelectedPostion() == -1) )
		{
			DialogOAOProfile oaoProfiling = DialogOAOProfile.newInstance(Short.parseShort(nodeAddr), "SYSTEM", "SYSTEM");
			showDialogFragment(oaoProfiling, DialogOAOProfile.ID);
			return;
		}
		Floor floor = getSelectedFloor();
		Zone zone = getSelectedZone();
		
		
		ZoneProfile profile = L.getProfile(Short.parseShort(nodeAddr));
		if(profile != null) {

			switch (profile.getProfileType()) {
			/*case HMP:
				showDialogFragment(FragmentHMPConfiguration
						                   .newInstance(nodeAddr,getSelectedZone().roomName, config.getNodeType(), getSelectedFloor().mFloorName), FragmentHMPConfiguration.ID);
				break;*/
				case VAV_REHEAT:
				case VAV_SERIES_FAN:
				case VAV_PARALLEL_FAN:
					VavProfileConfiguration config = profile.getProfileConfiguration(Short.parseShort(nodeAddr));
					showDialogFragment(FragmentVAVConfiguration
							.newInstance(Short.parseShort(nodeAddr), zone.getDisplayName(), config.getNodeType(), floor.getDisplayName(), profile.getProfileType()), FragmentVAVConfiguration.ID);
					break;
				case PLC:
					showDialogFragment(FragmentPLCConfiguration
							                   .newInstance(Short.parseShort(nodeAddr),zone.getDisplayName(), NodeType.SMART_NODE, floor.getDisplayName()), FragmentPLCConfiguration.ID);
				break;
				case DAB:
					showDialogFragment(FragmentDABConfiguration
											   .newInstance(Short.parseShort(nodeAddr),zone.getDisplayName(), NodeType.SMART_NODE, floor.getDisplayName(), profile.getProfileType()), FragmentDABConfiguration.ID);
					break;
				case EMR:
					showDialogFragment(FragmentEMRConfiguration
							                   .newInstance(Short.parseShort(nodeAddr),zone.getDisplayName(), NodeType.SMART_NODE, floor.getDisplayName()), FragmentEMRConfiguration.ID);
					break;
				case SMARTSTAT_CONVENTIONAL_PACK_UNIT:
					showDialogFragment(FragmentCPUConfiguration
							.newInstance(Short.parseShort(nodeAddr), zone.getDisplayName(), /*cpuConfig.getNodeType()*/ NodeType.SMART_STAT, floor.getDisplayName(), profile.getProfileType()), FragmentCPUConfiguration.ID);
					break;
				case SMARTSTAT_HEAT_PUMP_UNIT:
					showDialogFragment(FragmentHeatPumpConfiguration
							.newInstance(Short.parseShort(nodeAddr),zone.getDisplayName(),NodeType.SMART_STAT,floor.getDisplayName(),profile.getProfileType()),FragmentHeatPumpConfiguration.ID);
					break;
				case SMARTSTAT_TWO_PIPE_FCU:
					showDialogFragment(Fragment2PipeFanCoilUnitConfig
							.newInstance(Short.parseShort(nodeAddr),zone.getDisplayName(),NodeType.SMART_STAT,floor.getDisplayName(),profile.getProfileType()),Fragment2PipeFanCoilUnitConfig.ID);
					break;
				case SMARTSTAT_FOUR_PIPE_FCU:
					showDialogFragment(Fragment4PipeFanCoilUnitConfig
							.newInstance(Short.parseShort(nodeAddr),zone.getDisplayName(),NodeType.SMART_STAT,floor.getDisplayName(),profile.getProfileType()),Fragment4PipeFanCoilUnitConfig.ID);
					break;

			}
		}else
			Toast.makeText(getActivity(),"Zone profile is empty, recheck your DB",Toast.LENGTH_LONG);
		
		
	}
	
	class FloorComparator implements Comparator<Floor>
	{
		@Override
		public int compare(Floor a, Floor b) {
			return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
		}
	}
	
	class ZoneComparator implements Comparator<Zone>
	{
		@Override
		public int compare(Zone a, Zone b) {
			return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
		}
	}
	class ModuleComparator implements Comparator<Equip>
	{
		@Override
		public int compare(Equip a, Equip b) {
			return a.getGroup().compareToIgnoreCase(b.getGroup());
		}
	}
}
