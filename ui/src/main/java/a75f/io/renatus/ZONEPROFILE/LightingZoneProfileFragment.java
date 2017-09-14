package a75f.io.renatus.ZONEPROFILE;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;

import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.SmartNode;
import a75f.io.bo.building.SmartNodeOutput;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.bo.building.definitions.Port;
import a75f.io.logic.L;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.FloorPlanFragment;
import a75f.io.renatus.R;

import static a75f.io.logic.L.addZoneProfileToZone;

/**
 * Created by anilkumar isOn 27-10-2016.
 */

public class LightingZoneProfileFragment extends BaseDialogFragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener
{
	
	public static final  String ID  = LightingZoneProfileFragment.class.getSimpleName();
	private static final String TAG = "Lighting";
	
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
	ToggleButton lcmAnalog1OutOverride;
	ToggleButton lcmAnalog2OutOverride;
	
	ArrayList<String> zoneCircuitNames;
	Zone              mZone;
	LightProfile      mLightProfile;
	SmartNode         mSmartNode;
	SmartNodeOutput   smartNodeAnalogOutputOne;
	SmartNodeOutput   mSmartNodeRelayOne;
	SmartNodeOutput   relayOne;
	SmartNodeOutput   relayTwo;
	SmartNodeOutput   analogOne;
	SmartNodeOutput   analogTwo;
	private ArrayAdapter<CharSequence> relay1Adapter;
	private ArrayAdapter<CharSequence> relay2Adapter;
	private ArrayAdapter<CharSequence> analog1OutAdapter;
	private ArrayAdapter<CharSequence> analog2OutAdapter;
	private ArrayAdapter<CharSequence> analog1InAdapter;
	private ArrayAdapter<CharSequence> analog2InAdapter;
	private short                      mSmartNodeAddress;
	
	public LightingZoneProfileFragment()
	{
	}
	
