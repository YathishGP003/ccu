package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.telephony.gsm.GsmCellLocation;

import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.bpos.BPOSUtil;
import a75f.io.logic.bo.building.hvac.StandaloneFanStage;
import a75f.io.logic.bo.building.plc.PlcProfile;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
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

import org.joda.time.Interval;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.Parameter;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.device.mesh.Pulse;
import a75f.io.device.mesh.hyperstat.HyperStatMsgReceiver;
import a75f.io.logger.CcuLog;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.bpos.BPOSUtil;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ScheduleType;
import a75f.io.logic.bo.building.dualduct.DualDuctUtil;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.hvac.StandaloneFanStage;
import a75f.io.logic.bo.building.hyperstat.comman.FanModeCacheStorage;
import a75f.io.logic.bo.building.hyperstat.comman.HSZoneStatus;
import a75f.io.logic.bo.building.hyperstat.comman.SettingsKt;
import a75f.io.logic.bo.building.sshpu.HeatPumpUnitConfiguration;
import a75f.io.logic.jobs.HyperStatScheduler;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.jobs.StandaloneScheduler;
import a75f.io.logic.pubnub.UpdatePointHandler;
import a75f.io.logic.pubnub.ZoneDataInterface;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.modbusbox.EquipsManager;
import a75f.io.renatus.hyperstat.vrv.HyperStatVrvZoneViewKt;
import a75f.io.renatus.modbus.ZoneRecyclerModbusParamAdapter;
import a75f.io.renatus.schedules.ScheduleUtil;
import a75f.io.renatus.schedules.SchedulerFragment;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.GridItem;
import a75f.io.renatus.util.HeartBeatUtil;
import a75f.io.renatus.util.NonTempControl;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.RelayUtil;
import a75f.io.renatus.util.SeekArc;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static a75f.io.logic.bo.util.RenatusLogicIntentActions.ACTION_SITE_LOCATION_UPDATED;
import static a75f.io.renatus.schedules.ScheduleUtil.disconnectedIntervals;

public class ZoneFragmentNew extends Fragment implements ZoneDataInterface {
    private static final String LOG_TAG = " ZoneFragmentNew ";
    ExpandableListView expandableListView;
    HashMap<String, List<String>> expandableListDetail;


    ImageView floorMenu;
    public DrawerLayout mDrawerLayout;
    public LinearLayout drawer_screen;
    public ListView lvFloorList;
    public ArrayList<Floor> floorList = new ArrayList();
    public DataArrayAdapter<Floor> mFloorListAdapter;
    ArrayList<Zone> roomList = new ArrayList();

    private RelativeLayout weather_data = null;
    private TextView place;
    private TextView temperature;
    private TextView weather_condition;
    private ImageView weather_icon;
    private TextView maxmintemp;
    private TextView note;
    private Runnable weatherUpdate;
    private Handler weatherUpdateHandler;
    public RecyclerView recyclerView;
    GridLayout gridlayout;
    TableLayout tableLayout;
    private Animation in = null;
    private Animation inleft = null;

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
    double currentTempSensor = 0;
    int noTempSensor = 0;
    HashMap<String, Integer> mScheduleTypeMap = new HashMap<>();
    Prefs prefs;

    TextView zoneLoadTextView = null;
    public ZoneFragmentNew() {
    }

    public static ZoneFragmentNew newInstance() {
        return new ZoneFragmentNew();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_zones, container, false);
        parentRootView = rootView.findViewById(R.id.zone_fragment_temp);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.onViewCreated");
        expandableListView = view.findViewById(R.id.expandableListView);
        mDrawerLayout = view.findViewById(R.id.drawer_layout);
        drawer_screen = view.findViewById(R.id.drawer_screen);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        lvFloorList = view.findViewById(R.id.floorList);

        weather_data = (RelativeLayout) getView().findViewById(R.id.weather_data);
        place = (TextView) getView().findViewById(R.id.place);
        temperature = (TextView) getView().findViewById(R.id.temperature);
        weather_condition = (TextView) getView().findViewById(R.id.weather_condition);
        weather_icon = (ImageView) getView().findViewById(R.id.weather_icon);
        maxmintemp = (TextView) getView().findViewById(R.id.maxmintemp);
        note = (TextView) getView().findViewById(R.id.note);

