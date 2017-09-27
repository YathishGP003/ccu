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

/**
 * Created by ryant on 9/27/2017.
 */

public class DialogSmartNodeProfiling extends BaseDialogFragment
{
    
    public static final String ID = DialogSmartNodeProfiling.class.getSimpleName();
    private static final String TAG  = DialogSmartNodeProfiling.class.getSimpleName();
    
    Zone     mZone;
    LightProfile mLightProfile;
    Node         mNode;
    short        mNodeAddress;
    
    String       mRoomName;
    String       mFloorName;
    
//    @BindView(R.id.wrmProfilingRadioGrp) RadioGroup  wrmProfilingRadioGrp;
//    @BindView(R.id.dabModuleTypeRB)          RadioButton dabModuleTypeRB;
//    @BindView(R.id.lcmModuleTypeRB)          RadioButton lcmModuleTypeRB;
//    @BindView(R.id.sseModuleTypeRB)          RadioButton sseModuleTypeRB;
//    @BindView(R.id.rmModuleTypeRB)           RadioButton rmModuleTypeRB;
//    @BindView(R.id.emrModuleTypeRB)          RadioButton emrModuleTypeRB;
//    @BindView(R.id.ccmModuleTypeRB)          RadioButton ccmModuleTypeRB;
//    @BindView(R.id.hwpModuleTypeRB)          RadioButton hwpModuleTypeRB;
    
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View mView =  inflater.inflate(R.layout.wrm_module_selection, null);
        //ButterKnife.bind(this, mView);
       // Log.e(TAG, "INFLATED");
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mZone = L.findZoneByName(mFloorName, mRoomName);
        mLightProfile = (LightProfile) mZone.findProfile(ProfileType.LIGHT);
        mNode = mZone.getSmartNode(mNodeAddress);
        AlertDialog.Builder alertBuilder= new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle);
        alertBuilder.setTitle(mRoomName);
  
        alertBuilder.setView(mView);
        
        alertBuilder.setCancelable(false);
        alertBuilder.setPositiveButton("Pair", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

//                if(lcmModuleTypeRB.isChecked())
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
//                else if(sseModuleTypeRB.isChecked())
//                {
//                    if (L.isSimulation())
//                    {
//                        showDialogFragment(FragmentSSEConfiguration.newInstance(mNodeAddress, mRoomName, mFloorName), FragmentSSEConfiguration.ID);
//                    }
//                    else
//                    {
//                        FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SSE);
//                        showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
//
//                    }
//                    dialog.dismiss();
//                }
            }
        });
        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        return alertBuilder.create();

    }
    
    

    
    
    
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
    
//
//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//        LayoutInflater inflater = (LayoutInflater) LayoutInflater.from(getActivity());
//        View view = inflater.inflate(R.layout.wrm_module_selection, null);
//        Bundle b = getArguments();
//        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.wrmProfilingRadioGrp);
//        RadioButton dabModule = (RadioButton) view.findViewById(R.id.dabModuleTypeRB);
//        RadioButton sseModule = (RadioButton) view.findViewById(R.id.sseModuleTypeRB);
//        RadioButton refModule = (RadioButton) view.findViewById(R.id.rmModuleTypeRB);
//        RadioButton emModule = (RadioButton) view.findViewById(R.id.emrModuleTypeRB);
//        RadioButton ifttModule = (RadioButton) view.findViewById(R.id.ccmModuleTypeRB);
//        RadioButton hwmModule = (RadioButton) view.findViewById(R.id.hwpModuleTypeRB);
//        dabModule.setChecked(true);
//        if(hasModules) {
//            RadioButton rmModule = (RadioButton) view.findViewById(R.id.rmModuleTypeRB);
//            rmModule.setClickable(false);
//            rmModule.setOnCheckedChangeListener(null);
//            RadioButton hmpModule = (RadioButton) view.findViewById(R.id.hwpModuleTypeRB);
//            hmpModule.setClickable(false);
//            hmpModule.setOnCheckedChangeListener(null);
//        }
//        radioGroup.setOnCheckedChangeListener(this);
//
//        AlertDialog.Builder alertBuilder= new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle);
//        alertBuilder.setTitle(b.getString("roomname"));
//        alertBuilder.setView(view);
//        alertBuilder.setCancelable(false);
//        alertBuilder.setPositiveButton("Pair", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                FragmentTransaction ft = getFragmentManager().beginTransaction();
//                PairingInstructionDialog dlg = PairingInstructionDialog.newInstance(frag,selectedModule,deviceType, CCUUtils.WRM_DEVICE_TYPE.SMARTNODE.ordinal());
//                dlg.show(ft, PairingInstructionDialog.ID);
//                dialog.dismiss();
//            }
//        });
//        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//            }
//        });
//
//        if (!AlgoTuningParameters.getHandle().getUseSmartNodeInstall()) {//update
//
//
//            refModule.setEnabled(true);
//            refModule.setFocusable(true);
//            refModule.setClickable(true);
//            refModule.setTextColor(Color.BLACK);
//        }
//        return alertBuilder.create();
//    }
    
    
    
    
    
}
