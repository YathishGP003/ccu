package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

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
import a75f.io.logic.L;
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

import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.PROFILE_TYPE;

/**
 * Created by ryant on 9/27/2017.
 */

public class DialogSmartNodeProfiling extends BaseDialogFragment
{
    
    public static final String ID = DialogSmartNodeProfiling.class.getSimpleName();
    
    Zone         mZone;
    LightProfile mLightProfile;
    ProfileType mProfileType;
    
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
    @BindView(R.id.rl_vav_head)
    RelativeLayout rlVAVHead;

    @Nullable
    @BindView(R.id.rl_dabTitle)
    RelativeLayout rlDAB;

    @Nullable
    @BindView(R.id.rl_dab_head)
    RelativeLayout rlDABHead;

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

    @Nullable
    @BindView(R.id.ivDabSingleDuct)
    ImageView ivDabSingleDuct;

    @Nullable
    @BindView(R.id.ivDabDualDuct)
    ImageView ivDabDualDuct;

    @Nullable
    @BindView(R.id.iv_vavNoFan)
    ImageView iv_vavNoFan;

    @Nullable
    @BindView(R.id.iv_vavSeriesFan)
    ImageView iv_vavSeriesFan;

    @Nullable
    @BindView(R.id.iv_vavParallelFan)
    ImageView iv_vavParallelFan;

    @Nullable
    @BindView(R.id.textDABTitle)
    TextView textDABTitle;

    @Nullable
    @BindView(R.id.textDABTitleDesc)
    TextView textDABTitleDesc;

    @Nullable
    @BindView(R.id.textDabSingleDuct)
    TextView textDabSingleDuct;

    @Nullable
    @BindView(R.id.textDabSingleDuctDesc)
    TextView textDabSingleDuctDesc;

    @Nullable
    @BindView(R.id.textDabDualDuct)
    TextView textDabDualDuct;

    @Nullable
    @BindView(R.id.textDabDualDuctDesc)
    TextView textDabDualDuctDesc;

    @Nullable
    @BindView(R.id.textVAV)
    TextView textVAV;

    @Nullable
    @BindView(R.id.textVAVdesc)
    TextView textVAVdesc;

    @Nullable
    @BindView(R.id.textNoFan)
    TextView textNoFan;

    @Nullable
    @BindView(R.id.textNoFanDesc)
    TextView textNoFanDesc;

    @Nullable
    @BindView(R.id.textSeriesFan)
    TextView textSeriesFan;

    @Nullable
    @BindView(R.id.textSeriesFanDesc)
    TextView textSeriesFanDesc;

    @Nullable
    @BindView(R.id.textParallelFan)
    TextView textParallelFan;

