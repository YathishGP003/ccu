package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.modbus.ModbusProfile;
import a75f.io.modbusbox.EquipsManager;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.modbus.FragmentModbusConfiguration;
import a75f.io.renatus.modbus.RecyclerModbusParamAdapter;

public class FragmentBtuMeterConfiguration extends BaseDialogFragment {
    public static final String ID = FragmentBtuMeterConfiguration.class.getSimpleName();
    public ArrayList<Floor> floorList = new ArrayList();
    Comparator<Floor> floorComparator = new Comparator<Floor>() {
        @Override
        public int compare(Floor a, Floor b) {
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        }
    };

    ModbusProfile modbusProfile;
    EquipmentDevice equipmentDevice;
    private View rootView;
    private RecyclerView floorListView;
    List<EquipmentDevice> equipmentDeviceCollection;
    RecyclerModbusParamAdapter recyclerModbusParamAdapter;
    public static FragmentBtuMeterConfiguration newInstance(short meshAddress, String roomName, String floorName, ProfileType profileType) {
        FragmentBtuMeterConfiguration f = new FragmentBtuMeterConfiguration();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal());
        f.setArguments(bundle);
        return f;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_btu_meter_configuration, container, false);
        initConfiguration();
        return rootView;
    }

    void initConfiguration() {
        floorListView = rootView.findViewById(R.id.floorList);
        floorList = HSUtil.getFloors();
        Collections.sort(floorList, floorComparator);
        EnergyDistributionAdapter energyDistributionAdapter = new EnergyDistributionAdapter(floorList, getContext());
        floorListView.setLayoutManager(new LinearLayoutManager(getContext()));
        floorListView.setAdapter(energyDistributionAdapter);
        equipmentDeviceCollection  = EquipsManager.getInstance().getAllEquipments();
    }

    @Override
    public String getIdString() {
        return ID;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = 1200;
            int height = 720;
            dialog.getWindow().setLayout(width, height);
        }
    }
}
