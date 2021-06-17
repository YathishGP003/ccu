package a75f.io.renatus;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.Equip;
import a75f.io.logic.bo.building.dualduct.DualDuctUtil;
import a75f.io.logic.jobs.ScheduleProcessJob;
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
    }
}