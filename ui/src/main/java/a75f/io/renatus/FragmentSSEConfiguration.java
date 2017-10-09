package a75f.io.renatus;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;

import a75f.io.bo.building.BaseProfileConfiguration;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.LightProfileConfiguration;
import a75f.io.bo.building.NodeType;
import a75f.io.bo.building.Output;
import a75f.io.bo.building.SingleStageProfile;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.building.definitions.ScheduleMode;
import a75f.io.bo.building.definitions.SingleStageMode;
import a75f.io.bo.building.sse.SingleStageLogicalMap;
import a75f.io.logic.L;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;

import static a75f.io.logic.L.ccu;

/**
 * Created by ryant on 9/27/2017.
 */

public class FragmentSSEConfiguration extends BaseDialogFragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener
{
    
    public static final String ID = FragmentSSEConfiguration.class.getSimpleName();
    
    View view;
    boolean mbIsInEditMode = false;
    Spinner      spRelay1;
    Spinner      spRelay2;
    SwitchCompat relay1Switch;
    SwitchCompat relay2Switch;
    EditText     relay1EditText;
    EditText     relay2EditText;
    Spinner      spRelay1Action;
    Spinner      spRelay2Action;
    ImageView    editRelay1;
    ImageView    editRelay2;
    
    TextView     lcmSetCommand;
    TextView     lcmCancelCommand;
    ToggleButton lcmRelay1Override;
    ToggleButton lcmRelay2Override;
    
    ArrayList<String> zoneCircuitNames;
    Zone              mZone;
    
    private ArrayAdapter<CharSequence> relay1Adapter;
    private ArrayAdapter<CharSequence> relay2Adapter;
    private short                      mSmartNodeAddress;
    private NodeType                   mNodeType;
    
    public FragmentSSEConfiguration()
    {
    }
    
