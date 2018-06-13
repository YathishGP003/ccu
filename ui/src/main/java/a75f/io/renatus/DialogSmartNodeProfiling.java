package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.NodeType;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.logic.L;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
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
    
    short        mNodeAddress;
    
    String mRoomName;
    String mFloorName;
    
    @BindView(R.id.default_text_view)
    TextView    defaultTextView;
    @BindView(R.id.wrmProfilingRadioGrp)
    RadioGroup  wrmProfilingRadioGrp;
    @BindView(R.id.dabModuleTypeRB)
    RadioButton dabModuleTypeRB;
    @BindView(R.id.vavModuleTypeRB)
    RadioButton vavModuleTypeRB;
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
        
        return view;
    }
    
    private void openBLEPairingInstructions()
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
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.VAV, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
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
    @Override
    public String getIdString()
    {
        return ID;
    }
}
