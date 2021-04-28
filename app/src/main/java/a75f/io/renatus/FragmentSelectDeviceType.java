package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.lights.LightProfile;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.modbus.FragmentModbusConfiguration;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ryant on 9/27/2017.
 */

public class FragmentSelectDeviceType extends BaseDialogFragment
{
    public static final String ID = FragmentSelectDeviceType.class.getSimpleName();
    Zone         mZone;
    LightProfile mLightProfile;
    short        mNodeAddress;

    String       mRoomName;
    String       mFloorName;
    Boolean      misPaired;

    public static FragmentSelectDeviceType newInstance(short meshAddress, String roomName, String mFloorName, boolean isPaired)
    {
        FragmentSelectDeviceType f = new FragmentSelectDeviceType();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, mFloorName);
        bundle.putBoolean(FragmentCommonBundleArgs.ALREADY_PAIRED, isPaired);
        f.setArguments(bundle);
        return f;
    }

    @BindView(R.id.default_text_view)  TextView    defaultTextView;
    @BindView(R.id.deviceTypeSelection)RadioGroup  deviceTypeSelection;
    @BindView(R.id.smartNode)          RadioButton smartNode;
    @BindView(R.id.smartstat)          RadioButton smartstat;
    @BindView(R.id.hia)                RadioButton hia;
    @BindView(R.id.lpi)                RadioButton lpi;

    @BindView(R.id.rl_smartnode)
    RelativeLayout    rlSmartNode;
    @BindView(R.id.rl_smartstat)
    RelativeLayout    rlSmartStat;
    @BindView(R.id.rl_wirelesstemp)
    RelativeLayout    rlWTM;
    @BindView(R.id.rl_ccu)
    RelativeLayout    rlCCU;
    @BindView(R.id.imageGoback)
    ImageView imageGoback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        //setStyle(DialogFragment.STY, R.style.NewDialogStyle);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        defaultTextView.setText(HSUtil.getDis(mRoomName));
    }


    @OnClick(R.id.imageGoback) void onGoBackClick() {
        removeDialogFragment(ID);
    }

    @OnClick(R.id.rl_smartnode) void onSmartNodeClick() {
        if (isModbusPaired()) {
            return;
        }
        DialogSmartNodeProfiling wrmProfiling = DialogSmartNodeProfiling.newInstance(mNodeAddress, mRoomName, mFloorName, misPaired);
        showDialogFragment(wrmProfiling, DialogSmartNodeProfiling.ID);
    }

    @OnClick(R.id.rl_smartstat) void onSmartStatClick() {
        if (isModbusPaired()) {
            return;
        }
        DialogSmartStatProfiling smartStatProfiling = DialogSmartStatProfiling.newInstance(mNodeAddress, mRoomName, mFloorName);
        showDialogFragment(smartStatProfiling, DialogSmartStatProfiling.ID);
    }


    @OnClick(R.id.rl_wirelesstemp) void onWTMClick() {
        if (isModbusPaired()) {
            return;
        }
        DialogWTMProfiling wtmProfiling = DialogWTMProfiling.newInstance(mNodeAddress, mRoomName, mFloorName);
        showDialogFragment(wtmProfiling, DialogWTMProfiling.ID);
    }

    @OnClick(R.id.rl_ccu) void oCCUClick() {
        if (isModbusPaired()) {
            return;
        }
        boolean isCazExists = false;

        for(ZoneProfile zp :L.ccu().zoneProfiles) {
            if (zp.getProfileType() == ProfileType.TEMP_INFLUENCE) {
                isCazExists = true;
                break;
            }
        }
        if(isCazExists){
            closeAllBaseDialogFragments();
            Toast.makeText(getContext(),"CCU As Zone temperature influence is already enabled",Toast.LENGTH_LONG).show();
        }else if(misPaired){
            closeAllBaseDialogFragments();
            Toast.makeText(getContext(),"Temperature Influence profile cannot be paired with already paired modules",Toast.LENGTH_LONG).show();

        }
        else {
            //For CCU we have it as start address  ending with 99
            String ccuAddr = String.valueOf(mNodeAddress).substring(0, 2).concat("99");
            DialogCCUProfiling ccuProfiling = DialogCCUProfiling.newInstance(Short.parseShort(ccuAddr), mRoomName, mFloorName);
            showDialogFragment(ccuProfiling, DialogCCUProfiling.ID);
        }
    }

    @OnClick(R.id.rlModbus) void onModBusClick(){
        if (isNotModbus()){
            return;
        }
        showDialogFragment(FragmentModbusType.newInstance(mNodeAddress, mRoomName, mFloorName, misPaired),FragmentModbusType.MID);
    }

    @OnClick(R.id.first_button) void onFirstButtonClick() {
        removeDialogFragment(ID);
    }


    @OnClick(R.id.second_button) void onSecondButtonClick() {
        OpenModuleSelectionDialog();
    }



    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.devicetypeselectiondialog, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        misPaired = getArguments().getBoolean(FragmentCommonBundleArgs.ALREADY_PAIRED);
        //mZone = L.findZoneByName(mFloorName, mRoomName);
        //mLightProfile = (LightProfile) mZone.findProfile(ProfileType.LIGHT);

        return view;
    }


    private void OpenModuleSelectionDialog()
    {

        if (smartNode.isChecked())
        {
            DialogSmartNodeProfiling wrmProfiling = DialogSmartNodeProfiling.newInstance(mNodeAddress, mRoomName, mFloorName, misPaired);
            showDialogFragment(wrmProfiling, DialogSmartNodeProfiling.ID);

        }
        else if (smartstat.isChecked())
        {
            DialogSmartStatProfiling smartStatProfiling = DialogSmartStatProfiling.newInstance(mNodeAddress, mRoomName, mFloorName);
            showDialogFragment(smartStatProfiling, DialogSmartStatProfiling.ID);
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null)
        {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
        //setTitle();
    }

    private void setTitle() {
        Dialog dialog = getDialog();

        if (dialog == null) {
            return;
        }
        dialog.setTitle("Select device type to pair");
        TextView titleView = this.getDialog().findViewById(android.R.id.title);
        if(titleView != null)
        {
            titleView.setGravity(Gravity.CENTER);
            titleView.setTextColor(getResources().getColor(R.color.progress_color_orange));
        }
        int titleDividerId = getContext().getResources()
                .getIdentifier("titleDivider", "id", "android");

        View titleDivider = dialog.findViewById(titleDividerId);
        if (titleDivider != null) {
            titleDivider.setBackgroundColor(getContext().getResources()
                    .getColor(R.color.transparent));
        }
    }

    @Override
    public String getIdString()
    {
        return ID;
    }

    private boolean isModbusPaired() {
        ArrayList<Equip> zoneEquips  = HSUtil.getEquips(mRoomName);
        for (Equip equip: zoneEquips) {
            if (equip.getProfile().contains("MODBUS")) {
                Toast.makeText(getActivity(), "Unpair all Modbus Modules and try", Toast.LENGTH_LONG).show();
                closeAllBaseDialogFragments();
                return true;
            }
        }
        return false;
    }

    private boolean isNotModbus() {
        ArrayList<Equip> zoneEquips  = HSUtil.getEquips(mRoomName);
        for (Equip equip: zoneEquips) {
            if (!equip.getProfile().contains("MODBUS")) {
                Toast.makeText(getActivity(), "Unpair all Modules and try", Toast.LENGTH_LONG).show();
                closeAllBaseDialogFragments();
                return true;
            }
        }
        return false;
    }
}
