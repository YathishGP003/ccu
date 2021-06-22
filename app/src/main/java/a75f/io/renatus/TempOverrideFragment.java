package a75f.io.renatus;

import android.content.DialogInterface;
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
import java.util.HashMap;

import a75f.io.api.haystack.Equip;
import a75f.io.logic.bo.building.dualduct.DualDuctUtil;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.renatus.ENGG.HaystackExplorer;
import butterknife.ButterKnife;

/**
 * Created by Mahesh on 18-07-2019.
 */
public class TempOverrideFragment extends Fragment {
    ArrayList<HashMap> openZoneMap;

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /*for(int k=0;k<openZoneMap.size();k++) {
            Equip updatedEquip = new Equip.Builder().setHashMap(openZoneMap.get(k)).build();
            if (updatedEquip.getProfile().startsWith("DAB")) {
                HashMap dabPoints = ScheduleProcessJob.getDABEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "DAB Points:" + dabPoints.toString());
                loadDABPointsUI(dabPoints, inflater, linearLayoutZonePoints, updatedEquip.getGroup());
            }
            if (updatedEquip.getProfile().startsWith("VAV")) {
                HashMap vavPoints = ScheduleProcessJob.getVAVEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "VAV Points:" + vavPoints.toString());
                loadVAVPointsUI(vavPoints, inflater, linearLayoutZonePoints, updatedEquip.getGroup());
            }
            if (updatedEquip.getProfile().startsWith("SSE")) {
                HashMap ssePoints = ScheduleProcessJob.getSSEEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "SSE Points:" + ssePoints.toString());
                loadSSEPointsUI(ssePoints, inflater, linearLayoutZonePoints, updatedEquip.getGroup());
            }
            if (updatedEquip.getProfile().startsWith("TEMP_INFLUENCE")) {
                HashMap tiPoints = ScheduleProcessJob.getTIEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "TI Points:" + tiPoints.toString());
                loadTIPointsUI(tiPoints, inflater, linearLayoutZonePoints, updatedEquip.getGroup());
            }
            if (updatedEquip.getProfile().startsWith("SMARTSTAT_TWO_PIPE_FCU")) {
                HashMap p2FCUPoints = ScheduleProcessJob.get2PFCUEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "2PFCU Points:" + p2FCUPoints.toString());
                loadSS2PFCUPointsUI(p2FCUPoints, inflater, linearLayoutZonePoints, equipId, true, updatedEquip.getGroup());

            }
            if (updatedEquip.getProfile().startsWith("SMARTSTAT_FOUR_PIPE_FCU")) {
                HashMap p4FCUPoints = ScheduleProcessJob.get4PFCUEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "4PFCU Points:" + p4FCUPoints.toString());
                loadSS4PFCUPointsUI(p4FCUPoints, inflater, linearLayoutZonePoints, equipId, true, updatedEquip.getGroup());
            }
            if (updatedEquip.getProfile().startsWith("SMARTSTAT_CONVENTIONAL_PACK_UNIT")) {
                HashMap cpuEquipPoints = ScheduleProcessJob.getCPUEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "CPU Points:" + cpuEquipPoints.toString());
                loadSSCPUPointsUI(cpuEquipPoints, inflater, linearLayoutZonePoints, updatedEquip.getId(), true, updatedEquip.getGroup(),false);
                //isCPUloaded = true;
            }
            if (updatedEquip.getProfile().startsWith("SMARTSTAT_HEAT_PUMP_UNIT")) {
                HashMap hpuEquipPoints = ScheduleProcessJob.getHPUEquipPoints(updatedEquip.getId());
                Log.i("PointsValue", "HPU Points:" + hpuEquipPoints.toString());
                loadSSHPUPointsUI(hpuEquipPoints, inflater, linearLayoutZonePoints, updatedEquip.getId(), true, updatedEquip.getGroup(),false);
                //isHPUloaded = true;
            }
            if (updatedEquip.getProfile().startsWith("DUAL_DUCT")) {
                HashMap dualDuctPoints = DualDuctUtil.getEquipPointsForView(updatedEquip.getId());
                Log.i("PointsValue", "DUAL_DUCT Points:" + dualDuctPoints.toString());
                loadDualDuctPointsUI(dualDuctPoints, inflater, linearLayoutZonePoints, updatedEquip.getGroup());
            }
        }*/

        /*expandableListView = view.findViewById(R.id.expandableListView);

        expandableListDetail = new HashMap<>();
        updateAllData();
        expandableListAdapter = new EquipTempExpandableListAdapter(TempOverrideFragment.this, expandableListTitle, expandableListDetail, tunerMap, getActivity());
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

                *//*if (tunerName.contains("currentTemp") || tunerName.contains("Variable")) {
                    return true ;
                }*//*
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
                *//*if (lastExpandedPosition != -1
                    && groupPosition != lastExpandedPosition) {
                    expandableListView.collapseGroup(lastExpandedPosition);
                }*//*
                lastExpandedPosition = groupPosition;


            }
        });*/
    }
}