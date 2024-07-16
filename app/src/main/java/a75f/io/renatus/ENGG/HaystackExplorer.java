package a75f.io.renatus.ENGG;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import org.projecthaystack.HGrid;
import org.projecthaystack.HRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
import a75f.io.device.mesh.Pulse;
import a75f.io.device.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.jobs.SystemScheduleUtil;
import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.EquipTempExpandableListAdapter;
import a75f.io.renatus.R;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.RxjavaUtil;

public class HaystackExplorer extends Fragment
{
    ExpandableListView            expandableListView;
    EquipTempExpandableListAdapter         expandableListAdapter;
    List<String>                  expandableListTitle;
    HashMap<String, List<String>> expandableListDetail;

    
    HashMap<String, String> tunerMap = new HashMap();
    HashMap<String, String> equipMap = new HashMap();
    HashMap<String, String> scheduleMap = new HashMap();
    int lastExpandedPosition;

    // require pass code for environments QA and up.
    private boolean passCodeValidationRequired =
        !(BuildConfig.BUILD_TYPE.equals("local") || BuildConfig.BUILD_TYPE.equals("dev") ||
                BuildConfig.BUILD_TYPE.equals("qa") ||BuildConfig.BUILD_TYPE.equals("dev_qa"));
    
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
        return inflater.inflate(R.layout.fragment_tuner_explore, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        expandableListView = view.findViewById(R.id.expandableListView);
        
        expandableListDetail = new HashMap<>();
        updateAllData();
        expandableListAdapter = new EquipTempExpandableListAdapter(HaystackExplorer.this, expandableListTitle, expandableListDetail, tunerMap, getActivity());
        expandableListView.setAdapter(expandableListAdapter);
        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {

            if (passCodeValidationRequired) {
                showPassCodeScren();
                return true;
            }
            CcuLog.d(L.TAG_CCU_UI, "onChildClick "+groupPosition+" "+childPosition);
            String tunerName = expandableListDetail.get(expandableListTitle.get(groupPosition)).get(
                    childPosition);

            final EditText taskEditText = new EditText(getActivity());
            String tunerVal = getPointVal(tunerMap.get(tunerName));
            KeyListener keyListener = DigitsKeyListener.getInstance("0123456789.");
            taskEditText.setKeyListener(keyListener);

            HashMap<Object, Object> pointTags = CCUHsApi.getInstance().readMapById(tunerMap.get(tunerName));
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                                         .setTitle(tunerName)
                                         .setMessage(pointTags.toString()+"\n\ncurrentVal - "+tunerVal)
                                         .setView(taskEditText)
                                         .setPositiveButton("Save", (dialog1, which) -> {
                                             if (!taskEditText.getText().toString().trim().isEmpty()) {
                                                 setPointVal(tunerMap.get(tunerName), Double.parseDouble(taskEditText.getText().toString()) );
                                                 tunerMap.put(tunerMap.get(tunerName), taskEditText.getText().toString());
                                                 expandableListView.invalidateViews();
                                             }
                                         })
                                         .setNegativeButton("Cancel", null)
                                         .create();
            dialog.show();
            return false;
        });
        
