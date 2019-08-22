package a75f.io.renatus.tuners;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.renatus.R;

/**
 * Created by samjithsadasivan on 1/17/19.
 */

public class TunerFragment extends Fragment
{
    ExpandableListView            expandableListView;
    ExpandableListAdapter         expandableListAdapter;
    List<String>                  expandableListTitle;
    HashMap<String, List<String>> expandableListDetail;
    
    HashMap<String, String> tunerMap = new HashMap();
    int lastExpandedPosition;
    
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
        expandableListView = view.findViewById(R.id.expandableListView);
        
        expandableListDetail = new HashMap<>();
        updateData();
        expandableListTitle = new ArrayList<String>(expandableListDetail.keySet());
        expandableListAdapter = new ExpandableTunerListAdapter(getActivity(), expandableListTitle, expandableListDetail, tunerMap);
        expandableListView.setAdapter(expandableListAdapter);
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
                
                String tunerName = expandableListDetail.get(expandableListTitle.get(groupPosition)).get(
                                                childPosition);
                if (!tunerName.contains("coolingUserLimitMax")&&!tunerName.contains("coolingUserLimitMin")&&!tunerName.contains("heatingUserLimitMin")
                    &&!tunerName.contains("heatingUserLimitMax")&&!tunerName.contains("buildingLimitMin")&&!tunerName.contains("buildingLimitMax"))
                {
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
                }
                return false;
            }
        });
    
        expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
        
            @Override
            public void onGroupExpand(int groupPosition) {
                updateData();
                expandableListView.invalidateViews();
                if (lastExpandedPosition != -1
                    && groupPosition != lastExpandedPosition) {
                    expandableListView.collapseGroup(lastExpandedPosition);
                }
                lastExpandedPosition = groupPosition;
            }
        });
    }
    
    private void updateData() {
        tunerMap.clear();
        expandableListDetail.clear();
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");
        for (Map m : equips) {
            ArrayList<HashMap> tuners = CCUHsApi.getInstance().readAll("tuner and equipRef == \""+m.get("id")+"\"");
            ArrayList tunerList = new ArrayList();
        
            for (Map t : tuners) {
                tunerList.add(t.get("dis").toString());
                tunerMap.put(t.get("dis").toString(), t.get("id").toString());
            }
            
            ArrayList<HashMap> userIntents = CCUHsApi.getInstance().readAll("userIntent and equipRef == \""+m.get("id")+"\"");
            
            for (Map t : userIntents) {
                tunerList.add(t.get("dis").toString());
                tunerMap.put(t.get("dis").toString(), t.get("id").toString());
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
}
