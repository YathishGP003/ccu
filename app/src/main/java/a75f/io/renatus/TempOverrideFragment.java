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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.logic.Globals;
import butterknife.ButterKnife;

/**
 * Created by Mahesh on 18-07-2019.
 */
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
        return rootView;
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

        ArrayList<HashMap> Zonedevices = CCUHsApi.getInstance().readAll("device");
        Log.e("InsideTempOverrideFrag", "Zonedevices- " + Zonedevices);
        for (Map m : Zonedevices) {
            ArrayList<HashMap> tuners = CCUHsApi.getInstance().readAll("point and his and deviceRef == \""+m.get("id")+"\"");
            ArrayList tunerList = new ArrayList();

            for (Map t : tuners) {
                if (t.get("dis").toString().startsWith("Analog1In") || t.get("dis").toString().startsWith("Analog1Out") || t.get("dis").toString().startsWith("Analog2In") ||
                        t.get("dis").toString().startsWith("Analog2Out") || t.get("dis").toString().startsWith("relay") || t.get("dis").toString().startsWith("Th") ||
                        t.get("dis").toString().startsWith(siteName) && Objects.nonNull(t.get("dis").toString())) {
                    tunerList.add(t.get("dis").toString());
                    Collections.sort(tunerList, new Comparator<String>() {
                        @Override
                        public int compare(String s1, String s2) {
                            return s1.compareToIgnoreCase(s2);
                        }
                    });
                    pointMap.put(t.get("dis").toString(), t.get("id").toString());
                }
            }
            if (tunerList.isEmpty() == false) {
                expandableListDetail.put(m.get("dis").toString(), tunerList);
            }
            equipMap.put(m.get("dis").toString(), m.get("id").toString());
            expandableListTitle = new ArrayList<String>(expandableListDetail.keySet());
            expandableListAdapter = new TempOverrideExpandableListAdapter(TempOverrideFragment.this, expandableListTitle, expandableListDetail, pointMap, getActivity(), siteName);
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