package a75f.io.renatus;
import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.device.mesh.ThermistorUtil;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneProfile;
import android.view.View.OnClickListener;
import a75f.io.renatus.util.ZoneSorter;
import butterknife.BindView;
import butterknife.ButterKnife;

import static a75f.io.renatus.RenatusLandingActivity.btnTabs;
import static a75f.io.renatus.RenatusLandingActivity.mTabLayout;
import static a75f.io.renatus.RenatusLandingActivity.mViewPager;

public class TempOverrideFragment extends Fragment {

    ExpandableListView expandableListView;
    TempOverrideExpandableListAdapter expandableListAdapter;
    List<String> expandableListTitle;
    TreeMap<String, List<String>> expandableListDetail;
    HashMap<String, List<String>> expandableListDetail_CMDevice;
    private TempOverRiddenValue tempOverRiddenValue;


    TreeMap<String, String> pointMap = new TreeMap();
    HashMap<String, String> equipMap = new HashMap();
    int lastExpandedPosition;
    String siteName;
    private LinearLayout mRoot;
    Snackbar snackbar;

    @BindView(R.id.countdownTimer)
    TextView mCountdownView;
    @BindView(R.id.btnReset)
    Button btnReset;
    FrameLayout flTOContents;

    private static CountDownTimer countDownTimer;

    public TempOverrideFragment() {

    }

    public static TempOverrideFragment newInstance() {
        return new TempOverrideFragment();
    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_temp_override, container, false);
        ButterKnife.bind(this, rootView);
        snackbar = Snackbar.make(container, R.string.temproryOverride_Warningmessage, Snackbar.LENGTH_INDEFINITE);

        flTOContents = (FrameLayout) rootView.findViewById(R.id.fl_TO_Contents);
        //RenatusLandingActivity.snackbar.show();
        loadExistingZones();
        startCountDownTimer();

        showWarningMssgForTO();
        snackbar.show();

