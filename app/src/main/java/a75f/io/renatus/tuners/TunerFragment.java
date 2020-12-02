package a75f.io.renatus.tuners;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.renatus.R;

/**
 * Created by samjithsadasivan on 1/17/19.
 */

public class TunerFragment extends Fragment implements TunerItemClickListener
{
    ExpandableListView            expandableListView;
    ExpandableListAdapter         expandableListAdapter;
    List<String>                  expandableListTitle;
    //HashMap<String, List<String>> expandableListDetail;
    HashMap<String, List<HashMap>> expandableListDetail;

    HashMap<String, String> tunerMap = new HashMap();
    int lastExpandedPosition;

    RadioGroup radioGroupTuners;
    RadioButton radioButtonSystem;
    RadioButton radioButtonZone;
    RadioButton radioButtonModule;
    TextView reasonLabel;
    RecyclerView recyclerViewTuner;
    public TunerFragment()
    {
    }
    
    public static TunerFragment newInstance()
    {
        return new TunerFragment();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_tuner_editor, container, false);
        //ButterKnife.bind(this, rootView);
        return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        recyclerViewTuner = view.findViewById(R.id.recyclerTuner);
        expandableListView = view.findViewById(R.id.expandableListView);

        expandableListDetail = new HashMap<>();
        //updateData();



        radioGroupTuners = view.findViewById(R.id.radioGrpTuner);
        radioButtonSystem = view.findViewById(R.id.radioBtnSystem);
        radioButtonZone = view.findViewById(R.id.radioBtnZone);
        radioButtonModule = view.findViewById(R.id.radioBtnModule);

        //Default Show System Tuners
        radioGroupTuners.check(R.id.radioBtnSystem);
        getSystemTuners();

        reasonLabel = view.findViewById(R.id.textReasonLabel);
        String text = "<font color=#E24301>*</font> <font color=#999999>Reason for Change</font>";
        reasonLabel.setText(Html.fromHtml(text));

