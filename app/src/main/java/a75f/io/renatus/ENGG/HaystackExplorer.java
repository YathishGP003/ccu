package a75f.io.renatus.ENGG;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import a75f.io.logic.jobs.SystemScheduleUtil;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.EquipTempExpandableListAdapter;
import a75f.io.renatus.FloorPlanFragment;
import a75f.io.renatus.FragmentDABDualDuctConfiguration;
import a75f.io.renatus.R;
import a75f.io.renatus.util.ProgressDialogUtils;

public class HaystackExplorer extends Fragment
{
    ExpandableListView            expandableListView;
    EquipTempExpandableListAdapter         expandableListAdapter;
    List<String>                  expandableListTitle;
    HashMap<String, List<String>> expandableListDetail;

    
    HashMap<String, String> tunerMap = new HashMap();
    HashMap<String, String> equipMap = new HashMap();
    int lastExpandedPosition;

    // require pass code for environments QA and up.
    private boolean passCodeValidationRequired = !(BuildConfig.BUILD_TYPE.equals("local") || BuildConfig.BUILD_TYPE.equals("dev"));
    
    public HaystackExplorer()
    {
    }
    
    public static HaystackExplorer newInstance()
    {
        return new HaystackExplorer();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_tuner_explore, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        expandableListView = view.findViewById(R.id.expandableListView);
        
        expandableListDetail = new HashMap<>();
        updateAllData();
        expandableListAdapter = new EquipTempExpandableListAdapter(HaystackExplorer.this, expandableListTitle, expandableListDetail, tunerMap, getActivity());
        expandableListView.setAdapter(expandableListAdapter);
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
                /*if (lastExpandedPosition != -1
                    && groupPosition != lastExpandedPosition) {
                    expandableListView.collapseGroup(lastExpandedPosition);
                }*/
                lastExpandedPosition = groupPosition;


            }
        });
    
        setupLongClick();
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
    private void setupLongClick() {
        expandableListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
        
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
    
                if (passCodeValidationRequired) {
                    showPassCodeScren();
                    return true;
                }
                long packedPosition = expandableListView.getExpandableListPosition(position);
                if (ExpandableListView.getPackedPositionType(packedPosition) ==
                    ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
                    
                    String equip = expandableListTitle.get(groupPosition);
                    new AlertDialog.Builder(getContext())
                        .setTitle("Delete ?")
                        .setMessage("Do you want to delete "+equip+" and all its points?")
                           .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int which) {
                                   deleteEntity(equipMap.get(equip));
                               }
                           })
                       .setNegativeButton(android.R.string.no, null)
                       .setIcon(android.R.drawable.ic_dialog_alert)
                       .show();
                    return true;
                } else if (ExpandableListView.getPackedPositionType(packedPosition) ==
                           ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                    
                    int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
                    int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
                
                    String point = expandableListDetail.get(expandableListTitle.get(groupPosition)).get(
                        childPosition);
                
                    new AlertDialog.Builder(getContext())
                        .setTitle("Delete ?")
                        .setMessage("Points should be deleted only when there are duplicates, otherwise this may " +
                                    "result in app crashes.\n\n" +
                                    "Do you want to delete the point "+point+"?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                deleteEntity(tunerMap.get(point));
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                }
                return true;
            }
        });
    }
    
    private void deleteEntity(String entityId) {
    
        new AsyncTask<Void, Void, Void>() {
        
            @Override
            protected void onPreExecute() {
                ProgressDialogUtils.showProgressDialog(getActivity(), "Deleting ...");
                super.onPreExecute();
            }
        
            @Override
            protected Void doInBackground( final Void ... params ) {
                CCUHsApi.getInstance().deleteEntityTree(entityId);
                updateAllData();
                CCUHsApi.getInstance().syncEntityTree();
                return null;
            }
        
            @Override
            protected void onPostExecute( final Void result ) {
                ProgressDialogUtils.hideProgressDialog();
                expandableListAdapter.notifyDataSetChanged();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    private void updateAllData() {
        tunerMap.clear();
        expandableListDetail.clear();
        equipMap.clear();
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
            expandableListDetail.put(m.get("dis").toString(), tunerList);
            equipMap.put(m.get("dis").toString(), m.get("id").toString());
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
            equipMap.put(m.get("dis").toString(), m.get("id").toString());
        }
        expandableListTitle = new ArrayList<String>(expandableListDetail.keySet());
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
                    SystemScheduleUtil.handleDesiredTempUpdate(p, true, val);
    
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