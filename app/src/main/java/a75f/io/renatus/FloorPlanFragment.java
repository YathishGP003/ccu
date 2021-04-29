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

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

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
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.device.bacnet.BACnetUtils;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;
import a75f.io.renatus.modbus.FragmentModbusConfiguration;
import a75f.io.renatus.modbus.FragmentModbusEnergyMeterConfiguration;
import a75f.io.renatus.util.HttpsUtils.HTTPUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import butterknife.OnItemClick;

public class FloorPlanFragment extends Fragment {
    public static final String ACTION_BLE_PAIRING_COMPLETED =
            "a75f.io.renatus.BLE_PAIRING_COMPLETED";
    public static Zone selectedZone;
    public DataArrayAdapter<Floor> mFloorListAdapter;
    public DataArrayAdapter<Zone> mRoomListAdapter;
    public DataArrayAdapter<String> mModuleListAdapter;

    enum SysyemDeviceType {
        OAO,
        ENERGY_METER,
        BTU_METER
    }

    SysyemDeviceType sysyemDeviceType;

    @BindView(R.id.addFloorBtn)
    TextView addFloorBtn;
    @BindView(R.id.addRoomBtn)
    TextView addRoomBtn;
    @BindView(R.id.pairModuleBtn)
    TextView pairModuleBtn;
    @BindView(R.id.addFloorEdit)
    EditText addFloorEdit;
    @BindView(R.id.addRoomEdit)
    EditText addRoomEdit;
    @BindView(R.id.addModuleEdit)
    EditText addModuleEdit;
    @BindView(R.id.floorList)
    ListView floorListView;
    @BindView(R.id.roomList)
    ListView roomListView;
    @BindView(R.id.moduleList)
    ListView moduleListView;
    Short[] smartNodeAddresses;

    @BindView(R.id.lt_addfloor)
    LinearLayout addFloorlt;
    @BindView(R.id.lt_addzone)
    LinearLayout addZonelt;
    @BindView(R.id.lt_addModule)
    LinearLayout addModulelt;

    @BindView(R.id.rl_systemdevice)
    RelativeLayout rl_systemdevice;

    @BindView(R.id.rl_oao)
    RelativeLayout rl_oao;

    @BindView(R.id.rl_modbus_energy_meter)
    RelativeLayout rl_modbus_energy_meter;

    @BindView(R.id.rl_modbus_btu_meter)
    RelativeLayout rl_modbus_btu_meter;


    @BindView(R.id.textSystemDevice)
    TextView textViewSystemDevice;
    @BindView(R.id.textOAO)
    TextView textViewOAO;

    @BindView(R.id.textModbusEnergyMeter)
    TextView textModbusEnergyMeter;

    @BindView(R.id.textModbusBTUMeter)
    TextView textModbusBTUMeter;

    ArrayList<Floor> floorList = new ArrayList();
    ArrayList<Zone> roomList = new ArrayList();
    //
    private Zone roomToRename;
    private Floor floorToRename;
    ArrayList<Floor> siteFloorList = new ArrayList<>();
    ArrayList<String> siteRoomList = new ArrayList<>();

