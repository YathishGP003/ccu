package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
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
    Node         mNode;
    short        mNodeAddress;
    
    String       mRoomName;
    String       mFloorName;
    
    public static FragmentSelectDeviceType newInstance(short meshAddress, String roomName, String mFloorName)
    {
        FragmentSelectDeviceType f = new FragmentSelectDeviceType();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, mFloorName);
        f.setArguments(bundle);
        return f;
    }
    
    @BindView(R.id.default_text_view)
                                       TextView    defaultTextView;
    @BindView(R.id.deviceTypeSelection)RadioGroup  deviceTypeSelection;
    @BindView(R.id.smartNode)          RadioButton smartNode;
    @BindView(R.id.smartstat)          RadioButton smartstat;
    @BindView(R.id.hia)                RadioButton hia;
    @BindView(R.id.lpi)                RadioButton lpi;
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
        defaultTextView.setText(mRoomName + " - " + "Select device type to pair");
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
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mZone = L.findZoneByName(mFloorName, mRoomName);
        mLightProfile = (LightProfile) mZone.findProfile(ProfileType.LIGHT);
        mNode = mZone.getSmartNode(mNodeAddress);
        
        return view;
    }

    
    private void OpenModuleSelectionDialog()
    {
        
        if (smartNode.isChecked())
        {
            DialogSmartNodeProfiling wrmProfiling = DialogSmartNodeProfiling.newInstance(mNodeAddress, mRoomName, mFloorName);
            showDialogFragment(wrmProfiling, DialogSmartNodeProfiling.ID);
            
        }
        else if (smartstat.isChecked())
        {
            DialogSmartStatProfiling hiaselection = DialogSmartStatProfiling.newInstance(mNodeAddress, mRoomName, mFloorName);
            showDialogFragment(hiaselection, DialogSmartStatProfiling.ID);
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
}
