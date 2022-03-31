package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.BLE.FragmentDeviceScan;

import a75f.io.renatus.hyperstat.cpu.HyperStatCpuFragment;
import a75f.io.renatus.hyperstat.vrv.HyperStatVrvFragment;
import a75f.io.renatus.util.CCUUiUtil;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;

import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.ARG_NAME;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.ARG_PAIRING_ADDR;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.FLOOR_NAME;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.NODE_TYPE;
import static a75f.io.renatus.BASE.FragmentCommonBundleArgs.PROFILE_TYPE;

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

    @BindView(R.id.imageGoback)
    ImageView imageGoback;

    @BindView(R.id.pairinginstruct_daikin)
    ImageView pairinginstructDaikin;

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

    @Optional
    @OnClick(R.id.imageGoback)
    void onGoBackButtonClick()
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
        if (mProfileType == ProfileType.SSE)
        {
            if (L.isSimulation())
            {
                showDialogFragment(FragmentSSEConfiguration
                                           .newInstance(mNodeAddress, mRoomName, mNodeType, mFloorName,ProfileType.SSE), FragmentSSEConfiguration.ID);
            }
            else
            {
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan
                                                                .getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.SSE);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.HMP)
        {
            if (L.isSimulation())
            {
                showDialogFragment(FragmentHMPConfiguration
                                           .newInstance(mNodeAddress, mRoomName, mNodeType, mFloorName), FragmentHMPConfiguration.ID);
            }
            else
            {
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan
                                                                .getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.HMP);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.PLC)
        {
            if (L.isSimulation())
            {
                showDialogFragment(FragmentPLCConfiguration
                                           .newInstance(mNodeAddress, mRoomName, mNodeType, mFloorName), FragmentPLCConfiguration.ID);
            }
            else
            {
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan
                                                                .getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.PLC);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.EMR)
        {
            if (L.isSimulation())
            {
                showDialogFragment(FragmentEMRConfiguration
                                           .newInstance(mNodeAddress, mRoomName, mNodeType, mFloorName), FragmentEMRConfiguration.ID);
            }
            else
            {
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan
                                                                .getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.EMR);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.DAB)
        {
            if (L.ccu().systemProfile.getProfileType() == ProfileType.VAV_REHEAT || L.ccu().systemProfile.getProfileType() == ProfileType.VAV_SERIES_FAN || L.ccu().systemProfile.getProfileType() == ProfileType.VAV_PARALLEL_FAN
                    ||L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_ANALOG_RTU ||L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_RTU || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_HYBRID_RTU ||L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_VFD_RTU
                    ||L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_IE_RTU || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT){
                Toast.makeText(getActivity(),"Set System Profile to DAB and try",Toast.LENGTH_LONG).show();
                dismiss();
                return;
            }
            ArrayList<Equip> zoneEquips  = HSUtil.getEquips(mRoomName);
            for (Equip equip: zoneEquips) {
                if (equip.getProfile().contains("VAV")) {
                    Toast.makeText(getActivity(), "Unpair all VAV Zones and try", Toast.LENGTH_LONG).show();
                    dismiss();
                    return;
                }
            }
            if (L.isSimulation())
            {
                showDialogFragment(FragmentDABConfiguration
                                           .newInstance(mNodeAddress, mRoomName, mNodeType, mFloorName, mProfileType), FragmentDABConfiguration.ID);
            }
            else
            {
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan
                                                                .getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.DAB);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.DUAL_DUCT)
        {
            if (L.ccu().systemProfile.getProfileType() == ProfileType.VAV_REHEAT || L.ccu().systemProfile.getProfileType() == ProfileType.VAV_SERIES_FAN || L.ccu().systemProfile.getProfileType() == ProfileType.VAV_PARALLEL_FAN
                ||L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_ANALOG_RTU ||L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_RTU || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_HYBRID_RTU ||L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_VFD_RTU
                ||L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_IE_RTU || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT){
                Toast.makeText(getActivity(),"Set System Profile to DAB and try",Toast.LENGTH_LONG).show();
                dismiss();
                return;
            }
            ArrayList<Equip> zoneEquips  = HSUtil.getEquips(mRoomName);
            for (Equip equip: zoneEquips) {
                if (equip.getProfile().contains("VAV")) {
                    Toast.makeText(getActivity(), "Unpair all VAV Zones and try", Toast.LENGTH_LONG).show();
                    dismiss();
                    return;
                }
            }
            if (L.isSimulation())
            {
                showDialogFragment(FragmentDABDualDuctConfiguration.newInstance(mNodeAddress,
                                                                                mRoomName,
                                                                                mNodeType,
                                                                                mFloorName,
                                                                                mProfileType),
                                   FragmentDABDualDuctConfiguration.ID
                );
            }
            else
            {
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress,
                                                                                       mRoomName,
                                                                                       mFloorName,
                                                                                       mNodeType,
                                                                                       ProfileType.DUAL_DUCT
                );
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.VAV_REHEAT || mProfileType == ProfileType.VAV_SERIES_FAN || mProfileType == ProfileType.VAV_PARALLEL_FAN)
        {
            if(L.ccu().systemProfile.getProfileType() == ProfileType.DAB || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_ANALOG_RTU || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_STAGED_RTU || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_HYBRID_RTU
                    ||L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DAB_STAGED_VFD_RTU || L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_DEFAULT){
                Toast.makeText(getActivity(),"Set System Profile to VAV and try",Toast.LENGTH_LONG).show();
                dismiss();
                return;
            }
            ArrayList<Equip> zoneEquips  = HSUtil.getEquips(mRoomName);
           for (Equip equip: zoneEquips){
             if(equip.getProfile().contains("DAB")) {
                   Toast.makeText(getActivity(),"Unpair all DAB Zones and try",Toast.LENGTH_LONG).show();
                   dismiss();
                   return;
               }
           }
            if (L.isSimulation())
            {
                showDialogFragment(FragmentVAVConfiguration
                                           .newInstance(mNodeAddress, mRoomName, mNodeType, mFloorName, mProfileType), FragmentVAVConfiguration.ID);
            }
            else
            {
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, mProfileType);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.OAO)
        {
            if (L.isSimulation())
            {
                showDialogFragment(DialogOAOProfile
                                           .newInstance(mNodeAddress, "SYSTEM", "SYSTEM"), DialogOAOProfile.ID);
            }
            else
            {
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan
                                                                .getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.OAO);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.SMARTSTAT_CONVENTIONAL_PACK_UNIT)
        {
            if (L.isSimulation())
            {
                showDialogFragment(FragmentCPUConfiguration.newInstance(mNodeAddress, mRoomName, mNodeType, mFloorName,mProfileType), FragmentCPUConfiguration.ID);
            }
            else
            {
                Log.d("FragBleInstrScrn","CPU profile. device scan");
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.SMARTSTAT_CONVENTIONAL_PACK_UNIT);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.SMARTSTAT_HEAT_PUMP_UNIT)
        {
            if (L.isSimulation())
            {
                showDialogFragment(FragmentHeatPumpConfiguration.newInstance(mNodeAddress, mRoomName, mNodeType, mFloorName,mProfileType), FragmentHeatPumpConfiguration.ID);
            }
            else
            {
                Log.d("FragBleInstrScrn","CPU profile. device scan");
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.SMARTSTAT_HEAT_PUMP_UNIT);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.TEMP_MONITOR)
        {
            if (L.isSimulation())
            {
                showDialogFragment(FragmentTempMonitorConfiguration.newInstance(mNodeAddress, mRoomName, mNodeType, mFloorName), FragmentTempMonitorConfiguration.ID);
            }
            else
            {
                Log.d("FragBleInstrScrn","TEmp Monitor profile. device scan");
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.TEMP_MONITOR);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.TEMP_INFLUENCE)
        {
            //CCU As a Zone -- Temp influence profile - No pairing needed
           // if (L.isSimulation()) {
                showDialogFragment(FragmentTempInfConfiguration.newInstance(mNodeAddress, mRoomName, mNodeType, mFloorName), FragmentTempInfConfiguration.ID);
            /*}
            else
            {
                Log.d("FragBleInstrScrn","Temp influence profile. device scan");
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.TEMP_INFLUENCE);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }*/
        }
        else if (mProfileType == ProfileType.SMARTSTAT_TWO_PIPE_FCU)
        {
            if (L.isSimulation())
            {
                showDialogFragment(Fragment2PipeFanCoilUnitConfig.newInstance(mNodeAddress, mRoomName, mNodeType, mFloorName,mProfileType), Fragment2PipeFanCoilUnitConfig.ID);
            }
            else
            {
                Log.d("FragBleInstrScrn","CPU profile. device scan");
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.SMARTSTAT_TWO_PIPE_FCU);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.SMARTSTAT_FOUR_PIPE_FCU)
        {
            if (L.isSimulation())
            {
                showDialogFragment(Fragment4PipeFanCoilUnitConfig.newInstance(mNodeAddress, mRoomName, mNodeType, mFloorName,mProfileType), Fragment4PipeFanCoilUnitConfig.ID);
            }
            else
            {
                Log.d("FragBleInstrScrn","CPU profile. device scan");
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.SMARTSTAT_FOUR_PIPE_FCU);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.HYPERSTAT_VRV) {
            if (L.isSimulation()) {
                showDialogFragment(
                    HyperStatVrvFragment.Companion.newInstance(mNodeAddress, mRoomName, mFloorName),
                    HyperStatVrvFragment.ID);
            }
            else {
                Log.d("FragBleInstrScrn","Hyperstat VRV profile. device scan");
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.HYPERSTAT_VRV);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
        else if (mProfileType == ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT) {
            if (L.isSimulation()) {
                showDialogFragment(
                        HyperStatCpuFragment.Companion.newInstance(mNodeAddress, mRoomName, mFloorName,mNodeType,
                                ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT),
                        HyperStatCpuFragment.ID);
            }
            else {
                Log.d("FragBleInstrScrn","Hyperstat CPU profile. device scan");
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan.getInstance(mNodeAddress, mRoomName, mFloorName, mNodeType, ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.pairinginstructionscreen, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
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
            title.setText(getText(R.string.title_pairsn));
            if(CCUUiUtil.isDaikinEnvironment(getContext())) {
                pairinginstruct.setVisibility(View.GONE);
                pairinginstructDaikin.setVisibility(View.VISIBLE);
            } else {
                pairinginstruct.setImageResource(R.drawable.image_pairing_screen_snhn);
            }

        }
        else if (mNodeType == NodeType.SMART_STAT)
        {
            title.setText(getText(R.string.title_pairss));
            pairinginstruct.setImageResource(R.drawable.pairinginstruct);
        }
        else if (mNodeType == NodeType.HYPER_STAT) {
            title.setText(R.string.title_pairhss);
            pairinginstruct.setImageResource(R.drawable.sensepairscreen);
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