        radioGroupTuners.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBtnSystem) {
                Log.i("TunersUI","Selected:radioBtnSystem");
                getSystemTuners();
            } else if (checkedId == R.id.radioBtnZone) {
                Log.i("TunersUI","Selected:radioBtnZone");
                updateData();
            } else if (checkedId == R.id.radioBtnModule) {
                Log.i("TunersUI","Selected:radioBtnModule");
                updateData();
            }
          /*  expandableListTitle = new ArrayList<String>(expandableListDetail.keySet());
            expandableListAdapter = new ExpandableTunerListAdapter(getActivity(), expandableListTitle, expandableListDetail, tunerMap);
            expandableListView.setAdapter(expandableListAdapter);*/
        });



        /*expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
        
            @Override
            public void onGroupExpand(int groupPosition) {
                Toast.makeText(getActivity(),
                        expandableListTitle.get(groupPosition) + " List Expanded.",
                        Toast.LENGTH_SHORT).show();
            }
        });*/
        
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                
               /* String tunerName = expandableListDetail.get(expandableListTitle.get(groupPosition)).get(childPosition);
               *//* if (!tunerName.contains("coolingUserLimitMax")&&!tunerName.contains("coolingUserLimitMin")&&!tunerName.contains("heatingUserLimitMin")
                    &&!tunerName.contains("heatingUserLimitMax")&&!tunerName.contains("buildingLimitMin")&&!tunerName.contains("buildingLimitMax"))
                {*//*
                    Toast.makeText(getActivity(), expandableListTitle.get(groupPosition) + " -> " + tunerName, Toast.LENGTH_SHORT).show();

                    final EditText taskEditText = new EditText(getActivity());
                    String tunerVal = String.valueOf(getTuner(tunerMap.get(tunerName)));
                    AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setTitle(tunerName)
                            .setMessage(tunerVal)
                            .setView(taskEditText)
                            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (taskEditText.getText().toString().trim().length() > 0) {
                                        setTuner(tunerMap.get(tunerName), Double.parseDouble(taskEditText.getText().toString()));
                                        tunerMap.put(tunerMap.get(tunerName), taskEditText.getText().toString());
                                        expandableListView.invalidateViews();
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .create();
                    dialog.show();
             //   }*/
                return false;
            }
        });
    
        expandableListView.setOnGroupExpandListener(groupPosition -> {
            getSystemTuners();
            expandableListView.invalidateViews();
            if (lastExpandedPosition != -1
                && groupPosition != lastExpandedPosition) {
                expandableListView.collapseGroup(lastExpandedPosition);
            }
            lastExpandedPosition = groupPosition;
        });
    }


    private void updateData() {
        tunerMap.clear();
        expandableListDetail.clear();
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");
        Log.i("TunersUI","Equips:"+equips);
        for (Map m : equips) {
            ArrayList<HashMap> tuners = CCUHsApi.getInstance().readAll("tuner and equipRef == \""+m.get("id")+"\"");
            ArrayList tunerList = new ArrayList();
        
            for (Map t : tuners) {
                tunerList.add(t.get("dis").toString());
                tunerMap.put(t.get("dis").toString(), t.get("id").toString());
            }
            
            ArrayList<HashMap> userIntents = CCUHsApi.getInstance().readAll("userIntent and equipRef == \""+m.get("id")+"\"");
            
            for (Map t : userIntents) {
                if(!t.get("dis").toString().contains("desired")) {
                    tunerList.add(t.get("dis").toString());
                    tunerMap.put(t.get("dis").toString(), t.get("id").toString());
                }
            }
            expandableListDetail.put(m.get("dis").toString(), tunerList);
        }
    }
    public double getTuner(String id) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList values = hayStack.readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
    
    public void setTuner(String id, double val) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground( final String ... params ) {
                CCUHsApi.getInstance().writePoint(id, TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", val, 0);
                CCUHsApi.getInstance().writeHisValById(id, val);
                return null;
            }
        
            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    private void getSystemTuners() {

        TunerExpandableLayoutHelper tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(),
                recyclerViewTuner, this, 2);

        //tunerMap.clear();
        //expandableListTitle.clear();
        //expandableListDetail.clear();
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("tunerGroup");
        //Log.i("TunersUI","tunerGroup:"+equips);
        ArrayList<HashMap> genericTuners = new ArrayList<>();
        ArrayList<HashMap> alertTuners = new ArrayList<>();

        HashMap<String, List> alertTunerMap = new HashMap();
        HashMap<String, List> genericTunerMap = new HashMap();
        ArrayList alertTunerList = new ArrayList();
        ArrayList genericTunerList = new ArrayList();

        Map<Integer, List<String>> valuesMap = new HashMap<>();

        // Group by countryName
        Map<String, List<HashMap>> groupByTuner = equips.stream().collect(Collectors.groupingBy(p -> p.get("tunerGroup").toString()));
        Map<String, List<HashMap>> sortedGroupTuner = new TreeMap<>(groupByTuner);
        System.out.println("After Sorting:");
        Set set2 = sortedGroupTuner.entrySet();
        Iterator iterator2 = set2.iterator();
        while(iterator2.hasNext()) {
            Map.Entry me2 = (Map.Entry)iterator2.next();
            Log.i("TunersUI","Sorting-"+me2.getKey());
        }
        for(String groupTitle: sortedGroupTuner.keySet()){
           /* ArrayList<String> tunerList = new ArrayList<>();
            for(HashMap tunerValue : groupByTuner.get(groupTitle)) {
                tunerList.add(tunerValue.get("dis").toString());
                tunerMap.put(tunerValue.get("dis").toString(), tunerValue.get("id").toString());
            }
            //Log.i("TunersUI","groupTitle:"+groupTitle);
            //Log.i("TunersUI","tunerGroupList:"+tunerList);
            //expandableListDetail.put(groupTitle, groupByTuner.get(groupTitle));
            //expandableListTitle.add(groupTitle);*/

            tunerExpandableLayoutHelper.addSection(groupTitle, sortedGroupTuner.get(groupTitle));
            tunerExpandableLayoutHelper.notifyDataSetChanged();
        }

        //Log.i("TunersUI","expandableListDetailSize-ALERT:"+expandableListDetail.get("ALERT"));
        //Log.i("TunersUI","expandableListDetailSize-GENERIC:"+expandableListDetail.get("GENERIC"));
        //Log.i("TunersUI","expandableListDetailSize-ALERTsize:"+expandableListDetail.get("ALERT").size());
        //Log.i("TunersUI","expandableListDetailSize-GENERICsize:"+expandableListDetail.get("GENERIC").size());
        /*for (HashMap m : equips) {
            Log.i("TunersUI","tunerGroup:"+m.get("tunerGroup"));
            HashMap<String,String> tunerItem = m;
            Log.i("TunersUI","tunerItem:"+tunerItem);
            if(m.get("tunerGroup").toString().equals("GENERIC"))
            {
                genericTuners.add(m);
                genericTunerList.add(m);
            }
            if(m.get("tunerGroup").toString().equals("ALERT"))
            {
                alertTuners.add(m);
                alertTunerList.add(m);
            }

            alertTunerMap.put(m.get("tunerGroup").toString(),alertTunerList);

            ArrayList<HashMap> tuners = CCUHsApi.getInstance().readAll("tuner and equipRef == \""+m.get("id")+"\"");
            ArrayList tunerList = new ArrayList();

            for (Map t : tuners) {
                tunerList.add(t.get("dis").toString());
                tunerMap.put(t.get("dis").toString(), t.get("id").toString());
            }

            ArrayList<HashMap> userIntents = CCUHsApi.getInstance().readAll("userIntent and equipRef == \""+m.get("id")+"\"");

            for (Map t : userIntents) {
                if(!t.get("dis").toString().contains("desired")) {
                    tunerList.add(t.get("dis").toString());
                    tunerMap.put(t.get("dis").toString(), t.get("id").toString());
                }
            }
            expandableListDetail.put(m.get("dis").toString(), tunerList);
        }
        alertTunerMap.put("GENERIC",alertTunerList);
        Log.i("TunersUI","genericTuners:"+genericTuners);
        Log.i("TunersUI","alertTuners:"+alertTuners);
        Log.i("TunersUI","ALERT-alertTunerList:"+alertTunerMap);*/
    }
    @Override
    public void itemClicked(HashMap item) {
        Toast.makeText(getActivity(), "TunerUI-HashMap: " + item.get("dis") + " clicked", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void itemClicked(TunerGroupItem section) {
        Toast.makeText(getActivity(), "TunerUI-Section: " + section.getName() + " clicked", Toast.LENGTH_SHORT).show();
    }
}
