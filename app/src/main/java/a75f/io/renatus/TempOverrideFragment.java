package a75f.io.renatus;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Zone;
import a75f.io.logic.Globals;
import butterknife.ButterKnife;

public class TempOverrideFragment extends Fragment {
    ArrayList<HashMap> openZoneMap;

    ExpandableListView            expandableListView;
    TempOverrideExpandableListAdapter         expandableListAdapter;
    List<String> expandableListTitle;
    TreeMap<String, List<String>> expandableListDetail;
    HashMap<String, List<String>> expandableListDetail_CMDevice;


    TreeMap<String, String> pointMap = new TreeMap();
    HashMap<String, String> equipMap = new HashMap();
    int lastExpandedPosition;

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
        loadExistingZones();
        return rootView;
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
    public void onPause() {
        super.onPause();
        Globals.getInstance().setTemproryOverrideMode(false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        expandableListView = view.findViewById(R.id.expandableListView);
        expandableListDetail = new TreeMap<>();
        expandableListDetail_CMDevice = new HashMap<>();
        String siteName = CCUHsApi.getInstance().read("site").get("dis").toString();

        //ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone and roomRef");
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and group");
        ArrayList<String> equipsRef = new ArrayList<String>();
        for(int i = 0; i<equips.size(); i++) {
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
        //Log.e("InsideTempOverrideFrag", "Zonedevices- " + Zonedevices);
        for (Map m : Zonedevices) {
            //Log.e("InsideTempOverrideFrag","value_m- "+m);
            ArrayList<HashMap> tuners = CCUHsApi.getInstance().readAll("point and his and deviceRef == \""+m.get("id")+"\"");
            ArrayList tunerList = new ArrayList();
            ArrayList newTunerList = new ArrayList();

            //Log.e("InsideTempOverrideFrag","tuners- "+tuners);
            for (Map t : tuners) {
                if (t.get("dis").toString().startsWith("Analog1In") || t.get("dis").toString().startsWith("Analog1Out") || t.get("dis").toString().startsWith("Analog2In") ||
                        t.get("dis").toString().startsWith("Analog2Out") || t.get("dis").toString().startsWith("relay") || t.get("dis").toString().startsWith("Th") ||
                        t.get("dis").toString().startsWith(siteName) && Objects.nonNull(t.get("dis").toString())) {
                    String NewexpandedListText = t.get("dis").toString();
                    if (NewexpandedListText.startsWith("Analog")) {
                        //Log.e("InsideTempOverrideFrag","NewexpandedListText- "+NewexpandedListText + ", IsportEnabled- "+t.get("portEnabled").toString());
                        if(t.get("portEnabled").toString().equals("true")) {
                            tunerList.add(t.get("dis").toString());
                        }
                    } else if (NewexpandedListText.startsWith("relay")) {
                        if(t.get("portEnabled").toString().equals("true")) {
                            tunerList.add(t.get("dis").toString());
                        }
                    }else if (NewexpandedListText.startsWith("Th")) {
                        if(t.get("portEnabled").toString().equals("true")) {
                            tunerList.add(t.get("dis").toString());
                        }
                    } else if (NewexpandedListText.startsWith(siteName)) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, t.get("dis").toString().substring(siteName.length()+1, t.get("dis").toString().length()));
                        if (NewexpandedListText.startsWith("CM-analog1Out")) {
                            //Log.e("InsideTempOverrideFrag","NewexpandedListText- "+NewexpandedListText + ", IsgetConfigEnabled- "+getConfigEnabled("analog1"));
                            if(getConfigEnabled("analog1") > 0) {
                                tunerList.add(t.get("dis").toString());
                            }
                        }
                        else if (NewexpandedListText.startsWith("CM-analog2Out")) {
                            //Log.e("InsideTempOverrideFrag","NewexpandedListText- "+NewexpandedListText + ", IsgetConfigEnabled- "+getConfigEnabled("analog2"));
                            if(getConfigEnabled("analog2") > 0) {
                                tunerList.add(t.get("dis").toString());
                            }
                        } else if (NewexpandedListText.startsWith("CM-analog3Out")) {
                            if(getConfigEnabled("analog3") > 0) {
                                tunerList.add(t.get("dis").toString());
                            }
                        }else if (NewexpandedListText.startsWith("CM-analog4Out")) {
                            if(getConfigEnabled("analog4") > 0) {
                                tunerList.add(t.get("dis").toString());
                            }
                        }
                        else if (NewexpandedListText.startsWith("relay")) {
                            String relayPos = (t.get("dis").toString().substring(siteName.length()+6, siteName.length()+7));
                            if(getConfigEnabled("relay"+relayPos) > 0) {
                                tunerList.add(t.get("dis").toString());
                            }
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
                    for (int i=0; i<tunerList.size(); i++){
                        if (tunerList.get(i).toString().contains("Analog1In")){
                            if (!newTunerList.contains(tunerList.get(i))) {

                                newTunerList.add(tunerList.get(i));
                            }
                        }
                        else
                            continue;
                    }
                    for (int i=0; i<tunerList.size(); i++){
                        if (tunerList.get(i).toString().contains("Analog2In")){
                            if (!newTunerList.contains(tunerList.get(i))) {

                                newTunerList.add(tunerList.get(i));
                            }
                        }
                        else
                            continue;
                    }
                    for (int i=0; i<tunerList.size(); i++){
                        if (tunerList.get(i).toString().contains("Analog3In")){
                            if (!newTunerList.contains(tunerList.get(i))) {

                                newTunerList.add(tunerList.get(i));
                            }
                        }
                        else
                            continue;
                    }
                    for (int i=0; i<tunerList.size(); i++){
                        if (tunerList.get(i).toString().contains("Analog1Out") || tunerList.get(i).toString().contains("analog1Out")){
                            if (!newTunerList.contains(tunerList.get(i))) {

                                newTunerList.add(tunerList.get(i));
                            }
                        }
                        else
                            continue;
                    }
                    for (int i=0; i<tunerList.size(); i++){
                        if (tunerList.get(i).toString().contains("Analog2Out") || tunerList.get(i).toString().contains("analog2Out")){
                            if (!newTunerList.contains(tunerList.get(i))) {

                                newTunerList.add(tunerList.get(i));
                            }
                        }
                        else
                            continue;
                    }
                    for (int i=0; i<tunerList.size(); i++){
                        if (tunerList.get(i).toString().contains("Analog3Out") || tunerList.get(i).toString().contains("analog3Out")){
                            if (!newTunerList.contains(tunerList.get(i))) {

                                newTunerList.add(tunerList.get(i));
                            }
                        }
                        else
                            continue;
                    }
                    for (int i=0; i<tunerList.size(); i++){
                        if (tunerList.get(i).toString().contains("Th1In")){
                            if (!newTunerList.contains(tunerList.get(i))) {

                                newTunerList.add(tunerList.get(i));
                            }
                        }
                        else
                            continue;
                    }
                    for (int i=0; i<tunerList.size(); i++){
                        if (tunerList.get(i).toString().contains("Th2In")){
                            if (!newTunerList.contains(tunerList.get(i))) {

                                newTunerList.add(tunerList.get(i));
                            }
                        }
                        else
                            continue;
                    }
                    for (int i=0; i<tunerList.size(); i++){
                        if (tunerList.get(i).toString().contains("relay")){
                            if (!newTunerList.contains(tunerList.get(i))) {

                                newTunerList.add(tunerList.get(i));
                            }
                        }
                        else
                            continue;
                    }
                    pointMap.put(t.get("dis").toString(), t.get("id").toString());
                }
            }
            /*if (tunerList.isEmpty() == false) {
                expandableListDetail.put(m.get("dis").toString(), tunerList);
                Log.e("InsideTempOverrideFrag", "tunerList- " + tunerList);
            }*/
            if (newTunerList.isEmpty() == false) {
                expandableListDetail.put(m.get("dis").toString(), newTunerList);
                //Log.e("InsideTempOverrideFrag", "tunerList- " + newTunerList);
            }
            equipMap.put(m.get("dis").toString(), m.get("id").toString());
            //Log.e("InsideTempOverrideFrag", "equipMap- " + equipMap);
            expandableListTitle = new ArrayList<String>(expandableListDetail.keySet());

            expandableListAdapter = new TempOverrideExpandableListAdapter(TempOverrideFragment.this, expandableListTitle, expandableListDetail, pointMap, getActivity(), siteName, equipsRef);
            expandableListView.setAdapter(expandableListAdapter);
        }

        expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                // This is breaking HaystackExplorer for me since the second time we grab data here, the order
                // of the groups changes in the backing data, but not in the UI.  I'm unable to programmatically force the UI to update.
                // Recommend not updating data after UI drawn, unless we can get the expandable list to redraw.
                // updateAllData();
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

    public double getConfigEnabled(String config) {
        //Log.e("InsideTempOverrideExpandableListAdapter","config- "+config);
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and output and enabled and "+config);
        if (configPoint.isEmpty()){
            return 0.0;
        }
        else{
            //Log.e("InsideTempOverrideExpandableListAdapter","configPoint- "+configPoint);
            return hayStack.readPointPriorityVal(configPoint.get("id").toString());
        }

    }

    public static double getPointVal(String id) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Point p = new Point.Builder().setHashMap(hayStack.readMapById(id)).build();
        for (String marker : p.getMarkers())
        {
            if (marker.equals("his"))
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
                            try
                            {
                                return Double.parseDouble(valMap.get("val").toString());
                            }catch (Exception e) {
                                return 0;
                            }
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
}