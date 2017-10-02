package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import a75f.io.bo.building.NodeType;
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

import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.ARG_NAME;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.ARG_PAIRING_ADDR;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.FLOOR_NAME;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.NODE_TYPE;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.PROFILE_TYPE;

/**
 * Created by ryant on 9/28/2017.
 */

public class FragmentBLEInstructionScreen extends BaseDialogFragment
{
    
    public static final String ID = FragmentBLEInstructionScreen.class.getSimpleName();
    @BindView(R.id.pairinginstruct)
    ImageView pairinginstruct;
    @BindView(R.id.title)
    TextView  title;
    NodeType    mNodeType;
    Zone        mZone;
    ProfileType mProfileType;
    short       mNodeAddress;
    
    String mRoomName;
    String mFloorName;
    
    
    public static FragmentBLEInstructionScreen getInstance(short nodeAddress, String roomName,
                                                           String floorName,
                                                           ProfileType profileType,
                                                           NodeType nodeType)
    {
        FragmentBLEInstructionScreen fds = new FragmentBLEInstructionScreen();
        Bundle args = new Bundle();
        args.putShort(ARG_PAIRING_ADDR, nodeAddress);
        args.putString(ARG_NAME, roomName);
        args.putString(FLOOR_NAME, floorName);
        args.putString(PROFILE_TYPE, profileType.name());
        args.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.name());
        fds.setArguments(args);
        return fds;
    }
    
    
    @OnClick(R.id.first_button)
    void onFirstButtonClick()
    {
        removeDialogFragment(ID);
    }
    
    
    @OnClick(R.id.second_button)
    void onSecondButtonClick()
    {
        openBLEPairing();
    }
    
    
    private void openBLEPairing()
    {
        if (mProfileType == ProfileType.LIGHT)
        {
            if (L.isSimulation())
            {
                showDialogFragment(LightingZoneProfileFragment
                                           .newInstance(mNodeAddress, mRoomName, mNodeType, mFloorName), LightingZoneProfileFragment.ID);
            }
            else
            {
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan
                                                                .getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.LIGHT);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.SSE)
        {
            if (L.isSimulation())
            {
                showDialogFragment(FragmentSSEConfiguration
                                           .newInstance(mNodeAddress, mRoomName, mFloorName), FragmentSSEConfiguration.ID);
            }
            else
            {
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan
                                                                .getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.SSE);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.pairinginstructionscreen, container, false);
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mZone = L.findZoneByName(mFloorName, mRoomName);
        mProfileType = ProfileType.valueOf(getArguments().getString(PROFILE_TYPE));
        mNodeType = NodeType.valueOf(getArguments().getString(NODE_TYPE));
        return view;
    }
    
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        if (mNodeType == NodeType.SMART_NODE)
        {
            pairinginstruct.setImageResource(R.drawable.pairinginstructionsn);
        }
        else if (mNodeType == NodeType.SMART_STAT)
        {
            pairinginstruct.setImageResource(R.drawable.pairinginstruct);
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
    
    
    @Override
    public String getIdString()
    {
        return ID;
    }
}
