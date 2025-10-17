package a75f.io.renatus;

import static android.view.View.GONE;
import static a75f.io.api.haystack.CCUTagsDb.TAG_CCU_HS;
import static a75f.io.api.haystack.Tags.BACNET;
import static a75f.io.api.haystack.Tags.CONNECTMODULE;
import static a75f.io.api.haystack.util.SchedulableMigrationKt.validateMigration;
import static a75f.io.device.modbus.ModbusModelBuilderKt.buildModbusModel;
import static a75f.io.logic.L.TAG_CCU_INIT;
import static a75f.io.logic.bo.building.dab.DabProfile.CARRIER_PROD;
import static a75f.io.logic.bo.building.definitions.ProfileType.CONNECTNODE;
import static a75f.io.logic.bo.building.definitions.ProfileType.VAV_ACB;
import static a75f.io.logic.bo.building.definitions.ProfileType.VAV_PARALLEL_FAN;
import static a75f.io.logic.bo.building.definitions.ProfileType.VAV_REHEAT;
import static a75f.io.logic.bo.building.definitions.ProfileType.VAV_SERIES_FAN;
import static a75f.io.logic.bo.building.schedules.ScheduleManager.getScheduleStateString;
import static a75f.io.logic.bo.building.schedules.ScheduleUtil.disconnectedIntervals;
import static a75f.io.logic.bo.util.CCUUtils.getTruncatedString;
import static a75f.io.logic.bo.util.CustomScheduleUtilKt.isPointFollowingScheduleOrEvent;
import static a75f.io.logic.bo.util.DesiredTempDisplayMode.setPointStatusMessage;
import static a75f.io.logic.bo.util.RenatusLogicIntentActions.ACTION_SITE_LOCATION_UPDATED;
import static a75f.io.logic.bo.util.UnitUtils.StatusCelsiusVal;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsius;
import static a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsiusTwoDecimal;
import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;
import static a75f.io.logic.util.bacnet.BacnetModelBuilderKt.buildBacnetModel;
import static a75f.io.renatus.schedules.ScheduleUtil.getDayString;
import static a75f.io.renatus.ui.nontempprofiles.helper.BacnetKt.fetchZoneDataForBacnet;
import static a75f.io.renatus.ui.nontempprofiles.helper.BacnetKt.loadBacnetZone;
import static a75f.io.renatus.ui.nontempprofiles.helper.ConnectModuleKt.loadConnectModuleZone;
import static a75f.io.renatus.ui.nontempprofiles.helper.ModbusKt.loadModbusZone;
import static a75f.io.renatus.ui.nontempprofiles.utilities.CcuUtilsKt.cleanUpNonTempViewModel;
import static a75f.io.renatus.ui.nontempprofiles.utilities.CcuUtilsKt.cleanUpTempViewModel;
import static a75f.io.renatus.ui.nontempprofiles.utilities.UiUtilKt.showHeaderViewUI;
import static a75f.io.renatus.ui.tempprofiles.ScheduleUtilKt.doCallBack;
import static a75f.io.renatus.ui.tempprofiles.ScheduleUtilKt.editSchedule;
import static a75f.io.renatus.ui.tempprofiles.ScheduleUtilKt.refreshSchedules;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.core.text.HtmlCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.base.BaseInterval;
import org.projecthaystack.HDict;
import org.projecthaystack.HStr;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.bacnet.parser.BacnetModelDetailResponse;
import a75f.io.api.haystack.bacnet.parser.BacnetZoneViewItem;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.observer.HisWriteObservable;
import a75f.io.api.haystack.observer.PointScheduleUpdateInf;
import a75f.io.api.haystack.observer.PointWriteObservable;
import a75f.io.device.mesh.Pulse;
import a75f.io.device.mesh.hypersplit.HyperSplitMsgReceiver;
import a75f.io.device.mesh.hyperstat.HyperStatMsgReceiver;
import a75f.io.device.mesh.mystat.MyStatMsgReceiverKt;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.equips.hyperstat.MonitoringEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ScheduleType;
import a75f.io.logic.bo.building.dualduct.DualDuctUtil;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.hvac.StandaloneFanStage;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.sscpu.ConventionalPackageUnitUtil;
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage;
import a75f.io.logic.bo.building.system.client.RemotePointUpdateInterface;
import a75f.io.logic.bo.building.truecfm.TrueCFMUtil;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.bo.util.CustomScheduleUtilKt;
import a75f.io.logic.bo.util.TemperatureMode;
import a75f.io.logic.interfaces.ZoneDataInterface;
import a75f.io.logic.jobs.StandaloneScheduler;
import a75f.io.logic.jobs.SystemScheduleUtil;
import a75f.io.logic.schedule.ScheduleGroup;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.util.CommonTimeSlotFinder;
import a75f.io.logic.util.OfflineModeUtilKt;
import a75f.io.logic.util.PreferenceUtil;
import a75f.io.logic.util.uiutils.HyperStatSplitUserIntentHandler;
import a75f.io.logic.util.uiutils.HyperStatUserIntentHandler;
import a75f.io.logic.util.uiutils.MyStatUserIntentHandler;
import a75f.io.messaging.handler.UpdateEntityHandler;
import a75f.io.messaging.handler.UpdatePointHandler;
import a75f.io.renatus.anrwatchdog.ANRHandler;
import a75f.io.renatus.hyperstat.vrv.HyperStatVrvZoneViewKt;
import a75f.io.renatus.model.ZoneViewData;
import a75f.io.renatus.schedules.CustomControlDialog;
import a75f.io.renatus.schedules.NamedSchedule;
import a75f.io.renatus.schedules.ScheduleGroupFragment;
import a75f.io.renatus.schedules.ScheduleUtil;
import a75f.io.renatus.ui.ZoneViewModel;
import a75f.io.renatus.ui.model.HeaderViewItem;
import a75f.io.renatus.ui.nontempprofiles.helper.ZoneData;
import a75f.io.renatus.ui.nontempprofiles.utilities.CcuUtilsKt;
import a75f.io.renatus.ui.nontempprofiles.viewmodel.NonTempProfileViewModel;
import a75f.io.renatus.ui.tempprofiles.OnScheduleChangeListener;
import a75f.io.renatus.ui.tempprofiles.OnScheduleEditClickListener;
import a75f.io.renatus.ui.tempprofiles.OnScheduleViewClickListener;
import a75f.io.renatus.ui.tempprofiles.OnSpecialScheduleEditClickListener;
import a75f.io.renatus.ui.tempprofiles.OnVacationScheduleEditClickListener;
import a75f.io.renatus.ui.tempprofiles.helper.DabHelper;
import a75f.io.renatus.ui.tempprofiles.helper.HyperStatHelper;
import a75f.io.renatus.ui.tempprofiles.helper.HyperStatSplitHelper;
import a75f.io.renatus.ui.tempprofiles.helper.MyStatHelper;
import a75f.io.renatus.ui.tempprofiles.helper.OTNHelper;
import a75f.io.renatus.ui.tempprofiles.helper.SseHelper;
import a75f.io.renatus.ui.tempprofiles.helper.TIHelper;
import a75f.io.renatus.ui.tempprofiles.helper.VavHelper;
import a75f.io.renatus.ui.tempprofiles.viewmodel.TempProfileViewModel;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.GridItem;
import a75f.io.renatus.util.HeartBeatUtil;
import a75f.io.renatus.util.NonTempControl;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.SeekArc;
import a75f.io.renatus.util.TextListAdapter;
import a75f.io.renatus.views.AlertDialogAdapter;
import a75f.io.renatus.views.AlertDialogData;
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter;
import a75f.io.restserver.server.HttpServer;
import a75f.io.util.EntityKVStringParserUtilKt;
import a75f.io.util.ExecutorTask;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Unit;


public class ZoneFragmentNew extends Fragment implements ZoneDataInterface, PointScheduleUpdateInf {
    private static final int DELAY_ZONE_LOAD_MS = 20;
    private static final String LOG_TAG = " ZoneFragmentNew ";
    ExpandableListView expandableListView;
    HashMap<String, List<String>> expandableListDetail;


    ImageView floorMenu;
    public DrawerLayout mDrawerLayout;
    public LinearLayout drawer_screen;
    public ListView lvFloorList;
    public ArrayList<Floor> floorList = new ArrayList<>();
    public DataArrayAdapter<Floor> mFloorListAdapter;
    ArrayList<Zone> roomList = new ArrayList<>();

    private RelativeLayout weather_data = null;
    private TextView place;
    private TextView temperature;
    private TextView weather_condition;
    private ImageView weather_icon;
    private TextView maximumTemp;
    private TextView minimumTemp;
    private TextView note;
    private static Runnable weatherUpdate;
    private Handler weatherUpdateHandler;
    GridLayout gridlayout;
    TableLayout tableLayout;
    private Animation in = null;
    public static final String AIRFLOW_SENSOR = "airflow sensor";

    private int prevPosition = -1;
    private int currentPosition = -1;

    ImageView imag;
    boolean imageOn = false;
    int selectedView = 0;
    int clickedView = -1;
    ArrayList<View> gridItems = new ArrayList<>();
    ArrayList<LinearLayout> tableRows = new ArrayList<>();
    int numRows = 0;
    int columnCount = 4;
    int rowcount = 0;
    View parentRootView;
    Schedule mSchedule = null;
    ScrollView scrollViewParent;

    boolean zoneOpen = false;
    boolean zoneNonTempOpen = false;
    SeekArc seekArcOpen;
    NonTempControl nonTempControlOpen;
    View zonePointsOpen;
    Equip equipOpen;
    HashMap pointsOpen = new HashMap();
    ArrayList<SeekArc> seekArcArrayList = new ArrayList<>();
    ArrayList<TextView> statusTextArrayList = new ArrayList<>();
    ArrayList<View> zoneStatusArrayList = new ArrayList<>();
    boolean isFromPubNub = false;
    boolean isCPUFromPubNub = false;
    boolean isHPUFromPubNub = false;
    boolean isCPUloaded = false;
    boolean isHPUloaded = false;
    ArrayList<HashMap> openZoneMap;
    HashMap<String, Integer> mScheduleTypeMap = new HashMap<>();
    Prefs prefs;

    private final DecimalFormat PRECIPITATION_DECIMAL_FORMAT = new DecimalFormat("#.##");
    private final DecimalFormat HUMIDITY_DECIMAL_FORMAT = new DecimalFormat("#.#");

    TextView zoneLoadTextView = null;

    private ImageView weather_icon_offline;

    private TextView offline_description;

    private BroadcastReceiver siteLocationChangedReceiver;

    private List<BacnetZoneViewItem> bacNetPointsList = new ArrayList<>();

    private Equip visibleEquip;

    ZoneViewModel zoneViewModel = null;

    public ZoneFragmentNew() {
    }

    /**
     * We are currently dependent on deprecated API setUserVisibleHint to enable listeners for updating the temperature
     * and heartbeat. This might get called even before zoneView is drawn completely , all the updates from nodes can
     * delay the loading of zoneView further. Potential fix to this is an additional flag in conjunction with the
     * setUserVisibleHint to make sure the view is ready before we start listening to updates.
     */
    private boolean isZoneViewReady = false;

    private boolean isItemSelectedEvent = false;

