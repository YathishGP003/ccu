package a75f.io.renatus.ENGG;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
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
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.renatus.EquipTempExpandableListAdapter;
import a75f.io.renatus.R;

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
        updateAllData();
        expandableListTitle = new ArrayList<String>(expandableListDetail.keySet());
        expandableListAdapter = new EquipTempExpandableListAdapter(ZoneFragmentTemp.this, expandableListTitle, expandableListDetail, tunerMap, getActivity());
        expandableListView.setAdapter(expandableListAdapter);
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                
                String tunerName = expandableListDetail.get(expandableListTitle.get(groupPosition)).get(
                        childPosition);
                
                /*if (tunerName.contains("currentTemp") || tunerName.contains("Variable")) {
                    return true ;
                }*/
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
                updateAllData();
                for (int g = 0; g < expandableListAdapter.getGroupCount(); g++) {
                    if (g != groupPosition) {
                        expandableListView.collapseGroup(g);
                    }
                }
                expandableListView.invalidateViews();
                /*if (lastExpandedPosition != -1
                    && groupPosition != lastExpandedPosition) {
                    expandableListView.collapseGroup(lastExpandedPosition);
                }*/
                lastExpandedPosition = groupPosition;


            }
        });
    }
    
    private void updateAllData() {
        tunerMap.clear();
        expandableListDetail.clear();
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");
        for (Map m : equips) {
            ArrayList<HashMap> tuners = CCUHsApi.getInstance().readAll("point and his and equipRef == \""+m.get("id")+"\"");
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
    
            ArrayList<HashMap> configs = CCUHsApi.getInstance().readAll("config and equipRef == \""+m.get("id")+"\"");
    
            for (Map t : configs) {
                tunerList.add(t.get("dis").toString());
                tunerMap.put(t.get("dis").toString(), t.get("id").toString());
            }
            expandableListDetail.put(m.get("dis").toString()+" "+ m.get("id"), tunerList);
        }
    
        ArrayList<HashMap> devices = CCUHsApi.getInstance().readAll("device");
        for (Map m : devices) {
            ArrayList<HashMap> tuners = CCUHsApi.getInstance().readAll("point and his and deviceRef == \""+m.get("id")+"\"");
            ArrayList tunerList = new ArrayList();
        
            for (Map t : tuners) {
                tunerList.add(t.get("dis").toString());
                tunerMap.put(t.get("dis").toString(), t.get("id").toString());
            }
            expandableListDetail.put(m.get("dis").toString(), tunerList);
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
    
    public static boolean isNumeric(String strNum) {
        return strNum.matches("-?\\d+(\\.\\d+)?");
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