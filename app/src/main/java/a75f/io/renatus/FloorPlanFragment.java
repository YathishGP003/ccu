package a75f.io.renatus;

import static a75f.io.logic.bo.building.NodeType.CONNECTNODE;
import static a75f.io.logic.util.bacnet.BacnetUtilKt.addBacnetTags;
import static a75f.io.logic.util.bacnet.BacnetUtilKt.generateBacnetIdForRoom;
import static a75f.io.logic.util.bacnet.BacnetUtilKt.updateBacnetMstpLinearAndCovSubscription;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import a75f.io.api.haystack.AndroidHSClient;
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
import a75f.io.domain.api.DomainName;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.devices.ConnectNodeDevice;
import a75f.io.logger.CcuLog;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.TaskManager;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.cloud.CloudConnectionManager;
import a75f.io.logic.cloud.CloudConnectionResponseCallback;
import a75f.io.logic.connectnode.ConnectNodeEntitiesBuilder;
import a75f.io.logic.limits.SchedulabeLimits;
import a75f.io.logic.util.bacnet.BacnetUtilKt;
import a75f.io.renatus.bacnet.BacNetSelectModelView;
import a75f.io.renatus.hyperstat.vrv.HyperStatVrvFragment;
import a75f.io.renatus.profiles.acb.AcbProfileConfigFragment;
import a75f.io.renatus.profiles.connectnode.ConnectNodeFragment;
import a75f.io.renatus.profiles.hss.cpu.HyperStatSplitCpuFragment;
import a75f.io.renatus.profiles.dab.DabProfileConfigFragment;
import a75f.io.renatus.profiles.hss.unitventilator.ui.Pipe2UVFragment;
import a75f.io.renatus.profiles.hss.unitventilator.ui.Pipe4UVFragment;
import a75f.io.renatus.profiles.mystat.ui.MyStatCpuFragment;
import a75f.io.renatus.profiles.mystat.ui.MyStatHpuFragment;
import a75f.io.renatus.profiles.mystat.ui.MyStatPipe2Fragment;
import a75f.io.renatus.profiles.ti.TIFragment;
import a75f.io.renatus.profiles.otn.OtnProfileConfigFragment;
import a75f.io.renatus.profiles.sse.SseProfileConfigFragment;
import a75f.io.renatus.profiles.hyperstatv2.ui.HyperStatV2CpuFragment;
import a75f.io.renatus.profiles.hyperstatv2.ui.HyperStatV2Pipe2Fragment;
import a75f.io.renatus.profiles.hyperstatv2.ui.HyperStatV2HpuFragment;
import a75f.io.renatus.profiles.hyperstatv2.ui.HyperStatMonitoringFragment;
import a75f.io.renatus.profiles.plc.PlcProfileConfigFragment;
import a75f.io.renatus.profiles.vav.VavProfileConfigFragment;
import a75f.io.renatus.modbus.ModbusConfigView;
import a75f.io.renatus.modbus.util.ModbusLevel;
import a75f.io.renatus.util.HttpsUtils.HTTPUtils;
import a75f.io.renatus.util.NetworkUtil;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.util.ExecutorTask;
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
    View toastWarning;

    private enum FloorHandledCondition { ALLOW_NEW_FLOOR, ALLOW_RENAMING_FLOOR, ADD_NEW_FLOOR, ADD_RENAMED_FLOOR }

    //SysyemDeviceType sysyemDeviceType;

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

    AndroidHSClient hsLocalClient;

    @BindView(R.id.lt_addfloor)
    LinearLayout addFloorlt;
    @BindView(R.id.lt_addzone)
    LinearLayout addZonelt;
    @BindView(R.id.lt_addModule)
    LinearLayout addModulelt;

    ArrayList<Floor> floorList = new ArrayList<>();
    ArrayList<Zone> roomList = new ArrayList<>();
    private Zone roomToRename;
    private Floor floorToRename;

    public AndroidHSClient hsClientLocal;
    private static FloorPlanFragment instance;
    List<Floor> siteFloorList = new CopyOnWriteArrayList<>();
    List<String> siteRoomList = new CopyOnWriteArrayList<>();
    private FloorListActionMenuListener floorListActionMenuListener;
    private RoomListActionMenuListener roomListActionMenuListener;
    private ModuleListActionMenuListener moduleListActionMenuListener;
    private final BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_BLE_PAIRING_COMPLETED.equals(intent.getAction())) {
                ExecutorTask.executeBackground(() -> {
                    try {
                        if (mFloorListAdapter.getSelectedPostion() == -1) {

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
                });
            }
        }
    };


    private void setScheduleType(String zoneId) {

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            ArrayList<Equip> zoneEquips = HSUtil.getEquips(zoneId);
            if (zoneEquips != null && (!zoneEquips.isEmpty())) {
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
    public static FloorPlanFragment getInstance() {
        if (instance == null) {
           instance = new FloorPlanFragment();
        }
        return instance;
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
        toastWarning = getLayoutInflater().inflate(R.layout.custom_toast_layout_warning, rootView.findViewById(R.id.custom_toast_layout_warning));
        ButterKnife.bind(this, rootView);
        instance = this;
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        enableFloorButton();
        disableRoomModule();
        Globals.getInstance().selectedTab = 0;
        TaskManager.INSTANCE.disposeCurrentTask();
        ImageView toastImage = toastWarning.findViewById(R.id.custom_toast_image);
        TextView SuccessToast = toastWarning.findViewById(R.id.custom_toast_Title);
        TextView CopiedText = toastWarning.findViewById(R.id.custom_toast_message_detail);

        ViewGroup.MarginLayoutParams layoutParams;
        layoutParams = (ViewGroup.MarginLayoutParams) CopiedText.getLayoutParams();
        layoutParams.leftMargin = 0;
        CopiedText.setLayoutParams(layoutParams);

        SuccessToast.setText(R.string.Toast_Success_Title);
        SuccessToast.setTextColor(getResources().getColor(R.color.success_status));
        toastImage.setImageResource(R.drawable.font_awesome_custom_check_mark);
        toastImage.setColorFilter(getResources().getColor(R.color.success_status));
    }


    @Override
    public void onStart() {
        super.onStart();
        floorListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        floorListActionMenuListener = new FloorListActionMenuListener(this);
        floorListView.setMultiChoiceModeListener(floorListActionMenuListener);
        roomListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        roomListActionMenuListener = new RoomListActionMenuListener(this);
        roomListView.setMultiChoiceModeListener(roomListActionMenuListener);
        moduleListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        moduleListActionMenuListener = new ModuleListActionMenuListener(this);
        moduleListView.setMultiChoiceModeListener(moduleListActionMenuListener);
    }


    @Override
    public void onResume() {
        super.onResume();
        if(FloorPlanFragment.this.getUserVisibleHint()) {
            refreshScreen();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override public void onStop() {
        super.onStop();
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
        floorList.sort(new FloorComparator());
        updateFloors();
        CcuLog.i("UI_PROFILING", "FloorPlanFragment.refreshScreen Done");

    }
    public void destroyActionBar() {
            floorListActionMenuListener.destroyActionBar();
            roomListActionMenuListener.destroyActionBar();
            moduleListActionMenuListener.destroyActionBar();
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

        //setSystemUnselection();
        addFloorBtn.setEnabled(true);
        addZonelt.setEnabled(true);

    }


    private void selectFloor(int position) {
        mFloorListAdapter.setSelectedItem(position);
        roomList = HSUtil.getZones(getSelectedFloor().getId());
        roomList.sort(new ZoneComparator());
        updateRooms(roomList);
    }

    private void enableRoomBtn() {
        addZonelt.setVisibility(View.VISIBLE);
        addRoomBtn.setVisibility(View.VISIBLE);
        addRoomEdit.setVisibility(View.INVISIBLE);
    }

    private void updateRooms(ArrayList<Zone> zones) {
        mRoomListAdapter = new DataArrayAdapter<>(this.getActivity(), R.layout.listviewitem, zones, new ArrayList<>(), roomListActionMenuListener.selectedRoom);
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
        ExecutorTask.executeBackground( () -> {
            if (!HTTPUtils.isNetworkConnected()) {
                return;
            }
            HClient hClient = new HClient(CCUHsApi.getInstance().getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
            String siteUID = CCUHsApi.getInstance().getSiteIdRef().toString();

            if (siteUID == null) {
                return;
            }
            //for floor
            HDict tDict = new HDictBuilder().add("filter", "floor and siteRef == " + siteUID).toDict();
            HGrid floorPoint = hClient.call("read", HGridBuilder.dictToGrid(tDict));
            if (floorPoint == null) {
                return;
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
                if (zonePoint != null) {
                    Iterator zit = zonePoint.iterator();
                    siteRoomList.clear();
                    while (zit.hasNext()) {
                        HRow zr = (HRow) zit.next();
                        if (zr.getStr("dis") != null) {
                            siteRoomList.add(zr.getStr("dis"));
                        }
                    }
                    ArrayList<HashMap<Object, Object>> zones =
                            CCUHsApi.getInstance().readAllEntities(Tags.ROOM);
                    for (HashMap<Object, Object> zone : zones) {
                        if (!siteRoomList.contains(zone.get(Tags.DIS).toString())){
                            siteRoomList.add(zone.get(Tags.DIS).toString());
                        }
                    }
                }
            } catch (CallException e) {
                CcuLog.d(L.TAG_CCU_UI, "Failed to fetch room data " + e.getMessage());
                e.printStackTrace();
            }
        });

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
                    CcuLog.d(L.TAG_CCU_UI, "Zone name is null. Floor: " + f + ", Zone: "+zone.getHDict());
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


    private void updateModules(Zone zone) {
        CcuLog.d(L.TAG_CCU_UI, "Zone Selected " + zone.getDisplayName());
        List<Equip> zoneEquips = HSUtil.getEquipsWithoutSubEquips(zone.getId());
        HashMap<Object, Object> connectNodeDevice = ConnectNodeUtil.Companion.getConnectNodeForZone(zone.getId(), CCUHsApi.getInstance());
        if (zoneEquips != null && (!zoneEquips.isEmpty())) {
            mModuleListAdapter = new DataArrayAdapter<>(FloorPlanFragment.this.getActivity(), R.layout.listviewitem, createAddressList(zoneEquips, connectNodeDevice), moduleListActionMenuListener.seletedModules, new ArrayList<>());
            requireActivity().runOnUiThread(() -> moduleListView.setAdapter(mModuleListAdapter));
        } else if (ConnectNodeUtil.Companion.isZoneContainingEmptyConnectNode(zone.getId(), CCUHsApi.getInstance())) {
            mModuleListAdapter = new DataArrayAdapter<>(FloorPlanFragment.this.getActivity(),
                    R.layout.listviewitem, createAddressListByDevice(connectNodeDevice),
                    moduleListActionMenuListener.seletedModules, new ArrayList<>());
            requireActivity().runOnUiThread(() -> moduleListView.setAdapter(mModuleListAdapter));
        } else {
            requireActivity().runOnUiThread(() -> moduleListView.setAdapter(null));
        }
    }

    private ArrayList<String> createAddressList(List<Equip> equips, HashMap<Object, Object> connectNodeDevice) {
        equips.sort(new ModuleComparator());
        ArrayList<String> arrayList = new ArrayList<>();

        if (!connectNodeDevice.isEmpty()) {
            arrayList.add(connectNodeDevice.get("addr").toString());
            return arrayList;
        }

        for (Equip e : equips) {
            arrayList.add(e.getGroup());

        }
        return arrayList;
    }

    private ArrayList<String> createAddressListByDevice(HashMap<Object, Object> connectNodeDevice) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(connectNodeDevice.get("addr").toString() + " (No model configured)");
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
        floorListActionMenuListener.destroyActionBar();
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

    private void enableFloorEdit() {
        addFloorlt.setVisibility(View.INVISIBLE);
        addFloorBtn.setVisibility(View.INVISIBLE);
        addFloorEdit.setVisibility(View.VISIBLE);
        getBuildingFloorsZones("floor");
    }

    @OnEditorAction(R.id.addFloorEdit)
    public boolean handleFloorChange(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            floorListActionMenuListener.destroyActionBar();
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
            if (addFloorEdit.getText().toString().trim().length() > 0) {
                if (addFloorEdit.getText().toString().length() > 24) {
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.floor_name_should_have_less_chars), Toast.LENGTH_SHORT).show();
                    return;
                }
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
                HashMap<Object, Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
                for (Floor floor : siteFloorList) {
                    if (floor.getDisplayName().equals(addFloorEdit.getText().toString().trim())) {
                        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
                        adb.setMessage(getString(R.string.floor_name_should_have_less_chars) + floorToRename.getDisplayName() + getString(R.string.to_string) + hsFloor.getDisplayName() + "?");
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
                                List<HDict> roomPointList = CCUHsApi.getInstance().readAllHDictByQuery("not domainName and point and roomRef == \"" + zone.getId() + "\"");
                                String dis = siteMap.get("dis").toString() + "-" + hsFloor.getDisplayName()+ "-" + zone.getDisplayName();
                                for (HDict pointDict : roomPointList) {
                                    Point point = new Point.Builder().setHDict(pointDict).build();
                                    String pointDisName = point.getDisplayName();
                                    if(pointDisName != null){
                                        String[] splitStr = pointDisName.split("-");
                                        pointDisName = splitStr[splitStr.length-1];
                                    }
                                    point.setDisplayName(dis + "-" + pointDisName);
                                    point.setFloorRef(floor.getId());
                                    CCUHsApi.getInstance().updatePoint(point, point.getId());
                                }
                                for (Equip equipDetails : HSUtil.getEquips(zone.getId())) {
                                    equipDetails.setFloorRef(floor.getId());
                                    CCUHsApi.getInstance().updateEquip(equipDetails, equipDetails.getId());
                                    List<HDict> ponitsList = CCUHsApi.getInstance().readAllHDictByQuery("point and equipRef == \"" + equipDetails.getId() + "\"");
                                    HashMap device = CCUHsApi.getInstance().read("device and equipRef == \"" + equipDetails.getId() + "\"");
                                    if (device != null) {
                                        Device deviceDetails = new Device.Builder().setHashMap(device).build();
                                        deviceDetails.setFloorRef(floor.getId());
                                        CCUHsApi.getInstance().updateDevice(deviceDetails, deviceDetails.getId());
                                    }

                                    for (HDict pointDetailsMap : ponitsList) {
                                        Point pointDetails = new Point.Builder().setHDict(pointDetailsMap).build();
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
                            floorListView.requestFocusFromTouch();
                            siteFloorList.add(hsFloor);
                            dialog.dismiss();
                        });
                        adb.setNegativeButton(getResources().getString(R.string.cancel), (dialog, which) -> {
                            hideKeyboard();
                            refreshScreen();
                            floorListView.requestFocusFromTouch();
                            dialog.dismiss();
                        });
                        adb.show();

                        return;
                    }
                }

                floorList.add(hsFloor);

                CCUHsApi.getInstance().updateFloor(hsFloor, floorToRename.getId());
                updateFloors();
                hideKeyboard();
                floorList.sort(new FloorComparator());
                selectFloor(floorList.indexOf(hsFloor));
                refreshScreen();
                floorToRename = null;
                L.saveCCUState();
                CCUHsApi.getInstance().syncEntityTree();
                siteFloorList.add(hsFloor);
            } else {
                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.floor_cannot_be_empty), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addNewFloor(){
        if ((addFloorEdit.getText().toString().trim().length() > 0)) {
            if (addFloorEdit.getText().toString().length() > 24) {
                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.floor_name_should_have_less_chars), Toast.LENGTH_SHORT).show();
                return;
            }
            HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
            Floor hsFloor = new Floor.Builder()
                    .setDisplayName(addFloorEdit.getText().toString().trim())
                    .setSiteRef(siteMap.get("id").toString())
                    .build();
            for (Floor floor : siteFloorList) {
                if (floor.getDisplayName().equals(addFloorEdit.getText().toString().trim())) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
                    adb.setMessage(getString(R.string.floor_name_already_exists_would_you_like_to_continue));
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
                        floorListView.requestFocusFromTouch();
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
            floorList.sort(new FloorComparator());
            updateFloors();
            selectFloor(floorList.indexOf(hsFloor));
            L.saveCCUState();
            CCUHsApi.getInstance().syncEntityTree();
            floorListView.requestFocusFromTouch();

            hideKeyboard();
            Toast.makeText(getActivity().getApplicationContext(),
                    getString(R.string.floor_text) + addFloorEdit.getText() + getString(R.string.added_text), Toast.LENGTH_SHORT).show();

            HashMap<Object, Object> defaultNamedSchedule =  CCUHsApi.getInstance().readEntity
                    ("named and schedule and default and siteRef == "+CCUHsApi.getInstance().getSiteIdRef().toString());
            if(defaultNamedSchedule.isEmpty()) {
                ExecutorTask.executeBackground(() -> {
                    CCUHsApi.getInstance().importNamedSchedulebySite(new HClient(CCUHsApi.getInstance().getHSUrl(),
                            HayStackConstants.USER, HayStackConstants.PASS), new Site.Builder().setHashMap(CCUHsApi.getInstance().readEntity("site")).build());
                });
            }


            siteFloorList.add(hsFloor);

        } else {
            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.floor_cannot_be_empty), Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getActivity(), getString(R.string.floor_handled_server_is_down), Toast.LENGTH_LONG).show();
    }

    private void isConnectedToServer(FloorHandledCondition condition, Floor floor){
        if (!NetworkUtil.isNetworkConnected(getActivity())) {
            Toast.makeText(getActivity(), getString(R.string.floor_cannot_be_handled), Toast.LENGTH_LONG).show();
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
        floorListActionMenuListener.destroyActionBar();
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
            floorListActionMenuListener.destroyActionBar();
            int maxZoneNameLength = 24;
            if (roomToRename != null) {
                if (addRoomEdit.getText().toString().trim().length() > 0) {
                    for (String z : siteRoomList) {
                        if (z.equals(addRoomEdit.getText().toString().trim())) {
                            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.zone_already_exists) + addRoomEdit.getText(), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    }
                    roomList.remove(roomToRename);
                    siteRoomList.remove(roomToRename.getDisplayName());

                    if ((addRoomEdit.getText().toString().length() > maxZoneNameLength)) {
                        Toast.makeText(getActivity().getApplicationContext(), getString(R.string.zone_name_should_have_less_chars), Toast.LENGTH_SHORT).show();
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
                    roomList.sort(new ZoneComparator());
                    updateRooms(roomList);
                    selectRoom(roomList.indexOf(hsZone));
                    hideKeyboard();
                    roomToRename = null;
                    if (!siteRoomList.contains(addRoomEdit.getText().toString().trim())) {
                        siteRoomList.add(addRoomEdit.getText().toString().trim());
                    }
                    return true;
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.room_cannot_be_empty), Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            if (addRoomEdit.getText().toString().trim().length() > 0) {
                for (String z : siteRoomList) {
                    if (z.equals(addRoomEdit.getText().toString().trim())) {
                        Toast.makeText(getActivity().getApplicationContext(), getString(R.string.zone_already_exists) + addRoomEdit.getText(), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                if ((addRoomEdit.getText().toString().length() > maxZoneNameLength)) {
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.zone_name_should_have_less_chars), Toast.LENGTH_SHORT).show();
                    return true;
                }

                Toast.makeText(getActivity().getApplicationContext(),
                        getString(R.string.room_text) + addRoomEdit.getText() + getString(R.string.added_text), Toast.LENGTH_SHORT).show();
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
                String zoneSchedule = DefaultSchedules.generateDefaultZoneSchedule(zoneId);
                hsZone.setScheduleRef(zoneSchedule);
                HashMap<Object, Object> defaultNamedSchedule =  CCUHsApi.getInstance().readEntity
                        ("named and schedule and default and siteRef == "+CCUHsApi.getInstance().getSiteIdRef().toString());

                if(defaultNamedSchedule.isEmpty()){
                    hsZone.setScheduleRef(zoneSchedule);
                    Toast.makeText(getActivity().getApplicationContext(),
                                getString(R.string.zone_following_zone_schedule_not_available), Toast.LENGTH_SHORT).show();
                }else {
                    hsZone.setScheduleRef(defaultNamedSchedule.get("id").toString());
                }

                int bacnetId = generateBacnetIdForRoom(zoneId);
                hsZone.setBacnetId(bacnetId);
                hsZone.setBacnetType(Tags.DEVICE);
                CCUHsApi.getInstance().updateZone(hsZone, zoneId);
                L.saveCCUStateAsync();
                CCUHsApi.getInstance().syncEntityTree();
                roomList.add(hsZone);
                roomList.sort(new ZoneComparator());
                updateRooms(roomList);
                selectRoom(roomList.indexOf(hsZone));
                hideKeyboard();
                siteRoomList.add(addRoomEdit.getText().toString().trim());
                BacnetUtilKt.addBacnetTags(getActivity().getApplicationContext(), hsZone.getFloorRef(), hsZone.getId());
                return true;
            } else {
                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.room_cannot_be_empty), Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    @OnClick(R.id.pairModuleBtn)
    public void startPairing() {
        floorListActionMenuListener.destroyActionBar();
        roomListActionMenuListener.destroyActionBar();
        addModulelt.setVisibility(View.GONE);
        addModulelt.setEnabled(false);
        disableForMilliSeconds();

        Zone selectedZone = getSelectedZone();
        ArrayList<Equip> zoneEquips = HSUtil.getEquips(selectedZone.getId());
        ArrayList<Device> devices = HSUtil.getDevices(selectedZone.getId());
        boolean isPLCPaired = false;
        boolean isEMRPaired = false;
        boolean isCCUPaired = false;
        boolean isPaired = false;
        boolean isMonitoringPaired = false;
        boolean isOTNPaired = false;
        boolean isConnectNodePaired;

        if (!zoneEquips.isEmpty()) {
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
            }
            if(HSUtil.isZoneHasSubEquips(selectedZone.getId())){
                Toast.makeText(getActivity(), getString(R.string.no_modules_can_be_paired_because_of_modbus),
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        isConnectNodePaired = devices.size() > 0 && devices.get(0).getDomainName() != null && devices.get(0).getDomainName().equalsIgnoreCase(DomainName.connectNodeDevice);

        if (!isPLCPaired && !isEMRPaired && !isCCUPaired && !isMonitoringPaired && !isOTNPaired && !isConnectNodePaired) {
            short meshAddress = L.generateSmartNodeAddress();
            if (mFloorListAdapter.getSelectedPostion() == -1) {
            } else {
                if (zoneEquips.size() >= 3) {
                    Toast.makeText(getActivity(), getString(R.string.more_modules_not_allowed), Toast.LENGTH_LONG).show();
                    return;
                }
                // Checks to see if emulated and doesn't popup BLE dialogs

                //This should be moved to pair button for select device type screen.
                showDialogFragment(FragmentSelectDeviceType.newInstance(meshAddress, getSelectedZone().getId(), getSelectedFloor().getId(), isPaired), FragmentSelectDeviceType.ID);

            }
        } else {
            if (isPLCPaired) {
                Toast.makeText(getActivity(), getString(R.string.pi_loop_already_paired), Toast.LENGTH_LONG).show();
            }
            if (isEMRPaired) {
                Toast.makeText(getActivity(), getString(R.string.energy_meter_module_already_paired), Toast.LENGTH_LONG).show();
            }
            if (isCCUPaired) {
                Toast.makeText(getActivity(), getString(R.string.ccu_zone_already_paired), Toast.LENGTH_LONG).show();
            }
            if (isMonitoringPaired) {
                Toast.makeText(getActivity(), getString(R.string.hyperstat_monitoring_already_paired), Toast.LENGTH_LONG).show();
            }
            if (isOTNPaired) {
                Toast.makeText(getActivity(), getString(R.string.otn_already_paired), Toast.LENGTH_LONG).show();
            }
            if (isConnectNodePaired) {
                Toast.makeText(getActivity(), getString(R.string.connect_node_already_paired), Toast.LENGTH_LONG).show();
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

        selectFloor(position);

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
            return;
        }

        Floor floor = getSelectedFloor();
        Zone zone = getSelectedZone();
        boolean isConnectNode = nodeAddress.contains("No model configured") ||
                !ConnectNodeUtil.Companion.getConnectNodeForZone(zone.getId(), CCUHsApi.getInstance()).isEmpty();

        ZoneProfile profile = isConnectNode ? null : L.getProfile(Long.parseLong(nodeAddress));
        if (profile != null) {
            switch (profile.getProfileType()) {
                case VAV_REHEAT:
                case VAV_SERIES_FAN:
                case VAV_PARALLEL_FAN:
                    Equip equip = profile.getEquip();
                    CcuLog.i(L.TAG_CCU_UI, "equip domainName "+equip.getDomainName()+" "+profile.getProfileType());
                    NodeType nodeType = equip.getDomainName().contains("helionode") ? NodeType.HELIO_NODE : NodeType.SMART_NODE;
                    showDialogFragment(VavProfileConfigFragment.Companion
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), floor.getId(), nodeType, profile.getProfileType()), VavProfileConfigFragment.Companion.getID());
                    break;
                case VAV_ACB:
                    Equip equipHN = profile.getEquip();
                    CcuLog.i(L.TAG_CCU_UI, "equip domainName "+equipHN.getDomainName()+" "+profile.getProfileType());
                    NodeType nodeTypeHN = equipHN.getDomainName().contains("helionode") ? NodeType.HELIO_NODE : NodeType.SMART_NODE;
                    showDialogFragment(AcbProfileConfigFragment.Companion
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), floor.getId(), nodeTypeHN, profile.getProfileType()), AcbProfileConfigFragment.Companion.getID());
                    break;
                case PLC:
                    Equip plcEquip = profile.getEquip();
                    CcuLog.i(L.TAG_CCU_UI, "equip domainName "+plcEquip.getDomainName()+" "+profile.getProfileType());
                    NodeType nodeTypePlc = plcEquip.getDomainName().contains("helionode") ? NodeType.HELIO_NODE : NodeType.SMART_NODE;
                    showDialogFragment(PlcProfileConfigFragment.Companion
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), floor.getId(), nodeTypePlc, ProfileType.PLC), FragmentPLCConfiguration.ID);
                    break;
                case DAB:
                    Equip equipDab = profile.getEquip();
                    CcuLog.i(L.TAG_CCU_UI, "equip domainName "+equipDab.getDomainName()+" "+profile.getProfileType());
                    NodeType nodeTypeDab = equipDab.getDomainName().contains("helionode") ? NodeType.HELIO_NODE : NodeType.SMART_NODE;
                    showDialogFragment(DabProfileConfigFragment.Companion
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), floor.getId(), nodeTypeDab, profile.getProfileType()), DabProfileConfigFragment.Companion.getID());
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
                    showDialogFragment(TIFragment
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(),floor.getId(),NodeType.CONTROL_MOTE, ProfileType.TEMP_INFLUENCE), TIFragment.ID);
                    break;
                case SSE:
                    Equip equipSse = profile.getEquip();
                    CcuLog.i(L.TAG_CCU_UI, "equip domainName "+equipSse.getDomainName()+" "+profile.getProfileType());
                    NodeType helioNode = equipSse.getDomainName().contains("helionode") ? NodeType.HELIO_NODE : NodeType.SMART_NODE;
                    showDialogFragment(SseProfileConfigFragment.Companion
                            .newInstance(Short.parseShort(nodeAddress), zone.getId(), floor.getId(), helioNode, profile.getProfileType()), SseProfileConfigFragment.Companion.getID());
                    break;
                case HYPERSTAT_MONITORING:
                    showDialogFragment(HyperStatMonitoringFragment.Companion.newInstance(Short.parseShort(nodeAddress)
                            , zone.getId(), floor.getId(),NodeType.HYPER_STAT, profile.getProfileType()), HyperStatMonitoringFragment.Companion.getID());
                    break;
                case OTN:
                    showDialogFragment(OtnProfileConfigFragment.Companion.newInstance(Short.parseShort(nodeAddress),
                            zone.getId(), floor.getId(), NodeType.OTN, profile.getProfileType()), OtnProfileConfigFragment.Companion.getID());
                    break;
                case HYPERSTAT_VRV:
                    showDialogFragment(HyperStatVrvFragment.newInstance(Short.parseShort(nodeAddress)
                            , zone.getId(), floor.getId()), HyperStatVrvFragment.ID);
                    break;
                case HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT:
                    showDialogFragment(HyperStatV2CpuFragment.newInstance(Short.parseShort(nodeAddress)
                                    , zone.getId(), floor.getId(),NodeType.HYPER_STAT, profile.getProfileType()),
                            HyperStatV2CpuFragment.ID);
                    break;
                case HYPERSTAT_TWO_PIPE_FCU:
                    showDialogFragment(HyperStatV2Pipe2Fragment.Companion.newInstance(Short.parseShort(nodeAddress),
                                    zone.getId(), floor.getId(), NodeType.HYPER_STAT, ProfileType.HYPERSTAT_TWO_PIPE_FCU),
                            HyperStatV2Pipe2Fragment.ID);
                    break;
                case HYPERSTAT_HEAT_PUMP_UNIT:
                    showDialogFragment(HyperStatV2HpuFragment.newInstance(Short.parseShort(nodeAddress)
                            , zone.getId(), floor.getId(),NodeType.HYPER_STAT, profile.getProfileType()),
                            HyperStatV2HpuFragment.ID);
                    break;
                case HYPERSTATSPLIT_CPU:
                    showDialogFragment(HyperStatSplitCpuFragment.Companion.newInstance(Short.parseShort(nodeAddress)
                                    , zone.getId(), floor.getId(),NodeType.HYPERSTATSPLIT, profile.getProfileType()),
                            HyperStatSplitCpuFragment.Companion.getID());
                    break;
                case MYSTAT_PIPE2:
                    showDialogFragment(MyStatPipe2Fragment.Companion.newInstance(Short.parseShort(nodeAddress)
                                    , zone.getId(), floor.getId(),NodeType.MYSTAT, profile.getProfileType()),
                            MyStatPipe2Fragment.Companion.getID());
                    break;
                case MYSTAT_CPU:
                    showDialogFragment(MyStatCpuFragment.Companion.newInstance(Short.parseShort(nodeAddress)
                                    , zone.getId(), floor.getId(),NodeType.MYSTAT, profile.getProfileType()),
                            MyStatCpuFragment.Companion.getID());
                    break;
                case MYSTAT_HPU:
                    showDialogFragment(MyStatHpuFragment.Companion.newInstance(Short.parseShort(nodeAddress)
                                    , zone.getId(), floor.getId(),NodeType.MYSTAT, profile.getProfileType()),
                            MyStatHpuFragment.Companion.getID());
                    break;
                case HYPERSTATSPLIT_4PIPE_UV:
                    showDialogFragment(Pipe4UVFragment.Companion.newInstance(Short.parseShort(nodeAddress)
                                    , zone.getId(), floor.getId(),NodeType.HYPERSTATSPLIT, profile.getProfileType()),
                            Pipe4UVFragment.Companion.getID());
                    break;
                case HYPERSTATSPLIT_2PIPE_UV:
                    showDialogFragment(Pipe2UVFragment.Companion.newInstance(Short.parseShort(nodeAddress)
                                    , zone.getId(), floor.getId(),NodeType.HYPERSTATSPLIT, profile.getProfileType()),
                            Pipe2UVFragment.Companion.getID());
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
                case BACNET_DEFAULT:
                    showDialogFragment(BacNetSelectModelView.Companion.newInstance(nodeAddress, zone.getId(), floor.getId(), profile.getProfileType(), ModbusLevel.ZONE,""), BacNetSelectModelView.Companion.getID());
                    break;
            }

        } else if (isConnectNode) {
            showDialogFragment(ConnectNodeFragment.Companion.newInstance(Short.parseShort(ConnectNodeUtil.Companion.extractNodeAddressIfConnectPaired(nodeAddress)), zone.getId(),
                    floor.getId(), CONNECTNODE, ProfileType.CONNECTNODE).setIsNewPairing(false), ConnectNodeFragment.Companion.getIdString());
        } else
            Toast.makeText(getActivity(), getString(R.string.zone_profile_empty), Toast.LENGTH_LONG).show();


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
    public void enhancedToastMessage(String message) {
        TextView detailToast = toastWarning.findViewById(R.id.custom_toast_message_detail);
        detailToast.setVisibility(View.VISIBLE);
        toastWarning.setPadding(20,20,20,20);

        String toastMessage = message + getString(R.string.Toast_Success_Message_copied_Configuration);
        SpannableString spannable = new SpannableString(toastMessage);
        spannable.setSpan(
                new StyleSpan(Typeface.BOLD),
                0,
                message.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        ;
        detailToast.setText(spannable);
        detailToast.setTextColor(getResources().getColor(R.color.white));
        Toast toast = new Toast(Globals.getInstance().getApplicationContext());
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 50);
        toast.setView(toastWarning);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }
}
