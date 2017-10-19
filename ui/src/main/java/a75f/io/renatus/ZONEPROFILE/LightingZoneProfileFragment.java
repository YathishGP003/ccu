package a75f.io.renatus.ZONEPROFILE;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import a75f.io.bo.building.TestProfile;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.building.definitions.ScheduleMode;
import a75f.io.logic.L;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.FloorPlanFragment;
import a75f.io.renatus.R;

import static a75f.io.logic.L.ccu;

/**
 * Created by anilkumar isOn 27-10-2016.
 */

public class LightingZoneProfileFragment extends BaseDialogFragment
        implements CompoundButton.OnCheckedChangeListener, View.OnClickListener
{
    
    public static final  String ID  = LightingZoneProfileFragment.class.getSimpleName();
    private static final String TAG = "Lighting";
    
    ArrayList<Short> arrayAnalogMinMax;
    
    View view;
    boolean mbIsInEditMode = false;
    Spinner      spRelay1;
    Spinner      spRelay2;
    SwitchCompat relay1Switch;
    SwitchCompat relay2Switch;
    EditText     relay1EditText;
    EditText     relay2EditText;
    ImageView    editRelay1;
    ImageView    editRelay2;
    Spinner      spAnalog1Out;
    Spinner      spAnalog2Out;
    SwitchCompat analog1OutSwitch;
    SwitchCompat analog2OutSwitch;
    EditText     analog1OutEditText;
    EditText     analog2OutEditText;
    ImageView    editAnalog1Out;
    ImageView    editAnalog2Out;
    TextView     remainingChar;
    TextView     lcmSetCommand;
    TextView     lcmCancelCommand;
    Spinner      spAnalog1In;
    Spinner      spAnalog2In;
    SwitchCompat analog1InSwitch;
    SwitchCompat analog2InSwitch;
    EditText     analog1InEditText;
    EditText     analog2InEditText;
    ImageView    editAnalog1In;
    ImageView    editAnalog2In;
    
    ToggleButton lcmRelay1Override;
    ToggleButton lcmRelay2Override;
    Spinner      lcmAnalog1OutOverride;
    Spinner      lcmAnalog2OutOverride;
    
    ArrayList<String> zoneCircuitNames;
    Zone              mZone;
    
    Output smartNodeAnalogOutputOne;
    Output mSmartNodeRelayOne;
    
    private ArrayAdapter<CharSequence> relay1Adapter;
    private ArrayAdapter<CharSequence> relay2Adapter;
    private ArrayAdapter<CharSequence> analog1OutAdapter;
    private ArrayAdapter<CharSequence> analog2OutAdapter;
    private ArrayAdapter<CharSequence> analog1InAdapter;
    private ArrayAdapter<CharSequence> analog2InAdapter;
    private short                      mSmartNodeAddress;
    private NodeType                   mNodeType;
    
    
    public LightingZoneProfileFragment()
    {
    }
    
    
    public static LightingZoneProfileFragment newInstance(short smartNodeAddress, String roomName,
                                                          NodeType nodeType, String floorName)
    {
        LightingZoneProfileFragment f = new LightingZoneProfileFragment();
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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        view = inflater.inflate(R.layout.fragment_lighting_control_details, container, false);
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
        relay1Adapter = ArrayAdapter
                                .createFromResource(getActivity(), R.array.lcm_relay, R.layout.spinner_dropdown_item);
        relay1Adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spRelay1.setAdapter(relay1Adapter);
        relay2Adapter = ArrayAdapter
                                .createFromResource(getActivity(), R.array.lcm_relay, R.layout.spinner_dropdown_item);
        relay2Adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spRelay2.setAdapter(relay2Adapter);
        spAnalog1Out = (Spinner) view.findViewById(R.id.lcmAnalog1OutActuator);
        spAnalog2Out = (Spinner) view.findViewById(R.id.lcmAnalog2OutActuator);
        analog1OutSwitch = (SwitchCompat) view.findViewById(R.id.lcmAnalog1OutSwitch);
        analog2OutSwitch = (SwitchCompat) view.findViewById(R.id.lcmAnalog2OutSwitch);
        analog1OutEditText = (EditText) view.findViewById(R.id.lcmAnalog1OutEditName);
        analog2OutEditText = (EditText) view.findViewById(R.id.lcmAnalog2OutEditName);
        analog1OutAdapter = ArrayAdapter
                                    .createFromResource(getActivity(), R.array.lcm_analog_out, R.layout.spinner_dropdown_item);
        analog1OutAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spAnalog1Out.setAdapter(analog1OutAdapter);
        analog2OutAdapter = ArrayAdapter
                                    .createFromResource(getActivity(), R.array.lcm_analog_out, R.layout.spinner_dropdown_item);
        analog2OutAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spAnalog2Out.setAdapter(analog2OutAdapter);
        spAnalog1In = (Spinner) view.findViewById(R.id.lcmAnalog1InActuator);
        analog1InSwitch = (SwitchCompat) view.findViewById(R.id.lcmAnalog1InSwitch);
        analog1InEditText = (EditText) view.findViewById(R.id.lcmAnalog1InEditName);
        analog1InAdapter = ArrayAdapter
                                   .createFromResource(getActivity(), R.array.lcm_analog_in, R.layout.spinner_dropdown_item);
        analog1InAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spAnalog1In.setAdapter(analog1InAdapter);
        analog2InEditText = (EditText) view.findViewById(R.id.lcmAnalog2InEditName);
        analog2InSwitch = (SwitchCompat) view.findViewById(R.id.lcmAnalog2InSwitch);
        spAnalog2In = (Spinner) view.findViewById(R.id.lcmAnalog2InActuator);
        analog2InAdapter = ArrayAdapter
                                   .createFromResource(getActivity(), R.array.lcm_analog_in, R.layout.spinner_dropdown_item);
        analog2InAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spAnalog2In.setAdapter(analog2InAdapter);
        editRelay1 = (ImageView) view.findViewById(R.id.lcmRelay1EditImg);
        editRelay2 = (ImageView) view.findViewById(R.id.lcmRelay2EditImg);
        editAnalog1Out = (ImageView) view.findViewById(R.id.lcmAnalog1OutEditImg);
        editAnalog2Out = (ImageView) view.findViewById(R.id.lcmAnalog2OutEditImg);
        editAnalog1In = (ImageView) view.findViewById(R.id.lcmAnalog1InEditImg);
        editAnalog2In = (ImageView) view.findViewById(R.id.lcmAnalog2InEditImg);
        relay1Switch.setOnCheckedChangeListener(this);
        relay2Switch.setOnCheckedChangeListener(this);
        analog1OutSwitch.setOnCheckedChangeListener(this);
        analog2OutSwitch.setOnCheckedChangeListener(this);
        analog1InSwitch.setOnCheckedChangeListener(this);
        analog2InSwitch.setOnCheckedChangeListener(this);
        editRelay1.setOnClickListener(this);
        editRelay2.setOnClickListener(this);
        editAnalog1Out.setOnClickListener(this);
        editAnalog2Out.setOnClickListener(this);
        editAnalog1In.setOnClickListener(this);
        editAnalog2In.setOnClickListener(this);
        zoneCircuitNames = new ArrayList<>();
        TextView setBtn = (TextView) view.findViewById(R.id.lcmSetCommand);
        setBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                LightProfileConfiguration lightProfileConfiguration =
                        new LightProfileConfiguration();
                LightProfile mLightProfile =  new LightProfile();
                mZone.mZoneProfiles.add(mLightProfile);
                bindData(lightProfileConfiguration, mLightProfile, false);
                if (!mLightProfile.hasSchedules())
                {
                    mLightProfile
                            .addSchedules(ccu().getDefaultLightSchedule(), ScheduleMode.ZoneSchedule);
                }
                L.saveCCUState();
                getActivity()
                        .sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
                LightingZoneProfileFragment.this.closeAllBaseDialogFragments();
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
    
    
    private void setUpTestTriggers()
    {
        final Zone testZone = new Zone();
        final TestProfile testLightProfile = new TestProfile();
        testZone.mZoneProfiles.add(testLightProfile);
        testLightProfile.setCircuitTest(true);

        //TODO: if they close dialog how do we remove seed from CM?
        lcmRelay1Override = (ToggleButton) view.findViewById(R.id.testr1);
        lcmRelay2Override = (ToggleButton) view.findViewById(R.id.testr2);
        lcmAnalog1OutOverride = (Spinner) view.findViewById(R.id.testAna1out);
        lcmAnalog2OutOverride = (Spinner) view.findViewById(R.id.testAna2out);
        arrayAnalogMinMax = new ArrayList<>();
        for (int pos = 0; pos <= 100; pos++)
        {
            arrayAnalogMinMax.add((short) pos);
        }
        final ArrayAdapter<Short> analogAdapter =
                new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, arrayAnalogMinMax);
        analogAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        lcmAnalog1OutOverride.setAdapter(analogAdapter);
        lcmAnalog2OutOverride.setAdapter(analogAdapter);
        //TODO why is analog min a tuner???
        //lcmAnalog1OutOverride.setSelection();
        AdapterView.OnItemSelectedListener onItemSelectedListener =
                new AdapterView.OnItemSelectedListener()
                {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position,
                                               long id)
                    {
                        final LightProfileConfiguration testLightProfileConfiguration =
                                new LightProfileConfiguration();
                        bindData(testLightProfileConfiguration, testLightProfile, true);
                        L.sendTestMessage(mSmartNodeAddress, testZone);
                    }
                    
                    
                    @Override
                    public void onNothingSelected(AdapterView<?> parent)
                    {
                    }
                };
        CompoundButton.OnCheckedChangeListener onCheckChangedListener =
                new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                    {
                        final LightProfileConfiguration testLightProfileConfiguration =
                                new LightProfileConfiguration();
                        bindData(testLightProfileConfiguration, testLightProfile, true);
                        L.sendTestMessage(mSmartNodeAddress, testZone);
                    }
                };
        lcmRelay1Override.setOnCheckedChangeListener(onCheckChangedListener);
        lcmRelay2Override.setOnCheckedChangeListener(onCheckChangedListener);
        lcmAnalog1OutOverride.setOnItemSelectedListener(onItemSelectedListener);
        lcmAnalog2OutOverride.setOnItemSelectedListener(onItemSelectedListener);
    }
    
    
    public void bindData(BaseProfileConfiguration zoneProfileConfiguration, ZoneProfile zoneProfile,
                         boolean isTest)
    {
        Log.e("ERROR", "mZone: " + mZone.getNodes().size());
        zoneProfileConfiguration.setNodeType(mNodeType);
        Output relayOne;
        Output relayTwo;
        Output analogOne;
        Output analogTwo;
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
        if (analog1OutSwitch.isChecked())
        {
            analogOne = new Output();
            analogOne.setAddress(mSmartNodeAddress);
            analogOne.setPort(Port.ANALOG_OUT_ONE);
            if (spAnalog1Out.getSelectedItemPosition() == 0)
            {
                analogOne.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
            }
            else
            {
                analogOne.mOutputAnalogActuatorType = OutputAnalogActuatorType.TwoToTenV;
            }
            analogOne.setName(analog1OutEditText.getText().toString());
            if (isTest)
            {
                Log.i(TAG, "LOg: " + lcmAnalog1OutOverride.getSelectedItemPosition());
                analogOne.setTestVal(arrayAnalogMinMax
                                             .get(lcmAnalog1OutOverride.getSelectedItemPosition()));
            }
            zoneProfileConfiguration.getOutputs().add(analogOne);
        }
        if (analog2OutSwitch.isChecked())
        {
            analogTwo = new Output();
            analogTwo.setAddress(mSmartNodeAddress);
            analogTwo.setPort(Port.ANALOG_OUT_TWO);
            if (spAnalog2Out.getSelectedItemPosition() == 0)
            {
                analogTwo.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
            }
            else
            {
                analogTwo.mOutputAnalogActuatorType = OutputAnalogActuatorType.TwoToTenV;
            }
            analogTwo.setName(analog2OutEditText.getText().toString());
            if (isTest)
            {
                analogTwo.setTestVal(arrayAnalogMinMax
                                             .get(lcmAnalog2OutOverride.getSelectedItemPosition()));
            }
            zoneProfileConfiguration.getOutputs().add(analogTwo);
        }
        zoneProfile.getProfileConfiguration().put(mSmartNodeAddress, zoneProfileConfiguration);
    }
    //
    //    public Output getSmartNodeRelayOne(boolean isChecked)
    //    {
    //        if (mSmartNodeRelayOne == null)
    //        {
    //            mSmartNodeRelayOne = new Output();
    //            mSmartNodeRelayOne.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
    //            mSmartNodeRelayOne.setPort(Port.RELAY_ONE);
    //            mSmartNodeRelayOne.setName("Relay 1");
    //            mSmartNodeRelayOne.setAddress(mSmartNodeAddress);
    //            //TODO: add test circuit ability
    //            //mZone.getOutputs().put(mSmartNodeRelayOne.getUuid(), mSmartNodeRelayOne);
    //        }
    //        if (spRelay1.getSelectedItemPosition() == 0)
    //        {
    //            mSmartNodeRelayOne.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
    //        }
    //        else
    //        {
    //            mSmartNodeRelayOne.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
    //        }
    //        mSmartNodeRelayOne.setName(
    //                relay1EditText.getText() != null && relay1EditText.getText().length() > 0
    //                        ? relay1EditText.getText().toString() : "empty");
    //        return mSmartNodeRelayOne;
    //    }
    //
    //    public Output getSmartNodeAnalogOutputOne(boolean isChecked)
    //    {
    //        if (smartNodeAnalogOutputOne == null)
    //        {
    //            smartNodeAnalogOutputOne = new Output();
    //            smartNodeAnalogOutputOne.mOutputAnalogActuatorType =
    //                    OutputAnalogActuatorType.ZeroToTenV;
    //            smartNodeAnalogOutputOne.setPort(Port.ANALOG_OUT_ONE);
    //            smartNodeAnalogOutputOne.setName("Analog 1");
    //            smartNodeAnalogOutputOne.setAddress(mSmartNodeAddress);
    //            //TODO: add test circuit ability back
    //            //mZone.getOutputs().put(smartNodeAnalogOutputOne.getUuid(), smartNodeAnalogOutputOne);
    //        }
    //        if (spAnalog1Out.getSelectedItemPosition() == 0)
    //        {
    //            smartNodeAnalogOutputOne.mOutputAnalogActuatorType =
    //                    OutputAnalogActuatorType.ZeroToTenV;
    //        }
    //        else
    //        {
    //            smartNodeAnalogOutputOne.mOutputAnalogActuatorType = OutputAnalogActuatorType.TwoToTenV;
    //        }
    //        smartNodeAnalogOutputOne.setName(
    //                analog1OutEditText.getText() != null && analog1OutEditText.getText().length() > 0
    //                        ? analog1OutEditText.getText().toString() : "empty");
    //        return smartNodeAnalogOutputOne;
    //    }
    //    private void bindData()
    //    {
    //        relayOne = mZone.findPort(Port.RELAY_ONE, mSmartNodeAddress);
    //        relayTwo = mZone.findPort(Port.RELAY_TWO, mSmartNodeAddress);
    //        analogOne = mZone.findPort(Port.ANALOG_OUT_ONE, mSmartNodeAddress);
    //        analogTwo = mZone.findPort(Port.ANALOG_OUT_TWO, mSmartNodeAddress);
    //        //RelayOne
    //        spRelay1.setSelection(
    //                relayOne.mOutputRelayActuatorType == OutputRelayActuatorType.NormallyOpen ? 0 : 1);
    //        relay1EditText.setText(relayOne.getCircuitName());
    //        relay1Switch.setChecked(relayOne.mConfigured);
    //        //RelayTwo
    //        spRelay2.setSelection(
    //                relayOne.mOutputRelayActuatorType == OutputRelayActuatorType.NormallyOpen ? 0 : 1);
    //        relay2EditText.setText(relayTwo.getCircuitName());
    //        relay2Switch.setChecked(relayTwo.mConfigured);
    //        //AnalogOne
    //        spAnalog1Out.setSelection(
    //                analogOne.mOutputAnalogActuatorType == OutputAnalogActuatorType.ZeroToTenV ? 0 : 1);
    //        analog1OutEditText.setText(analogOne.getCircuitName());
    //        analog1OutSwitch.setChecked(analogOne.mConfigured);
    //        //AnalogTwo
    //        spAnalog2Out.setSelection(
    //                analogOne.mOutputAnalogActuatorType == OutputAnalogActuatorType.ZeroToTenV ? 0 : 1);
    //        analog2OutEditText.setText(analogTwo.getCircuitName());
    //        analog2OutSwitch.setChecked(analogTwo.mConfigured);
    //    }
    
    
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
            case R.id.lcmAnalog1OutEditImg:
                if (analog1OutSwitch.isChecked())
                {
                    showEditLogicalNameDialog(analog1OutEditText, v.getId());
                }
                break;
            case R.id.lcmAnalog2OutEditImg:
                if (analog2OutSwitch.isChecked())
                {
                    showEditLogicalNameDialog(analog2OutEditText, v.getId());
                }
                break;
            case R.id.lcmAnalog1InEditImg:
                if (analog1InSwitch.isChecked())
                {
                    showEditLogicalNameDialog(analog1InEditText, v.getId());
                }
                break;
            case R.id.lcmAnalog2InEditImg:
                if (analog2InSwitch.isChecked())
                {
                    showEditLogicalNameDialog(analog2InEditText, v.getId());
                }
                break;
        }
    }
    
    
    public void showEditLogicalNameDialog(final EditText etext, final int id)
    {
        AlertDialog.Builder alertBuilder =
                new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle);
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
                    Toast.makeText(getActivity(), "Circuit Name cannot be Empty", Toast.LENGTH_SHORT)
                         .show();
                }
                else
                {
                    if (!zoneCircuitNames.contains(displayName))
                    {
                        zoneCircuitNames.add(displayName);
                    }
                    else
                    {
                        Toast.makeText(getActivity(), "Circuit Name [" + displayName +
                                                      "] exists, enter a valid circuit name", Toast.LENGTH_LONG)
                             .show();
                        displayName = "";
                    }
                }
                etext.setText(displayName.length() > 15 ? displayName.substring(0, 12).concat("...")
                                      : displayName.toString());
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
