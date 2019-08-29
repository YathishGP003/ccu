package a75f.io.renatus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
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

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.device.mesh.Pulse;
import a75f.io.logger.CcuLog;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ScheduleType;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.jobs.StandaloneScheduler;
import a75f.io.logic.pubnub.UpdatePointHandler;
import a75f.io.logic.pubnub.ZoneDataInterface;
import a75f.io.renatus.schedules.ScheduleUtil;
import a75f.io.renatus.schedules.SchedulerFragment;
import a75f.io.renatus.util.GridItem;
import a75f.io.renatus.util.NonTempControl;
import a75f.io.renatus.util.SeekArc;

public class ZoneFragmentNew extends Fragment implements ZoneDataInterface
{
    ExpandableListView            expandableListView;
    ExpandableListAdapter         expandableListAdapter;
    List<String>                  expandableListTitle;
    HashMap<String, List<String>> expandableListDetail;


    HashMap<String, String> tunerMap = new HashMap();
    int lastExpandedPosition;

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
    private boolean isWeatherWidget = false;
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
    GridItem gridItemSelected = new GridItem();
    View parentRootView;
    Schedule mSchedule = null;
    int mScheduleType = -1;
    ScrollView scrollViewParent;
    Equip equipment;

    boolean zoneOpen = false;
    SeekArc seekArcOpen;
    NonTempControl nonTempControlOpen;
    ImageView imageEquipOpen;
    View zonePointsOpen;
    Equip equipOpen;
    View titleOpen;
    View statusOpen;
    View type1Open;
    View type2Open;
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
    double currentAverageTemp = 0;
    double currentTempSensor = 0;
    int noTempSensor = 0;
    public ZoneFragmentNew()
    {
    }
    
    public static ZoneFragmentNew newInstance()
    {
        return new ZoneFragmentNew();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_zones, container, false);
        parentRootView = rootView.findViewById(R.id.zone_fragment_temp);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
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
        tableLayout = (TableLayout) view.findViewById( R.id.tableRoot );
        gridlayout = (GridLayout) view.findViewById(R.id.gridview);
        recyclerView = (RecyclerView)view.findViewById(R.id.recyclerEquip);

