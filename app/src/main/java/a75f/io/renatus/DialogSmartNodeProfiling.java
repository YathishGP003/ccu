package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import a75f.io.api.haystack.HSUtil;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.lights.LightProfile;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;

/**
 * Created by ryant on 9/27/2017.
 */

public class DialogSmartNodeProfiling extends BaseDialogFragment
{
    
    public static final String ID = DialogSmartNodeProfiling.class.getSimpleName();
    
    Zone         mZone;
    LightProfile mLightProfile;
    
    short        mNodeAddress;
    
    String mRoomName;
    String mFloorName;
    boolean isPaired;
    
    @BindView(R.id.default_text_view)
    TextView    defaultTextView;

    @Nullable
    @BindView(R.id.rl_vav)
    RelativeLayout rlVAV;

    @Nullable
    @BindView(R.id.rl_dab)
    RelativeLayout rlDAB;

    @Nullable
    @BindView(R.id.rl_light)
    RelativeLayout rlLight;

    @Nullable
    @BindView(R.id.rl_tempmonitor)
    RelativeLayout rlTempMonitor;

    @Nullable
    @BindView(R.id.rl_cpc)
    RelativeLayout rlCPC;

    @Nullable
    @BindView(R.id.rl_picontrol)
    RelativeLayout rlPiControl;

    @Nullable
    @BindView(R.id.rl_energymeter)
    RelativeLayout rlEnergyMeter;

    @Nullable
    @BindView(R.id.rl_iftt)
    RelativeLayout rlIFTT;

    @Nullable
    @BindView(R.id.rl_vavNoFan)
    RelativeLayout rlVAVNoFan;

    @Nullable
    @BindView(R.id.rl_vavSeriesFan)
    RelativeLayout rlVAVSeriesFan;

    @Nullable
    @BindView(R.id.rl_vavParallelFan)
    RelativeLayout rlVAVParallelFan;

    @Nullable
    @BindView(R.id.imageViewArrow)
    ImageView imageViewArrow;

    @Nullable
    @BindView(R.id.ll_vavmods)
    LinearLayout lt_VAVProfile;

    @Nullable
    @BindView(R.id.imageGoback)
    ImageView imageGoback;
    
    @Nullable
    @BindView(R.id.ll_dabmods)
    LinearLayout lt_DabProfile;
    
    @Nullable
    @BindView(R.id.rl_dabSingleDuct)
    RelativeLayout rlDabSingleDuct;
    
    @Nullable
    @BindView(R.id.rl_dabDualDuct)
    RelativeLayout rlDabDualDuct;
    
