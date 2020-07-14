package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
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

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRow;
import org.projecthaystack.client.CallException;
import org.projecthaystack.client.HClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.device.bacnet.BACnetUtils;
import a75f.io.device.mesh.LSerial;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;
import a75f.io.renatus.util.HttpsUtils.HTTPUtils;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import butterknife.OnItemClick;

public class FloorPlanFragment extends Fragment
{
	public static final String ACTION_BLE_PAIRING_COMPLETED =
			"a75f.io.renatus.BLE_PAIRING_COMPLETED";
	public static Zone selectedZone;
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
	//
	private Zone roomToRename;
	private Floor floorToRename;
	ArrayList<Floor> siteFloorList = new ArrayList<>();
	ArrayList<String> siteRoomList = new ArrayList<>();

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
							//TODO Commented this out for seed messages
							//if(LSerial.getInstance().isConnected()) //If usb connected and pairing done then reseed
							//	LSerial.getInstance().setResetSeedMessage(true);
							try{
								if (mFloorListAdapter.getSelectedPostion() == -1) {
									updateOAOModule();
								}else
								{
									updateModules(getSelectedZone());
									setScheduleType(getSelectedZone().getId());
									//Update BACnet Database Revision by adding new module to zone
									ArrayList<Equip> zoneEquips = HSUtil.getEquips(getSelectedZone().getId());
									if (zoneEquips.size() == 1) {
										BACnetUtils.updateDatabaseRevision();
									}
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

	private void setScheduleType(String zoneId) {

		new Handler(Looper.getMainLooper()).postDelayed(() -> {

			ArrayList<Equip> zoneEquips  = HSUtil.getEquips(zoneId);
			if(zoneEquips != null && (zoneEquips.size() > 0)) {
				int newScheduleType = 0;
				for (Equip equip: zoneEquips){
					String scheduleTypeId = CCUHsApi.getInstance().readId("point and scheduleType and equipRef == \"" + equip.getId() + "\"");
					int mScheduleType = (int)CCUHsApi.getInstance().readPointPriorityVal(scheduleTypeId);
					if (mScheduleType > newScheduleType){
						newScheduleType = mScheduleType;
					}
				}

				for (Equip equip: zoneEquips) {
					String scheduleTypeId = CCUHsApi.getInstance().readId("point and scheduleType and equipRef == \"" + equip.getId() + "\"");

					CCUHsApi.getInstance().writeDefaultValById(scheduleTypeId, (double) newScheduleType);
					CCUHsApi.getInstance().writeHisValById(scheduleTypeId, (double)newScheduleType);
					
				}
			}
		},6000);

	}


	public FloorPlanFragment()
	{
	}
	
	
	public static FloorPlanFragment newInstance()
	{
		return new FloorPlanFragment();
	}
	
	
	private Zone getSelectedZone()
	{
		selectedZone = roomList.get(mRoomListAdapter.getSelectedPostion());
		return selectedZone;
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

		//getBuildingFloorsZones();
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


	@SuppressLint("StaticFieldLeak")
	public void getBuildingFloorsZones(String enableKeyboard) {
		loadExistingZones();
	//	ProgressDialogUtils.showProgressDialog(getContext(), "Fetching floors and zones...");
		new AsyncTask<String, Void,Void>(){

			@Override
			protected Void doInBackground(String... strings) {
				if (!HTTPUtils.isNetworkConnected()){
					return null;
				}
				HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
				HashMap site = CCUHsApi.getInstance().read("site");
				String siteLUID = site.get("id").toString();
				String siteGUID = CCUHsApi.getInstance().getGUID(siteLUID);

				if (siteGUID == null) {
					return null;
				}
				//for floor
				HDict tDict = new HDictBuilder().add("filter", "floor and siteRef == " + siteGUID).toDict();
				HGrid floorPoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
				if (floorPoint == null){
					return null;
				}
				Iterator it = floorPoint.iterator();

				siteFloorList.clear();
				while (it.hasNext())
				{
					while (it.hasNext())
					{
						HashMap<Object, Object> map = new HashMap<>();
						HRow                    r   = (HRow) it.next();
						HRow.RowIterator        ri  = (HRow.RowIterator) r.iterator();
						while (ri.hasNext())
						{
							HDict.MapEntry m = (HDict.MapEntry) ri.next();
							map.put(m.getKey(), m.getValue());
						}
						siteFloorList.add(new Floor.Builder().setHashMap(map).build());
					}
				}

				//for zones
				HDict zDict = new HDictBuilder().add("filter", "room and not oao and siteRef == " + siteGUID).toDict();
				
				try
				{
					HGrid zonePoint = hClient.call("read", HGridBuilder.dictToGrid(zDict));
					if (zonePoint == null){
						return null;
					}
					Iterator zit = zonePoint.iterator();
					siteRoomList.clear();
					while (zit.hasNext())
					{
						HRow zr = (HRow) zit.next();
						
						if (zr.getStr("dis") != null) {
							siteRoomList.add(zr.getStr("dis"));
						}
					}

				} catch (CallException e) {
					Log.d(L.TAG_CCU_UI, "Failed to fetch room data "+e.getMessage());
					//ProgressDialogUtils.hideProgressDialog();
					e.printStackTrace();
				}
				

				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				/*ProgressDialogUtils.hideProgressDialog();
				if (!TextUtils.isEmpty(enableKeyboard) && (enableKeyboard.contains("room") || enableKeyboard.contains("floor"))){
					InputMethodManager mgr =
							(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					mgr.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
				}*/
				super.onPostExecute(aVoid);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void loadExistingZones() {
		siteFloorList.clear();
		siteRoomList.clear();
		ArrayList<Floor> floorList = HSUtil.getFloors();
		siteFloorList.addAll(floorList);
		for (Floor f: floorList){
			ArrayList<Zone> zoneList = HSUtil.getZones(f.getId());
			for (Zone zone:zoneList){
				siteRoomList.add(zone.getDisplayName());
			}
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
			Log.d(L.TAG_CCU_UI,"Show OAO Equip ");
			mModuleListAdapter = new DataArrayAdapter<>(FloorPlanFragment.this.getActivity(), R.layout.listviewitem,createAddressList(equipList));
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					moduleListView.setAdapter(mModuleListAdapter);
					moduleListView.setVisibility(View.VISIBLE);
				}
				
			});
			return true;
		} else {
			moduleListView.setAdapter(null);
			Log.d(L.TAG_CCU_UI,"OAO Equip does not exist ");
			return false;
		}
		
	}
	private void updateModules(Zone zone)
	{
		Log.d(L.TAG_CCU_UI,"Zone Selected "+zone.getDisplayName());
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

		if (updateOAOModule())
		{
			moduleListView.setVisibility(View.VISIBLE);
		} else {
			enableModueButton();
		}
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
		textViewSystemDevice.setSelected(true);
		textViewOAO.setSelected(true);
		textViewOAO.setTextColor(Color.WHITE);
		rl_oao.setEnabled(false);
		rl_systemdevice.setEnabled(false);
		roomListView.setVisibility(View.GONE);
		moduleListView.setVisibility(View.GONE);
		addZonelt.setEnabled(false);
		addRoomBtn.setEnabled(false);
		if (addRoomEdit.getVisibility() == View.VISIBLE) {
			closeAddZoneEditViews();
		}
	}

	private void closeAddZoneEditViews() {
		addZonelt.setVisibility(View.VISIBLE);
		addRoomEdit.setVisibility(View.INVISIBLE);

		InputMethodManager mgr =
				(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.hideSoftInputFromWindow(addRoomEdit.getWindowToken(), 0);
	}

	private void setSytemUnselection()
	{
		rl_systemdevice.setBackgroundColor(Color.WHITE);
		rl_oao.setBackgroundColor(Color.WHITE);
		textViewSystemDevice.setSelected(false);
		textViewOAO.setSelected(false);
		textViewSystemDevice.setTextColor(getContext().getResources().getColor(R.color.text_color));
		textViewOAO.setTextColor(getContext().getResources().getColor(R.color.text_color));
		rl_systemdevice.setEnabled(true);
		rl_oao.setEnabled(true);
		roomListView.setVisibility(View.VISIBLE);
		moduleListView.setVisibility(View.VISIBLE);
		addZonelt.setEnabled(true);
		addRoomBtn.setEnabled(true);
	}

	private void enableFloorEdit()
	{
		addFloorlt.setVisibility(View.INVISIBLE);
		addFloorBtn.setVisibility(View.INVISIBLE);
		addFloorEdit.setVisibility(View.VISIBLE);
		getBuildingFloorsZones("floor");
	}
	
	
	@OnEditorAction(R.id.addFloorEdit)
	public boolean handleFloorChange(TextView v, int actionId, KeyEvent event)
	{
		if (actionId == EditorInfo.IME_ACTION_DONE)
		{
			if (floorToRename != null){

				floorList.remove(floorToRename);
				for (Floor f: new ArrayList<>(siteFloorList)){
					if (f.getDisplayName().equals(floorToRename.getDisplayName())) {
						siteFloorList.remove(f);
					}
				}
				Floor hsFloor = new Floor.Builder()
						.setDisplayName(addFloorEdit.getText().toString())
						.setSiteRef(floorToRename.getSiteRef())
						.build();
				hsFloor.setId(floorToRename.getId());
				for (Floor floor : siteFloorList) {
					if (floor.getDisplayName().equals(addFloorEdit.getText().toString())) {
						AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
						adb.setMessage("Floor name already exists in this site,would you like to continue?");
						adb.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
							hsFloor.setId(CCUHsApi.getInstance().addFloor(hsFloor));
							CCUHsApi.getInstance().putUIDMap(hsFloor.getId(), floor.getId());
							floorList.add(hsFloor);
							floorList.add(floorToRename);
							Collections.sort(floorList, new FloorComparator());
							updateFloors();
							selectFloor(floorList.size() - 1);

							InputMethodManager mgr = (InputMethodManager) getActivity()
									.getSystemService(Context.INPUT_METHOD_SERVICE);
							mgr.hideSoftInputFromWindow(addFloorEdit.getWindowToken(), 0);

							floorToRename = null;
							L.saveCCUState();
							CCUHsApi.getInstance().syncEntityTree();
							siteFloorList.add(hsFloor);
							dialog.dismiss();
						});
						adb.setNegativeButton(getResources().getString(R.string.cancel), (dialog, which) -> {
							dialog.dismiss();
						});
						adb.show();

						return true;
					}
				}

				floorList.add(hsFloor);
				CCUHsApi.getInstance().updateFloor(hsFloor, floorToRename.getId());

				Collections.sort(floorList, new FloorComparator());
				updateFloors();
				selectFloor(HSUtil.getFloors().size() - 1);

				InputMethodManager mgr = (InputMethodManager) getActivity()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.hideSoftInputFromWindow(addFloorEdit.getWindowToken(), 0);

				floorToRename = null;
				L.saveCCUState();
				CCUHsApi.getInstance().syncEntityTree();

				siteFloorList.add(hsFloor);
				return true;
			}

			if(addFloorEdit.getText().toString().length() > 0) {
				ArrayList<String> flrMarkers = new ArrayList<>();
				flrMarkers.add("writable");
				HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
				Floor hsFloor = new Floor.Builder()
						.setDisplayName(addFloorEdit.getText().toString()).setMarkers(flrMarkers)
						.setSiteRef(siteMap.get("id").toString())
						.build();
				for (Floor floor : siteFloorList) {
					if (floor.getDisplayName().equals(addFloorEdit.getText().toString())) {
						AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
						adb.setMessage("Floor name already exists in this site,would you like to continue?");
						adb.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
							hsFloor.setId(CCUHsApi.getInstance().addFloor(hsFloor));
							CCUHsApi.getInstance().putUIDMap(hsFloor.getId(), floor.getId());
							floorList.add(hsFloor);
							Collections.sort(floorList, new FloorComparator());
							updateFloors();
							selectFloor(HSUtil.getFloors().size() - 1);

							InputMethodManager mgr = (InputMethodManager) getActivity()
									.getSystemService(Context.INPUT_METHOD_SERVICE);
							mgr.hideSoftInputFromWindow(addFloorEdit.getWindowToken(), 0);

							floorToRename = null;
							L.saveCCUState();
							CCUHsApi.getInstance().syncEntityTree();

							siteFloorList.add(hsFloor);

							dialog.dismiss();
						});
						adb.setNegativeButton(getResources().getString(R.string.cancel), (dialog, which) -> {
							dialog.dismiss();
						});
						adb.show();

						return true;
					}
				}

				hsFloor.setId(CCUHsApi.getInstance().addFloor(hsFloor));
				floorList.add(hsFloor);
				Collections.sort(floorList, new FloorComparator());
				updateFloors();
				selectFloor(HSUtil.getFloors().size() - 1);
				L.saveCCUState();
				CCUHsApi.getInstance().syncEntityTree();

				InputMethodManager mgr = (InputMethodManager) getActivity()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.hideSoftInputFromWindow(addFloorEdit.getWindowToken(), 0);
				Toast.makeText(getActivity().getApplicationContext(),
						"Floor " + addFloorEdit.getText() + " added", Toast.LENGTH_SHORT).show();
				siteFloorList.add(hsFloor);
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
		getBuildingFloorsZones("room");
	}
	
	
	@OnFocusChange(R.id.addRoomEdit)
	public void handleRoomFocus(View v, boolean hasFocus)
	{
		if (!hasFocus)
		{
			enableRoomBtn();
		}
	}

	public void renameZone(Zone zone)
	{
		roomToRename = zone;
		enableRoomEdit();
		addRoomEdit.setText(zone.getDisplayName());
		addRoomEdit.requestFocus();
		addRoomEdit.setSelection(zone.getDisplayName().length());

		InputMethodManager mgr =
				(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.showSoftInput(addRoomEdit, InputMethodManager.SHOW_IMPLICIT);
	}

	public void renameFloor(Floor floor)
	{
		floorToRename = floor;
		enableFloorEdit();
		addFloorEdit.setText(floor.getDisplayName());
		addFloorEdit.requestFocus();
		addFloorEdit.setSelection(floor.getDisplayName().length());

		InputMethodManager mgr =
				(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.showSoftInput(addFloorEdit, InputMethodManager.SHOW_IMPLICIT);
	}

	@OnEditorAction(R.id.addRoomEdit)
	public boolean handleRoomChange(TextView v, int actionId, KeyEvent event)
	{
		if (actionId == EditorInfo.IME_ACTION_DONE)
		{

			if (roomToRename!= null){
				roomList.remove(roomToRename);
				siteRoomList.remove(roomToRename.getDisplayName());
					for (String z : siteRoomList) {
						if (z.equals(addRoomEdit.getText().toString())) {
							Toast.makeText(getActivity().getApplicationContext(), "Zone already exists : " + addRoomEdit.getText(), Toast.LENGTH_SHORT).show();
							return true;
						}
					}

				Zone hsZone = new Zone.Builder()
						.setDisplayName(addRoomEdit.getText().toString())
						.setFloorRef(roomToRename.getFloorRef())
						.setSiteRef(roomToRename.getSiteRef())
						.build();

				hsZone.setId(roomToRename.getId());
				CCUHsApi.getInstance().updateZone(hsZone, roomToRename.getId());
				L.saveCCUState();
				CCUHsApi.getInstance().syncEntityTree();
				roomList.add(hsZone);
				Collections.sort(roomList, new ZoneComparator());
				updateRooms(roomList);
				selectRoom(roomList.indexOf(hsZone));

				InputMethodManager mgr = (InputMethodManager) getActivity()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.hideSoftInputFromWindow(addRoomEdit.getWindowToken(), 0);

				roomToRename = null;
				if (!siteRoomList.contains(addRoomEdit.getText().toString())){
					siteRoomList.add(addRoomEdit.getText().toString());
				}
				return true;
			}

			if(addRoomEdit.getText().toString().length() > 0) {
					for (String z : siteRoomList) {
						if (z.equals(addRoomEdit.getText().toString())) {
							Toast.makeText(getActivity().getApplicationContext(), "Zone already exists : " + addRoomEdit.getText(), Toast.LENGTH_SHORT).show();
							return true;
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
				DefaultSchedules.setDefaultCoolingHeatingTemp();
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

				/*//TODO: update default building data
				Schedule buildingSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);
				Schedule zoneSchedule = CCUHsApi.getInstance().getScheduleById(hsZone.getScheduleRef());
				for (Schedule.Days days : zoneSchedule.getDays()) {
					days.setHeatingVal(buildingSchedule.getCurrentValues().getHeatingVal());
					days.setCoolingVal(buildingSchedule.getCurrentValues().getCoolingVal());
				}

				CCUHsApi.getInstance().updateZoneSchedule(zoneSchedule, zoneSchedule.getRoomRef());*/
				siteRoomList.add(addRoomEdit.getText().toString());
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

        if (mFloorListAdapter.getSelectedPostion() == -1) {
            short meshAddress = L.generateSmartNodeAddress();
            if (L.ccu().oaoProfile != null) {
                Toast.makeText(getActivity(), "OAO Module already paired", Toast.LENGTH_LONG).show();
            } else {
				if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT) {
					Toast.makeText(getActivity(), "Please set system profile to vav or dab to continue!", Toast.LENGTH_LONG).show();
					return;
				}
                showDialogFragment(FragmentBLEInstructionScreen.getInstance(meshAddress, "SYSTEM", "SYSTEM", ProfileType.OAO, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
            }
            return;
        }

		Zone selectedZone = getSelectedZone();
		ArrayList<Equip> zoneEquips  = HSUtil.getEquips(selectedZone.getId());
		boolean isPLCPaired = false;
		boolean isEMRPaired = false;
		boolean isCCUPaired = false;
		boolean isPaired = false;
		if(zoneEquips.size() > 0)
		{
			isPaired = true;
			for(int i=0;i<zoneEquips.size();i++)
			{
				if(zoneEquips.get(i).getProfile().contains("PLC"))
				{
					isPLCPaired = true;
				}if(zoneEquips.get(i).getProfile().contains("EMR"))
				{
					isEMRPaired = true;
				}if(zoneEquips.get(i).getProfile().contains("TEMP_INFLUENCE"))
				{
					isCCUPaired = true;
				}
			}
		}

		if(!isPLCPaired && !isEMRPaired && !isCCUPaired) {
			short meshAddress = L.generateSmartNodeAddress();
			if (mFloorListAdapter.getSelectedPostion() == -1) {
				if (L.ccu().oaoProfile != null) {
					Toast.makeText(getActivity(), "OAO Module already paired", Toast.LENGTH_LONG).show();
				} else {
					if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT) {
						Toast.makeText(getActivity(), "Please set system profile to vav or dab to continue!", Toast.LENGTH_LONG).show();
						return;
					}
					showDialogFragment(FragmentBLEInstructionScreen.getInstance(meshAddress, "SYSTEM", "SYSTEM", ProfileType.OAO, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
					//DialogOAOProfile oaoProfiling = DialogOAOProfile.newInstance(Short.parseShort(nodeAddr), "SYSTEM", "SYSTEM");
					//showDialogFragment(oaoProfiling, DialogOAOProfile.ID);
				}
			} else {
				if (zoneEquips.size() >= 3){
					Toast.makeText(getActivity(), "More than 3 modules are not allowed", Toast.LENGTH_LONG).show();
					return;
				}
				/* Checks to see if emulated and doesn't popup BLE dialogs */

				//This should be moved to pair button for select device type screen.
				showDialogFragment(FragmentSelectDeviceType.newInstance(meshAddress, getSelectedZone().getId(), getSelectedFloor().getId(),isPaired), FragmentSelectDeviceType.ID);
			}
		}else {
			if (isPLCPaired) {
				Toast.makeText(getActivity(), "Pi Loop Module is already paired in this zone", Toast.LENGTH_LONG).show();
			}
			if (isEMRPaired) {
				Toast.makeText(getActivity(), "Energy Meter Module is already paired in this zone", Toast.LENGTH_LONG).show();
			}
			if (isCCUPaired) {
				Toast.makeText(getActivity(), "CCU as Zone is already paired in this zone", Toast.LENGTH_LONG).show();
			}
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
		mModuleListAdapter.setSelectedItem(position);
		String nodeAddr = mModuleListAdapter.getItem(position);
		if (((mFloorListAdapter != null && mFloorListAdapter.getSelectedPostion() == -1) && (mRoomListAdapter != null && mRoomListAdapter.getSelectedPostion() != -1))|| (mRoomListAdapter == null || mRoomListAdapter.getSelectedPostion() == -1) )
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
							.newInstance(Short.parseShort(nodeAddr), zone.getId(), config.getNodeType(), floor.getId(), profile.getProfileType()), FragmentVAVConfiguration.ID);
					break;
				case PLC:
					showDialogFragment(FragmentPLCConfiguration
							                   .newInstance(Short.parseShort(nodeAddr),zone.getId(), NodeType.SMART_NODE, floor.getId()), FragmentPLCConfiguration.ID);
				break;
				case DAB:
					showDialogFragment(FragmentDABConfiguration
											   .newInstance(Short.parseShort(nodeAddr),zone.getId(), NodeType.SMART_NODE, floor.getId(), profile.getProfileType()), FragmentDABConfiguration.ID);
					break;
				case EMR:
					showDialogFragment(FragmentEMRConfiguration
							                   .newInstance(Short.parseShort(nodeAddr),zone.getId(), NodeType.SMART_NODE, floor.getId()), FragmentEMRConfiguration.ID);
					break;
				case SMARTSTAT_CONVENTIONAL_PACK_UNIT:
					showDialogFragment(FragmentCPUConfiguration
							.newInstance(Short.parseShort(nodeAddr), zone.getId(), /*cpuConfig.getNodeType()*/ NodeType.SMART_STAT, floor.getId(), profile.getProfileType()), FragmentCPUConfiguration.ID);
					break;
				case SMARTSTAT_HEAT_PUMP_UNIT:
					showDialogFragment(FragmentHeatPumpConfiguration
							.newInstance(Short.parseShort(nodeAddr),zone.getId(),NodeType.SMART_STAT,floor.getId(),profile.getProfileType()),FragmentHeatPumpConfiguration.ID);
					break;
				case SMARTSTAT_TWO_PIPE_FCU:
					showDialogFragment(Fragment2PipeFanCoilUnitConfig
							.newInstance(Short.parseShort(nodeAddr),zone.getId(),NodeType.SMART_STAT,floor.getId(),profile.getProfileType()),Fragment2PipeFanCoilUnitConfig.ID);
					break;
				case SMARTSTAT_FOUR_PIPE_FCU:
					showDialogFragment(Fragment4PipeFanCoilUnitConfig
							.newInstance(Short.parseShort(nodeAddr),zone.getId(),NodeType.SMART_STAT,floor.getId(),profile.getProfileType()),Fragment4PipeFanCoilUnitConfig.ID);
					break;
				case TEMP_INFLUENCE:
					showDialogFragment(FragmentTempInfConfiguration
							.newInstance(Short.parseShort(nodeAddr),zone.getId(), NodeType.CONTROL_MOTE, floor.getId()), FragmentTempInfConfiguration.ID);
					break;
				case SSE:
					showDialogFragment(FragmentSSEConfiguration
							.newInstance(Short.parseShort(nodeAddr),zone.getId(), NodeType.SMART_NODE, floor.getId(),profile.getProfileType()), FragmentSSEConfiguration.ID);
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