        recyclerView.setVisibility(View.GONE);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 4));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        in = AnimationUtils.makeInAnimation(getActivity(), false);
        inleft = AnimationUtils.makeInAnimation(getActivity(), true);
        in.setDuration(400);
        inleft.setDuration(400);

        //recyclerView.setHasFixedSize(true);
        expandableListDetail = new HashMap<>();

        floorList = HSUtil.getFloors();
        Collections.sort(floorList, new FloorComparator());

        mFloorListAdapter = new DataArrayAdapter<Floor>(getActivity(), R.layout.listviewitem,floorList);
        lvFloorList.setAdapter(mFloorListAdapter);
        loadGrid(parentRootView);

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
    }

    public void refreshScreen(String id)
    {
        if(getActivity() != null) {
            if(zoneOpen) {
                //Log.i("PubNub","Zone Point Updating:"+id+" Points:"+pointsOpen.toString());
                //if(pointsOpen.containsKey(id)) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                                updateTemperatureBasedZones(seekArcOpen, zonePointsOpen, equipOpen, getLayoutInflater());
                                tableLayout.invalidate();
                        }
                    });
                //}
                }
        }
    }

    public void refreshDesiredTemp(String nodeAddress,String pointcoolDT1, String pointheatDT1)
    {
        if(getActivity() != null) {
            int i;
            for (i = 0; i < seekArcArrayList.size(); i++) {
                GridItem gridItem = (GridItem) seekArcArrayList.get(i).getTag();
                if (gridItem.getNodeAddress() == Short.valueOf(nodeAddress)) {
                    SeekArc tempSeekArc = seekArcArrayList.get(i);
                    double pointheatDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and heating and group == \"" + nodeAddress + "\"");
                    double pointcoolDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and cooling and group == \"" + nodeAddress + "\"");
                    //float coolDt = Float.parseFloat(pointcoolDT);
                    //float heatDt = Float.parseFloat(pointheatDT);
                    if((tempSeekArc.getCoolingDesiredTemp() != pointcoolDT) || (tempSeekArc.getHeatingDesiredTemp() != pointheatDT)) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("Scheduler", "refreshDesiredTemp22 =" + pointcoolDT + "," + pointheatDT + "," + nodeAddress);
                                tempSeekArc.setCoolingDesiredTemp((float)pointcoolDT, false);
                                tempSeekArc.setHeatingDesiredTemp((float)pointheatDT, false);
                                tempSeekArc.invalidate();
                            }
                        });
                    }
                }
            }
        }
    }

    public void refreshScreenbySchedule(String nodeAddress, String equipId, String zoneId){
        if(getActivity() != null) {
            int i;
            String status = ScheduleProcessJob.getZoneStatusString(zoneId, equipId);
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
                            vacationStatusTV.setText(vacationStatus);
                            scheduleStatus.setText(status);
                        }
                    });
                }
            }
        }
    }

    public void updateTemperature(double currentTemp, short nodeAddress){
        if(getActivity() != null) {
            int i;
            if(currentTemp > 0) {
                for (i = 0; i < seekArcArrayList.size(); i++)
                {
                    GridItem gridItem = (GridItem) seekArcArrayList.get(i).getTag();
                    ArrayList<Short> zoneNodes = gridItem.getZoneNodes();
                    Log.i("CurrentTemp", "SensorCurrentTemp:" + currentTemp + " Node:" + nodeAddress+" zoneNodes:"+zoneNodes);
                    if(zoneNodes.contains(nodeAddress))
                    {
                            SeekArc tempSeekArc = seekArcArrayList.get(i);
                            new AsyncTask<String, Void, Double>() {
                                @Override
                                protected Double doInBackground( final String ... params ) {
                                    currentTempSensor = 0;
                                    noTempSensor = 0;
                                    ArrayList<HashMap> zoneEquips = gridItem.getZoneEquips();
                                    for (int j = 0; j < zoneEquips.size(); j++) {
                                        Equip tempEquip = new Equip.Builder().setHashMap(zoneEquips.get(j)).build();
                                        double avgTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and equipRef == \"" + tempEquip.getId() + "\"");
                                        if (avgTemp > 0) {
                                            currentTempSensor = (currentTempSensor + avgTemp);
                                        } else {
                                            noTempSensor++;
                                        }
                                    }
                                    if (currentTempSensor > 0 && zoneEquips.size() >1) {
                                            currentTempSensor = currentTempSensor / (zoneEquips.size() - noTempSensor);
                                            DecimalFormat decimalFormat = new DecimalFormat("#.##");
                                            currentTempSensor = Double.parseDouble(decimalFormat.format(currentTempSensor));
                                    }
                                    if(currentTempSensor > 0) {
                                        return currentTempSensor;
                                    }
                                    return null;
                                }

                                @Override
                                protected void onPostExecute( final Double result ) {
                                    if(result != null) {
                                        tempSeekArc.setCurrentTemp((float) (result.doubleValue()));
                                    }
                                }
                            }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "");

                           /* getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if(zoneEquips.size() > 1) {
                                            currentAverageTemp = currentAverageTemp / (zoneEquips.size() - noTempSensor);
                                            DecimalFormat decimalFormat = new DecimalFormat("#.##");
                                            currentAverageTemp = Double.parseDouble(decimalFormat.format(currentAverageTemp));
                                        }
                                        tempSeekArc.setCurrentTemp((float) (currentAverageTemp));
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });*/
                        //}
                        break;
                    }
                }
            }
        }
    }

    public void updateSensorValue(short nodeAddress){
        if(getActivity() != null) {
            if(zoneOpen) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (equipOpen.getProfile().contains("PLC") || equipOpen.getProfile().contains("EMR") || equipOpen.getProfile().contains("monitor")) {
                            updateNonTemperatureBasedZones(nonTempControlOpen, zonePointsOpen, equipOpen, getLayoutInflater());
                            tableLayout.invalidate();
                        }
                    }
                });
            }
        }
    }
    @Override
    public void onStart()
    {
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
            note.setText("Humidity : "+weatherHumidity+"%"+"\n"+"Precipitation : "+weatherPercipitation);
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

    private void selectFloor(int position)
    {
        mFloorListAdapter.setSelectedItem(position);
        roomList = HSUtil.getZones(floorList.get(position).getId());
        closeFloor();
        //updateData();
        showWeather();
        clickedView = -1;
        loadGrid(parentRootView);
        expandableListView.invalidateViews();
    }


    public void openFloor()
    {
        try {
            mDrawerLayout.openDrawer(drawer_screen);
        } catch (Exception e) {
            e.printStackTrace();
            if (mDrawerLayout != null && !mDrawerLayout.isShown()) {
                mDrawerLayout.openDrawer(drawer_screen);
            }
        }
    }

    public void closeFloor()
    {
        try {
            mDrawerLayout.closeDrawer(drawer_screen);
        } catch (Exception e) {
            e.printStackTrace();
            if (mDrawerLayout != null && mDrawerLayout.isShown()) {
                mDrawerLayout.closeDrawer(drawer_screen);
            }
        }
    }

    private void loadGrid(View rootView){
        rowcount = 0;
        if(floorList.size()>0) {
            ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone and roomRef and floorRef == \"" + floorList.get(mFloorListAdapter.getSelectedPostion()).getId() + "\"");
            ArrayList<HashMap> roomMap = CCUHsApi.getInstance().readAll("room and floorRef == \"" + floorList.get(mFloorListAdapter.getSelectedPostion()).getId() + "\"");

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
            Log.i("ZonesMap", "Size:" + zoneData.size() + " Data:" + zoneData);

            imag = new ImageView(getActivity());
            tableLayout.removeAllViews();
            gridItems.clear();
            tableRows.clear();
            String[] itemNames = getResources().getStringArray(R.array.sse_action_type);
            LinearLayout rowLayout = null;
            numRows = (zoneData.size() / columnCount);
            if (zoneData.size() % columnCount != 0)
                numRows++;

            Button[] buttons = new Button[itemNames.length];
            if (numRows > 0) {
                LinearLayout[] tablerowLayout = new LinearLayout[numRows];
                for (int j = 0; j < numRows; j++) {
                    rowLayout = new LinearLayout(getActivity());
                    tableRows.add(rowLayout);
                }
                tablerowLayout[0] = new LinearLayout(tableLayout.getContext());


                int i = 0;
                for (ArrayList<HashMap> equipZones : zoneData.values()) {
                    String zoneTitle = "";
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    View arcViewParent = null;
                    for (int m = 0; m < roomMap.size(); m++) {
                        String roomRef = equipZones.get(0).get("roomRef").toString();
                        String roomRef_Room = roomMap.get(m).get("id").toString();
                        //Log.i("RoomData","roomRef:"+roomRef+" roomMap:"+roomRef_Room);
                        if (roomRef_Room.equals(roomRef)) {
                            zoneTitle = roomMap.get(m).get("dis").toString();
                            break;
                        }
                    }
                    String profileType = "";

                    String profileVAV = "VAV";
                    String profileDAB = "DAB";
                    String profileSSE = "SSE";
                    String profileSmartStat = "SMARTSTAT";
                    String profileEM = "EMR";
                    String profilePLC = "PLC";
                    String profileTempMonitor = "TEMP_MONITOR";
                    String profileTempInfluence = "TEMP_INFLUENCE";

                    //Log.e("RoomData","ProfileType:"+profileType);
                    boolean tempModule = false;
                    boolean nontempModule = false;
                    for (HashMap equipTypes : equipZones) {
                        profileType = equipTypes.get("profile").toString();
                        Log.e("RoomData", "ProfileType:" + profileType);
                        if (profileType.contains(profileVAV) || profileType.contains(profileDAB)|| profileType.contains(profileSSE) || profileType.contains(profileSmartStat) || profileType.contains(profileTempInfluence)) {
                            tempModule = true;
                            Log.e("RoomData", "Load SmartNode ProfileType:" + profileType);
                        }
                        if (profileType.contains(profileEM)||profileType.contains(profilePLC) || profileType.contains(profileTempMonitor) || profileType.contains(profileTempInfluence)) {
                            nontempModule = true;
                            Log.e("RoomData", "Load SmartStat ProfileType:" + profileType);
                        }
                    }

                    if (tempModule) {
                        Log.e("RoomData", "Load Temperature Based View");
                        viewTemperatureBasedZone(inflater, rootView, equipZones, zoneTitle, i, tablerowLayout);
                    }
                    if (!tempModule && nontempModule) {
                        Log.e("RoomData", "Load Non Temperature Based View");
                        viewNonTemperatureBasedZone(inflater, rootView, equipZones, zoneTitle, i, tablerowLayout);
                        //arcViewParent = inflater.inflate(R.layout.zones_item_smartstat, (ViewGroup) rootView, false);
                    }
                    i++;
                }

            }
        }
    }

    private void viewTemperatureBasedZone(LayoutInflater inflater, View rootView, ArrayList<HashMap> zoneMap,String zoneTitle, int gridPosition, LinearLayout[] tablerowLayout)
    {

        Log.i("ProfileTypes","Points:"+zoneMap.toString());
        Equip p = new Equip.Builder().setHashMap(zoneMap.get(0)).build();
        double currentAverageTemp = 0;
        int noTempSensor = 0;
        ArrayList<Short> equipNodes = new ArrayList<>();
        double heatDeadband = 0;
        double coolDeadband = 0;
        double coolUpperlimit = 0;
        double coolLowerlimit = 0;
        double heatUpperlimit = 0;
        double heatLowerlimit = 0;

        for(int i=0;i<zoneMap.size();i++)
        {
            Equip avgTempEquip = new Equip.Builder().setHashMap(zoneMap.get(i)).build();
            double avgTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and equipRef == \"" + avgTempEquip.getId() + "\"");

            double heatDB = CCUHsApi.getInstance().readHisValByQuery("point and heating and deadband and base and equipRef == \"" + avgTempEquip.getId() + "\"");
            double coolDB = CCUHsApi.getInstance().readHisValByQuery("point and cooling and deadband and base and equipRef == \"" + avgTempEquip.getId() + "\"");

            double coolUL = CCUHsApi.getInstance().readHisValByQuery("point and limit and max and cooling and user and equipRef == \"" + avgTempEquip.getId() + "\"");
            double heatUL = CCUHsApi.getInstance().readHisValByQuery("point and limit and max and heating and user and equipRef == \"" + avgTempEquip.getId() + "\"");
            double coolLL = CCUHsApi.getInstance().readHisValByQuery("point and limit and min and cooling and user and equipRef == \"" + avgTempEquip.getId() + "\"");
            double heatLL = CCUHsApi.getInstance().readHisValByQuery("point and limit and min and heating and user and equipRef == \"" + avgTempEquip.getId() + "\"");
            if(heatDB < heatDeadband || heatDeadband == 0)
            {
                heatDeadband = heatDB;
            }
            if(coolDB < coolDeadband || coolDeadband == 0)
            {
                coolDeadband = coolDB;
            }
            if(avgTemp > 0)
            {
                currentAverageTemp = (currentAverageTemp + avgTemp);
            }else{
                noTempSensor++;
            }
            equipNodes.add(Short.valueOf(avgTempEquip.getGroup()));
            Log.i("EachzoneData","temp:"+avgTemp+" currentAvg:"+currentAverageTemp);

            if(heatDB == heatDeadband && coolDB == coolDeadband) // Setting User Limits based on deadband
            {
                coolUpperlimit = coolUL;
                coolLowerlimit = coolLL;
                heatUpperlimit = heatUL;
                heatLowerlimit = heatLL;
            }

        }
        if(zoneMap.size() > 1 && currentAverageTemp != 0){
            currentAverageTemp = currentAverageTemp/(zoneMap.size()-noTempSensor);
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            currentAverageTemp = Double.parseDouble(decimalFormat.format(currentAverageTemp));
        }
        Log.i("EachzoneData"," currentAvg:"+currentAverageTemp);
        String equipId = p.getId();
        int i = gridPosition;
        View arcView = null;
        arcView = inflater.inflate(R.layout.zones_item, (ViewGroup) rootView, false);
        View zoneDetails = inflater.inflate(R.layout.zones_item_details, null);
        //RecyclerView recyclerViewPoints = zoneDetails.findViewById(R.id.recyclerViewProfilePoints);
        LinearLayout linearLayoutZonePoints  = zoneDetails.findViewById(R.id.lt_profilepoints);
        TextView    scheduleStatus      = zoneDetails.findViewById(R.id.schedule_status_tv);
        Spinner scheduleSpinner     = zoneDetails.findViewById(R.id.schedule_spinner);
        ImageButton scheduleImageButton = zoneDetails.findViewById(R.id.schedule_edit_button);
        TextView vacationStatusTV = zoneDetails.findViewById(R.id.vacation_status);


        ArrayAdapter<CharSequence> scheduleAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.schedule, R.layout.spinner_zone_item);
        scheduleAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        scheduleSpinner.setAdapter(scheduleAdapter);

        String zoneId = Schedule.getZoneIdByEquipId(equipId);
        String status = ScheduleProcessJob.getZoneStatusString(zoneId, equipId);
        String vacationStatus = ScheduleProcessJob.getVacationStateString(zoneId);
        //Log.i("ZonePoints","zoneId:"+zoneId+" status:"+status+" vacationstatus:"+vacationStatus);

        vacationStatusTV.setText(vacationStatus);
        scheduleStatus.setText(status);
        String scheduleTypeId = CCUHsApi.getInstance().readId("point and scheduleType and equipRef == \""+equipId+"\"");
        mScheduleType = (int)CCUHsApi.getInstance().readPointPriorityVal(scheduleTypeId);

        mSchedule = Schedule.getScheduleByEquipId(equipId);

        scheduleImageButton.setTag(mSchedule.getId());
        scheduleImageButton.setOnClickListener(v ->
        {
            SchedulerFragment schedulerFragment    = SchedulerFragment.newInstance((String) v.getTag());
            FragmentManager childFragmentManager = getFragmentManager();
            childFragmentManager.beginTransaction()
                    .add(R.id.zone_fragment_temp, schedulerFragment)
                    .addToBackStack("schedule").commit();

            schedulerFragment.setOnExitListener(() -> {
                Toast.makeText(v.getContext(), "Refresh View", Toast.LENGTH_LONG).show();
                mSchedule = Schedule.getScheduleByEquipId(equipId);
                ScheduleProcessJob.updateSchedules(equipOpen);


            });
        });
        scheduleSpinner.setSelection(mScheduleType,false);
        if (mSchedule.isZoneSchedule())
        {
            scheduleImageButton.setVisibility(View.VISIBLE);
        } else if (mSchedule.isNamedSchedule())
        {
            scheduleImageButton.setVisibility(View.VISIBLE);
        } else
        {
            scheduleImageButton.setVisibility(View.GONE);
        }


        scheduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (position == 0 && (mScheduleType != -1))
                {
                    if (mSchedule.isZoneSchedule())
                    {
                        mSchedule.setDisabled(true);
                        CCUHsApi.getInstance().updateZoneSchedule(mSchedule,zoneId);
                    }
                    scheduleImageButton.setVisibility(View.GONE);

                    if (mScheduleType != ScheduleType.BUILDING.ordinal()) {
                        setScheduleType(scheduleTypeId, ScheduleType.BUILDING);
                        mScheduleType = ScheduleType.BUILDING.ordinal();
                    }

                    CCUHsApi.getInstance().scheduleSync();
                } else if (position == 1 && (mScheduleType != -1))
                {
                    if (mSchedule.isZoneSchedule() && mSchedule.getMarkers().contains("disabled"))
                    {
                        mSchedule.setDisabled(false);
                        CCUHsApi.getInstance().updateZoneSchedule(mSchedule, zoneId);
                        scheduleImageButton.setTag(mSchedule.getId());
                    } else
                    {

                        Zone     zone         = Schedule.getZoneforEquipId(equipId);
                        Schedule scheduleById = null;
                        if (zone.hasSchedule())
                        {
                            scheduleById = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
                            Log.d(L.TAG_CCU_UI," scheduleType changed to ZoneSchedule : "+scheduleTypeId);
                            scheduleById.setDisabled(false);
                            checkContainment(scheduleById);
                            CCUHsApi.getInstance().updateZoneSchedule(scheduleById, zone.getId());
                        } else if (!zone.hasSchedule())
                        {
                            Log.d(L.TAG_CCU_UI," Zone does not have Schedule : Shouldn't happen");
                            zone.setScheduleRef(DefaultSchedules.generateDefaultSchedule(true, zone.getId()));
                            CCUHsApi.getInstance().updateZone(zone, zone.getId());
                            scheduleById = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
                            //CCUHsApi.getInstance().syncEntityTree();
                        }
                        scheduleImageButton.setTag(scheduleById.getId());
                        scheduleImageButton.setVisibility(View.VISIBLE);
                        CCUHsApi.getInstance().scheduleSync();
                    }
                    if (mScheduleType != ScheduleType.ZONE.ordinal()) {
                        setScheduleType(scheduleTypeId, ScheduleType.ZONE);
                        mScheduleType = ScheduleType.ZONE.ordinal();
                    }
                } else
                {
                    //list named schedules
                }
                mSchedule = Schedule.getScheduleByEquipId(equipId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

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
        //seekArc.setOnTemperatureChangeListener(SeekArcMemShare.onTemperatureChangeListener);
        TextView textEquipment = arcView.findViewById(R.id.textEquipment);
        textEquipment.setText(zoneTitle);

        seekArc.scaletoNormal(250, 210);

        HashMap currTmep = CCUHsApi.getInstance().read("point and air and temp and sensor and current and equipRef == \"" + p.getId() + "\"");
        HashMap coolDT = CCUHsApi.getInstance().read("point and temp and desired and cooling and sp and equipRef == \"" + p.getId() + "\"");
        HashMap heatDT = CCUHsApi.getInstance().read("point and temp and desired and heating and sp and equipRef == \"" + p.getId() + "\"");
        HashMap coolUL = CCUHsApi.getInstance().read("point and limit and max and cooling and user and equipRef == \"" + p.getId() + "\"");
        HashMap heatUL = CCUHsApi.getInstance().read("point and limit and max and heating and user and equipRef == \"" + p.getId() + "\"");
        HashMap coolLL = CCUHsApi.getInstance().read("point and limit and min and cooling and user and equipRef == \"" + p.getId() + "\"");
        HashMap heatLL = CCUHsApi.getInstance().read("point and limit and min and heating and user and equipRef == \"" + p.getId() + "\"");
        HashMap heatDB = CCUHsApi.getInstance().read("point and heating and deadband and base and equipRef == \"" + p.getId() + "\"");
        HashMap coolDB = CCUHsApi.getInstance().read("point and cooling and deadband and base and equipRef == \"" + p.getId() + "\"");
        HashMap buildingMin = CCUHsApi.getInstance().read("building and limit and min and equipRef == \"" + L.ccu().systemProfile.getSystemEquipRef() + "\"");
        HashMap buildingMax = CCUHsApi.getInstance().read("building and limit and max and equipRef == \"" + L.ccu().systemProfile.getSystemEquipRef() + "\"");

        float pointcurrTmep = (float)getPointVal(currTmep.get("id").toString());
        /*if(pointcurrTmep == 0)
        {
            pointcurrTmep = 72;
        }*/
        float pointcoolDT = (float)getPointVal(coolDT.get("id").toString());
        float pointheatDT = (float)getPointVal(heatDT.get("id").toString());
        float pointcoolUL = (float)getPointVal(coolUL.get("id").toString());
        float pointheatUL = (float)getPointVal(heatUL.get("id").toString());
        float pointcoolLL = (float)getPointVal(coolLL.get("id").toString());
        float pointheatLL = (float)getPointVal(heatLL.get("id").toString());
        float pointbuildingMin = (float)getPointVal(buildingMin.get("id").toString());
        float pointbuildingMax = (float)getPointVal(buildingMax.get("id").toString());
        float pointheatDB = (float)getPointVal(heatDB.get("id").toString());
        float pointcoolDB = (float)getPointVal(coolDB.get("id").toString());

        String floorName = floorList.get(mFloorListAdapter.getSelectedPostion()).getDisplayName();
        Log.i("EachzoneData","CurrentTemp:"+currentAverageTemp+" FloorName:"+floorName+" ZoneName:"+zoneTitle+","+heatDeadband+","+coolDeadband);
        seekArc.setData(false, pointbuildingMin, pointbuildingMax, (float) heatUpperlimit, (float) heatLowerlimit, (float) coolLowerlimit, (float) coolUpperlimit, pointheatDT, pointcoolDT, (float) currentAverageTemp, (float) heatDeadband, (float) coolDeadband);
        seekArc.setDetailedView(false);
        LinearLayout.LayoutParams rowLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        arcView.setPadding(48,64,0,0);
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
            public void onTemperatureChange(SeekArc seekArc, float coolingDesiredTemp, float heatingDesiredTemp,boolean syncToHaystack) {
                if(syncToHaystack){
                    Log.i("Scheduler","cooldt:"+coolDT.get("id").toString()+" value:"+Double.parseDouble(Float.valueOf(coolingDesiredTemp).toString()));
                    Log.i("Scheduler","heatdt:"+heatDT.get("id").toString()+" value:"+Double.parseDouble(Float.valueOf(heatingDesiredTemp).toString()));
                    //setPointVal(coolDT.get("id").toString(),Double.parseDouble(Float.valueOf(coolingDesiredTemp).toString()));
                    //setPointVal(heatDT.get("id").toString(),Double.parseDouble(Float.valueOf(heatingDesiredTemp).toString()));
                    if(zoneMap.size() > 0)
                    {
                        for(int i=0;i<zoneMap.size();i++)
                        {
                            Equip zoneEquip = new Equip.Builder().setHashMap(zoneMap.get(i)).build();
                            HashMap coolDT = CCUHsApi.getInstance().read("point and temp and desired and cooling and sp and equipRef == \"" + zoneEquip.getId() + "\"");
                            HashMap heatDT = CCUHsApi.getInstance().read("point and temp and desired and heating and sp and equipRef == \"" + zoneEquip.getId() + "\"");
                            HashMap avgDT = CCUHsApi.getInstance().read("point and temp and desired and average and sp and equipRef == \"" + zoneEquip.getId() + "\"");
                            setPointVal(coolDT.get("id").toString(),Double.parseDouble(Float.valueOf(coolingDesiredTemp).toString()),heatDT.get("id").toString(),Double.parseDouble(Float.valueOf(heatingDesiredTemp).toString()),avgDT.get("id").toString());
                            //setPointVal(heatDT.get("id").toString(),Double.parseDouble(Float.valueOf(heatingDesiredTemp).toString()));
                        }
                    }
                }

            }
        });

        seekArc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GridItem gridItemNew = (GridItem) v.getTag();
                boolean isExpanded = false;
                int clickedItemRow =0;
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
                                        if(viewTag.getGridItem().equals("Temp")) {
                                            SeekArc seekArcExpanded = (SeekArc) gridItem.findViewById(R.id.seekArc);
                                            TextView textViewzone = (TextView) gridItem.findViewById(R.id.textEquipment);
                                            textViewzone.setTextAppearance(getActivity(), R.style.label_black);
                                            textViewzone.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                            tableLayout.removeViewAt(row + 1);
                                            seekArcExpanded.setDetailedView(false);
                                            seekArcExpanded.setBackgroundColor(getResources().getColor(R.color.white));
                                            seekArcExpanded.scaletoNormal(250, 210);
                                            gridItem.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                        }else {
                                            TextView textViewzone = (TextView) gridItem.findViewById(R.id.textEquipment);
                                            textViewzone.setTextAppearance(getActivity(),R.style.label_black);
                                            textViewzone.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                            tableLayout.removeViewAt(row + 1);
                                            NonTempControl nonTempControl = gridItem.findViewById(R.id.rl_nontemp);
                                            ScaleControlToNormal(250,210,nonTempControl);
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
                            scrollViewParent.scrollTo(0,seekArc.getTop());
                            //scrollViewParent.scrollTo(0,seekArc.getTop());
                            try {
                                textEquipment.setTextAppearance(getActivity(),R.style.label_orange);
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
                        textEquipment.setTextAppearance(getActivity(),R.style.label_black);
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
                    scrollViewParent.scrollTo(0,seekArc.getTop());
                    try {
                        textEquipment.setTextAppearance(getActivity(),R.style.label_orange);
                        textEquipment.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        zoneDetails.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        tableLayout.addView(zoneDetails, index);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isExpanded = true;
                }

                if(isExpanded) {
                    linearLayoutZonePoints.removeAllViews();
                    {
                        for(int k=0;k<zoneMap.size();k++)
                        {
                            Equip p = new Equip.Builder().setHashMap(zoneMap.get(k)).build();
                            String updatedEquipId = p.getId();
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
                                loadSSCPUPointsUI(cpuEquipPoints, inflater, linearLayoutZonePoints, updatedEquipId, false, p.getGroup(),false);
                                isCPUloaded = true;
                            }
                            if (p.getProfile().startsWith("SMARTSTAT_HEAT_PUMP_UNIT")) {
                                HashMap hpuEquipPoints = ScheduleProcessJob.getHPUEquipPoints(p.getId());
                                Log.i("PointsValue", "HPU Points:" + hpuEquipPoints.toString());
                                loadSSHPUPointsUI(hpuEquipPoints, inflater, linearLayoutZonePoints, updatedEquipId, false, p.getGroup(),false);
                                isHPUloaded = true;
                            }
                        }
                    }
                }
            }
        });
        //return view;
    }


    private void updateTemperatureBasedZones(SeekArc seekArcOpen, View zonePointsOpen, Equip equipOpen, LayoutInflater inflater)
    {
        Equip p = equipOpen;
        View zoneDetails = zonePointsOpen;
        SeekArc seekArc = seekArcOpen;
        String equipId = p.getId();

        LinearLayout linearLayoutZonePoints  = zoneDetails.findViewById(R.id.lt_profilepoints);
        TextView    scheduleStatus      = zoneDetails.findViewById(R.id.schedule_status_tv);
        Spinner scheduleSpinner     = zoneDetails.findViewById(R.id.schedule_spinner);
        ImageButton scheduleImageButton = zoneDetails.findViewById(R.id.schedule_edit_button);
        TextView vacationStatusTV = zoneDetails.findViewById(R.id.vacation_status);



        ArrayAdapter<CharSequence> scheduleAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.schedule, R.layout.spinner_zone_item);
        scheduleAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        scheduleSpinner.setAdapter(scheduleAdapter);

        String zoneId = Schedule.getZoneIdByEquipId(equipId);
        String status = ScheduleProcessJob.getZoneStatusString(zoneId, equipId);
        String vacationStatus = ScheduleProcessJob.getVacationStateString(zoneId);
        Log.i("ZonePoints","zoneId:"+zoneId+" status:"+status+" vacationstatus:"+vacationStatus);

        vacationStatusTV.setText(vacationStatus);
        scheduleStatus.setText(status);
        String scheduleTypeId = CCUHsApi.getInstance().readId("point and scheduleType and equipRef == \""+equipId+"\"");
        mScheduleType = (int)CCUHsApi.getInstance().readPointPriorityVal(scheduleTypeId);

        mSchedule = Schedule.getScheduleByEquipId(equipId);

        scheduleImageButton.setTag(mSchedule.getId());
        scheduleImageButton.setOnClickListener(v ->
        {
            SchedulerFragment schedulerFragment    = SchedulerFragment.newInstance((String) v.getTag());
            FragmentManager childFragmentManager = getFragmentManager();
            childFragmentManager.beginTransaction()
                    .add(R.id.zone_fragment_temp, schedulerFragment)
                    .addToBackStack("schedule").commit();

            schedulerFragment.setOnExitListener(() -> {
                Toast.makeText(v.getContext(), "Refresh View", Toast.LENGTH_LONG).show();
                mSchedule = Schedule.getScheduleByEquipId(equipId);
                ScheduleProcessJob.updateSchedules(equipOpen);


            });
        });
        scheduleSpinner.setSelection(mScheduleType,false);
        if (mSchedule.isZoneSchedule())
        {
            scheduleImageButton.setVisibility(View.VISIBLE);
        } else if (mSchedule.isNamedSchedule())
        {
            scheduleImageButton.setVisibility(View.VISIBLE);
        } else
        {
            scheduleImageButton.setVisibility(View.GONE);
        }


        scheduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (position == 0 && (mScheduleType != -1))
                {
                    if (mSchedule.isZoneSchedule())
                    {
                        mSchedule.setDisabled(true);
                        CCUHsApi.getInstance().updateZoneSchedule(mSchedule,zoneId);
                    }
                    scheduleImageButton.setVisibility(View.GONE);

                    if (mScheduleType != ScheduleType.BUILDING.ordinal()) {
                        setScheduleType(scheduleTypeId, ScheduleType.BUILDING);
                        mScheduleType = ScheduleType.BUILDING.ordinal();
                    }

                    CCUHsApi.getInstance().scheduleSync();
                } else if (position == 1 && (mScheduleType != -1))
                {
                    if (mSchedule.isZoneSchedule() && mSchedule.getMarkers().contains("disabled"))
                    {
                        mSchedule.setDisabled(false);
                        CCUHsApi.getInstance().updateZoneSchedule(mSchedule, zoneId);
                        scheduleImageButton.setTag(mSchedule.getId());
                    } else
                    {

                        Zone     zone         = Schedule.getZoneforEquipId(equipId);
                        Schedule scheduleById = null;
                        if (zone.hasSchedule())
                        {
                            scheduleById = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
                            Log.d(L.TAG_CCU_UI," scheduleType changed to ZoneSchedule : "+scheduleTypeId);
                            scheduleById.setDisabled(false);
                            checkContainment(scheduleById);
                            CCUHsApi.getInstance().updateZoneSchedule(scheduleById, zone.getId());
                        } else if (!zone.hasSchedule())
                        {
                            Log.d(L.TAG_CCU_UI," Zone does not have Schedule : Shouldn't happen");
                            zone.setScheduleRef(DefaultSchedules.generateDefaultSchedule(true, zone.getId()));
                            CCUHsApi.getInstance().updateZone(zone, zone.getId());
                            scheduleById = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
                            //CCUHsApi.getInstance().syncEntityTree();
                        }
                        scheduleImageButton.setTag(scheduleById.getId());
                        scheduleImageButton.setVisibility(View.VISIBLE);
                        CCUHsApi.getInstance().scheduleSync();
                    }
                    if (mScheduleType != ScheduleType.ZONE.ordinal()) {
                        setScheduleType(scheduleTypeId, ScheduleType.ZONE);
                        mScheduleType = ScheduleType.ZONE.ordinal();
                    }
                } else
                {
                    //list named schedules
                }
                mSchedule = Schedule.getScheduleByEquipId(equipId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });

        double pointcurrTmep = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and equipRef == \"" + p.getId() + "\"");
        HashMap buildingMin = CCUHsApi.getInstance().read("building and limit and min and equipRef == \"" + L.ccu().systemProfile.getSystemEquipRef() + "\"");
        HashMap buildingMax = CCUHsApi.getInstance().read("building and limit and max and equipRef == \"" + L.ccu().systemProfile.getSystemEquipRef() + "\"");
        if(pointcurrTmep == 0)
        {
            pointcurrTmep = 72;
        }
        double pointheatDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and heating and equipRef == \"" + p.getId() + "\"");
        double pointcoolDT = CCUHsApi.getInstance().readPointPriorityValByQuery("point and temp and desired and cooling and equipRef == \"" + p.getId() + "\"");
        double pointcoolUL = CCUHsApi.getInstance().readPointPriorityValByQuery("point and limit and max and cooling and user and equipRef == \"" + p.getId() + "\"");
        double pointheatUL = CCUHsApi.getInstance().readPointPriorityValByQuery("point and limit and max and heating and user and equipRef == \"" + p.getId() + "\"");
        double pointcoolLL = CCUHsApi.getInstance().readPointPriorityValByQuery("point and limit and min and cooling and user and equipRef == \"" + p.getId() + "\"");
        double pointheatLL = CCUHsApi.getInstance().readPointPriorityValByQuery("point and limit and min and heating and user and equipRef == \"" + p.getId() + "\"");
        double pointheatDB = CCUHsApi.getInstance().readPointPriorityValByQuery("point and heating and deadband and base and equipRef == \"" + p.getId() + "\"");
        double pointcoolDB = CCUHsApi.getInstance().readPointPriorityValByQuery("point and cooling and deadband and base and equipRef == \"" + p.getId() + "\"");

        float pointbuildingMin = (float)getPointVal(buildingMin.get("id").toString());
        float pointbuildingMax = (float)getPointVal(buildingMax.get("id").toString());

        if(!seekArc.isDetailedView())
        {
            seekArc.setData(false, pointbuildingMin, pointbuildingMax, (float)pointheatUL, (float)pointheatLL, (float)pointcoolLL, (float)pointcoolUL, (float)pointheatDT, (float) pointcoolDT, (float)pointcurrTmep, (float)pointheatDB, (float)pointcoolDB);
        }else {
            seekArc.setData(true, pointbuildingMin, pointbuildingMax, (float)pointheatUL, (float)pointheatLL, (float)pointcoolLL, (float)pointcoolUL, (float)pointheatDT, (float) pointcoolDT, (float)pointcurrTmep, (float)pointheatDB, (float)pointcoolDB);
        }

        linearLayoutZonePoints.removeAllViews();
        for(int k=0;k<openZoneMap.size();k++) {
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
                loadSSCPUPointsUI(cpuEquipPoints, inflater, linearLayoutZonePoints, equipId, true, updatedEquip.getGroup(),false);
                //isCPUloaded = true;
            }
            if (updatedEquip.getProfile().startsWith("SMARTSTAT_HEAT_PUMP_UNIT")) {
                HashMap hpuEquipPoints = ScheduleProcessJob.getHPUEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "HPU Points:" + hpuEquipPoints.toString());
                loadSSHPUPointsUI(hpuEquipPoints, inflater, linearLayoutZonePoints, equipId, true, updatedEquip.getGroup(),false);
                //isHPUloaded = true;
            }
        }
    }

    private void updateNonTemperatureBasedZones(NonTempControl nonTempControlOpen, View zonePointsOpen, Equip equipOpen, LayoutInflater inflater)
    {
        ArrayList<HashMap> zoneMap = openZoneMap;
        Log.i("ProfileTypes","Points:"+zoneMap.toString());
        Equip p = equipOpen;
        View zoneDetails = zonePointsOpen;
        LinearLayout linearLayoutZonePoints  = zoneDetails.findViewById(R.id.lt_profilepoints);
        NonTempControl nonTempControl = nonTempControlOpen;
        HashMap zoneEquips = zoneMap.get(0);

            linearLayoutZonePoints.removeAllViews();
            if (p.getProfile().startsWith("EMR")) {
                LinearLayout ll_status  = zoneDetails.findViewById(R.id.lt_status);
                LinearLayout ll_schedule = zoneDetails.findViewById(R.id.lt_schedule);
                ll_status.setVisibility(View.GONE);
                ll_schedule.setVisibility(View.GONE);
                HashMap emPoints = ScheduleProcessJob.getEMEquipPoints(p.getId());
                Log.i("PointsValue", "EM Points:" + emPoints.toString());
                if(emPoints.size() > 0) {
                    loadEMPointsUI(emPoints, inflater, linearLayoutZonePoints, p.getGroup());
                    double totalEm = (double)emPoints.get("Energy Reading");
                    double currentEm = (double)emPoints.get("Current Rate");
                    int currentValue = new BigDecimal(currentEm).intValue();
                    nonTempControl.setEmCurrentText(String.valueOf(currentValue));
                    nonTempControl.setEmTotalText(String.format("%.0f",totalEm));
                    nonTempControl.setEmCurrentText(String.valueOf(currentValue));
                    nonTempControl.setEmTotalUnitText("KWh");
                    nonTempControl.setEmCurrentUnitText("KW");
                }
            }
            if (p.getProfile().startsWith("PLC")) {
                LinearLayout ll_status  = zoneDetails.findViewById(R.id.lt_status);
                LinearLayout ll_schedule = zoneDetails.findViewById(R.id.lt_schedule);
                ll_status.setVisibility(View.GONE);
                ll_schedule.setVisibility(View.GONE);
                HashMap plcPoints = ScheduleProcessJob.getPiEquipPoints(p.getId());
                if(plcPoints.size() > 0) {
                    Log.i("PointsValue", "PiLoop Points:" + plcPoints.toString());
                    loadPLCPointsUI(plcPoints, inflater, linearLayoutZonePoints, p.getGroup());
                    double targetValue = (double)plcPoints.get("Target Value");
                    double inputValue = (double)plcPoints.get("Input Value");
                    nonTempControl.setPiInputText(String.format("%.2f",inputValue));
                    nonTempControl.setPiOutputText(String.valueOf(targetValue));
                    nonTempControl.setPiInputUnitText(plcPoints.get("Unit").toString());
                    nonTempControl.setPiOutputUnitText(plcPoints.get("Unit").toString());
                }
            }
    }


    private void viewNonTemperatureBasedZone(LayoutInflater inflater, View rootView, ArrayList<HashMap> zoneMap,String zoneTitle, int gridPosition, LinearLayout[] tablerowLayout)
    {
        Log.i("ProfileTypes","Points:"+zoneMap.toString());
        Equip p = new Equip.Builder().setHashMap(zoneMap.get(0)).build();

        String equipId = p.getId();

        int i = gridPosition;
        View arcView = null;
        arcView = inflater.inflate(R.layout.zones_item_nontemp, (ViewGroup) rootView, false);
        View zoneDetails = inflater.inflate(R.layout.zones_item_details, null);
        LinearLayout linearLayoutZonePoints  = zoneDetails.findViewById(R.id.lt_profilepoints);
        GridItem gridItemObj = new GridItem();
        gridItemObj.setGridID(i);
        gridItemObj.setGridItem("NonTemp");
        arcView.setClickable(true);
        arcView.setTag(gridItemObj);
        arcView.setId(i);
        Log.i("EachzoneData","Data:"+zoneMap);
        //ImageView imageView = arcView.findViewById(R.id.imageView);
        NonTempControl nonTempControl = arcView.findViewById(R.id.rl_nontemp);
        //imageView.setTag(gridItemObj);
        nonTempControl.setTag(gridItemObj);
        TextView textEquipment = arcView.findViewById(R.id.textEquipment);
        textEquipment.setText(zoneTitle);
        LinearLayout.LayoutParams rowLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        arcView.setPadding(48,56,0,0);
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
                tableLayout.addView(tablerowLayout[rowcount]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        HashMap zoneEquips = zoneMap.get(0);
        if((zoneEquips.get("profile").toString()).contains("PLC"))
        {
            //imageView.setImageResource(R.drawable.ic_zone_piloop);
            //imageView.setPadding(36,36,36,36);
            nonTempControl.setEquipType(1);
            nonTempControl.setImage(R.drawable.ic_zone_piloop);
            nonTempControl.setImageViewExpanded(R.drawable.ic_zone_piloop_max);
        }if((zoneEquips.get("profile").toString()).contains("TEMP_MONITOR"))
        {
            //imageView.setImageResource(R.drawable.ic_zone_tempmonitor);
            //imageView.setPadding(36,36,36,36);
            nonTempControl.setImage(R.drawable.ic_zone_tempmonitor);
            nonTempControl.setImageViewExpanded(R.drawable.ic_zone_tempmonitor);
        }if((zoneEquips.get("profile").toString()).contains("TEMP_INFLUENCE"))
        {
            //imageView.setImageResource(R.drawable.ic_zone_tempmonitor);
            //imageView.setPadding(36,36,36,36);
            nonTempControl.setImage(R.drawable.ic_zone_tempmonitor);
            nonTempControl.setImageViewExpanded(R.drawable.ic_zone_tempmonitor);
        }if((zoneEquips.get("profile").toString()).contains("EMR"))
        {
            //imageView.setImageResource(R.drawable.ic_zone_em);
            //imageView.setPadding(36,36,36,36);
            //emValues = arcView.findViewById(R.id.emValues);
            nonTempControl.setEquipType(0);
            nonTempControl.setImage(R.drawable.ic_zone_em);
            nonTempControl.setImageViewExpanded(R.drawable.ic_zone_em_max);
        }
        //ScaleImageToNormal(250,210,imageView);
        ScaleControlToNormal(250,210,nonTempControl);
        nonTempControl.setExpand(false);

        //imageView.setOnClickListener(new View.OnClickListener() {
        nonTempControl.setOnClickListener(new View.OnClickListener() {
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
                                        if(viewTag.getGridItem().equals("NonTemp")) {
                                            TextView textViewzone = (TextView) gridItem.findViewById(R.id.textEquipment);
                                            textViewzone.setTextAppearance(getActivity(),R.style.label_black);
                                            textViewzone.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                            tableLayout.removeViewAt(row + 1);
                                            //ImageView imageViewExpanded = gridItem.findViewById(R.id.imageView);
                                            NonTempControl nonTempControl = gridItem.findViewById(R.id.rl_nontemp);
                                            //ScaleImageToNormal(250,210,imageViewExpanded);
                                            ScaleControlToNormal(250,210,nonTempControl);
                                            nonTempControl.setExpand(false);
                                            nonTempControl.setBackgroundColor(getResources().getColor(R.color.white));
                                            gridItem.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                                            gridItem.invalidate();
                                        }else{
                                            SeekArc seekArcExpanded = (SeekArc) gridItem.findViewById(R.id.seekArc);
                                            TextView textViewzone = (TextView) gridItem.findViewById(R.id.textEquipment);
                                            textViewzone.setTextAppearance(getActivity(),R.style.label_black);
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
                            zoneOpen = true;
                            zonePointsOpen = zoneDetails;
                            equipOpen = p;
                            openZoneMap = zoneMap;
                            clickedView = gridItemNew.getGridID();
                            v.setBackgroundColor(getActivity().getResources().getColor(R.color.zoneselection_gray));
                            int index = clickedView / columnCount + 1;
                            //ScaleImageToBig(250,210,imageView);
                            ScaleControlToExpand(250,210,nonTempControl);
                            nonTempControl.setExpand(true);
                            nonTempControlOpen = nonTempControl;
                            imageOn = true;
                            isExpanded = true;
                            try {
                                textEquipment.setTextAppearance(getActivity(),R.style.label_orange);
                                textEquipment.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                                zoneDetails.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                                tableLayout.addView(zoneDetails, index);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (clickposition == clickedView) {
                        v.setBackgroundColor(getResources().getColor(R.color.white));
                        textEquipment.setTextAppearance(getActivity(),R.style.label_black);
                        textEquipment.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                        tableLayout.removeView(zoneDetails);
                        imageOn = false;
                        //ScaleImageToNormal(250,210,imageView);
                        ScaleControlToNormal(250,210,nonTempControl);
                        nonTempControl.setExpand(false);
                        showWeather();
                        clickedView = -1;
                        isExpanded = false;
                    }
                } else {
                    zoneOpen = true;
                    zonePointsOpen = zoneDetails;
                    equipOpen = p;
                    openZoneMap = zoneMap;
                    clickedView = gridItemNew.getGridID();
                    v.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                    int index = clickedView / columnCount + 1;
                    //ScaleImageToBig(250,210,imageView);
                    ScaleControlToExpand(250,210,nonTempControl);
                    nonTempControl.setExpand(true);
                    nonTempControlOpen = nonTempControl;
                    hideWeather();
                    imageOn = true;
                    isExpanded = true;
                    try {
                        textEquipment.setTextAppearance(getActivity(),R.style.label_orange);
                        textEquipment.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        zoneDetails.setBackgroundColor(getResources().getColor(R.color.zoneselection_gray));
                        tableLayout.addView(zoneDetails, index);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if(isExpanded) {
                    linearLayoutZonePoints.removeAllViews();
                    if (p.getProfile().startsWith("EMR")) {

                        nonTempControl.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,nonTempControl.getHeight()));
                        nonTempControl.invalidate();

                        LinearLayout ll_status  = zoneDetails.findViewById(R.id.lt_status);
                        LinearLayout ll_schedule = zoneDetails.findViewById(R.id.lt_schedule);
                        ll_status.setVisibility(View.GONE);
                        ll_schedule.setVisibility(View.GONE);
                        HashMap emPoints = ScheduleProcessJob.getEMEquipPoints(p.getId());
                        Log.i("PointsValue", "EM Points:" + emPoints.toString());
                        loadEMPointsUI(emPoints, inflater, linearLayoutZonePoints,p.getGroup());

                        double energyRead = (double)emPoints.get("Energy Reading");
                        double currentRead = (double)emPoints.get("Current Rate");
                        int currentValue = new BigDecimal(currentRead).intValue();
                        nonTempControl.setEmTotalText(String.format("%.0f",energyRead));
                        nonTempControl.setEmCurrentText(String.valueOf(currentValue));
                        //nonTempControl.setEmTotalText(emPoints.get("Energy Reading").toString());
                        nonTempControl.setEmTotalUnitText("KWh");
                        //nonTempControl.setEmCurrentText(emPoints.get("Current Rate").toString());
                        nonTempControl.setEmCurrentUnitText("KW");

                    } if (p.getProfile().startsWith("PLC")) {
                        LinearLayout ll_status  = zoneDetails.findViewById(R.id.lt_status);
                        LinearLayout ll_schedule = zoneDetails.findViewById(R.id.lt_schedule);
                        ll_status.setVisibility(View.GONE);
                        ll_schedule.setVisibility(View.GONE);
                        HashMap plcPoints = ScheduleProcessJob.getPiEquipPoints(p.getId());
                        Log.i("PointsValue", "PiLoop Points:" + plcPoints.toString());
                        loadPLCPointsUI(plcPoints, inflater, linearLayoutZonePoints,p.getGroup());

                        double targetValue = (double)plcPoints.get("Target Value");
                        double inputValue = (double)plcPoints.get("Input Value");
                        nonTempControl.setPiInputText(String.format("%.2f",inputValue));
                        nonTempControl.setPiOutputText(String.valueOf(targetValue));
                        nonTempControl.setPiInputUnitText(plcPoints.get("Unit").toString());
                        nonTempControl.setPiOutputUnitText(plcPoints.get("Unit").toString());
                    }
                }
            }
        });
    }

    public void loadVAVPointsUI(HashMap vavPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints,String nodeAddress)
    {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);
        View viewPointRow2 = inflater.inflate(R.layout.zones_item_type1, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);

        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);
        TextView textViewLabel3 = viewPointRow2.findViewById(R.id.text_point1label);
        TextView textViewLabel4 = viewPointRow2.findViewById(R.id.text_point2label);

        TextView textViewValue1 = viewPointRow1.findViewById(R.id.text_point1value);
        TextView textViewValue2 = viewPointRow1.findViewById(R.id.text_point2value);
        TextView textViewValue3 = viewPointRow2.findViewById(R.id.text_point1value);
        TextView textViewValue4 = viewPointRow2.findViewById(R.id.text_point2value);

        textViewTitle.setText(vavPoints.get("Profile").toString()+" ("+nodeAddress+")");
        textViewStatus.setText(vavPoints.get("Status").toString());
        textViewLabel1.setText("Damper : ");
        textViewValue1.setText(vavPoints.get("Damper").toString());
        textViewLabel2.setText("Reheat Coil : ");
        textViewValue2.setText(vavPoints.get("Reheat Coil").toString());
        textViewLabel3.setText("Supply Airfow : ");
        textViewValue3.setText(vavPoints.get("Supply Airfow").toString());
        textViewLabel4.setText("Discharge Airfow : ");
        textViewValue4.setText(vavPoints.get("Discharge Airflow").toString());

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        linearLayoutZonePoints.addView(viewPointRow1);
        viewPointRow2.setPadding(0,0,0,40);
        linearLayoutZonePoints.addView(viewPointRow2);
    }
    public void loadSSEPointsUI(HashMap ssePoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String nodeAddress)
    {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);
        textViewLabel2.setVisibility(View.GONE);
        TextView textViewValue1 = viewPointRow1.findViewById(R.id.text_point1value);
        TextView textViewValue2 = viewPointRow1.findViewById(R.id.text_point2value);
        textViewValue2.setVisibility(View.GONE);

        textViewTitle.setText(ssePoints.get("Profile").toString()+" ("+nodeAddress+")");
        textViewStatus.setText(ssePoints.get("Status").toString());
        textViewLabel1.setText("Discharge Airflow : ");
        textViewValue1.setText(ssePoints.get("Discharge Airflow").toString());

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        viewPointRow1.setPadding(0,0,0,40);
        linearLayoutZonePoints.addView(viewPointRow1);
    }
    public void loadTIPointsUI(HashMap tiPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String nodeAddress)
    {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        textViewLabel1.setVisibility(View.GONE);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);
        textViewLabel2.setVisibility(View.GONE);
        TextView textViewValue1 = viewPointRow1.findViewById(R.id.text_point1value);
        textViewValue1.setVisibility(View.GONE);
        TextView textViewValue2 = viewPointRow1.findViewById(R.id.text_point2value);
        textViewValue2.setVisibility(View.GONE);

        textViewTitle.setText(tiPoints.get("Profile").toString()+" ("+nodeAddress+")");
        textViewStatus.setText(tiPoints.get("Status").toString());

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        viewPointRow1.setPadding(0,0,0,40);
        linearLayoutZonePoints.addView(viewPointRow1);
    }
    public void loadDABPointsUI(HashMap dabPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String nodeAddress)
    {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);
        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);
        TextView textViewValue1 = viewPointRow1.findViewById(R.id.text_point1value);
        TextView textViewValue2 = viewPointRow1.findViewById(R.id.text_point2value);

        textViewTitle.setText(dabPoints.get("Profile").toString()+" ("+nodeAddress+")");
        textViewStatus.setText(dabPoints.get("Status").toString());
        textViewLabel1.setText("Damper : ");
        textViewLabel2.setText("Discharge Airflow : ");
        textViewValue1.setText(dabPoints.get("Damper").toString());
        textViewValue2.setText(dabPoints.get("Discharge Airflow").toString());

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        viewPointRow1.setPadding(0,0,0,40);
        linearLayoutZonePoints.addView(viewPointRow1);
    }

    public void loadSSCPUPointsUI(HashMap cpuEquipPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String equipId, boolean isPubNub, String nodeAddress, boolean isLoaded)
    {

        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type2, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);

        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);

        Spinner spinnerValue1 = viewPointRow1.findViewById(R.id.spinnerValue1);
        Spinner spinnerValue2 = viewPointRow1.findViewById(R.id.spinnerValue2);


        ArrayAdapter<CharSequence> conModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_conditionmode, R.layout.spinner_zone_item);
        conModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        spinnerValue1.setAdapter(conModeAdapter);

        ArrayAdapter<CharSequence> fanModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_fanmode, R.layout.spinner_zone_item);
        fanModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        spinnerValue2.setAdapter(fanModeAdapter);

        textViewTitle.setText(cpuEquipPoints.get("Profile").toString()+" ("+nodeAddress+")");
        textViewStatus.setText(cpuEquipPoints.get("Status").toString());
        textViewLabel1.setText("Conditioning Mode : ");
        textViewLabel2.setText("Fan Mode : ");

        int conditionMode = 0;
        int fanMode = 0;
        try {
            conditionMode = (int)((double)cpuEquipPoints.get("Conditioning Mode"));
            fanMode = (int)((double)cpuEquipPoints.get("Fan Mode"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        spinnerValue1.setSelection(conditionMode,false);
        spinnerValue2.setSelection(fanMode,false);

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);

        double fanHighHumdOption = (double)cpuEquipPoints.get("Fan High Humidity");
        double targetHumidity = 0;
        double targetDeHumidity = 0;
        if(fanHighHumdOption > 1.0)
        {
            View viewPointRow2 = inflater.inflate(R.layout.zones_item_type2, null);

            TextView textViewLabel3 = viewPointRow2.findViewById(R.id.text_point1label);
            Spinner spinnerValue3 = viewPointRow2.findViewById(R.id.spinnerValue1);
            TextView textViewLabel4 = viewPointRow2.findViewById(R.id.text_point2label);
            textViewLabel4.setVisibility(View.GONE);
            Spinner spinnerValue4 = viewPointRow2.findViewById(R.id.spinnerValue2);
            spinnerValue4.setVisibility(View.GONE);


            ArrayList<Integer> arrayHumdityTargetList = new ArrayList<Integer>();
            for (int pos = 1; pos <= 100; pos++)
                arrayHumdityTargetList.add(pos);

            ArrayAdapter<Integer> humidityTargetAdapter = new ArrayAdapter<Integer>(getActivity(),R.layout.spinner_zone_item,arrayHumdityTargetList);
            humidityTargetAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
            spinnerValue3.setAdapter(humidityTargetAdapter);

            if(fanHighHumdOption == 2.0) {
                textViewLabel3.setText("Target Humidity : ");
                targetHumidity = (double)cpuEquipPoints.get("Target Humidity");
                spinnerValue3.setSelection((int)targetHumidity -1);
            }else {
                textViewLabel3.setText("Target Dehumidify : ");
                targetDeHumidity = (double)cpuEquipPoints.get("Target Dehumidity");
                spinnerValue3.setSelection((int)targetDeHumidity - 1);
            }

            linearLayoutZonePoints.addView(viewPointRow2);
        }


        linearLayoutZonePoints.addView(viewPointRow1);
        viewPointRow1.setPadding(0,0,0,40);
        try {
            linearLayoutZonePoints.addView(viewPointRow1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int tempConditionMode = conditionMode;
        int tempFanMode = fanMode;
        //isFromPubNub = isPubNub;
        isCPUFromPubNub = isPubNub;
        isCPUloaded = isLoaded;
        String tempEquipId = equipId;
        spinnerValue1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(isCPUFromPubNub) {
                    if (tempConditionMode != position) {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "temp and operation and mode", position);
                    }
                    //isCPUFromPubNub = false;
                }else {
                    //if(isCPUloaded) {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "temp and operation and mode", position);
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
                if(isCPUFromPubNub) {
                    if (tempFanMode != position) {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                    }
                    isCPUFromPubNub = false;
                }else {
                    //if(isCPUloaded) {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                    //}
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //isCPUloaded = false;

    }
    public void loadSSHPUPointsUI(HashMap hpuEquipPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String equipId, boolean isPubNub, String nodeAddress, boolean isLoaded)
    {
        //Log.i("PubNub","UI Update for Zone Points");
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type2, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);

        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);

        Spinner spinnerValue1 = viewPointRow1.findViewById(R.id.spinnerValue1);
        Spinner spinnerValue2 = viewPointRow1.findViewById(R.id.spinnerValue2);

        ArrayAdapter<CharSequence> conModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_conditionmode, R.layout.spinner_zone_item);
        conModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        spinnerValue1.setAdapter(conModeAdapter);

        ArrayAdapter<CharSequence> fanModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_fanmode, R.layout.spinner_zone_item);
        fanModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        spinnerValue2.setAdapter(fanModeAdapter);

        textViewTitle.setText(hpuEquipPoints.get("Profile").toString()+" ("+nodeAddress+")");
        textViewStatus.setText(hpuEquipPoints.get("Status").toString());
        textViewLabel1.setText("Conditioning Mode : ");
        textViewLabel2.setText("Fan Mode : ");

        pointsOpen.put(hpuEquipPoints.get("StatusTag"),"StatusTag");
        pointsOpen.put(hpuEquipPoints.get("FanModeTag"),"FanModeTag");
        pointsOpen.put(hpuEquipPoints.get("ConditionModeTag"),"ConditionModeTag");

        int conditionMode = 0;
        int fanMode = 0;
        try {
            conditionMode = (int)(double)(hpuEquipPoints.get("Conditioning Mode"));
            fanMode = (int)(double)(hpuEquipPoints.get("Fan Mode"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        spinnerValue1.setSelection(conditionMode,false);
        spinnerValue2.setSelection(fanMode,false);

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);

        double fanHighHumdOption = (double)hpuEquipPoints.get("Fan High Humidity");
        double targetHumidity = 0;
        double targetDeHumidity = 0;
        if(fanHighHumdOption > 1.0)
        {
            View viewPointRow2 = inflater.inflate(R.layout.zones_item_type2, null);

            TextView textViewLabel3 = viewPointRow2.findViewById(R.id.text_point1label);
            Spinner spinnerValue3 = viewPointRow2.findViewById(R.id.spinnerValue1);
            TextView textViewLabel4 = viewPointRow2.findViewById(R.id.text_point2label);
            textViewLabel4.setVisibility(View.GONE);
            Spinner spinnerValue4 = viewPointRow2.findViewById(R.id.spinnerValue2);
            spinnerValue4.setVisibility(View.GONE);


            ArrayList<Integer> arrayHumdityTargetList = new ArrayList<Integer>();
            for (int pos = 1; pos <= 100; pos++)
                arrayHumdityTargetList.add(pos);

            ArrayAdapter<Integer> humidityTargetAdapter = new ArrayAdapter<Integer>(getActivity(),R.layout.spinner_zone_item,arrayHumdityTargetList);
            humidityTargetAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
            spinnerValue3.setAdapter(humidityTargetAdapter);

            if(fanHighHumdOption == 2.0) {
                textViewLabel3.setText("Target Humidity : ");
                targetHumidity = (double)hpuEquipPoints.get("Target Humidity");
                spinnerValue3.setSelection((int)targetHumidity -1);
            }else {
                textViewLabel3.setText("Target Dehumidify : ");
                targetDeHumidity = (double)hpuEquipPoints.get("Target Dehumidity");
                spinnerValue3.setSelection((int)targetDeHumidity - 1);
            }

            linearLayoutZonePoints.addView(viewPointRow2);
        }


        linearLayoutZonePoints.addView(viewPointRow1);
        viewPointRow1.setPadding(0,0,0,40);
        try {
            linearLayoutZonePoints.addView(viewPointRow1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int tempConditionMode = conditionMode;
        int tempfanMode = fanMode;
        //isFromPubNub = isPubNub;
        isHPUFromPubNub = isPubNub;
        isHPUloaded = isLoaded;
        String tempEquipId = equipId;
        spinnerValue1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    if(isHPUFromPubNub) {
                        if (tempConditionMode != position) {
                            Log.i("PubNub", "conditionMode:" + tempConditionMode + " position:" + position);
                            StandaloneScheduler.updateOperationalPoints(tempEquipId, "temp and operation and mode", position);
                        }
                        //isFromPubNub = false;
                    }else{
                        //if(isHPUloaded) {
                            StandaloneScheduler.updateOperationalPoints(tempEquipId, "temp and operation and mode", position);
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

        spinnerValue2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    if(isHPUFromPubNub) {
                        if(tempfanMode!= position) {
                            Log.i("PubNub","fanMode:"+tempfanMode+" position:"+position);
                            StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                        }
                        isHPUFromPubNub = false;
                    }
                    else {
                        //if(isHPUloaded) {
                            StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
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
    public void loadSS2PFCUPointsUI(HashMap p2FCUPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String equipId, boolean isPubNub, String nodeAddress)
    {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type2, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);

        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);

        Spinner spinnerValue1 = viewPointRow1.findViewById(R.id.spinnerValue1);
        Spinner spinnerValue2 = viewPointRow1.findViewById(R.id.spinnerValue2);


        ArrayAdapter<CharSequence> conModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_conditionmode, R.layout.spinner_zone_item);
        conModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        spinnerValue1.setAdapter(conModeAdapter);

        ArrayAdapter<CharSequence> fanModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_2pfcu_fanmode, R.layout.spinner_zone_item);
        fanModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        spinnerValue2.setAdapter(fanModeAdapter);

        textViewTitle.setText(p2FCUPoints.get("Profile").toString()+" ("+nodeAddress+")");
        textViewStatus.setText(p2FCUPoints.get("Status").toString());
        textViewLabel1.setText("Conditioning Mode : ");
        textViewLabel2.setText("Fan Mode : ");

        int conditionMode = 0;
        int fanMode = 0;
        try {
            conditionMode = (int)((double)p2FCUPoints.get("Conditioning Mode"));
            fanMode = (int)((double)p2FCUPoints.get("Fan Mode"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        spinnerValue1.setSelection(conditionMode,false);
        spinnerValue2.setSelection(fanMode,false);

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        linearLayoutZonePoints.addView(viewPointRow1);
        viewPointRow1.setPadding(0,0,0,40);
        try {
            linearLayoutZonePoints.addView(viewPointRow1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int tempConditionMode = conditionMode;
        int tempfanMode = fanMode;
        isFromPubNub = isPubNub;
        String tempEquipId = equipId;
        spinnerValue1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(isFromPubNub){
                    if(tempConditionMode != position) {
                    StandaloneScheduler.updateOperationalPoints(tempEquipId, "temp and operation and mode", position);
                    }
                    //isFromPubNub = false;
                }else{
                    StandaloneScheduler.updateOperationalPoints(tempEquipId, "temp and operation and mode", position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerValue2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(isFromPubNub) {
                    if(tempfanMode != position) {
                        StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                    }
                    isFromPubNub = false;
                }else {
                    StandaloneScheduler.updateOperationalPoints(tempEquipId, "fan and operation and mode", position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void loadSS4PFCUPointsUI(HashMap p4FCUPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String equipId, boolean isPubNub, String nodeAddress)
    {
        //boolean isFromPubNub = isPubNub;
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type2, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);

        TextView textViewLabel1 = viewPointRow1.findViewById(R.id.text_point1label);
        TextView textViewLabel2 = viewPointRow1.findViewById(R.id.text_point2label);

        Spinner spinnerValue1 = viewPointRow1.findViewById(R.id.spinnerValue1);
        Spinner spinnerValue2 = viewPointRow1.findViewById(R.id.spinnerValue2);


        ArrayAdapter<CharSequence> conModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_conditionmode, R.layout.spinner_zone_item);
        conModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        spinnerValue1.setAdapter(conModeAdapter);

        ArrayAdapter<CharSequence> fanModeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.smartstat_2pfcu_fanmode, R.layout.spinner_zone_item);
        fanModeAdapter.setDropDownViewResource(R.layout.spinner_item_grey);
        spinnerValue2.setAdapter(fanModeAdapter);

        textViewTitle.setText(p4FCUPoints.get("Profile").toString()+" ("+nodeAddress+")");
        textViewStatus.setText(p4FCUPoints.get("Status").toString());
        textViewLabel1.setText("Conditioning Mode : ");
        textViewLabel2.setText("Fan Mode : ");

        int conditionMode = 0;
        int fanMode = 0;
        try {
            conditionMode = (int)((double)p4FCUPoints.get("Conditioning Mode"));
            fanMode = (int)((double)p4FCUPoints.get("Fan Mode"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        spinnerValue1.setSelection(conditionMode,false);
        spinnerValue2.setSelection(fanMode,false);

        linearLayoutZonePoints.addView(viewTitle);
        linearLayoutZonePoints.addView(viewStatus);
        linearLayoutZonePoints.addView(viewPointRow1);
        viewPointRow1.setPadding(0,0,0,40);
        try {
            linearLayoutZonePoints.addView(viewPointRow1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int tempConditionMode = conditionMode;
        int tempFanMode = fanMode;
        isFromPubNub = isPubNub;
        spinnerValue1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(isFromPubNub)
                {
                    if(tempConditionMode != position) {
                        StandaloneScheduler.updateOperationalPoints(equipId, "temp and operation and mode", position);
                    }
                    isFromPubNub = false;
                }
                else{
                    StandaloneScheduler.updateOperationalPoints(equipId, "temp and operation and mode", position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerValue2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(isFromPubNub) {
                    if (tempFanMode != position) {
                        StandaloneScheduler.updateOperationalPoints(equipId, "fan and operation and mode", position);
                    }
                    isFromPubNub = false;
                }else {
                    StandaloneScheduler.updateOperationalPoints(equipId, "fan and operation and mode", position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    public void loadEMPointsUI(HashMap p4FCUPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String nodeAddress)
    {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);

        textViewTitle.setText(p4FCUPoints.get("Profile").toString()+" ("+nodeAddress+")");
        textViewStatus.setText(p4FCUPoints.get("Status").toString());
        viewStatus.setPadding(0,0,0,40);
        try {
            linearLayoutZonePoints.addView(viewTitle);
            linearLayoutZonePoints.addView(viewStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void loadPLCPointsUI(HashMap plcPoints, LayoutInflater inflater, LinearLayout linearLayoutZonePoints, String nodeAddress)
    {
        View viewTitle = inflater.inflate(R.layout.zones_item_title, null);
        View viewStatus = inflater.inflate(R.layout.zones_item_status, null);
        View viewPointRow1 = inflater.inflate(R.layout.zones_item_type1, null);
        View viewPointRow2 = inflater.inflate(R.layout.zones_item_type1, null);

        TextView textViewTitle = viewTitle.findViewById(R.id.textProfile);
        TextView textViewStatus = viewStatus.findViewById(R.id.text_status);

        TextView labelInputAir = viewPointRow1.findViewById(R.id.text_point1label);
        TextView labelTarget = viewPointRow1.findViewById(R.id.text_point2label);
        TextView labelOffsetAir = viewPointRow2.findViewById(R.id.text_point1label);
        TextView label2 = viewPointRow2.findViewById(R.id.text_point2label);

        TextView textViewInputAir = viewPointRow1.findViewById(R.id.text_point1value);
        TextView textViewTargetAir = viewPointRow1.findViewById(R.id.text_point2value);
        TextView textViewOffsetAir = viewPointRow2.findViewById(R.id.text_point1value);
        TextView value2 = viewPointRow2.findViewById(R.id.text_point2value);

        label2.setVisibility(View.GONE);
        value2.setVisibility(View.GONE);

        textViewTitle.setText(plcPoints.get("Profile").toString()+" ("+nodeAddress+")");
        textViewStatus.setText(plcPoints.get("Status").toString());

        labelInputAir.setText("Input  "+plcPoints.get("Unit Type").toString()+" : ");
        labelTarget.setText("Target "+plcPoints.get("Unit Type").toString()+" : ");

        double processValue = (double)plcPoints.get("Input Value");
        textViewInputAir.setText(String.format("%.2f",processValue)+plcPoints.get("Unit").toString());
        textViewTargetAir.setText(plcPoints.get("Target Value").toString()+plcPoints.get("Unit").toString());

        try {
            if((boolean)plcPoints.get("Dynamic Setpoint") == true)
            {
                    labelOffsetAir.setText("Offset "+plcPoints.get("Unit Type").toString()+" : ");
                    textViewOffsetAir.setText(plcPoints.get("Offset Value").toString()+plcPoints.get("Unit").toString());
                    viewPointRow2.setPadding(0,0,0,40);
                    linearLayoutZonePoints.addView(viewTitle);
                    linearLayoutZonePoints.addView(viewStatus);
                    linearLayoutZonePoints.addView(viewPointRow1);
                    linearLayoutZonePoints.addView(viewPointRow2);

            }else{
                    viewPointRow1.setPadding(0,0,0,40);
                    linearLayoutZonePoints.addView(viewTitle);
                    linearLayoutZonePoints.addView(viewStatus);
                    linearLayoutZonePoints.addView(viewPointRow1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static double getPointVal(String id) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Point p = new Point.Builder().setHashMap(hayStack.readMapById(id)).build();
        for (String marker : p.getMarkers())
        {
            if (marker.equals("writable"))
            {
                ArrayList values = hayStack.readPoint(id);
                if (values != null && values.size() > 0)
                {
                    for (int l = 1; l <= values.size(); l++)
                    {
                        HashMap valMap = ((HashMap) values.get(l - 1));
                        System.out.println(valMap);
                        if (valMap.get("val") != null)
                        {
                            return Double.parseDouble(valMap.get("val").toString());
                        }
                    }
                }
            }
        }
    
        for (String marker : p.getMarkers())
        {
            if (marker.equals("his"))
            {
                return hayStack.readHisValById(p.getId());
            }
        }
        return 0;
    }
    
    public void setPointVal(String coolid, double coolval,String heatid, double heatval, String avgid) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                CCUHsApi hayStack = CCUHsApi.getInstance();
                Point coolpoint = new Point.Builder().setHashMap(hayStack.readMapById(coolid)).build();
                Point heatpoint = new Point.Builder().setHashMap(hayStack.readMapById(heatid)).build();
                Point avgpoint = new Point.Builder().setHashMap(hayStack.readMapById(avgid)).build();
                double avgval = (coolval + heatval) / 2.0;
                if (coolpoint.getMarkers().contains("writable"))
                {
                    CcuLog.d(L.TAG_CCU_UI, "Set Writbale Val "+coolpoint.getDisplayName()+": " +coolid+","+heatpoint.getDisplayName()+","+heatval+","+avgpoint.getDisplayName());
                    ScheduleProcessJob.handleDesiredTempUpdate(coolpoint, heatpoint,avgpoint,true, coolval, heatval,avgval);

                }

                if (coolpoint.getMarkers().contains("his"))
                {
                    CcuLog.d(L.TAG_CCU_UI, "Set His Val "+coolid+": " +coolval);
                    hayStack.writeHisValById(coolid, coolval);
                }
                if (heatpoint.getMarkers().contains("his"))
                {
                    CcuLog.d(L.TAG_CCU_UI, "Set His Val "+heatid+": " +heatval);
                    hayStack.writeHisValById(heatid, heatval);
                }
                if (avgpoint.getMarkers().contains("his"))
                {
                    CcuLog.d(L.TAG_CCU_UI, "Set His Val "+avgid+": " +avgval);
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
        if(getUserVisibleHint())
        {
            UpdatePointHandler.setZoneDataInterface(this);
            Pulse.setCurrentTempInterface(this);
            ScheduleProcessJob.setScheduleDataInterface(this);
            ScheduleProcessJob.setZoneDataInterface(this);
        }
        weatherUpdateHandler = new Handler();
        weatherUpdate = new Runnable() {
            @Override
            public void run() {
                if (weatherUpdateHandler != null && getActivity() != null) {
                    if (weather_data.getVisibility() == View.VISIBLE) {
                        Log.e("weather", "update");
                        UpdateWeatherData();
                    }
                    weatherUpdateHandler.postDelayed(weatherUpdate, 15 * 60000);
                }
            }
        };

            weatherUpdate.run();
        }
    @Override
    public void onPause() {
        super.onPause();
        UpdatePointHandler.setZoneDataInterface(null);
        Pulse.setCurrentTempInterface(null);
        ScheduleProcessJob.setScheduleDataInterface(null);
        ScheduleProcessJob.setZoneDataInterface(null);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser) {
            UpdatePointHandler.setZoneDataInterface(this);
            Pulse.setCurrentTempInterface(this);
            ScheduleProcessJob.setScheduleDataInterface(this);
            ScheduleProcessJob.setZoneDataInterface(this);
        } else {

            UpdatePointHandler.setZoneDataInterface(null);
            Pulse.setCurrentTempInterface(null);
            ScheduleProcessJob.setScheduleDataInterface(null);
            ScheduleProcessJob.setZoneDataInterface(null);
        }
    }

    class FloorComparator implements Comparator<Floor>
    {
        @Override
        public int compare(Floor a, Floor b) {
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        }
    }

    public void ScaleImageToNormal(int height,int width, ImageView imageView)
    {
        int endHeight = (int) (height / 1.35);
        int endWidth = (int) (width / 1.35);
        imageView.getLayoutParams().height = endHeight;
        imageView.getLayoutParams().width = endWidth;
        imageView.setPadding(36,36,36,36);
    }

    public void ScaleControlToNormal(int height,int width, NonTempControl imageView)
    {
        int endHeight = (int) (height / 1.35);
        int endWidth = (int) (width / 1.35);
        imageView.getLayoutParams().height = endHeight;
        imageView.getLayoutParams().width = endWidth;
    }

    public void ScaleControlToExpand(int height, int width, NonTempControl imageView)
    {
        int endHeight = (int) (height * 1.35);
        int endWidth = (int) (width * 1.35);
        imageView.getLayoutParams().height = endHeight;
        imageView.getLayoutParams().width = endWidth;
        //imageView.setPadding(20 ,20,20,20);
    }

    public void ScaleImageToBig(int height, int width, ImageView imageView)
    {
        int endHeight = (int) (height * 1.35);
        int endWidth = (int) (width * 1.35);
        imageView.getLayoutParams().height = endHeight;
        imageView.getLayoutParams().width = endWidth;
        imageView.setPadding(36,36,36,36);
    }
    public void showWeather() {
        //if (isWeatherWidget) {
            weather_data.setVisibility(View.VISIBLE);
            //mod = 3;
            //weather_appear.setVisibility(View.GONE);
            //weather_data.startAnimation(inleft);
        TranslateAnimation animate = new TranslateAnimation(-weather_data.getWidth(),0,0,0);
        animate.setDuration(400);
        animate.setFillAfter(true);
        weather_data.startAnimation(animate);
        //}
    }

    public void hideWeather() {
        //if (isWeatherWidget) {
            //mod = 4;
            //weather_appear.setVisibility(View.VISIBLE);
            TranslateAnimation animate = new TranslateAnimation(0,-weather_data.getWidth()+5,0,0);
            animate.setDuration(400);
            animate.setFillAfter(true);
            weather_data.startAnimation(animate);
            //recyclerView.startAnimation(animate);
            //recyclerView.startAnimation(in);
            //gridlayout.startAnimation(in);
            tableLayout.startAnimation(in);
            //tableLayout.setVisibility(View.VISIBLE);
            weather_data.setVisibility(View.GONE);
            //weather_data.setVisibility(View.VISIBLE);

        //}
    }

    public boolean isWeatherShown(){
        boolean isShown = false;
        if(weather_data.getVisibility() == View.VISIBLE)
        {
            isShown = true;
        }
        else
        {
            isShown = false;
        }
        return isShown;
    }

    private void setScheduleType(String id, ScheduleType schedule) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground( final String ... params ) {
                CCUHsApi.getInstance().writeDefaultValById(id, (double)schedule.ordinal());
                ScheduleProcessJob.handleScheduleTypeUpdate(new Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(id)).build());
                return null;
            }

            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }
    
    
    private boolean checkContainment(Schedule zoneSchedule) {
        Schedule systemSchedule = CCUHsApi.getInstance().getSystemSchedule(false).get(0);
        ArrayList<Interval> intervalSpills = new ArrayList<>();
        ArrayList<Interval> systemIntervals = systemSchedule.getMergedIntervals();
        
        for (Interval v : systemIntervals)
        {
            CcuLog.d("CCU_UI", "Merged System interval " + v);
        }
        
        ArrayList<Interval> zoneIntervals = zoneSchedule.getScheduledIntervals();
        
        for(Interval z : zoneIntervals) {
            boolean add = true;
            for (Interval s: systemIntervals) {
                if (s.contains(z)) {
                    add = false;
                    break;
                } else if (s.overlaps(z)) {
                    if (z.getStartMillis() < s.getStartMillis()) {
                        intervalSpills.add(new Interval(z.getStartMillis(), s.getStartMillis()));
                    } else if (z.getEndMillis() > s.getEndMillis()) {
                        intervalSpills.add(new Interval(s.getEndMillis(), z.getEndMillis()));
                    }
                    add = false;
                }
            }
            if (add)
            {
                intervalSpills.add(z);
                CcuLog.d("CCU_UI", " Zone Interval not contained "+z);
            }
            
        }
        
        
        if (intervalSpills.size() > 0) {
            StringBuilder spillZones = new StringBuilder();
            for (Interval i : intervalSpills)
            {
                spillZones.append(ScheduleUtil.getDayString(i.getStart().getDayOfWeek()) + " (" + i.getStart().hourOfDay().get() + ":" + (i.getStart().minuteOfHour().get() == 0 ? "00" : i.getStart().minuteOfHour().get()) + " - " + i.getEnd().hourOfDay().get() + ":" + (i.getEnd().minuteOfHour().get() == 0 ? "00" : i.getEnd().minuteOfHour().get()) + ") \n");
            }
            
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
            builder.setMessage("Zone Schedule is outside building schedule currently set. " +
                               "Proceed with trimming the zone schedules to be within the building schedule \n"+spillZones)
                   .setCancelable(false)
                   .setTitle("Schedule Errors")
                   .setIcon(android.R.drawable.ic_dialog_alert)
                   .setNegativeButton("Edit", new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           SchedulerFragment schedulerFragment    = SchedulerFragment.newInstance(zoneSchedule.getId());
                           FragmentManager   childFragmentManager = getFragmentManager();
                           childFragmentManager.beginTransaction()
                                               .add(R.id.zone_fragment_temp, schedulerFragment)
                                               .addToBackStack("schedule").commit();
                    
                           schedulerFragment.setOnExitListener(() -> {
                               mSchedule = CCUHsApi.getInstance().getScheduleById(zoneSchedule.getId());
                               if (checkContainment(mSchedule))
                               {
                                   ScheduleProcessJob.updateSchedules();
                               }
                           });
                       }
                   })
                   .setPositiveButton("Force-Trim", new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           HashMap<String, ArrayList<Interval>> spillsMap = new HashMap<>();
                           spillsMap.put(zoneSchedule.getRoomRef(), intervalSpills);
                           ScheduleUtil.trimZoneSchedules(spillsMap);
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
    
}