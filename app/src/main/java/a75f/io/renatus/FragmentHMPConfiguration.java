package a75f.io.renatus;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.HmpProfile;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.util.CCUUiUtil;
import butterknife.BindView;
import butterknife.ButterKnife;



/**
 * Created by samjithsadasivan on 5/2/18.
 */

public class FragmentHMPConfiguration extends BaseDialogFragment
{
    public static final String TAG = "HMPConfig";
    public static final String ID = FragmentSSEConfiguration.class.getSimpleName();
    
    @BindView(R.id.hmpSetpoint)
    Spinner setpointSpinner;
    
    @BindView(R.id.actuator)
    Spinner actuatorSpinner;
    
    @BindView(R.id.pGainSpinner)
    Spinner pGainSpinner;
    
    @BindView(R.id.iGainSpinner)
    Spinner iGainSpinner;
    
    @BindView(R.id.pSpreadSpinner)
    Spinner propSpreadSpinner;
    
    @BindView(R.id.iTimeoutSpinner)
    Spinner integralTimeOutSpinner;
    
    @BindView(R.id.minValveSpinner)
    Spinner minValveSpinner;
    
    @BindView(R.id.maxValveSpinner)
    Spinner maxValveSpinner;
    
    @BindView(R.id.setBtn)
    Button setButton;
    
    int selection;
    
    
    
    private HmpProfile mHmpProfile;
    
    private Zone mZone;
    private short                      mSmartNodeAddress;
    private NodeType                   mNodeType;
    
    @Override
    public String getIdString()
    {
        return ID;
    }
    
    public FragmentHMPConfiguration()
    {
    }
    