    public static FragmentSSEConfiguration newInstance(short smartNodeAddress, String roomName, NodeType nodeType, String floorName)
    {
        FragmentSSEConfiguration f = new FragmentSSEConfiguration();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, smartNodeAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString());
        f.setArguments(bundle);
        return f;
    }
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        view = inflater.inflate(R.layout.fragment_sse_control_details, container, false);
        mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        String mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        String mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mNodeType = NodeType.valueOf(getArguments().getString(FragmentCommonBundleArgs.NODE_TYPE));
        mZone = L.findZoneByName(mFloorName, mRoomName);
        lcmSetCommand = (TextView) view.findViewById(R.id.lcmSetCommand);
        lcmCancelCommand = (TextView) view.findViewById(R.id.lcmCancelCommand);
        if (!mbIsInEditMode)
        {
            lcmCancelCommand.setVisibility(View.INVISIBLE);
        }
        setUpTestTriggers();
        spRelay1 = (Spinner) view.findViewById(R.id.lcmRelay1Actuator);
        spRelay2 = (Spinner) view.findViewById(R.id.lcmRelay2Actuator);
        relay1Switch = (SwitchCompat) view.findViewById(R.id.lcmRelay1Switch);
        relay2Switch = (SwitchCompat) view.findViewById(R.id.lcmRelay2Switch);
        relay1EditText = (EditText) view.findViewById(R.id.lcmRelay1EditName);
        relay2EditText = (EditText) view.findViewById(R.id.lcmRelay2EditName);
        spRelay1Action = (Spinner) view.findViewById(R.id.lcmRelay1HeatCoolFanSpinner);
        spRelay2Action = (Spinner) view.findViewById(R.id.lcmRelay2HeatCoolFanSpinner);
        ArrayAdapter sseActionType = ArrayAdapter.createFromResource(getActivity(), R.array.sse_action_type, R.layout.spinner_dropdown_item);
        sseActionType.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spRelay1Action.setAdapter(sseActionType);
        spRelay2Action.setAdapter(sseActionType);
        relay1Adapter = ArrayAdapter.createFromResource(getActivity(), R.array.lcm_relay, R.layout.spinner_dropdown_item);
        relay1Adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spRelay1.setAdapter(relay1Adapter);
        relay2Adapter = ArrayAdapter.createFromResource(getActivity(), R.array.lcm_relay, R.layout.spinner_dropdown_item);
        relay2Adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spRelay2.setAdapter(relay2Adapter);
        editRelay1 = (ImageView) view.findViewById(R.id.lcmRelay1EditImg);
        editRelay2 = (ImageView) view.findViewById(R.id.lcmRelay2EditImg);
        relay1Switch.setOnCheckedChangeListener(this);
        relay2Switch.setOnCheckedChangeListener(this);
        editRelay1.setOnClickListener(this);
        editRelay2.setOnClickListener(this);
        zoneCircuitNames = new ArrayList<>();
        TextView setBtn = (TextView) view.findViewById(R.id.lcmSetCommand);
        setBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                BaseProfileConfiguration baseProfileConfiguration = new BaseProfileConfiguration();
                SingleStageProfile mSingleStageProfile = (SingleStageProfile) mZone.findProfile(ProfileType.SSE);
                bindData(baseProfileConfiguration, mSingleStageProfile, false);
                SingleStageLogicalMap singleStageLogicalMap = null;
                if(mSingleStageProfile.getLogicalMap().containsKey(mSmartNodeAddress))
                {
                    singleStageLogicalMap = mSingleStageProfile.getLogicalMap().get(mSmartNodeAddress);
                }
                else
                {
                    singleStageLogicalMap = new SingleStageLogicalMap();
                    mSingleStageProfile.getLogicalMap().put(mSmartNodeAddress, singleStageLogicalMap);
                }
                
                singleStageLogicalMap.getLogicalMap().put(Port.RELAY_ONE, getSingleStageMode(spRelay1Action));
                singleStageLogicalMap.getLogicalMap().put(Port.RELAY_TWO, getSingleStageMode(spRelay2Action));
                
                mSingleStageProfile.setScheduleMode(ScheduleMode.SystemSchedule);
                mSingleStageProfile.setSchedules(ccu().getDefaultTemperatureSchedule());
                
                L.saveCCUState();
                getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                FragmentSSEConfiguration.this.closeAllBaseDialogFragments();
            }
        });
        TextView cancelBtn = (TextView) view.findViewById(R.id.lcmCancelCommand);
        cancelBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dismiss();
            }
        });
        return view;
    }
    
    private SingleStageMode getSingleStageMode(Spinner modeSpinner)
    {
        if (modeSpinner.getSelectedItemPosition() == 0)
        {
            return SingleStageMode.COOLING;
        }
        else if (modeSpinner.getSelectedItemPosition() == 1)
        {
            return SingleStageMode.HEATING;
        }
        else if (modeSpinner.getSelectedItemPosition() == 2)
        {
            return SingleStageMode.FAN;
        }
        else
        {
            return SingleStageMode.NOT_INSTALLED;
        }
    }
    
    private void setUpTestTriggers()
    {
        final Zone testZone = new Zone();
        final LightProfile testLightProfile = (LightProfile) testZone.findProfile(ProfileType.LIGHT);
        testLightProfile.setCircuitTest(true);
        lcmRelay1Override = (ToggleButton) view.findViewById(R.id.testr1);
        lcmRelay2Override = (ToggleButton) view.findViewById(R.id.testr2);
        //TODO why is analog min a tuner???
        CompoundButton.OnCheckedChangeListener onCheckChangedListener = new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                BaseProfileConfiguration baseProfileConfiguration = new BaseProfileConfiguration();
                bindData(baseProfileConfiguration , testLightProfile, true);
                L.sendTestMessage(mSmartNodeAddress, testZone);
            }
        };
        lcmRelay1Override.setOnCheckedChangeListener(onCheckChangedListener);
        lcmRelay2Override.setOnCheckedChangeListener(onCheckChangedListener);
    }
    
    public void bindData(BaseProfileConfiguration zoneProfileConfiguration, ZoneProfile zoneProfile, boolean isTest)
    {
        zoneProfileConfiguration.setNodeType(mNodeType);
        Output relayOne;
        Output relayTwo;
        if (relay1Switch.isChecked())
        {
            relayOne = new Output();
            relayOne.setAddress(mSmartNodeAddress);
            relayOne.setPort(Port.RELAY_ONE);
            if (spRelay1.getSelectedItemPosition() == 0)
            {
                relayOne.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            }
            else
            {
                relayOne.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
            }
            relayOne.setName(relay1EditText.getText().toString());
            if (isTest)
            {
                relayOne.setTestVal(lcmRelay1Override.isChecked() ? (short) 1 : 0);
            }
            zoneProfileConfiguration.getOutputs().add(relayOne);
        }
        if (relay2Switch.isChecked())
        {
            relayTwo = new Output();
            relayTwo.setAddress(mSmartNodeAddress);
            relayTwo.setPort(Port.RELAY_TWO);
            if (spRelay2.getSelectedItemPosition() == 0)
            {
                relayTwo.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
            }
            else
            {
                relayTwo.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
            }
            relayTwo.setName(relay2EditText.getText().toString());
            relayTwo.setPort(Port.RELAY_TWO);
            zoneProfileConfiguration.getOutputs().add(relayTwo);
            if (isTest)
            {
                relayTwo.setTestVal(lcmRelay2Override.isChecked() ? (short) 1 : 0);
            }
        }
        zoneProfile.getProfileConfiguration().put(mSmartNodeAddress, zoneProfileConfiguration);
    }
    
    @Override
    public String getIdString()
    {
        return ID;
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
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        switch (buttonView.getId())
        {
            case R.id.lcmRelay1Switch:
                break;
            case R.id.lcmRelay2Switch:
                break;
            case R.id.lcmAnalog1OutSwitch:
                break;
            case R.id.lcmAnalog2OutSwitch:
                break;
            case R.id.lcmAnalog1InSwitch:
                break;
            case R.id.lcmAnalog2InSwitch:
                break;
        }
    }
    
    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.lcmRelay1EditImg:
                if (relay1Switch.isChecked())
                {
                    showEditLogicalNameDialog(relay1EditText, v.getId());
                }
                break;
            case R.id.lcmRelay2EditImg:
                if (relay1Switch.isChecked())
                {
                    showEditLogicalNameDialog(relay2EditText, v.getId());
                }
                break;
        }
    }
    
    public void showEditLogicalNameDialog(final EditText etext, final int id)
    {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle);
        alertBuilder.setTitle("Assign Name for this Lighting Circuit");
        alertBuilder.setCancelable(false);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View view = inflater.inflate(R.layout.edit_circuit_name_dialog, null);
        final EditText input = (EditText) view.findViewById(R.id.editTextCircuitName);
        final TextView remainingChar = (TextView) view.findViewById(R.id.remainingChar);
        input.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
            }
            
            @Override
            public void afterTextChanged(Editable s)
            {
                if (input.getText().toString().length() > 20)
                {
                    int i = 30 - input.getText().toString().length();
                    remainingChar.setVisibility(View.VISIBLE);
                    remainingChar.setText(i + " ");
                }
                else
                {
                    remainingChar.setVisibility(View.GONE);
                }
            }
        });
        input.setText(etext.getText().toString());
        alertBuilder.setView(view);
        alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String displayName = input.getText().toString();
                if (displayName.isEmpty())
                {
                    Toast.makeText(getActivity(), "Circuit Name cannot be Empty", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if (!zoneCircuitNames.contains(displayName))
                    {
                        zoneCircuitNames.add(displayName);
                    }
                    else
                    {
                        Toast.makeText(getActivity(), "Circuit Name [" + displayName + "] exists, enter a valid circuit name", Toast.LENGTH_LONG).show();
                        displayName = "";
                    }
                }
                etext.setText(displayName.length() > 15 ? displayName.substring(0, 12).concat("...") : displayName.toString());
                dialog.dismiss();
            }
        });
        alertBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });
        alertBuilder.show();
    }
}