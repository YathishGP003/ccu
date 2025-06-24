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
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.lights.LightProfile;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.bacnet.BacNetSelectModelView;
import a75f.io.renatus.modbus.ModbusConfigView;
import a75f.io.renatus.modbus.util.ModbusLevel;

public class FragmentModbusType  extends BaseDialogFragment {

    public static final String MID = FragmentModbusType.class.getSimpleName();
    View modbusequip;
    View modbusem;
    View bacnetEm;
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
        bacnetEm = view.findViewById(R.id.rl_bacnet_equipment);
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        misPaired = getArguments().getBoolean(FragmentCommonBundleArgs.ALREADY_PAIRED);
        modbusequip.setOnClickListener(v -> {
            showDialogFragment(ModbusConfigView.Companion.newInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.MODBUS_DEFAULT,ModbusLevel.ZONE,""), ModbusConfigView.Companion.getID());
        });
        modbusem.setOnClickListener(v -> {
            ArrayList<Equip> zoneEquips = HSUtil.getEquips(mRoomName);
            if (zoneEquips.size() > 0){
                Toast.makeText(getActivity(), "Unpair all Modbus Modules and try to pair Energy meter", Toast.LENGTH_LONG).show();
                return;
            }
            showDialogFragment(ModbusConfigView.Companion.newInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.MODBUS_EMR,ModbusLevel.ZONE,""), ModbusConfigView.Companion.getID());
        });
        (view.findViewById(R.id.imageGoback)).setOnClickListener((v)->removeDialogFragment(MID));
        bacnetEm.setOnClickListener(v -> {
            showDialogFragment(BacNetSelectModelView.Companion.newInstance(String.valueOf(mNodeAddress), mRoomName, mFloorName, ProfileType.BACNET_DEFAULT,ModbusLevel.ZONE,""), BacNetSelectModelView.Companion.getID());
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
