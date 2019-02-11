package a75f.io.renatus;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.util.Log;
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
import a75f.io.api.haystack.Point;
import a75f.io.logic.tuners.TunerConstants;

/**
 * Created by samjithsadasivan on 1/31/19.
 */

public class ZoneFragmentTemp extends Fragment
{
    ExpandableListView            expandableListView;
    ExpandableListAdapter         expandableListAdapter;
    List<String>                  expandableListTitle;
    HashMap<String, List<String>> expandableListDetail;
    
    HashMap<String, String> tunerMap = new HashMap();
    int lastExpandedPosition;
    
    public ZoneFragmentTemp()
    {
    }
    
    public static ZoneFragmentTemp newInstance()
    {
        return new ZoneFragmentTemp();
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
        expandableListAdapter = new EquipTempExpandableListAdapter(getActivity(), expandableListTitle, expandableListDetail, tunerMap);
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
                
                if (tunerName.contains("currentTemp")) {
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
        for (Map m : equips)
        {
            if (m.get("profile") != null && !m.get("profile").toString().contains("SYSTEM"))
            {
                Log.d("CCU","Equip: "+m);
                HashMap currTmep = CCUHsApi.getInstance().read("point and air and temp and sensor and current and equipRef == \""+m.get("id")+"\"");
                HashMap coolDT = CCUHsApi.getInstance().read("point and air and temp and desired and cooling and sp and equipRef == \""+m.get("id")+"\"");
                HashMap heatDT = CCUHsApi.getInstance().read("point and air and temp and desired and heating and sp and equipRef == \""+m.get("id")+"\"");
                
                ArrayList tunerList = new ArrayList();
                tunerList.add(currTmep.get("dis").toString());
                tunerList.add(coolDT.get("dis").toString());
                tunerList.add(heatDT.get("dis").toString());
    
                tunerMap.put(currTmep.get("dis").toString(), currTmep.get("id").toString());
                tunerMap.put(coolDT.get("dis").toString(), coolDT.get("id").toString());
                tunerMap.put(heatDT.get("dis").toString(), heatDT.get("id").toString());
                
                expandableListDetail.put(m.get("dis").toString(), tunerList);
            }
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
    
    public void setPointVal(String id, double val) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground( final Void ... params ) {
    
                CCUHsApi hayStack = CCUHsApi.getInstance();
                Point p = new Point.Builder().setHashMap(hayStack.readMapById(id)).build();
                for (String marker : p.getMarkers())
                {
                    if (marker.equals("writable"))
                    {
                        CCUHsApi.getInstance().writePoint(id, TunerConstants.VAV_BUILDING_VAL_LEVEL, "ccu", val, 0);
                    }
                }
    
                for (String marker : p.getMarkers())
                {
                    if (marker.equals("his"))
                    {
                        hayStack.writeHisValById(id, val);
                    }
                }
                
                
                return null;
            }
            
            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }
    
}