    public ArrayList<NonTempProfileViewModel> nonTempProfileViewModels = new ArrayList<>();
    public ArrayList<TempProfileViewModel> tempProfileViewModels = new ArrayList<>();
    public static ZoneFragmentNew newInstance() {
        return new ZoneFragmentNew();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //useCelsius = CCUHsApi.getInstance().readEntity("displayUnit");
        View rootView = inflater.inflate(R.layout.fragment_zones, container, false);
        parentRootView = rootView.findViewById(R.id.zone_fragment_temp);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.onViewCreated");
        isZoneViewReady = false;
        expandableListView = view.findViewById(R.id.expandableListView);
        mDrawerLayout = view.findViewById(R.id.drawer_layout);
        drawer_screen = view.findViewById(R.id.drawer_screen);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        lvFloorList = view.findViewById(R.id.floorList);

        weather_data = view.findViewById(R.id.weather_data);
        place = view.findViewById(R.id.place);
        temperature = view.findViewById(R.id.temperature);
        weather_condition = view.findViewById(R.id.weather_condition);
        weather_icon = view.findViewById(R.id.weather_icon);
        maximumTemp = view.findViewById(R.id.maximumTemp);
        minimumTemp = view.findViewById(R.id.minimumTemp);
        note = view.findViewById(R.id.note);
        weather_icon_offline = view.findViewById(R.id.weather_icon_offline);
        offline_description = view.findViewById(R.id.offlineModeDesc);

        scrollViewParent = view.findViewById(R.id.scrollView_zones);
        tableLayout = view.findViewById(R.id.tableRoot);
        gridlayout = view.findViewById(R.id.gridview);

        in = AnimationUtils.makeInAnimation(getActivity(), false);
        Animation inleft = AnimationUtils.makeInAnimation(getActivity(), true);
        in.setDuration(400);
        inleft.setDuration(400);
        prefs = new Prefs(Globals.getInstance().getApplicationContext());
        expandableListDetail = new HashMap<>();

        floorList = HSUtil.getFloors();
        floorList.sort(new FloorComparator());

        mFloorListAdapter = new DataArrayAdapter<>(getActivity(), R.layout.listviewitem, floorList);
        lvFloorList.setAdapter(mFloorListAdapter);

        zoneLoadTextView = view.findViewById(R.id.zoneLoadTextView);
        zoneLoadTextView.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));

        loadGrid(parentRootView);

        if (floorList != null && !floorList.isEmpty()) {
            lvFloorList.setContentDescription(floorList.get(0).getDisplayName());
        }
        floorMenu = view.findViewById(R.id.floorMenu);
        floorMenu.setOnClickListener(v -> openFloor());

        lvFloorList.setOnItemClickListener((parent, view1, position, id) -> selectFloor(position));

        siteLocationChangedReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                CcuLog.i("CCU_WEATHER","ACTION_SITE_LOCATION_UPDATED ");
                WeatherDataDownloadService.getWeatherData();
                if (weatherUpdateHandler != null)
                    weatherUpdateHandler.post(weatherUpdate);
            }
        };

        getContext().registerReceiver(siteLocationChangedReceiver, new IntentFilter(ACTION_SITE_LOCATION_UPDATED));
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.onViewCreated Done");
        weatherUpdateHandler = new Handler();
        weatherInIt(3000);

        // To resolve app crash issue we have written the below code.
        lvFloorList.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        });
        if(!PreferenceUtil.isDataMigrationPopUpClosed() && isSRMigrationRequired() && !validateMigration()) {
            showMigrationPendingDialog(getActivity());
        }
    }

    private boolean isSRMigrationRequired() {
        return Domain.diagEquip.getMigrationVersion().readDefaultStrVal().isEmpty();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        weatherUpdateHandler.removeCallbacksAndMessages(null);
        if(siteLocationChangedReceiver != null){
            getContext().unregisterReceiver(siteLocationChangedReceiver);
        }
    }

    public void weatherInIt(int delay) {
        weatherUpdate = () -> {
            if (weatherUpdateHandler != null && getActivity() != null) {
                if (weather_data.getVisibility() == View.VISIBLE) {
                    CcuLog.i("weather", "update");
                    UpdateWeatherData();
                }
                weatherUpdateHandler.postDelayed(weatherUpdate, delay);
            }
        };

        weatherUpdate.run();
    }

    public void refreshScreen(String id, boolean isRemoteChange)
    {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.refreshScreen zoneOpen "+zoneOpen);
        if(getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(() -> {
                if(zoneOpen) {
                    try {
                        updateTemperatureBasedZones(seekArcOpen, zonePointsOpen, equipOpen, getLayoutInflater(), isRemoteChange);
                        tableLayout.invalidate();
                    } catch (IllegalStateException e) {
                        //getLayoutInflater() might fail if Fragment is not attached to FragmentManager yet.
                        CcuLog.e(L.TAG_CCU_UI, "Failed to refresh UI ", e);
                    }
                }
                gridlayout.invalidate();
            });
        }
    }


    HashMap<String, View> zoneStatus = new HashMap<>();

    public void refreshHeartBeatStatus(String nodeAddress) {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.refreshHeartBeatStatus zoneOpen "+zoneOpen);
        HashMap<Object, Object> equip = CCUHsApi.getInstance().readEntity("equip and group ==\""+ nodeAddress +"\"");
        if (!equip.isEmpty()) {
            HashMap<Object, Object> zone = CCUHsApi.getInstance().readMapById(equip.get("roomRef").toString());
            ArrayList<HashMap<Object, Object>> equipsInZone = CCUHsApi.getInstance()
                    .readAllEntities("equip and zone and roomRef ==\""
                            + zone.get("id") + "\"");
            if(!equipsInZone.isEmpty()) {
                boolean isZoneAlive = HeartBeatUtil.isZoneAlive(equipsInZone);
                View statusView  = zoneStatus.get(zone.get("dis").toString());
                if (statusView != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> HeartBeatUtil.zoneStatus(statusView, isZoneAlive));
                }
            }
        }
    }

    @Override
    public void updateBacnetUi(String id) {

    }

    public void refreshDesiredTemp(String nodeAddress, String pointcoolDT1, String pointheatDT1,
                                   String roomRef) {
        if (getActivity() != null ) {
            int i;
            for (i = 0; i < seekArcArrayList.size(); i++) {
                GridItem gridItem = (GridItem) seekArcArrayList.get(i).getTag();
                if (gridItem.getNodeAddress() == Short.parseShort(nodeAddress)) {
                    double pointHeatDT;
                    if (CCUUiUtil.isDomainEquip(nodeAddress, "node")) {
                        pointHeatDT = CCUUiUtil.readPriorityValByGroupId(DomainName.desiredTempHeating, nodeAddress);
                    } else {
                        pointHeatDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and heating and group == \"" + nodeAddress + "\"");
                    }

                    if(pointHeatDT == 0.0){
                        CcuLog.d(TAG_CCU_HS, "desiredTempHeating point val is 0.0");
                        return;
                    }
                    SeekArc tempSeekArc = seekArcArrayList.get(i);

                    double pointheatDT;
                    double pointcoolDT;
                        if (CCUUiUtil.isDomainEquip(nodeAddress, "node")) {
                            pointheatDT = CCUUiUtil.readPriorityValByGroupId(DomainName.desiredTempHeating, nodeAddress);
                            pointcoolDT = CCUUiUtil.readPriorityValByGroupId(DomainName.desiredTempCooling, nodeAddress);
                        } else {
                            pointheatDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and heating and group == \"" + nodeAddress + "\"");
                            pointcoolDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and cooling and group == \"" + nodeAddress + "\"");
                        }

                    int currentModeType = CCUHsApi.getInstance().readHisValByQuery("hvacMode and roomRef == \""
                            + roomRef + "\"").intValue();
                    if ((tempSeekArc.getCoolingDesiredTemp() != pointcoolDT) || (tempSeekArc.getHeatingDesiredTemp() != pointheatDT)
                            || tempSeekArc.getTemperatureMode() != currentModeType) {
                        getActivity().runOnUiThread(() -> {
                            CcuLog.d(LOG_TAG + "Scheduler", "refreshDesiredTemp22 =" + pointcoolDT + "," + pointheatDT + "," + nodeAddress);
                            tempSeekArc.setCoolingDesiredTemp((float) pointcoolDT, false);
                            tempSeekArc.setHeatingDesiredTemp((float) pointheatDT, false);
                            tempSeekArc.invalidate();
                        });
                    }
                }
            }
        }
    }

    public void refreshScreenbySchedule(String nodeAddress, String equipId, String zoneId) {
        if (getActivity() != null) {
            int i;
            String status = ScheduleManager.getInstance().getMultiModuleZoneStatusMessage(zoneId);
            String vacationStatus = ScheduleManager.getInstance().getVacationStateString(zoneId);
            String specialScheduleStatus = getScheduleStateString(zoneId);
            for (i = 0; i < zoneStatusArrayList.size(); i++) {
                GridItem gridItem = (GridItem) zoneStatusArrayList.get(i).getTag();
                if (gridItem.getNodeAddress() == Short.parseShort(nodeAddress)) {
                    View tempZoneDetails = zoneStatusArrayList.get(i);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView scheduleStatus = tempZoneDetails.findViewById(R.id.schedule_status_tv);
                            TextView vacationStatusTV = tempZoneDetails.findViewById(R.id.vacation_status);
                            TextView specialScheduleStatusText = tempZoneDetails.findViewById(R.id.special_status_status);
                            vacationStatusTV.setText(vacationStatus);
                            specialScheduleStatusText.setText(specialScheduleStatus);
                            int temperatureMode = CCUHsApi.getInstance().readHisValByQuery("zone and hvacMode and roomRef" +
                                    " == \"" + zoneId + "\"").intValue();
                            try {
                            if(isCelsiusTunerAvailableStatus()) {
                                scheduleStatus.setText(StatusCelsiusVal(status, temperatureMode));
                            } else {
                                scheduleStatus.setText(setPointStatusMessage(status, TemperatureMode.values()[temperatureMode]));
                            }
                            } catch (Exception e) {
                            }
                        }
                    });
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void updateTemperature(double currentTemp, short nodeAddress) {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.updateTemperature");
        if (getActivity() != null) {
            int i;
            if (currentTemp > 0) {
                for (i = 0; i < seekArcArrayList.size(); i++) {
                    GridItem gridItem = (GridItem) seekArcArrayList.get(i).getTag();
                    ArrayList<Short> zoneNodes = gridItem.getZoneNodes();
                    CcuLog.i(LOG_TAG + "CurrentTemp", "SensorCurrentTemp:" + currentTemp + " Node:" + nodeAddress + " zoneNodes:" + zoneNodes);
                    if (zoneNodes != null && !zoneNodes.isEmpty() && zoneNodes.contains(nodeAddress)) {
                        SeekArc tempSeekArc = seekArcArrayList.get(i);
                        ExecutorTask.executeBackground(() -> {
                            double currentTempSensor = 0;
                            double noTempSensor = 0;
                            final double currentTempVal ;
                            ArrayList<HashMap> zoneEquips = gridItem.getZoneEquips();
                            for (int j = 0; j < zoneEquips.size(); j++) {
                                Equip tempEquip = new Equip.Builder().setHashMap(zoneEquips.get(j)).build();
                                int statusVal;
                                if (CCUUiUtil.isDomainEquip(tempEquip.getId(), "equip")) {
                                    statusVal = CCUUiUtil.readHisValByEquipRef(DomainName.equipStatus, tempEquip.getId()).intValue();
                                } else {
                                    statusVal = CCUHsApi.getInstance().readHisValByQuery("point and status and not ota and his and not writable and equipRef ==\"" + tempEquip.getId() + "\"").intValue();
                                }
                                if (statusVal != ZoneState.TEMPDEAD.ordinal() && statusVal != ZoneState.RFDEAD.ordinal()) {
                                    double avgTemp = CCUHsApi.getInstance().readHisValByQuery("(point and domainName == \"" + DomainName.currentTemp + "\" and equipRef == \"" + tempEquip.getId() + "\") or " +
                                            "(temp and sensor and (current or space) and equipRef == \"" + tempEquip.getId() + "\")");
                                    currentTempSensor = (currentTempSensor + avgTemp);
                                } else {
                                    noTempSensor++;
                                }
                            }
                            if (currentTempSensor > 0 && zoneEquips.size() > 1) {
                                currentTempSensor = currentTempSensor / (zoneEquips.size() - noTempSensor);
                                DecimalFormat decimalFormat = new DecimalFormat("#.#");
                                currentTempSensor = Double.parseDouble(decimalFormat.format(Math.round(currentTempSensor * 10.0) / 10.0));
                            }
                            currentTempVal = currentTempSensor;
                            if (currentTempSensor > 0) {
                                getActivity().runOnUiThread(
                                        () ->{
                                            CcuLog.i(LOG_TAG + "CurrentTemp", "SensorCurrentTemp:" + currentTempVal + " Node:" + nodeAddress + " zoneNodes:" + zoneNodes);
                                            tempSeekArc.setCurrentTemp((float) (currentTempVal));
                                            tempSeekArc.invalidate();
                                            CcuLog.e("UI_PROFILING","ZoneFragmentNew.updateTemperature Arc updated");
                                        }
                                );
                            }
                        });
                        break;
                    } else {
                        CcuLog.e("UI_PROFILING","ZoneFragmentNew.updateTemperature zoneNodes is null or empty");
                    }
                }
            }
        }
        isItemSelectedEvent = false;
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.updateTemperature Done");
    }

    public void updateSensorValue(short nodeAddress) {
        if (getActivity() != null) {
            if (zoneNonTempOpen) {
                if (equipOpen.getProfile().contains("PLC") || equipOpen.getProfile().startsWith("EMR") || equipOpen.getProfile().contains("monitor")) {
                    getActivity().runOnUiThread(() -> {
                          tableLayout.invalidate();
                    });
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        lvFloorList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    }

    private void updateView(TextView textView, String value){
        if(textView != null){
            textView.setText(value);
        }
    }

    public void UpdateWeatherData() {
        String forMatValue = "%4.0f";

        if(OfflineModeUtilKt.isOfflineMode()){

            place.setVisibility(View.GONE);
            weather_icon.setVisibility(View.GONE);
            weather_icon_offline.setVisibility(View.VISIBLE);
            temperature.setVisibility(View.GONE);
            weather_condition.setVisibility(View.GONE);
            maximumTemp.setVisibility(View.GONE);
            minimumTemp.setVisibility(View.GONE);
            note.setVisibility(View.GONE);
            offline_description.setVisibility(View.VISIBLE);
            offline_description.setText(R.string.offline_description);

        }else if(WeatherDataDownloadService.getMinTemperature() != 0.0 && WeatherDataDownloadService.getMaxTemperature() != 0.0) {
            if (isCelsiusTunerAvailableStatus()) {
                updateView(temperature, String.format(Locale.ENGLISH, forMatValue, fahrenheitToCelsius(WeatherDataDownloadService.getTemperature())));
                updateView(maximumTemp, String.format(Locale.ENGLISH, forMatValue, fahrenheitToCelsius(WeatherDataDownloadService.getMaxTemperature())));
                updateView(minimumTemp, String.format(Locale.ENGLISH, forMatValue, fahrenheitToCelsius(WeatherDataDownloadService.getMinTemperature())));
            } else {
                updateView(temperature, String.format(Locale.ENGLISH, forMatValue, WeatherDataDownloadService.getTemperature()));
                updateView(maximumTemp, String.format(Locale.ENGLISH, forMatValue, WeatherDataDownloadService.getMaxTemperature()));
                updateView(minimumTemp, String.format(Locale.ENGLISH, forMatValue, WeatherDataDownloadService.getMinTemperature()));
            }

            double weatherPrecipitation = WeatherDataDownloadService.getPrecipitation();
            double weatherHumidity = WeatherDataDownloadService.getHumidity() * 100;

            double formattedPrecipitation = Double.parseDouble(PRECIPITATION_DECIMAL_FORMAT.format(weatherPrecipitation));
            double formattedHumidity = Double.parseDouble(HUMIDITY_DECIMAL_FORMAT.format(weatherHumidity));

            updateView(note, "Humidity : " + formattedHumidity + "%" + "\n" + "Precipitation : " + formattedPrecipitation);
            SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext());
            String city = spDefaultPrefs.getString("city", "");
            String country = spDefaultPrefs.getString("country", "");
            String state = spDefaultPrefs.getString("state", "");
            if(state.isEmpty()){
                state = CCUHsApi.getInstance().getSite().getGeoState();
            }
            updateView(place, city + ", " + state + ", " + country);
            updateView(weather_condition, WeatherDataDownloadService.getSummary());

            final ImageView ivWeatherIcon = weather_icon;
            if(ivWeatherIcon != null){
                String weatherIconId = WeatherDataDownloadService.getIcon();
                Context context = ivWeatherIcon.getContext();
                int id = context.getResources().getIdentifier("weather_icon_" + weatherIconId, "drawable", context.getPackageName());
                ivWeatherIcon.setImageResource(id);
            }
        }
    }

    private void selectFloor(int position) {
        mFloorListAdapter.setSelectedItem(position);
        closeFloorAndShowWeather( () -> {
            roomList = HSUtil.getZones(floorList.get(position).getId());
            loadGrid(parentRootView);
            lvFloorList.setContentDescription(floorList.get(position).getDisplayName());
            expandableListView.invalidateViews();
        });
    }



    public void openFloor() {
        try {
            mDrawerLayout.openDrawer(drawer_screen);
        } catch (Exception e) {
            e.printStackTrace();
            if (mDrawerLayout != null && !mDrawerLayout.isShown()) {
                mDrawerLayout.openDrawer(drawer_screen);
            }
        }
    }

    public void closeFloorAndShowWeather(Runnable onAnimationEnd) {
        if (mDrawerLayout != null && drawer_screen != null) {
            try {
                mDrawerLayout.closeDrawer(drawer_screen);
            } catch (Exception e) {
                if (mDrawerLayout.isShown()) {
                    mDrawerLayout.closeDrawer(drawer_screen);
                }
                e.printStackTrace();
            }

            mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                @Override
                public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {}

                @Override
                public void onDrawerOpened(@NonNull View drawerView) {}

                @Override
                public void onDrawerClosed(@NonNull View drawerView) {
                    // Remove listener immediately after closing the drawer
                    mDrawerLayout.removeDrawerListener(this);
                    showWeather(onAnimationEnd);
                }

                @Override
                public void onDrawerStateChanged(int newState) {}
            });
        } else {
            showWeather(onAnimationEnd);
        }
    }

    private void showWeather(Runnable onAnimationEnd) {
        weather_data.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(-weather_data.getWidth(), 0, 0, 0);
        animate.setDuration(400);
        animate.setFillAfter(true);

        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                if (onAnimationEnd != null) {
                    onAnimationEnd.run();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        weather_data.startAnimation(animate);
        clickedView = -1;
        tableLayout.removeAllViews();
        tableLayout.invalidate();
        zoneLoadTextView.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
        zoneLoadTextView.setVisibility(View.VISIBLE);
    }

    int gridPosition = 0;
    private void loadGrid(View rootView) {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.loadGrid");
        rowcount = 0;
        if (!floorList.isEmpty()) {
            ArrayList<HashMap<Object, Object>> roomList =
                CCUHsApi.getInstance().readAllEntities("room and floorRef == \"" + floorList.get(mFloorListAdapter.getSelectedPostion()).getId() + "\"");
            imag = new ImageView(getActivity());
            tableLayout.removeAllViews();
            gridItems.clear();
            tableRows.clear();
            LinearLayout rowLayout;
            numRows = (roomList.size() / columnCount);
            if (roomList.size() % columnCount != 0)
                numRows++;

            //Button[] buttons = new Button[itemNames.length];
            if (numRows > 0) {
                LinearLayout[] tablerowLayout = new LinearLayout[numRows];
                for (int j = 0; j < numRows; j++) {
                    rowLayout = new LinearLayout(getActivity());
                    tableRows.add(rowLayout);
                }
                tablerowLayout[0] = new LinearLayout(tableLayout.getContext());

                gridPosition = 0;
                Handler zoneLoadHandler = new Handler(Looper.getMainLooper());
                for (int m = 0; m < roomList.size(); m++) {
                    int zoneIndex = m;
                    zoneLoadHandler.postDelayed(() -> {
                        try {
                            CcuLog.d(TAG_CCU_INIT, "Loading Zone "+zoneIndex);
                            loadZone(rootView, tablerowLayout, roomList.get(zoneIndex));
                            if(zoneIndex == roomList.size() - 1) {
                                CcuLog.d(TAG_CCU_INIT, "Loading Zone Completed");
                                setCcuReady();
                            }
                        } catch (Exception e) {
                            CcuLog.e(TAG_CCU_INIT, "Loading Zone failed");
                            e.printStackTrace();
                        }
                    }, DELAY_ZONE_LOAD_MS);
                }
            } else {
                setCcuReady();
            }
        } else {
            setCcuReady();
        }
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.loadGrid Done");
    }

    private void setCcuReady() {
        CcuLog.i("UI_PROFILING","zoneViewready set");
        isZoneViewReady = true;
        CCUHsApi.getInstance().setCcuReady();
        setListeners();
        zoneLoadTextView.setVisibility(View.GONE);
        if (ANRHandler.isAnrWatchdogEnabled()) {
            //Configure ANR Watchdog with a delay so that app startup UI load does not trigger unwanted ANRs.
            new Handler(Looper.getMainLooper()).postDelayed(ANRHandler::configureANRWatchdog, 10000);
        }
        if(PreferenceUtil.getIsCcuLaunched()) {
            try {
                Toast.makeText(getContext(), "CCU Ready", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                //ignore as this might fail if activity is not in foreground
                CcuLog.e("UI_PROFILING", "Failed to show CCU Ready toast", e);
                e.printStackTrace();
            }
            CCUUtils.setCCUReadyProperty("true");
            PreferenceUtil.setIsCcuLaunched(false);
        }
    }

    private int loadZone(View rootView, LinearLayout[] tablerowLayout, HashMap roomMap) {

        String zoneTitle;
        LayoutInflater inflater = LayoutInflater.from(getContext());

        zoneTitle = roomMap.get("dis").toString();
        ArrayList<HashMap<Object, Object>> equips =
            CCUHsApi.getInstance().readAllEntities("equip and zone and roomRef ==\"" + roomMap.get("id").toString() + "\"");
        if (!equips.isEmpty()) {// zones has devices paired
            boolean isZoneAlive = HeartBeatUtil.isZoneAlive(equips);
            HashMap<Object, Object> connectNode = ConnectNodeUtil.Companion.getConnectNodeForZone(roomMap.get("id").toString(), CCUHsApi.getInstance());
            if (!connectNode.isEmpty()) {
                isZoneAlive = CCUUtils.isConnectModuleAlive(connectNode.get(Tags.ID).toString());
            }
            HashMap<String, ArrayList<HashMap>> zoneData = new HashMap<>();
            for (HashMap zoneModel : equips) {
                if (zoneData.containsKey(zoneModel.get("roomRef").toString())) {
                    ArrayList<HashMap> exisiting = zoneData.get(zoneModel.get("roomRef").toString());
                    exisiting.add(zoneModel);
                    zoneData.put(zoneModel.get("roomRef").toString(), exisiting);
                } else {
                    ArrayList<HashMap> newData = new ArrayList<>();
                    newData.add(zoneModel);
                    zoneData.put(zoneModel.get("roomRef").toString(), newData);
                }
            }
            CcuLog.d(LOG_TAG + "ZonesMap", "Size:" + zoneData.size() + " Data:" + zoneData);
            for (ArrayList<HashMap> equipZones : zoneData.values()) {

                String profileType = "";

                String profileVAV = "VAV";
                String profileDAB = "DAB";
                String profileSSE = "SSE";
                String profileSmartStat = "SMARTSTAT";
                String profileEM = "EMR";
                String profilePLC = "PLC";
                String profileTempMonitor = "TEMP_MONITOR";
                String profileTempInfluence = "TEMP_INFLUENCE";
                String profileDualDuct = "DUAL_DUCT";
                String profileModBus = "MODBUS";
                String profileBacnet = "BACNET_DEFAULT";
                String profileHyperStatMonitoring = "HYPERSTAT_MONITORING";
                String profileOTN = "OTN";
                String profileConnectNode = CONNECTNODE.name();

                boolean tempModule = false;
                boolean nontempModule = false;
                for (HashMap equipTypes : equipZones) {
                    profileType = equipTypes.get("profile").toString();
                    CcuLog.e(LOG_TAG + "RoomData", "ProfileType:" + profileType);
                    if (!profileType.contains(profileModBus) &&
                            profileType.contains(profileVAV) ||
                            profileType.contains(profileDAB) ||
                            profileType.contains(profileSSE) ||
                            profileType.contains(profileSmartStat) ||
                            profileType.contains(profileTempInfluence) ||
                            profileType.contains(profileDualDuct) ||
                            profileType.contains(ProfileType.HYPERSTAT_VRV.name()) ||
                            profileType.contains(profileOTN)||
                            profileType.contains(ProfileType.HYPERSTATSPLIT_CPU.name())||
                            profileType.contains(ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT.name())||
                            profileType.contains(ProfileType.HYPERSTAT_TWO_PIPE_FCU.name())||
                            profileType.contains(ProfileType.HYPERSTAT_HEAT_PUMP_UNIT.name())||
                            profileType.contains(ProfileType.HYPERSTATSPLIT_CPU.name())||
                            profileType.contains(ProfileType.MYSTAT_CPU.name())||
                            profileType.contains(ProfileType.MYSTAT_HPU.name())||
                            profileType.contains(ProfileType.MYSTAT_PIPE2.name())||
                            profileType.contains(ProfileType.HYPERSTATSPLIT_2PIPE_UV.name()) ||
                            profileType.contains(ProfileType.HYPERSTATSPLIT_4PIPE_UV.name()))
                    {
                        tempModule = true;
                    }
                    if (profileType.contains(profileEM) || profileType.contains(profilePLC)
                            || profileType.contains(profileTempMonitor)
                            || profileType.contains(profileModBus)
                            || profileType.contains(profileBacnet)
                            || profileType.contains(profileConnectNode)
                    ) {
                        nontempModule = true;
                    }
                }
                if (profileType.contains(profileHyperStatMonitoring)) {
                    viewMonitoringZone(inflater, rootView, equipZones, zoneTitle, gridPosition, tablerowLayout, isZoneAlive);
                }

                if (tempModule) {
                    viewTemperatureBasedZone(inflater, rootView, equipZones, zoneTitle, gridPosition, tablerowLayout, isZoneAlive);
                }
                if (!tempModule && nontempModule && !profileType.contains(profileHyperStatMonitoring)) {
                    viewNonTemperatureBasedZone(inflater, rootView, equipZones, zoneTitle, gridPosition,
                            tablerowLayout, isZoneAlive, roomMap.get("id").toString());

                }
                gridPosition++;
            }
        } else {
            //No devices paired
            boolean isZoneAlive = false;
            HashMap<Object, Object> connectNode = ConnectNodeUtil.Companion.getConnectNodeForZone(roomMap.get("id").toString(), CCUHsApi.getInstance());
            if (!connectNode.isEmpty()) {
                isZoneAlive = CCUUtils.isConnectModuleAlive(connectNode.get(Tags.ID).toString());
            }
            viewNonTemperatureBasedZone(inflater, rootView, new ArrayList<>(), zoneTitle, gridPosition, tablerowLayout,
                    isZoneAlive, roomMap.get("id") .toString());
            gridPosition++;
        }
        return gridPosition;
    }

    private void viewTemperatureBasedZone(LayoutInflater inflater,
                                          View rootView, ArrayList<HashMap> zoneMap,String zoneTitle,
                                          int gridPosition, LinearLayout[] tablerowLayout, boolean isZoneAlive)
    {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.viewTemperatureBasedZone");
        isItemSelectedEvent = false;

        CcuLog.i("ProfileTypes","Points:"+zoneMap.toString());
        Equip p = new Equip.Builder().setHashMap(zoneMap.get(0)).build();
        int temperatureMode = CCUHsApi.getInstance().readHisValByQuery("zone and hvacMode and roomRef" +
                " == \"" + p.getRoomRef() + "\"").intValue();
        CcuLog.i("ProfileTypes", "p:" + p.toString());
        double currentAverageTemp = 0;
        int noTempSensor = 0;
        ArrayList<Short> equipNodes = new ArrayList<>();
        double heatDeadband = 0;
        double coolDeadband = 0;
        double coolUpperlimit = 0;
        double coolLowerlimit = 0;
        double heatUpperlimit = 0;
        double heatLowerlimit = 0;
        double buildingLimitMax = BuildingTunerCache.getInstance().getBuildingLimitMax();
        double buildingLimitMin = BuildingTunerCache.getInstance().getBuildingLimitMin();
        cleanUpTempViewModel(tempProfileViewModels);
        tempProfileViewModels.clear();
        if(zoneViewModel != null){
            zoneViewModel.stopObservingZoneHealth();
            zoneViewModel = null;
        }
        for (int i = 0; i < zoneMap.size(); i++) {
            Equip avgTempEquip = new Equip.Builder().setHashMap(zoneMap.get(i)).build();
            double avgTemp = CCUHsApi.getInstance()
                    .readHisValByQuery("(point and domainName == \"" + DomainName.currentTemp + "\" and equipRef == \"" + avgTempEquip.getId() + "\") or " +
                            "(temp and sensor and (current or space) and equipRef == \"" + avgTempEquip.getId() + "\")");
            double heatDB = TunerUtil.getZoneHeatingDeadband(avgTempEquip.getRoomRef());
            double coolDB = TunerUtil.getZoneCoolingDeadband(avgTempEquip.getRoomRef());

            if (heatDB < heatDeadband || heatDeadband == 0) {
                heatDeadband = heatDB;
            }
            if (coolDB < coolDeadband || coolDeadband == 0) {
                coolDeadband = coolDB;
            }

            int statusVal;
            if (CCUUiUtil.isDomainEquip(avgTempEquip.getId(), "equip")) {
                statusVal = CCUUiUtil.readHisValByEquipRef(DomainName.equipStatus, avgTempEquip.getId()).intValue();
            } else {
                statusVal = CCUHsApi.getInstance().readHisValByQuery("point and status and not ota and his and not writable and equipRef ==\"" + avgTempEquip.getId() + "\"").intValue();
            }
            if (statusVal != ZoneState.TEMPDEAD.ordinal() && statusVal != ZoneState.RFDEAD.ordinal()) {
                currentAverageTemp = (currentAverageTemp + avgTemp);
            } else {
                noTempSensor++;
            }

            equipNodes.add(Short.valueOf(avgTempEquip.getGroup()));
            CcuLog.i("EachzoneData", "temp:" + avgTemp + " currentAvg:" + currentAverageTemp);

            if (heatDB == heatDeadband && coolDB == coolDeadband) // Setting User Limits based on deadband
            {
                coolUpperlimit = BuildingTunerCache.getInstance().getMaxCoolingUserLimit();
                coolLowerlimit = BuildingTunerCache.getInstance().getMinCoolingUserLimit();
                heatUpperlimit = BuildingTunerCache.getInstance().getMaxHeatingUserLimit();
                heatLowerlimit = BuildingTunerCache.getInstance().getMinHeatingUserLimit();
            }

        }
        if (zoneMap.size() > 1 && currentAverageTemp != 0) {
            currentAverageTemp = currentAverageTemp / (zoneMap.size() - noTempSensor);
            DecimalFormat decimalFormat = new DecimalFormat("#.#");
            currentAverageTemp = Double.parseDouble(decimalFormat.format(Math.round(currentAverageTemp * 10.0) / 10.0));
        }
        CcuLog.i("EachzoneData", " currentAvg:" + currentAverageTemp);

        final String[] equipId = {p.getId()};
        int i = gridPosition;
        View arcView;
        arcView = inflater.inflate(R.layout.zones_item, (ViewGroup) rootView, false);
        View zoneDetails = inflater.inflate(R.layout.zones_item_details, null);
        TextView    scheduleStatus      = zoneDetails.findViewById(R.id.schedule_status_tv);

        GridItem gridItemObj = new GridItem();
        gridItemObj.setGridID(i);
        gridItemObj.setGridItem("Temp");
        gridItemObj.setNodeAddress(Short.valueOf(p.getGroup()));
        gridItemObj.setZoneNodes(equipNodes);
        gridItemObj.setZoneEquips(zoneMap);
        arcView.setClickable(true);
        arcView.setTag(gridItemObj);
        arcView.setId(i);
        SeekArc seekArc = arcView.findViewById(R.id.seekArc);
        seekArc.setTag(gridItemObj);
        scheduleStatus.setTag(gridItemObj);
        zoneDetails.setTag(gridItemObj);
        TextView textEquipment = arcView.findViewById(R.id.textEquipment);
        textEquipment.setText(zoneTitle);
        textEquipment.setTextColor(getResources().getColor(R.color.text_color));
        TextView textViewModule = arcView.findViewById(R.id.module_status);
        View status_view = arcView.findViewById(R.id.status_view);
        HeartBeatUtil.zoneStatus(textViewModule, isZoneAlive);
        zoneStatus.put(zoneTitle, textViewModule);

        seekArc.scaletoNormal(260, 210);

        float heatUpperLimitVal;
        float heatLowerLimitVal ;
        float coolingLowerLimitVal ;
        float coolingUpperLimitVal ;
        float coolingDesired ;
        float heatingDesired ;
        float heatingDeadBand ;
        float coolingDeadBand;

        String roomRefZone = StringUtils.prependIfMissing(p.getRoomRef(), "@");
        heatUpperLimitVal = CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and point and limit and user and max and heating and roomRef == \"" + roomRefZone + "\"").floatValue();
        heatLowerLimitVal = CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and point and limit and user and min and heating and roomRef == \"" +roomRefZone + "\"").floatValue();
        coolingLowerLimitVal = CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and point and limit and user and min and cooling and roomRef == \"" + roomRefZone + "\"").floatValue();
        coolingUpperLimitVal = CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and point and limit and user and max and cooling and roomRef == \"" + roomRefZone + "\"").floatValue();
            if (CCUUiUtil.isDomainEquip(p.getId(), "equip")) {
                coolingDesired = CCUUiUtil.readPriorityValByRoomRef(DomainName.desiredTempCooling, roomRefZone).floatValue();
                heatingDesired = CCUUiUtil.readPriorityValByRoomRef(DomainName.desiredTempHeating, roomRefZone).floatValue();
            } else {
                coolingDesired = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and cooling and roomRef == \"" + roomRefZone + "\"").floatValue();
                heatingDesired = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and heating and roomRef == \"" + roomRefZone + "\"").floatValue();
            }

        heatingDeadBand = CCUHsApi.getInstance().readPointPriorityValByQuery("heating and deadband and zone and not multiplier and roomRef == \"" + roomRefZone + "\"").floatValue();
        coolingDeadBand = CCUHsApi.getInstance().readPointPriorityValByQuery("cooling and deadband and zone and not multiplier and roomRef == \"" + roomRefZone + "\"").floatValue();

        String zoneId = Schedule.getZoneIdByEquipId(equipId[0]);

        int modeType = CCUHsApi.getInstance().readHisValByQuery("zone and hvacMode and roomRef == \"" + zoneId + "\"").intValue();

        if(heatingDesired != 0 && coolingDesired !=0)
            seekArc.setData(false, (float) buildingLimitMin, (float)buildingLimitMax, heatLowerLimitVal,
                    heatUpperLimitVal, coolingLowerLimitVal, coolingUpperLimitVal, heatingDesired, coolingDesired,
                    (float)currentAverageTemp, heatingDeadBand, coolingDeadBand,modeType);

        seekArc.setDetailedView(false);
        LinearLayout.LayoutParams rowLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        arcView.setPadding(48, 64, 0, 0);
        try {
            tablerowLayout[rowcount].addView(arcView, rowLayoutParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (((i + 1) % columnCount == 0) && (i != 0)) {
            try {
                if (tablerowLayout[rowcount].getParent() != null) {
                    ((ViewGroup) tablerowLayout[rowcount].getParent()).removeView(tablerowLayout[rowcount]);
                }
                tableLayout.addView(tablerowLayout[rowcount++]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (rowcount < numRows)
                tablerowLayout[rowcount] = new LinearLayout(tableLayout.getContext());
        }
        if (rowcount < numRows) {
            try {
                if (tablerowLayout[rowcount].getParent() != null) {
                    ((ViewGroup) tablerowLayout[rowcount].getParent()).removeView(tablerowLayout[rowcount]);
                }
                tableLayout.addView(tablerowLayout[rowcount]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        seekArcArrayList.add(seekArc);
        statusTextArrayList.add(scheduleStatus);
        zoneStatusArrayList.add(zoneDetails);
        seekArc.setOnTemperatureChangeListener(new SeekArc.OnTemperatureChangeListener() {
            @Override
            public void onTemperatureChange(SeekArc seekArc, float coolingDesiredTemp, float heatingDesiredTemp, boolean syncToHaystack) {
                if (syncToHaystack) {
                    double pointheatDT;
                    double pointcoolDT;
                    if (CCUUiUtil.isDomainEquip(p.getId(), "equip")) {
                        pointheatDT = CCUUiUtil.readPriorityValByEquipRef(DomainName.desiredTempHeating, p.getId());
                        pointcoolDT = CCUUiUtil.readPriorityValByEquipRef(DomainName.desiredTempCooling, p.getId());
                    } else {
                        pointheatDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and heating and equipRef == \"" + p.getId() + "\"");
                        pointcoolDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and cooling and equipRef == \"" + p.getId() + "\"");
                    }

                    if (zoneMap.size() > 0) {
                        for (int i = 0; i < zoneMap.size(); i++) {
                            Equip zoneEquip = new Equip.Builder().setHashMap(zoneMap.get(i)).build();
                            double curCoolDt = 0;
                            double curHeatDt = 0;
                            if (pointcoolDT != coolingDesiredTemp)
                                curCoolDt = coolingDesiredTemp;
                            if (pointheatDT != heatingDesiredTemp)
                                curHeatDt = heatingDesiredTemp;
                            double curAvgDt = (coolingDesiredTemp + heatingDesiredTemp) / 2.0;
                            HashMap coolDT;
                            HashMap heatDT;
                            HashMap avgDT;
                           if(CCUUiUtil.isDomainEquip(zoneEquip.getId(), "equip")){
                               coolDT = CCUHsApi.getInstance().readEntity("point and domainName == \"" + DomainName.desiredTempCooling + "\" and equipRef == \"" + zoneEquip.getId() + "\"");
                               heatDT = CCUHsApi.getInstance().readEntity("point and domainName == \"" + DomainName.desiredTempHeating + "\" and equipRef == \"" + zoneEquip.getId() + "\"");
                               avgDT = CCUHsApi.getInstance().readEntity("point and domainName == \"" + DomainName.desiredTemp + "\" and equipRef == \"" + zoneEquip.getId() + "\"");
                           }else{
                               coolDT = CCUHsApi.getInstance().readEntity("point and temp and desired and cooling and sp and equipRef == \"" + zoneEquip.getId() + "\"");
                               heatDT = CCUHsApi.getInstance().readEntity("point and temp and desired and heating and sp and equipRef == \"" + zoneEquip.getId() + "\"");
                               avgDT = CCUHsApi.getInstance().readEntity("point and temp and desired and (avg or average) and sp and equipRef == \"" + zoneEquip.getId() + "\"");
                           }
                            setPointVal(coolDT.get("id").toString(), curCoolDt, heatDT.get("id").toString(), curHeatDt, avgDT.get("id").toString(), curAvgDt);
                        }
                    }
                }

            }
        });

        seekArc.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View v) {
                HisWriteObservable.INSTANCE.clear();
                PointWriteObservable.INSTANCE.clear();
                LinearLayout linearLayoutZonePoints  = zoneDetails.findViewById(R.id.lt_profilepoints);
                Spinner scheduleSpinner     = zoneDetails.findViewById(R.id.schedule_spinner);
                CCUUiUtil.setSpinnerDropDownColor(scheduleSpinner,getContext());
                ImageButton scheduleImageButton = zoneDetails.findViewById(R.id.schedule_edit_button);
                ImageButton vacationImageButton = zoneDetails.findViewById(R.id.vacation_edit_button);
                ImageButton specialScheduleImageButton = zoneDetails.findViewById(R.id.special_status_edit_button);
                TextView vacationStatusTV = zoneDetails.findViewById(R.id.vacation_status);
                TextView specialScheduleStatusText = zoneDetails.findViewById(R.id.special_status_status);
                ImageButton namedScheduleView = zoneDetails.findViewById(R.id.schedule_view_button);
                CcuLog.i("UI_PROFILING","ZoneFragmentNew.viewTemperatureBasedZone.SeekArc Onclick");
                currentPosition = scheduleSpinner.getSelectedItemPosition();
                GridItem gridItemNew = (GridItem) v.getTag();
                boolean isExpanded = false;

                int clickposition = gridItemNew.getGridID();
                if (clickedView != -1) {
                    if (clickposition != clickedView) {
                        // clicked on another zone when one zone is already opened
                        cleanUpTempViewModel(tempProfileViewModels);
                        tempProfileViewModels.clear();
                        if(zoneViewModel != null){
                            zoneViewModel.stopObservingZoneHealth();
                            zoneViewModel = null;
                        }
                        int tableRowCount = tableLayout.getChildCount();
                        if (tableLayout.getChildCount() > 1) {
                            boolean viewFound = false;
                            for (int row = 0; row < tableRowCount; row++) {
                                View rowView = tableLayout.getChildAt(row);
                                LinearLayout tableRow = (LinearLayout) rowView;
                                int cellCount = tableRow.getChildCount();
                                for (int j = 0; j < cellCount; j++) {
                                    RelativeLayout gridItem = (RelativeLayout) tableRow.getChildAt(j);
                                    GridItem viewTag = (GridItem) gridItem.getTag();
                                    if (viewTag.getGridID() == clickedView) {
                                        View statusView = gridItem.findViewById(R.id.status_view);
                                        statusView.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                        gridItem.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                        TextView textViewzone = (TextView) gridItem.findViewById(R.id.textEquipment);
                                        textViewzone.setTextAppearance(getActivity(), R.style.label_black);
                                        textViewzone.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                        tableLayout.removeViewAt(row + 1);
                                        if (viewTag.getGridItem().equals("Temp") || viewTag.getGridItem().equals("monitoring")) {
                                            SeekArc seekArcExpanded = (SeekArc) gridItem.findViewById(R.id.seekArc);
                                            seekArcExpanded.setDetailedView(false);
                                            seekArcExpanded.setBackgroundColor(getResources().getColor(R.color.white));
                                            seekArcExpanded.scaletoNormal(260, 210);
                                        } else {
                                            NonTempControl nonTempControl = gridItem.findViewById(R.id.rl_nontemp);
                                            ScaleControlToNormal(270,210,nonTempControl);
                                            nonTempControl.setExpand(false);
                                            //ScaleImageToNormal(250,210,imageViewExpanded);
                                            nonTempControl.setBackgroundColor(getResources().getColor(R.color.white));
                                        }

                                        imageOn = false;
                                        viewFound = true;
                                        break;
                                    }
                                }
                                if (viewFound) {
                                    break;
                                }
                            }

                            zoneOpen = true;
                            zoneNonTempOpen = false;
                            seekArcOpen = seekArc;
                            zonePointsOpen = zoneDetails;
                            equipOpen = p;
                            openZoneMap = zoneMap;
                            clickedView = gridItemNew.getGridID();
                            v.setBackgroundColor(getActivity().getResources().getColor(R.color.zoneselection_gray));
                            status_view.setBackgroundColor(getActivity().getResources().getColor(R.color.zoneselection_gray));
                            int index = clickedView / columnCount + 1;
                            seekArc.setDetailedView(true);
                            seekArc.scaletoNormalBig(260, 210);
                            imageOn = true;
                            selectedView = seekArc.getId();
                            try {
                                textEquipment.setTextAppearance(getActivity(),R.attr.label_orange);
                                textEquipment.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                                zoneDetails.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                                tableLayout.addView(zoneDetails, index);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            isExpanded = true;
                        }
                    } else if (clickposition == clickedView) {
                        cleanUpTempViewModel(tempProfileViewModels);
                        tempProfileViewModels.clear();
                        if(zoneViewModel != null){
                            zoneViewModel.stopObservingZoneHealth();
                            zoneViewModel = null;
                        }
                        v.setBackgroundColor(getResources().getColor(R.color.white));
                        textEquipment.setTextAppearance(getActivity(), R.style.label_black);
                        textEquipment.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                        status_view.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                        tableLayout.removeView(zoneDetails);
                        imageOn = false;
                        seekArc.setDetailedView(false);
                        seekArc.scaletoNormal(260, 210);
                        showWeather();
                        clickedView = -1;
                    }
                } else {
                    // zone opened here
                    zoneOpen = true;
                    zoneNonTempOpen = false;
                    seekArcOpen = seekArc;
                    zonePointsOpen = zoneDetails;
                    equipOpen = p;
                    openZoneMap = zoneMap;
                    clickedView = gridItemNew.getGridID();
                    seekArc.setClickable(true);
                    v.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                    status_view.setBackgroundColor(getActivity().getResources().getColor(R.color.zoneselection_gray));
                    int index = clickedView / columnCount + 1;
                    seekArc.setDetailedView(true);
                    seekArc.scaletoNormalBig(260, 210);
                    hideWeather();
                    imageOn = true;
                    selectedView = seekArc.getId();
                    try {
                        textEquipment.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        zoneDetails.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        tableLayout.addView(zoneDetails, index);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isExpanded = true;
                }

                if (isExpanded) {
                    //zone opened here
                    ArrayList<String> scheduleArray = new ArrayList<>();
                    scheduleArray.add("Zone Schedule");
                    scheduleArray.add("Shared Schedule");
                    boolean isSelectedScheduleAvailable = true;

                    ArrayList<Boolean> hasImage = new ArrayList<>();
                    hasImage.add(false);
                    hasImage.add(false);


                    List<HashMap<Object, Object>> namedScheds = CCUHsApi.getInstance().getAllNamedSchedules();
                    if(!namedScheds.isEmpty()){
                        for (HashMap<Object, Object> nameSched :
                                namedScheds) {
                            String namedScheduledis = Objects.requireNonNull(nameSched.get("dis")).toString();
                            if(nameSched.get("default") != null){
                                scheduleArray.add(getTruncatedString("Default - " + CCUHsApi.getInstance().getSiteName(), 25, 0, 25));
                                hasImage.add(true);
                            } else if(namedScheduledis.length() > 25){
                                scheduleArray.add(getTruncatedString(Objects.requireNonNull(nameSched.get("dis").toString()), 25, 0, 25));
                                hasImage.add(true);
                            }else{
                                scheduleArray.add(Objects.requireNonNull(nameSched.get("dis")).toString());
                                hasImage.add(true);
                            }
                        }
                    }else{
                        scheduleArray.add("No Shared Schedule available");
                        hasImage.add(false);
                        scheduleImageButton.setVisibility(View.GONE);
                        namedScheduleView.setVisibility(View.GONE);

                    }


                    CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(getActivity(), R.layout.custom_dropdown_item_with_image, scheduleArray, hasImage);

                    scheduleSpinner.setAdapter(adapter);
                    adapter.setSelectedPosition(scheduleSpinner.getSelectedItemPosition());
                    String zoneId = Schedule.getZoneIdByEquipId(equipId[0]);

                    try {
                        if( isCelsiusTunerAvailableStatus()) {
                            Observable.fromCallable(() -> ScheduleManager.getInstance().getMultiModuleZoneStatusMessage(zoneId))
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(status -> scheduleStatus.setText(StatusCelsiusVal(status, temperatureMode)));
                        } else {
                            Observable.fromCallable(() -> ScheduleManager.getInstance().getMultiModuleZoneStatusMessage(zoneId))
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(status -> scheduleStatus.setText(setPointStatusMessage(status, TemperatureMode.values()[temperatureMode])));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Observable.fromCallable(() -> ScheduleManager.getInstance().getVacationStateString(zoneId))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(vacationStatusTV::setText);

                    Observable.fromCallable(() -> ScheduleManager.getInstance().getScheduleStateString(zoneId))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(status -> specialScheduleStatusText.setText(status));

                    String scheduleTypeId = getScheduleTypeId(equipId[0]);
                    final Integer mScheduleType = (int) CCUHsApi.getInstance().readPointPriorityVal(scheduleTypeId);
                    CcuLog.d("ScheduleType", "mScheduleType==" + mScheduleType + "," + (int) CCUHsApi.getInstance().readPointPriorityVal(scheduleTypeId) + "," + p.getDisplayName());
                    mSchedule = Schedule.getScheduleByEquipId(equipId[0]);
                    scheduleSpinner.setTag(mScheduleType -1);
                    mScheduleTypeMap.put(equipId[0], mScheduleType);
                    scheduleImageButton.setTag(mSchedule.getId());
                    vacationImageButton.setTag(mSchedule.getId());
                    specialScheduleImageButton.setTag(mSchedule.getId());

                    if(isRoomsScheduleRefAvailable(equipId[0])){
                        isSelectedScheduleAvailable = false;
                    } else if(mSchedule.isNamedSchedule() && !namedScheds.isEmpty())
                        namedScheduleView.setVisibility(View.VISIBLE);

                    namedScheduleView.setOnClickListener(view -> {
                        String scheduleDis = (mSchedule.getDis());
                        String scheduleName = (mSchedule.getMarkers().contains("default")) ?
                                "Default - "+CCUHsApi.getInstance().getSiteName() :scheduleDis;

                        NamedSchedule namedSchedule =
                                NamedSchedule.getInstance(mSchedule.getId(),
                                        zoneId,  scheduleName,false);
                        FragmentManager childFragmentManager = getChildFragmentManager();
                        namedSchedule.show(childFragmentManager, "dialog");
                    });


                    if ((mSchedule.isZoneSchedule() && !mSchedule.isBuildingSchedule())
                            || (mSchedule.isNamedSchedule() && mSchedule.getMarkers().contains("default")
                            && !namedScheds.isEmpty() && OfflineModeUtilKt.isOfflineMode())){
                        scheduleImageButton.setVisibility(View.VISIBLE);
                    } else {
                        scheduleImageButton.setVisibility(View.GONE);
                    }

                    scheduleImageButton.setOnClickListener(scheduleImageView ->
                            imageButtonClickListener(scheduleImageView, zoneId, equipId, ZoneFragmentNew.this.getChildFragmentManager(),false));

                    vacationImageButton.setOnClickListener(vacationImageView ->
                    {
                        ScheduleGroupFragment schedulerFragment = new ScheduleGroupFragment().showVacationsLayout(zoneId, mSchedule);
                        FragmentManager childFragmentManager = getFragmentManager();
                        childFragmentManager.beginTransaction();
                        schedulerFragment.show(childFragmentManager, "dialog");
                    });
                    specialScheduleImageButton.setOnClickListener(splScheduleImageView ->
                            imageButtonClickListener(splScheduleImageView, zoneId, equipId, ZoneFragmentNew.this.getChildFragmentManager(),true));



                    scheduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            prevPosition = currentPosition;
                            currentPosition = position;
                            adapter.setSelectedPosition(position);

                            CcuLog.i("UI_PROFILING","ZoneFragmentNew.scheduleSpinner");
                            if(isItemSelectedEvent) {
                                isItemSelectedEvent = false;
                                isRemoteChangeApplied = false;
                                return;
                            }

                            if (position == 0 && (mScheduleType != -1)/*&& (mScheduleType != position)*/) {
                                namedScheduleView.setVisibility(View.GONE);
                                scheduleImageButton.setVisibility(View.VISIBLE);

                                if (mSchedule.isZoneSchedule()) {
                                    mSchedule.setDisabled(false);
                                    CCUHsApi.getInstance().updateZoneSchedule(mSchedule, zoneId);
                                    scheduleImageButton.setTag(mSchedule.getId());
                                    vacationImageButton.setTag(mSchedule.getId());
                                    specialScheduleImageButton.setTag(mSchedule.getId());
                                    transitionToZoneSchedules(zoneMap, mSchedule.getId(), zoneDetails, false);
                                } else {
                                    Zone zone = Schedule.getZoneforEquipId(equipId[0]);
                                    HashMap<Object, Object> scheduleHashmap = CCUHsApi.getInstance().readEntity("schedule and " +
                                            "not special and not vacation and roomRef " + "== " +zone.getId());
                                    Schedule scheduleById = CCUHsApi.getInstance().getScheduleById(scheduleHashmap.get("id").toString());
                                    scheduleById.setDisabled(false);

                                    if(!zone.hasSchedule()) {
                                        /* We are in a situation where there is a zone without a scheduleRef.
                                         * There might have been an error scenario that prevented attaching the scheduleRef, but
                                         * still created a schedule. Handle it before creating a new schedule again.
                                         */
                                        if (scheduleHashmap.isEmpty()) {
                                            DefaultSchedules.setDefaultCoolingHeatingTemp();
                                            DefaultSchedules.generateDefaultZoneSchedule(zone.getId());
                                        }
                                    }

                                    ContainmentDialogClickListener containmentDialogClickListener = isForceTrimmed -> {
                                        if(isForceTrimmed) {
                                            transitionToZoneSchedules(zoneMap, scheduleById.getId(), zoneDetails, false);
                                        } else {
                                            scheduleSpinner.setSelection(prevPosition);
                                        }
                                    };

                                    boolean isContainment = checkContainment(scheduleById, containmentDialogClickListener);
                                    if(mScheduleTypeMap.get(equipId[0]) != ScheduleType.ZONE.ordinal() && isContainment) {
                                        CCUHsApi.getInstance().updateZoneSchedule(scheduleById, zone.getId());
                                        transitionToZoneSchedules(zoneMap, scheduleById.getId(), zoneDetails, false);
                                    }
                                }
                            } else if (position == 1 && (mScheduleType != -1)) {
                                //No operation as it is a Named Schedule Title
                            } else if (position >= 2 && (mScheduleType != -1)) {
                                if(!namedScheds.isEmpty()) {
                                    mScheduleTypeMap.put(equipId[0], ScheduleType.NAMED.ordinal());
                                    scheduleImageButton.setVisibility(View.GONE);
                                    namedScheduleView.setVisibility(View.VISIBLE);
                                    HashMap<Object, Object> room = CCUHsApi.getInstance().readMapById(zoneId);
                                    HashMap<Object,Object> nameScheduleMap = namedScheds.get(position - 2);
                                    String namedScheduleId = nameScheduleMap.get("id").toString();
                                    String scheduleDis = (nameScheduleMap.get("dis").toString());

                                    if (nameScheduleMap.get("default") != null &&
                                            OfflineModeUtilKt.isOfflineMode()) {
                                        scheduleImageButton.setVisibility(View.VISIBLE);
                                    }

                                    String scheduleName = (nameScheduleMap.get("default") != null) ?
                                            "Default - " + CCUHsApi.getInstance().getSiteName() : scheduleDis;

                                    if (!namedScheduleId.equals(room.get("scheduleRef").toString())) {
                                        NamedSchedule namedSchedule =
                                                NamedSchedule.getInstance(namedScheds.get(position - 2).get("id").toString(),
                                                        zoneId, scheduleName, true);
                                        FragmentManager childFragmentManager = getChildFragmentManager();
                                        namedSchedule.show(childFragmentManager, "dialog");
                                        scheduleSpinner.setSelection(position);
                                        if (nameScheduleMap.get("default") != null
                                                && OfflineModeUtilKt.isOfflineMode()) {
                                            scheduleImageButton.setVisibility(View.VISIBLE);
                                        } else
                                            scheduleImageButton.setVisibility(View.GONE);

                                        namedSchedule.setOnExitListener(() -> {
                                            mSchedule = Schedule.getScheduleByEquipId(equipId[0]);
                                            ScheduleManager.getInstance().updateSchedules(equipOpen);
                                            Toast.makeText(getContext(), "Refresh View", Toast.LENGTH_LONG).show();
                                            scheduleImageButton.setTag(mSchedule.getId());
                                            vacationImageButton.setTag(mSchedule.getId());
                                            specialScheduleImageButton.setTag(mSchedule.getId());
                                        });

                                        namedSchedule.setOnCancelButtonClickListener(() -> {
                                            if(prevPosition == 0) {
                                                scheduleImageButton.setVisibility(View.VISIBLE);
                                                namedScheduleView.setVisibility(View.GONE);
                                                isItemSelectedEvent = true;
                                            }
                                            scheduleSpinner.setSelection(prevPosition);
                                        });
                                    }
                                    scheduleSpinner.setSelection(position);
                                }
                            }
                            mSchedule = Schedule.getScheduleByEquipId(equipId[0]);
                            scheduleImageButton.setTag(mSchedule.getId());
                            vacationImageButton.setTag(mSchedule.getId());
                            specialScheduleImageButton.setTag(mSchedule.getId());
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });

                    if(mScheduleType >= 2){
                        if(!isSelectedScheduleAvailable) {
                            scheduleSpinner.setSelection(1);
                        }else {
                            int spinnerposition = 2;
                            for (HashMap<Object, Object> a : namedScheds) {
                                if ((Objects.requireNonNull(Objects.requireNonNull(a.get("id"))).toString().substring(1)).equals
                                        (Schedule.getScheduleByEquipId(equipId[0]).getId())) {
                                    spinnerposition = namedScheds.indexOf(a) + 2;
                                }
                            }
                            isItemSelectedEvent = true;
                            scheduleSpinner.setSelection(spinnerposition, false);
                            adapter.setSelectedPosition(spinnerposition);
                        }
                    }else{
                        isItemSelectedEvent = true;
                        scheduleSpinner.setSelection(mScheduleType -1, false);
                    }

                    v.setContentDescription(zoneTitle);
                    linearLayoutZonePoints.removeAllViews();
                    if (scheduleSpinner.getSelectedItemPosition() == 0) {
                        scheduleImageButton.setVisibility(View.VISIBLE);
                        namedScheduleView.setVisibility(GONE);
                    } else {
                      if(!namedScheds.isEmpty() && isSelectedScheduleAvailable)
                        namedScheduleView.setVisibility(View.VISIBLE);
                    }

                    String vacationStatus = ScheduleManager.getInstance().getVacationStateString(zoneId);
                    vacationStatusTV.setText(vacationStatus);
                    String specialScheduleStatus = getScheduleStateString(zoneId);
                    specialScheduleStatusText.setText(specialScheduleStatus);

                    OnScheduleChangeListener scheduleChangeListener = position -> {
                        {
                            prevPosition = currentPosition;
                            currentPosition = position;
                            if (position == 0 && (mScheduleType != -1)) {
                                refreshSchedules(tempProfileViewModels);
                                if (mSchedule.isZoneSchedule()) {
                                    mSchedule.setDisabled(false);
                                    CCUHsApi.getInstance().updateZoneSchedule(mSchedule, zoneId);
                                    transitionToZoneSchedules(zoneMap, mSchedule.getId(), zoneDetails, true);
                                } else {
                                    Zone zone = Schedule.getZoneforEquipId(equipId[0]);
                                    HashMap<Object, Object> scheduleHashmap = CCUHsApi.getInstance().readEntity("schedule and " +
                                            "not special and not vacation and roomRef " + "== " +zone.getId());
                                    Schedule scheduleById = CCUHsApi.getInstance().getScheduleById(scheduleHashmap.get("id").toString());
                                    scheduleById.setDisabled(false);

                                    if(!zone.hasSchedule()) {
                                        /* We are in a situation where there is a zone without a scheduleRef.
                                         * There might have been an error scenario that prevented attaching the scheduleRef, but
                                         * still created a schedule. Handle it before creating a new schedule again.
                                         */
                                        if (scheduleHashmap.isEmpty()) {
                                            DefaultSchedules.setDefaultCoolingHeatingTemp();
                                            DefaultSchedules.generateDefaultZoneSchedule(zone.getId());
                                        }
                                    }

                                    ContainmentDialogClickListener containmentDialogClickListener = isForceTrimmed -> {
                                        if(isForceTrimmed) {
                                            transitionToZoneSchedules(zoneMap, scheduleById.getId(), zoneDetails, true);
                                        } else {
                                            scheduleSpinner.setSelection(prevPosition);
                                        }
                                    };

                                    boolean isContainment = checkContainment(scheduleById, containmentDialogClickListener);
                                    if(mScheduleTypeMap.get(equipId[0]) != ScheduleType.ZONE.ordinal() && isContainment) {
                                        CCUHsApi.getInstance().updateZoneSchedule(scheduleById, zone.getId());
                                        transitionToZoneSchedules(zoneMap, scheduleById.getId(), zoneDetails, true);
                                    }
                                }
                            } else if (position >= 2 && (mScheduleType != -1)) {
                                if(!namedScheds.isEmpty()) {
                                    mScheduleTypeMap.put(equipId[0], ScheduleType.NAMED.ordinal());
                                    refreshSchedules(tempProfileViewModels);
                                    HashMap<Object, Object> room = CCUHsApi.getInstance().readMapById(zoneId);
                                    HashMap<Object,Object> nameScheduleMap = namedScheds.get(position - 2);
                                    String namedScheduleId = nameScheduleMap.get("id").toString();
                                    String scheduleDis = (nameScheduleMap.get("dis").toString());

                                    if (nameScheduleMap.get("default") != null &&
                                            OfflineModeUtilKt.isOfflineMode()) {
                                        scheduleImageButton.setVisibility(View.VISIBLE);
                                    }

                                    String scheduleName = (nameScheduleMap.get("default") != null) ?
                                            "Default - " + CCUHsApi.getInstance().getSiteName() : scheduleDis;

                                    if (!namedScheduleId.equals(room.get("scheduleRef").toString())) {
                                        NamedSchedule namedSchedule =
                                                NamedSchedule.getInstance(namedScheds.get(position - 2).get("id").toString(),
                                                        zoneId, scheduleName, true);
                                        FragmentManager childFragmentManager = getChildFragmentManager();
                                        namedSchedule.show(childFragmentManager, "dialog");
                                        namedSchedule.setOnExitListener(() -> {
                                            mSchedule = Schedule.getScheduleByEquipId(equipId[0]);
                                            ScheduleManager.getInstance().updateSchedules(equipOpen);
                                            refreshSchedules(tempProfileViewModels);
                                        });

                                        namedSchedule.setOnCancelButtonClickListener(() -> {
                                            refreshSchedules(tempProfileViewModels);
                                        });
                                    }
                                }
                            }
                            mSchedule = Schedule.getScheduleByEquipId(equipId[0]);
                            refreshSchedules(tempProfileViewModels);
                        }
                    };

                    OnScheduleEditClickListener scheduleEditClickListener = position -> {
                        editSchedule(mSchedule, zoneId, equipId,
                                ZoneFragmentNew.this.getChildFragmentManager(),false, tempProfileViewModels);
                    };

                    OnScheduleViewClickListener scheduleViewClickListener = position -> {
                        String scheduleDis = (mSchedule.getDis());
                        String scheduleName = (mSchedule.getMarkers().contains("default")) ?
                                "Default - "+CCUHsApi.getInstance().getSiteName() :scheduleDis;

                        NamedSchedule namedSchedule =
                                NamedSchedule.getInstance(mSchedule.getId(),
                                        zoneId,  scheduleName,false);
                        FragmentManager childFragmentManager = getChildFragmentManager();
                        namedSchedule.show(childFragmentManager, "dialog");
                    };

                    OnSpecialScheduleEditClickListener specialScheduleEditClickListener = position -> {
                        editSchedule(mSchedule, zoneId, equipId,
                                ZoneFragmentNew.this.getChildFragmentManager(),true, tempProfileViewModels);
                    };

                    OnVacationScheduleEditClickListener vacationScheduleEditClickListener = position -> {
                        ScheduleGroupFragment schedulerFragment = new ScheduleGroupFragment().showVacationsLayout(zoneId, mSchedule);
                        FragmentManager childFragmentManager = getFragmentManager();
                        childFragmentManager.beginTransaction();
                        schedulerFragment.setTempProfileViewModels(tempProfileViewModels);
                        schedulerFragment.show(childFragmentManager, "dialog");
                    };

                    // below code is to observe zone health
                    if (!zoneMap.isEmpty() && zoneMap.get(0) != null) {
                        zoneViewModel = ZoneViewModel.Companion.create(
                                zoneMap.get(0).get("roomRef").toString()
                        );
                        zoneViewModel.setHeartbeatView(textViewModule);
                        zoneViewModel.setSeekArc(seekArcOpen);
                        zoneViewModel.setEquip(equipOpen);
                        zoneViewModel.observeZoneHealth();
                    }

                    {
                        boolean showSchedule;
                        for (int k = 0; k < zoneMap.size(); k++) {
                            showSchedule = k == 0;
                            Equip p = new Equip.Builder().setHashMap(zoneMap.get(k)).build();
                            String updatedEquipId = p.getId();
                            equipOpen = new Equip.Builder().setHashMap(zoneMap.get(0)).build();
                            equipId[0] = equipOpen.getId();
                            mSchedule = Schedule.getScheduleByEquipId(equipId[0]);
                            if (p.getProfile().startsWith("DAB")) {
                                disableVisibiltyForZoneScheduleUI(zoneDetails);
                                DabHelper.Companion.create(p,
                                                ProfileType.DAB)
                                        .loadDetailedView(
                                                inflater,
                                                tempProfileViewModels,
                                                linearLayoutZonePoints,
                                                showSchedule,
                                                (position, point) -> {
                                                    doCallBack(point, position, scheduleChangeListener,
                                                            scheduleViewClickListener, scheduleEditClickListener,
                                                            specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                    return Unit.INSTANCE;
                                                }
                                        );
                            }
                            if (p.getProfile().startsWith("VAV")) {
                                disableVisibiltyForZoneScheduleUI(zoneDetails);
                                if (p.getProfile().equalsIgnoreCase(VAV_REHEAT.name())) {
                                    VavHelper.Companion.create(p,
                                                    ProfileType.VAV_REHEAT)
                                            .loadDetailedView(
                                                    inflater,
                                                    tempProfileViewModels,
                                                    linearLayoutZonePoints,
                                                    showSchedule,
                                                    (position, point) -> {
                                                        doCallBack(point, position, scheduleChangeListener,
                                                                scheduleViewClickListener, scheduleEditClickListener,
                                                                specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                        return Unit.INSTANCE;
                                                    }
                                            );
                                }

                                if (p.getProfile().equalsIgnoreCase(VAV_SERIES_FAN.name())) {
                                    VavHelper.Companion.create(p,
                                                    ProfileType.VAV_SERIES_FAN)
                                            .loadDetailedView(
                                                    inflater,
                                                    tempProfileViewModels,
                                                    linearLayoutZonePoints,
                                                    showSchedule,
                                                    (position, point) -> {
                                                        doCallBack(point, position, scheduleChangeListener,
                                                                scheduleViewClickListener, scheduleEditClickListener,
                                                                specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                        return Unit.INSTANCE;
                                                    }
                                            );
                                }

                                if (p.getProfile().equalsIgnoreCase(VAV_PARALLEL_FAN.name())) {
                                    VavHelper.Companion.create(p,
                                                    ProfileType.VAV_PARALLEL_FAN)
                                            .loadDetailedView(
                                                    inflater,
                                                    tempProfileViewModels,
                                                    linearLayoutZonePoints,
                                                    showSchedule,
                                                    (position, point) -> {
                                                        doCallBack(point, position, scheduleChangeListener,
                                                                scheduleViewClickListener, scheduleEditClickListener,
                                                                specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                        return Unit.INSTANCE;
                                                    }
                                            );
                                }

                                if (p.getProfile().equalsIgnoreCase(VAV_ACB.name())) {
                                    VavHelper.Companion.create(p,
                                                    ProfileType.VAV_ACB)
                                            .loadDetailedView(
                                                    inflater,
                                                    tempProfileViewModels,
                                                    linearLayoutZonePoints,
                                                    showSchedule,
                                                    (position, point) -> {
                                                        doCallBack(point, position, scheduleChangeListener,
                                                                scheduleViewClickListener, scheduleEditClickListener,
                                                                specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                        return Unit.INSTANCE;
                                                    }
                                            );
                                }
                            }
                            if (p.getProfile().startsWith("TEMP_INFLUENCE")) {
                                disableVisibiltyForZoneScheduleUI(zoneDetails);
                                TIHelper.Companion.create(p,
                                                ProfileType.TEMP_INFLUENCE)
                                        .loadDetailedView(
                                                inflater,
                                                tempProfileViewModels,
                                                linearLayoutZonePoints,
                                                showSchedule,
                                                (position, point) -> {
                                                    doCallBack(point, position, scheduleChangeListener,
                                                            scheduleViewClickListener, scheduleEditClickListener,
                                                            specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                    return Unit.INSTANCE;
                                                }
                                        );
                            }
                            if (p.getProfile().startsWith("SMARTSTAT_TWO_PIPE_FCU")) {
                                HashMap p2FCUPoints = ZoneViewData.get2PFCUEquipPoints(p.getId());
                                loadSS2PFCUPointsUI(p2FCUPoints, inflater, linearLayoutZonePoints, updatedEquipId, false, p.getGroup());

                            }
                            if (p.getProfile().startsWith("SMARTSTAT_FOUR_PIPE_FCU")) {
                                HashMap p4FCUPoints = ZoneViewData.get4PFCUEquipPoints(p.getId());
                                loadSS4PFCUPointsUI(p4FCUPoints, inflater, linearLayoutZonePoints, updatedEquipId, false, p.getGroup());
                            }
                            if (p.getProfile().startsWith("SMARTSTAT_CONVENTIONAL_PACK_UNIT")) {
                                HashMap cpuEquipPoints = ZoneViewData.getCPUEquipPoints(p.getId());
                                loadSSCPUPointsUI(cpuEquipPoints, inflater, linearLayoutZonePoints, updatedEquipId, false, p.getGroup(), false);
                                isCPUloaded = true;
                            }
                            if (p.getProfile().startsWith("SMARTSTAT_HEAT_PUMP_UNIT")) {
                                HashMap hpuEquipPoints = ZoneViewData.getHPUEquipPoints(p.getId());
                                loadSSHPUPointsUI(hpuEquipPoints, inflater, linearLayoutZonePoints, updatedEquipId, false, p.getGroup(), false);
                                isHPUloaded = true;
                            }
                            if (p.getProfile().startsWith("DUAL_DUCT")) {
                                HashMap dualDuctPoints = DualDuctUtil.getEquipPointsForView(p.getId());
                                loadDualDuctPointsUI(dualDuctPoints, inflater, linearLayoutZonePoints, p.getGroup());
                            }
                            if (p.getProfile().equalsIgnoreCase(ProfileType.OTN.toString())) {
                                disableVisibiltyForZoneScheduleUI(zoneDetails);
                                OTNHelper.Companion.create(p,
                                                ProfileType.OTN)
                                        .loadDetailedView(
                                                inflater,
                                                tempProfileViewModels,
                                                linearLayoutZonePoints,
                                                showSchedule,
                                                (position, point) -> {
                                                    doCallBack(point, position, scheduleChangeListener,
                                                            scheduleViewClickListener, scheduleEditClickListener,
                                                            specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                    return Unit.INSTANCE;
                                                }
                                        );
                            }

                            if (p.getProfile().startsWith(ProfileType.HYPERSTAT_VRV.name())) {
                                HyperStatVrvZoneViewKt.loadView(inflater, linearLayoutZonePoints,
                                                                updatedEquipId, CCUHsApi.getInstance(), getActivity(),
                                                                p.getGroup());
                            }

                            if (p.getProfile().startsWith("SSE")) {
                                disableVisibiltyForZoneScheduleUI(zoneDetails);
                                SseHelper.Companion.create(p,
                                                ProfileType.SSE
                                        )
                                        .loadDetailedView(
                                                inflater,
                                                tempProfileViewModels,
                                                linearLayoutZonePoints,
                                                showSchedule,
                                                (position, point) -> {
                                                    doCallBack(point, position, scheduleChangeListener,
                                                            scheduleViewClickListener, scheduleEditClickListener,
                                                            specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                    return Unit.INSTANCE;
                                                }
                                        );
                            }

                            if (p.getProfile().startsWith(ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT.name())) {
                                disableVisibiltyForZoneScheduleUI(zoneDetails);
                                HyperStatHelper.Companion.create(p,
                                                ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT,
                                                getContext())
                                        .loadDetailedView(
                                                inflater,
                                                tempProfileViewModels,
                                                linearLayoutZonePoints,
                                                showSchedule,
                                                (position, point) -> {
                                                    doCallBack(point, position, scheduleChangeListener,
                                                            scheduleViewClickListener, scheduleEditClickListener,
                                                            specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                    return Unit.INSTANCE;
                                                }
                                        );
                            }
                            if (p.getProfile().startsWith(ProfileType.HYPERSTAT_HEAT_PUMP_UNIT.name())) {
                                disableVisibiltyForZoneScheduleUI(zoneDetails);
                                HyperStatHelper.Companion.create(p,
                                                ProfileType.HYPERSTAT_HEAT_PUMP_UNIT,
                                                getContext())
                                        .loadDetailedView(
                                                inflater,
                                                tempProfileViewModels,
                                                linearLayoutZonePoints,
                                                showSchedule,
                                                (position, point) -> {
                                                    doCallBack(point, position, scheduleChangeListener,
                                                            scheduleViewClickListener, scheduleEditClickListener,
                                                            specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                    return Unit.INSTANCE;
                                                }
                                        );
                            }
                            if (p.getProfile().startsWith(ProfileType.HYPERSTAT_TWO_PIPE_FCU.name())) {
                                disableVisibiltyForZoneScheduleUI(zoneDetails);
                                HyperStatHelper.Companion.create(p,
                                                ProfileType.HYPERSTAT_TWO_PIPE_FCU,
                                                getContext())
                                        .loadDetailedView(
                                                inflater,
                                                tempProfileViewModels,
                                                linearLayoutZonePoints,
                                                showSchedule,
                                                (position, point) -> {
                                                    doCallBack(point, position, scheduleChangeListener,
                                                            scheduleViewClickListener, scheduleEditClickListener,
                                                            specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                    return Unit.INSTANCE;
                                                }
                                        );
                            }
                            if (p.getProfile().startsWith(ProfileType.HYPERSTATSPLIT_CPU.name())) {
                                disableVisibiltyForZoneScheduleUI(zoneDetails);
                                HyperStatSplitHelper.Companion.create(p,
                                                ProfileType.HYPERSTATSPLIT_CPU,
                                                getContext())
                                        .loadDetailedView(
                                                inflater,
                                                tempProfileViewModels,
                                                linearLayoutZonePoints,
                                                showSchedule,
                                                (position, point) -> {
                                                    doCallBack(point, position, scheduleChangeListener,
                                                            scheduleViewClickListener, scheduleEditClickListener,
                                                            specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                    return Unit.INSTANCE;
                                                }
                                        );
                            }
                            if (p.getProfile().startsWith(ProfileType.MYSTAT_CPU.name())) {
                                disableVisibiltyForZoneScheduleUI(zoneDetails);
                                MyStatHelper.Companion.create(p,
                                                ProfileType.MYSTAT_CPU,
                                                getContext())
                                        .loadDetailedView(
                                                inflater,
                                                tempProfileViewModels,
                                                linearLayoutZonePoints,
                                                showSchedule,
                                                (position, point) -> {
                                                    doCallBack(point, position, scheduleChangeListener,
                                                            scheduleViewClickListener, scheduleEditClickListener,
                                                            specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                    return Unit.INSTANCE;
                                                }
                                        );
                            }
                            if (p.getProfile().startsWith(ProfileType.MYSTAT_PIPE2.name())) {
                                disableVisibiltyForZoneScheduleUI(zoneDetails);
                                MyStatHelper.Companion.create(p,
                                                ProfileType.MYSTAT_PIPE2,
                                                getContext())
                                        .loadDetailedView(
                                                inflater,
                                                tempProfileViewModels,
                                                linearLayoutZonePoints,
                                                showSchedule,
                                                (position, point) -> {
                                                    doCallBack(point, position, scheduleChangeListener,
                                                            scheduleViewClickListener, scheduleEditClickListener,
                                                            specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                    return Unit.INSTANCE;
                                                }
                                        );

                            }
                            if (p.getProfile().startsWith(ProfileType.MYSTAT_HPU.name())) {
                                disableVisibiltyForZoneScheduleUI(zoneDetails);
                                MyStatHelper.Companion.create(p,
                                                ProfileType.MYSTAT_HPU,
                                                getContext())
                                        .loadDetailedView(
                                                inflater,
                                                tempProfileViewModels,
                                                linearLayoutZonePoints,
                                                showSchedule,
                                                (position, point) -> {
                                                    doCallBack(point, position, scheduleChangeListener,
                                                            scheduleViewClickListener, scheduleEditClickListener,
                                                            specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                    return Unit.INSTANCE;
                                                }
                                        );
                            }
                            if (p.getProfile().startsWith(ProfileType.HYPERSTATSPLIT_4PIPE_UV.name())) {
                                disableVisibiltyForZoneScheduleUI(zoneDetails);
                                HyperStatSplitHelper.Companion.create(p,
                                                ProfileType.HYPERSTATSPLIT_4PIPE_UV,
                                                getContext())
                                        .loadDetailedView(
                                                inflater,
                                                tempProfileViewModels,
                                                linearLayoutZonePoints,
                                                showSchedule,
                                                (position, point) -> {
                                                    doCallBack(point, position, scheduleChangeListener,
                                                            scheduleViewClickListener, scheduleEditClickListener,
                                                            specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                    return Unit.INSTANCE;
                                                }
                                        );
                            }
                            if (p.getProfile().startsWith(ProfileType.HYPERSTATSPLIT_2PIPE_UV.name())) {
                                disableVisibiltyForZoneScheduleUI(zoneDetails);
                                HyperStatSplitHelper.Companion.create(p,
                                                ProfileType.HYPERSTATSPLIT_2PIPE_UV,
                                                getContext())
                                        .loadDetailedView(
                                                inflater,
                                                tempProfileViewModels,
                                                linearLayoutZonePoints,
                                                showSchedule,
                                                (position, point) -> {
                                                    doCallBack(point, position, scheduleChangeListener,
                                                            scheduleViewClickListener, scheduleEditClickListener,
                                                            specialScheduleEditClickListener, vacationScheduleEditClickListener);
                                                    return Unit.INSTANCE;
                                                }
                                        );

                            }
                        }
                    }
                    if((namedScheds.isEmpty() || !isSelectedScheduleAvailable) && mScheduleType == 2){
                        scheduleImageButton.setVisibility(View.GONE);
                        namedScheduleView.setVisibility(View.GONE);
                    }
                }
            }
        });

        CcuLog.i("UI_PROFILING","ZoneFragmentNew.viewTemperatureBasedZone Done");

    }
    @SuppressLint("ResourceType")
    private void imageButtonClickListener(View v, String zoneId, String[] equipId,
                                          FragmentManager childFragmentManager2, boolean isSpecial) {
        Schedule schedule = CCUHsApi.getInstance().getScheduleById((String)v.getTag());

        if(OfflineModeUtilKt.isOfflineMode() && schedule.isNamedSchedule() && !isSpecial) {
            TabLayout.Tab selectedTab = RenatusLandingActivity.mTabLayout.getTabAt(3);
            selectedTab.select();
        }else {
            ScheduleGroupFragment scheduleGroupFragment;
            if(isSpecial) {
                scheduleGroupFragment = new ScheduleGroupFragment().showSpecialScheduleLayout(zoneId, mSchedule);
            } else {
                scheduleGroupFragment = new ScheduleGroupFragment(schedule, schedule.getScheduleGroup());
            }
            FragmentManager childFragmentManager = childFragmentManager2;
            childFragmentManager.beginTransaction();
            scheduleGroupFragment.show(childFragmentManager, "dialog");
        }

    }


    private boolean isRemoteChangeApplied = false;
    private void updateTemperatureBasedZones(SeekArc seekArcOpen, View zonePointsOpen, Equip equipOpen,
                                             LayoutInflater inflater, boolean isRemoteChange) {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.updateTemperatureBasedZones");
        isItemSelectedEvent = false;
        boolean isSelectedScheduleAvailable = true;
        Equip p = equipOpen;
        int temperatureMode = CCUHsApi.getInstance().readHisValByQuery("zone and hvacMode and roomRef" +
                " == \"" + p.getRoomRef() + "\"").intValue();
        View zoneDetails = zonePointsOpen;
        SeekArc seekArc = seekArcOpen;
        String equipId = p.getId();

        LinearLayout linearLayoutZonePoints = zoneDetails.findViewById(R.id.lt_profilepoints);
        TextView scheduleStatus = zoneDetails.findViewById(R.id.schedule_status_tv);
        Spinner scheduleSpinner = zoneDetails.findViewById(R.id.schedule_spinner);
        ImageButton scheduleImageButton = zoneDetails.findViewById(R.id.schedule_edit_button);
        ImageButton vacationImageButton = zoneDetails.findViewById(R.id.vacation_edit_button);
        ImageButton specialScheduleImageButton = zoneDetails.findViewById(R.id.special_status_edit_button);
        TextView vacationStatusTV = zoneDetails.findViewById(R.id.vacation_status);
        TextView specialScheduleStatusText = zoneDetails.findViewById(R.id.special_status_status);
        ImageButton namedScheduleView = zoneDetails.findViewById(R.id.schedule_view_button);


        ArrayList<String> scheduleArray = new ArrayList<>();
//        scheduleArray.add("Building Schedule");
        scheduleArray.add("Zone Schedule");
        scheduleArray.add("Shared Schedule");

        ArrayList<Boolean> hasImage = new ArrayList<>();
        hasImage.add(false);
        hasImage.add(false);

        List<HashMap<Object, Object>> namedScheds = CCUHsApi.getInstance().getAllNamedSchedules();
        if(namedScheds.size() > 0){
            for (HashMap<Object, Object> nameSched :
                    namedScheds) {
                String namedScheduledis = Objects.requireNonNull(nameSched.get("dis")).toString();
                if(nameSched.get("default") != null){
                    scheduleArray.add(getTruncatedString("Default - " + CCUHsApi.getInstance().getSiteName(), 25, 0, 25));
                    hasImage.add(true);
                }else if(namedScheduledis.length() > 25){
                    scheduleArray.add(getTruncatedString(Objects.requireNonNull(nameSched.get("dis").toString()), 25, 0, 25));
                    hasImage.add(true);
                }else{
                    scheduleArray.add(Objects.requireNonNull(nameSched.get("dis")).toString());
                    hasImage.add(true);
                }
            }
        }else{
            scheduleArray.add("No Shared Schedule available");
            hasImage.add(false);
            scheduleImageButton.setVisibility(GONE);
            namedScheduleView.setVisibility(GONE);

        }

        CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(getActivity(), R.layout.custom_dropdown_item_with_image, scheduleArray, hasImage);
        scheduleSpinner.setAdapter(adapter);

        String zoneId = Schedule.getZoneIdByEquipId(equipId);

        try {
        if( isCelsiusTunerAvailableStatus()) {
            Observable.fromCallable(() -> ScheduleManager.getInstance().getMultiModuleZoneStatusMessage(zoneId))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(status -> scheduleStatus.setText(StatusCelsiusVal(status, temperatureMode)));
        } else {
            Observable.fromCallable(() -> ScheduleManager.getInstance().getMultiModuleZoneStatusMessage(zoneId))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(status -> scheduleStatus.setText(setPointStatusMessage(status, TemperatureMode.values()[temperatureMode])));
        }
        } catch (Exception e) {
        e.printStackTrace();
        }

        Observable.fromCallable(() -> ScheduleManager.getInstance().getVacationStateString(zoneId))
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(status -> vacationStatusTV.setText(status));

        Observable.fromCallable(() -> ScheduleManager.getInstance().getScheduleStateString(zoneId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> specialScheduleStatusText.setText(status));

        String scheduleTypeId = getScheduleTypeId(equipId);
        final int mScheduleType = (int) CCUHsApi.getInstance().readPointPriorityVal(scheduleTypeId);
        CcuLog.d("ScheduleType", "update mScheduleType==" + mScheduleType + "," + (int) CCUHsApi.getInstance().readPointPriorityVal(scheduleTypeId) + "," + p.getDisplayName());
        mScheduleTypeMap.put(equipId, mScheduleType);
        mSchedule = Schedule.getScheduleByEquipId(equipId);

        if(isRoomsScheduleRefAvailable(equipId)){
            isSelectedScheduleAvailable = false;
            namedScheduleView.setVisibility(View.GONE);
        } else if(mSchedule.isNamedSchedule() && !namedScheds.isEmpty()) {
            namedScheduleView.setVisibility(View.VISIBLE);
        }

        namedScheduleView.setOnClickListener(view -> {

            String scheduleDis = (mSchedule.getDis());
            String scheduleName = (mSchedule.getMarkers().contains("default")) ?
                    "Default - "+CCUHsApi.getInstance().getSiteName() :scheduleDis;

            NamedSchedule namedSchedule =
                    NamedSchedule.getInstance(mSchedule.getId(),
                            zoneId,  scheduleName,false);
            FragmentManager childFragmentManager = getChildFragmentManager();
            namedSchedule.show(childFragmentManager, "dialog");
        });

        scheduleImageButton.setTag(mSchedule.getId());
        vacationImageButton.setTag(mSchedule.getId());
        specialScheduleImageButton.setTag(mSchedule.getId());
        vacationImageButton.setOnClickListener(v ->
        {
            ScheduleGroupFragment schedulerFragment = new ScheduleGroupFragment().showVacationsLayout(zoneId, mSchedule);

            FragmentManager childFragmentManager = getFragmentManager();
            childFragmentManager.beginTransaction();
            schedulerFragment.show(childFragmentManager, "dialog");
        });
        scheduleImageButton.setOnClickListener(v ->
        {
            ScheduleGroupFragment schedulerFragment = new ScheduleGroupFragment(mSchedule, mSchedule.getScheduleGroup());

            FragmentManager childFragmentManager = getFragmentManager();
            childFragmentManager.beginTransaction();
            schedulerFragment.show(childFragmentManager, "dialog");
        });
        specialScheduleImageButton.setOnClickListener(v ->
        {
            ScheduleGroupFragment schedulerFragment = new ScheduleGroupFragment().showSpecialScheduleLayout(zoneId, mSchedule);
            FragmentManager childFragmentManager = getChildFragmentManager();
            childFragmentManager.beginTransaction();
            schedulerFragment.show(childFragmentManager, "dialog");
        });
        if(mScheduleType >= 2){
            if(!isSelectedScheduleAvailable) {
                scheduleSpinner.setSelection(1);
            }else {
                int spinnerposition = 2;
                for (HashMap<Object, Object> a : namedScheds) {
                    if ((Objects.requireNonNull(Objects.requireNonNull(a.get("id"))).toString().substring(1)).equals
                            (Schedule.getScheduleByEquipId(equipId).getId())) {
                        spinnerposition = namedScheds.indexOf(a) + 2;
                    }

                }
                isItemSelectedEvent = true;
                scheduleSpinner.setSelection(spinnerposition, false);
                adapter.setSelectedPosition(spinnerposition);
            }


        }else{
            isItemSelectedEvent = true;
            scheduleSpinner.setSelection(mScheduleType -1, false);
        }
        if (mSchedule.isZoneSchedule()
                || (mSchedule.isNamedSchedule() && mSchedule.getMarkers().contains("default") && !namedScheds.isEmpty()
        && OfflineModeUtilKt.isOfflineMode())){
            scheduleImageButton.setVisibility(View.VISIBLE);
        } else {
            scheduleImageButton.setVisibility(View.GONE);
        }
        isRemoteChangeApplied = isRemoteChange;
        scheduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    prevPosition = currentPosition;
                    currentPosition = position;
                    adapter.setSelectedPosition(position);

                if(isItemSelectedEvent) {
                    isItemSelectedEvent = false;
                    isRemoteChangeApplied = false;
                    return;
                }

               if (position == 0 && (mScheduleType != -1)/*&& (mScheduleType != position)*/) {
                  //  clearTempOverride(equipId);
                    namedScheduleView.setVisibility(View.GONE);
                    scheduleImageButton.setVisibility(View.VISIBLE);

                    if (mSchedule.isZoneSchedule() ) {
                        mSchedule.setDisabled(false);
                        CCUHsApi.getInstance().updateZoneScheduleWithoutUpdatingLastModifiedTime(mSchedule, zoneId);
                        scheduleImageButton.setTag(mSchedule.getId());
                        vacationImageButton.setTag(mSchedule.getId());
                        specialScheduleImageButton.setTag(mSchedule.getId());
                        transitionToZoneSchedules(openZoneMap, mSchedule.getId(), zoneDetails, false);
                    } else {
                       Zone zone = Schedule.getZoneforEquipId(equipId);
                       if (!zone.hasSchedule()) {
                            CcuLog.d(L.TAG_CCU_UI, " Zone does not have Schedule : Shouldn't happen");
                            /* We are in a situation where there is a zone without a scheduleRef.
                             * There might have been an error scenario that prevented attaching the scheduleRef, but
                             * still created a schedule. Handle it before creating a new schedule again.
                             */
                            HashMap<Object, Object> schedule = CCUHsApi.getInstance().readEntity("schedule and " +
                                    "not special and not vacation and roomRef " + "== " +zone.getId());

                           if (schedule.isEmpty()) {
                               DefaultSchedules.setDefaultCoolingHeatingTemp();
                               DefaultSchedules.generateDefaultZoneSchedule(zone.getId());
                           }
                       }
                        HashMap<Object, Object> scheduleHashMap = CCUHsApi.getInstance().readEntity("schedule and " +
                                "not special and not vacation and roomRef " + "== " +zone.getId());

                        Schedule scheduleById = CCUHsApi.getInstance().getScheduleById(scheduleHashMap.get("id").toString());
                        CcuLog.d(L.TAG_CCU_UI, " scheduleType changed to ZoneSchedule : " + scheduleTypeId);
                        scheduleById.setDisabled(false);
                        String scheduleId = scheduleById.getId();

                        ContainmentDialogClickListener containmentDialogClickListener = isForceTrimmed -> {
                            if(isForceTrimmed) {
                                transitionToZoneSchedules(openZoneMap, scheduleId, zoneDetails, false);
                            } else {
                                scheduleSpinner.setSelection(prevPosition);
                            }
                        };

                        boolean isContainment = checkContainment(scheduleById, containmentDialogClickListener);
                        if(mScheduleTypeMap.get(equipId) != ScheduleType.ZONE.ordinal() && isContainment) {
                            transitionToZoneSchedules(openZoneMap, scheduleId, zoneDetails, false);
                            CCUHsApi.getInstance().updateZoneScheduleWithoutUpdatingLastModifiedTime(scheduleById, zone.getId());
                        }
                   }
                } else if (position == 1 && (mScheduleType != -1)) {
                    //No operation as it is a Named Schedule
                } else if (position >= 2 && (mScheduleType != -1) && !isRemoteChangeApplied) {
                   if(!namedScheds.isEmpty()) {
                       mScheduleTypeMap.put(equipId, ScheduleType.NAMED.ordinal());
                       namedScheduleView.setVisibility(View.VISIBLE);
                       scheduleImageButton.setVisibility(View.GONE);
                       HashMap<Object, Object> room = CCUHsApi.getInstance().readMapById(zoneId);
                       HashMap<Object,Object> nameScheduleMap = namedScheds.get(position - 2);
                       String namedScheduleId = nameScheduleMap.get("id").toString();
                       String scheduleDis = (nameScheduleMap.get("dis").toString());
                       if (nameScheduleMap.get("default") != null && OfflineModeUtilKt.isOfflineMode()) {
                           scheduleImageButton.setVisibility(View.VISIBLE);
                       }

                       String scheduleName = (nameScheduleMap.get("default") != null) ?
                               "Default - " + CCUHsApi.getInstance().getSiteName() : scheduleDis;


                       if (!namedScheduleId.equals(room.get("scheduleRef").toString())) {
                           NamedSchedule namedSchedule =
                                   NamedSchedule.getInstance(namedScheduleId,
                                           zoneId, scheduleName, true);
                           FragmentManager childFragmentManager = getChildFragmentManager();
                           namedSchedule.show(childFragmentManager, "dialog");

                           namedSchedule.setOnExitListener(() -> {
                               mSchedule = Schedule.getScheduleByEquipId(equipId);
                               ScheduleManager.getInstance().updateSchedules(equipOpen);
                               Toast.makeText(getContext(), "Refresh View", Toast.LENGTH_LONG).show();
                               scheduleImageButton.setTag(mSchedule.getId());
                               namedScheduleView.setVisibility(View.VISIBLE);
                               vacationImageButton.setTag(mSchedule.getId());
                               specialScheduleImageButton.setTag(mSchedule.getId());
                               if (nameScheduleMap.get("default") != null &&
                                       OfflineModeUtilKt.isOfflineMode()) {
                                   scheduleImageButton.setVisibility(View.VISIBLE);
                               } else
                                   scheduleImageButton.setVisibility(View.GONE);
                               namedScheduleView.setVisibility(View.VISIBLE);
                           });

                           namedSchedule.setOnCancelButtonClickListener(() -> {
                               if(prevPosition == 0) {
                                   scheduleImageButton.setVisibility(View.VISIBLE);
                                   namedScheduleView.setVisibility(View.GONE);
                                   isItemSelectedEvent = true;
                               }
                               scheduleSpinner.setSelection(prevPosition);
                           });
                       }
                   }

                }
                mSchedule = Schedule.getScheduleByEquipId(equipId);
                scheduleImageButton.setTag(mSchedule.getId());
                vacationImageButton.setTag(mSchedule.getId());
                specialScheduleImageButton.setTag(mSchedule.getId());
                isRemoteChangeApplied = false;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        float buildingLimitMin ;
        float buildingLimitMax ;
        float coolingUpperLimitVal ;
        float heatUpperLimitVal ;
        float coolingLowerLimitVal ;
        float heatLowerLimitVal ;
        float heatingDesired ;
        float coolingDesired ;
        float heatingDeadBand ;
        float coolingDeadBand ;
        double currentTemp = 0;
        float avgTemp = 0;

            String roomRefZone = StringUtils.prependIfMissing(p.getRoomRef(), "@");
            buildingLimitMin = BuildingTunerCache.getInstance().getBuildingLimitMin().floatValue();
            buildingLimitMax = BuildingTunerCache.getInstance().getBuildingLimitMax().floatValue();
            heatUpperLimitVal = CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and point and limit and user and max and heating and roomRef == \"" + roomRefZone + "\"").floatValue();
            heatLowerLimitVal = CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and point and limit and user and min and heating and roomRef == \"" + roomRefZone + "\"").floatValue();
            coolingLowerLimitVal = CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and point and limit and user and min and cooling and roomRef == \"" + roomRefZone + "\"").floatValue();
            coolingUpperLimitVal = CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and point and limit and user and max and cooling and roomRef == \"" + roomRefZone + "\"").floatValue();

            if (CCUUiUtil.isDomainEquip(p.getId(), "equip")) {
                coolingDesired = CCUUiUtil.readPriorityValByRoomRef(DomainName.desiredTempCooling, roomRefZone).floatValue();
                heatingDesired = CCUUiUtil.readPriorityValByRoomRef(DomainName.desiredTempHeating, roomRefZone).floatValue();
            } else {
                coolingDesired = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and cooling and roomRef == \"" + roomRefZone + "\"").floatValue();
                heatingDesired = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and heating and roomRef == \"" + roomRefZone + "\"").floatValue();
        }
            heatingDeadBand = CCUHsApi.getInstance().readPointPriorityValByQuery("heating and deadband and zone and not multiplier and roomRef == \"" + roomRefZone + "\"").floatValue();
            coolingDeadBand = CCUHsApi.getInstance().readPointPriorityValByQuery("cooling and deadband and zone and not multiplier and roomRef == \"" + roomRefZone + "\"").floatValue();


        int modeType = CCUHsApi.getInstance().readHisValByQuery("zone and hvacMode and roomRef == \"" + zoneId + "\"").intValue();


        int noTempSensor = 0;
        for (int k = 0; k < openZoneMap.size(); k++) {
            Equip updatedEquip = new Equip.Builder().setHashMap(openZoneMap.get(k)).build();
            int statusVal;
            if (CCUUiUtil.isDomainEquip(updatedEquip.getId(), "equip")) {
                statusVal = CCUUiUtil.readHisValByEquipRef(DomainName.equipStatus, updatedEquip.getId()).intValue();
            } else {
                statusVal = CCUHsApi.getInstance().readHisValByQuery("point and not ota and status and his and not writable and equipRef ==\"" + updatedEquip.getId() + "\"").intValue();
            }
            if (statusVal != ZoneState.TEMPDEAD.ordinal() && statusVal != ZoneState.RFDEAD.ordinal()) {
                if (CCUUiUtil.isDomainEquip(updatedEquip.getId(), "equip")) {
                    currentTemp += CCUHsApi.getInstance().readHisValByQuery("domainName == \"" + DomainName.currentTemp + "\" and equipRef == \"" + updatedEquip.getId() + "\"");
                }else{
                    currentTemp += CCUHsApi.getInstance().readHisValByQuery("temp and sensor and (current or space) and equipRef == \"" + updatedEquip.getId() + "\"");
                }
            } else {
               noTempSensor++;
            }
            if (updatedEquip.getProfile().startsWith("SMARTSTAT_TWO_PIPE_FCU")) {
                if (k == 0) linearLayoutZonePoints.removeAllViews();
                HashMap p2FCUPoints = ZoneViewData.get2PFCUEquipPoints(updatedEquip.getId());
                loadSS2PFCUPointsUI(p2FCUPoints, inflater, linearLayoutZonePoints, equipId, true, updatedEquip.getGroup());

            }
            if (updatedEquip.getProfile().startsWith("SMARTSTAT_FOUR_PIPE_FCU")) {
                if (k == 0) linearLayoutZonePoints.removeAllViews();
                HashMap p4FCUPoints = ZoneViewData.get4PFCUEquipPoints(updatedEquip.getId());
                loadSS4PFCUPointsUI(p4FCUPoints, inflater, linearLayoutZonePoints, equipId, true, updatedEquip.getGroup());
            }
            if (updatedEquip.getProfile().startsWith("SMARTSTAT_CONVENTIONAL_PACK_UNIT")) {
                if (k == 0) linearLayoutZonePoints.removeAllViews();
                HashMap cpuEquipPoints = ZoneViewData.getCPUEquipPoints(updatedEquip.getId());
                loadSSCPUPointsUI(cpuEquipPoints, inflater, linearLayoutZonePoints, updatedEquip.getId(), true, updatedEquip.getGroup(), false);
                isCPUloaded = true;
            }
            if (updatedEquip.getProfile().startsWith("SMARTSTAT_HEAT_PUMP_UNIT")) {
                if (k == 0) linearLayoutZonePoints.removeAllViews();
                HashMap hpuEquipPoints = ZoneViewData.getHPUEquipPoints(updatedEquip.getId());
                loadSSHPUPointsUI(hpuEquipPoints, inflater, linearLayoutZonePoints, updatedEquip.getId(), true, updatedEquip.getGroup(), false);
                isHPUloaded = true;
            }
            if (updatedEquip.getProfile().startsWith("DUAL_DUCT")) {
                if (k == 0) linearLayoutZonePoints.removeAllViews();
                HashMap dualDuctPoints = DualDuctUtil.getEquipPointsForView(updatedEquip.getId());
                loadDualDuctPointsUI(dualDuctPoints, inflater, linearLayoutZonePoints, updatedEquip.getGroup());
            }

            if (updatedEquip.getProfile().startsWith(ProfileType.HYPERSTAT_VRV.name())) {
                if (k == 0) linearLayoutZonePoints.removeAllViews();
                HyperStatVrvZoneViewKt.loadView(inflater, linearLayoutZonePoints,
                                                updatedEquip.getId(), CCUHsApi.getInstance(), getActivity(),
                                                p.getGroup());
            }
        }

        if((namedScheds.isEmpty() || !isSelectedScheduleAvailable) && mScheduleType == 2 ){
            scheduleImageButton.setVisibility(View.GONE);
            namedScheduleView.setVisibility(View.GONE);
        }
        if(!openZoneMap.isEmpty()) {
            avgTemp = (float)currentTemp / (openZoneMap.size() - noTempSensor);
        }
        if(heatingDesired != 0 && coolingDesired !=0)
            seekArc.setData(seekArc.isDetailedView(), buildingLimitMin, buildingLimitMax, heatLowerLimitVal, heatUpperLimitVal,
                    coolingLowerLimitVal, coolingUpperLimitVal, heatingDesired, coolingDesired, (float) (Math.round(avgTemp * 10.0) / 10.0),
                    heatingDeadBand, coolingDeadBand,modeType);
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.updateTemperatureBasedZones Done");

    }

    ZoneData zoneDataForUi= null;
    private void viewNonTemperatureBasedZone(LayoutInflater inflater, View rootView,
                                             ArrayList<HashMap> zoneMap, String zoneTitle, int gridPosition,
                                             LinearLayout[] tablerowLayout, boolean isZoneAlive, String roomRef)
    {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.viewNonTemperatureBasedZone");
        cleanUpNonTempViewModel(nonTempProfileViewModels);
        nonTempProfileViewModels.clear();
        zoneDataForUi = null;
        if(zoneViewModel != null){
            zoneViewModel.stopObservingZoneHealth();
            zoneViewModel = null;
        }
        CCUHsApi.getInstance().registerPointScheduleUpdateInf(this);
        Equip p = null;
        int i = gridPosition;
        View arcView = null;
        arcView = inflater.inflate(R.layout.zones_item_nontemp, (ViewGroup) rootView, false);
        View zoneDetails = inflater.inflate(R.layout.zones_item_details, null);
        TextView vacationStatusTV = zoneDetails.findViewById(R.id.vacation_status);
        TextView vacationText = zoneDetails.findViewById(R.id.vacationText);
        ImageView vacationEditButton = zoneDetails.findViewById(R.id.vacation_edit_button);
        LinearLayout linearLayoutZonePoints = zoneDetails.findViewById(R.id.lt_profilepoints);
        ComposeView embededComposeView = zoneDetails.findViewById(R.id.compose_profile_points);
        GridItem gridItemObj = new GridItem();
        gridItemObj.setGridID(i);
        gridItemObj.setGridItem("NonTemp");
        arcView.setClickable(true);
        arcView.setTag(gridItemObj);
        arcView.setId(i);
        //ImageView imageView = arcView.findViewById(R.id.imageView);
        NonTempControl nonTempControl = arcView.findViewById(R.id.rl_nontemp);
        //imageView.setTag(gridItemObj);
        nonTempControl.setTag(gridItemObj);
        TextView textEquipment = arcView.findViewById(R.id.textEquipment);
        textEquipment.setTextColor(getResources().getColor(R.color.text_color));
        textEquipment.setText(zoneTitle);
        TextView textViewModule = arcView.findViewById(R.id.module_status);
        View status_view = arcView.findViewById(R.id.status_view);
        HeartBeatUtil.zoneStatus(textViewModule, isZoneAlive);
        zoneStatus.put(zoneTitle, textViewModule);
        LinearLayout.LayoutParams rowLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        arcView.setPadding(48, 56, 0, 0);
        try {
            tablerowLayout[rowcount].addView(arcView, rowLayoutParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (((i + 1) % columnCount == 0) && (i != 0)) {
            try {
                if (tablerowLayout[rowcount].getParent() != null) {
                    ((ViewGroup) tablerowLayout[rowcount].getParent()).removeView(tablerowLayout[rowcount]);
                }
                tableLayout.addView(tablerowLayout[rowcount++]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (rowcount < numRows)
                tablerowLayout[rowcount] = new LinearLayout(tableLayout.getContext());
        }
        if (rowcount < numRows) {
            try {
                if (tablerowLayout[rowcount].getParent() != null) {
                    ((ViewGroup) tablerowLayout[rowcount].getParent()).removeView(tablerowLayout[rowcount]);
                }
                tableLayout.addView(tablerowLayout[rowcount]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!zoneMap.isEmpty()) {
            CcuLog.i("ProfileTypes", "Points:" + zoneMap);
            p = new Equip.Builder().setHashMap(zoneMap.get(0)).build();
            HashMap zoneEquips = zoneMap.get(0);
            if ((zoneEquips.get("profile").toString()).contains("PLC")) {
                vacationStatusTV.setVisibility(View.GONE);
                vacationText.setVisibility(View.GONE);
                vacationEditButton.setVisibility(View.GONE);
                nonTempControl.setEquipType(1);
                nonTempControl.setImage(R.drawable.ic_zone_piloop);
                nonTempControl.setImageViewExpanded(R.drawable.ic_zone_piloop_max);
            }
            if ((zoneEquips.get("profile").toString()).contains("TEMP_MONITOR")) {
                nonTempControl.setImage(R.drawable.ic_zone_tempmonitor);
                nonTempControl.setImageViewExpanded(R.drawable.ic_zone_tempmonitor);
            }
            if ((zoneEquips.get("profile").toString()).contains("TEMP_INFLUENCE")) {
                nonTempControl.setImage(R.drawable.ic_zone_tempmonitor);
                nonTempControl.setImageViewExpanded(R.drawable.ic_zone_tempmonitor);
            }
            if ((zoneEquips.get("profile").toString()).contains("EMR")) {
                vacationStatusTV.setVisibility(View.GONE);
                vacationText.setVisibility(View.GONE);
                vacationEditButton.setVisibility(View.GONE);
                nonTempControl.setEquipType(0);
                nonTempControl.setImage(R.drawable.ic_zone_em);
                nonTempControl.setImageViewExpanded(R.drawable.ic_zone_em_max);
            }
            if ((zoneEquips.get("profile").toString()).contains("MODBUS")) {
                vacationStatusTV.setVisibility(View.GONE);
                vacationText.setVisibility(View.GONE);
                vacationEditButton.setVisibility(View.GONE);
                nonTempControl.setEquipType(2);
                nonTempControl.setImage(R.drawable.ic_zone_modbus);
                nonTempControl.setImageViewExpanded(R.drawable.ic_zone_modbus_mx);
            }
            if ((zoneEquips.get("profile").toString()).contains("BACNET_DEFAULT")) {
                vacationStatusTV.setVisibility(View.GONE);
                vacationText.setVisibility(View.GONE);
                vacationEditButton.setVisibility(View.GONE);
                nonTempControl.setEquipType(2);
                nonTempControl.setImage(R.drawable.ic_bacnet_logo);
                nonTempControl.setImageViewExpanded(R.drawable.ic_bacnet_logo);
            }
            if ((zoneEquips.get("profile").toString()).contains("MODBUS") && (zoneEquips.get("profile").toString()).contains("EMR")) {
                vacationStatusTV.setVisibility(View.GONE);
                vacationText.setVisibility(View.GONE);
                vacationEditButton.setVisibility(View.GONE);
                nonTempControl.setEquipType(2);
                nonTempControl.setImage(R.drawable.ic_zone_em);
                nonTempControl.setImageViewExpanded(R.drawable.ic_zone_em_max);
            }
            if (ConnectNodeUtil.Companion.isConnectNodePaired(roomRef)) {
                vacationStatusTV.setVisibility(View.GONE);
                vacationText.setVisibility(View.GONE);
                vacationEditButton.setVisibility(View.GONE);
                nonTempControl.setEquipType(2);
                nonTempControl.setImage(R.drawable.ic_zone_modbus);
                nonTempControl.setImageViewExpanded(R.drawable.ic_zone_modbus_mx);
            }

        } else if (ConnectNodeUtil.Companion.isConnectNodePaired(roomRef)) {
                vacationStatusTV.setVisibility(View.GONE);
                vacationText.setVisibility(View.GONE);
                vacationEditButton.setVisibility(View.GONE);
                nonTempControl.setEquipType(2);
                nonTempControl.setImage(R.drawable.ic_zone_modbus);
                nonTempControl.setImageViewExpanded(R.drawable.ic_zone_modbus_mx);
        } else {
            //No devices paired zone
            nonTempControl.setEquipType(2);
            nonTempControl.setImage(R.drawable.ic_no_device_paired_icon);
            nonTempControl.setImageViewExpanded(R.drawable.ic_no_device_paired_expanded_icon);
        }
        ScaleControlToNormal(270, 210, nonTempControl);
        nonTempControl.setExpand(false);
        Equip nonTempEquip = p;

        nonTempControl.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View v) {
                boolean isExpanded = false;
                GridItem gridItemNew = (GridItem) v.getTag();
                int clickposition = gridItemNew.getGridID();
                if (clickedView != -1) {
                    if (clickposition != clickedView) {
                        CcuUtilsKt.stopObservingAllEquipHealth(nonTempProfileViewModels);
                        cleanUpNonTempViewModel(nonTempProfileViewModels);
                        nonTempProfileViewModels.clear();
                        zoneDataForUi = null;
                        if(zoneViewModel != null){
                            zoneViewModel.stopObservingZoneHealth();
                            zoneViewModel = null;
                        }
                        int tableRowCount = tableLayout.getChildCount();
                        if (tableLayout.getChildCount() > 1) {
                            boolean viewFound = false;
                            for (int row = 0; row < tableRowCount; row++) {
                                View rowView = tableLayout.getChildAt(row);
                                LinearLayout tableRow = (LinearLayout) rowView;
                                int cellCount = tableRow.getChildCount();
                                for (int j = 0; j < cellCount; j++) {
                                    RelativeLayout gridItem = (RelativeLayout) tableRow.getChildAt(j);
                                    GridItem viewTag = (GridItem) gridItem.getTag();
                                    if (viewTag.getGridID() == clickedView) {
                                        View statusView = gridItem.findViewById(R.id.status_view);
                                        statusView.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                        TextView textViewzone = (TextView) gridItem.findViewById(R.id.textEquipment);
                                        textViewzone.setTextAppearance(getActivity(), R.style.label_black);
                                        textViewzone.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                        tableLayout.removeViewAt(row + 1);
                                        gridItem.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                        if (viewTag.getGridItem().equals("NonTemp")) {
                                            //ImageView imageViewExpanded = gridItem.findViewById(R.id.imageView);
                                            NonTempControl nonTempControl = gridItem.findViewById(R.id.rl_nontemp);
                                            //ScaleImageToNormal(250,210,imageViewExpanded);
                                            ScaleControlToNormal(270, 210, nonTempControl);
                                            nonTempControl.setExpand(false);
                                            nonTempControl.setBackgroundColor(getResources().getColor(R.color.white));
                                            gridItem.invalidate();
                                        } else {
                                            SeekArc seekArcExpanded = gridItem.findViewById(R.id.seekArc);
                                            seekArcExpanded.setDetailedView(false);
                                            seekArcExpanded.setBackgroundColor(getResources().getColor(R.color.white));
                                            seekArcExpanded.scaletoNormal(260, 210);

                                        }
                                        imageOn = false;
                                        viewFound = true;
                                        break;
                                    }
                                }
                                if (viewFound) {
                                    break;
                                }
                            }
                            zoneOpen = false;
                            zoneNonTempOpen = true;
                            zonePointsOpen = zoneDetails;
                            equipOpen = nonTempEquip;
                            openZoneMap = zoneMap;
                            clickedView = gridItemNew.getGridID();
                            v.setBackgroundColor(getActivity().getResources().getColor(R.color.zoneselection_gray));
                            status_view.setBackgroundColor(getActivity().getResources().getColor(R.color.zoneselection_gray));

                            int index = clickedView / columnCount + 1;
                            //ScaleImageToBig(250,210,imageView);
                            ScaleControlToExpand(250, 210, nonTempControl);
                            nonTempControl.setExpand(true);
                            nonTempControlOpen = nonTempControl;
                            imageOn = true;
                            isExpanded = true;
                            try {
                                textEquipment.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                                zoneDetails.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                                tableLayout.addView(zoneDetails, index);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        CcuUtilsKt.stopObservingAllEquipHealth(nonTempProfileViewModels);
                        cleanUpNonTempViewModel(nonTempProfileViewModels);
                        zoneDataForUi = null;
                        nonTempProfileViewModels.clear();
                        if(zoneViewModel != null){
                            zoneViewModel.stopObservingZoneHealth();
                            zoneViewModel = null;
                        }
                        v.setBackgroundColor(getResources().getColor(R.color.white));
                        status_view.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                        textEquipment.setTextAppearance(getActivity(), R.style.label_black);
                        textEquipment.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                        tableLayout.removeView(zoneDetails);
                        imageOn = false;
                        ScaleControlToNormal(270, 210, nonTempControl);
                        nonTempControl.setExpand(false);
                        showWeather();
                        clickedView = -1;
                        isExpanded = false;
                    }
                } else {
                    zoneOpen = false;
                    zoneNonTempOpen = true;
                    zonePointsOpen = zoneDetails;
                    equipOpen = nonTempEquip;
                    openZoneMap = zoneMap;
                    clickedView = gridItemNew.getGridID();
                    v.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                    status_view.setBackgroundColor(getActivity().getResources().getColor(R.color.zoneselection_gray));
                    int index = clickedView / columnCount + 1;
                    ScaleControlToExpand(250, 210, nonTempControl);
                    nonTempControl.setExpand(true);
                    nonTempControlOpen = nonTempControl;
                    hideWeather();
                    imageOn = true;
                    isExpanded = true;
                    try {
                        textEquipment.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        zoneDetails.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        tableLayout.addView(zoneDetails, index);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (isExpanded) {
                    linearLayoutZonePoints.removeAllViews();
                    // below code is to observe zone health
                    if (!zoneMap.isEmpty() && zoneMap.get(0) != null) {
                        zoneViewModel = ZoneViewModel.Companion.create(
                                zoneMap.get(0).get("roomRef").toString()
                        );
                        zoneViewModel.setHeartbeatView(textViewModule);
                        zoneViewModel.setSeekArc(seekArcOpen);
                        zoneViewModel.setEquip(equipOpen);
                        zoneViewModel.setExternalEquip(true);
                        zoneViewModel.observeZoneHealth();
                    }

                    if (ConnectNodeUtil.Companion.isConnectNodePaired(roomRef)) {
                        HashMap<Object, Object> connectNodeDevice = ConnectNodeUtil.Companion.getConnectNodeForZone(roomRef, CCUHsApi.getInstance());
                        disableVisibiltyForZoneScheduleUI(zoneDetails);
                        displayRoomSpecificCustomControlFields(zoneDetails, nonTempEquip);
                        zoneDataForUi = new ZoneData(zoneDetails, nonTempEquip, true);
                        List<EquipmentDevice> modbusDevices = new ArrayList<>();
                        for (EquipmentDevice equipmentDevice : buildModbusModel(roomRef)) {
                            modbusDevices.add(equipmentDevice);
                            if (equipmentDevice.getEquips() != null) {
                                modbusDevices.addAll(equipmentDevice.getEquips());
                            }
                        }
                        View zoneDetails = inflater.inflate(R.layout.item_modbus_detail_view, null);
                        if (modbusDevices.isEmpty()) {
                            zoneDetails.findViewById(R.id.equipment_type_layout).setVisibility(View.VISIBLE);
                            zoneDetails.findViewById(R.id.last_updated_layout).setVisibility(View.VISIBLE);
                            TextView textViewModule = zoneDetails.findViewById(R.id.module_status);
                            TextView tvEquipmentType = zoneDetails.findViewById(R.id.tvEquipmentType);
                            TextView textViewUpdatedTimeHeading = zoneDetails.findViewById(R.id.last_updated);
                            TextView textViewUpdatedTime = zoneDetails.findViewById(R.id.last_updated_status);
                            String connectNodeAddr = connectNodeDevice.get("addr").toString();
                            tvEquipmentType.setText("Connect Node (" + connectNodeAddr + ")");
                            textViewModule.setVisibility(View.VISIBLE);
                            textViewUpdatedTimeHeading.setVisibility(View.VISIBLE);
                            textViewUpdatedTime.setVisibility(View.VISIBLE);

                            LinearLayout noEquipPairedLayout = zoneDetails.findViewById(R.id.noEquipPaired);
                            LinearLayout modbusLayout = zoneDetails.findViewById(R.id.modbus_layout_detain);
                            TextView noModelMessage = zoneDetails.findViewById(R.id.tvNoEquipPaired);
                            String message = "<b>No Model Configured.</b> Go to Site Sequencer to set up a model for this Connect Node.";
                            noModelMessage.setText(HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY));
                            noEquipPairedLayout.setVisibility(View.VISIBLE);
                            modbusLayout.setPadding(40, 0, 0, 0);
                            noEquipPairedLayout.setBackgroundColor(getResources().getColor(R.color.lite_orange));
                            linearLayoutZonePoints.addView(zoneDetails);
                            return;
                        }

                        linearLayoutZonePoints.addView(zoneDetails);

                        List<HashMap<Object, Object>> connectNodeEquips = CCUHsApi.getInstance().readAllEntities(
                                "equip and not equipRef and roomRef == \"" + roomRef + "\""
                        );
                        CcuLog.i(L.TAG_CONNECT_NODE, "ConnectNode equips : " + connectNodeEquips);

                        if (nonTempEquip == null) {
                            CcuLog.i(L.TAG_CONNECT_NODE, "Non temp equips are null.");
                            return;
                        }
                        int index = 0;
                        for (EquipmentDevice modbusDevice : ConnectNodeUtil.Companion.reorderEquipments(fetchAllModbusEquips(nonTempEquip))) {
                            boolean isLastUpdatedTimeShowable = false;
                            if(index == 0) {
                                index =1;
                                isLastUpdatedTimeShowable = true;
                            }
                            loadConnectModuleZone(
                                    nonTempProfileViewModels,
                                    modbusDevice.deviceEquipRef,
                                    getEquipmentDeviceName(CONNECTMODULE, modbusDevice),
                                    isLastUpdatedTimeShowable,
                                    modbusDevice,
                                    inflater.inflate(R.layout.item_modbus_detail_view, null),
                                    linearLayoutZonePoints,
                                    connectNodeDevice
                            );
                        }
                    } else if (nonTempEquip != null) {
                        if (nonTempEquip.getProfile().startsWith("EMR")) {
                            disableVisibiltyForZoneScheduleUI(zoneDetails);
                            NonTempProfileViewModel viewModel = new NonTempProfileViewModel();
                            nonTempProfileViewModels.add(viewModel);
                            viewModel.setEquipId(nonTempEquip.getId());
                            showHeaderViewUI(embededComposeView,
                                    viewModel,
                                    nonTempEquip.getId()
                            );
                            viewModel.setEquipName("Energy Meter (" + nonTempEquip.getGroup() + ")");
                            viewModel.setProfile("EMR");
                            viewModel.loadEmrPoints(nonTempEquip.getId());
                            viewModel.observeEquipHealthByGroupId(nonTempEquip.getGroup());
                        }
                        if (nonTempEquip.getProfile().startsWith("PLC")) {
                            linearLayoutZonePoints.setVisibility(View.GONE);
                            disableVisibiltyForZoneScheduleUI(zoneDetails);
                            NonTempProfileViewModel viewModel = new NonTempProfileViewModel();
                            nonTempProfileViewModels.add(viewModel);
                            viewModel.setProfile("PLC");
                            viewModel.setEquipId(nonTempEquip.getId());
                            viewModel.setEquipName("Pi Loop Controller ("+nonTempEquip.getGroup()+")");
                            showHeaderViewUI(embededComposeView,
                                    viewModel,
                                    nonTempEquip.getId()
                            );
                            List<HeaderViewItem> headerItems = viewModel.getPlcUiItems(nonTempEquip.getId());
                            viewModel.initializeHeaderViewPoints(headerItems);
                            viewModel.observeEquipHealthByGroupId(nonTempEquip.getGroup());
                        }
                        if (nonTempEquip.getProfile().startsWith("MODBUS")) {
                            // Showing point based schedule layout for modbus devices
                            disableVisibiltyForZoneScheduleUI(zoneDetails);
                            displayRoomSpecificCustomControlFields(zoneDetails, nonTempEquip);
                            zoneDataForUi = new ZoneData(zoneDetails, nonTempEquip, true);
                            HashMap<Object, Object> parentModbusEquip = CCUHsApi.getInstance().readEntity("equip " +  // parent modbbus equip
                                    "and not equipRef and roomRef  == " + "\""+nonTempEquip.getRoomRef()+"\"");

                            for (EquipmentDevice modbusDevice: fetchAllModbusEquips(nonTempEquip)) {
                                loadModbusZone(
                                        nonTempProfileViewModels,
                                        modbusDevice.deviceEquipRef,
                                        getEquipmentDeviceName("MODBUS", modbusDevice),
                                        isLastUpdatedTimeShowable(parentModbusEquip, modbusDevice),
                                        modbusDevice,
                                        inflater.inflate(R.layout.item_modbus_detail_view, null),
                                        linearLayoutZonePoints
                                );
                            }
                        }
                        if (nonTempEquip.getProfile().startsWith("BACNET")) {
                            disableVisibiltyForZoneScheduleUI(zoneDetails);
                            displayRoomSpecificCustomControlFields(zoneDetails, nonTempEquip);
                            zoneDataForUi = new ZoneData(zoneDetails, nonTempEquip, true);
                            HashMap<Object,Object> bacnetDevice = CCUHsApi.getInstance().readEntity("device and equipRef == \"" + nonTempEquip.getId() + "\"");
                            visibleEquip = nonTempEquip;
                            loadBacnetZone(
                                    nonTempProfileViewModels,
                                    nonTempEquip.getId(),
                                    getEquipmentDeviceName("BACNET", nonTempEquip),
                                    true,
                                    bacnetDevice,
                                    inflater.inflate(R.layout.item_modbus_detail_view, null),
                                    linearLayoutZonePoints,
                                    bacNetPointsList,
                                    remotePointUpdateInterface
                            );
                        }
                    } else {
                        //Non paired devices
                        disableVisibiltyForZoneScheduleUI(zoneDetails);
                        loadNoDevicesPairedUI(inflater, linearLayoutZonePoints);
                    }
                }
            }
        });
        isItemSelectedEvent = false;
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.viewNonTemperatureBasedZone Done");

    }

    private RemotePointUpdateInterface remotePointUpdateInterface = (message, id, value) -> {
        CcuLog.d(LOG_TAG, "--updateMessage::>> " + message);
        if(isAdded() && isResumed()) {
            FragmentActivity activity = getActivity();
            if(activity != null) {
                activity.runOnUiThread(() -> {
                    Toast.makeText(CCUHsApi.getInstance().getContext(), message, Toast.LENGTH_SHORT).show();
                });
            }
        }
        if(!isPointFollowingScheduleOrEvent(id)) {
            CCUHsApi.getInstance().writeDefaultValById(id, Double.parseDouble(value));
            CCUHsApi.getInstance().writeHisValById(id, Double.parseDouble(value));
        }
    };


    private boolean isLastUpdatedTimeShowable(HashMap<Object, Object> parentModbusEquip, EquipmentDevice modbusDevice) {
        return (Integer.parseInt(parentModbusEquip.get("group").toString()) == modbusDevice.getSlaveId() &&
                (null == modbusDevice.getEquipRef() || (modbusDevice.getEquipRef() == modbusDevice.getDeviceEquipRef())))
                || (Integer.parseInt(parentModbusEquip.get("group").toString()) != modbusDevice.getSlaveId());
    }

    public void loadVAVPointsUI(HashMap vavPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String nodeAddress) {
        HashMap<Object, Object> equip = CCUHsApi.getInstance().readEntity("equip and group == \"" + nodeAddress + "\"");
        Equip updatedEquip = new Equip.Builder().setHashMap(equip).build();
        boolean isACB = updatedEquip.getProfile().equals(ProfileType.VAV_ACB.name());
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);
        View viewPointRow2 = inflater.inflate(R.layout.zones_item_type1, null);
        View viewDischarge = inflater.inflate(R.layout.zones_item_discharge, null);
        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);

        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);

        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);
        TextView textViewLabel3 = viewPointRow2.findViewById(R.id.text_point1label);
        TextView textViewLabel4 = viewPointRow2.findViewById(R.id.text_point2label);

        TextView textViewValue1 = viewPointRow1.findViewById(R.id.text_point1value);
        TextView textViewValue2 = viewPointRow1.findViewById(R.id.text_point2value);
        TextView textViewValue3 = viewPointRow2.findViewById(R.id.text_point1value);
        TextView textViewValue4 = viewPointRow2.findViewById(R.id.text_point2value);

        textViewTitle.setText(vavPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(vavPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        textViewLabel1.setText("Damper : ");
        textViewValue1.setText(vavPoints.get("Damper").toString());
        textViewLabel2.setText(isACB ? "CHW Valve : " : "Reheat Coil : ");
        textViewValue2.setText(vavPoints.get("Reheat Coil").toString());
        textViewLabel3.setText("Discharge Airflow Temperature: ");
        if( isCelsiusTunerAvailableStatus()) {
            textViewValue3.setText(String.valueOf(fahrenheitToCelsiusTwoDecimal(Double.parseDouble(vavPoints.get("Discharge Airflow").toString().replaceAll("[^0-9\\.]",""))))+ " \u00B0C");
        } else {
            textViewValue3.setText(vavPoints.get("Discharge Airflow").toString());
        }
        textViewLabel4.setText("Entering Airflow Temperature: ");
        if( isCelsiusTunerAvailableStatus()) {
            textViewValue4.setText(fahrenheitToCelsiusTwoDecimal(Double.parseDouble(vavPoints.get("Entering Airflow").toString().replaceAll("[^0-9\\.]", ""))) + " \u00B0C");
        } else {
            textViewValue4.setText(vavPoints.get("Entering Airflow").toString());
        }
        if (!Boolean.TRUE.equals(vavPoints.get(AIRFLOW_SENSOR)))  viewDischarge.setVisibility(View.GONE);
        if (displayValve(nodeAddress, isACB)) {
            textViewValue2.setVisibility(View.VISIBLE);
            textViewLabel2.setVisibility(View.VISIBLE);
        } else {
            LinearLayout.LayoutParams existingLayoutParams
                    = (LinearLayout.LayoutParams) textViewValue1.getLayoutParams();

            LinearLayout.LayoutParams newLayoutParams = new LinearLayout.LayoutParams(
                    existingLayoutParams.width,
                    existingLayoutParams.height,
                    existingLayoutParams.weight
            );
            newLayoutParams.setMargins(existingLayoutParams.leftMargin, existingLayoutParams.topMargin, existingLayoutParams.rightMargin, existingLayoutParams.bottomMargin);

            textViewLabel3.setLayoutParams(newLayoutParams);
            textViewValue3.setLayoutParams(newLayoutParams);
            textViewValue2.setVisibility(View.GONE);
            textViewLabel2.setVisibility(View.GONE);
        }
        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        linearLayoutZonePoints.addView(viewPointRow1);
        linearLayoutZonePoints.addView(viewPointRow2);
        if (TrueCFMUtil.isTrueCfmEnabled(CCUHsApi.getInstance(), updatedEquip.getId())) {
            View viewAirflowCFM = inflater.inflate(R.layout.zone_item_airflow_cfm, null);
            TextView airFlowCFMValue = viewAirflowCFM.findViewById(R.id.text_airflow_cfm_value);
            airFlowCFMValue.setText(vavPoints.get("Airflow CFM").toString());
            linearLayoutZonePoints.addView(viewAirflowCFM);
            viewAirflowCFM.setPadding(0, 0, 0, 40);
        }else {
            viewPointRow2.setPadding(0, 0, 0, 40);
        }
        if (isACB) {
            textViewLabel4.setVisibility(View.GONE);
            textViewValue4.setVisibility(View.GONE);
        } else {
            textViewLabel4.setVisibility(View.VISIBLE);
            textViewValue4.setVisibility(View.VISIBLE);
        }
    }

    private boolean displayValve(String nodeAddress, boolean isACB) {
        return isACB ?
                CCUHsApi.getInstance().readDefaultVal("domainName == \"" + DomainName.valveType + "\" and group == \""+nodeAddress+"\"") > 0.0 :
                CCUHsApi.getInstance().readDefaultVal("point and domainName == \"" + DomainName.reheatType + "\" and group == \""+nodeAddress+"\"") > 0.0;
    }

    public void loadTIPointsUI(HashMap tiPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String nodeAddress) {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);
        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        textViewLabel1.setVisibility(View.GONE);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);
        textViewLabel2.setVisibility(View.GONE);
        TextView textViewValue1 = viewPointRow1.findViewById(R.id.text_point1value);
        textViewValue1.setVisibility(View.GONE);
        TextView textViewValue2 = viewPointRow1.findViewById(R.id.text_point2value);
        textViewValue2.setVisibility(View.GONE);

        textViewTitle.setText(tiPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(tiPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        viewPointRow1.setPadding(0, 0, 0, 40);
        linearLayoutZonePoints.addView(viewPointRow1);
    }

    public void loadDABPointsUI(HashMap dabPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String nodeAddress) {
        HashMap<Object, Object> equip = CCUHsApi.getInstance().readEntity("equip and group == \"" + nodeAddress + "\"");
        Equip updatedEquip = new Equip.Builder().setHashMap(equip).build();
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);
        View viewDischarge = inflater.inflate(R.layout.zones_item_discharge, null);


        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);
        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);
        TextView textViewValue1 = viewPointRow1.findViewById(R.id.text_point1value);
        TextView textViewValue2 = viewPointRow1.findViewById(R.id.text_point2value);

        if (BuildConfig.BUILD_TYPE.equalsIgnoreCase(CARRIER_PROD)) {
            textViewTitle.setText("VVT-C" + " (" + nodeAddress + ")");
        } else {
            textViewTitle.setText(dabPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        }
        textViewStatus.setText(dabPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        textViewLabel1.setText("Damper : ");
        textViewLabel2.setText("Discharge Airflow Temparature : ");
        textViewValue1.setText(dabPoints.get("Damper").toString());
        if( isCelsiusTunerAvailableStatus()) {
            textViewValue2.setText(fahrenheitToCelsiusTwoDecimal(Double.parseDouble(dabPoints.get("Supply Airflow").toString().replaceAll("[^0-9\\.]", ""))) + " \u00B0C");
        } else {
            textViewValue2.setText(dabPoints.get("Supply Airflow").toString());
        }
        if (!Boolean.TRUE.equals(dabPoints.get(AIRFLOW_SENSOR)))  viewDischarge.setVisibility(View.GONE);


        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        linearLayoutZonePoints.addView(viewPointRow1);

        View viewReheat = null;
        if (CCUHsApi.getInstance().readDefaultVal("point and domainName == \"" + DomainName.reheatType + "\" and group == \""+nodeAddress+"\"") > 0) {
            viewReheat = inflater.inflate(R.layout.zone_item_type3, null);
            TextView reheatText = viewReheat.findViewById(R.id.text_label);
            TextView reheatVal = viewReheat.findViewById(R.id.text_value);
            reheatText.setText("Reheat Coil : ");
            reheatVal.setText(dabPoints.get("Reheat Coil").toString());
            linearLayoutZonePoints.addView(viewReheat);
        }

        if (TrueCFMUtil.isTrueCfmEnabled(CCUHsApi.getInstance(), updatedEquip.getId())) {
            View viewAirflowCFM = inflater.inflate(R.layout.zone_item_airflow_cfm, null);
            TextView airFlowCFMValue = viewAirflowCFM.findViewById(R.id.text_airflow_cfm_value);
            airFlowCFMValue.setText(dabPoints.get("Airflow CFM").toString() + " cfm" );
            linearLayoutZonePoints.addView(viewAirflowCFM);
            viewAirflowCFM.setPadding(0, 0, 0, 40);
        }else {
            if (viewReheat != null) {
                viewReheat.setPadding(0, 0, 0, 40);
            } else {
                viewPointRow1.setPadding(0, 0, 0, 40);
            }
        }
    }

    public void loadDualDuctPointsUI(HashMap dualDuctPoints, LayoutInflater inflater,
                                     LinearLayout linearLayoutZonePoints,
                                     String nodeAddress) {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);
        View viewPointRow2 = inflater.inflate(R.layout.zones_item_type1, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);

        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);
        TextView textViewLabel3 = viewPointRow2.findViewById(R.id.text_point1label);
        TextView textViewLabel4 = viewPointRow2.findViewById(R.id.text_point2label);

        TextView textViewValue1 = viewPointRow1.findViewById(R.id.text_point1value);
        TextView textViewValue2 = viewPointRow1.findViewById(R.id.text_point2value);
        TextView textViewValue3 = viewPointRow2.findViewById(R.id.text_point1value);
        TextView textViewValue4 = viewPointRow2.findViewById(R.id.text_point2value);

        if (BuildConfig.BUILD_TYPE.equalsIgnoreCase(CARRIER_PROD)) {
            textViewTitle.setText("VVT-C Dual Duct" + " (" + nodeAddress + ")");
        } else {
            textViewTitle.setText(dualDuctPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        }
        textViewStatus.setText(dualDuctPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        if( isCelsiusTunerAvailableStatus()) {
            if (dualDuctPoints.containsKey("CoolingSupplyAirflow") ) {
                textViewLabel1.setText("Cooling Supply Airflow : ");
                textViewValue1.setText(String.valueOf(fahrenheitToCelsiusTwoDecimal(Double.parseDouble(dualDuctPoints.get("CoolingSupplyAirflow").toString().replaceAll("[^0-9\\.]",""))))+ " \u00B0C");
            } else if (dualDuctPoints.containsKey("HeatingSupplyAirflow")) {
                textViewLabel1.setText("Heating Supply Airflow : ");
                textViewValue1.setText(String.valueOf(fahrenheitToCelsiusTwoDecimal(Double.parseDouble(dualDuctPoints.get("HeatingSupplyAirflow").toString().replaceAll("[^0-9\\.]",""))))+ " \u00B0C");
            }
        } else {
            if (dualDuctPoints.containsKey("CoolingSupplyAirflow") ) {
                textViewLabel1.setText("Cooling Supply Airflow : ");
                textViewValue1.setText(dualDuctPoints.get("CoolingSupplyAirflow").toString());
            } else if (dualDuctPoints.containsKey("HeatingSupplyAirflow")) {
                textViewLabel1.setText("Heating Supply Airflow : ");
                textViewValue1.setText(dualDuctPoints.get("HeatingSupplyAirflow").toString());
            }
        }

        textViewLabel2.setText("Discharge Airflow Temperature : ");
        try {
            if (isCelsiusTunerAvailableStatus()) {
                textViewValue2.setText(String.valueOf(fahrenheitToCelsiusTwoDecimal(Double.parseDouble(dualDuctPoints.get("DischargeAirflow").toString().replaceAll("[^0-9\\.]", "")))) + " \u00B0C");
            } else {
                textViewValue2.setText(dualDuctPoints.get("DischargeAirflow").toString());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (dualDuctPoints.containsKey("CoolingDamper")) {
            textViewLabel3.setText("Cooling Damper : ");
            textViewValue3.setText(dualDuctPoints.get("CoolingDamper").toString());
            if (dualDuctPoints.containsKey("HeatingDamper")) {
                textViewLabel4.setText("Heating Damper : ");
                textViewValue4.setText(dualDuctPoints.get("HeatingDamper").toString());
            }
        } else if (dualDuctPoints.containsKey("HeatingDamper")) {
            textViewLabel3.setText("Heating Damper : ");
            textViewValue3.setText(dualDuctPoints.get("HeatingDamper").toString());
        }

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        linearLayoutZonePoints.addView(viewPointRow1);
        viewPointRow2.setPadding(0, 0, 0, 40);
        linearLayoutZonePoints.addView(viewPointRow2);
    }

    public void loadSSCPUPointsUI(HashMap cpuEquipPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String equipId, boolean isPubNub, String nodeAddress, boolean isLoaded) {

        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type2, null);
        View viewDischarge = inflater.inflate(R.layout.zones_item_discharge, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);

        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);

        Spinner spinnerValue1 = viewPointRow1.findViewById(R.id.spinnerValue1);
        Spinner spinnerValue2 = viewPointRow1.findViewById(R.id.spinnerValue2);
        CCUUiUtil.setSpinnerDropDownColor(spinnerValue1,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spinnerValue2,getContext());
        TextView textAirflowValue = viewDischarge.findViewById(R.id.text_airflowValue);
        if( isCelsiusTunerAvailableStatus()) {
            textAirflowValue.setText(fahrenheitToCelsiusTwoDecimal(Double.parseDouble(cpuEquipPoints.get("Discharge Airflow").toString().replaceAll("[^0-9\\.]", ""))) + " \u00B0C");
        } else {
            textAirflowValue.setText(cpuEquipPoints.get("Discharge Airflow").toString());
        }

        if (!Boolean.TRUE.equals(cpuEquipPoints.get(AIRFLOW_SENSOR)))  viewDischarge.setVisibility(View.GONE);



        int conditionMode = 0;
        int fanMode = 0;
        try {
            conditionMode = (int) ((double) cpuEquipPoints.get("Conditioning Mode"));
            fanMode = (int) ((double) cpuEquipPoints.get("Fan Mode"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayAdapter<CharSequence> conModeAdapter = getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray(R.array.smartstat_conditionmode))));

        if (cpuEquipPoints.containsKey("condEnabled")) {
            if (cpuEquipPoints.get("condEnabled").toString().contains("Cool Only")) {
                conModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_conditionmode_coolonly))));
                if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1;
                }
            } else if (cpuEquipPoints.get("condEnabled").toString().contains("Heat Only")) {
                conModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_conditionmode_heatonly))));
                if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1;
                }
            }
            if (cpuEquipPoints.get("condEnabled").toString().contains("Off")) {
                conModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_conditionmode_off))));
                conditionMode = 0;
            }
        }
        conModeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerValue1.setAdapter(conModeAdapter);

        ArrayAdapter<CharSequence> fanModeAdapter = getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray(R.array.smartstat_fanmode))));
        if (cpuEquipPoints.containsKey("fanEnabled")) {
            if (cpuEquipPoints.get("fanEnabled").toString().contains("No High Fan")) {
                fanModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_fanmode_low))));
                if (fanMode > fanModeAdapter.getCount()) {
                    fanMode = StandaloneFanStage.AUTO.ordinal();//Fallback to Auto if an invalid configuration is set.
                }
            } else if (cpuEquipPoints.get("fanEnabled").toString().contains("No Fan")) {
                fanModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_fanmode_off))));
                fanMode = 0;
            }
        }
        fanModeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerValue2.setAdapter(fanModeAdapter);

        textViewTitle.setText(cpuEquipPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(cpuEquipPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        textViewLabel1.setText("Conditioning Mode : ");
        textViewLabel2.setText("Fan Mode : ");

        //Brute force approach to avoid a crash due to invalid configuration.
        if (fanMode >= fanModeAdapter.getCount()) {
            fanMode = 0;
        }
        if (conditionMode >= conModeAdapter.getCount()) {
            conditionMode = 0;
        }
        spinnerValue1.setSelection(conditionMode, false);
        spinnerValue2.setSelection(fanMode, false);

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);

        double fanHighHumdOption = Double.valueOf(cpuEquipPoints.get("Fan High Humidity").toString());
        double targetHumidity = 0;
        double targetDeHumidity = 0;
        if (fanHighHumdOption > 1.0) {
            View viewPointRow2 = inflater.inflate(R.layout.zones_item_type2, null);

            TextView textViewLabel3 = viewPointRow2.findViewById(R.id.text_point1label);
            Spinner humiditySpinner = viewPointRow2.findViewById(R.id.spinnerValue1);
            TextView textViewLabel4 = viewPointRow2.findViewById(R.id.text_point2label);
            textViewLabel4.setVisibility(View.GONE);
            Spinner spinnerValue4 = viewPointRow2.findViewById(R.id.spinnerValue2);
            spinnerValue4.setVisibility(View.GONE);



            ArrayList<String> arrayHumdityTargetList = new ArrayList<>();
            for (int pos = 1; pos <= 100; pos++)
                arrayHumdityTargetList.add(pos+"%");

            ArrayAdapter<String> humidityTargetAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_zone_item, arrayHumdityTargetList);
            humidityTargetAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            humiditySpinner.setAdapter(humidityTargetAdapter);

            if (fanHighHumdOption == 2.0) {
                textViewLabel3.setText("Target Humidity : ");
                targetHumidity = (double) cpuEquipPoints.get("Target Humidity");
                humiditySpinner.setSelection((int) targetHumidity - 1, false);
            } else {
                textViewLabel3.setText("Target Dehumidity : ");
                targetDeHumidity = (double) cpuEquipPoints.get("Target Dehumidity");
                humiditySpinner.setSelection((int) targetDeHumidity - 1, false);
            }

            linearLayoutZonePoints.addView(viewPointRow2);

            final double targetHumidValue = targetHumidity;
            final double targetDehumidValue = targetDeHumidity;
            humiditySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    try {
                        if (isHPUFromPubNub) {
                            if (fanHighHumdOption == 2.0) {
                                if (targetHumidValue != (position + 1)) {
                                    CcuLog.i("PubNub", "humidityValue:" + targetHumidValue + " position:" + position);
                                    StandaloneScheduler.updateOperationalPoints(equipId, "target and humidity", (double) position + 1);
                                }
                            } else if (fanHighHumdOption == 3.0) {
                                if (targetDehumidValue != (position + 1)) {
                                    CcuLog.i("PubNub", "DehumidityValue:" + targetDehumidValue + " position:" + position);
                                    StandaloneScheduler.updateOperationalPoints(equipId, "target and dehumidifier", (double) position + 1);
                                }
                            }
                        } else {
                            if (fanHighHumdOption == 2.0)
                                StandaloneScheduler.updateOperationalPoints(equipId, "target and humidity", (double) position + 1);
                            else if (fanHighHumdOption == 3.0)
                                StandaloneScheduler.updateOperationalPoints(equipId, "target and dehumidifier", (double) position + 1);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

        }


        linearLayoutZonePoints.addView(viewPointRow1);
        try {
            linearLayoutZonePoints.addView(viewPointRow1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        linearLayoutZonePoints.addView(viewDischarge);
        viewDischarge.setPadding(0, 0, 0, 40);

        int tempConditionMode = conditionMode;
        int tempFanMode = fanMode;
        //isFromPubNub = isPubNub;
        isCPUFromPubNub = isPubNub;
        isCPUloaded = isLoaded;
        String tempEquipId = equipId;
        spinnerValue1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                double enumVal = ConventionalPackageUnitUtil.getEnumforCPUCondMode(position , equipId);
                if (isCPUFromPubNub) {
                    if (tempConditionMode != position) {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "temp and conditioning and mode", enumVal);
                    }
                    //isCPUFromPubNub = false;
                } else {
                    //if(isCPUloaded) {
                    StandaloneScheduler.updateOperationalPoints(tempEquipId, "temp and conditioning and mode", enumVal);
                    //}
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        FanModeCacheStorage fanCacheStorage = FanModeCacheStorage.Companion.getSmartStatFanModeCache();
        spinnerValue2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isCPUFromPubNub) {
                    if (tempFanMode != position) {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                        if ((position != 0) && (position % 3 == 0))
                            fanCacheStorage.saveFanModeInCache(tempEquipId, position);
                        else
                            fanCacheStorage.removeFanModeFromCache(tempEquipId);
                    }
                    isCPUFromPubNub = false;
                } else {
                    StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                    if ((position != 0) && (position % 3 == 0)) //Save only Fan occupied period mode alone, else no need.
                        fanCacheStorage.saveFanModeInCache(tempEquipId, position);
                    else
                        fanCacheStorage.removeFanModeFromCache(tempEquipId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //isCPUloaded = false;

    }

    public void loadSSHPUPointsUI(HashMap hpuEquipPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String equipId, boolean isPubNub, String nodeAddress, boolean isLoaded) {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type2, null);
        View viewDischarge = inflater.inflate(R.layout.zones_item_discharge, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);

        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);

        Spinner conditionSpinner = viewPointRow1.findViewById(R.id.spinnerValue1);
        Spinner fanSpinner = viewPointRow1.findViewById(R.id.spinnerValue2);
        CCUUiUtil.setSpinnerDropDownColor(conditionSpinner,getContext());
        CCUUiUtil.setSpinnerDropDownColor(fanSpinner,getContext());
        TextView textAirflowValue = viewDischarge.findViewById(R.id.text_airflowValue);
        if( isCelsiusTunerAvailableStatus()) {
            textAirflowValue.setText(fahrenheitToCelsiusTwoDecimal(Double.parseDouble(hpuEquipPoints.get("Discharge Airflow").toString().replaceAll("[^0-9\\.]", ""))) + " \u00B0C");
        } else {
            textAirflowValue.setText(hpuEquipPoints.get("Discharge Airflow").toString());
        }
        if (!Boolean.TRUE.equals(hpuEquipPoints.get(AIRFLOW_SENSOR)))  viewDischarge.setVisibility(View.GONE);


        textViewTitle.setText(hpuEquipPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(hpuEquipPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        textViewLabel1.setText("Conditioning Mode : ");
        textViewLabel2.setText("Fan Mode : ");

        pointsOpen.put(hpuEquipPoints.get("StatusTag"), "StatusTag");
        pointsOpen.put(hpuEquipPoints.get("FanModeTag"), "FanModeTag");
        pointsOpen.put(hpuEquipPoints.get("ConditionModeTag"), "ConditionModeTag");

        int conditionMode = 0;
        int fanMode = 0;
        try {
            conditionMode = (int) (double) (hpuEquipPoints.get("Conditioning Mode"));
            fanMode = (int) (double) (hpuEquipPoints.get("Fan Mode"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        ArrayAdapter<CharSequence> conModeAdapter = getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray(R.array.smartstat_conditionmode))));
        if (hpuEquipPoints.containsKey("condEnabled")) {
            if (hpuEquipPoints.get("condEnabled").toString().contains("Cool Only")) {
                conModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_conditionmode_coolonly))));
                if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1;
                }
            } else if (hpuEquipPoints.get("condEnabled").toString().contains("Heat Only")) {
                conModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_conditionmode_heatonly))));
                if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1; //Select the last item.
                }
            } else if (hpuEquipPoints.get("condEnabled").toString().contains("Off")) {
                conModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_conditionmode_off))));
                conditionMode = 0;
            }

        }
        conModeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        conditionSpinner.setAdapter(conModeAdapter);
        ArrayAdapter<CharSequence> fanModeAdapter = getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray(R.array.smartstat_fanmode))));

        if (hpuEquipPoints.containsKey("fanEnabled")) {
            if (hpuEquipPoints.get("fanEnabled").toString().contains("No High Fan")) {
                fanModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_fanmode_low))));
                if (fanMode > fanModeAdapter.getCount()) {
                    fanMode = StandaloneFanStage.AUTO.ordinal();//Fallback to Auto if an invalid configuration is set.
                }
            } else if (hpuEquipPoints.get("fanEnabled").toString().contains("No Fan")) {
                fanModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_fanmode_off))));
                fanMode = 0;
            }
        }
        fanModeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        fanSpinner.setAdapter(fanModeAdapter);

        //Brute force approach to avoid a crash due to invalid configuration.
        if (fanMode >= fanModeAdapter.getCount()) {
            fanMode = 0;
        }
        if (conditionMode >= conModeAdapter.getCount()) {
            conditionMode = 0;
        }

        conditionSpinner.setSelection(conditionMode, false);
        fanSpinner.setSelection(fanMode, false);

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);

        double fanHighHumdOption = (double) hpuEquipPoints.get("Fan High Humidity");
        double targetHumidity = 0;
        double targetDeHumidity = 0;
        if (fanHighHumdOption > 1.0) {
            View viewPointRow2 = inflater.inflate(R.layout.zones_item_type2, null);

            TextView textViewLabel3 = viewPointRow2.findViewById(R.id.text_point1label);
            Spinner humiditySpinner = viewPointRow2.findViewById(R.id.spinnerValue1);
            TextView textViewLabel4 = viewPointRow2.findViewById(R.id.text_point2label);
            textViewLabel4.setVisibility(View.GONE);
            Spinner spinnerValue4 = viewPointRow2.findViewById(R.id.spinnerValue2);
            spinnerValue4.setVisibility(View.GONE);


            ArrayList<String> arrayHumdityTargetList = new ArrayList<>();
            for (int pos = 1; pos <= 100; pos++)
                arrayHumdityTargetList.add(pos+"%");

            ArrayAdapter<String> humidityTargetAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_zone_item, arrayHumdityTargetList);
            humidityTargetAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            humiditySpinner.setAdapter(humidityTargetAdapter);

            if (fanHighHumdOption > 1.0) {
                if (fanHighHumdOption == 2.0) {
                    textViewLabel3.setText("Target Humidity : ");
                    targetHumidity = (double) hpuEquipPoints.get("Target Humidity");
                    CcuLog.i("targetHumidity", "insideZoneFragment" + targetHumidity);
                    humiditySpinner.setSelection((int) targetHumidity - 1, false);
                } else {
                    textViewLabel3.setText("Target Dehumidity : ");
                    targetDeHumidity = (double) hpuEquipPoints.get("Target Dehumidity");
                    humiditySpinner.setSelection((int) targetDeHumidity - 1, false);
                }
            }

            linearLayoutZonePoints.addView(viewPointRow2);
            final double targetHumidValue = targetHumidity;
            final double targetDehumidValue = targetDeHumidity;
            humiditySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    try {
                        if (isHPUFromPubNub) {
                            if (fanHighHumdOption == 2.0) {
                                if (targetHumidValue != (position + 1)) {
                                    CcuLog.i("PubNub", "humidityValue:" + targetHumidValue + " position:" + position + 1);
                                    StandaloneScheduler.updateOperationalPoints(equipId, "target and humidity", (double) position + 1);
                                }
                            } else if (fanHighHumdOption == 3.0) {
                                if (targetDehumidValue != (position + 1)) {
                                    CcuLog.i("PubNub", "DehumidityValue:" + targetDehumidValue + " position:" + position + 1);
                                    StandaloneScheduler.updateOperationalPoints(equipId, "target and dehumidifier", (double) position + 1);
                                }
                            }
                        } else {
                            if (fanHighHumdOption == 2.0)
                                StandaloneScheduler.updateOperationalPoints(equipId, "target and humidity", (double) position + 1);
                            else if (fanHighHumdOption == 3.0)
                                StandaloneScheduler.updateOperationalPoints(equipId, "target and dehumidifier", (double) position + 1);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }


        linearLayoutZonePoints.addView(viewPointRow1);
        try {
            if (viewPointRow1.getParent() != null) {
                ((ViewGroup) viewPointRow1.getParent()).removeView(viewPointRow1);
            }
            linearLayoutZonePoints.addView(viewPointRow1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        linearLayoutZonePoints.addView(viewDischarge);
        viewDischarge.setPadding(0, 0, 0, 40);

        int tempConditionMode = conditionMode;
        int tempfanMode = fanMode;
        //isFromPubNub = isPubNub;
        isHPUFromPubNub = isPubNub;
        isHPUloaded = isLoaded;
        String tempEquipId = equipId;
        conditionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    if (isHPUFromPubNub) {
                        if (tempConditionMode != position) {
                            CcuLog.i("PubNub", "conditionMode:" + tempConditionMode + " position:" + position);
                            StandaloneScheduler.updateOperationalPoints(tempEquipId, "temp and conditioning and mode", position);
                        }
                        //isFromPubNub = false;
                    } else {
                        //if(isHPUloaded) {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "temp and conditioning and mode", position);
                        //}
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        FanModeCacheStorage fanCacheStorage = FanModeCacheStorage.Companion.getSmartStatFanModeCache();
        fanSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    if (isHPUFromPubNub) {
                        if (tempfanMode != position) {
                            CcuLog.i("PubNub", "fanMode:" + tempfanMode + " position:" + position);
                            StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                            if ((position != 0) && (position % 3 == 0))
                                fanCacheStorage.saveFanModeInCache(tempEquipId, position);
                            else
                                fanCacheStorage.removeFanModeFromCache(tempEquipId);
                        }
                        isHPUFromPubNub = false;
                    } else {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                        if ((position != 0) && (position % 3 == 0))
                            fanCacheStorage.saveFanModeInCache(tempEquipId, position);
                        else
                            fanCacheStorage.removeFanModeFromCache(tempEquipId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //isHPUloaded = false;

    }

    public void loadSS2PFCUPointsUI(HashMap p2FCUPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String equipId, boolean isPubNub, String nodeAddress) {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type2, null);
        View viewDischarge = inflater.inflate(R.layout.zones_item_discharge, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);

        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);

        TextView textAirflowValue = viewDischarge.findViewById(R.id.text_airflowValue);


        if( isCelsiusTunerAvailableStatus()) {
            textAirflowValue.setText(String.valueOf(fahrenheitToCelsiusTwoDecimal(Double.parseDouble(p2FCUPoints.get("Discharge Airflow").toString().replaceAll("[^0-9\\.]",""))))+ " \u00B0C");
        } else {
            textAirflowValue.setText(p2FCUPoints.get("Discharge Airflow").toString());
        }

        if (!Boolean.TRUE.equals(p2FCUPoints.get(AIRFLOW_SENSOR)))  viewDischarge.setVisibility(View.GONE);

        Spinner spinnerValue1 = viewPointRow1.findViewById(R.id.spinnerValue1);
        Spinner spinnerValue2 = viewPointRow1.findViewById(R.id.spinnerValue2);
        CCUUiUtil.setSpinnerDropDownColor(spinnerValue1,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spinnerValue2,getContext());

        int conditionMode = 0;
        int fanMode = 0;
        try {
            conditionMode = (int) ((double) p2FCUPoints.get("Conditioning Mode"));
            fanMode = (int) ((double) p2FCUPoints.get("Fan Mode"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayAdapter<CharSequence> conModeAdapter = getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray(R.array.smartstat_conditionmode))));

        if (p2FCUPoints.containsKey("condEnabled")) {
            if (p2FCUPoints.get("condEnabled").toString().contains("Cool Only")) {
                conModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_conditionmode_coolonly))));
                if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1; //Select the last item.
                }
            } else if (p2FCUPoints.get("condEnabled").toString().contains("Heat Only")) {
                conModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_conditionmode_heatonly))));
                if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1; //Select the last item
                }
            }
            if (p2FCUPoints.get("condEnabled").toString().contains("Off")) {
                conModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_conditionmode_off))));
                conditionMode = 0;
            }

        }
        conModeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerValue1.setAdapter(conModeAdapter);
        ArrayAdapter<CharSequence> fanModeAdapter = getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray(R.array.smartstat_2pfcu_fanmode))));
        if (p2FCUPoints.containsKey("fanEnabled")) {
            if (p2FCUPoints.get("fanEnabled").toString().contains("No High Fan")) {
                fanModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_2pfcu_fanmode_medium))));
                if (fanMode > fanModeAdapter.getCount()) {
                    fanMode = StandaloneFanStage.AUTO.ordinal();//Fallback to Auto if an invalid configuration is set.
                }
            } else if (p2FCUPoints.get("fanEnabled").toString().contains("No Medium High Fan")) {
                fanModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_2pfcu_fanmode_low))));
                if (fanMode > fanModeAdapter.getCount()) {
                    fanMode = StandaloneFanStage.AUTO.ordinal();//Fallback to Auto if an invalid configuration is set.
                }
            } else if (p2FCUPoints.get("fanEnabled").toString().contains("No Fan")) {
                fanModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_2pfcu_fanmode_off))));
                fanMode = 0;
            }
        }
        fanModeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerValue2.setAdapter(fanModeAdapter);

        textViewTitle.setText(p2FCUPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(p2FCUPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        textViewLabel1.setText("Conditioning Mode : ");
        textViewLabel2.setText("Fan Mode : ");

        //Brute force approach to avoid a crash due to invalid configuration.
        if (fanMode >= fanModeAdapter.getCount()) {
            fanMode = 0;
        }
        if (conditionMode >= conModeAdapter.getCount()) {
            conditionMode = 0;
        }

        spinnerValue1.setSelection(conditionMode, false);
        spinnerValue2.setSelection(fanMode, false);

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        linearLayoutZonePoints.addView(viewPointRow1);

        try {
            if (viewPointRow1.getParent() != null) {
                ((ViewGroup) viewPointRow1.getParent()).removeView(viewPointRow1);
            }
            linearLayoutZonePoints.addView(viewPointRow1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        linearLayoutZonePoints.addView(viewDischarge);
        viewDischarge.setPadding(0, 0, 0, 40);
        int tempConditionMode = conditionMode;
        int tempfanMode = fanMode;
        isFromPubNub = isPubNub;
        String tempEquipId = equipId;
        spinnerValue1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isFromPubNub) {
                    if (tempConditionMode != position) {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "temp and conditioning and mode", position);
                    }
                    //isFromPubNub = false;
                } else {
                    StandaloneScheduler.updateOperationalPoints(tempEquipId, "temp and conditioning and mode", position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        FanModeCacheStorage fanCacheStorage = FanModeCacheStorage.Companion.getSmartStatFanModeCache();
        spinnerValue2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isFromPubNub) {
                    if (tempfanMode != position) {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                        if ((position != 0) && (position % 3 == 0))
                            fanCacheStorage.saveFanModeInCache(tempEquipId, position);
                        else
                            fanCacheStorage.removeFanModeFromCache(tempEquipId);
                    }
                    isFromPubNub = false;
                } else {
                    StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                    if ((position != 0) && (position % 3 == 0))
                        fanCacheStorage.saveFanModeInCache(tempEquipId, position);
                    else
                        fanCacheStorage.removeFanModeFromCache(tempEquipId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void loadSS4PFCUPointsUI(HashMap p4FCUPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String equipId, boolean isPubNub, String nodeAddress) {
        //boolean isFromPubNub = isPubNub;
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type2, null);
        View viewDischarge = inflater.inflate(R.layout.zones_item_discharge, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);
        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);

        Spinner spinnerValue1 = viewPointRow1.findViewById(R.id.spinnerValue1);
        Spinner spinnerValue2 = viewPointRow1.findViewById(R.id.spinnerValue2);
        CCUUiUtil.setSpinnerDropDownColor(spinnerValue1,getContext());
        CCUUiUtil.setSpinnerDropDownColor(spinnerValue2,getContext());

        TextView textAirflowValue = viewDischarge.findViewById(R.id.text_airflowValue);

        if( isCelsiusTunerAvailableStatus()) {
            textAirflowValue.setText(String.valueOf(fahrenheitToCelsiusTwoDecimal(Double.parseDouble(p4FCUPoints.get("Discharge Airflow").toString().replaceAll("[^0-9\\.]",""))))+ " \u00B0C");
        } else {
            textAirflowValue.setText(p4FCUPoints.get("Discharge Airflow").toString());
        }
        if (!Boolean.TRUE.equals(p4FCUPoints.get(AIRFLOW_SENSOR)))  viewDischarge.setVisibility(View.GONE);


        int conditionMode = 0;
        int fanMode = 0;
        try {
            conditionMode = (int) ((double) p4FCUPoints.get("Conditioning Mode"));
            fanMode = (int) ((double) p4FCUPoints.get("Fan Mode"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayAdapter<CharSequence> conModeAdapter = getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray(R.array.smartstat_conditionmode))));
        if (p4FCUPoints.containsKey("condEnabled")) {
            if (p4FCUPoints.get("condEnabled").toString().contains("Cool Only")) {
                conModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_conditionmode_coolonly))));
                if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1; //Select the last item.
                }
            } else if (p4FCUPoints.get("condEnabled").toString().contains("Heat Only")) {
                conModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_conditionmode_heatonly))));
                if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1; //Select the last item.
                }
            }
            if (p4FCUPoints.get("condEnabled").toString().contains("Off")) {
                conModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_conditionmode_off))));
                conditionMode = 0;
            }

        }
        conModeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerValue1.setAdapter(conModeAdapter);

        ArrayAdapter<CharSequence> fanModeAdapter = getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray(R.array.smartstat_2pfcu_fanmode))));

        if (p4FCUPoints.containsKey("fanEnabled")) {
            if (p4FCUPoints.get("fanEnabled").toString().contains("No High Fan")) {
                fanModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_2pfcu_fanmode_medium))));
                if (fanMode > fanModeAdapter.getCount()) {
                    fanMode = StandaloneFanStage.AUTO.ordinal();//Fallback to Auto if an invalid configuration is set.
                }
            } else if (p4FCUPoints.get("fanEnabled").toString().contains("No Medium High Fan")) {
                fanModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_2pfcu_fanmode_low))));
                if (fanMode > fanModeAdapter.getCount()) {
                    fanMode = StandaloneFanStage.AUTO.ordinal();//Fallback to Auto if an invalid configuration is set.
                }
            } else if (p4FCUPoints.get("fanEnabled").toString().contains("No Fan")) {
                fanModeAdapter =getAdapterValue(new ArrayList(Arrays.asList(getResources().getStringArray( R.array.smartstat_2pfcu_fanmode_off))));
                fanMode = 0;
            }
        }
        fanModeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerValue2.setAdapter(fanModeAdapter);

        textViewTitle.setText(p4FCUPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(p4FCUPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        textViewLabel1.setText("Conditioning Mode : ");
        textViewLabel2.setText("Fan Mode : ");

        //Brute force approach to avoid a crash due to invalid configuration.
        if (fanMode >= fanModeAdapter.getCount()) {
            fanMode = 0;
        }
        if (conditionMode >= conModeAdapter.getCount()) {
            conditionMode = 0;
        }

        spinnerValue1.setSelection(conditionMode, false);
        spinnerValue2.setSelection(fanMode, false);

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        linearLayoutZonePoints.addView(viewPointRow1);

        try {
            if (viewPointRow1.getParent() != null) {
                ((ViewGroup) viewPointRow1.getParent()).removeView(viewPointRow1);
            }
            linearLayoutZonePoints.addView(viewPointRow1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        linearLayoutZonePoints.addView(viewDischarge);
        viewDischarge.setPadding(0, 0, 0, 40);
        int tempConditionMode = conditionMode;
        int tempFanMode = fanMode;
        isFromPubNub = isPubNub;
        FanModeCacheStorage fanCacheStorage = FanModeCacheStorage.Companion.getSmartStatFanModeCache();
        spinnerValue1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                double enumVal = ConventionalPackageUnitUtil.getEnumforFourPipeCondMode(position,equipId);
                if (isFromPubNub) {
                    if (tempConditionMode != position) {
                        StandaloneScheduler.updateOperationalPoints(equipId, "temp and conditioning and mode", enumVal);
                    }
                    isFromPubNub = false;
                } else {
                    StandaloneScheduler.updateOperationalPoints(equipId, "temp and conditioning and mode", enumVal);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerValue2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isFromPubNub) {
                    if (tempFanMode != position) {
                        StandaloneScheduler.updateOperationalPoints(equipId, "fan and operation and mode", position);
                        if ((position != 0) && (position % 3 == 0))
                            fanCacheStorage.saveFanModeInCache(equipId, position);
                        else
                            fanCacheStorage.removeFanModeFromCache(equipId);
                    }
                    isFromPubNub = false;
                } else {
                    StandaloneScheduler.updateOperationalPoints(equipId, "fan and operation and mode", position);
                    if ((position != 0) && (position % 3 == 0))
                        fanCacheStorage.saveFanModeInCache(equipId, position);
                    else
                        fanCacheStorage.removeFanModeFromCache(equipId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    public void loadNoDevicesPairedUI(LayoutInflater inflater, LinearLayout linearLayoutZonePoints) {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        textViewTitle.setVisibility(View.GONE);
        TextView statusTitle = viewStatus.findViewById(R.id.inner_status_title);
        statusTitle.setVisibility(View.GONE);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        textViewModule.setVisibility(View.GONE);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);
        textViewUpdatedTime.setVisibility(View.GONE);
        TextView textViewUpdatedText = viewStatus.findViewById(R.id.last_updated);
        textViewUpdatedText.setVisibility(View.GONE);
        textViewStatus.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        textViewStatus.setText(Html.fromHtml(getString(R.string.no_device_currently_paired)));
        viewStatus.setPadding(0, 0, 0, 40);
        try {
            linearLayoutZonePoints.addView(viewStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPointVal(String coolid, double coolval, String heatid, double heatval, String avgid, double avgval) {

        ExecutorTask.executeBackground( () -> {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            Point coolpoint = new Point.Builder().setHashMap(hayStack.readMapById(coolid)).build();
            Point heatpoint = new Point.Builder().setHashMap(hayStack.readMapById(heatid)).build();
            Point avgpoint = new Point.Builder().setHashMap(hayStack.readMapById(avgid)).build();

            if (coolpoint.getMarkers().contains("writable")) {
                CcuLog.d(L.TAG_CCU_UI, "Set Writbale Val " + coolpoint.getDisplayName() + ": " + coolid + "," + heatpoint.getDisplayName() + "," + heatval + "," + avgpoint.getDisplayName());
                SystemScheduleUtil.handleManualDesiredTempUpdate(coolpoint, heatpoint, avgpoint, coolval, heatval
                        , avgval, "CCU");

            }

            if (coolpoint.getMarkers().contains("his") && (coolval != 0)) {
                CcuLog.d(L.TAG_CCU_UI, "Set His Val " + coolid + ": " + coolval);
                hayStack.writeHisValById(coolid, coolval);
            }
            if (heatpoint.getMarkers().contains("his") && (heatval != 0)) {
                CcuLog.d(L.TAG_CCU_UI, "Set His Val " + heatid + ": " + heatval);
                hayStack.writeHisValById(heatid, heatval);
            }
            if (avgpoint.getMarkers().contains("his") && (ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.OCCUPIED)) {
                CcuLog.d(L.TAG_CCU_UI, "Set His Val " + avgid + ": " + avgval);
                hayStack.writeHisValById(avgid, avgval);
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        // loadGrid(parentRootView);
        if (weather_data.getVisibility() == View.VISIBLE) {
            CcuLog.i("weather", "update");
            UpdateWeatherData();
        }
        weatherInIt(15*60000);
        CcuLog.i("UI_PROFILING",""+getUserVisibleHint());
        if (isZoneViewReady) {
            setListeners();
        } else {
            CcuLog.i("UI_PROFILING","ZoneFragmentNew.onResume Temp Listening not set");
        }
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.onResume Done");
        HttpServer.Companion.setCurrentTempInterface(this);
    }

    private void setListeners() {
        if (getUserVisibleHint()) {
            ScheduleManager.getInstance().setZoneDataInterface(this);
            Pulse.setCurrentTempInterface(this);
            ScheduleManager.getInstance().setScheduleDataInterface(this);
            ScheduleManager.getInstance().setZoneDataInterface(this);
            StandaloneScheduler.setZoneDataInterface(this);
            HyperStatMsgReceiver.setCurrentTempInterface(this);
            MyStatMsgReceiverKt.setCurrentTempInterface(this);
            HyperSplitMsgReceiver.setCurrentTempInterface(this);
            HyperStatUserIntentHandler.Companion.setZoneDataInterface(this);
            MyStatUserIntentHandler.Companion.setZoneDataInterface(this);
            HyperStatSplitUserIntentHandler.Companion.setZoneDataInterface(this);
            UpdatePointHandler.setZoneDataInterface(this);
            UpdateEntityHandler.setZoneDataInterface(this);
        } else {
            CcuLog.i("UI_PROFILING","ZoneFragmentNew.setListeners Skipped : UserVisibleHint not set");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.onPause");
        ScheduleManager.getInstance().setZoneDataInterface(null);
        Pulse.setCurrentTempInterface(null);
        ScheduleManager.getInstance().setScheduleDataInterface(null);
        ScheduleManager.getInstance().setZoneDataInterface(null);
        StandaloneScheduler.setZoneDataInterface(null);
        HyperStatUserIntentHandler.Companion.setZoneDataInterface(this);
        MyStatUserIntentHandler.Companion.setZoneDataInterface(this);
        HyperStatSplitUserIntentHandler.Companion.setZoneDataInterface(null);
        HyperStatMsgReceiver.setCurrentTempInterface(null);
        MyStatMsgReceiverKt.setCurrentTempInterface(null);
        HyperSplitMsgReceiver.setCurrentTempInterface(null);
        UpdatePointHandler.setZoneDataInterface(null);
        UpdateEntityHandler.setZoneDataInterface(null);
        HttpServer.Companion.setModbusDataInterface(null);
        HttpServer.Companion.setCurrentTempInterface(null);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        CcuLog.i("UI_PROFILING","isVisibleToUser "+isVisibleToUser+" isZoneViewReady "+isZoneViewReady);
        if (isVisibleToUser && isZoneViewReady) {
            setListeners();
        } else {

            ScheduleManager.getInstance().setZoneDataInterface(null);
            Pulse.setCurrentTempInterface(null);
            ScheduleManager.getInstance().setScheduleDataInterface(null);
            ScheduleManager.getInstance().setZoneDataInterface(null);
            StandaloneScheduler.setZoneDataInterface(null);
            HyperStatUserIntentHandler.Companion.setZoneDataInterface(null);
            MyStatUserIntentHandler.Companion.setZoneDataInterface(this);
            HyperStatSplitUserIntentHandler.Companion.setZoneDataInterface(null);
            HyperStatMsgReceiver.setCurrentTempInterface(null);
            MyStatMsgReceiverKt.setCurrentTempInterface(null);
            HyperSplitMsgReceiver.setCurrentTempInterface(null);
            UpdatePointHandler.setZoneDataInterface(null);
            UpdateEntityHandler.setZoneDataInterface(null);
        }
    }

    @Override
    public void updateCustomScheduleView() {
        CcuLog.d("CCU_UI", "called updateCustomScheduleView");
        getActivity().runOnUiThread(() -> {
            if (zoneDataForUi != null && zoneDataForUi.isZoneExpanded()) {
                hideCustomScheduleView(zoneDataForUi);
                showCustomScheduleView(zoneDataForUi);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (zoneDataForUi != null) {
                        if(CustomScheduleUtilKt.isZoneUsingCustomSchedulesOrEvents(zoneDataForUi.getEquip().getRoomRef())){
                            hideCustomScheduleView(zoneDataForUi);
                            showCustomScheduleView(zoneDataForUi);
                            CcuLog.d("CCU_UI", " Show Custom Schedule View ");
                        } else {
                            hideCustomScheduleView(zoneDataForUi);
                            CcuLog.d("CCU_UI", " Hide Custom Schedule View ");
                        }
                    }
                }, 45_000);
            }
        });

    }

    @Override
    public void removeCustomScheduleView() {
        getActivity().runOnUiThread(() -> {
            CcuLog.d("CCU_UI", " Hide Custom Schedule View ");
            if (zoneDataForUi != null) {
                hideCustomScheduleView(zoneDataForUi);
            }
        });

    }

    class FloorComparator implements Comparator<Floor> {
        @Override
        public int compare(Floor a, Floor b) {
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        }
    }

    public void ScaleControlToNormal(int height, int width, NonTempControl imageView) {
        int endHeight = (int) (height / 1.35);
        int endWidth = (int) (width / 1.35);
        imageView.getLayoutParams().height = endHeight;
        imageView.getLayoutParams().width = endWidth;
    }

    public void ScaleControlToExpand(int height, int width, NonTempControl imageView) {
        int endHeight = (int) (height * 1.35);
        int endWidth = (int) (width * 1.35);
        imageView.getLayoutParams().height = endHeight;
        imageView.getLayoutParams().width = endWidth;
    }

    public void showWeather() {
        weather_data.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(-weather_data.getWidth(), 0, 0, 0);
        animate.setDuration(400);
        animate.setFillAfter(true);
        weather_data.startAnimation(animate);
    }

    public void hideWeather() {
        TranslateAnimation animate = new TranslateAnimation(0, (float) -weather_data.getWidth() + 5, 0, 0);
        animate.setDuration(400);
        animate.setFillAfter(true);
        weather_data.startAnimation(animate);
        tableLayout.startAnimation(in);
        weather_data.setVisibility(View.GONE);
    }

    private void setScheduleType(String id, ScheduleType schedule, ArrayList<HashMap> zoneMap) {
            CcuLog.d("CCU_UI", " Set Schedule type " + schedule.ordinal());
            CCUHsApi.getInstance().writeHisValById(id, (double) schedule.ordinal());
            Point p = new Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(id)).build();
            if (zoneMap.size() > 1) {
                for (int i = 0; i < zoneMap.size(); i++) {
                    Equip equip = new Equip.Builder().setHashMap(zoneMap.get(i)).build();
                    String scheduleTypeId = getScheduleTypeId(equip.getId());
                    CCUHsApi.getInstance().writeDefaultValById(scheduleTypeId, (double) schedule.ordinal());
                    CCUHsApi.getInstance().writeHisValById(scheduleTypeId, (double) schedule.ordinal());
                }
            } else
                CCUHsApi.getInstance().writeDefaultValById(id, (double) schedule.ordinal());
            SystemScheduleUtil.handleScheduleTypeUpdate(p);
    }


    private boolean checkContainment(Schedule zoneSchedule, ContainmentDialogClickListener listener) {
        Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);
        ArrayList<Interval> intervalSpills = new ArrayList<>();
        ArrayList<Interval> systemIntervals = systemSchedule.getMergedIntervals();
        ArrayList<Interval> zoneIntervals = zoneSchedule.getScheduledIntervals();
        if(zoneIntervals.isEmpty()) return true;
        separateOvernightSchedules(zoneIntervals);
        //sorting the zoneInterval
        zoneIntervals.sort(Comparator.comparingLong(BaseInterval::getStartMillis));
        updateSystemScheduleForSundayOvernight(zoneIntervals, systemIntervals, systemSchedule);
        generateIntervalSpills(zoneIntervals, systemIntervals, intervalSpills);
        if (intervalSpills.size() > 0) {
            generateOccupancyContainmentBreachedDialog(intervalSpills, zoneSchedule, listener);
            return false;
        } else {
            return true;
        }
    }

    String getScheduleTypeId(String equipId) {
        if(CCUUiUtil.isDomainEquip(equipId, "equip")) {
            return CCUHsApi.getInstance().readId("point and domainName == \"" + DomainName.scheduleType + "\" and equipRef == \"" + equipId + "\"");
        } else {
            return CCUHsApi.getInstance().readId("point and scheduleType and equipRef == \"" + equipId + "\"");
        }
    }

    private void viewMonitoringZone(LayoutInflater inflater, View rootView, ArrayList<HashMap> zoneMap, String zoneTitle, int gridPosition, LinearLayout[] tablerowLayout, boolean isZoneAlive) {

        CcuLog.i("ProfileTypes", "Points:" + zoneMap.toString());
        Equip p = new Equip.Builder().setHashMap(zoneMap.get(0)).build();
        CcuLog.i("ProfileTypes", "p:" + p.toString());
        double offsetAvg = 0;
        double currentAverageTemp = 0;
        double curTemp= 0;
        ArrayList<Short> equipNodes = new ArrayList<>();
        for (int i = 0; i < zoneMap.size(); i++) {
            MonitoringEquip monitoringEquip = (MonitoringEquip) Domain.INSTANCE.getDomainEquip(p.getId());
            double avgTemp = monitoringEquip.getCurrentTemp().readHisVal();
            currentAverageTemp = (currentAverageTemp + avgTemp);
            equipNodes.add( (short) monitoringEquip.getNodeAddress());
        }

        curTemp = currentAverageTemp ;

        CcuLog.i("EachzoneData", " currentAvg:" + currentAverageTemp);
        final String[] equipId = {p.getId()};
        int i = gridPosition;
        View arcView = inflater.inflate(R.layout.zones_item, (ViewGroup) rootView, false);
        View zoneDetails = inflater.inflate(R.layout.zones_item_details, null);
        View status_view = arcView.findViewById(R.id.status_view);
        TextView textViewModule = arcView.findViewById(R.id.module_status);
        HeartBeatUtil.zoneStatus(textViewModule, isZoneAlive);
        zoneStatus.put(zoneTitle, textViewModule);
        LinearLayout linearLayoutZonePoints = zoneDetails.findViewById(R.id.lt_profilepoints);
        LinearLayout linearLayoutschedulePoints = zoneDetails.findViewById(R.id.lt_schedule);
        LinearLayout linearLayoutstatusPoints = zoneDetails.findViewById(R.id.lt_status);
        linearLayoutstatusPoints.setVisibility(View.GONE);
        linearLayoutschedulePoints.setVisibility(View.GONE);
        TextView vacationStatusTV = zoneDetails.findViewById(R.id.vacation_status);
        TextView vacationText = zoneDetails.findViewById(R.id.vacationText);
        ImageView vacationEditButton = zoneDetails.findViewById(R.id.vacation_edit_button);
        GridItem gridItemObj = new GridItem();
        gridItemObj.setGridID(i);
        gridItemObj.setGridItem("monitoring");
        gridItemObj.setNodeAddress(Short.valueOf(p.getGroup()));
        gridItemObj.setZoneEquips(zoneMap);
        gridItemObj.setZoneNodes(equipNodes);
        arcView.setClickable(true);
        arcView.setTag(gridItemObj);
        arcView.setId(i);
        SeekArc seekArc = arcView.findViewById(R.id.seekArc);
        seekArc.setTag(gridItemObj);
        zoneDetails.setTag(gridItemObj);
        seekArc.scaletoNormal(260, 210);
        TextView textEquipment = arcView.findViewById(R.id.textEquipment);
        textEquipment.setText(zoneTitle);
        seekArc.setMonitoring(true);
        seekArc.setMonitoringData(false, (float)(curTemp));
        seekArc.setDetailedView(false);
        vacationStatusTV.setVisibility(View.GONE);
        vacationText.setVisibility(View.GONE);
        vacationEditButton.setVisibility(View.GONE);
        LinearLayout.LayoutParams rowLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        arcView.setPadding(48, 64, 0, 0);
        try {
            tablerowLayout[rowcount].addView(arcView, rowLayoutParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (((i + 1) % columnCount == 0) && (i != 0)) {
            try {
                if (tablerowLayout[rowcount].getParent() != null) {
                    ((ViewGroup) tablerowLayout[rowcount].getParent()).removeView(tablerowLayout[rowcount]);
                }
                tableLayout.addView(tablerowLayout[rowcount++]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (rowcount < numRows)
                tablerowLayout[rowcount] = new LinearLayout(tableLayout.getContext());
        }
        if (rowcount < numRows) {
            try {
                if (tablerowLayout[rowcount].getParent() != null) {
                    ((ViewGroup) tablerowLayout[rowcount].getParent()).removeView(tablerowLayout[rowcount]);
                }
                tableLayout.addView(tablerowLayout[rowcount]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        seekArcArrayList.add(seekArc);
        ComposeView embededComposeView = zoneDetails.findViewById(R.id.compose_profile_points);
        seekArc.setOnClickListener(v -> {
            GridItem gridItemNew = (GridItem) v.getTag();
            boolean isExpanded = false;
            int clickedItemRow = 0;
            int clickposition = gridItemNew.getGridID();
            if (clickedView != -1) {
                if (clickposition != clickedView) {
                    int newRowCount = 0;
                    int tableRowCount = tableLayout.getChildCount();
                    if (tableLayout.getChildCount() > 1) {
                        boolean viewFound = false;
                        for (int row = 0; row < tableRowCount; row++) {
                            View rowView = tableLayout.getChildAt(row);
                            LinearLayout tableRow = (LinearLayout) rowView;
                            int cellCount = tableRow.getChildCount();
                            for (int j = 0; j < cellCount; j++) {
                                RelativeLayout gridItem = (RelativeLayout) tableRow.getChildAt(j);
                                GridItem viewTag = (GridItem) gridItem.getTag();
                                if (viewTag.getGridID() == clickedView ) {
                                    TextView textViewzone = (TextView) gridItem.findViewById(R.id.textEquipment);
                                    textViewzone.setTextAppearance(getActivity(), R.style.label_black);
                                    textViewzone.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                    View statusView = gridItem.findViewById(R.id.status_view);
                                    statusView.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                    tableLayout.removeViewAt(row + 1);
                                    gridItem.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                    if (viewTag.getGridItem().equals("monitoring") || viewTag.getGridItem().equals("Temp")) {
                                        SeekArc seekArcExpanded = (SeekArc) gridItem.findViewById(R.id.seekArc);
                                        seekArcExpanded.setDetailedView(false);
                                        seekArcExpanded.setBackgroundColor(getResources().getColor(R.color.white));
                                        seekArcExpanded.scaletoNormal(260, 210);
                                    } else {
                                        NonTempControl nonTempControl = gridItem.findViewById(R.id.rl_nontemp);
                                        ScaleControlToNormal(260, 210, nonTempControl);
                                        nonTempControl.setExpand(false);
                                        //ScaleImageToNormal(250,210,imageViewExpanded);
                                        nonTempControl.setBackgroundColor(getResources().getColor(R.color.white));
                                    }

                                    isExpanded = false;
                                    imageOn = false;
                                    viewFound = true;
                                    break;
                                }
                            }
                            if (viewFound) {
                                clickedItemRow = row;
                                break;
                            }
                        }

                        zoneOpen = true;
                        zoneNonTempOpen = false;
                        seekArcOpen = seekArc;
                        zonePointsOpen = zoneDetails;
                        equipOpen = p;
                        openZoneMap = zoneMap;
                        clickedView = gridItemNew.getGridID();
                        v.setBackgroundColor(getActivity().getResources().getColor(R.color.zoneselection_gray));
                        int index = clickedView / columnCount + 1;
                        seekArc.setDetailedView(true);
                        //seekArc.setOnTemperatureChangeListener(SeekArcMemShare.onTemperatureChangeListener);
                        seekArc.scaletoNormalBig(260, 210);
                        imageOn = true;
                        selectedView = seekArc.getId();
                        status_view.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        try {
                            textEquipment.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                            zoneDetails.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                            tableLayout.addView(zoneDetails, index);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        isExpanded = true;
                    }
                } else if (clickposition == clickedView) {
                    status_view.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                    v.setBackgroundColor(getResources().getColor(R.color.white));
                    textEquipment.setTextAppearance(getActivity(), R.style.label_black);
                    textEquipment.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                    tableLayout.removeView(zoneDetails);
                    imageOn = false;
                    seekArc.setDetailedView(false);
                    seekArc.scaletoNormal(260, 210);
                    showWeather();
                    clickedView = -1;
                    isExpanded = false;
                }
            } else {
                zoneOpen = false;
                zoneNonTempOpen = true;
                seekArcOpen = seekArc;
                zonePointsOpen = zoneDetails;
                equipOpen = p;
                openZoneMap = zoneMap;
                clickedView = gridItemNew.getGridID();
                seekArc.setClickable(true);
                v.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                int index = clickedView / columnCount + 1;
                seekArc.setDetailedView(true);
                //seekArc.setOnTemperatureChangeListener(SeekArcMemShare.onTemperatureChangeListener);
                seekArc.scaletoNormalBig(260, 210);
                hideWeather();
                imageOn = true;
                selectedView = seekArc.getId();
                status_view.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                try {
                    textEquipment.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                    zoneDetails.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                    tableLayout.addView(zoneDetails, index);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            zoneOpen = false;
            v.setContentDescription(zoneTitle);
            linearLayoutZonePoints.removeAllViews();
            for (int k = 0; k < openZoneMap.size(); k++) {
                Equip updatedEquip = new Equip.Builder().setHashMap(openZoneMap.get(k)).build();
                if (updatedEquip.getProfile().contains("MONITORING")) {
                    NonTempProfileViewModel viewModel = new NonTempProfileViewModel();
                    seekArc.setCurrentTemp(Float.parseFloat(viewModel.monitorTemp(updatedEquip)+""));
                    nonTempProfileViewModels.add(viewModel);
                    viewModel.setProfile("MONITORING");
                    viewModel.setEquipId(updatedEquip.getId());
                    viewModel.setEquipName("MONITORING ("+updatedEquip.getGroup()+")");
                    viewModel.loadHyperStatMonitoringEquipPoints(updatedEquip.getId());
                    showHeaderViewUI(embededComposeView,
                            viewModel,
                            updatedEquip.getId()
                    );
                    viewModel.observeEquipHealthByGroupId(updatedEquip.getGroup().toString());

                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadOTNPointsUI(HashMap point, LayoutInflater inflater,
                                 LinearLayout linearLayoutZonePoints,
                                 String nodeAddress) {


        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.otn_zone_ui, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);

        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);


        TextView textViewValue1 = viewPointRow1.findViewById(R.id.text_point1value);
        TextView textViewValue2 = viewPointRow1.findViewById(R.id.text_point2value);

        CcuLog.d("OTNUtil","Status="+point.get("Status").toString() +
                "humidity ="+point.get("humidity").toString() +
                "forceoccupied" + point.get("forceoccupied"));

        textViewTitle.setText(point.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(point.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));


        textViewLabel1.setText(getString(R.string.humidity_text));
        textViewValue1.setText(point.get("humidity").toString() + "%");


        double occupied = (double) point.get("forceoccupied");
        if(occupied == (double)Occupancy.FORCEDOCCUPIED.ordinal()){
            textViewLabel2.setText(getString(R.string.temporary_hold));
            textViewValue2.setText(getString(R.string.yes_text));
        }else if(occupied == (double)Occupancy.AUTOFORCEOCCUPIED.ordinal()){
            textViewLabel2.setText(getString(R.string.temporary_hold_auto));
            textViewValue2.setText(getString(R.string.yes_text));
        }else{
            textViewLabel2.setText(getString(R.string.temporary_hold));
            textViewValue2.setText(getString(R.string.no_text));
        }
        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        linearLayoutZonePoints.addView(viewPointRow1);

    }

    /* If schedule revamp migration is still pending show migration pending alert*/
    private void showMigrationPendingDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.data_migration_pending))
                .setIcon(R.drawable.ic_alert)
                .setMessage(getString(R.string.data_migration_is_in_progress))
                .setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (validateMigration()) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            loadGrid(dialog, timer);
                        }
                    });
                }
            }
        }, 0, 3000);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                dialog.dismiss();
                timer.cancel(); // Cancel the timer and dismiss dialog after 30 minutes if still alive.
                PreferenceUtil.setDataMigrationPopUpClosed();
            }
        }, 30 * 60 * 1000);
    }

    public void loadGrid(AlertDialog dialog, Timer timer) {
        if(getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                loadGrid(parentRootView);
                dialog.dismiss();
                timer.cancel();
                PreferenceUtil.setDataMigrationPopUpClosed();
            });
        }
    }
    private CustomSpinnerDropDownAdapter getAdapterValue(ArrayList values) {
        return new CustomSpinnerDropDownAdapter(requireContext(), R.layout.spinner_dropdown_item, values);
    }

    private void transitionToZoneSchedules(ArrayList<HashMap> zoneMap, String scheduleId, View zoneDetails, boolean isComposeView) {
        Spinner scheduleSpinner     = zoneDetails.findViewById(R.id.schedule_spinner);
        ImageButton scheduleImageButton = zoneDetails.findViewById(R.id.schedule_edit_button);
        ImageButton vacationImageButton = zoneDetails.findViewById(R.id.vacation_edit_button);

        String zoneId = zoneMap.get(0).get("roomRef").toString();
        String scheduleTypeId = getScheduleTypeId(zoneMap.get(0).get("id").toString());

        HashMap<Object, Object> schedule = CCUHsApi.getInstance().readEntity("schedule and " +
                "not special and not vacation and roomRef " + "== " + zoneMap.get(0).get("roomRef"));
        HashMap<Object, Object> room = CCUHsApi.getInstance().readMapById(zoneId);
        Zone z = HSUtil.getZone(zoneId, Objects.requireNonNull(room.get("floorRef")).toString());
        if (z != null) {
            z.setScheduleRef(schedule.get("id").toString());
            CCUHsApi.getInstance().updateZone(z, zoneId);
        }

        if (!isComposeView) {
            scheduleSpinner.setSelection(0);
            scheduleImageButton.setTag(scheduleId);
            vacationImageButton.setTag(scheduleId);
            scheduleImageButton.setVisibility(View.VISIBLE);
        }
        CCUHsApi.getInstance().scheduleSync();

        int currentScheduleType = CCUHsApi.getInstance().readDefaultValById(scheduleTypeId).intValue();
        if (currentScheduleType != ScheduleType.ZONE.ordinal()) {
            setScheduleType(scheduleTypeId, ScheduleType.ZONE, zoneMap);
            CcuLog.d(L.TAG_CCU_UI, "Schedule Type updated");
        }
        mScheduleTypeMap.put(zoneMap.get(0).get("id").toString(), ScheduleType.ZONE.ordinal());
    }

    public void separateOvernightSchedules(ArrayList<Interval> zoneIntervals) {
        int size = zoneIntervals.size();
        for (int i = 0; i < size; i++) {
            Interval it = zoneIntervals.get(i);

            LocalTime startTimeOfDay = it.getStart().toLocalTime();
            LocalTime endTimeOfDay = it.getEnd().toLocalTime();

            // Check if the start time is after the end time and separating the overnight schedule
            if (startTimeOfDay.isAfter(endTimeOfDay)) {
                zoneIntervals.set(i, ScheduleUtil.OverNightEnding(it));
                zoneIntervals.add(ScheduleUtil.OverNightStarting(it));
            }
        }
    }

    private void updateSystemScheduleForSundayOvernight(ArrayList<Interval> zoneIntervals, ArrayList<Interval> systemIntervals, Schedule systemSchedule) {
        Interval ZonelastInterval = zoneIntervals.get(zoneIntervals.size()-1);
        LocalDate ZoneLastTimeOfDay = ZonelastInterval.getStart().toDateTime().toLocalDate();
        Interval systemLastInterval = systemIntervals.get(systemIntervals.size()-1);
        LocalDate systemLastTimeOfDay = systemLastInterval.getStart().toDateTime().toLocalDate();
        // checking for overnight for sunday ,if it is has overnight sch for sunday
        // we need to add the building occupancy for next week monday also **/
        if(ZoneLastTimeOfDay.isAfter(systemLastTimeOfDay))
        {
            Interval nextWeekDaySystemInterval = ScheduleUtil.AddingNextWeekDayForOverNight(systemSchedule);
            if(nextWeekDaySystemInterval!=null)
            {
                systemIntervals.add(nextWeekDaySystemInterval);
            }
        }
    }

    private void generateIntervalSpills(ArrayList<Interval> zoneIntervals, ArrayList<Interval> systemIntervals, ArrayList<Interval> intervalSpills) {
        for (Interval z : zoneIntervals) {
            boolean add = true;
            for (Interval s : systemIntervals) {
                if (s.contains(z)) {
                    add = false;
                    break;
                } else if (s.overlaps(z)) {
                    add = false;
                    for (Interval i : disconnectedIntervals(systemIntervals, z)) {
                        if (!intervalSpills.contains(i)) {
                            intervalSpills.add(i);
                        }
                    }

                }
            }
            if (add) {
                intervalSpills.add(z);
                CcuLog.d(L.TAG_CCU_UI, " Zone Interval not contained " + z);
            }
        }
    }

    private void generateOccupancyContainmentBreachedDialog(ArrayList<Interval> intervalSpills, Schedule zoneSchedule, ContainmentDialogClickListener listener) {
        StringBuilder spillZones = new StringBuilder();
        for (Interval i : intervalSpills) {
            spillZones.append(getDayString(i.getStart().getDayOfWeek())).append(" (").append(i.getStart().hourOfDay().get()).append(":").append(i.getStart().minuteOfHour().get() == 0 ? "00" : i.getStart().minuteOfHour().get()).append(" - ").append(i.getEnd().hourOfDay().get()).append(":").append(i.getEnd().minuteOfHour().get() == 0 ? "00" : i.getEnd().minuteOfHour().get()).append(") \n");
        }
        CommonTimeSlotFinder commonTimeSlotFinder = new CommonTimeSlotFinder();
        List<List<CommonTimeSlotFinder.TimeSlot>> commonTimeSlot =  commonTimeSlotFinder.
                getCommonTimeSlot(zoneSchedule.getScheduleGroup(), CCUHsApi.getInstance().getSystemSchedule(false).get(0).getDays(),
                        zoneSchedule.getDays(), true);
        List<List<CommonTimeSlotFinder.TimeSlot>> uncommonIntervals =
                commonTimeSlotFinder.getUnCommonTimeSlot(zoneSchedule.getScheduleGroup(), commonTimeSlot, zoneSchedule.getDays());
        Spanned message = HtmlCompat.fromHtml(
                "Force trim will erase the following time slot(s) of  <b>"
                        + ScheduleGroup.values()[zoneSchedule.getScheduleGroup()].getGroup() +
                        "</b> schedule group. Are you sure you want to proceed?"
                ,
                HtmlCompat.FROM_HTML_MODE_LEGACY);

        new AlertDialogAdapter(
                requireContext(),
                new AlertDialogData(
                        getString(R.string.zone_schedule_outside_building_occupancy),
                        message,
                        commonTimeSlotFinder.getSpilledZones(zoneSchedule, uncommonIntervals),
                        getString(R.string.force_trim_and_save),
                        v -> {
                            ScheduleUtil.trimScheduleTowardCommonTimeSlot(zoneSchedule, commonTimeSlot, commonTimeSlotFinder,
                                    CCUHsApi.getInstance());
                            listener.onDialogClick(true);
                        }, getString(R.string.cancel),
                        v -> listener.onDialogClick(false),
                        false,
                        false,
                        R.drawable.ic_dialog_alert
                )
        ).showCustomDialog();

    }


    boolean isRoomsScheduleRefAvailable(String equipId) {
        HDict equipDict = CCUHsApi.getInstance().readHDictById(equipId);
        Equip   equip        = new Equip.Builder().setHDict(equipDict).build();
        String roomRef = equip.getRoomRef().replace("@", "");

        HashMap<Object, Object> zoneHashMap = CCUHsApi.getInstance().readMapById(roomRef);
        Zone zone = new Zone.Builder().setHashMap(zoneHashMap).build();
        String ref = zone.getScheduleRef();

        Double scheduleType;
        if(CCUUiUtil.isDomainEquip(equipId, "equip")) {
            scheduleType =  CCUUiUtil.readPriorityValByEquipRef(DomainName.scheduleType, equipId);
        } else {
            scheduleType = CCUHsApi.getInstance().readPointPriorityValByQuery("point and scheduleType " +
                    "and roomRef == \""+ StringUtils.prependIfMissing(roomRef, "@")+"\"");
        }
        if (scheduleType != null && scheduleType.intValue() == 2) {
            Schedule schedule = CCUHsApi.getInstance().getScheduleById(ref);
            return schedule == null;
        }
        return true;
    }

    private void disableVisibiltyForZoneScheduleUI(View zoneDetails) {
        LinearLayout ll_status = zoneDetails.findViewById(R.id.lt_status);
        LinearLayout ll_schedule = zoneDetails.findViewById(R.id.lt_schedule);
        LinearLayout vc_schedule = zoneDetails.findViewById(R.id.vc_schedule);
        vc_schedule.setVisibility(View.GONE);
        ll_status.setVisibility(View.GONE);
        ll_schedule.setVisibility(View.GONE);
    }

    private void displayRoomSpecificCustomControlFields(View zoneDetails, Equip nonTempEquip) {
        if(nonTempEquip == null) {
            CcuLog.i(L.TAG_CONNECT_NODE, "Non temp equip is null for Connect Node.");
            return;
        }
        zoneDetails.findViewById(R.id.pointBasedScheduleLayout).setVisibility(View.VISIBLE);
        if(CustomScheduleUtilKt.isZoneUsingCustomSchedulesOrEvents(nonTempEquip.getRoomRef())) {
            updateCustomScheduleLabelValue(zoneDetails, nonTempEquip.getRoomRef());
            displaySchedulablePointWithoutSchedulesOrEvents(nonTempEquip, zoneDetails);
        }
    }


    private List<EquipmentDevice> fetchAllModbusEquips(Equip nonTempEquip) {
        List<EquipmentDevice> modbusDevices = new ArrayList<>();
        for (EquipmentDevice equipmentDevice : buildModbusModel(nonTempEquip.getRoomRef())) {
            modbusDevices.add(equipmentDevice);
            if (null != equipmentDevice.getEquips()) {
                modbusDevices.addAll(equipmentDevice.getEquips());
            }
        }
        CcuLog.i("MODBUS_UI", "ZoneData:" + modbusDevices);
        return modbusDevices;
    }

    private void updateCustomScheduleLabelValue(View zoneDetails, String roomRef) {
        ((TextView) zoneDetails.findViewById(R.id.pointScheduleAvailabilityView))
                .setText(R.string.point_schedule_assigned);
        ImageButton detailedPointScheduleViewButton = zoneDetails.findViewById(R.id.point_schedule_view_button);
        detailedPointScheduleViewButton.setVisibility(View.VISIBLE);
        detailedPointScheduleViewButton.setOnClickListener(view -> {
            CustomControlDialog dialog = new CustomControlDialog(roomRef);
            FragmentManager childFragmentManager = getChildFragmentManager();
            dialog.show(childFragmentManager, "dialog");
        });
    }

    private void displaySchedulablePointWithoutSchedulesOrEvents(Equip nonTempEquip, View zoneDetails) {

        List<String> schedulablePointDisWithoutCustomControl = new ArrayList<>();
        if (nonTempEquip.getMarkers().contains("modbus")
                || nonTempEquip.getMarkers().contains("connectModule")
                || nonTempEquip.getMarkers().contains("pcn")
                || nonTempEquip.getMarkers().contains("bacnet")) {
            schedulablePointDisWithoutCustomControl = CustomScheduleUtilKt
                    .fetchSchedulablePointsWithoutCustomControl(nonTempEquip.getRoomRef());
        }
        if(!schedulablePointDisWithoutCustomControl.isEmpty()) {

            zoneDetails.findViewById(R.id.schedulablePointsWithoutScheduleLayout).setVisibility(View.VISIBLE);
            TextListAdapter pointsWithoutSchedulelistAdapter = new TextListAdapter(schedulablePointDisWithoutCustomControl);

            RecyclerView pointsWithoutScheduleRecyclerView = zoneDetails.findViewById(R.id.point_without_schedule_recycler_view);
            pointsWithoutScheduleRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            pointsWithoutScheduleRecyclerView.setAdapter(pointsWithoutSchedulelistAdapter);
        }
    }

    private String getEquipmentDeviceName(String profileType, Object profileParam) {
        StringBuilder equipName = new StringBuilder();

        switch(profileType) {
            case "MODBUS":
                EquipmentDevice modbusDevice = (EquipmentDevice) profileParam;

                int displayIndex = modbusDevice.getName().lastIndexOf('-') + 1;
                String displayName = modbusDevice.getName().substring(displayIndex);
                if(!modbusDevice.getEquipType().contains(displayName)) {
                    equipName.append(displayName);
                } else {
                    for(String equipType : modbusDevice.getEquipType().split(",")){
                        equipName.append(StringUtils.capitalize(equipType.trim()));
                        equipName.append(" ");
                    }
                }
                return equipName.toString().trim() + "(" + modbusDevice.getSlaveId() + ")";

            case "BACNET":
                Equip nonTempEquip = (Equip) profileParam;
                Map<String, String> bacnetConfig = EntityKVStringParserUtilKt.getConfig(
                        nonTempEquip.getTags().getOrDefault(
                                "bacnetConfig",
                                HStr.make("")).toString()
                );
                String macAddr = bacnetConfig.get("macAddress");
                if (macAddr == null || macAddr.isEmpty()) {
                    macAddr = "NA";
                }
                String bacnetDeviceId = bacnetConfig.get("deviceId");
                if (bacnetDeviceId == null || bacnetDeviceId.isEmpty()) {
                    bacnetDeviceId = "NA";
                }

                for (BacnetModelDetailResponse item : buildBacnetModel(nonTempEquip.getRoomRef())){
                    equipName.append(item.getName());
                    CcuLog.d(BACNET, "EquipName: " + equipName);
                    bacNetPointsList = fetchZoneDataForBacnet(
                            item.getPoints(),
                            Objects.requireNonNull(nonTempEquip.getTags().get("bacnetConfig")).toString()
                    );
                    CcuLog.d(BACNET, "bacNetPointsList: " + bacNetPointsList.size());

                }
                return equipName.toString().trim() + " - " +
                        " ( " +
                        nonTempEquip.getGroup() +
                        " | Device ID: " +
                        bacnetDeviceId +
                        " | MAC Addr: " +
                        macAddr +
                        " )";
            case CONNECTMODULE:
                return ((EquipmentDevice) profileParam).getName();
            default:
                return "";
        }
    }

    public void showCustomScheduleView(ZoneData zonedata) {
        View zoneDetails = zonedata.getZoneDetails();
        Equip nonTempEquip = zonedata.getEquip();
        zoneDetails.findViewById(R.id.pointBasedScheduleLayout).setVisibility(View.VISIBLE);
        updateCustomScheduleLabelValue(zoneDetails, nonTempEquip.getRoomRef());
        displaySchedulablePointWithoutSchedulesOrEvents(nonTempEquip, zoneDetails);
    }

    public void hideCustomScheduleView(ZoneData zonedata) {
        View zoneDetails = zonedata.getZoneDetails();

        ImageButton detailedPointScheduleViewButton = zoneDetails.findViewById(R.id.point_schedule_view_button);
        detailedPointScheduleViewButton.setVisibility(View.GONE);

        TextView noSchedule = zoneDetails.findViewById(R.id.pointScheduleAvailabilityView);
        noSchedule.setText(R.string.no_point_schedule_assigned);

        zoneDetails.findViewById(R.id.schedulablePointsWithoutScheduleLayout).setVisibility(View.GONE);
    }
}

interface ContainmentDialogClickListener {
    void onDialogClick(Boolean isForceTrimmed);
}