    @Nullable
    @BindView(R.id.dabImageViewArrow)
    ImageView dabImageViewArrow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        defaultTextView.setText(HSUtil.getDis(mRoomName));
    }

    @Optional
    @OnClick(R.id.imageGoback)
    void onGoBackButtonClick()
    {
        removeDialogFragment(ID);
    }

    @Optional
    @OnClick(R.id.rl_vav)
    void onVAVOnClick()
    {
        if(lt_VAVProfile.getVisibility() == View.VISIBLE)
        {
            lt_VAVProfile.setVisibility(View.GONE);
            imageViewArrow.setRotation(0);
        }else {
            if (lt_VAVProfile.getVisibility() == View.GONE) {
                lt_VAVProfile.setVisibility(View.VISIBLE);
                imageViewArrow.setRotation(-90);
            }
        }
    }
    
    @Optional
    @OnClick(R.id.rl_dabTitle)
    void onDabOnClick()
    {
        if(lt_DabProfile.getVisibility() == View.VISIBLE)
        {
            lt_DabProfile.setVisibility(View.GONE);
            dabImageViewArrow.setRotation(0);
        }else {
            if (lt_DabProfile.getVisibility() == View.GONE) {
                lt_DabProfile.setVisibility(View.VISIBLE);
                dabImageViewArrow.setRotation(-90);
            }
        }
    }
    
    @Optional
    @OnClick(R.id.rl_dabSingleDuct)
    void onDabSingleDuctOnClick()
    {
        showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress,
                                                                    mRoomName,
                                                                    mFloorName,
                                                                    ProfileType.DAB,
                                                                    NodeType.SMART_NODE),
                           FragmentBLEInstructionScreen.ID
        );
    }
    
    
    @Optional
    @OnClick(R.id.rl_dabDualDuct)
    void onDabDualDuctOnClick()
    {
        showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress,
                                                                    mRoomName,
                                                                    mFloorName,
                                                                    ProfileType.DUAL_DUCT,
                                                                    NodeType.SMART_NODE),
                           FragmentBLEInstructionScreen.ID
        );
    }

    @Optional
    @OnClick(R.id.rl_sse)
    void onSSEOnClick()
    {
        showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SSE, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
    }
    @Optional
    @OnClick(R.id.rl_light)
    void onLightonClick()
    {
        //Not Yet Done
        //showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.LIGHT, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
    }

    @Optional
    @OnClick(R.id.rl_tempmonitor)
    void onTempMonitorOnClick()
    {
        showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.TEMP_MONITOR, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
    }

    @Optional
    @OnClick(R.id.rl_cpc)
    void onCPCOnClick()
    {

    }

    @Optional
    @OnClick(R.id.rl_picontrol)
    void onPiControlOnClick()
    {
        if(!isPaired) {
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.PLC, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
        }else{
            Toast.makeText(getActivity(), "Pi Loop cannot be paired with existing any module in this zone", Toast.LENGTH_LONG).show();
        }
    }

    @Optional
    @OnClick(R.id.rl_energymeter)
    void onEnergyMeterOnClick()
    {
        if(!isPaired) {
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.EMR, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
        }else{
            Toast.makeText(getActivity(), "Energy meter cannot be paired with existing any module in this zone", Toast.LENGTH_LONG).show();
        }
    }

    @Optional
    @OnClick(R.id.rl_iftt)
    void onIFTTOnClick()
    {
        //Not Yet Done
    }


    @Optional
    @OnClick(R.id.rl_vavNoFan)
    void onVAVNoFanOnClick()
    {
        showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.VAV_REHEAT, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
    }


    @Optional
    @OnClick(R.id.rl_vavSeriesFan)
    void onVAVSeriesFanOnClick()
    {
        //Not Yet Done
        //showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.VAV_SERIES_FAN, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
    }

    @Optional
    @OnClick(R.id.rl_vavParallelFan)
    void onVAVParallelFanOnClick()
    {
        //Not Yet Done
        //showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.VAV_PARALLEL_FAN, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
    }

    /*@OnClick(R.id.first_button)
    void onFirstButtonClick()
    {
        removeDialogFragment(ID);
    }*/
    
   /* @OnClick(R.id.second_button)
    void onSecondButtonClick()
    {
        openBLEPairingInstructions();
    }*/
    
    /*@OnClick(R.id.vavModuleTypeRB)
    void onVavSelected() {
        vavUnitSelector.setVisibility(View.VISIBLE);
    }
    */
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.wrm_module_selection, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        isPaired = getArguments().getBoolean(FragmentCommonBundleArgs.ALREADY_PAIRED);
        //mZone = L.findZoneByName(mFloorName, mRoomName);
        //mLightProfile = (LightProfile) mZone.findProfile(ProfileType.LIGHT);
        
        return view;
    }
    
   /* private void openBLEPairingInstructions()
    {
        if (lcmModuleTypeRB.isChecked())
        {
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.LIGHT, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
            
        }
        else if (sseModuleTypeRB.isChecked())
        {
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SSE, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
        }
        else if (hwpModuleTypeRB.isChecked())
        {
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.HMP, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
        }
        else if (vavModuleTypeRB.isChecked())
        {
            ProfileType profile = null;
            switch (vavUnitSelector.getCheckedRadioButtonId()) {
                case R.id.vavReheat:
                    profile = ProfileType.VAV_REHEAT;
                    break;
                case R.id.vavSeriesFan:
                    profile = ProfileType.VAV_SERIES_FAN;
                    break;
                case R.id.vavParallelFan:
                    profile = ProfileType.VAV_PARALLEL_FAN;
                    break;
                    
            }
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, profile, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
        } else if (piLoopModuleTypeRB.isChecked()) {
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.PLC, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
        }  else if (dabModuleTypeRB.isChecked()) {
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.DAB, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
        } else if (emrModuleTypeRB.isChecked()) {
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.EMR, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
        }
    }
    */
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
        setTitle();
    }
    
    private void setTitle() {
        Dialog dialog = getDialog();
        
        if (dialog == null) {
            return;
        }
        dialog.setTitle("Select Module Type");
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
    
    public static DialogSmartNodeProfiling newInstance(short meshAddress, String roomName, String floorName,boolean isPaired)
    {
        DialogSmartNodeProfiling f = new DialogSmartNodeProfiling();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putBoolean(FragmentCommonBundleArgs.ALREADY_PAIRED, isPaired);
        f.setArguments(bundle);
        return f;
    }
    @Override
    public String getIdString()
    {
        return ID;
    }
}