    private final BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {

                case ACTION_BLE_PAIRING_COMPLETED:
                    Log.i("Test", "onReceive: " + ACTION_BLE_PAIRING_COMPLETED);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //TODO Commented this out for seed messages
                            //if(LSerial.getInstance().isConnected()) //If usb connected and pairing done then reseed
                            //	LSerial.getInstance().setResetSeedMessage(true);
                            try {
                                if (mFloorListAdapter.getSelectedPostion() == -1) {
                                    if (sysyemDeviceType == SysyemDeviceType.ENERGY_METER) {
                                        getActivity().runOnUiThread(() -> onEnergyMeterClick());
                                    }
                                    if (sysyemDeviceType == SysyemDeviceType.OAO) {
                                        updateOAOModule();
                                    }
                                    if (sysyemDeviceType == SysyemDeviceType.BTU_METER) {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                onBTUMeterClick();
                                            }
                                        });
                                    }


                                } else {
                                    updateModules(getSelectedZone());
                                    setScheduleType(getSelectedZone().getId());
                                    //Update BACnet Database Revision by adding new module to zone
                                    ArrayList<Equip> zoneEquips = HSUtil.getEquips(getSelectedZone().getId());
                                    if (zoneEquips.size() == 1) {
                                        if (!zoneEquips.get(0).getMarkers().contains("pid") && !zoneEquips.get(0).getMarkers().contains("emr")) {
                                            BACnetUtils.updateDatabaseRevision();
                                        }
                                    }
                                }
                                //Crash here because of activity null while moving to other fragment and return back here after edit config
                                if ((getActivity() != null) && (mPairingReceiver != null))
                                    getActivity().unregisterReceiver(mPairingReceiver);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    break;
            }
        }
    };

    /**
     * Holding privious selection to Enable/disable the Selection between OAO/EneryMeter/BTUMeter
     */
    int priviousSelectedDevice = 0;

    private void setScheduleType(String zoneId) {

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            ArrayList<Equip> zoneEquips = HSUtil.getEquips(zoneId);
            if (zoneEquips != null && (zoneEquips.size() > 0)) {
                int newScheduleType = 0;
                for (Equip equip : zoneEquips) {
                    String scheduleTypeId = CCUHsApi.getInstance().readId("point and scheduleType and equipRef == \"" + equip.getId() + "\"");
                    int mScheduleType = (int) CCUHsApi.getInstance().readPointPriorityVal(scheduleTypeId);
                    if (mScheduleType > newScheduleType) {
                        newScheduleType = mScheduleType;
                    }
                }

                for (Equip equip : zoneEquips) {
                    String scheduleTypeId = CCUHsApi.getInstance().readId("point and scheduleType and equipRef == \"" + equip.getId() + "\"");

                    CCUHsApi.getInstance().writeDefaultValById(scheduleTypeId, (double) newScheduleType);
                    CCUHsApi.getInstance().writeHisValById(scheduleTypeId, (double) newScheduleType);

                }
            }
        }, 6000);

    }

    public FloorPlanFragment() {
    }


    public static FloorPlanFragment newInstance() {
        return new FloorPlanFragment();
    }


    private Zone getSelectedZone() {
        Log.i("test", "getSelectedZone:" + mRoomListAdapter.getSelectedPostion());
        selectedZone = roomList.get(mRoomListAdapter.getSelectedPostion());
        return selectedZone;
    }


    private Floor getSelectedFloor() {
        return floorList.get(mFloorListAdapter.getSelectedPostion());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.floorplan, container, false);
        ButterKnife.bind(this, rootView);

        //getBuildingFloorsZones();
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        enableFloorButton();
        disableRoomModule();
    }


    @Override
    public void onStart() {
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
    public void onResume() {
        super.onResume();
        refreshScreen();
    }


    @Override
    public void onPause() {
        super.onPause();
        saveData();
    }


    public void saveData() {
        //Save
        L.saveCCUState();

    }


    public void refreshScreen() {
        floorList = HSUtil.getFloors();
        Collections.sort(floorList, new FloorComparator());
        updateFloors();
    }


    private void updateFloors() {

        mFloorListAdapter = new DataArrayAdapter<>(this.getActivity(), R.layout.listviewitem, floorList);
        //mFloorListAdapter = new DataArrayAdapter<>(this.getActivity(), R.id.textData,floorList);
        floorListView.setAdapter(mFloorListAdapter);
        enableFloorButton();
        if (mFloorListAdapter.getCount() > 0) {
            selectFloor(0);
            enableRoomBtn();
        } else {
            if (mRoomListAdapter != null) {
                mRoomListAdapter.clear();
            }
            if (mModuleListAdapter != null) {
                mModuleListAdapter.clear();
            }
            //disableRoomModule();
        }

        setSystemUnselection();
        addFloorBtn.setEnabled(true);
        addZonelt.setEnabled(true);

    }


    private void selectFloor(int position) {
        mFloorListAdapter.setSelectedItem(position);
        roomList = HSUtil.getZones(getSelectedFloor().getId());
        Collections.sort(roomList, new ZoneComparator());
        updateRooms(roomList);

    }


    //
    private void enableRoomBtn() {
        addZonelt.setVisibility(View.VISIBLE);
        addRoomBtn.setVisibility(View.VISIBLE);
        addRoomEdit.setVisibility(View.INVISIBLE);
    }


    private void updateRooms(ArrayList<Zone> zones) {
        mRoomListAdapter = new DataArrayAdapter<>(this.getActivity(), R.layout.listviewitem, zones);
        //mRoomListAdapter = new DataArrayAdapter<>(this.getActivity(), R.id.textData,zones);
        roomListView.setAdapter(mRoomListAdapter);
        enableRoomBtn();
        if (mRoomListAdapter.getCount() > 0) {
            selectRoom(0);
            enableModueButton();
        } else {
            if (mModuleListAdapter != null) {
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
        new AsyncTask<String, Void, Void>() {

            @Override
            protected Void doInBackground(String... strings) {
                if (!HTTPUtils.isNetworkConnected()) {
                    return null;
                }
                HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
                String siteUID = CCUHsApi.getInstance().getSiteIdRef().toString();

                if (siteUID == null) {
                    return null;
                }
                //for floor
                HDict tDict = new HDictBuilder().add("filter", "floor and siteRef == " + siteUID).toDict();
                HGrid floorPoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
                if (floorPoint == null) {
                    return null;
                }
                Iterator it = floorPoint.iterator();

                siteFloorList.clear();
                while (it.hasNext()) {
                    while (it.hasNext()) {
                        HashMap<Object, Object> map = new HashMap<>();
                        HRow r = (HRow) it.next();
                        HRow.RowIterator ri = (HRow.RowIterator) r.iterator();
                        while (ri.hasNext()) {
                            HDict.MapEntry m = (HDict.MapEntry) ri.next();
                            map.put(m.getKey(), m.getValue());
                        }
                        siteFloorList.add(new Floor.Builder().setHashMap(map).build());
                    }
                }

                //for zones
                HDict zDict = new HDictBuilder().add("filter", "room and not oao and siteRef == " + siteUID).toDict();

                try {
                    HGrid zonePoint = hClient.call("read", HGridBuilder.dictToGrid(zDict));
                    if (zonePoint == null) {
                        return null;
                    }
                    Iterator zit = zonePoint.iterator();
                    siteRoomList.clear();
                    while (zit.hasNext()) {
                        HRow zr = (HRow) zit.next();

                        if (zr.getStr("dis") != null) {
                            siteRoomList.add(zr.getStr("dis"));
                        }
                    }

                } catch (CallException e) {
                    Log.d(L.TAG_CCU_UI, "Failed to fetch room data " + e.getMessage());
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
        for (Floor f : floorList) {
            ArrayList<Zone> zoneList = HSUtil.getZones(f.getId());
            for (Zone zone : zoneList) {
                siteRoomList.add(zone.getDisplayName());
            }
        }
    }

    private void selectRoom(int position) {
        mRoomListAdapter.setSelectedItem(position);
        updateModules(getSelectedZone());
    }


    private void enableModueButton() {
        addModulelt.setVisibility(View.VISIBLE);
        pairModuleBtn.setVisibility(View.VISIBLE);
    }


    private void disableModuButton() {
        addModulelt.setVisibility(View.INVISIBLE);
        pairModuleBtn.setVisibility(View.INVISIBLE);
    }


    private boolean updateOAOModule() {
        ArrayList<Equip> equipList = new ArrayList<>();
        for (HashMap m : CCUHsApi.getInstance().readAll("equip and oao")) {
            equipList.add(new Equip.Builder().setHashMap(m).build());
        }
        if (!equipList.isEmpty()) {
            Log.d(L.TAG_CCU_UI, "Show OAO Equip ");
            mModuleListAdapter = new DataArrayAdapter<>(FloorPlanFragment.this.getActivity(), R.layout.listviewitem, createAddressList(equipList));
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
            Log.d(L.TAG_CCU_UI, "OAO Equip does not exist ");
            return false;
        }

    }

    private boolean isSystemEM(String ref) {
        if (ref.equalsIgnoreCase("SYSTEM"))
            return true;
        return false;
    }

    private boolean updateEnergyMeterModule() {
        ArrayList<Equip> equipList = new ArrayList<>();
        for (HashMap m : CCUHsApi.getInstance().readAll("equip and emr")) {
            if (isSystemEM(m.get("roomRef").toString()) && isSystemEM(m.get("floorRef").toString())) {
                equipList.add(new Equip.Builder().setHashMap(m).build());
            }
        }

        if (equipList != null && (equipList.size() > 0)) {
            mModuleListAdapter = new DataArrayAdapter<>(FloorPlanFragment.this.getActivity(), R.layout.listviewitem, createAddressList(equipList));
            getActivity().runOnUiThread(() -> {
                moduleListView.setAdapter(mModuleListAdapter);
                moduleListView.setVisibility(View.VISIBLE);
            });
            return true;
        } else {
            moduleListView.setAdapter(null);
            return false;
        }
    }

    private boolean updateBTUMeterModule() {
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and btu");
        ArrayList<Equip> equipList = new ArrayList<>();
        for (HashMap m : equips) {
            if (m.get("roomRef").toString().equalsIgnoreCase("SYSTEM") && m.get("floorRef").toString().equalsIgnoreCase("SYSTEM")) {
                equipList.add(new Equip.Builder().setHashMap(m).build());
            }
        }

        if (equipList != null && (equipList.size() > 0)) {
            disableModuButton();
            mModuleListAdapter = new DataArrayAdapter<>(FloorPlanFragment.this.getActivity(), R.layout.listviewitem, createAddressList(equipList));
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
            return false;
        }

    }


    private void updateModules(Zone zone) {
        Log.d(L.TAG_CCU_UI, "Zone Selected " + zone.getDisplayName());
        ArrayList<Equip> zoneEquips = HSUtil.getEquips(zone.getId());
        if (zoneEquips != null && (zoneEquips.size() > 0)) {
            mModuleListAdapter = new DataArrayAdapter<>(FloorPlanFragment.this.getActivity(), R.layout.listviewitem, createAddressList(zoneEquips));
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

    private ArrayList<String> createAddressList(ArrayList<Equip> equips) {
        Collections.sort(equips, new ModuleComparator());
        ArrayList<String> arrayList = new ArrayList<>();

        for (Equip e : equips) {
            arrayList.add(e.getGroup());

        }
        return arrayList;
    }

    private void enableFloorButton() {
        addFloorlt.setVisibility(View.VISIBLE);
        addFloorBtn.setVisibility(View.VISIBLE);
        addFloorEdit.setVisibility(View.INVISIBLE);
    }


    private void disableRoomModule() {
        addFloorlt.setVisibility(View.INVISIBLE);
        addZonelt.setVisibility(View.INVISIBLE);
        addModulelt.setVisibility(View.INVISIBLE);

        addRoomBtn.setVisibility(View.INVISIBLE);
        addRoomEdit.setVisibility(View.INVISIBLE);
        pairModuleBtn.setVisibility(View.INVISIBLE);
        addModuleEdit.setVisibility(View.INVISIBLE);
    }


    @OnClick(R.id.addFloorBtn)
    public void handleFloorBtn() {
        enableFloorEdit();
        addFloorEdit.setText("");
        addFloorEdit.requestFocus();
        InputMethodManager mgr =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(addFloorEdit, InputMethodManager.SHOW_IMPLICIT);
    }

    @OnClick(R.id.lt_addfloor)
    public void addFloorBtn() {
        enableFloorEdit();
        addFloorEdit.setText("");
        addFloorEdit.requestFocus();
        InputMethodManager mgr =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(addFloorEdit, InputMethodManager.SHOW_IMPLICIT);
    }


    @OnClick(R.id.rl_systemdevice)
    public void systemDeviceOnClick() {
        setSystemSelection(1);
        if (floorList.size() > 0) {
            if (roomList.size() > 0) {
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

        if (updateOAOModule()) {
            moduleListView.setVisibility(View.VISIBLE);
        } else {
            enableModueButton();
        }
    }


    @OnClick(R.id.rl_oao)
    public void oaoOnClick() {
        setSystemSelection(1);
        if (floorList.size() > 0) {
            if (roomList.size() > 0 && mModuleListAdapter != null && mModuleListAdapter.getSelectedPostion() != -1) {
                if (mRoomListAdapter.getSelectedPostion() != -1) {
                    ArrayList<Equip> zoneEquips = HSUtil.getEquips(getSelectedZone().getId());
                    if (zoneEquips.size() > 0) {
                        mModuleListAdapter.setSelectedItem(-1);
                    }
                    mRoomListAdapter.setSelectedItem(-1);
                    addFloorBtn.setEnabled(false);
                    addZonelt.setEnabled(false);
                }
            }
        }
        mFloorListAdapter.setSelectedItem(-1);
        if (updateOAOModule()) {
            pairModuleBtn.setVisibility(View.VISIBLE);
            moduleListView.setVisibility(View.VISIBLE);
        } else {
            enableModueButton();
        }
    }


    @OnClick(R.id.rl_modbus_energy_meter)
    public void onEnergyMeterClick() {
        setSystemSelection(2);
        if (floorList.size() > 0) {
            if (roomList.size() > 0 && mModuleListAdapter != null && mModuleListAdapter.getSelectedPostion() != -1) {
                if (mRoomListAdapter.getSelectedPostion() != -1) {
                    ArrayList<Equip> zoneEquips = HSUtil.getEquips(getSelectedZone().getId());
                    if (zoneEquips.size() > 0) {
                        mModuleListAdapter.setSelectedItem(-1);
                    }
                    //mRoomListAdapter.setSelectedItem(-1);
                    addFloorBtn.setEnabled(false);
                    addZonelt.setEnabled(false);
                }
            }
        }
        mFloorListAdapter.setSelectedItem(-1);
        if (updateEnergyMeterModule()) {
            moduleListView.setVisibility(View.VISIBLE);
        } else {
            enableModueButton();
        }
    }


    @OnClick(R.id.rl_modbus_btu_meter)
    public void onBTUMeterClick() {
        setSystemSelection(3);
        if (floorList.size() > 0) {
            if (roomList.size() > 0 && mModuleListAdapter != null && mModuleListAdapter.getSelectedPostion() != -1) {
                if (mRoomListAdapter.getSelectedPostion() != -1) {
                    ArrayList<Equip> zoneEquips = HSUtil.getEquips(getSelectedZone().getId());
                    if (zoneEquips.size() > 0) {
                        mModuleListAdapter.setSelectedItem(-1);
                    }
                    //mRoomListAdapter.setSelectedItem(-1);
                    addFloorBtn.setEnabled(false);
                    addZonelt.setEnabled(false);
                }
            }
        }
        mFloorListAdapter.setSelectedItem(-1);
        if (updateBTUMeterModule()) {
            moduleListView.setVisibility(View.VISIBLE);
        } else {
            enableModueButton();
        }
    }

    private void setSystemSelection(int position) {

        rl_systemdevice.setBackground(getResources().getDrawable(R.drawable.ic_listselector));
        rl_systemdevice.setEnabled(false);
        textViewSystemDevice.setTextColor(getContext().getResources().getColor(R.color.white));

        if (position == 1) {
            sysyemDeviceType = SysyemDeviceType.OAO;
            rl_oao.setBackground(getResources().getDrawable(R.drawable.ic_listselector));
            textViewOAO.setSelected(true);
            textViewOAO.setTextColor(Color.WHITE);
            rl_oao.setEnabled(false);
        }
        if (position == 2) {
            sysyemDeviceType = SysyemDeviceType.ENERGY_METER;
            rl_modbus_energy_meter.setBackground(getResources().getDrawable(R.drawable.ic_listselector));
            textModbusEnergyMeter.setSelected(true);
            textModbusEnergyMeter.setTextColor(Color.WHITE);
            rl_modbus_energy_meter.setEnabled(false);
        }
        if (position == 3) {
            sysyemDeviceType = SysyemDeviceType.BTU_METER;
            rl_modbus_btu_meter.setBackground(getResources().getDrawable(R.drawable.ic_listselector));
            textModbusBTUMeter.setSelected(true);
            textModbusBTUMeter.setTextColor(Color.WHITE);
            rl_modbus_btu_meter.setEnabled(false);
        }
        if (position != priviousSelectedDevice)
            disablePreviousSelection(position);
        roomListView.setVisibility(View.GONE);
        moduleListView.setVisibility(View.GONE);
        addZonelt.setEnabled(false);
        addRoomBtn.setEnabled(false);
        if (addRoomEdit.getVisibility() == View.VISIBLE) {
            closeAddZoneEditViews();
        }
    }

    public void disablePreviousSelection(int position) {
        /**
         * Disable previous selection
         */
        if (priviousSelectedDevice != 0) {
            if (priviousSelectedDevice == 1) {
                rl_oao.setBackgroundColor(Color.WHITE);
                textViewOAO.setSelected(false);
                textViewOAO.setTextColor(getContext().getResources().getColor(R.color.text_color));
                rl_oao.setEnabled(true);
            }
            if (priviousSelectedDevice == 2) {
                rl_modbus_energy_meter.setBackgroundColor(Color.WHITE);
                textModbusEnergyMeter.setSelected(false);
                textModbusEnergyMeter.setTextColor(getContext().getResources().getColor(R.color.text_color));
                rl_modbus_energy_meter.setEnabled(true);
            }
            if (priviousSelectedDevice == 3) {
                rl_modbus_btu_meter.setBackgroundColor(Color.WHITE);
                textModbusBTUMeter.setSelected(false);
                textModbusBTUMeter.setTextColor(getContext().getResources().getColor(R.color.text_color));
                rl_modbus_btu_meter.setEnabled(true);
            }
        }
        priviousSelectedDevice = position;
    }


    private void closeAddZoneEditViews() {
        addZonelt.setVisibility(View.VISIBLE);
        addRoomEdit.setVisibility(View.INVISIBLE);

        InputMethodManager mgr =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(addRoomEdit.getWindowToken(), 0);
    }

    private void setSystemUnselection() {
        /**
         * System Devices configuration for un-selection
         */
        rl_systemdevice.setBackgroundColor(Color.WHITE);
        rl_systemdevice.setEnabled(true);
        textViewSystemDevice.setTextColor(getContext().getResources().getColor(R.color.text_color));
        textViewSystemDevice.setSelected(false);

        /**
         * OAO Configuration for un-selection
         */
        rl_oao.setBackgroundColor(Color.WHITE);
        textViewOAO.setSelected(false);
        textViewOAO.setTextColor(getContext().getResources().getColor(R.color.text_color));
        rl_oao.setEnabled(true);

        /**
         * Modbus Energy Meter Configuration for un-selection
         */
        rl_modbus_energy_meter.setBackgroundColor(Color.WHITE);
        textModbusEnergyMeter.setSelected(false);
        textModbusEnergyMeter.setTextColor(getContext().getResources().getColor(R.color.text_color));
        rl_modbus_energy_meter.setEnabled(true);

        /**
         * Modbus BTU Meter Configuration for un-selection
         */
        rl_modbus_btu_meter.setBackgroundColor(Color.WHITE);
        textModbusBTUMeter.setSelected(false);
        textModbusBTUMeter.setTextColor(getContext().getResources().getColor(R.color.text_color));
        rl_modbus_btu_meter.setEnabled(true);

        /**
         * Zone Configuration for un-selection
         */
        roomListView.setVisibility(View.VISIBLE);
        moduleListView.setVisibility(View.VISIBLE);
        addZonelt.setEnabled(true);
        addRoomBtn.setEnabled(true);
        priviousSelectedDevice = 0;
    }

    private void enableFloorEdit() {
        addFloorlt.setVisibility(View.INVISIBLE);
        addFloorBtn.setVisibility(View.INVISIBLE);
        addFloorEdit.setVisibility(View.VISIBLE);
        getBuildingFloorsZones("floor");
    }


    @OnEditorAction(R.id.addFloorEdit)
    public boolean handleFloorChange(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (floorToRename != null) {

                floorList.remove(floorToRename);
                for (Floor f : new ArrayList<>(siteFloorList)) {
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
                        adb.setMessage("Floor name already exists in this site,would you like to move all the zones associated with " + floorToRename.getDisplayName() + " to " + hsFloor.getDisplayName() + "?");
                        adb.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
                            if (!CCUHsApi.getInstance().entitySynced(floor.getId())) {
                                hsFloor.setId(CCUHsApi.getInstance().addRemoteFloor(hsFloor, floor.getId()));
                                CCUHsApi.getInstance().setSynced(hsFloor.getId(), floor.getId());
                            }

                            //move zones and modules under new floor
                            for (Zone zone : HSUtil.getZones(floorToRename.getId())) {
                                zone.setFloorRef(CCUHsApi.getInstance().getLUID(floor.getId()));
                                CCUHsApi.getInstance().updateZone(zone, zone.getId());
                                for (Equip q : HSUtil.getEquips(zone.getId())) {
                                    q.setFloorRef(floor.getId());
                                    CCUHsApi.getInstance().updateEquip(q, q.getId());
                                }
                            }

                            refreshScreen();

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
                            InputMethodManager mgr = (InputMethodManager) getActivity()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            mgr.hideSoftInputFromWindow(addFloorEdit.getWindowToken(), 0);

                            refreshScreen();

                            dialog.dismiss();
                        });
                        adb.show();

                        return true;
                    }
                }

                floorList.add(hsFloor);
                CCUHsApi.getInstance().updateFloor(hsFloor, floorToRename.getId());
                refreshScreen();

                InputMethodManager mgr = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.hideSoftInputFromWindow(addFloorEdit.getWindowToken(), 0);

                floorToRename = null;
                L.saveCCUState();
                CCUHsApi.getInstance().syncEntityTree();

                siteFloorList.add(hsFloor);
                return true;
            }

            if (addFloorEdit.getText().toString().length() > 0) {
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
                            if (! CCUHsApi.getInstance().entitySynced(floor.getId())) {
                                hsFloor.setId(CCUHsApi.getInstance().addRemoteFloor(hsFloor, floor.getId()));
                                CCUHsApi.getInstance().setSynced(hsFloor.getId(), floor.getId());
                            }
                            refreshScreen();

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
                            InputMethodManager mgr = (InputMethodManager) getActivity()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            mgr.hideSoftInputFromWindow(addFloorEdit.getWindowToken(), 0);

                            refreshScreen();

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
            } else {
                Toast.makeText(getActivity().getApplicationContext(), "Floor cannot be empty", Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    @OnFocusChange(R.id.addFloorEdit)
    public void handleFloorFocus(View v, boolean hasFocus) {
        if (!hasFocus) {
            enableFloorButton();
        }
    }


    @OnClick(R.id.addRoomBtn)
    public void handleRoomBtn() {
        enableRoomEdit();
        addRoomEdit.setText("");
        addRoomEdit.requestFocus();
        InputMethodManager mgr =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(addRoomEdit, InputMethodManager.SHOW_IMPLICIT);
    }


    @OnClick(R.id.lt_addzone)
    public void addRoomBtn() {
        enableRoomEdit();
        addRoomEdit.setText("");
        addRoomEdit.requestFocus();
        InputMethodManager mgr =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(addRoomEdit, InputMethodManager.SHOW_IMPLICIT);
    }

    private void enableRoomEdit() {
        addZonelt.setVisibility(View.INVISIBLE);
        addRoomBtn.setVisibility(View.INVISIBLE);
        addRoomEdit.setVisibility(View.VISIBLE);
        getBuildingFloorsZones("room");
    }


    @OnFocusChange(R.id.addRoomEdit)
    public void handleRoomFocus(View v, boolean hasFocus) {
        if (!hasFocus) {
            enableRoomBtn();
        }
    }

    public void renameZone(Zone zone) {
        roomToRename = zone;
        enableRoomEdit();
        addRoomEdit.setText(zone.getDisplayName());
        addRoomEdit.requestFocus();
        addRoomEdit.setSelection(zone.getDisplayName().length());

        InputMethodManager mgr =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(addRoomEdit, InputMethodManager.SHOW_IMPLICIT);
    }

    public void renameFloor(Floor floor) {
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
    public boolean handleRoomChange(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {

            if (roomToRename != null) {
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
                if (!siteRoomList.contains(addRoomEdit.getText().toString())) {
                    siteRoomList.add(addRoomEdit.getText().toString());
                }
                return true;
            }

            if (addRoomEdit.getText().toString().length() > 0) {
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
            } else {
                Toast.makeText(getActivity().getApplicationContext(), "Room cannot be empty", Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    @OnClick(R.id.pairModuleBtn)
    public void startPairing() {
        if (mFloorListAdapter.getSelectedPostion() == -1) {
            short meshAddress = L.generateSmartNodeAddress();

            if (priviousSelectedDevice == 1) {

                if (L.ccu().oaoProfile != null) {
                    Toast.makeText(getActivity(), "OAO Module already paired", Toast.LENGTH_LONG).show();
                } else {
                    if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT) {
                        Toast.makeText(getActivity(), "Please set system profile to vav or dab to continue!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    showDialogFragment(FragmentBLEInstructionScreen.getInstance(meshAddress, "SYSTEM", "SYSTEM", ProfileType.OAO, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
                }
            }
            if (priviousSelectedDevice == 2) {
                /**
                 * Modbus energy meter selection
                 */
                if (L.ccu().zoneProfiles.size() > 0) {
                    for (Iterator<ZoneProfile> it = L.ccu().zoneProfiles.iterator(); it.hasNext(); ) {
                        ZoneProfile p = it.next();
                        if (p.getProfileType() == ProfileType.MODBUS_EMR) {
                            Toast.makeText(getActivity(), " Energy Meter already paired", Toast.LENGTH_LONG).show();
                            return;
                        } else {
                            showDialogFragment(FragmentModbusConfiguration
                                    .newInstance(meshAddress, "SYSTEM", "SYSTEM", ProfileType.MODBUS_EMR), FragmentModbusConfiguration.ID);
                        }
                    }
                } else {
                    showDialogFragment(FragmentModbusConfiguration
                            .newInstance(meshAddress, "SYSTEM", "SYSTEM", ProfileType.MODBUS_EMR), FragmentModbusConfiguration.ID);
                }
            }
            if (priviousSelectedDevice == 3) {
                /**
                 * Modbus BTU meter selection
                 */
                showDialogFragment(FragmentModbusConfiguration
                        .newInstance(meshAddress, "SYSTEM", "SYSTEM", ProfileType.MODBUS_BTU), FragmentModbusConfiguration.ID);
            }
            return;
        }

        Zone selectedZone = getSelectedZone();
        ArrayList<Equip> zoneEquips = HSUtil.getEquips(selectedZone.getId());
        boolean isPLCPaired = false;
        boolean isEMRPaired = false;
        boolean isCCUPaired = false;
        boolean isPaired = false;

        if (zoneEquips.size() > 0) {
            isPaired = true;
            for (int i = 0; i < zoneEquips.size(); i++) {
                if (zoneEquips.get(i).getProfile().contains("PLC")) {
                    isPLCPaired = true;
                }
                if (zoneEquips.get(i).getProfile().contains("EMR_ZONE")) {
                    isEMRPaired = true;
                }
                if (zoneEquips.get(i).getProfile().contains("TEMP_INFLUENCE")) {
                    isCCUPaired = true;
                }
            }
        }

        if (!isPLCPaired && !isEMRPaired && !isCCUPaired) {
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
                if (zoneEquips.size() >= 3) {
                    Toast.makeText(getActivity(), "More than 3 modules are not allowed", Toast.LENGTH_LONG).show();
                    return;
                }
                /* Checks to see if emulated and doesn't popup BLE dialogs */

                //This should be moved to pair button for select device type screen.
                showDialogFragment(FragmentSelectDeviceType.newInstance(meshAddress, getSelectedZone().getId(), getSelectedFloor().getId(), isPaired), FragmentSelectDeviceType.ID);
            }
        } else {
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


    private void showDialogFragment(DialogFragment dialogFragment, String id) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(id);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        //TODO: no broadcast recievers
        getActivity().registerReceiver(mPairingReceiver, new IntentFilter(ACTION_BLE_PAIRING_COMPLETED));
        // Create and show the dialog.
        dialogFragment.show(ft, id);
    }


    @OnItemClick(R.id.floorList)
    public void setFloorListView(AdapterView<?> parent, View view, int position, long id) {
        selectFloor(position);
        setSystemUnselection();
    }


    @OnItemClick(R.id.roomList)
    public void setRoomListView(AdapterView<?> parent, View view, int position, long id) {
        selectRoom(position);
    }


    @OnItemClick(R.id.moduleList)
    public void setModuleListView(AdapterView<?> parent, View view, int position, long id) {
        selectModule(position);
    }


    private boolean isFloorAdded() {
        if (((mFloorListAdapter != null && mFloorListAdapter.getSelectedPostion() == -1) &&
                (mRoomListAdapter != null && mRoomListAdapter.getSelectedPostion() != -1)) ||
                (mRoomListAdapter == null || mRoomListAdapter.getSelectedPostion() == -1))
            return true;
        return false;
    }

    private void selectModule(int position) {
        mModuleListAdapter.setSelectedItem(position);
        String nodeAddr = mModuleListAdapter.getItem(position);
        Log.i("Test", "selectModule: " + sysyemDeviceType);
        Log.i("Test", "selectModule: " + mFloorListAdapter.getSelectedPostion());

        if (isFloorAdded()) {
            if (sysyemDeviceType == SysyemDeviceType.OAO) {
                DialogOAOProfile oaoProfiling = DialogOAOProfile.newInstance(Short.parseShort(nodeAddr), "SYSTEM", "SYSTEM");
                showDialogFragment(oaoProfiling, DialogOAOProfile.ID);
            }
            if (sysyemDeviceType == SysyemDeviceType.BTU_METER) {
                showDialogFragment(FragmentModbusConfiguration
                        .newInstance(Short.parseShort(nodeAddr), "SYSTEM", "SYSTEM", ProfileType.MODBUS_BTU), FragmentModbusConfiguration.ID);
            }
            if (sysyemDeviceType == SysyemDeviceType.ENERGY_METER) {
                showDialogFragment(FragmentModbusConfiguration
                        .newInstance(Short.parseShort(nodeAddr), "SYSTEM", "SYSTEM", ProfileType.MODBUS_EMR), FragmentModbusConfiguration.ID);
            }
            return;
        }

        Floor floor = getSelectedFloor();
        Zone zone = getSelectedZone();
        ZoneProfile profile = L.getProfile(Short.parseShort(nodeAddr));
        if (profile != null) {

            switch (profile.getProfileType()) {
                case VAV_REHEAT:
                case VAV_SERIES_FAN:
                case VAV_PARALLEL_FAN:
                    VavProfileConfiguration config = profile.getProfileConfiguration(Short.parseShort(nodeAddr));
                    showDialogFragment(FragmentVAVConfiguration
                            .newInstance(Short.parseShort(nodeAddr), zone.getId(), config.getNodeType(), floor.getId(), profile.getProfileType()), FragmentVAVConfiguration.ID);
                    break;
                case PLC:
                    showDialogFragment(FragmentPLCConfiguration
                            .newInstance(Short.parseShort(nodeAddr), zone.getId(), NodeType.SMART_NODE, floor.getId()), FragmentPLCConfiguration.ID);
                    break;
                case DAB:
                    showDialogFragment(FragmentDABConfiguration
                            .newInstance(Short.parseShort(nodeAddr), zone.getId(), NodeType.SMART_NODE, floor.getId(), profile.getProfileType()), FragmentDABConfiguration.ID);
                    break;
                case DUAL_DUCT:
                    showDialogFragment(FragmentDABDualDuctConfiguration
                            .newInstance(Short.parseShort(nodeAddr), zone.getId(), NodeType.SMART_NODE,
                                    floor.getId(), profile.getProfileType()), FragmentDABDualDuctConfiguration.ID
                    );
                    break;
                case EMR:
                    showDialogFragment(FragmentEMRConfiguration
                            .newInstance(Short.parseShort(nodeAddr), zone.getId(), NodeType.SMART_NODE, floor.getId()), FragmentEMRConfiguration.ID);
                    break;
                case SMARTSTAT_CONVENTIONAL_PACK_UNIT:
                    showDialogFragment(FragmentCPUConfiguration
                            .newInstance(Short.parseShort(nodeAddr), zone.getId(), /*cpuConfig.getNodeType()*/ NodeType.SMART_STAT, floor.getId(), profile.getProfileType()), FragmentCPUConfiguration.ID);
                    break;
                case SMARTSTAT_HEAT_PUMP_UNIT:
                    showDialogFragment(FragmentHeatPumpConfiguration
                            .newInstance(Short.parseShort(nodeAddr), zone.getId(), NodeType.SMART_STAT, floor.getId(), profile.getProfileType()), FragmentHeatPumpConfiguration.ID);
                    break;
                case SMARTSTAT_TWO_PIPE_FCU:
                    showDialogFragment(Fragment2PipeFanCoilUnitConfig
                            .newInstance(Short.parseShort(nodeAddr), zone.getId(), NodeType.SMART_STAT, floor.getId(), profile.getProfileType()), Fragment2PipeFanCoilUnitConfig.ID);
                    break;
                case SMARTSTAT_FOUR_PIPE_FCU:
                    showDialogFragment(Fragment4PipeFanCoilUnitConfig
                            .newInstance(Short.parseShort(nodeAddr), zone.getId(), NodeType.SMART_STAT, floor.getId(), profile.getProfileType()), Fragment4PipeFanCoilUnitConfig.ID);
                    break;
                case TEMP_INFLUENCE:
                    showDialogFragment(FragmentTempInfConfiguration
                            .newInstance(Short.parseShort(nodeAddr), zone.getId(), NodeType.CONTROL_MOTE, floor.getId()), FragmentTempInfConfiguration.ID);
                    break;
                case SSE:
                    showDialogFragment(FragmentSSEConfiguration
                            .newInstance(Short.parseShort(nodeAddr), zone.getId(), NodeType.SMART_NODE, floor.getId(), profile.getProfileType()), FragmentSSEConfiguration.ID);
                    break;
                case MODBUS_EMR_ZONE:
                    showDialogFragment(FragmentModbusEnergyMeterConfiguration
                            .newInstance(Short.parseShort(nodeAddr), zone.getId(), floor.getId(), profile.getProfileType()), FragmentModbusEnergyMeterConfiguration.ID);
                    break;
                case MODBUS_UPS30:
                case MODBUS_UPS80:
                case MODBUS_UPS400:
                case MODBUS_VRF:
                case MODBUS_PAC:
                case MODBUS_RRS:
                case MODBUS_WLD:
                case MODBUS_EM:
                case MODBUS_EMS:
                case MODBUS_ATS:
                case MODBUS_UPS150:
                case MODBUS_BTU:
                    showDialogFragment(FragmentModbusConfiguration
                            .newInstance(Short.parseShort(nodeAddr), zone.getId(), floor.getId(), profile.getProfileType()), FragmentModbusConfiguration.ID);
                    break;

            }
        } else
            Toast.makeText(getActivity(), "Zone profile is empty, recheck your DB", Toast.LENGTH_LONG).show();


    }

    class FloorComparator implements Comparator<Floor> {
        @Override
        public int compare(Floor a, Floor b) {
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        }
    }

    class ZoneComparator implements Comparator<Zone> {
        @Override
        public int compare(Zone a, Zone b) {
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        }
    }

    class ModuleComparator implements Comparator<Equip> {
        @Override
        public int compare(Equip a, Equip b) {
            return a.getGroup().compareToIgnoreCase(b.getGroup());
        }
    }
}