    public static FragmentHMPConfiguration newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName)
    {
        FragmentHMPConfiguration f = new FragmentHMPConfiguration();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, smartNodeAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString());
        f.setArguments(bundle);
        return f;
    }
    
    @Override
    public void onStart()
    {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null)
        {
            int width = ViewGroup.LayoutParams.WRAP_CONTENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
        setTitle();
    }
    
    private void setTitle() {
        Dialog dialog = getDialog();
        
        if (dialog == null) {
            return;
        }
        dialog.setTitle("Hot Water Mixing Package");
        TextView titleView = this.getDialog().findViewById(android.R.id.title);
        if(titleView != null)
        {
            titleView.setGravity(Gravity.CENTER);
            titleView.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
        }
        int titleDividerId = getContext().getResources()
                                         .getIdentifier("titleDivider", "id", "android");
        
        View titleDivider = dialog.findViewById(titleDividerId);
        if (titleDivider != null) {
            titleDivider.setBackgroundColor(getContext().getResources()
                                                        .getColor(R.color.transparent));
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_hmp_config, container, false);
        mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        String mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        String mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mNodeType = NodeType.valueOf(getArguments().getString(FragmentCommonBundleArgs.NODE_TYPE));
        mZone = L.findZoneByName(mFloorName, mRoomName);
        ButterKnife.bind(this, view);
        return view;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        
        mHmpProfile = (HmpProfile) mZone.findProfile(ProfileType.HMP);
        
        if (mHmpProfile == null) {
            mHmpProfile = new HmpProfile();
        }
        
        ArrayList<Integer> arrayHmpSetPoint = new ArrayList<Integer>();
        for (int pos = 50; pos <= 150; pos++) {
            arrayHmpSetPoint.add(pos);
        }
        ArrayAdapter<Integer> hmpSetpointAdapter = new ArrayAdapter<Integer>(getActivity(), android.R.layout.simple_spinner_item, arrayHmpSetPoint);
        hmpSetpointAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setpointSpinner.setAdapter(hmpSetpointAdapter);
        setpointSpinner.setSelection(hmpSetpointAdapter.getPosition((int)mHmpProfile.getSetTemperature()));
       
        
        ArrayList<String> analogTypes = new ArrayList<>();
        for (OutputAnalogActuatorType actuator : OutputAnalogActuatorType.values()) {
            analogTypes.add(actuator.displayName);
        }
        ArrayAdapter<String> analogoutActuatorType = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, analogTypes);
        analogoutActuatorType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actuatorSpinner.setAdapter(analogoutActuatorType);
        actuatorSpinner.setSelection(selection);
        actuatorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selection = position;
            }
        
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
        
        setButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                setupHmpProfile();
    
                L.saveCCUState();
                getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                FragmentHMPConfiguration.this.closeAllBaseDialogFragments();
            }
        });
    
        //Range of proportional/integral gain is 0.1 - 1.0
        ArrayList<Double> piGain = new ArrayList<Double>();
        for (int pos = 1; pos <= 10; pos++) {
            piGain.add((double)pos/10);
        }
        ArrayAdapter<Double> piGainAdapter = new ArrayAdapter<Double>(getActivity(), android.R.layout.simple_spinner_item, piGain);
        piGainAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pGainSpinner.setAdapter(piGainAdapter);
        pGainSpinner.setSelection(piGainAdapter.getPosition(mHmpProfile.getProportionalGain()));
        iGainSpinner.setAdapter(piGainAdapter);
        iGainSpinner.setSelection(piGainAdapter.getPosition(mHmpProfile.getIntegralGain()));
    
    
        //Range of proportional spread is 1 - 100
        ArrayList<Integer> pSpread = new ArrayList<Integer>();
        for (int pos = 1; pos <= 100; pos++) {
            pSpread.add(pos);
        }
        ArrayAdapter<Integer> pSpreadAdapter = new ArrayAdapter<Integer>(getActivity(), android.R.layout.simple_spinner_item, pSpread);
        pSpreadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        propSpreadSpinner.setAdapter(pSpreadAdapter);
        propSpreadSpinner.setSelection(pSpreadAdapter.getPosition(mHmpProfile.getProportionalSpread()));
    
        //Timeout period 5 min - 60 min
        ArrayList<Integer> integralTimeout = new ArrayList<Integer>();
        for (int pos = 5; pos <= 60; pos += 5) {
            integralTimeout.add(pos);
        }
        ArrayAdapter<Integer> timeAdapter = new ArrayAdapter<Integer>(getActivity(), android.R.layout.simple_spinner_item, integralTimeout);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        integralTimeOutSpinner.setAdapter(timeAdapter);
        integralTimeOutSpinner.setSelection(timeAdapter.getPosition(mHmpProfile.getIntegralMaxTimeout()));
    
        //Valve position 0-100
        ArrayList<Integer> valvePosition = new ArrayList<Integer>();
        for (int pos = 0; pos <= 100; pos += 5) {
            valvePosition.add(pos);
        }
    
        ArrayAdapter<Integer> valvePosAdapter = new ArrayAdapter<Integer>(getActivity(), android.R.layout.simple_spinner_item, valvePosition);
        valvePosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        minValveSpinner.setAdapter(valvePosAdapter);
        minValveSpinner.setSelection(valvePosAdapter.getPosition(mHmpProfile.getMinValvePosition()));
        maxValveSpinner.setAdapter(valvePosAdapter);
        maxValveSpinner.setSelection(valvePosAdapter.getPosition(mHmpProfile.getMaxValvePosition()));
    
    
    }
    
    public void setupHmpProfile() {
    
        Output analogOne = new Output();
        analogOne.setAddress(mSmartNodeAddress);
        analogOne.setPort(Port.ANALOG_OUT_ONE);
        analogOne.mOutputAnalogActuatorType = OutputAnalogActuatorType.values()[selection];
        
        BaseProfileConfiguration hmpProfileConfiguration = new BaseProfileConfiguration();
        hmpProfileConfiguration.setNodeType(mNodeType);
        hmpProfileConfiguration.setNodeAddress(mSmartNodeAddress);
        hmpProfileConfiguration.getOutputs().add(analogOne);
        mHmpProfile.getProfileConfiguration().put(mSmartNodeAddress,hmpProfileConfiguration);
    
        mHmpProfile.setSetTemperature(Integer.parseInt(setpointSpinner.getSelectedItem().toString()));
        mHmpProfile.setProportionalGain(Double.parseDouble(pGainSpinner.getSelectedItem().toString()));
        mHmpProfile.setIntegralGain(Double.parseDouble(iGainSpinner.getSelectedItem().toString()));
        mHmpProfile.setProportionalSpread(Integer.parseInt(propSpreadSpinner.getSelectedItem().toString()));
        mHmpProfile.setIntegralMaxTimeout(Integer.parseInt(integralTimeOutSpinner.getSelectedItem().toString()));
        mHmpProfile.setMinValvePosition(Integer.parseInt(minValveSpinner.getSelectedItem().toString()));
        mHmpProfile.setMaxValvePosition(Integer.parseInt(maxValveSpinner.getSelectedItem().toString()));
    
        if (mZone.findProfile(ProfileType.HMP) == null)
            mZone.mZoneProfiles.add(mHmpProfile);
        
        CcuLog.d(L.TAG_CCU_UI, "Set Temp : " + Integer.parseInt(setpointSpinner.getSelectedItem().toString()) +
                ", Actuator : " + OutputAnalogActuatorType.values()[selection]);
        
    }
}