        scrollViewParent = view.findViewById(R.id.scrollView_zones);
        tableLayout = (TableLayout) view.findViewById(R.id.tableRoot);
        gridlayout = (GridLayout) view.findViewById(R.id.gridview);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerEquip);

        recyclerView.setVisibility(View.GONE);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 4));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        in = AnimationUtils.makeInAnimation(getActivity(), false);
        inleft = AnimationUtils.makeInAnimation(getActivity(), true);
        in.setDuration(400);
        inleft.setDuration(400);

        //recyclerView.setHasFixedSize(true);
        prefs = new Prefs(getContext().getApplicationContext());
        expandableListDetail = new HashMap<>();

        floorList = HSUtil.getFloors();
        Collections.sort(floorList, new FloorComparator());

        mFloorListAdapter = new DataArrayAdapter<Floor>(getActivity(), R.layout.listviewitem, floorList);
        lvFloorList.setAdapter(mFloorListAdapter);
        
        zoneLoadTextView = view.findViewById(R.id.zoneLoadTextView);
        zoneLoadTextView.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
        
        loadGrid(parentRootView);
        
        if (floorList != null && floorList.size() > 0) {
            lvFloorList.setContentDescription(floorList.get(0).getDisplayName());
        }
        floorMenu = (ImageView) view.findViewById(R.id.floorMenu);
        floorMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFloor();
            }
        });

        lvFloorList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectFloor(position);
            }
        });
    
        getContext().registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                CcuLog.i("CCU_WEATHER","ACTION_SITE_LOCATION_UPDATED ");
                WeatherDataDownloadService.getWeatherData();
                if (weatherUpdateHandler != null)
                    weatherUpdateHandler.post(weatherUpdate);
            }
        }, new IntentFilter(ACTION_SITE_LOCATION_UPDATED));
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.onViewCreated Done");
    }

    public void refreshScreen(String id)
    {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.refreshScreen zoneOpen "+zoneOpen);
        if(getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(() -> {
                if(zoneOpen) {
                    updateTemperatureBasedZones(seekArcOpen, zonePointsOpen, equipOpen, getLayoutInflater());
                    tableLayout.invalidate();
                }
            });
        }
    }
    
    HashMap<String, View> zoneStatus = new HashMap<>();
    
    public void refreshHeartBeatStatus(String id) {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.refreshHeartBeatStatus zoneOpen "+zoneOpen);
        HashMap equip = CCUHsApi.getInstance().read("equip and group ==\""+id+"\"");
        if (!equip.isEmpty()) {
            HashMap zone = CCUHsApi.getInstance().readMapById(equip.get("roomRef").toString());
            ArrayList<HashMap> equipsInZone = CCUHsApi.getInstance().readAll("equip and zone and roomRef ==\""
                                                                             +zone.get("id")+ "\"");
            if(equipsInZone.size() > 0) {
                boolean isZoneAlive = HeartBeatUtil.isZoneAlive(equipsInZone);
                View statusView  = zoneStatus.get(zone.get("dis").toString());
                if (statusView != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> HeartBeatUtil.zoneStatus(statusView, isZoneAlive));
                }
            }
        }
    }

    public void refreshDesiredTemp(String nodeAddress, String pointcoolDT1, String pointheatDT1) {
        if (getActivity() != null ) {
            int i;
            for (i = 0; i < seekArcArrayList.size(); i++) {
                GridItem gridItem = (GridItem) seekArcArrayList.get(i).getTag();
                if (gridItem.getNodeAddress() == Short.valueOf(nodeAddress)) {
                    if( CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and" +
                            " desired and heating and group == \"" + nodeAddress + "\"") == null){
                        return;
                    }
                    SeekArc tempSeekArc = seekArcArrayList.get(i);
                    double pointheatDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and heating and group == \"" + nodeAddress + "\"");
                    double pointcoolDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and cooling and group == \"" + nodeAddress + "\"");
                    //float coolDt = Float.parseFloat(pointcoolDT);
                    //float heatDt = Float.parseFloat(pointheatDT);
                    if ((tempSeekArc.getCoolingDesiredTemp() != pointcoolDT) || (tempSeekArc.getHeatingDesiredTemp() != pointheatDT)) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(LOG_TAG + "Scheduler", "refreshDesiredTemp22 =" + pointcoolDT + "," + pointheatDT + "," + nodeAddress);
                                tempSeekArc.setCoolingDesiredTemp((float) pointcoolDT, false);
                                tempSeekArc.setHeatingDesiredTemp((float) pointheatDT, false);
                                tempSeekArc.invalidate();
                            }
                        });
                    }
                }
            }
        }
    }
    
    public void refreshScreenbySchedule(String nodeAddress, String equipId, String zoneId) {
        if (getActivity() != null) {
            int i;
            String status = ScheduleProcessJob.getZoneStatusMessage(zoneId, equipId);
            String vacationStatus = ScheduleProcessJob.getVacationStateString(zoneId);
            for (i = 0; i < zoneStatusArrayList.size(); i++) {
                GridItem gridItem = (GridItem) zoneStatusArrayList.get(i).getTag();
                if (gridItem.getNodeAddress() == Short.valueOf(nodeAddress)) {
                    View tempZoneDetails = zoneStatusArrayList.get(i);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView scheduleStatus = tempZoneDetails.findViewById(R.id.schedule_status_tv);
                            //Spinner scheduleSpinner = tempZoneDetails.findViewById(R.id.schedule_spinner);
                            TextView vacationStatusTV = tempZoneDetails.findViewById(R.id.vacation_status);
                            ImageButton vacationImageButton = tempZoneDetails.findViewById(R.id.vacation_edit_button);
                            vacationStatusTV.setText(vacationStatus);
                            scheduleStatus.setText(status);

                           /* if (vacationStatus.equals("Active Vacation"))
                            {
                                vacationImageButton.setVisibility(View.VISIBLE);
                            } else
                            {
                                vacationImageButton.setVisibility(View.GONE);
                            }*/
                        }
                    });
                }
            }
        }
    }

    public void updateTemperature(double currentTemp, short nodeAddress) {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.updateTemperature");
        if (getActivity() != null) {
            int i;
            if (currentTemp > 0) {
                double buildingLimitMax = BuildingTunerCache.getInstance().getBuildingLimitMax();
                double buildingLimitMin = BuildingTunerCache.getInstance().getBuildingLimitMin();
                double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();

                for (i = 0; i < seekArcArrayList.size(); i++) {
                    GridItem gridItem = (GridItem) seekArcArrayList.get(i).getTag();
                    ArrayList<Short> zoneNodes = gridItem.getZoneNodes();
                    Log.i(LOG_TAG + "CurrentTemp", "SensorCurrentTemp:" + currentTemp + " Node:" + nodeAddress + " zoneNodes:" + zoneNodes);
                    if (zoneNodes.contains(nodeAddress)) {
                        SeekArc tempSeekArc = seekArcArrayList.get(i);
                        new AsyncTask<String, Void, Double>() {
                            @Override
                            protected Double doInBackground(final String... params) {
                                currentTempSensor = 0;
                                noTempSensor = 0;
                                ArrayList<HashMap> zoneEquips = gridItem.getZoneEquips();
                                for (int j = 0; j < zoneEquips.size(); j++) {
                                    Equip tempEquip = new Equip.Builder().setHashMap(zoneEquips.get(j)).build();
                                    double avgTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and equipRef == \"" + tempEquip.getId() + "\"");
                                    if ((avgTemp <= (buildingLimitMax + tempDeadLeeway)) && (avgTemp >= (buildingLimitMin - tempDeadLeeway))) {
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
                                if (currentTempSensor > 0) {
                                    return currentTempSensor;
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(final Double result) {
                                if (result != null) {
                                    tempSeekArc.setCurrentTemp((float) (result.doubleValue()));
                                }
                            }
                        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "");
                        break;
                    }
                }
            }
        }
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.updateTemperature Done");
    }

    public void updateSensorValue(short nodeAddress) {
        if (getActivity() != null) {
            if (zoneNonTempOpen) {
                if (equipOpen.getProfile().contains("PLC") || equipOpen.getProfile().contains("EMR") || equipOpen.getProfile().contains("monitor")) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateNonTemperatureBasedZones(nonTempControlOpen, zonePointsOpen, equipOpen, getLayoutInflater());
                            tableLayout.invalidate();
                        }
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

    public void UpdateWeatherData() {
        if (WeatherDataDownloadService.getMinTemperature() != 0.0 && WeatherDataDownloadService.getMaxTemperature() != 0.0) {

            temperature.setText(String.format("%.0f", WeatherDataDownloadService.getTemperature()));
            maxmintemp.setText(String.format("%.0f", WeatherDataDownloadService.getMaxTemperature()) + "\n" + String.format("%.0f", WeatherDataDownloadService.getMinTemperature()));
            DecimalFormat df = new DecimalFormat("#.##");
            double weatherPercipitation = WeatherDataDownloadService.getPrecipitation();
            double weatherHumidity = WeatherDataDownloadService.getHumidity();
            weatherHumidity = weatherHumidity * 100;
            weatherPercipitation = Double.valueOf(df.format(weatherPercipitation));
            note.setText("Humidity : " + weatherHumidity + "%" + "\n" + "Precipitation : " + weatherPercipitation);
            SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext());
            String address = spDefaultPrefs.getString("address", "");
            String city = spDefaultPrefs.getString("city", "");
            String country = spDefaultPrefs.getString("country", "");
            if (address.isEmpty()) {
                place.setText(city + ", " + country);
            } else {
                //Address format could be City,State-ZIP,Country or State-ZIP,Country otherwise default to installer data
                String[] addrArray = address.split(",");
                if (addrArray != null && addrArray.length >= 3) {
                    place.setText(addrArray[0] + ", " + addrArray[2]);
                } else if (addrArray != null && addrArray.length == 2) {
                    place.setText(address);
                } else {
                    place.setText(city + ", " + country);
                }
            }

            weather_condition.setText(WeatherDataDownloadService.getSummary());
            String weather_icon_string = WeatherDataDownloadService.getIcon().toString().replaceAll("-", "");
            Context context = weather_icon.getContext();
            int id = context.getResources().getIdentifier(weather_icon_string, "drawable", context.getPackageName());
            weather_icon.setImageResource(id);
        }
    }

    private void selectFloor(int position) {
        mFloorListAdapter.setSelectedItem(position);
        roomList = HSUtil.getZones(floorList.get(position).getId());
        closeFloor();
        //updateData();
        showWeather();
        clickedView = -1;
        loadGrid(parentRootView);
        lvFloorList.setContentDescription(floorList.get(position).getDisplayName());
        expandableListView.invalidateViews();
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

    public void closeFloor() {
        try {
            mDrawerLayout.closeDrawer(drawer_screen);
        } catch (Exception e) {
            e.printStackTrace();
            if (mDrawerLayout != null && mDrawerLayout.isShown()) {
                mDrawerLayout.closeDrawer(drawer_screen);
            }
        }
    }
    int gridPosition = 0;
    private void loadGrid(View rootView) {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.loadGrid");
        rowcount = 0;
        if (floorList.size() > 0) {
            ArrayList<HashMap> roomList =
                CCUHsApi.getInstance().readAll("room and floorRef == \"" + floorList.get(mFloorListAdapter.getSelectedPostion()).getId() + "\"");
            imag = new ImageView(getActivity());
            tableLayout.removeAllViews();
            gridItems.clear();
            tableRows.clear();
            String[] itemNames = getResources().getStringArray(R.array.sse_action_type);
            LinearLayout rowLayout = null;
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
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    for (int m = 0; m < roomList.size(); m++) {
                        try {
                            loadZone(rootView, tablerowLayout, roomList.get(m));
                        } catch (Exception e) {
                            CcuLog.e(LOG_TAG, "Loading Zone failed");
                            e.printStackTrace();
                        }
                    }
                    setCcuReady();
                }, 100);
            } else {
                setCcuReady();
            }
        } else {
            setCcuReady();
        }
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.loadGrid Done");
    }
    
    private void setCcuReady() {
        Globals.getInstance().setCcuReady(true);
        setListeners();
        zoneLoadTextView.setVisibility(View.GONE);
    }


    private int loadZone(View rootView, LinearLayout[] tablerowLayout, HashMap roomMap) {

        String zoneTitle = "";
        LayoutInflater inflater = LayoutInflater.from(getContext());

        zoneTitle = roomMap.get("dis").toString();
        ArrayList<HashMap> equips =
            CCUHsApi.getInstance().readAll("equip and zone and roomRef ==\"" + roomMap.get("id").toString() + "\"");
        if (equips.size() > 0) {// zones has devices paired
            boolean isZoneAlive = HeartBeatUtil.isZoneAlive(equips);
            HashMap<String, ArrayList<HashMap>> zoneData = new HashMap<String, ArrayList<HashMap>>();
            for (HashMap zoneModel : equips) {
                if (zoneData.containsKey(zoneModel.get("roomRef").toString())) {
                    ArrayList<HashMap> exisiting = zoneData.get(zoneModel.get("roomRef").toString());
                    exisiting.add(zoneModel);
                    zoneData.put(zoneModel.get("roomRef").toString(), exisiting);
                } else {
                    ArrayList<HashMap> newData = new ArrayList<HashMap>();
                    newData.add(zoneModel);
                    zoneData.put(zoneModel.get("roomRef").toString(), newData);
                }
            }
            Log.d(LOG_TAG + "ZonesMap", "Size:" + zoneData.size() + " Data:" + zoneData);
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
                String profileHyperStatSense = "HYPERSTAT_SENSE";
                String profilebpos = "BPOS";

                boolean tempModule = false;
                boolean nontempModule = false;
                for (HashMap equipTypes : equipZones) {
                    profileType = equipTypes.get("profile").toString();
                    Log.e(LOG_TAG + "RoomData", "ProfileType:" + profileType);
                    if (!profileType.contains(profileModBus) &&
                            profileType.contains(profileVAV) ||
                            profileType.contains(profileDAB) ||
                            profileType.contains(profileSSE) ||
                            profileType.contains(profileSmartStat) ||
                            profileType.contains(profileTempInfluence) ||
                            profileType.contains(profileDualDuct) ||
                            profileType.contains(ProfileType.HYPERSTAT_VRV.name()) ||
                            profileType.contains(profilebpos)||
                            profileType.contains(ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT.name())
                    ) {
                        tempModule = true;
                    }
                    if (profileType.contains(profileEM) || profileType.contains(profilePLC)
                            || profileType.contains(profileTempMonitor)
                            || profileType.contains(profileTempInfluence)
                            || profileType.contains(profileModBus)) {
                        nontempModule = true;
                    }
                }
                if (profileType.contains(profileHyperStatSense)) {
                    viewSenseZone(inflater, rootView, equipZones, zoneTitle, gridPosition, tablerowLayout, isZoneAlive);
                }

                if (tempModule) {
                    viewTemperatureBasedZone(inflater, rootView, equipZones, zoneTitle, gridPosition, tablerowLayout, isZoneAlive);
                }
                if (!tempModule && nontempModule && !profileType.contains(profileHyperStatSense)) {
                    viewNonTemperatureBasedZone(inflater, rootView, equipZones, zoneTitle, gridPosition, tablerowLayout, isZoneAlive);
                    //arcViewParent = inflater.inflate(R.layout.zones_item_smartstat, (ViewGroup) rootView, false);
                }
                gridPosition++;
            }
        } else {
            //No devices paired
            viewNonTemperatureBasedZone(inflater, rootView, new ArrayList<HashMap>(), zoneTitle, gridPosition, tablerowLayout,
                                        false);
            gridPosition++;
        }
        return gridPosition;
    }

    private void viewTemperatureBasedZone(LayoutInflater inflater, View rootView, ArrayList<HashMap> zoneMap,String zoneTitle, int gridPosition, LinearLayout[] tablerowLayout, boolean isZoneAlive)
    {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.viewTemperatureBasedZone");

        Log.i("ProfileTypes","Points:"+zoneMap.toString());
        Equip p = new Equip.Builder().setHashMap(zoneMap.get(0)).build();
        Log.i("ProfileTypes", "p:" + p.toString());
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
        double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();

        for (int i = 0; i < zoneMap.size(); i++) {
            Equip avgTempEquip = new Equip.Builder().setHashMap(zoneMap.get(i)).build();
            double avgTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and equipRef == \"" + avgTempEquip.getId() + "\"");

            double heatDB = TunerUtil.getZoneHeatingDeadband(avgTempEquip.getRoomRef());
            double coolDB = TunerUtil.getZoneCoolingDeadband(avgTempEquip.getRoomRef());


            if (heatDB < heatDeadband || heatDeadband == 0) {
                heatDeadband = heatDB;
            }
            if (coolDB < coolDeadband || coolDeadband == 0) {
                coolDeadband = coolDB;
            }
            if ((avgTemp <= (buildingLimitMax + tempDeadLeeway)) && (avgTemp >= (buildingLimitMin - tempDeadLeeway))) {
                currentAverageTemp = (currentAverageTemp + avgTemp);
            } else {
                noTempSensor++;
            }
            equipNodes.add(Short.valueOf(avgTempEquip.getGroup()));
            Log.i("EachzoneData", "temp:" + avgTemp + " currentAvg:" + currentAverageTemp);

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
        Log.i("EachzoneData", " currentAvg:" + currentAverageTemp);
        final String[] equipId = {p.getId()};
        int i = gridPosition;
        View arcView = null;
        arcView = inflater.inflate(R.layout.zones_item, (ViewGroup) rootView, false);
        View zoneDetails = inflater.inflate(R.layout.zones_item_details, null);

        LinearLayout linearLayoutZonePoints  = zoneDetails.findViewById(R.id.lt_profilepoints);
        TextView    scheduleStatus      = zoneDetails.findViewById(R.id.schedule_status_tv);
        Spinner scheduleSpinner     = zoneDetails.findViewById(R.id.schedule_spinner);
        CCUUiUtil.setSpinnerDropDownColor(scheduleSpinner,getContext());
        ImageButton scheduleImageButton = zoneDetails.findViewById(R.id.schedule_edit_button);
        ImageButton vacationImageButton = zoneDetails.findViewById(R.id.vacation_edit_button);
        TextView vacationStatusTV = zoneDetails.findViewById(R.id.vacation_status);


        ArrayAdapter<CharSequence> scheduleAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.schedule, R.layout.spinner_zone_item);
        scheduleAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        scheduleSpinner.setAdapter(scheduleAdapter);

        String zoneId = Schedule.getZoneIdByEquipId(equipId[0]);

        Observable.fromCallable(() -> ScheduleProcessJob.getZoneStatusMessage(zoneId, equipId[0]))
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(status -> scheduleStatus.setText(status));

        Observable.fromCallable(() -> ScheduleProcessJob.getVacationStateString(zoneId))
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(status -> vacationStatusTV.setText(status));

        String scheduleTypeId = getScheduleTypeId(equipId[0]);
        final Integer mScheduleType = (int) CCUHsApi.getInstance().readPointPriorityVal(scheduleTypeId);
        Log.d("ScheduleType", "mScheduleType==" + mScheduleType + "," + (int) CCUHsApi.getInstance().readPointPriorityVal(scheduleTypeId) + "," + p.getDisplayName());
        mSchedule = Schedule.getScheduleByEquipId(equipId[0]);
        scheduleSpinner.setTag(mScheduleType);
        mScheduleTypeMap.put(equipId[0], mScheduleType);
        scheduleImageButton.setTag(mSchedule.getId());
        vacationImageButton.setTag(mSchedule.getId());

        if (mSchedule.isZoneSchedule() && !mSchedule.isBuildingSchedule()) {
            scheduleImageButton.setVisibility(View.VISIBLE);
        } else {
            scheduleImageButton.setVisibility(View.GONE);
        }

        scheduleImageButton.setOnClickListener(v ->
        {
            SchedulerFragment schedulerFragment = SchedulerFragment.newInstance((String) v.getTag(), false, zoneId);
            FragmentManager childFragmentManager = getFragmentManager();
            childFragmentManager.beginTransaction();
            schedulerFragment.show(childFragmentManager, "dialog");

            schedulerFragment.setOnExitListener(() -> {
                Toast.makeText(v.getContext(), "Refresh View", Toast.LENGTH_LONG).show();
                mSchedule = Schedule.getScheduleByEquipId(equipId[0]);
                ScheduleProcessJob.updateSchedules(equipOpen);
            });
        });

        vacationImageButton.setOnClickListener(v ->
        {
            SchedulerFragment schedulerFragment = SchedulerFragment.newInstance((String) v.getTag(), true, zoneId);
            FragmentManager childFragmentManager = getFragmentManager();
            childFragmentManager.beginTransaction();
            schedulerFragment.show(childFragmentManager, "dialog");

            schedulerFragment.setOnExitListener(() -> {
                Toast.makeText(v.getContext(), "Refresh View", Toast.LENGTH_LONG).show();
                mSchedule = Schedule.getScheduleByEquipId(equipId[0]);
                ScheduleProcessJob.updateSchedules(equipOpen);


            });
        });

        scheduleSpinner.setSelection(mScheduleType, false);

        scheduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CcuLog.i("UI_PROFILING","ZoneFragmentNew.scheduleSpinner");

                if (position == 0 && (mScheduleType != -1)) {
                    if (mSchedule.isZoneSchedule()) {
                        mSchedule.setDisabled(true);
                        CCUHsApi.getInstance().updateZoneSchedule(mSchedule, zoneId);
                    }
                    scheduleImageButton.setVisibility(View.GONE);

                    if (mScheduleTypeMap.get(equipId[0]) != ScheduleType.BUILDING.ordinal()) {
                        setScheduleType(scheduleTypeId, ScheduleType.BUILDING, zoneMap);
                        mScheduleTypeMap.put(equipId[0], ScheduleType.BUILDING.ordinal());
                    }
                    scheduleImageButton.setTag(mSchedule.getId());
                    vacationImageButton.setTag(mSchedule.getId());
                    CCUHsApi.getInstance().scheduleSync();
                } else if (position == 1 && (mScheduleType != -1)/*&& (mScheduleType != position)*/) {
                    clearTempOverride(equipId[0]);
                    boolean isContainment = true;
                    if (mSchedule.isZoneSchedule() && mSchedule.getMarkers().contains("disabled")) {
                        mSchedule.setDisabled(false);
                        CCUHsApi.getInstance().updateZoneSchedule(mSchedule, zoneId);
                        scheduleImageButton.setTag(mSchedule.getId());
                        vacationImageButton.setTag(mSchedule.getId());
                    } else {

                        Zone zone = Schedule.getZoneforEquipId(equipId[0]);
                        Schedule scheduleById = null;
                        if (zone.hasSchedule()) {
                            scheduleById = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
                            Log.d(L.TAG_CCU_UI, " scheduleType changed to ZoneSchedule : " + scheduleTypeId);
                            scheduleById.setDisabled(false);
                            isContainment = checkContainment(scheduleTypeId, scheduleById, scheduleSpinner, zoneMap);
                            CCUHsApi.getInstance().updateZoneSchedule(scheduleById, zone.getId());
                        } else if (!zone.hasSchedule()) {
                            Log.d(L.TAG_CCU_UI, " Zone does not have Schedule : Shouldn't happen");
                            DefaultSchedules.setDefaultCoolingHeatingTemp();
                            zone.setScheduleRef(DefaultSchedules.generateDefaultSchedule(true, zone.getId()));
                            CCUHsApi.getInstance().updateZone(zone, zone.getId());
                            scheduleById = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
                            //CCUHsApi.getInstance().syncEntityTree();
                        }
                        scheduleImageButton.setTag(scheduleById.getId());
                        vacationImageButton.setTag(scheduleById.getId());
                        scheduleImageButton.setVisibility(View.VISIBLE);
                        CCUHsApi.getInstance().scheduleSync();
                    }
                    if (mScheduleTypeMap.get(equipId[0]) != ScheduleType.ZONE.ordinal()) {
                        if (isContainment) {
                            setScheduleType(scheduleTypeId, ScheduleType.ZONE, zoneMap);
                        }
                        mScheduleTypeMap.put(equipId[0], ScheduleType.ZONE.ordinal());
                    }
                } else if (position == 2 && (mScheduleType != -1)) {
                    scheduleImageButton.setVisibility(View.GONE);
                } else {
                    //list named schedules
                }
                mSchedule = Schedule.getScheduleByEquipId(equipId[0]);
                scheduleImageButton.setTag(mSchedule.getId());
                vacationImageButton.setTag(mSchedule.getId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

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
        TextView textViewModule = arcView.findViewById(R.id.module_status);
        View status_view = arcView.findViewById(R.id.status_view);
        HeartBeatUtil.zoneStatus(textViewModule, isZoneAlive);
        zoneStatus.put(zoneTitle, textViewModule);

        seekArc.scaletoNormal(250, 210);
        String floorName = floorList.get(mFloorListAdapter.getSelectedPostion()).getDisplayName();

        double pointheatDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and heating and equipRef == \"" + p.getId() + "\"");
        double pointcoolDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and cooling and equipRef == \"" + p.getId() + "\"");

        Log.i("EachzoneData", "CurrentTemp:" + currentAverageTemp + " FloorName:" + floorName + " ZoneName:" + zoneTitle + "," + heatDeadband + "," + coolDeadband);
        seekArc.setData(false, (float) buildingLimitMin, (float)buildingLimitMax,
                        (float) heatUpperlimit, (float) heatLowerlimit, (float) coolLowerlimit,
                        (float) coolUpperlimit, (float) pointheatDT, (float) pointcoolDT,
                        (float) currentAverageTemp, (float) heatDeadband, (float) coolDeadband);

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

                    double pointheatDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and heating and equipRef == \"" + p.getId() + "\"");
                    double pointcoolDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and cooling and equipRef == \"" + p.getId() + "\"");
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
                            HashMap coolDT = CCUHsApi.getInstance().read("point and temp and desired and cooling and sp and equipRef == \"" + zoneEquip.getId() + "\"");
                            HashMap heatDT = CCUHsApi.getInstance().read("point and temp and desired and heating and sp and equipRef == \"" + zoneEquip.getId() + "\"");
                            HashMap avgDT = CCUHsApi.getInstance().read("point and temp and desired and average and sp and equipRef == \"" + zoneEquip.getId() + "\"");
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
                CcuLog.i("UI_PROFILING","ZoneFragmentNew.viewTemperatureBasedZone.SeekArc Onclick");

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
                                    if (viewTag.getGridID() == clickedView) {
                                        if (viewTag.getGridItem().equals("Temp") || viewTag.getGridItem().equals("Sense")) {
                                            SeekArc seekArcExpanded = (SeekArc) gridItem.findViewById(R.id.seekArc);
                                            TextView textViewzone = (TextView) gridItem.findViewById(R.id.textEquipment);
                                            textViewzone.setTextAppearance(getActivity(), R.style.label_black);
                                            textViewzone.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                            tableLayout.removeViewAt(row + 1);
                                            seekArcExpanded.setDetailedView(false);
                                            seekArcExpanded.setBackgroundColor(getResources().getColor(R.color.white));
                                            seekArcExpanded.scaletoNormal(250, 210);
                                            gridItem.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                        } else {
                                            TextView textViewzone = (TextView) gridItem.findViewById(R.id.textEquipment);
                                            textViewzone.setTextAppearance(getActivity(), R.style.label_black);
                                            textViewzone.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                            tableLayout.removeViewAt(row + 1);
                                            NonTempControl nonTempControl = gridItem.findViewById(R.id.rl_nontemp);
                                            ScaleControlToNormal(270,210,nonTempControl);
                                            nonTempControl.setExpand(false);
                                            //ScaleImageToNormal(250,210,imageViewExpanded);
                                            nonTempControl.setBackgroundColor(getResources().getColor(R.color.white));
                                            gridItem.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
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
                            status_view.setBackgroundColor(getActivity().getResources().getColor(R.color.zoneselection_gray));
                            int index = clickedView / columnCount + 1;
                            seekArc.setDetailedView(true);
                            seekArc.scaletoNormalBig(250, 210);
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
                        v.setBackgroundColor(getResources().getColor(R.color.white));
                        textEquipment.setTextAppearance(getActivity(), R.style.label_black);
                        textEquipment.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                        tableLayout.removeView(zoneDetails);
                        imageOn = false;
                        seekArc.setDetailedView(false);
                        seekArc.scaletoNormal(250, 210);
                        showWeather();
                        clickedView = -1;
                        isExpanded = false;
                    }
                } else {
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
                    seekArc.scaletoNormalBig(250, 210);
                    hideWeather();
                    imageOn = true;
                    selectedView = seekArc.getId();
                    try {
                        textEquipment.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
                        textEquipment.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        zoneDetails.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        tableLayout.addView(zoneDetails, index);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isExpanded = true;
                }

                if (isExpanded) {
                    v.setContentDescription(zoneTitle);
                    linearLayoutZonePoints.removeAllViews();
                    if (scheduleSpinner.getSelectedItemPosition() == 1) {
                        scheduleImageButton.setVisibility(View.VISIBLE);
                    } else {
                        scheduleImageButton.setVisibility(View.GONE);
                    }

                    String vacationStatus = ScheduleProcessJob.getVacationStateString(zoneId);
                    vacationStatusTV.setText(vacationStatus);
                    {
                        for (int k = 0; k < zoneMap.size(); k++) {
                            Equip p = new Equip.Builder().setHashMap(zoneMap.get(k)).build();
                            String updatedEquipId = p.getId();
                            equipOpen = new Equip.Builder().setHashMap(zoneMap.get(0)).build();
                            equipId[0] = equipOpen.getId();
                            mSchedule = Schedule.getScheduleByEquipId(equipId[0]);
                            if (p.getProfile().startsWith("DAB")) {
                                HashMap dabPoints = ScheduleProcessJob.getDABEquipPoints(p.getId());
                                Log.i("PointsValue", "DAB Points:" + dabPoints.toString());
                                loadDABPointsUI(dabPoints, inflater, linearLayoutZonePoints, p.getGroup());
                            }
                            if (p.getProfile().startsWith("VAV")) {
                                HashMap vavPoints = ScheduleProcessJob.getVAVEquipPoints(p.getId());
                                Log.i("PointsValue", "VAV Points:" + vavPoints.toString());
                                loadVAVPointsUI(vavPoints, inflater, linearLayoutZonePoints, p.getGroup());
                            }
                            if (p.getProfile().startsWith("SSE")) {
                                HashMap dabPoints = ScheduleProcessJob.getSSEEquipPoints(p.getId());
                                Log.i("PointsValue", "SSSE Points:" + dabPoints.toString());
                                loadSSEPointsUI(dabPoints, inflater, linearLayoutZonePoints, p.getGroup());
                            }
                            if (p.getProfile().startsWith("TEMP_INFLUENCE")) {
                                HashMap tiPoints = ScheduleProcessJob.getTIEquipPoints(p.getId());
                                Log.i("PointsValue", "TI Points:" + tiPoints.toString());
                                loadTIPointsUI(tiPoints, inflater, linearLayoutZonePoints, p.getGroup());
                            }
                            if (p.getProfile().startsWith("SMARTSTAT_TWO_PIPE_FCU")) {
                                HashMap p2FCUPoints = ScheduleProcessJob.get2PFCUEquipPoints(p.getId());
                                Log.i("PointsValue", "2PFCU Points:" + p2FCUPoints.toString());
                                loadSS2PFCUPointsUI(p2FCUPoints, inflater, linearLayoutZonePoints, updatedEquipId, false, p.getGroup());

                            }
                            if (p.getProfile().startsWith("SMARTSTAT_FOUR_PIPE_FCU")) {
                                HashMap p4FCUPoints = ScheduleProcessJob.get4PFCUEquipPoints(p.getId());
                                Log.i("PointsValue", "4PFCU Points:" + p4FCUPoints.toString());
                                loadSS4PFCUPointsUI(p4FCUPoints, inflater, linearLayoutZonePoints, updatedEquipId, false, p.getGroup());
                            }
                            if (p.getProfile().startsWith("SMARTSTAT_CONVENTIONAL_PACK_UNIT")) {
                                HashMap cpuEquipPoints = ScheduleProcessJob.getCPUEquipPoints(p.getId());
                                Log.i("PointsValue", "CPU Points:" + cpuEquipPoints.toString());
                                loadSSCPUPointsUI(cpuEquipPoints, inflater, linearLayoutZonePoints, updatedEquipId, false, p.getGroup(), false);
                                isCPUloaded = true;
                            }
                            if (p.getProfile().startsWith("SMARTSTAT_HEAT_PUMP_UNIT")) {
                                HashMap hpuEquipPoints = ScheduleProcessJob.getHPUEquipPoints(p.getId());
                                Log.i("PointsValue", "HPU Points:" + hpuEquipPoints.toString());
                                loadSSHPUPointsUI(hpuEquipPoints, inflater, linearLayoutZonePoints, updatedEquipId, false, p.getGroup(), false);
                                isHPUloaded = true;
                            }
                            if (p.getProfile().startsWith("DUAL_DUCT")) {
                                HashMap dualDuctPoints = DualDuctUtil.getEquipPointsForView(p.getId());
                                Log.i("PointsValue", "DualDuct Points:" + dualDuctPoints.toString());
                                loadDualDuctPointsUI(dualDuctPoints, inflater, linearLayoutZonePoints, p.getGroup());
                            }
                            if (p.getProfile().equalsIgnoreCase(ProfileType.BPOS.toString())) {
                                HashMap bpospoints = BPOSUtil.getbposPoints(p.getId());
                                Log.i("PointsValue", "BPOS Points:" + bpospoints.toString());
                                loadBPOSPointsUI(bpospoints, inflater, linearLayoutZonePoints, p.getGroup());
                            }

                            if (p.getProfile().startsWith(ProfileType.HYPERSTAT_VRV.name())) {
                                HyperStatVrvZoneViewKt.loadView(inflater, linearLayoutZonePoints,
                                                                updatedEquipId, CCUHsApi.getInstance(), getActivity(),
                                                                p.getGroup());
                            }

                            if (p.getProfile().startsWith(ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT.name())) {
                                HashMap cpuEquipPoints = ScheduleProcessJob.getHyperstatCPUEquipPoints(p);
                                Log.i("PointsValue", "CPU Points:" + cpuEquipPoints.toString());
                                loadHyperstatCpuProfile(cpuEquipPoints, inflater, linearLayoutZonePoints, updatedEquipId,  p.getGroup());
                                //isCPUloaded = true;
                            }
                        }
                    }
                }
            }
        });
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.viewTemperatureBasedZone Done");

    }


    private void clearTempOverride(String equipId) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ScheduleProcessJob.getSystemOccupancy() == Occupancy.UNOCCUPIED || ScheduleProcessJob.getSystemOccupancy() == Occupancy.FORCEDOCCUPIED) {
                    ScheduleProcessJob.clearTempOverrides(equipId);
                }
            }
        }, 400);
    }


    private void updateTemperatureBasedZones(SeekArc seekArcOpen, View zonePointsOpen, Equip equipOpen, LayoutInflater inflater) {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.updateTemperatureBasedZones");

        Equip p = equipOpen;
        View zoneDetails = zonePointsOpen;
        SeekArc seekArc = seekArcOpen;
        String equipId = p.getId();

        LinearLayout linearLayoutZonePoints = zoneDetails.findViewById(R.id.lt_profilepoints);
        TextView scheduleStatus = zoneDetails.findViewById(R.id.schedule_status_tv);
        Spinner scheduleSpinner = zoneDetails.findViewById(R.id.schedule_spinner);
        ImageButton scheduleImageButton = zoneDetails.findViewById(R.id.schedule_edit_button);
        ImageButton vacationImageButton = zoneDetails.findViewById(R.id.vacation_edit_button);
        TextView vacationStatusTV = zoneDetails.findViewById(R.id.vacation_status);


        ArrayAdapter<CharSequence> scheduleAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.schedule, R.layout.spinner_zone_item);
        scheduleAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        scheduleSpinner.setAdapter(scheduleAdapter);

        String zoneId = Schedule.getZoneIdByEquipId(equipId);
        Observable.fromCallable(() -> ScheduleProcessJob.getZoneStatusMessage(zoneId, p.getId()))
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(status -> scheduleStatus.setText(status));

        Observable.fromCallable(() -> ScheduleProcessJob.getVacationStateString(zoneId))
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(status -> vacationStatusTV.setText(status));

        String scheduleTypeId = getScheduleTypeId(equipId);
        final int mScheduleType = (int) CCUHsApi.getInstance().readPointPriorityVal(scheduleTypeId);
        Log.d("ScheduleType", "update mScheduleType==" + mScheduleType + "," + (int) CCUHsApi.getInstance().readPointPriorityVal(scheduleTypeId) + "," + p.getDisplayName());
        mScheduleTypeMap.put(equipId, mScheduleType);
        mSchedule = Schedule.getScheduleByEquipId(equipId);

        scheduleImageButton.setTag(mSchedule.getId());
        vacationImageButton.setTag(mSchedule.getId());
        vacationImageButton.setOnClickListener(v ->
        {
            SchedulerFragment schedulerFragment = SchedulerFragment.newInstance((String) v.getTag(), true, zoneId);
            FragmentManager childFragmentManager = getFragmentManager();
            childFragmentManager.beginTransaction();
            schedulerFragment.show(childFragmentManager, "dialog");

            schedulerFragment.setOnExitListener(() -> {
                Toast.makeText(v.getContext(), "Refresh View", Toast.LENGTH_LONG).show();
                mSchedule = Schedule.getScheduleByEquipId(equipId);
                ScheduleProcessJob.updateSchedules(equipOpen);
            });
        });
        scheduleImageButton.setOnClickListener(v ->
        {
            SchedulerFragment schedulerFragment = SchedulerFragment.newInstance((String) v.getTag(), false, zoneId);
            FragmentManager childFragmentManager = getFragmentManager();
            childFragmentManager.beginTransaction();
            schedulerFragment.show(childFragmentManager, "dialog");

            schedulerFragment.setOnExitListener(() -> {
                Toast.makeText(v.getContext(), "Refresh View", Toast.LENGTH_LONG).show();
                mSchedule = Schedule.getScheduleByEquipId(equipId);
                ScheduleProcessJob.updateSchedules(equipOpen);
            });
        });
        scheduleSpinner.setSelection(mScheduleType, false);
        if (mSchedule.isZoneSchedule()) {
            scheduleImageButton.setVisibility(View.VISIBLE);
        } else {
            scheduleImageButton.setVisibility(View.GONE);
        }
        
        scheduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0 && (mScheduleType != -1)) {
                    if (mSchedule.isZoneSchedule()) {
                        mSchedule.setDisabled(true);
                        CCUHsApi.getInstance().updateZoneSchedule(mSchedule, zoneId);
                    }
                    scheduleImageButton.setVisibility(View.GONE);

                    if (mScheduleTypeMap.get(equipId) != ScheduleType.BUILDING.ordinal()) {
                        setScheduleType(scheduleTypeId, ScheduleType.BUILDING, openZoneMap);
                        mScheduleTypeMap.put(equipId, ScheduleType.BUILDING.ordinal());
                    }

                    CCUHsApi.getInstance().scheduleSync();
                    scheduleImageButton.setTag(mSchedule.getId());
                    vacationImageButton.setTag(mSchedule.getId());
                } else if (position == 1 && (mScheduleType != -1)/*&& (mScheduleType != position)*/) {
                    clearTempOverride(equipId);
                    boolean isContainment = true;
                    if (mSchedule.isZoneSchedule() && mSchedule.getMarkers().contains("disabled")) {
                        mSchedule.setDisabled(false);
                        CCUHsApi.getInstance().updateZoneSchedule(mSchedule, zoneId);
                        scheduleImageButton.setTag(mSchedule.getId());
                        vacationImageButton.setTag(mSchedule.getId());
                    } else {

                        Zone zone = Schedule.getZoneforEquipId(equipId);
                        Schedule scheduleById = null;
                        if (zone.hasSchedule()) {
                            scheduleById = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
                            Log.d(L.TAG_CCU_UI, " scheduleType changed to ZoneSchedule : " + scheduleTypeId);
                            scheduleById.setDisabled(false);
                            isContainment = checkContainment(scheduleTypeId, scheduleById, scheduleSpinner, openZoneMap);
                            CCUHsApi.getInstance().updateZoneSchedule(scheduleById, zone.getId());
                        } else if (!zone.hasSchedule()) {
                            Log.d(L.TAG_CCU_UI, " Zone does not have Schedule : Shouldn't happen");
                            DefaultSchedules.setDefaultCoolingHeatingTemp();
                            zone.setScheduleRef(DefaultSchedules.generateDefaultSchedule(true, zone.getId()));
                            CCUHsApi.getInstance().updateZone(zone, zone.getId());
                            scheduleById = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
                            //CCUHsApi.getInstance().syncEntityTree();
                        }
                        scheduleImageButton.setTag(scheduleById.getId());
                        vacationImageButton.setTag(scheduleById.getId());
                        scheduleImageButton.setVisibility(View.VISIBLE);
                        CCUHsApi.getInstance().scheduleSync();
                    }
                    if (mScheduleTypeMap.get(equipId) != ScheduleType.ZONE.ordinal()) {
                        if (isContainment) {
                            setScheduleType(scheduleTypeId, ScheduleType.ZONE, openZoneMap);
                        }
                        mScheduleTypeMap.put(equipId, ScheduleType.ZONE.ordinal());
                    }
                } else if (position == 2 && (mScheduleType != -1)) {
                    scheduleImageButton.setVisibility(View.GONE);
                } else {
                    //list named schedules
                }
                mSchedule = Schedule.getScheduleByEquipId(equipId);
                scheduleImageButton.setTag(mSchedule.getId());
                vacationImageButton.setTag(mSchedule.getId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        double pointcurrTmep = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and equipRef == \"" + p.getId() + "\"");
        double pointbuildingMin = BuildingTunerCache.getInstance().getBuildingLimitMin();
        double pointbuildingMax = BuildingTunerCache.getInstance().getBuildingLimitMax();
        double pointcoolUL = BuildingTunerCache.getInstance().getMaxCoolingUserLimit();
        double pointheatUL = BuildingTunerCache.getInstance().getMaxHeatingUserLimit();
        double pointcoolLL = BuildingTunerCache.getInstance().getMinCoolingUserLimit();
        double pointheatLL = BuildingTunerCache.getInstance().getMinHeatingUserLimit();

        double pointheatDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and heating and equipRef == \"" + p.getId() + "\"");
        double pointcoolDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and cooling and equipRef == \"" + p.getId() + "\"");
        double pointheatDB = TunerUtil.getZoneHeatingDeadband(p.getRoomRef());
        double pointcoolDB = TunerUtil.getZoneCoolingDeadband(p.getRoomRef());

        if (!seekArc.isDetailedView()) {
            seekArc.setData(false, (float) pointbuildingMin, (float) pointbuildingMax, (float) pointheatUL, (float) pointheatLL, (float) pointcoolLL, (float) pointcoolUL, (float) pointheatDT, (float) pointcoolDT, (float) pointcurrTmep, (float) pointheatDB, (float) pointcoolDB);
        } else {
            seekArc.setData(true, (float) pointbuildingMin, (float) pointbuildingMax, (float) pointheatUL, (float) pointheatLL, (float) pointcoolLL, (float) pointcoolUL, (float) pointheatDT, (float) pointcoolDT, (float) pointcurrTmep, (float) pointheatDB, (float) pointcoolDB);
        }

        linearLayoutZonePoints.removeAllViews();
        for (int k = 0; k < openZoneMap.size(); k++) {
            Equip updatedEquip = new Equip.Builder().setHashMap(openZoneMap.get(k)).build();
            if (updatedEquip.getProfile().startsWith("DAB")) {
                HashMap dabPoints = ScheduleProcessJob.getDABEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "DAB Points:" + dabPoints.toString());
                loadDABPointsUI(dabPoints, inflater, linearLayoutZonePoints, updatedEquip.getGroup());
            }
            if (updatedEquip.getProfile().startsWith("VAV")) {
                HashMap vavPoints = ScheduleProcessJob.getVAVEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "VAV Points:" + vavPoints.toString());
                loadVAVPointsUI(vavPoints, inflater, linearLayoutZonePoints, updatedEquip.getGroup());
            }
            if (updatedEquip.getProfile().startsWith("SSE")) {
                HashMap ssePoints = ScheduleProcessJob.getSSEEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "SSE Points:" + ssePoints.toString());
                loadSSEPointsUI(ssePoints, inflater, linearLayoutZonePoints, updatedEquip.getGroup());
            }
            if (updatedEquip.getProfile().startsWith("TEMP_INFLUENCE")) {
                HashMap tiPoints = ScheduleProcessJob.getTIEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "TI Points:" + tiPoints.toString());
                loadTIPointsUI(tiPoints, inflater, linearLayoutZonePoints, updatedEquip.getGroup());
            }
            if (updatedEquip.getProfile().startsWith("SMARTSTAT_TWO_PIPE_FCU")) {
                HashMap p2FCUPoints = ScheduleProcessJob.get2PFCUEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "2PFCU Points:" + p2FCUPoints.toString());
                loadSS2PFCUPointsUI(p2FCUPoints, inflater, linearLayoutZonePoints, equipId, true, updatedEquip.getGroup());

            }
            if (updatedEquip.getProfile().startsWith("SMARTSTAT_FOUR_PIPE_FCU")) {
                HashMap p4FCUPoints = ScheduleProcessJob.get4PFCUEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "4PFCU Points:" + p4FCUPoints.toString());
                loadSS4PFCUPointsUI(p4FCUPoints, inflater, linearLayoutZonePoints, equipId, true, updatedEquip.getGroup());
            }
            if (updatedEquip.getProfile().startsWith("SMARTSTAT_CONVENTIONAL_PACK_UNIT")) {
                HashMap cpuEquipPoints = ScheduleProcessJob.getCPUEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "CPU Points:" + cpuEquipPoints.toString());
                loadSSCPUPointsUI(cpuEquipPoints, inflater, linearLayoutZonePoints, updatedEquip.getId(), true, updatedEquip.getGroup(), false);
                //isCPUloaded = true;
            }
            if (updatedEquip.getProfile().startsWith("SMARTSTAT_HEAT_PUMP_UNIT")) {
                HashMap hpuEquipPoints = ScheduleProcessJob.getHPUEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "HPU Points:" + hpuEquipPoints.toString());
                loadSSHPUPointsUI(hpuEquipPoints, inflater, linearLayoutZonePoints, updatedEquip.getId(), true, updatedEquip.getGroup(), false);
                //isHPUloaded = true;
            }
            if (updatedEquip.getProfile().startsWith("DUAL_DUCT")) {
                HashMap dualDuctPoints = DualDuctUtil.getEquipPointsForView(updatedEquip.getId());
                Log.i("PointsValue", "DUAL_DUCT Points:" + dualDuctPoints.toString());
                loadDualDuctPointsUI(dualDuctPoints, inflater, linearLayoutZonePoints, updatedEquip.getGroup());
            }
            if (updatedEquip.getProfile().equalsIgnoreCase(ProfileType.BPOS.toString())) {
                HashMap bposPoints = BPOSUtil.getbposPoints(updatedEquip.getId());
                Log.i("PointsValue", "BPOS Points:" + bposPoints.toString());
                loadBPOSPointsUI(bposPoints, inflater, linearLayoutZonePoints, updatedEquip.getGroup());
            }
            if (updatedEquip.getProfile().startsWith(ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT.name())) {
                HashMap cpuEquipPoints = ScheduleProcessJob.getHyperstatCPUEquipPoints(updatedEquip);
                Log.i("PointsValue", "CPU Points:" + cpuEquipPoints.toString());
                loadHyperstatCpuProfile(cpuEquipPoints, inflater, linearLayoutZonePoints, updatedEquip.getId(), updatedEquip.getGroup());

            }
            if (updatedEquip.getProfile().startsWith(ProfileType.HYPERSTAT_VRV.name())) {
                HyperStatVrvZoneViewKt.loadView(inflater, linearLayoutZonePoints,
                                                updatedEquip.getId(), CCUHsApi.getInstance(), getActivity(),
                                                p.getGroup());
            }
        }
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.updateTemperatureBasedZones Done");

    }


    private void updateNonTemperatureBasedZones(NonTempControl nonTempControlOpen, View zonePointsOpen, Equip equipOpen, LayoutInflater inflater) {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.updateNonTemperatureBasedZones");

        Equip p = equipOpen;
        View zoneDetails = zonePointsOpen;
        LinearLayout linearLayoutZonePoints = zoneDetails.findViewById(R.id.lt_profilepoints);
        NonTempControl nonTempControl = nonTempControlOpen;

        linearLayoutZonePoints.removeAllViews();
        if (p != null) {
            if (p.getProfile().startsWith("EMR")) {
                LinearLayout ll_status = zoneDetails.findViewById(R.id.lt_status);
                LinearLayout ll_schedule = zoneDetails.findViewById(R.id.lt_schedule);
                ll_status.setVisibility(View.GONE);
                ll_schedule.setVisibility(View.GONE);
                HashMap emPoints = ScheduleProcessJob.getEMEquipPoints(p.getId());
                Log.i("PointsValue", "EM Points:" + emPoints.toString());
                if (emPoints.size() > 0) {
                    loadEMPointsUI(emPoints, inflater, linearLayoutZonePoints, p.getGroup());
                    double totalEm = (double) emPoints.get("Energy Reading");
                    double currentEm = (double) emPoints.get("Current Rate");
                    int currentValue = new BigDecimal(currentEm).intValue();
                    nonTempControl.setEmCurrentText(String.valueOf(currentValue));
                    nonTempControl.setEmTotalText(String.format("%.0f", totalEm));
                    nonTempControl.setEmCurrentText(String.valueOf(currentValue));
                    nonTempControl.setEmTotalUnitText("KWh");
                    nonTempControl.setEmCurrentUnitText("KW");
                }
            }
            if (p.getProfile().startsWith("PLC")) {
                LinearLayout ll_status = zoneDetails.findViewById(R.id.lt_status);
                LinearLayout ll_schedule = zoneDetails.findViewById(R.id.lt_schedule);
                ll_status.setVisibility(View.GONE);
                ll_schedule.setVisibility(View.GONE);
                HashMap plcPoints = ScheduleProcessJob.getPiEquipPoints(p.getId());
                if (plcPoints.size() > 0) {
                    Log.i("PointsValue", "PiLoop Points:" + plcPoints.toString());
                    loadPLCPointsUI(plcPoints, inflater, linearLayoutZonePoints, p.getGroup());
                    double targetValue = (double) plcPoints.get("Target Value");
                    double inputValue = (double) plcPoints.get("Input Value");
                    nonTempControl.setPiInputText(String.format("%.2f", inputValue));
                    nonTempControl.setPiOutputText(String.valueOf(targetValue));
                    nonTempControl.setPiInputUnitText(plcPoints.get("Unit").toString());
                    if ((boolean) plcPoints.get("Dynamic Setpoint")) {
                        nonTempControl.setPiOutputUnitText(plcPoints.get("Dynamic Unit").toString());
                    } else {
                        nonTempControl.setPiOutputUnitText(plcPoints.get("Unit").toString());
                    }
                }
            }
        }
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.updateNonTemperatureBasedZone Done");

    }


    private void viewNonTemperatureBasedZone(LayoutInflater inflater, View rootView, ArrayList<HashMap> zoneMap,String zoneTitle, int gridPosition, LinearLayout[] tablerowLayout, boolean isZoneAlive)
    {
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.viewNonTemperatureBasedZone");


        Equip p = null;
        int i = gridPosition;
        View arcView = null;
        arcView = inflater.inflate(R.layout.zones_item_nontemp, (ViewGroup) rootView, false);
        View zoneDetails = inflater.inflate(R.layout.zones_item_details, null);
        LinearLayout linearLayoutZonePoints = zoneDetails.findViewById(R.id.lt_profilepoints);
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
        if (zoneMap.size() > 0) {
            Log.i("ProfileTypes", "Points:" + zoneMap.toString());
            p = new Equip.Builder().setHashMap(zoneMap.get(0)).build();

            String equipId = p.getId();
            HashMap zoneEquips = zoneMap.get(0);
            if ((zoneEquips.get("profile").toString()).contains("PLC")) {
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
                nonTempControl.setEquipType(0);
                nonTempControl.setImage(R.drawable.ic_zone_em);
                nonTempControl.setImageViewExpanded(R.drawable.ic_zone_em_max);
            }
            if ((zoneEquips.get("profile").toString()).contains("MODBUS")) {
                nonTempControl.setEquipType(2);
                nonTempControl.setImage(R.drawable.ic_zone_modbus);
                nonTempControl.setImageViewExpanded(R.drawable.ic_zone_modbus_mx);
            }
            if ((zoneEquips.get("profile").toString()).contains("MODBUS") && (zoneEquips.get("profile").toString()).contains("EMR")) {
                nonTempControl.setEquipType(2);
                nonTempControl.setImage(R.drawable.ic_zone_em);
                nonTempControl.setImageViewExpanded(R.drawable.ic_zone_em_max);
            }

        } else {
            //No devices paired zone
            nonTempControl.setEquipType(2);
            nonTempControl.setImage(R.drawable.ic_no_device_paired_icon);
            nonTempControl.setImageViewExpanded(R.drawable.ic_no_device_paired_expanded_icon);
        }
        //ScaleImageToNormal(250,210,imageView);
        ScaleControlToNormal(270,210,nonTempControl);
        nonTempControl.setExpand(false);

        //imageView.setOnClickListener(new View.OnClickListener() {
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
                                    if (viewTag.getGridID() == clickedView) {
                                        if (viewTag.getGridItem().equals("NonTemp")) {
                                            TextView textViewzone = (TextView) gridItem.findViewById(R.id.textEquipment);
                                            textViewzone.setTextAppearance(getActivity(), R.style.label_black);
                                            textViewzone.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                            tableLayout.removeViewAt(row + 1);
                                            //ImageView imageViewExpanded = gridItem.findViewById(R.id.imageView);
                                            NonTempControl nonTempControl = gridItem.findViewById(R.id.rl_nontemp);
                                            //ScaleImageToNormal(250,210,imageViewExpanded);
                                            ScaleControlToNormal(270,210,nonTempControl);
                                            nonTempControl.setExpand(false);
                                            nonTempControl.setBackgroundColor(getResources().getColor(R.color.white));
                                            gridItem.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                            gridItem.invalidate();
                                        } else {
                                            SeekArc seekArcExpanded = (SeekArc) gridItem.findViewById(R.id.seekArc);
                                            TextView textViewzone = (TextView) gridItem.findViewById(R.id.textEquipment);
                                            textViewzone.setTextAppearance(getActivity(), R.style.label_black);
                                            textViewzone.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                            tableLayout.removeViewAt(row + 1);
                                            seekArcExpanded.setDetailedView(false);
                                            seekArcExpanded.setBackgroundColor(getResources().getColor(R.color.white));
                                            seekArcExpanded.scaletoNormal(250, 210);
                                            gridItem.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                        }
                                        isExpanded = false;
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
                                textEquipment.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
                                textEquipment.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                                zoneDetails.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                                tableLayout.addView(zoneDetails, index);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (clickposition == clickedView) {
                        v.setBackgroundColor(getResources().getColor(R.color.white));
                        status_view.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                        textEquipment.setTextAppearance(getActivity(),R.style.label_black);
                        textEquipment.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                        tableLayout.removeView(zoneDetails);
                        imageOn = false;
                        //ScaleImageToNormal(250,210,imageView);
                        ScaleControlToNormal(270,210,nonTempControl);
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
                    //ScaleImageToBig(250,210,imageView);
                    ScaleControlToExpand(250, 210, nonTempControl);
                    nonTempControl.setExpand(true);
                    nonTempControlOpen = nonTempControl;
                    hideWeather();
                    imageOn = true;
                    isExpanded = true;
                    try {
                        textEquipment.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
                        textEquipment.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        zoneDetails.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        tableLayout.addView(zoneDetails, index);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (isExpanded) {
                    linearLayoutZonePoints.removeAllViews();
                    if (nonTempEquip != null) {
                        if (nonTempEquip.getProfile().startsWith("EMR")) {

                            nonTempControl.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, nonTempControl.getHeight()));
                            nonTempControl.invalidate();

                            LinearLayout ll_status = zoneDetails.findViewById(R.id.lt_status);
                            LinearLayout ll_schedule = zoneDetails.findViewById(R.id.lt_schedule);
                            ll_status.setVisibility(View.GONE);
                            ll_schedule.setVisibility(View.GONE);
                            HashMap emPoints = ScheduleProcessJob.getEMEquipPoints(nonTempEquip.getId());
                            Log.i("PointsValue", "EM Points:" + emPoints.toString());
                            loadEMPointsUI(emPoints, inflater, linearLayoutZonePoints, nonTempEquip.getGroup());

                            double energyRead = (double) emPoints.get("Energy Reading");
                            double currentRead = (double) emPoints.get("Current Rate");
                            int currentValue = new BigDecimal(currentRead).intValue();
                            nonTempControl.setEmTotalText(String.format("%.0f", energyRead));
                            nonTempControl.setEmCurrentText(String.valueOf(currentValue));
                            //nonTempControl.setEmTotalText(emPoints.get("Energy Reading").toString());
                            nonTempControl.setEmTotalUnitText("KWh");
                            //nonTempControl.setEmCurrentText(emPoints.get("Current Rate").toString());
                            nonTempControl.setEmCurrentUnitText("KW");

                        }
                        if (nonTempEquip.getProfile().startsWith("PLC")) {
                            LinearLayout ll_status = zoneDetails.findViewById(R.id.lt_status);
                            LinearLayout ll_schedule = zoneDetails.findViewById(R.id.lt_schedule);
                            ll_status.setVisibility(View.GONE);
                            ll_schedule.setVisibility(View.GONE);
                            HashMap plcPoints = ScheduleProcessJob.getPiEquipPoints(nonTempEquip.getId());
                            Log.i("PointsValue", "PiLoop Points:" + plcPoints.toString());
                            loadPLCPointsUI(plcPoints, inflater, linearLayoutZonePoints, nonTempEquip.getGroup());

                            double targetValue = (double) plcPoints.get("Target Value");
                            double inputValue = (double) plcPoints.get("Input Value");
                            nonTempControl.setPiInputText(String.format("%.2f", inputValue));
                            nonTempControl.setPiOutputText(String.valueOf(targetValue));
                            nonTempControl.setPiInputUnitText(plcPoints.get("Unit").toString());
                            if ((boolean) plcPoints.get("Dynamic Setpoint")) {
                                nonTempControl.setPiOutputUnitText(plcPoints.get("Dynamic Unit").toString());
                            } else {
                                nonTempControl.setPiOutputUnitText(plcPoints.get("Unit").toString());
                            }
                        }
                        if (nonTempEquip.getProfile().startsWith("MODBUS")) {

                            LinearLayout ll_status = zoneDetails.findViewById(R.id.lt_status);
                            LinearLayout ll_schedule = zoneDetails.findViewById(R.id.lt_schedule);
                            ll_status.setVisibility(View.GONE);
                            ll_schedule.setVisibility(View.GONE);

                            List<EquipmentDevice> modbusDevices = EquipsManager.getInstance().getAllMbEquips(nonTempEquip.getRoomRef());

                            Log.i("MODBUS_UI", "ZoneData:" + modbusDevices);

                            for (int i = 0; i < modbusDevices.size(); i++) {
                                List<Parameter> parameterList = new ArrayList<>();
                                if (Objects.nonNull(modbusDevices.get(i).getRegisters())) {
                                    for (Register registerTemp : modbusDevices.get(i).getRegisters()) {
                                        if (registerTemp.getParameters() != null) {
                                            for (Parameter p : registerTemp.getParameters()) {
                                                if (p.isDisplayInUI()) {
                                                    p.setParameterDefinitionType(registerTemp.getParameterDefinitionType());
                                                    parameterList.add(p);
                                                }
                                            }
                                        }
                                    }
                                }
                                View zoneDetails = inflater.inflate(R.layout.item_modbus_detail_view, null);

                                RecyclerView modbusParams = zoneDetails.findViewById(R.id.recyclerParams);
                                TextView tvEquipmentType = zoneDetails.findViewById(R.id.tvEquipmentType);
                                String nodeAddress =  String.valueOf(modbusDevices.get(i).getSlaveId());
                                tvEquipmentType.setText(modbusDevices.get(i).getEquipType()+ "("+modbusDevices.get(i).getSlaveId()+")");
                                TextView textViewModule = zoneDetails.findViewById(R.id.module_status);
                                HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
                                TextView textViewUpdatedTime = zoneDetails.findViewById(R.id.last_updated_status);
                                textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
                                GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(),2);
                                modbusParams.setLayoutManager(gridLayoutManager);
                                ZoneRecyclerModbusParamAdapter zoneRecyclerModbusParamAdapter = new ZoneRecyclerModbusParamAdapter(getContext(),modbusDevices.get(i).getEquipRef(),parameterList);
                                modbusParams.setAdapter(zoneRecyclerModbusParamAdapter);
                                modbusParams.invalidate();
                                linearLayoutZonePoints.addView(zoneDetails);
                                linearLayoutZonePoints.setPadding(0, 0, 0, 20);
                            }
                        }
                    } else {
                        //Non paired devices
                        LinearLayout ll_status = zoneDetails.findViewById(R.id.lt_status);
                        LinearLayout ll_schedule = zoneDetails.findViewById(R.id.lt_schedule);
                        ll_status.setVisibility(View.GONE);
                        ll_schedule.setVisibility(View.GONE);
                        loadNoDevicesPairedUI(inflater, linearLayoutZonePoints);
                    }
                }
            }
        });
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.viewNonTemperatureBasedZone Done");

    }

    public void loadVAVPointsUI(HashMap vavPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String nodeAddress) {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);
        View viewPointRow2 = inflater.inflate(R.layout.zones_item_type1, null);

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
        textViewLabel2.setText("Reheat Coil : ");
        textViewValue2.setText(vavPoints.get("Reheat Coil").toString());
        textViewLabel3.setText("Discharge Airflow : ");
        textViewValue3.setText(vavPoints.get("Discharge Airflow").toString());
        textViewLabel4.setText("Supply Airflow : ");
        textViewValue4.setText(vavPoints.get("Entering Airflow").toString());

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        linearLayoutZonePoints.addView(viewPointRow1);
        viewPointRow2.setPadding(0, 0, 0, 40);
        linearLayoutZonePoints.addView(viewPointRow2);

    }

    public void loadSSEPointsUI(HashMap ssePoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String nodeAddress) {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);
        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);
        textViewLabel2.setVisibility(View.GONE);
        TextView textViewValue1 = viewPointRow1.findViewById(R.id.text_point1value);
        TextView textViewValue2 = viewPointRow1.findViewById(R.id.text_point2value);
        textViewValue2.setVisibility(View.GONE);

        textViewTitle.setText(ssePoints.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(ssePoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        textViewLabel1.setText("Discharge Airflow : ");
        textViewValue1.setText(ssePoints.get("Discharge Airflow").toString());

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        viewPointRow1.setPadding(0, 0, 0, 40);
        linearLayoutZonePoints.addView(viewPointRow1);
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
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);
        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);
        TextView textViewValue1 = viewPointRow1.findViewById(R.id.text_point1value);
        TextView textViewValue2 = viewPointRow1.findViewById(R.id.text_point2value);

        textViewTitle.setText(dabPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(dabPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        textViewLabel1.setText("Damper : ");
        textViewLabel2.setText("Discharge Airflow : ");
        textViewValue1.setText(dabPoints.get("Damper").toString());
        textViewValue2.setText(dabPoints.get("Discharge Airflow").toString());

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        viewPointRow1.setPadding(0, 0, 0, 40);
        linearLayoutZonePoints.addView(viewPointRow1);
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

        textViewTitle.setText(dualDuctPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(dualDuctPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        if (dualDuctPoints.containsKey("CoolingSupplyAirflow") ) {
            textViewLabel1.setText("Cooling Supply Airflow : ");
            textViewValue1.setText(dualDuctPoints.get("CoolingSupplyAirflow").toString());
        } else if (dualDuctPoints.containsKey("HeatingSupplyAirflow")) {
            textViewLabel1.setText("Heating Supply Airflow : ");
            textViewValue1.setText(dualDuctPoints.get("HeatingSupplyAirflow").toString());
        }

        textViewLabel2.setText("Discharge Airflow : ");
        textViewValue2.setText(dualDuctPoints.get("DischargeAirflow").toString());

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
        textAirflowValue.setText(cpuEquipPoints.get("Discharge Airflow").toString());


        int conditionMode = 0;
        int fanMode = 0;
        try {
            conditionMode = (int) ((double) cpuEquipPoints.get("Conditioning Mode"));
            fanMode = (int) ((double) cpuEquipPoints.get("Fan Mode"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayAdapter<CharSequence> conModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_conditionmode, R.layout.spinner_zone_item);

        if (cpuEquipPoints.containsKey("condEnabled")) {
            if (cpuEquipPoints.get("condEnabled").toString().contains("Cool Only")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_coolonly,
                        R.layout.spinner_zone_item);
                if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1;
                }
            } else if (cpuEquipPoints.get("condEnabled").toString().contains("Heat Only")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_heatonly,
                        R.layout.spinner_zone_item);
                if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1;
                }
            }
            if (cpuEquipPoints.get("condEnabled").toString().contains("Off")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_off, R.layout.spinner_zone_item);
                conditionMode = 0;
            }

        }
        conModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        spinnerValue1.setAdapter(conModeAdapter);

        ArrayAdapter<CharSequence> fanModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_fanmode, R.layout.spinner_zone_item);
        if (cpuEquipPoints.containsKey("fanEnabled")) {
            if (cpuEquipPoints.get("fanEnabled").toString().contains("No High Fan")) {
                fanModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_fanmode_low, R.layout.spinner_zone_item);
                if (fanMode > fanModeAdapter.getCount()) {
                    fanMode = StandaloneFanStage.AUTO.ordinal();//Fallback to Auto if an invalid configuration is set.
                }
            } else if (cpuEquipPoints.get("fanEnabled").toString().contains("No Fan")) {
                fanModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_fanmode_off, R.layout.spinner_zone_item);
                fanMode = 0;
            }
        }
        fanModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        spinnerValue2.setAdapter(fanModeAdapter);

        textViewTitle.setText(cpuEquipPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(cpuEquipPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        textViewLabel1.setText("Conditioning Mode : ");
        textViewLabel2.setText("Fan Mode : ");

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


            ArrayList<Integer> arrayHumdityTargetList = new ArrayList<Integer>();
            for (int pos = 1; pos <= 100; pos++)
                arrayHumdityTargetList.add(pos);

            ArrayAdapter<Integer> humidityTargetAdapter = new ArrayAdapter<Integer>(getActivity(), R.layout.spinner_zone_item, arrayHumdityTargetList);
            humidityTargetAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
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
                                    Log.i("PubNub", "humidityValue:" + targetHumidValue + " position:" + position);
                                    StandaloneScheduler.updateOperationalPoints(equipId, "target and humidity", position + 1);
                                }
                            } else if (fanHighHumdOption == 3.0) {
                                if (targetDehumidValue != (position + 1)) {
                                    Log.i("PubNub", "DehumidityValue:" + targetDehumidValue + " position:" + position);
                                    StandaloneScheduler.updateOperationalPoints(equipId, "target and dehumidifier", position + 1);
                                }
                            }
                        } else {
                            if (fanHighHumdOption == 2.0)
                                StandaloneScheduler.updateOperationalPoints(equipId, "target and humidity", position + 1);
                            else if (fanHighHumdOption == 3.0)
                                StandaloneScheduler.updateOperationalPoints(equipId, "target and dehumidifier", position + 1);
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
                if (isCPUFromPubNub) {
                    if (tempConditionMode != position) {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "temp and conditioning and mode", position);
                    }
                    //isCPUFromPubNub = false;
                } else {
                    //if(isCPUloaded) {
                    StandaloneScheduler.updateOperationalPoints(tempEquipId, "temp and conditioning and mode", position);
                    //}
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerValue2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isCPUFromPubNub) {
                    if (tempFanMode != position) {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                        if ((position != 0) && (position % 3 == 0))
                            Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().putInt(tempEquipId, position).apply();
                        else
                            Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().remove(tempEquipId).commit();
                    }
                    isCPUFromPubNub = false;
                } else {
                    StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                    if ((position != 0) && (position % 3 == 0)) //Save only Fan occupied period mode alone, else no need.
                        Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().putInt(tempEquipId, position).apply();
                    else
                        Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().remove(tempEquipId).commit();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //isCPUloaded = false;

    }

    public void loadSSHPUPointsUI(HashMap hpuEquipPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String equipId, boolean isPubNub, String nodeAddress, boolean isLoaded) {
        //Log.i("PubNub","UI Update for Zone Points");
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
        textAirflowValue.setText(hpuEquipPoints.get("Discharge Airflow").toString());

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
        ArrayAdapter<CharSequence> conModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_conditionmode, R.layout.spinner_zone_item);
        if (hpuEquipPoints.containsKey("condEnabled")) {
            if (hpuEquipPoints.get("condEnabled").toString().contains("Cool Only")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_coolonly,
                        R.layout.spinner_zone_item);
                if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1;
                }
            } else if (hpuEquipPoints.get("condEnabled").toString().contains("Heat Only")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_heatonly,
                        R.layout.spinner_zone_item);
                if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1; //Select the last item.
                }
            } else if (hpuEquipPoints.get("condEnabled").toString().contains("Off")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_off, R.layout.spinner_zone_item);
                conditionMode = 0;
            }

        }
        conModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        conditionSpinner.setAdapter(conModeAdapter);
        ArrayAdapter<CharSequence> fanModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_fanmode, R.layout.spinner_zone_item);

        if (hpuEquipPoints.containsKey("fanEnabled")) {
            if (hpuEquipPoints.get("fanEnabled").toString().contains("No High Fan")) {
                fanModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_fanmode_low, R.layout.spinner_zone_item);
                if (fanMode > fanModeAdapter.getCount()) {
                    fanMode = StandaloneFanStage.AUTO.ordinal();//Fallback to Auto if an invalid configuration is set.
                }
            } else if (hpuEquipPoints.get("fanEnabled").toString().contains("No Fan")) {
                fanModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_fanmode_off, R.layout.spinner_zone_item);
                fanMode = 0;
            }
        }
        fanModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        fanSpinner.setAdapter(fanModeAdapter);

        conditionSpinner.setSelection(conditionMode, false);
        fanSpinner.setSelection(fanMode, false);

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);

        double fanHighHumdOption = (double) hpuEquipPoints.get("Fan High Humidity");
        //Log.e("fanHighHumdOption","insideZoneFragmentNew "+fanHighHumdOption);
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


            ArrayList<Integer> arrayHumdityTargetList = new ArrayList<Integer>();
            for (int pos = 1; pos <= 100; pos++)
                arrayHumdityTargetList.add(pos);

            ArrayAdapter<Integer> humidityTargetAdapter = new ArrayAdapter<Integer>(getActivity(), R.layout.spinner_zone_item, arrayHumdityTargetList);
            humidityTargetAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
            humiditySpinner.setAdapter(humidityTargetAdapter);

            if (HeatPumpUnitConfiguration.enableRelay5) {
                if (fanHighHumdOption == 2.0) {
                    textViewLabel3.setText("Dynamic Target Humidity : ");
                    targetHumidity = (double) hpuEquipPoints.get("Target Humidity");
                    Log.e("targetHumidity", "insideZoneFragment" + targetHumidity);
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
                                    Log.i("PubNub", "humidityValue:" + targetHumidValue + " position:" + position + 1);
                                    StandaloneScheduler.updateOperationalPoints(equipId, "target and humidity", position + 1);
                                }
                            } else if (fanHighHumdOption == 3.0) {
                                if (targetDehumidValue != (position + 1)) {
                                    Log.i("PubNub", "DehumidityValue:" + targetDehumidValue + " position:" + position + 1);
                                    StandaloneScheduler.updateOperationalPoints(equipId, "target and dehumidifier", position + 1);
                                }
                            }
                        } else {
                            if (fanHighHumdOption == 2.0)
                                StandaloneScheduler.updateOperationalPoints(equipId, "target and humidity", position + 1);
                            else if (fanHighHumdOption == 3.0)
                                StandaloneScheduler.updateOperationalPoints(equipId, "target and dehumidifier", position + 1);
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
                            Log.i("PubNub", "conditionMode:" + tempConditionMode + " position:" + position);
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

        fanSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    if (isHPUFromPubNub) {
                        if (tempfanMode != position) {
                            Log.i("PubNub", "fanMode:" + tempfanMode + " position:" + position);
                            StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                            if ((position != 0) && (position % 3 == 0))
                                Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().putInt(tempEquipId, position).apply();
                            else
                                Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().remove(tempEquipId).commit();
                        }
                        isHPUFromPubNub = false;
                    } else {
                        //if(isHPUloaded) {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                        if ((position != 0) && (position % 3 == 0))
                            Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().putInt(tempEquipId, position).apply();
                        else
                            Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().remove(tempEquipId).commit();
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

        textAirflowValue.setText(p2FCUPoints.get("Discharge Airflow").toString());

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

        ArrayAdapter<CharSequence> conModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_conditionmode, R.layout.spinner_zone_item);

        if (p2FCUPoints.containsKey("condEnabled")) {
            if (p2FCUPoints.get("condEnabled").toString().contains("Cool Only")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_coolonly,
                        R.layout.spinner_zone_item);
                if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1; //Select the last item.
                }
            } else if (p2FCUPoints.get("condEnabled").toString().contains("Heat Only")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_heatonly,
                        R.layout.spinner_zone_item);
                if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1; //Select the last item
                }
            }
            if (p2FCUPoints.get("condEnabled").toString().contains("Off")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_off, R.layout.spinner_zone_item);
                conditionMode = 0;
            }

        }
        conModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        spinnerValue1.setAdapter(conModeAdapter);
        ArrayAdapter<CharSequence> fanModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_2pfcu_fanmode, R.layout.spinner_zone_item);
        if (p2FCUPoints.containsKey("fanEnabled")) {
            if (p2FCUPoints.get("fanEnabled").toString().contains("No High Fan")) {
                fanModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_2pfcu_fanmode_medium,
                        R.layout.spinner_zone_item);
                if (fanMode > fanModeAdapter.getCount()) {
                    fanMode = StandaloneFanStage.AUTO.ordinal();//Fallback to Auto if an invalid configuration is set.
                }
            } else if (p2FCUPoints.get("fanEnabled").toString().contains("No Medium High Fan")) {
                fanModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_2pfcu_fanmode_low, R.layout.spinner_zone_item);
                if (fanMode > fanModeAdapter.getCount()) {
                    fanMode = StandaloneFanStage.AUTO.ordinal();//Fallback to Auto if an invalid configuration is set.
                }
            } else if (p2FCUPoints.get("fanEnabled").toString().contains("No Fan")) {
                fanModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_2pfcu_fanmode_off, R.layout.spinner_zone_item);
                fanMode = 0;
            }
        }
        fanModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        spinnerValue2.setAdapter(fanModeAdapter);

        textViewTitle.setText(p2FCUPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(p2FCUPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        textViewLabel1.setText("Conditioning Mode : ");
        textViewLabel2.setText("Fan Mode : ");

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
        spinnerValue2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isFromPubNub) {
                    if (tempfanMode != position) {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                        if ((position != 0) && (position % 3 == 0))
                            Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().putInt(tempEquipId, position).apply();
                        else
                            Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().remove(tempEquipId).commit();
                    }
                    isFromPubNub = false;
                } else {
                    StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                    if ((position != 0) && (position % 3 == 0))
                        Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().putInt(tempEquipId, position).apply();
                    else
                        Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().remove(tempEquipId).commit();
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

        textAirflowValue.setText(p4FCUPoints.get("Discharge Airflow").toString());


        int conditionMode = 0;
        int fanMode = 0;
        try {
            conditionMode = (int) ((double) p4FCUPoints.get("Conditioning Mode"));
            fanMode = (int) ((double) p4FCUPoints.get("Fan Mode"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayAdapter<CharSequence> conModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_conditionmode, R.layout.spinner_zone_item);
        if (p4FCUPoints.containsKey("condEnabled")) {
            if (p4FCUPoints.get("condEnabled").toString().contains("Cool Only")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_coolonly,
                        R.layout.spinner_zone_item);
                if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1; //Select the last item.
                }
            } else if (p4FCUPoints.get("condEnabled").toString().contains("Heat Only")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_heatonly,
                        R.layout.spinner_zone_item);
                if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1; //Select the last item.
                }
            }
            if (p4FCUPoints.get("condEnabled").toString().contains("Off")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_off, R.layout.spinner_zone_item);
                conditionMode = 0;
            }

        }
        conModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        spinnerValue1.setAdapter(conModeAdapter);

        ArrayAdapter<CharSequence> fanModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_2pfcu_fanmode, R.layout.spinner_zone_item);

        if (p4FCUPoints.containsKey("fanEnabled")) {
            if (p4FCUPoints.get("fanEnabled").toString().contains("No High Fan")) {
                fanModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_2pfcu_fanmode_medium,
                        R.layout.spinner_zone_item);
                if (fanMode > fanModeAdapter.getCount()) {
                    fanMode = StandaloneFanStage.AUTO.ordinal();//Fallback to Auto if an invalid configuration is set.
                }
            } else if (p4FCUPoints.get("fanEnabled").toString().contains("No Medium High Fan")) {
                fanModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_2pfcu_fanmode_low, R.layout.spinner_zone_item);
                if (fanMode > fanModeAdapter.getCount()) {
                    fanMode = StandaloneFanStage.AUTO.ordinal();//Fallback to Auto if an invalid configuration is set.
                }
            } else if (p4FCUPoints.get("fanEnabled").toString().contains("No Fan")) {
                fanModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_2pfcu_fanmode_off, R.layout.spinner_zone_item);
                fanMode = 0;
            }
        }
        fanModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        spinnerValue2.setAdapter(fanModeAdapter);

        textViewTitle.setText(p4FCUPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(p4FCUPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        textViewLabel1.setText("Conditioning Mode : ");
        textViewLabel2.setText("Fan Mode : ");

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
        spinnerValue1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isFromPubNub) {
                    if (tempConditionMode != position) {
                        StandaloneScheduler.updateOperationalPoints(equipId, "temp and conditioning and mode", position);
                    }
                    isFromPubNub = false;
                } else {
                    StandaloneScheduler.updateOperationalPoints(equipId, "temp and conditioning and mode", position);
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
                            Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().putInt(equipId, position).apply();
                        else
                            Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().remove(equipId).commit();
                    }
                    isFromPubNub = false;
                } else {
                    StandaloneScheduler.updateOperationalPoints(equipId, "fan and operation and mode", position);
                    if ((position != 0) && (position % 3 == 0))
                        Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().putInt(equipId, position).apply();
                    else
                        Globals.getInstance().getApplicationContext().getSharedPreferences("ss_fan_op_mode", Context.MODE_PRIVATE).edit().remove(equipId).commit();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    public void loadEMPointsUI(HashMap EmrPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String nodeAddress) {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);

        textViewTitle.setText(EmrPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(EmrPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        viewTitle.setPadding(40,20,0,0);
        viewStatus.setPadding(0, 0, 0, 40);
        try {
            linearLayoutZonePoints.addView(viewTitle);
            linearLayoutZonePoints.addView(viewStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        textViewStatus.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL);
        textViewStatus.setText(Html.fromHtml("<b>No device currently Paired</b> <br>Please go to the floor planner on settings page to pair a new device</br>"));
        viewStatus.setPadding(0, 0, 0, 40);
        try {
            linearLayoutZonePoints.addView(viewStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadPLCPointsUI(HashMap plcPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String nodeAddress) {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);
        View viewPointRow2 = inflater.inflate(R.layout.zones_item_type1, null);

        View loopOpRow = inflater.inflate(R.layout.zones_item_type1, null);
        TextView loopOpRowLabel = loopOpRow.findViewById(R.id.text_point1label);
        TextView loopOpRowValue = loopOpRow.findViewById(R.id.text_point1value);
        loopOpRowLabel.setText("Loop Output : ");
        loopOpRowValue.setText(plcPoints.get("LoopOutput") + "%");

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);

        TextView labelInputAir = viewPointRow1.findViewById(R.id.text_point1label);
        TextView labelTarget = viewPointRow1.findViewById(R.id.text_point2label);
        TextView labelOffsetAir = viewPointRow2.findViewById(R.id.text_point1label);
        LinearLayout lt_column2 = loopOpRow.findViewById(R.id.lt_column2);
        TextView label2 = viewPointRow2.findViewById(R.id.text_point2label);

        TextView textViewInputAir = viewPointRow1.findViewById(R.id.text_point1value);
        TextView textViewTargetAir = viewPointRow1.findViewById(R.id.text_point2value);
        TextView textViewOffsetAir = viewPointRow2.findViewById(R.id.text_point1value);
        TextView value2 = viewPointRow2.findViewById(R.id.text_point2value);

        label2.setVisibility(View.GONE);
        value2.setVisibility(View.GONE);
        lt_column2.setVisibility(View.GONE);

        textViewTitle.setText(plcPoints.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(plcPoints.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));

        labelInputAir.setText("Input  " + plcPoints.get("Unit Type").toString() + " : ");

        double processValue = (double) plcPoints.get("Input Value");
        textViewInputAir.setText(String.format("%.2f", processValue) + " " + plcPoints.get("Unit").toString());
        try {
            if ((boolean) plcPoints.get("Dynamic Setpoint") == true) {

                labelTarget.setText(plcPoints.get("Dynamic Unit Type").toString() + " : ");
                textViewTargetAir.setText(plcPoints.get("Target Value").toString() + " " + plcPoints.get("Dynamic Unit").toString());
                labelOffsetAir.setText("Offset " + plcPoints.get("Dynamic Unit Type").toString() + " : ");
                textViewOffsetAir.setText(plcPoints.get("Offset Value").toString() + " " + plcPoints.get("Dynamic Unit").toString());
                viewPointRow2.setPadding(0, 0, 0, 40);
                linearLayoutZonePoints.addView(viewTitle);
                linearLayoutZonePoints.addView(viewStatus);
                linearLayoutZonePoints.addView(loopOpRow);
                linearLayoutZonePoints.addView(viewPointRow1);
                linearLayoutZonePoints.addView(viewPointRow2);

            } else {
                labelTarget.setText(plcPoints.get("Dynamic Unit Type").toString().replace("Native-", "") + " : ");
                textViewTargetAir.setText(plcPoints.get("Target Value").toString() + " " + plcPoints.get("Dynamic Unit").toString());
                viewPointRow1.setPadding(0, 0, 0, 40);
                linearLayoutZonePoints.addView(viewTitle);
                linearLayoutZonePoints.addView(viewStatus);
                linearLayoutZonePoints.addView(loopOpRow);
                linearLayoutZonePoints.addView(viewPointRow1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setPointVal(String coolid, double coolval, String heatid, double heatval, String avgid, double avgval) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                CCUHsApi hayStack = CCUHsApi.getInstance();
                Point coolpoint = new Point.Builder().setHashMap(hayStack.readMapById(coolid)).build();
                Point heatpoint = new Point.Builder().setHashMap(hayStack.readMapById(heatid)).build();
                Point avgpoint = new Point.Builder().setHashMap(hayStack.readMapById(avgid)).build();

                if (coolpoint.getMarkers().contains("writable")) {
                    CcuLog.d(L.TAG_CCU_UI, "Set Writbale Val " + coolpoint.getDisplayName() + ": " + coolid + "," + heatpoint.getDisplayName() + "," + heatval + "," + avgpoint.getDisplayName());
                    ScheduleProcessJob.handleManualDesiredTempUpdate(coolpoint, heatpoint, avgpoint, coolval, heatval, avgval);

                }

                if (coolpoint.getMarkers().contains("his") && (coolval != 0)) {
                    CcuLog.d(L.TAG_CCU_UI, "Set His Val " + coolid + ": " + coolval);
                    hayStack.writeHisValById(coolid, coolval);
                }
                if (heatpoint.getMarkers().contains("his") && (heatval != 0)) {
                    CcuLog.d(L.TAG_CCU_UI, "Set His Val " + heatid + ": " + heatval);
                    hayStack.writeHisValById(heatid, heatval);
                }
                if (avgpoint.getMarkers().contains("his") && (ScheduleProcessJob.getSystemOccupancy() == Occupancy.OCCUPIED)) {
                    CcuLog.d(L.TAG_CCU_UI, "Set His Val " + avgid + ": " + avgval);
                    hayStack.writeHisValById(avgid, avgval);
                }
            }
        });
        thread.start();
    }


    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        // loadGrid(parentRootView);

        weatherUpdateHandler = new Handler();
        weatherUpdate = () -> {
            if (weatherUpdateHandler != null && getActivity() != null) {
                if (weather_data.getVisibility() == View.VISIBLE) {
                    Log.e("weather", "update");
                    UpdateWeatherData();
                }
                weatherUpdateHandler.postDelayed(weatherUpdate, 15 * 60000);
            }
        };

        weatherUpdate.run();
        //Globals.getInstance().setCcuReady(true);
        CcuLog.i("UI_PROFILING","ZoneFragmentNew.onResume Done");
    }

    private void setListeners() {
        if (getUserVisibleHint()) {
            UpdatePointHandler.setZoneDataInterface(this);
            Pulse.setCurrentTempInterface(this);
            ScheduleProcessJob.setScheduleDataInterface(this);
            ScheduleProcessJob.setZoneDataInterface(this);
            StandaloneScheduler.setZoneDataInterface(this);
            HyperStatMsgReceiver.setCurrentTempInterface(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        UpdatePointHandler.setZoneDataInterface(null);
        Pulse.setCurrentTempInterface(null);
        ScheduleProcessJob.setScheduleDataInterface(null);
        ScheduleProcessJob.setZoneDataInterface(null);
        StandaloneScheduler.setZoneDataInterface(null);
        HyperStatScheduler.Companion.setZoneDataInterface(this);
        HyperStatMsgReceiver.setCurrentTempInterface(null);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            UpdatePointHandler.setZoneDataInterface(this);
            Pulse.setCurrentTempInterface(this);
            ScheduleProcessJob.setScheduleDataInterface(this);
            ScheduleProcessJob.setZoneDataInterface(this);
            StandaloneScheduler.setZoneDataInterface(this);
            HyperStatScheduler.Companion.setZoneDataInterface(this);
            HyperStatMsgReceiver.setCurrentTempInterface(this);
        } else {

            UpdatePointHandler.setZoneDataInterface(null);
            Pulse.setCurrentTempInterface(null);
            ScheduleProcessJob.setScheduleDataInterface(null);
            ScheduleProcessJob.setZoneDataInterface(null);
            StandaloneScheduler.setZoneDataInterface(null);
            HyperStatScheduler.Companion.setZoneDataInterface(null);
            HyperStatMsgReceiver.setCurrentTempInterface(null);
        }
    }

    class FloorComparator implements Comparator<Floor> {
        @Override
        public int compare(Floor a, Floor b) {
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        }
    }

    public void ScaleImageToNormal(int height, int width, ImageView imageView) {
        int endHeight = (int) (height / 1.35);
        int endWidth = (int) (width / 1.35);
        imageView.getLayoutParams().height = endHeight;
        imageView.getLayoutParams().width = endWidth;
        imageView.setPadding(36, 36, 36, 36);
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

    public void ScaleImageToBig(int height, int width, ImageView imageView) {
        int endHeight = (int) (height * 1.35);
        int endWidth = (int) (width * 1.35);
        imageView.getLayoutParams().height = endHeight;
        imageView.getLayoutParams().width = endWidth;
        imageView.setPadding(36, 36, 36, 36);
    }

    public void showWeather() {
        weather_data.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(-weather_data.getWidth(), 0, 0, 0);
        animate.setDuration(400);
        animate.setFillAfter(true);
        weather_data.startAnimation(animate);
    }

    public void hideWeather() {
        TranslateAnimation animate = new TranslateAnimation(0, -weather_data.getWidth() + 5, 0, 0);
        animate.setDuration(400);
        animate.setFillAfter(true);
        weather_data.startAnimation(animate);
        tableLayout.startAnimation(in);
        weather_data.setVisibility(View.GONE);
    }

    public boolean isWeatherShown() {
        boolean isShown = false;
        if (weather_data.getVisibility() == View.VISIBLE) {
            isShown = true;
        } else {
            isShown = false;
        }
        return isShown;
    }

    private void setScheduleType(String id, ScheduleType schedule, ArrayList<HashMap> zoneMap) {
        Thread thread = new Thread(() -> {
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
            ScheduleProcessJob.handleScheduleTypeUpdate(p);
        });
        thread.start();
    }


    private boolean checkContainment(String scheduleTypeId, Schedule zoneSchedule, Spinner scheduleSpinner, ArrayList<HashMap> zoneMap) {
        Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);
        ArrayList<Interval> intervalSpills = new ArrayList<>();
        ArrayList<Interval> systemIntervals = systemSchedule.getMergedIntervals();

        for (Interval v : systemIntervals) {
            CcuLog.d("CCU_UI", "Merged System interval " + v);
        }

        ArrayList<Interval> zoneIntervals = zoneSchedule.getScheduledIntervals();

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


        if (intervalSpills.size() > 0) {
            StringBuilder spillZones = new StringBuilder();
            for (Interval i : intervalSpills) {
                spillZones.append(ScheduleUtil.getDayString(i.getStart().getDayOfWeek()) + " (" + i.getStart().hourOfDay().get() + ":" + (i.getStart().minuteOfHour().get() == 0 ? "00" : i.getStart().minuteOfHour().get()) + " - " + i.getEnd().hourOfDay().get() + ":" + (i.getEnd().minuteOfHour().get() == 0 ? "00" : i.getEnd().minuteOfHour().get()) + ") \n");
            }

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
            builder.setMessage("Zone Schedule is outside building schedule currently set. " +
                    "Proceed with trimming the zone schedules to be within the building schedule \n" + spillZones)
                    .setCancelable(false)
                    .setTitle("Schedule Errors")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            scheduleSpinner.setSelection(0);
                            dialog.dismiss();
                        }

                    })
                    .setPositiveButton("Force-Trim", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            setScheduleType(scheduleTypeId, ScheduleType.ZONE, zoneMap);
                            HashMap<String, ArrayList<Interval>> spillsMap = new HashMap<>();
                            spillsMap.put(zoneSchedule.getRoomRef(), intervalSpills);
                            ScheduleUtil.trimZoneSchedule(mSchedule, spillsMap);
                            CCUHsApi.getInstance().scheduleSync();
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
            return false;
        } else {
            return true;
        }
    }

    private String getScheduleTypeId(String equipId) {
        return CCUHsApi.getInstance().readId("point and scheduleType and equipRef == \"" + equipId + "\"");
    }
    
    private void loadSENSEPointsUI(HashMap sensePoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String nodeAddress) {

        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        textViewTitle.setText(sensePoints.get("Profile").toString() + " (" + nodeAddress + ")");
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
        linearLayoutZonePoints.addView(viewTitle);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        LinearLayout statusLayout=viewStatus.findViewById(R.id.ll_status);
        statusLayout.setVisibility(View.GONE);
        LinearLayout linearl = viewStatus.findViewById(R.id.lllastupdate);
        LinearLayout.LayoutParams param = (LinearLayout.LayoutParams)linearl.getLayoutParams();
        param.setMargins(55, 20, 0, 20);
        linearl.setLayoutParams(param);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
        linearLayoutZonePoints.addView(viewStatus);

        if (sensePoints.get("isTh1Enable") == "true") {
            View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);
            LinearLayout ll = (LinearLayout) viewPointRow1.findViewById(R.id.lt_column1);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)ll.getLayoutParams();
            params.setMargins(55, 0, 0, -40);
            ll.setLayoutParams(params);
            TextView label = viewPointRow1.findViewById(R.id.text_point1label);
            TextView val = viewPointRow1.findViewById(R.id.text_point1value);
            label.setText(sensePoints.get("Thermistor1").toString() + " : ");
            val.setText((sensePoints.get("Th1Val").toString()) + " " + (sensePoints.get("Unit3").toString()));
            linearLayoutZonePoints.addView(viewPointRow1);
        }

        if (sensePoints.get("isTh2Enable") == "true") {
            View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);
            LinearLayout ll = (LinearLayout) viewPointRow1.findViewById(R.id.lt_column1);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)ll.getLayoutParams();
            params.setMargins(55, 0, 0, -40);
            ll.setLayoutParams(params);
            TextView label = viewPointRow1.findViewById(R.id.text_point1label);
            TextView val = viewPointRow1.findViewById(R.id.text_point1value);
            label.setText(sensePoints.get("Thermistor2").toString() + " : ");
            val.setText((sensePoints.get("Th2Val").toString()) + " " + (sensePoints.get("Unit4").toString()));
            linearLayoutZonePoints.addView(viewPointRow1);
        }

        if (sensePoints.get("iAn1Enable") == "true") {
            View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);
            LinearLayout ll = (LinearLayout) viewPointRow1.findViewById(R.id.lt_column1);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)ll.getLayoutParams();
            params.setMargins(55, 0, 0, -40);
            ll.setLayoutParams(params);
            TextView label = viewPointRow1.findViewById(R.id.text_point1label);
            TextView val = viewPointRow1.findViewById(R.id.text_point1value);
            label.setText(sensePoints.get("Analog1").toString() + " : ");
            val.setText((sensePoints.get("An1Val").toString()) + " " + (sensePoints.get("Unit1").toString()));
            linearLayoutZonePoints.addView(viewPointRow1);
        }
        if (sensePoints.get("iAn2Enable") == "true") {
            View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);
            LinearLayout ll = (LinearLayout) viewPointRow1.findViewById(R.id.lt_column1);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)ll.getLayoutParams();
            params.setMargins(55, 0, 0, 0);
            ll.setLayoutParams(params);
            TextView label = viewPointRow1.findViewById(R.id.text_point1label);
            TextView val = viewPointRow1.findViewById(R.id.text_point1value);
            label.setText(sensePoints.get("Analog2").toString() + " : ");
            val.setText((sensePoints.get("An2Val").toString()) + " " + (sensePoints.get("Unit2").toString()));
            linearLayoutZonePoints.addView(viewPointRow1);
        }

    }
    private void viewSenseZone(LayoutInflater inflater, View rootView, ArrayList<HashMap> zoneMap, String zoneTitle, int gridPosition, LinearLayout[] tablerowLayout, boolean isZoneAlive) {

        Log.i("ProfileTypes", "Points:" + zoneMap.toString());
        Equip p = new Equip.Builder().setHashMap(zoneMap.get(0)).build();
        Log.i("ProfileTypes", "p:" + p.toString());
        double offsetAvg = 0;
        double currentAverageTemp = 0;
        double curTemp= 0;
        for (int i = 0; i < zoneMap.size(); i++) {
            Equip avgTempEquip = new Equip.Builder().setHashMap(zoneMap.get(i)).build();
            double avgTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and equipRef == \"" + avgTempEquip.getId() + "\"");
            currentAverageTemp = (currentAverageTemp + avgTemp);
        }

        curTemp = currentAverageTemp ;

        Log.i("EachzoneData", " currentAvg:" + currentAverageTemp);
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

        GridItem gridItemObj = new GridItem();
        gridItemObj.setGridID(i);
        gridItemObj.setGridItem("Sense");
        gridItemObj.setNodeAddress(Short.valueOf(p.getGroup()));
        gridItemObj.setZoneEquips(zoneMap);
        arcView.setClickable(true);
        arcView.setTag(gridItemObj);
        arcView.setId(i);
        SeekArc seekArc = arcView.findViewById(R.id.seekArc);
        seekArc.setTag(gridItemObj);
        zoneDetails.setTag(gridItemObj);
        seekArc.scaletoNormal(250, 210);
        TextView textEquipment = arcView.findViewById(R.id.textEquipment);
        textEquipment.setText(zoneTitle);
        seekArc.setSense(true);
        seekArc.setSenseData(false, (float)(curTemp));
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
        seekArc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                                        if (viewTag.getGridItem().equals("Sense") || viewTag.getGridItem().equals("Temp")) {
                                            SeekArc seekArcExpanded = (SeekArc) gridItem.findViewById(R.id.seekArc);
                                            TextView textViewzone = (TextView) gridItem.findViewById(R.id.textEquipment);
                                            textViewzone.setTextAppearance(getActivity(), R.style.label_black);
                                            textViewzone.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                            tableLayout.removeViewAt(row + 1);
                                            seekArcExpanded.setDetailedView(false);
                                            seekArcExpanded.setBackgroundColor(getResources().getColor(R.color.white));
                                            seekArcExpanded.scaletoNormal(250, 210);
                                            gridItem.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                        } else {
                                            TextView textViewzone = (TextView) gridItem.findViewById(R.id.textEquipment);
                                            textViewzone.setTextAppearance(getActivity(), R.style.label_black);
                                            textViewzone.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                            tableLayout.removeViewAt(row + 1);
                                            NonTempControl nonTempControl = gridItem.findViewById(R.id.rl_nontemp);
                                            ScaleControlToNormal(250, 210, nonTempControl);
                                            nonTempControl.setExpand(false);
                                            //ScaleImageToNormal(250,210,imageViewExpanded);
                                            nonTempControl.setBackgroundColor(getResources().getColor(R.color.white));
                                            gridItem.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
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
                            seekArc.scaletoNormalBig(250, 210);
                            imageOn = true;
                            selectedView = seekArc.getId();
                            try {
                                textEquipment.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
                                textEquipment.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                                zoneDetails.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                                tableLayout.addView(zoneDetails, index);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            isExpanded = true;
                        }
                    } else if (clickposition == clickedView) {
                        v.setBackgroundColor(getResources().getColor(R.color.white));
                        textEquipment.setTextAppearance(getActivity(), R.style.label_black);
                        textEquipment.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                        tableLayout.removeView(zoneDetails);
                        imageOn = false;
                        seekArc.setDetailedView(false);
                        seekArc.scaletoNormal(250, 210);
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
                    seekArc.scaletoNormalBig(250, 210);
                    hideWeather();
                    imageOn = true;
                    selectedView = seekArc.getId();
                    try {
                        textEquipment.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
                        textEquipment.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        zoneDetails.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        tableLayout.addView(zoneDetails, index);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isExpanded = true;
                }

                zoneOpen = false;
                v.setContentDescription(zoneTitle);
                linearLayoutZonePoints.removeAllViews();
                for (int k = 0; k < openZoneMap.size(); k++) {
                    Equip updatedEquip = new Equip.Builder().setHashMap(openZoneMap.get(k)).build();
                    if (updatedEquip.getProfile().contains("SENSE")) {
                        HashMap sensePoints = ScheduleProcessJob.getHyperStatSenseEquipPoints(updatedEquip.getGroup());
                        seekArc.setCurrentTemp(Float.parseFloat(sensePoints.get("curtempwithoffset").toString()));
                        loadSENSEPointsUI(sensePoints, inflater, linearLayoutZonePoints, updatedEquip.getGroup());
                    }
                }
            }
        });
    }

    private void loadBPOSPointsUI(HashMap point, LayoutInflater inflater,
                                  LinearLayout linearLayoutZonePoints,
                                  String nodeAddress) {


        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.bpos_zone_ui, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);

        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);


        TextView textViewValue1 = viewPointRow1.findViewById(R.id.text_point1value);
        TextView textViewValue2 = viewPointRow1.findViewById(R.id.text_point2value);

        Log.d("BPOSUtil","Status="+point.get("Status").toString() +
                "humidity ="+point.get("humidity").toString() +
                "forceoccupied" + point.get("forceoccupied"));

        textViewTitle.setText(point.get("Profile").toString() + " (" + nodeAddress + ")");
        textViewStatus.setText(point.get("Status").toString());
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));


        textViewLabel1.setText("Humidity: ");
        textViewValue1.setText(point.get("humidity").toString() + "%");


        double occupied = (double) point.get("forceoccupied");
        if(occupied == (double)Occupancy.FORCEDOCCUPIED.ordinal()){
            textViewLabel2.setText("Temporary Hold: ");
            textViewValue2.setText("Yes");
        }else if(occupied == (double)Occupancy.AUTOFORCEOCCUPIED.ordinal()){
            textViewLabel2.setText("Temporary Hold(AUTO): ");
            textViewValue2.setText("Yes");
        }else{
            textViewLabel2.setText("Temporary Hold: ");
            textViewValue2.setText("No");
        }
        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        linearLayoutZonePoints.addView(viewPointRow1);

    }


    public void loadHyperstatCpuProfile(HashMap cpuEquipPoints, LayoutInflater inflater,
                                        LinearLayout linearLayoutZonePoints,
                                        String equipId, String nodeAddress) {

        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type2, null);
        View viewPointRow2 = inflater.inflate(R.layout.zones_item_type2, null);
        View viewDischarge = inflater.inflate(R.layout.zones_item_discharge, null);

        setTitleStatusConfig(viewTitle,viewStatus,nodeAddress,
                cpuEquipPoints.get(HSZoneStatus.STATUS.name()).toString());

        setUpConditionFanConfig(viewPointRow1,cpuEquipPoints,equipId);

        setUpHumidifierDeHumidifier(viewPointRow2,cpuEquipPoints,equipId);

        TextView textAirflowValue = viewDischarge.findViewById(R.id.text_airflowValue);
        textAirflowValue.setText(cpuEquipPoints.get(HSZoneStatus.DISCHARGE_AIRFLOW.name()).toString());

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        linearLayoutZonePoints.addView(viewPointRow2);
        linearLayoutZonePoints.addView(viewPointRow1);
        linearLayoutZonePoints.addView(viewDischarge);


    }



    private void setTitleStatusConfig(View viewTitle,View viewStatus, String nodeAddress, String status ){
        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        textViewTitle.setText(SettingsKt.PROFILE + " - (" + nodeAddress + ")");
        TextView textViewModule = viewTitle.findViewById(R.id.module_status);
        HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);

        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        textViewStatus.setText(status);
        TextView textViewUpdatedTime = viewStatus.findViewById(R.id.last_updated_status);
        textViewUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
    }


    private void setUpConditionFanConfig(View viewPointRow1, HashMap cpuEquipPoints , String equipId){

        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        textViewLabel1.setText("Conditioning Mode : ");
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);
        textViewLabel2.setText("Fan Mode : ");
        Spinner conditioningModeSpinner = viewPointRow1.findViewById(R.id.spinnerValue1);
        Spinner fanModeSpinner = viewPointRow1.findViewById(R.id.spinnerValue2);
        CCUUiUtil.setSpinnerDropDownColor(conditioningModeSpinner,getContext());
        CCUUiUtil.setSpinnerDropDownColor(fanModeSpinner,getContext());

        int conditionMode = 0;
        int fanMode = 0;
        try {
            conditionMode = (int) ((double)cpuEquipPoints.get(HSZoneStatus.CONDITIONING_MODE.name()));
            fanMode = (int) ((double)cpuEquipPoints.get(HSZoneStatus.FAN_MODE.name()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayAdapter<CharSequence> conModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_conditionmode, R.layout.spinner_zone_item);

        if (cpuEquipPoints.containsKey(HSZoneStatus.CONDITIONING_ENABLED.name())) {
            if (cpuEquipPoints.get(HSZoneStatus.CONDITIONING_ENABLED.name()).toString().contains("Cool Only")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_coolonly,
                        R.layout.spinner_zone_item);
                if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1;
                }
            } else if (cpuEquipPoints.get(HSZoneStatus.CONDITIONING_ENABLED.name()).toString().contains("Heat Only")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_heatonly,
                        R.layout.spinner_zone_item);
                if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal()) {
                    conditionMode = conModeAdapter.getCount() - 1;
                }
            }
            if (cpuEquipPoints.get(HSZoneStatus.CONDITIONING_ENABLED.name()).toString().contains("Off")) {
                conModeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.smartstat_conditionmode_off, R.layout.spinner_zone_item);
                conditionMode = 0;
            }

        }
        conModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        conditioningModeSpinner.setAdapter(conModeAdapter);
        conditioningModeSpinner.setSelection(conditionMode, false);


        int fanSpinnerSelectionValues =
                RelayUtil.getFanOptionByLevel((Integer)cpuEquipPoints.get(HSZoneStatus.FAN_LEVEL.name()));
        ArrayAdapter<CharSequence> fanModeAdapter = ArrayAdapter.createFromResource(
                getActivity(),fanSpinnerSelectionValues, R.layout.spinner_zone_item);

        fanModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        fanModeSpinner.setAdapter(fanModeAdapter);
        fanModeSpinner.setSelection(fanMode, false);

        setSpinnerListenerForHyperstat(conditioningModeSpinner,HSZoneStatus.CONDITIONING_MODE,equipId,conditionMode);
        setSpinnerListenerForHyperstat(fanModeSpinner,HSZoneStatus.FAN_MODE,equipId,fanMode);

    }

    private void setUpHumidifierDeHumidifier(View viewPointRow2,HashMap cpuEquipPoints,  String equipId){

        TextView textViewLabel3 = viewPointRow2.findViewById(R.id.text_point1label);
        TextView textViewLabel4 = viewPointRow2.findViewById(R.id.text_point2label);

        Spinner humiditySpinner = viewPointRow2.findViewById(R.id.spinnerValue1);
        Spinner dehumiditySpinner = viewPointRow2.findViewById(R.id.spinnerValue2);

        ArrayList<String> arrayHumdityTargetList = new ArrayList<>();
        for (int pos = 1; pos <= 100; pos++)
            arrayHumdityTargetList.add(pos+"%");
        ArrayAdapter<String> humiditytargetadapter = new ArrayAdapter<>(
                getActivity(), R.layout.spinner_zone_item, arrayHumdityTargetList);
        humiditytargetadapter.setDropDownViewResource(R.layout.spinner_item_grey);

        if(cpuEquipPoints.containsKey(HSZoneStatus.TARGET_HUMIDITY.name())){
            textViewLabel3.setText("Target Humidity :");

            humiditySpinner.setAdapter(humiditytargetadapter);
            double targetHumidity = (double) cpuEquipPoints.get(HSZoneStatus.TARGET_HUMIDITY.name());
            humiditySpinner.setSelection((int) targetHumidity - 1, false);

            humiditySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    handleHumidityMode((int)targetHumidity,position,equipId);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

        }else{
            (viewPointRow2.findViewById(R.id.lt_column1)).setVisibility(View.GONE);
        }
        if(cpuEquipPoints.containsKey(HSZoneStatus.TARGET_DEHUMIDIFY.name())){
            textViewLabel4.setText("Target Dehumidity :");
            dehumiditySpinner.setAdapter(humiditytargetadapter);
            double targetDeHumidity = (double) cpuEquipPoints.get(HSZoneStatus.TARGET_DEHUMIDIFY.name());
            dehumiditySpinner.setSelection((int) targetDeHumidity - 1, false);
          /*  setSpinnerListenerForHyperstat(
                    dehumiditySpinner,HSZoneStatus.TARGET_DEHUMIDIFY,equipId,(int) targetDeHumidity);*/
            dehumiditySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    handleDeHumidityMode((int)targetDeHumidity,position,equipId);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            if((viewPointRow2.findViewById(R.id.lt_column1)).getVisibility() == (View.GONE)){
                textViewLabel4.setPadding(52,0,0,0);
            }

        }else{
            (viewPointRow2.findViewById(R.id.lt_column2)).setVisibility(View.GONE);
        }
    }

    private void setSpinnerListenerForHyperstat(View view, HSZoneStatus spinnerType,String equipId,
                                                int previousPosition){
        AdapterView.OnItemSelectedListener onItemSelectedListener =new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (spinnerType){
                    case CONDITIONING_MODE : handleConditionMode(previousPosition,position,equipId); break;
                    case FAN_MODE : handleFanMode(previousPosition,position,equipId); break;
                    case TARGET_HUMIDITY : handleHumidityMode(previousPosition,position,equipId);break;
                    case TARGET_DEHUMIDIFY : handleDeHumidityMode(previousPosition,position,equipId);break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };
        ((Spinner)view).setOnItemSelectedListener(onItemSelectedListener);
    }

    private void handleConditionMode(int previousPosition,int selectedPosition, String equipId){
        if (isCPUFromPubNub) {
            if (previousPosition != selectedPosition) {
                HyperStatScheduler.Companion.updateHyperstatUIPoints(equipId,
                        "temp and conditioning and mode and cpu", selectedPosition);
            }
        } else {
            HyperStatScheduler.Companion.updateHyperstatUIPoints(equipId,
                    "temp and conditioning and mode and cpu", selectedPosition);

        }
    }

    private void handleFanMode(int previousPosition, int selectedPosition, String equipId){
        if (isCPUFromPubNub) {
            if (previousPosition != selectedPosition) {
                updateFanMode(equipId,selectedPosition);
            }
            isCPUFromPubNub = false;
        } else {
            updateFanMode(equipId,selectedPosition);
        }
    }

    // Save the fan mode in cache
    private void updateFanMode(String equipId,int selectedPosition){

        FanModeCacheStorage cacheStorage = new FanModeCacheStorage();
        HyperStatScheduler.Companion.updateHyperstatUIPoints(
                equipId, "fan and operation and mode and cpu", selectedPosition);

        if ((selectedPosition != 0) && (selectedPosition % 3 == 0))
            cacheStorage.saveFanModeInCache(equipId,selectedPosition);
        else
            cacheStorage.removeFanModeFromCache(equipId);
    }

    private void handleHumidityMode(int targetHumidity,int selectedPosition, String equipId){
        if (isHPUFromPubNub) {
            if (targetHumidity != (selectedPosition + 1)) {
                HyperStatScheduler.Companion.updateHyperstatUIPoints(
                        equipId, "target and humidifier and cpu", selectedPosition + 1);
            }
        }else{
            HyperStatScheduler.Companion.updateHyperstatUIPoints(
                    equipId, "target and humidifier and cpu", selectedPosition + 1);
        }
    }

    private void handleDeHumidityMode(int targetDeHumidity,int selectedPosition, String equipId){
        if (isHPUFromPubNub) {
            if (targetDeHumidity != (selectedPosition + 1)) {
                HyperStatScheduler.Companion.updateHyperstatUIPoints(
                        equipId, "target and dehumidifier and cpu", selectedPosition + 1);
            }
        }else{
            HyperStatScheduler.Companion.updateHyperstatUIPoints(
                    equipId, "target and dehumidifier and cpu", selectedPosition + 1);
        }
    }

}
