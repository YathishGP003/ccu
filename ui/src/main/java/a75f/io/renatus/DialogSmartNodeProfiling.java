package a75f.io.renatus;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.Node;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.logic.L;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.BLE.FragmentDeviceScan;
import a75f.io.renatus.ZONEPROFILE.LightingZoneProfileFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ryant on 9/27/2017.
 */

public class DialogSmartNodeProfiling extends BaseDialogFragment
{
    
    public static final String ID = DialogSmartNodeProfiling.class.getSimpleName();
    
    Zone         mZone;
    LightProfile mLightProfile;
    Node         mNode;
    short        mNodeAddress;
    
    String mRoomName;
    String mFloorName;
    
    @BindView(R.id.default_text_view)
    TextView    defaultTextView;
    @BindView(R.id.wrmProfilingRadioGrp)
    RadioGroup  wrmProfilingRadioGrp;
    @BindView(R.id.dabModuleTypeRB)
    RadioButton dabModuleTypeRB;
    @BindView(R.id.lcmModuleTypeRB)
    RadioButton lcmModuleTypeRB;
    @BindView(R.id.sseModuleTypeRB)
    RadioButton sseModuleTypeRB;
    @BindView(R.id.rmModuleTypeRB)
    RadioButton rmModuleTypeRB;
    @BindView(R.id.emrModuleTypeRB)
    RadioButton emrModuleTypeRB;
    @BindView(R.id.ccmModuleTypeRB)
    RadioButton ccmModuleTypeRB;
    @BindView(R.id.hwpModuleTypeRB)
    RadioButton hwpModuleTypeRB;
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        defaultTextView.setText(mRoomName);
    }
    
    @OnClick(R.id.first_button)
    void onFirstButtonClick()
    {
        removeDialogFragment(ID);
    }
    
    @OnClick(R.id.second_button)
    void onSecondButtonClick()
    {
        openBLEPairingInstructions();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.wrm_module_selection, container, false);
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mZone = L.findZoneByName(mFloorName, mRoomName);
        mLightProfile = (LightProfile) mZone.findProfile(ProfileType.LIGHT);
        mNode = mZone.getSmartNode(mNodeAddress);
        return view;
    }
    
    private void openBLEPairingInstructions()
    {
        if (lcmModuleTypeRB.isChecked())
        {
            if (L.isSimulation())
            {
                showDialogFragment(LightingZoneProfileFragment.newInstance(mNodeAddress, mRoomName, mFloorName), LightingZoneProfileFragment.ID);
            }
            else
            {
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.LIGHT);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (sseModuleTypeRB.isChecked())
        {
            if (L.isSimulation())
            {
                showDialogFragment(FragmentSSEConfiguration.newInstance(mNodeAddress, mRoomName, mFloorName), FragmentSSEConfiguration.ID);
            }
            else
            {
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SSE);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
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
    }
    
//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState)
//    {
//        LayoutInflater inflater = LayoutInflater.from(getActivity());
//        View mView = inflater.inflate(R.layout.wrm_module_selection, null);
//        ButterKnife.bind(this, mView);
//        // Log.e(TAG, "INFLATED");
//        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
//        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
//        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
//        mZone = L.findZoneByName(mFloorName, mRoomName);
//        mLightProfile = (LightProfile) mZone.findProfile(ProfileType.LIGHT);
//        mNode = mZone.getSmartNode(mNodeAddress);
//        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle);
//        alertBuilder.setTitle(mRoomName);
//        alertBuilder.setView(mView);
//        alertBuilder.setCancelable(false);
//        alertBuilder.setPositiveButton("Pair", new DialogInterface.OnClickListener()
//        {
//            @Override
//            public void onClick(DialogInterface dialog, int which)
//            {
//                if (lcmModuleTypeRB.isChecked())
//                {
//                    if (L.isSimulation())
//                    {
//                        showDialogFragment(LightingZoneProfileFragment.newInstance(mNodeAddress, mRoomName, mFloorName), LightingZoneProfileFragment.ID);
//                    }
//                    else
//                    {
//                        FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.LIGHT);
//                        showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
//                    }
//                    dialog.dismiss();
//                }
//                else if (sseModuleTypeRB.isChecked())
//                {
//                    if (L.isSimulation())
//                    {
//                        showDialogFragment(FragmentSSEConfiguration.newInstance(mNodeAddress, mRoomName, mFloorName), FragmentSSEConfiguration.ID);
//                    }
//                    else
//                    {
//                        FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SSE);
//                        showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
//                    }
//                    dialog.dismiss();
//                }
//            }
//        });
//        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
//        {
//            @Override
//            public void onClick(DialogInterface dialog, int which)
//            {
//                dialog.dismiss();
//            }
//        });
//        return alertBuilder.create();
//    }
    
    public static DialogSmartNodeProfiling newInstance(short meshAddress, String roomName, String floorName)
    {
        DialogSmartNodeProfiling f = new DialogSmartNodeProfiling();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        f.setArguments(bundle);
        return f;
    }
}