        btnReset.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopCountdownTimer();
                startCountDownTimer();
            }
        });

        return rootView;
    }

    private void showWarningMssgForTO() {
        View snackbarView = snackbar.getView();
        TextView snackTextView = (TextView) snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        snackbar.setAction("CLOSE", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();

                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) flTOContents.getLayoutParams();
                lp.setMargins(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.CENTER_HORIZONTAL,0);
                flTOContents.setLayoutParams(lp);
            }
        });
        snackTextView.setMaxLines(2);
    }

    private void startCountDownTimer() {
        countDownTimer = new CountDownTimer(3601000, 1000) {
            @Override
            public void onTick(long l) {
                long millis = l;
                @SuppressLint("DefaultLocale") String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis), TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
                mCountdownView.setText(hms);
            }

            @Override
            public void onFinish() {
                onStop();
                if (getActivity() != null) {
                    StatusPagerAdapter mStatusPagerAdapter = new StatusPagerAdapter(getActivity().getSupportFragmentManager());
                    mViewPager.setAdapter(mStatusPagerAdapter);
                    btnTabs.getTabAt(1).select();
                    mTabLayout.post(() -> mTabLayout.setupWithViewPager(mViewPager, true));
                }
            }
        };
        countDownTimer.start();
    }

    public static void stopCountdownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    ArrayList<Floor> siteFloorList = new ArrayList<>();
    ArrayList<String> siteRoomList = new ArrayList<>();

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
    @Override
    public void onStop() {
        super.onStop();
        Globals.getInstance().setTemporaryOverrideMode(false);
        Globals.getInstance().resetTempOverCount();
        //RenatusLandingActivity.snackbar.dismiss();
        stopCountdownTimer();
        tempOverRiddenValue = TempOverRiddenValue.getInstance();
        for (Map.Entry<String, String> overrideVal : tempOverRiddenValue.getAllItems()) {
            String dataToReset = overrideVal.getValue();
            if (dataToReset != null) {
                String[] parts = dataToReset.split("-value-");
                String id = parts[0];
                String value1 = parts[1];
                Double val = Double.valueOf(value1);
                if (overrideVal.getKey().startsWith("Th"))
                    setPointValForThermistor(id, val);
                else setPointVal(id, val);
            }
        }
        tempOverRiddenValue.clear();
        snackbar.dismiss();
    }

    public void setPointVal(String id, double val) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        hayStack.writeHisValById(id, val);
        Object logicalPoint = hayStack.readMapById(id).get("pointRef");
        if (Objects.nonNull(logicalPoint)) {
            //hayStack.writeHisValById(logicalPoint.toString(), val / 1000);
            hayStack.writeHisValById(logicalPoint.toString(), val);
        }
    }

    public void setPointValForThermistor(String id, double val) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        hayStack.writeHisValById(id, val);
        Object logicalPoint = hayStack.readMapById(id).get("pointRef");
        if (Objects.nonNull(logicalPoint)) {
            hayStack.writeHisValById(logicalPoint.toString(), ThermistorUtil.getThermistorValueToTemp(val));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        expandableListView = view.findViewById(R.id.expandableListView);
        expandableListDetail = new TreeMap<>();
        expandableListDetail_CMDevice = new HashMap<>();
        siteName = CCUHsApi.getInstance().read("site").get("dis").toString();

        //ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone and roomRef");
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and group");
        ArrayList<String> equipsRef = new ArrayList<String>();
        for (int i = 0; i < equips.size(); i++) {
            equipsRef.add(i, equips.get(i).get("roomRef").toString());
        }
        Collections.sort(equipsRef, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        Collections.reverse(equipsRef);

        ArrayList<HashMap> Zonedevices = CCUHsApi.getInstance().readAll("device");
        for (Map m : Zonedevices) {
            ArrayList<HashMap> tuners = CCUHsApi.getInstance().readAll("point and his and deviceRef == \"" + m.get("id") + "\"");
            ArrayList tunerList = new ArrayList();
            ArrayList newTunerList = new ArrayList();
            for (Map t : tuners) {
                if (t.get("dis").toString().startsWith("Analog1In") || t.get("dis").toString().startsWith("Analog1Out") || t.get("dis").toString().startsWith("Analog2In") ||
                        t.get("dis").toString().startsWith("Analog2Out") || t.get("dis").toString().startsWith("relay") || t.get("dis").toString().startsWith("Th") ||
                        t.get("dis").toString().startsWith(siteName) && Objects.nonNull(t.get("dis").toString())) {
                    String NewexpandedListText = t.get("dis").toString();
                    if (NewexpandedListText.startsWith("Analog")) {
                        tunerList.add(t.get("dis").toString());
                    } else if (NewexpandedListText.startsWith("relay")) {
                        tunerList.add(t.get("dis").toString());
                    } else if (NewexpandedListText.startsWith("Th")) {
                        tunerList.add(t.get("dis").toString());
                    } else if (NewexpandedListText.startsWith(siteName)) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, t.get("dis").toString().substring(siteName.length() + 1, t.get("dis").toString().length()));
                        if (NewexpandedListText.startsWith("CM-analog1Out")) {
                            tunerList.add(t.get("dis").toString());
                        } else if (NewexpandedListText.startsWith("CM-analog2Out")) {
                            //Log.e("InsideTempOverrideFrag","NewexpandedListText- "+NewexpandedListText + ", IsgetConfigEnabled- "+getConfigEnabled("analog2"));
                            tunerList.add(t.get("dis").toString());
                        } else if (NewexpandedListText.startsWith("CM-analog3Out")) {
                            tunerList.add(t.get("dis").toString());
                        } else if (NewexpandedListText.startsWith("CM-analog4Out")) {
                            tunerList.add(t.get("dis").toString());
                        } else if (NewexpandedListText.startsWith("relay")) {
                            String relayPos = (t.get("dis").toString().substring(siteName.length() + 6, siteName.length() + 7));
                            tunerList.add(t.get("dis").toString());

                        }
                    }
                    //tunerList.add(t.get("dis").toString());
                    Collections.sort(tunerList, new Comparator<String>() {
                        @Override
                        public int compare(String s1, String s2) {
                            return s1.compareToIgnoreCase(s2);
                        }
                    });

                    newTunerList.clear();
                    for(Object point : tunerList) {
                        if (point.toString().contains("Analog1In")){
                            if (!newTunerList.contains(point)) {

                                newTunerList.add(point);
                            }
                        }
                    }

                    for(Object point : tunerList) {
                        if (point.toString().contains("Analog2In")){
                            if (!newTunerList.contains(point)) {

                                newTunerList.add(point);
                            }
                        }
                    }

                    for(Object point : tunerList) {
                        if (point.toString().contains("Analog3In")){
                            if (!newTunerList.contains(point)) {

                                newTunerList.add(point);
                            }
                        }
                    }

                    for(Object point : tunerList) {
                        if (point.toString().contains("Analog1Out") || point.toString().contains("analog1Out")){
                            if (!newTunerList.contains(point)) {

                                newTunerList.add(point);
                            }
                        }
                    }

                    for(Object point : tunerList) {
                        if (point.toString().contains("Analog2Out") || point.toString().contains("analog2Out")){
                            if (!newTunerList.contains(point)) {

                                newTunerList.add(point);
                            }
                        }
                    }

                    for(Object point : tunerList) {
                        if (point.toString().contains("Analog3Out") || point.toString().contains("analog3Out")){
                            if (!newTunerList.contains(point)) {

                                newTunerList.add(point);
                            }
                        }
                    }

                    for(Object point : tunerList) {
                        if (point.toString().contains("Analog4Out") || point.toString().contains("analog4Out")){
                            if (!newTunerList.contains(point)) {

                                newTunerList.add(point);
                            }
                        }
                    }
                    for(Object point : tunerList) {
                        if (point.toString().contains("Th1In")){
                            if (!newTunerList.contains(point)) {

                                newTunerList.add(point);
                            }
                        }
                    }

                    for(Object point : tunerList) {
                        if (point.toString().contains("Th2In")){
                            if (!newTunerList.contains(point)) {

                                newTunerList.add(point);
                            }
                        }
                    }

                    for(Object point : tunerList) {
                        if (point.toString().contains("relay")){
                            if (!newTunerList.contains(point)) {

                                newTunerList.add(point);
                            }
                        }
                    }
                    pointMap.put(t.get("dis").toString(), t.get("id").toString());
                }
            }
            if (newTunerList.isEmpty() == false) {
                expandableListDetail.put(m.get("dis").toString(), newTunerList);
            }
            equipMap.put(m.get("dis").toString(), m.get("id").toString());
            //Log.e("InsideTempOverrideFrag", "equipMap- " + equipMap);
            expandableListTitle = new ArrayList<String>(expandableListDetail.keySet());
            ArrayList<ZoneSorter> zoneNodesList = new ArrayList<>();
            for (int i = 0; i < expandableListTitle.size(); i++) {
                if (!expandableListTitle.get(i).equals("CM-device")) {
                    int nodeAddress = Integer.parseInt(expandableListTitle.get(i).substring(3));
                    ZoneProfile profile = L.getProfile(Short.parseShort(String.valueOf(nodeAddress)));
                    if (profile!=null) {
                        if (!profile.getProfileType().toString().equals("EMR") && !profile.getProfileType().toString().contains("MODBUS")) {
                            ZoneSorter zoneSorter = new ZoneSorter(expandableListTitle.get(i), nodeAddress);
                            zoneNodesList.add(zoneSorter);
                        }
                    }
                }
            }
            Collections.sort(zoneNodesList, new Comparator<ZoneSorter>() {
                @Override
                public int compare(ZoneSorter o1, ZoneSorter o2) {
                    return o1.getNodeAddress() - o2.getNodeAddress();
                }
            });
            List <String>sortedExpandableListTitle = new ArrayList<String>();
            for (ZoneSorter zoneName:zoneNodesList){
                sortedExpandableListTitle.add(zoneName.getZoneName());
            }
            sortedExpandableListTitle.add(0,"CM-device");
            expandableListAdapter = new TempOverrideExpandableListAdapter(TempOverrideFragment.this, sortedExpandableListTitle, expandableListDetail, pointMap, getActivity(), siteName, equipsRef);
            expandableListView.setAdapter(expandableListAdapter);
            expandableListAdapter.notifyDataSetChanged();

            expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
                @Override
                public void onGroupExpand(int groupPosition) {
                    for (int g = 0; g < expandableListAdapter.getGroupCount(); g++) {
                        if (g != groupPosition) {
                            expandableListView.collapseGroup(g);
                        }
                    }
                    expandableListView.invalidateViews();
                    if (lastExpandedPosition != -1
                            && groupPosition != lastExpandedPosition) {
                        expandableListView.collapseGroup(lastExpandedPosition);
                    }
                    lastExpandedPosition = groupPosition;

                }
            });
        }
    }

    public double getConfigEnabled(String config) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and output and enabled and "+config);
        if (configPoint.isEmpty()){
            return 0.0;
        }
        else{
            return hayStack.readPointPriorityVal(configPoint.get("id").toString());
        }

    }

    public static double getPointVal(String id) {
        return CCUHsApi.getInstance().readHisValById(id);
    }
}