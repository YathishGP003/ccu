package a75f.io.renatus;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.dualduct.DualDuctUtil;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.renatus.ENGG.HaystackExplorer;
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


    TreeMap<String, String> tunerMap = new TreeMap();
    HashMap<String, String> equipMap = new HashMap();
    int lastExpandedPosition;

    private boolean passCodeValidationRequired = !(BuildConfig.BUILD_TYPE.equals("local") || BuildConfig.BUILD_TYPE.equals("dev"));

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
        /*for (Map m:devices){
            ArrayList<HashMap> points = CCUHsApi.getInstance().readAll("point and physical and deviceRef == \""+m.get("id")+"\"");
            Log.e("InsideTempOverrideFrag","CM_points- "+points);
            ArrayList tunerList = new ArrayList();

            for (Map t : points) {
                Log.e("InsideTempOverrideFrag","t_check- "+t);
                tunerList.add(t.get("dis").toString());
                Log.e("InsideTempOverrideFrag","tunerList- "+tunerList);
                tunerMap.put(t.get("dis").toString(), t.get("id").toString());
                Log.e("InsideTempOverrideFrag","tunerMap- "+tunerMap);
            }
            expandableListDetail.put(m.get("dis").toString(), tunerList);
            equipMap.put(m.get("dis").toString(), m.get("id").toString());
        }*/

        ArrayList<HashMap> Zonedevices = CCUHsApi.getInstance().readAll("device");
        for (Map m : Zonedevices) {
            if (Objects.nonNull(m.get("group"))) {
                short group = Short.parseShort(m.get("group").toString());
                Log.e("InsideTempOverrideFrag", "device1- " + HSUtil.getDevice(group));
            }
            ArrayList<HashMap> tuners = CCUHsApi.getInstance().readAll("point and his and deviceRef == \""+m.get("id")+"\"");
            //Log.e("InsideTempOverrideFrag","tuners2- "+tuners);
            //Log.e("InsideTempOverrideFrag","disCheck- "+CCUHsApi.getInstance().read("occupied sp == \""+m.get("id")+"\""));
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
                    tunerMap.put(t.get("dis").toString(), t.get("id").toString());
                }
            }
            /*Log.e("InsideTempOverrideFrag", "expandableListDetail1- " + m.get("dis").toString());
            Log.e("InsideTempOverrideFrag", "tunerList1- " + tunerList);*/
            /*if (m.get("dis").toString().equals("CM-device")) {
                setSystemProfile(m, tunerList);
            }else {
                expandableListDetail.put(m.get("dis").toString(), tunerList);
                //Log.e("InsideTempOverrideFrag","expandableListDetail- "+expandableListDetail);
                equipMap.put(m.get("dis").toString(), m.get("id").toString());
                expandableListTitle = new ArrayList<String>(expandableListDetail.keySet());
                //updateAllData();
                expandableListAdapter = new TempOverrideExpandableListAdapter(TempOverrideFragment.this, expandableListTitle, expandableListDetail, tunerMap, getActivity());
                expandableListView.setAdapter(expandableListAdapter);
            }*/
            if (tunerList.isEmpty() == false) {
                Log.e("InsideTempOverrideFrag", "expandableListDetail1- " + m.get("dis").toString());
                Log.e("InsideTempOverrideFrag", "tunerList1- " + tunerList);
                expandableListDetail.put(m.get("dis").toString(), tunerList);
            }

            //Log.e("InsideTempOverrideFrag","expandableListDetail- "+expandableListDetail);
            equipMap.put(m.get("dis").toString(), m.get("id").toString());
            expandableListTitle = new ArrayList<String>(expandableListDetail.keySet());
            //updateAllData();
            expandableListAdapter = new TempOverrideExpandableListAdapter(TempOverrideFragment.this, expandableListTitle, expandableListDetail, tunerMap, getActivity(), siteName);
            expandableListView.setAdapter(expandableListAdapter);
        }
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {

                if (passCodeValidationRequired) {
                    showPassCodeScren();
                    return true;
                }
                Log.d("CCU_HE", "onChildClick "+groupPosition+" "+childPosition);
                String tunerName = expandableListDetail.get(expandableListTitle.get(groupPosition)).get(
                        childPosition);

                if (tunerName.contains("currentTemp") || tunerName.contains("Variable")) {
                    return true ;
                }
                Toast.makeText(getActivity(), expandableListTitle.get(groupPosition) + " -> " + tunerName, Toast.LENGTH_SHORT).show();

                final EditText taskEditText = new EditText(getActivity());
                String tunerVal = String.valueOf(getPointVal(tunerMap.get(tunerName)));
                KeyListener keyListener = DigitsKeyListener.getInstance("0123456789.");
                taskEditText.setKeyListener(keyListener);

                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setTitle(tunerName)
                        .setMessage(tunerVal)
                        .setView(taskEditText)
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (taskEditText.getText().toString().trim().length() > 0) {
                                    setPointVal(tunerMap.get(tunerName), Double.parseDouble(taskEditText.getText().toString()) );
                                    tunerMap.put(tunerMap.get(tunerName), taskEditText.getText().toString());
                                    expandableListView.invalidateViews();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                dialog.show();
                return false;
            }
        });

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

    /*private void setSystemProfile(Map m, ArrayList tunerList) {
        expandableListDetail_CMDevice.put(m.get("dis").toString(), tunerList);
        //Log.e("InsideTempOverrideFrag","expandableListDetail- "+expandableListDetail);
        equipMap.put(m.get("dis").toString(), m.get("id").toString());
        expandableListTitle = new ArrayList<String>(expandableListDetail_CMDevice.keySet());
        //updateAllData();
        expandableListAdapter = new TempOverrideExpandableListAdapter(TempOverrideFragment.this, expandableListTitle, expandableListDetail_CMDevice, tunerMap, getActivity());
    }*/

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

    private void showPassCodeScren() {
        final EditText taskEditText = new EditText(getActivity());
        KeyListener keyListener = DigitsKeyListener.getInstance("0123456789");
        taskEditText.setKeyListener(keyListener);

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle("Enter passcode")
                .setMessage("Changing haystack data may corrupt the device.\n" +
                        "We are making sure you know what you are doing.")
                .setView(taskEditText)
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (taskEditText.getText().toString().trim().equals("7575")) {
                            dialog.dismiss();
                            passCodeValidationRequired = false;
                        } else {
                            taskEditText.getText().clear();
                            Toast.makeText(getActivity(), "Incorrect passcode", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    public void setPointVal(String id, double val) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground( final String ... params ) {

                CCUHsApi hayStack = CCUHsApi.getInstance();
                Point p = new Point.Builder().setHashMap(hayStack.readMapById(id)).build();
                if (p.getMarkers().contains("writable"))
                {
                    CcuLog.d(L.TAG_CCU_UI, "Set Writbale Val "+p.getDisplayName()+": " +val);
                    //CCUHsApi.getInstance().pointWrite(HRef.copy(id), TunerConstants.MANUAL_OVERRIDE_VAL_LEVEL, "manual", HNum.make(val) , HNum.make(2 * 60 * 60 * 1000, "ms"));
                    ScheduleProcessJob.handleDesiredTempUpdate(p, true, val);

                }

                if (p.getMarkers().contains("his"))
                {
                    CcuLog.d(L.TAG_CCU_UI, "Set His Val "+id+": " +val);
                    hayStack.writeHisValById(id, val);
                }
                return null;
            }

            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }
}