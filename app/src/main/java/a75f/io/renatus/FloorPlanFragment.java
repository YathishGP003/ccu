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

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.apache.commons.lang3.StringUtils;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.device.bacnet.BacnetUtilKt;
import a75f.io.logger.CcuLog;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;
import a75f.io.logic.cloud.CloudConnectionManager;
import a75f.io.logic.cloud.CloudConnectionResponseCallback;
import a75f.io.logic.limits.SchedulabeLimits;
import a75f.io.renatus.hyperstat.ui.HyperStatFragment;
import a75f.io.renatus.hyperstat.vrv.HyperStatVrvFragment;
import a75f.io.renatus.hyperstatsplit.ui.HyperStatSplitFragment;
import a75f.io.renatus.profiles.acb.AcbProfileConfigFragment;
import a75f.io.renatus.profiles.acb.AcbProfileConfiguration;
import a75f.io.renatus.profiles.vav.VavProfileConfigFragment;
import a75f.io.renatus.modbus.ModbusConfigView;
import a75f.io.renatus.modbus.util.ModbusLevel;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.HttpsUtils.HTTPUtils;
import a75f.io.renatus.util.NetworkUtil;
import a75f.io.renatus.util.ProgressDialogUtils;
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
    private enum FloorHandledCondition { ALLOW_NEW_FLOOR, ALLOW_RENAMING_FLOOR, ADD_NEW_FLOOR, ADD_RENAMED_FLOOR }

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

    ArrayList<Floor> floorList = new ArrayList<>();
    ArrayList<Zone> roomList = new ArrayList<>();
    //
    private Zone roomToRename;
    private Floor floorToRename;
    List<Floor> siteFloorList = new CopyOnWriteArrayList<>();
    List<String> siteRoomList = new CopyOnWriteArrayList<>();
    private FloorListActionMenuListener floorListActionMenuListener;

    private final BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_BLE_PAIRING_COMPLETED.equals(intent.getAction())) {
                new Thread(() -> {
                    try {
                        if (mFloorListAdapter.getSelectedPostion() == -1) {
                            if (sysyemDeviceType == SysyemDeviceType.ENERGY_METER) {
                                requireActivity().runOnUiThread(FloorPlanFragment.this::onEnergyMeterClick);
                            }
                            if (sysyemDeviceType == SysyemDeviceType.OAO) {
                                updateOAOModule();
                            }
                            if (sysyemDeviceType == SysyemDeviceType.BTU_METER) {
                                requireActivity().runOnUiThread(FloorPlanFragment.this::onBTUMeterClick);
                            }


                        } else {
                            updateModules(getSelectedZone());
                            setScheduleType(getSelectedZone().getId());
                        }
                        //Crash here because of activity null while moving to other fragment and return back here after edit config
                        if ((getActivity() != null) && (mPairingReceiver != null))
                            getActivity().unregisterReceiver(mPairingReceiver);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    };

    /**
     * Holding privious selection to Enable/disable the Selection between OAO/EneryMeter/BTUMeter
     */
    int previousSelectedDevice = 0;

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
        floorListActionMenuListener = new FloorListActionMenuListener(this);
        floorListView.setMultiChoiceModeListener(floorListActionMenuListener);
        roomListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        roomListView.setMultiChoiceModeListener(new RoomListActionMenuListener(this));
        moduleListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        moduleListView.setMultiChoiceModeListener(new ModuleListActionMenuListener(this));
    }


    @Override
    public void onResume() {
        super.onResume();
        refreshScreen();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override public void onStop() {
        super.onStop();
        floorListActionMenuListener.dispose();
    }

    // callback from FloorListActionMenuListener
    public void showWait(String message) {
        ProgressDialogUtils.showProgressDialog(requireContext(), message);
    }

    public void hideWait() {
        ProgressDialogUtils.hideProgressDialog();
    }

    // callback from FloorListActionMenuListener
    public void refreshScreen() {
        CcuLog.i("UI_PROFILING", "FloorPlanFragment.refreshScreen");

        floorList = HSUtil.getFloors();
        Collections.sort(floorList, new FloorComparator());
        updateFloors();
        CcuLog.i("UI_PROFILING", "FloorPlanFragment.refreshScreen Done");

    }


    private void updateFloors() {

        mFloorListAdapter = new DataArrayAdapter<>(this.getActivity(), R.layout.listviewitem, floorList);
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
            disableZoneModule();
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

    private void enableRoomBtn() {
        addZonelt.setVisibility(View.VISIBLE);
        addRoomBtn.setVisibility(View.VISIBLE);
        addRoomEdit.setVisibility(View.INVISIBLE);
    }

    private void updateRooms(ArrayList<Zone> zones) {
        mRoomListAdapter = new DataArrayAdapter<>(this.getActivity(), R.layout.listviewitem, zones);
        roomListView.setAdapter(mRoomListAdapter);
        enableRoomBtn();
        if (mRoomListAdapter.getCount() > 0) {
            selectRoom(0);
            enableModueButton();
        } else {
            if (mModuleListAdapter != null) {
                mModuleListAdapter.clear();

            }
            disableModuButton();
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void getBuildingFloorsZones(String enableKeyboard) {
        loadExistingZones();
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
                    e.printStackTrace();
                }


                return null;
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
                if(zone.getDisplayName() != null) {
                    siteRoomList.add(zone.getDisplayName());
                }else {
                    Log.d(L.TAG_CCU_UI, "Zone name is null. Floor: " + f + ", Zone: "+zone.getHDict());
                }
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

    private boolean updateEnergyMeterModule() {
        ArrayList<Equip> equipList = new ArrayList<>();
        for (HashMap m : CCUHsApi.getInstance().readAll("equip and emr and not zone")) {
            equipList.add(new Equip.Builder().setHashMap(m).build());
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
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and btu and not zone");
        ArrayList<Equip> equipList = new ArrayList<>();
        for (HashMap m : equips) {
            equipList.add(new Equip.Builder().setHashMap(m).build());
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
        List<Equip> zoneEquips = HSUtil.getEquipsWithoutSubEquips(zone.getId());
        if (zoneEquips != null && (zoneEquips.size() > 0)) {
            mModuleListAdapter = new DataArrayAdapter<>(FloorPlanFragment.this.getActivity(), R.layout.listviewitem, createAddressList(zoneEquips));
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

    private ArrayList<String> createAddressList(List<Equip> equips) {
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

    private void disableZoneModule() {
        addZonelt.setVisibility(View.INVISIBLE);
        addModulelt.setVisibility(View.INVISIBLE);

        addRoomBtn.setVisibility(View.INVISIBLE);
        addRoomEdit.setVisibility(View.INVISIBLE);
        pairModuleBtn.setVisibility(View.INVISIBLE);
        addModuleEdit.setVisibility(View.INVISIBLE);
    }


    @OnClick(R.id.addFloorBtn)
    public void handleFloorBtn() {
        floorToRename = null;
        isConnectedToServer(FloorHandledCondition.ALLOW_NEW_FLOOR, null);
    }

    private void allowNewFloor(){
        enableFloorEdit();
        addFloorEdit.setText("");
        addFloorEdit.requestFocus();
        showKeyboard(addFloorEdit);
    }


    @OnClick(R.id.lt_addfloor)
    public void addFloorBtn() {
        floorToRename = null;
        enableFloorEdit();
        addFloorEdit.setText("");
        addFloorEdit.requestFocus();
        showKeyboard(addFloorEdit);
    }


    @OnClick(R.id.rl_systemdevice)
    public void systemDeviceOnClick() {

        rl_oao.setVisibility(View.VISIBLE);
        rl_modbus_energy_meter.setVisibility(View.VISIBLE);
        rl_modbus_btu_meter.setVisibility(View.VISIBLE);
        addZonelt.setVisibility(View.GONE); // we are not showing 'add zone+' here.
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
        int background = CCUUiUtil.getListSelectorBackground(getContext());

        rl_systemdevice.setBackgroundResource(background);
        rl_systemdevice.setEnabled(false);
        textViewSystemDevice.setTextColor(getContext().getResources().getColor(R.color.white));

        if (position == 1) {
            sysyemDeviceType = SysyemDeviceType.OAO;

            rl_oao.setBackgroundResource(background);
            textViewOAO.setSelected(true);
            textViewOAO.setTextColor(Color.WHITE);
            rl_oao.setEnabled(false);
        }
        if (position == 2) {
            sysyemDeviceType = SysyemDeviceType.ENERGY_METER;

            rl_modbus_energy_meter.setBackgroundResource(background);
            textModbusEnergyMeter.setSelected(true);
            textModbusEnergyMeter.setTextColor(Color.WHITE);
            rl_modbus_energy_meter.setEnabled(false);
        }
        if (position == 3) {
            sysyemDeviceType = SysyemDeviceType.BTU_METER;

            rl_modbus_btu_meter.setBackgroundResource(background);
            textModbusBTUMeter.setSelected(true);
            textModbusBTUMeter.setTextColor(Color.WHITE);
            rl_modbus_btu_meter.setEnabled(false);
        }
        if (position != previousSelectedDevice)
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
        if (previousSelectedDevice != 0) {
            if (previousSelectedDevice == 1) {
                rl_oao.setBackgroundColor(Color.WHITE);
                textViewOAO.setSelected(false);
                textViewOAO.setTextColor(getContext().getResources().getColor(R.color.text_color));
                rl_oao.setEnabled(true);
            }
            if (previousSelectedDevice == 2) {
                rl_modbus_energy_meter.setBackgroundColor(Color.WHITE);
                textModbusEnergyMeter.setSelected(false);
                textModbusEnergyMeter.setTextColor(getContext().getResources().getColor(R.color.text_color));
                rl_modbus_energy_meter.setEnabled(true);
            }
            if (previousSelectedDevice == 3) {
                rl_modbus_btu_meter.setBackgroundColor(Color.WHITE);
                textModbusBTUMeter.setSelected(false);
                textModbusBTUMeter.setTextColor(getContext().getResources().getColor(R.color.text_color));
                rl_modbus_btu_meter.setEnabled(true);
            }
        }
        previousSelectedDevice = position;
    }


    private void closeAddZoneEditViews() {
        addZonelt.setVisibility(View.VISIBLE);
        addRoomEdit.setVisibility(View.INVISIBLE);
        hideKeyboard();
    }

    private void setSystemUnselection() {
        /*
         * System Devices configuration for un-selection
         */
        rl_systemdevice.setBackgroundColor(Color.WHITE);
        rl_systemdevice.setEnabled(true);
        textViewSystemDevice.setTextColor(getContext().getResources().getColor(R.color.text_color));
        textViewSystemDevice.setSelected(false);

        /*
         * OAO Configuration for un-selection
         */
        rl_oao.setBackgroundColor(Color.WHITE);
        textViewOAO.setSelected(false);
        textViewOAO.setTextColor(getContext().getResources().getColor(R.color.text_color));
        rl_oao.setEnabled(true);

        /*
          Modbus Energy Meter Configuration for un-selection
         */
        rl_modbus_energy_meter.setBackgroundColor(Color.WHITE);
        textModbusEnergyMeter.setSelected(false);
        textModbusEnergyMeter.setTextColor(getContext().getResources().getColor(R.color.text_color));
        rl_modbus_energy_meter.setEnabled(true);

        /*
         * Modbus BTU Meter Configuration for un-selection
         */
        rl_modbus_btu_meter.setBackgroundColor(Color.WHITE);
        textModbusBTUMeter.setSelected(false);
        textModbusBTUMeter.setTextColor(getContext().getResources().getColor(R.color.text_color));
        rl_modbus_btu_meter.setEnabled(true);

        /*
         * Zone Configuration for un-selection
         */
        roomListView.setVisibility(View.VISIBLE);
        moduleListView.setVisibility(View.VISIBLE);
        addZonelt.setEnabled(true);
        addRoomBtn.setEnabled(true);
        previousSelectedDevice = 0;
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
                isConnectedToServer(FloorHandledCondition.ADD_RENAMED_FLOOR, null);
            } else {
                isConnectedToServer(FloorHandledCondition.ADD_NEW_FLOOR, null);
            }
        }
        return false;
    }


    private void addRenamedFloor(){
        if (floorToRename != null) {
            int floorSelectedIndex = this.mFloorListAdapter.getSelectedPostion();
            floorList.remove(floorToRename);
            siteFloorList = siteFloorList.stream().filter(floor -> !floor.getDisplayName().trim().
                    equals(floorToRename.getDisplayName().trim())).collect(Collectors.toList());
            Floor hsFloor = new Floor.Builder()
                    .setDisplayName(addFloorEdit.getText().toString().trim())
                    .setSiteRef(floorToRename.getSiteRef())
                    .build();
            hsFloor.setId(floorToRename.getId());
            hsFloor.setOrientation(floorToRename.getOrientation());
            hsFloor.setFloorNum(floorToRename.getFloorNum());
            hsFloor.setCreatedDateTime(floorToRename.getCreatedDateTime());
            hsFloor.setLastModifiedBy(floorToRename.getLastModifiedBy());
            hsFloor.setLastModifiedDateTime(floorToRename.getLastModifiedDateTime());
            for (Floor floor : siteFloorList) {
                if (floor.getDisplayName().equals(addFloorEdit.getText().toString().trim())) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
                    adb.setMessage("Floor name already exists in this site,would you like to move all the zones associated with " + floorToRename.getDisplayName() + " to " + hsFloor.getDisplayName() + "?");
                    adb.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
                        if (!CCUHsApi.getInstance().isEntityExisting(floor.getId())) {
                            hsFloor.setId(CCUHsApi.getInstance().addRemoteFloor(hsFloor,
                                    StringUtils.stripStart(floor.getId(), "@")));
                            CCUHsApi.getInstance().setSynced(hsFloor.getId());
                        }

                        //move zones and modules under new floor
                        for (Zone zone : HSUtil.getZones(floorToRename.getId())) {
                            zone.setFloorRef(floor.getId());
                            CCUHsApi.getInstance().updateZone(zone, zone.getId());
                            for (Equip equipDetails : HSUtil.getEquips(zone.getId())) {
                                equipDetails.setFloorRef(floor.getId());
                                CCUHsApi.getInstance().updateEquip(equipDetails, equipDetails.getId());
                                ArrayList<HashMap> ponitsList = CCUHsApi.getInstance().readAll("point and equipRef == \"" + equipDetails.getId()+"\"");
                                HashMap device = CCUHsApi.getInstance().read("device and equipRef == \"" + equipDetails.getId()+"\"");
                                if(device !=null ) {
                                    Device deviceDetails = new Device.Builder().setHashMap(device).build();
                                    deviceDetails.setFloorRef(floor.getId());
                                    CCUHsApi.getInstance().updateDevice(deviceDetails,deviceDetails.getId());
                                }

                                for(HashMap pointDetailsMap : ponitsList) {
                                    Point pointDetails = new Point.Builder().setHashMap(pointDetailsMap).build();
                                    pointDetails.setFloorRef(floor.getId());
                                    CCUHsApi.getInstance().updatePoint(pointDetails, pointDetails.getId());
                                }

                            }

                        }

                        refreshScreen();
                        hideKeyboard();
                        floorToRename = null;
                        L.saveCCUState();
                        CCUHsApi.getInstance().syncEntityTree();
                        siteFloorList.add(hsFloor);
                        dialog.dismiss();
                    });
                    adb.setNegativeButton(getResources().getString(R.string.cancel), (dialog, which) -> {
                        hideKeyboard();
                        refreshScreen();
                        dialog.dismiss();
                    });
                    adb.show();

                    return;
                }
            }

            floorList.add(hsFloor);
            CCUHsApi.getInstance().updateFloor(hsFloor, floorToRename.getId());
            refreshScreen();
            hideKeyboard();
            floorToRename = null;
            L.saveCCUState();
            CCUHsApi.getInstance().syncEntityTree();
            siteFloorList.add(hsFloor);
            if(this.floorList.size() == 0 || floorSelectedIndex == -1){
                this.systemDeviceOnClick();
            }
        }
    }

    private void addNewFloor(){
        if (addFloorEdit.getText().toString().length() > 0) {
            int floorSelectedIndex = this.mFloorListAdapter.getSelectedPostion();
            HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
            Floor hsFloor = new Floor.Builder()
                    .setDisplayName(addFloorEdit.getText().toString().trim())
                    .setSiteRef(siteMap.get("id").toString())
                    .build();
            for (Floor floor : siteFloorList) {
                if (floor.getDisplayName().equals(addFloorEdit.getText().toString().trim())) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
                    adb.setMessage("Floor name already exists in this site,would you like to continue?");
                    adb.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
                        if (! CCUHsApi.getInstance().isEntityExisting(floor.getId())) {
                            hsFloor.setId(CCUHsApi.getInstance().addRemoteFloor(hsFloor,
                                    StringUtils.stripStart(floor.getId(), "@")));
                            CCUHsApi.getInstance().setSynced(hsFloor.getId());
                        }

                        refreshScreen();
                        hideKeyboard();
                        floorToRename = null;
                        L.saveCCUState();
                        CCUHsApi.getInstance().syncEntityTree();
                        siteFloorList.add(hsFloor);
                        dialog.dismiss();
                    });

                    adb.setNegativeButton(getResources().getString(R.string.cancel), (dialog, which) -> {
                        hideKeyboard();
                        refreshScreen();
                        dialog.dismiss();
                    });
                    adb.show();

                    return;
                }
            }

            hsFloor.setId(CCUHsApi.getInstance().addFloor(hsFloor));
            floorList.add(hsFloor);
            Collections.sort(floorList, new FloorComparator());
            updateFloors();
            selectFloor(HSUtil.getFloors().size() - 1);
            L.saveCCUState();
            CCUHsApi.getInstance().syncEntityTree();

            hideKeyboard();
            Toast.makeText(getActivity().getApplicationContext(),
                    "Floor " + addFloorEdit.getText() + " added", Toast.LENGTH_SHORT).show();

            HashMap<Object, Object> defaultNamedSchedule =  CCUHsApi.getInstance().readEntity
                    ("named and schedule and default and siteRef == "+CCUHsApi.getInstance().getSiteIdRef().toString());
            if(defaultNamedSchedule.isEmpty()) {
                new Thread(() -> {
                    CCUHsApi.getInstance().importNamedSchedulebySite(new HClient(CCUHsApi.getInstance().getHSUrl(),
                            HayStackConstants.USER, HayStackConstants.PASS), new Site.Builder().setHashMap(CCUHsApi.getInstance().readEntity("site")).build());
                }).start();
            }


            siteFloorList.add(hsFloor);
            if(this.floorList.size() == 0 || floorSelectedIndex == -1){
                this.systemDeviceOnClick();
            }
        } else {
            Toast.makeText(getActivity().getApplicationContext(), "Floor cannot be empty", Toast.LENGTH_SHORT).show();
        }
    }

    @OnFocusChange(R.id.addFloorEdit)
    public void handleFloorFocus(View v, boolean hasFocus) {
        if (!hasFocus) {
            enableFloorButton();
        }
    }

    public void renameFloor(Floor floor) {
        isConnectedToServer(FloorHandledCondition.ALLOW_RENAMING_FLOOR, floor);
    }

    private void allowRenamingFloor(Floor floor){
        floorToRename = floor;
        enableFloorEdit();
        addFloorEdit.setText(floor.getDisplayName());
        addFloorEdit.requestFocus();
        addFloorEdit.setSelection(floor.getDisplayName().length());
        showKeyboard(addFloorEdit);
    }

    public void toastMessageOnServerDown(){
        Toast.makeText(getActivity(), "Floor cannot be handled when server is down", Toast.LENGTH_LONG).show();
    }

    private void isConnectedToServer(FloorHandledCondition condition, Floor floor){
        if (!NetworkUtil.isNetworkConnected(getActivity())) {
            Toast.makeText(getActivity(), "Floor cannot be handled when CCU is offline. Please connect to network.", Toast.LENGTH_LONG).show();
            return;
        }

        CloudConnectionResponseCallback responseCallback = new CloudConnectionResponseCallback() {
            @Override
            public void onSuccessResponse(boolean isOk) {
                if(!isOk){
                    hideWait();
                    toastMessageOnServerDown();
                    return;
                }
                switch (condition) {
                    case ALLOW_NEW_FLOOR:
                        allowNewFloor();
                        break;
                    case ALLOW_RENAMING_FLOOR:
                        allowRenamingFloor(floor);
                        break;
                    case ADD_NEW_FLOOR:
                        addNewFloor();
                        break;
                    case ADD_RENAMED_FLOOR:
                        addRenamedFloor();
                        break;
                }
            }

            @Override
            public void onErrorResponse(boolean isOk) {
                hideWait();
                toastMessageOnServerDown();
            }
        };
        new CloudConnectionManager().processAboutResponse(responseCallback);
    }

    @OnClick(R.id.addRoomBtn)
    public void handleRoomBtn() {
        roomToRename = null;
        enableRoomEdit();
        addRoomEdit.setText("");
        addRoomEdit.requestFocus();
        showKeyboard(addRoomEdit);
    }


    @OnClick(R.id.lt_addzone)
    public void addRoomBtn() {
        roomToRename = null;
        enableRoomEdit();
        addRoomEdit.setText("");
        addRoomEdit.requestFocus();
        showKeyboard(addRoomEdit);
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
            roomToRename = null;
        }
    }

    public void renameZone(Zone zone) {
        roomToRename = zone;
        enableRoomEdit();
        addRoomEdit.setText(zone.getDisplayName());
        addRoomEdit.requestFocus();
        addRoomEdit.setSelection(zone.getDisplayName().length());
        showKeyboard(addRoomEdit);
    }

    @OnEditorAction(R.id.addRoomEdit)
    public boolean handleRoomChange(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            int maxZoneNameLength = 24;
            if (roomToRename != null) {
                for (String z : siteRoomList) {
                   if (z.equals(addRoomEdit.getText().toString().trim())) {
                        Toast.makeText(getActivity().getApplicationContext(), "Zone already exists : " + addRoomEdit.getText(), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                roomList.remove(roomToRename);
                siteRoomList.remove(roomToRename.getDisplayName());

                if ((addRoomEdit.getText().toString().length() >  maxZoneNameLength)) {
                    Toast.makeText(getActivity().getApplicationContext(), "Zone name should have less than 25 characters", Toast.LENGTH_SHORT).show();
                    return true;
                }

                Zone hsZone = new Zone.Builder()
                        .setDisplayName(addRoomEdit.getText().toString().trim())
                        .setFloorRef(roomToRename.getFloorRef())
                        .setSiteRef(roomToRename.getSiteRef())
                        .setScheduleRef(roomToRename.getScheduleRef())
                        .build();

                hsZone.setId(roomToRename.getId());
                hsZone.setBacnetId(roomToRename.getBacnetId());
                hsZone.setBacnetType(roomToRename.getBacnetType());
                CCUHsApi.getInstance().updateZone(hsZone, roomToRename.getId());
                L.saveCCUState();
                CCUHsApi.getInstance().syncEntityTree();
                roomList.add(hsZone);
                Collections.sort(roomList, new ZoneComparator());
                updateRooms(roomList);
                selectRoom(roomList.indexOf(hsZone));
                hideKeyboard();
                roomToRename = null;
                if (!siteRoomList.contains(addRoomEdit.getText().toString().trim())) {
                    siteRoomList.add(addRoomEdit.getText().toString().trim());
                }
                return true;
            }

            if (addRoomEdit.getText().toString().length() > 0) {
                for (String z : siteRoomList) {
                    if (z.equals(addRoomEdit.getText().toString().trim())) {
                        Toast.makeText(getActivity().getApplicationContext(), "Zone already exists : " + addRoomEdit.getText(), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                if ((addRoomEdit.getText().toString().length() > maxZoneNameLength)) {
                    Toast.makeText(getActivity().getApplicationContext(), "Zone name should have less than 25 characters", Toast.LENGTH_SHORT).show();
                    return true;
                }

                Toast.makeText(getActivity().getApplicationContext(),
                        "Room " + addRoomEdit.getText() + " added", Toast.LENGTH_SHORT).show();
                Floor floor = floorList.get(mFloorListAdapter.getSelectedPostion());
                HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);

                Zone hsZone = new Zone.Builder()
                        .setDisplayName(addRoomEdit.getText().toString().trim())
                        .setFloorRef(floor.getId())
                        .setSiteRef(siteMap.get("id").toString())
                        .build();
                String zoneId = CCUHsApi.getInstance().addZone(hsZone);
                SchedulabeLimits.Companion.addSchedulableLimits(false,zoneId,hsZone.getDisplayName());
                hsZone.setId(zoneId);
                DefaultSchedules.setDefaultCoolingHeatingTemp();
                String zoneSchedule = DefaultSchedules.generateDefaultSchedule(true, zoneId);
                HashMap<Object, Object> defaultNamedSchedule =  CCUHsApi.getInstance().readEntity
                        ("named and schedule and default and siteRef == "+CCUHsApi.getInstance().getSiteIdRef().toString());

                if(defaultNamedSchedule.isEmpty()){
                    hsZone.setScheduleRef(zoneSchedule);
                    Toast.makeText(getActivity().getApplicationContext(),
                                "Zone following zone-schedule as default named schedule is not available", Toast.LENGTH_SHORT).show();
                }else {
                    hsZone.setScheduleRef(defaultNamedSchedule.get("id").toString());

                }

                CCUHsApi.getInstance().updateZone(hsZone, zoneId);
                L.saveCCUStateAsync();
                CCUHsApi.getInstance().syncEntityTree();
                roomList.add(hsZone);
                Collections.sort(roomList, new ZoneComparator());
                updateRooms(roomList);
                selectRoom(roomList.indexOf(hsZone));

                hideKeyboard();
                siteRoomList.add(addRoomEdit.getText().toString().trim());
                BacnetUtilKt.addBacnetTags(getActivity().getApplicationContext(), hsZone.getFloorRef(), hsZone.getId());
                return true;
            } else {
                Toast.makeText(getActivity().getApplicationContext(), "Room cannot be empty", Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    @OnClick(R.id.pairModuleBtn)
    public void startPairing() {
        addModulelt.setVisibility(View.GONE);
        addModulelt.setEnabled(false);
        disableForMilliSeconds();
        if (mFloorListAdapter.getSelectedPostion() == -1) {
            short meshAddress = L.generateSmartNodeAddress();

            if (previousSelectedDevice == 1) {

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
            /**
             * Modbus energy meter selection
             **/
            if (previousSelectedDevice == 2) {

                boolean isPaired = isModbusPairedSystem();
                if (isPaired) {
                    Toast.makeText(getActivity(), " Energy Meter already paired", Toast.LENGTH_LONG).show();
                    return;
                } else {
                    showDialogFragment(ModbusConfigView.Companion.newInstance(meshAddress, "SYSTEM", "SYSTEM", ProfileType.MODBUS_EMR, ModbusLevel.SYSTEM,"emr"), ModbusConfigView.Companion.getID());
                }
            }
            if (previousSelectedDevice == 3) {
                /**
                 * Modbus BTU meter selection
                 */
                showDialogFragment(ModbusConfigView.Companion.newInstance(meshAddress, "SYSTEM", "SYSTEM", ProfileType.MODBUS_BTU, ModbusLevel.SYSTEM,"btu"), ModbusConfigView.Companion.getID());
            }
            return;
        }

        Zone selectedZone = getSelectedZone();
        ArrayList<Equip> zoneEquips = HSUtil.getEquips(selectedZone.getId());
        boolean isPLCPaired = false;
        boolean isEMRPaired = false;
        boolean isCCUPaired = false;
        boolean isPaired = false;
        boolean isMonitoringPaired = false;
        boolean isOTNPaired = false;
        boolean isHSSplitPaired = false;

        if (zoneEquips.size() > 0) {
            isPaired = true;
            for (int i = 0; i < zoneEquips.size(); i++) {
                if (zoneEquips.get(i).getProfile().contains("PLC")) {
                    isPLCPaired = true;
                }
                if (zoneEquips.get(i).getProfile().contains("EMR")) {
                    isEMRPaired = true;
                }
                if (zoneEquips.get(i).getProfile().contains("TEMP_INFLUENCE")) {
                    isCCUPaired = true;
                }
                if (zoneEquips.get(i).getProfile().contains("MONITORING")) {
                    isMonitoringPaired = true;
                }
                if (zoneEquips.get(i).getProfile().contains("OTN")) {
                    isOTNPaired = true;
                }
                if (zoneEquips.get(i).getProfile().contains("HYPERSTATSPLIT_CPU")) {
                    isHSSplitPaired = true;
                }
            }
            if(HSUtil.isZoneHasSubEquips(selectedZone.getId())){
                Toast.makeText(getActivity(), "No module can be paired as modbus with sub equips is paired",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (!isPLCPaired && !isEMRPaired && !isCCUPaired && !isMonitoringPaired && !isOTNPaired && !isHSSplitPaired) {
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
            if (isMonitoringPaired) {
                Toast.makeText(getActivity(), "HyperStat Monitoring is already paired in this zone", Toast.LENGTH_LONG).show();
            }
            if (isHSSplitPaired) {
                Toast.makeText(getActivity(), "HyperStat Split is already paired in this zone", Toast.LENGTH_LONG).show();
            }
            if (isOTNPaired) {
                Toast.makeText(getActivity(), "OTN is already paired in this zone", Toast.LENGTH_LONG).show();
            }
            if (isHSSplitPaired) {
                Toast.makeText(getActivity(), "HyperStat Split is already paired in this zone; cannot pair additional modules.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showDialogFragment(DialogFragment dialogFragment, String id) {

        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        Fragment prev = getParentFragmentManager().findFragmentByTag(id);
        if (prev != null) {
            getParentFragmentManager().beginTransaction().remove(prev).commitAllowingStateLoss();
        }
        ft.addToBackStack(null);
        getActivity().registerReceiver(mPairingReceiver, new IntentFilter(ACTION_BLE_PAIRING_COMPLETED));
        // Create and show the dialog.
        dialogFragment.show(ft, id);
    }


    @OnItemClick(R.id.floorList)
    public void setFloorListView(AdapterView<?> parent, View view, int position, long id) {
        rl_oao.setVisibility(View.GONE);
        rl_modbus_energy_meter.setVisibility(View.GONE);
        rl_modbus_btu_meter.setVisibility(View.GONE);

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
        //disabling moduleListView to avoid multiple view creation
        moduleListView.setEnabled(false);
        disableModuleListForMilliSeconds();
    }


    private boolean isFloorAdded() {
        return ((mFloorListAdapter != null && mFloorListAdapter.getSelectedPostion() == -1) &&
                (mRoomListAdapter != null && mRoomListAdapter.getSelectedPostion() != -1)) ||
                (mRoomListAdapter == null || mRoomListAdapter.getSelectedPostion() == -1);
    }

    private void selectModule(int position) {
        mModuleListAdapter.setSelectedItem(position);
        String nodeAddress = mModuleListAdapter.getItem(position);

        if (isFloorAdded()) {
            if (sysyemDeviceType == SysyemDeviceType.OAO) {
                DialogOAOProfile oaoProfiling = DialogOAOProfile.newInstance(Short.parseShort(nodeAddress), "SYSTEM", "SYSTEM");
                showDialogFragment(oaoProfiling, DialogOAOProfile.ID);
            }
            if (sysyemDeviceType == SysyemDeviceType.BTU_METER) {
                showDialogFragment(ModbusConfigView.Companion.newInstance(Short.parseShort(nodeAddress), "SYSTEM", "SYSTEM", ProfileType.MODBUS_BTU, ModbusLevel.SYSTEM,"btu"), ModbusConfigView.Companion.getID());

            }
            if (sysyemDeviceType == SysyemDeviceType.ENERGY_METER) {
                showDialogFragment(ModbusConfigView.Companion.newInstance(Short.parseShort(nodeAddress), "SYSTEM", "SYSTEM", ProfileType.MODBUS_EMR,ModbusLevel.SYSTEM,"emr"), ModbusConfigView.Companion.getID());
            }
            return;
        }

        Floor floor = getSelectedFloor();
        Zone zone = getSelectedZone();
        ZoneProfile profile = L.getProfile(Short.parseShort(nodeAddress));
        if (profile != null) {

            switch (profile.getProfileType()) {
                case VAV_REHEAT:
                case VAV_SERIES_FAN:
                case VAV_PARALLEL_FAN:
                    VavProfileConfiguration config = profile.getProfileConfiguration(Short.parseShort(nodeAddress));
                    showDialogFragment(VavProfileConfigFragment.Companion
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), floor.getId(), profile.getProfileType()), VavProfileConfigFragment.Companion.getID());
                    break;
                case VAV_ACB:
                    showDialogFragment(AcbProfileConfigFragment.Companion
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), floor.getId(), profile.getProfileType()), AcbProfileConfigFragment.Companion.getID());
                    break;
                case PLC:
                    showDialogFragment(FragmentPLCConfiguration
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), NodeType.SMART_NODE, floor.getId()), FragmentPLCConfiguration.ID);
                    break;
                case DAB:
                    showDialogFragment(FragmentDABConfiguration
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), NodeType.SMART_NODE, floor.getId(), profile.getProfileType()), FragmentDABConfiguration.ID);
                    break;
                case DUAL_DUCT:
                    showDialogFragment(FragmentDABDualDuctConfiguration
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), NodeType.SMART_NODE,
                                    floor.getId(), profile.getProfileType()), FragmentDABDualDuctConfiguration.ID
                    );
                    break;
                case EMR:
                    showDialogFragment(FragmentEMRConfiguration
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), NodeType.SMART_NODE, floor.getId()), FragmentEMRConfiguration.ID);
                    break;
                case SMARTSTAT_CONVENTIONAL_PACK_UNIT:
                    showDialogFragment(FragmentCPUConfiguration
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), NodeType.SMART_STAT, floor.getId(), profile.getProfileType()), FragmentCPUConfiguration.ID);
                    break;
                case SMARTSTAT_HEAT_PUMP_UNIT:
                    showDialogFragment(FragmentHeatPumpConfiguration
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), NodeType.SMART_STAT, floor.getId(), profile.getProfileType()), FragmentHeatPumpConfiguration.ID);
                    break;
                case SMARTSTAT_TWO_PIPE_FCU:
                    showDialogFragment(Fragment2PipeFanCoilUnitConfig
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), NodeType.SMART_STAT, floor.getId(), profile.getProfileType()), Fragment2PipeFanCoilUnitConfig.ID);
                    break;
                case SMARTSTAT_FOUR_PIPE_FCU:
                    showDialogFragment(Fragment4PipeFanCoilUnitConfig
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), NodeType.SMART_STAT, floor.getId(), profile.getProfileType()), Fragment4PipeFanCoilUnitConfig.ID);
                    break;
                case TEMP_INFLUENCE:
                    showDialogFragment(FragmentTempInfConfiguration
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), NodeType.CONTROL_MOTE, floor.getId()), FragmentTempInfConfiguration.ID);
                    break;
                case SSE:
                    showDialogFragment(FragmentSSEConfiguration
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), NodeType.SMART_NODE, floor.getId(), profile.getProfileType()), FragmentSSEConfiguration.ID);
                    break;
                case HYPERSTAT_MONITORING:
                    showDialogFragment(HyperStatMonitoringFragment.newInstance(Short.parseShort(nodeAddress)
                            , zone.getId(), floor.getId(), profile.getProfileType()), HyperStatMonitoringFragment.ID);
                    break;
                case OTN:
                    showDialogFragment(FragmentOTNTempInfConfiguration.newInstance(Short.parseShort(nodeAddress),
                            zone.getId(), floor.getId(), profile.getProfileType()), FragmentOTNTempInfConfiguration.ID);
                    break;
                case HYPERSTAT_VRV:
                    showDialogFragment(HyperStatVrvFragment.newInstance(Short.parseShort(nodeAddress)
                        , zone.getId(), floor.getId()), HyperStatMonitoringFragment.ID);
                    break;

                case HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT:
                case HYPERSTAT_TWO_PIPE_FCU:
                case HYPERSTAT_HEAT_PUMP_UNIT:
                    showDialogFragment(HyperStatFragment.newInstance(Short.parseShort(nodeAddress)
                            , zone.getId(), floor.getId(),NodeType.HYPER_STAT, profile.getProfileType()),
                            HyperStatFragment.ID);
                    break;
                case HYPERSTATSPLIT_CPU:
                    showDialogFragment(HyperStatSplitFragment.newInstance(Short.parseShort(nodeAddress)
                                    , zone.getId(), floor.getId(),NodeType.HYPERSTATSPLIT, profile.getProfileType()),
                            HyperStatSplitFragment.ID);
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
                case MODBUS_EMR:
                case MODBUS_ATS:
                case MODBUS_UPS150:
                case MODBUS_BTU:
                case MODBUS_UPS40K:
                case MODBUS_UPSL:
                case MODBUS_UPSV:
                case MODBUS_UPSVL:
                case MODBUS_VAV_BACnet:
                case MODBUS_DEFAULT:
                case MODBUS_EMR_ZONE:
                    showDialogFragment(ModbusConfigView.Companion.newInstance(Short.parseShort(nodeAddress), zone.getId(), floor.getId(), profile.getProfileType(), ModbusLevel.ZONE,""), ModbusConfigView.Companion.getID());
                    break;
            }
        } else
            Toast.makeText(getActivity(), "Zone profile is empty, recheck your DB", Toast.LENGTH_LONG).show();


    }

    static class FloorComparator implements Comparator<Floor> {
        @Override
        public int compare(Floor a, Floor b) {
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        }
    }

    static class ZoneComparator implements Comparator<Zone> {
        @Override
        public int compare(Zone a, Zone b) {
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        }
    }

    static class ModuleComparator implements Comparator<Equip> {
        @Override
        public int compare(Equip a, Equip b) {
            return a.getGroup().compareToIgnoreCase(b.getGroup());
        }
    }


    //Disabling the Pair button for 2 seconds the enabling to avoid double click on pair module
    public void disableForMilliSeconds(){
        int delay = Integer.parseInt(getString(R.string.buttonDesableDelay));
        new Handler(Looper.getMainLooper()).postDelayed(()->{addModulelt.setEnabled(true);addModulelt.setVisibility(View.VISIBLE);},delay);
    }

    private void disableModuleListForMilliSeconds(){
        int delay = Integer.parseInt(getString(R.string.buttonDesableDelay));
        new Handler(Looper.getMainLooper()).postDelayed(()->moduleListView.setEnabled(true), delay);
    }

    private void showKeyboard(EditText editTextView){
        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(editTextView, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard(){
        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(requireView().getWindowToken(), 0);
    }

    private boolean isModbusPairedSystem(){
        return CCUHsApi.getInstance().readAllEntities("equip and modbus and emr and roomRef ==\"SYSTEM\"").size() > 0;
    }
}