    @Nullable
    @BindView(R.id.textParallelDesc)
    TextView textParallelDesc;

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
        showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress,
                                                                    mRoomName,
                                                                    mFloorName,
                                                                    ProfileType.VAV_SERIES_FAN,
                                                                    NodeType.SMART_NODE),
                           FragmentBLEInstructionScreen.ID
        );
    }

    @Optional
    @OnClick(R.id.rl_vavParallelFan)
    void onVAVParallelFanOnClick()
    {
        showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress,
                                                                    mRoomName,
                                                                    mFloorName,
                                                                    ProfileType.VAV_PARALLEL_FAN,
                                                                    NodeType.SMART_NODE),
                           FragmentBLEInstructionScreen.ID
        );
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
        mProfileType = ProfileType.values()[getArguments().getInt(FragmentCommonBundleArgs.PROFILE_TYPE)];
        
        return view;
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
        /*Code to disable VAV profiles if DAB is selected*/
        if(L.ccu().systemProfile.getProfileType() == ProfileType.DAB || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_ANALOG_RTU
                || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_STAGED_RTU || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_HYBRID_RTU
                ||L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_STAGED_VFD_RTU){
            //rlVAV.setEnabled(false);
            //lt_VAVProfile.setEnabled(false);
            rlVAVNoFan.setEnabled(false);
            rlVAVSeriesFan.setEnabled(false);
            rlVAVParallelFan.setEnabled(false);

            textNoFan.setTextColor(getResources().getColor(R.color.selection_gray));
            textNoFanDesc.setTextColor(getResources().getColor(R.color.selection_gray));
            textSeriesFan.setTextColor(getResources().getColor(R.color.selection_gray));
            textSeriesFanDesc.setTextColor(getResources().getColor(R.color.selection_gray));
            textParallelFan.setTextColor(getResources().getColor(R.color.selection_gray));
            textParallelDesc.setTextColor(getResources().getColor(R.color.selection_gray));

            textVAV.setTextColor(getResources().getColor(R.color.selection_gray));
            textVAVdesc.setTextColor(getResources().getColor(R.color.selection_gray));

            imageViewArrow.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            imageViewArrow.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            iv_vavSeriesFan.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            iv_vavSeriesFan.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            iv_vavParallelFan.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            iv_vavParallelFan.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            iv_vavNoFan.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            iv_vavNoFan.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;

            imageViewArrow.setImageDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.icon_arrowright_grey));
            iv_vavSeriesFan.setImageDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.icon_arrowright_grey));
            iv_vavParallelFan.setImageDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.icon_arrowright_grey));
            iv_vavNoFan.setImageDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.icon_arrowright_grey));
        }/*Code to disable DAB profiles if VAV is selected*/
        else if (L.ccu().systemProfile.getProfileType() == ProfileType.VAV_REHEAT || L.ccu().systemProfile.getProfileType() == ProfileType.VAV_SERIES_FAN
                || L.ccu().systemProfile.getProfileType() == ProfileType.VAV_PARALLEL_FAN ||L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_ANALOG_RTU
                ||L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_RTU || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_HYBRID_RTU
                ||L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_VFD_RTU ||L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_IE_RTU){
            rlDabSingleDuct.setEnabled(false);
            rlDabDualDuct.setEnabled(false);
            textDabSingleDuct.setTextColor(getResources().getColor(R.color.selection_gray));
            textDabDualDuct.setTextColor(getResources().getColor(R.color.selection_gray));
            textDabSingleDuctDesc.setTextColor(getResources().getColor(R.color.selection_gray));
            textDabDualDuctDesc.setTextColor(getResources().getColor(R.color.selection_gray));

            textDABTitle.setTextColor(getResources().getColor(R.color.selection_gray));
            textDABTitleDesc.setTextColor(getResources().getColor(R.color.selection_gray));
            dabImageViewArrow.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            dabImageViewArrow.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;

            ivDabSingleDuct.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            ivDabSingleDuct.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;

            ivDabDualDuct.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            ivDabDualDuct.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;

            dabImageViewArrow.setImageDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.icon_arrowright_grey));
            ivDabSingleDuct.setImageDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.icon_arrowright_grey));
            ivDabDualDuct.setImageDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.icon_arrowright_grey));
        }/*Code to disable VAV as well as DAB profiles if Default System Profile is selected*/
        else if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT){
            rlVAVNoFan.setEnabled(false);
            rlVAVSeriesFan.setEnabled(false);
            rlVAVParallelFan.setEnabled(false);

            textNoFan.setTextColor(getResources().getColor(R.color.selection_gray));
            textNoFanDesc.setTextColor(getResources().getColor(R.color.selection_gray));
            textSeriesFan.setTextColor(getResources().getColor(R.color.selection_gray));
            textSeriesFanDesc.setTextColor(getResources().getColor(R.color.selection_gray));
            textParallelFan.setTextColor(getResources().getColor(R.color.selection_gray));
            textParallelDesc.setTextColor(getResources().getColor(R.color.selection_gray));

            textVAV.setTextColor(getResources().getColor(R.color.selection_gray));
            textVAVdesc.setTextColor(getResources().getColor(R.color.selection_gray));

            imageViewArrow.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            imageViewArrow.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            iv_vavSeriesFan.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            iv_vavSeriesFan.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            iv_vavParallelFan.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            iv_vavParallelFan.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            iv_vavNoFan.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            iv_vavNoFan.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;

            imageViewArrow.setImageDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.icon_arrowright_grey));
            iv_vavSeriesFan.setImageDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.icon_arrowright_grey));
            iv_vavParallelFan.setImageDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.icon_arrowright_grey));
            iv_vavNoFan.setImageDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.icon_arrowright_grey));


            rlDabSingleDuct.setEnabled(false);
            rlDabDualDuct.setEnabled(false);
            textDabSingleDuct.setTextColor(getResources().getColor(R.color.selection_gray));
            textDabDualDuct.setTextColor(getResources().getColor(R.color.selection_gray));
            textDabSingleDuctDesc.setTextColor(getResources().getColor(R.color.selection_gray));
            textDabDualDuctDesc.setTextColor(getResources().getColor(R.color.selection_gray));

            textDABTitle.setTextColor(getResources().getColor(R.color.selection_gray));
            textDABTitleDesc.setTextColor(getResources().getColor(R.color.selection_gray));
            dabImageViewArrow.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            dabImageViewArrow.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;

            ivDabSingleDuct.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            ivDabSingleDuct.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;

            ivDabDualDuct.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            ivDabDualDuct.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;

            dabImageViewArrow.setImageDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.icon_arrowright_grey));
            ivDabSingleDuct.setImageDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.icon_arrowright_grey));
            ivDabDualDuct.setImageDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.icon_arrowright_grey));
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