	public static LightingZoneProfileFragment newInstance(short smartNodeAddress, String roomName, String floorName)
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
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		view = inflater.inflate(R.layout.fragment_lighting_control_details, null);
		mSmartNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
		String mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
		String mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
		mZone = L.findZoneByName(mFloorName, mRoomName);
		mLightProfile = (LightProfile) mZone.findLightProfile();
		mSmartNode = L.getSmartNodeAndSeed(mSmartNodeAddress, mRoomName);
		lcmSetCommand = (TextView) view.findViewById(R.id.lcmSetCommand);
		lcmCancelCommand = (TextView) view.findViewById(R.id.lcmCancelCommand);
		if (!mbIsInEditMode)
		{
			lcmCancelCommand.setVisibility(View.INVISIBLE);
		}
		//TODO: if they close dialog how do we remove seed from CM?
		lcmRelay1Override = (ToggleButton) view.findViewById(R.id.lcmRelay1Override);
		lcmRelay1Override.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				getSmartNodeRelayOne(isChecked);
				L.sendLightControlsMessage(mLightProfile);
			}
		});
		lcmRelay2Override = (ToggleButton) view.findViewById(R.id.lcmRelay2Override);
		lcmAnalog1OutOverride = (ToggleButton) view.findViewById(R.id.lcmAnalog1OutOverride);
		lcmAnalog2OutOverride = (ToggleButton) view.findViewById(R.id.lcmAnalog2OutOverride);
		lcmAnalog1OutOverride.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				getSmartNodeAnalogOutputOne(isChecked);
				Log.i(TAG, "lcmAnalog1OutOverride isChecked: " + isChecked);
				L.sendLightControlsMessage(mLightProfile);
			}
		});
		spRelay1 = (Spinner) view.findViewById(R.id.lcmRelay1Actuator);
		spRelay2 = (Spinner) view.findViewById(R.id.lcmRelay2Actuator);
		relay1Switch = (SwitchCompat) view.findViewById(R.id.lcmRelay1Switch);
		relay2Switch = (SwitchCompat) view.findViewById(R.id.lcmRelay2Switch);
		relay1EditText = (EditText) view.findViewById(R.id.lcmRelay1EditName);
		relay2EditText = (EditText) view.findViewById(R.id.lcmRelay2EditName);
		relay1Adapter = ArrayAdapter.createFromResource(getActivity(), R.array.lcm_relay, R.layout.spinner_dropdown_item);
		relay1Adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		spRelay1.setAdapter(relay1Adapter);
		relay2Adapter = ArrayAdapter.createFromResource(getActivity(), R.array.lcm_relay, R.layout.spinner_dropdown_item);
		relay2Adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		spRelay2.setAdapter(relay2Adapter);
		spAnalog1Out = (Spinner) view.findViewById(R.id.lcmAnalog1OutActuator);
		spAnalog2Out = (Spinner) view.findViewById(R.id.lcmAnalog2OutActuator);
		analog1OutSwitch = (SwitchCompat) view.findViewById(R.id.lcmAnalog1OutSwitch);
		analog2OutSwitch = (SwitchCompat) view.findViewById(R.id.lcmAnalog2OutSwitch);
		analog1OutEditText = (EditText) view.findViewById(R.id.lcmAnalog1OutEditName);
		analog2OutEditText = (EditText) view.findViewById(R.id.lcmAnalog2OutEditName);
		analog1OutAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.lcm_analog_out, R.layout.spinner_dropdown_item);
		analog1OutAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		spAnalog1Out.setAdapter(analog1OutAdapter);
		analog2OutAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.lcm_analog_out, R.layout.spinner_dropdown_item);
		analog2OutAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		spAnalog2Out.setAdapter(analog2OutAdapter);
		spAnalog1In = (Spinner) view.findViewById(R.id.lcmAnalog1InActuator);
		analog1InSwitch = (SwitchCompat) view.findViewById(R.id.lcmAnalog1InSwitch);
		analog1InEditText = (EditText) view.findViewById(R.id.lcmAnalog1InEditName);
		analog1InAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.lcm_analog_in, R.layout.spinner_dropdown_item);
		analog1InAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		spAnalog1In.setAdapter(analog1InAdapter);
		analog2InEditText = (EditText) view.findViewById(R.id.lcmAnalog2InEditName);
		analog2InSwitch = (SwitchCompat) view.findViewById(R.id.lcmAnalog2InSwitch);
		spAnalog2In = (Spinner) view.findViewById(R.id.lcmAnalog2InActuator);
		analog2InAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.lcm_analog_in, R.layout.spinner_dropdown_item);
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
		Button setBtn = (Button) view.findViewById(R.id.lcmSetCommand);
		setBtn.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				saveLightData();
				getActivity().sendBroadcast(new Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED));
				dismiss();
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
		bindData();
		return new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle).setTitle("Lighting Profile").setView(view).setCancelable(false).create();
	}
	
	public SmartNodeOutput getSmartNodeRelayOne(boolean isChecked)
	{
		if (mSmartNodeRelayOne == null)
		{
			mSmartNodeRelayOne = new SmartNodeOutput();
			mSmartNodeRelayOne.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
			mSmartNodeRelayOne.mSmartNodePort = Port.RELAY_ONE;
			mSmartNodeRelayOne.mName = "Relay 1";
			mSmartNodeRelayOne.mSmartNodeAddress = mSmartNodeAddress;
			mLightProfile.smartNodeOutputs.add(mSmartNodeRelayOne);
		}
		if (spRelay1.getSelectedItemPosition() == 0)
		{
			mSmartNodeRelayOne.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
		}
		else
		{
			mSmartNodeRelayOne.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
		}
		mSmartNodeRelayOne.mName = relay1EditText.getText() != null && relay1EditText.getText().length() > 0 ? relay1EditText.getText().toString() : "empty";
		return mSmartNodeRelayOne;
	}
	
	public SmartNodeOutput getSmartNodeAnalogOutputOne(boolean isChecked)
	{
		if (smartNodeAnalogOutputOne == null)
		{
			smartNodeAnalogOutputOne = new SmartNodeOutput();
			smartNodeAnalogOutputOne.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
			smartNodeAnalogOutputOne.mSmartNodePort = Port.ANALOG_OUT_ONE;
			smartNodeAnalogOutputOne.mName = "Analog 1";
			smartNodeAnalogOutputOne.mSmartNodeAddress = mSmartNodeAddress;
			mLightProfile.smartNodeOutputs.add(smartNodeAnalogOutputOne);
		}
		if (spAnalog1Out.getSelectedItemPosition() == 0)
		{
			smartNodeAnalogOutputOne.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
		}
		else
		{
			smartNodeAnalogOutputOne.mOutputAnalogActuatorType = OutputAnalogActuatorType.TwoToTenV;
		}
		smartNodeAnalogOutputOne.mName = analog1OutEditText.getText() != null && analog1OutEditText.getText().length() > 0 ? analog1OutEditText.getText().toString() : "empty";
		return smartNodeAnalogOutputOne;
	}
	
	public void saveLightData()
	{
		if (relay1Switch.isChecked())
		{
			if (spRelay1.getSelectedItemPosition() == 0)
			{
				relayOne.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
			}
			else
			{
				relayOne.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
			}
			relayOne.mName = relay1EditText.getText().toString();
			mLightProfile.addCircuit(relayOne);
		}
		else
		{
			mLightProfile.removeCircuit(relayOne);
		}
		if (relay2Switch.isChecked())
		{
			if (spRelay2.getSelectedItemPosition() == 0)
			{
				relayTwo.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen;
			}
			else
			{
				relayTwo.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyClose;
			}
			relayTwo.mName = relay2EditText.getText().toString();
			relayTwo.mSmartNodePort = Port.RELAY_TWO;
			mLightProfile.addCircuit(relayTwo);
		}
		else
		{
			mLightProfile.removeCircuit(relayTwo);
		}
		if (analog1OutSwitch.isChecked())
		{
			if (spAnalog1Out.getSelectedItemPosition() == 0)
			{
				analogOne.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
			}
			else
			{
				analogOne.mOutputAnalogActuatorType = OutputAnalogActuatorType.TwoToTenV;
			}
			analogOne.mName = analog1OutEditText.getText().toString();
			analogOne.mVal = 100;
			mLightProfile.addCircuit(analogOne);
		}
		else
		{
			mLightProfile.removeCircuit(analogOne);
		}
		if (analog2OutSwitch.isChecked())
		{
			if (spAnalog2Out.getSelectedItemPosition() == 0)
			{
				analogTwo.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV;
			}
			else
			{
				analogTwo.mOutputAnalogActuatorType = OutputAnalogActuatorType.TwoToTenV;
			}
			analogTwo.mName = analog2OutEditText.getText().toString();
			analogTwo.mVal = 100;
			mLightProfile.addCircuit(analogTwo);
		}
		else
		{
			mLightProfile.removeCircuit(analogTwo);
		}
		addZoneProfileToZone(mSmartNode, mZone, mLightProfile);
	}
	
	private void bindData()
	{
		ArrayList<SmartNodeOutput> smartNodeOutputs = mLightProfile.findSmartNodeOutputs(mSmartNodeAddress);
		relayOne = L.findPort(smartNodeOutputs, Port.RELAY_ONE, mSmartNodeAddress);
		relayTwo = L.findPort(smartNodeOutputs, Port.RELAY_TWO, mSmartNodeAddress);
		analogOne = L.findPort(smartNodeOutputs, Port.ANALOG_OUT_ONE, mSmartNodeAddress);
		analogTwo = L.findPort(smartNodeOutputs, Port.ANALOG_OUT_TWO, mSmartNodeAddress);
		//RelayOne
		spRelay1.setSelection(relayOne.mOutputRelayActuatorType == OutputRelayActuatorType.NormallyOpen ? 0 : 1);
		relay1EditText.setText(relayOne.getCircuitName());
		relay1Switch.setChecked(relayOne.mConfigured);
		//RelayTwo
		spRelay2.setSelection(relayOne.mOutputRelayActuatorType == OutputRelayActuatorType.NormallyOpen ? 0 : 1);
		relay2EditText.setText(relayTwo.getCircuitName());
		relay2Switch.setChecked(relayTwo.mConfigured);
		//AnalogOne
		spAnalog1Out.setSelection(analogOne.mOutputAnalogActuatorType == OutputAnalogActuatorType.ZeroToTenV ? 0 : 1);
		analog1OutEditText.setText(analogOne.getCircuitName());
		analog1OutSwitch.setChecked(analogOne.mConfigured);
		//AnalogTwo
		spAnalog2Out.setSelection(analogOne.mOutputAnalogActuatorType == OutputAnalogActuatorType.ZeroToTenV ? 0 : 1);
		analog2OutEditText.setText(analogTwo.getCircuitName());
		analog2OutSwitch.setChecked(analogTwo.mConfigured);
		//TODO: inputs
		//		ArrayList<SmartNodeInput> smartNodeInputs =
		//				mLightProfile.findSmartNodeInputs(mSmartNodeAddress);
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		switch (buttonView.getId())
		{
			case R.id.lcmRelay1Switch:
				relayOne.mConfigured = isChecked;
				break;
			case R.id.lcmRelay2Switch:
				relayTwo.mConfigured = isChecked;
				break;
			case R.id.lcmAnalog1OutSwitch:
				analogOne.mConfigured = isChecked;
				break;
			case R.id.lcmAnalog2OutSwitch:
				analogTwo.mConfigured = isChecked;
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
