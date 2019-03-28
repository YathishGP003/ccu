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

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.tuners.TunerConstants;

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
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        expandableListView = view.findViewById(R.id.expandableListView);
        
        expandableListDetail = new HashMap<>();
        updateData();
        expandableListTitle = new ArrayList<String>(expandableListDetail.keySet());
        expandableListAdapter = new EquipTempExpandableListAdapter(ZoneFragmentTemp.this, expandableListTitle, expandableListDetail, tunerMap);
        expandableListView.setAdapter(expandableListAdapter);
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                
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
        for (HashMap m : equips)
        {
            Log.d("CCU_UI","Equip: "+m);
            Equip p = new Equip.Builder().setHashMap(m).build();
            if (p.getProfile() != null && !p.getProfile().contains("SYSTEM") && p.getProfile().contains("VAV"))
            {
                HashMap currTmep = CCUHsApi.getInstance().read("point and air and temp and sensor and current and equipRef == \""+p.getId()+"\"");
                HashMap coolDT = CCUHsApi.getInstance().read("point and air and temp and desired and cooling and sp and equipRef == \""+p.getId()+"\"");
                HashMap heatDT = CCUHsApi.getInstance().read("point and air and temp and desired and heating and sp and equipRef == \""+p.getId()+"\"");
                
                if (currTmep.size() != 0 && coolDT.size() != 0 && heatDT.size() != 0) {
                    ArrayList tunerList = new ArrayList();
                    tunerList.add(currTmep.get("dis").toString());
                    tunerList.add(coolDT.get("dis").toString());
                    tunerList.add(heatDT.get("dis").toString());
                    tunerList.add("schedule");
    
                    tunerMap.put(currTmep.get("dis").toString(), currTmep.get("id").toString());
                    tunerMap.put(coolDT.get("dis").toString(), coolDT.get("id").toString());
                    tunerMap.put(heatDT.get("dis").toString(), heatDT.get("id").toString());
                    tunerMap.put("schedule", p.getId());
    
                    expandableListDetail.put(p.getDisplayName(), tunerList);
                }
            }
            
            if (p.getProfile() != null && p.getProfile().equals(ProfileType.PLC.name())) {
    
                HashMap pv = CCUHsApi.getInstance().read("point and process and variable and equipRef == \""+p.getId()+"\"");
                HashMap cv = CCUHsApi.getInstance().read("point and control and variable and equipRef == \""+p.getId()+"\"");
                
                ArrayList tunerList = new ArrayList();
                tunerList.add(pv.get("dis").toString());
                tunerList.add(cv.get("dis").toString());
                tunerList.add("schedule");
    
                tunerMap.put(pv.get("dis").toString(), pv.get("id").toString());
                tunerMap.put(cv.get("dis").toString(), cv.get("id").toString());
                
                tunerMap.put("schedule", p.getId());
                expandableListDetail.put(p.getDisplayName(), tunerList);
                
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
    
    public void setPointVal(String id, double val) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground( final String ... params ) {
    
                CCUHsApi hayStack = CCUHsApi.getInstance();
                Point p = new Point.Builder().setHashMap(hayStack.readMapById(id)).build();
                for (String marker : p.getMarkers())
                {
                    if (marker.equals("writable"))
                    {
                        CcuLog.d(L.TAG_CCU_UI, "Set Writbale Val "+p.getDisplayName()+": " +val);
                        CCUHsApi.getInstance().pointWrite(HRef.copy(id), TunerConstants.MANUAL_OVERRIDE_VAL_LEVEL, "manual", HNum.make(val) , HNum.make(2 * 60 * 60 * 1000, "ms"));
                    }
                }
    
                for (String marker : p.getMarkers())
                {
                    if (marker.equals("his"))
                    {
                        CcuLog.d(L.TAG_CCU_UI, "Set His Val "+id+": " +val);
                        hayStack.writeHisValById(id, val);
                    }
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