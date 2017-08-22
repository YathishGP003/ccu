package a75f.io.renatus.ZONEPROFILE;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.UUID;

import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.SmartNode;
import a75f.io.bo.building.SmartNodeOutput;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.definitions.Output;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.Port;
import a75f.io.logic.RoomBLL;
import a75f.io.logic.SmartNodeBLL;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.R;

/**
 * Created by anilkumar on 27-10-2016.
 */

public class LightingZoneProfileFragment extends BaseDialogFragment
		implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener,
				           View.OnClickListener
{
	
	public static final  String ID  = LightingZoneProfileFragment.class.getSimpleName();
	private static final String TAG = LightingZoneProfileFragment.class.getSimpleName();
	
	View        view;
	AlertDialog mAlertDialog;
	//FSVData mFSVData = null;
	boolean mbIsInEditMode = false;
	//LightingControlsData mLCMControls = null;
	Spinner   spRelay1;
	Spinner   spRelay2;
	Switch    relay1Switch;
	Switch    relay2Switch;
	EditText  relay1EditText;
	EditText  relay2EditText;
	ImageView editRelay1;
	ImageView editRelay2;
	Spinner   spAnalog1Out;
	Spinner   spAnalog2Out;
	Switch    analog1OutSwitch;
	Switch    analog2OutSwitch;
	EditText  analog1OutEditText;
	EditText  analog2OutEditText;
	ImageView editAnalog1Out;
	ImageView editAnalog2Out;
	TextView  remainingChar;
	TextView  lcmSetCommand;
	TextView  lcmCancelCommand;
	Spinner   spAnalog1In;
	Spinner   spAnalog2In;
	Switch    analog1InSwitch;
	Switch    analog2InSwitch;
	EditText  analog1InEditText;
	EditText  analog2InEditText;
	ImageView editAnalog1In;
	ImageView editAnalog2In;
	
	ToggleButton lcmRelay1Override;
	ToggleButton lcmRelay2Override;
	ToggleButton lcmAnalog1OutOverride;
	ToggleButton lcmAnalog2OutOverride;
	
	ArrayList<EditText> circuitsList   = new ArrayList<EditText>();
	ArrayList<Switch>   circuitEnabled = new ArrayList<Switch>();
	ArrayList<String> zoneCircuitNames;
	Zone         mZone;
	LightProfile mLightProfile;
	SmartNode    mSmartNode;
	SmartNodeOutput smartNodeAnalogOutputOne;
	private ArrayAdapter<CharSequence> relay1Adapter;
	private ArrayAdapter<CharSequence> relay2Adapter;
	private ArrayAdapter<CharSequence> analog1OutAdapter;
	private ArrayAdapter<CharSequence> analog2OutAdapter;
	private ArrayAdapter<CharSequence> analog1InAdapter;
	private ArrayAdapter<CharSequence> analog2InAdapter;
	private short                      mSmartNodeAddress;
	private String                     mRoomName;
	private String                     mFloorName;
	public LightingZoneProfileFragment()
	{
	}
	
	
	public static LightingZoneProfileFragment newInstance(short smartNodeAddress, String roomName,
	                                                      String floorName)
	{
		LightingZoneProfileFragment f = new LightingZoneProfileFragment();
		Bundle bundle = new Bundle();
		bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, smartNodeAddress);
		bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
		bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
		f.setArguments(bundle);
		return f;
	}
	
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		LayoutInflater inflater = (LayoutInflater) LayoutInflater.from(getActivity());
		view = inflater.inflate(R.layout.fragment_lighting_control_details, null);
		mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
		mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
		mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
		mZone = RoomBLL.findZoneByName(mFloorName, mRoomName);
		mLightProfile = new LightProfile(mZone.roomName);
		mSmartNode = SmartNodeBLL.getSmartNodeAndSeed(mSmartNodeAddress, mRoomName);
		getSmartNodeAnalogOutputOne();
		lcmSetCommand = (TextView) view.findViewById(R.id.lcmSetCommand);
		lcmCancelCommand = (TextView) view.findViewById(R.id.lcmCancelCommand);
		if (!mbIsInEditMode)
		{
			lcmCancelCommand.setVisibility(View.INVISIBLE);
		}
		//TODO: if they close dialog how do we remove seed from CM?
		lcmRelay1Override = (ToggleButton) view.findViewById(R.id.lcmRelay1Override);
		lcmRelay2Override = (ToggleButton) view.findViewById(R.id.lcmRelay2Override);
		lcmAnalog1OutOverride = (ToggleButton) view.findViewById(R.id.lcmAnalog1OutOverride);
		lcmAnalog2OutOverride = (ToggleButton) view.findViewById(R.id.lcmAnalog2OutOverride);
		lcmAnalog1OutOverride
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
				{
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
					{
						Log.i(TAG, "lcmAnalog1OutOverride isChecked: " + isChecked);
						mLightProfile.on = isChecked;
						SmartNodeBLL.sendControlsMessage(mLightProfile);
					}
				});
		spRelay1 = (Spinner) view.findViewById(R.id.lcmRelay1Actuator);
		spRelay2 = (Spinner) view.findViewById(R.id.lcmRelay2Actuator);
		relay1Switch = (Switch) view.findViewById(R.id.lcmRelay1Switch);
		relay2Switch = (Switch) view.findViewById(R.id.lcmRelay2Switch);
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
		analog1OutSwitch = (Switch) view.findViewById(R.id.lcmAnalog1OutSwitch);
		analog2OutSwitch = (Switch) view.findViewById(R.id.lcmAnalog2OutSwitch);
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
		analog1InSwitch = (Switch) view.findViewById(R.id.lcmAnalog1InSwitch);
		analog1InEditText = (EditText) view.findViewById(R.id.lcmAnalog1InEditName);
		analog1InAdapter = ArrayAdapter
				                   .createFromResource(getActivity(), R.array.lcm_analog_in, R.layout.spinner_dropdown_item);
		analog1InAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		spAnalog1In.setAdapter(analog1InAdapter);
		analog2InEditText = (EditText) view.findViewById(R.id.lcmAnalog2InEditName);
		analog2InSwitch = (Switch) view.findViewById(R.id.lcmAnalog2InSwitch);
		spAnalog2In = (Spinner) view.findViewById(R.id.lcmAnalog2InActuator);
		analog2InAdapter = ArrayAdapter
				                   .createFromResource(getActivity(), R.array.lcm_analog_in, R.layout.spinner_dropdown_item);
		analog2InAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		spAnalog2In.setAdapter(analog2InAdapter);
		relay1Switch.setOnCheckedChangeListener(this);
		relay2Switch.setOnCheckedChangeListener(this);
		analog1OutSwitch.setOnCheckedChangeListener(this);
		analog2OutSwitch.setOnCheckedChangeListener(this);
		analog1InSwitch.setOnCheckedChangeListener(this);
		analog2InSwitch.setOnCheckedChangeListener(this);
		Button setBtn = (Button) view.findViewById(R.id.lcmSetCommand);
		setBtn.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//create CcuApp structure and send data
			}
		});
		Button cancelBtn = (Button) view.findViewById(R.id.lcmCancelCommand);
		cancelBtn.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				dismiss();
			}
		});
		return new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle)
				       .setTitle("Lighting Test").setView(view).setCancelable(false).create();
	}
	
	
	public SmartNodeOutput getSmartNodeAnalogOutputOne()
	{
		if (smartNodeAnalogOutputOne == null)
		{
			smartNodeAnalogOutputOne = new SmartNodeOutput();
			smartNodeAnalogOutputOne.mOutput = Output.Analog;
			smartNodeAnalogOutputOne.mOutputAnalogActuatorType =
					OutputAnalogActuatorType.ZeroToTenV;
			smartNodeAnalogOutputOne.mSmartNodePort = Port.ANALOG_OUT_ONE;
			smartNodeAnalogOutputOne.mUniqueID = UUID.randomUUID();
			smartNodeAnalogOutputOne.mName = "Analog 1";
			smartNodeAnalogOutputOne.mSmartNodeAddress = mSmartNodeAddress;
			mLightProfile.smartNodeOutputs.add(smartNodeAnalogOutputOne);
		}
		return smartNodeAnalogOutputOne;
	}
	
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
		switch (parent.getId())
		{
		}
	}
	
	
	@Override
	public void onNothingSelected(AdapterView<?> parent)
	{
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
		   /* case R.id.lcmRelay1EditImg:
		        showEditLogicalNameDialog(relay1EditText,mLCMControls.getRelay1CircuitName(), v.getId());
                break;*/
		}
	}
}