        expandableListView.setOnGroupExpandListener(groupPosition -> {
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
            lastExpandedPosition = groupPosition;


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
                                 .setPositiveButton("Done", (dialog1, which) -> {
                                     if (taskEditText.getText().toString().trim().equals("7575")) {
                                        dialog1.dismiss();
                                        passCodeValidationRequired = false;
                                     } else {
                                         taskEditText.getText().clear();
                                         Toast.makeText(getActivity(), "Incorrect passcode", Toast.LENGTH_SHORT).show();
                                     }
                                 })
                                 .setCancelable(false)
                                 .create();
        dialog.show();
    }
    private void setupLongClick() {
        expandableListView.setOnItemLongClickListener((parent, view, position, id) -> {

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
                       .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteEntity(equipMap.get(equip), false))
                   .setNegativeButton(android.R.string.no, null)
                   .setIcon(R.drawable.ic_dialog_alert)
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
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {

                        if (point.contains("Building Schedule")) {
                            CcuLog.i(L.TAG_CCU_UI, " scheduleMap.size  "+scheduleMap.size());
                            if (scheduleMap.size() == 1) {
                                Toast.makeText(parent.getContext(),
                                        "Delete Failed ! Cant delete the only building schedule",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                            //Its a hack based on the current point length
                            int startIndex = point.indexOf("-");
                            String id1 = point.substring(startIndex+1, startIndex+37);
                            CcuLog.i(L.TAG_CCU_UI, " Delete Schedule : id "+ id1);
                            deleteEntity(id1, true);
                        } else {
                            deleteEntity(tunerMap.get(point), false);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(R.drawable.ic_dialog_alert)
                    .show();
            }
            return true;
        });
    }
    
    private void deleteEntity(String entityId, boolean schedule) {
    
        new AsyncTask<Void, Void, Void>() {
        
            @Override
            protected void onPreExecute() {
                ProgressDialogUtils.showProgressDialog(getActivity(), "Deleting ...");
                super.onPreExecute();
            }
        
            @Override
            protected Void doInBackground( final Void ... params ) {
                if (schedule) {
                    CCUHsApi.getInstance().deleteEntityItem(entityId);
                } else {
                    CCUHsApi.getInstance().deleteEntityTree(entityId);
                }
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
        scheduleMap.clear();

        HGrid buildingSchedulesGrid = CCUHsApi.getInstance().getHSClient().readAll("schedule and building and not vacation and not special and not named");
        List<String> schedulesList = new ArrayList<>();
        Iterator it = buildingSchedulesGrid.iterator();
        int scheduleNameCounter = 0;
        while (it.hasNext()) {
            HRow r = (HRow) it.next();
            schedulesList.add(new Schedule.Builder().setHDict(r).build().toString());
            scheduleMap.put(++scheduleNameCounter+""+r.get("dis").toString(), r.get("id").toString());
        }

        expandableListDetail.put("Building Schedule", schedulesList);

        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");
        for (Map m : equips) {
            ArrayList<HashMap> points = CCUHsApi.getInstance().readAll("point and equipRef == \""+m.get("id")+"\"");
            Set tunerList = new HashSet();
        
            for (Map t : points) {
                String name = t.get("domainName") != null ? t.get("domainName").toString()+":"+t.get("id") : t.get("dis").toString()+":dis";
                tunerList.add(name);
                tunerMap.put(name, t.get("id").toString());
            }

            List tunersList= new ArrayList(tunerList);
            expandableListDetail.put(m.get("dis").toString()+" : "+m, tunersList);
            Collections.sort(tunersList);
            equipMap.put(m.get("dis").toString(), m.get("id").toString());
        }
    
        ArrayList<HashMap> devices = CCUHsApi.getInstance().readAll("device");
        for (Map m : devices) {
            ArrayList<HashMap> tuners = CCUHsApi.getInstance().readAll("point and his and deviceRef == \""+m.get("id")+"\"");
            ArrayList tunerList = new ArrayList();
        
            for (Map t : tuners) {
                String name = t.get("domainName") != null ? t.get("domainName").toString()+":"+t.get("id") : t.get("dis").toString()+":dis";
                tunerList.add(name);
                tunerMap.put(name, t.get("id").toString());
            }

            if(m.containsKey("sourceModelVersion")) {
                expandableListDetail.put(m.get("dis").toString()+" : "+m, tunerList);
                equipMap.put(m.get("dis").toString(), m.get("id").toString());
            }
            else {
                expandableListDetail.put(m.get("dis").toString(), tunerList);
                equipMap.put(m.get("dis").toString(), m.get("id").toString());
            }
        }
        expandableListTitle = new ArrayList<>(expandableListDetail.keySet());
    }
    
    public static String getPointVal(String id) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        StringBuilder val = new StringBuilder();
        try {
            Point p = new Point.Builder().setHashMap(hayStack.readMapById(id)).build();
            for (String marker : p.getMarkers()) {
                if (marker.equals("writable")) {
                    ArrayList values = hayStack.readPoint(id);
                    if (values != null && !values.isEmpty()) {
                        for (int l = 1; l <= values.size(); l++) {
                            HashMap valMap = ((HashMap) values.get(l - 1));
                            System.out.println(valMap);
                            if (valMap.get("val") != null) {
                                val.append("level : ").append(l).append(" val : ").append(valMap.get("val")).append("\n");
                            }
                        }
                    }
                }
            }
            for (String marker : p.getMarkers())
            {
                if (marker.equals("his"))
                {
                    val.append("hisVal : ").append(hayStack.readHisValById(p.getId()));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    

        return val.toString();
    }
    
    public static boolean isNumeric(String strNum) {
        return strNum.matches("-?\\d+(\\.\\d+)?");
    }
    
    public void setPointVal(String id, double val) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground( final String ... params ) {
    
                CCUHsApi hayStack = CCUHsApi.getInstance();
                Point p = new Point.Builder().setHDict(hayStack.readHDictById(id)).build();
                if (p.getMarkers().contains("writable"))
                {
                    CcuLog.d(L.TAG_CCU_UI, "Set Writbale Val "+p.getDisplayName()+": " +val);
                    //CCUHsApi.getInstance().pointWrite(HRef.copy(id), TunerConstants.MANUAL_OVERRIDE_VAL_LEVEL, "manual", HNum.make(val) , HNum.make(2 * 60 * 60 * 1000, "ms"));
                    SystemScheduleUtil.handleDesiredTempUpdate(p, true, val);
    
                }
    
                if (p.getMarkers().contains("his"))
                {
                    CcuLog.d(L.TAG_CCU_UI, "Set His Val "+id+": " +val);
                    CcuLog.d(L.TAG_CCU_UI, "domainName "+p.getDomainName());
                    if (isCurrentTempPoint(p)) {
                        CcuLog.d(L.TAG_CCU_UI, "Set "+p.getDomainName()+" Equip "+p.getEquipRef());
                        Equip q = HSUtil.getEquipInfo(p.getEquipRef());
                        RxjavaUtil.executeBackground( () -> {
                            CmToCcuOverUsbSnRegularUpdateMessage_t msg = new CmToCcuOverUsbSnRegularUpdateMessage_t();
                            msg.update.smartNodeAddress.set(Integer.parseInt(q.getGroup()));
                            msg.update.roomTemperature.set((int)val);
                            Pulse.regularSNUpdate(msg);
                        });
                    } else {
                        hayStack.writeHisValueByIdWithoutCOV(id, val);
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

    private boolean isCurrentTempPoint(Point point) {
        if (point.getDomainName() != null) {
            return point.getDomainName().equals("currentTemp");
        }
        return point.getMarkers().contains("current") && point.getMarkers().contains("temp");
    }
}