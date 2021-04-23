package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.lights.LightProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.modbus.FragmentModbusConfiguration;
import a75f.io.renatus.modbus.FragmentModbusEnergyMeterConfiguration;
import butterknife.OnClick;

import static a75f.io.renatus.FloorPlanFragment.selectedZone;

public class FragmentModbusType  extends BaseDialogFragment {

    public static final String MID = FragmentModbusType.class.getSimpleName();
    View modbusequip;
    View modbusem;
    Zone mZone;
    LightProfile mLightProfile;
    short        mNodeAddress;

    String       mRoomName;
    String       mFloorName;
    Boolean      misPaired;


    @Override
    public String getIdString() {
        return MID;
    }


    public static FragmentModbusType newInstance(short meshAddress, String roomName, String floorName,boolean isPaired){
        FragmentModbusType f = new FragmentModbusType();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putBoolean(FragmentCommonBundleArgs.ALREADY_PAIRED, isPaired);
        f.setArguments(bundle);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.modbustypeselectiondialog, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        modbusequip  = view.findViewById(R.id.rl_mbequipment);
        modbusem = view.findViewById(R.id.rl_mbenergymeter);
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        misPaired = getArguments().getBoolean(FragmentCommonBundleArgs.ALREADY_PAIRED);
        modbusequip.setOnClickListener(v -> {
            FragmentModbusConfiguration modBusConfiguration = FragmentModbusConfiguration.newInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.MODBUS_UPS30);
            showDialogFragment(modBusConfiguration, FragmentModbusConfiguration.ID);
        });
        modbusem.setOnClickListener(v -> {
            FragmentModbusEnergyMeterConfiguration modBusEmConfiguration = FragmentModbusEnergyMeterConfiguration.newInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.MODBUS_EMR);
            showDialogFragment(modBusEmConfiguration, FragmentModbusEnergyMeterConfiguration.ID);
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null)
        {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }
}
